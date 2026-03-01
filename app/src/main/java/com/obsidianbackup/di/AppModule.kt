// di/AppModule.kt
package com.obsidianbackup.di

import android.content.Context
import com.obsidianbackup.billing.FeatureGateService
import com.obsidianbackup.cloud.CloudSyncManager
import com.obsidianbackup.data.repository.CatalogRepository
import com.obsidianbackup.domain.backup.BackupEngineFactory
import com.obsidianbackup.domain.backup.BackupEventBus
import com.obsidianbackup.domain.backup.BackupOrchestrator
import com.obsidianbackup.domain.repository.ICatalogRepository
import com.obsidianbackup.domain.usecase.BackupAppsUseCase
import com.obsidianbackup.domain.usecase.RestoreAppsUseCase
import com.obsidianbackup.domain.restore.TransactionalRestoreEngine
import com.obsidianbackup.domain.restore.TransactionalRestoreEngineImpl
import com.obsidianbackup.engine.BackupEngine
import com.obsidianbackup.engine.ObsidianBoxEngine
import com.obsidianbackup.engine.restore.RestoreJournal
import com.obsidianbackup.engine.shell.AuditLogger
import com.obsidianbackup.engine.shell.SafeShellExecutor
import com.obsidianbackup.error.ErrorRecoveryManager
import com.obsidianbackup.error.RetryStrategy
import com.obsidianbackup.features.FeatureFlagManager
import com.obsidianbackup.features.RemoteConfig
import com.obsidianbackup.features.SharedPreferencesRemoteConfig
import com.obsidianbackup.logging.ConsoleLogSink
import com.obsidianbackup.logging.FileLogSink
import com.obsidianbackup.logging.LogLevel
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.permissions.PermissionManager
import com.obsidianbackup.scanner.AppScanner
import com.obsidianbackup.storage.BackupCatalog
import com.obsidianbackup.verification.ChecksumVerifier
import com.obsidianbackup.verification.MerkleTree
import com.obsidianbackup.automation.BackupScheduler
import com.obsidianbackup.crypto.EncryptionEngine
import com.obsidianbackup.crypto.EncryptedBackupDecorator
import com.obsidianbackup.plugins.core.PluginLoader
import com.obsidianbackup.plugins.core.PluginManager
import com.obsidianbackup.plugins.core.PluginRegistry
import com.obsidianbackup.plugins.core.PluginSandbox
import com.obsidianbackup.plugins.discovery.ManifestPluginDiscovery
import com.obsidianbackup.plugins.discovery.PackagePluginDiscovery
import com.obsidianbackup.plugins.discovery.PluginValidator
import com.obsidianbackup.work.WorkManagerScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineExceptionHandler
import timber.log.Timber
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCoroutineExceptionHandler(
        logger: ObsidianLogger
    ): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            Timber.e(throwable, "Uncaught coroutine exception")
            logger.e("CoroutineException", "Uncaught exception in coroutine", throwable)
        }
    }

    @Provides
    @Singleton
    fun provideBackupEngineFactory(
        engine: BackupEngine
    ): BackupEngineFactory {
        return BackupEngineFactory(engine)
    }

    @Provides
    @Singleton
    fun provideBackupEngine(
        permissionManager: PermissionManager,
        catalog: BackupCatalog,
        logger: ObsidianLogger,
        @ApplicationContext context: Context,
        safeShellExecutor: com.obsidianbackup.engine.shell.SafeShellExecutor,
        appScanner: AppScanner
    ): BackupEngine {
        return ObsidianBoxEngine(
            permissionManager = permissionManager,
            catalog = catalog,
            backupRootPath = context.getExternalFilesDir("backups")?.absolutePath 
                ?: context.filesDir.absolutePath,
            logger = logger,
            shellExecutor = safeShellExecutor,
            appScanner = appScanner
        )
    }

    @Provides
    @Singleton
    fun provideTransactionalRestoreEngineImpl(
        shellExecutor: com.obsidianbackup.engine.ShellExecutor,
        journal: RestoreJournal,
        catalog: BackupCatalog,
        @ApplicationContext context: Context
    ): com.obsidianbackup.engine.TransactionalRestoreEngine {
        return com.obsidianbackup.engine.TransactionalRestoreEngine(
            shellExecutor = shellExecutor,
            journal = journal,
            catalog = catalog,
            backupRootPath = context.getExternalFilesDir("backups")?.absolutePath 
                ?: context.filesDir.absolutePath
        )
    }
    
    @Provides
    @Singleton
    fun provideTransactionalRestoreEngine(
        engineImpl: com.obsidianbackup.engine.TransactionalRestoreEngine
    ): TransactionalRestoreEngine {
        return TransactionalRestoreEngineImpl(engineImpl)
    }

    @Provides
    @Singleton
    fun provideShellExecutor(permissionManager: PermissionManager): com.obsidianbackup.engine.ShellExecutor {
        // Provide the basic engine ShellExecutor constructed from current permission mode
        return com.obsidianbackup.engine.ShellExecutor(permissionManager.currentMode.value)
    }

    @Provides
    @Singleton
    fun provideSafeShellExecutor(
        permissionManager: PermissionManager,
        auditLogger: AuditLogger,
        busyBoxManager: com.obsidianbackup.rootcore.busybox.BusyBoxManager
    ): com.obsidianbackup.engine.shell.SafeShellExecutor {
        return com.obsidianbackup.engine.shell.SafeShellExecutor(
            permissionMode = permissionManager.currentMode.value,
            auditLogger = auditLogger,
            busyBoxBinDir = busyBoxManager.getBusyBoxBinDir()
        )
    }

    @Provides
    @Singleton
    fun provideAuditLogger(@ApplicationContext context: Context): AuditLogger {
        val auditFile = File(context.getExternalFilesDir("logs"), "shell_audit.log")
        return AuditLogger(auditFile)
    }

    @Provides
    @Singleton
    fun provideRestoreJournal(@ApplicationContext context: Context): RestoreJournal {
        val journalDir = File(context.getExternalFilesDir("backups"), "journals")
        return RestoreJournal(journalDir)
    }

    @Provides
    @Singleton
    fun provideBackupEventBus(): BackupEventBus {
        return BackupEventBus()
    }

    @Provides
    @Singleton
    fun provideIncrementalBackupStrategy(
        catalog: BackupCatalog,
        verifier: ChecksumVerifier
    ): com.obsidianbackup.engine.IncrementalBackupStrategy {
        return com.obsidianbackup.engine.IncrementalBackupStrategy(
            catalog = catalog,
            checksumVerifier = verifier
        )
    }

    @Provides
    @Singleton
    fun provideBackupOrchestrator(
        engineFactory: BackupEngineFactory,
        catalogRepository: com.obsidianbackup.domain.repository.ICatalogRepository,
        verifier: ChecksumVerifier,
        eventBus: BackupEventBus,
        incrementalStrategy: com.obsidianbackup.engine.IncrementalBackupStrategy,
        errorRecovery: com.obsidianbackup.error.ErrorRecoveryManager,
        featureGateService: FeatureGateService,
        @ApplicationContext context: Context
    ): BackupOrchestrator {
        val backupRootPath = context.getExternalFilesDir("backups")?.absolutePath 
            ?: context.filesDir.absolutePath
        return BackupOrchestrator(
            engineFactory, 
            catalogRepository, 
            verifier, 
            eventBus, 
            incrementalStrategy,
            backupRootPath,
            errorRecovery,
            featureGateService
        )
    }

    @Provides
    @Singleton
    fun provideBackupAppsUseCase(
        backupOrchestrator: BackupOrchestrator,
        retryStrategy: RetryStrategy,
        errorRecovery: ErrorRecoveryManager,
        featureGateService: FeatureGateService
    ): BackupAppsUseCase {
        return BackupAppsUseCase(backupOrchestrator, retryStrategy, errorRecovery, featureGateService)
    }

    @Provides
    @Singleton
    fun provideRestoreAppsUseCase(
        transactionalRestoreEngine: TransactionalRestoreEngine,
        retryStrategy: RetryStrategy,
        errorRecovery: ErrorRecoveryManager
    ): RestoreAppsUseCase {
        return RestoreAppsUseCase(transactionalRestoreEngine, retryStrategy, errorRecovery)
    }

    @Provides
    @Singleton
    fun provideVerifySnapshotUseCase(
        checksumVerifier: ChecksumVerifier,
        catalogRepository: ICatalogRepository,
        backupCatalog: BackupCatalog
    ): com.obsidianbackup.domain.usecase.VerifySnapshotUseCase {
        return com.obsidianbackup.domain.usecase.VerifySnapshotUseCase(checksumVerifier, catalogRepository, backupCatalog)
    }

    @Provides
    @Singleton
    fun providePermissionManager(
        @ApplicationContext context: Context,
        logger: ObsidianLogger,
        rootDetector: com.obsidianbackup.rootcore.detection.RootDetector
    ): PermissionManager {
        return PermissionManager(context, logger, rootDetector)
    }

    @Provides
    @Singleton
    fun provideBackupCatalog(@ApplicationContext context: Context): BackupCatalog {
        return BackupCatalog(context, context.cacheDir.absolutePath)
    }
    
    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): com.obsidianbackup.data.repository.SettingsRepository {
        return com.obsidianbackup.data.repository.SettingsRepository(context)
    }
    
    @Provides
    @Singleton
    fun provideLogRepository(backupCatalog: BackupCatalog): com.obsidianbackup.data.repository.LogRepository {
        return com.obsidianbackup.data.repository.LogRepository(backupCatalog.getLogDao())
    }

    @Provides
    @Singleton
    fun provideLabelDao(backupCatalog: BackupCatalog): com.obsidianbackup.storage.LabelDao {
        return backupCatalog.getLabelDao()
    }

    @Provides
    @Singleton
    fun provideBackupProfileDao(backupCatalog: BackupCatalog): com.obsidianbackup.storage.BackupProfileDao {
        return backupCatalog.getProfileDao()
    }


    @Provides
    @Singleton
    fun provideAppScanner(@ApplicationContext context: Context): AppScanner {
        return AppScanner(context)
    }

    @Provides
    @Singleton
    fun provideObsidianLogger(
        fileLogSink: FileLogSink,
        consoleLogSink: ConsoleLogSink
    ): ObsidianLogger {
        return ObsidianLogger(
            minLevel = LogLevel.INFO,
            sinks = listOf(fileLogSink, consoleLogSink)
        )
    }

    @Provides
    @Singleton
    fun provideFileLogSink(@ApplicationContext context: Context): FileLogSink {
        val logDir = File(context.getExternalFilesDir("logs"), "app_logs")
        return FileLogSink(logDir)
    }

    @Provides
    @Singleton
    fun provideFileSystemManager(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): com.obsidianbackup.storage.FileSystemManager {
        return com.obsidianbackup.storage.FileSystemManager(context, logger)
    }

    @Provides
    @Singleton
    fun provideMediaStoreHelper(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): com.obsidianbackup.storage.MediaStoreHelper {
        return com.obsidianbackup.storage.MediaStoreHelper(context, logger)
    }

    @Provides
    @Singleton
    fun provideSafHelper(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): com.obsidianbackup.storage.SafHelper {
        return com.obsidianbackup.storage.SafHelper(context, logger)
    }

    @Provides
    @Singleton
    fun provideScopedStorageMigration(
        @ApplicationContext context: Context,
        fileSystemManager: com.obsidianbackup.storage.FileSystemManager,
        logger: ObsidianLogger
    ): com.obsidianbackup.storage.ScopedStorageMigration {
        return com.obsidianbackup.storage.ScopedStorageMigration(context, fileSystemManager, logger)
    }

    @Provides
    @Singleton
    fun provideStoragePermissionHelper(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): com.obsidianbackup.storage.StoragePermissionHelper {
        return com.obsidianbackup.storage.StoragePermissionHelper(context, logger)
    }

    @Provides
    @Singleton
    fun provideConsoleLogSink(): ConsoleLogSink {
        return ConsoleLogSink()
    }

    @Provides
    @Singleton
    fun provideRetryStrategy(): RetryStrategy {
        return RetryStrategy()
    }

    @Provides
    @Singleton
    fun provideErrorRecoveryManager(
        @ApplicationContext context: Context,
        logger: ObsidianLogger,
        backupCatalog: BackupCatalog,
        fileSystemManager: com.obsidianbackup.storage.FileSystemManager
    ): ErrorRecoveryManager {
        return ErrorRecoveryManager(context, logger, backupCatalog, fileSystemManager)
    }

    @Provides
    @Singleton
    fun provideRemoteConfig(@ApplicationContext context: Context): RemoteConfig {
        val sharedPreferences = context.getSharedPreferences("obsidianbackup_features", Context.MODE_PRIVATE)
        return SharedPreferencesRemoteConfig(sharedPreferences)
    }

    @Provides
    @Singleton
    fun provideFeatureFlagManager(remoteConfig: RemoteConfig): FeatureFlagManager {
        return FeatureFlagManager(remoteConfig)
    }

    @Provides
    @Singleton
    fun provideEncryptionEngine(): EncryptionEngine {
        return EncryptionEngine()
    }

    @Provides
    @Singleton
    fun provideEncryptedBackupDecorator(
        baseEngine: BackupEngine,
        encryptionEngine: EncryptionEngine,
        @ApplicationContext context: Context
    ): EncryptedBackupDecorator {
        return EncryptedBackupDecorator(
            baseEngine, 
            encryptionEngine,
            context.getExternalFilesDir("backups")?.absolutePath 
                ?: context.filesDir.absolutePath
        )
    }

    @Provides
    @Singleton
    fun providePluginLoader(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): PluginLoader {
        return PluginLoader(context, logger)
    }

    @Provides
    @Singleton
    fun providePluginManager(
        @ApplicationContext context: Context,
        pluginRegistry: PluginRegistry,
        pluginLoader: PluginLoader,
        backupCatalog: BackupCatalog,
        appScope: kotlinx.coroutines.CoroutineScope
    ): com.obsidianbackup.plugin.PluginManager {
        // Trusted cert fingerprints could come from configuration; empty set means permissive
        val trusted = emptySet<String>()
        return com.obsidianbackup.plugin.PluginManager(context, pluginRegistry, pluginLoader, backupCatalog, trusted, appScope)
    }

    @Provides
    @Singleton
    fun provideAppCoroutineScope(): kotlinx.coroutines.CoroutineScope {
        return kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
    }

    @Provides
    @Singleton
    fun provideTrustedCertFingerprints(@ApplicationContext context: Context): Set<String> {
        // Load from secure storage or configuration; default empty
        return emptySet()
    }

    @Provides
    @Singleton
    fun providePluginRegistry(): PluginRegistry {
        return PluginRegistry()
    }

    @Provides
    @Singleton
    fun providePluginSandbox(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): PluginSandbox {
        return PluginSandbox(context, logger)
    }

    // ManifestPluginDiscovery and PackagePluginDiscovery use @Inject constructor

    @Provides
    @Singleton
    fun providePluginValidator(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): PluginValidator {
        return PluginValidator(context, logger)
    }

    @Provides
    @Singleton
    fun provideCloudSyncManager(
        @ApplicationContext context: Context,
        backupCatalog: BackupCatalog,
        @Named("default") cloudProvider: com.obsidianbackup.cloud.CloudProvider,
        workManagerScheduler: WorkManagerScheduler,
        logger: ObsidianLogger,
        checksumVerifier: ChecksumVerifier
    ): CloudSyncManager {
        return CloudSyncManager(context, backupCatalog, cloudProvider, workManagerScheduler, logger, checksumVerifier)
    }

    @Provides
    @Singleton
    fun provideWorkManagerScheduler(
        @ApplicationContext context: Context,
        logger: ObsidianLogger
    ): WorkManagerScheduler {
        return WorkManagerScheduler(context, logger)
    }
    
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): androidx.work.WorkManager {
        return androidx.work.WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideDefaultCloudProvider(@ApplicationContext context: Context): com.obsidianbackup.plugins.interfaces.CloudProviderPlugin {
        return com.obsidianbackup.plugins.builtin.LocalCloudProvider(context)
    }

    @Provides
    @Singleton
    fun provideDefaultAutomationPlugin(
        @ApplicationContext context: Context,
        backupOrchestrator: BackupOrchestrator,
        backupScheduler: BackupScheduler,
        logger: ObsidianLogger
    ): com.obsidianbackup.plugins.builtin.DefaultAutomationPlugin {
        return com.obsidianbackup.plugins.builtin.DefaultAutomationPlugin(context, backupOrchestrator, backupScheduler, logger)
    }

    @Provides
    @Singleton
    fun provideCatalogRepository(backupCatalog: BackupCatalog): CatalogRepository {
        return CatalogRepository(backupCatalog)
    }

    @Provides
    @Singleton
    fun provideJson(): kotlinx.serialization.json.Json {
        return kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            prettyPrint = false
            encodeDefaults = true
        }
    }
}
