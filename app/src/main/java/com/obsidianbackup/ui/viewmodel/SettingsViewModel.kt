package com.obsidianbackup.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obsidianbackup.data.repository.SettingsRepository
import com.obsidianbackup.model.FeatureTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // FOSS build: all features unlocked, no subscription needed
    val currentTier: StateFlow<FeatureTier> = MutableStateFlow(FeatureTier.FREE).asStateFlow()
    
    val autoBackupEnabled: StateFlow<Boolean> = settingsRepository.autoBackupEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val cloudSyncEnabled: StateFlow<Boolean> = settingsRepository.cloudSyncEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val compressionEnabled: StateFlow<Boolean> = settingsRepository.compressionEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    
    val encryptionEnabled: StateFlow<Boolean> = settingsRepository.encryptionEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val verifyAfterBackup: StateFlow<Boolean> = settingsRepository.verifyAfterBackup
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    
    val debugMode: StateFlow<Boolean> = settingsRepository.debugMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val parallelOperationsEnabled: StateFlow<Boolean> = settingsRepository.parallelOperationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoBackupEnabled(enabled)
        }
    }
    
    fun setCloudSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCloudSyncEnabled(enabled)
        }
    }
    
    fun setCompressionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCompressionEnabled(enabled)
        }
    }
    
    fun setEncryptionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setEncryptionEnabled(enabled)
        }
    }
    
    fun setVerifyAfterBackup(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVerifyAfterBackup(enabled)
        }
    }
    
    fun setDebugMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDebugMode(enabled)
        }
    }
    
    fun setParallelOperationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setParallelOperationsEnabled(enabled)
        }
    }
}
