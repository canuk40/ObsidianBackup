# Performance Optimization Plan
**Target:** ObsidianBackup  
**Timeline:** 3 Sprints (6 weeks)  
**Team Size:** 2-3 developers

---

## SPRINT 1: Critical Fixes (Weeks 1-2)

### 🎯 Goal: Fix blocking operations and critical database issues

**Story Points:** 34  
**Risk:** LOW  
**ROI:** HIGH

---

### Epic 1.1: Remove Blocking Operations
**Priority:** P0 - CRITICAL  
**Effort:** 13 SP

#### Task 1.1.1: Fix runBlocking in DI Modules ⚠️
**Files:** `di/CloudModule.kt`  
**Lines:** 67, 82, 106  
**Effort:** 5 SP (1 day)

**Implementation:**
```kotlin
// BEFORE: Blocks main thread
@Provides
@Singleton
fun provideWebDavConfig(repository: SettingsRepository): WebDavConfig = runBlocking {
    repository.getWebDavConfig()
}

// AFTER: Use provider pattern
@Provides
@Singleton
fun provideWebDavConfigProvider(
    repository: SettingsRepository
): WebDavConfigProvider = WebDavConfigProvider(repository)

// Create wrapper class
class WebDavConfigProvider(private val repo: SettingsRepository) {
    private var cachedConfig: WebDavConfig? = null
    
    suspend fun get(): WebDavConfig {
        return cachedConfig ?: repo.getWebDavConfig().also { cachedConfig = it }
    }
    
    fun getCached(): WebDavConfig? = cachedConfig
}
```

**Acceptance Criteria:**
- [ ] No `runBlocking` calls in any Hilt module
- [ ] App startup time reduced by 100-200ms (measure with Macrobenchmark)
- [ ] No main thread blocking detected by StrictMode

**Testing:**
- Enable StrictMode in debug
- Profile app startup before/after
- Verify all cloud providers initialize correctly

---

#### Task 1.1.2: Fix runBlocking in Cloud Provider Init 🌩️
**Files:** 
- `cloud/providers/AzureBlobProvider.kt:61`
- `cloud/providers/BackblazeB2Provider.kt:63`
- `cloud/providers/DigitalOceanSpacesProvider.kt:59`
- `cloud/providers/AlibabaOSSProvider.kt:59`
- `cloud/providers/OracleCloudProvider.kt:60`
- `cloud/providers/BoxCloudProvider.kt:55`

**Effort:** 8 SP (1.5 days)

**Implementation:**
```kotlin
// BEFORE: Blocks in init
class AzureBlobProvider(...) : CloudProvider {
    init {
        runBlocking {
            authenticate()
        }
    }
}

// AFTER: Lazy authentication
class AzureBlobProvider(...) : CloudProvider {
    private val authToken: Deferred<String> by lazy {
        scope.async { authenticate() }
    }
    
    private suspend fun ensureAuthenticated() {
        authToken.await()
    }
    
    override suspend fun upload(file: File): Result<CloudFile> {
        ensureAuthenticated()
        // ... rest of upload
    }
}
```

**Acceptance Criteria:**
- [ ] All 6 providers updated
- [ ] No blocking in constructors
- [ ] Auth errors properly handled
- [ ] Unit tests updated

**Testing:**
- Test each provider individually
- Verify auth happens on first operation
- Test auth failure scenarios

---

### Epic 1.2: Database Optimizations
**Priority:** P0 - CRITICAL  
**Effort:** 13 SP

#### Task 1.2.1: Fix N+1 Query Problem 🗃️
**File:** `storage/BackupCatalog.kt`  
**Effort:** 8 SP (1.5 days)

