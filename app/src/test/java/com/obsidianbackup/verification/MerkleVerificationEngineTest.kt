package com.obsidianbackup.verification

import com.obsidianbackup.engine.BackupEngine
import com.obsidianbackup.model.*
import com.obsidianbackup.storage.BackupCatalog
import com.obsidianbackup.storage.BackupMetadata
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class MerkleVerificationEngineTest {
    
    private lateinit var merkleTree: MerkleTree
    private lateinit var checksumVerifier: ChecksumVerifier
    private lateinit var catalog: BackupCatalog
    private lateinit var engine: MerkleVerificationEngine
    private lateinit var tempDir: File
    
    @Before
    fun setup() {
        merkleTree = MerkleTree()
        checksumVerifier = ChecksumVerifier(merkleTree)
        catalog = mockk()
        engine = MerkleVerificationEngine(merkleTree, checksumVerifier, catalog)
        tempDir = createTempDir("merkle_engine_test")
    }
    
    @After
    fun teardown() {
        tempDir.deleteRecursively()
    }
    
    @Test
    fun generateMerkleRoot_withFiles_returnsRootHashAndSavesMetadata() = runBlocking {
        val snapshotDir = File(tempDir, "snapshot1")
        snapshotDir.mkdirs()
        
        val files = listOf(
            File(snapshotDir, "file1.txt").apply { writeText("Content 1") },
            File(snapshotDir, "file2.txt").apply { writeText("Content 2") },
            File(snapshotDir, "file3.txt").apply { writeText("Content 3") }
        )
        
        val rootHash = engine.generateMerkleRoot(files, snapshotDir)
        
        assertNotNull(rootHash)
        assertTrue(rootHash.isNotEmpty())
        
        val metadataFile = File(snapshotDir, ".merkle_metadata.json")
        assertTrue(metadataFile.exists())
        
        val proofsFile = File(snapshotDir, ".merkle_proofs.json")
        assertTrue(proofsFile.exists())
    }
    
    @Test
    fun generateMerkleRoot_withEmptyList_returnsEmptyString() = runBlocking {
        val snapshotDir = File(tempDir, "snapshot2")
        snapshotDir.mkdirs()
        
        val rootHash = engine.generateMerkleRoot(emptyList(), snapshotDir)
        
        assertEquals("", rootHash)
    }
    
    @Test
    fun generateMerkleRootFromChecksums_createsProofsFile() = runBlocking {
        val snapshotDir = File(tempDir, "snapshot3")
        snapshotDir.mkdirs()
        
        val checksums = mapOf(
            "file1.txt" to "abc123",
            "file2.txt" to "def456",
            "file3.txt" to "ghi789"
        )
        
        val rootHash = engine.generateMerkleRootFromChecksums(checksums, snapshotDir)
        
        assertNotNull(rootHash)
        assertTrue(rootHash.isNotEmpty())
        
        val proofsFile = File(snapshotDir, ".merkle_proofs.json")
        assertTrue(proofsFile.exists())
    }
    
    @Test
    fun verifyFileIncremental_withValidFile_returnsTrue() = runBlocking {
        val snapshotDir = File(tempDir, "snapshot4")
        snapshotDir.mkdirs()
        
        val files = listOf(
            File(snapshotDir, "file1.txt").apply { writeText("Content 1") },
            File(snapshotDir, "file2.txt").apply { writeText("Content 2") }
        )
        
        engine.generateMerkleRoot(files, snapshotDir)
        
        val isValid = engine.verifyFileIncremental(files[0], snapshotDir)
        
        assertTrue(isValid)
    }
    
    @Test
    fun verifyFileIncremental_withModifiedFile_returnsFalse() = runBlocking {
        val snapshotDir = File(tempDir, "snapshot5")
        snapshotDir.mkdirs()
        
        val files = listOf(
            File(snapshotDir, "file1.txt").apply { writeText("Content 1") },
            File(snapshotDir, "file2.txt").apply { writeText("Content 2") }
        )
        
        engine.generateMerkleRoot(files, snapshotDir)
        
        files[0].writeText("Modified content")
        
        val isValid = engine.verifyFileIncremental(files[0], snapshotDir)
        
        assertFalse(isValid)
    }
    
    @Test
    fun verifySnapshot_withMissingDirectory_returnsFailure() = runBlocking {
        val backupId = BackupId("nonexistent")
        val nonExistentDir = File(tempDir, "nonexistent")
        
        every { catalog.getSnapshotDirectory(backupId) } returns nonExistentDir
        
        val result = engine.verifySnapshot(backupId)
        
        assertFalse(result.allValid)
        assertEquals(0, result.filesChecked)
        assertTrue(result.corruptedFiles.isNotEmpty())
    }
    
    @Test
    fun verifySnapshot_withMerkleMetadata_performsMerkleVerification() = runBlocking {
        val snapshotDir = File(tempDir, "snapshot6")
        snapshotDir.mkdirs()
        
        val files = listOf(
            File(snapshotDir, "file1.txt").apply { writeText("Content 1") },
            File(snapshotDir, "file2.txt").apply { writeText("Content 2") }
        )
        
        engine.generateMerkleRoot(files, snapshotDir)
        
        val backupId = BackupId("snapshot6")
        every { catalog.getSnapshotDirectory(backupId) } returns snapshotDir
        
        val result = engine.verifySnapshot(backupId)
        
        assertTrue(result.allValid)
        assertEquals(2, result.filesChecked)
        assertTrue(result.corruptedFiles.isEmpty())
    }
    
    @Test
    fun verifySnapshot_withoutMerkleMetadata_fallsBackToChecksumVerification() = runBlocking {
        val snapshotDir = File(tempDir, "snapshot7")
        snapshotDir.mkdirs()
        
        val file = File(snapshotDir, "file1.txt").apply { writeText("Content 1") }
        
        val backupId = BackupId("snapshot7")
        val checksums = mapOf("file1.txt" to calculateFileChecksum(file))
        
        every { catalog.getSnapshotDirectory(backupId) } returns snapshotDir
        coEvery { catalog.getSnapshot(backupId) } returns BackupMetadata(
            snapshotId = backupId,
            timestamp = System.currentTimeMillis(),
            description = "Test",
            apps = emptyList(),
            components = emptySet(),
            compressionLevel = 6,
            encrypted = false,
            permissionMode = "auto",
            deviceInfo = com.obsidianbackup.model.DeviceInfo("", "", 0, ""),
            totalSize = 0L,
            checksums = checksums
        )
        
        val result = engine.verifySnapshot(backupId)
        
        assertTrue(result.allValid)
        assertEquals(1, result.filesChecked)
    }
    
    @Test
    fun backupApps_returnsFailure() = runBlocking {
        val request = BackupRequest(
            appIds = listOf(AppId("com.example.app")),
            components = emptySet()
        )
        
        val result = engine.backupApps(request)
        
        assertTrue(result is BackupResult.Failure)
    }
    
    @Test
    fun restoreApps_returnsFailure() = runBlocking {
        val request = RestoreRequest(
            snapshotId = SnapshotId("test"),
            appIds = listOf(AppId("com.example.app"))
        )
        
        val result = engine.restoreApps(request)
        
        assertTrue(result is RestoreResult.Failure)
    }
    
    @Test
    fun deleteSnapshot_returnsFalse() = runBlocking {
        val result = engine.deleteSnapshot(BackupId("test"))
        
        assertFalse(result)
    }
    
    @Test
    fun observeProgress_returnsFlowWithInitialState() = runBlocking {
        val progress = engine.observeProgress().first()
        
        assertEquals(OperationType.VERIFY, progress.operationType)
        assertEquals(0, progress.itemsCompleted)
        assertEquals(0, progress.totalItems)
    }
    
    @Test
    fun verifySnapshot_detectsCorruptedFiles() = runBlocking {
        val snapshotDir = File(tempDir, "snapshot8")
        snapshotDir.mkdirs()
        
        val files = listOf(
            File(snapshotDir, "file1.txt").apply { writeText("Content 1") },
            File(snapshotDir, "file2.txt").apply { writeText("Content 2") },
            File(snapshotDir, "file3.txt").apply { writeText("Content 3") }
        )
        
        engine.generateMerkleRoot(files, snapshotDir)
        
        files[1].writeText("Modified content")
        
        val backupId = BackupId("snapshot8")
        every { catalog.getSnapshotDirectory(backupId) } returns snapshotDir
        
        val result = engine.verifySnapshot(backupId)
        
        assertFalse(result.allValid)
        assertTrue(result.corruptedFiles.isNotEmpty())
        assertTrue(result.corruptedFiles.any { it.contains("file2.txt") })
    }
    
    private suspend fun calculateFileChecksum(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        
        file.inputStream().use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
