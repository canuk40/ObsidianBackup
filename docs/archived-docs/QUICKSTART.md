# Documentation Quick Start

Quick reference for using the ObsidianBackup documentation system.

## For Users

### Getting Started
1. Read [Getting Started Guide](user-guides/getting-started.md)
2. Review [Features Overview](user-guides/features-overview.md)
3. Configure backups: [Backup Configuration](user-guides/backup-configuration.md)
4. Set up cloud storage: [Cloud Storage Setup](user-guides/cloud-storage.md)
5. Enable automation: [Automation Guide](user-guides/automation.md)

### Need Help?
- [FAQ](user-guides/faq.md) - Common questions
- [Troubleshooting](user-guides/troubleshooting.md) - Problem resolution

## For Developers

### Contributing
1. Read [Architecture Overview](developer-guides/architecture.md)
2. Follow [Building from Source](developer-guides/building.md)
3. Review [Contributing Guidelines](developer-guides/contributing.md)
4. Check [Testing Guide](developer-guides/testing.md)

### Plugin Development
1. Read [Plugin Development Guide](developer-guides/plugin-development.md)
2. See [Plugin Example](examples/plugin-example.md)
3. Review [ADR 002: Plugin System](adr/002-plugin-system.md)

## Building Documentation

### Generate API Docs
```bash
./gradlew dokkaHtmlCustom
# Output: app/build/dokka/html/
```

### Build Static Site
```bash
cd docs/static-site
pip install mkdocs-material
mkdocs build
# Output: site/
```

### Serve Locally
```bash
cd docs/static-site
mkdocs serve
# Open http://localhost:8000
```

## Documentation Structure

```
docs/
├── user-guides/        # User documentation (8 files)
├── developer-guides/   # Developer docs (6 files)
├── adr/               # Architecture decisions (3 ADRs)
├── examples/          # Code examples
├── api/              # Generated API docs
└── static-site/      # MkDocs configuration
```

## Quick Links

- **Documentation Hub**: [README.md](README.md)
- **API Reference**: [api/index.html](api/index.html) (after generation)
- **System Overview**: [DOCUMENTATION_SYSTEM.md](../DOCUMENTATION_SYSTEM.md)
