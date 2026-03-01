# ObsidianBackup Platform Specification

**Version:** 1.0.0  
**Date:** February 1, 2026  
**Platform:** Android API 24+ (Android 7.0+)  

## 1. Plugin Ecosystem Design

### Package Layout

```
com.obsidianbackup.plugins/
├── api/
│   ├── PluginApiVersion.kt
│   ├── PluginMetadata.kt
│   ├── PluginCapability.kt
│   └── PluginException.kt
├── core/
│   ├── PluginManager.kt
│   ├── PluginRegistry.kt
│   ├── PluginLoader.kt
│   └── PluginSandbox.kt
├── interfaces/
│   ├── BackupEnginePlugin.kt
│   ├── CloudProviderPlugin.kt
│   ├── AutomationPlugin.kt
│   └── ExportPlugin.kt
├── discovery/
│   ├── ManifestPluginDiscovery.kt
│   ├── PackagePluginDiscovery.kt
│   └── PluginValidator.kt
└── builtin/
    ├── LocalCloudProvider.kt
    ├── WebDavCloudProvider.kt
    └── DefaultAutomationPlugin.kt
```

### Core Interfaces

#### PluginApiVersion.kt
```kotlin
package com.obsidianbackup.plugins.api

@JvmInline
value class PluginApiVersion(val version: Int) {
    val major: Int get() = version shr 16
    val minor: Int get() = version and 0xFFFF
    
    companion object {
        val V1_0 = PluginApiVersion((1 shl 16) or 0)
        val CURRENT = V1_0
    }
}

fun pluginApiVersion(major: Int, minor: Int): PluginApiVersion {
    require(major in 0..65535 && minor in 0..65535)
    return PluginApiVersion((major shl 16) or minor)
}
```

#### PluginMetadata.kt
```kotlin
package com.obsidianbackup.plugins.api

import android.content.pm.PackageInfo

data class PluginMetadata(
    val packageName: String,
    val className: String,
    val name: String,
    val description: String,
    val version: String,
    val apiVersion: PluginApiVersion,
    val capabilities: Set<PluginCapability>,
    val author: String,
    val website: String? = null,
    val minSdkVersion: Int = 24,
    val signatureSha256: String? = null // For signed plugins
)
```

#### PluginCapability.kt
```kotlin
package com.obsidianbackup.plugins.api

sealed class PluginCapability {
    // Backup Engine Capabilities
    object IncrementalBackup : PluginCapability()
    object EncryptionSupport : PluginCapability()
    object CompressionSupport : PluginCapability()
    
    // Cloud Provider Capabilities
    object MultiRegionSupport : PluginCapability()
    object BandwidthThrottling : PluginCapability()
    object ClientSideEncryption : PluginCapability()
    
    // Automation Capabilities
    object BackgroundExecution : PluginCapability()
    object SystemEventHooks : PluginCapability()
    object NetworkAwareness : PluginCapability()
    
    // Export Capabilities
    object StreamingExport : PluginCapability()
    object BatchExport : PluginCapability()
    object CustomFormatSupport : PluginCapability()
}
```

#### BackupEnginePlugin.kt
```kotlin
package com.obsidianbackup.plugins.interfaces

import com.obsidianbackup.model.*
import com.obsidianbackup.plugins.api.PluginMetadata
import kotlinx.coroutines.flow.Flow

interface BackupEnginePlugin {
    
    val metadata: PluginMetadata
    
    /**
     * Check if this engine can handle the given backup request
     */
    suspend fun canHandle(request: BackupRequest): Boolean
    
    /**
     * Perform backup operation
     */
    suspend fun backupApps(request: BackupRequest): BackupResult
    
    /**
     * Perform restore operation
     */
    suspend fun restoreApps(request: RestoreRequest): RestoreResult
    
    /**
     * Verify snapshot integrity
     */
    suspend fun verifySnapshot(snapshotId: SnapshotId): VerificationResult
    
    /**
     * Delete snapshot
     */
    suspend fun deleteSnapshot(snapshotId: SnapshotId): Boolean
    
    /**
     * Get engine capabilities for UI display
     */
    fun getCapabilities(): EngineCapabilities
    
    /**
     * Observe operation progress
     */
    fun observeProgress(): Flow<OperationProgress>
    
    /**
     * Cleanup resources
     */
    suspend fun cleanup()
}

data class EngineCapabilities(
    val supportsIncremental: Boolean = false,
    val supportsEncryption: Boolean = false,
    val supportsCompression: Boolean = true,
    val maxConcurrentOperations: Int = 1,
    val supportedFormats: List<String> = listOf("tar.zst")
)
```

#### CloudProviderPlugin.kt
```kotlin
package com.obsidianbackup.plugins.interfaces

import com.obsidianbackup.model.*
import com.obsidianbackup.plugins.api.PluginMetadata
import kotlinx.coroutines.flow.Flow

interface CloudProviderPlugin {
    
    val metadata: PluginMetadata
    
    /**
     * Initialize provider with configuration
     */
    suspend fun initialize(config: CloudConfig): Result<Unit>
    
    /**
     * Test connectivity and permissions
     */
    suspend fun testConnection(): CloudResult
    
    /**
     * Upload snapshot to cloud
     */
    suspend fun uploadSnapshot(snapshotId: SnapshotId, file: File): CloudResult
    
    /**
     * Download snapshot from cloud
     */
    suspend fun downloadSnapshot(snapshotId: SnapshotId): CloudResult
    
    /**
     * List available snapshots in cloud
     */
    suspend fun listSnapshots(): List<CloudSnapshot>
    
    /**
     * Delete snapshot from cloud
     */
    suspend fun deleteSnapshot(snapshotId: SnapshotId): CloudResult
    
    /**
     * Get provider capabilities
     */
    fun getCapabilities(): CloudCapabilities
    
    /**
     * Observe transfer progress
     */
    fun observeProgress(): Flow<TransferProgress>
    
    /**
     * Cleanup resources
     */
    suspend fun cleanup()
}

data class CloudConfig(
    val providerId: String,
    val credentials: Map<String, String>,
    val endpoint: String? = null,
    val region: String? = null,
    val bucket: String? = null
)

data class CloudResult(
    val success: Boolean,
    val snapshotId: SnapshotId? = null,
    val error: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

data class CloudSnapshot(
    val snapshotId: SnapshotId,
    val size: Long,
    val uploadedAt: Long,
    val checksum: String,
    val metadata: Map<String, String> = emptyMap()
)

data class CloudCapabilities(
    val supportsEncryption: Boolean = false,
    val supportsCompression: Boolean = false,
    val maxFileSize: Long = Long.MAX_VALUE,
    val supportedRegions: List<String> = emptyList(),
    val bandwidthThrottling: Boolean = false
)

data class TransferProgress(
    val snapshotId: SnapshotId,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val speedBps: Long
)
```

