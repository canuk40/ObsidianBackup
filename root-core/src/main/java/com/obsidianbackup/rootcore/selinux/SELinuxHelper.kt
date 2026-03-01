package com.obsidianbackup.rootcore.selinux

import com.obsidianbackup.rootcore.shell.ShellExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper for SELinux operations during backup/restore.
 *
 * Patterns derived from:
 * - Magisk's chcon u:object_r:system_file:s0 pattern
 * - Titanium Backup's restorecon -R after data restore
 * - ObsidianBox's 3-tier SELinux detection
 */
@Singleton
class SELinuxHelper @Inject constructor(
    private val shellExecutor: ShellExecutor
) {
    companion object {
        /** Standard context for system binaries. */
        const val SYSTEM_FILE_CONTEXT = "u:object_r:system_file:s0"
        /** Standard context for app data directories. */
        const val APP_DATA_CONTEXT = "u:object_r:app_data_file:s0"
    }

    /**
     * Get current SELinux enforcement mode.
     * Uses 3-tier detection: sysfs → getenforce → su -c getenforce.
     */
    suspend fun getEnforcementMode(): String = withContext(Dispatchers.IO) {
        // Tier 1: Read from kernel interface (fastest)
        try {
            val enforceFile = File("/sys/fs/selinux/enforce")
            if (enforceFile.exists() && enforceFile.canRead()) {
                return@withContext when (enforceFile.readText().trim()) {
                    "1" -> "Enforcing"
                    "0" -> "Permissive"
                    else -> "Unknown"
                }
            }
        } catch (_: Exception) { }

        // Tier 2: getenforce command (no root)
        try {
            val result = shellExecutor.executeShell("getenforce")
            if (result.isSuccess) {
                return@withContext when (result.stdout.trim().lowercase()) {
                    "enforcing" -> "Enforcing"
                    "permissive" -> "Permissive"
                    "disabled" -> "Disabled"
                    else -> "Unknown"
                }
            }
        } catch (_: Exception) { }

        // Tier 3: getenforce via root
        try {
            val result = shellExecutor.executeRootUnsafe("getenforce")
            if (result.isSuccess) {
                return@withContext when (result.stdout.trim().lowercase()) {
                    "enforcing" -> "Enforcing"
                    "permissive" -> "Permissive"
                    "disabled" -> "Disabled"
                    else -> "Unknown"
                }
            }
        } catch (_: Exception) { }

        "Unknown"
    }

    /**
     * Restore SELinux contexts on a path after data restore.
     * Critical for Android 4.3+ — without this, restored apps may not function.
     */
    suspend fun restoreContext(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = shellExecutor.executeRootUnsafe("restorecon -R $path")
            if (result.isSuccess) {
                Timber.d("SELinux contexts restored for: $path")
                true
            } else {
                Timber.w("restorecon failed for $path: ${result.stderr}")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to restore SELinux context for: $path")
            false
        }
    }

    /**
     * Set a specific SELinux context on a file/directory.
     */
    suspend fun setContext(path: String, context: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = shellExecutor.executeRootUnsafe("chcon $context $path 2>/dev/null")
            result.isSuccess
        } catch (e: Exception) {
            Timber.d("Failed to set SELinux context: ${e.message}")
            false
        }
    }

    /**
     * Get the SELinux context of a file/directory.
     */
    suspend fun getContext(path: String): String? = withContext(Dispatchers.IO) {
        try {
            val result = shellExecutor.executeRootUnsafe("ls -Z $path 2>/dev/null | head -1")
            if (result.isSuccess && result.stdout.isNotBlank()) {
                // Output format: "u:object_r:app_data_file:s0 /data/data/com.example"
                result.stdout.trim().split(Regex("\\s+")).firstOrNull()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if SELinux is in enforcing mode.
     */
    suspend fun isEnforcing(): Boolean {
        return getEnforcementMode() == "Enforcing"
    }
}
