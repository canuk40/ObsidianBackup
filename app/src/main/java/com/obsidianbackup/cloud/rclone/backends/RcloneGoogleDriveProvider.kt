// cloud/rclone/backends/RcloneGoogleDriveProvider.kt
package com.obsidianbackup.cloud.rclone.backends

import android.content.Context
import com.obsidianbackup.cloud.rclone.RcloneBackend
import com.obsidianbackup.cloud.rclone.RcloneCloudProvider
import com.obsidianbackup.plugins.interfaces.CloudCapabilities
import com.obsidianbackup.plugins.interfaces.CloudConfig

/**
 * Google Drive backend using rclone
 * 
 * Supports:
 * - OAuth2 authentication
 * - Service account authentication
 * - Team drives
 * - File metadata
 */
class RcloneGoogleDriveProvider(
    context: Context,
    remoteName: String = "gdrive"
) : RcloneCloudProvider(context, remoteName, RcloneBackend.GoogleDrive) {
    
    override val providerId: String = "rclone-gdrive"
    override val displayName: String = "Google Drive (rclone)"
    
    override val capabilities = CloudCapabilities(
        supportsEncryption = true,
        supportsCompression = true,
        maxFileSize = 5_000_000_000_000L, // 5TB
        supportedRegions = listOf("global"),
        bandwidthThrottling = true
    )
    
    override fun getCredentialsMap(config: CloudConfig): Map<String, String> {
        val credentials = mutableMapOf<String, String>()
        
        // OAuth2 token (JSON format)
        config.credentials["token"]?.let { token ->
            credentials["token"] = token
        }
        
        // Client ID and Secret (optional, but recommended)
        config.credentials["client_id"]?.let { clientId ->
            credentials["client_id"] = clientId
        }
        
        config.credentials["client_secret"]?.let { clientSecret ->
            credentials["client_secret"] = clientSecret
        }
        
        // Service account file (alternative to OAuth2)
        config.credentials["service_account_file"]?.let { saFile ->
            credentials["service_account_file"] = saFile
        }
        
        // Service account credentials (JSON string)
        config.credentials["service_account_credentials"]?.let { saCreds ->
            credentials["service_account_credentials"] = saCreds
        }
        
        return credentials
    }
    
    override fun getAdditionalOptions(config: CloudConfig): Map<String, String> {
        val options = mutableMapOf<String, String>()
        
        // Scope (default: drive)
        options["scope"] = config.credentials["scope"] ?: "drive"
        
        // Root folder ID (optional)
        config.credentials["root_folder_id"]?.let { rootId ->
            options["root_folder_id"] = rootId
        }
        
        // Team drive (optional)
        config.credentials["team_drive"]?.let { teamDrive ->
            options["team_drive"] = teamDrive
        }
        
        // Advanced options
        options["acknowledge_abuse"] = "false"
        options["keep_revision_forever"] = "false"
        options["use_trash"] = "true"
        
        return options
    }
}
