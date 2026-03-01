// work/BackupWorker.kt
package com.obsidianbackup.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.obsidianbackup.data.repository.SettingsRepository
import com.obsidianbackup.domain.usecase.BackupAppsUseCase
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.model.AppId
import com.obsidianbackup.model.BackupComponent
import com.obsidianbackup.performance.BatteryOptimizationManager
import com.obsidianbackup.performance.MemoryOptimizationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupAppsUseCase: BackupAppsUseCase,
    private val settingsRepository: SettingsRepository,
    private val logger: ObsidianLogger
) : CoroutineWorker(context, params) {

    private val batteryManager = BatteryOptimizationManager(applicationContext)
    private val memoryManager = MemoryOptimizationManager(applicationContext)

    override suspend fun doWork(): Result {
        logger.i(TAG, "Starting automated backup work")

        // Battery and memory optimizations
        if (!shouldProceedWithBackup()) {
            logger.w(TAG, "Backup conditions not optimal, rescheduling")
            return Result.retry()
        }

        return try {
            val appIds = settingsRepository.backupAppIds.first()
                .map { AppId(it) }
            
            if (appIds.isEmpty()) {
                logger.w(TAG, "No apps configured for backup, skipping")
                return Result.success()
            }
            
            val components = settingsRepository.backupComponents.first()
                .mapNotNull { componentName ->
                    try {
                        BackupComponent.valueOf(componentName)
                    } catch (e: IllegalArgumentException) {
                        logger.w(TAG, "Invalid backup component: $componentName")
                        null
                    }
                }
                .toSet()
            
            val request = com.obsidianbackup.model.BackupRequest(
                appIds = appIds,
                components = components.ifEmpty { setOf(BackupComponent.APK, BackupComponent.DATA) },
                incremental = settingsRepository.backupIncremental.first(),
                compressionLevel = getOptimalCompressionLevel(),
                encryptionEnabled = settingsRepository.encryptionEnabled.first(),
                description = "Automated backup"
            )

            // Set progress
            setProgress(workDataOf(PROGRESS_KEY to 0))

            val result = backupAppsUseCase(request)

            when (result) {
                is com.obsidianbackup.model.BackupResult.Success -> {
                    logger.i(TAG, "Automated backup completed successfully")
                    
                    // Output success data
                    val outputData = workDataOf(
                        OUTPUT_SNAPSHOT_ID to result.snapshotId.value,
                        OUTPUT_APPS_COUNT to result.appsBackedUp.size,
                        OUTPUT_TOTAL_SIZE to result.totalSize
                    )
                    
                    Result.success(outputData)
                }
                is com.obsidianbackup.model.BackupResult.Failure -> {
                    logger.e(TAG, "Automated backup failed: ${result.reason}")
                    Result.retry()
                }
                is com.obsidianbackup.model.BackupResult.PartialSuccess -> {
                    logger.w(TAG, "Automated backup partially successful")
                    
                    val outputData = workDataOf(
                        OUTPUT_SNAPSHOT_ID to result.snapshotId.value,
                        OUTPUT_APPS_COUNT to result.appsBackedUp.size,
                        OUTPUT_FAILED_COUNT to result.appsFailed.size
                    )
                    
                    Result.success(outputData) // Still consider it successful
                }
            }
        } catch (e: Exception) {
            logger.e(TAG, "Backup worker failed", e)
            Result.failure()
        } finally {
            // Clean up memory
            if (memoryManager.shouldReduceMemoryUsage()) {
                @Suppress("DEPRECATION")
                memoryManager.trimMemory(android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
            }
        }
    }

    /**
     * Check if backup should proceed based on battery and memory conditions
     */
    private suspend fun shouldProceedWithBackup(): Boolean {
        // Check battery conditions
        if (batteryManager.isPowerSaveMode()) {
            logger.w(TAG, "Device in power save mode")
            return false
        }

        // Check thermal state
        if (batteryManager.isThermalThrottling()) {
            logger.w(TAG, "Device is thermal throttling")
            return false
        }

        // Check memory conditions
        if (memoryManager.isLowMemory()) {
            logger.w(TAG, "Device low on memory")
            return false
        }

        // Check if optimal for background work
        if (!batteryManager.isOptimalForBackgroundWork()) {
            logger.w(TAG, "Conditions not optimal for background work")
            return false
        }

        return true
    }

    /**
     * Get optimal compression level based on device conditions
     */
    private fun getOptimalCompressionLevel(): Int {
        return when {
            batteryManager.isPowerSaveMode() -> 3 // Lower compression in power save
            batteryManager.isThermalThrottling() -> 4
            else -> 6 // Default compression level
        }
    }

    override suspend fun getForegroundInfo(): androidx.work.ForegroundInfo {
        // Create notification for foreground service (required for long-running tasks)
        val notification = androidx.core.app.NotificationCompat.Builder(
            applicationContext,
            NOTIFICATION_CHANNEL_ID
        )
            .setContentTitle("Backup in progress")
            .setContentText("Creating backup...")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setOngoing(true)
            .build()

        return androidx.work.ForegroundInfo(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "BackupWorker"
        private const val NOTIFICATION_CHANNEL_ID = "backup_channel"
        private const val NOTIFICATION_ID = 1001
        
        // Progress keys
        const val PROGRESS_KEY = "progress"
        
        // Output keys
        const val OUTPUT_SNAPSHOT_ID = "snapshot_id"
        const val OUTPUT_APPS_COUNT = "apps_count"
        const val OUTPUT_FAILED_COUNT = "failed_count"
        const val OUTPUT_TOTAL_SIZE = "total_size"
    }
}
