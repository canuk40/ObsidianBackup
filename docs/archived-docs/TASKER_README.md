# Tasker/MacroDroid Automation Integration

## 🤖 Complete Automation API

ObsidianBackup provides full automation support for Tasker, MacroDroid, and other automation apps, allowing you to trigger backups, restore snapshots, and react to backup events programmatically.

---

## 📚 Quick Links

- **[Complete API Documentation](TASKER_INTEGRATION.md)** - Full reference with 10+ examples
- **[Quick Start Guide](TASKER_QUICKSTART.md)** - Get started in 5 minutes
- **[Implementation Summary](TASKER_IMPLEMENTATION_SUMMARY.md)** - Technical details
- **[Files Manifest](TASKER_FILES_MANIFEST.md)** - Complete file listing

---

## ✨ Features

### 🎯 Actions (7)
Trigger operations from your automation app:
- **Start Backup** - Backup apps with custom parameters (compression, incremental, encryption)
- **Restore Snapshot** - Restore from any backup snapshot
- **Query Status** - Check operation progress in real-time
- **Cloud Sync** - Upload backups to cloud storage
- **Verify Backup** - Check backup integrity
- **Cancel Operation** - Stop running operations
- **Delete Snapshot** - Remove old backups

### 📢 Events (6)
React to backup operations:
- **Backup Complete** - Trigger actions when backup finishes
- **Backup Failed** - Handle backup errors
- **Restore Complete** - Act on successful restore
- **Restore Failed** - Handle restore errors
- **Cloud Sync Complete** - React to cloud uploads
- **Cloud Sync Failed** - Handle sync errors

### 🔍 Status Queries (4 URIs)
Query backup status synchronously:
- Query specific work status
- List all backups
- Get latest backup info
- Query snapshots

### 🔒 Security
- Package authorization whitelist
- Optional signature verification
- User-configurable authorized apps
- Built-in support for Tasker, MacroDroid, Automate, AutoTools

---

## 🚀 Quick Start

### Tasker Example: Daily Backup

**Profile:** Time (2:00 AM)

**Task:**
```
1. Send Intent
   - Action: com.obsidianbackup.tasker.ACTION_START_BACKUP
   - Extra: package_list = com.whatsapp,com.telegram.messenger
   - Extra: compression_level = 9 (Int)
   - Target: Broadcast Receiver

2. Flash: "Backup started"
```

### MacroDroid Example: Backup on WiFi

**Trigger:** WiFi Connected (Home)

**Action:** Send Intent
- Action: `com.obsidianbackup.tasker.ACTION_START_BACKUP`
- Package: Broadcast
- Extra: `package_list` = `com.myapp` (String Array)

---

## 📖 Example Use Cases

### 1. **Nightly Automated Backups**
Schedule backups to run every night when device is charging and connected to WiFi.

### 2. **Pre-Update Protection**
Automatically backup apps before system updates or major app updates.

### 3. **Cloud Sync on WiFi**
Upload backups to cloud storage when connected to home WiFi network.

### 4. **Conditional Restore**
Automatically restore apps on new device after factory reset.

### 5. **Backup Verification**
Periodically verify backup integrity and get notified of corruption.

### 6. **Smart Notifications**
Get detailed notifications with backup size, duration, and app count.

### 7. **Error Handling**
Send SMS alerts when backups fail or retry automatically.

---

## 🔧 Integration Architecture

### Components

1. **TaskerIntegration.kt**
   - BroadcastReceiver for handling automation intents
   - Worker-based execution (BackupWorker, RestoreWorker, etc.)
   - Security validation on all requests

2. **TaskerStatusProvider.kt**
   - ContentProvider for synchronous status queries
   - Returns structured cursor data
   - 4 query URIs for different data

3. **TaskerEventPublisher.kt**
   - Broadcasts events to automation apps
   - 6 event types for different outcomes
   - Progress updates for live tracking

4. **TaskerSecurityValidator.kt**
   - Package authorization system
   - Whitelist of known automation apps
   - Optional signature verification

5. **Plugin Actions**
   - BackupActionRunner
   - RestoreActionRunner
   - CloudSyncActionRunner
   - BackupStatusConditionRunner

---

## 🔐 Security Configuration

### Authorized Apps (Default)
- Tasker
- MacroDroid
- Automate
- AutoTools / AutoApps
- Join

### Add Custom App
1. Open Settings → Automation → Security
2. Tap "Add Authorized Package"
3. Enter package name (e.g., `com.myapp`)
4. Confirm

### Enable Debug Mode (Testing Only)
```kotlin
securityValidator.setDebugAllowAll(true)
```
⚠️ **Never enable in production builds!**

---

## 📋 API Reference Summary

