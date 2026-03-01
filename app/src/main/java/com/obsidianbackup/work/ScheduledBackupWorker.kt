package com.obsidianbackup.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.obsidianbackup.data.repository.ScheduleRepository
import com.obsidianbackup.domain.usecase.BackupAppsUseCase
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.model.AppId
import com.obsidianbackup.model.BackupComponent
import com.obsidianbackup.model.BackupResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@HiltWorker
class ScheduledBackupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val backupAppsUseCase: BackupAppsUseCase,
    private val scheduleRepository: ScheduleRepository,
    private val logger: ObsidianLogger
) : CoroutineWorker(context, params) {

    private val notificationManager = 
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val scheduleId = inputData.getString(KEY_SCHEDULE_ID) ?: return@withContext Result.failure()
            val appIdsJson = inputData.getString(KEY_APP_IDS) ?: return@withContext Result.failure()
            val componentsJson = inputData.getString(KEY_COMPONENTS) ?: return@withContext Result.failure()
            val scheduleName = inputData.getString(KEY_SCHEDULE_NAME) ?: "Scheduled Backup"

            logger.i(TAG, "Starting scheduled backup: $scheduleName (ID: $scheduleId)")

            // Show foreground notification
            setForeground(createForegroundInfo(scheduleName))

            // Parse data
            val appIds = Json.decodeFromString<List<String>>(appIdsJson).map { AppId(it) }
            val components = Json.decodeFromString<List<String>>(componentsJson)
                .mapNotNull { 
                    try { 
                        BackupComponent.valueOf(it) 
                    } catch (e: Exception) { 
                        null 
                    } 
                }
                .toSet()

            // Create backup request
            val request = com.obsidianbackup.model.BackupRequest(
                appIds = appIds,
                components = components,
                incremental = false,
                compressionLevel = 6,
                encryptionEnabled = false,
                description = "Scheduled: $scheduleName"
            )

            // Execute backup
            val backupResult = backupAppsUseCase(request)

            // Update schedule run times
            val now = System.currentTimeMillis()
            val schedule = scheduleRepository.getSchedule(scheduleId)
            if (schedule != null) {
                val nextRun = calculateNextRun(schedule)
                scheduleRepository.updateScheduleRunTimes(scheduleId, now, nextRun)
            }

            // Show completion notification
            when (backupResult) {
                is BackupResult.Success -> {
                    showCompletionNotification(
                        scheduleName,
                        "Successfully backed up ${backupResult.appsBackedUp.size} apps",
                        true
                    )
                    logger.i(TAG, "Scheduled backup completed successfully: $scheduleId")
                    Result.success()
                }
                is BackupResult.PartialSuccess -> {
                    showCompletionNotification(
                        scheduleName,
                        "Backed up ${backupResult.appsBackedUp.size} apps, ${backupResult.appsFailed.size} failed",
                        false
                    )
                    logger.w(TAG, "Scheduled backup partially completed: $scheduleId")
                    Result.success()
                }
                is BackupResult.Failure -> {
                    showCompletionNotification(
                        scheduleName,
                        "Backup failed: ${backupResult.reason}",
                        false
                    )
                    logger.e(TAG, "Scheduled backup failed: $scheduleId - ${backupResult.reason}")
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            logger.e(TAG, "Scheduled backup worker error: ${e.message}", e)
            showCompletionNotification("Scheduled Backup", "Error: ${e.message}", false)
            Result.failure()
        }
    }

    private fun createForegroundInfo(scheduleName: String): ForegroundInfo {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Running Scheduled Backup")
            .setContentText(scheduleName)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID_PROGRESS, notification)
    }

    private fun showCompletionNotification(title: String, message: String, success: Boolean) {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(if (success) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_COMPLETE, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Scheduled Backups",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for scheduled backup operations"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun calculateNextRun(schedule: com.obsidianbackup.data.repository.Schedule): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(java.util.Calendar.HOUR_OF_DAY, schedule.hour)
            set(java.util.Calendar.MINUTE, schedule.minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)

            when (schedule.frequency) {
                com.obsidianbackup.automation.BackupFrequency.DAILY -> 
                    add(java.util.Calendar.DAY_OF_MONTH, 1)
                com.obsidianbackup.automation.BackupFrequency.WEEKLY -> 
                    add(java.util.Calendar.WEEK_OF_YEAR, 1)
                com.obsidianbackup.automation.BackupFrequency.MONTHLY -> 
                    add(java.util.Calendar.MONTH, 1)
            }
        }
        return calendar.timeInMillis
    }

    companion object {
        private const val TAG = "ScheduledBackupWorker"
        private const val CHANNEL_ID = "scheduled_backup_channel"
        private const val NOTIFICATION_ID_PROGRESS = 2001
        private const val NOTIFICATION_ID_COMPLETE = 2002

        const val KEY_SCHEDULE_ID = "schedule_id"
        const val KEY_APP_IDS = "app_ids"
        const val KEY_COMPONENTS = "components"
        const val KEY_SCHEDULE_NAME = "schedule_name"
    }
}
