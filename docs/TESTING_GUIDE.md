# Testing Guide: Backup Implementation

## Quick Start Testing

### Prerequisites
1. Install the APK: `app/build/outputs/apk/free/debug/app-free-universal-debug.apk`
2. Grant necessary permissions (Storage, Accessibility if needed)
3. Device should have at least 2-3 user apps installed

### Test Scenario 1: Dashboard → Apps Flow
**Expected Time**: 1 minute

1. **Launch app** → Dashboard screen appears
2. **Verify dashboard**:
   - "Backup Apps" button is visible
   - "View Backups" button is visible
   - Permission mode chip shows in top bar (ROOT/SHIZUKU/ADB/SAF)
3. **Click "Backup Apps" button**
   - ✅ Should navigate to Apps screen (NOT just announce)
   - ✅ Apps screen shows list of installed apps
4. **Back to Dashboard** → Click "Backup Apps" again
   - ✅ Should navigate back to Apps screen

### Test Scenario 2: Select and Backup Single App
**Expected Time**: 30 seconds - 2 minutes (depends on app size)

1. **Navigate to Apps screen** (via Dashboard or bottom nav)
2. **Verify app list**:
   - Apps are displayed with name, package, version
   - Each app shows data size and APK size
   - Checkboxes are visible on the right
3. **Select one small app** (e.g., Calculator):
   - Tap anywhere on the row OR tap checkbox
   - ✅ Checkbox becomes checked
   - ✅ Floating Action Button (FAB) appears: "Backup 1 apps"
4. **Click FAB**:
   - ✅ Dialog appears: "Backup 1 apps"
   - ✅ Shows current permission mode
   - ✅ Lists components to backup (APK, Data, etc.)
5. **Click "Backup Now"**:
   - ✅ Dialog closes
   - ✅ Progress dialog appears: "Backing up apps..."
   - ✅ Shows circular progress indicator
   - ✅ Shows "Processing 1 apps"
6. **Wait for completion** (10-60 seconds):
   - ✅ Progress dialog disappears
   - ✅ Result dialog appears with green checkmark icon
   - ✅ Shows "Backup Completed"
   - ✅ Shows "1 apps backed up"
   - ✅ Shows total size in MB
   - ✅ Shows duration in seconds
7. **Click "OK"**:
   - ✅ Returns to Apps screen
   - ✅ Selected apps are deselected
   - ✅ FAB is hidden

### Test Scenario 3: Backup Multiple Apps
**Expected Time**: 1-5 minutes (depends on app sizes)

1. **Navigate to Apps screen**
2. **Select 3-5 apps**:
   - Tap each app to select
   - ✅ FAB updates: "Backup 3 apps", "Backup 4 apps", etc.
   - ✅ Counter at top shows "3 of 50 selected"
3. **Click FAB → "Backup Now"**:
   - ✅ Progress dialog shows
   - ✅ May take 1-5 minutes depending on app sizes
4. **Expected results**:
   - **If all succeed**: "Backup Completed" with green icon
   - **If some fail**: "Backup Issue" with red icon
     - Shows "X apps backed up"
     - Shows "Y apps failed"
     - Lists error messages for failed apps

### Test Scenario 4: Deselect Apps
**Expected Time**: 10 seconds

1. **Select 2 apps** → FAB shows "Backup 2 apps"
2. **Tap one selected app again**:
   - ✅ Checkbox unchecks
   - ✅ FAB updates to "Backup 1 apps"
3. **Tap remaining selected app**:
   - ✅ Checkbox unchecks
   - ✅ FAB disappears (no apps selected)

### Test Scenario 5: Permission Mode Testing
**Expected Time**: 2-3 minutes

Test backup functionality in different permission modes:

#### SAF Mode (Limited)
- May only backup APKs, not data
- Result may show partial success

#### ROOT Mode (Full)
- Backups APK + Data + OBB + External
- Should show complete success

#### SHIZUKU Mode (Full with Shizuku)
- Similar to ROOT but requires Shizuku service
- Should show complete success

#### ADB Mode (Via Wireless ADB)
- Similar to SHIZUKU
- May be slower due to network overhead

### Test Scenario 6: Error Handling
**Expected Time**: Variable

#### Test: Backup with insufficient storage
1. Ensure device has < 100MB free space
2. Try to backup large apps (games)
3. ✅ Should show error dialog with "Insufficient storage" message

