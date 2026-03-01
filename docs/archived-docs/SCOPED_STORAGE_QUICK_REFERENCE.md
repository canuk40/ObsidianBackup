# Scoped Storage - Quick Reference Card

## 🚀 TL;DR

ObsidianBackup now uses **scoped storage** - backup operations require **ZERO permissions** for normal users!

## 📍 Storage Locations

| Location | Path | Permissions | Use Case |
|----------|------|-------------|----------|
| **App-Private** | `/Android/data/com.obsidianbackup/files/backups/` | None | Default (all backups) |
| **Documents** | `/Documents/ObsidianBackup/` | None | User exports via MediaStore |
| **Custom** | User-selected | One-time grant | SAF exports |

## 💡 Quick Examples

### Create Backup (No Permissions!)
```kotlin
val backupDir = fileSystemManager.getBackupDirectory()
val snapshot = fileSystemManager.createSnapshotDirectory(snapshotId)
```

### Export to Documents (Android 10+)
```kotlin
mediaStoreHelper.exportBackup(file, "backup.tar.zst")
```

### Let User Choose Location
```kotlin
val intent = safHelper.createDirectoryPickerIntent()
startActivityForResult(intent, REQUEST_CODE)
```

## 🔑 Key Components

| Class | Purpose | One-Liner |
|-------|---------|-----------|
| **FileSystemManager** | Main storage API | `fileSystemManager.getBackupDirectory()` |
| **MediaStoreHelper** | Shared storage | `mediaStoreHelper.exportBackup(file, name)` |
| **SafHelper** | User directories | `safHelper.createDirectoryPickerIntent()` |
| **ScopedStorageMigration** | Auto migration | Runs on app startup automatically |
| **StoragePermissionHelper** | Permission checks | `storagePermissionHelper.hasAppPrivateStorageAccess()` |

## 📱 Android Version Support

| Android | API | Storage Model | Permissions Needed |
|---------|-----|---------------|-------------------|
| 9 and below | ≤28 | Legacy | READ/WRITE_EXTERNAL_STORAGE |
| 10 | 29 | Scoped (optional) | None for app-private |
| 11+ | 30+ | Scoped (mandatory) | None for app-private |
| 13+ | 33+ | Scoped + Media | None for app-private |

## ✅ What Changed

| Before | After |
|--------|-------|
| ❌ Requires `MANAGE_EXTERNAL_STORAGE` | ✅ Zero permissions for standard features |
| ❌ `/sdcard/ObsidianBackup/` | ✅ `/Android/data/.../files/backups/` |
| ❌ Manual permission requests | ✅ Automatic, transparent operation |
| ❌ Security audit flags | ✅ Play Store compliant |

## 🎯 Common Tasks

### Check Storage Space
```kotlin
if (!fileSystemManager.hasEnoughSpace(requiredBytes)) {
    showError("Not enough space")
}
```

### Get Storage Statistics
```kotlin
val stats = fileSystemManager.getStorageStats()
println("Used: ${stats.percentUsed}%")
```

### List All Backups
```kotlin
val backups = fileSystemManager.listBackups()
```

### Delete Old Backup
```kotlin
fileSystemManager.deleteSnapshotDirectory(snapshotId)
```

### Import Backup
```kotlin
mediaStoreHelper.importBackup(sourceUri, destFile)
```

## 🔧 Dependency Injection

```kotlin
@Inject lateinit var fileSystemManager: FileSystemManager
@Inject lateinit var mediaStoreHelper: MediaStoreHelper
@Inject lateinit var safHelper: SafHelper
@Inject lateinit var storagePermissionHelper: StoragePermissionHelper
```

## 📋 Permissions in Manifest

```xml
<!-- Legacy (Android 9 and below) -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="29" />

<!-- Media (Android 13+, optional) -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"
    android:minSdkVersion="33" />

<!-- Advanced features only (Android 11+) -->
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"
    android:minSdkVersion="30" />
```

## 🧪 Testing One-Liners

```kotlin
// Test app-private storage (always works)
assert(storagePermissionHelper.hasAppPrivateStorageAccess())

// Check migration status
val status = scopedStorageMigration.getMigrationStatus()

// Test backup creation
val dir = fileSystemManager.createSnapshotDirectory(UUID.randomUUID().toString())
assert(dir.exists())

// Test export (Android 10+)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    val result = mediaStoreHelper.exportBackup(testFile, "test.tar")
    assert(result.isSuccess)
}
```

## 🆘 Troubleshooting

| Problem | Solution |
|---------|----------|
| "Permission denied" | Use `fileSystemManager` instead of direct `File` API |
| "Migration failed" | Check logs, retry with `scopedStorageMigration.performMigrationIfNeeded()` |
| "Cannot export" | Use MediaStore (Android 10+) or SAF (all versions) |
| "No space" | Check with `fileSystemManager.hasEnoughSpace()` before operations |

## 📚 Documentation

- **Full Guide**: `SCOPED_STORAGE_MIGRATION.md`
- **Integration Examples**: `SCOPED_STORAGE_INTEGRATION.md`
- **Implementation Summary**: `SCOPED_STORAGE_IMPLEMENTATION_SUMMARY.md`
- **Verification Checklist**: `SCOPED_STORAGE_CHECKLIST.md`

## 🎉 Benefits

- ✅ **Zero Permissions** - No dialogs for standard users
- ✅ **Privacy First** - Sandboxed app data by default
- ✅ **Play Store Ready** - Fully compliant
- ✅ **Future Proof** - Android 10-15+ support
- ✅ **User Friendly** - Automatic migration

## 📞 Need Help?

1. Check inline KDoc comments in code
2. Review `SCOPED_STORAGE_INTEGRATION.md` for examples
3. See `SCOPED_STORAGE_MIGRATION.md` for architecture
4. Test with `SCOPED_STORAGE_CHECKLIST.md`

---

**Remember**: App-private storage needs **ZERO permissions**! 🎯
