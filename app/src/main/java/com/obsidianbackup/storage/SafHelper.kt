// storage/SafHelper.kt
package com.obsidianbackup.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.obsidianbackup.logging.ObsidianLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SafHelper - Storage Access Framework helper for user-selected directories
 * 
 * This class provides utilities for working with SAF directories chosen by users,
 * allowing backup exports to custom locations while maintaining scoped storage compliance.
 */
@Singleton
class SafHelper @Inject constructor(
    private val context: Context,
    private val logger: ObsidianLogger
) {
    
    companion object {
        private const val TAG = "SafHelper"
        const val PREF_KEY_SAF_URI = "saf_backup_directory_uri"
    }

    /**
     * Create an intent for the user to pick a directory
     */
    fun createDirectoryPickerIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        }
    }

    /**
     * Persist permissions for a SAF directory URI
     * This should be called after user selects a directory
     */
    fun persistDirectoryPermissions(uri: Uri): Boolean {
        return try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or 
                           Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            logger.i(TAG, "Persisted permissions for URI: $uri")
            true
        } catch (e: Exception) {
            logger.e(TAG, "Failed to persist URI permissions", e)
            false
        }
    }

    /**
     * Release permissions for a SAF directory URI
     */
    fun releaseDirectoryPermissions(uri: Uri): Boolean {
        return try {
            val releaseFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or 
                              Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            
            context.contentResolver.releasePersistableUriPermission(uri, releaseFlags)
            logger.i(TAG, "Released permissions for URI: $uri")
            true
        } catch (e: Exception) {
            logger.e(TAG, "Failed to release URI permissions", e)
            false
        }
    }

    /**
     * Get all persisted URIs
     */
    fun getPersistedUris(): List<Uri> {
        return try {
            context.contentResolver.persistedUriPermissions.map { it.uri }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to get persisted URIs", e)
            emptyList()
        }
    }

    /**
     * Check if we have persisted permissions for a URI
     */
    fun hasPersistedPermissions(uri: Uri): Boolean {
        return try {
            val persistedUris = context.contentResolver.persistedUriPermissions
            persistedUris.any { it.uri == uri && it.isReadPermission && it.isWritePermission }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to check persisted permissions", e)
            false
        }
    }

    /**
     * Export a file to a SAF directory
     */
    suspend fun exportFileToSafDirectory(
        sourceFile: File,
        safDirectoryUri: Uri,
        fileName: String,
        mimeType: String = "application/octet-stream"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists() || !sourceFile.canRead()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Source file does not exist or is not readable")
                )
            }

            val directory = DocumentFile.fromTreeUri(context, safDirectoryUri)
            if (directory == null || !directory.exists() || !directory.isDirectory) {
                return@withContext Result.failure(
                    IllegalArgumentException("Invalid SAF directory URI")
                )
            }

            // Check if file already exists and delete it
            var targetFile = directory.findFile(fileName)
            if (targetFile != null && targetFile.exists()) {
                logger.d(TAG, "File exists, deleting: $fileName")
                targetFile.delete()
            }
            
            // Create new file
            targetFile = directory.createFile(mimeType, fileName)
            if (targetFile == null) {
                return@withContext Result.failure(
                    RuntimeException("Failed to create file in SAF directory")
                )
            }

            // Copy file content
            context.contentResolver.openOutputStream(targetFile.uri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    val bytesWritten = inputStream.copyTo(outputStream)
                    logger.d(TAG, "Wrote $bytesWritten bytes to SAF file")
                }
            } ?: return@withContext Result.failure(
                RuntimeException("Failed to open output stream")
            )

            logger.i(TAG, "Successfully exported file to SAF directory: $fileName")
            return@withContext Result.success(targetFile.uri)
            
        } catch (e: Exception) {
            logger.e(TAG, "Failed to export file to SAF directory", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Import a file from a SAF URI
     */
    suspend fun importFileFromSafUri(
        sourceUri: Uri,
        destinationFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Ensure parent directory exists
            destinationFile.parentFile?.mkdirs()
            
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                destinationFile.outputStream().use { outputStream ->
                    val bytesRead = inputStream.copyTo(outputStream)
                    logger.d(TAG, "Imported $bytesRead bytes from SAF URI")
                }
            } ?: return@withContext Result.failure(
                RuntimeException("Failed to open input stream from URI")
            )

            logger.i(TAG, "Successfully imported file to: ${destinationFile.absolutePath}")
            return@withContext Result.success(destinationFile)
            
        } catch (e: Exception) {
            logger.e(TAG, "Failed to import file from SAF URI", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * List files in a SAF directory
     */
    suspend fun listFilesInSafDirectory(safDirectoryUri: Uri): Result<List<SafFileInfo>> = 
        withContext(Dispatchers.IO) {
        try {
            val directory = DocumentFile.fromTreeUri(context, safDirectoryUri)
            if (directory == null || !directory.exists() || !directory.isDirectory) {
                return@withContext Result.failure(
                    IllegalArgumentException("Invalid SAF directory URI")
                )
            }

            val files = directory.listFiles().mapNotNull { file ->
                if (file.isFile) {
                    SafFileInfo(
                        uri = file.uri,
                        name = file.name ?: "unknown",
                        size = file.length(),
                        lastModified = file.lastModified(),
                        mimeType = file.type
                    )
                } else {
                    null
                }
            }

            logger.d(TAG, "Found ${files.size} files in SAF directory")
            return@withContext Result.success(files)
            
        } catch (e: Exception) {
            logger.e(TAG, "Failed to list files in SAF directory", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Delete a file from a SAF directory
     */
    suspend fun deleteFileFromSafDirectory(fileUri: Uri): Result<Boolean> = 
        withContext(Dispatchers.IO) {
        try {
            val file = DocumentFile.fromSingleUri(context, fileUri)
            if (file == null || !file.exists()) {
                return@withContext Result.success(false)
            }

            val deleted = file.delete()
            logger.i(TAG, "Deleted file from SAF directory: $fileUri, success: $deleted")
            return@withContext Result.success(deleted)
            
        } catch (e: Exception) {
            logger.e(TAG, "Failed to delete file from SAF directory", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Get directory info from SAF URI
     */
    fun getDirectoryInfo(safDirectoryUri: Uri): DirectoryInfo? {
        return try {
            val directory = DocumentFile.fromTreeUri(context, safDirectoryUri)
            if (directory == null || !directory.exists()) {
                return null
            }

            DirectoryInfo(
                uri = directory.uri,
                name = directory.name ?: "Unknown",
                canRead = directory.canRead(),
                canWrite = directory.canWrite()
            )
        } catch (e: Exception) {
            logger.e(TAG, "Failed to get directory info", e)
            null
        }
    }

    /**
     * Create a subdirectory in a SAF directory
     */
    suspend fun createSubdirectory(
        parentUri: Uri,
        directoryName: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val parent = DocumentFile.fromTreeUri(context, parentUri)
            if (parent == null || !parent.exists() || !parent.isDirectory) {
                return@withContext Result.failure(
                    IllegalArgumentException("Invalid parent directory URI")
                )
            }

            // Check if subdirectory already exists
            var subdir = parent.findFile(directoryName)
            if (subdir != null && subdir.isDirectory) {
                logger.d(TAG, "Subdirectory already exists: $directoryName")
                return@withContext Result.success(subdir.uri)
            }

            // Create new subdirectory
            subdir = parent.createDirectory(directoryName)
            if (subdir == null) {
                return@withContext Result.failure(
                    RuntimeException("Failed to create subdirectory")
                )
            }

            logger.i(TAG, "Created subdirectory: $directoryName")
            return@withContext Result.success(subdir.uri)
            
        } catch (e: Exception) {
            logger.e(TAG, "Failed to create subdirectory", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Data class for SAF file information
     */
    data class SafFileInfo(
        val uri: Uri,
        val name: String,
        val size: Long,
        val lastModified: Long,
        val mimeType: String?
    )

    /**
     * Data class for SAF directory information
     */
    data class DirectoryInfo(
        val uri: Uri,
        val name: String,
        val canRead: Boolean,
        val canWrite: Boolean
    )
}
