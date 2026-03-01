// crypto/ZeroKnowledgeEncryptionTest.kt
package com.obsidianbackup.crypto

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.runBlocking
import java.io.File
import java.security.SecureRandom
import javax.crypto.SecretKey

/**
 * Unit tests for ZeroKnowledgeEncryption
 * 
 * Tests cryptographic operations without Android dependencies
 */
class ZeroKnowledgeEncryptionTest {
    
    private lateinit var zkEncryption: ZeroKnowledgeEncryption
    
    @Before
    fun setup() {
        zkEncryption = ZeroKnowledgeEncryption()
    }
    
    @Test
    fun `test salt generation produces unique values`() {
        val salt1 = zkEncryption.generateSalt()
        val salt2 = zkEncryption.generateSalt()
        
        assertEquals(32, salt1.size)
        assertEquals(32, salt2.size)
        assertFalse(salt1.contentEquals(salt2))
    }
    
    @Test
    fun `test key derivation produces consistent keys`() = runBlocking {
        val passphrase = "TestPassphrase123!".toCharArray()
        val salt = zkEncryption.generateSalt()
        
        val key1 = zkEncryption.deriveKeyFromPassphrase(passphrase, salt)
        val key2 = zkEncryption.deriveKeyFromPassphrase(passphrase.clone(), salt)
        
        assertNotNull(key1)
        assertNotNull(key2)
        assertArrayEquals(key1.encoded, key2.encoded)
    }
    
    @Test
    fun `test key derivation with different salts produces different keys`() = runBlocking {
        val passphrase = "TestPassphrase123!".toCharArray()
        val salt1 = zkEncryption.generateSalt()
        val salt2 = zkEncryption.generateSalt()
        
        val key1 = zkEncryption.deriveKeyFromPassphrase(passphrase, salt1)
        val key2 = zkEncryption.deriveKeyFromPassphrase(passphrase.clone(), salt2)
        
        assertFalse(key1.encoded.contentEquals(key2.encoded))
    }
    
    @Test
    fun `test key derivation with different passphrases produces different keys`() = runBlocking {
        val passphrase1 = "TestPassphrase1".toCharArray()
        val passphrase2 = "TestPassphrase2".toCharArray()
        val salt = zkEncryption.generateSalt()
        
        val key1 = zkEncryption.deriveKeyFromPassphrase(passphrase1, salt)
        val key2 = zkEncryption.deriveKeyFromPassphrase(passphrase2, salt)
        
        assertFalse(key1.encoded.contentEquals(key2.encoded))
    }
    
    @Test
    fun `test encryption and decryption roundtrip`() = runBlocking {
        val plaintext = "Hello, Zero-Knowledge World!".toByteArray()
        val key = zkEncryption.generateMasterKey()
        
        val encrypted = zkEncryption.encrypt(plaintext, key)
        val decrypted = zkEncryption.decrypt(encrypted, key)
        
        assertArrayEquals(plaintext, decrypted)
    }
    
    @Test
    fun `test encryption produces different ciphertext for same plaintext`() = runBlocking {
        val plaintext = "Same plaintext".toByteArray()
        val key = zkEncryption.generateMasterKey()
        
        val encrypted1 = zkEncryption.encrypt(plaintext, key)
        val encrypted2 = zkEncryption.encrypt(plaintext, key)
        
        // Due to random IV, ciphertexts should differ
        assertFalse(encrypted1.contentEquals(encrypted2))
        
        // But both should decrypt to same plaintext
        assertArrayEquals(plaintext, zkEncryption.decrypt(encrypted1, key))
        assertArrayEquals(plaintext, zkEncryption.decrypt(encrypted2, key))
    }
    
    @Test(expected = Exception::class)
    fun `test decryption with wrong key fails`() = runBlocking {
        val plaintext = "Secret data".toByteArray()
        val correctKey = zkEncryption.generateMasterKey()
        val wrongKey = zkEncryption.generateMasterKey()
        
        val encrypted = zkEncryption.encrypt(plaintext, correctKey)
        
        // This should throw due to authentication tag mismatch
        zkEncryption.decrypt(encrypted, wrongKey)
    }
    
    @Test(expected = Exception::class)
    fun `test decryption with tampered ciphertext fails`() = runBlocking {
        val plaintext = "Secret data".toByteArray()
        val key = zkEncryption.generateMasterKey()
        
        val encrypted = zkEncryption.encrypt(plaintext, key)
        
        // Tamper with ciphertext (flip a bit)
        encrypted[encrypted.size / 2] = (encrypted[encrypted.size / 2].toInt() xor 1).toByte()
        
        // This should throw due to authentication tag mismatch
        zkEncryption.decrypt(encrypted, key)
    }
    
