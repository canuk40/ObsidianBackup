// ui/screens/SmartSchedulingScreen.kt
package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obsidianbackup.model.TimeWindow
import com.obsidianbackup.model.BackupFrequency
import com.obsidianbackup.ui.theme.Spacing
import com.obsidianbackup.ui.viewmodel.SmartSchedulingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSchedulingScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SmartSchedulingViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val nextBackup by viewModel.nextBackup.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    "AI-Powered Scheduling",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Phase 1: Heuristic-based optimization",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "The system analyzes patterns and schedules backups during idle times—when your device is charging, connected to Wi-Fi, and not in active use.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Enable/Disable Toggle
            item {
                Card {
                    ListItem(
                        headlineContent = { Text("Enable Smart Scheduling") },
                        supportingContent = {
                            Text(if (config.enabled) "Active" else "Disabled")
                        },
                        leadingContent = {
                            Icon(
                                if (config.enabled) Icons.Default.Check else Icons.Default.Schedule,
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = config.enabled,
                                onCheckedChange = { enabled ->
                                    viewModel.updateConfig(config.copy(enabled = enabled))
                                },
                                enabled = !isLoading
                            )
                        }
                    )
                }
            }
            
            // Next Backup Preview
            if (config.enabled && nextBackup != null) {
                item {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    "Next Scheduled Backup",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                viewModel.getNextBackupTime(),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                viewModel.getNextBackupRelative(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            nextBackup?.let { prediction ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Reason: ${prediction.reason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    "Confidence: ${(prediction.confidence * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
            
            if (config.enabled) {
                // Time Window Selection
                item {
                    Text(
                        "Preferred Time Window",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                item {
                    Card {
                        Column {
                            TimeWindow.values().forEach { window ->
                                ListItem(
                                    headlineContent = { Text(window.displayName) },
                                    supportingContent = {
                                        if (window == TimeWindow.AUTO) {
                                            Text("Automatically predict optimal time")
                                        } else {
                                            Text("${window.startHour}:00 - ${window.endHour}:00")
                                        }
                                    },
                                    leadingContent = {
                                        RadioButton(
                                            selected = config.preferredTimeWindow == window,
                                            onClick = {
                                                viewModel.updateConfig(
                                                    config.copy(preferredTimeWindow = window)
                                                )
                                            }
                                        )
                                    },
                                    modifier = Modifier.selectable(
                                        selected = config.preferredTimeWindow == window,
                                        onClick = {
                                            viewModel.updateConfig(
                                                config.copy(preferredTimeWindow = window)
                                            )
                                        }
                                    )
                                )
                                if (window != TimeWindow.values().last()) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
                
                // Frequency Selection
                item {
                    Text(
                        "Backup Frequency",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                item {
                    Card {
                        Column {
                            BackupFrequency.values().forEach { frequency ->
                                ListItem(
                                    headlineContent = { Text(frequency.displayName) },
                                    leadingContent = {
                                        RadioButton(
                                            selected = config.backupFrequency == frequency,
                                            onClick = {
                                                viewModel.updateConfig(
                                                    config.copy(backupFrequency = frequency)
                                                )
                                            }
                                        )
                                    },
                                    modifier = Modifier.selectable(
                                        selected = config.backupFrequency == frequency,
                                        onClick = {
                                            viewModel.updateConfig(
                                                config.copy(backupFrequency = frequency)
                                            )
                                        }
                                    )
                                )
                                if (frequency != BackupFrequency.values().last()) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
                
                // Constraints Section
                item {
                    Text(
                        "Backup Conditions",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                item {
                    Card {
                        Column {
                            ListItem(
                                headlineContent = { Text("Only on Wi-Fi") },
                                supportingContent = { Text("Don't use mobile data") },
                                leadingContent = {
                                    Icon(Icons.Default.Wifi, contentDescription = null)
                                },
                                trailingContent = {
                                    Switch(
                                        checked = config.onlyOnWifi,
                                        onCheckedChange = { value ->
                                            viewModel.updateConfig(config.copy(onlyOnWifi = value))
                                        }
                                    )
                                }
                            )
                            HorizontalDivider()
                            ListItem(
                                headlineContent = { Text("Only when charging") },
                                supportingContent = { Text("Preserve battery life") },
                                leadingContent = {
                                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null)
                                },
                                trailingContent = {
                                    Switch(
                                        checked = config.onlyWhenCharging,
                                        onCheckedChange = { value ->
                                            viewModel.updateConfig(config.copy(onlyWhenCharging = value))
                                        }
                                    )
                                }
                            )
                            HorizontalDivider()
                            ListItem(
                                headlineContent = { Text("Minimum battery level") },
                                supportingContent = { Text("${config.minimumBatteryLevel}%") },
                                leadingContent = {
                                    Icon(Icons.Default.Battery4Bar, contentDescription = null)
                                },
                                trailingContent = {
                                    Text("${config.minimumBatteryLevel}%")
                                }
                            )
                        }
                    }
                }
                
                // Cancel Button
                item {
                    OutlinedButton(
                        onClick = { viewModel.cancelSchedule() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel Schedule")
                    }
                }
            }
        }
}
