// storage/BackupProfileDao.kt
package com.obsidianbackup.storage

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupProfileDao {
    
    @Query("SELECT * FROM backup_profiles WHERE id = :profileId")
    suspend fun getProfile(profileId: String): BackupProfileEntity?
    
    @Query("SELECT * FROM backup_profiles WHERE id = :profileId")
    fun observeProfile(profileId: String): Flow<BackupProfileEntity?>
    
    @Query("SELECT * FROM backup_profiles ORDER BY createdAt DESC")
    suspend fun getAllProfiles(): List<BackupProfileEntity>
    
    @Query("SELECT * FROM backup_profiles ORDER BY createdAt DESC")
    fun observeAllProfiles(): Flow<List<BackupProfileEntity>>
    
    @Query("SELECT * FROM backup_profiles WHERE isEnabled = 1 ORDER BY createdAt DESC")
    suspend fun getEnabledProfiles(): List<BackupProfileEntity>
    
    @Query("SELECT * FROM backup_profiles WHERE scheduleEnabled = 1 ORDER BY createdAt DESC")
    suspend fun getScheduledProfiles(): List<BackupProfileEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: BackupProfileEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<BackupProfileEntity>)
    
    @Update
    suspend fun update(profile: BackupProfileEntity)
    
    @Query("DELETE FROM backup_profiles WHERE id = :profileId")
    suspend fun delete(profileId: String)
    
    @Query("DELETE FROM backup_profiles")
    suspend fun deleteAll()
    
    @Query("UPDATE backup_profiles SET isEnabled = :enabled WHERE id = :profileId")
    suspend fun setEnabled(profileId: String, enabled: Boolean)
    
    @Query("UPDATE backup_profiles SET scheduleEnabled = :enabled WHERE id = :profileId")
    suspend fun setScheduleEnabled(profileId: String, enabled: Boolean)
    
    @Query("UPDATE backup_profiles SET lastBackupTimestamp = :timestamp WHERE id = :profileId")
    suspend fun updateLastBackupTimestamp(profileId: String, timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM backup_profiles")
    suspend fun getProfileCount(): Int
    
    @Query("SELECT COUNT(*) FROM backup_profiles WHERE isEnabled = 1")
    suspend fun getEnabledProfileCount(): Int
}
