// api/plugin/CloudProviderPlugin.kt
package com.obsidianbackup.api.plugin

import kotlinx.coroutines.flow.Flow
import java.io.File
import com.obsidianbackup.model.SnapshotId

/**
 * Plugin that provides cloud storage integration
 * Examples: Custom S3, enterprise object stores, self-hosted WebDAV
 */
interface CloudProviderPlugin : ObsidianBackupPlugin {

    /**
     * Provider name (e.g., "Amazon S3", "MinIO", "Custom WebDAV")
     */
    val providerName: String

    /**
     * Test connection to cloud provider
     */
    suspend fun testConnection(): PluginResult<ConnectionStatus>

    /**
     * Upload a snapshot to cloud storage
     */
    suspend fun uploadSnapshot(
        snapshotId: SnapshotId,
        files: List<File>,
        metadata: SnapshotMetadata
    ): PluginResult<CloudUploadResult>

    /**
     * Download a snapshot from cloud storage
     */
    suspend fun downloadSnapshot(
        snapshotId: SnapshotId,
        destinationDir: File
    ): PluginResult<CloudDownloadResult>

    /**
     * List available snapshots in cloud storage
     */
    suspend fun listSnapshots(): PluginResult<List<CloudSnapshotInfo>>

    /**
     * Delete a snapshot from cloud storage
     */
    suspend fun deleteSnapshot(snapshotId: SnapshotId): PluginResult<Unit>

    /**
     * Get storage quota information
     */
    suspend fun getStorageInfo(): PluginResult<StorageInfo>

    /**
     * Observe upload/download progress
     */
    fun observeTransferProgress(): Flow<TransferProgress>

    /**
     * Configure cloud provider credentials and settings
     */
    suspend fun configure(config: CloudConfiguration): PluginResult<Unit>
}

/**
 * Connection test result
 */
data class ConnectionStatus(
    val isConnected: Boolean,
    val latencyMs: Long? = null,
    val message: String? = null
)

/**
 * Snapshot metadata supplied on upload (platform should create this from BackupCatalog)
 */
data class SnapshotMetadata(
    val snapshotId: SnapshotId,
    val sizeBytes: Long,
    val checksum: String,
    val files: Map<String, Long> = emptyMap(), // relative path -> size
    val metadataJson: String? = null
)

/**
 * Upload result
 */
data class CloudUploadResult(
    val snapshotId: SnapshotId,
    val uploadedBytes: Long,
    val duration: Long,
    val remoteUrl: String? = null
)

/**
 * Download result including verification status
 */
data class CloudDownloadResult(
    val snapshotId: SnapshotId,
    val downloadedBytes: Long,
    val duration: Long,
    val verificationStatus: VerificationStatus
)

/**
 * Cloud snapshot listing info
 */
data class CloudSnapshotInfo(
    val snapshotId: SnapshotId,
    val timestamp: Long,
    val sizeBytes: Long,
    val checksum: String,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Storage/quota info
 */
data class StorageInfo(
    val totalBytes: Long,
    val usedBytes: Long,
    val availableBytes: Long,
    val fileCount: Int
)

/**
 * Transfer progress events
 */
sealed class TransferProgress {
    data class Uploading(
        val bytesTransferred: Long,
        val totalBytes: Long,
        val currentFile: String,
        val transferRate: Long // bytes/sec
    ) : TransferProgress()

    data class Downloading(
        val bytesTransferred: Long,
        val totalBytes: Long,
        val currentFile: String,
        val transferRate: Long
    ) : TransferProgress()

    object Completed : TransferProgress()
    data class Failed(val error: PluginError) : TransferProgress()
}

/**
 * Cloud provider configuration
 */
data class CloudConfiguration(
    val endpoint: String,
    val credentials: Map<String, String>,
    val bucket: String? = null,
    val region: String? = null,
    val customOptions: Map<String, Any> = emptyMap()
)

/**
 * Simple verification status for downloaded snapshots
 */
enum class VerificationStatus {
    VERIFIED,
    PARTIAL,
    FAILED
}
