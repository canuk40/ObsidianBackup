// widget/BackupWidget.kt
package com.obsidianbackup.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.obsidianbackup.R
import com.obsidianbackup.MainActivity

/**
 * Quick Backup Widget
 * Provides one-tap backup from home screen
 */
class BackupWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_backup)

        // Quick backup button
        val backupIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.obsidianbackup.ACTION_QUICK_BACKUP"
            putExtra("EXTRA_WIDGET_ACTION", "quick_backup")
        }
        val backupPendingIntent = PendingIntent.getActivity(
            context,
            0,
            backupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.backup_button, backupPendingIntent)

        // Open app button — tapping widget body opens app
        val openIntent = Intent(context, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            context,
            1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, openPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

/**
 * Backup Status Widget
 * Shows last backup time and status
 */
class BackupStatusWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_backup_status)

        // Update status from shared preferences or database
        val prefs = context.getSharedPreferences("backup_status", Context.MODE_PRIVATE)
        val lastBackup = prefs.getString("last_backup_time", "Never")
        val backupCount = prefs.getInt("total_backups", 0)

        views.setTextViewText(R.id.last_backup_text, "Last backup: $lastBackup")
        views.setTextViewText(R.id.backup_count_text, "$backupCount backups")

        // Click to open app
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        fun updateWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, BackupStatusWidget::class.java)
            )
            
            val intent = Intent(context, BackupStatusWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
