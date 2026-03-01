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
import java.text.SimpleDateFormat
import java.util.*

/**
 * Detailed backup status screen
 */
@Composable
fun StatusScreen(
    viewModel: WearViewModel = hiltViewModel()
) {
    val backupStatus by viewModel.backupStatus.collectAsState()
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        columnState = ScalingLazyColumnDefaults.responsive().create(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ListHeader {
                Text(text = "Backup Status")
            }
        }

        item {
            Card(
                onClick = { viewModel.requestStatus() }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (backupStatus.lastBackupSuccess) "Success" else "Failed",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (backupStatus.lastBackupSuccess) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    if (backupStatus.lastBackupTime > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dateFormat.format(Date(backupStatus.lastBackupTime)),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        item {
            TitleCard(title = { Text("Total Backups") }) {
                Text(
                    text = "${backupStatus.totalBackups}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        item {
            TitleCard(title = { Text("Backup Size") }) {
                Text(
                    text = "%.2f MB".format(backupStatus.backupSizeMB),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        if (backupStatus.nextScheduledBackup > 0) {
            item {
                TitleCard(title = { Text("Next Scheduled") }) {
                    Text(
                        text = dateFormat.format(Date(backupStatus.nextScheduledBackup)),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
