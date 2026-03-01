// storage/StoragePermissionHelper.kt
package com.obsidianbackup.storage

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.obsidianbackup.logging.ObsidianLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * StoragePermissionHelper - Manages storage-related permissions across Android versions
 * 
 * This class handles the complexity of storage permissions across different Android versions,
 * from legacy READ/WRITE_EXTERNAL_STORAGE to modern scoped storage patterns.
 */
@Singleton
class StoragePermissionHelper @Inject constructor(
    private val context: Context,
    private val logger: ObsidianLogger
) {
    
    companion object {
        private const val TAG = "StoragePermissionHelper"
    }

    /**
     * Check if app has necessary storage access for its primary operations
     * (app-private storage always available, no permissions needed)
     */
    fun hasAppPrivateStorageAccess(): Boolean {
        // App-private storage (getExternalFilesDir) is always available
        // No permissions required since Android 4.4 (API 19)
        return true
    }

    /**
     * Check if app can access all files (legacy or MANAGE_EXTERNAL_STORAGE)
     * This is only needed for advanced root/Shizuku features
     */
    fun hasAllFilesAccess(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ (API 30+): Check MANAGE_EXTERNAL_STORAGE
                Environment.isExternalStorageManager()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10 (API 29): Check legacy permissions
                hasLegacyStoragePermissions()
            }
            else -> {
                // Android 9 and below: Check READ/WRITE permissions
                hasLegacyStoragePermissions()
            }
        }
    }

    /**
     * Check if app has legacy READ/WRITE_EXTERNAL_STORAGE permissions
     */
    fun hasLegacyStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) { // API 32
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Not needed on Android 13+
            true
        }
    }

    /**
     * Check if app has media permissions (Android 13+)
     */
    fun hasMediaPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Not applicable on older versions
            true
        }
    }

    /**
     * Get list of permissions that need to be requested based on Android version
     * For normal operation: Returns empty list (no permissions needed)
     * For advanced features: Returns necessary permissions
     */
    fun getRequiredPermissionsForAdvancedFeatures(): List<String> {
        val permissions = mutableListOf<String>()
        
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ (API 33+)
                // Media permissions for accessing user's media files (optional)
                if (!hasMediaPermissions()) {
                    permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                    permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11-12 (API 30-32)
                // No runtime permissions needed for scoped storage
                // MANAGE_EXTERNAL_STORAGE requires special intent (handled separately)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10 (API 29)
                if (!hasLegacyStoragePermissions()) {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            else -> {
                // Android 9 and below (API ≤28)
                if (!hasLegacyStoragePermissions()) {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
        
        return permissions
    }

    /**
     * Create intent to request MANAGE_EXTERNAL_STORAGE permission
     * Only use for advanced features (root/Shizuku operations)
     */
    fun createManageStorageIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            } catch (e: Exception) {
                logger.e(TAG, "Failed to create MANAGE_EXTERNAL_STORAGE intent", e)
                // Fallback to general settings
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            }
        } else {
            logger.w(TAG, "MANAGE_EXTERNAL_STORAGE not available on this Android version")
            null
        }
    }

    /**
     * Get storage permission status for UI display
     */
    fun getStoragePermissionStatus(): StoragePermissionStatus {
        return StoragePermissionStatus(
            hasAppPrivateAccess = hasAppPrivateStorageAccess(),
            hasAllFilesAccess = hasAllFilesAccess(),
            hasLegacyPermissions = hasLegacyStoragePermissions(),
            hasMediaPermissions = hasMediaPermissions(),
            canRequestManageStorage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
            recommendedApproach = getRecommendedStorageApproach()
        )
    }

    /**
     * Get recommended storage approach for current Android version
     */
    private fun getRecommendedStorageApproach(): StorageApproach {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10+ (API 29+): Use scoped storage
                StorageApproach.SCOPED_STORAGE
            }
            else -> {
                // Android 9 and below: Use legacy storage
                StorageApproach.LEGACY_STORAGE
            }
        }
    }

    /**
     * Check if scoped storage is enforced
     */
    fun isScopedStorageEnforced(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    /**
     * Log storage permission status for debugging
     */
    fun logStorageStatus() {
        val status = getStoragePermissionStatus()
        logger.i(TAG, """
            Storage Permission Status:
            - App Private Access: ${status.hasAppPrivateAccess}
            - All Files Access: ${status.hasAllFilesAccess}
            - Legacy Permissions: ${status.hasLegacyPermissions}
            - Media Permissions: ${status.hasMediaPermissions}
            - Can Request Manage Storage: ${status.canRequestManageStorage}
            - Recommended Approach: ${status.recommendedApproach}
            - Scoped Storage Enforced: ${isScopedStorageEnforced()}
            - Android Version: ${Build.VERSION.SDK_INT}
        """.trimIndent())
    }

    /**
     * Data class for storage permission status
     */
    data class StoragePermissionStatus(
        val hasAppPrivateAccess: Boolean,
        val hasAllFilesAccess: Boolean,
        val hasLegacyPermissions: Boolean,
        val hasMediaPermissions: Boolean,
        val canRequestManageStorage: Boolean,
        val recommendedApproach: StorageApproach
    )

    /**
     * Storage approach enum
     */
    enum class StorageApproach {
        /** App-private storage (getExternalFilesDir) - No permissions needed */
        APP_PRIVATE,
        
        /** Scoped storage (MediaStore, SAF) - Android 10+ */
        SCOPED_STORAGE,
        
        /** Legacy external storage - Android 9 and below */
        LEGACY_STORAGE
    }
}
