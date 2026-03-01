---

# OBSIDIANBACKUP CODEBASE AUDIT REPORT
*Generated: 2026-02-21 — Full scan of app, wear, and tv modules*

---

## SECTION 1 — APP MODULE

### 1. Feature Coming Soon

**S1-CS-01** — `app/src/main/java/com/obsidianbackup/ui/screens/GamingScreen.kt` line 64  
Element: Entire `GamingScreen` composable body when `!uiState.featureEnabled`  
Code: `ComingSoonMessage(padding = padding, title = "Gaming Backups", description = "... version 1.1 ...")`  
Should do: Display real emulator detection, game save backup UI, and ROM management.

**S1-CS-02** — `app/src/main/java/com/obsidianbackup/ui/screens/PluginsScreen.kt` line 107  
Element: "Browse Plugin Store" button  
Code: `Toast.makeText(context, "Plugin Store - Coming Soon", Toast.LENGTH_SHORT).show()`  
Should do: Open a real in-app plugin marketplace or a WebView to the plugin registry.

**S1-CS-03** — `app/src/main/java/com/obsidianbackup/ui/screens/OtherScreens.kt` line 179  
Element: OAuth flow confirmation button inside `showOAuthDialog`  
Code: `Toast.makeText(context, "OAuth flow - Coming Soon", Toast.LENGTH_SHORT).show()`  
Should do: Launch the real OAuth 2.0 browser-based authentication flow.

**S1-CS-04** — `app/src/main/java/com/obsidianbackup/ui/screens/SettingsScreen.kt` line 108  
Element: "Compression Profile" settings item  
Code: `onClick = { showComingSoonDialog = "Compression Profile" }`  
Should do: Navigate to a screen to select compression levels (none/fast/balanced/best).

**S1-CS-05** — `app/src/main/java/com/obsidianbackup/ui/screens/SettingsScreen.kt` line 195  
Element: "Sync Policies" settings item  
Code: `onClick = { showComingSoonDialog = "Sync Policies" }`  
Should do: Navigate to sync policy configuration (frequency, conditions, conflict resolution).

**S1-CS-06** — `app/src/main/java/com/obsidianbackup/ui/screens/SettingsScreen.kt` line 214  
Element: "Play Games Cloud Sync" settings item  
Code: `onClick = { showComingSoonDialog = "Play Games Cloud Sync" }`  
Should do: Integrate with Google Play Games Services API for game save cloud sync.

**S1-CS-07** — `app/src/main/java/com/obsidianbackup/ui/screens/SettingsScreen.kt` line 233  
Element: "Privacy Settings" settings item  
Code: `onClick = { showComingSoonDialog = "Privacy Settings" }`  
Should do: Open a screen to control health data anonymization and export privacy options.

**S1-CS-08** — `app/src/main/java/com/obsidianbackup/ui/screens/SettingsScreen.kt` line 280  
Element: "Plugin Security" settings item  
Code: `onClick = { showComingSoonDialog = "Plugin Security" }`  
Should do: Open plugin sandbox configuration and permission management screen.

**S1-CS-09** — `app/src/main/java/com/obsidianbackup/ui/screens/SettingsScreen.kt` line 291  
Element: "Retention Policies" settings item  
Code: `onClick = { showComingSoonDialog = "Retention Policies" }`  
Should do: Navigate to auto-delete policy configuration (keep last N, time-based, size-based).

**S1-CS-10** — `app/src/main/java/com/obsidianbackup/ui/screens/SettingsScreen.kt` line 299  
Element: "Storage Limits" settings item  
Code: `onClick = { showComingSoonDialog = "Storage Limits" }`  
Should do: Navigate to maximum backup storage quota management screen.

**S1-CS-11** — `app/src/main/java/com/obsidianbackup/ui/screens/SettingsScreen.kt` line 310  
Element: "Permission Mode" settings item  
Code: `onClick = { showComingSoonDialog = "Permission Mode" }`  
Should do: Show radio buttons to prefer Root / Shizuku / SAF mode for backup operations.

**S1-CS-12** — ✅ **RESOLVED (2026-02-22)**  
BusyBox Options navigates to `BusyBoxOptionsScreen` which is fully implemented.  
`BusyBoxManager` extracts bundled static binaries from assets (`busybox_arm64`, `busybox_arm`, `busybox_x86_64`, `busybox_x86`) to `filesDir/bin/busybox`, applies SELinux `chcon u:object_r:system_file:s0` fix, and falls back to root shell execution. Verified: v1.29.3-osm0sis, 342 applets.

**S1-CS-13** — `app/src/main/java/com/obsidianbackup/ui/screens/SettingsScreen.kt` line 401  
Element: "Export Diagnostics" settings item  
Code: `onClick = { showComingSoonDialog = "Export Diagnostics" }`  
Should do: Collect and export system info + logs into a ZIP share intent.

**S1-CS-14** — `app/src/main/java/com/obsidianbackup/ui/screens/SettingsScreen.kt` line 409  
Element: "Export App Logs" settings item  
Code: `onClick = { showComingSoonDialog = "Export App Logs" }`  
Should do: Export Timber log buffer to file and launch a share intent.

**S1-CS-15** — `app/src/main/java/com/obsidianbackup/ui/screens/SettingsScreen.kt` line 417  
Element: "Export Shell Audit Logs" settings item  
Code: `onClick = { showComingSoonDialog = "Export Shell Audit Logs" }`  
Should do: Export root shell command audit trail to a file for compliance/debugging.

---

### 2. Stubs

