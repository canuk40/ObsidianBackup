package com.obsidianbackup.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.obsidianbackup.ui.components.animations.EmptyStateAnimation
import com.obsidianbackup.ui.theme.Spacing
import com.obsidianbackup.ui.theme.IconSize

/**
 * Empty state component with illustration and call to action
 */
@Composable
fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon or animation
        if (icon != null) {
            icon()
        } else {
            EmptyStateAnimation()
        }
        
        Spacer(modifier = Modifier.height(Spacing.lg))
        
        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(Spacing.xs))
        
        // Description
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Action button
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            Button(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}

/**
 * Empty state for no backups
 */
@Composable
fun NoBackupsEmptyState(
    onCreateBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Backups Yet",
        description = "Create your first backup to protect your apps and data",
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Default.BackupTable,
                contentDescription = null,
                modifier = Modifier.size(IconSize.hero),
                tint = MaterialTheme.colorScheme.primaryContainer
            )
        },
        actionLabel = "Create Backup",
        onActionClick = onCreateBackup
    )
}

/**
 * Empty state for no apps selected
 */
@Composable
fun NoAppsSelectedEmptyState(
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Apps Selected",
        description = "Select apps from the list to backup",
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = null,
                modifier = Modifier.size(IconSize.hero),
                tint = MaterialTheme.colorScheme.primaryContainer
            )
        }
    )
}

/**
 * Empty state for no search results
 */
@Composable
fun NoSearchResultsEmptyState(
    query: String,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Results Found",
        description = "No apps match \"$query\". Try a different search term.",
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(IconSize.hero),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

/**
 * Empty state for no logs
 */
@Composable
fun NoLogsEmptyState(
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Logs Yet",
        description = "Backup operations will appear here",
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Article,
                contentDescription = null,
                modifier = Modifier.size(IconSize.hero),
                tint = MaterialTheme.colorScheme.primaryContainer
            )
        }
    )
}

/**
 * Empty state for cloud not connected
 */
@Composable
fun CloudNotConnectedEmptyState(
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "Cloud Not Connected",
        description = "Connect to a cloud service to enable automatic backups and sync",
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.error
            )
        },
        actionLabel = "Connect Cloud",
        onActionClick = onConnect
    )
}

/**
 * Empty state for no automation rules
 */
@Composable
fun NoAutomationRulesEmptyState(
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Automation Rules",
        description = "Create automation rules to schedule automatic backups",
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Rule,
                contentDescription = null,
                modifier = Modifier.size(IconSize.hero),
                tint = MaterialTheme.colorScheme.primaryContainer
            )
        },
        actionLabel = "Create Rule",
        onActionClick = onCreate
    )
}

/**
 * Generic error state
 */
@Composable
fun ErrorState(
    title: String,
    description: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = title,
        description = description,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(IconSize.hero),
                tint = MaterialTheme.colorScheme.error
            )
        },
        actionLabel = if (onRetry != null) "Retry" else null,
        onActionClick = onRetry
    )
}
