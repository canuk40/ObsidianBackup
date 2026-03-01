# Scoped Storage Migration Guide

## Overview

ObsidianBackup has been migrated to use **scoped storage** patterns to comply with Android 10+ (API 29+) requirements and prepare for Android 14/15. This migration eliminates the reliance on `MANAGE_EXTERNAL_STORAGE` permission for normal backup operations.

## Architecture Changes

### Storage Strategy

#### Before Migration
- ❌ Required `MANAGE_EXTERNAL_STORAGE` for all operations
- ❌ Used public external storage directories
- ❌ Direct `File` API access to shared storage
- ❌ Non-compliant with Android 11+ scoped storage

#### After Migration
- ✅ **Primary Storage**: App-private external files (`getExternalFilesDir()`)
  - No permissions required
  - Automatically cleaned up on uninstall
  - Located at: `/Android/data/com.obsidianbackup/files/backups/`
  
- ✅ **User Exports**: Storage Access Framework (SAF)
  - User explicitly chooses export location
  - Persistent permissions
  - Scoped storage compliant
  
- ✅ **Shared Storage**: MediaStore API (Android 10+)
  - For sharing backups via file managers
  - Proper content:// URIs
  - No special permissions needed

- ✅ **Advanced Features**: `MANAGE_EXTERNAL_STORAGE` (optional)
  - Only for root/Shizuku-based operations
  - Not required for normal users
  - Properly scoped to API 30+

## New Components

### 1. FileSystemManager (`storage/FileSystemManager.kt`)

Central manager for all file system operations.

**Key Features:**
- Unified interface for all storage locations
- App-private storage management
- MediaStore integration (Android 10+)
- SAF directory operations
- Storage statistics and space checking
- Automatic directory creation and cleanup

**Usage Example:**
```kotlin
@Inject lateinit var fileSystemManager: FileSystemManager

// Get backup directory (no permissions needed)
val backupDir = fileSystemManager.getBackupDirectory()

// Create snapshot
val snapshotDir = fileSystemManager.createSnapshotDirectory(snapshotId)

// Check storage space
if (fileSystemManager.hasEnoughSpace(requiredBytes)) {
    // Proceed with backup
}

// Get storage stats
val stats = fileSystemManager.getStorageStats()
println("Used: ${stats.percentUsed}%")
```

### 2. MediaStoreHelper (`storage/MediaStoreHelper.kt`)

Handles backup exports to shared storage using MediaStore API.

**Key Features:**
- Export backups to Documents folder
- Import from MediaStore URIs
- Query existing backups
- Delete backups from shared storage
- Android 10+ (API 29+) support

**Usage Example:**
```kotlin
@Inject lateinit var mediaStoreHelper: MediaStoreHelper

// Export backup to shared storage (Android 10+)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    val result = mediaStoreHelper.exportBackup(
        sourceFile = backupFile,
        displayName = "backup_${timestamp}.tar.zst"
    )
    result.onSuccess { uri ->
        println("Exported to: $uri")
    }
}

// Query backups
val backups = mediaStoreHelper.queryBackups()
backups.onSuccess { list ->
    list.forEach { backup ->
        println("${backup.displayName}: ${backup.size} bytes")
    }
}
```

### 3. SafHelper (`storage/SafHelper.kt`)

Storage Access Framework helper for user-selected directories.

**Key Features:**
- Directory picker intent creation
- Persistent URI permissions
- Export to user-chosen locations
- Import from SAF URIs
- Subdirectory creation

**Usage Example:**
```kotlin
@Inject lateinit var safHelper: SafHelper

// Let user pick a directory
val intent = safHelper.createDirectoryPickerIntent()
startActivityForResult(intent, REQUEST_CODE_SAF)

// In onActivityResult
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == REQUEST_CODE_SAF && resultCode == RESULT_OK) {
        val uri = data?.data ?: return
        safHelper.persistDirectoryPermissions(uri)
        
        // Export backup
        lifecycleScope.launch {
            val result = safHelper.exportFileToSafDirectory(
                sourceFile = backupFile,
                safDirectoryUri = uri,
                fileName = "backup.tar.zst"
            )
        }
    }
}
```

