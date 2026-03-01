package com.obsidianbackup.service

import android.content.Context
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.rootcore.detection.RootDetector
import com.obsidianbackup.rootcore.busybox.BusyBoxManager
import com.obsidianbackup.rootcore.storage.StorageDetector
import com.obsidianbackup.permissions.PermissionManager
import com.obsidianbackup.model.PermissionMode
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pre-flight validation before backup/restore operations.
 * Modeled after Swift Backup's PreconditionsActivity — validates all
 * requirements before starting an operation to avoid mid-operation failures.
 */
@Singleton
class PreconditionsChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionManager: PermissionManager,
    private val rootDetector: RootDetector,
    private val busyBoxManager: BusyBoxManager,
    private val storageDetector: StorageDetector,
    private val logger: ObsidianLogger
) {
    companion object {
        private const val TAG = "Preconditions"
        private const val MIN_FREE_SPACE_BYTES = 100L * 1024 * 1024 // 100MB minimum
    }

    data class ValidationResult(
        val checks: List<Check>,
        val canProceed: Boolean
    ) {
        val criticalFailures get() = checks.filter { !it.passed && it.severity == Severity.CRITICAL }
        val warnings get() = checks.filter { !it.passed && it.severity == Severity.WARNING }
    }

    data class Check(
        val name: String,
        val description: String,
        val passed: Boolean,
        val severity: Severity,
        val details: String? = null
    )

    enum class Severity { CRITICAL, WARNING, INFO }

    /**
     * Run all precondition checks for a backup operation.
     */
    suspend fun validateForBackup(
        requireRoot: Boolean = false,
        requiredSpaceBytes: Long = MIN_FREE_SPACE_BYTES,
        requireCloud: Boolean = false
    ): ValidationResult {
        val checks = mutableListOf<Check>()

        // 1. Permission mode available
        val caps = permissionManager.detectCapabilities()
        val mode = permissionManager.currentMode.value

        checks.add(Check(
            name = "Permission Mode",
            description = "Backup permission mode is available",
            passed = mode != PermissionMode.SAF || !requireRoot,
            severity = if (requireRoot) Severity.CRITICAL else Severity.WARNING,
            details = "Current mode: $mode"
        ))

        // 2. Root access (if required)
        if (requireRoot) {
            val rootStatus = rootDetector.getOrRefreshStatus()
            checks.add(Check(
                name = "Root Access",
                description = "Root (su) access is granted",
                passed = rootStatus.rootGranted,
                severity = Severity.CRITICAL,
                details = buildString {
                    append("su: ${rootStatus.suAvailable}")
                    if (rootStatus.magiskDetected) append(", Magisk")
                    if (rootStatus.kernelSuDetected) append(", KernelSU")
                    if (rootStatus.aPatchDetected) append(", APatch")
                }
            ))
        }

        // 3. BusyBox available
        val bbAvailable = busyBoxManager.isAvailable()
        checks.add(Check(
            name = "BusyBox",
            description = "BusyBox binary is available for tar/compression",
            passed = bbAvailable,
            severity = Severity.WARNING,
            details = if (bbAvailable) busyBoxManager.getVersion() else "Not found"
        ))

        // 4. Storage space
        val storage = storageDetector.getPrimaryStorage()
        val hasSpace = storage != null && storage.availableBytes >= requiredSpaceBytes
        checks.add(Check(
            name = "Storage Space",
            description = "Sufficient free storage space",
            passed = hasSpace,
            severity = Severity.CRITICAL,
            details = if (storage != null) {
                "%.1f GB free of %.1f GB (need %d MB)".format(
                    storage.availableGb, storage.totalGb,
                    requiredSpaceBytes / (1024 * 1024)
                )
            } else "Unable to detect storage"
        ))

        // 5. Storage writable
        val storageWritable = try {
            val testFile = java.io.File(context.getExternalFilesDir("backups"), ".test")
            testFile.createNewFile() && testFile.delete()
        } catch (e: Exception) { false }

        checks.add(Check(
            name = "Storage Writable",
            description = "Backup directory is writable",
            passed = storageWritable,
            severity = Severity.CRITICAL
        ))

        // 6. Battery check
        val batteryOk = checkBatteryLevel()
        checks.add(Check(
            name = "Battery Level",
            description = "Battery level is sufficient (>15% or charging)",
            passed = batteryOk,
            severity = Severity.WARNING,
            details = "Battery check ${if (batteryOk) "OK" else "LOW"}"
        ))

        val canProceed = checks.none { !it.passed && it.severity == Severity.CRITICAL }

        Timber.i("$TAG Validation: ${checks.count { it.passed }}/${checks.size} passed, canProceed=$canProceed")
        return ValidationResult(checks = checks, canProceed = canProceed)
    }

    /**
     * Quick check — just returns whether backup can proceed (no details).
     */
    suspend fun canBackup(requireRoot: Boolean = false): Boolean {
        return validateForBackup(requireRoot = requireRoot).canProceed
    }

    private fun checkBatteryLevel(): Boolean {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            val level = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val isCharging = bm.isCharging
            isCharging || level > 15
        } catch (e: Exception) {
            true // Assume OK if we can't check
        }
    }
}
