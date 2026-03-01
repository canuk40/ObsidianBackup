package com.obsidianbackup.model

data class PermissionCapabilities(
    // Core backup capabilities
    val canBackupApk: Boolean = false,
    val canBackupData: Boolean = false,
    val canBackupObb: Boolean = false,
    val canBackupExternalData: Boolean = false,
    val canDoIncremental: Boolean = false,
    val canRestoreSelinux: Boolean = false,
    
    // Permission modes detected
    val hasRoot: Boolean = false,
    val hasShizuku: Boolean = false,
    val hasAdb: Boolean = false,
    val hasSaf: Boolean = true, // SAF always available
    
    // Root-specific capabilities
    val rootType: RootType = RootType.NONE,
    val hasBusybox: Boolean = false,
    val hasMagisk: Boolean = false,
    val canExecuteSuCommands: Boolean = false,
    
    // Storage capabilities based on API level
    val apiLevel: Int = 0,
    val hasScopedStorage: Boolean = false,
    val canAccessAllFiles: Boolean = false,
    val hasManageExternalStoragePermission: Boolean = false,
    
    // Service capabilities
    val isAccessibilityServiceEnabled: Boolean = false,
    val canUseBackupTransport: Boolean = false,
    
    // ADB-specific
    val adbWirelessEnabled: Boolean = false,
    val adbUsbEnabled: Boolean = false,
    
    // Shizuku-specific
    val shizukuVersion: Int = 0,
    val shizukuPermissionGranted: Boolean = false
) {
    /**
     * Get list of available permission modes
     */
    fun getAvailableModes(): List<PermissionMode> = buildList {
        if (hasRoot && canExecuteSuCommands) add(PermissionMode.ROOT)
        if (hasShizuku && shizukuPermissionGranted) add(PermissionMode.SHIZUKU)
        if (hasAdb) add(PermissionMode.ADB)
        add(PermissionMode.SAF) // Always available
    }
    
    /**
     * Get best available mode
     */
    fun getBestMode(): PermissionMode = when {
        canExecuteSuCommands && hasRoot -> PermissionMode.ROOT
        shizukuPermissionGranted && hasShizuku -> PermissionMode.SHIZUKU
        hasAdb -> PermissionMode.ADB
        else -> PermissionMode.SAF
    }
    
    /**
     * Check if a specific mode is available
     */
    fun isModeAvailable(mode: PermissionMode): Boolean = when (mode) {
        PermissionMode.ROOT -> hasRoot && canExecuteSuCommands
        PermissionMode.SHIZUKU -> hasShizuku && shizukuPermissionGranted
        PermissionMode.ADB -> hasAdb
        PermissionMode.SAF -> true
    }
    
    /**
     * Get capabilities for a specific backup engine
     */
    fun forEngine(engineType: String): EngineCapabilities = when (engineType.lowercase()) {
        "root", "rsync" -> EngineCapabilities(
            canBackupApk = canBackupApk && hasRoot,
            canBackupData = canBackupData && hasRoot,
            canBackupObb = canBackupObb && hasRoot,
            canDoIncremental = canDoIncremental && hasRoot,
            canRestoreSelinux = canRestoreSelinux && hasRoot
        )
        "shizuku" -> EngineCapabilities(
            canBackupApk = canBackupApk && hasShizuku,
            canBackupData = canBackupData && hasShizuku,
            canBackupObb = false,
            canDoIncremental = false,
            canRestoreSelinux = false
        )
        "adb" -> EngineCapabilities(
            canBackupApk = canBackupApk && hasAdb,
            canBackupData = false,
            canBackupObb = false,
            canDoIncremental = false,
            canRestoreSelinux = false
        )
        "saf" -> EngineCapabilities(
            canBackupApk = false,
            canBackupData = false,
            canBackupObb = false,
            canDoIncremental = false,
            canRestoreSelinux = false
        )
        else -> EngineCapabilities()
    }
    
    /**
     * Get human-readable status summary
     */
    fun getSummary(): String = buildString {
        appendLine("Permission Capabilities:")
        appendLine("  Available Modes: ${getAvailableModes().joinToString { it.displayName }}")
        appendLine("  Best Mode: ${getBestMode().displayName}")
        appendLine()
        appendLine("Backup Capabilities:")
        appendLine("  - APK Backup: ${if (canBackupApk) "✓" else "✗"}")
        appendLine("  - Data Backup: ${if (canBackupData) "✓" else "✗"}")
        appendLine("  - OBB Backup: ${if (canBackupObb) "✓" else "✗"}")
        appendLine("  - Incremental: ${if (canDoIncremental) "✓" else "✗"}")
        appendLine("  - SELinux Restore: ${if (canRestoreSelinux) "✓" else "✗"}")
        appendLine()
        appendLine("System Info:")
        appendLine("  - API Level: $apiLevel")
        appendLine("  - Root Type: ${rootType.name}")
        if (hasShizuku) appendLine("  - Shizuku Version: $shizukuVersion")
        if (hasBusybox) appendLine("  - Busybox: Available")
        if (hasMagisk) appendLine("  - Magisk: Detected")
    }
}

/**
 * Capabilities specific to a backup engine
 */
data class EngineCapabilities(
    val canBackupApk: Boolean = false,
    val canBackupData: Boolean = false,
    val canBackupObb: Boolean = false,
    val canDoIncremental: Boolean = false,
    val canRestoreSelinux: Boolean = false
)

enum class RootType {
    NONE,
    MAGISK,
    SUPERSU,
    KINGROOT,
    OTHER_SU
}
