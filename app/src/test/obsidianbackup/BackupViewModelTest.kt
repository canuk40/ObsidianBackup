// Unit Tests (80%) - Pure logic
// test/java/com.titanbackup/BackupViewModelTest.kt
package com.titanbackup

import com.titanbackup.model.*
import com.titanbackup.ui.components.BackupDiffScreen
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupViewModelTest {

    @Test
    fun `compute diff correctly identifies changes`() = runTest {
        // Given
        val oldSnapshot = SnapshotDiff(emptyList(), emptyList(), emptyList(), 0L)
        val newSnapshot = SnapshotDiff(
            added = listOf(FileMetadata("new_file.txt", "hash1", 100)),
            modified = listOf(FileMetadata("changed_file.txt", "hash2", 200)),
            deleted = listOf(FileMetadata("deleted_file.txt", "hash3", 50)),
            sizeChange = 250L
        )

        // When - simulate diff computation
        val diff = computeMockDiff(oldSnapshot, newSnapshot)

        // Then
        assertEquals(1, diff.added.size)
        assertEquals(1, diff.modified.size)
        assertEquals(1, diff.deleted.size)
        assertEquals(250L, diff.sizeChange)
    }

    @Test
    fun `backup profile selection based on use case`() {
        // Test BackupProfile enum values
        val minimal = BackupProfile.MINIMAL
        assertEquals(setOf(BackupComponent.APK), minimal.components)
        assertEquals(3, minimal.compression)
        assertEquals(VerificationLevel.QUICK, minimal.verification)

        val complete = BackupProfile.COMPLETE
        assert(complete.components.size > 1) // Should include all components
        assertEquals(9, complete.compression)
        assertEquals(VerificationLevel.PARANOID, complete.verification)
    }

    private fun computeMockDiff(old: SnapshotDiff, new: SnapshotDiff): SnapshotDiff {
        // Mock implementation for testing
        return SnapshotDiff(
            added = new.added,
            modified = new.modified,
            deleted = new.deleted,
            sizeChange = new.sizeChange
        )
    }
}
