// engine/shell/AuditLogger.kt
package com.obsidianbackup.engine.shell

import com.obsidianbackup.model.PermissionMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class AuditLogger(private val logFile: File) {
    private val json = Json { prettyPrint = false }

    init {
        logFile.parentFile?.mkdirs()
    }

    fun logExecution(command: String, mode: PermissionMode) {
        val entry = AuditEntry(
            timestamp = System.currentTimeMillis(),
            type = AuditType.EXECUTION,
            command = command,
            permissionMode = mode.displayName
        )
        writeEntry(entry)
    }

    fun logBlocked(command: String, reason: String) {
        val entry = AuditEntry(
            timestamp = System.currentTimeMillis(),
            type = AuditType.BLOCKED,
            command = command,
            details = reason
        )
        writeEntry(entry)
    }

    fun logOutput(output: String) {
        // Only log in debug mode to avoid huge files
        val isDebug = com.obsidianbackup.BuildConfig.DEBUG
        if (isDebug) {
            val entry = AuditEntry(
                timestamp = System.currentTimeMillis(),
                type = AuditType.OUTPUT,
                details = output
            )
            writeEntry(entry)
        }
    }

    fun logError(error: String) {
        val entry = AuditEntry(
            timestamp = System.currentTimeMillis(),
            type = AuditType.ERROR,
            details = error
        )
        writeEntry(entry)
    }

    fun logException(exception: Exception) {
        val entry = AuditEntry(
            timestamp = System.currentTimeMillis(),
            type = AuditType.EXCEPTION,
            details = exception.stackTraceToString()
        )
        writeEntry(entry)
    }

    private fun writeEntry(entry: AuditEntry) {
        try {
            logFile.appendText(json.encodeToString(entry) + "\n")
        } catch (e: Exception) {
            // Fail silently - don't block operations due to logging failure
        }
    }

    fun exportLogs(): File {
        return logFile
    }

    fun clearLogs() {
        try {
            logFile.writeText("")
        } catch (e: Exception) {
            // Fail silently
        }
    }

    fun getLogSize(): Long {
        return try {
            logFile.length()
        } catch (e: Exception) {
            0L
        }
    }
}

@Serializable
data class AuditEntry(
    val timestamp: Long,
    val type: AuditType,
    val command: String? = null,
    val permissionMode: String? = null,
    val details: String? = null
)

@Serializable
enum class AuditType {
    EXECUTION,
    BLOCKED,
    OUTPUT,
    ERROR,
    EXCEPTION
}
