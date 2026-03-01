package com.titanbackup.permissions

sealed class PermissionMode(val displayName: String, val priority: Int) {
    object ROOT : PermissionMode("Root", 4)
    object SHIZUKU : PermissionMode("Shizuku", 3)
    object ADB : PermissionMode("ADB", 2)
    object SAF : PermissionMode("Storage Access", 1)

    companion object {
        fun fromPriority(priority: Int): PermissionMode = when (priority) {
            4 -> ROOT
            3 -> SHIZUKU
            2 -> ADB
            else -> SAF
        }
    }
}
