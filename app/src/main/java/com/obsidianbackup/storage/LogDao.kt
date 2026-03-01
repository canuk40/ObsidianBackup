package com.obsidianbackup.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntity)
    
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<LogEntity>)
    
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<LogEntity>>
    
    @Query("SELECT * FROM logs WHERE level = :level ORDER BY timestamp DESC")
    fun getLogsByLevel(level: String): Flow<List<LogEntity>>
    
    @Query("SELECT * FROM logs WHERE operation_type = :operationType ORDER BY timestamp DESC")
    fun getLogsByOperation(operationType: String): Flow<List<LogEntity>>
    
    @Query("SELECT * FROM logs WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getLogsByDateRange(startTime: Long, endTime: Long): Flow<List<LogEntity>>
    
    @Transaction
    @Query("DELETE FROM logs WHERE timestamp < :before")
    suspend fun deleteLogsBefore(before: Long)
    
    @Transaction
    @Query("DELETE FROM logs")
    suspend fun clearAllLogs()
    
    @Query("SELECT COUNT(*) FROM logs")
    suspend fun getLogCount(): Int
}
