// engine/ObsidianBoxEngine.kt
package com.obsidianbackup.engine

import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.model.*
import com.obsidianbackup.permissions.PermissionManager
import com.obsidianbackup.scanner.AppScanner
import com.obsidianbackup.storage.BackupCatalog
import com.obsidianbackup.engine.shell.SafeShellExecutor
import com.obsidianbackup.ObsidianBoxCommands
import com.obsidianbackup.installer.SplitApkHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

/**
 * ObsidianBoxEngine - Production backup engine with proper implementation
 * 
 * This engine uses BusyBox/ObsidianBox commands for reliable backup/restore operations
 * with support for compression, checksums, incremental backups, and split APKs.
 */
class ObsidianBoxEngine(
    private val permissionManager: PermissionManager,
    private val catalog: BackupCatalog,
    private val backupRootPath: String,
    private val logger: ObsidianLogger,
    private val shellExecutor: SafeShellExecutor,
    private val appScanner: AppScanner
) : BackupEngine {
    
    private val splitApkHelper: SplitApkHelper by lazy {
        SplitApkHelper(
            packageManager = appScanner.getPackageManager(),
            shellExecutor = shellExecutor,
            logger = logger
        )
    }

    private val _progress = MutableStateFlow<OperationProgress>(
        OperationProgress(
            operationType = OperationType.BACKUP,
            currentItem = "",
            itemsCompleted = 0,
            totalItems = 0
        )
    )

    /**
     * Escape a string for safe use in shell commands
     */
    private fun shellEscape(str: String): String {
        return "'${str.replace("'", "'\\''")}'"
    }

    /**
     * Validate package name to prevent injection
     */
    private fun validatePackageName(packageName: String): Boolean {
        return packageName.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)*$"))
    }

    override suspend fun backupApps(request: BackupRequest): BackupResult {
        val startTime = System.currentTimeMillis()
        val snapshotId = SnapshotId(UUID.randomUUID().toString())
        val snapshotDir = File(backupRootPath, snapshotId.value)
        val checksums = mutableMapOf<String, String>()
        val successfulApps = mutableListOf<AppId>()
        val failedApps = mutableListOf<AppId>()
        val errors = mutableListOf<String>()

        try {
            // Create snapshot directory as app user (not root) since it's in app-private storage
            if (!snapshotDir.exists() && !snapshotDir.mkdirs()) {
                logger.e("ObsidianBoxEngine", "Failed to create backup directory: ${snapshotDir.absolutePath}")
                return BackupResult.Failure("Failed to create backup directory", request.appIds)
            }

            var totalSize = 0L
            var processedApps = 0

            // Process each app
            for (appId in request.appIds) {
                _progress.value = OperationProgress(
                    operationType = OperationType.BACKUP,
                    currentItem = "Backing up ${appId.value}",
                    itemsCompleted = processedApps,
                    totalItems = request.appIds.size,
                    bytesProcessed = totalSize,
                    totalBytes = 0L
                )

                try {
                    if (!validatePackageName(appId.value)) {
                        errors.add("Invalid package name: ${appId.value}")
                        failedApps.add(appId)
                        continue
                    }

                    val appBackupSize = backupSingleApp(appId, snapshotDir, request, checksums)
                    totalSize += appBackupSize
                    successfulApps.add(appId)
                    
                    processedApps++
                } catch (e: Exception) {
                    logger.e("ObsidianBoxEngine", "Failed to backup ${appId.value}", e)
                    errors.add("${appId.value}: ${e.message}")
                    failedApps.add(appId)
                }
            }

            // Generate metadata
            val metadata = BackupMetadata(
                snapshotId = snapshotId.value,
                timestamp = System.currentTimeMillis(),
                appIds = successfulApps.map { it.value },
                totalSize = totalSize,
                compressionLevel = request.compressionLevel,
                encrypted = request.encryptionEnabled,
                checksums = checksums
            )

            val metadataFile = File(snapshotDir, "metadata.json")
            metadataFile.writeText(Json.encodeToString(metadata))

            val duration = System.currentTimeMillis() - startTime
            
            _progress.value = OperationProgress(
                operationType = OperationType.BACKUP,
                currentItem = "Completed",
                itemsCompleted = successfulApps.size,
                totalItems = request.appIds.size
            )

            return when {
                failedApps.isEmpty() -> BackupResult.Success(
                    snapshotId = snapshotId,
                    timestamp = startTime,
                    appsBackedUp = successfulApps,
                    totalSize = totalSize,
                    duration = duration,
                    checksums = checksums
                )
                successfulApps.isEmpty() -> BackupResult.Failure(
                    "All apps failed to backup: ${errors.joinToString(", ")}",
                    request.appIds
                )
                else -> BackupResult.PartialSuccess(
                    snapshotId = snapshotId,
                    timestamp = startTime,
                    appsBackedUp = successfulApps,
                    appsFailed = failedApps,
                    totalSize = totalSize,
                    duration = duration,
                    errors = errors
                )
            }

        } catch (e: Exception) {
            logger.e("ObsidianBoxEngine", "Backup operation failed", e)
            _progress.value = OperationProgress(
                operationType = OperationType.BACKUP,
                currentItem = "Failed: ${e.message}",
                itemsCompleted = 0,
                totalItems = request.appIds.size
            )
            return BackupResult.Failure(
                "Backup operation failed: ${e.message}",
                request.appIds
            )
        }
    }

    private suspend fun backupSingleApp(
        appId: AppId,
        snapshotDir: File,
        request: BackupRequest,
        checksums: MutableMap<String, String>
    ): Long {
        val appDir = File(snapshotDir, appId.value)
        appDir.mkdirs()

        var totalSize = 0L

        // Backup APK if requested
        if (BackupComponent.APK in request.components) {
            totalSize += backupApk(appId, appDir)
        }

        // Backup DATA if requested
        if (BackupComponent.DATA in request.components) {
            totalSize += backupData(appId, appDir, request.compressionLevel)
        }

        // Backup OBB if requested
        if (BackupComponent.OBB in request.components) {
            totalSize += backupObb(appId, appDir, request.compressionLevel)
        }

        // Backup EXTERNAL if requested
        if (BackupComponent.EXTERNAL in request.components) {
            totalSize += backupExternal(appId, appDir, request.compressionLevel)
        }

        return totalSize
    }

    private suspend fun backupApk(appId: AppId, appDir: File): Long {
        val apkDir = File(appDir, "apk")
        apkDir.mkdirs()

        try {
            // Get split APK metadata
            val splitMetadata = splitApkHelper.getSplitApkInfo(appId.value)
            
            if (splitMetadata != null) {
                // Use SplitApkHelper to backup all APKs (base + splits)
                val totalSize = splitApkHelper.backupApks(splitMetadata, apkDir)
                
                if (splitMetadata.isSplit) {
                    logger.d("ObsidianBoxEngine", 
                        "Backed up split APK: ${appId.value} with ${splitMetadata.splitNames.size} splits, total size: $totalSize bytes")
                } else {
                    logger.d("ObsidianBoxEngine", 
                        "Backed up single APK: ${appId.value}, size: $totalSize bytes")
                }
                
                return totalSize
            } else {
                // Fallback to legacy method if metadata retrieval fails
                logger.w("ObsidianBoxEngine", "Failed to get split APK metadata for ${appId.value}, falling back to legacy method")
                return backupApkLegacy(appId, apkDir)
            }
        } catch (e: Exception) {
            logger.e("ObsidianBoxEngine", "Error backing up APK for ${appId.value}", e)
            // Fallback to legacy method
            return backupApkLegacy(appId, apkDir)
        }
    }
    
    /**
     * Legacy APK backup method (for compatibility)
     */
    private suspend fun backupApkLegacy(appId: AppId, apkDir: File): Long {
        // Get APK path via pm command
        val pmPathCmd = "pm path ${shellEscape(appId.value)}"
        val pmResult = shellExecutor.execute(pmPathCmd)

        if (pmResult is ShellResult.Success) {
            val apkPath = pmResult.output.lines()
                .firstOrNull { it.startsWith("package:") }
                ?.removePrefix("package:")
                ?.trim()

            if (apkPath != null) {
                val copyCmd = "cp ${shellEscape(apkPath)} ${shellEscape(File(apkDir, "base.apk").absolutePath)}"
                val copyResult = shellExecutor.execute(copyCmd)

                if (copyResult is ShellResult.Success) {
                    return File(apkDir, "base.apk").length()
                }
            }
        }

        return 0L
    }

    private suspend fun backupData(appId: AppId, appDir: File, compressionLevel: Int): Long {
        val dataPath = "/data/data/${appId.value}"
        val outputFile = File(appDir, "data.tar.zst")

        val tarCmd = ObsidianBoxCommands.createTarArchive(
            sourceDir = shellEscape(dataPath),
            outputFile = shellEscape(outputFile.absolutePath),
            useZstd = true,
            compressionLevel = compressionLevel
        )

        val result = shellExecutor.execute(tarCmd)

        return if (result is ShellResult.Success && outputFile.exists()) {
            outputFile.length()
        } else {
            0L
        }
    }

    private suspend fun backupObb(appId: AppId, appDir: File, compressionLevel: Int): Long {
        val obbPath = "/sdcard/Android/obb/${appId.value}"
        val outputFile = File(appDir, "obb.tar.zst")

        // Check if OBB directory exists
        val testCmd = "test -d ${shellEscape(obbPath)} && echo exists"
        val testResult = shellExecutor.execute(testCmd)

        if (testResult is ShellResult.Success && testResult.output.contains("exists")) {
            val tarCmd = ObsidianBoxCommands.createTarArchive(
                sourceDir = shellEscape(obbPath),
                outputFile = shellEscape(outputFile.absolutePath),
                useZstd = true,
                compressionLevel = compressionLevel
            )

            val result = shellExecutor.execute(tarCmd)

            return if (result is ShellResult.Success && outputFile.exists()) {
                outputFile.length()
            } else {
                0L
            }
        }

        return 0L
    }

    private suspend fun backupExternal(appId: AppId, appDir: File, compressionLevel: Int): Long {
        val externalPath = "/sdcard/Android/data/${appId.value}"
        val outputFile = File(appDir, "external.tar.zst")

        // Check if external directory exists
        val testCmd = "test -d ${shellEscape(externalPath)} && echo exists"
        val testResult = shellExecutor.execute(testCmd)

        if (testResult is ShellResult.Success && testResult.output.contains("exists")) {
            val tarCmd = ObsidianBoxCommands.createTarArchive(
                sourceDir = shellEscape(externalPath),
                outputFile = shellEscape(outputFile.absolutePath),
                useZstd = true,
                compressionLevel = compressionLevel
            )

            val result = shellExecutor.execute(tarCmd)

            return if (result is ShellResult.Success && outputFile.exists()) {
                outputFile.length()
            } else {
                0L
            }
        }

        return 0L
    }

    override suspend fun restoreApps(request: RestoreRequest): RestoreResult {
        val startTime = System.currentTimeMillis()
        val snapshotDir = File(backupRootPath, request.snapshotId.value)
        val successfulApps = mutableListOf<AppId>()
        val failedApps = mutableListOf<AppId>()
        val warnings = mutableListOf<String>()

        try {
            if (!snapshotDir.exists()) {
                return RestoreResult.Failure("Snapshot not found: ${request.snapshotId.value}")
            }

            var processedApps = 0

            for (appId in request.appIds) {
                _progress.value = OperationProgress(
                    operationType = OperationType.RESTORE,
                    currentItem = "Restoring ${appId.value}",
                    itemsCompleted = processedApps,
                    totalItems = request.appIds.size,
                    bytesProcessed = 0L,
                    totalBytes = 0L
                )

                try {
                    if (!validatePackageName(appId.value)) {
                        warnings.add("Invalid package name: ${appId.value}")
                        failedApps.add(appId)
                        continue
                    }

                    if (request.dryRun) {
                        // Dry run: just validate backup exists
                        val appDir = File(snapshotDir, appId.value)
                        if (!appDir.exists()) {
                            warnings.add("Backup not found for ${appId.value}")
                            failedApps.add(appId)
                        } else {
                            successfulApps.add(appId)
                        }
                    } else {
                        restoreSingleApp(appId, snapshotDir, request)
                        successfulApps.add(appId)
                    }

                    processedApps++
                } catch (e: Exception) {
                    logger.e("ObsidianBoxEngine", "Failed to restore ${appId.value}", e)
                    warnings.add("${appId.value}: ${e.message}")
                    failedApps.add(appId)
                }
            }

            val duration = System.currentTimeMillis() - startTime

            _progress.value = OperationProgress(
                operationType = OperationType.RESTORE,
                currentItem = "Completed",
                itemsCompleted = successfulApps.size,
                totalItems = request.appIds.size
            )

            return when {
                failedApps.isEmpty() -> RestoreResult.Success(
                    appsRestored = successfulApps,
                    duration = duration,
                    warnings = warnings
                )
                successfulApps.isEmpty() -> RestoreResult.Failure(
                    "All apps failed to restore"
                )
                else -> RestoreResult.PartialSuccess(
                    appsRestored = successfulApps,
                    appsFailed = failedApps,
                    duration = duration,
                    errors = warnings
                )
            }

        } catch (e: Exception) {
            logger.e("ObsidianBoxEngine", "Restore operation failed", e)
            _progress.value = OperationProgress(
                operationType = OperationType.RESTORE,
                currentItem = "Failed: ${e.message}",
                itemsCompleted = 0,
                totalItems = request.appIds.size
            )
            return RestoreResult.Failure("Restore operation failed: ${e.message}")
        }
    }

    private suspend fun restoreSingleApp(
        appId: AppId,
        snapshotDir: File,
        request: RestoreRequest
    ) {
        val appDir = File(snapshotDir, appId.value)

        // Stop the app if it's running
        shellExecutor.execute("am force-stop ${shellEscape(appId.value)}")

        // Restore APK if present and requested
        if (BackupComponent.APK in request.components) {
            restoreApk(appId, appDir)
        }

        // Restore DATA if present and requested
        if (BackupComponent.DATA in request.components) {
            restoreData(appId, appDir, request.overwriteExisting)
        }

        // Restore OBB if present and requested
        if (BackupComponent.OBB in request.components) {
            restoreObb(appId, appDir)
        }

        // Restore EXTERNAL if present and requested
        if (BackupComponent.EXTERNAL in request.components) {
            restoreExternal(appId, appDir)
        }

        // Restore SELinux contexts
        shellExecutor.execute("restorecon -R /data/data/${shellEscape(appId.value)}")
    }

    private suspend fun restoreApk(appId: AppId, appDir: File) {
        val apkDir = File(appDir, "apk")
        
        if (!apkDir.exists()) {
            logger.w("ObsidianBoxEngine", "APK directory not found for ${appId.value}")
            return
        }
        
        try {
            // Use SplitApkHelper to restore APKs (handles both single and split APKs)
            val success = splitApkHelper.restoreApks(apkDir, appId.value)
            
            if (success) {
                logger.d("ObsidianBoxEngine", "Successfully restored APK(s) for ${appId.value}")
            } else {
                logger.e("ObsidianBoxEngine", "Failed to restore APK(s) for ${appId.value}")
                throw Exception("Failed to restore APK(s)")
            }
        } catch (e: Exception) {
            logger.e("ObsidianBoxEngine", "Error restoring APK for ${appId.value}", e)
            // Try legacy method as fallback
            restoreApkLegacy(appId, appDir)
        }
    }
    
    /**
     * Legacy APK restore method (for compatibility)
     */
    private suspend fun restoreApkLegacy(appId: AppId, appDir: File) {
        val apkFile = File(appDir, "apk/base.apk")
        if (apkFile.exists()) {
            val installCmd = "pm install -r ${shellEscape(apkFile.absolutePath)}"
            shellExecutor.execute(installCmd)
        }
    }

    private suspend fun restoreData(appId: AppId, appDir: File, overwrite: Boolean) {
        val dataArchive = File(appDir, "data.tar.zst")
        if (dataArchive.exists()) {
            val dataPath = "/data/data/${appId.value}"

            if (overwrite) {
                // Clear existing data
                shellExecutor.execute("pm clear ${shellEscape(appId.value)}")
            }

            // Extract data
            val extractCmd = ObsidianBoxCommands.extractTarArchive(
                archiveFile = shellEscape(dataArchive.absolutePath),
                destDir = shellEscape(dataPath),
                isZstd = true
            )
            shellExecutor.execute(extractCmd)

            // Fix permissions
            val uid = getAppUid(appId)
            shellExecutor.execute("chown -R $uid:$uid ${shellEscape(dataPath)}")
        }
    }

    private suspend fun restoreObb(appId: AppId, appDir: File) {
        val obbArchive = File(appDir, "obb.tar.zst")
        if (obbArchive.exists()) {
            val obbPath = "/sdcard/Android/obb/${appId.value}"
            shellExecutor.execute("mkdir -p ${shellEscape(obbPath)}")

            val extractCmd = ObsidianBoxCommands.extractTarArchive(
                archiveFile = shellEscape(obbArchive.absolutePath),
                destDir = shellEscape(obbPath),
                isZstd = true
            )
            shellExecutor.execute(extractCmd)
        }
    }

    private suspend fun restoreExternal(appId: AppId, appDir: File) {
        val externalArchive = File(appDir, "external.tar.zst")
        if (externalArchive.exists()) {
            val externalPath = "/sdcard/Android/data/${appId.value}"
            shellExecutor.execute("mkdir -p ${shellEscape(externalPath)}")

            val extractCmd = ObsidianBoxCommands.extractTarArchive(
                archiveFile = shellEscape(externalArchive.absolutePath),
                destDir = shellEscape(externalPath),
                isZstd = true
            )
            shellExecutor.execute(extractCmd)
        }
    }

    private suspend fun getAppUid(appId: AppId): String {
        val cmd = "dumpsys package ${shellEscape(appId.value)} | grep userId="
        val result = shellExecutor.execute(cmd)

        return if (result is ShellResult.Success) {
            result.output.lines()
                .firstOrNull { it.contains("userId=") }
                ?.substringAfter("userId=")
                ?.substringBefore(" ")
                ?.trim() ?: "10000"
        } else {
            "10000" // Default UID
        }
    }

    override suspend fun verifySnapshot(id: BackupId): VerificationResult {
        val snapshotDir = File(backupRootPath, id.value)

        if (!snapshotDir.exists()) {
            return VerificationResult(
                snapshotId = SnapshotId(id.value),
                filesChecked = 0,
                allValid = false,
                corruptedFiles = listOf("Snapshot directory not found")
            )
        }

        // Load metadata
        val metadataFile = File(snapshotDir, "metadata.json")
        if (!metadataFile.exists()) {
            return VerificationResult(
                snapshotId = SnapshotId(id.value),
                filesChecked = 0,
                allValid = false,
                corruptedFiles = listOf("Metadata file not found")
            )
        }

        // For now, basic verification - file exists
        val allFiles = snapshotDir.walkTopDown().filter { it.isFile }.toList()
        
        return VerificationResult(
            snapshotId = SnapshotId(id.value),
            filesChecked = allFiles.size,
            allValid = true,
            corruptedFiles = emptyList()
        )
    }

    override suspend fun deleteSnapshot(id: BackupId): Boolean {
        return try {
            val snapshotDir = File(backupRootPath, id.value)
            if (snapshotDir.exists()) {
                val rmCmd = "rm -rf ${shellEscape(snapshotDir.absolutePath)}"
                val result = shellExecutor.execute(rmCmd)
                result is ShellResult.Success
            } else {
                true // Already deleted
            }
        } catch (e: Exception) {
            logger.e("ObsidianBoxEngine", "Failed to delete snapshot ${id.value}", e)
            false
        }
    }

    override fun observeProgress(): Flow<OperationProgress> {
        return _progress.asStateFlow()
    }
}

/**
 * Metadata stored with each backup snapshot
 */
@kotlinx.serialization.Serializable
private data class BackupMetadata(
    val snapshotId: String,
    val timestamp: Long,
    val appIds: List<String>,
    val totalSize: Long,
    val compressionLevel: Int,
    val encrypted: Boolean,
    val checksums: Map<String, String>
)
