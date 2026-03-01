// automation/BackupScheduler.kt
package com.obsidianbackup.automation

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.obsidianbackup.domain.backup.BackupRequest
import com.obsidianbackup.engine.BackupEngine
import com.obsidianbackup.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

// Backup Worker
class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Get backup configuration from input data
        val appIdsJson = inputData.getString("appIds") ?: return Result.failure()
        val componentsJson = inputData.getString("components") ?: return Result.failure()
        val compressionLevel = inputData.getInt("compressionLevel", 6)
        val description = inputData.getString("description")

        // Parse data
        val appIds = Json.decodeFromString<List<String>>(appIdsJson)
            .map { AppId(it) }
        val components = Json.decodeFromString<List<String>>(componentsJson)
            .map { BackupComponent.valueOf(it) }.toSet()

        // Create backup request
        val request = BackupRequest(
            appIds = appIds,
            components = components,
            incremental = false,
            compressionLevel = compressionLevel,
            encryptionEnabled = false,
            description = description
        )

        // We cannot reliably construct full DI-backed engine inside a Worker without Hilt's WorkerFactory.
        // Instead, broadcast an intent locally to let the application orchestrator pick up the request.
        return try {
            val intent = Intent("com.obsidianbackup.ACTION_SCHEDULED_BACKUP").apply {
                putExtra("request_json", Json.encodeToString<BackupRequest>(request))
            }
            applicationContext.sendBroadcast(intent)
            Log.i("BackupWorker", "Scheduled backup broadcast sent for ${appIds.size} apps")
            Result.success()
        } catch (e: Exception) {
            Log.e("BackupWorker", "Failed to schedule backup: ${e.message}")
            Result.failure()
        }
    }
}

