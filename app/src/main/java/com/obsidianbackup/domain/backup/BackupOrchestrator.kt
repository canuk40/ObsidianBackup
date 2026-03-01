// domain/backup/BackupOrchestrator.kt
package com.obsidianbackup.domain.backup

import com.obsidianbackup.billing.FeatureGateService
import com.obsidianbackup.domain.repository.ICatalogRepository
import com.obsidianbackup.engine.BackupEngine
import com.obsidianbackup.engine.IncrementalBackupStrategy
import com.obsidianbackup.engine.BackupPlan
import com.obsidianbackup.model.*
import com.obsidianbackup.model.FeatureId
import com.obsidianbackup.security.PathSecurityValidator
import com.obsidianbackup.verification.ChecksumVerifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class BackupOrchestrator(
    private val engineFactory: BackupEngineFactory,
    private val catalogRepository: ICatalogRepository,
    private val verifier: ChecksumVerifier,
    private val eventBus: BackupEventBus,
    private val incrementalStrategy: IncrementalBackupStrategy,
    private val backupRootPath: String = "/data/local/tmp/obsidianbackup",
    private val errorRecovery: com.obsidianbackup.error.ErrorRecoveryManager? = null,
    private val featureGateService: FeatureGateService? = null
) {
    
    private val _operationProgress = MutableStateFlow(
        OperationProgress(
            operationType = OperationType.BACKUP,
            currentItem = "Idle",
            itemsCompleted = 0,
            totalItems = 0
        )
    )
    val operationProgress: StateFlow<OperationProgress> = _operationProgress.asStateFlow()
    
    suspend fun executeBackup(request: BackupRequest): com.obsidianbackup.model.BackupResult {
        val engine = engineFactory.createForCurrentMode()
        val startTime = System.currentTimeMillis()
        
        Timber.tag("Backup").i("Starting backup for ${request.appIds.size} apps (incremental: ${request.incremental})")
        
        // Determine backup strategy
        val shouldUseIncremental = shouldUseIncrementalBackup(request)
        
        // Convert domain.backup.BackupRequest to model.BackupRequest
        val modelRequest = com.obsidianbackup.model.BackupRequest(
            appIds = request.appIds,
            components = request.components,
            incremental = shouldUseIncremental,
            compressionLevel = request.compressionLevel,
            encryptionEnabled = request.encryptionEnabled,
            description = request.description
        )

        return withRetry(maxAttempts = 3) {
            val result = try {
                if (shouldUseIncremental) {
                    executeIncrementalBackup(engine, modelRequest, startTime)
                } else {
                    executeFullBackup(engine, modelRequest, startTime)
                }
            } catch (e: Exception) {
                Timber.tag("Backup").e(e, "Backup failed")
                if (e is java.io.IOException || e.message?.contains("no space", ignoreCase = true) == true) {
                    try { errorRecovery?.attemptRecovery(
                        com.obsidianbackup.error.ObsidianError.InsufficientStorage(
                            message = e.message ?: "No space left",
                            requiredBytes = 0L,
                            availableBytes = 0L
                        )
                    ) } catch (re: Exception) { Timber.tag("Backup").w(re, "ErrorRecovery failed") }
                }
                return@withRetry com.obsidianbackup.model.BackupResult.Failure(
                    reason = e.message ?: "Unknown error",
                    appsFailed = request.appIds
                )
            }

            // Post-backup verification
            when (result) {
                is com.obsidianbackup.model.BackupResult.Success -> {
                    Timber.tag("Backup").i("Backup completed successfully: ${result.snapshotId.value}")
                    verifyAndCatalog(result)
                }
                is com.obsidianbackup.model.BackupResult.PartialSuccess -> {
                    Timber.tag("Backup").w("Backup partially completed: ${result.appsFailed.size} apps failed")
                }
                is com.obsidianbackup.model.BackupResult.Failure -> {
                    Timber.tag("Backup").e("Backup failed: ${result.reason}")
                }
            }

            result
        }
    }
    
    /**
     * Determine if incremental backup should be used.
     * Requires PRO subscription; falls back to full backup if gate check fails.
     */
    private suspend fun shouldUseIncrementalBackup(request: BackupRequest): Boolean {
        // User explicitly requested incremental
        if (request.incremental) {
            // Domain-layer gate: incremental backups require PRO
            if (featureGateService != null && !featureGateService.checkAccess(FeatureId.INCREMENTAL_BACKUPS)) {
                Timber.tag("BackupOrchestrator").w("Incremental backup gated — user lacks INCREMENTAL_BACKUPS feature")
                return false
            }

            // Check if we have a previous full backup for each app
            val hasBaseline = request.appIds.all { appId ->
                getLastFullBackup(appId) != null
            }
            
            if (!hasBaseline) {
                Timber.tag("BackupOrchestrator").i("No baseline found - forcing full backup")
                return false
            }
            
            return true
        }
        
        return false
    }
    
    /**
     * Get last full backup for an app
     */
    private suspend fun getLastFullBackup(appId: AppId): BackupId? {
        return withContext(Dispatchers.IO) {
            catalogRepository.getLastFullBackupForApp(appId)
        }
    }
    
    /**
     * Execute full backup
     */
    private suspend fun executeFullBackup(
        engine: BackupEngine,
        request: com.obsidianbackup.model.BackupRequest,
        startTime: Long
    ): com.obsidianbackup.model.BackupResult {
        
        updateProgress("Preparing full backup", 0, request.appIds.size)
        
        val result = engine.backupApps(request)
        
        // Add stats
        return when (result) {
            is com.obsidianbackup.model.BackupResult.Success -> {
                val duration = System.currentTimeMillis() - startTime
                result.copy(
                    duration = duration,
                    incrementalStats = IncrementalStats(
                        isIncremental = false,
                        baseSnapshotId = null,
                        filesScanned = 0,
                        filesChanged = 0,
                        filesUnchanged = 0,
                        filesDeleted = 0,
                        filesDeduped = 0,
                        deltaSize = result.totalSize,
                        savedSize = 0,
                        hardLinksCreated = 0
                    )
                )
            }
            is com.obsidianbackup.model.BackupResult.PartialSuccess -> {
                val duration = System.currentTimeMillis() - startTime
                result.copy(duration = duration)
            }
            is com.obsidianbackup.model.BackupResult.Failure -> result
        }
    }
    
    /**
     * Execute incremental backup with strategy integration
     */
    private suspend fun executeIncrementalBackup(
        engine: BackupEngine,
        request: com.obsidianbackup.model.BackupRequest,
        startTime: Long
    ): com.obsidianbackup.model.BackupResult {
        
        updateProgress("Scanning for changes", 0, request.appIds.size)
        
        // Generate incremental backup plans for each app
        val plans = mutableMapOf<AppId, BackupPlan>()
        var totalFilesScanned = 0
        var totalFilesChanged = 0
        var totalFilesUnchanged = 0
        var totalFilesDeleted = 0
        
        request.appIds.forEachIndexed { index, appId ->
            updateProgress("Scanning ${appId.value}", index, request.appIds.size)
            
            val lastBackupId = getLastFullBackup(appId)
            val plan = incrementalStrategy.createIncremental(appId, lastBackupId)
            plans[appId] = plan
            
            when (plan) {
                is BackupPlan.Full -> {
                    totalFilesScanned += plan.files.size
                    totalFilesChanged += plan.files.size
                    Timber.tag("IncrementalBackup").i("App ${appId.value}: Full backup (${plan.files.size} files)")
                }
                is BackupPlan.Incremental -> {
                    totalFilesScanned += plan.changedFiles.size + plan.unchangedFiles.size
                    totalFilesChanged += plan.changedFiles.size
                    totalFilesUnchanged += plan.unchangedFiles.size
                    
                    // Detect deleted files by comparing with base snapshot
                    val deletedFiles = incrementalStrategy.detectDeletedFiles(
                        plan.changedFiles + plan.unchangedFiles,
                        plan.baseSnapshot
                    )
                    totalFilesDeleted += deletedFiles.size
                    
                    Timber.tag("IncrementalBackup").i(
                        "App ${appId.value}: Incremental backup (${plan.changedFiles.size} changed, ${plan.unchangedFiles.size} unchanged, ${deletedFiles.size} deleted)"
                    )
                }
            }
        }
        
        // Execute backup with plans
        updateProgress("Backing up files", 0, totalFilesChanged)
        
        val snapshotId = SnapshotId("snapshot_${System.currentTimeMillis()}")
        val snapshotDir = File(backupRootPath, snapshotId.value)
        snapshotDir.mkdirs()
        
        // Execute each app's backup plan
        var totalBytesCopied = 0L
        val errors = mutableListOf<String>()
        
        plans.forEach { (appId, plan) ->
            try {
                val appSourceDir = PathSecurityValidator.getAppDataDirectory(appId)
                val appTargetDir = File(snapshotDir, appId.value)
                
                val execResult = incrementalStrategy.executeBackupPlan(plan, appSourceDir, appTargetDir)
                totalBytesCopied += execResult.bytesCopied
                errors.addAll(execResult.errors)
                
                Timber.tag("IncrementalBackup").i(
                    "App ${appId.value}: ${execResult.filesProcessed} files, ${execResult.bytesCopied} bytes copied"
                )
            } catch (e: Exception) {
                errors.add("Failed to backup ${appId.value}: ${e.message}")
                Timber.tag("IncrementalBackup").e(e, "Failed to backup app: ${appId.value}")
            }
        }
        
        // Get final stats
        val stats = incrementalStrategy.getStats()
        val duration = System.currentTimeMillis() - startTime
        
        Timber.tag("IncrementalBackup").i(
            "Backup complete: ${stats.hardLinksCreated} hard links, ${stats.filesDeduped} deduped, saved ${stats.savedBytes} bytes"
        )
        
        // Determine result type
        return if (errors.isEmpty()) {
            com.obsidianbackup.model.BackupResult.Success(
                snapshotId = snapshotId,
                timestamp = System.currentTimeMillis(),
                appsBackedUp = request.appIds,
                totalSize = totalBytesCopied,
                duration = duration,
                incrementalStats = IncrementalStats(
                    isIncremental = true,
                    baseSnapshotId = plans.values.firstOrNull { it is BackupPlan.Incremental }
                        ?.let { (it as BackupPlan.Incremental).baseSnapshot }
                        ?.let { SnapshotId(it.value) },
                    filesScanned = totalFilesScanned,
                    filesChanged = totalFilesChanged,
                    filesUnchanged = totalFilesUnchanged,
                    filesDeleted = totalFilesDeleted,
                    filesDeduped = stats.filesDeduped,
                    deltaSize = totalBytesCopied,
                    savedSize = stats.savedBytes,
                    hardLinksCreated = stats.hardLinksCreated
                )
            )
        } else {
            com.obsidianbackup.model.BackupResult.PartialSuccess(
                snapshotId = snapshotId,
                timestamp = System.currentTimeMillis(),
                appsBackedUp = request.appIds.filterNot { appId ->
                    errors.any { it.contains(appId.value) }
                },
                appsFailed = request.appIds.filter { appId ->
                    errors.any { it.contains(appId.value) }
                },
                totalSize = totalBytesCopied,
                duration = duration,
                errors = errors,
                incrementalStats = IncrementalStats(
                    isIncremental = true,
                    baseSnapshotId = plans.values.firstOrNull { it is BackupPlan.Incremental }
                        ?.let { (it as BackupPlan.Incremental).baseSnapshot }
                        ?.let { SnapshotId(it.value) },
                    filesScanned = totalFilesScanned,
                    filesChanged = totalFilesChanged,
                    filesUnchanged = totalFilesUnchanged,
                    filesDeleted = 0,
                    filesDeduped = stats.filesDeduped,
                    deltaSize = totalBytesCopied,
                    savedSize = stats.savedBytes,
                    hardLinksCreated = stats.hardLinksCreated
                )
            )
        }
    }
    
    /**
     * Update operation progress
     */
    private fun updateProgress(item: String, completed: Int, total: Int) {
        _operationProgress.value = OperationProgress(
            operationType = OperationType.BACKUP,
            currentItem = item,
            itemsCompleted = completed,
            totalItems = total
        )
    }

        private suspend fun verifyAndCatalog(result: com.obsidianbackup.model.BackupResult.Success) {
        // Save snapshot to catalog first
        val metadata = com.obsidianbackup.storage.BackupMetadata(
            snapshotId = com.obsidianbackup.model.BackupId(result.snapshotId.value),
            timestamp = result.timestamp,
            description = null,
            apps = result.appsBackedUp,
            components = setOf(BackupComponent.APK, BackupComponent.DATA),
            compressionLevel = 6,
            encrypted = false,
            permissionMode = "ROOT",
            deviceInfo = DeviceInfo(
                model = android.os.Build.MODEL,
                manufacturer = android.os.Build.MANUFACTURER,
                androidVersion = android.os.Build.VERSION.SDK_INT,
                buildFingerprint = android.os.Build.FINGERPRINT
            ),
            totalSize = result.totalSize,
            checksums = result.checksums,
            path = "$backupRootPath/${result.snapshotId.value}"
        )
        catalogRepository.saveSnapshot(metadata)
        Timber.tag("Catalog").i("Saved snapshot to catalog: ${result.snapshotId.value}")

        // Verify
        Timber.tag("Verify").i("Verifying snapshot: ${result.snapshotId.value}")
        val verification = engineFactory.createForCurrentMode().verifySnapshot(com.obsidianbackup.model.BackupId(result.snapshotId.value))
        catalogRepository.markVerified(result.snapshotId, verification.allValid)
        
        if (verification.allValid) {
            Timber.tag("Verify").i("Verification passed for ${result.snapshotId.value}")
        } else {
            Timber.tag("Verify").w("Verification failed: ${verification.corruptedFiles.size} corrupted files")
        }
    }
    
    /**
     * Get backup metadata by ID
     */
    suspend fun getBackupMetadata(backupId: BackupId): com.obsidianbackup.model.BackupMetadata? {
        return withContext(Dispatchers.IO) {
            catalogRepository.getBackupMetadata(backupId)
        }
    }
    
    /**
     * Execute backup for a backup profile
     * 
     * @param profile The backup profile to execute
     * @return BackupResult indicating success, partial success, or failure
     */
    suspend fun executeProfileBackup(profile: BackupProfile): com.obsidianbackup.model.BackupResult {
        Timber.tag("ProfileBackup").i("Executing profile backup: ${profile.name} (${profile.appIds.size} apps)")
        
        if (profile.appIds.isEmpty()) {
            Timber.tag("ProfileBackup").w("Profile ${profile.name} has no apps - skipping")
            return com.obsidianbackup.model.BackupResult.Failure(
                reason = "Profile has no apps to backup",
                appsFailed = emptyList()
            )
        }
        
        // Create backup request from profile
        val request = BackupRequest(
            appIds = profile.appIds,
            components = profile.components,
            incremental = profile.incremental,
            compressionLevel = profile.compressionLevel,
            encryptionEnabled = profile.encryptionEnabled,
            description = "Profile: ${profile.name}"
        )
        
        // Execute backup
        return executeBackup(request)
    }
    
    /**
     * Delete a backup by ID
     */
    suspend fun deleteBackup(backupId: BackupId) {
        withContext(Dispatchers.IO) {
            catalogRepository.deleteBackup(backupId)
            val backupDir = File(backupRootPath, backupId.value)
            if (backupDir.exists()) {
                backupDir.deleteRecursively()
            }
        }
    }
}

// Retry mechanism with exponential backoff
private suspend fun <T> withRetry(
    maxAttempts: Int,
    initialDelay: Long = 1000,
    maxDelay: Long = 10000,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxAttempts - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            Timber.tag("Retry").w(e, "Attempt ${attempt + 1} failed, retrying after ${currentDelay}ms")
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }
    
    // Last attempt with error handling
    return try {
        block()
    } catch (e: Exception) {
        Timber.tag("Retry").e(e, "Final attempt (${maxAttempts}) failed")
        throw e
    }
}

    