#### AutomationPlugin.kt
```kotlin
package com.obsidianbackup.plugins.interfaces

import com.obsidianbackup.model.*
import com.obsidianbackup.plugins.api.PluginMetadata
import kotlinx.coroutines.flow.Flow

interface AutomationPlugin {
    
    val metadata: PluginMetadata
    
    /**
     * Get available triggers
     */
    fun getAvailableTriggers(): List<AutomationTrigger>
    
    /**
     * Register a trigger
     */
    suspend fun registerTrigger(trigger: AutomationTrigger, config: TriggerConfig): Result<String>
    
    /**
     * Unregister a trigger
     */
    suspend fun unregisterTrigger(triggerId: String): Result<Unit>
    
    /**
     * Get active triggers
     */
    suspend fun getActiveTriggers(): List<ActiveTrigger>
    
    /**
     * Execute automation action
     */
    suspend fun executeAction(action: AutomationAction): Result<Unit>
    
    /**
     * Observe trigger events
     */
    fun observeTriggerEvents(): Flow<TriggerEvent>
}

data class AutomationTrigger(
    val id: String,
    val name: String,
    val description: String,
    val configSchema: Map<String, TriggerConfigField>
)

data class TriggerConfigField(
    val key: String,
    val type: ConfigFieldType,
    val label: String,
    val required: Boolean = false,
    val defaultValue: Any? = null,
    val options: List<String> = emptyList()
)

enum class ConfigFieldType {
    STRING, NUMBER, BOOLEAN, SELECT, MULTI_SELECT
}

data class TriggerConfig(
    val values: Map<String, Any>
)

data class ActiveTrigger(
    val id: String,
    val triggerId: String,
    val config: TriggerConfig,
    val enabled: Boolean = true
)

data class AutomationAction(
    val type: ActionType,
    val parameters: Map<String, Any>
)

enum class ActionType {
    BACKUP_APPS, RESTORE_SNAPSHOT, VERIFY_INTEGRITY, SYNC_TO_CLOUD
}

data class TriggerEvent(
    val triggerId: String,
    val timestamp: Long,
    val data: Map<String, Any>
)
```

#### ExportPlugin.kt
```kotlin
package com.obsidianbackup.plugins.interfaces

import com.obsidianbackup.model.*
import com.obsidianbackup.plugins.api.PluginMetadata
import kotlinx.coroutines.flow.Flow

interface ExportPlugin {
    
    val metadata: PluginMetadata
    
    /**
     * Get supported export formats
     */
    fun getSupportedFormats(): List<ExportFormat>
    
    /**
     * Export snapshot data
     */
    suspend fun exportSnapshot(
        snapshotId: SnapshotId,
        format: ExportFormat,
        destination: File
    ): ExportResult
    
    /**
     * Export catalog data
     */
    suspend fun exportCatalog(
        format: ExportFormat,
        destination: File,
        filter: CatalogFilter? = null
    ): ExportResult
    
    /**
     * Stream export for large datasets
     */
    fun exportStream(
        format: ExportFormat,
        filter: CatalogFilter? = null
    ): Flow<ExportChunk>
    
    /**
     * Validate export configuration
     */
    suspend fun validateConfig(config: ExportConfig): ValidationResult
}

data class ExportFormat(
    val id: String,
    val name: String,
    val description: String,
    val fileExtension: String,
    val supportsStreaming: Boolean = false,
    val compressionSupported: Boolean = true
)

data class ExportConfig(
    val format: ExportFormat,
    val includeMetadata: Boolean = true,
    val includeChecksums: Boolean = true,
    val compressionLevel: Int = 6,
    val customOptions: Map<String, Any> = emptyMap()
)

data class CatalogFilter(
    val dateRange: DateRange? = null,
    val appIds: Set<AppId>? = null,
    val snapshotIds: Set<SnapshotId>? = null,
    val minSize: Long? = null,
    val maxSize: Long? = null
)

data class DateRange(
    val startDate: Long,
    val endDate: Long
)

data class ExportResult(
    val success: Boolean,
    val exportedFile: File? = null,
    val recordCount: Int = 0,
    val totalSize: Long = 0,
    val error: String? = null
)

data class ExportChunk(
    val data: ByteArray,
    val sequenceNumber: Int,
    val isLast: Boolean,
    val checksum: String
)

data class ValidationResult(
    val valid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)
```

### Plugin Discovery Flow

