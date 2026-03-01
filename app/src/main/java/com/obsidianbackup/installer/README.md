# Installer Package

The `installer` package provides specialized tools for installing Android applications, with comprehensive support for split APKs.

## Overview

Split APKs (Android App Bundles) are the modern app distribution format that allows Google Play to deliver optimized APKs for each device configuration. This package handles the complexity of backing up and restoring these split APKs with proper progress tracking and error handling.

## Components

### SplitApkHelper

**Location**: `com.obsidianbackup.installer.SplitApkHelper`

**Purpose**: Detection and backup of split APKs

**Key Features**:
- Detect if an app uses split APKs
- Extract split APK metadata (base + splits)
- Backup all APKs to a directory structure
- Device compatibility checking (ABI, density)
- Split filtering for optimal restore

**Usage Example**:
```kotlin
val helper = SplitApkHelper(packageManager, shellExecutor, logger)

// Check if app uses splits
if (helper.isSplitApk("com.example.app")) {
    // Get split information
    val metadata = helper.getSplitApkInfo("com.example.app")
    
    // Backup all APKs
    val totalSize = helper.backupApks(metadata, targetDir)
    
    // Later: restore
    val success = helper.restoreApks(targetDir, "com.example.app")
}
```

### SplitApkInstaller

**Location**: `com.obsidianbackup.installer.SplitApkInstaller`

**Purpose**: Advanced installation with progress tracking and rollback

**Key Features**:
- Progress reporting via StateFlow
- Automatic rollback on failure
- Detailed error messages with phase tracking
- Support for both single and split APKs
- Session management for split installations

**Progress Tracking**:
```kotlin
val installer = SplitApkInstaller(shellExecutor, logger)

// Observe progress
lifecycleScope.launch {
    installer.progress.collect { progress ->
        println("Phase: ${progress.phase}")
        println("Split: ${progress.currentSplit} (${progress.splitIndex}/${progress.totalSplits})")
        println("Progress: ${progress.percentComplete}%")
        println("Bytes: ${progress.bytesProcessed}/${progress.totalBytes}")
    }
}

// Install split APKs
val result = installer.installSplitApks(baseApk, splitApks, packageName)

when (result) {
    is InstallResult.Success -> {
        println("Installed ${result.splitsInstalled} splits for ${result.packageName}")
    }
    is InstallResult.Failure -> {
        println("Failed at phase ${result.phase}: ${result.error}")
        println("Rollback successful: ${result.rollbackSuccessful}")
    }
}
```

**Installation from Directory**:
```kotlin
// Auto-detects split vs single APK
val result = installer.installApksFromDirectory(apkDir, packageName)
```

## Data Models

### SplitApkMetadata

Serializable metadata for split APK backups:

```kotlin
@Serializable
data class SplitApkMetadata(
    val packageName: String,
    val isSplit: Boolean,
    val baseApkPath: String,
    val splitNames: List<String> = emptyList(),
    val splitPaths: List<String> = emptyList()
)
```

Saved as `apk_metadata.json` in backup directories.

### SplitApkInfo

Information about a single split:

```kotlin
data class SplitApkInfo(
    val name: String,
    val path: String,
    val size: Long
)
```

### InstallProgress

Real-time installation progress:

```kotlin
data class InstallProgress(
    val currentSplit: String,        // Current APK being processed
    val splitIndex: Int,              // 0-based index
    val totalSplits: Int,             // Total APK count
    val bytesProcessed: Long,         // Bytes written so far
    val totalBytes: Long,             // Total bytes to write
    val percentComplete: Int,         // 0-100
    val phase: InstallPhase           // Current phase
)
```

### InstallPhase

Installation lifecycle phases:

```kotlin
enum class InstallPhase {
    PREPARING,        // Validating files, checking paths
    CREATING_SESSION, // Creating pm install session
    WRITING_APKS,     // Writing APK files to session
    COMMITTING,       // Finalizing installation
    COMPLETE,         // Successfully installed
    FAILED,           // Installation failed
    ROLLING_BACK      // Abandoning session, cleaning up
}
```

### InstallResult

Installation outcome:

```kotlin
sealed class InstallResult {
    data class Success(
        val packageName: String,
        val splitsInstalled: Int
    ) : InstallResult()
    
    data class Failure(
        val error: String,
        val phase: InstallPhase,
        val packageName: String?,
        val rollbackSuccessful: Boolean = false
    ) : InstallResult()
}
```

## Architecture Integration

### Backup Flow

```
BackupOrchestrator
  └─> ObsidianBoxEngine
       └─> SplitApkHelper
            ├─> getSplitApkInfo()
            └─> backupApks()
                 └─> SafeShellExecutor (cp commands)
```

### Restore Flow

```
RestoreOrchestrator
  └─> ObsidianBoxEngine
       └─> SplitApkHelper
            └─> restoreApks()
                 └─> SplitApkInstaller
                      ├─> installSplitApks()
                      │    └─> SafeShellExecutor (pm commands)
                      └─> rollbackAndFail() [on error]
```

## Split APK Types

### ABI Splits

Architecture-specific native code:
- `armeabi-v7a` - 32-bit ARM
- `arm64-v8a` - 64-bit ARM
- `x86` - 32-bit x86
- `x86_64` - 64-bit x86

**Device Compatibility**: Only restore splits matching device ABIs.

### Density Splits

Screen density resources:
- `ldpi`, `mdpi`, `hdpi`
- `xhdpi`, `xxhdpi`, `xxxhdpi`
- `tvdpi`

**Device Compatibility**: Generally optional, all can be restored.

### Language Splits

Localization resources:
- `config.en` - English
- `config.es` - Spanish
- `config.fr` - French
- etc.

**Device Compatibility**: All compatible, restore based on user preference.

