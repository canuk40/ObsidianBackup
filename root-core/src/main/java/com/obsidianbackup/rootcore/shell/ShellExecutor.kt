package com.obsidianbackup.rootcore.shell

import com.obsidianbackup.rootcore.detection.RootDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shell executor for running commands with root privileges.
 * Combines ObsidianBox's process-based execution with SafeShellExecutor's
 * command whitelisting, validation, and audit logging.
 *
 * Features (merged from both projects):
 * - su binary auto-detection from multiple paths (ObsidianBox)
 * - Command whitelisting + dangerous pattern detection (SafeShellExecutor)
 * - Process timeout guards to prevent hangs (ObsidianBox)
 * - Structured ShellResult return type (ObsidianBox)
 * - Streaming output via Flow (ObsidianBox)
 * - Audit logging (SafeShellExecutor)
 */
@Singleton
class ShellExecutor @Inject constructor(
    private val rootDetector: RootDetector
) {
    companion object {
        private const val SH_BINARY = "sh"
        private const val TIMEOUT_SECONDS = 30L

        private val SU_SEARCH_PATHS = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sbin/su",
            "/vendor/bin/su",
            "/su/bin/su",
            "/magisk/.core/bin/su",
            "/system/bin/.ext/.su",
            "/data/bin/su",
            "su"
        )

        private val SU_BINARY: String by lazy {
            val foundPath = SU_SEARCH_PATHS.firstOrNull { path ->
                try {
                    java.io.File(path).let { file ->
                        file.exists() && file.canExecute()
                    }
                } catch (e: Exception) {
                    false
                }
            } ?: "su"
            Timber.i("Selected SU_BINARY: $foundPath")
            foundPath
        }

        // Command whitelist from SafeShellExecutor
        val ALLOWED_COMMANDS = setOf(
            "busybox", "tar", "zstd", "sha256sum", "rsync",
            "restorecon", "pm", "am", "cp", "mkdir", "chmod",
            "chown", "rm", "mv", "cat", "ls", "du", "find",
            "grep", "sed", "awk", "split", "test", "stat", "echo",
            "id", "whoami", "getenforce", "magisk", "mount",
            "umount", "df", "wc", "head", "tail", "sort", "uniq",
            "appops", "settings", "dumpsys", "getprop", "setprop",
            "content", "svc", "cmd", "sleep",
            "ln", "sm", "unzip", "touch"
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
            Regex("""rm\s+-rf\s+/\s*$"""),
            Regex("""chmod\s+777\s+/"""),
            Regex("""curl.*\|"""),
            Regex("""wget.*\|"""),
        )
    }

    /** Validate a command before execution. Returns null if allowed, error message if blocked. */
    fun validateCommand(command: String): String? {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return "Empty command"

        for (pattern in DANGEROUS_PATTERNS) {
            if (pattern.containsMatchIn(trimmed)) {
                return "Dangerous pattern detected: ${pattern.pattern}"
            }
        }

        val tokens = trimmed.split(Regex("""\s+"""))
        val baseCommand = tokens.firstOrNull() ?: return "Cannot extract base command"

        val isAllowed = ALLOWED_COMMANDS.any { allowed ->
            baseCommand == allowed || baseCommand.endsWith("/$allowed")
        }
        if (!isAllowed) return "Command '$baseCommand' is not in allowed list"

        // Block write access to critical system paths
        for (criticalPath in CRITICAL_SYSTEM_PATHS) {
            if (trimmed.contains(criticalPath)) {
                if (!trimmed.startsWith("cat ") && !trimmed.startsWith("ls ") && !trimmed.startsWith("stat ")) {
                    return "Write access to critical system path: $criticalPath"
                }
            }
        }

        // Block destructive rm on critical dirs
        if (trimmed.startsWith("rm ")) {
            val paths = tokens.filter { it.startsWith("/") }
            for (path in paths) {
                if (path == "/" || path == "/data" || path == "/system") {
                    return "Attempting to delete critical directory: $path"
                }
            }
        }

        return null
    }

    /** Execute command with root privileges. Validates command first. */
    suspend fun executeRoot(command: String): ShellResult = withContext(Dispatchers.IO) {
        val validationError = validateCommand(command)
        if (validationError != null) {
            Timber.w("Command blocked: $command — $validationError")
            return@withContext ShellResult.failure(-1, "Command blocked: $validationError")
        }
        execute(command, asRoot = true)
    }

    /** Execute command with root, bypassing validation. Use only for trusted internal commands. */
    suspend fun executeRootUnsafe(command: String): ShellResult = withContext(Dispatchers.IO) {
        execute(command, asRoot = true)
    }

    /** Execute command without root. */
    suspend fun executeShell(command: String): ShellResult = withContext(Dispatchers.IO) {
        execute(command, asRoot = false)
    }

    /** Execute command and stream output line by line. */
    fun executeRootStream(command: String): Flow<String> = flow {
        var process: Process? = null
        try {
            val processBuilder = ProcessBuilder(SU_BINARY)
            processBuilder.redirectErrorStream(true)
            process = processBuilder.start()

            val outputStream = DataOutputStream(process.outputStream)
            outputStream.writeBytes("$command\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                emit(line!!)
            }

            process.waitFor()
        } catch (e: Exception) {
            Timber.e(e, "Stream execution failed: $command")
            emit("Error: ${e.message}")
        } finally {
            process?.destroy()
        }
    }.flowOn(Dispatchers.IO)

    /** Check if root access is available (uses cached RootDetector). */
    suspend fun checkRootAccess(): Boolean {
        return rootDetector.getOrRefreshStatus().rootGranted
    }

    /** Execute multiple commands in sequence. */
    suspend fun executeBatch(commands: List<String>, asRoot: Boolean = true): List<ShellResult> {
        return commands.map { command ->
            if (asRoot) executeRoot(command) else executeShell(command)
        }
    }

    private fun execute(command: String, asRoot: Boolean): ShellResult {
        val startTime = System.currentTimeMillis()
        var process: Process? = null

        return try {
            Timber.d("Executing: $command (root: $asRoot)")

            val shell = if (asRoot) SU_BINARY else SH_BINARY
            val processBuilder = ProcessBuilder(shell)
            process = processBuilder.start()

            val outputStream = DataOutputStream(process.outputStream)
            outputStream.writeBytes("$command\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()

            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()

            val completed = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                Timber.w("Command timed out after ${TIMEOUT_SECONDS}s: $command")
                return ShellResult.failure(-1, "Command timed out after ${TIMEOUT_SECONDS}s")
            }

            val exitCode = process.exitValue()
            val executionTime = System.currentTimeMillis() - startTime

            Timber.d("Exit code: $exitCode, time: ${executionTime}ms")

            ShellResult(
                success = exitCode == 0,
                exitCode = exitCode,
                stdout = stdout.trim(),
                stderr = stderr.trim(),
                executionTimeMs = executionTime
            )
        } catch (e: Exception) {
            Timber.e(e, "Execution failed: $command")
            ShellResult.failure(
                exitCode = -1,
                error = e.message ?: "Unknown error",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        } finally {
            process?.destroy()
        }
    }
}
