package com.obsidianbackup.data.repository

import com.obsidianbackup.model.LogEntry
import com.obsidianbackup.model.LogLevel
import com.obsidianbackup.model.OperationType
import com.obsidianbackup.model.SnapshotId
import com.obsidianbackup.storage.LogDao
import com.obsidianbackup.storage.LogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRepository @Inject constructor(
    private val logDao: LogDao
) {
    
    /**
     * Insert a log entry
     */
    suspend fun insertLog(
        operationType: OperationType,
        level: LogLevel,
        message: String,
        details: String? = null,
        snapshotId: SnapshotId? = null
    ) {
        val logEntity = LogEntity(
            timestamp = System.currentTimeMillis(),
            operationType = operationType.name,
            level = level.name,
            message = message,
            details = details,
            snapshotId = snapshotId?.value
        )
        logDao.insertLog(logEntity)
    }
    
    /**
     * Get recent logs
     */
    fun getRecentLogs(limit: Int = 100): Flow<List<LogEntry>> {
        return logDao.getRecentLogs(limit).map { entities ->
            entities.map { it.toLogEntry() }
        }
    }
    
    /**
     * Get logs by level
     */
    fun getLogsByLevel(level: LogLevel): Flow<List<LogEntry>> {
        return logDao.getLogsByLevel(level.name).map { entities ->
            entities.map { it.toLogEntry() }
        }
    }
    
    /**
     * Get logs by operation type
     */
    fun getLogsByOperation(operationType: OperationType): Flow<List<LogEntry>> {
        return logDao.getLogsByOperation(operationType.name).map { entities ->
            entities.map { it.toLogEntry() }
        }
    }
    
    /**
     * Get logs by date range
     */
    fun getLogsByDateRange(startTime: Long, endTime: Long): Flow<List<LogEntry>> {
        return logDao.getLogsByDateRange(startTime, endTime).map { entities ->
            entities.map { it.toLogEntry() }
        }
    }
    
    /**
     * Delete logs older than a certain time
     */
    suspend fun deleteLogsBefore(before: Long) {
        logDao.deleteLogsBefore(before)
    }
    
    /**
     * Clear all logs
     */
    suspend fun clearAllLogs() {
        logDao.clearAllLogs()
    }
    
    /**
     * Get log count
     */
    suspend fun getLogCount(): Int {
        return logDao.getLogCount()
    }
    
    private fun LogEntity.toLogEntry(): LogEntry {
        return LogEntry(
            timestamp = timestamp,
            operationType = OperationType.valueOf(operationType),
            level = LogLevel.valueOf(level),
            message = message,
            details = details,
            snapshotId = snapshotId?.let { SnapshotId(it) }
        )
    }
}
