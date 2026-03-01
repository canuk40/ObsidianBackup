// ui/components/LiveBackupConsole.kt
package com.obsidianbackup.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obsidianbackup.model.LogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import com.obsidianbackup.model.BackupComponent

@Composable
fun LiveBackupConsole(operationId: String, logsFlow: Flow<List<LogEntry>>? = null) {
    // If a flow is provided, collect it; otherwise show placeholder
    val logsState: State<List<LogEntry>> = if (logsFlow != null) {
        logsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    } else {
        remember { mutableStateOf<List<LogEntry>>(emptyList()) }
    }

    val logs by remember { logsState }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    LazyColumn {
        items(logs) { logEntry ->
            ConsoleLogRow(
                timestamp = logEntry.timestamp,
                command = logEntry.message,
                output = logEntry.details ?: "",
                exitCode = logEntry.exitCode ?: 0
            )
        }
    }

    // Export button
    Button(onClick = {
        scope.launch {
            exportLogs(logs, context)
        }
    }) {
        Text("Export Full Log")
    }
}

// Show exact BusyBox commands being run
@Composable
fun ConsoleLogRow(
    timestamp: Long,
    command: String,
    output: String,
    exitCode: Int
) {
    Card {
        Text("$ $command", fontFamily = FontFamily.Monospace)
        if (output.isNotEmpty()) {
            Text(output, fontSize = 12.sp, color = Color.Gray)
        }
        Text("Exit: $exitCode", color = if (exitCode == 0) Color.Green else Color.Red)
    }
}

