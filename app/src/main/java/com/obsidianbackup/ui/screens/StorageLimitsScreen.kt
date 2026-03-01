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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obsidianbackup.ui.theme.ObsidianColors
import com.obsidianbackup.ui.theme.Spacing
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageLimitsScreen(
    viewModel: StorageLimitsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Limits") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (!state.isLoaded) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
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
            item { Spacer(Modifier.height(Spacing.xs)) }

            // Device storage overview
            item { DeviceStorageCard(state) }

            // Backup storage usage
            item { BackupStorageCard(state) }

            // Limit setting
            item {
                Text("Backup Storage Limit", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "When the limit is reached, the oldest backups will be deleted automatically " +
                        "after each new backup. Set to 0 for no limit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item { LimitSliderCard(state, viewModel::setLimitMb) }

            // Preset buttons
            item { LimitPresetsRow(state.limitMb, viewModel::setLimitMb) }

            item { Spacer(Modifier.height(Spacing.md)) }
        }
    }
}

@Composable
private fun DeviceStorageCard(state: StorageLimitsUiState) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Icon(Icons.Default.PhoneAndroid, null, tint = ObsidianColors.MoltenOrange)
                Text("Device Storage", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(Spacing.sm))

            val usedMb = state.deviceTotalMb - state.deviceFreeMb
            val usedFraction = if (state.deviceTotalMb > 0)
                usedMb.toFloat() / state.deviceTotalMb.toFloat() else 0f

            LinearProgressIndicator(
                progress = { usedFraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    usedFraction > 0.9f -> MaterialTheme.colorScheme.error
                    usedFraction > 0.75f -> ObsidianColors.MoltenGold
                    else -> ObsidianColors.MoltenOrange
                }
            )
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${usedMb} MB used", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${state.deviceFreeMb} MB free / ${state.deviceTotalMb} MB total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BackupStorageCard(state: StorageLimitsUiState) {
    val limitMb = state.limitMb
    val usedFraction = if (limitMb > 0) state.usedMb.toFloat() / limitMb.toFloat() else 0f
    val isOverLimit = limitMb > 0 && state.usedMb > limitMb

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isOverLimit)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Icon(
                    if (isOverLimit) Icons.Default.Warning else Icons.Default.Backup,
                    null,
                    tint = if (isOverLimit) MaterialTheme.colorScheme.error else ObsidianColors.MoltenOrange
                )
                Text("Backup Storage", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if (isOverLimit) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small) {
                        Text("Over Limit", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
            Spacer(Modifier.height(Spacing.sm))

            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${state.usedMb} MB", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    Text("Used", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${state.snapshotCount}", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge)
                    Text("Snapshots", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (limitMb == 0) "∞" else "$limitMb MB",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = ObsidianColors.MoltenOrange)
                    Text("Limit", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (limitMb > 0) {
                Spacer(Modifier.height(Spacing.sm))
                LinearProgressIndicator(
                    progress = { usedFraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        usedFraction > 1f  -> MaterialTheme.colorScheme.error
                        usedFraction > 0.85f -> ObsidianColors.MoltenGold
                        else               -> ObsidianColors.MoltenOrange
                    }
                )
            }
        }
    }
}

@Composable
private fun LimitSliderCard(state: StorageLimitsUiState, onLimitChange: (Int) -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            val maxSliderMb = 51200 // 50 GB
            val sliderValue = if (state.limitMb == 0) 0f else state.limitMb.toFloat()

            Text(
                if (state.limitMb == 0) "No Limit" else formatMb(state.limitMb.toLong()),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = ObsidianColors.MoltenOrange
            )

            Slider(
                value = sliderValue,
                onValueChange = { onLimitChange(it.roundToInt()) },
                valueRange = 0f..maxSliderMb.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("No limit", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("50 GB", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LimitPresetsRow(currentLimitMb: Int, onLimitChange: (Int) -> Unit) {
    val presets = listOf(
        0      to "Off",
        512    to "512 MB",
        1024   to "1 GB",
        5120   to "5 GB",
        10240  to "10 GB",
        20480  to "20 GB"
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("Quick Presets", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            presets.take(3).forEach { (mb, label) ->
                FilterChip(
                    selected = currentLimitMb == mb,
                    onClick = { onLimitChange(mb) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            presets.drop(3).forEach { (mb, label) ->
                FilterChip(
                    selected = currentLimitMb == mb,
                    onClick = { onLimitChange(mb) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun formatMb(mb: Long): String = when {
    mb >= 1024 -> "%.1f GB".format(mb / 1024.0)
    else       -> "$mb MB"
}
