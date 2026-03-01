# Plugin Development Guide

Create custom automation plugins for ObsidianBackup.

## Overview

ObsidianBackup's plugin system allows you to extend automation capabilities with custom logic. Plugins can:

- Implement custom backup schedules
- React to system events
- Add conditional backup logic
- Integrate with third-party services
- Create custom workflows

## Plugin Architecture

### Plugin Interface

```kotlin
interface AutomationPlugin {
    /**
     * Unique plugin identifier (reverse domain notation)
     */
    val id: String
    
    /**
     * Human-readable plugin name
     */
    val name: String
    
    /**
     * Plugin version (semantic versioning)
     */
    val version: String
    
    /**
     * Plugin description
     */
    val description: String
    
    /**
     * Minimum ObsidianBackup version required
     */
    val minAppVersion: String
    
    /**
     * Determine if backup should be triggered
     * 
     * @param context Current automation context
     * @return true if backup should run, false otherwise
     */
    suspend fun shouldTrigger(context: AutomationContext): Boolean
    
    /**
     * Execute the backup automation
     * 
     * @param context Current automation context
     */
    suspend fun execute(context: AutomationContext)
    
    /**
     * Provide plugin configuration UI
     * 
     * @return Plugin configuration definition
     */
    fun configure(): PluginConfig
    
    /**
     * Called when plugin is loaded
     */
    fun onLoad() {}
    
    /**
     * Called when plugin is unloaded
     */
    fun onUnload() {}
}
```

### Automation Context

```kotlin
data class AutomationContext(
    val appContext: Context,
    val backupManager: BackupManager,
    val config: PluginConfiguration,
    val systemInfo: SystemInfo,
    val trigger: Trigger
)

data class SystemInfo(
    val batteryLevel: Int,
    val isCharging: Boolean,
    val networkType: NetworkType,
    val availableStorage: Long,
    val dayOfWeek: DayOfWeek,
    val timeOfDay: LocalTime
)

sealed class Trigger {
    object Schedule : Trigger()
    data class Event(val eventType: String) : Trigger()
    object Manual : Trigger()
}
```

### Plugin Configuration

```kotlin
data class PluginConfig(
    val settings: List<Setting>
)

sealed class Setting {
    abstract val key: String
    abstract val label: String
    abstract val description: String?
    
    data class Toggle(
        override val key: String,
        override val label: String,
        override val description: String? = null,
        val defaultValue: Boolean = false
    ) : Setting()
    
    data class Text(
        override val key: String,
        override val label: String,
        override val description: String? = null,
        val defaultValue: String = "",
        val validation: ((String) -> Boolean)? = null
    ) : Setting()
    
    data class Number(
        override val key: String,
        override val label: String,
        override val description: String? = null,
        val defaultValue: Int = 0,
        val min: Int? = null,
        val max: Int? = null
    ) : Setting()
    
    data class Selection(
        override val key: String,
        override val label: String,
        override val description: String? = null,
        val options: List<Option>,
        val defaultValue: String? = null
    ) : Setting()
    
    data class Option(val value: String, val label: String)
}
```

## Getting Started

### 1. Create Plugin Project

Create a new Android library or application project:

```groovy
// build.gradle (Module)
plugins {
    id 'com.android.library'  // or 'com.android.application'
    id 'kotlin-android'
}

android {
    namespace = "com.example.myplugin"
    compileSdk = 35
    
    defaultConfig {
        minSdk = 26
        targetSdk = 35
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // ObsidianBackup API (provided at runtime)
    compileOnly 'com.obsidianbackup:plugin-api:1.0.0'
    
    // Kotlin
    implementation 'org.jetbrains.kotlin:kotlin-stdlib:1.9.0'
    
    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
}
```

### 2. Implement Plugin Interface

Create your plugin class:

