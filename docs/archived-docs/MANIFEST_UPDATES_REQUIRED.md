# AndroidManifest.xml Updates Required

## Required Manifest Changes

### 1. Widget Declarations

Add to `app/src/main/AndroidManifest.xml`:

```xml
<application>
    <!-- Existing content -->
    
    <!-- Quick Backup Widget -->
    <receiver 
        android:name=".widget.BackupWidget"
        android:exported="true"
        android:label="Quick Backup">
        <intent-filter>
            <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
        </intent-filter>
        <meta-data
            android:name="android.appwidget.provider"
            android:resource="@xml/widget_backup_info" />
    </receiver>

    <!-- Backup Status Widget -->
    <receiver 
        android:name=".widget.BackupStatusWidget"
        android:exported="true"
        android:label="Backup Status">
        <intent-filter>
            <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
        </intent-filter>
        <meta-data
            android:name="android.appwidget.provider"
            android:resource="@xml/widget_backup_status_info" />
    </receiver>

</application>
```

### 2. Deep Link Support (Already Exists)

Verify deep link intent filter exists in MainActivity:

```xml
<activity 
    android:name=".MainActivity"
    android:exported="true">
    
    <!-- Existing launcher intent -->
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    
    <!-- Deep link support (should already exist) -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="obsidianbackup" />
    </intent-filter>
    
</activity>
```

### 3. Health Connect Permissions

Add health permissions:

```xml
<!-- Health Connect Permissions -->
<uses-permission android:name="android.permission.health.READ_STEPS" />
<uses-permission android:name="android.permission.health.READ_HEART_RATE" />
<uses-permission android:name="android.permission.health.READ_SLEEP" />
<uses-permission android:name="android.permission.health.READ_EXERCISE" />
<uses-permission android:name="android.permission.health.READ_NUTRITION" />
<uses-permission android:name="android.permission.health.READ_WEIGHT" />
<uses-permission android:name="android.permission.health.READ_HEIGHT" />
<uses-permission android:name="android.permission.health.READ_BODY_FAT" />
<uses-permission android:name="android.permission.health.READ_BLOOD_PRESSURE" />
<uses-permission android:name="android.permission.health.READ_BLOOD_GLUCOSE" />
```

### 4. Feature Metadata

Add feature support metadata:

```xml
<application>
    <!-- Gaming support -->
    <meta-data
        android:name="gaming_backup_support"
        android:value="true" />
    
    <!-- Health Connect support -->
    <meta-data
        android:name="health_connect_support"
        android:value="true" />
    
    <!-- Plugin system -->
    <meta-data
        android:name="plugin_system_support"
        android:value="true" />
</application>
```

### 5. Query Intent for Gaming

Add queries for emulator detection:

```xml
<queries>
    <!-- Emulator packages for gaming backup -->
    <package android:name="com.retroarch" />
    <package android:name="com.retroarch.aarch64" />
    <package android:name="org.ppsspp.ppsspp" />
    <package android:name="org.ppsspp.ppssppgold" />
    <package android:name="org.dolphinemu.dolphinemu" />
    <package android:name="com.dsemu.drastic" />
    <package android:name="com.drastic.free" />
    <package android:name="com.epsxe.ePSXe" />
    <package android:name="com.citra.citra_emu" />
    <package android:name="org.yuzu.yuzu_emu" />
    <package android:name="xyz.aethersx2.android" />
    
    <!-- Play Games -->
    <package android:name="com.google.android.play.games" />
    
    <!-- Tasker -->
    <package android:name="net.dinglisch.android.taskerm" />
</queries>
```

## Widget XML Resources Required

### Create `app/src/main/res/xml/widget_backup_info.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="110dp"
    android:minHeight="40dp"
    android:updatePeriodMillis="0"
    android:initialLayout="@layout/widget_backup"
    android:description="@string/widget_backup_description"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen"
    android:previewImage="@drawable/widget_backup_preview">
</appwidget-provider>
```

### Create `app/src/main/res/xml/widget_backup_status_info.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider 
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="250dp"
    android:minHeight="110dp"
    android:updatePeriodMillis="1800000"
    android:initialLayout="@layout/widget_backup_status"
    android:description="@string/widget_backup_status_description"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen"
    android:previewImage="@drawable/widget_backup_status_preview">
</appwidget-provider>
```

## Widget Layout Resources Required

### Create `app/src/main/res/layout/widget_backup.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="horizontal"
    android:padding="8dp"
    android:gravity="center">

    <Button
        android:id="@+id/backup_button"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:text="@string/widget_quick_backup"
        android:textSize="12sp" />

    <Button
        android:id="@+id/open_app_button"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="4dp"
        android:text="@string/widget_open_app"
        android:textSize="12sp" />

</LinearLayout>
```

### Create `app/src/main/res/layout/widget_backup_status.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/widget_root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp"
    android:background="@drawable/widget_background">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/widget_backup_status_title"
        android:textSize="16sp"
        android:textStyle="bold"
        android:textColor="?android:textColorPrimary" />

    <TextView
        android:id="@+id/last_backup_text"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:text="Last backup: Never"
        android:textSize="14sp"
        android:textColor="?android:textColorSecondary" />

    <TextView
        android:id="@+id/backup_count_text"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="4dp"
        android:text="0 backups"
        android:textSize="14sp"
        android:textColor="?android:textColorSecondary" />

</LinearLayout>
```

## String Resources Required

### Add to `app/src/main/res/values/strings.xml`:

```xml
<!-- Widget strings -->
<string name="widget_backup_description">Quick backup from home screen</string>
<string name="widget_backup_status_description">Display last backup status</string>
<string name="widget_quick_backup">Quick Backup</string>
<string name="widget_open_app">Open</string>
<string name="widget_backup_status_title">Backup Status</string>

<!-- New screen titles (if not already present) -->
<string name="screen_gaming">Gaming</string>
<string name="screen_health">Health</string>
<string name="screen_plugins">Plugins</string>

<!-- Content descriptions for accessibility -->
<string name="cd_nav_gaming">Navigate to gaming backups</string>
<string name="cd_nav_health">Navigate to health data</string>
<string name="cd_nav_plugins">Navigate to plugins</string>
```

## Drawable Resources Required

Create placeholder drawables (or use Material icons):

```
app/src/main/res/drawable/
├── widget_background.xml
├── widget_backup_preview.png
└── widget_backup_status_preview.png
```

### `widget_background.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="?android:colorBackground" />
    <corners android:radius="16dp" />
    <padding
        android:left="8dp"
        android:top="8dp"
        android:right="8dp"
        android:bottom="8dp" />
</shape>
```

## Checklist

- [ ] Add widget receivers to AndroidManifest.xml
- [ ] Create widget XML provider files
- [ ] Create widget layout files
- [ ] Add string resources
- [ ] Add drawable resources
- [ ] Add health permissions
- [ ] Add emulator query intents
- [ ] Verify deep link intent filter exists
- [ ] Add feature metadata
- [ ] Test widget installation
- [ ] Test deep links
- [ ] Test health permissions

## Notes

1. **Widget layouts** use RemoteViews, limited to simple layouts
2. **Health permissions** require runtime permission requests
3. **Query intents** needed for Android 11+ package visibility
4. **Preview images** should be screenshots of actual widgets

---

**Priority**: Medium  
**Impact**: Widgets and health features won't work without these  
**Effort**: 30 minutes to add all resources
