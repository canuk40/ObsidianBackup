// api/plugin/BackupEnginePlugin.kt
package com.obsidianbackup.api.plugin

import com.obsidianbackup.model.BackupRequest
import com.obsidianbackup.model.BackupResult
import com.obsidianbackup.model.RestoreRequest
import com.obsidianbackup.model.RestoreResult
import com.obsidianbackup.model.BackupId
import com.obsidianbackup.model.VerificationResult
import kotlinx.coroutines.flow.Flow

/**
 * Plugin that provides alternative backup engine implementation
 * Examples: Btrfs snapshots, Borg, Restic, remote rsync
 */
interface BackupEnginePlugin : ObsidianBackupPlugin {

    /**
     * Check if this engine can handle the given request
     */
    suspend fun canHandle(request: BackupRequest): Boolean

    /**
     * Execute backup operation
     */
    suspend fun backup(request: BackupRequest): PluginResult<BackupResult>

    /**
     * Execute restore operation
     */
    suspend fun restore(request: RestoreRequest): PluginResult<RestoreResult>

    /**
     * Verify backup integrity
     */
    suspend fun verify(snapshotId: BackupId): PluginResult<VerificationResult>

    /**
     * Observe backup/restore progress
     */
    fun observeProgress(): Flow<EngineProgress>

    /**
     * Engine-specific configuration
     */
    suspend fun configure(config: EngineConfiguration): PluginResult<Unit>

    /**
     * Get supported features of this engine
     */
    fun getSupportedFeatures(): Set<EngineFeature>
}

/**
 * Features that backup engines can support
 */
enum class EngineFeature {
    INCREMENTAL_BACKUP,
    COMPRESSION,
    ENCRYPTION,
    DEDUPLICATION,
    SNAPSHOT_DIFF,
    REMOTE_BACKUP,
    STREAMING_BACKUP,
    PARALLEL_OPERATIONS
}

/**
 * Engine configuration model
 */
data class EngineConfiguration(
    val compressionLevel: Int = 6,
    val enableEncryption: Boolean = false,
    val maxConcurrency: Int = 4,
    val customOptions: Map<String, String> = emptyMap()
)

/**
 * Progress model emitted by engines
 */
sealed class EngineProgress {
    object Idle : EngineProgress()
    data class InProgress(
        val currentItem: String,
        val itemsCompleted: Int,
        val totalItems: Int,
        val bytesProcessed: Long,
        val totalBytes: Long,
        val estimatedTimeRemaining: Long? = null
    ) : EngineProgress()
    data class Completed(val result: BackupResult) : EngineProgress()
    data class Failed(val error: PluginError) : EngineProgress()
}
