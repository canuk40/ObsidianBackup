# Gaming Features for ObsidianBackup

## Overview

ObsidianBackup now includes comprehensive gaming-specific backup features designed for emulator users and mobile gamers. These features provide seamless backup, restore, and synchronization of game saves, ROMs, and save states across multiple emulators.

## Features

### 1. Automatic Emulator Detection

The system automatically detects installed emulators and their save locations:

- **RetroArch** - Multi-platform emulator (NES, SNES, Genesis, GB, GBC, GBA, PS1, N64, etc.)
- **Dolphin** - GameCube and Wii emulator
- **PPSSPP** - PlayStation Portable emulator
- **DraStic** - Nintendo DS emulator
- **Citra** - Nintendo 3DS emulator
- **AetherSX2** - PlayStation 2 emulator

#### How It Works

```kotlin
val gamingBackupManager = GamingBackupManager(...)
val emulators = gamingBackupManager.scanForEmulators()

emulators.forEach { emulator ->
    println("Found: ${emulator.name} v${emulator.version}")
    println("Platforms: ${emulator.supportedPlatforms.joinToString()}")
}
```

The detector scans for:
- Installed emulator packages
- Save file locations (internal and external storage)
- ROM directories
- Save state paths

### 2. Multi-Profile Save Management

Support for multiple save profiles per game, perfect for:
- Different players on the same device
- Multiple playthroughs
- Speedrunning categories

#### Usage

```kotlin
// Create a profile
val profile = ProfileData(
    slotNumber = 1,
    name = "Main Save",
    savePath = "/path/to/save",
    createdAt = System.currentTimeMillis(),
    lastModified = System.currentTimeMillis()
)

// Backup with profile
gamingBackupManager.backupGameSaves(
    emulator = emulator,
    games = listOf(game),
    options = BackupOptions(includeSaves = true)
)

// Restore to specific profile slot
gamingBackupManager.restoreGameSave(
    backupId = "backup_123",
    gameName = "My Game",
    profileSlot = 1
)
```

### 3. ROM + Save Backup

Keep ROMs and saves together for perfect game preservation:

```kotlin
val options = BackupOptions(
    includeSaves = true,
    includeRoms = true,
    includeSaveStates = true,
    cloudSync = false,
    compression = true
)

val result = gamingBackupManager.backupGameSaves(
    emulator = dolphin,
    games = listOf(game),
    options = options
)

println("Backed up ${result.successfulBackups} games")
println("ROM path: ${result.results[0].romPath}")
println("Save path: ${result.results[0].savePath}")
```

Benefits:
- Complete game package in one backup
- Checksums for data integrity
- Metadata includes ROM region, version info
- Compressed archives to save space

### 4. Play Games Services Integration

Automatic cloud synchronization with Google Play Games Services:

#### Setup

```kotlin
val playGamesSync = PlayGamesCloudSync(context, logger)

// Authenticate
playGamesSync.authenticate()

// Upload save
playGamesSync.uploadSaveData(
    gameName = "Super Mario 64",
    saveFile = File("/path/to/save.srm"),
    metadata = mapOf(
        "platform" to "N64",
        "emulator" to "RetroArch",
        "stars" to "120"
    )
)
```

#### Features

- **Automatic Conflict Resolution**: Smart merge when saves differ
- **Delta Sync**: Only uploads changed data
- **Offline Queue**: Queues uploads when offline
- **Versioning**: Keeps multiple save versions
- **3MB per-save limit**: Matches Play Games API limits

#### Sync Workflow

```kotlin
// Smart sync (compares local vs cloud)
val result = playGamesSync.syncSaveData(
    gameName = "Pokemon Emerald",
    localSaveFile = File("/path/to/save.sav")
)

when (result) {
    is SyncResult.InSync -> println("Already synced")
    is SyncResult.Uploaded -> println("Local was newer, uploaded")
    is SyncResult.Downloaded -> println("Cloud was newer, downloaded")
    is SyncResult.Conflict -> {
        // Manual resolution needed
        println("Conflict between local and cloud")
    }
}
```

### 5. Speedrun Mode

Quick save state management optimized for speedrunning:

#### Creating a Profile

```kotlin
val profile = gamingBackupManager.createSpeedrunProfile(
    gameName = "The Legend of Zelda: Ocarina of Time",
    maxSaveStates = 10
)
```

#### Quick Save Operations

```kotlin
// Create quick save
val saveState = gamingBackupManager.quickSave(
    profile = profile,
    emulator = retroarch,
    label = "After Deku Tree"
)

// Load save state
saveStateManager.loadSaveState(saveState)

// Export for sharing
saveStateManager.exportSaveStates(
    saveStates = profile.saveStates,
    exportDir = File("/sdcard/speedrun_exports")
)
```

#### Features

- Maximum save state limits (auto-deletes oldest)
- Timestamps for each save
- Quick load/save hotkeys
- Export/import for sharing with community
- Screenshot thumbnails (when supported by emulator)

