package com.obsidianbackup.work

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.obsidianbackup.domain.restore.RestoreRequest
import com.obsidianbackup.domain.usecase.RestoreAppsUseCase
import com.obsidianbackup.error.Result as ErrorResult
import com.obsidianbackup.model.AppId
import com.obsidianbackup.model.BackupId
import com.obsidianbackup.tasker.TaskerIntegration
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * RestoreWorker - Restores a snapshot using the transactional restore engine.
 */
@HiltWorker
class RestoreWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val restoreAppsUseCase: RestoreAppsUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val snapshotId = inputData.getString("snapshotId") ?: return Result.failure()
        val appIdsJson = inputData.getString("appIds") ?: "[]"

        Log.i(TAG, "Restoring snapshot: $snapshotId")

        val appIds = try {
            Json.decodeFromString<List<String>>(appIdsJson).map { AppId(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse appIds JSON", e)
            return Result.failure()
        }

        // Broadcast: restore started
        applicationContext.sendBroadcast(Intent(TaskerIntegration.EVENT_RESTORE_COMPLETE).apply {
            putExtra(TaskerIntegration.EXTRA_SNAPSHOT_ID, snapshotId)
            putExtra(TaskerIntegration.EXTRA_STATUS, "in_progress")
        })

        val restoreRequest = RestoreRequest(
            backupId = BackupId(snapshotId),
            appIds = appIds.takeIf { it.isNotEmpty() }
        )

        return try {
            when (val result = restoreAppsUseCase(RestoreAppsUseCase.Params(restoreRequest))) {
                is ErrorResult.Success -> {
                    Log.i(TAG, "Restore completed for snapshot $snapshotId")
                    applicationContext.sendBroadcast(Intent(TaskerIntegration.EVENT_RESTORE_COMPLETE).apply {
                        putExtra(TaskerIntegration.EXTRA_SNAPSHOT_ID, snapshotId)
                        putExtra(TaskerIntegration.EXTRA_APPS_BACKED_UP, appIds.size)
                        putExtra(TaskerIntegration.EXTRA_RESULT_CODE, TaskerIntegration.RESULT_SUCCESS)
                    })
                    Result.success(workDataOf("snapshotId" to snapshotId))
                }
                is ErrorResult.Error -> {
                    Log.e(TAG, "Restore failed for snapshot $snapshotId: ${result.error.message}")
                    applicationContext.sendBroadcast(Intent(TaskerIntegration.EVENT_RESTORE_FAILED).apply {
                        putExtra(TaskerIntegration.EXTRA_SNAPSHOT_ID, snapshotId)
                        putExtra(TaskerIntegration.EXTRA_MESSAGE, result.error.message)
                        putExtra(TaskerIntegration.EXTRA_RESULT_CODE, TaskerIntegration.RESULT_FAILURE)
                    })
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Restore threw exception for snapshot $snapshotId", e)
            applicationContext.sendBroadcast(Intent(TaskerIntegration.EVENT_RESTORE_FAILED).apply {
                putExtra(TaskerIntegration.EXTRA_SNAPSHOT_ID, snapshotId)
                putExtra(TaskerIntegration.EXTRA_MESSAGE, e.message)
                putExtra(TaskerIntegration.EXTRA_RESULT_CODE, TaskerIntegration.RESULT_FAILURE)
            })
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "RestoreWorker"
    }
}
