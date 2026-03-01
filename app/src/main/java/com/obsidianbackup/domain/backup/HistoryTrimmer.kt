package com.obsidianbackup.domain.backup

import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.storage.BackupCatalog
import com.obsidianbackup.storage.LabelDao
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auto-trims backup history per app when exceeding max count.
 * Skips protected backups (marked by user as important).
 * Inspired by Titanium Backup's history trimming + Swift Backup's protected backups.
 */
@Singleton
class HistoryTrimmer @Inject constructor(
    private val catalog: BackupCatalog,
    private val labelDao: LabelDao,
    private val logger: ObsidianLogger
) {
    companion object {
        private const val TAG = "HistoryTrimmer"
        const val DEFAULT_MAX_HISTORY = 5
    }

    /**
     * Trim backup history for a specific app.
     * Deletes oldest non-protected snapshots when count exceeds [maxHistory].
     *
     * @param appId the package name
     * @param maxHistory max backups to keep per app (default 5)
     * @return number of snapshots deleted
     */
    suspend fun trimForApp(appId: String, maxHistory: Int = DEFAULT_MAX_HISTORY): Int {
        val protectedIds = labelDao.getAllProtectedSnapshotIds().toSet()
        val snapshots = catalog.getSnapshotsForApp(appId)

        if (snapshots.size <= maxHistory) return 0

        // Sort oldest first, skip protected ones
        val deletable = snapshots
            .sortedBy { it.timestamp }
            .filter { it.id !in protectedIds }

        val toDelete = deletable.size - maxHistory
        if (toDelete <= 0) return 0

        var deleted = 0
        for (snapshot in deletable.take(toDelete)) {
            try {
                catalog.deleteSnapshot(snapshot.id)
                deleted++
                Timber.d("$TAG Trimmed old snapshot ${snapshot.id} for $appId")
            } catch (e: Exception) {
                logger.e(TAG, "Failed to trim snapshot ${snapshot.id}", e)
            }
        }

        Timber.i("$TAG Trimmed $deleted snapshots for $appId (max=$maxHistory, protected=${protectedIds.size})")
        return deleted
    }

    /**
     * Trim history for all apps.
     * @return total number of snapshots deleted
     */
    suspend fun trimAll(maxHistory: Int = DEFAULT_MAX_HISTORY): Int {
        val allApps = catalog.getAllBackedUpAppIds()
        var totalDeleted = 0
        for (appId in allApps) {
            totalDeleted += trimForApp(appId, maxHistory)
        }
        if (totalDeleted > 0) {
            Timber.i("$TAG Total trimmed: $totalDeleted snapshots across ${allApps.size} apps")
        }
        return totalDeleted
    }
}