**Implementation:**
```kotlin
// NEW: Join query to fetch snapshots with app counts
@Query("""
    SELECT 
        s.id,
        s.timestamp,
        s.description,
        s.totalSize,
        s.encrypted,
        COUNT(DISTINCT ab.app_id) as appCount,
        SUM(ab.apk_size + ab.data_size) as totalAppSize
    FROM snapshots s
    LEFT JOIN app_backups ab ON s.id = ab.snapshot_id
    GROUP BY s.id
    ORDER BY s.timestamp DESC
""")
suspend fun getSnapshotsWithSummary(): List<SnapshotSummary>

data class SnapshotSummary(
    val id: String,
    val timestamp: Long,
    val description: String?,
    val totalSize: Long,
    val encrypted: Boolean,
    val appCount: Int,
    val totalAppSize: Long
)
```

**Acceptance Criteria:**
- [ ] Single query replaces N+1 pattern
- [ ] List loading time < 100ms for 100 snapshots
- [ ] UI responsiveness improved
- [ ] Backward compatible with existing code

**Testing:**
- Benchmark with 10, 100, 1000 snapshots
- Verify UI updates correctly
- Test with/without app backups

---

#### Task 1.2.2: Add Missing Database Indexes 📑
**Files:** `storage/AppBackupDao.kt`, `storage/LogDao.kt`  
**Effort:** 3 SP (0.5 days)

**Implementation:**
```kotlin
@Entity(
    tableName = "app_backups",
    foreignKeys = [
        ForeignKey(
            entity = SnapshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["snapshot_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        // NEW: Index on foreign key
        Index(value = ["snapshot_id"], name = "idx_app_backup_snapshot"),
        // NEW: Index for app history queries
        Index(value = ["app_id", "backup_timestamp"], name = "idx_app_backup_app_time"),
        // NEW: Composite index for common query
        Index(value = ["snapshot_id", "app_id"], name = "idx_app_backup_composite")
    ]
)
data class AppBackupEntity(...)

// For logs
@Entity(
    tableName = "logs",
    indices = [
        Index(value = ["timestamp"], name = "idx_log_timestamp"),
        Index(value = ["level"], name = "idx_log_level"),
        Index(value = ["operation_type"], name = "idx_log_operation"),
        // NEW: Composite for filtered queries
        Index(value = ["level", "timestamp"], name = "idx_log_level_time")
    ]
)
data class LogEntity(...)
```

**Acceptance Criteria:**
- [ ] Migration script created
- [ ] Indexes added to all critical columns
- [ ] Query performance measured before/after
- [ ] Database migration tested

**Testing:**
- Test migration from current version
- Verify queries use new indexes (EXPLAIN QUERY PLAN)
- Benchmark delete performance

---

#### Task 1.2.3: Add @Transaction Annotations 🔐
**Files:** All DAOs  
**Effort:** 2 SP (0.5 days)

**Implementation:**
```kotlin
@Dao
interface BackupCatalogDao {
    // NEW: Atomic multi-table operations
    @Transaction
    suspend fun deleteSnapshotWithDependencies(snapshotId: String) {
        deleteAppBackups(snapshotId)
        deleteSnapshotById(snapshotId)
    }
    
    @Transaction
    suspend fun createSnapshotWithApps(
        snapshot: SnapshotEntity,
        apps: List<AppBackupEntity>
    ) {
        insertSnapshot(snapshot)
        insertAppBackups(apps)
    }
    
    // Existing queries that need @Transaction
    @Transaction
    @Query("SELECT * FROM snapshots WHERE id = :id")
    suspend fun getSnapshotWithApps(id: String): SnapshotWithApps
}

// NEW: Relation class
data class SnapshotWithApps(
    @Embedded val snapshot: SnapshotEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "snapshot_id"
    )
    val apps: List<AppBackupEntity>
)
```

**Acceptance Criteria:**
- [ ] All multi-step operations wrapped in @Transaction
- [ ] Data integrity tests pass
- [ ] Concurrent access tests pass
- [ ] No performance regression

**Testing:**
- Test concurrent writes
- Test transaction rollback
- Verify atomic behavior

---

### Epic 1.3: File I/O Buffering
**Priority:** P0 - CRITICAL  
**Effort:** 8 SP

