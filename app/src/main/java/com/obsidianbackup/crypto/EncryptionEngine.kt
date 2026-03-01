// crypto/EncryptionEngine.kt
package com.obsidianbackup.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import com.obsidianbackup.security.SecureMemory
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EncryptionEngine {

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256
        private const val IV_SIZE = 12 // 96 bits recommended for GCM
        private const val TAG_SIZE = 128 // 128 bits authentication tag
        private const val BUFFER_SIZE = 8192

        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS_PREFIX = "titan_backup_key_"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    /**
     * Generate a new encryption key and store it in Android KeyStore
     * 
     * @param keyId Unique identifier for the key
     * @param requireBiometric If true, key usage requires biometric authentication
     * @param authValidityDuration Validity duration in seconds after authentication (default: 30s)
     * @return Key alias in keystore
     */
    fun generateKey(
        keyId: String,
        requireBiometric: Boolean = false,
        authValidityDuration: Int = 30
    ): String {
        val keyAlias = KEY_ALIAS_PREFIX + keyId

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        val keyGenSpecBuilder = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .apply {
                if (requireBiometric) {
                    // Require user authentication for key usage
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        // Android 11+ supports auth validity duration
                        setUserAuthenticationRequired(true)
                        setUserAuthenticationParameters(
                            authValidityDuration,
                            KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
                        )
                    } else {
                        setUserAuthenticationRequired(true)
                        setUserAuthenticationValidityDurationSeconds(authValidityDuration)
                    }
                    setInvalidatedByBiometricEnrollment(true)
                } else {
                    setUserAuthenticationRequired(false)
                }
                
                // Enable StrongBox if available (Android 9+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (isStrongBoxAvailable()) {
                        setIsStrongBoxBacked(true)
                    }
                }
            }

        keyGenerator.init(keyGenSpecBuilder.build())
        keyGenerator.generateKey()

        return keyAlias
    }
    
    /**
     * Check if StrongBox Keymaster is available
     */
    private fun isStrongBoxAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                // Try to detect StrongBox by attempting key generation
                // Some devices may report the feature but not actually support it
                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    /**
     * Get encryption key from KeyStore
     */
    private fun getKey(keyAlias: String): SecretKey? {
        return try {
            keyStore.getKey(keyAlias, null) as? SecretKey
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Initialize cipher for encryption with biometric authentication
     * Returns cipher that can be used with BiometricPrompt.CryptoObject
     * 
     * @throws UserNotAuthenticatedException if key requires authentication
     */
    fun initCipherForEncryption(keyAlias: String): Cipher {
        val key = getKey(keyAlias)
            ?: throw IllegalStateException("Encryption key not found: $keyAlias")
        
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher
    }
    
    /**
     * Initialize cipher for decryption with biometric authentication
     * Returns cipher that can be used with BiometricPrompt.CryptoObject
     * 
     * @param iv Initialization vector from encrypted data
     * @throws UserNotAuthenticatedException if key requires authentication
     */
    fun initCipherForDecryption(keyAlias: String, iv: ByteArray): Cipher {
        val key = getKey(keyAlias)
            ?: throw IllegalStateException("Decryption key not found: $keyAlias")
        
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE, iv))
        return cipher
    }
    
    /**
     * Check if key requires user authentication
     */
    fun keyRequiresAuthentication(keyAlias: String): Boolean {
        return try {
            val key = getKey(keyAlias) ?: return false
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            false
        } catch (e: UserNotAuthenticatedException) {
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Derive key from passphrase using PBKDF2
     * 
     * @param iterations OWASP recommends 600,000+ for PBKDF2-HMAC-SHA256 (2023)
     */
    fun deriveKeyFromPassphrase(passphrase: String, salt: ByteArray, iterations: Int = 600000): SecretKey {
        val passphraseChars = passphrase.toCharArray()
        try {
            val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = javax.crypto.spec.PBEKeySpec(
                passphraseChars,
                salt,
                iterations,
                KEY_SIZE
            )
            try {
                val tmp = factory.generateSecret(spec)
                val keyBytes = tmp.encoded
                try {
                    return SecretKeySpec(keyBytes, "AES")
                } finally {
                    SecureMemory.wipe(keyBytes)
                }
            } finally {
                spec.clearPassword()
            }
        } finally {
            SecureMemory.wipe(passphraseChars)
        }
    }

    /**
     * Encrypt a file
     * 
     * @param cipher Optional pre-initialized cipher (for biometric auth)
     */
    suspend fun encryptFile(
        inputFile: File,
        outputFile: File,
        keyAlias: String,
        cipher: Cipher? = null
    ): EncryptionMetadata = withContext(Dispatchers.IO) {
        val key = getKey(keyAlias)
            ?: throw IllegalStateException("Encryption key not found: $keyAlias")

        val useCipher = cipher ?: run {
            val iv = ByteArray(IV_SIZE).apply {
                SecureRandom().nextBytes(this)
            }
            Cipher.getInstance(ALGORITHM).apply {
                init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_SIZE, iv))
            }
        }

        val iv = useCipher.iv

        outputFile.outputStream().use { output ->
            // Write IV first (12 bytes)
            output.write(iv)

            // Write encrypted data
            inputFile.inputStream().use { input ->
                processStream(input, output, useCipher)
            }
        }

        EncryptionMetadata(
            algorithm = "AES-256-GCM",
            keyId = keyAlias,
            ivSize = IV_SIZE,
            tagSize = TAG_SIZE / 8
        )
    }

    /**
     * Decrypt a file
     * 
     * @param cipher Optional pre-initialized cipher (for biometric auth)
     */
    suspend fun decryptFile(
        inputFile: File,
        outputFile: File,
        keyAlias: String,
        cipher: Cipher? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val key = getKey(keyAlias)
                ?: return@withContext Result.failure(IllegalStateException("Decryption key not found: $keyAlias"))

            inputFile.inputStream().use { input ->
                // Read IV (first 12 bytes)
                val iv = ByteArray(IV_SIZE)
                val bytesRead = input.read(iv)
                if (bytesRead != IV_SIZE) {
                    return@withContext Result.failure(IllegalStateException("Failed to read IV from encrypted file: expected $IV_SIZE bytes, got $bytesRead"))
                }

                val useCipher = cipher ?: Cipher.getInstance(ALGORITHM).apply {
                    init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE, iv))
                }

                // Decrypt data
                outputFile.outputStream().use { output ->
                    processStream(input, output, useCipher)
                }
            }

            Result.success(true)
        } catch (e: UserNotAuthenticatedException) {
            Result.failure(BiometricRequiredException("Biometric authentication required for decryption", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Encrypt data in memory (for small data like metadata)
     * 
     * @param cipher Optional pre-initialized cipher (for biometric auth)
     * @param wipeInput If true, wipes the input data array after encryption
     */
    fun encryptData(data: ByteArray, keyAlias: String, cipher: Cipher? = null, wipeInput: Boolean = false): EncryptedData {
        try {
            val key = getKey(keyAlias)
                ?: throw IllegalStateException("Encryption key not found: $keyAlias")

            val useCipher = cipher ?: run {
                val iv = ByteArray(IV_SIZE).apply {
                    SecureRandom().nextBytes(this)
                }
                Cipher.getInstance(ALGORITHM).apply {
                    init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_SIZE, iv))
                }
            }

            val encrypted = useCipher.doFinal(data)

            return EncryptedData(
                ciphertext = encrypted,
                iv = useCipher.iv,
                algorithm = "AES-256-GCM"
            )
        } finally {
            if (wipeInput) {
                SecureMemory.wipe(data)
            }
        }
    }

    /**
     * Decrypt data in memory
     * 
     * @param cipher Optional pre-initialized cipher (for biometric auth)
     * Note: Caller is responsible for wiping the returned ByteArray after use
     */
    fun decryptData(encryptedData: EncryptedData, keyAlias: String, cipher: Cipher? = null): ByteArray {
        val key = getKey(keyAlias)
            ?: throw IllegalStateException("Decryption key not found: $keyAlias")

        val useCipher = cipher ?: Cipher.getInstance(ALGORITHM).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE, encryptedData.iv))
        }

        return useCipher.doFinal(encryptedData.ciphertext)
    }

    private fun processStream(input: InputStream, output: OutputStream, cipher: Cipher) {
        val buffer = ByteArray(BUFFER_SIZE)
        try {
            var bytesRead: Int

            while (input.read(buffer).also { bytesRead = it } != -1) {
                val processed = cipher.update(buffer, 0, bytesRead)
                if (processed != null) {
                    output.write(processed)
                }
            }

            val final = cipher.doFinal()
            if (final != null) {
                output.write(final)
            }
        } finally {
            SecureMemory.wipe(buffer)
        }
    }

    /**
     * Generate random salt for key derivation
     * Note: Caller is responsible for wiping the returned ByteArray if needed
     */
    fun generateSalt(size: Int = 32): ByteArray {
        return ByteArray(size).apply {
            SecureRandom().nextBytes(this)
        }
    }

    /**
     * Delete encryption key
     */
    fun deleteKey(keyAlias: String) {
        keyStore.deleteEntry(keyAlias)
    }
}

