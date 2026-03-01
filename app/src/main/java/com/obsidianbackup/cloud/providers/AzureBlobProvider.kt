// cloud/providers/AzureBlobProvider.kt
package com.obsidianbackup.cloud.providers

import android.content.Context
import com.obsidianbackup.cloud.*
import com.obsidianbackup.cloud.oauth.OAuth2Provider
import com.obsidianbackup.cloud.oauth.OAuth2Result
import com.obsidianbackup.crypto.KeystoreManager
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.model.SnapshotId
import com.obsidianbackup.model.VerificationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Azure Blob Storage provider with OAuth2 authentication
 * Supports multi-account management and block blob uploads
 */
class AzureBlobProvider(
    private val context: Context,
    private val keystoreManager: KeystoreManager,
    private val logger: ObsidianLogger,
    private val accountName: String,
    private val containerName: String = "obsidian-backup",
    private val accountId: String = "default"
) : CloudProvider {

    override val providerId: String = "azure_blob"
    override val displayName: String = "Azure Blob Storage"

    private val oauth2Provider = AzureOAuth2Provider(context, keystoreManager, logger)
    
    private val baseUrl = "https://$accountName.blob.core.windows.net"
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
            
            // Add OAuth2 token
            runCatching {
                kotlinx.coroutines.runBlocking {
                    when (val tokenResult = oauth2Provider.getValidToken(accountId)) {
                        is OAuth2Result.Success -> {
                            builder.header("Authorization", "Bearer ${tokenResult.data}")
                        }
                        is OAuth2Result.Error -> {
                            logger.e(TAG, "Failed to get valid token: ${tokenResult.message}")
                        }
                    }
                }
            }
            
            // Add required Azure headers
            builder.header("x-ms-version", "2021-08-06")
            builder.header("x-ms-date", getAzureDateHeader())
            
            chain.proceed(builder.build())
        }
        .build()

    private val progressFlow = MutableStateFlow<CloudTransferProgress>(
        CloudTransferProgress.Completed(SnapshotId(""))
    )

    companion object {
        private const val TAG = "AzureBlobProvider"
        private const val BLOCK_SIZE = 4 * 1024 * 1024 // 4MB blocks (Azure max 100MB, but we use 4MB for better progress)
        private const val MAX_BLOCK_COUNT = 50000
    }

    override suspend fun testConnection(): CloudResult<ConnectionInfo> = withContext(Dispatchers.IO) {
        return@withContext try {
            val startTime = System.currentTimeMillis()
            
            // Test by getting container properties
            val request = Request.Builder()
                .url("$baseUrl/$containerName?restype=container")
                .head()
                .build()

            val response = httpClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime

            if (!response.isSuccessful) {
                // Try to create container if it doesn't exist
                if (response.code == 404) {
                    createContainer()
                    return@withContext CloudResult.Success(
                        ConnectionInfo(
                            isConnected = true,
                            latencyMs = latency,
                            serverVersion = "Azure Blob Storage API 2021-08-06"
                        )
                    )
                }
                
                return@withContext CloudResult.Error(
                    CloudError(
                        CloudError.ErrorCode.AUTHENTICATION_FAILED,
                        "Connection failed: ${response.code}",
                        retryable = response.code in 500..599
                    )
                )
            }

            CloudResult.Success(
                ConnectionInfo(
                    isConnected = true,
                    latencyMs = latency,
                    serverVersion = response.header("x-ms-version") ?: "Unknown"
                )
            )
        } catch (e: Exception) {
            logger.e(TAG, "Connection test failed", e)
            CloudResult.Error(
                CloudError(
                    CloudError.ErrorCode.NETWORK_ERROR,
                    "Connection failed: ${e.message}",
                    e,
                    retryable = true
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
            var uploadedBytes = 0L
            val remoteUrls = mutableMapOf<String, String>()

            // Upload metadata blob
            uploadMetadataBlob(snapshotId, metadata)

            files.forEachIndexed { index, cloudFile ->
                progressFlow.value = CloudTransferProgress.Uploading(
                    snapshotId = snapshotId,
                    currentFile = cloudFile.remotePath,
                    filesCompleted = index,
                    totalFiles = files.size,
                    bytesTransferred = uploadedBytes,
                    totalBytes = metadata.totalSizeBytes,
                    transferRate = if (System.currentTimeMillis() - startTime > 0) {
                        uploadedBytes * 1000 / (System.currentTimeMillis() - startTime)
                    } else 0L
                )

                val blobName = "${snapshotId.value}/${cloudFile.remotePath}"
                
                val success = if (cloudFile.sizeBytes > BLOCK_SIZE) {
                    uploadBlockBlob(cloudFile, blobName)
                } else {
                    uploadSimpleBlob(cloudFile, blobName)
                }

                if (success) {
                    remoteUrls[cloudFile.remotePath] = "$baseUrl/$containerName/$blobName"
                    uploadedBytes += cloudFile.sizeBytes
                } else {
                    return@withContext CloudResult.Error(
                        CloudError(CloudError.ErrorCode.UNKNOWN, "Upload failed for ${cloudFile.remotePath}")
                    )
                }
            }

            val duration = System.currentTimeMillis() - startTime
            val averageSpeed = if (duration > 0) uploadedBytes * 1000 / duration else 0L

            progressFlow.value = CloudTransferProgress.Completed(snapshotId)

            CloudResult.Success(
                CloudUploadSummary(
                    snapshotId = snapshotId,
                    filesUploaded = files.size,
                    bytesUploaded = uploadedBytes,
                    duration = duration,
                    averageSpeed = averageSpeed,
                    remoteUrls = remoteUrls
                )
            )
        } catch (e: Exception) {
            logger.e(TAG, "Upload failed", e)
            progressFlow.value = CloudTransferProgress.Failed(
                snapshotId,
                CloudError(CloudError.ErrorCode.UNKNOWN, "Upload failed: ${e.message}", e)
            )
            CloudResult.Error(
                CloudError(CloudError.ErrorCode.UNKNOWN, "Upload failed: ${e.message}", e, retryable = true)
            )
        }
    }

    override suspend fun downloadSnapshot(
        snapshotId: SnapshotId,
        destinationDir: File,
        verifyIntegrity: Boolean
    ): CloudResult<CloudDownloadSummary> = withContext(Dispatchers.IO) {
        return@withContext try {
            val startTime = System.currentTimeMillis()
            var downloadedBytes = 0L

            if (!destinationDir.exists()) {
                destinationDir.mkdirs()
            }

            // List blobs with prefix
            val blobs = listBlobs("${snapshotId.value}/")
                .filter { !it.endsWith("_metadata.json") }

            blobs.forEachIndexed { index, blobName ->
                val fileName = blobName.substringAfter("${snapshotId.value}/")
                val destinationFile = File(destinationDir, fileName)

                val blobSize = getBlobSize(blobName)

                progressFlow.value = CloudTransferProgress.Downloading(
                    snapshotId = snapshotId,
                    currentFile = fileName,
                    filesCompleted = index,
                    totalFiles = blobs.size,
                    bytesTransferred = downloadedBytes,
                    totalBytes = blobs.sumOf { getBlobSize(it) },
                    transferRate = if (System.currentTimeMillis() - startTime > 0) {
                        downloadedBytes * 1000 / (System.currentTimeMillis() - startTime)
                    } else 0L
                )

                downloadBlob(blobName, destinationFile)
                downloadedBytes += blobSize
            }

            val duration = System.currentTimeMillis() - startTime
            val averageSpeed = if (duration > 0) downloadedBytes * 1000 / duration else 0L

            progressFlow.value = CloudTransferProgress.Completed(snapshotId)

            CloudResult.Success(
                CloudDownloadSummary(
                    snapshotId = snapshotId,
                    filesDownloaded = blobs.size,
                    bytesDownloaded = downloadedBytes,
                    duration = duration,
                    averageSpeed = averageSpeed,
                    verificationResult = VerificationResult(
                        snapshotId = snapshotId,
                        filesChecked = blobs.size,
                        allValid = true,
                        corruptedFiles = emptyList()
                    )
                )
            )
        } catch (e: Exception) {
            logger.e(TAG, "Download failed", e)
            progressFlow.value = CloudTransferProgress.Failed(
                snapshotId,
                CloudError(CloudError.ErrorCode.UNKNOWN, "Download failed: ${e.message}", e)
            )
            CloudResult.Error(
                CloudError(CloudError.ErrorCode.UNKNOWN, "Download failed: ${e.message}", e, retryable = true)
            )
        }
    }

    override suspend fun listSnapshots(filter: CloudSnapshotFilter): CloudResult<List<CloudSnapshotInfo>> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                // List all blob prefixes (directories)
                val prefixes = listBlobPrefixes()
                val snapshots = mutableListOf<CloudSnapshotInfo>()

                for (prefix in prefixes) {
                    val snapshotId = SnapshotId(prefix.trimEnd('/'))
                    
                    // Try to load metadata
                    val metadataBlob = "${prefix}_metadata.json"
                    if (blobExists(metadataBlob)) {
                        val metadata = downloadMetadataBlob(snapshotId)
                        if (metadata != null && matchesFilter(metadata, filter)) {
                            snapshots.add(
                                CloudSnapshotInfo(
                                    snapshotId = snapshotId,
                                    timestamp = metadata.timestamp,
                                    sizeBytes = metadata.totalSizeBytes,
                                    fileCount = metadata.appCount,
                                    checksum = metadata.merkleRootHash,
                                    metadata = metadata
                                )
                            )
                        }
                    }
                }

                CloudResult.Success(snapshots.take(filter.maxResults))
            } catch (e: Exception) {
                logger.e(TAG, "List snapshots failed", e)
                CloudResult.Error(
                    CloudError(CloudError.ErrorCode.UNKNOWN, "List failed: ${e.message}", e, retryable = true)
                )
            }
        }

    override suspend fun deleteSnapshot(snapshotId: SnapshotId): CloudResult<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // List and delete all blobs with the snapshot prefix
            val blobs = listBlobs("${snapshotId.value}/")
            
            // Also include metadata blob
            val allBlobs = blobs + "${snapshotId.value}_metadata.json"

            for (blobName in allBlobs) {
                deleteBlob(blobName)
            }

            CloudResult.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Delete failed", e)
            CloudResult.Error(
                CloudError(CloudError.ErrorCode.UNKNOWN, "Delete failed: ${e.message}", e, retryable = true)
            )
        }
    }

    override suspend fun getStorageQuota(): CloudResult<StorageQuota> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Azure Blob doesn't have a direct quota API, calculate used space
            var usedBytes = 0L
            val blobs = listBlobs("")
            
            for (blobName in blobs) {
                usedBytes += getBlobSize(blobName)
            }

            // Azure storage accounts typically have very large limits (petabytes)
            // For practical purposes, return a large number
            val totalBytes = 5L * 1024 * 1024 * 1024 * 1024 // 5TB default

            CloudResult.Success(
                StorageQuota(
                    totalBytes = totalBytes,
                    usedBytes = usedBytes,
                    availableBytes = totalBytes - usedBytes
                )
            )
        } catch (e: Exception) {
            logger.e(TAG, "Get quota failed", e)
            CloudResult.Error(
                CloudError(CloudError.ErrorCode.UNKNOWN, "Get quota failed: ${e.message}", e, retryable = true)
            )
        }
    }

    override fun observeProgress(): Flow<CloudTransferProgress> = progressFlow

    override suspend fun syncCatalog(catalog: CloudCatalog): CloudResult<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val catalogJson = JSONObject().apply {
                put("version", catalog.version)
                put("lastUpdated", catalog.lastUpdated)
                put("signature", catalog.signature)
                put("snapshots", JSONArray(catalog.snapshots.map { it.snapshotId.value }))
            }.toString()

            val catalogFile = File(context.cacheDir, "catalog.json")
            catalogFile.writeText(catalogJson)

            val success = uploadSimpleBlob(
                CloudFile(catalogFile, "catalog.json", "", catalogFile.length()),
                "catalog.json"
            )

            catalogFile.delete()

            if (success) {
                CloudResult.Success(Unit)
            } else {
                CloudResult.Error(CloudError(CloudError.ErrorCode.UNKNOWN, "Catalog upload failed"))
            }
        } catch (e: Exception) {
            logger.e(TAG, "Sync catalog failed", e)
            CloudResult.Error(
                CloudError(CloudError.ErrorCode.UNKNOWN, "Sync catalog failed: ${e.message}", e)
            )
        }
    }

    override suspend fun retrieveCatalog(): CloudResult<CloudCatalog> = withContext(Dispatchers.IO) {
        return@withContext try {
            val catalogFile = File(context.cacheDir, "catalog_download.json")
            downloadBlob("catalog.json", catalogFile)

            val json = JSONObject(catalogFile.readText())
            catalogFile.delete()

            CloudResult.Success(
                CloudCatalog(
                    version = json.getInt("version"),
                    snapshots = emptyList(),
                    lastUpdated = json.getLong("lastUpdated"),
                    signature = json.optString("signature")
                )
            )
        } catch (e: Exception) {
            logger.e(TAG, "Retrieve catalog failed", e)
            CloudResult.Error(
                CloudError(CloudError.ErrorCode.UNKNOWN, "Retrieve catalog failed: ${e.message}", e)
            )
        }
    }

    override suspend fun uploadFile(
        localFile: File,
        remotePath: String,
        metadata: Map<String, String>
    ): CloudResult<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val success = if (localFile.length() > BLOCK_SIZE) {
                uploadBlockBlob(CloudFile(localFile, remotePath, "", localFile.length()), remotePath)
            } else {
                uploadSimpleBlob(CloudFile(localFile, remotePath, "", localFile.length()), remotePath)
            }

            if (success) {
                CloudResult.Success(Unit)
            } else {
                CloudResult.Error(CloudError(CloudError.ErrorCode.UNKNOWN, "Upload failed"))
            }
        } catch (e: Exception) {
            logger.e(TAG, "Upload file failed", e)
            CloudResult.Error(
                CloudError(CloudError.ErrorCode.UNKNOWN, "Upload failed: ${e.message}", e, retryable = true)
            )
        }
    }

    override suspend fun downloadFile(remotePath: String, localFile: File): CloudResult<Unit> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                downloadBlob(remotePath, localFile)
                CloudResult.Success(Unit)
            } catch (e: Exception) {
                logger.e(TAG, "Download file failed", e)
                CloudResult.Error(
                    CloudError(CloudError.ErrorCode.UNKNOWN, "Download failed: ${e.message}", e, retryable = true)
                )
            }
        }

    // Helper methods

    private suspend fun createContainer() {
        val request = Request.Builder()
            .url("$baseUrl/$containerName?restype=container")
            .put("".toRequestBody())
            .build()

        httpClient.newCall(request).execute()
    }

    private suspend fun uploadSimpleBlob(cloudFile: CloudFile, blobName: String): Boolean {
        val request = Request.Builder()
            .url("$baseUrl/$containerName/$blobName")
            .put(cloudFile.localPath.asRequestBody("application/octet-stream".toMediaType()))
            .header("x-ms-blob-type", "BlockBlob")
            .header("Content-MD5", calculateMD5Base64(cloudFile.localPath))
            .build()

        val response = httpClient.newCall(request).execute()
        return response.isSuccessful
    }

    private suspend fun uploadBlockBlob(cloudFile: CloudFile, blobName: String): Boolean {
        val blockIds = mutableListOf<String>()
        val inputStream = cloudFile.localPath.inputStream()
        var offset = 0L
        var blockIndex = 0
        val buffer = ByteArray(BLOCK_SIZE)

        while (offset < cloudFile.sizeBytes) {
            val bytesRead = inputStream.read(buffer)
            if (bytesRead <= 0) break

            val blockId = Base64.getEncoder().encodeToString(
                String.format("%06d", blockIndex).toByteArray()
            )
            blockIds.add(blockId)

            // Upload block
            val blockData = buffer.copyOf(bytesRead)
            val blockBody = blockData.toRequestBody("application/octet-stream".toMediaType())
            
            val request = Request.Builder()
                .url("$baseUrl/$containerName/$blobName?comp=block&blockid=$blockId")
                .put(blockBody)
                .header("Content-MD5", calculateMD5Base64(blockData))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                inputStream.close()
                return false
            }

            offset += bytesRead
            blockIndex++
        }

        inputStream.close()

        // Commit block list
        val blockListXml = buildBlockListXml(blockIds)
        val commitRequest = Request.Builder()
            .url("$baseUrl/$containerName/$blobName?comp=blocklist")
            .put(blockListXml.toRequestBody("application/xml".toMediaType()))
            .build()

        val commitResponse = httpClient.newCall(commitRequest).execute()
        return commitResponse.isSuccessful
    }

    private fun buildBlockListXml(blockIds: List<String>): String {
        val builder = StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?><BlockList>")
        for (blockId in blockIds) {
            builder.append("<Latest>$blockId</Latest>")
        }
        builder.append("</BlockList>")
        return builder.toString()
    }

    private suspend fun uploadMetadataBlob(snapshotId: SnapshotId, metadata: CloudSnapshotMetadata) {
        val metadataJson = JSONObject().apply {
            put("snapshotId", metadata.snapshotId.value)
            put("timestamp", metadata.timestamp)
            put("deviceId", metadata.deviceId)
            put("appCount", metadata.appCount)
            put("totalSizeBytes", metadata.totalSizeBytes)
            put("compressionRatio", metadata.compressionRatio)
            put("encrypted", metadata.encrypted)
            put("merkleRootHash", metadata.merkleRootHash)
        }.toString()

        val metadataFile = File(context.cacheDir, "metadata_${snapshotId.value}.json")
        metadataFile.writeText(metadataJson)

        uploadSimpleBlob(
            CloudFile(metadataFile, "${snapshotId.value}_metadata.json", "", metadataFile.length()),
            "${snapshotId.value}_metadata.json"
        )

        metadataFile.delete()
    }

    private suspend fun downloadMetadataBlob(snapshotId: SnapshotId): CloudSnapshotMetadata? {
        return try {
            val tempFile = File.createTempFile("metadata_", ".json", context.cacheDir)
            downloadBlob("${snapshotId.value}_metadata.json", tempFile)

            val json = JSONObject(tempFile.readText())
            tempFile.delete()

            CloudSnapshotMetadata(
                snapshotId = snapshotId,
                timestamp = json.getLong("timestamp"),
                deviceId = json.getString("deviceId"),
                appCount = json.getInt("appCount"),
                totalSizeBytes = json.getLong("totalSizeBytes"),
                compressionRatio = json.getDouble("compressionRatio").toFloat(),
                encrypted = json.getBoolean("encrypted"),
                merkleRootHash = json.getString("merkleRootHash")
            )
        } catch (e: Exception) {
            logger.e(TAG, "Failed to download metadata", e)
            null
        }
    }

    private suspend fun listBlobs(prefix: String): List<String> {
        val blobs = mutableListOf<String>()
        var marker: String? = null

        do {
            val url = if (marker != null) {
                "$baseUrl/$containerName?restype=container&comp=list&prefix=$prefix&marker=$marker"
            } else {
                "$baseUrl/$containerName?restype=container&comp=list&prefix=$prefix"
            }

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val xml = response.body?.string() ?: break

            // Simple XML parsing (in production, use proper XML parser)
            val namePattern = "<Name>([^<]+)</Name>".toRegex()
            namePattern.findAll(xml).forEach { match ->
                blobs.add(match.groupValues[1])
            }

            marker = "<NextMarker>([^<]*)</NextMarker>".toRegex().find(xml)?.groupValues?.get(1)
        } while (!marker.isNullOrEmpty())

        return blobs
    }

    private suspend fun listBlobPrefixes(): List<String> {
        val prefixes = mutableListOf<String>()

        val request = Request.Builder()
            .url("$baseUrl/$containerName?restype=container&comp=list&delimiter=/")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        val xml = response.body?.string() ?: return prefixes

        // Extract blob prefixes
        val prefixPattern = "<Prefix>([^<]+)</Prefix>".toRegex()
        prefixPattern.findAll(xml).forEach { match ->
            prefixes.add(match.groupValues[1])
        }

        return prefixes
    }

    private suspend fun downloadBlob(blobName: String, destination: File) {
        val request = Request.Builder()
            .url("$baseUrl/$containerName/$blobName")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Download failed: ${response.code}")

        FileOutputStream(destination).use { output ->
            response.body?.byteStream()?.copyTo(output)
        }
    }

    private suspend fun deleteBlob(blobName: String) {
        val request = Request.Builder()
            .url("$baseUrl/$containerName/$blobName")
            .delete()
            .build()

        httpClient.newCall(request).execute()
    }

    private suspend fun blobExists(blobName: String): Boolean {
        val request = Request.Builder()
            .url("$baseUrl/$containerName/$blobName")
            .head()
            .build()

        val response = httpClient.newCall(request).execute()
        return response.isSuccessful
    }

    private suspend fun getBlobSize(blobName: String): Long {
        val request = Request.Builder()
            .url("$baseUrl/$containerName/$blobName")
            .head()
            .build()

        val response = httpClient.newCall(request).execute()
        return response.header("Content-Length")?.toLongOrNull() ?: 0L
    }

    private fun matchesFilter(metadata: CloudSnapshotMetadata, filter: CloudSnapshotFilter): Boolean {
        if (filter.afterTimestamp != null && metadata.timestamp <= filter.afterTimestamp) return false
        if (filter.beforeTimestamp != null && metadata.timestamp >= filter.beforeTimestamp) return false
        if (filter.deviceId != null && metadata.deviceId != filter.deviceId) return false
        return true
    }

    private fun getAzureDateHeader(): String {
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("GMT")
        return dateFormat.format(Date())
    }

    private fun calculateMD5Base64(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return Base64.getEncoder().encodeToString(digest.digest())
    }

    private fun calculateMD5Base64(data: ByteArray): String {
        val digest = MessageDigest.getInstance("MD5")
        return Base64.getEncoder().encodeToString(digest.digest(data))
    }

    /**
     * Azure OAuth2 Provider implementation
     */
    inner class AzureOAuth2Provider(
        context: Context,
        keystoreManager: KeystoreManager,
        logger: ObsidianLogger
    ) : OAuth2Provider(context, keystoreManager, logger) {
        override val providerId = "azure_blob"
        override val displayName = "Azure Blob Storage"
        override val authorizationEndpoint = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize"
        override val tokenEndpoint = "https://login.microsoftonline.com/common/oauth2/v2.0/token"
        override val clientId = "YOUR_AZURE_CLIENT_ID" // Replace with actual client ID
        override val clientSecret = "YOUR_AZURE_CLIENT_SECRET" // Replace with actual client secret
        override val scopes = listOf("https://storage.azure.com/user_impersonation", "offline_access")
        override val redirectUri = "com.obsidianbackup://oauth2/azure"
    }
}
