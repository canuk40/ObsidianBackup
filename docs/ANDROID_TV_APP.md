# Android TV App Implementation

## Overview

ObsidianBackup TV is a complete Android TV application that brings backup functionality to Android TV devices (Android 14+). The app features a 10-foot UI optimized for TV viewing with D-pad navigation, remote control support, and TV-specific backup categories.

## Architecture

### Module Structure
```
tv/
├── src/main/
│   ├── java/com/obsidianbackup/tv/
│   │   ├── TVApplication.kt                 # Application entry point
│   │   ├── ui/                              # UI components
│   │   │   ├── MainActivity.kt              # Main launcher activity
│   │   │   ├── MainFragment.kt              # Browse fragment with leanback
│   │   │   ├── CardPresenters.kt            # Card presenters for UI
│   │   │   ├── BackupDetailsActivity.kt     # Backup details screen
│   │   │   ├── BackupDetailsFragment.kt     # Details fragment
│   │   │   ├── SettingsActivity.kt          # Settings screen
│   │   │   ├── SettingsFragment.kt          # Settings fragment
│   │   │   ├── AppSelectionActivity.kt      # App selection screen
│   │   │   └── AppSelectionFragment.kt      # Vertical grid for app selection
│   │   ├── backup/                          # Backup logic
│   │   │   └── TVBackupManager.kt           # TV-specific backup manager
│   │   ├── navigation/                      # Navigation handling
│   │   │   └── TVNavigationHandler.kt       # D-pad and remote control handler
│   │   └── settings/                        # Settings management
│   │       └── TVSettingsManager.kt         # DataStore-based settings
│   └── res/
│       ├── layout/                          # TV-optimized layouts
│       ├── drawable/                        # Vector icons
│       ├── values/                          # Strings, colors, styles
│       └── xml/                             # Preferences
└── build.gradle.kts                         # Module configuration
```

## Key Features

### 1. TV-Optimized UI (10-foot Interface)

**Leanback Library Integration**
- `BrowseSupportFragment` for main navigation
- `DetailsSupportFragment` for backup details
- `VerticalGridSupportFragment` for app selection
- Large text sizes (18sp-32sp) for readability from distance
- High contrast color scheme for TV viewing

**UI Components**
- **Dashboard Cards**: Quick access to backup operations
- **App Cards**: Display installed TV apps with icons and status
- **Settings Cards**: Navigate to configuration options
- **Details View**: Show backup history and app information

### 2. D-Pad Navigation & Remote Control

**TVNavigationHandler**
- Handles all D-pad events (up, down, left, right)
- Remote control button support (select, back, menu)
- Media control integration (play/pause)
- Focus management for TV navigation
- Automatic focus on first focusable element

**Navigation Features**
- Directional navigation between cards
- Row-based browsing (horizontal scrolling)
- Focus animations (scale effect on focus)
- Back button handling with custom logic

### 3. TV-Specific Backup Categories

**Categorization System**
The app intelligently categorizes apps into:

1. **TV Apps**: Apps using `android.software.leanback` feature
2. **Streaming Apps**: Netflix, YouTube TV, Hulu, Disney+, HBO Max, etc.
3. **Games**: Apps marked with `FLAG_IS_GAME`
4. **Other**: Miscellaneous TV-compatible apps

**Popular Streaming Apps Detected**
- com.netflix.ninja (Netflix)
- com.google.android.youtube.tv (YouTube TV)
- com.amazon.amazonvideo.livingroom (Prime Video)
- com.hulu.livingroomplus (Hulu)
- com.disney.disneyplus (Disney+)
- com.hbo.hbonow (HBO Max)
- com.spotify.tv.android (Spotify)
- com.plexapp.android (Plex)
- com.apple.atve.androidtv.appletv (Apple TV+)

### 4. Backup Operations

**TVBackupManager**
- Scans installed TV apps
- Calculates app sizes (APK + data)
- Tracks backup status per app
- Storage status monitoring
- App selection management

**Backup Features**
- Full app backup (APK + data)
- Incremental backups
- Scheduled automatic backups
- Cloud sync support
- Compression and encryption options

### 5. Banner & TV Launcher Integration

**AndroidManifest Configuration**
```xml
<uses-feature
    android:name="android.software.leanback"
    android:required="true" />

<application
    android:banner="@drawable/tv_banner"
    android:theme="@style/Theme.Leanback">
    
    <activity android:name=".ui.MainActivity">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
        </intent-filter>
    </activity>
</application>
```

