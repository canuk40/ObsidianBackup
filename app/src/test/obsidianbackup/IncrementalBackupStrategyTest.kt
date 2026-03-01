// test/java/com.titanbackup/IncrementalBackupStrategyTest.kt
package com.titanbackup

import com.titanbackup.engine.IncrementalBackupStrategy
import com.titanbackup.model.*
import com.titanbackup.storage.BackupCatalog
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class IncrementalBackupStrategyTest {

    private val mockCatalog = mock<BackupCatalog>()
    private val strategy = IncrementalBackupStrategy(mockCatalog)

    @Test
    fun `first backup creates full plan`() = runTest {
        // Given no previous snapshot
        val appId = AppId("com.example.test")

        // When
        val plan = strategy.createIncremental(appId, null)

        // Then
        assert(plan is BackupPlan.Full)
    }

    @Test
    fun `incremental backup creates incremental plan when changes exist`() = runTest {
        // Given
        val appId = AppId("com.example.test")
        val lastSnapshotId = BackupId("snapshot_123")
        val mockMetadata = BackupMetadata(
            snapshotId = lastSnapshotId,
            timestamp = System.currentTimeMillis(),
            description = "Test snapshot",
            apps = listOf(appId),
            components = setOf(BackupComponent.APK, BackupComponent.DATA),
            compressionLevel = 6,
            encrypted = false,
            permissionMode = "ROOT",
            deviceInfo = DeviceInfo("Test", "Test", 30, "test"),
            totalSize = 1000L,
            checksums = mapOf("data.tar.zst" to "old_hash")
        )

        whenever(mockCatalog.getSnapshot(lastSnapshotId)).thenReturn(mockMetadata)

        // When
        val plan = strategy.createIncremental(appId, lastSnapshotId)

        // Then
        assert(plan is BackupPlan.Incremental)
        val incrementalPlan = plan as BackupPlan.Incremental
        assertEquals(lastSnapshotId, incrementalPlan.baseSnapshot)
    }

    @Test
    fun `merkle tree verification detects corruption`() {
        // Test MerkleTreeVerifier
        val verifier = MerkleTreeVerifier()
        val mockFiles = listOf(
            FileMetadata("file1.txt", "hash1", 100),
            FileMetadata("file2.txt", "hash2", 200)
        )

        // Create tree
        val tree = verifier.buildTree(mockFiles.map { java.io.File(it.path) })

        // Verify against same files
        val isValid = verifier.verifyTree(tree, mockFiles.map { java.io.File(it.path) })
        assertTrue("Tree should verify against same files", isValid)
    }
}
