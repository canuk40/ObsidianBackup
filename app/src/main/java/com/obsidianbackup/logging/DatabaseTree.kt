package com.obsidianbackup.logging

import com.obsidianbackup.data.repository.LogRepository
import com.obsidianbackup.model.LogLevel
import com.obsidianbackup.model.OperationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Timber tree that logs to the database via LogRepository
 */
@Singleton
class DatabaseTree @Inject constructor(
    private val logRepository: LogRepository
) : Timber.Tree() {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Map Android log priority to LogLevel
        val level = when (priority) {
            android.util.Log.VERBOSE -> LogLevel.VERBOSE
            android.util.Log.DEBUG -> LogLevel.DEBUG
            android.util.Log.INFO -> LogLevel.INFO
            android.util.Log.WARN -> LogLevel.WARN
            android.util.Log.ERROR -> LogLevel.ERROR
            else -> LogLevel.INFO
        }
        
        // Determine operation type from tag
        val operationType = when {
            tag?.contains("Backup", ignoreCase = true) == true -> OperationType.BACKUP
            tag?.contains("Restore", ignoreCase = true) == true -> OperationType.RESTORE
            tag?.contains("Verify", ignoreCase = true) == true -> OperationType.VERIFY
            tag?.contains("Delete", ignoreCase = true) == true -> OperationType.DELETE
            else -> OperationType.BACKUP // Default
        }
        
        val details = t?.let { throwable ->
            "${throwable.message}\n${throwable.stackTraceToString()}"
        }
        
        // Insert log asynchronously
        scope.launch {
            try {
                logRepository.insertLog(
                    operationType = operationType,
                    level = level,
                    message = message,
                    details = details
                )
            } catch (e: Exception) {
                // Avoid infinite loop - just log to Android logcat
                android.util.Log.e("DatabaseTree", "Failed to insert log", e)
            }
        }
        
        // Also log to Android logcat
        android.util.Log.println(priority, tag ?: "ObsidianBackup", message)
        t?.let { android.util.Log.println(priority, tag ?: "ObsidianBackup", it.stackTraceToString()) }
    }
}
