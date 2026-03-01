// cloud/ConflictResolver.kt
package com.obsidianbackup.cloud

import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.model.SnapshotId
import kotlin.math.abs

class ConflictResolver(
    private val logger: ObsidianLogger
) {

    enum class ConflictStrategy {
        SERVER_WINS, // Download and overwrite local
        CLIENT_WINS, // Upload and overwrite remote
        MANUAL,      // Ask user to choose
        MERGE        // Attempt to merge (not always possible)
    }

    data class ConflictInfo(
        val snapshotId: SnapshotId,
        val localTimestamp: Long,
        val remoteTimestamp: Long,
        val localSize: Long,
        val remoteSize: Long,
        val localChecksum: String,
        val remoteChecksum: String
    )

    fun detectConflict(localInfo: CloudSnapshotInfo, remoteInfo: CloudSnapshotInfo): ConflictInfo? {
        if (localInfo.snapshotId != remoteInfo.snapshotId) return null

        val hasConflict = localInfo.timestamp != remoteInfo.timestamp ||
                         localInfo.checksum != remoteInfo.checksum

        return if (hasConflict) {
            ConflictInfo(
                snapshotId = localInfo.snapshotId,
                localTimestamp = localInfo.timestamp,
                remoteTimestamp = remoteInfo.timestamp,
                localSize = localInfo.sizeBytes,
                remoteSize = remoteInfo.sizeBytes,
                localChecksum = localInfo.checksum,
                remoteChecksum = remoteInfo.checksum
            )
        } else null
    }

    fun resolveConflict(conflict: ConflictInfo, strategy: ConflictStrategy): ConflictResolution {
        return when (strategy) {
            ConflictStrategy.SERVER_WINS -> {
                logger.i(TAG, "Resolving conflict for ${conflict.snapshotId}: Server wins")
                ConflictResolution.DownloadRemote
            }
            ConflictStrategy.CLIENT_WINS -> {
                logger.i(TAG, "Resolving conflict for ${conflict.snapshotId}: Client wins")
                ConflictResolution.UploadLocal
            }
            ConflictStrategy.MANUAL -> {
                logger.i(TAG, "Resolving conflict for ${conflict.snapshotId}: Manual resolution needed")
                ConflictResolution.ManualResolution(conflict)
            }
            ConflictStrategy.MERGE -> {
                // For snapshots, merge is not typically possible
                // Fall back to manual or server wins
                if (canMerge(conflict)) {
                    logger.i(TAG, "Resolving conflict for ${conflict.snapshotId}: Merging")
                    ConflictResolution.Merge
                } else {
                    logger.w(TAG, "Cannot merge conflict for ${conflict.snapshotId}, falling back to manual")
                    ConflictResolution.ManualResolution(conflict)
                }
            }
        }
    }

    fun suggestStrategy(conflict: ConflictInfo): ConflictStrategy {
        return when {
            // If local is significantly newer, suggest client wins
            conflict.localTimestamp > conflict.remoteTimestamp + 3600000 -> { // 1 hour
                ConflictStrategy.CLIENT_WINS
            }
            // If remote is significantly newer, suggest server wins
            conflict.remoteTimestamp > conflict.localTimestamp + 3600000 -> {
                ConflictStrategy.SERVER_WINS
            }
            // If sizes are very different, manual resolution
            abs(conflict.localSize - conflict.remoteSize) > conflict.localSize * 0.1 -> {
                ConflictStrategy.MANUAL
            }
            // Otherwise, server wins (arbitrary choice)
            else -> ConflictStrategy.SERVER_WINS
        }
    }

    private fun canMerge(conflict: ConflictInfo): Boolean {
        // Snapshots are atomic units, merging is not supported
        return false
    }

    sealed class ConflictResolution {
        object DownloadRemote : ConflictResolution()
        object UploadLocal : ConflictResolution()
        object Merge : ConflictResolution()
        data class ManualResolution(val conflict: ConflictInfo) : ConflictResolution()
    }

    companion object {
        private const val TAG = "ConflictResolver"
    }
}
