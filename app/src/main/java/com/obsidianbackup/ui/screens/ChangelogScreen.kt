package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(onNavigateBack: () -> Unit) {
    val changelogEntries = remember {
        listOf(
            ChangelogEntry("1.0.0-alpha", "Initial alpha release", listOf(
                "Root and Shizuku permission support",
                "APK and data backup",
                "Gaming save backup support",
                "Health Connect integration",
                "Plugin system foundation"
            ))
        )
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            items(changelogEntries) { entry ->
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Version ${entry.version}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            entry.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        entry.changes.forEach { change ->
                            Row {
                                Text("• ", style = MaterialTheme.typography.bodyMedium)
                                Text(change, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
}

data class ChangelogEntry(
    val version: String,
    val description: String,
    val changes: List<String>
)
