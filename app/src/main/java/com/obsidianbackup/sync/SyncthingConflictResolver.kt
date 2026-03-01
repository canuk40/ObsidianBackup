// sync/SyncthingConflictResolver.kt
package com.obsidianbackup.sync

import android.content.Context
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.sync.models.ConflictResolution
import com.obsidianbackup.sync.models.SyncConflict
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Conflict resolver for Syncthing synchronization conflicts
 * 
 * Handles manual reconciliation of file conflicts that occur during sync.
 */
@Singleton
class SyncthingConflictResolver @Inject constructor(
    private val context: Context,
    private val logger: ObsidianLogger
) {
    companion object {
        private const val TAG = "SyncthingConflictResolver"
        private const val CONFLICT_SUFFIX = ".sync-conflict"
    }

    /**
     * Resolve a sync conflict based on user's choice
     */
    suspend fun resolve(
        conflict: SyncConflict,
        resolution: ConflictResolution
    ) = withContext(Dispatchers.IO) {
        logger.i(TAG, "Resolving conflict for ${conflict.filePath} with $resolution")
        
        val originalFile = File(conflict.filePath)
        val conflictFile = File("${conflict.filePath}$CONFLICT_SUFFIX")
        
        when (resolution) {
            ConflictResolution.KEEP_LOCAL -> {
                // Keep local version, delete remote (conflict) version
                if (conflictFile.exists()) {
                    conflictFile.delete()
                    logger.i(TAG, "Kept local version, deleted remote")
                }
            }
            
            ConflictResolution.KEEP_REMOTE -> {
                // Keep remote (conflict) version, replace local
                if (conflictFile.exists() && originalFile.exists()) {
                    originalFile.delete()
                    conflictFile.renameTo(originalFile)
                    logger.i(TAG, "Kept remote version, replaced local")
                }
            }
            
            ConflictResolution.KEEP_BOTH -> {
                // Keep both versions with different names
                if (conflictFile.exists()) {
                    val timestamp = System.currentTimeMillis()
                    val newName = "${originalFile.nameWithoutExtension}_$timestamp.${originalFile.extension}"
                    val newFile = File(originalFile.parent, newName)
                    conflictFile.renameTo(newFile)
                    logger.i(TAG, "Kept both versions: ${originalFile.name} and ${newFile.name}")
                }
            }
            
            ConflictResolution.MERGE_MANUAL -> {
                // Manual merge - create backup of both for user to merge
                if (conflictFile.exists()) {
                    val localBackup = File("${originalFile.absolutePath}.local")
                    val remoteBackup = File("${originalFile.absolutePath}.remote")
                    
                    originalFile.copyTo(localBackup, overwrite = true)
                    conflictFile.copyTo(remoteBackup, overwrite = true)
                    
                    logger.i(TAG, "Created backups for manual merge: .local and .remote")
                }
            }
        }
    }

    /**
     * Get conflict details for UI display
     */
    suspend fun getConflictDetails(conflict: SyncConflict): ConflictDetails = 
        withContext(Dispatchers.IO) {
            val originalFile = File(conflict.filePath)
            val conflictFile = File("${conflict.filePath}$CONFLICT_SUFFIX")
            
            ConflictDetails(
                conflict = conflict,
                localExists = originalFile.exists(),
                remoteExists = conflictFile.exists(),
                canAutoResolve = originalFile.exists() && conflictFile.exists(),
                localPreview = if (originalFile.exists()) {
                    getFilePreview(originalFile)
                } else null,
                remotePreview = if (conflictFile.exists()) {
                    getFilePreview(conflictFile)
                } else null
            )
        }

    /**
     * Get a preview of file contents (for text files)
     */
    private fun getFilePreview(file: File): String? {
        return try {
            if (isTextFile(file)) {
                file.readLines().take(10).joinToString("\n")
            } else {
                null
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to read file preview", e)
            null
        }
    }

    /**
     * Check if file is a text file
     */
    private fun isTextFile(file: File): Boolean {
        val textExtensions = setOf("txt", "md", "json", "xml", "java", "kt", "py", "js", "html", "css")
        return textExtensions.contains(file.extension.lowercase())
    }
}

/**
 * Detailed conflict information for UI
 */
data class ConflictDetails(
    val conflict: SyncConflict,
    val localExists: Boolean,
    val remoteExists: Boolean,
    val canAutoResolve: Boolean,
    val localPreview: String?,
    val remotePreview: String?
)
