// ml/analytics/BackupAnalytics.kt
package com.obsidianbackup.ml.analytics

import android.content.Context
import android.util.Log
import com.obsidianbackup.ml.BackupContext
import com.obsidianbackup.ml.prediction.FileActivityEvent
import com.obsidianbackup.model.AppId
import com.obsidianbackup.model.BackupComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime

/**
 * Analytics engine for tracking backup events and file activity
 * Provides data for ML model training and anomaly detection
 */
class BackupAnalytics(private val context: Context) {
    
    private val events = mutableListOf<BackupEvent>()
    private val fileActivity = mutableListOf<FileActivityRecord>()
    
    private val analyticsFile: File
        get() = File(context.filesDir, "ml_models/analytics.json")
    
    companion object {
        private const val TAG = "BackupAnalytics"
        private const val MAX_EVENTS = 500
        private const val MAX_FILE_ACTIVITY = 1000
    }
    
    /**
     * Record a backup event
     */
    suspend fun recordBackupEvent(
        appIds: List<AppId>,
        timestamp: LocalDateTime,
        components: Set<BackupComponent>,
        context: BackupContext,
        success: Boolean = true,
        durationMs: Long = 0,
        sizeMb: Long = 0
    ) = withContext(Dispatchers.IO) {
        val event = BackupEvent(
            id = generateEventId(),
            appIds = appIds,
            timestamp = timestamp,
            components = components.toList(),
            context = BackupContextSnapshot(
                batteryLevel = context.batteryLevel,
                isCharging = context.isCharging,
                isWifiConnected = context.isWifiConnected,
                locationCategory = context.locationCategory.name,
                activityType = context.activityType.name
            ),
            success = success,
            durationMs = durationMs,
            sizeMb = sizeMb
        )
        
        events.add(event)
        
        // Keep only recent events
        if (events.size > MAX_EVENTS) {
            events.removeAt(0)
        }
        
        save()
        
        Log.d(TAG, "Recorded backup event: ${appIds.size} apps, ${sizeMb}MB, ${durationMs}ms")
    }
    
    /**
     * Record file activity for anomaly detection
     */
    suspend fun recordFileActivity(
        appId: AppId,
        changeCount: Long,
        timestamp: LocalDateTime = LocalDateTime.now()
    ) = withContext(Dispatchers.IO) {
        val record = FileActivityRecord(
            appId = appId.value,
            changeCount = changeCount,
            timestamp = timestamp
        )
        
        fileActivity.add(record)
        
        // Keep only recent activity
        if (fileActivity.size > MAX_FILE_ACTIVITY) {
            fileActivity.removeAt(0)
        }
        
        save()
        
        Log.d(TAG, "Recorded file activity: ${appId.value}, $changeCount changes")
    }
    
    /**
     * Get recent file activity for anomaly detection
     */
    suspend fun getRecentFileActivity(
        hours: Int = 24
    ): List<FileActivityEvent> = withContext(Dispatchers.IO) {
        val cutoff = LocalDateTime.now().minusHours(hours.toLong())
        
        fileActivity
            .filter { it.timestamp.isAfter(cutoff) }
            .map { record ->
                FileActivityEvent(
                    appId = AppId(record.appId),
                    changeCount = record.changeCount,
                    timestamp = record.timestamp
                )
            }
    }
    
    /**
     * Get backup statistics
     */
    suspend fun getStatistics(): BackupStatistics = withContext(Dispatchers.Default) {
        val successfulBackups = events.count { it.success }
        val totalBackups = events.size
        val successRate = if (totalBackups > 0) {
            successfulBackups.toFloat() / totalBackups
        } else 0f
        
        val avgDuration = if (events.isNotEmpty()) {
            events.map { it.durationMs }.average().toLong()
        } else 0L
        
        val avgSize = if (events.isNotEmpty()) {
            events.map { it.sizeMb }.average().toLong()
        } else 0L
        
        val totalSize = events.sumOf { it.sizeMb }
        
        // Find most backed up apps
        val appFrequency = mutableMapOf<String, Int>()
        events.forEach { event ->
            event.appIds.forEach { appId ->
                appFrequency[appId.value] = (appFrequency[appId.value] ?: 0) + 1
            }
        }
        
        val topApps = appFrequency.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { AppBackupFrequency(AppId(it.key), it.value) }
        
        // Find peak backup times
        val hourFrequency = events.groupingBy { it.timestamp.hour }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { PeakTime(it.key, it.value) }
        
        BackupStatistics(
            totalBackups = totalBackups,
            successfulBackups = successfulBackups,
            successRate = successRate,
            avgDurationMs = avgDuration,
            avgSizeMb = avgSize,
            totalSizeMb = totalSize,
            topApps = topApps,
            peakTimes = hourFrequency
        )
    }
    
