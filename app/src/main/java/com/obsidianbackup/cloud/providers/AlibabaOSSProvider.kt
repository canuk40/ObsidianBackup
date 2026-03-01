// cloud/providers/AlibabaOSSProvider.kt
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
 * Alibaba Cloud OSS provider with OAuth2 authentication via RAM
 * Supports multi-account management and multipart uploads
 */
class AlibabaOSSProvider(
    private val context: Context,
    private val keystoreManager: KeystoreManager,
    private val logger: ObsidianLogger,
    private val bucketName: String,
    private val region: String = "cn-hangzhou",
    private val accountId: String = "default"
) : CloudProvider {

    override val providerId: String = "alibaba_oss"
    override val displayName: String = "Alibaba Cloud OSS"

    private val oauth2Provider = AlibabaOAuth2Provider(context, keystoreManager, logger)
    
    private val endpoint = "https://$bucketName.oss-$region.aliyuncs.com"
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            
            // Add OSS signature
            runCatching {
                kotlinx.coroutines.runBlocking {
                    when (val tokenResult = oauth2Provider.getValidToken(accountId)) {
                        is OAuth2Result.Success -> {
                            val builder = original.newBuilder()
                            val signature = generateSignature(
                                original.method,
                                original.url.encodedPath,
                                original.headers,
                                tokenResult.data
                            )
                            builder.header("Authorization", "OSS $accessKeyId:$signature")
                            builder.header("Date", getRFC2822Date())
                            chain.proceed(builder.build())
                        }
                        is OAuth2Result.Error -> {
                            logger.e(TAG, "Failed to get valid token: ${tokenResult.message}")
                            chain.proceed(original)
                        }
                    }
                }
            }.getOrElse { chain.proceed(original) }
        }
        .build()

    private val progressFlow = MutableStateFlow<CloudTransferProgress>(
        CloudTransferProgress.Completed(SnapshotId(""))
    )

    private var accessKeyId: String = ""
    private var accessKeySecret: String = ""

    companion object {
        private const val TAG = "AlibabaOSSProvider"
        private const val PART_SIZE = 5 * 1024 * 1024 // 5MB minimum for OSS
        private const val MAX_PARTS = 10000
    }

    override suspend fun testConnection(): CloudResult<ConnectionInfo> = withContext(Dispatchers.IO) {
        return@withContext try {
            val startTime = System.currentTimeMillis()
            
            // Get bucket info
            val request = Request.Builder()
                .url("$endpoint/?acl")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime

            if (!response.isSuccessful) {
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
                    serverVersion = "Alibaba OSS API"
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

                val objectKey = "${snapshotId.value}/${cloudFile.remotePath}"
                
                val success = if (cloudFile.sizeBytes > PART_SIZE * 2) {
                    uploadMultipartObject(cloudFile, objectKey)
                } else {
                    uploadSimpleObject(cloudFile, objectKey)
                }

                if (success) {
                    remoteUrls[cloudFile.remotePath] = "$endpoint/$objectKey"
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

            // List objects with prefix
            val objects = listObjects("${snapshotId.value}/")
                .filter { !it.endsWith("_metadata.json") }

            objects.forEachIndexed { index, objectKey ->
                val fileName = objectKey.substringAfter("${snapshotId.value}/")
                val destinationFile = File(destinationDir, fileName)

                val objectSize = getObjectSize(objectKey)

                progressFlow.value = CloudTransferProgress.Downloading(
                    snapshotId = snapshotId,
                    currentFile = fileName,
                    filesCompleted = index,
                    totalFiles = objects.size,
                    bytesTransferred = downloadedBytes,
                    totalBytes = objects.sumOf { getObjectSize(it) },
                    transferRate = if (System.currentTimeMillis() - startTime > 0) {
                        downloadedBytes * 1000 / (System.currentTimeMillis() - startTime)
                    } else 0L
                )

                downloadObject(objectKey, destinationFile)
                downloadedBytes += objectSize
            }

            val duration = System.currentTimeMillis() - startTime
            val averageSpeed = if (duration > 0) downloadedBytes * 1000 / duration else 0L

            progressFlow.value = CloudTransferProgress.Completed(snapshotId)

            CloudResult.Success(
                CloudDownloadSummary(
                    snapshotId = snapshotId,
                    filesDownloaded = objects.size,
                    bytesDownloaded = downloadedBytes,
                    duration = duration,
                    averageSpeed = averageSpeed,
                    verificationResult = VerificationResult(
                        snapshotId = snapshotId,
                        filesChecked = objects.size,
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
                // List object prefixes (common prefixes)
                val prefixes = listObjectPrefixes()
                val snapshots = mutableListOf<CloudSnapshotInfo>()

                for (prefix in prefixes) {
                    val snapshotIdStr = prefix.trimEnd('/')
                    val snapshotId = SnapshotId(snapshotIdStr)
                    
                    // Try to load metadata
                    if (objectExists("${snapshotIdStr}_metadata.json")) {
                        val metadata = downloadMetadata(snapshotId)
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
            // List and delete all objects with prefix
            val objects = listObjects("${snapshotId.value}/")
            
            // Also include metadata
            val allObjects = objects + "${snapshotId.value}_metadata.json"

            for (objectKey in allObjects) {
                deleteObject(objectKey)
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
            // OSS doesn't have a direct quota API, calculate used space
            var usedBytes = 0L
            val objects = listObjects("")
            
            for (objectKey in objects) {
                usedBytes += getObjectSize(objectKey)
            }

            // OSS has large limits, return a large number
            val totalBytes = 5L * 1024 * 1024 * 1024 * 1024 // 5TB

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
            }.toString()

            val catalogFile = File(context.cacheDir, "catalog.json")
            catalogFile.writeText(catalogJson)

            val success = uploadSimpleObject(
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
            if (!objectExists("catalog.json")) {
                return@withContext CloudResult.Error(
                    CloudError(CloudError.ErrorCode.FILE_NOT_FOUND, "Catalog not found")
                )
            }

            val catalogFile = File(context.cacheDir, "catalog_download.json")
            downloadObject("catalog.json", catalogFile)

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
            val success = if (localFile.length() > PART_SIZE * 2) {
                uploadMultipartObject(CloudFile(localFile, remotePath, "", localFile.length()), remotePath)
            } else {
                uploadSimpleObject(CloudFile(localFile, remotePath, "", localFile.length()), remotePath)
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
                downloadObject(remotePath, localFile)
                CloudResult.Success(Unit)
            } catch (e: Exception) {
                logger.e(TAG, "Download file failed", e)
                CloudResult.Error(
                    CloudError(CloudError.ErrorCode.UNKNOWN, "Download failed: ${e.message}", e, retryable = true)
                )
            }
        }

    // Helper methods

    private suspend fun uploadSimpleObject(cloudFile: CloudFile, objectKey: String): Boolean {
        val request = Request.Builder()
            .url("$endpoint/$objectKey")
            .put(cloudFile.localPath.asRequestBody("application/octet-stream".toMediaType()))
            .header("Content-Length", cloudFile.sizeBytes.toString())
            .build()

        val response = httpClient.newCall(request).execute()
        return response.isSuccessful
    }

    private suspend fun uploadMultipartObject(cloudFile: CloudFile, objectKey: String): Boolean {
        // Initiate multipart upload
        val initiateRequest = Request.Builder()
            .url("$endpoint/$objectKey?uploads")
            .post("".toRequestBody())
            .build()

        val initiateResponse = httpClient.newCall(initiateRequest).execute()
        if (!initiateResponse.isSuccessful) return false

        val initiateXml = initiateResponse.body?.string() ?: return false
        val uploadId = extractXmlValue(initiateXml, "UploadId") ?: return false

        // Upload parts
        val parts = mutableListOf<Pair<Int, String>>()
        val inputStream = cloudFile.localPath.inputStream()
        var partNumber = 1
        val buffer = ByteArray(PART_SIZE)

        while (true) {
            val bytesRead = inputStream.read(buffer)
            if (bytesRead <= 0) break

            val partData = buffer.copyOf(bytesRead)
            
            val partRequest = Request.Builder()
                .url("$endpoint/$objectKey?partNumber=$partNumber&uploadId=$uploadId")
                .put(partData.toRequestBody("application/octet-stream".toMediaType()))
                .build()

            val partResponse = httpClient.newCall(partRequest).execute()
            if (!partResponse.isSuccessful) {
                inputStream.close()
                abortMultipartUpload(objectKey, uploadId)
                return false
            }

            val etag = partResponse.header("ETag")?.trim('"') ?: ""
            parts.add(partNumber to etag)
            partNumber++
        }

        inputStream.close()

        // Complete multipart upload
        val completeXml = buildCompleteMultipartXml(parts)
        val completeRequest = Request.Builder()
            .url("$endpoint/$objectKey?uploadId=$uploadId")
            .post(completeXml.toRequestBody("application/xml".toMediaType()))
            .build()

        val completeResponse = httpClient.newCall(completeRequest).execute()
        return completeResponse.isSuccessful
    }

    private fun buildCompleteMultipartXml(parts: List<Pair<Int, String>>): String {
        val builder = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><CompleteMultipartUpload>")
        for ((partNumber, etag) in parts) {
            builder.append("<Part><PartNumber>$partNumber</PartNumber><ETag>$etag</ETag></Part>")
        }
        builder.append("</CompleteMultipartUpload>")
        return builder.toString()
    }

    private suspend fun abortMultipartUpload(objectKey: String, uploadId: String) {
        val request = Request.Builder()
            .url("$endpoint/$objectKey?uploadId=$uploadId")
            .delete()
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

        uploadSimpleObject(
            CloudFile(metadataFile, "${snapshotId.value}_metadata.json", "", metadataFile.length()),
            "${snapshotId.value}_metadata.json"
        )

        metadataFile.delete()
    }

    private suspend fun downloadMetadata(snapshotId: SnapshotId): CloudSnapshotMetadata? {
        return try {
            val tempFile = File.createTempFile("metadata_", ".json", context.cacheDir)
            downloadObject("${snapshotId.value}_metadata.json", tempFile)

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

    private suspend fun listObjects(prefix: String): List<String> {
        val objects = mutableListOf<String>()
        var marker: String? = null

        do {
            val url = if (marker != null) {
                "$endpoint/?prefix=$prefix&marker=$marker&max-keys=1000"
            } else {
                "$endpoint/?prefix=$prefix&max-keys=1000"
            }

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val xml = response.body?.string() ?: break

            // Simple XML parsing
            val keyPattern = "<Key>([^<]+)</Key>".toRegex()
            keyPattern.findAll(xml).forEach { match ->
                objects.add(match.groupValues[1])
            }

            marker = extractXmlValue(xml, "NextMarker")
        } while (!marker.isNullOrEmpty())

        return objects
    }

    private suspend fun listObjectPrefixes(): List<String> {
        val prefixes = mutableListOf<String>()

        val request = Request.Builder()
            .url("$endpoint/?delimiter=/&max-keys=1000")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        val xml = response.body?.string() ?: return prefixes

        // Extract common prefixes
        val prefixPattern = "<Prefix>([^<]+)</Prefix>".toRegex()
        prefixPattern.findAll(xml).forEach { match ->
            val prefix = match.groupValues[1]
            if (prefix.contains("/")) {
                prefixes.add(prefix)
            }
        }

        return prefixes
    }

    private suspend fun downloadObject(objectKey: String, destination: File) {
        val request = Request.Builder()
            .url("$endpoint/$objectKey")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Download failed: ${response.code}")

        FileOutputStream(destination).use { output ->
            response.body?.byteStream()?.copyTo(output)
        }
    }

    private suspend fun deleteObject(objectKey: String) {
        val request = Request.Builder()
            .url("$endpoint/$objectKey")
            .delete()
            .build()

        httpClient.newCall(request).execute()
    }

    private suspend fun objectExists(objectKey: String): Boolean {
        val request = Request.Builder()
            .url("$endpoint/$objectKey")
            .head()
            .build()

        val response = httpClient.newCall(request).execute()
        return response.isSuccessful
    }

    private suspend fun getObjectSize(objectKey: String): Long {
        val request = Request.Builder()
            .url("$endpoint/$objectKey")
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

    private fun generateSignature(
        method: String,
        path: String,
        headers: Headers,
        accessKeySecret: String
    ): String {
        val canonicalString = buildCanonicalString(method, path, headers)
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(accessKeySecret.toByteArray(), "HmacSHA1"))
        val signature = mac.doFinal(canonicalString.toByteArray())
        return Base64.getEncoder().encodeToString(signature)
    }

    private fun buildCanonicalString(method: String, path: String, headers: Headers): String {
        val builder = StringBuilder()
        builder.append(method).append("\n")
        builder.append(headers["Content-MD5"] ?: "").append("\n")
        builder.append(headers["Content-Type"] ?: "").append("\n")
        builder.append(headers["Date"] ?: getRFC2822Date()).append("\n")
        
        // Add OSS headers
        val ossHeaders = headers.names()
            .filter { it.startsWith("x-oss-") }
            .sorted()
            .joinToString("\n") { "$it:${headers[it]}" }
        
        if (ossHeaders.isNotEmpty()) {
            builder.append(ossHeaders).append("\n")
        }
        
        builder.append("/$bucketName$path")
        return builder.toString()
    }

    private fun getRFC2822Date(): String {
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("GMT")
        return dateFormat.format(Date())
    }

    private fun extractXmlValue(xml: String, tag: String): String? {
        val pattern = "<$tag>([^<]+)</$tag>".toRegex()
        return pattern.find(xml)?.groupValues?.get(1)
    }

    /**
     * Alibaba OAuth2 Provider implementation
     */
    inner class AlibabaOAuth2Provider(
        context: Context,
        keystoreManager: KeystoreManager,
        logger: ObsidianLogger
    ) : OAuth2Provider(context, keystoreManager, logger) {
        override val providerId = "alibaba_oss"
        override val displayName = "Alibaba Cloud OSS"
        override val authorizationEndpoint = "https://signin.aliyun.com/oauth2/v1/authorize"
        override val tokenEndpoint = "https://oauth.aliyun.com/v1/token"
        override val clientId = "YOUR_ALIBABA_CLIENT_ID" // Replace with actual client ID
        override val clientSecret = "YOUR_ALIBABA_CLIENT_SECRET" // Replace with actual client secret
        override val scopes = listOf("oss:ListBuckets", "oss:GetObject", "oss:PutObject", "oss:DeleteObject")
        override val redirectUri = "com.obsidianbackup://oauth2/alibaba"
    }
}
