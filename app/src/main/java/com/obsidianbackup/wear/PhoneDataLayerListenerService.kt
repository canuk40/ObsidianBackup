package com.obsidianbackup.wear

import android.util.Log
import com.google.android.gms.wearable.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Listens for messages from Wear OS watches
 */
@AndroidEntryPoint
class PhoneDataLayerListenerService : WearableListenerService() {

    @Inject
    lateinit var phoneDataLayerRepository: PhoneDataLayerRepository

    @Inject
    lateinit var workManager: androidx.work.WorkManager

    @Inject
    lateinit var backupCatalog: com.obsidianbackup.storage.BackupCatalog
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "Message received from watch: ${messageEvent.path}")
        
        when (messageEvent.path) {
            "/backup_trigger" -> {
                val message = String(messageEvent.data)
                handleBackupTrigger(message)
            }
            "/backup_status" -> {
                handleStatusRequest()
            }
        }
    }

    private fun handleBackupTrigger(message: String) {
        Log.d(TAG, "Backup trigger from watch: $message")
        
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.obsidianbackup.work.BackupWorker>()
            .addTag("watch_triggered")
            .build()
        
        workManager.enqueue(workRequest)
        
        // Launch coroutine for async work
        serviceScope.launch {
            try {
                phoneDataLayerRepository.sendBackupStatus(
                    BackupStatusData(
                        isRunning = true,
                        lastBackupTime = System.currentTimeMillis(),
                        lastBackupSuccess = false,
                        status = "Backup started from watch"
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send backup status", e)
            }
        }
    }

    private fun handleStatusRequest() {
        Log.d(TAG, "Status request from watch")
        
        // Launch coroutine for async work
        serviceScope.launch {
            try {
                val allBackups = backupCatalog.getAllBackupsSync()
                val latestBackup = allBackups.maxByOrNull { snapshot -> snapshot.timestamp }
                
                val status = BackupStatusData(
                    isRunning = false,
                    lastBackupTime = latestBackup?.timestamp ?: 0L,
                    lastBackupSuccess = latestBackup?.verified ?: false,
                    totalBackups = allBackups.size,
                    backupSizeMB = latestBackup?.totalSize?.div(1_048_576f) ?: 0f
                )
                
                phoneDataLayerRepository.sendBackupStatus(status)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to query backup status", e)
            }
        }
    }

    companion object {
        private const val TAG = "PhoneDataLayerListener"
    }
}
