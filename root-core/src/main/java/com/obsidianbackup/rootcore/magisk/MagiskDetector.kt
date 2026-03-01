package com.obsidianbackup.rootcore.magisk

import com.obsidianbackup.rootcore.shell.RootCommandResult
import com.obsidianbackup.rootcore.shell.ShellExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects and manages Magisk modules via filesystem introspection.
 *
 * Patterns ported from ObsidianBox v31 MagiskRepository:
 * - Module enumeration via /data/adb/modules/
 * - Property parsing from module.prop
 * - Enable/disable via filesystem markers
 */
@Singleton
class MagiskDetector @Inject constructor(
    private val shellExecutor: ShellExecutor
) {
    companion object {
        private const val MODULES_PATH = "/data/adb/modules"
        private const val MAGISK_DB_PATH = "/data/adb/magisk.db"
    }

    /** Get Magisk version string, or null if not installed. */
    suspend fun getMagiskVersion(): String? = withContext(Dispatchers.IO) {
        try {
            val result = shellExecutor.executeRootUnsafe("magisk -v 2>/dev/null")
            if (result.isSuccess && result.stdout.isNotBlank()) {
                result.stdout.trim()
            } else null
        } catch (e: Exception) {
            Timber.d("Failed to get Magisk version: ${e.message}")
            null
        }
    }

    /** Get Magisk version code, or null if not installed. */
    suspend fun getMagiskVersionCode(): Int? = withContext(Dispatchers.IO) {
        try {
            val result = shellExecutor.executeRootUnsafe("magisk -V 2>/dev/null")
            if (result.isSuccess) result.stdout.trim().toIntOrNull() else null
        } catch (e: Exception) {
            null
        }
    }

    /** List all installed Magisk modules. */
    suspend fun listModules(): RootCommandResult<List<MagiskModule>> = withContext(Dispatchers.IO) {
        try {
            val result = shellExecutor.executeRootUnsafe("ls $MODULES_PATH 2>/dev/null")
            if (!result.isSuccess) {
                return@withContext RootCommandResult.Success(emptyList())
            }

            val modules = result.stdout.lines()
                .filter { it.isNotBlank() }
                .mapNotNull { moduleId -> getModuleInfo(moduleId) }

            RootCommandResult.Success(modules)
        } catch (e: Exception) {
            Timber.e(e, "Failed to list Magisk modules")
            RootCommandResult.Error(e.message ?: "Failed to list modules")
        }
    }

    /** Get detailed info for a single module by ID. */
    suspend fun getModuleInfo(moduleId: String): MagiskModule? = withContext(Dispatchers.IO) {
        try {
            val propResult = shellExecutor.executeRootUnsafe(
                "cat $MODULES_PATH/$moduleId/module.prop 2>/dev/null"
            )
            if (!propResult.isSuccess) return@withContext null

            val props = propResult.stdout.lines()
                .filter { it.contains("=") }
                .associate {
                    val (key, value) = it.split("=", limit = 2)
                    key.trim() to value.trim()
                }

            val modulePath = "$MODULES_PATH/$moduleId"

            // Check state files
            val disableResult = shellExecutor.executeRootUnsafe(
                "test -f $modulePath/disable && echo yes || echo no"
            )
            val isEnabled = disableResult.stdout.trim() != "yes"

            val removeResult = shellExecutor.executeRootUnsafe(
                "test -f $modulePath/remove && echo yes || echo no"
            )
            val isRemoved = removeResult.stdout.trim() == "yes"

            MagiskModule(
                id = props["id"] ?: moduleId,
                name = props["name"] ?: moduleId,
                version = props["version"] ?: "",
                author = props["author"] ?: "",
                description = props["description"] ?: "",
                isEnabled = isEnabled,
                isRemoved = isRemoved,
                path = modulePath
            )
        } catch (e: Exception) {
            Timber.d("Failed to read module $moduleId: ${e.message}")
            null
        }
    }

    /** Enable a Magisk module (removes 'disable' file). */
    suspend fun enableModule(moduleId: String): RootCommandResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val result = shellExecutor.executeRootUnsafe(
                "rm -f $MODULES_PATH/$moduleId/disable"
            )
            if (result.isSuccess) {
                RootCommandResult.Success(true)
            } else {
                RootCommandResult.CommandFailed(result.exitCode, result.stderr)
            }
        } catch (e: Exception) {
            RootCommandResult.Error(e.message ?: "Failed to enable module")
        }
    }

    /** Disable a Magisk module (creates 'disable' file). */
    suspend fun disableModule(moduleId: String): RootCommandResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val result = shellExecutor.executeRootUnsafe(
                "touch $MODULES_PATH/$moduleId/disable"
            )
            if (result.isSuccess) {
                RootCommandResult.Success(true)
            } else {
                RootCommandResult.CommandFailed(result.exitCode, result.stderr)
            }
        } catch (e: Exception) {
            RootCommandResult.Error(e.message ?: "Failed to disable module")
        }
    }

    /** Mark a Magisk module for removal on next reboot. */
    suspend fun removeModule(moduleId: String): RootCommandResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val result = shellExecutor.executeRootUnsafe(
                "touch $MODULES_PATH/$moduleId/remove"
            )
            if (result.isSuccess) {
                RootCommandResult.Success(true)
            } else {
                RootCommandResult.CommandFailed(result.exitCode, result.stderr)
            }
        } catch (e: Exception) {
            RootCommandResult.Error(e.message ?: "Failed to mark module for removal")
        }
    }
}
