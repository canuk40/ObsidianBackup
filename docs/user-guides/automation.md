# Automation Guide

Automate your backups with ObsidianBackup's flexible automation system.

## Overview

ObsidianBackup provides a powerful plugin-based automation system that allows you to:

- Schedule backups at specific times
- Trigger backups based on events
- Create custom automation rules
- Develop your own automation plugins

## Built-in Automation

### Scheduled Backups

Create time-based backup schedules.

#### Creating a Schedule

1. Navigate to **Automation** screen
2. Tap **Create Schedule**
3. Configure schedule:
   - **Name**: Descriptive name
   - **Frequency**: Daily, Weekly, Monthly, Custom
   - **Time**: Specific time of day
   - **Apps**: Which apps to backup
   - **Destination**: Local or cloud
4. Set conditions (optional):
   - Minimum battery level
   - Network type (Wi-Fi only)
   - Charging required
5. Enable the schedule

#### Schedule Examples

**Daily Backup**
```
Name: Daily Evening Backup
Frequency: Daily
Time: 11:00 PM
Apps: All user apps
Destination: Google Drive
Conditions:
  - Wi-Fi only
  - Battery > 30%
```

**Weekly Full Backup**
```
Name: Sunday Full Backup
Frequency: Weekly (Sunday)
Time: 2:00 AM
Apps: All apps + system
Destination: Local + Cloud
Conditions:
  - Charging
  - Wi-Fi only
```

**Monthly Archive**
```
Name: Monthly Archive
Frequency: Monthly (1st day)
Time: 3:00 AM
Apps: All apps
Backup Type: Full (not incremental)
Destination: S3 (via Rclone)
Retention: Keep 12 months
```

### Event-Based Triggers

Trigger backups based on system events.

#### Available Events

**Installation Events:**
- App installed
- App uninstalled
- App updated

**System Events:**
- Device boot
- Device shutdown
- Screen on/off
- Battery state change

**Network Events:**
- Wi-Fi connected
- Wi-Fi disconnected
- Network available
- Network lost

#### Creating Event Triggers

1. Navigate to **Automation** → **Triggers**
2. Tap **Add Trigger**
3. Select event type
4. Configure action:
   - Backup specific apps
   - Backup all apps
   - Sync to cloud
   - Run custom script
5. Set conditions (optional)
6. Enable trigger

#### Trigger Examples

**App Update Backup**
```
Event: App Updated
Action: Backup updated app
Destination: Local
Conditions: None
```

**Wi-Fi Sync**
```
Event: Wi-Fi Connected
Action: Sync pending backups to cloud
Delay: 5 minutes
Conditions: Battery > 20%
```

## Automation Plugins

Extend automation with custom plugins.

### Plugin System Overview

ObsidianBackup uses a plugin architecture for automation:

```
AutomationPlugin Interface
├── TimeBasedPlugin
├── EventBasedPlugin
├── ConditionBasedPlugin
└── CustomPlugin (user-developed)
```

### Built-in Plugins

#### 1. DefaultAutomationPlugin

Basic scheduling and triggering.

**Features:**
- Daily/weekly/monthly schedules
- Time-based execution
- Basic conditions
- Simple configuration

**Configuration:**
```kotlin
{
  "schedule": "daily",
  "time": "23:00",
  "apps": ["all"],
  "conditions": {
    "wifi_only": true,
    "min_battery": 30
  }
}
```

#### 2. SmartBackupPlugin

Intelligent backup scheduling.

**Features:**
- Learns usage patterns
- Optimizes backup timing
- Predicts storage needs
- Adapts to user behavior

**Configuration:**
```kotlin
{
  "learning_period": 7,  // days
  "backup_priority": "high",
  "optimization_mode": "battery" | "speed" | "balanced"
}
```

#### 3. CloudSyncPlugin

Automatic cloud synchronization.

**Features:**
- Continuous sync monitoring
- Queue management
- Bandwidth optimization
- Retry logic

**Configuration:**
```kotlin
{
  "sync_mode": "automatic",
  "providers": ["gdrive", "s3"],
  "strategy": "primary_secondary",
  "retry_count": 3
}
```

#### 4. BatteryAwarePlugin

Battery-optimized scheduling.

**Features:**
- Monitor battery level
- Defer when low battery
- Prioritize when charging
- Adaptive performance

**Configuration:**
```kotlin
{
  "min_battery": 30,
  "charging_boost": true,
  "low_battery_defer": true,
  "critical_threshold": 15
}
```

### Installing Third-Party Plugins

1. Download plugin APK or JAR
2. Navigate to **Automation** → **Plugins**
3. Tap **Install Plugin**
4. Select plugin file
5. Review permissions
6. Install and enable

### Plugin Management

**View Installed Plugins:**
```
Automation → Plugins → Installed
```

**Enable/Disable Plugin:**
```
Tap plugin → Toggle enabled state
```

**Configure Plugin:**
```
Tap plugin → Settings icon
```

**Update Plugin:**
```
Tap plugin → Check for updates
```

**Uninstall Plugin:**
```
Tap plugin → Uninstall
```

## Developing Custom Plugins

Create your own automation plugins.

### Plugin Development Guide

See [Plugin Development Guide](../developer-guides/plugin-development.md) for complete details.

### Quick Start

1. Create plugin project
2. Implement `AutomationPlugin` interface
3. Define plugin metadata
4. Implement automation logic
5. Build and test
6. Package as APK or JAR
7. Install in ObsidianBackup

### Example Plugin

