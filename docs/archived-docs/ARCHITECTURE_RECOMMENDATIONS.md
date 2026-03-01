# ObsidianBackup - Architecture Recommendations

**Date:** 2024  
**Goal:** Achieve 9.5/10 Clean Architecture compliance  
**Current Score:** 8.2/10

---

## Priority 1: Critical Fixes (Weeks 1-2)

### 1.1 Eliminate Layer Violations

#### Create CatalogRepository Interface

**Problem:** Domain/UI directly depends on `BackupCatalog` storage implementation

**Solution:**
```kotlin
// Step 1: Create interface in domain layer
// File: domain/repository/ICatalogRepository.kt

package com.obsidianbackup.domain.repository

interface ICatalogRepository {
    suspend fun saveBackup(metadata: BackupMetadata): Result<Unit>
    suspend fun getBackup(id: BackupId): Result<BackupMetadata?>
    suspend fun getAllBackups(): Flow<List<BackupMetadata>>
    suspend fun deleteBackup(id: BackupId): Result<Unit>
    suspend fun updateBackup(metadata: BackupMetadata): Result<Unit>
    suspend fun getBackupsByApp(packageName: String): Flow<List<BackupMetadata>>
}

// Step 2: Implement in data layer
// File: data/repository/CatalogRepositoryImpl.kt

package com.obsidianbackup.data.repository

@Singleton
class CatalogRepositoryImpl @Inject constructor(
    private val backupCatalog: BackupCatalog  // Storage implementation
) : ICatalogRepository {
    
    override suspend fun saveBackup(metadata: BackupMetadata): Result<Unit> {
        return try {
            backupCatalog.saveBackupMetadata(metadata)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun getBackup(id: BackupId): Result<BackupMetadata?> {
        return try {
            val backup = backupCatalog.getBackupMetadata(id)
            Result.Success(backup)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override fun getAllBackups(): Flow<List<BackupMetadata>> {
        return backupCatalog.observeAllBackups()
    }
    
    // ... implement other methods
}

// Step 3: Update DI module
// File: di/RepositoryModule.kt

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindCatalogRepository(
        impl: CatalogRepositoryImpl
    ): ICatalogRepository
}

// Step 4: Update consumers
// File: domain/backup/BackupOrchestrator.kt

class BackupOrchestrator @Inject constructor(
    private val catalogRepository: ICatalogRepository,  // ✅ Use interface
    private val engineFactory: BackupEngineFactory,
    private val eventBus: BackupEventBus
) {
    suspend fun backupApps(request: BackupRequest): BackupResult {
        // Use catalogRepository instead of catalog
        catalogRepository.saveBackup(metadata)
    }
}

// File: ui/screens/EnhancedBackupsScreen.kt

@Composable
fun EnhancedBackupsScreen(
    viewModel: BackupsViewModel = hiltViewModel()
    // ✅ Remove catalog parameter completely
) {
    val backups by viewModel.backups.collectAsState()
    // Use backups from ViewModel state
}

// Update ViewModel
@HiltViewModel
class BackupsViewModel @Inject constructor(
    private val catalogRepository: ICatalogRepository
) : ViewModel() {
    val backups: StateFlow<List<BackupInfo>> = 
        catalogRepository.getAllBackups()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
```

**Files to Modify:**
1. Create `domain/repository/ICatalogRepository.kt`
2. Create `data/repository/CatalogRepositoryImpl.kt`
3. Update `di/RepositoryModule.kt`
4. Update `domain/backup/BackupOrchestrator.kt` (line 9)
5. Update `domain/backup/BackupEngineFactory.kt` (line 11)
6. Update `ui/screens/EnhancedBackupsScreen.kt` (line 37)
7. Update `presentation/backups/BackupsViewModel.kt`

**Impact:** Fixes 3 critical layer violations

---

### 1.2 Implement Global Coroutine Exception Handling

**Problem:** 100+ `launch {}` blocks without exception handlers

