# Health Connect Integration Guide

## Overview

ObsidianBackup now supports backing up fitness and health data through Google's Health Connect platform. This integration enables users to securely backup, restore, and export their health records with full privacy controls.

## Features

### Supported Data Types

1. **Steps** - Daily step count and walking activity
2. **Heart Rate** - Heart rate measurements and trends
3. **Sleep** - Sleep sessions, stages, and quality metrics
4. **Workouts** - Exercise sessions and activity records
5. **Nutrition** - Meal logs and nutritional information
6. **Body Measurements** - Weight, height, body fat percentage

### Key Capabilities

- ✅ **Incremental Sync** - Only backup new records since last backup
- ✅ **Privacy Controls** - Granular control over which data types to backup
- ✅ **Multiple Export Formats** - JSON and CSV export options
- ✅ **Data Anonymization** - Optional anonymization for privacy
- ✅ **Encrypted Storage** - All health backups are encrypted locally
- ✅ **Offline Operation** - No cloud required for local backups
- ✅ **Restore Capability** - Full restore of health data

## Architecture

### Components

```
health/
├── HealthConnectManager.kt       # Main manager for health operations
├── HealthDataExporter.kt         # Export/import to JSON/CSV
├── HealthDataStore.kt            # Local storage and preferences
├── HealthPrivacyScreen.kt        # Privacy controls UI
└── HealthPrivacyViewModel.kt     # UI state management
```

### Flow Diagram

```
User → HealthPrivacyScreen → HealthPrivacyViewModel
                                      ↓
                              HealthConnectManager
                                      ↓
                    ┌─────────────────┴─────────────────┐
                    ↓                                   ↓
            HealthDataExporter                  HealthDataStore
                    ↓                                   ↓
            Local Files (JSON/CSV)              DataStore Preferences
```

## Usage

### 1. Setup

Health Connect integration is automatically configured through Hilt dependency injection. No manual setup required.

### 2. Check Availability

```kotlin
@Inject
lateinit var healthConnectManager: HealthConnectManager

suspend fun checkHealthConnect() {
    val available = healthConnectManager.isHealthConnectAvailable()
    if (available) {
        // Health Connect is ready
    } else {
        // Prompt user to install Health Connect
    }
}
```

### 3. Request Permissions

```kotlin
val dataTypes = setOf(
    HealthDataType.STEPS,
    HealthDataType.HEART_RATE,
    HealthDataType.SLEEP
)

val requiredPermissions = healthConnectManager.getRequiredPermissions(dataTypes)
// Use ActivityResultContracts to request permissions
```

### 4. Backup Health Data

```kotlin
suspend fun backupHealthData() {
    val dataTypes = setOf(
        HealthDataType.STEPS,
        HealthDataType.HEART_RATE,
        HealthDataType.SLEEP,
        HealthDataType.WORKOUTS,
        HealthDataType.NUTRITION,
        HealthDataType.BODY_MEASUREMENTS
    )
    
    val result = healthConnectManager.backupHealthData(
        dataTypes = dataTypes,
        exportFormat = ExportFormat.JSON
    )
    
    when {
        result.isSuccess -> {
            val backupResult = result.getOrNull()
            println("Backed up ${backupResult?.totalRecords} records")
        }
        result.isFailure -> {
            println("Backup failed: ${result.exceptionOrNull()?.message}")
        }
    }
}
```

### 5. Restore Health Data

```kotlin
suspend fun restoreHealthData(backupPath: String) {
    val dataTypes = setOf(
        HealthDataType.STEPS,
        HealthDataType.HEART_RATE
    )
    
    val result = healthConnectManager.restoreHealthData(
        backupPath = backupPath,
        dataTypes = dataTypes
    )
    
    when {
        result.isSuccess -> {
            val restoreResult = result.getOrNull()
            println("Restored ${restoreResult?.totalRecordsRestored} records")
        }
        result.isFailure -> {
            println("Restore failed: ${result.exceptionOrNull()?.message}")
        }
    }
}
```

### 6. Export to Standard Formats

```kotlin
// Export to JSON
suspend fun exportToJSON() {
    val result = healthConnectManager.exportToFormat(
        dataTypes = setOf(HealthDataType.STEPS, HealthDataType.HEART_RATE),
        format = ExportFormat.JSON,
        outputPath = "/path/to/export.json"
    )
}

// Export to CSV
suspend fun exportToCSV() {
    val result = healthConnectManager.exportToFormat(
        dataTypes = setOf(HealthDataType.STEPS),
        format = ExportFormat.CSV,
        outputPath = "/path/to/export.csv"
    )
}
```

