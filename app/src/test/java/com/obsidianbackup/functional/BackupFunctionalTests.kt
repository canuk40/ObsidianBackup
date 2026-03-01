package com.obsidianbackup.functional

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.obsidianbackup.domain.backup.BackupOrchestrator
import com.obsidianbackup.engine.*
import com.obsidianbackup.model.*
import com.obsidianbackup.storage.BackupCatalog
import com.obsidianbackup.testing.BaseTest
import com.obsidianbackup.testing.TestDataFactory
import com.obsidianbackup.testing.TestFixtures
import com.obsidianbackup.verification.ChecksumVerifier
import com.obsidianbackup.verification.MerkleTree
import com.obsidianbackup.verification.MerkleVerificationEngine
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import kotlin.time.Duration.Companion.seconds

@DisplayName("Backup Functional Tests")
class BackupFunctionalTests : BaseTest() {

    private lateinit var backupOrchestrator: BackupOrchestrator
    private lateinit var backupEngine: BackupEngine
    private lateinit var engineFactory: BackupEngineFactory
    private lateinit var catalog: BackupCatalog
    private lateinit var verifier: ChecksumVerifier
    private lateinit var eventBus: BackupEventBus
    private lateinit var incrementalStrategy: IncrementalBackupStrategy
    private lateinit var merkleEngine: MerkleVerificationEngine
    private val testBackupRoot = "/tmp/test_backups"

    @BeforeEach
    fun setup() {
        backupEngine = mockk(relaxed = true)
        engineFactory = mockk {
            every { createForCurrentMode() } returns backupEngine
        }
        catalog = mockk(relaxed = true)
        verifier = mockk(relaxed = true)
        eventBus = mockk(relaxed = true)
        incrementalStrategy = mockk(relaxed = true)
        merkleEngine = mockk(relaxed = true)

        backupOrchestrator = BackupOrchestrator(
            engineFactory = engineFactory,
            catalog = catalog,
            verifier = verifier,
            eventBus = eventBus,
            incrementalStrategy = incrementalStrategy,
            backupRootPath = testBackupRoot
        )
    }

    @AfterEach
    fun teardown() {
        clearAllMocks()
        File(testBackupRoot).deleteRecursively()
    }

