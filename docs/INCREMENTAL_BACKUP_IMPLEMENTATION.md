# Incremental Backup Implementation

## Overview

This document describes the implementation of incremental file scanning for the ObsidianBackup Android application. The implementation is based on industry-standard algorithms from rsync, git, and research on efficient filesystem traversal.

## Location

**File**: `app/src/main/java/com/obsidianbackup/engine/IncrementalBackupStrategy.kt`

## Algorithm Design

### Three-Level Change Detection

The implementation uses a cascading detection strategy inspired by rsync and git:

#### Level 1: Size Comparison (Instant)
- Compares current file size with previous snapshot
- If size differs, file is definitely modified
- No disk I/O required - uses filesystem metadata only
- **Complexity**: O(1) per file

#### Level 2: Modification Time (mtime) Comparison (Instant)
- Compares last modification timestamp with previous snapshot
- If mtime differs (but size matches), file is likely modified
- Still uses only filesystem metadata
- **Complexity**: O(1) per file

#### Level 3: Content Hash Comparison (On-Demand)
- Only executed if size OR mtime changed
- Calculates SHA-256 checksum of file content
- Guarantees detection of actual content changes
- **Complexity**: O(file_size) per changed file

### Optimization: Unchanged File Handling

If both size AND mtime match the previous snapshot:
- File is considered unchanged
- Previous checksum is reused without re-hashing
- Dramatically reduces I/O for large backups with few changes
- **Example**: 100,000 files with 100 changed → only 100 hash calculations instead of 100,000

## Architecture

### Core Components

```kotlin
class IncrementalBackupStrategy(
    private val catalog: BackupCatalog,
    private val checksumVerifier: ChecksumVerifier,
    private val parallelism: Int = Runtime.getRuntime().availableProcessors()
)
```

#### Key Methods

1. **`createIncremental()`**
   - Entry point for creating backup plans
   - Returns either Full or Incremental backup plan
   - Compares current state with previous snapshot

2. **`scanAppFiles()`**
   - Scans application data directory
   - Loads previous snapshot metadata
   - Returns list of changed files only

3. **`scanForChangedFiles()`**
   - Implements parallel directory traversal
   - Uses producer-consumer pattern with Channel
   - Processes files concurrently across CPU cores

4. **`walkDirectoryTree()`**
   - Non-recursive tree traversal using explicit stack
   - Prevents stack overflow on deep directory structures
   - Filters out cache/temporary directories

5. **`processFile()`**
   - Three-level detection logic per file
   - Integrates with ChecksumVerifier
   - Returns FileMetadata with change flag

### Data Structures

#### FileSnapshot
```kotlin
data class FileSnapshot(
    val path: String,      // Relative path from app root
    val size: Long,        // File size in bytes
    val mtime: Long,       // Last modification timestamp
    val checksum: String   // SHA-256 hash
)
```

Stored format: `path|size|mtime|checksum` (pipe-separated)

#### FileMetadata
```kotlin
data class FileMetadata(
    val path: String,
    val checksum: String,
    val size: Long,
    val mtime: Long = 0L,
    val unchanged: Boolean = false  // True if verified unchanged
)
```

## Parallel Processing

### Producer-Consumer Architecture

```
┌─────────────┐         ┌──────────────┐         ┌────────────────┐
│  Directory  │  feeds  │   Channel    │  feeds  │  Worker Pool   │
│  Walker     │────────>│  (Unbounded) │────────>│  (N threads)   │
│ (Producer)  │         │    Queue     │         │  (Consumers)   │
└─────────────┘         └──────────────┘         └────────────────┘
                                                          │
                                                          ▼
                                                  ┌──────────────────┐
                                                  │ ConcurrentHashMap│
                                                  │    (Results)     │
                                                  └──────────────────┘
```

### Parallelism Configuration

- Default: `Runtime.getRuntime().availableProcessors()` threads
- Adjustable based on device capabilities
- Each worker processes files independently
- Thread-safe result collection via ConcurrentHashMap

## Performance Characteristics

### Time Complexity

| Operation | Best Case | Average Case | Worst Case |
|-----------|-----------|--------------|------------|
| Unchanged files | O(1) | O(1) | O(1) |
| Changed files (small) | O(n) | O(n) | O(n) |
| Changed files (large) | O(n) | O(n) | O(n) |
| Full tree scan | O(k) | O(k) | O(k) |

Where:
- n = file size
- k = number of files

### Space Complexity

- O(k) for snapshot metadata storage
- O(k/p) for Channel queue size (bounded by parallelism)
- O(k) for results map

### I/O Optimization

For a typical incremental backup scenario:

**Example: 100,000 files, 99,900 unchanged, 100 changed**

Traditional approach:
- Read metadata: 100,000 × ~500 bytes = 50 MB
- Hash all files: 100,000 × average_size = Potentially TB of I/O

Our approach:
- Read metadata: 100,000 × ~500 bytes = 50 MB
- Hash changed only: 100 × average_size = Minimal I/O
- **I/O reduction: 99.9%**

## Snapshot Persistence

### Storage Format

**File**: `{snapshot_dir}/file_snapshot.txt`

