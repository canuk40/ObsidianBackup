// crypto/ZeroKnowledgeManager.kt
package com.obsidianbackup.crypto

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import javax.crypto.SecretKey

/**
 * Manager for Zero-Knowledge Encryption Mode
 * Handles key lifecycle, configuration, and privacy audits
 */
class ZeroKnowledgeManager(private val context: Context) {
    
    private val zkEncryption = ZeroKnowledgeEncryption()
    private val Context.zkDataStore: DataStore<Preferences> by preferencesDataStore(name = "zero_knowledge")
    
    companion object {
        private const val TAG = "ZKManager"
        
        // DataStore keys
        private val KEY_ENABLED = booleanPreferencesKey("zk_enabled")
        private val KEY_SALT = stringPreferencesKey("zk_salt")
        private val KEY_BACKUP_EXPORTED = booleanPreferencesKey("zk_backup_exported")
        private val KEY_LOCAL_ONLY = booleanPreferencesKey("zk_local_only")
        private val KEY_SEARCH_INDEX = booleanPreferencesKey("zk_search_index")
        private val KEY_PRIVACY_AUDIT = booleanPreferencesKey("zk_privacy_audit")
        private val KEY_SETUP_COMPLETE = booleanPreferencesKey("zk_setup_complete")
        private val KEY_LAST_AUDIT = longPreferencesKey("zk_last_audit")
        
        // In-memory key cache (cleared on app restart)
        @Volatile
        private var cachedMasterKey: SecretKey? = null
        
        // Privacy audit thresholds
        private const val AUDIT_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
    }
    
    /**
     * Initialize zero-knowledge mode with user passphrase
     * This is the first-time setup
     * 
     * @param passphrase User's master passphrase
     * @return True if initialization successful
     */
    suspend fun initializeZeroKnowledge(passphrase: CharArray): Result<Unit> {
        return try {
            // Generate new salt
            val salt = zkEncryption.generateSalt()
            
            // Derive master key
            val masterKey = zkEncryption.deriveKeyFromPassphrase(passphrase, salt)
            
            // Cache the key in memory
            cachedMasterKey = masterKey
            
            // Store configuration
            context.zkDataStore.edit { prefs ->
                prefs[KEY_ENABLED] = true
                prefs[KEY_SALT] = android.util.Base64.encodeToString(
                    salt, 
                    android.util.Base64.NO_WRAP
                )
                prefs[KEY_BACKUP_EXPORTED] = false
                prefs[KEY_LOCAL_ONLY] = false
                prefs[KEY_SEARCH_INDEX] = true
                prefs[KEY_PRIVACY_AUDIT] = true
                prefs[KEY_SETUP_COMPLETE] = true
            }
            
            Log.i(TAG, "Zero-knowledge mode initialized successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize zero-knowledge mode", e)
            Result.failure(e)
        }
    }
    
    /**
     * Unlock zero-knowledge mode with passphrase
     * Must be called after app restart to decrypt data
     * 
     * @param passphrase User's master passphrase
     * @return True if unlock successful
     */
    suspend fun unlockWithPassphrase(passphrase: CharArray): Result<Unit> {
        return try {
            val prefs = context.zkDataStore.data.first()
            val saltBase64 = prefs[KEY_SALT] 
                ?: return Result.failure(IllegalStateException("Zero-knowledge not initialized"))
            
            val salt = android.util.Base64.decode(saltBase64, android.util.Base64.NO_WRAP)
            
            // Derive master key from passphrase
            val masterKey = zkEncryption.deriveKeyFromPassphrase(passphrase, salt)
            
            // Verify key by attempting to decrypt a test value
            // (In production, you might encrypt a known value during setup)
            
            // Cache the key in memory
            cachedMasterKey = masterKey
            
            Log.i(TAG, "Zero-knowledge mode unlocked")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unlock zero-knowledge mode", e)
            Result.failure(e)
        }
    }
    
    /**
     * Lock zero-knowledge mode (clear cached keys)
     */
    fun lock() {
        cachedMasterKey?.let { key ->
            // Securely wipe the key from memory
            val keyBytes = key.encoded
            zkEncryption.secureWipe(keyBytes)
        }
        cachedMasterKey = null
        Log.i(TAG, "Zero-knowledge mode locked")
    }
    
    /**
     * Check if zero-knowledge mode is enabled
     */
    suspend fun isEnabled(): Boolean {
        return context.zkDataStore.data.first()[KEY_ENABLED] ?: false
    }
    
    /**
     * Check if zero-knowledge mode is unlocked (key cached)
     */
    fun isUnlocked(): Boolean {
        return cachedMasterKey != null
    }
    
    /**
     * Get current master key (must be unlocked)
     */
    fun getMasterKey(): SecretKey? {
        return cachedMasterKey
    }
    
    /**
     * Get zero-knowledge configuration
     */
    suspend fun getConfig(): ZeroKnowledgeConfig? {
        val prefs = context.zkDataStore.data.first()
        val saltBase64 = prefs[KEY_SALT] ?: return null
        
        return ZeroKnowledgeConfig(
            enabled = prefs[KEY_ENABLED] ?: false,
            salt = android.util.Base64.decode(saltBase64, android.util.Base64.NO_WRAP),
            keyBackupExported = prefs[KEY_BACKUP_EXPORTED] ?: false,
            localOnlyMode = prefs[KEY_LOCAL_ONLY] ?: false,
            searchIndexEnabled = prefs[KEY_SEARCH_INDEX] ?: true,
            privacyAuditEnabled = prefs[KEY_PRIVACY_AUDIT] ?: true
        )
    }
    
