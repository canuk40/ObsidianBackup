// ml/prediction/BackupPredictor.kt
package com.obsidianbackup.ml.prediction

import android.content.Context
import android.util.Log
import com.obsidianbackup.ml.BackupContext
import com.obsidianbackup.ml.FileAnomaly
import com.obsidianbackup.ml.models.BackupPrediction
import com.obsidianbackup.model.AppId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.exp

/**
 * TensorFlow Lite-based predictor for backup scheduling.
 * 
 * ML Model Training Required:
 * - Run ml_training/train_backup_predictor.py to generate backup_predictor.tflite
 * - Copy backup_predictor.tflite to app/src/main/assets/
 * - Copy scaler_params.json to app/src/main/res/raw/
 * 
 * Graceful Degradation:
 * - If model file missing: Uses heuristic-based fallback predictions
 * - If model loads: Uses ML-based predictions with GPU acceleration
 * 
 * Research Citations:
 * - TensorFlow Lite GPU Delegate: https://ai.google.dev/edge/litert/android/gpu
 * - NNAPI Deprecated (Android 15): https://developer.android.com/ndk/guides/neuralnetworks/migration-guide
 */
@Singleton
class BackupPredictor @Inject constructor(@ApplicationContext private val context: Context) {
    
    private var interpreter: Interpreter? = null
    private var gpuDelegate: org.tensorflow.lite.gpu.GpuDelegate? = null
    private val trainingData = mutableListOf<TrainingDataPoint>()
    private var modelAccuracy = 0f
    private var isInitialized = false
    private var usingFallback = false
    private var scalerParams: ScalerParams? = null
    
    // Feature indices for model input
    private companion object {
        private const val TAG = "[BackupPredictor]"
        private const val MODEL_FILE = "ml_models/backup_predictor.tflite"
        private const val SCALER_PARAMS_FILE = "scaler_params.json"
        private const val MAX_TRAINING_SAMPLES = 500

        // A real trained TFLite model is at minimum ~50 KiB.
        // The current placeholder is only 5.3 KB — anything below this threshold
        // is treated as an invalid/stub model so the fallback heuristics are used
        // instead of silently returning garbage predictions.
        private const val MIN_VALID_MODEL_SIZE_BYTES = 50 * 1024L  // 50 KiB
        
        // Input features (normalized to 0-1) - UPDATED to 8 features
        private const val FEATURE_HOUR_OF_DAY = 0           // 0-23 -> normalized
        private const val FEATURE_DAY_OF_WEEK = 1           // 0-6 -> normalized
        private const val FEATURE_BATTERY_LEVEL = 2         // 0-100 -> normalized
        private const val FEATURE_IS_CHARGING = 3           // boolean -> 0 or 1
        private const val FEATURE_IS_WIFI = 4               // boolean -> 0 or 1
        private const val FEATURE_LOCATION = 5              // category -> normalized
        private const val FEATURE_ACTIVITY = 6              // category -> normalized
        private const val FEATURE_TIME_SINCE_LAST_BACKUP = 7 // hours (0-168) -> normalized
        private const val INPUT_SIZE = 8
        
        // Anomaly detection thresholds
        private const val ANOMALY_THRESHOLD = 2.5f     // Standard deviations
        private const val MIN_SAMPLES_FOR_ANOMALY = 5
    }
    
