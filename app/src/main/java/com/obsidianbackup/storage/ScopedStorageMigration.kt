// storage/ScopedStorageMigration.kt
package com.obsidianbackup.storage

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.obsidianbackup.logging.ObsidianLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ScopedStorageMigration - Handles migration from legacy storage to scoped storage
 * 
 * This class manages the one-time migration of backup data from legacy external storage
 * locations (that required MANAGE_EXTERNAL_STORAGE) to app-private storage locations
 * that comply with scoped storage requirements.
 */
@Singleton
class ScopedStorageMigration @Inject constructor(
    private val context: Context,
    private val fileSystemManager: FileSystemManager,
    private val logger: ObsidianLogger
) {
    
    companion object {
        private const val TAG = "ScopedStorageMigration"
        private const val PREFS_NAME = "scoped_storage_migration"
        private const val KEY_MIGRATION_COMPLETED = "migration_completed"
        private const val KEY_MIGRATION_VERSION = "migration_version"
        private const val CURRENT_MIGRATION_VERSION = 1
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Check if migration has been completed
     */
    fun isMigrationCompleted(): Boolean {
        val completed = prefs.getBoolean(KEY_MIGRATION_COMPLETED, false)
        val version = prefs.getInt(KEY_MIGRATION_VERSION, 0)
        return completed && version >= CURRENT_MIGRATION_VERSION
    }

    /**
     * Mark migration as completed
     */
    private fun markMigrationCompleted() {
        prefs.edit().apply {
            putBoolean(KEY_MIGRATION_COMPLETED, true)
            putInt(KEY_MIGRATION_VERSION, CURRENT_MIGRATION_VERSION)
            apply()
        }
        logger.i(TAG, "Migration marked as completed")
    }

    /**
     * Perform full migration check and execution
     */
    suspend fun performMigrationIfNeeded(): MigrationResult = withContext(Dispatchers.IO) {
        try {
            // Check if migration is needed
            if (isMigrationCompleted()) {
                logger.i(TAG, "Migration already completed, skipping")
                return@withContext MigrationResult.AlreadyCompleted
            }

            // For Android 10+ (API 29+), scoped storage is mandatory
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                logger.i(TAG, "Pre-Android 10 device, no migration needed")
                markMigrationCompleted()
                return@withContext MigrationResult.NotRequired
            }

            logger.i(TAG, "Starting scoped storage migration...")
            
            // Find legacy backup locations
            val legacyLocations = findLegacyBackupLocations()
            
            if (legacyLocations.isEmpty()) {
                logger.i(TAG, "No legacy backup locations found")
                markMigrationCompleted()
                return@withContext MigrationResult.NoDataToMigrate
            }

            // Migrate data from legacy locations
            val migratedBytes = migrateLegacyBackups(legacyLocations)
            
            // Mark as completed
            markMigrationCompleted()
            
            logger.i(TAG, "Migration completed successfully, migrated $migratedBytes bytes")
            return@withContext MigrationResult.Success(
                migratedFiles = legacyLocations.size,
                migratedBytes = migratedBytes
            )
            
        } catch (e: Exception) {
            logger.e(TAG, "Migration failed", e)
            return@withContext MigrationResult.Failed(e.message ?: "Unknown error")
        }
    }

    /**
     * Find legacy backup locations that might exist
     */
    private fun findLegacyBackupLocations(): List<File> {
        val legacyLocations = mutableListOf<File>()
        
        // Common legacy locations
        val possibleLocations = listOf(
            // Public external storage (deprecated)
            File(context.getExternalFilesDir(null)?.parentFile?.parentFile, "ObsidianBackup"),
            File("/sdcard/ObsidianBackup"),
            File("/storage/emulated/0/ObsidianBackup"),
            
            // App-specific legacy locations
            File(context.filesDir.parentFile, "external/backups"),
            File(context.filesDir, "legacy_backups")
        )
        
        for (location in possibleLocations) {
            if (location.exists() && location.isDirectory) {
                val files = location.listFiles()
                if (files != null && files.isNotEmpty()) {
                    logger.d(TAG, "Found legacy location: ${location.absolutePath} with ${files.size} items")
                    legacyLocations.add(location)
                }
            }
        }
        
        return legacyLocations
    }

    /**
     * Migrate backups from legacy locations to new scoped storage location
     */
    private suspend fun migrateLegacyBackups(legacyLocations: List<File>): Long = 
        withContext(Dispatchers.IO) {
        var totalBytes = 0L
        val newBackupDir = fileSystemManager.getBackupDirectory()
        
        for (legacyLocation in legacyLocations) {
            try {
                val files = legacyLocation.listFiles() ?: continue
                
                for (file in files) {
                    if (file.isDirectory) {
                        // This might be a snapshot directory
                        val snapshotId = file.name
                        val newSnapshotDir = File(newBackupDir, snapshotId)
                        
                        if (newSnapshotDir.exists()) {
                            logger.d(TAG, "Snapshot already exists in new location: $snapshotId")
                            continue
                        }
                        
                        // Copy entire snapshot directory
                        val copiedBytes = copyDirectoryRecursively(file, newSnapshotDir)
                        totalBytes += copiedBytes
                        logger.i(TAG, "Migrated snapshot: $snapshotId ($copiedBytes bytes)")
                    } else {
                        // Individual file - copy to new location
                        val newFile = File(newBackupDir, file.name)
                        if (!newFile.exists()) {
                            file.copyTo(newFile, overwrite = false)
                            totalBytes += file.length()
                            logger.d(TAG, "Migrated file: ${file.name} (${file.length()} bytes)")
                        }
                    }
                }
                
                // Optionally: Delete legacy location after successful migration
                // Commented out for safety - users can manually delete
                // legacyLocation.deleteRecursively()
                
            } catch (e: Exception) {
                logger.e(TAG, "Failed to migrate from: ${legacyLocation.absolutePath}", e)
            }
        }
        
        return@withContext totalBytes
    }

    /**
     * Copy directory recursively
     */
    private fun copyDirectoryRecursively(source: File, destination: File): Long {
        var totalBytes = 0L
        
        if (!destination.exists()) {
            destination.mkdirs()
        }
        
        source.listFiles()?.forEach { file ->
            val newFile = File(destination, file.name)
            
            if (file.isDirectory) {
                totalBytes += copyDirectoryRecursively(file, newFile)
            } else {
                try {
                    file.copyTo(newFile, overwrite = false)
                    totalBytes += file.length()
                } catch (e: Exception) {
                    logger.e(TAG, "Failed to copy file: ${file.absolutePath}", e)
                }
            }
        }
        
        return totalBytes
    }

    /**
     * Get migration status information
     */
    fun getMigrationStatus(): MigrationStatus {
        val completed = isMigrationCompleted()
        val version = prefs.getInt(KEY_MIGRATION_VERSION, 0)
        
        return MigrationStatus(
            isCompleted = completed,
            migrationVersion = version,
            currentVersion = CURRENT_MIGRATION_VERSION,
            needsMigration = !completed || version < CURRENT_MIGRATION_VERSION
        )
    }

    /**
     * Reset migration status (for testing or troubleshooting)
     */
    fun resetMigrationStatus() {
        prefs.edit().apply {
            remove(KEY_MIGRATION_COMPLETED)
            remove(KEY_MIGRATION_VERSION)
            apply()
        }
        logger.w(TAG, "Migration status reset")
    }

    /**
     * Migration result sealed class
     */
    sealed class MigrationResult {
        object AlreadyCompleted : MigrationResult()
        object NotRequired : MigrationResult()
        object NoDataToMigrate : MigrationResult()
        data class Success(val migratedFiles: Int, val migratedBytes: Long) : MigrationResult()
        data class Failed(val error: String) : MigrationResult()
    }

    /**
     * Migration status data class
     */
    data class MigrationStatus(
        val isCompleted: Boolean,
        val migrationVersion: Int,
        val currentVersion: Int,
        val needsMigration: Boolean
    )
}
