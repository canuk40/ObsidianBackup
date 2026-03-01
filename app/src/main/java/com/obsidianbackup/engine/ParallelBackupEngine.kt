// engine/ParallelBackupEngine.kt
package com.obsidianbackup.engine

import com.obsidianbackup.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.UUID

class ParallelBackupEngine(
    private val executor: ShellExecutor,
    private val backupRootPath: String,
    private val maxConcurrency: Int = 4
) {
    suspend fun backupApps(request: BackupRequest): BackupResult = coroutineScope {
        val startTime = System.currentTimeMillis()
        val snapshotId = SnapshotId(UUID.randomUUID().toString())
        val snapshotDir = File(backupRootPath, snapshotId.value)
        
        snapshotDir.mkdirs()
        
        val semaphore = Semaphore(maxConcurrency)
        val results = request.appIds.map { appId ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    backupSingleApp(appId, request, snapshotDir)
                }
            }
        }.awaitAll()

        aggregateResults(results, snapshotId, startTime, request.appIds)
    }

    private suspend fun backupSingleApp(
        appId: AppId,
        request: BackupRequest,
        snapshotDir: File
    ): AppBackupResult {
        return try {
            val appDir = File(snapshotDir, appId.value)
            appDir.mkdirs()
            
            var totalSize = 0L
            val checksums = mutableMapOf<String, String>()
            
            if (BackupComponent.APK in request.components) {
                val apkFile = File(appDir, "${appId.value}.apk")
                val result = executor.execute("pm path ${appId.value}")
                if (result is ShellResult.Success) {
                    val apkPath = result.output.trim().removePrefix("package:")
                    executor.execute("cp $apkPath ${apkFile.absolutePath}")
                    totalSize += apkFile.length()
                }
            }
            
            if (BackupComponent.DATA in request.components) {
                val dataArchive = File(appDir, "data.tar.zst")
                val dataDir = "/data/data/${appId.value}"
                val tarCmd = "tar -cf - -C $dataDir . | zstd -${request.compressionLevel} -T0 > ${dataArchive.absolutePath}"
                executor.execute(tarCmd)
                totalSize += dataArchive.length()
            }
            
            AppBackupResult.Success(appId, totalSize, checksums)
        } catch (e: Exception) {
            AppBackupResult.Failure(appId, e.message ?: "Unknown error")
        }
    }

    private fun aggregateResults(
        results: List<AppBackupResult>,
        snapshotId: SnapshotId,
        startTime: Long,
        requestedApps: List<AppId>
    ): BackupResult {
        val successResults = results.filterIsInstance<AppBackupResult.Success>()
        val failureResults = results.filterIsInstance<AppBackupResult.Failure>()
        
        val totalSize = successResults.sumOf { it.size }
        val duration = System.currentTimeMillis() - startTime
        val allChecksums = successResults.flatMap { it.checksums.entries }.associate { it.key to it.value }
        
        return when {
            failureResults.isEmpty() -> BackupResult.Success(
                snapshotId = snapshotId,
                timestamp = startTime,
                appsBackedUp = successResults.map { it.appId },
                totalSize = totalSize,
                duration = duration,
                checksums = allChecksums
            )
            successResults.isEmpty() -> BackupResult.Failure(
                reason = "All apps failed to backup",
                appsFailed = requestedApps
            )
            else -> BackupResult.PartialSuccess(
                snapshotId = snapshotId,
                timestamp = startTime,
                appsBackedUp = successResults.map { it.appId },
                appsFailed = failureResults.map { it.appId },
                totalSize = totalSize,
                duration = duration,
                errors = failureResults.map { "${it.appId.value}: ${it.error}" }
            )
        }
    }
}

sealed class AppBackupResult {
    data class Success(
        val appId: AppId,
        val size: Long,
        val checksums: Map<String, String>
    ) : AppBackupResult()
    
    data class Failure(
        val appId: AppId,
        val error: String
    ) : AppBackupResult()
}
