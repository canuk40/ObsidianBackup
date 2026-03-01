// plugins/interfaces/CloudProviderPlugin.kt
package com.obsidianbackup.plugins.interfaces

import com.obsidianbackup.model.*
import com.obsidianbackup.plugins.api.PluginMetadata
import kotlinx.coroutines.flow.Flow
import java.io.File

interface CloudProvider {
    val providerId: String
    val displayName: String
    val capabilities: CloudCapabilities

    suspend fun initialize(config: CloudConfig): kotlin.Result<Unit>
    suspend fun testConnection(): kotlin.Result<Unit>

    suspend fun uploadFile(
        localFile: File,
        remotePath: String,
        metadata: Map<String, String> = emptyMap()
    ): CloudResult

    suspend fun downloadFile(
        remotePath: String,
        localFile: File
    ): CloudResult

    suspend fun listFiles(prefix: String = ""): List<CloudFile>
    suspend fun deleteFile(remotePath: String): CloudResult

    suspend fun getFileMetadata(remotePath: String): CloudFile?

    fun observeTransferProgress(): Flow<TransferProgress>
}

data class CloudConfig(
    val providerId: String,
    val credentials: Map<String, String>,
    val endpoint: String? = null,
    val region: String? = null,
    val bucket: String? = null
)

data class CloudResult(
    val success: Boolean,
    val snapshotId: SnapshotId? = null,
    val error: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

data class CloudFile(
    val path: String,
    val size: Long,
    val lastModified: Long,
    val checksum: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

data class CloudCapabilities(
    val supportsEncryption: Boolean = false,
    val supportsCompression: Boolean = false,
    val maxFileSize: Long = Long.MAX_VALUE,
    val supportedRegions: List<String> = emptyList(),
    val bandwidthThrottling: Boolean = false
)

data class TransferProgress(
    val snapshotId: SnapshotId,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val speedBps: Long
)

interface CloudProviderPlugin {

    val metadata: PluginMetadata

    /**
     * Initialize provider with configuration
     */
    suspend fun initialize(config: CloudConfig): kotlin.Result<Unit>

    /**
     * Test connectivity and permissions
     */
    suspend fun testConnection(): CloudResult

    /**
     * Upload snapshot to cloud
     */
    suspend fun uploadSnapshot(snapshotId: SnapshotId, file: java.io.File): CloudResult

    /**
     * Download snapshot from cloud
     */
    suspend fun downloadSnapshot(snapshotId: SnapshotId): CloudResult

    /**
     * List available snapshots in cloud
     */
    suspend fun listSnapshots(): List<CloudSnapshot>

    /**
     * Delete snapshot from cloud
     */
    suspend fun deleteSnapshot(snapshotId: SnapshotId): CloudResult

    /**
     * Get provider capabilities
     */
    fun getCapabilities(): CloudCapabilities

    /**
     * Observe transfer progress
     */
    fun observeProgress(): Flow<TransferProgress>

    /**
     * Cleanup resources
     */
    suspend fun cleanup()
}

data class CloudSnapshot(
    val snapshotId: SnapshotId,
    val size: Long,
    val uploadedAt: Long,
    val checksum: String,
    val metadata: Map<String, String> = emptyMap()
)