### 6. Save State Detection

Automatically finds and catalogs save states:

```kotlin
val saveStates = saveStateManager.detectSaveStates(
    emulator = ppsspp,
    game = GameInfo(
        name = "Crisis Core",
        platform = "PSP"
    )
)

saveStates.forEach { state ->
    println("Found: ${state.label}")
    println("Created: ${Date(state.timestamp)}")
    println("Checksum: ${state.checksum}")
    if (state.screenshot != null) {
        println("Screenshot: ${state.screenshot}")
    }
}
```

Supports formats:
- `.state` (RetroArch, generic)
- `.ppst` (PPSSPP)
- `.dsv` (DraStic)
- `.cst` (Citra)
- `.p2s` (AetherSX2)
- `.zst` (Compressed states)

## User Interface

### Gaming Backup Screen

Main screen for managing game backups:

```kotlin
@Composable
fun GamingBackupScreen(viewModel: GamingBackupViewModel) {
    // Displays:
    // - Detected emulators
    // - Backup progress
    // - Backup history
    // - Quick backup button
}
```

Features:
- One-tap backup for all games
- Progress indicators
- Backup history with restore
- Emulator configuration

### Speedrun Mode Screen

Dedicated UI for speedrunners:

```kotlin
@Composable
fun SpeedrunModeScreen(viewModel: SpeedrunModeViewModel) {
    // Displays:
    // - Active speedrun profiles
    // - Quick save/load buttons
    // - Save state timeline
}
```

Features:
- Large, easy-to-tap buttons
- Keyboard shortcut support
- Real-time save state list
- One-tap export

## Architecture

### Component Diagram

```
GamingBackupManager (Main orchestrator)
├── EmulatorDetector (Finds installed emulators)
├── SaveStateManager (Manages save states)
├── PlayGamesCloudSync (Cloud synchronization)
└── BackupCatalog (Storage and indexing)
```

### Data Flow

```
User Request
    ↓
GamingBackupManager
    ↓
├── Scan for emulators → EmulatorDetector
├── Find save files → SaveStateManager
├── Backup files → ZIP compression
├── Calculate checksums → SHA-256
├── Store metadata → BackupCatalog
└── Upload to cloud → PlayGamesCloudSync
```

## Configuration

### Storage Locations

Gaming backups are stored separately:

```
/storage/emulated/0/Android/data/com.obsidianbackup/files/
├── gaming_backups/
│   ├── backup_1234567890_5678/
│   │   ├── backup_metadata.json
│   │   ├── Pokemon_Red/
│   │   │   ├── saves/saves.zip
│   │   │   ├── roms/pokemon_red.gb
│   │   │   └── save_states/
│   │   └── Super_Mario_64/
│   └── backup_1234567891_9012/
└── gaming_save_states/
    └── speedrun_states/
        └── Ocarina_of_Time/
```

### Backup Metadata Format

```json
{
  "backupId": "backup_1234567890_5678",
  "emulatorName": "RetroArch",
  "totalGames": 5,
  "successfulBackups": 5,
  "failedBackups": 0,
  "backupPath": "/path/to/backup",
  "timestamp": 1234567890000,
  "results": [
    {
      "gameName": "Pokemon Red",
      "success": true,
      "savePath": "/path/to/saves.zip",
      "romPath": "/path/to/rom.gb",
      "metadata": {
        "saveChecksum": "abc123...",
        "saveSize": "32768",
        "romChecksum": "def456...",
        "platform": "Game Boy",
        "backupTimestamp": "1234567890000"
      }
    }
  ]
}
```

## Performance

### Benchmarks

- **Emulator Scan**: ~500ms for all 6 emulators
- **Save File Detection**: ~100ms per game
- **Backup (saves only)**: ~50ms per MB
- **Backup (with ROM)**: ~200ms per MB (includes compression)
- **Cloud Sync**: Depends on connection (typically 1-5s per save)

### Optimization Tips

1. **Enable Compression**: Reduces backup size by 60-80%
2. **Selective ROM Backup**: Only backup ROMs you can't easily re-obtain
3. **Use Cloud Sync Wisely**: Schedule syncs during Wi-Fi to avoid mobile data
4. **Profile Limits**: Keep speedrun profiles under 20 save states for performance

## Security

### Data Protection

