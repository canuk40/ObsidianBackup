# 📚 ObsidianBackup Documentation

Complete documentation system for ObsidianBackup Android backup solution.

## 📖 Quick Access

### For Users
- **[Getting Started Guide](docs/user-guides/getting-started.md)** - Installation and first backup
- **[Features Overview](docs/user-guides/features-overview.md)** - All available features
- **[FAQ](docs/user-guides/faq.md)** - Frequently asked questions
- **[Troubleshooting](docs/user-guides/troubleshooting.md)** - Problem resolution

### For Developers
- **[Architecture Overview](docs/developer-guides/architecture.md)** - System design
- **[Plugin Development](docs/developer-guides/plugin-development.md)** - Create custom plugins
- **[Building from Source](docs/developer-guides/building.md)** - Build instructions
- **[Contributing Guidelines](docs/developer-guides/contributing.md)** - How to contribute

### Documentation Hub
- **[Documentation Index](docs/README.md)** - Complete documentation index
- **[Quick Start](docs/QUICKSTART.md)** - Quick reference guide
- **[System Overview](DOCUMENTATION_SYSTEM.md)** - Documentation system details

## 🏗️ Documentation Structure

```
📦 ObsidianBackup
├── 📄 DOCUMENTATION_SYSTEM.md         # Documentation system overview
├── 📄 DOCUMENTATION_SUMMARY.txt       # Implementation summary
├── 📁 docs/                           # Main documentation directory
│   ├── 📄 README.md                   # Documentation hub
│   ├── 📄 index.md                    # Static site homepage
│   ├── 📄 QUICKSTART.md               # Quick reference
│   │
│   ├── 📁 user-guides/                # 8 user guides
│   │   ├── getting-started.md
│   │   ├── features-overview.md
│   │   ├── backup-configuration.md
│   │   ├── cloud-storage.md
│   │   ├── automation.md
│   │   ├── biometric-security.md
│   │   ├── faq.md
│   │   └── troubleshooting.md
│   │
│   ├── 📁 developer-guides/           # 6 developer guides
│   │   ├── architecture.md
│   │   ├── plugin-development.md
│   │   ├── building.md
│   │   ├── testing.md
│   │   ├── contributing.md
│   │   └── security-policy.md
│   │
│   ├── 📁 adr/                        # Architecture decisions
│   │   ├── README.md
│   │   ├── 001-architecture-layered.md
│   │   ├── 002-plugin-system.md
│   │   └── 003-merkle-tree-verification.md
│   │
│   ├── 📁 examples/                   # Code examples
│   │   ├── README.md
│   │   ├── basic-backup.md
│   │   └── plugin-example.md
│   │
│   ├── 📁 api/                        # Generated API docs
│   │   └── (Dokka generated)
│   │
│   └── 📁 static-site/                # MkDocs configuration
│       ├── mkdocs.yml
│       └── stylesheets/
│           └── extra.css
```

## 🔧 Building Documentation

### Generate API Documentation (Dokka)

```bash
./gradlew dokkaHtml
# Output: app/build/dokka/html/
```

### Build Static Documentation Site (MkDocs)

```bash
# Install MkDocs
pip install mkdocs-material

# Build site
cd docs/static-site
mkdocs build
# Output: docs/static-site/site/
```

### Serve Documentation Locally

```bash
cd docs/static-site
mkdocs serve
# Open http://localhost:8000
```

## 📊 Documentation Stats

- **Total Files**: 22 markdown files
- **User Guides**: 8 comprehensive guides
- **Developer Guides**: 6 technical guides
- **ADRs**: 3 architectural decisions
- **Code Examples**: Multiple practical examples
- **Total Content**: ~6,000 lines of documentation

## ✨ Features

✅ **Comprehensive Coverage** - All features documented
✅ **Multiple Audiences** - Users, developers, contributors
✅ **Code Examples** - Practical implementation samples
✅ **API Reference** - Auto-generated from source
✅ **Search Functionality** - Full-text search
✅ **Responsive Design** - Mobile and desktop friendly
✅ **Dark/Light Mode** - Theme support
✅ **Easy to Update** - Markdown-based

## 🚀 Key Documentation

### User Documentation
1. [Getting Started](docs/user-guides/getting-started.md) - Setup and first use
2. [Backup Configuration](docs/user-guides/backup-configuration.md) - Configure settings
3. [Cloud Storage Setup](docs/user-guides/cloud-storage.md) - Cloud providers
4. [Automation Guide](docs/user-guides/automation.md) - Automated backups
5. [Biometric Security](docs/user-guides/biometric-security.md) - Security features
6. [FAQ](docs/user-guides/faq.md) - Common questions
7. [Troubleshooting](docs/user-guides/troubleshooting.md) - Problem solving

### Developer Documentation
1. [Architecture Overview](docs/developer-guides/architecture.md) - System design
2. [Plugin Development](docs/developer-guides/plugin-development.md) - Create plugins
3. [Building from Source](docs/developer-guides/building.md) - Compilation
4. [Testing Guide](docs/developer-guides/testing.md) - Testing strategies
5. [Contributing](docs/developer-guides/contributing.md) - Contribution guide
6. [Security Policy](docs/developer-guides/security-policy.md) - Security

## 🤝 Contributing to Documentation

We welcome documentation improvements!

1. Fork the repository
2. Edit markdown files in `docs/`
3. Test with `mkdocs serve`
4. Submit pull request

See [Contributing Guidelines](docs/developer-guides/contributing.md) for details.

## 📝 Documentation Standards

- **Markdown**: GitHub-flavored markdown
- **Style**: Clear, concise, active voice
- **Code**: Syntax-highlighted, complete examples
- **Links**: Relative links for internal references
- **Structure**: Hierarchical headings

## 🔍 Finding Information

- **Search**: Use MkDocs search (when site is running)
- **Index**: Check [docs/README.md](docs/README.md)
- **Quick Start**: See [docs/QUICKSTART.md](docs/QUICKSTART.md)
- **GitHub**: Use GitHub repository search

## 📧 Support

- **GitHub Issues**: Bug reports and feature requests
- **GitHub Discussions**: Questions and community support
- **Documentation**: All guides in `docs/` directory
- **Email**: docs@obsidianbackup.com

## 📜 License

Documentation licensed under CC BY 4.0.
Code licensed under GPL-3.0.

---

**Documentation System**: Implemented and Complete ✅
**Last Updated**: 2024-02-08
**Version**: 1.0.0
