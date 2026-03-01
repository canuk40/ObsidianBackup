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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import com.obsidianbackup.model.*
import com.obsidianbackup.permissions.PermissionManager
import com.obsidianbackup.ui.components.*
import com.obsidianbackup.ui.utils.rememberHapticFeedback
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced BackupsScreen with microinteractions and UX improvements
 */
@Composable
fun EnhancedBackupsScreen(
    permissionManager: PermissionManager
) {
    val haptic = rememberHapticFeedback()
    var selectedSnapshot by remember { mutableStateOf<BackupSnapshot?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    val snapshotsState = remember { mutableStateOf<List<BackupSnapshot>>(emptyList()) }
    
    // Simulate loading
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        snapshotsState.value = listOf(
            BackupSnapshot(
                id = BackupId("snapshot_1"),
                timestamp = System.currentTimeMillis() - 86400000,
                description = "Daily auto-backup",
                apps = emptyList(),
                totalSize = 1024L * 1024 * 150,
                compressionRatio = 0.65f,
                encrypted = false,
                verified = true,
                permissionMode = "ROOT",
                deviceInfo = DeviceInfo(
                    model = "Pixel 8",
                    manufacturer = "Google",
                    androidVersion = 14,
                    buildFingerprint = "google/pixel8/pixel8:14/..."
                )
            )
        )
        isLoading = false
    }
    
    val snapshots = snapshotsState.value
    
    if (selectedSnapshot != null) {
        EnhancedSnapshotDetailScreen(
            snapshot = selectedSnapshot!!,
            permissionManager = permissionManager,
            onBack = {
                haptic.light()
                selectedSnapshot = null
            }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with stats
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Backup Snapshots",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (!isLoading) {
                        Text(
                            text = "${snapshots.size} snapshots • ${snapshots.sumOf { it.totalSize / 1024 / 1024 }} MB total",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Content with pull-to-refresh
            PullToRefresh(
                isRefreshing = isRefreshing,
                onRefresh = {
                    haptic.medium()
                    isRefreshing = true
                    // Note: In production, use proper coroutine scope
                }
            ) {
                when {
                    isLoading -> BackupsScreenSkeleton()
                    snapshots.isEmpty() -> {
                        NoBackupsEmptyState(
                            onCreateBackup = {
                                haptic.medium()
                                // Navigate to create backup
                            }
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(snapshots, key = { it.id.value }) { snapshot ->
                                EnhancedSnapshotListItem(
                                    snapshot = snapshot,
                                    onClick = {
                                        haptic.light()
                                        selectedSnapshot = snapshot
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Floating action button for creating new backup
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            EnhancedFloatingActionButton(
                onClick = {
                    haptic.heavy()
                    // Navigate to create backup
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create backup")
            }
        }
    }
}

@Composable
fun EnhancedSnapshotListItem(
    snapshot: BackupSnapshot,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    
    EnhancedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Backup,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Column {
                    Text(
                        text = snapshot.description ?: "Backup ${snapshot.id.value.take(8)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateFormat.format(Date(snapshot.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${snapshot.apps.size} apps • ${snapshot.totalSize / 1024 / 1024} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (snapshot.verified) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (snapshot.encrypted) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Encrypted",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSnapshotDetailScreen(
    snapshot: BackupSnapshot,
    permissionManager: PermissionManager,
    onBack: () -> Unit
) {
    val haptic = rememberHapticFeedback()
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup Details") },
                navigationIcon = {
                    EnhancedIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    EnhancedIconButton(
                        onClick = {
                            haptic.medium()
                            // Share backup
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    EnhancedIconButton(
                        onClick = {
                            haptic.heavy()
                            // Delete backup
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card
            item {
                EnhancedCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = snapshot.description ?: "Backup ${snapshot.id.value.take(8)}",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = dateFormat.format(Date(snapshot.timestamp)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Stats card
            item {
                EnhancedCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Statistics",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        DetailRow("Apps", "${snapshot.apps.size}")
                        DetailRow("Size", "${snapshot.totalSize / 1024 / 1024} MB")
                        DetailRow("Compression", "${(snapshot.compressionRatio * 100).toInt()}%")
                        DetailRow("Permission Mode", snapshot.permissionMode)
                    }
                }
            }
            
            // Security card
            item {
                EnhancedCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Security",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        DetailRow(
                            "Encrypted",
                            if (snapshot.encrypted) "Yes" else "No",
                            icon = if (snapshot.encrypted) Icons.Default.Lock else Icons.Default.LockOpen
                        )
                        DetailRow(
                            "Verified",
                            if (snapshot.verified) "Yes" else "No",
                            icon = if (snapshot.verified) Icons.Default.CheckCircle else Icons.Default.Warning
                        )
                    }
                }
            }
            
            // Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EnhancedButton(
                        onClick = {
                            haptic.heavy()
                            // Restore backup
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore Backup")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            haptic.medium()
                            // Verify backup
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify Integrity")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
