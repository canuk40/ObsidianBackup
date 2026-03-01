# ObsidianBackup v1.0.0 Release Notes

**Release Date:** February 2026  
**Version:** 1.0.0-beta  
**Status:** 🟢 **BUILD SUCCESSFUL** — Release APK signed and verified on physical rooted device (Ulefone Armor X13, Android 15)

---

## 🆕 **v1.0.0-beta.4 — BusyBox, Release Signing & PRO Gating (2026-02-22)**

### BusyBox — Fully Wired and Verified
- Assets-based binary delivery: `busybox_arm64`, `busybox_arm`, `busybox_x86_64`, `busybox_x86` extracted from APK assets to `filesDir/bin/busybox` at runtime
- **SELinux fix:** `extractBundledBusyBox()` calls `chcon u:object_r:system_file:s0` via root after extraction — required on Android 10+ Enforcing mode where `app_data_file` context binaries cannot be executed directly
- **Root fallback execution:** `verifyBusyBox()`, `getVersion()`, `listApplets()` fall back to `su -c` if direct process execution is blocked by SELinux
- **PATH injection:** `SafeShellExecutor` prepends `filesDir/bin` to PATH for all root commands so `tar`, `rsync`, `zstd` etc. resolve to bundled BusyBox
- **Verified on device:** BusyBox v1.29.3-osm0sis, 342/342 applets enabled

### Release Signing — Dedicated Keystore
- New `obsidianbackup.jks` generated: 4096-bit RSA, 10,000 day validity, alias `obsidianbackup`
- `keystore.properties` at project root (gitignored)
- `build.gradle.kts` updated: keystore path now relative to project root, no longer cross-project dependency
- R8/ProGuard: added `-dontwarn org.tensorflow.lite.gpu.**` — fixes release build failure from missing TFLite GPU delegate classes
- **First release APK built and installed on device (180MB):** `com.obsidianbackup.free`, signed and running in ROOT mode ✅

### PRO Gating — Re-Enabled
- `PRO_GATING_ENABLED = true` now set for **all** build variants including `debug`
- PRO/TEAM/ENTERPRISE paywalls active — premium features show upgrade prompts for free-tier users
- Verified on release build: Standard Encryption, Zero-Knowledge, Cloud Providers all show `(PRO)` badges

### License
- Repository license changed from MIT → **GPL-3.0** (commit `f81acec`)

---

## 🆕 **v1.0.0-beta.3 — WorkManager, Security & ML Wiring (2026-02-21)**

### WorkManager Integration
- `BackupsViewModel` now dispatches `DeleteWorker`, `RestoreWorker`, and `VerifyWorker` via `OneTimeWorkRequestBuilder` — backup operations run off the main thread with proper lifecycle management
- `SmartBackupWorker` ML learning loop enabled: records real `BackupEvent` after each backup (timing, success, app count, device state) so the model improves with use

### Security Hardening
- **Tasker signature verification**: Full SHA-256 fingerprint (`973fe25b...6636d`) for Tasker extracted from installed APK and added to `KNOWN_SIGNATURES` — spoofed callers using Tasker's package name are now rejected
- **Backblaze B2 credentials**: Moved from hardcoded source strings to `local.properties` + `BuildConfig.B2_KEY_ID` / `BuildConfig.B2_APPLICATION_KEY` — credentials never committed to git
- **Health Connect permissions**: Real `PermissionController.createRequestPermissionResultContract()` launcher replacing `delay(500)` fake grant

### Plugin Registration
- `RcloneGoogleDrivePlugin`, `RcloneDropboxPlugin`, `RcloneS3Plugin`, `FilecoinCloudProviderPlugin` registered in `ObsidianBackupApplication.registerBuiltInPlugins()` — all 4 were previously invisible to the plugin system

### ML / Smart Scheduling
- `BackupPredictor` annotated `@Singleton @Inject constructor(@ApplicationContext)` — now fully injectable via Hilt
- `SmartSchedulingViewModel` injects `BackupPredictor` and exposes `suggestedTimes: StateFlow<List<BackupPrediction>>`

### Navigation & Onboarding
- Stub `CommunityScreen.kt` / `FeedbackScreen.kt` deleted; real `community/` package screens imported
- Onboarding detection moved from SharedPreferences to `OnboardingManager` DataStore via `DashboardViewModel.onboardingCompleted` StateFlow
- Settings → Advanced now includes Simplified Mode navigation entry
- Health screen now has Privacy & Data Permissions button wired to `health_privacy` route

