package com.obsidianbackup.core.models

import com.obsidianbackup.model.AppId as OldAppId
import com.obsidianbackup.model.SnapshotId as OldSnapshotId
import com.obsidianbackup.model.BackupId as OldBackupId
import com.obsidianbackup.model.BackupComponent as OldBackupComponent
import com.obsidianbackup.model.BackupRequest as OldBackupRequest
import com.obsidianbackup.model.BackupResult as OldBackupResult
import com.obsidianbackup.model.RestoreRequest as OldRestoreRequest
import com.obsidianbackup.model.RestoreResult as OldRestoreResult
import com.obsidianbackup.model.VerificationResult as OldVerificationResult
import com.obsidianbackup.model.OperationProgress as OldOperationProgress
import com.obsidianbackup.model.OperationType as OldOperationType
import com.obsidianbackup.model.AppInfo as OldAppInfo
import com.obsidianbackup.model.BackupSnapshot as OldBackupSnapshot
import com.obsidianbackup.model.DeviceInfo as OldDeviceInfo
import com.obsidianbackup.model.LogEntry as OldLogEntry
import com.obsidianbackup.model.LogLevel as OldLogLevel

// Re-export model types under the new package
typealias AppId = OldAppId
typealias SnapshotId = OldSnapshotId
typealias BackupId = OldBackupId
typealias BackupComponent = OldBackupComponent
typealias BackupRequest = OldBackupRequest
typealias BackupResult = OldBackupResult
typealias RestoreRequest = OldRestoreRequest
typealias RestoreResult = OldRestoreResult
typealias VerificationResult = OldVerificationResult
typealias OperationProgress = OldOperationProgress
typealias OperationType = OldOperationType
typealias AppInfo = OldAppInfo
typealias BackupSnapshot = OldBackupSnapshot
typealias DeviceInfo = OldDeviceInfo
typealias LogEntry = OldLogEntry
typealias LogLevel = OldLogLevel