**Format**: Pipe-separated values
```
path|size|mtime|checksum
data/shared_prefs/prefs.xml|1024|1707523200000|a1b2c3d4...
databases/app.db|524288|1707523210000|e5f6g7h8...
```

### Advantages
- Human-readable format
- Easy to parse and validate
- Portable across systems
- Minimal overhead

## Android-Specific Optimizations

### Directory Filtering

Automatically skips:
- Cache directories (`cache`, `code_cache`)
- No-backup directories (`no_backup`)
- Hidden files/directories (starting with `.`)
- Temporary files (`.tmp`, `.temp`)

### Path Handling

- Uses Android app data path: `/data/data/{package_name}`
- Relative paths from app root for portability
- SecurityException handling for restricted directories

## Integration Points

### ChecksumVerifier Integration

```kotlin
suspend fun calculateChecksum(file: File): String
```

- Uses SHA-256 algorithm
- 8KB buffer for efficient reading
- Runs on Dispatchers.IO for non-blocking operation

### BackupCatalog Integration

```kotlin
suspend fun getSnapshot(id: BackupId): BackupMetadata?
fun getSnapshotDirectory(id: BackupId): File
```

- Loads previous backup metadata
- Accesses snapshot storage directories
- Manages snapshot lifecycle

## Usage Example

```kotlin
val strategy = IncrementalBackupStrategy(
    catalog = backupCatalog,
    checksumVerifier = checksumVerifier,
    parallelism = 4  // Use 4 worker threads
)

// Create incremental backup plan
val plan = strategy.createIncremental(
    appId = AppId("com.example.app"),
    lastSnapshotId = BackupId("snapshot_20240101")
)

when (plan) {
    is BackupPlan.Full -> {
        // First backup - all files
        println("Full backup: ${plan.files.size} files")
    }
    is BackupPlan.Incremental -> {
        // Incremental - only changed files
        println("Incremental backup: ${plan.changedFiles.size} changed files")
        println("Base snapshot: ${plan.baseSnapshot.value}")
    }
}

// After backup, save metadata for next incremental
strategy.saveSnapshotMetadata(
    snapshotId = newSnapshotId,
    files = allFiles
)
```

## Error Handling

### SecurityException
- Caught during directory traversal
- Logged but doesn't stop entire scan
- Affected directories skipped

### File Access Errors
- Non-existent files: returned as null
- Unreadable files: returned as null
- Doesn't crash entire backup operation

### Corrupted Metadata
- Parse errors: ignored individual lines
- Missing metadata file: treated as full backup
- Fallback to full scan on errors

## Testing Considerations

### Test Scenarios

1. **First backup (no previous snapshot)**
   - Should hash all files
   - Should create initial snapshot metadata

2. **No changes since last backup**
   - Should reuse all previous checksums
   - Minimal I/O operations

3. **Few files changed**
   - Should detect changed files via size/mtime
   - Should hash only changed files

4. **File deleted**
   - Use `detectDeletedFiles()` to identify
   - Compare current vs previous file lists

5. **Large directory (100,000+ files)**
   - Should complete in reasonable time
   - Should not exhaust memory
   - Should parallelize effectively

6. **Deep directory structure**
   - Should not cause stack overflow
   - Non-recursive algorithm handles any depth

## Performance Benchmarks (Expected)

Based on research and algorithm design:

| Scenario | Files | Changed | Time (est.) | I/O |
|----------|-------|---------|-------------|-----|
| First backup | 10,000 | 10,000 | ~30s | Full |
| No changes | 10,000 | 0 | ~0.5s | Minimal |
| 1% changed | 10,000 | 100 | ~2s | Minimal |
| Large backup | 100,000 | 1,000 | ~20s | 1% |

*Note: Actual performance depends on device, storage type, file sizes*

## Future Enhancements

### Potential Optimizations

1. **Block-level deduplication**
   - Implement rsync rolling checksum
   - Transfer only changed blocks for large files

2. **Bloom filters**
   - Quick existence check for deleted files
   - Reduce memory for large file sets

3. **Compressed metadata**
   - Use binary format for snapshot storage
   - Reduce metadata file size by 50-70%

4. **Adaptive parallelism**
   - Adjust thread count based on file count/size
   - Optimize for small vs large file scenarios

5. **Incremental hashing**
   - Hash files as they're modified (background)
   - Reduce backup time further

## References

### Research Sources

1. **Rsync Algorithm**
   - Three-level detection: size → mtime → checksum
   - Link-dest for space-efficient incrementals
   - Delta transfer algorithm

2. **Git Change Detection**
   - Index-based metadata tracking
   - Efficient scanning via lstat metadata
   - "Racy-git" problem solution

3. **File Tree Traversal**
   - User-space I/O scheduling (78% improvement)
   - Concurrent traversal patterns
   - Non-recursive stack-based algorithms

4. **Android Storage Optimization**
   - SAF vs java.io.File performance
   - Metadata caching strategies
   - Directory exclusion patterns

## Conclusion

This implementation provides:
- ✅ Efficient incremental scanning for 100,000+ files
- ✅ Minimal I/O via three-level detection
- ✅ Parallel processing for modern multi-core devices
- ✅ Robust error handling
- ✅ Android-specific optimizations
- ✅ Industry-standard algorithms (rsync, git)

The solution balances performance, reliability, and maintainability for Android backup operations.
