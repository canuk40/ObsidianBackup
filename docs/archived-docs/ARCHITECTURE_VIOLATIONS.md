# ObsidianBackup - Architecture Violations

**Date:** 2024  
**Severity Levels:** 🔴 Critical | 🟡 Important | 🟢 Minor

---

## 🔴 Critical Violations

### 1. UI Layer Direct Storage Access

**Violation:** Presentation layer imports storage implementation  
**Severity:** 🔴 Critical  
**Impact:** Breaks abstraction boundary, tight coupling

```kotlin
File: /root/workspace/ObsidianBackup/app/src/main/java/com/obsidianbackup/ui/screens/EnhancedBackupsScreen.kt
Line: 37

// ❌ VIOLATION
import com.obsidianbackup.storage.BackupCatalog

@Composable
fun EnhancedBackupsScreen(
    viewModel: BackupsViewModel = hiltViewModel(),
    catalog: BackupCatalog? = null  // Direct storage dependency in UI
) { ... }
```

**Why This Is Bad:**
- UI tightly coupled to database implementation
- Cannot swap storage layer without changing UI
- Violates dependency inversion principle
- Makes testing difficult

**Fix:**
```kotlin
// ✅ CORRECT
// No catalog parameter needed - get data through ViewModel
@Composable
fun EnhancedBackupsScreen(
    viewModel: BackupsViewModel = hiltViewModel()
) {
    val backups by viewModel.backups.collectAsState()
    // Use backups from ViewModel state
}

// Update ViewModel to expose backup data
@HiltViewModel
class BackupsViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository  // Use repository
) : ViewModel() {
    val backups: StateFlow<List<BackupInfo>> = 
        catalogRepository.getAllBackups()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
```

---

### 2. Domain Layer Storage Dependency

**Violation:** Domain classes directly import storage layer  
**Severity:** 🔴 Critical  
**Impact:** Domain layer not independent, cannot use without storage implementation

```kotlin
File: /root/workspace/ObsidianBackup/app/src/main/java/com/obsidianbackup/domain/backup/BackupOrchestrator.kt
Line: 9

// ❌ VIOLATION
import com.obsidianbackup.storage.BackupCatalog

class BackupOrchestrator @Inject constructor(
    private val catalog: BackupCatalog,  // Direct storage dependency
    private val engineFactory: BackupEngineFactory,
    private val eventBus: BackupEventBus
) { ... }
```

```kotlin
File: /root/workspace/ObsidianBackup/app/src/main/java/com/obsidianbackup/domain/backup/BackupEngineFactory.kt
Line: 11

// ❌ VIOLATION
import com.obsidianbackup.storage.BackupCatalog

class BackupEngineFactory @Inject constructor(
    private val catalog: BackupCatalog,  // Direct storage dependency
    @Named("ObsidianBox") private val obsidianBoxEngine: BackupEngine,
    @Named("BusyBox") private val busyBoxEngine: BackupEngine
) { ... }
```

**Why This Is Bad:**
- Domain layer should be independent of implementation details
- Violates Clean Architecture: Domain should not know about Data/Storage
- Cannot test domain logic without database
- Storage changes require domain layer changes

**Fix:**
```kotlin
// ✅ CORRECT - Create repository interface in domain layer

// In domain/repository/CatalogRepository.kt
interface CatalogRepository {
    suspend fun saveBackup(metadata: BackupMetadata): Result<Unit>
    suspend fun getBackup(id: BackupId): Result<BackupMetadata?>
    suspend fun getAllBackups(): Flow<List<BackupMetadata>>
    suspend fun deleteBackup(id: BackupId): Result<Unit>
}

// Update domain classes to use interface
class BackupOrchestrator @Inject constructor(
    private val catalogRepository: CatalogRepository,  // Use interface
    private val engineFactory: BackupEngineFactory,
    private val eventBus: BackupEventBus
) { ... }

// In data layer - implement the interface
// data/repository/CatalogRepositoryImpl.kt
class CatalogRepositoryImpl @Inject constructor(
    private val backupCatalog: BackupCatalog  // Storage implementation
) : CatalogRepository {
    override suspend fun saveBackup(metadata: BackupMetadata) = 
        backupCatalog.saveBackupMetadata(metadata)
    // ... other implementations
}

// Update DI module
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindCatalogRepository(
        impl: CatalogRepositoryImpl
    ): CatalogRepository
}
```

