package com.obsidianbackup.community

import com.obsidianbackup.logging.ObsidianLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * No-op analytics manager for FOSS build.
 * No data is collected or transmitted.
 */
@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: ObsidianLogger
) {
    val analyticsEnabled: Flow<Boolean> = flowOf(false)

    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        logger.d("AnalyticsManager", "No-op: analytics disabled in FOSS build")
    }

    suspend fun logEvent(eventName: String, parameters: Map<String, Any> = emptyMap()) {
        // No-op
    }

    suspend fun logScreenView(screenName: String) {
        // No-op
    }

    suspend fun logBackupEvent(
        action: String,
        backupType: String? = null,
        success: Boolean? = null,
        durationMs: Long? = null
    ) {
        // No-op
    }

    suspend fun setUserProperty(name: String, value: String) {
        // No-op
    }
}
