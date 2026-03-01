package com.obsidianbackup.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.obsidianbackup.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {
    
    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun dashboardScreen_isDisplayed() {
        composeTestRule.onNodeWithText("Dashboard", substring = true, ignoreCase = true)
            .assertExists()
    }
    
    @Test
    fun dashboardScreen_showsBackupButton() {
        composeTestRule.onNode(
            hasText("Backup", substring = true, ignoreCase = true) or 
            hasContentDescription("Backup", substring = true, ignoreCase = true)
        ).assertExists()
    }
}
