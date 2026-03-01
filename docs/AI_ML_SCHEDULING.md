# AI/ML-Powered Predictive Backup Scheduling

## Overview

ObsidianBackup now features cutting-edge AI/ML-powered predictive backup scheduling that learns from your behavior and automatically schedules backups at optimal times. The system uses **on-device machine learning** (Google ML Kit and TensorFlow Lite) to ensure complete privacy - no data ever leaves your device.

## Key Features

### 🧠 Smart Pattern Recognition
- **Learns Your Habits**: Analyzes when you typically perform backups
- **Time Pattern Detection**: Identifies your preferred backup times (e.g., evenings, weekends)
- **Context Clustering**: Groups similar backup scenarios for better predictions
- **Confidence Scoring**: Only acts on high-confidence predictions (>70%)

### 🔮 Predictive Scheduling
- **Forecast Optimal Times**: Predicts when you'll likely need a backup next
- **Proactive Suggestions**: Recommends backups before important events
- **24-Hour Lookahead**: Generates predictions for the next day
- **Multi-Factor Analysis**: Considers battery, WiFi, location, activity, and time patterns

### 📍 Context-Aware Triggers
The system monitors multiple context signals to optimize backup timing:

| Context | Description | Impact |
|---------|-------------|--------|
| **Battery Level** | Current charge percentage | Waits for >30% or charging |
| **Charging State** | Whether device is plugged in | Prefers charging for large backups |
| **WiFi Connection** | Network availability | Enables cloud sync |
| **Location Category** | HOME, WORK, COMMUTE | HOME preferred for backups |
| **Activity Type** | STILL, WALKING, DRIVING | STILL preferred (not in motion) |
| **Time Patterns** | Day of week, hour of day | Learns your schedule |
| **Storage Space** | Available storage | Ensures adequate space (>500MB) |

### 🚨 Anomaly Detection
- **Unusual File Activity**: Detects spikes in app data changes
- **Z-Score Analysis**: Uses statistical methods to identify outliers (>2.5σ)
- **Severity Scoring**: Prioritizes anomalies by importance (0.0-1.0)
- **Automatic Triggers**: Can initiate urgent backups for critical apps

### 💬 Natural Language Queries
Process backup requests in plain English:

```kotlin
// Examples that work:
"backup my games"
"backup WhatsApp from yesterday"
"backup all messaging apps now"
"backup photos and videos from last week"
"backup telegram and signal"
"backup everything"
```

#### Supported Query Components

**Apps**: WhatsApp, Telegram, Signal, Instagram, Facebook, Chrome, Gmail
**Categories**: games, social/messaging, photos/videos, notes/documents
**Time Ranges**: now, today, yesterday, last week, last month
**Components**: APK, data, external storage, OBB files

### 🛡️ Privacy-Preserving Design
- **100% On-Device**: All ML processing happens locally
- **No Cloud Dependency**: Works completely offline
- **No Data Transmission**: Your patterns never leave your device
- **Exportable Models**: Backup and restore your learned patterns

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     SmartScheduler                       │
│  (Main orchestrator for AI/ML backup scheduling)        │
└──────────────┬──────────────────────────────────────────┘
               │
    ┌──────────┴──────────────────────────┐
    │                                     │
┌───▼──────────────┐            ┌────────▼──────────────┐
│ UserHabitModel   │            │  BackupPredictor      │
│ (Pattern Learn)  │            │  (TensorFlow Lite)    │
└──────────────────┘            └───────────────────────┘
    │                                     │
    │  ┌──────────────────────────────────┴──────┐
    │  │                                          │
┌───▼──▼───────────┐  ┌────────────────┐  ┌─────▼──────────┐
│ BackupAnalytics  │  │ ContextAware   │  │ NLProcessor    │
│ (Track Events)   │  │ Manager        │  │ (Parse Queries)│
└──────────────────┘  └────────────────┘  └────────────────┘
```

## Components

### 1. SmartScheduler.kt
Main coordinator that orchestrates all ML components.

**Key Methods**:
```kotlin
// Initialize ML models
suspend fun initialize()

// Learn from backup behavior
suspend fun learnFromBackup(
    appIds: List<AppId>,
    timestamp: LocalDateTime,
    components: Set<BackupComponent>,
    context: BackupContext
)

// Predict optimal backup time
suspend fun predictNextBackupTime(): BackupPrediction?

// Schedule smart backup
suspend fun scheduleSmartBackup()

// Process natural language queries
suspend fun processNaturalLanguageQuery(query: String): NLQueryResult

