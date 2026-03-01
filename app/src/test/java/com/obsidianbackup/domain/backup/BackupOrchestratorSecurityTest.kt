// domain/backup/BackupOrchestratorSecurityTest.kt
package com.obsidianbackup.domain.backup

import com.obsidianbackup.engine.*
import com.obsidianbackup.model.*
import com.obsidianbackup.storage.BackupCatalog
import com.obsidianbackup.verification.ChecksumVerifier
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * Security tests for BackupOrchestrator to ensure path traversal vulnerabilities are fixed.
 */
class BackupOrchestratorSecurityTest {

    private lateinit var engineFactory: BackupEngineFactory
    private lateinit var catalog: BackupCatalog
    private lateinit var verifier: ChecksumVerifier
    private lateinit var eventBus: BackupEventBus
    private lateinit var incrementalStrategy: IncrementalBackupStrategy
    private lateinit var orchestrator: BackupOrchestrator
    
    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setup() {
        engineFactory = mockk(relaxed = true)
        catalog = mockk(relaxed = true)
        verifier = mockk(relaxed = true)
        eventBus = mockk(relaxed = true)
        incrementalStrategy = mockk(relaxed = true)
        
        val backupRootPath = tempDir.toString()
        orchestrator = BackupOrchestrator(
            engineFactory = engineFactory,
            catalog = catalog,
            verifier = verifier,
            eventBus = eventBus,
            incrementalStrategy = incrementalStrategy,
            backupRootPath = backupRootPath
        )
    }

    @AfterEach
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `executeBackup rejects malicious app IDs with path traversal`() = runTest {
        // Malicious app ID attempting path traversal
        val maliciousAppId = AppId("../../../etc/passwd")
        
        val request = BackupRequest(
            appIds = listOf(maliciousAppId),
            components = setOf(BackupComponent.APK, BackupComponent.DATA),
            incremental = false,
            compressionLevel = CompressionLevel.NORMAL
        )

        // Mock engine
        val mockEngine = mockk<BackupEngine>(relaxed = true)
        every { engineFactory.createForCurrentMode() } returns mockEngine

        // Mock incremental strategy to return plans
        coEvery { 
            incrementalStrategy.createIncremental(any(), any()) 
        } returns BackupPlan.Full(emptyList())

        // Execute backup - should fail with SecurityException
        val result = orchestrator.executeBackup(request)
        
        // Verify it failed
        assertTrue(result is com.obsidianbackup.model.BackupResult.Failure)
        
        // The error should mention the malicious app
        if (result is com.obsidianbackup.model.BackupResult.Failure) {
            assertTrue(
                result.appsFailed.any { it.contains(maliciousAppId.value) } ||
                result.reason.contains("Invalid app ID") ||
                result.reason.contains("SecurityException")
            )
        }
    }

    @Test
    fun `executeBackup accepts valid package names`() = runTest {
        val validAppId = AppId("com.example.testapp")
        
        val request = BackupRequest(
            appIds = listOf(validAppId),
            components = setOf(BackupComponent.APK, BackupComponent.DATA),
            incremental = false,
            compressionLevel = CompressionLevel.NORMAL
        )

        // Mock successful backup
        val mockEngine = mockk<BackupEngine>(relaxed = true)
        every { engineFactory.createForCurrentMode() } returns mockEngine
        
        coEvery { 
            incrementalStrategy.createIncremental(validAppId, null) 
        } returns BackupPlan.Full(emptyList())
        
        coEvery {
            incrementalStrategy.executeBackupPlan(any(), any(), any())
        } returns ExecutionResult(
            filesProcessed = 0,
            bytesCopied = 0,
            errors = emptyList()
        )

        // Should not throw exception
        val result = orchestrator.executeBackup(request)
        
        // Should complete successfully or with expected behavior
        // (May fail due to actual file system access, but shouldn't be SecurityException)
        if (result is com.obsidianbackup.model.BackupResult.Failure) {
            assertFalse(
                result.reason.contains("Invalid app ID") ||
                result.reason.contains("Path traversal")
            )
        }
    }

    @Test
    fun `various malicious app ID formats are rejected`() = runTest {
        val maliciousAppIds = listOf(
            "../../../etc/passwd",
            "../../system",
            "/etc/passwd",
            "com.example/../../etc/passwd",
            "com.example\\..\\..\\system",
            "com.example..malicious",
            "..malicious.app"
        )

        maliciousAppIds.forEach { malicious ->
            val appId = AppId(malicious)
            val request = BackupRequest(
                appIds = listOf(appId),
                components = setOf(BackupComponent.APK),
                incremental = false,
                compressionLevel = CompressionLevel.NORMAL
            )

            val mockEngine = mockk<BackupEngine>(relaxed = true)
            every { engineFactory.createForCurrentMode() } returns mockEngine
            
            coEvery { 
                incrementalStrategy.createIncremental(any(), any()) 
            } returns BackupPlan.Full(emptyList())

            val result = orchestrator.executeBackup(request)
            
            assertTrue(
                result is com.obsidianbackup.model.BackupResult.Failure,
                "Should reject malicious app ID: $malicious"
            )
        }
    }

    @Test
    fun `incremental backup with malicious app ID is rejected`() = runTest {
        val maliciousAppId = AppId("../../../root/.ssh/id_rsa")
        
        val request = BackupRequest(
            appIds = listOf(maliciousAppId),
            components = setOf(BackupComponent.DATA),
            incremental = true,
            compressionLevel = CompressionLevel.NORMAL
        )

        val mockEngine = mockk<BackupEngine>(relaxed = true)
        every { engineFactory.createForCurrentMode() } returns mockEngine

        // The createIncremental call itself should fail with SecurityException
        coEvery { 
            incrementalStrategy.createIncremental(maliciousAppId, any()) 
        } throws SecurityException("Invalid app ID format")

        val result = orchestrator.executeBackup(request)
        
        assertTrue(result is com.obsidianbackup.model.BackupResult.Failure)
    }

    @Test
    fun `batch backup rejects all malicious app IDs`() = runTest {
        val mixedAppIds = listOf(
            AppId("com.example.validapp"),
            AppId("../../../etc/passwd"),
            AppId("com.another.validapp"),
            AppId("../../system")
        )
        
        val request = BackupRequest(
            appIds = mixedAppIds,
            components = setOf(BackupComponent.APK, BackupComponent.DATA),
            incremental = false,
            compressionLevel = CompressionLevel.NORMAL
        )

        val mockEngine = mockk<BackupEngine>(relaxed = true)
        every { engineFactory.createForCurrentMode() } returns mockEngine
        
        // Valid apps should get plans
        coEvery { 
            incrementalStrategy.createIncremental(
                match { it.value.startsWith("com.") },
                any()
            ) 
        } returns BackupPlan.Full(emptyList())
        
        // Malicious apps should throw
        coEvery { 
            incrementalStrategy.createIncremental(
                match { it.value.contains("..") || it.value.contains("/") },
                any()
            ) 
        } throws SecurityException("Invalid app ID format")
        
        coEvery {
            incrementalStrategy.executeBackupPlan(any(), any(), any())
        } returns ExecutionResult(
            filesProcessed = 0,
            bytesCopied = 0,
            errors = emptyList()
        )

        val result = orchestrator.executeBackup(request)
        
        // Result should indicate failures for malicious apps
        if (result is com.obsidianbackup.model.BackupResult.Failure) {
            assertTrue(
                result.appsFailed.size >= 2,
                "Should fail at least the 2 malicious apps"
            )
        }
    }
}