private suspend fun exportLogs(logs: List<LogEntry>, context: android.content.Context) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val exportFile = java.io.File(
                context.getExternalFilesDir(null),
                "backup_logs_${System.currentTimeMillis()}.txt"
            )
            
            exportFile.bufferedWriter().use { writer ->
                writer.write("ObsidianBackup Log Export\n")
                writer.write("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
                writer.write("Total Entries: ${logs.size}\n")
                writer.write("${"=".repeat(80)}\n\n")
                
                logs.forEach { logEntry ->
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    writer.write("Timestamp: ${dateFormat.format(java.util.Date(logEntry.timestamp))}\n")
                    writer.write("Level: ${logEntry.level}\n")
                    writer.write("Operation: ${logEntry.operationType}\n")
                    writer.write("Message: ${logEntry.message}\n")
                    logEntry.details?.let { writer.write("Details: $it\n") }
                    logEntry.exitCode?.let { writer.write("Exit Code: $it\n") }
                    writer.write("${"-".repeat(40)}\n\n")
                }
            }
            
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(
                    context,
                    "Logs exported to ${exportFile.name}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Log.e("LiveBackupConsole", "Failed to export logs", e)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(
                    context,
                    "Failed to export logs: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

// Helper for collecting flows with lifecycle; replace earlier broken helper
@Composable
fun <T> collectFlowWithLifecycle(flow: Flow<T>, initial: T): State<T> {
    return flow.collectAsStateWithLifecycle(initial)
}

@Composable
fun BackupDiffScreen(
    oldSnapshot: com.obsidianbackup.model.BackupId, 
    newSnapshot: com.obsidianbackup.model.BackupId,
    viewModel: com.obsidianbackup.ui.viewmodel.DiffViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val diff by viewModel.getDiff(oldSnapshot, newSnapshot).collectAsStateWithLifecycle(initialValue = null)
    
    if (diff == null) {
        Box(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator()
        }
    } else {
        val actualDiff = diff!!
        LazyColumn {
            item { Text("Files Added: ${actualDiff.added.size}", color = Color.Green) }
            items<FileMetadata>(actualDiff.added) { file -> FileRow(file, ChangeType.ADDED) }

            item { Text("Files Modified: ${actualDiff.modified.size}", color = Color(0xFFFFA500)) }
            items<FileMetadata>(actualDiff.modified) { file -> FileRow(file, ChangeType.MODIFIED) }

            item { Text("Files Deleted: ${actualDiff.deleted.size}", color = Color.Red) }
            items<FileMetadata>(actualDiff.deleted) { file -> FileRow(file, ChangeType.DELETED) }
        }
    }
}

@Composable
fun FileRow(file: FileMetadata, changeType: ChangeType) {
    Text("${changeType.name}: ${file.path}")
}

data class SnapshotDiff(
    val added: List<FileMetadata>,
    val modified: List<FileMetadata>,
    val deleted: List<FileMetadata>,
    val sizeChange: Long
)

enum class ChangeType {
    ADDED, MODIFIED, DELETED
}

// Placeholder data class
data class FileMetadata(val path: String, val checksum: String, val size: Long)

data class RestoreImpactReport(
    val appsToInstall: List<com.obsidianbackup.model.AppId>,
    val appsToOverwrite: List<com.obsidianbackup.model.AppId>,
    val dataToReplace: Map<com.obsidianbackup.model.AppId, Long>, // Size in bytes
    val permissionsRequired: List<String>,
    val selinuxWarnings: List<String>,
    val estimatedTime: kotlin.time.Duration,
    val riskLevel: RiskLevel
)

enum class RiskLevel {
    LOW,     // All apps in snapshot, no system apps
    MEDIUM,  // Some missing apps or system apps
    HIGH     // Major version mismatch or system modifications
}

@Composable
fun RestoreSimulationDialog(
    snapshot: com.obsidianbackup.model.BackupSnapshot,
    viewModel: RestoreSimulationViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val report by viewModel.simulateRestore(snapshot).collectAsStateWithLifecycle(
        initialValue = RestoreImpactReport(
            appsToInstall = emptyList(),
            appsToOverwrite = emptyList(),
            dataToReplace = emptyMap(),
            permissionsRequired = emptyList(),
            selinuxWarnings = emptyList(),
            estimatedTime = kotlin.time.Duration.ZERO,
            riskLevel = RiskLevel.LOW
        )
    )

    androidx.compose.material3.AlertDialog(
        onDismissRequest = { viewModel.dismissDialog() },
        title = { Text("Restore Impact Analysis") },
        text = {
            androidx.compose.foundation.layout.Column {
                Text("Risk Level: ${report.riskLevel}",
                    color = when(report.riskLevel) {
                        RiskLevel.HIGH -> Color.Red
                        RiskLevel.MEDIUM -> Color(0xFFFFA500) // Orange
                        else -> Color.Green
                    })

                Text("${report.appsToInstall.size} apps will be installed")
                Text("${report.appsToOverwrite.size} apps will be overwritten")
                Text("Est. time: ${report.estimatedTime.inWholeMinutes} min")

                if (report.selinuxWarnings.isNotEmpty()) {
                    Text("⚠ SELinux warnings:", color = Color(0xFFFFA500)) // Orange
                    report.selinuxWarnings.forEach { Text("  • $it") }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { viewModel.proceedWithRestore(snapshot) }) {
                Text("Proceed")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = { viewModel.dismissDialog() }) {
                Text("Cancel")
            }
        }
    )
}

enum class BackupProfile(
    val components: Set<BackupComponent>,
    val compression: Int,
    val verification: VerificationLevel
) {
    MINIMAL(
        components = setOf(BackupComponent.APK),
        compression = 3,
        verification = VerificationLevel.QUICK
    ),
    STANDARD(
        components = setOf(BackupComponent.APK, BackupComponent.DATA),
        compression = 6,
        verification = VerificationLevel.FULL
    ),
    COMPLETE(
        components = BackupComponent.values().toSet(),
        compression = 9,
        verification = VerificationLevel.PARANOID
    ),
    CLOUD_OPTIMIZED(
        components = setOf(BackupComponent.DATA, BackupComponent.EXTERNAL_DATA),
        compression = 9,
        verification = VerificationLevel.FULL
    )
}

enum class VerificationLevel {
    QUICK,    // SHA256 only
    FULL,     // SHA256 + tar integrity
    PARANOID  // SHA256 + tar + Merkle tree + file count
}

@dagger.hilt.android.lifecycle.HiltViewModel
class RestoreSimulationViewModel @javax.inject.Inject constructor() : androidx.lifecycle.ViewModel() {
    fun simulateRestore(snapshot: com.obsidianbackup.model.BackupSnapshot): kotlinx.coroutines.flow.Flow<RestoreImpactReport> {
        return kotlinx.coroutines.flow.flow {
            emit(RestoreImpactReport(
                appsToInstall = emptyList(),
                appsToOverwrite = emptyList(),
                dataToReplace = emptyMap(),
                permissionsRequired = emptyList(),
                selinuxWarnings = emptyList(),
                estimatedTime = kotlin.time.Duration.ZERO,
                riskLevel = RiskLevel.LOW
            ))
        }
    }
    
    fun dismissDialog() {
    }
    
    fun proceedWithRestore(snapshot: com.obsidianbackup.model.BackupSnapshot) {
    }
}
