# Troubleshooting Guide

This guide helps you diagnose and resolve common issues with ObsidianBackup.

## Quick Diagnostics

### Check System Status

Run built-in diagnostics:
```
Settings → Diagnostics → Run System Check
```

This checks:
- Permissions
- Storage availability
- Root access (if configured)
- Cloud connectivity
- Backup integrity

## Common Issues

### Backup Issues

#### Backup Fails to Start

**Symptoms:**
- Backup button does nothing
- Backup immediately fails
- No progress shown

**Solutions:**

1. **Check Permissions**
   ```
   Settings → Permissions → Grant All
   ```
   Required permissions:
   - Storage (read/write)
   - Network (for cloud)
   - Root (for system backup)

2. **Check Storage Space**
   ```
   Dashboard → Storage → Available Space
   ```
   Ensure sufficient space (2x backup size recommended)

3. **Restart App**
   - Force stop app
   - Clear cache (Settings → Apps → ObsidianBackup → Clear Cache)
   - Restart app

4. **Check Logs**
   ```
   Settings → Logs → Recent Errors
   ```

#### Backup Extremely Slow

**Symptoms:**
- Backup takes hours
- Progress bar barely moves
- App becomes unresponsive

**Causes & Solutions:**

1. **Large Data Size**
   - Use incremental backup
   - Exclude cache and temporary files
   - Backup in smaller batches

2. **Slow Storage**
   - Use internal storage instead of SD card
   - Check SD card health
   - Consider USB 3.0 external drive

3. **High Compression**
   ```
   Settings → Backup → Compression → Fast or None
   ```

4. **Verification Overhead**
   ```
   Settings → Backup → Verification → Quick (or disable)
   ```

5. **Resource Contention**
   - Close other apps
   - Reduce thread count
   - Wait for background tasks to complete

#### Backup Completes but Files Missing

**Symptoms:**
- Backup shows success
- Some apps or files not included
- Backup size smaller than expected

**Solutions:**

1. **Check Filters**
   ```
   Settings → Backup → Filters
   ```
   Ensure apps aren't filtered out

2. **Check Permissions**
   - Some apps require specific permissions
   - Grant accessibility service if needed
   - Enable root for system apps

3. **Check Backup Scope**
   ```
   Settings → Backup → Scope → Apps + Data
   ```

4. **Review Backup Log**
   ```
   Backups → [Select Backup] → View Log
   ```
   Check for skipped items

#### Backup Corrupted

**Symptoms:**
- Cannot restore from backup
- Verification fails
- Error reading backup archive

**Solutions:**

1. **Verify Backup Integrity**
   ```
   Backups → [Select Backup] → Verify
   ```

2. **Check Merkle Tree**
   ```
   Backups → [Select Backup] → Merkle Verification
   ```

3. **Re-download from Cloud** (if applicable)
   ```
   Backups → [Select Backup] → Re-download
   ```

4. **Attempt Partial Recovery**
   ```
   Backups → [Select Backup] → Advanced → Extract What's Possible
   ```

5. **Create New Backup**
   - Previous backup likely corrupted during creation
   - Check storage health before new backup

### Restore Issues

#### Restore Fails

**Symptoms:**
- Restore button does nothing
- Restore fails with error
- App partially restores

**Solutions:**

1. **Check Available Storage**
   - Ensure 2x backup size available
   - Clear temporary files

2. **Grant Install Permissions**
   ```
   Settings → Apps → Special Access → Install Unknown Apps → ObsidianBackup → Allow
   ```

3. **Check Package Installer**
   - Ensure package installer not disabled
   - Update Play Store if needed

4. **Root Access** (for system apps)
   - Verify root access granted
   - Check Shizuku status if used

5. **Verify Backup First**
   ```
   Backups → [Select Backup] → Verify → Restore
   ```

#### App Installs but Data Not Restored

**Symptoms:**
- APK installs successfully
- App opens but no data
- Settings/preferences lost

**Solutions:**

1. **Check Restore Options**
   ```
   Restore → Options → Apps + Data (not Apps Only)
   ```

2. **Check App Permissions**
   - Grant runtime permissions to restored app
   - Some apps need permissions before data restore

