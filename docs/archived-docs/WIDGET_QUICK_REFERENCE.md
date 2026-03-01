# Widget Quick Reference Guide

## Quick Start

### Adding Widget to Home Screen (User)
1. Long press on home screen
2. Tap "Widgets"
3. Find "TitanBackup"
4. Select "Backup Status" widget
5. Drag to home screen
6. Resize as needed (2x2, 4x2, or 4x4)

## Developer Integration

### Update Widget After Backup

```kotlin
// In BackupWorker.kt or BackupEngine.kt

// When backup starts
WidgetIntegration.onBackupStarted(context)

// When backup completes
when (result) {
    is BackupResult.Success -> 
        WidgetIntegration.onBackupCompleted(context, result)
    
    is BackupResult.PartialSuccess -> 
        WidgetIntegration.onBackupPartialSuccess(context, result)
    
    is BackupResult.Failure -> 
        WidgetIntegration.onBackupFailed(context, result)
}
```

### Manual Widget Update

```kotlin
// Force refresh all widgets
WidgetIntegration.refreshWidget(context)

// Or directly
BackupStatusWidget.updateWidgets(context)
```

### Update Specific Widget Data

```kotlin
BackupStatusWidget.updateWidgetStatus(
    context = context,
    status = BackupStatusWidget.BackupStatus.IDLE,
    appsBackedUp = 42,
    lastBackupTime = System.currentTimeMillis()
)
```

## Widget States

```kotlin
enum class BackupStatus {
    IDLE,      // Green indicator - Ready for backup
    RUNNING,   // Blue indicator - Backup in progress
    FAILED     // Red indicator - Last backup failed
}
```

## Data Storage

Widget data is stored in SharedPreferences:
- **File:** `widget_backup_status`
- **Keys:**
  - `last_backup_time`: Long (milliseconds)
  - `apps_backed_up`: Int (count)
  - `backup_status`: String (IDLE/RUNNING/FAILED)

## Files Overview

### Kotlin Files
- `BackupStatusWidget.kt` - Main widget provider (273 lines)
- `WidgetUpdateService.kt` - Periodic updates (104 lines)
- `WidgetIntegration.kt` - Integration helper (87 lines)

### Layout Files
- `widget_backup_status.xml` - 2x2 small widget
- `widget_backup_status_large.xml` - 4x2 horizontal widget
- `widget_backup_status_extra_large.xml` - 4x4 full widget

### Resource Files
- `backup_widget_info.xml` - Widget configuration
- `widget_background.xml` - Main background drawable
- `widget_button_background.xml` - Button styling
- `widget_preview.xml` - Preview image

### Strings (strings.xml)
- `widget_description`
- `widget_status_idle/running/failed`
- `widget_backup_now`
- `widget_view_backups`
- etc.

## Testing Commands

### Check Widget is Registered
```bash
adb shell dumpsys package com.titanbackup | grep -A 10 "AppWidget"
```

### Force Widget Update
```bash
adb shell am broadcast -a android.appwidget.action.APPWIDGET_UPDATE
```

### Check WorkManager Jobs
```bash
adb shell dumpsys jobscheduler | grep WidgetUpdate
```

## Troubleshooting

### Widget Not Appearing
1. Check AndroidManifest.xml has widget receiver
2. Verify backup_widget_info.xml exists
3. Rebuild app (clean build)
4. Check min SDK compatibility

### Widget Not Updating
1. Check SharedPreferences data: 
   ```bash
   adb shell run-as com.titanbackup cat /data/data/com.titanbackup/shared_prefs/widget_backup_status.xml
   ```
2. Verify WorkManager is scheduled
3. Check system battery optimization settings
4. Force manual update via code

### Click Actions Not Working
1. Check PendingIntent flags (IMMUTABLE)
2. Verify intent actions in manifest
3. Check MainActivity launch mode
4. Test on different Android versions

## Common Customizations

### Change Update Interval
In `WidgetUpdateService.kt`:
```kotlin
private const val UPDATE_INTERVAL_MINUTES = 15L // Change this
```

### Add More Status Types
In `BackupStatusWidget.kt`:
```kotlin
enum class BackupStatus {
    IDLE,
    RUNNING,
    FAILED,
    SCHEDULED,  // Add new status
    PAUSED      // Add new status
}
```

### Customize Colors
In `colors.xml`:
```xml
<color name="widget_status_idle">#4CAF50</color>
<color name="widget_status_running">#2196F3</color>
<color name="widget_status_failed">#F44336</color>
```

## Performance Notes

- **Update Frequency:** 15 minutes (Android minimum)
- **Battery Impact:** Low (WorkManager constraints)
- **Memory Usage:** Minimal (RemoteViews)
- **Network Usage:** None (local data only)

## Material You Features

- ✅ Dynamic system colors (Android 12+)
- ✅ Rounded corners (16dp)
- ✅ Ripple effects
- ✅ Proper elevation
- ✅ Typography hierarchy
- ✅ Dark mode support

## Compatibility

- **Min SDK:** 21 (Android 5.0)
- **Target SDK:** 33 (Android 13)
- **Material You:** 31+ (Android 12+)
- **Widget Sizing:** 31+ (Android 12+)

## Next Steps

1. ✅ Widget implementation complete
2. ⏳ Test on physical device
3. ⏳ Integrate with BackupWorker
4. ⏳ Add to app tour/onboarding
5. ⏳ Create widget configuration activity (optional)
6. ⏳ Add widget analytics (optional)

## Resources

- Full documentation: `WIDGET_IMPLEMENTATION.md`
- Android Widgets: https://developer.android.com/guide/topics/appwidgets
- Material You: https://m3.material.io/