**S1-ST-01** — `app/src/main/java/com/obsidianbackup/billing/SubscriptionManager.kt` lines 237–242  
Function: `getSubscriptionHistory(): Flow<List<SubscriptionHistoryEntry>>`  
Returns: `emptyList()` with comment `// This is a simplified version`  
Should do: Query and return the actual purchase history from `BillingRepository`.

**S1-ST-02** — `app/src/main/java/com/obsidianbackup/cloud/providers/PCloudProvider.kt` lines 112–118  
Function: `listSnapshots(filter): CloudResult<List<CloudSnapshotInfo>>`  
Returns: `CloudResult.Error(CloudError(UNKNOWN, "pCloud snapshot listing not yet implemented"))`  
Code: `// Upload uses pCloud REST API but snapshot catalog listing is not implemented.`  
Should do: Query `/listfolder` pCloud API endpoint and map to `CloudSnapshotInfo`.

**S1-ST-03** — `app/src/main/java/com/obsidianbackup/cloud/providers/FtpCloudProvider.kt` lines 113–119  
Function: `listSnapshots(filter): CloudResult<List<CloudSnapshotInfo>>`  
Returns: `CloudResult.Error(CloudError(UNKNOWN, "FTP/SFTP snapshot listing not yet implemented"))`  
Code: `// FTP/SFTP transport layer (Apache Commons Net / JSch) is not integrated yet.`  
Also: `uploadFile()` and `downloadFile()` both return `CloudResult.Success(Unit)` without performing any real transfer — `// Would use Apache Commons FTPClient or JSch in production`.  
Should do: Integrate Apache Commons Net FTPClient or JSch SFTP for actual file transfer and directory listing.

**S1-ST-04** — `app/src/main/java/com/obsidianbackup/cloud/providers/FtpCloudProvider.kt` line 132  
Function: `uploadFile(localFile, remotePath, metadata): CloudResult<Unit>`  
Returns: `CloudResult.Success(Unit)` without transferring any bytes.  
Code: `// Would use Apache Commons FTPClient or JSch in production`

**S1-ST-05** — `app/src/main/java/com/obsidianbackup/cloud/providers/FtpCloudProvider.kt` line 144  
Function: `downloadFile(remotePath, localFile): CloudResult<Unit>`  
Returns: `CloudResult.Success(Unit)` without downloading any bytes.

**S1-ST-06** — `app/src/main/java/com/obsidianbackup/cloud/providers/SmbCloudProvider.kt` line 122  
Function: `uploadFile()` / `downloadFile()`  
Returns: `CloudResult.Success(Unit)` without real SMB transfer.  
Code: `// SMB file copy — would use jcifs-ng or smbj library in production`

**S1-ST-07** — `app/src/main/java/com/obsidianbackup/cloud/providers/MegaCloudProvider.kt` lines 114–120  
Function: `listSnapshots(filter): CloudResult<List<CloudSnapshotInfo>>`  
Returns: `CloudResult.Success(emptyList())` — no real MEGA directory listing.

**S1-ST-08** — `app/src/main/java/com/obsidianbackup/gaming/GamingBackupManager.kt` lines 321–327  
Function: `restoreGameSaves(zipFile, emulator, gameName, profileSlot)`  
Returns: Nothing (empty body).  
Code: `// Implementation would restore files to emulator-specific locations` / `// This is a simplified version`  
Should do: Unzip save files and copy them to the emulator's save directory using root shell.

**S1-ST-09** — `app/src/main/java/com/obsidianbackup/cloud/oauth/OAuth2Provider.kt` lines 222–226  
Function: `listAccounts(): List<String>`  
Returns: `listOf("default")` always.  
Code: `// For now, simplified version`  
Should do: Read persisted OAuth account list from `KeystoreManager`.

**S1-ST-10** — `app/src/main/java/com/obsidianbackup/tasker/TaskerIntegration.kt` lines 419–423  
Function: `getCallingPackage(context: Context): String`  
Returns: `"unknown"` always.  
Code: `// This is a simplified implementation / In production, use proper caller identification`  
Should do: Use `Binder.getCallingUid()` and `PackageManager` to resolve the caller's package name.

**S1-ST-11** — `app/src/main/java/com/obsidianbackup/presentation/gaming/GamingBackupViewModel.kt` lines 46–58  
Function: `backupEmulator()` game scan section  
Code: `// Scan for games (simplified - in real app, user would select games)` — hardcodes a dummy `GameInfo("Example Game", ...)` instead of reading from `GamingBackupManager.detectGames()`.

**S1-ST-12** — `app/src/main/java/com/obsidianbackup/cloud/GoogleDriveProvider.kt` lines 476–490  
Function: `listSnapshots()` snapshot ID extraction  
Code: `// Extract snapshots (simplified)` — uses a fragile regex on raw JSON instead of the Drive API's structured file list response.

**S1-ST-13** — `app/src/main/java/com/obsidianbackup/cloud/GoogleDriveProvider.kt` lines 542–550  
Function: `downloadFile()` file lookup  
Code: `// Find file by name (simplified - should use proper path resolution)` — searches by filename only, which is ambiguous on Google Drive where multiple files can have the same name.

**S1-ST-14** — `app/src/main/java/com/obsidianbackup/cloud/providers/OracleCloudProvider.kt` line 737  
Function: `buildAuthorizationHeader()`  
Returns: `val signatureBase64 = "PLACEHOLDER_SIGNATURE"` — OCI REST auth is never actually signed.  
Code: `// Would use actual private key signing`  
Should do: Load the OCI private key PEM and sign the request string with RSA-SHA256.