**Banner Specifications**
- Dimensions: 320x180 dp (recommended)
- Format: PNG or vector drawable
- Location: res/drawable-xhdpi/tv_banner.xml
- Design: High contrast with app branding

### 6. Settings & Preferences

**TV Preferences (LeanbackPreferenceFragmentCompat)**
- Automatic backup toggle
- Backup frequency (hourly, daily, weekly, monthly)
- Include app data option
- Cloud provider selection
- Compression/encryption settings

**DataStore Integration**
Settings are persisted using Jetpack DataStore:
- Auto backup enabled/disabled
- Backup frequency preference
- Cloud sync configuration
- Security settings

### 7. Large Text & High Contrast

**Typography Scale**
- Title: 32sp (bold)
- Subtitle: 24sp
- Body: 20sp
- Button: 18sp

**Color Scheme**
- Primary: #2196F3 (Blue)
- Accent: #FF9800 (Orange)
- Background: #0F0F0F (Near black)
- Card Background: #212121 (Dark gray)
- Text Primary: #FFFFFF (White)
- Text Secondary: #B3B3B3 (Light gray)

**Contrast Ratios**
- White on dark background: 21:1 (WCAG AAA)
- All UI elements meet TV accessibility standards

### 8. Android 14 TV Compatibility

**Target SDK: 35 (Android 14+)**
- Compile SDK: 35
- Min SDK: 21 (Android TV minimum)
- Build Tools: 35.0.0

**Android 14 Features**
- Updated permissions model
- Modern TV launcher support
- Enhanced focus handling
- Predictive back gesture support

## Implementation Details

### Dependency Injection (Hilt)

All major components use Hilt for dependency injection:

```kotlin
@HiltAndroidApp
class TVApplication : Application()

@AndroidEntryPoint
class MainActivity : FragmentActivity()

@Singleton
class TVBackupManager @Inject constructor(
    @ApplicationContext private val context: Context
)
```

### Card Presenters

Three custom presenters for different card types:

1. **DashboardCardPresenter**: Dashboard action cards
2. **TVAppCardPresenter**: App display cards with icons
3. **SettingsCardPresenter**: Settings navigation cards

Each presenter handles:
- View creation and binding
- Focus animations
- Data display formatting

### Focus Management

```kotlin
cardView.setOnFocusChangeListener { _, hasFocus ->
    val scale = if (hasFocus) 1.1f else 1.0f
    cardView.animate()
        .scaleX(scale)
        .scaleY(scale)
        .setDuration(150)
        .start()
}
```

### Remote Control Input

Handled through `TVNavigationHandler`:
- DPAD_UP/DOWN/LEFT/RIGHT: Navigation
- DPAD_CENTER/ENTER: Selection
- BACK: Return to previous screen
- MENU: Open context menu
- MEDIA_PLAY/PAUSE: Media controls

## Build & Installation

### Build the TV Module

```bash
cd /root/workspace/ObsidianBackup
./gradlew :tv:assembleDebug
```

### Install on Android TV

**Via ADB**
```bash
adb connect <TV_IP_ADDRESS>:5555
adb install -r tv/build/outputs/apk/debug/tv-debug.apk
```

**Via Google Play Console**
1. Build release APK: `./gradlew :tv:assembleRelease`
2. Upload to Play Console
3. Enable Android TV category
4. Submit for review

### Testing on TV Emulator

```bash
# Create Android TV AVD
avdmanager create avd -n AndroidTV -k "system-images;android-34;google_apis;x86_64" -d "tv_1080p"

# Launch emulator
emulator -avd AndroidTV
```

## Usage Guide

### For End Users

**Launching the App**
1. Navigate to apps section on TV home screen
2. Find "ObsidianBackup TV" with banner
3. Click to launch

**Performing a Backup**
1. Navigate to "Backup Now" card using D-pad
2. Press SELECT/ENTER on remote
3. Wait for backup to complete
4. Notification shows completion

**Viewing Backup History**
1. Navigate to "Recent Backups"
2. Press SELECT to view details
3. Use D-pad to browse history
4. Select backup to restore

**Configuring Settings**
1. Navigate to Settings row
2. Select desired setting
3. Use D-pad to change values
4. Press BACK to save

### For Developers

**Adding New Card Types**

