// performance/MemoryOptimizationManager.kt
package com.obsidianbackup.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

/**
 * Manages memory optimization and provides utilities for memory-efficient operations
 */
class MemoryOptimizationManager(context: Context) {
    
    private val contextRef = WeakReference(context)
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        ?: throw IllegalStateException("ActivityManager service not available")
    
    /**
     * Get available memory information
     */
    fun getMemoryInfo(): MemoryInfo {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val runtime = Runtime.getRuntime()
        
        return MemoryInfo(
            availableMemory = memInfo.availMem,
            totalMemory = memInfo.totalMem,
            threshold = memInfo.threshold,
            lowMemory = memInfo.lowMemory,
            usedMemory = runtime.totalMemory() - runtime.freeMemory(),
            maxMemory = runtime.maxMemory(),
            freeMemory = runtime.freeMemory()
        )
    }
    
    /**
     * Check if device is in low memory state
     */
    fun isLowMemory(): Boolean {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.lowMemory
    }
    
    /**
     * Get memory usage percentage (0-100)
     */
    fun getMemoryUsagePercentage(): Int {
        val memInfo = getMemoryInfo()
        val used = memInfo.totalMemory - memInfo.availableMemory
        return ((used.toDouble() / memInfo.totalMemory.toDouble()) * 100).toInt()
    }
    
    /**
     * Check if app should reduce memory usage
     */
    fun shouldReduceMemoryUsage(): Boolean {
        val usage = getMemoryUsagePercentage()
        return usage > MEMORY_PRESSURE_THRESHOLD || isLowMemory()
    }
    
    /**
     * Request garbage collection (use sparingly)
     */
    fun requestGarbageCollection() {
        System.gc()
    }
    
    /**
     * Trim memory based on level
     */
    fun trimMemory(level: Int) {
        contextRef.get()?.let { context ->
            // Clear image caches if using Coil
            try {
                coil.Coil.imageLoader(context).memoryCache?.clear()
            } catch (e: Exception) {
                // Coil might not be initialized yet
            }
            
            // Request GC for critical levels
            when (level) {
                android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
                android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                    requestGarbageCollection()
                }
            }
        }
    }
    
    /**
     * Get native heap allocation info
     */
    fun getNativeHeapInfo(): NativeHeapInfo {
        return NativeHeapInfo(
            allocatedSize = Debug.getNativeHeapAllocatedSize(),
            freeSize = Debug.getNativeHeapFreeSize(),
            totalSize = Debug.getNativeHeapSize()
        )
    }
    
    /**
     * Calculate optimal chunk size for processing based on available memory
     */
    suspend fun getOptimalProcessingChunkSize(itemSize: Long): Int = withContext(Dispatchers.Default) {
        val memInfo = getMemoryInfo()
        val availableForProcessing = (memInfo.availableMemory * 0.3).toLong() // Use 30% of available
        
        val optimalCount = (availableForProcessing / itemSize).toInt()
        
        return@withContext when {
            optimalCount > MAX_CHUNK_SIZE -> MAX_CHUNK_SIZE
            optimalCount < MIN_CHUNK_SIZE -> MIN_CHUNK_SIZE
            else -> optimalCount
        }
    }
    
    /**
     * Check if there's enough memory for an operation
     */
    fun hasEnoughMemoryFor(requiredBytes: Long): Boolean {
        val memInfo = getMemoryInfo()
        return memInfo.availableMemory > requiredBytes * MEMORY_SAFETY_MULTIPLIER
    }
    
    /**
     * Get memory class (max heap size in MB)
     */
    fun getMemoryClass(): Int {
        return activityManager.memoryClass
    }
    
    /**
     * Get large memory class if available
     */
    fun getLargeMemoryClass(): Int {
        return activityManager.largeMemoryClass
    }
    
    data class MemoryInfo(
        val availableMemory: Long,
        val totalMemory: Long,
        val threshold: Long,
        val lowMemory: Boolean,
        val usedMemory: Long,
        val maxMemory: Long,
        val freeMemory: Long
    ) {
        fun toReadableString(): String {
            return """
                Available: ${availableMemory / 1024 / 1024} MB
                Total: ${totalMemory / 1024 / 1024} MB
                Used: ${usedMemory / 1024 / 1024} MB
                Max: ${maxMemory / 1024 / 1024} MB
                Low Memory: $lowMemory
            """.trimIndent()
        }
    }
    
    data class NativeHeapInfo(
        val allocatedSize: Long,
        val freeSize: Long,
        val totalSize: Long
    )
    
    companion object {
        private const val MEMORY_PRESSURE_THRESHOLD = 80 // Percentage
        private const val MEMORY_SAFETY_MULTIPLIER = 1.5 // Safety margin
        private const val MIN_CHUNK_SIZE = 10
        private const val MAX_CHUNK_SIZE = 1000
    }
}

/**
 * Object pool for reusing expensive objects
 */
class ObjectPool<T>(
    private val factory: () -> T,
    private val reset: (T) -> Unit,
    private val maxSize: Int = 10
) {
    private val pool = mutableListOf<T>()
    
    @Synchronized
    fun acquire(): T {
        return if (pool.isNotEmpty()) {
            pool.removeAt(pool.size - 1)
        } else {
            factory()
        }
    }
    
    @Synchronized
    fun release(obj: T) {
        if (pool.size < maxSize) {
            reset(obj)
            pool.add(obj)
        }
    }
    
    @Synchronized
    fun clear() {
        pool.clear()
    }
}
