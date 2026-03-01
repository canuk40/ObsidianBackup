// data/repository/CatalogRepository.kt
package com.obsidianbackup.data.repository

import com.obsidianbackup.domain.repository.ICatalogRepository
import com.obsidianbackup.model.AppId
import com.obsidianbackup.model.BackupId
import com.obsidianbackup.model.SnapshotId
import com.obsidianbackup.model.BackupSnapshot
import com.obsidianbackup.storage.BackupCatalog
import com.obsidianbackup.storage.BackupMetadata
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogRepository @Inject constructor(
    private val backupCatalog: BackupCatalog
) : ICatalogRepository {
    override suspend fun saveSnapshot(metadata: BackupMetadata) {
        backupCatalog.saveSnapshot(metadata)
    }

    override suspend fun getSnapshot(id: SnapshotId): BackupMetadata? {
        return backupCatalog.getSnapshot(BackupId(id.value))
    }

    override fun getAllSnapshots(): Flow<List<BackupSnapshot>> {
        return backupCatalog.getAllSnapshots()
    }

    override suspend fun deleteSnapshot(id: SnapshotId) {
        backupCatalog.deleteSnapshot(BackupId(id.value))
    }

    override suspend fun markVerified(id: SnapshotId, verified: Boolean) {
        backupCatalog.markVerified(BackupId(id.value), verified)
    }

    override suspend fun getTotalBackupSize(): Long {
        return backupCatalog.getTotalBackupSize()
    }

    override suspend fun getSnapshotCount(): Int {
        return backupCatalog.getSnapshotCount()
    }

    override suspend fun importSnapshot(metadataFile: File): BackupMetadata? {
        return backupCatalog.importSnapshot(metadataFile)
    }
    
    override suspend fun getBackupMetadata(id: BackupId): com.obsidianbackup.model.BackupMetadata? {
        return backupCatalog.getBackupMetadata(id)
    }
    
    override suspend fun deleteBackup(id: BackupId) {
        backupCatalog.deleteBackup(id)
    }
    
    override suspend fun getLastFullBackupForApp(appId: AppId): BackupId? {
        return backupCatalog.getLastFullBackupForApp(appId)
    }
}