    /**
     * Initialize the TensorFlow Lite interpreter with GPU acceleration.
     * 
     * Graceful degradation:
     * - Tries to load model from assets/backup_predictor.tflite
     * - If model missing: Logs warning and uses heuristic fallback
     * - If model found: Initializes with GPU delegate if available, CPU fallback otherwise
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            val modelBuffer = loadModelFile()
            if (modelBuffer != null) {
                val options = Interpreter.Options().apply {
                    numThreads = 2
                    // Try GPU delegate; fall back to CPU if GPU is unavailable on this device
                    try {
                        val gpu = org.tensorflow.lite.gpu.GpuDelegate()
                        addDelegate(gpu)
                        gpuDelegate = gpu
                        Log.i(TAG, "TFLite GPU delegate enabled")
                    } catch (e: Exception) {
                        Log.d(TAG, "GPU delegate unavailable (${e.message}), using CPU")
                    }
                }
                interpreter = Interpreter(modelBuffer, options)
                isInitialized = true
                val accel = if (gpuDelegate != null) "GPU" else "CPU"
                Log.i(TAG, "TFLite model initialized successfully ($accel)")
            } else {
                // Model not found - use fallback heuristics
                isInitialized = false
                usingFallback = true
                Log.w(TAG, "$MODEL_FILE not found in assets — using heuristics fallback")
            }
        } catch (e: java.io.FileNotFoundException) {
            Log.w(TAG, "$MODEL_FILE not found in assets — using heuristics fallback: ${e.message}")
            isInitialized = false
            usingFallback = true
        } catch (e: Exception) {
            Log.e(TAG, "TFLite model initialization failed: ${e.message}")
            isInitialized = false
            usingFallback = true
        }
    }
    
    /**
     * Load TensorFlow Lite model file from assets.
     * 
     * Checks both:
     * 1. app/src/main/assets/backup_predictor.tflite (preferred)
     * 2. app-private filesDir/ml_models/ (fallback for dynamic models)
     */
    private fun loadModelFile(): MappedByteBuffer? {
        return try {
            // Try assets first (standard location after training)
            val fd = context.assets.openFd(MODEL_FILE)
            fd.use { fileDescriptor ->
                // Validity check: reject suspiciously small files (placeholder / corrupted model)
                if (fileDescriptor.declaredLength < MIN_VALID_MODEL_SIZE_BYTES) {
                    Log.w(
                        TAG,
                        "TFLite model '$MODEL_FILE' is only ${fileDescriptor.declaredLength} bytes " +
                        "(minimum expected: $MIN_VALID_MODEL_SIZE_BYTES bytes). " +
                        "The committed model is a placeholder. " +
                        "Run ml_training/train_backup_predictor.py to generate a valid model, " +
                        "then copy the output to app/src/main/assets/$MODEL_FILE. " +
                        "Falling back to heuristic predictions."
                    )
                    return null
                }
                FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                    val fileChannel = inputStream.channel
                    val startOffset = fileDescriptor.startOffset
                    val declaredLength = fileDescriptor.declaredLength
                    fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
                }
            }
        } catch (e: Exception) {
            // Try filesDir fallback
            try {
                val modelPath = File(context.filesDir, MODEL_FILE)
                if (modelPath.exists()) {
                    // Apply same minimum size check for dynamic models
                    if (modelPath.length() < MIN_VALID_MODEL_SIZE_BYTES) {
                        Log.w(TAG,
                            "Dynamic model at ${modelPath.absolutePath} is only " +
                            "${modelPath.length()} bytes — treating as invalid placeholder.")
                        return null
                    }
                    FileInputStream(modelPath).use { inputStream ->
                        val fileChannel = inputStream.channel
                        fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size())
                    }
                } else {
                    null
                }
            } catch (e2: Exception) {
                null
            }
        }
    }
    
    /**
     * Load scaler parameters from resources.
     * 
     * StandardScaler normalization parameters from training:
     * - mean: per-feature mean values
     * - scale: per-feature standard deviations
     */
    private fun loadScalerParams(): ScalerParams? {
        return try {
            val inputStream = context.resources.openRawResource(
                context.resources.getIdentifier(
                    SCALER_PARAMS_FILE.removeSuffix(".json"),
                    "raw",
                    context.packageName
                )
            )
            val json = inputStream.bufferedReader().use { it.readText() }
            val gson = com.google.gson.Gson()
            gson.fromJson(json, ScalerParams::class.java).also {
                Log.i(TAG, "Scaler parameters loaded (${it.mean.size} features)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Scaler params not found, using default normalization")
            null
        }
    }
    
    /**
     * Train model with new backup event
     */
    suspend fun train(
        appIds: List<AppId>,
        timestamp: LocalDateTime,
        context: BackupContext
    ) = withContext(Dispatchers.Default) {
        val dataPoint = TrainingDataPoint(
            timestamp = timestamp,
            appIds = appIds,
            context = context,
            wasSuccessful = true
        )
        
        trainingData.add(dataPoint)
        
        // Keep only recent samples
        if (trainingData.size > MAX_TRAINING_SAMPLES) {
            trainingData.removeAt(0)
        }
        
        // Update model accuracy estimate
        updateModelAccuracy()
        
        Log.d(TAG, "Added training sample, total: ${trainingData.size}")
    }
    
    /**
     * Predict next backup times based on current context
     */
    suspend fun predictNextBackups(
        currentContext: BackupContext,
        maxPredictions: Int = 5
    ): List<BackupPrediction> = withContext(Dispatchers.Default) {
        
        if (trainingData.isEmpty()) {
            return@withContext emptyList()
        }
        
        val predictions = mutableListOf<BackupPrediction>()
        
        // Generate predictions for next 24 hours
        val now = LocalDateTime.now()
        for (hoursAhead in 1..24 step 2) {
            val predictedTime = now.plusHours(hoursAhead.toLong())
            val predictedContext = estimateContext(predictedTime, currentContext)
            
            val confidence = if (isInitialized && interpreter != null) {
                predictUsingTFLite(predictedContext, hoursAhead.toLong())
            } else {
                predictUsingHeuristics(predictedContext, predictedTime)
            }
            
            if (confidence > 0.5f) {
                predictions.add(
                    BackupPrediction(
                        id = "pred_${System.currentTimeMillis()}_$hoursAhead",
                        predictedTime = predictedTime,
                        confidence = confidence,
                        estimatedMinutesUntil = hoursAhead * 60L,
                        suggestedAppIds = findMostLikelyApps(predictedContext),
                        estimatedSizeMb = estimateBackupSize(),
                        suggestsCloudSync = shouldSuggestCloudSync(predictedContext),
                        reason = "Based on learned backup patterns"
                    )
                )
            }
        }
        
        // Sort by confidence and return top predictions
        predictions.sortedByDescending { it.confidence }.take(maxPredictions)
    }
    
    /**
     * Predict using TensorFlow Lite model (with GPU acceleration if available).
     */
    private fun predictUsingTFLite(
        context: BackupContext,
        timeSinceLastBackup: Long = 24L
    ): Float {
        try {
            val input = prepareInputFeatures(context, timeSinceLastBackup)
            val output = Array(1) { FloatArray(1) }
            
            interpreter?.run(input, output)
            
            return output[0][0].coerceIn(0f, 1f)
        } catch (e: Exception) {
            Log.e(TAG, "TFLite prediction failed: ${e.message}", e)
            return 0f
        }
    }
    
    /**
     * Fallback prediction using heuristics when TFLite unavailable
     */
    private fun predictUsingHeuristics(
        context: BackupContext,
        predictedTime: LocalDateTime
    ): Float {
        var confidence = 0.5f
        
        // Find similar historical backups
        val similarBackups = trainingData.filter { dataPoint ->
            isSimilarContext(dataPoint.context, context, dataPoint.timestamp, predictedTime)
        }
        
        if (similarBackups.isEmpty()) {
            return 0.3f
        }
        
        // Calculate confidence based on historical patterns
        confidence += (similarBackups.size.toFloat() / trainingData.size) * 0.3f
        
        // Boost confidence for optimal conditions
        if (context.isCharging) confidence += 0.1f
        if (context.isWifiConnected) confidence += 0.1f
        if (context.batteryLevel > 50f) confidence += 0.05f
        
        return confidence.coerceIn(0f, 1f)
    }
    
    /**
     * Prepare input features for TFLite model (8 features, normalized).
     * 
     * Uses StandardScaler normalization if scaler_params.json available,
     * otherwise uses simple 0-1 normalization.
     * 
     * Feature order must match training script (train_backup_predictor.py):
     * 0. hour_of_day (0-23)
     * 1. day_of_week (0-6)
     * 2. battery_level (0-100)
     * 3. is_charging (0/1)
     * 4. is_wifi_connected (0/1)
     * 5. location_category (0-2)
     * 6. activity_type (0-1)
     * 7. time_since_last_backup (0-168 hours)
     */
    private fun prepareInputFeatures(
        context: BackupContext,
        timeSinceLastBackup: Long = 24L  // Default: 24 hours
    ): Array<FloatArray> {
        val features = FloatArray(INPUT_SIZE)
        
        // Extract raw feature values
        val rawFeatures = floatArrayOf(
            context.timeOfDay.hour.toFloat(),           // 0-23
            context.dayOfWeek.value.toFloat(),          // 1-7 -> need 0-6
            context.batteryLevel,                        // 0-100
            if (context.isCharging) 1f else 0f,         // 0/1
            if (context.isWifiConnected) 1f else 0f,    // 0/1
            context.locationCategory.ordinal.toFloat(), // 0-2
            context.activityType.ordinal.toFloat(),     // 0-5
            timeSinceLastBackup.toFloat()               // hours since last backup
        )
        
        // Normalize using scaler params (if available)
        if (scalerParams != null && scalerParams!!.mean.size == INPUT_SIZE) {
            for (i in 0 until INPUT_SIZE) {
                features[i] = (rawFeatures[i] - scalerParams!!.mean[i]) / scalerParams!!.scale[i]
            }
        } else {
            // Fallback: simple 0-1 normalization
            features[FEATURE_HOUR_OF_DAY] = rawFeatures[0] / 24f
            features[FEATURE_DAY_OF_WEEK] = (rawFeatures[1] - 1f) / 6f  // 1-7 -> 0-1
            features[FEATURE_BATTERY_LEVEL] = rawFeatures[2] / 100f
            features[FEATURE_IS_CHARGING] = rawFeatures[3]
            features[FEATURE_IS_WIFI] = rawFeatures[4]
            features[FEATURE_LOCATION] = rawFeatures[5] / 2f
            features[FEATURE_ACTIVITY] = rawFeatures[6] / 5f
            features[FEATURE_TIME_SINCE_LAST_BACKUP] = (rawFeatures[7] / 168f).coerceAtMost(1f)  // Cap at 1 week
        }
        
        return arrayOf(features)
    }
    
    /**
     * Check if two contexts are similar
     */
    private fun isSimilarContext(
        c1: BackupContext,
        c2: BackupContext,
        t1: LocalDateTime,
        t2: LocalDateTime
    ): Boolean {
        // Time similarity (within 2 hours)
        val hourDiff = abs(t1.hour - t2.hour)
        if (hourDiff > 2) return false
        
        // Day of week match
        if (c1.dayOfWeek != c2.dayOfWeek) return false
        
        // Location match
        if (c1.locationCategory != c2.locationCategory) return false
        
        return true
    }
    
    /**
     * Estimate context for future time
     */
    private fun estimateContext(
        futureTime: LocalDateTime,
        currentContext: BackupContext
    ): BackupContext {
        // For short-term predictions, assume context mostly stays the same
        // but adjust time-dependent features
        return currentContext.copy(
            timeOfDay = futureTime.toLocalTime(),
            dayOfWeek = futureTime.dayOfWeek
        )
    }
    
    /**
     * Find most likely apps to backup based on context
     */
    private fun findMostLikelyApps(context: BackupContext): List<AppId> {
        val appFrequency = mutableMapOf<AppId, Int>()
        
        trainingData.forEach { dataPoint ->
            if (isSimilarContext(dataPoint.context, context, dataPoint.timestamp, LocalDateTime.now())) {
                dataPoint.appIds.forEach { appId ->
                    appFrequency[appId] = (appFrequency[appId] ?: 0) + 1
                }
            }
        }
        
        return appFrequency
            .entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key }
    }
    
    /**
     * Estimate backup size based on historical data
     */
    private fun estimateBackupSize(): Long {
        // Simple heuristic: average of recent backup sizes
        // In real implementation, would track actual sizes
        return 250L // Default: 250 MB
    }
    
    /**
     * Determine if cloud sync should be suggested
     */
    private fun shouldSuggestCloudSync(context: BackupContext): Boolean {
        return context.isWifiConnected && context.isCharging
    }
    
    /**
     * Detect anomalies in file activity
     */
    suspend fun detectAnomalies(
        fileActivity: List<FileActivityEvent>
    ): List<FileAnomaly> = withContext(Dispatchers.Default) {
        
        if (fileActivity.size < MIN_SAMPLES_FOR_ANOMALY) {
            return@withContext emptyList()
        }
        
        val anomalies = mutableListOf<FileAnomaly>()
        
        // Group activity by app
        val activityByApp = fileActivity.groupBy { it.appId }
        
        activityByApp.forEach { (appId, events) ->
            // Calculate statistics
            val changes = events.map { it.changeCount.toFloat() }
            val mean = changes.average().toFloat()
            val stdDev = calculateStdDev(changes, mean)
            
            // Find outliers (z-score > threshold)
            events.forEach { event ->
                val zScore = abs((event.changeCount - mean) / stdDev)
                
                if (zScore > ANOMALY_THRESHOLD) {
                    anomalies.add(
                        FileAnomaly(
                            appId = appId,
                            description = "Unusual file activity: ${event.changeCount} changes (${zScore.toInt()}σ)",
                            severity = (zScore / ANOMALY_THRESHOLD).coerceAtMost(1f),
                            detectedAt = LocalDateTime.now()
                        )
                    )
                }
            }
        }
        
        anomalies
    }
    
    /**
     * Calculate standard deviation
     */
    private fun calculateStdDev(values: List<Float>, mean: Float): Float {
        if (values.isEmpty()) return 0f
        
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance).toFloat()
    }
    
    /**
     * Update model accuracy estimate
     */
    private fun updateModelAccuracy() {
        // Simplified accuracy calculation
        // In production, would compare predictions with actual outcomes
        modelAccuracy = if (trainingData.size >= 20) {
            0.75f + (trainingData.size.toFloat() / MAX_TRAINING_SAMPLES) * 0.2f
        } else {
            0.5f
        }.coerceAtMost(0.95f)
    }
    
    /**
     * Get current model accuracy
     */
    fun getAccuracy(): Float = modelAccuracy
    
    /**
     * Reset predictor
     */
    suspend fun reset() = withContext(Dispatchers.IO) {
        trainingData.clear()
        modelAccuracy = 0f
        Log.w(TAG, "Predictor reset")
    }
    
    /**
     * Clean up resources (interpreter).
     */
    fun close() {
        gpuDelegate?.close()
        gpuDelegate = null
        interpreter?.close()
        interpreter = null
        Log.i(TAG, "Predictor resources released")
    }
    
    /**
     * Check if using ML model or fallback heuristics.
     */
    fun isUsingMlModel(): Boolean = isInitialized && !usingFallback
}

/**
 * Scaler parameters from StandardScaler during training.
 * 
 * Loaded from app/src/main/res/raw/scaler_params.json
 * Generated by ml_training/train_backup_predictor.py
 */
data class ScalerParams(
    val mean: List<Float>,
    val scale: List<Float>,
    val features: List<String>
)

data class TrainingDataPoint(
    val timestamp: LocalDateTime,
    val appIds: List<AppId>,
    val context: BackupContext,
    val wasSuccessful: Boolean
)

data class FileActivityEvent(
    val appId: AppId,
    val changeCount: Long,
    val timestamp: LocalDateTime
)
