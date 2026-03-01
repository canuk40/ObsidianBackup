# Community Engagement Features Documentation

## Overview

ObsidianBackup includes comprehensive community engagement features designed with **privacy-first** principles. All features respect user privacy, collect no PII (Personally Identifiable Information), and provide full transparency and control.

## 🎯 Implemented Features

### 1. In-App Feedback System

**Location**: `com.obsidianbackup.community.FeedbackManager`

The feedback system allows users to submit bug reports, feature requests, and improvements directly from the app.

#### Features:
- **Multiple feedback types**:
  - Bug Reports
  - Feature Requests
  - Improvements
  - Other

- **Optional details**:
  - Title and description (required)
  - Email address (optional for follow-up)
  - Attach logs (optional)
  - Automatic device info collection

- **Local storage**: All feedback is stored locally with unique IDs
- **Status tracking**: Track submission status (Idle, Submitting, Success, Error)

#### Usage Example:
```kotlin
feedbackManager.submitFeedback(
    type = FeedbackType.BUG_REPORT,
    title = "Backup fails on Android 14",
    description = "Description of the issue...",
    email = "user@example.com", // Optional
    attachLogs = true
)
```

#### UI:
- **Screen**: `FeedbackScreen.kt`
- Access via: Community → Send Feedback
- Floating action button for quick submission
- View previously submitted feedback

---

### 2. Firebase Crashlytics Integration

**Location**: `com.obsidianbackup.community.CrashlyticsManager`

Automatic crash reporting to help identify and fix bugs quickly.

#### Privacy Features:
- **No PII collection**: All PII is automatically filtered
- **User consent**: Crash reporting can be enabled/disabled
- **Data sanitization**: 
  - Email addresses → `[EMAIL]`
  - Phone numbers → `[PHONE]`
  - API keys/tokens → `[TOKEN]`

#### Features:
- Non-fatal exception recording
- Custom keys for debugging context
- Anonymized user IDs only
- Automatic crash collection

#### Configuration:
```kotlin
// Initialize in Application class
crashlyticsManager.initialize(crashReportingEnabled = true)

// Record exceptions
crashlyticsManager.recordException(exception)

// Set custom debugging keys (no PII)
crashlyticsManager.setCustomKey("backup_type", "incremental")
```

#### Setup:
1. Add your `google-services.json` to `/app/`
2. Configure Firebase project in Firebase Console
3. Enable Crashlytics in Firebase Console
4. Deploy and test

---

### 3. Privacy-Respecting Analytics

**Location**: `com.obsidianbackup.community.AnalyticsManager`

Analytics that respect user privacy with **opt-in** model and zero PII collection.

#### Privacy Principles:
- ✅ **Opt-in by default**: Users must explicitly enable
- ✅ **No PII**: Automatic filtering of personal information
- ✅ **Transparent**: Users know exactly what's tracked
- ✅ **User control**: Can be disabled anytime

#### What We Track:
- App screens viewed (screen_view)
- Feature usage (e.g., backup_operation)
- Error rates (anonymized)
- Performance metrics (backup duration, etc.)

#### What We DON'T Track:
- ❌ Names, emails, phone numbers
- ❌ IP addresses
- ❌ User content or file names
- ❌ Personal identifiers
- ❌ Location data

#### Usage Example:
```kotlin
// Enable/disable analytics
analyticsManager.setAnalyticsEnabled(true)

// Log events (automatically filtered for PII)
analyticsManager.logEvent("backup_completed", mapOf(
    "backup_type" to "incremental",
    "duration_ms" to 5000L
))

// Log screen views
analyticsManager.logScreenView("DashboardScreen")
```

#### User Controls:
- Toggle in: Community → Privacy → Analytics switch
- Default: **Disabled** (opt-in)

---

### 4. Beta Program Management

**Location**: `com.obsidianbackup.community.BetaProgramManager`

Allow users to opt into beta channels for early access to features.

#### Beta Channels:
1. **Stable**: Production-ready features
2. **Beta**: Early access to upcoming features
3. **Alpha**: Cutting-edge features (may be unstable)

#### Features:
- Seamless enrollment/unenrollment
- Channel switching
- Beta-exclusive feature flags
- Automatic updates when enrolled

#### Usage:
```kotlin
// Enroll in beta
betaProgramManager.enrollInBeta(BetaChannel.BETA)

// Leave beta
betaProgramManager.leaveBeta()

// Check enrollment status
betaProgramManager.betaEnrolled.collect { enrolled ->
    // Update UI
}
```

