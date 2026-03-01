// performance/PerformanceProfiler.kt
package com.obsidianbackup.performance

import android.os.Build
import android.os.Debug
import android.os.Trace
import android.util.Log
import com.obsidianbackup.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Performance profiling utilities for monitoring app performance
 */
object PerformanceProfiler {
    
    private const val TAG = "PerformanceProfiler"
    private val traces = ConcurrentHashMap<String, Long>()
    private val measurements = ConcurrentHashMap<String, MutableList<Long>>()
    
    /**
     * Begin a trace section (Android Profiler compatible)
     */
    inline fun <T> trace(sectionName: String, block: () -> T): T {
        beginTrace(sectionName)
        try {
            return block()
        } finally {
            endTrace()
        }
    }
    
    /**
     * Async trace with coroutines
     */
    suspend inline fun <T> traceAsync(sectionName: String, crossinline block: suspend () -> T): T {
        beginTrace(sectionName)
        try {
            return block()
        } finally {
            endTrace()
        }
    }
    
    /**
     * Begin a trace section
     */
    fun beginTrace(sectionName: String) {
        if (BuildConfig.DEBUG) {
            Trace.beginSection(sectionName)
            traces[sectionName] = System.nanoTime()
        }
    }
    
    /**
     * End the current trace section
     */
    fun endTrace() {
        if (BuildConfig.DEBUG) {
            Trace.endSection()
        }
    }
    
    /**
     * Measure execution time
     */
    internal inline fun <T> measure(operationName: String, block: () -> T): T {
        val startTime = System.nanoTime()
        try {
            return block()
        } finally {
            val duration = System.nanoTime() - startTime
            recordMeasurement(operationName, duration)
        }
    }
    
    /**
     * Measure async execution time
     */
    internal suspend inline fun <T> measureAsync(operationName: String, crossinline block: suspend () -> T): T {
        val startTime = System.nanoTime()
        try {
            return block()
        } finally {
            val duration = System.nanoTime() - startTime
            recordMeasurement(operationName, duration)
        }
    }
    
    /**
     * Record a measurement
     */
    internal fun recordMeasurement(operationName: String, durationNanos: Long) {
        if (BuildConfig.DEBUG) {
            measurements.getOrPut(operationName) { mutableListOf() }.add(durationNanos)
            
            val durationMs = durationNanos / 1_000_000.0
            if (durationMs > SLOW_OPERATION_THRESHOLD_MS) {
                Log.w(TAG, "Slow operation '$operationName': ${durationMs}ms")
            }
        }
    }
    
    /**
     * Get statistics for an operation
     */
    fun getStats(operationName: String): OperationStats? {
        val measurements = measurements[operationName] ?: return null
        if (measurements.isEmpty()) return null
        
        val sorted = measurements.sorted()
        val count = measurements.size
        val sum = measurements.sum()
        val avg = sum / count
        
        return OperationStats(
            operationName = operationName,
            count = count,
            avgNanos = avg,
            minNanos = sorted.first(),
            maxNanos = sorted.last(),
            medianNanos = sorted[count / 2],
            p95Nanos = sorted[(count * 0.95).toInt()],
            p99Nanos = sorted[(count * 0.99).toInt()]
        )
    }
    
    /**
     * Get all statistics
     */
    fun getAllStats(): Map<String, OperationStats> {
        return measurements.keys.mapNotNull { key ->
            getStats(key)?.let { key to it }
        }.toMap()
    }
    
    /**
     * Print all statistics to log
     */
    fun printStats() {
        if (!BuildConfig.DEBUG) return
        
        Log.d(TAG, "=== Performance Statistics ===")
        getAllStats().forEach { (name, stats) ->
            Log.d(TAG, stats.toString())
        }
    }
    
    /**
     * Clear all measurements
     */
    fun clear() {
        traces.clear()
        measurements.clear()
    }
    
    /**
     * Start method tracing (creates trace file)
     */
    fun startMethodTracing(traceName: String = "obsidian_backup") {
        if (BuildConfig.DEBUG) {
            Debug.startMethodTracing(traceName)
            Log.d(TAG, "Started method tracing: $traceName")
        }
    }
    
    /**
     * Stop method tracing
     */
    fun stopMethodTracing() {
        if (BuildConfig.DEBUG) {
            Debug.stopMethodTracing()
            Log.d(TAG, "Stopped method tracing")
        }
    }
    
    /**
     * Get current thread time in nanoseconds
     */
    fun getCurrentThreadTimeNano(): Long {
        return Debug.threadCpuTimeNanos()
    }
    
    /**
     * Log memory usage
     */
    fun logMemoryUsage(tag: String = "MemoryUsage") {
        if (BuildConfig.DEBUG) {
            val runtime = Runtime.getRuntime()
            val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
            val maxMemory = runtime.maxMemory() / 1024 / 1024
            val freeMemory = runtime.freeMemory() / 1024 / 1024
            
            Log.d(TAG, "$tag - Used: ${usedMemory}MB, Max: ${maxMemory}MB, Free: ${freeMemory}MB")
        }
    }
    
    data class OperationStats(
        val operationName: String,
        val count: Int,
        val avgNanos: Long,
        val minNanos: Long,
        val maxNanos: Long,
        val medianNanos: Long,
        val p95Nanos: Long,
        val p99Nanos: Long
    ) {
        override fun toString(): String {
            return """
                $operationName:
                  Count: $count
                  Avg: ${avgNanos / 1_000_000.0}ms
                  Min: ${minNanos / 1_000_000.0}ms
                  Max: ${maxNanos / 1_000_000.0}ms
                  Median: ${medianNanos / 1_000_000.0}ms
                  P95: ${p95Nanos / 1_000_000.0}ms
                  P99: ${p99Nanos / 1_000_000.0}ms
            """.trimIndent()
        }
    }
    
    private const val SLOW_OPERATION_THRESHOLD_MS = 16 // One frame at 60fps
}

/**
 * Extension function for easy profiling
 */
suspend fun <T> profileOperation(name: String, block: suspend () -> T): T {
    return PerformanceProfiler.traceAsync(name) {
        PerformanceProfiler.measureAsync(name, block)
    }
}
