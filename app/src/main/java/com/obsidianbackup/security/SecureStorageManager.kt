// security/SecureStorageManager.kt
package com.obsidianbackup.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.obsidianbackup.logging.ObsidianLogger
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secure Storage Manager using Android Keystore
 * Implements OWASP MASVS-STORAGE requirements
 * 
 * Features:
 * - Hardware-backed encryption (StrongBox if available)
 * - Encrypted SharedPreferences for sensitive data
 * - AES-256-GCM encryption
 * - Biometric authentication integration
 * - Secure key generation and storage
 */
class SecureStorageManager(
    private val context: Context,
    private val logger: ObsidianLogger
) {
    companion object {
        private const val TAG = "SecureStorage"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "obsidian_backup_master_key"
        private const val ENCRYPTED_PREFS_NAME = "obsidian_secure_prefs"
        
        // Encryption constants
        private const val KEY_SIZE = 256
        private const val GCM_TAG_LENGTH = 128
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
    
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
            load(null)
        }
    }
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setUserAuthenticationRequired(false)
            .apply {
                // Use StrongBox if available (Pixel 3+ and some other devices)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (context.packageManager.hasSystemFeature(
                            android.content.pm.PackageManager.FEATURE_STRONGBOX_KEYSTORE
                        )) {
                        setRequestStrongBoxBacked(true)
                        logger.i(TAG, "Using StrongBox-backed master key")
                    }
                }
            }
            .build()
    }
    
    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * Store encrypted string
     */
    fun putString(key: String, value: String) {
        try {
            encryptedPrefs.edit().putString(key, value).apply()
            logger.d(TAG, "Stored encrypted string for key: $key")
        } catch (e: Exception) {
            logger.e(TAG, "Failed to store encrypted string", e)
            throw SecureStorageException("Failed to store encrypted data", e)
        }
    }
    
    /**
     * Retrieve encrypted string
     */
    fun getString(key: String, defaultValue: String? = null): String? {
        return try {
            encryptedPrefs.getString(key, defaultValue)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to retrieve encrypted string", e)
            defaultValue
        }
    }
    
    /**
     * Store encrypted integer
     */
    fun putInt(key: String, value: Int) {
        try {
            encryptedPrefs.edit().putInt(key, value).apply()
        } catch (e: Exception) {
            logger.e(TAG, "Failed to store encrypted int", e)
            throw SecureStorageException("Failed to store encrypted data", e)
        }
    }
    
    /**
     * Retrieve encrypted integer
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return try {
            encryptedPrefs.getInt(key, defaultValue)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to retrieve encrypted int", e)
            defaultValue
        }
    }
    
    /**
     * Store encrypted boolean
     */
    fun putBoolean(key: String, value: Boolean) {
        try {
            encryptedPrefs.edit().putBoolean(key, value).apply()
        } catch (e: Exception) {
            logger.e(TAG, "Failed to store encrypted boolean", e)
            throw SecureStorageException("Failed to store encrypted data", e)
        }
    }
    
    /**
     * Retrieve encrypted boolean
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return try {
            encryptedPrefs.getBoolean(key, defaultValue)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to retrieve encrypted boolean", e)
            defaultValue
        }
    }
    
    /**
     * Remove encrypted entry
     */
    fun remove(key: String) {
        try {
            encryptedPrefs.edit().remove(key).apply()
            logger.d(TAG, "Removed encrypted entry: $key")
        } catch (e: Exception) {
            logger.e(TAG, "Failed to remove encrypted entry", e)
        }
    }
    
    /**
     * Clear all encrypted preferences
     */
    fun clear() {
        try {
            encryptedPrefs.edit().clear().apply()
            logger.i(TAG, "Cleared all encrypted preferences")
        } catch (e: Exception) {
            logger.e(TAG, "Failed to clear encrypted preferences", e)
        }
    }
    
    /**
     * Generate or retrieve a secret key from Android Keystore
     */
    fun getOrCreateSecretKey(
        keyAlias: String,
        requireBiometricAuth: Boolean = false,
        authValidityDuration: Int = 30
    ): SecretKey {
        // Check if key exists
        if (keyStore.containsAlias(keyAlias)) {
            return keyStore.getKey(keyAlias, null) as SecretKey
        }
        
        // Generate new key
        return generateSecretKey(keyAlias, requireBiometricAuth, authValidityDuration)
    }
    
    /**
     * Generate a new secret key in Android Keystore
     */
    private fun generateSecretKey(
        keyAlias: String,
        requireBiometricAuth: Boolean,
        authValidityDuration: Int
    ): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        
        val builder = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .setUserAuthenticationRequired(requireBiometricAuth)
            .setRandomizedEncryptionRequired(true)
        
        // Set authentication validity duration
        if (requireBiometricAuth) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(
                    authValidityDuration,
                    KeyProperties.AUTH_BIOMETRIC_STRONG
                )
            } else {
                @Suppress("DEPRECATION")
                builder.setUserAuthenticationValidityDurationSeconds(authValidityDuration)
            }
        }
        
        // Use StrongBox if available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (context.packageManager.hasSystemFeature(
                    android.content.pm.PackageManager.FEATURE_STRONGBOX_KEYSTORE
                )) {
                builder.setIsStrongBoxBacked(true)
                logger.i(TAG, "Generating StrongBox-backed key: $keyAlias")
            }
        }
        
        keyGenerator.init(builder.build())
        val key = keyGenerator.generateKey()
        
        logger.i(TAG, "Generated secret key: $keyAlias, biometric=$requireBiometricAuth")
        return key
    }
    
    /**
     * Encrypt data using a specific key
     */
    fun encryptData(data: ByteArray, keyAlias: String): EncryptedData {
        val key = getOrCreateSecretKey(keyAlias)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        
        return EncryptedData(
            data = encrypted,
            iv = iv,
            keyAlias = keyAlias
        )
    }
    
    /**
     * Decrypt data using a specific key
     */
    fun decryptData(encryptedData: EncryptedData, cipher: Cipher? = null): ByteArray {
        val key = getOrCreateSecretKey(encryptedData.keyAlias)
        
        val decryptCipher = cipher ?: Cipher.getInstance(TRANSFORMATION).apply {
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, encryptedData.iv)
            init(Cipher.DECRYPT_MODE, key, spec)
        }
        
        return decryptCipher.doFinal(encryptedData.data)
    }
    
    /**
     * Get cipher for biometric authentication
     */
    fun getCipherForDecryption(keyAlias: String, iv: ByteArray): Cipher {
        val key = getOrCreateSecretKey(keyAlias, requireBiometricAuth = true)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher
    }
    
    /**
     * Get cipher for biometric authentication (encryption)
     */
    fun getCipherForEncryption(keyAlias: String): Cipher {
        val key = getOrCreateSecretKey(keyAlias, requireBiometricAuth = true)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher
    }
    
    /**
     * Delete a key from Keystore
     */
    fun deleteKey(keyAlias: String) {
        try {
            keyStore.deleteEntry(keyAlias)
            logger.i(TAG, "Deleted key: $keyAlias")
        } catch (e: Exception) {
            logger.e(TAG, "Failed to delete key: $keyAlias", e)
        }
    }
    
    /**
     * Check if a key exists
     */
    fun hasKey(keyAlias: String): Boolean {
        return keyStore.containsAlias(keyAlias)
    }
    
    /**
     * Wipe all secure data (factory reset)
     */
    fun wipeAllData() {
        try {
            // Clear encrypted preferences
            clear()
            
            // Delete all keys in keystore
            val aliases = keyStore.aliases()
            while (aliases.hasMoreElements()) {
                val alias = aliases.nextElement()
                if (alias.startsWith("obsidian_")) {
                    keyStore.deleteEntry(alias)
                }
            }
            
            logger.i(TAG, "Wiped all secure data")
        } catch (e: Exception) {
            logger.e(TAG, "Failed to wipe secure data", e)
            throw SecureStorageException("Failed to wipe secure data", e)
        }
    }
    
    /**
     * Encrypted data container
     */
    data class EncryptedData(
        val data: ByteArray,
        val iv: ByteArray,
        val keyAlias: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as EncryptedData
            
            if (!data.contentEquals(other.data)) return false
            if (!iv.contentEquals(other.iv)) return false
            if (keyAlias != other.keyAlias) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + iv.contentHashCode()
            result = 31 * result + keyAlias.hashCode()
            return result
        }
    }
}

/**
 * Exception for secure storage operations
 */
class SecureStorageException(message: String, cause: Throwable? = null) : 
    Exception(message, cause)
