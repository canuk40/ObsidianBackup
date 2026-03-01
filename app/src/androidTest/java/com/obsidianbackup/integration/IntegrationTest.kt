package com.obsidianbackup.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.obsidianbackup.features.Feature
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class IntegrationTest {

    @Test
    fun testFeatureFlags_AllDefined() {
        val features = Feature.values()
        assertTrue(features.size >= 13, "Should have at least 13 features")
    }

    @Test
    fun testNavigationScreens_AllDefined() {
        val screens = com.obsidianbackup.ui.Screen.items
        assertTrue(screens.isNotEmpty(), "Should have navigation screens")
    }
}
