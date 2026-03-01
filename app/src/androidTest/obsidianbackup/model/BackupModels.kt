// model/BackupModels.kt
package com.titanbackup.model

@JvmInline
value class BackupId(val value: String)

@JvmInline
value class AppId(val value: String)

enum class BackupComponent {
    APK, DATA, OBB, EXTERNAL_DATA, CACHE
}

data class BackupRequest(
    val appIds: List<AppId>,
    val components: Set<BackupComponent>,
    val incremental: Boolean = false,
    val compressionLevel: Int = 6,
    val encryptionEnabled: Boolean = false,
    val description: String? = null
)

sealed class BackupResult {
    data class Success(
        val snapshotId: BackupId,
        val timestamp: Long,
        val appsBackedUp: List<AppId>,
        val totalSize: Long,
        val duration: Long,
        val checksums: Map<String, String>
    ) : BackupResult()

    data class PartialSuccess(
        val snapshotId: BackupId,
        val timestamp: Long,
        val appsBackedUp: List<AppId>,
        val appsFailed: List<AppId>,
        val totalSize: Long,
        val duration: Long,
        val errors: List<String>
    ) : BackupResult()

    data class Failure(
        val reason: String,
        val appsFailed: List<AppId>
    ) : BackupResult()
}

data class RestoreRequest(
    val snapshotId: BackupId,
    val appIds: List<AppId>,
    val components: Set<BackupComponent>,
    val dryRun: Boolean = false,
    val overwriteExisting: Boolean = false
)

sealed class RestoreResult {
    data class Success(
        val appsRestored: List<AppId>,
        val duration: Long,
        val warnings: List<String>
    ) : RestoreResult()

    data class PartialSuccess(
        val appsRestored: List<AppId>,
        val appsFailed: List<AppId>,
        val duration: Long,
        val errors: List<String>
    ) : RestoreResult()

    data class Failure(
        val reason: String
    ) : RestoreResult()
}

sealed class VerificationResult {
    data class Verified(
        val snapshotId: BackupId,
        val filesChecked: Int,
        val allValid: Boolean,
        val corruptedFiles: List<String>
    ) : VerificationResult()

    data class Failed(
        val snapshotId: BackupId,
        val reason: String
    ) : VerificationResult()
}

enum class OperationType {
    BACKUP, RESTORE, VERIFY, DELETE
}

sealed class OperationProgress {
    object Idle : OperationProgress()

    data class InProgress(
        val operationType: OperationType,
        val currentItem: String,
        val itemsCompleted: Int,
        val totalItems: Int,
        val bytesProcessed: Long,
        val totalBytes: Long
    ) : OperationProgress()

    data class Completed(
        val operationType: OperationType,
        val itemsCompleted: Int,
        val totalItems: Int
    ) : OperationProgress()
}