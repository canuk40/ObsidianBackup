// sync/models/SyncthingModels.kt
package com.obsidianbackup.sync.models

import kotlinx.serialization.Serializable

/**
 * Syncthing state
 */
sealed class SyncthingState {
    object Disconnected : SyncthingState()
    data class Connected(val deviceId: String) : SyncthingState()
    data class Error(val message: String) : SyncthingState()
}

/**
 * Syncthing device representation
 */
@Serializable
data class SyncthingDevice(
    val deviceId: String,
    val name: String,
    val connected: Boolean = false,
    val introducer: Boolean = false,
    val compression: CompressionMethod = CompressionMethod.METADATA,
    val addresses: List<String> = listOf("dynamic"),
    val certName: String = "",
    val maxSendKbps: Int = 0,
    val maxRecvKbps: Int = 0
)

/**
 * Syncthing folder representation
 */
@Serializable
data class SyncthingFolder(
    val id: String,
    val label: String,
    val path: String,
    val devices: List<String>,
    val type: FolderType = FolderType.SEND_RECEIVE,
    val rescanIntervalS: Int = 3600,
    val fsWatcherEnabled: Boolean = true,
    val ignorePerms: Boolean = false,
    val versioning: VersioningConfig? = null,
    val paused: Boolean = false
)

/**
 * Folder type
 */
@Serializable
enum class FolderType {
    SEND_RECEIVE,
    SEND_ONLY,
    RECEIVE_ONLY
}

/**
 * Compression method
 */
@Serializable
enum class CompressionMethod {
    METADATA,
    ALWAYS,
    NEVER
}

/**
 * Versioning configuration
 */
@Serializable
data class VersioningConfig(
    val type: VersioningType,
    val params: Map<String, String> = emptyMap()
)

@Serializable
enum class VersioningType {
    NONE,
    TRASH_CAN,
    SIMPLE,
    STAGGERED,
    EXTERNAL
}

/**
 * Sync status
 */
sealed class SyncStatus {
    object Idle : SyncStatus()
    
    data class Syncing(
        val folders: Map<String, FolderSyncInfo>,
        val devices: Map<String, DeviceConnection>,
        val globalBytes: Long,
        val needBytes: Long,
        val uploadRate: Long,
        val downloadRate: Long
    ) : SyncStatus()
}

@Serializable
data class FolderSyncInfo(
    val folderId: String,
    val completion: Double,
    val globalBytes: Long,
    val needBytes: Long,
    val state: String
)

@Serializable
data class DeviceConnection(
    val deviceId: String,
    val connected: Boolean,
    val address: String,
    val clientVersion: String,
    val inBytesTotal: Long,
    val outBytesTotal: Long
)

/**
 * Discovered device during discovery
 */
@Serializable
data class DiscoveredDevice(
    val deviceId: String,
    val name: String?,
    val addresses: List<String>
)

/**
 * Network preference for syncing
 */
enum class NetworkPreference {
    WIFI_ONLY,
    MOBILE_DATA,
    ALWAYS
}

/**
 * Folder sync options
 */
data class FolderSyncOptions(
    val folderType: FolderType,
    val rescanIntervalS: Int,
    val fsWatcherEnabled: Boolean,
    val ignorePerms: Boolean,
    val versioning: VersioningConfig?
) {
    companion object {
        fun default() = FolderSyncOptions(
            folderType = FolderType.SEND_RECEIVE,
            rescanIntervalS = 3600,
            fsWatcherEnabled = true,
            ignorePerms = false,
            versioning = VersioningConfig(
                type = VersioningType.SIMPLE,
                params = mapOf("keep" to "5")
            )
        )
    }
}

/**
 * Sync conflict
 */
@Serializable
data class SyncConflict(
    val id: String,
    val filePath: String,
    val localVersion: String,
    val remoteVersion: String,
    val localModified: Long,
    val remoteModified: Long,
    val localSize: Long,
    val remoteSize: Long
)

/**
 * Conflict resolution choice
 */
enum class ConflictResolution {
    KEEP_LOCAL,
    KEEP_REMOTE,
    KEEP_BOTH,
    MERGE_MANUAL
}

/**
 * Conflict file information from API
 */
@Serializable
data class ConflictFile(
    val path: String,
    val localVersion: String,
    val remoteVersion: String,
    val localModified: Long,
    val remoteModified: Long,
    val localSize: Long,
    val remoteSize: Long
)

/**
 * Sync statistics
 */
@Serializable
data class SyncStatistics(
    val totalBytes: Long,
    val syncedBytes: Long,
    val uploadRate: Long,
    val downloadRate: Long,
    val lastSyncTime: Long,
    val filesInSync: Int,
    val filesOutOfSync: Int
) {
    companion object {
        fun empty() = SyncStatistics(
            totalBytes = 0,
            syncedBytes = 0,
            uploadRate = 0,
            downloadRate = 0,
            lastSyncTime = 0,
            filesInSync = 0,
            filesOutOfSync = 0
        )
    }
}

/**
 * System status from Syncthing API
 */
@Serializable
data class SystemStatus(
    val totalBytes: Long = 0,
    val syncedBytes: Long = 0,
    val uploadRate: Long = 0,
    val downloadRate: Long = 0,
    val lastSyncTime: Long = 0,
    val filesInSync: Int = 0,
    val filesOutOfSync: Int = 0,
    val uptime: Long = 0,
    val myID: String = "",
    val alloc: Long = 0,
    val sys: Long = 0
)

/**
 * Folder completion info
 */
@Serializable
data class FolderCompletion(
    val folders: Map<String, FolderSyncInfo>,
    val globalBytes: Long,
    val needBytes: Long
)

/**
 * Connection info from Syncthing
 */
@Serializable
data class ConnectionInfo(
    val connections: Map<String, DeviceConnection>,
    val uploadRate: Long,
    val downloadRate: Long
)
