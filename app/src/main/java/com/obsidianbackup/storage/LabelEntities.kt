package com.obsidianbackup.storage

import androidx.room.*

/**
 * User-created label for organizing apps into groups.
 * Inspired by Swift Backup's label system — enables selective batch operations.
 */
@Entity(
    tableName = "app_labels",
    indices = [
        Index(value = ["name"], name = "idx_label_name", unique = true)
    ]
)
data class AppLabelEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "color") val color: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

/**
 * Join table linking apps to labels (many-to-many).
 */
@Entity(
    tableName = "app_label_assignments",
    primaryKeys = ["app_id", "label_id"],
    indices = [
        Index(value = ["label_id"], name = "idx_assignment_label"),
        Index(value = ["app_id"], name = "idx_assignment_app")
    ]
)
data class AppLabelAssignment(
    @ColumnInfo(name = "app_id") val appId: String,
    @ColumnInfo(name = "label_id") val labelId: String
)

/**
 * Blacklist entry for excluding apps from batch operations.
 * Two modes: HIDE (exclude entirely) or APK_ONLY (skip data, backup APK only).
 * Inspired by Swift Backup's blacklist system.
 */
@Entity(
    tableName = "backup_blacklist",
    indices = [
        Index(value = ["app_id"], name = "idx_blacklist_app", unique = true)
    ]
)
data class BlacklistEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "app_id") val appId: String,
    @ColumnInfo(name = "mode") val mode: String, // "HIDE" or "APK_ONLY"
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

/**
 * Note attached to an individual backup snapshot.
 * Users can annotate backups with context (e.g., "Before ROM flash", "Working config").
 */
@Entity(
    tableName = "backup_notes",
    indices = [
        Index(value = ["snapshot_id"], name = "idx_note_snapshot")
    ]
)
data class BackupNoteEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "snapshot_id") val snapshotId: String,
    @ColumnInfo(name = "note_text") val noteText: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Protection flag on snapshots — prevents auto-deletion by history trimmer.
 * Stored as a separate entity rather than a column so protection state
 * can be toggled independently without touching the snapshot record.
 */
@Entity(
    tableName = "protected_backups",
    indices = [
        Index(value = ["snapshot_id"], name = "idx_protected_snapshot", unique = true)
    ]
)
data class ProtectedBackupEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "snapshot_id") val snapshotId: String,
    @ColumnInfo(name = "protected_at") val protectedAt: Long = System.currentTimeMillis()
)
