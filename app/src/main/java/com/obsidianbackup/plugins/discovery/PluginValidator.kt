// plugins/discovery/PluginValidator.kt
package com.obsidianbackup.plugins.discovery

import android.content.pm.PackageManager
import android.content.Context
import android.os.Build
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.plugins.api.PluginApiVersion
import com.obsidianbackup.plugins.api.PluginMetadata
import java.security.MessageDigest

class PluginValidator(
    private val context: Context,
    private val logger: ObsidianLogger
) {

    data class ValidationResult(
        val valid: Boolean,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList()
    )

    fun validate(plugin: PluginMetadata): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Basic validation
        if (plugin.packageName.isBlank()) {
            errors.add("Package name is required")
        }

        if (plugin.className.isBlank()) {
            errors.add("Class name is required")
        }

        if (plugin.name.isBlank()) {
            errors.add("Plugin name is required")
        }

        // API version validation
        if (plugin.apiVersion.version > PluginApiVersion.CURRENT.version) {
            errors.add("Plugin requires newer API version: ${plugin.apiVersion} > ${PluginApiVersion.CURRENT}")
        }

        // SDK version validation
        if (Build.VERSION.SDK_INT < plugin.minSdkVersion) {
            errors.add("Device SDK version ${Build.VERSION.SDK_INT} < required ${plugin.minSdkVersion}")
        }

        // Signature validation (for signed plugins)
        if (plugin.signatureSha256 != null) {
            if (!validateSignature(plugin)) {
                errors.add("Plugin signature validation failed")
            }
        } else {
            warnings.add("Plugin is not signed - consider using signed plugins for security")
        }

        // Capability validation
        validateCapabilities(plugin, errors, warnings)

        // Class accessibility validation
        if (!validateClassAccessibility(plugin)) {
            errors.add("Plugin class ${plugin.className} is not accessible")
        }

        val valid = errors.isEmpty()

        if (valid) {
            logger.d(TAG, "Plugin ${plugin.name} validation passed")
        } else {
            logger.w(TAG, "Plugin ${plugin.name} validation failed: ${errors.joinToString(", ")}")
        }

        return ValidationResult(valid, errors, warnings)
    }

    private fun validateSignature(plugin: PluginMetadata): Boolean {
        val expectedHash = plugin.signatureSha256 ?: return false
        return try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    plugin.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(plugin.packageName, PackageManager.GET_SIGNATURES)
            }

            val signers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                info.signatures
            }

            val actualHash = signers?.firstOrNull()?.let { sig ->
                val md = MessageDigest.getInstance("SHA-256")
                md.update(sig.toByteArray())
                md.digest().joinToString("") { "%02x".format(it) }
            } ?: return false

            actualHash == expectedHash
        } catch (e: PackageManager.NameNotFoundException) {
            logger.w(TAG, "Plugin package not found: ${plugin.packageName}")
            false
        } catch (e: Exception) {
            logger.e(TAG, "Signature validation failed for ${plugin.packageName}", e)
            false
        }
    }

    private fun validateCapabilities(
        plugin: PluginMetadata,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val capabilities = plugin.capabilities

        // Check for conflicting capabilities
        if (capabilities.contains(com.obsidianbackup.plugins.api.PluginCapability.IncrementalBackup) &&
            !capabilities.contains(com.obsidianbackup.plugins.api.PluginCapability.CompressionSupport)) {
            warnings.add("Incremental backup without compression may be inefficient")
        }

        // Check for required capabilities based on plugin type
        // This is a simplified check - in practice, you'd have more sophisticated validation
        if (capabilities.isEmpty()) {
            warnings.add("Plugin declares no capabilities - may be limited in functionality")
        }
    }

    private fun validateClassAccessibility(plugin: PluginMetadata): Boolean {
        return try {
            // Try to load the class to check if it's accessible
            Class.forName(plugin.className)
            true
        } catch (e: ClassNotFoundException) {
            false
        } catch (e: Exception) {
            logger.e(TAG, "Unexpected error validating class ${plugin.className}", e)
            false
        }
    }

    companion object {
        private const val TAG = "PluginValidator"
    }
}
