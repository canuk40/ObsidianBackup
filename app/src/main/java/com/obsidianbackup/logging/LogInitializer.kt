package com.obsidianbackup.logging

import com.obsidianbackup.data.repository.LogRepository
import com.obsidianbackup.model.LogLevel
import com.obsidianbackup.model.OperationType
import com.obsidianbackup.model.SnapshotId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Initializes sample logs for testing
 */
@Singleton
class LogInitializer @Inject constructor(
    private val logRepository: LogRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    fun initializeSampleLogs() {
        scope.launch {
            try {
                val count = logRepository.getLogCount()
                
                // Only insert sample logs if database is empty
                if (count == 0) {
                    Timber.d("Initializing sample logs")
                    
                    // Insert sample logs
                    logRepository.insertLog(
                        operationType = OperationType.BACKUP,
                        level = LogLevel.INFO,
                        message = "ObsidianBackup initialized successfully",
                        details = "All systems operational"
                    )
                    
                    logRepository.insertLog(
                        operationType = OperationType.BACKUP,
                        level = LogLevel.INFO,
                        message = "Database ready",
                        details = "Using Room database version 7"
                    )
                    
                    logRepository.insertLog(
                        operationType = OperationType.VERIFY,
                        level = LogLevel.INFO,
                        message = "Settings persistence enabled",
                        details = "Using DataStore for settings storage"
                    )
                    
                    Timber.d("Sample logs initialized")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize sample logs")
            }
        }
    }
}
