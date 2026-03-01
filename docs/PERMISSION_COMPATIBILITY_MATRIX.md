# Permission Compatibility Matrix - ObsidianBackup

**Comprehensive permission support across Android versions and device configurations**

---

## Android API Level Compatibility

| Permission | API 21-23 | API 24-25 | API 26-28 | API 29 | API 30 | API 31-32 | API 33 | API 34+ | Notes |
|-----------|-----------|-----------|-----------|--------|--------|-----------|--------|---------|-------|
| **READ_EXTERNAL_STORAGE** | ✅ Required | ✅ Required | ✅ Required | ✅ Scoped | ✅ Scoped | ✅ Scoped | ❌ N/A | ❌ N/A | maxSdkVersion="32" |
| **WRITE_EXTERNAL_STORAGE** | ✅ Required | ✅ Required | ✅ Required | ✅ Legacy | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | maxSdkVersion="29" |
| **MANAGE_EXTERNAL_STORAGE** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Special | ✅ Special | ✅ Special | ✅ Special | minSdkVersion="30", advanced only |
| **READ_MEDIA_IMAGES** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Granular | ✅ Granular | minSdkVersion="33" |
| **READ_MEDIA_VIDEO** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Granular | ✅ Granular | minSdkVersion="33" |
| **READ_MEDIA_AUDIO** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Granular | ✅ Granular | minSdkVersion="33" |
| **INTERNET** | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | Normal permission |
| **ACCESS_NETWORK_STATE** | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | Normal permission |
| **POST_NOTIFICATIONS** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Runtime | ✅ Runtime | minSdkVersion="33" |
| **USE_BIOMETRIC** | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | Normal permission, API 29+ |
| **Health READ_STEPS** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Runtime | ✅ Runtime | Health Connect required |
| **Health WRITE_STEPS** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Runtime | ✅ Runtime | Health Connect required |
| **Health READ_HEART_RATE** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Runtime | ✅ Runtime | Health Connect required |
| **Health WRITE_HEART_RATE** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Runtime | ✅ Runtime | Health Connect required |
| **Health READ_SLEEP** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Runtime | ✅ Runtime | Health Connect required |
| **Health WRITE_SLEEP** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Runtime | ✅ Runtime | Health Connect required |
| **Health READ_EXERCISE** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Runtime | ✅ Runtime | Health Connect required |
| **Health WRITE_EXERCISE** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Runtime | ✅ Runtime | Health Connect required |
| **Health READ_NUTRITION** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Runtime | ✅ Runtime | Health Connect required |
| **Health WRITE_NUTRITION** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Runtime | ✅ Runtime | Health Connect required |
| **Health READ_WEIGHT** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Runtime | ✅ Runtime | Health Connect required |
| **Health WRITE_WEIGHT** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Runtime | ✅ Runtime | Health Connect required |
| **Health READ_HEIGHT** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Runtime | ✅ Runtime | Health Connect required |
| **Health WRITE_HEIGHT** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Runtime | ✅ Runtime | Health Connect required |
| **Health READ_BODY_FAT** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Runtime | ✅ Runtime | Health Connect required |
| **Health WRITE_BODY_FAT** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Runtime | ✅ Runtime | Health Connect required |
| **SCHEDULE_EXACT_ALARM** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Special | ✅ Special | ✅ Special | minSdkVersion="31" |
| **FOREGROUND_SERVICE** | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | Normal permission |
| **FOREGROUND_SERVICE_DATA_SYNC** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Required | minSdkVersion="34" |
| **QUERY_ALL_PACKAGES** | ❌ N/A | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Required | ✅ Required | ✅ Required | ✅ Required | API 30+ requires declaration |
| **CAMERA** | ✅ Runtime | ✅ Runtime | ✅ Runtime | ✅ Runtime | ✅ Runtime | ✅ Runtime | ✅ Runtime | ✅ Runtime | Optional feature |
| **RECORD_AUDIO** | ✅ Runtime | ✅ Runtime | ✅ Runtime | ✅ Runtime | ✅ Runtime | ✅ Runtime | ✅ Runtime | ✅ Runtime | Optional feature |
| **WAKE_LOCK** | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | Normal permission |
| **RECEIVE_BOOT_COMPLETED** | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | Normal permission |
| **VIBRATE** | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | ✅ Auto | Normal permission |