#### Task 1.3.1: Add Buffering to All File Streams 📁
**Files:** 15+ files with FileInputStream/FileOutputStream  
**Effort:** 8 SP (1.5 days)

**Implementation:**
```kotlin
// BEFORE: Unbuffered (slow)
FileInputStream(file).use { input ->
    val bytes = input.readBytes()
}

// AFTER: Buffered (3-5x faster)
FileInputStream(file).buffered(BUFFER_SIZE).use { input ->
    val bytes = input.readBytes()
}

// Create utility class
object FileIOUtils {
    const val DEFAULT_BUFFER_SIZE = 8192 // 8KB
    const val LARGE_BUFFER_SIZE = 65536 // 64KB for large files
    
    fun File.bufferedInputStream(
        bufferSize: Int = if (length() > 1_000_000) LARGE_BUFFER_SIZE else DEFAULT_BUFFER_SIZE
    ): BufferedInputStream {
        return inputStream().buffered(bufferSize)
    }
    
    fun File.bufferedOutputStream(
        bufferSize: Int = if (exists() && length() > 1_000_000) LARGE_BUFFER_SIZE else DEFAULT_BUFFER_SIZE
    ): BufferedOutputStream {
        return outputStream().buffered(bufferSize)
    }
}
```

**Files to update:**
1. `crypto/ZeroKnowledgeEncryption.kt:193-194, 240-249`
2. `verification/ChecksumVerifier.kt:89`
3. `cloud/WebDavCloudProvider.kt:501, 587`
4. `cloud/providers/AzureBlobProvider.kt:660`
5. `cloud/providers/BackblazeB2Provider.kt:855`
6. `cloud/providers/DigitalOceanSpacesProvider.kt:673`
7. `cloud/providers/AlibabaOSSProvider.kt:665`
8. `cloud/providers/OracleCloudProvider.kt:679`
9. `cloud/providers/BoxCloudProvider.kt:722`
10. `gaming/GamingBackupManager.kt:205`
11. `ml/prediction/BackupPredictor.kt:82, 89`

**Acceptance Criteria:**
- [ ] All file streams use buffering
- [ ] Buffer size adapts to file size
- [ ] 3x minimum speedup on large files (benchmark)
- [ ] No memory regressions

**Testing:**
- Benchmark file operations before/after
- Test with various file sizes (1KB to 1GB)
- Verify memory usage stays constant
- Profile with Android Profiler

---

## SPRINT 2: High Priority Optimizations (Weeks 3-4)

### 🎯 Goal: Improve memory usage and Compose performance

**Story Points:** 29  
**Risk:** LOW-MEDIUM  
**ROI:** MEDIUM-HIGH

---

### Epic 2.1: Memory Optimizations
**Priority:** P1 - HIGH  
**Effort:** 13 SP

#### Task 2.1.1: Implement Bitmap Downsampling 🖼️
**File:** `scanner/AppScanner.kt`  
**Effort:** 5 SP (1 day)

**Implementation:**
```kotlin
// Create new bitmap utils
object BitmapUtils {
    fun Drawable.toBitmap(
        maxWidth: Int = 128,
        maxHeight: Int = 128,
        config: Bitmap.Config = Bitmap.Config.RGB_565
    ): Bitmap {
        if (this is BitmapDrawable && bitmap != null) {
            return Bitmap.createScaledBitmap(bitmap, maxWidth, maxHeight, true)
        }
        
        val width = intrinsicWidth.coerceAtMost(maxWidth)
        val height = intrinsicHeight.coerceAtMost(maxHeight)
        
        val bitmap = Bitmap.createBitmap(width, height, config)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }
}

// Update AppScanner
suspend fun loadAppIcon(packageName: String): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val drawable = packageManager.getApplicationIcon(packageName)
        drawable.toBitmap(
            maxWidth = 128,
            maxHeight = 128,
            config = Bitmap.Config.RGB_565 // Half memory of ARGB_8888
        )
    } catch (e: Exception) {
        null
    }
}
```

