// util/JsonUtils.kt
package com.obsidianbackup.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

object JsonUtils {
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = false
    }
    
    fun parseStringList(jsonString: String): List<String> {
        return try {
            json.decodeFromString<List<String>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun encodeStringList(list: List<String>): String {
        return json.encodeToString(list)
    }
}
