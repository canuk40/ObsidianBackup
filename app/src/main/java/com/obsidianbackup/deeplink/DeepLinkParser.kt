package com.obsidianbackup.deeplink

import android.net.Uri

/**
 * Parser for converting URIs to DeepLinkAction objects
 */
class DeepLinkParser {
    
    companion object {
        private const val SCHEME_CUSTOM = "obsidianbackup"
        private const val SCHEME_HTTPS = "https"
        private const val HOST_APP_LINK = "obsidianbackup.app"
        
        // Path patterns
        private const val PATH_BACKUP = "backup"
        private const val PATH_RESTORE = "restore"
        private const val PATH_SETTINGS = "settings"
        private const val PATH_AUTOMATION = "automation"
        private const val PATH_LOGS = "logs"
        private const val PATH_DASHBOARD = "dashboard"
        private const val PATH_CLOUD = "cloud"
        private const val PATH_APP = "app"
        
        // Query parameters
        private const val PARAM_PACKAGES = "packages"
        private const val PARAM_SNAPSHOT = "snapshot"
        private const val PARAM_PROVIDER = "provider"
        private const val PARAM_AUTO_CONNECT = "autoConnect"
        private const val PARAM_INCLUDE_DATA = "includeData"
        private const val PARAM_INCLUDE_APK = "includeApk"
        private const val PARAM_PACKAGE = "package"
    }
    
    /**
     * Parse a URI into a DeepLinkAction
     */
    fun parse(uri: Uri): DeepLinkAction {
        // Validate scheme
        if (!isValidScheme(uri)) {
            return DeepLinkAction.Invalid("Invalid scheme: ${uri.scheme}")
        }
        
        // For HTTPS app links, validate host
        if (uri.scheme == SCHEME_HTTPS && uri.host != HOST_APP_LINK) {
            return DeepLinkAction.Invalid("Invalid host: ${uri.host}")
        }
        
        // Parse path
        val path = uri.path?.removePrefix("/")?.lowercase() ?: ""
        val pathSegments = uri.pathSegments ?: emptyList()
        
        return when {
            path.isEmpty() || path == PATH_DASHBOARD -> {
                DeepLinkAction.OpenDashboard
            }
            
            path.startsWith(PATH_BACKUP) -> {
                parseBackupAction(uri)
            }
            
            path.startsWith(PATH_RESTORE) -> {
                parseRestoreAction(uri)
            }
            
            path.startsWith(PATH_SETTINGS) -> {
                parseSettingsAction(uri, pathSegments)
            }
            
            path == PATH_AUTOMATION -> {
                DeepLinkAction.OpenAutomation
            }
            
            path == PATH_LOGS -> {
                DeepLinkAction.OpenLogs
            }
            
            path.startsWith(PATH_CLOUD) -> {
                parseCloudAction(uri, pathSegments)
            }
            
            path.startsWith(PATH_APP) -> {
                parseAppAction(uri)
            }
            
            else -> {
                DeepLinkAction.Invalid("Unknown path: $path")
            }
        }
    }
    
    private fun isValidScheme(uri: Uri): Boolean {
        return uri.scheme == SCHEME_CUSTOM || uri.scheme == SCHEME_HTTPS
    }
    
    private fun parseBackupAction(uri: Uri): DeepLinkAction {
        val packagesParam = uri.getQueryParameter(PARAM_PACKAGES)
        val packages = packagesParam?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
        
        val includeData = uri.getQueryParameter(PARAM_INCLUDE_DATA)?.toBoolean() ?: true
        val includeApk = uri.getQueryParameter(PARAM_INCLUDE_APK)?.toBoolean() ?: true
        
        return DeepLinkAction.StartBackup(
            packageNames = packages,
            includeData = includeData,
            includeApk = includeApk
        )
    }
    
    private fun parseRestoreAction(uri: Uri): DeepLinkAction {
        val snapshotId = uri.getQueryParameter(PARAM_SNAPSHOT)
        if (snapshotId.isNullOrEmpty()) {
            return DeepLinkAction.Invalid("Missing snapshot ID")
        }
        
        val packagesParam = uri.getQueryParameter(PARAM_PACKAGES)
        val packages = packagesParam?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
        
        return DeepLinkAction.RestoreSnapshot(
            snapshotId = snapshotId,
            packageNames = packages
        )
    }
    
    private fun parseSettingsAction(uri: Uri, pathSegments: List<String>): DeepLinkAction {
        if (pathSegments.size == 1) {
            return DeepLinkAction.OpenSettings
        }
        
        val screen = when (pathSegments.getOrNull(1)?.lowercase()) {
            "automation" -> SettingsScreen.AUTOMATION
            "cloud" -> SettingsScreen.CLOUD
            "security" -> SettingsScreen.SECURITY
            "storage" -> SettingsScreen.STORAGE
            "notifications" -> SettingsScreen.NOTIFICATIONS
            "advanced" -> SettingsScreen.ADVANCED
            "about" -> SettingsScreen.ABOUT
            else -> return DeepLinkAction.Invalid("Unknown settings screen: ${pathSegments[1]}")
        }
        
        return DeepLinkAction.OpenSettingsScreen(screen)
    }
    
    private fun parseCloudAction(uri: Uri, pathSegments: List<String>): DeepLinkAction {
        val subPath = pathSegments.getOrNull(1)?.lowercase()
        
        return when (subPath) {
            "connect" -> {
                val providerParam = uri.getQueryParameter(PARAM_PROVIDER)?.lowercase()
                val provider = when (providerParam) {
                    "webdav" -> CloudProvider.WEBDAV
                    "rclone" -> CloudProvider.RCLONE
                    "googledrive", "google_drive" -> CloudProvider.GOOGLE_DRIVE
                    "dropbox" -> CloudProvider.DROPBOX
                    "onedrive" -> CloudProvider.ONEDRIVE
                    "nextcloud" -> CloudProvider.NEXTCLOUD
                    "owncloud" -> CloudProvider.OWNCLOUD
                    "custom" -> CloudProvider.CUSTOM
                    null -> return DeepLinkAction.Invalid("Missing cloud provider")
                    else -> return DeepLinkAction.Invalid("Unknown cloud provider: $providerParam")
                }
                
                val autoConnect = uri.getQueryParameter(PARAM_AUTO_CONNECT)?.toBoolean() ?: false
                
                DeepLinkAction.ConnectCloudProvider(
                    provider = provider,
                    autoConnect = autoConnect
                )
            }
            
            null, "settings" -> DeepLinkAction.OpenCloudSettings
            
            else -> DeepLinkAction.Invalid("Unknown cloud action: $subPath")
        }
    }
    
    private fun parseAppAction(uri: Uri): DeepLinkAction {
        val packageName = uri.getQueryParameter(PARAM_PACKAGE)
        if (packageName.isNullOrEmpty()) {
            return DeepLinkAction.Invalid("Missing package name")
        }
        
        return DeepLinkAction.OpenAppDetails(packageName)
    }
    
    /**
     * Validate package name format
     */
    private fun isValidPackageName(packageName: String): Boolean {
        return packageName.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$"))
    }
}
