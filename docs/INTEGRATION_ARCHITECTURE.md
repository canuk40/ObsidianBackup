# ObsidianBackup Final Integration Architecture

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         MainActivity                             │
│                    (@AndroidEntryPoint)                         │
│                                                                  │
│  ┌────────────────────────────────────────────────────────┐   │
│  │              ObsidianBackupApp (Compose)                │   │
│  │                                                          │   │
│  │  ┌──────────────────────────────────────────────────┐  │   │
│  │  │         Navigation System                         │  │   │
│  │  │  ┌───────────────────────────────────────────┐   │  │   │
│  │  │  │ Bottom Nav (mainItems)                     │   │  │   │
│  │  │  │ Dashboard │ Apps │ Backups │ ... │Settings │   │  │   │
│  │  │  └───────────────────────────────────────────┘   │  │   │
│  │  │  ┌───────────────────────────────────────────┐   │  │   │
│  │  │  │ Drawer/Menu (drawerItems)                 │   │  │   │
│  │  │  │ + Gaming │ Health │ Plugins                │   │  │   │
│  │  │  └───────────────────────────────────────────┘   │  │   │
│  │  └──────────────────────────────────────────────────┘  │   │
│  │                                                          │   │
│  │  ┌──────────────────────────────────────────────────┐  │   │
│  │  │         Screen Routing (when/else)               │  │   │
│  │  │                                                   │  │   │
│  │  │  Dashboard → DashboardScreen                     │  │   │
│  │  │  Apps      → AppsScreen                          │  │   │
│  │  │  Backups   → BackupsScreen                       │  │   │
│  │  │  Automation→ AutomationScreen                    │  │   │
│  │  │  Gaming    → GamingScreen        ✨ NEW         │  │   │
│  │  │  Health    → HealthScreen        ✨ NEW         │  │   │
│  │  │  Plugins   → PluginsScreen       ✨ NEW         │  │   │
│  │  │  Logs      → LogsScreen                          │  │   │
│  │  │  Settings  → SettingsScreen                      │  │   │
│  │  └──────────────────────────────────────────────────┘  │   │
│  └────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     Presentation Layer                           │
│                      (ViewModels)                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  DashboardViewModel    BackupViewModel    GamingViewModel ✨    │
│  PluginsViewModel ✨   HealthViewModel ✨                       │
│                                                                  │
│  All annotated with @HiltViewModel                              │
│  State management via StateFlow                                 │
└─────────────────────────────────────────────────────────────────┘
                              ↓
                        Inject Dependencies
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                 Dependency Injection (Hilt)                      │
│                   SingletonComponent                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────┐  ┌─────────────┐  ┌──────────────┐            │
│  │ AppModule  │  │ CloudModule │  │ DeepLink     │            │
│  │            │  │             │  │ Module       │            │
│  │ • Backup   │  │ • OAuth2    │  │ • Parser     │            │
│  │   Engine   │  │ • GDrive    │  │ • Router     │            │
│  │ • Restore  │  │ • WebDAV    │  │ • Analytics  │            │
│  │ • Catalog  │  │ • Rclone    │  └──────────────┘            │
│  │ • Crypto   │  │ • CloudSync │                               │
│  │ • Plugins  │  └─────────────┘                               │
│  │ • Logger   │                                                 │
│  │ • Features │  ┌─────────────┐  ┌──────────────┐            │
│  └────────────┘  │ Gaming      │  │ Health       │            │
│                  │ Module ✨   │  │ Module ✨    │            │
│                  │             │  │              │            │
│                  │ • Emulator  │  │ • Connect    │            │
│                  │   Detector  │  │   Manager    │            │
│                  │ • SaveState │  │ • Data       │            │
│                  │   Manager   │  │   Exporter   │            │
│                  │ • PlayGames │  │ • Data       │            │
│                  │   Sync      │  │   Store      │            │
│                  │ • Gaming    │  └──────────────┘            │
│                  │   Manager   │                               │
│                  └─────────────┘  ┌──────────────┐            │
│                                   │ ML Module ✨ │            │
│                  ┌─────────────┐  │              │            │
│                  │ Tasker      │  │ • Context    │            │
│                  │ Module ✨   │  │   Detector   │            │
│                  │             │  │ • Pattern    │            │
│                  │ • Status    │  │   Analyzer   │            │
│                  │   Provider  │  │ • Intent     │            │
│                  │ • Tasker    │  │   Parser     │            │
│                  │   Integrate │  │ • Prediction │            │
│                  └─────────────┘  │   Model      │            │
│                                   │ • Time       │            │
│                                   │   Predictor  │            │
│                                   │ • Smart      │            │
│                                   │   Scheduler  │            │
│                                   └──────────────┘            │
└─────────────────────────────────────────────────────────────────┘
                              ↓
                     Domain Layer Services
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                      Feature Services                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────┐  ┌─────────────────┐  ┌────────────────┐  │
│  │ Backup Engine  │  │ Gaming Manager  │  │ Health Manager │  │
│  │ • Parallel     │  │ • Scan Emulator │  │ • Check Avail  │  │
│  │ • Incremental  │  │ • Backup Saves  │  │ • Export Data  │  │
│  │ • Split APK    │  │ • Play Games    │  │ • Privacy      │  │
│  └────────────────┘  └─────────────────┘  └────────────────┘  │
│                                                                  │
│  ┌────────────────┐  ┌─────────────────┐  ┌────────────────┐  │
│  │ Cloud Sync     │  │ Plugin Manager  │  │ Smart Schedule │  │
│  │ • GDrive       │  │ • Discovery     │  │ • ML Predict   │  │
│  │ • WebDAV       │  │ • Load/Unload   │  │ • Context      │  │
│  │ • S3/Rclone    │  │ • Sandbox       │  │ • Optimize     │  │
│  └────────────────┘  └─────────────────┘  └────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
                        Data Layer
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Data Repositories                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐  ┌─────────────┐  ┌──────────────┐          │
│  │ BackupRepo   │  │ CatalogRepo │  │ SettingsRepo │          │
│  └──────────────┘  └─────────────┘  └──────────────┘          │
│                                                                  │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              Room Database (Local)                     │    │
│  │  • AppBackupEntity                                     │    │
│  │  • BackupScheduleEntity                                │    │
│  │  • SettingsEntity                                      │    │
│  │  • HealthDataEntity                    ✨ NEW         │    │
│  │  • GamingBackupEntity                  ✨ NEW         │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌────────────────────────────────────────────────────────┐    │
│  │           External Storage / Cloud                      │    │
│  │  • Backup Files (.tar.zst)                             │    │
│  │  • Restore Journals                                    │    │
│  │  • Audit Logs                                          │    │
│  │  • Health Exports                      ✨ NEW         │    │
│  │  • Gaming Saves                        ✨ NEW         │    │
│  └────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

