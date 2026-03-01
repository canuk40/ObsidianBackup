// scanner/AppScanner.kt
package com.obsidianbackup.scanner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.obsidianbackup.model.AppId
import com.obsidianbackup.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AppScanner(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    
    /**
     * Get the package manager instance
     */
    fun getPackageManager(): PackageManager = packageManager

    /**
     * Scan all installed applications
     */
    suspend fun scanInstalledApps(
        includeSystemApps: Boolean = false
    ): List<AppInfo> = withContext(Dispatchers.IO) {
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        packages.mapNotNull { appInfo ->
            try {
                val packageInfo = packageManager.getPackageInfo(appInfo.packageName, 0)
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                if (!includeSystemApps && isSystemApp) {
                    return@mapNotNull null
                }

                val dataDir = appInfo.dataDir?.let { File(it) }
                val dataSize = dataDir?.let { calculateDirectorySize(it) } ?: 0L

                val apkFile = File(appInfo.sourceDir)
                val apkSize = if (apkFile.exists()) apkFile.length() else 0L
                
                // Check for split APKs
                val splitDirs = appInfo.splitSourceDirs
                val hasSplitApks = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    splitDirs != null && splitDirs.isNotEmpty()
                } else {
                    false
                }
                
                // Calculate total APK size including splits
                val totalApkSize = if (hasSplitApks && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    val splitSizes = splitDirs?.sumOf { path ->
                        File(path).length()
                    } ?: 0L
                    apkSize + splitSizes
                } else {
                    apkSize
                }

                AppInfo(
                    appId = AppId(appInfo.packageName),
                    packageName = appInfo.packageName,
                    appName = appInfo.loadLabel(packageManager).toString(),
                    versionName = packageInfo.versionName ?: "Unknown",
                    versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toLong()
                    },
                    isSystemApp = isSystemApp,
                    isUpdatedSystemApp = isUpdatedSystemApp,
                    dataSize = dataSize,
                    apkSize = totalApkSize,
                    lastUpdateTime = packageInfo.lastUpdateTime,
                    icon = null // Will be loaded separately if needed
                )
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.appName.lowercase() }
    }

    /**
     * Get detailed info for a specific app
     */
    suspend fun getAppInfo(packageName: String): AppInfo? = withContext(Dispatchers.IO) {
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            val dataDir = appInfo.dataDir?.let { File(it) }
            val dataSize = dataDir?.let { calculateDirectorySize(it) } ?: 0L

            val apkFile = File(appInfo.sourceDir)
            val apkSize = if (apkFile.exists()) apkFile.length() else 0L
            
            // Check for split APKs and calculate total size
            val splitDirs = appInfo.splitSourceDirs
            val totalApkSize = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && 
                splitDirs != null) {
                val splitSizes: Long = splitDirs.sumOf { path: String ->
                    File(path).length()
                }
                apkSize + splitSizes
            } else {
                apkSize
            }

            AppInfo(
                appId = AppId(appInfo.packageName),
                packageName = appInfo.packageName,
                appName = appInfo.loadLabel(packageManager).toString(),
                versionName = packageInfo.versionName ?: "Unknown",
                versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                    },
                isSystemApp = isSystemApp,
                isUpdatedSystemApp = isUpdatedSystemApp,
                dataSize = dataSize,
                apkSize = totalApkSize,
                lastUpdateTime = packageInfo.lastUpdateTime,
                icon = null
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Load app icon with optimized memory usage
     * Downsamples to 128x128 and uses RGB_565 for 75% memory reduction
     */
    suspend fun loadAppIcon(packageName: String, maxSize: Int = 128): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val drawable = packageManager.getApplicationIcon(packageName)
            drawableToBitmap(drawable, maxSize)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get APK file path
     */
    fun getApkPath(packageName: String): String? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            appInfo.sourceDir
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get data directory path
     */
    fun getDataPath(packageName: String): String? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            appInfo.dataDir
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if app has OBB files
     */
    fun hasObbFiles(packageName: String): Boolean {
        val obbDir = File("/storage/emulated/0/Android/obb/$packageName")
        return obbDir.exists() && obbDir.listFiles()?.isNotEmpty() == true
    }

    /**
     * Check if app has external data
     */
    fun hasExternalData(packageName: String): Boolean {
        val externalDir = File("/storage/emulated/0/Android/data/$packageName")
        return externalDir.exists() && externalDir.listFiles()?.isNotEmpty() == true
    }
    
    /**
     * Check if an app uses split APKs
     */
    fun hasSplitApks(packageName: String): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val splitDirs = appInfo.splitSourceDirs
                splitDirs != null && splitDirs.isNotEmpty()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get number of split APKs for an app
     */
    fun getSplitCount(packageName: String): Int {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                appInfo.splitSourceDirs?.size ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Get split APK names for an app
     */
    fun getSplitNames(packageName: String): List<String> {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                appInfo.splitNames?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get OBB size
     */
    fun getObbSize(packageName: String): Long {
        val obbDir = File("/storage/emulated/0/Android/obb/$packageName")
        return if (obbDir.exists()) calculateDirectorySize(obbDir) else 0L
    }

    /**
     * Get external data size
     */
    fun getExternalDataSize(packageName: String): Long {
        val externalDir = File("/storage/emulated/0/Android/data/$packageName")
        return if (externalDir.exists()) calculateDirectorySize(externalDir) else 0L
    }

    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        if (directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    calculateDirectorySize(file)
                } else {
                    file.length()
                }
            }
        }
        return size
    }

    private fun drawableToBitmap(drawable: Drawable, maxSize: Int = 128): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            // Downsample if larger than maxSize
            val bitmap = drawable.bitmap
            return if (bitmap.width > maxSize || bitmap.height > maxSize) {
                Bitmap.createScaledBitmap(bitmap, maxSize, maxSize, true)
            } else {
                bitmap
            }
        }

        // For other drawables, create optimized bitmap
        val width = if (drawable.intrinsicWidth > 0) minOf(drawable.intrinsicWidth, maxSize) else maxSize
        val height = if (drawable.intrinsicHeight > 0) minOf(drawable.intrinsicHeight, maxSize) else maxSize
        
        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.RGB_565  // 50% memory vs ARGB_8888
        )

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }
}
