# Wear OS Quick Start Guide

## Overview
This guide helps you quickly get started with the ObsidianBackup Wear OS companion app.

## Prerequisites
- ✅ Wear OS watch (version 3.0+)
- ✅ Phone with ObsidianBackup installed
- ✅ Watch paired with phone via Wear OS app
- ✅ Google Play Services on both devices

## Installation

### Option 1: From Google Play (Recommended)
1. Open Google Play Store on your watch
2. Search for "ObsidianBackup"
3. Install the Wear OS version
4. App will auto-sync with phone

### Option 2: Side-load via ADB
```bash
# Build the Wear OS APK
cd /root/workspace/ObsidianBackup
./gradlew :wear:assembleDebug

# Install to watch (via USB debugging or WiFi ADB)
adb -e install wear/build/outputs/apk/debug/wear-debug.apk
```

## First Launch

1. **Open the app** on your watch
2. **Check connection status** - Should show "Connected" if phone app is running
3. **Grant permissions** if prompted
4. **Test backup trigger** - Tap "Start Backup"

## Adding to Watch Face

### Add Complication
1. **Long-press** on your watch face
2. **Tap "Customize"** or the gear icon
3. **Select a complication slot**
4. **Scroll to "ObsidianBackup"**
5. **Choose complication type**:
   - Short Text: Shows status (OK/Failed)
   - Long Text: Shows last backup time
   - Ranged Value: Shows progress percentage
   - Small Image: Shows status icon
6. **Tap to confirm**

### Add Tile
1. **Swipe left** from your watch face
2. **Swipe to the end** and tap "+"
3. **Find "Backup" tile**
4. **Tap to add**
5. **Swipe to use**

## Usage

### Trigger Backup

**From Tile:**
- Swipe to Backup tile
- Tap "Backup Now"
- Wait for confirmation vibration

**From App:**
- Open ObsidianBackup on watch
- Tap "Start Backup" button
- View real-time progress

**From Complication:**
- Tap the complication
- Opens main app
- Tap "Start Backup"

### View Status
1. Open app or tap complication
2. Swipe up to see "View Status"
3. Shows:
   - Last backup time
   - Success/failure status
   - Total backups count
   - Backup size

### Monitor Progress
During an active backup:
1. Open app
2. Navigate to progress screen
3. See:
   - Percentage complete
   - Current file
   - Files processed
   - Time remaining (estimated)

### Cancel Backup
1. During backup, open app
2. Tap "Cancel Backup"
3. Confirm cancellation

## Troubleshooting

### Watch Not Connecting
**Symptom:** "Phone not connected" message

**Solutions:**
1. Ensure phone app is running
2. Check Bluetooth connection in Wear OS app
3. Restart both devices
4. Re-pair watch in Wear OS app on phone

### Complication Not Updating
**Symptom:** Shows old data

**Solutions:**
1. Trigger a backup to force refresh
2. Restart watch app
3. Remove and re-add complication
4. Check Google Play Services

### Tile Not Appearing
**Symptom:** Can't find Backup tile

**Solutions:**
1. Verify watch is Wear OS 3.0+
2. Reinstall watch app
3. Clear app data
4. Check with different watch face

### Status Not Syncing
**Symptom:** Different status on phone vs watch

**Solutions:**
1. Open phone app to trigger sync
2. Pull to refresh on watch
3. Check Data Layer connection
4. Verify Google Play Services

## Features

### Quick Actions
- ✅ Start backup from watch
- ✅ Cancel running backup
- ✅ View current status
- ✅ Monitor real-time progress

### Complications
- ✅ 4 different styles
- ✅ Auto-updates every 5 minutes
- ✅ Tap to open full app
- ✅ Battery efficient

### Tiles
- ✅ One-tap backup trigger
- ✅ Status at a glance
- ✅ No navigation required

### Notifications
- ✅ Backup started
- ✅ Backup completed
- ✅ Errors/warnings
- ✅ Low battery alerts

## Settings

Currently synced from phone app:
- Auto-backup enabled/disabled
- Cloud sync settings
- Notification preferences
- Haptic feedback toggle

**To change:** Update in phone app settings

## Performance Tips

### Battery Optimization
- Close app when not actively monitoring
- Use complication instead of keeping app open
- Disable haptic feedback if not needed
- Reduce notification frequency

### Best Practices
- Keep watch near phone during backup
- Charge watch before long backups
- Use WiFi for faster sync
- Close other apps during backup

## Limitations

- **Requires phone proximity** - Bluetooth/WiFi range
- **No offline backups** - Phone must be accessible
- **Google Play Services** - Required for Data Layer
- **Battery impact** - ~3-5% per backup monitored
- **Wear OS 3.0+** - Older versions not supported

## FAQ

**Q: Can I backup directly from watch without phone?**
A: No, the watch app is a companion to the phone app. All backup operations run on the phone.

**Q: How often does the complication update?**
A: Every 5 minutes when idle, real-time during active backup.

**Q: Does this drain my watch battery?**
A: Minimal impact when idle (<1%/hour), moderate during active monitoring (3-5%/hour).

**Q: Can I use multiple watches with one phone?**
A: Yes, all paired watches will receive updates.

**Q: What if my phone isn't nearby?**
A: The watch will show "Phone not connected" and features will be unavailable until phone is in range.

**Q: Can I schedule backups from watch?**
A: Not yet, scheduling is done through phone app settings.

## Support

For issues or questions:
1. Check phone app connection first
2. Review troubleshooting section above
3. Check logcat: `adb logcat -s WearApp`
4. Report issues with "Wear OS" label

## Keyboard Shortcuts

On supported watches with physical buttons:

- **Top button** - Open app
- **Bottom button** - Start backup (when in app)
- **Rotate crown** - Scroll through options
- **Press crown** - Select/confirm

## Advanced

### Developer Options
Enable USB debugging to:
- Install custom builds
- View detailed logs
- Test beta features

### ADB Commands
```bash
# Install APK
adb -e install wear-debug.apk

# View logs
adb logcat -s WearApp

# Clear app data
adb shell pm clear com.obsidianbackup.wear

# Force stop
adb shell am force-stop com.obsidianbackup.wear
```

## What's Next?

Upcoming features:
- ⏳ Offline history cache
- ⏳ Voice commands
- ⏳ Custom watch face
- ⏳ Restore preview
- ⏳ Analytics dashboard

## Version Info

**Current Version:** 1.0.0
**Release Date:** 2024
**Min Wear OS:** 3.0 (API 30)
**Target Wear OS:** 5.0 (API 33)

---

**Tip:** Add the Backup tile to your first tile slot for quickest access! 🚀
