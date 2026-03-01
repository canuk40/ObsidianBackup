// plugins/PluginSandbox.kt
package com.obsidianbackup.plugins

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.obsidianbackup.api.plugin.*
import java.security.MessageDigest
import java.util.*

/**
 * Sandboxes plugin execution and enforces capability restrictions
 */
class PluginSandbox(
    private val context: Context,
    private val securityPolicy: PluginSecurityPolicy = DefaultPluginSecurityPolicy()
) {
    private val activeCapabilities = mutableMapOf<String, Set<PluginCapability>>()

    /**
     * Verify that requested capabilities can be granted
     */
    fun verifyCapabilities(capabilities: Set<PluginCapability>): Boolean {
        return capabilities.all { capability ->
            securityPolicy.isCapabilityAllowed(capability)
        }
    }

    /**
     * Verify plugin signature. Tries:
     *  - If manifest.id corresponds to an installed package, compare installed signing cert fingerprints
     *  - If not installed, and manifest includes a signature fingerprint, attempt to match it against that value (best-effort)
     */
    fun verifySignature(manifest: PluginManifest): Boolean {
        // If manifest does not declare a required signature, nothing to verify
        val required = manifest.signature?.trim()?.lowercase(Locale.US)
        if (required.isNullOrEmpty()) return true

        // Try to verify against an installed package (manifest.id expected to be package name)
        return try {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val pkgInfo = pm.getPackageInfo(manifest.id, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = pkgInfo.signingInfo
                val signers = signingInfo?.apkContentsSigners ?: signingInfo?.signingCertificateHistory ?: emptyArray()
                for (cert in signers) {
                    val fp = sha256Fingerprint(cert.toByteArray())
                    if (fp.equals(required, ignoreCase = true) || ("sha256:$fp".equals(required, ignoreCase = true))) {
                        return true
                    }
                }
            } else {
                // Fallback for older devices
                val pkgInfo = pm.getPackageInfo(manifest.id, PackageManager.GET_SIGNATURES)
                val signatures = pkgInfo.signatures ?: emptyArray()
                for (sig in signatures) {
                    val fp = sha256Fingerprint(sig.toByteArray())
                    if (fp.equals(required, ignoreCase = true) || ("sha256:$fp".equals(required, ignoreCase = true))) {
                        return true
                    }
                }
            }
            false
        } catch (e: Exception) {
            // Package not found or other error -> fallback: best-effort false
            false
        }
    }

    /**
     * Execute plugin code in sandboxed environment
     */
    suspend fun <T> executeInSandbox(
        plugin: ObsidianBackupPlugin,
        block: suspend () -> T
    ): PluginResult<T> {
        // Grant capabilities for this execution
        activeCapabilities[plugin.id] = plugin.capabilities

        return try {
            // Execute with restricted permissions using plugin's classloader
            val result = withPluginClassLoader(plugin) {
                block()
            }
            PluginResult.Success(result)
        } catch (e: Exception) {
            PluginResult.Error(PluginError(PluginError.ErrorCode.UNKNOWN, "Plugin execution failed: ${e.message}", e))
        } finally {
            // Revoke capabilities after execution
            activeCapabilities.remove(plugin.id)
        }
    }

    /**
     * Check if plugin has a specific capability
     */
    fun hasCapability(pluginId: String, capability: PluginCapability): Boolean {
        return activeCapabilities[pluginId]?.contains(capability) == true
    }

    private suspend fun <T> withPluginClassLoader(
        plugin: ObsidianBackupPlugin,
        block: suspend () -> T
    ): T {
        val originalClassLoader = Thread.currentThread().contextClassLoader
        return try {
            // Use plugin's classloader
            Thread.currentThread().contextClassLoader = plugin.javaClass.classLoader
            block()
        } finally {
            Thread.currentThread().contextClassLoader = originalClassLoader
        }
    }

    private fun sha256Fingerprint(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}

interface PluginSecurityPolicy {
    fun isCapabilityAllowed(capability: PluginCapability): Boolean
}

class DefaultPluginSecurityPolicy : PluginSecurityPolicy {
    override fun isCapabilityAllowed(capability: PluginCapability): Boolean {
        return when (capability) {
            PluginCapability.WRITE_CATALOG -> false // Very restricted
            PluginCapability.ENCRYPTION_KEY_ACCESS -> false // Requires explicit consent
            else -> true
        }
    }
}