### 7. Privacy Settings

```kotlin
// Update privacy settings
val privacySettings = HealthPrivacySettings(
    enabledDataTypes = setOf(
        HealthDataType.STEPS,
        HealthDataType.HEART_RATE
    ),
    anonymizeData = true,
    excludeSensitiveData = true,
    retentionDays = 365  // Keep data for 1 year
)

healthConnectManager.updatePrivacySettings(privacySettings)
```

### 8. Get Backup Statistics

```kotlin
suspend fun getStatistics() {
    val stats = healthConnectManager.getBackupStatistics()
    println("Last backup: ${stats.lastBackupTime}")
    println("Total size: ${stats.totalBackupSize} bytes")
    println("Record counts: ${stats.recordCountsByType}")
}
```

## Privacy & Security

### Data Protection

1. **Local Storage Only** - All health backups are stored locally in app-private `filesDir` (never external storage)
2. **Encryption** - Data written via `EncryptedFile` (AES256_GCM_HKDF_4KB scheme) — PHI never touches disk unencrypted
3. **No Cloud Upload** - Health data is never automatically uploaded to cloud (unless user explicitly exports)
4. **Granular Control** - Users control exactly which data types are backed up
5. **HIPAA Audit Logging** - `SecurityAuditLogger.logPhiAccess()` records every PHI read/write with timestamp, userId, and compliance flag

### Privacy Controls

The `HealthPrivacyScreen` provides:

- **Data Type Selection** - Enable/disable specific health data types
- **Anonymization** - Option to remove personally identifiable information
- **Sensitive Data Exclusion** - Skip potentially sensitive health metrics
- **Retention Policy** - Auto-delete old backups after specified days

### Compliance

- ✅ **HIPAA Ready** - Designed with health data privacy in mind
- ✅ **GDPR Compatible** - Users have full control over their data
- ✅ **Transparent** - Clear privacy notices and controls
- ✅ **Audit Trail** - All operations are logged for accountability

## API Reference

### HealthConnectManager

#### Methods

| Method | Description | Returns |
|--------|-------------|---------|
| `isHealthConnectAvailable()` | Check if Health Connect is available | `Boolean` |
| `getRequiredPermissions(dataTypes)` | Get permissions needed for data types | `Set<String>` |
| `backupHealthData(dataTypes, format)` | Backup health data | `Result<HealthBackupResult>` |
| `restoreHealthData(backupPath, dataTypes)` | Restore health data | `Result<HealthRestoreResult>` |
| `exportToFormat(dataTypes, format, outputPath)` | Export to JSON/CSV | `Result<String>` |
| `updatePrivacySettings(settings)` | Update privacy preferences | `Unit` |
| `getBackupStatistics()` | Get backup stats | `HealthBackupStatistics` |
| `deleteAllHealthBackups()` | Delete all health backups | `Result<Unit>` |

### Data Models

#### HealthDataType

```kotlin
enum class HealthDataType {
    STEPS,
    HEART_RATE,
    SLEEP,
    WORKOUTS,
    NUTRITION,
    BODY_MEASUREMENTS
}
```

#### ExportFormat

```kotlin
enum class ExportFormat {
    JSON,
    CSV
}
```

#### HealthPrivacySettings

```kotlin
data class HealthPrivacySettings(
    val enabledDataTypes: Set<HealthDataType>,
    val anonymizeData: Boolean,
    val excludeSensitiveData: Boolean,
    val retentionDays: Int  // 0 = keep forever
)
```

## File Formats

### JSON Export Format

```json
{
  "dataType": "STEPS",
  "exportTime": "2024-01-15T10:30:00Z",
  "records": [
    {
      "count": 8542,
      "startTime": "2024-01-15T00:00:00Z",
      "endTime": "2024-01-15T23:59:59Z",
      "metadata": "record_id_123"
    }
  ]
}
```

### CSV Export Format

```csv
timestamp,count,start_time,end_time
1705315800000,8542,2024-01-15T00:00:00Z,2024-01-15T23:59:59Z
1705402200000,9123,2024-01-16T00:00:00Z,2024-01-16T23:59:59Z
```

## Error Handling

### Common Errors

