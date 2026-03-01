package com.obsidianbackup.deeplink

import android.content.Context
import android.net.Uri
import com.obsidianbackup.logging.ObsidianLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analytics tracker for deep link usage
 */
@Singleton
class DeepLinkAnalytics @Inject constructor(
    private val context: Context,
    private val logger: ObsidianLogger
) {
    
    private val json = Json { prettyPrint = false }
    private val scope = CoroutineScope(Dispatchers.IO)
    // Local-only audit log — records deep link events on-device for debugging.
    // No data is transmitted externally.
    private val analyticsFile: File by lazy {
        File(context.filesDir, "deeplink_audit.jsonl")
    }
    
    @Serializable
    data class DeepLinkAnalyticsEvent(
        val timestamp: Long,
        val uri: String,
        val action: String,
        val success: Boolean,
        val authenticated: Boolean,
        val errorReason: String? = null,
        val source: String? = null,
        val durationMs: Long? = null,
        val metadata: Map<String, String> = emptyMap()
    )
    
    /**
     * Track a deep link event
     */
    fun trackDeepLinkEvent(event: DeepLinkEvent) {
        scope.launch {
            try {
                val analyticsEvent = DeepLinkAnalyticsEvent(
                    timestamp = event.timestamp,
                    uri = event.metadata["uri"] ?: "unknown",
                    action = event.action,
                    success = event.success,
                    authenticated = event.metadata["authenticated"]?.toBoolean() ?: false,
                    errorReason = event.errorReason,
                    source = event.source,
                    durationMs = event.metadata["durationMs"]?.toLongOrNull(),
                    metadata = event.metadata
                )
                
                // Log to file
                analyticsFile.appendText(json.encodeToString(analyticsEvent) + "\n")
                
                // Log to system logger
                if (event.success) {
                    logger.i(
                        TAG,
                        "Deep link handled: ${event.action}",
                        metadata = event.metadata
                    )
                } else {
                    logger.w(
                        TAG,
                        "Deep link failed: ${event.action} - ${event.errorReason}",
                        metadata = event.metadata
                    )
                }
                
                // Rotate log file if too large
                rotateLogsIfNeeded()
            } catch (e: Exception) {
                logger.e(TAG, "Failed to track deep link event", e)
            }
        }
    }
    
    /**
     * Track successful deep link
     */
    fun trackSuccess(
        uri: Uri,
        action: DeepLinkAction,
        authenticated: Boolean = false,
        durationMs: Long? = null
    ) {
        val event = DeepLinkEvent(
            action = action.javaClass.simpleName,
            source = uri.getQueryParameter("source"),
            success = true,
            metadata = mapOf(
                "uri" to uri.toString(),
                "authenticated" to authenticated.toString()
            ).let { map ->
                durationMs?.let { map + ("durationMs" to it.toString()) } ?: map
            }
        )
        trackDeepLinkEvent(event)
    }
    
    /**
     * Track failed deep link
     */
    fun trackFailure(
        uri: Uri,
        action: DeepLinkAction,
        reason: String
    ) {
        val event = DeepLinkEvent(
            action = action.javaClass.simpleName,
            source = uri.getQueryParameter("source"),
            success = false,
            errorReason = reason,
            metadata = mapOf(
                "uri" to uri.toString()
            )
        )
        trackDeepLinkEvent(event)
    }
    
    /**
     * Get analytics summary
     */
    suspend fun getAnalyticsSummary(): DeepLinkAnalyticsSummary {
        return try {
            val events = readAnalyticsEvents()
            
            DeepLinkAnalyticsSummary(
                totalEvents = events.size,
                successfulEvents = events.count { it.success },
                failedEvents = events.count { !it.success },
                authenticatedEvents = events.count { it.authenticated },
                actionCounts = events.groupingBy { it.action }.eachCount(),
                averageDurationMs = events.mapNotNull { it.durationMs }.average().takeIf { !it.isNaN() },
                mostCommonErrors = events
                    .mapNotNull { it.errorReason }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .take(5)
                    .associate { it.key to it.value }
            )
        } catch (e: Exception) {
            logger.e(TAG, "Failed to generate analytics summary", e)
            DeepLinkAnalyticsSummary()
        }
    }
    
    private fun readAnalyticsEvents(): List<DeepLinkAnalyticsEvent> {
        return try {
            if (!analyticsFile.exists()) {
                return emptyList()
            }
            
            analyticsFile.readLines()
                .mapNotNull { line ->
                    try {
                        json.decodeFromString<DeepLinkAnalyticsEvent>(line)
                    } catch (e: Exception) {
                        null
                    }
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun rotateLogsIfNeeded() {
        try {
            val maxSize = 5 * 1024 * 1024 // 5 MB
            if (analyticsFile.exists() && analyticsFile.length() > maxSize) {
                val backupFile = File(context.filesDir, "deeplink_analytics.old.jsonl")
                analyticsFile.renameTo(backupFile)
                backupFile.delete()
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to rotate analytics logs", e)
        }
    }
    
    /**
     * Export analytics data for analysis
     */
    fun exportAnalytics(): File? {
        return if (analyticsFile.exists()) {
            analyticsFile
        } else {
            null
        }
    }
    
    /**
     * Clear analytics data
     */
    fun clearAnalytics() {
        scope.launch {
            try {
                analyticsFile.delete()
                logger.i(TAG, "Analytics data cleared")
            } catch (e: Exception) {
                logger.e(TAG, "Failed to clear analytics", e)
            }
        }
    }
    
    companion object {
        private const val TAG = "DeepLinkAnalytics"
    }
}

/**
 * Summary of deep link analytics
 */
data class DeepLinkAnalyticsSummary(
    val totalEvents: Int = 0,
    val successfulEvents: Int = 0,
    val failedEvents: Int = 0,
    val authenticatedEvents: Int = 0,
    val actionCounts: Map<String, Int> = emptyMap(),
    val averageDurationMs: Double? = null,
    val mostCommonErrors: Map<String, Int> = emptyMap()
)
