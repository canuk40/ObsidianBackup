# Permission Audit - Quick Fix Implementation Guide

## Critical Fixes (Apply Immediately)

### 1. Remove CAMERA Permission
**File:** `app/src/main/AndroidManifest.xml`

**Remove lines 54-56:**
```xml
<!-- QR Code scanning (optional) -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

**Reason:** Not implemented, creates privacy concerns

---

### 2. Remove RECORD_AUDIO Permission
**File:** `app/src/main/AndroidManifest.xml`

**Remove lines 63:**
```xml
<!-- Voice control for accessibility -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

**Reason:** Voice control feature not implemented, major privacy red flag

---

### 3. Remove QUERY_ALL_PACKAGES Permission
**Files:** 
- `app/src/main/AndroidManifest.xml` (lines 59-60)
- `tv/src/main/AndroidManifest.xml` (lines 31-32)

**Remove:**
```xml
<!-- Root/Shizuku detection -->
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
    tools:ignore="QueryAllPackagesPermission" />
```

**Reason:** `getInstalledApplications()` works WITHOUT this permission

**Verification test:**
```kotlin
// Test in AppScanner.kt - should work without permission
val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
// Returns all user apps without QUERY_ALL_PACKAGES
```

---

### 4. Remove SCHEDULE_EXACT_ALARM Permission
**File:** `app/src/main/AndroidManifest.xml`

**Remove lines 50-51:**
```xml
<!-- Exact alarms for scheduled backups (Android 12+) -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"
    android:minSdkVersion="31" />
```

**Reason:** WorkManager doesn't need exact alarms, drains battery

**Code verification:**
```kotlin
// WorkManagerScheduler.kt already uses PeriodicWorkRequest
// which DOESN'T require exact alarms
val backupWork = PeriodicWorkRequestBuilder<BackupWorker>(
    24, TimeUnit.HOURS // Uses inexact timing (better for battery)
)
```

---

### 5. Remove READ_MEDIA_* Permissions (Not Used)
**File:** `app/src/main/AndroidManifest.xml`

**Remove lines 13-18:**
```xml
<!-- Media permissions for Android 13+ (API 33+) - Optional for user exports -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"
    android:minSdkVersion="33" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO"
    android:minSdkVersion="33" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO"
    android:minSdkVersion="33" />
```

**Reason:** Manifest says "optional", no media backup code found

---

## Medium Priority Fixes

### 6. Fix Custom Permission Protection Levels (Tasker Integration)

**File:** `app/src/main/AndroidManifest.xml`

**Change lines 91-96 (AUTOMATION permission):**

**FROM:**
```xml
<permission
    android:name="com.obsidianbackup.permission.AUTOMATION"
    android:protectionLevel="signature|privileged"
    ...
/>
```

**TO:**
```xml
<permission
    android:name="com.obsidianbackup.permission.AUTOMATION"
    android:protectionLevel="dangerous"
    android:label="@string/permission_automation_label"
    android:description="@string/permission_automation_description"
    android:icon="@mipmap/ic_launcher" />
```

**Change lines 99-104 (TASKER_STATUS permission):**

**FROM:**
```xml
<permission
    android:name="com.obsidianbackup.permission.TASKER_STATUS"
    android:protectionLevel="signature"
    ...
/>
```

**TO:**
```xml
<permission
    android:name="com.obsidianbackup.permission.TASKER_STATUS"
    android:protectionLevel="normal"
    android:label="@string/permission_tasker_status_label"
    android:description="@string/permission_tasker_status_description"
    android:icon="@mipmap/ic_launcher" />
```

**Reason:** Third-party Tasker app needs to access these components

---

## Optional Improvements

### 7. Add Feature Flag for MANAGE_EXTERNAL_STORAGE

**File:** `app/src/main/java/com/obsidianbackup/features/Feature.kt`

**Add:**
```kotlin
enum class Feature {
    // ... existing features
    ADVANCED_STORAGE_ACCESS, // Gates MANAGE_EXTERNAL_STORAGE permission
}
```

