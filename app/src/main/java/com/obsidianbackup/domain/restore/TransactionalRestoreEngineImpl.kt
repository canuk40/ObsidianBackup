// domain/restore/TransactionalRestoreEngineImpl.kt
package com.obsidianbackup.domain.restore

import com.obsidianbackup.model.AppId
import com.obsidianbackup.model.BackupId
import com.obsidianbackup.engine.TransactionalRestoreEngine as EngineImpl
import javax.inject.Inject

/**
 * Implementation of domain TransactionalRestoreEngine interface
 * that delegates to the engine TransactionalRestoreEngine
 */
class TransactionalRestoreEngineImpl @Inject constructor(
    private val engineImpl: EngineImpl
) : TransactionalRestoreEngine {
    
    override suspend fun beginTransaction(backupId: BackupId): RestoreTransaction {
        // Create a new transaction with unique ID
        return RestoreTransaction(
            id = java.util.UUID.randomUUID().toString(),
            backupId = backupId,
            startedAt = System.currentTimeMillis()
        )
    }
    
    override suspend fun restoreApp(
        transaction: RestoreTransaction,
        appId: AppId
    ): RestoreResult {
        return try {
            // Delegate to engine implementation
            val request = com.obsidianbackup.model.RestoreRequest(
                snapshotId = com.obsidianbackup.model.SnapshotId(transaction.backupId.value),
                appIds = listOf(appId),
                components = setOf(
                    com.obsidianbackup.model.BackupComponent.APK,
                    com.obsidianbackup.model.BackupComponent.DATA
                )
            )
            
            val result = engineImpl.restoreApps(request)
            
            when (result) {
                is com.obsidianbackup.model.RestoreResult.Success -> 
                    RestoreResult.Success(restoredAppIds = result.appsRestored)
                is com.obsidianbackup.model.RestoreResult.PartialSuccess ->
                    RestoreResult.PartialSuccess(
                        restoredAppIds = result.appsRestored,
                        failedAppIds = result.appsFailed,
                        errors = result.errors.mapIndexed { index, error -> 
                            result.appsFailed.getOrNull(index)?.let { it to Exception(error) }
                        }.filterNotNull().toMap()
                    )
                is com.obsidianbackup.model.RestoreResult.Failure ->
                    RestoreResult.Failure(error = Exception(result.reason))
            }
        } catch (e: Exception) {
            RestoreResult.Failure(error = e)
        }
    }
    
    override suspend fun commit(transaction: RestoreTransaction): RestoreResult {
        // Transaction commit is handled by the engine
        return RestoreResult.Success(restoredAppIds = emptyList())
    }
    
    override suspend fun rollback(transaction: RestoreTransaction) {
        // Rollback is handled by the engine's RestoreTransaction
        // This is a no-op at domain layer as engine handles it
    }
}
