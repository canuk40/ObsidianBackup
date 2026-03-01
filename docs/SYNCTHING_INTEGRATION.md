# Syncthing Integration for ObsidianBackup

## Overview

Complete Syncthing integration for cross-device backup synchronization in ObsidianBackup. This implementation provides native Syncthing support with a comprehensive UI for managing devices, folders, and resolving conflicts.

## Features

### 1. **Native Syncthing Integration**
- Direct integration with Syncthing REST API
- No external app dependencies
- Embedded Syncthing process management
- Automatic service lifecycle management

### 2. **Device Management**
- Auto-discovery of nearby devices
- QR code-based device pairing
- Manual device ID entry
- Device connection status monitoring
- Device removal and management

### 3. **Folder Synchronization**
- Share backup directories across devices
- Multiple folder support
- Folder-level sync options:
  - Send/Receive (bidirectional)
  - Send-only
  - Receive-only
- File system watcher support
- Automatic rescan intervals

### 4. **Conflict Resolution**
- Automatic conflict detection
- Manual reconciliation UI with 4 strategies:
  - **Keep Local**: Discard remote changes
  - **Keep Remote**: Discard local changes
  - **Keep Both**: Save both versions with different names
  - **Manual Merge**: Create .local and .remote backups for user merging
- File comparison view (size, modification time)
- Visual conflict indicators

### 5. **Real-time Sync Status**
- Live upload/download rate display
- Per-folder sync progress
- Global sync statistics
- Connection status for each device
- Bytes synced/remaining indicators

### 6. **Network Preferences**
- **WiFi Only**: Sync only on WiFi networks
- **WiFi & Mobile Data**: Sync on any connection
- **Always**: Sync regardless of network
- Automatic folder pause/resume based on preference

### 7. **File Versioning**
- Multiple versioning strategies:
  - Simple (keep N versions)
  - Staggered (time-based retention)
  - Trash can (deleted files recovery)
  - External versioning
- Configurable per folder

## Architecture

### Core Components

```
sync/
├── SyncthingManager.kt           # Main integration manager
├── SyncthingApiClient.kt         # REST API client
├── SyncthingConflictResolver.kt  # Conflict resolution logic
└── models/
    └── SyncthingModels.kt        # Data models

ui/screens/syncthing/
├── SyncthingScreen.kt            # Main settings screen
├── DevicePairingScreen.kt        # Device pairing UI
└── ConflictResolutionScreen.kt   # Conflict resolution UI
```

### State Management

All state is managed through Kotlin StateFlow for reactive UI updates:

```kotlin
val syncState: StateFlow<SyncthingState>        // Connection state
val devices: StateFlow<List<SyncthingDevice>>   // Paired devices
val folders: StateFlow<List<SyncthingFolder>>   // Shared folders
val syncStatus: StateFlow<SyncStatus>           // Real-time sync status
val conflicts: StateFlow<List<SyncConflict>>    // Active conflicts
val networkPreference: StateFlow<NetworkPreference> // Network settings
```

## Usage

### 1. Initialize Syncthing

```kotlin
@Inject
lateinit var syncthingManager: SyncthingManager

// Initialize in Application or Activity
lifecycleScope.launch {
    val result = syncthingManager.initialize()
    result.onSuccess {
        // Syncthing is ready
    }.onFailure { e ->
        // Handle initialization error
    }
}
```

### 2. Add a Device

```kotlin
// Using QR code
val qrCodeData = syncthingManager.generatePairingQRCode()
// Display QR code to other device

// Or parse scanned QR code
val pairingInfo = syncthingManager.parsePairingQRCode(scannedData)
pairingInfo?.let { (deviceId, deviceName) ->
    syncthingManager.addDevice(deviceId, deviceName)
}

// Or manual entry
syncthingManager.addDevice(
    deviceId = "ABCD123-EFGH456",
    name = "My Tablet"
)
```

### 3. Share a Folder

```kotlin
val result = syncthingManager.shareFolder(
    folderPath = "/storage/emulated/0/ObsidianBackup/backups",
    folderLabel = "Obsidian Backups",
    deviceIds = listOf("DEVICE-ID-1", "DEVICE-ID-2"),
    syncOptions = FolderSyncOptions(
        folderType = FolderType.SEND_RECEIVE,
        rescanIntervalS = 3600,
        fsWatcherEnabled = true,
        ignorePerms = false,
        versioning = VersioningConfig(
            type = VersioningType.SIMPLE,
            params = mapOf("keep" to "5")
        )
    )
)
```

