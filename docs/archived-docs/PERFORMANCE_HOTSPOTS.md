# Performance Hotspots Reference
**Quick reference for critical performance bottlenecks**

---

## 🔥 CRITICAL HOTSPOTS (Fix Immediately)

### 1. Main Thread Blocking in DI Modules
**Impact:** App startup delay, ANRs  
**Location:** `di/CloudModule.kt:67, 82, 106`

```kotlin
// BLOCKER: Lines 67, 82, 106
@Provides fun provideConfig(): Config = runBlocking { ... }  // 🔴 BLOCKS MAIN THREAD!
```

**Fix:** Use provider pattern, lazy init, or async loading  
**Effort:** 5 SP (1 day)  
**ROI:** HIGH - reduces startup time by 100-200ms

---

### 2. N+1 Query Problem
**Impact:** UI freezes with many snapshots  
**Location:** `storage/BackupCatalog.kt`

```kotlin
// PROBLEM: Loading 100 snapshots = 101 queries!
val snapshots = dao.getAllSnapshots()  // 1 query
snapshots.forEach { snapshot ->
    val apps = dao.getApps(snapshot.id)  // N queries!
}
```

**Fix:** Use JOIN query or batch loading  
**Effort:** 8 SP (1.5 days)  
**ROI:** HIGH - 10x faster list loading

---

### 3. Unbuffered File I/O
**Impact:** 3-5x slower file operations  
**Location:** 15+ files

```kotlin
// SLOW: No buffering
FileInputStream(file).use { ... }  // 🐌

// FAST: With buffering
FileInputStream(file).buffered(8192).use { ... }  // ⚡
```

**Files:**
- `crypto/ZeroKnowledgeEncryption.kt:193-194, 240-249`
- `verification/ChecksumVerifier.kt:89`
- `cloud/WebDavCloudProvider.kt:501, 587`
- 10+ cloud providers

**Fix:** Add `.buffered()` to all streams  
**Effort:** 8 SP (1.5 days)  
**ROI:** HIGH - 3-5x speedup on large files

---

### 4. Missing @Transaction in Database
**Impact:** Data corruption, race conditions  
**Location:** All DAOs

```kotlin
// UNSAFE: Not atomic
suspend fun deleteSnapshot(id: String) {
    deleteSnapshotById(id)
    deleteAppBackups(id)  // Can fail halfway!
}

// SAFE: Wrapped in transaction
@Transaction
suspend fun deleteSnapshot(id: String) { ... }
```

**Fix:** Add `@Transaction` to multi-step operations  
**Effort:** 2 SP (0.5 days)  
**ROI:** HIGH - prevents data loss

---

### 5. Full-Resolution Bitmap Loading
**Impact:** Memory spikes, OOM crashes  
**Location:** `scanner/AppScanner.kt:142-145`

```kotlin
// MEMORY HOG: Loads full resolution (e.g., 512x512 ARGB_8888 = 1MB per icon!)
fun loadAppIcon(pkg: String): Bitmap? = 
    packageManager.getApplicationIcon(pkg).toBitmap()

// OPTIMIZED: 128x128 RGB_565 = 32KB per icon (31x smaller!)
fun loadAppIcon(pkg: String): Bitmap? = 
    packageManager.getApplicationIcon(pkg).toBitmap(128, 128, RGB_565)
```

**Fix:** Downsample to display size (128x128)  
**Effort:** 5 SP (1 day)  
**ROI:** HIGH - 75% memory reduction

---

## 🔥 HIGH PRIORITY HOTSPOTS

### 6. Missing Database Indexes
**Impact:** Slow queries, slow deletes  
**Location:** `storage/AppBackupDao.kt`, `storage/LogDao.kt`

```sql
-- MISSING INDEXES:
CREATE INDEX idx_app_backup_snapshot ON app_backups(snapshot_id);
CREATE INDEX idx_app_backup_app_time ON app_backups(app_id, backup_timestamp);
CREATE INDEX idx_log_level_time ON logs(level, timestamp);
```

**Fix:** Add indexes to foreign keys and common WHERE clauses  
**Effort:** 3 SP (0.5 days)  
**ROI:** MEDIUM-HIGH - faster queries

---

### 7. Serial File Uploads
**Impact:** Slow cloud backups  
**Location:** Backup orchestrator

```kotlin
// SLOW: One at a time
files.forEach { file -> 
    cloudProvider.upload(file)  // Waits for each!
}

// FAST: Parallel uploads (3 concurrent)
files.map { file ->
    async { cloudProvider.upload(file) }
}.awaitAll()
```

**Fix:** Upload files in parallel with semaphore  
**Effort:** 5 SP (1 day)  
**ROI:** MEDIUM-HIGH - 50% faster uploads

---

### 8. No HTTP Caching
**Impact:** Redundant downloads, bandwidth waste  
**Location:** All OkHttpClient instances