| Error | Cause | Solution |
|-------|-------|----------|
| `Health Connect not available` | Health Connect not installed | Prompt user to install Health Connect app |
| `Permission denied` | User denied health permissions | Request permissions again with explanation |
| `Backup failed` | Storage full or corrupted | Check storage space, clear old backups |
| `Restore failed` | Invalid backup format | Verify backup file integrity |

### Example Error Handling

```kotlin
try {
    val result = healthConnectManager.backupHealthData(dataTypes, format)
    if (result.isFailure) {
        when (val exception = result.exceptionOrNull()) {
            is SecurityException -> {
                // Permission denied - request permissions
            }
            is IllegalStateException -> {
                // Health Connect not available
            }
            else -> {
                // Generic error - log and notify user
            }
        }
    }
} catch (e: Exception) {
    logger.e("Health backup failed", e)
}
```

## Best Practices

### Performance

1. **Incremental Backups** - Use incremental sync to only backup new data
2. **Background Processing** - Run backups in WorkManager for reliability
3. **Batch Operations** - Group multiple data type backups together
4. **Compression** - Use compressed formats for large datasets

### User Experience

1. **Clear Communication** - Explain why health permissions are needed
2. **Progress Indicators** - Show progress during long-running operations
3. **Error Recovery** - Provide clear error messages and recovery options
4. **Privacy First** - Make privacy controls prominent and easy to use

### Data Management

1. **Regular Backups** - Schedule automatic backups (e.g., weekly)
2. **Retention Policy** - Implement automatic cleanup of old backups
3. **Verification** - Verify backup integrity after completion
4. **Export Options** - Provide multiple export formats for flexibility

## Integration Checklist

- [x] Add Health Connect dependency to build.gradle.kts
- [x] Add health permissions to AndroidManifest.xml
- [x] Create HealthConnectManager for core operations
- [x] Implement HealthDataExporter for JSON/CSV export
- [x] Create HealthDataStore for local persistence
- [x] Build HealthPrivacyScreen for UI controls
- [x] Add providers to DI module (AppModule)
- [x] Add Health Connect queries to manifest
- [x] Document API and usage patterns
- [x] Implement error handling and logging

## Testing

### Manual Testing

1. Install Health Connect from Play Store
2. Grant health permissions to ObsidianBackup
3. Add sample health data in Health Connect
4. Open ObsidianBackup → Health Privacy
5. Enable desired data types
6. Click "Backup All Health Data"
7. Verify backup files created in `/backups/health_data/`
8. Test export to JSON and CSV
9. Test restore functionality
10. Verify privacy settings work correctly

### Automated Testing

```kotlin
@Test
fun testHealthDataBackup() = runTest {
    val manager = HealthConnectManager(context, logger, exporter, store)
    
    val result = manager.backupHealthData(
        dataTypes = setOf(HealthDataType.STEPS),
        exportFormat = ExportFormat.JSON
    )
    
    assertTrue(result.isSuccess)
    assertNotNull(result.getOrNull()?.totalRecords)
}
```

## Troubleshooting

### Health Connect Not Available

**Problem**: `isHealthConnectAvailable()` returns false

**Solutions**:
- Ensure Health Connect is installed
- Check device runs Android 9+ (API 28+)
- Verify app has correct permissions in manifest

### No Data Backed Up

**Problem**: Backup completes but 0 records

**Solutions**:
- Check Health Connect app has data
- Verify correct permissions granted
- Check time range filter settings
- Enable data types in privacy settings

### Export Failed

**Problem**: Export to JSON/CSV fails

**Solutions**:
- Check storage permissions
- Verify output directory exists
- Check available storage space
- Review logs for specific error

## Future Enhancements

- [ ] Cloud sync for health backups
- [ ] Advanced analytics and insights
- [ ] Health data visualization
- [ ] Integration with fitness devices
- [ ] Automated anomaly detection
- [ ] ML-based health predictions
- [ ] Sharing with healthcare providers
- [ ] Import from other fitness apps

## Support

For issues or questions:
- Check logs in `/logs/app_logs/`
- Review privacy settings
- Verify Health Connect app is up to date
- Contact support with log files

## References

- [Health Connect Documentation](https://developer.android.com/health-and-fitness/guides/health-connect)
- [Health Connect API Reference](https://developer.android.com/reference/kotlin/androidx/health/connect/client/package-summary)
- [Privacy Best Practices](https://developer.android.com/health-and-fitness/guides/health-connect/develop/privacy)

---

**Version**: 1.0.0  
**Last Updated**: 2024-01-15  
**Maintainer**: ObsidianBackup Team
