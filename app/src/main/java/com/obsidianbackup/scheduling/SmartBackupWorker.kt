// scheduling/SmartBackupWorker.kt
package com.obsidianbackup.scheduling

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.obsidianbackup.domain.backup.BackupOrchestrator
import com.obsidianbackup.data.repository.BackupProfileRepository
import com.obsidianbackup.data.repository.PreferencesRepository
import com.obsidianbackup.model.BackupProfileId
import com.obsidianbackup.model.BackupResult
import com.obsidianbackup.model.BackupEvent
import com.obsidianbackup.model.DeviceState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * WorkManager worker for executing smart scheduled backups
 */
@HiltWorker
class SmartBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupOrchestrator: BackupOrchestrator,
    private val profileRepository: BackupProfileRepository,
    private val preferencesRepository: PreferencesRepository,
    private val smartScheduler: SmartScheduler
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "SmartBackupWorker"
        const val KEY_PROFILE_ID = "profile_id"
        const val KEY_REASON = "reason"
        const val KEY_SUCCESS_COUNT = "success_count"
        const val KEY_FAILURE_COUNT = "failure_count"
        const val KEY_TOTAL_SIZE = "total_size"
        const val KEY_ERROR = "error"
    }
    
    override suspend fun doWork(): Result {
        return try {
            val profileIdString = inputData.getString(KEY_PROFILE_ID)
            val reason = inputData.getString(KEY_REASON) ?: "Smart scheduled backup"
            
            Timber.tag(TAG).d("Starting smart backup: $reason")
            
            if (profileIdString == null) {
                Timber.tag(TAG).e("No profile ID provided")
                return Result.failure(
                    workDataOf(KEY_ERROR to "Missing profile ID")
                )
            }
            
            val profileId = BackupProfileId(profileIdString)
            
            // Load backup profile from repository
            val profile = profileRepository.getProfile(profileId)
            
            if (profile == null) {
                Timber.tag(TAG).e("Profile not found: $profileIdString")
                return Result.failure(
                    workDataOf(KEY_ERROR to "Profile not found: $profileIdString")
                )
            }
            
            Timber.tag(TAG).d("Loaded profile: ${profile.name}, apps: ${profile.appIds.size}")
            
            // Check if profile is enabled
            if (!profile.isEnabled) {
                Timber.tag(TAG).w("Profile disabled, skipping: ${profile.name}")
                return Result.success(
                    workDataOf(
                        KEY_SUCCESS_COUNT to 0,
                        KEY_FAILURE_COUNT to 0,
                        KEY_TOTAL_SIZE to 0L
                    )
                )
            }
            
            // Execute backup for the profile
            val backupStartTime = System.currentTimeMillis()
            val backupResult = backupOrchestrator.executeProfileBackup(profile)
            val backupDurationMs = System.currentTimeMillis() - backupStartTime
            
            // Update profile's last backup timestamp
            profileRepository.updateLastBackupTimestamp(
                profileId,
                System.currentTimeMillis()
            )
            
            // Record event for ML learning
            val now = System.currentTimeMillis()
            val cal = java.util.Calendar.getInstance()
            val event = BackupEvent(
                timestamp = now,
                duration = java.time.Duration.ofMillis(backupDurationMs),
                success = backupResult is BackupResult.Success,
                deviceState = DeviceState(
                    timestamp = now,
                    batteryLevel = 100,
                    isCharging = false,
                    isOnWifi = false,
                    screenOn = false,
                    hourOfDay = cal.get(java.util.Calendar.HOUR_OF_DAY),
                    dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
                ),
                appCount = profile.appIds.size,
                dataSize = 0L
            )
            try { smartScheduler.recordBackupEvent(event) }
            catch (e: Exception) { Timber.tag(TAG).w(e, "Failed to record ML event") }
            
            // Reschedule next backup
            val config = preferencesRepository.getSmartSchedulingConfig().first()
            smartScheduler.scheduleSmartBackup(config)
            
            // Process result
            when (backupResult) {
                is BackupResult.Success -> {
                    Timber.tag(TAG).i("Backup completed successfully: ${backupResult.appsBackedUp.size} apps")
                    Result.success(
                        workDataOf(
                            KEY_SUCCESS_COUNT to backupResult.appsBackedUp.size,
                            KEY_FAILURE_COUNT to 0,
                            KEY_TOTAL_SIZE to backupResult.totalSize
                        )
                    )
                }
                is BackupResult.PartialSuccess -> {
                    Timber.tag(TAG).w("Backup partially completed: ${backupResult.appsBackedUp.size} success, ${backupResult.appsFailed.size} failed")
                    Result.success(
                        workDataOf(
                            KEY_SUCCESS_COUNT to backupResult.appsBackedUp.size,
                            KEY_FAILURE_COUNT to backupResult.appsFailed.size,
                            KEY_TOTAL_SIZE to backupResult.totalSize
                        )
                    )
                }
                is BackupResult.Failure -> {
                    Timber.tag(TAG).e("Backup failed: ${backupResult.reason}")
                    Result.failure(
                        workDataOf(
                            KEY_ERROR to backupResult.reason,
                            KEY_FAILURE_COUNT to backupResult.appsFailed.size
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Smart backup worker failed")
            Result.retry()
        }
    }
}