```kotlin
// PluginManager.kt
class PluginManager(
    private val context: Context,
    private val pluginRegistry: PluginRegistry,
    private val pluginLoader: PluginLoader,
    private val pluginValidator: PluginValidator
) {
    
    suspend fun discoverPlugins(): List<PluginMetadata> {
        val discovered = mutableListOf<PluginMetadata>()
        
        // 1. Manifest-based discovery
        val manifestPlugins = ManifestPluginDiscovery(context).discover()
        discovered.addAll(manifestPlugins)
        
        // 2. Package-based discovery (signed plugins)
        val packagePlugins = PackagePluginDiscovery(context).discover()
        discovered.addAll(packagePlugins)
        
        // 3. Validate and register
        val validPlugins = discovered.filter { plugin ->
            pluginValidator.validate(plugin).also { result ->
                if (!result.valid) {
                    Log.w(TAG, "Invalid plugin ${plugin.packageName}: ${result.errors}")
                }
            }.valid
        }
        
        pluginRegistry.registerPlugins(validPlugins)
        return validPlugins
    }
    
    suspend fun <T : Any> loadPlugin(metadata: PluginMetadata): Result<T> {
        return try {
            val plugin = pluginLoader.loadPlugin(metadata) as T
            pluginRegistry.markPluginLoaded(metadata.packageName)
            Result.Success(plugin)
        } catch (e: Exception) {
            Result.Error(PluginException.PluginLoadFailed(metadata.packageName, e))
        }
    }
    
    fun getAvailablePlugins(type: PluginType): List<PluginMetadata> {
        return pluginRegistry.getPluginsByType(type)
    }
}

enum class PluginType {
    BACKUP_ENGINE, CLOUD_PROVIDER, AUTOMATION, EXPORT
}
```

## 2. Cloud Sync Layer Architecture

### Core Abstractions

```kotlin
// CloudProvider.kt
interface CloudProvider {
    
    val providerId: String
    val displayName: String
    val capabilities: CloudCapabilities
    
    suspend fun initialize(config: CloudConfig): Result<Unit>
    suspend fun testConnection(): Result<Unit>
    
    suspend fun uploadFile(
        localFile: File,
        remotePath: String,
        metadata: Map<String, String> = emptyMap()
    ): CloudResult
    
    suspend fun downloadFile(
        remotePath: String,
        localFile: File
    ): CloudResult
    
    suspend fun listFiles(prefix: String = ""): List<CloudFile>
    suspend fun deleteFile(remotePath: String): CloudResult
    
    suspend fun getFileMetadata(remotePath: String): CloudFile?
    
    fun observeTransferProgress(): Flow<TransferProgress>
}

data class CloudFile(
    val path: String,
    val size: Long,
    val lastModified: Long,
    val checksum: String? = null,
    val metadata: Map<String, String> = emptyMap()
)
```

### CloudSyncManager

```kotlin
// CloudSyncManager.kt
class CloudSyncManager(
    private val context: Context,
    private val backupCatalog: BackupCatalog,
    private val cloudProvider: CloudProvider,
    private val workManager: WorkManager,
    private val logger: TitanLogger
) {
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    suspend fun syncSnapshot(snapshotId: SnapshotId, policy: SyncPolicy): Result<Unit> {
        _syncState.value = SyncState.Syncing(snapshotId)
        
        return try {
            // 1. Get snapshot metadata
            val metadata = backupCatalog.getSnapshot(snapshotId)
                ?: return Result.Error(SyncError.SnapshotNotFound(snapshotId))
            
            // 2. Check if already synced
            if (isSnapshotSynced(snapshotId)) {
                logger.i(TAG, "Snapshot $snapshotId already synced")
                _syncState.value = SyncState.Idle
                return Result.Success(Unit)
            }
            
            // 3. Upload snapshot archive
            val archiveFile = getSnapshotArchiveFile(snapshotId)
            val remotePath = "snapshots/${snapshotId.value}.tar.zst"
            
            val uploadResult = cloudProvider.uploadFile(
                localFile = archiveFile,
                remotePath = remotePath,
                metadata = mapOf(
                    "snapshotId" to snapshotId.value,
                    "timestamp" to metadata.timestamp.toString(),
                    "size" to metadata.totalSize.toString()
                )
            )
            
            if (!uploadResult.success) {
                return Result.Error(SyncError.UploadFailed(uploadResult.error ?: "Unknown error"))
            }
            
            // 4. Upload signed catalog
            val catalogResult = uploadCatalog()
            if (catalogResult is Result.Error) {
                return catalogResult
            }
            
            // 5. Mark as synced
            markSnapshotSynced(snapshotId)
            
            _syncState.value = SyncState.Idle
            logger.i(TAG, "Successfully synced snapshot $snapshotId")
            Result.Success(Unit)
            
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            Result.Error(SyncError.UnexpectedError(e))
        }
    }
    
    suspend fun restoreFromCloud(snapshotId: SnapshotId): Result<File> {
        _syncState.value = SyncState.Downloading(snapshotId)
        
        return try {
            val remotePath = "snapshots/${snapshotId.value}.tar.zst"
            val localFile = File(context.cacheDir, "${snapshotId.value}.tar.zst")
            
            val downloadResult = cloudProvider.downloadFile(remotePath, localFile)
            
            if (!downloadResult.success) {
                _syncState.value = SyncState.Idle
                return Result.Error(SyncError.DownloadFailed(downloadResult.error ?: "Unknown error"))
            }
            
            _syncState.value = SyncState.Idle
            Result.Success(localFile)
            
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            Result.Error(SyncError.UnexpectedError(e))
        }
    }
    
    private suspend fun uploadCatalog(): Result<Unit> {
        val catalogFile = exportSignedCatalog()
        val uploadResult = cloudProvider.uploadFile(
            localFile = catalogFile,
            remotePath = "catalog.json",
            metadata = mapOf("type" to "catalog", "version" to "1.0")
        )
        
        return if (uploadResult.success) {
            Result.Success(Unit)
        } else {
            Result.Error(SyncError.CatalogUploadFailed(uploadResult.error ?: "Unknown error"))
        }
    }
    
    private suspend fun isSnapshotSynced(snapshotId: SnapshotId): Boolean {
        // Check local sync status
        return getSyncStatus(snapshotId)?.synced == true
    }
    
    private suspend fun markSnapshotSynced(snapshotId: SnapshotId) {
        // Update local sync status
        updateSyncStatus(snapshotId, synced = true, lastSync = System.currentTimeMillis())
    }
    
    private fun getSnapshotArchiveFile(snapshotId: SnapshotId): File {
        return File(context.getExternalFilesDir("backups"), "${snapshotId.value}.tar.zst")
    }
    
    private suspend fun exportSignedCatalog(): File {
        // Export catalog with integrity signatures
        return File(context.cacheDir, "catalog.json")
    }
}

sealed class SyncState {
    object Idle : SyncState()
    data class Syncing(val snapshotId: SnapshotId) : SyncState()
    data class Downloading(val snapshotId: SnapshotId) : SyncState()
    data class Error(val message: String) : SyncState()
}

sealed class SyncError : Exception() {
    data class SnapshotNotFound(val snapshotId: SnapshotId) : SyncError()
    data class UploadFailed(val reason: String) : SyncError()
    data class DownloadFailed(val reason: String) : SyncError()
    data class CatalogUploadFailed(val reason: String) : SyncError()
    data class UnexpectedError(val cause: Throwable) : SyncError()
}

data class SyncPolicy(
    val syncOnBackup: Boolean = true,
    val syncOnWifiOnly: Boolean = true,
    val syncOnCharging: Boolean = false,
    val maxConcurrentSyncs: Int = 1,
    val retryPolicy: RetryPolicy = RetryPolicy()
)

data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMs: Long = 1000,
    val maxDelayMs: Long = 30000,
    val backoffMultiplier: Double = 2.0
)
```

