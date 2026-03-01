package com.obsidianbackup.tasker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.obsidianbackup.model.*
import com.obsidianbackup.security.TaskerSecurityValidator
import com.obsidianbackup.work.BackupWorker
import com.obsidianbackup.work.CloudSyncWorker
import com.obsidianbackup.work.RestoreWorker
import com.obsidianbackup.work.VerifyWorker
import com.obsidianbackup.work.DeleteWorker
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.util.concurrent.TimeUnit

/**
 * TaskerIntegration - BroadcastReceiver for Tasker/MacroDroid automation
 * 
 * Supports intents for:
 * - Starting backup operations
 * - Restoring snapshots
 * - Querying operation status
 * - Triggering cloud sync
 * - Verifying backups
 * 
 * Security: All intents are validated against authorized caller packages
 */
class TaskerIntegration : BroadcastReceiver() {

    companion object {
        private const val TAG = "TaskerIntegration"

        // Intent Actions
        const val ACTION_START_BACKUP = "com.obsidianbackup.tasker.ACTION_START_BACKUP"
        const val ACTION_RESTORE_SNAPSHOT = "com.obsidianbackup.tasker.ACTION_RESTORE_SNAPSHOT"
        const val ACTION_QUERY_STATUS = "com.obsidianbackup.tasker.ACTION_QUERY_STATUS"
        const val ACTION_TRIGGER_CLOUD_SYNC = "com.obsidianbackup.tasker.ACTION_TRIGGER_CLOUD_SYNC"
        const val ACTION_VERIFY_BACKUP = "com.obsidianbackup.tasker.ACTION_VERIFY_BACKUP"
        const val ACTION_CANCEL_OPERATION = "com.obsidianbackup.tasker.ACTION_CANCEL_OPERATION"
        const val ACTION_DELETE_SNAPSHOT = "com.obsidianbackup.tasker.ACTION_DELETE_SNAPSHOT"

        // Event Actions (broadcast by app)
        const val EVENT_BACKUP_COMPLETE = "com.obsidianbackup.tasker.EVENT_BACKUP_COMPLETE"
        const val EVENT_BACKUP_FAILED = "com.obsidianbackup.tasker.EVENT_BACKUP_FAILED"
        const val EVENT_RESTORE_COMPLETE = "com.obsidianbackup.tasker.EVENT_RESTORE_COMPLETE"
        const val EVENT_RESTORE_FAILED = "com.obsidianbackup.tasker.EVENT_RESTORE_FAILED"
        const val EVENT_SYNC_COMPLETE = "com.obsidianbackup.tasker.EVENT_SYNC_COMPLETE"
        const val EVENT_SYNC_FAILED = "com.obsidianbackup.tasker.EVENT_SYNC_FAILED"

        // Intent Parameters
        const val EXTRA_PACKAGE_LIST = "package_list"
        const val EXTRA_BACKUP_ID = "backup_id"
        const val EXTRA_SNAPSHOT_ID = "snapshot_id"
        const val EXTRA_CLOUD_PROVIDER = "cloud_provider"
        const val EXTRA_BACKUP_COMPONENTS = "backup_components"
        const val EXTRA_COMPRESSION_LEVEL = "compression_level"
        const val EXTRA_INCREMENTAL = "incremental"
        const val EXTRA_ENCRYPTION_ENABLED = "encryption_enabled"
        const val EXTRA_DESCRIPTION = "description"
        const val EXTRA_WORK_REQUEST_ID = "work_request_id"

        // Response Parameters
        const val EXTRA_STATUS = "status"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_PROGRESS = "progress"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_APPS_BACKED_UP = "apps_backed_up"
        const val EXTRA_APPS_FAILED = "apps_failed"
        const val EXTRA_SNAPSHOT_TIMESTAMP = "snapshot_timestamp"
        const val EXTRA_TOTAL_SIZE = "total_size"
        const val EXTRA_DURATION = "duration"

        // Result Codes
        const val RESULT_SUCCESS = 0
        const val RESULT_PARTIAL_SUCCESS = 1
        const val RESULT_FAILURE = 2
        const val RESULT_INVALID_PARAMS = 3
        const val RESULT_PERMISSION_DENIED = 4
        const val RESULT_IN_PROGRESS = 5
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        Log.d(TAG, "Received intent: ${intent.action}")

        // Manual DI - get from Application
        val app = context.applicationContext as? com.obsidianbackup.ObsidianBackupApplication
        val securityValidator = app?.securityValidator ?: run {
            Log.e(TAG, "Failed to get securityValidator from Application")
            return
        }

        // Security validation
        val callingPackage = intent.getStringExtra("calling_package") 
            ?: getCallingPackage(context)
        
        if (!securityValidator.isAuthorizedPackage(callingPackage)) {
            Log.w(TAG, "Unauthorized package: $callingPackage")
            sendErrorResponse(context, intent, "Unauthorized caller", RESULT_PERMISSION_DENIED)
            return
        }

        try {
            when (intent.action) {
                ACTION_START_BACKUP -> handleStartBackup(context, intent)
                ACTION_RESTORE_SNAPSHOT -> handleRestoreSnapshot(context, intent)
                ACTION_QUERY_STATUS -> handleQueryStatus(context, intent)
                ACTION_TRIGGER_CLOUD_SYNC -> handleTriggerCloudSync(context, intent)
                ACTION_VERIFY_BACKUP -> handleVerifyBackup(context, intent)
                ACTION_CANCEL_OPERATION -> handleCancelOperation(context, intent)
                ACTION_DELETE_SNAPSHOT -> handleDeleteSnapshot(context, intent)
                else -> {
                    Log.w(TAG, "Unknown action: ${intent.action}")
                    sendErrorResponse(context, intent, "Unknown action", RESULT_INVALID_PARAMS)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling intent: ${intent.action}", e)
            sendErrorResponse(context, intent, "Internal error: ${e.message}", RESULT_FAILURE)
        }
    }

    private fun handleStartBackup(context: Context, intent: Intent) {
        Log.i(TAG, "Starting backup via Tasker")

        // Extract parameters
        val packageList = intent.getStringArrayExtra(EXTRA_PACKAGE_LIST)
        val componentsStr = intent.getStringArrayExtra(EXTRA_BACKUP_COMPONENTS)
        val compressionLevel = intent.getIntExtra(EXTRA_COMPRESSION_LEVEL, 6)
        val incremental = intent.getBooleanExtra(EXTRA_INCREMENTAL, false)
        val encryptionEnabled = intent.getBooleanExtra(EXTRA_ENCRYPTION_ENABLED, false)
        val description = intent.getStringExtra(EXTRA_DESCRIPTION)

        // Validate parameters
        if (packageList == null || packageList.isEmpty()) {
            sendErrorResponse(context, intent, "Package list is required", RESULT_INVALID_PARAMS)
            return
        }

        // Parse components
        val components = componentsStr?.mapNotNull { 
            try { BackupComponent.valueOf(it) } catch (e: Exception) { null }
        }?.toSet() ?: setOf(BackupComponent.APK, BackupComponent.DATA)

        // Create backup request
        val appIds = packageList.map { AppId(it) }
        val backupRequest = BackupRequest(
            appIds = appIds,
            components = components,
            incremental = incremental,
            compressionLevel = compressionLevel.coerceIn(0, 9),
            encryptionEnabled = encryptionEnabled,
            description = description ?: "Tasker backup"
        )

        // Schedule backup work
        val workManager = WorkManager.getInstance(context)
        val inputData = workDataOf(
            "appIds" to Json.encodeToString(appIds.map { it.value }),
            "components" to Json.encodeToString(components.map { it.name }),
            "compressionLevel" to compressionLevel,
            "incremental" to incremental,
            "encryptionEnabled" to encryptionEnabled,
            "description" to backupRequest.description
        )

        val workRequest = OneTimeWorkRequestBuilder<BackupWorker>()
            .setInputData(inputData)
            .addTag("tasker_backup")
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueue(workRequest)

        // Send response
        val response = Intent(ACTION_START_BACKUP + "_RESPONSE").apply {
            putExtra(EXTRA_WORK_REQUEST_ID, workRequest.id.toString())
            putExtra(EXTRA_STATUS, "started")
            putExtra(EXTRA_MESSAGE, "Backup scheduled for ${packageList.size} packages")
            putExtra(EXTRA_RESULT_CODE, RESULT_SUCCESS)
        }
        context.sendBroadcast(response)

        Log.i(TAG, "Backup work scheduled: ${workRequest.id}")
    }

    private fun handleRestoreSnapshot(context: Context, intent: Intent) {
        Log.i(TAG, "Restoring snapshot via Tasker")

        // Extract parameters
        val snapshotId = intent.getStringExtra(EXTRA_SNAPSHOT_ID)
        val packageList = intent.getStringArrayExtra(EXTRA_PACKAGE_LIST)
        val componentsStr = intent.getStringArrayExtra(EXTRA_BACKUP_COMPONENTS)

        // Validate parameters
        if (snapshotId.isNullOrEmpty()) {
            sendErrorResponse(context, intent, "Snapshot ID is required", RESULT_INVALID_PARAMS)
            return
        }

        // Parse components
        val components = componentsStr?.mapNotNull { 
            try { BackupComponent.valueOf(it) } catch (e: Exception) { null }
        }?.toSet() ?: setOf(BackupComponent.APK, BackupComponent.DATA)

        // Parse app IDs (if specified, restore only those apps)
        val appIds = packageList?.map { AppId(it) } ?: emptyList()

        // Create restore request
        val restoreRequest = RestoreRequest(
            snapshotId = SnapshotId(snapshotId),
            appIds = appIds,
            components = components,
            dryRun = false,
            overwriteExisting = true
        )

        // Schedule restore work (would need RestoreWorker implementation)
        val inputData = workDataOf(
            "snapshotId" to snapshotId,
            "appIds" to Json.encodeToString(appIds.map { it.value }),
            "components" to Json.encodeToString(components.map { it.name })
        )

        val workManager = WorkManager.getInstance(context)
        val workRequest = OneTimeWorkRequestBuilder<RestoreWorker>()
            .setInputData(inputData)
            .addTag("tasker_restore")
            .build()

        workManager.enqueue(workRequest)

        // Send response
        val response = Intent(ACTION_RESTORE_SNAPSHOT + "_RESPONSE").apply {
            putExtra(EXTRA_WORK_REQUEST_ID, workRequest.id.toString())
            putExtra(EXTRA_STATUS, "started")
            putExtra(EXTRA_MESSAGE, "Restore scheduled for snapshot $snapshotId")
            putExtra(EXTRA_RESULT_CODE, RESULT_SUCCESS)
        }
        context.sendBroadcast(response)

        Log.i(TAG, "Restore work scheduled: ${workRequest.id}")
    }

    private fun handleQueryStatus(context: Context, intent: Intent) {
        Log.d(TAG, "Querying status via Tasker")

        val workRequestId = intent.getStringExtra(EXTRA_WORK_REQUEST_ID)
        
        if (workRequestId.isNullOrEmpty()) {
            sendErrorResponse(context, intent, "Work request ID is required", RESULT_INVALID_PARAMS)
            return
        }

        val workManager = WorkManager.getInstance(context)
        val workInfoFuture = workManager.getWorkInfoById(java.util.UUID.fromString(workRequestId))

        // This is a synchronous query for simplicity
        try {
            val workInfo = workInfoFuture.get(5, TimeUnit.SECONDS)
            
            val response = Intent(ACTION_QUERY_STATUS + "_RESPONSE").apply {
                putExtra(EXTRA_WORK_REQUEST_ID, workRequestId)
                putExtra(EXTRA_STATUS, workInfo?.state?.name ?: "UNKNOWN")
                putExtra(EXTRA_PROGRESS, workInfo?.progress?.getInt("progress", 0) ?: 0)
                putExtra(EXTRA_RESULT_CODE, RESULT_SUCCESS)
            }
            context.sendBroadcast(response)
            
            Log.d(TAG, "Status query successful: ${workInfo?.state}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query work status", e)
            sendErrorResponse(context, intent, "Failed to query status: ${e.message}", RESULT_FAILURE)
        }
    }

    private fun handleTriggerCloudSync(context: Context, intent: Intent) {
        Log.i(TAG, "Triggering cloud sync via Tasker")

        val cloudProvider = intent.getStringExtra(EXTRA_CLOUD_PROVIDER)
        val snapshotId = intent.getStringExtra(EXTRA_SNAPSHOT_ID)

        // Schedule cloud sync work
        val inputData = workDataOf(
            "cloudProvider" to (cloudProvider ?: "default"),
            "snapshotId" to snapshotId
        )

        val workManager = WorkManager.getInstance(context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag("tasker_cloud_sync")
            .build()

        workManager.enqueue(workRequest)

        // Send response
        val response = Intent(ACTION_TRIGGER_CLOUD_SYNC + "_RESPONSE").apply {
            putExtra(EXTRA_WORK_REQUEST_ID, workRequest.id.toString())
            putExtra(EXTRA_STATUS, "started")
            putExtra(EXTRA_MESSAGE, "Cloud sync scheduled")
            putExtra(EXTRA_RESULT_CODE, RESULT_SUCCESS)
        }
        context.sendBroadcast(response)

        Log.i(TAG, "Cloud sync work scheduled: ${workRequest.id}")
    }

    private fun handleVerifyBackup(context: Context, intent: Intent) {
        Log.i(TAG, "Verifying backup via Tasker")

        val snapshotId = intent.getStringExtra(EXTRA_SNAPSHOT_ID)

        if (snapshotId.isNullOrEmpty()) {
            sendErrorResponse(context, intent, "Snapshot ID is required", RESULT_INVALID_PARAMS)
            return
        }

        // Schedule verification work
        val inputData = workDataOf("snapshotId" to snapshotId)

        val workManager = WorkManager.getInstance(context)
        val workRequest = OneTimeWorkRequestBuilder<VerifyWorker>()
            .setInputData(inputData)
            .addTag("tasker_verify")
            .build()

        workManager.enqueue(workRequest)

        // Send response
        val response = Intent(ACTION_VERIFY_BACKUP + "_RESPONSE").apply {
            putExtra(EXTRA_WORK_REQUEST_ID, workRequest.id.toString())
            putExtra(EXTRA_STATUS, "started")
            putExtra(EXTRA_MESSAGE, "Verification scheduled for snapshot $snapshotId")
            putExtra(EXTRA_RESULT_CODE, RESULT_SUCCESS)
        }
        context.sendBroadcast(response)

        Log.i(TAG, "Verification work scheduled: ${workRequest.id}")
    }

    private fun handleCancelOperation(context: Context, intent: Intent) {
        Log.i(TAG, "Cancelling operation via Tasker")

        val workRequestId = intent.getStringExtra(EXTRA_WORK_REQUEST_ID)

        if (workRequestId.isNullOrEmpty()) {
            sendErrorResponse(context, intent, "Work request ID is required", RESULT_INVALID_PARAMS)
            return
        }

        val workManager = WorkManager.getInstance(context)
        workManager.cancelWorkById(java.util.UUID.fromString(workRequestId))

        // Send response
        val response = Intent(ACTION_CANCEL_OPERATION + "_RESPONSE").apply {
            putExtra(EXTRA_WORK_REQUEST_ID, workRequestId)
            putExtra(EXTRA_STATUS, "cancelled")
            putExtra(EXTRA_MESSAGE, "Operation cancelled")
            putExtra(EXTRA_RESULT_CODE, RESULT_SUCCESS)
        }
        context.sendBroadcast(response)

        Log.i(TAG, "Operation cancelled: $workRequestId")
    }

    private fun handleDeleteSnapshot(context: Context, intent: Intent) {
        Log.i(TAG, "Deleting snapshot via Tasker")

        val snapshotId = intent.getStringExtra(EXTRA_SNAPSHOT_ID)

        if (snapshotId.isNullOrEmpty()) {
            sendErrorResponse(context, intent, "Snapshot ID is required", RESULT_INVALID_PARAMS)
            return
        }

        // Schedule delete work
        val inputData = workDataOf("snapshotId" to snapshotId)

        val workManager = WorkManager.getInstance(context)
        val workRequest = OneTimeWorkRequestBuilder<DeleteWorker>()
            .setInputData(inputData)
            .addTag("tasker_delete")
            .build()

        workManager.enqueue(workRequest)

        // Send response
        val response = Intent(ACTION_DELETE_SNAPSHOT + "_RESPONSE").apply {
            putExtra(EXTRA_WORK_REQUEST_ID, workRequest.id.toString())
            putExtra(EXTRA_STATUS, "started")
            putExtra(EXTRA_MESSAGE, "Deletion scheduled for snapshot $snapshotId")
            putExtra(EXTRA_RESULT_CODE, RESULT_SUCCESS)
        }
        context.sendBroadcast(response)

        Log.i(TAG, "Delete work scheduled: ${workRequest.id}")
    }

    private fun sendErrorResponse(context: Context, intent: Intent, message: String, resultCode: Int) {
        val response = Intent(intent.action + "_RESPONSE").apply {
            putExtra(EXTRA_STATUS, "error")
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_RESULT_CODE, resultCode)
        }
        context.sendBroadcast(response)
        Log.e(TAG, "Error response sent: $message")
    }

    private fun getCallingPackage(context: Context): String {
        // This is a simplified implementation
        // In production, use proper caller identification
        return "unknown"
    }
}
