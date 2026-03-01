// E2E Tests (5%) - Full backup/restore flows
// androidTest/java/com.titanbackup/BackupRestoreE2ETest.kt
package com.titanbackup

import android.Manifest
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.titanbackup.model.AppId
import com.titanbackup.model.BackupComponent
import com.titanbackup.model.BackupRequest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRestoreE2ETest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val permissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun `complete backup and restore flow on rooted emulator`() {
        // This test requires a rooted emulator with test data

        // Launch app
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->

            // Navigate to Apps screen
            onView(withId(R.id.apps_tab)).perform(click())

            // Select test app
            onView(withText("Test App")).perform(click())

            // Start backup
            onView(withId(R.id.backup_button)).perform(click())

            // Confirm backup dialog
            onView(withText("Backup")).perform(click())

            // Wait for backup completion (would need proper waiting mechanism)
            Thread.sleep(5000) // Simplified for demo

            // Verify backup appears in list
            onView(withId(R.id.backups_tab)).perform(click())
            onView(withText(containsString("Test App"))).check(matches(isDisplayed()))

            // Start restore
            onView(withText(containsString("Test App"))).perform(click())
            onView(withId(R.id.restore_button)).perform(click())

            // Confirm restore
            onView(withText("Restore")).perform(click())

            // Verify restore completion
            onView(withText("Restore completed")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun `permission mode detection and fallback`() {
        // Test automatic permission detection

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->

            // Check permission indicator shows current mode
            onView(withId(R.id.permission_indicator)).check(matches(isDisplayed()))

            // If not root, should show fallback options
            val permissionChip = uiDevice.findObject(UiSelector().resourceId("permission_indicator"))
            val permissionMode = permissionChip.text

            assertTrue("Should show valid permission mode",
                permissionMode in listOf("ROOT", "SHIZUKU", "ADB", "SAF"))
        }
    }

    @Test
    fun `large backup with progress indication`() {
        // Test backup of large app with progress UI

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->

            // Select large app
            onView(withText("Large Test App")).perform(click())

            // Start backup
            onView(withId(R.id.backup_button)).perform(click())
            onView(withText("Backup")).perform(click())

            // Verify progress indicators
            onView(withId(R.id.progress_bar)).check(matches(isDisplayed()))
            onView(withId(R.id.progress_text)).check(matches(withText(containsString("%"))))

            // Wait for completion
            Thread.sleep(10000) // Would need proper async waiting

            // Verify success notification
            val notification = uiDevice.findObject(
                UiSelector().textContains("Backup completed")
            )
            assertTrue("Success notification should appear", notification.exists())
        }
    }

    @Test
    fun `error handling and recovery UI`() {
        // Test error scenarios and recovery options

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->

            // Trigger backup with insufficient permissions
            onView(withText("Protected App")).perform(click())
            onView(withId(R.id.backup_button)).perform(click())
            onView(withText("Backup")).perform(click())

            // Verify error dialog appears
            onView(withText("Backup Failed")).check(matches(isDisplayed()))
            onView(withText(containsString("permission"))).check(matches(isDisplayed()))

            // Check recovery options are shown
            onView(withText("Try Different Mode")).check(matches(isDisplayed()))
            onView(withText("Grant Permissions")).check(matches(isDisplayed()))
        }
    }
}
