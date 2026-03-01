# Widget Architecture Diagram

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        HOME SCREEN                              │
│                                                                 │
│  ┌───────────────────┐  ┌───────────────────┐                 │
│  │  Widget (2x2)     │  │  Widget (4x2)     │                 │
│  │  ╔═══════════════╗│  │  ╔══════════════════════╗           │
│  │  ║ Status: Ready ║│  │  ║ Status: Ready        ║           │
│  │  ║               ║│  │  ║ Last: Dec 08, 14:30  ║           │
│  │  ║      42       ║│  │  ║                      ║           │
│  │  ║  apps backed  ║│  │  ║  42 apps backed up   ║           │
│  │  ║               ║│  │  ║                      ║           │
│  │  ║ [Backup][View]║│  │  ║ [Backup] [View More] ║           │
│  │  ╚═══════════════╝│  │  ╚══════════════════════╝           │
│  └───────────────────┘  └───────────────────┘                 │
└─────────────────────────────────────────────────────────────────┘
                    │                   │
                    │ User Click        │ User Click
                    ▼                   ▼
┌─────────────────────────────────────────────────────────────────┐
│              BackupStatusWidget (AppWidgetProvider)             │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │  onUpdate()  │  │ onReceive()  │  │ onEnabled()  │        │
│  └──────────────┘  └──────────────┘  └──────────────┘        │
└─────────────────────────────────────────────────────────────────┘
                    │                   │
                    │ Read/Write        │ Schedule
                    ▼                   ▼
┌─────────────────────────────┐  ┌─────────────────────────────┐
│    SharedPreferences        │  │   WidgetUpdateService       │
│    "widget_backup_status"   │  │   (CoroutineWorker)         │
│                             │  │                             │
│  • last_backup_time         │  │  • Periodic: 15 min        │
│  • apps_backed_up           │  │  • Battery conscious       │
│  • backup_status            │  │  • Auto scheduling         │
└─────────────────────────────┘  └─────────────────────────────┘
                    ▲                   ▲
                    │ Update            │ Query Data
                    │                   │
┌─────────────────────────────────────────────────────────────────┐
│                   WidgetIntegration Helper                      │
│                                                                 │
│  • onBackupStarted()        → Set status to RUNNING           │
│  • onBackupCompleted()      → Update stats + IDLE             │
│  • onBackupFailed()         → Set status to FAILED            │
│  • refreshWidget()          → Force update                     │
└─────────────────────────────────────────────────────────────────┘
                    ▲
                    │ Called by
                    │
┌─────────────────────────────────────────────────────────────────┐
│                       BackupWorker                              │
│                    (Your Backup Logic)                          │
│                                                                 │
│  doWork() {                                                     │
│    WidgetIntegration.onBackupStarted()                         │
│    result = performBackup()                                     │
│    WidgetIntegration.onBackupCompleted(result)                 │
│  }                                                              │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow Diagram

```
User Action: "Backup Now"
         │
         ▼
┌────────────────────┐
│  Widget Button     │
│  Click Event       │
└────────────────────┘
         │
         ▼
┌────────────────────┐
│ BackupStatusWidget │
│  onReceive()       │
│  ACTION_BACKUP_NOW │
└────────────────────┘
         │
         ▼
┌────────────────────┐
│ WorkManager        │
│ Enqueue            │
│ BackupWorker       │
└────────────────────┘
         │
         ▼
┌────────────────────┐
│ BackupWorker       │
│ doWork()           │
└────────────────────┘
         │
         ├──► onBackupStarted()
         │         │
         │         ▼
         │    Update SharedPrefs
         │    status = RUNNING
         │         │
         │         ▼
         │    Broadcast Widget Update
         │         │
         │         ▼
         │    Widget shows "Backing up..."
         │
         ▼
    Perform Backup
         │
         ▼
    Backup Complete
         │
         ├──► onBackupCompleted()
         │         │
         │         ▼
         │    Update SharedPrefs:
         │    - last_backup_time
         │    - apps_backed_up  
         │    - status = IDLE
         │         │
         │         ▼
         │    Broadcast Widget Update
         │         │
         │         ▼
         │    Widget shows updated stats
         │
         ▼
    Return Success
```

## Component Interaction

