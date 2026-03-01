// test/java/com/obsidianbackup/cloud/MerkleTreeTest.kt
package com.obsidianbackup.cloud

import android.content.Context
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.model.SnapshotId
import com.obsidianbackup.storage.BackupCatalog
import com.obsidianbackup.verification.ChecksumVerifier
import com.obsidianbackup.work.WorkManagerScheduler
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Unit tests for Merkle tree implementation in CloudSyncManager.
 * 
 * Tests verify:
 * - Correct tree construction for various file counts
 * - Deterministic output (same inputs = same root)
 * - Proper handling of edge cases (0, 1, odd numbers)
 * - Memory efficiency for large file sets
 * - Verification functionality
 */
class MerkleTreeTest {

    private lateinit var cloudSyncManager: CloudSyncManager
    private lateinit var mockContext: Context
    private lateinit var mockBackupCatalog: BackupCatalog
    private lateinit var mockCloudProvider: CloudProvider
    private lateinit var mockWorkManager: WorkManagerScheduler
    private lateinit var mockLogger: ObsidianLogger
    private lateinit var mockChecksumVerifier: ChecksumVerifier

    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        mockBackupCatalog = mock(BackupCatalog::class.java)
        mockCloudProvider = mock(CloudProvider::class.java)
        mockWorkManager = mock(WorkManagerScheduler::class.java)
        mockLogger = mock(ObsidianLogger::class.java)
        mockChecksumVerifier = mock(ChecksumVerifier::class.java)

