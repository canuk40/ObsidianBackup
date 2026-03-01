package com.obsidianbackup.community

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.obsidianbackup.logging.ObsidianLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages beta program enrollment and early access features
 */
@Singleton
class BetaProgramManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: ObsidianLogger,
    private val analyticsManager: AnalyticsManager
) {
    
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "beta_program_prefs")
    
    private val BETA_ENROLLED_KEY = booleanPreferencesKey("beta_enrolled")
    private val BETA_CHANNEL_KEY = stringPreferencesKey("beta_channel")
    private val BETA_JOINED_DATE_KEY = stringPreferencesKey("beta_joined_date")
    
    val betaEnrolled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BETA_ENROLLED_KEY] ?: false
    }
    
    val betaChannel: Flow<BetaChannel> = context.dataStore.data.map { preferences ->
        val channelName = preferences[BETA_CHANNEL_KEY] ?: BetaChannel.STABLE.name
        BetaChannel.valueOf(channelName)
    }
    
    suspend fun enrollInBeta(channel: BetaChannel = BetaChannel.BETA): Result<Unit> {
        return try {
            context.dataStore.edit { preferences ->
                preferences[BETA_ENROLLED_KEY] = true
                preferences[BETA_CHANNEL_KEY] = channel.name
                preferences[BETA_JOINED_DATE_KEY] = System.currentTimeMillis().toString()
            }
            
            analyticsManager.logEvent("beta_enrolled", mapOf(
                "channel" to channel.name
            ))
            
            logger.i("BetaProgramManager", "User enrolled in beta channel: ${channel.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e("BetaProgramManager", "Failed to enroll in beta", e)
            Result.failure(e)
        }
    }
    
    suspend fun leaveBeta(): Result<Unit> {
        return try {
            context.dataStore.edit { preferences ->
                preferences[BETA_ENROLLED_KEY] = false
                preferences[BETA_CHANNEL_KEY] = BetaChannel.STABLE.name
            }
            
            analyticsManager.logEvent("beta_left", emptyMap())
            
            logger.i("BetaProgramManager", "User left beta program")
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e("BetaProgramManager", "Failed to leave beta", e)
            Result.failure(e)
        }
    }
    
    suspend fun switchChannel(channel: BetaChannel): Result<Unit> {
        return try {
            context.dataStore.edit { preferences ->
                preferences[BETA_CHANNEL_KEY] = channel.name
            }
            
            analyticsManager.logEvent("beta_channel_switched", mapOf(
                "new_channel" to channel.name
            ))
            
            logger.i("BetaProgramManager", "Switched to channel: ${channel.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e("BetaProgramManager", "Failed to switch channel", e)
            Result.failure(e)
        }
    }
    
    fun getBetaFeatures(channel: BetaChannel): List<BetaFeature> {
        return when (channel) {
            BetaChannel.ALPHA -> listOf(
                BetaFeature("experimental_sync", "Experimental Cloud Sync"),
                BetaFeature("advanced_compression", "Advanced Compression Algorithms"),
                BetaFeature("ml_deduplication", "ML-based Deduplication"),
                BetaFeature("beta_ui", "New UI Components")
            )
            BetaChannel.BETA -> listOf(
                BetaFeature("advanced_compression", "Advanced Compression Algorithms"),
                BetaFeature("beta_ui", "New UI Components")
            )
            BetaChannel.STABLE -> emptyList()
        }
    }
}

enum class BetaChannel(val displayName: String, val description: String) {
    STABLE("Stable", "Stable release with well-tested features"),
    BETA("Beta", "Early access to upcoming features"),
    ALPHA("Alpha", "Cutting-edge features (may be unstable)")
}

data class BetaFeature(
    val id: String,
    val name: String,
    val enabled: Boolean = true
)
