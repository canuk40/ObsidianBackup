// engine/IncrementalBackupStrategy.kt
package com.obsidianbackup.engine

import androidx.collection.LruCache
import com.obsidianbackup.model.*
import com.obsidianbackup.security.PathSecurityValidator
import com.obsidianbackup.storage.BackupCatalog
import com.obsidianbackup.verification.ChecksumVerifier
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class IncrementalBackupStrategy(
    private val catalog: BackupCatalog,
    private val checksumVerifier: ChecksumVerifier,
    private val parallelism: Int = Runtime.getRuntime().availableProcessors()
) {
    private val chunkCache = LruCache<String, ChunkMetadata>(1000)
    
    // Deduplication: map checksum -> file path (for hard linking)
    private val deduplicationMap = ConcurrentHashMap<String, String>()
    
    // Statistics tracking
    private val hardLinksCreated = AtomicInteger(0)
    private val filesDeduped = AtomicInteger(0)
    private val savedBytes = AtomicLong(0)

    suspend fun createIncremental(
        appId: AppId,
        lastSnapshotId: BackupId?
    ): BackupPlan {
        Timber.tag("IncrementalBackup").d("Creating backup plan for app: ${appId.value}")
        
        val currentFiles = scanAppFiles(appId, lastSnapshotId)

        if (lastSnapshotId == null) {
            Timber.tag("IncrementalBackup").i("No previous snapshot - creating full backup")
            return BackupPlan.Full(currentFiles)
        }

        val lastSnapshot = catalog.getSnapshot(lastSnapshotId)
        val changedFiles = currentFiles.filter { file ->
            val lastChecksum = lastSnapshot?.checksums?.get(file.path)
            lastChecksum == null || lastChecksum != file.checksum
        }

        Timber.tag("IncrementalBackup").i(
            "Incremental plan: ${changedFiles.size} changed, ${currentFiles.size - changedFiles.size} unchanged"
        )

        return BackupPlan.Incremental(
            baseSnapshot = lastSnapshotId,
            changedFiles = changedFiles,
            linkDest = lastSnapshot?.path ?: "",
            unchangedFiles = currentFiles.filter { file ->
                val lastChecksum = lastSnapshot?.checksums?.get(file.path)
                lastChecksum != null && lastChecksum == file.checksum
            }
        )
    }

    /**
     * Scan app files with incremental optimization:
     * 1. Load previous snapshot metadata
     * 2. Compare size/mtime first (fast)
     * 3. Only hash files that potentially changed
     * 4. Use parallel scanning for large directories
     */
    private suspend fun scanAppFiles(
        appId: AppId,
        lastSnapshotId: BackupId?
    ): List<FileMetadata> = withContext(Dispatchers.IO) {
        val appDataDir = getAppDataDirectory(appId)
        if (!appDataDir.exists() || !appDataDir.isDirectory) {
            return@withContext emptyList()
        }

        // Load previous snapshot metadata for comparison
        val previousSnapshot = lastSnapshotId?.let { loadSnapshotMetadata(it) } ?: emptyMap()

        // Scan directory tree and detect changes
        val changedFiles = scanForChangedFiles(appDataDir, previousSnapshot)

        changedFiles
    }

    /**
     * Three-level change detection:
     * Level 1: Size change (instant)
     * Level 2: Modification time change (instant)
     * Level 3: Content checksum (only if needed)
     */
    private suspend fun scanForChangedFiles(
        rootDir: File,
        previousSnapshot: Map<String, FileSnapshot>
    ): List<FileMetadata> = coroutineScope {
        val results = ConcurrentHashMap<String, FileMetadata>()
        val fileQueue = Channel<File>(capacity = Channel.UNLIMITED)

        // Producer: Walk directory tree and enqueue files
        val producerJob = launch {
            walkDirectoryTree(rootDir, rootDir, fileQueue)
            fileQueue.close()
        }

        // Consumers: Process files in parallel
        val consumers = List(parallelism) {
            launch {
                for (file in fileQueue) {
                    val relativePath = file.relativeTo(rootDir).path
                    val metadata = processFile(file, relativePath, previousSnapshot[relativePath])
                    if (metadata != null) {
                        results[relativePath] = metadata
                    }
                }
            }
        }

        // Wait for all work to complete
        producerJob.join()
        consumers.joinAll()

        results.values.toList()
    }

    /**
     * Efficient directory tree traversal using explicit stack (non-recursive)
     * to avoid stack overflow on deep trees
     */
    private suspend fun walkDirectoryTree(
        currentDir: File,
        rootDir: File,
        fileQueue: Channel<File>
    ) {
        val dirStack = ArrayDeque<File>()
        dirStack.add(currentDir)

        while (dirStack.isNotEmpty()) {
            val dir = dirStack.removeFirst()

            val entries = try {
                dir.listFiles() ?: emptyArray()
            } catch (e: SecurityException) {
                emptyArray()
            }

            for (entry in entries) {
                when {
                    entry.isFile && !shouldSkipFile(entry) -> {
                        fileQueue.send(entry)
                    }
                    entry.isDirectory && !shouldSkipDirectory(entry) -> {
                        dirStack.add(entry)
                    }
                }
            }
        }
    }

    /**
     * Process single file with three-level detection
     */
    private suspend fun processFile(
        file: File,
        relativePath: String,
        previousSnapshot: FileSnapshot?
    ): FileMetadata? {
        if (!file.exists() || !file.canRead()) {
            return null
        }

        val currentSize = file.length()
        val currentMtime = file.lastModified()

        // Level 1: Size check (instant)
        if (previousSnapshot != null && currentSize != previousSnapshot.size) {
            // Size changed - definitely modified
            val checksum = checksumVerifier.calculateChecksum(file)
            return FileMetadata(relativePath, checksum, currentSize, currentMtime)
        }

        // Level 2: Modification time check (instant)
        if (previousSnapshot != null && currentMtime != previousSnapshot.mtime) {
            // mtime changed - likely modified, verify with checksum
            val checksum = checksumVerifier.calculateChecksum(file)
            return FileMetadata(relativePath, checksum, currentSize, currentMtime)
        }

        // Level 3: Content unchanged (size + mtime match)
        if (previousSnapshot != null) {
            // Trust the previous checksum without re-hashing (optimization)
            return FileMetadata(
                relativePath,
                previousSnapshot.checksum,
                currentSize,
                currentMtime,
                unchanged = true
            )
        }

        // New file - need to hash
        val checksum = checksumVerifier.calculateChecksum(file)
        return FileMetadata(relativePath, checksum, currentSize, currentMtime)
    }

    /**
     * Load previous snapshot metadata from disk
     */
    private suspend fun loadSnapshotMetadata(snapshotId: BackupId): Map<String, FileSnapshot> =
        withContext(Dispatchers.IO) {
            val snapshotDir = catalog.getSnapshotDirectory(snapshotId)
            val metadataFile = File(snapshotDir, "file_snapshot.txt")

            if (!metadataFile.exists()) {
                return@withContext emptyMap()
            }

            try {
                metadataFile.readLines()
                    .filter { it.isNotBlank() }
                    .mapNotNull { line -> parseSnapshotLine(line) }
                    .associateBy { it.path }
            } catch (e: Exception) {
                emptyMap()
            }
        }

    /**
     * Parse snapshot metadata line: "path|size|mtime|checksum"
     */
    private fun parseSnapshotLine(line: String): FileSnapshot? {
        val parts = line.split("|")
        if (parts.size != 4) return null

        return try {
            FileSnapshot(
                path = parts[0],
                size = parts[1].toLong(),
                mtime = parts[2].toLong(),
                checksum = parts[3]
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Save snapshot metadata for next incremental scan
     */
    suspend fun saveSnapshotMetadata(
        snapshotId: BackupId,
        files: List<FileMetadata>
    ) = withContext(Dispatchers.IO) {
        val snapshotDir = catalog.getSnapshotDirectory(snapshotId)
        snapshotDir.mkdirs()

        val metadataFile = File(snapshotDir, "file_snapshot.txt")
        metadataFile.bufferedWriter().use { writer ->
            files.forEach { file ->
                writer.write("${file.path}|${file.size}|${file.mtime}|${file.checksum}\n")
            }
        }
    }

    /**
     * Detect deleted files by comparing with previous snapshot
     */
    suspend fun detectDeletedFiles(
        currentFiles: List<FileMetadata>,
        lastSnapshotId: BackupId
    ): List<String> = withContext(Dispatchers.IO) {
        val previousSnapshot = loadSnapshotMetadata(lastSnapshotId)
        val currentPaths = currentFiles.map { it.path }.toSet()

        previousSnapshot.keys.filter { path -> path !in currentPaths }
    }

    /**
     * Get app data directory (Android-specific paths)
     * Uses secure path validation to prevent path traversal attacks.
     */
    private fun getAppDataDirectory(appId: AppId): File {
        return PathSecurityValidator.getAppDataDirectory(appId)
    }

    /**
     * Skip system and temporary files
     */
    private fun shouldSkipFile(file: File): Boolean {
        val name = file.name
        return name.startsWith(".") ||
                name.endsWith(".tmp") ||
                name.endsWith(".temp") ||
                name == "cache"
    }

    /**
     * Skip cache and temporary directories
     */
    private fun shouldSkipDirectory(dir: File): Boolean {
        val name = dir.name
        return name.startsWith(".") ||
                name == "cache" ||
                name == "code_cache" ||
                name == "no_backup"
    }
    
    /**
     * Create hard link from base snapshot to new snapshot (optimization)
     * This avoids copying unchanged files - just creates a hard link
     */
    suspend fun createHardLink(
        sourcePath: String,
        destPath: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(sourcePath)
            val destFile = File(destPath)
            
            if (!sourceFile.exists()) {
                Timber.tag("HardLink").w("Source file does not exist: $sourcePath")
                return@withContext false
            }
            
            destFile.parentFile?.mkdirs()
            
            // Use Java NIO Files.createLink for hard link support
            Files.createLink(destFile.toPath(), sourceFile.toPath())
            
            hardLinksCreated.incrementAndGet()
            savedBytes.addAndGet(sourceFile.length())
            
            Timber.tag("HardLink").d("Created hard link: $destPath -> $sourcePath")
            true
        } catch (e: Exception) {
            Timber.tag("HardLink").e(e, "Failed to create hard link")
            // Fallback to regular copy
            copyFileFallback(sourcePath, destPath)
        }
    }
    
    /**
     * Fallback file copy when hard linking fails
     */
    private suspend fun copyFileFallback(
        sourcePath: String,
        destPath: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(sourcePath)
            val destFile = File(destPath)
            destFile.parentFile?.mkdirs()
            
            Files.copy(
                sourceFile.toPath(),
                destFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES
            )
            
            Timber.tag("HardLink").d("Copied file (fallback): $destPath")
            true
        } catch (e: Exception) {
            Timber.tag("HardLink").e(e, "Failed to copy file")
            false
        }
    }
    
    /**
     * Deduplicate file by checking if identical content already exists
     * Returns the path to the existing file or null if not found
     */
    suspend fun deduplicateFile(
        file: File,
        checksum: String,
        targetDir: File
    ): String? = withContext(Dispatchers.IO) {
        // Check if we've seen this checksum before
        val existingPath = deduplicationMap[checksum]
        
        if (existingPath != null && File(existingPath).exists()) {
            filesDeduped.incrementAndGet()
            savedBytes.addAndGet(file.length())
            Timber.tag("Dedup").d("Found duplicate: $checksum -> $existingPath")
            return@withContext existingPath
        }
        
        // Register this file for future deduplication
        val targetPath = File(targetDir, file.name).absolutePath
        deduplicationMap[checksum] = targetPath
        
        null
    }
    
    /**
     * Execute backup plan with hard link optimization
     */
    suspend fun executeBackupPlan(
        plan: BackupPlan,
        sourceDir: File,
        targetDir: File
    ): ExecutionResult = withContext(Dispatchers.IO) {
        // Reset stats
        hardLinksCreated.set(0)
        filesDeduped.set(0)
        savedBytes.set(0)
        
        when (plan) {
            is BackupPlan.Full -> executeFull(plan, sourceDir, targetDir)
            is BackupPlan.Incremental -> executeIncremental(plan, sourceDir, targetDir)
        }
    }
    
    /**
     * Execute full backup
     */
    private suspend fun executeFull(
        plan: BackupPlan.Full,
        sourceDir: File,
        targetDir: File
    ): ExecutionResult = coroutineScope {
        var bytesCopied = 0L
        val errors = mutableListOf<String>()
        
        plan.files.forEach { fileMetadata ->
            try {
                val sourceFile = File(sourceDir, fileMetadata.path)
                val targetFile = File(targetDir, fileMetadata.path)
                
                // Check for deduplication opportunity
                val dedupPath = deduplicateFile(sourceFile, fileMetadata.checksum, targetDir)
                
                if (dedupPath != null) {
                    // Create hard link to existing file
                    createHardLink(dedupPath, targetFile.absolutePath)
                } else {
                    // Copy new file
                    targetFile.parentFile?.mkdirs()
                    Files.copy(
                        sourceFile.toPath(),
                        targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES
                    )
                    bytesCopied += fileMetadata.size
                }
            } catch (e: Exception) {
                errors.add("Failed to backup ${fileMetadata.path}: ${e.message}")
                Timber.tag("BackupExec").e(e, "Failed to backup file: ${fileMetadata.path}")
            }
        }
        
        ExecutionResult(
            filesProcessed = plan.files.size,
            bytesCopied = bytesCopied,
            hardLinksCreated = hardLinksCreated.get(),
            filesDeduped = filesDeduped.get(),
            savedBytes = savedBytes.get(),
            errors = errors
        )
    }
    
    /**
     * Execute incremental backup with hard link optimization
     */
    private suspend fun executeIncremental(
        plan: BackupPlan.Incremental,
        sourceDir: File,
        targetDir: File
    ): ExecutionResult = coroutineScope {
        var bytesCopied = 0L
        val errors = mutableListOf<String>()
        
        // 1. Hard link unchanged files from base snapshot
        val baseSnapshotDir = catalog.getSnapshotDirectory(plan.baseSnapshot)
        
        plan.unchangedFiles.forEach { fileMetadata ->
            try {
                val baseFile = File(baseSnapshotDir, fileMetadata.path)
                val targetFile = File(targetDir, fileMetadata.path)
                
                if (baseFile.exists()) {
                    createHardLink(baseFile.absolutePath, targetFile.absolutePath)
                } else {
                    Timber.tag("BackupExec").w("Base file missing: ${fileMetadata.path}")
                    // Copy from source as fallback
                    val sourceFile = File(sourceDir, fileMetadata.path)
                    if (sourceFile.exists()) {
                        copyFileFallback(sourceFile.absolutePath, targetFile.absolutePath)
                    }
                }
            } catch (e: Exception) {
                errors.add("Failed to link ${fileMetadata.path}: ${e.message}")
                Timber.tag("BackupExec").e(e, "Failed to link file: ${fileMetadata.path}")
            }
        }
        
        // 2. Copy changed files
        plan.changedFiles.forEach { fileMetadata ->
            try {
                val sourceFile = File(sourceDir, fileMetadata.path)
                val targetFile = File(targetDir, fileMetadata.path)
                
                // Check for deduplication opportunity
                val dedupPath = deduplicateFile(sourceFile, fileMetadata.checksum, targetDir)
                
                if (dedupPath != null) {
                    createHardLink(dedupPath, targetFile.absolutePath)
                } else {
                    targetFile.parentFile?.mkdirs()
                    Files.copy(
                        sourceFile.toPath(),
                        targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES
                    )
                    bytesCopied += fileMetadata.size
                }
            } catch (e: Exception) {
                errors.add("Failed to backup ${fileMetadata.path}: ${e.message}")
                Timber.tag("BackupExec").e(e, "Failed to backup file: ${fileMetadata.path}")
            }
        }
        
        ExecutionResult(
            filesProcessed = plan.changedFiles.size + plan.unchangedFiles.size,
            bytesCopied = bytesCopied,
            hardLinksCreated = hardLinksCreated.get(),
            filesDeduped = filesDeduped.get(),
            savedBytes = savedBytes.get(),
            errors = errors
        )
    }
    
    /**
     * Get statistics for last operation
     */
    fun getStats(): BackupStats {
        return BackupStats(
            hardLinksCreated = hardLinksCreated.get(),
            filesDeduped = filesDeduped.get(),
            savedBytes = savedBytes.get()
        )
    }
}

sealed class BackupPlan {
    data class Full(val files: List<FileMetadata>) : BackupPlan()
    data class Incremental(
        val baseSnapshot: BackupId,
        val changedFiles: List<FileMetadata>,
        val unchangedFiles: List<FileMetadata>,
        val linkDest: String
    ) : BackupPlan()
}

/**
 * Snapshot of file metadata for incremental comparison
 */
data class FileSnapshot(
    val path: String,
    val size: Long,
    val mtime: Long,
    val checksum: String
)

/**
 * File metadata with change tracking
 */
data class FileMetadata(
    val path: String,
    val checksum: String,
    val size: Long,
    val mtime: Long = 0L,
    val unchanged: Boolean = false
)

data class ChunkMetadata(val id: String, val size: Long)

/**
 * Backup execution result
 */
data class ExecutionResult(
    val filesProcessed: Int,
    val bytesCopied: Long,
    val hardLinksCreated: Int,
    val filesDeduped: Int,
    val savedBytes: Long,
    val errors: List<String>
)

/**
 * Backup statistics
 */
data class BackupStats(
    val hardLinksCreated: Int,
    val filesDeduped: Int,
    val savedBytes: Long
)
