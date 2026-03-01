// gaming/EmulatorDetector.kt
package com.obsidianbackup.gaming

import android.content.Context
import android.content.pm.PackageManager
import com.obsidianbackup.gaming.models.DetectedEmulator
import com.obsidianbackup.gaming.models.EmulatorType
import com.obsidianbackup.logging.ObsidianLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmulatorDetector @Inject constructor(
    private val context: Context,
    private val logger: ObsidianLogger
) {
    
    companion object {
        private const val TAG = "EmulatorDetector"
        
        // Emulator package definitions
        private val EMULATOR_CONFIGS = listOf(
            EmulatorConfig(
                type = EmulatorType.RETROARCH,
                packageName = "com.retroarch",
                name = "RetroArch",
                savePaths = listOf(
                    "/storage/emulated/0/RetroArch/saves/{game}.srm",
                    "/storage/emulated/0/Android/data/com.retroarch/files/saves/{game}.srm"
                ),
                saveStatePaths = listOf(
                    "/storage/emulated/0/RetroArch/states/{game}.state",
                    "/storage/emulated/0/Android/data/com.retroarch/files/states/{game}.state*"
                ),
                romPaths = listOf(
                    "/storage/emulated/0/RetroArch/roms",
                    "/storage/emulated/0/Android/data/com.retroarch/files/roms"
                ),
                supportedPlatforms = listOf(
                    "NES", "SNES", "Genesis", "Game Boy", "Game Boy Color", 
                    "Game Boy Advance", "PlayStation", "Nintendo 64", "Sega CD"
                )
            ),
            EmulatorConfig(
                type = EmulatorType.DOLPHIN,
                packageName = "org.dolphinemu.dolphinemu",
                name = "Dolphin Emulator",
                savePaths = listOf(
                    "/storage/emulated/0/dolphin-emu/GC/{game}/Saves",
                    "/storage/emulated/0/Android/data/org.dolphinemu.dolphinemu/files/GC/{game}/Saves"
                ),
                saveStatePaths = listOf(
                    "/storage/emulated/0/dolphin-emu/StateSaves/{game}.state*",
                    "/storage/emulated/0/Android/data/org.dolphinemu.dolphinemu/files/StateSaves"
                ),
                romPaths = listOf(
                    "/storage/emulated/0/dolphin-emu/GameCube",
                    "/storage/emulated/0/dolphin-emu/Wii"
                ),
                supportedPlatforms = listOf("GameCube", "Wii")
            ),
            EmulatorConfig(
                type = EmulatorType.PPSSPP,
                packageName = "org.ppsspp.ppsspp",
                name = "PPSSPP",
                savePaths = listOf(
                    "/storage/emulated/0/PSP/SAVEDATA/{game}",
                    "/storage/emulated/0/Android/data/org.ppsspp.ppsspp/files/PSP/SAVEDATA"
                ),
                saveStatePaths = listOf(
                    "/storage/emulated/0/PSP/PPSSPP_STATE/{game}_*.ppst",
                    "/storage/emulated/0/Android/data/org.ppsspp.ppsspp/files/PSP/PPSSPP_STATE"
                ),
                romPaths = listOf(
                    "/storage/emulated/0/PSP/GAME",
                    "/storage/emulated/0/PSP/ISO"
                ),
                supportedPlatforms = listOf("PlayStation Portable")
            ),
            EmulatorConfig(
                type = EmulatorType.DRASTIC,
                packageName = "com.dsemu.drastic",
                name = "DraStic DS Emulator",
                savePaths = listOf(
                    "/storage/emulated/0/DraStic/backup/{game}.dsv",
                    "/storage/emulated/0/Android/data/com.dsemu.drastic/files/backup"
                ),
                saveStatePaths = listOf(
                    "/storage/emulated/0/DraStic/savestates/{game}.ds*",
                    "/storage/emulated/0/Android/data/com.dsemu.drastic/files/savestates"
                ),
                romPaths = listOf(
                    "/storage/emulated/0/DraStic/roms"
                ),
                supportedPlatforms = listOf("Nintendo DS")
            ),
            EmulatorConfig(
                type = EmulatorType.CITRA,
                packageName = "org.citra.citra_emu",
                name = "Citra Emulator",
                savePaths = listOf(
                    "/storage/emulated/0/citra-emu/sdmc/Nintendo 3DS/*/*/title/{game}/data",
                    "/storage/emulated/0/Android/data/org.citra.citra_emu/files/sdmc"
                ),
                saveStatePaths = listOf(
                    "/storage/emulated/0/citra-emu/states/{game}.cst*",
                    "/storage/emulated/0/Android/data/org.citra.citra_emu/files/states"
                ),
                romPaths = listOf(
                    "/storage/emulated/0/citra-emu/roms",
                    "/storage/emulated/0/3DS"
                ),
                supportedPlatforms = listOf("Nintendo 3DS")
            ),
            EmulatorConfig(
                type = EmulatorType.AETHERSX2,
                packageName = "xyz.aethersx2.android",
                name = "AetherSX2",
                savePaths = listOf(
                    "/storage/emulated/0/Android/data/xyz.aethersx2.android/files/memcards/{game}",
                    "/storage/emulated/0/AetherSX2/memcards"
                ),
                saveStatePaths = listOf(
                    "/storage/emulated/0/Android/data/xyz.aethersx2.android/files/sstates/{game}*.p2s",
                    "/storage/emulated/0/AetherSX2/sstates"
                ),
                romPaths = listOf(
                    "/storage/emulated/0/AetherSX2/roms",
                    "/storage/emulated/0/PS2"
                ),
                supportedPlatforms = listOf("PlayStation 2")
            )
        )
    }
    
    suspend fun detectInstalledEmulators(): List<DetectedEmulator> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val detected = mutableListOf<DetectedEmulator>()
        
        EMULATOR_CONFIGS.forEach { config ->
            try {
                val packageInfo = packageManager.getPackageInfo(config.packageName, 0)
                val isInstalled = packageInfo != null
                
                if (isInstalled) {
                    val savePaths = detectExistingSavePaths(config.savePaths)
                    val romPaths = detectExistingRomPaths(config.romPaths)
                    
                    val emulator = DetectedEmulator(
                        type = config.type,
                        packageName = config.packageName,
                        name = config.name,
                        version = packageInfo.versionName ?: "Unknown",
                        isInstalled = true,
                        savePaths = savePaths.ifEmpty { config.savePaths },
                        saveStatePaths = config.saveStatePaths,
                        romPaths = romPaths.ifEmpty { config.romPaths },
                        supportedPlatforms = config.supportedPlatforms,
                        installedApkPath = packageInfo.applicationInfo?.sourceDir ?: ""
                    )
                    
                    detected.add(emulator)
                    logger.i(TAG, "Detected emulator: ${config.name} v${emulator.version}")
                }
            } catch (e: PackageManager.NameNotFoundException) {
                logger.d(TAG, "Emulator not installed: ${config.name}")
            } catch (e: Exception) {
                logger.e(TAG, "Error detecting emulator ${config.name}", e)
            }
        }
        
        detected
    }
    
    private fun detectExistingSavePaths(patterns: List<String>): List<String> {
        return patterns.filter { pattern ->
            val basePath = pattern.replace("/{game}.*".toRegex(), "")
            File(basePath).exists()
        }
    }
    
    private fun detectExistingRomPaths(paths: List<String>): List<String> {
        return paths.filter { path ->
            File(path).exists()
        }
    }
    
    fun getSupportedPlatforms(): List<String> {
        return EMULATOR_CONFIGS.flatMap { it.supportedPlatforms }.distinct().sorted()
    }
    
    fun getEmulatorForPlatform(platform: String): EmulatorConfig? {
        return EMULATOR_CONFIGS.find { 
            it.supportedPlatforms.any { p -> 
                p.equals(platform, ignoreCase = true) 
            }
        }
    }
    
    suspend fun scanForRoms(emulator: DetectedEmulator): List<File> = withContext(Dispatchers.IO) {
        val roms = mutableListOf<File>()
        
        emulator.romPaths.forEach { path ->
            val dir = File(path)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (isRomFile(file)) {
                        roms.add(file)
                    }
                }
            }
        }
        
        logger.i(TAG, "Found ${roms.size} ROMs for ${emulator.name}")
        roms
    }
    
    private fun isRomFile(file: File): Boolean {
        if (!file.isFile) return false
        
        val extension = file.extension.lowercase()
        val romExtensions = setOf(
            // Nintendo
            "nes", "sfc", "smc", "gb", "gbc", "gba", "n64", "z64", "v64", "nds", "3ds", "cia",
            // Sega
            "smd", "gen", "bin", "32x", "gg", "sms",
            // Sony
            "iso", "cso", "pbp", "cue", "bin", "img", "mdf",
            // Other
            "zip", "7z", "rar"
        )
        
        return romExtensions.contains(extension)
    }
    
    data class EmulatorConfig(
        val type: EmulatorType,
        val packageName: String,
        val name: String,
        val savePaths: List<String>,
        val saveStatePaths: List<String>,
        val romPaths: List<String>,
        val supportedPlatforms: List<String>
    )
}
