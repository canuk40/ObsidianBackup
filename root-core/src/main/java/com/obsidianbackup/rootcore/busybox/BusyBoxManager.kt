package com.obsidianbackup.rootcore.busybox

import android.content.Context
import android.os.Build
import com.obsidianbackup.rootcore.shell.ShellResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages BusyBox binary extraction and discovery.
 * Strategy mirrors Magisk and Titanium — all serious root backup apps
 * bundle their own BusyBox to guarantee consistent Unix utilities.
 *
 * Priority: bundled → Magisk's → system → error
 */
@Singleton
class BusyBoxManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "BusyBoxManager"
        private const val BUSYBOX_FILENAME = "busybox"

        // Search paths in priority order (after bundled)
        private val SYSTEM_BUSYBOX_PATHS = listOf(
            "/data/adb/magisk/busybox",     // Magisk's bundled BusyBox (best)
            "/data/adb/ksu/bin/busybox",    // KernelSU BusyBox
            "/system/xbin/busybox",
            "/system/bin/busybox",
            "/sbin/busybox",
            "/vendor/bin/busybox",
            "/data/local/xbin/busybox",
            "/data/local/bin/busybox"
        )

        // Maps Android ABI to bundled asset name
        private fun getAssetName(): String? {
            val abis = Build.SUPPORTED_ABIS
            return when {
                abis.any { it == "arm64-v8a" }   -> "busybox_arm64"
                abis.any { it == "armeabi-v7a" } -> "busybox_arm"
                abis.any { it == "x86_64" }      -> "busybox_x86_64"
                abis.any { it == "x86" }         -> "busybox_x86"
                else -> null
            }
        }
    }

    @Volatile
    private var resolvedBusyBox: String? = null

    /**
     * Get path to a working BusyBox binary.
     * Returns null if no BusyBox is available.
     */
    suspend fun getBusyBoxPath(): String? = withContext(Dispatchers.IO) {
        resolvedBusyBox?.let { return@withContext it }

        // 1. Check bundled binary (extracted from APK native libs)
        val bundled = getBundledBusyBoxPath()
        if (bundled != null && verifyBusyBox(bundled)) {
            Timber.i("$TAG Using bundled BusyBox: $bundled")
            resolvedBusyBox = bundled
            return@withContext bundled
        }

        // 2. Check Magisk and system paths
        for (path in SYSTEM_BUSYBOX_PATHS) {
            try {
                val file = File(path)
                if (file.exists() && file.canExecute() && verifyBusyBox(path)) {
                    Timber.i("$TAG Using system BusyBox: $path")
                    resolvedBusyBox = path
                    return@withContext path
                }
            } catch (e: Exception) {
                // Permission denied or path inaccessible
            }
        }

        // 3. Check via `which busybox`
        try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "busybox"))
            val output = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() == 0 && output.isNotEmpty() && verifyBusyBox(output)) {
                Timber.i("$TAG Using which-discovered BusyBox: $output")
                resolvedBusyBox = output
                return@withContext output
            }
        } catch (e: Exception) {
            // which not available
        }

        Timber.w("$TAG No BusyBox found on device")
        null
    }

    /**
     * Extract bundled BusyBox from APK assets to app-private directory.
     * Binaries are stored as busybox_arm64, busybox_arm, busybox_x86_64, busybox_x86 in assets.
     */
    suspend fun extractBundledBusyBox(): File? = withContext(Dispatchers.IO) {
        try {
            val assetName = getAssetName() ?: run {
                Timber.w("$TAG No bundled busybox for ABI: ${Build.SUPPORTED_ABIS.firstOrNull()}")
                return@withContext null
            }

            val targetDir = File(context.filesDir, "bin")
            targetDir.mkdirs()
            val target = File(targetDir, BUSYBOX_FILENAME)

            // Only extract if not already present with correct size
            val assetSize = context.assets.open(assetName).use { it.available().toLong() }
            if (target.exists() && target.length() == assetSize) {
                target.setExecutable(true, false)
                fixSelinuxContext(target.absolutePath)
                return@withContext target
            }

            context.assets.open(assetName).use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
            target.setExecutable(true, false)
            // Fix SELinux context so the app process can execute the binary directly.
            // Without this, Android 10+ W^X/SELinux blocks execution of app_data_file context.
            fixSelinuxContext(target.absolutePath)
            Timber.i("$TAG Extracted BusyBox ($assetName) to: ${target.absolutePath}")
            target
        } catch (e: Exception) {
            Timber.e(e, "$TAG Failed to extract bundled BusyBox from assets")
            null
        }
    }

    /**
     * Get environment variables for BusyBox standalone mode.
     * ASH_STANDALONE=1 ensures BusyBox ash shell uses its own applets.
     * PATH is prepended with the busybox bin directory so shell commands
     * like `busybox tar` resolve to our bundled binary.
     */
    fun getEnvironment(): Map<String, String> {
        val env = mutableMapOf("ASH_STANDALONE" to "1")
        val bbPath = resolvedBusyBox ?: File(context.filesDir, "bin/$BUSYBOX_FILENAME")
            .takeIf { it.exists() }?.absolutePath
        bbPath?.let { bb ->
            val bbDir = File(bb).parent
            if (bbDir != null) {
                env["BBDIR"] = bbDir
                env["BUSYBOX_PATH"] = bb
            }
        }
        return env
    }

    /**
     * Returns the directory containing the busybox binary (for prepending to PATH).
     * Valid even before getBusyBoxPath() has been called (uses the extraction target).
     */
    fun getBusyBoxBinDir(): String = File(context.filesDir, "bin").absolutePath

    /**
     * Check if BusyBox is available on this device.
     */
    suspend fun isAvailable(): Boolean {
        return getBusyBoxPath() != null
    }

    /**
     * Get the version string of the resolved BusyBox binary.
     */
    suspend fun getVersion(): String? = withContext(Dispatchers.IO) {
        val bbPath = getBusyBoxPath() ?: return@withContext null
        try {
            // Try direct execution; busybox --help outputs version on first line of stderr
            val process = Runtime.getRuntime().exec(arrayOf(bbPath, "--help"))
            val stdoutThread = Thread { runCatching { process.inputStream.readBytes() } }
            stdoutThread.start()
            val firstLine = process.errorStream.bufferedReader().readLine()
            stdoutThread.join(2000)
            process.waitFor()
            process.destroy()
            if (firstLine?.contains("BusyBox", ignoreCase = true) == true) return@withContext firstLine
        } catch (_: Exception) { }
        // Fallback: run via root
        val suPaths = listOf("/system_ext/bin/su", "/system/bin/su", "/system/xbin/su", "su")
        for (su in suPaths) {
            try {
                val proc = Runtime.getRuntime().exec(arrayOf(su, "-c", "$bbPath --help 2>&1"))
                val line = proc.inputStream.bufferedReader().readLine()
                proc.waitFor(); proc.destroy()
                if (line?.contains("BusyBox", ignoreCase = true) == true) return@withContext line
            } catch (_: Exception) { }
        }
        null
    }

    /**
     * List available applets in the resolved BusyBox.
     */
    suspend fun listApplets(): List<String> = withContext(Dispatchers.IO) {
        val bbPath = getBusyBoxPath() ?: return@withContext emptyList()
        try {
            val process = Runtime.getRuntime().exec(arrayOf(bbPath, "--list"))
            val applets = process.inputStream.bufferedReader().readLines()
            process.waitFor(); process.destroy()
            if (applets.isNotEmpty()) return@withContext applets
        } catch (_: Exception) { }
        // Fallback: run via root
        val suPaths = listOf("/system_ext/bin/su", "/system/bin/su", "/system/xbin/su", "su")
        for (su in suPaths) {
            try {
                val proc = Runtime.getRuntime().exec(arrayOf(su, "-c", "$bbPath --list"))
                val applets = proc.inputStream.bufferedReader().readLines()
                proc.waitFor(); proc.destroy()
                if (applets.isNotEmpty()) return@withContext applets
            } catch (_: Exception) { }
        }
        emptyList()
    }

    /**
     * Change the SELinux context of the binary to system_file so the app process can
     * execute it directly (Android 10+ blocks execution of app_data_file context binaries).
     */
    private fun fixSelinuxContext(path: String) {
        val suPaths = listOf("/system_ext/bin/su", "/system/bin/su", "/system/xbin/su", "su")
        for (su in suPaths) {
            try {
                val proc = Runtime.getRuntime().exec(arrayOf(su, "-c", "chcon u:object_r:system_file:s0 $path"))
                proc.waitFor()
                proc.destroy()
                return
            } catch (_: Exception) { }
        }
    }

    private fun getBundledBusyBoxPath(): String? {
        val extracted = File(context.filesDir, "bin/$BUSYBOX_FILENAME")
        // Don't check canExecute() — SELinux in untrusted_app domain blocks it even with system_file context.
        // verifyBusyBox() will do the actual execution test (falling back to root if needed).
        if (extracted.exists()) {
            return extracted.absolutePath
        }
        return null
    }

    private fun verifyBusyBox(path: String): Boolean {
        // Try direct execution first; fall back to root shell for SELinux-restricted environments.
        return verifyDirect(path) || verifyViaRoot(path)
    }

    private fun verifyDirect(path: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf(path, "--help"))
            val stdoutThread = Thread { runCatching { process.inputStream.readBytes() } }
            stdoutThread.start()
            val stderr = process.errorStream.bufferedReader().readLine() ?: ""
            stdoutThread.join(2000)
            process.waitFor()
            process.destroy()
            stderr.contains("BusyBox", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    private fun verifyViaRoot(path: String): Boolean {
        val suPaths = listOf("/system_ext/bin/su", "/system/bin/su", "/system/xbin/su", "su")
        for (su in suPaths) {
            try {
                val proc = Runtime.getRuntime().exec(arrayOf(su, "-c", "$path --help 2>&1"))
                val output = proc.inputStream.bufferedReader().readLine() ?: ""
                proc.waitFor()
                proc.destroy()
                if (output.contains("BusyBox", ignoreCase = true)) return true
            } catch (_: Exception) { }
        }
        return false
    }
}
