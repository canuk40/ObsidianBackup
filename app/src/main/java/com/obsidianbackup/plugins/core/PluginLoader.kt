// plugins/core/PluginLoader.kt
package com.obsidianbackup.plugins.core

import android.content.Context
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.plugins.api.PluginMetadata
import com.obsidianbackup.plugins.api.PluginException
import dalvik.system.PathClassLoader
import java.io.File

class PluginLoader(
    private val context: Context,
    private val logger: ObsidianLogger? = null
) {

    private val loadedClasses = mutableMapOf<String, Class<*>>()

    suspend fun loadPlugin(metadata: PluginMetadata): Any {
        try {
            // Check if already loaded
            loadedClasses[metadata.className]?.let { clazz ->
                return instantiatePlugin(clazz, metadata)
            }

            // Load plugin APK if it's a package-based plugin
            val pluginApk = getPluginApkFile(metadata)
            if (pluginApk != null && pluginApk.exists()) {
                return loadFromApk(pluginApk, metadata)
            }

            // Try loading from current classpath (for built-in plugins)
            return loadFromClasspath(metadata)

        } catch (e: Exception) {
            logger?.e(TAG, "Failed to load plugin ${metadata.packageName}", e)
            throw PluginException.PluginLoadFailed(metadata.packageName, e)
        }
    }

    private fun loadFromApk(apkFile: File, metadata: PluginMetadata): Any {
        logger?.d(TAG, "Loading plugin from APK: ${apkFile.absolutePath}")

        val classLoader = PathClassLoader(
            apkFile.absolutePath,
            context.classLoader
        )

        val clazz = Class.forName(metadata.className, true, classLoader)
        loadedClasses[metadata.className] = clazz

        return instantiatePlugin(clazz, metadata)
    }

    private fun loadFromClasspath(metadata: PluginMetadata): Any {
        logger?.d(TAG, "Loading plugin from classpath: ${metadata.className}")

        val clazz = Class.forName(metadata.className)
        loadedClasses[metadata.className] = clazz

        return instantiatePlugin(clazz, metadata)
    }

    private fun instantiatePlugin(clazz: Class<*>, metadata: PluginMetadata): Any {
        // Try to find a constructor that takes Context
        return try {
            val constructor = clazz.getConstructor(Context::class.java)
            constructor.newInstance(context)
        } catch (e: NoSuchMethodException) {
            // Try no-arg constructor
            clazz.getConstructor().newInstance()
        }
    }

    private fun getPluginApkFile(metadata: PluginMetadata): File? {
        // For signed plugins, the APK should be in a specific directory
        val pluginsDir = File(context.filesDir, "plugins")
        return File(pluginsDir, "${metadata.packageName}.apk").takeIf { it.exists() }
    }

    suspend fun unloadPlugin(metadata: PluginMetadata) {
        loadedClasses.remove(metadata.className)
        logger?.d(TAG, "Unloaded plugin: ${metadata.packageName}")
    }

    companion object {
        private const val TAG = "PluginLoader"
    }
}
