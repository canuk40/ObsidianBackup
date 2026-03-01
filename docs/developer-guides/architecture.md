# Architecture Overview

## System Architecture

ObsidianBackup follows a modular, layered architecture designed for maintainability, testability, and extensibility.

## Architecture Layers

```
┌─────────────────────────────────────────────┐
│           Presentation Layer                │
│  (Jetpack Compose UI, ViewModels, Screens)  │
├─────────────────────────────────────────────┤
│            Domain Layer                     │
│  (Use Cases, Business Logic, Entities)      │
├─────────────────────────────────────────────┤
│             Data Layer                      │
│   (Repositories, Data Sources, DAOs)        │
├─────────────────────────────────────────────┤
│          Infrastructure Layer               │
│  (Android APIs, File System, Network, DB)   │
└─────────────────────────────────────────────┘
```

## Core Components

### 1. Presentation Layer

**Technology:** Jetpack Compose, Material Design 3

**Components:**
- **Screens**: UI screens (Dashboard, Apps, Backups, Settings, etc.)
- **ViewModels**: State management and business logic coordination
- **UI Components**: Reusable composables
- **Navigation**: Jetpack Navigation Compose

**Key Files:**
```
app/src/main/java/com/titanbackup/ui/
├── screens/
│   ├── DashboardScreen.kt
│   ├── AppsScreen.kt
│   ├── BackupsScreen.kt
│   ├── AutomationScreen.kt
│   └── SettingsScreen.kt
├── components/
│   ├── AppCard.kt
│   ├── BackupCard.kt
│   └── StatusIndicator.kt
└── theme/
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

**Design Patterns:**
- MVVM (Model-View-ViewModel)
- Unidirectional Data Flow
- Single Source of Truth
- Repository Pattern

### 2. Domain Layer

**Purpose:** Business logic and use cases

**Components:**
- **Use Cases**: Single-responsibility business operations
- **Entities**: Core business objects
- **Repositories (Interfaces)**: Data access contracts
- **Domain Models**: Business data structures

**Key Packages:**
```
domain/
├── usecases/
│   ├── backup/
│   │   ├── CreateBackupUseCase.kt
│   │   ├── RestoreBackupUseCase.kt
│   │   └── VerifyBackupUseCase.kt
│   ├── cloud/
│   │   ├── SyncToCloudUseCase.kt
│   │   └── DownloadFromCloudUseCase.kt
│   └── automation/
│       ├── ExecuteScheduleUseCase.kt
│       └── TriggerEventUseCase.kt
├── models/
│   ├── Backup.kt
│   ├── App.kt
│   ├── Schedule.kt
│   └── CloudProvider.kt
└── repositories/
    ├── BackupRepository.kt
    ├── AppRepository.kt
    └── CloudRepository.kt
```

**Principles:**
- Clean Architecture
- Dependency Inversion
- Single Responsibility
- Testability

### 3. Data Layer

**Purpose:** Data access and persistence

**Components:**
- **Repositories (Implementations)**: Data access logic
- **Data Sources**: Local and remote data sources
- **DAOs**: Room database access objects
- **API Services**: Network API interfaces

**Key Packages:**
```
data/
├── repositories/
│   ├── BackupRepositoryImpl.kt
│   ├── AppRepositoryImpl.kt
│   └── CloudRepositoryImpl.kt
├── local/
│   ├── database/
│   │   ├── AppDatabase.kt
│   │   ├── BackupDao.kt
│   │   └── entities/
│   └── preferences/
│       └── SettingsDataStore.kt
├── remote/
│   ├── gdrive/
│   │   └── GoogleDriveApi.kt
│   ├── webdav/
│   │   └── WebDAVClient.kt
│   └── rclone/
│       └── RcloneApi.kt
└── models/
    ├── BackupEntity.kt
    └── AppEntity.kt
```

### 4. Infrastructure Layer

**Purpose:** Platform-specific implementations

**Components:**
- **Backup Engine**: Core backup/restore logic
- **Cloud Sync**: Cloud provider integrations
- **Automation**: Scheduling and triggers
- **Security**: Encryption and authentication
- **Storage**: File system operations

## Key Subsystems

### Backup Engine

**Responsibility:** Create and restore backups

```
engine/
├── BackupEngine.kt           // Main backup orchestration
├── RestoreEngine.kt          // Main restore orchestration
├── strategies/
│   ├── FullBackupStrategy.kt
│   ├── IncrementalBackupStrategy.kt
│   └── DifferentialBackupStrategy.kt
├── formats/
│   ├── TarArchiveFormat.kt
│   ├── ZipArchiveFormat.kt
│   └── ArchiveFormatRegistry.kt
├── compression/
│   ├── GzipCompression.kt
│   ├── Bzip2Compression.kt
│   └── XzCompression.kt
└── verification/
    ├── MerkleTreeVerifier.kt
    ├── ChecksumVerifier.kt
    └── IntegrityChecker.kt
```

**Process Flow:**
```
1. Select apps to backup
2. Determine backup strategy (full/incremental)
3. For each app:
   a. Extract APK (if requested)
   b. Copy app data
   c. Calculate checksums
   d. Update Merkle tree
