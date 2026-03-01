package com.obsidianbackup.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.obsidianbackup.testing.mocks.BackupSnapshot
import com.obsidianbackup.testing.mocks.MockBackupRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*

@DisplayName("Backup Repository Tests")
class BackupRepositoryTest {
    
    private lateinit var repository: MockBackupRepository
    
    @BeforeEach
    fun setup() {
        repository = MockBackupRepository()
    }
    
    @Test
    fun testInsertBackup() = runTest {
        val backup = BackupSnapshot(
            id = "backup-1",
            backupConfigId = "config-1",
            timestamp = System.currentTimeMillis(),
            totalFiles = 100,
            totalSize = 1024 * 1024,
            status = "COMPLETED"
        )
        
        val result = repository.insertBackup(backup)
        
        assertThat(result.isSuccess).isTrue()
    }
    
    @Test
    fun testGetBackup() = runTest {
        val backup = BackupSnapshot(
            id = "backup-1",
            backupConfigId = "config-1",
            timestamp = System.currentTimeMillis(),
            totalFiles = 100,
            totalSize = 1024 * 1024,
            status = "COMPLETED"
        )
        repository.addBackup(backup)
        
        val retrieved = repository.getBackup("backup-1")
        
        assertThat(retrieved).isNotNull()
    }
}
