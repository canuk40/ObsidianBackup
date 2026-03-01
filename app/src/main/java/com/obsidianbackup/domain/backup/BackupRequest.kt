// domain/backup/BackupRequest.kt
package com.obsidianbackup.domain.backup

import com.obsidianbackup.model.AppId
import com.obsidianbackup.model.BackupComponent
import kotlinx.serialization.Serializable

@Serializable
data class BackupRequest(
    val appIds: List<AppId>,
    val components: Set<BackupComponent> = emptySet(),
    val scheduledBackup: Boolean = false,
    val priority: Int = 0,
    val incremental: Boolean = false,
    val compressionLevel: Int = 6,
    val encryptionEnabled: Boolean = true,
    val description: String? = null
)
