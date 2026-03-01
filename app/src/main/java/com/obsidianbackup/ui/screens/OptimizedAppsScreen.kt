// ui/screens/OptimizedAppsScreen.kt
package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.obsidianbackup.model.AppId
import com.obsidianbackup.model.AppInfo
import com.obsidianbackup.performance.observeScrollToEnd
import com.obsidianbackup.permissions.PermissionManager
import kotlinx.coroutines.launch

/**
 * Optimized AppsScreen with pagination and efficient rendering
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizedAppsScreen(
    permissionManager: PermissionManager,
    allApps: List<AppInfo>,
    onBackupRequested: (List<AppId>) -> Unit = {},
    pageSize: Int = 50 // Configurable page size
) {
    var selectedApps by remember { mutableStateOf(setOf<AppId>()) }
    var showBackupDialog by remember { mutableStateOf(false) }
    
    // Pagination state
    var displayedAppsCount by remember { mutableStateOf(pageSize) }
    val displayedApps by remember(allApps, displayedAppsCount) {
        derivedStateOf {
            allApps.take(displayedAppsCount)
        }
    }
    
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Observe scroll for pagination
    listState.observeScrollToEnd(threshold = 10) {
        if (displayedAppsCount < allApps.size) {
            displayedAppsCount = (displayedAppsCount + pageSize).coerceAtMost(allApps.size)
        }
    }
    
    Scaffold(
        floatingActionButton = {
            if (selectedApps.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showBackupDialog = true },
                    icon = { Icon(Icons.Default.Backup, contentDescription = null) },
                    text = { Text("Backup ${selectedApps.size} apps") }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            // Performance optimization: add content padding
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item(
                key = "header",
                contentType = "header"
            ) {
                ListItem(
                    headlineContent = { 
                        Text("Select apps to backup")
                    },
                    supportingContent = { 
                        Text("${selectedApps.size} of ${allApps.size} selected")
                    }
                )
                HorizontalDivider()
            }
            
            // Optimized items with key and contentType
            items(
                items = displayedApps,
                key = { app -> app.appId.value }, // Stable key for better performance
                contentType = { "app_item" } // Content type for better recycling
            ) { app ->
                OptimizedAppListItem(
                    app = app,
                    isSelected = selectedApps.contains(app.appId),
                    onSelectionChange = { selected ->
                        selectedApps = if (selected) {
                            selectedApps + app.appId
                        } else {
                            selectedApps - app.appId
                        }
                    }
                )
            }
            
            // Loading indicator when more items are available
            if (displayedAppsCount < allApps.size) {
                item(
                    key = "loading",
                    contentType = "loading"
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
    
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Confirm Backup") },
            text = { Text("Backup ${selectedApps.size} selected apps?") },
            confirmButton = {
                TextButton(onClick = {
                    onBackupRequested(selectedApps.toList())
                    showBackupDialog = false
                }) {
                    Text("Backup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Optimized app list item with minimal recomposition
 */
@Composable
fun OptimizedAppListItem(
    app: AppInfo,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Use remember to avoid unnecessary recompositions
    val sizeText = remember(app.dataSize, app.apkSize) {
        val totalSize = app.dataSize + app.apkSize
        "${totalSize / 1024 / 1024} MB"
    }
    
    ListItem(
        headlineContent = { Text(app.appName) },
        supportingContent = { 
            Text("${app.packageName} • $sizeText")
        },
        leadingContent = {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange
            )
        },
        modifier = modifier
    )
}