**Solution:**
```kotlin
// Step 1: Create global exception handler
// File: app/ObsidianBackupApp.kt

class ObsidianBackupApp : Application(), Configuration.Provider {
    
    @Inject
    lateinit var logger: TitanLogger
    
    @Inject
    lateinit var crashlytics: CrashlyticsManager
    
    val globalExceptionHandler by lazy {
        CoroutineExceptionHandler { context, throwable ->
            logger.e("UNCAUGHT_COROUTINE", "Exception in coroutine: $context", throwable)
            crashlytics.recordException(throwable)
            
            // Show user-friendly notifications for critical errors
            when (throwable) {
                is BackupFailureException -> {
                    notifyBackupError(throwable)
                }
                is CloudSyncException -> {
                    notifyCloudSyncError(throwable)
                }
                is StorageException -> {
                    notifyStorageError(throwable)
                }
            }
        }
    }
    
    private fun notifyBackupError(e: BackupFailureException) {
        // Show notification to user
        val notificationManager = getSystemService<NotificationManager>()
        // ... create and show notification
    }
}

// Step 2: Create injectable exception handler
// File: di/AppModule.kt

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideCoroutineExceptionHandler(
        logger: TitanLogger,
        crashlytics: CrashlyticsManager
    ): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { context, throwable ->
            logger.e("COROUTINE_ERROR", "Uncaught exception: $context", throwable)
            crashlytics.recordException(throwable)
        }
    }
    
    @Provides
    @Singleton
    @Named("ApplicationScope")
    fun provideApplicationScope(
        exceptionHandler: CoroutineExceptionHandler
    ): CoroutineScope {
        return CoroutineScope(
            SupervisorJob() + 
            Dispatchers.Default + 
            exceptionHandler
        )
    }
}

// Step 3: Use in ViewModels
// File: presentation/backup/BackupViewModel.kt

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupAppsUseCase: BackupAppsUseCase,
    private val exceptionHandler: CoroutineExceptionHandler
) : ViewModel() {
    
    fun startBackup(request: BackupRequest) {
        viewModelScope.launch(exceptionHandler) {  // ✅ Add handler
            _state.update { it.copy(isBackingUp = true) }
            
            val result = backupAppsUseCase(request)
            
            _state.update { 
                it.copy(
                    isBackingUp = false,
                    backupResult = result
                )
            }
        }
    }
}

// Step 4: Use in background scopes
// File: cloud/CloudSyncManager.kt

@Singleton
class CloudSyncManager @Inject constructor(
    @Named("ApplicationScope") private val applicationScope: CoroutineScope,
    // exceptionHandler already included in scope
) {
    fun startBackgroundSync() {
        applicationScope.launch {  // Handler already attached
            syncToCloud()
        }
    }
}
```

**Files to Modify:**
1. Update `app/ObsidianBackupApp.kt`
2. Update `di/AppModule.kt` (add exception handler provider)
3. Update all ViewModels to inject `CoroutineExceptionHandler`
4. Update `CloudSyncManager`, `BackupScheduler`, etc. to use scoped exception handler

**Impact:** Catches all uncaught coroutine exceptions

---

### 1.3 Fix Empty Catch Blocks

**Problem:** 11 instances of silent exception swallowing

**Solution:**
```kotlin
// File: plugins/PluginManager.kt (line 61)

// ❌ BEFORE
try {
    val plugin = pluginLoader.loadPlugin(pluginClass)
    registry.register(plugin)
} catch (_: Exception) {
    // ignore
}

// ✅ AFTER
try {
    val plugin = pluginLoader.loadPlugin(pluginClass)
    registry.register(plugin)
} catch (e: PluginLoadException) {
    logger.e(TAG, "Failed to load plugin: ${pluginClass.name}", e)
    analyticsManager.recordError("plugin_load_failure", mapOf(
        "plugin_class" to pluginClass.name,
        "error" to e.message
    ))
    // Notify observer of failure
    _pluginLoadErrors.tryEmit(PluginLoadError(pluginClass, e))
} catch (e: SecurityException) {
    logger.e(TAG, "Security violation loading plugin: ${pluginClass.name}", e)
    // Security issues are critical - report to crashlytics
    crashlytics.recordException(e)
} catch (e: Exception) {
    logger.e(TAG, "Unexpected error loading plugin: ${pluginClass.name}", e)
    crashlytics.recordException(e)
}

// File: plugin/PluginAPI.kt (line 69)

// ❌ BEFORE
catch (_: Exception) {
}

// ✅ AFTER
catch (e: CloudSyncException) {
    logger.e(TAG, "Cloud sync failed for backup: $backupId", e)
    return CloudResult.Error("Sync failed: ${e.message}")
} catch (e: NetworkException) {
    logger.w(TAG, "Network error during cloud sync", e)
    return CloudResult.Error("Network error: Check connection")
} catch (e: Exception) {
    logger.e(TAG, "Unexpected error in cloud sync", e)
    crashlytics.recordException(e)
    return CloudResult.Error("Unexpected error: ${e.message}")
}
```

