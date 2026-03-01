package com.obsidianbackup.rootcore.detection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [RootDetector] using shell commands.
 *
 * Detection strategy:
 * 1. Check if su binary exists in known locations
 * 2. Attempt to execute `su -c id` to verify root is granted
 * 3. Check for Magisk/KernelSU/APatch binary presence
 * 4. Read SELinux status from /sys/fs/selinux/enforce
 * 5. Query Zygisk/DenyList from Magisk SQLite
 *
 * All operations are performed on IO dispatcher with appropriate timeouts.
 *
 * Ported from ObsidianBox v31 production code.
 */
@Singleton
class RootDetectorImpl @Inject constructor() : RootDetector {

    private val cachedStatus = AtomicReference<RootStatus>(RootStatus.UNKNOWN)

    override suspend fun detectRootStatus(): RootStatus = withContext(Dispatchers.IO) {
        Timber.i("Starting root detection...")

        try {
            val suAvailable = checkSuBinaryExists()
            Timber.i("su binary available: $suAvailable")

            val rootGranted = if (suAvailable) {
                checkRootAccess()
            } else {
                false
            }
            Timber.i("Root granted: $rootGranted")

            val magiskDetected = checkMagiskPresent()
            Timber.d("Magisk detected: $magiskDetected")

            val kernelSuDetected = checkKernelSuPresent()
            Timber.d("KernelSU detected: $kernelSuDetected")

            val aPatchDetected = checkAPatchPresent()
            Timber.d("APatch detected: $aPatchDetected")

            val selinuxMode = getSelinuxMode(rootGranted)
            Timber.d("SELinux mode: $selinuxMode")

            val zygiskEnabled = if (magiskDetected && rootGranted) checkZygiskEnabled() else false
            Timber.d("Zygisk enabled: $zygiskEnabled")

            val denyListEnabled = if (magiskDetected && rootGranted) checkDenyListEnabled() else false
            Timber.d("DenyList enabled: $denyListEnabled")

            val ramdiskDetected = checkRamdiskPresent()
            Timber.d("Ramdisk detected: $ramdiskDetected")

            val status = RootStatus(
                suAvailable = suAvailable,
                rootGranted = rootGranted,
                magiskDetected = magiskDetected,
                kernelSuDetected = kernelSuDetected,
                aPatchDetected = aPatchDetected,
                zygiskEnabled = zygiskEnabled,
                denyListEnabled = denyListEnabled,
                ramdiskDetected = ramdiskDetected,
                selinuxMode = selinuxMode
            )

            cachedStatus.set(status)
            Timber.i("Root detection complete: ${status.summary}")

            status
        } catch (e: Exception) {
            Timber.e(e, "Root detection failed")
            RootStatus.NOT_ROOTED
        }
    }

    override suspend fun quickCheckSuPresent(): Boolean = withContext(Dispatchers.IO) {
        checkSuBinaryExists()
    }

    override fun getCachedStatus(maxAgeMs: Long): RootStatus {
        val cached = cachedStatus.get()
        return if (cached.isStale(maxAgeMs)) {
            RootStatus.UNKNOWN
        } else {
            cached
        }
    }

    override suspend fun getOrRefreshStatus(maxAgeMs: Long): RootStatus {
        val cached = cachedStatus.get()
        return if (cached.isStale(maxAgeMs)) {
            Timber.d("Root cache stale, refreshing...")
            detectRootStatus()
        } else {
            cached
        }
    }

    override fun clearCache() {
        cachedStatus.set(RootStatus.UNKNOWN)
    }

