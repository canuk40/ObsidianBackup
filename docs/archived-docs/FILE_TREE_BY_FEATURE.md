# FILE TREE BY FEATURE - ObsidianBackup Project

**Generated**: 2026-02-09

This document organizes all project files by feature/functionality area, showing relationships and dependencies.

---

## 📋 Table of Contents

1. [Backup & Restore Core](#1-backup--restore-core)
2. [Cloud Storage Integration](#2-cloud-storage-integration)
3. [Security & Encryption](#3-security--encryption)
4. [User Interface & Presentation](#4-user-interface--presentation)
5. [Gaming Features](#5-gaming-features)
6. [Health Data Management](#6-health-data-management)
7. [Machine Learning & AI](#7-machine-learning--ai)
8. [Plugin System](#8-plugin-system)
9. [Billing & Monetization](#9-billing--monetization)
10. [Community & Engagement](#10-community--engagement)
11. [Tasker Integration](#11-tasker-integration)
12. [Deep Linking](#12-deep-linking)
13. [Accessibility Features](#13-accessibility-features)
14. [Performance Optimization](#14-performance-optimization)
15. [Syncthing Integration](#15-syncthing-integration)
16. [Data Persistence](#16-data-persistence)
17. [Device Migration](#17-device-migration)
18. [Android TV](#18-android-tv)
19. [Wear OS](#19-wear-os)
20. [Enterprise Edition](#20-enterprise-edition)
21. [Web Companion](#21-web-companion)
22. [Testing Infrastructure](#22-testing-infrastructure)
23. [Documentation](#23-documentation)
24. [Build & Configuration](#24-build--configuration)

---

## 1. Backup & Restore Core

**Purpose**: Core backup/restore engine, scheduling, and orchestration

### Engine Components
```
app/src/main/java/com/obsidianbackup/engine/
├── BackupEngine.kt                     [256 lines] - Core backup interface
├── ObsidianBoxEngine.kt                [533 lines] - Main engine orchestrator
├── ParallelBackupEngine.kt             [234 lines] - Parallel processing
├── IncrementalBackupStrategy.kt        [289 lines] - Incremental backups
├── AdaptiveBackgroundStrategy.kt       [168 lines] - Adaptive strategies
├── TransactionalRestoreEngine.kt       [125 lines] - Atomic restores
├── SplitApkHelper.kt                   [87 lines] - Split APK support
├── ArchiveFormat.kt                    [143 lines] - Archive formats
├── ShellExecutor.kt                    [142 lines] - Shell execution
├── ObsidianBoxCommands.kt              [182 lines] - Shell commands
├── restore/
│   ├── RestoreJournal.kt               [118 lines] - Restore journaling
│   └── RestoreTransaction.kt           [115 lines] - Transaction management
└── shell/
    ├── SafeShellExecutor.kt            [179 lines] - Secure shell executor
    └── AuditLogger.kt                  [142 lines] - Command audit logging
```

### Domain Layer
```
app/src/main/java/com/obsidianbackup/domain/
├── backup/
│   ├── BackupOrchestrator.kt           [53 lines] - Backup coordination
│   └── BackupEngineFactory.kt          [52 lines] - Engine factory
└── usecase/
    ├── BackupAppsUseCase.kt            [45 lines] - Backup use case
    ├── RestoreAppsUseCase.kt           [43 lines] - Restore use case
    └── VerifySnapshotUseCase.kt        [42 lines] - Verification use case
```

### Automation
```
app/src/main/java/com/obsidianbackup/automation/
└── BackupScheduler.kt                  [205 lines] - Automated scheduling

app/src/main/java/com/obsidianbackup/work/
├── BackupWorker.kt                     [156 lines] - Background backup worker
├── CloudSyncWorker.kt                  [134 lines] - Cloud sync worker
└── HealthSyncWorker.kt                 [94 lines] - Health sync worker
```

### Verification
```
app/src/main/java/com/obsidianbackup/verification/
└── ChecksumVerifier.kt                 [118 lines] - Checksum verification
```

### Models
```
app/src/main/java/com/obsidianbackup/model/
├── BackupModels.kt                     [189 lines] - Core data models
└── BackupEngine.kt                     [52 lines] - Engine models
```

**Total**: ~18 files, ~3,500 LOC

**Dependencies**: 
- Requires: crypto, storage, scanner
- Used by: domain, presentation, ui

---

## 2. Cloud Storage Integration

**Purpose**: Multi-cloud backup storage with 50+ provider support

### Core Cloud Abstraction
```
app/src/main/java/com/obsidianbackup/cloud/
├── CloudProvider.kt                    [342 lines] - Provider interface
├── CloudSyncManager.kt                 [456 lines] - Sync orchestration
├── CloudSyncRepository.kt              [287 lines] - Data layer
├── ConflictResolver.kt                 [324 lines] - Conflict resolution
├── OAuth2Manager.kt                    [192 lines] - OAuth authentication
└── oauth/
    └── OAuth2Provider.kt               [274 lines] - OAuth abstraction
```

### Native Providers
```
app/src/main/java/com/obsidianbackup/cloud/
├── GoogleDriveProvider.kt              [345 lines] - Google Drive API
├── WebDavCloudProvider.kt              [704 lines] - WebDAV protocol
└── FilecoinCloudProvider.kt            [893 lines] - Filecoin decentralized
```

### Additional Cloud Providers
```
app/src/main/java/com/obsidianbackup/cloud/providers/
├── AzureBlobProvider.kt                [812 lines] - Microsoft Azure
├── BackblazeB2Provider.kt              [934 lines] - Backblaze B2
├── AlibabaOSSProvider.kt               [798 lines] - Alibaba Cloud
├── DigitalOceanSpacesProvider.kt       [801 lines] - DigitalOcean
├── OracleCloudProvider.kt              [793 lines] - Oracle Cloud
└── BoxCloudProvider.kt                 [787 lines] - Box.com
```

### Rclone Integration (40+ providers)
```
app/src/main/java/com/obsidianbackup/cloud/rclone/
├── RcloneCloudProvider.kt              [445 lines] - Rclone wrapper
├── RcloneConfigManager.kt              [267 lines] - Config management
├── RcloneExecutor.kt                   [289 lines] - Command executor
├── RcloneProviderFactory.kt            [82 lines] - Factory
└── backends/
    ├── RcloneS3Provider.kt             [87 lines] - S3 backend
    ├── RcloneGoogleDriveProvider.kt    [92 lines] - GDrive backend
    └── RcloneDropboxProvider.kt        [89 lines] - Dropbox backend
```

### UI Integration
```
app/src/main/java/com/obsidianbackup/ui/cloud/
└── CloudProviderConfigScreen.kt        [379 lines] - Provider config UI

app/src/main/java/com/obsidianbackup/ui/screens/
└── FilecoinConfigScreen.kt             [698 lines] - Filecoin setup UI
```

**Total**: ~23 files, ~10,000 LOC

**Dependencies**:
- Requires: crypto, oauth, error handling
- Used by: engine, presentation, plugins

---

## 3. Security & Encryption

**Purpose**: Military-grade encryption, biometric auth, zero-knowledge protection

### Encryption Core
```
app/src/main/java/com/obsidianbackup/crypto/
├── EncryptionEngine.kt                 [412 lines] - AES-256-GCM encryption
├── PostQuantumCrypto.kt                [923 lines] - Post-quantum (Kyber, Dilithium)
├── ZeroKnowledgeEncryption.kt          [512 lines] - Zero-knowledge encryption
├── ZeroKnowledgeManager.kt             [256 lines] - ZK management
├── KeystoreManager.kt                  [387 lines] - Android Keystore
├── PrivacyAuditor.kt                   [298 lines] - Privacy compliance
└── PQCBenchmark.kt                     [245 lines] - PQC benchmarking
```

### Authentication & Security
```
app/src/main/java/com/obsidianbackup/security/
├── BiometricAuthManager.kt             [398 lines] - Biometric manager
├── BiometricAuthIntegration.kt         [456 lines] - Biometric integration
├── BiometricSettings.kt                [287 lines] - Biometric settings
├── BiometricExampleUsage.kt            [134 lines] - Usage examples
├── PasskeyManager.kt                   [498 lines] - Passkey/WebAuthn
├── SecureStorageManager.kt             [423 lines] - Secure storage
├── SecureDatabaseHelper.kt             [278 lines] - Encrypted database
├── CertificatePinningManager.kt        [312 lines] - SSL pinning
├── RootDetectionManager.kt             [234 lines] - Root detection
├── WebViewSecurityManager.kt           [305 lines] - WebView security
└── TaskerSecurityValidator.kt          [456 lines] - Tasker security
```

### UI Components
```
app/src/main/java/com/obsidianbackup/ui/screens/
└── ZeroKnowledgeScreen.kt              [693 lines] - Zero-knowledge UI
```

**Total**: ~18 files, ~7,000 LOC

**Dependencies**:
- Requires: keystore, biometric APIs
- Used by: engine, cloud, storage

**Key Features**:
- AES-256-GCM encryption
- Post-quantum cryptography (Kyber-1024, Dilithium-5)
- Zero-knowledge encryption for cloud
- Biometric authentication
- Hardware-backed keystore
- Certificate pinning

---

## 4. User Interface & Presentation

**Purpose**: Modern Material 3 UI with Jetpack Compose

### Core UI
```
app/src/main/java/com/obsidianbackup/ui/
├── ObsidianBackupApp.kt                [68 lines] - Root composable
├── Navigation.kt                       [89 lines] - Navigation graph
└── MainActivity.kt                     [75 lines] - Main activity
```

### Screens (18 screens)
```
app/src/main/java/com/obsidianbackup/ui/screens/
├── DashboardScreen.kt                  [423 lines] - Main dashboard
├── BackupsScreen.kt                    [312 lines] - Backup list
├── EnhancedBackupsScreen.kt            [389 lines] - Enhanced backup view
├── AppsScreen.kt                       [298 lines] - App selection
├── OptimizedAppsScreen.kt              [289 lines] - Optimized app list
├── SettingsScreen.kt                   [389 lines] - App settings
├── AutomationScreen.kt                 [267 lines] - Automation config
├── LogsScreen.kt                       [234 lines] - Backup logs
├── PluginsScreen.kt                    [267 lines] - Plugin management
├── FeatureFlagsScreen.kt               [178 lines] - Feature flags
├── GamingScreen.kt                     [378 lines] - Gaming features
├── GamingBackupScreen.kt               [456 lines] - Game backup UI
├── SpeedrunModeScreen.kt               [198 lines] - Speedrun mode
├── HealthScreen.kt                     [512 lines] - Health data UI
├── SimplifiedModeScreen.kt             [234 lines] - Accessibility mode
├── FilecoinConfigScreen.kt             [698 lines] - Filecoin config
├── ZeroKnowledgeScreen.kt              [693 lines] - Zero-knowledge UI
└── OtherScreens.kt                     [156 lines] - Miscellaneous screens
```

### Community Screens
```
app/src/main/java/com/obsidianbackup/ui/screens/community/
├── CommunityScreen.kt                  [298 lines] - Community hub
├── FeedbackScreen.kt                   [256 lines] - Feedback form
├── OnboardingScreen.kt                 [151 lines] - User onboarding
├── ChangelogAndTipsScreens.kt          [267 lines] - Changelog & tips
└── CommunityViewModels.kt              [234 lines] - Community VMs
```

### Syncthing Screens
```
app/src/main/java/com/obsidianbackup/ui/screens/syncthing/
├── SyncthingScreen.kt                  [362 lines] - Syncthing main
├── DevicePairingScreen.kt              [312 lines] - Device pairing
└── ConflictResolutionScreen.kt         [345 lines] - Conflict resolution
```

### Reusable Components
```
app/src/main/java/com/obsidianbackup/ui/components/
├── EnhancedComponents.kt               [298 lines] - Enhanced components
├── LiveBackupConsole.kt                [189 lines] - Real-time console
├── SkeletonLoading.kt                  [56 lines] - Skeleton loaders
├── Microinteractions.kt                [78 lines] - Micro-animations
├── EmptyStates.kt                      [156 lines] - Empty state views
└── animations/
    └── LottieAnimations.kt             [519 lines] - Lottie animations
```

### Theme & Styling
```
app/src/main/java/com/obsidianbackup/ui/theme/
├── Theme.kt                            [134 lines] - Material 3 theme
├── Color.kt                            [156 lines] - Color palette
└── Type.kt                             [75 lines] - Typography
```

### Navigation & Utilities
```
app/src/main/java/com/obsidianbackup/ui/
├── navigation/
│   └── NavigationTransitions.kt        [233 lines] - Screen transitions
├── onboarding/
│   └── OnboardingFlow.kt               [166 lines] - Onboarding flow
└── utils/
    ├── HapticFeedback.kt               [178 lines] - Haptic feedback
    ├── AnimationSpecs.kt               [123 lines] - Animation specs
    └── PredictiveBackGesture.kt        [176 lines] - Predictive back
```

### View Models
```
app/src/main/java/com/obsidianbackup/presentation/
├── backup/
│   ├── BackupViewModel.kt              [78 lines] - Backup VM
│   ├── BackupState.kt                  [21 lines] - Backup state
│   └── BackupIntent.kt                 [17 lines] - Backup intent
├── dashboard/
│   └── DashboardViewModel.kt           [73 lines] - Dashboard VM
├── gaming/
│   ├── GamingViewModel.kt              [89 lines] - Gaming VM
│   ├── GamingBackupViewModel.kt        [92 lines] - Gaming backup VM
│   └── SpeedrunViewModel.kt            [68 lines] - Speedrun VM
├── health/
│   └── HealthViewModel.kt              [76 lines] - Health VM
└── plugins/
    └── PluginsViewModel.kt             [98 lines] - Plugins VM
```

**Total**: ~45 files, ~10,000 LOC

**Dependencies**:
- Requires: presentation, domain, models
- UI framework: Jetpack Compose, Material 3

---

## 5. Gaming Features

**Purpose**: Gaming-specific backup for saves, ROMs, emulators

### Core Gaming Components
```
app/src/main/java/com/obsidianbackup/gaming/
├── GamingBackupManager.kt              [412 lines] - Gaming backup manager
├── SaveStateManager.kt                 [248 lines] - Save state management
├── RomScanner.kt                       [298 lines] - ROM detection
├── EmulatorDetector.kt                 [267 lines] - Emulator detection
├── PlayGamesCloudSync.kt               [356 lines] - Google Play Games sync
└── models/
    └── GamingModels.kt                 [137 lines] - Gaming data models
```

### UI Components
```
app/src/main/java/com/obsidianbackup/ui/screens/
├── GamingScreen.kt                     [378 lines] - Gaming main screen
├── GamingBackupScreen.kt               [456 lines] - Game backup UI
└── SpeedrunModeScreen.kt               [198 lines] - Speedrun mode

app/src/main/java/com/obsidianbackup/presentation/gaming/
├── GamingViewModel.kt                  [89 lines] - Gaming VM
├── GamingBackupViewModel.kt            [92 lines] - Backup VM
└── SpeedrunViewModel.kt                [68 lines] - Speedrun VM
```

### Dependency Injection
```
app/src/main/java/com/obsidianbackup/di/
└── GamingModule.kt                     [89 lines] - Gaming DI module
```

**Total**: ~12 files, ~3,000 LOC

**Features**:
- Emulator detection (Retroarch, PPSSPP, Dolphin, etc.)
- ROM scanning and organization
- Save state backup/restore
- Play Games Cloud sync
- Speedrun mode for quick restores
- Cloud storage integration

---

## 6. Health Data Management

**Purpose**: Health Connect integration for fitness data backup

### Core Health Components
```
app/src/main/java/com/obsidianbackup/health/
├── HealthConnectManager.kt             [498 lines] - Health Connect API
├── HealthDataStore.kt                  [387 lines] - Data persistence
├── HealthDataExporter.kt               [423 lines] - Data export
├── HealthPrivacyScreen.kt              [278 lines] - Privacy UI
└── HealthPrivacyViewModel.kt           [195 lines] - Privacy VM
```

### UI Components
```
app/src/main/java/com/obsidianbackup/ui/screens/
└── HealthScreen.kt                     [512 lines] - Health data UI

app/src/main/java/com/obsidianbackup/presentation/health/
└── HealthViewModel.kt                  [76 lines] - Health VM
```

### Background Work
```
app/src/main/java/com/obsidianbackup/work/
└── HealthSyncWorker.kt                 [94 lines] - Background sync
```

### Dependency Injection
```
app/src/main/java/com/obsidianbackup/di/
└── HealthModule.kt                     [92 lines] - Health DI module
```

**Total**: ~9 files, ~2,500 LOC

**Supported Data Types**:
- Steps, distance, calories
- Heart rate, blood pressure
- Sleep data
- Workout sessions
- Nutrition data
- Body measurements

---

## 7. Machine Learning & AI

**Purpose**: Smart scheduling, predictions, context-aware backups

### ML Core
```
app/src/main/java/com/obsidianbackup/ml/
├── SmartScheduler.kt                   [558 lines] - ML-driven scheduling
├── analytics/
│   └── BackupAnalytics.kt              [372 lines] - Analytics collection
├── context/
│   └── ContextAwareManager.kt          [359 lines] - Context awareness
├── models/
│   ├── UserHabitModel.kt               [232 lines] - User behavior model
│   └── BackupPrediction.kt             [198 lines] - Prediction models
├── nlp/
│   └── NaturalLanguageProcessor.kt     [330 lines] - NLP processor
└── prediction/
    └── BackupPredictor.kt              [413 lines] - Prediction engine
```

### Examples
```
app/src/main/java/com/obsidianbackup/examples/
└── SmartBackupIntegration.kt           [198 lines] - Integration example
```

### Dependency Injection
```
app/src/main/java/com/obsidianbackup/di/
└── MLModule.kt                         [134 lines] - ML DI module
```

**Total**: ~9 files, ~2,800 LOC

**Features**:
- User habit learning
- Optimal backup time prediction
- Battery-aware scheduling
- Network condition detection
- Natural language commands
- Context-aware decisions

---

## 8. Plugin System

**Purpose**: Extensible plugin architecture for custom functionality

### Plugin Infrastructure
```
app/src/main/java/com/obsidianbackup/plugins/
├── PluginManager.kt                    [234 lines] - Plugin lifecycle
├── PluginSandbox.kt                    [114 lines] - Security sandbox
├── core/
│   ├── PluginManager.kt                [89 lines] - Core manager
│   ├── PluginLoader.kt                 [123 lines] - Class loading
│   ├── PluginRegistry.kt               [78 lines] - Plugin registry
│   └── PluginSandbox.kt                [82 lines] - Sandboxing
├── api/
│   ├── PluginApiVersion.kt             [18 lines] - API versioning
│   ├── PluginCapability.kt             [23 lines] - Capabilities
│   ├── PluginException.kt              [21 lines] - Exceptions
│   └── PluginMetadata.kt               [20 lines] - Metadata
├── discovery/
│   ├── ManifestPluginDiscovery.kt      [112 lines] - Manifest discovery
│   ├── PackagePluginDiscovery.kt       [134 lines] - Package discovery
│   └── PluginValidator.kt              [69 lines] - Validation
└── interfaces/
    ├── Plugin.kt                       [57 lines] - Base plugin interface
    ├── BackupEnginePlugin.kt           [76 lines] - Engine plugin
    ├── CloudProviderPlugin.kt          [94 lines] - Cloud plugin
    ├── AutomationPlugin.kt             [89 lines] - Automation plugin
    └── ExportPlugin.kt                 [67 lines] - Export plugin
```

### Built-in Plugins
```
app/src/main/java/com/obsidianbackup/plugins/builtin/
├── DefaultAutomationPlugin.kt          [612 lines] - Default automation
├── FilecoinCloudProviderPlugin.kt      [298 lines] - Filecoin plugin
├── LocalCloudProvider.kt               [234 lines] - Local storage
├── RcloneS3Plugin.kt                   [118 lines] - Rclone S3
├── RcloneGoogleDrivePlugin.kt          [134 lines] - Rclone GDrive
├── RcloneDropboxPlugin.kt              [112 lines] - Rclone Dropbox
└── AutomationPluginExamples.kt         [178 lines] - Examples
```

### API Layer
```
app/src/main/java/com/obsidianbackup/api/plugin/
├── PluginApi.kt                        [126 lines] - Plugin API
├── BackupEnginePlugin.kt               [112 lines] - Engine API
├── CloudProviderPlugin.kt              [138 lines] - Cloud API
├── AutomationPlugin.kt                 [125 lines] - Automation API
├── ExportPlugin.kt                     [95 lines] - Export API
└── PluginCapability.kt                 [85 lines] - Capability API
```

### Context Implementation
```
app/src/main/java/com/obsidianbackup/plugin/
├── PluginAPI.kt                        [234 lines] - API implementation
└── PluginContextImpl.kt                [121 lines] - Context implementation
```

### UI
```
app/src/main/java/com/obsidianbackup/ui/screens/
└── PluginsScreen.kt                    [267 lines] - Plugin management UI

app/src/main/java/com/obsidianbackup/presentation/plugins/
└── PluginsViewModel.kt                 [98 lines] - Plugins VM
```

**Total**: ~30 files, ~4,500 LOC

**Features**:
- Dynamic plugin loading
- Security sandboxing
- Version management
- Plugin discovery (manifest + package)
- Built-in plugins
- API for custom plugins

---

## 9. Billing & Monetization

**Purpose**: Subscription management and feature gating

### Billing Core
```
app/src/main/java/com/obsidianbackup/billing/
├── BillingManager.kt                   [182 lines] - Billing coordination
├── SubscriptionManager.kt              [218 lines] - Subscription management
├── BillingRepository.kt                [127 lines] - Data layer
├── BillingModels.kt                    [143 lines] - Data models
├── FeatureGateService.kt               [156 lines] - Feature access
├── ProFeatureGate.kt                   [134 lines] - Pro gating
├── RevenueAnalytics.kt                 [198 lines] - Revenue tracking
└── di/
    └── BillingModule.kt                [64 lines] - DI module
```

### UI Components
```
app/src/main/java/com/obsidianbackup/billing/ui/
├── SubscriptionScreen.kt               [456 lines] - Subscription UI
├── SubscriptionViewModel.kt            [289 lines] - Subscription VM
└── UpgradePrompts.kt                   [88 lines] - Upgrade prompts
```

### Models
```
app/src/main/java/com/obsidianbackup/model/
└── FeatureTier.kt                      [67 lines] - Feature tiers
```

**Total**: ~11 files, ~2,100 LOC

**Features**:
- Google Play Billing integration
- Subscription tiers (Free, Pro, Enterprise)
- Feature gates
- Revenue analytics
- Upgrade prompts
- Trial management

---

## 10. Community & Engagement

**Purpose**: User feedback, analytics, tips, onboarding

### Community Core
```
app/src/main/java/com/obsidianbackup/community/
├── AnalyticsManager.kt                 [134 lines] - Analytics tracking
├── FeedbackManager.kt                  [156 lines] - User feedback
├── CrashlyticsManager.kt               [89 lines] - Crash reporting
├── BetaProgramManager.kt               [98 lines] - Beta program
├── ChangelogManager.kt                 [112 lines] - Changelog
├── TipsManager.kt                      [88 lines] - Tips & tricks
├── OnboardingManager.kt                [123 lines] - Onboarding
├── CommunityForumManager.kt            [127 lines] - Forum integration
└── ConfigSharingManager.kt             [145 lines] - Config sharing
```

### UI Components
```
app/src/main/java/com/obsidianbackup/ui/screens/community/
├── CommunityScreen.kt                  [298 lines] - Community hub
├── FeedbackScreen.kt                   [256 lines] - Feedback form
├── OnboardingScreen.kt                 [151 lines] - Onboarding UI
├── ChangelogAndTipsScreens.kt          [267 lines] - Changelog/tips
└── CommunityViewModels.kt              [234 lines] - View models
```

**Total**: ~14 files, ~2,300 LOC

**Features**:
- Firebase Analytics
- Crashlytics integration
- In-app feedback
- Beta program management
- Changelog display
- Tips & tutorials
- Config sharing

---

## 11. Tasker Integration

**Purpose**: Automation integration with Tasker app

### Tasker Components
```
app/src/main/java/com/obsidianbackup/tasker/
├── TaskerIntegration.kt                [634 lines] - Main integration
├── TaskerEventPublisher.kt             [267 lines] - Event publishing
├── TaskerStatusProvider.kt             [311 lines] - Status provider
└── plugin/
    └── TaskerPluginActions.kt          [219 lines] - Plugin actions
```

### Security
```
app/src/main/java/com/obsidianbackup/security/
└── TaskerSecurityValidator.kt          [456 lines] - Security validation
```

### Dependency Injection
```
app/src/main/java/com/obsidianbackup/di/
└── TaskerModule.kt                     [80 lines] - Tasker DI
```

**Total**: ~6 files, ~2,000 LOC

**Features**:
- Tasker action support
- Event publishing
- Status queries
- Security validation
- Variable support

---

## 12. Deep Linking

**Purpose**: Deep link support for backup URLs

### Deep Link Core
```
app/src/main/java/com/obsidianbackup/deeplink/
├── DeepLinkHandler.kt                  [278 lines] - Main handler
├── DeepLinkParser.kt                   [245 lines] - URL parsing
├── DeepLinkRouter.kt                   [312 lines] - Routing logic
├── DeepLinkGenerator.kt                [189 lines] - Link generation
├── DeepLinkAuthenticator.kt            [234 lines] - Authentication
├── DeepLinkAnalytics.kt                [143 lines] - Analytics
├── DeepLinkIntegration.kt              [156 lines] - Integration
├── DeepLinkModels.kt                   [198 lines] - Data models
├── DeepLinkActivity.kt                 [167 lines] - Deep link activity
├── DeepLinkTestActivity.kt             [68 lines] - Testing activity
└── DeepLinkModule.kt                   [67 lines] - DI module
```

**Total**: ~11 files, ~2,000 LOC

**Features**:
- obsidianbackup:// URL scheme
- Web link support (assetlinks.json)
- Backup restoration via link
- Authentication & security
- Analytics tracking

---

## 13. Accessibility Features

**Purpose**: Accessibility support for all users

### Accessibility Core
```
app/src/main/java/com/obsidianbackup/accessibility/
├── AccessibilityHelper.kt              [138 lines] - Accessibility utilities
├── SimplifiedModeViewModel.kt          [156 lines] - Simplified mode VM
└── VoiceControlHandler.kt              [130 lines] - Voice commands
```

### UI
```
app/src/main/java/com/obsidianbackup/ui/screens/
└── SimplifiedModeScreen.kt             [234 lines] - Simplified UI
```

**Total**: ~4 files, ~650 LOC

**Features**:
- TalkBack support
- Simplified mode
- Voice control
- High contrast themes
- Large text support

---

## 14. Performance Optimization

**Purpose**: App performance optimization

### Performance Core
```
app/src/main/java/com/obsidianbackup/performance/
├── PerformanceProfiler.kt              [209 lines] - Performance profiling
├── PerformanceConfig.kt                [89 lines] - Configuration
├── BatteryOptimizationManager.kt       [234 lines] - Battery optimization
├── MemoryOptimizationManager.kt        [198 lines] - Memory management
├── NetworkOptimizationManager.kt       [167 lines] - Network optimization
├── ImageOptimizationManager.kt         [178 lines] - Image optimization
└── LazyListOptimizer.kt                [145 lines] - List optimization
```

**Total**: ~7 files, ~1,200 LOC

**Features**:
- Performance profiling
- Battery optimization
- Memory management
- Network optimization
- Image caching
- Lazy loading

---

## 15. Syncthing Integration

**Purpose**: Decentralized peer-to-peer sync

### Syncthing Core
```
app/src/main/java/com/obsidianbackup/sync/
├── SyncthingManager.kt                 [317 lines] - Syncthing manager
├── SyncthingApiClient.kt               [378 lines] - API client
├── SyncthingConflictResolver.kt        [321 lines] - Conflict resolution
└── models/
    └── SyncthingModels.kt              [265 lines] - Data models
```

### UI Components
```
app/src/main/java/com/obsidianbackup/ui/screens/syncthing/
├── SyncthingScreen.kt                  [362 lines] - Main screen
├── DevicePairingScreen.kt              [312 lines] - Device pairing
└── ConflictResolutionScreen.kt         [345 lines] - Conflict UI
```

**Total**: ~7 files, ~2,300 LOC

---

## 16. Data Persistence

**Purpose**: Local database and file storage

### Room Database
```
app/src/main/java/com/obsidianbackup/storage/
├── AppBackupDao.kt                     [127 lines] - Backup DAO
├── AppBackupEntity.kt                  [98 lines] - Backup entity
├── BackupScheduleDao.kt                [112 lines] - Schedule DAO
├── BackupScheduleEntity.kt             [87 lines] - Schedule entity
├── SettingsDao.kt                      [89 lines] - Settings DAO
├── SettingsEntity.kt                   [72 lines] - Settings entity
├── BackupCatalog.kt                    [289 lines] - Backup catalog
└── migrations/
    └── DatabaseMigrations.kt           [154 lines] - Schema migrations
```

### File System
```
app/src/main/java/com/obsidianbackup/storage/
├── FileSystemManager.kt                [356 lines] - File operations
├── MediaStoreHelper.kt                 [234 lines] - MediaStore API
├── SafHelper.kt                        [198 lines] - SAF operations
├── ScopedStorageMigration.kt           [267 lines] - Storage migration
└── StoragePermissionHelper.kt          [168 lines] - Permissions
```

### Data Layer
```
app/src/main/java/com/obsidianbackup/data/
├── local/catalog/
│   └── BackupCatalogFacade.kt          [32 lines] - Catalog facade
└── repository/
    ├── AppRepository.kt                [38 lines] - App repository
    ├── BackupRepository.kt             [43 lines] - Backup repository
    └── CatalogRepository.kt            [38 lines] - Catalog repository
```

**Total**: ~16 files, ~2,400 LOC

---

## 17. Device Migration

**Purpose**: WiFi Direct device-to-device migration

### Migration Core
```
app/src/main/java/com/obsidianbackup/migration/
├── WiFiDirectMigration.kt              [57 lines] - WiFi Direct wrapper
├── client/
│   └── MigrationClient.kt              [251 lines] - Migration client
├── server/
│   └── MigrationServer.kt              [368 lines] - Migration server
└── protocol/
    └── MigrationProtocol.kt            [215 lines] - Protocol definitions
```

**Total**: ~4 files, ~900 LOC

---

## 18. Android TV

**Purpose**: Android TV leanback UI

### TV Module
```
tv/src/main/java/com/obsidianbackup/tv/
├── TVApplication.kt                    [89 lines] - TV application
├── backup/
│   └── TVBackupManager.kt              [178 lines] - TV backup manager
├── settings/
│   └── TVSettingsManager.kt            [145 lines] - TV settings
├── navigation/
│   └── TVNavigationHandler.kt          [123 lines] - TV navigation
└── ui/
    ├── MainActivity.kt                 [98 lines] - TV main activity
    ├── MainFragment.kt                 [234 lines] - Main fragment
    ├── AppSelectionActivity.kt         [156 lines] - App selection
    ├── AppSelectionFragment.kt         [189 lines] - App selection fragment
    ├── BackupDetailsActivity.kt        [145 lines] - Backup details
    ├── BackupDetailsFragment.kt        [198 lines] - Backup details fragment
    ├── SettingsActivity.kt             [112 lines] - Settings activity
    ├── SettingsFragment.kt             [167 lines] - Settings fragment
    └── CardPresenters.kt               [134 lines] - Card presenters
```

**Total**: ~13 files, ~2,000 LOC

---

## 19. Wear OS

**Purpose**: Smartwatch companion app

### Wear Module
```
wear/src/main/java/com/obsidianbackup/wear/
├── WearBackupApplication.kt            [78 lines] - Wear application
├── complications/
│   ├── BackupComplication.kt           [156 lines] - Backup complication
│   └── StatusComplication.kt           [134 lines] - Status complication
├── data/
│   ├── WearDataClient.kt               [189 lines] - Data layer client
│   └── WearRepository.kt               [145 lines] - Wear repository
├── di/
│   └── WearModule.kt                   [67 lines] - Wear DI
├── presentation/
│   ├── screens/
│   │   ├── WearDashboardScreen.kt      [234 lines] - Dashboard
│   │   ├── WearBackupScreen.kt         [198 lines] - Backup screen
│   │   └── WearSettingsScreen.kt       [167 lines] - Settings
│   ├── theme/
│   │   ├── WearTheme.kt                [89 lines] - Wear theme
│   │   └── WearColors.kt               [56 lines] - Wear colors
│   └── viewmodel/
│       ├── WearDashboardViewModel.kt   [123 lines] - Dashboard VM
│       └── WearBackupViewModel.kt      [112 lines] - Backup VM
├── tiles/
│   ├── BackupTile.kt                   [178 lines] - Backup tile
│   └── StatusTile.kt                   [156 lines] - Status tile
└── utils/
    ├── WearDataSyncManager.kt          [145 lines] - Data sync
    └── WearNotificationManager.kt      [123 lines] - Notifications
```

**Total**: ~16 files, ~2,500 LOC

---

## 20. Enterprise Edition

**Purpose**: Enterprise management backend (Ktor server)

### Enterprise Backend
```
enterprise/backend/src/main/kotlin/com/obsidianbackup/enterprise/
├── Application.kt                      [156 lines] - Server entry point
├── auth/
│   ├── JWTAuth.kt                      [234 lines] - JWT authentication
│   └── SAMLService.kt                  [312 lines] - SAML SSO
├── database/
│   ├── DatabaseFactory.kt              [178 lines] - Database setup
│   └── Tables.kt                       [267 lines] - Table definitions
├── models/
│   └── Models.kt                       [423 lines] - Data models
├── plugins/
│   ├── CORS.kt                         [67 lines] - CORS plugin
│   ├── Serialization.kt                [45 lines] - JSON serialization
│   ├── Logging.kt                      [56 lines] - Logging plugin
│   └── StatusPages.kt                  [89 lines] - Error handling
├── routes/
│   ├── RootRoutes.kt                   [123 lines] - Root routes
│   ├── AuthRoutes.kt                   [198 lines] - Auth endpoints
│   ├── DeviceRoutes.kt                 [234 lines] - Device management
│   ├── PolicyRoutes.kt                 [189 lines] - Policy management
│   ├── RBACRoutes.kt                   [167 lines] - RBAC endpoints
│   ├── AuditRoutes.kt                  [145 lines] - Audit logs
│   └── ReportRoutes.kt                 [156 lines] - Reports
└── services/
    ├── UserService.kt                  [245 lines] - User management
    ├── DeviceService.kt                [267 lines] - Device service
    ├── PolicyService.kt                [234 lines] - Policy service
    ├── RBACService.kt                  [198 lines] - RBAC service
    ├── AuditService.kt                 [189 lines] - Audit service
    └── ReportService.kt                [178 lines] - Report service
```

**Total**: ~25 files, ~5,000 LOC

---

## 21. Web Companion

**Purpose**: React-based web interface

### Web Companion (Summary)
```
web-companion/
├── src/
│   ├── components/         # React components (20+ files)
│   ├── hooks/              # Custom hooks (8 files)
│   ├── pages/              # Page components (12 files)
│   ├── services/           # API services (6 files)
│   ├── stores/             # State management (5 files)
│   ├── styles/             # CSS styles (10 files)
│   └── utils/              # Utilities (8 files)
├── server/
│   └── routes/             # Express routes (6 files)
└── public/                 # Static assets
```

**Total**: ~75 files, ~8,000 LOC

---

## 22. Testing Infrastructure

**Purpose**: Unit and instrumentation tests

### Unit Tests
```
app/src/test/java/com/obsidianbackup/
├── engine/
│   ├── BackupEngineTest.kt
│   ├── IncrementalBackupStrategyTest.kt
│   └── ObsidianBoxEngineTest.kt
├── cloud/
│   ├── CloudBackupTest.kt
│   ├── FilecoinCloudProviderTest.kt
│   └── MerkleTreeTest.kt
├── crypto/
│   ├── PostQuantumCryptoTest.kt
│   └── ZeroKnowledgeEncryptionTest.kt
├── gaming/
│   └── GamingBackupTest.kt
├── repository/
│   └── BackupRepositoryTest.kt
└── testing/
    ├── mocks/               # Mock objects
    ├── TestUtils.kt
    ├── TestConstants.kt
    └── BaseTest.kt
```

### Instrumentation Tests
```
app/src/androidTest/java/com/obsidianbackup/
├── accessibility/
│   └── AccessibilityTest.kt
├── integration/
│   ├── IntegrationTest.kt
│   └── BackupRestoreIntegrationTest.kt
├── ui/
│   └── DashboardScreenTest.kt
└── testing/
    └── HiltTestRunner.kt
```

**Total**: ~66 files, ~5,000 LOC

---

## 23. Documentation

**Purpose**: Comprehensive project documentation

### Documentation Structure
```
docs/
├── user-guides/            # User documentation (8 guides)
│   ├── getting-started.md
│   ├── features-overview.md
│   ├── backup-configuration.md
│   ├── cloud-storage.md
│   ├── automation.md
│   ├── biometric-security.md
│   ├── troubleshooting.md
│   └── faq.md
├── developer-guides/       # Developer documentation (6 guides)
│   ├── architecture.md
│   ├── building.md
│   ├── contributing.md
│   ├── testing.md
│   ├── plugin-development.md
│   └── security-policy.md
├── adr/                    # Architecture Decision Records
│   ├── 001-architecture-layered.md
│   ├── 002-plugin-system.md
│   └── 003-merkle-tree-verification.md
├── examples/               # Code examples
├── api/                    # API documentation
└── static-site/            # MkDocs site
```

### Root Documentation (185+ files)
```
Root Documentation Files:
├── README.md                           - Main project readme
├── specification.md                    - Full specification
├── ACCESSIBILITY_*.md                  - Accessibility docs (7 files)
├── CLOUD_*.md                          - Cloud docs (5 files)
├── ZERO_KNOWLEDGE_*.md                 - Zero-knowledge docs (4 files)
├── BIOMETRIC_*.md                      - Biometric docs (3 files)
├── GAMING_*.md                         - Gaming docs (2 files)
├── POST_QUANTUM_*.md                   - PQC docs (2 files)
├── TASKER_*.md                         - Tasker docs (3 files)
├── ML_*.md                             - ML docs (2 files)
├── WIDGET_*.md                         - Widget docs (2 files)
├── SPLIT_APK_*.md                      - Split APK docs (3 files)
├── UX_*.md                             - UX docs (3 files)
├── SECURITY_*.md                       - Security docs (2 files)
├── ENTERPRISE_EDITION.md               - Enterprise docs
├── ANDROID_TV_APP.md                   - TV docs
├── WEAR_OS_*.txt                       - Wear docs
├── DOCUMENTATION_SYSTEM.md             - Doc system
├── FIREBASE_SETUP.md                   - Firebase setup
├── IMPLEMENTATION_SUMMARY.md           - Implementation summary
└── *_QUICK_REFERENCE.md                - Quick references (10+ files)
```

**Total**: ~200 files, ~20,000 LOC

---

## 24. Build & Configuration

**Purpose**: Build system and configuration

### Gradle Configuration
```
Root Build Files:
├── build.gradle.kts                    - Root build script
├── settings.gradle.kts                 - Project settings
├── gradle.properties                   - Gradle properties
├── local.properties                    - Local configuration
├── gradlew                             - Gradle wrapper (Unix)
└── gradlew.bat                         - Gradle wrapper (Windows)

Module Build Files:
├── app/build.gradle.kts                [~1200 lines] - App build config
├── tv/build.gradle.kts                 - TV build config
├── wear/build.gradle.kts               - Wear build config
└── enterprise/backend/build.gradle.kts - Backend build config
```

### Configuration Files
```
Configuration Files:
├── app/proguard-rules.pro              - ProGuard rules
├── app/google-services.json            - Firebase config
├── .well-known/assetlinks.json         - Digital Asset Links
├── functions/package.json              - Cloud functions
└── web-companion/package.json          - Web app dependencies
```

### Scripts
```
scripts/
├── run_tests.sh                        - Test runner
├── generate_coverage.sh                - Coverage generator
├── run_instrumentation_tests.sh        - Instrumentation tests
└── verify_*.sh                         - Verification scripts (5 files)
```

**Total**: ~20 files

---

## 🔗 Feature Relationships

### Core Dependencies Graph

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  (ViewModels, UI Screens, Compose Components)               │
└──────────────┬──────────────────────────────────────────────┘
               │
               ├─► Backup/Restore ◄──┬─► Cloud Storage ◄─┬─► Security
               │                     │                    │
               ├─► Gaming Features   │                    ├─► Plugins
               │                     │                    │
               ├─► Health Data ──────┤                    └─► Billing
               │                     │
               ├─► ML/AI ────────────┘
               │
               └─► Community/Tasker/Accessibility
                   
┌────────────────────────────────────────────────────────────┐
│                     Domain Layer                           │
│   (Use Cases, Orchestrators, Business Logic)               │
└──────────────┬─────────────────────────────────────────────┘
               │
               └─► Data Layer (Storage, Repositories, DAOs)
```

### Cross-Feature Dependencies

| Feature | Depends On |
|---------|-----------|
| **Backup/Restore** | crypto, storage, scanner, error handling |
| **Cloud Storage** | crypto, oauth, error handling, engine |
| **Gaming** | engine, storage, cloud |
| **Health** | engine, storage, crypto, work |
| **ML/AI** | storage, analytics, models |
| **Plugins** | engine, cloud, api, sandbox |
| **UI** | presentation, domain, deeplink, theme |
| **Billing** | model, repository, analytics |
| **Tasker** | engine, security, events |

---

*This document organizes files by feature area. For a complete file tree, see `COMPLETE_FILE_TREE.md`. For package-level details, see `PACKAGE_STRUCTURE.md`.*
