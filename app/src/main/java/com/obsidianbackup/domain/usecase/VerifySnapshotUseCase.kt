// domain/usecase/VerifySnapshotUseCase.kt
package com.obsidianbackup.domain.usecase

import com.obsidianbackup.domain.repository.ICatalogRepository
import com.obsidianbackup.model.BackupId
import com.obsidianbackup.model.SnapshotId
import com.obsidianbackup.model.VerificationResult
import com.obsidianbackup.verification.ChecksumVerifier
import javax.inject.Inject

class VerifySnapshotUseCase @Inject constructor(
    private val checksumVerifier: ChecksumVerifier,
    private val catalogRepository: ICatalogRepository,
    private val backupCatalog: com.obsidianbackup.storage.BackupCatalog
) {
    suspend operator fun invoke(snapshotId: BackupId): VerificationResult {
        // Get snapshot metadata from catalog
        val metadata = backupCatalog.getSnapshotMetadata(snapshotId)
            ?: return VerificationResult(
                snapshotId = SnapshotId(snapshotId.value),
                filesChecked = 0,
                allValid = false,
                corruptedFiles = listOf("Snapshot not found in catalog")
            )
        
        // Get the snapshot directory on disk
        val snapshotDir = backupCatalog.getSnapshotDirectory(snapshotId)
        if (!snapshotDir.exists() || !snapshotDir.isDirectory) {
            return VerificationResult(
                snapshotId = SnapshotId(snapshotId.value),
                filesChecked = 0,
                allValid = false,
                corruptedFiles = listOf("Snapshot directory not found: ${snapshotDir.absolutePath}")
            )
        }
        
        // Get stored checksums from metadata
        val expectedChecksums = metadata.checksums
        
        // Verify snapshot using checksumVerifier
        return checksumVerifier.verifySnapshot(snapshotDir, expectedChecksums)
    }
}
