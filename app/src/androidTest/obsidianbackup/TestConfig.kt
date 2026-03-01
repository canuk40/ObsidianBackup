// androidTest/java/com.titanbackup/TestConfig.kt
package com.titanbackup

import androidx.test.platform.app.InstrumentationRegistry

object TestConfig {

    val isEmulator = InstrumentationRegistry.getArguments().getString("emulator") == "true"
    val isRooted = InstrumentationRegistry.getArguments().getString("rooted") == "true"
    val testDataPath = InstrumentationRegistry.getArguments().getString("testDataPath") ?: "/sdcard/test_data"

    val skipE2ETests = !isEmulator || !isRooted

    // Test data configuration
    val largeTestAppPackage = "com.example.largetestapp"
    val protectedTestAppPackage = "com.example.protectedapp"
    val normalTestAppPackage = "com.example.normalapp"

    // Timeout configurations
    val backupTimeoutMs = 30000L  // 30 seconds
    val restoreTimeoutMs = 45000L // 45 seconds
    val uiActionTimeoutMs = 5000L  // 5 seconds

    // Test resource IDs (would match actual R.id values)
    object TestIds {
        const val appsTab = "apps_tab"
        const val backupsTab = "backups_tab"
        const val backupButton = "backup_button"
        const val restoreButton = "restore_button"
        const val progressBar = "progress_bar"
        const val progressText = "progress_text"
        const val permissionIndicator = "permission_indicator"
    }
}