    private fun checkSuBinaryExists(): Boolean {
        // Check known paths — use both File.exists() and symlink check
        for (path in RootDetector.SU_BINARY_PATHS) {
            try {
                val file = File(path)
                val exists = file.exists()
                val canExec = file.canExecute()
                val isSymlink = java.nio.file.Files.isSymbolicLink(file.toPath())
                Timber.i("Checking su at $path: exists=$exists canExec=$canExec symlink=$isSymlink")
                if ((exists && canExec) || isSymlink) {
                    Timber.i("Found su at: $path")
                    return true
                }
            } catch (e: Exception) {
                Timber.w("Error checking su at $path: ${e.message}")
            }
        }

        // Fallback: try 'which su'
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val result = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            Timber.i("'which su' returned: '$result'")
            if (result.isNotEmpty()) {
                true
            } else false
        } catch (e: Exception) {
            Timber.w("'which su' failed: ${e.message}")
            false
        }
    }

    private suspend fun checkRootAccess(): Boolean {
        return try {
            withTimeout(RootDetector.ROOT_COMMAND_TIMEOUT_MS) {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = reader.readText()
                val exitCode = process.waitFor()
                reader.close()

                val isRoot = output.contains("uid=0") || output.contains("root")
                Timber.d("su -c id output: $output, exitCode: $exitCode, isRoot: $isRoot")

                isRoot && exitCode == 0
            }
        } catch (e: TimeoutCancellationException) {
            Timber.w("Root access check timed out - user may have denied permission")
            false
        } catch (e: Exception) {
            Timber.d("Root access check failed: ${e.message}")
            false
        }
    }

    private fun checkMagiskPresent(): Boolean {
        for (path in RootDetector.MAGISK_BINARY_PATHS) {
            try {
                val file = File(path)
                if (file.exists() || java.nio.file.Files.isSymbolicLink(file.toPath())) {
                    Timber.d("Found Magisk at: $path")
                    return true
                }
            } catch (_: Exception) {}
        }

        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "magisk"))
            val result = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            result.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    private fun checkKernelSuPresent(): Boolean {
        for (path in RootDetector.KERNELSU_PATHS) {
            if (File(path).exists()) {
                Timber.d("Found KernelSU at: $path")
                return true
            }
        }
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "ksud"))
            val result = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            result.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    private fun checkAPatchPresent(): Boolean {
        for (path in RootDetector.APATCH_PATHS) {
            if (File(path).exists()) {
                Timber.d("Found APatch at: $path")
                return true
            }
        }
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "apd"))
            val result = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            result.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    private fun getSelinuxMode(rootAvailable: Boolean = false): String? {
        return try {
            val enforceFile = File("/sys/fs/selinux/enforce")
            if (enforceFile.exists() && enforceFile.canRead()) {
                val value = enforceFile.readText().trim()
                return when (value) {
                    "1" -> "Enforcing"
                    "0" -> "Permissive"
                    else -> "Unknown"
                }
            }

            try {
                val process = Runtime.getRuntime().exec("getenforce")
                val result = process.inputStream.bufferedReader().readText().trim()
                process.waitFor()

                val mode = when (result.lowercase()) {
                    "enforcing" -> "Enforcing"
                    "permissive" -> "Permissive"
                    "disabled" -> "Disabled"
                    else -> null
                }
                if (mode != null) return mode
            } catch (_: Exception) {
                Timber.d("getenforce without root failed, trying with su")
            }

            if (rootAvailable) {
                try {
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "getenforce"))
                    val result = process.inputStream.bufferedReader().readText().trim()
                    process.waitFor()

                    when (result.lowercase()) {
                        "enforcing" -> "Enforcing"
                        "permissive" -> "Permissive"
                        "disabled" -> "Disabled"
                        else -> result.ifEmpty { null }
                    }
                } catch (e: Exception) {
                    Timber.d("su -c getenforce failed: ${e.message}")
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.d("Failed to get SELinux mode: ${e.message}")
            null
        }
    }

    private fun checkZygiskEnabled(): Boolean {
        try {
            val envVal = System.getenv("ZYGISK_ENABLED")
            if (envVal == "1") return true
            if (envVal == "0") return false
        } catch (_: Exception) { }

        try {
            val process = Runtime.getRuntime().exec(arrayOf(
                "su", "-c",
                "magisk --sqlite \"SELECT value FROM settings WHERE key='zygisk'\""
            ))
            val completed = process.waitFor(5, TimeUnit.SECONDS)
            if (completed) {
                val result = process.inputStream.bufferedReader().readText().trim()
                if (result.contains("value=1")) return true
                if (result.contains("value=0")) return false
            } else {
                process.destroyForcibly()
                Timber.d("Zygisk SQLite query timed out")
            }
        } catch (e: Exception) {
            Timber.d("Magisk SQLite zygisk query failed: ${e.message}")
        }

        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "test -d /data/adb/magisk/zygisk && echo yes || echo no"))
            val completed = process.waitFor(3, TimeUnit.SECONDS)
            if (completed) {
                val result = process.inputStream.bufferedReader().readText().trim()
                if (result == "yes") return true
            } else {
                process.destroyForcibly()
            }
        } catch (_: Exception) { }

        return false
    }

    private fun checkDenyListEnabled(): Boolean {
        try {
            val process = Runtime.getRuntime().exec(arrayOf(
                "su", "-c",
                "magisk --sqlite \"SELECT value FROM settings WHERE key='denylist'\""
            ))
            val completed = process.waitFor(5, TimeUnit.SECONDS)
            if (completed) {
                val result = process.inputStream.bufferedReader().readText().trim()
                if (result.contains("value=1")) return true
                if (result.contains("value=0")) return false
            } else {
                process.destroyForcibly()
                Timber.d("DenyList SQLite query timed out")
            }
        } catch (e: Exception) {
            Timber.d("Magisk SQLite denylist query failed: ${e.message}")
        }
        return false
    }

    private fun checkRamdiskPresent(): Boolean? {
        return try {
            val mounts = File("/proc/mounts").readText()
            val rootMount = mounts.lines().firstOrNull { it.contains(" / ") }

            if (rootMount != null) {
                val systemAsRoot = !rootMount.contains("rootfs")
                val legacySar = rootMount.contains("/dev/root")

                val cmdline = File("/proc/cmdline").readText()
                val isAB = cmdline.contains("androidboot.slot_suffix=") ||
                        cmdline.contains("androidboot.slot=")

                when {
                    isAB -> true
                    legacySar -> false
                    !systemAsRoot -> true
                    else -> true
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.d("Ramdisk detection failed: ${e.message}")
            null
        }
    }
}
