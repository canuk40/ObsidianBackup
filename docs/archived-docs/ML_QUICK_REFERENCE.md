# AI/ML Smart Scheduling - Quick Reference

## 🚀 Quick Start

```kotlin
// 1. Initialize
val smartScheduler = SmartScheduler(context)
smartScheduler.initialize()

// 2. Schedule smart backup
smartScheduler.scheduleSmartBackup()

// 3. Learn from backups
smartScheduler.learnFromBackup(
    appIds = listOf(AppId("com.whatsapp")),
    timestamp = LocalDateTime.now(),
    components = setOf(BackupComponent.APK, BackupComponent.DATA),
    context = currentContext
)
```

## 💬 Natural Language Examples

```kotlin
// Process user queries
val queries = listOf(
    "backup my games",
    "backup WhatsApp from yesterday",
    "backup all messaging apps now",
    "backup photos and videos from last week"
)

queries.forEach { query ->
    val result = smartScheduler.processNaturalLanguageQuery(query)
    when (result) {
        is NLQueryResult.Success -> {
            println("Apps: ${result.appIds}")
            println("Time: ${result.timeRange}")
            println("Components: ${result.components}")
        }
        is NLQueryResult.Error -> {
            println("Error: ${result.message}")
        }
    }
}
```

## 📊 Get Recommendations

```kotlin
// Get smart recommendations
val recommendations = smartScheduler.getRecommendations()

recommendations.forEach { rec ->
    println("${rec.priority}: ${rec.reason}")
    println("Confidence: ${rec.confidence}")
    println("Apps: ${rec.suggestedApps}")
}
```

## 🔍 Anomaly Detection

```kotlin
// Detect unusual file activity
val anomalies = smartScheduler.detectAnomalies()

anomalies.forEach { anomaly ->
    if (anomaly.severity > 0.7f) {
        println("⚠️ URGENT: ${anomaly.appId}")
        println("   ${anomaly.description}")
        // Trigger immediate backup
    }
}
```

## 📈 Model Statistics

```kotlin
// Check ML model performance
val stats = smartScheduler.getModelStats()

println("Total samples: ${stats.totalSamples}")
println("Model accuracy: ${(stats.modelAccuracy * 100).toInt()}%")
println("Ready: ${stats.isReady}")
println("Last training: ${stats.lastTrainingTime}")

// Need at least 10 samples to start making predictions
if (stats.totalSamples < 10) {
    println("Collecting more training data...")
}
```

## 🎯 Predictions

```kotlin
// Get next backup prediction
val prediction = smartScheduler.predictNextBackupTime()

prediction?.let {
    println("Next backup in ${it.estimatedMinutesUntil} minutes")
    println("Confidence: ${(it.confidence * 100).toInt()}%")
    println("Suggested apps: ${it.suggestedAppIds}")
    println("Estimated size: ${it.estimatedSizeMb} MB")
    println("Cloud sync: ${it.suggestsCloudSync}")
}
```

## 🔄 Model Management

```kotlin
// Export model for backup
val modelData = smartScheduler.exportModel()
// Save to file or cloud storage

// Import model (e.g., on new device)
smartScheduler.importModel(modelData)

// Reset model (clear all learned data)
smartScheduler.resetModels()
```

## ⚙️ Configuration

```kotlin
// In SmartScheduler.kt, adjust these constants:

// Minimum confidence to act on prediction
const val MIN_CONFIDENCE_THRESHOLD = 0.7f  // 70%

// High confidence for automatic scheduling
const val HIGH_CONFIDENCE_THRESHOLD = 0.85f  // 85%

// Minimum samples before making predictions
const val MIN_SAMPLES_FOR_PREDICTION = 10

// Anomaly detection sensitivity
const val ANOMALY_THRESHOLD = 2.5f  // Standard deviations
```

## 🎨 UI Integration

