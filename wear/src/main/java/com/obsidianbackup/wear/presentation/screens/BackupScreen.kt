package com.obsidianbackup.wear.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material3.*
import com.obsidianbackup.wear.presentation.viewmodel.WearViewModel
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.google.android.horologist.compose.material.Chip
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main backup control screen
 */
@Composable
fun BackupScreen(
    onNavigateToStatus: () -> Unit,
    onNavigateToProgress: () -> Unit,
    viewModel: WearViewModel = hiltViewModel()
) {
    val backupStatus by viewModel.backupStatus.collectAsState()
    val isPhoneConnected by viewModel.isPhoneConnected.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        columnState = ScalingLazyColumnDefaults.responsive().create(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ListHeader {
                Text(
                    text = "ObsidianBackup",
                    textAlign = TextAlign.Center
                )
            }
        }

        if (!isPhoneConnected) {
            item {
                Text(
                    text = "Phone not connected",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        item {
            Chip(
                label = {
                    Text(
                        text = if (backupStatus.isRunning) "Backup Running" else "Start Backup",
                        maxLines = 2,
                        textAlign = TextAlign.Center
                    )
                },
                onClick = {
                    if (backupStatus.isRunning) {
                        onNavigateToProgress()
                    } else {
                        viewModel.triggerBackup()
                    }
                },
                enabled = isPhoneConnected && !isLoading,
                colors = ChipDefaults.primaryChipColors()
            )
        }

        if (backupStatus.isRunning) {
            item {
                Chip(
                    label = { Text("Cancel Backup") },
                    onClick = { viewModel.cancelBackup() },
                    enabled = isPhoneConnected,
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
        }

        item {
            Chip(
                label = { Text("View Status") },
                onClick = onNavigateToStatus,
                enabled = isPhoneConnected,
                colors = ChipDefaults.secondaryChipColors()
            )
        }

        if (backupStatus.lastBackupTime > 0) {
            item {
                val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                Text(
                    text = "Last: ${dateFormat.format(Date(backupStatus.lastBackupTime))}",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
