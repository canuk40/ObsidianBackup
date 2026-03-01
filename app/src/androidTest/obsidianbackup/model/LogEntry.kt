// model/LogEntry.kt
package com.titanbackup.model

import java.util.*

data class LogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long,
    val operationType: OperationType,
    val level: LogLevel,
    val message: String,
    val details: String? = null,
    val snapshotId: BackupId? = null
)

enum class LogLevel {
    DEBUG, INFO, WARNING, ERROR
}