---

### 3. TODOs

No `// TODO` or `// FIXME` comments were found in `app/src/main/**/*.kt` production source files. (Grep returned no results.)

---

### 4. Dead Code

**S1-DC-01** — `app/src/main/java/com/obsidianbackup/tv/ui/BackupDetailsFragment.kt` lines 60–62 *(in TV module — see Section 3)*  
`actionsAdapter.add(Action(ACTION_BACKUP, ...))` / `Action(ACTION_RESTORE, ...)` / `Action(ACTION_DELETE, ...)` are added to the row but no `OnActionClickedListener` is ever registered on `DetailsOverviewRowPresenter`. The buttons render and receive focus but produce no effect when selected.

**S1-DC-02** — `app/src/main/java/com/obsidianbackup/wear/PhoneDataLayerRepository.kt` — `sendBackupProgress()` method  
The method exists and contains `dataClient.putDataItem(...)` to push progress to the watch, but it is **never called** from anywhere in the app module — not from `BackupWorker`, `BackupOrchestrator`, `BackupAppsUseCase`, or any ViewModel. As a result, the watch's `ProgressScreen` always shows 0% because no progress data is ever written to the Data Layer during an actual backup run.

---

### 5. Disconnected Code

**S1-DIS-01** — `app/src/main/java/com/obsidianbackup/accessibility/VoiceControlHandler.kt`  
What it does: Full `SpeechRecognizer`-based voice command handler (`@Singleton`, `@Inject`), translating commands like "backup my apps" or "restore" into app actions.  
Problem: The Hilt module (`AccessibilityModule.kt`) notes it is handled automatically but it is **never injected into any ViewModel, Fragment, or Activity** in the production codebase (only instantiated directly in `AccessibilityTest`).  
Where to connect: Should be injected into `MainActivity` or `SimplifiedModeViewModel`, with `initialize()` and `startListening()` called on user opt-in.

**S1-DIS-02** — `app/src/main/java/com/obsidianbackup/crypto/PQCBenchmark.kt`  
What it does: Benchmarking harness for post-quantum algorithm performance metrics.  
Problem: Never referenced from any live code path — no ViewModel, screen, or DI binding mentions it.  
Where to connect: Should be wired into a developer/diagnostic screen or a feature-flagged benchmark button in the ZeroKnowledge or Feature Flags screen.

**S1-DIS-03** — `app/src/main/java/com/obsidianbackup/wear/PhoneDataLayerRepository.kt::sendBackupProgress()`  
(Also listed under Dead Code S1-DC-02.) The method has a complete implementation calling `dataClient.putDataItem()` but is never called from any backup execution path.

---

### 6. Placeholder Values

**S1-PH-01** — `app/src/main/java/com/obsidianbackup/cloud/providers/OracleCloudProvider.kt` line 737  
Placeholder: `val signatureBase64 = "PLACEHOLDER_SIGNATURE"`  
Needed: Real RSA-SHA256 signature of the OCI signing string using the configured private key.

**S1-PH-02** — `app/src/main/java/com/obsidianbackup/security/CertificatePinningManager.kt` lines 60–64  
Placeholder:  
```
"webdav.example.com" to listOf(
    // "sha256/YOUR_WEBDAV_PIN_HERE"
)
```
Needed: Real SHA-256 certificate pins for any WebDAV servers the app connects to, or removal of the example domain.

---

### 7. Simplified Implementations

**S1-SIM-01** — `app/src/main/java/com/obsidianbackup/crypto/PostQuantumCrypto.kt` lines 480–586 (`MockPQCProvider` inner class)  
Shortcut: All five post-quantum operations (`generateKEMKeyPair`, `encapsulate`, `decapsulate`, `generateSignatureKeyPair`, `sign`, `verify`) use a `MockPQCProvider` that generates random bytes, logs `(NOT SECURE)`, and in `verify()` always returns `true` if the signature length is correct.  
Code: `// Mock implementation: Always return true for valid format` — signature verification always passes.  
Should use: A real ML-KEM/ML-DSA implementation via Bouncy Castle's PQC provider or the Android Security Library when Android platform support lands.

**S1-SIM-02** — `app/src/main/java/com/obsidianbackup/cloud/WebDavCloudProvider.kt` lines 878, 894, 909, 920  
Shortcut: Manual string-building and regex-based JSON parsing used for catalog serialization.  
Code: `// Simple JSON serialization - in production use kotlinx.serialization`  
Should use: `kotlinx.serialization` (already a dependency) for type-safe JSON encoding/decoding.

**S1-SIM-03** — `app/src/main/java/com/obsidianbackup/plugins/discovery/PackagePluginDiscovery.kt` line 57  
Shortcut: Plugin APK metadata discovery uses `getPackageArchiveInfo` which is brittle on Android 13+ (returns null for cross-package APKs without additional flags).  
Code: `// This is a simplified implementation - in practice, you'd use PackageManager`  
Should use: Full `GET_SIGNATURES` + `GET_META_DATA` flags and signature chain validation.

**S1-SIM-04** — `app/src/main/java/com/obsidianbackup/plugins/discovery/PluginValidator.kt` line 128  
Code: `// This is a simplified check - in practice, you'd have more sophisticated validation`  
The signature hash check only compares the SHA-256 of the first signing certificate's DER bytes against a stored hash. It does not verify the certificate chain or check for certificate revocation.

**S1-SIM-05** — `app/src/main/java/com/obsidianbackup/tasker/TaskerIntegration.kt` line 420  
Code: `// This is a simplified implementation` — `getCallingPackage()` returns `"unknown"` always, which means the Tasker security validator always gets `"unknown"` as the caller, bypassing package-based allow-listing logic in release builds.