### 4. Monitor Sync Status

```kotlin
syncthingManager.syncStatus.collect { status ->
    when (status) {
        is SyncStatus.Idle -> {
            // No active sync
        }
        is SyncStatus.Syncing -> {
            // Display upload/download rates
            val uploadRate = status.uploadRate
            val downloadRate = status.downloadRate
            
            // Per-folder progress
            status.folders.forEach { (folderId, info) ->
                val completion = info.completion
                val remaining = info.needBytes
            }
        }
    }
}
```

### 5. Resolve Conflicts

```kotlin
syncthingManager.conflicts.collect { conflicts ->
    conflicts.forEach { conflict ->
        // Display conflict to user
        // User selects resolution strategy
        syncthingManager.resolveConflict(
            conflictId = conflict.id,
            resolution = ConflictResolution.KEEP_LOCAL
        )
    }
}
```

### 6. Set Network Preference

```kotlin
syncthingManager.setNetworkPreference(NetworkPreference.WIFI_ONLY)
```

## API Reference

### SyncthingManager

#### Methods

- `suspend fun initialize(): Result<Unit>`
  - Initialize Syncthing service
  
- `suspend fun shutdown()`
  - Gracefully shutdown Syncthing
  
- `suspend fun addDevice(deviceId: String, name: String, introducer: Boolean = false): Result<SyncthingDevice>`
  - Add a device for synchronization
  
- `suspend fun removeDevice(deviceId: String): Result<Unit>`
  - Remove a paired device
  
- `suspend fun shareFolder(folderPath: String, folderLabel: String, deviceIds: List<String>, syncOptions: FolderSyncOptions): Result<SyncthingFolder>`
  - Share a folder with devices
  
- `suspend fun unshareFolder(folderId: String): Result<Unit>`
  - Stop sharing a folder
  
- `suspend fun startDiscovery(): Flow<DiscoveredDevice>`
  - Start device discovery
  
- `fun stopDiscovery()`
  - Stop device discovery
  
- `fun generatePairingQRCode(): String`
  - Generate QR code data for pairing
  
- `fun parsePairingQRCode(qrData: String): PairingInfo?`
  - Parse QR code from another device
  
- `suspend fun setNetworkPreference(preference: NetworkPreference)`
  - Set network usage preference
  
- `suspend fun resolveConflict(conflictId: String, resolution: ConflictResolution): Result<Unit>`
  - Resolve a sync conflict
  
- `suspend fun getSyncStatistics(): SyncStatistics`
  - Get detailed sync statistics

## UI Screens

### SyncthingScreen

Main settings screen showing:
- Connection status
- List of paired devices
- Shared folders with sync progress
- Network preference settings
- Conflict alerts

### DevicePairingScreen

Device pairing interface with:
- QR code display (for scanning by other devices)
- QR code scanner (to scan other devices)
- Manual device ID entry
- Device name input

### ConflictResolutionScreen

Conflict resolution interface with:
- List of all conflicts
- File information comparison
- Resolution options (Keep Local, Keep Remote, Keep Both, Manual Merge)
- Confirmation dialogs

## Configuration

### AndroidManifest.xml Permissions

Add these permissions for Syncthing:

