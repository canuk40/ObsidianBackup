// work/CloudSyncWorker.kt
package com.obsidianbackup.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.obsidianbackup.cloud.CloudSyncManager
import com.obsidianbackup.cloud.SyncPolicy
import com.obsidianbackup.cloud.RetryPolicy
import com.obsidianbackup.data.repository.SettingsRepository
import com.obsidianbackup.logging.ObsidianLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class CloudSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cloudSyncManager: CloudSyncManager,
    private val settingsRepository: SettingsRepository,
    private val logger: ObsidianLogger
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        logger.i(TAG, "Starting cloud sync work")

        return try {
            val policy = SyncPolicy(
                syncOnBackup = settingsRepository.syncOnBackup.first(),
                syncOnWifiOnly = settingsRepository.syncOnWifiOnly.first(),
                syncOnCharging = settingsRepository.syncOnCharging.first(),
                maxConcurrentSyncs = settingsRepository.maxConcurrentSyncs.first(),
                retryPolicy = RetryPolicy(
                    maxAttempts = settingsRepository.syncRetryMaxAttempts.first(),
                    initialDelayMs = settingsRepository.syncRetryInitialDelayMs.first().toLong(),
                    backoffMultiplier = settingsRepository.syncRetryBackoffMultiplier.first().toDouble()
                )
            )
            val result = cloudSyncManager.syncAllPending(policy)

            when (result) {
                is com.obsidianbackup.model.Result.Success -> {
                    logger.i(TAG, "Cloud sync completed successfully")
                    Result.success()
                }
                is com.obsidianbackup.model.Result.Error -> {
                    logger.e(TAG, "Cloud sync failed: ${result.message}", result.exception)
                    Result.retry()
                }
                is com.obsidianbackup.model.Result.Loading -> {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            logger.e(TAG, "Cloud sync worker failed", e)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "CloudSyncWorker"
    }
}