    /**
     * Export key backup
     */
    suspend fun exportKeyBackup(backupPassphrase: CharArray): Result<String> {
        return try {
            val masterKey = cachedMasterKey 
                ?: return Result.failure(IllegalStateException("Zero-knowledge not unlocked"))
            
            val backup = zkEncryption.exportKeyBackup(masterKey, backupPassphrase)
            
            // Mark as exported
            context.zkDataStore.edit { prefs ->
                prefs[KEY_BACKUP_EXPORTED] = true
            }
            
            Log.i(TAG, "Key backup exported")
            Result.success(backup)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export key backup", e)
            Result.failure(e)
        }
    }
    
    /**
     * Import key backup
     */
    suspend fun importKeyBackup(
        backupData: String, 
        backupPassphrase: CharArray
    ): Result<Unit> {
        return try {
            val masterKey = zkEncryption.importKeyBackup(backupData, backupPassphrase)
            
            // Cache the imported key
            cachedMasterKey = masterKey
            
            // Generate new salt for storage
            val salt = zkEncryption.generateSalt()
            
            // Store configuration
            context.zkDataStore.edit { prefs ->
                prefs[KEY_ENABLED] = true
                prefs[KEY_SALT] = android.util.Base64.encodeToString(
                    salt,
                    android.util.Base64.NO_WRAP
                )
                prefs[KEY_BACKUP_EXPORTED] = true
                prefs[KEY_SETUP_COMPLETE] = true
            }
            
            Log.i(TAG, "Key backup imported")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import key backup", e)
            Result.failure(e)
        }
    }
    
    /**
     * Enable/disable local-only mode (never touch internet)
     */
    suspend fun setLocalOnlyMode(enabled: Boolean) {
        context.zkDataStore.edit { prefs ->
            prefs[KEY_LOCAL_ONLY] = enabled
        }
        Log.i(TAG, "Local-only mode: $enabled")
    }
    
    /**
     * Enable/disable searchable encryption index
     */
    suspend fun setSearchIndexEnabled(enabled: Boolean) {
        context.zkDataStore.edit { prefs ->
            prefs[KEY_SEARCH_INDEX] = enabled
        }
    }
    
    /**
     * Perform privacy audit
     * Verifies zero-knowledge properties
     */
    suspend fun performPrivacyAudit(): PrivacyAuditResult {
        // Use dedicated privacy auditor for comprehensive checks
        val auditor = PrivacyAuditor(context)
        val result = auditor.performAudit(this)
        
        // Store last audit timestamp
        context.zkDataStore.edit { prefs ->
            prefs[KEY_LAST_AUDIT] = result.timestamp
        }
        
        Log.i(TAG, "Privacy audit completed: ${if (result.passed) "PASSED" else "WARNINGS"}")
        return result
    }
    
    /**
     * Get last privacy audit result (if recent)
     */
    suspend fun getLastAuditResult(): PrivacyAuditResult? {
        val prefs = context.zkDataStore.data.first()
        val lastAudit = prefs[KEY_LAST_AUDIT] ?: return null
        
        val age = System.currentTimeMillis() - lastAudit
        if (age > AUDIT_INTERVAL_MS) {
            return null // Audit too old
        }
        
        return performPrivacyAudit()
    }
    
    /**
     * Encrypt file with zero-knowledge encryption
     */
    suspend fun encryptFile(
        inputFile: File,
        outputFile: File
    ): Result<Unit> {
        return try {
            val masterKey = cachedMasterKey
                ?: return Result.failure(IllegalStateException("Zero-knowledge not unlocked"))
            
            val config = getConfig()
                ?: return Result.failure(IllegalStateException("Zero-knowledge not configured"))
            
            zkEncryption.encryptFile(inputFile, outputFile, masterKey, config.salt)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt file", e)
            Result.failure(e)
        }
    }
    
    /**
     * Decrypt file with zero-knowledge encryption
     */
    suspend fun decryptFile(
        encryptedFile: File,
        outputFile: File
    ): Result<Unit> {
        return try {
            val masterKey = cachedMasterKey
                ?: return Result.failure(IllegalStateException("Zero-knowledge not unlocked"))
            
            zkEncryption.decryptFile(encryptedFile, outputFile, masterKey)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt file", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create searchable index for encrypted content
     */
    suspend fun createSearchIndex(content: String): Map<String, ByteArray>? {
        val masterKey = cachedMasterKey ?: return null
        val config = getConfig() ?: return null
        
        if (!config.searchIndexEnabled) return null
        
        return zkEncryption.createSearchIndex(content, masterKey)
    }
    
    /**
     * Disable zero-knowledge mode (WARNING: irreversible)
     */
    suspend fun disableZeroKnowledge(): Result<Unit> {
        return try {
            lock()
            
            context.zkDataStore.edit { prefs ->
                prefs.clear()
            }
            
            Log.w(TAG, "Zero-knowledge mode disabled")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disable zero-knowledge mode", e)
            Result.failure(e)
        }
    }
    
    /**
     * Observe zero-knowledge configuration changes
     */
    fun observeConfig(): Flow<ZeroKnowledgeConfig?> {
        return context.zkDataStore.data.map { prefs ->
            val saltBase64 = prefs[KEY_SALT] ?: return@map null
            
            ZeroKnowledgeConfig(
                enabled = prefs[KEY_ENABLED] ?: false,
                salt = android.util.Base64.decode(saltBase64, android.util.Base64.NO_WRAP),
                keyBackupExported = prefs[KEY_BACKUP_EXPORTED] ?: false,
                localOnlyMode = prefs[KEY_LOCAL_ONLY] ?: false,
                searchIndexEnabled = prefs[KEY_SEARCH_INDEX] ?: true,
                privacyAuditEnabled = prefs[KEY_PRIVACY_AUDIT] ?: true
            )
        }
    }
}
