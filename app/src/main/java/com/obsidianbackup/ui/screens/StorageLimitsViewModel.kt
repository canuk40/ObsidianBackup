package com.obsidianbackup.ui.screens

import android.content.Context
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obsidianbackup.data.repository.SettingsRepository
import com.obsidianbackup.storage.BackupCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageLimitsUiState(
    val limitMb: Int = 0,               // 0 = unlimited
    val usedMb: Long = 0,
    val snapshotCount: Int = 0,
    val deviceFreeMb: Long = 0,
    val deviceTotalMb: Long = 0,
    val isLoaded: Boolean = false
)

@HiltViewModel
class StorageLimitsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupCatalog: BackupCatalog,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(StorageLimitsUiState())
    val state: StateFlow<StorageLimitsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.storageLimitMb.collect { limitMb ->
                _state.update { it.copy(limitMb = limitMb) }
            }
        }

        viewModelScope.launch {
            backupCatalog.getAllSnapshots().collect { snapshots ->
                val usedBytes = snapshots.sumOf { it.totalSize }
                val (freeBytes, totalBytes) = getDeviceStorage()
                _state.update {
                    it.copy(
                        usedMb = usedBytes / 1024 / 1024,
                        snapshotCount = snapshots.size,
                        deviceFreeMb = freeBytes / 1024 / 1024,
                        deviceTotalMb = totalBytes / 1024 / 1024,
                        isLoaded = true
                    )
                }
            }
        }
    }

    fun setLimitMb(mb: Int) {
        viewModelScope.launch {
            settingsRepository.setStorageLimitMb(mb)
        }
    }

    private fun getDeviceStorage(): Pair<Long, Long> {
        return try {
            val path = context.getExternalFilesDir(null)?.absolutePath
                ?: Environment.getDataDirectory().absolutePath
            val stat = StatFs(path)
            val free = stat.availableBlocksLong * stat.blockSizeLong
            val total = stat.blockCountLong * stat.blockSizeLong
            Pair(free, total)
        } catch (e: Exception) {
            Pair(0L, 0L)
        }
    }
}