```xml
<!-- Required for Syncthing -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

<!-- Optional: For QR code scanning -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

### Build Dependencies

Already added in `app/build.gradle.kts`:

```kotlin
// QR Code generation and scanning (ZXing)
implementation("com.google.zxing:core:3.5.2")
implementation("com.journeyapps:zxing-android-embedded:4.3.0")
```

## Data Models

### SyncthingState

```kotlin
sealed class SyncthingState {
    object Disconnected : SyncthingState()
    data class Connected(val deviceId: String) : SyncthingState()
    data class Error(val message: String) : SyncthingState()
}
```

### SyncthingDevice

```kotlin
data class SyncthingDevice(
    val deviceId: String,
    val name: String,
    val connected: Boolean,
    val introducer: Boolean,
    val compression: CompressionMethod,
    val addresses: List<String>
)
```

### SyncthingFolder

```kotlin
data class SyncthingFolder(
    val id: String,
    val label: String,
    val path: String,
    val devices: List<String>,
    val type: FolderType,
    val rescanIntervalS: Int,
    val fsWatcherEnabled: Boolean,
    val ignorePerms: Boolean,
    val versioning: VersioningConfig?,
    val paused: Boolean
)
```

### SyncConflict

```kotlin
data class SyncConflict(
    val id: String,
    val filePath: String,
    val localVersion: String,
    val remoteVersion: String,
    val localModified: Long,
    val remoteModified: Long,
    val localSize: Long,
    val remoteSize: Long
)
```

## Testing

### Manual Testing Checklist

1. **Initialization**
   - [ ] Syncthing service starts successfully
   - [ ] Device ID is generated and displayed
   - [ ] API becomes available within timeout

2. **Device Pairing**
   - [ ] QR code is generated correctly
   - [ ] QR code can be scanned by another device
   - [ ] Manual device ID entry works
   - [ ] Device appears in device list after pairing

3. **Folder Sharing**
   - [ ] Folder can be shared with devices
   - [ ] Sync options are applied correctly
   - [ ] Folder appears on remote device
   - [ ] Files sync bidirectionally

4. **Conflict Resolution**
   - [ ] Conflicts are detected automatically
   - [ ] All 4 resolution strategies work:
     - Keep Local
     - Keep Remote
     - Keep Both
     - Manual Merge
   - [ ] Conflict disappears after resolution

5. **Network Preferences**
   - [ ] WiFi Only: Sync pauses on mobile data
   - [ ] WiFi Only: Sync resumes on WiFi
   - [ ] WiFi & Mobile Data: Sync on both networks
   - [ ] Always: Sync regardless of network

6. **Sync Status**
   - [ ] Upload/download rates update in real-time
   - [ ] Per-folder progress displays correctly
   - [ ] Connection status shows for each device

## Security Considerations

1. **Device Authentication**
   - Devices are authenticated using Syncthing's device ID system
   - Each device has a unique cryptographic certificate
   - Device IDs are 52-character base32-encoded strings

2. **Data Encryption**
   - All data is encrypted in transit using TLS
   - Syncthing uses TLS 1.3 with perfect forward secrecy
   - No plaintext data is transmitted

3. **Local Storage**
   - API keys stored in private SharedPreferences
   - Syncthing home directory in app-private storage
   - Configuration files protected by Android sandbox

4. **Network Security**
   - Local discovery uses multicast DNS
   - Global discovery uses HTTPS with certificate pinning
   - All connections verified using device certificates

## Performance Optimization

1. **Sync Intervals**
   - Default rescan interval: 3600 seconds (1 hour)
   - File system watcher for instant detection
   - Configurable per folder

2. **Network Optimization**
   - Compression: METADATA (compress only metadata)
   - Connection limits configurable
   - Rate limiting support

3. **Battery Optimization**
   - Respects Android Doze mode
   - WorkManager for scheduled syncs
   - Network preference to reduce mobile data usage

## Troubleshooting

### Syncthing Won't Start

1. Check if native library exists: `libsyncthing.so`
2. Verify storage permissions
3. Check logs for initialization errors
4. Ensure API port (8384) is available

### Device Not Discovered

1. Ensure both devices on same network
2. Check WiFi network allows multicast/mDNS
3. Enable global discovery if on different networks
4. Verify device IDs are correct

### Sync Not Working

1. Check device connection status
2. Verify folder is shared with correct devices
3. Check network preference settings
4. Look for conflicts that need resolution
5. Verify file permissions

### Conflicts Constantly Appearing

1. Check for clock synchronization issues
2. Verify file system supports timestamps
3. Check for file permission mismatches
4. Consider using "Ignore Permissions" option

## Future Enhancements

1. **Advanced Features**
   - [ ] Relay server support
   - [ ] Custom discovery servers
   - [ ] Bandwidth scheduling
   - [ ] Ignore patterns UI

2. **UI Improvements**
   - [ ] Sync history visualization
   - [ ] Bandwidth usage graphs
   - [ ] Folder statistics dashboard
   - [ ] Dark mode optimizations

3. **Integration**
   - [ ] Backup schedule integration
   - [ ] Notification channels for conflicts
   - [ ] Widget showing sync status
   - [ ] Tasker/automation plugin

## References

- [Syncthing Documentation](https://docs.syncthing.net/)
- [Syncthing REST API](https://docs.syncthing.net/dev/rest.html)
- [Syncthing Android](https://github.com/syncthing/syncthing-android)

## Support

For issues or questions about Syncthing integration:
1. Check this documentation
2. Review Syncthing logs in app
3. Consult official Syncthing documentation
4. Open issue in ObsidianBackup repository

## License

Syncthing is licensed under the Mozilla Public License Version 2.0.
This integration follows the same license terms.