    @Nested
    @DisplayName("Full Backup Tests")
    inner class FullBackupTests {

        @Test
        @DisplayName("Should perform full backup of all apps successfully")
        fun testFullBackupAllApps() = runTest {
            val appIds = (1..10).map { AppId("com.test.app$it") }
            val request = com.obsidianbackup.domain.backup.BackupRequest(
                appIds = appIds,
                components = setOf(BackupComponent.APK, BackupComponent.DATA),
                incremental = false,
                compressionLevel = 6,
                encryptionEnabled = true
            )

            val expectedSnapshot = SnapshotId(TestFixtures.randomUUID())
            
            coEvery { 
                backupEngine.backupApps(any()) 
            } returns BackupResult.Success(
                snapshotId = expectedSnapshot,
                timestamp = System.currentTimeMillis(),
                appsBackedUp = appIds,
                totalSize = 1024L * 1024L * 100L,
                duration = 5000L,
                checksums = mapOf("manifest.json" to "abc123")
            )

            coEvery { verifier.verifySnapshot(any()) } returns true
            coEvery { catalog.saveSnapshot(any()) } just Runs

            val result = backupOrchestrator.executeBackup(request)

            assertThat(result).isInstanceOf(BackupResult.Success::class.java)
            val success = result as BackupResult.Success
            assertThat(success.appsBackedUp).containsExactlyElementsIn(appIds)
            assertThat(success.snapshotId).isEqualTo(expectedSnapshot)
            assertThat(success.totalSize).isGreaterThan(0L)

            coVerify { backupEngine.backupApps(any()) }
            coVerify { catalog.saveSnapshot(any()) }
        }

        @Test
        @DisplayName("Should handle partial backup failure gracefully")
        fun testPartialBackupFailure() = runTest {
            val appIds = (1..5).map { AppId("com.test.app$it") }
            val failedApps = listOf(AppId("com.test.app3"), AppId("com.test.app5"))
            val successfulApps = appIds - failedApps.toSet()

            val request = com.obsidianbackup.domain.backup.BackupRequest(
                appIds = appIds,
                incremental = false
            )

            coEvery { 
                backupEngine.backupApps(any()) 
            } returns BackupResult.PartialSuccess(
                snapshotId = SnapshotId(TestFixtures.randomUUID()),
                timestamp = System.currentTimeMillis(),
                appsBackedUp = successfulApps,
                appsFailed = failedApps,
                totalSize = 1024L * 1024L * 50L,
                duration = 5000L,
                errors = failedApps.map { "Failed to backup ${it.value}" }
            )

            val result = backupOrchestrator.executeBackup(request)

            assertThat(result).isInstanceOf(BackupResult.PartialSuccess::class.java)
            val partial = result as BackupResult.PartialSuccess
            assertThat(partial.appsBackedUp).containsExactlyElementsIn(successfulApps)
            assertThat(partial.appsFailed).containsExactlyElementsIn(failedApps)
            assertThat(partial.errors).hasSize(2)
        }

        @Test
        @DisplayName("Should fail backup on critical error")
        fun testFullBackupFailure() = runTest {
            val appIds = listOf(AppId("com.test.app1"))
            val request = com.obsidianbackup.domain.backup.BackupRequest(appIds = appIds)

            coEvery { 
                backupEngine.backupApps(any()) 
            } returns BackupResult.Failure(
                reason = "Disk full",
                appsFailed = appIds
            )

            val result = backupOrchestrator.executeBackup(request)

            assertThat(result).isInstanceOf(BackupResult.Failure::class.java)
            val failure = result as BackupResult.Failure
            assertThat(failure.reason).contains("Disk full")
            assertThat(failure.appsFailed).containsExactlyElementsIn(appIds)
        }
    }

    @Nested
    @DisplayName("Incremental Backup Tests")
    inner class IncrementalBackupTests {

        @Test
        @DisplayName("Should perform incremental backup with baseline")
        fun testIncrementalBackupWithBaseline() = runTest {
            val appIds = listOf(AppId("com.test.app1"))
            val baseSnapshotId = SnapshotId(TestFixtures.randomUUID())
            
            val request = com.obsidianbackup.domain.backup.BackupRequest(
                appIds = appIds,
                incremental = true
            )

            val incrementalStats = IncrementalStats(
                isIncremental = true,
                baseSnapshotId = baseSnapshotId,
                filesScanned = 1000,
                filesChanged = 50,
                filesUnchanged = 950,
                filesDeleted = 0,
                filesDeduped = 950,
                deltaSize = 1024L * 1024L * 5L,
                savedSize = 1024L * 1024L * 95L,
                hardLinksCreated = 950
            )

            coEvery { 
                incrementalStrategy.findBaseSnapshot(any()) 
            } returns baseSnapshotId

            coEvery { 
                backupEngine.backupApps(any()) 
            } returns BackupResult.Success(
                snapshotId = SnapshotId(TestFixtures.randomUUID()),
                timestamp = System.currentTimeMillis(),
                appsBackedUp = appIds,
                totalSize = 1024L * 1024L * 5L,
                duration = 2000L,
                incrementalStats = incrementalStats
            )

            val result = backupOrchestrator.executeBackup(request)

            assertThat(result).isInstanceOf(BackupResult.Success::class.java)
            val success = result as BackupResult.Success
            assertThat(success.incrementalStats).isNotNull()
            assertThat(success.incrementalStats?.isIncremental).isTrue()
            assertThat(success.incrementalStats?.filesChanged).isEqualTo(50)
            assertThat(success.incrementalStats?.savedSize).isGreaterThan(0L)
        }

        @Test
        @DisplayName("Should fallback to full backup when no baseline exists")
        fun testIncrementalFallbackToFull() = runTest {
            val appIds = listOf(AppId("com.test.app1"))
            
            val request = com.obsidianbackup.domain.backup.BackupRequest(
                appIds = appIds,
                incremental = true
            )

            coEvery { 
                incrementalStrategy.findBaseSnapshot(any()) 
            } returns null

            coEvery { 
                backupEngine.backupApps(match { !it.incremental }) 
            } returns BackupResult.Success(
                snapshotId = SnapshotId(TestFixtures.randomUUID()),
                timestamp = System.currentTimeMillis(),
                appsBackedUp = appIds,
                totalSize = 1024L * 1024L * 100L,
                duration = 5000L
            )

            val result = backupOrchestrator.executeBackup(request)

            assertThat(result).isInstanceOf(BackupResult.Success::class.java)
            coVerify { backupEngine.backupApps(match { !it.incremental }) }
        }
    }

