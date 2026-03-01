package com.obsidianbackup.tv.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tv_settings")

@Singleton
class TVSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    object PreferenceKeys {
        val AUTO_BACKUP = booleanPreferencesKey("auto_backup")
        val BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
        val INCLUDE_DATA = booleanPreferencesKey("include_data")
        val CLOUD_PROVIDER = stringPreferencesKey("cloud_provider")
        val COMPRESSION = booleanPreferencesKey("compression")
        val ENCRYPTION = booleanPreferencesKey("encryption")
    }

    val autoBackup: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.AUTO_BACKUP] ?: false
    }

    val backupFrequency: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.BACKUP_FREQUENCY] ?: "daily"
    }

    val includeData: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.INCLUDE_DATA] ?: true
    }

    val cloudProvider: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.CLOUD_PROVIDER] ?: "none"
    }

    val compression: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.COMPRESSION] ?: true
    }

    val encryption: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.ENCRYPTION] ?: false
    }

    suspend fun setAutoBackup(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.AUTO_BACKUP] = enabled
        }
    }

    suspend fun setBackupFrequency(frequency: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.BACKUP_FREQUENCY] = frequency
        }
    }

    suspend fun setIncludeData(include: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.INCLUDE_DATA] = include
        }
    }

    suspend fun setCloudProvider(provider: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.CLOUD_PROVIDER] = provider
        }
    }

    suspend fun setCompression(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.COMPRESSION] = enabled
        }
    }

    suspend fun setEncryption(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.ENCRYPTION] = enabled
        }
    }
}
