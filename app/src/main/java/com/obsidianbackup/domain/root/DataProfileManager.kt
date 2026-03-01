package com.obsidianbackup.domain.root

import com.obsidianbackup.rootcore.shell.ShellExecutor
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data profiles manager — allows multiple data profiles per app.
 *
 * Inspired by Titanium Backup's data profile feature. Swaps /data/data/<pkg>/ contents
 * to support multiple identities (e.g., "Work" and "Personal") for the same app.
 *
 * Profiles are stored in /data/obsidian_profiles/<pkg>/<profile_name>/.
 */
@Singleton
class DataProfileManager @Inject constructor(
    private val shellExecutor: ShellExecutor
) {
    companion object {
        private const val TAG = "[DataProfiles]"
        private const val PROFILES_BASE = "/data/obsidian_profiles"
    }

    data class DataProfile(
        val packageName: String,
        val profileName: String,
        val sizeBytes: Long = 0,
        val isActive: Boolean = false
    )

    /**
     * Save current app data as a named profile.
     */
    suspend fun saveProfile(packageName: String, profileName: String): Result<DataProfile> = runCatching {
        val dataDir = "/data/data/$packageName"
        val profileDir = "$PROFILES_BASE/$packageName/$profileName"

        // Stop the app
        shellExecutor.executeRoot("am force-stop $packageName")

        // Create profile directory
        shellExecutor.executeRoot("mkdir -p $profileDir")

        // Copy current data to profile
        val result = shellExecutor.executeRoot("cp -a $dataDir/. $profileDir/")
        if (!result.success) {
            throw RuntimeException("Failed to save profile: ${result.stderr}")
        }

        val size = getDirectorySize(profileDir)
        Timber.d("$TAG Saved profile '$profileName' for $packageName (${size / 1024}KB)")
        DataProfile(packageName, profileName, size, isActive = true)
    }

    /**
     * Switch to a different data profile by swapping /data/data/<pkg>/ contents.
     */
    suspend fun switchProfile(packageName: String, profileName: String): Result<Unit> = runCatching {
        val dataDir = "/data/data/$packageName"
        val profileDir = "$PROFILES_BASE/$packageName/$profileName"

        // Verify profile exists
        val check = shellExecutor.executeRoot("test -d $profileDir && echo EXISTS")
        if (!check.stdout.contains("EXISTS")) {
            throw RuntimeException("Profile '$profileName' not found")
        }

        // Stop the app
        shellExecutor.executeRoot("am force-stop $packageName")

        // Get app UID for permissions
        val uidResult = shellExecutor.executeRoot("stat -c %u $dataDir")
        val uid = uidResult.stdout.trim()

        // Save current state as "_current" backup
        val currentBackup = "$PROFILES_BASE/$packageName/_current"
        shellExecutor.executeRoot("rm -rf $currentBackup && mkdir -p $currentBackup")
        shellExecutor.executeRoot("cp -a $dataDir/. $currentBackup/")

        // Clear current data and restore profile
        shellExecutor.executeRoot("rm -rf $dataDir/*")
        val restoreResult = shellExecutor.executeRoot("cp -a $profileDir/. $dataDir/")
        if (!restoreResult.success) {
            // Rollback
            shellExecutor.executeRoot("rm -rf $dataDir/* && cp -a $currentBackup/. $dataDir/")
            throw RuntimeException("Failed to switch profile: ${restoreResult.stderr}")
        }

        // Fix ownership and SELinux context
        shellExecutor.executeRoot("chown -R $uid:$uid $dataDir")
        shellExecutor.executeRoot("restorecon -R $dataDir")

        Timber.d("$TAG Switched $packageName to profile '$profileName'")
    }

    /**
     * Delete a saved profile.
     */
    suspend fun deleteProfile(packageName: String, profileName: String): Boolean {
        val result = shellExecutor.executeRoot("rm -rf $PROFILES_BASE/$packageName/$profileName")
        return result.success
    }

    /**
     * List all saved profiles for a package.
     */
    suspend fun listProfiles(packageName: String): List<DataProfile> {
        val profileBase = "$PROFILES_BASE/$packageName"
        val result = shellExecutor.executeRoot("ls -1 $profileBase 2>/dev/null")
        if (!result.success) return emptyList()

        return result.stdout.lines()
            .filter { it.isNotBlank() && it != "_current" }
            .map { name ->
                val size = getDirectorySize("$profileBase/$name")
                DataProfile(packageName, name, size)
            }
    }

    /**
     * Get storage usage for all profiles of all packages.
     */
    suspend fun getTotalProfileSize(): Long {
        return getDirectorySize(PROFILES_BASE)
    }

    private suspend fun getDirectorySize(path: String): Long {
        val result = shellExecutor.executeRoot("du -sb $path 2>/dev/null")
        return if (result.success) {
            result.stdout.trim().split("\\s+".toRegex()).firstOrNull()?.toLongOrNull() ?: 0L
        } else 0L
    }
}
