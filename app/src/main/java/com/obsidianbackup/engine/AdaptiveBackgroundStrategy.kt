// engine/AdaptiveBackgroundStrategy.kt
package com.obsidianbackup.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

class AdaptiveBackgroundStrategy(
    private val context: Context,
    private val sdkVersion: Int
) {
    fun getOptimalStrategy(): BackgroundStrategy {
        return when {
            sdkVersion >= 35 -> {
                BackgroundStrategy.ForegroundService(
                    notification = createPersistentNotification()
                )
            }
            sdkVersion >= 31 -> {
                BackgroundStrategy.WorkManager(
                    expedited = true
                )
            }
            else -> {
                BackgroundStrategy.WorkManager(
                    expedited = false
                )
            }
        }
    }

    private fun createPersistentNotification(): android.app.Notification {
        val channelId = "obsidian_backup_service"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Backup Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent notification for backup operations"
                setShowBadge(false)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        
        return NotificationCompat.Builder(context, channelId)
            .setContentTitle("ObsidianBackup Service")
            .setContentText("Backup service is running")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}

sealed class BackgroundStrategy {
    data class ForegroundService(val notification: android.app.Notification) : BackgroundStrategy()
    data class WorkManager(val expedited: Boolean) : BackgroundStrategy()
}
