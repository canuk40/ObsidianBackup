// engine/restore/RestoreJournal.kt
package com.obsidianbackup.engine.restore

import com.obsidianbackup.model.BackupId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class RestoreJournal(private val journalDir: File) {
    private val json = Json { prettyPrint = true }
    private val mutex = Mutex()

    init {
        journalDir.mkdirs()
    }

    suspend fun beginTransaction(snapshotId: BackupId): RestoreTransaction {
        return mutex.withLock {
            val transactionId = UUID.randomUUID().toString()
            val transaction = RestoreTransaction(
                transactionId = transactionId,
                snapshotId = snapshotId,
                journalDir = journalDir
            )

            saveMetadata(transaction.metadata)
            transaction
        }
    }

    suspend fun saveMetadata(metadata: TransactionMetadata) {
        mutex.withLock {
            val file = File(journalDir, "${metadata.transactionId}.journal")
            file.writeText(json.encodeToString(metadata))
        }
    }

    suspend fun loadTransaction(transactionId: String): TransactionMetadata? {
        return mutex.withLock {
            val file = File(journalDir, "$transactionId.journal")
            if (!file.exists()) return@withLock null

            try {
                json.decodeFromString<TransactionMetadata>(file.readText())
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun rollback(transaction: RestoreTransaction) {
        mutex.withLock {
            val metadata = transaction.metadata.copy(status = TransactionStatus.ROLLED_BACK)
            saveMetadata(metadata)
        }
    }

    suspend fun finalizeTransaction(transaction: RestoreTransaction) {
        mutex.withLock {
            val metadata = transaction.metadata.copy(status = TransactionStatus.COMMITTED)
            saveMetadata(metadata)

            // Archive journal file
            val file = File(journalDir, "${transaction.transactionId}.journal")
            val archiveFile = File(journalDir, "archive/${transaction.transactionId}.journal")
            archiveFile.parentFile?.mkdirs()
            file.renameTo(archiveFile)
        }
    }

    suspend fun recoverIncomplete(): List<RestoreTransaction> {
        return mutex.withLock {
            journalDir.listFiles()
                ?.filter { it.extension == "journal" }
                ?.mapNotNull { file ->
                    try {
                        val metadata = json.decodeFromString<TransactionMetadata>(file.readText())
                        if (metadata.status == TransactionStatus.IN_PROGRESS) {
                            RestoreTransaction(
                                transactionId = metadata.transactionId,
                                snapshotId = BackupId(metadata.snapshotId),
                                journalDir = journalDir,
                                existingMetadata = metadata
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
        }
    }
}
