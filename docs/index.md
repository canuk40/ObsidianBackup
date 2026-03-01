# ObsidianBackup Documentation

Welcome to ObsidianBackup - a comprehensive Android backup solution.

[Get Started](user-guides/getting-started.md){ .md-button .md-button--primary }
[View on GitHub](https://github.com/obsidianbackup/ObsidianBackup){ .md-button }

## Features

<div class="grid cards" markdown>

-   :material-backup-restore:{ .lg .middle } __Complete Backup Solution__

    ---

    Backup apps, data, and system files with support for multiple backup strategies

    [:octicons-arrow-right-24: Learn more](user-guides/features-overview.md)

-   :material-cloud-sync:{ .lg .middle } __Cloud Integration__

    ---

    Sync with Google Drive, WebDAV, and 40+ providers via Rclone

    [:octicons-arrow-right-24: Cloud Setup](user-guides/cloud-storage.md)

-   :material-security:{ .lg .middle } __Biometric Security__

    ---

    Secure backups with fingerprint, face, or passkey authentication

    [:octicons-arrow-right-24: Security Guide](user-guides/biometric-security.md)

-   :material-robot:{ .lg .middle } __Automation__

    ---

    Schedule backups and create custom automation plugins

    [:octicons-arrow-right-24: Automation Guide](user-guides/automation.md)

</div>

## Quick Start

### Installation

```bash
# Download latest APK
wget https://github.com/obsidianbackup/releases/latest/obsidianbackup.apk

# Install
adb install obsidianbackup.apk
```

### First Backup

1. Launch ObsidianBackup
2. Grant necessary permissions
3. Select apps to backup
4. Choose backup location
5. Tap "Backup Now"

See [Getting Started Guide](user-guides/getting-started.md) for detailed instructions.

## Documentation Sections

### For Users

- **[Getting Started](user-guides/getting-started.md)** - Setup and first backup
- **[Features Overview](user-guides/features-overview.md)** - All available features
- **[Backup Configuration](user-guides/backup-configuration.md)** - Configure backups
- **[Cloud Storage](user-guides/cloud-storage.md)** - Cloud provider setup
- **[Automation](user-guides/automation.md)** - Automated backups
- **[FAQ](user-guides/faq.md)** - Common questions
- **[Troubleshooting](user-guides/troubleshooting.md)** - Problem resolution

### For Developers

- **[Architecture Overview](developer-guides/architecture.md)** - System design
- **[Plugin Development](developer-guides/plugin-development.md)** - Create plugins
- **[Building from Source](developer-guides/building.md)** - Build the app
- **[Testing Guide](developer-guides/testing.md)** - Testing strategies
- **[Contributing](developer-guides/contributing.md)** - Contribute code
- **[Security Policy](developer-guides/security-policy.md)** - Security guidelines

### Technical Documentation

- **[ADRs](adr/README.md)** - Architecture decisions
- **[Code Examples](examples/README.md)** - Implementation examples
- **[API Reference](api/index.html)** - Complete API documentation

## Community

- **GitHub**: [github.com/obsidianbackup](https://github.com/obsidianbackup)
- **Discussions**: [GitHub Discussions](https://github.com/obsidianbackup/discussions)
- **Issues**: [GitHub Issues](https://github.com/obsidianbackup/issues)
- **Email**: support@obsidianbackup.com

## License

ObsidianBackup is licensed under the GNU General Public License v3.0 (GPL-3.0).

Documentation is licensed under CC BY 4.0.
