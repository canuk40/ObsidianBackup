// error/ErrorRecovery.kt
package com.obsidianbackup.error

import android.content.Context
import android.net.ConnectivityManager
import com.obsidianbackup.logging.LogLevel
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.model.BackupId
import com.obsidianbackup.storage.BackupCatalog
import com.obsidianbackup.storage.FileSystemManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

class ErrorRecoveryManager(
    private val context: Context,
    private val logger: ObsidianLogger,
    private val backupCatalog: BackupCatalog,
    private val fileSystemManager: FileSystemManager
) {
    suspend fun attemptRecovery(error: ObsidianError): RecoveryResult {
        logger.i("ErrorRecovery", "Attempting recovery for: ${error.message}")

        return when (error) {
            is ObsidianError.PermissionDenied -> recoverPermission(error)
            is ObsidianError.InsufficientStorage -> recoverStorage(error)
            is ObsidianError.NetworkError -> recoverNetwork(error)
            is ObsidianError.IOError -> recoverIO(error)
            else -> RecoveryResult.CannotRecover("No recovery strategy available")
        }
    }

    private suspend fun recoverPermission(error: ObsidianError.PermissionDenied): RecoveryResult {
        // Try to re-request permission or guide user
        return RecoveryResult.RequiresUserAction(
            "Please grant ${error.requiredPermission} permission and try again"
        )
    }

    private suspend fun recoverStorage(error: ObsidianError.InsufficientStorage): RecoveryResult {
        val required = error.requiredBytes
        val available = error.availableBytes
        val deficit = required - available

        // Try to free up space by cleaning old backups
        val freed = cleanOldBackups(deficit)

        return if (freed >= deficit) {
            RecoveryResult.Recovered("Freed ${freed / 1024 / 1024} MB by cleaning old backups")
        } else {
            RecoveryResult.RequiresUserAction(
                "Need ${deficit / 1024 / 1024} MB more space. Please free up storage."
            )
        }
    }

    private suspend fun recoverNetwork(error: ObsidianError.NetworkError): RecoveryResult {
        // Check if network is available now
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork

        return if (network != null) {
            RecoveryResult.Recovered("Network connection restored")
        } else {
            RecoveryResult.RequiresUserAction("Please connect to network and try again")
        }
    }

    private suspend fun recoverIO(error: ObsidianError.IOError): RecoveryResult {
        // Wait a bit and retry
        delay(2000)
        return RecoveryResult.Recovered("IO operation can be retried")
    }

    private suspend fun cleanOldBackups(bytesNeeded: Long): Long {
        var totalFreed = 0L
        val snapshots = backupCatalog.getAllBackupsSync()
            .sortedBy { it.timestamp } // oldest first

        for (snapshot in snapshots) {
            if (totalFreed >= bytesNeeded) break
            val snapshotSize = snapshot.totalSize
            try {
                fileSystemManager.deleteSnapshotDirectory(snapshot.id.value)
                backupCatalog.deleteSnapshot(snapshot.id)
                totalFreed += snapshotSize
                logger.i("ErrorRecovery", "Auto-freed ${snapshotSize}B by deleting snapshot ${snapshot.id.value}")
            } catch (e: Exception) {
                logger.e("ErrorRecovery", "Failed to delete snapshot ${snapshot.id.value} during storage recovery", e)
            }
        }

        return totalFreed
    }
}

sealed class RecoveryResult {
    data class Recovered(val message: String) : RecoveryResult()
    data class RequiresUserAction(val message: String) : RecoveryResult()
    data class CannotRecover(val reason: String) : RecoveryResult()
}
