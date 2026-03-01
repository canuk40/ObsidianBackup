// plugins/builtin/RcloneDropboxPlugin.kt
package com.obsidianbackup.plugins.builtin

import android.content.Context
import com.obsidianbackup.cloud.rclone.backends.RcloneDropboxProvider
import com.obsidianbackup.model.SnapshotId
import com.obsidianbackup.plugins.api.PluginApiVersion
import com.obsidianbackup.plugins.api.PluginCapability
import com.obsidianbackup.plugins.api.PluginMetadata
import com.obsidianbackup.plugins.interfaces.*
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Dropbox cloud provider plugin using rclone
 */
class RcloneDropboxPlugin(
    private val context: Context
) : CloudProviderPlugin {
    
    override val metadata = PluginMetadata(
        packageName = "com.obsidianbackup.rclone.dropbox",
        className = "com.obsidianbackup.plugins.builtin.RcloneDropboxPlugin",
        name = "Dropbox (rclone)",
        description = "Multi-cloud backup to Dropbox using rclone",
        version = "1.0.0",
        apiVersion = PluginApiVersion.V1_0,
        capabilities = setOf(
            PluginCapability.ClientSideEncryption,
            PluginCapability.BandwidthThrottling
        ),
        author = "ObsidianBackup Team",
        minSdkVersion = 24
    )
    
    private val provider = RcloneDropboxProvider(context)
    
    override suspend fun initialize(config: CloudConfig): kotlin.Result<Unit> {
        return provider.initialize(config)
    }
    
    override suspend fun testConnection(): CloudResult {
        return when (val result = provider.testConnection()) {
            else -> {
                result.fold(
                    onSuccess = { CloudResult(success = true) },
                    onFailure = { error -> CloudResult(success = false, error = error.message) }
                )
            }
        }
    }
    
    override suspend fun uploadSnapshot(snapshotId: SnapshotId, file: File): CloudResult {
        val remotePath = "snapshots/${snapshotId.value}.tar.zst"
        return provider.uploadFile(file, remotePath)
    }
    
    override suspend fun downloadSnapshot(snapshotId: SnapshotId): CloudResult {
        val remotePath = "snapshots/${snapshotId.value}.tar.zst"
        val localFile = File(context.cacheDir, "${snapshotId.value}.tar.zst")
        return provider.downloadFile(remotePath, localFile)
    }
    
    override suspend fun listSnapshots(): List<CloudSnapshot> {
        val files = provider.listFiles("snapshots/")
        return files.mapNotNull { file ->
            val filename = file.path.substringAfterLast("/")
            val snapshotId = filename.removeSuffix(".tar.zst")
            if (snapshotId.isNotBlank()) {
                CloudSnapshot(
                    snapshotId = SnapshotId(snapshotId),
                    size = file.size,
                    uploadedAt = file.lastModified,
                    checksum = file.checksum ?: "",
                    metadata = file.metadata
                )
            } else null
        }
    }
    
    override suspend fun deleteSnapshot(snapshotId: SnapshotId): CloudResult {
        val remotePath = "snapshots/${snapshotId.value}.tar.zst"
        return provider.deleteFile(remotePath)
    }
    
    override fun getCapabilities(): CloudCapabilities {
        return provider.capabilities
    }
    
    override fun observeProgress(): Flow<TransferProgress> {
        return provider.observeTransferProgress()
    }
    
    override suspend fun cleanup() {
        // Nothing to clean up
    }
}