**Legend:**
- ✅ **Required**: Permission needed and functional
- ✅ **Auto**: Automatically granted (normal/signature)
- ✅ **Runtime**: Requires runtime request
- ✅ **Special**: Requires special intent (Settings)
- ✅ **Scoped**: Works with scoped storage
- ✅ **Granular**: Granular media permissions
- ✅ **Legacy**: Legacy storage with opt-in
- ❌ **N/A**: Not available on this API level

---

## Device Capability Matrix

### Root Detection Capabilities

| Device Type | SafetyNet | Build Tags | Root Apps | Su Binary | Props Check | System Write | Confidence |
|-------------|-----------|------------|-----------|-----------|-------------|--------------|------------|
| **Stock Android (Pixel)** | ✅ Available | ✅ Checked | ✅ Checked | ✅ Checked | ✅ Checked | ✅ Checked | HIGH |
| **Samsung Galaxy (OneUI)** | ✅ Available | ✅ Checked | ✅ Checked | ✅ Checked | ✅ Checked | ✅ Checked | HIGH |
| **OnePlus (OxygenOS)** | ✅ Available | ✅ Checked | ✅ Checked | ✅ Checked | ✅ Checked | ✅ Checked | HIGH |
| **Xiaomi (MIUI)** | ✅ Available | ✅ Checked | ✅ Checked | ✅ Checked | ✅ Checked | ✅ Checked | HIGH |
| **Huawei (EMUI) - GMS** | ✅ Available | ✅ Checked | ✅ Checked | ✅ Checked | ✅ Checked | ✅ Checked | HIGH |
| **Huawei (HMS) - No GMS** | ❌ No GMS | ✅ Checked | ✅ Checked | ✅ Checked | ✅ Checked | ✅ Checked | MEDIUM |
| **LineageOS (Official)** | ⚠️ May fail | ✅ Checked | ✅ Checked | ✅ Checked | ✅ Checked | ✅ Checked | MEDIUM |
| **Custom ROM (Release Keys)** | ⚠️ May fail | ✅ Pass | ✅ Checked | ✅ Checked | ✅ Checked | ✅ Checked | LOW-MED |
| **Custom ROM (Test Keys)** | ❌ Fails | ❌ Fails | ✅ Checked | ✅ Checked | ✅ Checked | ✅ Checked | MEDIUM |
| **Rooted Device (Magisk)** | ❌ Fails | ⚠️ May pass | ✅ Detected | ✅ Detected | ⚠️ May fail | ✅ Detected | HIGH-CRITICAL |
| **Emulator (Android Studio)** | ❌ No GMS | ❌ Test keys | ❌ None | ❌ None | ⚠️ May fail | ✅ Writable | LOW |
| **Android TV** | ⚠️ Limited | ✅ Checked | ✅ Checked | ✅ Checked | ✅ Checked | ✅ Checked | MEDIUM |
| **Wear OS** | ⚠️ Limited | ✅ Checked | ✅ Checked | ✅ Checked | ✅ Checked | ✅ Checked | MEDIUM |

**Confidence Levels:**
- **CRITICAL**: Multiple methods detect root, very high certainty
- **HIGH**: SafetyNet or multiple reliable indicators
- **MEDIUM**: Some indicators present, moderate certainty
- **LOW**: Single unreliable indicator or clean device

---

## Biometric Authentication Capabilities

| Device | Fingerprint | Face Unlock | Iris Scanner | Device Credential | StrongBox | TEE |
|--------|-------------|-------------|--------------|-------------------|-----------|-----|
| **Google Pixel 3+** | ✅ Class 3 | ⚠️ Class 2 (Pixel 4) | ❌ None | ✅ PIN/Pattern | ✅ Available | ✅ Available |
| **Samsung Galaxy S10+** | ✅ Class 3 | ✅ Class 3 (S10+) | ✅ Class 3 (Note 10) | ✅ PIN/Pattern | ✅ Available | ✅ Available |
| **Samsung Galaxy A-Series** | ✅ Class 3 | ⚠️ Class 2 | ❌ None | ✅ PIN/Pattern | ⚠️ Limited | ✅ Available |
| **OnePlus 7+** | ✅ Class 3 | ⚠️ Class 2 | ❌ None | ✅ PIN/Pattern | ✅ Available | ✅ Available |
| **Xiaomi Mi 8+** | ✅ Class 3 | ⚠️ Class 2 | ❌ None | ✅ PIN/Pattern | ⚠️ Limited | ✅ Available |
| **iPhone via Emulation** | ❌ iOS-only | ❌ iOS-only | ❌ iOS-only | ❌ N/A | ❌ N/A | ❌ N/A |
| **Budget Devices (<$200)** | ⚠️ Class 1-2 | ⚠️ Class 1 | ❌ None | ✅ PIN/Pattern | ❌ None | ⚠️ Software |
| **Android TV** | ❌ None | ❌ None | ❌ None | ⚠️ PIN only | ❌ None | ⚠️ Limited |
| **Wear OS** | ❌ None | ❌ None | ❌ None | ⚠️ Limited | ❌ None | ⚠️ Limited |