### Cloud Plugin Integration

```kotlin
// CloudProviderPlugin.kt (extends the plugin interface)
class CloudProviderPluginImpl : CloudProviderPlugin {
    
    override val metadata = PluginMetadata(
        packageName = "com.example.cloudprovider",
        className = "CloudProviderPluginImpl",
        name = "Example Cloud Provider",
        description = "Cloud storage provider plugin",
        version = "1.0.0",
        apiVersion = PluginApiVersion.CURRENT,
        capabilities = setOf(PluginCapability.ClientSideEncryption),
        author = "Example Corp"
    )
    
    private var cloudProvider: CloudProvider? = null
    
    override suspend fun initialize(config: CloudConfig): Result<Unit> {
        return try {
            cloudProvider = when (config.providerId) {
                "s3" -> S3CloudProvider(config)
                "webdav" -> WebDavCloudProvider(config)
                else -> return Result.Error(PluginException.UnsupportedProvider(config.providerId))
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(PluginException.InitializationFailed(e))
        }
    }
    
    override suspend fun testConnection(): CloudResult {
        return cloudProvider?.testConnection() ?: CloudResult(success = false, error = "Not initialized")
    }
    
    // Delegate other methods to cloudProvider...
}
```

### Sync Flow Diagrams

**Sync After Backup Flow:**
```
User initiates backup
    ↓
BackupEngine.backupApps() → BackupResult.Success
    ↓
CloudSyncManager.syncSnapshot(snapshotId, policy)
    ↓
Check sync policy (WiFi, charging, etc.)
    ↓
Upload snapshot archive to cloud
    ↓
Upload signed catalog
    ↓
Mark snapshot as synced locally
    ↓
Notify user of successful sync
```

**Restore from Cloud Flow:**
```
User selects cloud snapshot
    ↓
CloudSyncManager.restoreFromCloud(snapshotId)
    ↓
Download snapshot archive from cloud
    ↓
Verify integrity (optional)
    ↓
Pass archive to RestoreEngine
    ↓
Normal transactional restore flow
```

## 3. Device-to-Device Migration Flow

### Protocol Design

**Migration Protocol Overview:**
```
Source Device (Server)          Target Device (Client)
     │                               │
     │ 1. Start Migration Server     │
     │    - Generate ephemeral key   │
     │    - Start WiFi Direct        │
     │    - Advertise via mDNS       │
     │                               │
     │ 2. Display QR Code           │
     │    - Contains: server IP,     │
     │      port, public key         │
     │                               │
     │                               │ 3. Scan QR Code
     │                               │    - Extract connection info
     │                               │    - Connect to server
     │                               │
     │ 4. Receive connection         │
     │    - Verify client key        │
     │    - Establish encrypted      │
     │      channel                  │
     │                               │
     │ 5. Capability exchange        │
     │    - Android versions         │
     │    - Permission modes         │
     │    - Available snapshots      │
     │                               │
     │ 6. User selection             │
     │    - Client sends selection   │
     │                               │
     │ 7. Transfer catalog           │
     │    - Send selected snapshots  │
     │    - Integrity verification   │
     │                               │
     │ 8. Restore on target          │
     │    - Use normal restore       │
     │      pipeline                 │
```

### Kotlin Components

