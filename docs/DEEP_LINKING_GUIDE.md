# Deep Linking Guide for ObsidianBackup

## Overview

ObsidianBackup supports comprehensive deep linking functionality that allows external applications, web pages, automation tools, and emails to trigger backup operations, navigate to specific screens, and manage cloud connections.

## Supported URI Schemes

### 1. Custom URI Scheme: `obsidianbackup://`
- Works from any Android app, browser, or automation tool
- No domain verification required
- Always available

### 2. HTTPS App Links: `https://obsidianbackup.app/`
- Opens directly in the app (no browser disambiguation)
- Requires domain verification via assetlinks.json
- Better user experience for web integration

## Deep Link Patterns

### Backup Operations

#### Start Backup (All Apps)
```
obsidianbackup://backup
```
Triggers a full backup of all installed applications.

#### Backup Specific Apps
```
obsidianbackup://backup?packages=com.example.app1,com.example.app2
```
Parameters:
- `packages` (optional): Comma-separated list of package names
- `includeData` (optional): Include app data (default: true)
- `includeApk` (optional): Include APK files (default: true)

Example:
```
obsidianbackup://backup?packages=com.whatsapp&includeData=true&includeApk=false
```

### Restore Operations

#### Restore Snapshot
```
obsidianbackup://restore?snapshot=<snapshot_id>
```
**Requires Authentication** - Biometric or device credential

Parameters:
- `snapshot` (required): The snapshot ID to restore
- `packages` (optional): Restore only specific packages

Example:
```
obsidianbackup://restore?snapshot=backup_20240101_120000&packages=com.example.app
```

### Navigation

#### Open Dashboard
```
obsidianbackup://dashboard
```

#### Open Backups Screen
```
obsidianbackup://backups
```

#### Open Settings
```
obsidianbackup://settings
```

#### Open Specific Settings Screen
```
obsidianbackup://settings/<screen>
```

Available screens:
- `automation` - Automation settings
- `cloud` - Cloud provider settings
- `security` - Security settings (requires authentication)
- `storage` - Storage settings
- `notifications` - Notification settings
- `advanced` - Advanced settings
- `about` - About screen

Examples:
```
obsidianbackup://settings/automation
obsidianbackup://settings/cloud
obsidianbackup://settings/security
```

#### Open Automation
```
obsidianbackup://automation
```

#### Open Logs
```
obsidianbackup://logs
```

### Cloud Operations

#### Connect Cloud Provider
```
obsidianbackup://cloud/connect?provider=<provider>&autoConnect=<true|false>
```
**Requires Authentication**

Parameters:
- `provider` (required): Cloud provider name
  - `webdav`
  - `rclone`
  - `googledrive`
  - `dropbox`
  - `onedrive`
  - `nextcloud`
  - `owncloud`
  - `custom`
- `autoConnect` (optional): Auto-connect without user confirmation (default: false)

Example:
```
obsidianbackup://cloud/connect?provider=webdav&autoConnect=false
```

#### Open Cloud Settings
```
obsidianbackup://cloud/settings
```

### App Management

#### Open App Details
```
obsidianbackup://app?package=<package_name>
```

Parameters:
- `package` (required): Package name of the app

Example:
```
obsidianbackup://app?package=com.example.myapp
```

## HTTPS App Links

All patterns above work with HTTPS App Links:

```
https://obsidianbackup.app/backup
https://obsidianbackup.app/restore?snapshot=backup_123
https://obsidianbackup.app/settings/automation
https://obsidianbackup.app/cloud/connect?provider=webdav
```

## Security & Authentication

### Authentication Requirements

The following actions require biometric or device credential authentication:

1. **Restore Operations** - Modifies device state
2. **Cloud Provider Connection** - Involves sensitive credentials
3. **Security Settings Access** - Accesses sensitive configuration

### Authentication Configuration

Default authentication settings:
- Biometric authentication preferred (fingerprint, face unlock)
- Device credential fallback (PIN, pattern, password)
- 30-second timeout
- 3 maximum attempts

### Bypassing Authentication

Authentication can only be bypassed if:
1. Device has no biometric/credential security enabled
2. Feature is explicitly disabled in app settings (not recommended)

## Usage Examples

### From Android ADB

```bash
# Trigger backup
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://backup"

# Backup specific apps
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://backup?packages=com.whatsapp,com.telegram"

# Restore snapshot
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://restore?snapshot=backup_20240101"

# Open settings
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://settings/automation"
```

