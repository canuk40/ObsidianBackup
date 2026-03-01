// presentation/apps/AppsViewModel.kt
package com.obsidianbackup.presentation.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obsidianbackup.domain.usecase.BackupAppsUseCase
import com.obsidianbackup.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val backupAppsUseCase: BackupAppsUseCase
) : ViewModel() {

    // FOSS build: all features unlocked, no subscription needed
    val currentTier: StateFlow<FeatureTier> = MutableStateFlow(FeatureTier.FREE).asStateFlow()

    private val _state = MutableStateFlow<AppsState>(AppsState.Idle)
    val state: StateFlow<AppsState> = _state.asStateFlow()

    fun backupApps(
        appIds: List<AppId>,
        components: Set<BackupComponent> = setOf(BackupComponent.APK, BackupComponent.DATA)
    ) {
        viewModelScope.launch {
            _state.value = AppsState.BackingUp(appIds.size, 0)
            
            val request = BackupRequest(
                appIds = appIds,
                components = components,
                incremental = false,
                compressionLevel = 6,
                encryptionEnabled = false
            )
            
            val result = backupAppsUseCase(request)
            
            _state.value = when (result) {
                is BackupResult.Success -> AppsState.BackupSuccess(
                    snapshotId = result.snapshotId,
                    appsBackedUp = result.appsBackedUp.size,
                    totalSize = result.totalSize,
                    duration = result.duration
                )
                is BackupResult.PartialSuccess -> AppsState.BackupPartialSuccess(
                    snapshotId = result.snapshotId,
                    appsBackedUp = result.appsBackedUp.size,
                    appsFailed = result.appsFailed.size,
                    errors = result.errors
                )
                is BackupResult.Failure -> AppsState.BackupError(result.reason)
            }
        }
    }
    
    fun resetState() {
        _state.value = AppsState.Idle
    }
}

sealed class AppsState {
    object Idle : AppsState()
    data class BackingUp(val totalApps: Int, val currentProgress: Int) : AppsState()
    data class BackupSuccess(
        val snapshotId: SnapshotId,
        val appsBackedUp: Int,
        val totalSize: Long,
        val duration: Long
    ) : AppsState()
    data class BackupPartialSuccess(
        val snapshotId: SnapshotId,
        val appsBackedUp: Int,
        val appsFailed: Int,
        val errors: List<String>
    ) : AppsState()
    data class BackupError(val message: String) : AppsState()
}
