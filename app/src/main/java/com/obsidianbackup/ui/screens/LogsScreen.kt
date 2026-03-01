// ui/screens/LogsScreen.kt
package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.Arrangement
import com.obsidianbackup.ui.theme.Spacing
import com.obsidianbackup.ui.theme.IconSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.animation.AnimatedVisibility
import androidx.hilt.navigation.compose.hiltViewModel
import com.obsidianbackup.model.*
import com.obsidianbackup.ui.viewmodel.LogsViewModel
import com.obsidianbackup.ui.components.EnhancedFloatingActionButton
import com.obsidianbackup.ui.utils.Animations
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogsScreen(
    viewModel: LogsViewModel = hiltViewModel()
) {
    var selectedLog by remember { mutableStateOf<LogEntry?>(null) }
    val logs by viewModel.logs.collectAsState()
    val selectedLevel by viewModel.selectedLevel.collectAsState()
    val selectedOperation by viewModel.selectedOperation.collectAsState()
    
    val listState = rememberLazyListState()
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 5 }
    }
    val scope = rememberCoroutineScope()
    
    if (selectedLog != null) {
        LogDetailScreen(
            log = selectedLog!!,
            onBack = { selectedLog = null }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Filter chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    item {
                        FilterChip(
                            selected = selectedLevel == null && selectedOperation == null,
                            onClick = { viewModel.clearFilters() },
                            label = { Text("All") }
                        )
                    }
                    
                    items(LogLevel.entries.toTypedArray()) { level ->
                        FilterChip(
                            selected = selectedLevel == level,
                            onClick = { 
                                viewModel.setLevelFilter(if (selectedLevel == level) null else level)
                            },
                            label = { Text(level.name) },
                            leadingIcon = if (selectedLevel == level) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(IconSize.small)) }
                            } else null
                        )
                    }
                    
                    items(OperationType.entries.toTypedArray()) { operation ->
                        FilterChip(
                            selected = selectedOperation == operation,
                            onClick = {
                                viewModel.setOperationFilter(if (selectedOperation == operation) null else operation)
                            },
                            label = { Text(operation.name) }
                        )
                    }
                }
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState
                ) {
                    item {
                        ListItem(
                            headlineContent = { Text("Operation Logs") },
                            supportingContent = { Text("${logs.size} entries") },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = {
                                        viewModel.clearAllLogs()
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Clear logs")
                                    }
                                    IconButton(onClick = {
                                        Log.i("LogsScreen", "Export logs requested; ${logs.size} entries")
                                        // Export functionality would go here
                                    }) {
                                        Icon(Icons.Default.Share, contentDescription = "Export")
                                    }
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                    
                    if (logs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.xl),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Text(
                                    text = "No logs yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(logs, key = { it.timestamp }) { log ->
                            LogListItem(
                                log = log,
                                onClick = { selectedLog = log }
                            )
                        }
                    }
                }
            }
            
            // Scroll to top FAB
            AnimatedVisibility(
                visible = showScrollToTop,
                enter = Animations.fabEnterAnimation,
                exit = Animations.fabExitAnimation,
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomEnd)
                    .padding(Spacing.md)
            ) {
                EnhancedFloatingActionButton(
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    }
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Scroll to top")
                }
            }
        }
        
    }
}

@Composable
fun LogListItem(
    log: LogEntry,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd HH:mm:ss", Locale.getDefault()) }
    val icon = when (log.operationType) {
        OperationType.BACKUP -> Icons.Default.Backup
        OperationType.RESTORE -> Icons.Default.RestorePage
        OperationType.VERIFY -> Icons.Default.CheckCircle
        OperationType.DELETE -> Icons.Default.Delete
    }
    
    // Color-coded by log level
    val (iconTint, backgroundColor) = when (log.level) {
        LogLevel.ERROR -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.errorContainer
        LogLevel.WARN -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.tertiaryContainer
        LogLevel.INFO -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.onSurface to MaterialTheme.colorScheme.surface
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        ListItem(
            headlineContent = { 
                Text(
                    log.message,
                    fontFamily = FontFamily.Monospace
                ) 
            },
            supportingContent = {
                Text(
                    "${dateFormat.format(Date(log.timestamp))} • ${log.operationType.name}",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            leadingContent = {
                Icon(
                    icon, 
                    contentDescription = "${log.operationType.name}: ${log.message}", 
                    tint = iconTint,
                    modifier = Modifier.size(IconSize.medium)
                )
            },
            trailingContent = {
                Badge {
                    Text(
                        log.level.name,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            },
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDetailScreen(
    log: LogEntry,
    onBack: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text("Timestamp", style = MaterialTheme.typography.labelSmall)
                    Text(dateFormat.format(Date(log.timestamp)))
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text("Operation", style = MaterialTheme.typography.labelSmall)
                    Text(log.operationType.name)
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text("Level", style = MaterialTheme.typography.labelSmall)
                    Text(log.level.name)
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text("Message", style = MaterialTheme.typography.labelSmall)
                    Text(log.message)
                    if (log.details != null) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text("Details", style = MaterialTheme.typography.labelSmall)
                        Text(log.details)
                    }
                    if (log.snapshotId != null) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text("Snapshot ID", style = MaterialTheme.typography.labelSmall)
                        Text(log.snapshotId.value)
                    }
                }
            }
        }
    }
}
