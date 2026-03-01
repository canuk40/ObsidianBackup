// presentation/plugins/PluginsViewModel.kt
package com.obsidianbackup.presentation.plugins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obsidianbackup.features.Feature
import com.obsidianbackup.features.FeatureFlagManager
import com.obsidianbackup.model.FeatureTier
import com.obsidianbackup.plugins.core.Plugin
import com.obsidianbackup.plugins.core.PluginManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PluginsViewModel @Inject constructor(
    private val pluginManager: PluginManager,
    private val featureFlagManager: FeatureFlagManager
) : ViewModel() {

    // FOSS build: all features unlocked
    val currentTier: StateFlow<FeatureTier> = MutableStateFlow(FeatureTier.FREE).asStateFlow()

    private val _uiState = MutableStateFlow(PluginsUiState())
    val uiState: StateFlow<PluginsUiState> = _uiState.asStateFlow()

    init {
        checkFeatureEnabled()
        loadPlugins()
    }

    private fun checkFeatureEnabled() {
        viewModelScope.launch {
            val enabled = featureFlagManager.isEnabled(Feature.PLUGIN_SYSTEM)
            _uiState.update { it.copy(
                featureEnabled = enabled,
                isCheckingFeature = false           // Set to false after check completes
            )}
        }
    }

    private fun loadPlugins() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val installedPlugins = pluginManager.getInstalledPlugins()
                val enabledPlugins = pluginManager.getEnabledPlugins()
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        installedPlugins = installedPlugins,
                        enabledPlugins = enabledPlugins
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun togglePlugin(plugin: Plugin, enabled: Boolean) {
        viewModelScope.launch {
            try {
                if (enabled) {
                    pluginManager.enablePlugin(plugin.id)
                } else {
                    pluginManager.disablePlugin(plugin.id)
                }
                loadPlugins()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun discoverPlugins() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDiscovering = true) }
            pluginManager.discoverPlugins()
            loadPlugins()
            _uiState.update { it.copy(isDiscovering = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class PluginsUiState(
    val featureEnabled: Boolean = false,
    val isCheckingFeature: Boolean = true,   // Add loading state — fixes M-6 flash of disabled state
    val isLoading: Boolean = false,
    val isDiscovering: Boolean = false,
    val installedPlugins: List<Plugin> = emptyList(),
    val enabledPlugins: List<Plugin> = emptyList(),
    val error: String? = null
)
