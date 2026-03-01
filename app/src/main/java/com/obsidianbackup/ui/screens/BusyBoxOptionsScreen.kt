// ui/screens/BusyBoxOptionsScreen.kt
package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obsidianbackup.ui.theme.ObsidianColors
import com.obsidianbackup.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusyBoxOptionsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: BusyBoxOptionsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    var showTarFlagsDialog by remember { mutableStateOf(false) }
    var showRsyncFlagsDialog by remember { mutableStateOf(false) }
    var showZstdFlagsDialog by remember { mutableStateOf(false) }
    var appletSearchQuery by remember { mutableStateOf("") }

    // Flag edit dialogs
    if (showTarFlagsDialog) {
        FlagsEditDialog(
            title = "Custom tar Flags",
            placeholder = "--exclude=*.tmp --exclude=*.log",
            currentValue = state.customTarFlags,
            onSave = { viewModel.saveTarFlags(it); showTarFlagsDialog = false },
            onDismiss = { showTarFlagsDialog = false }
        )
    }
    if (showRsyncFlagsDialog) {
        FlagsEditDialog(
            title = "Custom rsync Flags",
            placeholder = "--exclude=.cache --checksum",
            currentValue = state.customRsyncFlags,
            onSave = { viewModel.saveRsyncFlags(it); showRsyncFlagsDialog = false },
            onDismiss = { showRsyncFlagsDialog = false }
        )
    }
    if (showZstdFlagsDialog) {
        FlagsEditDialog(
            title = "Custom zstd Flags",
            placeholder = "--long --adapt",
            currentValue = state.customZstdFlags,
            onSave = { viewModel.saveZstdFlags(it); showZstdFlagsDialog = false },
            onDismiss = { showZstdFlagsDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BusyBox Options") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ObsidianColors.MoltenOrange)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Status Card
            item {
                Spacer(Modifier.height(Spacing.xs))
                BusyBoxStatusCard(state)
            }

            if (!state.isAvailable) return@LazyColumn

            // Custom Flags
            item {
                Text(
                    "Command Flags",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                FlagsCard(
                    label = "tar",
                    description = "Used when archiving app data directories",
                    currentFlags = state.customTarFlags,
                    onClick = { showTarFlagsDialog = true }
                )
            }

            item {
                FlagsCard(
                    label = "rsync",
                    description = "Used for incremental file sync",
                    currentFlags = state.customRsyncFlags,
                    onClick = { showRsyncFlagsDialog = true }
                )
            }

            item {
                FlagsCard(
                    label = "zstd",
                    description = "Used for compression",
                    currentFlags = state.customZstdFlags,
                    onClick = { showZstdFlagsDialog = true }
                )
            }

            // Shell Mode
            item {
                Text(
                    "Shell Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ASH Standalone Mode", fontWeight = FontWeight.Medium)
                            Text(
                                "Forces BusyBox ash to use its own applets (recommended)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.ashStandaloneMode,
                            onCheckedChange = { viewModel.setAshStandaloneMode(it) }
                        )
                    }
                }
            }

            // Applet Browser
            if (state.applets.isNotEmpty()) {
                item {
                    Text(
                        "Applets (${state.applets.size - state.disabledApplets.size} / ${state.applets.size} enabled)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    OutlinedTextField(
                        value = appletSearchQuery,
                        onValueChange = { appletSearchQuery = it },
                        label = { Text("Search applets") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                val filtered = state.applets.filter {
                    appletSearchQuery.isBlank() || it.contains(appletSearchQuery, ignoreCase = true)
                }

                items(filtered) { applet ->
                    AppletRow(
                        applet = applet,
                        enabled = applet !in state.disabledApplets,
                        onToggle = { viewModel.setAppletEnabled(applet, it) }
                    )
                }

                item { Spacer(Modifier.height(Spacing.md)) }
            }
        }
    }
}

@Composable
private fun BusyBoxStatusCard(state: BusyBoxUiState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (state.isAvailable)
                ObsidianColors.MoltenOrange.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Icon(
                imageVector = if (state.isAvailable) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (state.isAvailable) ObsidianColors.MoltenOrange else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
            Column {
                Text(
                    if (state.isAvailable) "BusyBox Available" else "BusyBox Not Found",
                    fontWeight = FontWeight.Bold
                )
                if (state.version != null) {
                    Text(
                        state.version,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state.busyBoxPath != null) {
                    Text(
                        state.busyBoxPath,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!state.isAvailable) {
                    Text(
                        "No BusyBox binary found. Root with Magisk or install BusyBox.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun FlagsCard(
    label: String,
    description: String,
    currentFlags: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Build,
                contentDescription = null,
                tint = ObsidianColors.MoltenOrange,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (currentFlags.isNotBlank()) {
                    Text(
                        currentFlags,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = ObsidianColors.MoltenOrange
                    )
                } else {
                    Text(
                        "No custom flags (tap to add)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AppletRow(
    applet: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            applet,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            modifier = Modifier.height(24.dp)
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}

@Composable
private fun FlagsEditDialog(
    title: String,
    placeholder: String,
    currentValue: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(currentValue) { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    "Enter extra flags to append to the command. Leave blank to use defaults.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(placeholder, fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                    singleLine = false,
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.trim()) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
