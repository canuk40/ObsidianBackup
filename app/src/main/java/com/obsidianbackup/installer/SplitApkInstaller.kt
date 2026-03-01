// installer/SplitApkInstaller.kt
package com.obsidianbackup.installer

import com.obsidianbackup.engine.shell.SafeShellExecutor
import com.obsidianbackup.engine.ShellResult
import com.obsidianbackup.logging.ObsidianLogger
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Progress callback for split APK installation
 */
data class InstallProgress(
    val currentSplit: String,
    val splitIndex: Int,
    val totalSplits: Int,
    val bytesProcessed: Long,
    val totalBytes: Long,
    val percentComplete: Int,
    val phase: InstallPhase
)

/**
 * Installation phase tracking
 */
enum class InstallPhase {
    PREPARING,
    CREATING_SESSION,
    WRITING_APKS,
    COMMITTING,
    COMPLETE,
    FAILED,
    ROLLING_BACK
}

/**
 * Result of installation operation
 */
sealed class InstallResult {
    data class Success(val packageName: String, val splitsInstalled: Int) : InstallResult()
    data class Failure(
        val error: String,
        val phase: InstallPhase,
        val packageName: String?,
        val rollbackSuccessful: Boolean = false
    ) : InstallResult()
}

/**
 * Dedicated installer for split APKs with progress reporting and rollback mechanism
 */
