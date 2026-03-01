# Features Overview

ObsidianBackup provides a comprehensive set of features for Android backup and restoration.

## Core Features

### 1. Complete Backup Solution

#### App Backup
- Backup installed applications (APK files)
- Support for split APKs and App Bundles
- Preserve app signatures and metadata

#### Data Backup
- Application data and preferences
- Internal storage data
- External storage data (SD card)
- Cache data (optional)

#### System Backup
- System settings
- SMS and call logs
- Contacts and calendar
- Wi-Fi passwords and configurations

### 2. Multiple Backup Strategies

#### Full Backup
- Complete backup of all selected data
- Creates a new backup set every time
- Best for initial backups or major changes

#### Incremental Backup
- Only backs up changed files since last backup
- Significantly faster than full backups
- Reduces storage requirements
- Uses Merkle Tree for change detection

#### Differential Backup
- Backs up changes since last full backup
- Balance between full and incremental
- Easier restoration than incremental

### 3. Cloud Storage Integration

#### Google Drive
- Seamless integration with Google Drive API
- Automatic authentication with Google account
- Folder organization and management
- Bandwidth optimization

#### WebDAV
- Connect to any WebDAV-compatible server
- Nextcloud, ownCloud, and other self-hosted solutions
- Custom endpoint configuration
- SSL/TLS support

#### Rclone Integration
- Support for 40+ cloud storage providers
- Amazon S3, Dropbox, OneDrive, and more
- Advanced configuration options
- Encryption support

### 4. Data Integrity and Security

#### Merkle Tree Verification
- Cryptographic verification of backup integrity
- Detect file corruption or tampering
- Efficient incremental backup strategy
- Visual verification interface

#### Biometric Authentication
- Fingerprint authentication
- Face recognition (Android 10+)
- Passkey support (Android 14+)
- Protect sensitive backups

#### Encryption
- AES-256 encryption for backup data
- Password-protected backups
- Secure key storage
- End-to-end encryption for cloud storage

### 5. Automation and Scheduling

#### Scheduled Backups
- Daily, weekly, monthly schedules
- Custom time selection
- Battery and network conditions
- App selection profiles

#### Automation Plugins
- Extensible plugin architecture
- Custom automation rules
- Event-based triggers
- Third-party plugin support

#### Plugin Types
- **Time-based**: Schedule by date and time
- **Event-based**: Trigger on system events
- **Condition-based**: Execute based on conditions
- **Custom**: Develop your own plugins

### 6. Advanced Features

#### Deep Linking
- Launch backup operations from external apps
- URL scheme: `obsidianbackup://`
- Intent-based integration
- Automated workflows

#### Parallel Processing
- Multi-threaded backup and restore
- Improved performance on multi-core devices
- Configurable thread count
- Resource optimization

#### Split APK Support
- Full support for Android App Bundles
- Automatic split APK detection
- Preserve all APK splits
- Correct restoration order

#### Scoped Storage Compatibility
- Android 11+ scoped storage support
- Media Store integration
- SAF (Storage Access Framework)
- No legacy storage dependencies

### 7. User Interface

#### Material Design 3
- Modern, clean interface
- Dark mode support
- Adaptive layouts for tablets
- Smooth animations

#### Dashboard
- Real-time backup status
- Storage usage statistics
- Recent backup history
- Quick actions

#### App Management
- Sort and filter applications
- Batch operations
- App information display
- Search functionality

### 8. Enterprise Features

#### Pro Features
- Advanced automation
- Priority cloud sync
- Extended retention policies
- Advanced scheduling

#### Billing Integration
- In-app purchases
- Subscription management
- Feature gates
- Trial periods

### 9. Cross-Platform Support

#### Android Mobile
- Phone and tablet support
- Adaptive layouts
- Touch-optimized interface

#### Android TV
- TV-optimized interface
- Remote control navigation
- Large screen layouts

#### Wear OS
- Basic backup control
- Backup status monitoring
- Quick actions

## Feature Comparison

| Feature | Free | Pro |
|---------|------|-----|
| Basic Backup/Restore | ✓ | ✓ |
| Local Storage | ✓ | ✓ |
| Cloud Storage | ✓ | ✓ |
| Incremental Backup | ✓ | ✓ |
| Biometric Security | ✓ | ✓ |
| Basic Scheduling | ✓ | ✓ |
| Advanced Automation | - | ✓ |
| Custom Plugins | - | ✓ |
| Priority Sync | - | ✓ |
| Extended Retention | - | ✓ |
| Batch Operations | Limited | ✓ |
| Multiple Profiles | 1 | Unlimited |

## Performance Characteristics

### Backup Speed
- **Full Backup**: ~50-100 MB/s (device dependent)
- **Incremental**: ~200-500 MB/s (only changed files)
- **Cloud Upload**: Network bandwidth dependent

### Storage Efficiency
- **Compression**: 30-50% reduction typical
- **Incremental**: 70-90% reduction after first backup
- **Deduplication**: Additional 10-20% savings

### Resource Usage
- **Memory**: 50-150 MB during operation
- **CPU**: Multi-threaded, optimized for efficiency
- **Battery**: Optimized for low power consumption

## Upcoming Features

See our [roadmap](https://github.com/obsidianbackup/roadmap) for planned features.

## Next Steps

- [Backup Configuration](backup-configuration.md) - Configure backup settings
- [Cloud Storage Setup](cloud-storage.md) - Set up cloud providers
- [Automation Guide](automation.md) - Automate your backups
- [Biometric Security](biometric-security.md) - Secure your backups
