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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.obsidianbackup.ui.theme.ObsidianColors
import com.obsidianbackup.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetentionPoliciesScreen(
    viewModel: RetentionPoliciesViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Retention Policies") },
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

            // Current stats banner
            item { StatsBanner(state) }

            // Preview banner (only when something would be pruned)
            if (state.previewDeleteCount > 0) {
                item { PreviewBanner(state) }
            }

            // Retention mode
            item {
                Text("Retention Mode", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
            }
            item { RetentionModeCard(state.retentionMode, viewModel::setRetentionMode) }

            // Keep count (shown when mode includes COUNT)
            if (state.retentionMode == "COUNT" || state.retentionMode == "BOTH") {
                item {
                    CountPolicyCard(
                        keepCount = state.keepCount,
                        onCountChange = viewModel::setKeepCount
                    )
                }
            }

            // Age-based (shown when mode includes DAYS)
            if (state.retentionMode == "DAYS" || state.retentionMode == "BOTH") {
                item {
                    AgePolicyCard(
                        retentionDays = state.retentionDays,
                        onDaysChange = viewModel::setRetentionDays
                    )
                }
            }

            // Apply now button
            item {
                Spacer(Modifier.height(Spacing.xs))
                Button(
                    onClick = viewModel::runNow,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isRunning && state.previewDeleteCount > 0
                ) {
                    if (state.isRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(Spacing.xs))
                        Text("Pruning…")
                    } else {
                        Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.xs))
                        Text(
                            if (state.previewDeleteCount > 0)
                                "Apply Now — Delete ${state.previewDeleteCount} Backup(s)"
                            else "Nothing to Prune"
                        )
                    }
                }

                state.lastResult?.let { result ->
                    Spacer(Modifier.height(Spacing.xs))
                    Text(result, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item { Spacer(Modifier.height(Spacing.md)) }
        }
    }
}

@Composable
private fun StatsBanner(state: RetentionUiState) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(Spacing.md).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatChip(
                icon = Icons.Default.Folder,
                value = state.currentSnapshotCount.toString(),
                label = "Snapshots"
            )
            StatChip(
                icon = Icons.Default.Storage,
                value = "${state.currentTotalSizeMb} MB",
                label = "Used"
            )
        }
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = ObsidianColors.MoltenOrange, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PreviewBanner(state: RetentionUiState) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Icon(Icons.Default.Warning, null,
                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            Text(
                "Current policy would delete ${state.previewDeleteCount} snapshot(s) " +
                    "and free ~${state.previewFreedMb} MB",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RetentionModeCard(currentMode: String, onModeChange: (String) -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {

            data class ModeOption(val key: String, val label: String, val desc: String)
            val options = listOf(
                ModeOption("COUNT", "Keep Last N",     "Keep only the most recent N snapshots."),
                ModeOption("DAYS",  "Keep by Age",     "Delete snapshots older than N days."),
                ModeOption("BOTH",  "Count + Age",     "Apply both rules — whichever triggers first.")
            )

            options.forEach { opt ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = currentMode == opt.key,
                        onClick = { onModeChange(opt.key) }
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(opt.label, fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium)
                        Text(opt.desc, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun CountPolicyCard(keepCount: Int, onCountChange: (Int) -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text("Keep Last N Snapshots", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Spacing.sm))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = { if (keepCount > 1) onCountChange(keepCount - 1) },
                    enabled = keepCount > 1
                ) { Icon(Icons.Default.Remove, "Decrease") }

                Text(
                    if (keepCount == 0) "Unlimited" else keepCount.toString(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                IconButton(
                    onClick = { onCountChange(keepCount + 1) },
                    enabled = keepCount < 999
                ) { Icon(Icons.Default.Add, "Increase") }
            }

            Slider(
                value = keepCount.toFloat().coerceIn(1f, 50f),
                onValueChange = { onCountChange(it.toInt()) },
                valueRange = 1f..50f,
                steps = 48,
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("1", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("50+", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AgePolicyCard(retentionDays: Int, onDaysChange: (Int) -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text("Delete Snapshots Older Than", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Spacing.sm))

            val presets = listOf(7, 14, 30, 60, 90, 180, 365)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                presets.take(4).forEach { preset ->
                    FilterChip(
                        selected = retentionDays == preset,
                        onClick = { onDaysChange(preset) },
                        label = { Text("${preset}d", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                presets.drop(4).forEach { preset ->
                    FilterChip(
                        selected = retentionDays == preset,
                        onClick = { onDaysChange(preset) },
                        label = { Text(if (preset >= 365) "1yr" else "${preset}d",
                            style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xs))
            Text(
                "Currently: $retentionDays days${if (retentionDays == 0) " (unlimited)" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