```kotlin
// NO CACHE: Re-downloads every time
OkHttpClient.Builder().build()

// WITH CACHE: Saves bandwidth
OkHttpClient.Builder()
    .cache(Cache(cacheDir, 50 * 1024 * 1024))  // 50MB
    .build()
```

**Fix:** Add HTTP cache to OkHttp clients  
**Effort:** 3 SP (0.5 days)  
**ROI:** MEDIUM - 30% bandwidth reduction

---

### 9. Missing LazyColumn Keys
**Impact:** Unnecessary recompositions  
**Location:** 35+ LazyColumn instances

```kotlin
// BAD: Recomposes entire list on any change
LazyColumn {
    items(apps) { app -> AppItem(app) }
}

// GOOD: Only recomposes changed items
LazyColumn {
    items(apps, key = { it.id }) { app -> AppItem(app) }
}
```

**Fix:** Add `key` parameter to all `items()` calls  
**Effort:** 5 SP (1 day)  
**ROI:** MEDIUM - smoother scrolling

---

### 10. Unstable Compose Lambdas
**Impact:** Excessive recompositions  
**Location:** Multiple screens

```kotlin
// BAD: New lambda on every recomposition
@Composable
fun Screen() {
    Button(onClick = { doSomething() }) { ... }  // New object!
}

// GOOD: Stable callback
@Composable
fun Screen() {
    val onClick = remember { { doSomething() } }
    Button(onClick = onClick) { ... }
}
```

**Fix:** Remember lambdas or use stable callbacks  
**Effort:** 3 SP (0.5 days)  
**ROI:** MEDIUM - fewer recompositions

---

## 🟡 MEDIUM PRIORITY HOTSPOTS

### 11. Large JSON Columns in Memory
**Impact:** Slow list loading, memory waste  
**Location:** `storage/BackupCatalog.kt:35-36`

```kotlin
// BLOATED: Loads everything even for list view
data class SnapshotEntity(
    val id: String,
    val appsJson: String,        // 100KB+
    val componentsJson: String,  // 50KB+
    val checksumsJson: String    // 200KB+
)
```

**Fix:** Move to separate table, lazy load  
**Effort:** 5 SP (1 day)  
**ROI:** MEDIUM - 60% memory reduction

---

### 12. No Coroutine Timeouts
**Impact:** Operations can hang indefinitely  
**Location:** Network operations

```kotlin
// HANGS: No timeout
suspend fun sync() {
    apiClient.performSync()  // Could wait forever!
}

// SAFE: With timeout
suspend fun sync() = withTimeout(30_000) {
    apiClient.performSync()
}
```

**Fix:** Add timeouts to all network operations  
**Effort:** 5 SP (1 day)  
**ROI:** LOW-MEDIUM - better UX

---

### 13. No Image Cache Limits
**Impact:** Unbounded memory growth  
**Location:** Image loading configuration

```kotlin
// Configure Coil
ImageLoader.Builder(context)
    .memoryCache {
        MemoryCache.Builder(context)
            .maxSizePercent(0.15)  // Limit to 15% of heap
            .build()
    }
    .build()
```

**Fix:** Configure image cache size limits  
**Effort:** 3 SP (0.5 days)  
**ROI:** LOW-MEDIUM - prevents OOM

---

### 14. No Full-Text Search for Logs
**Impact:** Slow log filtering  
**Location:** `storage/LogDao.kt`

```kotlin
// SLOW: Uses LIKE
@Query("SELECT * FROM logs WHERE message LIKE '%' || :term || '%'")

// FAST: Use FTS4/FTS5
@Query("SELECT * FROM logs JOIN logs_fts WHERE logs_fts MATCH :term")
```

**Fix:** Add FTS virtual table  
**Effort:** 5 SP (1 day)  
**ROI:** LOW-MEDIUM - faster search

---

## 📊 PERFORMANCE TESTING CHECKLIST

### Before Each PR
- [ ] Run unit tests
- [ ] Profile memory usage
- [ ] Check for StrictMode violations
- [ ] Review layout inspector

### Before Merging Epic
- [ ] Run benchmark suite
- [ ] Memory Profiler shows no leaks
- [ ] CPU Profiler shows no spikes
- [ ] Systrace shows 60 FPS
- [ ] No ANRs in stress test

### Before Release
- [ ] All benchmarks pass
- [ ] Performance regression tests pass
- [ ] Beta metrics show improvement
- [ ] No P0/P1 performance bugs

---

## 🛠️ QUICK FIX TEMPLATES

### Add Buffering to File Stream
```kotlin
// Before
FileInputStream(file).use { input -> ... }

// After
FileInputStream(file).buffered(8192).use { input -> ... }
```

### Add Database Index
```kotlin
@Entity(
    tableName = "table_name",
    indices = [
        Index(value = ["foreign_key"], name = "idx_table_fk")
    ]
)
```

### Add Transaction
```kotlin
@Transaction
suspend fun multiStepOperation() {
    dao.step1()
    dao.step2()
}
```

### Add LazyColumn Key
```kotlin
LazyColumn {
    items(list, key = { it.id }) { item ->
        ItemComposable(item)
    }
}
```