### From Web Browser

```html
<a href="obsidianbackup://backup">Start Backup</a>
<a href="obsidianbackup://settings">Open Settings</a>
<a href="https://obsidianbackup.app/backup">Start Backup (App Link)</a>
```

### From Email

Plain text or HTML emails can include deep links:

```
Trigger your backup: obsidianbackup://backup

Or use this link: https://obsidianbackup.app/backup
```

### From Tasker/Automation Apps

**Tasker Example:**
1. Create new Task
2. Add Action: "Send Intent"
3. Action: `android.intent.action.VIEW`
4. Data: `obsidianbackup://backup`

**MacroDroid Example:**
1. Add Action: "Launch App/Shortcut"
2. Select "Custom URI"
3. Enter: `obsidianbackup://backup`

### From Other Android Apps

```kotlin
val intent = Intent(Intent.ACTION_VIEW).apply {
    data = Uri.parse("obsidianbackup://backup?packages=com.example.app")
}
startActivity(intent)
```

### From QR Codes

Generate QR codes containing deep links:

```
obsidianbackup://backup
obsidianbackup://restore?snapshot=backup_123
```

Users can scan these QR codes to trigger actions.

## Analytics & Tracking

All deep link usage is tracked for analytics:

- Timestamp
- Action type
- Success/failure status
- Authentication status
- Duration
- Error messages (if any)
- Source (if provided via `?source=` parameter)

### Viewing Analytics

Analytics can be viewed in the app's logs or exported for analysis:

```kotlin
val analytics = deepLinkAnalytics.getAnalyticsSummary()
println("Total events: ${analytics.totalEvents}")
println("Success rate: ${analytics.successfulEvents.toFloat() / analytics.totalEvents}")
```

## Testing

### Manual Testing with ADB

Use the provided test script:

```bash
./test_deep_links.sh
```

Or test individual links:

```bash
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://backup"
```

### Automated Testing

The app includes a `DeepLinkTestActivity` for comprehensive testing:

```bash
adb shell am start -n com.obsidianbackup/.deeplink.DeepLinkTestActivity
```

This provides a UI to validate and launch all deep link patterns.

### Validation

Validate deep links programmatically:

```kotlin
val result = deepLinkHandler.validateDeepLink(Uri.parse("obsidianbackup://backup"))
if (result.valid) {
    println("Action: ${result.action}")
    println("Requires auth: ${result.requiresAuth}")
} else {
    println("Error: ${result.errorMessage}")
}
```

## Setting Up HTTPS App Links

### 1. Generate SHA-256 Fingerprint

```bash
# For debug key
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android

# For release key
keytool -list -v -keystore /path/to/release.keystore -alias <your_alias>
```

### 2. Update assetlinks.json

Edit `.well-known/assetlinks.json`:

```json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "com.obsidianbackup",
      "sha256_cert_fingerprints": [
        "YOUR_ACTUAL_SHA256_FINGERPRINT_HERE"
      ]
    }
  }
]
```

### 3. Host assetlinks.json

Upload to: `https://obsidianbackup.app/.well-known/assetlinks.json`

Requirements:
- Must be served over HTTPS
- Content-Type: `application/json`
- Must be accessible without redirects

### 4. Verify App Links

```bash
# Test on device
adb shell pm get-app-links com.obsidianbackup

# Request verification
adb shell pm verify-app-links --re-verify com.obsidianbackup

# Check status
adb shell pm get-app-links --user cur com.obsidianbackup
```

## Error Handling

### Common Errors

1. **"Invalid scheme"**
   - URI doesn't use `obsidianbackup://` or `https://obsidianbackup.app/`

2. **"Missing required parameter"**
   - Required parameters (e.g., `snapshot` for restore) not provided

3. **"Authentication failed"**
   - User cancelled biometric prompt or authentication timed out

4. **"Unknown path"**
   - Unsupported deep link path

### Error Responses

Failed deep links show a toast message with the error and log the failure:

```
Failed to process deep link: Missing snapshot ID
```

Check logs for detailed error information:

```bash
adb logcat | grep DeepLink
```

## Best Practices

### 1. URL Encoding

Always URL-encode parameters:

```kotlin
val packages = URLEncoder.encode("com.app1,com.app2", "UTF-8")
val uri = "obsidianbackup://backup?packages=$packages"
```