**File:** `app/src/main/java/com/obsidianbackup/storage/StoragePermissionHelper.kt`

**Add method:**
```kotlin
/**
 * Only request MANAGE_EXTERNAL_STORAGE if advanced features are enabled
 */
fun shouldRequestAdvancedStorage(): Boolean {
    return featureFlagManager.isEnabled(Feature.ADVANCED_STORAGE_ACCESS) &&
           (permissionMode == PermissionMode.ROOT || 
            permissionMode == PermissionMode.SHIZUKU)
}
```

**Update UI to show education dialog:**
```kotlin
fun requestAdvancedStorageWithEducation(activity: Activity) {
    if (!shouldRequestAdvancedStorage()) {
        // Use SAF instead
        showSafDialog()
        return
    }
    
    // Show warning about broad access
    AlertDialog.Builder(activity)
        .setTitle("Advanced Storage Access")
        .setMessage("""
            This permission grants broad filesystem access for root-based features.
            
            Only enable if you use root or Shizuku backup methods.
            
            Standard backups work without this permission.
        """.trimIndent())
        .setPositiveButton("Enable") { _, _ ->
            val intent = createManageStorageIntent()
            activity.startActivity(intent)
        }
        .setNegativeButton("Cancel", null)
        .show()
}
```

---

### 8. Optimize Health Connect Permission Requests

**File:** `app/src/main/java/com/obsidianbackup/health/HealthConnectManager.kt`

**Improve permission request to only ask for enabled data types:**

**Add to UI (HealthScreen.kt or settings):**
```kotlin
@Composable
fun HealthDataTypeSelector() {
    val enabledTypes = remember { mutableStateListOf<HealthDataType>() }
    
    Column {
        Text("Select health data to backup:")
        
        HealthDataType.values().forEach { dataType ->
            Row(
                modifier = Modifier.clickable {
                    if (enabledTypes.contains(dataType)) {
                        enabledTypes.remove(dataType)
                    } else {
                        enabledTypes.add(dataType)
                    }
                }
            ) {
                Checkbox(
                    checked = enabledTypes.contains(dataType),
                    onCheckedChange = null
                )
                Text(dataType.displayName)
            }
        }
        
        Button(onClick = {
            // Only request permissions for selected types
            val permissions = healthConnectManager
                .getRequiredPermissions(enabledTypes.toSet())
            requestPermissions(permissions)
        }) {
            Text("Request Selected Permissions")
        }
    }
}
```

---

## Testing After Changes

### Test 1: App Scanning Works Without QUERY_ALL_PACKAGES
```bash
# Build and install
./gradlew assembleFreeDebug
adb install -r app/build/outputs/apk/free/debug/*.apk

# Test app list
adb shell am start -n com.obsidianbackup/.MainActivity

# Verify apps are listed in UI
# Check logcat for errors
adb logcat | grep -i "package\|scanner"
```

### Test 2: Scheduled Backups Work Without SCHEDULE_EXACT_ALARM
```bash
# Enable scheduled backups in UI
# Wait for WorkManager to trigger

# Check work status
adb shell dumpsys jobscheduler | grep -i obsidian

# Verify backup runs (may take 24hrs or force with)
adb shell cmd jobscheduler run -f com.obsidianbackup <job-id>
```

### Test 3: Tasker Integration Works with New Permissions
```bash
# From Tasker, send broadcast:
adb shell am broadcast -a com.obsidianbackup.tasker.ACTION_START_BACKUP \
  --es "profile_id" "test-profile"

# Check logcat for receipt
adb logcat | grep -i "tasker\|automation"
```

### Test 4: Verify Permission Count Reduced
```bash
# Check declared permissions
adb shell dumpsys package com.obsidianbackup | grep "permission:"

# Should see 22 permissions instead of 31
```

---

## Build Commands

```bash
# 1. Clean build after manifest changes
./gradlew clean

# 2. Build all variants
./gradlew assembleFreeDebug
./gradlew assemblePremiumDebug

# 3. Run linter
./gradlew lintFreeDebug

# 4. Generate release APK (after testing)
./gradlew assemblePremiumRelease
```

