// ui/screens/PermissionModeScreen.kt
package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obsidianbackup.model.PermissionCapabilities
import com.obsidianbackup.model.PermissionMode
import com.obsidianbackup.permissions.PermissionManager
import com.obsidianbackup.ui.theme.ObsidianColors
import com.obsidianbackup.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionModeScreen(
    permissionManager: PermissionManager,
    onNavigateBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val currentMode by permissionManager.currentMode.collectAsStateWithLifecycle()
    val capabilities by permissionManager.capabilities.collectAsStateWithLifecycle()
    var forcedMode by remember { mutableStateOf(permissionManager.getForcedMode()) }
    var isDetecting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permission Mode") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item { Spacer(Modifier.height(Spacing.xs)) }

            // Active mode banner
            item {
                ActiveModeBanner(currentMode, forcedMode != null)
            }

            // Re-detect button
            item {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isDetecting = true
                            permissionManager.invalidateCache()
                            permissionManager.detectBestMode()
                            // If user had a forced mode, keep it; just refresh caps
                            isDetecting = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isDetecting
                ) {
                    if (isDetecting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(Spacing.xs))
                    } else {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.xs))
                    }
                    Text("Re-detect Capabilities")
                }
            }

            item {
                Text(
                    "Available Modes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "Select a mode to override auto-detection. The app picks the best available mode automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Mode cards
            item { ModeCard(PermissionMode.ROOT, capabilities, forcedMode, currentMode) { selected ->
                forcedMode = if (selected) PermissionMode.ROOT else null
                permissionManager.forceMode(if (selected) PermissionMode.ROOT else null)
                if (!selected) scope.launch { permissionManager.detectBestMode() }
            } }

            item { ModeCard(PermissionMode.SHIZUKU, capabilities, forcedMode, currentMode) { selected ->
                forcedMode = if (selected) PermissionMode.SHIZUKU else null
                permissionManager.forceMode(if (selected) PermissionMode.SHIZUKU else null)
                if (!selected) scope.launch { permissionManager.detectBestMode() }
            } }

            item { ModeCard(PermissionMode.ADB, capabilities, forcedMode, currentMode) { selected ->
                forcedMode = if (selected) PermissionMode.ADB else null
                permissionManager.forceMode(if (selected) PermissionMode.ADB else null)
                if (!selected) scope.launch { permissionManager.detectBestMode() }
            } }

            item { ModeCard(PermissionMode.SAF, capabilities, forcedMode, currentMode) { selected ->
                forcedMode = if (selected) PermissionMode.SAF else null
                permissionManager.forceMode(if (selected) PermissionMode.SAF else null)
                if (!selected) scope.launch { permissionManager.detectBestMode() }
            } }

            // Capability comparison table
            item {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "Capability Comparison",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item { CapabilityTable(capabilities) }

            item { Spacer(Modifier.height(Spacing.md)) }
        }
    }
}

@Composable
private fun ActiveModeBanner(mode: PermissionMode, isForced: Boolean) {
    val (color, icon) = when (mode) {
        PermissionMode.ROOT    -> ObsidianColors.MoltenOrange to Icons.Default.AdminPanelSettings
        PermissionMode.SHIZUKU -> ObsidianColors.MoltenGold   to Icons.Default.DeveloperMode
        PermissionMode.ADB     -> MaterialTheme.colorScheme.secondary to Icons.Default.Cable
        PermissionMode.SAF     -> MaterialTheme.colorScheme.tertiary  to Icons.Default.FolderOpen
    }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = color.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(36.dp))
            Column {
                Text(
                    "Active: ${mode.displayName}${if (isForced) " (forced)" else " (auto)"}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    modeDescription(mode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    mode: PermissionMode,
    caps: PermissionCapabilities,
    forcedMode: PermissionMode?,
    currentMode: PermissionMode,
    onToggle: (Boolean) -> Unit
) {
    val isAvailable = caps.isModeAvailable(mode)
    val isForced = forcedMode == mode
    val isActive = currentMode == mode

    val borderColor = when {
        isForced -> ObsidianColors.MoltenOrange
        isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isForced)
                ObsidianColors.MoltenOrange.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Available indicator
            Icon(
                imageVector = if (isAvailable) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (isAvailable) ObsidianColors.MoltenOrange
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(mode.displayName, fontWeight = FontWeight.Bold)
                    if (!isAvailable) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                "Not available",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    modeDescription(mode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RadioButton(
                selected = isForced,
                onClick = { onToggle(!isForced) },
                enabled = isAvailable || isForced
            )
        }
    }
}

@Composable
private fun CapabilityTable(caps: PermissionCapabilities) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // Header row
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Capability", modifier = Modifier.weight(2f),
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                for (mode in PermissionMode.entries) {
                    Text(
                        mode.displayName.take(4),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianColors.MoltenOrange
                    )
                }
            }

            HorizontalDivider()

            CapRow("APK Backup",       true,  true,  true,  false)
            CapRow("App Data",         true,  true,  false, false)
            CapRow("OBB Files",        true,  false, false, false)
            CapRow("Incremental",      true,  false, false, false)
            CapRow("SELinux Restore",  true,  false, false, false)
            CapRow("No Root Needed",   false, true,  true,  true)
            CapRow("Always Available", false, false, false, true)
        }
    }
}

@Composable
private fun CapRow(label: String, root: Boolean, shizuku: Boolean, adb: Boolean, saf: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(2f),
            style = MaterialTheme.typography.bodySmall)
        for (supported in listOf(root, shizuku, adb, saf)) {
            Text(
                if (supported) "✓" else "–",
                modifier = Modifier.weight(1f),
                color = if (supported) ObsidianColors.MoltenOrange
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                fontWeight = if (supported) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun modeDescription(mode: PermissionMode) = when (mode) {
    PermissionMode.ROOT    -> "Full access via su. Best backup/restore capability. Requires Magisk or equivalent."
    PermissionMode.SHIZUKU -> "Elevated access via Shizuku app. Backs up APKs + data without full root."
    PermissionMode.ADB     -> "ADB backup protocol. APK backup only. No data access."
    PermissionMode.SAF     -> "Storage Access Framework only. Limited to files the user explicitly grants access to."
}
