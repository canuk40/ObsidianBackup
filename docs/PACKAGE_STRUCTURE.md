# PACKAGE STRUCTURE - ObsidianBackup Project

**Generated**: 2026-02-09  
**Purpose**: Detailed package-level documentation with classes, methods, and dependencies

---

## 📋 Table of Contents

1. [Package Overview](#package-overview)
2. [Core Application Packages](#core-application-packages)
3. [Feature Packages](#feature-packages)
4. [Infrastructure Packages](#infrastructure-packages)
5. [Platform Packages](#platform-packages)
6. [Package Dependencies](#package-dependencies)
7. [Statistics by Package](#statistics-by-package)

---

## Package Overview

The ObsidianBackup project follows Clean Architecture principles with clear separation of concerns:

```
┌─────────────────────────────────────────────────────┐
│              Presentation Layer (UI)                │
│  ViewModels, Compose Screens, UI Components         │
└───────────────────┬─────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────┐
│              Domain Layer (Business Logic)           │
│  Use Cases, Orchestrators, Business Rules            │
└───────────────────┬─────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────┐
│              Data Layer (Storage)                    │
│  Repositories, DAOs, Entities, Remote APIs           │
└───────────────────┬─────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────┐
│         Framework Layer (Android, External)          │
│  Android APIs, Third-party Libraries                 │
└─────────────────────────────────────────────────────┘
```

**Total Packages**: 85+  
**Total Classes**: 383  
**Total Interfaces**: 63  
**Total Data Classes**: 432  
**Total Objects**: 62

---

## Core Application Packages

### 1. `com.obsidianbackup.engine`

**Purpose**: Core backup/restore engine with multiple strategies

**Files**: 10 | **LOC**: 2,059 | **Layer**: Domain

#### Classes

| Class | Lines | Purpose |
|-------|-------|---------|
| `BackupEngine` | 256 | Core backup interface and base implementation |
| `ObsidianBoxEngine` | 533 | Main backup/restore orchestrator |
| `ParallelBackupEngine` | 234 | Parallel backup processing for performance |
| `IncrementalBackupStrategy` | 289 | Incremental/differential backup logic |
| `AdaptiveBackgroundStrategy` | 168 | Adaptive backup strategies |
| `TransactionalRestoreEngine` | 125 | Atomic restore with rollback |
| `ArchiveFormat` | 143 | Archive format handling (tar, zip) |
| `SplitApkHelper` | 87 | Split APK detection and handling |
| `ShellExecutor` | 142 | Shell command execution |
| `ObsidianBoxCommands` | 182 | Shell command definitions |

#### Key Methods

```kotlin
// BackupEngine interface
interface BackupEngine {
    suspend fun backup(request: BackupRequest): Result<BackupResult>
    suspend fun restore(request: RestoreRequest): Result<RestoreResult>
    suspend fun verify(snapshot: BackupSnapshot): Result<VerificationResult>
    suspend fun cancel(operationId: String)
}

// ObsidianBoxEngine orchestrator
class ObsidianBoxEngine : BackupEngine {
    suspend fun backupApp(packageName: String, options: BackupOptions)
    suspend fun restoreApp(backupPath: String, options: RestoreOptions)
    suspend fun listBackups(filter: BackupFilter): List<BackupSnapshot>
    fun observeProgress(): Flow<BackupProgress>
}

// IncrementalBackupStrategy
class IncrementalBackupStrategy {
    suspend fun calculateDiff(old: Snapshot, new: Snapshot): Delta
    suspend fun applyDiff(base: Snapshot, delta: Delta): Snapshot
    fun computeChecksum(data: ByteArray): String
}
```

#### Dependencies

- **Requires**: crypto, storage, scanner, model, verification
- **Used by**: domain.usecase, presentation, work
- **External**: Kotlin Coroutines, kotlinx.serialization

---

### 2. `com.obsidianbackup.engine.restore`

**Purpose**: Restore-specific components

**Files**: 2 | **LOC**: 233

#### Classes

| Class | Lines | Purpose |
|-------|-------|---------|
| `RestoreJournal` | 118 | Journal for restore operations |
| `RestoreTransaction` | 115 | Transaction management for atomic restores |

#### Key Methods

```kotlin
class RestoreJournal {
    suspend fun beginRestore(request: RestoreRequest): Transaction
    suspend fun commitRestore(transaction: Transaction)
    suspend fun rollbackRestore(transaction: Transaction)
    fun getRestoreHistory(): List<RestoreEntry>
}
```

---

### 3. `com.obsidianbackup.engine.shell`

**Purpose**: Safe shell execution with auditing

**Files**: 2 | **LOC**: 321

#### Classes

| Class | Lines | Purpose |
|-------|-------|---------|
| `SafeShellExecutor` | 179 | Secure shell command executor |
| `AuditLogger` | 142 | Command execution audit logging |

#### Key Methods

```kotlin
class SafeShellExecutor {
    suspend fun execute(command: String, args: List<String>): ShellResult
    suspend fun executeAsRoot(command: String): ShellResult
    fun validateCommand(command: String): Boolean
}
```

---

### 4. `com.obsidianbackup.cloud`

**Purpose**: Cloud storage abstraction layer

**Files**: 8 | **LOC**: 2,913 | **Layer**: Data

#### Classes & Interfaces

| Class/Interface | Lines | Purpose |
|-----------------|-------|---------|
| `CloudProvider` (interface) | 342 | Cloud storage provider interface |
| `CloudSyncManager` | 456 | Cloud sync orchestration |
| `CloudSyncRepository` | 287 | Cloud sync data layer |
| `ConflictResolver` | 324 | Sync conflict resolution |
| `GoogleDriveProvider` | 345 | Google Drive API implementation |
| `WebDavCloudProvider` | 704 | WebDAV protocol implementation |
| `FilecoinCloudProvider` | 893 | Filecoin decentralized storage |
| `OAuth2Manager` | 192 | OAuth2 authentication |

#### Key Methods

```kotlin
interface CloudProvider {
    suspend fun upload(file: File, path: String): Result<CloudFile>
    suspend fun download(path: String, destination: File): Result<File>
    suspend fun list(path: String): Result<List<CloudFile>>
    suspend fun delete(path: String): Result<Unit>
    suspend fun authenticate(): Result<AuthToken>
}

class CloudSyncManager {
    suspend fun syncBackup(backup: BackupSnapshot): Result<SyncResult>
    suspend fun syncRestore(cloudPath: String): Result<RestoreResult>
    fun observeSyncStatus(): Flow<SyncStatus>
    suspend fun resolveConflicts(conflicts: List<Conflict>): Result<Unit>
}
```

#### Data Models

```kotlin
data class CloudFile(
    val name: String,
    val path: String,
    val size: Long,
    val modified: Instant,
    val checksum: String?
)

data class CloudSnapshotMetadata(
    val id: String,
    val timestamp: Instant,
    val apps: List<String>,
    val size: Long,
    val encrypted: Boolean
)
```

#### Dependencies

- **Requires**: crypto, oauth, error, model
- **Used by**: engine, presentation, plugins
- **External**: OkHttp, Retrofit, Google Drive API

---

### 5. `com.obsidianbackup.cloud.providers`

**Purpose**: Additional cloud storage providers

**Files**: 6 | **LOC**: 4,725

#### Providers

| Provider | Lines | Cloud Service |
|----------|-------|--------------|
| `AzureBlobProvider` | 812 | Microsoft Azure Blob Storage |
| `BackblazeB2Provider` | 934 | Backblaze B2 |
| `AlibabaOSSProvider` | 798 | Alibaba Cloud Object Storage |
| `DigitalOceanSpacesProvider` | 801 | DigitalOcean Spaces |
| `OracleCloudProvider` | 793 | Oracle Cloud Storage |
| `BoxCloudProvider` | 787 | Box.com |

All providers implement the `CloudProvider` interface.

---

### 6. `com.obsidianbackup.cloud.rclone`

**Purpose**: Rclone integration for 40+ cloud providers

**Files**: 7 | **LOC**: 1,351

#### Classes

| Class | Lines | Purpose |
|-------|-------|---------|
| `RcloneCloudProvider` | 445 | Rclone provider wrapper |
| `RcloneConfigManager` | 267 | Rclone configuration management |
| `RcloneExecutor` | 289 | Rclone command executor |
| `RcloneProviderFactory` | 82 | Factory for Rclone backends |
| `RcloneS3Provider` | 87 | S3 backend |
| `RcloneGoogleDriveProvider` | 92 | Google Drive backend |
| `RcloneDropboxProvider` | 89 | Dropbox backend |

#### Key Methods

```kotlin
class RcloneCloudProvider(val config: RcloneConfig) : CloudProvider {
    suspend fun createRemote(remoteName: String, type: String, config: Map<String, String>)
    suspend fun listRemotes(): List<RemoteConfig>
    suspend fun testConnection(remoteName: String): Boolean
}
```

---

### 7. `com.obsidianbackup.crypto`

**Purpose**: Security and encryption layer

**Files**: 7 | **LOC**: 3,033 | **Layer**: Infrastructure

#### Classes

| Class | Lines | Purpose |
|-------|-------|---------|
| `EncryptionEngine` | 412 | AES-256-GCM encryption/decryption |
| `PostQuantumCrypto` | 923 | Post-quantum cryptography (Kyber, Dilithium) |
| `ZeroKnowledgeEncryption` | 512 | Zero-knowledge encryption for cloud |
| `ZeroKnowledgeManager` | 256 | Zero-knowledge key management |
| `KeystoreManager` | 387 | Android Keystore management |
| `PrivacyAuditor` | 298 | Privacy compliance auditing |
| `PQCBenchmark` | 245 | Post-quantum crypto benchmarking |

#### Key Methods

```kotlin
class EncryptionEngine {
    suspend fun encrypt(data: ByteArray, key: SecretKey): EncryptedData
    suspend fun decrypt(encrypted: EncryptedData, key: SecretKey): ByteArray
    suspend fun generateKey(keySize: Int = 256): SecretKey
    fun deriveKey(password: String, salt: ByteArray): SecretKey
}

class PostQuantumCrypto {
    suspend fun generateKyberKeyPair(): KyberKeyPair
    suspend fun encapsulateKyber(publicKey: KyberPublicKey): KyberCiphertext
    suspend fun decapsulateKyber(ciphertext: KyberCiphertext, secretKey: KyberSecretKey): ByteArray
    suspend fun signDilithium(message: ByteArray, secretKey: DilithiumSecretKey): ByteArray
    suspend fun verifyDilithium(message: ByteArray, signature: ByteArray, publicKey: DilithiumPublicKey): Boolean
}

class ZeroKnowledgeEncryption {
    suspend fun encryptForCloud(data: ByteArray, masterPassword: String): ZKProof
    suspend fun decryptFromCloud(proof: ZKProof, masterPassword: String): ByteArray
    fun generateZKProof(plaintext: ByteArray, key: SecretKey): ZKProof
}
```

#### Dependencies

- **Requires**: Android Keystore, BouncyCastle
- **Used by**: engine, cloud, storage
- **External**: BC PQC, Android Security

---

### 8. `com.obsidianbackup.security`

**Purpose**: Authentication and security hardening

**Files**: 11 | **LOC**: 3,781

#### Classes

| Class | Lines | Purpose |
|-------|-------|---------|
| `BiometricAuthManager` | 398 | Biometric authentication manager |
| `BiometricAuthIntegration` | 456 | Biometric integration layer |
| `BiometricSettings` | 287 | Biometric preferences |
| `BiometricExampleUsage` | 134 | Usage examples |
| `PasskeyManager` | 498 | Passkey/WebAuthn support |
| `SecureStorageManager` | 423 | Encrypted secure storage |
| `SecureDatabaseHelper` | 278 | Encrypted database helper |
| `CertificatePinningManager` | 312 | SSL certificate pinning |
| `RootDetectionManager` | 234 | Root/jailbreak detection |
| `WebViewSecurityManager` | 305 | WebView security hardening |
| `TaskerSecurityValidator` | 456 | Tasker integration security |

#### Key Methods

```kotlin
class BiometricAuthManager {
    suspend fun authenticate(title: String, subtitle: String): Result<BiometricResult>
    fun isBiometricAvailable(): Boolean
    fun getSupportedBiometricTypes(): List<BiometricType>
}

class PasskeyManager {
    suspend fun createPasskey(username: String): Result<Passkey>
    suspend fun authenticateWithPasskey(): Result<AuthResult>
    suspend fun listPasskeys(): List<Passkey>
}

class RootDetectionManager {
    fun isDeviceRooted(): Boolean
    fun checkSafetyNetAttestation(): SafetyNetResult
}
```

---

### 9. `com.obsidianbackup.ui`

**Purpose**: User interface layer

**Files**: 45+ | **LOC**: ~10,000 | **Layer**: Presentation

#### Structure

```
ui/
├── ObsidianBackupApp.kt         # Root composable
├── Navigation.kt                # Navigation graph
├── MainActivity.kt              # Activity host
├── screens/                     # App screens (26 files)
├── components/                  # Reusable UI (6 files)
├── theme/                       # Material 3 theme (3 files)
├── navigation/                  # Navigation utilities
├── onboarding/                  # Onboarding flow
├── utils/                       # UI utilities
└── cloud/                       # Cloud config screens
```

#### Key Screens

| Screen | Lines | Purpose |
|--------|-------|---------|
| `DashboardScreen` | 423 | Main app dashboard |
| `BackupsScreen` | 312 | Backup list and management |
| `AppsScreen` | 298 | App selection |
| `SettingsScreen` | 389 | App settings |
| `FilecoinConfigScreen` | 698 | Filecoin configuration |
| `ZeroKnowledgeScreen` | 693 | Zero-knowledge encryption UI |
| `HealthScreen` | 512 | Health Connect UI |
| `GamingBackupScreen` | 456 | Gaming backup UI |

#### Reusable Components

```kotlin
@Composable
fun EnhancedCard(title: String, content: @Composable () -> Unit)

@Composable
fun LiveBackupConsole(progress: BackupProgress)

@Composable
fun SkeletonLoader(isLoading: Boolean, content: @Composable () -> Unit)

@Composable
fun LottieLoadingAnimation(animationRes: Int)
```

---

### 10. `com.obsidianbackup.presentation`

**Purpose**: MVVM view models

**Files**: 8 | **LOC**: 612 | **Layer**: Presentation

#### ViewModels

| ViewModel | Lines | Purpose |
|-----------|-------|---------|
| `BackupViewModel` | 78 | Backup screen state management |
| `DashboardViewModel` | 73 | Dashboard state |
| `GamingViewModel` | 89 | Gaming features state |
| `GamingBackupViewModel` | 92 | Gaming backup state |
| `SpeedrunViewModel` | 68 | Speedrun mode state |
| `HealthViewModel` | 76 | Health Connect state |
| `PluginsViewModel` | 98 | Plugin management state |

#### Example ViewModel

```kotlin
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupUseCase: BackupAppsUseCase,
    private val repository: BackupRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(BackupState())
    val state: StateFlow<BackupState> = _state.asStateFlow()
    
    fun onIntent(intent: BackupIntent) {
        when (intent) {
            is BackupIntent.StartBackup -> startBackup(intent.apps)
            is BackupIntent.CancelBackup -> cancelBackup()
            is BackupIntent.RefreshList -> refreshBackups()
        }
    }
    
    private fun startBackup(apps: List<String>) {
        viewModelScope.launch {
            backupUseCase(apps).collect { result ->
                _state.update { it.copy(progress = result) }
            }
        }
    }
}
```

---

### 11. `com.obsidianbackup.domain`

**Purpose**: Business logic layer

**Files**: 5 | **LOC**: 235 | **Layer**: Domain

#### Use Cases

| Use Case | Lines | Purpose |
|----------|-------|---------|
| `BackupAppsUseCase` | 45 | Backup apps use case |
| `RestoreAppsUseCase` | 43 | Restore apps use case |
| `VerifySnapshotUseCase` | 42 | Verify backup integrity |

#### Orchestrators

| Class | Lines | Purpose |
|-------|-------|---------|
| `BackupOrchestrator` | 53 | Backup workflow coordination |
| `BackupEngineFactory` | 52 | Engine creation factory |

#### Use Case Pattern

```kotlin
class BackupAppsUseCase @Inject constructor(
    private val engine: BackupEngine,
    private val repository: BackupRepository
) {
    operator fun invoke(apps: List<String>): Flow<BackupProgress> = flow {
        val request = BackupRequest(apps = apps)
        engine.backup(request).collect { progress ->
            emit(progress)
            repository.updateProgress(progress)
        }
    }
}
```

---

### 12. `com.obsidianbackup.plugins`

**Purpose**: Extensible plugin system

**Files**: 30+ | **LOC**: 4,500 | **Layer**: Framework

#### Structure

```
plugins/
├── PluginManager.kt             # Plugin lifecycle
├── PluginSandbox.kt             # Security sandbox
├── core/                        # Plugin infrastructure (4 files)
├── api/                         # Plugin API (4 files)
├── builtin/                     # Built-in plugins (7 files)
├── discovery/                   # Plugin discovery (3 files)
└── interfaces/                  # Plugin interfaces (5 files)
```

#### Plugin Interfaces

```kotlin
interface Plugin {
    val metadata: PluginMetadata
    fun initialize(context: PluginContext)
    fun shutdown()
}

interface BackupEnginePlugin : Plugin {
    suspend fun performBackup(request: BackupRequest): Result<BackupResult>
}

interface CloudProviderPlugin : Plugin, CloudProvider {
    val providerName: String
    val requiresAuth: Boolean
}

interface AutomationPlugin : Plugin {
    fun onEvent(event: AutomationEvent)
    fun registerTrigger(trigger: AutomationTrigger)
}
```

#### Built-in Plugins

| Plugin | Lines | Purpose |
|--------|-------|---------|
| `DefaultAutomationPlugin` | 612 | Default automation |
| `FilecoinCloudProviderPlugin` | 298 | Filecoin provider |
| `LocalCloudProvider` | 234 | Local storage provider |
| `RcloneS3Plugin` | 118 | Rclone S3 backend |
| `RcloneGoogleDrivePlugin` | 134 | Rclone GDrive backend |
| `RcloneDropboxPlugin` | 112 | Rclone Dropbox backend |

---

### 13. `com.obsidianbackup.gaming`

**Purpose**: Gaming-specific features

**Files**: 6 | **LOC**: 1,718 | **Layer**: Feature

#### Classes

| Class | Lines | Purpose |
|-------|-------|---------|
| `GamingBackupManager` | 412 | Gaming backup orchestration |
| `SaveStateManager` | 248 | Save state management |
| `RomScanner` | 298 | ROM detection and scanning |
| `EmulatorDetector` | 267 | Emulator detection |
| `PlayGamesCloudSync` | 356 | Play Games Cloud sync |
| `GamingModels` | 137 | Gaming data models |

#### Key Methods

```kotlin
class GamingBackupManager {
    suspend fun scanInstalledGames(): List<GameInfo>
    suspend fun backupGameSaves(game: GameInfo): Result<BackupResult>
    suspend fun restoreGameSaves(backup: GameBackup): Result<RestoreResult>
}

class EmulatorDetector {
    fun detectInstalledEmulators(): List<EmulatorInfo>
    fun getSavePaths(emulator: EmulatorInfo): List<Path>
}
```

---

### 14. `com.obsidianbackup.health`

**Purpose**: Health Connect integration

**Files**: 5 | **LOC**: 1,781 | **Layer**: Feature

#### Classes

| Class | Lines | Purpose |
|-------|-------|---------|
| `HealthConnectManager` | 498 | Health Connect API integration |
| `HealthDataStore` | 387 | Health data persistence |
| `HealthDataExporter` | 423 | Health data export |
| `HealthPrivacyScreen` | 278 | Privacy UI |
| `HealthPrivacyViewModel` | 195 | Privacy view model |

#### Supported Data Types

- Steps, distance, calories
- Heart rate, blood pressure
- Sleep data
- Workout sessions
- Nutrition data
- Body measurements

---

### 15. `com.obsidianbackup.ml`

**Purpose**: Machine learning and AI

**Files**: 7 | **LOC**: 2,462 | **Layer**: Feature

#### Classes

| Class | Lines | Purpose |
|-------|-------|---------|
| `SmartScheduler` | 558 | ML-driven backup scheduling |
| `BackupPredictor` | 413 | Backup prediction engine |
| `UserHabitModel` | 232 | User behavior model |
| `BackupPrediction` | 198 | Prediction data models |
| `ContextAwareManager` | 359 | Context-aware decisions |
| `BackupAnalytics` | 372 | Analytics collection |
| `NaturalLanguageProcessor` | 330 | NLP for commands |

#### Key Methods

```kotlin
class SmartScheduler {
    suspend fun predictOptimalBackupTime(): Instant
    suspend fun shouldBackupNow(context: BackupContext): Boolean
    fun learnFromUserBehavior(action: UserAction)
}
```

---

### 16. `com.obsidianbackup.storage`

**Purpose**: Local data persistence

**Files**: 13 | **LOC**: 2,205 | **Layer**: Data

#### Room Entities

| Entity | Lines | Purpose |
|--------|-------|---------|
| `AppBackupEntity` | 98 | Backup metadata |
| `BackupScheduleEntity` | 87 | Scheduled backup |
| `SettingsEntity` | 72 | App settings |

#### DAOs

| DAO | Lines | Purpose |
|-----|-------|---------|
| `AppBackupDao` | 127 | Backup operations |
| `BackupScheduleDao` | 112 | Schedule operations |
| `SettingsDao` | 89 | Settings operations |

#### File System

| Class | Lines | Purpose |
|-------|-------|---------|
| `FileSystemManager` | 356 | File operations |
| `MediaStoreHelper` | 234 | MediaStore API |
| `SafHelper` | 198 | Storage Access Framework |
| `ScopedStorageMigration` | 267 | Android 11+ migration |
| `StoragePermissionHelper` | 168 | Permission management |
| `BackupCatalog` | 289 | Backup catalog |

---

### 17. `com.obsidianbackup.billing`

**Purpose**: Monetization and subscriptions

**Files**: 10 | **LOC**: 2,055 | **Layer**: Feature

#### Classes

| Class | Lines | Purpose |
|-------|-------|---------|
| `BillingManager` | 182 | Billing coordination |
| `SubscriptionManager` | 218 | Subscription management |
| `BillingRepository` | 127 | Billing data layer |
| `FeatureGateService` | 156 | Feature access control |
| `ProFeatureGate` | 134 | Pro feature gating |
| `RevenueAnalytics` | 198 | Revenue tracking |
| `SubscriptionScreen` | 456 | Subscription UI |
| `SubscriptionViewModel` | 289 | Subscription VM |

---

### 18. `com.obsidianbackup.tasker`

**Purpose**: Tasker automation integration

**Files**: 4 | **LOC**: 1,250 | **Layer**: Integration

#### Classes

| Class | Lines | Purpose |
|-------|-------|---------|
| `TaskerIntegration` | 634 | Main Tasker integration |
| `TaskerEventPublisher` | 267 | Event publishing |
| `TaskerStatusProvider` | 311 | Status queries |
| `TaskerPluginActions` | 219 | Tasker action definitions |

---

### 19. `com.obsidianbackup.deeplink`

**Purpose**: Deep link support

**Files**: 11 | **LOC**: 2,057 | **Layer**: Integration

#### Classes

| Class | Lines | Purpose |
|-------|-------|---------|
| `DeepLinkHandler` | 278 | Main deep link handler |
| `DeepLinkParser` | 245 | URL parsing |
| `DeepLinkRouter` | 312 | Routing logic |
| `DeepLinkGenerator` | 189 | Link generation |
| `DeepLinkAuthenticator` | 234 | Deep link authentication |
| `DeepLinkAnalytics` | 143 | Analytics tracking |

---

## Platform Packages

### TV Module (`com.obsidianbackup.tv`)

**Files**: 13 | **LOC**: ~2,000

```
tv/
├── TVApplication.kt             # TV app class
├── ui/                          # TV UI (9 files)
├── backup/                      # TV backup manager
├── settings/                    # TV settings
└── navigation/                  # TV navigation
```

### Wear Module (`com.obsidianbackup.wear`)

**Files**: 16 | **LOC**: ~2,500

```
wear/
├── WearBackupApplication.kt     # Wear app class
├── complications/               # Watch complications (2 files)
├── tiles/                       # Wear OS tiles (2 files)
├── data/                        # Data layer (2 files)
├── presentation/                # Wear UI (8 files)
├── di/                          # DI module
└── utils/                       # Utilities (2 files)
```

### Enterprise Backend (`com.obsidianbackup.enterprise`)

**Files**: 25 | **LOC**: ~5,000

```
enterprise/
├── Application.kt               # Server entry
├── auth/                        # Authentication (2 files)
├── database/                    # Database (2 files)
├── models/                      # Data models
├── plugins/                     # Ktor plugins (4 files)
├── routes/                      # API routes (7 files)
└── services/                    # Business services (6 files)
```

---

## Package Dependencies

### Dependency Graph

```
┌─────────────────────────────────────────────────────┐
│                  Presentation                       │
│  (ui, presentation, screens, components)            │
└───────────┬───────────────────────┬─────────────────┘
            │                       │
            │                       │
┌───────────▼────────┐   ┌─────────▼────────┐
│      Domain        │   │     Features     │
│  (usecase, backup) │   │ (gaming, health) │
└───────────┬────────┘   └─────────┬────────┘
            │                       │
            │        ┌──────────────┘
            │        │
┌───────────▼────────▼──────────────┐
│          Engine & Data            │
│  (engine, cloud, storage)         │
└───────────┬───────────────────────┘
            │
┌───────────▼───────────────────────┐
│       Infrastructure              │
│  (crypto, security, di)           │
└───────────────────────────────────┘
```

### Package Coupling Matrix

| Package | Depends On | Used By |
|---------|-----------|---------|
| **engine** | crypto, storage, model | domain, presentation, work |
| **cloud** | crypto, oauth, engine | engine, plugins, presentation |
| **crypto** | Android Keystore | engine, cloud, storage, security |
| **ui** | presentation, domain, deeplink | - |
| **presentation** | domain, repository | ui |
| **domain** | engine, repository | presentation, ui |
| **storage** | crypto | engine, repository |
| **plugins** | engine, cloud, api | - |
| **gaming** | engine, storage, cloud | presentation |
| **health** | engine, storage, crypto | presentation |
| **ml** | storage, analytics | engine |
| **security** | crypto, storage | engine, tasker |

---

## Statistics by Package

### LOC Distribution (Top 25)

| Rank | Package | Files | LOC | % of Total |
|------|---------|-------|-----|-----------|
| 1 | `ui/screens/` | 26 | 7,200 | 12.4% |
| 2 | `cloud/providers/` | 6 | 4,725 | 8.1% |
| 3 | `security/` | 11 | 3,781 | 6.5% |
| 4 | `crypto/` | 7 | 3,033 | 5.2% |
| 5 | `cloud/` | 8 | 2,913 | 5.0% |
| 6 | `ml/` | 7 | 2,462 | 4.2% |
| 7 | `ui/components/` | 6 | 2,300 | 4.0% |
| 8 | `storage/` | 13 | 2,205 | 3.8% |
| 9 | `deeplink/` | 11 | 2,057 | 3.5% |
| 10 | `engine/` | 10 | 2,059 | 3.5% |
| 11 | `billing/` | 10 | 2,055 | 3.5% |
| 12 | `health/` | 5 | 1,781 | 3.1% |
| 13 | `gaming/` | 6 | 1,718 | 3.0% |
| 14 | `plugins/builtin/` | 7 | 1,686 | 2.9% |
| 15 | `cloud/rclone/` | 7 | 1,351 | 2.3% |
| 16 | `sync/` | 4 | 1,281 | 2.2% |
| 17 | `tasker/` | 4 | 1,250 | 2.2% |
| 18 | `performance/` | 7 | 1,220 | 2.1% |
| 19 | `ui/screens/community/` | 5 | 1,206 | 2.1% |
| 20 | `community/` | 9 | 1,072 | 1.8% |
| 21 | `ui/screens/syncthing/` | 3 | 1,019 | 1.8% |
| 22 | `di/` | 6 | 818 | 1.4% |
| 23 | `presentation/` | 8 | 612 | 1.1% |
| 24 | `permissions/` | 2 | 593 | 1.0% |
| 25 | `api/plugin/` | 6 | 681 | 1.2% |

**Total Top 25**: ~48,820 LOC (84% of codebase)

### Package Complexity Scores

| Package | Avg Complexity | Classes | Interfaces | Data Classes |
|---------|---------------|---------|-----------|--------------|
| **engine** | 8.5 | 10 | 2 | 15 |
| **cloud** | 12.3 | 8 | 1 | 20 |
| **crypto** | 15.2 | 7 | 0 | 8 |
| **ui** | 6.2 | 45 | 5 | 30 |
| **plugins** | 9.5 | 30 | 10 | 12 |
| **gaming** | 9.3 | 6 | 1 | 8 |
| **health** | 10.1 | 5 | 1 | 15 |
| **ml** | 11.5 | 7 | 0 | 10 |

---

## Summary

- **Total Packages**: 85+
- **Total Classes**: 383
- **Total Interfaces**: 63
- **Total Data Classes**: 432
- **Total Objects**: 62
- **Total LOC**: 82,862
- **Average LOC per Package**: ~975
- **Average LOC per File**: ~160

**Architecture Quality**: ✅ Excellent
- Clear separation of concerns
- Low coupling between layers
- High cohesion within packages
- Well-defined interfaces
- Extensive use of dependency injection

---

*This document provides detailed package-level documentation. For file listings, see `COMPLETE_FILE_TREE.md`. For feature organization, see `FILE_TREE_BY_FEATURE.md`. For project overview, see `FILE_TREE_SUMMARY.md`.*
