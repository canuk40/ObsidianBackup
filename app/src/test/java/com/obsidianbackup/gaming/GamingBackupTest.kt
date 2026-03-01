// Test file for gaming backup features
package com.obsidianbackup.gaming

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.obsidianbackup.gaming.models.*
import com.obsidianbackup.logging.ObsidianLogger
import com.obsidianbackup.storage.BackupCatalog
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.File

class GamingBackupManagerTest {
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var emulatorDetector: EmulatorDetector
    
    @Mock
    private lateinit var saveStateManager: SaveStateManager
    
    @Mock
    private lateinit var playGamesSync: PlayGamesCloudSync
    
    @Mock
    private lateinit var backupCatalog: BackupCatalog
    
    @Mock
    private lateinit var logger: ObsidianLogger
    
    private lateinit var gamingBackupManager: GamingBackupManager
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        gamingBackupManager = GamingBackupManager(
            context = context,
            emulatorDetector = emulatorDetector,
            saveStateManager = saveStateManager,
            playGamesSync = playGamesSync,
            backupCatalog = backupCatalog,
            logger = logger
        )
    }
    
    @Test
    fun `scanForEmulators returns detected emulators`() = runTest {
        // Given
        val mockEmulators = listOf(
            DetectedEmulator(
                type = EmulatorType.RETROARCH,
                packageName = "com.retroarch",
                name = "RetroArch",
                version = "1.14.0",
                isInstalled = true,
                savePaths = listOf("/storage/emulated/0/RetroArch/saves"),
                saveStatePaths = listOf("/storage/emulated/0/RetroArch/states"),
                romPaths = listOf("/storage/emulated/0/RetroArch/roms"),
                supportedPlatforms = listOf("NES", "SNES"),
                installedApkPath = "/data/app/com.retroarch"
            )
        )
        
        `when`(emulatorDetector.detectInstalledEmulators()).thenReturn(mockEmulators)
        
        // When
        val result = gamingBackupManager.scanForEmulators()
        
        // Then
        assertEquals(1, result.size)
        assertEquals("RetroArch", result[0].name)
        assertEquals(EmulatorType.RETROARCH, result[0].type)
    }
    
    @Test
    fun `backupGameSaves creates valid backup`() = runTest {
        // Given
        val emulator = DetectedEmulator(
            type = EmulatorType.RETROARCH,
            packageName = "com.retroarch",
            name = "RetroArch",
            version = "1.14.0",
            isInstalled = true,
            savePaths = listOf("/test/saves"),
            saveStatePaths = listOf("/test/states"),
            romPaths = listOf("/test/roms"),
            supportedPlatforms = listOf("NES"),
            installedApkPath = "/test/apk"
        )
        
        val game = GameInfo(
            name = "Test Game",
            platform = "NES"
        )
        
        val options = BackupOptions(
            includeSaves = true,
            includeRoms = false,
            includeSaveStates = true
        )
        
        `when`(context.getExternalFilesDir(null)).thenReturn(File("/test"))
        
        // When
        val result = gamingBackupManager.backupGameSaves(emulator, listOf(game), options)
        
        // Then
        assertNotNull(result)
        assertEquals("RetroArch", result.emulatorName)
        assertEquals(1, result.totalGames)
    }
    
    @Test
    fun `createSpeedrunProfile creates valid profile`() {
        // When
        val profile = gamingBackupManager.createSpeedrunProfile(
            gameName = "Super Mario 64",
            maxSaveStates = 10
        )
        
        // Then
        assertNotNull(profile)
        assertEquals("Super Mario 64", profile.gameName)
        assertEquals(10, profile.maxSaveStates)
        assertEquals(0, profile.saveStates.size)
    }
}

class EmulatorDetectorTest {
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var packageManager: PackageManager
    
    @Mock
    private lateinit var logger: ObsidianLogger
    
    private lateinit var emulatorDetector: EmulatorDetector
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(context.packageManager).thenReturn(packageManager)
        
