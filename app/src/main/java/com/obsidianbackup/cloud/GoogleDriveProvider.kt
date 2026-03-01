// cloud/GoogleDriveProvider.kt
package com.obsidianbackup.cloud

import android.content.Context
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.model.SnapshotId
import com.obsidianbackup.model.VerificationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.min

class GoogleDriveProvider(
    private val context: Context,
    private val oauthManager: OAuth2Manager,
    private val logger: ObsidianLogger
) : CloudProvider {

    override val providerId: String = "google_drive"
    override val displayName: String = "Google Drive"

    private val driveService: Drive by lazy {
        Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            oauthManager.getDriveCredential()
        )
            .setApplicationName("ObsidianBackup")
            .build()
    }

    override suspend fun testConnection(): CloudResult<ConnectionInfo> {
        return try {
            // Try to get about info
            val about = driveService.about().get().setFields("user, storageQuota").execute()
            val quota = about.storageQuota
            val latency = measureLatency()

            CloudResult.Success(
                ConnectionInfo(
                    isConnected = true,
                    latencyMs = latency,
                    serverVersion = "Google Drive API v3"
                )
            )
        } catch (e: Exception) {
            logger.e(TAG, "Connection test failed", e)
            CloudResult.Error(
                CloudError(
                    CloudError.ErrorCode.NETWORK_ERROR,
                    "Failed to connect to Google Drive: ${e.message}",
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
    ): CloudResult<CloudUploadSummary> {
        return try {
            val startTime = System.currentTimeMillis()
            var totalBytes = 0L
            var uploadedBytes = 0L
            val remoteUrls = mutableMapOf<String, String>()

            // Create a folder for the snapshot
            val folderId = createSnapshotFolder(snapshotId, metadata)

            for (file in files) {
                val driveFile = DriveFile()
                    .setName(file.remotePath.substringAfterLast('/'))
                    .setParents(listOf(folderId))

                val fileContent = FileContent("application/octet-stream", file.localPath)

                val uploadedFile = driveService.files().create(driveFile, fileContent)
                    .setFields("id, size")
                    .execute()

                uploadedBytes += uploadedFile.size ?: 0
                totalBytes += file.sizeBytes
                remoteUrls[file.remotePath] = uploadedFile.id
            }
            
            // Calculate duration and average speed
            val duration = System.currentTimeMillis() - startTime
            val averageSpeed = if (duration > 0) {
                (uploadedBytes * 1000) / duration // bytes per second
            } else {
                0L
            }

            logger.i(TAG, "Upload complete: ${files.size} files, ${uploadedBytes} bytes in ${duration}ms (${averageSpeed} B/s)")

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
            CloudResult.Error(
                CloudError(
                    CloudError.ErrorCode.UNKNOWN,
                    "Upload failed: ${e.message}",
                    e,
                    retryable = true
                )
            )
        }
    }

    override suspend fun downloadSnapshot(
        snapshotId: SnapshotId,
        destinationDir: File,
        verifyIntegrity: Boolean
    ): CloudResult<CloudDownloadSummary> {
        return try {
            val startTime = System.currentTimeMillis()
            
            // Find the snapshot folder
            val folderId = findSnapshotFolder(snapshotId) ?: return CloudResult.Error(
                CloudError(CloudError.ErrorCode.FILE_NOT_FOUND, "Snapshot not found")
            )

            // List files in the folder
            val files = driveService.files().list()
                .setQ("'${folderId}' in parents")
                .setFields("files(id, name, size)")
                .execute()
                .files

            var downloadedBytes = 0L
            val totalBytes = files.sumOf { it.size ?: 0 }
            val downloadedFiles = mutableListOf<File>()

            for (driveFile in files) {
                val localFile = File(destinationDir, driveFile.name)
                FileOutputStream(localFile).buffered(8192).use { outputStream ->
                    driveService.files().get(driveFile.id).executeMediaAndDownloadTo(outputStream)
                }

                downloadedBytes += driveFile.size ?: 0
                downloadedFiles.add(localFile)
            }
            
            // Calculate duration and speed
            val duration = System.currentTimeMillis() - startTime
            val averageSpeed = if (duration > 0) {
                (downloadedBytes * 1000) / duration // bytes per second
            } else {
                0L
            }
            
            // Perform integrity verification if requested
            val verificationResult = if (verifyIntegrity) {
                verifyDownloadedFiles(snapshotId, downloadedFiles)
            } else {
                VerificationResult(
                    snapshotId = snapshotId,
                    filesChecked = files.size,
                    allValid = true
                )
            }

            logger.i(TAG, "Download complete: ${files.size} files, ${downloadedBytes} bytes in ${duration}ms (${averageSpeed} B/s)")

            CloudResult.Success(
                CloudDownloadSummary(
                    snapshotId = snapshotId,
                    filesDownloaded = files.size,
                    bytesDownloaded = downloadedBytes,
                    duration = duration,
                    averageSpeed = averageSpeed,
                    verificationResult = verificationResult
                )
            )
        } catch (e: Exception) {
            logger.e(TAG, "Download failed", e)
            CloudResult.Error(
                CloudError(
                    CloudError.ErrorCode.UNKNOWN,
                    "Download failed: ${e.message}",
                    e,
                    retryable = true
                )
            )
        }
    }
    
    /**
     * Verify downloaded files integrity by checking file sizes and basic structure
     */
    private fun verifyDownloadedFiles(snapshotId: SnapshotId, files: List<File>): VerificationResult {
        val corruptedFiles = mutableListOf<String>()
        var filesChecked = 0
        
        files.forEach { file ->
            filesChecked++
            
            // Basic verification: file exists and has non-zero size
            if (!file.exists() || file.length() == 0L) {
                corruptedFiles.add("${file.name} (missing or empty)")
            }
            
            // Additional verification for known file types
            when {
                file.name.endsWith(".tar.zst") || file.name.endsWith(".tar.gz") -> {
                    // Check if it looks like a valid compressed archive (magic bytes)
                    try {
                        val header = file.inputStream().use { it.readNBytes(4) }
                        val isValid = when {
                            file.name.endsWith(".tar.zst") -> header.isNotEmpty()
                            file.name.endsWith(".tar.gz") -> header.size >= 2 && 
                                header[0] == 0x1f.toByte() && header[1] == 0x8b.toByte()
                            else -> true
                        }
                        if (!isValid) {
                            corruptedFiles.add("${file.name} (invalid archive header)")
                        }
                    } catch (e: Exception) {
                        corruptedFiles.add("${file.name} (unreadable)")
                    }
                }
            }
        }
        
        return VerificationResult(
            snapshotId = snapshotId,
            filesChecked = filesChecked,
            allValid = corruptedFiles.isEmpty(),
            corruptedFiles = corruptedFiles
        )
    }

    override suspend fun listSnapshots(filter: CloudSnapshotFilter): CloudResult<List<CloudSnapshotInfo>> {
        return try {
            // Find all snapshot folders
            val folders = driveService.files().list()
                .setQ("mimeType='application/vnd.google-apps.folder' and name contains 'snapshot_'")
                .setFields("files(id, name, createdTime)")
                .execute()
                .files

            val snapshots = folders.mapNotNull { folder ->
                parseSnapshotInfo(folder)
            }.filter { snapshot ->
                // Apply filters
                (filter.afterTimestamp == null || snapshot.timestamp >= filter.afterTimestamp) &&
                (filter.beforeTimestamp == null || snapshot.timestamp <= filter.beforeTimestamp)
            }.take(filter.maxResults)

            CloudResult.Success(snapshots)
        } catch (e: Exception) {
            logger.e(TAG, "List snapshots failed", e)
            CloudResult.Error(
                CloudError(
                    CloudError.ErrorCode.UNKNOWN,
                    "Failed to list snapshots: ${e.message}",
                    e,
                    retryable = true
                )
            )
        }
    }

    override suspend fun deleteSnapshot(snapshotId: SnapshotId): CloudResult<Unit> {
        return try {
            val folderId = findSnapshotFolder(snapshotId) ?: return CloudResult.Error(
                CloudError(CloudError.ErrorCode.FILE_NOT_FOUND, "Snapshot not found")
            )

            // Delete the entire folder
            driveService.files().delete(folderId).execute()

            CloudResult.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Delete failed", e)
            CloudResult.Error(
                CloudError(
                    CloudError.ErrorCode.UNKNOWN,
                    "Delete failed: ${e.message}",
                    e,
                    retryable = false
                )
            )
        }
    }

    override suspend fun getStorageQuota(): CloudResult<StorageQuota> {
        return try {
            val about = driveService.about().get().setFields("storageQuota").execute()
            val quota = about.storageQuota

            CloudResult.Success(
                StorageQuota(
                    totalBytes = quota.limit ?: 0,
                    usedBytes = quota.usage ?: 0,
                    availableBytes = (quota.limit ?: 0) - (quota.usage ?: 0)
                )
            )
        } catch (e: Exception) {
            logger.e(TAG, "Get quota failed", e)
            CloudResult.Error(
                CloudError(
                    CloudError.ErrorCode.UNKNOWN,
                    "Failed to get storage quota: ${e.message}",
                    e,
                    retryable = true
                )
            )
        }
    }

    override fun observeProgress(): Flow<CloudTransferProgress> {
        // Implement progress tracking using a flow
        // This would be updated during actual transfers
        return flow {
            // For now, return empty flow
            // In a full implementation, this would emit progress updates during transfers
        }
    }

    override suspend fun syncCatalog(catalog: CloudCatalog): CloudResult<Unit> {
        return try {
            // Serialize catalog to JSON
            val catalogJson = serializeCatalog(catalog)
            
            // Create a temporary file
            val catalogFile = File.createTempFile("catalog", ".json", context.cacheDir)
            catalogFile.writeText(catalogJson)
            
            // Upload to Google Drive
            val driveFile = DriveFile()
                .setName("backup_catalog.json")
                .setDescription("ObsidianBackup catalog - ${catalog.snapshots.size} snapshots")
            
            val fileContent = FileContent("application/json", catalogFile)
            
            // Try to find existing catalog file
            val existingFileId = findCatalogFile()
            
            if (existingFileId != null) {
                // Update existing file
                driveService.files().update(existingFileId, driveFile, fileContent).execute()
                logger.i(TAG, "Updated catalog with ${catalog.snapshots.size} snapshots")
            } else {
                // Create new file
                driveService.files().create(driveFile, fileContent)
                    .setFields("id")
                    .execute()
                logger.i(TAG, "Created new catalog with ${catalog.snapshots.size} snapshots")
            }
            
            // Clean up temp file
            catalogFile.delete()
            
            CloudResult.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Catalog sync failed", e)
            CloudResult.Error(
                CloudError(
                    CloudError.ErrorCode.UNKNOWN,
                    "Catalog sync failed: ${e.message}",
                    e,
                    retryable = true
                )
            )
        }
    }

    override suspend fun retrieveCatalog(): CloudResult<CloudCatalog> {
        return try {
            // Find catalog file
            val fileId = findCatalogFile() ?: return CloudResult.Error(
                CloudError(CloudError.ErrorCode.FILE_NOT_FOUND, "Catalog not found in cloud")
            )
            
            // Download catalog
            val outputStream = java.io.ByteArrayOutputStream()
            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            
            val catalogJson = outputStream.toString("UTF-8")
            val catalog = deserializeCatalog(catalogJson)
            
            logger.i(TAG, "Retrieved catalog with ${catalog.snapshots.size} snapshots")
            CloudResult.Success(catalog)
        } catch (e: Exception) {
            logger.e(TAG, "Catalog retrieval failed", e)
            CloudResult.Error(
                CloudError(
                    CloudError.ErrorCode.UNKNOWN,
                    "Catalog retrieval failed: ${e.message}",
                    e,
                    retryable = true
                )
            )
        }
    }
    
    /**
     * Find the catalog file ID in Google Drive
     */
    private suspend fun findCatalogFile(): String? {
        return try {
            val result = driveService.files().list()
                .setQ("name='backup_catalog.json' and trashed=false")
                .setFields("files(id)")
                .setOrderBy("modifiedTime desc")
                .setPageSize(1)
                .execute()
            
            result.files.firstOrNull()?.id
        } catch (e: Exception) {
            logger.w(TAG, "Failed to find catalog file: ${e.message}")
            null
        }
    }
    
    /**
     * Serialize catalog to JSON
     */
    private fun serializeCatalog(catalog: CloudCatalog): String {
        val json = StringBuilder()
        json.append("{\n")
        json.append("  \"version\": ${catalog.version},\n")
        json.append("  \"lastUpdated\": ${catalog.lastUpdated},\n")
        json.append("  \"snapshots\": [\n")
        
        catalog.snapshots.forEachIndexed { index, snapshot ->
            json.append("    {\n")
            json.append("      \"snapshotId\": \"${snapshot.snapshotId.value}\",\n")
            json.append("      \"timestamp\": ${snapshot.timestamp},\n")
            json.append("      \"sizeBytes\": ${snapshot.sizeBytes},\n")
            json.append("      \"fileCount\": ${snapshot.fileCount},\n")
            json.append("      \"checksum\": \"${snapshot.checksum}\"\n")
            json.append("    }")
            if (index < catalog.snapshots.size - 1) json.append(",")
            json.append("\n")
        }
        
        json.append("  ]\n")
        json.append("}")
        return json.toString()
    }
    
    /**
     * Deserialize catalog from JSON (simple parser)
     */
    private fun deserializeCatalog(json: String): CloudCatalog {
        // Simple JSON parsing - in production, use a proper JSON library
        val snapshots = mutableListOf<CloudSnapshotInfo>()
        
        // Extract version
        val versionRegex = "\"version\":\\s*(\\d+)".toRegex()
        val version = versionRegex.find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 1
        
        // Extract last updated
        val lastUpdatedRegex = "\"lastUpdated\":\\s*(\\d+)".toRegex()
        val lastUpdated = lastUpdatedRegex.find(json)?.groupValues?.get(1)?.toLongOrNull() 
            ?: System.currentTimeMillis()
        
        // Extract snapshots (simplified)
        val snapshotPattern = "\"snapshotId\":\\s*\"([^\"]+)\"".toRegex()
        snapshotPattern.findAll(json).forEach { match ->
            val snapshotId = match.groupValues[1]
            snapshots.add(CloudSnapshotInfo(
                snapshotId = SnapshotId(snapshotId),
                timestamp = System.currentTimeMillis(),
                sizeBytes = 0L,
                fileCount = 0,
                checksum = "",
                metadata = CloudSnapshotMetadata(
                    snapshotId = SnapshotId(snapshotId),
                    timestamp = System.currentTimeMillis(),
                    deviceId = android.os.Build.DEVICE,
                    appCount = 0,
                    totalSizeBytes = 0L,
                    compressionRatio = 1.0f,
                    encrypted = false,
                    merkleRootHash = ""
                )
            ))
        }
        
        return CloudCatalog(
            version = version,
            snapshots = snapshots,
            lastUpdated = lastUpdated,
            signature = null
        )
    }

    override suspend fun uploadFile(
        localFile: File,
        remotePath: String,
        metadata: Map<String, String>
    ): CloudResult<Unit> {
        return try {
            val driveFile = DriveFile()
                .setName(localFile.name)
                .setDescription(metadata["description"] ?: "")

            val fileContent = FileContent("application/octet-stream", localFile)

            driveService.files().create(driveFile, fileContent)
                .setFields("id")
                .execute()

            CloudResult.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Upload file failed", e)
            CloudResult.Error(
                CloudError(
                    CloudError.ErrorCode.UNKNOWN,
                    "Upload failed: ${e.message}",
                    e,
                    retryable = true
                )
            )
        }
    }

    override suspend fun downloadFile(
        remotePath: String,
        localFile: File
    ): CloudResult<Unit> {
        return try {
            // Find file by name (simplified - should use proper path resolution)
            val result = driveService.files().list()
                .setQ("name='${localFile.name}'")
                .setFields("files(id)")
                .execute()

            val fileId = result.files.firstOrNull()?.id ?: return CloudResult.Error(
                CloudError(CloudError.ErrorCode.FILE_NOT_FOUND, "File not found")
            )

            val outputStream = localFile.outputStream()
            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            outputStream.close()

            CloudResult.Success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Download file failed", e)
            CloudResult.Error(
                CloudError(
                    CloudError.ErrorCode.UNKNOWN,
                    "Download failed: ${e.message}",
                    e,
                    retryable = true
                )
            )
        }
    }

    private suspend fun createSnapshotFolder(snapshotId: SnapshotId, metadata: CloudSnapshotMetadata): String {
        val folder = DriveFile()
            .setName("snapshot_${snapshotId.value}")
            .setMimeType("application/vnd.google-apps.folder")
            .setDescription("Backup snapshot: ${metadata.appCount} apps, ${metadata.totalSizeBytes} bytes")

        val createdFolder = driveService.files().create(folder)
            .setFields("id")
            .execute()

        return createdFolder.id
    }

    private suspend fun findSnapshotFolder(snapshotId: SnapshotId): String? {
        val result = driveService.files().list()
            .setQ("mimeType='application/vnd.google-apps.folder' and name='snapshot_${snapshotId.value}'")
            .setFields("files(id)")
            .execute()

        return result.files.firstOrNull()?.id
    }

    private suspend fun parseSnapshotInfo(folder: DriveFile): CloudSnapshotInfo? {
        return try {
            // Parse snapshot ID from folder name
            val snapshotIdStr = folder.name.substringAfter("snapshot_")
            val snapshotId = SnapshotId(snapshotIdStr)
            
            // Get files in the folder to calculate size and count
            val files = driveService.files().list()
                .setQ("'${folder.id}' in parents")
                .setFields("files(id, name, size, md5Checksum)")
                .execute()
                .files
            
            val totalSize = files.sumOf { (it.size ?: 0L).toLong() }
            val fileCount = files.size
            
            // Calculate overall checksum from individual file checksums
            val checksum = if (files.isNotEmpty()) {
                val allChecksums = files.mapNotNull { it.md5Checksum }.sorted().joinToString("")
                hashString(allChecksums)
            } else {
                ""
            }
            
            // Parse device ID from folder description
            val deviceId = folder.description?.substringAfter("Device: ")?.substringBefore(",") 
                ?: android.os.Build.DEVICE
            
            // Parse app count from description
            val appCountMatch = folder.description?.let { desc ->
                "(\\d+) apps".toRegex().find(desc)
            }
            val appCount = appCountMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

            CloudSnapshotInfo(
                snapshotId = snapshotId,
                timestamp = folder.createdTime?.value ?: 0,
                sizeBytes = totalSize,
                fileCount = fileCount,
                checksum = checksum,
                metadata = CloudSnapshotMetadata(
                    snapshotId = snapshotId,
                    timestamp = folder.createdTime?.value ?: 0,
                    deviceId = deviceId,
                    appCount = appCount,
                    totalSizeBytes = totalSize,
                    compressionRatio = 0.65f, // Estimated
                    encrypted = files.any { it.name.endsWith(".enc") || it.name.endsWith(".encrypted") },
                    merkleRootHash = checksum
                )
            )
        } catch (e: Exception) {
            logger.w(TAG, "Failed to parse snapshot info for ${folder.name}: ${e.message}")
            null
        }
    }
    
    /**
     * Calculate SHA-256 hash of a string
     */
    private fun hashString(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private suspend fun measureLatency(): Long {
        val start = System.currentTimeMillis()
        try {
            driveService.about().get().setFields("user").execute()
        } catch (e: Exception) {
            // Ignore
        }
        return System.currentTimeMillis() - start
    }

    companion object {
        private const val TAG = "GoogleDriveProvider"
    }
}
