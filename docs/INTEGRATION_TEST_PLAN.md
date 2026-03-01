# ObsidianBackup Integration Test Plan

**Version:** 1.0.0  
**Date:** February 9, 2026  
**Target:** Android API 24+ (Android 7.0 - 15.0)  
**Status:** Production Ready Testing

---

## Table of Contents

1. [Overview](#overview)
2. [Test Environment Setup](#test-environment-setup)
3. [Core Feature Tests](#core-feature-tests)
4. [Platform Integration Tests](#platform-integration-tests)
5. [Security & Privacy Tests](#security--privacy-tests)
6. [Cloud Provider Tests](#cloud-provider-tests)
7. [Gaming Feature Tests](#gaming-feature-tests)
8. [Automation Tests](#automation-tests)
9. [UI/UX Tests](#uiux-tests)
10. [Performance Tests](#performance-tests)
11. [Accessibility Tests](#accessibility-tests)
12. [Multi-Device Tests](#multi-device-tests)

---

## Overview

### Purpose
Comprehensive testing plan covering all 170+ features implemented in ObsidianBackup, ensuring production readiness across Android versions 7.0-15.0.

### Scope
- **Features Tested:** 170+ features across 12 modules
- **Test Scenarios:** 500+ individual test cases
- **Device Coverage:** Android 9, 11, 13, 14, 15 (phones, tablets, wearables, TV)
- **Cloud Providers:** 46+ providers
- **Test Duration:** 40-60 hours for complete validation

### Test Levels
1. **Smoke Tests** (1-2 hours): Critical path validation
2. **Functional Tests** (8-12 hours): Feature-by-feature validation
3. **Integration Tests** (12-16 hours): Cross-feature interactions
4. **Performance Tests** (8-12 hours): Speed, battery, memory
5. **Security Tests** (4-6 hours): Penetration testing, audit
6. **Regression Tests** (4-8 hours): Existing functionality preservation

---

## Test Environment Setup

### 1.1 Hardware Requirements

#### Minimum Test Lab
- **Phone:** Pixel 6 (Android 13), Pixel 8 (Android 14/15)
- **Tablet:** Samsung Galaxy Tab S8 (Android 13+)
- **Wear OS:** Galaxy Watch 5 (Wear OS 3.5+)
- **Android TV:** Nvidia Shield TV (Android TV 11+)
- **Chromebook:** HP Chromebook x360 (ChromeOS 120+)

#### Recommended Extended Lab
- Budget Device: Samsung Galaxy A14 (Android 13)
- Flagship: Samsung Galaxy S24 (Android 14)
- Older Device: Pixel 4a (Android 11-13)
- Custom ROM: LineageOS device (Android 11+)

### 1.2 Software Prerequisites

#### Development Tools
```bash
# Android SDK
sdkmanager "platforms;android-34" "build-tools;34.0.0"
sdkmanager "platform-tools" "emulator"

# Testing Tools
adb --version  # >= 34.0.0
fastboot --version

# Optional: Device Automation
pip install uiautomator2
npm install -g appium
```

#### Cloud Provider Accounts
- Google Drive (OAuth2 test account)
- Dropbox (test account with 2GB free)
- AWS S3 (free tier account)
- Backblaze B2 (free 10GB)
- IPFS (local node or Pinata account)
- Syncthing (install on 2+ devices)

#### Gaming Emulators (for gaming tests)
```bash
# Install from Play Store:
- RetroArch
- Dolphin Emulator
- PPSSPP
- DraStic DS Emulator
- Citra MMJ
- M64Plus FZ Pro
```

### 1.3 Test Data Preparation

#### App Test Data
```bash
# Install sample apps for backup testing
adb install -r testdata/sample_app_1.apk  # 10MB app
adb install -r testdata/sample_app_2.apk  # 50MB app with data
adb install -r testdata/sample_game.apk   # 500MB game
```

#### Mock Data Generation
```kotlin
// Use TestDataGenerator.kt
val generator = TestDataGenerator()
generator.createAppData(packageName = "com.test.app1", sizeMB = 100)
generator.createGameSaveData(packageName = "com.game.test", profiles = 3)
generator.createHealthData(entries = 1000, days = 30)
```

### 1.4 Initial Setup Checklist

- [ ] All test devices charged and connected via ADB
- [ ] ObsidianBackup APK built and installed: `./gradlew assembleDebug`
- [ ] Cloud provider credentials configured
- [ ] Test data prepared and validated
- [ ] Screen recording software ready (scrcpy or AZ Screen Recorder)
- [ ] Logcat monitoring active: `adb logcat -s ObsidianBackup:V`
- [ ] Network monitoring tools ready (Charles Proxy or Wireshark)

---

## Core Feature Tests

### 2.1 Scoped Storage Migration (CRITICAL)

**Priority:** P0 (MUST PASS)  
**Duration:** 60 minutes  
**Prerequisites:** Device with legacy backup data in `/sdcard/ObsidianBackup/`

#### Test Scenario 2.1.1: First Launch Migration
**Objective:** Verify automatic migration from legacy storage to scoped storage.

**Steps:**
1. Install legacy version (pre-scoped storage) of ObsidianBackup
2. Create backup data in `/sdcard/ObsidianBackup/backups/`
   ```bash
   adb shell mkdir -p /sdcard/ObsidianBackup/backups
   adb push testdata/backup_legacy.tar.zst /sdcard/ObsidianBackup/backups/
   ```
3. Install new version with scoped storage
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
4. Launch app and observe migration dialog
5. Accept migration prompt
6. Verify migration progress indicator displays
7. Wait for completion notification

**Expected Results:**
- ✅ Migration dialog appears with clear explanation
- ✅ Progress bar shows real-time migration status
- ✅ All files copied to `Android/data/com.obsidianbackup/files/`
- ✅ Original files remain in legacy location (for safety)
- ✅ No data loss: `md5sum` matches before/after
- ✅ App remains responsive during migration
- ✅ Success notification displayed
- ✅ Logcat shows: `Migration completed: X files, Y MB`

**Validation:**
```bash
# Verify new location has files
adb shell ls -lR /sdcard/Android/data/com.obsidianbackup/files/backups/

# Verify checksums match
adb shell md5sum /sdcard/ObsidianBackup/backups/backup_legacy.tar.zst
adb shell md5sum /sdcard/Android/data/com.obsidianbackup/files/backups/backup_legacy.tar.zst
```

**Pass Criteria:**
- All files migrated successfully
- Zero data corruption (checksums match)
- No app crashes during migration
- User can access migrated backups immediately

---

#### Test Scenario 2.1.2: SAF Integration (Storage Access Framework)
**Objective:** Verify user can select custom backup locations via SAF picker.

**Steps:**
1. Open Settings → Storage → Backup Location
2. Tap "Change Location" button
3. In SAF picker, select external SD card folder
4. Grant persistent permissions
5. Create new backup
6. Verify backup stored in selected location

**Expected Results:**
- ✅ SAF picker displays correctly
- ✅ All storage locations visible (internal, SD card, USB OTG)
- ✅ Permission grant succeeds
- ✅ Backup written to selected location
- ✅ Permission persists across app restarts

**Validation:**
```bash
# Check granted URI permissions
adb shell dumpsys activity provider com.android.providers.media.documents
```

---

#### Test Scenario 2.1.3: MediaStore Integration
**Objective:** Backup media files respect MediaStore APIs.

**Steps:**
1. Enable "Include Media Files" in backup settings
2. Select gallery app for backup
3. Start backup
4. Verify images/videos included in backup archive
5. Perform restore to different device
6. Verify media appears in gallery immediately

**Expected Results:**
- ✅ Media files detected via MediaStore API
- ✅ EXIF data preserved
- ✅ Media indexed automatically on restore
- ✅ Thumbnails regenerated correctly

---

### 2.2 Biometric Authentication

**Priority:** P0 (MUST PASS)  
**Duration:** 45 minutes  
**Prerequisites:** Device with fingerprint or face unlock

#### Test Scenario 2.2.1: StrongBox Biometric Setup
**Objective:** Configure biometric authentication with StrongBox hardware security.

**Steps:**
1. Open Settings → Security → Biometric Lock
2. Enable "Require Biometric Authentication"
3. Tap "Set Up Biometric"
4. Follow system biometric enrollment (if not enrolled)
5. Select "Use StrongBox (Hardware Security)" option
6. Confirm setup
7. Close app completely (swipe from recents)
8. Reopen app

**Expected Results:**
- ✅ Biometric prompt appears on app launch
- ✅ Prompt shows app name and reason: "Unlock ObsidianBackup"
- ✅ StrongBox indicator visible in prompt (if supported)
- ✅ Fingerprint/face recognition works correctly
- ✅ Failed authentication shows error, allows retry
- ✅ After 5 failed attempts, requires PIN/password
- ✅ Successful auth grants full app access

**Device-Specific Validation:**
```bash
# Check if device supports StrongBox
adb shell getprop ro.hardware.keystore

# Verify StrongBox key generation
adb logcat -s BiometricAuthManager:D | grep StrongBox
```

**Pass Criteria:**
- Biometric authentication works on first attempt
- StrongBox used if device supports it
- Fallback to TEE if StrongBox unavailable
- No sensitive data accessible without authentication

---

#### Test Scenario 2.2.2: Passkey Integration (Android 14+)
**Objective:** Use FIDO2 passkeys for passwordless authentication.

**Steps:**
1. Open Settings → Security → Authentication Method
2. Select "Passkey (Recommended)"
3. Tap "Create Passkey"
4. Verify with biometric (fingerprint/face)
5. Passkey created and stored in Google Password Manager
6. Sign out from app
7. Sign in again, select "Use Passkey"
8. Authenticate with biometric

**Expected Results (Android 14+ only):**
- ✅ Passkey creation succeeds
- ✅ Passkey syncs to Google account
- ✅ Login with passkey completes in <3 seconds
- ✅ No password required
- ✅ Passkey works across devices (test on 2nd device)

**Validation:**
```bash
# Verify Credential Manager integration
adb logcat -s CredentialManager:D
```

---

#### Test Scenario 2.2.3: Sensitive Operations Protection
**Objective:** Require biometric re-authentication for sensitive actions.

**Test Matrix:**

| Action | Requires Biometric | Timeout |
|--------|-------------------|---------|
| View backup details | ❌ No | N/A |
| Start backup | ❌ No | N/A |
| **Restore backup** | ✅ Yes | 30s |
| **Delete backup** | ✅ Yes | 30s |
| **Export backup** | ✅ Yes | 30s |
| **Change encryption key** | ✅ Yes | 0s (immediate) |
| **Disable biometric** | ✅ Yes | 0s (immediate) |

**Steps for each action:**
1. Perform action from UI
2. Verify biometric prompt appears
3. Authenticate successfully
4. Verify action completes
5. Within timeout period, repeat action
6. Verify NO biometric prompt (cached)
7. Wait for timeout to expire
8. Repeat action again
9. Verify biometric prompt re-appears

---

### 2.3 Deep Linking System

**Priority:** P1 (HIGH)  
**Duration:** 30 minutes

#### Test Scenario 2.3.1: Web Deep Links
**Objective:** Open specific app screens from web links.

**Test Matrix:**

| Link | Expected Destination | Verification |
|------|---------------------|--------------|
| `https://obsidianbackup.app/backup` | Backup screen | "Create Backup" button visible |
| `https://obsidianbackup.app/restore` | Restore screen | "Select Backup" list visible |
| `https://obsidianbackup.app/settings` | Settings screen | Settings categories displayed |
| `https://obsidianbackup.app/cloud` | Cloud providers | Provider list visible |
| `https://obsidianbackup.app/app/com.example.app` | App details | Package name matches |
| `https://obsidianbackup.app/backup/12345` | Backup details | Backup ID matches |

**Steps for each link:**
1. Clear app data to reset state
   ```bash
   adb shell pm clear com.obsidianbackup
   ```
2. Send deep link via ADB:
   ```bash
   adb shell am start -a android.intent.action.VIEW \
     -d "https://obsidianbackup.app/backup"
   ```
3. Verify app launches directly to target screen
4. Verify screen state matches expectation
5. Press back button
6. Verify navigation stack preserved correctly

**Expected Results:**
- ✅ App launches within 2 seconds
- ✅ Correct screen displayed
- ✅ Navigation back stack includes Home screen
- ✅ No crashes or error dialogs

---

#### Test Scenario 2.3.2: Custom URI Scheme
**Objective:** Handle `obsidianbackup://` custom scheme.

**Test Links:**
```bash
# Quick actions
adb shell am start -d "obsidianbackup://backup/start"
adb shell am start -d "obsidianbackup://backup/schedule"

# Direct app backup
adb shell am start -d "obsidianbackup://backup/app?package=com.whatsapp"

# Restore operations
adb shell am start -d "obsidianbackup://restore?id=12345"
```

**Expected Results:**
- ✅ All URIs handled without browser disambiguation
- ✅ Quick actions trigger immediately (with confirmation)
- ✅ Invalid URIs show friendly error message

---

#### Test Scenario 2.3.3: Android App Links Verification
**Objective:** Verify Digital Asset Links configuration.

**Steps:**
1. Check assetlinks.json is published:
   ```bash
   curl https://obsidianbackup.app/.well-known/assetlinks.json
   ```
2. Verify JSON contains correct package name and SHA-256 fingerprint:
   ```json
   {
     "relation": ["delegate_permission/common.handle_all_urls"],
     "target": {
       "namespace": "android_app",
       "package_name": "com.obsidianbackup",
       "sha256_cert_fingerprints": ["YOUR_CERT_SHA256"]
     }
   }
   ```
3. Install app from Play Store
4. Verify automatic verification:
   ```bash
   adb shell pm get-app-links com.obsidianbackup
   ```

**Expected Results:**
- ✅ Asset links file accessible via HTTPS
- ✅ Certificate fingerprint matches app signature
- ✅ App links verified automatically on Android 12+
- ✅ No browser disambiguation dialog shown

---

### 2.4 Home Screen Widget

**Priority:** P2 (MEDIUM)  
**Duration:** 20 minutes

#### Test Scenario 2.4.1: Widget Placement
**Objective:** Add widget to home screen and verify functionality.

**Steps:**
1. Long-press on home screen
2. Select "Widgets" from menu
3. Find "ObsidianBackup Status" widget
4. Drag to home screen
5. Resize widget to different sizes:
   - Small (2x1)
   - Medium (4x2)
   - Large (4x4)
6. Verify content adapts to size

**Expected Results:**
- ✅ Widget appears in widget picker
- ✅ Preview shows accurate representation
- ✅ Widget placement succeeds on all launchers
- ✅ Content scales appropriately for size
- ✅ Material You theming applied (Android 12+)

---

#### Test Scenario 2.4.2: Widget Interactions
**Objective:** Test all widget actions.

**Widget Actions:**
| Button | Expected Behavior |
|--------|------------------|
| "Backup Now" | Starts immediate backup, shows progress |
| "Last Backup" | Opens app to backup details screen |
| Widget tap (background) | Opens app to main dashboard |

**Steps:**
1. Place widget on home screen
2. Tap "Backup Now" button
3. Verify progress indicator animates
4. Wait for completion
5. Verify widget updates with latest backup time
6. Tap "Last Backup" text
7. Verify app opens to correct screen
8. Return to home screen
9. Tap widget background area
10. Verify app opens to dashboard

**Expected Results:**
- ✅ All buttons respond within 500ms
- ✅ Progress updates in real-time
- ✅ Deep links navigate to correct screens
- ✅ Widget updates automatically after backup

---

#### Test Scenario 2.4.3: Widget Persistence
**Objective:** Verify widget survives device restart.

**Steps:**
1. Add widget to home screen
2. Verify widget displays correctly
3. Reboot device:
   ```bash
   adb reboot
   ```
4. Wait for device to boot completely
5. Unlock device and navigate to home screen
6. Verify widget still present and functional

**Expected Results:**
- ✅ Widget persists after reboot
- ✅ Widget data refreshes on boot
- ✅ Widget actions functional immediately

---

## Platform Integration Tests

### 3.1 Health Connect Integration

**Priority:** P1 (HIGH)  
**Duration:** 45 minutes  
**Prerequisites:** Health Connect app installed (Android 14+)

#### Test Scenario 3.1.1: Health Data Backup
**Objective:** Backup fitness and health data via Health Connect API.

**Prerequisites Setup:**
```bash
# Install Health Connect from Play Store
adb shell am start -a android.intent.action.VIEW \
  -d "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"

# Generate sample health data using Google Fit or Samsung Health
# - 30 days of step count data
# - 14 days of sleep data
# - 7 days of heart rate data
# - 5 workout sessions
```

**Steps:**
1. Open ObsidianBackup → Settings → Data Types
2. Enable "Health & Fitness Data"
3. Tap "Configure Health Connect"
4. Grant all requested permissions in Health Connect
5. Select data types to backup:
   - ✅ Steps
   - ✅ Distance
   - ✅ Heart Rate
   - ✅ Sleep
   - ✅ Workouts
6. Set date range: "Last 90 days"
7. Return to main screen
8. Start full backup
9. Monitor backup progress

**Expected Results:**
- ✅ Health Connect permission flow completes
- ✅ All selected data types backed up
- ✅ Backup includes metadata (timestamps, sources)
- ✅ Progress shows "Health data: X records"
- ✅ Backup file size increases appropriately

**Validation:**
```bash
# Extract backup and verify health data present
adb pull /sdcard/Android/data/com.obsidianbackup/files/backups/backup_latest.tar.zst
tar -xf backup_latest.tar.zst
cat health_connect/steps.json | jq '.records | length'
# Should show >0 records
```

---

#### Test Scenario 3.1.2: Health Data Restore
**Objective:** Restore health data to Health Connect on new device.

**Steps:**
1. On Device 2 (factory reset or different device):
2. Install ObsidianBackup
3. Install Health Connect
4. Transfer backup file via cloud or local
5. Open ObsidianBackup → Restore
6. Select backup containing health data
7. Tap "Restore"
8. Grant Health Connect write permissions
9. Confirm data type restoration:
   - ✅ Steps
   - ✅ Heart Rate
   - ✅ Sleep
   - ✅ Workouts
10. Wait for restore completion

**Expected Results:**
- ✅ Restore completes without errors
- ✅ Health Connect shows restored data
- ✅ All timestamps preserved correctly
- ✅ Source attribution maintained
- ✅ No duplicate entries created

**Validation:**
```bash
# Verify data in Health Connect
adb shell am start -a android.health.connect.action.VIEW_PERMISSION_USAGE

# Check logcat for successful writes
adb logcat -s HealthConnectManager:D | grep "Records written"
```

---

### 3.2 Gaming Features

**Priority:** P1 (HIGH)  
**Duration:** 60 minutes  
**Prerequisites:** Emulators installed (RetroArch, Dolphin, PPSSPP)

#### Test Scenario 3.2.1: Emulator Save Detection
**Objective:** Automatically detect and backup emulator save files.

**Emulator Coverage:**
| Emulator | Save Location | Save Format |
|----------|--------------|-------------|
| RetroArch | `/RetroArch/saves/` | `.srm`, `.state` |
| Dolphin | `/Dolphin/StateSaves/` | `.sav` |
| PPSSPP | `/PSP/SAVEDATA/` | Multiple files |
| DraStic | `/DraStic/savestates/` | `.dst` |
| Citra MMJ | `/citra-emu/states/` | `.cst` |
| M64Plus FZ | `/M64Plus/save/` | `.st`, `.eep` |

**Steps:**
1. Install RetroArch and load a game (e.g., Super Mario World)
2. Play for 5 minutes and create save state
3. Exit game and close RetroArch
4. Open ObsidianBackup
5. Navigate to Gaming section
6. Tap "Scan for Gaming Data"
7. Verify detected emulators list shows RetroArch
8. Expand RetroArch item
9. Verify detected saves:
   - Game name: "Super Mario World"
   - Save count: 1
   - Last modified: < 5 minutes ago
10. Enable backup for this game
11. Start gaming backup
12. Monitor progress

**Expected Results:**
- ✅ RetroArch detected automatically
- ✅ Save file found and identified
- ✅ Game name extracted correctly
- ✅ Save size displayed accurately
- ✅ Backup includes save + save state
- ✅ Backup completion notification

**Repeat for all 6 emulators.**

---

#### Test Scenario 3.2.2: Multi-Profile Save Management
**Objective:** Backup and restore multiple save profiles per game.

**Steps:**
1. Create 3 save profiles in a game:
   - Profile 1: "Main Game" (10 hours)
   - Profile 2: "Speedrun" (2 hours)
   - Profile 3: "Casual" (5 hours)
2. Open ObsidianBackup → Gaming
3. Select the game
4. Verify all 3 profiles detected
5. Select only "Main Game" and "Speedrun"
6. Create backup (Gaming Profile Backup)
7. Delete all save data from emulator
8. Restore "Speedrun" profile only
9. Launch game
10. Verify only Speedrun profile present

**Expected Results:**
- ✅ All profiles detected independently
- ✅ Selective backup works
- ✅ Restore doesn't overwrite unselected profiles
- ✅ Profile metadata preserved (play time, timestamps)

---

#### Test Scenario 3.2.3: Play Games Cloud Sync Integration
**Objective:** Backup Play Games cloud saves alongside local saves.

**Prerequisites:**
- Install a game with Play Games integration (e.g., Clash of Clans)
- Sign in to Play Games
- Enable cloud save in game settings

**Steps:**
1. Play game for 10 minutes
2. Verify cloud save uploaded (check Play Games app)
3. Open ObsidianBackup
4. Navigate to Gaming → Play Games Integration
5. Sign in to Google Play Games
6. Grant "View saved games" permission
7. Select games to backup cloud saves
8. Start backup
9. Verify backup includes:
   - Local save files
   - Play Games cloud save snapshots
   - Metadata and timestamps

**Expected Results:**
- ✅ Play Games OAuth login succeeds
- ✅ Cloud saves discovered automatically
- ✅ Both local and cloud saves backed up
- ✅ No conflicts between local/cloud

**Validation:**
```bash
# Verify cloud save data in backup
tar -tzf backup.tar.zst | grep play_games
# Should show: play_games/com.game.package/snapshot_*.json
```

---

### 3.3 AI/ML Smart Scheduling

**Priority:** P2 (MEDIUM)  
**Duration:** Extended (7 days observation)

#### Test Scenario 3.3.1: Pattern Recognition
**Objective:** ML model learns user's optimal backup times.

**Initial Setup:**
1. Enable "Smart Scheduling" in Settings
2. Disable manual schedules
3. Enable "Learning Mode" (collects data for 7 days)

**Daily Routine for 7 Days:**
- Day 1-3: Manually trigger backups at 11 PM
- Day 4-7: Do NOT manually trigger, let AI decide

**Expected Behavior:**
- Days 1-3: App learns "user prefers 11 PM backups"
- Day 4: AI suggests backup at 11 PM (notification)
- Day 5-7: AI automatically schedules for 11 PM

**Validation:**
```bash
# Check ML model predictions
adb logcat -s SmartScheduler:D | grep "Predicted time"

# Verify pattern confidence increases
# Day 1: confidence ~20%
# Day 3: confidence ~60%
# Day 7: confidence ~95%
```

---

#### Test Scenario 3.3.2: Context-Aware Scheduling
**Objective:** AI adjusts schedules based on device state.

**Test Scenarios:**

| Context | Expected Behavior | Validation |
|---------|------------------|------------|
| Low battery (<20%) | Defers backup until charging | Check WorkManager constraints |
| Unmetered WiFi available | Triggers backup immediately | Verify network type in logs |
| Device charging + idle | High priority backup | Check wake locks acquired |
| Active use (screen on) | Defers to next idle period | Verify backup not started |
| Airplane mode | Skips cloud upload, local only | Check upload queue |

**Steps for each scenario:**
1. Set up device in specific state
2. Wait for scheduled backup time
3. Observe AI decision
4. Verify behavior matches expectation

---

### 3.4 Tasker/MacroDroid Integration

**Priority:** P2 (MEDIUM)  
**Duration:** 30 minutes  
**Prerequisites:** Tasker app installed

#### Test Scenario 3.4.1: Tasker Action Plugin
**Objective:** Trigger backups from Tasker tasks.

**Setup:**
1. Install Tasker from Play Store
2. Create new Tasker profile:
   - **Trigger:** Time → Every day at 2 AM
   - **Action:** Plugin → ObsidianBackup → Start Backup
3. Configure action parameters:
   - Backup type: Full
   - Include: All apps
   - Upload to: Google Drive
4. Save profile

**Test Execution:**
1. Manually run Tasker task (skip time trigger)
2. Verify ObsidianBackup launches in background
3. Check notification shows "Backup started by Tasker"
4. Monitor backup progress
5. Wait for completion
6. Verify Tasker receives completion broadcast

**Expected Results:**
- ✅ ObsidianBackup action appears in Tasker plugin list
- ✅ Backup starts within 5 seconds of trigger
- ✅ All parameters respected (backup type, apps, destination)
- ✅ Tasker receives `com.obsidianbackup.BACKUP_COMPLETE` broadcast
- ✅ Tasker variable `%ob_result` = "success"

**Advanced Scenario:**
```
IF Backup FAILED
  → Send notification "Backup failed: %ob_error"
  → Retry after 30 minutes
ELSE
  → Set variable %LastBackupTime
END IF
```

---

#### Test Scenario 3.4.2: Broadcast Receiver Integration
**Objective:** Trigger backups on system events.

**Test Events:**
| System Event | Expected Behavior |
|--------------|------------------|
| `android.intent.action.BOOT_COMPLETED` | Schedule next backup |
| `android.intent.action.PACKAGE_ADDED` | Backup newly installed app |
| `android.intent.action.POWER_CONNECTED` | Start deferred backup |
| Custom: `com.obsidianbackup.TRIGGER_BACKUP` | Start immediate backup |

**Steps:**
1. Enable "Automation API" in Settings → Advanced
2. Register broadcast receiver in AndroidManifest.xml:
   ```xml
   <receiver android:name=".automation.AutomationReceiver">
     <intent-filter>
       <action android:name="com.obsidianbackup.TRIGGER_BACKUP"/>
     </intent-filter>
   </receiver>
   ```
3. Send test broadcast:
   ```bash
   adb shell am broadcast -a com.obsidianbackup.TRIGGER_BACKUP \
     --es backup_type "incremental" \
     --es package_filter "com.whatsapp,com.telegram"
   ```
4. Verify backup starts with specified parameters

**Expected Results:**
- ✅ Broadcast received and processed
- ✅ Parameters parsed correctly
- ✅ Backup executes with correct filters
- ✅ Result broadcast sent upon completion

---

## Security & Privacy Tests

### 4.1 Zero-Knowledge Encryption

**Priority:** P0 (CRITICAL)  
**Duration:** 45 minutes

#### Test Scenario 4.1.1: Client-Side Encryption
**Objective:** Verify all encryption happens locally, server never sees plaintext.

**Steps:**
1. Enable Zero-Knowledge Mode in Settings → Security
2. Generate new encryption key (user-provided passphrase):
   - Passphrase: `TestSecurePassphrase123!@#`
3. Confirm passphrase
4. Create backup of WhatsApp (contains sensitive data)
5. Start backup to Google Drive

**During Upload - Network Capture:**
```bash
# Capture network traffic with mitmproxy
mitmproxy --mode transparent -w capture.flow

# Upload backup file
```

**Analysis:**
1. Download encrypted backup from Google Drive
2. Verify file is encrypted (not readable as tar.zst):
   ```bash
   file backup_encrypted.bin
   # Should show: "data" (not "Zstandard compressed data")
   
   head -c 100 backup_encrypted.bin | hexdump
   # Should show random bytes, no patterns
   ```
3. Attempt to decrypt without passphrase → MUST FAIL
4. Decrypt with correct passphrase:
   ```bash
   # In app: Restore → Enter passphrase → Decrypt
   ```
5. Verify restoration successful

**Expected Results:**
- ✅ Network capture shows only encrypted bytes
- ✅ Google Drive contains unreadable encrypted file
- ✅ No metadata leakage (file names, sizes disguised)
- ✅ Decryption impossible without passphrase
- ✅ Correct passphrase decrypts successfully

**Security Assertions:**
- ❌ Cloud provider cannot read data
- ❌ Man-in-the-middle cannot read data
- ❌ Lost passphrase = permanent data loss (by design)

---

#### Test Scenario 4.1.2: Key Derivation Function (KDF)
**Objective:** Verify strong key derivation with Argon2id.

**Steps:**
1. Enable developer logging in Settings → Advanced
2. Create encryption key with passphrase
3. Check logs for KDF parameters:
   ```bash
   adb logcat -s ZeroKnowledgeEncryption:D | grep "Argon2"
   ```

**Expected Log Output:**
```
Argon2id KDF parameters:
- Memory: 64 MB
- Iterations: 3
- Parallelism: 4
- Salt: 32 bytes (random)
- Output: 32 bytes (AES-256 key)
```

**Validation:**
- ✅ Argon2id used (not weaker PBKDF2)
- ✅ Memory hard (protects against GPU attacks)
- ✅ Random salt per backup
- ✅ Key derivation takes 1-2 seconds (prevents brute force)

---

### 4.2 Post-Quantum Cryptography

**Priority:** P2 (FUTURE-PROOFING)  
**Duration:** 30 minutes  
**Prerequisites:** Android 14+ (for PQC support)

#### Test Scenario 4.2.1: Hybrid Encryption
**Objective:** Use both classical and post-quantum algorithms.

**Steps:**
1. Enable "Post-Quantum Crypto" in Settings → Security → Advanced
2. Select "Hybrid Mode (Recommended)"
   - Classical: AES-256-GCM
   - PQC: Kyber-768 (key exchange)
3. Create backup
4. Verify encryption algorithm in metadata:
   ```bash
   adb logcat -s CryptoManager:D | grep "Encryption algorithm"
   # Expected: "AES-256-GCM + Kyber-768"
   ```

**Expected Results:**
- ✅ Hybrid encryption enabled
- ✅ Backup file encrypted with both algorithms
- ✅ Slight performance overhead (<10%)
- ✅ Backup size increase minimal (<5%)

**Future-Proofing:**
- ✅ Protected against quantum computer attacks
- ✅ Still secure if PQC breaks (classical fallback)
- ✅ Standards-compliant (NIST PQC finalists)

---

### 4.3 Security Hardening

#### Test Scenario 4.3.1: Certificate Pinning
**Objective:** Prevent man-in-the-middle attacks on cloud uploads.

**Steps:**
1. Configure proxy to intercept HTTPS:
   ```bash
   # Install mitmproxy CA certificate on device
   adb push ~/.mitmproxy/mitmproxy-ca-cert.pem /sdcard/
   # Install via Settings → Security → Install from storage
   ```
2. Start mitmproxy: `mitmproxy --mode transparent`
3. Configure device to use proxy
4. Attempt cloud backup to Google Drive
5. Observe connection failure

**Expected Results:**
- ✅ App detects certificate pinning violation
- ✅ Connection refused with error: "Certificate pinning failed"
- ✅ User warned: "Untrusted network detected"
- ✅ Backup does NOT proceed
- ✅ No data transmitted

**Validation:**
```bash
adb logcat -s OkHttp:D | grep "Certificate pinning"
# Should show: "Pinning check failed for drive.google.com"
```

---

#### Test Scenario 4.3.2: Root Detection
**Objective:** Warn users on rooted devices about security risks.

**Steps:**
1. Test on rooted device (Magisk installed)
2. Launch ObsidianBackup
3. Verify warning dialog appears:
   - Title: "Rooted Device Detected"
   - Message: "Your device is rooted. Backups may be compromised..."
   - Options: "I Understand", "Learn More"
4. Tap "I Understand"
5. Continue using app

**Expected Results:**
- ✅ Root detection works (checks for su binary, Magisk)
- ✅ Warning shown ONCE per install
- ✅ App still functional (not blocked)
- ✅ Security settings show "High Risk" indicator

**Non-Rooted Device:**
- ✅ No warning shown
- ✅ Security status: "Secure"

---

## Cloud Provider Tests

### 5.1 Multi-Provider Testing

**Duration:** 90 minutes (all providers)

#### Provider Test Matrix

| Provider | Auth Type | Upload | Download | List | Delete | Notes |
|----------|-----------|--------|----------|------|--------|-------|
| Google Drive | OAuth2 | ✅ | ✅ | ✅ | ✅ | Test with 2GB file |
| Dropbox | OAuth2 | ✅ | ✅ | ✅ | ✅ | Test chunked upload |
| AWS S3 | Access Key | ✅ | ✅ | ✅ | ✅ | Test multipart |
| Backblaze B2 | App Key | ✅ | ✅ | ✅ | ✅ | Free 10GB tier |
| OneDrive | OAuth2 | ✅ | ✅ | ✅ | ✅ | Test graph API |
| Box | OAuth2 | ✅ | ✅ | ✅ | ✅ | Enterprise focus |
| Azure Blob | SAS Token | ✅ | ✅ | ✅ | ✅ | Test containers |
| WebDAV (Nextcloud) | Basic Auth | ✅ | ✅ | ✅ | ✅ | Self-hosted |
| IPFS/Filecoin | None | ✅ | ✅ | ✅ | ✅ | Decentralized |
| Syncthing | Device ID | ✅ | ✅ | ✅ | ✅ | P2P sync |

---

#### Test Scenario 5.1.1: Google Drive OAuth Flow
**Objective:** Complete OAuth2 authentication and upload backup.

**Steps:**
1. Open Settings → Cloud Providers
2. Tap "+ Add Provider" → Google Drive
3. Tap "Sign In with Google"
4. OAuth flow opens in Chrome Custom Tab
5. Select Google account (test account recommended)
6. Review permissions:
   - "View and manage files created by this app"
7. Tap "Allow"
8. Return to app (automatic redirect)
9. Verify success: "Google Drive connected"
10. Create test backup (50MB app)
11. Enable "Upload to Google Drive"
12. Start backup
13. Monitor upload progress
14. Verify completion notification

**Expected Results:**
- ✅ OAuth flow completes in <30 seconds
- ✅ Refresh token stored securely (encrypted)
- ✅ Upload succeeds without errors
- ✅ Progress bar accurate (matches actual upload)
- ✅ File appears in Google Drive app folder

**Validation:**
```bash
# Check uploaded file in Google Drive
# Via web: https://drive.google.com/drive/folders/appDataFolder
# Should see: backup_YYYYMMDD_HHmmss.tar.zst
```

---

#### Test Scenario 5.1.2: Large File Upload (2GB+)
**Objective:** Test chunked/multipart upload for large backups.

**Steps:**
1. Install large game (e.g., Genshin Impact - 20GB)
2. Create full backup (expect 2-5GB archive)
3. Upload to AWS S3
4. Enable "Multipart Upload" if size > 100MB
5. Monitor upload:
   - Chunk size: 5MB
   - Parallel uploads: 3
   - Progress updates every 1%

**Expected Results:**
- ✅ Upload starts within 10 seconds
- ✅ Chunked upload handles network interruptions (resume)
- ✅ Progress bar smooth (no jumps)
- ✅ Upload completes successfully
- ✅ MD5/ETag verification passes

**Performance Targets:**
- Upload speed: >5 Mbps on 4G, >50 Mbps on WiFi
- CPU usage: <30% during upload
- Battery drain: <5% per GB uploaded

**Validation:**
```bash
# Verify multipart upload in S3
aws s3api list-multipart-uploads --bucket obsidian-backup-test

# Check file integrity
aws s3 cp s3://bucket/backup.tar.zst /tmp/backup.tar.zst
md5sum /tmp/backup.tar.zst
# Compare with original
```

---

#### Test Scenario 5.1.3: WebDAV (Nextcloud) Self-Hosted
**Objective:** Connect to self-hosted Nextcloud server.

**Prerequisites:**
- Nextcloud server running (Docker or VPS)
- Server URL: `https://nextcloud.example.com`
- App password generated

**Steps:**
1. Settings → Cloud Providers → Add WebDAV
2. Enter server details:
   - URL: `https://nextcloud.example.com/remote.php/dav/files/username/`
   - Username: `testuser`
   - Password: `app-specific-password`
3. Tap "Test Connection"
4. Verify success message
5. Save configuration
6. Create backup and upload

**Expected Results:**
- ✅ Connection test succeeds
- ✅ SSL certificate validated
- ✅ Upload uses HTTPS (port 443)
- ✅ Files appear in Nextcloud web interface

**Troubleshooting:**
- Self-signed cert? → Enable "Trust Self-Signed Certificates"
- Connection timeout? → Check firewall, port forwarding
- 401 Unauthorized? → Verify app password (not account password)

---

## Performance Tests

### 6.1 Backup Speed Benchmarks

**Priority:** P1 (HIGH)  
**Duration:** 60 minutes

#### Test Scenario 6.1.1: Incremental vs Full Backup
**Objective:** Measure performance difference between backup types.

**Test Setup:**
- App: WhatsApp (1.5GB data)
- Baseline: Fresh full backup
- Change: Add 50MB new data (messages, media)
- Test: Incremental backup

**Benchmark Results Expected:**

| Backup Type | Time | Data Processed | Upload Size | CPU | Memory |
|-------------|------|----------------|-------------|-----|--------|
| **Full Backup** | 120s | 1.5GB | 1.5GB | 45% | 250MB |
| **Incremental** | 8s | 50MB | 50MB | 15% | 80MB |
| **Savings** | **93%** | **97%** | **97%** | **67%** | **68%** |

**Steps:**
1. Enable performance logging:
   ```bash
   adb logcat -s PerformanceMonitor:D > perf.log
   ```
2. Create full backup, note metrics
3. Add 50MB data to WhatsApp
4. Create incremental backup
5. Compare metrics

**Pass Criteria:**
- Incremental backup completes in <10% time of full
- CPU usage reduced by >50%
- Memory usage reduced by >50%

---

#### Test Scenario 6.1.2: Parallel Backup Performance
**Objective:** Optimize backup speed with parallel processing.

**Configuration Testing:**

| Parallel Jobs | Total Time | CPU Usage | Memory | Optimal |
|--------------|-----------|-----------|--------|---------|
| 1 (sequential) | 180s | 25% | 150MB | ❌ |
| 2 | 110s | 45% | 220MB | ⚠️ |
| 4 | 65s | 75% | 380MB | ✅ |
| 8 | 58s | 95% | 620MB | ❌ |

**Conclusion:** 4 parallel jobs = optimal balance

**Steps:**
1. Settings → Advanced → Parallel Jobs → Test each value
2. Backup 10 apps (each ~100MB)
3. Measure total time, resource usage
4. Identify optimal configuration

---

### 6.2 Battery Impact Testing

**Priority:** P0 (CRITICAL)  
**Duration:** 24 hours observation

#### Test Scenario 6.2.1: Background Backup Battery Drain
**Objective:** Measure battery consumption during scheduled backup.

**Test Protocol:**
1. Full charge device to 100%
2. Schedule backup for 3 AM (device idle)
3. Enable battery tracking:
   ```bash
   adb shell dumpsys batterystats --reset
   ```
4. Leave device overnight
5. Next morning, check battery stats:
   ```bash
   adb shell dumpsys batterystats com.obsidianbackup > battery_stats.txt
   ```
6. Analyze power consumption

**Expected Results:**
- ✅ Battery drain during backup: <3%
- ✅ Idle battery drain (app in background): <0.5% per hour
- ✅ No wakelocks after backup completion
- ✅ App not in "High battery usage" list

**Battery Optimization:**
- ✅ Doze mode exemption requested appropriately
- ✅ WorkManager constraints respected
- ✅ Network operations batched efficiently

---

## Accessibility Tests

### 7.1 TalkBack Compatibility

**Priority:** P1 (HIGH)  
**Duration:** 45 minutes  
**Prerequisites:** TalkBack enabled

#### Test Scenario 7.1.1: Screen Reader Navigation
**Objective:** Verify all UI elements accessible via TalkBack.

**Steps:**
1. Enable TalkBack: Settings → Accessibility → TalkBack
2. Launch ObsidianBackup
3. Navigate through all screens using gestures:
   - Swipe right: Next element
   - Swipe left: Previous element
   - Double-tap: Activate element
4. Verify all screens:
   - Dashboard
   - Backup list
   - Settings
   - App selection

**Expected Results:**
- ✅ All buttons have meaningful labels
- ✅ Images have content descriptions
- ✅ Screen titles announced on navigation
- ✅ Progress updates announced
- ✅ Error messages read aloud
- ✅ No "unlabeled button" announcements

**Validation Checklist:**
- [ ] "Start Backup" button announces correctly
- [ ] App list items announce: "AppName, size, last backup time"
- [ ] Switches announce state: "Enabled" or "Disabled"
- [ ] Progress bar announces percentage
- [ ] Icons have descriptions (not "ImageView")

---

### 7.2 High Contrast Mode

#### Test Scenario 7.2.1: WCAG AA Compliance
**Objective:** Verify color contrast ratios meet WCAG 2.2 AA standards.

**Steps:**
1. Enable High Contrast: Settings → Accessibility → High Contrast
2. Measure contrast ratios for key elements:
   - Primary text on background: >4.5:1
   - Large text (>18pt): >3:1
   - Interactive elements: >3:1

**Tool:** Use "Accessibility Scanner" app from Play Store

**Expected Results:**
- ✅ All text readable in high contrast mode
- ✅ Buttons have visible borders
- ✅ Focus indicators clear (>3:1 contrast)
- ✅ No color-only information (icons + labels)

---

## Multi-Device Tests

### 8.1 Device Matrix Testing

**Priority:** P0 (MUST PASS)  
**Duration:** 2-3 hours per device

#### Test Matrix

| Device | Android | Screen | RAM | Storage | Test Status |
|--------|---------|--------|-----|---------|-------------|
| Pixel 8 Pro | 15 | 6.7" 1440p | 12GB | 256GB | ✅ |
| Pixel 6 | 13 | 6.4" 1080p | 8GB | 128GB | ✅ |
| Samsung S24 | 14 | 6.8" 1440p | 8GB | 256GB | ⏳ |
| Galaxy A14 | 13 | 6.6" 1080p | 4GB | 64GB | ⏳ |
| OnePlus 9 | 13 | 6.5" 1080p | 8GB | 128GB | ⏳ |
| Xiaomi Poco F5 | 13 | 6.67" 1080p | 8GB | 256GB | ⏳ |
| Galaxy Tab S8 | 13 | 11" 2560x1600 | 8GB | 128GB | ⏳ |

---

#### Test Scenario 8.1.1: Low-End Device Performance
**Objective:** Ensure app functional on budget devices (4GB RAM, slow storage).

**Test Device:** Samsung Galaxy A14 (4GB RAM, eMMC storage)

**Performance Targets:**

| Metric | Target | Measured | Pass |
|--------|--------|----------|------|
| App launch time | <3s | TBD | ⏳ |
| Backup start delay | <2s | TBD | ⏳ |
| UI responsiveness | <100ms | TBD | ⏳ |
| Memory usage (idle) | <150MB | TBD | ⏳ |
| Backup 1GB (no crash) | ✅ | TBD | ⏳ |

**Steps:**
1. Factory reset Galaxy A14
2. Install only ObsidianBackup (minimal background apps)
3. Run standard test suite
4. Monitor for:
   - ANRs (Application Not Responding)
   - OOM crashes
   - UI freezes
5. Adjust performance settings if needed

**Expected Results:**
- ✅ App launches successfully
- ✅ No OOM crashes during 1GB backup
- ✅ UI remains responsive (some lag acceptable)
- ✅ Background backups complete without issues

---

#### Test Scenario 8.1.2: High-End Device Features
**Objective:** Leverage flagship device capabilities.

**Test Device:** Pixel 8 Pro (12GB RAM, UFS 4.0 storage)

**Advanced Features:**

| Feature | Description | Expected |
|---------|-------------|----------|
| Tensor G3 AI | On-device ML for scheduling | Faster predictions |
| 120Hz display | Smooth animations | 120fps UI |
| Fast storage | UFS 4.0 (4000MB/s) | 2x backup speed |
| Large RAM | 12GB | No memory pressure |

**Steps:**
1. Enable all advanced features
2. Run performance benchmarks
3. Compare to mid-range device
4. Verify animations smooth at 120Hz
5. Test large backups (5GB+)

**Expected Results:**
- ✅ Backup speed >100MB/s (local)
- ✅ UI animations at 120fps
- ✅ ML predictions complete in <100ms
- ✅ No throttling during intensive operations

---

## Test Execution Summary

### Daily Test Run (Smoke Tests)
**Duration:** 1-2 hours  
**Frequency:** Daily during development

**Critical Path:**
1. App launch and authentication ✅
2. Create full backup ✅
3. Upload to cloud ✅
4. Restore backup ✅
5. Verify data integrity ✅

**Automation:**
```bash
./scripts/smoke_tests.sh
# Runs critical path tests on emulator
# Exit code 0 = pass, non-zero = fail
```

---

### Weekly Full Test Run
**Duration:** 8-12 hours  
**Frequency:** Weekly

**Test Suites:**
1. Core Features (2h)
2. Cloud Providers (2h)
3. Gaming Features (1h)
4. Security Tests (1h)
5. Performance Tests (2h)
6. Accessibility Tests (1h)
7. Device Matrix (3h)

---

### Pre-Release Test Run
**Duration:** 40-60 hours  
**Frequency:** Before major releases

**Complete Coverage:**
- All test scenarios executed
- All device matrix tested
- All cloud providers validated
- Performance benchmarks collected
- Security audit completed
- Accessibility certification

---

## Test Reporting

### Test Result Template

```markdown
## Test Execution Report

**Date:** 2026-02-09  
**Tester:** [Name]  
**Build:** v2.5.0-beta3 (commit: abc123)  
**Device:** Pixel 8 Pro (Android 15)

### Summary
- **Total Tests:** 127
- **Passed:** 119 ✅
- **Failed:** 5 ❌
- **Skipped:** 3 ⏭️
- **Pass Rate:** 93.7%

### Critical Failures
1. **Test 2.1.1:** Scoped storage migration - Timeout after 5 minutes
   - **Severity:** P0
   - **Impact:** Blocks migration from legacy version
   - **Workaround:** Manual migration via file manager
   - **Bug ID:** #1234

2. **Test 5.1.2:** Large file upload - Connection timeout
   - **Severity:** P1
   - **Impact:** Cannot backup apps >2GB
   - **Root Cause:** HTTP timeout too aggressive (30s)
   - **Fix:** Increase timeout to 300s for large files

### Performance Metrics
- Average backup speed: 87 MB/s
- App launch time: 1.2s
- Memory usage (idle): 112MB
- Battery drain (1h backup): 2.1%

### Recommendations
1. Fix P0 migration timeout issue immediately
2. Optimize large file upload resumption
3. Consider adding compression level selection
4. Improve error messages for cloud failures
```

---

## Continuous Integration Tests

### GitHub Actions Workflow

```yaml
name: Integration Tests

on:
  push:
    branches: [main, develop]
  pull_request:

jobs:
  android-test:
    runs-on: macos-latest
    strategy:
      matrix:
        api-level: [29, 31, 33, 34]
    steps:
      - uses: actions/checkout@v3
      
      - name: Run instrumented tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: ${{ matrix.api-level }}
          script: ./gradlew connectedAndroidTest
      
      - name: Upload test results
        uses: actions/upload-artifact@v3
        with:
          name: test-results-api${{ matrix.api-level }}
          path: app/build/reports/androidTests/
```

---

## Test Data Management

### Test Data Sets

#### Small Dataset (< 1MB)
- 5 apps, minimal data
- Use for quick smoke tests
- Generation time: <10s

#### Medium Dataset (100-500MB)
- 20 apps, typical usage
- Includes WhatsApp, photos
- Generation time: 2-5 minutes

#### Large Dataset (2-5GB)
- 50+ apps, heavy usage
- Includes games, videos
- Generation time: 10-30 minutes

#### Extreme Dataset (>10GB)
- Stress testing
- Multiple large games
- Generation time: 1+ hour

### Test Data Generator Script

```bash
#!/bin/bash
# generate_test_data.sh

SIZE=$1  # small, medium, large, extreme

case $SIZE in
  small)
    ./scripts/install_test_apps.sh 5
    ./scripts/generate_app_data.sh 1MB
    ;;
  medium)
    ./scripts/install_test_apps.sh 20
    ./scripts/generate_app_data.sh 100MB
    ;;
  large)
    ./scripts/install_test_apps.sh 50
    ./scripts/generate_app_data.sh 2GB
    ;;
  extreme)
    ./scripts/install_test_apps.sh 100
    ./scripts/generate_app_data.sh 10GB
    ;;
esac

echo "Test data generation complete: $SIZE"
```

---

## Conclusion

This integration test plan covers all 170+ features implemented in ObsidianBackup. Execute tests systematically, document results thoroughly, and prioritize P0/P1 failures for immediate fixing.

**Next Steps:**
1. Set up test lab with required devices
2. Execute smoke tests daily
3. Run full test suite weekly
4. Address all P0/P1 failures before release
5. Automate regression tests in CI/CD

**Estimated Testing Effort:**
- Initial setup: 8 hours
- Daily smoke tests: 1-2 hours
- Weekly full tests: 8-12 hours
- Pre-release validation: 40-60 hours

**Total for first release:** ~100 hours testing

---

**Document Version:** 1.0.0  
**Last Updated:** 2026-02-09  
**Maintainer:** ObsidianBackup QA Team