#### Test: Backup without permissions
1. Revoke storage permissions in Settings
2. Try to backup apps
3. ✅ Should show error dialog with permission error

#### Test: Backup system apps (if allowed)
1. Enable "Show system apps" (if available)
2. Try to backup system apps
3. ✅ May fail with permission errors (expected)

## Verification Checklist

### UI Components
- ✅ Dashboard "Backup Apps" button navigates to Apps screen
- ✅ Apps screen shows app list with checkboxes
- ✅ FAB appears/disappears based on selection
- ✅ FAB shows correct count
- ✅ Backup dialog shows permission mode and components
- ✅ Progress dialog appears during backup
- ✅ Result dialog shows success/failure/partial with details
- ✅ Dialogs are accessible (TalkBack compatible)

### Functional Tests
- ✅ Single app backup works
- ✅ Multiple app backup works
- ✅ Selection/deselection works
- ✅ Progress tracking works
- ✅ Error handling works
- ✅ Different permission modes work
- ✅ Backup metadata is saved
- ✅ Backup files are created on disk

### File System Verification
After successful backup, check:
```bash
adb shell ls -la /data/data/com.obsidianbackup/files/backups/
# OR
adb shell ls -la /sdcard/Android/data/com.obsidianbackup/files/backups/
```

Expected structure:
```
backups/
  <uuid>/
    metadata.json
    com.example.app/
      apk/
        base.apk
        split_*.apk (if split APK)
      data.tar.zst
      obb.tar.zst (if OBB exists)
      external.tar.zst (if external exists)
```

### Database Verification
Check if backup metadata is in Room database:
```bash
adb shell run-as com.obsidianbackup cat databases/backup_database
# OR use Database Inspector in Android Studio
```

## Performance Benchmarks

| Apps | Total Size | Expected Time (ROOT) | Expected Time (SAF) |
|------|-----------|---------------------|---------------------|
| 1 small (5MB) | 5MB | 10-15s | 20-30s |
| 1 medium (50MB) | 50MB | 30-60s | 1-2min |
| 1 large (500MB) | 500MB | 2-5min | 5-10min |
| 5 small | 25MB | 30-60s | 1-2min |
| 5 medium | 250MB | 2-5min | 5-10min |

## Known Issues / Expected Behavior

### ✅ Expected
1. **Progress not granular**: Shows total count, not per-app progress
2. **Cannot cancel**: Progress dialog is non-dismissible during backup
3. **No background**: Backup stops if app is killed
4. **SAF limitations**: May only backup APKs in SAF mode

### ❌ Bugs to Report
If you encounter:
- App crashes during backup → Report with logs
- Backup succeeds but files not created → Report with device info
- Progress dialog stuck → Report with app selection
- Navigation broken → Report with steps to reproduce

## Advanced Testing

### Test with ADB
```bash
# Monitor logs during backup
adb logcat -s ObsidianBackup:* ObsidianBoxEngine:* BackupOrchestrator:*

# Check shell commands being executed
adb logcat -s ShellExecutor:*

# Monitor file creation
adb shell watch ls -la /data/data/com.obsidianbackup/files/backups/
```

### Test with Different App Types
1. **Small utility apps**: Calculator, Clock (fast backup)
2. **Medium apps**: Browsers, Email apps (moderate backup)
3. **Large games**: Games with OBB files (slow backup, tests all components)
4. **Split APK apps**: Modern apps from Play Store (tests split APK handling)

## Reporting Issues

If you find issues, please provide:
1. **Device info**: Model, Android version, root status
2. **Permission mode**: ROOT/SHIZUKU/ADB/SAF
3. **Apps selected**: List of apps you tried to backup
4. **Steps to reproduce**: Exact sequence of actions
5. **Logs**: `adb logcat` output during the issue
6. **Screenshots**: Of error dialogs or unexpected behavior

## Success Criteria

Implementation is considered successful if:
✅ Dashboard button navigates to Apps screen
✅ Apps can be selected/deselected
✅ FAB triggers actual backup operation
✅ Progress dialog appears during backup
✅ Result dialog shows success/failure with details
✅ Backup files are created on disk
✅ Metadata is saved to database
✅ No crashes during normal operation
✅ Works in at least one permission mode (ROOT or SAF)

---

**Status**: Ready for testing! 🚀
**Build**: `app-free-universal-debug.apk` (97MB)
**Last Updated**: Current build
