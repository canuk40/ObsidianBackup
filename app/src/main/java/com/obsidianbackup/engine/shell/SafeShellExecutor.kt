// engine/shell/SafeShellExecutor.kt
package com.obsidianbackup.engine.shell

import com.obsidianbackup.model.PermissionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

data class CommandValidationResult(
    val isAllowed: Boolean,
    val reason: String? = null,
    val sanitizedCommand: String? = null
)

class SafeShellExecutor(
    private val permissionMode: PermissionMode,
    private val auditLogger: AuditLogger,
    private val busyBoxBinDir: String? = null
) {

    companion object {
        private val ALLOWED_COMMANDS = setOf(
            "busybox", "tar", "zstd", "sha256sum", "rsync",
            "restorecon", "pm", "am", "cp", "mkdir", "chmod",
            "chown", "rm", "mv", "cat", "ls", "du", "find",
            "grep", "sed", "awk", "split", "test", "stat", "echo"
        )

        private val CRITICAL_SYSTEM_PATHS = setOf(
            "/system/framework",
            "/system/bin",
            "/system/lib",
            "/vendor/",
            "/init",
            "/boot",
            "/recovery"
        )

        private val DANGEROUS_PATTERNS = listOf(
            Regex("""[;&|`$]"""),  // Shell metacharacters
            Regex(""">\s*/dev/"""), // Redirect to device
            Regex("""rm\s+-rf\s+/\s*$"""), // Delete root
            Regex("""chmod\s+777\s+/"""), // Blanket permissions on root
            Regex("""curl.*\|"""), // Piped curl
            Regex("""wget.*\|"""), // Piped wget
            Regex("""\$\(.*\)"""), // Command substitution
            Regex("""`.*`"""), // Backtick command substitution
            Regex("""<\(.*\)"""), // Process substitution
        )
    }

    suspend fun execute(command: String): com.obsidianbackup.engine.ShellResult {
        // Validate command before execution
        val validation = validateCommand(command)

        if (!validation.isAllowed) {
            auditLogger.logBlocked(command, validation.reason ?: "Unknown")
            return com.obsidianbackup.engine.ShellResult.Error(
                "Command blocked: ${validation.reason}",
                exitCode = -1
            )
        }

        val commandToExecute = validation.sanitizedCommand ?: command

        // Log to audit trail
        auditLogger.logExecution(commandToExecute, permissionMode)

        return executeUnsafe(commandToExecute)
    }

    private fun validateCommand(command: String): CommandValidationResult {
        val trimmed = command.trim()

        // Check for empty command
        if (trimmed.isEmpty()) {
            return CommandValidationResult(false, "Empty command")
        }

        // Check for dangerous patterns
        DANGEROUS_PATTERNS.forEach { pattern ->
            if (pattern.containsMatchIn(trimmed)) {
                return CommandValidationResult(
                    false,
                    "Dangerous pattern detected: ${pattern.pattern}"
                )
            }
        }

        // Extract base command
        val tokens = trimmed.split(Regex("""\s+"""))
        val baseCommand = tokens.firstOrNull() ?: return CommandValidationResult(
            false,
            "Cannot extract base command"
        )

        // Check if base command is allowed
        val isAllowedCommand = ALLOWED_COMMANDS.any { allowed ->
            baseCommand.contains(allowed) || baseCommand.endsWith(allowed)
        }

        if (!isAllowedCommand) {
            return CommandValidationResult(
                false,
                "Command '$baseCommand' is not in allowed list"
            )
        }

        // Check for critical system path access
        CRITICAL_SYSTEM_PATHS.forEach { criticalPath ->
            if (trimmed.contains(criticalPath)) {
                // Allow read-only operations on system paths
                if (!trimmed.contains(" -r ") && !trimmed.contains("cat ") && !trimmed.contains("ls ")) {
                    return CommandValidationResult(
                        false,
                        "Write access to critical system path: $criticalPath"
                    )
                }
            }
        }

        // Validate paths in rm commands
        if (trimmed.startsWith("rm ")) {
            val paths = extractPaths(trimmed)
            paths.forEach { path ->
                if (path == "/" || path == "/data" || path == "/system") {
                    return CommandValidationResult(
                        false,
                        "Attempting to delete critical directory: $path"
                    )
                }
            }
        }

        // Sanitize command (escape special characters)
        val sanitized = sanitizeCommand(trimmed)

        return CommandValidationResult(
            isAllowed = true,
            sanitizedCommand = sanitized
        )
    }

    private fun extractPaths(command: String): List<String> {
        val tokens = command.split(Regex("""\s+"""))
        return tokens.filter { it.startsWith("/") || it.startsWith("./") }
    }

    private fun sanitizeCommand(command: String): String {
        // Remove any null bytes and other dangerous characters
        var sanitized = command.replace("\u0000", "")
        
        // Remove any attempts at command substitution
        sanitized = sanitized.replace(Regex("""\$\{[^}]*\}"""), "")
        sanitized = sanitized.replace(Regex("""\$\([^)]*\)"""), "")
        
        return sanitized
    }

    private suspend fun executeUnsafe(command: String): com.obsidianbackup.engine.ShellResult = withContext(Dispatchers.IO) {
        try {
            val fullCommand = when (permissionMode) {
                PermissionMode.ROOT -> {
                    // Prepend busybox bin dir to PATH so bundled busybox is resolved
                    val wrappedCommand = if (busyBoxBinDir != null) {
                        "PATH='$busyBoxBinDir':\$PATH; $command"
                    } else {
                        command
                    }
                    arrayOf("su", "-c", wrappedCommand)
                }
                else -> {
                    arrayOf("sh", "-c", command)
                }
            }

            val process = Runtime.getRuntime().exec(fullCommand)

            val output = StringBuilder()
            val error = StringBuilder()

            // Read output stream
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.forEachLine { line ->
                    output.append(line).append("\n")
                    auditLogger.logOutput(line)
                }
            }

            // Read error stream
            BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                reader.forEachLine { line ->
                    error.append(line).append("\n")
                    auditLogger.logError(line)
                }
            }

            val exitCode = process.waitFor()

            if (exitCode == 0) {
                com.obsidianbackup.engine.ShellResult.Success(output.toString(), exitCode)
            } else {
                com.obsidianbackup.engine.ShellResult.Error(error.toString(), exitCode)
            }

        } catch (e: Exception) {
            auditLogger.logException(e)
            com.obsidianbackup.engine.ShellResult.Error(e.message ?: "Unknown error", -1)
        }
    }
}
