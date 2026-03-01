// ml/nlp/NaturalLanguageProcessor.kt
package com.obsidianbackup.ml.nlp

import android.content.Context
import android.util.Log
import com.obsidianbackup.ml.NLQueryResult
import com.obsidianbackup.ml.TimeRange
import com.obsidianbackup.model.AppId
import com.obsidianbackup.model.BackupComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.*

/**
 * Natural Language Processor for backup queries using on-device NLP
 * Supports queries like:
 * - "backup my games"
 * - "backup WhatsApp from yesterday"
 * - "backup all messaging apps now"
 * - "backup photos and videos from last week"
 */
class NaturalLanguageProcessor(private val context: Context) {
    
    private val appCategoryMap = mutableMapOf<String, List<String>>()
    private val timePatterns = mutableMapOf<String, TimeRangeCalculator>()
    
    companion object {
        private const val TAG = "NLProcessor"
        
        // Common app categories
        private val CATEGORY_GAMES = listOf("game", "games", "gaming")
        private val CATEGORY_SOCIAL = listOf("social", "messaging", "chat", "whatsapp", "telegram", "signal")
        private val CATEGORY_MEDIA = listOf("photo", "photos", "video", "videos", "media", "gallery")
        private val CATEGORY_PRODUCTIVITY = listOf("note", "notes", "document", "documents", "office")
        private val CATEGORY_ALL = listOf("all", "everything", "every app")
        
        // Time keywords
        private val TIME_NOW = listOf("now", "immediately", "asap")
        private val TIME_TODAY = listOf("today")
        private val TIME_YESTERDAY = listOf("yesterday")
        private val TIME_LAST_WEEK = listOf("last week", "past week")
        private val TIME_LAST_MONTH = listOf("last month", "past month")
        
        // Component keywords
        private val COMPONENT_APK = listOf("apk", "app", "application")
        private val COMPONENT_DATA = listOf("data", "files")
        private val COMPONENT_EXTERNAL = listOf("external", "sd card", "storage")
        private val COMPONENT_OBB = listOf("obb", "game data")
    }
    
