package com.obsidianbackup.domain.root

import com.obsidianbackup.rootcore.magisk.MagiskDetector
import com.obsidianbackup.rootcore.magisk.MagiskModule
import com.obsidianbackup.rootcore.shell.ShellExecutor
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Magisk module management — list, enable, disable, install, remove modules.
 *
 * Ported from ObsidianBox's MagiskRepository patterns.
 * Reads module props from /data/adb/modules/<id>/module.prop.
 */
@Singleton
class MagiskModuleManager @Inject constructor(
    private val shellExecutor: ShellExecutor,
    private val magiskDetector: MagiskDetector
) {
    companion object {
        private const val TAG = "[MagiskModule]"
        private const val MODULES_DIR = "/data/adb/modules"
    }

    data class ModuleDetail(
        val id: String,
        val name: String,
        val version: String,
        val versionCode: Int,
        val author: String,
        val description: String,
        val enabled: Boolean,
        val remove: Boolean = false,
        val updateAvailable: Boolean = false
    )

    /**
     * List all installed Magisk modules with full details.
     */
    suspend fun listModules(): List<ModuleDetail> {
        val result = shellExecutor.executeRoot("ls -1 $MODULES_DIR 2>/dev/null")
        if (!result.success) return emptyList()

        return result.stdout.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { moduleId -> readModuleProps(moduleId) }
    }

    /**
     * Enable a disabled module.
     */
    suspend fun enableModule(moduleId: String): Boolean {
        val disableFile = "$MODULES_DIR/$moduleId/disable"
        val result = shellExecutor.executeRoot("rm -f $disableFile")
        Timber.d("$TAG Enabled module: $moduleId")
        return result.success
    }

    /**
     * Disable a module (creates 'disable' file).
     */
    suspend fun disableModule(moduleId: String): Boolean {
        val disableFile = "$MODULES_DIR/$moduleId/disable"
        val result = shellExecutor.executeRoot("touch $disableFile")
        Timber.d("$TAG Disabled module: $moduleId")
        return result.success
    }

    /**
     * Mark a module for removal on next reboot (creates 'remove' file).
     */
    suspend fun markForRemoval(moduleId: String): Boolean {
        val removeFile = "$MODULES_DIR/$moduleId/remove"
        val result = shellExecutor.executeRoot("touch $removeFile")
        Timber.d("$TAG Marked module for removal: $moduleId")
        return result.success
    }

    /**
     * Cancel pending removal of a module.
     */
    suspend fun cancelRemoval(moduleId: String): Boolean {
        val result = shellExecutor.executeRoot("rm -f $MODULES_DIR/$moduleId/remove")
        return result.success
    }

    /**
     * Install a Magisk module from a zip file.
     * Uses `magisk --install-module` (Magisk v24+) or manual extraction.
     */
    suspend fun installModule(zipPath: String): Result<String> = runCatching {
        val version = magiskDetector.getMagiskVersionCode()
        if (version != null && version >= 24000) {
            // Modern Magisk install
            val result = shellExecutor.executeRoot("magisk --install-module $zipPath")
            if (result.success) {
                Timber.d("$TAG Installed module from $zipPath")
                return@runCatching "Module installed (reboot required)"
            }
        }

        // Fallback: manual extraction
        val tmpDir = "/data/local/tmp/magisk_module_${System.currentTimeMillis()}"
        shellExecutor.executeRoot("mkdir -p $tmpDir && unzip -o $zipPath -d $tmpDir")

        // Read module.prop for ID
        val propResult = shellExecutor.executeRoot("cat $tmpDir/module.prop")
        val moduleId = propResult.stdout.lines()
            .firstOrNull { it.startsWith("id=") }
            ?.substringAfter("id=")
            ?.trim()
            ?: throw RuntimeException("Invalid module: no id in module.prop")

        // Move to modules directory
        val targetDir = "$MODULES_DIR/$moduleId"
        shellExecutor.executeRoot("rm -rf $targetDir && mv $tmpDir $targetDir")
        shellExecutor.executeRoot("chmod -R 755 $targetDir")

        Timber.d("$TAG Manually installed module: $moduleId")
        "Module '$moduleId' installed (reboot required)"
    }

    /**
     * Backup a module's directory for later restoration.
     */
    suspend fun backupModule(moduleId: String, outputDir: String): Boolean {
        val result = shellExecutor.executeRoot(
            "tar -cf $outputDir/${moduleId}.tar -C $MODULES_DIR $moduleId"
        )
        return result.success
    }

    /**
     * Restore a module from backup.
     */
    suspend fun restoreModule(tarPath: String): Boolean {
        val result = shellExecutor.executeRoot("tar -xf $tarPath -C $MODULES_DIR")
        return result.success
    }

    private suspend fun readModuleProps(moduleId: String): ModuleDetail? {
        val propResult = shellExecutor.executeRoot("cat $MODULES_DIR/$moduleId/module.prop")
        if (!propResult.success) return null

        val props = propResult.stdout.lines()
            .filter { it.contains("=") }
            .associate { line ->
                val key = line.substringBefore("=").trim()
                val value = line.substringAfter("=").trim()
                key to value
            }

        val enabled = shellExecutor.executeRoot("test -f $MODULES_DIR/$moduleId/disable && echo DISABLED")
        val remove = shellExecutor.executeRoot("test -f $MODULES_DIR/$moduleId/remove && echo REMOVE")

        return ModuleDetail(
            id = props["id"] ?: moduleId,
            name = props["name"] ?: moduleId,
            version = props["version"] ?: "",
            versionCode = props["versionCode"]?.toIntOrNull() ?: 0,
            author = props["author"] ?: "",
            description = props["description"] ?: "",
            enabled = !enabled.stdout.contains("DISABLED"),
            remove = remove.stdout.contains("REMOVE")
        )
    }
}
