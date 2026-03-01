package com.obsidianbackup.tasker

import android.content.Context
import android.content.Intent
import android.util.Log
import com.obsidianbackup.model.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TaskerEventPublisher - Publishes events to Tasker/MacroDroid
 * 
 * Broadcasts events that can be used as triggers in automation apps:
 * - Backup complete/failed
 * - Restore complete/failed
 * - Cloud sync complete/failed
 * - Verification complete
 */
@Singleton
class TaskerEventPublisher @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "TaskerEventPublisher"
    }
    
    /**
     * Publish backup complete event
     */
    fun publishBackupComplete(result: BackupResult.Success) {
        Log.i(TAG, "Publishing backup complete event")
        
        val intent = Intent(TaskerIntegration.EVENT_BACKUP_COMPLETE).apply {
            putExtra(TaskerIntegration.EXTRA_SNAPSHOT_ID, result.snapshotId.value)
            putExtra(TaskerIntegration.EXTRA_SNAPSHOT_TIMESTAMP, result.timestamp)
            putExtra(TaskerIntegration.EXTRA_APPS_BACKED_UP, result.appsBackedUp.size)
            putExtra(TaskerIntegration.EXTRA_TOTAL_SIZE, result.totalSize)
            putExtra(TaskerIntegration.EXTRA_DURATION, result.duration)
            putExtra(TaskerIntegration.EXTRA_RESULT_CODE, TaskerIntegration.RESULT_SUCCESS)
            putExtra(TaskerIntegration.EXTRA_STATUS, "success")
            
            // Add package names array
            val packageNames = result.appsBackedUp.map { it.value }.toTypedArray()
            putExtra(TaskerIntegration.EXTRA_PACKAGE_LIST, packageNames)
        }
        
        context.sendBroadcast(intent)
        Log.d(TAG, "Backup complete event published for ${result.snapshotId.value}")
    }
    
    /**
     * Publish backup partial success event
     */
    fun publishBackupPartialSuccess(result: BackupResult.PartialSuccess) {
        Log.i(TAG, "Publishing backup partial success event")
        
        val intent = Intent(TaskerIntegration.EVENT_BACKUP_COMPLETE).apply {
            putExtra(TaskerIntegration.EXTRA_SNAPSHOT_ID, result.snapshotId.value)
            putExtra(TaskerIntegration.EXTRA_SNAPSHOT_TIMESTAMP, result.timestamp)
            putExtra(TaskerIntegration.EXTRA_APPS_BACKED_UP, result.appsBackedUp.size)
            putExtra(TaskerIntegration.EXTRA_APPS_FAILED, result.appsFailed.size)
            putExtra(TaskerIntegration.EXTRA_TOTAL_SIZE, result.totalSize)
            putExtra(TaskerIntegration.EXTRA_DURATION, result.duration)
            putExtra(TaskerIntegration.EXTRA_RESULT_CODE, TaskerIntegration.RESULT_PARTIAL_SUCCESS)
            putExtra(TaskerIntegration.EXTRA_STATUS, "partial_success")
            putExtra(TaskerIntegration.EXTRA_MESSAGE, "Some apps failed: ${result.appsFailed.size}")
            
            // Add package names
            val succeededPackages = result.appsBackedUp.map { it.value }.toTypedArray()
            val failedPackages = result.appsFailed.map { it.value }.toTypedArray()
            putExtra(TaskerIntegration.EXTRA_PACKAGE_LIST, succeededPackages)
            putExtra("failed_packages", failedPackages)
        }
        
        context.sendBroadcast(intent)
        Log.d(TAG, "Backup partial success event published")
    }
    
    /**
     * Publish backup failed event
     */
    fun publishBackupFailed(result: BackupResult.Failure) {
        Log.i(TAG, "Publishing backup failed event")
        
        val intent = Intent(TaskerIntegration.EVENT_BACKUP_FAILED).apply {
            putExtra(TaskerIntegration.EXTRA_APPS_FAILED, result.appsFailed.size)
            putExtra(TaskerIntegration.EXTRA_RESULT_CODE, TaskerIntegration.RESULT_FAILURE)
            putExtra(TaskerIntegration.EXTRA_STATUS, "failed")
            putExtra(TaskerIntegration.EXTRA_MESSAGE, result.reason)
            
            val failedPackages = result.appsFailed.map { it.value }.toTypedArray()
            putExtra("failed_packages", failedPackages)
        }
        
        context.sendBroadcast(intent)
        Log.d(TAG, "Backup failed event published: ${result.reason}")
    }
    
    /**
     * Publish restore complete event
     */
    fun publishRestoreComplete(result: RestoreResult.Success, snapshotId: SnapshotId) {
        Log.i(TAG, "Publishing restore complete event")
        
        val intent = Intent(TaskerIntegration.EVENT_RESTORE_COMPLETE).apply {
            putExtra(TaskerIntegration.EXTRA_SNAPSHOT_ID, snapshotId.value)
            putExtra(TaskerIntegration.EXTRA_APPS_BACKED_UP, result.appsRestored.size)
            putExtra(TaskerIntegration.EXTRA_DURATION, result.duration)
            putExtra(TaskerIntegration.EXTRA_RESULT_CODE, TaskerIntegration.RESULT_SUCCESS)
            putExtra(TaskerIntegration.EXTRA_STATUS, "success")
            
            if (result.warnings.isNotEmpty()) {
                putExtra("warnings", result.warnings.toTypedArray())
            }
            
            val packageNames = result.appsRestored.map { it.value }.toTypedArray()
            putExtra(TaskerIntegration.EXTRA_PACKAGE_LIST, packageNames)
        }
        
        context.sendBroadcast(intent)
        Log.d(TAG, "Restore complete event published")
    }
    
    /**
     * Publish restore partial success event
     */
    fun publishRestorePartialSuccess(result: RestoreResult.PartialSuccess, snapshotId: SnapshotId) {
        Log.i(TAG, "Publishing restore partial success event")
        
        val intent = Intent(TaskerIntegration.EVENT_RESTORE_COMPLETE).apply {
            putExtra(TaskerIntegration.EXTRA_SNAPSHOT_ID, snapshotId.value)
            putExtra(TaskerIntegration.EXTRA_APPS_BACKED_UP, result.appsRestored.size)
            putExtra(TaskerIntegration.EXTRA_APPS_FAILED, result.appsFailed.size)
            putExtra(TaskerIntegration.EXTRA_DURATION, result.duration)
            putExtra(TaskerIntegration.EXTRA_RESULT_CODE, TaskerIntegration.RESULT_PARTIAL_SUCCESS)
            putExtra(TaskerIntegration.EXTRA_STATUS, "partial_success")
            
            val succeededPackages = result.appsRestored.map { it.value }.toTypedArray()
            val failedPackages = result.appsFailed.map { it.value }.toTypedArray()
            putExtra(TaskerIntegration.EXTRA_PACKAGE_LIST, succeededPackages)
            putExtra("failed_packages", failedPackages)
        }
        
        context.sendBroadcast(intent)
        Log.d(TAG, "Restore partial success event published")
    }
    
    /**
     * Publish restore failed event
     */
    fun publishRestoreFailed(result: RestoreResult.Failure, snapshotId: SnapshotId) {
        Log.i(TAG, "Publishing restore failed event")
        
        val intent = Intent(TaskerIntegration.EVENT_RESTORE_FAILED).apply {
            putExtra(TaskerIntegration.EXTRA_SNAPSHOT_ID, snapshotId.value)
            putExtra(TaskerIntegration.EXTRA_RESULT_CODE, TaskerIntegration.RESULT_FAILURE)
            putExtra(TaskerIntegration.EXTRA_STATUS, "failed")
            putExtra(TaskerIntegration.EXTRA_MESSAGE, result.reason)
        }
        
        context.sendBroadcast(intent)
        Log.d(TAG, "Restore failed event published: ${result.reason}")
    }
    
    /**
     * Publish cloud sync complete event
     */
    fun publishCloudSyncComplete(snapshotId: String?, bytesUploaded: Long, duration: Long) {
        Log.i(TAG, "Publishing cloud sync complete event")
        
        val intent = Intent(TaskerIntegration.EVENT_SYNC_COMPLETE).apply {
            if (snapshotId != null) {
                putExtra(TaskerIntegration.EXTRA_SNAPSHOT_ID, snapshotId)
            }
            putExtra(TaskerIntegration.EXTRA_TOTAL_SIZE, bytesUploaded)
            putExtra(TaskerIntegration.EXTRA_DURATION, duration)
            putExtra(TaskerIntegration.EXTRA_RESULT_CODE, TaskerIntegration.RESULT_SUCCESS)
            putExtra(TaskerIntegration.EXTRA_STATUS, "success")
        }
        
        context.sendBroadcast(intent)
        Log.d(TAG, "Cloud sync complete event published")
    }
    
    /**
     * Publish cloud sync failed event
     */
    fun publishCloudSyncFailed(snapshotId: String?, reason: String) {
        Log.i(TAG, "Publishing cloud sync failed event")
        
        val intent = Intent(TaskerIntegration.EVENT_SYNC_FAILED).apply {
            if (snapshotId != null) {
                putExtra(TaskerIntegration.EXTRA_SNAPSHOT_ID, snapshotId)
            }
            putExtra(TaskerIntegration.EXTRA_RESULT_CODE, TaskerIntegration.RESULT_FAILURE)
            putExtra(TaskerIntegration.EXTRA_STATUS, "failed")
            putExtra(TaskerIntegration.EXTRA_MESSAGE, reason)
        }
        
        context.sendBroadcast(intent)
        Log.d(TAG, "Cloud sync failed event published: $reason")
    }
    
    /**
     * Publish verification complete event
     */
    fun publishVerificationComplete(result: VerificationResult) {
        Log.i(TAG, "Publishing verification complete event")
        
        val intent = Intent("com.obsidianbackup.tasker.EVENT_VERIFICATION_COMPLETE").apply {
            putExtra(TaskerIntegration.EXTRA_SNAPSHOT_ID, result.snapshotId.value)
            putExtra("files_checked", result.filesChecked)
            putExtra("all_valid", result.allValid)
            putExtra(TaskerIntegration.EXTRA_RESULT_CODE, 
                if (result.allValid) TaskerIntegration.RESULT_SUCCESS 
                else TaskerIntegration.RESULT_FAILURE)
            putExtra(TaskerIntegration.EXTRA_STATUS, 
                if (result.allValid) "verified" else "corrupted")
            
            if (result.corruptedFiles.isNotEmpty()) {
                putExtra("corrupted_files", result.corruptedFiles.toTypedArray())
            }
        }
        
        context.sendBroadcast(intent)
        Log.d(TAG, "Verification complete event published")
    }
    
    /**
     * Publish progress update event (for live tracking in Tasker)
     */
    fun publishProgress(progress: OperationProgress) {
        val intent = Intent("com.obsidianbackup.tasker.EVENT_PROGRESS").apply {
            putExtra("operation_type", progress.operationType.name)
            putExtra("current_item", progress.currentItem)
            putExtra("items_completed", progress.itemsCompleted)
            putExtra("total_items", progress.totalItems)
            putExtra("bytes_processed", progress.bytesProcessed)
            putExtra("total_bytes", progress.totalBytes)
            putExtra(TaskerIntegration.EXTRA_PROGRESS, 
                (progress.progressPercentage * 100).toInt())
        }
        
        context.sendBroadcast(intent)
    }
}
