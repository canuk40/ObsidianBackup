# Split APK Quick Reference Guide

## For Developers

### Quick Start

```kotlin
// Check if an app uses split APKs
val appScanner = AppScanner(context)
if (appScanner.hasSplitApks("com.example.app")) {
    val count = appScanner.getSplitCount("com.example.app")
    val names = appScanner.getSplitNames("com.example.app")
    println("App has $count splits: $names")
}

// Backup automatically handles split APKs
val engine = ObsidianBoxEngine(...)
val result = engine.backupApps(BackupRequest(
    appIds = listOf(AppId("com.example.app")),
    components = setOf(BackupComponent.APK)
))

// Restore automatically handles split APKs
val restoreResult = engine.restoreApps(RestoreRequest(
    snapshotId = snapshotId,
    appIds = listOf(AppId("com.example.app"))
))
```

### Shell Commands

```bash
# List all APK paths for a package (shows splits)
pm path com.example.app

# Manual session install
SESSION=$(pm install-create -S <total_bytes> | grep -oP '\[\K\d+')
pm install-write -S <bytes> $SESSION 0 /path/to/base.apk
pm install-write -S <bytes> $SESSION 1 /path/to/split1.apk
pm install-commit $SESSION

# Check device architecture
getprop ro.product.cpu.abilist
# Output: arm64-v8a,armeabi-v7a,armeabi
```

### Common Split Types

| Split Name | Type | Description |
|------------|------|-------------|
| `base.apk` | Base | Core app (always required) |
| `config.arm64_v8a` | ABI | 64-bit ARM native libraries |
| `config.armeabi_v7a` | ABI | 32-bit ARM native libraries |
| `config.x86` | ABI | 32-bit Intel native libraries |
| `config.x86_64` | ABI | 64-bit Intel native libraries |
| `config.xxhdpi` | Density | High-res graphics resources |
| `config.xxxhdpi` | Density | Extra high-res graphics |
| `config.en` | Language | English localization |
| `config.es` | Language | Spanish localization |

### Architecture Matrix

| Device ABI | Compatible Splits |
|-----------|------------------|
| arm64-v8a | arm64-v8a, armeabi-v7a, armeabi |
| armeabi-v7a | armeabi-v7a, armeabi |
| x86_64 | x86_64, x86 |
| x86 | x86 |

### API Methods

#### SplitApkHelper

```kotlin
// Detection
fun isSplitApk(packageName: String): Boolean
fun getSplitApkInfo(packageName: String): SplitApkMetadata?

// Device info
fun getDeviceArchitecture(): String  // Returns: "arm64-v8a"
fun getSupportedAbis(): List<String>  // Returns: ["arm64-v8a", "armeabi-v7a"]

// Split analysis
fun isAbiSplit(splitName: String): Boolean
fun isDensitySplit(splitName: String): Boolean
fun isSplitCompatible(splitName: String): Boolean

// Filtering
fun filterSplitsForRestore(
    splits: List<SplitApkInfo>,
    filterAbi: Boolean = true
): List<SplitApkInfo>

// Operations
suspend fun backupApks(metadata: SplitApkMetadata, targetDir: File): Long
suspend fun restoreApks(apkDir: File, packageName: String): Boolean
```

#### AppScanner

```kotlin
fun hasSplitApks(packageName: String): Boolean
fun getSplitCount(packageName: String): Int
fun getSplitNames(packageName: String): List<String>
fun getPackageManager(): PackageManager
```

### Backup Directory Structure

```
<backup_root>/
└── <snapshot_id>/
    └── <package_name>/
        ├── apk/
        │   ├── base.apk                    # Base APK (required)
        │   ├── config.arm64_v8a.apk       # ABI split
        │   ├── config.xxhdpi.apk          # Density split
        │   ├── config.en.apk              # Language split
        │   └── apk_metadata.json          # Split metadata
        ├── data.tar.zst                   # App data
        ├── obb.tar.zst                    # OBB files
        └── external.tar.zst               # External data
```

### Metadata Format

```json
{
  "packageName": "com.example.app",
  "isSplit": true,
  "baseApkPath": "/data/app/~~abc123==/com.example.app/base.apk",
  "splitNames": [
    "config.arm64_v8a",
    "config.xxhdpi",
    "config.en"
  ],
  "splitPaths": [
    "/data/app/~~abc123==/com.example.app/split_config.arm64_v8a.apk",
    "/data/app/~~abc123==/com.example.app/split_config.xxhdpi.apk",
    "/data/app/~~abc123==/com.example.app/split_config.en.apk"
  ]
}
```

