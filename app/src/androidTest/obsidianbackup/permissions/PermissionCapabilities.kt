package com.titanbackup.permissions

data class PermissionCapabilities(
    val canBackupApk: Boolean,
    val canBackupData: Boolean,
    val canBackupObb: Boolean,
    val canBackupExternalData: Boolean,
    val canDoIncremental: Boolean,
    val canRestoreSelinux: Boolean,
    val canPreservePermissions: Boolean,
    val canAccessSystemApps: Boolean,
    val canFreezeApps: Boolean,
    val canModifySystem: Boolean,
    val maxConcurrentOperations: Int
) {
    companion object {
        fun forMode(mode: PermissionMode): PermissionCapabilities = when (mode) {
            PermissionMode.ROOT -> PermissionCapabilities(
                canBackupApk = true,
                canBackupData = true,
                canBackupObb = true,
                canBackupExternalData = true,
                canDoIncremental = true,
                canRestoreSelinux = true,
                canPreservePermissions = true,
                canAccessSystemApps = true,
                canFreezeApps = true,
                canModifySystem = true,
                maxConcurrentOperations = 4
            )
            PermissionMode.SHIZUKU -> PermissionCapabilities(
                canBackupApk = true,
                canBackupData = true,
                canBackupObb = true,
                canBackupExternalData = true,
                canDoIncremental = true,
                canRestoreSelinux = false,
                canPreservePermissions = true,
                canAccessSystemApps = true,
                canFreezeApps = false,
                canModifySystem = false,
                maxConcurrentOperations = 3
            )
            PermissionMode.ADB -> PermissionCapabilities(
                canBackupApk = true,
                canBackupData = true,
                canBackupObb = false,
                canBackupExternalData = false,
                canDoIncremental = false,
                canRestoreSelinux = false,
                canPreservePermissions = false,
                canAccessSystemApps = false,
                canFreezeApps = false,
                canModifySystem = false,
                maxConcurrentOperations = 2
            )
            PermissionMode.SAF -> PermissionCapabilities(
                canBackupApk = false,
                canBackupData = false,
                canBackupObb = false,
                canBackupExternalData = true,
                canDoIncremental = false,
                canRestoreSelinux = false,
                canPreservePermissions = false,
                canAccessSystemApps = false,
                canFreezeApps = false,
                canModifySystem = false,
                maxConcurrentOperations = 1
            )
        }
    }
}