// Detect anomalies
suspend fun detectAnomalies(): List<FileAnomaly>

// Get recommendations
suspend fun getRecommendations(): List<BackupRecommendation>
```

### 2. UserHabitModel.kt
Pattern recognition system that learns user backup habits.

**Features**:
- Collects backup samples (up to 1000)
- Extracts temporal and contextual patterns
- Groups similar behaviors using clustering
- Calculates pattern confidence scores
- Matches current context to learned patterns

**Pattern Matching Algorithm**:
```kotlin
// A pattern matches if score >= 0.5
matchScore = 
  (time_similarity * 0.3) +      // Within 2 hours
  (day_match * 0.2) +             // Same day of week
  (charging_match * 0.15) +       // Charging state
  (wifi_match * 0.15) +           // WiFi state
  (location_match * 0.1) +        // Location category
  (activity_match * 0.1)          // Activity type
```

### 3. BackupPredictor.kt
TensorFlow Lite-based predictor for backup timing.

**Input Features** (normalized to 0-1):
1. Hour of day (0-23 → 0-1)
2. Day of week (0-6 → 0-1)
3. Battery level (0-100 → 0-1)
4. Is charging (boolean → 0 or 1)
5. Is WiFi connected (boolean → 0 or 1)
6. Location category (enum → 0-1)
7. Activity type (enum → 0-1)

**Prediction Process**:
1. Generate predictions for next 24 hours (2-hour intervals)
2. Run TFLite model or heuristics-based prediction
3. Filter predictions by confidence threshold (>0.5)
4. Sort by confidence and return top 5
5. Estimate backup size and cloud sync requirements

**Anomaly Detection**:
- Uses z-score analysis on file activity
- Threshold: 2.5 standard deviations
- Minimum samples required: 5
- Severity score: z-score / threshold (capped at 1.0)

### 4. ContextAwareManager.kt
Monitors device state for context-aware scheduling.

**Monitored Signals**:
- Battery status via BatteryManager
- Network connectivity via ConnectivityManager
- Storage availability via StatFs
- Location category (time-based heuristics)
- Activity recognition (ML Kit integration ready)

**Context Quality Score** (0-1):
```kotlin
score = 
  battery_contribution (0-0.3) +   // Charging = 0.3, >70% = 0.25
  wifi_contribution (0-0.2) +       // Connected = 0.2
  activity_contribution (0-0.2) +   // Still = 0.2
  location_contribution (0-0.15) +  // Home = 0.15
  storage_contribution (0-0.15)     // >1GB = 0.15
```

### 5. NaturalLanguageProcessor.kt
Parses natural language backup queries.

**Parsing Pipeline**:
1. Normalize query (lowercase, trim)
2. Extract app identifiers (specific apps or categories)
3. Extract time range (now, today, yesterday, etc.)
4. Extract backup components (APK, data, external, OBB)
5. Validate query structure
6. Return structured result

**Supported Patterns**:
- **All apps**: "all", "everything", "every app"
- **Categories**: "games", "social", "messaging", "photos", "videos", "notes"
- **Specific apps**: "WhatsApp", "Telegram", "Signal", "Instagram", etc.
- **Time ranges**: "now", "today", "yesterday", "last week", "last month"
- **Components**: "apk", "data", "external", "obb"

### 6. BackupAnalytics.kt
Tracks backup events and file activity for ML training.

**Tracked Metrics**:
- Backup events (success/failure, duration, size)
- Context snapshots at backup time
- File activity per app
- App backup frequency
- Peak backup times
- Success rates

**Statistics Generated**:
- Total/successful backups
- Success rate
- Average duration and size
- Most backed up apps
- Peak backup hours
- Daily/weekly/monthly averages
- Context preferences (charging, WiFi, location)

## Usage Examples

### Basic Usage

```kotlin
// Initialize smart scheduler
val smartScheduler = SmartScheduler(context)
smartScheduler.initialize()

// Schedule smart backup based on ML predictions
smartScheduler.scheduleSmartBackup()

// Get recommendations
val recommendations = smartScheduler.getRecommendations()
recommendations.forEach { rec ->
    println("Recommendation: ${rec.reason}")
    println("Confidence: ${rec.confidence}")
    println("Apps: ${rec.suggestedApps}")
}
```

### Learning from Backups

```kotlin
// After each backup, teach the ML model
smartScheduler.learnFromBackup(
    appIds = listOf(AppId("com.whatsapp")),
    timestamp = LocalDateTime.now(),
    components = setOf(BackupComponent.APK, BackupComponent.DATA),
    context = currentContext
)
```

### Natural Language Queries

```kotlin
// Process user query
val result = smartScheduler.processNaturalLanguageQuery(
    "backup my games from yesterday"
)

