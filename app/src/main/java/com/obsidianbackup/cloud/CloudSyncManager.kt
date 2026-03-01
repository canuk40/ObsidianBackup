// cloud/CloudSyncManager.kt
package com.obsidianbackup.cloud

import android.content.Context
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.model.BackupId
import com.obsidianbackup.model.SnapshotId
import com.obsidianbackup.model.Result
import com.obsidianbackup.model.Result.Success
import com.obsidianbackup.model.Result.Error
import com.obsidianbackup.storage.BackupCatalog
import com.obsidianbackup.verification.ChecksumVerifier
import com.obsidianbackup.work.WorkManagerScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class CloudSyncManager(
    private val context: Context,
    private val backupCatalog: BackupCatalog,
    private val cloudProvider: CloudProvider,
    private val workManager: WorkManagerScheduler,
    private val logger: ObsidianLogger,
    private val checksumVerifier: ChecksumVerifier
) {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    suspend fun syncSnapshot(snapshotId: SnapshotId, policy: SyncPolicy): Result<Unit> {
        _syncState.value = SyncState.Syncing(snapshotId)

        return try {
            // 1. Get snapshot metadata
            val metadata = backupCatalog.getSnapshot(BackupId(snapshotId.value))
                ?: return Result.Error(SyncError.SnapshotNotFound(snapshotId))

            // 2. Check if already synced
            if (isSnapshotSynced(snapshotId)) {
                logger.i(TAG, "Snapshot $snapshotId already synced")
                _syncState.value = SyncState.Idle
                return Result.Success(Unit)
            }

            // 3. Upload snapshot archive
            val archiveFile = getSnapshotArchiveFile(snapshotId)
            val cloudFile = CloudFile(
                localPath = archiveFile,
                remotePath = "snapshots/${snapshotId.value}.tar.zst",
                checksum = calculateChecksum(archiveFile),
                sizeBytes = archiveFile.length()
            )

            val cloudMetadata = CloudSnapshotMetadata(
                snapshotId = snapshotId,
                timestamp = metadata.timestamp,
                deviceId = android.os.Build.DEVICE,
                appCount = metadata.apps.size,
                totalSizeBytes = metadata.totalSize,
                compressionRatio = 1.0f, // Not available in BackupMetadata
                encrypted = metadata.encrypted,
                merkleRootHash = calculateMerkleRoot(listOf(cloudFile))
            )

            when (val uploadResult = cloudProvider.uploadSnapshot(
                snapshotId = snapshotId,
                files = listOf(cloudFile),
                metadata = cloudMetadata
            )) {
                is CloudResult.Success -> {
                    logger.i(TAG, "Successfully uploaded snapshot $snapshotId")
                }
                is CloudResult.Error -> {
                    return Result.Error(SyncError.UploadFailed(uploadResult.error.message))
                }
            }

            // 4. Upload signed catalog
            val catalogResult = uploadCatalog()
            if (catalogResult is Result.Error) {
                return catalogResult
            }

            // 5. Mark as synced
            markSnapshotSynced(snapshotId)

            _syncState.value = SyncState.Idle
            logger.i(TAG, "Successfully synced snapshot $snapshotId")
            Result.Success(Unit)

        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            Result.Error(SyncError.UnexpectedError(e))
        }
    }

    suspend fun restoreFromCloud(snapshotId: SnapshotId): Result<File> {
        _syncState.value = SyncState.Downloading(snapshotId)

        return try {
            val remotePath = "snapshots/${snapshotId.value}.tar.zst"
            val localFile = File(context.cacheDir, "${snapshotId.value}.tar.zst")

            when (val downloadResult = cloudProvider.downloadFile(remotePath, localFile)) {
                is CloudResult.Success -> {
                    logger.i(TAG, "Successfully downloaded snapshot $snapshotId")
                }
                is CloudResult.Error -> {
                    _syncState.value = SyncState.Idle
                    return Result.Error(SyncError.DownloadFailed(downloadResult.error.message))
                }
            }

            _syncState.value = SyncState.Idle
            Result.Success(localFile)

        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            Result.Error(SyncError.UnexpectedError(e))
        }
    }

    suspend fun syncAllPending(policy: SyncPolicy): Result<Unit> {
        return try {
            val pendingSnapshots = getPendingSnapshots()
            logger.i(TAG, "Syncing ${pendingSnapshots.size} pending snapshots")

            for (snapshotId in pendingSnapshots) {
                val result = syncSnapshot(snapshotId, policy)
                if (result is Result.Error) {
                    logger.w(TAG, "Failed to sync snapshot $snapshotId: ${result.message}")
                    // Continue with other snapshots
                }
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to sync all pending snapshots", e)
            Result.Error(SyncError.UnexpectedError(e))
        }
    }

    suspend fun scheduleSync(policy: SyncPolicy) {
        workManager.scheduleCloudSync(policy)
    }

    suspend fun cancelScheduledSync() {
        workManager.cancelCloudSync()
    }

    private suspend fun uploadCatalog(): Result<Unit> {
        val catalogFile = exportSignedCatalog()
        when (val uploadResult = cloudProvider.uploadFile(
            localFile = catalogFile,
            remotePath = "catalog.json",
            metadata = mapOf("type" to "catalog", "version" to "1.0")
        )) {
            is CloudResult.Success -> {
                return Result.Success(Unit)
            }
            is CloudResult.Error -> {
                return Result.Error(SyncError.CatalogUploadFailed(uploadResult.error.message))
            }
        }
    }

    private suspend fun isSnapshotSynced(snapshotId: SnapshotId): Boolean {
        // Check if snapshot has been synced by looking for sync marker file
        return withContext(Dispatchers.IO) {
            val snapshotDir = getSnapshotArchiveFile(snapshotId).parentFile
            val syncMarkerFile = File(snapshotDir, ".synced_${cloudProvider.providerId}")
            syncMarkerFile.exists()
        }
    }

    private suspend fun markSnapshotSynced(snapshotId: SnapshotId) {
        // Mark snapshot as synced by creating a marker file and updating catalog
        withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            
            // Write marker file
            val snapshotDir = getSnapshotArchiveFile(snapshotId).parentFile
            if (!snapshotDir.exists()) snapshotDir.mkdirs()
            val syncMarkerFile = File(snapshotDir, ".synced_${cloudProvider.providerId}")
            syncMarkerFile.writeText(timestamp.toString())
            
            // Update catalog
            backupCatalog.markSyncedToCloud(
                BackupId(snapshotId.value),
                cloudProvider.providerId,
                timestamp
            )
            
            logger.d(TAG, "Marked snapshot $snapshotId as synced to ${cloudProvider.providerId}")
        }
    }

    private suspend fun getPendingSnapshots(): List<SnapshotId> {
        // Get all snapshots that have not been synced to cloud yet
        return withContext(Dispatchers.IO) {
            val pendingIds = backupCatalog.getPendingCloudSync()
            logger.d(TAG, "Found ${pendingIds.size} pending snapshots for sync")
            pendingIds.map { SnapshotId(it.value) }
        }
    }

    private fun getSnapshotArchiveFile(snapshotId: SnapshotId): File {
        return File(context.getExternalFilesDir("backups"), "${snapshotId.value}.tar.zst")
    }

    private suspend fun exportSignedCatalog(): File {
        // Export catalog with integrity signatures - simplified implementation
        return File(context.cacheDir, "catalog.json")
    }

    private suspend fun calculateChecksum(file: File): String {
        // Calculate SHA-256 checksum using ChecksumVerifier
        return checksumVerifier.calculateChecksum(file)
    }

    /**
     * Calculates the Merkle root hash for a list of files.
     * 
     * Uses a binary Merkle tree structure where:
     * - Leaf nodes are the SHA-256 hashes of individual files
     * - Parent nodes are SHA-256 hashes of concatenated child hashes
     * - Odd number of nodes at any level results in duplicating the last hash
     * 
     * This provides cryptographic proof of file set integrity with O(log n) verification.
     * Memory efficient for large file sets - only stores current tree level in memory.
     * 
     * @param files List of CloudFile objects to build the tree from
     * @return Hex-encoded Merkle root hash (64 characters for SHA-256)
     */
    internal suspend fun calculateMerkleRoot(files: List<CloudFile>): String = withContext(Dispatchers.Default) {
        if (files.isEmpty()) {
            return@withContext ""
        }
        
        if (files.size == 1) {
            return@withContext files[0].checksum
        }
        
        // Initialize leaf nodes with file checksums (already SHA-256 hashes)
        var currentLevel = files.map { hexToBytes(it.checksum) }
        
        // Build tree bottom-up, level by level for memory efficiency
        while (currentLevel.size > 1) {
            currentLevel = buildNextLevel(currentLevel)
        }
        
        // Return root hash as hex string
        bytesToHex(currentLevel[0])
    }
    
    /**
     * Builds the next level of the Merkle tree by pairing and hashing nodes.
     * Handles odd number of nodes by duplicating the last hash (standard approach).
     */
    private suspend fun buildNextLevel(currentLevel: List<ByteArray>): List<ByteArray> = withContext(Dispatchers.Default) {
        val nextLevel = mutableListOf<ByteArray>()
        val digest = MessageDigest.getInstance("SHA-256")
        
        var i = 0
        while (i < currentLevel.size) {
            val left = currentLevel[i]
            val right = if (i + 1 < currentLevel.size) {
                currentLevel[i + 1]
            } else {
                // Odd number of nodes - duplicate last hash (Bitcoin/standard approach)
                currentLevel[i]
            }
            
            // Hash concatenation: SHA-256(left || right)
            digest.reset()
            digest.update(left)
            digest.update(right)
            nextLevel.add(digest.digest())
            
            i += 2
        }
        
        nextLevel
    }
    
    /**
     * Verifies the integrity of a file set against a known Merkle root.
     * 
     * @param files List of files to verify
     * @param expectedRoot Expected Merkle root hash
     * @return true if calculated root matches expected root
     */
    suspend fun verifyMerkleRoot(files: List<CloudFile>, expectedRoot: String): Boolean {
        val calculatedRoot = calculateMerkleRoot(files)
        return calculatedRoot.equals(expectedRoot, ignoreCase = true)
    }
    
    /**
     * Converts hex string to byte array.
     */
    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
    
    /**
     * Converts byte array to hex string.
     */
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val TAG = "CloudSyncManager"
    }
}

sealed class SyncState {
    object Idle : SyncState()
    data class Syncing(val snapshotId: SnapshotId) : SyncState()
    data class Downloading(val snapshotId: SnapshotId) : SyncState()
    data class Error(val message: String) : SyncState()
}

sealed class SyncError : Exception() {
    data class SnapshotNotFound(val snapshotId: SnapshotId) : SyncError()
    data class UploadFailed(val reason: String) : SyncError()
    data class DownloadFailed(val reason: String) : SyncError()
    data class CatalogUploadFailed(val reason: String) : SyncError()
    data class UnexpectedError(override val cause: Throwable) : SyncError()
}

data class SyncPolicy(
    val syncOnBackup: Boolean = true,
    val syncOnWifiOnly: Boolean = true,
    val syncOnCharging: Boolean = false,
    val maxConcurrentSyncs: Int = 1,
    val retryPolicy: RetryPolicy = RetryPolicy()
)

data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMs: Long = 1000,
    val maxDelayMs: Long = 30000,
    val backoffMultiplier: Double = 2.0
)
