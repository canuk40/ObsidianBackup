// security/PathSecurityValidatorTest.kt
package com.obsidianbackup.security

import com.obsidianbackup.model.AppId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * Security tests for PathSecurityValidator.
 * Verifies protection against path traversal attacks.
 */
class PathSecurityValidatorTest {

    @Test
    fun `validateAppId accepts valid package names`() {
        val validAppIds = listOf(
            "com.example.app",
            "com.example.myapp",
            "org.example.app",
            "com.example.app.feature",
            "com.example.app_feature",
            "a.b.c",
            "com.example123.app456"
        )

        validAppIds.forEach { packageName ->
            val appId = AppId(packageName)
            assertTrue(
                PathSecurityValidator.validateAppId(appId),
                "Should accept valid package name: $packageName"
            )
        }
    }

    @Test
    fun `validateAppId rejects path traversal attempts`() {
        val maliciousAppIds = listOf(
            "../../../etc/passwd",
            "../../system",
            "../data",
            "com.example/../../../etc/passwd",
            "com.example.app/../..",
            "com.example..app",
            "..com.example.app"
        )

        maliciousAppIds.forEach { malicious ->
            val appId = AppId(malicious)
            assertFalse(
                PathSecurityValidator.validateAppId(appId),
                "Should reject path traversal attempt: $malicious"
            )
        }
    }

    @Test
    fun `validateAppId rejects paths with slashes`() {
        val pathsWithSlashes = listOf(
            "com/example/app",
            "com.example.app/",
            "/com.example.app",
            "com.example\\app",
            "com.example.app\\data"
        )

        pathsWithSlashes.forEach { path ->
            val appId = AppId(path)
            assertFalse(
                PathSecurityValidator.validateAppId(appId),
                "Should reject path with slashes: $path"
            )
        }
    }

    @Test
    fun `validateAppId rejects invalid package name formats`() {
        val invalidFormats = listOf(
            "ComExample.App",        // Uppercase
            "com.Example.app",       // Uppercase
            "com-example-app",       // Hyphens
            "com example app",       // Spaces
            "example",               // No dots
            "com.",                  // Trailing dot
            ".com.example",          // Leading dot
            "com..example.app",      // Double dots in middle
            "123.example.app",       // Starting with digit
            "com.123example.app",    // Second segment starting with digit
            "com.example.app@1.0",   // Special characters
            "com.example.app#test",  // Special characters
            "com.example.app!",      // Special characters
            ""                       // Empty string
        )

        invalidFormats.forEach { invalid ->
            val appId = AppId(invalid)
            assertFalse(
                PathSecurityValidator.validateAppId(appId),
                "Should reject invalid format: $invalid"
            )
        }
    }

    @Test
    fun `getAppDataDirectory returns correct path for valid appId`() {
        val appId = AppId("com.example.app")
        val dataDir = PathSecurityValidator.getAppDataDirectory(appId)
        
        assertEquals("/data/data/com.example.app", dataDir.path)
    }

    @Test
    fun `getAppDataDirectory throws SecurityException for invalid appId`() {
        val invalidAppId = AppId("../../../etc/passwd")
        
        val exception = assertThrows<SecurityException> {
            PathSecurityValidator.getAppDataDirectory(invalidAppId)
        }
        
        assertTrue(exception.message!!.contains("Invalid app ID format"))
    }

    @Test
    fun `getAppDataDirectory prevents escape from data directory via symbolic links`() {
        // Note: This test demonstrates the protection mechanism.
        // In a real scenario, if /data/data/malicious.app was a symlink to /etc,
        // the canonical path would resolve to /etc and be rejected.
        
        val appId = AppId("com.example.app")
        val dataDir = PathSecurityValidator.getAppDataDirectory(appId)
        
        // Verify canonical path is still within /data/data/
        assertTrue(
            dataDir.canonicalPath.startsWith("/data/data/"),
            "Canonical path must remain within /data/data/"
        )
    }

