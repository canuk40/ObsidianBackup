package com.obsidianbackup.engine

import com.obsidianbackup.model.*
import kotlinx.coroutines.flow.Flow

interface BackupEngine {
    suspend fun backupApps(request: BackupRequest): BackupResult
    suspend fun restoreApps(request: RestoreRequest): RestoreResult
    suspend fun verifySnapshot(id: BackupId): com.obsidianbackup.model.VerificationResult
    suspend fun deleteSnapshot(id: BackupId): Boolean
    fun observeProgress(): Flow<OperationProgress>
    suspend fun cleanup() { /* optional cleanup */ }
}