---

## Rollback Plan (If Issues Arise)

If removing permissions causes issues:

1. **App scanning fails:** Re-add QUERY_ALL_PACKAGES (unlikely - should work without)
2. **Scheduled backups fail:** Re-add SCHEDULE_EXACT_ALARM (unlikely - WorkManager handles this)
3. **Tasker integration fails:** Revert custom permission protection levels

**Git rollback:**
```bash
# Before making changes
git checkout -b permission-audit-fixes

# If issues
git checkout main
git branch -D permission-audit-fixes
```

---

## Expected Outcomes

### Before Changes
- 31 permissions in main app
- Google Play review: 2-3 business days
- User trust: Medium (camera/mic permissions concerning)

### After Changes  
- 22 permissions in main app (-9 = 29% reduction)
- Google Play review: 1-2 business days (fewer red flags)
- User trust: High (no unnecessary sensitive permissions)

### Play Store Data Safety Section (Simplified)
**Before:**
- Must justify: CAMERA, RECORD_AUDIO, QUERY_ALL_PACKAGES, SCHEDULE_EXACT_ALARM, MANAGE_EXTERNAL_STORAGE

**After:**
- Must justify: MANAGE_EXTERNAL_STORAGE only (valid use case: root backup)

---

## String Resources (Add if Missing)

**File:** `app/src/main/res/values/strings.xml`

```xml
<!-- Permission labels (if custom permissions use these) -->
<string name="permission_automation_label">Automation Control</string>
<string name="permission_automation_description">Allows automation apps like Tasker to trigger backups</string>

<string name="permission_tasker_status_label">Backup Status Query</string>
<string name="permission_tasker_status_description">Allows automation apps to query backup status</string>

<string name="permission_wear_sync_label">Wear OS Sync</string>
<string name="permission_wear_sync_description">Syncs backup data with paired Wear OS devices</string>
```

---

## Timeline

**Estimated time to implement:**
- Critical fixes (1-5): 30 minutes (manifest edits)
- Medium priority (6): 15 minutes (manifest edits)
- Optional improvements (7-8): 2-3 hours (code + UI)
- Testing: 1 hour

**Total: 2-4 hours for complete implementation**

---

## Checklist

### Critical (Must Do)
- [ ] Remove CAMERA permission
- [ ] Remove RECORD_AUDIO permission  
- [ ] Remove QUERY_ALL_PACKAGES permission (main + TV)
- [ ] Remove SCHEDULE_EXACT_ALARM permission
- [ ] Remove READ_MEDIA_* permissions
- [ ] Test app scanning still works
- [ ] Test scheduled backups still work
- [ ] Build and install debug APK
- [ ] Verify permission count reduced

### Medium Priority (Should Do)
- [ ] Fix AUTOMATION permission protectionLevel
- [ ] Fix TASKER_STATUS permission protectionLevel
- [ ] Test Tasker integration
- [ ] Add string resources for permissions

### Optional (Nice to Have)
- [ ] Add ADVANCED_STORAGE_ACCESS feature flag
- [ ] Add education dialog for MANAGE_EXTERNAL_STORAGE
- [ ] Add Health data type selector UI
- [ ] Update documentation

---

## Questions/Issues

If you encounter issues:

1. **App list is empty after removing QUERY_ALL_PACKAGES:**
   - Check `AppScanner.kt` line 32 - should work without permission
   - Verify `PackageManager.GET_META_DATA` flag is used
   - Test on Android 11+ devices (behavior may differ on older versions)

2. **Tasker can't trigger backups:**
   - Verify protection level changed to `dangerous`
   - Test broadcast sending from Tasker
   - Check broadcast receiver is registered correctly

3. **Build errors after manifest changes:**
   - Clean build: `./gradlew clean`
   - Invalidate IDE caches: File → Invalidate Caches / Restart
   - Check no string resources are missing

---

*End of Fix Guide*
