// presentation/backup/BackupViewModel.kt
package com.obsidianbackup.presentation.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obsidianbackup.domain.usecase.BackupAppsUseCase
import com.obsidianbackup.domain.usecase.GetInstalledAppsUseCase
import com.obsidianbackup.error.Result
import com.obsidianbackup.error.ObsidianError
import com.obsidianbackup.model.BackupRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupAppsUseCase: BackupAppsUseCase,
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(BackupState())
    val state: StateFlow<BackupState> = _state.asStateFlow()
    
    private var backupJob: kotlinx.coroutines.Job? = null

    fun processIntent(intent: BackupIntent) {
        when (intent) {
            is BackupIntent.SelectApp -> selectApp(intent.appId)
            is BackupIntent.UnselectApp -> unselectApp(intent.appId)
            BackupIntent.StartBackup -> startBackup()
            BackupIntent.CancelBackup -> cancelBackup()
        }
    }

    private fun selectApp(appId: com.obsidianbackup.model.AppId) {
        _state.update { it.copy(selectedApps = it.selectedApps + appId) }
    }

    private fun unselectApp(appId: com.obsidianbackup.model.AppId) {
        _state.update { it.copy(selectedApps = it.selectedApps - appId) }
    }

    private fun startBackup() {
        // Cancel any existing backup operation
        backupJob?.cancel()
        
        backupJob = viewModelScope.launch {
            _state.update { it.copy(backupProgress = BackupProgress.InProgress) }

            val result = backupAppsUseCase(
                BackupRequest(
                    appIds = _state.value.selectedApps.toList()
                )
            )

            _state.update {
                when (result) {
                    is com.obsidianbackup.model.BackupResult.Success -> it.copy(
                        backupProgress = BackupProgress.Completed(result)
                    )
                    is com.obsidianbackup.model.BackupResult.Failure -> it.copy(
                        backupProgress = BackupProgress.Failed(result.reason),
                        error = result.reason
                    )
                    is com.obsidianbackup.model.BackupResult.PartialSuccess -> it.copy(
                        backupProgress = BackupProgress.Completed(result)
                    )
                }
            }
        }
    }

    private fun cancelBackup() {
        backupJob?.cancel()
        backupJob = null
        _state.update { it.copy(backupProgress = BackupProgress.Idle) }
    }
    
    override fun onCleared() {
        super.onCleared()
        backupJob?.cancel()
    }
}
