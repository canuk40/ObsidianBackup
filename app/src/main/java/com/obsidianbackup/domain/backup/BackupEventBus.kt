// domain/backup/BackupEventBus.kt
package com.obsidianbackup.domain.backup

import com.obsidianbackup.ml.analytics.BackupEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Event bus for backup-related events
 */
class BackupEventBus {
    private val _events = MutableSharedFlow<BackupEvent>()
    val events: SharedFlow<BackupEvent> = _events.asSharedFlow()
    
    suspend fun emit(event: BackupEvent) {
        _events.emit(event)
    }
}
