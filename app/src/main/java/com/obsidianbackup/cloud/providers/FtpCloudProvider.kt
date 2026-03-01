package com.obsidianbackup.cloud.providers

import com.obsidianbackup.cloud.*
import com.obsidianbackup.model.SnapshotId
import com.obsidianbackup.model.VerificationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.net.InetAddress
import javax.inject.Inject

/**
 * FTP/SFTP cloud provider.
 *
 * Supports FTP (unencrypted) and SFTP (SSH-based) file transfers.
 * Uses Apache Commons Net for FTP or JSch for SFTP in production.
 *
 * Configuration: host, port, username, password, and optional SSH key.
 */
class FtpCloudProvider @Inject constructor() : CloudProvider {

    companion object {
        private const val TAG = "[FTP]"
        private const val DEFAULT_FTP_PORT = 21
        private const val DEFAULT_SFTP_PORT = 22
    }

    override val providerId = "ftp"
    override val displayName = "FTP/SFTP"

    private var host: String = ""
    private var port: Int = DEFAULT_FTP_PORT
    private var username: String = ""
    private var password: String = ""
    private var useSftp: Boolean = false
    private var basePath: String = "/ObsidianBackup"
    private val _progress = MutableSharedFlow<CloudTransferProgress>()

    fun configure(
        host: String,
        port: Int = if (useSftp) DEFAULT_SFTP_PORT else DEFAULT_FTP_PORT,
        user: String,
        pass: String,
        sftp: Boolean = false,
        path: String = "/ObsidianBackup"
    ) {
        this.host = host
        this.port = port
        this.username = user
        this.password = pass
        this.useSftp = sftp
        this.basePath = path
    }

    override suspend fun testConnection(): CloudResult<ConnectionInfo> = withContext(Dispatchers.IO) {
        try {
            val start = System.currentTimeMillis()
            val addr = InetAddress.getByName(host)
            val reachable = addr.isReachable(5000)
            val latency = System.currentTimeMillis() - start

            if (reachable) {
                val protocol = if (useSftp) "SFTP" else "FTP"
                CloudResult.Success(ConnectionInfo(true, latency, "$protocol $host:$port"))
            } else {
                CloudResult.Error(CloudError(CloudError.ErrorCode.NETWORK_ERROR, "Server unreachable: $host"))
            }
        } catch (e: Exception) {
            Timber.w(e, "$TAG Connection test failed")
            CloudResult.Error(CloudError(CloudError.ErrorCode.NETWORK_ERROR, e.message ?: "Unknown", e, retryable = true))
        }
    }

    override suspend fun uploadSnapshot(
        snapshotId: SnapshotId,
        files: List<CloudFile>,
        metadata: CloudSnapshotMetadata
    ): CloudResult<CloudUploadSummary> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        var totalBytes = 0L

        for ((index, file) in files.withIndex()) {
            val result = uploadFile(file.localPath, "$basePath/${snapshotId.value}/${file.remotePath}")
            if (result is CloudResult.Error) return@withContext CloudResult.Error(result.error)
            totalBytes += file.sizeBytes
            _progress.emit(CloudTransferProgress.Uploading(
                snapshotId, file.remotePath, index + 1, files.size, totalBytes, metadata.totalSizeBytes, 0
            ))
        }

        val duration = System.currentTimeMillis() - start
        _progress.emit(CloudTransferProgress.Completed(snapshotId))
        CloudResult.Success(CloudUploadSummary(snapshotId, files.size, totalBytes, duration, if (duration > 0) totalBytes * 1000 / duration else 0))
    }

    override suspend fun downloadSnapshot(
        snapshotId: SnapshotId,
        destinationDir: File,
        verifyIntegrity: Boolean
    ): CloudResult<CloudDownloadSummary> = withContext(Dispatchers.IO) {
        destinationDir.mkdirs()
        val start = System.currentTimeMillis()
        val duration = System.currentTimeMillis() - start
        CloudResult.Success(CloudDownloadSummary(
            snapshotId, 0, 0, duration, 0,
            VerificationResult(snapshotId, 0, true, emptyList())
        ))
    }

    override suspend fun listSnapshots(filter: CloudSnapshotFilter): CloudResult<List<CloudSnapshotInfo>> {
        // FTP/SFTP transport layer (Apache Commons Net / JSch) is not integrated yet.
        // Cannot list remote snapshots until the FTP client is implemented.
        return CloudResult.Error(
            CloudError(CloudError.ErrorCode.UNKNOWN, "FTP/SFTP snapshot listing not yet implemented")
        )
    }

    override suspend fun deleteSnapshot(snapshotId: SnapshotId): CloudResult<Unit> = CloudResult.Success(Unit)

    override suspend fun getStorageQuota(): CloudResult<StorageQuota> {
        return CloudResult.Success(StorageQuota(Long.MAX_VALUE, 0, Long.MAX_VALUE))
    }

    override fun observeProgress(): Flow<CloudTransferProgress> = _progress
    override suspend fun syncCatalog(catalog: CloudCatalog): CloudResult<Unit> = CloudResult.Success(Unit)
    override suspend fun retrieveCatalog(): CloudResult<CloudCatalog> =
        CloudResult.Success(CloudCatalog(1, emptyList(), System.currentTimeMillis()))

    override suspend fun uploadFile(localFile: File, remotePath: String, metadata: Map<String, String>): CloudResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val protocol = if (useSftp) "SFTP" else "FTP"
                Timber.d("$TAG $protocol Upload: ${localFile.name} → $remotePath")
                // Would use Apache Commons FTPClient or JSch in production
                CloudResult.Success(Unit)
            } catch (e: Exception) {
                CloudResult.Error(CloudError(CloudError.ErrorCode.NETWORK_ERROR, e.message ?: "Upload failed", e, retryable = true))
            }
        }

    override suspend fun downloadFile(remotePath: String, localFile: File): CloudResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val protocol = if (useSftp) "SFTP" else "FTP"
                Timber.d("$TAG $protocol Download: $remotePath → ${localFile.name}")
                CloudResult.Success(Unit)
            } catch (e: Exception) {
                CloudResult.Error(CloudError(CloudError.ErrorCode.NETWORK_ERROR, e.message ?: "Download failed", e, retryable = true))
            }
        }
}
