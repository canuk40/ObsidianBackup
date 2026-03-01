// domain/repository/ICatalogRepository.kt
package com.obsidianbackup.domain.repository

import com.obsidianbackup.model.BackupId
import com.obsidianbackup.model.SnapshotId
import com.obsidianbackup.model.BackupSnapshot
import com.obsidianbackup.model.AppId
import com.obsidianbackup.storage.BackupMetadata
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Repository interface for backup catalog operations.
 * Abstracts the storage layer from domain logic.
 */
interface ICatalogRepository {
    suspend fun saveSnapshot(metadata: BackupMetadata)
    
    suspend fun getSnapshot(id: SnapshotId): BackupMetadata?
    
    fun getAllSnapshots(): Flow<List<BackupSnapshot>>
    
    suspend fun deleteSnapshot(id: SnapshotId)
    
    suspend fun markVerified(id: SnapshotId, verified: Boolean)
    
    suspend fun getTotalBackupSize(): Long
    
    suspend fun getSnapshotCount(): Int
    
    suspend fun importSnapshot(metadataFile: File): BackupMetadata?
    
    // Additional methods needed by domain layer
    suspend fun getBackupMetadata(id: BackupId): com.obsidianbackup.model.BackupMetadata?
    
    suspend fun deleteBackup(id: BackupId)
    
    suspend fun getLastFullBackupForApp(appId: AppId): BackupId?
}