#### Beta Features by Channel:
- **Alpha**: ML Deduplication, Experimental Sync, Advanced Compression
- **Beta**: Advanced Compression, New UI Components
- **Stable**: All production features

#### UI:
- Access via: Community → Beta Program card
- One-click enrollment
- Visual indicator when enrolled

---

### 5. Changelog Viewer

**Location**: `com.obsidianbackup.community.ChangelogManager`

Display version history and what's new in each release.

#### Features:
- Version-by-version changelog
- Categorized changes:
  - 🆕 **Features**: New functionality
  - ⬆️ **Improvements**: Enhancements
  - 🐛 **Bug Fixes**: Issues resolved
  - 🔒 **Security**: Security updates
  - ⚠️ **Deprecated**: Removed features

- Release dates
- Version highlights
- Search/filter by version

#### Usage:
```kotlin
// Get all changelogs
changelogManager.changelog.collect { entries ->
    // Display in UI
}

// Get specific version
changelogManager.getChangelogForVersion("1.0.0")

// Get latest changes
changelogManager.getLatestChanges(count = 5)
```

#### UI:
- Access via: Community → Changelog
- Chronological display
- Color-coded change types
- Expandable entries

---

### 6. Community Forum Integration

**Location**: `com.obsidianbackup.community.CommunityForumManager`

Direct links to community channels and support resources.

#### Integrated Platforms:
1. **Discord** - Real-time chat and support
   - URL: `https://discord.gg/obsidianbackup`
   
2. **Reddit** - Discussion and tips
   - URL: `https://reddit.com/r/obsidianbackup`
   
3. **GitHub** - Bug reports and contributions
   - URL: `https://github.com/obsidianbackup/obsidianbackup`
   
4. **Documentation** - Comprehensive guides
   - URL: `https://docs.obsidianbackup.app`
   
5. **Support** - Troubleshooting and FAQs
   - URL: `https://obsidianbackup.app/support`

#### Features:
- One-click navigation to external links
- Opens in default browser
- Automatic intent handling
- Error handling for missing browsers

#### Usage:
```kotlin
// Open specific platforms
communityForumManager.openDiscord()
communityForumManager.openReddit()
communityForumManager.openGitHub()

// Get all community links
val links = communityForumManager.getCommunityLinks()
```

#### UI:
- Access via: Community → Community Channels section
- Icon-based navigation cards
- Platform descriptions
- External link indicators

---

### 7. Backup Config Sharing (Anonymized)

**Location**: `com.obsidianbackup.community.ConfigSharingManager`

Share backup configurations with other users while protecting privacy.

#### Privacy Features:
- **Complete anonymization**: All PII stripped
- **SHA-256 hashing**: Names and identifiers hashed
- **No credentials**: Cloud credentials never shared
- **JSON format**: Human-readable export

#### What Gets Shared:
✅ Backup settings (compression, encryption enabled/disabled)
✅ Schedule types
✅ Retention policies
✅ Bandwidth limits
✅ Content types (apps, media, documents)

#### What's NOT Shared:
❌ Account names
❌ Email addresses
❌ Cloud credentials
❌ File paths
❌ Personal identifiers

#### Usage:
```kotlin
// Export config
val configJson = configSharingManager.exportConfig(backupConfig)

// Import config
val result = configSharingManager.importConfig(jsonString)

// Generate shareable link (for future web integration)
val link = configSharingManager.generateShareLink(backupConfig)
```

#### Example Anonymized Config:
```json
{
  "id": "a3f5c8d9e2b1f0a7",
  "backupType": "incremental",
  "compressionEnabled": true,
  "encryptionEnabled": true,
  "scheduleType": "nightly",
  "cloudProvider": "b7d2e1f4a9c3d8e5",
  "includeApps": true,
  "includeMedia": false,
  "retentionDays": 30,
  "bandwidthLimitMbps": 10
}
```

---

### 8. User Onboarding Tutorial

**Location**: `com.obsidianbackup.community.OnboardingManager`

Interactive tutorial for new users.

#### Onboarding Steps:
1. **Welcome** - Introduction to ObsidianBackup
2. **Storage** - Choose backup destinations
3. **Security** - Learn about encryption
4. **Automation** - Set up automatic backups
5. **Control** - Monitor and manage

#### Features:
- Swipeable pages
- Skip option
- Progress indicators
- Completion tracking
- Can be restarted from settings

#### Implementation:
```kotlin
// Check if onboarding completed
onboardingManager.onboardingCompleted.collect { completed ->
    if (!completed) {
        navigateToOnboarding()
    }
}

// Complete onboarding
onboardingManager.completeOnboarding()

// Skip onboarding
onboardingManager.skipOnboarding()
```

