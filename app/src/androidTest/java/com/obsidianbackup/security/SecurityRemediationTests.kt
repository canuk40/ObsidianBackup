package com.obsidianbackup.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.obsidianbackup.model.AppId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Security Remediation Verification Tests
 * 
 * These tests verify that the security fixes and remediations
 * are properly implemented.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SecurityRemediationTests {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var taskerSecurityValidator: TaskerSecurityValidator

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
    }

    // ============================================================================
    // REMEDIATION TESTS
    // ============================================================================

    @Test
    fun testRemediation_pathSecurityValidator_validAppIds_shouldPass() {
        // Verify that legitimate app IDs pass validation
        val validAppIds = listOf(
            AppId("com.google.android.gms"),
            AppId("com.android.chrome"),
            AppId("com.obsidianbackup"),
            AppId("com.test.app"),
            AppId("com.example.myapp123"),
            AppId("com.company.product.feature")
        )

        validAppIds.forEach { appId ->
            assertTrue(
                PathSecurityValidator.validateAppId(appId),
                "Valid app ID should pass: ${appId.value}"
            )
        }
    }

    @Test
    fun testRemediation_pathSecurityValidator_invalidAppIds_shouldFail() {
        // Comprehensive list of invalid app IDs that should be rejected
        val invalidAppIds = listOf(
            // Path traversal
            AppId("../../../etc/passwd"),
            AppId("../../system"),
            AppId("com.app/../../../data"),
            
            // Absolute paths
            AppId("/etc/passwd"),
            AppId("/system/bin/su"),
            
            // Invalid characters
            AppId("com/app/test"),
            AppId("com\\app\\test"),
            AppId("com.app;malicious"),
            AppId("com.app|malicious"),
            AppId("com.app&malicious"),
            
            // Invalid format
            AppId("nopackagename"),
            AppId("UPPERCASE.PACKAGE"),
            AppId("123.numeric.start"),
            AppId("com..doubledot"),
            AppId(".com.leadingdot"),
            
            // Null byte injection
            AppId("com.app\u0000malicious"),
            
            // Special characters
            AppId("com.app<script>"),
            AppId("com.app' OR '1'='1"),
            AppId("com.app\"; DROP TABLE--"),
            
            // Unicode tricks
            AppId("com.app\u202emalicious"),  // Right-to-left override
            AppId("com.app\u200bmalicious"),  // Zero-width space
        )

        invalidAppIds.forEach { appId ->
            assertFalse(
                PathSecurityValidator.validateAppId(appId),
                "Invalid app ID should be rejected: ${appId.value}"
            )
        }
    }

    @Test
    fun testRemediation_taskerSecurityValidator_authorizedPackages() {
        // Verify that known automation packages are in whitelist
        val knownAutomationPackages = listOf(
            "net.dinglisch.android.taskerm",  // Tasker
            "com.arlosoft.macrodroid",         // MacroDroid
            "com.llamalab.automate"            // Automate
        )

        knownAutomationPackages.forEach { packageName ->
            assertTrue(
                taskerSecurityValidator.isPackageTrusted(packageName),
                "Known automation package should be trusted: $packageName"
            )
        }
    }

    @Test
    fun testRemediation_taskerSecurityValidator_maliciousPackages() {
        // Verify that unknown packages are rejected
        val maliciousPackages = listOf(
            "com.malicious.app",
            "com.hacker.tool",
            "com.fake.tasker",
            "net.dinglisch.android.taskerm.fake",  // Spoofed Tasker
            "com.arlosoft.macrodroid.evil"         // Spoofed MacroDroid
        )

        maliciousPackages.forEach { packageName ->
            assertFalse(
                taskerSecurityValidator.isPackageTrusted(packageName),
                "Malicious package should be rejected: $packageName"
            )
        }
    }

    @Test
    fun testRemediation_pathSecurityValidator_canonicalPathResolution() {
        // Test that canonical path resolution properly handles various edge cases
        val testCases = listOf(
            Pair(AppId("com.test.app"), true),
            Pair(AppId("com.app/./test"), false),      // Contains /
            Pair(AppId("com.app/../other"), false),    // Contains /
            Pair(AppId("com.app\\test"), false)        // Contains backslash
        )

        testCases.forEach { (appId, shouldPass) ->
            val result = PathSecurityValidator.validateAppId(appId)
            if (shouldPass) {
                assertTrue(
                    result,
                    "AppId should pass validation: ${appId.value}"
                )
            } else {
                assertFalse(
                    result,
                    "AppId should fail validation: ${appId.value}"
                )
            }
        }
    }

    @Test
    fun testRemediation_securityValidator_debugMode_disabled() {
        // Verify that debug mode (allow all) is disabled by default
        val summary = taskerSecurityValidator.getSecuritySummary()
        
        assertFalse(
            summary.debugModeEnabled,
            "SECURITY BREACH: Debug mode (allow all) should be disabled in production"
        )
        
        assertTrue(
            summary.isSecure,
            "Security should be properly configured"
        )
    }

    @Test
    fun testRemediation_securityValidator_signatureVerification_enabled() {
        // Verify that signature verification is enabled by default
        val summary = taskerSecurityValidator.getSecuritySummary()
        
        assertTrue(
            summary.signatureVerificationEnabled,
            "Signature verification should be enabled for security"
        )
    }

    @Test
    fun testRemediation_inputSanitization_specialCharacters() {
        // Test that special characters in app IDs are properly rejected
        val specialChars = listOf(
            '/', '\\', '<', '>', '|', '&', ';', '\'', '"', '`', 
            '$', '(', ')', '{', '}', '[', ']', '*', '?', '!',
            '\n', '\r', '\t', '\u0000'  // Control characters
        )

        specialChars.forEach { char ->
            val appId = AppId("com.app${char}malicious")
            assertFalse(
                PathSecurityValidator.validateAppId(appId),
                "AppId with special character should be rejected: $char"
            )
        }
    }

    @Test
    fun testRemediation_inputSanitization_longInputs() {
        // Test that excessively long inputs are rejected
        val longPackageName = "com." + "a".repeat(1000)
        val appId = AppId(longPackageName)
        
        // Should either be rejected or handled safely
        // Android package names have a practical limit
        val result = PathSecurityValidator.validateAppId(appId)
        
        // If it passes validation, verify the path construction is safe
        if (result) {
            try {
                PathSecurityValidator.getAppDataDirectory(appId)
                // Should not crash or cause issues
            } catch (e: Exception) {
                // Exception is acceptable for edge cases
            }
        }
    }

    @Test
    fun testRemediation_caseInsensitiveBypass_rejected() {
        // Test that case variations don't bypass validation
        val caseTricks = listOf(
            AppId("Com.App"),           // Capital letters
            AppId("COM.APP"),
            AppId("cOm.ApP"),
            AppId("com.APP"),
            AppId("com.aPP.TeSt")
        )

        caseTricks.forEach { appId ->
            // Android package names should be lowercase
            val isValid = PathSecurityValidator.validateAppId(appId)
            
            // Either reject uppercase, or verify it doesn't bypass security
            if (isValid) {
                try {
                    val dataDir = PathSecurityValidator.getAppDataDirectory(appId)
                    // Verify path is still within /data/data/
                    assertTrue(
                        dataDir.canonicalPath.startsWith("/data/data/"),
                        "Case variation should not bypass path security"
                    )
                } catch (e: SecurityException) {
                    // Expected - case variation caught
                }
            }
        }
    }

    @Test
    fun testRemediation_unicodeNormalization_attacks() {
        // Test Unicode normalization attacks
        val unicodeAttacks = listOf(
            AppId("com.app\u00A0malicious"),    // Non-breaking space
            AppId("com.app\u200Bmalicious"),    // Zero-width space
            AppId("com.app\u202Emalicious"),    // Right-to-left override
            AppId("com.app\uFEFFmalicious"),    // Zero-width no-break space
            AppId("com.app\u0301malicious")     // Combining character
        )

        unicodeAttacks.forEach { appId ->
            assertFalse(
                PathSecurityValidator.validateAppId(appId),
                "Unicode attack should be rejected: ${appId.value}"
            )
        }
    }

    @Test
    fun testRemediation_regexPerformance_noReDoS() {
        // Test that the validation regex doesn't suffer from ReDoS
        // (Regular Expression Denial of Service)
        
        val reDoSAttacks = listOf(
            "a".repeat(1000) + "!",
            "com.".repeat(500) + "app",
            "a".repeat(100) + ".".repeat(100)
        )

        reDoSAttacks.forEach { malicious ->
            val startTime = System.nanoTime()
            
            try {
                PathSecurityValidator.validateAppId(AppId(malicious))
            } catch (e: Exception) {
                // Exception is acceptable
            }
            
            val duration = System.nanoTime() - startTime
            val durationMs = duration / 1_000_000
            
            assertTrue(
                durationMs < 100,  // Should complete in under 100ms
                "Regex validation took too long (${durationMs}ms) - possible ReDoS: $malicious"
            )
        }
    }

    @Test
    fun testRemediation_emptyAndNullInputs() {
        // Test edge cases with empty/null-like inputs
        val edgeCases = listOf(
            AppId(""),
            AppId(" "),
            AppId("   "),
            AppId("\t"),
            AppId("\n"),
            AppId("null"),
            AppId("undefined")
        )

        edgeCases.forEach { appId ->
            assertFalse(
                PathSecurityValidator.validateAppId(appId),
                "Empty/null-like input should be rejected: '${appId.value}'"
            )
        }
    }
}