```kotlin
// 1. Create data class
data class NewCard(val title: String, val data: Any)

// 2. Create presenter
class NewCardPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        // Inflate layout
    }
    
    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        // Bind data
    }
}

// 3. Add to MainFragment
val newHeader = HeaderItem(5, "New Section")
val newAdapter = ArrayObjectAdapter(NewCardPresenter())
rowsAdapter.add(ListRow(newHeader, newAdapter))
```

**Extending Backup Categories**

```kotlin
// Add to TVBackupManager
enum class TVAppCategory {
    TV_APP,
    STREAMING,
    GAME,
    PRODUCTIVITY, // New category
    OTHER
}

private fun categorizeApp(appInfo: ApplicationInfo): TVAppCategory {
    // Add categorization logic
    val productivityApps = setOf("com.microsoft.office", ...)
    if (appInfo.packageName in productivityApps) {
        return TVAppCategory.PRODUCTIVITY
    }
    // ...
}
```

## Testing

### Manual Testing Checklist

- [ ] App launches from TV launcher
- [ ] Banner displays correctly
- [ ] D-pad navigation works in all directions
- [ ] Remote control buttons respond correctly
- [ ] Focus animations work smoothly
- [ ] Cards display app information
- [ ] Backup operation completes successfully
- [ ] Settings can be modified
- [ ] Back button returns to previous screen
- [ ] Text is readable from 10 feet away
- [ ] High contrast is maintained throughout

### Automated Testing

Create UI tests using Espresso:

```kotlin
@Test
fun testNavigationToSettings() {
    onView(withId(R.id.main_browse_fragment))
        .perform(pressKey(KeyEvent.KEYCODE_DPAD_DOWN))
        .perform(pressKey(KeyEvent.KEYCODE_DPAD_RIGHT))
        .perform(pressKey(KeyEvent.KEYCODE_DPAD_CENTER))
    
    onView(withText("Backup Settings"))
        .check(matches(isDisplayed()))
}
```

## Performance Considerations

### Memory Management
- Recycle bitmaps for app icons
- Use Glide for image loading
- Limit grid adapter size

### UI Responsiveness
- Load apps asynchronously
- Use coroutines for background work
- Implement pagination for large lists

### Battery Optimization
- Use WorkManager for scheduled backups
- Respect battery saver mode
- Minimize wake locks

## Troubleshooting

### Common Issues

**Banner Not Displaying**
- Ensure banner dimensions are correct (320x180 dp)
- Check banner is in res/drawable-xhdpi/
- Verify android:banner attribute in manifest

**Navigation Not Working**
- Confirm views are focusable (android:focusable="true")
- Check focus order is logical
- Verify D-pad events are not consumed

**Apps Not Detected**
- Check QUERY_ALL_PACKAGES permission
- Verify app filtering logic
- Ensure system apps are filtered correctly

**Focus Animations Stuttering**
- Reduce animation duration
- Use hardware acceleration
- Optimize card layouts

## Future Enhancements

### Planned Features
- [ ] Voice search integration
- [ ] Recommendations row for quick restore
- [ ] Multiple user profiles
- [ ] Parental controls
- [ ] Network backup over LAN
- [ ] TV provider integration for channels
- [ ] Picture-in-picture backup progress
- [ ] Android TV input framework integration

### Advanced Features
- [ ] Multi-device sync
- [ ] Remote backup from phone to TV
- [ ] TV-to-TV direct transfer
- [ ] Cloud backup streaming
- [ ] Differential backups
- [ ] Backup verification with checksums

## Resources

### Documentation
- [Android TV Design Guidelines](https://developer.android.com/design/ui/tv)
- [Leanback Library Guide](https://developer.android.com/training/tv/start/start)
- [TV Input Framework](https://developer.android.com/training/tv/tif)

### Tools
- Android TV Emulator
- Remote Control Simulator
- TV Layout Inspector

### Sample Code
- [Android TV Samples](https://github.com/android/tv-samples)
- [Leanback Showcase](https://github.com/android/tv-samples/tree/main/LeanbackShowcase)

## License

Same as main ObsidianBackup project.

## Support

For issues specific to TV version:
1. Check TV device logs: `adb logcat -s ObsidianBackupTV`
2. Verify TV API level compatibility
3. Test on TV emulator first
4. Report issues with TV model information

---

**Version**: 1.0
**Last Updated**: 2024
**Target Platform**: Android TV (Android 14+)
**Minimum SDK**: API 21 (Android 5.0)
