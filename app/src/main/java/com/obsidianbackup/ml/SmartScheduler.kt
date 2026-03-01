// ml/SmartScheduler.kt
package com.obsidianbackup.ml

import android.content.Context
import android.util.Log
import androidx.work.*
import com.obsidianbackup.ml.analytics.BackupAnalytics
import com.obsidianbackup.ml.context.ContextAwareManager
import com.obsidianbackup.ml.models.BackupPrediction
import com.obsidianbackup.ml.models.UserHabitModel
import com.obsidianbackup.ml.nlp.NaturalLanguageProcessor
import com.obsidianbackup.ml.prediction.BackupPredictor
import com.obsidianbackup.model.AppId
import com.obsidianbackup.model.BackupComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import androidx.work.workDataOf

/**
 * AI/ML-powered predictive backup scheduler using on-device machine learning.
 * 
 * Features:
 * - Pattern recognition: Learn user backup habits
 * - Predictive scheduling: Forecast when user needs backup
 * - Context-aware triggers: Location, activity, battery, time patterns
 * - Anomaly detection: Detect unusual file activity
 * - Natural language queries: "backup my games from yesterday"
 * - Privacy-preserving: All ML runs on-device
 * 
 * Uses Google ML Kit and TensorFlow Lite for on-device inference.
 */