### Add HTTP Cache
```kotlin
OkHttpClient.Builder()
    .cache(Cache(File(cacheDir, "http"), 50 * 1024 * 1024))
    .build()
```

### Add Coroutine Timeout
```kotlin
suspend fun operation() = withTimeout(30_000) {
    // operation code
}
```

### Downsample Bitmap
```kotlin
fun Drawable.toBitmap(size: Int = 128): Bitmap {
    // ... create scaled bitmap
}
```

### Remember Lambda
```kotlin
@Composable
fun Screen() {
    val onClick = remember { { doAction() } }
    Button(onClick = onClick) { ... }
}
```

---

## 📈 PERFORMANCE METRICS DASHBOARD

### Targets

| Metric | Current | Target | Priority |
|--------|---------|--------|----------|
| App Startup | 1200ms | <800ms | 🔴 Critical |
| Snapshot List (100) | 500ms | <100ms | 🔴 Critical |
| File Encrypt (1MB) | 150ms | <100ms | 🟡 High |
| Icon Load | 50ms | <20ms | 🟡 High |
| Cloud Upload (10MB) | 45s | <20s | 🟡 High |
| Memory (idle) | 150MB | <100MB | 🟡 High |
| Memory (peak) | 350MB | <250MB | 🟡 High |
| Scroll FPS | 45 | 60 | 🟢 Medium |

### Success Criteria

**Ship when:**
- ✅ All critical metrics meet targets
- ✅ No P0/P1 performance bugs
- ✅ Benchmark suite passes
- ✅ Beta metrics show 30% improvement
- ✅ No user complaints about slowness

---

## 🔍 PROFILING COMMANDS

### Memory Profiling
```bash
# Dump heap
adb shell am dumpheap com.obsidianbackup /sdcard/heap.hprof
adb pull /sdcard/heap.hprof

# Analyze with Android Studio Memory Profiler
```

### CPU Profiling
```bash
# Start systrace
python systrace.py -t 10 -o trace.html sched freq idle am wm gfx view binder_driver hal dalvik camera input res

# Analyze with Chrome: chrome://tracing
```

### Database Profiling
```sql
-- Check query plans
EXPLAIN QUERY PLAN SELECT * FROM snapshots WHERE timestamp > ?;

-- Analyze table stats
ANALYZE;
```

### Network Profiling
```bash
# Monitor network traffic
adb shell tcpdump -i any -s0 -w /sdcard/capture.pcap
adb pull /sdcard/capture.pcap

# Analyze with Wireshark
```

---

## 📚 REFERENCES

### Documentation
- [Android Performance Best Practices](https://developer.android.com/topic/performance)
- [Jetpack Compose Performance](https://developer.android.com/jetpack/compose/performance)
- [Room Performance](https://developer.android.com/training/data-storage/room/async-queries)
- [OkHttp Recipes](https://github.com/square/okhttp/wiki/Recipes)

### Tools
- Android Studio Profiler
- Macrobenchmark
- Systrace / Perfetto
- Layout Inspector
- Firebase Performance

### Videos
- [Android Performance Patterns](https://www.youtube.com/playlist?list=PLWz5rJ2EKKc9CBxr3BVjPTPoDPLdPIFCE)
- [Jetpack Compose Performance](https://www.youtube.com/watch?v=EOQB8PTLkpY)

---

## 🚀 QUICK WINS (< 1 Hour Each)

1. ✅ Add buffering to 3 most-used file streams
2. ✅ Add 1 critical database index
3. ✅ Enable HTTP caching on main OkHttpClient
4. ✅ Add keys to 5 most-scrolled LazyColumns
5. ✅ Remember 3 most-recomposed lambdas
6. ✅ Add timeout to cloud sync operation
7. ✅ Enable StrictMode in debug builds
8. ✅ Configure Coil memory cache limit

**Total Time:** ~4-5 hours  
**Impact:** Noticeable performance improvement

---

## 🎯 1-WEEK SPRINT

### Day 1: Database
- Remove runBlocking from DI modules
- Add critical indexes
- Add @Transaction annotations

### Day 2: File I/O
- Add buffering to all file streams
- Optimize file copy with progress

### Day 3: Memory
- Implement bitmap downsampling
- Configure image cache limits

### Day 4: Compose
- Add keys to all LazyColumns
- Fix unstable lambdas

### Day 5: Network
- Add HTTP caching
- Implement parallel uploads

### Day 6: Testing
- Write benchmarks
- Profile all changes

### Day 7: Buffer
- Fix any issues
- Document improvements

---

## 📞 HELP & SUPPORT

**Performance Questions:**
- Check documentation first
- Ask in #performance Slack channel
- Escalate critical issues to tech lead

**Tools Issues:**
- Android Studio Profiler guide
- Macrobenchmark setup guide
- StrictMode interpretation guide

**Code Review:**
- All performance changes need approval
- Include benchmark results in PR
- Show before/after profiling
