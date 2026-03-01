package com.obsidianbackup.functional

import com.google.common.truth.Truth.assertThat
import com.obsidianbackup.engine.*
import com.obsidianbackup.engine.restore.RestoreJournal
import com.obsidianbackup.engine.restore.RestoreTransaction
import com.obsidianbackup.engine.restore.StepType
import com.obsidianbackup.model.*
import com.obsidianbackup.storage.BackupCatalog
import com.obsidianbackup.testing.BaseTest
import com.obsidianbackup.testing.TestFixtures
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

@DisplayName("Restore Functional Tests")
class RestoreFunctionalTests : BaseTest() {

    private lateinit var restoreEngine: TransactionalRestoreEngine
    private lateinit var shellExecutor: ShellExecutor
    private lateinit var journal: RestoreJournal
    private lateinit var catalog: BackupCatalog
    private val testBackupRoot = "/tmp/test_restores"

    @BeforeEach
    fun setup() {
        shellExecutor = mockk(relaxed = true)
        journal = mockk(relaxed = true)
        catalog = mockk(relaxed = true)

        restoreEngine = TransactionalRestoreEngine(
            shellExecutor = shellExecutor,
            journal = journal,
            catalog = catalog,
            backupRootPath = testBackupRoot
        )

        File(testBackupRoot).mkdirs()
    }

    @AfterEach
    fun teardown() {
        clearAllMocks()
        File(testBackupRoot).deleteRecursively()
    }

    @Nested
    @DisplayName("Full Restore Tests")
    inner class FullRestoreTests {

        @Test
        @DisplayName("Should restore all apps successfully")
        fun testFullRestoreSuccess() = runTest {
            val appIds = (1..5).map { AppId("com.test.app$it") }
            val snapshotId = SnapshotId(TestFixtures.randomUUID())
            val request = RestoreRequest(
                snapshotId = snapshotId,
                appIds = appIds,
                components = setOf(BackupComponent.APK, BackupComponent.DATA)
            )

            val transaction = RestoreTransaction(
                transactionId = TestFixtures.randomUUID(),
                metadata = RestoreTransaction.TransactionMetadata(
                    snapshotId = BackupId(snapshotId.value),
                    startTime = System.currentTimeMillis(),
                    appCount = appIds.size
                )
            )

            coEvery { journal.beginTransaction(any()) } returns transaction
            coEvery { journal.finalizeTransaction(any()) } just Runs
            coEvery { 
                shellExecutor.execute(any()) 
            } returns ShellResult.Success(output = "OK", exitCode = 0)

            val result = restoreEngine.restoreApps(request)

            assertThat(result).isInstanceOf(RestoreResult.Success::class.java)
            val success = result as RestoreResult.Success
            assertThat(success.appsRestored).containsExactlyElementsIn(appIds)
            assertThat(success.warnings).isEmpty()

            coVerify { journal.beginTransaction(BackupId(snapshotId.value)) }
            coVerify { journal.finalizeTransaction(transaction) }
        }

        @Test
        @DisplayName("Should restore apps with warnings")
        fun testRestoreWithWarnings() = runTest {
            val appIds = listOf(AppId("com.test.app1"), AppId("com.test.app2"))
            val snapshotId = SnapshotId(TestFixtures.randomUUID())
            val request = RestoreRequest(
                snapshotId = snapshotId,
                appIds = appIds
            )

            val transaction = RestoreTransaction(
                transactionId = TestFixtures.randomUUID(),
                metadata = RestoreTransaction.TransactionMetadata(
                    snapshotId = BackupId(snapshotId.value),
                    startTime = System.currentTimeMillis(),
                    appCount = appIds.size
                )
            )

            coEvery { journal.beginTransaction(any()) } returns transaction
            coEvery { journal.finalizeTransaction(any()) } just Runs
            coEvery { 
                shellExecutor.execute(any()) 
            } returns ShellResult.Success(output = "Warning: permissions changed", exitCode = 0)

            val result = restoreEngine.restoreApps(request)

            assertThat(result).isInstanceOf(RestoreResult.Success::class.java)
            val success = result as RestoreResult.Success
            assertThat(success.appsRestored).hasSize(2)
        }
    }

