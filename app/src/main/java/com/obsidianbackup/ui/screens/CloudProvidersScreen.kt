package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.*
import com.obsidianbackup.ui.theme.Spacing
import com.obsidianbackup.ui.theme.Elevation
import com.obsidianbackup.ui.theme.IconSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudProvidersScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProvider: (String) -> Unit = {},
    viewModel: CloudProvidersViewModel = hiltViewModel()
) {
    Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                "Configure cloud storage providers",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Provider list
            Text(
                "Connected Providers",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = Spacing.sm)
            )

            CloudProviderCard(
                name = "Google Drive",
                icon = Icons.Default.Cloud,
                status = "Not Connected",
                statusColor = MaterialTheme.colorScheme.outline,
                onClick = { onNavigateToProvider("google_drive") }
            )

            CloudProviderCard(
                name = "Dropbox",
                icon = Icons.Default.CloudUpload,
                status = "Not Connected",
                statusColor = MaterialTheme.colorScheme.outline,
                onClick = { onNavigateToProvider("dropbox") }
            )

            CloudProviderCard(
                name = "OneDrive",
                icon = Icons.Default.CloudQueue,
                status = "Not Connected",
                statusColor = MaterialTheme.colorScheme.outline,
                onClick = { onNavigateToProvider("onedrive") }
            )

            // AWS S3 — temporarily hidden pending credential setup
            // CloudProviderCard(name = "AWS S3", ...)

            // Backblaze B2 — temporarily hidden pending credential setup
            // CloudProviderCard(name = "Backblaze B2", ...)

            CloudProviderCard(
                name = "Filecoin/IPFS",
                icon = Icons.Default.Public,
                status = "Not Connected",
                statusColor = MaterialTheme.colorScheme.outline,
                onClick = { onNavigateToProvider("filecoin") }
            )

            CloudProviderCard(
                name = "Oracle Cloud",
                icon = Icons.Default.CloudCircle,
                status = "Not Connected",
                statusColor = MaterialTheme.colorScheme.outline,
                onClick = { onNavigateToProvider("oracle_cloud") }
            )
        }
}

@Composable
fun CloudProviderCard(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    status: String,
    statusColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit = {}
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.large),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusIndicator(statusColor)
                        Text(
                            status,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(color: androidx.compose.ui.graphics.Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "status_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "status_alpha"
    )
    
    Surface(
        modifier = Modifier
            .size(8.dp)
            .alpha(alpha),
        shape = MaterialTheme.shapes.small,
        color = color
    ) {}
}
