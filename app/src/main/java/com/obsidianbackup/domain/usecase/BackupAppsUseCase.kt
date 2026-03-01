// domain/usecase/BackupAppsUseCase.kt
package com.obsidianbackup.domain.usecase

import com.obsidianbackup.billing.FeatureGateService
import com.obsidianbackup.domain.backup.BackupOrchestrator
import com.obsidianbackup.error.*
import com.obsidianbackup.model.BackupRequest
import com.obsidianbackup.model.BackupResult
import com.obsidianbackup.model.FeatureId
import javax.inject.Inject

class BackupAppsUseCase @Inject constructor(
    private val backupOrchestrator: BackupOrchestrator,
    private val retryStrategy: RetryStrategy,
    private val errorRecovery: ErrorRecoveryManager,
    private val featureGateService: FeatureGateService
) {
    suspend operator fun invoke(request: BackupRequest): BackupResult {
        // Domain-layer gate: incremental backups require PRO
        if (request.incremental && !featureGateService.checkAccess(FeatureId.INCREMENTAL_BACKUPS)) {
            return BackupResult.Failure(
                reason = "Incremental backups require a Pro subscription",
                appsFailed = request.appIds
            )
        }
        // Domain-layer gate: encryption requires PRO
        if (request.encryptionEnabled && !featureGateService.checkAccess(FeatureId.ENCRYPTION)) {
            return BackupResult.Failure(
                reason = "Backup encryption requires a Pro subscription",
                appsFailed = request.appIds
            )
        }

        return try {
            // Convert model.BackupRequest to domain.backup.BackupRequest
            val domainRequest = com.obsidianbackup.domain.backup.BackupRequest(
                appIds = request.appIds,
                components = request.components,
                incremental = request.incremental,
                compressionLevel = request.compressionLevel,
                encryptionEnabled = request.encryptionEnabled,
                description = request.description
            )
            backupOrchestrator.executeBackup(domainRequest)
        } catch (e: Exception) {
            val error = mapExceptionToError(e)
            
            // Attempt automatic recovery
            when (val recovery = errorRecovery.attemptRecovery(error)) {
                is RecoveryResult.Recovered -> {
                    // Retry after successful recovery
                    val domainRequest = com.obsidianbackup.domain.backup.BackupRequest(
                        appIds = request.appIds,
                        components = request.components,
                        incremental = request.incremental,
                        compressionLevel = request.compressionLevel,
                        encryptionEnabled = request.encryptionEnabled,
                        description = request.description
                    )
                    backupOrchestrator.executeBackup(domainRequest)
                }
                is RecoveryResult.RequiresUserAction,
                is RecoveryResult.CannotRecover -> {
                    // Return failure result
                    BackupResult.Failure(
                        reason = error.message,
                        appsFailed = request.appIds
                    )
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
}
