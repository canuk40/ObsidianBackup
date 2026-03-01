package com.obsidianbackup.tasker.plugin

import android.content.Context
import android.content.Intent
import com.obsidianbackup.tasker.TaskerIntegration
import java.util.UUID

/**
 * Tasker Plugin Action Components
 * 
 * Provides Tasker-compatible action interfaces for ObsidianBackup operations.
 * These are simplified implementations that work with standard Tasker intents.
 */

/**
 * Input model for backup action
 */
data class BackupActionInput(
    var packages: String = "",
    var components: String = "APK,DATA",
    var compression: Int = 6,
    var incremental: Boolean = false,
    var encryption: Boolean = false,
    var description: String = ""
)

/**
 * Output model for backup action
 */
data class BackupActionOutput(
    var workId: String = "",
    var status: String = "",
    var message: String = ""
)

/**
 * Backup Action Runner - Executes backup action from Tasker
 */
class BackupActionRunner {
    
    fun run(context: Context, input: BackupActionInput): BackupActionOutput {
        val packages = input.packages.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        
        if (packages.isEmpty()) {
            return BackupActionOutput(
                workId = "",
                status = "error",
                message = "No packages specified"
            )
        }

        // Send backup intent
        val intent = Intent(TaskerIntegration.ACTION_START_BACKUP).apply {
            putExtra(TaskerIntegration.EXTRA_PACKAGE_LIST, packages.toTypedArray())
            putExtra(TaskerIntegration.EXTRA_BACKUP_COMPONENTS, 
                input.components.split(",").map { it.trim() }.toTypedArray())
            putExtra(TaskerIntegration.EXTRA_COMPRESSION_LEVEL, input.compression)
            putExtra(TaskerIntegration.EXTRA_INCREMENTAL, input.incremental)
            putExtra(TaskerIntegration.EXTRA_ENCRYPTION_ENABLED, input.encryption)
            putExtra(TaskerIntegration.EXTRA_DESCRIPTION, input.description)
            putExtra("calling_package", context.packageName)
        }
        
        context.sendBroadcast(intent)
        
        return BackupActionOutput(
            workId = UUID.randomUUID().toString(),
            status = "scheduled",
            message = "Backup scheduled for ${packages.size} apps"
        )
    }
}

/**
 * Restore Action Input
 */
data class RestoreActionInput(
    var snapshotId: String = "",
    var packages: String = "",
    var components: String = "APK,DATA"
)

/**
 * Restore Action Output
 */
data class RestoreActionOutput(
    var workId: String = "",
    var status: String = "",
    var message: String = ""
)

/**
 * Restore Action Runner
 */
class RestoreActionRunner {
    
    fun run(context: Context, input: RestoreActionInput): RestoreActionOutput {
        if (input.snapshotId.isEmpty()) {
            return RestoreActionOutput(
                workId = "",
                status = "error",
                message = "Snapshot ID is required"
            )
        }

        val packages = input.packages.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val intent = Intent(TaskerIntegration.ACTION_RESTORE_SNAPSHOT).apply {
            putExtra(TaskerIntegration.EXTRA_SNAPSHOT_ID, input.snapshotId)
            if (packages.isNotEmpty()) {
                putExtra(TaskerIntegration.EXTRA_PACKAGE_LIST, packages.toTypedArray())
            }
            putExtra(TaskerIntegration.EXTRA_BACKUP_COMPONENTS,
                input.components.split(",").map { it.trim() }.toTypedArray())
            putExtra("calling_package", context.packageName)
        }
        
        context.sendBroadcast(intent)
        
        return RestoreActionOutput(
            workId = UUID.randomUUID().toString(),
            status = "scheduled",
            message = "Restore scheduled for snapshot ${input.snapshotId}"
        )
    }
}

/**
 * Cloud Sync Action Input
 */
data class CloudSyncActionInput(
    var cloudProvider: String = "default",
    var snapshotId: String = ""
)

/**
 * Cloud Sync Action Output
 */
data class CloudSyncActionOutput(
    var workId: String = "",
    var status: String = "",
    var message: String = ""
)

/**
 * Cloud Sync Action Runner
 */
class CloudSyncActionRunner {
    
    fun run(context: Context, input: CloudSyncActionInput): CloudSyncActionOutput {
        val intent = Intent(TaskerIntegration.ACTION_TRIGGER_CLOUD_SYNC).apply {
            putExtra(TaskerIntegration.EXTRA_CLOUD_PROVIDER, input.cloudProvider)
            if (input.snapshotId.isNotEmpty()) {
                putExtra(TaskerIntegration.EXTRA_SNAPSHOT_ID, input.snapshotId)
            }
            putExtra("calling_package", context.packageName)
        }
        
        context.sendBroadcast(intent)
        
        return CloudSyncActionOutput(
            workId = UUID.randomUUID().toString(),
            status = "scheduled",
            message = "Cloud sync scheduled"
        )
    }
}

/**
 * Tasker Condition - Check Backup Status
 */
data class BackupStatusConditionInput(
    var workId: String = ""
)

data class BackupStatusConditionOutput(
    var isComplete: Boolean = false,
    var status: String = "",
    var progress: Int = 0
)

/**
 * Backup Status Condition Runner
 */
class BackupStatusConditionRunner {
    
    fun check(context: Context, input: BackupStatusConditionInput): BackupStatusConditionOutput {
        // Query status using ContentProvider
        val uri = android.net.Uri.parse("content://com.obsidianbackup.tasker/status/${input.workId}")
        
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val state = it.getString(it.getColumnIndexOrThrow("state"))
                    val progress = it.getInt(it.getColumnIndexOrThrow("progress"))
                    
                    return BackupStatusConditionOutput(
                        isComplete = state == "SUCCEEDED" || state == "FAILED",
                        status = state,
                        progress = progress
                    )
                }
            }
        } catch (e: Exception) {
            // Failed to query
        }
        
        return BackupStatusConditionOutput(
            isComplete = false,
            status = "UNKNOWN",
            progress = 0
        )
    }
}