```kotlin
package com.example.myplugin

import com.obsidianbackup.plugin.AutomationPlugin
import com.obsidianbackup.plugin.AutomationContext
import com.obsidianbackup.plugin.PluginConfig
import com.obsidianbackup.plugin.Setting

class MyCustomPlugin : AutomationPlugin {
    override val id = "com.example.myplugin"
    override val name = "My Custom Plugin"
    override val version = "1.0.0"
    override val description = "A custom automation plugin"
    override val minAppVersion = "1.0.0"
    
    override suspend fun shouldTrigger(context: AutomationContext): Boolean {
        // Your logic to determine if backup should run
        val enabled = context.config.getBoolean("enabled", true)
        if (!enabled) return false
        
        // Example: Only trigger on Mondays
        return context.systemInfo.dayOfWeek == DayOfWeek.MONDAY
    }
    
    override suspend fun execute(context: AutomationContext) {
        // Your backup logic
        val apps = context.config.getStringList("apps", emptyList())
        val destination = context.config.getString("destination", "local")
        
        context.backupManager.startBackup(
            apps = apps,
            destination = destination,
            type = BackupType.INCREMENTAL
        )
    }
    
    override fun configure(): PluginConfig {
        return PluginConfig(
            settings = listOf(
                Setting.Toggle(
                    key = "enabled",
                    label = "Enable Plugin",
                    description = "Enable or disable this plugin",
                    defaultValue = true
                ),
                Setting.Selection(
                    key = "destination",
                    label = "Backup Destination",
                    options = listOf(
                        Setting.Option("local", "Local Storage"),
                        Setting.Option("cloud", "Cloud Storage")
                    ),
                    defaultValue = "local"
                ),
                Setting.Number(
                    key = "min_battery",
                    label = "Minimum Battery Level",
                    description = "Minimum battery % to trigger backup",
                    defaultValue = 30,
                    min = 0,
                    max = 100
                )
            )
        )
    }
    
    override fun onLoad() {
        // Initialize plugin resources
    }
    
    override fun onUnload() {
        // Cleanup plugin resources
    }
}
```

### 3. Create Plugin Manifest

Create `plugin.json` in `assets/`:

```json
{
  "id": "com.example.myplugin",
  "name": "My Custom Plugin",
  "version": "1.0.0",
  "description": "A custom automation plugin",
  "author": "Your Name",
  "license": "GPL-3.0",
  "minAppVersion": "1.0.0",
  "mainClass": "com.example.myplugin.MyCustomPlugin",
  "permissions": [
    "android.permission.INTERNET",
    "android.permission.ACCESS_NETWORK_STATE"
  ]
}
```

### 4. Build Plugin

Build your plugin:

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/aar/myplugin-release.aar` (for library)
or `app/build/outputs/apk/release/myplugin-release.apk` (for app)

## Plugin Examples

### Example 1: Weather-Based Backup

Backup based on weather conditions:

```kotlin
class WeatherBackupPlugin : AutomationPlugin {
    override val id = "com.example.weatherbackup"
    override val name = "Weather Backup"
    override val version = "1.0.0"
    override val description = "Backup on rainy days"
    override val minAppVersion = "1.0.0"
    
    private val weatherApi = WeatherApi()
    
    override suspend fun shouldTrigger(context: AutomationContext): Boolean {
        val apiKey = context.config.getString("api_key", "")
        if (apiKey.isEmpty()) return false
        
        val location = getCurrentLocation(context.appContext)
        val weather = weatherApi.getWeather(location, apiKey)
        
        // Trigger backup if it's raining
        return weather.isRaining
    }
    
    override suspend fun execute(context: AutomationContext) {
        val apps = listOf("all")  // Backup all apps
        context.backupManager.startBackup(
            apps = apps,
            destination = "cloud",  // Upload to cloud
            type = BackupType.INCREMENTAL
        )
    }
    
    override fun configure(): PluginConfig {
        return PluginConfig(
            settings = listOf(
                Setting.Text(
                    key = "api_key",
                    label = "Weather API Key",
                    description = "OpenWeatherMap API key"
                )
            )
        )
    }
}
```

### Example 2: Smart Battery Plugin

Intelligent battery-based scheduling:

```kotlin
class SmartBatteryPlugin : AutomationPlugin {
    override val id = "com.example.smartbattery"
    override val name = "Smart Battery"
    override val version = "1.0.0"
    override val description = "Smart battery-aware backups"
    override val minAppVersion = "1.0.0"
    
    override suspend fun shouldTrigger(context: AutomationContext): Boolean {
        val minBattery = context.config.getInt("min_battery", 30)
        val requireCharging = context.config.getBoolean("require_charging", false)
        
        val battery = context.systemInfo.batteryLevel
        val charging = context.systemInfo.isCharging
        
        // Trigger if battery above minimum
        if (battery < minBattery) return false
        
        // Optionally require charging
        if (requireCharging && !charging) return false
        
        return true
    }
    
