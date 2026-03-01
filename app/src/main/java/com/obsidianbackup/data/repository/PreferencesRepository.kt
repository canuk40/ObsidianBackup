// data/repository/PreferencesRepository.kt
package com.obsidianbackup.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.obsidianbackup.model.SmartSchedulingConfig
import com.obsidianbackup.model.TimeWindow
import com.obsidianbackup.model.BackupFrequency
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.automationDataStore: DataStore<Preferences> by preferencesDataStore(name = "automation_settings")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Smart Scheduling Keys
        private val SMART_SCHEDULING_ENABLED = booleanPreferencesKey("smart_scheduling_enabled")
        private val SMART_SCHEDULING_TIME_WINDOW = stringPreferencesKey("smart_scheduling_time_window")
        private val SMART_SCHEDULING_ONLY_WIFI = booleanPreferencesKey("smart_scheduling_only_wifi")
        private val SMART_SCHEDULING_ONLY_CHARGING = booleanPreferencesKey("smart_scheduling_only_charging")
        private val SMART_SCHEDULING_MIN_BATTERY = intPreferencesKey("smart_scheduling_min_battery")
        private val SMART_SCHEDULING_FREQUENCY = stringPreferencesKey("smart_scheduling_frequency")
        private val SMART_SCHEDULING_PROFILE_ID = stringPreferencesKey("smart_scheduling_profile_id")
        
        // Parallel Operations
        private val PARALLEL_OPERATIONS_ENABLED = booleanPreferencesKey("parallel_operations_enabled")
    }
    
    /**
     * Get smart scheduling configuration
     */
    fun getSmartSchedulingConfig(): Flow<SmartSchedulingConfig> {
        return context.automationDataStore.data.map { preferences ->
            SmartSchedulingConfig(
                enabled = preferences[SMART_SCHEDULING_ENABLED] ?: false,
                preferredTimeWindow = TimeWindow.valueOf(
                    preferences[SMART_SCHEDULING_TIME_WINDOW] ?: TimeWindow.AUTO.name
                ),
                onlyOnWifi = preferences[SMART_SCHEDULING_ONLY_WIFI] ?: true,
                onlyWhenCharging = preferences[SMART_SCHEDULING_ONLY_CHARGING] ?: true,
                minimumBatteryLevel = preferences[SMART_SCHEDULING_MIN_BATTERY] ?: 50,
                backupFrequency = BackupFrequency.valueOf(
                    preferences[SMART_SCHEDULING_FREQUENCY] ?: BackupFrequency.DAILY.name
                ),
                profileId = preferences[SMART_SCHEDULING_PROFILE_ID]
            )
        }
    }
    
    /**
     * Save smart scheduling configuration
     */
    suspend fun saveSmartSchedulingConfig(config: SmartSchedulingConfig) {
        context.automationDataStore.edit { preferences ->
            preferences[SMART_SCHEDULING_ENABLED] = config.enabled
            preferences[SMART_SCHEDULING_TIME_WINDOW] = config.preferredTimeWindow.name
            preferences[SMART_SCHEDULING_ONLY_WIFI] = config.onlyOnWifi
            preferences[SMART_SCHEDULING_ONLY_CHARGING] = config.onlyWhenCharging
            preferences[SMART_SCHEDULING_MIN_BATTERY] = config.minimumBatteryLevel
            preferences[SMART_SCHEDULING_FREQUENCY] = config.backupFrequency.name
            config.profileId?.let {
                preferences[SMART_SCHEDULING_PROFILE_ID] = it
            }
        }
    }
    
    /**
     * Get parallel operations enabled state
     */
    fun getParallelOperationsEnabled(): Flow<Boolean> {
        return context.automationDataStore.data.map { preferences ->
            preferences[PARALLEL_OPERATIONS_ENABLED] ?: false
        }
    }
    
    /**
     * Set parallel operations enabled state
     */
    suspend fun setParallelOperationsEnabled(enabled: Boolean) {
        context.automationDataStore.edit { preferences ->
            preferences[PARALLEL_OPERATIONS_ENABLED] = enabled
        }
    }
}
