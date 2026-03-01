// permissions/PermissionManager.kt
package com.obsidianbackup.permissions

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.model.PermissionCapabilities
import com.obsidianbackup.model.PermissionMode
import com.obsidianbackup.model.RootType
import com.obsidianbackup.rootcore.detection.RootDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.content.SharedPreferences
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    private val context: Context,
    private val logger: ObsidianLogger,
    private val rootDetector: RootDetector
) {
    companion object {
        private const val TAG = "PermissionManager"
        private const val PREFS_NAME = "permission_manager"
        private const val KEY_FORCED_MODE = "forced_mode"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _currentMode = MutableStateFlow(PermissionMode.SAF)
    val currentMode: StateFlow<PermissionMode> = _currentMode.asStateFlow()

    private val _capabilities = MutableStateFlow(PermissionCapabilities())
    val capabilities: StateFlow<PermissionCapabilities> = _capabilities.asStateFlow()

    // Cache detection results
    private var cachedCapabilities: PermissionCapabilities? = null
    private var lastDetectionTime: Long = 0
    private val cacheValidityMs = 30_000L // 30 seconds cache
    
    // Real-time permission change monitoring
    private var isMonitoring = false

    /**
     * Detect capabilities comprehensively
     * Returns cached result if recent, otherwise performs full detection
     */
    suspend fun detectCapabilities(): PermissionCapabilities {
        val currentTime = System.currentTimeMillis()
        
        // Return cached result if still valid
        if (cachedCapabilities != null && (currentTime - lastDetectionTime) < cacheValidityMs) {
            return cachedCapabilities!!
        }

        // Perform full detection
        return withContext(Dispatchers.IO) {
            try {
                val caps = performFullDetection()
                cachedCapabilities = caps
                lastDetectionTime = currentTime
                _capabilities.value = caps
                logger.i(TAG, "Capabilities detected: $caps")
                caps
            } catch (e: Exception) {
                logger.e(TAG, "Error detecting capabilities", e)
                // Return safe defaults on error
                PermissionCapabilities()
            }
        }
    }

    private suspend fun performFullDetection(): PermissionCapabilities {
        // Detect root capabilities
        val rootDetection = detectRootCapabilities()
        
        // Detect Shizuku capabilities
        val shizukuDetection = detectShizukuCapabilities()
        
        // Detect ADB capabilities
        val adbDetection = detectAdbCapabilities()
        
        // Detect storage capabilities based on API level
        val storageCapabilities = detectStorageCapabilities()
        
        // Detect service capabilities
        val serviceCapabilities = detectServiceCapabilities()

        // Combine all detections
        return PermissionCapabilities(
            // Core backup capabilities
            canBackupApk = rootDetection.canExecuteSuCommands || shizukuDetection.shizukuPermissionGranted,
            canBackupData = rootDetection.canExecuteSuCommands || shizukuDetection.shizukuPermissionGranted,
            canDoIncremental = rootDetection.canExecuteSuCommands,
            canRestoreSelinux = rootDetection.canExecuteSuCommands,
            
            // Permission modes
            hasRoot = rootDetection.hasRoot,
            hasShizuku = shizukuDetection.hasShizuku,
            hasAdb = adbDetection.adbWirelessEnabled || adbDetection.adbUsbEnabled,
            hasSaf = true,
            
            // Root-specific
            rootType = rootDetection.rootType,
            hasBusybox = rootDetection.hasBusybox,
            hasMagisk = rootDetection.hasMagisk,
            canExecuteSuCommands = rootDetection.canExecuteSuCommands,
            
            // Storage capabilities
            apiLevel = storageCapabilities.apiLevel,
            hasScopedStorage = storageCapabilities.hasScopedStorage,
            canAccessAllFiles = storageCapabilities.canAccessAllFiles,
            hasManageExternalStoragePermission = storageCapabilities.hasManageExternalStoragePermission,
            
            // Service capabilities
            isAccessibilityServiceEnabled = serviceCapabilities.isAccessibilityServiceEnabled,
            canUseBackupTransport = serviceCapabilities.canUseBackupTransport,
            
            // ADB-specific
            adbWirelessEnabled = adbDetection.adbWirelessEnabled,
            adbUsbEnabled = adbDetection.adbUsbEnabled,
            
            // Shizuku-specific
            shizukuVersion = shizukuDetection.shizukuVersion,
            shizukuPermissionGranted = shizukuDetection.shizukuPermissionGranted
        )
    }

    /**
     * Root detection delegated to root-core RootDetector (production-tested from ObsidianBox v31)
     */
    private suspend fun detectRootCapabilities(): RootDetection = withContext(Dispatchers.IO) {
        try {
            logger.i(TAG, "Calling rootDetector.getOrRefreshStatus()...")
            val status = rootDetector.getOrRefreshStatus()
            logger.i(TAG, "Root status: suAvailable=${status.suAvailable}, rootGranted=${status.rootGranted}, magisk=${status.magiskDetected}")
            
            val rootType = when {
                status.magiskDetected -> RootType.MAGISK
                status.kernelSuDetected -> RootType.OTHER_SU
                status.aPatchDetected -> RootType.OTHER_SU
                status.suAvailable -> RootType.OTHER_SU
                else -> RootType.NONE
            }

            RootDetection(
                hasRoot = status.suAvailable,
                rootType = rootType,
                hasBusybox = checkBusyboxExists(),
                hasMagisk = status.magiskDetected,
                canExecuteSuCommands = status.rootGranted
            )
        } catch (e: Exception) {
            logger.e(TAG, "Error during root detection", e)
            logger.e(TAG, "Error during root detection", e)
            RootDetection(
                hasRoot = false,
                rootType = RootType.NONE,
                hasBusybox = false,
                hasMagisk = false,
                canExecuteSuCommands = false
            )
        }
    }

    private fun checkBusyboxExists(): Boolean {
        val busyboxPaths = listOf(
            "/system/xbin/busybox",
            "/system/bin/busybox",
            "/data/local/xbin/busybox",
            "/data/adb/magisk/busybox"
        )
        return busyboxPaths.any { path ->
            try { File(path).exists() } catch (e: Exception) { false }
        }
    }

    /**
     * Detect Shizuku service availability and permissions
     */
    private suspend fun detectShizukuCapabilities(): ShizukuDetection = withContext(Dispatchers.IO) {
        var hasShizuku = false
        var shizukuVersion = 0
        var shizukuPermissionGranted = false

        try {
            // Check if Shizuku binder is available
            if (Shizuku.pingBinder()) {
                hasShizuku = true
                
                // Get Shizuku version
                try {
                    shizukuVersion = Shizuku.getVersion()
                } catch (e: Exception) {
                    logger.w(TAG, "Failed to get Shizuku version: ${e.message}")
                }
                
                // Check if permission is granted
                try {
                    shizukuPermissionGranted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                } catch (e: Exception) {
                    logger.w(TAG, "Failed to check Shizuku permission: ${e.message}")
                }
            }
        } catch (e: Exception) {
            logger.e(TAG, "Error during Shizuku detection", e)
        }

        ShizukuDetection(
            hasShizuku = hasShizuku,
            shizukuVersion = shizukuVersion,
            shizukuPermissionGranted = shizukuPermissionGranted
        )
    }

    /**
     * Detect ADB availability (both wireless and USB)
     */
    private suspend fun detectAdbCapabilities(): AdbDetection = withContext(Dispatchers.IO) {
        var adbWirelessEnabled = false
        var adbUsbEnabled = false

        try {
            // Method 1: Check if ADB is enabled via Settings.Global
            val adbEnabled = try {
                Settings.Global.getInt(
                    context.contentResolver,
                    Settings.Global.ADB_ENABLED,
                    0
                ) == 1
            } catch (e: Exception) {
                false
            }

            if (adbEnabled) {
                adbUsbEnabled = true
            }

            // Method 2: Check for wireless ADB (Android 11+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    // Check if wireless debugging is enabled
                    val wirelessEnabled = Settings.Global.getInt(
                        context.contentResolver,
                        "adb_wifi_enabled",
                        0
                    ) == 1
                    
                    if (wirelessEnabled) {
                        adbWirelessEnabled = true
                    }
                } catch (e: Exception) {
                    logger.w(TAG, "Failed to check wireless ADB: ${e.message}")
                }
            }

            // Method 3: Check for ADB authorization files (requires root)
            // This is a fallback that won't work without root
            val adbKeysFile = File("/data/misc/adb/adb_keys")
            if (adbKeysFile.exists()) {
                adbUsbEnabled = true
            }

        } catch (e: Exception) {
            logger.e(TAG, "Error during ADB detection", e)
        }

        AdbDetection(
            adbWirelessEnabled = adbWirelessEnabled,
            adbUsbEnabled = adbUsbEnabled
        )
    }

    /**
     * Detect storage capabilities based on Android API level
     */
    private fun detectStorageCapabilities(): StorageDetection {
        val apiLevel = Build.VERSION.SDK_INT
        val hasScopedStorage = apiLevel >= Build.VERSION_CODES.Q // Android 10+
        
        // Check for MANAGE_EXTERNAL_STORAGE permission (Android 11+)
        val hasManageExternalStorage = if (apiLevel >= Build.VERSION_CODES.R) {
            try {
                context.checkSelfPermission(android.Manifest.permission.MANAGE_EXTERNAL_STORAGE) == 
                    PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }

        // For pre-Android 10, check legacy storage permissions
        val canAccessAllFiles = if (apiLevel < Build.VERSION_CODES.Q) {
            context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == 
                PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == 
                PackageManager.PERMISSION_GRANTED
        } else {
            hasManageExternalStorage
        }

        return StorageDetection(
            apiLevel = apiLevel,
            hasScopedStorage = hasScopedStorage,
            canAccessAllFiles = canAccessAllFiles,
            hasManageExternalStoragePermission = hasManageExternalStorage
        )
    }

    /**
     * Detect service-related capabilities
     */
    private fun detectServiceCapabilities(): ServiceDetection {
        var isAccessibilityServiceEnabled = false
        var canUseBackupTransport = false

        try {
            // Check if any accessibility service for this app is enabled
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            
            if (enabledServices != null) {
                isAccessibilityServiceEnabled = enabledServices.contains(context.packageName)
            }
        } catch (e: Exception) {
            logger.w(TAG, "Failed to check accessibility service: ${e.message}")
        }

        try {
            // Check if backup is allowed for this app
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                0
            )
            canUseBackupTransport = (appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
        } catch (e: Exception) {
            logger.w(TAG, "Failed to check backup transport: ${e.message}")
        }

        return ServiceDetection(
            isAccessibilityServiceEnabled = isAccessibilityServiceEnabled,
            canUseBackupTransport = canUseBackupTransport
        )
    }

    /**
     * Invalidate cache to force fresh detection
     */
    fun invalidateCache() {
        cachedCapabilities = null
        lastDetectionTime = 0
    }

    /**
     * Force a specific permission mode (user override).
     * Pass null to revert to auto-detection.
     */
    fun forceMode(mode: PermissionMode?) {
        if (mode == null) {
            prefs.edit().remove(KEY_FORCED_MODE).apply()
        } else {
            prefs.edit().putString(KEY_FORCED_MODE, mode.name).apply()
            _currentMode.value = mode
            logger.i(TAG, "Permission mode forced to: ${mode.displayName}")
        }
    }

    /** Returns the user-forced mode, or null if using auto-detection. */
    fun getForcedMode(): PermissionMode? {
        val stored = prefs.getString(KEY_FORCED_MODE, null) ?: return null
        return runCatching { PermissionMode.valueOf(stored) }.getOrNull()
    }

    /**
     * Start monitoring for real-time permission changes
     */
    suspend fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        
        withContext(Dispatchers.IO) {
            while (isMonitoring) {
                try {
                    // Re-detect capabilities
                    val caps = detectCapabilities()
                    
                    // Update mode if needed
                    val newMode = determineBestMode(caps)
                    if (newMode != _currentMode.value) {
                        _currentMode.value = newMode
                        logger.i(TAG, "Permission mode changed to: $newMode")
                    }
                    
                    // Wait before next check (5 seconds)
                    kotlinx.coroutines.delay(5000)
                } catch (e: Exception) {
                    logger.e(TAG, "Error in monitoring loop", e)
                    kotlinx.coroutines.delay(10000) // Wait longer on error
                }
            }
        }
    }
    
    /**
     * Stop monitoring for permission changes
     */
    fun stopMonitoring() {
        isMonitoring = false
    }
    
    /**
     * Determine best permission mode based on capabilities
     */
    private fun determineBestMode(caps: PermissionCapabilities): PermissionMode {
        return when {
            caps.canExecuteSuCommands && caps.hasRoot -> PermissionMode.ROOT
            caps.shizukuPermissionGranted && caps.hasShizuku -> PermissionMode.SHIZUKU
            caps.hasAdb -> PermissionMode.ADB
            else -> PermissionMode.SAF
        }
    }

    suspend fun detectBestMode() {
        // First detect all capabilities
        val caps = detectCapabilities()
        
        // Determine best mode based on capabilities
        val detectedMode = when {
            caps.canExecuteSuCommands -> PermissionMode.ROOT
            caps.shizukuPermissionGranted -> PermissionMode.SHIZUKU
            caps.adbWirelessEnabled || caps.adbUsbEnabled -> PermissionMode.ADB
            else -> PermissionMode.SAF
        }
        
        _currentMode.value = detectedMode
        logger.i(TAG, "Detected best permission mode: $detectedMode")
    }

    private fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su -c echo test")
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    private fun isAdbAvailable(): Boolean {
        return try {
            // Check if ADB is enabled via Settings.Global
            val adbEnabled = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            ) == 1
            
            adbEnabled
        } catch (e: Exception) {
            false
        }
    }

    fun capabilitiesFor(mode: PermissionMode): PermissionCapabilities {
        return when (mode) {
            PermissionMode.ROOT -> PermissionCapabilities(
                canBackupApk = true,
                canBackupData = true,
                canDoIncremental = true,
                canRestoreSelinux = true
            )
            PermissionMode.SHIZUKU -> PermissionCapabilities(
                canBackupApk = true,
                canBackupData = true,
                canDoIncremental = false,
                canRestoreSelinux = false
            )
            PermissionMode.ADB -> PermissionCapabilities(
                canBackupApk = true,
                canBackupData = false,
                canDoIncremental = false,
                canRestoreSelinux = false
            )
            PermissionMode.SAF -> PermissionCapabilities(
                canBackupApk = false,
                canBackupData = false,
                canDoIncremental = false,
                canRestoreSelinux = false
            )
        }
    }

    // Internal data classes for detection results
    private data class RootDetection(
        val hasRoot: Boolean,
        val rootType: RootType,
        val hasBusybox: Boolean,
        val hasMagisk: Boolean,
        val canExecuteSuCommands: Boolean
    )

    private data class ShizukuDetection(
        val hasShizuku: Boolean,
        val shizukuVersion: Int,
        val shizukuPermissionGranted: Boolean
    )

    private data class AdbDetection(
        val adbWirelessEnabled: Boolean,
        val adbUsbEnabled: Boolean
    )

    private data class StorageDetection(
        val apiLevel: Int,
        val hasScopedStorage: Boolean,
        val canAccessAllFiles: Boolean,
        val hasManageExternalStoragePermission: Boolean
    )

    private data class ServiceDetection(
        val isAccessibilityServiceEnabled: Boolean,
        val canUseBackupTransport: Boolean
    )
}
