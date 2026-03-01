package com.obsidianbackup.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obsidianbackup.data.repository.CatalogRepository
import com.obsidianbackup.model.BackupId
import com.obsidianbackup.model.SnapshotId
import com.obsidianbackup.ui.components.SnapshotDiff
import com.obsidianbackup.ui.components.FileMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class DiffViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    fun getDiff(oldSnapshot: BackupId, newSnapshot: BackupId): Flow<SnapshotDiff> = flow {
        try {
            // Convert BackupId to SnapshotId for catalog repository
            val oldSnapshotId = SnapshotId(oldSnapshot.value)
            val newSnapshotId = SnapshotId(newSnapshot.value)
            
            // Fetch both snapshots
            val oldMetadata = catalogRepository.getSnapshot(oldSnapshotId)
            val newMetadata = catalogRepository.getSnapshot(newSnapshotId)
            
            if (oldMetadata == null || newMetadata == null) {
                emit(SnapshotDiff(
                    added = emptyList(),
                    modified = emptyList(),
                    deleted = emptyList(),
                    sizeChange = 0L
                ))
                return@flow
            }
            
            // Parse file lists from checksums
            val oldFiles = oldMetadata.checksums.keys.toSet()
            val newFiles = newMetadata.checksums.keys.toSet()
            
            // Calculate diff
            val addedFiles = (newFiles - oldFiles).map { path ->
                FileMetadata(
                    path = path,
                    size = 0L, // Size not stored in checksums
                    checksum = newMetadata.checksums[path] ?: ""
                )
            }
            
            val deletedFiles = (oldFiles - newFiles).map { path ->
                FileMetadata(
                    path = path,
                    size = 0L,
                    checksum = oldMetadata.checksums[path] ?: ""
                )
            }
            
            val modifiedFiles = (oldFiles intersect newFiles).mapNotNull { path ->
                val oldChecksum = oldMetadata.checksums[path]
                val newChecksum = newMetadata.checksums[path]
                if (oldChecksum != newChecksum) {
                    FileMetadata(
                        path = path,
                        size = 0L,
                        checksum = newChecksum ?: ""
                    )
                } else {
                    null
                }
            }
            
            val sizeChange = newMetadata.totalSize - oldMetadata.totalSize
            
            val diff = SnapshotDiff(
                added = addedFiles,
                modified = modifiedFiles,
                deleted = deletedFiles,
                sizeChange = sizeChange
            )
            
            emit(diff)
        } catch (e: Exception) {
            // Return empty diff on error
            emit(SnapshotDiff(
                added = emptyList(),
                modified = emptyList(),
                deleted = emptyList(),
                sizeChange = 0L
            ))
        }
    }.flowOn(Dispatchers.IO)
}
