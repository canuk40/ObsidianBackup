# Feature Flags — ObsidianBackup

**Last Updated:** 2026-02-20  
**Status:** ✅ Implemented — 33 runtime flags + compile-time PRO bypass

---

## Overview

ObsidianBackup has two distinct feature-control mechanisms:

1. **`PRO_GATING_ENABLED`** — Compile-time `BuildConfig` field. Controls whether subscription paywalls are enforced. Always `false` in debug builds so testing is never blocked by upgrade dialogs.

2. **Runtime Feature Flags** — 33 `Feature` enum entries managed by `FeatureFlagManager`. Each flag can be toggled on/off at runtime, locally or via Firebase Remote Config, without a release.

---

## PRO_GATING_ENABLED

### What it controls

When `false`, two code paths are bypassed:

| Path | File | Effect |
|------|------|--------|
| `FeatureFlags.isFeatureAvailable()` | `model/FeatureTier.kt` | Short-circuits to `true` for any `FeatureId`, bypassing tier checks used by `ProFeatureGate` composable and `FeatureGateService` |
| `SubscriptionManager.currentTier` | `billing/SubscriptionManager.kt` | Emits `FeatureTier.PRO` regardless of Play billing state, bypassing all direct `currentTier != FeatureTier.FREE` checks in screen composables |

These two paths together cover **every** PRO gate in the codebase.

### Build type values

Defined in `app/build.gradle.kts`:

```kotlin
buildTypes {
    debug {
        buildConfigField("Boolean", "PRO_GATING_ENABLED", "false")  // ← testing: all unlocked
    }
    release {
        buildConfigField("Boolean", "PRO_GATING_ENABLED", "true")   // ← production: billing enforced
    }
    benchmark {
        buildConfigField("Boolean", "PRO_GATING_ENABLED", "true")   // ← perf profiling: billing enforced
    }
}
```

### Testing the paywall in debug

To experience the paywall dialogs in a debug build (useful for UI/copy review):

```kotlin
// app/build.gradle.kts — debug block, temporarily:
buildConfigField("Boolean", "PRO_GATING_ENABLED", "true")
```

Then rebuild: `./gradlew assembleFreeDebug`

---

## Runtime Feature Flags

### Architecture

```
Feature (enum)
    ↓
FeatureFlagManager          ← Hilt @Singleton
    ↓
SharedPreferencesRemoteConfig   ← local storage + Firebase upstream
    ↓
Firebase Remote Config      ← server-side overrides (optional)
```

**Files:**
- `app/src/main/java/com/obsidianbackup/features/FeatureFlags.kt` — enum + manager + config adapter
- `app/src/main/java/com/obsidianbackup/ui/screens/FeatureFlagsScreen.kt` — in-app toggle UI

**Injection:** `FeatureFlagManager` is `@Singleton` injected via Hilt. `MainActivity` injects it and passes it to `ObsidianBackupApp` → `NavigationHost` → `FeatureFlagsScreen`.

### Usage

```kotlin
// In any ViewModel or UseCase — inject FeatureFlagManager via Hilt
@HiltViewModel
class SomeViewModel @Inject constructor(
    private val featureFlagManager: FeatureFlagManager
) : ViewModel() {
    fun doSomething() {
        if (featureFlagManager.isEnabled(Feature.GAMING_BACKUP)) {
            // proceed
        }
    }
}
```

### In-app toggle

**Settings → Feature Flags** — all 33 flags are listed with descriptions and live toggle switches. Changes persist immediately to `SharedPreferences`.

### Firebase Remote Config override

Firebase Remote Config keys map directly to `Feature.name` (lowercase). To remotely disable a flag:

```
Key:   gaming_backup
Value: false
```

Changes propagate on the next config fetch interval (default: 12 hours in production, 30 seconds during debug).

---

## All 33 Flags

### Core Engine

| Flag | Key | Default | Description |
|------|-----|---------|-------------|
| `PARALLEL_BACKUP` | `parallel_backup` | ✅ ON | Back up multiple apps simultaneously |
| `INCREMENTAL_BACKUP` | `incremental_backup` | ✅ ON | Only transfer changed blocks |
| `MERKLE_VERIFICATION` | `merkle_verification` | ✅ ON | SHA-256 Merkle tree integrity checks |
| `SPLIT_APK_SUPPORT` | `split_apk_support` | ✅ ON | Full Android App Bundle backup |

### Cloud / Sync

| Flag | Key | Default | Description |
|------|-----|---------|-------------|
| `CLOUD_SYNC` | `cloud_sync` | ✅ ON | Master cloud sync toggle |
| `GOOGLE_DRIVE` | `google_drive` | ✅ ON | Google Drive provider |
| `DROPBOX` | `dropbox` | ✅ ON | Dropbox provider |
| `ONEDRIVE` | `onedrive` | ✅ ON | Microsoft OneDrive provider |
| `AWS_S3` | `aws_s3` | ✅ ON | AWS S3 / compatible provider |
| `SFTP` | `sftp` | ✅ ON | SFTP server |
| `WEBDAV` | `webdav` | ✅ ON | WebDAV (Nextcloud, ownCloud, etc.) |
| `SYNCTHING` | `syncthing` | ✅ ON | Syncthing peer-to-peer sync |
| `DECENTRALIZED_STORAGE` | `decentralized_storage` | ❌ OFF | IPFS / Filecoin storage |

