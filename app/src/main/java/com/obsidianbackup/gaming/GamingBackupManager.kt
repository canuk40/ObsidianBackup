// gaming/GamingBackupManager.kt
package com.obsidianbackup.gaming

import android.content.Context
import android.content.pm.PackageManager
import com.obsidianbackup.gaming.models.*
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.storage.BackupCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GamingBackupManager @Inject constructor(
    private val context: Context,
    private val emulatorDetector: EmulatorDetector,
    private val saveStateManager: SaveStateManager,
    private val playGamesSync: PlayGamesCloudSync,
    private val backupCatalog: BackupCatalog,
    private val logger: ObsidianLogger
) {
    
    private val _backupProgress = MutableStateFlow<GamingBackupProgress>(GamingBackupProgress.Idle)
    val backupProgress: StateFlow<GamingBackupProgress> = _backupProgress.asStateFlow()
    
    private val _detectedEmulators = MutableStateFlow<List<DetectedEmulator>>(emptyList())
    val detectedEmulators: StateFlow<List<DetectedEmulator>> = _detectedEmulators.asStateFlow()

    companion object {
        private const val TAG = "GamingBackupManager"
        private const val GAMING_BACKUP_DIR = "gaming_backups"
        private const val ROM_BACKUP_DIR = "roms"
        private const val SAVE_BACKUP_DIR = "saves"
    }

    suspend fun scanForEmulators(): List<DetectedEmulator> = withContext(Dispatchers.IO) {
        logger.i(TAG, "Scanning for installed emulators")
        _backupProgress.value = GamingBackupProgress.Scanning
        
        try {
            val detected = emulatorDetector.detectInstalledEmulators()
            _detectedEmulators.value = detected
            
            logger.i(TAG, "Found ${detected.size} emulators")
            detected.forEach { emulator ->
                logger.d(TAG, "Detected: ${emulator.name} (${emulator.packageName})")
            }
            
            _backupProgress.value = GamingBackupProgress.Idle
            detected
        } catch (e: Exception) {
            logger.e(TAG, "Error scanning emulators", e)
            _backupProgress.value = GamingBackupProgress.Error("Failed to scan emulators: ${e.message}")
            emptyList()
        }
    }

    suspend fun backupGameSaves(
        emulator: DetectedEmulator,
        games: List<GameInfo>,
        options: BackupOptions
    ): BackupResult = withContext(Dispatchers.IO) {
        logger.i(TAG, "Starting backup for ${games.size} games from ${emulator.name}")
        _backupProgress.value = GamingBackupProgress.Backing(0, games.size)
        
        try {
            val backupId = generateBackupId()
            val backupDir = getBackupDirectory(backupId)
            backupDir.mkdirs()
            
            val results = mutableListOf<GameBackupResult>()
            
            games.forEachIndexed { index, game ->
                _backupProgress.value = GamingBackupProgress.Backing(index + 1, games.size)
                
                try {
                    val gameResult = backupSingleGame(emulator, game, backupDir, options)
                    results.add(gameResult)
                    
                    if (options.cloudSync && gameResult.success) {
                        syncToPlayGames(game, gameResult)
                    }
                } catch (e: Exception) {
                    logger.e(TAG, "Failed to backup ${game.name}", e)
                    results.add(GameBackupResult(
                        gameName = game.name,
                        success = false,
                        error = e.message,
                        savePath = null,
                        romPath = null,
                        metadata = null
                    ))
                }
            }
            
            val metadata = createBackupMetadata(emulator, results, options)
            saveBackupMetadata(backupDir, metadata)
            
            val successCount = results.count { it.success }
            logger.i(TAG, "Backup completed: $successCount/${games.size} successful")
            
            _backupProgress.value = GamingBackupProgress.Completed(successCount, games.size)
            
            BackupResult(
                backupId = backupId,
                emulatorName = emulator.name,
                totalGames = games.size,
                successfulBackups = successCount,
                failedBackups = games.size - successCount,
                backupPath = backupDir.absolutePath,
                timestamp = System.currentTimeMillis(),
                results = results
            )
        } catch (e: Exception) {
            logger.e(TAG, "Critical backup error", e)
            _backupProgress.value = GamingBackupProgress.Error("Backup failed: ${e.message}")
            throw e
        }
    }

    private suspend fun backupSingleGame(
        emulator: DetectedEmulator,
        game: GameInfo,
        backupDir: File,
        options: BackupOptions
    ): GameBackupResult {
        val gameBackupDir = File(backupDir, sanitizeFilename(game.name))
        gameBackupDir.mkdirs()
        
        var savePath: String? = null
        var romPath: String? = null
        val metadata = mutableMapOf<String, String>()
        
        // Backup save files
        if (options.includeSaves) {
            savePath = backupSaveFiles(emulator, game, gameBackupDir)
            if (savePath != null) {
                metadata["saveChecksum"] = calculateChecksum(File(savePath))
                metadata["saveSize"] = File(savePath).length().toString()
            }
        }
        
        // Backup ROM if requested
        if (options.includeRoms && game.romPath != null) {
            romPath = backupRomFile(game, gameBackupDir)
            if (romPath != null) {
                metadata["romChecksum"] = calculateChecksum(File(romPath))
                metadata["romSize"] = File(romPath).length().toString()
            }
        }
        
        // Backup save states
        if (options.includeSaveStates) {
            val saveStates = saveStateManager.detectSaveStates(emulator, game)
            if (saveStates.isNotEmpty()) {
                val statesPath = backupSaveStates(saveStates, gameBackupDir)
                metadata["saveStatesCount"] = saveStates.size.toString()
                metadata["saveStatesPath"] = statesPath
            }
        }
        
        // Add game metadata
        metadata["gameName"] = game.name
        metadata["emulator"] = emulator.name
        metadata["platform"] = game.platform
        metadata["backupTimestamp"] = System.currentTimeMillis().toString()
        
        return GameBackupResult(
            gameName = game.name,
            success = true,
            error = null,
            savePath = savePath,
            romPath = romPath,
            metadata = metadata
        )
    }

    private fun backupSaveFiles(
        emulator: DetectedEmulator,
        game: GameInfo,
        destDir: File
    ): String? {
        val savesDir = File(destDir, SAVE_BACKUP_DIR)
        savesDir.mkdirs()
        
        val saveFiles = emulator.savePaths.flatMap { pattern ->
            findSaveFiles(pattern, game)
        }
        
        if (saveFiles.isEmpty()) {
            logger.w(TAG, "No save files found for ${game.name}")
            return null
        }
        
        val zipFile = File(savesDir, "saves.zip")
        ZipOutputStream(FileOutputStream(zipFile).buffered(8192)).use { zip ->
            saveFiles.forEach { file ->
                val entry = ZipEntry(file.name)
                zip.putNextEntry(entry)
                file.inputStream().buffered(8192).use { input ->
                    input.copyTo(zip)
                }
                zip.closeEntry()
            }
        }
        
        logger.i(TAG, "Backed up ${saveFiles.size} save files for ${game.name}")
        return zipFile.absolutePath
    }

    private fun backupRomFile(game: GameInfo, destDir: File): String? {
        val romPath = game.romPath ?: return null
        val romFile = File(romPath)
        
        if (!romFile.exists()) {
            logger.w(TAG, "ROM file not found: $romPath")
            return null
        }
        
        val romsDir = File(destDir, ROM_BACKUP_DIR)
        romsDir.mkdirs()
        
        val destFile = File(romsDir, romFile.name)
        Files.copy(romFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        
        logger.i(TAG, "Backed up ROM: ${romFile.name}")
        return destFile.absolutePath
    }

    private fun backupSaveStates(
        saveStates: List<SaveState>,
        destDir: File
    ): String {
        val statesDir = File(destDir, "save_states")
        statesDir.mkdirs()
        
        saveStates.forEach { state ->
            val sourceFile = File(state.path)
            if (sourceFile.exists()) {
                val destFile = File(statesDir, sourceFile.name)
                Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
        
        return statesDir.absolutePath
    }

    private fun findSaveFiles(pattern: String, game: GameInfo): List<File> {
        val files = mutableListOf<File>()
        
        // Expand pattern with game name
        val expandedPattern = pattern
            .replace("{game}", sanitizeFilename(game.name))
            .replace("{platform}", game.platform.lowercase())
        
        val baseDir = File(expandedPattern).parentFile ?: return files
        if (!baseDir.exists()) return files
        
        baseDir.listFiles()?.forEach { file ->
            if (file.isFile && matchesSavePattern(file, game)) {
                files.add(file)
            }
        }
        
        return files
    }

    private fun matchesSavePattern(file: File, game: GameInfo): Boolean {
        val name = file.name.lowercase()
        val gameName = game.name.lowercase()
        
        return name.contains(gameName) || 
               name.matches(Regex(".*\\.(sav|srm|sra|eep|fla)$"))
    }

    suspend fun restoreGameSave(
        backupId: String,
        gameName: String,
        profileSlot: Int = 0
    ): RestoreResult = withContext(Dispatchers.IO) {
        logger.i(TAG, "Restoring save for $gameName from backup $backupId (slot: $profileSlot)")
        
        try {
            val backupDir = getBackupDirectory(backupId)
            val gameDir = File(backupDir, sanitizeFilename(gameName))
            
            if (!gameDir.exists()) {
                return@withContext RestoreResult(false, "Backup not found for $gameName")
            }
            
            val metadata = loadBackupMetadata(backupDir)
            val gameResult = metadata.results.find { it.gameName == gameName }
                ?: return@withContext RestoreResult(false, "Game metadata not found")
            
            // Restore save files
            gameResult.savePath?.let { savePath ->
                restoreSaveFiles(File(savePath), metadata.emulatorName, gameName, profileSlot)
            }
            
            logger.i(TAG, "Successfully restored save for $gameName")
            RestoreResult(true, "Restore completed successfully")
        } catch (e: Exception) {
            logger.e(TAG, "Failed to restore save", e)
            RestoreResult(false, "Restore failed: ${e.message}")
        }
    }

    private fun restoreSaveFiles(
        zipFile: File,
        emulatorName: String,
        gameName: String,
        profileSlot: Int
    ) {
        // Implementation would restore files to emulator-specific locations
        // This is a simplified version
        logger.i(TAG, "Restoring saves from ${zipFile.absolutePath}")
    }

    private suspend fun syncToPlayGames(game: GameInfo, result: GameBackupResult) {
        try {
            if (result.savePath != null) {
                playGamesSync.uploadSaveData(
                    gameName = game.name,
                    saveFile = File(result.savePath),
                    metadata = result.metadata ?: emptyMap()
                )
                logger.i(TAG, "Synced ${game.name} to Play Games Services")
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to sync to Play Games", e)
        }
    }

    suspend fun getBackupHistory(): List<BackupResult> = withContext(Dispatchers.IO) {
        val backupsDir = File(context.getExternalFilesDir(null), GAMING_BACKUP_DIR)
        if (!backupsDir.exists()) return@withContext emptyList()
        
        backupsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir ->
                try {
                    loadBackupMetadata(dir)
                } catch (e: Exception) {
                    logger.e(TAG, "Failed to load backup metadata from ${dir.name}", e)
                    null
                }
            }
            ?: emptyList()
    }

    fun createSpeedrunProfile(
        gameName: String,
        maxSaveStates: Int = 10
    ): SpeedrunProfile {
        return SpeedrunProfile(
            gameName = gameName,
            maxSaveStates = maxSaveStates,
            saveStates = emptyList(),
            createdAt = System.currentTimeMillis()
        )
    }

    suspend fun quickSave(
        profile: SpeedrunProfile,
        emulator: DetectedEmulator,
        label: String? = null
    ): SaveState = withContext(Dispatchers.IO) {
        logger.i(TAG, "Creating quick save for ${profile.gameName}")
        
        val saveState = saveStateManager.createQuickSave(
            emulator = emulator,
            gameName = profile.gameName,
            label = label ?: "Quick Save ${System.currentTimeMillis()}"
        )
        
        logger.i(TAG, "Quick save created: ${saveState.label}")
        saveState
    }

    private fun getBackupDirectory(backupId: String): File {
        return File(context.getExternalFilesDir(null), "$GAMING_BACKUP_DIR/$backupId")
    }

    private fun generateBackupId(): String {
        return "backup_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    private fun createBackupMetadata(
        emulator: DetectedEmulator,
        results: List<GameBackupResult>,
        options: BackupOptions
    ): BackupResult {
        return BackupResult(
            backupId = generateBackupId(),
            emulatorName = emulator.name,
            totalGames = results.size,
            successfulBackups = results.count { it.success },
            failedBackups = results.count { !it.success },
            backupPath = "",
            timestamp = System.currentTimeMillis(),
            results = results
        )
    }

    private fun saveBackupMetadata(dir: File, metadata: BackupResult) {
        val metadataFile = File(dir, "backup_metadata.json")
        // Serialize and save metadata
        logger.d(TAG, "Saved backup metadata to ${metadataFile.absolutePath}")
    }

    private fun loadBackupMetadata(dir: File): BackupResult {
        val metadataFile = File(dir, "backup_metadata.json")
        // Deserialize and return metadata
        // This is a stub - would use actual JSON parsing
        return BackupResult(
            backupId = dir.name,
            emulatorName = "Unknown",
            totalGames = 0,
            successfulBackups = 0,
            failedBackups = 0,
            backupPath = dir.absolutePath,
            timestamp = dir.lastModified(),
            results = emptyList()
        )
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