**Search and Replace Pattern:**
```kotlin
// Find all instances:
grep -rn "catch.*Exception.*{.*}" app/src/main/java --include="*.kt" | grep -v "//"

// For each instance:
1. Identify the operation being performed
2. Add specific exception types (IOException, SecurityException, etc.)
3. Add logging with context
4. Add metrics/analytics tracking
5. Consider recovery strategies (retry, fallback, user notification)
6. Add crashlytics for unexpected exceptions
```

**Impact:** All errors properly logged and tracked

---

## Priority 2: Important Improvements (Weeks 3-4)

### 2.1 Add Missing DI Annotations

**Problem:** 2 ViewModels + 3 UseCases missing annotations

**Solution:**
```kotlin
// File: ui/cloud/CloudProviderConfigScreen.kt (line 329)

// ❌ BEFORE
class CloudConfigViewModel : ViewModel() {
    // ...
}

// ✅ AFTER
@HiltViewModel
class CloudConfigViewModel @Inject constructor(
    private val cloudProviderRepository: CloudProviderRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    init {
        // Load saved provider config
        val providerId = savedStateHandle.get<String>("provider_id")
        loadProviderConfig(providerId)
    }
    
    private fun loadProviderConfig(providerId: String?) {
        viewModelScope.launch {
            // Load config
        }
    }
}

// File: ui/components/LiveBackupConsole.kt (line 282)

// ❌ BEFORE
class RestoreSimulationViewModel @javax.inject.Inject constructor() : ViewModel()

// ✅ AFTER
@HiltViewModel
class RestoreSimulationViewModel @Inject constructor() : ViewModel()

// File: domain/usecase/BackupAppsUseCase.kt

// ❌ BEFORE
class BackupAppsUseCase(
    private val backupOrchestrator: BackupOrchestrator
) { ... }

// ✅ AFTER
class BackupAppsUseCase @Inject constructor(
    private val backupOrchestrator: BackupOrchestrator
) { ... }

// Repeat for RestoreAppsUseCase.kt and VerifySnapshotUseCase.kt
```

**Impact:** All components properly injectable via Hilt

---

### 2.2 Extract Business Logic from ViewModels

#### Create Formatter Utilities

```kotlin
// File: util/DateFormatter.kt

@Singleton
class DateFormatter @Inject constructor() {
    
    fun formatRelativeTime(timestamp: Long): String {
        if (timestamp == 0L) return "Never"
        
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < DateUtils.MINUTE_IN_MILLIS -> 
                "Just now"
            diff < DateUtils.HOUR_IN_MILLIS -> 
                "${diff / DateUtils.MINUTE_IN_MILLIS} minutes ago"
            diff < DateUtils.DAY_IN_MILLIS -> 
                "${diff / DateUtils.HOUR_IN_MILLIS} hours ago"
            diff < DateUtils.WEEK_IN_MILLIS ->
                "${diff / DateUtils.DAY_IN_MILLIS} days ago"
            else -> 
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    .format(Date(timestamp))
        }
    }
    
    fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "$hours h ${minutes % 60} m"
            minutes > 0 -> "$minutes m ${seconds % 60} s"
            else -> "$seconds s"
        }
    }
}

// File: util/SizeFormatter.kt

@Singleton
class SizeFormatter @Inject constructor() {
    
    fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> "%.2f GB".format(gb)
            mb >= 1 -> "%.2f MB".format(mb)
            kb >= 1 -> "%.2f KB".format(kb)
            else -> "$bytes B"
        }
    }
    
    fun formatBytesShort(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> "%.1f GB".format(gb)
            mb >= 1 -> "%.0f MB".format(mb)
            kb >= 1 -> "%.0f KB".format(kb)
            else -> "${bytes}B"
        }
    }
}

// Update DashboardViewModel
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dateFormatter: DateFormatter,
    private val sizeFormatter: SizeFormatter,
    // ... other dependencies
) : ViewModel() {
    
    val lastBackupText: String get() = 
        dateFormatter.formatRelativeTime(lastBackup?.timestamp ?: 0)
    
    val totalSizeText: String get() = 
        sizeFormatter.formatBytes(totalBackupSize)
    
    // No formatting logic in ViewModel anymore ✅
}
```