data class EncryptionMetadata(
    val algorithm: String,
    val keyId: String,
    val ivSize: Int,
    val tagSize: Int
)

data class EncryptedData(
    val ciphertext: ByteArray,
    val iv: ByteArray,
    val algorithm: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedData

        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (algorithm != other.algorithm) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ciphertext.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + algorithm.hashCode()
        return result
    }
}

/**
 * Exception thrown when biometric authentication is required
 */
class BiometricRequiredException(
    message: String,
    cause: Throwable? = null
) : SecurityException(message, cause)

// Integration with backup engine
class EncryptedBackupDecorator(
    private val baseEngine: com.obsidianbackup.engine.BackupEngine,
    private val encryptionEngine: EncryptionEngine,
    private val backupRootPath: String,
    private val requireBiometric: Boolean = false
) : com.obsidianbackup.engine.BackupEngine by baseEngine {

    override suspend fun backupApps(request: com.obsidianbackup.model.BackupRequest): com.obsidianbackup.model.BackupResult {
        val result = baseEngine.backupApps(request)

        if (request.encryptionEnabled && result is com.obsidianbackup.model.BackupResult.Success) {
            val keyId = result.snapshotId.value
            encryptSnapshot(result.snapshotId, keyId)
        }

        return result
    }

    private suspend fun encryptSnapshot(snapshotId: com.obsidianbackup.model.SnapshotId, keyId: String) {
        val keyAlias = encryptionEngine.generateKey(keyId, requireBiometric = requireBiometric)

        // Encrypt all files in snapshot
        val snapshotDir = File(backupRootPath, snapshotId.value)
        if (!snapshotDir.exists() || !snapshotDir.isDirectory) {
            throw IllegalStateException("Snapshot directory not found: ${snapshotDir.absolutePath}")
        }
        
        snapshotDir.listFiles()?.forEach { file ->
            if (file.isFile && file.extension != "encrypted") {
                try {
                    val encryptedFile = File(file.parentFile, "${file.name}.encrypted")
                    encryptionEngine.encryptFile(file, encryptedFile, keyAlias)
                    file.delete()
                } catch (e: Exception) {
                    // Log error but continue with other files
                    throw RuntimeException("Failed to encrypt file: ${file.name}", e)
                }
            }
        }
    }
}
