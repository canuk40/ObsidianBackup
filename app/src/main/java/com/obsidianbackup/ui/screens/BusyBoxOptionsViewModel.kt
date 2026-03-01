// ui/screens/BusyBoxOptionsViewModel.kt
package com.obsidianbackup.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obsidianbackup.rootcore.busybox.BusyBoxManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BusyBoxUiState(
    val isLoading: Boolean = true,
    val isAvailable: Boolean = false,
    val busyBoxPath: String? = null,
    val version: String? = null,
    val applets: List<String> = emptyList(),
    val disabledApplets: Set<String> = emptySet(),
    val customTarFlags: String = "",
    val customRsyncFlags: String = "",
    val customZstdFlags: String = "",
    val ashStandaloneMode: Boolean = true
)

@HiltViewModel
class BusyBoxOptionsViewModel @Inject constructor(
    private val busyBoxManager: BusyBoxManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val PREFS = "busybox_options"
        private const val KEY_DISABLED_APPLETS = "disabled_applets"
        private const val KEY_TAR_FLAGS = "tar_flags"
        private const val KEY_RSYNC_FLAGS = "rsync_flags"
        private const val KEY_ZSTD_FLAGS = "zstd_flags"
        private const val KEY_ASH_STANDALONE = "ash_standalone"
    }

    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    private val _uiState = MutableStateFlow(BusyBoxUiState())
    val uiState: StateFlow<BusyBoxUiState> = _uiState.asStateFlow()

    init {
        loadSavedPrefs()
        detectBusyBox()
    }

    private fun loadSavedPrefs() {
        val disabled = prefs.getStringSet(KEY_DISABLED_APPLETS, emptySet()) ?: emptySet()
        _uiState.update {
            it.copy(
                disabledApplets = disabled,
                customTarFlags = prefs.getString(KEY_TAR_FLAGS, "") ?: "",
                customRsyncFlags = prefs.getString(KEY_RSYNC_FLAGS, "") ?: "",
                customZstdFlags = prefs.getString(KEY_ZSTD_FLAGS, "") ?: "",
                ashStandaloneMode = prefs.getBoolean(KEY_ASH_STANDALONE, true)
            )
        }
    }

    private fun detectBusyBox() {
        viewModelScope.launch {
            // Extract bundled binary first (no-op if already extracted)
            busyBoxManager.extractBundledBusyBox()

            val path = busyBoxManager.getBusyBoxPath()
            val version = if (path != null) busyBoxManager.getVersion() else null
            val applets = if (path != null) busyBoxManager.listApplets() else emptyList()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isAvailable = path != null,
                    busyBoxPath = path,
                    version = version,
                    applets = applets.sorted()
                )
            }
        }
    }

    fun setAppletEnabled(applet: String, enabled: Boolean) {
        val current = _uiState.value.disabledApplets.toMutableSet()
        if (enabled) current.remove(applet) else current.add(applet)
        _uiState.update { it.copy(disabledApplets = current) }
        prefs.edit().putStringSet(KEY_DISABLED_APPLETS, current).apply()
    }

    fun saveTarFlags(flags: String) {
        _uiState.update { it.copy(customTarFlags = flags) }
        prefs.edit().putString(KEY_TAR_FLAGS, flags).apply()
    }

    fun saveRsyncFlags(flags: String) {
        _uiState.update { it.copy(customRsyncFlags = flags) }
        prefs.edit().putString(KEY_RSYNC_FLAGS, flags).apply()
    }

    fun saveZstdFlags(flags: String) {
        _uiState.update { it.copy(customZstdFlags = flags) }
        prefs.edit().putString(KEY_ZSTD_FLAGS, flags).apply()
    }

    fun setAshStandaloneMode(enabled: Boolean) {
        _uiState.update { it.copy(ashStandaloneMode = enabled) }
        prefs.edit().putBoolean(KEY_ASH_STANDALONE, enabled).apply()
    }
}
