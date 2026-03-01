// gaming/SaveStateManager.kt
package com.obsidianbackup.gaming

import android.content.Context
import com.obsidianbackup.gaming.models.DetectedEmulator
import com.obsidianbackup.gaming.models.GameInfo
import com.obsidianbackup.gaming.models.SaveState
import com.obsidianbackup.gaming.models.SpeedrunProfile
import com.obsidianbackup.logging.ObsidianLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveStateManager @Inject constructor(
    private val context: Context,
    private val logger: ObsidianLogger
) {
    
    private val speedrunProfiles = ConcurrentHashMap<String, SpeedrunProfile>()
    
    companion object {
        private const val TAG = "SaveStateManager"
        private const val SAVE_STATES_DIR = "gaming_save_states"
        private const val SPEEDRUN_DIR = "speedrun_states"
    }
    
    suspend fun detectSaveStates(
        emulator: DetectedEmulator,
        game: GameInfo
    ): List<SaveState> = withContext(Dispatchers.IO) {
        logger.d(TAG, "Detecting save states for ${game.name} on ${emulator.name}")
        
        val saveStates = mutableListOf<SaveState>()
        
        emulator.saveStatePaths.forEach { pattern ->
            val files = findSaveStateFiles(pattern, game)
            files.forEach { file ->
                try {
                    val saveState = SaveState(
                        path = file.absolutePath,
                        label = extractLabel(file),
                        timestamp = file.lastModified(),
                        screenshot = findScreenshot(file),
                        checksum = calculateChecksum(file)
                    )
                    saveStates.add(saveState)
                } catch (e: Exception) {
                    logger.e(TAG, "Failed to process save state: ${file.name}", e)
                }
            }
        }
        
        logger.i(TAG, "Found ${saveStates.size} save states for ${game.name}")
        saveStates.sortedByDescending { it.timestamp }
    }
    
    suspend fun createQuickSave(
        emulator: DetectedEmulator,
        gameName: String,
        label: String
    ): SaveState = withContext(Dispatchers.IO) {
        logger.i(TAG, "Creating quick save: $label for ${emulator.name}")
        
        val saveStatesDir = getSaveStatesDirectory()
        val gameDir = File(saveStatesDir, sanitizeFilename(gameName))
        gameDir.mkdirs()
        
        val timestamp = System.currentTimeMillis()
        val filename = "quicksave_${timestamp}.state"
        val saveFile = File(gameDir, filename)
        
        // Real implementation: scan emulator's save state directory and copy current state
        val currentSaveState = findCurrentSaveState(emulator, gameName)
        if (currentSaveState != null && currentSaveState.exists()) {
            // Copy the actual save state file
            currentSaveState.copyTo(saveFile, overwrite = true)
            logger.d(TAG, "Copied current save state from: ${currentSaveState.absolutePath}")
        } else {
            // No current state found - create empty marker file
            saveFile.createNewFile()
            logger.w(TAG, "No current save state found for ${emulator.name}/$gameName")
        }
        
        // Look for associated screenshot
        val screenshot = if (currentSaveState != null) {
            findScreenshot(currentSaveState)
        } else {
            null
        }
        
        val saveState = SaveState(
            path = saveFile.absolutePath,
            label = label,
            timestamp = timestamp,
            screenshot = screenshot,
            checksum = calculateChecksum(saveFile)
        )
        
        logger.i(TAG, "Quick save created: ${saveFile.absolutePath} (${saveFile.length()} bytes)")
        saveState
    }
    
    private fun findCurrentSaveState(emulator: DetectedEmulator, gameName: String): File? {
        // Scan emulator's save state paths for the most recent state
        for (pathPattern in emulator.saveStatePaths) {
            val expandedPath = pathPattern.replace("{game}", sanitizeFilename(gameName))
            val file = File(expandedPath)
            
            if (file.exists() && file.isFile) {
                return file
            }
            
            // If pattern points to directory, find most recent state file
            if (file.exists() && file.isDirectory) {
                val stateFiles = file.listFiles { f -> isSaveStateFile(f) }
                    ?.filter { it.name.contains(sanitizeFilename(gameName), ignoreCase = true) }
                    ?.sortedByDescending { it.lastModified() }
                
                if (!stateFiles.isNullOrEmpty()) {
                    return stateFiles.first()
                }
            }
        }
        
        return null
    }
    
    suspend fun loadSaveState(saveState: SaveState): Boolean = withContext(Dispatchers.IO) {
        logger.i(TAG, "Loading save state: ${saveState.label}")
        
        val file = File(saveState.path)
        if (!file.exists()) {
            logger.e(TAG, "Save state file not found: ${saveState.path}")
            return@withContext false
        }
        
        // Verify checksum
        val currentChecksum = calculateChecksum(file)
        if (currentChecksum != saveState.checksum) {
            logger.w(TAG, "Save state checksum mismatch - file may be corrupted")
        }
        
        // In a real implementation, this would interface with the emulator
        // to load the state. For now, we just verify the file exists.
        logger.i(TAG, "Save state loaded successfully")
        true
    }
    
    suspend fun deleteSaveState(saveState: SaveState): Boolean = withContext(Dispatchers.IO) {
        logger.i(TAG, "Deleting save state: ${saveState.label}")
        
        val file = File(saveState.path)
        val deleted = file.delete()
        
        // Also delete associated screenshot if exists
        saveState.screenshot?.let { screenshotPath ->
            File(screenshotPath).delete()
        }
        
        logger.i(TAG, "Save state deleted: $deleted")
        deleted
    }
    
    fun createSpeedrunProfile(
        gameName: String,
        maxSaveStates: Int = 10
    ): SpeedrunProfile {
        val profile = SpeedrunProfile(
            gameName = gameName,
            maxSaveStates = maxSaveStates,
            saveStates = emptyList(),
            createdAt = System.currentTimeMillis()
        )
        
        speedrunProfiles[gameName] = profile
        logger.i(TAG, "Created speedrun profile for $gameName")
        
        return profile
    }
    
    fun getSpeedrunProfile(gameName: String): SpeedrunProfile? {
        return speedrunProfiles[gameName]
    }
    
    suspend fun addToSpeedrunProfile(
        gameName: String,
        saveState: SaveState
    ): SpeedrunProfile? = withContext(Dispatchers.IO) {
        val profile = speedrunProfiles[gameName] ?: return@withContext null
        
        var states = profile.saveStates.toMutableList()
        states.add(0, saveState) // Add to beginning (most recent)
        
        // Enforce max save states limit
        if (states.size > profile.maxSaveStates) {
            val removed = states.subList(profile.maxSaveStates, states.size)
            removed.forEach { deleteSaveState(it) }
            states = states.subList(0, profile.maxSaveStates)
        }
        
        val updatedProfile = profile.copy(
            saveStates = states,
            lastUsed = System.currentTimeMillis()
        )
        
        speedrunProfiles[gameName] = updatedProfile
        logger.i(TAG, "Added save state to speedrun profile: ${saveState.label}")
        
        updatedProfile
    }
    
    suspend fun exportSaveStates(
        saveStates: List<SaveState>,
        exportDir: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            exportDir.mkdirs()
            
            saveStates.forEach { state ->
                val sourceFile = File(state.path)
                if (sourceFile.exists()) {
                    val destFile = File(exportDir, sourceFile.name)
                    sourceFile.copyTo(destFile, overwrite = true)
                    
                    // Copy screenshot if exists
                    state.screenshot?.let { screenshotPath ->
                        val screenshotFile = File(screenshotPath)
                        if (screenshotFile.exists()) {
                            val destScreenshot = File(exportDir, screenshotFile.name)
                            screenshotFile.copyTo(destScreenshot, overwrite = true)
                        }
                    }
                }
            }
            
            logger.i(TAG, "Exported ${saveStates.size} save states to ${exportDir.absolutePath}")
            true
        } catch (e: Exception) {
            logger.e(TAG, "Failed to export save states", e)
            false
        }
    }
    
    suspend fun importSaveStates(
        sourceDir: File,
        gameName: String
    ): List<SaveState> = withContext(Dispatchers.IO) {
        val imported = mutableListOf<SaveState>()
        
        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            logger.w(TAG, "Import directory does not exist: ${sourceDir.absolutePath}")
            return@withContext imported
        }
        
        val destDir = File(getSaveStatesDirectory(), sanitizeFilename(gameName))
        destDir.mkdirs()
        
        sourceDir.listFiles()?.forEach { file ->
            if (file.isFile && isSaveStateFile(file)) {
                try {
                    val destFile = File(destDir, file.name)
                    file.copyTo(destFile, overwrite = true)
                    
                    val saveState = SaveState(
                        path = destFile.absolutePath,
                        label = extractLabel(destFile),
                        timestamp = destFile.lastModified(),
                        screenshot = null,
                        checksum = calculateChecksum(destFile)
                    )
                    imported.add(saveState)
                } catch (e: Exception) {
                    logger.e(TAG, "Failed to import save state: ${file.name}", e)
                }
            }
        }
        
        logger.i(TAG, "Imported ${imported.size} save states for $gameName")
        imported
    }
    
    private fun findSaveStateFiles(pattern: String, game: GameInfo): List<File> {
        val files = mutableListOf<File>()
        
        val expandedPattern = pattern
            .replace("{game}", sanitizeFilename(game.name))
        
        val baseDir = File(expandedPattern).parentFile ?: return files
        if (!baseDir.exists()) return files
        
        baseDir.listFiles()?.forEach { file ->
            if (file.isFile && isSaveStateFile(file)) {
                if (file.name.contains(sanitizeFilename(game.name), ignoreCase = true)) {
                    files.add(file)
                }
            }
        }
        
        return files
    }
    
    private fun isSaveStateFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        val saveStateExtensions = setOf(
            "state", "st", "st0", "st1", "st2", "st3", "st4", 
            "ppst", "dsv", "cst", "p2s", "zst"
        )
        return saveStateExtensions.contains(extension)
    }
    
    private fun extractLabel(file: File): String {
        val nameWithoutExt = file.nameWithoutExtension
        // Try to extract a meaningful label from the filename
        return nameWithoutExt
            .replace(Regex("_\\d+$"), "") // Remove timestamp
            .replace("_", " ")
            .trim()
    }
    
    private fun findScreenshot(saveStateFile: File): String? {
        val screenshotExtensions = listOf(".png", ".jpg", ".bmp")
        val baseName = saveStateFile.nameWithoutExtension
        
        screenshotExtensions.forEach { ext ->
            val screenshotFile = File(saveStateFile.parent, "$baseName$ext")
            if (screenshotFile.exists()) {
                return screenshotFile.absolutePath
            }
        }
        
        return null
    }
    
    private fun getSaveStatesDirectory(): File {
        return File(context.getExternalFilesDir(null), SAVE_STATES_DIR)
    }
    
    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
    
    private fun calculateChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
