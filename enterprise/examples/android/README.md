# Android Enterprise Client SDK

This directory contains the Android SDK for integrating with ObsidianBackup Enterprise Edition.

## Files

- **EnterpriseClient.kt** - Main SDK implementation
  - Device registration
  - Policy synchronization
  - Compliance checking
  - Remote wipe handling
  - Backup reporting

## Usage

```kotlin
// Initialize client
val client = EnterpriseClient(
    baseUrl = "https://enterprise.obsidianbackup.com",
    apiKey = "your-api-key"
)

// Register device
val device = client.registerDevice(
    deviceName = android.os.Build.MODEL,
    osVersion = android.os.Build.VERSION.RELEASE,
    appVersion = "1.0.0"
)

// Get policies
val policies = client.getDevicePolicies(device.id)

// Report backup
client.reportBackup(
    BackupReport(
        deviceId = device.id,
        backupId = "backup-123",
        status = "COMPLETED",
        bytesBackedUp = 1024000,
        filesBackedUp = 50
    )
)
```

## Integration Steps

1. Copy `EnterpriseClient.kt` to your Android project
2. Add dependencies to `build.gradle`:
   ```gradle
   implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
   implementation "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2"
   ```
3. Initialize client in your Application class
4. Register device on first launch
5. Sync policies periodically
6. Report backups to backend

## API Documentation

See parent directory `ENTERPRISE_EDITION.md` for complete API reference.