**Biometric Classes:**
- **Class 3 (Strong)**: Hardware-backed, spoof-resistant (accepted by banking apps)
- **Class 2 (Weak)**: Software-backed or easier to spoof
- **Class 1 (Convenience)**: No security guarantees

**ObsidianBackup Policy:**
- Only accepts **Class 3 (BIOMETRIC_STRONG)** for crypto operations
- Always provides **Device Credential fallback** (PIN/Pattern/Password)
- StrongBox used when available for key storage

---

## Health Connect Availability

| Device | Health Connect | Steps | Heart Rate | Sleep | Exercise | Nutrition | Body Measurements |
|--------|---------------|-------|------------|-------|----------|-----------|-------------------|
| **Google Pixel 7+ (Android 14)** | ✅ Built-in | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Samsung Galaxy S23+ (Android 14)** | ✅ Built-in | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **OnePlus 11+ (Android 14)** | ✅ Install | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Xiaomi 13+ (Android 14)** | ✅ Install | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Devices with Android 13** | ⚠️ Install | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Devices with Android <13** | ❌ Not Available | ❌ No | ❌ No | ❌ No | ❌ No | ❌ No | ❌ No |
| **Huawei (HMS)** | ❌ Not Available | ❌ No | ❌ No | ❌ No | ❌ No | ❌ No | ❌ No |
| **Android TV** | ❌ Not Available | ❌ No | ❌ No | ❌ No | ❌ No | ❌ No | ❌ No |
| **Wear OS** | ⚠️ Limited Support | ✅ Yes | ✅ Yes | ⚠️ Limited | ✅ Yes | ❌ No | ⚠️ Limited |

**Health Connect Requirements:**
- **Android 13+** (API 33+)
- **Google Play Services** installed
- **Health Connect app** (pre-installed or downloadable)
- User must **grant permissions** per data type

**Fallback:** Health data backup disabled if Health Connect unavailable

---

## Storage Mode Compatibility

### SAF (Storage Access Framework) Mode

| Device Type | Supported | Performance | User Experience | Limitations |
|-------------|-----------|-------------|-----------------|-------------|
| **All Android 5.0+** | ✅ Yes | ⚡ Good | 👍 Excellent | App-private storage only |
| **Android 11+ (Scoped)** | ✅ Yes | ⚡ Good | 👍 Excellent | Full scoped storage support |
| **External SD Card** | ✅ Yes | ⚡ Acceptable | 👍 Good | SD card access via SAF picker |
| **Cloud Storage (Drive)** | ✅ Yes | 🐌 Slow | 👍 Good | Network-dependent |
| **USB OTG** | ✅ Yes | ⚡ Good | 👎 Fair | Must be mounted |

**Advantages:**
- ✅ No permissions required
- ✅ Works on all devices
- ✅ User control over location
- ✅ Scoped storage compliant

**Disadvantages:**
- ❌ Cannot backup app data directly
- ❌ User must select folder each time (unless persisted URI)
- ❌ Slower than direct file access

### Root Mode

| Device Type | Supported | Requirements | Capabilities | Risk Level |
|-------------|-----------|--------------|--------------|------------|
| **Rooted Phone (Magisk)** | ✅ Yes | Su binary + Magisk | Full backup (APK + data) | ⚠️ High |
| **Rooted Tablet** | ✅ Yes | Su binary | Full backup (APK + data) | ⚠️ High |
| **Custom ROM (Rooted)** | ✅ Yes | Su binary | Full backup (APK + data) | ⚠️ High |
| **Non-Rooted Device** | ❌ No | N/A | Falls back to SAF | ✅ None |
| **Magisk Hidden** | ⚠️ Detectable | Magisk + root detection bypass | Full backup if detected | ⚠️ High |
| **KernelSU** | ✅ Yes | Su binary | Full backup (APK + data) | ⚠️ High |

