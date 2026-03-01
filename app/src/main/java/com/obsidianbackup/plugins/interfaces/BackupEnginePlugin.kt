// plugins/interfaces/BackupEnginePlugin.kt
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