### 4. ScopedStorageMigration (`storage/ScopedStorageMigration.kt`)

Handles one-time migration from legacy storage to scoped storage.

**Key Features:**
- Automatic migration on app startup
- Finds legacy backup locations
- Copies data to new locations
- Migration status tracking
- Safe and idempotent

**Migration Process:**
1. App starts → checks if migration completed
2. If not, scans for legacy locations:
   - `/sdcard/ObsidianBackup`
   - `/storage/emulated/0/ObsidianBackup`
   - Other common legacy paths
3. Copies all backups to app-private storage
4. Marks migration as completed
5. Legacy data preserved (not deleted) for safety

## Permission Changes

### AndroidManifest.xml Updates

```xml
<!-- Before: Required for all Android versions -->
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />

<!-- After: Properly scoped -->
<!-- Legacy permissions (Android 9 and below) -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="29" />

<!-- Media permissions (Android 13+) - Optional for exports -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"
    android:minSdkVersion="33" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO"
    android:minSdkVersion="33" />

<!-- MANAGE_EXTERNAL_STORAGE - Only for advanced root/Shizuku features -->
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"
    android:minSdkVersion="30"
    tools:ignore="ScopedStorage" />
```

### Application Tag Updates

```xml
<application
    android:requestLegacyExternalStorage="true"
    tools:targetApi="q">
```

This allows smooth migration for users upgrading from older versions.

## Migration for Developers

### Updating Existing Code

#### Before:
```kotlin
// ❌ Direct file access to external storage
val backupDir = File(Environment.getExternalStorageDirectory(), "ObsidianBackup")
backupDir.mkdirs()
```

#### After:
```kotlin
// ✅ Use FileSystemManager
val backupDir = fileSystemManager.getBackupDirectory()
// Directory automatically created, no permissions needed
```

#### Before:
```kotlin
// ❌ Manual file copying
FileInputStream(source).use { input ->
    FileOutputStream(dest).use { output ->
        input.copyTo(output)
    }
}
```

#### After:
```kotlin
// ✅ Use FileSystemManager
fileSystemManager.copyFile(source, dest)
```

#### Before:
```kotlin
// ❌ Exporting to arbitrary external location
val exportFile = File("/sdcard/backup.tar")
source.copyTo(exportFile)
```

#### After (Option 1 - MediaStore):
```kotlin
// ✅ Export using MediaStore (Android 10+)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    mediaStoreHelper.exportBackup(
        sourceFile = source,
        displayName = "backup.tar"
    )
}
```

#### After (Option 2 - SAF):
```kotlin
// ✅ Let user choose location via SAF
val intent = safHelper.createDirectoryPickerIntent()
// Handle result and export
```

## Testing Guidelines

### Test Scenarios

1. **Fresh Install (No Migration)**
   - Install app on clean device
   - Create backup
   - Verify backup in app-private storage
   - Check no permissions requested (except for root/Shizuku)

2. **Upgrade from Old Version**
   - Install old version with legacy backups
   - Upgrade to new version
   - Verify migration completes
   - Check all backups accessible
   - Verify legacy data preserved

3. **Export to Shared Storage**
   - Create backup
   - Export via MediaStore (Android 10+)
   - Verify file in Documents/ObsidianBackup
   - Open with file manager
   - Verify accessible

4. **SAF Export**
   - Create backup
   - Choose export directory via SAF
   - Export backup
   - Verify file in chosen location
   - Verify permissions persisted

5. **Storage Space**
   - Check storage statistics
   - Verify space checking before backup
   - Test low storage scenario

6. **Multi-Android Version**
   - Test on Android 9 (legacy storage)
   - Test on Android 10 (scoped storage introduction)
   - Test on Android 11+ (scoped storage mandatory)
   - Test on Android 13+ (media permissions)
   - Test on Android 14/15 (latest requirements)

## API Level Compatibility

| Android Version | API Level | Storage Model | Status |
|----------------|-----------|---------------|--------|
| Android 9 and below | ≤28 | Legacy external storage | ✅ Supported (legacy permissions) |
| Android 10 | 29 | Scoped storage (optional) | ✅ Full support |
| Android 11 | 30 | Scoped storage (mandatory) | ✅ Full support |
| Android 12 | 31-32 | Scoped storage | ✅ Full support |
| Android 13 | 33 | Media permissions | ✅ Full support |
| Android 14 | 34 | Enhanced scoped storage | ✅ Full support |
| Android 15 | 35 | Latest requirements | ✅ Ready |

