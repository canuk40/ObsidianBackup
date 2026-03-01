package com.obsidianbackup.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.obsidianbackup.model.BackupId
import com.obsidianbackup.verification.MerkleVerificationEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * VerifyWorker - Verifies backup integrity using Merkle tree checksums.
 */
@HiltWorker
class VerifyWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val verificationEngine: MerkleVerificationEngine
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val snapshotId = inputData.getString("snapshotId") ?: return Result.failure()
        Log.i(TAG, "Verifying snapshot: $snapshotId")

        return try {
            val result = verificationEngine.verifySnapshot(BackupId(snapshotId))
            if (result.allValid) {
                Log.i(TAG, "Snapshot verified OK: $snapshotId (${result.filesChecked} files)")
                Result.success(workDataOf("filesChecked" to result.filesChecked))
            } else {
                Log.w(TAG, "Snapshot verification FAILED: $snapshotId — ${result.corruptedFiles.size} corrupted file(s)")
                Result.failure(workDataOf(
                    "corruptedCount" to result.corruptedFiles.size,
                    "snapshotId" to snapshotId
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Verification failed for snapshot $snapshotId", e)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "VerifyWorker"
    }
}
