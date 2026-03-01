// presentation/gaming/GamingBackupViewModel.kt
package com.obsidianbackup.presentation.gaming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obsidianbackup.gaming.GamingBackupManager
import com.obsidianbackup.gaming.models.*
import com.obsidianbackup.model.FeatureTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GamingBackupViewModel @Inject constructor(
    private val gamingBackupManager: GamingBackupManager
) : ViewModel() {

    // FOSS build: all features unlocked
    val currentTier: StateFlow<FeatureTier> = MutableStateFlow(FeatureTier.FREE).asStateFlow()
    
    val detectedEmulators: StateFlow<List<DetectedEmulator>> = 
        gamingBackupManager.detectedEmulators
    
    val backupProgress: StateFlow<GamingBackupProgress> = 
        gamingBackupManager.backupProgress
    
    private val _backupHistory = MutableStateFlow<List<BackupResult>>(emptyList())
    val backupHistory: StateFlow<List<BackupResult>> = _backupHistory.asStateFlow()
    
    init {
        viewModelScope.launch {
            scanForEmulators()
            loadBackupHistory()
        }
    }
    
    suspend fun scanForEmulators() {
        gamingBackupManager.scanForEmulators()
    }
    
    suspend fun backupEmulator(emulator: DetectedEmulator, options: BackupOptions) {
        viewModelScope.launch {
            try {
                // Scan for games (simplified - in real app, user would select games)
                val games = listOf(
                    GameInfo(
                        name = "Example Game",
                        platform = emulator.supportedPlatforms.firstOrNull() ?: "Unknown"
                    )
                )
                
                val result = gamingBackupManager.backupGameSaves(emulator, games, options)
                loadBackupHistory()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    private suspend fun loadBackupHistory() {
        _backupHistory.value = gamingBackupManager.getBackupHistory()
    }
}