## Feature Flag Control Flow

```
┌──────────────────────────────────────┐
│     FeatureFlagManager               │
│                                      │
│  ┌────────────────────────────────┐ │
│  │  Local Overrides (Debug)       │ │
│  │  • setLocalOverride()          │ │
│  │  • clearLocalOverride()        │ │
│  └────────────────────────────────┘ │
│           ↓ Priority Check           │
│  ┌────────────────────────────────┐ │
│  │  Remote Config                 │ │
│  │  • SharedPreferences (local)   │ │
│  │  • Firebase RC (optional)      │ │
│  │  • Custom backend (pluggable)  │ │
│  └────────────────────────────────┘ │
│           ↓ Default Values           │
│  ┌────────────────────────────────┐ │
│  │  getDefaultValue()             │ │
│  │  • PARALLEL_BACKUP     = true  │ │
│  │  • GAMING_BACKUP       = true  │ │
│  │  • HEALTH_CONNECT_SYNC = true  │ │
│  │  • PLUGIN_SYSTEM       = false │ │
│  │  • etc.                        │ │
│  └────────────────────────────────┘ │
└──────────────────────────────────────┘
         ↓ isEnabled(feature)
┌──────────────────────────────────────┐
│       ViewModel/UI Layer             │
│  • Check before showing UI           │
│  • Disable functionality if false    │
│  • Show "Feature Disabled" message   │
└──────────────────────────────────────┘
```

## Deep Link Flow

```
External App/URL
    ↓
obsidianbackup://gaming/backup?emulator=retroarch
    ↓
┌──────────────────────────────────────┐
│      DeepLinkActivity                │
│  • Receives intent                   │
│  • Validates URL scheme              │
└──────────────────────────────────────┘
    ↓
┌──────────────────────────────────────┐
│      DeepLinkAuthenticator           │
│  • Security validation               │
│  • Token verification (if required)  │
└──────────────────────────────────────┘
    ↓
┌──────────────────────────────────────┐
│      DeepLinkParser                  │
│  • Parse URI components              │
│  • Extract action & parameters       │
└──────────────────────────────────────┘
    ↓
┌──────────────────────────────────────┐
│      DeepLinkRouter                  │
│  • Route to appropriate handler      │
│  • ACTION_BACKUP                     │
│  • ACTION_RESTORE                    │
│  • ACTION_NAVIGATE                   │
│  • gaming_backup        ✨ NEW      │
│  • health_export        ✨ NEW      │
│  • plugin_install       ✨ NEW      │
└──────────────────────────────────────┘
    ↓
┌──────────────────────────────────────┐
│      DeepLinkHandler                 │
│  • Execute action                    │
│  • Update MainActivity extras        │
└──────────────────────────────────────┘
    ↓
┌──────────────────────────────────────┐
│      MainActivity                    │
│  • Process extras                    │
│  • Navigate to screen                │
│  • Trigger action                    │
└──────────────────────────────────────┘
    ↓
┌──────────────────────────────────────┐
│      DeepLinkAnalytics               │
│  • Track usage                       │
│  • Log for debugging                 │
└──────────────────────────────────────┘
```

