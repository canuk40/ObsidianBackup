package com.obsidianbackup.ui.screens.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obsidianbackup.community.*
import com.obsidianbackup.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(
    viewModel: ChangelogViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val changelog by viewModel.changelog.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Changelog") },
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
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            items(changelog) { entry ->
                ChangelogEntryCard(entry)
            }
        }
    }
}

@Composable
fun ChangelogEntryCard(entry: ChangelogEntry) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Version ${entry.version}",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = entry.releaseDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            entry.highlights?.let {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            entry.changes.forEach { change ->
                Row(
                    modifier = Modifier.padding(vertical = Spacing.xs)
                ) {
                    Icon(
                        imageVector = when (change.type) {
                            ChangeType.FEATURE -> Icons.Default.NewReleases
                            ChangeType.IMPROVEMENT -> Icons.AutoMirrored.Filled.TrendingUp
                            ChangeType.BUGFIX -> Icons.Default.BugReport
                            ChangeType.SECURITY -> Icons.Default.Security
                            ChangeType.DEPRECATED -> Icons.Default.Warning
                        },
                        contentDescription = "${change.type.name}: ${change.description}",
                        modifier = Modifier.size(16.dp),
                        tint = when (change.type) {
                            ChangeType.FEATURE -> MaterialTheme.colorScheme.primary
                            ChangeType.IMPROVEMENT -> MaterialTheme.colorScheme.tertiary
                            ChangeType.BUGFIX -> MaterialTheme.colorScheme.error
                            ChangeType.SECURITY -> MaterialTheme.colorScheme.error
                            ChangeType.DEPRECATED -> MaterialTheme.colorScheme.outline
                        }
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = change.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipsScreen(
    viewModel: TipsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val tips by viewModel.tips.collectAsState()
    val dismissedTips by viewModel.dismissedTips.collectAsState()
    val tipOfTheDay by viewModel.tipOfTheDay.collectAsState()
    
    var selectedCategory by remember { mutableStateOf<TipCategory?>(null) }
    
    val filteredTips = if (selectedCategory != null) {
        tips.filter { it.category == selectedCategory }
    } else {
        tips
    }.filterNot { it.id in dismissedTips }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tips & Tricks") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetTips() }) {
                        Icon(Icons.Default.Refresh, "Reset")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            tipOfTheDay?.let { tip ->
                item {
                    TipOfTheDayCard(
                        tip = tip,
                        onDismiss = { viewModel.dismissTip(tip.id) }
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All") }
                    )
                    TipCategory.values().forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.name.replace("_", " ")) }
                        )
                    }
                }
            }
            
            items(filteredTips) { tip ->
                TipCard(
                    tip = tip,
                    onDismiss = { viewModel.dismissTip(tip.id) }
                )
            }
        }
    }
}

@Composable
fun TipOfTheDayCard(tip: Tip, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row {
                    Icon(Icons.Default.Lightbulb, "Tip of the Day", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text("Tip of the Day", style = MaterialTheme.typography.titleMedium)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Dismiss tip", modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(tip.title, style = MaterialTheme.typography.titleSmall)
            Text(tip.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun TipCard(tip: Tip, onDismiss: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(tip.title, style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Dismiss tip", modifier = Modifier.size(16.dp))
                }
            }
            Text(tip.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(Spacing.xs))
            AssistChip(
                onClick = {},
                label = { Text(tip.category.name.replace("_", " "), style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}
