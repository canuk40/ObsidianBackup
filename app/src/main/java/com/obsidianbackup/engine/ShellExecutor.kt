// engine/ShellExecutor.kt
package com.obsidianbackup.engine

import android.util.Log
import com.obsidianbackup.model.PermissionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

sealed class ShellResult {
    data class Success(val output: String, val exitCode: Int = 0) : ShellResult()
    data class Error(val error: String, val exitCode: Int) : ShellResult()
}

class ShellExecutor(private val permissionMode: PermissionMode) {

    private val shellPrefix = when (permissionMode) {
        PermissionMode.ROOT -> "su -c"
        PermissionMode.SHIZUKU -> "sh" // Shizuku commands go through service
        PermissionMode.ADB -> "sh"
        PermissionMode.SAF -> "sh"
    }

    suspend fun execute(command: String): ShellResult = withContext(Dispatchers.IO) {
        try {
            val fullCommand = if (permissionMode == PermissionMode.ROOT) {
                "$shellPrefix \"$command\""
            } else {
                command
            }

            val process = Runtime.getRuntime().exec(fullCommand)

            val output = StringBuilder()
            val error = StringBuilder()

            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.forEachLine { output.append(it).append("\n") }
            }

            BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                reader.forEachLine { error.append(it).append("\n") }
            }

            val exitCode = process.waitFor()

            if (exitCode == 0) {
                ShellResult.Success(output.toString(), exitCode)
            } else {
                ShellResult.Error(error.toString(), exitCode)
            }
        } catch (e: Exception) {
            ShellResult.Error(e.message ?: "Unknown error", -1)
        }
    }

    suspend fun executeMultiple(commands: List<String>): List<ShellResult> {
        return commands.map { execute(it) }
    }

    // Check if BusyBox is available
    suspend fun checkBusyBox(): Boolean {
        val result = execute("busybox --help")
        return result is ShellResult.Success
    }

    // Get BusyBox version
    suspend fun getBusyBoxVersion(): String? {
        val result = execute("busybox | head -n 1")
        return if (result is ShellResult.Success) {
            result.output.trim()
        } else null
    }
}

class SafeShellExecutor(private val mode: PermissionMode) {
    private val allowedCommands = setOf(
        "busybox", "tar", "zstd", "sha256sum", "rsync",
        "restorecon", "pm", "am", "cp", "mkdir", "chmod"
    )

    private val deniedPaths = setOf(
        "/system/framework",
        "/system/bin",
        "/vendor",
        "/init.rc"
    )

    suspend fun execute(command: String): ShellResult {
        // Validate command is safe
        if (!validateCommand(command)) {
            return ShellResult.Error("Command rejected by safety policy", -1)
        }

        // Log all commands for audit
        auditLog.record(command, mode)

        return executeUnsafe(command)
    }

    private fun validateCommand(command: String): Boolean {
        val tokens = command.split(" ")
        val baseCommand = tokens.firstOrNull() ?: return false

        if (!allowedCommands.any { baseCommand.contains(it) }) {
            return false
        }

        // Check for path traversal or denied paths
        return !deniedPaths.any { denied ->
            command.contains(denied)
        }
    }

    private suspend fun executeUnsafe(command: String): ShellResult = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(command)

            val output = StringBuilder()
            val error = StringBuilder()

            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.forEachLine { output.append(it).append("\n"); auditLog.logOutput(it) }
            }

            BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                reader.forEachLine { error.append(it).append("\n"); auditLog.logError(it) }
            }

            val exitCode = process.waitFor()

            if (exitCode == 0) {
                ShellResult.Success(output.toString(), exitCode)
            } else {
                ShellResult.Error(error.toString(), exitCode)
            }
        } catch (e: Exception) {
            auditLog.logException(e)
            ShellResult.Error(e.message ?: "Unknown error", -1)
        }
    }
}

object auditLog {
    private var logFile: File? = null
    private const val MAX_LOG_SIZE = 10 * 1024 * 1024 // 10 MB
    
    fun initialize(logDirectory: File) {
        logDirectory.mkdirs()
        logFile = File(logDirectory, "shell_audit.log")
        
        // Rotate if too large
        if ((logFile?.length() ?: 0) > MAX_LOG_SIZE) {
            val backup = File(logDirectory, "shell_audit.log.old")
            logFile?.renameTo(backup)
            logFile = File(logDirectory, "shell_audit.log")
        }
    }
    
    fun record(command: String, mode: PermissionMode) {
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US)
            .format(java.util.Date())
        val entry = "$timestamp [$mode] EXEC: $command\n"
        
        Log.i("SafeShellExecutor", "[$mode] $command")
        appendToFile(entry)
    }

    fun logOutput(line: String) {
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US)
            .format(java.util.Date())
        val entry = "$timestamp [OUTPUT] $line\n"
        
        Log.d("SafeShellExecutor", line)
        appendToFile(entry)
    }

    fun logError(line: String) {
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US)
            .format(java.util.Date())
        val entry = "$timestamp [ERROR] $line\n"
        
        Log.e("SafeShellExecutor", line)
        appendToFile(entry)
    }

    fun logException(e: Exception) {
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US)
            .format(java.util.Date())
        val stackTrace = e.stackTraceToString()
        val entry = "$timestamp [EXCEPTION] ${e.message}\n$stackTrace\n"
        
        Log.e("SafeShellExecutor", "Exception", e)
        appendToFile(entry)
    }
    
    private fun appendToFile(entry: String) {
        try {
            logFile?.appendText(entry)
        } catch (e: Exception) {
            Log.w("auditLog", "Failed to write to audit log: ${e.message}")
        }
    }
    
    fun getLogContent(): String {
        return try {
            val file = logFile
            if (file != null && file.exists()) {
                file.readText(Charsets.UTF_8)
            } else {
                "No audit log available"
            }
        } catch (e: Exception) {
            "Error reading audit log: ${e.message}"
        }
    }
    
    fun clearLog() {
        try {
            logFile?.writeText("")
        } catch (e: Exception) {
            Log.w("auditLog", "Failed to clear audit log: ${e.message}")
        }
    }
}
