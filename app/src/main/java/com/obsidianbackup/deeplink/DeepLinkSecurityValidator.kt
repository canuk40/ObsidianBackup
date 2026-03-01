package com.obsidianbackup.deeplink

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.net.Uri
import android.os.Build
import com.obsidianbackup.logging.ObsidianLogger
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Security validator for deep links to prevent URI interception attacks
 * 
 * Validates:
 * - Custom scheme (obsidianbackup://) links require trusted app signatures
 * - HTTPS app links are automatically verified by Android
 * - All deep link attempts are audited for security analysis
 */
@Singleton
class DeepLinkSecurityValidator @Inject constructor(
    private val context: Context,
    private val logger: ObsidianLogger
) {
    
    private val packageManager: PackageManager = context.packageManager
    
    /**
     * Whitelist of trusted package signatures (SHA-256 hashes)
     * These packages are allowed to send obsidianbackup:// custom scheme deep links
     * 
     * Add your trusted automation apps, companion apps, or custom launchers here
     */
    private val trustedSignatures = setOf(
        // ObsidianBackup itself (this app)
        getOwnAppSignature(),
        
        // Tasker - automation app
        "E0:89:8E:49:89:6F:FA:5A:D7:84:A3:11:24:D3:93:0F:7F:0B:09:B9:91:25:EF:1E:F7:B0:D8:A0:BC:B0:5A:85",
        
        // MacroDroid - automation app  
        "3F:4F:8E:2F:22:13:67:E6:5D:F7:0B:F2:3F:D0:8C:5E:96:7E:44:93:C8:3F:F9:0D:B2:4E:A1:73:5F:8D:4C:1A",
        
        // Termux - terminal emulator (common for advanced backup scripts)
        "26:F1:C2:4A:F6:DF:3F:8B:4C:70:4A:E4:7B:36:1A:7C:4B:F5:2F:6A:8D:1B:6E:0D:3C:C2:7F:9A:8F:11:B3:5D"
    )
    
    /**
     * Whitelist of trusted package names (optional layer)
     * These must also have matching signatures from trustedSignatures
     */
    private val trustedPackages = setOf(
        context.packageName, // This app
        "net.dinglisch.android.taskerm", // Tasker
        "com.arlosoft.macrodroid", // MacroDroid
        "com.termux" // Termux
    )
    
    /**
     * Validate a deep link's origin to prevent URI interception attacks
     * 
     * @param uri The deep link URI
     * @param callingPackage The package name of the calling app (null if unknown)
     * @return ValidationResult with success status and audit details
     */
    fun validateDeepLinkOrigin(uri: Uri, callingPackage: String?): ValidationResult {
        val scheme = uri.scheme?.lowercase()
        val timestamp = System.currentTimeMillis()
        
        logger.i(TAG, "Validating deep link origin: scheme=$scheme, caller=$callingPackage, uri=$uri")
        
        // HTTPS app links are verified by Android's App Link system - always trusted
        if (scheme == "https") {
            val result = ValidationResult(
                allowed = true,
                reason = "HTTPS app link (verified by Android)",
                scheme = scheme,
                callingPackage = callingPackage,
                signatureVerified = true
            )
            logAudit(uri, callingPackage, result, timestamp)
            return result
        }
        
        // Custom scheme (obsidianbackup://) requires signature verification
        if (scheme == "obsidianbackup") {
            
            // No calling package - potentially malicious
            if (callingPackage == null) {
                val result = ValidationResult(
                    allowed = false,
                    reason = "Custom scheme deep link with no caller identity - potential URI interception",
                    scheme = scheme,
                    callingPackage = null,
                    signatureVerified = false
                )
                logger.w(TAG, "SECURITY: Rejected deep link with no caller: $uri")
                logAudit(uri, callingPackage, result, timestamp)
                return result
            }
            
            // Verify calling app is in whitelist
            if (!trustedPackages.contains(callingPackage)) {
                val result = ValidationResult(
                    allowed = false,
                    reason = "Calling package '$callingPackage' is not in trusted whitelist",
                    scheme = scheme,
                    callingPackage = callingPackage,
                    signatureVerified = false
                )
                logger.w(TAG, "SECURITY: Rejected deep link from untrusted package: $callingPackage")
                logAudit(uri, callingPackage, result, timestamp)
                return result
            }
            
            // Verify calling app's signature
            val signatureValid = verifyAppSignature(callingPackage)
            if (!signatureValid) {
                val result = ValidationResult(
                    allowed = false,
                    reason = "Calling package '$callingPackage' signature does not match trusted whitelist",
                    scheme = scheme,
                    callingPackage = callingPackage,
                    signatureVerified = false
                )
                logger.w(TAG, "SECURITY: Rejected deep link - signature mismatch for package: $callingPackage")
                logAudit(uri, callingPackage, result, timestamp)
                return result
            }
            
            // All checks passed
            val result = ValidationResult(
                allowed = true,
                reason = "Verified trusted app signature",
                scheme = scheme,
                callingPackage = callingPackage,
                signatureVerified = true
            )
            logger.i(TAG, "Deep link validated successfully from trusted app: $callingPackage")
            logAudit(uri, callingPackage, result, timestamp)
            return result
        }
        
        // Unknown/unsupported scheme
        val result = ValidationResult(
            allowed = false,
            reason = "Unsupported URI scheme: $scheme",
            scheme = scheme,
            callingPackage = callingPackage,
            signatureVerified = false
        )
        logger.w(TAG, "SECURITY: Rejected deep link with unsupported scheme: $scheme")
        logAudit(uri, callingPackage, result, timestamp)
        return result
    }
    
    /**
     * Verify an app's signature against the trusted whitelist
     * Uses API 28+ signing certificate API when available
     */
    fun verifyAppSignature(packageName: String): Boolean {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // API 28+ - use GET_SIGNING_CERTIFICATES
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                // API 27 and below - use deprecated GET_SIGNATURES
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                )
            }
            
            val signatures = extractSignatures(packageInfo)
            
            // Check if any signature matches the whitelist
            val signatureHashes = signatures.map { computeSignatureHash(it) }
            val hasMatch = signatureHashes.any { hash -> trustedSignatures.contains(hash) }
            
            if (!hasMatch) {
                logger.w(TAG, "Package $packageName signatures: ${signatureHashes.joinToString()}")
                logger.w(TAG, "None match trusted whitelist")
            }
            
            hasMatch
            
        } catch (e: PackageManager.NameNotFoundException) {
            logger.e(TAG, "Package not found: $packageName", e)
            false
        } catch (e: Exception) {
            logger.e(TAG, "Error verifying signature for $packageName", e)
            false
        }
    }
    
    /**
     * Extract signatures from PackageInfo based on API level
     */
    private fun extractSignatures(packageInfo: PackageInfo): Array<Signature> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // API 28+
            val signingInfo: SigningInfo? = packageInfo.signingInfo
            if (signingInfo == null) {
                emptyArray()
            } else if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            // API 27 and below
            @Suppress("DEPRECATION")
            packageInfo.signatures ?: emptyArray()
        }
    }
    
    /**
     * Compute SHA-256 hash of a signature
     * Returns hash in colon-separated hex format
     */
    private fun computeSignatureHash(signature: Signature): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(signature.toByteArray())
        return hash.joinToString(":") { "%02X".format(it) }
    }
    
    /**
     * Get this app's own signature for self-trust
     */
    private fun getOwnAppSignature(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }
            
            val signatures = extractSignatures(packageInfo)
            if (signatures.isNotEmpty()) {
                computeSignatureHash(signatures[0])
            } else {
                "UNKNOWN"
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to get own app signature", e)
            "UNKNOWN"
        }
    }
    
    /**
     * Log security audit entry for deep link validation
     * Includes all relevant details for security analysis
     */
    private fun logAudit(
        uri: Uri,
        callingPackage: String?,
        result: ValidationResult,
        timestamp: Long
    ) {
        val metadata = mutableMapOf(
            "timestamp" to timestamp.toString(),
            "uri" to uri.toString(),
            "scheme" to (uri.scheme ?: "null"),
            "host" to (uri.host ?: "null"),
            "allowed" to result.allowed.toString(),
            "reason" to result.reason,
            "callingPackage" to (callingPackage ?: "null"),
            "signatureVerified" to result.signatureVerified.toString()
        )
        
        // Add calling package signature if available
        if (callingPackage != null) {
            try {
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageManager.getPackageInfo(
                        callingPackage,
                        PackageManager.GET_SIGNING_CERTIFICATES
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(
                        callingPackage,
                        PackageManager.GET_SIGNATURES
                    )
                }
                val signatures = extractSignatures(packageInfo)
                if (signatures.isNotEmpty()) {
                    metadata["callerSignature"] = computeSignatureHash(signatures[0])
                }
            } catch (e: Exception) {
                // Ignore - signature not critical for audit
            }
        }
        
        val logLevel = if (result.allowed) "INFO" else "WARN"
        logger.log(
            if (result.allowed) com.obsidianbackup.logging.LogLevel.INFO else com.obsidianbackup.logging.LogLevel.WARN,
            TAG,
            "SECURITY_AUDIT: Deep link ${if (result.allowed) "ALLOWED" else "REJECTED"}",
            metadata = metadata
        )
    }
    
    /**
     * Get list of trusted package names (for diagnostic/debug purposes)
     */
    fun getTrustedPackages(): Set<String> = trustedPackages
    
    /**
     * Check if a package is trusted (without signature verification)
     */
    fun isPackageTrusted(packageName: String): Boolean {
        return trustedPackages.contains(packageName)
    }
    
    companion object {
        private const val TAG = "DeepLinkSecurityValidator"
    }
}

/**
 * Result of deep link origin validation
 */
data class ValidationResult(
    val allowed: Boolean,
    val reason: String,
    val scheme: String?,
    val callingPackage: String?,
    val signatureVerified: Boolean
)
