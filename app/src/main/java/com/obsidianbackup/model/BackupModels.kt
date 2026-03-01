package com.obsidianbackup.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = AppIdSerializer::class)
@JvmInline
value class AppId(val value: String)

object AppIdSerializer : KSerializer<AppId> {
    override val descriptor: SerialDescriptor = 
        PrimitiveSerialDescriptor("AppId", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: AppId) {
        encoder.encodeString(value.value)
    }
    
    override fun deserialize(decoder: Decoder): AppId {
        return AppId(decoder.decodeString())
    }
}

@JvmInline
value class SnapshotId(val value: String)

@JvmInline
value class BackupId(val value: String)

data class BackupRequest(
    val appIds: List<AppId>,
    val components: Set<BackupComponent> = setOf(BackupComponent.APK, BackupComponent.DATA),
    val incremental: Boolean = false,
    val compressionLevel: Int = 6,
    val encryptionEnabled: Boolean = false,
    val description: String? = null
)

sealed class BackupResult {
    data class Success(
        val snapshotId: SnapshotId,
        val timestamp: Long,
        val appsBackedUp: List<AppId>,
        val totalSize: Long,
        val duration: Long,
        val checksums: Map<String, String> = emptyMap(),
        val incrementalStats: IncrementalStats? = null
    ) : BackupResult()

    data class PartialSuccess(
        val snapshotId: SnapshotId,
        val timestamp: Long,
        val appsBackedUp: List<AppId>,
        val appsFailed: List<AppId>,
        val totalSize: Long,
        val duration: Long,
        val errors: List<String> = emptyList(),
        val incrementalStats: IncrementalStats? = null
    ) : BackupResult()

    data class Failure(
        val reason: String,
        val appsFailed: List<AppId>
    ) : BackupResult()
}

/**
 * Incremental backup statistics
 */
data class IncrementalStats(
    val isIncremental: Boolean,
    val baseSnapshotId: SnapshotId?,
    val filesScanned: Int,
    val filesChanged: Int,
    val filesUnchanged: Int,
    val filesDeleted: Int,
    val filesDeduped: Int,
    val deltaSize: Long,
    val savedSize: Long,
    val hardLinksCreated: Int
)

data class RestoreRequest(
    val snapshotId: SnapshotId,
    val appIds: List<AppId>,
    val components: Set<BackupComponent> = setOf(BackupComponent.APK, BackupComponent.DATA),
    val dryRun: Boolean = false,
    val overwriteExisting: Boolean = true
)

sealed class RestoreResult {
    data class Success(
        val appsRestored: List<AppId>,
        val duration: Long,
        val warnings: List<String> = emptyList()
    ) : RestoreResult()

    data class PartialSuccess(
        val appsRestored: List<AppId>,
        val appsFailed: List<AppId>,
        val duration: Long,
        val errors: List<String> = emptyList()
    ) : RestoreResult()

    data class Failure(
        val reason: String
    ) : RestoreResult()
}

/**
 * Result from simulating a restore operation (dryRun = true)
 */
data class RestoreSimulationResult(
    val snapshotId: SnapshotId,
    val appIds: List<AppId>,
    val estimatedDuration: Long,
    val conflicts: List<RestoreConflict> = emptyList(),
    val warnings: List<String> = emptyList(),
    val canProceed: Boolean
)

/**
 * A conflict discovered during restore simulation
 */
data class RestoreConflict(
    val appId: AppId,
    val type: ConflictType,
    val currentVersion: String,
    val backupVersion: String,
    val description: String
)

enum class ConflictType {
    VERSION_MISMATCH,
    APP_NOT_INSTALLED,
    INSUFFICIENT_SPACE,
    PERMISSION_MISSING
}

data class VerificationResult(
    val snapshotId: SnapshotId,
    val filesChecked: Int,
    val allValid: Boolean,
    val corruptedFiles: List<String> = emptyList()
)

data class OperationProgress(
    val operationType: OperationType,
    val currentItem: String,
    val itemsCompleted: Int,
    val totalItems: Int,
    val bytesProcessed: Long = 0L,
    val totalBytes: Long = 0L
) {
    val progressPercentage: Float
        get() = if (totalItems > 0) (itemsCompleted.toFloat() / totalItems) else 0f

    val isComplete: Boolean
        get() = itemsCompleted >= totalItems
}

enum class OperationType {
    BACKUP, RESTORE, VERIFY, DELETE
}

data class AppInfo(
    val appId: AppId,
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val isUpdatedSystemApp: Boolean = false,
    val dataSize: Long,
    val apkSize: Long,
    val lastUpdateTime: Long,
    val icon: android.graphics.Bitmap? = null
)

data class BackupSnapshot(
    val id: BackupId,
    val timestamp: Long,
    val description: String?,
    val apps: List<AppInfo>,
    val totalSize: Long,
    val compressionRatio: Float,
    val encrypted: Boolean,
    val verified: Boolean,
    val permissionMode: String,
    val deviceInfo: DeviceInfo
)

@Serializable
data class DeviceInfo(
    val model: String,
    val manufacturer: String,
    val androidVersion: Int,
    val buildFingerprint: String
)

/**
 * Backup metadata model (used in engines and domain layer)
 */
data class BackupMetadata(
    val snapshotId: BackupId,
    val timestamp: Long,
    val description: String,
    val appIds: List<AppId>,
    val components: Set<BackupComponent>,
    val compressionLevel: Int,
    val encrypted: Boolean,
    val totalSize: Long,
    val deviceInfo: DeviceInfo
)

data class LogEntry(
    val timestamp: Long,
    val operationType: OperationType,
    val level: LogLevel,
    val message: String,
    val details: String? = null,
    val snapshotId: SnapshotId? = null,
    val exitCode: Int? = null
)

enum class LogLevel {
    VERBOSE, DEBUG, INFO, WARN, ERROR
}
