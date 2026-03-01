package com.obsidianbackup.wear.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obsidianbackup.wear.data.BackupProgress
import com.obsidianbackup.wear.data.BackupStatus
import com.obsidianbackup.wear.data.DataLayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Wear OS UI
 */
@HiltViewModel
class WearViewModel @Inject constructor(
    private val dataLayerRepository: DataLayerRepository
) : ViewModel() {

    val backupStatus: StateFlow<BackupStatus> = dataLayerRepository.backupStatus
    val backupProgress: StateFlow<BackupProgress> = dataLayerRepository.backupProgress
    
    private val _isPhoneConnected = MutableStateFlow(false)
    val isPhoneConnected: StateFlow<Boolean> = _isPhoneConnected.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        checkPhoneConnection()
        requestStatus()
    }

    fun triggerBackup() {
        viewModelScope.launch {
            _isLoading.value = true
            val success = dataLayerRepository.requestBackup()
            _isLoading.value = false
            
            if (success) {
                // Optionally provide haptic feedback
            }
        }
    }

    fun cancelBackup() {
        viewModelScope.launch {
            dataLayerRepository.cancelBackup()
        }
    }

    fun requestStatus() {
        viewModelScope.launch {
            dataLayerRepository.requestStatus()
        }
    }

    private fun checkPhoneConnection() {
        viewModelScope.launch {
            _isPhoneConnected.value = dataLayerRepository.isPhoneConnected()
        }
    }
}
