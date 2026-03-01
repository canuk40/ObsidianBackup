// sync/SyncthingManager.kt
package com.obsidianbackup.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.sync.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SyncthingManager - Native Syncthing integration for cross-device backup synchronization
 * 
 * Features:
 * - Auto-discovery of other devices
 * - Conflict resolution with manual reconciliation UI
 * - Real-time sync status display
 * - Folder sharing for backup directories
 * - Device pairing via QR code and device ID
 * - Network preferences (WiFi only, mobile data, always)
 */
@Singleton
class SyncthingManager @Inject constructor(
    private val context: Context,
    private val syncthingApi: SyncthingApiClient,
    private val conflictResolver: SyncthingConflictResolver,
    private val logger: ObsidianLogger
) {
    companion object {
        private const val TAG = "SyncthingManager"
        private const val DEFAULT_API_PORT = 8384
        private const val SYNC_CHECK_INTERVAL_MS = 5000L
        private const val DISCOVERY_TIMEOUT_MS = 30000L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Syncthing state flows
    private val _syncState = MutableStateFlow<SyncthingState>(SyncthingState.Disconnected)
    val syncState: StateFlow<SyncthingState> = _syncState.asStateFlow()
    
    private val _devices = MutableStateFlow<List<SyncthingDevice>>(emptyList())
    val devices: StateFlow<List<SyncthingDevice>> = _devices.asStateFlow()
    
    private val _folders = MutableStateFlow<List<SyncthingFolder>>(emptyList())
    val folders: StateFlow<List<SyncthingFolder>> = _folders.asStateFlow()
    
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    private val _conflicts = MutableStateFlow<List<SyncConflict>>(emptyList())
    val conflicts: StateFlow<List<SyncConflict>> = _conflicts.asStateFlow()
    
    private val _networkPreference = MutableStateFlow(NetworkPreference.WIFI_ONLY)
    val networkPreference: StateFlow<NetworkPreference> = _networkPreference.asStateFlow()

    private var syncMonitorJob: Job? = null
    private var discoveryJob: Job? = null

    /**
     * Initialize Syncthing service
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            logger.i(TAG, "Initializing Syncthing...")
            
            // Check if Syncthing binary is available
            if (!isSyncthingInstalled()) {
                logger.e(TAG, "Syncthing binary not found")
                return@withContext Result.failure(
                    SyncthingException("Syncthing not installed")
                )
            }
            
            // Start Syncthing service
            startSyncthingService()
            
            // Wait for API to become available
            waitForApiAvailable()
            
            // Get device ID and configuration
            val deviceId = syncthingApi.getDeviceId()
            logger.i(TAG, "Device ID: $deviceId")
            
            _syncState.value = SyncthingState.Connected(deviceId)
            
            // Load initial configuration
            loadConfiguration()
            
            // Start monitoring sync status
            startSyncMonitoring()
            
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to initialize Syncthing", e)
            _syncState.value = SyncthingState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * Shutdown Syncthing service
     */
    suspend fun shutdown() = withContext(Dispatchers.IO) {
        try {
            logger.i(TAG, "Shutting down Syncthing...")
            
            syncMonitorJob?.cancel()
            discoveryJob?.cancel()
            
            syncthingApi.shutdown()
            _syncState.value = SyncthingState.Disconnected
            
            logger.i(TAG, "Syncthing shutdown complete")
        } catch (e: Exception) {
            logger.e(TAG, "Error during shutdown", e)
        }
    }

    /**
     * Add a device for synchronization
     */
    suspend fun addDevice(
        deviceId: String,
        name: String,
        introducer: Boolean = false
    ): Result<SyncthingDevice> = withContext(Dispatchers.IO) {
        try {
            logger.i(TAG, "Adding device: $name ($deviceId)")
            
            val device = SyncthingDevice(
                deviceId = deviceId,
                name = name,
                connected = false,
                introducer = introducer,
                compression = CompressionMethod.METADATA,
                addresses = listOf("dynamic")
            )
            
            syncthingApi.addDevice(device)
            
            // Reload device list
            loadDevices()
            
            Result.success(device)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to add device", e)
            Result.failure(e)
        }
    }

    /**
     * Remove a device
     */
    suspend fun removeDevice(deviceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            logger.i(TAG, "Removing device: $deviceId")
            syncthingApi.removeDevice(deviceId)
            loadDevices()
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to remove device", e)
            Result.failure(e)
        }
    }

    /**
     * Share a folder with devices
     */
    suspend fun shareFolder(
        folderPath: String,
        folderLabel: String,
        deviceIds: List<String>,
        syncOptions: FolderSyncOptions = FolderSyncOptions.default()
    ): Result<SyncthingFolder> = withContext(Dispatchers.IO) {
        try {
            logger.i(TAG, "Sharing folder: $folderLabel at $folderPath")
            
            // Generate folder ID
            val folderId = generateFolderId(folderPath)
            
            val folder = SyncthingFolder(
                id = folderId,
                label = folderLabel,
                path = folderPath,
                devices = deviceIds,
                type = syncOptions.folderType,
                rescanIntervalS = syncOptions.rescanIntervalS,
                fsWatcherEnabled = syncOptions.fsWatcherEnabled,
                ignorePerms = syncOptions.ignorePerms,
                versioning = syncOptions.versioning
            )
            
            syncthingApi.addFolder(folder)
            
            // Reload folder list
            loadFolders()
            
            Result.success(folder)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to share folder", e)
            Result.failure(e)
        }
    }

    /**
     * Remove a shared folder
     */
    suspend fun unshareFolder(folderId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            logger.i(TAG, "Unsharing folder: $folderId")
            syncthingApi.removeFolder(folderId)
            loadFolders()
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to unshare folder", e)
            Result.failure(e)
        }
    }

    /**
     * Start device discovery
     */
    suspend fun startDiscovery(): Flow<DiscoveredDevice> = flow {
        logger.i(TAG, "Starting device discovery...")
        
        try {
            // Enable global discovery
            syncthingApi.setGlobalDiscovery(true)
            
            // Enable local discovery
            syncthingApi.setLocalDiscovery(true)
            
            discoveryJob = scope.launch {
                var elapsedTime = 0L
                
                while (elapsedTime < DISCOVERY_TIMEOUT_MS) {
                    val discoveredDevices = syncthingApi.getDiscoveredDevices()
                    
                    discoveredDevices.forEach { device ->
                        emit(device)
                    }
                    
                    delay(5000L)
                    elapsedTime += 5000L
                }
            }
        } catch (e: Exception) {
            logger.e(TAG, "Discovery error", e)
            throw e
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Stop device discovery
     */
    fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
    }

    /**
     * Generate QR code data for device pairing
     */
    fun generatePairingQRCode(): String {
        val state = _syncState.value
        return if (state is SyncthingState.Connected) {
            // Format: syncthing://device?id=DEVICE_ID&name=DEVICE_NAME
            "syncthing://device?id=${state.deviceId}&name=${android.os.Build.MODEL}"
        } else {
            throw IllegalStateException("Not connected to Syncthing")
        }
    }

    /**
     * Parse QR code data for device pairing
     */
    fun parsePairingQRCode(qrData: String): PairingInfo? {
        return try {
            if (!qrData.startsWith("syncthing://device?")) {
                return null
            }
            
            val uri = android.net.Uri.parse(qrData)
            val deviceId = uri.getQueryParameter("id")
            val deviceName = uri.getQueryParameter("name")
            
            if (deviceId != null && deviceName != null) {
                PairingInfo(deviceId, deviceName)
            } else {
                null
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to parse QR code", e)
            null
        }
    }

    /**
     * Set network preference for syncing
     */
    suspend fun setNetworkPreference(preference: NetworkPreference) {
        _networkPreference.value = preference
        
        // Update Syncthing configuration based on preference
        when (preference) {
            NetworkPreference.WIFI_ONLY -> {
                // Pause sync if not on WiFi
                if (!isWifiConnected()) {
                    pauseAllFolders()
                }
            }
            NetworkPreference.MOBILE_DATA -> {
                // Resume sync on any connection
                resumeAllFolders()
            }
            NetworkPreference.ALWAYS -> {
                // Always sync regardless of network
                resumeAllFolders()
            }
        }
    }

    /**
     * Resolve a sync conflict
     */
    suspend fun resolveConflict(
        conflictId: String,
        resolution: ConflictResolution
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val conflict = _conflicts.value.find { it.id == conflictId }
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Conflict not found: $conflictId")
                )
            
            conflictResolver.resolve(conflict, resolution)
            
            // Remove from conflicts list
            _conflicts.value = _conflicts.value.filter { it.id != conflictId }
            
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to resolve conflict", e)
            Result.failure(e)
        }
    }

    /**
     * Get detailed sync statistics
     */
    suspend fun getSyncStatistics(): SyncStatistics = withContext(Dispatchers.IO) {
        try {
            val stats = syncthingApi.getSystemStatus()
            
            SyncStatistics(
                totalBytes = stats.totalBytes,
                syncedBytes = stats.syncedBytes,
                uploadRate = stats.uploadRate,
                downloadRate = stats.downloadRate,
                lastSyncTime = stats.lastSyncTime,
                filesInSync = stats.filesInSync,
                filesOutOfSync = stats.filesOutOfSync
            )
        } catch (e: Exception) {
            logger.e(TAG, "Failed to get statistics", e)
            SyncStatistics.empty()
        }
    }

    // Private helper methods

    private fun isSyncthingInstalled(): Boolean {
        val syncthingBinary = File(context.applicationInfo.nativeLibraryDir, "libsyncthing.so")
        return syncthingBinary.exists()
    }

    private suspend fun startSyncthingService() {
        // Start Syncthing native service
        val homeDir = File(context.filesDir, "syncthing")
        homeDir.mkdirs()
        
        syncthingApi.start(
            homeDir = homeDir.absolutePath,
            apiKey = getOrCreateApiKey(),
            guiAddress = "127.0.0.1:$DEFAULT_API_PORT"
        )
    }

    private suspend fun waitForApiAvailable() {
        var attempts = 0
        val maxAttempts = 30
        
        while (attempts < maxAttempts) {
            try {
                if (syncthingApi.ping()) {
                    logger.i(TAG, "Syncthing API available")
                    return
                }
            } catch (e: Exception) {
                // API not ready yet
            }
            
            delay(1000L)
            attempts++
        }
        
        throw SyncthingException("Syncthing API did not become available")
    }

    private suspend fun loadConfiguration() {
        loadDevices()
        loadFolders()
    }

    private suspend fun loadDevices() {
        try {
            val deviceList = syncthingApi.getDevices()
            _devices.value = deviceList
        } catch (e: Exception) {
            logger.e(TAG, "Failed to load devices", e)
        }
    }

    private suspend fun loadFolders() {
        try {
            val folderList = syncthingApi.getFolders()
            _folders.value = folderList
        } catch (e: Exception) {
            logger.e(TAG, "Failed to load folders", e)
        }
    }

    private fun startSyncMonitoring() {
        syncMonitorJob = scope.launch {
            while (isActive) {
                try {
                    updateSyncStatus()
                    checkForConflicts()
                    
                    // Check network preference
                    checkNetworkAndPauseIfNeeded()
                } catch (e: Exception) {
                    logger.e(TAG, "Error in sync monitoring", e)
                }
                
                delay(SYNC_CHECK_INTERVAL_MS)
            }
        }
    }

    private suspend fun updateSyncStatus() {
        try {
            val completion = syncthingApi.getFolderCompletion()
            val connections = syncthingApi.getConnections()
            
            _syncStatus.value = SyncStatus.Syncing(
                folders = completion.folders,
                devices = connections.connections,
                globalBytes = completion.globalBytes,
                needBytes = completion.needBytes,
                uploadRate = connections.uploadRate,
                downloadRate = connections.downloadRate
            )
        } catch (e: Exception) {
            logger.e(TAG, "Failed to update sync status", e)
        }
    }

    private suspend fun checkForConflicts() {
        try {
            val conflictFiles = syncthingApi.getConflicts()
            
            val newConflicts = conflictFiles.map { conflictFile ->
                SyncConflict(
                    id = generateConflictId(conflictFile),
                    filePath = conflictFile.path,
                    localVersion = conflictFile.localVersion,
                    remoteVersion = conflictFile.remoteVersion,
                    localModified = conflictFile.localModified,
                    remoteModified = conflictFile.remoteModified,
                    localSize = conflictFile.localSize,
                    remoteSize = conflictFile.remoteSize
                )
            }
            
            _conflicts.value = newConflicts
        } catch (e: Exception) {
            logger.e(TAG, "Failed to check conflicts", e)
        }
    }

    private suspend fun checkNetworkAndPauseIfNeeded() {
        if (_networkPreference.value == NetworkPreference.WIFI_ONLY) {
            if (!isWifiConnected()) {
                pauseAllFolders()
            } else {
                resumeAllFolders()
            }
        }
    }

    private fun isWifiConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
            as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private suspend fun pauseAllFolders() {
        _folders.value.forEach { folder ->
            try {
                syncthingApi.pauseFolder(folder.id)
            } catch (e: Exception) {
                logger.e(TAG, "Failed to pause folder ${folder.id}", e)
            }
        }
    }

    private suspend fun resumeAllFolders() {
        _folders.value.forEach { folder ->
            try {
                syncthingApi.resumeFolder(folder.id)
            } catch (e: Exception) {
                logger.e(TAG, "Failed to resume folder ${folder.id}", e)
            }
        }
    }

    private fun generateFolderId(path: String): String {
        val md = MessageDigest.getInstance("MD5")
        val hash = md.digest(path.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }.take(8)
    }

    private fun generateConflictId(conflictFile: ConflictFile): String {
        return "${conflictFile.path}_${conflictFile.localModified}"
    }

    private fun getOrCreateApiKey(): String {
        val prefs = context.getSharedPreferences("syncthing_prefs", Context.MODE_PRIVATE)
        var apiKey = prefs.getString("api_key", null)
        
        if (apiKey == null) {
            apiKey = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("api_key", apiKey).apply()
        }
        
        return apiKey
    }
}

// Data class for pairing information
data class PairingInfo(
    val deviceId: String,
    val deviceName: String
)

// Custom exception for Syncthing errors
class SyncthingException(message: String) : Exception(message)