```kotlin
// MigrationServer.kt
class MigrationServer(
    private val context: Context,
    private val backupCatalog: BackupCatalog,
    private val encryptionEngine: EncryptionEngine,
    private val logger: TitanLogger
) {
    
    private val serverSocket = ServerSocket(0) // Auto-assign port
    private val ephemeralKeyPair = generateEphemeralKeyPair()
    private var wifiDirectManager: WifiP2pManager? = null
    
    suspend fun startMigration(): MigrationSession {
        val session = MigrationSession(
            serverPort = serverSocket.localPort,
            serverKey = ephemeralKeyPair.public,
            sessionId = UUID.randomUUID().toString()
        )
        
        // Start WiFi Direct
        setupWifiDirect()
        
        // Start mDNS advertisement
        advertiseService(session)
        
        // Start server coroutine
        coroutineScope {
            launch(Dispatchers.IO) {
                acceptConnections(session)
            }
        }
        
        return session
    }
    
    private suspend fun acceptConnections(session: MigrationSession) {
        while (isActive) {
            try {
                val clientSocket = serverSocket.accept()
                launch {
                    handleClient(clientSocket, session)
                }
            } catch (e: Exception) {
                logger.e(TAG, "Error accepting connection", e)
            }
        }
    }
    
    private suspend fun handleClient(socket: Socket, session: MigrationSession) {
        socket.use { clientSocket ->
            val input = DataInputStream(clientSocket.getInputStream())
            val output = DataOutputStream(clientSocket.getOutputStream())
            
            try {
                // 1. Key exchange
                performKeyExchange(input, output, session)
                
                // 2. Capability exchange
                exchangeCapabilities(input, output)
                
                // 3. Handle requests
                handleMigrationRequests(input, output)
                
            } catch (e: Exception) {
                logger.e(TAG, "Error handling client", e)
            }
        }
    }
    
    private suspend fun performKeyExchange(
        input: DataInputStream,
        output: DataOutputStream,
        session: MigrationSession
    ) {
        // Send server public key
        val serverKeyBytes = ephemeralKeyPair.public.encoded
        output.writeInt(serverKeyBytes.size)
        output.write(serverKeyBytes)
        
        // Receive client public key
        val clientKeySize = input.readInt()
        val clientKeyBytes = ByteArray(clientKeySize)
        input.readFully(clientKeyBytes)
        
        // Establish shared secret
        session.clientKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(clientKeyBytes))
        session.sharedSecret = deriveSharedSecret(ephemeralKeyPair.private, session.clientKey)
    }
    
    private suspend fun exchangeCapabilities(
        input: DataInputStream,
        output: DataOutputStream
    ) {
        val serverCapabilities = MigrationCapabilities(
            androidVersion = android.os.Build.VERSION.SDK_INT,
            titanBackupVersion = "1.0.0",
            permissionMode = PermissionMode.SAF, // Current mode
            availableSnapshots = backupCatalog.getAllSnapshots().first().map { it.id }
        )
        
        // Send server capabilities
        val capabilitiesJson = Json.encodeToString(serverCapabilities)
        val capabilitiesBytes = capabilitiesJson.toByteArray()
        output.writeInt(capabilitiesBytes.size)
        output.write(capabilitiesBytes)
        
        // Receive client capabilities
        val clientCapabilitiesSize = input.readInt()
        val clientCapabilitiesBytes = ByteArray(clientCapabilitiesSize)
        input.readFully(clientCapabilitiesBytes)
        
        val clientCapabilities = Json.decodeFromString<MigrationCapabilities>(String(clientCapabilitiesBytes))
        // Store for later use
    }
}

// MigrationClient.kt
class MigrationClient(
    private val context: Context,
    private val logger: TitanLogger
) {
    
    private val ephemeralKeyPair = generateEphemeralKeyPair()
    
    suspend fun connectToServer(qrData: QrConnectionData): MigrationSession {
        val socket = Socket(qrData.serverIp, qrData.serverPort)
        
        return socket.use { serverSocket ->
            val input = DataInputStream(serverSocket.getInputStream())
            val output = DataOutputStream(serverSocket.getOutputStream())
            
            val session = MigrationSession(
                serverIp = qrData.serverIp,
                serverPort = qrData.serverPort,
                clientKey = ephemeralKeyPair.public,
                sessionId = UUID.randomUUID().toString()
            )
            
            // Perform key exchange
            performKeyExchange(input, output, session, qrData.serverKey)
            
            // Exchange capabilities
            exchangeCapabilities(input, output, session)
            
            session
        }
    }
    
    suspend fun requestSnapshotList(session: MigrationSession): List<SnapshotId> {
        // Send request for snapshot list
        // Receive and return list
        return emptyList()
    }
    
    suspend fun downloadSnapshot(
        session: MigrationSession,
        snapshotId: SnapshotId,
        destination: File
    ): Result<Unit> {
        // Request snapshot download
        // Stream data with integrity checks
        return Result.Success(Unit)
    }
}

// Data Models
data class MigrationSession(
    val sessionId: String,
    val serverIp: String? = null,
    val serverPort: Int? = null,
    val serverKey: PublicKey? = null,
    var clientKey: PublicKey? = null,
    var sharedSecret: ByteArray? = null,
    val startTime: Long = System.currentTimeMillis()
)

data class MigrationCapabilities(
    val androidVersion: Int,
    val titanBackupVersion: String,
    val permissionMode: PermissionMode,
    val availableSnapshots: List<SnapshotId>
)

data class QrConnectionData(
    val serverIp: String,
    val serverPort: Int,
    val serverKey: PublicKey
)
```

### Integration with Backup/Restore Engine

```kotlin
// MigrationRestoreManager.kt
class MigrationRestoreManager(
    private val restoreEngine: TransactionalRestoreEngine,
    private val migrationClient: MigrationClient,
    private val logger: TitanLogger
) {
    
    suspend fun performMigrationRestore(
        session: MigrationSession,
        selectedSnapshots: List<SnapshotId>
    ): Result<Unit> {
        return try {
            for (snapshotId in selectedSnapshots) {
                // 1. Download snapshot from source
                val tempFile = File(context.cacheDir, "${snapshotId.value}.tar.zst")
                val downloadResult = migrationClient.downloadSnapshot(session, snapshotId, tempFile)
                
                if (downloadResult is Result.Error) {
                    logger.e(TAG, "Failed to download snapshot $snapshotId", downloadResult.error)
                    continue
                }
                
                // 2. Restore using normal pipeline
                val restoreRequest = RestoreRequest(
                    snapshotId = snapshotId,
                    appIds = emptyList(), // Restore all apps
                    components = setOf(BackupComponent.APK, BackupComponent.DATA),
                    dryRun = false,
                    overwriteExisting = true
                )
                
                val restoreResult = restoreEngine.restoreApps(restoreRequest)
                
                when (restoreResult) {
                    is RestoreResult.Success -> {
                        logger.i(TAG, "Successfully restored snapshot $snapshotId")
                    }
                    is RestoreResult.Failure -> {
                        logger.e(TAG, "Failed to restore snapshot $snapshotId", restoreResult.error)
                        // Continue with other snapshots
                    }
                    is RestoreResult.PartialSuccess -> {
                        logger.w(TAG, "Partially restored snapshot $snapshotId")
                    }
                }
                
                // 3. Cleanup temp file
                tempFile.delete()
            }
            
            Result.Success(Unit)
            
        } catch (e: Exception) {
            logger.e(TAG, "Migration restore failed", e)
            Result.Error(MigrationError.RestoreFailed(e))
        }
    }
}
```

