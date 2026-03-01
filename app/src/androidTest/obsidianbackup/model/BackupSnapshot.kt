// model/BackupSnapshot.kt
package com.titanbackup.model

data class BackupSnapshot(
    val id: BackupId,
    val timestamp: Long,
    val description: String?,
    val apps: List<AppInfo>,
    val totalSize: Long,
    val compressionRatio: Float,
    val encrypted: Boolean,
    val verified: Boolean,
    val permissionMode: String,
    val deviceInfo: DeviceInfo
)

data class DeviceInfo(
    val model: String,
    val manufacturer: String,
    val androidVersion: Int,
    val buildFingerprint: String
)