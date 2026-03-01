// ui/screens/GamingBackupScreen.kt
package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.Arrangement
import com.obsidianbackup.ui.theme.Spacing
import com.obsidianbackup.ui.theme.Elevation
import com.obsidianbackup.ui.theme.IconSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.obsidianbackup.gaming.GamingBackupManager
import com.obsidianbackup.gaming.models.*
import com.obsidianbackup.presentation.gaming.GamingBackupViewModel
import com.obsidianbackup.ui.components.EnhancedCard
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamingBackupScreen(
    viewModel: GamingBackupViewModel = hiltViewModel(),
) {
    val detectedEmulators by viewModel.detectedEmulators.collectAsState()
    val backupProgress by viewModel.backupProgress.collectAsState()
    val scope = rememberCoroutineScope()
    
    var showBackupDialog by remember { mutableStateOf(false) }
    var selectedEmulator by remember { mutableStateOf<DetectedEmulator?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gaming Backup") },
                actions = {
                    IconButton(onClick = {
                        scope.launch { viewModel.scanForEmulators() }
                    }) {
                        Icon(Icons.Default.Refresh, "Scan for emulators")
                    }
                }
            )
        },
        floatingActionButton = {
            if (detectedEmulators.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showBackupDialog = true },
                    icon = { Icon(Icons.Default.Backup, null) },
                    text = { Text("Backup Games") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Progress indicator
            when (val progress = backupProgress) {
                is GamingBackupProgress.Scanning -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Scanning for emulators...",
                        modifier = Modifier.padding(Spacing.md)
                    )
                }
                is GamingBackupProgress.Backing -> {
                    LinearProgressIndicator(
                        progress = { progress.current.toFloat() / progress.total },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Backing up ${progress.current} of ${progress.total} games",
                        modifier = Modifier.padding(Spacing.md)
                    )
                }
                is GamingBackupProgress.Completed -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null)
                            Spacer(Modifier.width(Spacing.xs))
                            Text(
                                "Backup completed: ${progress.successful}/${progress.total} successful",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                is GamingBackupProgress.Error -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, null)
                            Spacer(Modifier.width(Spacing.xs))
                            Text(
                                progress.message,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                else -> {}
            }
            
            // Emulator list
            if (detectedEmulators.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        Icon(
                            Icons.Default.SportsEsports,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "No emulators detected",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            "Install an emulator and tap Scan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { 
                            scope.launch { viewModel.scanForEmulators() }
                        }) {
                            Icon(Icons.Default.Search, null)
                            Text("Scan for Emulators")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    items(detectedEmulators) { emulator ->
                        EmulatorCard(
                            emulator = emulator,
                            onClick = {
                                selectedEmulator = emulator
                                showBackupDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    if (showBackupDialog && selectedEmulator != null) {
        BackupOptionsDialog(
            emulator = selectedEmulator!!,
            onDismiss = { 
                showBackupDialog = false
                selectedEmulator = null
            },
            onConfirm = { options ->
                scope.launch {
                    viewModel.backupEmulator(selectedEmulator!!, options)
                    showBackupDialog = false
                    selectedEmulator = null
                }
            }
        )
    }
}

@Composable
fun EmulatorCard(
    emulator: DetectedEmulator,
    onClick: () -> Unit
) {
    val emulatorIcon = when {
        emulator.name.contains("RetroArch", ignoreCase = true) -> Icons.Default.Gamepad
        emulator.name.contains("Dolphin", ignoreCase = true) -> Icons.Default.SportsEsports
        emulator.name.contains("PPSSPP", ignoreCase = true) -> Icons.Default.Games
        emulator.name.contains("DuckStation", ignoreCase = true) -> Icons.Default.SportsEsports
        else -> Icons.Default.SportsEsports
    }
    
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    EnhancedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Emulator icon
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(IconSize.xlarge)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                emulatorIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(IconSize.large)
                            )
                        }
                    }
                    
                    Column {
                        Text(
                            emulator.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Version ${emulator.version}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            emulator.supportedPlatforms.take(3).joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            HorizontalDivider()
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // Info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tap to configure backup",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

@Composable
fun BackupOptionsDialog(
    emulator: DetectedEmulator,
    onDismiss: () -> Unit,
    onConfirm: (BackupOptions) -> Unit
) {
    var includeSaves by remember { mutableStateOf(true) }
    var includeRoms by remember { mutableStateOf(false) }
    var includeSaveStates by remember { mutableStateOf(true) }
    var cloudSync by remember { mutableStateOf(false) }
    var compression by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup ${emulator.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    "Select what to backup:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Save Files")
                    Switch(checked = includeSaves, onCheckedChange = { includeSaves = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ROM Files")
                    Switch(checked = includeRoms, onCheckedChange = { includeRoms = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Save States")
                    Switch(checked = includeSaveStates, onCheckedChange = { includeSaveStates = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cloud Sync")
                    Switch(checked = cloudSync, onCheckedChange = { cloudSync = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Compression")
                    Switch(checked = compression, onCheckedChange = { compression = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        BackupOptions(
                            includeSaves = includeSaves,
                            includeRoms = includeRoms,
                            includeSaveStates = includeSaveStates,
                            cloudSync = cloudSync,
                            compression = compression
                        )
                    )
                    onDismiss()
                }
            ) {
                Text("Start Backup")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


