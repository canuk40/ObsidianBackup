// domain/usecase/RestoreAppsUseCase.kt
package com.obsidianbackup.domain.usecase

import com.obsidianbackup.domain.restore.TransactionalRestoreEngine
import com.obsidianbackup.model.AppId
import com.obsidianbackup.model.BackupComponent
import com.obsidianbackup.model.SnapshotId
import com.obsidianbackup.error.*
import com.obsidianbackup.error.Result as ErrorResult
import javax.inject.Inject

class RestoreAppsUseCase @Inject constructor(
    private val transactionalRestoreEngine: TransactionalRestoreEngine,
    private val retryStrategy: RetryStrategy,
    private val errorRecovery: ErrorRecoveryManager
) {
    suspend operator fun invoke(params: Params): ErrorResult<com.obsidianbackup.model.RestoreResult> {
        return retryStrategy.execute {
            try {
                // Convert domain RestoreRequest to model RestoreRequest
                val modelRequest = com.obsidianbackup.model.RestoreRequest(
                    snapshotId = SnapshotId(params.request.backupId.value),
                    appIds = params.request.appIds ?: emptyList(),
                    components = setOf(BackupComponent.APK, BackupComponent.DATA)
                )
                
                // Begin transactional restore
                val transaction = transactionalRestoreEngine.beginTransaction(params.request.backupId)
                
                // Restore each app within the transaction
                val errors = mutableMapOf<AppId, Throwable>()
                val restored = mutableListOf<AppId>()
                
                for (appId in modelRequest.appIds) {
                    try {
                        val result = transactionalRestoreEngine.restoreApp(transaction, appId)
                        when (result) {
                            is com.obsidianbackup.domain.restore.RestoreResult.Success -> {
                                restored.addAll(result.restoredAppIds)
                            }
                            is com.obsidianbackup.domain.restore.RestoreResult.Failure -> {
                                errors[appId] = result.error
                            }
                            is com.obsidianbackup.domain.restore.RestoreResult.PartialSuccess -> {
                                restored.addAll(result.restoredAppIds)
                                result.errors.forEach { (id, error) -> errors[id] = error }
                            }
                        }
                    } catch (e: Exception) {
                        errors[appId] = e
                    }
                }
                
                // Commit or rollback based on results
                val result = if (errors.isEmpty()) {
                    transactionalRestoreEngine.commit(transaction)
                    com.obsidianbackup.model.RestoreResult.Success(
                        appsRestored = restored,
                        duration = System.currentTimeMillis() - transaction.startedAt
                    )
                } else if (restored.isEmpty()) {
                    // Complete failure - rollback
                    transactionalRestoreEngine.rollback(transaction)
                    com.obsidianbackup.model.RestoreResult.Failure(
                        reason = "All apps failed to restore: ${errors.values.firstOrNull()?.message ?: "Unknown error"}"
                    )
                } else {
                    // Partial success - commit what we can
                    transactionalRestoreEngine.commit(transaction)
                    com.obsidianbackup.model.RestoreResult.PartialSuccess(
                        appsRestored = restored,
                        appsFailed = errors.keys.toList(),
                        duration = System.currentTimeMillis() - transaction.startedAt,
                        errors = errors.map { (appId, throwable) -> 
                            "${appId.value}: ${throwable.message}" 
                        }
                    )
                }
                
                ErrorResult.Success(result)
            } catch (e: Exception) {
                val error = mapExceptionToError(e)

                // Attempt automatic recovery
                when (val recovery = errorRecovery.attemptRecovery(error)) {
                    is RecoveryResult.Recovered -> {
                        // Retry after successful recovery
                        invoke(params)
                    }
                    is RecoveryResult.RequiresUserAction -> {
                        ErrorResult.Error(error)
                    }
                    is RecoveryResult.CannotRecover -> {
                        ErrorResult.Error(error)
                    }
                }
            }
        }
    }

    private fun mapExceptionToError(e: Exception): ObsidianError {
        return when (e) {
            is SecurityException -> ObsidianError.PermissionDenied(
                message = e.message ?: "Permission denied",
                requiredPermission = "Unknown"
            )
            is java.io.IOException -> ObsidianError.IOError(
                message = e.message ?: "IO error",
                cause = e
            )
            else -> ObsidianError.Unknown(
                message = e.message ?: "Unknown error",
                cause = e
            )
        }
    }

    data class Params(val request: com.obsidianbackup.domain.restore.RestoreRequest)
}