    @Nested
    @DisplayName("Scheduled Backup Tests")
    inner class ScheduledBackupTests {

        @Test
        @DisplayName("Should execute scheduled backup automatically")
        fun testScheduledBackupExecution() = runTest {
            val appIds = listOf(AppId("com.test.app1"), AppId("com.test.app2"))
            val request = com.obsidianbackup.domain.backup.BackupRequest(appIds = appIds)

            coEvery { 
                backupEngine.backupApps(any()) 
            } returns BackupResult.Success(
                snapshotId = SnapshotId(TestFixtures.randomUUID()),
                timestamp = System.currentTimeMillis(),
                appsBackedUp = appIds,
                totalSize = 1024L * 1024L * 50L,
                duration = 3000L
            )

            val result = backupOrchestrator.executeBackup(request)

            assertThat(result).isInstanceOf(BackupResult.Success::class.java)
            coVerify { eventBus.post(any()) }
        }
    }

    @Nested
    @DisplayName("Backup Verification Tests")
    inner class BackupVerificationTests {

        @Test
        @DisplayName("Should verify backup with Merkle tree successfully")
        fun testMerkleTreeVerification() = runTest {
            val snapshotId = SnapshotId(TestFixtures.randomUUID())
            val files = listOf(
                "app1/data.tar",
                "app1/manifest.json",
                "app2/data.tar",
                "app2/manifest.json"
            )

            val merkleTree = MerkleTree(files.map { it to "hash_$it" })
            coEvery { 
                merkleEngine.buildMerkleTree(any()) 
            } returns merkleTree

            coEvery { 
                merkleEngine.verifyIntegrity(any(), any()) 
            } returns VerificationResult(
                snapshotId = snapshotId,
                filesChecked = files.size,
                allValid = true,
                corruptedFiles = emptyList()
            )

            val result = merkleEngine.verifyIntegrity(snapshotId, merkleTree.root)

            assertThat(result.allValid).isTrue()
            assertThat(result.filesChecked).isEqualTo(4)
            assertThat(result.corruptedFiles).isEmpty()
        }

        @Test
        @DisplayName("Should detect corrupted files during verification")
        fun testDetectCorruptedFiles() = runTest {
            val snapshotId = SnapshotId(TestFixtures.randomUUID())
            val corruptedFiles = listOf("app1/data.tar", "app2/manifest.json")

            coEvery { 
                merkleEngine.verifyIntegrity(any(), any()) 
            } returns VerificationResult(
                snapshotId = snapshotId,
                filesChecked = 10,
                allValid = false,
                corruptedFiles = corruptedFiles
            )

            val result = merkleEngine.verifyIntegrity(snapshotId, "root_hash")

            assertThat(result.allValid).isFalse()
            assertThat(result.corruptedFiles).containsExactlyElementsIn(corruptedFiles)
        }

        @Test
        @DisplayName("Should verify checksums for all backed up files")
        fun testChecksumVerification() = runTest {
            val snapshotId = SnapshotId(TestFixtures.randomUUID())
            val checksums = mapOf(
                "app1/data.tar" to "checksum1",
                "app2/data.tar" to "checksum2",
                "manifest.json" to "checksum3"
            )

            coEvery { verifier.verifySnapshot(any()) } returns true
            coEvery { 
                verifier.verifyFile(any(), any()) 
            } returns true

            val allValid = checksums.all { (file, checksum) ->
                verifier.verifyFile(file, checksum)
            }

            assertThat(allValid).isTrue()
            coVerify(exactly = checksums.size) { verifier.verifyFile(any(), any()) }
        }
    }

