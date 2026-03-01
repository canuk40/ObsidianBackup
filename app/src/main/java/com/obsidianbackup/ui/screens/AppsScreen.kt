package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import com.obsidianbackup.ui.theme.Spacing
import com.obsidianbackup.ui.theme.ObsidianColors
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Search
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.obsidianbackup.ui.utils.Animations
import com.obsidianbackup.ui.utils.FabAnimation
import com.obsidianbackup.ui.utils.listItemEnterAnimation
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obsidianbackup.R
import com.obsidianbackup.accessibility.AccessibilityHelper
import com.obsidianbackup.model.AppId
import com.obsidianbackup.model.AppInfo
import com.obsidianbackup.model.BackupComponent
import com.obsidianbackup.model.BackupType
import com.obsidianbackup.model.FeatureTier
import com.obsidianbackup.permissions.PermissionManager
import com.obsidianbackup.presentation.apps.AppsState
import com.obsidianbackup.presentation.apps.AppsViewModel
import com.obsidianbackup.scanner.AppScanner
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * App filter types for the filter chips
 */
enum class AppFilter(val label: String) {
    ALL("All"),
    USER("User"),
    SYSTEM("System"),
    UPDATED("Updated")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    permissionManager: PermissionManager,
    appScanner: AppScanner,
    onBackupRequested: (List<AppId>) -> Unit = {}
) {
    val viewModel: AppsViewModel = hiltViewModel()
    val backupState by viewModel.state.collectAsState()
    val currentTier by viewModel.currentTier.collectAsStateWithLifecycle(initialValue = FeatureTier.FREE)
    
    var selectedApps by remember { mutableStateOf(setOf<AppId>()) }
    var showBackupDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Filter state
    var selectedFilter by remember { mutableStateOf(AppFilter.ALL) }

    LaunchedEffect(Unit) {
        installedApps = appScanner.scanInstalledApps(includeSystemApps = true)
    }

    // Filter apps based on search query and filter type
    val filteredApps = remember(installedApps, searchQuery, selectedFilter) {
        installedApps.filter { app ->
            val matchesSearch = searchQuery.isEmpty() ||
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                AppFilter.ALL -> true
                AppFilter.USER -> !app.isSystemApp
                AppFilter.SYSTEM -> app.isSystemApp
                AppFilter.UPDATED -> app.isUpdatedSystemApp
            }

            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        floatingActionButton = {
            FabAnimation(
                visible = selectedApps.isNotEmpty(),
                content = {
                    ExtendedFloatingActionButton(
                        onClick = { 
                            showBackupDialog = true
                            AccessibilityHelper.announceForAccessibility(
                                context,
                                context.getString(R.string.announce_apps_count, selectedApps.size)
                            )
                        },
                        icon = { Icon(Icons.Default.Backup, contentDescription = null) },
                        text = { Text("Backup ${selectedApps.size} apps") },
                        modifier = Modifier.semantics {
                            contentDescription = context.getString(R.string.cd_backup_button)
                        },
                        containerColor = ObsidianColors.MoltenOrange,
                        contentColor = ObsidianColors.TextOnMolten
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            @Suppress("DEPRECATION")
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { /* no-op: list filters reactively */ },
                active = false,
                onActiveChange = { },
                placeholder = { Text("Search apps...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = ObsidianColors.MoltenOrange
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs)
            ) {
                // Search suggestions could go here
            }

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                AppFilter.values().forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ObsidianColors.MoltenOrange,
                            selectedLabelColor = ObsidianColors.TextOnMolten
                        )
                    )
                }
            }

            HorizontalDivider(color = ObsidianColors.Divider)

            // Apps List
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    ListItem(
                        headlineContent = {
                            Text(
                                "Select apps to backup",
                                modifier = Modifier.semantics { heading() }
                            )
                        },
                        supportingContent = {
                            Text("${selectedApps.size} of ${filteredApps.size} selected")
                        }
                    )
                    HorizontalDivider()
                }

                items(
                    items = filteredApps,
                    key = { it.packageName }
                ) { app ->
                    AnimatedVisibility(
                        visible = true,
                        enter = listItemEnterAnimation,
                        modifier = Modifier.animateItem()
                    ) {
                        AppListItem(
                            app = app,
                            isSelected = selectedApps.contains(app.appId),
                            onSelectionChange = { selected ->
                                selectedApps = if (selected) {
                                    selectedApps + app.appId
                                } else {
                                    selectedApps - app.appId
                                }

                                // Announce selection state
                                val announcement = if (selected) {
                                    context.getString(R.string.announce_app_selected, app.appName)
                                } else {
                                    context.getString(R.string.announce_app_deselected, app.appName)
                                }
                                AccessibilityHelper.announceForAccessibility(context, announcement)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showBackupDialog) {
        BackupDialog(
            appCount = selectedApps.size,
            permissionManager = permissionManager,
            currentTier = currentTier,
            onDismiss = { showBackupDialog = false },
            onConfirm = {
                AccessibilityHelper.announceForAccessibility(
                    context,
                    context.getString(R.string.announce_backup_started)
                )
                // Trigger actual backup via ViewModel
                viewModel.backupApps(
                    appIds = selectedApps.toList(),
                    components = setOf(BackupComponent.APK, BackupComponent.DATA)
                )
                onBackupRequested(selectedApps.toList())
                showBackupDialog = false
                selectedApps = setOf()
            }
        )
    }
    
    // Show backup progress and results
    when (backupState) {
        is AppsState.BackingUp -> {
            BackupProgressDialog(
                totalApps = (backupState as AppsState.BackingUp).totalApps
            )
        }
        is AppsState.BackupSuccess -> {
            val state = backupState as AppsState.BackupSuccess
            LaunchedEffect(backupState) {
                AccessibilityHelper.announceForAccessibility(
                    context,
                    "Backup completed successfully. ${state.appsBackedUp} apps backed up."
                )
            }
            BackupResultDialog(
                success = true,
                message = "Backup completed successfully!\n\n${state.appsBackedUp} apps backed up\nTotal size: ${state.totalSize / 1024 / 1024} MB\nDuration: ${if (state.duration < 1000L) "${state.duration}ms" else "${state.duration / 1000}s"}",
                onDismiss = { viewModel.resetState() }
            )
        }
        is AppsState.BackupPartialSuccess -> {
            val state = backupState as AppsState.BackupPartialSuccess
            BackupResultDialog(
                success = false,
                message = "Backup partially completed.\n\n${state.appsBackedUp} apps backed up\n${state.appsFailed} apps failed\n\nErrors:\n${state.errors.joinToString("\n")}",
                onDismiss = { viewModel.resetState() }
            )
        }
        is AppsState.BackupError -> {
            BackupResultDialog(
                success = false,
                message = "Backup failed: ${(backupState as AppsState.BackupError).message}",
                onDismiss = { viewModel.resetState() }
            )
        }
        else -> { /* Idle state - do nothing */ }
    }
}

@Composable
fun AppListItem(
    app: AppInfo,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val selectionState = if (isSelected) "selected" else "not selected"
    val contentDesc = stringResource(
        R.string.cd_app_icon,
        app.appName
    ) + ", $selectionState"
    
    ListItem(
        headlineContent = { Text(app.appName) },
        supportingContent = {
            Column {
                Text("${app.packageName} • v${app.versionName}")
                Text("Data: ${app.dataSize / 1024 / 1024} MB • APK: ${app.apkSize / 1024 / 1024} MB")
            }
        },
        leadingContent = {
            Icon(
                Icons.Default.Android,
                contentDescription = null
            )
        },
        trailingContent = {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange,
                modifier = Modifier
                    .size(AccessibilityHelper.MIN_TOUCH_TARGET_SIZE_DP.dp)
                    .semantics { 
                        contentDescription = context.getString(R.string.cd_select_app)
                    }
            )
        },
        modifier = Modifier
            .clickable { onSelectionChange(!isSelected) }
            .heightIn(min = AccessibilityHelper.MIN_TOUCH_TARGET_SIZE_DP.dp)
            .semantics {
                this.contentDescription = contentDesc
                this.selected = isSelected
            }
    )
}

@Composable
fun BackupDialog(
    appCount: Int,
    permissionManager: PermissionManager,
    currentTier: FeatureTier,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val currentMode by permissionManager.currentMode.collectAsState()
    val capabilities by permissionManager.capabilities.collectAsState()
    val context = LocalContext.current
    
    // Backup type state (FULL vs INCREMENTAL)
    var backupType by remember { mutableStateOf(BackupType.FULL) }
    
    // FOSS build: all features available
    @Suppress("UNUSED_VARIABLE") val isPro = true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Backup $appCount apps",
                modifier = Modifier.semantics { heading() }
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text("Using: ${currentMode.displayName}")
                Spacer(modifier = Modifier.height(Spacing.xs))
                
                Text("Components to backup:")
                if (capabilities.canBackupApk) Text("• APK files")
                if (capabilities.canBackupData) Text("• App data")
                if (capabilities.canBackupObb) Text("• OBB files")
                if (capabilities.canBackupExternalData) Text("• External storage")
                
                Spacer(modifier = Modifier.height(Spacing.md))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(Spacing.md))
                
                // Backup type selection
                Text(
                    "Backup Type:",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                
                // Full backup radio button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { backupType = BackupType.FULL }
                        .padding(vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = backupType == BackupType.FULL,
                        onClick = { backupType = BackupType.FULL }
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Column {
                        Text("Full Backup", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Backs up all app data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Incremental backup radio button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { backupType = BackupType.INCREMENTAL }
                        .padding(vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = backupType == BackupType.INCREMENTAL,
                        onClick = { backupType = BackupType.INCREMENTAL }
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Incremental Backup", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Only backs up changed files (faster, smaller)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.heightIn(min = AccessibilityHelper.MIN_TOUCH_TARGET_SIZE_DP.dp)
            ) {
                Text("Backup Now")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = AccessibilityHelper.MIN_TOUCH_TARGET_SIZE_DP.dp)
            ) {
                Text("Cancel")
            }
        }
    )
    
}

@Composable
fun BackupProgressDialog(totalApps: Int) {
    AlertDialog(
        onDismissRequest = { /* Cannot dismiss during backup */ },
        title = { Text("Backing up apps...") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(Spacing.md))
                Text("Processing $totalApps apps")
                Text("Please wait...", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { }
    )
}

@Composable
fun BackupResultDialog(
    success: Boolean,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = { 
            Text(if (success) "Backup Completed" else "Backup Issue") 
        },
        text = { 
            Text(message)
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