    /**
     * Get backup frequency analysis
     */
    suspend fun getFrequencyAnalysis(): FrequencyAnalysis = withContext(Dispatchers.Default) {
        if (events.isEmpty()) {
            return@withContext FrequencyAnalysis(
                dailyAverage = 0f,
                weeklyAverage = 0f,
                monthlyAverage = 0f
            )
        }
        
        val now = LocalDateTime.now()
        val oneDayAgo = now.minusDays(1)
        val oneWeekAgo = now.minusWeeks(1)
        val oneMonthAgo = now.minusMonths(1)
        
        val lastDay = events.count { it.timestamp.isAfter(oneDayAgo) }
        val lastWeek = events.count { it.timestamp.isAfter(oneWeekAgo) }
        val lastMonth = events.count { it.timestamp.isAfter(oneMonthAgo) }
        
        FrequencyAnalysis(
            dailyAverage = lastDay.toFloat(),
            weeklyAverage = (lastWeek / 7f),
            monthlyAverage = (lastMonth / 30f)
        )
    }
    
    /**
     * Get context analysis to understand backup patterns
     */
    suspend fun getContextAnalysis(): ContextAnalysis = withContext(Dispatchers.Default) {
        if (events.isEmpty()) {
            return@withContext ContextAnalysis(
                preferredChargingState = 0.5f,
                preferredWifiState = 0.5f,
                avgBatteryLevel = 50f,
                commonLocations = emptyMap(),
                commonActivities = emptyMap()
            )
        }
        
        val chargingRatio = events.count { it.context.isCharging }.toFloat() / events.size
        val wifiRatio = events.count { it.context.isWifiConnected }.toFloat() / events.size
        val avgBattery = events.map { it.context.batteryLevel }.average().toFloat()
        
        val locationFreq = events.groupingBy { it.context.locationCategory }
            .eachCount()
            .mapValues { it.value.toFloat() / events.size }
        
        val activityFreq = events.groupingBy { it.context.activityType }
            .eachCount()
            .mapValues { it.value.toFloat() / events.size }
        
        ContextAnalysis(
            preferredChargingState = chargingRatio,
            preferredWifiState = wifiRatio,
            avgBatteryLevel = avgBattery,
            commonLocations = locationFreq,
            commonActivities = activityFreq
        )
    }
    
    /**
     * Generate event ID
     */
    private fun generateEventId(): String {
        return "event_${System.currentTimeMillis()}_${(0..999).random()}"
    }
    
    /**
     * Save analytics data to disk
     */
    private suspend fun save() = withContext(Dispatchers.IO) {
        try {
            analyticsFile.parentFile?.mkdirs()
            val data = AnalyticsData(
                events = events,
                fileActivity = fileActivity
            )
            val json = Json.encodeToString(data)
            analyticsFile.writeText(json)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save analytics: ${e.message}", e)
        }
    }
    
    /**
     * Load analytics data from disk
     */
    suspend fun load() = withContext(Dispatchers.IO) {
        try {
            if (analyticsFile.exists()) {
                val json = analyticsFile.readText()
                val data = Json.decodeFromString<AnalyticsData>(json)
                events.clear()
                events.addAll(data.events)
                fileActivity.clear()
                fileActivity.addAll(data.fileActivity)
                Log.i(TAG, "Loaded ${events.size} events and ${fileActivity.size} activity records")
            } else {
                Log.d(TAG, "No analytics file found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load analytics: ${e.message}", e)
        }
    }
    
    /**
     * Reset analytics data
     */
    suspend fun reset() = withContext(Dispatchers.IO) {
        events.clear()
        fileActivity.clear()
        analyticsFile.delete()
        Log.w(TAG, "Analytics reset")
    }
    
    /**
     * Export analytics for analysis
     */
    suspend fun export(): String = withContext(Dispatchers.IO) {
        val data = AnalyticsData(
            events = events,
            fileActivity = fileActivity
        )
        Json.encodeToString(data)
    }
}

@Serializable
private data class AnalyticsData(
    val events: List<BackupEvent>,
    val fileActivity: List<FileActivityRecord>
)

@Serializable
data class BackupEvent(
    val id: String,
    val appIds: List<AppId>,
    @Serializable(with = com.obsidianbackup.ml.models.LocalDateTimeSerializer::class)
    val timestamp: LocalDateTime,
    val components: List<BackupComponent>,
    val context: BackupContextSnapshot,
    val success: Boolean,
    val durationMs: Long,
    val sizeMb: Long
)

@Serializable
data class BackupContextSnapshot(
    val batteryLevel: Float,
    val isCharging: Boolean,
    val isWifiConnected: Boolean,
    val locationCategory: String,
    val activityType: String
)

@Serializable
data class FileActivityRecord(
    val appId: String,
    val changeCount: Long,
    @Serializable(with = com.obsidianbackup.ml.models.LocalDateTimeSerializer::class)
    val timestamp: LocalDateTime
)

data class BackupStatistics(
    val totalBackups: Int,
    val successfulBackups: Int,
    val successRate: Float,
    val avgDurationMs: Long,
    val avgSizeMb: Long,
    val totalSizeMb: Long,
    val topApps: List<AppBackupFrequency>,
    val peakTimes: List<PeakTime>
)

data class AppBackupFrequency(
    val appId: AppId,
    val count: Int
)

data class PeakTime(
    val hour: Int,
    val count: Int
)

data class FrequencyAnalysis(
    val dailyAverage: Float,
    val weeklyAverage: Float,
    val monthlyAverage: Float
)

data class ContextAnalysis(
    val preferredChargingState: Float,
    val preferredWifiState: Float,
    val avgBatteryLevel: Float,
    val commonLocations: Map<String, Float>,
    val commonActivities: Map<String, Float>
)
