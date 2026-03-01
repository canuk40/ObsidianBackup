package com.obsidianbackup.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.obsidianbackup.model.BackupId
import com.obsidianbackup.storage.BackupCatalog
import com.obsidianbackup.storage.FileSystemManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * DeleteWorker - Deletes a snapshot from the catalog and filesystem.
 */
@HiltWorker
class DeleteWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val catalog: BackupCatalog,
    private val fileSystemManager: FileSystemManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val snapshotId = inputData.getString("snapshotId") ?: return Result.failure()
        Log.i(TAG, "Deleting snapshot: $snapshotId")

        return try {
            // Remove files from disk first
            val filesDeleted = fileSystemManager.deleteSnapshotDirectory(snapshotId)
            if (!filesDeleted) {
                Log.w(TAG, "Snapshot directory not found or already deleted for $snapshotId")
            }
            // Remove record from catalog
            catalog.deleteSnapshot(snapshotId)
            Log.i(TAG, "Snapshot deleted successfully: $snapshotId")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Deletion failed for snapshot $snapshotId", e)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "DeleteWorker"
    }
}
