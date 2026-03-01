// engine/restore/RestoreTransaction.kt
package com.obsidianbackup.engine.restore

import com.obsidianbackup.model.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

@Serializable
data class RestoreStep(
    val stepId: String = UUID.randomUUID().toString(),
    val appId: String,
    val stepType: StepType,
    val timestamp: Long = System.currentTimeMillis(),
    val data: Map<String, String> = emptyMap()
)

@Serializable
enum class StepType {
    BACKUP_CURRENT_STATE,
    STOP_APP,
    CLEAR_DATA,
    RESTORE_APK,
    RESTORE_DATA,
    RESTORE_OBB,
    RESTORE_EXTERNAL,
    RESTORE_SELINUX,
    RESTORE_PERMISSIONS,
    START_APP
}

@Serializable
data class TransactionMetadata(
    val transactionId: String,
    val snapshotId: String,
    val startTime: Long,
    val steps: List<RestoreStep>,
    val status: TransactionStatus
)

@Serializable
enum class TransactionStatus {
    IN_PROGRESS,
    COMMITTED,
    ROLLED_BACK,
    FAILED
}

class RestoreTransaction(
    val transactionId: String,
    val snapshotId: BackupId,
    private val journalDir: File,
    existingMetadata: TransactionMetadata? = null
) {
    private val steps = mutableListOf<RestoreStep>()
    private val safetyBackups = mutableMapOf<AppId, File>()

    var metadata: TransactionMetadata = existingMetadata ?: TransactionMetadata(
        transactionId = transactionId,
        snapshotId = snapshotId.value,
        startTime = System.currentTimeMillis(),
        steps = emptyList(),
        status = TransactionStatus.IN_PROGRESS
    )
        private set

    suspend fun log(step: RestoreStep) {
        steps.add(step)
        metadata = metadata.copy(steps = steps.toList())
        saveToJournal()
    }

    suspend fun createSafetyBackup(appId: AppId, backupDir: File) {
        safetyBackups[appId] = backupDir
        log(RestoreStep(
            appId = appId.value,
            stepType = StepType.BACKUP_CURRENT_STATE,
            data = mapOf("backupPath" to backupDir.absolutePath)
        ))
    }

    suspend fun commit(appId: AppId) {
        // Mark app as successfully restored
        log(RestoreStep(
            appId = appId.value,
            stepType = StepType.START_APP,
            data = mapOf("status" to "committed")
        ))

        // Remove safety backup for this app
        safetyBackups[appId]?.deleteRecursively()
        safetyBackups.remove(appId)
    }

    suspend fun rollbackApp(appId: AppId, executor: com.obsidianbackup.engine.ShellExecutor): Boolean {
        val safetyBackup = safetyBackups[appId] ?: return false

        try {
            // Stop app
            executor.execute("am force-stop ${appId.value}")

            // Restore from safety backup
            val dataPath = "/data/data/${appId.value}"
            executor.execute("rm -rf $dataPath")
            executor.execute("cp -a ${safetyBackup.absolutePath} $dataPath")

            // Restore SELinux context
            executor.execute("restorecon -R $dataPath")

            return true
        } catch (e: Exception) {
            return false
        }
    }

    suspend fun rollbackAll(executor: com.obsidianbackup.engine.ShellExecutor): List<AppId> {
        val rolledBack = mutableListOf<AppId>()

        safetyBackups.keys.reversed().forEach { appId ->
            if (rollbackApp(appId, executor)) {
                rolledBack.add(appId)
            }
        }

        metadata = metadata.copy(status = TransactionStatus.ROLLED_BACK)
        saveToJournal()

        return rolledBack
    }

    private fun saveToJournal() {
        val file = File(journalDir, "$transactionId.journal")
        file.writeText(Json.encodeToString(metadata))
    }
}
