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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.obsidianbackup.community.*
import com.obsidianbackup.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onNavigateToFeedback: () -> Unit = {},
    onNavigateToChangelog: () -> Unit = {},
    onNavigateToTips: () -> Unit = {},
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val communityLinks by viewModel.communityLinks.collectAsState()
    val betaEnrolled by viewModel.betaEnrolled.collectAsState()
    val analyticsEnabled by viewModel.analyticsEnabled.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.NavigateToFeedback -> onNavigateToFeedback()
                is NavigationEvent.NavigateToChangelog -> onNavigateToChangelog()
                is NavigationEvent.NavigateToTips -> onNavigateToTips()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = Spacing.md)
    ) {
            // Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.md)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Community",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = "Join Our Community",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Connect with other users, get support, and help shape the future of ObsidianBackup",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Community Links
            item {
                Text(
                    text = "Community Channels",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            items(communityLinks) { link ->
                CommunityLinkItem(
                    link = link,
                    onClick = { viewModel.openCommunityLink(link) }
                )
            }
            
            // Feedback Section
            item {
                Text(
                    text = "Feedback & Support",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = Spacing.md)
                )
            }
            
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.navigateToFeedback() }
                ) {
                    ListItem(
                        headlineContent = { Text("Send Feedback") },
                        supportingContent = { Text("Report bugs or suggest features") },
                        leadingContent = {
                            Icon(Icons.Default.Feedback, contentDescription = "Feedback")
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Navigate")
                        }
                    )
                }
            }
            
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.navigateToChangelog() }
                ) {
                    ListItem(
                        headlineContent = { Text("Changelog") },
                        supportingContent = { Text("See what's new") },
                        leadingContent = {
                            Icon(Icons.Default.NewReleases, contentDescription = "Changelog")
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Navigate")
                        }
                    )
                }
            }
            
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.navigateToTips() }
                ) {
                    ListItem(
                        headlineContent = { Text("Tips & Tricks") },
                        supportingContent = { Text("Learn best practices") },
                        leadingContent = {
                            Icon(Icons.Default.Lightbulb, contentDescription = "Tips")
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Navigate")
                        }
                    )
                }
            }
            
            // Beta Program
            item {
                Text(
                    text = "Beta Program",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = Spacing.md)
                )
            }
            
            item {
                BetaProgramCard(
                    enrolled = betaEnrolled,
                    onEnrollClick = { viewModel.enrollInBeta() },
                    onLeaveClick = { viewModel.leaveBeta() }
                )
            }
            
            // Privacy Settings
            item {
                Text(
                    text = "Privacy",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = Spacing.md)
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.md)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Privacy-Respecting Analytics",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Help us improve (no PII collected)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = analyticsEnabled,
                                onCheckedChange = { viewModel.setAnalyticsEnabled(it) }
                            )
                        }
                    }
                }
            }
        }
}

@Composable
fun CommunityLinkItem(
    link: CommunityLink,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        ListItem(
            headlineContent = { Text(link.name) },
            supportingContent = { Text(link.description) },
            leadingContent = {
                Icon(
                    imageVector = when (link.icon) {
                        "discord" -> Icons.AutoMirrored.Filled.Chat
                        "reddit" -> Icons.Default.Forum
                        "github" -> Icons.Default.Code
                        "docs" -> Icons.Default.Description
                        "support" -> Icons.Default.Support
                        else -> Icons.Default.Link
                    },
                    contentDescription = link.name
                )
            },
            trailingContent = {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open externally")
            }
        )
    }
}

@Composable
fun BetaProgramCard(
    enrolled: Boolean,
    onEnrollClick: () -> Unit,
    onLeaveClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enrolled) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Science,
                    contentDescription = "Beta program",
                    tint = if (enrolled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (enrolled) "Beta Tester" else "Join Beta Program",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (enrolled) 
                            "You're helping shape the future!" 
                        else 
                            "Get early access to new features",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            Button(
                onClick = if (enrolled) onLeaveClick else onEnrollClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (enrolled) "Leave Beta" else "Enroll in Beta")
            }
        }
    }
}
