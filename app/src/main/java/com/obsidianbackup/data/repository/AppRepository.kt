// data/repository/AppRepository.kt
package com.obsidianbackup.data.repository

import android.graphics.Bitmap
import com.obsidianbackup.model.AppId
import com.obsidianbackup.model.AppInfo
import com.obsidianbackup.scanner.AppScanner

class AppRepository(
    private val appScanner: AppScanner
) {
    suspend fun scanInstalledApps(includeSystemApps: Boolean = false): List<AppInfo> {
        return appScanner.scanInstalledApps(includeSystemApps)
    }

    suspend fun getAppInfo(packageName: String): AppInfo? {
        return appScanner.getAppInfo(packageName)
    }

    suspend fun loadAppIcon(packageName: String): Bitmap? {
        return appScanner.loadAppIcon(packageName)
    }

    fun getApkPath(packageName: String): String? {
        return appScanner.getApkPath(packageName)
    }

    fun getDataPath(packageName: String): String? {
        return appScanner.getDataPath(packageName)
    }

    fun hasObbFiles(packageName: String): Boolean {
        return appScanner.hasObbFiles(packageName)
    }

    fun hasExternalData(packageName: String): Boolean {
        return appScanner.hasExternalData(packageName)
    }

    fun getObbSize(packageName: String): Long {
        return appScanner.getObbSize(packageName)
    }

    fun getExternalDataSize(packageName: String): Long {
        return appScanner.getExternalDataSize(packageName)
    }
}