## Widget Integration

```
┌──────────────────────────────────────┐
│     Home Screen Launcher             │
└──────────────────────────────────────┘
         ↓ User adds widget
┌──────────────────────────────────────┐
│     BackupWidget / StatusWidget      │
│                                      │
│  onUpdate(appWidgetIds)              │
│    ↓                                 │
│  RemoteViews Layout                  │
│    • Set text                        │
│    • Set click handlers              │
│    • Update appearance               │
│    ↓                                 │
│  PendingIntent → MainActivity        │
│    • Quick backup action             │
│    • Open app                        │
└──────────────────────────────────────┘
         ↓ User clicks
┌──────────────────────────────────────┐
│     MainActivity                     │
│  • Receive EXTRA_WIDGET_ACTION       │
│  • Trigger backup                    │
│  • Update widget                     │
└──────────────────────────────────────┘
         ↓ Backup completes
┌──────────────────────────────────────┐
│  BackupStatusWidget.updateWidgets()  │
│  • Update last backup time           │
│  • Update backup count               │
│  • Broadcast update intent           │
└──────────────────────────────────────┘
```

## Settings Hierarchy

```
Settings Screen
├── Backup & Restore
│   ├── Default Components
│   ├── Compression Profile
│   └── Verification Level
├── Encryption
│   ├── Zero-Knowledge
│   └── Standard (PRO)
├── Cloud & Sync
│   ├── Providers (GDrive, WebDAV, S3)
│   ├── Decentralized (IPFS)
│   └── Sync Policies
├── Gaming & Emulators         ✨ NEW
│   ├── Gaming Backups
│   └── Play Games Sync
├── Health & Fitness           ✨ NEW
│   ├── Health Connect
│   └── Privacy Settings
├── Automation & Scheduling    ✨ NEW
│   ├── Smart Scheduling
│   └── Tasker Integration
├── Plugins                    ✨ NEW
│   ├── Plugin System
│   └── Security
├── Retention & Cleanup
│   ├── Policies
│   └── Storage Limits
├── Permissions
│   ├── Mode (ROOT/Shizuku/ADB)
│   └── Request
├── Advanced
│   ├── BusyBox Options
│   ├── Debug Mode
│   ├── Export Diagnostics
│   └── Feature Flags
└── About
    ├── Version
    └── Licenses
```

## Data Flow Example: Gaming Backup

```
1. User Flow
   GamingScreen (UI)
   ↓ User taps "Scan"
   GamingViewModel.scanEmulators()
   ↓ viewModelScope.launch
   GamingBackupManager.scanForEmulators()
   ↓
   EmulatorDetector.detectInstalledEmulators()
   ↓ PackageManager query
   List<DetectedEmulator>
   ↓ StateFlow update
   _detectedEmulators.value = list
   ↓ Compose recomposition
   GamingScreen shows emulators

2. Backup Flow
   GamingScreen
   ↓ User selects emulator & games
   GamingViewModel.backupSelectedGames()
   ↓
   GamingBackupManager.backupGameSaves()
   ↓
   SaveStateManager.copySaves()
   ↓
   BackupCatalog.recordBackup()
   ↓
   StateFlow progress updates
   ↓
   UI shows progress bar
```

## Testing Architecture

```
┌──────────────────────────────────────┐
│        Integration Tests             │
│  app/src/androidTest/.../            │
│                                      │
│  IntegrationTest                     │
│    • testFeatureFlags_AllDefined     │
│    • testNavigationScreens_AllDef... │
└──────────────────────────────────────┘
         ↓ Verifies
┌──────────────────────────────────────┐
│     Production Code                  │
│                                      │
│  • Feature enum completeness         │
│  • Navigation.Screen.items           │
│  • No circular dependencies          │
└──────────────────────────────────────┘
         ↓ Runs on
┌──────────────────────────────────────┐
│     Test Device/Emulator             │
│  • Android instrumentation tests     │
│  • Requires running device           │
└──────────────────────────────────────┘
```

## Summary

**Total Integration Points**: 47+
- 4 New Hilt Modules
- 3 New ViewModels  
- 3 New UI Screens
- 8 New Feature Flags
- 4 New Navigation Items
- 4 New Settings Sections
- 3 New Deep Link Handlers
- 2 New Widgets
- 1 Onboarding Flow
- 15+ Documentation Files

**Architecture Principles**:
✅ Single Responsibility - Each module has one purpose  
✅ Dependency Inversion - All deps injected via Hilt  
✅ Open/Closed - Feature flags enable/disable without code changes  
✅ Interface Segregation - Small, focused interfaces  
✅ DRY - Common patterns extracted to base classes  

**Integration Status**: ✅ Complete and Production Ready
