package com.obsidianbackup.deeplink

import android.net.Uri

/**
 * Sealed class representing all possible deep link actions
 */
sealed class DeepLinkAction {
    
    // Backup operations
    data class StartBackup(
        val packageNames: List<String>? = null,
        val includeData: Boolean = true,
        val includeApk: Boolean = true
    ) : DeepLinkAction()
    
    // Restore operations
    data class RestoreSnapshot(
        val snapshotId: String,
        val packageNames: List<String>? = null
    ) : DeepLinkAction()
    
    // Navigation
    object OpenDashboard : DeepLinkAction()
    object OpenBackups : DeepLinkAction()
    object OpenSettings : DeepLinkAction()
    
    data class OpenSettingsScreen(
        val screen: SettingsScreen
    ) : DeepLinkAction()
    
    object OpenAutomation : DeepLinkAction()
    object OpenLogs : DeepLinkAction()
    
    // Cloud operations
    data class ConnectCloudProvider(
        val provider: CloudProvider,
        val autoConnect: Boolean = false
    ) : DeepLinkAction()
    
    object OpenCloudSettings : DeepLinkAction()
    
    // App management
    data class OpenAppDetails(
        val packageName: String
    ) : DeepLinkAction()
    
    // Invalid/Unknown
    data class Invalid(val reason: String) : DeepLinkAction()
}

/**
 * Settings screens that can be deep linked
 */
enum class SettingsScreen {
    MAIN,
    AUTOMATION,
    CLOUD,
    SECURITY,
    STORAGE,
    NOTIFICATIONS,
    ADVANCED,
    ABOUT
}

/**
 * Cloud provider types
 */
enum class CloudProvider {
    WEBDAV,
    RCLONE,
    GOOGLE_DRIVE,
    DROPBOX,
    ONEDRIVE,
    NEXTCLOUD,
    OWNCLOUD,
    CUSTOM
}

/**
 * Result of deep link processing
 */
sealed class DeepLinkResult {
    data class Success(
        val action: DeepLinkAction,
        val metadata: Map<String, String> = emptyMap()
    ) : DeepLinkResult()
    
    data class AuthenticationRequired(
        val action: DeepLinkAction,
        val reason: String
    ) : DeepLinkResult()
    
    data class Error(
        val reason: String,
        val originalUri: Uri? = null
    ) : DeepLinkResult()
}

/**
 * Configuration for deep link authentication requirements
 */
data class DeepLinkAuthConfig(
    val requireBiometric: Boolean = true,
    val allowDeviceCredential: Boolean = true,
    val timeout: Long = 30000, // 30 seconds
    val maxAttempts: Int = 3
)

/**
 * Deep link analytics event
 */
data class DeepLinkEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val source: String? = null,
    val success: Boolean,
    val errorReason: String? = null,
    val metadata: Map<String, String> = emptyMap()
)