**Acceptance Criteria:**
- [ ] All app icons limited to 128x128
- [ ] Memory usage reduced by 75% for icons
- [ ] No visual quality loss
- [ ] Icon loading time unchanged or faster

**Testing:**
- Load 1000 app icons, measure memory
- Visual comparison before/after
- Benchmark loading time

---

#### Task 2.1.2: Implement Lazy Loading for Large JSON Columns 📄
**File:** `storage/BackupCatalog.kt`  
**Effort:** 5 SP (1 day)

**Implementation:**
```kotlin
// Separate table for large data
@Entity(
    tableName = "snapshot_details",
    foreignKeys = [
        ForeignKey(
            entity = SnapshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["snapshotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["snapshotId"])]
)
data class SnapshotDetailsEntity(
    @PrimaryKey val snapshotId: String,
    val appsJson: String,
    val componentsJson: String,
    val checksumsJson: String
)

// Simplified snapshot entity
@Entity(tableName = "snapshots")
data class SnapshotEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val description: String?,
    val totalSize: Long,
    // ... other small fields
    // REMOVED: appsJson, componentsJson, checksumsJson
)

// DAO methods
@Dao
interface SnapshotDetailsDao {
    @Query("SELECT * FROM snapshot_details WHERE snapshotId = :id")
    suspend fun getDetails(id: String): SnapshotDetailsEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: SnapshotDetailsEntity)
}

// Repository layer
suspend fun getSnapshotWithDetails(id: String): SnapshotWithDetails? {
    val snapshot = snapshotDao.getSnapshotById(id) ?: return null
    val details = detailsDao.getDetails(id) // Lazy load only when needed
    return SnapshotWithDetails(snapshot, details)
}
```

**Acceptance Criteria:**
- [ ] Snapshot list loads 10x faster
- [ ] Memory usage reduced by 60% for list screens
- [ ] Details loaded only on demand
- [ ] Migration preserves existing data

**Testing:**
- Benchmark list loading
- Memory profile before/after
- Test detail loading

---

#### Task 2.1.3: Configure Image Cache Limits 🗂️
**Effort:** 3 SP (0.5 days)

**Implementation:**
```kotlin
// In Application class
class ObsidianBackupApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Configure Coil image loading
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15) // 15% of app memory
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024) // 50MB
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
        
        Coil.setImageLoader(imageLoader)
    }
}
```

**Acceptance Criteria:**
- [ ] Memory cache limited to 15% of heap
- [ ] Disk cache limited to 50MB
- [ ] Image loading remains fast
- [ ] No OOM errors under load

---

### Epic 2.2: Compose Optimizations
**Priority:** P1 - HIGH  
**Effort:** 8 SP

#### Task 2.2.1: Add Key Parameters to LazyColumns 🔑
**Files:** 35+ LazyColumn instances  
**Effort:** 5 SP (1 day)

**Implementation:**
```kotlin
// Create extension for stable keys
fun <T> LazyListScope.stableItems(
    items: List<T>,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    itemContent: @Composable LazyItemScope.(item: T) -> Unit
) {
    items(
        count = items.size,
        key = if (key != null) { index: Int -> key(items[index]) } else null,
        contentType = { index: Int -> contentType(items[index]) }
    ) { index ->
        itemContent(items[index])
    }
}

// Usage in screens
LazyColumn {
    stableItems(
        items = apps,
        key = { it.packageName },
        contentType = { "app_item" }
    ) { app ->
        AppListItem(app)
    }
}
```

**Files to update:**
- `ui/screens/AppsScreen.kt`
- `ui/screens/BackupsScreen.kt`
- `ui/screens/LogsScreen.kt`
- `ui/screens/GamingScreen.kt`
- 30+ more screens

**Acceptance Criteria:**
- [ ] All LazyColumn items have stable keys
- [ ] Recomposition count reduced by 50%
- [ ] Smoother scrolling
- [ ] No visual regressions

