# Deep Linking Implementation - Quick Reference

## 📋 Overview

This implementation provides a comprehensive deep linking system for ObsidianBackup Android app with:

- ✅ Custom URI scheme (`obsidianbackup://`)
- ✅ HTTPS App Links (`https://obsidianbackup.app/`)
- ✅ Biometric authentication for sensitive operations
- ✅ Complete analytics tracking
- ✅ Production-ready error handling
- ✅ Testing tools and documentation

## 🚀 Quick Start

### Testing Deep Links

```bash
# Run the test script
./test_deep_links.sh

# Or test manually with ADB
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://backup"
```

### Common Deep Links

```bash
# Start backup
obsidianbackup://backup

# Restore snapshot (requires auth)
obsidianbackup://restore?snapshot=<id>

# Open settings
obsidianbackup://settings

# Open automation settings
obsidianbackup://settings/automation

# Connect cloud provider (requires auth)
obsidianbackup://cloud/connect?provider=webdav
```

## 📁 File Structure

```
app/src/main/java/com/obsidianbackup/deeplink/
├── DeepLinkActivity.kt          # Entry point for deep links
├── DeepLinkHandler.kt           # Main coordinator
├── DeepLinkRouter.kt            # Navigation logic
├── DeepLinkParser.kt            # URI parsing
├── DeepLinkAuthenticator.kt     # Authentication handling
├── DeepLinkAnalytics.kt         # Usage tracking
├── DeepLinkModels.kt            # Data models
├── DeepLinkModule.kt            # Dagger DI module
└── DeepLinkTestActivity.kt      # Testing UI

AndroidManifest.xml              # Intent filter registration
.well-known/assetlinks.json      # App Links verification
test_deep_links.sh               # Testing script
DEEP_LINKING_GUIDE.md            # Full documentation
```

## 🔧 Architecture

### Flow Diagram

```
External Source → Intent
    ↓
DeepLinkActivity (receives intent)
    ↓
DeepLinkHandler (coordinates)
    ↓
┌──────────────────────────┐
│ DeepLinkParser           │ → Parse URI
└──────────────────────────┘
    ↓
┌──────────────────────────┐
│ DeepLinkAuthenticator    │ → Check auth requirement
└──────────────────────────┘
    ↓
┌──────────────────────────┐
│ DeepLinkRouter           │ → Execute action
└──────────────────────────┘
    ↓
┌──────────────────────────┐
│ DeepLinkAnalytics        │ → Track event
└──────────────────────────┘
    ↓
MainActivity (with extras)
```

### Key Components

1. **DeepLinkActivity** - Transparent activity that receives intents
2. **DeepLinkParser** - Converts URIs to structured actions
3. **DeepLinkAuthenticator** - Handles biometric/credential authentication
4. **DeepLinkRouter** - Routes actions to appropriate destinations
5. **DeepLinkAnalytics** - Tracks usage and errors
6. **DeepLinkHandler** - Orchestrates the entire flow

## 🔐 Authentication

### Actions Requiring Authentication

| Action | Requires Auth | Reason |
|--------|--------------|---------|
| Backup | ❌ No | Safe operation |
| Restore | ✅ Yes | Modifies device state |
| Cloud Connect | ✅ Yes | Involves credentials |
| Open Settings | ❌ No | Navigation only |
| Security Settings | ✅ Yes | Sensitive settings |

### Authentication Flow

```kotlin
if (authenticator.requiresAuthentication(action)) {
    val authenticated = authenticator.authenticate(activity, action)
    if (!authenticated) {
        return AuthenticationRequired
    }
}
```

## 📊 Analytics

All deep link events are tracked:

```kotlin
data class DeepLinkEvent(
    val timestamp: Long,
    val action: String,
    val success: Boolean,
    val authenticated: Boolean,
    val errorReason: String?,
    val durationMs: Long?
)
```

Analytics are stored in `deeplink_analytics.jsonl` and can be exported for analysis.

## 🧪 Testing

### 1. Using Test Script

```bash
./test_deep_links.sh
```

Provides interactive menu for testing all deep link patterns.

### 2. Using Test Activity

```bash
adb shell am start -n com.obsidianbackup/.deeplink.DeepLinkTestActivity
```

Visual UI for validating and launching deep links.

### 3. Manual Testing

```bash
# Test backup
adb shell am start -a android.intent.action.VIEW \
  -d "obsidianbackup://backup?packages=com.android.chrome"

# Test settings
adb shell am start -a android.intent.action.VIEW \
  -d "obsidianbackup://settings/automation"

# Check logs
adb logcat | grep DeepLink
```

## 🌐 HTTPS App Links Setup

### 1. Generate SHA-256 Fingerprint

```bash
# Debug keystore
keytool -list -v -keystore ~/.android/debug.keystore \
  -alias androiddebugkey -storepass android | grep SHA256

# Release keystore
keytool -list -v -keystore /path/to/release.keystore \
  -alias <alias> | grep SHA256
```

### 2. Update assetlinks.json

Edit `.well-known/assetlinks.json` with your SHA-256 fingerprint.

### 3. Host assetlinks.json

Upload to: `https://obsidianbackup.app/.well-known/assetlinks.json`

Requirements:
- HTTPS only
- Content-Type: `application/json`
- No redirects

