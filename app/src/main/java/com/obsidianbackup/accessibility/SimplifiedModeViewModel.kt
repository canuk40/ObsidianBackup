package com.obsidianbackup.accessibility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.obsidianbackup.data.repository.AppRepository
import com.obsidianbackup.domain.usecase.BackupAppsUseCase
import com.obsidianbackup.model.AppId
import com.obsidianbackup.model.BackupRequest
import com.obsidianbackup.model.BackupResult
import com.obsidianbackup.work.RestoreWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for simplified mode designed for elderly users and accessibility.
 * Features:
 * - Large touch targets (minimum 48x48 dp)
 * - High contrast colors
 * - Simple language
 * - Reduced cognitive load
 * - Clear visual hierarchy
 */
@HiltViewModel
class SimplifiedModeViewModel @Inject constructor(
    private val backupAppsUseCase: BackupAppsUseCase,
    private val appRepository: AppRepository,
    private val workManager: WorkManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SimplifiedUiState())
    val uiState: StateFlow<SimplifiedUiState> = _uiState.asStateFlow()
    
    private val _simplifiedModeEnabled = MutableStateFlow(false)
    val simplifiedModeEnabled: StateFlow<Boolean> = _simplifiedModeEnabled.asStateFlow()
    
    /**
     * Enable or disable simplified mode
     */
    fun setSimplifiedMode(enabled: Boolean) {
        _simplifiedModeEnabled.value = enabled
    }
    
    /**
     * Perform backup with simple confirmation
     */
    fun performBackup() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isBackupInProgress = true,
                lastAction = "Starting backup..."
            )

            try {
                val apps = appRepository.scanInstalledApps(includeSystemApps = false)
                val request = BackupRequest(appIds = apps.map { AppId(it.packageName) })
                val result = backupAppsUseCase(request)

                _uiState.value = _uiState.value.copy(
                    isBackupInProgress = false,
                    lastAction = when (result) {
                        is BackupResult.Success -> "Backup completed successfully!"
                        is BackupResult.Failure -> "Backup failed: ${result.reason}"
                        else -> "Backup finished."
                    },
                    lastBackupTime = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBackupInProgress = false,
                    lastAction = "Backup failed: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Perform restore with simple confirmation.
     * @param snapshotId ID of the snapshot to restore; null restores the latest.
     */
    fun performRestore(snapshotId: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRestoreInProgress = true,
                lastAction = "Starting restore..."
            )

            if (snapshotId == null) {
                _uiState.value = _uiState.value.copy(
                    isRestoreInProgress = false,
                    lastAction = "No backup selected to restore."
                )
                return@launch
            }

            val request = OneTimeWorkRequestBuilder<RestoreWorker>()
                .setInputData(workDataOf("snapshotId" to snapshotId))
                .build()

            workManager.enqueue(request)

            workManager.getWorkInfoByIdLiveData(request.id).observeForever { info ->
                when (info?.state?.name) {
                    "SUCCEEDED" -> _uiState.value = _uiState.value.copy(
                        isRestoreInProgress = false,
                        lastAction = "Restore completed successfully!",
                        lastRestoreTime = System.currentTimeMillis()
                    )
                    "FAILED" -> _uiState.value = _uiState.value.copy(
                        isRestoreInProgress = false,
                        lastAction = "Restore failed. Please try again."
                    )
                    else -> Unit
                }
            }
        }
    }
    
    /**
     * View backups list
     */
    fun viewBackups() {
        _uiState.value = _uiState.value.copy(
            showBackupsList = true
        )
    }
    
    /**
     * Dismiss backups list
     */
    fun dismissBackupsList() {
        _uiState.value = _uiState.value.copy(
            showBackupsList = false
        )
    }
}

/**
 * UI state for simplified mode
 */
data class SimplifiedUiState(
    val isBackupInProgress: Boolean = false,
    val isRestoreInProgress: Boolean = false,
    val showBackupsList: Boolean = false,
    val lastAction: String = "Welcome! Tap a button to get started.",
    val lastBackupTime: Long? = null,
    val lastRestoreTime: Long? = null
)
