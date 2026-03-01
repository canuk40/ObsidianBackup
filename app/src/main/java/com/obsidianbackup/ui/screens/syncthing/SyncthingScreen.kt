// ui/screens/syncthing/SyncthingScreen.kt
package com.obsidianbackup.ui.screens.syncthing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obsidianbackup.sync.SyncthingManager
import com.obsidianbackup.sync.models.*

/**
 * Main Syncthing settings and status screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncthingScreen(
    syncthingManager: SyncthingManager,
    onNavigateToDevicePairing: () -> Unit,
    onNavigateToConflicts: () -> Unit,
    viewModel: SyncthingViewModel = hiltViewModel()
) {
    val syncState by syncthingManager.syncState.collectAsState()
    val devices by syncthingManager.devices.collectAsState()
    val folders by syncthingManager.folders.collectAsState()
    val syncStatus by syncthingManager.syncStatus.collectAsState()
    val conflicts by syncthingManager.conflicts.collectAsState()
    val networkPreference by syncthingManager.networkPreference.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Syncthing Sync") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            item {
                StatusCard(syncState, syncStatus)
            }
            
            // Conflicts Alert
            if (conflicts.isNotEmpty()) {
                item {
                    ConflictsAlert(
                        conflictCount = conflicts.size,
                        onClick = onNavigateToConflicts
                    )
                }
            }
            
            // Network Preference
            item {
                NetworkPreferenceCard(
                    currentPreference = networkPreference,
                    onPreferenceChange = { preference ->
                        scope.launch {
                            syncthingManager.setNetworkPreference(preference)
                        }
                    }
                )
            }
            
            // Devices Section
            item {
                Text(
                    text = "Devices",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                Button(
                    onClick = onNavigateToDevicePairing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Device")
                }
            }
            
            items(devices) { device ->
                DeviceCard(device)
            }
            
            // Folders Section
            item {
                Text(
                    text = "Shared Folders",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            items(folders) { folder ->
                FolderCard(folder, syncStatus)
            }
        }
    }
}

@Composable
private fun StatusCard(
    syncState: SyncthingState,
    syncStatus: SyncStatus
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (syncState) {
                is SyncthingState.Connected -> MaterialTheme.colorScheme.primaryContainer
                is SyncthingState.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Sync Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Icon(
                    imageVector = when (syncState) {
                        is SyncthingState.Connected -> Icons.Default.CheckCircle
                        is SyncthingState.Error -> Icons.Default.Error
                        else -> Icons.Default.Info
                    },
                    contentDescription = null
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            when (syncState) {
                is SyncthingState.Connected -> {
                    Text("Device ID: ${syncState.deviceId.take(7)}...")
                    
                    if (syncStatus is SyncStatus.Syncing) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("↑ ${formatBytes(syncStatus.uploadRate)}/s")
                        Text("↓ ${formatBytes(syncStatus.downloadRate)}/s")
                    }
                }
                is SyncthingState.Error -> {
                    Text("Error: ${syncState.message}")
                }
                SyncthingState.Disconnected -> {
                    Text("Not connected")
                }
            }
        }
    }
}

@Composable
private fun ConflictsAlert(
    conflictCount: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$conflictCount conflict${if (conflictCount > 1) "s" else ""} need resolution",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun NetworkPreferenceCard(
    currentPreference: NetworkPreference,
    onPreferenceChange: (NetworkPreference) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Network Preference",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            NetworkPreference.values().forEach { preference ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentPreference == preference,
                        onClick = { onPreferenceChange(preference) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = when (preference) {
                                NetworkPreference.WIFI_ONLY -> "WiFi Only"
                                NetworkPreference.MOBILE_DATA -> "WiFi & Mobile Data"
                                NetworkPreference.ALWAYS -> "Always"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = when (preference) {
                                NetworkPreference.WIFI_ONLY -> "Sync only when connected to WiFi"
                                NetworkPreference.MOBILE_DATA -> "Sync on WiFi and mobile data"
                                NetworkPreference.ALWAYS -> "Sync on any network"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(device: SyncthingDevice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (device.connected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = device.deviceId.take(7) + "...",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = if (device.connected) "Connected" else "Disconnected",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (device.connected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            Icon(
                imageVector = if (device.connected) {
                    Icons.Default.PhoneAndroid
                } else {
                    Icons.Default.PhoneDisabled
                },
                contentDescription = null
            )
        }
    }
}

@Composable
private fun FolderCard(
    folder: SyncthingFolder,
    syncStatus: SyncStatus
) {
    val folderInfo = if (syncStatus is SyncStatus.Syncing) {
        syncStatus.folders[folder.id]
    } else null
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = folder.path,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(Icons.Default.Folder, contentDescription = null)
            }
            
            if (folderInfo != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { (folderInfo.completion / 100.0).toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${folderInfo.completion.toInt()}% synced (${formatBytes(folderInfo.needBytes)} remaining)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> "%.2f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.2f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }
}
