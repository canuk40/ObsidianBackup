// ui/screens/HealthScreen.kt
package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.Column
import com.obsidianbackup.ui.theme.Spacing
import com.obsidianbackup.ui.theme.IconSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obsidianbackup.presentation.health.HealthViewModel
import com.obsidianbackup.ui.components.EnhancedButton

@Composable
fun HealthScreen(
    viewModel: HealthViewModel = hiltViewModel(),
    onNavigateToHealthPrivacy: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    val privacySettings by viewModel.privacySettings.collectAsState()

    if (!uiState.featureEnabled) {
        DisabledFeatureMessage(PaddingValues(0.dp), "Health Connect Sync")
    } else {
        HealthContent(
            padding = PaddingValues(0.dp),
            uiState = uiState,
            onRequestPermissions = { /* Health Connect removed — pending org account setup */ },
            onExport = { start, end ->
                viewModel.backupHealthData()
            },
            onUpdatePrivacy = viewModel::updatePrivacySetting,
            anonymize = privacySettings.anonymizeData,
            onNavigateToHealthPrivacy = onNavigateToHealthPrivacy
        )
    }
}

@Composable
private fun HealthContent(
    padding: PaddingValues,
    uiState: com.obsidianbackup.presentation.health.HealthUiState,
    onRequestPermissions: () -> Unit,
    onExport: (Long, Long) -> Unit,
    onUpdatePrivacy: (Boolean) -> Unit,
    anonymize: Boolean,
    onNavigateToHealthPrivacy: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        if (!uiState.healthConnectAvailable) {
            InfoCard(
                title = "Health Connect Not Available",
                message = "Health Connect is not installed on this device. Please install it from the Play Store.",
                icon = Icons.Default.Warning,
                color = MaterialTheme.colorScheme.error
            )
        } else if (!uiState.permissionGranted) {
            InfoCard(
                title = "Permissions Required",
                message = "Grant Health Connect permissions to backup your health data.",
                icon = Icons.Default.HealthAndSafety
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            EnhancedButton(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Security, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text("Grant Permissions")
            }
        } else {
            // Health Connect branding
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color(0xFF34A853) // Health Connect green
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(IconSize.large)
                    )
                    Column {
                        Text(
                            "Health Connect",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            "Connected & Ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            
            // Data types with icons
            Text(
                "Available Data Types",
                style = MaterialTheme.typography.titleMedium
            )
            
            HealthDataTypeCard(
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                title = "Steps",
                description = "Daily step count",
                color = MaterialTheme.colorScheme.primary
            )
            
            HealthDataTypeCard(
                icon = Icons.Default.Favorite,
                title = "Heart Rate",
                description = "BPM measurements",
                color = Color(0xFFE91E63) // Pink for heart
            )
            
            HealthDataTypeCard(
                icon = Icons.Default.Bedtime,
                title = "Sleep",
                description = "Sleep sessions",
                color = Color(0xFF9C27B0) // Purple for sleep
            )
            
            // Sync progress
            if (uiState.isRequestingPermissions) {
                ElevatedCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md)
                    ) {
                        Text(
                            "Syncing Health Data",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF34A853)
                        )
                    }
                }
            }
            
            // Privacy settings
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PrivacyTip,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                "Anonymize Data",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Remove personal identifiers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = anonymize,
                        onCheckedChange = onUpdatePrivacy
                    )
                }
            }
            
            EnhancedButton(
                onClick = { onExport(0, System.currentTimeMillis()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Backup, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text("Backup Health Data")
            }

            OutlinedButton(
                onClick = onNavigateToHealthPrivacy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PrivacyTip, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text("Privacy & Data Permissions")
            }
        }
    }
}

@Composable
private fun HealthDataTypeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(IconSize.xlarge)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(IconSize.large)
                    )
                }
            }
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = color)
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
