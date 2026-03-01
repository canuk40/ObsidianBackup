// presentation/dashboard/DashboardViewModel.kt
package com.obsidianbackup.presentation.dashboard

import com.obsidianbackup.community.OnboardingManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obsidianbackup.data.repository.CatalogRepository
import com.obsidianbackup.permissions.PermissionManager
import com.obsidianbackup.util.DateFormatter
import com.obsidianbackup.util.SizeFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val permissionManager: PermissionManager,
    private val dateFormatter: DateFormatter,
    private val sizeFormatter: SizeFormatter,
    private val onboardingManager: OnboardingManager
) : ViewModel() {

    // initialValue = true so existing users never see onboarding again
    val onboardingCompleted: StateFlow<Boolean> = onboardingManager.onboardingCompleted
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = true)

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            combine(
                catalogRepository.getAllSnapshots(),
                permissionManager.currentMode,
                permissionManager.capabilities
            ) { snapshots, mode, capabilities ->
                val totalBackups = snapshots.size
                val lastBackup = snapshots.maxByOrNull { it.timestamp }?.timestamp
                val totalSize = snapshots.sumOf { it.totalSize }

                DashboardState(
                    totalBackups = totalBackups,
                    lastBackup = lastBackup,
                    totalSizeMB = totalSize / (1024 * 1024),
                    currentMode = mode,
                    capabilities = capabilities,
                    dateFormatter = dateFormatter,
                    sizeFormatter = sizeFormatter
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            onboardingManager.completeOnboarding()
        }
    }
}

data class DashboardState(
    val totalBackups: Int = 0,
    val lastBackup: Long? = null,
    val totalSizeMB: Long = 0,
    val currentMode: com.obsidianbackup.model.PermissionMode = com.obsidianbackup.model.PermissionMode.SAF,
    val capabilities: com.obsidianbackup.model.PermissionCapabilities = com.obsidianbackup.model.PermissionCapabilities(),
    private val dateFormatter: DateFormatter? = null,
    private val sizeFormatter: SizeFormatter? = null
) {
    val lastBackupText: String
        get() = lastBackup?.let {
            dateFormatter?.formatRelativeTime(it) ?: "N/A"
        } ?: "Never"

    val totalSizeText: String
        get() = sizeFormatter?.formatBytes(totalSizeMB * 1024 * 1024) ?: "N/A"
}
