# Widget Integration Checklist

Use this checklist to integrate the home screen widget into your backup workflow.

## ✅ Pre-Integration Verification

- [x] All widget files created and in correct locations
- [x] Widget receiver registered in AndroidManifest.xml
- [x] String and color resources added
- [x] Documentation reviewed

## 📋 Integration Steps

### Step 1: Update BackupWorker
Add widget integration to your BackupWorker class:

**File:** `app/src/main/java/com/titanbackup/work/BackupWorker.kt`

```kotlin
import com.titanbackup.widget.WidgetIntegration

class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // ✅ ADD THIS: Notify widget backup started
            WidgetIntegration.onBackupStarted(applicationContext)
            
            // Your existing backup logic
            val result = performBackup()
            
            // ✅ ADD THIS: Update widget based on result
            when (result) {
                is BackupResult.Success -> {
                    WidgetIntegration.onBackupCompleted(applicationContext, result)
                }
                is BackupResult.PartialSuccess -> {
                    WidgetIntegration.onBackupPartialSuccess(applicationContext, result)
                }
                is BackupResult.Failure -> {
                    WidgetIntegration.onBackupFailed(applicationContext, result)
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            // ✅ ADD THIS: Update widget on error
            WidgetIntegration.onBackupFailed(
                applicationContext,
                BackupResult.Failure(e.message ?: "Unknown error", emptyList())
            )
            Result.failure()
        }
    }
}
```

**Checklist:**
- [ ] Import WidgetIntegration
- [ ] Add onBackupStarted() call at start
- [ ] Add result handling in when statement
- [ ] Add error handling in catch block
- [ ] Test backup triggers widget updates

### Step 2: Update MainActivity Navigation
Handle navigation from widget to specific screens:

**File:** `app/src/main/java/com/titanbackup/MainActivity.kt`

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // ✅ ADD THIS: Handle widget navigation intent
    handleWidgetIntent(intent)
    
    // Your existing onCreate code...
}

override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    
    // ✅ ADD THIS: Handle new intents from widget
    intent?.let { handleWidgetIntent(it) }
}

private fun handleWidgetIntent(intent: Intent) {
    val navigateTo = intent.getStringExtra("navigate_to")
    
    when (navigateTo) {
        "backups" -> {
            // Navigate to backups screen
            // Example for Compose:
            // navController.navigate("backups")
        }
    }
}
```

**Checklist:**
- [ ] Add intent handling in onCreate()
- [ ] Add intent handling in onNewIntent()
- [ ] Implement navigation to backups screen
- [ ] Test "View Backups" button opens correct screen

### Step 3: (Optional) Update App Launch
Refresh widget when app opens:

**File:** `app/src/main/java/com/titanbackup/ObsidianBackupApplication.kt` or `MainActivity.kt`

```kotlin
import com.titanbackup.widget.WidgetIntegration

override fun onCreate() {
    super.onCreate()
    
    // Your existing initialization...
    
    // ✅ ADD THIS: Refresh widget on app launch
    WidgetIntegration.refreshWidget(this)
}
```

**Checklist:**
- [ ] Add widget refresh on app launch
- [ ] Test widget updates when app opens

### Step 4: (Optional) Update ViewModel
If you want to trigger widget updates from ViewModel:

**File:** `app/src/main/java/com/titanbackup/presentation/backup/BackupViewModel.kt`

```kotlin
import com.titanbackup.widget.WidgetIntegration

