# Firebase Setup for Community Features

## Overview

ObsidianBackup's community engagement features use Firebase for:
- **Crashlytics**: Crash reporting and error tracking
- **Analytics**: Privacy-respecting usage analytics (opt-in)

## Quick Setup

### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click "Add project"
3. Name it "ObsidianBackup" (or your preferred name)
4. Disable Google Analytics if not needed (optional)
5. Click "Create project"

### 2. Add Android App

1. In Firebase Console, click "Add app" → Android
2. Enter package name: `com.obsidianbackup`
3. Enter app nickname: "ObsidianBackup"
4. Leave SHA-1 empty (optional for now)
5. Click "Register app"

### 3. Download Configuration File

1. Download `google-services.json`
2. Replace the placeholder file at:
   ```
   /app/google-services.json
   ```

### 4. Enable Firebase Services

#### Crashlytics:
1. In Firebase Console, go to "Crashlytics"
2. Click "Enable Crashlytics"
3. Follow the setup instructions

#### Analytics (Optional):
1. In Firebase Console, go to "Analytics"
2. Enable Analytics if desired
3. Configure data sharing settings

## Configuration File Structure

Your `google-services.json` should look like this:

```json
{
  "project_info": {
    "project_number": "123456789000",
    "project_id": "obsidianbackup-xxxxx",
    "storage_bucket": "obsidianbackup-xxxxx.appspot.com"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:123456789000:android:xxxxx",
        "android_client_info": {
          "package_name": "com.obsidianbackup"
        }
      },
      "oauth_client": [],
      "api_key": [
        {
          "current_key": "AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": []
        }
      }
    }
  ],
  "configuration_version": "1"
}
```

## Testing

### Test Crashlytics

1. Build and run the app
2. Force a test crash:
   ```kotlin
   throw RuntimeException("Test Crash for Crashlytics")
   ```
3. Restart the app
4. Check Firebase Console → Crashlytics (reports appear within 5 minutes)

### Test Analytics

1. Enable analytics in the app:
   - Go to Community → Privacy
   - Toggle "Privacy-Respecting Analytics"

2. Use the app normally

3. Enable debug mode:
   ```bash
   adb shell setprop debug.firebase.analytics.app com.obsidianbackup
   ```

4. Check Firebase Console → Analytics → DebugView

5. Verify events appear

## Privacy Configuration

### Disable Data Collection by Default

The app is configured to:
- ✅ Disable analytics by default (opt-in required)
- ✅ Allow users to disable crash reporting
- ✅ Filter all PII from reports
- ✅ Provide transparency about data collection

### User Controls

Users can control data collection in:
- **Community Screen** → Privacy section
- Analytics toggle
- Crash reporting can be disabled (requires code change)

## Without Firebase (Development Only)

If you don't want to set up Firebase:

1. Comment out Firebase dependencies in `app/build.gradle.kts`:
   ```kotlin
   // implementation(platform(libs.firebase.bom))
   // implementation(libs.firebase.crashlytics)
   // implementation(libs.firebase.analytics)
   ```

2. Comment out plugins:
   ```kotlin
   // alias(libs.plugins.google.services)
   // alias(libs.plugins.firebase.crashlytics)
   ```

3. Remove Firebase initialization from `ObsidianBackupApplication.kt`:
   ```kotlin
   // crashlyticsManager.initialize(crashReportingEnabled = true)
   ```

**Note**: Some community features will not work without Firebase.

## Production Deployment

### Status: ✅ CONFIGURED (2026-02-20)
`google-services.json` now contains real credentials for Firebase project `obsidianbackup-prod`.

### Before Release:

1. ✅ Replace placeholder `google-services.json` with production config — **DONE** (project: obsidianbackup-prod)
2. ✅ Enable ProGuard/R8 rules for Firebase
3. ✅ Test crash reporting in production build
4. ✅ Verify analytics collection works
5. ✅ Review Firebase security rules
6. ✅ Set up alerts for crashes in Firebase Console

### ProGuard Rules

Add to `proguard-rules.pro` if needed:
```proguard
# Firebase
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Crashlytics
-keepattributes *Annotation*
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**
```

## Troubleshooting

### Build Fails

**Error**: "google-services.json not found"
- **Solution**: Make sure the file is at `/app/google-services.json`

**Error**: "Unable to resolve firebase-crashlytics"
- **Solution**: Sync Gradle and rebuild

### Crashes Not Appearing

1. Wait 5-10 minutes for first report
2. Ensure app was restarted after crash
3. Check internet connection
4. Verify Crashlytics is enabled in Firebase Console

### Analytics Not Working

1. Verify analytics is enabled in app settings
2. Check debug mode is enabled for testing
3. Wait 24 hours for data to appear (or use DebugView)
4. Ensure `google-services.json` is properly configured

## Support

For Firebase-specific issues:
- [Firebase Documentation](https://firebase.google.com/docs/android/setup)
- [Crashlytics Documentation](https://firebase.google.com/docs/crashlytics)
- [Analytics Documentation](https://firebase.google.com/docs/analytics)

For app-specific issues:
- See `COMMUNITY_FEATURES.md`
- Join our Discord: https://discord.gg/obsidianbackup

## Security Notes

### API Key Security

- The API key in `google-services.json` is safe to commit
- It's used only to identify your Firebase project
- Restrict it in Firebase Console → Project Settings → Cloud API Keys

### Data Privacy

- Review our privacy policy
- All PII is automatically filtered
- Users have full control over data collection
- Analytics is opt-in by default

---

**Last Updated**: 2024-01-15
**Firebase SDK Version**: 32.7.0
