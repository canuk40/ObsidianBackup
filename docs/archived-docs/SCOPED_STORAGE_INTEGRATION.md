# Scoped Storage Integration Examples

## Quick Start Guide for Developers

This document provides practical examples of how to use the new scoped storage components in ObsidianBackup.

## Basic Setup

### 1. Inject Dependencies

```kotlin
class MyBackupActivity : AppCompatActivity() {
    
    @Inject
    lateinit var fileSystemManager: FileSystemManager
    
    @Inject
    lateinit var mediaStoreHelper: MediaStoreHelper
    
    @Inject
    lateinit var safHelper: SafHelper
    
    @Inject
    lateinit var storagePermissionHelper: StoragePermissionHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hilt will inject dependencies
    }
}
```

## Common Use Cases

### Use Case 1: Create a Backup (Standard Operation)

**No permissions required!** This uses app-private storage.

```kotlin
suspend fun createBackup(appIds: List<AppId>) {
    // Get backup directory (automatic, no permissions)
    val backupDir = fileSystemManager.getBackupDirectory()
    
    // Check if enough space
    val requiredSpace = estimateBackupSize(appIds)
    if (!fileSystemManager.hasEnoughSpace(requiredSpace)) {
        showError("Insufficient storage space")
        return
    }
    
    // Create snapshot directory
    val snapshotId = UUID.randomUUID().toString()
    val snapshotDir = fileSystemManager.createSnapshotDirectory(snapshotId)
    
    // Perform backup (existing backup engine code)
    performBackup(appIds, snapshotDir)
    
    // Done! Backup stored in app-private storage
}
```

### Use Case 2: List All Backups

```kotlin
suspend fun loadBackups(): List<BackupInfo> {
    val backups = fileSystemManager.listBackups()
    
    return backups.map { snapshotDir ->
        BackupInfo(
            id = snapshotDir.name,
            timestamp = snapshotDir.lastModified(),
            size = calculateSize(snapshotDir)
        )
    }
}
```

### Use Case 3: Export Backup to Shared Storage (Android 10+)

```kotlin
suspend fun exportBackupToDocuments(snapshotId: String) {
    // Only available on Android 10+
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        showError("Feature requires Android 10+")
        return
    }
    
    // Get snapshot directory
    val snapshotDir = fileSystemManager.getSnapshotDirectory(snapshotId)
        ?: return
    
    // Create archive file in cache
    val archiveFile = File(fileSystemManager.getCacheDirectory(), 
        "backup_${snapshotId}.tar.zst")
    
    // Archive the snapshot (compress to single file)
    archiveSnapshot(snapshotDir, archiveFile)
    
    // Export to MediaStore
    val result = mediaStoreHelper.exportBackup(
        sourceFile = archiveFile,
        displayName = "obsidian_backup_${System.currentTimeMillis()}.tar.zst"
    )
    
    result.onSuccess { uri ->
        showSuccess("Exported to Documents/ObsidianBackup")
        // Optionally share the URI
        shareBackup(uri)
    }.onFailure { error ->
        showError("Export failed: ${error.message}")
    }
    
    // Clean up cache file
    archiveFile.delete()
}
```

### Use Case 4: Let User Choose Export Location (SAF)

```kotlin
private val safExportLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        result.data?.data?.let { uri ->
            lifecycleScope.launch {
                handleSafExport(uri)
            }
        }
    }
}

fun exportBackupToCustomLocation(snapshotId: String) {
    // Store snapshot ID for later
    pendingExportSnapshotId = snapshotId
    
    // Launch directory picker
    val intent = safHelper.createDirectoryPickerIntent()
    safExportLauncher.launch(intent)
}

private suspend fun handleSafExport(directoryUri: Uri) {
    val snapshotId = pendingExportSnapshotId ?: return
    
    // Persist permissions
    safHelper.persistDirectoryPermissions(directoryUri)
    
    // Get snapshot
    val snapshotDir = fileSystemManager.getSnapshotDirectory(snapshotId)
        ?: return
    
    // Create archive
    val archiveFile = File(fileSystemManager.getCacheDirectory(), 
        "backup_${snapshotId}.tar.zst")
    archiveSnapshot(snapshotDir, archiveFile)
    
    // Export to chosen directory
    val result = safHelper.exportFileToSafDirectory(
        sourceFile = archiveFile,
        safDirectoryUri = directoryUri,
        fileName = "obsidian_backup_${System.currentTimeMillis()}.tar.zst"
    )
    
    result.onSuccess { uri ->
        showSuccess("Exported successfully")
    }.onFailure { error ->
        showError("Export failed: ${error.message}")
    }
    
    archiveFile.delete()
}
```

### Use Case 5: Import Backup from File

