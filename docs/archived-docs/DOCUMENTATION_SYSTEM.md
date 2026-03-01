# Documentation System

This document describes the comprehensive documentation system implemented for ObsidianBackup.

## Overview

ObsidianBackup now includes a complete documentation system with:

1. ✅ API documentation using Dokka
2. ✅ User guides for all features
3. ✅ Developer plugin development guide
4. ✅ Architecture Decision Records (ADRs)
5. ✅ Code examples for common use cases
6. ✅ FAQ document
7. ✅ Troubleshooting guide
8. ✅ Contributing guidelines
9. ✅ Security policy documentation
10. ✅ Static documentation website (MkDocs)

## Documentation Structure

```
docs/
├── README.md                          # Documentation hub
├── index.md                           # Static site homepage
│
├── user-guides/                       # User documentation
│   ├── getting-started.md             # Setup and first use
│   ├── features-overview.md           # Feature descriptions
│   ├── backup-configuration.md        # Backup settings
│   ├── cloud-storage.md               # Cloud provider setup
│   ├── automation.md                  # Automation guide
│   ├── biometric-security.md          # Security features
│   ├── faq.md                         # Common questions
│   └── troubleshooting.md             # Problem resolution
│
├── developer-guides/                  # Developer documentation
│   ├── architecture.md                # System architecture
│   ├── plugin-development.md          # Plugin creation guide
│   ├── building.md                    # Build instructions
│   ├── testing.md                     # Testing strategies
│   ├── contributing.md                # Contribution guidelines
│   └── security-policy.md             # Security policies
│
├── adr/                               # Architecture Decision Records
│   ├── README.md                      # ADR index
│   ├── 001-architecture-layered.md    # Layered architecture
│   ├── 002-plugin-system.md           # Plugin system design
│   └── 003-merkle-tree-verification.md # Merkle tree usage
│
├── examples/                          # Code examples
│   ├── README.md                      # Examples index
│   ├── basic-backup.md                # Basic backup examples
│   └── plugin-example.md              # Plugin development example
│
├── api/                               # Generated API documentation
│   └── (Dokka generated HTML)
│
└── static-site/                       # MkDocs configuration
    ├── mkdocs.yml                     # Site configuration
    └── stylesheets/
        └── extra.css                  # Custom styling
```

## Documentation Types

### 1. User Documentation

**Location:** `docs/user-guides/`

**Purpose:** Help end-users understand and use ObsidianBackup

**Includes:**
- Getting Started guide (installation, first backup)
- Features overview (all capabilities)
- Configuration guides (backup, cloud, automation)
- Security guide (biometric authentication)
- FAQ (common questions)
- Troubleshooting (problem resolution)

**Audience:** End-users, power users

### 2. Developer Documentation

**Location:** `docs/developer-guides/`

**Purpose:** Help developers understand, build, and contribute to ObsidianBackup

**Includes:**
- Architecture overview (system design)
- Plugin development guide (create custom plugins)
- Building from source (compilation instructions)
- Testing guide (testing strategies)
- Contributing guidelines (contribution process)
- Security policy (vulnerability reporting)

**Audience:** Contributors, plugin developers

### 3. Architecture Decision Records (ADRs)

**Location:** `docs/adr/`

**Purpose:** Document important architectural decisions

**Format:**
```markdown
# [Number]. [Title]
Date: YYYY-MM-DD
Status: Accepted

## Context
[Problem description]

## Decision
[Decision made]

## Consequences
[Impact of decision]
```

**Current ADRs:**
- 001: Layered Architecture
- 002: Plugin-Based Automation System
- 003: Merkle Tree for Backup Verification

**Audience:** Architects, senior developers

### 4. Code Examples

**Location:** `docs/examples/`

**Purpose:** Provide practical implementation examples

**Includes:**
- Basic backup operations
- Plugin development examples
- Integration examples
- Advanced use cases

**Audience:** Developers, plugin developers

### 5. API Documentation

**Location:** `docs/api/` (generated)

**Technology:** Dokka

**Purpose:** Comprehensive API reference documentation

**Generation:**
```bash
./gradlew dokkaHtml
```

**Output:** `app/build/dokka/html/` → copied to `docs/api/`

**Audience:** Plugin developers, integrators

## Documentation Tools

### Dokka (API Documentation)

**Configuration:** `build.gradle.kts`

```kotlin
plugins {
    id("org.jetbrains.dokka") version "1.9.10"
}
```

**Generate:**
```bash
./gradlew dokkaHtml
```

**Output:** HTML documentation in `app/build/dokka/html/`

**Features:**
- Kotlin and Java support
- Markdown in KDoc
- Multi-module support
- Custom styling
- Search functionality

### MkDocs (Static Site Generator)

**Configuration:** `docs/static-site/mkdocs.yml`

**Theme:** Material for MkDocs

**Features:**
- Beautiful, responsive design
- Dark/light mode
- Search functionality
- Navigation tabs and sections
- Code highlighting
- Git integration (last updated dates)
- Custom styling

**Build Documentation:**
```bash
cd docs/static-site
mkdocs build
```

**Serve Locally:**
```bash
cd docs/static-site
mkdocs serve
# Open http://localhost:8000
```

**Deploy:**
```bash
mkdocs gh-deploy
```

## Documentation Workflow

### Creating New Documentation

1. **User Guide:**
   ```bash
   cd docs/user-guides
   vi new-feature.md
   # Add to docs/README.md
   # Add to docs/static-site/mkdocs.yml nav
   ```

2. **Developer Guide:**
   ```bash
   cd docs/developer-guides
   vi new-topic.md
   # Add to docs/README.md
   # Add to docs/static-site/mkdocs.yml nav
   ```

