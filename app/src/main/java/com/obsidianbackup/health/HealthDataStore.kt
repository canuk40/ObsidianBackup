// health/HealthDataStore.kt
package com.obsidianbackup.health

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.obsidianbackup.logging.ObsidianLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val Context.healthDataStore: DataStore<Preferences> by preferencesDataStore(name = "health_settings")

@Singleton
class HealthDataStore @Inject constructor(
    private val context: Context,
    private val logger: ObsidianLogger
) {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    private val backupDir: File by lazy {
        File(context.getExternalFilesDir("backups"), "health_data").apply {
            mkdirs()
        }
    }
    
    companion object {
        private const val TAG = "HealthDataStore"
        private val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        private val PRIVACY_SETTINGS = stringPreferencesKey("privacy_settings")
        private val RECORD_COUNTS = stringPreferencesKey("record_counts")
    }
    
    /**
     * Get last backup timestamp
     */
    suspend fun getLastBackupTimestamp(): Instant? {
        return try {
            val prefs = context.healthDataStore.data.first()
            val timestamp = prefs[LAST_BACKUP_TIMESTAMP]
            timestamp?.let { Instant.ofEpochMilli(it) }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to get last backup timestamp", e)
            null
        }
    }
    
    /**
     * Save last backup timestamp
     */
    suspend fun saveLastBackupTimestamp(timestamp: Instant) {
        try {
            context.healthDataStore.edit { prefs ->
                prefs[LAST_BACKUP_TIMESTAMP] = timestamp.toEpochMilli()
            }
            logger.i(TAG, "Saved last backup timestamp: $timestamp")
        } catch (e: Exception) {
            logger.e(TAG, "Failed to save last backup timestamp", e)
        }
    }
    
    /**
     * Get privacy settings
     */
    suspend fun getPrivacySettings(): HealthPrivacySettings {
        return try {
            val prefs = context.healthDataStore.data.first()
            val settingsJson = prefs[PRIVACY_SETTINGS]
            if (settingsJson != null) {
                json.decodeFromString<HealthPrivacySettings>(settingsJson)
            } else {
                HealthPrivacySettings()
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to get privacy settings", e)
            HealthPrivacySettings()
        }
    }
    
    /**
     * Save privacy settings
     */
    suspend fun savePrivacySettings(settings: HealthPrivacySettings) {
        try {
            context.healthDataStore.edit { prefs ->
                prefs[PRIVACY_SETTINGS] = json.encodeToString(settings)
            }
            logger.i(TAG, "Saved privacy settings")
        } catch (e: Exception) {
            logger.e(TAG, "Failed to save privacy settings", e)
        }
    }
    
    /**
     * Get total backup size
     */
    suspend fun getTotalBackupSize(): Long {
        return try {
            backupDir.walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()
        } catch (e: Exception) {
            logger.e(TAG, "Failed to calculate backup size", e)
            0L
        }
    }
    
    /**
     * Get record counts by type
     */
    suspend fun getRecordCounts(): Map<HealthDataType, Int> {
        return try {
            val prefs = context.healthDataStore.data.first()
            val countsJson = prefs[RECORD_COUNTS]
            if (countsJson != null) {
                json.decodeFromString<Map<String, Int>>(countsJson)
                    .mapKeys { HealthDataType.valueOf(it.key) }
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to get record counts", e)
            emptyMap()
        }
    }
    
    /**
     * Save record counts
     */
    suspend fun saveRecordCounts(counts: Map<HealthDataType, Int>) {
        try {
            context.healthDataStore.edit { prefs ->
                val countsMap = counts.mapKeys { it.key.name }
                prefs[RECORD_COUNTS] = json.encodeToString(countsMap)
            }
            logger.i(TAG, "Saved record counts")
        } catch (e: Exception) {
            logger.e(TAG, "Failed to save record counts", e)
        }
    }
    
    /**
     * Delete all health backups
     */
    suspend fun deleteAllBackups() {
        try {
            backupDir.deleteRecursively()
            backupDir.mkdirs()
            
            // Clear preferences
            context.healthDataStore.edit { prefs ->
                prefs.clear()
            }
            
            logger.i(TAG, "Deleted all health backups")
        } catch (e: Exception) {
            logger.e(TAG, "Failed to delete backups", e)
            throw e
        }
    }
}
