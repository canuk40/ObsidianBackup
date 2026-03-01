// plugins/core/PluginRegistry.kt
package com.obsidianbackup.plugins.core

import com.obsidianbackup.plugins.api.PluginMetadata
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class PluginType {
    BACKUP_ENGINE,
    CLOUD_PROVIDER,
    AUTOMATION,
    EXPORT
}

class PluginRegistry {

    private val mutex = Mutex()
    private val plugins = mutableMapOf<String, PluginMetadata>()
    private val loadedPlugins = mutableSetOf<String>()

    suspend fun registerPlugins(pluginList: List<PluginMetadata>) {
        mutex.withLock {
            pluginList.forEach { plugin ->
                plugins[plugin.packageName] = plugin
            }
        }
    }

    suspend fun getPlugin(packageName: String): PluginMetadata? {
        return mutex.withLock {
            plugins[packageName]
        }
    }

    suspend fun getAllPlugins(): List<PluginMetadata> {
        return mutex.withLock {
            plugins.values.toList()
        }
    }

    suspend fun getPluginsByType(type: PluginType): List<PluginMetadata> {
        return mutex.withLock {
            plugins.values.filter { plugin ->
                when (type) {
                    PluginType.BACKUP_ENGINE -> plugin.capabilities.any { it is com.obsidianbackup.plugins.api.PluginCapability.IncrementalBackup }
                    PluginType.CLOUD_PROVIDER -> plugin.capabilities.any { it is com.obsidianbackup.plugins.api.PluginCapability.MultiRegionSupport }
                    PluginType.AUTOMATION -> plugin.capabilities.any { it is com.obsidianbackup.plugins.api.PluginCapability.BackgroundExecution }
                    PluginType.EXPORT -> plugin.capabilities.any { it is com.obsidianbackup.plugins.api.PluginCapability.StreamingExport }
                }
            }
        }
    }

    suspend fun markPluginLoaded(packageName: String) {
        mutex.withLock {
            loadedPlugins.add(packageName)
        }
    }

    suspend fun markPluginUnloaded(packageName: String) {
        mutex.withLock {
            loadedPlugins.remove(packageName)
        }
    }

    suspend fun isPluginLoaded(packageName: String): Boolean {
        return mutex.withLock {
            loadedPlugins.contains(packageName)
        }
    }

    suspend fun unregisterPlugin(packageName: String) {
        mutex.withLock {
            plugins.remove(packageName)
            loadedPlugins.remove(packageName)
        }
    }

    suspend fun clearRegistry() {
        mutex.withLock {
            plugins.clear()
            loadedPlugins.clear()
        }
    }
}
