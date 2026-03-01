package com.obsidianbackup.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obsidianbackup.data.repository.SettingsRepository
import com.obsidianbackup.domain.usecase.RetentionEnforcementUseCase
import com.obsidianbackup.storage.BackupCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RetentionUiState(
    val retentionMode: String = "COUNT",    // "COUNT" | "DAYS" | "BOTH"
    val keepCount: Int = 10,                // max snapshots (0 = unlimited)
    val retentionDays: Int = 30,            // max age in days (0 = unlimited)
    val storageLimitMb: Int = 0,            // 0 = unlimited
    val currentSnapshotCount: Int = 0,
    val currentTotalSizeMb: Long = 0,
    val previewDeleteCount: Int = 0,
    val previewFreedMb: Long = 0,
    val isRunning: Boolean = false,
    val lastResult: String? = null,
    val isLoaded: Boolean = false
)

@HiltViewModel
class RetentionPoliciesViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupCatalog: BackupCatalog,
    private val retentionEnforcement: RetentionEnforcementUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RetentionUiState())
    val state: StateFlow<RetentionUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.retentionMode,
                settingsRepository.backupKeepCount,
                settingsRepository.backupRetentionDays,
                settingsRepository.storageLimitMb
            ) { mode, count, days, limitMb ->
                _state.update {
                    it.copy(
                        retentionMode = mode,
                        keepCount = count,
                        retentionDays = days,
                        storageLimitMb = limitMb,
                        isLoaded = true
                    )
                }
            }.collect()
        }

        // Live snapshot stats
        viewModelScope.launch {
            backupCatalog.getAllSnapshots().collect { snapshots ->
                val totalMb = snapshots.sumOf { it.totalSize } / 1024 / 1024
                _state.update {
                    it.copy(
                        currentSnapshotCount = snapshots.size,
                        currentTotalSizeMb = totalMb
                    )
                }
                refreshPreview()
            }
        }
    }

    fun setRetentionMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setRetentionMode(mode)
            refreshPreview()
        }
    }

    fun setKeepCount(count: Int) {
        viewModelScope.launch {
            settingsRepository.setBackupKeepCount(count)
            refreshPreview()
        }
    }

    fun setRetentionDays(days: Int) {
        viewModelScope.launch {
            settingsRepository.setBackupRetentionDays(days)
            refreshPreview()
        }
    }

    fun setStorageLimitMb(mb: Int) {
        viewModelScope.launch {
            settingsRepository.setStorageLimitMb(mb)
            refreshPreview()
        }
    }

    fun runNow() {
        viewModelScope.launch {
            _state.update { it.copy(isRunning = true, lastResult = null) }
            val result = retentionEnforcement.enforce()
            _state.update {
                it.copy(
                    isRunning = false,
                    lastResult = if (result.deletedCount == 0) "Nothing to prune."
                                 else "Deleted ${result.deletedCount} backup(s), freed ${result.freedBytes / 1024 / 1024} MB."
                )
            }
            refreshPreview()
        }
    }

    private fun refreshPreview() {
        viewModelScope.launch {
            val preview = retentionEnforcement.preview()
            _state.update {
                it.copy(
                    previewDeleteCount = preview.deletedCount,
                    previewFreedMb = preview.freedBytes / 1024 / 1024
                )
            }
        }
    }
}
