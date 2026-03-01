# Gaming Backup Features - Quick Start Guide

## Installation

The gaming backup features are already integrated into ObsidianBackup. No additional installation required!

## First Time Setup

### 1. Enable Gaming Features

Navigate to **Settings** → **Features** and ensure "Gaming Backup" is enabled.

### 2. Grant Permissions

The app needs storage permissions to access emulator data:
- **Storage Access**: For reading/writing save files
- **All Files Access** (Android 11+): For accessing emulator directories

### 3. Install Emulators

Install your preferred emulators from Google Play:
- RetroArch (Multi-platform)
- Dolphin (GameCube/Wii)
- PPSSPP (PSP)
- DraStic (Nintendo DS)
- Citra (3DS)
- AetherSX2 (PS2)

## Quick Start Scenarios

### Scenario 1: Backup All Game Saves

**Goal**: Backup all saves from your emulators

1. Open ObsidianBackup
2. Navigate to **Gaming** tab
3. Tap **Scan for Emulators**
4. Select an emulator from the list
5. Choose backup options:
   - ✅ Save Files
   - ✅ Save States
   - ☐ ROM Files (optional)
   - ☐ Cloud Sync (optional)
6. Tap **Start Backup**

**Result**: All game saves are backed up to `/storage/emulated/0/Android/data/com.obsidianbackup/files/gaming_backups/`

### Scenario 2: Cloud Sync Game Saves

**Goal**: Sync your Pokemon save to the cloud

1. Navigate to **Gaming** → **Cloud Sync**
2. Tap **Sign in with Google Play Games**
3. Authorize the app
4. Select games to sync
5. Tap **Sync to Cloud**

**Result**: Your saves are uploaded to Google Play Games Services and will sync across devices.

### Scenario 3: Speedrun Mode

**Goal**: Create quick save states for speedrunning

1. Navigate to **Gaming** → **Speedrun Mode**
2. Tap **Create Profile**
3. Enter game name: "Super Mario 64"
4. Set max save states: 10
5. Tap **Create**

**Usage during gameplay**:
- Press the **Quick Save** button to create a save state
- Tap any save state to load it
- Swipe to delete old states

### Scenario 4: Multi-Profile Management

**Goal**: Maintain separate saves for different family members

1. Backup your current save
2. Restore to profile slot 1
3. Start new game
4. Backup new save to profile slot 2
5. Switch between profiles by restoring different slots

### Scenario 5: Backup Before ROM Hack

**Goal**: Backup your saves before installing a ROM hack

1. Navigate to **Gaming**
2. Select your emulator
3. Enable all options:
   - ✅ Save Files
   - ✅ Save States
   - ✅ ROM Files
4. Create backup with label: "Before ROM Hack"
5. Install your ROM hack
6. If issues occur, restore from backup

## UI Walkthrough

### Main Gaming Screen

```
┌─────────────────────────────────┐
│  Gaming Backup          [Scan]  │
├─────────────────────────────────┤
│                                 │
│  📱 RetroArch              →    │
│     Version 1.14.0             │
│     NES, SNES, Genesis...      │
│                                 │
│  🎮 Dolphin Emulator       →    │
│     Version 5.0-19812          │
│     GameCube, Wii              │
│                                 │
│  🎯 PPSSPP                 →    │
│     Version 1.15.4             │
│     PlayStation Portable       │
│                                 │
└─────────────────────────────────┘
                [💾 Backup Games]
```

### Speedrun Mode Screen

```
┌─────────────────────────────────┐
│  Speedrun Mode                  │
├─────────────────────────────────┤
│  🏃 Super Mario 64              │
│     5/10 save states            │
│  [Quick Save]  [Export]         │
├─────────────────────────────────┤
│  Save States                    │
│                                 │
│  🕐 16:32:15 - Deku Tree Skip   │
│                      [▶] [🗑]   │
│                                 │
│  🕐 16:30:42 - Forest Temple    │
│                      [▶] [🗑]   │
│                                 │
│  🕐 16:28:19 - Kakariko Skip    │
│                      [▶] [🗑]   │
│                                 │
└─────────────────────────────────┘
                       [➕]
```

