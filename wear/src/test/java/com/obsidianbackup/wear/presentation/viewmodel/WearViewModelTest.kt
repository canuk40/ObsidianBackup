package com.obsidianbackup.wear.presentation.viewmodel

import com.obsidianbackup.wear.data.BackupProgress
import com.obsidianbackup.wear.data.BackupStatus
import com.obsidianbackup.wear.data.DataLayerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WearViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeBackupStatus = MutableStateFlow(BackupStatus())
    private val fakeBackupProgress = MutableStateFlow(BackupProgress())

    private lateinit var repository: DataLayerRepository
    private lateinit var viewModel: WearViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true) {
            coEvery { backupStatus } returns fakeBackupStatus
            coEvery { backupProgress } returns fakeBackupProgress
            coEvery { isPhoneConnected() } returns false
        }

        viewModel = WearViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init calls requestStatus on repository`() = runTest {
        advanceUntilIdle()
        coVerify(atLeast = 1) { repository.requestStatus() }
    }

    @Test
    fun `init calls isPhoneConnected on repository`() = runTest {
        advanceUntilIdle()
        coVerify(atLeast = 1) { repository.isPhoneConnected() }
    }

    @Test
    fun `isPhoneConnected state reflects repository response when true`() = runTest {
        coEvery { repository.isPhoneConnected() } returns true

        // Recreate ViewModel so it re-runs init with new stubbing
        val vm = WearViewModel(repository)
        advanceUntilIdle()

        assertTrue(vm.isPhoneConnected.value)
    }

    @Test
    fun `isPhoneConnected state is false when repository returns false`() = runTest {
        coEvery { repository.isPhoneConnected() } returns false

        val vm = WearViewModel(repository)
        advanceUntilIdle()

        assertFalse(vm.isPhoneConnected.value)
    }

    @Test
    fun `triggerBackup resets isLoading to false after success`() = runTest {
        coEvery { repository.requestBackup() } returns true

        viewModel.triggerBackup()
        advanceUntilIdle()

        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `triggerBackup resets isLoading to false after failure`() = runTest {
        coEvery { repository.requestBackup() } returns false

        viewModel.triggerBackup()
        advanceUntilIdle()

        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `triggerBackup calls requestBackup on repository`() = runTest {
        coEvery { repository.requestBackup() } returns true

        viewModel.triggerBackup()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.requestBackup() }
    }

    @Test
    fun `cancelBackup delegates to repository`() = runTest {
        viewModel.cancelBackup()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.cancelBackup() }
    }

    @Test
    fun `requestStatus delegates to repository`() = runTest {
        viewModel.requestStatus()
        advanceUntilIdle()

        // requestStatus is called once in init and once explicitly
        coVerify(atLeast = 2) { repository.requestStatus() }
    }

    @Test
    fun `backupStatus flow is sourced from repository`() = runTest {
        val expectedStatus = BackupStatus(
            isRunning = true,
            lastBackupSuccess = true,
            totalBackups = 5
        )
        fakeBackupStatus.value = expectedStatus
        advanceUntilIdle()

        assertEquals(expectedStatus, viewModel.backupStatus.value)
    }

    @Test
    fun `backupProgress flow is sourced from repository`() = runTest {
        val expectedProgress = BackupProgress(
            percentage = 75,
            filesProcessed = 30,
            totalFiles = 40
        )
        fakeBackupProgress.value = expectedProgress
        advanceUntilIdle()

        assertEquals(expectedProgress, viewModel.backupProgress.value)
    }
}
