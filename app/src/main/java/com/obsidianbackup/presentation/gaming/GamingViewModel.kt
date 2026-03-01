// presentation/gaming/GamingViewModel.kt
package com.obsidianbackup.presentation.gaming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obsidianbackup.features.Feature
import com.obsidianbackup.features.FeatureFlagManager
import com.obsidianbackup.gaming.GamingBackupManager
import com.obsidianbackup.gaming.models.BackupOptions
import com.obsidianbackup.gaming.models.DetectedEmulator
import com.obsidianbackup.gaming.models.GameInfo
import com.obsidianbackup.gaming.models.GamingBackupProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GamingViewModel @Inject constructor(
    private val gamingBackupManager: GamingBackupManager,
    private val featureFlagManager: FeatureFlagManager
) : ViewModel() {

    val backupProgress = gamingBackupManager.backupProgress
    val detectedEmulators = gamingBackupManager.detectedEmulators

    private val _uiState = MutableStateFlow(GamingUiState())
    val uiState: StateFlow<GamingUiState> = _uiState.asStateFlow()

    init {
        checkFeatureEnabled()
        scanEmulators()
    }

    private fun checkFeatureEnabled() {
        viewModelScope.launch {
            val enabled = featureFlagManager.isEnabled(Feature.GAMING_BACKUP)
            _uiState.update { it.copy(featureEnabled = enabled) }
        }
    }

    fun scanEmulators() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val emulators = gamingBackupManager.scanForEmulators()
            _uiState.update { 
                it.copy(
                    isScanning = false,
                    emulators = emulators
                )
            }
        }
    }

    fun selectEmulator(emulator: DetectedEmulator) {
        _uiState.update { it.copy(selectedEmulator = emulator) }
    }

    fun toggleGameSelection(game: GameInfo) {
        _uiState.update { state ->
            val selectedGames = state.selectedGames.toMutableList()
            if (selectedGames.contains(game)) {
                selectedGames.remove(game)
            } else {
                selectedGames.add(game)
            }
            state.copy(selectedGames = selectedGames)
        }
    }

    fun backupSelectedGames() {
        val emulator = _uiState.value.selectedEmulator ?: return
        val games = _uiState.value.selectedGames
        if (games.isEmpty()) return

        viewModelScope.launch {
            val options = BackupOptions(
                includeRoms = _uiState.value.includeRoms,
                includeSaves = true,
                compression = true
            )
            gamingBackupManager.backupGameSaves(emulator, games, options)
        }
    }

    fun toggleIncludeRoms(include: Boolean) {
        _uiState.update { it.copy(includeRoms = include) }
    }
}

data class GamingUiState(
    val featureEnabled: Boolean = false,
    val isScanning: Boolean = false,
    val emulators: List<DetectedEmulator> = emptyList(),
    val selectedEmulator: DetectedEmulator? = null,
    val selectedGames: List<GameInfo> = emptyList(),
    val includeRoms: Boolean = false
)