```kotlin
@Composable
fun SmartBackupCard(viewModel: SmartBackupViewModel) {
    val predictions by viewModel.predictions.collectAsState()
    
    Card {
        Column {
            Text("Smart Predictions", style = MaterialTheme.typography.titleLarge)
            
            predictions.take(3).forEach { prediction ->
                ListItem(
                    headlineContent = { 
                        Text("In ${prediction.estimatedMinutesUntil} min") 
                    },
                    supportingContent = { 
                        Text("${(prediction.confidence * 100).toInt()}% confidence") 
                    },
                    leadingContent = {
                        Icon(Icons.Default.Schedule, "Prediction")
                    }
                )
            }
            
            Button(onClick = { viewModel.scheduleSmartBackup() }) {
                Text("Schedule Smart Backup")
            }
        }
    }
}
```

## 📱 Worker Integration

```kotlin
class BackupWorker : CoroutineWorker(context, params) {
    private val smartScheduler = SmartScheduler(context)
    
    override suspend fun doWork(): Result {
        // Perform backup...
        val result = performBackup(appIds, components)
        
        // Learn from this backup
        val contextManager = ContextAwareManager(context)
        smartScheduler.learnFromBackup(
            appIds = appIds,
            timestamp = LocalDateTime.now(),
            components = components,
            context = contextManager.getCurrentContext()
        )
        
        return if (result.success) Result.success() else Result.retry()
    }
}
```

## 🔐 Permissions

```xml
<!-- AndroidManifest.xml -->

<!-- Optional: For better activity detection -->
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />

<!-- For network state monitoring -->
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## 🐛 Debugging

```kotlin
// Enable verbose logging
Log.d("SmartScheduler", "Current predictions: ${predictions.value}")

// Check context score
val contextManager = ContextAwareManager(context)
val score = contextManager.getContextScore()
Log.d("Context", "Score: $score (optimal: >0.7)")

// Verify model is ready
val stats = smartScheduler.getModelStats()
if (!stats.isReady) {
    Log.w("ML", "Model not ready yet. Samples: ${stats.totalSamples}/10")
}
```

## 📊 Analytics Export

```kotlin
// Export analytics for model training
val analytics = BackupAnalytics(context)
val json = analytics.export()

// Save to file
File(context.filesDir, "analytics_export.json").writeText(json)

// Use with training script:
// python3 train_backup_predictor.py
```

## 🎓 Training Custom Model

```bash
# 1. Export analytics from app
# 2. Save as analytics_export.json
# 3. Run training script:
cd ml_training
python3 train_backup_predictor.py

# 4. Copy trained model:
cp backup_predictor.tflite ../app/src/main/assets/ml_models/

# 5. Rebuild app
```

## ⚡ Performance Tips

- Call `initialize()` once at app startup
- Use coroutines for all ML operations (avoid UI thread)
- Batch learning calls (don't call after every backup)
- Prune old data periodically (call `resetModels()` if needed)
- Monitor memory usage with model stats

## 🔍 Troubleshooting

**No predictions?**
- Check: `stats.totalSamples >= 10`
- Check: Model initialized successfully
- Check: Sufficient training data variety

**Low confidence?**
- Need more training samples
- Inconsistent backup patterns
- Try lowering `MIN_CONFIDENCE_THRESHOLD`

**NLP not working?**
- Ensure query contains "backup"
- Include app name or category
- Check supported patterns in docs

**High memory?**
- Reduce `MAX_SAMPLES` in UserHabitModel
- Reduce `MAX_EVENTS` in BackupAnalytics
- Call `resetModels()` to clear old data

## 📚 Documentation

- **Full Guide**: `AI_ML_SCHEDULING.md`
- **Implementation**: `ML_IMPLEMENTATION_SUMMARY.md`
- **API Docs**: Inline in source files
- **Training**: `ml_training/README.md`

## 🎯 Key Files

- `SmartScheduler.kt` - Main API
- `UserHabitModel.kt` - Pattern learning
- `BackupPredictor.kt` - ML predictions
- `ContextAwareManager.kt` - Device monitoring
- `NaturalLanguageProcessor.kt` - Query parsing
- `BackupAnalytics.kt` - Event tracking

---

**Pro Tip**: Let the ML model collect data for at least 1-2 weeks before relying on predictions for production use.

**Remember**: All ML runs on-device. Your data never leaves your phone! 🔒
