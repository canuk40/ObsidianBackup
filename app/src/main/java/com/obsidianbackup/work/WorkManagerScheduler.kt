// work/WorkManagerScheduler.kt
package com.obsidianbackup.work

import android.content.Context
import androidx.work.*
import com.obsidianbackup.cloud.SyncPolicy
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.performance.BatteryOptimizationManager
import java.util.concurrent.TimeUnit

class WorkManagerScheduler(
    private val context: Context,
    private val logger: ObsidianLogger
) {

    private val workManager = WorkManager.getInstance(context)
    private val batteryManager = BatteryOptimizationManager(context)

    fun scheduleCloudSync(policy: SyncPolicy) {
        // Battery-optimized constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (policy.syncOnWifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
            )
            .apply {
                if (policy.syncOnCharging) {
                    setRequiresCharging(true)
                }
                // Add battery constraint for background sync
                setRequiresBatteryNotLow(true)
            }
            .build()

        val syncWork = PeriodicWorkRequestBuilder<CloudSyncWorker>(
            6, TimeUnit.HOURS // Sync every 6 hours
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                policy.retryPolicy.initialDelayMs,
                TimeUnit.MILLISECONDS
            )
            // Use expedited work for better performance on Android 12+
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(TAG_CLOUD_SYNC)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_CLOUD_SYNC,
            ExistingPeriodicWorkPolicy.KEEP, // Changed from REPLACE to avoid cancelling running work
            syncWork
        )

        logger.i(TAG, "Scheduled battery-optimized cloud sync work")
    }

    fun cancelCloudSync() {
        workManager.cancelUniqueWork(WORK_NAME_CLOUD_SYNC)
        logger.i(TAG, "Cancelled cloud sync work")
    }

    fun scheduleBackupAutomation() {
        // Strict constraints for automated backups to preserve battery
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true) // Added storage check
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val backupWork = PeriodicWorkRequestBuilder<BackupWorker>(
            24, TimeUnit.HOURS, // Daily backup
            2, TimeUnit.HOURS   // Flex interval for battery optimization
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(TAG_BACKUP)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_BACKUP,
            ExistingPeriodicWorkPolicy.KEEP,
            backupWork
        )

        logger.i(TAG, "Scheduled battery-optimized backup automation work")
    }

    fun cancelBackupAutomation() {
        workManager.cancelUniqueWork(WORK_NAME_BACKUP)
        logger.i(TAG, "Cancelled backup automation work")
    }

    fun scheduleOneTimeBackup(delayMinutes: Long = 0) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()
            
        val backupWork = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(constraints)
            .apply {
                if (delayMinutes > 0) {
                    setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                }
            }
            // Use expedited for user-initiated backups
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(TAG_BACKUP)
            .build()

        workManager.enqueue(backupWork)
        logger.i(TAG, "Scheduled one-time backup work with delay: $delayMinutes minutes")
    }
    
    /**
     * Schedule adaptive sync based on battery and network conditions
     */
    fun scheduleAdaptiveSync() {
        val isOptimal = batteryManager.isIgnoringBatteryOptimizations()
        
        val intervalHours = if (isOptimal) 4L else 12L // Adaptive interval
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // WiFi only for adaptive
            .setRequiresBatteryNotLow(true)
            .setRequiresDeviceIdle(true) // Run during device idle
            .build()
        
        val syncWork = PeriodicWorkRequestBuilder<CloudSyncWorker>(
            intervalHours, TimeUnit.HOURS,
            1, TimeUnit.HOURS // Flex interval
        )
            .setConstraints(constraints)
            .addTag(TAG_ADAPTIVE_SYNC)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_ADAPTIVE_SYNC,
            ExistingPeriodicWorkPolicy.REPLACE,
            syncWork
        )
        
        logger.i(TAG, "Scheduled adaptive sync with $intervalHours hour interval")
    }
    
    /**
     * Get work info for monitoring
     */
    fun getWorkInfo(tag: String) = workManager.getWorkInfosByTag(tag)
    
    /**
     * Cancel all work
     */
    fun cancelAllWork() {
        workManager.cancelAllWork()
        logger.i(TAG, "Cancelled all scheduled work")
    }

    companion object {
        private const val TAG = "WorkManagerScheduler"
        private const val TAG_CLOUD_SYNC = "cloud_sync"
        private const val TAG_BACKUP = "backup"
        private const val TAG_ADAPTIVE_SYNC = "adaptive_sync"
        private const val WORK_NAME_CLOUD_SYNC = "cloud_sync_periodic"
        private const val WORK_NAME_BACKUP = "backup_periodic"
        private const val WORK_NAME_ADAPTIVE_SYNC = "adaptive_sync_periodic"
    }
}
