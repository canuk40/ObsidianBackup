// cloud/rclone/backends/RcloneS3Provider.kt
package com.obsidianbackup.cloud.rclone.backends

import android.content.Context
import com.obsidianbackup.cloud.rclone.RcloneBackend
import com.obsidianbackup.cloud.rclone.RcloneCloudProvider
import com.obsidianbackup.plugins.interfaces.CloudCapabilities
import com.obsidianbackup.plugins.interfaces.CloudConfig

/**
 * Amazon S3 (and compatible) backend using rclone
 * 
 * Supports:
 * - AWS S3
 * - Wasabi
 * - Backblaze B2 (S3 compatible API)
 * - MinIO
 * - DigitalOcean Spaces
 * - Any S3-compatible storage
 */
class RcloneS3Provider(
    context: Context,
    remoteName: String = "s3"
) : RcloneCloudProvider(context, remoteName, RcloneBackend.S3) {
    
    override val providerId: String = "rclone-s3"
    override val displayName: String = "S3 Compatible (rclone)"
    
    override val capabilities = CloudCapabilities(
        supportsEncryption = true,
        supportsCompression = true,
        maxFileSize = 5_000_000_000_000L, // 5TB
        supportedRegions = listOf(
            "us-east-1", "us-west-1", "us-west-2",
            "eu-west-1", "eu-central-1",
            "ap-southeast-1", "ap-northeast-1"
        ),
        bandwidthThrottling = true
    )
    
    override fun getCredentialsMap(config: CloudConfig): Map<String, String> {
        val credentials = mutableMapOf<String, String>()
        
        // S3 provider type
        credentials["provider"] = config.credentials["provider"] ?: "AWS"
        
        // Access credentials
        config.credentials["access_key_id"]?.let { accessKey ->
            credentials["access_key_id"] = accessKey
        }
        
        config.credentials["secret_access_key"]?.let { secretKey ->
            credentials["secret_access_key"] = secretKey
        }
        
        // Session token (for temporary credentials)
        config.credentials["session_token"]?.let { sessionToken ->
            credentials["session_token"] = sessionToken
        }
        
        // Region
        config.region?.let { region ->
            credentials["region"] = region
        }
        
        // Custom endpoint (for S3-compatible services)
        config.endpoint?.let { endpoint ->
            credentials["endpoint"] = endpoint
        }
        
        return credentials
    }
    
    override fun getAdditionalOptions(config: CloudConfig): Map<String, String> {
        val options = mutableMapOf<String, String>()
        
        // Bucket name (required for S3)
        config.bucket?.let { bucket ->
            options["bucket"] = bucket
        }
        
        // ACL (Access Control List)
        options["acl"] = config.credentials["acl"] ?: "private"
        
        // Storage class
        options["storage_class"] = config.credentials["storage_class"] ?: "STANDARD"
        
        // Server-side encryption
        config.credentials["server_side_encryption"]?.let { sse ->
            options["server_side_encryption"] = sse
        }
        
        // Upload chunk size
        options["chunk_size"] = config.credentials["chunk_size"] ?: "5M"
        
        // Path style (for some S3-compatible services)
        if (config.credentials["force_path_style"] == "true") {
            options["force_path_style"] = "true"
        }
        
        // Disable checksum for compatibility
        if (config.credentials["disable_checksum"] == "true") {
            options["disable_checksum"] = "true"
        }
        
        return options
    }
}
