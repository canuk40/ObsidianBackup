# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**ObsidianBackup** — FOSS Android backup/restore app (com.obsidianbackup)
- `versionCode: 8`, `versionName: 1.0.8` in `app/build.gradle.kts`
- **Always increment versionCode before every Play Store build**
- Single build flavor — no paywalls, all features unlocked

## Build Commands

```bash
# Standard release build (AAB for Play Store + APK for device)
./gradlew bundleRelease assembleRelease --no-daemon --stacktrace

# Debug build
./gradlew assembleDebug --no-daemon --stacktrace

# Static analysis
./gradlew detekt

# Unit tests
./gradlew testDebugUnitTest

# Instrumented tests (requires connected device)
./gradlew connectedAndroidTest

# Code coverage
./gradlew jacocoTestReport
```

Output locations:
- AAB: `app/build/outputs/bundle/release/app-release.aab`
- APK: `app/build/outputs/apk/release/app-release.apk`

Install to device: `adb -s BAUOUKZ9ZTNVZPXK install -r --streaming <path/to/apk>`

## Architecture

Clean Architecture with MVVM:

**Domain layer** (`domain/`) — use cases and repository interfaces.

**Data layer** (`data/repository/`) — Room + SQLCipher (encrypted DB), DataStore for preferences, repository implementations for apps, catalog, settings, logs, and schedules.

**Engine layer** (`engine/`) — `BackupEngine` and `RestoreEngine` execute backup/restore operations. Root shell and Shizuku integration live here.

**Presentation layer** (`presentation/`) — Jetpack Compose screens with Hilt-injected ViewModels.

Key subsystems:
- `cloud/` — Cloud provider integrations (Google Drive, Dropbox, WebDAV, S3, Azure, B2, pCloud, Oracle, Alibaba OSS) and Rclone backend
- `verification/` — Merkle tree integrity verification for backups
- `billing/FeatureGateService` — FOSS stub; always returns all features enabled
- `scheduling/` — WorkManager background jobs
- `plugins/` — Plugin framework and sandbox
- `security/` — Root detection (local methods only), Tasker security validation
- `crypto/` — SQLCipher, JWT verification (Nimbus JOSE+JWT)
- `community/AnalyticsManager` — No-op stub; no data is collected or transmitted

DI: Hilt throughout. Modules in `di/` wire repositories, use cases, and data sources.

## Testing

- **Framework**: JUnit 5 (Jupiter) + JUnit 4 Vintage compat
- **Mocking**: MockK
- **Assertions**: Truth (fluent)
- **Flow testing**: Turbine
- **Android tests**: Hilt test runner (`HiltTestRunner`), Espresso, Compose UI Test
- **Coverage**: JaCoCo — `app/build/reports/jacoco/`
- **Room schemas** exported to `app/src/androidTest/assets/schemas/` for migration tests

Run a single test class:
```bash
./gradlew testDebugUnitTest --tests "com.obsidianbackup.FullyQualifiedTestClass"
```

## Lint & Code Quality

- **Detekt** config: `config/detekt.yml` (cyclomatic complexity ≤15, method length ≤60 lines)
- **Android Lint**: non-blocking (abortOnError=false), MissingTranslation disabled
- Kotlin opt-ins required: `ExperimentalCoroutinesApi`, `ExperimentalMaterial3Api`

## Secrets & Signing

- `local.properties` — SDK path, cloud API keys (B2, etc). Never commit.
- `keystore.properties` — Release signing credentials. Never commit.
- `local.properties.template` — Template showing required keys.
- `app/google-services.json` — Committed as a placeholder template; fill in your own Firebase project for Crashlytics.
- Release signing keystore: `obsidianbackup.jks`. Build fails fast if missing.

## Modules

- `:app` — Main app module (all source under `app/src/main/java/com/obsidianbackup/`)
- `:root-core` — Shared root shell infrastructure (from ObsidianBox v31)
- `:tv`, `:wear` — Disabled/stub modules (not built)

## Key Stack Versions

- Kotlin: 2.0.21 | AGP: 8.13.2 | Gradle: 8.13
- compileSdk/targetSdk: 35 | minSdk: 26
- Compose BOM: 2024.10.00 | Room: 2.6.1 | Hilt: (see `gradle/libs.versions.toml`)
- Coroutines: 1.9.0 | OkHttp: 4.12.0 | Ktor: 2.3.7
- TensorFlow Lite: 2.14.0 | SQLCipher: 4.6.1
