# Accessibility Quick Reference

Quick reference for implementing accessibility features in ObsidianBackup.

## Common Patterns

### Adding Content Descriptions

```kotlin
// Icons
Icon(
    imageVector = Icons.Default.Backup,
    contentDescription = stringResource(R.string.cd_backup_button)
)

// Images
Image(
    painter = painterResource(R.drawable.logo),
    contentDescription = "ObsidianBackup logo"
)
```

### Touch Target Sizing

```kotlin
import com.obsidianbackup.accessibility.AccessibilityHelper

Button(
    onClick = { /* action */ },
    modifier = Modifier.heightIn(min = AccessibilityHelper.MIN_TOUCH_TARGET_SIZE_DP.dp)
) {
    Text("Action")
}
```

### Semantic Properties

```kotlin
// Mark as heading
Text(
    text = "Dashboard",
    modifier = Modifier.semantics { heading() }
)

// Add content description
Button(
    onClick = { /* action */ },
    modifier = Modifier.semantics {
        contentDescription = "Backup all applications"
    }
) {
    Text("Backup")
}

// Indicate selection state
Checkbox(
    checked = isSelected,
    onCheckedChange = { /* change */ },
    modifier = Modifier.semantics {
        selected = isSelected
    }
)
```

### Screen Reader Announcements

```kotlin
import com.obsidianbackup.accessibility.AccessibilityHelper

// Announce important events
AccessibilityHelper.announceForAccessibility(
    context,
    context.getString(R.string.announce_backup_started)
)

// Announce with custom message
AccessibilityHelper.announceForAccessibility(
    context,
    "Backup completed: ${appCount} apps backed up"
)
```

### High Contrast Theme

```kotlin
// In your activity or composable root
ObsidianBackupTheme(
    highContrast = preferences.highContrastEnabled,
    darkTheme = isSystemInDarkTheme()
) {
    // Your content
}
```

### Voice Control

```kotlin
// Initialize voice control
val voiceControlHandler = VoiceControlHandler(context)
voiceControlHandler.initialize()

// Start listening
voiceControlHandler.startListening()

// Handle commands
voiceControlHandler.lastCommand.observe(this) { command ->
    when (command) {
        is VoiceCommand.BackupApps -> startBackup()
        is VoiceCommand.RestoreApps -> startRestore()
        is VoiceCommand.CheckStatus -> showStatus()
        is VoiceCommand.OpenSettings -> navigateToSettings()
        is VoiceCommand.CancelOperation -> cancelOperation()
        is VoiceCommand.ShowHelp -> showHelp()
        is VoiceCommand.Unknown -> handleUnknownCommand(command.input)
    }
}

// Stop listening
voiceControlHandler.stopListening()

// Clean up
voiceControlHandler.release()
```

### Simplified Mode

```kotlin
// Enable simplified mode
SimplifiedModeScreen(viewModel = hiltViewModel())

// Check if simplified mode is enabled
val viewModel: SimplifiedModeViewModel = hiltViewModel()
val isSimplified by viewModel.simplifiedModeEnabled.collectAsState()
```

### Color Contrast Checking

```kotlin
import com.obsidianbackup.accessibility.AccessibilityHelper

// Calculate contrast ratio
val ratio = AccessibilityHelper.calculateContrastRatio(
    foreground = Color.Black.toArgb(),
    background = Color.White.toArgb()
)

// Check WCAG AA compliance
val meetsAA = AccessibilityHelper.meetsWCAGAA(
    foreground = textColor.toArgb(),
    background = backgroundColor.toArgb(),
    isLargeText = false
)

// Check WCAG AAA compliance
val meetsAAA = AccessibilityHelper.meetsWCAGAAA(
    foreground = textColor.toArgb(),
    background = backgroundColor.toArgb(),
    isLargeText = false
)
```

### Accessibility State Detection