        emulatorDetector = EmulatorDetector(context, logger)
    }
    
    @Test
    fun `detectInstalledEmulators finds RetroArch`() = runTest {
        // Given
        val packageInfo = PackageInfo().apply {
            packageName = "com.retroarch"
            versionName = "1.14.0"
        }
        
        `when`(packageManager.getPackageInfo("com.retroarch", 0)).thenReturn(packageInfo)
        
        // When
        val result = emulatorDetector.detectInstalledEmulators()
        
        // Then
        assertTrue(result.any { it.packageName == "com.retroarch" })
    }
    
    @Test
    fun `getSupportedPlatforms returns all platforms`() {
        // When
        val platforms = emulatorDetector.getSupportedPlatforms()
        
        // Then
        assertTrue(platforms.contains("NES"))
        assertTrue(platforms.contains("SNES"))
        assertTrue(platforms.contains("GameCube"))
        assertTrue(platforms.contains("PlayStation Portable"))
    }
    
    @Test
    fun `getEmulatorForPlatform returns correct emulator`() {
        // When
        val config = emulatorDetector.getEmulatorForPlatform("Nintendo DS")
        
        // Then
        assertNotNull(config)
        assertEquals(EmulatorType.DRASTIC, config?.type)
        assertEquals("com.dsemu.drastic", config?.packageName)
    }
}

class SaveStateManagerTest {
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var logger: ObsidianLogger
    
    private lateinit var saveStateManager: SaveStateManager
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(context.getExternalFilesDir(null)).thenReturn(File("/test"))
        
        saveStateManager = SaveStateManager(context, logger)
    }
    
    @Test
    fun `createSpeedrunProfile creates valid profile`() {
        // When
        val profile = saveStateManager.createSpeedrunProfile(
            gameName = "The Legend of Zelda",
            maxSaveStates = 15
        )
        
        // Then
        assertNotNull(profile)
        assertEquals("The Legend of Zelda", profile.gameName)
        assertEquals(15, profile.maxSaveStates)
        assertEquals(0, profile.saveStates.size)
    }
    
    @Test
    fun `getSpeedrunProfile returns created profile`() {
        // Given
        val profile = saveStateManager.createSpeedrunProfile("Test Game", 10)
        
        // When
        val retrieved = saveStateManager.getSpeedrunProfile("Test Game")
        
        // Then
        assertNotNull(retrieved)
        assertEquals(profile.gameName, retrieved?.gameName)
    }
}

class PlayGamesCloudSyncTest {
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var logger: ObsidianLogger
    
    private lateinit var playGamesSync: PlayGamesCloudSync
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        playGamesSync = PlayGamesCloudSync(context, logger)
    }
    
    @Test
    fun `authenticate updates authentication state`() = runTest {
        // When
        val result = playGamesSync.authenticate()
        
        // Then
        assertTrue(result)
        assertTrue(playGamesSync.isAuthenticated.value)
    }
}

class RomScannerTest {
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var logger: ObsidianLogger
    
    private lateinit var romScanner: RomScanner
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        romScanner = RomScanner(context, logger)
    }
    
    @Test
    fun `extractGameInfo parses game name correctly`() = runTest {
        // Given
        val testFile = File("/test/roms/Super Mario 64 (USA).z64")
        val emulator = DetectedEmulator(
            type = EmulatorType.RETROARCH,
            packageName = "com.retroarch",
            name = "RetroArch",
            version = "1.0",
            isInstalled = true,
            savePaths = emptyList(),
            saveStatePaths = emptyList(),
            romPaths = emptyList(),
            supportedPlatforms = listOf("Nintendo 64"),
            installedApkPath = ""
        )
        
        // When
        val gameInfo = romScanner.extractGameInfo(testFile, emulator)
        
        // Then
        assertEquals("Super Mario 64", gameInfo.name)
        assertEquals("USA", gameInfo.region)
        assertEquals("Nintendo 64", gameInfo.platform)
    }
}