    override suspend fun execute(context: AutomationContext) {
        val battery = context.systemInfo.batteryLevel
        val charging = context.systemInfo.isCharging
        
        // Choose backup type based on battery
        val type = when {
            charging || battery > 80 -> BackupType.FULL
            battery > 50 -> BackupType.INCREMENTAL
            else -> BackupType.INCREMENTAL  // Light backup
        }
        
        // Adjust performance based on battery
        val threadCount = when {
            charging -> 8
            battery > 50 -> 4
            else -> 2
        }
        
        context.backupManager.startBackup(
            apps = listOf("all"),
            type = type,
            threadCount = threadCount
        )
    }
    
    override fun configure(): PluginConfig {
        return PluginConfig(
            settings = listOf(
                Setting.Number(
                    key = "min_battery",
                    label = "Minimum Battery %",
                    defaultValue = 30,
                    min = 0,
                    max = 100
                ),
                Setting.Toggle(
                    key = "require_charging",
                    label = "Require Charging",
                    defaultValue = false
                )
            )
        )
    }
}
```

### Example 3: Time Window Plugin

Backup only during specific time windows:

```kotlin
class TimeWindowPlugin : AutomationPlugin {
    override val id = "com.example.timewindow"
    override val name = "Time Window"
    override val version = "1.0.0"
    override val description = "Backup during specific time windows"
    override val minAppVersion = "1.0.0"
    
    override suspend fun shouldTrigger(context: AutomationContext): Boolean {
        val startTime = LocalTime.parse(
            context.config.getString("start_time", "22:00")
        )
        val endTime = LocalTime.parse(
            context.config.getString("end_time", "06:00")
        )
        val currentTime = context.systemInfo.timeOfDay
        
        // Handle overnight windows (e.g., 22:00 to 06:00)
        return if (startTime < endTime) {
            currentTime >= startTime && currentTime <= endTime
        } else {
            currentTime >= startTime || currentTime <= endTime
        }
    }
    
    override suspend fun execute(context: AutomationContext) {
        context.backupManager.startBackup(
            apps = listOf("all"),
            destination = "cloud",
            type = BackupType.INCREMENTAL
        )
    }
    
    override fun configure(): PluginConfig {
        return PluginConfig(
            settings = listOf(
                Setting.Text(
                    key = "start_time",
                    label = "Start Time",
                    description = "Format: HH:mm (24-hour)",
                    defaultValue = "22:00",
                    validation = { it.matches(Regex("\\d{2}:\\d{2}")) }
                ),
                Setting.Text(
                    key = "end_time",
                    label = "End Time",
                    description = "Format: HH:mm (24-hour)",
                    defaultValue = "06:00",
                    validation = { it.matches(Regex("\\d{2}:\\d{2}")) }
                )
            )
        )
    }
}
```

### Example 4: App Usage Plugin

Backup based on app usage patterns:

```kotlin
class AppUsagePlugin : AutomationPlugin {
    override val id = "com.example.appusage"
    override val name = "App Usage"
    override val version = "1.0.0"
    override val description = "Backup frequently used apps"
    override val minAppVersion = "1.0.0"
    
    override suspend fun shouldTrigger(context: AutomationContext): Boolean {
        // Trigger daily at configured time
        val triggerTime = LocalTime.parse(
            context.config.getString("time", "23:00")
        )
        val currentTime = context.systemInfo.timeOfDay
        
        return currentTime.hour == triggerTime.hour &&
               currentTime.minute == triggerTime.minute
    }
    
    override suspend fun execute(context: AutomationContext) {
        val threshold = context.config.getInt("usage_threshold", 10)
        
        // Get app usage stats (requires PACKAGE_USAGE_STATS permission)
        val usageStats = getAppUsageStats(context.appContext)
        
        // Filter apps used more than threshold minutes today
        val appsToBackup = usageStats
            .filter { it.usageTime > threshold * 60 * 1000 }
            .map { it.packageName }
        
        if (appsToBackup.isNotEmpty()) {
            context.backupManager.startBackup(
                apps = appsToBackup,
                type = BackupType.INCREMENTAL
            )
        }
    }
    