### 4. Verify

```bash
# Check verification status
adb shell pm get-app-links com.obsidianbackup

# Force re-verification
adb shell pm verify-app-links --re-verify com.obsidianbackup
```

## 🔍 Troubleshooting

### Deep Links Not Opening App

1. Check app is installed:
   ```bash
   adb shell pm list packages | grep obsidianbackup
   ```

2. Check intent filters:
   ```bash
   adb shell dumpsys package com.obsidianbackup | grep -A 20 "intent-filter"
   ```

3. Test with explicit activity:
   ```bash
   adb shell am start -n com.obsidianbackup/.deeplink.DeepLinkActivity \
     -d "obsidianbackup://backup"
   ```

### App Links Opening in Browser

1. Verify assetlinks.json is accessible:
   ```bash
   curl https://obsidianbackup.app/.well-known/assetlinks.json
   ```

2. Check verification status:
   ```bash
   adb shell pm get-app-links com.obsidianbackup
   ```

3. Reset and re-verify:
   ```bash
   adb shell pm set-app-links com.obsidianbackup 0
   adb shell pm verify-app-links --re-verify com.obsidianbackup
   ```

### Authentication Not Working

1. Check biometric availability:
   - Settings → Security → Authentication Status in app

2. Verify device security:
   - Ensure device has PIN/pattern/password or biometric enrolled

3. Test non-auth actions first:
   - Try navigation actions that don't require authentication

### View Logs

```bash
# All deep link logs
adb logcat | grep -i deeplink

# Errors only
adb logcat | grep -i "deeplink.*error"

# Analytics
adb logcat | grep DeepLinkAnalytics
```

## 📚 Examples

### From Tasker

```
Action: Send Intent
  Action: android.intent.action.VIEW
  Data: obsidianbackup://backup
  Target: Activity
```

### From HTML

```html
<a href="obsidianbackup://backup">Start Backup</a>
<a href="https://obsidianbackup.app/backup">Start Backup (App Link)</a>
```

### From Android Code

```kotlin
val intent = Intent(Intent.ACTION_VIEW).apply {
    data = Uri.parse("obsidianbackup://backup?packages=com.example.app")
}
startActivity(intent)
```

### From Command Line

```bash
# Chrome browser
adb shell am start -a android.intent.action.VIEW \
  -d "obsidianbackup://backup" \
  com.android.chrome

# Direct to app
adb shell am start -a android.intent.action.VIEW \
  -d "obsidianbackup://backup" \
  com.obsidianbackup
```

## 🔒 Security Best Practices

1. **Never include sensitive data in URIs**
   - ❌ `obsidianbackup://restore?password=secret`
   - ✅ `obsidianbackup://restore?snapshot=id123`

2. **Always validate input**
   - All parameters are validated before processing
   - Invalid package names are rejected
   - Unknown actions return errors

3. **Require authentication for sensitive operations**
   - Restore operations
   - Cloud provider connections
   - Security settings access

4. **Log all deep link activity**
   - Track success and failures
   - Monitor for suspicious patterns
   - Export analytics for review

## 📖 Full Documentation

See [DEEP_LINKING_GUIDE.md](DEEP_LINKING_GUIDE.md) for complete documentation including:

- All supported URI patterns
- Detailed parameter documentation
- Integration examples
- API reference
- Advanced troubleshooting

## 🎯 Supported Actions

| Category | Action | URI Pattern | Auth Required |
|----------|--------|-------------|---------------|
| Backup | Start Backup | `backup` | No |
| Backup | Backup Apps | `backup?packages=...` | No |
| Restore | Restore Snapshot | `restore?snapshot=...` | Yes |
| Navigation | Dashboard | `dashboard` | No |
| Navigation | Backups | `backups` | No |
| Navigation | Settings | `settings` | No |
| Settings | Automation | `settings/automation` | No |
| Settings | Cloud | `settings/cloud` | No |
| Settings | Security | `settings/security` | Yes |
| Cloud | Connect | `cloud/connect?provider=...` | Yes |
| Cloud | Settings | `cloud/settings` | No |
| App | Details | `app?package=...` | No |

## 🚦 Status Codes

Deep links return one of three results:

1. **Success** - Action completed successfully
   ```kotlin
   DeepLinkResult.Success(action, metadata)
   ```

2. **AuthenticationRequired** - Authentication failed or cancelled
   ```kotlin
   DeepLinkResult.AuthenticationRequired(action, reason)
   ```

3. **Error** - Invalid URI or execution error
   ```kotlin
   DeepLinkResult.Error(reason, originalUri)
   ```

## 💡 Tips

- Use URL encoding for special characters in parameters
- Add `?source=<name>` to track deep link sources
- Test on multiple Android versions (API 24+)
- Monitor analytics to understand usage patterns
- Keep authentication timeout reasonable (30s default)

## 📞 Support

For issues or questions:

1. Check logs: `adb logcat | grep DeepLink`
2. Run test suite: `./test_deep_links.sh`
3. Use test activity: `DeepLinkTestActivity`
4. Review full documentation: `DEEP_LINKING_GUIDE.md`

---

**Version:** 1.0.0  
**Last Updated:** 2024  
**Maintainer:** ObsidianBackup Team