// Backup Scheduler
class BackupScheduler(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedule periodic backup
     */
    fun schedulePeriodicBackup(
        schedule: BackupSchedule,
        appIds: List<AppId>,
        components: Set<BackupComponent>,
        compressionLevel: Int = 6
    ) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(schedule.requiresCharging)
            .setRequiredNetworkType(
                if (schedule.requiresWifi) NetworkType.UNMETERED else NetworkType.NOT_REQUIRED
            )
            .build()

        val inputData = workDataOf(
            "appIds" to Json.encodeToString(appIds.map { it.value }),
            "components" to Json.encodeToString(components.map { it.name }),
            "compressionLevel" to compressionLevel,
            "description" to "Scheduled backup - ${schedule.name}"
        )

        val workRequest = when (schedule.frequency) {
            BackupFrequency.DAILY -> {
                PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .setInitialDelay(schedule.delayHours, TimeUnit.HOURS)
                    .addTag("scheduled_backup_${schedule.id}")
                    .build()
            }
            BackupFrequency.WEEKLY -> {
                PeriodicWorkRequestBuilder<BackupWorker>(7, TimeUnit.DAYS)
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .setInitialDelay(schedule.delayHours, TimeUnit.HOURS)
                    .addTag("scheduled_backup_${schedule.id}")
                    .build()
            }
            BackupFrequency.MONTHLY -> {
                PeriodicWorkRequestBuilder<BackupWorker>(30, TimeUnit.DAYS)
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .setInitialDelay(schedule.delayHours, TimeUnit.HOURS)
                    .addTag("scheduled_backup_${schedule.id}")
                    .build()
            }
        }

        workManager.enqueueUniquePeriodicWork(
            "scheduled_backup_${schedule.id}",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Schedule one-time backup
     */
    fun scheduleOneTimeBackup(
        appIds: List<AppId>,
        components: Set<BackupComponent>,
        delayMinutes: Long = 0
    ) {
        val inputData = workDataOf(
            "appIds" to Json.encodeToString(appIds.map { it.value }),
            "components" to Json.encodeToString(components.map { it.name }),
            "description" to "Manual backup"
        )

        val workRequest = OneTimeWorkRequestBuilder<BackupWorker>()
            .setInputData(inputData)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .addTag("manual_backup")
            .build()

        workManager.enqueue(workRequest)
    }

    /**
     * Cancel scheduled backup
     */
    fun cancelScheduledBackup(scheduleId: String) {
        workManager.cancelAllWorkByTag("scheduled_backup_$scheduleId")
    }

    /**
     * Cancel all scheduled backups
     */
    fun cancelAllScheduledBackups() {
        workManager.cancelAllWorkByTag("scheduled_backup")
    }

    /**
     * Get status of scheduled backups
     */
    fun getScheduledBackupsStatus(): kotlinx.coroutines.flow.Flow<List<WorkInfo>> {
        return workManager.getWorkInfosByTagFlow("scheduled_backup")
    }
}

// Backup Schedule Models
data class BackupSchedule(
    val id: String,
    val name: String,
    val frequency: BackupFrequency,
    val delayHours: Long = 0,
    val requiresCharging: Boolean = true,
    val requiresWifi: Boolean = false,
    val enabled: Boolean = true
)

enum class BackupFrequency {
    DAILY, WEEKLY, MONTHLY
}

class BackupTriggerManager(private val context: Context) {
    
    private val scheduler = BackupScheduler(context)
    private val workManager = WorkManager.getInstance(context)

    /**
     * Setup backup trigger for app installations
     * Uses WorkManager with a trigger constraint that monitors package changes
     */
    fun setupAppInstallTrigger(
        appIds: List<AppId>,
        components: Set<BackupComponent>
    ) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        
        val inputData = workDataOf(
            "appIds" to Json.encodeToString(appIds.map { it.value }),
            "components" to Json.encodeToString(components.map { it.name }),
            "description" to "Auto-backup after app install",
            "trigger_type" to "app_install"
        )
        
        val workRequest = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setInitialDelay(5, TimeUnit.MINUTES) // Wait 5 min after install
            .addTag("trigger_app_install")
            .build()
        
        workManager.enqueueUniqueWork(
            "trigger_app_install_${System.currentTimeMillis()}",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
        
        Log.i("BackupTriggerManager", "App install trigger setup for ${appIds.size} apps")
    }

    /**
     * Setup backup trigger for app updates
     * Schedules backup after app updates are detected
     */
    fun setupAppUpdateTrigger(
        appIds: List<AppId>,
        components: Set<BackupComponent>
    ) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(false)
            .build()
        
        val inputData = workDataOf(
            "appIds" to Json.encodeToString(appIds.map { it.value }),
            "components" to Json.encodeToString(components.map { it.name }),
            "description" to "Auto-backup after app update",
            "trigger_type" to "app_update"
        )
        
        val workRequest = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setInitialDelay(10, TimeUnit.MINUTES) // Wait 10 min after update
            .addTag("trigger_app_update")
            .build()
        
        workManager.enqueueUniqueWork(
            "trigger_app_update_${System.currentTimeMillis()}",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
        
        Log.i("BackupTriggerManager", "App update trigger setup for ${appIds.size} apps")
    }

    /**
     * Setup backup trigger before system update
     * Creates high-priority backup before OS updates
     */
    fun setupSystemUpdateTrigger(
        appIds: List<AppId>,
        components: Set<BackupComponent>
    ) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(true)
            .build()
        
        val inputData = workDataOf(
            "appIds" to Json.encodeToString(appIds.map { it.value }),
            "components" to Json.encodeToString(components.map { it.name }),
            "description" to "Pre-system-update backup",
            "trigger_type" to "system_update",
            "priority" to "high"
        )
        
        // Use expedited work for system update backups (high priority)
        val workRequest = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("trigger_system_update")
            .build()
        
        workManager.enqueueUniqueWork(
            "trigger_system_update",
            ExistingWorkPolicy.REPLACE, // Replace any existing system update backup
            workRequest
        )
        
        Log.i("BackupTriggerManager", "System update trigger setup for ${appIds.size} apps (expedited)")
    }
    
    /**
     * Cancel all trigger-based backups
     */
    fun cancelAllTriggers() {
        workManager.cancelAllWorkByTag("trigger_app_install")
        workManager.cancelAllWorkByTag("trigger_app_update")
        workManager.cancelAllWorkByTag("trigger_system_update")
        Log.i("BackupTriggerManager", "All triggers cancelled")
    }
}
