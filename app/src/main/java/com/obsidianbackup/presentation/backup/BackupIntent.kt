// presentation/backup/BackupIntent.kt
package com.obsidianbackup.presentation.backup

import com.obsidianbackup.model.AppId

sealed class BackupIntent {
    data class SelectApp(val appId: AppId) : BackupIntent()
    data class UnselectApp(val appId: AppId) : BackupIntent()
    object StartBackup : BackupIntent()
    object CancelBackup : BackupIntent()
}
