// presentation/gaming/SpeedrunViewModel.kt
package com.obsidianbackup.presentation.gaming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obsidianbackup.gaming.SaveStateManager
import com.obsidianbackup.gaming.models.SaveState
import com.obsidianbackup.gaming.models.SpeedrunProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpeedrunViewModel @Inject constructor(
    private val saveStateManager: SaveStateManager
) : ViewModel() {
    
    private val _profiles = MutableStateFlow<List<SpeedrunProfile>>(emptyList())
    val profiles: StateFlow<List<SpeedrunProfile>> = _profiles.asStateFlow()
    
    private val _currentProfile = MutableStateFlow<SpeedrunProfile?>(null)
    val currentProfile: StateFlow<SpeedrunProfile?> = _currentProfile.asStateFlow()
    
    suspend fun createProfile(gameName: String, maxStates: Int) {
        val profile = saveStateManager.createSpeedrunProfile(gameName, maxStates)
        _profiles.value = _profiles.value + profile
    }
    
    fun selectProfile(profile: SpeedrunProfile) {
        _currentProfile.value = profile
    }
    
    fun closeProfile() {
        _currentProfile.value = null
    }
    
    suspend fun createQuickSave() {
        val profile = _currentProfile.value ?: return
        
        viewModelScope.launch {
            try {
                // In a real implementation, this would get the emulator from the profile
                // For now, we create a basic save state
                val saveState = SaveState(
                    path = "",
                    label = "Quick Save ${System.currentTimeMillis()}",
                    timestamp = System.currentTimeMillis(),
                    screenshot = null,
                    checksum = ""
                )
                
                val updated = saveStateManager.addToSpeedrunProfile(profile.gameName, saveState)
                if (updated != null) {
                    _currentProfile.value = updated
                    updateProfileInList(updated)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    suspend fun loadSaveState(saveState: SaveState) {
        viewModelScope.launch {
            saveStateManager.loadSaveState(saveState)
        }
    }
    
    suspend fun deleteSaveState(saveState: SaveState) {
        val profile = _currentProfile.value ?: return
        
        viewModelScope.launch {
            if (saveStateManager.deleteSaveState(saveState)) {
                val updated = profile.copy(
                    saveStates = profile.saveStates.filter { it != saveState }
                )
                _currentProfile.value = updated
                updateProfileInList(updated)
            }
        }
    }
    
    private fun updateProfileInList(updated: SpeedrunProfile) {
        _profiles.value = _profiles.value.map { 
            if (it.gameName == updated.gameName) updated else it 
        }
    }
}