**S1-SIM-06** — `app/src/main/java/com/obsidianbackup/cloud/CloudSyncManager.kt` line 217  
Code: `// Export catalog with integrity signatures - simplified implementation`  
The catalog sync omits the integrity signature step, meaning the cloud-synced catalog has no tamper detection.

---

## SECTION 2 — WEAR MODULE

### 1. Feature Coming Soon
No "Coming Soon" dialogs or `ComingSoonMessage` composables found in `wear/src/`.

### 2. Stubs
No stub functions returning hardcoded failure values found in `wear/src/`.

### 3. TODOs
No `// TODO` or `// FIXME` comments found in `wear/src/main/`.

### 4. Dead Code

**S2-DC-01** — `wear/src/main/java/com/obsidianbackup/wear/tiles/BackupTileService.kt` line 88  
Code: `.setClassName("com.obsidianbackup.MainActivity")`  
The "Backup Now" tile button is configured to launch `com.obsidianbackup.MainActivity` with `context.packageName` (which is `com.obsidianbackup.wear`). No class named `com.obsidianbackup.MainActivity` exists in the wear module — the wear app's main activity is `com.obsidianbackup.wear.presentation.MainActivity`. This intent will silently fail to launch any screen.

**S2-DC-02** — The tile button click ID `"backup_trigger"` is defined but the `TileService` has no `onTileInteraction()` override to handle it. The tile sends no `MessageClient.sendMessage()` on click — it only attempts (and fails) to launch an Activity. The watch backup trigger from the tile is completely non-functional.

### 5. Disconnected Code
No fully-implemented classes in `wear/src/` that are completely unreferenced from live code paths.

### 6. Placeholder Values
No placeholder API keys or dummy credentials found in `wear/src/`.

### 7. Simplified Implementations
No `// simplified` or `// in production use` comments found in `wear/src/`.

---

### 8. Unwired from App (Wear-Specific)

**S2-W-01 — `BACKUP_PROGRESS_PATH` write: UNWIRED**  
The wear module's `DataLayerListenerService` listens on `/backup_progress` (line `DataLayerPaths.BACKUP_PROGRESS_PATH`) and `DataLayerRepository.updateBackupProgress()` accepts updates from the phone. However, `PhoneDataLayerRepository.sendBackupProgress()` in the app module is **never called** from `BackupWorker`, `BackupOrchestrator`, or any UseCase. Result: `ProgressScreen` in the watch always shows 0%, 0 files, empty status.

**S2-W-02 — `SETTINGS_PATH` write: UNWIRED**  
`DataLayerListenerService` handles `DataLayerPaths.SETTINGS_PATH` in `onDataChanged()` and calls `dataLayerRepository.updateSettings()`. No corresponding `DataClient.putDataItem()` on path `/settings` exists anywhere in the app module. Wear settings are never pushed from the phone.

**S2-W-03 — Capability declaration: WIRED (OK)**  
`app/src/main/res/values/wear.xml` declares `obsidian_backup_phone_app` capability, matching `DataLayerPaths.CAPABILITY_PHONE_APP` in the wear module. Discovery by the watch will succeed.

**S2-W-04 — Backup trigger → `BackupWorker`: WIRED (OK)**  
`PhoneDataLayerListenerService.onMessageReceived()` correctly enqueues a `BackupWorker` when it receives the `/backup_trigger` message from the watch. This path works end-to-end.

**S2-W-05 — Status response: PARTIALLY WIRED**  
`handleStatusRequest()` in `PhoneDataLayerListenerService` calls `backupCatalog.getAllBackupsSync()` and `sendBackupStatus()`. The status path is functional for historical data, but `isRunning` is always `false` because no in-progress status is pushed during active backup execution.

---

### 9. Missing App-Side Wear Integration

**S2-MI-01** — `app/src/main/AndroidManifest.xml` registers `PhoneDataLayerListenerService` ✅ (line 283), but the `<intent-filter>` uses `android:scheme="wear"` which is correct for legacy Wearable API but should also include the newer `com.google.android.gms.wearable.*` action filters. On modern Wearable API (3.x), the service may not receive data events without the `DATA_CHANGED` / `MESSAGE_RECEIVED` actions — these ARE present in the filter, so this is functionally OK.

**S2-MI-02** — **No ViewModels or Repositories in the app module call `sendBackupProgress()`** during backup execution. The `BackupWorker`, `AppsViewModel`, `BackupViewModel`, and `BackupOrchestrator` have zero references to `PhoneDataLayerRepository`. The progress feed to the watch is completely absent from the backup execution path.

---

### 10. Missing Wear UI Screens

| Screen | Exists? | Notes |
|--------|---------|-------|
| Main backup control (`BackupScreen`) | ✅ Yes | Full Compose implementation |
| Progress screen (`ProgressScreen`) | ✅ Yes | Full Compose implementation; data never arrives (see S2-W-01) |
| Status screen (`StatusScreen`) | ✅ Yes | Full Compose implementation |
| Quick settings tile | ✅ Yes | Exists but has wrong class name (S2-DC-01) |
| Complication data source | ✅ Yes | 4 complication types implemented |
| App selection / per-app backup screen | ❌ No | No way to choose which apps to back up from the watch |
| Restore screen | ❌ No | No way to trigger or monitor a restore from the watch |
| Settings screen | ❌ No | Watch cannot view or change any app settings |
| Connectivity error screen | ❌ No | "Phone not connected" is inline text in BackupScreen only |

