// storage/AppBackupDao.kt
package com.obsidianbackup.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.ColumnInfo
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AppBackupDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppBackup(appBackup: AppBackupEntity)

    // Optimized: Transaction for batch operations
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppBackups(appBackups: List<AppBackupEntity>)

    // Optimized: Uses index on snapshot_id
    @Query("SELECT * FROM app_backups WHERE snapshot_id = :snapshotId")
    suspend fun getAppBackupsForSnapshot(snapshotId: String): List<AppBackupEntity>
    
    // Optimized: Paged query for large result sets
    @Query("SELECT * FROM app_backups WHERE snapshot_id = :snapshotId LIMIT :limit OFFSET :offset")
    suspend fun getAppBackupsForSnapshotPaged(snapshotId: String, limit: Int, offset: Int): List<AppBackupEntity>

    // Optimized: Uses index on app_id and backup_timestamp
    @Query("SELECT * FROM app_backups WHERE app_id = :appId ORDER BY backup_timestamp DESC LIMIT :limit")
    suspend fun getAppBackupHistory(appId: String, limit: Int = 10): List<AppBackupEntity>
    
    // Optimized: Get only most recent backup for an app
    @Query("SELECT * FROM app_backups WHERE app_id = :appId ORDER BY backup_timestamp DESC LIMIT 1")
    suspend fun getLatestAppBackup(appId: String): AppBackupEntity?

    // Optimized: Aggregation uses index
    @Query("SELECT SUM(apk_size + data_size + obb_size + external_size) FROM app_backups WHERE snapshot_id = :snapshotId")
    suspend fun getTotalSnapshotSize(snapshotId: String): Long?

    @Transaction
    @Query("DELETE FROM app_backups WHERE snapshot_id = :snapshotId")
    suspend fun deleteAppBackupsForSnapshot(snapshotId: String)
    
    // Optimized: Batch delete
    @Transaction
    @Query("DELETE FROM app_backups WHERE snapshot_id IN (:snapshotIds)")
    suspend fun deleteAppBackupsForSnapshots(snapshotIds: List<String>)

    @Query("SELECT COUNT(*) FROM app_backups WHERE snapshot_id = :snapshotId")
    suspend fun getAppCountForSnapshot(snapshotId: String): Int
    
    // Optimized: Get summary without large JSON fields
    @Query("SELECT id, snapshot_id, app_id, apk_size, data_size, backup_timestamp FROM app_backups WHERE snapshot_id = :snapshotId")
    suspend fun getAppBackupSummaries(snapshotId: String): List<AppBackupSummary>

    // Get all snapshot IDs that contain a given app
    @Query("SELECT DISTINCT snapshot_id FROM app_backups WHERE app_id = :appId ORDER BY backup_timestamp DESC")
    suspend fun getSnapshotIdsForApp(appId: String): List<String>

    // Get all unique app IDs across all backups
    @Query("SELECT DISTINCT app_id FROM app_backups")
    suspend fun getAllDistinctAppIds(): List<String>
}

// Lightweight data class for list displays
data class AppBackupSummary(
    val id: Long,
    @ColumnInfo(name = "snapshot_id") val snapshotId: String,
    @ColumnInfo(name = "app_id") val appId: String,
    @ColumnInfo(name = "apk_size") val apkSize: Long,
    @ColumnInfo(name = "data_size") val dataSize: Long,
    @ColumnInfo(name = "backup_timestamp") val backupTimestamp: Long
)
