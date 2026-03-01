# COMPLETE FILE TREE - ObsidianBackup Project
**Generated**: 2026-02-09 00:57:38

---

## 📊 Project Statistics

| Metric | Count |
|--------|-------|
| **Kotlin Files** | 521 |
| **Total LOC (Kotlin)** | 82889 |
| **XML Files** | 360 |
| **Markdown Files** | 197 |
| **Gradle Files** | 8 |
| **Test Files** | 66 |
| **Packages** | 85+ |


## 🌳 Complete Directory Structure with Descriptions

### Root Directory

```
ObsidianBackup/
├── app/                                    # Main Android application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/obsidianbackup/
│   │   │   │   ├── accessibility/          # Accessibility features (3 files, ~424 LOC)
│   │   │   │   │   ├── AccessibilityHelper.kt              [138 lines, 5.2KB] - Accessibility utilities
│   │   │   │   │   ├── SimplifiedModeViewModel.kt          [156 lines, 6.1KB] - Simplified UI view model
│   │   │   │   │   └── VoiceControlHandler.kt              [130 lines, 5.0KB] - Voice command handler
│   │   │   │   │
│   │   │   │   ├── api/plugin/             # Plugin API interfaces (6 files, ~681 LOC)
│   │   │   │   │   ├── AutomationPlugin.kt                 [125 lines, 4.8KB] - Automation plugin interface
│   │   │   │   │   ├── BackupEnginePlugin.kt               [112 lines, 4.3KB] - Backup engine plugin API
│   │   │   │   │   ├── CloudProviderPlugin.kt              [138 lines, 5.3KB] - Cloud provider plugin API
│   │   │   │   │   ├── ExportPlugin.kt                     [95 lines, 3.7KB] - Export plugin interface
│   │   │   │   │   ├── PluginApi.kt                        [126 lines, 4.9KB] - Core plugin API
│   │   │   │   │   └── PluginCapability.kt                 [85 lines, 3.3KB] - Plugin capability definitions
│   │   │   │   │
│   │   │   │   ├── automation/             # Backup automation (1 file, ~205 LOC)
│   │   │   │   │   └── BackupScheduler.kt                  [205 lines, 8.0KB] - Automated backup scheduling
│   │   │   │   │
│   │   │   │   ├── billing/                # Monetization & subscriptions (7 files, ~1158 LOC)
│   │   │   │   │   ├── BillingManager.kt                   [182 lines, 7.1KB] - Billing coordination
│   │   │   │   │   ├── BillingModels.kt                    [143 lines, 5.5KB] - Billing data models
│   │   │   │   │   ├── BillingRepository.kt                [127 lines, 4.9KB] - Billing data access
│   │   │   │   │   ├── FeatureGateService.kt               [156 lines, 6.0KB] - Feature access control
│   │   │   │   │   ├── ProFeatureGate.kt                   [134 lines, 5.2KB] - Pro feature gating
│   │   │   │   │   ├── RevenueAnalytics.kt                 [198 lines, 7.7KB] - Revenue tracking
│   │   │   │   │   ├── SubscriptionManager.kt              [218 lines, 8.5KB] - Subscription management
│   │   │   │   │   └── di/BillingModule.kt                 [64 lines, 2.5KB] - DI module
│   │   │   │   │
│   │   │   │   ├── cloud/                  # Cloud storage integration (8 files, ~2913 LOC)
│   │   │   │   │   ├── CloudProvider.kt                    [342 lines, 13.3KB] - Cloud provider interface
│   │   │   │   │   ├── CloudSyncManager.kt                 [456 lines, 17.8KB] - Cloud sync orchestration
│   │   │   │   │   ├── CloudSyncRepository.kt              [287 lines, 11.2KB] - Cloud sync data layer
│   │   │   │   │   ├── ConflictResolver.kt                 [324 lines, 12.6KB] - Sync conflict resolution
│   │   │   │   │   ├── FilecoinCloudProvider.kt            [893 lines, 34.8KB] - Filecoin integration
│   │   │   │   │   ├── GoogleDriveProvider.kt              [345 lines, 13.4KB] - Google Drive provider
│   │   │   │   │   ├── OAuth2Manager.kt                    [192 lines, 7.5KB] - OAuth authentication
│   │   │   │   │   ├── WebDavCloudProvider.kt              [704 lines, 27.4KB] - WebDAV provider
│   │   │   │   │   ├── oauth/OAuth2Provider.kt             [274 lines, 10.7KB] - OAuth provider abstraction
│   │   │   │   │   │
│   │   │   │   │   └── providers/          # Additional cloud providers (6 files, ~4725 LOC)
│   │   │   │   │       ├── AlibabaOSSProvider.kt           [798 lines, 31.1KB] - Alibaba Cloud OSS
│   │   │   │   │       ├── AzureBlobProvider.kt            [812 lines, 31.6KB] - Azure Blob Storage
│   │   │   │   │       ├── BackblazeB2Provider.kt          [934 lines, 36.4KB] - Backblaze B2
│   │   │   │   │       ├── BoxCloudProvider.kt             [787 lines, 30.7KB] - Box.com provider
│   │   │   │   │       ├── DigitalOceanSpacesProvider.kt   [801 lines, 31.2KB] - DigitalOcean Spaces
│   │   │   │   │       └── OracleCloudProvider.kt          [793 lines, 31.5KB] - Oracle Cloud Storage
│   │   │   │   │
│   │   │   │   ├── cloud/rclone/           # Rclone integration (4 files, ~1083 LOC)
│   │   │   │   │   ├── RcloneCloudProvider.kt              [445 lines, 17.3KB] - Rclone provider wrapper
│   │   │   │   │   ├── RcloneConfigManager.kt              [267 lines, 10.4KB] - Rclone config management
│   │   │   │   │   ├── RcloneExecutor.kt                   [289 lines, 11.2KB] - Rclone command executor
│   │   │   │   │   ├── RcloneProviderFactory.kt            [82 lines, 3.2KB] - Rclone provider factory
│   │   │   │   │   └── backends/           # Rclone backend implementations (3 files)
│   │   │   │   │       ├── RcloneDropboxProvider.kt        [89 lines, 3.5KB]
│   │   │   │   │       ├── RcloneGoogleDriveProvider.kt    [92 lines, 3.6KB]
│   │   │   │   │       └── RcloneS3Provider.kt             [87 lines, 3.4KB]
│   │   │   │   │
│   │   │   │   ├── community/              # Community features (9 files, ~1072 LOC)
│   │   │   │   │   ├── AnalyticsManager.kt                 [134 lines, 5.2KB] - Analytics tracking
│   │   │   │   │   ├── BetaProgramManager.kt               [98 lines, 3.8KB] - Beta program management
│   │   │   │   │   ├── ChangelogManager.kt                 [112 lines, 4.4KB] - Changelog display
│   │   │   │   │   ├── CommunityForumManager.kt            [127 lines, 4.9KB] - Forum integration
│   │   │   │   │   ├── ConfigSharingManager.kt             [145 lines, 5.6KB] - Config sharing
│   │   │   │   │   ├── CrashlyticsManager.kt               [89 lines, 3.5KB] - Crash reporting
│   │   │   │   │   ├── FeedbackManager.kt                  [156 lines, 6.1KB] - User feedback
│   │   │   │   │   ├── OnboardingManager.kt                [123 lines, 4.8KB] - User onboarding
│   │   │   │   │   └── TipsManager.kt                      [88 lines, 3.4KB] - Tips & tricks
│   │   │   │   │
│   │   │   │   ├── crypto/                 # Security & encryption (7 files, ~3033 LOC)
│   │   │   │   │   ├── EncryptionEngine.kt                 [412 lines, 16.0KB] - Encryption/decryption
│   │   │   │   │   ├── KeystoreManager.kt                  [387 lines, 15.1KB] - Keystore management
│   │   │   │   │   ├── PostQuantumCrypto.kt                [923 lines, 35.9KB] - Post-quantum crypto
│   │   │   │   │   ├── PQCBenchmark.kt                     [245 lines, 9.5KB] - PQC benchmarking
│   │   │   │   │   ├── PrivacyAuditor.kt                   [298 lines, 11.6KB] - Privacy auditing
│   │   │   │   │   ├── ZeroKnowledgeEncryption.kt          [512 lines, 19.9KB] - Zero-knowledge encryption
│   │   │   │   │   └── ZeroKnowledgeManager.kt             [256 lines, 10.0KB] - Zero-knowledge management
│   │   │   │   │
│   │   │   │   ├── data/                   # Data layer
│   │   │   │   │   ├── local/catalog/BackupCatalogFacade.kt [32 lines, 1.2KB]
│   │   │   │   │   └── repository/         # Repositories (3 files, ~119 LOC)
│   │   │   │   │       ├── AppRepository.kt                [38 lines, 1.5KB]
│   │   │   │   │       ├── BackupRepository.kt             [43 lines, 1.7KB]
│   │   │   │   │       └── CatalogRepository.kt            [38 lines, 1.5KB]
│   │   │   │   │
│   │   │   │   ├── deeplink/               # Deep linking (11 files, ~2057 LOC)
│   │   │   │   │   ├── DeepLinkActivity.kt                 [167 lines, 6.5KB] - Deep link activity
│   │   │   │   │   ├── DeepLinkAnalytics.kt                [143 lines, 5.6KB] - Deep link analytics
│   │   │   │   │   ├── DeepLinkAuthenticator.kt            [234 lines, 9.1KB] - Deep link authentication
│   │   │   │   │   ├── DeepLinkGenerator.kt                [189 lines, 7.4KB] - Deep link generation
│   │   │   │   │   ├── DeepLinkHandler.kt                  [278 lines, 10.8KB] - Deep link handling
│   │   │   │   │   ├── DeepLinkIntegration.kt              [156 lines, 6.1KB] - Deep link integration
│   │   │   │   │   ├── DeepLinkModels.kt                   [198 lines, 7.7KB] - Deep link models
│   │   │   │   │   ├── DeepLinkModule.kt                   [67 lines, 2.6KB] - DI module
│   │   │   │   │   ├── DeepLinkParser.kt                   [245 lines, 9.5KB] - URL parsing
│   │   │   │   │   ├── DeepLinkRouter.kt                   [312 lines, 12.1KB] - Routing logic
│   │   │   │   │   └── DeepLinkTestActivity.kt             [68 lines, 2.6KB] - Testing activity
│   │   │   │   │
│   │   │   │   ├── di/                     # Dependency injection (6 files, ~818 LOC)
│   │   │   │   │   ├── AppModule.kt                        [245 lines, 9.5KB] - Core app DI
│   │   │   │   │   ├── CloudModule.kt                      [178 lines, 6.9KB] - Cloud DI
│   │   │   │   │   ├── GamingModule.kt                     [89 lines, 3.5KB] - Gaming DI
│   │   │   │   │   ├── HealthModule.kt                     [92 lines, 3.6KB] - Health DI
│   │   │   │   │   ├── MLModule.kt                         [134 lines, 5.2KB] - ML DI
│   │   │   │   │   └── TaskerModule.kt                     [80 lines, 3.1KB] - Tasker DI
│   │   │   │   │
│   │   │   │   ├── domain/                 # Business logic layer
│   │   │   │   │   ├── backup/             # Backup domain (2 files, ~105 LOC)
│   │   │   │   │   │   ├── BackupEngineFactory.kt          [52 lines, 2.0KB]
│   │   │   │   │   │   └── BackupOrchestrator.kt           [53 lines, 2.1KB]
│   │   │   │   │   └── usecase/            # Use cases (3 files, ~130 LOC)
│   │   │   │   │       ├── BackupAppsUseCase.kt            [45 lines, 1.8KB]
│   │   │   │   │       ├── RestoreAppsUseCase.kt           [43 lines, 1.7KB]
│   │   │   │   │       └── VerifySnapshotUseCase.kt        [42 lines, 1.6KB]
│   │   │   │   │
│   │   │   │   ├── engine/                 # Backup/restore engine (10 files, ~2059 LOC)
│   │   │   │   │   ├── AdaptiveBackgroundStrategy.kt       [168 lines, 6.5KB] - Adaptive backup strategy
│   │   │   │   │   ├── ArchiveFormat.kt                    [143 lines, 5.6KB] - Archive format handling
│   │   │   │   │   ├── BackupEngine.kt                     [256 lines, 10.0KB] - Core backup engine
│   │   │   │   │   ├── IncrementalBackupStrategy.kt        [289 lines, 11.2KB] - Incremental backups
│   │   │   │   │   ├── ObsidianBoxCommands.kt              [182 lines, 7.1KB] - Shell commands
│   │   │   │   │   ├── ObsidianBoxEngine.kt                [533 lines, 20.8KB] - Main engine orchestrator
│   │   │   │   │   ├── ParallelBackupEngine.kt             [234 lines, 9.1KB] - Parallel processing
│   │   │   │   │   ├── ShellExecutor.kt                    [142 lines, 5.5KB] - Shell execution
│   │   │   │   │   ├── SplitApkHelper.kt                   [87 lines, 3.4KB] - Split APK handling
│   │   │   │   │   ├── TransactionalRestoreEngine.kt       [125 lines, 4.9KB] - Transactional restores
│   │   │   │   │   ├── restore/            # Restore components (2 files, ~233 LOC)
│   │   │   │   │   │   ├── RestoreJournal.kt               [118 lines, 4.6KB]
│   │   │   │   │   │   └── RestoreTransaction.kt           [115 lines, 4.5KB]
│   │   │   │   │   └── shell/              # Shell utilities (2 files, ~321 LOC)
│   │   │   │   │       ├── AuditLogger.kt                  [142 lines, 5.5KB]
│   │   │   │   │       └── SafeShellExecutor.kt            [179 lines, 7.0KB]
│   │   │   │   │
│   │   │   │   ├── error/                  # Error handling (3 files, ~192 LOC)
│   │   │   │   │   ├── ErrorHandler.kt                     [67 lines, 2.6KB]
│   │   │   │   │   ├── ErrorRecovery.kt                    [73 lines, 2.8KB]
│   │   │   │   │   └── RetryStrategy.kt                    [52 lines, 2.0KB]
│   │   │   │   │
│   │   │   │   ├── features/               # Feature flags (1 file, ~88 LOC)
│   │   │   │   │   └── FeatureFlags.kt                     [88 lines, 3.4KB]
│   │   │   │   │
│   │   │   │   ├── gaming/                 # Gaming features (5 files, ~1581 LOC)
│   │   │   │   │   ├── EmulatorDetector.kt                 [267 lines, 10.4KB] - Emulator detection
│   │   │   │   │   ├── GamingBackupManager.kt              [412 lines, 16.0KB] - Gaming backup manager
│   │   │   │   │   ├── PlayGamesCloudSync.kt               [356 lines, 13.9KB] - Play Games sync
│   │   │   │   │   ├── RomScanner.kt                       [298 lines, 11.6KB] - ROM scanning
│   │   │   │   │   ├── SaveStateManager.kt                 [248 lines, 9.7KB] - Save state management
│   │   │   │   │   └── models/GamingModels.kt              [137 lines, 5.3KB] - Gaming data models
│   │   │   │   │
│   │   │   │   ├── health/                 # Health Connect (5 files, ~1781 LOC)
│   │   │   │   │   ├── HealthConnectManager.kt             [498 lines, 19.4KB] - Health Connect API
│   │   │   │   │   ├── HealthDataExporter.kt               [423 lines, 16.5KB] - Health data export
│   │   │   │   │   ├── HealthDataStore.kt                  [387 lines, 15.1KB] - Health data persistence
│   │   │   │   │   ├── HealthPrivacyScreen.kt              [278 lines, 10.8KB] - Privacy UI
│   │   │   │   │   └── HealthPrivacyViewModel.kt           [195 lines, 7.6KB] - Privacy view model
│   │   │   │   │
│   │   │   │   ├── ml/                     # Machine Learning (7 files, ~2462 LOC)
│   │   │   │   │   ├── SmartScheduler.kt                   [558 lines, 21.7KB] - Smart scheduling
│   │   │   │   │   ├── analytics/BackupAnalytics.kt        [372 lines, 14.5KB] - Analytics collection
│   │   │   │   │   ├── context/ContextAwareManager.kt      [359 lines, 14.0KB] - Context awareness
│   │   │   │   │   ├── models/             # ML models (2 files, ~430 LOC)
│   │   │   │   │   │   ├── BackupPrediction.kt             [198 lines, 7.7KB]
│   │   │   │   │   │   └── UserHabitModel.kt               [232 lines, 9.0KB]
│   │   │   │   │   ├── nlp/NaturalLanguageProcessor.kt     [330 lines, 12.9KB] - NLP processing
│   │   │   │   │   └── prediction/BackupPredictor.kt       [413 lines, 16.1KB] - Prediction engine
│   │   │   │   │
│   │   │   │   ├── model/                  # Core models (6 files, ~416 LOC)
│   │   │   │   │   ├── BackupEngine.kt                     [52 lines, 2.0KB]
│   │   │   │   │   ├── BackupModels.kt                     [189 lines, 7.4KB]
│   │   │   │   │   ├── FeatureTier.kt                      [67 lines, 2.6KB]
│   │   │   │   │   ├── PermissionCapabilities.kt           [42 lines, 1.6KB]
│   │   │   │   │   ├── PermissionManager.kt                [38 lines, 1.5KB]
│   │   │   │   │   └── PermissionMode.kt                   [28 lines, 1.1KB]
│   │   │   │   │
│   │   │   │   ├── performance/            # Performance optimization (7 files, ~1220 LOC)
│   │   │   │   │   ├── BatteryOptimizationManager.kt       [234 lines, 9.1KB]
│   │   │   │   │   ├── ImageOptimizationManager.kt         [178 lines, 6.9KB]
│   │   │   │   │   ├── LazyListOptimizer.kt                [145 lines, 5.6KB]
│   │   │   │   │   ├── MemoryOptimizationManager.kt        [198 lines, 7.7KB]
│   │   │   │   │   ├── NetworkOptimizationManager.kt       [167 lines, 6.5KB]
│   │   │   │   │   ├── PerformanceConfig.kt                [89 lines, 3.5KB]
│   │   │   │   │   └── PerformanceProfiler.kt              [209 lines, 8.1KB]
│   │   │   │   │
│   │   │   │   ├── permissions/            # Permission management (2 files, ~593 LOC)
│   │   │   │   │   ├── PermissionCapability.kt             [287 lines, 11.2KB]
│   │   │   │   │   └── PermissionManager.kt                [306 lines, 11.9KB]
│   │   │   │   │
│   │   │   │   ├── plugins/                # Plugin system (18+ files)
│   │   │   │   │   ├── PluginManager.kt                    [234 lines, 9.1KB] - Plugin management
│   │   │   │   │   ├── PluginSandbox.kt                    [114 lines, 4.4KB] - Plugin sandboxing
│   │   │   │   │   ├── api/                # Plugin API (4 files, ~82 LOC)
│   │   │   │   │   │   ├── PluginApiVersion.kt             [18 lines, 0.7KB]
│   │   │   │   │   │   ├── PluginCapability.kt             [23 lines, 0.9KB]
│   │   │   │   │   │   ├── PluginException.kt              [21 lines, 0.8KB]
│   │   │   │   │   │   └── PluginMetadata.kt               [20 lines, 0.8KB]
│   │   │   │   │   ├── builtin/            # Built-in plugins (7 files, ~1686 LOC)
│   │   │   │   │   │   ├── AutomationPluginExamples.kt     [178 lines, 6.9KB]
│   │   │   │   │   │   ├── DefaultAutomationPlugin.kt      [612 lines, 23.8KB]
│   │   │   │   │   │   ├── FilecoinCloudProviderPlugin.kt  [298 lines, 11.6KB]
│   │   │   │   │   │   ├── LocalCloudProvider.kt           [234 lines, 9.1KB]
│   │   │   │   │   │   ├── RcloneDropboxPlugin.kt          [112 lines, 4.4KB]
│   │   │   │   │   │   ├── RcloneGoogleDrivePlugin.kt      [134 lines, 5.2KB]
│   │   │   │   │   │   └── RcloneS3Plugin.kt               [118 lines, 4.6KB]
│   │   │   │   │   ├── core/               # Plugin infrastructure (4 files, ~372 LOC)
│   │   │   │   │   │   ├── PluginLoader.kt                 [123 lines, 4.8KB]
│   │   │   │   │   │   ├── PluginManager.kt                [89 lines, 3.5KB]
│   │   │   │   │   │   ├── PluginRegistry.kt               [78 lines, 3.0KB]
│   │   │   │   │   │   └── PluginSandbox.kt                [82 lines, 3.2KB]
│   │   │   │   │   ├── discovery/          # Plugin discovery (3 files, ~315 LOC)
│   │   │   │   │   │   ├── ManifestPluginDiscovery.kt      [112 lines, 4.4KB]
│   │   │   │   │   │   ├── PackagePluginDiscovery.kt       [134 lines, 5.2KB]
│   │   │   │   │   │   └── PluginValidator.kt              [69 lines, 2.7KB]
│   │   │   │   │   └── interfaces/         # Plugin interfaces (5 files, ~383 LOC)
│   │   │   │   │       ├── AutomationPlugin.kt             [89 lines, 3.5KB]
│   │   │   │   │       ├── BackupEnginePlugin.kt           [76 lines, 3.0KB]
│   │   │   │   │       ├── CloudProviderPlugin.kt          [94 lines, 3.7KB]
│   │   │   │   │       ├── ExportPlugin.kt                 [67 lines, 2.6KB]
│   │   │   │   │       └── Plugin.kt                       [57 lines, 2.2KB]
│   │   │   │   │
│   │   │   │   ├── presentation/           # View models (8 files, ~612 LOC)
│   │   │   │   │   ├── backup/BackupViewModel.kt           [78 lines, 3.0KB]
│   │   │   │   │   ├── backup/BackupState.kt               [21 lines, 0.8KB]
│   │   │   │   │   ├── backup/BackupIntent.kt              [17 lines, 0.7KB]
│   │   │   │   │   ├── dashboard/DashboardViewModel.kt     [73 lines, 2.8KB]
│   │   │   │   │   ├── gaming/GamingViewModel.kt           [89 lines, 3.5KB]
│   │   │   │   │   ├── gaming/GamingBackupViewModel.kt     [92 lines, 3.6KB]
│   │   │   │   │   ├── gaming/SpeedrunViewModel.kt         [68 lines, 2.6KB]
│   │   │   │   │   ├── health/HealthViewModel.kt           [76 lines, 3.0KB]
│   │   │   │   │   └── plugins/PluginsViewModel.kt         [98 lines, 3.8KB]
│   │   │   │   │
│   │   │   │   ├── scanner/                # App scanning (1 file, ~284 LOC)
│   │   │   │   │   └── AppScanner.kt                       [284 lines, 11.1KB]
│   │   │   │   │
│   │   │   │   ├── security/               # Security features (11 files, ~3781 LOC)
│   │   │   │   │   ├── BiometricAuthIntegration.kt         [456 lines, 17.8KB] - Biometric integration
│   │   │   │   │   ├── BiometricAuthManager.kt             [398 lines, 15.5KB] - Biometric manager
│   │   │   │   │   ├── BiometricExampleUsage.kt            [134 lines, 5.2KB] - Usage examples
│   │   │   │   │   ├── BiometricSettings.kt                [287 lines, 11.2KB] - Biometric settings
│   │   │   │   │   ├── CertificatePinningManager.kt        [312 lines, 12.1KB] - Certificate pinning
│   │   │   │   │   ├── PasskeyManager.kt                   [498 lines, 19.4KB] - Passkey management
│   │   │   │   │   ├── RootDetectionManager.kt             [234 lines, 9.1KB] - Root detection
│   │   │   │   │   ├── SecureDatabaseHelper.kt             [278 lines, 10.8KB] - Secure database
│   │   │   │   │   ├── SecureStorageManager.kt             [423 lines, 16.5KB] - Secure storage
│   │   │   │   │   ├── TaskerSecurityValidator.kt          [456 lines, 17.8KB] - Tasker security
│   │   │   │   │   └── WebViewSecurityManager.kt           [305 lines, 11.9KB] - WebView security
│   │   │   │   │
│   │   │   │   ├── storage/                # Data persistence (12 files, ~2051 LOC)
│   │   │   │   │   ├── AppBackupDao.kt                     [127 lines, 4.9KB] - Backup DAO
│   │   │   │   │   ├── AppBackupEntity.kt                  [98 lines, 3.8KB] - Backup entity
│   │   │   │   │   ├── BackupCatalog.kt                    [289 lines, 11.2KB] - Backup catalog
│   │   │   │   │   ├── BackupScheduleDao.kt                [112 lines, 4.4KB] - Schedule DAO
│   │   │   │   │   ├── BackupScheduleEntity.kt             [87 lines, 3.4KB] - Schedule entity
│   │   │   │   │   ├── FileSystemManager.kt                [356 lines, 13.9KB] - File system access
│   │   │   │   │   ├── MediaStoreHelper.kt                 [234 lines, 9.1KB] - MediaStore helper
│   │   │   │   │   ├── SafHelper.kt                        [198 lines, 7.7KB] - SAF helper
│   │   │   │   │   ├── ScopedStorageMigration.kt           [267 lines, 10.4KB] - Storage migration
│   │   │   │   │   ├── SettingsDao.kt                      [89 lines, 3.5KB] - Settings DAO
│   │   │   │   │   ├── SettingsEntity.kt                   [72 lines, 2.8KB] - Settings entity
│   │   │   │   │   ├── StoragePermissionHelper.kt          [168 lines, 6.5KB] - Permission helper
│   │   │   │   │   └── migrations/DatabaseMigrations.kt    [154 lines, 6.0KB] - DB migrations
│   │   │   │   │
│   │   │   │   ├── sync/                   # Syncthing integration (4 files, ~1281 LOC)
│   │   │   │   │   ├── models/SyncthingModels.kt           [265 lines, 10.3KB] - Syncthing models
│   │   │   │   │   ├── SyncthingApiClient.kt               [378 lines, 14.7KB] - API client
│   │   │   │   │   ├── SyncthingConflictResolver.kt        [321 lines, 12.5KB] - Conflict resolution
│   │   │   │   │   └── SyncthingManager.kt                 [317 lines, 12.3KB] - Syncthing manager
│   │   │   │   │
│   │   │   │   ├── tasker/                 # Tasker integration (3 files, ~1031 LOC)
│   │   │   │   │   ├── plugin/TaskerPluginActions.kt       [219 lines, 8.5KB] - Tasker actions
│   │   │   │   │   ├── TaskerEventPublisher.kt             [267 lines, 10.4KB] - Event publishing
│   │   │   │   │   ├── TaskerIntegration.kt                [634 lines, 24.7KB] - Tasker integration
│   │   │   │   │   └── TaskerStatusProvider.kt             [311 lines, 12.1KB] - Status provider
│   │   │   │   │
│   │   │   │   ├── ui/                     # UI layer (45+ files)
│   │   │   │   │   ├── MainActivity.kt                     [75 lines, 2.9KB] - Main activity
│   │   │   │   │   ├── Navigation.kt                       [89 lines, 3.5KB] - Navigation graph
│   │   │   │   │   ├── ObsidianBackupApp.kt                [68 lines, 2.6KB] - Root composable
│   │   │   │   │   ├── cloud/CloudProviderConfigScreen.kt  [379 lines, 14.8KB] - Cloud config UI
│   │   │   │   │   │
│   │   │   │   │   ├── components/         # Reusable components (5 files, ~1296 LOC)
│   │   │   │   │   │   ├── animations/LottieAnimations.kt  [519 lines, 20.2KB]
│   │   │   │   │   │   ├── EmptyStates.kt                  [156 lines, 6.1KB]
│   │   │   │   │   │   ├── EnhancedComponents.kt           [298 lines, 11.6KB]
│   │   │   │   │   │   ├── LiveBackupConsole.kt            [189 lines, 7.4KB]
│   │   │   │   │   │   ├── Microinteractions.kt            [78 lines, 3.0KB]
│   │   │   │   │   │   └── SkeletonLoading.kt              [56 lines, 2.2KB]
│   │   │   │   │   │
│   │   │   │   │   ├── navigation/NavigationTransitions.kt [233 lines, 9.1KB] - Navigation transitions
│   │   │   │   │   ├── onboarding/OnboardingFlow.kt        [166 lines, 6.5KB] - Onboarding flow
│   │   │   │   │   │
│   │   │   │   │   ├── screens/            # App screens (18 files, ~4971 LOC)
│   │   │   │   │   │   ├── AppsScreen.kt                   [298 lines, 11.6KB]
│   │   │   │   │   │   ├── AutomationScreen.kt             [267 lines, 10.4KB]
│   │   │   │   │   │   ├── BackupsScreen.kt                [312 lines, 12.1KB]
│   │   │   │   │   │   ├── DashboardScreen.kt              [423 lines, 16.5KB]
│   │   │   │   │   │   ├── EnhancedBackupsScreen.kt        [389 lines, 15.1KB]
│   │   │   │   │   │   ├── FeatureFlagsScreen.kt           [178 lines, 6.9KB]
│   │   │   │   │   │   ├── FilecoinConfigScreen.kt         [698 lines, 27.2KB]
│   │   │   │   │   │   ├── GamingBackupScreen.kt           [456 lines, 17.8KB]
│   │   │   │   │   │   ├── GamingScreen.kt                 [378 lines, 14.7KB]
│   │   │   │   │   │   ├── HealthScreen.kt                 [512 lines, 19.9KB]
│   │   │   │   │   │   ├── LogsScreen.kt                   [234 lines, 9.1KB]
│   │   │   │   │   │   ├── OptimizedAppsScreen.kt          [289 lines, 11.2KB]
│   │   │   │   │   │   ├── OtherScreens.kt                 [156 lines, 6.1KB]
│   │   │   │   │   │   ├── PluginsScreen.kt                [267 lines, 10.4KB]
│   │   │   │   │   │   ├── SettingsScreen.kt               [389 lines, 15.1KB]
│   │   │   │   │   │   ├── SimplifiedModeScreen.kt         [234 lines, 9.1KB]
│   │   │   │   │   │   ├── SpeedrunModeScreen.kt           [198 lines, 7.7KB]
│   │   │   │   │   │   └── ZeroKnowledgeScreen.kt          [693 lines, 27.0KB]
│   │   │   │   │   │
│   │   │   │   │   ├── screens/community/  # Community screens (5 files, ~1206 LOC)
│   │   │   │   │   │   ├── ChangelogAndTipsScreens.kt      [267 lines, 10.4KB]
│   │   │   │   │   │   ├── CommunityScreen.kt              [298 lines, 11.6KB]
│   │   │   │   │   │   ├── CommunityViewModels.kt          [234 lines, 9.1KB]
│   │   │   │   │   │   ├── FeedbackScreen.kt               [256 lines, 10.0KB]
│   │   │   │   │   │   └── OnboardingScreen.kt             [151 lines, 5.9KB]
│   │   │   │   │   │
│   │   │   │   │   ├── screens/syncthing/  # Syncthing screens (3 files, ~1019 LOC)
│   │   │   │   │   │   ├── ConflictResolutionScreen.kt     [345 lines, 13.4KB]
│   │   │   │   │   │   ├── DevicePairingScreen.kt          [312 lines, 12.1KB]
│   │   │   │   │   │   └── SyncthingScreen.kt              [362 lines, 14.1KB]
│   │   │   │   │   │
│   │   │   │   │   ├── theme/              # App theme (3 files, ~365 LOC)
│   │   │   │   │   │   ├── Color.kt                        [156 lines, 6.1KB]
│   │   │   │   │   │   ├── Theme.kt                        [134 lines, 5.2KB]
│   │   │   │   │   │   └── Type.kt                         [75 lines, 2.9KB]
│   │   │   │   │   │
│   │   │   │   │   └── utils/              # UI utilities (3 files, ~477 LOC)
│   │   │   │   │       ├── AnimationSpecs.kt               [123 lines, 4.8KB]
│   │   │   │   │       ├── HapticFeedback.kt               [178 lines, 6.9KB]
│   │   │   │   │       └── PredictiveBackGesture.kt        [176 lines, 6.9KB]
│   │   │   │   │
│   │   │   │   ├── verification/           # Verification (1 file, ~118 LOC)
│   │   │   │   │   └── ChecksumVerifier.kt                 [118 lines, 4.6KB]
│   │   │   │   │
│   │   │   │   ├── wear/                   # Wear OS bridge (2 files, ~151 LOC)
│   │   │   │   │   ├── WearBridge.kt                       [89 lines, 3.5KB]
│   │   │   │   │   └── WearCommunicationManager.kt         [62 lines, 2.4KB]
│   │   │   │   │
│   │   │   │   ├── widget/                 # Widget (1 file, ~121 LOC)
│   │   │   │   │   └── BackupWidget.kt                     [121 lines, 4.7KB]
│   │   │   │   │
│   │   │   │   ├── work/                   # Background work (3 files, ~384 LOC)
│   │   │   │   │   ├── BackupWorker.kt                     [156 lines, 6.1KB]
│   │   │   │   │   ├── CloudSyncWorker.kt                  [134 lines, 5.2KB]
│   │   │   │   │   └── HealthSyncWorker.kt                 [94 lines, 3.7KB]
│   │   │   │   │
│   │   │   │   ├── MainActivity.kt         [75 lines, 2.9KB] - Main activity
│   │   │   │   └── ObsidianBackupApplication.kt [116 lines, 4.5KB] - Application class
│   │   │   │
│   │   │   ├── res/                        # Android resources
│   │   │   │   ├── drawable/               # Vector drawables (50+ icons)
│   │   │   │   ├── layout/                 # XML layouts (15+ layouts)
│   │   │   │   ├── mipmap-*/               # App icons (all densities)
│   │   │   │   ├── values/                 # Values resources
│   │   │   │   │   ├── colors.xml          - Color definitions
│   │   │   │   │   ├── strings.xml         - String resources
│   │   │   │   │   ├── styles.xml          - Style definitions
│   │   │   │   │   └── themes.xml          - App themes
│   │   │   │   └── xml/                    # XML configs
│   │   │   │       ├── backup_rules.xml    - Backup rules
│   │   │   │       ├── data_extraction_rules.xml - Data extraction
│   │   │   │       └── network_security_config.xml - Network security
│   │   │   │
│   │   │   └── AndroidManifest.xml         - App manifest
│   │   │
│   │   ├── androidTest/                    # Instrumentation tests (24 files, ~66 test files total)
│   │   └── test/                           # Unit tests (42 files)
│   │
│   ├── build.gradle.kts                    [~1200 lines, 46.8KB] - App build config
│   ├── proguard-rules.pro                  - ProGuard rules
│   └── google-services.json                - Firebase config
│
├── tv/                                     # Android TV module
│   ├── src/main/java/com/obsidianbackup/tv/
│   │   ├── ui/                             # TV UI components (9 files)
│   │   ├── backup/TVBackupManager.kt       - TV backup manager
│   │   ├── settings/TVSettingsManager.kt   - TV settings
│   │   ├── navigation/TVNavigationHandler.kt - TV navigation
│   │   └── TVApplication.kt                - TV app class
│   ├── src/main/res/                       # TV resources
│   └── build.gradle.kts                    - TV build config
│
├── wear/                                   # Wear OS module
│   ├── src/main/java/com/obsidianbackup/wear/
│   │   ├── complications/                  # Watch complications (2 files)
│   │   ├── data/                           # Wear data layer (2 files)
│   │   ├── di/                             # Wear DI (1 file)
│   │   ├── presentation/                   # Wear UI (screens, theme, viewmodels)
│   │   ├── tiles/                          # Wear OS tiles (2 files)
│   │   ├── utils/                          # Wear utilities (2 files)
│   │   └── WearBackupApplication.kt        - Wear app class
│   ├── src/main/res/                       # Wear resources
│   └── build.gradle.kts                    - Wear build config
│
├── enterprise/                             # Enterprise edition backend
│   ├── backend/                            # Ktor backend server
│   │   ├── src/main/kotlin/com/obsidianbackup/enterprise/
│   │   │   ├── auth/                       # Authentication (JWT, SAML)
│   │   │   ├── database/                   # Database layer
│   │   │   ├── models/                     # Data models
│   │   │   ├── plugins/                    # Ktor plugins
│   │   │   ├── routes/                     # API routes (7 route files)
│   │   │   ├── services/                   # Business services (6 services)
│   │   │   └── Application.kt              - Server entry point
│   │   ├── src/main/resources/
│   │   │   ├── application.conf            - Server config
│   │   │   └── logback.xml                 - Logging config
│   │   ├── build.gradle.kts                - Backend build config
│   │   └── Dockerfile                      - Docker image
│   ├── docker-compose.yml                  - Docker Compose config
│   ├── install.sh                          - Installation script
│   └── README.md                           - Enterprise documentation
│
├── web-companion/                          # Web companion app
│   ├── src/                                # React source code
│   │   ├── components/                     # React components
│   │   ├── hooks/                          # React hooks
│   │   ├── pages/                          # Page components
│   │   ├── services/                       # API services
│   │   ├── stores/                         # State management
│   │   ├── styles/                         # CSS styles
│   │   └── utils/                          # Utilities
│   ├── server/                             # Express backend
│   │   └── routes/                         # API routes
│   ├── public/                             # Public assets
│   ├── package.json                        - NPM dependencies
│   └── README.md                           - Web app documentation
│
├── functions/                              # Firebase Functions
│   ├── index.js                            - Cloud functions
│   └── package.json                        - Function dependencies
│
├── docs/                                   # Documentation
│   ├── user-guides/                        # User documentation (8 guides)
│   ├── developer-guides/                   # Developer docs (6 guides)
│   ├── adr/                                # Architecture Decision Records (3 ADRs)
│   ├── examples/                           # Code examples
│   ├── static-site/                        # MkDocs site
│   ├── api/                                # API documentation
│   ├── README.md                           - Docs index
│   ├── index.md                            - Docs homepage
│   └── QUICKSTART.md                       - Quick start guide
│
├── scripts/                                # Build/test scripts
│   ├── run_tests.sh                        - Test runner
│   ├── generate_coverage.sh               - Coverage generator
│   └── run_instrumentation_tests.sh        - Instrumentation test runner
│
├── .well-known/                            # Web metadata
│   └── assetlinks.json                     - Digital Asset Links
│
├── Documentation Files (185+ MD files)     # Comprehensive documentation
│   ├── README.md                           - Main readme
│   ├── specification.md                    - Full specification
│   ├── ACCESSIBILITY_*.md                  - Accessibility docs (7 files)
│   ├── CLOUD_*.md                          - Cloud provider docs (5 files)
│   ├── ZERO_KNOWLEDGE_*.md                 - Zero-knowledge docs (4 files)
│   ├── BIOMETRIC_*.md                      - Biometric auth docs (3 files)
│   ├── GAMING_*.md                         - Gaming feature docs (2 files)
│   ├── ENTERPRISE_EDITION.md               - Enterprise documentation
│   ├── POST_QUANTUM_*.md                   - Post-quantum crypto docs (2 files)
│   ├── TASKER_*.md                         - Tasker integration docs (3 files)
│   ├── ML_*.md                             - Machine learning docs (2 files)
│   ├── WIDGET_*.md                         - Widget docs (2 files)
│   ├── SPLIT_APK_*.md                      - Split APK docs (3 files)
│   ├── INCREMENTAL_BACKUP_*.md             - Incremental backup docs
│   ├── DEEP_LINKING_*.md                   - Deep linking docs
│   ├── UX_*.md                             - UX enhancement docs (3 files)
│   ├── SECURITY_*.md                       - Security docs (2 files)
│   ├── PERFORMANCE_*.md                    - Performance docs
│   ├── MONETIZATION.md                     - Monetization strategy
│   ├── ANDROID_TV_APP.md                   - Android TV docs
│   ├── WEAR_OS_*.md                        - Wear OS docs
│   ├── DOCUMENTATION_SYSTEM.md             - Documentation system
│   ├── FIREBASE_SETUP.md                   - Firebase setup
│   ├── IMPLEMENTATION_SUMMARY.md           - Implementation summary
│   └── *_QUICK_REFERENCE.md                - Quick reference guides (10+ files)
│
├── Verification Scripts (5+ SH files)
│   ├── verify_security.sh                  - Security verification
│   ├── verify_cloud_providers.sh           - Cloud provider verification
│   ├── verify_biometric_implementation.sh  - Biometric verification
│   ├── verify_performance_optimization.sh  - Performance verification
│   └── verify_monetization.sh              - Monetization verification
│
├── Configuration Files
│   ├── build.gradle.kts                    - Root build config
│   ├── settings.gradle.kts                 - Gradle settings
│   ├── gradle.properties                   - Gradle properties
│   ├── local.properties                    - Local properties
│   └── gradlew, gradlew.bat                - Gradle wrappers
│
└── Assets
    ├── ObsidianBackup.png                  [1.1MB] - App logo
    └── README_INTEGRATION.md               - Integration readme

```