### Feature Splits

Dynamic feature modules:
- `config.xxxx` - Various features
- App-specific feature modules

**Device Compatibility**: Depends on app requirements.

## Error Handling

### Rollback Mechanism

When installation fails, SplitApkInstaller automatically:

1. Identifies the failure phase
2. Calls `pm install-abandon <sessionId>` to clean up
3. Deletes any partially installed splits
4. Returns detailed error information
5. Reports rollback success/failure

### Common Errors

**Base APK not found**:
```kotlin
InstallResult.Failure(
    error = "Base APK not found in /path/to/backup",
    phase = InstallPhase.PREPARING,
    packageName = "com.example.app"
)
```

**Session creation failed**:
```kotlin
InstallResult.Failure(
    error = "Failed to create install session: Insufficient storage",
    phase = InstallPhase.CREATING_SESSION,
    packageName = "com.example.app"
)
```

**Split write failed**:
```kotlin
InstallResult.Failure(
    error = "Failed to write APK 'config.arm64_v8a.apk' to session: I/O error",
    phase = InstallPhase.WRITING_APKS,
    packageName = "com.example.app",
    rollbackSuccessful = true
)
```

**Commit failed**:
```kotlin
InstallResult.Failure(
    error = "Failed to commit install session: INSTALL_FAILED_VERSION_DOWNGRADE",
    phase = InstallPhase.COMMITTING,
    packageName = "com.example.app",
    rollbackSuccessful = true
)
```

## Backup Directory Structure

```
backup_root/
└── snapshot_id/
    └── apps/
        └── com.example.app/
            ├── apk_metadata.json      # Metadata
            ├── base.apk               # Base APK
            ├── config.arm64_v8a.apk   # ABI split
            ├── config.xxhdpi.apk      # Density split
            ├── config.en.apk          # Language split
            └── ...                    # Other splits
```

## Performance Considerations

### Backup Performance

- **Parallel copying**: Uses shell `cp` commands (fast)
- **Minimal overhead**: Direct file system operations
- **Size tracking**: Real-time progress via file sizes

### Restore Performance

- **Session-based**: Uses PackageManager sessions (Android standard)
- **Atomic**: All-or-nothing installation
- **Progress granular**: Per-split progress reporting
- **Rollback fast**: Session abandonment is immediate

## Testing

### Unit Tests

Location: `app/src/test/java/com/obsidianbackup/installer/`

Tests cover:
- Split detection logic
- Metadata parsing
- Device compatibility checking
- Session ID extraction
- Error handling

### Integration Tests

Location: `app/src/androidTest/java/com/obsidianbackup/installer/`

Tests cover:
- Full backup/restore cycle
- Progress callback behavior
- Rollback mechanism
- Real device installation

## Dependencies

### Direct Dependencies
- `SafeShellExecutor` - Shell command execution
- `ObsidianLogger` - Logging
- `PackageManager` - App information
- Kotlin Coroutines - Async operations
- kotlinx.serialization - JSON serialization

### Transitive Dependencies
- Android SDK APIs (Build, ApplicationInfo)
- Kotlin standard library

## Security Considerations

### Shell Command Safety

All shell commands use proper escaping:
```kotlin
val escapedPath = "'${path.replace("'", "'\\''")}'"
```

### Permission Requirements

- `WRITE_EXTERNAL_STORAGE` - Backup to external storage
- Root/shell access - `pm install` commands
- `QUERY_ALL_PACKAGES` - Android 11+ app querying

### Data Privacy

- APK files are copied, not moved
- Original APKs remain untouched
- Metadata is JSON (human-readable)
- No network access

## Migration from Legacy

### From Old SplitApkHelper (engine package)

**Before**:
```kotlin
// Old location
import com.obsidianbackup.engine.SplitApkHelper

val helper = SplitApkHelper(...)
val success = helper.restoreApks(dir, pkg)
```

**After**:
```kotlin
// New location
import com.obsidianbackup.installer.SplitApkHelper
import com.obsidianbackup.installer.SplitApkInstaller

val helper = SplitApkHelper(...)

// Option 1: Use helper (simple)
val success = helper.restoreApks(dir, pkg)

// Option 2: Use installer directly (with progress)
val installer = helper.getInstaller()
installer.progress.collect { /* update UI */ }
val result = installer.installApksFromDirectory(dir, pkg)
```

## Roadmap

### Planned Features

- [ ] Parallel split uploads to cloud storage
- [ ] Selective split restore (user choice)
- [ ] Split signature verification
- [ ] OBB (expansion file) support
- [ ] Delta updates for split APKs
- [ ] Compression for backup storage

### Known Limitations

- Requires Android 5.0+ (API 21) for split APK support
- Root/shell access required for `pm` commands
- Large apps may take significant time
- Storage space must be sufficient for temporary session

## Troubleshooting

### Issue: "Base APK not found"

**Cause**: Backup incomplete or corrupted

**Solution**: Re-backup the app, verify storage integrity

### Issue: "Failed to create install session"

**Cause**: Insufficient storage, permissions

**Solution**: Free up space, check root access

### Issue: Rollback failed after install failure

**Cause**: Session already abandoned, system issue

**Solution**: Manual cleanup via `pm list sessions` and `pm install-abandon`

### Issue: Progress stuck at 0%

**Cause**: APK write taking long time

**Solution**: Wait, check logcat for detailed progress

## References

- [Android App Bundles](https://developer.android.com/guide/app-bundle)
- [PackageInstaller API](https://developer.android.com/reference/android/content/pm/PackageInstaller)
- [Split APKs Overview](https://developer.android.com/studio/build/configure-apk-splits)

---

**Last Updated**: Feb 2024
**Maintainer**: ObsidianBackup Team
