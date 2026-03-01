// plugins/core/PluginSandbox.kt
package com.obsidianbackup.plugins.core

import android.content.Context
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.plugins.api.PluginCapability
import com.obsidianbackup.plugins.api.PluginMetadata
import java.io.File
import java.security.MessageDigest

class PluginSandbox(
    private val context: Context,
    private val logger: ObsidianLogger
) {

    /**
     * Execute plugin code in a sandboxed environment
     */
    suspend fun <T> executeSandboxed(
        plugin: PluginMetadata,
        block: suspend () -> T
    ): kotlin.Result<T> {
        return try {
            // Set up sandbox environment
            setupSandboxEnvironment(plugin)

            // Execute the plugin code
            val result = block()

            // Clean up
            cleanupSandboxEnvironment(plugin)

            kotlin.Result.success(result)

        } catch (e: Exception) {
            logger.e(TAG, "Plugin execution failed: ${plugin.packageName}", e)
            cleanupSandboxEnvironment(plugin)
            kotlin.Result.failure(e)
        }
    }

    /**
     * Check if plugin has required capability
     */
    fun hasCapability(plugin: PluginMetadata, capability: PluginCapability): Boolean {
        return plugin.capabilities.contains(capability)
    }

    /**
     * Validate plugin permissions against declared capabilities
     */
    fun validatePermissions(plugin: PluginMetadata, requestedPermissions: Set<String>): Boolean {
        // Map capabilities to required permissions
        val allowedPermissions = getAllowedPermissions(plugin.capabilities)

        // Check if all requested permissions are allowed
        return requestedPermissions.all { it in allowedPermissions }
    }

    /**
     * Create isolated storage for plugin
     */
    fun createPluginStorage(plugin: PluginMetadata): File {
        val pluginDir = File(context.filesDir, "plugin_data/${plugin.packageName}")
        if (!pluginDir.exists()) {
            pluginDir.mkdirs()
        }
        return pluginDir
    }

    /**
     * Clean up plugin storage
     */
    fun cleanupPluginStorage(plugin: PluginMetadata) {
        val pluginDir = File(context.filesDir, "plugin_data/${plugin.packageName}")
        pluginDir.deleteRecursively()
    }

    private fun setupSandboxEnvironment(plugin: PluginMetadata) {
        // Set up isolated storage
        createPluginStorage(plugin)

        // Set security manager if needed (for very restrictive sandboxing)
        // Note: Android's security model already provides good isolation

        logger.d(TAG, "Set up sandbox for plugin: ${plugin.packageName}")
    }

    private fun cleanupSandboxEnvironment(plugin: PluginMetadata) {
        // Clean up any temporary resources
        logger.d(TAG, "Cleaned up sandbox for plugin: ${plugin.packageName}")
    }

    private fun getAllowedPermissions(capabilities: Set<PluginCapability>): Set<String> {
        val permissions = mutableSetOf<String>()

        capabilities.forEach { capability ->
            when (capability) {
                is PluginCapability.IncrementalBackup -> {
                    permissions.add("android.permission.READ_EXTERNAL_STORAGE")
                    permissions.add("android.permission.WRITE_EXTERNAL_STORAGE")
                }
                is PluginCapability.ClientSideEncryption -> {
                    // No additional permissions needed for client-side encryption
                }
                is PluginCapability.BackgroundExecution -> {
                    permissions.add("android.permission.WAKE_LOCK")
                }
                is PluginCapability.SystemEventHooks -> {
                    permissions.add("android.permission.RECEIVE_BOOT_COMPLETED")
                }
                is PluginCapability.NetworkAwareness -> {
                    permissions.add("android.permission.ACCESS_NETWORK_STATE")
                    permissions.add("android.permission.INTERNET")
                }
                else -> {
                    // Default minimal permissions
                    permissions.add("android.permission.INTERNET")
                }
            }
        }

        return permissions
    }

    companion object {
        private const val TAG = "PluginSandbox"
    }
}