## 4. Enterprise API Definition

### AIDL/IPC Interface

```kotlin
// IObsidianBackupService.aidl
package com.obsidianbackup.api;

import com.obsidianbackup.api.BackupRequest;
import com.obsidianbackup.api.RestoreRequest;
import com.obsidianbackup.api.ApiResult;
import com.obsidianbackup.api.SnapshotInfo;
import com.obsidianbackup.api.LogEntry;

interface IObsidianBackupService {
    
    // Status and information
    ApiResult getStatus();
    List<SnapshotInfo> getSnapshots();
    List<LogEntry> getLogs(long sinceTimestamp);
    
    // Backup operations
    ApiResult startBackup(in BackupRequest request);
    ApiResult cancelBackup(String operationId);
    
    // Restore operations
    ApiResult startRestore(in RestoreRequest request);
    ApiResult cancelRestore(String operationId);
    
    // Verification
    ApiResult verifySnapshot(String snapshotId);
    
    // Configuration
    ApiResult setPolicy(in BackupPolicy policy);
    ApiResult getPolicy();
    
    // Enterprise features
    ApiResult createProfile(in ProfileConfig config);
    ApiResult switchProfile(String profileId);
    List<String> getProfiles();
}

// BackupRequest.aidl
parcelable BackupRequest {
    List<String> appIds;
    List<String> components;
    boolean incremental = false;
    int compressionLevel = 6;
    boolean encryptionEnabled = false;
    String description;
    String profileId;
}

// ApiResult.aidl
parcelable ApiResult {
    boolean success;
    String operationId;
    String errorMessage;
    Bundle metadata;
}
```

### REST API Shape

```kotlin
// EnterpriseApiService.kt
class EnterpriseApiService(
    private val backupOrchestrator: BackupOrchestrator,
    private val authManager: AuthManager,
    private val profileManager: ProfileManager
) {
    
    // Status endpoint
    @GET("/api/v1/status")
    suspend fun getStatus(@Header("Authorization") token: String): StatusResponse {
        authManager.validateToken(token)
        
        val status = backupOrchestrator.getStatus()
        return StatusResponse(
            version = BuildConfig.VERSION_NAME,
            uptime = System.currentTimeMillis() - startTime,
            activeOperations = status.activeOperations,
            lastBackup = status.lastBackupTimestamp,
            storageUsed = status.storageUsed
        )
    }
    
    // Backup endpoint
    @POST("/api/v1/backup")
    suspend fun startBackup(
        @Header("Authorization") token: String,
        @Body request: BackupRequestDto
    ): OperationResponse {
        authManager.validateToken(token)
        
        val operationId = backupOrchestrator.startBackup(request.toDomain())
        return OperationResponse(operationId = operationId)
    }
    
    // Snapshots endpoint
    @GET("/api/v1/snapshots")
    suspend fun getSnapshots(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): List<SnapshotDto> {
        authManager.validateToken(token)
        
        return backupCatalog.getSnapshots(limit, offset)
            .map { it.toDto() }
    }
    
    // Logs endpoint
    @GET("/api/v1/logs")
    suspend fun getLogs(
        @Header("Authorization") token: String,
        @Query("since") since: Long? = null,
        @Query("level") level: String? = null,
        @Query("limit") limit: Int = 100
    ): List<LogEntryDto> {
        authManager.validateToken(token)
        
        return logRepository.getLogs(
            since = since,
            minLevel = level?.let { LogLevel.valueOf(it) },
            limit = limit
        ).map { it.toDto() }
    }
    
    // Policy management
    @PUT("/api/v1/policy")
    suspend fun setPolicy(
        @Header("Authorization") token: String,
        @Body policy: BackupPolicyDto
    ): SuccessResponse {
        authManager.requireAdmin(token)
        
        profileManager.setPolicy(policy.toDomain())
        return SuccessResponse()
    }
}

// Data Transfer Objects
data class StatusResponse(
    val version: String,
    val uptime: Long,
    val activeOperations: Int,
    val lastBackup: Long?,
    val storageUsed: Long
)

data class BackupRequestDto(
    val appIds: List<String>,
    val components: List<String>,
    val incremental: Boolean = false,
    val compressionLevel: Int = 6,
    val encryptionEnabled: Boolean = false,
    val description: String? = null,
    val profileId: String? = null
) {
    fun toDomain() = BackupRequest(
        appIds = appIds.map { AppId(it) },
        components = components.map { BackupComponent.valueOf(it) }.toSet(),
        incremental = incremental,
        compressionLevel = compressionLevel,
        encryptionEnabled = encryptionEnabled,
        description = description
    )
}

data class OperationResponse(
    val operationId: String,
    val status: String = "accepted"
)

data class SuccessResponse(
    val success: Boolean = true
)
```

### Authentication Model

```kotlin
// AuthManager.kt
class AuthManager(
    private val context: Context,
    private val keyStore: KeyStore
) {
    
    suspend fun validateToken(token: String): UserContext {
        // Decode and verify JWT token
        val claims = verifyJwtToken(token)
        
        return UserContext(
            userId = claims["userId"] as String,
            profileId = claims["profileId"] as String,
            permissions = (claims["permissions"] as List<String>).map { Permission.valueOf(it) }
        )
    }
    
    suspend fun requireAdmin(token: String) {
        val context = validateToken(token)
        if (!context.permissions.contains(Permission.ADMIN)) {
            throw SecurityException("Admin permission required")
        }
    }
    
    private suspend fun verifyJwtToken(token: String): Map<String, Any> {
        // JWT verification with Android KeyStore key
        return jwtVerifier.verify(token)
    }
}

data class UserContext(
    val userId: String,
    val profileId: String,
    val permissions: List<Permission>
)

enum class Permission {
    READ, BACKUP, RESTORE, VERIFY, ADMIN
}
```

### Enterprise Mode Activation

