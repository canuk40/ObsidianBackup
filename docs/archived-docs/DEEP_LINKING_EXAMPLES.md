# Deep Linking Examples - Copy & Paste Ready

## 🚀 Quick Testing Commands

### Backup Commands
```bash
# Full backup
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://backup"

# Backup Chrome
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://backup?packages=com.android.chrome"

# Backup multiple apps
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://backup?packages=com.android.chrome,com.android.vending"

# Backup data only (no APK)
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://backup?includeApk=false"
```

### Restore Commands (Requires Auth)
```bash
# Restore snapshot
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://restore?snapshot=backup_20240101"

# Restore specific app from snapshot
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://restore?snapshot=backup_20240101&packages=com.android.chrome"
```

### Navigation Commands
```bash
# Dashboard
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://dashboard"

# Backups screen
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://backups"

# Automation
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://automation"

# Logs
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://logs"

# Settings
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://settings"
```

### Settings Commands
```bash
# Automation settings
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://settings/automation"

# Cloud settings
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://settings/cloud"

# Security settings (Requires Auth)
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://settings/security"

# Storage settings
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://settings/storage"

# About
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://settings/about"
```

### Cloud Commands (Require Auth)
```bash
# Connect WebDAV
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://cloud/connect?provider=webdav"

# Connect Nextcloud
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://cloud/connect?provider=nextcloud"

# Cloud settings
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://cloud/settings"
```

### App Commands
```bash
# Open Chrome details
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://app?package=com.android.chrome"

# Open Play Store details
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://app?package=com.android.vending"
```

## 🌐 HTML Examples

### Webpage Integration
```html
<!DOCTYPE html>
<html>
<head>
    <title>ObsidianBackup Deep Links</title>
</head>
<body>
    <h1>ObsidianBackup Actions</h1>
    
    <!-- Backup Links -->
    <h2>Backup</h2>
    <a href="obsidianbackup://backup">Start Full Backup</a><br>
    <a href="obsidianbackup://backup?packages=com.whatsapp">Backup WhatsApp</a><br>
    
    <!-- Navigation Links -->
    <h2>Navigation</h2>
    <a href="obsidianbackup://dashboard">Dashboard</a><br>
    <a href="obsidianbackup://backups">View Backups</a><br>
    <a href="obsidianbackup://automation">Automation</a><br>
    <a href="obsidianbackup://settings">Settings</a><br>
    
    <!-- Settings Links -->
    <h2>Settings</h2>
    <a href="obsidianbackup://settings/automation">Automation Settings</a><br>
    <a href="obsidianbackup://settings/cloud">Cloud Settings</a><br>
    
    <!-- App Links (HTTPS) -->
    <h2>App Links (HTTPS)</h2>
    <a href="https://obsidianbackup.app/backup">Start Backup</a><br>
    <a href="https://obsidianbackup.app/settings/automation">Automation Settings</a><br>
</body>
</html>
```

### Email Template
```html
<!DOCTYPE html>
<html>
<body style="font-family: Arial, sans-serif; padding: 20px;">
    <h2>ObsidianBackup Reminder</h2>
    <p>Time to back up your apps!</p>
    
    <div style="margin: 20px 0;">
        <a href="obsidianbackup://backup" 
           style="display: inline-block; padding: 10px 20px; 
                  background: #4CAF50; color: white; 
                  text-decoration: none; border-radius: 4px;">
            Start Backup Now
        </a>
    </div>
    
    <p>Or choose an action:</p>
    <ul>
        <li><a href="obsidianbackup://backup">Full Backup</a></li>
        <li><a href="obsidianbackup://settings/automation">Setup Automation</a></li>
        <li><a href="obsidianbackup://cloud/connect?provider=webdav">Connect Cloud Storage</a></li>
    </ul>
    
    <hr>
    <p style="font-size: 12px; color: #666;">
        Alternatively, use these HTTPS links:<br>
        <a href="https://obsidianbackup.app/backup">https://obsidianbackup.app/backup</a>
    </p>
</body>
</html>
```

## 📱 Android Code Examples

### Basic Intent
```kotlin
fun openBackupScreen(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("obsidianbackup://backup")
    }
    context.startActivity(intent)
}
```

### With Parameters
```kotlin
fun backupSpecificApps(context: Context, packages: List<String>) {
    val packagesParam = packages.joinToString(",")
    val uri = "obsidianbackup://backup?packages=$packagesParam"
    
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(uri)
    }
    context.startActivity(intent)
}
```

### Notification with Deep Link
```kotlin
fun createBackupNotification(context: Context): Notification {
    val deepLinkIntent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("obsidianbackup://backup")
    }
    
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        deepLinkIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    
    return NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle("Backup Reminder")
        .setContentText("Tap to start backup")
        .setSmallIcon(R.drawable.ic_backup)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()
}
```

### Share Deep Link
```kotlin
fun shareBackupLink(context: Context) {
    val deepLink = "obsidianbackup://backup?packages=com.example.app"
    
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, deepLink)
        putExtra(Intent.EXTRA_TITLE, "Share Backup Link")
    }
    
    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
}
```

### Compose Button Example
```kotlin
@Composable
fun BackupButton() {
    val context = LocalContext.current
    
    Button(
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("obsidianbackup://backup")
            }
            context.startActivity(intent)
        }
    ) {
        Text("Start Backup")
    }
}
```

## 🤖 Tasker Examples

### Simple Backup Task
```
Task: Start Backup
A1: Send Intent
    Action: android.intent.action.VIEW
    Data: obsidianbackup://backup
    Target: Activity
```