```
┌──────────────────────────────────────────────────────────────────┐
│                         ANDROID SYSTEM                           │
│                                                                  │
│  ┌────────────────┐        ┌─────────────────┐                 │
│  │ AppWidgetHost  │◄──────►│ AppWidgetManager│                 │
│  │ (Launcher)     │        │                 │                 │
│  └────────────────┘        └─────────────────┘                 │
│         │                           │                           │
│         │                           │                           │
└─────────┼───────────────────────────┼───────────────────────────┘
          │                           │
          │ Update UI                 │ Lifecycle Events
          ▼                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                   YOUR APP (ObsidianBackup)                      │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              BackupStatusWidget.kt                         │ │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ │ │
│  │  │onUpdate()│  │onReceive()│  │onEnabled()│  │onDisable│ │ │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘ │ │
│  └────────────────────────────────────────────────────────────┘ │
│          │                │                │                     │
│          │                │                │                     │
│  ┌───────▼────────────────▼────────────────▼─────────┐          │
│  │            RemoteViews Builder                    │          │
│  │  • Set text views                                 │          │
│  │  • Set click handlers                             │          │
│  │  • Apply layouts                                  │          │
│  └───────────────────────────────────────────────────┘          │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              WidgetUpdateService.kt                        │ │
│  │  (Periodic Worker - Every 15 minutes)                      │ │
│  │                                                            │ │
│  │  doWork() {                                                │ │
│  │    refreshWidgetData()                                     │ │
│  │    updateWidgets()                                         │ │
│  │  }                                                         │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              WidgetIntegration.kt                          │ │
│  │  (Helper for Backup System Integration)                    │ │
│  │                                                            │ │
│  │  • onBackupStarted()                                       │ │
│  │  • onBackupCompleted()                                     │ │
│  │  • onBackupFailed()                                        │ │
│  └────────────────────────────────────────────────────────────┘ │
│          │                                                        │
│          ▼                                                        │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              SharedPreferences                             │ │
│  │  File: "widget_backup_status"                              │ │
│  │                                                            │ │
│  │  {                                                         │ │
│  │    "last_backup_time": 1702056600000,                     │ │
│  │    "apps_backed_up": 42,                                  │ │
│  │    "backup_status": "IDLE"                                │ │
│  │  }                                                         │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

## Widget Layout Hierarchy

```
RemoteViews
    └── LinearLayout (widget_container)
        ├── LinearLayout (header)
        │   ├── View (status_indicator) ● [Colored dot]
        │   └── TextView (status_text) "Ready"
        │
        ├── LinearLayout (main_content)
        │   ├── TextView (apps_count) "42" [Large font]
        │   ├── TextView (label) "apps backed up"
        │   └── TextView (last_backup_time) "Dec 08, 14:30"
        │
        └── LinearLayout (action_buttons)
            ├── Button (btn_backup_now) "Backup Now"
            └── Button (btn_view_backups) "View Backups"
```

## Size-Based Layout Selection

```
Widget Added/Resized
         │
         ▼
┌────────────────────┐
│ Get Widget Options │
│ (minWidth/Height)  │
└────────────────────┘
         │
         ▼
    Size Decision
         │
    ┌────┴────┬────────────┐
    │         │            │
    ▼         ▼            ▼
┌─────┐   ┌─────┐      ┌─────┐
│2x2  │   │4x2  │      │4x4  │
│Small│   │Large│      │XL   │
└─────┘   └─────┘      └─────┘
    │         │            │
    ▼         ▼            ▼
widget_     widget_      widget_
backup_     backup_      backup_
status.xml  status_      status_
            large.xml    extra_
                        large.xml
```

## Material You Theme Integration

```
┌────────────────────────────────────────────┐
│         Android System                     │
│                                            │
│  ┌──────────────────────────────────────┐ │
│  │  User's Wallpaper                    │ │
│  │  (Color extraction)                  │ │
│  └──────────────────────────────────────┘ │
│                   │                        │
│                   ▼                        │
│  ┌──────────────────────────────────────┐ │
│  │  Material You Color Palette          │ │
│  │  • system_accent1_100               │ │
│  │  • system_accent1_200               │ │
│  │  • system_neutral1_50               │ │
│  │  • system_neutral1_100              │ │
│  └──────────────────────────────────────┘ │
└────────────────────────────────────────────┘
                   │
                   │ Applied to
                   ▼
┌────────────────────────────────────────────┐
│         Widget Drawables                   │
│                                            │
│  widget_background.xml                     │
│    <solid android:color=                   │
│      "@android:color/system_neutral1_50"/> │
│                                            │
│  widget_button_background.xml              │
│    <solid android:color=                   │
│      "@android:color/system_accent1_100"/> │
│                                            │
│  widget_card_background.xml                │
│    <solid android:color=                   │
│      "@android:color/system_neutral1_100"/>│
└────────────────────────────────────────────┘
                   │
                   │ Result
                   ▼
┌────────────────────────────────────────────┐
│  Widget automatically matches user's       │
│  system theme and wallpaper colors!        │
└────────────────────────────────────────────┘
```

## Update Scheduling

```
Time: T+0 (Widget Added)
│
├──► onEnabled() called
│       │
│       └──► Schedule PeriodicWorkRequest
│               │
│               └──► WidgetUpdateService
│                      Repeat: Every 15 minutes
│
Time: T+15 min
│
├──► WidgetUpdateService.doWork()
│       │
│       ├──► Query backup data
│       └──► Update all widgets
│
Time: T+30 min
│
├──► WidgetUpdateService.doWork()
│       (Repeat...)
│
Time: T+X (User triggers backup)
│
├──► BackupWorker.doWork()
│       │
│       ├──► WidgetIntegration.onBackupStarted()
│       │       └──► Immediate widget update
│       │
│       ├──► Perform backup
│       │
│       └──► WidgetIntegration.onBackupCompleted()
│               └──► Immediate widget update
│
Time: T+Y (Widget Removed)
│
└──► onDisabled() called
        │
        └──► Cancel PeriodicWorkRequest
                (No more updates)
```

## Error Handling Flow

```
Backup Operation Start
         │
         ▼
    Try Block
         │
    ┌────┴────┬────────────┐
    │         │            │
Success   Partial      Failure
    │         │            │
    ▼         ▼            ▼
Update    Update       Update
IDLE      IDLE         FAILED
Green     Green        Red
    │         │            │
    └─────────┴────────────┘
              │
              ▼
       Catch Block
              │
              ▼
         Update
         FAILED
          Red
              │
              ▼
      User sees error
      status in widget
```

---

## Legend

```
┌─────┐
│ Box │  = Component/Process
└─────┘

   │
   │     = Data/Control Flow
   ▼

◄─────►  = Bidirectional Communication

● = Status Indicator Dot

[Button] = Interactive Element
```
