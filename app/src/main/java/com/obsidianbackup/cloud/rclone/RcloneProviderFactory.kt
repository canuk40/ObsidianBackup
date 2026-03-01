// cloud/rclone/RcloneProviderFactory.kt
package com.obsidianbackup.cloud.rclone

import android.content.Context
import com.obsidianbackup.cloud.rclone.backends.RcloneDropboxProvider
import com.obsidianbackup.cloud.rclone.backends.RcloneGoogleDriveProvider
import com.obsidianbackup.cloud.rclone.backends.RcloneS3Provider
import com.obsidianbackup.plugins.interfaces.CloudCapabilities
import com.obsidianbackup.plugins.interfaces.CloudConfig

/**
 * Factory for creating rclone-based cloud providers
 */
object RcloneProviderFactory {
    
    /**
     * Supported provider types
     */
    enum class ProviderType {
        GOOGLE_DRIVE,
        DROPBOX,
        S3,
        ONEDRIVE,
        BACKBLAZE_B2,
        WEBDAV,
        SFTP;
        
        companion object {
            fun fromString(type: String): ProviderType? {
                return when (type.lowercase()) {
                    "gdrive", "google_drive", "googledrive", "drive" -> GOOGLE_DRIVE
                    "dropbox" -> DROPBOX
                    "s3", "aws", "wasabi", "minio" -> S3
                    "onedrive", "onedrive_business" -> ONEDRIVE
                    "b2", "backblaze" -> BACKBLAZE_B2
                    "webdav" -> WEBDAV
                    "sftp" -> SFTP
                    else -> null
                }
            }
        }
    }
    
    /**
     * Create a cloud provider instance
     */
    fun create(
        context: Context,
        providerType: ProviderType,
        remoteName: String? = null
    ): RcloneCloudProvider {
        return when (providerType) {
            ProviderType.GOOGLE_DRIVE -> RcloneGoogleDriveProvider(
                context,
                remoteName ?: "gdrive"
            )
            
            ProviderType.DROPBOX -> RcloneDropboxProvider(
                context,
                remoteName ?: "dropbox"
            )
            
            ProviderType.S3 -> RcloneS3Provider(
                context,
                remoteName ?: "s3"
            )
            
            ProviderType.ONEDRIVE -> createGenericProvider(
                context, 
                RcloneBackend.OneDrive, 
                remoteName ?: "onedrive",
                "OneDrive",
                "rclone-onedrive"
            )
            
            ProviderType.BACKBLAZE_B2 -> createGenericProvider(
                context,
                RcloneBackend.BackblazeB2,
                remoteName ?: "b2",
                "Backblaze B2",
                "rclone-b2"
            )
            
            ProviderType.WEBDAV -> createGenericProvider(
                context,
                RcloneBackend.WebDAV,
                remoteName ?: "webdav",
                "WebDAV",
                "rclone-webdav"
            )
            
            ProviderType.SFTP -> createGenericProvider(
                context,
                RcloneBackend.SFTP,
                remoteName ?: "sftp",
                "SFTP",
                "rclone-sftp"
            )
        }
    }
    
    /**
     * Create from config
     */
    fun createFromConfig(
        context: Context,
        config: CloudConfig
    ): RcloneCloudProvider {
        val providerType = config.credentials["provider_type"]
            ?: throw IllegalArgumentException("provider_type not specified in config")
        
        val type = ProviderType.fromString(providerType)
            ?: throw IllegalArgumentException("Unknown provider type: $providerType")
        
        val remoteName = config.credentials["remote_name"]
        
        return create(context, type, remoteName)
    }
    
