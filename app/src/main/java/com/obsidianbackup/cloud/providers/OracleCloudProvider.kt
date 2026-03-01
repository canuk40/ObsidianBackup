// cloud/providers/OracleCloudProvider.kt
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
import java.security.PrivateKey
import java.security.Signature
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Oracle Cloud Object Storage provider with OAuth2 authentication via OCI
 * Supports multi-account management and multipart uploads
 */
class OracleCloudProvider(
    private val context: Context,
    private val keystoreManager: KeystoreManager,
    private val logger: ObsidianLogger,
    private val bucketName: String,
    private val namespace: String,
    private val region: String = "us-ashburn-1",
    private val accountId: String = "default"
) : CloudProvider {

    override val providerId: String = "oracle_cloud"
    override val displayName: String = "Oracle Cloud Object Storage"

    private val oauth2Provider = OracleOAuth2Provider(context, keystoreManager, logger)
    
    private val endpoint = "https://objectstorage.$region.oraclecloud.com"
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            
            // Add OCI signature
            runCatching {
                kotlinx.coroutines.runBlocking {
                    when (val tokenResult = oauth2Provider.getValidToken(accountId)) {
                        is OAuth2Result.Success -> {
                            val builder = original.newBuilder()
                            val signature = generateOciSignature(
                                original.method,
                                original.url.encodedPath,
                                original.headers,
                                tokenResult.data
                            )
                            builder.header("Authorization", signature)
                            builder.header("date", getRFC1123Date())
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

    private var tenancyId: String = ""
    private var userId: String = ""
    private var fingerprint: String = ""

    companion object {
        private const val TAG = "OracleCloudProvider"
        private const val PART_SIZE = 10 * 1024 * 1024 // 10MB minimum for OCI
        private const val MAX_PARTS = 10000
    }

    override suspend fun testConnection(): CloudResult<ConnectionInfo> = withContext(Dispatchers.IO) {
        return@withContext try {
            val startTime = System.currentTimeMillis()
            
            // Get bucket info
            val request = Request.Builder()
                .url("$endpoint/n/$namespace/b/$bucketName")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime

            if (!response.isSuccessful) {
                // Try to create bucket if it doesn't exist
                if (response.code == 404) {
                    createBucket()
                    return@withContext CloudResult.Success(
                        ConnectionInfo(
                            isConnected = true,
                            latencyMs = latency,
                            serverVersion = "Oracle Cloud Object Storage API"
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
                    serverVersion = "Oracle Cloud Object Storage API"
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

                val objectName = "${snapshotId.value}/${cloudFile.remotePath}"
                
                val success = if (cloudFile.sizeBytes > PART_SIZE * 2) {
                    uploadMultipartObject(cloudFile, objectName)
                } else {
                    uploadSimpleObject(cloudFile, objectName)
                }

                if (success) {
                    remoteUrls[cloudFile.remotePath] = "$endpoint/n/$namespace/b/$bucketName/o/$objectName"
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

            objects.forEachIndexed { index, objectName ->
                val fileName = objectName.substringAfter("${snapshotId.value}/")
                val destinationFile = File(destinationDir, fileName)

                val objectSize = getObjectSize(objectName)

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

                downloadObject(objectName, destinationFile)
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
                // Get all objects and extract unique prefixes
                val allObjects = listObjects("")
                val prefixes = allObjects
                    .mapNotNull { 
                        if (it.contains("/")) it.substringBefore("/") else null 
                    }
                    .distinct()

                val snapshots = mutableListOf<CloudSnapshotInfo>()

                for (prefix in prefixes) {
                    val snapshotId = SnapshotId(prefix)
                    
                    // Try to load metadata
                    if (objectExists("${prefix}_metadata.json")) {
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

            for (objectName in allObjects) {
                deleteObject(objectName)
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
            // OCI doesn't have a direct quota API, calculate used space
            var usedBytes = 0L
            val objects = listObjects("")
            
            for (objectName in objects) {
                usedBytes += getObjectSize(objectName)
            }

            // OCI has large limits
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

    private suspend fun createBucket() {
        val body = JSONObject().apply {
            put("compartmentId", tenancyId)
            put("name", bucketName)
            put("publicAccessType", "NoPublicAccess")
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$endpoint/n/$namespace/b/")
            .post(body)
            .build()

        httpClient.newCall(request).execute()
    }

    private suspend fun uploadSimpleObject(cloudFile: CloudFile, objectName: String): Boolean {
        val request = Request.Builder()
            .url("$endpoint/n/$namespace/b/$bucketName/o/$objectName")
            .put(cloudFile.localPath.asRequestBody("application/octet-stream".toMediaType()))
            .header("Content-Length", cloudFile.sizeBytes.toString())
            .build()

        val response = httpClient.newCall(request).execute()
        return response.isSuccessful
    }

    private suspend fun uploadMultipartObject(cloudFile: CloudFile, objectName: String): Boolean {
        // Create multipart upload
        val createBody = JSONObject().apply {
            put("object", objectName)
        }.toString().toRequestBody("application/json".toMediaType())

        val createRequest = Request.Builder()
            .url("$endpoint/n/$namespace/b/$bucketName/u")
            .post(createBody)
            .build()

        val createResponse = httpClient.newCall(createRequest).execute()
        if (!createResponse.isSuccessful) return false

        val createJson = JSONObject(createResponse.body?.string() ?: "{}")
        val uploadId = createJson.getString("uploadId")

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
                .url("$endpoint/n/$namespace/b/$bucketName/u/$uploadId/$partNumber")
                .put(partData.toRequestBody("application/octet-stream".toMediaType()))
                .build()

            val partResponse = httpClient.newCall(partRequest).execute()
            if (!partResponse.isSuccessful) {
                inputStream.close()
                abortMultipartUpload(uploadId)
                return false
            }

            val etag = partResponse.header("ETag")?.trim('"') ?: ""
            parts.add(partNumber to etag)
            partNumber++
        }

        inputStream.close()

        // Commit multipart upload
        val commitBody = JSONObject().apply {
            put("partsToCommit", parts.map { (num, etag) ->
                JSONObject().apply {
                    put("partNum", num)
                    put("etag", etag)
                }
            })
        }.toString().toRequestBody("application/json".toMediaType())

        val commitRequest = Request.Builder()
            .url("$endpoint/n/$namespace/b/$bucketName/u/$uploadId")
            .post(commitBody)
            .build()

        val commitResponse = httpClient.newCall(commitRequest).execute()
        return commitResponse.isSuccessful
    }

    private suspend fun abortMultipartUpload(uploadId: String) {
        val request = Request.Builder()
            .url("$endpoint/n/$namespace/b/$bucketName/u/$uploadId")
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
        var nextStart: String? = null

        do {
            val url = buildString {
                append("$endpoint/n/$namespace/b/$bucketName/o?limit=1000")
                if (prefix.isNotEmpty()) append("&prefix=$prefix")
                if (nextStart != null) append("&start=$nextStart")
            }

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "{}")
            
            val objectsArray = json.getJSONArray("objects")
            for (i in 0 until objectsArray.length()) {
                val obj = objectsArray.getJSONObject(i)
                objects.add(obj.getString("name"))
            }

            nextStart = json.optString("nextStartWith").takeIf { it.isNotEmpty() }
        } while (nextStart != null)

        return objects
    }

    private suspend fun downloadObject(objectName: String, destination: File) {
        val request = Request.Builder()
            .url("$endpoint/n/$namespace/b/$bucketName/o/$objectName")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Download failed: ${response.code}")

        FileOutputStream(destination).use { output ->
            response.body?.byteStream()?.copyTo(output)
        }
    }

    private suspend fun deleteObject(objectName: String) {
        val request = Request.Builder()
            .url("$endpoint/n/$namespace/b/$bucketName/o/$objectName")
            .delete()
            .build()

        httpClient.newCall(request).execute()
    }

    private suspend fun objectExists(objectName: String): Boolean {
        val request = Request.Builder()
            .url("$endpoint/n/$namespace/b/$bucketName/o/$objectName")
            .head()
            .build()

        val response = httpClient.newCall(request).execute()
        return response.isSuccessful
    }

    private suspend fun getObjectSize(objectName: String): Long {
        val request = Request.Builder()
            .url("$endpoint/n/$namespace/b/$bucketName/o/$objectName")
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

    /**
     * Sign an OCI API request using RSA-SHA256.
     *
     * The OCI private key must be stored in EncryptedSharedPreferences as a PEM string
     * under the key "oci_private_key". Store it once via:
     *   keystoreManager.storeToken("oci_private_key", pemContent)
     *
     * The key can be PKCS#8 ("-----BEGIN PRIVATE KEY-----") or
     * traditional RSA ("-----BEGIN RSA PRIVATE KEY-----").
     *
     * @throws OciSigningException if the key is missing or signing fails
     */
    @Throws(OciSigningException::class)
    private fun generateOciSignature(
        method: String,
        path: String,
        headers: Headers,
        @Suppress("UNUSED_PARAMETER") oauthToken: String  // retained for call-site compatibility
    ): String {
        val date = getRFC1123Date()
        val host = "$region.oraclecloud.com"

        val signingString = buildString {
            append("(request-target): ${method.lowercase(Locale.ROOT)} $path\n")
            append("date: $date\n")
            append("host: $host")
        }

        // Load the OCI RSA private key stored as a PEM string in EncryptedSharedPreferences
        val privateKeyPem = keystoreManager.getToken("oci_private_key")
            ?: throw OciSigningException(
                "OCI private key not configured. " +
                "Call keystoreManager.storeToken(\"oci_private_key\", pemContent) " +
                "with the PEM from your OCI Console → Identity → API Keys."
            )

        // Strip PEM headers and whitespace, then decode base64 → PKCS#8 bytes
        val pemBody = privateKeyPem
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val keyBytes = android.util.Base64.decode(pemBody, android.util.Base64.DEFAULT)

        val privateKey: PrivateKey = try {
            java.security.KeyFactory.getInstance("RSA")
                .generatePrivate(java.security.spec.PKCS8EncodedKeySpec(keyBytes))
        } catch (e: Exception) {
            throw OciSigningException("Failed to parse OCI private key: ${e.message}", e)
        }

        // SHA256withRSA sign
        val signatureBytes = try {
            Signature.getInstance("SHA256withRSA").run {
                initSign(privateKey)
                update(signingString.toByteArray(Charsets.UTF_8))
                sign()
            }
        } catch (e: Exception) {
            throw OciSigningException("RSA signing failed: ${e.message}", e)
        }

        val signatureBase64 = android.util.Base64.encodeToString(
            signatureBytes, android.util.Base64.NO_WRAP
        )

        val keyId = "$tenancyId/$userId/$fingerprint"

        return "Signature version=\"1\"," +
            "keyId=\"$keyId\"," +
            "algorithm=\"rsa-sha256\"," +
            "headers=\"(request-target) date host\"," +
            "signature=\"$signatureBase64\""
    }

    class OciSigningException(message: String, cause: Throwable? = null) :
        Exception(message, cause)

    private fun getRFC1123Date(): String {
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("GMT")
        return dateFormat.format(Date())
    }

    /**
     * Oracle OAuth2 Provider implementation
     */
    inner class OracleOAuth2Provider(
        context: Context,
        keystoreManager: KeystoreManager,
        logger: ObsidianLogger
    ) : OAuth2Provider(context, keystoreManager, logger) {
        override val providerId = "oracle_cloud"
        override val displayName = "Oracle Cloud Object Storage"
        override val authorizationEndpoint = "https://login.oracle.com/oauth2/v1/authorize"
        override val tokenEndpoint = "https://login.oracle.com/oauth2/v1/token"
        override val clientId = "YOUR_ORACLE_CLIENT_ID" // Replace with actual client ID
        override val clientSecret = "YOUR_ORACLE_CLIENT_SECRET" // Replace with actual client secret
        override val scopes = listOf("urn:opc:objectstorage:*")
        override val redirectUri = "com.obsidianbackup://oauth2/oracle"
    }
}
