package com.obsidianbackup.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "logs",
    indices = [
        Index(value = ["timestamp"], name = "idx_log_timestamp"),
        Index(value = ["level"], name = "idx_log_level"),
        Index(value = ["operation_type"], name = "idx_log_operation")
    ]
)
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    @ColumnInfo(name = "operation_type") val operationType: String,
    val level: String,
    val message: String,
    val details: String? = null,
    @ColumnInfo(name = "snapshot_id") val snapshotId: String? = null
)
