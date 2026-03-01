package com.obsidianbackup.wear.data

import kotlinx.serialization.Serializable

/**
 * Backup status data model for watch display
 */
@Serializable
data class BackupStatus(
    val isRunning: Boolean = false,
    val lastBackupTime: Long = 0L,
    val lastBackupSuccess: Boolean = false,
    val nextScheduledBackup: Long = 0L,
    val totalBackups: Int = 0,
    val backupSizeMB: Float = 0f
)

/**
 * Backup progress data model
 */
@Serializable
data class BackupProgress(
    val currentFile: String = "",
    val filesProcessed: Int = 0,
    val totalFiles: Int = 0,
    val bytesProcessed: Long = 0L,
    val totalBytes: Long = 0L,
    val percentage: Int = 0,
    val status: String = ""
)

/**
 * Settings synced from phone
 */
@Serializable
data class WearSettings(
    val autoBackupEnabled: Boolean = false,
    val cloudSyncEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val hapticFeedback: Boolean = true
)
