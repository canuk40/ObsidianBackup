# Health Connect Quick Start

## 5-Minute Setup Guide

### 1. Prerequisites

- Android 9+ (API 28+)
- Health Connect app installed
- Health permissions granted

### 2. Add to Navigation (Quick Example)

```kotlin
// In Navigation.kt, add route:
composable("health_privacy") {
    HealthPrivacyScreen(
        onNavigateBack = { navController.navigateUp() }
    )
}

// Add menu item to navigate:
Button(onClick = { navController.navigate("health_privacy") }) {
    Text("Health Data Privacy")
}
```

### 3. Quick Backup Example

```kotlin
// Inject the manager
@Inject
lateinit var healthConnectManager: HealthConnectManager

// Backup all data types
suspend fun quickBackup() {
    val allTypes = setOf(
        HealthDataType.STEPS,
        HealthDataType.HEART_RATE,
        HealthDataType.SLEEP,
        HealthDataType.WORKOUTS,
        HealthDataType.NUTRITION,
        HealthDataType.BODY_MEASUREMENTS
    )
    
    healthConnectManager.backupHealthData(
        dataTypes = allTypes,
        exportFormat = ExportFormat.JSON
    )
}
```

### 4. Quick Export Example

```kotlin
// Export to JSON
healthConnectManager.exportToFormat(
    dataTypes = setOf(HealthDataType.STEPS),
    format = ExportFormat.JSON,
    outputPath = "health_export.json"
)

// Export to CSV
healthConnectManager.exportToFormat(
    dataTypes = setOf(HealthDataType.STEPS),
    format = ExportFormat.CSV,
    outputPath = "health_export.csv"
)
```

## Files Overview

| File | Purpose | Lines |
|------|---------|-------|
| HealthConnectManager.kt | Core backup/restore logic | ~500 |
| HealthDataExporter.kt | JSON/CSV export | ~400 |
| HealthDataStore.kt | Local storage | ~150 |
| HealthPrivacyScreen.kt | Privacy UI | ~500 |
| HealthPrivacyViewModel.kt | UI state | ~150 |

## Key Features at a Glance

✅ **6 Data Types**: Steps, Heart Rate, Sleep, Workouts, Nutrition, Body Measurements  
✅ **2 Export Formats**: JSON, CSV  
✅ **Incremental Sync**: Only new data since last backup  
✅ **Privacy Controls**: Granular data type selection  
✅ **Encrypted**: All data encrypted at rest  
✅ **Offline**: No cloud required  

## Common Use Cases

### Daily Backup (Automated)

```kotlin
// In WorkManager
class HealthBackupWorker @Inject constructor(
    private val healthConnectManager: HealthConnectManager
) : CoroutineWorker() {
    override suspend fun doWork(): Result {
        val backupResult = healthConnectManager.backupHealthData(
            dataTypes = HealthDataType.values().toSet(),
            exportFormat = ExportFormat.JSON
        )
        return if (backupResult.isSuccess) Result.success() else Result.retry()
    }
}
```

### User-Initiated Export

```kotlin
// One-time export to share with doctor
Button(onClick = {
    scope.launch {
        val path = healthConnectManager.exportToFormat(
            dataTypes = setOf(HealthDataType.HEART_RATE, HealthDataType.SLEEP),
            format = ExportFormat.CSV,
            outputPath = "health_report_${Date().time}.csv"
        )
        // Share file...
    }
}) {
    Text("Export Health Report")
}
```

### Privacy-Conscious Backup

```kotlin
// Backup with anonymization
healthConnectManager.updatePrivacySettings(
    HealthPrivacySettings(
        enabledDataTypes = setOf(HealthDataType.STEPS),
        anonymizeData = true,
        excludeSensitiveData = true,
        retentionDays = 90
    )
)
```

## Permissions

Add to your activity or fragment:

```kotlin
val healthPermissions = healthConnectManager.getRequiredPermissions(
    setOf(HealthDataType.STEPS, HealthDataType.HEART_RATE)
)

// Use ActivityResultContract
val requestPermissions = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
) { permissions ->
    if (permissions.all { it.value }) {
        // All permissions granted
    }
}

requestPermissions.launch(healthPermissions.toTypedArray())
```

## Storage Locations

- **Backups**: `/data/data/com.obsidianbackup/files/backups/health_data/`
- **Exports**: User-selected location
- **Preferences**: DataStore (`health_settings`)

## Error Handling Cheat Sheet

```kotlin
try {
    val result = healthConnectManager.backupHealthData(...)
    when {
        result.isSuccess -> showSuccess()
        result.isFailure -> {
            when (val e = result.exceptionOrNull()) {
                is SecurityException -> requestPermissions()
                is IllegalStateException -> promptInstallHealthConnect()
                else -> showGenericError(e?.message)
            }
        }
    }
} catch (e: Exception) {
    logger.e("Backup failed", e)
}
```

## Troubleshooting

| Issue | Fix |
|-------|-----|
| "Health Connect not available" | Install Health Connect from Play Store |
| "Permission denied" | Request health permissions |
| "0 records backed up" | Check Health Connect has data |
| "Backup failed" | Check storage space |

## Testing Checklist

- [ ] Health Connect installed
- [ ] Permissions granted
- [ ] Sample data in Health Connect
- [ ] Navigate to Health Privacy screen
- [ ] Enable data types
- [ ] Run backup
- [ ] Verify files created
- [ ] Test export JSON/CSV
- [ ] Test restore
- [ ] Test privacy settings

## Next Steps

1. Read full [HEALTH_CONNECT_INTEGRATION.md](./HEALTH_CONNECT_INTEGRATION.md)
2. Add navigation route to your app
3. Test with sample Health Connect data
4. Implement WorkManager for scheduled backups
5. Add export sharing functionality

---

**Questions?** Check the main integration guide or logs at `/logs/app_logs/`.