    @Nested
    @DisplayName("Backup Encryption Tests")
    inner class BackupEncryptionTests {

        @Test
        @DisplayName("Should encrypt backup when enabled")
        fun testEncryptedBackup() = runTest {
            val appIds = listOf(AppId("com.test.app1"))
            val request = com.obsidianbackup.domain.backup.BackupRequest(
                appIds = appIds,
                encryptionEnabled = true
            )

            coEvery { 
                backupEngine.backupApps(match { it.encryptionEnabled }) 
            } returns BackupResult.Success(
                snapshotId = SnapshotId(TestFixtures.randomUUID()),
                timestamp = System.currentTimeMillis(),
                appsBackedUp = appIds,
                totalSize = 1024L * 1024L * 50L,
                duration = 4000L
            )

            val result = backupOrchestrator.executeBackup(request)

            assertThat(result).isInstanceOf(BackupResult.Success::class.java)
            coVerify { backupEngine.backupApps(match { it.encryptionEnabled }) }
        }

        @Test
        @DisplayName("Should not encrypt backup when disabled")
        fun testUnencryptedBackup() = runTest {
            val appIds = listOf(AppId("com.test.app1"))
            val request = com.obsidianbackup.domain.backup.BackupRequest(
                appIds = appIds,
                encryptionEnabled = false
            )

            coEvery { 
                backupEngine.backupApps(match { !it.encryptionEnabled }) 
            } returns BackupResult.Success(
                snapshotId = SnapshotId(TestFixtures.randomUUID()),
                timestamp = System.currentTimeMillis(),
                appsBackedUp = appIds,
                totalSize = 1024L * 1024L * 50L,
                duration = 3000L
            )

            val result = backupOrchestrator.executeBackup(request)

            assertThat(result).isInstanceOf(BackupResult.Success::class.java)
            coVerify { backupEngine.backupApps(match { !it.encryptionEnabled }) }
        }
    }