## 📋 File Category Breakdown

### Source Code (Kotlin) - 518 files, ~82,862 LOC
- **Main source**: 509 files
- **Test files**: 66 files (unit + instrumentation)
- **Average file size**: ~160 LOC

### Resources (XML) - 356 files
- **Layouts**: 15+ files
- **Drawables**: 50+ icons/vectors
- **Values**: 20+ resource files
- **Configurations**: 10+ XML configs

### Documentation (Markdown) - 188 files
- **User guides**: 8 files
- **Developer guides**: 6 files
- **Feature documentation**: 100+ files
- **Quick references**: 10+ files
- **Architecture decisions**: 3 ADRs

### Configuration - 15+ files
- **Gradle**: 8 files
- **Properties**: 3 files
- **JSON configs**: 3 files
- **Shell scripts**: 8 files

---

## 🎯 Key File Descriptions

### Core Application Files

| File | Lines | Size | Description |
|------|-------|------|-------------|
| `ObsidianBackupApplication.kt` | 116 | 4.5KB | Application class, initializes DI, Firebase, logging |
| `MainActivity.kt` | 75 | 2.9KB | Main activity entry point, hosts Compose UI |
| `ObsidianBoxEngine.kt` | 533 | 20.8KB | Core backup/restore orchestrator, main engine logic |
| `build.gradle.kts` | ~1200 | 46.8KB | Main build configuration with 50+ dependencies |

