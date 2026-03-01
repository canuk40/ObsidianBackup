package com.obsidianbackup.data.local.catalog

import com.obsidianbackup.storage.BackupCatalog as OldBackupCatalog
import com.obsidianbackup.core.models.BackupId
import com.obsidianbackup.core.models.BackupSnapshot
import kotlinx.coroutines.flow.Flow
import java.io.File

class BackupCatalogFacade(private val delegate: OldBackupCatalog) {

    suspend fun saveSnapshot(metadata: com.obsidianbackup.storage.BackupMetadata) {
        delegate.saveSnapshot(metadata)
    }

    suspend fun getSnapshot(id: com.obsidianbackup.core.models.BackupId): com.obsidianbackup.storage.BackupMetadata? {
        return delegate.getSnapshot(com.obsidianbackup.model.BackupId(id.value))
    }

    fun getAllSnapshots(): Flow<List<BackupSnapshot>> {
        return delegate.getAllSnapshots()
    }

    fun getSnapshotDirectory(id: com.obsidianbackup.core.models.BackupId): File {
        return delegate.getSnapshotDirectory(com.obsidianbackup.model.BackupId(id.value))
    }

    suspend fun markSyncedToCloud(id: com.obsidianbackup.core.models.BackupId, providerId: String, ts: Long) {
        delegate.markSyncedToCloud(com.obsidianbackup.model.BackupId(id.value), providerId, ts)
    }

    suspend fun importSnapshot(metadataFile: File) = delegate.importSnapshot(metadataFile)
}
