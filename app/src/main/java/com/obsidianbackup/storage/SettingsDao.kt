// storage/SettingsDao.kt
package com.obsidianbackup.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SettingsEntity)

    @Query("SELECT value FROM settings WHERE key = :key")
    suspend fun getSetting(key: String): String?

    @Query("SELECT * FROM settings")
    fun getAllSettings(): Flow<List<SettingsEntity>>

    @Query("DELETE FROM settings WHERE key = :key")
    suspend fun deleteSetting(key: String)

    @Query("UPDATE settings SET value = :value, updated_at = :updatedAt WHERE key = :key")
    suspend fun updateSetting(key: String, value: String, updatedAt: Long)

    // Convenience methods
    suspend fun putString(key: String, value: String) {
        insertSetting(SettingsEntity(key, value, System.currentTimeMillis()))
    }

    suspend fun getString(key: String, defaultValue: String = ""): String {
        return getSetting(key) ?: defaultValue
    }

    suspend fun putBoolean(key: String, value: Boolean) {
        putString(key, value.toString())
    }

    suspend fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return getString(key).toBooleanStrictOrNull() ?: defaultValue
    }

    suspend fun putInt(key: String, value: Int) {
        putString(key, value.toString())
    }

    suspend fun getInt(key: String, defaultValue: Int = 0): Int {
        return getString(key).toIntOrNull() ?: defaultValue
    }

    suspend fun putLong(key: String, value: Long) {
        putString(key, value.toString())
    }

    suspend fun getLong(key: String, defaultValue: Long = 0L): Long {
        return getString(key).toLongOrNull() ?: defaultValue
    }
}
