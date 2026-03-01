// plugins/core/Plugin.kt
package com.obsidianbackup.plugins.core

import com.obsidianbackup.plugins.api.PluginMetadata

/**
 * Data class representing a plugin for UI display purposes.
 * This is distinct from the Plugin interface in plugins/interfaces.
 */
data class Plugin(
    val id: String,
    val name: String,
    val version: String,
    val description: String = "",
    val enabled: Boolean = false,
    val metadata: PluginMetadata? = null
) {
    companion object {
        /**
         * Convert from interface Plugin to data class Plugin for UI
         */
        fun fromInterface(plugin: com.obsidianbackup.plugins.interfaces.Plugin, enabled: Boolean = false): Plugin {
            return Plugin(
                id = plugin.id,
                name = plugin.metadata.name,
                version = plugin.metadata.version,
                description = plugin.metadata.description,
                enabled = enabled,
                metadata = plugin.metadata
            )
        }
    }
}
