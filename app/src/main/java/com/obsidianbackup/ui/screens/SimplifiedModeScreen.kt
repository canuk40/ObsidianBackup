package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obsidianbackup.accessibility.SimplifiedModeViewModel
import com.obsidianbackup.ui.theme.Spacing

/**
 * Simplified mode screen for elderly users and enhanced accessibility.
 * Features:
 * - Extra large touch targets (minimum 72dp height)
 * - High contrast colors
 * - Large text (minimum 20sp)
 * - Simple, clear language
 * - Reduced UI complexity
 */
@Composable
fun SimplifiedModeScreen(
    viewModel: SimplifiedModeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Simple Backup",
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.semantics { heading() },
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(Spacing.md))
        
        // Status message
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = uiState.lastAction,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(Spacing.lg)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Large action buttons
        SimplifiedButton(
            text = "Backup Now",
            icon = Icons.Default.Backup,
            onClick = { viewModel.performBackup() },
            enabled = !uiState.isBackupInProgress && !uiState.isRestoreInProgress,
            isLoading = uiState.isBackupInProgress,
            contentDescription = "Tap to backup all your applications now"
        )
        
        SimplifiedButton(
            text = "Restore",
            icon = Icons.Default.RestorePage,
            onClick = { viewModel.performRestore() },
            enabled = !uiState.isBackupInProgress && !uiState.isRestoreInProgress,
            isLoading = uiState.isRestoreInProgress,
            contentDescription = "Tap to restore applications from backup"
        )
        
        SimplifiedButton(
            text = "View Backups",
            icon = Icons.AutoMirrored.Filled.List,
            onClick = { viewModel.viewBackups() },
            enabled = !uiState.isBackupInProgress && !uiState.isRestoreInProgress,
            contentDescription = "Tap to view list of all backups"
        )
        
        Spacer(modifier = Modifier.height(Spacing.md))
    }
    
    // Show loading overlay if needed
    if (uiState.isBackupInProgress || uiState.isRestoreInProgress) {
        LoadingOverlay()
    }
}

@Composable
private fun SimplifiedButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    contentDescription: String
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp) // Extra large touch target
            .semantics { 
                this.contentDescription = contentDescription
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 4.dp
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 6.dp
        )
    }
}
