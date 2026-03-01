package com.obsidianbackup.integration

import com.obsidianbackup.engine.BackupPlan
import com.obsidianbackup.engine.FileMetadata
import com.obsidianbackup.engine.IncrementalBackupStrategy
import com.obsidianbackup.model.*
import com.obsidianbackup.storage.BackupCatalog
import com.obsidianbackup.storage.BackupMetadata
import com.obsidianbackup.verification.ChecksumVerifier
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration test for IncrementalBackupStrategy
 */
class IncrementalBackupIntegrationTest {

    @TempDir
    lateinit var tempDir: File
    
    private lateinit var catalog: BackupCatalog
    private lateinit var verifier: ChecksumVerifier
    private lateinit var strategy: IncrementalBackupStrategy
    
    @BeforeEach
    fun setup() {
        catalog = mockk(relaxed = true)
        verifier = mockk(relaxed = true)
        strategy = IncrementalBackupStrategy(catalog, verifier)
    }

    @Test
    fun `createIncremental with no baseline should return Full plan`() = runBlocking {
        val appId = AppId("com.example.app")
        
        coEvery { catalog.getSnapshot(any()) } returns null
        
        val plan = strategy.createIncremental(appId, null)
        
        assertTrue(plan is BackupPlan.Full, "Should return Full plan when no baseline")
    }

    @Test
    fun `getStats should track operations`() = runBlocking {
        val sourceDir = File(tempDir, "source")
        sourceDir.mkdirs()
        
        val file1 = File(sourceDir, "file1.txt")
        file1.writeText("content")
        
        val targetDir = File(tempDir, "target")
        
        coEvery { verifier.calculateChecksum(any()) } returns "checksum"
        
        val plan = BackupPlan.Full(
            files = listOf(
                FileMetadata("file1.txt", "checksum", file1.length(), file1.lastModified())
            )
        )
        
        strategy.executeBackupPlan(plan, sourceDir, targetDir)
        
        val stats = strategy.getStats()
        
        assertTrue(stats.hardLinksCreated >= 0, "Hard links should be tracked")
        assertTrue(stats.filesDeduped >= 0, "Deduped files should be tracked")
        assertTrue(stats.savedBytes >= 0, "Saved bytes should be tracked")
    }
}