#### Create Permission UseCase

```kotlin
// File: domain/usecase/RequestHealthPermissionsUseCase.kt

class RequestHealthPermissionsUseCase @Inject constructor(
    private val healthConnectManager: HealthConnectManager
) {
    suspend operator fun invoke(
        dataTypes: Set<HealthDataType>
    ): Result<PermissionStatus> {
        return try {
            val requiredPermissions = healthConnectManager
                .getRequiredPermissions(dataTypes)
            
            if (requiredPermissions.isEmpty()) {
                return Result.Success(PermissionStatus.Granted(dataTypes))
            }
            
            val grantResult = healthConnectManager
                .requestPermissions(requiredPermissions)
            
            if (grantResult.allGranted) {
                Result.Success(PermissionStatus.Granted(dataTypes))
            } else {
                Result.Success(
                    PermissionStatus.PartiallyGranted(
                        granted = grantResult.grantedPermissions,
                        denied = grantResult.deniedPermissions
                    )
                )
            }
        } catch (e: SecurityException) {
            Result.Error(e)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    sealed class PermissionStatus {
        data class Granted(val dataTypes: Set<HealthDataType>) : PermissionStatus()
        data class PartiallyGranted(
            val granted: Set<String>,
            val denied: Set<String>
        ) : PermissionStatus()
        object Denied : PermissionStatus()
    }
}

// Update HealthViewModel
@HiltViewModel
class HealthViewModel @Inject constructor(
    private val requestPermissionsUseCase: RequestHealthPermissionsUseCase,
    private val backupHealthDataUseCase: BackupHealthDataUseCase
) : ViewModel() {
    
    fun requestPermissions() {
        viewModelScope.launch {
            _state.update { it.copy(isRequestingPermissions = true) }
            
            val result = requestPermissionsUseCase(
                setOf(
                    HealthDataType.STEPS,
                    HealthDataType.HEART_RATE,
                    HealthDataType.SLEEP
                )
            )
            
            _state.update { 
                it.copy(
                    isRequestingPermissions = false,
                    permissionStatus = result
                )
            }
        }
    }
}
```

#### Create Game Selection UseCase

```kotlin
// File: domain/usecase/ManageGameSelectionUseCase.kt

class ManageGameSelectionUseCase @Inject constructor() {
    
    fun toggleSelection(
        currentSelection: List<GameInfo>,
        game: GameInfo
    ): List<GameInfo> {
        return if (game in currentSelection) {
            currentSelection - game
        } else {
            currentSelection + game
        }
    }
    
    fun selectAll(
        availableGames: List<GameInfo>
    ): List<GameInfo> {
        return availableGames
    }
    
    fun deselectAll(): List<GameInfo> {
        return emptyList()
    }
    
    fun selectByType(
        availableGames: List<GameInfo>,
        gameType: GameType
    ): List<GameInfo> {
        return availableGames.filter { it.type == gameType }
    }
}

// Update GamingViewModel
@HiltViewModel
class GamingViewModel @Inject constructor(
    private val manageGameSelectionUseCase: ManageGameSelectionUseCase,
    private val gamingBackupManager: GamingBackupManager
) : ViewModel() {
    
    fun toggleGameSelection(game: GameInfo) {
        val newSelection = manageGameSelectionUseCase.toggleSelection(
            _state.value.selectedGames,
            game
        )
        _state.update { it.copy(selectedGames = newSelection) }
    }
    
    fun selectAllGames() {
        val newSelection = manageGameSelectionUseCase.selectAll(
            _state.value.availableGames
        )
        _state.update { it.copy(selectedGames = newSelection) }
    }
}
```

