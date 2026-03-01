// data/repository/BackupRepository.kt
package com.obsidianbackup.data.repository

import com.obsidianbackup.engine.BackupEngine
import com.obsidianbackup.model.*

class BackupRepository(
    private val backupEngine: BackupEngine
) {
    suspend fun backupApps(request: BackupRequest): BackupResult {
        return backupEngine.backupApps(request)
    }

    suspend fun restoreApps(request: RestoreRequest): RestoreResult {
        return backupEngine.restoreApps(request)
    }

    suspend fun verifySnapshot(id: BackupId): VerificationResult {
        return backupEngine.verifySnapshot(id)
    }

    suspend fun deleteSnapshot(id: BackupId): Boolean {
        return backupEngine.deleteSnapshot(id)
    }

    fun observeProgress() = backupEngine.observeProgress()
}
