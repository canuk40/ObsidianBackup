package com.obsidianbackup.community

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.obsidianbackup.logging.ObsidianLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages user onboarding experience
 */
@Singleton
class OnboardingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: ObsidianLogger,
    private val analyticsManager: AnalyticsManager
) {
    
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding_prefs")
    
    private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
    private val CURRENT_STEP_KEY = intPreferencesKey("current_step")
    
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED_KEY] ?: false
    }
    
    val currentStep: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[CURRENT_STEP_KEY] ?: 0
    }
    
    suspend fun startOnboarding() {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = false
            preferences[CURRENT_STEP_KEY] = 0
        }
        analyticsManager.logEvent("onboarding_started", emptyMap())
    }
    
    suspend fun completeOnboarding() {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = true
        }
        analyticsManager.logEvent("onboarding_completed", emptyMap())
        logger.i("OnboardingManager", "Onboarding completed")
    }
    
    suspend fun setCurrentStep(step: Int) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_STEP_KEY] = step
        }
    }
    
    suspend fun skipOnboarding() {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = true
        }
        analyticsManager.logEvent("onboarding_skipped", emptyMap())
    }
    
    fun getOnboardingSteps(): List<OnboardingStep> {
        return listOf(
            OnboardingStep(
                id = 0,
                title = "Welcome to ObsidianBackup",
                description = "Your complete backup solution with military-grade encryption",
                illustration = "welcome"
            ),
            OnboardingStep(
                id = 1,
                title = "Choose Your Storage",
                description = "Select from local storage, Google Drive, or WebDAV servers",
                illustration = "storage"
            ),
            OnboardingStep(
                id = 2,
                title = "Secure with Encryption",
                description = "Your data is protected with AES-256 encryption",
                illustration = "security"
            ),
            OnboardingStep(
                id = 3,
                title = "Automate Backups",
                description = "Schedule automatic backups on WiFi, charging, or custom times",
                illustration = "automation"
            ),
            OnboardingStep(
                id = 4,
                title = "Stay in Control",
                description = "Monitor backups, restore data, and manage your privacy settings",
                illustration = "control"
            )
        )
    }
}

data class OnboardingStep(
    val id: Int,
    val title: String,
    val description: String,
    val illustration: String
)
