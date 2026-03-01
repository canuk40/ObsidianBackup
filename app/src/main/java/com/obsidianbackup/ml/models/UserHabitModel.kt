// ml/models/UserHabitModel.kt
package com.obsidianbackup.ml.models

import android.content.Context
import android.util.Log
import com.obsidianbackup.ml.BackupContext
import com.obsidianbackup.model.AppId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * ML model for learning and recognizing user backup habits
 * Uses on-device pattern recognition without cloud dependencies
 */
class UserHabitModel(private val context: Context) {
    private val samples = mutableListOf<BackupSample>()
    private val patterns = mutableListOf<BackupPattern>()
    private var lastTrainingTime: LocalDateTime? = null
    
    private val modelFile: File
        get() = File(context.filesDir, "ml_models/user_habits.json")
    
    companion object {
        private const val TAG = "UserHabitModel"
        private const val MAX_SAMPLES = 1000
        private const val MIN_PATTERN_OCCURRENCES = 3
        private const val PATTERN_SIMILARITY_THRESHOLD = 0.8f
    }
    
    /**
     * Add a new backup sample for learning
     */
    suspend fun addSample(
        dayOfWeek: DayOfWeek,
        timeOfDay: LocalTime,
        appIds: List<AppId>,
        context: BackupContext
    ) = withContext(Dispatchers.IO) {
        val sample = BackupSample(
            timestamp = LocalDateTime.now(),
            dayOfWeek = dayOfWeek,
            timeOfDay = timeOfDay,
            appIds = appIds,
            batteryLevel = context.batteryLevel,
            isCharging = context.isCharging,
            isWifiConnected = context.isWifiConnected,
            locationCategory = context.locationCategory,
            activityType = context.activityType
        )
        
        samples.add(sample)
        
        // Keep only recent samples
        if (samples.size > MAX_SAMPLES) {
            samples.removeAt(0)
        }
        
        // Trigger pattern extraction if enough samples
        if (samples.size >= MIN_PATTERN_OCCURRENCES) {
            extractPatterns()
        }
        
        save()
        Log.d(TAG, "Added sample: ${sample.timestamp}, total samples: ${samples.size}")
    }
    
    /**
     * Extract patterns from collected samples using clustering
     */
    private suspend fun extractPatterns() = withContext(Dispatchers.Default) {
        patterns.clear()
        
        // Group samples by day of week and time window (2-hour buckets)
        val groupedSamples = samples.groupBy { sample ->
            val hourBucket = sample.timeOfDay.hour / 2
            "${sample.dayOfWeek.value}_$hourBucket"
        }
        
        // Find patterns that occur frequently
        groupedSamples.forEach { (key, groupSamples) ->
            if (groupSamples.size >= MIN_PATTERN_OCCURRENCES) {
                val pattern = analyzePattern(groupSamples)
                if (pattern.confidence >= PATTERN_SIMILARITY_THRESHOLD) {
                    patterns.add(pattern)
                }
            }
        }
        
        lastTrainingTime = LocalDateTime.now()
        Log.i(TAG, "Extracted ${patterns.size} patterns from ${samples.size} samples")
    }
    
    /**
     * Analyze a group of samples to extract common pattern
     */
    private fun analyzePattern(samples: List<BackupSample>): BackupPattern {
        val avgTime = averageTime(samples.map { it.timeOfDay })
        val commonDayOfWeek = samples.groupingBy { it.dayOfWeek }
            .eachCount()
            .maxByOrNull { it.value }?.key ?: DayOfWeek.MONDAY
        
        // Find most common apps
        val appFrequency = mutableMapOf<AppId, Int>()
        samples.forEach { sample ->
            sample.appIds.forEach { appId ->
                appFrequency[appId] = (appFrequency[appId] ?: 0) + 1
            }
        }
        
        val commonApps = appFrequency
            .filter { it.value >= samples.size / 2 } // Apps in at least 50% of samples
            .keys.toList()
        
        // Calculate pattern confidence based on consistency
        val confidence = calculatePatternConfidence(samples)
        
        // Determine common context
        val avgBatteryLevel = samples.map { it.batteryLevel }.average().toFloat()
        val chargingRatio = samples.count { it.isCharging }.toFloat() / samples.size
        val wifiRatio = samples.count { it.isWifiConnected }.toFloat() / samples.size
        
        val commonLocation = samples.groupingBy { it.locationCategory }
            .eachCount()
            .maxByOrNull { it.value }?.key
        
        val commonActivity = samples.groupingBy { it.activityType }
            .eachCount()
            .maxByOrNull { it.value }?.key
        
        return BackupPattern(
            id = "${commonDayOfWeek.value}_${avgTime.hour}",
            dayOfWeek = commonDayOfWeek,
            timeOfDay = avgTime,
            appIds = commonApps,
            confidence = confidence,
            occurrences = samples.size,
            avgBatteryLevel = avgBatteryLevel,
            preferredCharging = chargingRatio > 0.5f,
            preferredWifi = wifiRatio > 0.5f,
            commonLocation = commonLocation,
            commonActivity = commonActivity
        )
    }
    
