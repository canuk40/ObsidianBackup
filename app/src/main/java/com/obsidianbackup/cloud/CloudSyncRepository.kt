// cloud/CloudSyncRepository.kt
package com.obsidianbackup.cloud

import android.content.Context
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.model.SnapshotId
import com.obsidianbackup.model.Result
import com.obsidianbackup.model.Result.Success
import com.obsidianbackup.model.Result.Error
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class CloudSyncRepository(
    private val context: Context,
    private val cloudProvider: CloudProvider,
    private val logger: ObsidianLogger
) {

    private val _syncQueue = MutableStateFlow<List<QueuedSync>>(emptyList())
    val syncQueue: Flow<List<QueuedSync>> = _syncQueue.asStateFlow()

    private val _failedUploads = MutableStateFlow<List<FailedUpload>>(emptyList())
    val failedUploads: Flow<List<FailedUpload>> = _failedUploads.asStateFlow()

    suspend fun enqueueUpload(snapshotId: SnapshotId, files: List<CloudFile>): Result<Unit> {
        return try {
            val queuedSync = QueuedSync(
                snapshotId = snapshotId,
                files = files,
                priority = SyncPriority.NORMAL,
                createdAt = System.currentTimeMillis()
            )

            val currentQueue = _syncQueue.value.toMutableList()
            currentQueue.add(queuedSync)
            _syncQueue.value = currentQueue

            logger.i(TAG, "Enqueued upload for snapshot $snapshotId with ${files.size} files")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to enqueue upload", e)
            Result.Error("Enqueue failed: ${e.message}")
        }
    }

    suspend fun processQueue(): Result<Unit> {
        val queue = _syncQueue.value
        if (queue.isEmpty()) return Result.Success(Unit)

        var processed = 0
        var failed = 0

        for (sync in queue) {
            when (val result = performUpload(sync)) {
                is Result.Success -> {
                    processed++
                    removeFromQueue(sync)
                }
                is Result.Error -> {
                    failed++
                    handleUploadFailure(sync, result.message)
                }
                is Result.Loading -> {
                    // Skip loading state - should not occur in synchronous processing
                }
            }
        }

        logger.i(TAG, "Processed $processed uploads, $failed failed")
        return if (failed == 0) Result.Success(Unit) else Result.Error("$failed uploads failed")
    }

    suspend fun retryFailedUploads(): Result<Unit> {
        val failed = _failedUploads.value
        if (failed.isEmpty()) return Result.Success(Unit)

        val retryQueue = failed.map { failedUpload ->
            QueuedSync(
                snapshotId = failedUpload.snapshotId,
                files = failedUpload.files,
                priority = SyncPriority.HIGH, // Retry with higher priority
                createdAt = System.currentTimeMillis()
            )
        }

        _syncQueue.value = _syncQueue.value + retryQueue
        _failedUploads.value = emptyList()

        logger.i(TAG, "Retried ${retryQueue.size} failed uploads")
        return Result.Success(Unit)
    }

    suspend fun getSyncStatus(snapshotId: SnapshotId): SyncStatus {
        val inQueue = _syncQueue.value.any { it.snapshotId == snapshotId }
        val failed = _failedUploads.value.any { it.snapshotId == snapshotId }

        return when {
            inQueue -> SyncStatus.QUEUED
            failed -> SyncStatus.FAILED
            else -> SyncStatus.COMPLETED
        }
    }

    suspend fun cancelSync(snapshotId: SnapshotId): Result<Unit> {
        val updatedQueue = _syncQueue.value.filter { it.snapshotId != snapshotId }
        _syncQueue.value = updatedQueue

        logger.i(TAG, "Cancelled sync for snapshot $snapshotId")
        return Result.Success(Unit)
    }

    suspend fun deduplicateFiles(files: List<CloudFile>): List<CloudFile> {
        // Check existing files by checksum and deduplicate based on content hash
        val existingChecksums = mutableSetOf<String>()
        
        try {
            when (val result = cloudProvider.listSnapshots()) {
                is CloudResult.Success -> {
                    // Build set of existing checksums from all cloud snapshots
                    result.data.forEach { snapshotInfo ->
                        existingChecksums.add(snapshotInfo.checksum)
                        // Note: In a full implementation, we'd track individual file checksums
                        // For now, we track snapshot-level checksums
                    }
                    logger.d(TAG, "Found ${existingChecksums.size} existing checksums for deduplication")
                }
                is CloudResult.Error -> {
                    logger.w(TAG, "Failed to list snapshots for deduplication: ${result.error.message}")
                }
            }
        } catch (e: Exception) {
            logger.e(TAG, "Deduplication check failed", e)
        }

        // Filter out files that already exist (by checksum)
        val deduplicated = files.filter { file ->
            !existingChecksums.contains(file.checksum)
        }
        
        val savedCount = files.size - deduplicated.size
        if (savedCount > 0) {
            logger.i(TAG, "Deduplication: skipped $savedCount files already in cloud")
        }
        
        return deduplicated
    }

    private suspend fun performUpload(sync: QueuedSync): Result<Unit> {
        return try {
            // Calculate compression ratio from actual vs compressed file sizes
            val compressionRatio = calculateCompressionRatio(sync.files)
            
            // Detect encryption by checking file extensions or patterns
            val encrypted = detectEncryption(sync.files)
            
            // Calculate merkle root hash for file integrity verification
            val merkleRootHash = calculateMerkleRoot(sync.files)
            
            val metadata = CloudSnapshotMetadata(
                snapshotId = sync.snapshotId,
                timestamp = System.currentTimeMillis(),
                deviceId = android.os.Build.DEVICE,
                appCount = sync.files.size,
                totalSizeBytes = sync.files.sumOf { it.sizeBytes },
                compressionRatio = compressionRatio,
                encrypted = encrypted,
                merkleRootHash = merkleRootHash
            )

            when (val result = cloudProvider.uploadSnapshot(sync.snapshotId, sync.files, metadata)) {
                is CloudResult.Success -> {
                    logger.i(TAG, "Successfully uploaded snapshot ${sync.snapshotId} (compression: ${compressionRatio}, encrypted: $encrypted)")
                    Result.Success(Unit)
                }
                is CloudResult.Error -> {
                    logger.e(TAG, "Upload failed for ${sync.snapshotId}: ${result.error.message}")
                    Result.Error(result.error.message)
                }
            }
        } catch (e: Exception) {
            logger.e(TAG, "Unexpected error during upload", e)
            Result.Error("Unexpected error: ${e.message}")
        }
    }
    
    /**
     * Calculate compression ratio based on file extensions and actual sizes
     * Estimates original size by looking at file patterns
     */
    private fun calculateCompressionRatio(files: List<CloudFile>): Float {
        var compressedSize = 0L
        var estimatedOriginalSize = 0L
        
        files.forEach { file ->
            val size = file.sizeBytes
            compressedSize += size
            
            // Estimate original size based on file type
            when {
                // Compressed archive formats - good compression
                file.remotePath.endsWith(".tar.zst") || 
                file.remotePath.endsWith(".tar.gz") ||
                file.remotePath.endsWith(".tar.bz2") -> {
                    estimatedOriginalSize += (size * 2.5).toLong() // ~60% compression
                }
                // APK files - already compressed
                file.remotePath.endsWith(".apk") -> {
                    estimatedOriginalSize += size // ~100% (no additional compression)
                }
                // Data files - moderate compression
                file.remotePath.endsWith(".db") ||
                file.remotePath.endsWith(".sqlite") -> {
                    estimatedOriginalSize += (size * 1.5).toLong() // ~66% compression
                }
                else -> {
                    estimatedOriginalSize += (size * 2.0).toLong() // ~50% compression default
                }
            }
        }
        
        return if (estimatedOriginalSize > 0) {
            compressedSize.toFloat() / estimatedOriginalSize.toFloat()
        } else {
            1.0f
        }
    }
    
    /**
     * Detect if files are encrypted based on patterns and extensions
     */
    private fun detectEncryption(files: List<CloudFile>): Boolean {
        return files.any { file ->
            file.remotePath.endsWith(".enc") ||
            file.remotePath.endsWith(".encrypted") ||
            file.remotePath.contains(".aes") ||
            file.remotePath.contains("_encrypted")
        }
    }
    
    /**
     * Calculate merkle root hash for file integrity verification
     * Uses a simple binary merkle tree approach
     */
    private fun calculateMerkleRoot(files: List<CloudFile>): String {
        if (files.isEmpty()) return ""
        if (files.size == 1) return files[0].checksum
        
        // Build merkle tree from checksums
        var currentLevel = files.map { it.checksum }
        
        while (currentLevel.size > 1) {
            val nextLevel = mutableListOf<String>()
            
            var i = 0
            while (i < currentLevel.size) {
                val left = currentLevel[i]
                val right = if (i + 1 < currentLevel.size) currentLevel[i + 1] else left
                
                // Hash the concatenation
                val combined = hashString(left + right)
                nextLevel.add(combined)
                
                i += 2
            }
            
            currentLevel = nextLevel
        }
        
        return currentLevel[0]
    }
    
    /**
     * Calculate SHA-256 hash of a string
     */
    private fun hashString(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun removeFromQueue(sync: QueuedSync) {
        val updatedQueue = _syncQueue.value.filter { it != sync }
        _syncQueue.value = updatedQueue
    }

    private fun handleUploadFailure(sync: QueuedSync, error: String) {
        // Track retry count - check if this snapshot already failed before
        val existingFailure = _failedUploads.value.find { it.snapshotId == sync.snapshotId }
        val retryCount = (existingFailure?.retryCount ?: 0) + 1
        
        val failedUpload = FailedUpload(
            snapshotId = sync.snapshotId,
            files = sync.files,
            error = error,
            failedAt = System.currentTimeMillis(),
            retryCount = retryCount
        )

        // Update or add to failed uploads list
        val currentFailed = _failedUploads.value.toMutableList()
        currentFailed.removeIf { it.snapshotId == sync.snapshotId }
        currentFailed.add(failedUpload)
        _failedUploads.value = currentFailed
        
        logger.w(TAG, "Upload failed for ${sync.snapshotId} (attempt #$retryCount): $error")

        // Remove from queue
        removeFromQueue(sync)
    }

    enum class SyncPriority {
        LOW, NORMAL, HIGH
    }

    enum class SyncStatus {
        QUEUED, UPLOADING, COMPLETED, FAILED
    }

    data class QueuedSync(
        val snapshotId: SnapshotId,
        val files: List<CloudFile>,
        val priority: SyncPriority,
        val createdAt: Long
    )

    data class FailedUpload(
        val snapshotId: SnapshotId,
        val files: List<CloudFile>,
        val error: String,
        val failedAt: Long,
        val retryCount: Int
    )

    companion object {
        private const val TAG = "CloudSyncRepository"
    }
}
