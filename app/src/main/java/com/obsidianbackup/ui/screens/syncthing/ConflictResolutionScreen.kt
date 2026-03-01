// ui/screens/syncthing/ConflictResolutionScreen.kt
package com.obsidianbackup.ui.screens.syncthing

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obsidianbackup.sync.SyncthingManager
import com.obsidianbackup.sync.models.ConflictResolution
import com.obsidianbackup.sync.models.SyncConflict
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Conflict resolution UI for manual reconciliation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflictResolutionScreen(
    syncthingManager: SyncthingManager,
    onNavigateBack: () -> Unit
) {
    val conflicts by syncthingManager.conflicts.collectAsState()
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resolve Conflicts") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (conflicts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "No conflicts to resolve",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "All files are in sync",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${conflicts.size} conflict${if (conflicts.size > 1) "s" else ""} found",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                
                items(conflicts) { conflict ->
                    ConflictCard(
                        conflict = conflict,
                        onResolve = { resolution ->
                            scope.launch {
                                syncthingManager.resolveConflict(conflict.id, resolution)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConflictCard(
    conflict: SyncConflict,
    onResolve: (ConflictResolution) -> Unit
) {
    var showResolutionDialog by remember { mutableStateOf(false) }
    var selectedResolution by remember { mutableStateOf<ConflictResolution?>(null) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // File path
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = conflict.filePath.split("/").last(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = conflict.filePath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            HorizontalDivider()
            
            // Local version
            VersionInfo(
                title = "Local Version",
                size = conflict.localSize,
                modified = conflict.localModified
            )
            
            // Remote version
            VersionInfo(
                title = "Remote Version",
                size = conflict.remoteSize,
                modified = conflict.remoteModified
            )
            
            HorizontalDivider()
            
            // Resolution buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        selectedResolution = ConflictResolution.KEEP_LOCAL
                        showResolutionDialog = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Keep Local", maxLines = 1)
                }
                
                OutlinedButton(
                    onClick = {
                        selectedResolution = ConflictResolution.KEEP_REMOTE
                        showResolutionDialog = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Keep Remote", maxLines = 1)
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        selectedResolution = ConflictResolution.KEEP_BOTH
                        showResolutionDialog = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Keep Both", maxLines = 1)
                }
                
                OutlinedButton(
                    onClick = {
                        selectedResolution = ConflictResolution.MERGE_MANUAL
                        showResolutionDialog = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Manual Merge", maxLines = 1)
                }
            }
        }
    }
    
    // Confirmation dialog
    if (showResolutionDialog && selectedResolution != null) {
        AlertDialog(
            onDismissRequest = { showResolutionDialog = false },
            title = { Text("Confirm Resolution") },
            text = {
                Text(getResolutionDescription(selectedResolution!!))
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResolve(selectedResolution!!)
                        showResolutionDialog = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResolutionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun VersionInfo(
    title: String,
    size: Long,
    modified: Long
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Size: ${formatBytes(size)}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Modified: ${formatDate(modified)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun getResolutionDescription(resolution: ConflictResolution): String {
    return when (resolution) {
        ConflictResolution.KEEP_LOCAL -> 
            "Keep the local version and discard the remote version."
        ConflictResolution.KEEP_REMOTE -> 
            "Keep the remote version and discard the local version."
        ConflictResolution.KEEP_BOTH -> 
            "Keep both versions with different names."
        ConflictResolution.MERGE_MANUAL -> 
            "Create backups of both versions (.local and .remote) for manual merging."
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

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
