package com.obsidianbackup.wear.utils

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ln
import kotlin.math.pow

/**
 * Formatting utilities for Wear OS displays
 */
object FormatUtils {
    
    private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

    /**
     * Format timestamp to readable date/time
     */
    fun formatDateTime(timestamp: Long): String {
        if (timestamp <= 0) return "Never"
        return dateFormat.format(Date(timestamp))
    }

    /**
     * Format timestamp to time only
     */
    fun formatTime(timestamp: Long): String {
        if (timestamp <= 0) return "--:--"
        return timeFormat.format(Date(timestamp))
    }

    /**
     * Format timestamp to short date
     */
    fun formatShortDate(timestamp: Long): String {
        if (timestamp <= 0) return "Never"
        return shortDateFormat.format(Date(timestamp))
    }

    /**
     * Format bytes to human readable size
     */
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
        
        return String.format(
            "%.2f %s",
            bytes / 1024.0.pow(digitGroups.toDouble()),
            units[digitGroups]
        )
    }

    /**
     * Format duration in milliseconds to readable string
     */
    fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }

    /**
     * Format relative time (e.g., "2m ago", "5h ago")
     */
    fun formatRelativeTime(timestamp: Long): String {
        if (timestamp <= 0) return "Never"
        
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            days > 0 -> "${days}d ago"
            hours > 0 -> "${hours}h ago"
            minutes > 0 -> "${minutes}m ago"
            seconds > 30 -> "${seconds}s ago"
            else -> "Just now"
        }
    }
}
