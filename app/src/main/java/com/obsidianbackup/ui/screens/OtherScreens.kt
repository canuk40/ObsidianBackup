package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.obsidianbackup.model.AppId
import android.widget.Toast

@Composable
fun AutomationScreen() {
    val context = LocalContext.current
    var scheduledBackups by remember { mutableStateOf(listOf<ScheduledBackup>()) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Automation", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { showAddDialog = true }) {
            Text("Add Scheduled Backup")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(scheduledBackups) { backup ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Backup at ${backup.hour}:${backup.minute}")
                        Text("Days: ${backup.days.joinToString(", ")}")
                        Text("Apps: ${backup.appIds.size} selected")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddScheduleDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { schedule ->
                scheduledBackups = scheduledBackups + schedule
                showAddDialog = false
            }
        )
    }
}

data class ScheduledBackup(
    val hour: Int,
    val minute: Int,
    val days: List<String>,
    val appIds: List<AppId>
)

@Composable
fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onAdd: (ScheduledBackup) -> Unit
) {
    var hour by remember { mutableStateOf(9) }
    var minute by remember { mutableStateOf(0) }
    var selectedDays by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Scheduled Backup") },
        text = {
            Column {
                Row {
                    Text("Time: ")
                    NumberPicker(value = hour, onValueChange = { hour = it }, range = 0..23)
                    Text(":")
                    NumberPicker(value = minute, onValueChange = { minute = it }, range = 0..59)
                }

                Text("Days:")
                Row {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                        Checkbox(
                            checked = day in selectedDays,
                            onCheckedChange = {
                                selectedDays = if (it) selectedDays + day else selectedDays - day
                            }
                        )
                        Text(day)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onAdd(ScheduledBackup(hour, minute, selectedDays.toList(), emptyList()))
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange
) {
    var text by remember { mutableStateOf(value.toString()) }

    TextField(
        value = text,
        onValueChange = {
            text = it
            it.toIntOrNull()?.let { num ->
                if (num in range) onValueChange(num)
            }
        },
        modifier = Modifier.width(60.dp)
    )
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var zstdLevel by remember { mutableStateOf(3) }
    var aesEnabled by remember { mutableStateOf(false) }
    var retentionCount by remember { mutableStateOf(10) }
    var forceSAF by remember { mutableStateOf(false) }
    var customFlags by remember { mutableStateOf("") }
    var showOAuthDialog by remember { mutableStateOf(false) }
    var showCustomFlagsDialog by remember { mutableStateOf(false) }

    if (showOAuthDialog) {
        AlertDialog(
            onDismissRequest = { showOAuthDialog = false },
            title = { Text("Add Cloud Account") },
            text = { Text("OAuth flow would launch here. Select a provider: Google Drive, Dropbox, or OneDrive.") },
            confirmButton = {
                TextButton(onClick = { 
                    showOAuthDialog = false
                    Toast.makeText(context, "OAuth flow - Coming Soon", Toast.LENGTH_SHORT).show()
                }) {
                    Text("OK")
                }
            }
        )
    }

    if (showCustomFlagsDialog) {
        AlertDialog(
            onDismissRequest = { showCustomFlagsDialog = false },
            title = { Text("Custom BusyBox Flags") },
            text = {
                Column {
                    Text("Add custom flags for tar, rsync, zstd:")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = customFlags,
                        onValueChange = { customFlags = it },
                        placeholder = { Text("e.g., --exclude=*.tmp") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    showCustomFlagsDialog = false
                    Toast.makeText(context, "Custom flags saved", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomFlagsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Compression Profiles", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Zstd Level: $zstdLevel", modifier = Modifier.weight(1f))
                    Button(onClick = { if (zstdLevel > 1) zstdLevel-- }) { Text("-") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { if (zstdLevel < 22) zstdLevel++ }) { Text("+") }
                }
                Text(
                    "Level 1 = fastest, Level 22 = best compression",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Encryption Settings", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AES-256-GCM Encryption", modifier = Modifier.weight(1f))
                    Switch(
                        checked = aesEnabled,
                        onCheckedChange = { 
                            aesEnabled = it
                            Toast.makeText(context, "AES-256-GCM ${if (it) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Cloud Providers", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showOAuthDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Cloud Account")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Retention Policies", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Keep $retentionCount backups", modifier = Modifier.weight(1f))
                    Button(onClick = { if (retentionCount > 1) retentionCount-- }) { Text("-") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { if (retentionCount < 999) retentionCount++ }) { Text("+") }
                }
                Text(
                    "Older backups will be automatically deleted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Permission Mode Preferences", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Force SAF Mode", modifier = Modifier.weight(1f))
                    Switch(
                        checked = forceSAF,
                        onCheckedChange = { 
                            forceSAF = it
                            Toast.makeText(context, "SAF mode ${if (it) "forced" else "auto"}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                Text(
                    "Use Storage Access Framework even if root/Shizuku available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Advanced BusyBox Options", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showCustomFlagsDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Configure Custom Flags")
                }
                if (customFlags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Current: $customFlags",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Debug/Diagnostic Options", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        Toast.makeText(context, "Exporting diagnostics bundle...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export Logs Bundle")
                }
                Text(
                    "Includes app logs, shell audit, and system info",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