    /**
     * Calculate confidence score for a pattern based on sample consistency
     */
    private fun calculatePatternConfidence(samples: List<BackupSample>): Float {
        if (samples.size < 2) return 0f
        
        var totalSimilarity = 0f
        var comparisons = 0
        
        // Compare each sample with every other sample
        for (i in samples.indices) {
            for (j in i + 1 until samples.size) {
                totalSimilarity += calculateSampleSimilarity(samples[i], samples[j])
                comparisons++
            }
        }
        
        return if (comparisons > 0) totalSimilarity / comparisons else 0f
    }
    
    /**
     * Calculate similarity between two backup samples
     */
    private fun calculateSampleSimilarity(s1: BackupSample, s2: BackupSample): Float {
        var similarity = 0f
        var weights = 0f
        
        // Time similarity (within 2 hours)
        val timeDiff = kotlin.math.abs(s1.timeOfDay.hour - s2.timeOfDay.hour)
        similarity += (1f - (timeDiff / 24f)) * 3f
        weights += 3f
        
        // Day of week match
        if (s1.dayOfWeek == s2.dayOfWeek) {
            similarity += 2f
        }
        weights += 2f
        
        // App overlap
        val commonApps = s1.appIds.intersect(s2.appIds.toSet())
        val totalApps = s1.appIds.union(s2.appIds.toSet())
        if (totalApps.isNotEmpty()) {
            similarity += (commonApps.size.toFloat() / totalApps.size) * 2f
        }
        weights += 2f
        
        // Context similarity
        if (s1.isCharging == s2.isCharging) similarity += 1f
        weights += 1f
        
        if (s1.isWifiConnected == s2.isWifiConnected) similarity += 1f
        weights += 1f
        
        if (s1.locationCategory == s2.locationCategory) similarity += 1f
        weights += 1f
        
        return similarity / weights
    }
    
    /**
     * Calculate average time from list of times
     */
    private fun averageTime(times: List<LocalTime>): LocalTime {
        val avgMinutes = times.map { it.hour * 60 + it.minute }.average().toInt()
        return LocalTime.of(avgMinutes / 60, avgMinutes % 60)
    }
    
    /**
     * Get all learned patterns
     */
    fun getPatterns(): List<BackupPattern> = patterns.toList()
    
    /**
     * Get total number of samples
     */
    fun getSampleCount(): Int = samples.size
    
    /**
     * Get last training time
     */
    fun getLastTrainingTime(): LocalDateTime? = lastTrainingTime
    
