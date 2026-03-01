package com.obsidianbackup.wear.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Data Layer communication between phone and watch
 */
@Singleton
class DataLayerRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataClient: DataClient by lazy { Wearable.getDataClient(context) }
    private val messageClient: MessageClient by lazy { Wearable.getMessageClient(context) }
    private val capabilityClient: CapabilityClient by lazy { Wearable.getCapabilityClient(context) }

    private val _backupStatus = MutableStateFlow(BackupStatus())
    val backupStatus: StateFlow<BackupStatus> = _backupStatus.asStateFlow()

    private val _backupProgress = MutableStateFlow(BackupProgress())
    val backupProgress: StateFlow<BackupProgress> = _backupProgress.asStateFlow()

    private val _settings = MutableStateFlow(WearSettings())
    val settings: StateFlow<WearSettings> = _settings.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Send backup request to phone
     */
    suspend fun requestBackup(): Boolean {
        return try {
            val nodes = getConnectedNodes()
            if (nodes.isEmpty()) {
                Log.w(TAG, "No connected nodes found")
                return false
            }

            nodes.forEach { nodeId ->
                messageClient.sendMessage(
                    nodeId,
                    DataLayerPaths.BACKUP_TRIGGER_PATH,
                    MessageTypes.REQUEST_BACKUP.toByteArray()
                ).await()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request backup", e)
            false
        }
    }

    /**
     * Request current status from phone
     */
    suspend fun requestStatus(): Boolean {
        return try {
            val nodes = getConnectedNodes()
            if (nodes.isEmpty()) return false

            nodes.forEach { nodeId ->
                messageClient.sendMessage(
                    nodeId,
                    DataLayerPaths.BACKUP_STATUS_PATH,
                    MessageTypes.REQUEST_STATUS.toByteArray()
                ).await()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request status", e)
            false
        }
    }

    /**
     * Cancel ongoing backup
     */
    suspend fun cancelBackup(): Boolean {
        return try {
            val nodes = getConnectedNodes()
            if (nodes.isEmpty()) return false

            nodes.forEach { nodeId ->
                messageClient.sendMessage(
                    nodeId,
                    DataLayerPaths.BACKUP_TRIGGER_PATH,
                    MessageTypes.CANCEL_BACKUP.toByteArray()
                ).await()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel backup", e)
            false
        }
    }

    /**
     * Update backup status from Data Layer
     */
    fun updateBackupStatus(data: ByteArray) {
        try {
            val status = json.decodeFromString<BackupStatus>(String(data))
            _backupStatus.value = status
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode backup status", e)
        }
    }

    /**
     * Update backup progress from Data Layer
     */
    fun updateBackupProgress(data: ByteArray) {
        try {
            val progress = json.decodeFromString<BackupProgress>(String(data))
            _backupProgress.value = progress
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode backup progress", e)
        }
    }

    /**
     * Update settings from Data Layer
     */
    fun updateSettings(data: ByteArray) {
        try {
            val settings = json.decodeFromString<WearSettings>(String(data))
            _settings.value = settings
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode settings", e)
        }
    }

    /**
     * Get list of connected nodes (phones)
     */
    private suspend fun getConnectedNodes(): List<String> {
        return try {
            val capabilityInfo = capabilityClient
                .getCapability(DataLayerPaths.CAPABILITY_PHONE_APP, CapabilityClient.FILTER_REACHABLE)
                .await()
            capabilityInfo.nodes.map { it.id }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get connected nodes", e)
            emptyList()
        }
    }

    /**
     * Check if phone app is connected
     */
    suspend fun isPhoneConnected(): Boolean {
        return getConnectedNodes().isNotEmpty()
    }

    companion object {
        private const val TAG = "DataLayerRepository"
    }
}
