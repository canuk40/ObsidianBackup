package com.obsidianbackup.security

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TaskerSecurityValidator - Validates calling packages for Tasker integration
 * 
 * Ensures only authorized automation apps (Tasker, MacroDroid, etc.) can
 * trigger backup operations via intents.
 * 
 * Security features:
 * - Package signature verification
 * - Whitelist of known automation apps
 * - User-configurable authorized apps
 */
@Singleton
class TaskerSecurityValidator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "TaskerSecurityValidator"
        
        // Known automation app package names
        private val KNOWN_AUTOMATION_PACKAGES = setOf(
            "net.dinglisch.android.taskerm",     // Tasker
            "com.arlosoft.macrodroid",           // MacroDroid
            "com.twofortyfouram.locale",         // Locale (legacy)
            "com.llamalab.automate",             // Automate
            "com.joaomgcd.autotools",            // AutoTools
            "com.joaomgcd.autoapps",             // AutoApps
            "com.joaomgcd.join",                 // Join
            "com.balda.tasker",                  // Tasker Plugin Helper
            "de.szalkowski.activitylauncher"     // Activity Launcher
        )
        
        // Known automation app signature SHA-256 fingerprints.
        // Compute with: apksigner verify --print-certs app.apk | grep SHA-256
        // Or: keytool -printcert -jarfile app.apk | grep SHA256
        // If a package is absent from this map, signature check is skipped (allow by package name only).
        private val KNOWN_SIGNATURES = mapOf<String, Set<String>>(
            "net.dinglisch.android.taskerm" to setOf(
                "973fe25b9be28fb7436d49582b04277767c852539be31783d134a55621b6636d"
            ),
        )
        
        private const val PREFS_NAME = "tasker_security"
        private const val KEY_AUTHORIZED_PACKAGES = "authorized_packages"
        private const val KEY_REQUIRE_SIGNATURE = "require_signature"
        private const val KEY_ALLOW_ALL = "allow_all_debug" // For testing only
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Check if a package is authorized to trigger automation
     */
    fun isAuthorizedPackage(packageName: String?): Boolean {
        if (packageName.isNullOrEmpty()) {
            Log.w(TAG, "Empty package name")
            return false
        }
        
        // Debug mode - allow all (ONLY for development)
        if (prefs.getBoolean(KEY_ALLOW_ALL, false)) {
            Log.w(TAG, "DEBUG MODE: Allowing package $packageName")
            return true
        }
        
        // Check if it's our own package
        if (packageName == context.packageName) {
            return true
        }
        
        // Check against known automation apps
        if (packageName in KNOWN_AUTOMATION_PACKAGES) {
            Log.d(TAG, "Known automation package: $packageName")
            
            // Optionally verify signature
            if (prefs.getBoolean(KEY_REQUIRE_SIGNATURE, true)) {
                return verifyPackageSignature(packageName)
            }
            return true
        }
        
        // Check user-authorized packages
        val authorizedPackages = getAuthorizedPackages()
        if (packageName in authorizedPackages) {
            Log.d(TAG, "User-authorized package: $packageName")
            return true
        }
        
        Log.w(TAG, "Unauthorized package: $packageName")
        return false
    }
    
    /**
     * Verify package signature against known signatures
     */
    private fun verifyPackageSignature(packageName: String): Boolean {
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            )
            
            val signatures = packageInfo.signatures
            if (signatures.isNullOrEmpty()) {
                Log.w(TAG, "No signatures found for $packageName")
                return false
            }
            
            // Get known signatures for this package
            val knownSigs = KNOWN_SIGNATURES[packageName]
            if (knownSigs == null) {
                Log.d(TAG, "No signature verification configured for $packageName")
                return true // Allow if no signature is configured
            }
            
            // Check if any signature matches using full SHA-256 fingerprint (H-6: replace truncated .take(10))
            for (signature in signatures) {
                val digest = MessageDigest.getInstance("SHA-256")
                val signatureBytes = signature.toByteArray()
                val sha256 = digest.digest(signatureBytes).joinToString("") { "%02x".format(it) }
                if (sha256 in knownSigs) {
                    Log.d(TAG, "Signature verified for $packageName")
                    return true
                }
            }
            
            Log.w(TAG, "Signature verification failed for $packageName")
            return false
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Package not found: $packageName", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying signature for $packageName", e)
            return false
        }
    }
    
    /**
     * Get list of user-authorized packages
     */
    fun getAuthorizedPackages(): Set<String> {
        val packagesStr = prefs.getString(KEY_AUTHORIZED_PACKAGES, "") ?: ""
        return packagesStr.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
    
    /**
     * Add a package to authorized list
     */
    fun authorizePackage(packageName: String) {
        val current = getAuthorizedPackages().toMutableSet()
        current.add(packageName)
        saveAuthorizedPackages(current)
        Log.i(TAG, "Package authorized: $packageName")
    }
    
    /**
     * Remove a package from authorized list
     */
    fun revokePackage(packageName: String) {
        val current = getAuthorizedPackages().toMutableSet()
        current.remove(packageName)
        saveAuthorizedPackages(current)
        Log.i(TAG, "Package revoked: $packageName")
    }
    
    /**
     * Save authorized packages to preferences
     */
    private fun saveAuthorizedPackages(packages: Set<String>) {
        prefs.edit()
            .putString(KEY_AUTHORIZED_PACKAGES, packages.joinToString(","))
            .apply()
    }
    
    /**
     * Check if a package is installed
     */
    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Get list of installed automation apps
     */
    fun getInstalledAutomationApps(): List<String> {
        return KNOWN_AUTOMATION_PACKAGES.filter { isPackageInstalled(it) }
    }
    
    /**
     * Enable/disable signature verification
     */
    fun setRequireSignatureVerification(required: Boolean) {
        prefs.edit()
            .putBoolean(KEY_REQUIRE_SIGNATURE, required)
            .apply()
        Log.i(TAG, "Signature verification ${if (required) "enabled" else "disabled"}")
    }
    
    /**
     * Enable/disable debug mode (allow all packages)
     * WARNING: Only for development!
     */
    fun setDebugAllowAll(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_ALLOW_ALL, enabled)
            .apply()
        Log.w(TAG, "Debug allow-all mode ${if (enabled) "ENABLED" else "disabled"}")
    }
    
    /**
     * Reset to default settings
     */
    fun resetToDefaults() {
        prefs.edit()
            .clear()
            .putBoolean(KEY_REQUIRE_SIGNATURE, true)
            .putBoolean(KEY_ALLOW_ALL, false)
            .apply()
        Log.i(TAG, "Security settings reset to defaults")
    }
    
    /**
     * Get security summary for UI display
     */
    fun getSecuritySummary(): SecuritySummary {
        return SecuritySummary(
            authorizedPackages = getAuthorizedPackages().size,
            installedAutomationApps = getInstalledAutomationApps(),
            signatureVerificationEnabled = prefs.getBoolean(KEY_REQUIRE_SIGNATURE, true),
            debugModeEnabled = prefs.getBoolean(KEY_ALLOW_ALL, false)
        )
    }
}

/**
 * Security summary data class
 */
data class SecuritySummary(
    val authorizedPackages: Int,
    val installedAutomationApps: List<String>,
    val signatureVerificationEnabled: Boolean,
    val debugModeEnabled: Boolean
) {
    val isSecure: Boolean
        get() = !debugModeEnabled && signatureVerificationEnabled
}