---

### 3. DAO Exposed in Public DI

**Violation:** Data access objects exposed in dependency injection  
**Severity:** 🔴 Critical  
**Impact:** Bypasses repository pattern, enables layer violations

```kotlin
File: /root/workspace/ObsidianBackup/app/src/main/java/com/obsidianbackup/di/AutomationModule.kt
Lines: 33-37

// ❌ VIOLATION
@Provides
@Singleton
fun provideBackupScheduleDao(
    backupCatalog: BackupCatalog
): BackupScheduleDao {  // DAO should not be in DI
    return backupCatalog.getScheduleDao()
}
```

**Why This Is Bad:**
- DAOs should only be accessed through repositories
- Allows direct database access from any layer
- Bypasses business logic and validation
- Breaks encapsulation

**Fix:**
```kotlin
// ✅ CORRECT - Remove DAO from DI, only provide repository

// Remove this entire function from AutomationModule.kt

// Instead, ensure ScheduleRepository is in DI
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideScheduleRepository(
        backupCatalog: BackupCatalog  // DAO accessed internally
    ): ScheduleRepository {
        return ScheduleRepository(backupCatalog.getScheduleDao())
    }
}

// All consumers use repository only
class ScheduleManager @Inject constructor(
    private val scheduleRepository: ScheduleRepository  // Not DAO
) { ... }
```

---

### 4. Empty Catch Blocks

**Violation:** Exceptions silently swallowed  
**Severity:** 🔴 Critical  
**Impact:** Failures invisible to users and developers

**Instance 1:**
```kotlin
File: /root/workspace/ObsidianBackup/app/src/main/java/com/obsidianbackup/plugins/PluginManager.kt
Line: 61

// ❌ VIOLATION
try {
    val plugin = pluginLoader.loadPlugin(pluginClass)
    registry.register(plugin)
} catch (_: Exception) {
    // ignore  // SILENT FAILURE
}
```

**Instance 2:**
```kotlin
File: /root/workspace/ObsidianBackup/app/src/main/java/com/obsidianbackup/plugin/PluginAPI.kt
Line: 69

// ❌ VIOLATION
catch (_: Exception) {
    // No logging, no error handling
}
```

**Why This Is Bad:**
- Plugin failures completely hidden from user
- No telemetry or crash reporting
- Cannot diagnose production issues
- Violates fail-fast principle

**Fix:**
```kotlin
// ✅ CORRECT - Log, report, handle
catch (e: PluginLoadException) {
    logger.e(TAG, "Failed to load plugin: ${pluginClass.name}", e)
    analyticsManager.recordError("plugin_load_failure", e)
    // Optionally notify user
    _pluginLoadErrors.emit(PluginLoadError(pluginClass, e))
} catch (e: SecurityException) {
    logger.e(TAG, "Security violation loading plugin", e)
    // Handle security issues differently
} catch (e: Exception) {
    logger.e(TAG, "Unexpected plugin load error", e)
    crashlytics.recordException(e)
}
```

---

### 5. Missing CoroutineExceptionHandler

**Violation:** No global coroutine exception handling  
**Severity:** 🔴 Critical  
**Impact:** Unhandled exceptions in coroutines crash silently

```kotlin
// ❌ VIOLATION - Current state
// 100+ launch { } blocks without exception handlers

viewModelScope.launch {  // No exception handling
    backupUseCase.invoke(request)  // If throws, may crash
}

CoroutineScope(Dispatchers.IO).launch {  // Fire-and-forget
    cloudSync()  // No error handling
}
```

**Why This Is Bad:**
- Coroutine exceptions not caught by try-catch
- Silent crashes in background operations
- No crash reports for production issues
- Violates robust error handling

