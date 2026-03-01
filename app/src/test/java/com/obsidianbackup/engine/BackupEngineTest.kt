package com.obsidianbackup.engine

import app.cash.turbine.test
import com.obsidianbackup.testing.TestDataFactory
import com.obsidianbackup.testing.mocks.*
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.time.Duration.Companion.seconds

@DisplayName("Backup Engine Tests")
class BackupEngineTest {
    
    private lateinit var mockBackupEngine: MockBackupEngine
    
    @BeforeEach
    fun setup() {
        mockBackupEngine = MockBackupEngine()
    }
    
    @AfterEach
    fun teardown() {
        mockBackupEngine.reset()
    }
    
    @Nested
    @DisplayName("Backup Operations")
    inner class BackupOperations {
        
        @Test
        @DisplayName("Should start backup successfully")
        fun testStartBackupSuccess() = runTest {
            val request = BackupRequest(
                configId = "config-1",
                sourcePaths = listOf("/data/app1"),
                destinationPath = "/backup",
                totalFiles = 100
            )
            
            val result = mockBackupEngine.startBackup(request)
            
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()).isEqualTo("backup-123")
            assertThat(mockBackupEngine.backupCalls).hasSize(1)
        }
        
        @Test
        @DisplayName("Should handle backup failure")
        fun testStartBackupFailure() = runTest {
            mockBackupEngine.shouldFail = true
            val request = BackupRequest(
                configId = "config-1",
                sourcePaths = listOf("/data/app1"),
                destinationPath = "/backup",
                totalFiles = 100
            )
            
            val result = mockBackupEngine.startBackup(request)
            
            assertThat(result.isFailure).isTrue()
        }
        
        @ParameterizedTest
        @CsvSource(
            "true, true",
            "true, false",
            "false, true",
            "false, false"
        )
        @DisplayName("Should handle different backup configurations")
        fun testDifferentBackupConfigurations(incremental: Boolean, encrypted: Boolean) = runTest {
            val request = BackupRequest(
                configId = "config-1",
                sourcePaths = listOf("/data/app1"),
                destinationPath = "/backup",
                totalFiles = 100,
                isIncremental = incremental,
                encryptionEnabled = encrypted
            )
            
            val result = mockBackupEngine.startBackup(request)
            
            assertThat(result.isSuccess).isTrue()
            assertThat(mockBackupEngine.backupCalls[0].isIncremental).isEqualTo(incremental)
            assertThat(mockBackupEngine.backupCalls[0].encryptionEnabled).isEqualTo(encrypted)
        }
    }
}
