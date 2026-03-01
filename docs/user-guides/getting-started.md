# Getting Started with ObsidianBackup

## Overview

ObsidianBackup is a comprehensive Android backup solution that allows you to backup apps, data, and system files securely with support for multiple cloud storage providers.

## Features

- **Complete Backup**: Backup apps, data, APKs, and system files
- **Cloud Integration**: Support for Google Drive, WebDAV, and Rclone
- **Incremental Backups**: Save time and storage with incremental backup strategy
- **Merkle Tree Verification**: Ensure data integrity with cryptographic verification
- **Biometric Security**: Secure your backups with fingerprint or face authentication
- **Automation**: Schedule automatic backups with flexible automation plugins
- **Split APK Support**: Full support for Android App Bundles and split APKs
- **Scoped Storage**: Compatible with modern Android storage requirements

## Requirements

- **Android Version**: Android 8.0 (API 26) or higher
- **Root Access**: Required for full system backups (optional for user data only)
- **Storage**: Sufficient local or cloud storage space
- **Permissions**: Storage, network, and optional root permissions

## Installation

### From APK

1. Download the latest APK from the releases page
2. Enable "Install from Unknown Sources" in your device settings
3. Install the APK
4. Grant necessary permissions when prompted

### From Source

See [Building from Source](../developer-guides/building.md) for detailed instructions.

## First Time Setup

### 1. Launch the App

Open ObsidianBackup from your app drawer.

### 2. Grant Permissions

The app will request several permissions:

- **Storage Access**: Required to read and write backup data
- **Network Access**: Required for cloud storage sync
- **Root Access** (optional): Required for full system backups

### 3. Configure Backup Location

Choose where to store your backups:

- **Local Storage**: Store backups on device or SD card
- **Cloud Storage**: Configure Google Drive, WebDAV, or Rclone

See [Cloud Storage Setup](cloud-storage.md) for detailed configuration.

### 4. Enable Biometric Security (Optional)

Protect your backups with biometric authentication:

1. Go to **Settings** → **Security**
2. Enable **Biometric Authentication**
3. Choose authentication type (Fingerprint, Face, or Passkey)

See [Biometric Security](biometric-security.md) for more details.

## Creating Your First Backup

### Manual Backup

1. Navigate to the **Apps** screen
2. Select the apps you want to backup
3. Tap **Backup Selected**
4. Choose backup options:
   - Include app data
   - Include APK
   - Include external data
5. Tap **Start Backup**
6. Monitor progress on the **Dashboard**

### Scheduled Backup

1. Navigate to **Automation** screen
2. Tap **Create Schedule**
3. Configure:
   - Backup frequency (daily, weekly, monthly)
   - Time of day
   - Apps to include
   - Backup destination
4. Enable the schedule
5. Backups will run automatically

See [Automation Guide](automation.md) for advanced automation features.

## Restoring Backups

### Restore Apps

1. Navigate to **Backups** screen
2. Select a backup set
3. Choose apps to restore
4. Tap **Restore Selected**
5. Grant any required permissions
6. Wait for restoration to complete

### Restore Options

- **App + Data**: Restore both application and its data
- **Data Only**: Restore only app data (app must be installed)
- **APK Only**: Install the app without data

## Next Steps

- [Features Overview](features-overview.md) - Learn about all available features
- [Backup Configuration](backup-configuration.md) - Configure advanced backup options
- [Cloud Storage Setup](cloud-storage.md) - Set up cloud storage providers
- [Automation Guide](automation.md) - Automate your backups
- [FAQ](faq.md) - Frequently asked questions
- [Troubleshooting](troubleshooting.md) - Common issues and solutions

## Getting Help

- Check the [FAQ](faq.md) for common questions
- Visit [Troubleshooting Guide](troubleshooting.md) for problem resolution
- Report issues on [GitHub Issues](https://github.com/obsidianbackup/issues)
- Join discussions on [GitHub Discussions](https://github.com/obsidianbackup/discussions)
