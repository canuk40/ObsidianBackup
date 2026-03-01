// storage/BackupScheduleDao.kt
package com.obsidianbackup.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: BackupScheduleEntity)

    @Update
    suspend fun updateSchedule(schedule: BackupScheduleEntity)

    @Query("SELECT * FROM backup_schedules WHERE id = :id")
    suspend fun getSchedule(id: String): BackupScheduleEntity?

    @Query("SELECT * FROM backup_schedules WHERE enabled = 1 ORDER BY next_run ASC")
    fun getEnabledSchedules(): Flow<List<BackupScheduleEntity>>

    @Query("SELECT * FROM backup_schedules ORDER BY created_at DESC")
    fun getAllSchedules(): Flow<List<BackupScheduleEntity>>

    @Query("DELETE FROM backup_schedules WHERE id = :id")
    suspend fun deleteSchedule(id: String)

    @Query("UPDATE backup_schedules SET enabled = :enabled WHERE id = :id")
    suspend fun setScheduleEnabled(id: String, enabled: Boolean)

    @Query("UPDATE backup_schedules SET last_run = :lastRun, next_run = :nextRun WHERE id = :id")
    suspend fun updateScheduleRunTimes(id: String, lastRun: Long, nextRun: Long)

    @Query("SELECT * FROM backup_schedules WHERE next_run <= :currentTime AND enabled = 1 ORDER BY next_run ASC")
    suspend fun getDueSchedules(currentTime: Long): List<BackupScheduleEntity>
}