```kotlin
// EnterpriseModeManager.kt
class EnterpriseModeManager(
    private val context: Context,
    private val devicePolicyManager: DevicePolicyManager
) {
    
    fun isEnterpriseModeAvailable(): Boolean {
        // Check if device is managed by EMM
        val deviceAdmin = ComponentName(context, DeviceAdminReceiver::class.java)
        return devicePolicyManager.isAdminActive(deviceAdmin) ||
               devicePolicyManager.isDeviceOwnerApp(context.packageName) ||
               devicePolicyManager.isProfileOwnerApp(context.packageName)
    }
    
    fun enableEnterpriseMode(): Result<Unit> {
        return try {
            // Enable enterprise features
            // - API endpoints
            // - Profile management
            // - Enhanced logging
            // - Remote management
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(EnterpriseError.ActivationFailed(e))
        }
    }
    
    fun getEnterpriseCapabilities(): EnterpriseCapabilities {
        return EnterpriseCapabilities(
            apiEnabled = true,
            profilesEnabled = true,
            remoteManagement = true,
            enhancedLogging = true
        )
    }
}

data class EnterpriseCapabilities(
    val apiEnabled: Boolean,
    val profilesEnabled: Boolean,
    val remoteManagement: Boolean,
    val enhancedLogging: Boolean
)
```

## 5. UX Polish Roadmap (Structure Only)

### Phase 1: Foundational Polish
- **Typography System**: Consistent font scales, weights, and spacing
- **Color Palette**: Semantic color system (success, warning, error, info)
- **Component Library**: Reusable composables for common patterns
- **Status Language**: "Protected", "At risk", "Degraded mode", "Needs attention"

### Phase 2: Guided Flows
- **First-Run Experience**: Progressive onboarding with permission explanations
- **Post-Action Screens**: Clear summaries of backup/restore operations
- **Error Recovery**: Guided troubleshooting with actionable steps

### Phase 3: Power-User Affordances
- **Advanced Settings**: Progressive disclosure of complex options
- **Keyboard Shortcuts**: Efficiency features for frequent users
- **Saved Configurations**: Named profiles for different use cases

### Screen Map
```
Main Screens:
├── Dashboard (overview + quick actions)
├── Apps (selection + batch operations)
├── Backups (history + management)
├── Automation (schedules + triggers)
├── Logs (transparency + export)
└── Settings (configuration + advanced)

Modal Flows:
├── Permission Setup Wizard
├── Backup Configuration
├── Restore Confirmation
├── Error Recovery Guide
└── First-Run Onboarding
```

## 6. Long-term Product Vision (Structure Only)

### Concise Product Vision Statement
"ObsidianBackup: The reference standard for user-owned, transparent Android backup—empowering users with enterprise-grade reliability and complete control over their data."

### 3-Year Milestones
- **Year 1**: Establish as go-to tool for Android modders and power users
- **Year 2**: Plugin ecosystem with community-contributed engines and providers
- **Year 3**: Recognized industry standard for backup security and transparency

### 5-Year Milestones
- **Year 4**: Desktop companion applications for cross-platform management
- **Year 5**: Self-hosted server option for families and small organizations

### Supporting Features
- **Plugin Ecosystem**: Enables community innovation and third-party integrations
- **Cloud Agnostic Sync**: rclone-powered multi-provider support
- **Enterprise API**: Enables integration with management systems
- **Open Formats**: Ensures long-term accessibility of backed-up data

## 7. Launch Strategy (Structure Only)

### Phase 0: Private Alpha
- Internal dogfooding and iteration
- Core engine reliability validation
- UX clarity and error handling refinement

### Phase 1: Closed Technical Beta
- Target: XDA, Android modding communities
- Positioning: "Enterprise-grade backup for people who care about their data"
- Focus: Technical feedback, edge case handling, device compatibility

### Phase 2: Public Paid Launch
- One-time Pro pricing model
- Clear differentiation from Titanium/Swift Backup
- Emphasis: Open formats, verification, transparency, no tracking

### Phase 3: Thought Leadership
- Technical blog posts and whitepapers
- Conference talks on backup security
- Industry recognition as security standard

### Messaging Pillars
1. **Security First**: Hardware-backed encryption, audited operations
2. **User Control**: Open formats, no vendor lock-in
3. **Enterprise Reliability**: Transactional operations, comprehensive verification
4. **Transparency**: Full audit trails, clear status communication

### Milestone Checklist
- [ ] Alpha completion with <5% crash rate
- [ ] Beta with 100+ active users and comprehensive feedback
- [ ] Launch with 4.8+ star rating and positive reviews
- [ ] Post-launch: 50+ beta users convert to paid

## 8. Technical Whitepaper Outline

### Refined Outline

1. **Abstract**
   - Problem statement and solution overview

2. **Introduction**
   - Android backup landscape
   - Legacy tool limitations
   - ObsidianBackup positioning

3. **Design Principles**
   - Open formats and portability
   - Verifiable integrity
   - Hybrid permission model
   - Transactional safety

4. **Architecture Overview**
   - Layered design (presentation → domain → data → infrastructure)
   - Engine abstraction and plugin system
   - Catalog and verification pipeline

5. **Security Model**
   - AES-256-GCM with Android KeyStore
   - Command execution auditing
   - SELinux context handling
   - Threat model and controls

6. **Reliability Model**
   - Transactional restore operations
   - Failure modes and automatic recovery
   - Comprehensive error handling

7. **Extensibility**
   - Plugin ecosystem architecture
   - Cloud provider abstraction
   - Enterprise API integration

8. **Implementation Details**
   - BusyBox tooling integration
   - Incremental backup with rsync
   - Merkle tree verification
   - Audit logging system

9. **Performance Characteristics**
   - Benchmark results
   - Memory and CPU usage
   - Storage efficiency

10. **Future Directions**
    - Cross-device migration
    - Cross-platform companions
    - Ecosystem integrations

### Key Diagrams to Include
- System architecture overview
- Backup/restore flow with verification
- Plugin system architecture
- Security control layers
- Performance benchmark charts

### Mapping to Code Architecture
- Reference specific classes and packages
- Link to implementation details
- Provide code examples for key concepts

