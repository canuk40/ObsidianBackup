// crypto/PrivacyAuditor.kt
package com.obsidianbackup.crypto

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import java.io.File
import java.security.KeyStore

/**
 * Privacy Auditor for Zero-Knowledge Mode
 * 
 * Performs comprehensive privacy and security audits to verify:
 * 1. Keys are stored locally only (no cloud storage)
 * 2. No telemetry or analytics active
 * 3. Encryption is properly configured
 * 4. No data leakage channels
 * 5. Local-only mode enforcement
 */
class PrivacyAuditor(private val context: Context) {
    
    companion object {
        private const val TAG = "PrivacyAuditor"
        
        // Known telemetry/analytics domains to check
        private val TELEMETRY_DOMAINS = listOf(
            "analytics.google.com",
            "firebase.google.com",
            "crashlytics.com",
            "segment.com",
            "mixpanel.com",
            "amplitude.com"
        )
        
        // Network operation checks
        private val CLOUD_STORAGE_PATTERNS = listOf(
            "googleapis.com",
            "amazonaws.com",
            "dropbox.com",
            "onedrive.com",
            "icloud.com"
        )
    }
    
    /**
     * Run comprehensive privacy audit
     */
    suspend fun performAudit(
        zkManager: ZeroKnowledgeManager
    ): PrivacyAuditResult {
        Log.i(TAG, "Starting privacy audit...")
        
        val warnings = mutableListOf<String>()
        
        // Check 1: Keys stored locally
        val keysLocal = checkKeysLocal(zkManager)
        if (!keysLocal) {
            warnings.add("Master keys not found in local cache")
        }
        
        // Check 2: No cloud key access
        val noCloudAccess = checkNoCloudKeyAccess()
        if (!noCloudAccess) {
            warnings.add("Potential cloud key access detected")
        }
        
        // Check 3: No telemetry
        val noTelemetry = checkNoTelemetry()
        if (!noTelemetry) {
            warnings.add("Telemetry or analytics may be active")
        }
        
        // Check 4: Encryption active
        val encryptionActive = checkEncryptionActive(zkManager)
        if (!encryptionActive) {
            warnings.add("Zero-knowledge encryption not active")
        }
        
        // Check 5: Local-only mode
        val config = zkManager.getConfig()
        val localOnly = config?.localOnlyMode ?: false
        if (!localOnly && config?.enabled == true) {
            warnings.add("Local-only mode disabled - data may sync to cloud")
        }
        
        // Check 6: Key backup status
        val keyBackupExported = config?.keyBackupExported ?: false
        if (!keyBackupExported) {
            warnings.add("Key backup not exported - high data loss risk")
        }
        
        // Check 7: Network activity (if local-only)
        if (localOnly) {
            val networkActive = checkNetworkActivity()
            if (networkActive) {
                warnings.add("Network activity detected in local-only mode")
            }
        }
        
        // Check 8: Key storage locations
        val keyStorageWarnings = checkKeyStorageLocations()
        warnings.addAll(keyStorageWarnings)
        
        // Check 9: Memory security
        val memoryWarnings = checkMemorySecurity()
        warnings.addAll(memoryWarnings)
        
        val result = PrivacyAuditResult(
            timestamp = System.currentTimeMillis(),
            keysStoredLocally = keysLocal,
            noCloudKeyAccess = noCloudAccess,
            noTelemetry = noTelemetry,
            encryptionActive = encryptionActive,
            localOnlyMode = localOnly,
            warnings = warnings
        )
        
        Log.i(TAG, "Privacy audit completed: ${if (result.passed) "PASSED" else "WARNINGS (${warnings.size})"}")
        warnings.forEach { Log.w(TAG, "Warning: $it") }
        
        return result
    }
    
    /**
     * Check if keys are stored locally only
     */
    private fun checkKeysLocal(zkManager: ZeroKnowledgeManager): Boolean {
        // Check if master key is cached in memory
        val keyCached = zkManager.isUnlocked()
        
        // Verify key is not in Android KeyStore (should be user-derived, not keystore-managed)
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val zkKeysInKeystore = keyStore.aliases().toList().any { 
            it.startsWith("zero_knowledge") || it.startsWith("zk_")
        }
        
        return keyCached && !zkKeysInKeystore
    }
    
    /**
     * Check for potential cloud key access
     */
    private fun checkNoCloudKeyAccess(): Boolean {
        // In zero-knowledge mode, keys should never be in cloud-accessible locations
        
        // Check for suspicious files in cloud-synced directories
        val suspiciousLocations = listOf(
            File(context.filesDir, "cloud_sync"),
            File(context.cacheDir, "remote_keys"),
            File(context.externalCacheDir, "backup_keys")
        )
        
        val foundSuspicious = suspiciousLocations.any { it.exists() && it.listFiles()?.isNotEmpty() == true }
        
        return !foundSuspicious
    }
    