### Scheduled Backup (Daily at 2 AM)
```
Profile: Daily Backup
    Time: 02:00
Enter Task: Start Backup
A1: Send Intent
    Action: android.intent.action.VIEW
    Data: obsidianbackup://backup
    Target: Activity
```

### Backup When Connected to WiFi
```
Profile: WiFi Backup
    State: Wifi Connected [ SSID:HomeWiFi ]
Enter Task: Start Backup
A1: Send Intent
    Action: android.intent.action.VIEW
    Data: obsidianbackup://backup
    Target: Activity
```

### Backup Specific Apps
```
Task: Backup WhatsApp
A1: Send Intent
    Action: android.intent.action.VIEW
    Data: obsidianbackup://backup?packages=com.whatsapp
    Target: Activity
```

## 🔗 QR Code Examples

### QR Code Data (Text to Encode)

**Full Backup:**
```
obsidianbackup://backup
```

**WhatsApp Backup:**
```
obsidianbackup://backup?packages=com.whatsapp
```

**Open Settings:**
```
obsidianbackup://settings
```

**Connect WebDAV:**
```
obsidianbackup://cloud/connect?provider=webdav
```

### Generate QR Code Online
1. Go to: https://www.qr-code-generator.com/
2. Select "Text" type
3. Paste any deep link above
4. Generate and download

### Generate QR Code in Code
```kotlin
// Using ZXing library
fun generateQRCode(deepLink: String): Bitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(
        deepLink,
        BarcodeFormat.QR_CODE,
        512,
        512,
        hints
    )
    
    val width = bitMatrix.width
    val height = bitMatrix.height
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(
                x, 
                y, 
                if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            )
        }
    }
    
    return bitmap
}

// Usage
val qrBitmap = generateQRCode("obsidianbackup://backup")
```

## 📧 Email Integration Examples

### Gmail API
```kotlin
fun sendBackupReminderEmail(toEmail: String) {
    val deepLink = "obsidianbackup://backup"
    val appLink = "https://obsidianbackup.app/backup"
    
    val htmlBody = """
        <p>Time to backup your apps!</p>
        <p><a href="$deepLink">Click here to start backup</a></p>
        <p>Or use: <a href="$appLink">$appLink</a></p>
    """.trimIndent()
    
    // Send email using Gmail API or Intent
}
```

### Email Intent
```kotlin
fun sendEmailWithDeepLink(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf("user@example.com"))
        putExtra(Intent.EXTRA_SUBJECT, "ObsidianBackup Reminder")
        putExtra(
            Intent.EXTRA_TEXT,
            """
            Start your backup: obsidianbackup://backup
            Or visit: https://obsidianbackup.app/backup
            """.trimIndent()
        )
    }
    
    context.startActivity(Intent.createChooser(intent, "Send Email"))
}
```

## 🎯 Shortcuts Examples

### Static Shortcut (shortcuts.xml)
```xml
<shortcuts xmlns:android="http://schemas.android.com/apk/res/android">
    <shortcut
        android:shortcutId="start_backup"
        android:enabled="true"
        android:icon="@drawable/ic_backup"
        android:shortcutShortLabel="@string/backup"
        android:shortcutLongLabel="@string/start_backup">
        <intent
            android:action="android.intent.action.VIEW"
            android:data="obsidianbackup://backup" />
        <categories android:name="android.shortcut.conversation" />
    </shortcut>
</shortcuts>
```

### Dynamic Shortcut
```kotlin
fun createBackupShortcut(context: Context) {
    val shortcutManager = context.getSystemService(ShortcutManager::class.java)
    
    val shortcut = ShortcutInfo.Builder(context, "backup_shortcut")
        .setShortLabel("Backup")
        .setLongLabel("Start Backup")
        .setIcon(Icon.createWithResource(context, R.drawable.ic_backup))
        .setIntent(
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("obsidianbackup://backup")
            }
        )
        .build()
    
    shortcutManager?.dynamicShortcuts = listOf(shortcut)
}
```

## 🔄 Automation Tools

### MacroDroid
```
Trigger: Time/Date (Daily 2:00 AM)
Action: Launch App/Shortcut
  Type: Custom URI
  URI: obsidianbackup://backup
```

### Automate
```
Flow: Daily Backup
  Block 1: Time
    Time: 02:00
  Block 2: App Start
    URI: obsidianbackup://backup
```

### IFTTT
```
If: Time (Every day at 2:00 AM)
Then: Android Device → Launch URL
  URL: obsidianbackup://backup
```

## 🧪 Testing Commands

### Test All Patterns
```bash
# Run test script
./test_deep_links.sh

# Or test manually:
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://backup"
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://settings/automation"
adb shell am start -a android.intent.action.VIEW -d "obsidianbackup://cloud/connect?provider=webdav"
```

### Check Logs
```bash
# Filter deep link logs
adb logcat | grep -i deeplink

# Filter errors
adb logcat | grep -E "DeepLink.*[Ee]rror"

# All related logs
adb logcat | grep -E "DeepLink|MainActivity"
```

### Verify App Links
```bash
# Check verification status
adb shell pm get-app-links com.obsidianbackup

# View intent filters
adb shell dumpsys package com.obsidianbackup | grep -A 30 "intent-filter"
```

---

**Quick Reference:** See DEEP_LINKING_README.md  
**Full Guide:** See DEEP_LINKING_GUIDE.md  
**Implementation:** See DEEP_LINKING_IMPLEMENTATION.md
