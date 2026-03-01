package com.obsidianbackup.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obsidianbackup.automation.BackupFrequency
import com.obsidianbackup.automation.ScheduleManager
import com.obsidianbackup.data.repository.Schedule
import com.obsidianbackup.data.repository.ScheduleRepository
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.model.AppId
import com.obsidianbackup.model.BackupComponent
import com.obsidianbackup.model.FeatureTier
import com.obsidianbackup.scanner.AppScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject



data class AutomationState(
    val schedules: List<Schedule> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val availableApps: List<AppInfo> = emptyList()
)

data class AppInfo(
    val id: AppId,
    val name: String,
    val packageName: String,
    val isSelected: Boolean = false
)

sealed class AutomationIntent {
    object LoadSchedules : AutomationIntent()
    object ShowCreateDialog : AutomationIntent()
    object HideCreateDialog : AutomationIntent()
    data class CreateSchedule(
        val name: String,
        val frequency: BackupFrequency,
        val hour: Int,
        val minute: Int,
        val appIds: List<AppId>,
        val components: Set<BackupComponent>,
        val requiresCharging: Boolean,
        val requiresWifi: Boolean
    ) : AutomationIntent()
    data class DeleteSchedule(val scheduleId: String) : AutomationIntent()
    data class ToggleSchedule(val scheduleId: String, val enabled: Boolean) : AutomationIntent()
    data class ToggleAppSelection(val appId: AppId) : AutomationIntent()
}

@HiltViewModel
class AutomationViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val scheduleManager: ScheduleManager,
    private val appScanner: AppScanner,
    private val logger: ObsidianLogger
) : ViewModel() {

    // FOSS build: all features unlocked
    val currentTier: StateFlow<FeatureTier> = MutableStateFlow(FeatureTier.FREE).asStateFlow()

    private val _state = MutableStateFlow(AutomationState())
    val state: StateFlow<AutomationState> = _state.asStateFlow()

    init {
        handleIntent(AutomationIntent.LoadSchedules)
        loadAvailableApps()
    }

    fun handleIntent(intent: AutomationIntent) {
        when (intent) {
            is AutomationIntent.LoadSchedules -> loadSchedules()
            is AutomationIntent.ShowCreateDialog -> showCreateDialog()
            is AutomationIntent.HideCreateDialog -> hideCreateDialog()
            is AutomationIntent.CreateSchedule -> createSchedule(intent)
            is AutomationIntent.DeleteSchedule -> deleteSchedule(intent.scheduleId)
            is AutomationIntent.ToggleSchedule -> toggleSchedule(intent.scheduleId, intent.enabled)
            is AutomationIntent.ToggleAppSelection -> toggleAppSelection(intent.appId)
        }
    }

    private fun loadSchedules() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }
                scheduleRepository.getAllSchedules()
                    .catch { e ->
                        logger.e(TAG, "Error loading schedules", e)
                        _state.update { it.copy(isLoading = false, error = e.message) }
                    }
                    .collect { schedules ->
                        _state.update { it.copy(schedules = schedules, isLoading = false) }
                    }
            } catch (e: Exception) {
                logger.e(TAG, "Error loading schedules", e)
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun loadAvailableApps() {
        viewModelScope.launch {
            try {
                val apps = appScanner.scanInstalledApps(includeSystemApps = false).map { app ->
                    AppInfo(
                        id = AppId(app.packageName),
                        name = app.appName,
                        packageName = app.packageName,
                        isSelected = false
                    )
                }
                _state.update { it.copy(availableApps = apps) }
            } catch (e: Exception) {
                logger.e(TAG, "Error loading apps", e)
            }
        }
    }

    private fun showCreateDialog() {
        _state.update { it.copy(showCreateDialog = true) }
    }

    private fun hideCreateDialog() {
        _state.update { 
            it.copy(
                showCreateDialog = false,
                availableApps = it.availableApps.map { app -> app.copy(isSelected = false) }
            ) 
        }
    }

    private fun createSchedule(intent: AutomationIntent.CreateSchedule) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }

                val schedule = scheduleRepository.createSchedule(
                    name = intent.name,
                    frequency = intent.frequency,
                    hour = intent.hour,
                    minute = intent.minute,
                    appIds = intent.appIds,
                    components = intent.components,
                    requiresCharging = intent.requiresCharging,
                    requiresWifi = intent.requiresWifi
                )

                // Schedule the backup with WorkManager
                scheduleManager.scheduleBackup(schedule)

                logger.i(TAG, "Schedule created: ${schedule.name}")
                _state.update { it.copy(isLoading = false, showCreateDialog = false) }
            } catch (e: Exception) {
                logger.e(TAG, "Error creating schedule", e)
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            try {
                scheduleManager.cancelSchedule(scheduleId)
                scheduleRepository.deleteSchedule(scheduleId)
                logger.i(TAG, "Schedule deleted: $scheduleId")
            } catch (e: Exception) {
                logger.e(TAG, "Error deleting schedule", e)
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun toggleSchedule(scheduleId: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                scheduleRepository.setScheduleEnabled(scheduleId, enabled)
                
                if (enabled) {
                    val schedule = scheduleRepository.getSchedule(scheduleId)
                    if (schedule != null) {
                        scheduleManager.scheduleBackup(schedule)
                        logger.i(TAG, "Schedule enabled: $scheduleId")
                    }
                } else {
                    scheduleManager.cancelSchedule(scheduleId)
                    logger.i(TAG, "Schedule disabled: $scheduleId")
                }
            } catch (e: Exception) {
                logger.e(TAG, "Error toggling schedule", e)
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun toggleAppSelection(appId: AppId) {
        _state.update { state ->
            state.copy(
                availableApps = state.availableApps.map { app ->
                    if (app.id == appId) app.copy(isSelected = !app.isSelected)
                    else app
                }
            )
        }
    }

    companion object {
        private const val TAG = "AutomationViewModel"
    }
}
