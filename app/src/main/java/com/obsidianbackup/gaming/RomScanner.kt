// gaming/RomScanner.kt
package com.obsidianbackup.gaming

import android.content.Context
import com.obsidianbackup.gaming.models.DetectedEmulator
import com.obsidianbackup.gaming.models.GameInfo
import com.obsidianbackup.logging.ObsidianLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans for ROM files and extracts game metadata.
 * Supports multiple ROM formats and provides progress updates.
 */
@Singleton
class RomScanner @Inject constructor(
    private val context: Context,
    private val logger: ObsidianLogger
) {
    
    companion object {
        private const val TAG = "RomScanner"
        
        // ROM file extensions by platform
        private val PLATFORM_EXTENSIONS = mapOf(
            "NES" to setOf("nes", "unf", "unif"),
            "SNES" to setOf("sfc", "smc", "swc"),
            "Game Boy" to setOf("gb", "gbc", "sgb"),
            "Game Boy Advance" to setOf("gba", "agb"),
            "Genesis" to setOf("smd", "gen", "bin", "md"),
            "Nintendo 64" to setOf("n64", "z64", "v64"),
            "Nintendo DS" to setOf("nds", "dsi"),
            "Nintendo 3DS" to setOf("3ds", "cia", "3dsx"),
            "PlayStation" to setOf("cue", "bin", "img", "chd", "pbp"),
            "PlayStation Portable" to setOf("iso", "cso", "pbp"),
            "PlayStation 2" to setOf("iso", "bin", "img"),
            "GameCube" to setOf("iso", "gcm", "gcz", "ciso"),
            "Wii" to setOf("iso", "wbfs", "wad", "dol")
        )
    }
    
    /**
     * Scan all ROM paths for the given emulator.
     * Returns a Flow that emits progress updates.
     */
    fun scanRoms(emulator: DetectedEmulator): Flow<ScanProgress> = flow {
        logger.i(TAG, "Starting ROM scan for ${emulator.name}")
        emit(ScanProgress.Started)
        
        val roms = mutableListOf<GameInfo>()
        var totalFiles = 0
        var scannedFiles = 0
        
        // Count total files first
        emulator.romPaths.forEach { path ->
            val dir = File(path)
            if (dir.exists() && dir.isDirectory) {
                totalFiles += dir.listFiles()?.count { isRomFile(it) } ?: 0
            }
        }
        
        emit(ScanProgress.Scanning(0, totalFiles))
        
        // Scan each ROM path
        emulator.romPaths.forEach { path ->
            val dir = File(path)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (isRomFile(file)) {
                        try {
                            val gameInfo = extractGameInfo(file, emulator)
                            roms.add(gameInfo)
                            scannedFiles++
                            
                            emit(ScanProgress.Scanning(scannedFiles, totalFiles))
                            emit(ScanProgress.Found(gameInfo))
                        } catch (e: Exception) {
                            logger.e(TAG, "Failed to process ROM: ${file.name}", e)
                        }
                    }
                }
            }
        }
        
        logger.i(TAG, "ROM scan completed: found ${roms.size} games")
        emit(ScanProgress.Completed(roms))
    }
    
    /**
     * Extract game information from ROM file.
     */
    suspend fun extractGameInfo(
        romFile: File,
        emulator: DetectedEmulator
    ): GameInfo = withContext(Dispatchers.IO) {
        val platform = detectPlatform(romFile, emulator)
        val region = detectRegion(romFile)
        
        GameInfo(
            name = extractGameName(romFile),
            platform = platform,
            romPath = romFile.absolutePath,
            romChecksum = calculateChecksum(romFile),
            region = region
        )
    }
    
    /**
     * Detect platform from file extension and emulator.
     */
    private fun detectPlatform(romFile: File, emulator: DetectedEmulator): String {
        val extension = romFile.extension.lowercase()
        
        // Try to match extension to platform
        PLATFORM_EXTENSIONS.forEach { (platform, extensions) ->
            if (extensions.contains(extension) && emulator.supportedPlatforms.contains(platform)) {
                return platform
            }
        }
        
        // Fallback to first supported platform
        return emulator.supportedPlatforms.firstOrNull() ?: "Unknown"
    }
    
    /**
     * Detect region from filename.
     * Common patterns: (USA), (Japan), (Europe), [U], [J], [E]
     */
    private fun detectRegion(romFile: File): String {
        val name = romFile.nameWithoutExtension.uppercase()
        
        return when {
            name.contains("(USA)") || name.contains("[U]") -> "USA"
            name.contains("(JAPAN)") || name.contains("[J]") -> "Japan"
            name.contains("(EUROPE)") || name.contains("[E]") -> "Europe"
            name.contains("(WORLD)") -> "World"
            name.contains("(ASIA)") -> "Asia"
            else -> "Unknown"
        }
    }
    
    /**
     * Extract clean game name from filename.
     * Removes region tags, version info, etc.
     */
    private fun extractGameName(romFile: File): String {
        var name = romFile.nameWithoutExtension
        
        // Remove common tags
        name = name.replace(Regex("\\(.*?\\)"), "") // Remove (USA), (Rev A), etc.
        name = name.replace(Regex("\\[.*?\\]"), "") // Remove [!], [U], etc.
        name = name.replace(Regex("\\{.*?\\}"), "") // Remove {Other tags}
        
        // Remove version numbers like "v1.0", "Rev 1"
        name = name.replace(Regex("v\\d+\\.\\d+"), "")
        name = name.replace(Regex("Rev \\w+"), "")
        
        // Replace underscores and multiple spaces
        name = name.replace("_", " ")
        name = name.replace(Regex("\\s+"), " ")
        
        return name.trim()
    }
    
    /**
     * Check if file is a ROM file based on extension.
     */
    private fun isRomFile(file: File): Boolean {
        if (!file.isFile) return false
        
        val extension = file.extension.lowercase()
        return PLATFORM_EXTENSIONS.values.any { it.contains(extension) }
    }
    
    /**
     * Calculate SHA-256 checksum of ROM file.
     * For large files, only hash first 1MB for performance.
     */
    private fun calculateChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val maxBytes = 1024 * 1024 // 1MB
        
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var totalRead = 0
            var read: Int
            
            while (input.read(buffer).also { read = it } != -1 && totalRead < maxBytes) {
                val bytesToHash = minOf(read, maxBytes - totalRead)
                digest.update(buffer, 0, bytesToHash)
                totalRead += bytesToHash
            }
        }
        
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    
    sealed class ScanProgress {
        object Started : ScanProgress()
        data class Scanning(val current: Int, val total: Int) : ScanProgress()
        data class Found(val game: GameInfo) : ScanProgress()
        data class Completed(val games: List<GameInfo>) : ScanProgress()
        data class Error(val message: String) : ScanProgress()
    }
}
