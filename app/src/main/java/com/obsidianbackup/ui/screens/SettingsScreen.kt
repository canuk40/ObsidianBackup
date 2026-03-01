// ui/screens/SettingsScreen.kt
package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import com.obsidianbackup.ui.theme.Spacing
import com.obsidianbackup.ui.theme.Elevation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.hilt.navigation.compose.hiltViewModel
import com.obsidianbackup.permissions.PermissionManager
import com.obsidianbackup.ui.viewmodel.SettingsViewModel
import com.obsidianbackup.ui.components.EnhancedSwitch
import com.obsidianbackup.diagnostics.DiagnosticsViewModel
import com.obsidianbackup.diagnostics.DiagnosticsState
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.obsidianbackup.R

@Composable
fun SettingsScreen(
    permissionManager: PermissionManager,
    onNavigateToZeroKnowledge: () -> Unit = {},
    onNavigateToFeatureFlags: () -> Unit = {},
    onNavigateToCloudProviders: () -> Unit = {},
    onNavigateToFilecoin: () -> Unit = {},
    onNavigateToGaming: () -> Unit = {},
    onNavigateToHealth: () -> Unit = {},
    onNavigateToAutomation: () -> Unit = {},
    onNavigateToPlugins: () -> Unit = {},
    onNavigateToSmartScheduling: () -> Unit = {},
    onNavigateToLicenses: () -> Unit = {},   // H-1: Open Source Licenses now navigates
    onNavigateToVersionInfo: () -> Unit = {},
    onNavigateToSimplifiedMode: () -> Unit = {},
    onNavigateToBusyBox: () -> Unit = {},
    onNavigateToPermissionMode: () -> Unit = {},
    onNavigateToRetentionPolicies: () -> Unit = {},
    onNavigateToStorageLimits: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    diagnosticsViewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    var showComingSoonDialog by remember { mutableStateOf<String?>(null) }

    val diagnosticsState by diagnosticsViewModel.state.collectAsState()
    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        diagnosticsViewModel.resetState()
    }

    // Launch share sheet when export is ready
    LaunchedEffect(diagnosticsState) {
        if (diagnosticsState is DiagnosticsState.ReadyToShare) {
            shareLauncher.launch((diagnosticsState as DiagnosticsState.ReadyToShare).intent)
        }
    }
    
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState()
    val cloudSyncEnabled by viewModel.cloudSyncEnabled.collectAsState()
    val compressionEnabled by viewModel.compressionEnabled.collectAsState()
    val encryptionEnabled by viewModel.encryptionEnabled.collectAsState()
    val verifyAfterBackup by viewModel.verifyAfterBackup.collectAsState()
    val debugMode by viewModel.debugMode.collectAsState()
    val parallelOperationsEnabled by viewModel.parallelOperationsEnabled.collectAsState()

    // Show toast when there's nothing to export
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(diagnosticsState) {
        if (diagnosticsState is DiagnosticsState.NoLogs) {
            android.widget.Toast.makeText(context, "No log files found yet", android.widget.Toast.LENGTH_SHORT).show()
            diagnosticsViewModel.resetState()
        } else if (diagnosticsState is DiagnosticsState.Error) {
            android.widget.Toast.makeText(context, (diagnosticsState as DiagnosticsState.Error).message, android.widget.Toast.LENGTH_LONG).show()
            diagnosticsViewModel.resetState()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = Spacing.xs)
    ) {
        item {
            SettingsSection(stringResource(R.string.settings_section_backup_restore))
        }
        item {
            SettingsToggleItem(
                title = stringResource(R.string.settings_auto_backup_title),
                subtitle = stringResource(R.string.settings_auto_backup_subtitle),
                icon = Icons.Default.Schedule,
                checked = autoBackupEnabled,
                onCheckedChange = viewModel::setAutoBackupEnabled
            )
        }
        item {
            SettingsToggleItem(
                title = stringResource(R.string.settings_compression_title),
                subtitle = stringResource(R.string.settings_compression_subtitle),
                icon = Icons.Default.Archive,
                checked = compressionEnabled,
                onCheckedChange = viewModel::setCompressionEnabled
            )
        }
        item {
            AnimatedVisibility(
                visible = compressionEnabled,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                SettingsItem(
                    title = stringResource(R.string.settings_compression_profile_title),
                    subtitle = stringResource(R.string.settings_compression_profile_subtitle),
                    icon = Icons.Default.Settings,
                    onClick = { showComingSoonDialog = "Compression Profile" }
                )
            }
        }
        item {
            SettingsToggleItem(
                title = stringResource(R.string.settings_verification_title),
                subtitle = stringResource(R.string.settings_verification_subtitle),
                icon = Icons.Default.CheckCircle,
                checked = verifyAfterBackup,
                onCheckedChange = viewModel::setVerifyAfterBackup
            )
        }
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))
        }
        item {
            SettingsSection(stringResource(R.string.settings_section_encryption))
        }
        item {
            SettingsToggleItem(
                title = stringResource(R.string.settings_encryption_title),
                subtitle = stringResource(R.string.settings_encryption_subtitle),
                icon = Icons.Default.Lock,
                checked = encryptionEnabled,
                onCheckedChange = viewModel::setEncryptionEnabled
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_zero_knowledge_title),
                subtitle = stringResource(R.string.settings_zero_knowledge_subtitle),
                icon = Icons.Default.Security,
                onClick = { onNavigateToZeroKnowledge() }
            )
        }
        item {
            SettingsSection(stringResource(R.string.settings_section_cloud_sync))
        }
        item {
            SettingsToggleItem(
                title = stringResource(R.string.settings_cloud_sync_title),
                subtitle = stringResource(R.string.settings_cloud_sync_subtitle),
                icon = Icons.Default.Cloud,
                checked = cloudSyncEnabled,
                onCheckedChange = viewModel::setCloudSyncEnabled
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_cloud_providers_title),
                subtitle = stringResource(R.string.settings_cloud_providers_subtitle),
                icon = Icons.Default.Storage,
                onClick = onNavigateToCloudProviders
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_decentralized_title),
                subtitle = stringResource(R.string.settings_decentralized_subtitle),
                icon = Icons.Default.Public,
                onClick = onNavigateToFilecoin
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_sync_policies_title),
                subtitle = stringResource(R.string.settings_sync_policies_subtitle),
                icon = Icons.Default.Sync,
                onClick = { showComingSoonDialog = "Sync Policies" }
            )
        }
        item {
            SettingsSection(stringResource(R.string.settings_section_gaming))
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_gaming_backups_title),
                subtitle = stringResource(R.string.settings_gaming_backups_subtitle),
                icon = Icons.Default.Games,
                onClick = onNavigateToGaming
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_play_games_title),
                subtitle = stringResource(R.string.settings_play_games_subtitle),
                icon = Icons.Default.Cloud,
                onClick = { showComingSoonDialog = "Play Games Cloud Sync" }
            )
        }
        item {
            SettingsSection(stringResource(R.string.settings_section_health))
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_health_connect_title),
                subtitle = stringResource(R.string.settings_health_connect_subtitle),
                icon = Icons.Default.Favorite,
                onClick = onNavigateToHealth
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_privacy_settings_title),
                subtitle = stringResource(R.string.settings_privacy_settings_subtitle),
                icon = Icons.Default.PrivacyTip,
                onClick = { showComingSoonDialog = "Privacy Settings" }
            )
        }
        item {
            SettingsSection(stringResource(R.string.settings_section_automation))
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_smart_scheduling_title),
                subtitle = stringResource(R.string.settings_smart_scheduling_subtitle),
                icon = Icons.Default.AutoAwesome,
                onClick = { onNavigateToSmartScheduling() }
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_tasker_integration_title),
                subtitle = stringResource(R.string.settings_tasker_integration_subtitle),
                icon = Icons.Default.Build,
                onClick = onNavigateToAutomation
            )
        }
        item {
            SettingsSection(stringResource(R.string.settings_section_plugins))
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_plugin_system_title),
                subtitle = stringResource(R.string.settings_plugin_system_subtitle),
                icon = Icons.Default.Extension,
                onClick = onNavigateToPlugins
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_plugin_security_title),
                subtitle = stringResource(R.string.settings_plugin_security_subtitle),
                icon = Icons.Default.Security,
                onClick = { showComingSoonDialog = "Plugin Security" }
            )
        }
        item {
            SettingsSection(stringResource(R.string.settings_section_retention))
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_retention_policies_title),
                subtitle = stringResource(R.string.settings_retention_policies_subtitle),
                icon = Icons.Default.DeleteSweep,
                onClick = { onNavigateToRetentionPolicies() }
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_storage_limits_title),
                subtitle = stringResource(R.string.settings_storage_limits_subtitle),
                icon = Icons.Default.Storage,
                onClick = { onNavigateToStorageLimits() }
            )
        }
        item {
            SettingsSection(stringResource(R.string.settings_section_permissions))
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_permission_mode_title),
                subtitle = stringResource(R.string.settings_permission_mode_subtitle),
                icon = Icons.Default.Security,
                onClick = { onNavigateToPermissionMode() }
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_request_permissions_title),
                subtitle = stringResource(R.string.settings_request_permissions_subtitle),
                icon = Icons.Default.Refresh,
                onClick = {
                    scope.launch {
                        permissionManager.detectBestMode()
                    }
                }
            )
        }
        item {
            SettingsSection(stringResource(R.string.settings_section_advanced))
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_simplified_mode_title),
                subtitle = stringResource(R.string.settings_simplified_mode_subtitle),
                icon = Icons.Default.Accessibility,
                onClick = onNavigateToSimplifiedMode
            )
        }
        item {
            SettingsToggleItem(
                title = stringResource(R.string.settings_debug_mode_title),
                subtitle = stringResource(R.string.settings_debug_mode_subtitle),
                icon = Icons.Default.BugReport,
                checked = debugMode,
                onCheckedChange = viewModel::setDebugMode
            )
        }
        item {
            SettingsToggleItem(
                title = stringResource(R.string.settings_parallel_ops_title),
                subtitle = stringResource(R.string.settings_parallel_ops_subtitle),
                icon = Icons.Default.Speed,
                checked = parallelOperationsEnabled,
                onCheckedChange = viewModel::setParallelOperationsEnabled
            )
        }
        // Info card for parallel operations
        if (parallelOperationsEnabled) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_parallel_ops_info),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_busybox_title),
                subtitle = stringResource(R.string.settings_busybox_subtitle),
                icon = Icons.Default.Build,
                onClick = { onNavigateToBusyBox() }
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_export_diagnostics_title),
                subtitle = stringResource(R.string.settings_export_diagnostics_subtitle),
                icon = Icons.Default.FileDownload,
                onClick = { diagnosticsViewModel.exportDiagnosticsBundle() }
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_export_logs_title),
                subtitle = stringResource(R.string.settings_export_logs_subtitle),
                icon = Icons.Default.FileDownload,
                onClick = { diagnosticsViewModel.exportAppLogs() }
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_export_shell_audit_title),
                subtitle = stringResource(R.string.settings_export_shell_audit_subtitle),
                icon = Icons.Default.History,
                onClick = { diagnosticsViewModel.exportShellAudit() }
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_feature_flags_title),
                subtitle = stringResource(R.string.settings_feature_flags_subtitle),
                icon = Icons.Default.Flag,
                onClick = onNavigateToFeatureFlags
            )
        }
        item {
            SettingsSection(stringResource(R.string.settings_section_about))
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_version_title),
                subtitle = stringResource(R.string.settings_version_subtitle),
                icon = Icons.Default.Info,
                onClick = { onNavigateToVersionInfo() }
            )
        }
        item {
            SettingsItem(
                title = stringResource(R.string.settings_licenses_title),
                subtitle = stringResource(R.string.settings_licenses_subtitle),
                icon = Icons.Default.Description,
                onClick = onNavigateToLicenses    // H-1 / M-7: Navigate instead of showing dialog
            )
        }
    }
    
    // Coming Soon Dialog
    showComingSoonDialog?.let { feature ->
        AlertDialog(
            onDismissRequest = { showComingSoonDialog = null },
            title = { Text(stringResource(R.string.dialog_coming_soon_title)) },
            text = { Text("$feature will be available in a future update.") },
            confirmButton = {
                TextButton(onClick = { showComingSoonDialog = null }) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        )
    }
    
}

@Composable
fun SettingsSection(title: String) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.padding(top = Spacing.sm)
    )
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null
) {
    ListItem(
        headlineContent = { 
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(title)
                badge?.invoke()
            }
        },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(icon, contentDescription = null)
        },
        modifier = if (onClick != null) {
            Modifier.clickable(onClick = onClick)
        } else {
            Modifier
        }
    )
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    badge: (@Composable () -> Unit)? = null
) {
    ListItem(
        headlineContent = { 
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(title)
                badge?.invoke()
            }
        },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(icon, contentDescription = null)
        },
        trailingContent = {
            EnhancedSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    )
}
