# Contributing to ObsidianBackup

Thank you for your interest in contributing! ObsidianBackup is a privacy-first, FOSS Android backup app for rooted devices. Every contribution helps.

---

## Ground rules

- **No tracking, no ads, no paywalls.** Contributions that introduce analytics, advertising SDKs, or subscription gating will not be accepted.
- **Privacy by default.** New features that handle user data must store it locally and encrypted. No data leaves the device without explicit user action.
- **Root operations must be auditable.** Any shell commands executed as root must go through `SafeShellExecutor` and be logged to the shell audit trail.

---

## How to contribute

### Reporting bugs

Open a [GitHub Issue](../../issues) with:
- Android version and device model
- Root method (Magisk / KernelSU / APatch / Shizuku)
- Steps to reproduce
- Logcat output (Settings → Logs → Export)

### Suggesting features

Open an issue tagged `enhancement`. Describe the use case, not just the feature.

### Submitting code

1. Fork the repository and create a branch: `git checkout -b feature/my-change`
2. Follow the existing code style (Kotlin, Clean Architecture, Hilt DI)
3. Add or update unit tests for any changed behaviour
4. Run the test suite: `./gradlew testDebugUnitTest`
5. Run static analysis: `./gradlew detekt`
6. Open a Pull Request against `main` with a clear description

### Commit style

Use conventional commits:
```
feat: add WebDAV chunked upload support
fix: crash when restoring split APKs on Android 14
docs: update root permission table in README
```

---

## Architecture overview

```
Presentation (Compose UI + ViewModels)
        ↓
Domain (UseCases)
        ↓
Data (Repositories + Room DB)
        ↓
Engine (BackupEngine / RestoreEngine — root shell)
```

See `docs/` for detailed guides.

---

## Building from source

See [README.md](README.md#building-from-source).

---

## License

By contributing, you agree to license your contributions under the [GNU General Public License v3.0](LICENSE).
