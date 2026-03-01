// ui/viewmodel/SmartSchedulingViewModel.kt
package com.obsidianbackup.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obsidianbackup.data.repository.PreferencesRepository
import com.obsidianbackup.model.FeatureTier
import com.obsidianbackup.model.SmartSchedulingConfig
import com.obsidianbackup.model.SchedulePrediction
import com.obsidianbackup.scheduling.SmartScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.obsidianbackup.ml.prediction.BackupPredictor
import com.obsidianbackup.ml.models.BackupPrediction
import com.obsidianbackup.ml.BackupContext
import java.time.LocalTime
import java.time.DayOfWeek
import java.util.*
import java.text.SimpleDateFormat
import javax.inject.Inject

@HiltViewModel
class SmartSchedulingViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val smartScheduler: SmartScheduler,
    private val backupPredictor: BackupPredictor
) : ViewModel() {

    // FOSS build: all features unlocked
    val currentTier: StateFlow<FeatureTier> = MutableStateFlow(FeatureTier.FREE).asStateFlow()
    
    private val _config = MutableStateFlow(SmartSchedulingConfig())
    val config: StateFlow<SmartSchedulingConfig> = _config.asStateFlow()
    
    private val _nextBackup = MutableStateFlow<SchedulePrediction?>(null)
    val nextBackup: StateFlow<SchedulePrediction?> = _nextBackup.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _suggestedTimes = MutableStateFlow<List<BackupPrediction>>(emptyList())
    val suggestedTimes: StateFlow<List<BackupPrediction>> = _suggestedTimes.asStateFlow()
    
    init {
        loadConfig()
        loadNextBackup()
    }
    
    private fun loadConfig() {
        viewModelScope.launch {
            preferencesRepository.getSmartSchedulingConfig().collect { config ->
                _config.value = config
                // Update next backup prediction when config changes
                updateNextBackupPrediction()
            }
        }
    }
    
    private fun loadNextBackup() {
        viewModelScope.launch {
            _nextBackup.value = smartScheduler.getNextScheduledBackup()
        }
    }
    
    fun updateConfig(newConfig: SmartSchedulingConfig) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Save config
            preferencesRepository.saveSmartSchedulingConfig(newConfig)
            
            // Schedule or cancel based on enabled state
            smartScheduler.scheduleSmartBackup(newConfig)
            
            // Update prediction
            updateNextBackupPrediction()
            
            _isLoading.value = false
        }
    }
    
    private fun updateNextBackupPrediction() {
        viewModelScope.launch {
            if (_config.value.enabled) {
                _nextBackup.value = smartScheduler.predictNextBackupTime(_config.value)
            } else {
                _nextBackup.value = null
            }
        }
    }
    
    fun getNextBackupTime(): String {
        val prediction = _nextBackup.value ?: return "Not scheduled"
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
        return dateFormat.format(Date(prediction.nextBackupTime))
    }
    
    fun getNextBackupRelative(): String {
        val prediction = _nextBackup.value ?: return "Not scheduled"
        val now = System.currentTimeMillis()
        val diff = prediction.nextBackupTime - now
        
        if (diff < 0) return "Overdue"
        
        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff / (1000 * 60)) % 60
        
        return when {
            hours < 1 -> "In $minutes minutes"
            hours < 24 -> "In $hours hours"
            else -> {
                val days = hours / 24
                "In $days days"
            }
        }
    }
    
    fun getSuggestedScheduleTime() {
        viewModelScope.launch {
            val context = BackupContext(
                batteryLevel = 1f,
                isCharging = true,
                isWifiConnected = true,
                locationCategory = com.obsidianbackup.ml.LocationCategory.HOME,
                activityType = com.obsidianbackup.ml.ActivityType.STILL,
                timeOfDay = LocalTime.now(),
                dayOfWeek = DayOfWeek.from(java.time.LocalDate.now()),
                storageAvailableMb = 1024L
            )
            _suggestedTimes.value = backupPredictor.predictNextBackups(context)
        }
    }

    fun cancelSchedule() {
        viewModelScope.launch {
            val newConfig = _config.value.copy(enabled = false)
            updateConfig(newConfig)
            smartScheduler.cancelSmartBackup()
        }
    }
}