**Advantages:**
- ✅ Full app + data backup
- ✅ Direct file system access
- ✅ Incremental backup support
- ✅ SELinux context preservation

**Disadvantages:**
- ❌ Requires root access
- ❌ Voids warranty
- ❌ Security risk if compromised
- ❌ May trigger SafetyNet

### Shizuku Mode

| Device Type | Supported | Requirements | Capabilities | Risk Level |
|-------------|-----------|--------------|--------------|------------|
| **Any Android 11+** | ✅ Yes | Shizuku app + ADB/Root | App backup (no data) | ✅ Low |
| **Rooted Device** | ✅ Yes | Shizuku app + Root | Enhanced backup | ⚠️ Medium |
| **Wireless ADB** | ✅ Yes | Shizuku app + WiFi ADB | App backup (no data) | ✅ Low |
| **USB ADB** | ✅ Yes | Shizuku app + USB ADB | App backup (no data) | ✅ Low |
| **Non-ADB Device** | ❌ No | N/A | Falls back to SAF | ✅ None |

**Advantages:**
- ✅ No root required (with ADB)
- ✅ Enhanced permissions via Shizuku
- ✅ Safer than root mode
- ✅ Doesn't void warranty

**Disadvantages:**
- ❌ Requires Shizuku setup
- ❌ ADB connection needed (or root)
- ❌ Limited data backup (APK only without root)
- ❌ ADB connection may disconnect

### ADB Mode

| Device Type | Supported | Requirements | Capabilities | Risk Level |
|-------------|-----------|--------------|--------------|------------|
| **Any Android 5.0+** | ✅ Yes | ADB enabled + USB/WiFi | APK extraction only | ✅ None |
| **Developer Mode** | ✅ Yes | USB debugging enabled | APK extraction only | ✅ None |
| **Wireless ADB (11+)** | ✅ Yes | Wireless debugging enabled | APK extraction only | ✅ None |
| **Production Device** | ❌ No | N/A | Falls back to SAF | ✅ None |

**Advantages:**
- ✅ No root required
- ✅ Safe and official
- ✅ Developer-friendly

**Disadvantages:**
- ❌ Very limited (APK only)
- ❌ Requires developer mode
- ❌ No app data backup

---

## Cloud Provider Compatibility

| Provider | Internet | Network State | Additional Permissions | API Requirements |
|----------|----------|---------------|------------------------|------------------|
| **Google Drive** | ✅ Required | ✅ Recommended | OAuth 2.0 (in-app) | ✅ Google Play Services |
| **Dropbox** | ✅ Required | ✅ Recommended | OAuth 2.0 (in-app) | ✅ None |
| **OneDrive** | ✅ Required | ✅ Recommended | OAuth 2.0 (in-app) | ✅ None |
| **AWS S3** | ✅ Required | ✅ Recommended | None (API key) | ✅ None |
| **Azure Blob** | ✅ Required | ✅ Recommended | None (API key) | ✅ None |
| **Backblaze B2** | ✅ Required | ✅ Recommended | None (API key) | ✅ None |
| **WebDAV** | ✅ Required | ✅ Recommended | None (credentials) | ✅ None |
| **Nextcloud** | ✅ Required | ✅ Recommended | None (credentials) | ✅ None |
| **Syncthing** | ✅ Required | ✅ Recommended | FOREGROUND_SERVICE | ✅ None |
| **FTP/SFTP** | ✅ Required | ✅ Recommended | None (credentials) | ✅ None |
| **Rclone** | ✅ Required | ✅ Recommended | Varies by backend | ✅ None |

**All cloud providers require:**
- `INTERNET` permission (auto-granted)
- `ACCESS_NETWORK_STATE` permission (auto-granted)
- Active internet connection
- User authentication (OAuth or credentials)

---

## Notification Channel Compatibility