---

## SECTION 3 — TV MODULE

### 1. Feature Coming Soon
No "Coming Soon" dialogs found in `tv/src/`.

### 2. Stubs

**S3-ST-01** — `tv/src/main/java/com/obsidianbackup/tv/backup/TVBackupManager.kt` lines 191–193  
Function: `startBackup()`  
Body: `// Implement backup logic` / `// This would integrate with the main app's backup engine`  
Returns: `Unit` (empty body — no backup is started).  
Should do: Enqueue a `BackupWorker` via WorkManager or invoke the app module's backup service via IPC.

**S3-ST-02** — `tv/src/main/java/com/obsidianbackup/tv/ui/BackupDetailsFragment.kt` lines 73–74  
Function: `setupRelatedContent()` backup history  
Code: Hardcodes two fake entries — `BackupItem("Full Backup", "2024-01-15 14:30", "125 MB")` and `BackupItem("Full Backup", "2024-01-10 10:15", "120 MB")`.  
Should do: Query a real data source (ContentProvider, shared Room DB, or IPC to app module) for actual backup history.

**S3-ST-03** — `tv/src/main/java/com/obsidianbackup/tv/ui/BackupDetailsFragment.kt` lines 60–62 (also S1-DC-01)  
Actions "Backup Now", "Restore", "Delete Backup" are added to `actionsAdapter` but **no `OnActionClickedListener` is registered** on the `DetailsOverviewRowPresenter`. All three buttons are dead UI — pressing D-pad select on them does nothing.

### 3. TODOs
No `// TODO` or `// FIXME` comments found in `tv/src/main/`.

### 4. Dead Code

**S3-DC-01** — `tv/src/main/java/com/obsidianbackup/tv/navigation/TVNavigationHandler.kt`  
The `handleKeyEvent()` function handles `KEYCODE_DPAD_*`, `KEYCODE_ENTER`, `KEYCODE_BACK`, `KEYCODE_MENU`, and media keys — but `TVNavigationHandler` is **never referenced from any Activity or Fragment** in the TV module. No TV class imports or calls it. It is entirely unreachable at runtime.

**S3-DC-02** — `tv/src/main/java/com/obsidianbackup/tv/ui/BackupDetailsFragment.kt` — Action constants `ACTION_BACKUP`, `ACTION_RESTORE`, `ACTION_DELETE` are defined but no `setOnActionClickedListener` is registered. The Leanback framework requires registering a listener on `DetailsOverviewRowPresenter` to receive action click callbacks; without it the actions silently do nothing.

### 5. Disconnected Code

**S3-DIS-01** — `tv/src/main/java/com/obsidianbackup/tv/navigation/TVNavigationHandler.kt`  
Full implementation, never called. (Also S3-DC-01.)  
Where to connect: `MainActivity.onKeyDown()` and `MainFragment.onKeyDown()` should delegate to `TVNavigationHandler.handleKeyEvent()`.

**S3-DIS-02** — `tv/src/main/java/com/obsidianbackup/tv/settings/TVSettingsManager.kt`  
Fully implemented DataStore-backed settings manager with flows for `autoBackup`, `backupFrequency`, `includeData`, `cloudProvider`, `compression`, `encryption`.  
Problem: **Never injected into any Fragment, Activity, or ViewModel** in the TV module. `SettingsFragment` uses `LeanbackPreferenceFragmentCompat` and loads from `R.xml.tv_preferences` directly, completely bypassing `TVSettingsManager`.  
Where to connect: `SettingsFragment.PrefsFragment` `onPreferenceChangeListener` callbacks should call `TVSettingsManager` suspending setters.

### 6. Placeholder Values
No placeholder API keys or dummy credentials found in `tv/src/`.

### 7. Simplified Implementations
No `// simplified` or `// in production use` comments found in `tv/src/`.

---

### 11. Scaffold vs Implementation

| Class | Implementation Status |
|-------|-----------------------|
| `MainActivity` | Real scaffold: inflates layout, attaches `MainFragment`. Minimal but functional. |
| `MainFragment` | **Real implementation**: Builds full `BrowseSupportFragment` row set (Dashboard, TV Apps, Streaming, Games, Settings), queries `TVBackupManager`, handles clicks. Functional UI. |
| `AppSelectionActivity` | Real scaffold: inflates layout, attaches `AppSelectionFragment`. Minimal but functional. |
| `AppSelectionFragment` | **Real implementation**: `VerticalGridSupportFragment` with 4-column grid, loads all TV apps, handles selection toggle. Functional UI. |
| `BackupDetailsActivity` | Real scaffold: inflates layout, passes extras to `BackupDetailsFragment`. Minimal but functional. |
| `BackupDetailsFragment` | **Scaffold with dead actions**: Row, image, action buttons implemented — but action click listener missing and backup history is hardcoded (see S3-ST-02, S3-DC-02). |
| `SettingsActivity` | Real scaffold: inflates layout, attaches `SettingsFragment`. Minimal but functional. |
| `SettingsFragment` | Real implementation: Leanback preference fragment loading from XML. Functional UI. Settings changes do NOT persist via `TVSettingsManager` (disconnected — see S3-DIS-02). |
| `TVBackupManager` | **Partially real**: App scanning and categorization are real using `PackageManager`. `startBackup()` is an empty stub (see S3-ST-01). |
| `TVNavigationHandler` | **Disconnected scaffold**: Implements all key handling logic but is never called (see S3-DC-01). |
| `TVSettingsManager` | **Disconnected real implementation**: Full DataStore persistence, never used (see S3-DIS-02). |
| `CardPresenters` (`DashboardCardPresenter`, `TVAppCardPresenter`, `SettingsCardPresenter`) | **Real implementations**: Full Leanback `Presenter` binding with focus animations. |

