// security/BiometricSettings.kt
package com.obsidianbackup.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

/**
 * BiometricSettings manages user preferences for biometric authentication
 * 
 * Settings are stored in DataStore for reactive updates and persistence.
 */
class BiometricSettings(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "biometric_settings")
        
        private val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val BIOMETRIC_FOR_BACKUP = booleanPreferencesKey("biometric_for_backup")
        private val BIOMETRIC_FOR_RESTORE = booleanPreferencesKey("biometric_for_restore")
        private val BIOMETRIC_FOR_SETTINGS = booleanPreferencesKey("biometric_for_settings")
        private val BIOMETRIC_FOR_DELETE = booleanPreferencesKey("biometric_for_delete")
        private val BIOMETRIC_FOR_EXPORT = booleanPreferencesKey("biometric_for_export")
        private val USE_PASSKEY = booleanPreferencesKey("use_passkey")
    }

    /**
     * Flow of biometric enabled state
     */
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRIC_ENABLED] ?: false
    }

    /**
     * Flow of biometric required for backup operations
     */
    val biometricForBackup: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRIC_FOR_BACKUP] ?: false
    }

    /**
     * Flow of biometric required for restore operations
     */
    val biometricForRestore: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRIC_FOR_RESTORE] ?: true // Default enabled for restore
    }

    /**
     * Flow of biometric required for settings changes
     */
    val biometricForSettings: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRIC_FOR_SETTINGS] ?: false
    }

    /**
     * Flow of biometric required for delete operations
     */
    val biometricForDelete: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRIC_FOR_DELETE] ?: true // Default enabled for delete
    }

    /**
     * Flow of biometric required for export operations
     */
    val biometricForExport: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRIC_FOR_EXPORT] ?: true // Default enabled for export
    }

    /**
     * Flow of passkey usage preference (Android 14+)
     */
    val usePasskey: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USE_PASSKEY] ?: false
    }

    /**
     * Enable/disable biometric authentication globally
     */
    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED] = enabled
        }
    }

    /**
     * Enable/disable biometric for backup operations
     */
    suspend fun setBiometricForBackup(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_FOR_BACKUP] = enabled
        }
    }

    /**
     * Enable/disable biometric for restore operations
     */
    suspend fun setBiometricForRestore(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_FOR_RESTORE] = enabled
        }
    }

    /**
     * Enable/disable biometric for settings changes
     */
    suspend fun setBiometricForSettings(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_FOR_SETTINGS] = enabled
        }
    }

    /**
     * Enable/disable biometric for delete operations
     */
    suspend fun setBiometricForDelete(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_FOR_DELETE] = enabled
        }
    }

    /**
     * Enable/disable biometric for export operations
     */
    suspend fun setBiometricForExport(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_FOR_EXPORT] = enabled
        }
    }

    /**
     * Enable/disable passkey usage (Android 14+)
     */
    suspend fun setUsePasskey(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_PASSKEY] = enabled
        }
    }

    /**
     * Get all settings as snapshot
     */
    suspend fun getSnapshot(): BiometricSettingsSnapshot {
        val preferences = context.dataStore.data.first()
        return BiometricSettingsSnapshot(
            biometricEnabled = preferences[BIOMETRIC_ENABLED] ?: false,
            biometricForBackup = preferences[BIOMETRIC_FOR_BACKUP] ?: false,
            biometricForRestore = preferences[BIOMETRIC_FOR_RESTORE] ?: true,
            biometricForSettings = preferences[BIOMETRIC_FOR_SETTINGS] ?: false,
            biometricForDelete = preferences[BIOMETRIC_FOR_DELETE] ?: true,
            biometricForExport = preferences[BIOMETRIC_FOR_EXPORT] ?: true,
            usePasskey = preferences[USE_PASSKEY] ?: false
        )
    }

    /**
     * Reset all settings to defaults
     */
    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

/**
 * Snapshot of biometric settings
 */
data class BiometricSettingsSnapshot(
    val biometricEnabled: Boolean,
    val biometricForBackup: Boolean,
    val biometricForRestore: Boolean,
    val biometricForSettings: Boolean,
    val biometricForDelete: Boolean,
    val biometricForExport: Boolean,
    val usePasskey: Boolean
)

// Import first() extension from kotlinx.coroutines.flow
// No need for custom implementation
