package com.obsidianbackup.tv.backup

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Environment
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TVBackupManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager = context.packageManager
    private val selectedApps = mutableSetOf<String>()

    data class TVApp(
        val name: String,
        val packageName: String,
        val icon: Drawable?,
        val size: Long,
        val isBackedUp: Boolean,
        val category: TVAppCategory
    )

    enum class TVAppCategory {
        TV_APP,
        STREAMING,
        GAME,
        OTHER
    }

    fun getTVApps(): List<TVApp> {
        return getInstalledApps().filter { it.category == TVAppCategory.TV_APP }
    }

    fun getStreamingApps(): List<TVApp> {
        val streamingPackages = setOf(
            "com.netflix.ninja",
            "com.google.android.youtube.tv",
            "com.amazon.amazonvideo.livingroom",
            "com.hulu.livingroomplus",
            "com.disney.disneyplus",
            "com.hbo.hbonow",
            "com.spotify.tv.android",
            "com.plexapp.android",
            "com.apple.atve.androidtv.appletv"
        )
        
        return getInstalledApps().filter { 
            it.packageName in streamingPackages || it.category == TVAppCategory.STREAMING 
        }
    }

    fun getTVGames(): List<TVApp> {
        return getInstalledApps().filter { it.category == TVAppCategory.GAME }
    }

    fun getAllTVApps(): List<TVApp> {
        return getInstalledApps()
    }

    private fun getInstalledApps(): List<TVApp> {
        val apps = mutableListOf<TVApp>()
        
        try {
            val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            
            for (appInfo in packages) {
                // Skip system apps unless they're popular TV apps
                if (isSystemApp(appInfo) && !isPopularTVApp(appInfo.packageName)) {
                    continue
                }

                try {
                    val name = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(appInfo)
                    val size = getAppSize(appInfo)
                    val category = categorizeApp(appInfo)
                    val isBackedUp = isAppBackedUp(appInfo.packageName)

                    apps.add(TVApp(
                        name = name,
                        packageName = appInfo.packageName,
                        icon = icon,
                        size = size,
                        isBackedUp = isBackedUp,
                        category = category
                    ))
                } catch (e: Exception) {
                    // Skip apps that cause errors
                    continue
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return apps.sortedBy { it.name }
    }

    private fun categorizeApp(appInfo: ApplicationInfo): TVAppCategory {
        val packageName = appInfo.packageName
        
        // Check for streaming apps
        val streamingKeywords = listOf("video", "tv", "netflix", "youtube", "hulu", "disney", "stream")
        if (streamingKeywords.any { packageName.contains(it, ignoreCase = true) }) {
            return TVAppCategory.STREAMING
        }

        // Check for games
        if ((appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0) {
            return TVAppCategory.GAME
        }

        // Check if it's a TV app using leanback feature
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_CONFIGURATIONS)
            if (packageInfo.reqFeatures?.any { it.name == "android.software.leanback" } == true) {
                return TVAppCategory.TV_APP
            }
        } catch (e: Exception) {
            // Continue
        }

        return TVAppCategory.OTHER
    }

    private fun isSystemApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    }

    private fun isPopularTVApp(packageName: String): Boolean {
        val popularApps = setOf(
            "com.google.android.youtube.tv",
            "com.netflix.ninja",
            "com.amazon.amazonvideo.livingroom",
            "com.google.android.videos",
            "com.google.android.tvlauncher"
        )
        return packageName in popularApps
    }

    private fun getAppSize(appInfo: ApplicationInfo): Long {
        var size = 0L
        try {
            val sourceDir = File(appInfo.sourceDir)
            size = sourceDir.length()
            
            // Add data dir size if accessible
            appInfo.dataDir?.let { dataPath ->
                size += getFolderSize(File(dataPath))
            }
        } catch (e: Exception) {
            // Return 0 if we can't determine size
        }
        return size
    }

    private fun getFolderSize(folder: File): Long {
        var size = 0L
        try {
            if (folder.isDirectory) {
                folder.listFiles()?.forEach { file ->
                    size += if (file.isDirectory) {
                        getFolderSize(file)
                    } else {
                        file.length()
                    }
                }
            } else {
                size = folder.length()
            }
        } catch (e: Exception) {
            // Return current size if we hit an error
        }
        return size
    }

    private fun isAppBackedUp(packageName: String): Boolean {
        val backupDir = File(context.getExternalFilesDir(null), "backups/$packageName")
        return backupDir.exists() && backupDir.listFiles()?.isNotEmpty() == true
    }

    fun startBackup() {
        // Implement backup logic
        // This would integrate with the main app's backup engine
    }

    fun getStorageStatus(): String {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val usedBytes = totalBytes - bytesAvailable
        
        return "${formatBytes(usedBytes)} used / ${formatBytes(totalBytes)} total"
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    fun toggleAppSelection(packageName: String) {
        if (selectedApps.contains(packageName)) {
            selectedApps.remove(packageName)
        } else {
            selectedApps.add(packageName)
        }
    }

    fun getSelectedApps(): Set<String> = selectedApps.toSet()
}
