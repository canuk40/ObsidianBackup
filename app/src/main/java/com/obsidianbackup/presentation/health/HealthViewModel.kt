// presentation/health/HealthViewModel.kt
package com.obsidianbackup.presentation.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obsidianbackup.features.Feature
import com.obsidianbackup.features.FeatureFlagManager
import com.obsidianbackup.health.HealthConnectManager
import com.obsidianbackup.model.FeatureTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
    private val featureFlagManager: FeatureFlagManager
) : ViewModel() {

    // FOSS build: all features unlocked
    val currentTier: StateFlow<FeatureTier> = MutableStateFlow(FeatureTier.FREE).asStateFlow()

    val backupState = healthConnectManager.backupState
    val privacySettings = healthConnectManager.privacySettings

    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    val healthPermissions: Set<String> by lazy {
        healthConnectManager.getRequiredPermissions(
            setOf(
                com.obsidianbackup.health.HealthDataType.STEPS,
                com.obsidianbackup.health.HealthDataType.HEART_RATE,
                com.obsidianbackup.health.HealthDataType.SLEEP,
                com.obsidianbackup.health.HealthDataType.WORKOUTS,
                com.obsidianbackup.health.HealthDataType.NUTRITION,
                com.obsidianbackup.health.HealthDataType.BODY_MEASUREMENTS
            )
        )
    }

    init {
        checkFeatureEnabled()
        checkHealthConnectAvailability()
        checkExistingPermissions()
    }

    private fun checkFeatureEnabled() {
        viewModelScope.launch {
            val enabled = featureFlagManager.isEnabled(Feature.HEALTH_CONNECT_SYNC)
            _uiState.update { it.copy(
                featureEnabled = enabled,
                isCheckingFeature = false
            )}
        }
    }

    private fun checkHealthConnectAvailability() {
        viewModelScope.launch {
            val available = healthConnectManager.isHealthConnectAvailable()
            _uiState.update { it.copy(healthConnectAvailable = available) }
        }
    }

    private fun checkExistingPermissions() {
        viewModelScope.launch {
            val alreadyGranted = healthConnectManager.checkGrantedPermissions(healthPermissions)
            if (alreadyGranted) {
                _uiState.update { it.copy(permissionGranted = true) }
            }
        }
    }

    fun onPermissionsResult(granted: Set<String>) {
        _uiState.update { it.copy(
            isRequestingPermissions = false,
            permissionGranted = granted.containsAll(healthPermissions)
        ) }
    }

    fun requestPermissions() {
        _uiState.update { it.copy(isRequestingPermissions = true) }
        // Launcher is triggered from the Composable via healthPermissions
    }

    fun backupHealthData() {
        viewModelScope.launch {
            healthConnectManager.backupHealthData(
                dataTypes = setOf(
                    com.obsidianbackup.health.HealthDataType.STEPS,
                    com.obsidianbackup.health.HealthDataType.HEART_RATE,
                    com.obsidianbackup.health.HealthDataType.SLEEP
                )
            )
        }
    }

    fun updatePrivacySetting(anonymize: Boolean) {
        viewModelScope.launch {
            val currentSettings = healthConnectManager.privacySettings.value
            healthConnectManager.updatePrivacySettings(
                currentSettings.copy(anonymizeData = anonymize)
            )
        }
    }
}

data class HealthUiState(
    val featureEnabled: Boolean = false,
    val isCheckingFeature: Boolean = true,   // Add loading state — fixes M-6 flash of disabled state
    val healthConnectAvailable: Boolean = false,
    val permissionGranted: Boolean = false,
    val isRequestingPermissions: Boolean = false
)