    override fun configure(): PluginConfig {
        return PluginConfig(
            settings = listOf(
                Setting.Text(
                    key = "time",
                    label = "Backup Time",
                    defaultValue = "23:00"
                ),
                Setting.Number(
                    key = "usage_threshold",
                    label = "Usage Threshold (minutes)",
                    description = "Backup apps used more than N minutes",
                    defaultValue = 10,
                    min = 1,
                    max = 1440
                )
            )
        )
    }
    
    private fun getAppUsageStats(context: Context): List<AppUsage> {
        // Implementation using UsageStatsManager
        // ...
    }
}

data class AppUsage(
    val packageName: String,
    val usageTime: Long
)
```

## Advanced Features

### Persistent Storage

Store plugin state:

```kotlin
class MyPlugin : AutomationPlugin {
    private lateinit var preferences: SharedPreferences
    
    override fun onLoad() {
        preferences = context.getSharedPreferences(
            "plugin_$id",
            Context.MODE_PRIVATE
        )
    }
    
    override suspend fun execute(context: AutomationContext) {
        // Save last run time
        preferences.edit()
            .putLong("last_run", System.currentTimeMillis())
            .apply()
    }
}
```

### Network Requests

Make API calls:

```kotlin
class MyPlugin : AutomationPlugin {
    private val client = OkHttpClient()
    
    override suspend fun shouldTrigger(context: AutomationContext): Boolean {
        val response = withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://api.example.com/should-backup")
                .build()
            client.newCall(request).execute()
        }
        return response.isSuccessful
    }
}
```

### Notifications

Send custom notifications:

```kotlin
class MyPlugin : AutomationPlugin {
    override suspend fun execute(context: AutomationContext) {
        // Send notification
        val notification = NotificationCompat.Builder(context.appContext, "plugin")
            .setContentTitle("Backup Started")
            .setContentText("Plugin initiated backup")
            .setSmallIcon(R.drawable.ic_backup)
            .build()
        
        val notificationManager = context.appContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }
}
```

### Error Handling

Handle errors gracefully:

```kotlin
class MyPlugin : AutomationPlugin {
    override suspend fun execute(context: AutomationContext) {
        try {
            context.backupManager.startBackup(apps = listOf("all"))
        } catch (e: BackupException) {
            // Log error
            Log.e(TAG, "Backup failed", e)
            
            // Notify user
            showErrorNotification(context, e.message)
            
            // Optionally retry
            retryBackup(context, maxRetries = 3)
        }
    }
}
```

## Testing Plugins

### Unit Tests

```kotlin
class MyPluginTest {
    @Test
    fun testShouldTrigger() = runBlocking {
        val plugin = MyCustomPlugin()
        val context = createMockContext(
            batteryLevel = 50,
            dayOfWeek = DayOfWeek.MONDAY
        )
        
        assertTrue(plugin.shouldTrigger(context))
    }
    
    @Test
    fun testConfiguration() {
        val plugin = MyCustomPlugin()
        val config = plugin.configure()
        
        assertEquals(3, config.settings.size)
        assertTrue(config.settings[0] is Setting.Toggle)
    }
}
```

### Integration Tests

Test with actual ObsidianBackup:

1. Install plugin in ObsidianBackup
2. Enable plugin
3. Trigger automation manually
4. Verify backup created
5. Check plugin logs

## Publishing Plugins

### 1. Package Plugin

```bash
./gradlew assembleRelease
```

### 2. Sign Plugin

```bash
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
    -keystore my-key.keystore \
    myplugin-release.apk \
    alias_name
```

### 3. Distribute

**Options:**
- GitHub Releases
- Direct download
- Plugin repository (coming soon)
- F-Droid
- Google Play (as companion app)

## Best Practices

1. **Error Handling**: Always handle exceptions
2. **Performance**: Avoid blocking operations
3. **Battery**: Be battery-conscious
4. **Permissions**: Request minimal permissions
5. **Configuration**: Provide sensible defaults
6. **Logging**: Log important events
7. **Testing**: Write comprehensive tests
8. **Documentation**: Document configuration options
9. **Versioning**: Follow semantic versioning
10. **Compatibility**: Test with multiple app versions

## Plugin API Reference

See [API Documentation](../api/index.html) for detailed API reference.

## Next Steps

- [Architecture Overview](architecture.md) - Understand system architecture
- [Testing Guide](testing.md) - Testing strategies
- [Code Examples](../examples/README.md) - More examples
