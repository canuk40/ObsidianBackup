package com.obsidianbackup.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obsidianbackup.data.repository.LogRepository
import com.obsidianbackup.model.FeatureTier
import com.obsidianbackup.model.LogEntry
import com.obsidianbackup.model.LogLevel
import com.obsidianbackup.model.OperationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val logRepository: LogRepository
) : ViewModel() {

    // FOSS build: all features unlocked
    val currentTier: StateFlow<FeatureTier> = MutableStateFlow(FeatureTier.FREE).asStateFlow()
    
    private val _selectedLevel = MutableStateFlow<LogLevel?>(null)
    val selectedLevel: StateFlow<LogLevel?> = _selectedLevel.asStateFlow()
    
    private val _selectedOperation = MutableStateFlow<OperationType?>(null)
    val selectedOperation: StateFlow<OperationType?> = _selectedOperation.asStateFlow()
    
    // Get all logs and apply filters
    val logs: StateFlow<List<LogEntry>> = combine(
        logRepository.getRecentLogs(500),
        _selectedLevel,
        _selectedOperation
    ) { allLogs, level, operation ->
        allLogs
            .filter { log -> level == null || log.level == level }
            .filter { log -> operation == null || log.operationType == operation }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    fun setLevelFilter(level: LogLevel?) {
        _selectedLevel.value = level
    }
    
    fun setOperationFilter(operation: OperationType?) {
        _selectedOperation.value = operation
    }
    
    fun clearFilters() {
        _selectedLevel.value = null
        _selectedOperation.value = null
    }
    
    fun clearAllLogs() {
        viewModelScope.launch {
            logRepository.clearAllLogs()
        }
    }
}
