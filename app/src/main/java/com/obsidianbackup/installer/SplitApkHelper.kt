// installer/SplitApkHelper.kt
package com.obsidianbackup.installer

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.obsidianbackup.engine.shell.SafeShellExecutor
import com.obsidianbackup.engine.ShellResult
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.model.AppId
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Metadata for split APK backups
 */
@Serializable
data class SplitApkMetadata(
    val packageName: String,
    val isSplit: Boolean,
    val baseApkPath: String,
    val splitNames: List<String> = emptyList(),
    val splitPaths: List<String> = emptyList()
)

/**
 * Information about a split APK
 */
data class SplitApkInfo(
    val name: String,
    val path: String,
    val size: Long
)

/**
 * Helper class for handling split APK detection, backup, and restore
 */
class SplitApkHelper(
    private val packageManager: PackageManager,
    private val shellExecutor: SafeShellExecutor,
    private val logger: ObsidianLogger
) {
    
    private val installer: SplitApkInstaller by lazy {
        SplitApkInstaller(shellExecutor, logger)
    }
    
    companion object {
        private const val TAG = "SplitApkHelper"
        
        // Common split types
        private val ABI_SPLITS = listOf(
            "armeabi", "armeabi-v7a", "arm64-v8a", 
            "x86", "x86_64", "mips", "mips64"
        )
        
        private val DENSITY_SPLITS = listOf(
            "ldpi", "mdpi", "hdpi", "xhdpi", 
            "xxhdpi", "xxxhdpi", "tvdpi"
        )
    }
    
    /**
     * Check if an app uses split APKs
     */
    fun isSplitApk(packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val splitDirs = appInfo.splitSourceDirs
                splitDirs != null && splitDirs.isNotEmpty()
            } else {
                false // Split APKs not supported before Android 5.0
            }
        } catch (e: Exception) {
            logger.e(TAG, "Error checking split APK for $packageName", e)
            false
        }
    }
    
    /**
     * Get split APK information for a package
     */
    fun getSplitApkInfo(packageName: String): SplitApkMetadata? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val baseApkPath = appInfo.sourceDir
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val splitSourceDirs = appInfo.splitSourceDirs
                val splitNames = appInfo.splitNames
                
                if (splitSourceDirs != null && splitNames != null) {
                    SplitApkMetadata(
                        packageName = packageName,
                        isSplit = true,
                        baseApkPath = baseApkPath,
                        splitNames = splitNames.toList(),
                        splitPaths = splitSourceDirs.toList()
                    )
                } else {
                    // Not a split APK
                    SplitApkMetadata(
                        packageName = packageName,
                        isSplit = false,
                        baseApkPath = baseApkPath
                    )
                }
            } else {
                // Pre-Lollipop, no split APK support
                SplitApkMetadata(
                    packageName = packageName,
                    isSplit = false,
                    baseApkPath = baseApkPath
                )
            }
        } catch (e: Exception) {
            logger.e(TAG, "Error getting split APK info for $packageName", e)
            null
        }
    }
    
    /**
     * Get all APK paths using pm command (alternative method)
     */
    suspend fun getApkPathsViaShell(packageName: String): List<String> {
        val cmd = "pm path $packageName"
        val result = shellExecutor.execute(cmd)
        
        return if (result is ShellResult.Success) {
            result.output.lines()
                .filter { it.startsWith("package:") }
                .map { it.removePrefix("package:").trim() }
                .filter { it.isNotEmpty() }
        } else {
            emptyList()
        }
    }
    
    /**
     * Get current device architecture
     */
    fun getDeviceArchitecture(): String {
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
    }
    
    /**
     * Get all supported ABIs for the device
     */
    fun getSupportedAbis(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Build.SUPPORTED_ABIS.toList()
        } else {
            @Suppress("DEPRECATION")
            listOf(Build.CPU_ABI, Build.CPU_ABI2).filter { it.isNotEmpty() }
        }
    }
    
    /**
     * Check if a split name is an ABI split
     */
    fun isAbiSplit(splitName: String): Boolean {
        return ABI_SPLITS.any { abi ->
            splitName.contains(abi, ignoreCase = true)
        }
    }
    
    /**
     * Check if a split name is a density split
     */
    fun isDensitySplit(splitName: String): Boolean {
        return DENSITY_SPLITS.any { density ->
            splitName.contains(density, ignoreCase = true)
        }
    }
    
    /**
     * Check if a split is compatible with the current device
     */
    fun isSplitCompatible(splitName: String): Boolean {
        // ABI splits - check against device ABIs
        if (isAbiSplit(splitName)) {
            val supportedAbis = getSupportedAbis()
            return supportedAbis.any { abi ->
                splitName.contains(abi, ignoreCase = true)
            }
        }
        
        // For other splits (language, density), generally compatible
        // Language splits can be selectively restored
        // Density splits are often optional
        return true
    }
    
    /**
     * Filter splits for restore based on device compatibility
     */
    fun filterSplitsForRestore(
        splits: List<SplitApkInfo>,
        filterAbi: Boolean = true
    ): List<SplitApkInfo> {
        return if (filterAbi) {
            splits.filter { split ->
                // Keep non-ABI splits and compatible ABI splits
                !isAbiSplit(split.name) || isSplitCompatible(split.name)
            }
        } else {
            splits
        }
    }
    
    /**
     * Backup all APKs (base + splits) to a directory
     */
    suspend fun backupApks(
        metadata: SplitApkMetadata,
        targetDir: File
    ): Long {
        targetDir.mkdirs()
        
        var totalSize = 0L
        
        // Copy base APK
        val baseApk = File(metadata.baseApkPath)
        if (baseApk.exists()) {
            val targetBase = File(targetDir, "base.apk")
            val copyCmd = "cp '${metadata.baseApkPath}' '${targetBase.absolutePath}'"
            val result = shellExecutor.execute(copyCmd)
            
            if (result is ShellResult.Success && targetBase.exists()) {
                totalSize += targetBase.length()
                logger.d(TAG, "Copied base APK: ${targetBase.length()} bytes")
            } else {
                logger.e(TAG, "Failed to copy base APK")
            }
        }
        
        // Copy split APKs
        if (metadata.isSplit) {
            metadata.splitPaths.forEachIndexed { index, splitPath ->
                val splitName = if (index < metadata.splitNames.size) {
                    metadata.splitNames[index]
                } else {
                    "split_$index"
                }
                
                val splitFile = File(splitPath)
                if (splitFile.exists()) {
                    val targetSplit = File(targetDir, "$splitName.apk")
                    val copyCmd = "cp '$splitPath' '${targetSplit.absolutePath}'"
                    val result = shellExecutor.execute(copyCmd)
                    
                    if (result is ShellResult.Success && targetSplit.exists()) {
                        totalSize += targetSplit.length()
                        logger.d(TAG, "Copied split APK '$splitName': ${targetSplit.length()} bytes")
                    } else {
                        logger.e(TAG, "Failed to copy split APK '$splitName'")
                    }
                }
            }
        }
        
        // Save metadata
        val metadataFile = File(targetDir, "apk_metadata.json")
        metadataFile.writeText(Json.encodeToString(metadata))
        
        return totalSize
    }
    
    /**
     * Restore APKs using pm install session for split APKs (delegates to SplitApkInstaller)
     */
    suspend fun restoreApks(
        apkDir: File,
        packageName: String
    ): Boolean {
        val result = installer.installApksFromDirectory(apkDir, packageName)
        return result is InstallResult.Success
    }
}
