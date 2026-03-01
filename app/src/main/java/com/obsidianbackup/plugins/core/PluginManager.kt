// plugins/core/PluginManager.kt
package com.obsidianbackup.plugins.core

import android.content.Context
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.plugins.discovery.ManifestPluginDiscovery
import com.obsidianbackup.plugins.discovery.PackagePluginDiscovery
import com.obsidianbackup.plugins.discovery.PluginValidator
import com.obsidianbackup.plugins.interfaces.Plugin as PluginInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluginManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pluginRegistry: PluginRegistry,
    private val pluginLoader: PluginLoader,
    private val pluginValidator: PluginValidator,
    private val manifestDiscovery: ManifestPluginDiscovery,
    private val packageDiscovery: PackagePluginDiscovery,
    private val logger: ObsidianLogger
) {

    private val loadedPlugins = mutableMapOf<String, PluginInterface>()
    private val enabledPluginIds = mutableSetOf<String>()

    suspend fun loadPlugins() {
        val discoveries = listOf(
            manifestDiscovery,
            packageDiscovery
        )

        for (discovery in discoveries) {
            val pluginMetadataList = discovery.discoverPlugins()
            for (pluginMetadata in pluginMetadataList) {
                val validationResult = pluginValidator.validate(pluginMetadata)
                if (validationResult.valid) {
                    try {
                        val plugin = pluginLoader.loadPlugin(pluginMetadata)
                        if (plugin is PluginInterface) {
                            loadedPlugins[plugin.id] = plugin
                            pluginRegistry.registerPlugins(listOf(pluginMetadata))
                            plugin.onLoad()
                            logger.i(TAG, "Loaded plugin: ${plugin.id}")
                        }
                    } catch (e: Exception) {
                        logger.e(TAG, "Failed to load plugin: ${pluginMetadata.packageName}", e)
                    }
                } else {
                    logger.w(TAG, "Plugin validation failed: ${pluginMetadata.packageName}")
                }
            }
        }
    }

    fun getPlugin(id: String): PluginInterface? {
        return loadedPlugins[id]
    }

    fun getAllPlugins(): List<PluginInterface> {
        return loadedPlugins.values.toList()
    }

    suspend fun unloadPlugin(id: String) {
        loadedPlugins[id]?.let { plugin ->
            plugin.onUnload()
            loadedPlugins.remove(id)
            pluginRegistry.unregisterPlugin(id)
            logger.i(TAG, "Unloaded plugin: $id")
        }
    }

    // UI-friendly methods
    fun getInstalledPlugins(): List<Plugin> {
        return loadedPlugins.values.map { plugin ->
            Plugin.fromInterface(plugin, enabled = enabledPluginIds.contains(plugin.id))
        }
    }

    fun getEnabledPlugins(): List<Plugin> {
        return loadedPlugins.values
            .filter { enabledPluginIds.contains(it.id) }
            .map { plugin -> Plugin.fromInterface(plugin, enabled = true) }
    }

    fun enablePlugin(id: String) {
        if (loadedPlugins.containsKey(id)) {
            enabledPluginIds.add(id)
            logger.i(TAG, "Enabled plugin: $id")
        }
    }

    fun disablePlugin(id: String) {
        enabledPluginIds.remove(id)
        logger.i(TAG, "Disabled plugin: $id")
    }

    suspend fun discoverPlugins() {
        loadPlugins()
    }

    companion object {
        private const val TAG = "PluginManager"
    }
}
