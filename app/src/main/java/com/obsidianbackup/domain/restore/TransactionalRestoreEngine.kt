// domain/restore/TransactionalRestoreEngine.kt
package com.obsidianbackup.domain.restore

import com.obsidianbackup.model.AppId
import com.obsidianbackup.model.BackupId

/**
 * Transactional restore engine with rollback support
 */
interface TransactionalRestoreEngine {
    suspend fun beginTransaction(backupId: BackupId): RestoreTransaction
    suspend fun restoreApp(transaction: RestoreTransaction, appId: AppId): RestoreResult
    suspend fun commit(transaction: RestoreTransaction): RestoreResult
    suspend fun rollback(transaction: RestoreTransaction)
}

data class RestoreTransaction(
    val id: String,
    val backupId: BackupId,
    val startedAt: Long = System.currentTimeMillis()
)

sealed class RestoreResult {
    data class Success(val restoredAppIds: List<AppId>) : RestoreResult()
    data class Failure(val error: Throwable) : RestoreResult()
    data class PartialSuccess(
        val restoredAppIds: List<AppId>,
        val failedAppIds: List<AppId>,
        val errors: Map<AppId, Throwable>
    ) : RestoreResult()
}

data class RestoreRequest(
    val backupId: BackupId,
    val appIds: List<AppId>? = null,
    val skipSystemApps: Boolean = false
)
