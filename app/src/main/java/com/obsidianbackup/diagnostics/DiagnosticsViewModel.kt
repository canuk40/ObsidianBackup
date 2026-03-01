// diagnostics/DiagnosticsViewModel.kt
package com.obsidianbackup.diagnostics

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class DiagnosticsState {
    data object Idle : DiagnosticsState()
    data object Working : DiagnosticsState()
    data class ReadyToShare(val intent: Intent) : DiagnosticsState()
    data class Error(val message: String) : DiagnosticsState()
    data object NoLogs : DiagnosticsState()
}

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val exporter: DiagnosticsExporter,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<DiagnosticsState>(DiagnosticsState.Idle)
    val state: StateFlow<DiagnosticsState> = _state.asStateFlow()

    fun exportAppLogs() = export {
        exporter.getAppLogsFile()
    }

    fun exportShellAudit() = export {
        exporter.getShellAuditFile()
    }

    fun exportDiagnosticsBundle() = export {
        exporter.buildDiagnosticsBundle()
    }

    fun resetState() {
        _state.value = DiagnosticsState.Idle
    }

    private fun export(block: suspend () -> File?) {
        viewModelScope.launch {
            _state.value = DiagnosticsState.Working
            try {
                val file = block()
                if (file == null || !file.exists()) {
                    _state.value = DiagnosticsState.NoLogs
                    return@launch
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val mime = if (file.extension == "zip") "application/zip" else "text/plain"
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "ObsidianBackup — ${file.name}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                _state.value = DiagnosticsState.ReadyToShare(
                    Intent.createChooser(intent, "Share ${file.name}")
                )
            } catch (e: Exception) {
                _state.value = DiagnosticsState.Error(e.message ?: "Export failed")
            }
        }
    }
}
