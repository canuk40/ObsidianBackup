// plugin/PluginAPI.kt
package com.obsidianbackup.plugin

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import com.obsidianbackup.api.plugin.ObsidianBackupPlugin
import com.obsidianbackup.api.plugin.BackupContext
import com.obsidianbackup.api.plugin.RestoreContext
import com.obsidianbackup.api.plugin.BackupProcessor
import com.obsidianbackup.api.plugin.RestoreProcessor
import com.obsidianbackup.api.plugin.PluginCapability
import com.obsidianbackup.api.plugin.PluginContext
import com.obsidianbackup.model.*
import java.io.File
import com.obsidianbackup.model.BackupRequest
import com.obsidianbackup.model.BackupResult
import com.obsidianbackup.model.RestoreRequest
import com.obsidianbackup.model.RestoreResult
import com.obsidianbackup.model.BackupId as ModelBackupId
import com.obsidianbackup.storage.BackupCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import com.obsidianbackup.plugins.core.PluginLoader
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.plugins.api.PluginMetadata
import com.obsidianbackup.plugins.core.PluginRegistry

// Example plugin: Cloud sync
class DropboxSyncPlugin(
    private val context: Context,
    private val rclone: RcloneSyncEngine?
) : ObsidianBackupPlugin {
    override val id = "com.obsidianbackup.plugin.dropbox"
    override val name = "Dropbox Sync"
    override val version = "1.0.0"
    override val author: String = "ObsidianBackup"
    override val apiVersion = com.obsidianbackup.api.plugin.PluginApiVersion.CURRENT
    override val description: String = "Dropbox sync plugin (uses rclone)"
    override val capabilities: Set<PluginCapability> = emptySet()

    override suspend fun initialize(context: PluginContext): com.obsidianbackup.api.plugin.PluginResult<Unit> {
        // No-op initialization
        return com.obsidianbackup.api.plugin.PluginResult.Success(Unit)
    }

    override suspend fun cleanup() {
        // Cleanup Dropbox sync plugin resources
        // Release any open connections or cancel pending operations
    }

    override fun onBackupStart(context: BackupContext) {
        // Called when backup starts - prepare for potential sync
    }

    override fun onBackupComplete(result: BackupResult) {
        if (result is BackupResult.Success) {
            try {
                rclone?.let { engine ->
                    CoroutineScope(Dispatchers.IO).launch {
                        runCatching {
                            engine.syncToCloud(ModelBackupId(result.snapshotId.value), CloudRemoteConfig("dropbox:"))
                        }.onFailure { e ->
                            timber.log.Timber.e(e, "Failed to sync backup to cloud")
                        }
                    }
                }
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Error initiating cloud sync")
            }
        }
    }

    override fun onRestoreStart(context: RestoreContext) {
        // Called when restore starts - prepare for potential sync from cloud
    }

    override fun provideBackupProcessor(): BackupProcessor? = null
    override fun provideRestoreProcessor(): RestoreProcessor? = null

    override fun provideConfigurationScreen() = @Composable {
        DropboxSettingsScreen()
    }
}

@Composable
fun DropboxSettingsScreen() {
    Text("Dropbox Settings")
}