---

### 12. Missing TV Data Layer

The TV module has **no mechanism to access the main app's backup data**. Specifically:

- **No ContentProvider** registered in either `tv/AndroidManifest.xml` or `app/AndroidManifest.xml` for cross-module backup data access.
- **No shared Room database**: `TVBackupManager` uses the TV module's own `context.getExternalFilesDir(null)` to check `isAppBackedUp()` — it looks for backup folders written by the main app but has no dependency on the main app's `BackupCatalog` or Room DAO.
- **No API calls**: No HTTP client to a local/remote backup service.
- **No IPC**: No AIDL, Messenger, or Binder service connecting TV to app.
- **No Nearby Connections, no Bluetooth, no local socket**: No companion protocol of any kind.
- **Result**: `isAppBackedUp()` only works if the TV is running on the same device as the phone app AND the main app writes its backups to the same external files directory path the TV module assumes — an untested assumption. `TVBackupManager.startBackup()` is an empty stub and cannot call the main app's engine at all.

---

### 13. Unwired from App (TV-Specific)

**S3-MI-01** — `app/src/main/AndroidManifest.xml` contains **no reference to any TV module package**, activity, ContentProvider, or service. The TV module package (`com.obsidianbackup.tv`) does not appear anywhere in app-module source files or manifests.

**S3-MI-02** — No ContentProvider is registered in the app module's manifest for the TV module to query backup catalog data.

**S3-MI-03** — No `NearbyConnections`, `CompanionDeviceManager`, or local socket server exists in the app module to serve backup data to the TV module.

**S3-MI-04** — The app module NavGraph (`NavigationHost.kt`) has no reference to the TV module package or any cross-module intent. The two modules are completely isolated at the IPC layer.

---

## SECTION 4 — MASTER SUMMARY TABLE