#### UI:
- Auto-launches on first app start
- Horizontal pager with animations
- "Skip" button in top bar
- "Get Started" on final page

---

### 9. Tips and Tricks System

**Location**: `com.obsidianbackup.community.TipsManager`

Contextual tips to help users get the most from the app.

#### Features:
- **Tip of the Day**: Random daily tip
- **Categorized tips**:
  - 🔒 Security
  - ⚡ Performance
  - ⚙️ Settings
  - 🤖 Automation
  - ✅ Best Practices
  - 💼 Productivity

- **Dismissible**: Hide tips you've seen
- **Priority levels**: High, Medium, Low
- **Reset option**: Show all tips again

#### Available Tips (10 total):
1. Enable Encryption (Security, High)
2. Use Incremental Backups (Performance, Medium)
3. Backup on WiFi (Settings, Medium)
4. Schedule Regular Backups (Automation, High)
5. Verify Your Backups (Best Practice, High)
6. Set Retention Policies (Settings, Low)
7. Selective Backups (Performance, Medium)
8. Enable Biometric Lock (Security, Medium)
9. Use Quick Action Widget (Productivity, Low)
10. Check Backup Logs (Best Practice, Medium)

#### Usage:
```kotlin
// Get tip of the day
tipsManager.getTipOfTheDay()?.let { tip ->
    displayTip(tip)
}

// Get tips by category
val securityTips = tipsManager.getTipsByCategory(TipCategory.SECURITY)

// Dismiss a tip
tipsManager.dismissTip(tipId)

// Reset all tips
tipsManager.resetTips()
```

#### UI:
- Access via: Community → Tips & Tricks
- Tip of the Day card at top
- Filter by category
- Dismiss individual tips
- Reset all from menu

---

## 📱 User Interface

### Navigation Structure

```
Main Menu
└── Community
    ├── Community Channels
    │   ├── Discord
    │   ├── Reddit
    │   ├── GitHub
    │   ├── Documentation
    │   └── Support
    │
    ├── Feedback & Support
    │   ├── Send Feedback → FeedbackScreen
    │   ├── Changelog → ChangelogScreen
    │   └── Tips & Tricks → TipsScreen
    │
    ├── Beta Program
    │   ├── Enrollment Card
    │   └── Channel Selector
    │
    └── Privacy
        └── Analytics Toggle
```

### Screen Routes

- `/community` - Main community hub
- `/feedback` - Submit and view feedback
- `/changelog` - Version history
- `/tips` - Tips and tricks
- `/onboarding` - First-time user tutorial

---

## 🔧 Technical Architecture

### Dependency Injection (Hilt)

All managers are singleton-scoped and injected via Hilt:

```kotlin
@Singleton
class FeedbackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: ObsidianLogger,
    private val analyticsManager: AnalyticsManager
)
```

### Data Storage

- **DataStore**: User preferences (analytics, beta enrollment, onboarding status)
- **In-Memory**: Temporary data (feedback submissions, tips)
- **Firebase**: Analytics and crash reports (opt-in)

### State Management

All managers expose `StateFlow` for reactive UI:

```kotlin
val analyticsEnabled: Flow<Boolean>
val betaEnrolled: Flow<Boolean>
val feedbackList: StateFlow<List<FeedbackItem>>
```

---

## 🔐 Privacy & Security

### Data Collection Policy

#### What We Collect (with consent):
- App usage patterns (screens viewed, features used)
- Crash reports and error logs
- Performance metrics (backup duration, file counts)
- Device information (manufacturer, model, Android version)

#### What We NEVER Collect:
- Personal names or emails (unless voluntarily provided in feedback)
- File contents or names
- Cloud credentials
- IP addresses
- Location data
- Contacts or personal data

### User Controls

1. **Analytics**: Opt-in toggle in Community screen
2. **Crash Reporting**: Can be disabled in settings
3. **Feedback Email**: Optional field
4. **Beta Program**: Opt-in enrollment

### Data Anonymization

All shared data is anonymized:
- SHA-256 hashing for identifiers
- PII filtering in analytics
- Sanitization of crash logs
- No cross-user correlation

---

## 🚀 Setup Instructions

### Firebase Setup

1. **Create Firebase Project**:
   ```
   https://console.firebase.google.com
   ```

2. **Add Android App**:
   - Package name: `com.obsidianbackup`
   - Download `google-services.json`
   - Place in `/app/` directory

3. **Enable Services**:
   - Firebase Analytics
   - Firebase Crashlytics