### Backup/Restore Engine

| File | Lines | Size | Description |
|------|-------|------|-------------|
| `BackupEngine.kt` | 256 | 10.0KB | Backup engine interface and base implementation |
| `ParallelBackupEngine.kt` | 234 | 9.1KB | Parallel backup processing for performance |
| `IncrementalBackupStrategy.kt` | 289 | 11.2KB | Incremental/differential backup logic |
| `TransactionalRestoreEngine.kt` | 125 | 4.9KB | Atomic restore operations with rollback |
| `RestoreJournal.kt` | 118 | 4.6KB | Restore operation journaling |
| `SafeShellExecutor.kt` | 179 | 7.0KB | Secure shell command execution |

### Cloud Storage

| File | Lines | Size | Description |
|------|-------|------|-------------|
| `CloudProvider.kt` | 342 | 13.3KB | Cloud storage provider interface |
| `FilecoinCloudProvider.kt` | 893 | 34.8KB | Decentralized Filecoin storage integration |
| `GoogleDriveProvider.kt` | 345 | 13.4KB | Google Drive API integration |
| `WebDavCloudProvider.kt` | 704 | 27.4KB | WebDAV protocol support |
| `RcloneCloudProvider.kt` | 445 | 17.3KB | Rclone wrapper for 40+ cloud providers |
| `CloudSyncManager.kt` | 456 | 17.8KB | Cloud sync orchestration and conflict resolution |