| ID | Module | File | Issue Type | Description | Severity |
|----|--------|------|------------|-------------|----------|
| S1-CS-01 | app | `ui/screens/GamingScreen.kt:64` | Coming Soon | Entire Gaming feature behind ComingSoonMessage when feature flag off | High |
| S1-CS-02 | app | `ui/screens/PluginsScreen.kt:107` | Coming Soon | Plugin Store button shows Toast instead of marketplace | High |
| S1-CS-03 | app | `ui/screens/OtherScreens.kt:179` | Coming Soon | OAuth flow button shows Coming Soon Toast | High |
| S1-CS-04 | app | `ui/screens/SettingsScreen.kt:108` | Coming Soon | Compression Profile → Coming Soon dialog | Medium |
| S1-CS-05 | app | `ui/screens/SettingsScreen.kt:195` | Coming Soon | Sync Policies → Coming Soon dialog | Medium |
| S1-CS-06 | app | `ui/screens/SettingsScreen.kt:214` | Coming Soon | Play Games Cloud Sync → Coming Soon dialog | Medium |
| S1-CS-07 | app | `ui/screens/SettingsScreen.kt:233` | Coming Soon | Privacy Settings → Coming Soon dialog | Medium |
| S1-CS-08 | app | `ui/screens/SettingsScreen.kt:280` | Coming Soon | Plugin Security → Coming Soon dialog | Medium |
| S1-CS-09 | app | `ui/screens/SettingsScreen.kt:291` | Coming Soon | Retention Policies → Coming Soon dialog | High |
| S1-CS-10 | app | `ui/screens/SettingsScreen.kt:299` | Coming Soon | Storage Limits → Coming Soon dialog | Medium |
| S1-CS-11 | app | `ui/screens/SettingsScreen.kt:310` | Coming Soon | Permission Mode selector → Coming Soon dialog | High |
| S1-CS-12 | app | `ui/screens/SettingsScreen.kt:393` | ✅ RESOLVED | BusyBox Options → fully implemented, 342 applets verified | — |
| S1-CS-13 | app | `ui/screens/SettingsScreen.kt:401` | Coming Soon | Export Diagnostics → Coming Soon dialog | Medium |
| S1-CS-14 | app | `ui/screens/SettingsScreen.kt:409` | Coming Soon | Export App Logs → Coming Soon dialog | Medium |
| S1-CS-15 | app | `ui/screens/SettingsScreen.kt:417` | Coming Soon | Export Shell Audit Logs → Coming Soon dialog | Medium |
| S1-ST-01 | app | `billing/SubscriptionManager.kt:240` | Stub | `getSubscriptionHistory()` returns `emptyList()` always | Medium |
| S1-ST-02 | app | `cloud/providers/PCloudProvider.kt:115` | Stub | `listSnapshots()` returns Error("not yet implemented") | High |
| S1-ST-03 | app | `cloud/providers/FtpCloudProvider.kt:113` | Stub | `listSnapshots()` returns Error("not yet implemented") | High |
| S1-ST-04 | app | `cloud/providers/FtpCloudProvider.kt:132` | Stub | `uploadFile()` returns Success without transferring any bytes | Critical |
| S1-ST-05 | app | `cloud/providers/FtpCloudProvider.kt:144` | Stub | `downloadFile()` returns Success without downloading any bytes | Critical |
| S1-ST-06 | app | `cloud/providers/SmbCloudProvider.kt:122` | Stub | `uploadFile()`/`downloadFile()` return Success without SMB transfer | Critical |
| S1-ST-07 | app | `cloud/providers/MegaCloudProvider.kt:114` | Stub | `listSnapshots()` returns `emptyList()` — no MEGA listing | High |
| S1-ST-08 | app | `gaming/GamingBackupManager.kt:324` | Stub | `restoreGameSaves()` has empty body | High |
| S1-ST-09 | app | `cloud/oauth/OAuth2Provider.kt:225` | Stub | `listAccounts()` always returns `listOf("default")` | Medium |
| S1-ST-10 | app | `tasker/TaskerIntegration.kt:420` | Stub | `getCallingPackage()` always returns `"unknown"` | High |
| S1-ST-11 | app | `presentation/gaming/GamingBackupViewModel.kt:47` | Stub | Hardcodes `GameInfo("Example Game")` instead of scanning real games | High |
| S1-ST-12 | app | `cloud/GoogleDriveProvider.kt:479` | Simplified | Snapshot list extraction uses fragile regex on raw JSON | Medium |
| S1-ST-13 | app | `cloud/GoogleDriveProvider.kt:545` | Simplified | File lookup by name only — ambiguous on Drive | Medium |
| S1-ST-14 | app | `cloud/providers/OracleCloudProvider.kt:737` | Placeholder | `PLACEHOLDER_SIGNATURE` — OCI requests are never signed | Critical |
| S1-DC-01 | app | `wear/PhoneDataLayerRepository.kt:78` | Dead Code | `sendBackupProgress()` exists but is never called; watch progress always 0% | High |
| S1-DIS-01 | app | `accessibility/VoiceControlHandler.kt` | Disconnected | Full voice command handler, never injected into any live screen | Medium |
| S1-DIS-02 | app | `crypto/PQCBenchmark.kt` | Disconnected | PQC benchmark class, never referenced | Medium |
| S1-PH-01 | app | `cloud/providers/OracleCloudProvider.kt:737` | Placeholder | `"PLACEHOLDER_SIGNATURE"` literal in OCI auth header | Critical |
| S1-PH-02 | app | `security/CertificatePinningManager.kt:60` | Placeholder | `"webdav.example.com"` with commented-out pin | Medium |
| S1-SIM-01 | app | `crypto/PostQuantumCrypto.kt:480–586` | Simplified | MockPQCProvider — all PQC operations are random bytes; verify() always true | Critical |
| S1-SIM-02 | app | `cloud/WebDavCloudProvider.kt:878,894,909,920` | Simplified | Manual string JSON instead of kotlinx.serialization | Medium |
| S1-SIM-03 | app | `plugins/discovery/PackagePluginDiscovery.kt:57` | Simplified | Brittle APK metadata extraction on Android 13+ | Medium |
| S1-SIM-04 | app | `plugins/discovery/PluginValidator.kt:128` | Simplified | No certificate chain or revocation check on plugin signatures | High |
| S1-SIM-05 | app | `tasker/TaskerIntegration.kt:420` | Simplified | Caller identification always returns "unknown" | High |
| S1-SIM-06 | app | `cloud/CloudSyncManager.kt:217` | Simplified | Catalog sync omits integrity signature step | High |
| S2-DC-01 | wear | `wear/tiles/BackupTileService.kt:88` | Dead Code | Tile button launches `com.obsidianbackup.MainActivity` (does not exist in wear package) — intent fails silently | Critical |
| S2-DC-02 | wear | `wear/tiles/BackupTileService.kt` | Dead Code | Tile click ID `backup_trigger` has no `onTileInteraction()` handler; no message sent to phone | Critical |
| S2-W-01 | wear | `wear/data/DataLayerListenerService.kt` | Unwired | `/backup_progress` writes never happen in app module; watch progress always zero | High |
| S2-W-02 | wear | `wear/data/DataLayerListenerService.kt` | Unwired | `/settings` writes never happen in app module; watch settings never synced | Medium |
| S2-MI-02 | app | `wear/PhoneDataLayerRepository.kt` | Missing Integration | BackupWorker/Orchestrator never call `sendBackupProgress()` or `sendBackupStatus()` during active backup | High |
| S3-ST-01 | tv | `tv/backup/TVBackupManager.kt:191` | Stub | `startBackup()` is an empty body — no backup is ever started from TV | Critical |
| S3-ST-02 | tv | `tv/ui/BackupDetailsFragment.kt:73` | Stub | Hardcoded backup history "2024-01-15 14:30 / 125 MB" — not real data | High |
| S3-ST-03 | tv | `tv/ui/BackupDetailsFragment.kt:60` | Dead Code | Backup/Restore/Delete action buttons have no `OnActionClickedListener` — all dead | Critical |
| S3-DC-01 | tv | `tv/navigation/TVNavigationHandler.kt` | Disconnected | Full key-event handler object never called from any Activity or Fragment | Medium |
| S3-DIS-01 | tv | `tv/navigation/TVNavigationHandler.kt` | Disconnected | Never imported or invoked — all key routing falls to framework defaults | Medium |
| S3-DIS-02 | tv | `tv/settings/TVSettingsManager.kt` | Disconnected | DataStore settings manager never injected; SettingsFragment bypasses it | Medium |
| S3-MI-01 | tv | App manifest / app source | Missing Integration | App module has zero references to TV module package; no IPC, no ContentProvider | Critical |
| S3-MI-02 | tv | App manifest | Missing Integration | No ContentProvider registered for TV to query backup catalog | Critical |
| S3-MI-03 | tv | TV module | Missing Data Layer | No connection protocol (IPC/Nearby/socket) from TV to app module backup data | Critical |

---

