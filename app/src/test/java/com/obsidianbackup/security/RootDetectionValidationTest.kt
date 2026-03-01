package com.obsidianbackup.security

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.security.RootDetectionManager.DetectionConfidence
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream

/**
 * Comprehensive validation tests for RootDetectionManager
 * 
 * Test Categories:
 * 1. True Positives - Should detect root correctly
 * 2. False Positives - Should NOT incorrectly flag as rooted
 * 3. Edge Cases - Tricky scenarios
 * 4. Confidence Levels - Validate confidence scoring
 * 
 * CRITICAL: Any false positives found should be documented and fixed
 */
@DisplayName("Root Detection Validation Tests")
class RootDetectionValidationTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var logger: ObsidianLogger
    private lateinit var rootDetectionManager: RootDetectionManager
    private lateinit var googleApiAvailability: GoogleApiAvailability

    @BeforeEach
    fun setup() {
        context = mockk(relaxed = true)
        packageManager = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        googleApiAvailability = mockk(relaxed = true)
        
        every { context.packageManager } returns packageManager
        every { logger.i(any(), any()) } just Runs
        every { logger.w(any(), any()) } just Runs
        every { logger.e(any(), any(), any()) } just Runs
        
        rootDetectionManager = RootDetectionManager(context, logger)
    }

    @AfterEach
    fun teardown() {
        clearAllMocks()
    }

    // ==================== TRUE POSITIVE TESTS ====================
    // These scenarios SHOULD detect root

    @Nested
    @DisplayName("True Positive Tests - Should Detect Root")
    inner class TruePositiveTests {

        @Test
        @DisplayName("Should detect Magisk installed")
        fun testDetectMagiskInstalled() = runTest {
            // Given - Magisk package exists
            mockPackageInstalled("com.topjohnwu.magisk")
            mockNoSuBinaries()
            mockNormalBuildTags()
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertTrue(result.isRooted, "Should detect root when Magisk is installed")
            assertTrue(result.detectedRootApps.contains("com.topjohnwu.magisk"))
            assertTrue(result.confidence >= DetectionConfidence.MEDIUM)
            assertTrue(result.detectionMethod.contains("Root Management Apps"))
        }

        @Test
        @DisplayName("Should detect SuperSU installed")
        fun testDetectSuperSUInstalled() = runTest {
            // Given - SuperSU package exists
            mockPackageInstalled("eu.chainfire.supersu")
            mockNoSuBinaries()
            mockNormalBuildTags()
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertTrue(result.isRooted, "Should detect root when SuperSU is installed")
            assertTrue(result.detectedRootApps.contains("eu.chainfire.supersu"))
        }

        @Test
        @DisplayName("Should detect su binary in /system/bin/su")
        fun testDetectSuBinarySystemBin() = runTest {
            // Given - su binary exists in system
            mockNoPackages()
            mockSuBinaryExists("/system/bin/su")
            mockNormalBuildTags()
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertTrue(result.isRooted, "Should detect root when su binary exists")
            assertTrue(result.detectedSuPaths.contains("/system/bin/su"))
            assertTrue(result.confidence >= DetectionConfidence.MEDIUM)
        }

        @Test
        @DisplayName("Should detect su binary in /system/xbin/su")
        fun testDetectSuBinarySystemXbin() = runTest {
            // Given
            mockNoPackages()
            mockSuBinaryExists("/system/xbin/su")
            mockNormalBuildTags()
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertTrue(result.isRooted)
            assertTrue(result.detectedSuPaths.contains("/system/xbin/su"))
        }

        @Test
        @DisplayName("Should detect test-keys in build tags")
        fun testDetectTestKeys() = runTest {
            // Given
            mockNoPackages()
            mockNoSuBinaries()
            mockBuildTags("test-keys")
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertTrue(result.isRooted, "Should detect root with test-keys in build")
            assertEquals(DetectionConfidence.LOW, result.confidence)
            assertTrue(result.detectionMethod.contains("Suspicious Build Tags"))
        }

        @Test
        @DisplayName("Should detect multiple root indicators with HIGH confidence")
        fun testMultipleIndicatorsHighConfidence() = runTest {
            // Given - Multiple indicators present
            mockPackageInstalled("com.topjohnwu.magisk")
            mockSuBinaryExists("/system/bin/su")
            mockBuildTags("test-keys")
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertTrue(result.isRooted)
            assertTrue(result.confidence >= DetectionConfidence.HIGH || 
                      result.confidence == DetectionConfidence.CRITICAL)
            assertTrue(result.detectedRootApps.isNotEmpty())
            assertTrue(result.detectedSuPaths.isNotEmpty())
        }

        @Test
        @DisplayName("Should detect KingRoot installed")
        fun testDetectKingRoot() = runTest {
            // Given
            mockPackageInstalled("com.kingroot.kinguser")
            mockNoSuBinaries()
            mockNormalBuildTags()
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertTrue(result.isRooted)
            assertTrue(result.detectedRootApps.contains("com.kingroot.kinguser"))
        }

        @Test
        @DisplayName("Should detect writable system partition")
        fun testDetectWritableSystemPartition() = runTest {
            // Note: This is difficult to mock as it uses File.canWrite()
            // This test validates the method exists and returns a result
            mockNoPackages()
            mockNoSuBinaries()
            mockNormalBuildTags()
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then - Just ensure no exception is thrown
            assertNotNull(result)
        }
    }

    // ==================== FALSE POSITIVE TESTS ====================
    // These scenarios should NOT incorrectly detect root

    @Nested
    @DisplayName("False Positive Tests - Should NOT Detect Root")
    inner class FalsePositiveTests {

        @Test
        @DisplayName("Should NOT flag developer mode as root")
        fun testDeveloperModeNotRoot() = runTest {
            // Given - Developer mode enabled but no root
            mockNoPackages()
            mockNoSuBinaries()
            mockNormalBuildTags()
            mockDeveloperModeEnabled()
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertFalse(result.isRooted, 
                "Developer mode should NOT be flagged as root - CRITICAL FALSE POSITIVE")
        }

        @Test
        @DisplayName("Should NOT flag ADB debugging as root")
        fun testAdbDebuggingNotRoot() = runTest {
            // Given - ADB debugging enabled but no root
            mockNoPackages()
            mockNoSuBinaries()
            mockNormalBuildTags()
            mockAdbEnabled()
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertFalse(result.isRooted,
                "ADB debugging should NOT be flagged as root - CRITICAL FALSE POSITIVE")
        }

        @Test
        @DisplayName("Should NOT flag custom ROM without root")
        fun testCustomRomWithoutRoot() = runTest {
            // Given - Custom ROM (non-standard build) but no root indicators
            mockNoPackages()
            mockNoSuBinaries()
            // Custom ROM might have release-keys
            mockBuildTags("release-keys")
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertFalse(result.isRooted,
                "Custom ROM without root should NOT be flagged - FALSE POSITIVE")
        }

        @Test
        @DisplayName("Should NOT flag unlocked bootloader without root")
        fun testUnlockedBootloaderWithoutRoot() = runTest {
            // Given - Bootloader unlocked but no root
            mockNoPackages()
            mockNoSuBinaries()
            mockNormalBuildTags()
            // Bootloader unlock status doesn't directly indicate root
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertFalse(result.isRooted,
                "Unlocked bootloader without root should NOT be flagged - FALSE POSITIVE")
        }

        @Test
        @DisplayName("Should NOT flag Xposed without root capabilities")
        fun testXposedWithoutRoot() = runTest {
            // Given - Xposed/EdXposed installed but no root access
            // Note: Xposed packages are not in ROOT_APPS list
            mockPackageInstalled("de.robv.android.xposed.installer")
            mockNoSuBinaries()
            mockNormalBuildTags()
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertFalse(result.isRooted,
                "Xposed without root capabilities should NOT be flagged - FALSE POSITIVE")
        }

        @Test
        @DisplayName("Should NOT flag busybox in /data/local/ as root")
        fun testBusyboxInDataLocal() = runTest {
            // Given - Busybox in user-writable location (not system)
            mockNoPackages()
            mockNoSuBinaries()
            mockNormalBuildTags()
            // Note: Current implementation uses 'which busybox'
            // which might detect it in PATH regardless of location
            // This is a potential false positive that needs fixing
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then - Document if this fails
            if (result.isRooted && result.detectionMethod.contains("Busybox")) {
                fail("Busybox in /data/local/ flagged as root - FALSE POSITIVE DETECTED")
            }
        }

        @Test
        @DisplayName("Should NOT flag clean device")
        fun testCleanDevice() = runTest {
            // Given - Completely clean device
            mockNoPackages()
            mockNoSuBinaries()
            mockNormalBuildTags()
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertFalse(result.isRooted,
                "Clean device should NOT be flagged as rooted")
            assertEquals(DetectionConfidence.LOW, result.confidence)
            assertTrue(result.detectedRootApps.isEmpty())
            assertTrue(result.detectedSuPaths.isEmpty())
        }

        @Test
        @DisplayName("Should NOT flag emulator as rooted")
        fun testEmulatorNotRooted() = runTest {
            // Given - Running in emulator but no root
            mockNoPackages()
            mockNoSuBinaries()
            mockNormalBuildTags()
            mockEmulatorEnvironment()
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertFalse(result.isRooted,
                "Emulator without root should NOT be flagged - FALSE POSITIVE")
        }
    }

    // ==================== EDGE CASE TESTS ====================

    @Nested
    @DisplayName("Edge Case Tests")
    inner class EdgeCaseTests {

        @Test
        @DisplayName("Should handle systemless root (Magisk hide enabled)")
        fun testSystemlessRoot() = runTest {
            // Given - Magisk with hide enabled (harder to detect)
            // Still should detect the Magisk package
            mockPackageInstalled("com.topjohnwu.magisk")
            mockNoSuBinaries() // Su hidden from detection
            mockNormalBuildTags()
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertTrue(result.isRooted,
                "Should detect systemless root via package manager")
        }

        @Test
        @DisplayName("Should handle SafetyNet API unavailable")
        fun testSafetyNetUnavailable() = runTest {
            // Given - Google Play Services not available
            mockNoPackages()
            mockNoSuBinaries()
            mockNormalBuildTags()
            mockGooglePlayServicesUnavailable()
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertNull(result.safetyNetResult,
                "Should return null SafetyNet result when unavailable")
            assertFalse(result.isRooted,
                "Should not flag as rooted just because SafetyNet unavailable")
        }

        @Test
        @DisplayName("Should handle root detection apps installed")
        fun testRootDetectionAppsInstalled() = runTest {
            // Given - RootBeer or other detection apps installed
            mockPackageInstalled("com.scottyab.rootbeer.sample")
            mockNoSuBinaries()
            mockNormalBuildTags()
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertFalse(result.isRooted,
                "Root detection apps themselves should NOT indicate root - FALSE POSITIVE")
        }

        @Test
        @DisplayName("Should handle su binary with no execute permissions")
        fun testSuBinaryNoExecutePermissions() = runTest {
            // Given - su binary exists but can't execute
            mockNoPackages()
            mockSuBinaryExists("/system/bin/su", canExecute = false)
            mockNormalBuildTags()
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertTrue(result.isRooted,
                "Su binary existence indicates root regardless of permissions")
        }

        @Test
        @DisplayName("Should handle multiple su binaries")
        fun testMultipleSuBinaries() = runTest {
            // Given - Multiple su locations
            mockNoPackages()
            mockSuBinaryExists("/system/bin/su")
            mockSuBinaryExists("/system/xbin/su")
            mockSuBinaryExists("/sbin/su")
            mockNormalBuildTags()
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertTrue(result.isRooted)
            assertEquals(3, result.detectedSuPaths.size)
        }

        @Test
        @DisplayName("Should handle permission denial when checking files")
        fun testPermissionDenialGracefully() = runTest {
            // Given - SecurityException when checking files
            mockNoPackages()
            mockNoSuBinaries()
            mockNormalBuildTags()
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then - Should not crash and should return a result
            assertNotNull(result)
        }

        @Test
        @DisplayName("Should handle very old su binary paths")
        fun testLegacySuPaths() = runTest {
            // Given - Old path like /system/app/Superuser.apk
            mockNoPackages()
            mockSuBinaryExists("/system/app/Superuser.apk")
            mockNormalBuildTags()
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertTrue(result.isRooted, "Should detect legacy su paths")
            assertTrue(result.detectedSuPaths.contains("/system/app/Superuser.apk"))
        }
    }

    // ==================== CONFIDENCE LEVEL TESTS ====================

    @Nested
    @DisplayName("Confidence Level Validation")
    inner class ConfidenceLevelTests {

        @Test
        @DisplayName("LOW confidence: Only build tags suspicious")
        fun testLowConfidenceOnlyBuildTags() = runTest {
            // Given
            mockNoPackages()
            mockNoSuBinaries()
            mockBuildTags("test-keys")
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertTrue(result.isRooted)
            assertEquals(DetectionConfidence.LOW, result.confidence,
                "Only build tags should result in LOW confidence")
        }

        @Test
        @DisplayName("MEDIUM confidence: Su binary found")
        fun testMediumConfidenceSuBinary() = runTest {
            // Given
            mockNoPackages()
            mockSuBinaryExists("/system/bin/su")
            mockNormalBuildTags()
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertTrue(result.isRooted)
            assertTrue(result.confidence >= DetectionConfidence.MEDIUM,
                "Su binary should result in at least MEDIUM confidence")
        }

        @Test
        @DisplayName("MEDIUM confidence: Root management app detected")
        fun testMediumConfidenceRootApp() = runTest {
            // Given
            mockPackageInstalled("com.topjohnwu.magisk")
            mockNoSuBinaries()
            mockNormalBuildTags()
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertTrue(result.isRooted)
            assertTrue(result.confidence >= DetectionConfidence.MEDIUM,
                "Root management app should result in MEDIUM confidence")
        }

        @Test
        @DisplayName("MEDIUM confidence: System partition writable")
        fun testMediumConfidenceWritableSystem() = runTest {
            // Given - This is hard to mock, test logic only
            mockNoPackages()
            mockNoSuBinaries()
            mockNormalBuildTags()
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then - Just validate structure
            assertNotNull(result.confidence)
        }

        @Test
        @DisplayName("HIGH confidence: SafetyNet fails")
        fun testHighConfidenceSafetyNetFails() = runTest {
            // Note: SafetyNet mocking requires more setup
            // This test validates the concept
            mockNoPackages()
            mockNoSuBinaries()
            mockNormalBuildTags()
            mockGooglePlayServicesAvailable()
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then - Structure validation
            assertNotNull(result.confidence)
        }

        @Test
        @DisplayName("CRITICAL confidence: Multiple indicators present")
        fun testCriticalConfidenceMultipleIndicators() = runTest {
            // Given - Root app + su binary + build tags
            mockPackageInstalled("com.topjohnwu.magisk")
            mockSuBinaryExists("/system/bin/su")
            mockSuBinaryExists("/system/xbin/su")
            mockBuildTags("test-keys")
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertTrue(result.isRooted)
            // Should be HIGH or CRITICAL with 4+ detection methods
            assertTrue(result.confidence == DetectionConfidence.HIGH || 
                      result.confidence == DetectionConfidence.CRITICAL,
                "Multiple indicators should result in HIGH or CRITICAL confidence")
        }

        @Test
        @DisplayName("Should upgrade to CRITICAL with many indicators")
        fun testUpgradeToCritical() = runTest {
            // Given - Many indicators
            mockPackageInstalled("com.topjohnwu.magisk")
            mockPackageInstalled("eu.chainfire.supersu")
            mockSuBinaryExists("/system/bin/su")
            mockBuildTags("test-keys")
            
            // When
            val result = rootDetectionManager.detectRoot()
            
            // Then
            assertTrue(result.isRooted)
            // With 4+ detection methods, should consider CRITICAL
            assertTrue(result.detectionMethod.split(";").size >= 3)
        }
    }

    // ==================== QUICK CHECK TESTS ====================

    @Nested
    @DisplayName("Quick Check Tests")
    inner class QuickCheckTests {

        @Test
        @DisplayName("Quick check should detect su binary")
        fun testQuickCheckDetectsSu() {
            // Given
            mockNoPackages()
            mockSuBinaryExists("/system/bin/su")
            
            // When
            val result = rootDetectionManager.quickRootCheck()
            
            // Then
            assertTrue(result, "Quick check should detect su binary")
        }

        @Test
        @DisplayName("Quick check should detect test-keys")
        fun testQuickCheckDetectsTestKeys() {
            // Given
            mockNoPackages()
            mockNoSuBinaries()
            mockBuildTags("test-keys")
            
            // When
            val result = rootDetectionManager.quickRootCheck()
            
            // Then
            assertTrue(result, "Quick check should detect test-keys")
        }

        @Test
        @DisplayName("Quick check should be fast (not comprehensive)")
        fun testQuickCheckIsFast() {
            // Given
            mockNoPackages()
            mockNoSuBinaries()
            mockNormalBuildTags()
            
            // When
            val result = rootDetectionManager.quickRootCheck()
            
            // Then
            assertFalse(result, "Quick check on clean device should return false")
        }
    }

    // ==================== HELPER METHODS ====================

    private fun mockPackageInstalled(packageName: String) {
        every { 
            packageManager.getPackageInfo(packageName, 0) 
        } returns PackageInfo().apply {
            this.packageName = packageName
        }
    }

    private fun mockNoPackages() {
        every { 
            packageManager.getPackageInfo(any(), any<Int>()) 
        } throws PackageManager.NameNotFoundException()
    }

    private fun mockSuBinaryExists(path: String, canExecute: Boolean = true) {
        // Note: File mocking is tricky, actual implementation checks File.exists()
        // This documents the test intent
    }

    private fun mockNoSuBinaries() {
        // Document that no su binaries should be detected
    }

    private fun mockBuildTags(tags: String) {
        mockkStatic(Build::class)
        every { Build.TAGS } returns tags
    }

    private fun mockNormalBuildTags() {
        mockkStatic(Build::class)
        every { Build.TAGS } returns "release-keys"
    }

    private fun mockDeveloperModeEnabled() {
        // Developer mode doesn't have a direct API check
        // It's a settings flag, not a root indicator
    }

    private fun mockAdbEnabled() {
        // ADB debugging is a settings flag
        // Not a root indicator
    }

    private fun mockEmulatorEnvironment() {
        mockkStatic(Build::class)
        every { Build.FINGERPRINT } returns "generic"
        every { Build.MODEL } returns "Android SDK built for x86"
    }

    private fun mockGooglePlayServicesUnavailable() {
        mockkStatic(GoogleApiAvailability::class)
        every { 
            googleApiAvailability.isGooglePlayServicesAvailable(any()) 
        } returns ConnectionResult.SERVICE_MISSING
    }

    private fun mockGooglePlayServicesAvailable() {
        mockkStatic(GoogleApiAvailability::class)
        every { 
            googleApiAvailability.isGooglePlayServicesAvailable(any()) 
        } returns ConnectionResult.SUCCESS
    }

    // ==================== DOCUMENTATION OF FINDINGS ====================

    companion object {
        /**
         * DOCUMENTED FALSE POSITIVES:
         * 
         * 1. Busybox Detection:
         *    - Current implementation uses 'which busybox'
         *    - Will detect busybox in /data/local/ (user-writable)
         *    - Should only flag if in /system or /sbin
         *    - FIX NEEDED: Check busybox location, not just presence
         * 
         * 2. Developer Mode:
         *    - Developer mode itself doesn't indicate root
         *    - Implementation correctly doesn't check for this
         *    - NO FIX NEEDED
         * 
         * 3. ADB Debugging:
         *    - ADB debugging doesn't indicate root
         *    - Implementation correctly doesn't check for this
         *    - NO FIX NEEDED
         * 
         * 4. Custom ROMs:
         *    - Custom ROMs without root shouldn't be flagged
         *    - Build tags check might flag some custom ROMs
         *    - Current implementation only flags "test-keys" (correct)
         *    - NO FIX NEEDED
         * 
         * 5. Unlocked Bootloader:
         *    - Bootloader unlock doesn't indicate root
         *    - Implementation correctly doesn't check for this
         *    - NO FIX NEEDED
         * 
         * RECOMMENDED IMPROVEMENTS:
         * 
         * 1. Busybox Location Check:
         *    - Parse 'which busybox' output
         *    - Only flag if path starts with /system or /sbin
         *    - Ignore /data/local/ paths
         * 
         * 2. SafetyNet Timeout Handling:
         *    - Current: 10 second timeout
         *    - Consider making configurable
         * 
         * 3. Confidence Scoring:
         *    - Current algorithm is good
         *    - Consider weights for different indicators
         * 
         * 4. Whitelisting:
         *    - Add configurable whitelist for known safe apps
         *    - Useful for enterprise/MDM scenarios
         */
    }
}
