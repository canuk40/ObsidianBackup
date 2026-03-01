// crypto/ZeroKnowledgeEncryption.kt
package com.obsidianbackup.crypto

import android.security.keystore.KeyProperties
import android.util.Base64
import com.obsidianbackup.security.SecureMemory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor

/**
 * Zero-Knowledge Encryption Engine
 * 
 * SECURITY PROPERTIES:
 * 1. Client-side only encryption - keys never leave device
 * 2. User-managed keys - no cloud recovery possible
 * 3. PBKDF2 with 600k iterations for key derivation
 * 4. AES-256-GCM for encryption
 * 5. Cryptographically secure random for all randomness
 * 6. No telemetry, no analytics, no key escrow
 * 
 * KEY LOSS = DATA LOSS - User is solely responsible for key backup
 */
class ZeroKnowledgeEncryption {
    
    companion object {
        // Encryption constants
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val KEY_SIZE = 256 // bits
        private const val IV_SIZE = 12 // 96 bits for GCM
        private const val TAG_SIZE = 128 // bits
        private const val SALT_SIZE = 32 // 256 bits
        
        // PBKDF2 constants - OWASP recommended
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA512"
        private const val PBKDF2_ITERATIONS = 600_000 // OWASP 2023 recommendation
        
        // Key backup format
        private const val KEY_BACKUP_VERSION = 1
        private const val KEY_BACKUP_MAGIC = "OBZKE" // ObsidianBackup Zero-Knowledge Encryption
        
        // Buffer size for streaming
        private const val BUFFER_SIZE = 8192
        
        // Metadata prefix for encrypted files
        private const val ENCRYPTED_FILE_MAGIC = "OBZKEF" // ZK Encrypted File
        private const val METADATA_VERSION = 1
    }
    
    private val secureRandom = SecureRandom()
    
    /**
     * Derive encryption key from passphrase using PBKDF2
     * This is the PRIMARY key derivation method for zero-knowledge mode
     * 
     * @param passphrase User's master passphrase (must be strong)
     * @param salt Cryptographic salt (generated randomly for new keys)
     * @return Derived SecretKey suitable for AES-256-GCM
     * Note: passphrase is wiped after use
     */
    suspend fun deriveKeyFromPassphrase(
        passphrase: CharArray,
        salt: ByteArray
    ): SecretKey = withContext(Dispatchers.Default) {
        require(passphrase.isNotEmpty()) { "Passphrase cannot be empty" }
        require(salt.size == SALT_SIZE) { "Salt must be $SALT_SIZE bytes" }
        
        try {
            val keySpec: KeySpec = PBEKeySpec(
                passphrase,
                salt,
                PBKDF2_ITERATIONS,
                KEY_SIZE
            )
            
            val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            val keyBytes = factory.generateSecret(keySpec).encoded
            try {
                SecretKeySpec(keyBytes, KEY_ALGORITHM)
            } finally {
                SecureMemory.wipe(keyBytes)
            }
        } finally {
            // Clear passphrase from memory
            SecureMemory.wipe(passphrase)
        }
    }
    
    /**
     * Generate new random salt for key derivation
     */
    fun generateSalt(): ByteArray {
        return ByteArray(SALT_SIZE).apply {
            secureRandom.nextBytes(this)
        }
    }
    
    /**
     * Generate random master key directly (for import/export)
     * Note: Caller is responsible for securely wiping the key bytes if needed
     */
    fun generateMasterKey(): SecretKey {
        val keyBytes = ByteArray(KEY_SIZE / 8)
        try {
            secureRandom.nextBytes(keyBytes)
            return SecretKeySpec(keyBytes, KEY_ALGORITHM)
        } finally {
            SecureMemory.wipe(keyBytes)
        }
    }
    
    /**
     * Encrypt data with zero-knowledge encryption
     * 
     * @param plaintext Data to encrypt
     * @param key Master encryption key
     * @return Encrypted data with IV prepended
     */
    suspend fun encrypt(
        plaintext: ByteArray,
        key: SecretKey
    ): ByteArray = withContext(Dispatchers.Default) {
        val iv = ByteArray(IV_SIZE).apply {
            secureRandom.nextBytes(this)
        }
        
        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)
        
        val ciphertext = cipher.doFinal(plaintext)
        
