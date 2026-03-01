// health/HealthPrivacyViewModel.kt
package com.obsidianbackup.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obsidianbackup.logging.ObsidianLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HealthPrivacyViewModel @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
    private val logger: ObsidianLogger
) : ViewModel() {
    
    val privacySettings: StateFlow<HealthPrivacySettings> = 
        healthConnectManager.privacySettings
    
    val backupState: StateFlow<HealthBackupState> = 
        healthConnectManager.backupState
    
    private val _statistics = MutableStateFlow<HealthBackupStatistics?>(null)
    val statistics: StateFlow<HealthBackupStatistics?> = _statistics.asStateFlow()
    
    companion object {
        private const val TAG = "HealthPrivacyViewModel"
    }
    
    init {
        loadStatistics()
    }
    
    fun toggleDataType(dataType: HealthDataType, enabled: Boolean) {
        viewModelScope.launch {
            val current = privacySettings.value
            val updatedTypes = if (enabled) {
                current.enabledDataTypes + dataType
            } else {
                current.enabledDataTypes - dataType
            }
            
            val updated = current.copy(enabledDataTypes = updatedTypes)
            healthConnectManager.updatePrivacySettings(updated)
        }
    }
    
    fun updatePrivacySettings(settings: HealthPrivacySettings) {
        viewModelScope.launch {
            healthConnectManager.updatePrivacySettings(settings)
        }
    }
    
    fun backupAllHealthData() {
        viewModelScope.launch {
            try {
                val enabledTypes = privacySettings.value.enabledDataTypes
                val result = healthConnectManager.backupHealthData(
                    dataTypes = enabledTypes,
                    exportFormat = ExportFormat.JSON
                )
                
                if (result.isSuccess) {
                    logger.i(TAG, "Health data backup completed successfully")
                    loadStatistics()
                } else {
                    logger.e(TAG, "Health data backup failed: ${result.exceptionOrNull()}")
                }
            } catch (e: Exception) {
                logger.e(TAG, "Error during health data backup", e)
            }
        }
    }
    
    fun exportToJSON() {
        viewModelScope.launch {
            try {
                val enabledTypes = privacySettings.value.enabledDataTypes
                val result = healthConnectManager.exportToFormat(
                    dataTypes = enabledTypes,
                    format = ExportFormat.JSON,
                    outputPath = "health_export_${System.currentTimeMillis()}.json"
                )
                
                if (result.isSuccess) {
                    logger.i(TAG, "Health data exported to JSON: ${result.getOrNull()}")
                }
            } catch (e: Exception) {
                logger.e(TAG, "Error exporting to JSON", e)
            }
        }
    }
    
    fun exportToCSV() {
        viewModelScope.launch {
            try {
                val enabledTypes = privacySettings.value.enabledDataTypes
                val result = healthConnectManager.exportToFormat(
                    dataTypes = enabledTypes,
                    format = ExportFormat.CSV,
                    outputPath = "health_export_${System.currentTimeMillis()}.csv"
                )
                
                if (result.isSuccess) {
                    logger.i(TAG, "Health data exported to CSV: ${result.getOrNull()}")
                }
            } catch (e: Exception) {
                logger.e(TAG, "Error exporting to CSV", e)
            }
        }
    }
    
    fun deleteAllBackups() {
        viewModelScope.launch {
            try {
                val result = healthConnectManager.deleteAllHealthBackups()
                if (result.isSuccess) {
                    logger.i(TAG, "All health backups deleted")
                    loadStatistics()
                }
            } catch (e: Exception) {
                logger.e(TAG, "Error deleting backups", e)
            }
        }
    }
    
    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                val stats = healthConnectManager.getBackupStatistics()
                _statistics.value = stats
            } catch (e: Exception) {
                logger.e(TAG, "Error loading statistics", e)
            }
        }
    }
}