when (result) {
    is NLQueryResult.Success -> {
        println("Apps: ${result.appIds}")
        println("Time range: ${result.timeRange}")
        println("Components: ${result.components}")
    }
    is NLQueryResult.Error -> {
        println("Error: ${result.message}")
    }
}
```

### Anomaly Detection

```kotlin
// Check for unusual file activity
val anomalies = smartScheduler.detectAnomalies()
anomalies.forEach { anomaly ->
    println("⚠️ Anomaly in ${anomaly.appId}")
    println("   ${anomaly.description}")
    println("   Severity: ${anomaly.severity}")
}
```

### Model Statistics

```kotlin
// Get ML model performance metrics
val stats = smartScheduler.getModelStats()
println("Total samples: ${stats.totalSamples}")
println("Model accuracy: ${stats.modelAccuracy}")
println("Is ready: ${stats.isReady}")
println("Last training: ${stats.lastTrainingTime}")
```

### Export/Import Models

```kotlin
// Export learned model for backup
val modelData = smartScheduler.exportModel()
// Save to file or cloud

// Import model on new device
smartScheduler.importModel(modelData)
```

## ML Model Training

### User Habit Model

The user habit model uses **unsupervised learning** to discover patterns:

1. **Data Collection**: Collects up to 1000 backup samples
2. **Feature Extraction**: Extracts temporal and contextual features
3. **Clustering**: Groups samples by day/time buckets
4. **Pattern Recognition**: Identifies recurring patterns (≥3 occurrences)
5. **Confidence Calculation**: Computes pattern consistency (>0.8 required)

### TensorFlow Lite Model

The TFLite predictor uses a **simple neural network**:

**Architecture** (recommended):
```
Input Layer:    7 features (normalized)
Hidden Layer 1: 16 neurons, ReLU activation
Hidden Layer 2: 8 neurons, ReLU activation
Output Layer:   1 neuron, Sigmoid activation (0-1 probability)
```

**Training Data**:
- Historical backup events
- Context at backup time
- Success/failure labels
- Continuous learning from new data

**Model File**: `assets/ml_models/backup_predictor.tflite`

**Current model** (trained 2026-02-20):
- Architecture: Dense(32, relu) → Dropout(0.2) → Dense(16, relu) → Dense(1, sigmoid)
- Input features: hour, day_of_week, battery_level, charging, wifi_connected, location, activity, time_since_backup (8 features, float32)
- AUC: **0.86** · Size: **5.3 KB** · Inference: **<1 ms**
- Scaler params: `assets/ml_models/scaler_params.json`

To retrain with updated data:
1. Collect backup event data (BackupAnalytics)
2. Export to training dataset
3. Run: `cd ml_training && python train_backup_predictor.py`
4. Copy output to `app/src/main/assets/ml_models/`

## Performance Considerations

### Memory Usage
- **UserHabitModel**: ~500KB (1000 samples)
- **BackupAnalytics**: ~1MB (500 events + 1000 activity records)
- **TFLite Model**: ~50KB (small neural network)
- **Total**: ~2MB in memory when active

### CPU Usage
- **Pattern extraction**: ~100ms (on-demand)
- **TFLite inference**: ~10ms per prediction
- **Context monitoring**: ~5ms per update
- **NLP parsing**: ~20ms per query

### Battery Impact
- **Background monitoring**: Minimal (<1% per day)
- **Periodic learning**: Every 6 hours during idle
- **Anomaly detection**: Every 1 hour (lightweight)

### Storage Usage
- **Model files**: ~100KB on disk
- **Analytics data**: ~500KB (compressed JSON)
- **Total**: ~600KB persistent storage

## Configuration

### Confidence Thresholds

```kotlin
// Minimum confidence to act on prediction
const val MIN_CONFIDENCE_THRESHOLD = 0.7f

// High confidence for automatic scheduling
const val HIGH_CONFIDENCE_THRESHOLD = 0.85f
```

### Learning Parameters

```kotlin
// Minimum samples before making predictions
const val MIN_SAMPLES_FOR_PREDICTION = 10

// Maximum samples to keep in memory
const val MAX_SAMPLES = 1000

// Minimum pattern occurrences
const val MIN_PATTERN_OCCURRENCES = 3

// Pattern similarity threshold
const val PATTERN_SIMILARITY_THRESHOLD = 0.8f
```

### Anomaly Detection

```kotlin
// Standard deviations for anomaly threshold
const val ANOMALY_THRESHOLD = 2.5f

