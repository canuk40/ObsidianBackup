// storage/SettingsEntity.kt
package com.obsidianbackup.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val key: String,
    val value: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
