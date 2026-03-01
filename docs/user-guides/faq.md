# Frequently Asked Questions (FAQ)

## General Questions

### What is ObsidianBackup?

ObsidianBackup is a comprehensive Android backup solution that allows you to backup apps, data, and system files with support for multiple cloud storage providers, biometric security, and automated scheduling.

### Is root access required?

Root access is optional:
- **Without root**: Backup user apps and their data (requires app permissions)
- **With root**: Full system backup including system apps and protected data

### What Android versions are supported?

- **Minimum**: Android 8.0 (API 26)
- **Recommended**: Android 11+ for best compatibility
- **Target**: Android 14 (API 35)

### Is ObsidianBackup free?

Yes, with optional Pro features:
- **Free**: Core backup/restore, basic scheduling, cloud storage
- **Pro**: Advanced automation, custom plugins, priority sync, extended retention

### Is my data secure?

Yes, ObsidianBackup uses:
- AES-256 encryption for backups
- Biometric authentication protection
- Hardware-backed key storage (Android Keystore)
- End-to-end encryption for cloud storage
- No telemetry or data collection

## Backup Questions

### How long does a backup take?

Depends on:
- Data size
- Device performance
- Backup type (full vs incremental)
- Storage destination (local vs cloud)

**Typical times:**
- Small apps (< 100 MB): 30 seconds - 2 minutes
- Medium apps (100 MB - 1 GB): 2-10 minutes
- Large apps (> 1 GB): 10-60 minutes
- Full system: 30 minutes - 2 hours

### What gets backed up?

**User Apps:**
- APK files (including split APKs)
- Application data
- Shared preferences
- Databases
- Internal files
- External data (if accessible)

**System (with root):**
- System apps
- System settings
- SMS/MMS
- Call logs
- Contacts
- Wi-Fi passwords

### Can I backup to multiple locations?

Yes! Configure multiple backup destinations:
- Local storage + cloud
- Multiple cloud providers
- Primary and secondary destinations
- Automatic failover

### How much storage do I need?

Depends on apps and data:
- **Small setup** (essential apps): 1-5 GB
- **Medium setup** (typical user): 5-20 GB
- **Large setup** (power user): 20-100 GB
- **Full system**: 50-200 GB

**Optimization:**
- Use incremental backups (70-90% reduction)
- Enable compression (30-50% reduction)
- Configure retention policies
- Exclude cache and temporary files

### Can I backup app data only (without APK)?

Yes! Configure backup scope:
- Apps only
- Data only
- Apps + data (default)

### What about split APKs?

Fully supported! ObsidianBackup:
- Detects all APK splits automatically
- Preserves split structure
- Maintains installation order
- Validates split integrity

## Restore Questions

### How do I restore a backup?

1. Navigate to **Backups** screen
2. Select backup set
3. Choose apps to restore
4. Tap **Restore Selected**
5. Grant permissions if prompted
6. Wait for completion

### Can I restore to a different device?

Yes, if:
- Device has same or higher Android version
- Device architecture is compatible (ARM/x86)
- Apps are compatible with device
- Sufficient storage available

**Note:** System-specific data may not be compatible across devices.

### Do I need to uninstall apps before restoring?

No, but options:
- **Overwrite**: Restore over existing installation
- **Data only**: Restore data to installed app
- **Fresh install**: Uninstall first, then restore

### Can I restore individual files?

Yes! Options:
- Extract backup archive
- Browse backup contents
- Restore specific files or folders
- Use file manager integration

### What if restore fails?

**Troubleshooting steps:**
1. Check available storage space
2. Verify backup integrity
3. Grant necessary permissions
4. Check app compatibility
5. Review error logs
6. Try restoring to different location
7. Contact support with logs

## Cloud Storage Questions

### Which cloud providers are supported?

**Direct integration:**
- Google Drive
- WebDAV (Nextcloud, ownCloud, etc.)

**Via Rclone:**
- Amazon S3
- Dropbox
- Microsoft OneDrive
- Backblaze B2
- 40+ more providers

### Is cloud storage required?

No, optional:
- Backup to local storage only
- Backup to SD card
- Backup to external USB drive
- Cloud backup is optional

### How much cloud storage do I need?

Same as local storage requirements:
- Small setup: 1-5 GB
- Medium setup: 5-20 GB
- Large setup: 20-100 GB

**Most cloud providers offer:**
- Google Drive: 15 GB free
- Dropbox: 2 GB free
- OneDrive: 5 GB free
- Paid plans: 100 GB - unlimited

### Are backups encrypted before upload?

Yes! Options:
- Client-side encryption (before upload)
- AES-256 encryption
- Password or biometric-protected keys
- Zero-knowledge architecture

### What happens if cloud upload fails?

**Automatic handling:**
- Retry with exponential backoff
- Queue for later upload
- Notification of failure
- Keep local backup copy

**Manual handling:**
- View failed uploads
- Retry manually
- Change destination
- Review error logs

## Automation Questions

### How do I schedule automatic backups?

1. Navigate to **Automation** screen
2. Tap **Create Schedule**
3. Configure frequency, time, and conditions
4. Select apps to backup
5. Enable schedule

### Can I create multiple schedules?

Yes! Create unlimited schedules:
- Different apps
- Different times
- Different destinations
- Different conditions

### What if my device is off during scheduled backup?

**Options:**
- Run as soon as device turns on (default)
- Skip missed backup
- Run at next scheduled time

### Can I trigger backups from other apps?

Yes! Use deep linking:
```
obsidianbackup://backup?apps=com.example.app
```