## Code Examples

### Basic Backup

```kotlin
// Initialize
val gamingBackupManager: GamingBackupManager = // Injected via Hilt

// Scan for emulators
val emulators = gamingBackupManager.scanForEmulators()

// Select emulator
val retroarch = emulators.first { it.type == EmulatorType.RETROARCH }

// Define games to backup
val games = listOf(
    GameInfo(
        name = "Pokemon Red",
        platform = "Game Boy",
        romPath = "/sdcard/RetroArch/roms/gb/pokemon_red.gb"
    )
)

// Configure options
val options = BackupOptions(
    includeSaves = true,
    includeRoms = false,
    includeSaveStates = true,
    cloudSync = false,
    compression = true
)

// Perform backup
val result = gamingBackupManager.backupGameSaves(retroarch, games, options)

// Check result
if (result.successfulBackups == games.size) {
    println("✅ Backup completed successfully!")
    println("Backup ID: ${result.backupId}")
    println("Location: ${result.backupPath}")
} else {
    println("❌ Some backups failed")
    result.results.filter { !it.success }.forEach {
        println("Failed: ${it.gameName} - ${it.error}")
    }
}
```

### Cloud Sync

```kotlin
val playGamesSync: PlayGamesCloudSync = // Injected

// Authenticate
if (playGamesSync.authenticate()) {
    // Upload save
    val uploaded = playGamesSync.uploadSaveData(
        gameName = "The Legend of Zelda",
        saveFile = File("/sdcard/RetroArch/saves/zelda.srm"),
        metadata = mapOf(
            "hearts" to "20",
            "triforces" to "8",
            "platform" to "NES"
        )
    )
    
    if (uploaded) {
        println("☁️ Synced to cloud!")
    }
}

// Later, on another device...
val downloaded = playGamesSync.downloadSaveData(
    gameName = "The Legend of Zelda",
    destinationFile = File("/sdcard/RetroArch/saves/zelda.srm")
)

if (downloaded) {
    println("⬇️ Downloaded from cloud!")
}
```

### Speedrun Quick Save

```kotlin
val saveStateManager: SaveStateManager = // Injected

// Create profile
val profile = saveStateManager.createSpeedrunProfile(
    gameName = "Super Mario 64",
    maxSaveStates = 10
)

// During gameplay - create quick save
val saveState = saveStateManager.createQuickSave(
    emulator = retroarch,
    gameName = "Super Mario 64",
    label = "Before Bowser"
)

println("💾 Quick save created: ${saveState.label}")

// Load save state
saveStateManager.loadSaveState(saveState)
println("📂 Save state loaded!")

// Delete save state
saveStateManager.deleteSaveState(saveState)
println("🗑️ Save state deleted")
```

### Restore Backup

```kotlin
// Get backup history
val backups = gamingBackupManager.getBackupHistory()

// Find specific backup
val latestBackup = backups.maxByOrNull { it.timestamp }

if (latestBackup != null) {
    // Restore specific game
    val result = gamingBackupManager.restoreGameSave(
        backupId = latestBackup.backupId,
        gameName = "Pokemon Red",
        profileSlot = 0  // Main profile
    )
    
    if (result.success) {
        println("✅ Restore completed!")
    } else {
        println("❌ Restore failed: ${result.message}")
    }
}
```

## Testing

### Manual Testing Checklist

- [ ] Install RetroArch
- [ ] Create a save file in a game
- [ ] Open ObsidianBackup → Gaming
- [ ] Tap "Scan for Emulators"
- [ ] Verify RetroArch appears in list
- [ ] Tap RetroArch
- [ ] Enable all backup options
- [ ] Tap "Start Backup"
- [ ] Verify progress indicator
- [ ] Check backup completed successfully
- [ ] Delete original save file
- [ ] Restore from backup
- [ ] Verify save file is restored correctly

