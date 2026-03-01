// storage/MediaStoreHelper.kt
package com.obsidianbackup.storage

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.obsidianbackup.logging.ObsidianLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MediaStoreHelper - Manages backup exports to shared storage using MediaStore API
 * 
 * This class handles exporting backups to shared storage in a scoped storage
 * compliant way for Android 10+ devices.
 */
@Singleton
class MediaStoreHelper @Inject constructor(
    private val context: Context,
    private val logger: ObsidianLogger
) {
    
    companion object {
        private const val TAG = "MediaStoreHelper"
        val BACKUP_RELATIVE_PATH = "${Environment.DIRECTORY_DOCUMENTS}/ObsidianBackup"
        const val BACKUP_MIME_TYPE = "application/x-tar"
    }

    /**
     * Export a backup file to shared Documents folder using MediaStore
     * Available on Android 10+ (API 29+)
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun exportBackup(
        sourceFile: File,
        displayName: String,
        relativePath: String = BACKUP_RELATIVE_PATH
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists() || !sourceFile.canRead()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Source file does not exist or is not readable")
                )
            }

            val resolver: ContentResolver = context.contentResolver
            
            // Create content values for the new file
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, BACKUP_MIME_TYPE)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1) // Mark as pending during write
            }
            
            // Insert into MediaStore
            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = resolver.insert(collection, contentValues)
            
            if (itemUri == null) {
                return@withContext Result.failure(
                    RuntimeException("Failed to create MediaStore entry")
                )
            }

            try {
                // Write file content
                resolver.openOutputStream(itemUri)?.use { outputStream ->
                    sourceFile.inputStream().use { inputStream ->
                        val bytesWritten = inputStream.copyTo(outputStream)
                        logger.d(TAG, "Wrote $bytesWritten bytes to MediaStore")
                    }
                }

                // Mark as complete
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)

                logger.i(TAG, "Successfully exported backup to MediaStore: $displayName")
                return@withContext Result.success(itemUri)
                
            } catch (e: Exception) {
                // If write fails, delete the incomplete entry
                resolver.delete(itemUri, null, null)
                throw e
            }
            
        } catch (e: Exception) {
            logger.e(TAG, "Failed to export backup to MediaStore", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Import a backup from a MediaStore URI
     */
    suspend fun importBackup(
        sourceUri: Uri,
        destinationFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Ensure parent directory exists
            destinationFile.parentFile?.mkdirs()
            
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                destinationFile.outputStream().use { outputStream ->
                    val bytesRead = inputStream.copyTo(outputStream)
                    logger.d(TAG, "Imported $bytesRead bytes from MediaStore")
                }
            } ?: return@withContext Result.failure(
                RuntimeException("Failed to open input stream from URI")
            )

            logger.i(TAG, "Successfully imported backup to: ${destinationFile.absolutePath}")
            return@withContext Result.success(destinationFile)
            
        } catch (e: Exception) {
            logger.e(TAG, "Failed to import backup from MediaStore", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Query backups from MediaStore
     * Returns list of backup file URIs found in the ObsidianBackup folder
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun queryBackups(): Result<List<BackupMediaInfo>> = withContext(Dispatchers.IO) {
        try {
            val resolver: ContentResolver = context.contentResolver
            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.RELATIVE_PATH
            )
            
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("%ObsidianBackup%")
            val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            
            val backups = mutableListOf<BackupMediaInfo>()
            
            resolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val uri = Uri.withAppendedPath(collection, id.toString())
                    
                    backups.add(
                        BackupMediaInfo(
                            uri = uri,
                            displayName = cursor.getString(nameColumn),
                            size = cursor.getLong(sizeColumn),
                            dateModified = cursor.getLong(dateColumn) * 1000, // Convert to millis
                            relativePath = cursor.getString(pathColumn)
                        )
                    )
                }
            }
            
            logger.i(TAG, "Found ${backups.size} backups in MediaStore")
            return@withContext Result.success(backups)
            
        } catch (e: Exception) {
            logger.e(TAG, "Failed to query backups from MediaStore", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Delete a backup from MediaStore
     */
    suspend fun deleteBackup(uri: Uri): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val resolver: ContentResolver = context.contentResolver
            val deletedRows = resolver.delete(uri, null, null)
            
            val success = deletedRows > 0
            logger.i(TAG, "Deleted backup from MediaStore: $uri, success: $success")
            
            return@withContext Result.success(success)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to delete backup from MediaStore", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Check if MediaStore API is available (Android 10+)
     */
    fun isMediaStoreAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    /**
     * Data class representing a backup file in MediaStore
     */
    data class BackupMediaInfo(
        val uri: Uri,
        val displayName: String,
        val size: Long,
        val dateModified: Long,
        val relativePath: String
    )
}
