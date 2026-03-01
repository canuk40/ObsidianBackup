package com.obsidianbackup.automation

import android.content.Context
import androidx.work.*
import com.obsidianbackup.data.repository.Schedule
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.work.ScheduledBackupWorker
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleManager @Inject constructor(
    private val context: Context,
    private val logger: ObsidianLogger
) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleBackup(schedule: Schedule) {
        logger.i(TAG, "Scheduling backup: ${schedule.name} (${schedule.frequency})")

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .apply {
                if (schedule.requiresCharging) {
                    setRequiresCharging(true)
                }
                if (schedule.requiresWifi) {
                    setRequiredNetworkType(NetworkType.UNMETERED)
                } else {
                    setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                }
            }
            .build()

        val inputData = workDataOf(
            ScheduledBackupWorker.KEY_SCHEDULE_ID to schedule.id,
            ScheduledBackupWorker.KEY_APP_IDS to Json.encodeToString(schedule.appIds.map { it.value }),
            ScheduledBackupWorker.KEY_COMPONENTS to Json.encodeToString(schedule.components.map { it.name }),
            ScheduledBackupWorker.KEY_SCHEDULE_NAME to schedule.name
        )

        val initialDelay = calculateInitialDelay(schedule)
        val repeatInterval = when (schedule.frequency) {
            BackupFrequency.DAILY -> 1L to TimeUnit.DAYS
            BackupFrequency.WEEKLY -> 7L to TimeUnit.DAYS
            BackupFrequency.MONTHLY -> 30L to TimeUnit.DAYS
        }

        val workRequest = PeriodicWorkRequestBuilder<ScheduledBackupWorker>(
            repeatInterval.first,
            repeatInterval.second
        )
            .setConstraints(constraints)
            .setInputData(inputData)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(TAG_SCHEDULED_BACKUP)
            .addTag("schedule_${schedule.id}")
            .build()

        workManager.enqueueUniquePeriodicWork(
            getWorkName(schedule.id),
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )

        logger.i(TAG, "Scheduled backup enqueued: ${schedule.name}, next run in ${initialDelay}ms")
    }

    fun cancelSchedule(scheduleId: String) {
        logger.i(TAG, "Cancelling schedule: $scheduleId")
        workManager.cancelUniqueWork(getWorkName(scheduleId))
    }

    fun cancelAllSchedules() {
        logger.i(TAG, "Cancelling all schedules")
        workManager.cancelAllWorkByTag(TAG_SCHEDULED_BACKUP)
    }

    fun getScheduleStatus(scheduleId: String) = 
        workManager.getWorkInfosForUniqueWork(getWorkName(scheduleId))

    private fun calculateInitialDelay(schedule: Schedule): Long {
        val now = System.currentTimeMillis()
        val nextRun = schedule.nextRun ?: return 0L
        return (nextRun - now).coerceAtLeast(0L)
    }

    private fun getWorkName(scheduleId: String) = "scheduled_backup_$scheduleId"

    /**
     * Called by BootReceiver after device reboot.
     * WorkManager persists periodic work across reboots automatically,
     * so this primarily ensures WorkManager is initialized and logs the event.
     */
    fun rescheduleAllOnBoot() {
        logger.i(TAG, "Boot completed — WorkManager periodic work is auto-persisted")
        // WorkManager periodic work requests survive reboots automatically.
        // This hook exists for any future one-shot or alarm-based schedules
        // that need re-registration (e.g., AlarmManager exact alarms on Android 12+).
    }

    companion object {
        private const val TAG = "ScheduleManager"
        private const val TAG_SCHEDULED_BACKUP = "scheduled_backup"
    }
}
