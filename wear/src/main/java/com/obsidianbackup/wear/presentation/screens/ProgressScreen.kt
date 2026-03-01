package com.obsidianbackup.wear.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material3.*
import com.obsidianbackup.wear.presentation.viewmodel.WearViewModel
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults

/**
 * Real-time backup progress screen
 */
@Composable
fun ProgressScreen(
    viewModel: WearViewModel = hiltViewModel()
) {
    val backupProgress by viewModel.backupProgress.collectAsState()
    val backupStatus by viewModel.backupStatus.collectAsState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        columnState = ScalingLazyColumnDefaults.responsive().create(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ListHeader {
                Text(text = "Backup Progress")
            }
        }

        if (!backupStatus.isRunning) {
            item {
                Text(
                    text = "No backup running",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            item {
                CircularProgressIndicator(
                    progress = backupProgress.percentage / 100f,
                    modifier = Modifier
                        .size(100.dp)
                        .padding(16.dp),
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                )
            }

            item {
                Text(
                    text = "${backupProgress.percentage}%",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            }

            if (backupProgress.status.isNotEmpty()) {
                item {
                    Text(
                        text = backupProgress.status,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            item {
                TitleCard(title = { Text("Files") }) {
                    Text(
                        text = "${backupProgress.filesProcessed} / ${backupProgress.totalFiles}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (backupProgress.currentFile.isNotEmpty()) {
                item {
                    TitleCard(title = { Text("Current File") }) {
                        Text(
                            text = backupProgress.currentFile,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                Chip(
                    label = { Text("Cancel") },
                    onClick = { viewModel.cancelBackup() },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
        }
    }
}