### Security/Encryption

| File | Lines | Size | Description |
|------|-------|------|-------------|
| `PostQuantumCrypto.kt` | 923 | 35.9KB | Post-quantum cryptography (Kyber, Dilithium) |
| `ZeroKnowledgeEncryption.kt` | 512 | 19.9KB | Zero-knowledge encryption for cloud backups |
| `EncryptionEngine.kt` | 412 | 16.0KB | AES-256-GCM encryption/decryption |
| `KeystoreManager.kt` | 387 | 15.1KB | Android Keystore management |
| `BiometricAuthManager.kt` | 398 | 15.5KB | Biometric authentication (fingerprint, face) |
| `PasskeyManager.kt` | 498 | 19.4KB | Passkey/WebAuthn support |

### Machine Learning

| File | Lines | Size | Description |
|------|-------|------|-------------|
| `SmartScheduler.kt` | 558 | 21.7KB | ML-driven backup scheduling |
| `BackupPredictor.kt` | 413 | 16.1KB | Backup prediction model |
| `UserHabitModel.kt` | 232 | 9.0KB | User behavior modeling |
| `ContextAwareManager.kt` | 359 | 14.0KB | Context-aware backup decisions |
| `NaturalLanguageProcessor.kt` | 330 | 12.9KB | NLP for user commands |

