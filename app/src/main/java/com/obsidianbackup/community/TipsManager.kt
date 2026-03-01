package com.obsidianbackup.community

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.obsidianbackup.logging.ObsidianLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Manages contextual tips and tricks system
 */
@Singleton
class TipsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: ObsidianLogger
) {
    
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tips_prefs")
    
    private val DISMISSED_TIPS_KEY = stringSetPreferencesKey("dismissed_tips")
    
    val dismissedTips: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[DISMISSED_TIPS_KEY] ?: emptySet()
    }
    
    suspend fun dismissTip(tipId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[DISMISSED_TIPS_KEY] ?: emptySet()
            preferences[DISMISSED_TIPS_KEY] = current + tipId
        }
        logger.d("TipsManager", "Tip dismissed: $tipId")
    }
    
    suspend fun resetTips() {
        context.dataStore.edit { preferences ->
            preferences.remove(DISMISSED_TIPS_KEY)
        }
        logger.i("TipsManager", "All tips reset")
    }
    
    fun getAllTips(): List<Tip> {
        return listOf(
            Tip(
                id = "tip_encryption",
                title = "Enable Encryption",
                description = "Always enable encryption for cloud backups to protect your data",
                category = TipCategory.SECURITY,
                priority = TipPriority.HIGH
            ),
            Tip(
                id = "tip_incremental",
                title = "Use Incremental Backups",
                description = "Incremental backups save time and bandwidth by only backing up changed files",
                category = TipCategory.PERFORMANCE,
                priority = TipPriority.MEDIUM
            ),
            Tip(
                id = "tip_wifi_only",
                title = "Backup on WiFi",
                description = "Configure backups to run only on WiFi to save mobile data",
                category = TipCategory.SETTINGS,
                priority = TipPriority.MEDIUM
            ),
            Tip(
                id = "tip_schedule",
                title = "Schedule Regular Backups",
                description = "Set up nightly backups to ensure your data is always protected",
                category = TipCategory.AUTOMATION,
                priority = TipPriority.HIGH
            ),
            Tip(
                id = "tip_verify",
                title = "Verify Your Backups",
                description = "Regularly verify backup integrity to ensure data can be restored",
                category = TipCategory.BEST_PRACTICE,
                priority = TipPriority.HIGH
            ),
            Tip(
                id = "tip_retention",
                title = "Set Retention Policies",
                description = "Configure how long to keep old backups to manage storage space",
                category = TipCategory.SETTINGS,
                priority = TipPriority.LOW
            ),
            Tip(
                id = "tip_selective",
                title = "Selective Backups",
                description = "Choose which apps and data to backup to save time and space",
                category = TipCategory.PERFORMANCE,
                priority = TipPriority.MEDIUM
            ),
            Tip(
                id = "tip_biometric",
                title = "Enable Biometric Lock",
                description = "Protect your backups with fingerprint or face authentication",
                category = TipCategory.SECURITY,
                priority = TipPriority.MEDIUM
            ),
            Tip(
                id = "tip_widgets",
                title = "Use Quick Action Widget",
                description = "Add a home screen widget for one-tap backups",
                category = TipCategory.PRODUCTIVITY,
                priority = TipPriority.LOW
            ),
            Tip(
                id = "tip_logs",
                title = "Check Backup Logs",
                description = "Review logs regularly to catch any backup issues early",
                category = TipCategory.BEST_PRACTICE,
                priority = TipPriority.MEDIUM
            )
        )
    }
    
    suspend fun getTipOfTheDay(): Tip? {
        val dismissed = dismissedTips.first()
        val availableTips = getAllTips().filter { it.id !in dismissed }
        
        return if (availableTips.isNotEmpty()) {
            availableTips[Random.nextInt(availableTips.size)]
        } else null
    }
    
    fun getTipsByCategory(category: TipCategory): List<Tip> {
        return getAllTips().filter { it.category == category }
    }
}

data class Tip(
    val id: String,
    val title: String,
    val description: String,
    val category: TipCategory,
    val priority: TipPriority
)

enum class TipCategory {
    SECURITY,
    PERFORMANCE,
    SETTINGS,
    AUTOMATION,
    BEST_PRACTICE,
    PRODUCTIVITY
}

enum class TipPriority {
    HIGH,
    MEDIUM,
    LOW
}
