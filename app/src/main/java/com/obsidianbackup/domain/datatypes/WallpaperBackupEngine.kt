package com.obsidianbackup.domain.datatypes

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import com.obsidianbackup.rootcore.shell.ShellExecutor
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wallpaper backup and restore engine.
 *
 * Root approach: copies wallpaper files directly from /data/system/users/0/
 * Non-root fallback: uses WallpaperManager API (limited — can't get lock screen wallpaper).
 *
 * Wallpaper locations:
 *   /data/system/users/0/wallpaper           — home screen wallpaper
 *   /data/system/users/0/wallpaper_lock      — lock screen wallpaper (Android 7+)
 *   /data/system/users/0/wallpaper_info.xml  — wallpaper metadata
 */
@Singleton
class WallpaperBackupEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shellExecutor: ShellExecutor
) {
    companion object {
        private const val TAG = "[WallpaperBackup]"
        private const val SYSTEM_WALLPAPER_DIR = "/data/system/users/0"
        private const val HOME_WALLPAPER = "wallpaper"
        private const val LOCK_WALLPAPER = "wallpaper_lock"
        private const val WALLPAPER_INFO = "wallpaper_info.xml"
    }

    data class WallpaperBackupResult(
        val homeWallpaperBacked: Boolean = false,
        val lockWallpaperBacked: Boolean = false,
        val infoFileBacked: Boolean = false
    )

    /**
     * Backup wallpapers by copying system files to output directory.
     */
    suspend fun backup(outputDir: File): Result<WallpaperBackupResult> = runCatching {
        outputDir.mkdirs()
        var result = WallpaperBackupResult()

        // Home wallpaper
        val homeResult = copyWallpaperFile(
            "$SYSTEM_WALLPAPER_DIR/$HOME_WALLPAPER",
            File(outputDir, HOME_WALLPAPER)
        )
        if (homeResult) result = result.copy(homeWallpaperBacked = true)

        // Lock screen wallpaper (Android 7+)
        val lockResult = copyWallpaperFile(
            "$SYSTEM_WALLPAPER_DIR/$LOCK_WALLPAPER",
            File(outputDir, LOCK_WALLPAPER)
        )
        if (lockResult) result = result.copy(lockWallpaperBacked = true)

        // Wallpaper info metadata
        val infoResult = copyWallpaperFile(
            "$SYSTEM_WALLPAPER_DIR/$WALLPAPER_INFO",
            File(outputDir, WALLPAPER_INFO)
        )
        if (infoResult) result = result.copy(infoFileBacked = true)

        Timber.d("$TAG Backup result: home=${result.homeWallpaperBacked}, lock=${result.lockWallpaperBacked}")
        result
    }

    /**
     * Restore wallpapers from backup directory.
     * Uses root to copy files back and set proper permissions.
     */
    suspend fun restore(inputDir: File): Result<WallpaperBackupResult> = runCatching {
        var result = WallpaperBackupResult()

        // Restore home wallpaper
        val homeFile = File(inputDir, HOME_WALLPAPER)
        if (homeFile.exists()) {
            val restored = restoreWallpaperFile(homeFile, "$SYSTEM_WALLPAPER_DIR/$HOME_WALLPAPER")
            if (restored) {
                result = result.copy(homeWallpaperBacked = true)
                // Also set via WallpaperManager for immediate effect
                try {
                    val wm = WallpaperManager.getInstance(context)
                    FileInputStream(homeFile).use { wm.setStream(it) }
                } catch (e: Exception) {
                    Timber.w(e, "$TAG WallpaperManager.setStream fallback failed")
                }
            }
        }

        // Restore lock screen wallpaper
        val lockFile = File(inputDir, LOCK_WALLPAPER)
        if (lockFile.exists()) {
            val restored = restoreWallpaperFile(lockFile, "$SYSTEM_WALLPAPER_DIR/$LOCK_WALLPAPER")
            if (restored) result = result.copy(lockWallpaperBacked = true)
        }

        // Restore wallpaper info
        val infoFile = File(inputDir, WALLPAPER_INFO)
        if (infoFile.exists()) {
            val restored = restoreWallpaperFile(infoFile, "$SYSTEM_WALLPAPER_DIR/$WALLPAPER_INFO")
            if (restored) result = result.copy(infoFileBacked = true)
        }

        Timber.d("$TAG Restore result: home=${result.homeWallpaperBacked}, lock=${result.lockWallpaperBacked}")
        result
    }

    private suspend fun copyWallpaperFile(sourcePath: String, destFile: File): Boolean {
        val check = shellExecutor.executeRoot("test -f $sourcePath && echo EXISTS")
        if (!check.success || !check.stdout.contains("EXISTS")) return false

        val result = shellExecutor.executeRoot("cp $sourcePath ${destFile.absolutePath}")
        if (result.success) {
            // Make readable by app
            shellExecutor.executeRoot("chmod 644 ${destFile.absolutePath}")
            return true
        }
        return false
    }

    private suspend fun restoreWallpaperFile(sourceFile: File, destPath: String): Boolean {
        val tmpPath = "/data/local/tmp/wallpaper_${System.currentTimeMillis()}"
        shellExecutor.executeRoot("cp ${sourceFile.absolutePath} $tmpPath")
        val result = shellExecutor.executeRoot("cp $tmpPath $destPath && chmod 600 $destPath && chown system:system $destPath")
        shellExecutor.executeRoot("rm -f $tmpPath")
        return result.success
    }
}