**Testing:**
- Enable layout inspector
- Count recompositions before/after
- Test with large lists (1000+ items)
- Verify scroll performance

---

#### Task 2.2.2: Fix Unstable Lambda Parameters 🔄
**Files:** Multiple composables  
**Effort:** 3 SP (0.5 days)

**Implementation:**
```kotlin
// Create stable callbacks
@Stable
interface AppListCallbacks {
    fun onAppClick(app: AppInfo)
    fun onAppLongClick(app: AppInfo)
    fun onBackupClick(app: AppInfo)
}

// In ViewModel or remember
@Composable
fun AppsScreen(viewModel: AppsViewModel = hiltViewModel()) {
    val callbacks = remember {
        object : AppListCallbacks {
            override fun onAppClick(app: AppInfo) = viewModel.onAppSelected(app)
            override fun onAppLongClick(app: AppInfo) = viewModel.onAppLongPress(app)
            override fun onBackupClick(app: AppInfo) = viewModel.backupApp(app)
        }
    }
    
    AppsList(apps = state.apps, callbacks = callbacks)
}

// Or use rememberUpdatedState for simpler cases
@Composable
fun MyComponent(onClick: () -> Unit) {
    val currentOnClick by rememberUpdatedState(onClick)
    Button(onClick = { currentOnClick() }) {
        Text("Click me")
    }
}
```

**Acceptance Criteria:**
- [ ] No lambdas recreated unnecessarily
- [ ] Recomposition analyzer shows improvements
- [ ] Performance tests pass
- [ ] Code remains readable

---

### Epic 2.3: Network Optimizations
**Priority:** P1 - HIGH  
**Effort:** 8 SP

#### Task 2.3.1: Add HTTP Response Caching 🌐
**Files:** All OkHttpClient builders  
**Effort:** 3 SP (0.5 days)

**Implementation:**
```kotlin
// In DI module
@Provides
@Singleton
fun provideHttpCache(@ApplicationContext context: Context): Cache {
    return Cache(
        directory = File(context.cacheDir, "http_cache"),
        maxSize = 50L * 1024 * 1024 // 50 MB
    )
}

@Provides
@Singleton
fun provideOkHttpClient(cache: Cache): OkHttpClient {
    return OkHttpClient.Builder()
        .cache(cache)
        .addNetworkInterceptor(CacheInterceptor())
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
}

class CacheInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        
        // Cache successful GET requests for 5 minutes
        if (chain.request().method == "GET" && response.isSuccessful) {
            return response.newBuilder()
                .header("Cache-Control", "public, max-age=300")
                .build()
        }
        
        return response
    }
}
```

**Acceptance Criteria:**
- [ ] HTTP cache enabled for all clients
- [ ] Cache size limited to 50MB
- [ ] Appropriate cache headers set
- [ ] Bandwidth usage reduced by 30%

---

#### Task 2.3.2: Implement Parallel File Uploads ⬆️
**File:** Cloud provider implementations  
**Effort:** 5 SP (1 day)

**Implementation:**
```kotlin
// In BackupOrchestrator or CloudSyncManager
suspend fun uploadFilesParallel(
    files: List<File>,
    cloudProvider: CloudProvider,
    maxConcurrent: Int = 3
): Result<List<CloudFile>> = withContext(Dispatchers.IO) {
    val semaphore = Semaphore(maxConcurrent)
    
    val results = files.map { file ->
        async {
            semaphore.withPermit {
                try {
                    cloudProvider.upload(file)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }
    }.awaitAll()
    
    val successes = results.filterIsInstance<Result.Success<CloudFile>>()
    val failures = results.filterIsInstance<Result.Failure>()
    
    if (failures.isNotEmpty()) {
        Result.Failure("${failures.size} uploads failed")
    } else {
        Result.Success(successes.map { it.data })
    }
}

// With progress tracking
suspend fun uploadWithProgress(
    files: List<File>,
    cloudProvider: CloudProvider,
    onProgress: (current: Int, total: Int) -> Unit
): Result<List<CloudFile>> = withContext(Dispatchers.IO) {
    val uploaded = AtomicInteger(0)
    val semaphore = Semaphore(3)
    
    files.map { file ->
        async {
            semaphore.withPermit {
                cloudProvider.upload(file).also {
                    val count = uploaded.incrementAndGet()
                    onProgress(count, files.size)
                }
            }
        }
    }.awaitAll()
    
    // ... handle results
}
```

