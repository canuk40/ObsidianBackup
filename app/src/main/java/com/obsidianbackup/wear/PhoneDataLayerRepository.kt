package com.obsidianbackup.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data models for Wear OS communication (phone app side)
 */
@Serializable
data class BackupStatusData(
    val isRunning: Boolean = false,
    val lastBackupTime: Long = 0L,
    val lastBackupSuccess: Boolean = false,
    val nextScheduledBackup: Long = 0L,
    val totalBackups: Int = 0,
    val backupSizeMB: Float = 0f,
    val status: String = ""
)

@Serializable
data class BackupProgressData(
    val currentFile: String = "",
    val filesProcessed: Int = 0,
    val totalFiles: Int = 0,
    val bytesProcessed: Long = 0L,
    val totalBytes: Long = 0L,
    val percentage: Int = 0,
    val status: String = ""
)

/**
 * Manages Wear OS Data Layer communication from phone app
 */
@Singleton
class PhoneDataLayerRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataClient: DataClient by lazy { Wearable.getDataClient(context) }
    private val messageClient: MessageClient by lazy { Wearable.getMessageClient(context) }
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "PhoneDataLayerRepo"
        private const val BACKUP_STATUS_PATH = "/backup_status"
        private const val BACKUP_PROGRESS_PATH = "/backup_progress"
        private const val BACKUP_TRIGGER_PATH = "/backup_trigger"
    }

    /**
     * Send backup status to all connected watches
     */
    suspend fun sendBackupStatus(status: BackupStatusData) {
        try {
            val jsonData = json.encodeToString(status)
            val putDataReq = PutDataMapRequest.create(BACKUP_STATUS_PATH).apply {
                dataMap.putString("status", jsonData)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(putDataReq).await()
            Log.d(TAG, "Backup status sent to watches")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send backup status", e)
        }
    }

    /**
     * Send backup progress update to all connected watches
     */
    suspend fun sendBackupProgress(progress: BackupProgressData) {
        try {
            val jsonData = json.encodeToString(progress)
            val putDataReq = PutDataMapRequest.create(BACKUP_PROGRESS_PATH).apply {
                dataMap.putString("progress", jsonData)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(putDataReq).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send backup progress", e)
        }
    }

    /**
     * Get list of connected watch nodes
     */
    suspend fun getConnectedWatches(): List<Node> {
        return try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            Log.d(TAG, "Found ${nodes.size} connected watches")
            nodes
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get connected watches", e)
            emptyList()
        }
    }
}