// Minimum samples for anomaly detection
const val MIN_SAMPLES_FOR_ANOMALY = 5
```

## Privacy & Security

### Data Privacy
- ✅ **All ML runs on-device** - no cloud processing
- ✅ **No telemetry** - no data collection or transmission
- ✅ **No PII** - only app IDs and timestamps stored
- ✅ **Local storage** - models saved in app private directory
- ✅ **Encrypted backups** - model exports can be encrypted

### Permissions Required
```xml
<!-- Optional: For better activity detection -->
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />

<!-- For network state monitoring -->
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### Data Collected
The ML system only stores:
- App IDs (package names)
- Backup timestamps
- Backup success/failure
- Device context (battery, WiFi, etc.) - **no GPS coordinates**
- File change counts (numbers only, not filenames)

### GDPR Compliance
- ✅ Right to erasure: `smartScheduler.resetModels()`
- ✅ Data portability: `smartScheduler.exportModel()`
- ✅ Transparent processing: Open source, auditable code
- ✅ Purpose limitation: Only used for backup optimization

## Troubleshooting

### Model Not Learning
**Problem**: ML model not making predictions after several backups.

**Solutions**:
1. Ensure at least 10 backup samples collected
2. Check model stats: `smartScheduler.getModelStats()`
3. Verify backups are being recorded: `learnFromBackup()` called
4. Check logs for initialization errors

### Low Prediction Confidence
**Problem**: Predictions have low confidence scores (<0.7).

**Solutions**:
1. Need more training data (perform more backups)
2. Backup patterns may be inconsistent (vary time/context)
3. Check context availability (battery, network monitoring)
4. Lower threshold temporarily for testing

### NLP Queries Not Working
**Problem**: Natural language queries return errors.

**Solutions**:
1. Ensure query contains "backup" keyword
2. Include app identifier (name or category)
3. Check supported patterns in NaturalLanguageProcessor
4. Validate query: `nlpProcessor.validateQuery()`

### High Memory Usage
**Problem**: App using excessive memory.

**Solutions**:
1. Reduce MAX_SAMPLES (default: 1000)
2. Reduce MAX_EVENTS in BackupAnalytics (default: 500)
3. Call `smartScheduler.resetModels()` to clear old data
4. Disable anomaly detection if not needed

## Future Enhancements

### Planned Features
- [ ] **Federated Learning**: Learn from patterns across devices (privacy-preserving)
- [ ] **Advanced NLP**: Support more complex queries with ML Kit Natural Language
- [ ] **Predictive Restoration**: Suggest which apps to restore based on usage
- [ ] **Multi-Device Sync**: Sync learned patterns across user's devices
- [ ] **App Usage Prediction**: Predict which apps user will need next
- [ ] **Backup Size Prediction**: Accurate size estimation using historical data
- [ ] **Network Bandwidth Prediction**: Optimize cloud sync timing
- [ ] **Custom ML Models**: Allow users to train custom models

### ML Model Improvements
- [ ] **LSTM Networks**: Better temporal pattern recognition
- [ ] **Attention Mechanisms**: Focus on important context signals
- [ ] **Transfer Learning**: Pre-trained models for faster convergence
- [ ] **Online Learning**: Continuous model updates without retraining
- [ ] **Ensemble Methods**: Combine multiple models for better accuracy

## API Reference

See inline documentation in source files for detailed API reference:
- `SmartScheduler.kt` - Main API
- `UserHabitModel.kt` - Pattern learning
- `BackupPredictor.kt` - ML predictions
- `ContextAwareManager.kt` - Context monitoring
- `NaturalLanguageProcessor.kt` - NLP parsing
- `BackupAnalytics.kt` - Event tracking

## Contributing

To contribute to ML features:

1. **Test with real data**: Use app for several weeks to generate training data
2. **Evaluate accuracy**: Track prediction accuracy and report issues
3. **Propose improvements**: Submit ML model enhancements
4. **Add training scripts**: Help create TFLite model training pipeline
5. **Improve NLP**: Expand natural language understanding

## License

Same as ObsidianBackup project license.

## Support

For ML-related issues:
1. Check logs with tag `SmartScheduler`, `BackupPredictor`, etc.
2. Export analytics: `smartScheduler.getStatistics()`
3. Report model performance issues with sample data
4. Include model stats in bug reports

---

**Built with ❤️ using Google ML Kit and TensorFlow Lite**

*"Backup smarter, not harder - let AI learn when you need it most."*