    @Nested
    @DisplayName("Selective Restore Tests")
    inner class SelectiveRestoreTests {

        @Test
        @DisplayName("Should restore only APK component")
        fun testRestoreApkOnly() = runTest {
            val appIds = listOf(AppId("com.test.app1"))
            val snapshotId = SnapshotId(TestFixtures.randomUUID())
            val request = RestoreRequest(
                snapshotId = snapshotId,
                appIds = appIds,
                components = setOf(BackupComponent.APK)
            )

            val transaction = mockTransaction(snapshotId, appIds.size)
            coEvery { journal.beginTransaction(any()) } returns transaction
            coEvery { journal.finalizeTransaction(any()) } just Runs
            coEvery { 
                shellExecutor.execute(match { it.contains("pm install") }) 
            } returns ShellResult.Success("Success", 0)

            val result = restoreEngine.restoreApps(request)

            assertThat(result).isInstanceOf(RestoreResult.Success::class.java)
            coVerify { 
                shellExecutor.execute(match { it.contains("pm install") }) 
            }
            coVerify(exactly = 0) { 
                shellExecutor.execute(match { it.contains("tar -xf") && it.contains("data") }) 
            }
        }

        @Test
        @DisplayName("Should restore only DATA component")
        fun testRestoreDataOnly() = runTest {
            val appIds = listOf(AppId("com.test.app1"))
            val snapshotId = SnapshotId(TestFixtures.randomUUID())
            val request = RestoreRequest(
                snapshotId = snapshotId,
                appIds = appIds,
                components = setOf(BackupComponent.DATA)
            )

            val transaction = mockTransaction(snapshotId, appIds.size)
            coEvery { journal.beginTransaction(any()) } returns transaction
            coEvery { journal.finalizeTransaction(any()) } just Runs
            coEvery { 
                shellExecutor.execute(match { it.contains("tar -xf") && it.contains("data") }) 
            } returns ShellResult.Success("Success", 0)

            val result = restoreEngine.restoreApps(request)

            assertThat(result).isInstanceOf(RestoreResult.Success::class.java)
            coVerify { 
                shellExecutor.execute(match { it.contains("tar -xf") && it.contains("data") }) 
            }
        }

        @Test
        @DisplayName("Should restore subset of apps from snapshot")
        fun testRestoreSubset() = runTest {
            val allAppIds = (1..10).map { AppId("com.test.app$it") }
            val restoreAppIds = allAppIds.take(3)
            val snapshotId = SnapshotId(TestFixtures.randomUUID())
            val request = RestoreRequest(
                snapshotId = snapshotId,
                appIds = restoreAppIds
            )

            val transaction = mockTransaction(snapshotId, restoreAppIds.size)
            coEvery { journal.beginTransaction(any()) } returns transaction
            coEvery { journal.finalizeTransaction(any()) } just Runs
            coEvery { shellExecutor.execute(any()) } returns ShellResult.Success("Success", 0)

            val result = restoreEngine.restoreApps(request)

            assertThat(result).isInstanceOf(RestoreResult.Success::class.java)
            val success = result as RestoreResult.Success
            assertThat(success.appsRestored).containsExactlyElementsIn(restoreAppIds)
            assertThat(success.appsRestored).hasSize(3)
        }
    }

    @Nested
    @DisplayName("Transactional Restore Tests")
    inner class TransactionalRestoreTests {

        @Test
        @DisplayName("Should maintain ACID properties during restore")
        fun testAcidProperties() = runTest {
            val appIds = listOf(AppId("com.test.app1"))
            val snapshotId = SnapshotId(TestFixtures.randomUUID())
            val request = RestoreRequest(snapshotId = snapshotId, appIds = appIds)

            val transaction = mockTransaction(snapshotId, appIds.size)
            coEvery { journal.beginTransaction(any()) } returns transaction
            coEvery { journal.finalizeTransaction(any()) } just Runs
            coEvery { shellExecutor.execute(any()) } returns ShellResult.Success("Success", 0)

            val result = restoreEngine.restoreApps(request)

            assertThat(result).isInstanceOf(RestoreResult.Success::class.java)
            
            inOrder {
                coVerify { journal.beginTransaction(any()) }
                coVerify { shellExecutor.execute(any()) }
                coVerify { journal.finalizeTransaction(transaction) }
            }
        }

        @Test
        @DisplayName("Should create safety backup before restore")
        fun testSafetyBackupCreation() = runTest {
            val appIds = listOf(AppId("com.test.app1"))
            val snapshotId = SnapshotId(TestFixtures.randomUUID())
            val request = RestoreRequest(snapshotId = snapshotId, appIds = appIds)

            val transaction = mockTransaction(snapshotId, appIds.size)
            val safetyBackups = mutableListOf<File>()
            
            coEvery { journal.beginTransaction(any()) } returns transaction
            coEvery { 
                transaction.createSafetyBackup(any(), any()) 
            } answers {
                safetyBackups.add(secondArg())
            }
            coEvery { shellExecutor.execute(any()) } returns ShellResult.Success("Success", 0)

            restoreEngine.restoreApps(request)

            assertThat(safetyBackups).isNotEmpty()
        }

        @Test
        @DisplayName("Should maintain transaction isolation")
        fun testTransactionIsolation() = runTest {
            val appIds = listOf(AppId("com.test.app1"))
            val snapshotId = SnapshotId(TestFixtures.randomUUID())
            val request = RestoreRequest(snapshotId = snapshotId, appIds = appIds)

            val transaction1 = mockTransaction(snapshotId, appIds.size)
            coEvery { journal.beginTransaction(any()) } returns transaction1
            coEvery { journal.finalizeTransaction(any()) } just Runs
            coEvery { shellExecutor.execute(any()) } returns ShellResult.Success("Success", 0)

            val result = restoreEngine.restoreApps(request)

            assertThat(result).isInstanceOf(RestoreResult.Success::class.java)
            coVerify(exactly = 1) { journal.beginTransaction(any()) }
        }
    }

