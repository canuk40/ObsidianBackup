// plugins/discovery/PackagePluginDiscovery.kt
package com.obsidianbackup.plugins.discovery

import android.content.Context
import android.content.pm.PackageManager
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.plugins.api.PluginMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

class PackagePluginDiscovery @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: ObsidianLogger
) : PluginDiscovery {

    private val pluginsDir = File(context.filesDir, "plugins")

    override suspend fun discoverPlugins(): List<PluginMetadata> {
        return discover()
    }

    suspend fun discover(): List<PluginMetadata> {
        val plugins = mutableListOf<PluginMetadata>()

        try {
            if (!pluginsDir.exists()) {
                pluginsDir.mkdirs()
                return plugins // No plugins installed yet
            }

            val pluginFiles = pluginsDir.listFiles { file ->
                file.isFile && file.extension == "apk"
            } ?: emptyArray()

            for (pluginApk in pluginFiles) {
                try {
                    val metadata = extractPluginMetadata(pluginApk)
                    if (metadata != null) {
                        plugins.add(metadata)
                        logger.d(TAG, "Discovered package plugin: ${metadata.name}")
                    }
                } catch (e: Exception) {
                    logger.e(TAG, "Failed to load plugin from ${pluginApk.name}", e)
                }
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to discover package plugins", e)
        }

        return plugins
    }

    private fun extractPluginMetadata(pluginApk: File): PluginMetadata? {
        // For signed plugins, we need to extract metadata from the APK
        // This is a simplified implementation - in practice, you'd use PackageManager
        // to get package info from the APK file

        try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageArchiveInfo(pluginApk.absolutePath, PackageManager.GET_META_DATA)

            if (packageInfo != null) {
                val applicationInfo = packageInfo.applicationInfo
                if (applicationInfo != null) {
                    applicationInfo.sourceDir = pluginApk.absolutePath
                    applicationInfo.publicSourceDir = pluginApk.absolutePath

                    val metaData = applicationInfo.metaData
                    if (metaData != null && metaData.containsKey("obsidianbackup.plugin")) {
                        // Use the manifest discovery logic
                        val manifestDiscovery = ManifestPluginDiscovery(context, logger)
                        return manifestDiscovery.parsePluginMetadata(packageInfo, metaData)
                    }
                }
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to extract metadata from ${pluginApk.name}", e)
        }

        return null
    }

    /**
     * Install a plugin APK
     */
    suspend fun installPlugin(apkFile: File): kotlin.Result<PluginMetadata> {
        return try {
            // Validate the APK
            val metadata = extractPluginMetadata(apkFile)
                ?: return kotlin.Result.failure(Exception("Invalid plugin APK"))

            // Copy to plugins directory
            val targetFile = File(pluginsDir, "${metadata.packageName}.apk")
            apkFile.copyTo(targetFile, overwrite = true)

            logger.i(TAG, "Installed plugin: ${metadata.name}")
            kotlin.Result.success(metadata)

        } catch (e: Exception) {
            logger.e(TAG, "Failed to install plugin", e)
            kotlin.Result.failure(e)
        }
    }

    /**
     * Uninstall a plugin
     */
    suspend fun uninstallPlugin(packageName: String): kotlin.Result<Unit> {
        return try {
            val pluginFile = File(pluginsDir, "$packageName.apk")
            if (pluginFile.exists()) {
                pluginFile.delete()
                logger.i(TAG, "Uninstalled plugin: $packageName")
                kotlin.Result.success(Unit)
            } else {
                kotlin.Result.failure(Exception("Plugin not found: $packageName"))
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to uninstall plugin: $packageName", e)
            kotlin.Result.failure(e)
        }
    }

    /**
     * Get installed plugin APKs
     */
    fun getInstalledPlugins(): List<File> {
        return pluginsDir.listFiles { file ->
            file.isFile && file.extension == "apk"
        }?.toList() ?: emptyList()
    }

    companion object {
        private const val TAG = "PackagePluginDiscovery"
    }
}