Or Intent-based integration:
```kotlin
val intent = Intent("com.obsidianbackup.BACKUP")
intent.putExtra("apps", arrayOf("com.example.app"))
startActivity(intent)
```

### How do I create custom automation?

Options:
1. Use built-in automation plugins
2. Develop custom plugin (see [Plugin Development Guide](../developer-guides/plugin-development.md))
3. Use external automation apps (Tasker, Automate)
4. Deep linking from scripts

## Security Questions

### How secure is biometric authentication?

Very secure:
- Hardware-backed biometric sensors
- Android BiometricPrompt API (industry standard)
- Keys stored in secure hardware (TEE/StrongBox)
- No biometric data in backups
- Fallback to device credentials

### Can someone access my backups without authentication?

No, if encryption and biometric auth enabled:
- Backups encrypted with AES-256
- Keys protected by biometric
- Cannot decrypt without authentication
- Even with physical device access

### What if I forget my backup password?

**Without biometric:**
- No password recovery possible
- Backups cannot be decrypted
- Prevention: Use biometric auth or document password securely

**With biometric:**
- Use biometric to access
- Fallback to device credential
- Can change password

### Is my data sent to ObsidianBackup servers?

No! ObsidianBackup:
- No cloud servers
- No data collection
- No telemetry
- Direct device-to-cloud
- Zero-knowledge architecture

### How do I verify backup integrity?

Multiple methods:
1. Merkle tree verification
2. SHA-256 checksums
3. Archive integrity test
4. Test restore
5. Visual verification interface

## Performance Questions

### Why is backup slow?

**Possible causes:**
- Large data size
- Slow storage (SD card)
- High compression level
- Verification enabled
- Cloud upload bandwidth

**Optimization:**
- Use incremental backup
- Reduce compression level
- Disable verification (not recommended)
- Use local storage first
- Upgrade storage device

### Why is restore slow?

**Possible causes:**
- Large backup size
- Cloud download speed
- Device performance
- Verification enabled

**Optimization:**
- Pre-download from cloud
- Use local backup
- Reduce verification
- Close other apps

### Does backup drain battery?

**Battery usage:**
- Background backup: Low (optimized)
- Active backup: Medium
- Cloud upload: Medium to High
- Charging recommended for large backups

**Battery optimization:**
- Schedule during charging
- Use battery-aware plugin
- Reduce backup frequency
- Incremental backups

### Why does app use so much storage?

**Storage usage:**
- Backup archives
- Temporary files
- Cache data
- Metadata

**Cleanup:**
- Configure retention policies
- Delete old backups
- Clear cache
- Review storage usage in settings

## Compatibility Questions

### Does it work with App Bundles?

Yes! Full support for:
- Split APKs
- Android App Bundles
- Dynamic feature modules
- Language packs

### What about Magisk modules?

**With root:**
- Can backup Magisk modules
- Backup module configuration
- Restore on compatible device

**Note:** Module compatibility depends on device and Android version

### Can I backup system apps?

**With root:** Yes
**Without root:** Only updated system apps

### Does it work with work profile?

Yes! Support for:
- Work profile apps
- Personal and work separation
- Managed device policies
- Enterprise features

## Troubleshooting Questions

### Backup fails immediately

**Check:**
1. Storage permissions granted
2. Sufficient storage space
3. Root access (if required)
4. No conflicting apps running
5. Check logs for specific error

### App crashes on startup

**Solutions:**
1. Clear app cache
2. Reinstall app
3. Check Android version compatibility
4. Disable biometric temporarily
5. Review crash logs

### Cloud sync not working

**Check:**
1. Network connectivity
2. Cloud provider credentials
3. Storage quota
4. Provider service status
5. Retry configuration

### Cannot restore specific app

**Possible causes:**
- App not compatible with device
- Insufficient permissions
- Corrupted backup
- Android version mismatch

**Solutions:**
1. Verify backup integrity
2. Check app compatibility
3. Try data-only restore
4. Contact support

## Getting More Help

### Where can I find more information?

- [User Guides](getting-started.md)
- [Troubleshooting Guide](troubleshooting.md)
- [Developer Guides](../developer-guides/architecture.md)
- [GitHub Discussions](https://github.com/obsidianbackup/discussions)

### How do I report a bug?

1. Check [existing issues](https://github.com/obsidianbackup/issues)
2. Gather information:
   - Device model and Android version
   - App version
   - Steps to reproduce
   - Error logs
3. Create new issue with details

### How do I request a feature?

1. Check [existing requests](https://github.com/obsidianbackup/discussions)
2. Create discussion with:
   - Feature description
   - Use case
   - Expected behavior
3. Community votes on features

### Where can I get support?

- [GitHub Discussions](https://github.com/obsidianbackup/discussions) - Community support
- [GitHub Issues](https://github.com/obsidianbackup/issues) - Bug reports
- [Documentation](README.md) - Guides and references
- Email: support@obsidianbackup.com

## Contributing

### How can I contribute?

Multiple ways:
- Report bugs
- Submit feature requests
- Improve documentation
- Translate to other languages
- Develop plugins
- Submit code contributions

See [Contributing Guidelines](../developer-guides/contributing.md) for details.

### Can I develop plugins?

Yes! See [Plugin Development Guide](../developer-guides/plugin-development.md).

### How do I translate the app?

1. Fork repository
2. Add translations in `res/values-<lang>`
3. Test translations
4. Submit pull request

See [Contributing Guidelines](../developer-guides/contributing.md).