**Fix:**
```kotlin
// ✅ CORRECT - Add global exception handler

// In Application class
class ObsidianBackupApp : Application() {
    val globalExceptionHandler = CoroutineExceptionHandler { context, throwable ->
        Timber.e(throwable, "Uncaught coroutine exception: $context")
        FirebaseCrashlytics.getInstance().recordException(throwable)
        
        // Show user-friendly error for critical failures
        if (throwable is BackupFailureException) {
            notificationManager.showBackupErrorNotification(throwable)
        }
    }
}

// Use in ViewModels
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val exceptionHandler: CoroutineExceptionHandler
) : ViewModel() {
    fun startBackup() {
        viewModelScope.launch(exceptionHandler) {  // Add handler
            backupUseCase.invoke(request)
        }
    }
}

// Use in background scopes
val backgroundScope = CoroutineScope(
    Dispatchers.IO + 
    SupervisorJob() + 
    exceptionHandler  // Global handler
)
```

---

## 🟡 Important Violations

### 6. Missing @HiltViewModel Annotations

**Violation:** ViewModels not properly integrated with Hilt  
**Severity:** 🟡 Important  
**Impact:** Cannot inject dependencies, manual instantiation required

**Instance 1:**
```kotlin
File: /root/workspace/ObsidianBackup/app/src/main/java/com/obsidianbackup/ui/cloud/CloudProviderConfigScreen.kt
Line: 329

// ❌ VIOLATION
class CloudConfigViewModel : ViewModel() {
    // Cannot inject dependencies
}
```

**Instance 2:**
```kotlin
File: /root/workspace/ObsidianBackup/app/src/main/java/com/obsidianbackup/ui/components/LiveBackupConsole.kt
Line: 282

// ❌ VIOLATION
class RestoreSimulationViewModel @javax.inject.Inject constructor() : ViewModel() {
    // Has @Inject but missing @HiltViewModel
}
```

**Fix:**
```kotlin
// ✅ CORRECT
@HiltViewModel
class CloudConfigViewModel @Inject constructor(
    private val cloudProviderRepository: CloudProviderRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    // Now can inject dependencies
}

@HiltViewModel  // Add this annotation
class RestoreSimulationViewModel @Inject constructor() : ViewModel() {
    // ...
}
```

---

### 7. Missing @Inject on UseCases

**Violation:** UseCases have constructor parameters but no @Inject annotation  
**Severity:** 🟡 Important  
**Impact:** Cannot be automatically injected, requires manual provision

```kotlin
File: /root/workspace/ObsidianBackup/app/src/main/java/com/obsidianbackup/domain/usecase/BackupAppsUseCase.kt

// ❌ VIOLATION
class BackupAppsUseCase(  // Missing @Inject
    private val backupOrchestrator: BackupOrchestrator
) {
    suspend operator fun invoke(request: BackupRequest): Result<BackupResult> {
        // ...
    }
}
```

**Same Issue In:**
- `RestoreAppsUseCase.kt`
- `VerifySnapshotUseCase.kt`

**Only `GetInstalledAppsUseCase` has correct `@Inject`**

**Fix:**
```kotlin
// ✅ CORRECT
class BackupAppsUseCase @Inject constructor(  // Add @Inject
    private val backupOrchestrator: BackupOrchestrator
) {
    suspend operator fun invoke(request: BackupRequest): Result<BackupResult> {
        // ...
    }
}
```

---

### 8. Business Logic in ViewModels

**Violation:** ViewModels contain business logic instead of delegating to UseCases  
**Severity:** 🟡 Important  
**Impact:** Reduced testability, code duplication, violates SRP

**Instance 1: Formatting Logic**
```kotlin
File: /root/workspace/ObsidianBackup/app/src/main/java/com/obsidianbackup/presentation/dashboard/DashboardViewModel.kt
Lines: 65-96

// ❌ VIOLATION - Business logic in ViewModel
val lastBackupText: String get() = formatRelativeTime(lastBackup?.timestamp ?: 0)

private fun formatRelativeTime(timestamp: Long): String {
    if (timestamp == 0L) return "Never"
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} minutes ago"
        diff < 86400_000 -> "${diff / 3600_000} hours ago"
        else -> "${diff / 86400_000} days ago"
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
```