    @Nested
    @DisplayName("Rollback Tests")
    inner class RollbackTests {

        @Test
        @DisplayName("Should rollback on single app failure")
        fun testRollbackSingleApp() = runTest {
            val appIds = listOf(AppId("com.test.app1"), AppId("com.test.app2"))
            val snapshotId = SnapshotId(TestFixtures.randomUUID())
            val request = RestoreRequest(snapshotId = snapshotId, appIds = appIds)

            val transaction = mockTransaction(snapshotId, appIds.size)
            coEvery { journal.beginTransaction(any()) } returns transaction
            coEvery { journal.finalizeTransaction(any()) } just Runs
            
            var callCount = 0
            coEvery { shellExecutor.execute(any()) } answers {
                callCount++
                if (callCount == 2) {
                    throw RuntimeException("Restore failed for app2")
                }
                ShellResult.Success("Success", 0)
            }
            
            coEvery { transaction.rollbackApp(any(), any()) } just Runs

            val result = restoreEngine.restoreApps(request)

            assertThat(result).isInstanceOf(RestoreResult.PartialSuccess::class.java)
            val partial = result as RestoreResult.PartialSuccess
            assertThat(partial.appsRestored).hasSize(1)
            assertThat(partial.appsFailed).hasSize(1)
            
            coVerify { transaction.rollbackApp(AppId("com.test.app2"), shellExecutor) }
        }

        @Test
        @DisplayName("Should rollback all apps on critical failure")
        fun testRollbackAll() = runTest {
            val appIds = listOf(AppId("com.test.app1"), AppId("com.test.app2"))
            val snapshotId = SnapshotId(TestFixtures.randomUUID())
            val request = RestoreRequest(snapshotId = snapshotId, appIds = appIds)

            val transaction = mockTransaction(snapshotId, appIds.size)
            coEvery { journal.beginTransaction(any()) } returns transaction
            coEvery { transaction.rollbackAll(any()) } returns appIds
            coEvery { journal.rollback(any()) } just Runs
            coEvery { 
                shellExecutor.execute(any()) 
            } throws OutOfMemoryError("Critical failure")

            val result = restoreEngine.restoreApps(request)

            assertThat(result).isInstanceOf(RestoreResult.Failure::class.java)
            val failure = result as RestoreResult.Failure
            assertThat(failure.reason).contains("Critical restore failure")
            assertThat(failure.reason).contains("Rolled back ${appIds.size} apps")
            
            coVerify { transaction.rollbackAll(shellExecutor) }
            coVerify { journal.rollback(transaction) }
        }

        @Test
        @DisplayName("Should restore from safety backup on rollback")
        fun testRestoreFromSafetyBackup() = runTest {
            val appIds = listOf(AppId("com.test.app1"))
            val snapshotId = SnapshotId(TestFixtures.randomUUID())
            val request = RestoreRequest(snapshotId = snapshotId, appIds = appIds)

            val transaction = mockTransaction(snapshotId, appIds.size)
            val safetyBackupRestored = mutableListOf<AppId>()
            
            coEvery { journal.beginTransaction(any()) } returns transaction
            coEvery { 
                transaction.rollbackApp(any(), any()) 
            } answers {
                safetyBackupRestored.add(firstArg())
            }
            coEvery { 
                shellExecutor.execute(any()) 
            } throws RuntimeException("Restore failed")

            restoreEngine.restoreApps(request)

            assertThat(safetyBackupRestored).containsExactly(appIds[0])
        }
    }

