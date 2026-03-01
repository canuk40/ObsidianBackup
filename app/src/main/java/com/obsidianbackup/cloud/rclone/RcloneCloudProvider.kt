// cloud/rclone/RcloneCloudProvider.kt
package com.obsidianbackup.cloud.rclone

import android.content.Context
import com.obsidianbackup.model.SnapshotId
import com.obsidianbackup.plugins.interfaces.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Base implementation for rclone-based cloud providers
 * Provides common functionality for all rclone backends
 */
abstract class RcloneCloudProvider(
    protected val context: Context,
    protected val remoteName: String,
    protected val backend: RcloneBackend
) : CloudProvider {
    
    protected val executor = RcloneExecutor(context)
    protected val configManager = RcloneConfigManager(context, File(executor.getConfigPath()))
    
    private val _progressFlow = MutableStateFlow<TransferProgress?>(null)
    
    protected val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    override suspend fun initialize(config: CloudConfig): kotlin.Result<Unit> {
        // Initialize executor
        val initResult = executor.initialize()
        if (initResult is RcloneResult.Error) {
            return kotlin.Result.failure(
                Exception(initResult.error.message, initResult.error.cause)
            )
        }
        
        // Create remote configuration
        val credentials = getCredentialsMap(config)
        val additionalOptions = getAdditionalOptions(config)
        
        val configResult = configManager.createOrUpdateRemote(
            remoteName = remoteName,
            backend = backend,
            credentials = credentials,
            additionalOptions = additionalOptions
        )
        
        return when (configResult) {
            is RcloneResult.Success -> kotlin.Result.success(Unit)
            is RcloneResult.Error -> kotlin.Result.failure(
                Exception(configResult.error.message, configResult.error.cause)
            )
        }
    }
    
    override suspend fun testConnection(): kotlin.Result<Unit> {
        // Test connection by listing root directory
        val result = executor.executeCommand(
            listOf("lsd", "$remoteName:", "--max-depth", "1"),
            timeout = 10000
        )
        
        return when (result) {
            is RcloneResult.Success -> kotlin.Result.success(Unit)
            is RcloneResult.Error -> kotlin.Result.failure(
                Exception(result.error.message, result.error.cause)
            )
        }
    }
    
    override suspend fun uploadFile(
        localFile: File,
        remotePath: String,
        metadata: Map<String, String>
    ): CloudResult {
        if (!localFile.exists()) {
            return CloudResult(
                success = false,
                error = "Local file does not exist: ${localFile.absolutePath}"
            )
        }
        
        val remoteFullPath = "$remoteName:$remotePath"
        
        // Use copyto for single file upload
        val result = executor.executeCommand(
            listOf(
                "copyto",
                localFile.absolutePath,
                remoteFullPath,
                "--progress",
                "--stats", "1s"
            ),
            timeout = 300000, // 5 minutes
            progressCallback = { line ->
                parseProgressLine(line, localFile.name, localFile.length())
            }
        )
        
        return when (result) {
            is RcloneResult.Success -> {
                CloudResult(
                    success = true,
                    metadata = mapOf(
                        "remote_path" to remotePath,
                        "size" to localFile.length().toString()
                    )
                )
            }
            is RcloneResult.Error -> {
                CloudResult(
                    success = false,
                    error = result.error.message
                )
            }
        }
    }
    
    override suspend fun downloadFile(
        remotePath: String,
        localFile: File
    ): CloudResult {
        val remoteFullPath = "$remoteName:$remotePath"
        
        // Create parent directory if needed
        localFile.parentFile?.mkdirs()
        
        // Use copyto for single file download
        val result = executor.executeCommand(
            listOf(
                "copyto",
                remoteFullPath,
                localFile.absolutePath,
                "--progress",
                "--stats", "1s"
            ),
            timeout = 300000, // 5 minutes
            progressCallback = { line ->
                parseProgressLine(line, remotePath, 0)
            }
        )
        
        return when (result) {
            is RcloneResult.Success -> {
                CloudResult(success = true)
            }
            is RcloneResult.Error -> {
                CloudResult(
                    success = false,
                    error = result.error.message
                )
            }
        }
    }
    
    override suspend fun listFiles(prefix: String): List<CloudFile> {
        val remotePrefix = "$remoteName:$prefix"
        
        // Use lsjson to get file listing with metadata
        val result = executor.executeJsonCommand<List<RcloneFileInfo>>(
            listOf("lsjson", remotePrefix, "--recursive"),
            timeout = 30000
        )
        
        return when (result) {
            is RcloneResult.Success -> {
                result.data.map { fileInfo ->
                    CloudFile(
                        path = fileInfo.Path,
                        size = fileInfo.Size,
                        lastModified = parseRcloneTime(fileInfo.ModTime),
                        checksum = fileInfo.Hashes?.get("md5"),
                        metadata = mapOf(
                            "is_dir" to fileInfo.IsDir.toString(),
                            "mime_type" to (fileInfo.MimeType ?: "")
                        )
                    )
                }
            }
            is RcloneResult.Error -> {
                emptyList()
            }
        }
    }
    
    override suspend fun deleteFile(remotePath: String): CloudResult {
        val remoteFullPath = "$remoteName:$remotePath"
        
        val result = executor.executeCommand(
            listOf("deletefile", remoteFullPath),
            timeout = 30000
        )
        
        return when (result) {
            is RcloneResult.Success -> CloudResult(success = true)
            is RcloneResult.Error -> CloudResult(
                success = false,
                error = result.error.message
            )
        }
    }
    
    override suspend fun getFileMetadata(remotePath: String): CloudFile? {
        val files = listFiles(remotePath)
        return files.firstOrNull { it.path == remotePath }
    }
    
    override fun observeTransferProgress(): Flow<TransferProgress> {
        return _progressFlow.asStateFlow() as Flow<TransferProgress>
    }
    
    /**
     * Parse rclone progress output
     * Format: "Transferred:   1.234 MiB / 10.000 MiB, 12%, 100.00 KiB/s, ETA 1m30s"
     */
    private fun parseProgressLine(line: String, fileName: String, totalBytes: Long) {
        if (line.contains("Transferred:")) {
            try {
                // Extract transferred bytes and speed
                val parts = line.split(",")
                
                // Parse bytes transferred (e.g., "1.234 MiB / 10.000 MiB")
                val bytesPattern = Regex("""([\d.]+)\s+(\w+)\s+/\s+([\d.]+)\s+(\w+)""")
                val bytesMatch = bytesPattern.find(line)
                
                // Parse speed (e.g., "100.00 KiB/s")
                val speedPattern = Regex("""([\d.]+)\s+(\w+)/s""")
                val speedMatch = speedPattern.find(line)
                
                if (bytesMatch != null && speedMatch != null) {
                    val transferred = parseSize(bytesMatch.groupValues[1], bytesMatch.groupValues[2])
                    val total = parseSize(bytesMatch.groupValues[3], bytesMatch.groupValues[4])
                    val speedValue = speedMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                    val speedUnit = speedMatch.groupValues[2]
                    val speed = parseSize(speedValue.toString(), speedUnit)
                    
                    // Emit progress (placeholder SnapshotId)
                    _progressFlow.value = TransferProgress(
                        snapshotId = SnapshotId("current"),
                        bytesTransferred = transferred,
                        totalBytes = total.coerceAtLeast(totalBytes),
                        speedBps = speed
                    )
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
    }
    
    /**
     * Parse size string to bytes (e.g., "1.5 MiB" -> bytes)
     */
    private fun parseSize(value: String, unit: String): Long {
        val numValue = value.toDoubleOrNull() ?: 0.0
        return when (unit.uppercase()) {
            "B", "BYTES" -> numValue.toLong()
            "KIB", "KB" -> (numValue * 1024).toLong()
            "MIB", "MB" -> (numValue * 1024 * 1024).toLong()
            "GIB", "GB" -> (numValue * 1024 * 1024 * 1024).toLong()
            "TIB", "TB" -> (numValue * 1024 * 1024 * 1024 * 1024).toLong()
            else -> numValue.toLong()
        }
    }
    
    /**
     * Parse rclone time string (ISO 8601)
     */
    private fun parseRcloneTime(timeStr: String): Long {
        return try {
            // Simplified: Use current time as fallback
            // In production, use proper ISO 8601 parser
            java.time.Instant.parse(timeStr).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    
    /**
     * Map CloudConfig to backend-specific credentials
     * Subclasses should override this
     */
    protected abstract fun getCredentialsMap(config: CloudConfig): Map<String, String>
    
    /**
     * Get additional backend-specific options
     * Subclasses can override this
     */
    protected open fun getAdditionalOptions(config: CloudConfig): Map<String, String> {
        // Default implementation returns base rclone options
        return mapOf(
            "skip_links" to "false",
            "no_check_certificate" to "false",
            "no_modtime" to "false"
        )
    }
}

/**
 * rclone lsjson output format
 */
@Serializable
data class RcloneFileInfo(
    val Path: String,
    val Name: String,
    val Size: Long,
    val MimeType: String? = null,
    val ModTime: String,
    val IsDir: Boolean,
    val Hashes: Map<String, String>? = null,
    val ID: String? = null,
    val OrigID: String? = null
)
