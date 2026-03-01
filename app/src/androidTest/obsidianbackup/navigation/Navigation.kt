// Navigation.kt
package com.titanbackup.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Apps : Screen("apps", "Apps", Icons.Default.Apps)
    object Backups : Screen("backups", "Backups", Icons.Default.Backup)
    object Automation : Screen("automation", "Automation", Icons.Default.Schedule)
    object Logs : Screen("logs", "Logs", Icons.Default.List)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        val items = listOf(Dashboard, Apps, Backups, Automation, Logs, Settings)
    }
}