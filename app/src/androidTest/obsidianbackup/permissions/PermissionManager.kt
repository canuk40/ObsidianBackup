// PermissionManagerTest.kt — REPLACE TODO stubs
package com.obsidianbackup.permissions

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionManagerTest {

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val permissionManager = PermissionManager(context)

    // Previously: TODO: Implement actual detection logic
    @Test
    fun detectsGrantedPermission() {
        val result = permissionManager.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        assertTrue("READ_EXTERNAL_STORAGE must be detected as granted (granted by @Rule)", result)
    }

    // Previously: TODO: Implement permission request flow
    @Test
    fun returnsCorrectPermissionStatusForMissingPermission() {
        // WRITE_CALENDAR is not in the GrantPermissionRule above
        val result = permissionManager.hasPermission(Manifest.permission.WRITE_CALENDAR)
        assertFalse("WRITE_CALENDAR should not be granted (not granted by @Rule)", result)
    }
}