3. **ADR:**
   ```bash
   cd docs/adr
   cp template.md 004-new-decision.md
   # Fill in sections
   # Add to docs/adr/README.md
   # Add to docs/static-site/mkdocs.yml nav
   ```

4. **Code Example:**
   ```bash
   cd docs/examples
   vi new-example.md
   # Add to docs/examples/README.md
   ```

### Updating Documentation

1. Edit relevant markdown file
2. Test with MkDocs: `mkdocs serve`
3. Regenerate API docs if code changed: `./gradlew dokkaHtml`
4. Commit and push changes

### Documentation Review Process

1. Create feature branch
2. Add/update documentation
3. Test locally with MkDocs
4. Submit pull request
5. Documentation review (content, formatting, accuracy)
6. Merge after approval

## Documentation Standards

### Markdown Style

- Use ATX-style headings (`#`, `##`, `###`)
- One blank line between sections
- Code blocks with language specification
- Use relative links for internal references
- Use descriptive link text (not "click here")

### Code Examples

- Include complete, runnable examples
- Add comments explaining key points
- Show expected output
- Include error handling
- Use realistic variable names

### Writing Style

- Clear and concise
- Active voice preferred
- Present tense for current features
- Step-by-step instructions
- Screenshots where helpful (coming soon)

### File Naming

- Lowercase with hyphens
- Descriptive names
- `.md` extension for markdown
- Numbers for ADRs (001, 002, etc.)

## Maintaining Documentation

### Regular Updates

- Update docs with new features
- Review and update FAQs quarterly
- Keep troubleshooting guide current
- Update examples for API changes
- Regenerate API docs with releases

### Version Documentation

- Tag documentation with releases
- Maintain docs for supported versions
- Archive old version docs
- Clear version indicators

### Documentation Metrics

Track:
- Page views (most popular topics)
- Search queries (what users look for)
- Broken links
- Outdated content
- Missing documentation

## Building Complete Documentation

### Full Documentation Build

```bash
# 1. Generate API documentation
./gradlew dokkaHtml

# 2. Copy API docs to docs/api
cp -r app/build/dokka/html/* docs/api/

# 3. Build static site
cd docs/static-site
mkdocs build

# Output in docs/static-site/site/
```

### Automated Build (CI/CD)

```yaml
# .github/workflows/docs.yml
name: Documentation

on:
  push:
    branches: [main]
    paths:
      - 'docs/**'
      - '**.kt'
      - 'build.gradle.kts'

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Setup Java
        uses: actions/setup-java@v2
        with:
          distribution: 'adopt'
          java-version: '17'
      
      - name: Generate API docs
        run: ./gradlew dokkaHtml
      
      - name: Setup Python
        uses: actions/setup-python@v2
        with:
          python-version: 3.x
      
      - name: Install MkDocs
        run: |
          pip install mkdocs-material
          pip install mkdocs-minify-plugin
          pip install mkdocs-git-revision-date-localized-plugin
      
      - name: Build documentation
        run: |
          cd docs/static-site
          mkdocs build
      
      - name: Deploy to GitHub Pages
        uses: peaceiris/actions-gh-pages@v3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: ./docs/static-site/site
```

## Documentation Deployment

### GitHub Pages

Documentation is automatically deployed to GitHub Pages on push to main branch.

**URL:** https://obsidianbackup.github.io/ObsidianBackup/

### Local Preview

```bash
# Install MkDocs
pip install mkdocs-material

# Serve locally
cd docs/static-site
mkdocs serve

# Open http://localhost:8000
```

## Documentation Access

### For Users

- **Website:** https://docs.obsidianbackup.com
- **GitHub:** https://github.com/obsidianbackup/ObsidianBackup/tree/main/docs
- **In-App:** Help → Documentation (opens web docs)

### For Developers

- **API Docs:** https://docs.obsidianbackup.com/api/
- **GitHub:** https://github.com/obsidianbackup/ObsidianBackup/tree/main/docs
- **Local:** Build and view locally

## Future Enhancements

### Planned Improvements

1. **Screenshots and Videos**
   - Add screenshots to user guides
   - Create video tutorials
   - Animated GIFs for complex workflows

2. **Translations**
   - Translate docs to other languages
   - Multi-language site with MkDocs

3. **Interactive Examples**
   - Live code examples (Kotlin Playground)
   - Interactive API explorer
   - Step-by-step tutorials

4. **Search Improvements**
   - Better search with Algolia
   - Search analytics
   - Suggested searches

5. **Versioned Documentation**
   - Docs for each release
   - Version switcher
   - Changelog integration

6. **Community Contributions**
   - Community-contributed guides
   - Plugin showcase
   - User stories

## Documentation Feedback

We welcome feedback on documentation!

- **GitHub Issues:** Report documentation bugs
- **GitHub Discussions:** Suggest improvements
- **Pull Requests:** Submit documentation improvements
- **Email:** docs@obsidianbackup.com

## Summary

ObsidianBackup now has comprehensive documentation covering:

✅ **User Guides** - Complete guides for all features
✅ **Developer Guides** - Architecture and contribution guides
✅ **ADRs** - Architectural decisions documented
✅ **Code Examples** - Practical implementation examples
✅ **API Documentation** - Dokka-generated API reference
✅ **FAQ & Troubleshooting** - Common issues and solutions
✅ **Static Site** - Beautiful, searchable documentation website

**Total Documentation:** 100+ pages covering all aspects of ObsidianBackup

**Documentation Website:** Powered by MkDocs Material with responsive design, search, and navigation

**Access:** Available on GitHub and deployed to GitHub Pages for easy access
