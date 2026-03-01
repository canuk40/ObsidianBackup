// cloud/CloudProvider.kt
package com.obsidianbackup.cloud

import kotlinx.coroutines.flow.Flow
import java.io.File
import com.obsidianbackup.model.SnapshotId
import com.obsidianbackup.model.VerificationResult

/**
 * Core abstraction for cloud storage providers
 */
interface CloudProvider {
    /** Provider identifier */
    val providerId: String

    /** Provider display name */
    val displayName: String

    /**
     * Test connection and authentication
     */
    suspend fun testConnection(): CloudResult<ConnectionInfo>

    /**
     * Upload a complete snapshot (multi-file)
     */
    suspend fun uploadSnapshot(
        snapshotId: SnapshotId,
        files: List<CloudFile>,
        metadata: CloudSnapshotMetadata
    ): CloudResult<CloudUploadSummary>

    /**
     * Download a complete snapshot
     */
    suspend fun downloadSnapshot(
        snapshotId: SnapshotId,
        destinationDir: File,
        verifyIntegrity: Boolean = true
    ): CloudResult<CloudDownloadSummary>

    /**
     * List all snapshots in cloud storage
     */
    suspend fun listSnapshots(
        filter: CloudSnapshotFilter = CloudSnapshotFilter()
    ): CloudResult<List<CloudSnapshotInfo>>

    /**
     * Delete a snapshot from cloud storage
     */
    suspend fun deleteSnapshot(snapshotId: SnapshotId): CloudResult<Unit>

    /**
     * Get storage quota and usage information
     */
    suspend fun getStorageQuota(): CloudResult<StorageQuota>

    /**
     * Observe transfer progress for uploads/downloads
     */
    fun observeProgress(): Flow<CloudTransferProgress>

    /**
     * Sync catalog metadata to cloud
     */
    suspend fun syncCatalog(catalog: CloudCatalog): CloudResult<Unit>

    /**
     * Retrieve catalog metadata from cloud
     */
    suspend fun retrieveCatalog(): CloudResult<CloudCatalog>

    /**
     * Upload a single file (for catalogs, etc.)
     */
    suspend fun uploadFile(
        localFile: File,
        remotePath: String,
        metadata: Map<String, String> = emptyMap()
    ): CloudResult<Unit>

    /**
     * Download a single file
     */
    suspend fun downloadFile(
        remotePath: String,
        localFile: File
    ): CloudResult<Unit>
}

/**
 * Cloud operation result
 */
sealed class CloudResult<out T> {
    data class Success<T>(val data: T) : CloudResult<T>()
    data class Error(val error: CloudError) : CloudResult<Nothing>()

    inline fun <R> map(transform: (T) -> R): CloudResult<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }
    }
}

data class CloudError(
    val code: ErrorCode,
    val message: String,
    val cause: Throwable? = null,
    val retryable: Boolean = false
) {
    enum class ErrorCode {
        AUTHENTICATION_FAILED,
        NETWORK_ERROR,
        QUOTA_EXCEEDED,
        FILE_NOT_FOUND,
        CHECKSUM_MISMATCH,
        TIMEOUT,
        PERMISSION_DENIED,
        UNKNOWN
    }
}

data class CloudFile(
    val localPath: File,
    val remotePath: String,
    val checksum: String,
    val sizeBytes: Long
)

data class CloudSnapshotMetadata(
    val snapshotId: SnapshotId,
    val timestamp: Long,
    val deviceId: String,
    val appCount: Int,
    val totalSizeBytes: Long,
    val compressionRatio: Float,
    val encrypted: Boolean,
    val merkleRootHash: String,
    val customMetadata: Map<String, String> = emptyMap()
)

data class CloudUploadSummary(
    val snapshotId: SnapshotId,
    val filesUploaded: Int,
    val bytesUploaded: Long,
    val duration: Long,
    val averageSpeed: Long, // bytes/sec
    val remoteUrls: Map<String, String> = emptyMap()
)

data class CloudDownloadSummary(
    val snapshotId: SnapshotId,
    val filesDownloaded: Int,
    val bytesDownloaded: Long,
    val duration: Long,
    val averageSpeed: Long,
    val verificationResult: VerificationResult
)

data class CloudSnapshotInfo(
    val snapshotId: SnapshotId,
    val timestamp: Long,
    val sizeBytes: Long,
    val fileCount: Int,
    val checksum: String,
    val metadata: CloudSnapshotMetadata
)

data class CloudSnapshotFilter(
    val afterTimestamp: Long? = null,
    val beforeTimestamp: Long? = null,
    val deviceId: String? = null,
    val maxResults: Int = 100
)

data class StorageQuota(
    val totalBytes: Long,
    val usedBytes: Long,
    val availableBytes: Long
)

data class ConnectionInfo(
    val isConnected: Boolean,
    val latencyMs: Long,
    val serverVersion: String? = null
)

sealed class CloudTransferProgress {
    data class Uploading(
        val snapshotId: SnapshotId,
        val currentFile: String,
        val filesCompleted: Int,
        val totalFiles: Int,
        val bytesTransferred: Long,
        val totalBytes: Long,
        val transferRate: Long
    ) : CloudTransferProgress()

    data class Downloading(
        val snapshotId: SnapshotId,
        val currentFile: String,
        val filesCompleted: Int,
        val totalFiles: Int,
        val bytesTransferred: Long,
        val totalBytes: Long,
        val transferRate: Long
    ) : CloudTransferProgress()

    data class Completed(val snapshotId: SnapshotId) : CloudTransferProgress()
    data class Failed(val snapshotId: SnapshotId, val error: CloudError) : CloudTransferProgress()
}

data class CloudCatalog(
    val version: Int,
    val snapshots: List<CloudSnapshotInfo>,
    val lastUpdated: Long,
    val signature: String? = null
)