## SECTION 5 — PRIORITY FIX ORDER

### 🔴 Fix Immediately (prevents functional use or causes silent failure on launch)

1. **S2-DC-01 / S2-DC-02** — Wear tile "Backup Now" button launches a non-existent class and has no message handler. Fix `className` to `"com.obsidianbackup.wear.presentation.MainActivity"` and add `onTileInteraction()` to send a `MessageClient.sendMessage()` backup trigger.
2. **S1-ST-04 / S1-ST-05** — FTP `uploadFile()`/`downloadFile()` return `Success` without performing any I/O. Any user who configures FTP backup will silently lose data with no error.
3. **S1-ST-06** — SMB `uploadFile()`/`downloadFile()` same problem as FTP.
4. **S3-ST-01** — TV `startBackup()` is an empty body. A TV user pressing "Backup Now" triggers nothing.
5. **S3-ST-03** — TV detail view Backup/Restore/Delete buttons produce no action (missing `OnActionClickedListener`).
6. **S1-PH-01 / S1-ST-14** — OCI `PLACEHOLDER_SIGNATURE` means Oracle Cloud Storage is completely broken at the auth layer.
7. **S1-SIM-01** — `MockPQCProvider.verify()` always returns `true` if signature bytes are the right length. Any code relying on PQC signature verification for security makes no real trust decision.

---

### 🟠 Fix This Sprint (high-impact, low-effort quick wins)

8. **S2-MI-02 / S2-W-01** — Wire `sendBackupProgress()` into `BackupWorker` or `BackupOrchestrator` so the watch's ProgressScreen actually shows live data. Low code change, high user-visible impact.
9. **S2-W-02** — Push app settings to watch via `/settings` DataItem path.
10. **S3-ST-02** — Replace hardcoded TV backup history with a real data query (even just reading from external files if no IPC yet).
11. **S1-ST-10 / S1-SIM-05** — `getCallingPackage()` returning `"unknown"` means Tasker security is ineffective in release; fix with `Binder.getCallingUid()` + `PackageManager.getNameForUid()`.
12. **S1-SIM-06** — Add catalog integrity signature to `CloudSyncManager.exportCatalog()`.
13. **S3-DC-01 / S3-DIS-01** — Wire `TVNavigationHandler.handleKeyEvent()` into `MainActivity.onKeyDown()`.
14. **S3-DIS-02** — Inject `TVSettingsManager` into `SettingsFragment` preference listeners.
15. **S1-ST-09** — `OAuth2Provider.listAccounts()` returning hardcoded `"default"` will cause UI to show one account when none or multiple are configured.

---

### 🟡 Fix Before v1 Launch (core functionality gaps)

16. **S1-CS-01** — Gaming Backups "Coming Soon" gate — implement or remove from nav. The feature flag `GAMING_BACKUP` exists; the ViewModel and Manager exist — the gate just needs to be turned on.
17. **S1-CS-02** — Plugin Store button — implement or hide; showing a Toast for a core plugin system feature is unacceptable for launch.
18. **S1-CS-03** — OAuth flow button — wire to real `OAuth2Manager` flow.
19. **S1-CS-09 / S1-CS-10** — Retention Policies and Storage Limits — both have Manager infrastructure; need screens.
20. **S1-CS-11** — Permission Mode selector — the `PermissionManager.detectBestMode()` exists; need a radio-button preference screen.
21. **S1-CS-13 / S1-CS-14 / S1-CS-15** — Export Diagnostics/Logs/Audit — wire to file export and share intent.
22. **S1-ST-02 / S1-ST-03** — PCloud and FTP `listSnapshots()` returning error — users cannot browse or restore from these providers.
23. **S1-ST-07** — MEGA `listSnapshots()` returning `emptyList()` — silent data loss on restore UI.
24. **S1-ST-08** — `GamingBackupManager.restoreGameSaves()` empty body — gaming restore is non-functional.
25. **S1-ST-11** — `GamingBackupViewModel.backupEmulator()` hardcodes `"Example Game"` — gaming backup is non-functional.
26. **S1-SIM-01** — MockPQCProvider must be replaced or feature-flagged out; PQC claims must not ship as mock.
27. **S3-MI-01 / S3-MI-02 / S3-MI-03** — TV module data layer entirely absent. Must define an IPC mechanism (shared ContentProvider or local broadcast) before TV is considered functional.

---

### ⚪ Post-Launch Backlog (polish and completeness)

28. **S1-CS-04 / S1-CS-05 / S1-CS-06 / S1-CS-07 / S1-CS-08** — Remaining Settings Coming Soon items (Compression Profile, Sync Policies, Play Games, Privacy, Plugin Security). ~~S1-CS-12 (BusyBox Options) resolved 2026-02-22.~~
29. **S1-ST-01** — `getSubscriptionHistory()` returning `emptyList()` — implement real purchase history.
30. **S1-DIS-01** — Wire `VoiceControlHandler` into `SimplifiedModeScreen` for accessibility.
31. **S1-DIS-02** — Surface `PQCBenchmark` in a developer diagnostic screen.
32. **S1-SIM-02** — Replace WebDAV manual JSON with `kotlinx.serialization`.
33. **S1-SIM-03 / S1-SIM-04** — Improve plugin APK validation on Android 13+ and add certificate chain checking.
34. **S1-PH-02** — Replace `webdav.example.com` placeholder certificate pins with real pins.
35. **S2-missing** — Add Wear OS app selection screen, restore screen, and settings screen.
36. **S3-missing** — Add per-app backup capability to TV via proper data layer connection to main app.