```kotlin
// Check if accessibility services are enabled
val isAccessibilityEnabled = AccessibilityHelper.isAccessibilityEnabled(context)

// Check if screen reader is active
val isScreenReaderActive = AccessibilityHelper.isScreenReaderActive(context)

// Use in Compose
val accessibilityState = rememberAccessibilityState()
if (accessibilityState.isScreenReaderActive) {
    // Provide extra context for screen reader users
}
```

## String Resources

All accessibility strings are in `res/values/strings.xml`:

```xml
<!-- Content Descriptions -->
<string name="cd_backup_button">Backup selected applications</string>
<string name="cd_nav_dashboard">Navigate to Dashboard</string>

<!-- Announcements -->
<string name="announce_backup_started">Backup started</string>
<string name="announce_backup_complete">Backup completed successfully</string>

<!-- Voice Commands -->
<string name="voice_command_backup">backup my apps</string>

<!-- Simplified Mode -->
<string name="simplified_backup_now">Backup Now</string>
```

## Color Resources

High contrast colors are in `res/values/colors.xml`:

```xml
<!-- High Contrast Colors -->
<color name="hc_primary">#FF007700</color>
<color name="hc_primary_dark">#FF00FF00</color>
<color name="hc_background_light">#FFFFFFFF</color>
<color name="hc_background_dark">#FF000000</color>
```

## Testing Commands

```bash
# Run accessibility tests
./gradlew connectedAndroidTest --tests "com.obsidianbackup.accessibility.AccessibilityTest"

# Run all tests
./gradlew test

# Check contrast ratios
./gradlew assembleDebug
# Then use Android Accessibility Scanner on device
```

## Common Mistakes to Avoid

❌ **DON'T:**
```kotlin
// Empty content description
Icon(Icons.Default.Backup, contentDescription = "")

// Touch target too small
Button(onClick = {}, modifier = Modifier.size(24.dp)) {}

// Color-only information
Text("Success", color = Color.Green) // No text indication

// Missing semantic properties
Text("Title") // Should be marked as heading
```

✅ **DO:**
```kotlin
// Descriptive content description
Icon(Icons.Default.Backup, contentDescription = "Backup applications")

// Adequate touch target
Button(
    onClick = {},
    modifier = Modifier.heightIn(min = 48.dp)
) {}

// Text + color information
Row {
    Icon(Icons.Default.CheckCircle, contentDescription = null)
    Text("Success")
}

// Proper semantic properties
Text(
    "Title",
    modifier = Modifier.semantics { heading() }
)
```

## Checklist for New Features

- [ ] All icons have contentDescription
- [ ] Touch targets are minimum 48x48 dp
- [ ] Color contrast is 4.5:1 or higher
- [ ] Semantic properties added (heading, selected, etc.)
- [ ] Screen reader announcements for important events
- [ ] Focus order is logical
- [ ] Works with TalkBack enabled
- [ ] Works in high contrast mode
- [ ] Works in simplified mode (if applicable)
- [ ] Keyboard navigation supported
- [ ] No information conveyed by color alone

## Resources

- **Full Guide:** `ACCESSIBILITY_GUIDE.md`
- **Implementation Summary:** `ACCESSIBILITY_IMPLEMENTATION_SUMMARY.md`
- **Helper Class:** `accessibility/AccessibilityHelper.kt`
- **Test Suite:** `androidTest/accessibility/AccessibilityTest.kt`

## Quick Tips

1. **Test with TalkBack:** Always test with TalkBack enabled
2. **Use Accessibility Scanner:** Scan every screen
3. **Check Contrast:** Use the built-in contrast checker
4. **Large Touch Targets:** When in doubt, make it bigger
5. **Descriptive Labels:** Be specific in contentDescription
6. **Announce Changes:** Use announceForAccessibility() for important updates
7. **Semantic Structure:** Use proper headings and landmarks
8. **Test Early:** Add accessibility from the start, not as an afterthought

---

**Need Help?** Check `ACCESSIBILITY_GUIDE.md` for detailed information.