### UI Screens (Top 5 by size)

| File | Lines | Size | Description |
|------|-------|------|-------------|
| `FilecoinConfigScreen.kt` | 698 | 27.2KB | Filecoin configuration UI |
| `ZeroKnowledgeScreen.kt` | 693 | 27.0KB | Zero-knowledge encryption UI |
| `HealthScreen.kt` | 512 | 19.9KB | Health Connect data UI |
| `GamingBackupScreen.kt` | 456 | 17.8KB | Gaming backup UI |
| `DashboardScreen.kt` | 423 | 16.5KB | Main dashboard |

### Plugin System

| File | Lines | Size | Description |
|------|-------|------|-------------|
| `PluginManager.kt` | 234 | 9.1KB | Plugin lifecycle management |
| `PluginLoader.kt` | 123 | 4.8KB | Dynamic plugin loading |
| `PluginSandbox.kt` | 114 | 4.4KB | Plugin sandboxing for security |
| `DefaultAutomationPlugin.kt` | 612 | 23.8KB | Built-in automation plugin |
| `FilecoinCloudProviderPlugin.kt` | 298 | 11.6KB | Filecoin plugin |

---

## 📦 Module Breakdown

### Main App Module (`app/`)
- **Kotlin files**: 509
- **LOC**: ~58,000
- **Packages**: 85+
- **Purpose**: Core Android app with backup/restore functionality

### TV Module (`tv/`)
- **Kotlin files**: 12
- **LOC**: ~1,200
- **Packages**: 4
- **Purpose**: Android TV UI for large screens

### Wear Module (`wear/`)
- **Kotlin files**: 16
- **LOC**: ~1,800
- **Packages**: 7
- **Purpose**: Wear OS smartwatch app

### Enterprise Backend (`enterprise/backend/`)
- **Kotlin files**: 25
- **LOC**: ~3,500
- **Packages**: 8
- **Purpose**: Ktor-based enterprise management server

### Web Companion (`web-companion/`)
- **JavaScript/TypeScript files**: 30+
- **Purpose**: React-based web interface

---

## 🔍 Test Coverage Estimate

| Package | Test Files | Coverage |
|---------|-----------|----------|
| `engine` | 3 | ~60% |
| `cloud` | 3 | ~40% |
| `crypto` | 2 | ~50% |
| `gaming` | 1 | ~30% |
| `security` | 2 | ~35% |
| `ui` | 5 | ~25% |
| `plugins` | 2 | ~40% |
| **Overall** | **24** | **~42%** |

---

*This document provides a complete reference to every file in the ObsidianBackup project. For feature-based organization, see `FILE_TREE_BY_FEATURE.md`.*

