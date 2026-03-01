package com.obsidianbackup.rootcore.selinux

import com.obsidianbackup.rootcore.shell.ShellExecutor
import com.obsidianbackup.rootcore.shell.ShellResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SELinux-aware restore helper.
 * After every data restore, SELinux contexts MUST be restored with `restorecon -R`
 * otherwise apps will crash with permission denied on Android 4.3+.
 *
 * Also handles storing/restoring original contexts for accurate restoration.
 */
@Singleton
class SELinuxRestoreHelper @Inject constructor(
    private val shellExecutor: ShellExecutor,
    private val seLinuxHelper: SELinuxHelper
) {
    companion object {
        private const val TAG = "SELinuxRestore"
    }

    /**
     * Restore SELinux contexts after a data restore operation.
     * This is CRITICAL — without it, apps will fail to read their own data.
     *
     * @param packageDataPath e.g. "/data/data/com.example.app"
     * @return true if restorecon succeeded
     */
    suspend fun restoreContextsAfterRestore(packageDataPath: String): Boolean {
        return seLinuxHelper.restoreContext(packageDataPath)
    }

    /**
     * Full post-restore fixup: chown + chmod + restorecon.
     * Call this after extracting data to /data/data/<pkg>/.
     *
     * @param packageName the package name
     * @param uid the UID from PackageManager
     * @param dataPath path to the app data directory
     */
    suspend fun fixPermissionsAfterRestore(
        packageName: String,
        uid: Int,
        dataPath: String = "/data/data/$packageName"
    ): PostRestoreResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<ShellResult>()

        // 1. Set ownership
        val chownResult = shellExecutor.executeRoot("chown -R $uid:$uid $dataPath")
        results.add(chownResult)
        if (!chownResult.success) {
            Timber.w("$TAG chown failed for $packageName: ${chownResult.stderr}")
        }

        // 2. Set permissions (771 for directories, 660 for files — standard Android)
        val chmodDirResult = shellExecutor.executeRoot(
            "find $dataPath -type d -exec chmod 771 {} +"
        )
        results.add(chmodDirResult)

        val chmodFileResult = shellExecutor.executeRoot(
            "find $dataPath -type f -exec chmod 660 {} +"
        )
        results.add(chmodFileResult)

        // 3. Restore SELinux contexts (CRITICAL)
        val restoreconOk = restoreContextsAfterRestore(dataPath)
        if (!restoreconOk) {
            Timber.e("$TAG restorecon failed for $packageName")
        }

        val succeeded = results.count { it.success } + (if (restoreconOk) 1 else 0)
        val total = results.size + 1
        Timber.i("$TAG Post-restore fixup done for $packageName ($succeeded/$total succeeded)")
        
        PostRestoreResult(
            shellResults = results,
            restoreconSuccess = restoreconOk,
            allSucceeded = results.all { it.success } && restoreconOk
        )
    }

    /**
     * Capture SELinux context of a path before backup.
     * Returns the context string (e.g., "u:object_r:app_data_file:s0:c512,c768")
     */
    suspend fun captureContext(path: String): String? {
        return seLinuxHelper.getContext(path)
    }

    /**
     * Set a specific SELinux context on a path.
     * Use when you need to restore a previously captured context.
     */
    suspend fun setContext(path: String, context: String): Boolean {
        return seLinuxHelper.setContext(path, context)
    }

    data class PostRestoreResult(
        val shellResults: List<ShellResult>,
        val restoreconSuccess: Boolean,
        val allSucceeded: Boolean
    )
}
