// storage/FileSystemManager.kt
package com.obsidianbackup.storage

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import com.obsidianbackup.logging.ObsidianLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FileSystemManager - Handles all file system operations with scoped storage compliance
 * 
 * This class provides a unified interface for file operations that works across
 * Android versions, prioritizing scoped storage APIs (MediaStore, SAF) over
 * legacy file operations.
 * 
 * Key features:
 * - MediaStore API for shared media files
 * - Storage Access Framework (SAF) for user-selected directories
 * - App-private storage (getExternalFilesDir) for backup data
 * - Backwards compatibility with older Android versions
 */
@Singleton
class FileSystemManager @Inject constructor(
    private val context: Context,
    private val logger: ObsidianLogger
) {
    
    companion object {
        private const val TAG = "FileSystemManager"
        private const val BACKUP_MIME_TYPE = "application/octet-stream"
        private const val ARCHIVE_MIME_TYPE = "application/x-tar"
    }

    /**
     * Storage location types
     */
    enum class StorageLocation {
        /** App-private external storage - no permissions needed (Android 4.4+) */
        APP_EXTERNAL_FILES,
        
        /** App-private cache - no permissions needed */
        APP_CACHE,
        
        /** Shared storage via MediaStore - scoped storage compliant */
        MEDIA_STORE,
        
        /** User-selected directory via SAF - scoped storage compliant */
        SAF_DIRECTORY
    }

    /**
     * Get app-private external files directory for backups
     * This is the PRIMARY location for backup storage (no permissions required)
     */
    fun getBackupDirectory(): File {
        val dir = context.getExternalFilesDir("backups")
            ?: context.filesDir.resolve("backups")
        
        if (!dir.exists()) {
            dir.mkdirs()
        }
        
        logger.d(TAG, "Backup directory: ${dir.absolutePath}")
        return dir
    }

    /**
     * Get app-private external files directory for logs
     */
    fun getLogsDirectory(): File {
        val dir = context.getExternalFilesDir("logs")
            ?: context.filesDir.resolve("logs")
        
        if (!dir.exists()) {
            dir.mkdirs()
        }
        
        return dir
    }

    /**
     * Get app-private cache directory
     */
    fun getCacheDirectory(): File {
        val dir = context.externalCacheDir ?: context.cacheDir
        return dir
    }

    /**
     * Get temporary directory for operations
     */
    fun getTempDirectory(): File {
        val dir = File(getCacheDirectory(), "temp")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Create a snapshot directory in app-private storage
     */
    fun createSnapshotDirectory(snapshotId: String): File {
        val backupDir = getBackupDirectory()
        val snapshotDir = File(backupDir, snapshotId)
        
        if (!snapshotDir.exists()) {
            snapshotDir.mkdirs()
        }
        
        return snapshotDir
    }

    /**
     * Get existing snapshot directory
     */
    fun getSnapshotDirectory(snapshotId: String): File? {
        val backupDir = getBackupDirectory()
        val snapshotDir = File(backupDir, snapshotId)
        
        return if (snapshotDir.exists() && snapshotDir.isDirectory) {
            snapshotDir
        } else {
            null
        }
    }

    /**
     * Delete a snapshot directory recursively
     */
    suspend fun deleteSnapshotDirectory(snapshotId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val snapshotDir = getSnapshotDirectory(snapshotId)
            if (snapshotDir != null && snapshotDir.exists()) {
                val deleted = snapshotDir.deleteRecursively()
                logger.i(TAG, "Deleted snapshot directory: $snapshotId, success: $deleted")
                return@withContext deleted
            }
            return@withContext true
        } catch (e: Exception) {
            logger.e(TAG, "Failed to delete snapshot directory: $snapshotId", e)
            return@withContext false
        }
    }

    /**
     * Export backup to shared storage using MediaStore (Android 10+)
     * This allows users to access backups via file manager
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun exportBackupToMediaStore(
        sourceFile: File,
        displayName: String,
        relativePath: String = "${Environment.DIRECTORY_DOCUMENTS}/ObsidianBackup"
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val resolver: ContentResolver = context.contentResolver
            
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, ARCHIVE_MIME_TYPE)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }
            
            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = resolver.insert(collection, contentValues)
            
            if (itemUri != null) {
                resolver.openOutputStream(itemUri)?.use { outputStream ->
                    sourceFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                logger.i(TAG, "Exported backup to MediaStore: $displayName")
                return@withContext itemUri
            } else {
                logger.e(TAG, "Failed to create MediaStore entry for: $displayName")
                return@withContext null
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to export backup to MediaStore", e)
            return@withContext null
        }
    }

    /**
     * Import backup from MediaStore URI
     */
    suspend fun importBackupFromUri(
        sourceUri: Uri,
        destinationFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                destinationFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            logger.i(TAG, "Imported backup from URI to: ${destinationFile.absolutePath}")
            return@withContext true
        } catch (e: Exception) {
            logger.e(TAG, "Failed to import backup from URI", e)
            return@withContext false
        }
    }

    /**
     * Create intent to let user pick a directory using SAF
     */
    fun createDirectoryPickerIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
    }

    /**
     * Persist URI permissions for SAF directory
     */
    fun persistDirectoryPermissions(uri: Uri) {
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or 
                           Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            logger.i(TAG, "Persisted permissions for URI: $uri")
        } catch (e: Exception) {
            logger.e(TAG, "Failed to persist URI permissions", e)
        }
    }

    /**
     * Export backup to SAF directory
     */
    suspend fun exportBackupToSafDirectory(
        sourceFile: File,
        safDirectoryUri: Uri,
        displayName: String
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val directory = DocumentFile.fromTreeUri(context, safDirectoryUri)
            if (directory == null || !directory.isDirectory) {
                logger.e(TAG, "Invalid SAF directory URI")
                return@withContext null
            }

            // Check if file already exists
            var targetFile = directory.findFile(displayName)
            if (targetFile != null) {
                targetFile.delete()
            }
            
            targetFile = directory.createFile(ARCHIVE_MIME_TYPE, displayName)
            if (targetFile == null) {
                logger.e(TAG, "Failed to create file in SAF directory")
                return@withContext null
            }

            context.contentResolver.openOutputStream(targetFile.uri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            logger.i(TAG, "Exported backup to SAF directory: $displayName")
            return@withContext targetFile.uri
        } catch (e: Exception) {
            logger.e(TAG, "Failed to export backup to SAF directory", e)
            return@withContext null
        }
    }

    /**
     * List all backups in app-private storage
     */
    suspend fun listBackups(): List<File> = withContext(Dispatchers.IO) {
        try {
            val backupDir = getBackupDirectory()
            val snapshots = backupDir.listFiles { file ->
                file.isDirectory && !file.name.startsWith(".")
            }?.toList() ?: emptyList()
            
            logger.d(TAG, "Found ${snapshots.size} backup snapshots")
            return@withContext snapshots
        } catch (e: Exception) {
            logger.e(TAG, "Failed to list backups", e)
            return@withContext emptyList()
        }
    }

    /**
     * Calculate total size of app-private backup storage
     */
    suspend fun calculateBackupStorageSize(): Long = withContext(Dispatchers.IO) {
        try {
            val backupDir = getBackupDirectory()
            return@withContext calculateDirectorySize(backupDir)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to calculate backup storage size", e)
            return@withContext 0L
        }
    }

    /**
     * Calculate size of a directory recursively
     */
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    calculateDirectorySize(file)
                } else {
                    file.length()
                }
            }
        }
        return size
    }

    /**
     * Clean up temporary files
     */
    suspend fun cleanupTempFiles(): Boolean = withContext(Dispatchers.IO) {
        try {
            val tempDir = getTempDirectory()
            val deleted = tempDir.deleteRecursively()
            if (deleted) {
                tempDir.mkdirs()
            }
            logger.i(TAG, "Cleaned up temp files: $deleted")
            return@withContext deleted
        } catch (e: Exception) {
            logger.e(TAG, "Failed to cleanup temp files", e)
            return@withContext false
        }
    }

    /**
     * Check if we have enough storage space for a backup
     */
    fun hasEnoughSpace(requiredBytes: Long): Boolean {
        val backupDir = getBackupDirectory()
        val availableBytes = backupDir.usableSpace
        
        val hasSpace = availableBytes > requiredBytes * 1.1 // 10% buffer
        logger.d(TAG, "Storage check - Required: $requiredBytes, Available: $availableBytes, Has space: $hasSpace")
        
        return hasSpace
    }

    /**
     * Get storage statistics
     */
    data class StorageStats(
        val totalSpace: Long,
        val usableSpace: Long,
        val backupSize: Long,
        val percentUsed: Float
    )

    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val backupDir = getBackupDirectory()
        val backupSize = calculateBackupStorageSize()
        
        return@withContext StorageStats(
            totalSpace = backupDir.totalSpace,
            usableSpace = backupDir.usableSpace,
            backupSize = backupSize,
            percentUsed = if (backupDir.totalSpace > 0) {
                (backupSize.toFloat() / backupDir.totalSpace.toFloat()) * 100f
            } else {
                0f
            }
        )
    }

    /**
     * Copy file safely with error handling
     */
    suspend fun copyFile(source: File, destination: File): Boolean = withContext(Dispatchers.IO) {
        try {
            source.inputStream().use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            logger.d(TAG, "Copied file: ${source.absolutePath} -> ${destination.absolutePath}")
            return@withContext true
        } catch (e: Exception) {
            logger.e(TAG, "Failed to copy file", e)
            return@withContext false
        }
    }

    /**
     * Check if a file exists and is readable
     */
    fun isFileAccessible(file: File): Boolean {
        return file.exists() && file.canRead()
    }
}
