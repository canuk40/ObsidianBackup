// storage/BackupScheduleEntity.kt
package com.obsidianbackup.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backup_schedules")
data class BackupScheduleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val frequency: String,
    val enabled: Boolean = true,
    @ColumnInfo(name = "app_ids_json") val appIdsJson: String,
    @ColumnInfo(name = "components_json") val componentsJson: String,
    @ColumnInfo(name = "last_run") val lastRun: Long? = null,
    @ColumnInfo(name = "next_run") val nextRun: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