        // Format: IV || ciphertext (includes auth tag)
        iv + ciphertext
    }
    
    /**
     * Decrypt data encrypted with zero-knowledge encryption
     * 
     * @param encrypted Encrypted data with IV prepended
     * @param key Master encryption key
     * @return Decrypted plaintext
     */
    suspend fun decrypt(
        encrypted: ByteArray,
        key: SecretKey
    ): ByteArray = withContext(Dispatchers.Default) {
        require(encrypted.size > IV_SIZE) { "Invalid encrypted data size" }
        
        val iv = encrypted.sliceArray(0 until IV_SIZE)
        val ciphertext = encrypted.sliceArray(IV_SIZE until encrypted.size)
        
        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
        
        cipher.doFinal(ciphertext)
    }
    
    /**
     * Encrypt a file with streaming encryption
     * Adds metadata header with salt for key derivation tracking
     * 
     * @param inputFile File to encrypt
     * @param outputFile Encrypted output file
     * @param key Encryption key
     * @param salt Salt used for key derivation (stored in header)
     */
    suspend fun encryptFile(
        inputFile: File,
        outputFile: File,
        key: SecretKey,
        salt: ByteArray
    ) = withContext(Dispatchers.IO) {
        val iv = ByteArray(IV_SIZE).apply {
            secureRandom.nextBytes(this)
        }
        
        FileInputStream(inputFile).buffered(8192).use { input ->
            FileOutputStream(outputFile).buffered(8192).use { output ->
                // Write metadata header
                writeEncryptedFileHeader(output, salt, iv)
                
                // Initialize cipher
                val cipher = Cipher.getInstance(ALGORITHM)
                val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
                cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)
                
                // Stream encryption
                val buffer = ByteArray(BUFFER_SIZE)
                try {
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        val encrypted = cipher.update(buffer, 0, bytesRead)
                        if (encrypted != null) {
                            output.write(encrypted)
                        }
                    }
                    
                    // Finalize and write authentication tag
                    val finalBlock = cipher.doFinal()
                    if (finalBlock != null) {
                        output.write(finalBlock)
                    }
                } finally {
                    SecureMemory.wipe(buffer)
                }
            }
        }
    }
    
    /**
     * Decrypt a file with streaming decryption
     * 
     * @param encryptedFile Encrypted input file
     * @param outputFile Decrypted output file
     * @param key Decryption key
     * @return Salt used for key derivation (from header)
     */
    suspend fun decryptFile(
        encryptedFile: File,
        outputFile: File,
        key: SecretKey
    ): ByteArray = withContext(Dispatchers.IO) {
        FileInputStream(encryptedFile).buffered(8192).use { input ->
            // Read and validate metadata header
            val (salt, iv) = readEncryptedFileHeader(input)
            
            // Initialize cipher
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
            
            FileOutputStream(outputFile).buffered(8192).use { output ->
                // Stream decryption
                val buffer = ByteArray(BUFFER_SIZE)
                try {
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        val decrypted = cipher.update(buffer, 0, bytesRead)
                        if (decrypted != null) {
                            output.write(decrypted)
                        }
                    }
                    
                    // Finalize and verify authentication tag
                    val finalBlock = cipher.doFinal()
                    if (finalBlock != null) {
                        output.write(finalBlock)
                    }
                } finally {
                    SecureMemory.wipe(buffer)
                }
            }
            
            salt
        }
    }
    
    /**
     * Export master key encrypted with backup passphrase
     * This allows user to backup their key securely
     * 
     * @param masterKey The master encryption key to backup
     * @param backupPassphrase User's backup passphrase (can be different from master)
     * @return Base64-encoded encrypted key backup
     */
    suspend fun exportKeyBackup(
        masterKey: SecretKey,
        backupPassphrase: CharArray
    ): String = withContext(Dispatchers.Default) {
        // Generate salt for backup encryption
        val backupSalt = generateSalt()
        
        // Derive backup encryption key from backup passphrase
        val backupKey = deriveKeyFromPassphrase(backupPassphrase, backupSalt)
        
        // Encrypt master key
        val masterKeyBytes = masterKey.encoded
        val encryptedMasterKey = encrypt(masterKeyBytes, backupKey)
        
        // Format: MAGIC | VERSION | SALT | ENCRYPTED_KEY
        val backup = ByteArrayOutputStream().apply {
            write(KEY_BACKUP_MAGIC.toByteArray(Charsets.UTF_8))
            write(KEY_BACKUP_VERSION)
            write(backupSalt)
            write(encryptedMasterKey)
        }.toByteArray()
        
        // Encode as Base64 for easy storage/transmission
        Base64.encodeToString(backup, Base64.NO_WRAP)
    }
    
    /**
     * Import master key from encrypted backup
     * 
     * @param backupData Base64-encoded encrypted key backup
     * @param backupPassphrase User's backup passphrase
     * @return Restored master key
     */
    suspend fun importKeyBackup(
        backupData: String,
        backupPassphrase: CharArray
    ): SecretKey = withContext(Dispatchers.Default) {
        val backup = Base64.decode(backupData, Base64.NO_WRAP)
        val input = ByteArrayInputStream(backup)
        
        // Verify magic bytes
        val magic = ByteArray(KEY_BACKUP_MAGIC.length)
        input.read(magic)
        require(magic.contentEquals(KEY_BACKUP_MAGIC.toByteArray(Charsets.UTF_8))) {
            "Invalid key backup format"
        }
        
        // Read version
        val version = input.read()
        require(version == KEY_BACKUP_VERSION) {
            "Unsupported key backup version: $version"
        }
        
        // Read salt
        val backupSalt = ByteArray(SALT_SIZE)
        input.read(backupSalt)
        
        // Read encrypted master key
        val encryptedMasterKey = input.readBytes()
        
        // Derive backup decryption key
        val backupKey = deriveKeyFromPassphrase(backupPassphrase, backupSalt)
        
        // Decrypt master key
        val masterKeyBytes = decrypt(encryptedMasterKey, backupKey)
        
        SecretKeySpec(masterKeyBytes, KEY_ALGORITHM)
    }
    
    /**
     * Securely hash data for searchable encryption index
     * Uses HMAC-SHA256 with key as secret
     * 
     * @param data Data to hash
     * @param key Search index key
     * @return Hash suitable for searchable index
     */
    suspend fun searchableHash(
        data: String,
        key: SecretKey
    ): ByteArray = withContext(Dispatchers.Default) {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(key)
        mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }
    
    /**
     * Create searchable encryption index for a document
     * Uses deterministic encryption for exact match searches
     * 
     * @param content Document content
     * @param key Master key
     * @return Map of searchable terms to encrypted indices
     */
    suspend fun createSearchIndex(
        content: String,
        key: SecretKey
    ): Map<String, ByteArray> = withContext(Dispatchers.Default) {
        // Extract searchable terms (basic tokenization)
        val terms = content.lowercase()
            .split(Regex("\\W+"))
            .filter { it.length >= 3 } // Minimum term length
            .distinct()
        
        // Create deterministic hash for each term
        terms.associateWith { term ->
            searchableHash(term, key)
        }
    }
    
    /**
     * Verify data integrity without decryption
     * Uses HMAC for authenticated verification
     * 
     * @param encrypted Encrypted data
     * @param key Encryption key
     * @return True if data is authentic and unmodified
     */
    suspend fun verifyIntegrity(
        encrypted: ByteArray,
        key: SecretKey
    ): Boolean = withContext(Dispatchers.Default) {
        try {
            // GCM provides authenticated encryption
            // Attempting to decrypt will verify integrity
            decrypt(encrypted, key)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // Private helper methods
    
    private fun writeEncryptedFileHeader(
        output: OutputStream,
        salt: ByteArray,
        iv: ByteArray
    ) {
        output.write(ENCRYPTED_FILE_MAGIC.toByteArray(Charsets.UTF_8))
        output.write(METADATA_VERSION)
        output.write(salt)
        output.write(iv)
    }
    
    private fun readEncryptedFileHeader(
        input: InputStream
    ): Pair<ByteArray, ByteArray> {
        // Verify magic bytes
        val magic = ByteArray(ENCRYPTED_FILE_MAGIC.length)
        input.read(magic)
        require(magic.contentEquals(ENCRYPTED_FILE_MAGIC.toByteArray(Charsets.UTF_8))) {
            "Invalid encrypted file format"
        }
        
        // Read version
        val version = input.read()
        require(version == METADATA_VERSION) {
            "Unsupported encrypted file version: $version"
        }
        
        // Read salt
        val salt = ByteArray(SALT_SIZE)
        input.read(salt)
        
        // Read IV
        val iv = ByteArray(IV_SIZE)
        input.read(iv)
        
        return Pair(salt, iv)
    }
    
    /**
     * Securely wipe ByteArray from memory
     */
    fun secureWipe(data: ByteArray) {
        secureRandom.nextBytes(data) // Overwrite with random data
        data.fill(0) // Then zero out
    }
    
    /**
     * Securely wipe CharArray from memory
     */
    fun secureWipe(data: CharArray) {
        data.fill('\u0000')
    }
}

/**
 * Zero-Knowledge Mode Configuration
 */
data class ZeroKnowledgeConfig(
    val enabled: Boolean = false,
    val salt: ByteArray,
    val keyBackupExported: Boolean = false,
    val localOnlyMode: Boolean = false,
    val searchIndexEnabled: Boolean = true,
    val privacyAuditEnabled: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ZeroKnowledgeConfig

        if (enabled != other.enabled) return false
        if (!salt.contentEquals(other.salt)) return false
        if (keyBackupExported != other.keyBackupExported) return false
        if (localOnlyMode != other.localOnlyMode) return false
        if (searchIndexEnabled != other.searchIndexEnabled) return false
        if (privacyAuditEnabled != other.privacyAuditEnabled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + keyBackupExported.hashCode()
        result = 31 * result + localOnlyMode.hashCode()
        result = 31 * result + searchIndexEnabled.hashCode()
        result = 31 * result + privacyAuditEnabled.hashCode()
        return result
    }
}

/**
 * Privacy Audit Result
 */
data class PrivacyAuditResult(
    val timestamp: Long,
    val keysStoredLocally: Boolean,
    val noCloudKeyAccess: Boolean,
    val noTelemetry: Boolean,
    val encryptionActive: Boolean,
    val localOnlyMode: Boolean,
    val warnings: List<String> = emptyList()
) {
    val passed: Boolean
        get() = keysStoredLocally && noCloudKeyAccess && noTelemetry && 
                encryptionActive && warnings.isEmpty()
}
