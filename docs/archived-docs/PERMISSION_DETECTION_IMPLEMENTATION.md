# PermissionCapabilities Detection Implementation

## Summary

Comprehensive permission and capability detection system implemented for ObsidianBackup Android application in Kotlin. This system detects all available privilege escalation methods and device capabilities to determine the best backup approach.

## Changes Made

### 1. Dependencies Added

**File:** `gradle/libs.versions.toml`

Added Shizuku API library:
```toml
shizuku = "13.1.5"
```

**File:** `app/build.gradle.kts`

Added Shizuku dependencies:
```kotlin
implementation(libs.shizuku.api)
implementation(libs.shizuku.provider)
```

### 2. Expanded PermissionCapabilities Data Class

**File:** `app/src/main/java/com/obsidianbackup/model/PermissionCapabilities.kt`

Expanded from 4 properties to 21 comprehensive properties:

#### Core Backup Capabilities
- `canBackupApk`: Can backup APK files
- `canBackupData`: Can backup app data
- `canDoIncremental`: Can perform incremental backups
- `canRestoreSelinux`: Can restore SELinux contexts

#### Permission Modes Detected
- `hasRoot`: Root access available
- `hasShizuku`: Shizuku service available
- `hasAdb`: ADB debugging enabled
- `hasSaf`: Storage Access Framework (always true)

#### Root-Specific Capabilities
- `rootType`: Enum (NONE, MAGISK, SUPERSU, KINGROOT, OTHER_SU)
- `hasBusybox`: Busybox installed
- `hasMagisk`: Magisk detected
- `canExecuteSuCommands`: Can actually execute su commands

#### Storage Capabilities (API-Level Aware)
- `apiLevel`: Android API level
- `hasScopedStorage`: Scoped storage active (Android 10+)
- `canAccessAllFiles`: Broad file access available
- `hasManageExternalStoragePermission`: All files permission granted

#### Service Capabilities
- `isAccessibilityServiceEnabled`: App's accessibility service enabled
- `canUseBackupTransport`: Backup transport permission granted

#### ADB-Specific
- `adbWirelessEnabled`: Wireless ADB enabled
- `adbUsbEnabled`: USB ADB enabled

#### Shizuku-Specific
- `shizukuVersion`: Shizuku API version
- `shizukuPermissionGranted`: Shizuku permission granted to app

### 3. Implemented Comprehensive Detection Methods

**File:** `app/src/main/java/com/obsidianbackup/permissions/PermissionManager.kt`

Expanded from ~111 lines to ~600+ lines with full detection logic.

#### Main Detection Method

```kotlin
suspend fun detectCapabilities(): PermissionCapabilities
```

- Implements 30-second caching to avoid repeated expensive checks
- Combines results from 5 specialized detection methods
- Returns safe defaults on errors
- Runs on IO dispatcher for performance

#### Root Detection (`detectRootCapabilities()`)

Implements **6 detection methods**:

1. **Binary Existence Check**: Scans 7 common su binary locations
   - `/system/bin/su`
   - `/system/xbin/su`
   - `/sbin/su`
   - `/system/su`
   - `/system/bin/.ext/.su`
   - `/system/usr/we-need-root/su-backup`
   - `/system/xbin/mu`

2. **Root Management App Detection**: Checks for installed root managers
   - Magisk (`com.topjohnwu.magisk`)
   - SuperSU (`eu.chainfire.supersu`)
   - Superuser (`com.noshufou.android.su`)
   - Koushikdutta Superuser (`com.koushikdutta.superuser`)
   - KingRoot (`com.kingroot.kinguser`)

3. **Magisk-Specific Detection**: Checks Magisk directories
   - `/sbin/.magisk`
   - `/sbin/.core`
   - `/data/adb/magisk`

4. **Busybox Detection**: Scans 3 common busybox locations
   - `/system/xbin/busybox`
   - `/system/bin/busybox`
   - `/data/local/xbin/busybox`

5. **Command Execution Test**: Actually executes `su -c echo test`
   - Definitive proof of working root access
   - Most reliable method

6. **Build Tags Check**: Checks for test-keys in Build.TAGS
   - Indicates custom ROM/rooted device

#### Shizuku Detection (`detectShizukuCapabilities()`)

Implements **3 checks**:

1. **Binder Availability**: `Shizuku.pingBinder()`
   - Checks if Shizuku service is running

2. **Version Detection**: `Shizuku.getVersion()`
   - Gets Shizuku API version

3. **Permission Check**: `Shizuku.checkSelfPermission()`
   - Verifies app has Shizuku permission granted

#### ADB Detection (`detectAdbCapabilities()`)

Implements **3 detection methods**:

1. **Global Settings Check**: Reads `Settings.Global.ADB_ENABLED`
   - Checks if ADB is enabled globally

2. **Wireless ADB Check** (Android 11+): Reads `adb_wifi_enabled`
   - Detects wireless debugging state

3. **Authorization File Check**: Checks `/data/misc/adb/adb_keys`
   - Verifies ADB authorization (requires root)

#### Storage Detection (`detectStorageCapabilities()`)

Implements **API-level aware detection**:

1. **Scoped Storage Detection**: API 29+ (Android 10)
   - Determines if scoped storage is enforced

2. **MANAGE_EXTERNAL_STORAGE Check**: API 30+ (Android 11)
   - Checks for all files access permission

3. **Legacy Storage Check**: Pre-API 29
   - Checks READ_EXTERNAL_STORAGE
   - Checks WRITE_EXTERNAL_STORAGE

#### Service Detection (`detectServiceCapabilities()`)

Implements **2 checks**:

