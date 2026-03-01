# Community Features Quick Reference

## 🚀 Quick Start

### For Users
1. Open ObsidianBackup
2. Navigate to **Community** tab (bottom navigation)
3. Explore features:
   - Submit feedback
   - Join beta program
   - View changelog
   - See tips & tricks
   - Connect with community

### For Developers
```kotlin
// Inject managers via Hilt
@Inject lateinit var feedbackManager: FeedbackManager
@Inject lateinit var analyticsManager: AnalyticsManager
@Inject lateinit var betaProgramManager: BetaProgramManager
```

---

## 📂 File Locations

### Managers
```
app/src/main/java/com/obsidianbackup/community/
├── FeedbackManager.kt
├── AnalyticsManager.kt
├── BetaProgramManager.kt
├── ChangelogManager.kt
├── CommunityForumManager.kt
├── ConfigSharingManager.kt
├── OnboardingManager.kt
├── TipsManager.kt
└── CrashlyticsManager.kt
```

### UI Screens
```
app/src/main/java/com/obsidianbackup/ui/screens/community/
├── CommunityScreen.kt
├── FeedbackScreen.kt
├── ChangelogAndTipsScreens.kt
├── OnboardingScreen.kt
└── CommunityViewModels.kt
```

---

## 🎯 Common Tasks

### Submit Feedback
```kotlin
viewModelScope.launch {
    feedbackManager.submitFeedback(
        type = FeedbackType.BUG_REPORT,
        title = "Issue title",
        description = "Detailed description",
        email = "optional@email.com",
        attachLogs = true
    )
}
```

### Log Analytics Event
```kotlin
viewModelScope.launch {
    analyticsManager.logEvent("custom_event", mapOf(
        "parameter1" to "value1",
        "parameter2" to 123
    ))
}
```

### Check Beta Status
```kotlin
betaProgramManager.betaEnrolled.collect { enrolled ->
    if (enrolled) {
        // Show beta features
    }
}
```

### Get Tip of the Day
```kotlin
viewModelScope.launch {
    val tip = tipsManager.getTipOfTheDay()
    tip?.let { displayTip(it) }
}
```

---

## 🔧 Configuration

### Enable Analytics
```kotlin
// In settings or community screen
analyticsManager.setAnalyticsEnabled(true)
```

### Initialize Crashlytics
```kotlin
// In Application.onCreate()
crashlyticsManager.initialize(crashReportingEnabled = true)
```

### Setup Firebase
1. Replace `app/google-services.json`
2. See `FIREBASE_SETUP.md` for details

---

## 📱 Navigation Routes

```kotlin
Screen.Community.route        // "community"
Screen.Feedback.route         // "feedback"
Screen.Changelog.route        // "changelog"
Screen.Tips.route             // "tips"
Screen.Onboarding.route       // "onboarding"
```

---

## 🎨 UI Components

### Community Hub Card
```kotlin
CommunityScreen(
    viewModel = hiltViewModel()
)
```

### Feedback Dialog
```kotlin
FeedbackDialog(
    onDismiss = { /* ... */ },
    onSubmit = { type, title, desc, email, logs ->
        viewModel.submitFeedback(type, title, desc, email, logs)
    }
)
```

### Onboarding Flow
```kotlin
OnboardingScreen(
    onComplete = {
        // Navigate to main screen
    }
)
```

---

## 📊 Analytics Events

| Event | Parameters | Description |
|-------|------------|-------------|
| `feedback_submitted` | type, has_email, has_logs | User submits feedback |
| `beta_enrolled` | channel | User joins beta |
| `onboarding_completed` | - | Onboarding finished |
| `backup_operation` | action, type, success | Backup event |

---

## 🔐 Privacy Settings

```kotlin
// Check if analytics enabled
analyticsManager.analyticsEnabled.first()

// Check if crash reporting enabled
crashlyticsManager.isCrashlyticsEnabled()

// All settings are opt-in by default
```

---

## 🧪 Testing

### Test Crash Reporting
```kotlin
throw RuntimeException("Test Crash")
```

### Test Analytics (Debug)
```bash
adb shell setprop debug.firebase.analytics.app com.obsidianbackup
```

### Verify Events
Check Firebase Console → Analytics → DebugView

---

## 📚 Documentation

- **Full Guide**: `COMMUNITY_FEATURES.md`
- **Firebase Setup**: `FIREBASE_SETUP.md`
- **Implementation**: `COMMUNITY_IMPLEMENTATION_SUMMARY.md`

---

## 💡 Tips

1. **Privacy First**: Always check user consent before logging
2. **Filter PII**: Use built-in sanitization methods
3. **Test Locally**: Use DebugView for analytics testing
4. **Update Changelog**: Keep users informed of changes
5. **Monitor Crashes**: Review Crashlytics daily

---

## 🆘 Troubleshooting

### Build Fails
- Verify `google-services.json` exists
- Sync Gradle files
- Check Firebase dependencies

### Analytics Not Working
- Enable in Community → Privacy
- Wait 24 hours or use DebugView
- Check internet connection

### Crashlytics Not Reporting
- Restart app after crash
- Wait 5-10 minutes
- Verify Crashlytics enabled in Firebase

---

## 🔗 Links

- Discord: https://discord.gg/obsidianbackup
- Reddit: https://reddit.com/r/obsidianbackup
- GitHub: https://github.com/obsidianbackup/obsidianbackup
- Docs: https://docs.obsidianbackup.app

---

**Quick Access**: Bookmark this file for rapid reference during development!