**Fix:**
```kotlin
// ✅ CORRECT - Extract to utility classes

// util/DateFormatter.kt
object DateFormatter {
    fun formatRelativeTime(timestamp: Long): String {
        // Logic here
    }
}

// util/SizeFormatter.kt
object SizeFormatter {
    fun formatBytes(bytes: Long): String {
        // Logic here
    }
}

// ViewModel now clean
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dateFormatter: DateFormatter,
    private val sizeFormatter: SizeFormatter,
    // ...
) : ViewModel() {
    val lastBackupText: String get() = 
        dateFormatter.formatRelativeTime(lastBackup?.timestamp ?: 0)
    
    val totalSizeText: String get() = 
        sizeFormatter.formatBytes(totalBackupSize)
}
```

**Instance 2: Permission Handling**
```kotlin
File: /root/workspace/ObsidianBackup/app/src/main/java/com/obsidianbackup/presentation/health/HealthViewModel.kt
Lines: 45-73

// ❌ VIOLATION - Permission logic should be UseCase
fun requestPermissions() {
    val permissions = healthConnectManager.getRequiredPermissions(
        setOf(
            HealthDataType.STEPS,
            HealthDataType.HEART_RATE,
            // ...
        )
    )
    // Complex permission request logic
}
```

**Fix:**
```kotlin
// ✅ CORRECT - Extract to UseCase

// domain/usecase/RequestHealthPermissionsUseCase.kt
class RequestHealthPermissionsUseCase @Inject constructor(
    private val healthConnectManager: HealthConnectManager
) {
    suspend operator fun invoke(
        dataTypes: Set<HealthDataType>
    ): Result<PermissionStatus> {
        val permissions = healthConnectManager.getRequiredPermissions(dataTypes)
        return healthConnectManager.requestPermissions(permissions)
    }
}

// ViewModel delegates
@HiltViewModel
class HealthViewModel @Inject constructor(
    private val requestPermissionsUseCase: RequestHealthPermissionsUseCase
) : ViewModel() {
    fun requestPermissions() {
        viewModelScope.launch {
            val result = requestPermissionsUseCase(
                setOf(HealthDataType.STEPS, HealthDataType.HEART_RATE)
            )
            _state.update { it.copy(permissionStatus = result) }
        }
    }
}
```

**Instance 3: Game Selection Logic**
```kotlin
File: /root/workspace/ObsidianBackup/app/src/main/java/com/obsidianbackup/presentation/gaming/GamingViewModel.kt
Lines: 59-69

// ❌ VIOLATION - Selection logic is business logic
fun toggleGameSelection(game: GameInfo) {
    val selectedGames = state.selectedGames.toMutableList()
    if (selectedGames.contains(game)) {
        selectedGames.remove(game)
    } else {
        selectedGames.add(game)
    }
    _state.update { it.copy(selectedGames = selectedGames) }
}
```

**Fix:**
```kotlin
// ✅ CORRECT - Extract to UseCase

// domain/usecase/ManageGameSelectionUseCase.kt
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
}

// ViewModel delegates
@HiltViewModel
class GamingViewModel @Inject constructor(
    private val manageGameSelectionUseCase: ManageGameSelectionUseCase
) : ViewModel() {
    fun toggleGameSelection(game: GameInfo) {
        val newSelection = manageGameSelectionUseCase.toggleSelection(
            state.selectedGames,
            game
        )
        _state.update { it.copy(selectedGames = newSelection) }
    }
}
```

---

## 🟢 Minor Violations

### 9. Local Composable State

**Violation:** UI state managed in Composables instead of ViewModels  
**Severity:** 🟢 Minor  
**Impact:** State lost on recomposition, harder to test

```kotlin
File: /root/workspace/ObsidianBackup/app/src/main/java/com/obsidianbackup/ui/screens/AppsScreen.kt

// ❌ VIOLATION
@Composable
fun AppsScreen() {
    var selectedApps by remember { mutableStateOf(setOf<AppId>()) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    
    // ... UI using local state
}
```