4. Create archive
5. Compress (optional)
6. Encrypt (optional)
7. Store locally
8. Sync to cloud (if configured)
9. Verify integrity
10. Update metadata
```

### Cloud Sync System

**Responsibility:** Synchronize backups with cloud storage

```
cloud/
├── CloudSyncManager.kt       // Orchestrates cloud operations
├── providers/
│   ├── GoogleDriveProvider.kt
│   ├── WebDAVProvider.kt
│   ├── RcloneProvider.kt
│   └── CloudProvider.kt      // Interface
├── upload/
│   ├── ChunkedUploader.kt
│   ├── ParallelUploader.kt
│   └── ResumableUploader.kt
├── download/
│   ├── StreamingDownloader.kt
│   └── CachedDownloader.kt
└── sync/
    ├── SyncStrategy.kt
    ├── ConflictResolver.kt
    └── QueueManager.kt
```

**Sync Strategies:**
- **Primary/Secondary**: Upload to primary, fallback to secondary
- **Redundant**: Upload to all providers
- **Load Balanced**: Distribute across providers

### Automation System

**Responsibility:** Schedule and execute automated backups

```
automation/
├── AutomationManager.kt      // Main automation orchestrator
├── scheduler/
│   ├── WorkManagerScheduler.kt
│   ├── Schedule.kt
│   └── ScheduleExecutor.kt
├── triggers/
│   ├── EventTrigger.kt
│   ├── TimeTrigger.kt
│   └── ConditionTrigger.kt
├── plugins/
│   ├── AutomationPlugin.kt   // Plugin interface
│   ├── PluginManager.kt
│   ├── PluginLoader.kt
│   └── builtin/
│       ├── DefaultAutomationPlugin.kt
│       ├── SmartBackupPlugin.kt
│       └── BatteryAwarePlugin.kt
└── conditions/
    ├── BatteryCondition.kt
    ├── NetworkCondition.kt
    └── StorageCondition.kt
```

**Automation Flow:**
```
1. WorkManager triggers scheduled task
2. Check preconditions (battery, network, etc.)
3. Load automation plugin
4. Plugin determines if backup should run
5. Execute backup with plugin configuration
6. Handle success/failure
7. Update next run time
8. Send notifications
```

### Security System

**Responsibility:** Authentication and encryption

```
security/
├── authentication/
│   ├── BiometricAuthenticator.kt
│   ├── BiometricPromptBuilder.kt
│   ├── PasskeyManager.kt
│   └── CredentialManager.kt
├── encryption/
│   ├── BackupEncryptor.kt
│   ├── AESEncryption.kt
│   ├── KeyGenerator.kt
│   └── KeystoreManager.kt
├── verification/
│   ├── IntegrityVerifier.kt
│   └── SignatureVerifier.kt
└── keystore/
    ├── AndroidKeystore.kt
    └── EncryptedPreferences.kt
```

**Security Features:**
- Hardware-backed key storage (Android Keystore)
- Biometric authentication (BiometricPrompt)
- AES-256 encryption
- Merkle tree integrity verification
- Passkey support (Android 14+)

### Storage System

**Responsibility:** File system operations

```
storage/
├── StorageManager.kt         // Storage orchestration
├── local/
│   ├── LocalStorageProvider.kt
│   ├── ScopedStorageHandler.kt
│   └── MediaStoreHandler.kt
├── external/
│   ├── SDCardHandler.kt
│   └── USBStorageHandler.kt
├── saf/
│   ├── SAFProvider.kt        // Storage Access Framework
│   └── DocumentTreeHandler.kt
└── cache/
    ├── CacheManager.kt
    └── TemporaryFileHandler.kt
```

## Data Flow

### Backup Creation Flow

```
UI Layer (Screen)
    ↓ User initiates backup
ViewModel
    ↓ Calls use case
CreateBackupUseCase
    ↓ Orchestrates backup
BackupRepository
    ↓ Delegates to engine
BackupEngine
    ↓ Executes strategy
BackupStrategy (Full/Incremental)
    ↓ Creates archive
ArchiveFormat (TAR/ZIP)
    ↓ Stores locally
LocalStorage
    ↓ Syncs to cloud (if configured)
CloudSyncManager
    ↓ Updates database
Room Database
    ↓ Emits result
Flow<BackupResult>
    ↓ Updates UI
ViewModel → UI
```

### Restore Flow

```
UI Layer (Screen)
    ↓ User selects restore
ViewModel
    ↓ Calls use case
RestoreBackupUseCase
    ↓ Orchestrates restore
BackupRepository
    ↓ Downloads from cloud (if needed)
CloudSyncManager
    ↓ Verifies integrity
IntegrityVerifier
    ↓ Extracts archive
ArchiveFormat
    ↓ Installs APK
PackageInstaller
    ↓ Restores data
RestoreEngine
    ↓ Updates database
Room Database
    ↓ Emits result
Flow<RestoreResult>
    ↓ Updates UI
