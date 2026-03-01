package com.obsidianbackup.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obsidianbackup.R
import com.obsidianbackup.accessibility.AccessibilityHelper
import com.obsidianbackup.permissions.PermissionManager
import com.obsidianbackup.presentation.dashboard.DashboardViewModel
import com.obsidianbackup.navigation.Screen
import com.obsidianbackup.ui.components.EnhancedButton
import com.obsidianbackup.ui.theme.ObsidianColors
import com.obsidianbackup.ui.theme.Spacing
import com.obsidianbackup.ui.theme.Elevation
import com.obsidianbackup.ui.theme.IconSize
import com.obsidianbackup.ui.utils.Animations

@Composable
fun DashboardScreen(
    permissionManager: PermissionManager,
    onNavigate: (Screen) -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {}
) {
    val viewModel: DashboardViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val capabilities by permissionManager.capabilities.collectAsState()
    val context = LocalContext.current
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()

    LaunchedEffect(onboardingCompleted) {
        if (!onboardingCompleted) {
            onNavigateToOnboarding()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.md)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        // Quick Stats row - 3 mini cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            StatMiniCard(
                icon = Icons.Default.Backup,
                label = stringResource(R.string.stat_label_backups),
                value = "${state.totalBackups}",
                color = ObsidianColors.MoltenOrange,
                modifier = Modifier.weight(1f)
            )
            StatMiniCard(
                icon = Icons.Default.Schedule,
                label = stringResource(R.string.stat_label_last),
                value = state.lastBackupText,
                color = ObsidianColors.MoltenAmber,
                modifier = Modifier.weight(1f)
            )
            StatMiniCard(
                icon = Icons.Default.Storage,
                label = stringResource(R.string.stat_label_size),
                value = state.totalSizeText,
                color = ObsidianColors.MoltenGold,
                modifier = Modifier.weight(1f)
            )
        }

        // Permission status card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() },
            colors = CardDefaults.cardColors(
                containerColor = ObsidianColors.Surface
            ),
            border = BorderStroke(1.dp, ObsidianColors.Border)
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = ObsidianColors.RootGranted,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = stringResource(R.string.permission_status_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Mode: ${state.currentMode.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(Spacing.xs))

                CapabilityRow(stringResource(R.string.capability_backup_apk), capabilities.canBackupApk)
                CapabilityRow(stringResource(R.string.capability_backup_data), capabilities.canBackupData)
                CapabilityRow(stringResource(R.string.capability_incremental), capabilities.canDoIncremental)
                CapabilityRow(stringResource(R.string.capability_restore_selinux), capabilities.canRestoreSelinux)
            }
        }

        // Quick Actions
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.FlashOn,
                contentDescription = null,
                tint = ObsidianColors.MoltenOrange,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.quick_actions_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            EnhancedButton(
                onClick = { 
                    onNavigate(Screen.Apps)
                    AccessibilityHelper.announceForAccessibility(
                        context,
                        context.getString(R.string.cd_nav_apps)
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ObsidianColors.MoltenOrange
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .semantics { 
                        contentDescription = context.getString(R.string.cd_backup_button)
                    }
            ) {
                Icon(
                    Icons.Default.Backup, 
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(stringResource(R.string.action_backup), fontWeight = FontWeight.SemiBold)
            }

            EnhancedButton(
                onClick = { 
                    onNavigate(Screen.Backups)
                    AccessibilityHelper.announceForAccessibility(
                        context,
                        context.getString(R.string.cd_nav_backups)
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ObsidianColors.MoltenAmber
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .semantics { 
                        contentDescription = context.getString(R.string.cd_restore_button)
                    }
            ) {
                Icon(
                    Icons.Default.RestorePage, 
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(stringResource(R.string.action_restore), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StatMiniCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = ObsidianColors.Surface
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ObsidianColors.TextPrimary,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = ObsidianColors.TextTertiary
            )
        }
    }
}

@Composable
fun CapabilityRow(name: String, enabled: Boolean) {
    val context = LocalContext.current
    val contentDesc = stringResource(
        if (enabled) R.string.cd_capability_enabled else R.string.cd_capability_disabled,
        name
    )
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = Spacing.xxxs)
            .semantics { 
                contentDescription = contentDesc
            }
    ) {
        Icon(
            imageVector = if (enabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (enabled) ObsidianColors.RootGranted else ObsidianColors.Error,
            modifier = Modifier.size(IconSize.small)
        )
        Spacer(modifier = Modifier.width(Spacing.xs))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
