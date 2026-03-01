// api/plugin/PluginCapability.kt
package com.obsidianbackup.api.plugin

import kotlinx.serialization.Serializable

/**
 * Capabilities that plugins can declare
 * Platform enforces these at runtime
 */
sealed class PluginCapability {
    /** Plugin can read backup catalogs */
    object READ_CATALOG : PluginCapability()

    /** Plugin can write to backup catalogs (very restricted) */
    object WRITE_CATALOG : PluginCapability()

    /** Plugin can access network */
    object NETWORK_ACCESS : PluginCapability()

    /** Plugin can execute local file operations */
    object FILE_SYSTEM_ACCESS : PluginCapability()

    /** Plugin can schedule background work */
    object BACKGROUND_EXECUTION : PluginCapability()

    /** Plugin can show notifications */
    object NOTIFICATIONS : PluginCapability()

    /** Plugin needs encryption key access */
    object ENCRYPTION_KEY_ACCESS : PluginCapability()

    /** Custom capability with permission check */
    data class Custom(val name: String, val description: String) : PluginCapability()
}

/**
 * Plugin manifest (declared in plugin package)
 */
@Serializable
data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val apiVersion: String,
    val description: String,
    val pluginClass: String, // Fully qualified class name
    val capabilities: List<String>,
    val minPlatformVersion: String? = null,
    val signature: String? = null, // For signed plugins

    // Optional metadata
    val icon: String? = null,
    val website: String? = null,
    val supportEmail: String? = null,
    val license: String? = null
)
