// engine/BackupEngine.kt
package com.titanbackup.engine

import com.titanbackup.model.*
import kotlinx.coroutines.flow.Flow

interface BackupEngine {
    suspend fun backupApps(request: BackupRequest): BackupResult
    suspend fun restoreApps(request: RestoreRequest): RestoreResult
    suspend fun verifySnapshot(id: BackupId): VerificationResult
    suspend fun deleteSnapshot(id: BackupId): Boolean
    fun observeProgress(): Flow<OperationProgress>
}
