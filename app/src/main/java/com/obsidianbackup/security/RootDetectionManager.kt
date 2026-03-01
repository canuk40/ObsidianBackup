// security/RootDetectionManager.kt
package com.obsidianbackup.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.obsidianbackup.logging.ObsidianLogger
import java.io.File

/**
 * Enhanced Root Detection using multiple detection methods
 * Implements OWASP MASVS-RESILIENCE requirements
 *
 * Detection methods:
 * 1. Build tags inspection
 * 2. Root management apps detection
 * 3. Su binary detection
 * 4. Dangerous properties check
 * 5. Read-write system check
 * 6. Busybox detection
 */
class RootDetectionManager(
    private val context: Context,
    private val logger: ObsidianLogger
) {
    companion object {
        private const val TAG = "RootDetection"
        
        // Root management apps
        private val ROOT_APPS = arrayOf(
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.yellowes.su",
            "com.topjohnwu.magisk",
            "com.kingroot.kinguser",
            "com.kingo.root",
            "com.smedialink.oneclickroot",
            "com.zhiqupk.root.global",
            "com.alephzain.framaroot"
        )
        
        // Su binary paths — generic
        private val SU_PATHS = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/su/bin",
            "/system/xbin/daemonsu"
        )

        // Chipset-specific su paths (from HARDWARE_COMPATIBILITY_RESEARCH.md)
        private val SU_PATHS_MEDIATEK = arrayOf(
            "/system_ext/bin/su",   // Ulefone Armor X13 / MediaTek k65v1
            "/vendor/bin/su",
            "/system/bin/magisk/su"
        )
        private val SU_PATHS_QUALCOMM = arrayOf(
            "/vendor/bin/su",
            "/sbin/.magisk/su",
            "/debug_ramdisk/su"
        )
        private val SU_PATHS_SAMSUNG_EXYNOS = arrayOf(
            "/system/bin/.ext/su",
            "/system/usr/we-need-root/su-backup"
        )
        
        // Dangerous properties
        // Note: ro.debuggable=1 is normal for userdebug/eng builds
        // Only flag ro.secure=0 which directly indicates insecure boot
        private val DANGEROUS_PROPS = mapOf(
            "[ro.secure]" to "[0]"  // Insecure boot - very strong root indicator
        )
    }
    
    /**
     * Comprehensive root detection result
     */
    data class RootDetectionResult(
        val isRooted: Boolean,
        val detectionMethod: String,
        val detectedRootApps: List<String> = emptyList(),
        val detectedSuPaths: List<String> = emptyList(),
        val dangerousProps: List<String> = emptyList(),
        val confidence: DetectionConfidence = DetectionConfidence.LOW
    )

    enum class DetectionConfidence {
        LOW,      // Only basic checks passed
        MEDIUM,   // Multiple checks failed
        HIGH,     // Critical checks failed
        CRITICAL  // Multiple critical indicators detected
    }
    
    /**
     * Perform comprehensive root detection
     * Returns null if inconclusive, true if rooted, false if not rooted
     */
    suspend fun detectRoot(): RootDetectionResult {
        val detectionMethods = mutableListOf<String>()
        var isRooted = false
        var confidence = DetectionConfidence.LOW

        // 1. Check for root management apps
        val rootApps = detectRootManagementApps()
        if (rootApps.isNotEmpty()) {
            isRooted = true
            confidence = maxOf(confidence, DetectionConfidence.MEDIUM)
            detectionMethods.add("Root Management Apps: ${rootApps.joinToString()}")
        }

        // 2. Check for su binaries
        val suPaths = detectSuBinaries()
        if (suPaths.isNotEmpty()) {
            isRooted = true
            confidence = maxOf(confidence, DetectionConfidence.MEDIUM)
            detectionMethods.add("Su Binaries: ${suPaths.joinToString()}")
        }

        // 3. Check build tags
        if (checkBuildTags()) {
            isRooted = true
            confidence = maxOf(confidence, DetectionConfidence.LOW)
            detectionMethods.add("Suspicious Build Tags")
        }

        // 4. Check dangerous properties
        val dangProps = checkDangerousProperties()
        if (dangProps.isNotEmpty()) {
            isRooted = true
            confidence = maxOf(confidence, DetectionConfidence.MEDIUM)
            detectionMethods.add("Dangerous Properties: ${dangProps.joinToString()}")
        }

        // 5. Check if /system is writable
        if (isSystemWritable()) {
            isRooted = true
            confidence = maxOf(confidence, DetectionConfidence.MEDIUM)
            detectionMethods.add("Writable System Partition")
        }

        // 6. Check for busybox
        if (checkBusybox()) {
            isRooted = true
            confidence = maxOf(confidence, DetectionConfidence.LOW)
            detectionMethods.add("Busybox Detected")
        }
        
        // Upgrade to CRITICAL if multiple high-confidence indicators
        if (confidence == DetectionConfidence.HIGH && detectionMethods.size > 2) {
            confidence = DetectionConfidence.CRITICAL
        }
        
        logger.i(TAG, "Root detection result: rooted=$isRooted, confidence=$confidence, methods=$detectionMethods")
        
        return RootDetectionResult(
            isRooted = isRooted,
            detectionMethod = detectionMethods.joinToString("; "),
            detectedRootApps = rootApps,
            detectedSuPaths = suPaths,
            dangerousProps = dangProps,
            confidence = confidence
        )
    }
    
    /**
     * Detect root management applications
     */
    private fun detectRootManagementApps(): List<String> {
        val detected = mutableListOf<String>()
        val pm = context.packageManager
        
        for (packageName in ROOT_APPS) {
            try {
                pm.getPackageInfo(packageName, 0)
                detected.add(packageName)
                logger.w(TAG, "Detected root app: $packageName")
            } catch (e: PackageManager.NameNotFoundException) {
                // App not installed, which is good
            }
        }
        
        return detected
    }
    
    /**
     * Check for su binary in common locations
     */
    private fun detectSuBinaries(): List<String> {
        val detected = mutableListOf<String>()

        // Combine generic paths with chipset-specific paths
        val chipset = getChipsetFamily()
        val allPaths = SU_PATHS + when (chipset) {
            ChipsetFamily.MEDIATEK -> SU_PATHS_MEDIATEK
            ChipsetFamily.QUALCOMM -> SU_PATHS_QUALCOMM
            ChipsetFamily.SAMSUNG_EXYNOS -> SU_PATHS_SAMSUNG_EXYNOS
            else -> emptyArray()
        }

        for (path in allPaths) {
            try {
                val file = File(path)
                if (file.exists()) {
                    detected.add(path)
                    logger.w(TAG, "Detected su binary: $path")
                }
            } catch (e: Exception) {
                // Access denied or error checking file
            }
        }
        
        return detected
    }

    private enum class ChipsetFamily { MEDIATEK, QUALCOMM, SAMSUNG_EXYNOS, GOOGLE_TENSOR, UNISOC, UNKNOWN }

    private fun getChipsetFamily(): ChipsetFamily {
        val hardware = Build.HARDWARE.lowercase()
        val board = Build.BOARD.lowercase()
        val soc = runCatching {
            // ro.hardware.chipname or ro.board.platform via system properties
            val cls = Class.forName("android.os.SystemProperties")
            val get = cls.getMethod("get", String::class.java)
            listOf("ro.hardware.chipname", "ro.board.platform", "ro.chipname")
                .map { get.invoke(null, it) as? String ?: "" }
                .firstOrNull { it.isNotEmpty() }?.lowercase() ?: ""
        }.getOrElse { "" }

        return when {
            hardware.contains("mt") || board.contains("mt") || soc.contains("mt") ||
                soc.contains("helio") || soc.contains("dimensity") -> ChipsetFamily.MEDIATEK
            hardware.contains("qcom") || board.contains("msm") || board.contains("sm") ||
                soc.contains("snapdragon") || soc.contains("qcom") -> ChipsetFamily.QUALCOMM
            hardware.contains("exynos") || soc.contains("exynos") ||
                board.contains("exynos") -> ChipsetFamily.SAMSUNG_EXYNOS
            hardware.contains("tensor") || soc.contains("tensor") ||
                board.contains("slider") -> ChipsetFamily.GOOGLE_TENSOR
            soc.contains("ums") || soc.contains("sc") -> ChipsetFamily.UNISOC
            else -> ChipsetFamily.UNKNOWN
        }
    }
    
    /**
     * Check for suspicious build tags
     * Only flags "test-keys" which indicates unofficial/development build
     * Custom ROMs with "release-keys" are considered legitimate
     */
    private fun checkBuildTags(): Boolean {
        val buildTags = Build.TAGS
        val isTestKeys = buildTags != null && buildTags.contains("test-keys")
        
        if (isTestKeys) {
            logger.w(TAG, "Build signed with test-keys: $buildTags")
        }
        
        return isTestKeys
    }
    
    /**
     * Check for dangerous system properties
     */
    private fun checkDangerousProperties(): List<String> {
        val detected = mutableListOf<String>()
        
        try {
            val process = Runtime.getRuntime().exec("getprop")
            val properties = process.inputStream.bufferedReader().readText()
            
            for ((prop, value) in DANGEROUS_PROPS) {
                if (properties.contains(prop) && properties.contains(value)) {
                    detected.add("$prop=$value")
                    logger.w(TAG, "Dangerous property detected: $prop=$value")
                }
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to check properties", e)
        }
        
        return detected
    }
    
    /**
     * Check if /system partition is writable
     */
    private fun isSystemWritable(): Boolean {
        return try {
            val systemDir = File("/system")
            systemDir.canWrite()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check for busybox installation in system locations
     * Only flag if busybox is in /system or /sbin (indicates system modification)
     * Busybox in /data/local/ is user-installed and doesn't indicate root
     */
    private fun checkBusybox(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("which busybox")
            val output = process.inputStream.bufferedReader().readText().trim()
            
            if (output.isEmpty()) {
                return false
            }
            
            // Only flag if busybox is in system locations
            // /data/local/ is user-writable and doesn't indicate root
            val isSystemLocation = output.startsWith("/system") || 
                                  output.startsWith("/sbin") ||
                                  output.startsWith("/vendor")
            
            if (isSystemLocation) {
                logger.w(TAG, "Busybox detected in system location: $output")
            }
            
            isSystemLocation
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Quick root check (synchronous, less comprehensive)
     */
    fun quickRootCheck(): Boolean {
        // Check build tags
        if (checkBuildTags()) return true
        
        // Check for common su binaries
        for (path in SU_PATHS.take(5)) {
            if (File(path).exists()) return true
        }
        
        return false
    }
}