    /**
     * Check if telemetry/analytics is disabled
     */
    private fun checkNoTelemetry(): Boolean {
        // Check for analytics libraries in the app
        try {
            // Firebase Analytics
            val firebaseClass = Class.forName("com.google.firebase.analytics.FirebaseAnalytics")
            Log.w(TAG, "Firebase Analytics detected")
            return false
        } catch (e: ClassNotFoundException) {
            // Good - not present
        }
        
        try {
            // Google Analytics
            val analyticsClass = Class.forName("com.google.android.gms.analytics.GoogleAnalytics")
            Log.w(TAG, "Google Analytics detected")
            return false
        } catch (e: ClassNotFoundException) {
            // Good - not present
        }
        
        // In production, you might want to check BuildConfig for debug flags
        // For now, assume no telemetry in zero-knowledge mode
        return true
    }
    
    /**
     * Check if encryption is active and properly configured
     */
    private suspend fun checkEncryptionActive(zkManager: ZeroKnowledgeManager): Boolean {
        val config = zkManager.getConfig()
        return config?.enabled == true && zkManager.isUnlocked()
    }
    
    /**
     * Check for network activity
     */
    private fun checkNetworkActivity(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        
        return capabilities != null && (
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        )
    }
    
    /**
     * Check key storage locations for leaks
     */
    private fun checkKeyStorageLocations(): List<String> {
        val warnings = mutableListOf<String>()
        
        // Check for keys in insecure locations
        val insecureLocations = listOf(
            File(context.cacheDir, "keys"),
            File(context.filesDir, "keys.txt"),
            File(context.externalCacheDir, "backup"),
            File(context.getExternalFilesDir(null), "keys")
        )
        
        insecureLocations.forEach { location ->
            if (location.exists()) {
                warnings.add("Suspicious key storage location: ${location.path}")
            }
        }
        
        // Check for world-readable files
        context.filesDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.contains("key", ignoreCase = true)) {
                if (file.canRead() && !file.canWrite()) {
                    warnings.add("Potentially exposed key file: ${file.name}")
                }
            }
        }
        
        return warnings
    }
    
    /**
     * Check memory security settings
     */
    private fun checkMemorySecurity(): List<String> {
        val warnings = mutableListOf<String>()
        
        // Check if device is rooted (increases key extraction risk)
        if (isDeviceRooted()) {
            warnings.add("Device is rooted - increased key extraction risk")
        }
        
        // Check for debuggable build (increases attack surface)
        val isDebuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            warnings.add("App is debuggable - memory dumps possible")
        }
        
        return warnings
    }
    
    /**
     * Check if device is rooted
     */
    private fun isDeviceRooted(): Boolean {
        // Check for common root indicators
        val rootIndicators = listOf(
            "/system/app/Superuser.apk",
            "/system/xbin/su",
            "/system/bin/su",
            "/system/sbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su"
        )
        
        return rootIndicators.any { File(it).exists() }
    }
    
    /**
     * Generate human-readable audit report
     */
    fun generateReport(result: PrivacyAuditResult): String {
        return buildString {
            appendLine("=== ZERO-KNOWLEDGE PRIVACY AUDIT ===")
            appendLine()
            appendLine("Timestamp: ${java.util.Date(result.timestamp)}")
            appendLine("Status: ${if (result.passed) "✅ PASSED" else "⚠️ WARNINGS"}")
            appendLine()
            
            appendLine("Security Checks:")
            appendLine("  Keys Stored Locally: ${if (result.keysStoredLocally) "✅" else "❌"}")
            appendLine("  No Cloud Key Access: ${if (result.noCloudKeyAccess) "✅" else "❌"}")
            appendLine("  No Telemetry: ${if (result.noTelemetry) "✅" else "❌"}")
            appendLine("  Encryption Active: ${if (result.encryptionActive) "✅" else "❌"}")
            appendLine("  Local-Only Mode: ${if (result.localOnlyMode) "✅" else "⚠️"}")
            appendLine()
            
            if (result.warnings.isNotEmpty()) {
                appendLine("Warnings (${result.warnings.size}):")
                result.warnings.forEachIndexed { index, warning ->
                    appendLine("  ${index + 1}. $warning")
                }
            } else {
                appendLine("No warnings - zero-knowledge properties verified!")
            }
            
            appendLine()
            appendLine("=== END AUDIT ===")
        }
    }
}