```kotlin
private val importLauncher = registerForActivityResult(
    ActivityResultContracts.OpenDocument()
) { uri ->
    uri?.let { 
        lifecycleScope.launch {
            importBackup(it)
        }
    }
}

fun startImportBackup() {
    importLauncher.launch(arrayOf("*/*"))
}

private suspend fun importBackup(sourceUri: Uri) {
    // Create destination in app-private storage
    val snapshotId = UUID.randomUUID().toString()
    val archiveFile = File(
        fileSystemManager.getCacheDirectory(),
        "imported_${snapshotId}.tar.zst"
    )
    
    // Import from URI
    val result = mediaStoreHelper.importBackup(sourceUri, archiveFile)
    
    result.onSuccess { file ->
        // Extract to snapshot directory
        val snapshotDir = fileSystemManager.createSnapshotDirectory(snapshotId)
        extractArchive(file, snapshotDir)
        
        showSuccess("Backup imported successfully")
        file.delete()
    }.onFailure { error ->
        showError("Import failed: ${error.message}")
    }
}
```

### Use Case 6: Check Storage Status

```kotlin
fun showStorageStatus() {
    lifecycleScope.launch {
        val stats = fileSystemManager.getStorageStats()
        
        val message = """
            Total Space: ${formatBytes(stats.totalSpace)}
            Available: ${formatBytes(stats.usableSpace)}
            Backups Size: ${formatBytes(stats.backupSize)}
            Used: ${stats.percentUsed.roundToInt()}%
        """.trimIndent()
        
        showDialog("Storage Status", message)
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> "%.2f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.2f KB".format(bytes / 1_000.0)
        else -> "$bytes bytes"
    }
}
```

### Use Case 7: Check Permissions Status

```kotlin
fun checkStoragePermissions() {
    val status = storagePermissionHelper.getStoragePermissionStatus()
    
    // App-private storage is always available
    check(status.hasAppPrivateAccess) { "App-private storage must be available" }
    
    // Log status for debugging
    storagePermissionHelper.logStorageStatus()
    
    // Determine what features are available
    when {
        status.hasAllFilesAccess -> {
            // Full access - enable advanced root/Shizuku features
            enableAdvancedFeatures()
        }
        status.recommendedApproach == StorageApproach.SCOPED_STORAGE -> {
            // Modern scoped storage - standard features
            enableStandardFeatures()
        }
        else -> {
            // Basic features only
            enableBasicFeatures()
        }
    }
}
```

### Use Case 8: Request Advanced Permissions (Optional)

**Only for root/Shizuku features!** Normal users don't need this.

```kotlin
fun requestAdvancedStorageAccess() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Create intent for MANAGE_EXTERNAL_STORAGE
        val intent = storagePermissionHelper.createManageStorageIntent()
        if (intent != null) {
            // Show explanation dialog first
            showDialog(
                "Advanced Features",
                "This permission is only needed for root/Shizuku-based operations. " +
                "Standard backups work without it.",
                positiveButton = "Continue" to {
                    startActivity(intent)
                },
                negativeButton = "Cancel" to {}
            )
        }
    } else {
        showInfo("Advanced features not available on this Android version")
    }
}
```

### Use Case 9: Clean Up Old Backups

```kotlin
suspend fun cleanupOldBackups(keepCount: Int = 10) {
    val backups = fileSystemManager.listBackups()
        .sortedByDescending { it.lastModified() }
    
    val toDelete = backups.drop(keepCount)
    
    var deletedSize = 0L
    var deletedCount = 0
    
    for (backup in toDelete) {
        val size = fileSystemManager.calculateDirectorySize(backup)
        if (fileSystemManager.deleteSnapshotDirectory(backup.name)) {
            deletedSize += size
            deletedCount++
        }
    }
    
    showSuccess("Deleted $deletedCount old backups (${formatBytes(deletedSize)} freed)")
}
```

### Use Case 10: Handle Storage Migration

**This happens automatically on app startup, but you can check status:**

```kotlin
fun checkMigrationStatus() {
    val status = scopedStorageMigration.getMigrationStatus()
    
    if (status.needsMigration) {
        showInfo("Storage migration in progress...")
        
        lifecycleScope.launch {
            val result = scopedStorageMigration.performMigrationIfNeeded()
            
            when (result) {
                is MigrationResult.Success -> {
                    showSuccess(
                        "Migrated ${result.migratedFiles} backups " +
                        "(${formatBytes(result.migratedBytes)})"
                    )
                }
                is MigrationResult.Failed -> {
                    showError("Migration failed: ${result.error}")
                }
                else -> {
                    // Already completed or not needed
                }
            }
        }
    } else {
        showInfo("Storage migration already completed")
    }
}
```

## UI Integration Examples

### ViewModel Example

```kotlin
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val fileSystemManager: FileSystemManager,
    private val mediaStoreHelper: MediaStoreHelper,
    private val storagePermissionHelper: StoragePermissionHelper
) : ViewModel() {
    
    private val _backups = MutableStateFlow<List<BackupInfo>>(emptyList())
    val backups: StateFlow<List<BackupInfo>> = _backups.asStateFlow()
    
    private val _storageStats = MutableStateFlow<StorageStats?>(null)
    val storageStats: StateFlow<StorageStats?> = _storageStats.asStateFlow()
    
    init {
        loadBackups()
        loadStorageStats()
    }
    
    fun loadBackups() {
        viewModelScope.launch {
            val backupList = fileSystemManager.listBackups()
                .map { dir ->
                    BackupInfo(
                        id = dir.name,
                        timestamp = dir.lastModified(),
                        size = fileSystemManager.calculateDirectorySize(dir)
                    )
                }
            _backups.value = backupList
        }
    }
    
    fun loadStorageStats() {
        viewModelScope.launch {
            _storageStats.value = fileSystemManager.getStorageStats()
        }
    }
    
    fun exportBackup(backupId: String) {
        viewModelScope.launch {
            // Implementation from Use Case 3
        }
    }
    
    fun deleteBackup(backupId: String) {
        viewModelScope.launch {
            if (fileSystemManager.deleteSnapshotDirectory(backupId)) {
                loadBackups()
                loadStorageStats()
            }
        }
    }
}
```