    @Test
    fun `test empty data encryption and decryption`() = runBlocking {
        val plaintext = ByteArray(0)
        val key = zkEncryption.generateMasterKey()
        
        val encrypted = zkEncryption.encrypt(plaintext, key)
        val decrypted = zkEncryption.decrypt(encrypted, key)
        
        assertArrayEquals(plaintext, decrypted)
    }
    
    @Test
    fun `test large data encryption and decryption`() = runBlocking {
        val plaintext = ByteArray(1_000_000) { it.toByte() }
        val key = zkEncryption.generateMasterKey()
        
        val encrypted = zkEncryption.encrypt(plaintext, key)
        val decrypted = zkEncryption.decrypt(encrypted, key)
        
        assertArrayEquals(plaintext, decrypted)
    }
    
    @Test
    fun `test key backup export and import`() = runBlocking {
        val masterKey = zkEncryption.generateMasterKey()
        val backupPassphrase = "BackupPassphrase123!".toCharArray()
        
        // Export
        val backup = zkEncryption.exportKeyBackup(masterKey, backupPassphrase)
        assertNotNull(backup)
        assertTrue(backup.isNotEmpty())
        
        // Import
        val restoredKey = zkEncryption.importKeyBackup(backup, backupPassphrase.clone())
        
        assertArrayEquals(masterKey.encoded, restoredKey.encoded)
    }
    
    @Test(expected = Exception::class)
    fun `test key backup import with wrong passphrase fails`() = runBlocking {
        val masterKey = zkEncryption.generateMasterKey()
        val correctPassphrase = "CorrectPassphrase".toCharArray()
        val wrongPassphrase = "WrongPassphrase".toCharArray()
        
        val backup = zkEncryption.exportKeyBackup(masterKey, correctPassphrase)
        
        // This should fail authentication
        zkEncryption.importKeyBackup(backup, wrongPassphrase)
    }
    
    @Test
    fun `test searchable hash produces deterministic output`() = runBlocking {
        val term = "searchable"
        val key = zkEncryption.generateMasterKey()
        
        val hash1 = zkEncryption.searchableHash(term, key)
        val hash2 = zkEncryption.searchableHash(term, key)
        
        assertArrayEquals(hash1, hash2)
    }
    
    @Test
    fun `test searchable hash with different keys produces different hashes`() = runBlocking {
        val term = "searchable"
        val key1 = zkEncryption.generateMasterKey()
        val key2 = zkEncryption.generateMasterKey()
        
        val hash1 = zkEncryption.searchableHash(term, key1)
        val hash2 = zkEncryption.searchableHash(term, key2)
        
        assertFalse(hash1.contentEquals(hash2))
    }
    
    @Test
    fun `test search index creation`() = runBlocking {
        val content = "This is a test document with searchable terms"
        val key = zkEncryption.generateMasterKey()
        
        val index = zkEncryption.createSearchIndex(content, key)
        
        assertNotNull(index)
        assertTrue(index.isNotEmpty())
        
        // Check that some terms are indexed (lowercase, min 3 chars)
        assertTrue(index.containsKey("this"))
        assertTrue(index.containsKey("test"))
        assertTrue(index.containsKey("document"))
        assertTrue(index.containsKey("searchable"))
        assertTrue(index.containsKey("terms"))
        
        // Short terms should not be indexed
        assertFalse(index.containsKey("is"))
        assertFalse(index.containsKey("a"))
    }
    
    @Test
    fun `test verify integrity with correct key`() = runBlocking {
        val plaintext = "Integrity test data".toByteArray()
        val key = zkEncryption.generateMasterKey()
        
        val encrypted = zkEncryption.encrypt(plaintext, key)
        val valid = zkEncryption.verifyIntegrity(encrypted, key)
        
        assertTrue(valid)
    }
    
    @Test
    fun `test verify integrity with wrong key`() = runBlocking {
        val plaintext = "Integrity test data".toByteArray()
        val correctKey = zkEncryption.generateMasterKey()
        val wrongKey = zkEncryption.generateMasterKey()
        
        val encrypted = zkEncryption.encrypt(plaintext, correctKey)
        val valid = zkEncryption.verifyIntegrity(encrypted, wrongKey)
        
        assertFalse(valid)
    }
    
    @Test
    fun `test verify integrity with tampered data`() = runBlocking {
        val plaintext = "Integrity test data".toByteArray()
        val key = zkEncryption.generateMasterKey()
        
        val encrypted = zkEncryption.encrypt(plaintext, key)
        
        // Tamper
        encrypted[encrypted.size / 2] = (encrypted[encrypted.size / 2].toInt() xor 1).toByte()
        
        val valid = zkEncryption.verifyIntegrity(encrypted, key)
        
        assertFalse(valid)
    }
    