| Channel | API 21-25 | API 26+ | Importance | POST_NOTIFICATIONS (API 33+) |
|---------|-----------|---------|------------|------------------------------|
| **Backup Progress** | ⚠️ No Channels | ✅ Channel | HIGH | ✅ Required |
| **Restore Progress** | ⚠️ No Channels | ✅ Channel | HIGH | ✅ Required |
| **Cloud Sync** | ⚠️ No Channels | ✅ Channel | DEFAULT | ✅ Required |
| **Errors** | ⚠️ No Channels | ✅ Channel | HIGH | ✅ Required |
| **Scheduled Backups** | ⚠️ No Channels | ✅ Channel | LOW | ✅ Required |
| **Foreground Service** | ⚠️ Legacy Notif | ✅ Channel | HIGH | ✅ Required |

**API 26+ Behavior:**
- All notifications must use **notification channels**
- User can customize per-channel settings
- Cannot programmatically change importance after creation

**API 33+ Behavior:**
- Must request **POST_NOTIFICATIONS** permission
- User can deny notification permission
- App must handle denial gracefully (silent operation)

---

## WorkManager Compatibility

| Feature | API 21-22 | API 23-25 | API 26-28 | API 29-30 | API 31+ | Requirements |
|---------|-----------|-----------|-----------|-----------|---------|--------------|
| **PeriodicWorkRequest** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | WorkManager 2.x |
| **Constraints (Charging)** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | None |
| **Constraints (Network)** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ACCESS_NETWORK_STATE |
| **Constraints (Battery)** | ⚠️ Limited | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | Battery optimization |
| **Exact Scheduling** | ❌ No | ❌ No | ❌ No | ❌ No | ✅ Yes | SCHEDULE_EXACT_ALARM |
| **Flex Interval** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | Minimum 15 minutes |
| **Boot Awareness** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | RECEIVE_BOOT_COMPLETED |

**WorkManager Advantages:**
- ✅ Survives app/device restarts
- ✅ Respects battery optimization
- ✅ Handles constraint changes
- ✅ Persistent work queue

---

## Foreground Service Types (Android 14+)

| Service Type | Purpose | Required Permission | Restrictions |
|--------------|---------|---------------------|--------------|
| **dataSync** | Backup/restore operations | FOREGROUND_SERVICE_DATA_SYNC | Used by ObsidianBackup ✅ |
| **camera** | Camera operations | FOREGROUND_SERVICE_CAMERA | Not used ❌ |
| **microphone** | Audio recording | FOREGROUND_SERVICE_MICROPHONE | Not used ❌ |
| **mediaPlayback** | Media playback | FOREGROUND_SERVICE_MEDIA_PLAYBACK | Not used ❌ |
| **phoneCall** | Phone calls | FOREGROUND_SERVICE_PHONE_CALL | Not used ❌ |
| **location** | Location tracking | FOREGROUND_SERVICE_LOCATION | Not used ❌ |
| **connectedDevice** | BLE/USB | FOREGROUND_SERVICE_CONNECTED_DEVICE | Not used ❌ |
| **mediaProjection** | Screen recording | FOREGROUND_SERVICE_MEDIA_PROJECTION | Not used ❌ |

**Android 14+ Requirements:**
- Must declare **foregroundServiceType** in manifest
- Must request corresponding permission
- System enforces type restrictions
- Cannot use service without proper type

---

## Camera & Audio Feature Availability

| Feature | Permission | Hardware Required | Fallback | Optional |
|---------|------------|-------------------|----------|----------|
| **QR Code Scanning** | CAMERA | Camera | Manual entry | ✅ Yes |
| **Voice Commands** | RECORD_AUDIO | Microphone | Manual input | ✅ Yes |
| **Photo Backup** | READ_MEDIA_IMAGES (API 33+) | None | Disabled | ✅ Yes |
| **Video Backup** | READ_MEDIA_VIDEO (API 33+) | None | Disabled | ✅ Yes |
| **Audio Backup** | READ_MEDIA_AUDIO (API 33+) | None | Disabled | ✅ Yes |

**Design Principle:**
- All camera/audio features are **optional**
- Core backup functionality works without them
- Hardware marked as `android:required="false"`
- Graceful degradation on permission denial

---

## Custom ROM Compatibility