## User-Facing Changes

### Backup Location
- **Old**: `/sdcard/ObsidianBackup/` (required special permission)
- **New**: `/Android/data/com.obsidianbackup/files/backups/` (no permission needed)

### Export Options
Users now have three ways to export backups:

1. **App-Private Storage** (Default)
   - No permissions needed
   - Accessible via Android settings
   - Automatically cleaned on uninstall

2. **Documents Folder** (via MediaStore)
   - Available on Android 10+
   - Exports to `/Documents/ObsidianBackup/`
   - Accessible via file managers
   - No special permissions

3. **Custom Location** (via SAF)
   - User chooses any accessible location
   - SD card, USB drive, cloud folders
   - One-time permission grant

### Migration Notice
On first launch after upgrade:
- Automatic background migration
- No user action required
- Old backups remain untouched
- Notification on completion

## Performance Considerations

### App-Private Storage Benefits
- ✅ **Faster**: Direct file system access
- ✅ **No permissions**: Zero permission dialogs
- ✅ **Reliable**: No scoped storage restrictions
- ✅ **Automatic cleanup**: Uninstall removes data

### Potential Issues
- ⚠️ **Space Limited**: Same partition as app
- ⚠️ **Uninstall Loss**: Data deleted on uninstall (by design)
- ℹ️ **Solution**: Offer export to permanent locations

## Security Improvements

### Before Migration
- ❌ Full filesystem access via `MANAGE_EXTERNAL_STORAGE`
- ❌ Read/write to any external storage location
- ❌ Potential security audit issues

### After Migration
- ✅ Minimal permissions requested
- ✅ App-private storage sandboxing
- ✅ User-controlled exports only
- ✅ Play Store policy compliant
- ✅ No security audit flags

## Troubleshooting

### Issue: Migration Failed
**Symptoms**: Old backups not accessible after upgrade

**Solutions**:
1. Check app logs for migration errors
2. Verify old backup location exists
3. Check storage permissions on old version
4. Manually trigger migration: `scopedStorageMigration.performMigrationIfNeeded()`
5. Reset migration status for retry: `scopedStorageMigration.resetMigrationStatus()`

### Issue: Cannot Export Backup
**Symptoms**: Export fails or shows permission error

**Solutions**:
1. Android 10+: Use MediaStore export
2. Any Android: Use SAF and let user pick location
3. Check storage space available
4. Verify file not already open/locked

### Issue: Backups Disappear After Uninstall
**Symptoms**: Backups lost when app reinstalled

**Expected Behavior**: App-private storage is deleted on uninstall by design

**Solutions**:
1. Export backups before uninstalling
2. Use cloud sync feature
3. Use SAF to save to permanent location
4. Document this behavior for users

## Future Enhancements

1. **Automatic Cloud Backup**
   - Sync to cloud provider
   - Persists across uninstalls

2. **SD Card Support**
   - Direct SD card exports via SAF
   - Automatic SD card detection

3. **Smart Storage Management**
   - Automatic cleanup of old backups
   - Compression improvements
   - Deduplication

4. **Migration Notifications**
   - User-visible migration progress
   - Post-migration summary
   - Export recommendations

## References

- [Android Scoped Storage Guide](https://developer.android.com/training/data-storage)
- [MediaStore API](https://developer.android.com/reference/android/provider/MediaStore)
- [Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider)
- [App-specific storage](https://developer.android.com/training/data-storage/app-specific)
- [Android 14 changes](https://developer.android.com/about/versions/14/changes/storage)

## Summary

This migration makes ObsidianBackup:
- ✅ **Compliant** with Android 10-15 requirements
- ✅ **Secure** with minimal permission requests
- ✅ **User-friendly** with zero-permission default operation
- ✅ **Flexible** with multiple export options
- ✅ **Future-proof** for upcoming Android versions
- ✅ **Play Store ready** for publication

The app now uses best practices for Android storage management while maintaining all backup/restore functionality.
