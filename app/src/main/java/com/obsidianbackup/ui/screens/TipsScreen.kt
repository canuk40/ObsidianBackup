package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipsScreen(onNavigateBack: () -> Unit) {
    val tips = remember {
        listOf(
            Tip("Enable Compression", "Reduce backup size by up to 70% with compression. Find it in Settings > Compression."),
            Tip("Use Incremental Backups", "Save time and space by only backing up changed files. Enable in backup options."),
            Tip("Verify Backups", "Always verify backups after creation to ensure data integrity. Enable in Settings > Verification."),
            Tip("Schedule Automatic Backups", "Set up automatic backups to run during off-peak hours. Go to Settings > Auto Backup."),
            Tip("Gaming Saves", "Backup your emulator saves and game data. Visit the Gaming screen to configure."),
            Tip("Root vs Shizuku", "Root provides full access, but Shizuku is a safer alternative that doesn't require device rooting.")
        )
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            items(tips) { tip ->
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    tip.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    tip.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
}

data class Tip(val title: String, val description: String)