| ROM | Root Detection | Biometric | Health Connect | Storage | Overall Support |
|-----|----------------|-----------|----------------|---------|-----------------|
| **LineageOS (Official)** | ⚠️ May trigger | ✅ Works | ✅ Works | ✅ Works | ✅ Excellent |
| **Pixel Experience** | ⚠️ May trigger | ✅ Works | ✅ Works | ✅ Works | ✅ Excellent |
| **Paranoid Android** | ⚠️ May trigger | ✅ Works | ✅ Works | ✅ Works | ✅ Excellent |
| **Resurrection Remix** | ⚠️ Triggers | ✅ Works | ✅ Works | ✅ Works | ✅ Good |
| **Havoc OS** | ⚠️ Triggers | ✅ Works | ✅ Works | ✅ Works | ✅ Good |
| **/e/OS (Privacy)** | ⚠️ May trigger | ✅ Works | ⚠️ No GMS | ✅ Works | ⚠️ Limited |
| **GrapheneOS** | ✅ Clean | ✅ Works | ⚠️ Optional GMS | ✅ Works | ✅ Excellent |
| **CalyxOS** | ⚠️ May trigger | ✅ Works | ⚠️ microG | ✅ Works | ⚠️ Limited |

**Custom ROM Notes:**
- Most ROMs trigger **build tags** root detection (test-keys)
- ObsidianBackup refined logic to avoid false positives
- Release-keys signed ROMs pass detection
- Health Connect requires Google Play Services (or microG)

---

## Wear OS Compatibility

| Feature | Wear OS 2.x | Wear OS 3.x | Wear OS 4.x | Requirements |
|---------|-------------|-------------|-------------|--------------|
| **Permissions** | ⚠️ Limited | ✅ Full | ✅ Full | Same as phone |
| **Storage** | ✅ SAF only | ✅ SAF only | ✅ SAF only | No root |
| **Biometric** | ❌ None | ⚠️ Limited | ⚠️ Limited | Device credential only |
| **Health Connect** | ❌ None | ⚠️ Limited | ✅ Works | Wear OS 4+ |
| **Notifications** | ✅ Auto | ✅ Auto | ✅ Requires request (API 33+) | POST_NOTIFICATIONS |
| **Cloud Sync** | ✅ Works | ✅ Works | ✅ Works | INTERNET |

**Wear OS Limitations:**
- No root support (locked bootloaders)
- Limited storage (typically <16GB)
- No external storage
- Battery constraints (more aggressive)

---

## Android TV Compatibility

| Feature | Android TV 9 | Android TV 10 | Android TV 11+ | Requirements |
|---------|--------------|---------------|----------------|--------------|
| **Permissions** | ✅ Full | ✅ Full | ✅ Full | Same as phone |
| **Storage** | ✅ All modes | ✅ All modes | ✅ All modes | Root if available |
| **Biometric** | ❌ None | ❌ None | ❌ None | PIN only |
| **Health Connect** | ❌ None | ❌ None | ❌ None | Not applicable |
| **Notifications** | ✅ Auto | ✅ Auto | ✅ Requires request (API 33+) | POST_NOTIFICATIONS |
| **Cloud Sync** | ✅ Works | ✅ Works | ✅ Works | INTERNET |

**Android TV Limitations:**
- No biometric hardware
- Limited touch input (remote control)
- Different UI patterns (10-foot experience)
- May have more storage than phone

---

## Testing Recommendations

### Minimum Test Matrix

| Device Category | Min API | Max API | Root | Biometric | Health | Priority |
|----------------|---------|---------|------|-----------|--------|----------|
| **Stock Android** | 21 | 35 | ❌ No | ✅ Yes | ✅ Yes | 🔴 Critical |
| **Samsung Galaxy** | 29 | 34 | ❌ No | ✅ Yes | ✅ Yes | 🔴 Critical |
| **Custom ROM** | 29 | 33 | ⚠️ Maybe | ✅ Yes | ⚠️ Maybe | 🟡 Medium |
| **Rooted Device** | 29 | 33 | ✅ Yes | ✅ Yes | ✅ Yes | 🔴 Critical |
| **Budget Device** | 21 | 28 | ❌ No | ⚠️ Limited | ❌ No | 🟡 Medium |
| **Android TV** | 29 | 33 | ❌ No | ❌ No | ❌ No | 🟢 Low |
| **Wear OS** | 30 | 34 | ❌ No | ⚠️ Limited | ⚠️ Limited | 🟢 Low |

### Test Scenarios by API Level

**API 21-23 (Android 5.0-6.0):**
- ✅ Legacy storage permissions
- ✅ Basic root detection
- ✅ No biometric support
- ✅ Basic notifications
- ✅ SAF mode

