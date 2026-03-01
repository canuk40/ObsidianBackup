// plugins/PluginManager.kt
package com.obsidianbackup.plugins

import android.content.Context
import android.content.pm.PackageManager
import com.obsidianbackup.api.plugin.*
import com.obsidianbackup.plugins.core.PluginLoader
import com.obsidianbackup.plugins.core.PluginRegistry as CorePluginRegistry
import com.obsidianbackup.plugin.PluginContextImpl
import com.obsidianbackup.plugins.api.PluginMetadata
import com.obsidianbackup.logging.ObsidianLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipEntry

class PluginManager(
    @ApplicationContext private val context: Context,
    private val pluginDir: File,
    private val pluginRegistry: CorePluginRegistry,
    private val pluginSandbox: PluginSandbox
) {
    private val _loadedPlugins = MutableStateFlow<Map<String, ObsidianBackupPlugin>>(emptyMap())
    val loadedPlugins: Flow<Map<String, ObsidianBackupPlugin>> = _loadedPlugins.asStateFlow()

    /**
     * Discover plugins from multiple sources
     */
    suspend fun discoverPlugins(): List<PluginManifest> {
        val discovered = mutableListOf<PluginManifest>()

        // 1. Scan plugin directory
        discovered.addAll(scanPluginDirectory())

        // 2. Check AndroidManifest metadata
        discovered.addAll(scanInstalledApps())

        // 3. Load from core plugin registry
        try {
            val coreList = pluginRegistry.getAllPlugins()
            coreList.forEach { meta ->
                discovered.add(
                    PluginManifest(
                        id = meta.packageName,
                        name = meta.name,
                        version = meta.version,
                        author = meta.author,
                        apiVersion = "1.0.0",
                        description = meta.description,
                        pluginClass = meta.className,
                        capabilities = emptyList(),
                        minPlatformVersion = null,
                        signature = meta.signatureSha256
                    )
                )
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to discover plugin from manifest")
        }

        return discovered.distinctBy { it.id }
    }

    /**
     * Load and initialize a plugin
     */
    suspend fun loadPlugin(manifest: PluginManifest): Result<ObsidianBackupPlugin> {
        try {
            // 1. Verify API compatibility
            val apiVersion = try { PluginApiVersion.parse(manifest.apiVersion) } catch (_: Exception) { PluginApiVersion(1,0,0) }
            if (!apiVersion.isCompatibleWith(PluginApiVersion.MIN_SUPPORTED)) {
                return Result.failure(PluginException("Plugin API version $apiVersion is not supported"))
            }

            // 2. Verify signature (if provided) using sandbox helper
            if (!manifest.signature.isNullOrBlank()) {
                if (!pluginSandbox.verifySignature(manifest)) {
                    return Result.failure(PluginException("Plugin signature verification failed"))
                }
            }

            // 3. Load plugin class via core PluginLoader
            val loader = PluginLoader(context, ObsidianLogger())
            val coreMeta = PluginMetadata(
                packageName = manifest.id,
                className = manifest.pluginClass,
                name = manifest.name,
                description = manifest.description,
                version = manifest.version,
                apiVersion = com.obsidianbackup.plugins.api.PluginApiVersion.CURRENT,
                capabilities = emptySet(),
                author = manifest.author,
                website = null,
                minSdkVersion = 24,
                signatureSha256 = manifest.signature
            )

            val instanceAny = try { loader.loadPlugin(coreMeta) } catch (_: Exception) { null }

            val plugin = (instanceAny as? ObsidianBackupPlugin) ?: run {
                // fallback: proxy plugin
                object : ObsidianBackupPlugin {
                    override val id: String = manifest.id
                    override val name: String = manifest.name
                    override val version: String = manifest.version
                    override val author: String = manifest.author
                    override val apiVersion = PluginApiVersion.parse(manifest.apiVersion)
                    override val description: String = manifest.description
                    override val capabilities: Set<PluginCapability> = emptySet()

                    override suspend fun initialize(context: PluginContext): PluginResult<Unit> = PluginResult.Success(Unit)
                    override suspend fun cleanup() {
                        // Fallback plugin cleanup - no resources to release
                    }
                }
            }

            // 4. Verify capabilities via sandbox
            if (!pluginSandbox.verifyCapabilities(plugin.capabilities)) {
                return Result.failure(PluginException("Plugin capabilities cannot be granted"))
            }

            // 5. Initialize plugin in sandbox
            val pluginContext = createPluginContext(manifest.id)
            val initResult = pluginSandbox.executeInSandbox(plugin) {
                plugin.initialize(pluginContext)
            }

            when (initResult) {
                is PluginResult.Success<*> -> {
                    _loadedPlugins.value = _loadedPlugins.value + (manifest.id to plugin)
                    try { 
                        pluginRegistry.registerPlugins(listOf(coreMeta)) 
                    } catch (e: Exception) {
                        timber.log.Timber.e(e, "Failed to register plugin: ${manifest.id}")
                    }
                    return Result.success(plugin)
                }
                is PluginResult.Error -> {
                    return Result.failure(PluginException("Plugin initialization failed: ${initResult.error.message}"))
                }
            }

            return Result.failure(PluginException("Unknown plugin initialization result"))

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Unload a plugin
     */
    suspend fun unloadPlugin(pluginId: String): Result<Unit> {
        val plugin = _loadedPlugins.value[pluginId] ?: return Result.failure(PluginException("Plugin not loaded: $pluginId"))
        return try {
            plugin.cleanup()
            _loadedPlugins.value = _loadedPlugins.value - pluginId
            pluginRegistry.unregisterPlugin(pluginId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getPlugin(pluginId: String): ObsidianBackupPlugin? = _loadedPlugins.value[pluginId]

    fun <T : ObsidianBackupPlugin> getPluginsOfType(clazz: Class<T>): List<T> =
        _loadedPlugins.value.values.filter { clazz.isInstance(it) }.map { clazz.cast(it) }

    /**
     * Cleanup all loaded plugins - releases resources and unloads plugins
     */
    suspend fun cleanup() {
        val plugins = _loadedPlugins.value.values.toList()
        plugins.forEach { plugin ->
            try {
                plugin.cleanup()
            } catch (e: Exception) {
                // Log error but continue cleanup
            }
        }
        _loadedPlugins.value = emptyMap()
    }

    /**
     * Broadcast backup start event to all loaded plugins
     */
    suspend fun notifyBackupStart(context: BackupContext) {
        _loadedPlugins.value.values.forEach { plugin ->
            try {
                pluginSandbox.executeInSandbox(plugin) {
                    plugin.onBackupStart(context)
                }
            } catch (e: Exception) {
                // Log error but continue notifying other plugins
            }
        }
    }

    /**
     * Broadcast backup complete event to all loaded plugins
     */
    suspend fun notifyBackupComplete(result: com.obsidianbackup.model.BackupResult) {
        _loadedPlugins.value.values.forEach { plugin ->
            try {
                pluginSandbox.executeInSandbox(plugin) {
                    plugin.onBackupComplete(result)
                }
            } catch (e: Exception) {
                // Log error but continue notifying other plugins
            }
        }
    }

    /**
     * Broadcast restore start event to all loaded plugins
     */
    suspend fun notifyRestoreStart(context: RestoreContext) {
        _loadedPlugins.value.values.forEach { plugin ->
            try {
                pluginSandbox.executeInSandbox(plugin) {
                    plugin.onRestoreStart(context)
                }
            } catch (e: Exception) {
                // Log error but continue notifying other plugins
            }
        }
    }

    private fun createPluginContext(pluginId: String): PluginContext {
        val dir = File(pluginDir, pluginId).apply { mkdirs() }
        val prefs = context.getSharedPreferences("plugin_$pluginId", Context.MODE_PRIVATE)
        return PluginContextImpl(context, dir, prefs)
    }

    private suspend fun scanPluginDirectory(): List<PluginManifest> {
        return pluginDir.listFiles()
            ?.filter { it.extension == "apk" || it.extension == "jar" }
            ?.mapNotNull { file ->
                try {
                    // try to find assets/obsidianbackup-manifest.json or obsidianbackup-manifest.json
                    ZipFile(file).use { zip ->
                        val candidates = listOf("assets/obsidianbackup-manifest.json", "obsidianbackup-manifest.json", "assets/plugin_manifest.json")
                        val entry: ZipEntry? = candidates.mapNotNull { name -> zip.getEntry(name) }.firstOrNull()
                        if (entry != null) {
                            zip.getInputStream(entry).use { ins ->
                                val text = ins.readBytes().decodeToString()
                                return@mapNotNull Json.decodeFromString(PluginManifest.serializer(), text)
                            }
                        }
                    }
                    null
                } catch (e: Exception) { null }
            } ?: emptyList()
    }

    private fun scanInstalledApps(): List<PluginManifest> {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return packages.mapNotNull { appInfo ->
            appInfo.metaData?.getString("titan.backup.plugin")?.let { manifestPath ->
                try {
                    val manifest = context.createPackageContext(appInfo.packageName, 0)
                        .assets.open(manifestPath)
                        .use { input -> Json.decodeFromString(PluginManifest.serializer(), input.readBytes().decodeToString()) }
                    manifest
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}

class PluginException(message: String, cause: Throwable? = null) : Exception(message, cause)