### Intent Actions
```
com.obsidianbackup.tasker.ACTION_START_BACKUP
com.obsidianbackup.tasker.ACTION_RESTORE_SNAPSHOT
com.obsidianbackup.tasker.ACTION_QUERY_STATUS
com.obsidianbackup.tasker.ACTION_TRIGGER_CLOUD_SYNC
com.obsidianbackup.tasker.ACTION_VERIFY_BACKUP
com.obsidianbackup.tasker.ACTION_CANCEL_OPERATION
com.obsidianbackup.tasker.ACTION_DELETE_SNAPSHOT
```

### Events
```
com.obsidianbackup.tasker.EVENT_BACKUP_COMPLETE
com.obsidianbackup.tasker.EVENT_BACKUP_FAILED
com.obsidianbackup.tasker.EVENT_RESTORE_COMPLETE
com.obsidianbackup.tasker.EVENT_RESTORE_FAILED
com.obsidianbackup.tasker.EVENT_SYNC_COMPLETE
com.obsidianbackup.tasker.EVENT_SYNC_FAILED
```

### ContentProvider
```
content://com.obsidianbackup.tasker/status/{work_id}
content://com.obsidianbackup.tasker/backups
content://com.obsidianbackup.tasker/latest
content://com.obsidianbackup.tasker/snapshots
```

---

## 🧪 Testing

### Test Script
```bash
# Run automated tests
adb shell < test_tasker_integration.sh

# Monitor logs
adb logcat | grep TaskerIntegration
```

### Manual Testing
1. Create a simple Tasker profile
2. Add "Send Intent" action with backup intent
3. Run task manually
4. Check for response broadcasts
5. Verify backup was created

---

## 📝 Example Profiles

### Profile 1: Weekly Cloud Backup
```
Trigger: Sunday 3:00 AM + WiFi + Charging

Task:
1. Start Backup (all apps)
2. Wait for EVENT_BACKUP_COMPLETE
3. Trigger Cloud Sync
4. Notify on completion
```

### Profile 2: App Install Auto-Backup
```
Trigger: App Installed

Task:
1. Wait 30 seconds
2. Start Backup (installed app)
3. Notify: "Backed up %app_name%"
```

### Profile 3: Smart Restore
```
Trigger: Device Boot

Task:
1. Query latest backup
2. If exists and < 7 days old:
   - Restore all apps
   - Notify progress
```

See **[TASKER_INTEGRATION.md](TASKER_INTEGRATION.md)** for 10+ detailed examples.

---

## 🛠️ Troubleshooting

### Common Issues

**Problem:** Permission Denied (code 4)
- **Solution:** Authorize your automation app in Settings → Automation → Security

**Problem:** Intent not received
- **Solution:** Use "Broadcast Receiver" as target, not "Activity"

**Problem:** No response from queries
- **Solution:** Check work_request_id is valid and work hasn't been pruned

**Problem:** Events not triggering
- **Solution:** Enable intent logging in Tasker, verify action string matches exactly

See full troubleshooting guide in [TASKER_INTEGRATION.md](TASKER_INTEGRATION.md).

---

## 📦 What's Included

### Implementation Files (5)
- TaskerIntegration.kt (524 lines)
- TaskerStatusProvider.kt (260 lines)
- TaskerEventPublisher.kt (247 lines)
- TaskerPluginActions.kt (219 lines)
- TaskerSecurityValidator.kt (260 lines)

### Documentation (4 files)
- Complete API documentation (17.9 KB)
- Quick start guide (5.2 KB)
- Implementation summary (9.4 KB)
- Files manifest (7.7 KB)

### Test Scripts (1)
- Automated testing script (1.6 KB)

**Total:** 1,510 lines of code, 1,330+ lines of documentation

---

## 🎓 Learning Resources

1. **[Tasker Documentation](https://tasker.joaoapps.com/userguide/en/)**
2. **[MacroDroid Documentation](https://www.macrodroid.com/)**
3. **[Android Intents Guide](https://developer.android.com/guide/components/intents-filters)**
4. **[WorkManager Documentation](https://developer.android.com/topic/libraries/architecture/workmanager)**

---

## 🤝 Compatibility

- **Tasker:** 6.0+
- **MacroDroid:** 5.0+
- **Automate:** 1.30+
- **Android:** 8.0+ (API 26+)

---

## 📞 Support

- **Full Documentation:** [TASKER_INTEGRATION.md](TASKER_INTEGRATION.md)
- **Quick Start:** [TASKER_QUICKSTART.md](TASKER_QUICKSTART.md)
- **GitHub Issues:** Report bugs and request features
- **Email:** support@obsidianbackup.app

---

## 🎯 Next Steps

1. Read the [Quick Start Guide](TASKER_QUICKSTART.md)
2. Try the example profiles from [TASKER_INTEGRATION.md](TASKER_INTEGRATION.md)
3. Create your first automation profile
4. Join the community and share your profiles!

---

**Version:** 1.0.0  
**Status:** Production Ready  
**Last Updated:** February 2024

