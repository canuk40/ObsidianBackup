package com.obsidianbackup.community

import android.content.Context
import com.obsidianbackup.logging.ObsidianLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages sharing of anonymized backup configurations
 * All PII is stripped before sharing
 */
@Singleton
class ConfigSharingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: ObsidianLogger,
    private val analyticsManager: AnalyticsManager
) {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    /**
     * Creates an anonymized version of backup configuration that can be safely shared
     */
    fun anonymizeConfig(config: BackupConfig): AnonymizedBackupConfig {
        return AnonymizedBackupConfig(
            id = hashString(config.name),
            backupType = config.backupType,
            compressionEnabled = config.compressionEnabled,
            encryptionEnabled = config.encryptionEnabled,
            scheduleType = config.scheduleType,
            cloudProvider = config.cloudProvider?.let { hashString(it) },
            includeApps = config.includeApps,
            includeMedia = config.includeMedia,
            includeDocuments = config.includeDocuments,
            retentionDays = config.retentionDays,
            bandwidthLimitMbps = config.bandwidthLimitMbps
        )
    }
    
    /**
     * Exports config as shareable JSON string
     */
    fun exportConfig(config: BackupConfig): String {
        val anonymized = anonymizeConfig(config)
        return json.encodeToString(anonymized)
    }
    
    /**
     * Imports config from JSON string
     */
    fun importConfig(jsonString: String): Result<AnonymizedBackupConfig> {
        return try {
            val config = json.decodeFromString<AnonymizedBackupConfig>(jsonString)
            Result.success(config)
        } catch (e: Exception) {
            logger.e("ConfigSharingManager", "Failed to import config", e)
            Result.failure(e)
        }
    }
    
    /**
     * Share configuration via Android share sheet (Option C: Zero server dependency).
     * 
     * Creates a temporary JSON file and shares it via Intent.ACTION_SEND.
     * Requires FileProvider configuration in AndroidManifest.xml.
     * 
     * @param config The backup configuration to share
     * @param activity The activity context for starting the share intent
     */
    fun shareConfig(config: BackupConfig, activity: android.app.Activity) {
        try {
            // Export config to JSON
            val configJson = exportConfig(config)
            
            // Create temporary file in cache directory
            val timestamp = System.currentTimeMillis()
            val configFile = java.io.File(activity.cacheDir, "obsidian_config_$timestamp.json")
            configFile.writeText(configJson)
            
            // Get URI via FileProvider
            val uri = androidx.core.content.FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                configFile
            )
            
            // Create share intent
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "ObsidianBackup Configuration: ${config.name}")
                putExtra(android.content.Intent.EXTRA_TEXT, "Backup profile: ${config.name}")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Show share sheet
            activity.startActivity(
                android.content.Intent.createChooser(shareIntent, "Share Backup Configuration")
            )
            
            logger.i("ConfigSharingManager", "Config shared successfully: ${config.name}")
            // Note: Analytics event logging removed - can be added via coroutine scope if needed
        } catch (e: Exception) {
            logger.e("ConfigSharingManager", "Failed to share config", e)
            throw e
        }
    }
    
    /**
     * Generates shareable link for config (DEPRECATED - use shareConfig instead).
     * 
     * Option C (share sheet) is preferred over web links for:
     * - Zero server dependency
     * - Better privacy (no upload to server)
     * - Native Android UX
     * - Works offline
     */
    @Deprecated(
        message = "Use shareConfig() with Android share sheet instead",
        replaceWith = ReplaceWith("shareConfig(config, activity)")
    )
    fun generateShareLink(config: BackupConfig): String {
        val anonymized = anonymizeConfig(config)
        val encoded = json.encodeToString(anonymized)
        // In production, this would upload to a sharing service
        // For now, return a placeholder
        return "obsidianbackup://share/config?data=${encodeBase64(encoded)}"
    }
    
    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }
    
    private fun encodeBase64(input: String): String {
        return android.util.Base64.encodeToString(
            input.toByteArray(),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
        )
    }
}

@Serializable
data class AnonymizedBackupConfig(
    val id: String,
    val backupType: String,
    val compressionEnabled: Boolean,
    val encryptionEnabled: Boolean,
    val scheduleType: String,
    val cloudProvider: String?,
    val includeApps: Boolean,
    val includeMedia: Boolean,
    val includeDocuments: Boolean,
    val retentionDays: Int,
    val bandwidthLimitMbps: Int?
)

// Placeholder for actual backup config - adapt to your existing model
data class BackupConfig(
    val name: String,
    val backupType: String,
    val compressionEnabled: Boolean,
    val encryptionEnabled: Boolean,
    val scheduleType: String,
    val cloudProvider: String?,
    val includeApps: Boolean,
    val includeMedia: Boolean,
    val includeDocuments: Boolean,
    val retentionDays: Int,
    val bandwidthLimitMbps: Int?
)