    /**
     * Get list of all supported providers
     */
    fun getSupportedProviders(): List<ProviderInfo> {
        return listOf(
            ProviderInfo(
                type = ProviderType.GOOGLE_DRIVE,
                displayName = "Google Drive",
                description = "Google Drive cloud storage with 15GB free",
                icon = "ic_google_drive",
                requiresOAuth = true,
                maxFileSize = 5_000_000_000_000L // 5TB
            ),
            ProviderInfo(
                type = ProviderType.DROPBOX,
                displayName = "Dropbox",
                description = "Dropbox cloud storage with 2GB free",
                icon = "ic_dropbox",
                requiresOAuth = true,
                maxFileSize = 350_000_000_000L // 350GB
            ),
            ProviderInfo(
                type = ProviderType.S3,
                displayName = "S3 Compatible",
                description = "AWS S3 and compatible storage (Wasabi, Backblaze, MinIO)",
                icon = "ic_s3",
                requiresOAuth = false,
                maxFileSize = 5_000_000_000_000L // 5TB
            )
        )
    }
    
    /**
     * Create a generic rclone provider for backends without specific implementations
     */
    private fun createGenericProvider(
        context: Context,
        backend: RcloneBackend,
        remoteName: String,
        displayName: String,
        providerId: String
    ): RcloneCloudProvider {
        return object : RcloneCloudProvider(context, remoteName, backend) {
            override val providerId: String = providerId
            override val displayName: String = displayName
            
            override val capabilities = CloudCapabilities(
                supportsEncryption = true,
                supportsCompression = true,
                maxFileSize = 5_000_000_000_000L, // 5TB default
                supportedRegions = listOf("global"),
                bandwidthThrottling = true
            )
            
            override fun getCredentialsMap(config: CloudConfig): Map<String, String> {
                // Pass through all credentials from config
                return config.credentials.toMap()
            }
            
            override fun getAdditionalOptions(config: CloudConfig): Map<String, String> {
                return emptyMap()
            }
        }
    }
    
    /**
     * Validate configuration for a provider
     */
    fun validateConfig(providerType: ProviderType, config: CloudConfig): ValidationResult {
        val errors = mutableListOf<String>()
        
        when (providerType) {
            ProviderType.GOOGLE_DRIVE -> {
                if (!config.credentials.containsKey("token") && 
                    !config.credentials.containsKey("service_account_file")) {
                    errors.add("OAuth2 token or service account required")
                }
            }
            
            ProviderType.DROPBOX -> {
                if (!config.credentials.containsKey("token")) {
                    errors.add("OAuth2 token required")
                }
            }
            
            ProviderType.S3 -> {
                if (!config.credentials.containsKey("access_key_id")) {
                    errors.add("access_key_id required")
                }
                if (!config.credentials.containsKey("secret_access_key")) {
                    errors.add("secret_access_key required")
                }
            }
            
            ProviderType.ONEDRIVE -> {
                if (!config.credentials.containsKey("token") && 
                    !config.credentials.containsKey("client_id")) {
                    errors.add("OAuth2 token or client credentials required")
                }
            }
            
            ProviderType.BACKBLAZE_B2 -> {
                if (!config.credentials.containsKey("account")) {
                    errors.add("account ID required")
                }
                if (!config.credentials.containsKey("key")) {
                    errors.add("application key required")
                }
            }
            
            ProviderType.WEBDAV -> {
                if (!config.credentials.containsKey("url")) {
                    errors.add("WebDAV URL required")
                }
                if (!config.credentials.containsKey("user")) {
                    errors.add("username required")
                }
                if (!config.credentials.containsKey("pass")) {
                    errors.add("password required")
                }
            }
            
            ProviderType.SFTP -> {
                if (!config.credentials.containsKey("host")) {
                    errors.add("host required")
                }
                if (!config.credentials.containsKey("user")) {
                    errors.add("username required")
                }
                if (!config.credentials.containsKey("pass") && 
                    !config.credentials.containsKey("key_file")) {
                    errors.add("password or SSH key file required")
                }
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
}

/**
 * Provider information for UI display
 */
data class ProviderInfo(
    val type: RcloneProviderFactory.ProviderType,
    val displayName: String,
    val description: String,
    val icon: String,
    val requiresOAuth: Boolean,
    val maxFileSize: Long
)

/**
 * Configuration validation result
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult()
}