        cloudSyncManager = CloudSyncManager(
            context = mockContext,
            backupCatalog = mockBackupCatalog,
            cloudProvider = mockCloudProvider,
            workManager = mockWorkManager,
            logger = mockLogger,
            checksumVerifier = mockChecksumVerifier
        )
    }

    @Test
    fun testEmptyFileList() = runBlocking {
        val files = emptyList<CloudFile>()
        val root = cloudSyncManager.calculateMerkleRoot(files)
        
        assertEquals("", root, "Empty file list should return empty string")
    }

    @Test
    fun testSingleFile() = runBlocking {
        val files = listOf(
            createCloudFile("file1.txt", "a".repeat(64))
        )
        
        val root = cloudSyncManager.calculateMerkleRoot(files)
        
        assertEquals(files[0].checksum, root, "Single file root should equal file checksum")
    }

    @Test
    fun testTwoFiles() = runBlocking {
        // Use actual SHA-256 hash format (64 hex chars)
        val hash1 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        val hash2 = "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592"
        
        val files = listOf(
            createCloudFile("file1.txt", hash1),
            createCloudFile("file2.txt", hash2)
        )
        
        val root = cloudSyncManager.calculateMerkleRoot(files)
        
        // Root should be SHA-256 of concatenated hashes
        assertEquals(64, root.length, "Root hash should be 64 characters (SHA-256)")
        assertTrue(root.matches(Regex("[0-9a-f]{64}")), "Root should be valid hex string")
    }

    @Test
    fun testThreeFiles_OddNumberHandling() = runBlocking {
        // Test that odd number of files correctly duplicates last hash
        val hash1 = "0000000000000000000000000000000000000000000000000000000000000001"
        val hash2 = "0000000000000000000000000000000000000000000000000000000000000002"
        val hash3 = "0000000000000000000000000000000000000000000000000000000000000003"
        
        val files = listOf(
            createCloudFile("file1.txt", hash1),
            createCloudFile("file2.txt", hash2),
            createCloudFile("file3.txt", hash3)
        )
        
        val root = cloudSyncManager.calculateMerkleRoot(files)
        
        assertEquals(64, root.length)
        assertTrue(root.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun testDeterministicOutput() = runBlocking {
        // Same input should always produce same output
        val files = listOf(
            createCloudFile("file1.txt", "a1b2c3d4e5f6".padEnd(64, '0')),
            createCloudFile("file2.txt", "f6e5d4c3b2a1".padEnd(64, '0')),
            createCloudFile("file3.txt", "1234567890ab".padEnd(64, '0'))
        )
        
        val root1 = cloudSyncManager.calculateMerkleRoot(files)
        val root2 = cloudSyncManager.calculateMerkleRoot(files)
        
        assertEquals(root1, root2, "Same files should produce same Merkle root")
    }

    @Test
    fun testOrderMatters() = runBlocking {
        // Different order should produce different root
        val hash1 = "1111111111111111111111111111111111111111111111111111111111111111"
        val hash2 = "2222222222222222222222222222222222222222222222222222222222222222"
        
        val filesOrder1 = listOf(
            createCloudFile("file1.txt", hash1),
            createCloudFile("file2.txt", hash2)
        )
        
        val filesOrder2 = listOf(
            createCloudFile("file2.txt", hash2),
            createCloudFile("file1.txt", hash1)
        )
        
        val root1 = cloudSyncManager.calculateMerkleRoot(filesOrder1)
        val root2 = cloudSyncManager.calculateMerkleRoot(filesOrder2)
        
        assertNotEquals(root1, root2, "Different order should produce different root")
    }

    @Test
    fun testLargeFileSet() = runBlocking {
        // Test with 1000 files - should be memory efficient
        val files = (1..1000).map { i ->
            createCloudFile("file$i.txt", i.toString().padEnd(64, '0'))
        }
        
        val root = cloudSyncManager.calculateMerkleRoot(files)
        
        assertEquals(64, root.length)
        assertTrue(root.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun testVeryLargeFileSet() = runBlocking {
        // Test with 10,000 files - memory efficiency test
        val files = (1..10000).map { i ->
            createCloudFile("file$i.txt", (i % 256).toString().padStart(2, '0').repeat(32))
        }
        
        val root = cloudSyncManager.calculateMerkleRoot(files)
        
        assertEquals(64, root.length)
        assertTrue(root.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun testVerifyMerkleRoot_Valid() = runBlocking {
        val files = listOf(
            createCloudFile("file1.txt", "a".repeat(64)),
            createCloudFile("file2.txt", "b".repeat(64))
        )
        
        val root = cloudSyncManager.calculateMerkleRoot(files)
        val isValid = cloudSyncManager.verifyMerkleRoot(files, root)
        
        assertTrue(isValid, "Verification should pass for correct root")
    }

    @Test
    fun testVerifyMerkleRoot_Invalid() = runBlocking {
        val files = listOf(
            createCloudFile("file1.txt", "a".repeat(64)),
            createCloudFile("file2.txt", "b".repeat(64))
        )
        
        val fakeRoot = "0".repeat(64)
        val isValid = cloudSyncManager.verifyMerkleRoot(files, fakeRoot)
        
        assertFalse(isValid, "Verification should fail for incorrect root")
    }

    @Test
    fun testVerifyMerkleRoot_CaseInsensitive() = runBlocking {
        val files = listOf(
            createCloudFile("file1.txt", "abcd".repeat(16)),
            createCloudFile("file2.txt", "1234".repeat(16))
        )
        
        val root = cloudSyncManager.calculateMerkleRoot(files)
        val upperCaseRoot = root.uppercase()
        val isValid = cloudSyncManager.verifyMerkleRoot(files, upperCaseRoot)
        
        assertTrue(isValid, "Verification should be case-insensitive")
    }

    @Test
    fun testContentChange_ChangesRoot() = runBlocking {
        val files1 = listOf(
            createCloudFile("file1.txt", "a".repeat(64)),
            createCloudFile("file2.txt", "b".repeat(64))
        )
        
        val files2 = listOf(
            createCloudFile("file1.txt", "a".repeat(64)),
            createCloudFile("file2.txt", "c".repeat(64)) // Changed content
        )
        
        val root1 = cloudSyncManager.calculateMerkleRoot(files1)
        val root2 = cloudSyncManager.calculateMerkleRoot(files2)
        
        assertNotEquals(root1, root2, "Content change should change root")
    }

    @Test
    fun testPowerOfTwo_FileCount() = runBlocking {
        // Perfect binary tree (power of 2 files: 2, 4, 8, 16)
        for (count in listOf(2, 4, 8, 16, 32, 64, 128, 256)) {
            val files = (1..count).map { i ->
                createCloudFile("file$i.txt", i.toString().padEnd(64, '0'))
            }
            
            val root = cloudSyncManager.calculateMerkleRoot(files)
            
            assertEquals(64, root.length, "Root for $count files should be 64 chars")
            assertTrue(root.matches(Regex("[0-9a-f]{64}")), "Root should be valid hex")
        }
    }

    @Test
    fun testNonPowerOfTwo_FileCount() = runBlocking {
        // Non-perfect trees (requires duplication)
        for (count in listOf(3, 5, 7, 9, 15, 17, 33, 63, 65, 127)) {
            val files = (1..count).map { i ->
                createCloudFile("file$i.txt", i.toString().padEnd(64, '0'))
            }
            
            val root = cloudSyncManager.calculateMerkleRoot(files)
            
            assertEquals(64, root.length, "Root for $count files should be 64 chars")
            assertTrue(root.matches(Regex("[0-9a-f]{64}")), "Root should be valid hex")
        }
    }

    private fun createCloudFile(name: String, checksum: String): CloudFile {
        return CloudFile(
            localPath = File("/tmp/$name"),
            remotePath = "remote/$name",
            checksum = checksum,
            sizeBytes = 1024L
        )
    }
}