// Plugin manager with sandboxing - integrated with core PluginLoader/Registry
class PluginManager(
    private val context: Context,
    private val coreRegistry: PluginRegistry,
    private val pluginLoader: PluginLoader,
    private val catalog: BackupCatalog? = null,
    private val trustedCertFingerprints: Set<String> = emptySet(),
    private val appScope: CoroutineScope
) {
    private val loadedPlugins = mutableMapOf<String, ObsidianBackupPlugin>()

    private fun computeApkSha256(apkFile: File): String? {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            apkFile.inputStream().use { fis ->
                val buffer = ByteArray(8192)
                var read: Int
                while (fis.read(buffer).also { read = it } > 0) {
                    md.update(buffer, 0, read)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun getApkSigningFingerprints(apkPath: String): List<String> {
        val pm = context.packageManager
        val pkgInfo = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_SIGNING_CERTIFICATES)
            ?: throw IllegalArgumentException("Cannot read package info from APK: $apkPath")
        
        pkgInfo.applicationInfo?.let { ai ->
            ai.sourceDir = apkPath
            ai.publicSourceDir = apkPath
        }
        
        val signingInfo = pkgInfo.signingInfo
            ?: throw IllegalStateException("APK has no signing information: $apkPath")
        
        val signatures = signingInfo.apkContentsSigners ?: signingInfo.signingCertificateHistory
            ?: throw IllegalStateException("Cannot extract signatures from APK: $apkPath")
        
        val md = MessageDigest.getInstance("SHA-256")
        return signatures.map { sig ->
            val digest = md.digest(sig.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        }
    }

    suspend fun loadPlugin(apkPath: String): ObsidianBackupPlugin? {
        try {
            val packageInfo = context.packageManager.getPackageArchiveInfo(apkPath, PackageManager.GET_META_DATA)
            val pluginId = packageInfo?.packageName ?: return null

            val apkFile = File(apkPath)
            val apkSha = if (apkFile.exists()) computeApkSha256(apkFile) else null

            // Certificate-based verification: get signing fingerprints
            val certFingerprints = getApkSigningFingerprints(apkPath)

            // Enforce allowlist if configured
            if (trustedCertFingerprints.isNotEmpty()) {
                val matched = certFingerprints.any { trustedCertFingerprints.contains(it) }
                if (!matched) {
                    return null
                }
            }

            // find plugin class name in manifest metadata
            val metaClassName = try {
                context.packageManager.getPackageArchiveInfo(apkPath, PackageManager.GET_META_DATA)?.applicationInfo?.metaData?.getString("obsidianbackup.plugin.class")
            } catch (e: Exception) {
                null
            }

            val plugin: ObsidianBackupPlugin? = if (!metaClassName.isNullOrBlank()) {
                val metadata = com.obsidianbackup.plugins.api.PluginMetadata(
                    packageName = packageInfo.packageName ?: pluginId,
                    className = metaClassName,
                    name = packageInfo.packageName ?: pluginId,
                    description = "",
                    version = packageInfo.versionName ?: "0",
                    apiVersion = com.obsidianbackup.plugins.api.PluginApiVersion.CURRENT,
                    capabilities = emptySet(),
                    author = "unknown",
                    website = null,
                    minSdkVersion = 24,
                    signatureSha256 = apkSha
                )

                val instance = pluginLoader.loadPlugin(metadata)
                (instance as? ObsidianBackupPlugin)
            } else null

            val finalPlugin = plugin ?: object : ObsidianBackupPlugin {
                override val id: String = pluginId
                override val name: String = packageInfo.packageName ?: "unknown"
                override val version: String = packageInfo.versionName ?: "0"
                override val author: String = "unknown"
                override val apiVersion = com.obsidianbackup.api.plugin.PluginApiVersion.CURRENT
                override val description: String = "Proxy plugin for $pluginId"
                override val capabilities: Set<PluginCapability> = emptySet()

                override suspend fun initialize(context: PluginContext) = com.obsidianbackup.api.plugin.PluginResult.Success(Unit)
                override suspend fun cleanup() {
                    // Proxy plugin cleanup - no resources to release
                }
                override fun onBackupStart(context: BackupContext) {
                    // Proxy plugin - no-op for lifecycle events
                }
                override fun onBackupComplete(result: BackupResult) {
                    // Proxy plugin - no-op for lifecycle events
                }
                override fun onRestoreStart(context: RestoreContext) {
                    // Proxy plugin - no-op for lifecycle events
                }
                override fun provideBackupProcessor(): BackupProcessor? = null
                override fun provideRestoreProcessor(): RestoreProcessor? = null
                override fun provideConfigurationScreen(): (@Composable () -> Unit)? = null
            }

            val pluginDir = File(context.filesDir, "plugins/${finalPlugin.id}").apply { mkdirs() }
            val prefs = context.getSharedPreferences("plugin_${finalPlugin.id}", Context.MODE_PRIVATE)
            val pluginContext = PluginContextImpl(context, pluginDir, prefs, catalog)

            appScope.launch {
                try { 
                    finalPlugin.initialize(pluginContext) 
                } catch (e: Exception) {
                    timber.log.Timber.e(e, "Failed to initialize plugin: ${finalPlugin.id}")
                }
            }

            // register in core registry if real plugin
            plugin?.let {
                coreRegistry.registerPlugins(listOf(com.obsidianbackup.plugins.api.PluginMetadata(
                    packageName = it.id,
                    className = metaClassName ?: "",
                    name = it.name,
                    description = it.description,
                    version = it.version,
                    apiVersion = com.obsidianbackup.plugins.api.PluginApiVersion.CURRENT,
                    capabilities = emptySet(),
                    author = it.author
                )))
            }

            loadedPlugins[finalPlugin.id] = finalPlugin
            return finalPlugin

        } catch (e: Exception) {
            return null
        }
    }

    fun getPlugin(pluginId: String): ObsidianBackupPlugin? = loadedPlugins[pluginId]

    fun unloadPlugin(pluginId: String) {
        loadedPlugins[pluginId]?.let { plugin ->
            appScope.launch {
                try {
                    plugin.cleanup()
                } catch (_: Exception) {
                    // Log error but continue
                }
            }
        }
        loadedPlugins.remove(pluginId)
    }

    /**
     * Cleanup all loaded plugins
     */
    suspend fun cleanup() {
        val plugins = loadedPlugins.values.toList()
        plugins.forEach { plugin ->
            try {
                plugin.cleanup()
            } catch (e: Exception) {
                // Log error but continue cleanup
            }
        }
        loadedPlugins.clear()
    }

    /**
     * Notify all plugins that a backup is starting
     */
    fun notifyBackupStart(context: BackupContext) {
        loadedPlugins.values.forEach { plugin ->
            try {
                plugin.onBackupStart(context)
            } catch (e: Exception) {
                // Log error but continue notifying other plugins
            }
        }
    }

    /**
     * Notify all plugins that a backup has completed
     */
    fun notifyBackupComplete(result: BackupResult) {
        loadedPlugins.values.forEach { plugin ->
            try {
                plugin.onBackupComplete(result)
            } catch (e: Exception) {
                // Log error but continue notifying other plugins
            }
        }
    }

    /**
     * Notify all plugins that a restore is starting
     */
    fun notifyRestoreStart(context: RestoreContext) {
        loadedPlugins.values.forEach { plugin ->
            try {
                plugin.onRestoreStart(context)
            } catch (e: Exception) {
                // Log error but continue notifying other plugins
            }
        }
    }
}

class RcloneSyncEngine(private val shellExecutor: com.obsidianbackup.engine.ShellExecutor) {
    suspend fun syncToCloud(snapshotId: com.obsidianbackup.model.BackupId, remoteConfig: CloudRemoteConfig): SyncResult {
        val command = """
            rclone sync \
              ${getSnapshotPath(snapshotId)} \
              ${remoteConfig.remote}:obsidianbackup/${snapshotId.value} \
              --checksum \
              --progress
        """.trimIndent()

        val result = shellExecutor.execute(command)
        return when (result) {
            is com.obsidianbackup.engine.ShellResult.Success -> SyncResult.Success
            is com.obsidianbackup.engine.ShellResult.Error -> SyncResult.Failed(result.error)
        }
    }

    private fun getSnapshotPath(snapshotId: com.obsidianbackup.model.BackupId): String {
        val base = contextFilesBase()
        val snapshots = File(base, "backups")
        return File(snapshots, snapshotId.value).absolutePath
    }

    private fun contextFilesBase(): String = System.getProperty("user.home") ?: "/data/user/0/com.obsidianbackup/files"
}

data class CloudRemoteConfig(val remote: String, val encryption: Boolean = true)

sealed class SyncResult { object Success : SyncResult(); data class Failed(val error: String) : SyncResult() }