**Impact:** ViewModels reduced to pure orchestration, testable business logic

---

### 2.3 Remove DAO from Public DI

**Solution:**
```kotlin
// File: di/AutomationModule.kt

// ❌ REMOVE THIS
@Provides
@Singleton
fun provideBackupScheduleDao(
    backupCatalog: BackupCatalog
): BackupScheduleDao {
    return backupCatalog.getScheduleDao()
}

// ✅ Only provide repository
@Provides
@Singleton
fun provideScheduleRepository(
    backupCatalog: BackupCatalog
): ScheduleRepository {
    return ScheduleRepository(
        scheduleDao = backupCatalog.getScheduleDao()  // Private to module
    )
}

// Update any consumers of BackupScheduleDao to use ScheduleRepository
```

**Files to Check:**
```bash
# Find all usages of BackupScheduleDao
grep -rn "BackupScheduleDao" app/src/main/java --include="*.kt"

# Replace with ScheduleRepository injections
```

**Impact:** DAOs no longer exposed, enforces repository pattern

---

## Priority 3: Long-term Enhancements (Weeks 5-6)

### 3.1 Add Repository Interfaces

**Pattern to Apply:**
```kotlin
// For each repository, create interface

// Step 1: Define interface in domain/repository/
interface ILogRepository {
    suspend fun getRecentLogs(limit: Int): List<LogEntry>
    suspend fun clearLogs(): Result<Unit>
    suspend fun addLog(entry: LogEntry): Result<Unit>
    fun observeLogs(): Flow<List<LogEntry>>
}

// Step 2: Rename implementation in data/repository/
class LogRepositoryImpl @Inject constructor(
    private val logDao: LogDao
) : ILogRepository {
    // Implement interface
}

// Step 3: Update DI binding
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindLogRepository(
        impl: LogRepositoryImpl
    ): ILogRepository
}

// Step 4: Update consumers
@HiltViewModel
class LogsViewModel @Inject constructor(
    private val logRepository: ILogRepository  // Interface
) : ViewModel()
```

**Repositories to Update:**
1. `LogRepository` → `ILogRepository` + `LogRepositoryImpl`
2. `AppRepository` → `IAppRepository` + `AppRepositoryImpl`
3. `SettingsRepository` → `ISettingsRepository` + `SettingsRepositoryImpl`
4. `ScheduleRepository` → `IScheduleRepository` + `ScheduleRepositoryImpl`
5. `BackupRepository` → `IBackupRepository` + `BackupRepositoryImpl`
6. `CloudProviderRepository` → `ICloudProviderRepository` + `CloudProviderRepositoryImpl`
7. `CatalogRepository` (new) → `ICatalogRepository` + `CatalogRepositoryImpl`

**Impact:** Easier testing, clearer contracts, better modularity

---

### 3.2 Hoist Composable State to ViewModels

**Pattern to Apply:**
```kotlin
// For each screen with local state

// ❌ BEFORE: Local state in Composable
@Composable
fun AppsScreen() {
    var selectedApps by remember { mutableStateOf(setOf<AppId>()) }
    var showDialog by remember { mutableStateOf(false) }
    // ...
}

// ✅ AFTER: State in ViewModel

// Step 1: Define state in ViewModel
@HiltViewModel
class AppsViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(AppsState())
    val state: StateFlow<AppsState> = _state.asStateFlow()
    
    data class AppsState(
        val selectedApps: Set<AppId> = emptySet(),
        val showBackupDialog: Boolean = false,
        val showRestoreDialog: Boolean = false,
        val installedApps: List<AppInfo> = emptyList(),
        val isLoading: Boolean = false
    )
    
    fun toggleAppSelection(appId: AppId) {
        _state.update {
            val newSelection = if (appId in it.selectedApps) {
                it.selectedApps - appId
            } else {
                it.selectedApps + appId
            }
            it.copy(selectedApps = newSelection)
        }
    }
    
    fun showBackupDialog() {
        _state.update { it.copy(showBackupDialog = true) }
    }
    
    fun hideBackupDialog() {
        _state.update { it.copy(showBackupDialog = false) }
    }
}

// Step 2: Refactor Composable
@Composable
fun AppsScreen(
    viewModel: AppsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    AppsContent(
        state = state,
        onToggleApp = viewModel::toggleAppSelection,
        onShowBackupDialog = viewModel::showBackupDialog,
        onHideBackupDialog = viewModel::hideBackupDialog
    )
}

@Composable
fun AppsContent(
    state: AppsState,
    onToggleApp: (AppId) -> Unit,
    onShowBackupDialog: () -> Unit,
    onHideBackupDialog: () -> Unit
) {
    // Pure UI, no state management
}
```

