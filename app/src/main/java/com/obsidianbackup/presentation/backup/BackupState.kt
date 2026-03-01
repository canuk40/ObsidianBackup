// presentation/backup/BackupState.kt
package com.obsidianbackup.presentation.backup

import com.obsidianbackup.model.AppId
import com.obsidianbackup.model.AppInfo

data class BackupState(
    val apps: List<AppInfo> = emptyList(),
    val selectedApps: Set<AppId> = emptySet(),
    val backupProgress: BackupProgress = BackupProgress.Idle,
    val error: String? = null
)

sealed class BackupProgress {
    object Idle : BackupProgress()
    object InProgress : BackupProgress()
    data class Completed(val result: com.obsidianbackup.model.BackupResult) : BackupProgress()
    data class Failed(val error: String) : BackupProgress()
}