```kotlin
class MyCustomPlugin : AutomationPlugin {
    override val id = "com.example.mycustomplugin"
    override val name = "My Custom Plugin"
    override val version = "1.0.0"
    
    override suspend fun shouldTrigger(context: AutomationContext): Boolean {
        // Your logic to determine if backup should run
        return context.dayOfWeek == DayOfWeek.MONDAY
    }
    
    override suspend fun execute(context: AutomationContext) {
        // Your backup logic
        context.backupManager.startBackup(
            apps = context.config.apps,
            destination = context.config.destination
        )
    }
    
    override fun configure(): PluginConfig {
        // Return configuration UI
        return PluginConfig(
            settings = listOf(
                Setting.toggle("enabled", "Enable Plugin"),
                Setting.text("api_key", "API Key")
            )
        )
    }
}
```

## Advanced Automation

### Chaining Actions

Execute multiple actions in sequence.

**Example: Backup → Verify → Upload → Cleanup**
```kotlin
{
  "actions": [
    {
      "type": "backup",
      "apps": "all",
      "destination": "local"
    },
    {
      "type": "verify",
      "method": "merkle_tree"
    },
    {
      "type": "upload",
      "provider": "gdrive",
      "path": "/backups"
    },
    {
      "type": "cleanup",
      "retention": "keep_last_7"
    }
  ]
}
```

### Conditional Logic

Use conditions to control execution flow.

**Example: Upload only if Wi-Fi and charging**
```kotlin
{
  "condition": {
    "operator": "AND",
    "conditions": [
      {"type": "network", "value": "wifi"},
      {"type": "battery", "value": "charging"}
    ]
  },
  "action": {
    "type": "upload",
    "provider": "gdrive"
  }
}
```

### Parallel Execution

Run multiple actions simultaneously.

**Example: Upload to multiple providers**
```kotlin
{
  "parallel": true,
  "actions": [
    {"type": "upload", "provider": "gdrive"},
    {"type": "upload", "provider": "s3"},
    {"type": "upload", "provider": "dropbox"}
  ]
}
```

## Automation Profiles

Create different automation profiles for different scenarios.

### Profile Types

**Work Profile:**
- Backup during work hours
- Focus on productivity apps
- Quick incremental backups
- Local storage

**Home Profile:**
- Backup during night
- All apps and data
- Full backups
- Cloud storage

**Travel Profile:**
- Manual backups only
- Essential apps
- Metered network awareness
- Local storage only

### Profile Switching

**Manual:**
```
Automation → Profiles → Select Profile
```

**Automatic:**
```
Automation → Profiles → Auto-Switch
Configure triggers:
  - Location-based
  - Time-based
  - Network-based
```

## Notifications

Configure automation notifications.

### Notification Types

**Start Notification:**
- Backup started
- Scheduled time
- Apps included

**Progress Notification:**
- Current progress
- ETA
- Speed

**Completion Notification:**
- Success/failure
- Duration
- Statistics
- Quick actions

**Error Notification:**
- Error details
- Suggested actions
- Retry option

### Notification Settings

```
Automation → Notifications
```

**Options:**
- Show all notifications
- Only show errors
- Silent notifications
- Notification sound
- Vibration
- LED color

## Monitoring and Logs

### Automation History

View history of automated backups:

```
Automation → History
```

**Information Displayed:**
- Execution time
- Trigger type (schedule/event/manual)
- Duration
- Success/failure
- Apps backed up
- Storage used

### Automation Logs

Detailed logs for debugging:

```
Settings → Logs → Automation Logs
```

**Log Levels:**
- DEBUG: Detailed execution steps
- INFO: General information
- WARN: Warnings and issues
- ERROR: Errors and failures

### Export Logs

Export logs for troubleshooting:

1. Navigate to **Logs** screen
2. Filter by date range
3. Tap **Export**
4. Choose format (TXT, JSON, CSV)
5. Share or save

## Performance Optimization

### Battery Optimization

**Tips:**
- Use WorkManager for scheduling (built-in)
- Avoid frequent small backups
- Batch operations when possible
- Monitor battery impact in settings

### Network Optimization

**Tips:**
- Use Wi-Fi only for large transfers
- Implement bandwidth limits
- Upload during off-peak hours
- Use compression

### Storage Optimization

**Tips:**
- Use incremental backups
- Configure retention policies
- Clean up old backups automatically
- Monitor storage usage

## Troubleshooting

### Scheduled Backup Not Running

**Check:**
1. Schedule is enabled
2. Conditions are met (battery, network)
3. App has necessary permissions
4. Battery optimization disabled for app
5. Check automation logs for errors

### Backup Fails During Automation

**Check:**
1. Storage space available
2. Network connectivity (for cloud)
3. Permissions granted
4. No conflicting backups running
5. Check error logs

### Plugin Not Working

**Check:**
1. Plugin is enabled
2. Plugin is compatible with app version
3. Plugin configuration is correct
4. Check plugin logs
5. Reinstall plugin if needed

## Best Practices

1. **Test Automations**: Test schedules before relying on them
2. **Monitor Regularly**: Check automation history periodically
3. **Set Realistic Schedules**: Don't over-automate
4. **Configure Notifications**: Stay informed of backup status
5. **Verify Backups**: Periodically test restores
6. **Update Plugins**: Keep plugins up to date
7. **Document Configuration**: Note your automation setup
8. **Plan for Failures**: Configure retry logic and fallbacks

## Next Steps

- [Plugin Development Guide](../developer-guides/plugin-development.md) - Create plugins
- [Backup Configuration](backup-configuration.md) - Configure backup settings
- [Troubleshooting](troubleshooting.md) - Common issues