**Screens to Update (10+ screens):**
1. `AppsScreen.kt`
2. `SettingsScreen.kt`
3. `AutomationScreen.kt`
4. `GamingBackupScreen.kt`
5. `HealthPrivacyScreen.kt`
6. `PluginsScreen.kt`
7. `CloudProviderConfigScreen.kt`
8. `LogsScreen.kt`
9. `DiffScreen.kt`
10. Any other screens with local `remember { mutableStateOf(...) }`

**Impact:** Better testability, state persistence, cleaner separation

---

### 3.3 Split Complex UseCases

**Problem:** `RestoreAppsUseCase` is 107 lines with multiple responsibilities

**Solution:**
```kotlin
// Split into smaller UseCases

// File: domain/usecase/RestoreAppsUseCase.kt (simplified)
class RestoreAppsUseCase @Inject constructor(
    private val transactionalRestoreEngine: TransactionalRestoreEngine,
    private val validateRestoreUseCase: ValidateRestoreUseCase,
    private val errorRecoveryManager: ErrorRecoveryManager
) {
    suspend operator fun invoke(params: Params): Result<RestoreResult> {
        // Just orchestrate, delegate complexity
        val validation = validateRestoreUseCase(params.snapshotId)
        if (validation is Result.Error) {
            return Result.Error(validation.error)
        }
        
        return transactionalRestoreEngine.restore(params)
    }
    
    data class Params(
        val snapshotId: SnapshotId,
        val selectedApps: List<AppId>,
        val restoreData: Boolean = true,
        val restoreCache: Boolean = false
    )
}

// File: domain/usecase/ValidateRestoreUseCase.kt (new)
class ValidateRestoreUseCase @Inject constructor(
    private val catalogRepository: ICatalogRepository
) {
    suspend operator fun invoke(snapshotId: SnapshotId): Result<ValidationResult> {
        val snapshot = catalogRepository.getBackup(BackupId(snapshotId.value))
        
        return when {
            snapshot == null -> 
                Result.Error(ValidationError.SnapshotNotFound(snapshotId))
            snapshot.isCorrupted -> 
                Result.Error(ValidationError.SnapshotCorrupted(snapshotId))
            !snapshot.isCompatible -> 
                Result.Error(ValidationError.IncompatibleVersion(snapshot.version))
            else -> 
                Result.Success(ValidationResult.Valid)
        }
    }
    
    sealed class ValidationError : Exception() {
        data class SnapshotNotFound(val id: SnapshotId) : ValidationError()
        data class SnapshotCorrupted(val id: SnapshotId) : ValidationError()
        data class IncompatibleVersion(val version: Int) : ValidationError()
    }
    
    sealed class ValidationResult {
        object Valid : ValidationResult()
    }
}

// File: domain/usecase/PrepareRestoreEnvironmentUseCase.kt (new)
class PrepareRestoreEnvironmentUseCase @Inject constructor(
    private val storageManager: FileSystemManager,
    private val permissionManager: PermissionManager
) {
    suspend operator fun invoke(): Result<Unit> {
        // Check permissions
        if (!permissionManager.hasStoragePermission()) {
            return Result.Error(PermissionError.StoragePermissionDenied)
        }
        
        // Verify space
        val availableSpace = storageManager.getAvailableSpace()
        if (availableSpace < MIN_REQUIRED_SPACE) {
            return Result.Error(StorageError.InsufficientSpace(availableSpace))
        }
        
        // Create temp directory
        return try {
            storageManager.createRestoreTempDirectory()
            Result.Success(Unit)
        } catch (e: IOException) {
            Result.Error(e)
        }
    }
    
    companion object {
        private const val MIN_REQUIRED_SPACE = 100 * 1024 * 1024L // 100 MB
    }
}
```