3. **Restore Data Separately**
   ```
   Backups → [Select App] → Restore Data Only
   ```

4. **Check Root Access** (if needed)
   - Some data requires root
   - Grant root access and retry

#### Split APK Installation Fails

**Symptoms:**
- Base APK installs
- Split APKs fail
- App crashes on launch

**Solutions:**

1. **Verify Split Integrity**
   ```
   Backups → [Select App] → Verify Splits
   ```

2. **Manual Split Installation**
   - Extract backup
   - Install using SAI or Split APK Installer
   - Then restore data

3. **Check Android Version**
   - Ensure device supports split APKs (Android 5.0+)
   - Some devices have broken split support

4. **Use Bundle Restore**
   ```
   Settings → Restore → Use Bundle Mode
   ```

### Cloud Storage Issues

#### Cannot Connect to Cloud Provider

**Symptoms:**
- Authentication fails
- Connection timeout
- "Provider unavailable" error

**Solutions:**

**Google Drive:**
1. Check Google account credentials
2. Revoke access: Google Account → Security → Third-party apps
3. Reconnect in ObsidianBackup
4. Check Google Drive quota

**WebDAV:**
1. Verify server URL (include https://)
2. Check username and password
3. Test server accessibility in browser
4. Check firewall/network restrictions
5. Verify WebDAV is enabled on server

**Rclone:**
1. Check rclone configuration
2. Verify provider credentials
3. Update rclone binary
4. Test with rclone command line

#### Upload Fails or Stalls

**Symptoms:**
- Upload starts but never completes
- Progress stuck at percentage
- Upload fails after long time

**Solutions:**

1. **Check Network Connection**
   - Switch between Wi-Fi and mobile data
   - Test network speed
   - Check network stability

2. **Reduce Upload Size**
   - Split into smaller backups
   - Use compression
   - Upload incrementally

3. **Adjust Chunk Size**
   ```
   Settings → Cloud → Advanced → Chunk Size → 10 MB
   ```

4. **Check Provider Limits**
   - File size limits (Google Drive: 5 TB)
   - Bandwidth limits
   - Daily quota

5. **Enable Resume Support**
   ```
   Settings → Cloud → Resume Uploads
   ```

#### Download from Cloud Fails

**Symptoms:**
- Cannot download backup
- Download fails partway through
- "File not found" error

**Solutions:**

1. **Check File Exists**
   - Verify in cloud provider interface
   - Check file wasn't deleted

2. **Check Quota**
   - Ensure download quota available
   - Some providers limit downloads

3. **Manual Download**
   - Download via provider interface
   - Import into ObsidianBackup

4. **Re-upload from Local**
   - If local backup exists
   - Upload fresh copy

### Automation Issues

#### Scheduled Backup Doesn't Run

**Symptoms:**
- Scheduled time passes
- No backup created
- No notification

**Solutions:**

1. **Check Schedule Enabled**
   ```
   Automation → Schedules → [Select] → Enabled
   ```

2. **Check Conditions Met**
   - Battery level sufficient
   - Network available (if required)
   - Not in power-saving mode
   - Device not in use (if configured)

3. **Battery Optimization**
   ```
   Settings → Apps → ObsidianBackup → Battery → Don't Optimize
   ```

4. **Background Restrictions**
   ```
   Settings → Apps → ObsidianBackup → Mobile Data & Wi-Fi → No Restrictions
   ```

5. **Check WorkManager**
   ```
   Settings → Diagnostics → WorkManager Status
   ```

6. **Review Automation Logs**
   ```
   Automation → History → [Select Schedule]
   ```

#### Automation Plugin Not Working

**Symptoms:**
- Plugin installed but not running
- Plugin actions don't execute
- Plugin shows error

**Solutions:**

1. **Check Plugin Enabled**
   ```
   Automation → Plugins → [Select] → Enable
   ```

2. **Check Plugin Configuration**
   ```
   Automation → Plugins → [Select] → Settings
   ```

3. **Update Plugin**
   ```
   Automation → Plugins → [Select] → Check for Updates
   ```

4. **Reinstall Plugin**
   - Uninstall plugin
   - Restart app
   - Reinstall plugin

5. **Check Plugin Logs**
   ```
   Automation → Plugins → [Select] → Logs
   ```

6. **Plugin Compatibility**
   - Verify plugin compatible with app version
   - Check plugin requirements

### Security Issues

#### Biometric Authentication Fails

**Symptoms:**
- Fingerprint not recognized
- Face unlock fails
- Authentication timeout

**Solutions:**

1. **Re-enroll Biometric**
   - Device Settings → Security
   - Remove and re-add biometric

2. **Clean Sensor**
   - Wipe fingerprint sensor
   - Ensure good lighting for face

3. **Use Fallback**
   ```
   Authentication prompt → Use Password/PIN
   ```

4. **Check Lockout**
   - Wait for lockout period to expire
   - Too many failed attempts

5. **Disable and Re-enable**
   ```
   Settings → Security → Biometric Auth → Toggle off/on
   ```

#### Cannot Decrypt Backup

**Symptoms:**
- "Wrong password" error
- "Decryption failed" error
- Cannot open encrypted backup

**Solutions:**

1. **Verify Password**
   - Check Caps Lock
   - Try different keyboard
   - Verify password spelling

2. **Use Biometric** (if configured)
   ```
   Restore → Authenticate with Biometric
   ```

3. **Check Backup Integrity**
   ```
   Backups → [Select] → Verify
   ```

4. **Key Invalidated**
   - If biometric changed
   - If device security changed
   - May need original device

**Prevention:**
- Document passwords securely
- Use biometric with fallback
- Test restore before needed

#### Passkey Not Working

**Symptoms:**
- Cannot create passkey
- Passkey authentication fails
- Passkey not syncing

**Solutions:**

1. **Check Android Version**
   - Requires Android 14+
   - Update if possible

2. **Update Google Play Services**
   ```
   Play Store → Google Play Services → Update
   ```

3. **Enable Passkey Sync**
   ```
   Device Settings → Google → Passwords → Passkeys → Enable Sync
   ```

4. **Recreate Passkey**
   ```
   Settings → Security → Passkeys → Delete → Create New
   ```

### Performance Issues

#### App Slow or Laggy

**Symptoms:**
- UI sluggish
- Long loading times
- App hangs

**Solutions:**

1. **Clear Cache**
   ```
   Settings → Apps → ObsidianBackup → Storage → Clear Cache
   ```

2. **Reduce Active Backups**
   - Archive old backups
   - Delete unnecessary backups
   - Clean up metadata

3. **Check Device Resources**
   - Close other apps
   - Restart device
   - Check available RAM

4. **Update App**
   - Check for updates
   - Performance improvements in newer versions

5. **Reduce UI Animations**
   ```
   Settings → Display → Reduce Animations
   ```

#### High Battery Drain

**Symptoms:**
- App uses significant battery
- Battery drains quickly
- Device heats up

**Solutions:**

1. **Check Background Activity**
   ```
   Settings → Battery → App Usage
   ```

2. **Reduce Backup Frequency**
   - Less frequent schedules
   - Larger intervals
   - Manual backups only

3. **Optimize Settings**
   - Use incremental backups
   - Reduce compression
   - Disable continuous sync

4. **Schedule During Charging**
   ```
   Automation → [Schedule] → Conditions → Require Charging
   ```

5. **Check for Stuck Operations**
   ```
   Dashboard → Active Operations
   ```
   Cancel any stuck operations

#### High Storage Usage

**Symptoms:**
- App uses too much storage
- Storage fills up quickly
- "Storage full" warnings

**Solutions:**

1. **Check Storage Breakdown**
   ```
   Settings → Storage → Usage Details
   ```

2. **Configure Retention**
   ```
   Settings → Backup → Retention → Keep Last 7
   ```

3. **Delete Old Backups**
   ```
   Backups → Select → Delete
   ```

4. **Clear Temporary Files**
   ```
   Settings → Storage → Clear Temporary Files
   ```

5. **Move to Cloud**
   - Upload to cloud
   - Delete local copies
   - Download on demand

### Permissions Issues

#### "Permission Denied" Errors

**Symptoms:**
- Cannot access files
- Cannot backup certain apps
- Operations fail with permission error

**Solutions:**

1. **Grant All Permissions**
   ```
   Settings → Permissions → Grant All Required
   ```

2. **Enable Accessibility** (if needed)
   ```
   Device Settings → Accessibility → ObsidianBackup
   ```

3. **Storage Access Framework**
   ```
   Settings → Storage → Grant Access
   ```
   Select directories manually

4. **Root Access** (if available)
   ```
   Settings → Root → Grant SuperUser Access
   ```

5. **Shizuku** (root alternative)
   - Install Shizuku
   - Start Shizuku service
   - Grant in ObsidianBackup

#### SAF (Storage Access Framework) Issues

**Symptoms:**
- Cannot select folders
- "Access denied" for SD card
- Cannot write to external storage

**Solutions:**

1. **Re-grant Access**
   ```
   Settings → Storage → Revoke → Grant Again
   ```

2. **Use Different Storage**
   - Try internal storage
   - Use different SD card
   - Use cloud storage

3. **Check SD Card**
   - Ensure SD card mounted
   - Check SD card format (exFAT recommended)
   - Test SD card health

4. **Android 11+ Scoped Storage**
   ```
   Settings → Storage → Use Scoped Storage
   ```

## Advanced Troubleshooting

### Collecting Logs

1. Enable Debug Logging
   ```
   Settings → Advanced → Debug Logging → Enable
   ```

2. Reproduce Issue

3. Export Logs
   ```
   Settings → Logs → Export → Share
   ```

4. Disable Debug Logging
   ```
   Settings → Advanced → Debug Logging → Disable
   ```

### Database Issues

If app crashes on startup:

1. **Backup Database** (if accessible)
   ```bash
   adb pull /data/data/com.obsidianbackup/databases/backup.db
   ```

2. **Clear App Data** (last resort)
   ```
   Settings → Apps → ObsidianBackup → Storage → Clear Data
   ```
   **Warning:** This deletes all app configuration

3. **Reinstall App**
   - Uninstall app
   - Download latest version
   - Install fresh

### Network Diagnostics

Test network connectivity:

```
Settings → Diagnostics → Network Test
```

This tests:
- Internet connectivity
- DNS resolution
- Cloud provider reachability
- Bandwidth
- Latency

### Factory Reset Recovery

If all else fails:

1. **Backup Current State** (if possible)
2. **Note Configuration**
3. **Export Backup Metadata**
   ```
   Backups → Export Manifest
   ```
4. **Uninstall App**
5. **Reinstall Latest Version**
6. **Restore Configuration**
7. **Import Backups**

## Getting Help

### Before Asking for Help

Gather information:
- Device model and Android version
- App version
- Steps to reproduce
- Error messages
- Relevant logs

### Support Channels

1. **Documentation** - Check guides first
2. **FAQ** - [faq.md](faq.md)
3. **GitHub Issues** - Bug reports
4. **GitHub Discussions** - General help
5. **Email Support** - support@obsidianbackup.com

### Reporting Bugs

Include:
1. Issue description
2. Expected behavior
3. Actual behavior
4. Steps to reproduce
5. Logs (if applicable)
6. Device information
7. Screenshots (if relevant)

## Prevention

### Best Practices

1. **Regular Testing** - Test restores periodically
2. **Multiple Backups** - Don't rely on single backup
3. **Cloud Redundancy** - Use multiple providers
4. **Monitor Status** - Check dashboard regularly
5. **Keep Updated** - Update app and Android
6. **Document Setup** - Note configuration
7. **Verify Backups** - Enable verification
8. **Secure Storage** - Use encryption

### Monitoring

Set up monitoring:
```
Settings → Notifications → Alerts
```

Enable alerts for:
- Backup failures
- Storage low
- Sync issues
- Quota warnings

## Next Steps

- [FAQ](faq.md) - Frequently asked questions
- [Getting Started](getting-started.md) - Setup guide
- [GitHub Issues](https://github.com/obsidianbackup/issues) - Report bugs
