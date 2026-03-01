package com.obsidianbackup.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.dataStore
    
    // Setting keys
    object Keys {
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val CLOUD_SYNC_ENABLED = booleanPreferencesKey("cloud_sync_enabled")
        val COMPRESSION_ENABLED = booleanPreferencesKey("compression_enabled")
        val ENCRYPTION_ENABLED = booleanPreferencesKey("encryption_enabled")
        val VERIFY_AFTER_BACKUP = booleanPreferencesKey("verify_after_backup")
        val DEBUG_MODE = booleanPreferencesKey("debug_mode")
        val COMPRESSION_LEVEL = intPreferencesKey("compression_level")
        val BACKUP_RETENTION_DAYS = intPreferencesKey("backup_retention_days")
        val BACKUP_KEEP_COUNT = intPreferencesKey("backup_keep_count")           // max snapshots to keep (0 = unlimited)
        val STORAGE_LIMIT_MB = intPreferencesKey("storage_limit_mb")             // max total storage in MB (0 = unlimited)
        val RETENTION_MODE = stringPreferencesKey("retention_mode")              // "DAYS" | "COUNT" | "BOTH"
        val PERMISSION_MODE = stringPreferencesKey("permission_mode")
        
        // Cloud sync settings
        val SYNC_ON_BACKUP = booleanPreferencesKey("sync_on_backup")
        val SYNC_ON_WIFI_ONLY = booleanPreferencesKey("sync_on_wifi_only")
        val SYNC_ON_CHARGING = booleanPreferencesKey("sync_on_charging")
        val MAX_CONCURRENT_SYNCS = intPreferencesKey("max_concurrent_syncs")
        val SYNC_RETRY_MAX_ATTEMPTS = intPreferencesKey("sync_retry_max_attempts")
        val SYNC_RETRY_INITIAL_DELAY_MS = intPreferencesKey("sync_retry_initial_delay_ms")
        val SYNC_RETRY_BACKOFF_MULTIPLIER = intPreferencesKey("sync_retry_backoff_multiplier")
        
        // Backup automation settings
        val BACKUP_APP_IDS = stringPreferencesKey("backup_app_ids")
        val BACKUP_COMPONENTS = stringPreferencesKey("backup_components")
        val BACKUP_INCREMENTAL = booleanPreferencesKey("backup_incremental")
        
        // Performance settings
        val PARALLEL_OPERATIONS_ENABLED = booleanPreferencesKey("parallel_operations_enabled")
    }
    
    // Auto backup enabled
    val autoBackupEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[Keys.AUTO_BACKUP_ENABLED] ?: false }
    
    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.AUTO_BACKUP_ENABLED] = enabled
        }
    }
    
    // Cloud sync enabled
    val cloudSyncEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[Keys.CLOUD_SYNC_ENABLED] ?: false }
    
    suspend fun setCloudSyncEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.CLOUD_SYNC_ENABLED] = enabled
        }
    }
    
    // Compression enabled
    val compressionEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[Keys.COMPRESSION_ENABLED] ?: true }
    
    suspend fun setCompressionEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.COMPRESSION_ENABLED] = enabled
        }
    }
    
    // Encryption enabled
    val encryptionEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[Keys.ENCRYPTION_ENABLED] ?: false }
    
    suspend fun setEncryptionEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.ENCRYPTION_ENABLED] = enabled
        }
    }
    
    // Verify after backup
    val verifyAfterBackup: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[Keys.VERIFY_AFTER_BACKUP] ?: true }
    
    suspend fun setVerifyAfterBackup(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.VERIFY_AFTER_BACKUP] = enabled
        }
    }
    
    // Debug mode
    val debugMode: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[Keys.DEBUG_MODE] ?: false }
    
    suspend fun setDebugMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.DEBUG_MODE] = enabled
        }
    }
    
    // Compression level
    val compressionLevel: Flow<Int> = dataStore.data
        .map { preferences -> preferences[Keys.COMPRESSION_LEVEL] ?: 6 }
    
    suspend fun setCompressionLevel(level: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.COMPRESSION_LEVEL] = level
        }
    }
    
    // Backup retention days
    val backupRetentionDays: Flow<Int> = dataStore.data
        .map { preferences -> preferences[Keys.BACKUP_RETENTION_DAYS] ?: 30 }
    
    suspend fun setBackupRetentionDays(days: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.BACKUP_RETENTION_DAYS] = days
        }
    }

    // Max backups to keep (0 = unlimited)
    val backupKeepCount: Flow<Int> = dataStore.data
        .map { preferences -> preferences[Keys.BACKUP_KEEP_COUNT] ?: 10 }

    suspend fun setBackupKeepCount(count: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.BACKUP_KEEP_COUNT] = count
        }
    }

    // Storage limit in MB (0 = unlimited)
    val storageLimitMb: Flow<Int> = dataStore.data
        .map { preferences -> preferences[Keys.STORAGE_LIMIT_MB] ?: 0 }

    suspend fun setStorageLimitMb(mb: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.STORAGE_LIMIT_MB] = mb
        }
    }

    // Retention mode
    val retentionMode: Flow<String> = dataStore.data
        .map { preferences -> preferences[Keys.RETENTION_MODE] ?: "COUNT" }

    suspend fun setRetentionMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[Keys.RETENTION_MODE] = mode
        }
    }
    
    // Permission mode
    val permissionMode: Flow<String> = dataStore.data
        .map { preferences -> preferences[Keys.PERMISSION_MODE] ?: "AUTO" }
    
    suspend fun setPermissionMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[Keys.PERMISSION_MODE] = mode
        }
    }
    
    // Cloud Sync Settings
    val syncOnBackup: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[Keys.SYNC_ON_BACKUP] ?: true }
    
    suspend fun setSyncOnBackup(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.SYNC_ON_BACKUP] = enabled
        }
    }
    
    val syncOnWifiOnly: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[Keys.SYNC_ON_WIFI_ONLY] ?: true }
    
    suspend fun setSyncOnWifiOnly(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.SYNC_ON_WIFI_ONLY] = enabled
        }
    }
    
    val syncOnCharging: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[Keys.SYNC_ON_CHARGING] ?: false }
    
    suspend fun setSyncOnCharging(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.SYNC_ON_CHARGING] = enabled
        }
    }
    
    val maxConcurrentSyncs: Flow<Int> = dataStore.data
        .map { preferences -> preferences[Keys.MAX_CONCURRENT_SYNCS] ?: 1 }
    
    suspend fun setMaxConcurrentSyncs(count: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.MAX_CONCURRENT_SYNCS] = count
        }
    }
    
    val syncRetryMaxAttempts: Flow<Int> = dataStore.data
        .map { preferences -> preferences[Keys.SYNC_RETRY_MAX_ATTEMPTS] ?: 3 }
    
    suspend fun setSyncRetryMaxAttempts(attempts: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.SYNC_RETRY_MAX_ATTEMPTS] = attempts
        }
    }
    
    val syncRetryInitialDelayMs: Flow<Int> = dataStore.data
        .map { preferences -> preferences[Keys.SYNC_RETRY_INITIAL_DELAY_MS] ?: 1000 }
    
    suspend fun setSyncRetryInitialDelayMs(delayMs: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.SYNC_RETRY_INITIAL_DELAY_MS] = delayMs
        }
    }
    
    val syncRetryBackoffMultiplier: Flow<Int> = dataStore.data
        .map { preferences -> preferences[Keys.SYNC_RETRY_BACKOFF_MULTIPLIER] ?: 2 }
    
    suspend fun setSyncRetryBackoffMultiplier(multiplier: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.SYNC_RETRY_BACKOFF_MULTIPLIER] = multiplier
        }
    }
    
    // Backup Automation Settings
    val backupAppIds: Flow<List<String>> = dataStore.data
        .map { preferences ->
            preferences[Keys.BACKUP_APP_IDS]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        }
    
    suspend fun setBackupAppIds(appIds: List<String>) {
        dataStore.edit { preferences ->
            preferences[Keys.BACKUP_APP_IDS] = appIds.joinToString(",")
        }
    }
    
    val backupComponents: Flow<Set<String>> = dataStore.data
        .map { preferences ->
            preferences[Keys.BACKUP_COMPONENTS]?.split(",")?.filter { it.isNotBlank() }?.toSet() 
                ?: setOf("APK", "DATA")
        }
    
    suspend fun setBackupComponents(components: Set<String>) {
        dataStore.edit { preferences ->
            preferences[Keys.BACKUP_COMPONENTS] = components.joinToString(",")
        }
    }
    
    val backupIncremental: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[Keys.BACKUP_INCREMENTAL] ?: true }
    
    suspend fun setBackupIncremental(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.BACKUP_INCREMENTAL] = enabled
        }
    }
    
    // Parallel operations enabled
    val parallelOperationsEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[Keys.PARALLEL_OPERATIONS_ENABLED] ?: false }
    
    suspend fun setParallelOperationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.PARALLEL_OPERATIONS_ENABLED] = enabled
        }
    }
}
