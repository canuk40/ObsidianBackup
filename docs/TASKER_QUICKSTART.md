# Tasker Integration Quick Start Guide

Get started with ObsidianBackup automation in 5 minutes.

## Prerequisites

- ObsidianBackup installed
- Tasker or MacroDroid installed
- Basic understanding of automation apps

---

## Quick Setup (Tasker)

### 1. Create Your First Backup Profile

**Profile Name:** "Daily Backup"

**Trigger:**
- **Event:** Time
- **Time:** 02:00 (2 AM)

**Task:** "Run Backup"
```
1. Send Intent
   - Action: com.obsidianbackup.tasker.ACTION_START_BACKUP
   - Target: Broadcast Receiver
   - Extra 1:
     - Name: package_list
     - Value: com.whatsapp,com.example.myapp
     - Type: String Array (select "Multiple" and comma-separate)
   - Extra 2:
     - Name: compression_level
     - Value: 9
     - Type: Int

2. Flash
   - Text: "Backup started!"
```

**Done!** Your backup will run every night at 2 AM.

---

### 2. Add Backup Complete Notification

**Profile Name:** "Backup Success"

**Trigger:**
- **Event:** Intent Received
- **Action:** `com.obsidianbackup.tasker.EVENT_BACKUP_COMPLETE`

**Task:** "Show Success"
```
1. Notify
   - Title: Backup Complete ✓
   - Text: Backed up %apps_backed_up apps
   - Icon: android:star_on
```

---

### 3. Add Failure Alert

**Profile Name:** "Backup Failed"

**Trigger:**
- **Event:** Intent Received
- **Action:** `com.obsidianbackup.tasker.EVENT_BACKUP_FAILED`

**Task:** "Show Error"
```
1. Notify
   - Title: Backup Failed ✗
   - Text: Error: %message
   - Priority: High
   - Sound: Alert
```

---

## Quick Setup (MacroDroid)

### 1. Daily Backup Macro

**Macro Name:** "Daily Backup"

**Trigger:**
- **Time Trigger:** 02:00 Daily

**Actions:**
1. **Send Intent**
   - Action: `com.obsidianbackup.tasker.ACTION_START_BACKUP`
   - Package: Broadcast
   - Add Extra:
     - Key: `package_list`
     - Value: `com.whatsapp,com.example.myapp`
     - Type: String Array
   - Add Extra:
     - Key: `compression_level`
     - Value: `9`
     - Type: Integer

2. **Toast:** "Backup started"

**Constraints:**
- Battery Level > 20%
- WiFi Connected

---

## Common Package Names

Apps you might want to backup:

```
com.whatsapp                    # WhatsApp
com.telegram.messenger          # Telegram
com.android.chrome              # Chrome
com.google.android.apps.photos  # Google Photos
com.spotify.music               # Spotify
com.netflix.mediaclient         # Netflix
com.facebook.katana             # Facebook
com.instagram.android           # Instagram
com.twitter.android             # Twitter
```

To find any app's package name:
1. Open Play Store
2. Find the app
3. Look at URL: `play.google.com/store/apps/details?id=PACKAGE_NAME`

---

## Test Your Setup

### Method 1: Run Task Manually

In Tasker:
1. Long-press your task
2. Tap "Play" icon
3. Check notification

### Method 2: Use Test Script

```bash
adb shell am broadcast \
  -a com.obsidianbackup.tasker.ACTION_START_BACKUP \
  --esa package_list "com.example.app" \
  --es calling_package "com.obsidianbackup"
```

### Method 3: Check Logs

```bash
adb logcat | grep TaskerIntegration
```

---

## Troubleshooting

### "Permission Denied" Error

**Solution:** Authorize Tasker in ObsidianBackup
1. Open ObsidianBackup
2. Go to Settings → Automation → Security
3. Enable "Authorize Tasker"

### Intent Not Received

**Solution:** Check target type
- Make sure "Target: Broadcast Receiver" is selected
- NOT "Activity" or "Service"

### No Response

**Solution:** Enable intent logging
1. Tasker → Preferences → Misc
2. Enable "Intents Logging"
3. Check Tasker log for broadcasts

---

## Next Steps

1. **Read Full Docs:** See `TASKER_INTEGRATION.md` for complete API
2. **Add Cloud Sync:** Trigger cloud uploads after backup
3. **Create Restore Profiles:** Auto-restore on new device
4. **Add Conditions:** Only backup when charging, on WiFi, etc.

---

## Example: Complete Automation

**Profile:** "Smart Backup System"

### Part 1: Nightly Backup
```
Trigger: Time 02:00 + Battery > 50% + WiFi Connected + Charging

Task:
1. Send Intent: Start Backup (all apps)
2. Variable Set: %backup_started = true
```

### Part 2: Backup Success Handler
```
Trigger: Intent Received (EVENT_BACKUP_COMPLETE)

Task:
1. Variable Set: %snapshot = %snapshot_id
2. Send Intent: Trigger Cloud Sync (snapshot: %snapshot)
3. Notify: "Backup complete, syncing to cloud..."
```

### Part 3: Cloud Sync Complete
```
Trigger: Intent Received (EVENT_SYNC_COMPLETE)

Task:
1. Notify: "Backup and sync successful!"
2. Variable Set: %last_backup = %TIMES
```

### Part 4: Any Failure
```
Trigger: Intent Received (EVENT_BACKUP_FAILED or EVENT_SYNC_FAILED)

Task:
1. Notify: "Backup failed: %message" (Priority: High)
2. Send SMS to backup phone (optional)
```

---

## Security Note

By default, only known automation apps can trigger backups:
- Tasker
- MacroDroid
- Automate
- AutoTools
- Join

To authorize other apps, go to:
**Settings → Automation → Security → Add Authorized App**

---

## Support

- **Full Documentation:** `TASKER_INTEGRATION.md`
- **Examples:** See documentation for 10+ example profiles
- **Issues:** GitHub Issues page
- **Email:** support@obsidianbackup.app

---

**Version:** 1.0.0  
**Last Updated:** February 2024  
**Estimated Setup Time:** 5 minutes