**Acceptance Criteria:**
- [ ] Files upload in parallel (max 3 concurrent)
- [ ] Upload time reduced by 50% for multiple files
- [ ] Progress tracking works correctly
- [ ] Network bandwidth utilized efficiently
- [ ] Memory usage stays constant

---

## SPRINT 3: Polish & Monitoring (Weeks 5-6)

### 🎯 Goal: Add performance monitoring and fix remaining issues

**Story Points:** 21  
**Risk:** LOW  
**ROI:** MEDIUM

---

### Epic 3.1: Performance Monitoring
**Priority:** P2 - MEDIUM  
**Effort:** 8 SP

#### Task 3.1.1: Add Performance Benchmarks 📊
**Effort:** 5 SP (1 day)

**Implementation:**
```kotlin
// app/build.gradle.kts - add benchmark module
dependencies {
    androidTestImplementation("androidx.benchmark:benchmark-junit4:1.2.0")
}

// Create benchmark tests
@RunWith(AndroidJUnit4::class)
@LargeTest
class BackupBenchmark {
    
    @get:Rule
    val benchmarkRule = BenchmarkRule()
    
    @Test
    fun benchmarkSnapshotListLoading() {
        benchmarkRule.measureRepeated {
            // Setup
            val catalog = runWithTimingDisabled {
                createCatalogWithSnapshots(100)
            }
            
            // Measure
            catalog.getAllSnapshots().first()
        }
    }
    
    @Test
    fun benchmarkFileEncryption() {
        val file = createTestFile(1 * 1024 * 1024) // 1MB
        
        benchmarkRule.measureRepeated {
            encryptionEngine.encrypt(file)
        }
    }
}
```

**Benchmarks to add:**
- Database query performance
- File I/O operations
- Encryption/decryption
- Bitmap loading
- Network requests
- List rendering

**Acceptance Criteria:**
- [ ] 10+ benchmarks covering critical paths
- [ ] CI/CD runs benchmarks on PRs
- [ ] Performance baselines established
- [ ] Regression alerts configured

---

#### Task 3.1.2: Enable StrictMode in Debug 🚨
**Effort:** 2 SP (0.5 days)

**Implementation:**
```kotlin
class ObsidianBackupApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
    }
    
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyFlashScreen() // Visual indicator
                .build()
        )
        
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
    }
}
```

**Acceptance Criteria:**
- [ ] StrictMode enabled in debug builds
- [ ] All violations documented
- [ ] Critical violations fixed
- [ ] Known violations added to baseline

---

#### Task 3.1.3: Add Production Performance Monitoring 📈
**Effort:** 1 SP (0.25 days)

**Implementation:**
```kotlin
// Use Firebase Performance Monitoring
dependencies {
    implementation("com.google.firebase:firebase-perf-ktx:20.5.0")
}

// Add custom traces
val trace = Firebase.performance.newTrace("backup_operation")
trace.start()
try {
    backupEngine.backupApps(request)
    trace.incrementMetric("apps_backed_up", request.apps.size)
    trace.putAttribute("backup_type", request.type.name)
} finally {
    trace.stop()
}

// Add custom metrics
class PerformanceTracker @Inject constructor() {
    fun trackBackupDuration(duration: Long) {
        Firebase.performance.newHttpMetric(
            "backup_duration_ms", 
            duration
        )
    }
}
```

**Acceptance Criteria:**
- [ ] Firebase Performance added
- [ ] Critical operations traced
- [ ] Custom metrics for key operations
- [ ] Dashboard configured