class SplitApkInstaller(
    private val shellExecutor: SafeShellExecutor,
    private val logger: ObsidianLogger
) {
    companion object {
        private const val TAG = "SplitApkInstaller"
    }
    
    private val _progress = MutableStateFlow(
        InstallProgress(
            currentSplit = "",
            splitIndex = 0,
            totalSplits = 0,
            bytesProcessed = 0L,
            totalBytes = 0L,
            percentComplete = 0,
            phase = InstallPhase.PREPARING
        )
    )
    val progress: StateFlow<InstallProgress> = _progress.asStateFlow()
    
    /**
     * Install a single APK file
     */
    suspend fun installSingleApk(apkFile: File, packageName: String): InstallResult {
        logger.d(TAG, "Installing single APK: ${apkFile.name}")
        
        _progress.value = InstallProgress(
            currentSplit = apkFile.name,
            splitIndex = 0,
            totalSplits = 1,
            bytesProcessed = 0L,
            totalBytes = apkFile.length(),
            percentComplete = 0,
            phase = InstallPhase.PREPARING
        )
        
        if (!apkFile.exists()) {
            return InstallResult.Failure(
                error = "APK file not found: ${apkFile.absolutePath}",
                phase = InstallPhase.PREPARING,
                packageName = packageName
            )
        }
        
        _progress.value = _progress.value.copy(phase = InstallPhase.WRITING_APKS)
        
        val installCmd = "pm install -r '${apkFile.absolutePath}'"
        val result = shellExecutor.execute(installCmd)
        
        return if (result is ShellResult.Success) {
            _progress.value = _progress.value.copy(
                bytesProcessed = apkFile.length(),
                percentComplete = 100,
                phase = InstallPhase.COMPLETE
            )
            logger.d(TAG, "Successfully installed single APK: $packageName")
            InstallResult.Success(packageName = packageName, splitsInstalled = 1)
        } else {
            val error = (result as? ShellResult.Error)?.error ?: "Unknown error"
            _progress.value = _progress.value.copy(phase = InstallPhase.FAILED)
            logger.e(TAG, "Failed to install single APK: $error")
            InstallResult.Failure(
                error = "Installation failed: $error",
                phase = InstallPhase.WRITING_APKS,
                packageName = packageName
            )
        }
    }
    
    /**
     * Install split APKs using pm install-create session with progress tracking and rollback
     */
    suspend fun installSplitApks(
        baseApk: File,
        splitApks: List<File>,
        packageName: String
    ): InstallResult {
        val allApks = listOf(baseApk) + splitApks
        val totalSize = allApks.sumOf { it.length() }
        var sessionId: String? = null
        
        logger.d(TAG, "Installing ${allApks.size} APKs for $packageName (total: $totalSize bytes)")
        
        _progress.value = InstallProgress(
            currentSplit = "",
            splitIndex = 0,
            totalSplits = allApks.size,
            bytesProcessed = 0L,
            totalBytes = totalSize,
            percentComplete = 0,
            phase = InstallPhase.PREPARING
        )
        
        try {
            // Step 1: Create install session
            _progress.value = _progress.value.copy(phase = InstallPhase.CREATING_SESSION)
            
            val createSessionCmd = "pm install-create -S $totalSize"
            val createResult = shellExecutor.execute(createSessionCmd)
            
            if (createResult !is ShellResult.Success) {
                val error = (createResult as? ShellResult.Error)?.error ?: "Unknown error"
                logger.e(TAG, "Failed to create install session: $error")
                return InstallResult.Failure(
                    error = "Failed to create install session: $error",
                    phase = InstallPhase.CREATING_SESSION,
                    packageName = packageName
                )
            }
            
            // Extract session ID
            sessionId = extractSessionId(createResult.output)
            if (sessionId == null) {
                logger.e(TAG, "Failed to extract session ID from: ${createResult.output}")
                return InstallResult.Failure(
                    error = "Failed to extract session ID from output: ${createResult.output}",
                    phase = InstallPhase.CREATING_SESSION,
                    packageName = packageName
                )
            }
            
            logger.d(TAG, "Created install session: $sessionId")
            
            // Step 2: Write each APK to the session with progress tracking
            _progress.value = _progress.value.copy(phase = InstallPhase.WRITING_APKS)
            
            var bytesProcessed = 0L
            allApks.forEachIndexed { index, apkFile ->
                val apkSize = apkFile.length()
                val apkName = apkFile.name
                
                _progress.value = InstallProgress(
                    currentSplit = apkName,
                    splitIndex = index,
                    totalSplits = allApks.size,
                    bytesProcessed = bytesProcessed,
                    totalBytes = totalSize,
                    percentComplete = ((bytesProcessed * 100) / totalSize).toInt(),
                    phase = InstallPhase.WRITING_APKS
                )
                
                logger.d(TAG, "Writing APK $index/${allApks.size}: $apkName ($apkSize bytes)")
                
                if (!apkFile.exists()) {
                    val error = "APK file not found: ${apkFile.absolutePath}"
                    logger.e(TAG, error)
                    return rollbackAndFail(
                        sessionId = sessionId,
                        error = error,
                        phase = InstallPhase.WRITING_APKS,
                        packageName = packageName
                    )
                }
                
                val writeCmd = "pm install-write -S $apkSize $sessionId $index '${apkFile.absolutePath}'"
                val writeResult = shellExecutor.execute(writeCmd)
                
                if (writeResult !is ShellResult.Success) {
                    val error = (writeResult as? ShellResult.Error)?.error ?: "Unknown error"
                    logger.e(TAG, "Failed to write APK '$apkName': $error")
                    return rollbackAndFail(
                        sessionId = sessionId,
                        error = "Failed to write APK '$apkName' to session: $error",
                        phase = InstallPhase.WRITING_APKS,
                        packageName = packageName
                    )
                }
                
                bytesProcessed += apkSize
                logger.d(TAG, "Successfully wrote APK '$apkName' (index $index)")
            }
            
            // Step 3: Commit the session
            _progress.value = _progress.value.copy(
                bytesProcessed = totalSize,
                percentComplete = 95,
                phase = InstallPhase.COMMITTING
            )
            
            logger.d(TAG, "Committing install session $sessionId")
            
            val commitCmd = "pm install-commit $sessionId"
            val commitResult = shellExecutor.execute(commitCmd)
            
            return if (commitResult is ShellResult.Success) {
                _progress.value = InstallProgress(
                    currentSplit = "",
                    splitIndex = allApks.size,
                    totalSplits = allApks.size,
                    bytesProcessed = totalSize,
                    totalBytes = totalSize,
                    percentComplete = 100,
                    phase = InstallPhase.COMPLETE
                )
                logger.d(TAG, "Successfully installed split APKs for $packageName")
                InstallResult.Success(packageName = packageName, splitsInstalled = allApks.size)
            } else {
                val error = (commitResult as? ShellResult.Error)?.error ?: "Unknown error"
                logger.e(TAG, "Failed to commit install session: $error")
                rollbackAndFail(
                    sessionId = sessionId,
                    error = "Failed to commit install session: $error",
                    phase = InstallPhase.COMMITTING,
                    packageName = packageName
                )
            }
            
        } catch (e: Exception) {
            logger.e(TAG, "Exception during installation", e)
            return rollbackAndFail(
                sessionId = sessionId,
                error = "Exception during installation: ${e.message}",
                phase = InstallPhase.FAILED,
                packageName = packageName
            )
        }
    }
    
    /**
     * Install APKs from a directory (auto-detects split vs single)
     */
    suspend fun installApksFromDirectory(
        apkDir: File,
        packageName: String
    ): InstallResult {
        logger.d(TAG, "Installing APKs from directory: ${apkDir.absolutePath}")
        
        val baseApk = File(apkDir, "base.apk")
        if (!baseApk.exists()) {
            return InstallResult.Failure(
                error = "Base APK not found in ${apkDir.absolutePath}",
                phase = InstallPhase.PREPARING,
                packageName = packageName
            )
        }
        
        // Find all split APKs
        val splitApks = apkDir.listFiles { file ->
            file.name.endsWith(".apk") && file.name != "base.apk"
        }?.toList() ?: emptyList()
        
        return if (splitApks.isEmpty()) {
            // Single APK installation
            installSingleApk(baseApk, packageName)
        } else {
            // Split APK installation
            installSplitApks(baseApk, splitApks, packageName)
        }
    }
    
    /**
     * Rollback installation and return failure result
     */
    private suspend fun rollbackAndFail(
        sessionId: String?,
        error: String,
        phase: InstallPhase,
        packageName: String?
    ): InstallResult {
        var rollbackSuccessful = false
        
        if (sessionId != null) {
            _progress.value = _progress.value.copy(phase = InstallPhase.ROLLING_BACK)
            logger.d(TAG, "Rolling back install session $sessionId")
            
            val abandonCmd = "pm install-abandon $sessionId"
            val abandonResult = shellExecutor.execute(abandonCmd)
            
            rollbackSuccessful = abandonResult is ShellResult.Success
            
            if (rollbackSuccessful) {
                logger.d(TAG, "Successfully rolled back session $sessionId")
            } else {
                logger.e(TAG, "Failed to rollback session $sessionId: ${(abandonResult as? ShellResult.Error)?.error}")
            }
        }
        
        _progress.value = _progress.value.copy(phase = InstallPhase.FAILED)
        
        return InstallResult.Failure(
            error = error,
            phase = phase,
            packageName = packageName,
            rollbackSuccessful = rollbackSuccessful
        )
    }
    
    /**
     * Extract session ID from pm install-create output
     * Format: "Success: created install session [123456]"
     */
    private fun extractSessionId(output: String): String? {
        val regex = Regex("""\[(\d+)\]""")
        return regex.find(output)?.groupValues?.get(1)
    }
    
    /**
     * Reset progress state
     */
    fun resetProgress() {
        _progress.value = InstallProgress(
            currentSplit = "",
            splitIndex = 0,
            totalSplits = 0,
            bytesProcessed = 0L,
            totalBytes = 0L,
            percentComplete = 0,
            phase = InstallPhase.PREPARING
        )
    }
}
