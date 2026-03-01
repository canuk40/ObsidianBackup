# Backup Configuration

This guide covers all backup configuration options in ObsidianBackup.

## Backup Settings

### General Settings

#### Backup Location
- **Local Storage**: Device internal storage or SD card
- **Cloud Storage**: Remote storage provider
- **Path**: Custom backup directory path

#### Backup Format
- **Archive Format**: TAR, ZIP, or custom
- **Compression**: None, GZIP, BZIP2, XZ
- **Encryption**: AES-256, disabled

#### Backup Scope
- **Apps Only**: Only application APKs
- **Data Only**: Only application data
- **Apps + Data**: Complete backup (recommended)
- **System**: Include system settings and data

### Advanced Settings

#### Incremental Backup
Enable incremental backups to only backup changed files:

```
Settings → Backup → Incremental Backup
```

**Options:**
- Enable incremental backups
- Merkle tree verification
- Change detection sensitivity
- Full backup frequency (after N incrementals)

#### Parallel Processing
Configure parallel backup operations:

```
Settings → Performance → Parallel Processing
```

**Options:**
- Thread count (2-8, default: 4)
- I/O buffer size
- Memory limit

#### Split APK Handling
Configure how split APKs are backed up:

```
Settings → Backup → Split APK
```

**Options:**
- Backup all splits (recommended)
- Base APK only
- Preserve split order
- Verify split integrity

## Backup Profiles

Create different backup profiles for different scenarios.

### Creating a Profile

1. Navigate to **Settings** → **Backup Profiles**
2. Tap **Create Profile**
3. Configure profile settings:
   - Name
   - Apps to include
   - Backup type (full/incremental)
   - Storage location
   - Encryption settings

### Profile Examples

#### Daily Essentials
- Selected critical apps
- Incremental backup
- Cloud storage
- Morning schedule

#### Full System Backup
- All apps and data
- Full backup
- Local storage
- Weekly schedule

#### Media Backup
- Photo and video apps
- Data only
- Cloud storage with high retention
- Daily schedule

## Backup Filters

Control what gets backed up with filters.

### App Filters

**Include:**
- System apps
- User apps
- Updated system apps
- Disabled apps

**Exclude:**
- Apps by name pattern
- Apps by package
- Apps by size threshold
- Apps without data

### Data Filters

**Include:**
- Internal data
- External data
- OBB files
- Cache data
- Shared preferences

**Exclude:**
- Temporary files
- Log files
- Files by pattern
- Large files (threshold)

## Compression Settings

### Compression Levels

#### None
- No compression
- Fastest backup speed
- Largest backup size
- Use for pre-compressed data

#### Fast (Level 1-3)
- Light compression
- Good balance of speed/size
- ~20-30% size reduction
- Recommended for daily backups

#### Normal (Level 4-6)
- Standard compression
- Good size reduction
- ~40-50% size reduction
- Default setting

#### Maximum (Level 7-9)
- Maximum compression
- Slowest backup speed
- ~50-60% size reduction
- Use for archival backups

### Compression Algorithms

#### GZIP
- Good balance of speed and compression
- Universal compatibility
- Default algorithm

#### BZIP2
- Better compression than GZIP
- Slower than GZIP
- Good for text-heavy data

#### XZ
- Best compression ratio
- Slowest algorithm
- Use for archival backups

## Encryption Settings

### Encryption Types

#### Password-based Encryption
1. Enable encryption in backup settings
2. Set a strong password
3. Confirm password
4. Password is required for restoration

**Best Practices:**
- Use 12+ character passwords
- Include mixed case, numbers, symbols
- Don't reuse passwords
- Store password securely

#### Biometric Encryption
1. Enable biometric authentication
2. Backups are encrypted with device key
3. Biometric required for restoration
4. Falls back to password if biometric unavailable

### Key Storage

**Options:**
- **Android Keystore**: Hardware-backed security
- **Encrypted Preferences**: Software encryption
- **Custom**: Provide your own key

## Retention Policies

Configure how long backups are kept.

### Local Storage Retention

```
Settings → Backup → Retention
```

**Options:**
- Keep all backups
- Keep last N backups (1-100)
- Keep backups for N days (1-365)
- Keep backups until space needed

### Cloud Storage Retention

**Options:**
- Sync all backups
- Keep last N backups in cloud
- Keep backups for N days
- Auto-cleanup old backups

## Verification Settings

### Integrity Verification

**Post-Backup Verification:**
- CRC32 checksums
- SHA-256 hashes
- Merkle tree verification
- Archive integrity test

**Restore Verification:**
- Verify before restore
- Verify after restore
- Skip verification (not recommended)

### Backup Validation

**Automatic Validation:**
- Validate after creation
- Validate on schedule
- Validate before upload

**Validation Options:**
- Quick validation (metadata only)
- Full validation (all files)
- Smart validation (sample files)

## Network Settings

### Upload Settings

**Bandwidth Control:**
- Unlimited
- Limit to N MB/s
- Adaptive (based on network type)

**Network Type:**
- Any network
- Wi-Fi only
- Wi-Fi or Ethernet
- Unmetered networks only

**Retry Settings:**
- Retry count (0-10)
- Retry delay
- Exponential backoff

### Download Settings

**Restore Bandwidth:**
- Unlimited (default)
- Limit to N MB/s

**Caching:**
- Cache downloaded backups
- Cache size limit
- Auto-cleanup cache

## Battery Optimization

### Backup Timing

**Battery Considerations:**
- Require minimum battery level (10-90%)
- Require charging
- Ignore battery optimization (not recommended)

**Power-Saving Mode:**
- Defer backups when in power-saving
- Use reduced performance
- Continue anyway

## Notifications

### Backup Notifications

**Progress Notifications:**
- Show ongoing progress
- Show percentage complete
- Show ETA
- Sound and vibration

**Completion Notifications:**
- Success notification
- Failure notification with error
- Summary statistics
- Quick actions (view, restore)

### Quiet Hours

Configure when to suppress notifications:
- Start time
- End time
- Days of week
- Emergency notifications only

## Troubleshooting Configuration Issues

### Backup Fails to Start

**Check:**
- Storage permissions granted
- Sufficient storage space
- Network connectivity (for cloud)
- Battery level and charging status

### Slow Backup Performance

**Optimize:**
- Reduce compression level
- Disable verification
- Increase thread count
- Use incremental backup

### High Storage Usage

**Reduce:**
- Enable compression
- Configure retention policies
- Use incremental backups
- Clean up old backups

## Configuration Best Practices

1. **Start Simple**: Use default settings initially
2. **Test Restores**: Verify backups can be restored
3. **Monitor Performance**: Adjust settings based on performance
4. **Regular Maintenance**: Review and clean up old backups
5. **Document Changes**: Note configuration changes
6. **Security First**: Always enable encryption for sensitive data

## Next Steps

- [Cloud Storage Setup](cloud-storage.md) - Configure cloud providers
- [Automation Guide](automation.md) - Automate backups
- [Troubleshooting](troubleshooting.md) - Common issues