### Device Transfer

| Flag | Key | Default | Description |
|------|-----|---------|-------------|
| `WIFI_DIRECT_MIGRATION` | `wifi_direct_migration` | ❌ OFF | Wi-Fi Direct device-to-device migration |

### Backup Types

| Flag | Key | Default | Description |
|------|-----|---------|-------------|
| `GAMING_BACKUP` | `gaming_backup` | ✅ ON | Game save detection and emulator support |
| `HEALTH_CONNECT` | `health_connect` | ✅ ON | Health Connect fitness data backup |
| `PLUGIN_SYSTEM` | `plugin_system` | ✅ ON | Third-party plugin support |

### Scheduling & Automation

| Flag | Key | Default | Description |
|------|-----|---------|-------------|
| `SMART_SCHEDULING` | `smart_scheduling` | ✅ ON | ML-based optimal backup timing |
| `TASKER_INTEGRATION` | `tasker_integration` | ✅ ON | Tasker / Automate / Locale intents |
| `AUTOMATION_RULES` | `automation_rules` | ✅ ON | In-app rule builder |

### Security

| Flag | Key | Default | Description |
|------|-----|---------|-------------|
| `BIOMETRIC_AUTH` | `biometric_auth` | ✅ ON | Fingerprint / face / iris lock |
| `STANDARD_ENCRYPTION` | `standard_encryption` | ✅ ON | AES-256-GCM with Android Keystore |
| `POST_QUANTUM_CRYPTO` | `post_quantum_crypto` | ❌ OFF | Post-quantum encryption (experimental) |

### Diagnostics

| Flag | Key | Default | Description |
|------|-----|---------|-------------|
| `EXPORTABLE_LOGS` | `exportable_logs` | ✅ ON | In-app log viewer and export |
| `EXPORT_DIAGNOSTICS` | `export_diagnostics` | ✅ ON | Full diagnostics bundle export |
| `EXPORT_SHELL_AUDIT` | `export_shell_audit` | ✅ ON | Root shell audit log export |

### Storage

| Flag | Key | Default | Description |
|------|-----|---------|-------------|
| `RETENTION_POLICIES` | `retention_policies` | ✅ ON | Auto-delete old backups by age / count |
| `BUSYBOX_OPTIONS` | `busybox_options` | ✅ ON | BusyBox binary options |

### UI Modes

| Flag | Key | Default | Description |
|------|-----|---------|-------------|
| `SIMPLIFIED_MODE` | `simplified_mode` | ❌ OFF | Reduced UI for non-technical users |
| `SPEEDRUN_MODE` | `speedrun_mode` | ❌ OFF | Minimal chrome for competitive speedrunners |

### System Integrations

| Flag | Key | Default | Description |
|------|-----|---------|-------------|
| `DEEP_LINKING` | `deep_linking` | ✅ ON | Intent / URL deep link support |
| `WIDGET_SUPPORT` | `widget_support` | ✅ ON | Home screen quick-backup widget |
| `QUICK_SETTINGS_TILE` | `quick_settings_tile` | ✅ ON | Quick Settings panel tile |

---

## Operational Runbook

### Disabling a broken feature (production incident)

1. Open Firebase Console → Remote Config
2. Add parameter: `feature_name` = `false` (use the `Key` column above)
3. Publish — takes effect on next fetch (up to 12 hours, or restart)
4. For immediate effect: force-stop the app or call `fetchAndActivate()` in a hot-fix push

### Re-enabling after fix

1. Remove the parameter from Remote Config (reverts to local default) — OR —
2. Set parameter to `true` and publish

### Checking flag state via ADB

```bash
adb shell run-as com.obsidianbackup.free.debug \
  cat /data/data/com.obsidianbackup.free.debug/shared_prefs/feature_flags.xml
```

---

## Adding a New Flag

1. Add entry to `Feature` enum in `features/FeatureFlags.kt`
2. Add `Feature.YOUR_FLAG -> "Description text"` to the `description()` extension
3. Add `Feature.YOUR_FLAG -> true/false` to `featureDefault()` in `SharedPreferencesRemoteConfig`
4. Check the flag in your ViewModel/UseCase: `featureFlagManager.isEnabled(Feature.YOUR_FLAG)`
5. Update this document

---

## Related Files

| File | Purpose |
|------|---------|
| `app/src/main/java/com/obsidianbackup/features/FeatureFlags.kt` | Enum, manager, config adapter |
| `app/src/main/java/com/obsidianbackup/model/FeatureTier.kt` | `isFeatureAvailable()` — PRO gate bypass point 1 |
| `app/src/main/java/com/obsidianbackup/billing/SubscriptionManager.kt` | `currentTier` flow — PRO gate bypass point 2 |
| `app/src/main/java/com/obsidianbackup/ui/screens/FeatureFlagsScreen.kt` | In-app toggle UI |
| `app/src/main/java/com/obsidianbackup/MainActivity.kt` | Injects + passes `FeatureFlagManager` |
| `app/build.gradle.kts` | `PRO_GATING_ENABLED` BuildConfig field definitions |
