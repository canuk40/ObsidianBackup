package com.obsidianbackup.domain.root

import com.obsidianbackup.rootcore.shell.ShellExecutor
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Move apps and data to/from SD card (adoptable storage or external).
 *
 * Uses `pm move-package` for app moves and direct file operations for data.
 * Requires root for system-level moves.
 */
@Singleton
class AppToSdManager @Inject constructor(
    private val shellExecutor: ShellExecutor
) {
    companion object {
        private const val TAG = "[AppToSd]"
    }

    data class MoveResult(
        val packageName: String,
        val success: Boolean,
        val message: String,
        val newLocation: String? = null
    )

    data class StorageInfo(
        val internalUsedBytes: Long,
        val externalAvailableBytes: Long,
        val adoptableStorageId: String? = null
    )

    /**
     * Move an app to external/adopted storage via `pm move-package`.
     * Only works with adoptable storage on Android 6+.
     */
    suspend fun moveToSd(packageName: String): MoveResult {
        val volumeId = getAdoptableVolumeId()
        if (volumeId == null) {
            return MoveResult(packageName, false, "No adoptable storage found")
        }

        val result = shellExecutor.executeRoot("pm move-package $packageName $volumeId")
        return if (result.success) {
            Timber.d("$TAG Moved $packageName to $volumeId")
            MoveResult(packageName, true, "Moved to external storage", volumeId)
        } else {
            Timber.w("$TAG Failed to move $packageName: ${result.stderr}")
            MoveResult(packageName, false, result.stderr)
        }
    }

    /**
     * Move an app back to internal storage.
     */
    suspend fun moveToInternal(packageName: String): MoveResult {
        val result = shellExecutor.executeRoot("pm move-package $packageName internal")
        return if (result.success) {
            Timber.d("$TAG Moved $packageName to internal")
            MoveResult(packageName, true, "Moved to internal storage", "internal")
        } else {
            MoveResult(packageName, false, result.stderr)
        }
    }

    /**
     * Move app's data directory to external storage using symlinks.
     * Creates /sdcard/ObsidianBackup/app_data/<pkg>/ and symlinks from /data/data/<pkg>/.
     */
    suspend fun moveDataToSd(packageName: String, externalDataDir: String): MoveResult {
        val dataDir = "/data/data/$packageName"
        val extDir = "$externalDataDir/$packageName"

        // Stop app first
        shellExecutor.executeRoot("am force-stop $packageName")

        // Get app UID for chown
        val uidResult = shellExecutor.executeRoot("stat -c %u $dataDir")
        val uid = uidResult.stdout.trim()

        // Create external dir and copy data
        shellExecutor.executeRoot("mkdir -p $extDir")
        val copyResult = shellExecutor.executeRoot("cp -a $dataDir/. $extDir/")
        if (!copyResult.success) {
            return MoveResult(packageName, false, "Copy failed: ${copyResult.stderr}")
        }

        // Rename original and create symlink
        shellExecutor.executeRoot("mv $dataDir ${dataDir}_backup")
        val linkResult = shellExecutor.executeRoot("ln -s $extDir $dataDir")
        if (!linkResult.success) {
            // Rollback
            shellExecutor.executeRoot("rm -f $dataDir && mv ${dataDir}_backup $dataDir")
            return MoveResult(packageName, false, "Symlink failed: ${linkResult.stderr}")
        }

        // Fix ownership
        shellExecutor.executeRoot("chown -h $uid:$uid $dataDir")
        shellExecutor.executeRoot("rm -rf ${dataDir}_backup")

        Timber.d("$TAG Data for $packageName moved to SD via symlink")
        return MoveResult(packageName, true, "Data moved to SD via symlink", extDir)
    }

    /**
     * Get the current install location for a package.
     */
    suspend fun getInstallLocation(packageName: String): String {
        val result = shellExecutor.executeRoot("pm path $packageName")
        return if (result.success) result.stdout.trim() else "unknown"
    }

    /**
     * Find adoptable storage volume ID.
     */
    private suspend fun getAdoptableVolumeId(): String? {
        val result = shellExecutor.executeRoot("sm list-volumes")
        if (!result.success) return null

        // Look for "private" volumes (adopted SD cards)
        for (line in result.stdout.lines()) {
            if (line.contains("private") && line.contains("mounted")) {
                return line.split(" ").firstOrNull()
            }
        }
        return null
    }
}
