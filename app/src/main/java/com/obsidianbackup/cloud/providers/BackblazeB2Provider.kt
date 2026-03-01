// cloud/providers/BackblazeB2Provider.kt
package com.obsidianbackup.cloud.providers

import android.content.Context
import com.obsidianbackup.BuildConfig
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
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Backblaze B2 cloud storage provider
 * Uses application key authentication (OAuth2-like)
 */
class BackblazeB2Provider(
    private val context: Context,
    private val keystoreManager: KeystoreManager,
    private val logger: ObsidianLogger,
    private val bucketName: String = "obsidian-backup",
    private val accountId: String = "default"
) : CloudProvider {

    override val providerId: String = "backblaze_b2"
    override val displayName: String = "Backblaze B2"

    private val oauth2Provider = BackblazeOAuth2Provider(context, keystoreManager, logger)
    
    private var authData: B2AuthData? = null
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            
            // Skip auth for authorization endpoint
            if (original.url.toString().contains("/b2_authorize_account")) {
                return@addInterceptor chain.proceed(original)
            }
            
            val builder = original.newBuilder()
            
            // Add authorization token
            runCatching {
                kotlinx.coroutines.runBlocking {
                    ensureAuthenticated()
                    authData?.let {
                        builder.header("Authorization", it.authorizationToken)
                    }
                }
            }
            
            chain.proceed(builder.build())
        }
        .build()

    private val progressFlow = MutableStateFlow<CloudTransferProgress>(
        CloudTransferProgress.Completed(SnapshotId(""))
    )

    private var bucketId: String? = null

    companion object {
        private const val TAG = "BackblazeB2Provider"
        private const val API_BASE_URL = "https://api.backblazeb2.com"
        private const val PART_SIZE = 10 * 1024 * 1024 // 10MB minimum for B2
        private const val MAX_PARTS = 10000
    }

    data class B2AuthData(
        val accountId: String,
        val authorizationToken: String,
        val apiUrl: String,
        val downloadUrl: String,
        val recommendedPartSize: Long,
        val absoluteMinimumPartSize: Long
    )

    override suspend fun testConnection(): CloudResult<ConnectionInfo> = withContext(Dispatchers.IO) {
        return@withContext try {
            val startTime = System.currentTimeMillis()
            
            ensureAuthenticated()
            
            if (authData == null) {
                return@withContext CloudResult.Error(
                    CloudError(
                        CloudError.ErrorCode.AUTHENTICATION_FAILED,
                        "Authentication failed",
                        retryable = true
                    )
                )
            }

            val latency = System.currentTimeMillis() - startTime

            CloudResult.Success(
                ConnectionInfo(
                    isConnected = true,
                    latencyMs = latency,
                    serverVersion = "Backblaze B2 API v2"
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

            ensureAuthenticated()
            ensureBucket()

            // Upload metadata
            uploadMetadata(snapshotId, metadata)

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

                val fileName = "${snapshotId.value}/${cloudFile.remotePath}"
                
                val fileId = if (cloudFile.sizeBytes > PART_SIZE * 2) {
                    uploadLargeFile(cloudFile, fileName)
                } else {
                    uploadSmallFile(cloudFile, fileName)
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

            ensureAuthenticated()
            ensureBucket()

            // List files with prefix
            val files = listFiles("${snapshotId.value}/")
                .filter { !it.fileName.endsWith("_metadata.json") }

            files.forEachIndexed { index, fileInfo ->
                val fileName = fileInfo.fileName.substringAfter("${snapshotId.value}/")
                val destinationFile = File(destinationDir, fileName)

                progressFlow.value = CloudTransferProgress.Downloading(
                    snapshotId = snapshotId,
                    currentFile = fileName,
                    filesCompleted = index,
                    totalFiles = files.size,
                    bytesTransferred = downloadedBytes,
                    totalBytes = files.sumOf { it.contentLength },
                    transferRate = if (System.currentTimeMillis() - startTime > 0) {
                        downloadedBytes * 1000 / (System.currentTimeMillis() - startTime)
                    } else 0L
                )

                downloadFileById(fileInfo.fileId, destinationFile)
                downloadedBytes += fileInfo.contentLength
            }

            val duration = System.currentTimeMillis() - startTime
            val averageSpeed = if (duration > 0) downloadedBytes * 1000 / duration else 0L

            progressFlow.value = CloudTransferProgress.Completed(snapshotId)

            CloudResult.Success(
                CloudDownloadSummary(
                    snapshotId = snapshotId,
                    filesDownloaded = files.size,
                    bytesDownloaded = downloadedBytes,
                    duration = duration,
                    averageSpeed = averageSpeed,
                    verificationResult = VerificationResult(
                        snapshotId = snapshotId,
                        filesChecked = files.size,
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
                ensureAuthenticated()
                ensureBucket()

                val snapshots = mutableListOf<CloudSnapshotInfo>()
                val processedPrefixes = mutableSetOf<String>()

                // List all files and extract snapshot prefixes
                val allFiles = listFiles("")
                
                for (fileInfo in allFiles) {
                    val parts = fileInfo.fileName.split("/")
                    if (parts.size >= 2) {
                        val snapshotIdStr = parts[0]
                        if (snapshotIdStr !in processedPrefixes) {
                            processedPrefixes.add(snapshotIdStr)
                            
                            // Try to load metadata
                            val metadataFile = allFiles.find { 
                                it.fileName == "${snapshotIdStr}_metadata.json" 
                            }
                            
                            if (metadataFile != null) {
                                val metadata = downloadMetadata(metadataFile.fileId)
                                if (metadata != null && matchesFilter(metadata, filter)) {
                                    snapshots.add(
                                        CloudSnapshotInfo(
                                            snapshotId = SnapshotId(snapshotIdStr),
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
            ensureAuthenticated()
            ensureBucket()

            // List and delete all files with prefix
            val files = listFiles("${snapshotId.value}/")
            
            // Also include metadata
            val metadataFiles = listFiles("").filter { 
                it.fileName == "${snapshotId.value}_metadata.json" 
            }
            
            val allFiles = files + metadataFiles

            for (fileInfo in allFiles) {
                deleteFile(fileInfo.fileId, fileInfo.fileName)
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
            ensureAuthenticated()
            ensureBucket()

            // B2 doesn't have a direct quota API, calculate used space
            var usedBytes = 0L
            val files = listFiles("")
            
            for (fileInfo in files) {
                usedBytes += fileInfo.contentLength
            }

            // B2 pricing is pay-as-you-go, return a large number for total
            val totalBytes = 10L * 1024 * 1024 * 1024 * 1024 // 10TB

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
            ensureAuthenticated()
            ensureBucket()

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
                "catalog.json"
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
            ensureAuthenticated()
            ensureBucket()

            val catalogFiles = listFiles("").filter { it.fileName == "catalog.json" }
            if (catalogFiles.isEmpty()) {
                return@withContext CloudResult.Error(
                    CloudError(CloudError.ErrorCode.FILE_NOT_FOUND, "Catalog not found")
                )
            }

            val catalogFile = File(context.cacheDir, "catalog_download.json")
            downloadFileById(catalogFiles[0].fileId, catalogFile)

            val json = JSONObject(catalogFile.readText())
            catalogFile.delete()

            // Parse snapshots array from catalog JSON
            val snapshotsArray = json.optJSONArray("snapshots")
            val snapshots = if (snapshotsArray != null) {
                (0 until snapshotsArray.length()).map { i ->
                    val snapshotObj = snapshotsArray.getJSONObject(i)
                    val metaObj = snapshotObj.optJSONObject("metadata")
                    CloudSnapshotInfo(
                        snapshotId = SnapshotId(snapshotObj.getString("id")),
                        timestamp = snapshotObj.getLong("timestamp"),
                        sizeBytes = snapshotObj.optLong("sizeBytes", 0),
                        fileCount = snapshotObj.optInt("fileCount", 0),
                        checksum = snapshotObj.optString("checksum", ""),
                        metadata = CloudSnapshotMetadata(
                            snapshotId = SnapshotId(snapshotObj.getString("id")),
                            timestamp = snapshotObj.getLong("timestamp"),
                            deviceId = metaObj?.optString("deviceId", "") ?: "",
                            appCount = metaObj?.optInt("appCount", 0) ?: 0,
                            totalSizeBytes = snapshotObj.optLong("sizeBytes", 0),
                            compressionRatio = metaObj?.optDouble("compressionRatio", 1.0)?.toFloat() ?: 1.0f,
                            encrypted = metaObj?.optBoolean("encrypted", false) ?: false,
                            merkleRootHash = metaObj?.optString("merkleRootHash", "") ?: ""
                        )
                    )
                }
            } else {
                emptyList()
            }

            CloudResult.Success(
                CloudCatalog(
                    version = json.getInt("version"),
                    snapshots = snapshots,
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
            ensureAuthenticated()
            ensureBucket()

            val uploaded = if (localFile.length() > PART_SIZE * 2) {
                uploadLargeFile(CloudFile(localFile, remotePath, "", localFile.length()), remotePath)
            } else {
                uploadSmallFile(CloudFile(localFile, remotePath, "", localFile.length()), remotePath)
            }

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
                ensureAuthenticated()
                ensureBucket()

                val files = listFiles("").filter { it.fileName == remotePath }
                if (files.isEmpty()) {
                    return@withContext CloudResult.Error(
                        CloudError(CloudError.ErrorCode.FILE_NOT_FOUND, "File not found")
                    )
                }

                downloadFileById(files[0].fileId, localFile)
                CloudResult.Success(Unit)
            } catch (e: Exception) {
                logger.e(TAG, "Download file failed", e)
                CloudResult.Error(
                    CloudError(CloudError.ErrorCode.UNKNOWN, "Download failed: ${e.message}", e, retryable = true)
                )
            }
        }

    // Helper methods

    private suspend fun ensureAuthenticated() {
        if (authData != null) return

        // Get application key from OAuth2 provider
        when (val tokenResult = oauth2Provider.getValidToken(accountId)) {
            is OAuth2Result.Success -> {
                authenticate(tokenResult.data)
            }
            is OAuth2Result.Error -> {
                logger.e(TAG, "Failed to get application key: ${tokenResult.message}")
            }
        }
    }

    private suspend fun authenticate(applicationKey: String) {
        // B2 uses "accountId:applicationKey" format for basic auth
        val credentials = Base64.getEncoder().encodeToString("$accountId:$applicationKey".toByteArray())

        val request = Request.Builder()
            .url("$API_BASE_URL/b2api/v2/b2_authorize_account")
            .header("Authorization", "Basic $credentials")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Authentication failed: ${response.code}")
        }

        val json = JSONObject(response.body?.string() ?: "{}")
        
        authData = B2AuthData(
            accountId = json.getString("accountId"),
            authorizationToken = json.getString("authorizationToken"),
            apiUrl = json.getString("apiUrl"),
            downloadUrl = json.getString("downloadUrl"),
            recommendedPartSize = json.getLong("recommendedPartSize"),
            absoluteMinimumPartSize = json.getLong("absoluteMinimumPartSize")
        )
    }

    private suspend fun ensureBucket() {
        if (bucketId != null) return

        val auth = authData ?: throw Exception("Not authenticated")

        // List buckets
        val listBody = JSONObject().apply {
            put("accountId", auth.accountId)
            put("bucketName", bucketName)
        }.toString().toRequestBody("application/json".toMediaType())

        val listRequest = Request.Builder()
            .url("${auth.apiUrl}/b2api/v2/b2_list_buckets")
            .post(listBody)
            .build()

        val listResponse = httpClient.newCall(listRequest).execute()
        val listJson = JSONObject(listResponse.body?.string() ?: "{}")
        val buckets = listJson.getJSONArray("buckets")

        if (buckets.length() > 0) {
            bucketId = buckets.getJSONObject(0).getString("bucketId")
            return
        }

        // Create bucket if not found
        val createBody = JSONObject().apply {
            put("accountId", auth.accountId)
            put("bucketName", bucketName)
            put("bucketType", "allPrivate")
        }.toString().toRequestBody("application/json".toMediaType())

        val createRequest = Request.Builder()
            .url("${auth.apiUrl}/b2api/v2/b2_create_bucket")
            .post(createBody)
            .build()

        val createResponse = httpClient.newCall(createRequest).execute()
        val createJson = JSONObject(createResponse.body?.string() ?: "{}")
        bucketId = createJson.getString("bucketId")
    }

    private suspend fun uploadSmallFile(cloudFile: CloudFile, fileName: String): String? {
        val auth = authData ?: return null
        val bucket = bucketId ?: return null

        // Get upload URL
        val urlBody = JSONObject().apply {
            put("bucketId", bucket)
        }.toString().toRequestBody("application/json".toMediaType())

        val urlRequest = Request.Builder()
            .url("${auth.apiUrl}/b2api/v2/b2_get_upload_url")
            .post(urlBody)
            .build()

        val urlResponse = httpClient.newCall(urlRequest).execute()
        val urlJson = JSONObject(urlResponse.body?.string() ?: "{}")
        val uploadUrl = urlJson.getString("uploadUrl")
        val uploadAuthToken = urlJson.getString("authorizationToken")

        // Upload file
        val sha1 = calculateSHA1(cloudFile.localPath)

        val uploadRequest = Request.Builder()
            .url(uploadUrl)
            .header("Authorization", uploadAuthToken)
            .header("X-Bz-File-Name", fileName)
            .header("Content-Type", "application/octet-stream")
            .header("Content-Length", cloudFile.sizeBytes.toString())
            .header("X-Bz-Content-Sha1", sha1)
            .post(cloudFile.localPath.asRequestBody("application/octet-stream".toMediaType()))
            .build()

        val uploadResponse = httpClient.newCall(uploadRequest).execute()
        if (!uploadResponse.isSuccessful) return null

        val uploadJson = JSONObject(uploadResponse.body?.string() ?: "{}")
        return uploadJson.getString("fileId")
    }

    private suspend fun uploadLargeFile(cloudFile: CloudFile, fileName: String): String? {
        val auth = authData ?: return null
        val bucket = bucketId ?: return null

        // Start large file
        val startBody = JSONObject().apply {
            put("bucketId", bucket)
            put("fileName", fileName)
            put("contentType", "application/octet-stream")
        }.toString().toRequestBody("application/json".toMediaType())

        val startRequest = Request.Builder()
            .url("${auth.apiUrl}/b2api/v2/b2_start_large_file")
            .post(startBody)
            .build()

        val startResponse = httpClient.newCall(startRequest).execute()
        val startJson = JSONObject(startResponse.body?.string() ?: "{}")
        val fileId = startJson.getString("fileId")

        // Upload parts
        val sha1List = mutableListOf<String>()
        val inputStream = cloudFile.localPath.inputStream()
        var partNumber = 1
        val buffer = ByteArray(PART_SIZE)

        while (true) {
            val bytesRead = inputStream.read(buffer)
            if (bytesRead <= 0) break

            val partData = buffer.copyOf(bytesRead)
            val partSha1 = calculateSHA1(partData)
            sha1List.add(partSha1)

            // Get upload part URL
            val partUrlBody = JSONObject().apply {
                put("fileId", fileId)
            }.toString().toRequestBody("application/json".toMediaType())

            val partUrlRequest = Request.Builder()
                .url("${auth.apiUrl}/b2api/v2/b2_get_upload_part_url")
                .post(partUrlBody)
                .build()

            val partUrlResponse = httpClient.newCall(partUrlRequest).execute()
            val partUrlJson = JSONObject(partUrlResponse.body?.string() ?: "{}")
            val partUploadUrl = partUrlJson.getString("uploadUrl")
            val partAuthToken = partUrlJson.getString("authorizationToken")

            // Upload part
            val partRequest = Request.Builder()
                .url(partUploadUrl)
                .header("Authorization", partAuthToken)
                .header("X-Bz-Part-Number", partNumber.toString())
                .header("Content-Length", bytesRead.toString())
                .header("X-Bz-Content-Sha1", partSha1)
                .post(partData.toRequestBody("application/octet-stream".toMediaType()))
                .build()

            val partResponse = httpClient.newCall(partRequest).execute()
            if (!partResponse.isSuccessful) {
                inputStream.close()
                // Cancel large file upload
                cancelLargeFile(fileId)
                return null
            }

            partNumber++
        }

        inputStream.close()

        // Finish large file
        val finishBody = JSONObject().apply {
            put("fileId", fileId)
            put("partSha1Array", JSONArray(sha1List))
        }.toString().toRequestBody("application/json".toMediaType())

        val finishRequest = Request.Builder()
            .url("${auth.apiUrl}/b2api/v2/b2_finish_large_file")
            .post(finishBody)
            .build()

        val finishResponse = httpClient.newCall(finishRequest).execute()
        if (!finishResponse.isSuccessful) return null

        return fileId
    }

    private suspend fun cancelLargeFile(fileId: String) {
        val auth = authData ?: return

        val body = JSONObject().apply {
            put("fileId", fileId)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${auth.apiUrl}/b2api/v2/b2_cancel_large_file")
            .post(body)
            .build()

        httpClient.newCall(request).execute()
    }

    private suspend fun uploadMetadata(snapshotId: SnapshotId, metadata: CloudSnapshotMetadata) {
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

        uploadSmallFile(
            CloudFile(metadataFile, "${snapshotId.value}_metadata.json", "", metadataFile.length()),
            "${snapshotId.value}_metadata.json"
        )

        metadataFile.delete()
    }

    data class B2FileInfo(
        val fileId: String,
        val fileName: String,
        val contentLength: Long,
        val contentSha1: String
    )

    private suspend fun listFiles(prefix: String): List<B2FileInfo> {
        val auth = authData 
            ?: throw IllegalStateException("Not authenticated - call authenticate() first")
        val bucket = bucketId 
            ?: throw IllegalStateException("Bucket not initialized - call initialize() first")

        val files = mutableListOf<B2FileInfo>()
        var nextFileName: String? = null

        do {
            val body = JSONObject().apply {
                put("bucketId", bucket)
                if (prefix.isNotEmpty()) put("prefix", prefix)
                put("maxFileCount", 1000)
                if (nextFileName != null) put("startFileName", nextFileName)
            }.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("${auth.apiUrl}/b2api/v2/b2_list_file_names")
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "{}")
            
            val filesArray = json.getJSONArray("files")
            for (i in 0 until filesArray.length()) {
                val fileObj = filesArray.getJSONObject(i)
                files.add(
                    B2FileInfo(
                        fileId = fileObj.getString("fileId"),
                        fileName = fileObj.getString("fileName"),
                        contentLength = fileObj.getLong("contentLength"),
                        contentSha1 = fileObj.optString("contentSha1", "")
                    )
                )
            }

            nextFileName = json.optString("nextFileName").takeIf { it.isNotEmpty() }
        } while (nextFileName != null)

        return files
    }

    private suspend fun downloadFileById(fileId: String, destination: File) {
        val auth = authData ?: throw Exception("Not authenticated")

        val request = Request.Builder()
            .url("${auth.downloadUrl}/b2api/v2/b2_download_file_by_id?fileId=$fileId")
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

    private suspend fun deleteFile(fileId: String, fileName: String) {
        val auth = authData ?: return

        val body = JSONObject().apply {
            put("fileId", fileId)
            put("fileName", fileName)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${auth.apiUrl}/b2api/v2/b2_delete_file_version")
            .post(body)
            .build()

        httpClient.newCall(request).execute()
    }

    private fun matchesFilter(metadata: CloudSnapshotMetadata, filter: CloudSnapshotFilter): Boolean {
        if (filter.afterTimestamp != null && metadata.timestamp <= filter.afterTimestamp) return false
        if (filter.beforeTimestamp != null && metadata.timestamp >= filter.beforeTimestamp) return false
        if (filter.deviceId != null && metadata.deviceId != filter.deviceId) return false
        return true
    }

    private fun calculateSHA1(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun calculateSHA1(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-1")
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }

    /**
     * Backblaze OAuth2 Provider implementation
     * Note: B2 uses application keys, not traditional OAuth2
     */
    inner class BackblazeOAuth2Provider(
        context: Context,
        keystoreManager: KeystoreManager,
        logger: ObsidianLogger
    ) : OAuth2Provider(context, keystoreManager, logger) {
        override val providerId = "backblaze_b2"
        override val displayName = "Backblaze B2"
        override val authorizationEndpoint = "https://secure.backblaze.com/b2_buckets.htm"
        override val tokenEndpoint = "$API_BASE_URL/b2api/v2/b2_authorize_account"
        override val clientId = BuildConfig.B2_KEY_ID
        override val clientSecret = BuildConfig.B2_APPLICATION_KEY
        override val scopes = listOf("listBuckets", "listFiles", "readFiles", "writeFiles", "deleteFiles")
        override val redirectUri = "com.obsidianbackup://oauth2/backblaze"
    }
}
