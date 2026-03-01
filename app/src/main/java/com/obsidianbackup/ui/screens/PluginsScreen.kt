// ui/screens/PluginsScreen.kt
package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.Arrangement
import com.obsidianbackup.ui.theme.Spacing
import com.obsidianbackup.ui.theme.Elevation
import com.obsidianbackup.ui.theme.IconSize
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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox as BadgeBox
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.hilt.navigation.compose.hiltViewModel
import com.obsidianbackup.plugins.core.Plugin
import com.obsidianbackup.presentation.plugins.PluginsViewModel
import com.obsidianbackup.ui.components.EnhancedSwitch
import android.widget.Toast
@Composable
fun PluginsScreen(
    viewModel: PluginsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            if (uiState.featureEnabled) {
                FloatingActionButton(onClick = {
                    Toast.makeText(context, "Plugin Store - Coming Soon", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.Add, "Add plugin")
                }
            }
        }
    ) { padding ->
        if (!uiState.featureEnabled) {
            DisabledFeatureMessage(padding, "Plugin System")
        } else {
            PluginsContent(
                padding = padding,
                uiState = uiState,
                onTogglePlugin = viewModel::togglePlugin,
                onClearError = viewModel::clearError
            )
        }
    }
}
@Composable
private fun PluginsContent(
    padding: PaddingValues,
    uiState: com.obsidianbackup.presentation.plugins.PluginsUiState,
    onTogglePlugin: (Plugin, Boolean) -> Unit,
    onClearError: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        if (uiState.error != null) {
            Snackbar(
                modifier = Modifier.padding(Spacing.md),
                action = {
                    TextButton(onClick = onClearError) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(uiState.error)
            }
        }
        if (uiState.isLoading || uiState.isDiscovering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        if (uiState.isDiscovering) "Discovering plugins..." else "Loading..."
                    )
                }
            }
        } else if (uiState.installedPlugins.isEmpty()) {
            EmptyState("No plugins installed")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    Text(
                        "Installed Plugins (${uiState.installedPlugins.size})",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                items(uiState.installedPlugins) { plugin ->
                    PluginCard(
                        plugin = plugin,
                        isEnabled = uiState.enabledPlugins.contains(plugin),
                        onToggle = { enabled -> onTogglePlugin(plugin, enabled) }
                    )
                }
            }
        }
    }
}
@Composable
private fun PluginCard(
    plugin: Plugin,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isEnabled) Elevation.medium else Elevation.low
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Icon(
                        Icons.Default.Extension,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.large),
                        tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            Text(
                                plugin.name, 
                                style = MaterialTheme.typography.titleMedium
                            )
                            // Status badge
                            if (isEnabled) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text("Enabled", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(Spacing.xxs))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Text(
                                "v${plugin.version}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                plugin.metadata?.author ?: "Unknown",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Settings button
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        enabled = isEnabled
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Plugin settings",
                            tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    
                    // Enable/Disable switch
                    EnhancedSwitch(
                        checked = isEnabled,
                        onCheckedChange = onToggle
                    )
                }
            }
            
            // Expandable description section
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md)
                        .padding(bottom = Spacing.md)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = Spacing.xs))
                    Text(
                        "Description",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(Spacing.xxs))
                    Text(
                        plugin.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Expand/Collapse button
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xs)
            ) {
                Text(if (expanded) "Show Less" else "Show More")
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.small)
                )
            }
        }
    }
    
    // Plugin Settings Dialog
    if (showSettingsDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("${plugin.name} Settings") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        "Plugin configuration options",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))
                    
                    // Plugin-specific settings would go here
                    Text(
                        "Package: ${plugin.metadata?.packageName ?: "N/A"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "API Version: ${plugin.metadata?.apiVersion ?: "N/A"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    if (plugin.metadata?.website != null) {
                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        TextButton(
                            onClick = {
                                uriHandler.openUri(plugin.metadata.website!!)
                            }
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null)
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text("Visit Website")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