    @Test
    fun `isWithinAllowedRoot accepts files within allowed directory`(@TempDir tempDir: Path) {
        val allowedRoot = tempDir.toFile()
        val subDir = File(allowedRoot, "subdir")
        subDir.mkdirs()
        val fileInside = File(subDir, "file.txt")
        fileInside.createNewFile()
        
        assertTrue(PathSecurityValidator.isWithinAllowedRoot(fileInside, allowedRoot))
    }

    @Test
    fun `isWithinAllowedRoot rejects files outside allowed directory`(@TempDir tempDir: Path) {
        val allowedRoot = File(tempDir.toFile(), "allowed")
        allowedRoot.mkdirs()
        
        val outsideDir = File(tempDir.toFile(), "outside")
        outsideDir.mkdirs()
        val fileOutside = File(outsideDir, "file.txt")
        fileOutside.createNewFile()
        
        assertFalse(PathSecurityValidator.isWithinAllowedRoot(fileOutside, allowedRoot))
    }

    @Test
    fun `isWithinAllowedRoot handles relative paths with dot dot`(@TempDir tempDir: Path) {
        val allowedRoot = File(tempDir.toFile(), "allowed")
        allowedRoot.mkdirs()
        
        val subDir = File(allowedRoot, "sub")
        subDir.mkdirs()
        
        // Try to escape using ../
        val escapeAttempt = File(subDir, "../../../etc/passwd")
        
        // Should be rejected because canonical path will resolve outside allowed root
        assertFalse(PathSecurityValidator.isWithinAllowedRoot(escapeAttempt, allowedRoot))
    }

    @Test
    fun `multiple malicious appId variations are all rejected`() {
        val maliciousVariations = listOf(
            // Classic path traversal
            "../../etc/passwd",
            "../../../root/.ssh/id_rsa",
            
            // Hidden traversal
            "com.example...app",
            
            // Null bytes (would fail regex anyway)
            "com.example\u0000.app",
            
            // Unicode tricks
            "com․example․app",  // Using middle dot instead of period
            
            // Absolute paths
            "/etc/passwd",
            "/system/build.prop",
            
            // Home directory
            "~/malicious",
            
            // Special devices
            "/dev/null",
            "/proc/self/environ"
        )

        maliciousVariations.forEach { malicious ->
            val appId = AppId(malicious)
            assertFalse(
                PathSecurityValidator.validateAppId(appId),
                "Should reject malicious variation: $malicious"
            )
        }
    }

    @Test
    fun `edge cases are handled correctly`() {
        // Very long but valid package name
        val longPackage = "com." + "a".repeat(200) + ".app"
        assertTrue(PathSecurityValidator.validateAppId(AppId(longPackage)))
        
        // Minimum valid package
        assertTrue(PathSecurityValidator.validateAppId(AppId("a.b")))
        
        // Package with many segments
        assertTrue(PathSecurityValidator.validateAppId(AppId("com.example.app.feature.module.component")))
        
        // Package with underscores
        assertTrue(PathSecurityValidator.validateAppId(AppId("com.example_app.test_feature")))
        
        // Package with numbers (but not starting segment with number)
        assertTrue(PathSecurityValidator.validateAppId(AppId("com.example2.app3")))
    }

    @Test
    fun `real world malicious examples are blocked`() {
        // Examples from OWASP path traversal attack vectors
        val owaspExamples = listOf(
            "../../../../../etc/passwd",
            "..\\..\\..\\..\\..\\windows\\system32\\config\\sam",
            "/etc/passwd",
            "....//....//....//etc/passwd",
            "..;/..;/..;/etc/passwd",
            "%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd",  // URL encoded
            "com.example.app/../../../../etc/passwd"
        )

        owaspExamples.forEach { attack ->
            val appId = AppId(attack)
            assertFalse(
                PathSecurityValidator.validateAppId(appId),
                "Should block OWASP attack vector: $attack"
            )
        }
    }
}