    /**
     * Load model from disk
     */
    suspend fun load() = withContext(Dispatchers.IO) {
        try {
            if (modelFile.exists()) {
                val json = modelFile.readText()
                val data = Json.decodeFromString<UserHabitModelData>(json)
                samples.clear()
                samples.addAll(data.samples)
                patterns.clear()
                patterns.addAll(data.patterns)
                lastTrainingTime = data.lastTrainingTime
                Log.i(TAG, "Loaded model: ${samples.size} samples, ${patterns.size} patterns")
            } else {
                Log.d(TAG, "No model file found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model: ${e.message}", e)
        }
    }
    
    /**
     * Save model to disk
     */
    suspend fun save() = withContext(Dispatchers.IO) {
        try {
            modelFile.parentFile?.mkdirs()
            val data = UserHabitModelData(
                samples = samples,
                patterns = patterns,
                lastTrainingTime = lastTrainingTime
            )
            val json = Json.encodeToString(data)
            modelFile.writeText(json)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save model: ${e.message}", e)
        }
    }
    
    /**
     * Reset model
     */
    suspend fun reset() = withContext(Dispatchers.IO) {
        samples.clear()
        patterns.clear()
        lastTrainingTime = null
        modelFile.delete()
        Log.w(TAG, "Model reset")
    }
    
    /**
     * Export model as byte array
     */
    suspend fun export(): ByteArray = withContext(Dispatchers.IO) {
        val data = UserHabitModelData(
            samples = samples,
            patterns = patterns,
            lastTrainingTime = lastTrainingTime
        )
        Json.encodeToString(data).toByteArray()
    }
    
    /**
     * Import model from byte array
     */
    suspend fun import(data: ByteArray) = withContext(Dispatchers.IO) {
        try {
            val json = data.decodeToString()
            val modelData = Json.decodeFromString<UserHabitModelData>(json)
            samples.clear()
            samples.addAll(modelData.samples)
            patterns.clear()
            patterns.addAll(modelData.patterns)
            lastTrainingTime = modelData.lastTrainingTime
            save()
            Log.i(TAG, "Model imported successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import model: ${e.message}", e)
        }
    }
}

@Serializable
data class BackupSample(
    @Serializable(with = LocalDateTimeSerializer::class)
    val timestamp: LocalDateTime,
    val dayOfWeek: DayOfWeek,
    @Serializable(with = LocalTimeSerializer::class)
    val timeOfDay: LocalTime,
    val appIds: List<AppId>,
    val batteryLevel: Float,
    val isCharging: Boolean,
    val isWifiConnected: Boolean,
    val locationCategory: com.obsidianbackup.ml.LocationCategory,
    val activityType: com.obsidianbackup.ml.ActivityType
)

@Serializable
data class BackupPattern(
    val id: String,
    val dayOfWeek: DayOfWeek,
    @Serializable(with = LocalTimeSerializer::class)
    val timeOfDay: LocalTime,
    val appIds: List<AppId>,
    val confidence: Float,
    val occurrences: Int,
    val avgBatteryLevel: Float,
    val preferredCharging: Boolean,
    val preferredWifi: Boolean,
    val commonLocation: com.obsidianbackup.ml.LocationCategory?,
    val commonActivity: com.obsidianbackup.ml.ActivityType?
) {
    /**
     * Check if current context matches this pattern
     */
    fun matches(context: BackupContext): Boolean {
        var matchScore = 0f
        
        // Time match (within 2 hours)
        val timeDiff = kotlin.math.abs(context.timeOfDay.hour - timeOfDay.hour)
        if (timeDiff <= 2) matchScore += 0.3f
        
        // Day of week match
        if (context.dayOfWeek == dayOfWeek) matchScore += 0.2f
        
        // Charging state match
        if (preferredCharging && context.isCharging) matchScore += 0.15f
        
        // WiFi match
        if (preferredWifi && context.isWifiConnected) matchScore += 0.15f
        
        // Location match
        if (commonLocation == context.locationCategory) matchScore += 0.1f
        
        // Activity match
        if (commonActivity == context.activityType) matchScore += 0.1f
        
        return matchScore >= 0.5f
    }
}

@Serializable
private data class UserHabitModelData(
    val samples: List<BackupSample>,
    val patterns: List<BackupPattern>,
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastTrainingTime: LocalDateTime?
)

// Serializers for Java time classes
object LocalDateTimeSerializer : kotlinx.serialization.KSerializer<LocalDateTime> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
        "LocalDateTime",
        kotlinx.serialization.descriptors.PrimitiveKind.STRING
    )
    
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: LocalDateTime) {
        encoder.encodeString(value.toString())
    }
    
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString())
    }
}

object LocalTimeSerializer : kotlinx.serialization.KSerializer<LocalTime> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
        "LocalTime",
        kotlinx.serialization.descriptors.PrimitiveKind.STRING
    )
    
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: LocalTime) {
        encoder.encodeString(value.toString())
    }
    
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): LocalTime {
        return LocalTime.parse(decoder.decodeString())
    }
}
