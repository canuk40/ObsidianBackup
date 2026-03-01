# ObsidianBackup

**A free and open-source Android backup and restore app for rooted devices.**

ObsidianBackup gives you full control over your device data: back up apps, app data, SMS/MMS, call logs, contacts, media, and system settings — then restore them locally or sync to any cloud provider you choose. No ads. No telemetry. No paywalls. All features are unlocked for every user.

---

## What it does

- **App + data backup and restore** — Full APK + data backup using root or Shizuku, with incremental and scheduled backup support.
- **Cloud sync** — Send backups to Google Drive, Dropbox, WebDAV, Backblaze B2, AWS S3 (and S3-compatible endpoints), Azure Blob Storage, Alibaba OSS, Oracle Cloud, or local storage.
- **SMS / MMS backup** — Full message thread backup and restore via root.
- **Call log backup** — Back up and restore your call history.
- **Contacts backup** — Export and restore contacts in standard formats.
- **Health Connect sync** — Back up Health Connect data.
- **Gaming backup** — Save state backup for Android emulators (RetroArch, Dolphin, etc.).
- **Scheduled backups** — WorkManager-powered schedules: nightly, on charge, weekly, on WiFi.
- **Tasker / MacroDroid integration** — Trigger backups from automation apps using signature-protected intents.
- **Wear OS companion** — Status and controls on your watch.
- **Merkle-tree integrity verification** — Cryptographic verification of backup archives.
- **Encrypted database** — All local backup metadata is stored in a SQLCipher-encrypted Room database.
- **Biometric lock** — Optional biometric authentication to open the app.
- **Plugin system** — Extensible plugin framework for adding custom cloud providers and automation workflows.

---

## Root permissions — what is required and why

ObsidianBackup requests elevated privileges only for operations that genuinely require them. Standard Android APIs are used where possible.

| Permission / Capability | Why it is needed |
|------------------------|------------------|
| **Root shell** (`su`) | Required to read app private data directories (`/data/data/<pkg>/`) that are inaccessible to other apps without root. Without this, only APKs (not app data) can be backed up. |
| **Shizuku** (alternative to root) | Shizuku provides adb-level access without a full root shell. Used for app data backup on rooted devices where ADB shell is preferred over `su`. |
| `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` | Required on Android ≤ 9 to read/write backup archives on external storage. |
| `MANAGE_EXTERNAL_STORAGE` | Required on Android 11+ for advanced operations (e.g., backup of files outside app-private storage). Prompted at runtime — not granted silently. |
| `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` | Android 13+ granular media permissions. Used only if the user opts into media backup. |
| `READ_SMS` | SMS/MMS backup. On rooted devices root auto-grants this; on non-root devices it is requested at runtime. |
| `READ_CALL_LOG` / `WRITE_CALL_LOG` | Call log backup and restore. |
| `READ_CONTACTS` / `WRITE_CONTACTS` | Contacts backup and restore. |
| `SET_WALLPAPER` | Wallpaper restore. |
| `QUERY_ALL_PACKAGES` | Required to enumerate installed apps so the user can select which apps to back up. |
| `INTERNET` / `ACCESS_NETWORK_STATE` | Cloud sync to user-configured providers. No data is sent without explicit user action. |
| `WAKE_LOCK` | Keeps the CPU awake during long backup operations running in a WorkManager foreground service. |
| `RECEIVE_BOOT_COMPLETED` | Re-arms backup schedules after device reboot. |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC` | Required to run backup operations in the background as a foreground service with a persistent notification. |
| `POST_NOTIFICATIONS` | Shows backup progress and completion notifications (Android 13+). |
| `SCHEDULE_EXACT_ALARM` | Required on Android 12+ to schedule backups at exact times. |
| `CAMERA` | QR code scanning — used for WiFi Direct pairing and for scanning cloud provider configuration codes. |
| `USE_BIOMETRIC` | Optional biometric lock for the app. |
| `VIBRATE` | Haptic feedback on backup completion. |

**No permissions are used for tracking, advertising, or any purpose unrelated to backup and restore operations.**

---

## Building from source

### Prerequisites

- Android Studio Hedgehog or later (or command-line tools)
- JDK 17
- Android SDK with `compileSdk 35`
- A release keystore (for signed builds — optional for debug)

### Clone and build

```bash
git clone https://github.com/YOUR_USERNAME/ObsidianBackup.git
cd ObsidianBackup
```

#### Debug build (no signing required)

```bash
./gradlew assembleDebug --no-daemon --stacktrace
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

#### Release build (requires keystore)

1. Copy `local.properties.template` to `local.properties` and fill in your SDK path and any optional cloud API keys.
2. Create `keystore.properties` with your signing credentials:

```properties
storePassword=YOUR_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

3. Place your keystore at `obsidianbackup.jks` in the project root.

```bash
./gradlew bundleRelease assembleRelease --no-daemon --stacktrace
```

Output:
- AAB (Play Store): `app/build/outputs/bundle/release/app-release.aab`
- APK (sideload): `app/build/outputs/apk/release/app-release.apk`

#### Install to connected device

```bash
adb install -r --streaming app/build/outputs/apk/release/app-release.apk
```

### Optional: Firebase Crashlytics

The `app/google-services.json` in this repo is a **placeholder template** with `YOUR_*` values. Crashlytics will not function with the template — this is intentional.

If you want crash reporting in your own builds:
1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
2. Add your `google-services.json` (replace the placeholder)
3. To avoid accidentally committing real credentials: `git update-index --assume-unchanged app/google-services.json`

If you do not want Crashlytics at all, leave the placeholder as-is — Firebase initialization will fail silently and the app will function normally.

### Running tests

```bash
# Unit tests
./gradlew testDebugUnitTest --no-daemon

# Static analysis
./gradlew detekt

# Code coverage
./gradlew jacocoTestReport
```

---

## Architecture

Clean Architecture with MVVM:

- **Domain layer** (`domain/`) — Use cases and repository interfaces. No Android dependencies.
- **Data layer** (`data/`) — Room + SQLCipher encrypted database, DataStore preferences, repository implementations.
- **Engine layer** (`engine/`) — `BackupEngine` and `RestoreEngine`. Root shell and Shizuku integration.
- **Presentation layer** (`presentation/`, `ui/`) — Jetpack Compose screens with Hilt-injected ViewModels.
- **Cloud layer** (`cloud/`) — Provider integrations using Rclone and native SDKs.
- **Plugin system** (`plugins/`) — Sandboxed plugin framework for third-party extensions.

Dependency injection: Hilt throughout.

---

## Privacy

- **Zero analytics** — `AnalyticsManager` is a no-op stub. No user behaviour data is collected or transmitted.
- **No ads** — No ad SDKs are present.
- **No paywalls** — All features are unlocked. `FeatureGateService` always returns `true` for every feature.
- **Crashlytics is opt-in for builders** — The shipped `google-services.json` is a non-functional placeholder. Builders who want crash reports supply their own Firebase project.
- **No PII in crash reports** — `CrashlyticsManager` strips emails, phone numbers, and tokens before logging.
- **Local root operations only** — Root detection uses only local heuristics. No data leaves the device during root/Shizuku setup.
- **Encrypted local storage** — All backup metadata is stored in a SQLCipher-encrypted database.
- **Signature-protected integrations** — Tasker/MacroDroid and Wear OS integrations require matching app signature, preventing unauthorized access.

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

---

## License

ObsidianBackup is licensed under the **GNU General Public License v3.0**.

See [LICENSE](LICENSE) for the full text.

> This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