**Pattern:**
1. Keep orchestrator UseCase thin (<50 lines)
2. Extract validation → `ValidateXUseCase`
3. Extract preparation → `PrepareXUseCase`
4. Extract cleanup → `CleanupXUseCase`
5. Extract recovery → `RecoverFromXErrorUseCase`

**Impact:** Single responsibility, easier testing, better reusability

---

### 3.4 Implement Comprehensive Testing

```kotlin
// File: app/src/test/java/com/obsidianbackup/domain/usecase/BackupAppsUseCaseTest.kt

@OptIn(ExperimentalCoroutinesApi::class)
class BackupAppsUseCaseTest {
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var backupOrchestrator: BackupOrchestrator
    private lateinit var useCase: BackupAppsUseCase
    
    @Before
    fun setup() {
        backupOrchestrator = mockk()
        useCase = BackupAppsUseCase(backupOrchestrator)
    }
    
    @Test
    fun `invoke with valid request returns success`() = runTest {
        // Given
        val request = BackupRequest(
            apps = listOf(AppId("com.example.app")),
            includeData = true
        )
        val expectedResult = BackupResult.Success(
            snapshotId = SnapshotId("snapshot-123"),
            appsBackedUp = 1
        )
        
        coEvery { backupOrchestrator.backupApps(request) } returns expectedResult
        
        // When
        val result = useCase(request)
        
        // Then
        assertThat(result).isEqualTo(expectedResult)
        coVerify(exactly = 1) { backupOrchestrator.backupApps(request) }
    }
    
    @Test
    fun `invoke with empty apps list returns error`() = runTest {
        // Given
        val request = BackupRequest(apps = emptyList())
        
        // When
        val result = useCase(request)
        
        // Then
        assertThat(result).isInstanceOf(BackupResult.Failure::class.java)
        assertThat((result as BackupResult.Failure).reason)
            .contains("No apps selected")
    }
}

// File: app/src/test/java/com/obsidianbackup/presentation/backup/BackupViewModelTest.kt

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var backupAppsUseCase: BackupAppsUseCase
    private lateinit var viewModel: BackupViewModel
    
    @Before
    fun setup() {
        backupAppsUseCase = mockk()
        viewModel = BackupViewModel(backupAppsUseCase)
    }
    
    @Test
    fun `startBackup updates state to backing up`() = runTest {
        // Given
        val request = BackupRequest(apps = listOf(AppId("test")))
        coEvery { backupAppsUseCase(any()) } returns BackupResult.Success(
            snapshotId = SnapshotId("test"),
            appsBackedUp = 1
        )
        
        // When
        viewModel.startBackup(request)
        
        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertThat(state.isBackingUp).isTrue()
        }
    }
}
```

**Test Coverage Goals:**
- UseCases: 90%+ coverage
- ViewModels: 80%+ coverage
- Repositories: 85%+ coverage
- Critical paths: 95%+ coverage

---

## Implementation Roadmap

### Week 1-2: Critical Fixes
- [ ] Create `ICatalogRepository` interface
- [ ] Implement `CatalogRepositoryImpl`
- [ ] Update all BackupCatalog consumers
- [ ] Implement global `CoroutineExceptionHandler`
- [ ] Fix all 11 empty catch blocks
- [ ] Add logging and crashlytics to exception handlers

**Deliverables:**
- Zero layer violations
- All exceptions logged
- Global error handling active

---

### Week 3-4: Important Improvements
- [ ] Add `@HiltViewModel` to CloudConfigViewModel
- [ ] Add `@HiltViewModel` to RestoreSimulationViewModel
- [ ] Add `@Inject` to 3 UseCases
- [ ] Create `DateFormatter` utility
- [ ] Create `SizeFormatter` utility
- [ ] Extract `RequestHealthPermissionsUseCase`
- [ ] Extract `ManageGameSelectionUseCase`
- [ ] Update DashboardViewModel
- [ ] Update HealthViewModel
- [ ] Update GamingViewModel
- [ ] Remove `BackupScheduleDao` from public DI