ViewModel → UI
```

## Dependency Injection

**Framework:** Hilt (Dagger)

**Module Structure:**
```
di/
├── AppModule.kt              // App-level dependencies
├── DatabaseModule.kt         // Room database
├── NetworkModule.kt          // Retrofit, OkHttp
├── RepositoryModule.kt       // Repository bindings
├── UseCaseModule.kt          // Use case bindings
├── CloudModule.kt            // Cloud providers
├── SecurityModule.kt         // Security components
└── AutomationModule.kt       // Automation plugins
```

**Key Bindings:**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindBackupRepository(
        impl: BackupRepositoryImpl
    ): BackupRepository
    
    @Binds
    abstract fun bindCloudRepository(
        impl: CloudRepositoryImpl
    ): CloudRepository
}
```

## Threading Model

**Coroutines & Dispatchers:**

```kotlin
// Main thread (UI updates)
Dispatchers.Main

// IO operations (network, disk)
Dispatchers.IO

// CPU-intensive work (compression, encryption)
Dispatchers.Default

// Custom thread pool for backup operations
val BackupDispatcher = Executors.newFixedThreadPool(4)
    .asCoroutineDispatcher()
```

**Example Use:**
```kotlin
class BackupViewModel @Inject constructor(
    private val createBackupUseCase: CreateBackupUseCase
) : ViewModel() {
    fun createBackup(apps: List<App>) {
        viewModelScope.launch {
            createBackupUseCase(apps)
                .flowOn(Dispatchers.IO)  // Execute on IO thread
                .collect { result ->      // Collect on Main thread
                    _uiState.update { it.copy(backupResult = result) }
                }
        }
    }
}
```

## Database Schema

**Technology:** Room

**Entities:**
```kotlin
@Entity(tableName = "backups")
data class BackupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val timestamp: Long,
    val type: BackupType,
    val size: Long,
    val apps: List<String>,
    val destination: String,
    val encrypted: Boolean,
    val merkleRoot: String?
)

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val packageName: String,
    val name: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val dataSize: Long,
    val lastBackup: Long?
)

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val enabled: Boolean,
    val frequency: Frequency,
    val time: String,
    val apps: List<String>,
    val conditions: Map<String, Any>
)
```

**Relationships:**
```kotlin
data class BackupWithApps(
    @Embedded val backup: BackupEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "backupId"
    )
    val apps: List<BackupAppEntity>
)
```

## Plugin Architecture

**Plugin Interface:**
```kotlin
interface AutomationPlugin {
    val id: String
    val name: String
    val version: String
    
    suspend fun shouldTrigger(context: AutomationContext): Boolean
    suspend fun execute(context: AutomationContext)
    fun configure(): PluginConfig
}
```

**Plugin Loading:**
```kotlin
class PluginManager @Inject constructor() {
    fun loadPlugin(path: String): AutomationPlugin {
        val dexClassLoader = DexClassLoader(
            path,
            cacheDir.absolutePath,
            null,
            javaClass.classLoader
        )
        val pluginClass = dexClassLoader.loadClass(className)
        return pluginClass.newInstance() as AutomationPlugin
    }
}
```

## Error Handling

**Strategy:** Result-based error handling

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// Usage
suspend fun createBackup(): Result<Backup> {
    return try {
        val backup = backupEngine.create()
        Result.Success(backup)
    } catch (e: Exception) {
        Result.Error(e)
    }
}
```

## Testing Strategy

**Unit Tests:**
- ViewModels (business logic)
- Use Cases (domain logic)
- Repositories (data access)
- Utilities and helpers

**Integration Tests:**
- Database operations
- API integrations
- File system operations

**UI Tests:**
- Screen navigation
- User interactions
- Form validation

**Test Structure:**
```
test/
├── unit/
│   ├── viewmodels/
│   ├── usecases/
│   └── repositories/
├── integration/
│   ├── database/
│   └── api/
└── ui/
    └── screens/
```

## Performance Considerations

**Optimization Strategies:**
1. **Lazy Loading**: Load data on demand
2. **Pagination**: Page large lists
3. **Caching**: Cache frequently accessed data
4. **Background Processing**: Use WorkManager
5. **Parallel Operations**: Multiple threads for I/O
6. **Memory Management**: Release resources promptly
7. **Database Indexing**: Index frequently queried fields

## Security Considerations

**Security Measures:**
1. **Encryption at Rest**: AES-256 for local backups
2. **Encryption in Transit**: TLS for cloud uploads
3. **Key Storage**: Android Keystore (hardware-backed)
4. **Authentication**: Biometric + device credential
5. **Input Validation**: Sanitize all inputs
6. **Secure Communication**: Certificate pinning
7. **Code Obfuscation**: ProGuard/R8

## Scalability

**Design for Scale:**
- Modular architecture (add features independently)
- Plugin system (extend functionality)
- Concurrent operations (handle multiple backups)
- Cloud providers (add new providers)
- Platform support (Android TV, Wear OS)

## Next Steps

- [Plugin Development Guide](plugin-development.md)
- [Testing Guide](testing.md)
- [Contributing Guidelines](contributing.md)