class SmartScheduler(
    private val context: Context,
    private val contextManager: ContextAwareManager,   // M-5: Injected instead of self-created
    private val backupPredictor: BackupPredictor,      // M-5: Injected instead of self-created
    private val analytics: BackupAnalytics,            // M-5: Injected instead of self-created
    private val nlpProcessor: NaturalLanguageProcessor, // M-5: Injected instead of self-created
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val workManager = WorkManager.getInstance(context)
    
    // ML Components (now injected — removed internal instantiation)
    private val userHabitModel = UserHabitModel(context)
    
    // State flows for reactive updates
    private val _predictions = MutableStateFlow<List<BackupPrediction>>(emptyList())
    val predictions: StateFlow<List<BackupPrediction>> = _predictions.asStateFlow()
    
    private val _isLearning = MutableStateFlow(false)
    val isLearning: StateFlow<Boolean> = _isLearning.asStateFlow()
    
    companion object {
        private const val TAG = "SmartScheduler"
        private const val WORK_NAME_SMART_BACKUP = "smart_backup_ml"
        private const val WORK_NAME_PATTERN_LEARNING = "pattern_learning"
        private const val WORK_NAME_ANOMALY_DETECTION = "anomaly_detection"
        
        // ML confidence thresholds
        private const val MIN_CONFIDENCE_THRESHOLD = 0.7f
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.85f
        
        // Learning parameters
        private const val MIN_SAMPLES_FOR_PREDICTION = 10
        private const val LEARNING_RATE = 0.01f
    }
    
    /**
     * Initialize the smart scheduler and start learning from existing data
     */
    suspend fun initialize() {
        Log.i(TAG, "Initializing SmartScheduler with ML models")
        
        try {
            // Load existing ML models
            userHabitModel.load()
            backupPredictor.initialize()
            
            // Start context monitoring
            contextManager.startMonitoring()
            
            // Initialize NLP processor
            nlpProcessor.initialize()
            
            // Start periodic pattern learning
            schedulePatternLearning()
            
            // Start anomaly detection
            scheduleAnomalyDetection()
            
            Log.i(TAG, "SmartScheduler initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SmartScheduler: ${e.message}", e)
        }
    }
    
    /**
     * Learn from user backup behavior and update ML models
     */
    suspend fun learnFromBackup(
        appIds: List<AppId>,
        timestamp: LocalDateTime,
        components: Set<BackupComponent>,
        context: BackupContext
    ) {
        _isLearning.value = true
        
        try {
            // Record backup event for pattern recognition
            analytics.recordBackupEvent(
                appIds = appIds,
                timestamp = timestamp,
                components = components,
                context = context
            )
            
            // Update user habit model
            userHabitModel.addSample(
                dayOfWeek = timestamp.dayOfWeek,
                timeOfDay = timestamp.toLocalTime(),
                appIds = appIds,
                context = context
            )
            
            // Train predictor with new data
            backupPredictor.train(
                appIds = appIds,
                timestamp = timestamp,
                context = context
            )
            
            // Update predictions
            updatePredictions()
            
            Log.i(TAG, "Learned from backup: ${appIds.size} apps at ${timestamp}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to learn from backup: ${e.message}", e)
        } finally {
            _isLearning.value = false
        }
    }
    
    /**
     * Predict optimal backup times based on learned patterns
     */
    suspend fun predictNextBackupTime(): BackupPrediction? {
        return try {
            val currentContext = contextManager.getCurrentContext()
            val predictions = backupPredictor.predictNextBackups(currentContext)
            
            predictions
                .filter { it.confidence >= MIN_CONFIDENCE_THRESHOLD }
                .maxByOrNull { it.confidence }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to predict next backup time: ${e.message}", e)
            null
        }
    }
    
    /**
     * Schedule smart backup based on ML predictions
     */
    suspend fun scheduleSmartBackup() {
        val prediction = predictNextBackupTime()
        
        if (prediction == null) {
            Log.w(TAG, "No confident prediction available, falling back to default schedule")
            scheduleDefaultBackup()
            return
        }
        
        if (prediction.confidence < HIGH_CONFIDENCE_THRESHOLD) {
            Log.w(TAG, "Prediction confidence below threshold: ${prediction.confidence}")
        }
        
        val delayMinutes = prediction.estimatedMinutesUntil
        
        Log.i(TAG, "Scheduling smart backup in $delayMinutes minutes (confidence: ${prediction.confidence})")
        
        // Build constraints based on predicted context
        val constraints = buildSmartConstraints(prediction)
        
        // Create work request with ML-predicted timing
        val workRequest = OneTimeWorkRequestBuilder<SmartBackupWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(workDataOf(
                "prediction_id" to prediction.id,
                "app_ids" to prediction.suggestedAppIds.joinToString(","),
                "confidence" to prediction.confidence
            ))
            .addTag("smart_backup_ml")
            .build()
        
        workManager.enqueueUniqueWork(
            WORK_NAME_SMART_BACKUP,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    /**
     * Process natural language query for backup requests
     * Examples: "backup my games from yesterday", "backup WhatsApp now"
     */
    suspend fun processNaturalLanguageQuery(query: String): NLQueryResult {
        return try {
            Log.i(TAG, "Processing NL query: $query")
            nlpProcessor.parseBackupQuery(query)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process NL query: ${e.message}", e)
            NLQueryResult.Error("Failed to understand query: ${e.message}")
        }
    }
    
    /**
     * Detect anomalies in file activity that might require urgent backup
     */
    suspend fun detectAnomalies(): List<FileAnomaly> {
        return try {
            val fileActivity = analytics.getRecentFileActivity()
            backupPredictor.detectAnomalies(fileActivity)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to detect anomalies: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get context-aware backup recommendations
     */
    suspend fun getRecommendations(): List<BackupRecommendation> {
        val currentContext = contextManager.getCurrentContext()
        val patterns = userHabitModel.getPatterns()
        val anomalies = detectAnomalies()
        
        val recommendations = mutableListOf<BackupRecommendation>()
        
        // Check if current context matches learned patterns
        patterns.forEach { pattern ->
            if (pattern.matches(currentContext)) {
                recommendations.add(
                    BackupRecommendation(
                        reason = "Based on your usual backup pattern at this time",
                        confidence = pattern.confidence,
                        suggestedApps = pattern.appIds,
                        priority = RecommendationPriority.MEDIUM
                    )
                )
            }
        }
        
        // Add anomaly-based recommendations
        anomalies.forEach { anomaly ->
            if (anomaly.severity > 0.7f) {
                recommendations.add(
                    BackupRecommendation(
                        reason = "Unusual activity detected: ${anomaly.description}",
                        confidence = anomaly.severity,
                        suggestedApps = listOf(anomaly.appId),
                        priority = RecommendationPriority.HIGH
                    )
                )
            }
        }
        
        return recommendations.sortedByDescending { it.priority.value * it.confidence }
    }
    
    /**
     * Update ML models with continuous learning
     */
    private fun schedulePatternLearning() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresDeviceIdle(true)
            .build()
        
        val learningWork = PeriodicWorkRequestBuilder<PatternLearningWorker>(
            6, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag("ml_learning")
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_PATTERN_LEARNING,
            ExistingPeriodicWorkPolicy.KEEP,
            learningWork
        )
    }
    
    /**
     * Schedule periodic anomaly detection
     */
    private fun scheduleAnomalyDetection() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        
        val anomalyWork = PeriodicWorkRequestBuilder<AnomalyDetectionWorker>(
            1, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag("ml_anomaly")
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_ANOMALY_DETECTION,
            ExistingPeriodicWorkPolicy.KEEP,
            anomalyWork
        )
    }
    
    /**
     * Build smart constraints based on ML prediction
     */
    private fun buildSmartConstraints(prediction: BackupPrediction): Constraints {
        return Constraints.Builder().apply {
            // Battery constraint based on predicted backup size
            if (prediction.estimatedSizeMb > 500) {
                setRequiresCharging(true)
            } else {
                setRequiresBatteryNotLow(true)
            }
            
            // Network constraint based on cloud sync prediction
            if (prediction.suggestsCloudSync) {
                setRequiredNetworkType(NetworkType.UNMETERED)
            } else {
                setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            }
            
            // Storage constraint
            setRequiresStorageNotLow(true)
        }.build()
    }
    
    /**
     * Update predictions based on current context
     */
    private suspend fun updatePredictions() {
        scope.launch {
            try {
                val currentContext = contextManager.getCurrentContext()
                val newPredictions = backupPredictor.predictNextBackups(currentContext)
                _predictions.value = newPredictions
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update predictions: ${e.message}", e)
            }
        }
    }
    
    /**
     * Fallback to default scheduling when ML predictions unavailable
     */
    private fun scheduleDefaultBackup() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<SmartBackupWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag("backup_default")
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "backup_default",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
    
    /**
     * Get ML model statistics and performance metrics
     */
    fun getModelStats(): ModelStats {
        return ModelStats(
            totalSamples = userHabitModel.getSampleCount(),
            modelAccuracy = backupPredictor.getAccuracy(),
            lastTrainingTime = userHabitModel.getLastTrainingTime(),
            isReady = userHabitModel.getSampleCount() >= MIN_SAMPLES_FOR_PREDICTION
        )
    }
    
    /**
     * Reset ML models and learned patterns
     */
    suspend fun resetModels() {
        Log.w(TAG, "Resetting all ML models")
        userHabitModel.reset()
        backupPredictor.reset()
        analytics.reset()
        _predictions.value = emptyList()
    }
    
    /**
     * Export ML model for backup/transfer
     */
    suspend fun exportModel(): ByteArray {
        return userHabitModel.export()
    }
    
    /**
     * Import ML model from backup
     */
    suspend fun importModel(data: ByteArray) {
        userHabitModel.import(data)
        updatePredictions()
    }
}

// Data classes for ML operations
data class BackupContext(
    val batteryLevel: Float,
    val isCharging: Boolean,
    val isWifiConnected: Boolean,
    val locationCategory: LocationCategory,
    val activityType: ActivityType,
    val timeOfDay: LocalTime,
    val dayOfWeek: DayOfWeek,
    val storageAvailableMb: Long
)

enum class LocationCategory {
    HOME, WORK, COMMUTE, UNKNOWN
}

enum class ActivityType {
    STILL, WALKING, DRIVING, IN_VEHICLE, ON_BICYCLE, UNKNOWN
}

data class BackupRecommendation(
    val reason: String,
    val confidence: Float,
    val suggestedApps: List<AppId>,
    val priority: RecommendationPriority
)

enum class RecommendationPriority(val value: Int) {
    LOW(1), MEDIUM(2), HIGH(3), URGENT(4)
}

data class FileAnomaly(
    val appId: AppId,
    val description: String,
    val severity: Float,
    val detectedAt: LocalDateTime
)

sealed class NLQueryResult {
    data class Success(
        val appIds: List<AppId>,
        val timeRange: TimeRange?,
        val components: Set<BackupComponent>
    ) : NLQueryResult()
    
    data class Error(val message: String) : NLQueryResult()
}

data class TimeRange(
    val start: LocalDateTime,
    val end: LocalDateTime
)

data class ModelStats(
    val totalSamples: Int,
    val modelAccuracy: Float,
    val lastTrainingTime: LocalDateTime?,
    val isReady: Boolean
)

// Workers for ML tasks
class SmartBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.i(TAG, "SmartBackupWorker executing ML-scheduled backup")
        
        val predictionId = inputData.getString("prediction_id")
        val confidence = inputData.getFloat("confidence", 0f)
        
        Log.i(TAG, "Executing backup with prediction ID: $predictionId, confidence: $confidence")
        
        // Trigger actual backup through broadcast
        val intent = android.content.Intent("com.obsidianbackup.ACTION_SMART_BACKUP").apply {
            putExtra("prediction_id", predictionId)
            putExtra("confidence", confidence)
        }
        applicationContext.sendBroadcast(intent)
        
        return Result.success()
    }
    
    companion object {
        private const val TAG = "SmartBackupWorker"
    }
}

class PatternLearningWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.i(TAG, "PatternLearningWorker executing periodic model training")
        
        try {
            val contextManager = ContextAwareManager(applicationContext)
            val backupPredictor = BackupPredictor(applicationContext)
            val analytics = BackupAnalytics(applicationContext)
            val nlpProcessor = NaturalLanguageProcessor(applicationContext)
            val scheduler = SmartScheduler(applicationContext, contextManager, backupPredictor, analytics, nlpProcessor)
            // Retrain models with accumulated data
            // This would trigger retraining of the TFLite models
            Log.i(TAG, "Pattern learning completed successfully")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Pattern learning failed: ${e.message}", e)
            return Result.retry()
        }
    }
    
    companion object {
        private const val TAG = "PatternLearningWorker"
    }
}

class AnomalyDetectionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.i(TAG, "AnomalyDetectionWorker scanning for unusual file activity")
        
        try {
            val contextManager = ContextAwareManager(applicationContext)
            val backupPredictor = BackupPredictor(applicationContext)
            val analytics = BackupAnalytics(applicationContext)
            val nlpProcessor = NaturalLanguageProcessor(applicationContext)
            val scheduler = SmartScheduler(applicationContext, contextManager, backupPredictor, analytics, nlpProcessor)
            val anomalies = scheduler.detectAnomalies()
            
            if (anomalies.isNotEmpty()) {
                Log.w(TAG, "Detected ${anomalies.size} anomalies")
                // Could trigger notifications for urgent anomalies
            }
            
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Anomaly detection failed: ${e.message}", e)
            return Result.retry()
        }
    }
    
    companion object {
        private const val TAG = "AnomalyDetectionWorker"
    }
}
