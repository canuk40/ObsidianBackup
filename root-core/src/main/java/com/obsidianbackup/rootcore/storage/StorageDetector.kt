package com.obsidianbackup.rootcore.storage

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Storage detection using 3-tier strategy (from TWRP/Magisk patterns):
 * Tier 1: Environment variables (EXTERNAL_STORAGE, SECONDARY_STORAGE)
 * Tier 2: Android API (Environment.getExternalStorageDirectory, getExternalFilesDirs)
 * Tier 3: Hardcoded paths (common mount points)
 */
@Singleton
class StorageDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "StorageDetector"

        private val HARDCODED_STORAGE_PATHS = listOf(
            "/storage/emulated/0",
            "/storage/sdcard0",
            "/storage/sdcard1",
            "/mnt/sdcard",
            "/mnt/extsd",
            "/sdcard",
            "/storage/usbdisk",
            "/mnt/usb_storage"
        )
    }

    data class StorageInfo(
        val path: String,
        val totalBytes: Long,
        val availableBytes: Long,
        val isRemovable: Boolean,
        val isEmulated: Boolean,
        val source: String
    ) {
        val usedBytes: Long get() = totalBytes - availableBytes
        val usagePercent: Float get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes) * 100f else 0f
        val availableGb: Float get() = availableBytes / (1024f * 1024f * 1024f)
        val totalGb: Float get() = totalBytes / (1024f * 1024f * 1024f)
    }

    /**
     * Detect all available storage locations.
     * Uses 3-tier strategy for comprehensive detection.
     */
    fun detectAllStorage(): List<StorageInfo> {
        val results = mutableMapOf<String, StorageInfo>()

        // Tier 1: Environment variables
        detectFromEnvironment().forEach { results[it.path] = it }

        // Tier 2: Android API
        detectFromApi().forEach { results[it.path] = it }

        // Tier 3: Hardcoded paths
        detectFromHardcodedPaths().forEach { results.putIfAbsent(it.path, it) }

        val allStorage = results.values.toList()
        Timber.i("$TAG Detected ${allStorage.size} storage locations")
        return allStorage
    }

    /**
     * Get the primary storage location suitable for backups.
     */
    fun getPrimaryStorage(): StorageInfo? {
        return detectAllStorage()
            .filter { it.availableBytes > 100 * 1024 * 1024 } // At least 100MB free
            .maxByOrNull { it.availableBytes }
    }

    /**
     * Check if there's enough space for a backup of the given size.
     */
    fun hasEnoughSpace(requiredBytes: Long, storagePath: String? = null): Boolean {
        val storage = if (storagePath != null) {
            getStorageInfo(storagePath)
        } else {
            getPrimaryStorage()
        }
        return storage != null && storage.availableBytes >= requiredBytes
    }

    /**
     * Get storage info for a specific path.
     */
    fun getStorageInfo(path: String): StorageInfo? {
        return try {
            val statFs = StatFs(path)
            StorageInfo(
                path = path,
                totalBytes = statFs.totalBytes,
                availableBytes = statFs.availableBytes,
                isRemovable = false,
                isEmulated = path.contains("emulated"),
                source = "direct"
            )
        } catch (e: Exception) {
            Timber.w(e, "$TAG Failed to get storage info for: $path")
            null
        }
    }

    private fun detectFromEnvironment(): List<StorageInfo> {
        val results = mutableListOf<StorageInfo>()

        System.getenv("EXTERNAL_STORAGE")?.let { path ->
            createStorageInfo(path, isRemovable = false, isEmulated = true, source = "env:EXTERNAL_STORAGE")
                ?.let { results.add(it) }
        }

        System.getenv("SECONDARY_STORAGE")?.let { paths ->
            paths.split(":").forEach { path ->
                if (path.isNotBlank()) {
                    createStorageInfo(path, isRemovable = true, isEmulated = false, source = "env:SECONDARY_STORAGE")
                        ?.let { results.add(it) }
                }
            }
        }

        return results
    }

    private fun detectFromApi(): List<StorageInfo> {
        val results = mutableListOf<StorageInfo>()

        // Primary external storage
        @Suppress("DEPRECATION")
        val primaryPath = Environment.getExternalStorageDirectory().absolutePath
        createStorageInfo(primaryPath, isRemovable = false, isEmulated = Environment.isExternalStorageEmulated(), source = "api:primary")
            ?.let { results.add(it) }

        // All external storage locations (includes SD cards)
        val externalDirs = context.getExternalFilesDirs(null)
        externalDirs.forEachIndexed { index, dir ->
            if (dir != null) {
                // Resolve to root of that storage volume
                val storagePath = resolveStorageRoot(dir.absolutePath)
                val isRemovable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Environment.isExternalStorageRemovable(dir)
                } else {
                    index > 0
                }
                createStorageInfo(storagePath, isRemovable = isRemovable, isEmulated = !isRemovable, source = "api:external[$index]")
                    ?.let { results.add(it) }
            }
        }

        return results
    }

    private fun detectFromHardcodedPaths(): List<StorageInfo> {
        return HARDCODED_STORAGE_PATHS.mapNotNull { path ->
            val file = File(path)
            if (file.exists() && file.isDirectory && file.canRead()) {
                createStorageInfo(path, isRemovable = path.contains("sdcard1") || path.contains("usb"),
                    isEmulated = path.contains("emulated"), source = "hardcoded")
            } else null
        }
    }

    private fun createStorageInfo(path: String, isRemovable: Boolean, isEmulated: Boolean, source: String): StorageInfo? {
        return try {
            val file = File(path)
            if (!file.exists()) return null
            val statFs = StatFs(path)
            StorageInfo(
                path = path,
                totalBytes = statFs.totalBytes,
                availableBytes = statFs.availableBytes,
                isRemovable = isRemovable,
                isEmulated = isEmulated,
                source = source
            )
        } catch (e: Exception) {
            Timber.w(e, "$TAG Failed to stat: $path")
            null
        }
    }

    private fun resolveStorageRoot(appSpecificPath: String): String {
        // App-specific path looks like /storage/XXXX-XXXX/Android/data/com.obsidianbackup/files
        // We want /storage/XXXX-XXXX
        val androidIndex = appSpecificPath.indexOf("/Android/")
        return if (androidIndex > 0) {
            appSpecificPath.substring(0, androidIndex)
        } else {
            appSpecificPath
        }
    }
}