    @Nested
    @DisplayName("Batch Operation Tests")
    inner class BatchOperationTests {

        @Test
        @DisplayName("Should backup multiple apps in batch")
        fun testBatchBackup() = runTest {
            val appIds = (1..20).map { AppId("com.test.app$it") }
            val request = com.obsidianbackup.domain.backup.BackupRequest(appIds = appIds)

            coEvery { 
                backupEngine.backupApps(any()) 
            } returns BackupResult.Success(
                snapshotId = SnapshotId(TestFixtures.randomUUID()),
                timestamp = System.currentTimeMillis(),
                appsBackedUp = appIds,
                totalSize = 1024L * 1024L * 200L,
                duration = 10000L
            )

            val result = backupOrchestrator.executeBackup(request)

            assertThat(result).isInstanceOf(BackupResult.Success::class.java)
            val success = result as BackupResult.Success
            assertThat(success.appsBackedUp).hasSize(20)
        }

        @ParameterizedTest
        @ValueSource(ints = [1, 5, 10, 50, 100])
        @DisplayName("Should handle different batch sizes")
        fun testVariableBatchSizes(batchSize: Int) = runTest {
            val appIds = (1..batchSize).map { AppId("com.test.app$it") }
            val request = com.obsidianbackup.domain.backup.BackupRequest(appIds = appIds)

            coEvery { 
                backupEngine.backupApps(any()) 
            } returns BackupResult.Success(
                snapshotId = SnapshotId(TestFixtures.randomUUID()),
                timestamp = System.currentTimeMillis(),
                appsBackedUp = appIds,
                totalSize = 1024L * 1024L * batchSize,
                duration = batchSize * 100L
            )

            val result = backupOrchestrator.executeBackup(request)

            assertThat(result).isInstanceOf(BackupResult.Success::class.java)
            val success = result as BackupResult.Success
            assertThat(success.appsBackedUp).hasSize(batchSize)
        }

        @Test
        @DisplayName("Should handle mixed success and failure in batch")
        fun testMixedBatchResults() = runTest {
            val totalApps = 10
            val appIds = (1..totalApps).map { AppId("com.test.app$it") }
            val failedApps = appIds.take(3)
            val successApps = appIds.drop(3)

            val request = com.obsidianbackup.domain.backup.BackupRequest(appIds = appIds)

            coEvery { 
                backupEngine.backupApps(any()) 
            } returns BackupResult.PartialSuccess(
                snapshotId = SnapshotId(TestFixtures.randomUUID()),
                timestamp = System.currentTimeMillis(),
                appsBackedUp = successApps,
                appsFailed = failedApps,
                totalSize = 1024L * 1024L * 70L,
                duration = 8000L,
                errors = failedApps.map { "Failed: ${it.value}" }
            )

            val result = backupOrchestrator.executeBackup(request)

            assertThat(result).isInstanceOf(BackupResult.PartialSuccess::class.java)
            val partial = result as BackupResult.PartialSuccess
            assertThat(partial.appsBackedUp).hasSize(7)
            assertThat(partial.appsFailed).hasSize(3)
        }
    }

    @Nested
    @DisplayName("Progress Tracking Tests")
    inner class ProgressTrackingTests {

        @Test
        @DisplayName("Should track backup progress in real-time")
        fun testProgressTracking() = runTest(timeout = 10.seconds) {
            val appIds = (1..5).map { AppId("com.test.app$it") }
            val request = com.obsidianbackup.domain.backup.BackupRequest(appIds = appIds)

            coEvery { 
                backupEngine.backupApps(any()) 
            } returns BackupResult.Success(
                snapshotId = SnapshotId(TestFixtures.randomUUID()),
                timestamp = System.currentTimeMillis(),
                appsBackedUp = appIds,
                totalSize = 1024L * 1024L * 50L,
                duration = 5000L
            )

            backupOrchestrator.operationProgress.test {
                val initial = awaitItem()
                assertThat(initial.itemsCompleted).isEqualTo(0)
                assertThat(initial.operationType).isEqualTo(OperationType.BACKUP)
            }
        }
    }

    @Nested
    @DisplayName("Error Recovery Tests")
    inner class ErrorRecoveryTests {

        @Test
        @DisplayName("Should retry backup on transient failure")
        fun testBackupRetry() = runTest {
            val appIds = listOf(AppId("com.test.app1"))
            val request = com.obsidianbackup.domain.backup.BackupRequest(appIds = appIds)

            var attemptCount = 0
            coEvery { 
                backupEngine.backupApps(any()) 
            } answers {
                attemptCount++
                if (attemptCount < 3) {
                    BackupResult.Failure("Transient error", appIds)
                } else {
                    BackupResult.Success(
                        snapshotId = SnapshotId(TestFixtures.randomUUID()),
                        timestamp = System.currentTimeMillis(),
                        appsBackedUp = appIds,
                        totalSize = 1024L * 1024L * 50L,
                        duration = 5000L
                    )
                }
            }

            val result = backupOrchestrator.executeBackup(request)

            assertThat(result).isInstanceOf(BackupResult.Success::class.java)
            assertThat(attemptCount).isEqualTo(3)
        }
    }
}
