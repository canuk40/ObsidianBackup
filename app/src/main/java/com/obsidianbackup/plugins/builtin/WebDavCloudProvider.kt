// plugins/builtin/WebDavCloudProvider.kt
package com.obsidianbackup.plugins.builtin

import android.content.Context
import com.obsidianbackup.model.SnapshotId
import com.obsidianbackup.plugins.api.PluginApiVersion
import com.obsidianbackup.plugins.api.PluginCapability
import com.obsidianbackup.plugins.api.PluginMetadata
import com.obsidianbackup.plugins.interfaces.*
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class WebDavCloudProvider(
    private val context: Context
) : CloudProviderPlugin {

    override val metadata = PluginMetadata(
        packageName = "com.obsidianbackup.webdav",
        className = "com.obsidianbackup.plugins.builtin.WebDavCloudProvider",
        name = "WebDAV",
        description = "Store backups on WebDAV-compatible servers (Nextcloud, ownCloud, etc.)",
        version = "1.0.0",
        apiVersion = PluginApiVersion.V1_0,
        capabilities = setOf(
            PluginCapability.ClientSideEncryption,
            PluginCapability.BandwidthThrottling
        ),
        author = "ObsidianBackup Team",
        minSdkVersion = 24
    )

    private val cloudProvider = WebDavCloudProviderImpl(context)

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
        val localFile = File(context.cacheDir, "${snapshotId.value}.tar.zst")
        return cloudProvider.downloadFile("snapshots/${snapshotId.value}.tar.zst", localFile)
    }

    override suspend fun listSnapshots(): List<CloudSnapshot> {
        val files = cloudProvider.listFiles("snapshots/")
        return files.mapNotNull { file ->
            val filename = file.path.substringAfterLast("/")
            val snapshotId = filename.removeSuffix(".tar.zst")
            if (snapshotId.isNotBlank()) {
                CloudSnapshot(
                    snapshotId = SnapshotId(snapshotId),
                    size = file.size,
                    uploadedAt = file.lastModified,
                    checksum = file.checksum ?: "",
                    metadata = mapOf("webdav" to "true")
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

private class WebDavCloudProviderImpl(
    private val context: Context
) : CloudProvider {

    override val providerId: String = "webdav"
    override val displayName: String = "WebDAV"
    override val capabilities = CloudCapabilities(
        supportsEncryption = true,
        supportsCompression = true,
        maxFileSize = 5L * 1024 * 1024 * 1024, // 5GB default
        supportedRegions = emptyList(),
        bandwidthThrottling = true
    )

    private var sardine: Sardine? = null
    private var baseUrl: String = ""
    private val progressFlow = MutableStateFlow<TransferProgress?>(null)
    
    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    override suspend fun initialize(config: CloudConfig): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        kotlin.runCatching {
            baseUrl = config.endpoint ?: throw IllegalArgumentException("WebDAV endpoint URL is required")
            
            // Ensure baseUrl ends with /
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/"
            }

            val username = config.credentials["username"] 
                ?: throw IllegalArgumentException("Username is required")
            val password = config.credentials["password"] 
                ?: throw IllegalArgumentException("Password is required")

            sardine = OkHttpSardine(okHttpClient)
            
            Timber.d("[WebDAV] Initialized with endpoint: $baseUrl")
        }
    }

    override suspend fun testConnection(): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        kotlin.runCatching {
            val client = sardine ?: throw IllegalStateException("WebDAV client not initialized")
            
            // Test connection using OPTIONS request
            client.exists(baseUrl)
            
            // Try to create snapshots directory if it doesn't exist
            val snapshotsUrl = "${baseUrl}snapshots/"
            if (!client.exists(snapshotsUrl)) {
                client.createDirectory(snapshotsUrl)
                Timber.d("[WebDAV] Created snapshots directory")
            }
            
            Timber.d("[WebDAV] Connection test successful")
        }.onFailure { e ->
            Timber.e(e, "[WebDAV] Connection test failed")
        }
    }

    override suspend fun uploadFile(
        localFile: File,
        remotePath: String,
        metadata: Map<String, String>
    ): CloudResult = withContext(Dispatchers.IO) {
        try {
            val client = sardine ?: return@withContext CloudResult(
                success = false,
                error = "WebDAV client not initialized"
            )

            if (!localFile.exists()) {
                return@withContext CloudResult(success = false, error = "Local file not found")
            }

            val remoteUrl = baseUrl + remotePath.trimStart('/')
            val fileSize = localFile.length()
            
            Timber.d("[WebDAV] Uploading ${localFile.name} ($fileSize bytes) to $remoteUrl")

            // Create parent directories if needed
            val parentUrl = remoteUrl.substringBeforeLast("/")
            if (!client.exists(parentUrl)) {
                createDirectoryRecursive(client, parentUrl)
            }

            // Upload with progress tracking
            FileInputStream(localFile).use { inputStream ->
                var bytesTransferred = 0L
                val startTime = System.currentTimeMillis()
                
                // For large files, we could implement chunked upload
                // For now, use direct upload with Sardine
                client.put(remoteUrl, localFile, "application/octet-stream")
                
                bytesTransferred = fileSize
                
                // Emit final progress
                progressFlow.value = TransferProgress(
                    snapshotId = SnapshotId(localFile.nameWithoutExtension),
                    bytesTransferred = bytesTransferred,
                    totalBytes = fileSize,
                    speedBps = calculateSpeed(bytesTransferred, System.currentTimeMillis() - startTime)
                )
            }

            // Calculate checksum
            val checksum = calculateMd5(localFile)

            CloudResult(
                success = true,
                metadata = mapOf(
                    "size" to fileSize.toString(),
                    "checksum" to checksum,
                    "url" to remoteUrl
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "[WebDAV] Upload failed")
            CloudResult(success = false, error = e.message ?: "Upload failed")
        }
    }

    override suspend fun downloadFile(
        remotePath: String,
        localFile: File
    ): CloudResult = withContext(Dispatchers.IO) {
        try {
            val client = sardine ?: return@withContext CloudResult(
                success = false,
                error = "WebDAV client not initialized"
            )

            val remoteUrl = baseUrl + remotePath.trimStart('/')
            
            if (!client.exists(remoteUrl)) {
                return@withContext CloudResult(success = false, error = "Remote file not found")
            }

            Timber.d("[WebDAV] Downloading from $remoteUrl to ${localFile.absolutePath}")

            // Get file size
            val resources = client.list(remoteUrl, 0)
            val fileSize = resources.firstOrNull()?.contentLength ?: 0L

            // Create parent directory
            localFile.parentFile?.mkdirs()

            // Download with progress tracking
            client.get(remoteUrl).use { inputStream ->
                FileOutputStream(localFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesTransferred = 0L
                    val startTime = System.currentTimeMillis()
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        bytesTransferred += bytesRead

                        // Emit progress every 1MB
                        if (bytesTransferred % (1024 * 1024) == 0L || bytesTransferred == fileSize) {
                            progressFlow.value = TransferProgress(
                                snapshotId = SnapshotId(localFile.nameWithoutExtension),
                                bytesTransferred = bytesTransferred,
                                totalBytes = fileSize,
                                speedBps = calculateSpeed(bytesTransferred, System.currentTimeMillis() - startTime)
                            )
                        }
                    }
                }
            }

            CloudResult(success = true, metadata = mapOf("size" to fileSize.toString()))
        } catch (e: Exception) {
            Timber.e(e, "[WebDAV] Download failed")
            CloudResult(success = false, error = e.message ?: "Download failed")
        }
    }

    override suspend fun listFiles(prefix: String): List<CloudFile> = withContext(Dispatchers.IO) {
        try {
            val client = sardine ?: return@withContext emptyList()
            
            val url = baseUrl + prefix.trimStart('/')
            
            if (!client.exists(url)) {
                Timber.w("[WebDAV] Directory not found: $url")
                return@withContext emptyList()
            }

            val resources = client.list(url, 1) // Depth 1 - only immediate children
            
            resources.filter { !it.isDirectory }.map { resource ->
                CloudFile(
                    path = resource.path.removePrefix(baseUrl),
                    size = resource.contentLength ?: 0L,
                    lastModified = resource.modified?.time ?: 0L,
                    checksum = resource.etag,
                    metadata = mapOf(
                        "contentType" to (resource.contentType ?: "application/octet-stream")
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "[WebDAV] List files failed")
            emptyList()
        }
    }

    override suspend fun deleteFile(remotePath: String): CloudResult = withContext(Dispatchers.IO) {
        try {
            val client = sardine ?: return@withContext CloudResult(
                success = false,
                error = "WebDAV client not initialized"
            )

            val remoteUrl = baseUrl + remotePath.trimStart('/')
            
            if (!client.exists(remoteUrl)) {
                return@withContext CloudResult(success = false, error = "File not found")
            }

            client.delete(remoteUrl)
            Timber.d("[WebDAV] Deleted: $remoteUrl")

            CloudResult(success = true)
        } catch (e: Exception) {
            Timber.e(e, "[WebDAV] Delete failed")
            CloudResult(success = false, error = e.message ?: "Delete failed")
        }
    }

    override suspend fun getFileMetadata(remotePath: String): CloudFile? = withContext(Dispatchers.IO) {
        try {
            val client = sardine ?: return@withContext null
            
            val remoteUrl = baseUrl + remotePath.trimStart('/')
            
            if (!client.exists(remoteUrl)) {
                return@withContext null
            }

            val resources = client.list(remoteUrl, 0)
            val resource = resources.firstOrNull() ?: return@withContext null

            CloudFile(
                path = remotePath,
                size = resource.contentLength ?: 0L,
                lastModified = resource.modified?.time ?: 0L,
                checksum = resource.etag,
                metadata = mapOf(
                    "contentType" to (resource.contentType ?: "application/octet-stream")
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "[WebDAV] Get metadata failed")
            null
        }
    }

    override fun observeTransferProgress(): Flow<TransferProgress> {
        return progressFlow.asStateFlow() as Flow<TransferProgress>
    }

    fun cleanup() {
        sardine = null
        progressFlow.value = null
    }

    // Helper methods

    private fun createDirectoryRecursive(client: Sardine, url: String) {
        val parts = url.removePrefix(baseUrl).split("/").filter { it.isNotBlank() }
        var currentUrl = baseUrl
        
        for (part in parts) {
            currentUrl += "$part/"
            if (!client.exists(currentUrl)) {
                client.createDirectory(currentUrl)
                Timber.d("[WebDAV] Created directory: $currentUrl")
            }
        }
    }

    private fun calculateSpeed(bytes: Long, timeMs: Long): Long {
        if (timeMs == 0L) return 0L
        return (bytes * 1000) / timeMs // bytes per second
    }

    private fun calculateMd5(file: File): String {
        val md = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