### 2. Source Tracking

Add `source` parameter for analytics:

```
obsidianbackup://backup?source=email_campaign
obsidianbackup://backup?source=automation_tool
```

### 3. User Experience

- Always provide feedback for long-running operations
- Show authentication prompts with clear messaging
- Handle errors gracefully with user-friendly messages

### 4. Security

- Never include sensitive data in deep links
- Always require authentication for destructive operations
- Validate all input parameters

### 5. Testing

- Test on multiple Android versions
- Test with and without authentication enabled
- Test error cases and edge conditions
- Verify analytics tracking

## Troubleshooting

### Deep Links Not Working

1. **Verify app is installed**
   ```bash
   adb shell pm list packages | grep obsidianbackup
   ```

2. **Check intent filters**
   ```bash
   adb shell dumpsys package com.obsidianbackup
   ```

3. **Test with explicit intent**
   ```bash
   adb shell am start -n com.obsidianbackup/.deeplink.DeepLinkActivity -d "obsidianbackup://backup"
   ```

### App Links Not Opening in App

1. **Verify assetlinks.json**
   ```bash
   curl https://obsidianbackup.app/.well-known/assetlinks.json
   ```

2. **Check verification status**
   ```bash
   adb shell pm get-app-links com.obsidianbackup
   ```

3. **Reset app link preferences**
   ```bash
   adb shell pm set-app-links com.obsidianbackup 0
   adb shell pm verify-app-links --re-verify com.obsidianbackup
   ```

### Authentication Issues

1. **Check biometric availability**
   - Go to Settings → Security → Authentication Status

2. **Test without authentication requirement**
   - Use non-sensitive actions (backup, navigation)

3. **Check device security settings**
   - Ensure device has PIN/pattern/password or biometric enrolled

## API Reference

### DeepLinkHandler

```kotlin
suspend fun handleDeepLink(
    uri: Uri,
    activity: FragmentActivity,
    authConfig: DeepLinkAuthConfig = DeepLinkAuthConfig()
): DeepLinkResult

fun validateDeepLink(uri: Uri): DeepLinkValidationResult

fun generateDeepLink(action: DeepLinkAction): Uri?
```

### DeepLinkAction Types

- `StartBackup`
- `RestoreSnapshot`
- `OpenDashboard`
- `OpenBackups`
- `OpenSettings`
- `OpenSettingsScreen`
- `OpenAutomation`
- `OpenLogs`
- `ConnectCloudProvider`
- `OpenCloudSettings`
- `OpenAppDetails`
- `Invalid`

### DeepLinkResult Types

- `Success` - Action completed successfully
- `AuthenticationRequired` - Authentication failed or cancelled
- `Error` - Invalid URI or execution error

## Integration Examples

### Backup Scheduler

```kotlin
fun scheduleBackup() {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("obsidianbackup://backup?source=scheduler")
    }
    context.startActivity(intent)
}
```

### Notification Action

```kotlin
val backupIntent = Intent(Intent.ACTION_VIEW, Uri.parse("obsidianbackup://backup"))
val pendingIntent = PendingIntent.getActivity(context, 0, backupIntent, PendingIntent.FLAG_IMMUTABLE)

val notification = NotificationCompat.Builder(context, CHANNEL_ID)
    .setContentTitle("Time to Backup")
    .setContentText("Tap to start backup")
    .setContentIntent(pendingIntent)
    .build()
```

### Widget Action

```kotlin
val intent = Intent(Intent.ACTION_VIEW).apply {
    data = Uri.parse("obsidianbackup://backup")
    flags = Intent.FLAG_ACTIVITY_NEW_TASK
}

val pendingIntent = PendingIntent.getActivity(
    context, 
    0, 
    intent, 
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
)

remoteViews.setOnClickPendingIntent(R.id.backup_button, pendingIntent)
```

## Support

For issues, questions, or feature requests related to deep linking:

1. Check the logs: `adb logcat | grep DeepLink`
2. Review analytics: Settings → Advanced → Deep Link Analytics
3. Test with the built-in test activity
4. Verify authentication settings

## Changelog

### Version 1.0.0
- Initial deep linking implementation
- Support for custom URI scheme (`obsidianbackup://`)
- HTTPS App Links integration
- Biometric authentication
- Comprehensive analytics
- Test activity and documentation
