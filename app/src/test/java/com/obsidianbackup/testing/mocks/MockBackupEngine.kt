package com.obsidianbackup.testing.mocks

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockBackupEngine : BackupEngine {
    var backupCalls = mutableListOf<BackupRequest>()
    var restoreCalls = mutableListOf<RestoreRequest>()
    var shouldFail = false
    var failureMessage = "Mock failure"
    
    private val _progress = MutableStateFlow<BackupProgress>(BackupProgress.Idle)
    override val progress: Flow<BackupProgress> = _progress.asStateFlow()
    
    override suspend fun startBackup(request: BackupRequest): Result<String> {
        backupCalls.add(request)
        return if (shouldFail) {
            _progress.value = BackupProgress.Error(failureMessage)
            Result.failure(Exception(failureMessage))
        } else {
            _progress.value = BackupProgress.Running(0, request.totalFiles)
            _progress.value = BackupProgress.Completed("backup-123")
            Result.success("backup-123")
        }
    }
    
    override suspend fun startRestore(request: RestoreRequest): Result<Unit> {
        restoreCalls.add(request)
        return if (shouldFail) {
            _progress.value = BackupProgress.Error(failureMessage)
            Result.failure(Exception(failureMessage))
        } else {
            _progress.value = BackupProgress.Running(0, 100)
            _progress.value = BackupProgress.Completed("restore-complete")
            Result.success(Unit)
        }
    }
    
    override suspend fun cancelBackup() {
        _progress.value = BackupProgress.Idle
    }
    
    fun reset() {
        backupCalls.clear()
        restoreCalls.clear()
        shouldFail = false
        _progress.value = BackupProgress.Idle
    }
    
    fun simulateProgress(current: Int, total: Int) {
        _progress.value = BackupProgress.Running(current, total)
    }
}

interface BackupEngine {
    val progress: Flow<BackupProgress>
    suspend fun startBackup(request: BackupRequest): Result<String>
    suspend fun startRestore(request: RestoreRequest): Result<Unit>
    suspend fun cancelBackup()
}

data class BackupRequest(
    val configId: String,
    val sourcePaths: List<String>,
    val destinationPath: String,
    val totalFiles: Int,
    val isIncremental: Boolean = false,
    val compressionEnabled: Boolean = true,
    val encryptionEnabled: Boolean = false
)

data class RestoreRequest(
    val backupId: String,
    val destinationPath: String,
    val selectedFiles: List<String>? = null
)

sealed class BackupProgress {
    object Idle : BackupProgress()
    data class Running(val current: Int, val total: Int) : BackupProgress()
    data class Completed(val backupId: String) : BackupProgress()
    data class Error(val message: String) : BackupProgress()
}
