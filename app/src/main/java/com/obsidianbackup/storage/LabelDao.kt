package com.obsidianbackup.storage

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for app labels, label assignments, and blacklist operations.
 */
@Dao
interface LabelDao {
    // --- Labels ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabel(label: AppLabelEntity)

    @Update
    suspend fun updateLabel(label: AppLabelEntity)

    @Delete
    suspend fun deleteLabel(label: AppLabelEntity)

    @Query("SELECT * FROM app_labels ORDER BY name ASC")
    fun getAllLabels(): Flow<List<AppLabelEntity>>

    @Query("SELECT * FROM app_labels WHERE id = :labelId")
    suspend fun getLabelById(labelId: String): AppLabelEntity?

    // --- Label Assignments ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun assignLabel(assignment: AppLabelAssignment)

    @Delete
    suspend fun removeAssignment(assignment: AppLabelAssignment)

    @Query("DELETE FROM app_label_assignments WHERE app_id = :appId")
    suspend fun removeAllLabelsFromApp(appId: String)

    @Query("DELETE FROM app_label_assignments WHERE label_id = :labelId")
    suspend fun removeAllAppsFromLabel(labelId: String)

    @Query("""
        SELECT al.* FROM app_labels al
        INNER JOIN app_label_assignments ala ON al.id = ala.label_id
        WHERE ala.app_id = :appId
    """)
    fun getLabelsForApp(appId: String): Flow<List<AppLabelEntity>>

    @Query("SELECT app_id FROM app_label_assignments WHERE label_id = :labelId")
    fun getAppIdsForLabel(labelId: String): Flow<List<String>>

    @Query("SELECT app_id FROM app_label_assignments WHERE label_id = :labelId")
    suspend fun getAppIdsForLabelSync(labelId: String): List<String>

    // --- Blacklist ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlacklist(entry: BlacklistEntity)

    @Delete
    suspend fun deleteBlacklist(entry: BlacklistEntity)

    @Query("DELETE FROM backup_blacklist WHERE app_id = :appId")
    suspend fun removeFromBlacklist(appId: String)

    @Query("SELECT * FROM backup_blacklist ORDER BY app_id ASC")
    fun getAllBlacklisted(): Flow<List<BlacklistEntity>>

    @Query("SELECT * FROM backup_blacklist WHERE app_id = :appId")
    suspend fun getBlacklistEntry(appId: String): BlacklistEntity?

    @Query("SELECT app_id FROM backup_blacklist WHERE mode = 'HIDE'")
    suspend fun getHiddenAppIds(): List<String>

    @Query("SELECT app_id FROM backup_blacklist WHERE mode = 'APK_ONLY'")
    suspend fun getApkOnlyAppIds(): List<String>

    // --- Backup Notes ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: BackupNoteEntity)

    @Update
    suspend fun updateNote(note: BackupNoteEntity)

    @Delete
    suspend fun deleteNote(note: BackupNoteEntity)

    @Query("SELECT * FROM backup_notes WHERE snapshot_id = :snapshotId")
    suspend fun getNotesForSnapshot(snapshotId: String): List<BackupNoteEntity>

    @Query("SELECT * FROM backup_notes ORDER BY created_at DESC")
    fun getAllNotes(): Flow<List<BackupNoteEntity>>

    // --- Protected Backups ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun protectBackup(entry: ProtectedBackupEntity)

    @Query("DELETE FROM protected_backups WHERE snapshot_id = :snapshotId")
    suspend fun unprotectBackup(snapshotId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM protected_backups WHERE snapshot_id = :snapshotId)")
    suspend fun isProtected(snapshotId: String): Boolean

    @Query("SELECT snapshot_id FROM protected_backups")
    suspend fun getAllProtectedSnapshotIds(): List<String>
}
