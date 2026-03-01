package com.obsidianbackup.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.obsidianbackup.automation.ScheduleManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Boot receiver to re-register scheduled backups after device reboot.
 * Both Swift Backup and Titanium Backup use BOOT_COMPLETED to persist schedules.
 *
 * Must be registered in AndroidManifest.xml with:
 * <receiver android:name=".receiver.BootReceiver"
 *     android:exported="false"
 *     android:enabled="true">
 *     <intent-filter>
 *         <action android:name="android.intent.action.BOOT_COMPLETED" />
 *         <action android:name="android.intent.action.QUICKBOOT_POWERON" />
 *     </intent-filter>
 * </receiver>
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduleManager: ScheduleManager

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON") {
            return
        }

        Timber.i("$TAG Boot completed — re-arming backup schedules")

        try {
            // WorkManager automatically re-schedules periodic work after reboot
            // if the work was previously enqueued. However, we ensure it's initialized.
            WorkManager.getInstance(context)

            // Re-arm any schedules that might need manual re-registration
            scheduleManager.rescheduleAllOnBoot()

            Timber.i("$TAG Backup schedules re-armed successfully")
        } catch (e: Exception) {
            Timber.e(e, "$TAG Failed to re-arm schedules on boot")
        }
    }
}