### Automated Testing

```kotlin
@Test
fun `backup creates valid archive`() = runTest {
    val manager = GamingBackupManager(...)
    
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
    
    val result = manager.backupGameSaves(
        emulator = emulator,
        games = listOf(game),
        options = BackupOptions()
    )
    
    assertTrue(result.successfulBackups == 1)
    assertTrue(File(result.backupPath).exists())
}
```

## Troubleshooting

### Issue: No Emulators Detected

**Cause**: Emulators not installed or different package names

**Fix**:
1. Verify emulators are installed
2. Check package names match expected values
3. Grant storage permissions

### Issue: Save Files Not Found

**Cause**: Non-standard save paths or permissions

**Fix**:
1. Check emulator settings for custom paths
2. Grant "All Files Access" permission
3. Try manual path specification

### Issue: Cloud Sync Fails

**Cause**: Not authenticated or network issues

**Fix**:
1. Sign in to Google Play Games
2. Check internet connection
3. Verify app has Play Games permission

### Issue: Backup Too Large

**Cause**: ROM files or uncompressed saves

**Fix**:
1. Disable ROM backup
2. Enable compression
3. Clean up old save states

## Best Practices

### Daily Workflow
1. **Morning**: Start gaming session
2. **During gameplay**: Use speedrun mode for quick saves
3. **Evening**: Full backup with cloud sync

### Before Major Changes
- Always create a full backup (saves + ROMs)
- Label backups descriptively
- Test restore on non-critical save first

### Cloud Sync Strategy
- Enable auto-sync for important games
- Use Wi-Fi for large uploads
- Keep local backups as fallback

### Storage Management
- Set retention policy (e.g., keep last 30 days)
- Archive old backups to external storage
- Clean up speedrun profiles regularly

## Advanced Usage

### Custom Emulator Support

Add support for unsupported emulators:

```kotlin
// In EmulatorDetector.kt, add to EMULATOR_CONFIGS:
EmulatorConfig(
    type = EmulatorType.CUSTOM,
    packageName = "com.example.emulator",
    name = "My Custom Emulator",
    savePaths = listOf("/sdcard/MyEmulator/saves/{game}.sav"),
    saveStatePaths = listOf("/sdcard/MyEmulator/states/{game}*.state"),
    romPaths = listOf("/sdcard/MyEmulator/roms"),
    supportedPlatforms = listOf("Custom Platform")
)
```

### Batch Operations

```kotlin
// Backup multiple emulators at once
val emulators = gamingBackupManager.scanForEmulators()

emulators.forEach { emulator ->
    launch {
        val games = // Scan for games
        gamingBackupManager.backupGameSaves(
            emulator = emulator,
            games = games,
            options = BackupOptions(cloudSync = true)
        )
    }
}
```

### Scheduled Backups

```kotlin
// Set up WorkManager for automatic backups
val workRequest = PeriodicWorkRequestBuilder<GamingBackupWorker>(
    repeatInterval = 1,
    repeatIntervalTimeUnit = TimeUnit.DAYS
).setConstraints(
    Constraints.Builder()
        .setRequiresCharging(true)
        .setRequiresBatteryNotLow(true)
        .build()
).build()

WorkManager.getInstance(context).enqueue(workRequest)
```

## Resources

- **Documentation**: See GAMING_FEATURES.md for full API reference
- **Sample App**: Check `app/src/main/java/com/obsidianbackup/ui/screens/` for UI examples
- **Tests**: See `app/src/test/` for unit tests

## Support

Need help? Contact:
- GitHub Issues: Report bugs and feature requests
- Discord: Real-time community support
- Email: support@obsidianbackup.com

---

**Happy Gaming! 🎮**