---

### Epic 3.2: Remaining Optimizations
**Priority:** P2 - MEDIUM  
**Effort:** 13 SP

#### Task 3.2.1: Add Coroutine Timeouts ⏱️
**Files:** Network operations, sync operations  
**Effort:** 5 SP (1 day)

**Implementation:**
```kotlin
// Create timeout extension
suspend fun <T> withOperationTimeout(
    timeoutMs: Long = 30_000L,
    operation: suspend () -> T
): Result<T> = try {
    withTimeout(timeoutMs) {
        Result.Success(operation())
    }
} catch (e: TimeoutCancellationException) {
    Result.Failure("Operation timed out after ${timeoutMs}ms")
} catch (e: Exception) {
    Result.Failure(e.message ?: "Unknown error")
}

// Usage
suspend fun syncWithCloud() = withOperationTimeout(60_000L) {
    cloudProvider.sync()
}
```

**Acceptance Criteria:**
- [ ] All network ops have timeouts
- [ ] Timeouts configurable per operation
- [ ] Timeout errors handled gracefully
- [ ] Users see meaningful error messages

---

#### Task 3.2.2: Implement FTS for Log Search 🔍
**File:** `storage/LogDao.kt`  
**Effort:** 5 SP (1 day)

**Implementation:**
```kotlin
// Create FTS table
@Entity(tableName = "logs_fts")
@Fts4(contentEntity = LogEntity::class)
data class LogFtsEntity(
    val message: String,
    val operationType: String
)

@Dao
interface LogDao {
    // Existing queries...
    
    // NEW: FTS query
    @Query("""
        SELECT logs.* FROM logs
        JOIN logs_fts ON logs.rowid = logs_fts.rowid
        WHERE logs_fts MATCH :query
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun searchLogs(query: String, limit: Int = 100): List<LogEntity>
}
```

**Acceptance Criteria:**
- [ ] Full-text search on log messages
- [ ] Search time < 50ms for 10k logs
- [ ] Supports complex queries
- [ ] Migration preserves existing data

---

#### Task 3.2.3: Optimize Image Loading 🖼️
**Effort:** 3 SP (0.5 days)

**Implementation:**
```kotlin
// Use Coil with optimizations
@Composable
fun AppIcon(packageName: String, size: Dp = 48.dp) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(packageName)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .size(size.value.toInt())
            .crossfade(true)
            .build(),
        contentDescription = "App icon",
        modifier = Modifier.size(size)
    )
}
```

**Acceptance Criteria:**
- [ ] Icons load smoothly while scrolling
- [ ] Memory usage optimized
- [ ] Disk cache prevents redundant loads
- [ ] Crossfade animation smooth

---

## SUCCESS METRICS

### Performance Targets

| Metric | Current | Target | Measurement |
|--------|---------|--------|-------------|
| App startup (cold) | ~1.2s | <800ms | Macrobenchmark |
| Snapshot list load (100 items) | ~500ms | <100ms | Benchmark |
| File encryption (1MB) | ~150ms | <100ms | Benchmark |
| Bitmap loading (per icon) | ~50ms | <20ms | Benchmark |
| Cloud upload (10MB, 5 files) | ~45s | <20s | Integration test |
| Database migration | ~2s | <500ms | Benchmark |
| Scroll FPS (large lists) | ~45 FPS | 60 FPS | Systrace |
| Memory usage (idle) | ~150MB | <100MB | Memory Profiler |
| Memory usage (peak) | ~350MB | <250MB | Memory Profiler |

### Quality Gates

**Before Merging Each Epic:**
- [ ] All benchmarks pass with 95% confidence
- [ ] No StrictMode violations
- [ ] Memory Profiler shows no leaks
- [ ] CPU usage < 30% during operations
- [ ] ANR rate < 0.1%
- [ ] All tests green

**Before Release:**
- [ ] Performance regression tests pass
- [ ] Production monitoring shows improvements
- [ ] User-facing metrics improved by 30%
- [ ] No P0/P1 performance bugs