**Fix:**
```kotlin
// ✅ CORRECT - Hoist to ViewModel

// ViewModel
@HiltViewModel
class AppsViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(AppsState())
    val state: StateFlow<AppsState> = _state.asStateFlow()
    
    data class AppsState(
        val selectedApps: Set<AppId> = emptySet(),
        val showBackupDialog: Boolean = false,
        val installedApps: List<AppInfo> = emptyList()
    )
    
    fun toggleAppSelection(appId: AppId) {
        _state.update { 
            it.copy(
                selectedApps = if (appId in it.selectedApps) {
                    it.selectedApps - appId
                } else {
                    it.selectedApps + appId
                }
            )
        }
    }
    
    fun showBackupDialog() {
        _state.update { it.copy(showBackupDialog = true) }
    }
}

// Composable
@Composable
fun AppsScreen(
    viewModel: AppsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    AppsContent(
        selectedApps = state.selectedApps,
        showBackupDialog = state.showBackupDialog,
        installedApps = state.installedApps,
        onToggleApp = viewModel::toggleAppSelection,
        onShowBackupDialog = viewModel::showBackupDialog
    )
}
```

---

### 10. LaunchedEffect Business Logic

**Violation:** Business operations in Composable side effects  
**Severity:** 🟢 Minor  
**Impact:** Breaks separation of concerns

```kotlin
File: /root/workspace/ObsidianBackup/app/src/main/java/com/obsidianbackup/ui/screens/AppsScreen.kt
Lines: 73-75

// ❌ VIOLATION
@Composable
fun AppsScreen() {
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        installedApps = appScanner.scanInstalledApps(
            includeSystemApps = true
        )
    }
}
```

**Fix:**
```kotlin
// ✅ CORRECT - Load in ViewModel

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(AppsState())
    val state: StateFlow<AppsState> = _state.asStateFlow()
    
    init {
        loadInstalledApps()
    }
    
    private fun loadInstalledApps() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val apps = getInstalledAppsUseCase(includeSystemApps = true)
            _state.update { 
                it.copy(
                    installedApps = apps,
                    isLoading = false
                )
            }
        }
    }
}
```

---

### 11. Missing Repository Interface Abstractions

**Violation:** No repository interfaces, only concrete implementations  
**Severity:** 🟢 Minor  
**Impact:** Harder to mock, less flexible

```kotlin
// ❌ CURRENT STATE
class LogRepository @Inject constructor(
    private val logDao: LogDao
) {
    suspend fun getRecentLogs(limit: Int): List<LogEntry> { ... }
}

// Consumer directly depends on implementation
class LogsViewModel @Inject constructor(
    private val logRepository: LogRepository  // Concrete class
) : ViewModel()
```

**Fix:**
```kotlin
// ✅ CORRECT - Add interface

// domain/repository/ILogRepository.kt
interface ILogRepository {
    suspend fun getRecentLogs(limit: Int): List<LogEntry>
    suspend fun clearLogs(): Result<Unit>
    fun observeLogs(): Flow<List<LogEntry>>
}

// data/repository/LogRepositoryImpl.kt
class LogRepositoryImpl @Inject constructor(
    private val logDao: LogDao
) : ILogRepository {
    override suspend fun getRecentLogs(limit: Int): List<LogEntry> { ... }
    // ...
}

// DI Module
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindLogRepository(
        impl: LogRepositoryImpl
    ): ILogRepository
}

// Consumer depends on interface
class LogsViewModel @Inject constructor(
    private val logRepository: ILogRepository  // Interface
) : ViewModel()
```

---

## Summary

| Severity | Count | Issues |
|----------|-------|--------|
| 🔴 Critical | 5 | UI storage access, domain storage dependency, DAO exposure, empty catches, missing exception handler |
| 🟡 Important | 3 | Missing @HiltViewModel (2), missing @Inject (3 UseCases), fat ViewModels (3) |
| 🟢 Minor | 3 | Local composable state, LaunchedEffect logic, missing interfaces |
| **TOTAL** | **11** | **Architecture violations found** |

**Estimated Fix Time:**
- Critical: 4-6 hours
- Important: 2-3 hours
- Minor: 2-3 hours
- **Total: 8-12 hours**

---

**Next Steps:**
1. Address critical violations first (layer separation, error handling)
2. Fix important DI issues (annotations)
3. Refactor fat ViewModels
4. Consider minor improvements for long-term maintainability
