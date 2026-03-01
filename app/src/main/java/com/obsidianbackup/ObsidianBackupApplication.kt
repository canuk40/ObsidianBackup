// ObsidianBackupApplication.kt
package com.obsidianbackup

import android.app.Application
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.plugins.api.PluginApiVersion
import com.obsidianbackup.plugins.api.PluginCapability
import com.obsidianbackup.plugins.api.PluginMetadata
import com.obsidianbackup.plugins.api.PluginCapability.ScheduledExecution
import com.obsidianbackup.plugins.builtin.DefaultAutomationPlugin
import com.obsidianbackup.plugins.builtin.FilecoinCloudProviderPlugin
import com.obsidianbackup.plugins.builtin.RcloneDropboxPlugin
import com.obsidianbackup.plugins.builtin.RcloneGoogleDrivePlugin
import com.obsidianbackup.plugins.builtin.RcloneS3Plugin
import com.obsidianbackup.plugins.core.PluginRegistry
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineExceptionHandler
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class ObsidianBackupApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var pluginRegistry: PluginRegistry
    
    @Inject
    lateinit var logger: ObsidianLogger
    
    @Inject
    lateinit var scopedStorageMigration: com.obsidianbackup.storage.ScopedStorageMigration
    
    @Inject
    lateinit var securityValidator: com.obsidianbackup.security.TaskerSecurityValidator
    
    @Inject
    lateinit var databaseTree: com.obsidianbackup.logging.DatabaseTree
    
    @Inject
    lateinit var logInitializer: com.obsidianbackup.logging.LogInitializer
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    // Global coroutine exception handler
    val globalExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Uncaught coroutine exception")
        logger.e("CoroutineException", "Uncaught exception in coroutine", throwable)
    }
    
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + globalExceptionHandler)
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    
    override fun onCreate() {
        super.onCreate()
        
        // Enable StrictMode in debug builds for performance monitoring
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
        
        // Initialize Timber with debug tree first
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Plant database tree after injection is complete
        Timber.plant(databaseTree)
        
        Timber.i("ObsidianBackup started")
        
        // Initialize sample logs if database is empty
        logInitializer.initializeSampleLogs()
        
        // Perform scoped storage migration if needed
        appScope.launch {
            performStorageMigration()
        }
        
        // Register built-in plugins
        appScope.launch {
            registerBuiltInPlugins()
        }
    }
    
    private class ProductionTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // In production, could write to LogRepository or just Android Log
            // For now, use Android log
            android.util.Log.println(priority, tag ?: "ObsidianBackup", message)
        }
    }
    
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        
        Timber.d("StrictMode enabled for performance monitoring")
    }
    
    private suspend fun performStorageMigration() {
        try {
            logger.i("ObsidianBackupApplication", "Checking scoped storage migration status...")
            val result = scopedStorageMigration.performMigrationIfNeeded()
            
            when (result) {
                is com.obsidianbackup.storage.ScopedStorageMigration.MigrationResult.Success -> {
                    logger.i("ObsidianBackupApplication", 
                        "Storage migration completed: ${result.migratedFiles} files, ${result.migratedBytes} bytes")
                }
                is com.obsidianbackup.storage.ScopedStorageMigration.MigrationResult.Failed -> {
                    logger.e("ObsidianBackupApplication", 
                        "Storage migration failed: ${result.error}")
                }
                com.obsidianbackup.storage.ScopedStorageMigration.MigrationResult.AlreadyCompleted -> {
                    logger.d("ObsidianBackupApplication", "Storage migration already completed")
                }
                com.obsidianbackup.storage.ScopedStorageMigration.MigrationResult.NotRequired -> {
                    logger.d("ObsidianBackupApplication", "Storage migration not required")
                }
                com.obsidianbackup.storage.ScopedStorageMigration.MigrationResult.NoDataToMigrate -> {
                    logger.d("ObsidianBackupApplication", "No data to migrate")
                }
            }
        } catch (e: Exception) {
            logger.e("ObsidianBackupApplication", "Error during storage migration", e)
        }
    }
    
    private suspend fun registerBuiltInPlugins() {
        val builtInPlugins = listOf(
            PluginMetadata(
                packageName = "com.obsidianbackup.automation.default",
                className = "com.obsidianbackup.plugins.builtin.DefaultAutomationPlugin",
                name = "Default Automation",
                description = "Built-in automation workflows for nightly, on-charge, weekly, and WiFi-based backups",
                version = "1.0.0",
                apiVersion = PluginApiVersion.V1_0,
                capabilities = setOf(
                    PluginCapability.BackgroundExecution,
                    ScheduledExecution
                ),
                author = "ObsidianBackup Team",
                minSdkVersion = 24
            ),
            PluginMetadata(
                packageName = "com.obsidianbackup.local",
                className = "com.obsidianbackup.plugins.builtin.LocalCloudProvider",
                name = "Local Storage",
                description = "Store backups on local device storage",
                version = "1.0.0",
                apiVersion = PluginApiVersion.V1_0,
                capabilities = setOf(
                    PluginCapability.ClientSideEncryption,
                    PluginCapability.BandwidthThrottling
                ),
                author = "ObsidianBackup Team",
                minSdkVersion = 24
            ),
            PluginMetadata(
                packageName = "com.obsidianbackup.rclone.gdrive",
                className = "com.obsidianbackup.plugins.builtin.RcloneGoogleDrivePlugin",
                name = "Google Drive (rclone)",
                description = "Multi-cloud backup to Google Drive using rclone",
                version = "1.0.0",
                apiVersion = PluginApiVersion.V1_0,
                capabilities = setOf(
                    PluginCapability.ClientSideEncryption,
                    PluginCapability.BandwidthThrottling,
                    PluginCapability.MultiRegionSupport
                ),
                author = "ObsidianBackup Team",
                minSdkVersion = 24
            ),
            PluginMetadata(
                packageName = "com.obsidianbackup.rclone.dropbox",
                className = "com.obsidianbackup.plugins.builtin.RcloneDropboxPlugin",
                name = "Dropbox (rclone)",
                description = "Multi-cloud backup to Dropbox using rclone",
                version = "1.0.0",
                apiVersion = PluginApiVersion.V1_0,
                capabilities = setOf(
                    PluginCapability.ClientSideEncryption,
                    PluginCapability.BandwidthThrottling
                ),
                author = "ObsidianBackup Team",
                minSdkVersion = 24
            ),
            PluginMetadata(
                packageName = "com.obsidianbackup.rclone.s3",
                className = "com.obsidianbackup.plugins.builtin.RcloneS3Plugin",
                name = "S3 Compatible (rclone)",
                description = "Multi-cloud backup to S3-compatible storage using rclone",
                version = "1.0.0",
                apiVersion = PluginApiVersion.V1_0,
                capabilities = setOf(
                    PluginCapability.ClientSideEncryption,
                    PluginCapability.BandwidthThrottling,
                    PluginCapability.MultiRegionSupport
                ),
                author = "ObsidianBackup Team",
                minSdkVersion = 24
            ),
            PluginMetadata(
                packageName = "com.obsidianbackup.plugins.builtin.filecoin",
                className = "com.obsidianbackup.plugins.builtin.FilecoinCloudProviderPlugin",
                name = "Filecoin/IPFS (Decentralized)",
                description = "Decentralized, censorship-resistant backup storage using IPFS and Filecoin",
                version = "1.0.0",
                apiVersion = PluginApiVersion.V1_0,
                capabilities = setOf(
                    PluginCapability.ClientSideEncryption,
                    PluginCapability.MultiRegionSupport
                ),
                author = "ObsidianBackup Team",
                minSdkVersion = 24
            )
        )
        
        pluginRegistry.registerPlugins(builtInPlugins)
        logger.i("ObsidianBackupApplication", "Registered ${builtInPlugins.size} built-in plugins")
    }
}
