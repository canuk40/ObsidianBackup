package com.obsidianbackup.deeplink

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.obsidianbackup.MainActivity
import com.obsidianbackup.logging.ObsidianLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes deep link actions to appropriate destinations
 * Handles navigation and action execution
 */
@Singleton
class DeepLinkRouter @Inject constructor(
    private val context: Context,
    private val logger: ObsidianLogger
) {
    
    companion object {
        private const val TAG = "DeepLinkRouter"
        private const val SCHEME = "obsidianbackup"
        
        // Navigation routes
        const val EXTRA_NAVIGATION_ROUTE = "deep_link_navigation_route"
        const val EXTRA_ACTION_TYPE = "deep_link_action_type"
        const val EXTRA_ACTION_DATA = "deep_link_action_data"
        
        // Action types
        const val ACTION_BACKUP = "backup"
        const val ACTION_RESTORE = "restore"
        const val ACTION_NAVIGATE = "navigate"
    }
    
    /**
     * Route a deep link action
     */
    fun route(action: DeepLinkAction, activity: FragmentActivity): RouteResult {
        logger.d(TAG, "Routing action: ${action.javaClass.simpleName}")
        
        return when (action) {
            is DeepLinkAction.StartBackup -> routeBackup(action, activity)
            is DeepLinkAction.RestoreSnapshot -> routeRestore(action, activity)
            is DeepLinkAction.OpenDashboard -> routeNavigation("dashboard", activity)
            is DeepLinkAction.OpenBackups -> routeNavigation("backups", activity)
            is DeepLinkAction.OpenSettings -> routeNavigation("settings", activity)
            is DeepLinkAction.OpenSettingsScreen -> routeSettingsScreen(action, activity)
            is DeepLinkAction.OpenAutomation -> routeNavigation("automation", activity)
            is DeepLinkAction.OpenLogs -> routeNavigation("logs", activity)
            is DeepLinkAction.ConnectCloudProvider -> routeCloudConnect(action, activity)
            is DeepLinkAction.OpenCloudSettings -> routeNavigation("settings/cloud", activity)
            is DeepLinkAction.OpenAppDetails -> routeAppDetails(action, activity)
            is DeepLinkAction.Invalid -> RouteResult(false, errorMessage = action.reason)
        }
    }
    
    private fun routeBackup(action: DeepLinkAction.StartBackup, activity: FragmentActivity): RouteResult {
        return try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_ACTION_TYPE, ACTION_BACKUP)
                putExtra(EXTRA_NAVIGATION_ROUTE, "backups")
                
                // Add package filter if specified
                action.packageNames?.let { packages ->
                    putExtra("packages", packages.toTypedArray())
                }
                putExtra("includeData", action.includeData)
                putExtra("includeApk", action.includeApk)
            }
            
            activity.startActivity(intent)
            RouteResult(true)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to route backup action", e)
            RouteResult(false, errorMessage = e.message)
        }
    }
    
    private fun routeRestore(action: DeepLinkAction.RestoreSnapshot, activity: FragmentActivity): RouteResult {
        return try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_ACTION_TYPE, ACTION_RESTORE)
                putExtra(EXTRA_NAVIGATION_ROUTE, "backups")
                putExtra("snapshotId", action.snapshotId)
                
                action.packageNames?.let { packages ->
                    putExtra("packages", packages.toTypedArray())
                }
            }
            
            activity.startActivity(intent)
            RouteResult(true)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to route restore action", e)
            RouteResult(false, errorMessage = e.message)
        }
    }
    
    private fun routeNavigation(route: String, activity: FragmentActivity): RouteResult {
        return try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_ACTION_TYPE, ACTION_NAVIGATE)
                putExtra(EXTRA_NAVIGATION_ROUTE, route)
            }
            
            activity.startActivity(intent)
            RouteResult(true)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to route navigation", e)
            RouteResult(false, errorMessage = e.message)
        }
    }
    
    private fun routeSettingsScreen(action: DeepLinkAction.OpenSettingsScreen, activity: FragmentActivity): RouteResult {
        val route = when (action.screen) {
            SettingsScreen.MAIN -> "settings"
            SettingsScreen.AUTOMATION -> "settings/automation"
            SettingsScreen.CLOUD -> "settings/cloud"
            SettingsScreen.SECURITY -> "settings/security"
            SettingsScreen.STORAGE -> "settings/storage"
            SettingsScreen.NOTIFICATIONS -> "settings/notifications"
            SettingsScreen.ADVANCED -> "settings/advanced"
            SettingsScreen.ABOUT -> "settings/about"
        }
        
        return routeNavigation(route, activity)
    }
    
    private fun routeCloudConnect(action: DeepLinkAction.ConnectCloudProvider, activity: FragmentActivity): RouteResult {
        return try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_ACTION_TYPE, "cloud_connect")
                putExtra(EXTRA_NAVIGATION_ROUTE, "settings/cloud")
                putExtra("provider", action.provider.name)
                putExtra("autoConnect", action.autoConnect)
            }
            
            activity.startActivity(intent)
            RouteResult(true)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to route cloud connect", e)
            RouteResult(false, errorMessage = e.message)
        }
    }
    
    private fun routeAppDetails(action: DeepLinkAction.OpenAppDetails, activity: FragmentActivity): RouteResult {
        return try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_ACTION_TYPE, ACTION_NAVIGATE)
                putExtra(EXTRA_NAVIGATION_ROUTE, "apps/${action.packageName}")
                putExtra("packageName", action.packageName)
            }
            
            activity.startActivity(intent)
            RouteResult(true)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to route app details", e)
            RouteResult(false, errorMessage = e.message)
        }
    }
    
    /**
     * Generate a deep link URI for an action
     */
    fun generateUri(action: DeepLinkAction): Uri? {
        return when (action) {
            is DeepLinkAction.StartBackup -> {
                Uri.Builder()
                    .scheme(SCHEME)
                    .authority("")
                    .path("backup")
                    .apply {
                        action.packageNames?.let {
                            appendQueryParameter("packages", it.joinToString(","))
                        }
                        appendQueryParameter("includeData", action.includeData.toString())
                        appendQueryParameter("includeApk", action.includeApk.toString())
                    }
                    .build()
            }
            
            is DeepLinkAction.RestoreSnapshot -> {
                Uri.Builder()
                    .scheme(SCHEME)
                    .authority("")
                    .path("restore")
                    .appendQueryParameter("snapshot", action.snapshotId)
                    .apply {
                        action.packageNames?.let {
                            appendQueryParameter("packages", it.joinToString(","))
                        }
                    }
                    .build()
            }
            
            is DeepLinkAction.OpenDashboard -> {
                Uri.parse("$SCHEME://dashboard")
            }
            
            is DeepLinkAction.OpenBackups -> {
                Uri.parse("$SCHEME://backups")
            }
            
            is DeepLinkAction.OpenSettings -> {
                Uri.parse("$SCHEME://settings")
            }
            
            is DeepLinkAction.OpenSettingsScreen -> {
                val screenPath = action.screen.name.lowercase()
                Uri.parse("$SCHEME://settings/$screenPath")
            }
            
            is DeepLinkAction.OpenAutomation -> {
                Uri.parse("$SCHEME://automation")
            }
            
            is DeepLinkAction.OpenLogs -> {
                Uri.parse("$SCHEME://logs")
            }
            
            is DeepLinkAction.ConnectCloudProvider -> {
                Uri.Builder()
                    .scheme(SCHEME)
                    .authority("")
                    .path("cloud/connect")
                    .appendQueryParameter("provider", action.provider.name.lowercase())
                    .appendQueryParameter("autoConnect", action.autoConnect.toString())
                    .build()
            }
            
            is DeepLinkAction.OpenCloudSettings -> {
                Uri.parse("$SCHEME://cloud/settings")
            }
            
            is DeepLinkAction.OpenAppDetails -> {
                Uri.Builder()
                    .scheme(SCHEME)
                    .authority("")
                    .path("app")
                    .appendQueryParameter("package", action.packageName)
                    .build()
            }
            
            is DeepLinkAction.Invalid -> null
        }
    }
}

/**
 * Result of routing operation
 */
data class RouteResult(
    val success: Boolean,
    val errorMessage: String? = null
)
