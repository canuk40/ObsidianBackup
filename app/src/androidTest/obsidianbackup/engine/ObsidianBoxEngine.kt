// ObsidianBoxEngineTest.kt
// REPLACE entire file content:
// Old package: com.titanbackup.engine  ← WRONG (old project name)
// New package: com.obsidianbackup.engine  ← CORRECT

package com.obsidianbackup.engine   // ← FIXED package name

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ObsidianBoxEngineTest {

    private lateinit var engine: ObsidianBoxEngine

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Inject real engine via Hilt test component or manual construction
        engine = ObsidianBoxEngine(context)
    }

    @Test
    fun engineReportsCorrectCapabilities() {
        assertTrue("ObsidianBoxEngine should support app backup", engine.supportsBackup())
        assertTrue("ObsidianBoxEngine should support restore", engine.supportsRestore())
    }

    @Test
    fun backupWithEmptyRequestReturnsSuccess() = runBlocking {
        val request = BackupRequest(appIds = emptySet(), options = BackupOptions.default())
        val result = engine.backupApps(request)
        assertTrue("Empty backup request should succeed", result is BackupResult.Success)
    }
}
