// security/audit/AuditLogDao.kt
package com.obsidianbackup.security.audit

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * HIPAA Audit Log DAO - Append-Only Interface
 * 
 * CRITICAL SECURITY POLICY:
 * - INSERT operations ONLY (no UPDATE, no DELETE)
 * - Immutable audit trail per HIPAA requirements
 * - Queries for compliance reporting only
 * 
 * Retention: 6 years minimum (HIPAA requirement)
 */
@Dao
interface AuditLogDao {
    
    /**
     * Insert new audit log entry
     * 
     * HIPAA Compliance: Append-only operation
     * 
     * @param log Audit log entity to insert
     * @return Row ID of inserted log
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLog(log: AuditLogEntity): Long
    
    /**
     * Insert multiple audit logs (batch operation)
     * 
     * @param logs List of audit logs to insert
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLogs(logs: List<AuditLogEntity>)
    
    /**
     * Get all audit logs (for compliance export)
     * 
     * WARNING: Can be large dataset - use with pagination
     * 
     * @return List of all audit logs
     */
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<AuditLogEntity>
    
    /**
     * Get audit logs for specific user
     * 
     * @param userId User identifier
     * @return List of logs for user
     */
    @Query("SELECT * FROM audit_logs WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getLogsForUser(userId: String): List<AuditLogEntity>
    
    /**
     * Get audit logs for specific action
     * 
     * @param action Action type (EXPORT, IMPORT, etc.)
     * @return List of logs for action
     */
    @Query("SELECT * FROM audit_logs WHERE action = :action ORDER BY timestamp DESC")
    suspend fun getLogsForAction(action: String): List<AuditLogEntity>
    
    /**
     * Get audit logs in date range
     * 
     * @param startTime Start timestamp (milliseconds)
     * @param endTime End timestamp (milliseconds)
     * @return List of logs in range
     */
    @Query("SELECT * FROM audit_logs WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getLogsInRange(startTime: Long, endTime: Long): List<AuditLogEntity>
    
    /**
     * Get failed audit logs (for security monitoring)
     * 
     * @return List of failed operations
     */
    @Query("SELECT * FROM audit_logs WHERE outcome = 'FAILURE' ORDER BY timestamp DESC")
    suspend fun getFailedLogs(): List<AuditLogEntity>
    
    /**
     * Get audit logs by data type
     * 
     * @param dataType Type of data (e.g., "StepsRecord")
     * @return List of logs for data type
     */
    @Query("SELECT * FROM audit_logs WHERE dataType = :dataType ORDER BY timestamp DESC")
    suspend fun getLogsByDataType(dataType: String): List<AuditLogEntity>
    
    /**
     * Get recent audit logs (last N entries)
     * 
     * @param limit Number of recent logs to retrieve
     * @return List of recent logs
     */
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 100): List<AuditLogEntity>
    
    /**
     * Get audit logs as Flow for real-time monitoring
     * 
     * @return Flow of all audit logs
     */
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun observeLogs(): Flow<List<AuditLogEntity>>
    
    /**
     * Get count of audit logs
     * 
     * @return Total number of audit log entries
     */
    @Query("SELECT COUNT(*) FROM audit_logs")
    suspend fun getLogCount(): Int
    
    /**
     * Get count of failed operations
     * 
     * @return Number of failed audit entries
     */
    @Query("SELECT COUNT(*) FROM audit_logs WHERE outcome = 'FAILURE'")
    suspend fun getFailedLogCount(): Int
    
    /**
     * Get audit statistics by action
     * 
     * @return Map of action to count
     */
    @Query("SELECT action, COUNT(*) as count FROM audit_logs GROUP BY action")
    suspend fun getActionStatistics(): List<ActionStatistic>
    
    /**
     * SECURITY POLICY ENFORCEMENT:
     * 
     * NO UPDATE OPERATIONS ALLOWED
     * NO DELETE OPERATIONS ALLOWED
     * 
     * Audit logs are immutable per HIPAA requirements.
     * Any attempt to modify or delete logs is a compliance violation.
     * 
     * Retention: Logs must be retained for minimum 6 years.
     * After 6 years, logs may be archived to external storage but NOT deleted.
     */
}

/**
 * Data class for action statistics query result
 */
data class ActionStatistic(
    val action: String,
    val count: Int
)
