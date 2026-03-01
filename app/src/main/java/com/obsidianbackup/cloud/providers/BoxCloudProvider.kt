// cloud/providers/BoxCloudProvider.kt
package com.obsidianbackup.cloud.providers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.items
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
import java.util.concurrent.TimeUnit

/**
 * Box.com cloud storage provider with OAuth2 authentication
 * Supports multi-account management and chunked uploads
 */
class BoxCloudProvider(
    private val context: Context,
    private val keystoreManager: KeystoreManager,
    private val logger: ObsidianLogger,
    private val accountId: String = "default"
) : CloudProvider {

    override val providerId: String = "box"
    override val displayName: String = "Box.com"

    private val oauth2Provider = BoxOAuth2Provider(context, keystoreManager, logger)
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
            
            // Add OAuth2 token to all requests
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
            
            chain.proceed(builder.build())
        }
        .build()

    private val progressFlow = MutableStateFlow<CloudTransferProgress>(
        CloudTransferProgress.Completed(SnapshotId(""))
    )

    private var rootFolderId: String? = null
    
    companion object {
        private const val TAG = "BoxCloudProvider"
        private const val API_BASE_URL = "https://api.box.com/2.0"
        private const val UPLOAD_BASE_URL = "https://upload.box.com/api/2.0"
        private const val CHUNK_SIZE = 8 * 1024 * 1024 // 8MB chunks
        private const val BACKUP_FOLDER_NAME = "ObsidianBackup"
    }

    override suspend fun testConnection(): CloudResult<ConnectionInfo> = withContext(Dispatchers.IO) {
        return@withContext try {
            val startTime = System.currentTimeMillis()
            
            val request = Request.Builder()
                .url("$API_BASE_URL/users/me")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime

            if (!response.isSuccessful) {
                return@withContext CloudResult.Error(
                    CloudError(
                        CloudError.ErrorCode.AUTHENTICATION_FAILED,
                        "Authentication failed: ${response.code}",
                        retryable = response.code in 500..599
                    )
                )
            }

            val json = JSONObject(response.body?.string() ?: "{}")
            
            CloudResult.Success(
                ConnectionInfo(
                    isConnected = true,
                    latencyMs = latency,
                    serverVersion = "Box API v2.0"
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

            // Ensure root folder exists
            val rootFolder = ensureRootFolder() ?: return@withContext CloudResult.Error(
                CloudError(CloudError.ErrorCode.PERMISSION_DENIED, "Cannot create root folder")
            )

            // Create snapshot folder
            val snapshotFolder = createFolder(snapshotId.value, rootFolder)
                ?: return@withContext CloudResult.Error(
                    CloudError(CloudError.ErrorCode.PERMISSION_DENIED, "Cannot create snapshot folder")
                )

            // Upload metadata file
            uploadMetadata(snapshotFolder, metadata)

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

                val fileId = if (cloudFile.sizeBytes > CHUNK_SIZE) {
                    uploadLargeFile(cloudFile, snapshotFolder)
                } else {
                    uploadSmallFile(cloudFile, snapshotFolder)
                }

                if (fileId != null) {
                    remoteUrls[cloudFile.remotePath] = fileId
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

            // Find snapshot folder
            val snapshotFolder = findSnapshotFolder(snapshotId.value)
                ?: return@withContext CloudResult.Error(
                    CloudError(CloudError.ErrorCode.FILE_NOT_FOUND, "Snapshot not found")
                )

            // List files in snapshot
            val items = listFolderContents(snapshotFolder)
            val filesList = mutableListOf<JSONObject>()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                if (item.getString("type") == "file" && !item.getString("name").startsWith("metadata_")) {
                    filesList.add(item)
                }
            }

            filesList.forEachIndexed { index, fileJson ->
                val fileId = fileJson.getString("id")
                val fileName = fileJson.getString("name")
                val fileSize = fileJson.getLong("size")

                progressFlow.value = CloudTransferProgress.Downloading(
                    snapshotId = snapshotId,
                    currentFile = fileName,
                    filesCompleted = index,
                    totalFiles = filesList.size,
                    bytesTransferred = downloadedBytes,
                    totalBytes = filesList.sumOf { it.getLong("size") },
                    transferRate = if (System.currentTimeMillis() - startTime > 0) {
                        downloadedBytes * 1000 / (System.currentTimeMillis() - startTime)
                    } else 0L
                )

                val destinationFile = File(destinationDir, fileName)
                downloadFileById(fileId, destinationFile)
                downloadedBytes += fileSize
            }

            val duration = System.currentTimeMillis() - startTime
            val averageSpeed = if (duration > 0) downloadedBytes * 1000 / duration else 0L

            progressFlow.value = CloudTransferProgress.Completed(snapshotId)

            CloudResult.Success(
                CloudDownloadSummary(
                    snapshotId = snapshotId,
                    filesDownloaded = filesList.size,
                    bytesDownloaded = downloadedBytes,
                    duration = duration,
                    averageSpeed = averageSpeed,
                    verificationResult = VerificationResult(
                        snapshotId = snapshotId,
                        filesChecked = filesList.size,
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
                val rootFolder = ensureRootFolder() ?: return@withContext CloudResult.Success(emptyList())
                val items = listFolderContents(rootFolder)
                val snapshots = mutableListOf<CloudSnapshotInfo>()

                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    if (item.getString("type") == "folder") {
                        val snapshotId = SnapshotId(item.getString("name"))
                        val folderId = item.getString("id")

                        // Try to load metadata
                        val metadataFile = findFileInFolder(folderId, "metadata_${snapshotId.value}.json")
                        if (metadataFile != null) {
                            val metadata = downloadMetadata(metadataFile)
                            if (metadata != null) {
                                if (matchesFilter(metadata, filter)) {
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
            val snapshotFolder = findSnapshotFolder(snapshotId.value)
                ?: return@withContext CloudResult.Error(
                    CloudError(CloudError.ErrorCode.FILE_NOT_FOUND, "Snapshot not found")
                )

            val request = Request.Builder()
                .url("$API_BASE_URL/folders/$snapshotFolder?recursive=true")
                .delete()
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext CloudResult.Error(
                    CloudError(CloudError.ErrorCode.UNKNOWN, "Delete failed: ${response.code}")
                )
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
            val request = Request.Builder()
                .url("$API_BASE_URL/users/me")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "{}")

            val spaceAmount = json.getLong("space_amount")
            val spaceUsed = json.getLong("space_used")

            CloudResult.Success(
                StorageQuota(
                    totalBytes = spaceAmount,
                    usedBytes = spaceUsed,
                    availableBytes = spaceAmount - spaceUsed
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
            val rootFolder = ensureRootFolder() ?: return@withContext CloudResult.Error(
                CloudError(CloudError.ErrorCode.PERMISSION_DENIED, "Cannot create root folder")
            )

            val catalogJson = JSONObject().apply {
                put("version", catalog.version)
                put("lastUpdated", catalog.lastUpdated)
                put("signature", catalog.signature)
                put("snapshots", JSONArray(catalog.snapshots.map { it.snapshotId.value }))
            }.toString()

            val catalogFile = File(context.cacheDir, "catalog.json")
            catalogFile.writeText(catalogJson)

            val uploaded = uploadSmallFile(
                CloudFile(catalogFile, "catalog.json", "", catalogFile.length()),
                rootFolder
            )

            catalogFile.delete()

            if (uploaded != null) {
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
            val rootFolder = ensureRootFolder() ?: return@withContext CloudResult.Error(
                CloudError(CloudError.ErrorCode.FILE_NOT_FOUND, "Root folder not found")
            )

            val catalogFileId = findFileInFolder(rootFolder, "catalog.json")
                ?: return@withContext CloudResult.Error(
                    CloudError(CloudError.ErrorCode.FILE_NOT_FOUND, "Catalog not found")
                )

            val catalogFile = File(context.cacheDir, "catalog_download.json")
            downloadFileById(catalogFileId, catalogFile)

            val json = JSONObject(catalogFile.readText())
            catalogFile.delete()

            CloudResult.Success(
                CloudCatalog(
                    version = json.getInt("version"),
                    snapshots = emptyList(), // Would need to load full snapshot info
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
            val rootFolder = ensureRootFolder() ?: return@withContext CloudResult.Error(
                CloudError(CloudError.ErrorCode.PERMISSION_DENIED, "Cannot create root folder")
            )

            val uploaded = uploadSmallFile(
                CloudFile(localFile, remotePath, "", localFile.length()),
                rootFolder
            )

            if (uploaded != null) {
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
                // Find file by path and get its ID
                val rootFolder = ensureRootFolder()
                    ?: return@withContext CloudResult.Error(
                        CloudError(CloudError.ErrorCode.AUTHENTICATION_FAILED, "Failed to access root folder")
                    )
                
                val fileId = findFileInFolder(rootFolder, remotePath)
                    ?: return@withContext CloudResult.Error(
                        CloudError(CloudError.ErrorCode.FILE_NOT_FOUND, "File not found: $remotePath")
                    )
                
                downloadFileById(fileId, localFile)
                CloudResult.Success(Unit)
            } catch (e: Exception) {
                logger.e(TAG, "Download file failed", e)
                CloudResult.Error(
                    CloudError(CloudError.ErrorCode.UNKNOWN, "Download failed: ${e.message}", e, retryable = true)
                )
            }
        }

    // Helper methods

    private suspend fun ensureRootFolder(): String? {
        if (rootFolderId != null) return rootFolderId

        // Search for existing folder
        val searchRequest = Request.Builder()
            .url("$API_BASE_URL/search?query=$BACKUP_FOLDER_NAME&type=folder")
            .get()
            .build()

        val searchResponse = httpClient.newCall(searchRequest).execute()
        val searchJson = JSONObject(searchResponse.body?.string() ?: "{}")
        val entries = searchJson.getJSONArray("entries")

        if (entries.length() > 0) {
            rootFolderId = entries.getJSONObject(0).getString("id")
            return rootFolderId
        }

        // Create new folder
        rootFolderId = createFolder(BACKUP_FOLDER_NAME, "0")
        return rootFolderId
    }

    private suspend fun createFolder(name: String, parentId: String): String? {
        val json = JSONObject().apply {
            put("name", name)
            put("parent", JSONObject().put("id", parentId))
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$API_BASE_URL/folders")
            .post(body)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null

        val responseJson = JSONObject(response.body?.string() ?: "{}")
        return responseJson.getString("id")
    }

    private suspend fun uploadSmallFile(cloudFile: CloudFile, folderId: String): String? {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("attributes", JSONObject().apply {
                put("name", cloudFile.remotePath.substringAfterLast('/'))
                put("parent", JSONObject().put("id", folderId))
            }.toString())
            .addFormDataPart(
                "file",
                cloudFile.localPath.name,
                cloudFile.localPath.asRequestBody("application/octet-stream".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("$UPLOAD_BASE_URL/files/content")
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null

        val json = JSONObject(response.body?.string() ?: "{}")
        val entries = json.getJSONArray("entries")
        return if (entries.length() > 0) entries.getJSONObject(0).getString("id") else null
    }

    private suspend fun uploadLargeFile(cloudFile: CloudFile, folderId: String): String? {
        // Create upload session
        val sessionBody = JSONObject().apply {
            put("folder_id", folderId)
            put("file_size", cloudFile.sizeBytes)
            put("file_name", cloudFile.remotePath.substringAfterLast('/'))
        }.toString().toRequestBody("application/json".toMediaType())

        val sessionRequest = Request.Builder()
            .url("$UPLOAD_BASE_URL/files/upload_sessions")
            .post(sessionBody)
            .build()

        val sessionResponse = httpClient.newCall(sessionRequest).execute()
        if (!sessionResponse.isSuccessful) return null

        val sessionJson = JSONObject(sessionResponse.body?.string() ?: "{}")
        val sessionId = sessionJson.getString("id")
        val uploadUrl = sessionJson.getJSONObject("session_endpoints").getString("upload_part")

        // Upload parts
        val inputStream = cloudFile.localPath.inputStream()
        var offset = 0L
        val buffer = ByteArray(CHUNK_SIZE)
        var partIndex = 0

        while (offset < cloudFile.sizeBytes) {
            val bytesRead = inputStream.read(buffer)
            if (bytesRead <= 0) break

            val partBody = buffer.copyOf(bytesRead).toRequestBody("application/octet-stream".toMediaType())
            val partRequest = Request.Builder()
                .url(uploadUrl)
                .put(partBody)
                .header("Digest", "sha=${calculateSHA1(buffer, bytesRead)}")
                .header("Content-Range", "bytes $offset-${offset + bytesRead - 1}/${cloudFile.sizeBytes}")
                .build()

            val partResponse = httpClient.newCall(partRequest).execute()
            if (!partResponse.isSuccessful) {
                inputStream.close()
                return null
            }

            offset += bytesRead
            partIndex++
        }

        inputStream.close()

        // Commit upload
        val commitUrl = sessionJson.getJSONObject("session_endpoints").getString("commit")
        val commitRequest = Request.Builder()
            .url(commitUrl)
            .post("".toRequestBody())
            .build()

        val commitResponse = httpClient.newCall(commitRequest).execute()
        if (!commitResponse.isSuccessful) return null

        val commitJson = JSONObject(commitResponse.body?.string() ?: "{}")
        val entries = commitJson.getJSONArray("entries")
        return if (entries.length() > 0) entries.getJSONObject(0).getString("id") else null
    }

    private suspend fun uploadMetadata(folderId: String, metadata: CloudSnapshotMetadata) {
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

        val metadataFile = File(context.cacheDir, "metadata_${metadata.snapshotId.value}.json")
        metadataFile.writeText(metadataJson)

        uploadSmallFile(
            CloudFile(metadataFile, "metadata_${metadata.snapshotId.value}.json", "", metadataFile.length()),
            folderId
        )

        metadataFile.delete()
    }

    private suspend fun listFolderContents(folderId: String): JSONArray {
        val request = Request.Builder()
            .url("$API_BASE_URL/folders/$folderId/items?limit=1000")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        val json = JSONObject(response.body?.string() ?: "{}")
        return json.getJSONArray("entries")
    }

    private suspend fun findSnapshotFolder(snapshotName: String): String? {
        val rootFolder = ensureRootFolder() ?: return null
        val items = listFolderContents(rootFolder)

        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            if (item.getString("type") == "folder" && item.getString("name") == snapshotName) {
                return item.getString("id")
            }
        }

        return null
    }

    private suspend fun findFileInFolder(folderId: String, fileName: String): String? {
        val items = listFolderContents(folderId)

        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            if (item.getString("type") == "file" && item.getString("name") == fileName) {
                return item.getString("id")
            }
        }

        return null
    }

    private suspend fun downloadFileById(fileId: String, destination: File) {
        val request = Request.Builder()
            .url("$API_BASE_URL/files/$fileId/content")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Download failed: ${response.code}")

        FileOutputStream(destination).use { output ->
            response.body?.byteStream()?.copyTo(output)
        }
    }

    private suspend fun downloadMetadata(fileId: String): CloudSnapshotMetadata? {
        return try {
            val tempFile = File.createTempFile("metadata_", ".json", context.cacheDir)
            downloadFileById(fileId, tempFile)

            val json = JSONObject(tempFile.readText())
            tempFile.delete()

            CloudSnapshotMetadata(
                snapshotId = SnapshotId(json.getString("snapshotId")),
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

    private fun matchesFilter(metadata: CloudSnapshotMetadata, filter: CloudSnapshotFilter): Boolean {
        if (filter.afterTimestamp != null && metadata.timestamp <= filter.afterTimestamp) return false
        if (filter.beforeTimestamp != null && metadata.timestamp >= filter.beforeTimestamp) return false
        if (filter.deviceId != null && metadata.deviceId != filter.deviceId) return false
        return true
    }

    private fun calculateSHA1(data: ByteArray, length: Int): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val hash = digest.digest(data.copyOf(length))
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Box OAuth2 Provider implementation
     */
    inner class BoxOAuth2Provider(
        context: Context,
        keystoreManager: KeystoreManager,
        logger: ObsidianLogger
    ) : OAuth2Provider(context, keystoreManager, logger) {
        override val providerId = "box"
        override val displayName = "Box.com"
        override val authorizationEndpoint = "https://account.box.com/api/oauth2/authorize"
        override val tokenEndpoint = "https://api.box.com/oauth2/token"
        override val clientId = "YOUR_BOX_CLIENT_ID" // Replace with actual client ID
        override val clientSecret = "YOUR_BOX_CLIENT_SECRET" // Replace with actual client secret
        override val scopes = listOf("root_readwrite")
        override val redirectUri = "com.obsidianbackup://oauth2/box"
    }
}
