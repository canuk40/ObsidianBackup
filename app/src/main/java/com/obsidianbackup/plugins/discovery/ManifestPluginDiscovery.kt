// plugins/discovery/ManifestPluginDiscovery.kt
package com.obsidianbackup.plugins.discovery

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.plugins.api.PluginApiVersion
import com.obsidianbackup.plugins.api.PluginCapability
import com.obsidianbackup.plugins.api.PluginMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ManifestPluginDiscovery @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: ObsidianLogger? = null
) : PluginDiscovery {

    override suspend fun discoverPlugins(): List<PluginMetadata> {
        val packageManager = context.packageManager
        val plugins = mutableListOf<PluginMetadata>()

        try {
            val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
            for (packageInfo in packages) {
                val pluginMetadata = extractPluginMetadata(packageInfo)
                if (pluginMetadata != null) {
                    plugins.add(pluginMetadata)
                }
            }
        } catch (e: Exception) {
            logger?.e(TAG, "Failed to discover manifest plugins", e)
        }

        return plugins
    }

    private fun extractPluginMetadata(packageInfo: PackageInfo): PluginMetadata? {
        val applicationInfo = packageInfo.applicationInfo ?: return null
        val metaData = applicationInfo.metaData ?: return null

        // Check if this is an Obsidian Backup plugin
        if (!metaData.containsKey("obsidianbackup.plugin.id")) {
            return null
        }

        return parsePluginMetadata(packageInfo, metaData)
    }

    fun parsePluginMetadata(packageInfo: PackageInfo, metaData: Bundle): PluginMetadata? {
        try {
            val pluginId = metaData.getString("obsidianbackup.plugin.id") ?: return null
            val pluginName = metaData.getString("obsidianbackup.plugin.name") ?: pluginId
            val pluginVersion = metaData.getString("obsidianbackup.plugin.version") ?: "1.0.0"
            val className = metaData.getString("obsidianbackup.plugin.class") ?: return null
            val description = metaData.getString("obsidianbackup.plugin.description") ?: ""
            val author = metaData.getString("obsidianbackup.plugin.author") ?: "Unknown"
            val website = metaData.getString("obsidianbackup.plugin.website")
            val apiVersion = metaData.getInt("obsidianbackup.plugin.apiVersion", PluginApiVersion.CURRENT.version)
            val minSdkVersion = metaData.getInt("obsidianbackup.plugin.minSdkVersion", 24)
            
            // Parse capabilities
            val capabilitiesString = metaData.getString("obsidianbackup.plugin.capabilities") ?: ""
            val capabilities = parseCapabilities(capabilitiesString)

            return PluginMetadata(
                packageName = packageInfo.packageName,
                className = className,
                name = pluginName,
                description = description,
                version = pluginVersion,
                apiVersion = PluginApiVersion(apiVersion),
                capabilities = capabilities,
                author = author,
                website = website,
                minSdkVersion = minSdkVersion
            )
        } catch (e: Exception) {
            logger?.e(TAG, "Failed to parse plugin metadata from ${packageInfo.packageName}", e)
            return null
        }
    }

    private fun parseCapabilities(capabilitiesString: String): Set<PluginCapability> {
        if (capabilitiesString.isBlank()) return emptySet()
        
        return capabilitiesString.split(",")
            .map { it.trim() }
            .mapNotNull { capability ->
                try {
                    when (capability) {
                        "IncrementalBackup" -> PluginCapability.IncrementalBackup
                        "EncryptionSupport" -> PluginCapability.EncryptionSupport
                        "CompressionSupport" -> PluginCapability.CompressionSupport
                        "MultiRegionSupport" -> PluginCapability.MultiRegionSupport
                        "BandwidthThrottling" -> PluginCapability.BandwidthThrottling
                        "ClientSideEncryption" -> PluginCapability.ClientSideEncryption
                        "BackgroundExecution" -> PluginCapability.BackgroundExecution
                        "ScheduledExecution" -> PluginCapability.ScheduledExecution
                        "SystemEventHooks" -> PluginCapability.SystemEventHooks
                        "NetworkAwareness" -> PluginCapability.NetworkAwareness
                        "StreamingExport" -> PluginCapability.StreamingExport
                        "BatchExport" -> PluginCapability.BatchExport
                        "CustomFormatSupport" -> PluginCapability.CustomFormatSupport
                        else -> {
                            logger?.w(TAG, "Unknown plugin capability: $capability")
                            null
                        }
                    }
                } catch (e: IllegalArgumentException) {
                    logger?.w(TAG, "Unknown plugin capability: $capability")
                    null
                }
            }
            .toSet()
    }

    companion object {
        private const val TAG = "ManifestPluginDiscovery"
    }
}