### Error Handling

```kotlin
try {
    val metadata = splitApkHelper.getSplitApkInfo(packageName)
    if (metadata?.isSplit == true) {
        // Handle split APK
        val size = splitApkHelper.backupApks(metadata, targetDir)
    } else {
        // Handle single APK (legacy)
        backupSingleApk(packageName, targetDir)
    }
} catch (e: Exception) {
    logger.e("Error", "Failed to backup APK", e)
    // Fallback to legacy method
}
```

### Logging Examples

```
D/SplitApkHelper: Backed up split APK: com.chrome.beta with 4 splits, total size: 52428800 bytes
D/SplitApkHelper: Created install session: 123456789
D/SplitApkHelper: Wrote APK 'base.apk' to session (index 0)
D/SplitApkHelper: Successfully installed split APKs

E/SplitApkHelper: Failed to create install session: Insufficient storage
E/SplitApkHelper: Failed to copy split APK 'config.es'
```

### Testing Checklist

- [ ] Test with split APK app (Chrome, YouTube)
- [ ] Test with non-split APK app (sideloaded APK)
- [ ] Verify all splits are backed up
- [ ] Verify metadata.json is created
- [ ] Test restore on same device
- [ ] Test restore on different architecture (if possible)
- [ ] Check logs for errors
- [ ] Verify app launches after restore
- [ ] Test mixed backup (split + non-split apps)
- [ ] Test legacy backup compatibility

### Known Issues

None currently identified. Report issues with:
- Package name
- Device architecture
- Android version
- Split count and names
- Error logs

### Performance Tips

1. **Batch Operations**: Backup multiple apps in one session
2. **Compression**: Enable compression for data/obb, not for APKs (already compressed)
3. **Filtering**: Filter incompatible splits early to save time
4. **Parallel Backup**: Different apps can be backed up in parallel (not implemented yet)

### Best Practices

1. **Always check isSplit**: Don't assume all apps are split APKs
2. **Handle both cases**: Support single and split APKs
3. **Log extensively**: Log split operations for debugging
4. **Validate metadata**: Check metadata.json exists before restore
5. **Graceful degradation**: Fall back to legacy methods on error
6. **Version checks**: Check API level before accessing split fields

### Troubleshooting

| Problem | Cause | Solution |
|---------|-------|----------|
| "App not installed" | Missing base APK | Verify base.apk exists |
| UnsatisfiedLinkError | Wrong ABI split | Check device architecture |
| "Insufficient storage" | Low disk space | Free up space |
| Session creation fails | pm service error | Check permissions |
| Restore slow | Many splits | Expected, pm writes are sequential |

### References

- [Android Developer Docs: Split APKs](https://developer.android.com/guide/app-bundle)
- [ApplicationInfo.splitSourceDirs](https://developer.android.com/reference/android/content/pm/ApplicationInfo#splitSourceDirs)
- [PackageInstaller.Session](https://developer.android.com/reference/android/content/pm/PackageInstaller.Session)
- [SAI GitHub](https://github.com/Aefyr/SAI)

### Version Compatibility

| Feature | Min API | Notes |
|---------|---------|-------|
| Split APKs | 21 (Lollipop) | splitSourceDirs introduced |
| splitNames | 21 (Lollipop) | Same as splitSourceDirs |
| SUPPORTED_ABIS | 21 (Lollipop) | Replaces CPU_ABI |
| Session Install | 21 (Lollipop) | PackageInstaller API |

### Code Examples

#### Manual Split Detection

```kotlin
val pm = context.packageManager
val appInfo = pm.getApplicationInfo("com.example.app", 0)

if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
    val hasSplits = appInfo.splitSourceDirs != null && 
                    appInfo.splitSourceDirs.isNotEmpty()
    
    if (hasSplits) {
        println("Base: ${appInfo.sourceDir}")
        appInfo.splitSourceDirs.forEachIndexed { i, path ->
            val name = appInfo.splitNames[i]
            println("Split [$name]: $path")
        }
    }
}
```

#### Custom Split Filtering

```kotlin
fun filterByLanguage(splits: List<SplitApkInfo>, lang: String): List<SplitApkInfo> {
    return splits.filter { split ->
        !split.name.startsWith("config.") || 
        split.name == "config.$lang" ||
        !split.name.contains(Regex("config\\.[a-z]{2}"))
    }
}

// Usage: Keep only English splits
val englishSplits = filterByLanguage(allSplits, "en")
```

---

**Last Updated**: Current implementation
**Status**: Production Ready
**License**: As per ObsidianBackup project
