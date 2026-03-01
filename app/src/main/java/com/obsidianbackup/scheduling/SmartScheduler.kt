// scheduling/SmartScheduler.kt
package com.obsidianbackup.scheduling

import android.content.Context
import androidx.work.*
import com.obsidianbackup.data.repository.PreferencesRepository
import com.obsidianbackup.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Smart backup scheduler using heuristic-based prediction
 * Phase 1: Rule-based scheduling
 * Phase 2: ML-based prediction (future)
 */
@Singleton
class SmartScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager,
    private val preferencesRepository: PreferencesRepository
) {
    companion object {
        private const val SMART_BACKUP_WORK_TAG = "smart_backup_work"
        private const val SMART_BACKUP_WORK_NAME = "smart_backup_periodic"
    }
    
    /**
     * Schedule smart backup based on configuration
     */
    suspend fun scheduleSmartBackup(config: SmartSchedulingConfig) {
        if (!config.enabled) {
            cancelSmartBackup()
            return
        }
        
        val prediction = predictNextBackupTime(config)
        val delay = prediction.nextBackupTime - System.currentTimeMillis()
        
        if (delay < 0) {
            // Time is in the past, schedule for next occurrence
            scheduleNextOccurrence(config)
            return
        }
        
        val constraints = buildConstraints(config)
        
        val workRequest = OneTimeWorkRequestBuilder<SmartBackupWorker>()
            .setConstraints(constraints)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(SMART_BACKUP_WORK_TAG)
            .setInputData(
                workDataOf(
                    "profile_id" to config.profileId,
                    "reason" to prediction.reason
                )
            )
            .build()
        
        workManager.enqueueUniqueWork(
            SMART_BACKUP_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    /**
     * Predict next optimal backup time using heuristics
     */
    fun predictNextBackupTime(config: SmartSchedulingConfig): SchedulePrediction {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        val nextBackupTime = when (config.preferredTimeWindow) {
            TimeWindow.AUTO -> predictAutoTime(config, calendar)
            else -> calculateFixedTimeWindow(config.preferredTimeWindow, calendar)
        }
        
        return SchedulePrediction(
            nextBackupTime = nextBackupTime,
            confidence = if (config.preferredTimeWindow == TimeWindow.AUTO) 0.75f else 1.0f,
            reason = generateReason(config.preferredTimeWindow, calendar),
            constraints = ScheduleConstraints(
                requiresWifi = config.onlyOnWifi,
                requiresCharging = config.onlyWhenCharging,
                minimumBattery = config.minimumBatteryLevel
            )
        )
    }
    
    /**
     * Phase 1: Heuristic-based prediction
     * Predicts optimal time based on:
     * - Preferred night time (3-5 AM typical idle period)
     * - Device charging patterns
     * - Avoiding peak usage hours
     */
    private fun predictAutoTime(config: SmartSchedulingConfig, calendar: Calendar): Long {
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        
        // Target: 3 AM (statistically lowest device usage)
        val targetHour = 3
        
        calendar.set(Calendar.HOUR_OF_DAY, targetHour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // If 3 AM today has passed, schedule for tomorrow
        if (currentHour >= targetHour) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return calendar.timeInMillis
    }
    
    /**
     * Calculate backup time for fixed time window
     */
    private fun calculateFixedTimeWindow(timeWindow: TimeWindow, calendar: Calendar): Long {
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val targetHour = timeWindow.startHour
        
        calendar.set(Calendar.HOUR_OF_DAY, targetHour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // Handle night time window (crosses midnight)
        if (timeWindow == TimeWindow.NIGHT) {
            if (currentHour < 6) {
                // It's early morning, schedule for tonight
                if (currentHour < targetHour - 24) {
                    // Current time is before target (e.g., 2 AM < 11 PM)
                    calendar.add(Calendar.DAY_OF_YEAR, 0) // Today
                }
            } else {
                // It's daytime, schedule for tonight
                calendar.add(Calendar.DAY_OF_YEAR, 0)
            }
        } else {
            // For other windows, if target hour has passed, schedule for tomorrow
            if (currentHour >= targetHour) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        return calendar.timeInMillis
    }
    
    /**
     * Schedule next occurrence based on frequency
     */
    private suspend fun scheduleNextOccurrence(config: SmartSchedulingConfig) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR, config.backupFrequency.intervalHours.toInt())
        
        val newConfig = config.copy()
        scheduleSmartBackup(newConfig)
    }
    
    /**
     * Build WorkManager constraints from config
     */
    private fun buildConstraints(config: SmartSchedulingConfig): Constraints {
        return Constraints.Builder()
            .apply {
                if (config.onlyOnWifi) {
                    setRequiredNetworkType(NetworkType.UNMETERED)
                }
                if (config.onlyWhenCharging) {
                    setRequiresCharging(true)
                }
                if (config.minimumBatteryLevel > 0) {
                    setRequiresBatteryNotLow(true)
                }
                // Require device idle for best performance
                setRequiresDeviceIdle(true)
            }
            .build()
    }
    
    /**
     * Generate human-readable reason for backup time
     */
    private fun generateReason(timeWindow: TimeWindow, calendar: Calendar): String {
        return when (timeWindow) {
            TimeWindow.AUTO -> "AI predicted: 3 AM during typical idle period"
            TimeWindow.NIGHT -> "Night time backup when device is idle"
            TimeWindow.MORNING -> "Morning backup before daily activities"
            TimeWindow.AFTERNOON -> "Afternoon backup during low usage"
            TimeWindow.EVENING -> "Evening backup after work hours"
        }
    }
    
    /**
     * Cancel smart backup scheduling
     */
    fun cancelSmartBackup() {
        workManager.cancelUniqueWork(SMART_BACKUP_WORK_NAME)
        workManager.cancelAllWorkByTag(SMART_BACKUP_WORK_TAG)
    }
    
    /**
     * Get next scheduled backup time
     */
    suspend fun getNextScheduledBackup(): SchedulePrediction? {
        val config = preferencesRepository.getSmartSchedulingConfig().first()
        return if (config.enabled) {
            predictNextBackupTime(config)
        } else {
            null
        }
    }
    
    /**
     * Record backup event for future ML training (Phase 2)
     */
    suspend fun recordBackupEvent(event: BackupEvent) {
        // Phase 2: Store in Room database for ML training
        // For now, just log
        android.util.Log.d("SmartScheduler", "Backup event recorded: $event")
    }
}
