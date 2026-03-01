// cloud/rclone/backends/RcloneDropboxProvider.kt
package com.obsidianbackup.cloud.rclone.backends

import android.content.Context
import com.obsidianbackup.cloud.rclone.RcloneBackend
import com.obsidianbackup.cloud.rclone.RcloneCloudProvider
import com.obsidianbackup.plugins.interfaces.CloudCapabilities
import com.obsidianbackup.plugins.interfaces.CloudConfig

/**
 * Dropbox backend using rclone
 * 
 * Supports:
 * - OAuth2 authentication
 * - App folder mode
 * - Shared folders
 */
class RcloneDropboxProvider(
    context: Context,
    remoteName: String = "dropbox"
) : RcloneCloudProvider(context, remoteName, RcloneBackend.Dropbox) {
    
    override val providerId: String = "rclone-dropbox"
    override val displayName: String = "Dropbox (rclone)"
    
    override val capabilities = CloudCapabilities(
        supportsEncryption = true,
        supportsCompression = true,
        maxFileSize = 350_000_000_000L, // 350GB
        supportedRegions = listOf("global"),
        bandwidthThrottling = true
    )
    
    override fun getCredentialsMap(config: CloudConfig): Map<String, String> {
        val credentials = mutableMapOf<String, String>()
        
        // OAuth2 token (JSON format)
        config.credentials["token"]?.let { token ->
            credentials["token"] = token
        }
        
        // Client ID and Secret (optional)
        config.credentials["client_id"]?.let { clientId ->
            credentials["client_id"] = clientId
        }
        
        config.credentials["client_secret"]?.let { clientSecret ->
            credentials["client_secret"] = clientSecret
        }
        
        return credentials
    }
    
    override fun getAdditionalOptions(config: CloudConfig): Map<String, String> {
        val options = mutableMapOf<String, String>()
        
        // Chunk size for uploads
        options["chunk_size"] = "48M"
        
        // Impersonate user (for business accounts)
        config.credentials["impersonate"]?.let { impersonate ->
            options["impersonate"] = impersonate
        }
        
        // Shared folder mode
        config.credentials["shared_folder"]?.let { sharedFolder ->
            options["shared_folder"] = sharedFolder
        }
        
        return options
    }
}