    @Nested
    @DisplayName("SELinux Context Tests")
    inner class SelinuxContextTests {

        @Test
        @DisplayName("Should restore SELinux contexts correctly")
        fun testSelinuxContextRestore() = runTest {
            val appIds = listOf(AppId("com.test.app1"))
            val snapshotId = SnapshotId(TestFixtures.randomUUID())
            val request = RestoreRequest(snapshotId = snapshotId, appIds = appIds)

            val transaction = mockTransaction(snapshotId, appIds.size)
            val selinuxCommands = mutableListOf<String>()
            
            coEvery { journal.beginTransaction(any()) } returns transaction
            coEvery { journal.finalizeTransaction(any()) } just Runs
            coEvery { 
                shellExecutor.execute(capture(selinuxCommands)) 
            } returns ShellResult.Success("Success", 0)

            restoreEngine.restoreApps(request)

            val hasSelinuxRestore = selinuxCommands.any { 
                it.contains("restorecon") || it.contains("chcon")
            }
            assertThat(hasSelinuxRestore).isTrue()
        }

        @Test
        @DisplayName("Should handle SELinux context failure gracefully")
        fun testSelinuxContextFailure() = runTest {
            val appIds = listOf(AppId("com.test.app1"))
            val snapshotId = SnapshotId(TestFixtures.randomUUID())
            val request = RestoreRequest(snapshotId = snapshotId, appIds = appIds)

            val transaction = mockTransaction(snapshotId, appIds.size)
            coEvery { journal.beginTransaction(any()) } returns transaction
            coEvery { journal.finalizeTransaction(any()) } just Runs
            coEvery { 
                shellExecutor.execute(match { it.contains("restorecon") }) 
            } returns ShellResult.Failure("SELinux not available", 1)
            coEvery { 
                shellExecutor.execute(match { !it.contains("restorecon") }) 
            } returns ShellResult.Success("Success", 0)

            val result = restoreEngine.restoreApps(request)

            assertThat(result).isInstanceOf(RestoreResult.Success::class.java)
            val success = result as RestoreResult.Success
            assertThat(success.warnings).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("Dry Run Tests")
    inner class DryRunTests {

        @Test
        @DisplayName("Should perform dry run without making changes")
        fun testDryRun() = runTest {
            val appIds = listOf(AppId("com.test.app1"))
            val snapshotId = SnapshotId(TestFixtures.randomUUID())
            val request = RestoreRequest(
                snapshotId = snapshotId,
                appIds = appIds,
                dryRun = true
            )

            val transaction = mockTransaction(snapshotId, appIds.size)
            coEvery { journal.beginTransaction(any()) } returns transaction

            val result = restoreEngine.restoreApps(request)

            coVerify(exactly = 0) { shellExecutor.execute(any()) }
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    inner class PerformanceTests {

        @ParameterizedTest
        @ValueSource(ints = [1, 10, 50, 100])
        @DisplayName("Should handle different restore sizes efficiently")
        fun testDifferentRestoreSizes(appCount: Int) = runTest {
            val appIds = (1..appCount).map { AppId("com.test.app$it") }
            val snapshotId = SnapshotId(TestFixtures.randomUUID())
            val request = RestoreRequest(snapshotId = snapshotId, appIds = appIds)

            val transaction = mockTransaction(snapshotId, appIds.size)
            coEvery { journal.beginTransaction(any()) } returns transaction
            coEvery { journal.finalizeTransaction(any()) } just Runs
            coEvery { shellExecutor.execute(any()) } returns ShellResult.Success("Success", 0)

            val startTime = System.currentTimeMillis()
            val result = restoreEngine.restoreApps(request)
            val duration = System.currentTimeMillis() - startTime

            assertThat(result).isInstanceOf(RestoreResult.Success::class.java)
            val success = result as RestoreResult.Success
            assertThat(success.appsRestored).hasSize(appCount)
            assertThat(duration).isLessThan(appCount * 1000L)
        }
    }

    private fun mockTransaction(snapshotId: SnapshotId, appCount: Int): RestoreTransaction {
        return RestoreTransaction(
            transactionId = TestFixtures.randomUUID(),
            metadata = RestoreTransaction.TransactionMetadata(
                snapshotId = BackupId(snapshotId.value),
                startTime = System.currentTimeMillis(),
                appCount = appCount
            )
        )
    }
}
