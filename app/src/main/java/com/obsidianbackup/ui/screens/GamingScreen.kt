// ui/screens/GamingScreen.kt
package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obsidianbackup.gaming.models.DetectedEmulator
import com.obsidianbackup.gaming.models.GameInfo
import com.obsidianbackup.gaming.models.GamingBackupProgress
import com.obsidianbackup.presentation.gaming.GamingViewModel

@Composable
fun GamingScreen(
    viewModel: GamingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val backupProgress by viewModel.backupProgress.collectAsState()
    val detectedEmulators by viewModel.detectedEmulators.collectAsState()

    if (!uiState.featureEnabled) {
            ComingSoonMessage(
                padding = PaddingValues(0.dp),
                title = "Gaming Backups",
                description = "Save data backup for emulators and mobile games.\n\nThis feature will be available in version 1.1 with support for:\n• Play Games Cloud Sync\n• Emulator save state backups\n• ROM file management\n• Game save data preservation",
                icon = Icons.Default.SportsEsports
            )
        } else {
            GamingContent(
                padding = PaddingValues(0.dp),
                uiState = uiState,
                backupProgress = backupProgress,
                emulators = detectedEmulators,
                onEmulatorSelected = viewModel::selectEmulator,
                onGameToggled = viewModel::toggleGameSelection,
                onBackup = viewModel::backupSelectedGames,
                onToggleRoms = viewModel::toggleIncludeRoms
            )
        }
}

@Composable
private fun GamingContent(
    padding: PaddingValues,
    uiState: com.obsidianbackup.presentation.gaming.GamingUiState,
    backupProgress: GamingBackupProgress,
    emulators: List<DetectedEmulator>,
    onEmulatorSelected: (DetectedEmulator) -> Unit,
    onGameToggled: (GameInfo) -> Unit,
    onBackup: () -> Unit,
    onToggleRoms: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
    ) {
        if (uiState.isScanning) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("Scanning for emulators...", modifier = Modifier.padding(top = 8.dp))
        } else if (emulators.isEmpty()) {
            EmptyState("No emulators detected")
        } else {
            Text("Detected Emulators (${emulators.size})", style = MaterialTheme.typography.titleLarge)
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(emulators) { emulator ->
                    EmulatorCard(
                        emulator = emulator,
                        isSelected = uiState.selectedEmulator == emulator,
                        onClick = { onEmulatorSelected(emulator) }
                    )
                }
            }

            if (uiState.selectedEmulator != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Checkbox(
                        checked = uiState.includeRoms,
                        onCheckedChange = onToggleRoms
                    )
                    Text("Include ROM files")
                }

                Button(
                    onClick = onBackup,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.selectedGames.isNotEmpty()
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Backup ${uiState.selectedGames.size} game(s)")
                }
            }

            when (backupProgress) {
                is GamingBackupProgress.Backing -> {
                    LinearProgressIndicator(
                        progress = { backupProgress.current.toFloat() / backupProgress.total },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    Text("Backing up ${backupProgress.current}/${backupProgress.total}")
                }
                is GamingBackupProgress.Error -> {
                    Text(
                        text = "Error: ${backupProgress.message}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun EmulatorCard(
    emulator: DetectedEmulator,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(emulator.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    emulator.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onClick) {
                Icon(
                    if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null
                )
            }
        }
    }
}

/**
 * Coming Soon message for features under development
 */
@Composable
internal fun ComingSoonMessage(
    padding: PaddingValues,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "This feature is currently in development and will be available soon.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
internal fun DisabledFeatureMessage(padding: PaddingValues, featureName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Feature disabled",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "$featureName feature is disabled",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Enable it in Feature Flags settings",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = "Empty state",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