**Deliverables:**
- All components properly annotated
- ViewModels under 100 lines
- Business logic in UseCases

---

### Week 5-6: Long-term Enhancements
- [ ] Create interfaces for all 7 repositories
- [ ] Refactor repositories to interface + impl pattern
- [ ] Update DI bindings to use @Binds
- [ ] Hoist state from 10+ Composables to ViewModels
- [ ] Split `RestoreAppsUseCase` into smaller UseCases
- [ ] Split `BackupAppsUseCase` error handling
- [ ] Write unit tests for UseCases (90% coverage)
- [ ] Write unit tests for ViewModels (80% coverage)
- [ ] Write integration tests for repositories

**Deliverables:**
- Interface-based repository abstractions
- Clean, testable ViewModels
- Single-responsibility UseCases
- Comprehensive test suite

---

## Success Metrics

### Before Improvements
- **Architecture Score:** 8.2/10
- **Layer Violations:** 3 critical
- **Empty Catches:** 11 instances
- **Fat ViewModels:** 3 instances
- **Missing Annotations:** 5 instances
- **Test Coverage:** ~60%

### After Improvements
- **Architecture Score:** 9.5/10 ⬆️
- **Layer Violations:** 0 ✅
- **Empty Catches:** 0 ✅
- **Fat ViewModels:** 0 ✅
- **Missing Annotations:** 0 ✅
- **Test Coverage:** 85%+ ⬆️

---

## Long-term Architectural Goals

### 1. Multi-module Architecture
Consider splitting into Gradle modules:
```
:app (application)
:domain (business logic, interfaces)
:data (repositories, data sources)
:presentation (ViewModels, UI state)
:ui-compose (Compose components)
:features:backup
:features:cloud
:features:gaming
:core:network
:core:database
:core:security
```

**Benefits:**
- Enforced layer separation at build level
- Parallel builds
- Better encapsulation
- Clearer dependencies

### 2. Domain-Driven Design (DDD)
Organize by feature domains:
```
backup/
  domain/ (entities, use cases, repositories)
  data/ (implementations)
  presentation/ (ViewModels)
  ui/ (screens, composables)

cloud/
  domain/
  data/
  presentation/
  ui/

gaming/
  domain/
  data/
  presentation/
  ui/
```

### 3. Event-Driven Architecture
Implement event bus for cross-feature communication:
```kotlin
sealed class DomainEvent {
    data class BackupCompleted(val snapshotId: SnapshotId) : DomainEvent()
    data class CloudSyncStarted(val provider: CloudProvider) : DomainEvent()
    data class PermissionGranted(val permission: Permission) : DomainEvent()
}

interface EventBus {
    suspend fun publish(event: DomainEvent)
    fun subscribe(): Flow<DomainEvent>
}
```

### 4. Repository Pattern Enhancement
Add more sophisticated caching:
```kotlin
interface CachePolicy {
    val ttl: Duration
    val strategy: CacheStrategy
}

enum class CacheStrategy {
    MEMORY_ONLY,
    DISK_ONLY,
    MEMORY_FIRST,
    NETWORK_FIRST
}
```

---

## Conclusion

Following this roadmap will elevate ObsidianBackup from **8.2/10 to 9.5/10** in Clean Architecture compliance. The improvements focus on:

1. **Eliminating architectural violations** (layer separation)
2. **Strengthening error handling** (global exception handling)
3. **Improving code organization** (extract business logic)
4. **Enhancing testability** (interfaces, single responsibility)
5. **Future-proofing** (modularity, DDD, events)

**Estimated Total Effort:** 6 weeks (1 developer)  
**Highest Impact:** Priority 1 fixes (weeks 1-2)  
**Long-term Value:** Priority 3 enhancements (weeks 5-6)

**Next Step:** Begin with Priority 1 Critical Fixes, focusing on layer violations and exception handling.