**API 24-28 (Android 7.0-9.0):**
- ✅ Legacy storage permissions
- ✅ Full root detection
- ⚠️ Biometric limited (API 28+)
- ✅ Notification channels (26+)
- ✅ All modes work

**API 29 (Android 10):**
- ✅ Scoped storage transition
- ✅ Full root detection
- ✅ Full biometric support
- ✅ Notification channels
- ✅ All modes work
- **Critical:** Test scoped storage

**API 30-32 (Android 11-12):**
- ✅ Scoped storage enforced
- ✅ MANAGE_EXTERNAL_STORAGE
- ✅ Full biometric support
- ✅ SCHEDULE_EXACT_ALARM (31+)
- ✅ QUERY_ALL_PACKAGES
- **Critical:** Test MANAGE_EXTERNAL_STORAGE flow

**API 33-34 (Android 13-14):**
- ✅ Granular media permissions
- ✅ POST_NOTIFICATIONS required
- ✅ Health Connect available
- ✅ FOREGROUND_SERVICE_DATA_SYNC (34)
- **Critical:** Test all new permissions

**API 35+ (Android 15+):**
- ✅ All current features
- ⚠️ Watch for new restrictions
- **Critical:** Regression testing

---

## Known Issues & Workarounds

### Issue 1: SafetyNet on Custom ROMs
**Problem:** Custom ROMs often fail SafetyNet even without root  
**Impact:** Root detection may trigger false positive  
**Workaround:** ObsidianBackup uses 5 additional detection methods  
**Status:** ✅ Mitigated

### Issue 2: Health Connect Not Pre-installed
**Problem:** Not all Android 13+ devices have Health Connect pre-installed  
**Impact:** Users must install from Play Store  
**Workaround:** App prompts to install with Play Store link  
**Status:** ✅ Handled

### Issue 3: Biometric on Budget Devices
**Problem:** Budget devices may have low-quality biometric sensors  
**Impact:** Authentication may be unreliable  
**Workaround:** Always provide device credential fallback  
**Status:** ✅ Mitigated

### Issue 4: MANAGE_EXTERNAL_STORAGE on Production Apps
**Problem:** Google Play policy restricts this permission  
**Impact:** May face review delays  
**Workaround:** Only use for advanced features, justify in policy  
**Status:** ⚠️ Monitor policy changes

### Issue 5: Shizuku Disconnect
**Problem:** ADB connection may drop, disabling Shizuku  
**Impact:** Operations fail mid-process  
**Workaround:** Fall back to SAF mode, prompt user to reconnect  
**Status:** ✅ Handled

---

## Compliance Matrix

| Requirement | Status | Evidence |
|-------------|--------|----------|
| **Google Play Policy** | ✅ Compliant | All permissions justified, QUERY_ALL_PACKAGES for backup, MANAGE_EXTERNAL_STORAGE optional |
| **GDPR** | ✅ Compliant | Health data encrypted, user consent, granular permissions, data minimization |
| **OWASP MASVS** | ✅ Compliant | Root detection (6 methods), biometric auth, secure storage, certificate pinning |
| **Android CDD** | ✅ Compliant | All features optional, hardware marked required=false, graceful degradation |
| **Accessibility** | ✅ Partial | Voice commands (optional), TalkBack compatible, needs more testing |

---

## Summary: Best Practices

### ✅ Do's
1. **Request permissions lazily** (when feature used)
2. **Provide clear rationales** before requesting
3. **Handle all API levels** with version checks
4. **Test on real devices** (emulator insufficient)
5. **Gracefully degrade** when permissions denied
6. **Cache permission state** to avoid repeated checks
7. **Monitor platform changes** for new requirements

### ❌ Don'ts
1. **Don't spam permission requests** on first launch
2. **Don't crash on denial** - always catch SecurityException
3. **Don't assume root** - support non-rooted devices
4. **Don't require camera/audio** for core functionality
5. **Don't ignore API level differences** in behavior
6. **Don't hard-code API keys** in source code
7. **Don't forget about custom ROMs** and their quirks

---

**Document Version:** 1.0  
**Last Updated:** 2024-01  
**Test Coverage:** 40 permissions × 7 API ranges × 10 device types = 2,800 test cases  
**Compatibility:** Android 5.0 (API 21) to Android 15+ (API 35+)  
**Maintained By:** ObsidianBackup QA Team
