// ui/screens/AutomationScreen.kt
package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import com.obsidianbackup.ui.theme.Spacing
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obsidianbackup.automation.BackupFrequency
import com.obsidianbackup.model.AppId
import com.obsidianbackup.model.BackupComponent
import com.obsidianbackup.permissions.PermissionManager
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AutomationScreen(
    permissionManager: PermissionManager,
    viewModel: AutomationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.handleIntent(AutomationIntent.ShowCreateDialog) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Schedule")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = "Scheduled Backups",
                style = MaterialTheme.typography.titleLarge
            )

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.schedules.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.xl),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No schedules configured\nTap + to create one",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    items(state.schedules) { schedule ->
                        ScheduleCard(
                            schedule = schedule,
                            onToggle = { enabled ->
                                viewModel.handleIntent(AutomationIntent.ToggleSchedule(schedule.id, enabled))
                            },
                            onDelete = {
                                viewModel.handleIntent(AutomationIntent.DeleteSchedule(schedule.id))
                            }
                        )
                    }
                }
            }

            state.error?.let { error ->
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (state.showCreateDialog) {
        CreateScheduleDialog(
            availableApps = state.availableApps,
            onDismiss = { viewModel.handleIntent(AutomationIntent.HideCreateDialog) },
            onCreate = { name, frequency, hour, minute, appIds, components, charging, wifi ->
                viewModel.handleIntent(
                    AutomationIntent.CreateSchedule(
                        name, frequency, hour, minute, appIds, components, charging, wifi
                    )
                )
            },
            onToggleApp = { appId ->
                viewModel.handleIntent(AutomationIntent.ToggleAppSelection(appId))
            }
        )
    }
}

@Composable
fun ScheduleCard(
    schedule: com.obsidianbackup.data.repository.Schedule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = schedule.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${schedule.frequency.name} at ${String.format("%02d:%02d", schedule.hour, schedule.minute)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = schedule.enabled,
                    onCheckedChange = onToggle
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = "${schedule.appIds.size} app(s) • ${schedule.components.size} component(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            schedule.nextRun?.let { nextRun ->
                val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                Text(
                    text = "Next run: ${dateFormat.format(Date(nextRun))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (schedule.requiresCharging || schedule.requiresWifi) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Conditions: ${listOfNotNull(
                        if (schedule.requiresCharging) "Charging" else null,
                        if (schedule.requiresWifi) "WiFi" else null
                    ).joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Schedule",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScheduleDialog(
    availableApps: List<AppInfo>,
    onDismiss: () -> Unit,
    onCreate: (String, BackupFrequency, Int, Int, List<AppId>, Set<BackupComponent>, Boolean, Boolean) -> Unit,
    onToggleApp: (AppId) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(BackupFrequency.DAILY) }
    var hour by remember { mutableStateOf(2) }
    var minute by remember { mutableStateOf(0) }
    var requiresCharging by remember { mutableStateOf(true) }
    var requiresWifi by remember { mutableStateOf(false) }
    var includeApk by remember { mutableStateOf(true) }
    var includeData by remember { mutableStateOf(true) }
    var allAppsSelected by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Schedule") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Schedule Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Frequency", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        BackupFrequency.values().forEach { freq ->
                            FilterChip(
                                selected = frequency == freq,
                                onClick = { frequency = freq },
                                label = { Text(freq.name) }
                            )
                        }
                    }
                }

                item {
                    Text("Time", style = MaterialTheme.typography.labelLarge)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = hour.toString(),
                            onValueChange = { hour = it.toIntOrNull()?.coerceIn(0, 23) ?: hour },
                            label = { Text("Hour") },
                            modifier = Modifier.weight(1f)
                        )
                        Text(":")
                        OutlinedTextField(
                            value = minute.toString(),
                            onValueChange = { minute = it.toIntOrNull()?.coerceIn(0, 59) ?: minute },
                            label = { Text("Min") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Text("Components", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        FilterChip(
                            selected = includeApk,
                            onClick = { includeApk = !includeApk },
                            label = { Text("APK") }
                        )
                        FilterChip(
                            selected = includeData,
                            onClick = { includeData = !includeData },
                            label = { Text("Data") }
                        )
                    }
                }

                item {
                    Text("Apps", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("All Apps", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = allAppsSelected,
                            onCheckedChange = { allAppsSelected = it }
                        )
                    }
                }

                item {
                    Text("Conditions", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Requires Charging")
                        Switch(checked = requiresCharging, onCheckedChange = { requiresCharging = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Requires WiFi")
                        Switch(checked = requiresWifi, onCheckedChange = { requiresWifi = it })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        val selectedApps = if (allAppsSelected) {
                            availableApps.map { it.id }
                        } else {
                            availableApps.filter { it.isSelected }.map { it.id }
                        }

                        val components = mutableSetOf<BackupComponent>()
                        if (includeApk) components.add(BackupComponent.APK)
                        if (includeData) components.add(BackupComponent.DATA)

                        onCreate(
                            name,
                            frequency,
                            hour,
                            minute,
                            selectedApps,
                            components,
                            requiresCharging,
                            requiresWifi
                        )
                    }
                },
                enabled = name.isNotBlank() && (includeApk || includeData)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
