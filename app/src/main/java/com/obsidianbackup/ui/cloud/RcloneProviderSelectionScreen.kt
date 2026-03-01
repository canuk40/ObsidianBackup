// ui/cloud/RcloneProviderSelectionScreen.kt
package com.obsidianbackup.ui.cloud

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.obsidianbackup.cloud.rclone.RcloneProviderFactory
import com.obsidianbackup.cloud.rclone.ProviderInfo

/**
 * Provider selection screen for rclone-based cloud storage
 * Displays all supported providers from RcloneProviderFactory
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RcloneProviderSelectionScreen(
    onProviderSelected: (RcloneProviderFactory.ProviderType) -> Unit,
    onNavigateBack: () -> Unit
) {
    val supportedProviders = remember { RcloneProviderFactory.getSupportedProviders() }
    
    LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Choose a cloud storage provider",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(supportedProviders) { provider ->
                ProviderCard(
                    provider = provider,
                    onClick = { onProviderSelected(provider.type) }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                InfoCard()
            }
        }
}

@Composable
private fun ProviderCard(
    provider: ProviderInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Provider icon
            Icon(
                imageVector = getProviderIcon(provider.type),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            // Provider info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = provider.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = provider.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Additional info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (provider.requiresOAuth) {
                        Chip(
                            label = "OAuth2",
                            icon = Icons.Default.Lock
                        )
                    }
                    
                    Chip(
                        label = formatMaxFileSize(provider.maxFileSize),
                        icon = Icons.Default.Storage
                    )
                }
            }
            
            // Navigate arrow
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Configure",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Chip(
    label: String,
    icon: ImageVector
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "About rclone integration",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Text(
                    text = "ObsidianBackup uses rclone to support 40+ cloud storage providers. " +
                            "After selecting a provider, you'll need to configure authentication.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

private fun getProviderIcon(type: RcloneProviderFactory.ProviderType): ImageVector {
    return when (type) {
        RcloneProviderFactory.ProviderType.GOOGLE_DRIVE -> Icons.Default.CloudQueue
        RcloneProviderFactory.ProviderType.DROPBOX -> Icons.Default.CloudUpload
        RcloneProviderFactory.ProviderType.S3 -> Icons.Default.Storage
        RcloneProviderFactory.ProviderType.ONEDRIVE -> Icons.Default.Cloud
        RcloneProviderFactory.ProviderType.BACKBLAZE_B2 -> Icons.Default.CloudDownload
        RcloneProviderFactory.ProviderType.WEBDAV -> Icons.Default.CloudCircle
        RcloneProviderFactory.ProviderType.SFTP -> Icons.Default.Storage
    }
}

private fun formatMaxFileSize(bytes: Long): String {
    return when {
        bytes == Long.MAX_VALUE -> "Unlimited"
        bytes >= 1_000_000_000_000L -> "${bytes / 1_000_000_000_000L} TB"
        bytes >= 1_000_000_000L -> "${bytes / 1_000_000_000L} GB"
        else -> "${bytes / 1_000_000L} MB"
    }
}

/**
 * Configuration screen for a specific rclone provider
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RcloneProviderConfigScreen(
    providerType: RcloneProviderFactory.ProviderType,
    onSave: (Map<String, String>) -> Unit,
    onNavigateBack: () -> Unit
) {
    var credentials by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var remoteName by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure ${providerType.name}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Remote name field
            OutlinedTextField(
                value = remoteName,
                onValueChange = { remoteName = it },
                label = { Text("Remote Name") },
                placeholder = { Text("e.g., my-${providerType.name.lowercase()}") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Provider-specific credential fields
            when (providerType) {
                RcloneProviderFactory.ProviderType.GOOGLE_DRIVE -> GoogleDriveFields(
                    credentials = credentials,
                    onCredentialsChange = { credentials = it }
                )
                RcloneProviderFactory.ProviderType.DROPBOX -> DropboxFields(
                    credentials = credentials,
                    onCredentialsChange = { credentials = it }
                )
                RcloneProviderFactory.ProviderType.ONEDRIVE -> OneDriveFields(
                    credentials = credentials,
                    onCredentialsChange = { credentials = it }
                )
                RcloneProviderFactory.ProviderType.S3 -> S3Fields(
                    credentials = credentials,
                    onCredentialsChange = { credentials = it }
                )
                else -> GenericFields(
                    credentials = credentials,
                    onCredentialsChange = { credentials = it }
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Save button
            val isOAuthProvider = providerType in listOf(
                RcloneProviderFactory.ProviderType.GOOGLE_DRIVE,
                RcloneProviderFactory.ProviderType.DROPBOX,
                RcloneProviderFactory.ProviderType.ONEDRIVE
            )
            Button(
                onClick = { 
                    val config = credentials.toMutableMap()
                    config["remote_name"] = remoteName
                    config["provider_type"] = providerType.name
                    onSave(config)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = remoteName.isNotBlank() && (isOAuthProvider || credentials.isNotEmpty())
            ) {
                Text("Save Configuration")
            }
        }
    }
}

@Composable
private fun GoogleDriveFields(
    credentials: Map<String, String>,
    onCredentialsChange: (Map<String, String>) -> Unit
) {
    val context = LocalContext.current
    val isConnected = credentials["token"]?.isNotBlank() == true
    OAuthSignInCard(
        providerName = "Google Drive",
        isConnected = isConnected,
        buttonLabel = "Sign in with Google",
        buttonIcon = Icons.Default.Cloud,
        onSignIn = {
            // Launch browser OAuth flow; token is received via deep link obsidianbackup://oauth2redirect
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://accounts.google.com/o/oauth2/v2/auth")
                    .buildUpon()
                    .appendQueryParameter("scope", "https://www.googleapis.com/auth/drive.file")
                    .appendQueryParameter("response_type", "code")
                    .appendQueryParameter("redirect_uri", "obsidianbackup://oauth2redirect")
                    .build()
            )
            context.startActivity(intent)
        }
    )
}

@Composable
private fun DropboxFields(
    credentials: Map<String, String>,
    onCredentialsChange: (Map<String, String>) -> Unit
) {
    val context = LocalContext.current
    val isConnected = credentials["token"]?.isNotBlank() == true
    OAuthSignInCard(
        providerName = "Dropbox",
        isConnected = isConnected,
        buttonLabel = "Connect Dropbox",
        buttonIcon = Icons.Default.CloudUpload,
        onSignIn = {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://www.dropbox.com/oauth2/authorize")
                    .buildUpon()
                    .appendQueryParameter("response_type", "code")
                    .appendQueryParameter("redirect_uri", "obsidianbackup://oauth2redirect")
                    .build()
            )
            context.startActivity(intent)
        }
    )
}

@Composable
private fun OneDriveFields(
    credentials: Map<String, String>,
    onCredentialsChange: (Map<String, String>) -> Unit
) {
    val context = LocalContext.current
    val isConnected = credentials["token"]?.isNotBlank() == true
    OAuthSignInCard(
        providerName = "OneDrive",
        isConnected = isConnected,
        buttonLabel = "Sign in with Microsoft",
        buttonIcon = Icons.Default.CloudQueue,
        onSignIn = {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://login.microsoftonline.com/common/oauth2/v2.0/authorize")
                    .buildUpon()
                    .appendQueryParameter("scope", "Files.ReadWrite offline_access")
                    .appendQueryParameter("response_type", "code")
                    .appendQueryParameter("redirect_uri", "obsidianbackup://oauth2redirect")
                    .build()
            )
            context.startActivity(intent)
        }
    )
}

@Composable
private fun OAuthSignInCard(
    providerName: String,
    isConnected: Boolean,
    buttonLabel: String,
    buttonIcon: ImageVector,
    onSignIn: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isConnected) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$providerName connected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                OutlinedButton(
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reconnect")
                }
            } else {
                Text(
                    text = "$providerName uses secure OAuth2 — no password needed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(buttonIcon, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(buttonLabel)
                }
            }
        }
    }
}

@Composable
private fun S3Fields(
    credentials: Map<String, String>,
    onCredentialsChange: (Map<String, String>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "S3 Compatible Storage Configuration",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        OutlinedTextField(
            value = credentials["access_key_id"] ?: "",
            onValueChange = { 
                onCredentialsChange(credentials + ("access_key_id" to it))
            },
            label = { Text("Access Key ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = credentials["secret_access_key"] ?: "",
            onValueChange = { 
                onCredentialsChange(credentials + ("secret_access_key" to it))
            },
            label = { Text("Secret Access Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = credentials["region"] ?: "us-east-1",
            onValueChange = { 
                onCredentialsChange(credentials + ("region" to it))
            },
            label = { Text("Region") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = credentials["endpoint"] ?: "",
            onValueChange = { 
                onCredentialsChange(credentials + ("endpoint" to it))
            },
            label = { Text("Endpoint (Optional)") },
            placeholder = { Text("For S3-compatible services") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = credentials["bucket"] ?: "",
            onValueChange = { 
                onCredentialsChange(credentials + ("bucket" to it))
            },
            label = { Text("Bucket Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
private fun GenericFields(
    credentials: Map<String, String>,
    onCredentialsChange: (Map<String, String>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Provider-specific configuration",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "Consult the rclone documentation for this provider's required credentials.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