fun startBackup() {
    viewModelScope.launch {
        try {
            // Notify widget
            WidgetIntegration.onBackupStarted(getApplication())
            
            // Your backup logic...
            val result = backupEngine.performBackup(...)
            
            // Update widget
            when (result) {
                is BackupResult.Success -> {
                    WidgetIntegration.onBackupCompleted(getApplication(), result)
                }
                // ... other cases
            }
        } catch (e: Exception) {
            WidgetIntegration.onBackupFailed(
                getApplication(),
                BackupResult.Failure(e.message ?: "", emptyList())
            )
        }
    }
}
```

**Checklist:**
- [ ] Add widget integration to backup start
- [ ] Add widget integration to backup completion
- [ ] Add widget integration to backup failure

## 🧪 Testing Checklist

### Basic Functionality
- [ ] Build and install app successfully
- [ ] Widget appears in widget picker
- [ ] Can add widget to home screen
- [ ] Widget shows default state (0 apps, "Never")
- [ ] Widget adapts to light/dark mode

### Widget Interactions
- [ ] "Backup Now" button starts backup
- [ ] Widget status changes to "Backing up..."
- [ ] After backup, status changes to "Ready"
- [ ] Apps count updates correctly
- [ ] Last backup time updates correctly
- [ ] "View Backups" button opens app to backups screen
- [ ] Tapping widget container opens app

### Widget Updates
- [ ] Widget updates during backup
- [ ] Widget updates after backup completes
- [ ] Widget updates on app launch
- [ ] Widget updates periodically (wait 15 min)
- [ ] Multiple widgets update together

### Error Handling
- [ ] Widget shows "Failed" on backup error
- [ ] Widget recovers from failed state
- [ ] Widget handles app force stop
- [ ] Widget survives device reboot

### Different Sizes
- [ ] Small widget (2x2) works correctly
- [ ] Large widget (4x2) works correctly
- [ ] Extra large widget (4x4) works correctly
- [ ] Resizing widget switches layouts

### Edge Cases
- [ ] Fresh install (no backups yet)
- [ ] Very large app counts (1000+)
- [ ] Very old backup dates
- [ ] Rapid backup triggers
- [ ] Low memory situations

## 🐛 Debugging Commands

### Check Widget Registration
```bash
adb shell dumpsys package com.titanbackup | grep -A 10 "AppWidget"
```

### View Widget Data
```bash
adb shell run-as com.titanbackup cat /data/data/com.titanbackup/shared_prefs/widget_backup_status.xml
```

### Force Widget Update
```bash
adb shell am broadcast -a android.appwidget.action.APPWIDGET_UPDATE -n com.titanbackup/.widget.BackupStatusWidget
```

### Check WorkManager Status
```bash
adb shell dumpsys jobscheduler | grep -A 20 WidgetUpdate
```

### Monitor Logs
```bash
adb logcat | grep -i "widget\|backup"
```

## 📝 Post-Integration Tasks

### Documentation
- [ ] Update app README with widget information
- [ ] Add widget screenshots to app listing
- [ ] Update user guide with widget setup
- [ ] Add widget to release notes

### Quality Assurance
- [ ] Run full test suite
- [ ] Test on multiple Android versions
- [ ] Test on different screen sizes
- [ ] Test on different OEM skins (Samsung, etc.)
- [ ] Performance testing (battery drain)

### Analytics (Optional)
- [ ] Track widget installations
- [ ] Track widget button clicks
- [ ] Track widget update frequency
- [ ] Monitor widget errors

### User Experience
- [ ] Add widget tutorial on first launch
- [ ] Add "Add Widget" prompt in app
- [ ] Add widget settings in app (future)
- [ ] Collect user feedback

## 🎉 Completion Criteria

The widget integration is complete when:

- ✅ All integration steps completed
- ✅ All tests passing
- ✅ Widget updates correctly on backup events
- ✅ Navigation works from widget to app
- ✅ No crashes or errors in logcat
- ✅ Tested on at least one physical device
- ✅ Documentation updated

## 📚 Reference Documentation

- **WIDGET_IMPLEMENTATION.md** - Complete technical documentation
- **WIDGET_QUICK_REFERENCE.md** - Quick code snippets and tips
- **WIDGET_SUMMARY.md** - Overview and file listing

## 💡 Tips

1. **Start Simple:** Test basic widget display before integration
2. **Use Logcat:** Monitor logs while testing widget interactions
3. **Test Frequently:** Test widget after each integration step
4. **Multiple Devices:** Test on different Android versions
5. **Error Handling:** Make sure widget handles all error cases gracefully

## 🆘 Need Help?

If you encounter issues:

1. Check **WIDGET_IMPLEMENTATION.md** Troubleshooting section
2. Review **WIDGET_QUICK_REFERENCE.md** for common solutions
3. Use debugging commands above to inspect widget state
4. Check Android Studio Logcat for error messages
5. Verify all files are in correct locations

---

**Remember:** Test thoroughly before releasing! Widgets are visible to users 24/7 on their home screen.