    /**
     * Initialize NLP processor
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            // Initialize app category mappings
            initializeCategories()
            
            // Initialize time pattern recognizers
            initializeTimePatterns()
            
            Log.i(TAG, "NLP processor initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize NLP processor: ${e.message}", e)
        }
    }
    
    /**
     * Parse natural language backup query
     */
    suspend fun parseBackupQuery(query: String): NLQueryResult = withContext(Dispatchers.Default) {
        try {
            val normalizedQuery = query.lowercase(Locale.getDefault()).trim()
            
            Log.i(TAG, "Parsing query: $normalizedQuery")
            
            // Extract app identifiers
            val appIds = extractAppIds(normalizedQuery)
            
            // Extract time range
            val timeRange = extractTimeRange(normalizedQuery)
            
            // Extract components
            val components = extractComponents(normalizedQuery)
            
            if (appIds.isEmpty()) {
                return@withContext NLQueryResult.Error("Could not identify which apps to backup")
            }
            
            NLQueryResult.Success(
                appIds = appIds,
                timeRange = timeRange,
                components = components
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse query: ${e.message}", e)
            NLQueryResult.Error("Failed to understand query: ${e.message}")
        }
    }
    
    /**
     * Extract app IDs from query
     */
    private fun extractAppIds(query: String): List<AppId> {
        val appIds = mutableListOf<AppId>()
        
        // Check for "all apps" patterns
        if (CATEGORY_ALL.any { query.contains(it) }) {
            // Return special marker for "all apps"
            return listOf(AppId("*"))
        }
        
        // Check for specific app names
        when {
            query.contains("whatsapp") -> appIds.add(AppId("com.whatsapp"))
            query.contains("telegram") -> appIds.add(AppId("org.telegram.messenger"))
            query.contains("signal") -> appIds.add(AppId("org.thoughtcrime.securesms"))
            query.contains("instagram") -> appIds.add(AppId("com.instagram.android"))
            query.contains("facebook") -> appIds.add(AppId("com.facebook.katana"))
            query.contains("twitter") || query.contains("x") -> appIds.add(AppId("com.twitter.android"))
            query.contains("chrome") -> appIds.add(AppId("com.android.chrome"))
            query.contains("gmail") -> appIds.add(AppId("com.google.android.gm"))
        }
        
        // Check for category-based selection
        when {
            CATEGORY_GAMES.any { query.contains(it) } -> {
                appIds.add(AppId("category:GAME"))
            }
            CATEGORY_SOCIAL.any { query.contains(it) } -> {
                appIds.add(AppId("category:SOCIAL"))
            }
            CATEGORY_MEDIA.any { query.contains(it) } -> {
                appIds.add(AppId("category:MEDIA"))
            }
            CATEGORY_PRODUCTIVITY.any { query.contains(it) } -> {
                appIds.add(AppId("category:PRODUCTIVITY"))
            }
        }
        
        return appIds
    }
    
    /**
     * Extract time range from query
     */
    private fun extractTimeRange(query: String): TimeRange? {
        val now = LocalDateTime.now()
        
        return when {
            TIME_NOW.any { query.contains(it) } -> {
                TimeRange(start = now, end = now)
            }
            TIME_TODAY.any { query.contains(it) } -> {
                val startOfDay = now.toLocalDate().atStartOfDay()
                TimeRange(start = startOfDay, end = now)
            }
            TIME_YESTERDAY.any { query.contains(it) } -> {
                val yesterday = now.minusDays(1)
                val startOfYesterday = yesterday.toLocalDate().atStartOfDay()
                val endOfYesterday = startOfYesterday.plusDays(1)
                TimeRange(start = startOfYesterday, end = endOfYesterday)
            }
            TIME_LAST_WEEK.any { query.contains(it) } -> {
                val lastWeek = now.minusWeeks(1)
                TimeRange(start = lastWeek, end = now)
            }
            TIME_LAST_MONTH.any { query.contains(it) } -> {
                val lastMonth = now.minusMonths(1)
                TimeRange(start = lastMonth, end = now)
            }
            else -> null
        }
    }
    
    /**
     * Extract backup components from query
     */
    private fun extractComponents(query: String): Set<BackupComponent> {
        val components = mutableSetOf<BackupComponent>()
        
        // Default to all components if not specified
        var hasExplicitComponents = false
        
        if (COMPONENT_APK.any { query.contains(it) }) {
            components.add(BackupComponent.APK)
            hasExplicitComponents = true
        }
        
        if (COMPONENT_DATA.any { query.contains(it) }) {
            components.add(BackupComponent.DATA)
            hasExplicitComponents = true
        }
        
        if (COMPONENT_EXTERNAL.any { query.contains(it) }) {
            components.add(BackupComponent.EXTERNAL_DATA)
            hasExplicitComponents = true
        }
        
        if (COMPONENT_OBB.any { query.contains(it) }) {
            components.add(BackupComponent.OBB)
            hasExplicitComponents = true
        }
        
        // If no explicit components mentioned, include all
        if (!hasExplicitComponents) {
            components.addAll(BackupComponent.values())
        }
        
        return components
    }
    
    /**
     * Initialize app category mappings
     */
    private fun initializeCategories() {
        appCategoryMap["games"] = listOf(
            "com.supercell.clashofclans",
            "com.mojang.minecraftpe",
            "com.pubg.krmobile"
        )
        
        appCategoryMap["social"] = listOf(
            "com.whatsapp",
            "com.facebook.katana",
            "com.instagram.android",
            "org.telegram.messenger"
        )
        
        appCategoryMap["media"] = listOf(
            "com.google.android.apps.photos",
            "com.android.gallery3d"
        )
        
        appCategoryMap["productivity"] = listOf(
            "com.microsoft.office.word",
            "com.google.android.apps.docs",
            "com.evernote"
        )
    }
    
    /**
     * Initialize time pattern recognizers
     */
    private fun initializeTimePatterns() {
        timePatterns["now"] = { TimeRange(LocalDateTime.now(), LocalDateTime.now()) }
        timePatterns["today"] = {
            val now = LocalDateTime.now()
            val startOfDay = now.toLocalDate().atStartOfDay()
            TimeRange(startOfDay, now)
        }
        timePatterns["yesterday"] = {
            val yesterday = LocalDateTime.now().minusDays(1)
            val startOfDay = yesterday.toLocalDate().atStartOfDay()
            val endOfDay = startOfDay.plusDays(1)
            TimeRange(startOfDay, endOfDay)
        }
    }
    
    /**
     * Get suggestions for autocomplete
     */
    fun getSuggestions(partial: String): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (partial.length < 2) return suggestions
        
        val lower = partial.lowercase(Locale.getDefault())
        
        // Suggest common queries
        val commonQueries = listOf(
            "backup my games",
            "backup WhatsApp",
            "backup all apps now",
            "backup photos from yesterday",
            "backup social media apps",
            "backup telegram and signal",
            "backup everything from last week"
        )
        
        suggestions.addAll(
            commonQueries.filter { it.lowercase(Locale.getDefault()).contains(lower) }
        )
        
        return suggestions.take(5)
    }
    
    /**
     * Validate query before execution
     */
    fun validateQuery(query: String): ValidationResult {
        val normalized = query.lowercase(Locale.getDefault()).trim()
        
        // Check minimum length
        if (normalized.length < 5) {
            return ValidationResult.Invalid("Query too short")
        }
        
        // Check for backup intent
        if (!normalized.contains("backup") && 
            !normalized.contains("save") && 
            !normalized.contains("export")) {
            return ValidationResult.Invalid("Query must contain backup intent")
        }
        
        // Check for app identification
        val hasAppReference = CATEGORY_ALL.any { normalized.contains(it) } ||
                CATEGORY_GAMES.any { normalized.contains(it) } ||
                CATEGORY_SOCIAL.any { normalized.contains(it) } ||
                CATEGORY_MEDIA.any { normalized.contains(it) } ||
                normalized.contains("whatsapp") ||
                normalized.contains("telegram") ||
                normalized.contains("app")
        
        if (!hasAppReference) {
            return ValidationResult.Invalid("Could not identify which apps to backup")
        }
        
        return ValidationResult.Valid
    }
}

typealias TimeRangeCalculator = () -> TimeRange

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}
