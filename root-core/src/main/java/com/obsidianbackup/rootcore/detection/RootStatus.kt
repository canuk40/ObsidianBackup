package com.obsidianbackup.rootcore.detection

/**
 * Represents the current root capability status of the device.
 *
 * Ported from ObsidianBox v31 production code.
 */
data class RootStatus(
    /** True if the `su` binary is found in PATH. Does NOT guarantee root access is granted. */
    val suAvailable: Boolean,

    /** True if root commands can be executed (i.e., `su -c id` returned uid=0). */
    val rootGranted: Boolean,

    /** True if Magisk is detected (via magisk binary or Magisk manager). */
    val magiskDetected: Boolean,

    /** True if KernelSU is detected (via ksud binary or /data/adb/ksu/). */
    val kernelSuDetected: Boolean = false,

    /** True if APatch is detected (via apd binary or /data/adb/ap/). */
    val aPatchDetected: Boolean = false,

    /** True if Zygisk is enabled in Magisk settings. */
    val zygiskEnabled: Boolean = false,

    /** True if Magisk DenyList is enabled. */
    val denyListEnabled: Boolean = false,

    /** Whether the device boots with a ramdisk. null = unknown/not checked. */
    val ramdiskDetected: Boolean? = null,

    /** SELinux mode: "Enforcing", "Permissive", "Disabled", or null if unknown. */
    val selinuxMode: String? = null,

    /** Human-readable summary of root status for UI display. */
    val summary: String = buildSummary(rootGranted, suAvailable, magiskDetected, kernelSuDetected, aPatchDetected, zygiskEnabled),

    /** Timestamp when this status was detected (epoch millis). */
    val detectedAt: Long = System.currentTimeMillis()
) {
    /** Check if this status is older than the given duration in milliseconds. */
    fun isStale(maxAgeMs: Long): Boolean {
        return System.currentTimeMillis() - detectedAt > maxAgeMs
    }

    /** Returns true if root is fully available and granted for operations. */
    fun canExecuteRootCommands(): Boolean = rootGranted

    /** Returns a status level for UI indicators. */
    fun getStatusLevel(): RootStatusLevel = when {
        rootGranted -> RootStatusLevel.GRANTED
        suAvailable -> RootStatusLevel.AVAILABLE_NOT_GRANTED
        else -> RootStatusLevel.NOT_AVAILABLE
    }

    companion object {
        private fun buildSummary(
            rootGranted: Boolean,
            suAvailable: Boolean,
            magiskDetected: Boolean,
            kernelSuDetected: Boolean,
            aPatchDetected: Boolean,
            zygiskEnabled: Boolean
        ): String {
            val zygiskSuffix = if (zygiskEnabled) " + Zygisk" else ""
            return when {
                rootGranted && magiskDetected -> "Root granted (Magisk$zygiskSuffix)"
                rootGranted && kernelSuDetected -> "Root granted (KernelSU)"
                rootGranted && aPatchDetected -> "Root granted (APatch)"
                rootGranted -> "Root granted"
                suAvailable -> "Root available but not granted"
                else -> "Root not available"
            }
        }

        val UNKNOWN = RootStatus(
            suAvailable = false,
            rootGranted = false,
            magiskDetected = false,
            selinuxMode = null,
            summary = "Not checked",
            detectedAt = 0L
        )

        val NOT_ROOTED = RootStatus(
            suAvailable = false,
            rootGranted = false,
            magiskDetected = false,
            selinuxMode = null,
            summary = "Root not available"
        )
    }
}

/** Status levels for root availability, used for UI indicators. */
enum class RootStatusLevel {
    /** Root is granted and available for use. UI: Green indicator. */
    GRANTED,
    /** su binary is present but permission has not been granted. UI: Yellow/amber indicator. */
    AVAILABLE_NOT_GRANTED,
    /** No su binary found, device appears to be non-rooted. UI: Red/gray indicator. */
    NOT_AVAILABLE
}
