package com.obsidianbackup.domain.root

import com.obsidianbackup.rootcore.shell.ShellExecutor
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dalvik/ART cache cleanup utility.
 *
 * Clears app caches, Dalvik/ART dex caches, and optimized profiles.
 * Useful for freeing storage and forcing dex recompilation.
 */
@Singleton
class CacheCleanupManager @Inject constructor(
    private val shellExecutor: ShellExecutor
) {
    companion object {
        private const val TAG = "[CacheCleanup]"

        private val SYSTEM_CACHE_PATHS = listOf(
            "/data/dalvik-cache",
            "/cache/dalvik-cache"
        )
    }

    data class CleanupResult(
        val packageName: String? = null,
        val bytesFreed: Long = 0,
        val success: Boolean = true,
        val message: String = ""
    )

    data class BatchCleanupResult(
        val results: List<CleanupResult>,
        val totalBytesFreed: Long
    )

    /**
     * Clear a specific app's cache directory.
     */
    suspend fun clearAppCache(packageName: String): CleanupResult {
        val cacheDir = "/data/data/$packageName/cache"
        val codeCacheDir = "/data/data/$packageName/code_cache"

        val sizeBefore = getDirectorySize(cacheDir) + getDirectorySize(codeCacheDir)

        shellExecutor.executeRoot("rm -rf $cacheDir/*")
        shellExecutor.executeRoot("rm -rf $codeCacheDir/*")

        val sizeAfter = getDirectorySize(cacheDir) + getDirectorySize(codeCacheDir)
        val freed = sizeBefore - sizeAfter

        Timber.d("$TAG Cleared cache for $packageName: freed ${freed / 1024}KB")
        return CleanupResult(packageName, freed, true, "Cache cleared")
    }

    /**
     * Clear Dalvik/ART dex cache for a specific app.
     * Forces recompilation on next launch.
     */
    suspend fun clearDexCache(packageName: String): CleanupResult {
        // ART profiles and compiled code
        val profileResult = shellExecutor.executeRoot(
            "find /data/dalvik-cache -name '*${packageName.replace('.', '@')}*' -delete"
        )

        // Also clear app's oat directory
        val oatResult = shellExecutor.executeRoot(
            "rm -rf /data/app/*${packageName}*/oat"
        )

        val success = profileResult.success || oatResult.success
        Timber.d("$TAG Cleared dex cache for $packageName: success=$success")
        return CleanupResult(packageName, 0, success, if (success) "Dex cache cleared" else "Failed")
    }

    /**
     * Clear ALL Dalvik/ART cache system-wide.
     * This is aggressive — forces recompilation of all apps on next boot.
     */
    suspend fun clearAllDalvikCache(): CleanupResult {
        var totalFreed = 0L

        for (path in SYSTEM_CACHE_PATHS) {
            val size = getDirectorySize(path)
            val result = shellExecutor.executeRoot("rm -rf $path/*")
            if (result.success) totalFreed += size
        }

        Timber.d("$TAG Cleared all Dalvik/ART cache: freed ${totalFreed / 1024 / 1024}MB")
        return CleanupResult(null, totalFreed, true, "All Dalvik/ART cache cleared")
    }

    /**
     * Batch clear cache for multiple apps.
     */
    suspend fun batchClearCache(packageNames: List<String>): BatchCleanupResult {
        val results = packageNames.map { clearAppCache(it) }
        val totalFreed = results.sumOf { it.bytesFreed }
        return BatchCleanupResult(results, totalFreed)
    }

    /**
     * Get total cache size for an app.
     */
    suspend fun getAppCacheSize(packageName: String): Long {
        return getDirectorySize("/data/data/$packageName/cache") +
            getDirectorySize("/data/data/$packageName/code_cache")
    }

    /**
     * Trigger ART background dex optimization (bg-dexopt-job).
     */
    suspend fun triggerDexOptimization(): Boolean {
        val result = shellExecutor.executeRoot("cmd package bg-dexopt-job")
        return result.success
    }

    /**
     * Force compile a specific app with speed profile.
     */
    suspend fun compileApp(packageName: String, mode: String = "speed-profile"): Boolean {
        val result = shellExecutor.executeRoot("cmd package compile -m $mode -f $packageName")
        return result.success
    }

    private suspend fun getDirectorySize(path: String): Long {
        val result = shellExecutor.executeRoot("du -sb $path 2>/dev/null")
        return if (result.success) {
            result.stdout.trim().split("\\s+".toRegex()).firstOrNull()?.toLongOrNull() ?: 0L
        } else 0L
    }
}
