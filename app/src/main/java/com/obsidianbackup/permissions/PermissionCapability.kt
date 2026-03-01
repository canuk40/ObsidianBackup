// permissions/PermissionCapability.kt
package com.obsidianbackup.permissions

import android.content.Context
import com.obsidianbackup.engine.ShellExecutor
import com.obsidianbackup.engine.ShellResult
import com.obsidianbackup.model.PermissionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface PermissionCapability {
    val name: String
    suspend fun isAvailable(context: Context): Boolean
    fun getPriority(): Int
}

data class PermissionProfile(
    val capabilities: Set<PermissionCapability>,
    val displayName: String
) {
    suspend fun canBackupApk(context: Context): Boolean =
        capabilities.any { it is ApkAccessCapability && it.isAvailable(context) }

    suspend fun canBackupData(context: Context): Boolean =
        capabilities.any { it is DataAccessCapability && it.isAvailable(context) }
}

// Concrete capabilities
class RootCapability : PermissionCapability {
    override val name = "root"
    override fun getPriority() = 100
    override suspend fun isAvailable(context: Context): Boolean {
        return checkRootAccess()
    }
}

class ShizukuCapability : PermissionCapability {
    override val name = "shizuku"
    override fun getPriority() = 80
    override suspend fun isAvailable(context: Context): Boolean {
        return ShizukuService.isAvailable(context)
    }
}

class ApkAccessCapability : PermissionCapability {
    override val name = "apk_access"
    override fun getPriority() = 50
    override suspend fun isAvailable(context: Context): Boolean {
        return try {
            val executor = ShellExecutor(PermissionMode.ROOT)
            val result = executor.execute("pm list packages -f | head -n 1")
            result is ShellResult.Success
        } catch (e: Exception) {
            false
        }
    }
}

class DataAccessCapability : PermissionCapability {
    override val name = "data_access"
    override fun getPriority() = 50
    override suspend fun isAvailable(context: Context): Boolean {
        return try {
            val executor = ShellExecutor(PermissionMode.ROOT)
            val result = executor.execute("ls /data/data | head -n 1")
            result is ShellResult.Success
        } catch (e: Exception) {
            false
        }
    }
}

// Helper function to check root access
private suspend fun checkRootAccess(): Boolean = withContext(Dispatchers.IO) {
    try {
        val process = Runtime.getRuntime().exec("su -c id")
        val exitCode = process.waitFor()
        exitCode == 0
    } catch (e: Exception) {
        false
    }
}

// Stub for Shizuku service
object ShizukuService {
    suspend fun isAvailable(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            // Stub implementation - check if Shizuku package is installed
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}
