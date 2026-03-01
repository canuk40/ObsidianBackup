package com.obsidianbackup.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Apps : Screen("apps", "Apps", Icons.Default.Android)
    object Backups : Screen("backups", "Backups", Icons.Default.Backup)
    object Automation : Screen("automation", "Automation", Icons.Default.Schedule)
    object Gaming : Screen("gaming", "Gaming", Icons.Default.Gamepad)
    object Health : Screen("health", "Health", Icons.Default.Favorite)
    object Plugins : Screen("plugins", "Plugins", Icons.Default.Extension)
    object Logs : Screen("logs", "Logs", Icons.AutoMirrored.Filled.List)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object SmartScheduling : Screen("smart_scheduling", "Smart Scheduling", Icons.Default.AutoAwesome)
    object FeatureFlags : Screen("feature_flags", "Feature Flags", Icons.Default.Flag)
    object Community : Screen("community", "Community", Icons.Default.People)
    object Feedback : Screen("feedback", "Feedback", Icons.Default.Feedback)
    object Changelog : Screen("changelog", "Changelog", Icons.Default.NewReleases)
    object Tips : Screen("tips", "Tips", Icons.Default.Lightbulb)
    object Onboarding : Screen("onboarding", "Onboarding", Icons.AutoMirrored.Filled.Help)

    companion object {
        val mainItems = listOf(Dashboard, Apps, Backups, Automation, Logs, Settings)
        val drawerItems = listOf(Dashboard, Apps, Backups, Automation, Gaming, Health, Plugins, Logs, Settings, Community)
        val items = drawerItems
    }
}
