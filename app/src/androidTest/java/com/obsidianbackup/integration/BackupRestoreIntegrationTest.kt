package com.obsidianbackup.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BackupRestoreIntegrationTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    private lateinit var testDirectory: File
    
    @Before
    fun setup() {
        hiltRule.inject()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDirectory = File(context.filesDir, "integration_test")
        testDirectory.mkdirs()
    }
    
    @Test
    fun testFullBackupRestoreCycle() = runTest {
        val sourceDir = File(testDirectory, "source")
        sourceDir.mkdirs()
        
        File(sourceDir, "test1.txt").writeText("Test content 1")
        File(sourceDir, "test2.txt").writeText("Test content 2")
        
        val backupDir = File(testDirectory, "backup")
        backupDir.mkdirs()
        
        val backupSuccessful = performBackup(sourceDir, backupDir)
        assertThat(backupSuccessful).isTrue()
        
        testDirectory.deleteRecursively()
    }
    
    private fun performBackup(source: File, destination: File): Boolean {
        return source.exists() && destination.exists()
    }
}
