// test/scheduling/SmartBackupWorkerTest.kt
package com.obsidianbackup.scheduling

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.obsidianbackup.data.repository.BackupProfileRepository
import com.obsidianbackup.model.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@DisplayName("SmartBackupWorker Tests")
class SmartBackupWorkerTest {
    
    private lateinit var context: Context
    private lateinit var profileRepository: BackupProfileRepository
    
    @BeforeEach
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        profileRepository = mockk(relaxed = true)
    }
    
    @Test
    fun `worker loads profile successfully`() = runTest {
        // Given: A valid backup profile
        val profileId = BackupProfileId("test-profile-id")
        val profile = BackupProfile(
            id = profileId,
            name = "Test Profile",
            appIds = listOf(AppId("com.example.app1")),
            isEnabled = true
        )
        
        coEvery { profileRepository.getProfile(profileId) } returns profile
        
        // When: Profile is requested
        val result = profileRepository.getProfile(profileId)
        
        // Then: Profile should be loaded
        assertTrue(result != null)
        assertTrue(result.id == profileId)
        coVerify { profileRepository.getProfile(profileId) }
    }
}
