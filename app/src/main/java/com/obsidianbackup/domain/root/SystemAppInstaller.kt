package com.obsidianbackup.domain.root

import com.obsidianbackup.rootcore.shell.ShellExecutor
import com.obsidianbackup.rootcore.selinux.SELinuxRestoreHelper
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * System app installer — install/uninstall apps to /system/app or /system/priv-app.
 *
 * Requires root and remounting /system as read-write.
 * Handles SELinux context restoration and permission fixes.
 */
@Singleton
class SystemAppInstaller @Inject constructor(
    private val shellExecutor: ShellExecutor,
    private val selinuxHelper: SELinuxRestoreHelper
) {
    companion object {
        private const val TAG = "[SystemApp]"
        private const val SYSTEM_APP = "/system/app"
        private const val SYSTEM_PRIV_APP = "/system/priv-app"
    }

    data class InstallResult(
        val packageName: String,
        val success: Boolean,
        val message: String,
        val installPath: String? = null
    )

    /**
     * Install an APK as a system app.
     *
     * @param apkPath Path to the APK file
     * @param packageName Package name for the directory
     * @param privileged Install to priv-app (grants signature-level permissions)
     */
    suspend fun installAsSystemApp(
        apkPath: String,
        packageName: String,
        privileged: Boolean = false
    ): InstallResult {
        val targetBase = if (privileged) SYSTEM_PRIV_APP else SYSTEM_APP
        val targetDir = "$targetBase/$packageName"

        // Remount /system as rw
        val mountResult = shellExecutor.executeRoot("mount -o rw,remount /system")
        if (!mountResult.success) {
            // Try mount-master approach for Magisk
            val mmResult = shellExecutor.executeRoot("mount -o rw,remount /")
            if (!mmResult.success) {
                return InstallResult(packageName, false, "Cannot remount /system: ${mountResult.stderr}")
            }
        }

        try {
            // Create target directory
            shellExecutor.executeRoot("mkdir -p $targetDir")

            // Copy APK
            val copyResult = shellExecutor.executeRoot("cp $apkPath $targetDir/base.apk")
            if (!copyResult.success) {
                return InstallResult(packageName, false, "Copy failed: ${copyResult.stderr}")
            }

            // Set permissions
            shellExecutor.executeRoot("chmod 755 $targetDir")
            shellExecutor.executeRoot("chmod 644 $targetDir/base.apk")
            shellExecutor.executeRoot("chown -R root:root $targetDir")

            // Restore SELinux context
            selinuxHelper.fixPermissionsAfterRestore(packageName, 0, targetDir)

            Timber.d("$TAG Installed $packageName to $targetDir")
            return InstallResult(packageName, true, "Installed as system app", targetDir)
        } finally {
            // Remount /system as ro
            shellExecutor.executeRoot("mount -o ro,remount /system")
        }
    }

    /**
     * Remove a system app.
     */
    suspend fun removeSystemApp(packageName: String): InstallResult {
        val paths = listOf(
            "$SYSTEM_APP/$packageName",
            "$SYSTEM_PRIV_APP/$packageName"
        )

        val mountResult = shellExecutor.executeRoot("mount -o rw,remount /system")
        if (!mountResult.success) {
            shellExecutor.executeRoot("mount -o rw,remount /")
        }

        try {
            for (path in paths) {
                val check = shellExecutor.executeRoot("test -d $path && echo EXISTS")
                if (check.stdout.contains("EXISTS")) {
                    shellExecutor.executeRoot("rm -rf $path")
                    Timber.d("$TAG Removed system app: $path")
                    return InstallResult(packageName, true, "Removed from $path", path)
                }
            }

            return InstallResult(packageName, false, "System app not found")
        } finally {
            shellExecutor.executeRoot("mount -o ro,remount /system")
        }
    }

    /**
     * Convert a user app to system app (move from /data/app to /system/app).
     */
    suspend fun convertToSystemApp(packageName: String, privileged: Boolean = false): InstallResult {
        // Find current APK path
        val pathResult = shellExecutor.executeRoot("pm path $packageName")
        if (!pathResult.success) {
            return InstallResult(packageName, false, "Package not found")
        }

        val apkPath = pathResult.stdout.lines()
            .firstOrNull { it.startsWith("package:") }
            ?.removePrefix("package:")
            ?.trim()
            ?: return InstallResult(packageName, false, "Cannot determine APK path")

        return installAsSystemApp(apkPath, packageName, privileged)
    }

    /**
     * Check if a package is installed as a system app.
     */
    suspend fun isSystemApp(packageName: String): Boolean {
        val result = shellExecutor.executeRoot("pm dump $packageName | grep 'flags=' | head -1")
        return result.success && result.stdout.contains("SYSTEM")
    }
}
