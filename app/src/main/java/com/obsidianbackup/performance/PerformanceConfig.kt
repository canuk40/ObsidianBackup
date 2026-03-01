// performance/PerformanceConfig.kt
package com.obsidianbackup.performance

import android.content.Context
import android.os.Build
import androidx.work.Configuration
import androidx.work.WorkManager
import com.obsidianbackup.BuildConfig

/**
 * Centralized performance configuration for the application
 */
object PerformanceConfig {
    
    // Database Configuration
    const val DB_CACHE_SIZE = 10 * 1024 * 1024 // 10MB
    const val DB_PAGE_SIZE = 50
    const val DB_MAX_CONNECTIONS = 4
    
    // Memory Configuration
    const val MEMORY_CACHE_PERCENTAGE = 25 // 25% of available memory
    const val MEMORY_PRESSURE_THRESHOLD = 80 // Percentage
    const val MAX_BITMAP_SIZE = 2048 // Max dimension
    
    // Network Configuration
    const val HTTP_CACHE_SIZE = 50L * 1024 * 1024 // 50MB
    const val NETWORK_TIMEOUT_SECONDS = 30L
    const val MAX_RETRY_ATTEMPTS = 3
    
    // Image Configuration
    const val IMAGE_DISK_CACHE_SIZE = 100L * 1024 * 1024 // 100MB
    const val IMAGE_MEMORY_CACHE_PERCENTAGE = 25
    const val IMAGE_QUALITY_HIGH = 90
    const val IMAGE_QUALITY_MEDIUM = 80
    const val IMAGE_QUALITY_LOW = 60
    
    // UI Configuration
    const val LIST_PAGE_SIZE = 50
    const val LIST_PREFETCH_DISTANCE = 10
    const val LIST_PAGINATION_THRESHOLD = 10
    
    // WorkManager Configuration
    const val WORK_BACKUP_INTERVAL_HOURS = 24L
    const val WORK_SYNC_INTERVAL_HOURS = 6L
    const val WORK_FLEX_INTERVAL_HOURS = 2L
    
    // Battery Configuration
    const val MIN_BATTERY_LEVEL = 20
    const val OPTIMAL_BATTERY_LEVEL = 50
    const val THERMAL_THRESHOLD = 3 // MODERATE
    
    /**
     * Initialize performance configurations
     */
    fun initialize(context: Context) {
        initializeWorkManager(context)
        initializeProfiler()
    }
    
    /**
     * Configure WorkManager for optimal performance
     */
    private fun initializeWorkManager(context: Context) {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
            .setMaxSchedulerLimit(20) // Limit concurrent work
            .build()
        
        try {
            WorkManager.initialize(context, config)
        } catch (e: IllegalStateException) {
            // Already initialized
        }
    }
    
    /**
     * Initialize performance profiler
     */
    private fun initializeProfiler() {
        if (BuildConfig.DEBUG) {
            // Enable profiling in debug builds
            PerformanceProfiler.clear()
        }
    }
    
    /**
     * Check if device supports high-performance features
     */
    fun isHighPerformanceDevice(context: Context): Boolean {
        val memoryManager = MemoryOptimizationManager(context)
        val memoryClass = memoryManager.getMemoryClass()
        
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> memoryClass >= 512 // 512MB+
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> memoryClass >= 256 // 256MB+
            else -> memoryClass >= 128 // 128MB+
        }
    }
    
    /**
     * Get recommended page size based on device capabilities
     */
    fun getRecommendedPageSize(context: Context): Int {
        return if (isHighPerformanceDevice(context)) {
            LIST_PAGE_SIZE * 2 // 100 items for high-end devices
        } else {
            LIST_PAGE_SIZE // 50 items for low-end devices
        }
    }
    
    /**
     * Get recommended image quality based on network and battery
     */
    fun getRecommendedImageQuality(
        networkManager: NetworkOptimizationManager,
        batteryManager: BatteryOptimizationManager
    ): Int {
        return when {
            batteryManager.isPowerSaveMode() -> IMAGE_QUALITY_LOW
            !networkManager.isWifiConnected() -> IMAGE_QUALITY_MEDIUM
            else -> IMAGE_QUALITY_HIGH
        }
    }
}

/**
 * Performance mode enumeration
 */
enum class PerformanceMode {
    HIGH_PERFORMANCE,  // Fastest, uses more resources
    BALANCED,          // Default, balanced approach
    POWER_SAVER        // Slowest, conserves battery
}
