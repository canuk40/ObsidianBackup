package com.obsidianbackup.testing.mocks

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockBackupRepository : BackupRepository {
    private val backups = mutableListOf<BackupSnapshot>()
    private val _backupFlow = MutableStateFlow<List<BackupSnapshot>>(emptyList())
    
    var shouldFail = false
    var failureMessage = "Mock failure"
    
    override fun getAllBackups(): Flow<List<BackupSnapshot>> = _backupFlow.asStateFlow()
    
    override suspend fun getBackup(id: String): BackupSnapshot? {
        return if (shouldFail) null else backups.find { it.id == id }
    }
    
    override suspend fun insertBackup(backup: BackupSnapshot): Result<Long> {
        return if (shouldFail) {
            Result.failure(Exception(failureMessage))
        } else {
            backups.add(backup)
            _backupFlow.value = backups.toList()
            Result.success(backup.id.hashCode().toLong())
        }
    }
    
    override suspend fun updateBackup(backup: BackupSnapshot): Result<Unit> {
        return if (shouldFail) {
            Result.failure(Exception(failureMessage))
        } else {
            val index = backups.indexOfFirst { it.id == backup.id }
            if (index >= 0) {
                backups[index] = backup
                _backupFlow.value = backups.toList()
            }
            Result.success(Unit)
        }
    }
    
    override suspend fun deleteBackup(id: String): Result<Unit> {
        return if (shouldFail) {
            Result.failure(Exception(failureMessage))
        } else {
            backups.removeIf { it.id == id }
            _backupFlow.value = backups.toList()
            Result.success(Unit)
        }
    }
    
    override suspend fun deleteAllBackups(): Result<Unit> {
        return if (shouldFail) {
            Result.failure(Exception(failureMessage))
        } else {
            backups.clear()
            _backupFlow.value = emptyList()
            Result.success(Unit)
        }
    }
    
    fun reset() {
        backups.clear()
        _backupFlow.value = emptyList()
        shouldFail = false
    }
    
    fun addBackup(backup: BackupSnapshot) {
        backups.add(backup)
        _backupFlow.value = backups.toList()
    }
}

interface BackupRepository {
    fun getAllBackups(): Flow<List<BackupSnapshot>>
    suspend fun getBackup(id: String): BackupSnapshot?
    suspend fun insertBackup(backup: BackupSnapshot): Result<Long>
    suspend fun updateBackup(backup: BackupSnapshot): Result<Unit>
    suspend fun deleteBackup(id: String): Result<Unit>
    suspend fun deleteAllBackups(): Result<Unit>
}

data class BackupSnapshot(
    val id: String,
    val backupConfigId: String,
    val timestamp: Long,
    val totalFiles: Int,
    val totalSize: Long,
    val status: String,
    val errorMessage: String? = null
)
