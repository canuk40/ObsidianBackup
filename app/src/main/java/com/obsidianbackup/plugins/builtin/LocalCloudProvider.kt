// plugins/builtin/LocalCloudProvider.kt
package com.obsidianbackup.plugins.builtin

import android.content.Context
import com.obsidianbackup.model.SnapshotId
import com.obsidianbackup.plugins.api.PluginApiVersion
import com.obsidianbackup.plugins.api.PluginCapability
import com.obsidianbackup.plugins.api.PluginMetadata
import com.obsidianbackup.plugins.interfaces.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.io.File

class LocalCloudProvider(
    private val context: Context
) : CloudProviderPlugin {

    override val metadata = PluginMetadata(
        packageName = "com.obsidianbackup.local",
        className = "com.obsidianbackup.plugins.builtin.LocalCloudProvider",
        name = "Local Storage",
        description = "Store backups on local device storage",
        version = "1.0.0",
        apiVersion = PluginApiVersion.V1_0,
        capabilities = setOf(
            PluginCapability.ClientSideEncryption,
            PluginCapability.BandwidthThrottling
        ),
        author = "ObsidianBackup Team",
        minSdkVersion = 24
    )

    private val cloudProvider = LocalCloudProviderImpl(context)

    override suspend fun initialize(config: CloudConfig): kotlin.Result<Unit> {
        return cloudProvider.initialize(config)
    }

    override suspend fun testConnection(): CloudResult {
        return cloudProvider.testConnection().fold(
            onSuccess = { CloudResult(success = true) },
            onFailure = { error -> CloudResult(success = false, error = error.message) }
        )
    }

    override suspend fun uploadSnapshot(snapshotId: SnapshotId, file: File): CloudResult {
        return cloudProvider.uploadFile(file, "snapshots/${snapshotId.value}.tar.zst")
    }

    override suspend fun downloadSnapshot(snapshotId: SnapshotId): CloudResult {
        return cloudProvider.downloadFile("snapshots/${snapshotId.value}.tar.zst", File(context.cacheDir, "${snapshotId.value}.tar.zst"))
    }

    override suspend fun listSnapshots(): List<CloudSnapshot> {
        val files = cloudProvider.listFiles("snapshots/")
        return files.mapNotNull { file ->
            // Extract snapshot ID from filename
            val filename = file.path.substringAfterLast("/")
            val snapshotId = filename.removeSuffix(".tar.zst")
            if (snapshotId.isNotBlank()) {
                CloudSnapshot(
                    snapshotId = SnapshotId(snapshotId),
                    size = file.size,
                    uploadedAt = file.lastModified,
                    checksum = file.checksum ?: "",
                    metadata = mapOf("local" to "true")
                )
            } else null
        }
    }

    override suspend fun deleteSnapshot(snapshotId: SnapshotId): CloudResult {
        return cloudProvider.deleteFile("snapshots/${snapshotId.value}.tar.zst")
    }

    override fun getCapabilities(): CloudCapabilities {
        return cloudProvider.capabilities
    }

    override fun observeProgress(): Flow<TransferProgress> {
        return cloudProvider.observeTransferProgress()
    }

    override suspend fun cleanup() {
        cloudProvider.cleanup()
    }
}

private class LocalCloudProviderImpl(
    private val context: Context
) : CloudProvider {

    override val providerId: String = "local"
    override val displayName: String = "Local Storage"
    override val capabilities = CloudCapabilities(
        supportsEncryption = true,
        supportsCompression = true,
        maxFileSize = Long.MAX_VALUE,
        supportedRegions = listOf("local"),
        bandwidthThrottling = false
    )

    private val localStorageDir = File(context.getExternalFilesDir("cloud_backup") ?: context.filesDir, "backups")

    init {
        localStorageDir.mkdirs()
    }

    override suspend fun initialize(config: CloudConfig): kotlin.Result<Unit> {
        // Local storage doesn't need special initialization
        return kotlin.Result.success(Unit)
    }

    override suspend fun testConnection(): kotlin.Result<Unit> {
        return if (localStorageDir.canWrite()) {
            kotlin.Result.success(Unit)
        } else {
            kotlin.Result.failure(Exception("Cannot write to local storage"))
        }
    }

    override suspend fun uploadFile(localFile: File, remotePath: String, metadata: Map<String, String>): CloudResult {
        return try {
            val remoteFile = File(localStorageDir, remotePath)
            remoteFile.parentFile?.mkdirs()

            localFile.copyTo(remoteFile, overwrite = true)

            CloudResult(
                success = true,
                metadata = mapOf(
                    "size" to remoteFile.length().toString(),
                    "path" to remoteFile.absolutePath
                )
            )
        } catch (e: Exception) {
            CloudResult(success = false, error = e.message)
        }
    }

    override suspend fun downloadFile(remotePath: String, localFile: File): CloudResult {
        return try {
            val remoteFile = File(localStorageDir, remotePath)

            if (!remoteFile.exists()) {
                return CloudResult(success = false, error = "File not found")
            }

            remoteFile.copyTo(localFile, overwrite = true)

            CloudResult(success = true)
        } catch (e: Exception) {
            CloudResult(success = false, error = e.message)
        }
    }

    override suspend fun listFiles(prefix: String): List<CloudFile> {
        val prefixDir = File(localStorageDir, prefix.trimEnd('/'))
        return prefixDir.listFiles()?.map { file ->
            CloudFile(
                path = file.relativeTo(localStorageDir).path,
                size = file.length(),
                lastModified = file.lastModified(),
                checksum = null // Could implement checksum calculation
            )
        } ?: emptyList()
    }

    override suspend fun deleteFile(remotePath: String): CloudResult {
        return try {
            val file = File(localStorageDir, remotePath)
            val deleted = file.delete()

            CloudResult(success = deleted, error = if (!deleted) "File not found" else null)
        } catch (e: Exception) {
            CloudResult(success = false, error = e.message)
        }
    }

    override suspend fun getFileMetadata(remotePath: String): CloudFile? {
        val file = File(localStorageDir, remotePath)
        return if (file.exists()) {
            CloudFile(
                path = remotePath,
                size = file.length(),
                lastModified = file.lastModified(),
                checksum = null
            )
        } else null
    }

    override fun observeTransferProgress(): Flow<TransferProgress> {
        return emptyFlow() // Local transfers are instantaneous
    }

    fun cleanup() {
        // No cleanup needed for local storage
    }
}
