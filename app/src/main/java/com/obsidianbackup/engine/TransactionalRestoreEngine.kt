// engine/TransactionalRestoreEngine.kt
package com.obsidianbackup.engine

import com.obsidianbackup.engine.restore.RestoreJournal
import com.obsidianbackup.engine.restore.RestoreStep
import com.obsidianbackup.engine.restore.RestoreTransaction
import com.obsidianbackup.engine.restore.StepType
import com.obsidianbackup.model.*
import com.obsidianbackup.storage.BackupCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.io.File

class TransactionalRestoreEngine(
    private val shellExecutor: ShellExecutor,
    private val journal: RestoreJournal,
    private val catalog: BackupCatalog,
    private val backupRootPath: String,
    private val backupOrchestrator: com.obsidianbackup.domain.backup.BackupOrchestrator? = null
) : BackupEngine {

    override suspend fun restoreApps(request: RestoreRequest): RestoreResult {
        val transaction = journal.beginTransaction(BackupId(request.snapshotId.value))
        val safetyBackupRoot = File(backupRootPath, "safety_backups/${transaction.transactionId}")
        safetyBackupRoot.mkdirs()

        val restored = mutableListOf<AppId>()
        val failed = mutableListOf<AppId>()
        val warnings = mutableListOf<String>()

        try {
            val snapshotDir = File(backupRootPath, request.snapshotId.value)

            request.appIds.forEach { appId ->
                try {
                    restoreAppWithTransaction(
                        appId = appId,
                        snapshotDir = snapshotDir,
                        transaction = transaction,
                        safetyBackupRoot = safetyBackupRoot,
                        request = request
                    )
                    restored.add(appId)
                    transaction.commit(appId)

                } catch (e: Exception) {
                    failed.add(appId)
                    warnings.add("Failed to restore ${appId.value}: ${e.message}")

                    // Rollback this app
                    transaction.rollbackApp(appId, shellExecutor)
                }
            }

            journal.finalizeTransaction(transaction)

            return if (failed.isEmpty()) {
                RestoreResult.Success(
                    appsRestored = restored,
                    duration = System.currentTimeMillis() - transaction.metadata.startTime,
                    warnings = warnings
                )
            } else {
                RestoreResult.PartialSuccess(
                    appsRestored = restored,
                    appsFailed = failed,
                    duration = System.currentTimeMillis() - transaction.metadata.startTime,
                    errors = warnings
                )
            }

        } catch (e: Exception) {
            // Critical failure - rollback everything
            val rolledBack = transaction.rollbackAll(shellExecutor)
            journal.rollback(transaction)

            return RestoreResult.Failure(
                reason = "Critical restore failure: ${e.message}. Rolled back ${rolledBack.size} apps."
            )
        } finally {
            // Cleanup safety backups
            safetyBackupRoot.deleteRecursively()
        }
    }

    private suspend fun restoreAppWithTransaction(
        appId: AppId,
        snapshotDir: File,
        transaction: RestoreTransaction,
        safetyBackupRoot: File,
        request: RestoreRequest
    ) {
        val appBackupDir = File(snapshotDir, appId.value)
        val safetyBackup = File(safetyBackupRoot, appId.value)

        // Step 1: Create safety backup of current state
        transaction.log(RestoreStep(appId = appId.value, stepType = StepType.BACKUP_CURRENT_STATE))
        createCurrentStateBackup(appId, safetyBackup)
        transaction.createSafetyBackup(appId, safetyBackup)

        // Step 2: Stop app
        transaction.log(RestoreStep(appId = appId.value, stepType = StepType.STOP_APP))
        shellExecutor.execute("am force-stop ${appId.value}")

        // Step 3: Restore APK if requested
        if (BackupComponent.APK in request.components) {
            transaction.log(RestoreStep(appId = appId.value, stepType = StepType.RESTORE_APK))
            restoreApk(appId, appBackupDir)
        }

        // Step 4: Clear existing data if overwriting
        if (request.overwriteExisting) {
            transaction.log(RestoreStep(appId = appId.value, stepType = StepType.CLEAR_DATA))
            shellExecutor.execute("pm clear ${appId.value}")
        }

        // Step 5: Restore data
        if (BackupComponent.DATA in request.components) {
            transaction.log(RestoreStep(appId = appId.value, stepType = StepType.RESTORE_DATA))
            restoreData(appId, appBackupDir)
        }

        // Step 6: Restore OBB
        if (BackupComponent.OBB in request.components) {
            transaction.log(RestoreStep(appId = appId.value, stepType = StepType.RESTORE_OBB))
            restoreObb(appId, appBackupDir)
        }

        // Step 7: Restore external data
        if (BackupComponent.EXTERNAL_DATA in request.components) {
            transaction.log(RestoreStep(appId = appId.value, stepType = StepType.RESTORE_EXTERNAL))
            restoreExternalData(appId, appBackupDir)
        }

        // Step 8: Restore SELinux contexts
        transaction.log(RestoreStep(appId = appId.value, stepType = StepType.RESTORE_SELINUX))
        shellExecutor.execute("restorecon -R /data/data/${appId.value}")

        // Step 9: Restore permissions
        transaction.log(RestoreStep(appId = appId.value, stepType = StepType.RESTORE_PERMISSIONS))
        restorePermissions(appId)
    }

    private suspend fun createCurrentStateBackup(appId: AppId, destDir: File) {
        destDir.mkdirs()
        val dataPath = "/data/data/${appId.value}"
        shellExecutor.execute("cp -a $dataPath ${destDir.absolutePath}")
    }

    private suspend fun restoreApk(appId: AppId, sourceDir: File) {
        val apkFile = File(sourceDir, "base.apk")
        if (apkFile.exists()) {
            shellExecutor.execute("pm install -r ${apkFile.absolutePath}")
        }
    }

    private suspend fun restoreData(appId: AppId, sourceDir: File) {
        val archiveFile = File(sourceDir, "data.tar.zst")
        if (archiveFile.exists()) {
            val dataPath = "/data/data/${appId.value}"
            shellExecutor.execute(
                "zstd -d -c ${archiveFile.absolutePath} | busybox tar -xf - -C $dataPath"
            )
        }
    }

    private suspend fun restoreObb(appId: AppId, sourceDir: File) {
        val archiveFile = File(sourceDir, "obb.tar.zst")
        if (archiveFile.exists()) {
            val obbPath = "/storage/emulated/0/Android/obb/${appId.value}"
            shellExecutor.execute("mkdir -p $obbPath")
            shellExecutor.execute(
                "zstd -d -c ${archiveFile.absolutePath} | busybox tar -xf - -C $obbPath"
            )
        }
    }

    private suspend fun restoreExternalData(appId: AppId, sourceDir: File) {
        val archiveFile = File(sourceDir, "external.tar.zst")
        if (archiveFile.exists()) {
            val externalPath = "/storage/emulated/0/Android/data/${appId.value}"
            shellExecutor.execute("mkdir -p $externalPath")
            shellExecutor.execute(
                "zstd -d -c ${archiveFile.absolutePath} | busybox tar -xf - -C $externalPath"
            )
        }
    }

    private suspend fun restorePermissions(appId: AppId) {
        // Restore uid/gid from metadata if available
        val dataPath = "/data/data/${appId.value}"
        shellExecutor.execute("chown -R system:system $dataPath")
    }

    // Implement other BackupEngine methods...
    override suspend fun backupApps(request: BackupRequest): BackupResult {
        // Delegate to BackupOrchestrator if available
        return if (backupOrchestrator != null) {
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
        } else {
            BackupResult.Failure(
                reason = "BackupOrchestrator not available. Use BackupOrchestrator for backups.",
                appsFailed = request.appIds
            )
        }
    }

    override suspend fun verifySnapshot(id: BackupId): VerificationResult {
        // Use catalog to get snapshot metadata and perform verification
        val metadata = catalog.getSnapshotMetadata(id)
            ?: return VerificationResult(
                snapshotId = SnapshotId(id.value),
                filesChecked = 0,
                allValid = false,
                corruptedFiles = listOf("Snapshot ${id.value} not found in catalog")
            )
        
        val snapshotDir = File(backupRootPath, id.value)
        if (!snapshotDir.exists()) {
            return VerificationResult(
                snapshotId = SnapshotId(id.value),
                filesChecked = 0,
                allValid = false,
                corruptedFiles = listOf("Snapshot directory not found: ${snapshotDir.absolutePath}")
            )
        }
        
        // Basic verification: check that expected files exist
        val corruptedFiles = mutableListOf<String>()
        
        metadata.apps.forEach { appId ->
            val appDir = File(snapshotDir, appId.value)
            if (!appDir.exists()) {
                corruptedFiles.add("${appId.value}/")
            }
        }
        
        return VerificationResult(
            snapshotId = SnapshotId(id.value),
            filesChecked = metadata.apps.size,
            allValid = corruptedFiles.isEmpty(),
            corruptedFiles = corruptedFiles
        )
    }

    override suspend fun deleteSnapshot(id: BackupId): Boolean {
        // Delegate to BackupCatalog
        return try {
            catalog.deleteBackup(id)
            val snapshotDir = File(backupRootPath, id.value)
            if (snapshotDir.exists()) {
                snapshotDir.deleteRecursively()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun observeProgress(): Flow<OperationProgress> {
        return emptyFlow()
    }
}
