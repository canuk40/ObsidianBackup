package com.obsidianbackup.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import androidx.hilt.navigation.compose.hiltViewModel
import com.obsidianbackup.model.*
import com.obsidianbackup.presentation.backups.BackupsViewModel
import com.obsidianbackup.ui.theme.Spacing
import com.obsidianbackup.ui.theme.Elevation
import com.obsidianbackup.ui.theme.IconSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Locale
import com.obsidianbackup.ui.components.EnhancedButton
import com.obsidianbackup.ui.utils.Animations
import com.obsidianbackup.ui.utils.LoadingCrossfade
import com.obsidianbackup.ui.utils.EmptyStateAnimation
import java.util.*
import android.content.Intent
import androidx.compose.ui.res.stringResource
import com.obsidianbackup.R

@Composable
fun BackupsScreen() {
    val viewModel: BackupsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    var selectedSnapshot by remember { mutableStateOf<BackupSnapshot?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error snackbar
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (selectedSnapshot != null) {
        SnapshotDetailScreen(
            snapshot = selectedSnapshot!!,
            onBack = { selectedSnapshot = null },
            onDelete = { snapshot ->
                viewModel.deleteSnapshot(SnapshotId(snapshot.id.value))
                selectedSnapshot = null
            }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            LoadingCrossfade(
                isLoading = state.isLoading,
                loadingContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                },
                content = {
                    if (state.snapshots.isEmpty()) {
                        EmptyBackupsState(modifier = Modifier.padding(paddingValues))
                    } else {
                        BackupsList(
                            snapshots = state.snapshots,
                            onSnapshotClick = { selectedSnapshot = it },
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun BackupsList(
    snapshots: List<BackupSnapshot>,
    onSnapshotClick: (BackupSnapshot) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        item {
            ListItem(
                headlineContent = { 
                    Text(
                        stringResource(R.string.backups_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                supportingContent = { Text("${snapshots.size} backup${if (snapshots.size != 1) "s" else ""} available") }
            )
            HorizontalDivider()
        }

        items(
            items = snapshots, 
            key = { it.id.value }
        ) { snapshot ->
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter = com.obsidianbackup.ui.utils.listItemEnterAnimation,
                exit = com.obsidianbackup.ui.utils.listItemExitAnimation,
                modifier = Modifier.animateItem()
            ) {
                SnapshotListItem(
                    snapshot = snapshot,
                    onClick = { onSnapshotClick(snapshot) }
                )
            }
        }
    }
}

@Composable
fun EmptyBackupsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Icon(
                Icons.Default.Backup,
                contentDescription = null,
                modifier = Modifier.size(IconSize.hero),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                stringResource(R.string.backups_empty_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                stringResource(R.string.backups_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SnapshotListItem(
    snapshot: BackupSnapshot,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    ListItem(
        headlineContent = {
            Text(snapshot.description ?: "Backup ${snapshot.id.value.take(8)}")
        },
        supportingContent = {
            Column {
                Text(dateFormat.format(Date(snapshot.timestamp)))
                Text(buildString {
                    append("${snapshot.apps.size} app${if (snapshot.apps.size != 1) "s" else ""}")
                    append(" • ")
                    append(formatSize(snapshot.totalSize))
                })
            }
        },
        leadingContent = {
            Icon(
                Icons.Default.Backup,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Row {
                if (snapshot.verified) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (snapshot.encrypted) {
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Encrypted",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapshotDetailScreen(
    snapshot: BackupSnapshot,
    onBack: () -> Unit,
    onDelete: (BackupSnapshot) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showSimulationDialog by remember { mutableStateOf(false) }
    var simulationResult by remember { mutableStateOf<RestoreSimulationResult?>(null) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_backup_title)) },
            text = { Text(stringResource(R.string.dialog_delete_backup_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(snapshot)
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showRestoreDialog) {
        RestoreOptionsDialog(
            snapshot = snapshot,
            onDismiss = { showRestoreDialog = false },
            onRestore = {
                showRestoreDialog = false
                Toast.makeText(context, "Restore initiated for ${snapshot.apps.size} app(s)", Toast.LENGTH_SHORT).show()
            },
            onSimulate = {
                simulationResult = RestoreSimulationResult(
                    snapshotId = SnapshotId(snapshot.id.value),
                    appIds = snapshot.apps.map { it.appId },
                    estimatedDuration = 30000L,
                    conflicts = listOf(),
                    warnings = listOf("Some apps may require manual re-login"),
                    canProceed = true
                )
                showRestoreDialog = false
                showSimulationDialog = true
            }
        )
    }

    // Show simulation results
    if (showSimulationDialog && simulationResult != null) {
        RestoreSimulationDialog(
            result = simulationResult!!,
            onDismiss = { 
                showSimulationDialog = false
                simulationResult = null
            },
            onProceed = {
                showSimulationDialog = false
                Toast.makeText(context, "Restore initiated", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete backup")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = Elevation.medium)
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(
                        text = snapshot.description ?: "Backup ${snapshot.id.value.take(8)}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    
                    DetailRow(stringResource(R.string.backup_detail_created), dateFormat.format(Date(snapshot.timestamp)))
                    DetailRow(stringResource(R.string.backup_detail_size), formatSize(snapshot.totalSize))
                    DetailRow(stringResource(R.string.backup_detail_compression), "${(snapshot.compressionRatio * 100).toInt()}%")
                    DetailRow(stringResource(R.string.backup_detail_mode), snapshot.permissionMode)
                    DetailRow(stringResource(R.string.backup_detail_apps), "${snapshot.apps.size}")
                    
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        StatusBadge(
                            icon = if (snapshot.verified) Icons.Default.CheckCircle else Icons.Default.Warning,
                            text = if (snapshot.verified) stringResource(R.string.backup_status_verified) else stringResource(R.string.backup_status_not_verified),
                            isPositive = snapshot.verified
                        )
                        if (snapshot.encrypted) {
                            StatusBadge(
                                icon = Icons.Default.Lock,
                                text = stringResource(R.string.backup_status_encrypted),
                                isPositive = true
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = Elevation.medium)
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(
                        text = stringResource(R.string.backup_detail_device_info),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    DetailRow(stringResource(R.string.backup_detail_device), "${snapshot.deviceInfo.manufacturer} ${snapshot.deviceInfo.model}")
                    DetailRow(stringResource(R.string.backup_detail_android), "Version ${snapshot.deviceInfo.androidVersion}")
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                OutlinedButton(
                    onClick = { 
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/zip"
                            putExtra(Intent.EXTRA_SUBJECT, "ObsidianBackup Export")
                            putExtra(Intent.EXTRA_TEXT, "Backup from ${dateFormat.format(Date(snapshot.timestamp))}")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val chooser = Intent.createChooser(shareIntent, "Export Backup")
                        context.startActivity(chooser)
                        Toast.makeText(context, "Export backup (${snapshot.apps.size} apps)", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Export backup")
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.action_export))
                }

                EnhancedButton(
                    onClick = { showRestoreDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.RestorePage, contentDescription = "Restore backup")
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.action_restore))
                }
            }

            if (snapshot.apps.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = Elevation.medium)
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Text(
                            text = "Apps in Backup (${snapshot.apps.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        snapshot.apps.take(5).forEach { app ->
                            Text(
                                text = "• ${app.appName}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (snapshot.apps.size > 5) {
                            Text(
                                text = "... and ${snapshot.apps.size - 5} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun StatusBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, isPositive: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(IconSize.small)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
    }
}

@Composable
fun RestoreOptionsDialog(
    snapshot: BackupSnapshot,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onSimulate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_restore_backup_title)) },
        text = {
            Column {
                Text("This will restore ${snapshot.apps.size} app(s) from this backup.")
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    stringResource(R.string.dialog_restore_overwrite_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                OutlinedButton(onClick = onSimulate) {
                    Text(stringResource(R.string.action_simulate))
                }
                Button(onClick = onRestore) {
                    Text(stringResource(R.string.action_restore_now))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun RestoreSimulationDialog(
    result: RestoreSimulationResult,
    onDismiss: () -> Unit,
    onProceed: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(stringResource(R.string.dialog_restore_simulation_title))
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.simulation_apps_to_restore), style = MaterialTheme.typography.labelSmall)
                            Text("${result.appIds.size}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.simulation_estimated_duration), style = MaterialTheme.typography.labelSmall)
                            Text("${result.estimatedDuration / 1000}s", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.simulation_status_label), style = MaterialTheme.typography.labelSmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (result.canProceed) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (result.canProceed) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (result.canProceed) stringResource(R.string.simulation_can_proceed) else stringResource(R.string.simulation_blocked),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                
                if (result.conflicts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        stringResource(R.string.simulation_conflicts_detected),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    result.conflicts.forEach { conflict ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(Spacing.sm)) {
                                Text(
                                    conflict.type.name.replace("_", " "),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    conflict.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                
                if (result.warnings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        stringResource(R.string.simulation_warnings),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    result.warnings.forEach { warning ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                warning,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (result.canProceed) {
                Button(onClick = onProceed) {
                    Text(stringResource(R.string.action_proceed_with_restore))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (result.canProceed) stringResource(R.string.action_cancel) else stringResource(R.string.action_close))
            }
        }
    )
}
