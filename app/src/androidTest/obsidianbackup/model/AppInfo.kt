// model/AppInfo.kt
package com.titanbackup.model

data class AppInfo(
    val appId: AppId,
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val dataSize: Long,
    val apkSize: Long,
    val lastUpdateTime: Long,
    val icon: Any? = null // Bitmap or ImageVector
)