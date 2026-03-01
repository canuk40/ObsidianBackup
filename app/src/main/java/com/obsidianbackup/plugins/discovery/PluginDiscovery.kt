// plugins/discovery/PluginDiscovery.kt
package com.obsidianbackup.plugins.discovery

import com.obsidianbackup.plugins.api.PluginMetadata

/**
 * Interface for plugin discovery mechanisms
 */
interface PluginDiscovery {
    /**
     * Discover available plugins
     * @return List of discovered plugin metadata
     */
    suspend fun discoverPlugins(): List<PluginMetadata>
}