## 9. Security Model Documentation

### Threat Model

**Attacker Types:**
- **Local Attacker**: Physical access to device
- **App-Level Attacker**: Malicious apps on same device
- **Root Attacker**: Compromised system with root access
- **Remote Attacker**: Network-based attacks
- **Supply Chain Attacker**: Compromised dependencies or updates

**Assets to Protect:**
- **Backup Archives**: User data and application states
- **Encryption Keys**: Stored in Android KeyStore
- **Catalog Database**: Metadata and integrity information
- **Audit Logs**: Security event history
- **Configuration**: User preferences and settings

### Security Controls

**Cryptographic Controls:**
- AES-256-GCM encryption with Android KeyStore
- PBKDF2 key derivation for passphrase-based encryption
- SHA256 checksums for integrity verification
- Merkle trees for large dataset verification

**Access Controls:**
- Android KeyStore for hardware-backed key storage
- Strict permission model (Root → Shizuku → ADB → SAF)
- Command validation and sandboxing
- Enterprise API with authentication

**Integrity Controls:**
- Transactional operations with rollback
- File integrity verification before restore
- Catalog integrity with cryptographic signatures
- SELinux context validation and restoration

**Audit Controls:**
- Complete command execution logging
- Security event monitoring
- Tamper-evident audit trails
- Exportable security logs

### Hardening Guidelines

**Recommended Device Settings:**
- Enable Android KeyStore (automatic)
- Use strong device lock (PIN/biometric)
- Enable SELinux enforcing mode
- Keep system updated

**Paranoid Mode Usage:**
- Disable automatic cloud sync
- Require manual verification before restore
- Enable full audit logging
- Use passphrase-based encryption only

**Backup Verification:**
- Independent verification scripts
- Cross-check checksums manually
- Validate Merkle tree roots
- Test restore in safe environment

### Security Model Doc Outline

1. **Threat Model**
   - Attacker types and capabilities
   - Asset valuation and protection priorities

2. **Security Architecture**
   - Defense in depth layers
   - Cryptographic foundations
   - Access control mechanisms

3. **Implementation Security**
   - Code-level security controls
   - Dependency security
   - Build security

4. **Operational Security**
   - User hardening guidelines
   - Incident response procedures
   - Security monitoring

### Ties to Engine and Plugin Architecture

**Engine Layer Security:**
- SafeShellExecutor validates all commands
- AuditLogger captures all shell operations
- EncryptionEngine isolates cryptographic operations

**Plugin System Security:**
- PluginCapability declarations limit plugin access
- PluginValidator ensures plugin integrity
- Sandboxing prevents plugin escape

**Data Layer Security:**
- Room database with encrypted sensitive data
- File system encryption for archives
- Secure deletion of temporary files

## 10. Developer Integration Guide

### Proposed Structure for DEVELOPER_GUIDE.md

1. **Getting Started**
   - ObsidianBackup concepts overview
   - Development environment setup
   - API versioning and compatibility

2. **Core Concepts**
   - Snapshots, catalogs, and manifests
   - Engine abstraction and capabilities
   - Permission modes and their implications
   - Backup components and restore targets

3. **Local Integration**
   - Android Intents for basic operations
   - Bound service with AIDL interface
   - Content provider for data access
   - Broadcast receivers for status updates

4. **Enterprise Integration**
   - REST API endpoints and authentication
   - Profile management for multi-tenant scenarios
   - Remote management and monitoring
   - Bulk operations and automation

5. **Plugin Development**
   - Plugin API structure and lifecycle
   - Developing a CloudProviderPlugin
   - Creating a BackupEnginePlugin
   - Testing and validation procedures

6. **Integration Patterns**
   - Backup before system updates
   - Restore after ROM flashes
   - Automated backup scheduling
   - Log aggregation and monitoring

7. **Best Practices**
   - Error handling and recovery
   - Performance considerations
   - Security guidelines
   - Testing strategies

8. **Troubleshooting**
   - Common integration issues
   - Debug logging and diagnostics
   - Support channels and resources

### Example Code Snippets

**Basic Intent Integration:**
```kotlin
// Trigger backup of specific apps
val intent = Intent("com.obsidianbackup.action.BACKUP").apply {
    putStringArrayListExtra("appIds", arrayListOf("com.example.app1", "com.example.app2"))
    putStringArrayListExtra("components", arrayListOf("APK", "DATA"))
}
context.startActivity(intent)
```

**AIDL Service Binding:**
```kotlin
// Bind to ObsidianBackup service
val intent = Intent("com.obsidianbackup.api.BIND_SERVICE")
val connection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val api = IObsidianBackupService.Stub.asInterface(service)
        // Use API...
    }
}
context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
```

**Plugin Development Example:**
```kotlin
class MyCloudProvider : CloudProviderPlugin {
    override val metadata = PluginMetadata(
        packageName = "com.example.myprovider",
        // ... other metadata
    )
    
    override suspend fun uploadFile(localFile: File, remotePath: String): CloudResult {
        // Implementation...
        return CloudResult(success = true)
    }
    
    // ... other methods
}
```

### Recommended Integration Patterns

1. **Pre-Update Backup**: Integrate with ROM flashers and system updaters
2. **Post-Install Restore**: Automate restore after device setup
3. **Scheduled Backups**: Use WorkManager for reliable scheduling
4. **Health Monitoring**: Aggregate logs for system health monitoring
5. **Compliance Automation**: Enterprise backup policy enforcement

---

This specification provides a complete, implementation-ready architecture for ObsidianBackup with concrete interfaces, data models, flow diagrams, and integration points. The design emphasizes security, extensibility, and user control while maintaining enterprise-grade reliability. Each component is designed to be testable, maintainable, and evolvable for future requirements. 

The plugin ecosystem, cloud sync layer, migration flow, and enterprise API provide the foundation for ObsidianBackup to become the reference standard for Android backup platforms. The documentation structure ensures that developers can easily understand and integrate with the system. 

**Ready for implementation.** 🚀
