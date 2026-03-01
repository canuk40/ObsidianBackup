// Integration Tests (15%) - Engine + Storage integration
// androidTest/java/com.titanbackup/BackupEngineIntegrationTest.kt
package com.titanbackup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.titanbackup.engine.*
import com.titanbackup.model.*
import com.titanbackup.storage.BackupCatalog
import com.titanbackup.storage.BackupDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BackupEngineIntegrationTest {

    private lateinit var context: Context
    private lateinit var database: BackupDatabase
    private lateinit var catalog: BackupCatalog
    private lateinit var mockShellExecutor: ShellExecutor
    private lateinit var engine: BusyBoxEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, BackupDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        catalog = BackupCatalog(context, context.cacheDir.absolutePath)

        // Mock shell executor for integration tests
        mockShellExecutor = object : ShellExecutor(PermissionMode.ROOT) {
            override suspend fun execute(command: String): ShellResult {
                // Mock successful execution
                return ShellResult.Success("Mock output", 0)
            }
        }

        engine = BusyBoxEngine(
            permissionManager = mock(), // Would need proper mock
            catalog = catalog,
            backupRootPath = context.cacheDir.absolutePath
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `backup metadata is saved to database`() = runTest {
        // Given
        val metadata = BackupMetadata(
            snapshotId = BackupId("test_snapshot"),
            timestamp = System.currentTimeMillis(),
            description = "Integration test backup",
            apps = listOf(AppId("com.example.test")),
            components = setOf(BackupComponent.APK),
            compressionLevel = 6,
            encrypted = false,
            permissionMode = "ROOT",
            deviceInfo = DeviceInfo("Test", "Test", 30, "test"),
            totalSize = 1000L,
            checksums = mapOf("test" to "hash"),
            merkleRootHash = "root_hash",
            merkleTreeJson = "{}"
        )

        // When
        catalog.saveSnapshot(metadata)

        // Then
        val retrieved = catalog.getSnapshot(BackupId("test_snapshot"))
        assertNotNull("Metadata should be retrievable", retrieved)
        assertEquals(metadata.snapshotId, retrieved?.snapshotId)
        assertEquals(metadata.description, retrieved?.description)
    }

    @Test
    fun `catalog provides snapshot count and size statistics`() = runTest {
        // Given multiple snapshots
        val snapshots = listOf(
            createTestMetadata("snap1", 1000L),
            createTestMetadata("snap2", 2000L),
            createTestMetadata("snap3", 1500L)
        )

        snapshots.forEach { catalog.saveSnapshot(it) }

        // When
        val count = catalog.getSnapshotCount()
        val totalSize = catalog.getTotalBackupSize()

        // Then
        assertEquals(3, count)
        assertEquals(4500L, totalSize)
    }

    private fun createTestMetadata(id: String, size: Long): BackupMetadata {
        return BackupMetadata(
            snapshotId = BackupId(id),
            timestamp = System.currentTimeMillis(),
            description = "Test $id",
            apps = listOf(AppId("com.example.test")),
            components = setOf(BackupComponent.APK),
            compressionLevel = 6,
            encrypted = false,
            permissionMode = "ROOT",
            deviceInfo = DeviceInfo("Test", "Test", 30, "test"),
            totalSize = size,
            checksums = mapOf("test" to "hash"),
            merkleRootHash = "root_hash",
            merkleTreeJson = "{}"
        )
    }
}