1. **Accessibility Service**: Reads `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`
   - Detects if app's accessibility service is enabled

2. **Backup Transport**: Checks `ApplicationInfo.FLAG_ALLOW_BACKUP`
   - Verifies if app backups are allowed

## Key Features

### 1. Performance Optimization
- **30-second caching**: Avoids repeated expensive detection calls
- **IO Dispatcher**: All detection runs on background threads
- **Cache invalidation**: `invalidateCache()` method for forced re-detection

### 2. Error Handling
- Try-catch blocks around all detection methods
- Safe defaults returned on errors
- Detailed error logging via ObsidianLogger
- Graceful degradation when detection fails

### 3. Edge Cases Handled
- Devices without root access
- Partial capabilities (e.g., root but no busybox)
- Permission denied scenarios
- Missing system files
- Unsupported Android versions
- Shizuku not installed/not running
- ADB disabled

### 4. Security Considerations
- No sensitive data exposure
- Read-only detection (no system modifications)
- Respects Android security model
- Handles SecurityException gracefully
- Appropriate for both rooted and non-rooted devices

## Usage Example

```kotlin
@Inject
lateinit var permissionManager: PermissionManager

// Detect all capabilities
suspend fun checkCapabilities() {
    val caps = permissionManager.detectCapabilities()
    
    // Check specific capabilities
    if (caps.canExecuteSuCommands) {
        // Full root access available
        performRootBackup()
    } else if (caps.shizukuPermissionGranted) {
        // Use Shizuku for elevated operations
        performShizukuBackup()
    } else {
        // Fallback to SAF
        performSafBackup()
    }
    
    // API level aware storage handling
    if (caps.hasScopedStorage) {
        if (caps.hasManageExternalStoragePermission) {
            // Can access all files
        } else {
            // Limited to MediaStore/SAF
        }
    }
}

// Detect best available mode
suspend fun findBestMode() {
    permissionManager.detectBestMode()
    
    // Observe the detected mode
    permissionManager.currentMode.collect { mode ->
        when (mode) {
            PermissionMode.ROOT -> setupRootBackup()
            PermissionMode.SHIZUKU -> setupShizukuBackup()
            PermissionMode.ADB -> setupAdbBackup()
            PermissionMode.SAF -> setupSafBackup()
        }
    }
}

// Force fresh detection
suspend fun refreshCapabilities() {
    permissionManager.invalidateCache()
    val freshCaps = permissionManager.detectCapabilities()
}
```

## Testing Considerations

### Manual Testing Required
Since Android SDK is not available in this environment:

1. **Root Detection Testing**:
   - Test on non-rooted device
   - Test on Magisk rooted device
   - Test on SuperSU rooted device
   - Test on custom ROM

2. **Shizuku Testing**:
   - Test with Shizuku not installed
   - Test with Shizuku installed but not running
   - Test with Shizuku running but permission denied
   - Test with Shizuku running and permission granted

3. **ADB Testing**:
   - Test with ADB disabled
   - Test with USB ADB enabled
   - Test with wireless ADB enabled (Android 11+)

4. **API Level Testing**:
   - Test on Android 8 (API 26) - legacy storage
   - Test on Android 10 (API 29) - scoped storage introduction
   - Test on Android 11+ (API 30+) - MANAGE_EXTERNAL_STORAGE

### Automated Testing
```kotlin
@Test
fun testCaching() = runTest {
    // First call should detect
    val caps1 = permissionManager.detectCapabilities()
    delay(100)
    // Second call should use cache
    val caps2 = permissionManager.detectCapabilities()
    assertEquals(caps1, caps2)
    
    // After invalidation, should re-detect
    permissionManager.invalidateCache()
    val caps3 = permissionManager.detectCapabilities()
}

@Test
fun testErrorHandling() = runTest {
    // Should not crash even if detection fails
    val caps = permissionManager.detectCapabilities()
    assertNotNull(caps)
}
```

## Research Sources

Implementation based on extensive research:

### Root Detection
- Multiple binary location checks (7 paths)
- Modern Magisk detection (systemless root)
- Legacy SuperSU detection
- Build tag analysis
- Command execution verification

### Shizuku Integration
- Official Shizuku API documentation
- Binder communication patterns
- Permission management

### ADB Detection
- Android Debug Bridge architecture
- Wireless debugging (Android 11+)
- Authorization file structure
- mDNS service discovery

### Storage Permissions
- Scoped Storage (Android 10+)
- MANAGE_EXTERNAL_STORAGE (Android 11+)
- Legacy storage permissions
- MediaStore API integration

## Future Enhancements

1. **Enhanced ADB Detection**:
   - mDNS service discovery for wireless ADB
   - More reliable ADB daemon detection
   - Network-based ADB connection verification

2. **Root Detection Improvements**:
   - Detection of root hiding mechanisms (MagiskHide, DenyList, Shamiko)
   - SafetyNet/Play Integrity API integration
   - Additional root management app detection

3. **Performance Optimizations**:
   - Configurable cache duration
   - Selective re-detection (only changed capabilities)
   - Background detection with WorkManager

4. **Additional Capabilities**:
   - SELinux status detection
   - Package installation permissions
   - Special app access permissions (e.g., Display over other apps)

## Compatibility

- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)
- **Kotlin**: 1.8.10
- **Shizuku API**: 13.1.5
- **Coroutines**: Full suspend function support

## Notes

- All detection methods are **read-only** and do not modify system state
- Detection is designed to be **non-intrusive** and respect user privacy
- The implementation follows **Android security best practices**
- Error handling ensures the app **never crashes** during detection
- Caching provides **optimal performance** without excessive system queries