### Error Recovery
- `BackupOrchestrator` wired to `ErrorRecoveryManager`: storage failures now trigger `attemptRecovery(ObsidianError.InsufficientStorage(...))`

### Gitignore
- `ml_training/venv/` and `ml_training/__pycache__/` added to `.gitignore`

---

## 🆕 **v1.0.0-beta.2 — Feature Flags & PRO Bypass (2026-02-20)**

### Feature Flag System (33 runtime flags)

Every major feature now has a runtime kill-switch managed by `FeatureFlagManager`. Features can be disabled server-side via Firebase Remote Config or locally via **Settings → Feature Flags** — without a release.

**Categories covered:** Core Engine · Cloud/Sync (per-provider) · Backup Types · Scheduling · Security · Diagnostics · Storage · UI Modes · System Integrations

See [`FEATURE_FLAGS.md`](FEATURE_FLAGS.md) for the complete flag reference.

### PRO_GATING_ENABLED Build Flag

Debug builds now bypass all subscription paywalls automatically:
- `debug` → `PRO_GATING_ENABLED = false` — all PRO/TEAM/ENTERPRISE screens accessible
- `release` → `PRO_GATING_ENABLED = true` — normal billing enforcement

Two bypass points wired: `FeatureFlags.isFeatureAvailable()` + `SubscriptionManager.currentTier`

### Physical Device Testing

App now tested and verified on **Ulefone Armor X13** (Android 15, Magisk-rooted). All 10 screens confirmed working; no PRO gates blocking testing.

### Bug Fixes

- ✅ Feature Flags screen no longer shows "Requires Firebase Remote Config" — `FeatureFlagManager` now properly injected in `MainActivity`
- ✅ Automation, Health Connect, Plugins screens all show real UI (no upgrade dialog) in debug

---

## 🎉 **v1.0.0-beta — Initial Release**

ObsidianBackup is a **root-focused Android backup engine** designed for power users. Built on modern Clean Architecture with the backup core ported from ObsidianBox v31 (shipping on Google Play), it provides full `/data/data` access, APK extraction, Merkle tree verification, and zero-knowledge encryption.

---

## ✨ **What's Working (Verified Feb 15, 2026)**

### 🛡️ **Root Detection & Integration**
- **Magisk 26.4** detection with Zygisk + DenyList
- Symlink-based su detection (`/sbin/su`, `/system/xbin/su`)
- SELinux status detection (Enforcing mode)
- Persistent root shell via ShellExecutor
- Automatic Magisk su policy configuration

### 📦 **Full App Backup (End-to-End Verified)**
- Select apps from 208+ detected packages (system + user)
- APK extraction via root shell (`/data/app/` access)
- Split APK support (Android App Bundles)
- App data backup via `tar` with zstd compression
- OBB and external data backup
- SHA-256 checksums per file
- Metadata JSON per snapshot
- **Tested:** 36 MB APK backup completed successfully

### ✅ **Backup Verification & Catalog**
- Merkle tree integrity verification after backup
- Green verified checkmark on validated backups
- Dual persistence: Room database + portable JSON
- Dashboard stats: Total Backups, Last Backup, Total Size
- Backup history with app count and size display

