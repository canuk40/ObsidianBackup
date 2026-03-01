// util/SizeFormatter.kt
package com.obsidianbackup.util

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Utility for formatting file sizes
 */
@Singleton
class SizeFormatter @Inject constructor() {
    
    fun formatBytes(bytes: Long): String {
        if (bytes < 0) return "0 B"
        if (bytes < 1024) return "$bytes B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return String.format("%.1f %s", value, units[digitGroups])
    }
    
    fun formatBytesCompact(bytes: Long): String {
        if (bytes < 0) return "0"
        if (bytes < 1024) return "$bytes B"
        
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> String.format("%.1f GB", gb)
            mb >= 1 -> String.format("%.1f MB", mb)
            else -> String.format("%.1f KB", kb)
        }
    }
}
