// cloud/rclone/RcloneConfigManager.kt
package com.obsidianbackup.cloud.rclone

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Manages rclone configuration file creation and storage
 * Handles secure storage of credentials and OAuth tokens
 */
class RcloneConfigManager(
    private val context: Context,
    private val configFile: File
) {
    
    companion object {
        private const val CONFIG_SECTION_PATTERN = "\\[([^\\]]+)\\]"
    }

    private val masterKeyAlias: String by lazy {
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    }

    /**
     * Write rclone config (contains OAuth tokens, access keys, secrets) encrypted.
     * Previously: writeConfig(configFile, newConfig) — plaintext!
     * SOURCE: https://developer.android.com/reference/androidx/security/crypto/EncryptedFile
     */
    fun writeConfig(file: File, newConfig: String) {
        file.parentFile?.mkdirs()
        if (file.exists()) file.delete()  // EncryptedFile requires non-existent file

        val encryptedFile = EncryptedFile.Builder(
            file,
            context,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileOutput().use { outputStream ->
            outputStream.write(newConfig.toByteArray(Charsets.UTF_8))
        }
    }

    /**
     * Read rclone config (encrypted).
     */
    fun readConfig(file: File): String {
        if (!file.exists()) return ""

        val encryptedFile = EncryptedFile.Builder(
            file,
            context,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        return encryptedFile.openFileInput().use { inputStream ->
            inputStream.readBytes().toString(Charsets.UTF_8)
        }
    }
    
    /**
     * Create or update a remote configuration
     */
    suspend fun createOrUpdateRemote(
        remoteName: String,
        backend: RcloneBackend,
        credentials: Map<String, String>,
        additionalOptions: Map<String, String> = emptyMap()
    ): RcloneResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val config = buildConfigSection(remoteName, backend, credentials, additionalOptions)
            
            // Read existing config
            val existingConfig = if (configFile.exists()) {
                readConfig(configFile)
            } else {
                ""
            }
            
            // Remove existing section for this remote if it exists
            val updatedConfig = removeRemoteSection(existingConfig, remoteName)
            
            // Append new configuration
            val newConfig = if (updatedConfig.isNotEmpty() && !updatedConfig.endsWith("\n\n")) {
                "$updatedConfig\n\n$config"
            } else {
                "$updatedConfig$config"
            }
            
            // Write to file
            configFile.parentFile?.mkdirs()
            writeConfig(configFile, newConfig)
            
            // Set proper permissions (read/write for owner only)
            configFile.setReadable(true, true)
            configFile.setWritable(true, true)
            configFile.setExecutable(false)
            
            RcloneResult.Success(Unit)
            
        } catch (e: Exception) {
            RcloneResult.Error(
                RcloneError(
                    code = RcloneErrorCode.CONFIG_ERROR,
                    message = "Failed to create/update remote: ${e.message}",
                    cause = e,
                    retryable = false
                )
            )
        }
    }
    
    /**
     * Build configuration section for a remote
     */
    private fun buildConfigSection(
        remoteName: String,
        backend: RcloneBackend,
        credentials: Map<String, String>,
        additionalOptions: Map<String, String>
    ): String {
        val lines = mutableListOf<String>()
        lines.add("[$remoteName]")
        lines.add("type = ${backend.type}")
        
        // Add credentials
        credentials.forEach { (key, value) ->
            lines.add("$key = $value")
        }
        
        // Add additional options
        additionalOptions.forEach { (key, value) ->
            lines.add("$key = $value")
        }
        
        return lines.joinToString("\n") + "\n"
    }
    
    /**
     * Remove a remote section from config text
     */
    private fun removeRemoteSection(configText: String, remoteName: String): String {
        val lines = configText.lines()
        val result = mutableListOf<String>()
        var inTargetSection = false
        
        for (line in lines) {
            if (line.trim().startsWith("[")) {
                val sectionName = line.trim().removeSurrounding("[", "]")
                inTargetSection = sectionName == remoteName
                if (!inTargetSection) {
                    result.add(line)
                }
            } else if (!inTargetSection) {
                result.add(line)
            }
        }
        
        return result.joinToString("\n")
    }
    
    /**
     * Delete a remote from configuration
     */
    suspend fun deleteRemote(remoteName: String): RcloneResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!configFile.exists()) {
                return@withContext RcloneResult.Success(Unit)
            }
            
            val existingConfig = readConfig(configFile)
            val updatedConfig = removeRemoteSection(existingConfig, remoteName)
            
            writeConfig(configFile, updatedConfig)
            
            RcloneResult.Success(Unit)
            
        } catch (e: Exception) {
            RcloneResult.Error(
                RcloneError(
                    code = RcloneErrorCode.CONFIG_ERROR,
                    message = "Failed to delete remote: ${e.message}",
                    cause = e,
                    retryable = false
                )
            )
        }
    }
    
    /**
     * List all configured remotes
     */
    suspend fun listRemotes(): RcloneResult<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (!configFile.exists()) {
                return@withContext RcloneResult.Success(emptyList())
            }
            
            val configText = readConfig(configFile)
            val remotes = mutableListOf<String>()
            
            val sectionPattern = Regex(CONFIG_SECTION_PATTERN)
            sectionPattern.findAll(configText).forEach { match ->
                remotes.add(match.groupValues[1])
            }
            
            RcloneResult.Success(remotes)
            
        } catch (e: Exception) {
            RcloneResult.Error(
                RcloneError(
                    code = RcloneErrorCode.CONFIG_ERROR,
                    message = "Failed to list remotes: ${e.message}",
                    cause = e,
                    retryable = false
                )
            )
        }
    }
    
    /**
     * Check if a remote exists
     */
    suspend fun hasRemote(remoteName: String): Boolean {
        return when (val result = listRemotes()) {
            is RcloneResult.Success -> result.data.contains(remoteName)
            is RcloneResult.Error -> false
        }
    }
    
    /**
     * Get configuration for a specific remote
     */
    suspend fun getRemoteConfig(remoteName: String): RcloneResult<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            if (!configFile.exists()) {
                return@withContext RcloneResult.Error(
                    RcloneError(
                        code = RcloneErrorCode.CONFIG_ERROR,
                        message = "Config file does not exist",
                        retryable = false
                    )
                )
            }
            
            val configText = readConfig(configFile)
            val lines = configText.lines()
            val config = mutableMapOf<String, String>()
            var inTargetSection = false
            
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("[")) {
                    val sectionName = trimmed.removeSurrounding("[", "]")
                    inTargetSection = sectionName == remoteName
                } else if (inTargetSection && trimmed.contains("=")) {
                    val parts = trimmed.split("=", limit = 2)
                    if (parts.size == 2) {
                        config[parts[0].trim()] = parts[1].trim()
                    }
                }
            }
            
            if (config.isEmpty()) {
                return@withContext RcloneResult.Error(
                    RcloneError(
                        code = RcloneErrorCode.CONFIG_ERROR,
                        message = "Remote '$remoteName' not found",
                        retryable = false
                    )
                )
            }
            
            RcloneResult.Success(config)
            
        } catch (e: Exception) {
            RcloneResult.Error(
                RcloneError(
                    code = RcloneErrorCode.CONFIG_ERROR,
                    message = "Failed to get remote config: ${e.message}",
                    cause = e,
                    retryable = false
                )
            )
        }
    }
}

/**
 * Supported rclone backends
 */
sealed class RcloneBackend(val type: String) {
    object GoogleDrive : RcloneBackend("drive")
    object Dropbox : RcloneBackend("dropbox")
    object S3 : RcloneBackend("s3")
    object OneDrive : RcloneBackend("onedrive")
    object BackblazeB2 : RcloneBackend("b2")
    object WebDAV : RcloneBackend("webdav")
    object SFTP : RcloneBackend("sftp")
    object Local : RcloneBackend("local")
    data class Custom(val backendType: String) : RcloneBackend(backendType)
}