4. **Update Configuration**:
   ```json
   {
     "project_id": "your-project-id",
     "package_name": "com.obsidianbackup",
     "api_key": "your-api-key"
   }
   ```

### Build Configuration

Dependencies are already added:

```kotlin
// Firebase BOM
implementation(platform(libs.firebase.bom))
implementation(libs.firebase.crashlytics)
implementation(libs.firebase.analytics)
```

Plugins:
```kotlin
alias(libs.plugins.google.services)
alias(libs.plugins.firebase.crashlytics)
```

### Testing

1. **Test Feedback System**:
   ```kotlin
   // Navigate to Community → Send Feedback
   // Submit test feedback
   ```

2. **Test Analytics** (Debug):
   ```bash
   adb shell setprop debug.firebase.analytics.app com.obsidianbackup
   ```

3. **Test Crashlytics**:
   ```kotlin
   // Force a test crash
   throw RuntimeException("Test Crash")
   ```

4. **Verify in Firebase Console**:
   - Analytics → Events
   - Crashlytics → Dashboard

---

## 📊 Analytics Events

### Tracked Events

| Event Name | Parameters | Description |
|------------|------------|-------------|
| `feedback_submitted` | type, has_email, has_logs | User submits feedback |
| `beta_enrolled` | channel | User joins beta program |
| `beta_left` | - | User leaves beta |
| `onboarding_started` | - | User begins onboarding |
| `onboarding_completed` | - | User completes onboarding |
| `onboarding_skipped` | - | User skips onboarding |
| `backup_operation` | action, type, success, duration_ms | Backup event |
| `screen_view` | screen_name | Screen navigation |

### Custom Properties

- `beta_channel`: Current beta channel
- `analytics_enabled`: Analytics status
- `app_version`: Current version

---

## 🎨 UI Components

### Material Design 3

All screens use Material 3 components:
- `Card`, `OutlinedCard`
- `TopAppBar`, `BottomAppBar`
- `FloatingActionButton`
- `Button`, `TextButton`, `IconButton`
- `Switch`, `Checkbox`, `FilterChip`
- `AlertDialog`, `ExposedDropdownMenu`

### Compose Architecture

- **State Hoisting**: ViewModels manage state
- **Reactive UI**: StateFlow/Flow observation
- **Navigation**: Jetpack Navigation Compose
- **Dependency Injection**: Hilt ViewModel integration

---

## 🧪 Testing Checklist

### Feature Testing

- [ ] Submit bug report with logs
- [ ] Submit feature request with email
- [ ] View feedback history
- [ ] Enroll in beta program
- [ ] Switch beta channels
- [ ] Leave beta program
- [ ] View changelog entries
- [ ] Dismiss tips
- [ ] View tip of the day
- [ ] Complete onboarding
- [ ] Skip onboarding
- [ ] Toggle analytics
- [ ] Open Discord link
- [ ] Open Reddit link
- [ ] Open GitHub link
- [ ] Open documentation
- [ ] Open support page

### Privacy Testing

- [ ] Verify analytics is opt-in
- [ ] Confirm no PII in analytics
- [ ] Test crashlytics PII filtering
- [ ] Verify config anonymization
- [ ] Check local data storage

---

## 🔄 Future Enhancements

### Planned Features

1. **In-App Chat Support**
   - Direct messaging with support team
   - Automated responses for common issues

2. **Community Leaderboard**
   - Anonymous contribution tracking
   - Beta tester recognition

3. **Feature Voting**
   - Vote on proposed features
   - Transparent roadmap

4. **Config Marketplace**
   - Share and discover backup configs
   - Rating and commenting system

5. **Advanced Analytics Dashboard**
   - User-facing analytics
   - Backup success rates
   - Storage trends

6. **Localized Tips**
   - Tips in multiple languages
   - Region-specific best practices

---

## 📞 Support & Contribution

### Getting Help

1. **Documentation**: https://docs.obsidianbackup.app
2. **Discord**: https://discord.gg/obsidianbackup
3. **GitHub Issues**: https://github.com/obsidianbackup/obsidianbackup/issues
4. **Email**: support@obsidianbackup.app

### Contributing

1. Fork the repository
2. Create feature branch
3. Implement community features
4. Submit pull request
5. Join our Discord for discussions

---

## 📄 License

Community features are part of ObsidianBackup and follow the project's license.

---

## 🎉 Acknowledgments

Built with privacy-first principles and user feedback at the core. Special thanks to our beta testers and community members!

**Last Updated**: 2024-01-15
**Version**: 1.0.0
**Maintainer**: ObsidianBackup Team
