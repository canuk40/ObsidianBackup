# ObsidianBackup Documentation

Welcome to the comprehensive documentation for ObsidianBackup - a powerful Android backup solution.

## Documentation Structure

### User Guides
- [Getting Started](user-guides/getting-started.md)
- [Features Overview](user-guides/features-overview.md)
- [Backup Configuration](user-guides/backup-configuration.md)
- [Cloud Storage Setup](user-guides/cloud-storage.md)
- [Automation Guide](user-guides/automation.md)
- [Biometric Security](user-guides/biometric-security.md)
- [FAQ](user-guides/faq.md)
- [Troubleshooting](user-guides/troubleshooting.md)

### Developer Guides
- [Architecture Overview](developer-guides/architecture.md)
- [Plugin Development Guide](developer-guides/plugin-development.md)
- [Building from Source](developer-guides/building.md)
- [Testing Guide](developer-guides/testing.md)
- [Contributing Guidelines](developer-guides/contributing.md)
- [Security Policy](developer-guides/security-policy.md)

### Architecture Decision Records (ADRs)
- [ADR Index](adr/README.md)
- Architecture decisions and rationale

### Code Examples
- [Common Use Cases](examples/README.md)
- Sample implementations and integrations

### API Documentation
- [Dokka Generated API Docs](api/index.html)
- Comprehensive API reference

## Quick Links

- [Project Homepage](https://github.com/obsidianbackup)
- [Issue Tracker](https://github.com/obsidianbackup/issues)
- [Discussions](https://github.com/obsidianbackup/discussions)

## Documentation Generation

To generate the full documentation:

```bash
# Generate API documentation with Dokka
./gradlew dokkaHtml

# Generate static documentation site
cd docs/static-site
mkdocs build

# View documentation locally
mkdocs serve
```

## Contributing to Documentation

We welcome documentation improvements! See [Contributing Guidelines](developer-guides/contributing.md) for details.

## License

This documentation is licensed under CC BY 4.0.