### Compose UI Example

```kotlin
@Composable
fun BackupListScreen(viewModel: BackupViewModel = hiltViewModel()) {
    val backups by viewModel.backups.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()
    
    Column {
        // Storage stats card
        storageStats?.let { stats ->
            StorageStatsCard(stats)
        }
        
        // Backup list
        LazyColumn {
            items(backups) { backup ->
                BackupItem(
                    backup = backup,
                    onExport = { viewModel.exportBackup(backup.id) },
                    onDelete = { viewModel.deleteBackup(backup.id) }
                )
            }
        }
    }
}

@Composable
fun StorageStatsCard(stats: StorageStats) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Storage Usage", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = stats.percentUsed / 100f,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("${stats.percentUsed.roundToInt()}% used")
            Text("${formatBytes(stats.backupSize)} / ${formatBytes(stats.totalSpace)}")
        }
    }
}
```

## Best Practices

### 1. Always Use FileSystemManager

❌ **Don't:**
```kotlin
val backupDir = File("/sdcard/ObsidianBackup")
```

✅ **Do:**
```kotlin
val backupDir = fileSystemManager.getBackupDirectory()
```

### 2. Check Space Before Operations

```kotlin
if (!fileSystemManager.hasEnoughSpace(requiredBytes)) {
    // Show error or prompt cleanup
    return
}
```

### 3. Handle Errors Gracefully

```kotlin
result.onSuccess { uri ->
    // Handle success
}.onFailure { error ->
    // Log and show user-friendly message
    logger.e(TAG, "Operation failed", error)
    showError("Operation failed: ${error.message}")
}
```

### 4. Clean Up Cache Files

```kotlin
try {
    // Use cache file
    processFile(cacheFile)
} finally {
    // Always clean up
    cacheFile.delete()
}
```

### 5. Use Coroutines for IO Operations

```kotlin
// Always use IO dispatcher for file operations
viewModelScope.launch(Dispatchers.IO) {
    fileSystemManager.exportBackup(...)
}
```

## Testing

### Unit Test Example

```kotlin
@Test
fun `test backup directory creation`() = runTest {
    val fileSystemManager = FileSystemManager(context, logger)
    
    val backupDir = fileSystemManager.getBackupDirectory()
    
    assertThat(backupDir.exists()).isTrue()
    assertThat(backupDir.isDirectory).isTrue()
    assertThat(backupDir.canWrite()).isTrue()
}
```

### Integration Test Example

```kotlin
@Test
fun `test export and import backup`() = runTest {
    // Create test backup
    val snapshotId = UUID.randomUUID().toString()
    val snapshotDir = fileSystemManager.createSnapshotDirectory(snapshotId)
    
    // Add test files
    File(snapshotDir, "test.txt").writeText("test data")
    
    // Export (if Android 10+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val archiveFile = File(fileSystemManager.getCacheDirectory(), "test.tar")
        // ... archive logic ...
        
        val result = mediaStoreHelper.exportBackup(archiveFile, "test_backup.tar")
        
        assertThat(result.isSuccess).isTrue()
    }
}
```

## Troubleshooting

### Issue: FileNotFoundException

**Cause**: Trying to access non-existent file or directory

**Solution**: Always check existence before accessing:
```kotlin
val dir = fileSystemManager.getSnapshotDirectory(snapshotId)
if (dir == null || !dir.exists()) {
    // Handle missing directory
    return
}
```

### Issue: SecurityException on Export

**Cause**: Trying to write to protected location without permission

**Solution**: Use MediaStore or SAF for exports:
```kotlin
// Use MediaStore (Android 10+)
mediaStoreHelper.exportBackup(file, displayName)

// Or use SAF (all versions)
safHelper.exportFileToSafDirectory(file, userChosenUri, fileName)
```

### Issue: Out of Space

**Cause**: Insufficient storage space

**Solution**: Check before operations and provide cleanup options:
```kotlin
if (!fileSystemManager.hasEnoughSpace(requiredBytes)) {
    // Show cleanup dialog
    showCleanupDialog()
    return
}
```

## Summary

- ✅ Use `FileSystemManager` for all file operations
- ✅ App-private storage requires no permissions
- ✅ Use MediaStore or SAF for user-accessible exports
- ✅ Handle errors with Result types
- ✅ Clean up cache files after use
- ✅ Check storage space before operations
- ✅ Use coroutines with IO dispatcher

This keeps the app compliant with modern Android storage requirements while maintaining excellent UX!
