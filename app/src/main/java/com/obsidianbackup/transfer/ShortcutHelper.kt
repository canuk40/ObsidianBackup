package com.obsidianbackup.transfer

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Home screen shortcut manager for quick backup actions.
 *
 * Creates pinned shortcuts for common operations:
 * - Quick backup all
 * - Backup specific label
 * - Open transfer server
 */
@Singleton
class ShortcutHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "[Shortcuts]"
        const val ACTION_QUICK_BACKUP = "com.obsidianbackup.action.QUICK_BACKUP"
        const val ACTION_BACKUP_LABEL = "com.obsidianbackup.action.BACKUP_LABEL"
        const val ACTION_OPEN_TRANSFER = "com.obsidianbackup.action.OPEN_TRANSFER"
        const val EXTRA_LABEL_ID = "label_id"
    }

    /**
     * Create a pinned shortcut for "Quick Backup All" on home screen.
     */
    fun createQuickBackupShortcut(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false

        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return false
        if (!shortcutManager.isRequestPinShortcutSupported) return false

        val intent = Intent(ACTION_QUICK_BACKUP).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val shortcut = ShortcutInfo.Builder(context, "quick_backup")
            .setShortLabel("Quick Backup")
            .setLongLabel("Backup all apps now")
            .setIcon(Icon.createWithResource(context, android.R.drawable.ic_menu_save))
            .setIntent(intent)
            .build()

        return shortcutManager.requestPinShortcut(shortcut, null)
    }

    /**
     * Create a pinned shortcut to backup a specific label group.
     */
    fun createLabelBackupShortcut(labelId: Long, labelName: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false

        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return false
        if (!shortcutManager.isRequestPinShortcutSupported) return false

        val intent = Intent(ACTION_BACKUP_LABEL).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_LABEL_ID, labelId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val shortcut = ShortcutInfo.Builder(context, "backup_label_$labelId")
            .setShortLabel("Backup: $labelName")
            .setLongLabel("Backup all apps in $labelName")
            .setIcon(Icon.createWithResource(context, android.R.drawable.ic_menu_save))
            .setIntent(intent)
            .build()

        return shortcutManager.requestPinShortcut(shortcut, null)
    }

    /**
     * Create a pinned shortcut to open the Wi-Fi transfer server.
     */
    fun createTransferShortcut(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false

        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return false
        if (!shortcutManager.isRequestPinShortcutSupported) return false

        val intent = Intent(ACTION_OPEN_TRANSFER).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val shortcut = ShortcutInfo.Builder(context, "wifi_transfer")
            .setShortLabel("Transfer")
            .setLongLabel("Start Wi-Fi Transfer Server")
            .setIcon(Icon.createWithResource(context, android.R.drawable.ic_menu_share))
            .setIntent(intent)
            .build()

        return shortcutManager.requestPinShortcut(shortcut, null)
    }

    /**
     * Register dynamic shortcuts (shown in long-press app icon menu).
     */
    fun updateDynamicShortcuts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return

        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return

        val shortcuts = listOf(
            ShortcutInfo.Builder(context, "dynamic_backup")
                .setShortLabel("Backup")
                .setLongLabel("Quick Backup All")
                .setIcon(Icon.createWithResource(context, android.R.drawable.ic_menu_save))
                .setIntent(Intent(ACTION_QUICK_BACKUP).apply {
                    setPackage(context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                .setRank(0)
                .build(),
            ShortcutInfo.Builder(context, "dynamic_transfer")
                .setShortLabel("Transfer")
                .setLongLabel("Wi-Fi Transfer")
                .setIcon(Icon.createWithResource(context, android.R.drawable.ic_menu_share))
                .setIntent(Intent(ACTION_OPEN_TRANSFER).apply {
                    setPackage(context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                .setRank(1)
                .build()
        )

        try {
            shortcutManager.dynamicShortcuts = shortcuts
            Timber.d("$TAG Updated ${shortcuts.size} dynamic shortcuts")
        } catch (e: Exception) {
            Timber.w(e, "$TAG Failed to update shortcuts")
        }
    }
}
