package com.obsidianbackup.rootcore.permissions

import com.obsidianbackup.rootcore.shell.ShellExecutor
import com.obsidianbackup.rootcore.shell.ShellResult
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auto-grant permissions via root using `pm grant` pattern.
 * Inspired by TWRP's permission auto-grant — when root is available,
 * we can grant dangerous permissions without user interaction.
 *
 * Only works with root access. Falls back gracefully without root.
 */
@Singleton
class RootPermissionGranter @Inject constructor(
    private val shellExecutor: ShellExecutor
) {
    companion object {
        private const val TAG = "RootPermGrant"

        /**
         * Permissions commonly needed by backup apps that can be auto-granted with root.
         */
        val BACKUP_PERMISSIONS = listOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
            "android.permission.PACKAGE_USAGE_STATS",
            "android.permission.QUERY_ALL_PACKAGES",
            "android.permission.REQUEST_INSTALL_PACKAGES",
            "android.permission.REQUEST_DELETE_PACKAGES",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.READ_SMS",
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG"
        )

        /**
         * System-level permissions that require root to grant.
         */
        val SYSTEM_PERMISSIONS = listOf(
            "android.permission.INTERACT_ACROSS_USERS_FULL",
            "android.permission.BACKUP",
            "android.permission.CONFIRM_FULL_BACKUP",
            "android.permission.WRITE_SECURE_SETTINGS"
        )
    }

    /**
     * Grant a single permission to a package via root.
     */
    suspend fun grantPermission(packageName: String, permission: String): ShellResult {
        Timber.d("$TAG Granting $permission to $packageName")
        return shellExecutor.executeRoot("pm grant $packageName $permission")
    }

    /**
     * Grant all common backup permissions to a package.
     * Silently skips permissions that fail (some may not be applicable on all API levels).
     */
    suspend fun grantBackupPermissions(packageName: String): GrantResult {
        return grantPermissions(packageName, BACKUP_PERMISSIONS)
    }

    /**
     * Grant a list of permissions, returning detailed results.
     */
    suspend fun grantPermissions(packageName: String, permissions: List<String>): GrantResult {
        val granted = mutableListOf<String>()
        val failed = mutableListOf<Pair<String, String>>()

        for (permission in permissions) {
            val result = grantPermission(packageName, permission)
            if (result.success) {
                granted.add(permission)
            } else {
                failed.add(permission to (result.stderr.ifEmpty { "Unknown error" }))
                Timber.w("$TAG Failed to grant $permission to $packageName: ${result.stderr}")
            }
        }

        Timber.i("$TAG Granted ${granted.size}/${permissions.size} permissions to $packageName")
        return GrantResult(granted = granted, failed = failed)
    }

    /**
     * Revoke a permission from a package via root.
     */
    suspend fun revokePermission(packageName: String, permission: String): ShellResult {
        Timber.d("$TAG Revoking $permission from $packageName")
        return shellExecutor.executeRoot("pm revoke $packageName $permission")
    }

    /**
     * Grant self (ObsidianBackup) all backup-related permissions.
     * Call this on first launch with root to avoid permission dialogs.
     */
    suspend fun grantSelfPermissions(selfPackageName: String): GrantResult {
        return grantBackupPermissions(selfPackageName)
    }

    /**
     * Set an app ops permission via appops command (for special permissions).
     */
    suspend fun setAppOps(packageName: String, op: String, mode: String = "allow"): ShellResult {
        Timber.d("$TAG Setting appops $op=$mode for $packageName")
        return shellExecutor.executeRoot("appops set $packageName $op $mode")
    }

    /**
     * Enable MANAGE_EXTERNAL_STORAGE for the app via appops (Android 11+).
     */
    suspend fun enableAllFilesAccess(packageName: String): ShellResult {
        return setAppOps(packageName, "MANAGE_EXTERNAL_STORAGE", "allow")
    }

    data class GrantResult(
        val granted: List<String>,
        val failed: List<Pair<String, String>>
    ) {
        val allSucceeded: Boolean get() = failed.isEmpty()
        val grantedCount: Int get() = granted.size
        val totalAttempted: Int get() = granted.size + failed.size
    }
}
