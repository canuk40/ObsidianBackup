package com.obsidianbackup.tasker

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.obsidianbackup.security.TaskerSecurityValidator
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * TaskerStatusProvider - ContentProvider for querying backup/restore status
 * 
 * Allows Tasker/MacroDroid to query operation status synchronously
 * without requiring callbacks.
 * 
 * Content URIs:
 * - content://com.obsidianbackup.tasker/status/{work_id} - Query work status
 * - content://com.obsidianbackup.tasker/backups - List recent backups
 * - content://com.obsidianbackup.tasker/latest - Get latest backup info
 */
class TaskerStatusProvider : ContentProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TaskerStatusProviderEntryPoint {
        fun securityValidator(): TaskerSecurityValidator
    }

    private lateinit var workManager: WorkManager
    private lateinit var securityValidator: TaskerSecurityValidator

    companion object {
        private const val TAG = "TaskerStatusProvider"
        const val AUTHORITY = "com.obsidianbackup.tasker"
        
        private const val STATUS = 1
        private const val BACKUPS = 2
        private const val LATEST = 3
        private const val SNAPSHOTS = 4
        private const val SNAPSHOT_BY_ID = 5

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "status/*", STATUS)
            addURI(AUTHORITY, "backups", BACKUPS)
            addURI(AUTHORITY, "latest", LATEST)
            addURI(AUTHORITY, "snapshots", SNAPSHOTS)
            addURI(AUTHORITY, "snapshots/*", SNAPSHOT_BY_ID)
        }

        // Column names for status query
        val STATUS_COLUMNS = arrayOf(
            "work_id",
            "state",
            "progress",
            "output_data",
            "run_attempt_count",
            "tags"
        )

        // Column names for backup listing
        val BACKUP_COLUMNS = arrayOf(
            "snapshot_id",
            "timestamp",
            "app_count",
            "total_size",
            "description",
            "status"
        )
    }

    override fun onCreate(): Boolean {
        // ContentProvider.onCreate() is called before Application.onCreate()
        // Delay WorkManager access until first use
        Log.d(TAG, "TaskerStatusProvider created (WorkManager will be initialized on first use)")
        return true
    }
    
    private fun ensureInitialized() {
        if (::workManager.isInitialized) return
        
        val context = context ?: return
        workManager = WorkManager.getInstance(context)
        
        // Get SecurityValidator from Hilt
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            TaskerStatusProviderEntryPoint::class.java
        )
        securityValidator = entryPoint.securityValidator()
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        ensureInitialized()  // Initialize on first use
        Log.d(TAG, "Query: $uri")

        // Security check
        val callingPackage = callingPackage ?: "unknown"
        if (!securityValidator.isAuthorizedPackage(callingPackage)) {
            Log.w(TAG, "Unauthorized query from: $callingPackage")
            return null
        }

        return when (uriMatcher.match(uri)) {
            STATUS -> queryWorkStatus(uri)
            BACKUPS -> queryBackups()
            LATEST -> queryLatestBackup()
            SNAPSHOTS -> querySnapshots()
            SNAPSHOT_BY_ID -> querySnapshotById(uri)
            else -> {
                Log.w(TAG, "Unknown URI: $uri")
                null
            }
        }
    }

    private fun queryWorkStatus(uri: Uri): Cursor? {
        val workId = uri.lastPathSegment ?: return null
        
        try {
            val uuid = UUID.fromString(workId)
            val workInfoFuture = workManager.getWorkInfoById(uuid)
            val workInfo = workInfoFuture.get(3, TimeUnit.SECONDS)

            if (workInfo == null) {
                Log.w(TAG, "Work not found: $workId")
                return null
            }

            val cursor = MatrixCursor(STATUS_COLUMNS)
            cursor.addRow(arrayOf(
                workId,
                workInfo.state.name,
                workInfo.progress.getInt("progress", 0),
                workInfo.outputData.toString(),
                workInfo.runAttemptCount,
                workInfo.tags.joinToString(",")
            ))

            Log.d(TAG, "Returning work status: ${workInfo.state}")
            return cursor
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query work status", e)
            return null
        }
    }

    private fun queryBackups(): Cursor? {
        val cursor = MatrixCursor(BACKUP_COLUMNS)
        
        try {
            // Query all backup work
            val workInfos = workManager.getWorkInfosByTag("tasker_backup")
                .get(3, TimeUnit.SECONDS)

            for (workInfo in workInfos.take(20)) { // Limit to 20 most recent
                val outputData = workInfo.outputData
                cursor.addRow(arrayOf(
                    outputData.getString("snapshot_id") ?: "",
                    outputData.getLong("timestamp", 0L),
                    outputData.getInt("app_count", 0),
                    outputData.getLong("total_size", 0L),
                    outputData.getString("description") ?: "",
                    workInfo.state.name
                ))
            }

            Log.d(TAG, "Returning ${cursor.count} backups")
            return cursor
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query backups", e)
            return cursor
        }
    }

    private fun queryLatestBackup(): Cursor? {
        val cursor = MatrixCursor(BACKUP_COLUMNS)
        
        try {
            val workInfos = workManager.getWorkInfosByTag("tasker_backup")
                .get(3, TimeUnit.SECONDS)

            // Get the most recent completed backup
            val latest = workInfos.firstOrNull { it.state == WorkInfo.State.SUCCEEDED }

            if (latest != null) {
                val outputData = latest.outputData
                cursor.addRow(arrayOf(
                    outputData.getString("snapshot_id") ?: "",
                    outputData.getLong("timestamp", 0L),
                    outputData.getInt("app_count", 0),
                    outputData.getLong("total_size", 0L),
                    outputData.getString("description") ?: "",
                    latest.state.name
                ))
            }

            Log.d(TAG, "Returning latest backup")
            return cursor
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query latest backup", e)
            return cursor
        }
    }

    private fun querySnapshots(): Cursor? {
        val cursor = MatrixCursor(BACKUP_COLUMNS)
        // This would query from a database in production
        // For now, return empty cursor
        Log.d(TAG, "Querying all snapshots")
        return cursor
    }

    private fun querySnapshotById(uri: Uri): Cursor? {
        val snapshotId = uri.lastPathSegment ?: return null
        val cursor = MatrixCursor(BACKUP_COLUMNS)
        
        // This would query from a database in production
        Log.d(TAG, "Querying snapshot: $snapshotId")
        return cursor
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            STATUS -> "vnd.android.cursor.item/vnd.$AUTHORITY.status"
            BACKUPS, SNAPSHOTS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.backup"
            LATEST, SNAPSHOT_BY_ID -> "vnd.android.cursor.item/vnd.$AUTHORITY.backup"
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        // Not supported
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        // Not supported
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        // Not supported
        return 0
    }
}
