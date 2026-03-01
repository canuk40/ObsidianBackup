# Tasker/MacroDroid Integration for ObsidianBackup

Complete automation API documentation with security and example profiles.

## Table of Contents
- [Overview](#overview)
- [Security](#security)
- [Intent Actions](#intent-actions)
- [Events](#events)
- [ContentProvider](#contentprovider)
- [Tasker Examples](#tasker-examples)
- [MacroDroid Examples](#macrodroid-examples)
- [Troubleshooting](#troubleshooting)

## Overview

ObsidianBackup provides a comprehensive automation API for Tasker, MacroDroid, and other automation apps. You can trigger backups, restore snapshots, query status, and react to backup events.

### Features
- ✅ Start backup operations with custom parameters
- ✅ Restore specific snapshots
- ✅ Query operation status
- ✅ Trigger cloud sync
- ✅ Verify backup integrity
- ✅ Cancel running operations
- ✅ Delete snapshots
- ✅ Event-based triggers (backup complete, failed, etc.)
- ✅ ContentProvider for synchronous queries
- ✅ Security validation of calling packages

---

## Security

### Package Authorization

ObsidianBackup validates all automation requests to prevent unauthorized access. Signature verification uses **full SHA-256 certificate fingerprints** (updated 2026-02-21 — Tasker fingerprint verified against installed APK on physical device).

#### Known Verified Signatures

| App | Package | SHA-256 Fingerprint |
|-----|---------|---------------------|
| Tasker | `net.dinglisch.android.taskerm` | `973fe25b9be28fb7436d49582b04277767c852539be31783d134a55621b6636d` |

> **Note:** Other apps in the whitelist (MacroDroid, Automate, etc.) are allowed by package name only until their fingerprints are added. To add a fingerprint, pull the APK via `adb shell pm path <package>`, then run `~/Android/Sdk/build-tools/35.0.0/apksigner verify --print-certs <apk>` and add the SHA-256 to `KNOWN_SIGNATURES` in `TaskerSecurityValidator.kt`.

#### Authorized Automation Apps (by default):
- Tasker (`net.dinglisch.android.taskerm`)
- MacroDroid (`com.arlosoft.macrodroid`)
- Automate (`com.llamalab.automate`)
- AutoTools (`com.joaomgcd.autotools`)
- AutoApps (`com.joaomgcd.autoapps`)
- Join (`com.joaomgcd.join`)

#### Enable Additional Apps

To authorize additional automation apps:

1. Open ObsidianBackup Settings
2. Navigate to **Automation → Security**
3. Tap **Add Authorized Package**
4. Enter the package name
5. Confirm authorization

#### Security Settings

```kotlin
// Example: Configure security programmatically
securityValidator.authorizePackage("com.myapp.automation")
securityValidator.setRequireSignatureVerification(true)
```

**⚠️ Important:** Never disable signature verification in production!

---

## Intent Actions

### 1. Start Backup

Trigger a backup operation with configurable parameters.

**Action:** `com.obsidianbackup.tasker.ACTION_START_BACKUP`

**Required Extras:**
- `package_list` (String[]): Array of package names to backup

**Optional Extras:**
- `backup_components` (String[]): Components to backup (default: `["APK", "DATA"]`)
  - Valid values: `APK`, `DATA`, `OBB`, `EXTERNAL_DATA`
- `compression_level` (Int): Compression level 0-9 (default: 6)
- `incremental` (Boolean): Enable incremental backup (default: false)
- `encryption_enabled` (Boolean): Encrypt backup (default: false)
- `description` (String): Backup description

**Response Intent:** `com.obsidianbackup.tasker.ACTION_START_BACKUP_RESPONSE`

**Response Extras:**
- `work_request_id` (String): Work ID for status tracking
- `status` (String): "started" or "error"
- `message` (String): Status message
- `result_code` (Int): Result code (0=success)

**Example (Tasker):**
```
Action: Send Intent
  Action: com.obsidianbackup.tasker.ACTION_START_BACKUP
  Extra: package_list:com.example.app1,com.example.app2 (String Array)
  Extra: backup_components:APK,DATA (String Array)
  Extra: compression_level:9 (Int)
  Extra: incremental:true (Boolean)
  Extra: description:Scheduled backup (String)
  Target: Broadcast Receiver
```

---

### 2. Restore Snapshot

Restore apps from a backup snapshot.

**Action:** `com.obsidianbackup.tasker.ACTION_RESTORE_SNAPSHOT`

**Required Extras:**
- `snapshot_id` (String): Snapshot ID to restore

**Optional Extras:**
- `package_list` (String[]): Specific packages to restore (default: all)
- `backup_components` (String[]): Components to restore (default: `["APK", "DATA"]`)

**Response Intent:** `com.obsidianbackup.tasker.ACTION_RESTORE_SNAPSHOT_RESPONSE`

**Response Extras:**
- `work_request_id` (String): Work ID for status tracking
- `status` (String): "started" or "error"
- `message` (String): Status message
- `result_code` (Int): Result code (0=success)

**Example (Tasker):**
```
Action: Send Intent
  Action: com.obsidianbackup.tasker.ACTION_RESTORE_SNAPSHOT
  Extra: snapshot_id:backup_20240215_123045 (String)
  Extra: package_list:com.example.app1 (String Array)
  Target: Broadcast Receiver
```

---

### 3. Query Status

Query the status of a running operation.

**Action:** `com.obsidianbackup.tasker.ACTION_QUERY_STATUS`

**Required Extras:**
- `work_request_id` (String): Work ID from start backup/restore response

**Response Intent:** `com.obsidianbackup.tasker.ACTION_QUERY_STATUS_RESPONSE`

**Response Extras:**
- `work_request_id` (String): Work ID
- `status` (String): Work state (`ENQUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `CANCELLED`)
- `progress` (Int): Progress percentage (0-100)
- `result_code` (Int): Result code

**Example (Tasker):**
```
Action: Send Intent
  Action: com.obsidianbackup.tasker.ACTION_QUERY_STATUS
  Extra: work_request_id:%work_id (String)
  Target: Broadcast Receiver
```

---

### 4. Trigger Cloud Sync

Upload a backup to cloud storage.

**Action:** `com.obsidianbackup.tasker.ACTION_TRIGGER_CLOUD_SYNC`

**Optional Extras:**
- `cloud_provider` (String): Cloud provider (default: "default")
  - Valid values: `default`, `gdrive`, `dropbox`, `webdav`, `rclone`
- `snapshot_id` (String): Specific snapshot to sync (default: latest)

**Response Intent:** `com.obsidianbackup.tasker.ACTION_TRIGGER_CLOUD_SYNC_RESPONSE`

**Example (Tasker):**
```
Action: Send Intent
  Action: com.obsidianbackup.tasker.ACTION_TRIGGER_CLOUD_SYNC
  Extra: cloud_provider:gdrive (String)
  Extra: snapshot_id:backup_20240215_123045 (String)
  Target: Broadcast Receiver
```

---

### 5. Verify Backup

Verify integrity of a backup snapshot.

**Action:** `com.obsidianbackup.tasker.ACTION_VERIFY_BACKUP`

**Required Extras:**
- `snapshot_id` (String): Snapshot ID to verify

**Response Intent:** `com.obsidianbackup.tasker.ACTION_VERIFY_BACKUP_RESPONSE`

**Example (Tasker):**
```
Action: Send Intent
  Action: com.obsidianbackup.tasker.ACTION_VERIFY_BACKUP
  Extra: snapshot_id:backup_20240215_123045 (String)
  Target: Broadcast Receiver
```

---

### 6. Cancel Operation

Cancel a running backup/restore operation.

**Action:** `com.obsidianbackup.tasker.ACTION_CANCEL_OPERATION`

**Required Extras:**
- `work_request_id` (String): Work ID to cancel

**Response Intent:** `com.obsidianbackup.tasker.ACTION_CANCEL_OPERATION_RESPONSE`

---

### 7. Delete Snapshot

Delete a backup snapshot.

**Action:** `com.obsidianbackup.tasker.ACTION_DELETE_SNAPSHOT`

**Required Extras:**
- `snapshot_id` (String): Snapshot ID to delete

**Response Intent:** `com.obsidianbackup.tasker.ACTION_DELETE_SNAPSHOT_RESPONSE`

---

## Events

Events are broadcast by ObsidianBackup when operations complete. Use these as triggers in Tasker/MacroDroid.

### Backup Complete

**Action:** `com.obsidianbackup.tasker.EVENT_BACKUP_COMPLETE`

**Extras:**
- `snapshot_id` (String): Created snapshot ID
- `snapshot_timestamp` (Long): Backup timestamp (milliseconds)
- `apps_backed_up` (Int): Number of apps backed up
- `total_size` (Long): Total backup size in bytes
- `duration` (Long): Duration in milliseconds
- `result_code` (Int): 0=success, 1=partial success
- `status` (String): "success" or "partial_success"
- `package_list` (String[]): Backed up packages

### Backup Failed

**Action:** `com.obsidianbackup.tasker.EVENT_BACKUP_FAILED`

**Extras:**
- `apps_failed` (Int): Number of failed apps
- `result_code` (Int): 2=failure
- `status` (String): "failed"
- `message` (String): Error message
- `failed_packages` (String[]): Failed packages

### Restore Complete

**Action:** `com.obsidianbackup.tasker.EVENT_RESTORE_COMPLETE`

**Extras:**
- `snapshot_id` (String): Restored snapshot ID
- `apps_backed_up` (Int): Number of apps restored
- `duration` (Long): Duration in milliseconds
- `result_code` (Int): 0=success, 1=partial success
- `status` (String): "success" or "partial_success"
- `package_list` (String[]): Restored packages

### Restore Failed

**Action:** `com.obsidianbackup.tasker.EVENT_RESTORE_FAILED`

**Extras:**
- `snapshot_id` (String): Snapshot ID
- `result_code` (Int): 2=failure
- `status` (String): "failed"
- `message` (String): Error message

### Cloud Sync Complete

**Action:** `com.obsidianbackup.tasker.EVENT_SYNC_COMPLETE`

**Extras:**
- `snapshot_id` (String): Synced snapshot ID (optional)
- `total_size` (Long): Bytes uploaded
- `duration` (Long): Duration in milliseconds
- `result_code` (Int): 0=success
- `status` (String): "success"

### Cloud Sync Failed

**Action:** `com.obsidianbackup.tasker.EVENT_SYNC_FAILED`

**Extras:**
- `snapshot_id` (String): Snapshot ID (optional)
- `result_code` (Int): 2=failure
- `status` (String): "failed"
- `message` (String): Error message

---

## ContentProvider

For synchronous queries (without waiting for broadcast responses), use the ContentProvider.

**Authority:** `com.obsidianbackup.tasker`

### Query Work Status

**URI:** `content://com.obsidianbackup.tasker/status/{work_id}`

**Columns:**
- `work_id` (String): Work request ID
- `state` (String): Work state
- `progress` (Int): Progress (0-100)
- `output_data` (String): Output data
- `run_attempt_count` (Int): Retry attempts
- `tags` (String): Work tags

**Example (Tasker):**
```
Action: Content Provider Query
  URI: content://com.obsidianbackup.tasker/status/%work_id
  Variable Array: %status
```

### Query Backups

**URI:** `content://com.obsidianbackup.tasker/backups`

**Columns:**
- `snapshot_id` (String): Snapshot ID
- `timestamp` (Long): Backup timestamp
- `app_count` (Int): Number of apps
- `total_size` (Long): Total size
- `description` (String): Description
- `status` (String): Status

### Query Latest Backup

**URI:** `content://com.obsidianbackup.tasker/latest`

Returns the most recent successful backup.

---

## Tasker Examples

### Example 1: Nightly Backup

**Profile:**
- **Trigger:** Time (3:00 AM)
- **Conditions:** 
  - Battery Level > 30%
  - WiFi Connected
  - Device Charging

**Task: "Nightly Backup"**
```
1. Variable Set
   Name: %packages
   To: com.whatsapp,com.telegram.messenger,com.example.app

2. Send Intent
   Action: com.obsidianbackup.tasker.ACTION_START_BACKUP
   Extra: package_list:%packages (String Array - split by comma)
   Extra: backup_components:APK,DATA (String Array)
   Extra: compression_level:9 (Int)
   Extra: incremental:true (Boolean)
   Extra: description:Nightly automated backup (String)
   Target: Broadcast Receiver

3. Wait
   Seconds: 5

4. Flash
   Text: Backup started
```

---

### Example 2: Pre-System Update Backup

**Profile:**
- **Event:** Intent Received
  - Action: `android.intent.action.BOOT_COMPLETED`

**Task: "Pre-Update Backup"**
```
1. Flash
   Text: Running pre-update backup...

2. Send Intent
   Action: com.obsidianbackup.tasker.ACTION_START_BACKUP
   Extra: package_list:* (backup all apps)
   Extra: description:Pre-system-update backup (String)
   Target: Broadcast Receiver
```

---

### Example 3: Backup Complete Notification

**Profile:**
- **Event:** Intent Received
  - Action: `com.obsidianbackup.tasker.EVENT_BACKUP_COMPLETE`

**Task: "Backup Success Notification"**
```
1. Variable Set
   Name: %apps_count
   To: %apps_backed_up (from intent extra)

2. Variable Set
   Name: %size_mb
   To: %total_size / 1048576

3. Notify
   Title: Backup Complete ✓
   Text: Backed up %apps_count apps (%size_mb MB)
   Icon: android:star_on
```

---

### Example 4: Backup Failed Alert

**Profile:**
- **Event:** Intent Received
  - Action: `com.obsidianbackup.tasker.EVENT_BACKUP_FAILED`

**Task: "Backup Failed Alert"**
```
1. Variable Set
   Name: %error
   To: %message (from intent extra)

2. Notify
   Title: Backup Failed ✗
   Text: Error: %error
   Priority: High
   Sound: Alert

3. Send SMS (optional)
   Number: YOUR_NUMBER
   Message: ObsidianBackup failed: %error
```

---

### Example 5: Weekly Cloud Sync

**Profile:**
- **Day:** Sunday
- **Time:** 4:00 AM
- **Conditions:**
  - WiFi Connected (SSID: Home)
  - Battery Level > 50%

**Task: "Weekly Cloud Sync"**
```
1. Send Intent
   Action: com.obsidianbackup.tasker.ACTION_TRIGGER_CLOUD_SYNC
   Extra: cloud_provider:gdrive (String)
   Target: Broadcast Receiver

2. Flash
   Text: Cloud sync started
```

---

### Example 6: Query Backup Status

**Task: "Check Backup Status"**
```
1. Variable Set
   Name: %work_id
   To: [work ID from previous backup]

2. Content Provider Query
   URI: content://com.obsidianbackup.tasker/status/%work_id
   Variable Array: %status

3. If %status(2) ~ SUCCEEDED
   Then:
     Flash: Backup completed successfully
   Else:
     Flash: Backup status: %status(2)
```

---

### Example 7: Conditional Restore

**Profile:**
- **Event:** Device Boot

**Task: "Restore After Factory Reset"**
```
1. Flash
   Text: Checking for restore...

2. Content Provider Query
   URI: content://com.obsidianbackup.tasker/latest
   Variable Array: %latest_backup

3. If %latest_backup1 !~ ""
   Then:
     4. Send Intent
        Action: com.obsidianbackup.tasker.ACTION_RESTORE_SNAPSHOT
        Extra: snapshot_id:%latest_backup1 (String)
        Target: Broadcast Receiver
     
     5. Notify
        Title: Restore Started
        Text: Restoring from %latest_backup1
```

---

## MacroDroid Examples

### Example 1: Daily Backup (MacroDroid)

**Trigger:**
- **Time Trigger:** Daily at 2:00 AM

**Actions:**
1. **Send Intent**
   - Action: `com.obsidianbackup.tasker.ACTION_START_BACKUP`
   - Package: Broadcast
   - Extras:
     - Key: `package_list` | Value: `com.whatsapp,com.telegram.messenger` | Type: String Array
     - Key: `compression_level` | Value: `9` | Type: Integer
     - Key: `description` | Value: `Daily MacroDroid backup` | Type: String

2. **Toast Message:** "Backup started"

**Constraints:**
- Battery Level > 20%
- WiFi Connected

---

### Example 2: Backup on App Install (MacroDroid)

**Trigger:**
- **Application Installed:** Any App

**Actions:**
1. **Wait:** 30 seconds

2. **Send Intent**
   - Action: `com.obsidianbackup.tasker.ACTION_START_BACKUP`
   - Package: Broadcast
   - Extras:
     - Key: `package_list` | Value: `%installed_package%` | Type: String Array
     - Key: `description` | Value: `Backup after install` | Type: String

---

### Example 3: Backup Complete Notification (MacroDroid)

**Trigger:**
- **Intent Received:** `com.obsidianbackup.tasker.EVENT_BACKUP_COMPLETE`

**Actions:**
1. **Notification**
   - Title: "Backup Complete ✓"
   - Text: "Snapshot: {snapshot_id}"

2. **Log to File:** "Backup completed at {time}"

---

## Troubleshooting

### Permission Denied Error

**Problem:** Receiving `RESULT_PERMISSION_DENIED` (code 4)

**Solutions:**
1. Check if your automation app is in the authorized list:
   - Settings → Automation → Security → Authorized Apps
2. Verify package signature verification is not blocking:
   - Temporarily disable signature verification for testing
3. Enable debug mode (development only):
   ```kotlin
   securityValidator.setDebugAllowAll(true)
   ```

### Intent Not Received

**Problem:** Broadcast receiver not receiving intents

**Solutions:**
1. Verify the intent action is correct (case-sensitive)
2. Use "Broadcast Receiver" as target in Tasker, not "Activity"
3. Check Android logs:
   ```bash
   adb logcat | grep TaskerIntegration
   ```

### Work Status Always Returns NULL

**Problem:** ContentProvider queries return no data

**Solutions:**
1. Verify the work_request_id is valid
2. Check if ObsidianBackup has permission to access WorkManager
3. Ensure the work hasn't been pruned (WorkManager prunes old work)

### Events Not Triggering

**Problem:** Event broadcasts not received by Tasker

**Solutions:**
1. Enable event logging in Tasker:
   - Preferences → Misc → Intents Logging
2. Verify intent action in profile exactly matches event action
3. Check if Android is restricting background broadcasts (Android 8+)

### Invalid Parameters Error

**Problem:** Receiving `RESULT_INVALID_PARAMS` (code 3)

**Solutions:**
1. Verify all required extras are provided
2. Check data types match (String Array vs String)
3. Validate package names are correct
4. Ensure compression level is between 0-9

---

## Advanced Usage

### Chaining Operations

Run multiple operations in sequence:

**Task: "Full Backup Pipeline"**
```
1. Send Intent: Start Backup
   [Store work_id in %backup_work_id]

2. Wait Until: Query status returns SUCCEEDED
   Loop:
     Content Provider Query: status/%backup_work_id
     If %status ~ SUCCEEDED: Exit Loop
     Wait 10 seconds

3. Send Intent: Trigger Cloud Sync
   Extra: snapshot_id from backup response

4. Flash: Backup and sync complete
```

### Error Handling

Robust error handling with retries:

**Task: "Backup with Retry"**
```
1. Variable Set: %retry_count = 0

2. Loop Until: %retry_count > 3
   a. Send Intent: Start Backup
   b. Wait 60 seconds
   c. Query Status
   d. If Status = SUCCEEDED: Stop
   e. Variable Add: %retry_count + 1
   f. Flash: Retry %retry_count/3

3. If %retry_count > 3
   Then: Notify: Backup failed after 3 retries
```

---

## API Reference Summary

### Result Codes
- `0` - `RESULT_SUCCESS`
- `1` - `RESULT_PARTIAL_SUCCESS`
- `2` - `RESULT_FAILURE`
- `3` - `RESULT_INVALID_PARAMS`
- `4` - `RESULT_PERMISSION_DENIED`
- `5` - `RESULT_IN_PROGRESS`

### Work States
- `ENQUEUED` - Work is queued
- `RUNNING` - Work is executing
- `SUCCEEDED` - Work completed successfully
- `FAILED` - Work failed
- `CANCELLED` - Work was cancelled
- `BLOCKED` - Work is blocked by constraints

### Component Types
- `APK` - Application package
- `DATA` - App data directory
- `OBB` - OBB files (large game data)
- `EXTERNAL_DATA` - External storage data

---

## Support

For issues or questions:
- GitHub: [ObsidianBackup Issues](https://github.com/obsidianbackup/issues)
- Documentation: [Full API Docs](https://obsidianbackup.app/docs)
- Email: support@obsidianbackup.app

---

**Version:** 1.0.0  
**Last Updated:** February 2024  
**Compatible With:** Tasker 6.0+, MacroDroid 5.0+, Automate 1.30+
