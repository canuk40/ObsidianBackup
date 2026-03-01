// cloud/WebDavCloudProvider.kt
package com.obsidianbackup.cloud

import android.content.Context
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.model.SnapshotId
import com.obsidianbackup.model.VerificationResult
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.StringReader
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

/**
 * WebDAV Cloud Provider Implementation
 * Supports WebDAV-compatible servers (Nextcloud, OwnCloud, Apache, etc.)
 */
class WebDavCloudProvider @Inject constructor(
    private val context: Context,
    private val logger: ObsidianLogger,
    private val config: WebDavConfig
) : CloudProvider {

    override val providerId: String = "webdav"
    override val displayName: String = "WebDAV"

    private val progressFlow = MutableSharedFlow<CloudTransferProgress>(replay = 1)
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    
    private val sardine: Sardine by lazy {
        OkHttpSardine(httpClient).apply {
            setCredentials(config.username, config.password)
        }
    }
    
    private val serverCapabilities: WebDavServerCapabilities by lazy {
        detectServerCapabilities()
    }

    override suspend fun testConnection(): CloudResult<ConnectionInfo> = withContext(Dispatchers.IO) {
        return@withContext try {
            val startTime = System.currentTimeMillis()
            
            // Test connection by attempting to list the root directory
            sardine.list(config.baseUrl, 0)
            
            val latency = System.currentTimeMillis() - startTime
            
            CloudResult.Success(
                ConnectionInfo(
                    isConnected = true,
                    latencyMs = latency,
                    serverVersion = detectServerVersion()
                )
            )
        } catch (e: Exception) {
            logger.e(TAG, "Connection test failed", e)
            CloudResult.Error(
                CloudError(
                    code = when {
                        e is IOException -> CloudError.ErrorCode.NETWORK_ERROR
                        e.message?.contains("401") == true || e.message?.contains("403") == true -> 
                            CloudError.ErrorCode.AUTHENTICATION_FAILED
                        else -> CloudError.ErrorCode.UNKNOWN
                    },
                    message = "Failed to connect to WebDAV server: ${e.message}",
                    cause = e,
                    retryable = e is IOException
                )
            )
        }
    }

    override suspend fun uploadSnapshot(
        snapshotId: SnapshotId,
        files: List<CloudFile>,
        metadata: CloudSnapshotMetadata
    ): CloudResult<CloudUploadSummary> = withContext(Dispatchers.IO) {
        return@withContext try {
            val startTime = System.currentTimeMillis()
            var totalBytesUploaded = 0L
            val remoteUrls = mutableMapOf<String, String>()
            
            // Create snapshot folder
            val snapshotPath = "${config.baseUrl}/snapshots/snapshot_${snapshotId.value}"
            ensureDirectoryExists(snapshotPath)
            
            // Upload metadata file first
            uploadMetadataFile(snapshotPath, metadata)
            
            // Upload each file with progress tracking
            files.forEachIndexed { index, cloudFile ->
                emitProgress(
                    CloudTransferProgress.Uploading(
                        snapshotId = snapshotId,
                        currentFile = cloudFile.remotePath,
                        filesCompleted = index,
                        totalFiles = files.size,
                        bytesTransferred = totalBytesUploaded,
                        totalBytes = files.sumOf { it.sizeBytes },
                        transferRate = calculateTransferRate(totalBytesUploaded, startTime)
                    )
                )
                
                val fileUrl = uploadFile(snapshotPath, cloudFile)
                remoteUrls[cloudFile.remotePath] = fileUrl
                totalBytesUploaded += cloudFile.sizeBytes
            }
            
            val duration = System.currentTimeMillis() - startTime
            val averageSpeed = if (duration > 0) (totalBytesUploaded * 1000 / duration) else 0L
            
            emitProgress(CloudTransferProgress.Completed(snapshotId))
            
            CloudResult.Success(
                CloudUploadSummary(
                    snapshotId = snapshotId,
                    filesUploaded = files.size,
                    bytesUploaded = totalBytesUploaded,
                    duration = duration,
                    averageSpeed = averageSpeed,
                    remoteUrls = remoteUrls
                )
            )
        } catch (e: Exception) {
            logger.e(TAG, "Upload snapshot failed", e)
            val error = mapExceptionToCloudError(e, "Upload failed")
            emitProgress(CloudTransferProgress.Failed(snapshotId, error))
            CloudResult.Error(error)
        }
    }

    override suspend fun downloadSnapshot(
        snapshotId: SnapshotId,
        destinationDir: File,
        verifyIntegrity: Boolean
    ): CloudResult<CloudDownloadSummary> = withContext(Dispatchers.IO) {
        return@withContext try {
            val startTime = System.currentTimeMillis()
            val snapshotPath = "${config.baseUrl}/snapshots/snapshot_${snapshotId.value}"
            
            // Check if snapshot exists
            if (!sardine.exists(snapshotPath)) {
                return@withContext CloudResult.Error(
                    CloudError(
                        CloudError.ErrorCode.FILE_NOT_FOUND,
                        "Snapshot not found: ${snapshotId.value}"
                    )
                )
            }
            
            // List all files in the snapshot
            val resources = sardine.list(snapshotPath, 1)
                .filter { !it.isDirectory && !it.name.endsWith("metadata.json") }
            
            var totalBytesDownloaded = 0L
            val totalBytes = resources.sumOf { it.contentLength ?: 0L }
            var filesDownloaded = 0
            
            // Download each file
            resources.forEachIndexed { index, resource ->
                emitProgress(
                    CloudTransferProgress.Downloading(
                        snapshotId = snapshotId,
                        currentFile = resource.name,
                        filesCompleted = index,
                        totalFiles = resources.size,
                        bytesTransferred = totalBytesDownloaded,
                        totalBytes = totalBytes,
                        transferRate = calculateTransferRate(totalBytesDownloaded, startTime)
                    )
                )
                
                val localFile = File(destinationDir, resource.name)
                downloadFileInternal(resource.href.toString(), localFile)
                
                totalBytesDownloaded += resource.contentLength ?: 0L
                filesDownloaded++
            }
            
            val duration = System.currentTimeMillis() - startTime
            val averageSpeed = if (duration > 0) (totalBytesDownloaded * 1000 / duration) else 0L
            
            // Verify integrity if requested
            val verificationResult = if (verifyIntegrity) {
                verifyDownloadedFiles(snapshotId, destinationDir, resources)
            } else {
                VerificationResult(
                    snapshotId = snapshotId,
                    filesChecked = filesDownloaded,
                    allValid = true,
                    corruptedFiles = emptyList()
                )
            }
            
            emitProgress(CloudTransferProgress.Completed(snapshotId))
            
            CloudResult.Success(
                CloudDownloadSummary(
                    snapshotId = snapshotId,
                    filesDownloaded = filesDownloaded,
                    bytesDownloaded = totalBytesDownloaded,
                    duration = duration,
                    averageSpeed = averageSpeed,
                    verificationResult = verificationResult
                )
            )
        } catch (e: Exception) {
            logger.e(TAG, "Download snapshot failed", e)
            val error = mapExceptionToCloudError(e, "Download failed")
            emitProgress(CloudTransferProgress.Failed(snapshotId, error))
            CloudResult.Error(error)
        }
    }

    override suspend fun listSnapshots(
        filter: CloudSnapshotFilter
    ): CloudResult<List<CloudSnapshotInfo>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshotsPath = "${config.baseUrl}/snapshots"
            
            // Ensure snapshots directory exists
            if (!sardine.exists(snapshotsPath)) {
                ensureDirectoryExists(snapshotsPath)
                return@withContext CloudResult.Success(emptyList())
            }
            
            // List all snapshot directories
            val resources = sardine.list(snapshotsPath, 1)
                .filter { it.isDirectory && it.name.startsWith("snapshot_") }
            
            val snapshots = resources.mapNotNull { resource ->
                try {
                    parseSnapshotInfo(resource)
                } catch (e: Exception) {
                    logger.e(TAG, "Failed to parse snapshot info: ${resource.name}", e)
                    null
                }
            }.filter { snapshot ->
                // Apply filters
                (filter.afterTimestamp == null || snapshot.timestamp >= filter.afterTimestamp) &&
                (filter.beforeTimestamp == null || snapshot.timestamp <= filter.beforeTimestamp) &&
                (filter.deviceId == null || snapshot.metadata.deviceId == filter.deviceId)
            }.take(filter.maxResults)
            
            CloudResult.Success(snapshots)
        } catch (e: Exception) {
            logger.e(TAG, "List snapshots failed", e)
            CloudResult.Error(mapExceptionToCloudError(e, "Failed to list snapshots"))
        }
    }

    override suspend fun deleteSnapshot(snapshotId: SnapshotId): CloudResult<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshotPath = "${config.baseUrl}/snapshots/snapshot_${snapshotId.value}"
            
            if (!sardine.exists(snapshotPath)) {
                return@withContext CloudResult.Error(
                    CloudError(
                        CloudError.ErrorCode.FILE_NOT_FOUND,
                        "Snapshot not found: ${snapshotId.value}"
                    )
                )
            }
            
            // Delete the entire snapshot directory
            sardine.delete(snapshotPath)
            
            CloudResult.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Delete snapshot failed", e)
            CloudResult.Error(mapExceptionToCloudError(e, "Delete failed", retryable = false))
        }
    }

    override suspend fun getStorageQuota(): CloudResult<StorageQuota> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Try to get quota information via PROPFIND
            val quotaInfo = getQuotaInformation()
            
            CloudResult.Success(quotaInfo)
        } catch (e: Exception) {
            logger.e(TAG, "Get storage quota failed", e)
            // Return unlimited quota if not supported
            CloudResult.Success(
                StorageQuota(
                    totalBytes = Long.MAX_VALUE,
                    usedBytes = 0L,
                    availableBytes = Long.MAX_VALUE
                )
            )
        }
    }

    override fun observeProgress(): Flow<CloudTransferProgress> {
        return progressFlow.asSharedFlow()
    }

    override suspend fun syncCatalog(catalog: CloudCatalog): CloudResult<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val catalogPath = "${config.baseUrl}/catalog.json"
            val catalogJson = serializeCatalog(catalog)
            
            val tempFile = File.createTempFile("catalog", ".json", context.cacheDir)
            try {
                tempFile.writeText(catalogJson)
                sardine.put(catalogPath, tempFile, "application/json")
                CloudResult.Success(Unit)
            } finally {
                tempFile.delete()
            }
        } catch (e: Exception) {
            logger.e(TAG, "Sync catalog failed", e)
            CloudResult.Error(mapExceptionToCloudError(e, "Failed to sync catalog"))
        }
    }

    override suspend fun retrieveCatalog(): CloudResult<CloudCatalog> = withContext(Dispatchers.IO) {
        return@withContext try {
            val catalogPath = "${config.baseUrl}/catalog.json"
            
            if (!sardine.exists(catalogPath)) {
                // Return empty catalog if not exists
                return@withContext CloudResult.Success(
                    CloudCatalog(
                        version = 1,
                        snapshots = emptyList(),
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
            
            val tempFile = File.createTempFile("catalog", ".json", context.cacheDir)
            try {
                sardine.get(catalogPath).use { inputStream ->
                    FileOutputStream(tempFile).buffered(8192).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                val catalogJson = tempFile.readText()
                val catalog = deserializeCatalog(catalogJson)
                
                CloudResult.Success(catalog)
            } finally {
                tempFile.delete()
            }
        } catch (e: Exception) {
            logger.e(TAG, "Retrieve catalog failed", e)
            CloudResult.Error(mapExceptionToCloudError(e, "Failed to retrieve catalog"))
        }
    }

    override suspend fun uploadFile(
        localFile: File,
        remotePath: String,
        metadata: Map<String, String>
    ): CloudResult<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val fullPath = "${config.baseUrl}/$remotePath"
            
            // Ensure parent directory exists
            val parentPath = fullPath.substringBeforeLast('/')
            ensureDirectoryExists(parentPath)
            
            // Upload file with chunking for large files
            if (localFile.length() > CHUNK_SIZE) {
                uploadFileChunked(localFile, fullPath)
            } else {
                sardine.put(fullPath, localFile, "application/octet-stream")
            }
            
            CloudResult.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Upload file failed: ${localFile.name}", e)
            CloudResult.Error(mapExceptionToCloudError(e, "Upload failed"))
        }
    }

    override suspend fun downloadFile(
        remotePath: String,
        localFile: File
    ): CloudResult<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val fullPath = "${config.baseUrl}/$remotePath"
            
            if (!sardine.exists(fullPath)) {
                return@withContext CloudResult.Error(
                    CloudError(
                        CloudError.ErrorCode.FILE_NOT_FOUND,
                        "File not found: $remotePath"
                    )
                )
            }
            
            downloadFileInternal(fullPath, localFile)
            
            CloudResult.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Download file failed: $remotePath", e)
            CloudResult.Error(mapExceptionToCloudError(e, "Download failed"))
        }
    }

    // Private helper methods

    private fun ensureDirectoryExists(path: String) {
        if (!sardine.exists(path)) {
            // Create parent directories recursively
            val parts = path.removePrefix(config.baseUrl).split("/").filter { it.isNotEmpty() }
            var currentPath = config.baseUrl
            
            for (part in parts) {
                currentPath += "/$part"
                if (!sardine.exists(currentPath)) {
                    sardine.createDirectory(currentPath)
                }
            }
        }
    }

    private fun uploadFile(snapshotPath: String, cloudFile: CloudFile): String {
        val fileName = cloudFile.remotePath.substringAfterLast('/')
        val fileUrl = "$snapshotPath/$fileName"
        
        if (cloudFile.localPath.length() > CHUNK_SIZE) {
            uploadFileChunked(cloudFile.localPath, fileUrl)
        } else {
            sardine.put(fileUrl, cloudFile.localPath, "application/octet-stream")
        }
        
        return fileUrl
    }

    private fun uploadFileChunked(localFile: File, remoteUrl: String) {
        val fileSize = localFile.length()
        val chunkSize = config.chunkSize
        
        // If file is small or server doesn't support chunked upload, use simple PUT
        if (fileSize <= chunkSize || !serverCapabilities.supportsChunkedUpload) {
            uploadFileWithProgress(localFile, remoteUrl)
            return
        }
        
        // Chunked upload strategy depends on server type
        when {
            serverCapabilities.isNextcloud || serverCapabilities.isOwnCloud -> {
                uploadFileNextcloudChunked(localFile, remoteUrl, chunkSize)
            }
            serverCapabilities.supportsHttpChunkedEncoding -> {
                uploadFileWithProgress(localFile, remoteUrl)
            }
            else -> {
                // Fallback to standard upload
                uploadFileWithProgress(localFile, remoteUrl)
            }
        }
    }
    
    private fun uploadFileNextcloudChunked(localFile: File, remoteUrl: String, chunkSize: Long) {
        // Nextcloud/OwnCloud chunked upload protocol
        val fileName = localFile.name
        val fileSize = localFile.length()
        val uploadId = System.currentTimeMillis().toString()
        val chunksPath = "${config.baseUrl}/uploads/${config.username}/$uploadId"
        
        try {
            // Create chunks directory
            ensureDirectoryExists(chunksPath)
            
            // Upload chunks
            FileInputStream(localFile).buffered(8192).use { inputStream ->
                var chunkIndex = 0
                var remainingBytes = fileSize
                val buffer = ByteArray(chunkSize.toInt())
                
                while (remainingBytes > 0) {
                    val bytesToRead = minOf(chunkSize, remainingBytes).toInt()
                    val bytesRead = inputStream.read(buffer, 0, bytesToRead)
                    
                    if (bytesRead <= 0) break
                    
                    val chunkPath = "$chunksPath/${String.format("%08d", chunkIndex)}"
                    val chunkFile = File.createTempFile("chunk_$chunkIndex", ".tmp", context.cacheDir)
                    
                    try {
                        chunkFile.writeBytes(buffer.copyOf(bytesRead))
                        sardine.put(chunkPath, chunkFile, "application/octet-stream")
                    } finally {
                        chunkFile.delete()
                    }
                    
                    remainingBytes -= bytesRead
                    chunkIndex++
                }
            }
            
            // Assemble chunks using MOVE
            val assembleUrl = "$chunksPath/.file"
            sardine.move(assembleUrl, remoteUrl)
            
        } catch (e: Exception) {
            logger.e(TAG, "Chunked upload failed, falling back to standard upload", e)
            // Cleanup chunks directory
            try {
                sardine.delete(chunksPath)
            } catch (cleanupError: Exception) {
                logger.e(TAG, "Failed to cleanup chunks", cleanupError)
            }
            // Fallback to standard upload
            uploadFileWithProgress(localFile, remoteUrl)
        }
    }
    
    private fun uploadFileWithProgress(localFile: File, remoteUrl: String) {
        val fileSize = localFile.length()
        val bytesUploaded = AtomicLong(0)
        
        val progressRequestBody = object : RequestBody() {
            override fun contentType() = "application/octet-stream".toMediaType()
            
            override fun contentLength() = fileSize
            
            override fun writeTo(sink: BufferedSink) {
                localFile.source().use { source ->
                    val buffer = okio.Buffer()
                    var bytesRead: Long
                    
                    while (source.read(buffer, PROGRESS_UPDATE_INTERVAL).also { bytesRead = it } != -1L) {
                        sink.write(buffer, bytesRead)
                        bytesUploaded.addAndGet(bytesRead)
                        
                        // Progress callback could be added here if needed
                        // For now, progress is tracked at the file level in uploadSnapshot
                    }
                }
            }
        }
        
        val request = Request.Builder()
            .url(remoteUrl)
            .put(progressRequestBody)
            .apply {
                val credentials = okhttp3.Credentials.basic(config.username, config.password)
                header("Authorization", credentials)
            }
            .build()
        
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Upload failed with code: ${response.code}")
            }
        }
    }

    private fun downloadFileInternal(remoteUrl: String, localFile: File) {
        sardine.get(remoteUrl).use { inputStream ->
            FileOutputStream(localFile).buffered(8192).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    private fun uploadMetadataFile(snapshotPath: String, metadata: CloudSnapshotMetadata) {
        val metadataJson = serializeMetadata(metadata)
        val tempFile = File.createTempFile("metadata", ".json", context.cacheDir)
        try {
            tempFile.writeText(metadataJson)
            sardine.put("$snapshotPath/metadata.json", tempFile, "application/json")
        } finally {
            tempFile.delete()
        }
    }

    private fun parseSnapshotInfo(resource: DavResource): CloudSnapshotInfo {
        val snapshotIdStr = resource.name.removePrefix("snapshot_")
        val snapshotId = SnapshotId(snapshotIdStr)
        
        // Try to fetch metadata
        val metadataPath = "${resource.href}/metadata.json"
        val metadata = try {
            if (sardine.exists(metadataPath)) {
                sardine.get(metadataPath).use { inputStream ->
                    val json = inputStream.readBytes().toString(Charsets.UTF_8)
                    deserializeMetadata(json, snapshotId)
                }
            } else {
                createDefaultMetadata(snapshotId, resource)
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to read metadata for ${resource.name}", e)
            createDefaultMetadata(snapshotId, resource)
        }
        
        // Get total size of all files in snapshot
        val files = try {
            sardine.list(resource.href.toString(), 1).filter { !it.isDirectory }
        } catch (e: Exception) {
            emptyList()
        }
        
        val totalSize = files.sumOf { it.contentLength ?: 0L }
        val fileCount = files.size
        
        return CloudSnapshotInfo(
            snapshotId = snapshotId,
            timestamp = resource.modified?.time ?: System.currentTimeMillis(),
            sizeBytes = totalSize,
            fileCount = fileCount,
            checksum = metadata.merkleRootHash,
            metadata = metadata
        )
    }

    private fun createDefaultMetadata(snapshotId: SnapshotId, resource: DavResource): CloudSnapshotMetadata {
        return CloudSnapshotMetadata(
            snapshotId = snapshotId,
            timestamp = resource.modified?.time ?: System.currentTimeMillis(),
            deviceId = "unknown",
            appCount = 0,
            totalSizeBytes = 0L,
            compressionRatio = 1.0f,
            encrypted = false,
            merkleRootHash = ""
        )
    }

    private fun detectServerVersion(): String? {
        return try {
            val request = Request.Builder()
                .url(config.baseUrl)
                .method("OPTIONS", null)
                .apply {
                    val credentials = okhttp3.Credentials.basic(config.username, config.password)
                    header("Authorization", credentials)
                }
                .build()
            
            httpClient.newCall(request).execute().use { response ->
                response.header("Server") ?: response.header("X-Powered-By") ?: "WebDAV Server"
            }
        } catch (e: Exception) {
            logger.d(TAG, "Failed to detect server version: ${e.message}")
            null
        }
    }
    
    private fun detectServerCapabilities(): WebDavServerCapabilities {
        return try {
            val request = Request.Builder()
                .url(config.baseUrl)
                .method("OPTIONS", null)
                .apply {
                    val credentials = okhttp3.Credentials.basic(config.username, config.password)
                    header("Authorization", credentials)
                }
                .build()
            
            httpClient.newCall(request).execute().use { response ->
                val server = response.header("Server")?.lowercase() ?: ""
                val davHeader = response.header("DAV") ?: ""
                val allowHeader = response.header("Allow")?.lowercase() ?: ""
                
                WebDavServerCapabilities(
                    supportsChunkedUpload = davHeader.contains("chunked-upload") || 
                                            server.contains("nextcloud") || 
                                            server.contains("owncloud"),
                    supportsHttpChunkedEncoding = allowHeader.contains("put"),
                    supportsQuota = davHeader.contains("quota"),
                    isNextcloud = server.contains("nextcloud"),
                    isOwnCloud = server.contains("owncloud"),
                    isApache = server.contains("apache"),
                    serverVersion = server
                )
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to detect server capabilities", e)
            // Return conservative defaults
            WebDavServerCapabilities(
                supportsChunkedUpload = false,
                supportsHttpChunkedEncoding = true,
                supportsQuota = false,
                isNextcloud = false,
                isOwnCloud = false,
                isApache = false,
                serverVersion = "Unknown"
            )
        }
    }

    private fun getQuotaInformation(): StorageQuota {
        return try {
            // Build PROPFIND request for quota information
            val propfindBody = """
                <?xml version="1.0" encoding="utf-8"?>
                <D:propfind xmlns:D="DAV:">
                    <D:prop>
                        <D:quota-available-bytes/>
                        <D:quota-used-bytes/>
                    </D:prop>
                </D:propfind>
            """.trimIndent()
            
            val request = Request.Builder()
                .url(config.baseUrl)
                .method("PROPFIND", RequestBody.create("application/xml".toMediaType(), propfindBody))
                .header("Depth", "0")
                .header("Content-Type", "application/xml; charset=utf-8")
                .apply {
                    // Add basic auth header
                    val credentials = okhttp3.Credentials.basic(config.username, config.password)
                    header("Authorization", credentials)
                }
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    parseQuotaResponse(responseBody)
                } else {
                    getDefaultQuota()
                }
            } else {
                logger.d(TAG, "PROPFIND quota request failed with code: ${response.code}")
                getDefaultQuota()
            }
        } catch (e: Exception) {
            logger.d(TAG, "Failed to get quota information, using defaults: ${e.message}")
            getDefaultQuota()
        }
    }
    
    private fun parseQuotaResponse(xmlResponse: String): StorageQuota {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(InputSource(StringReader(xmlResponse)))
            
            // Parse quota-available-bytes
            val availableNodes = document.getElementsByTagNameNS("DAV:", "quota-available-bytes")
            val availableBytes = if (availableNodes.length > 0) {
                val element = availableNodes.item(0) as Element
                element.textContent.trim().toLongOrNull() ?: Long.MAX_VALUE
            } else {
                Long.MAX_VALUE
            }
            
            // Parse quota-used-bytes
            val usedNodes = document.getElementsByTagNameNS("DAV:", "quota-used-bytes")
            val usedBytes = if (usedNodes.length > 0) {
                val element = usedNodes.item(0) as Element
                element.textContent.trim().toLongOrNull() ?: 0L
            } else {
                0L
            }
            
            // Calculate total bytes
            val totalBytes = if (availableBytes == Long.MAX_VALUE) {
                Long.MAX_VALUE
            } else {
                usedBytes + availableBytes
            }
            
            logger.d(TAG, "Parsed quota: total=$totalBytes, used=$usedBytes, available=$availableBytes")
            
            StorageQuota(
                totalBytes = totalBytes,
                usedBytes = usedBytes,
                availableBytes = availableBytes
            )
        } catch (e: Exception) {
            logger.e(TAG, "Failed to parse quota XML response", e)
            getDefaultQuota()
        }
    }
    
    private fun getDefaultQuota(): StorageQuota {
        // Return unlimited quota as fallback
        return StorageQuota(
            totalBytes = Long.MAX_VALUE,
            usedBytes = 0L,
            availableBytes = Long.MAX_VALUE
        )
    }

    private fun verifyDownloadedFiles(
        snapshotId: SnapshotId,
        directory: File,
        resources: List<DavResource>
    ): VerificationResult {
        val corruptedFiles = mutableListOf<String>()
        var filesChecked = 0
        
        resources.forEach { resource ->
            val localFile = File(directory, resource.name)
            if (localFile.exists()) {
                filesChecked++
                // Could add checksum verification here if metadata includes checksums
            } else {
                corruptedFiles.add(resource.name)
            }
        }
        
        return VerificationResult(
            snapshotId = snapshotId,
            filesChecked = filesChecked,
            allValid = corruptedFiles.isEmpty(),
            corruptedFiles = corruptedFiles
        )
    }

    private fun calculateTransferRate(bytesTransferred: Long, startTime: Long): Long {
        val duration = System.currentTimeMillis() - startTime
        return if (duration > 0) (bytesTransferred * 1000 / duration) else 0L
    }

    private fun mapExceptionToCloudError(
        e: Exception,
        defaultMessage: String,
        retryable: Boolean = true
    ): CloudError {
        return CloudError(
            code = when {
                e is IOException -> CloudError.ErrorCode.NETWORK_ERROR
                e.message?.contains("401") == true || e.message?.contains("403") == true -> 
                    CloudError.ErrorCode.AUTHENTICATION_FAILED
                e.message?.contains("404") == true -> 
                    CloudError.ErrorCode.FILE_NOT_FOUND
                e.message?.contains("507") == true || e.message?.contains("quota") == true -> 
                    CloudError.ErrorCode.QUOTA_EXCEEDED
                e.message?.contains("timeout") == true -> 
                    CloudError.ErrorCode.TIMEOUT
                else -> CloudError.ErrorCode.UNKNOWN
            },
            message = "$defaultMessage: ${e.message}",
            cause = e,
            retryable = retryable
        )
    }

    private suspend fun emitProgress(progress: CloudTransferProgress) {
        progressFlow.emit(progress)
    }

    private fun serializeMetadata(metadata: CloudSnapshotMetadata): String {
        // Simple JSON serialization - in production use kotlinx.serialization
        return """
            {
                "snapshotId": "${metadata.snapshotId.value}",
                "timestamp": ${metadata.timestamp},
                "deviceId": "${metadata.deviceId}",
                "appCount": ${metadata.appCount},
                "totalSizeBytes": ${metadata.totalSizeBytes},
                "compressionRatio": ${metadata.compressionRatio},
                "encrypted": ${metadata.encrypted},
                "merkleRootHash": "${metadata.merkleRootHash}"
            }
        """.trimIndent()
    }

    private fun deserializeMetadata(json: String, snapshotId: SnapshotId): CloudSnapshotMetadata {
        // Simple JSON deserialization - in production use kotlinx.serialization
        // This is a placeholder; real implementation would parse JSON properly
        return CloudSnapshotMetadata(
            snapshotId = snapshotId,
            timestamp = System.currentTimeMillis(),
            deviceId = "unknown",
            appCount = 0,
            totalSizeBytes = 0L,
            compressionRatio = 1.0f,
            encrypted = false,
            merkleRootHash = ""
        )
    }

    private fun serializeCatalog(catalog: CloudCatalog): String {
        // Simple JSON serialization - in production use kotlinx.serialization
        return """
            {
                "version": ${catalog.version},
                "lastUpdated": ${catalog.lastUpdated},
                "snapshots": []
            }
        """.trimIndent()
    }

    private fun deserializeCatalog(json: String): CloudCatalog {
        // Simple JSON deserialization - in production use kotlinx.serialization
        return CloudCatalog(
            version = 1,
            snapshots = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
    }

    companion object {
        private const val TAG = "WebDavCloudProvider"
        private const val CHUNK_SIZE = 10L * 1024 * 1024 // 10 MB chunks
        private const val PROGRESS_UPDATE_INTERVAL = 8192L // 8KB buffer for progress updates
    }
}

/**
 * WebDAV server capabilities detection
 */
private data class WebDavServerCapabilities(
    val supportsChunkedUpload: Boolean,
    val supportsHttpChunkedEncoding: Boolean,
    val supportsQuota: Boolean,
    val isNextcloud: Boolean,
    val isOwnCloud: Boolean,
    val isApache: Boolean,
    val serverVersion: String
)

/**
 * WebDAV Configuration
 */
data class WebDavConfig(
    val baseUrl: String,
    val username: String,
    val password: String,
    val useDigestAuth: Boolean = false,
    val chunkSize: Long = 10L * 1024 * 1024, // 10 MB default
    val enableProgressCallbacks: Boolean = true
)
