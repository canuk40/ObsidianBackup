// presentation/backups/BackupsViewModel.kt
package com.obsidianbackup.presentation.backups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.obsidianbackup.data.repository.CatalogRepository
import com.obsidianbackup.model.BackupSnapshot
import com.obsidianbackup.model.FeatureTier
import com.obsidianbackup.model.SnapshotId
import com.obsidianbackup.work.DeleteWorker
import com.obsidianbackup.work.RestoreWorker
import com.obsidianbackup.work.VerifyWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupsViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val workManager: WorkManager
) : ViewModel() {

    // FOSS build: all features unlocked
    val currentTier: StateFlow<FeatureTier> = MutableStateFlow(FeatureTier.FREE).asStateFlow()

    private val _state = MutableStateFlow(BackupsState())
    val state: StateFlow<BackupsState> = _state.asStateFlow()

    init {
        loadBackups()
    }

    private fun loadBackups() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                catalogRepository.getAllSnapshots().collect { snapshots ->
                    _state.update { 
                        it.copy(
                            snapshots = snapshots,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load backups"
                    )
                }
            }
        }
    }

    fun deleteSnapshot(id: SnapshotId) {
        val request = OneTimeWorkRequestBuilder<DeleteWorker>()
            .setInputData(workDataOf("snapshotId" to id.value))
            .build()
        workManager.enqueue(request)
    }

    fun restoreSnapshot(id: SnapshotId) {
        val request = OneTimeWorkRequestBuilder<RestoreWorker>()
            .setInputData(workDataOf("snapshotId" to id.value))
            .build()
        workManager.enqueue(request)
    }

    fun verifySnapshot(id: SnapshotId) {
        val request = OneTimeWorkRequestBuilder<VerifyWorker>()
            .setInputData(workDataOf("snapshotId" to id.value))
            .build()
        workManager.enqueue(request)
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

data class BackupsState(
    val snapshots: List<BackupSnapshot> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
