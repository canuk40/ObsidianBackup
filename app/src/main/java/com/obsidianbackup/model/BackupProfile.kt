// model/BackupProfile.kt
package com.obsidianbackup.model

import kotlinx.serialization.Serializable

/**
 * Represents a backup profile configuration for scheduled/smart backups
 */
@Serializable
data class BackupProfile(
    val id: BackupProfileId,
    val name: String,
    val appIds: List<AppId>,
    val components: Set<BackupComponent> = setOf(BackupComponent.APK, BackupComponent.DATA),
    val incremental: Boolean = false,
    val compressionLevel: Int = 6,
    val encryptionEnabled: Boolean = false,
    val isEnabled: Boolean = true,
    val scheduleEnabled: Boolean = false,
    val scheduleCron: String? = null, // Cron expression for scheduling
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastBackupTimestamp: Long? = null,
    val cloudSyncEnabled: Boolean = false,
    val cloudProviderId: String? = null
)

@JvmInline
@Serializable
value class BackupProfileId(val value: String) {
    companion object {
        fun generate(): BackupProfileId = BackupProfileId(java.util.UUID.randomUUID().toString())
    }
}

/**
 * Statistics for a backup profile
 */
data class BackupProfileStats(
    val profileId: BackupProfileId,
    val totalBackups: Int,
    val successfulBackups: Int,
    val failedBackups: Int,
    val totalSize: Long,
    val lastBackupTimestamp: Long?,
    val nextScheduledBackup: Long?
)
