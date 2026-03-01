// security/PathSecurityValidator.kt
package com.obsidianbackup.security

import com.obsidianbackup.model.AppId
import java.io.File

/**
 * Security validator for preventing path traversal attacks.
 * 
 * Validates app IDs and ensures file paths cannot escape the /data/data/ directory.
 */
object PathSecurityValidator {
    
    private const val DATA_DIR_PREFIX = "/data/data/"
    
    /**
     * Validates that an AppId conforms to valid Android package name format.
     * 
     * Package names must:
     * - Start with a lowercase letter
     * - Contain at least one dot
     * - Use only lowercase letters, digits, underscores, and dots
     * - Not contain path traversal sequences
     * 
     * @param appId The AppId to validate
     * @return true if valid, false otherwise
     */
    fun validateAppId(appId: AppId): Boolean {
        val value = appId.value
        
        // Check for path traversal attempts
        if (value.contains("..") || value.contains("/") || value.contains("\\")) {
            return false
        }
        
        // Valid Android package name pattern
        val packageRegex = "^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$".toRegex()
        return packageRegex.matches(value)
    }
    
    /**
     * Safely constructs an app data directory path with security validation.
     * 
     * This method:
     * 1. Validates the app ID format
     * 2. Constructs the path
     * 3. Resolves to canonical path (eliminates symlinks and .. references)
     * 4. Verifies the canonical path is still within /data/data/
     * 
     * @param appId The app ID to get directory for
     * @return A validated File object pointing to the app's data directory
     * @throws SecurityException if validation fails or path traversal detected
     */
    fun getAppDataDirectory(appId: AppId): File {
        // Step 1: Validate app ID format
        if (!validateAppId(appId)) {
            throw SecurityException("Invalid app ID format: ${appId.value}")
        }
        
        // Step 2: Construct path
        val dataDir = File("$DATA_DIR_PREFIX${appId.value}")
        
        // Step 3: Resolve to canonical path
        val canonicalPath = try {
            dataDir.canonicalPath
        } catch (e: Exception) {
            throw SecurityException("Failed to resolve canonical path for: ${appId.value}", e)
        }
        
        // Step 4: Verify path is still within /data/data/
        if (!canonicalPath.startsWith(DATA_DIR_PREFIX)) {
            throw SecurityException("Path traversal attempt detected: $canonicalPath")
        }
        
        return File(canonicalPath)
    }
    
    /**
     * Validates that a file path is within the allowed backup root directory.
     * 
     * @param file The file to validate
     * @param allowedRoot The allowed root directory
     * @return true if the file is within the allowed root, false otherwise
     */
    fun isWithinAllowedRoot(file: File, allowedRoot: File): Boolean {
        val canonicalFile = try {
            file.canonicalPath
        } catch (e: Exception) {
            return false
        }
        
        val canonicalRoot = try {
            allowedRoot.canonicalPath
        } catch (e: Exception) {
            return false
        }
        
        return canonicalFile.startsWith(canonicalRoot)
    }
}