### 🎨 **Neon Shield UI (Material 3)**
- Dark cybersecurity theme matching app icon
- Neon cyan primary (#00E5FF) + purple secondary (#BB86FC)
- Branded navigation drawer with app icon and section labels
- Dashboard with stat mini-cards and colored quick-action buttons
- Permission mode chip with shield icon in top bar
- 10 screens: Dashboard, Apps, Backups, Automation, Gaming, Health, Plugins, Logs, Settings, Community
- Enhanced components: haptic feedback, scale animations, press effects

### 🔐 **Security**
- AES-256-GCM encryption with Android Keystore
- Zero-Knowledge encryption mode (client-side)
- Biometric authentication (fingerprint, face, iris)
- Root detection with multi-method chain
- Path traversal protection in file operations

### ☁️ **Cloud & Sync**
- WebDAV integration (Nextcloud, ownCloud, etc.)
- rclone backend (40+ cloud providers)
- Cloud provider configuration UI
- Background sync via WorkManager

### 🤖 **Automation**
- Scheduled backups with WorkManager
- Smart scheduling (battery, Wi-Fi constraints)
- Tasker integration with custom intents
- Backup profiles with configurable triggers

### 🎮 **Gaming & Health**
- Gaming backup screen with emulator detection
- Health Connect integration (50+ health metrics)
- Game save auto-detection

---

## 🏗️ **Architecture**

### Backup Pipeline
```
AppsViewModel → BackupAppsUseCase → BackupOrchestrator
  → ObsidianBoxEngine.backupApps() [root shell: cp APK, tar data]
    → backupSingleApp() [APK + data + OBB per app]
  → verifyAndCatalog() [Merkle verify + Room DB + JSON]
```

### Key Components
| Component | Role |
|-----------|------|
| `ObsidianBoxEngine` | 648-line production backup engine (from ObsidianBox) |
| `BackupOrchestrator` | Retry, incremental strategy, verification |
| `BackupCatalog` | Room DB + JSON dual persistence |
| `ShellExecutor` | Root/Shizuku/ADB/SAF unified shell |
| `RootDetectorImpl` | Multi-method Magisk/KernelSU/SuperSU detection |

### Module Structure
```
:app         — Main Android app (phone)
:root-core   — Root detection library (ported from ObsidianBox)
:tv          — Android TV variant
:wear        — Wear OS variant
```

---

## 📦 **Build Variants**

| Variant | Description |
|---------|-------------|
| `assembleFreeDebug` | Free tier, 3 profiles, debuggable |
| `assemblePremiumDebug` | **Fully unlocked**, 999 profiles, debuggable |
| `assemblePremiumRelease` | Production, R8 minified, signed |

### APK Outputs (Premium Debug)
| ABI | Size |
|-----|------|
| universal | 96 MB |
| arm64-v8a | 85 MB |
| armeabi-v7a | 78 MB |
| x86_64 | 87 MB (emulator) |

---

## 🐛 **Bug Fixes in This Release**

### Critical (P0) — All Resolved
1. **Build compilation failures** — 219 errors → 0 errors
2. **App crash on launch** — Compose `Screen` sealed class init NPE
3. **Root detection failure** — `UNKNOWN` status cached with `detectedAt = Long.MAX_VALUE`
4. **Backup "not implemented"** — Factory created stub `BusyBoxEngine` instead of DI-injected `ObsidianBoxEngine`
5. **Permission denied writing metadata** — Root shell `mkdir` created root-owned dirs in app space

### High (P1)
6. **Backups not appearing** — `saveSnapshot()` call missing in `verifyAndCatalog()`
7. **Catalog ENOENT** — Missing `parentFile.mkdirs()` in `BackupCatalog.saveSnapshot()`

### Minor (P2)
8. **"0 apps" display** — `getAllSnapshots()` returned `emptyList()` instead of parsing `appsJson`
9. **UI theme mismatch** — Rethemed from molten orange to neon cyan/purple matching app icon

---

## ⚠️ **Known Limitations**

- Restore operation not yet tested on emulator
- Multi-app backup not yet tested
- Firebase/Crashlytics not configured (no google-services.json)
- x86_64 ABI split included in debug (remove before production)
- Unit test suite (~305 methods) not yet executed
- SAF mode limited to media/documents only (root required for full backup)

---

## 🗓️ **Roadmap**

### v1.0.0 Release (Target: March 2026)
- [ ] Test restore flow end-to-end
- [ ] Test multi-app backup
- [ ] Run unit test suite
- [ ] Test on physical rooted device
- [ ] Generate signed AAB for Play Store
- [ ] Complete Play Store listing

### v1.1 (Q2 2026)
- Android TV app
- Import from Titanium Backup / Swift Backup
- Multi-language support
- OBB large file support
- Label/tagging system
- Batch operations

### v1.2 (Q3 2026)
- Wear OS standalone
- Desktop companion (Windows, macOS, Linux)
- REST API for remote management

---

## 🔧 **Emulator Testing Setup**

| Property | Value |
|----------|-------|
| AVD | `test_rooted`, API 33, x86_64, Google APIs |
| Magisk | 26.4 with Zygisk + DenyList |
| Root policy | `sqlite3 /data/adb/magisk.db "INSERT OR REPLACE INTO policies VALUES(UID, 2, 0, 1, 1);"` |
| App package | `com.obsidianbackup.free.debug` / `com.obsidianbackup.debug` |

---

**Thank you for testing ObsidianBackup!** 🛡️