---

## ROLLOUT STRATEGY

### Phase 1: Internal Testing (Week 7)
- Deploy to internal testers
- Monitor crash-free rate
- Collect performance metrics
- Verify benchmarks in wild

### Phase 2: Beta Release (Week 8)
- Deploy to beta channel (10% of users)
- A/B test performance improvements
- Monitor Firebase Performance
- Collect user feedback

### Phase 3: Production Release (Week 9)
- Gradual rollout: 25% → 50% → 100%
- Monitor metrics daily
- Have rollback plan ready
- Communicate improvements to users

---

## RISK MITIGATION

### Technical Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Database migration fails | LOW | HIGH | Extensive testing, backup/restore mechanism |
| Performance regression | MEDIUM | MEDIUM | Automated benchmarks, gradual rollout |
| Memory leaks introduced | LOW | HIGH | Strict code review, memory profiling |
| Network issues with parallel uploads | MEDIUM | LOW | Fallback to serial, retry logic |
| StrictMode false positives | MEDIUM | LOW | Baseline violations, conditional enable |

### Process Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Scope creep | MEDIUM | MEDIUM | Strict epic boundaries, timebox tasks |
| Resource unavailability | LOW | HIGH | Cross-training, documentation |
| Testing delays | MEDIUM | MEDIUM | Automated testing, CI/CD |
| Merge conflicts | HIGH | LOW | Small PRs, frequent integration |

---

## TEAM RESPONSIBILITIES

### Developer 1: Database & I/O
- Epic 1.2: Database Optimizations
- Task 1.3.1: File I/O Buffering
- Task 2.1.2: Lazy Loading

### Developer 2: Coroutines & Network
- Epic 1.1: Remove Blocking Operations
- Epic 2.3: Network Optimizations
- Task 3.2.1: Coroutine Timeouts

### Developer 3: UI & Memory
- Epic 2.1: Memory Optimizations
- Epic 2.2: Compose Optimizations
- Task 3.2.3: Image Loading

### All: Testing & Monitoring
- Code reviews for all PRs
- Write benchmarks for their areas
- Monitor performance metrics

---

## DEPENDENCIES

### Technical Dependencies
- Kotlin 1.9+
- Compose 1.5+
- Room 2.6+
- OkHttp 4.11+
- Coil 2.5+

### External Dependencies
- Firebase Performance SDK
- Macrobenchmark library
- Android Studio profiling tools

### Internal Dependencies
- Feature flags for gradual rollout
- Monitoring infrastructure
- CI/CD pipeline updates

---

## COMMUNICATION PLAN

### Daily
- Stand-up: Progress, blockers
- Slack: Quick questions, updates

### Weekly
- Sprint planning: Refine upcoming tasks
- Performance review: Check metrics
- Demo: Show progress to stakeholders

### Bi-weekly
- Sprint retrospective: Learn, improve
- Performance report: Share with leadership

---

## POST-LAUNCH

### Week 10+: Monitoring & Iteration
- Monitor production metrics
- Fix any issues that arise
- Gather user feedback
- Plan next optimization cycle

### Continuous Improvement
- Add more benchmarks
- Profile new features
- Keep dependencies updated
- Share learnings with team

---

## APPENDIX

### Tools & Resources
- Android Studio Profiler
- Systrace / Perfetto
- Firebase Performance
- Macrobenchmark
- Layout Inspector
- Memory Profiler

### Documentation
- Performance best practices
- Benchmark writing guide
- Profiling guide
- Optimization checklist

### References
- [Android Performance Patterns](https://www.youtube.com/playlist?list=PLWz5rJ2EKKc9CBxr3BVjPTPoDPLdPIFCE)
- [Compose Performance](https://developer.android.com/jetpack/compose/performance)
- [Room Performance](https://developer.android.com/training/data-storage/room/async-queries)
- [OkHttp Best Practices](https://square.github.io/okhttp/features/caching/)
