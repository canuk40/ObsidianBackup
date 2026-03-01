// model/SmartSchedulingModels.kt
package com.obsidianbackup.model

import java.time.LocalTime
import java.time.Duration

/**
 * Configuration for ML-based smart backup scheduling
 */
data class SmartSchedulingConfig(
    val enabled: Boolean = false,
    val preferredTimeWindow: TimeWindow = TimeWindow.AUTO,
    val onlyOnWifi: Boolean = true,
    val onlyWhenCharging: Boolean = true,
    val minimumBatteryLevel: Int = 50,
    val backupFrequency: BackupFrequency = BackupFrequency.DAILY,
    val profileId: String? = null // Which profile to auto-backup
)

/**
 * Time windows for smart scheduling
 */
enum class TimeWindow(val displayName: String, val startHour: Int, val endHour: Int) {
    NIGHT("Night (11 PM - 6 AM)", 23, 6),
    MORNING("Morning (6 AM - 12 PM)", 6, 12),
    AFTERNOON("Afternoon (12 PM - 6 PM)", 12, 18),
    EVENING("Evening (6 PM - 11 PM)", 18, 23),
    AUTO("Auto (AI-predicted)", 3, 5); // Default to 3-5 AM for auto
    
    fun getStartTime(): LocalTime = LocalTime.of(startHour, 0)
    fun getEndTime(): LocalTime = LocalTime.of(endHour, 0)
}

/**
 * Backup frequency options
 */
enum class BackupFrequency(val displayName: String, val intervalHours: Long) {
    HOURLY("Hourly", 1),
    EVERY_6_HOURS("Every 6 hours", 6),
    EVERY_12_HOURS("Every 12 hours", 12),
    DAILY("Daily", 24),
    WEEKLY("Weekly", 168),
    MONTHLY("Monthly", 720);
    
    fun getDuration(): Duration = Duration.ofHours(intervalHours)
}

/**
 * Result of smart scheduling prediction
 */
data class SchedulePrediction(
    val nextBackupTime: Long, // Unix timestamp in milliseconds
    val confidence: Float, // 0.0 to 1.0
    val reason: String, // Human-readable explanation
    val constraints: ScheduleConstraints
)

/**
 * Constraints for backup scheduling
 */
data class ScheduleConstraints(
    val requiresWifi: Boolean,
    val requiresCharging: Boolean,
    val minimumBattery: Int,
    val avoidPeakUsage: Boolean = true
)

/**
 * Device state snapshot for ML prediction
 */
data class DeviceState(
    val timestamp: Long,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val isOnWifi: Boolean,
    val screenOn: Boolean,
    val hourOfDay: Int,
    val dayOfWeek: Int
)

/**
 * Historical backup event for learning
 */
data class BackupEvent(
    val timestamp: Long,
    val duration: Duration,
    val success: Boolean,
    val deviceState: DeviceState,
    val appCount: Int,
    val dataSize: Long
)
