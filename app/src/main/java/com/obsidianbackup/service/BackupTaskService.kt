package com.obsidianbackup.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.obsidianbackup.domain.backup.BackupOrchestrator
import com.obsidianbackup.domain.backup.BackupRequest
import com.obsidianbackup.logging.ObsidianLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject

/**
 * Centralized backup task manager and foreground service.
 * Modeled after Swift Backup's TaskManager + TaskService pattern:
 * - Sequential task execution in a foreground service
 * - WakeLock lifecycle (30-min timeout)
 * - Cancel from notification
 * - Progress via StateFlow
 * - Mutex to prevent concurrent execution
 */
@AndroidEntryPoint
class BackupTaskService : Service() {

    companion object {
        private const val TAG = "BackupTaskService"
        private const val CHANNEL_ID = "obsidian_backup_tasks"
        private const val NOTIFICATION_ID = 1001
        private const val WAKELOCK_TAG = "ObsidianBackup:BackupTask"
        private const val WAKELOCK_TIMEOUT = 30L * 60 * 1000 // 30 minutes

        const val ACTION_START = "com.obsidianbackup.action.START_BACKUP"
        const val ACTION_CANCEL = "com.obsidianbackup.action.CANCEL_BACKUP"

        fun startBackup(context: Context) {
            val intent = Intent(context, BackupTaskService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancel(context: Context) {
            val intent = Intent(context, BackupTaskService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }
    }

    @Inject lateinit var backupOrchestrator: BackupOrchestrator
    @Inject lateinit var logger: ObsidianLogger

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val taskQueue = CopyOnWriteArrayList<BackupTask>()
    private val mutex = Mutex()
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentJob: Job? = null

    private val _taskState = MutableStateFlow(TaskState())
    val taskState: StateFlow<TaskState> = _taskState.asStateFlow()

    data class BackupTask(
        val id: String,
        val request: BackupRequest,
        val label: String = ""
    )

    data class TaskState(
        val isRunning: Boolean = false,
        val currentTaskIndex: Int = 0,
        val totalTasks: Int = 0,
        val currentApp: String = "",
        val progress: Float = 0f,
        val error: String? = null
    )

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                cancelAll()
                stopSelf()
            }
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification("Preparing backup..."))
                acquireWakeLock()
                executeTasks()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun enqueue(task: BackupTask) {
        taskQueue.add(task)
        Timber.d("$TAG Enqueued task: ${task.id}")
    }

    fun enqueueAll(tasks: List<BackupTask>) {
        taskQueue.addAll(tasks)
        Timber.d("$TAG Enqueued ${tasks.size} tasks")
    }

    private fun executeTasks() {
        currentJob = serviceScope.launch {
            mutex.withLock {
                _taskState.value = TaskState(
                    isRunning = true,
                    totalTasks = taskQueue.size
                )

                var index = 0
                for (task in taskQueue) {
                    if (!isActive) break

                    _taskState.value = _taskState.value.copy(
                        currentTaskIndex = index + 1,
                        currentApp = task.label.ifEmpty { task.id },
                        progress = index.toFloat() / taskQueue.size
                    )

                    updateNotification("Backing up ${task.label.ifEmpty { task.id }} (${index + 1}/${taskQueue.size})")

                    try {
                        backupOrchestrator.executeBackup(task.request)
                        Timber.i("$TAG Task completed: ${task.id}")
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.e(e, "$TAG Task failed: ${task.id}")
                        logger.e(TAG, "Backup task failed: ${task.id}", e)
                    }

                    index++
                }

                _taskState.value = TaskState(
                    isRunning = false,
                    totalTasks = taskQueue.size,
                    currentTaskIndex = taskQueue.size,
                    progress = 1f
                )

                taskQueue.clear()
            }

            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun cancelAll() {
        currentJob?.cancel()
        taskQueue.clear()
        _taskState.value = TaskState(isRunning = false, error = "Cancelled by user")
        releaseWakeLock()
        Timber.i("$TAG All tasks cancelled")
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG).apply {
            acquire(WAKELOCK_TIMEOUT)
        }
        Timber.d("$TAG WakeLock acquired")
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Backup Tasks",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing backup operation notifications"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        val cancelIntent = Intent(this, BackupTaskService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPending = PendingIntent.getService(
            this, 0, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ObsidianBackup")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPending)
            .setProgress(100, 0, true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, createNotification(text))
    }

    override fun onDestroy() {
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }
}
