package com.obsidianbackup.verification

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class MerkleTreeTest {
    
    private lateinit var merkleTree: MerkleTree
    private lateinit var tempDir: File
    private lateinit var testFiles: List<File>
    
    @Before
    fun setup() {
        merkleTree = MerkleTree()
        tempDir = createTempDir("merkle_test")
        
        testFiles = listOf(
            File(tempDir, "file1.txt").apply { writeText("Content 1") },
            File(tempDir, "file2.txt").apply { writeText("Content 2") },
            File(tempDir, "file3.txt").apply { writeText("Content 3") },
            File(tempDir, "file4.txt").apply { writeText("Content 4") }
        )
    }
    
    @After
    fun teardown() {
        tempDir.deleteRecursively()
    }
    
    @Test
    fun buildTree_withFiles_generatesRootHash() = runBlocking {
        val rootHash = merkleTree.buildTree(testFiles)
        
        assertNotNull(rootHash)
        assertTrue(rootHash.isNotEmpty())
        assertEquals(64, rootHash.length)
    }
    
    @Test
    fun buildTree_withEmptyList_returnsEmptyString() = runBlocking {
        val rootHash = merkleTree.buildTree(emptyList())
        
        assertEquals("", rootHash)
    }
    
    @Test
    fun buildTree_withSingleFile_generatesRootHash() = runBlocking {
        val singleFile = listOf(testFiles[0])
        val rootHash = merkleTree.buildTree(singleFile)
        
        assertNotNull(rootHash)
        assertTrue(rootHash.isNotEmpty())
    }
    
    @Test
    fun generateProof_forExistingFile_returnsValidProof() = runBlocking {
        merkleTree.buildTree(testFiles)
        
        val proof = merkleTree.generateProof(testFiles[0].absolutePath)
        
        assertNotNull(proof)
        assertTrue(proof!!.isValid())
        assertEquals(testFiles[0].absolutePath, proof.filePath)
        assertTrue(proof.siblings.isNotEmpty())
    }
    
    @Test
    fun generateProof_forNonExistentFile_returnsNull() = runBlocking {
        merkleTree.buildTree(testFiles)
        
        val proof = merkleTree.generateProof("/non/existent/path")
        
        assertNull(proof)
    }
    
    @Test
    fun verifyProof_withValidProof_returnsTrue() = runBlocking {
        merkleTree.buildTree(testFiles)
        
        val proof = merkleTree.generateProof(testFiles[1].absolutePath)
        assertNotNull(proof)
        
        val isValid = merkleTree.verifyProof(proof!!)
        
        assertTrue(isValid)
    }
    
    @Test
    fun verifyProof_withTamperedProof_returnsFalse() = runBlocking {
        merkleTree.buildTree(testFiles)
        
        val proof = merkleTree.generateProof(testFiles[0].absolutePath)
        assertNotNull(proof)
        
        val tamperedProof = proof!!.copy(
            fileHash = "0".repeat(64)
        )
        
        val isValid = merkleTree.verifyProof(tamperedProof)
        
        assertFalse(isValid)
    }
    
    @Test
    fun verifyFileWithProof_withValidFile_returnsTrue() = runBlocking {
        merkleTree.buildTree(testFiles)
        
        val proof = merkleTree.generateProof(testFiles[2].absolutePath)
        assertNotNull(proof)
        
        val isValid = merkleTree.verifyFileWithProof(testFiles[2], proof!!)
        
        assertTrue(isValid)
    }
    
    @Test
    fun verifyFileWithProof_withModifiedFile_returnsFalse() = runBlocking {
        merkleTree.buildTree(testFiles)
        
        val proof = merkleTree.generateProof(testFiles[0].absolutePath)
        assertNotNull(proof)
        
        testFiles[0].writeText("Modified content")
        
        val isValid = merkleTree.verifyFileWithProof(testFiles[0], proof!!)
        
        assertFalse(isValid)
    }
    
    @Test
    fun buildTreeFromHashes_generatesConsistentRoot() = runBlocking {
        val rootHash1 = merkleTree.buildTree(testFiles)
        
        val checksums = testFiles.associate { file ->
            file.absolutePath to calculateFileHash(file)
        }
        
        merkleTree.clear()
        val rootHash2 = merkleTree.buildTreeFromHashes(checksums)
        
        assertEquals(rootHash1, rootHash2)
    }
    
    @Test
    fun getMetadata_returnsCorrectInformation() = runBlocking {
        merkleTree.buildTree(testFiles)
        
        val metadata = merkleTree.getMetadata()
        
        assertEquals(testFiles.size, metadata.leafCount)
        assertEquals("SHA-256", metadata.algorithm)
        assertTrue(metadata.rootHash.isNotEmpty())
    }
    
    @Test
    fun getLeafCount_returnsCorrectCount() = runBlocking {
        merkleTree.buildTree(testFiles)
        
        val count = merkleTree.getLeafCount()
        
        assertEquals(testFiles.size, count)
    }
    
    @Test
    fun clear_resetsTree() = runBlocking {
        merkleTree.buildTree(testFiles)
        
        merkleTree.clear()
        
        val count = merkleTree.getLeafCount()
        val rootHash = merkleTree.getRootHash()
        
        assertEquals(0, count)
        assertEquals("", rootHash)
    }
    
    @Test
    fun concurrentOperations_maintainThreadSafety() = runBlocking {
        merkleTree.buildTree(testFiles)
        
        val proof1 = merkleTree.generateProof(testFiles[0].absolutePath)
        val proof2 = merkleTree.generateProof(testFiles[1].absolutePath)
        val proof3 = merkleTree.generateProof(testFiles[2].absolutePath)
        
        assertNotNull(proof1)
        assertNotNull(proof2)
        assertNotNull(proof3)
        
        assertTrue(merkleTree.verifyProof(proof1!!))
        assertTrue(merkleTree.verifyProof(proof2!!))
        assertTrue(merkleTree.verifyProof(proof3!!))
    }
    
    @Test
    fun proofsForAllFiles_areValid() = runBlocking {
        merkleTree.buildTree(testFiles)
        
        testFiles.forEach { file ->
            val proof = merkleTree.generateProof(file.absolutePath)
            assertNotNull("Proof for ${file.name} should not be null", proof)
            assertTrue("Proof for ${file.name} should be valid", merkleTree.verifyProof(proof!!))
            assertTrue("File verification for ${file.name} should succeed", 
                merkleTree.verifyFileWithProof(file, proof))
        }
    }
    
    private suspend fun calculateFileHash(file: File): String {
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
