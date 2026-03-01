# Split APK Implementation Testing Guide

## Overview
This document describes how to test the split APK backup and restore functionality in ObsidianBackup.

## Prerequisites
- Android device or emulator with Android 5.0+ (API 21+)
- Root access or ADB access
- Apps installed via Play Store (which use split APKs)

## Testing Steps

### 1. Check Split APK Detection

Use `pm path` command to check if an app uses split APKs:

```bash
# Replace com.example.app with actual package name
adb shell pm path com.example.app

# Expected output for split APK:
# package:/data/app/~~<hash>/<package>/base.apk
# package:/data/app/~~<hash>/<package>/split_config.arm64_v8a.apk
# package:/data/app/~~<hash>/<package>/split_config.xxhdpi.apk
# package:/data/app/~~<hash>/<package>/split_config.en.apk

# Expected output for single APK:
# package:/data/app/com.example.app/base.apk
```

### 2. Identify Split Types

Common split APK patterns:
- **Base APK**: `base.apk` - Required, contains core app code
- **ABI splits**: `split_config.arm64_v8a.apk`, `split_config.x86.apk` - Architecture-specific native libraries
- **Density splits**: `split_config.xxhdpi.apk`, `split_config.xxxhdpi.apk` - Screen density resources
- **Language splits**: `split_config.en.apk`, `split_config.es.apk` - Language-specific resources

### 3. Test Device Architecture Detection

```bash
# Check device architecture
adb shell getprop ro.product.cpu.abilist

# Common outputs:
# arm64-v8a,armeabi-v7a,armeabi (64-bit ARM)
# x86_64,x86 (64-bit Intel)
# x86 (32-bit Intel)
```

### 4. Test Backup with Split APK

Within the ObsidianBackup app:
1. Select an app that uses split APKs (e.g., most Google apps, large games)
2. Start backup with APK component enabled
3. Check backup directory for:
   - `apk/base.apk`
   - `apk/split_config.*.apk` files
   - `apk/apk_metadata.json` (contains split information)

### 5. Manual Verification of Backup

```bash
# List backed up APK files
adb shell ls -lh /path/to/backup/<package>/apk/

# Check metadata
adb shell cat /path/to/backup/<package>/apk/apk_metadata.json
```

Expected metadata format:
```json
{
  "packageName": "com.example.app",
  "isSplit": true,
  "baseApkPath": "/data/app/~~hash/com.example.app/base.apk",
  "splitNames": [
    "config.arm64_v8a",
    "config.xxhdpi",
    "config.en"
  ],
  "splitPaths": [
    "/data/app/~~hash/com.example.app/split_config.arm64_v8a.apk",
    "/data/app/~~hash/com.example.app/split_config.xxhdpi.apk",
    "/data/app/~~hash/com.example.app/split_config.en.apk"
  ]
}
```

### 6. Test Restore with Split APK

#### Restore to Same Device:
1. Uninstall the app
2. Use ObsidianBackup to restore from backup
3. Verify app launches correctly
4. Check all features work (especially those using native libraries)

#### Restore to Different Device (Architecture):
1. Create backup on ARM device
2. Try restore on x86 device (or vice versa)
3. Verify:
   - Only compatible ABI splits are installed
   - App launches without library errors
   - Incompatible ABI splits are filtered out

### 7. Test Edge Cases

#### Mixed Backup (Split + Non-Split Apps):
1. Create backup including:
   - Apps with split APKs (e.g., Chrome, YouTube)
   - Apps without split APKs (e.g., older apps, sideloaded APKs)
2. Restore all apps
3. Verify both types restore correctly

#### Partial Split Restoration:
1. Backup app with multiple language splits
2. Manually remove some language split APKs from backup
3. Restore app
4. Verify app still works (missing language splits are optional)

#### Architecture Mismatch:
1. Backup app with arm64-v8a split on ARM device
2. Attempt restore on x86 device
3. Expected: Restore should fail gracefully with clear error message

### 8. Manual Install Session Test

Test the pm install session mechanism directly:

```bash
# Create install session
adb shell pm install-create -S <total_size_bytes>
# Output: Success: created install session [123456]

# Write each APK
adb shell pm install-write -S <apk_size> 123456 0 /path/to/base.apk
adb shell pm install-write -S <apk_size> 123456 1 /path/to/split1.apk
adb shell pm install-write -S <apk_size> 123456 2 /path/to/split2.apk

# Commit session
adb shell pm install-commit 123456

# Or abandon if needed
adb shell pm install-abandon 123456
```

### 9. Logging and Debugging

Check ObsidianBackup logs for:
- Split APK detection: "Backed up split APK: <package> with N splits"
- Install session creation: "Created install session: <id>"
- Split filtering: "Filtered splits for device compatibility"
- Errors: "Failed to copy split APK", "Failed to create install session"

### 10. Performance Testing

Compare backup/restore times:
- Single APK app: ~1-5 seconds
- Split APK app (3-5 splits): ~5-15 seconds
- Large split APK app (10+ splits): ~15-30 seconds

## Known Apps with Split APKs

Good test candidates (most Play Store apps now use splits):
- **Google Chrome** (com.android.chrome)
- **YouTube** (com.google.android.youtube)
- **Google Maps** (com.google.android.apps.maps)
- **WhatsApp** (com.whatsapp)
- **Facebook** (com.facebook.katana)
- **Instagram** (com.instagram.android)
- Most games from Play Store

## Common Issues and Solutions

### Issue: "Failed to create install session"
- **Cause**: Insufficient storage space
- **Solution**: Free up space or reduce backup size

### Issue: "App not installed" error during restore
- **Cause**: Missing base APK or critical splits
- **Solution**: Verify backup contains base.apk

### Issue: App crashes after restore with "UnsatisfiedLinkError"
- **Cause**: Wrong ABI split installed
- **Solution**: Verify ABI filtering is working correctly

### Issue: Restore slow with many splits
- **Cause**: Each split requires separate write operation
- **Solution**: This is expected, pm session install is sequential

## Success Criteria

✅ Split APK detection works for installed apps
✅ All splits (base + config) are backed up
✅ Metadata file is created with correct information
✅ Single APKs continue to work (backward compatibility)
✅ Split APKs restore successfully using pm session
✅ ABI filtering prevents architecture mismatches
✅ Mixed backups (split + non-split) work correctly
✅ Partial splits (missing optional splits) restore without errors
✅ Error messages are clear and actionable

## Regression Testing

Ensure existing functionality still works:
- Single APK backup/restore
- Data backup/restore
- OBB backup/restore
- External data backup/restore
- Incremental backups
- Compressed backups
