// storage/AppBackupEntity.kt
package com.obsidianbackup.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Optimized with indexes for common queries
@Entity(
    tableName = "app_backups",
    indices = [
        Index(value = ["snapshot_id"], name = "idx_app_backup_snapshot"),
        Index(value = ["app_id"], name = "idx_app_backup_app_id"),
        Index(value = ["backup_timestamp"], name = "idx_app_backup_timestamp")
    ]
)
data class AppBackupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "snapshot_id") val snapshotId: String,
    @ColumnInfo(name = "app_id") val appId: String,
    @ColumnInfo(name = "apk_size") val apkSize: Long,
    @ColumnInfo(name = "data_size") val dataSize: Long,
    @ColumnInfo(name = "obb_size") val obbSize: Long = 0,
    @ColumnInfo(name = "external_size") val externalSize: Long = 0,
    @ColumnInfo(name = "backup_timestamp") val backupTimestamp: Long,
    @ColumnInfo(name = "components_json") val componentsJson: String
)
