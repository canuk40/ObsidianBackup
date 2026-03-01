// domain/backup/BackupEngineFactory.kt
package com.obsidianbackup.domain.backup

import com.obsidianbackup.engine.BackupEngine

class BackupEngineFactory(
    private val engine: BackupEngine
) {
    suspend fun createForCurrentMode(): BackupEngine = engine
}