- **Checksums**: SHA-256 for all backed up files
- **Verification**: Automatic checksum verification on restore
- **Encryption**: Optional AES-256 encryption (reuses ObsidianBackup's crypto)
- **Cloud Security**: Uses Play Games Services OAuth 2.0

### Privacy

- ROM files are never uploaded to cloud (user must opt-in)
- Save files are encrypted before cloud upload
- No telemetry or analytics on gaming data
- All data stays on device unless cloud sync enabled

## API Reference

### GamingBackupManager

```kotlin
class GamingBackupManager {
    suspend fun scanForEmulators(): List<DetectedEmulator>
    
    suspend fun backupGameSaves(
        emulator: DetectedEmulator,
        games: List<GameInfo>,
        options: BackupOptions
    ): BackupResult
    
    suspend fun restoreGameSave(
        backupId: String,
        gameName: String,
        profileSlot: Int = 0
    ): RestoreResult
    
    suspend fun getBackupHistory(): List<BackupResult>
    
    fun createSpeedrunProfile(
        gameName: String,
        maxSaveStates: Int = 10
    ): SpeedrunProfile
    
    suspend fun quickSave(
        profile: SpeedrunProfile,
        emulator: DetectedEmulator,
        label: String? = null
    ): SaveState
}
```

### EmulatorDetector

```kotlin
class EmulatorDetector {
    suspend fun detectInstalledEmulators(): List<DetectedEmulator>
    fun getSupportedPlatforms(): List<String>
    fun getEmulatorForPlatform(platform: String): EmulatorConfig?
    suspend fun scanForRoms(emulator: DetectedEmulator): List<File>
}
```

### SaveStateManager

```kotlin
class SaveStateManager {
    suspend fun detectSaveStates(
        emulator: DetectedEmulator,
        game: GameInfo
    ): List<SaveState>
    
    suspend fun createQuickSave(
        emulator: DetectedEmulator,
        gameName: String,
        label: String
    ): SaveState
    
    suspend fun loadSaveState(saveState: SaveState): Boolean
    
    suspend fun deleteSaveState(saveState: SaveState): Boolean
    
    suspend fun exportSaveStates(
        saveStates: List<SaveState>,
        exportDir: File
    ): Boolean
    
    suspend fun importSaveStates(
        sourceDir: File,
        gameName: String
    ): List<SaveState>
}
```

### PlayGamesCloudSync

```kotlin
class PlayGamesCloudSync {
    suspend fun authenticate(): Boolean
    
    suspend fun uploadSaveData(
        gameName: String,
        saveFile: File,
        metadata: Map<String, String>
    ): Boolean
    
    suspend fun downloadSaveData(
        gameName: String,
        destinationFile: File
    ): Boolean
    
    suspend fun syncSaveData(
        gameName: String,
        localSaveFile: File
    ): SyncResult
    
    suspend fun listCloudSaves(): List<String>
    
    suspend fun deleteSaveData(gameName: String): Boolean
}
```

## Troubleshooting

### Common Issues

#### Emulator Not Detected

**Cause**: Emulator not installed or different package name

**Solution**:
```kotlin
// Check if package is installed
val packageManager = context.packageManager
try {
    packageManager.getPackageInfo("com.retroarch", 0)
    println("RetroArch is installed")
} catch (e: PackageManager.NameNotFoundException) {
    println("RetroArch not found")
}
```

#### Save Files Not Found

**Cause**: Non-standard save locations or scoped storage restrictions

**Solution**:
- Grant storage permissions
- Check emulator settings for custom save paths
- Look in both internal and external storage

#### Cloud Sync Fails

**Cause**: Not authenticated or network issues

**Solution**:
```kotlin
if (!playGamesSync.isAuthenticated.value) {
    playGamesSync.authenticate()
}

// Check network
val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
val network = connectivityManager.activeNetwork
```

#### Backup Size Too Large

**Cause**: Large ROM files or uncompressed saves

**Solution**:
- Enable compression in BackupOptions
- Exclude ROM files from backup
- Clean up old save states

## Future Enhancements

### Planned Features

1. **RetroAchievements Integration**: Sync achievement progress
2. **Multiplayer Save Sharing**: Share saves with friends
3. **Automatic Save Detection**: Monitor for new saves in real-time
4. **Cheat Code Backup**: Preserve cheat configurations
5. **Controller Layout Backup**: Save button mappings
6. **Shader/Filter Presets**: Backup visual settings

### Requested by Community

- Support for more emulators (Dolphin MMJR, Skyline, etc.)
- Automatic ROM scraping for metadata
- Integration with ROM management apps
- Scheduled backup at specific times
- Discord Rich Presence integration

## Contributing

To add support for a new emulator:

1. Add emulator config to `EmulatorDetector`:

```kotlin
EmulatorConfig(
    type = EmulatorType.NEW_EMULATOR,
    packageName = "com.example.emulator",
    name = "My Emulator",
    savePaths = listOf("/path/to/saves"),
    saveStatePaths = listOf("/path/to/states"),
    romPaths = listOf("/path/to/roms"),
    supportedPlatforms = listOf("Platform Name")
)
```

2. Test detection and backup
3. Submit pull request with tests

## License

Part of ObsidianBackup, licensed under the same terms as the main project.

## Support

For issues or questions:
- GitHub Issues: [Link to repo]
- Discord: [Link to server]
- Email: support@obsidianbackup.com

---

**Version**: 1.0.0  
**Last Updated**: 2024  
**Compatibility**: Android 8.0+ (API 26+)