    @Test
    fun `test secure wipe of byte array`() {
        val data = ByteArray(32) { 0xFF.toByte() }
        
        zkEncryption.secureWipe(data)
        
        // Data should be zeroed
        assertTrue(data.all { it == 0.toByte() })
    }
    
    @Test
    fun `test secure wipe of char array`() {
        val data = CharArray(32) { 'A' }
        
        zkEncryption.secureWipe(data)
        
        // Data should be zeroed
        assertTrue(data.all { it == '\u0000' })
    }
    
    @Test
    fun `test master key generation produces 256-bit keys`() {
        val key = zkEncryption.generateMasterKey()
        
        assertEquals(32, key.encoded.size) // 256 bits = 32 bytes
        assertEquals("AES", key.algorithm)
    }
    
    @Test
    fun `test encryption overhead is minimal`() = runBlocking {
        val plaintext = ByteArray(1000)
        val key = zkEncryption.generateMasterKey()
        
        val encrypted = zkEncryption.encrypt(plaintext, key)
        
        // Overhead: 12 bytes IV + 16 bytes auth tag = 28 bytes
        assertEquals(plaintext.size + 28, encrypted.size)
    }
    
    @Test
    fun `test PBKDF2 iterations are sufficient`() = runBlocking {
        // This test verifies that key derivation takes reasonable time
        // (not too fast = vulnerable to brute force)
        val passphrase = "TestPassphrase".toCharArray()
        val salt = zkEncryption.generateSalt()
        
        val startTime = System.currentTimeMillis()
        zkEncryption.deriveKeyFromPassphrase(passphrase, salt)
        val duration = System.currentTimeMillis() - startTime
        
        // Should take at least 100ms (600k iterations)
        // On most devices, this takes 1-2 seconds
        assertTrue("Key derivation too fast: ${duration}ms", duration > 100)
    }
    
    @Test
    fun `test encrypted data format includes IV`() = runBlocking {
        val plaintext = "Test".toByteArray()
        val key = zkEncryption.generateMasterKey()
        
        val encrypted = zkEncryption.encrypt(plaintext, key)
        
        // First 12 bytes should be IV
        val iv = encrypted.sliceArray(0 until 12)
        
        // IV should be unique (not all zeros)
        assertFalse(iv.all { it == 0.toByte() })
    }
}

/**
 * Integration tests requiring file system access
 * (May need to be run as Android instrumentation tests)
 */
class ZeroKnowledgeEncryptionFileTest {
    
    private lateinit var zkEncryption: ZeroKnowledgeEncryption
    private lateinit var tempDir: File
    
    @Before
    fun setup() {
        zkEncryption = ZeroKnowledgeEncryption()
        tempDir = File(System.getProperty("java.io.tmpdir"), "zk_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
    }
    
    @Test
    fun `test file encryption and decryption`() = runBlocking {
        val inputFile = File(tempDir, "input.txt")
        val encryptedFile = File(tempDir, "encrypted.zkenc")
        val decryptedFile = File(tempDir, "decrypted.txt")
        
        val originalContent = "Test file content for encryption"
        inputFile.writeText(originalContent)
        
        val key = zkEncryption.generateMasterKey()
        val salt = zkEncryption.generateSalt()
        
        // Encrypt
        zkEncryption.encryptFile(inputFile, encryptedFile, key, salt)
        assertTrue(encryptedFile.exists())
        assertTrue(encryptedFile.length() > inputFile.length())
        
        // Decrypt
        val recoveredSalt = zkEncryption.decryptFile(encryptedFile, decryptedFile, key)
        assertTrue(decryptedFile.exists())
        assertArrayEquals(salt, recoveredSalt)
        
        // Verify content
        assertEquals(originalContent, decryptedFile.readText())
        
        // Cleanup
        inputFile.delete()
        encryptedFile.delete()
        decryptedFile.delete()
    }
    
    @Test
    fun `test large file encryption`() = runBlocking {
        val inputFile = File(tempDir, "large_input.bin")
        val encryptedFile = File(tempDir, "large_encrypted.zkenc")
        val decryptedFile = File(tempDir, "large_decrypted.bin")
        
        // Create 10MB file
        val originalContent = ByteArray(10 * 1024 * 1024) { it.toByte() }
        inputFile.writeBytes(originalContent)
        
        val key = zkEncryption.generateMasterKey()
        val salt = zkEncryption.generateSalt()
        
        // Encrypt
        zkEncryption.encryptFile(inputFile, encryptedFile, key, salt)
        
        // Decrypt
        zkEncryption.decryptFile(encryptedFile, decryptedFile, key)
        
        // Verify
        assertArrayEquals(originalContent, decryptedFile.readBytes())
        
        // Cleanup
        inputFile.delete()
        encryptedFile.delete()
        decryptedFile.delete()
    }
}
