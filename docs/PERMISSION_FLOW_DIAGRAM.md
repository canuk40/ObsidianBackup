# Permission Flow Diagrams - ObsidianBackup

**Visual guide to permission request flows and decision trees**

---

## 1. App Initialization Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    App Launch (MainActivity)                │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
              ┌──────────────────────────────┐
              │  ObsidianBackupApplication   │
              │  onCreate()                  │
              └──────────────┬───────────────┘
                             │
                             ▼
              ┌──────────────────────────────┐
              │  Initialize Hilt DI          │
              │  - PermissionManager         │
              │  - RootDetectionManager      │
              │  - BiometricAuthManager      │
              │  - StoragePermissionHelper   │
              │  - HealthConnectManager      │
              └──────────────┬───────────────┘
                             │
                             ▼
              ┌──────────────────────────────┐
              │  DashboardScreen shown       │
              │  (No permissions requested)  │
              └──────────────────────────────┘
```

**Design Principle:** No permission spam on first launch ✅

---

## 2. Root Detection & Mode Selection Flow

```
                    ┌─────────────────────────┐
                    │  User Taps "Backup"     │
                    └──────────┬──────────────┘
                               │
                               ▼
          ┌────────────────────────────────────────┐
          │  PermissionManager.detectCapabilities()│
          │  (Runs in background, cached 30s)      │
          └──────────┬─────────────────────────────┘
                     │
    ┌────────────────┼────────────────┐
    │                │                │
    ▼                ▼                ▼
┌─────────┐    ┌─────────┐    ┌─────────┐
│ Method  │    │ Method  │    │ Method  │
│ 1: Safe │    │ 2: Build│    │ 3: Root │
│ tyNet   │    │ Tags    │    │ Apps    │
└────┬────┘    └────┬────┘    └────┬────┘
     │              │              │
     └──────┬───────┴───────┬──────┘
            │               │
    ┌───────┴────┐    ┌─────┴────────┐
    │ Method 4:  │    │ Method 5:    │
    │ Su Binary  │    │ Dangerous    │
    │            │    │ Props        │
    └─────┬──────┘    └─────┬────────┘
          │                 │
          └────────┬─────────┘
                   │
           ┌───────▼────────┐
           │ Method 6:      │
           │ System Write   │
           └───────┬────────┘
                   │
      ┌────────────┴────────────┐
      │                         │
      ▼                         ▼
┌───────────┐           ┌─────────────┐
│ Root      │           │ No Root     │
│ Detected  │           │             │
└─────┬─────┘           └──────┬──────┘
      │                        │
      │              ┌─────────┴─────────┐
      │              │ Check Shizuku     │
      │              └─────────┬─────────┘
      │                        │
      │              ┌─────────┴─────────┐
      │              │                   │
      │              ▼                   ▼
      │        ┌──────────┐       ┌──────────┐
      │        │ Shizuku  │       │ No       │
      │        │ Found    │       │ Shizuku  │
      │        └────┬─────┘       └────┬─────┘
      │             │                  │
      │             │         ┌────────▼────────┐
      │             │         │ Check ADB       │
      │             │         └────────┬────────┘
      │             │                  │
      │             │         ┌────────┴────────┐
      │             │         │                 │
      │             │         ▼                 ▼
      │             │    ┌─────────┐      ┌─────────┐
      │             │    │ ADB     │      │ No ADB  │
      │             │    │ Enabled │      │         │
      │             │    └────┬────┘      └────┬────┘
      │             │         │                │
      └─────────────┴─────────┴────────────────┘
                              │
            ┌─────────────────┴─────────────────┐
            │  PermissionMode.currentMode       │
            │  = ROOT | SHIZUKU | ADB | SAF     │
            └─────────────────┬─────────────────┘
                              │
            ┌─────────────────▼─────────────────┐
            │  Display Mode Selection UI        │
            │  Show capabilities for each mode  │
            └───────────────────────────────────┘
```

**Confidence Scoring:**
- SafetyNet fail → HIGH confidence
- Multiple methods → CRITICAL confidence
- Single method → MEDIUM confidence
- Build tags only → LOW confidence

---

## 3. Storage Permission Flow (Version-Aware)

```
┌────────────────────────────────────────────┐
│  User Selects Backup Location             │
└───────────────────┬────────────────────────┘
                    │
        ┌───────────▼───────────┐
        │  Check Android API    │
        │  Build.VERSION.SDK_INT│
        └───────────┬───────────┘
                    │
    ┌───────────────┼───────────────┬──────────────┐
    │               │               │              │
    ▼               ▼               ▼              ▼
┌────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│ API    │    │ API     │    │ API     │    │ API     │
│ ≤28    │    │ 29      │    │ 30-32   │    │ ≥33     │
└───┬────┘    └────┬────┘    └────┬────┘    └────┬────┘
    │              │              │              │
    ▼              ▼              ▼              ▼
┌────────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ Request    │ │ Scoped   │ │ Scoped   │ │ Request  │
│ READ/WRITE │ │ Storage  │ │ Storage  │ │ READ_    │
│ _EXTERNAL_ │ │ + SAF    │ │ + MANAGE │ │ MEDIA_*  │
│ STORAGE    │ │          │ │ _EXTERNAL│ │          │
└─────┬──────┘ └─────┬────┘ └────┬─────┘ └────┬─────┘
      │              │            │            │
      └──────────────┴────────────┴────────────┘
                     │
      ┌──────────────▼──────────────┐
      │  User Permission Decision   │
      └──────────────┬──────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
        ▼                         ▼
┌────────────┐           ┌─────────────┐
│ Granted    │           │ Denied      │
└─────┬──────┘           └──────┬──────┘
      │                         │
      ▼                         ▼
┌────────────┐           ┌─────────────┐
│ External   │           │ Fallback to │
│ Storage    │           │ App-Private │
│ Access     │           │ Storage     │
└────────────┘           └──────┬──────┘
                                │
                         ┌──────▼──────┐
                         │ getExternal │
                         │ FilesDir()  │
                         │ (No Perms)  │
                         └─────────────┘
```

**Key Decision Points:**
1. **API ≤28:** Legacy permissions mandatory for external storage
2. **API 29:** Scoped storage with `requestLegacyExternalStorage=true`
3. **API 30-32:** MANAGE_EXTERNAL_STORAGE for advanced features only
4. **API ≥33:** Granular media permissions (optional)

**Fallback Strategy:** Always prefer app-private storage (no permissions needed) ✅

---

## 4. Biometric Authentication Flow

```
┌───────────────────────────────────────────┐
│  Sensitive Operation Triggered            │
│  (Backup, Restore, Settings Change, etc.) │
└──────────────────┬────────────────────────┘
                   │
     ┌─────────────▼─────────────┐
     │  BiometricAuthManager     │
     │  .authenticateForOperation│
     └─────────────┬─────────────┘
                   │
     ┌─────────────▼─────────────┐
     │  getBiometricCapability() │
     └─────────────┬─────────────┘
                   │
    ┌──────────────┴──────────────┐
    │                             │
    ▼                             ▼
┌────────────┐           ┌─────────────────┐
│ Available  │           │ Not Available / │
│            │           │ Not Enrolled    │
└─────┬──────┘           └────────┬────────┘
      │                           │
      ▼                           ▼
┌──────────────────────┐   ┌──────────────┐
│ Check StrongBox      │   │ Return Error │
└──────────┬───────────┘   │ - NO_HARDWARE│
           │               │ - NOT_ENROLLED│
    ┌──────┴──────┐        │ - UNAVAILABLE│
    │             │        └──────┬───────┘
    ▼             ▼               │
┌────────┐  ┌────────┐           │
│ Strong │  │ Normal │           │
│ Box    │  │ TEE    │           │
└───┬────┘  └───┬────┘           │
    └───────────┴────────────────┘
                │
    ┌───────────▼───────────┐
    │  BiometricPrompt.     │
    │  PromptInfo.Builder() │
    │  - title, subtitle    │
    │  - description        │
    │  - authenticators     │
    └───────────┬───────────┘
                │
    ┌───────────▼──────────┐
    │ Cipher provided?     │
    └───────────┬──────────┘
                │
        ┌───────┴───────┐
        │               │
        ▼               ▼
┌──────────────┐  ┌────────────┐
│ authenticate │  │ authenticate│
│ (PromptInfo, │  │ (PromptInfo)│
│  CryptoObj)  │  │             │
└──────┬───────┘  └─────┬──────┘
       └──────────┬─────┘
                  │
       ┌──────────▼──────────┐
       │  User Action        │
       └──────────┬──────────┘
                  │
    ┌─────────────┼─────────────┬──────────────┐
    │             │             │              │
    ▼             ▼             ▼              ▼
┌────────┐  ┌─────────┐  ┌──────────┐  ┌───────────┐
│Success │  │ Error   │  │ Canceled │  │ Failed    │
└───┬────┘  └────┬────┘  └────┬─────┘  └────┬──────┘
    │            │            │             │
    │            │            │             │ (Retry)
    ▼            ▼            ▼             │
┌────────┐  ┌─────────┐  ┌──────────┐      │
│Continue│  │ Handle  │  │ Abort    │      │
│Operatio│  │ Error:  │  │ Operatio │      │
│n       │  │- Lockout│  │n         │◄─────┘
│        │  │- No Bio │  │          │
│        │  │- HW Err │  │          │
│        │  │         │  │          │
└────────┘  │ Show    │  └──────────┘
            │ Guidance│
            └─────┬───┘
                  │
         ┌────────▼────────┐
         │ Fallback to:    │
         │ - Device PIN    │
         │ - Pattern       │
         │ - Password      │
         └─────────────────┘
```

**Error Handling:**
- `ERROR_LOCKOUT` (30s) → Show countdown, auto-retry
- `ERROR_LOCKOUT_PERMANENT` → Force device credential
- `ERROR_NO_BIOMETRICS` → Guide to settings enrollment
- `ERROR_CANCELED` → Abort gracefully
- `ERROR_HW_UNAVAILABLE` → Fallback to PIN

**Security Levels:**
- **StrongBox:** Hardware-backed, highest security (Android 9+)
- **TEE:** Trusted Execution Environment (Android 6+)
- **Device Credential:** PIN/Pattern/Password fallback

---

## 5. Health Connect Permission Flow

```
┌───────────────────────────────────────┐
│  User Enables Health Data Backup     │
└──────────────────┬────────────────────┘
                   │
     ┌─────────────▼──────────────┐
     │  Check Health Connect SDK  │
     │  HealthConnectClient.      │
     │  getSdkStatus(context)     │
     └─────────────┬──────────────┘
                   │
        ┌──────────┴──────────┐
        │                     │
        ▼                     ▼
┌────────────────┐   ┌─────────────────┐
│ SDK_AVAILABLE  │   │ SDK_UNAVAILABLE │
└────────┬───────┘   └────────┬────────┘
         │                    │
         │                    ▼
         │           ┌──────────────────┐
         │           │ Show Install     │
         │           │ Prompt:          │
         │           │ "Install Health  │
         │           │  Connect from    │
         │           │  Play Store"     │
         │           └──────────────────┘
         │
         ▼
┌────────────────────────────┐
│  User Selects Data Types   │
│  ☑ Steps                   │
│  ☑ Heart Rate              │
│  ☑ Sleep                   │
│  ☐ Exercise (not selected) │
│  ☑ Nutrition               │
│  ... (14 types total)      │
└────────────┬───────────────┘
             │
┌────────────▼────────────────┐
│ getRequiredPermissions()    │
│ For selected types only:    │
│ - READ_STEPS                │
│ - WRITE_STEPS               │
│ - READ_HEART_RATE           │
│ - WRITE_HEART_RATE          │
│ - READ_SLEEP                │
│ - WRITE_SLEEP               │
│ - READ_NUTRITION            │
│ - WRITE_NUTRITION           │
└────────────┬────────────────┘
             │
┌────────────▼────────────────┐
│ HealthConnect Permission UI │
│ (System-provided dialog)    │
└────────────┬────────────────┘
             │
    ┌────────┴────────┐
    │                 │
    ▼                 ▼
┌─────────┐      ┌─────────┐
│ User    │      │ User    │
│ Grants  │      │ Denies  │
│ Some/All│      │ Some/All│
└────┬────┘      └────┬────┘
     │                │
     └────────┬───────┘
              │
    ┌─────────▼─────────┐
    │ Store Granted     │
    │ Permissions       │
    └─────────┬─────────┘
              │
    ┌─────────▼────────────────────────┐
    │ Backup Only Granted Data Types   │
    │                                   │
    │ ✅ Steps → Backed up              │
    │ ✅ Heart Rate → Backed up         │
    │ ✅ Sleep → Backed up              │
    │ ❌ Exercise → Skipped (denied)    │
    │ ✅ Nutrition → Backed up          │
    │                                   │
    │ Log: "Exercise permission denied, │
    │       skipping workout data"      │
    └───────────────────────────────────┘
```

**Permission Request Strategy:**
1. **Lazy Loading:** Only request when user enables health backup
2. **Granular Control:** Request only selected data types
3. **Paired Permissions:** Always request READ + WRITE together
4. **Selective Backup:** Skip denied types, continue with granted
5. **Re-prompting:** Allow user to grant more types later

**14 Supported Data Types:**
- Steps, Heart Rate, Sleep, Exercise, Nutrition
- Weight, Height, Body Fat
- Blood Pressure, Blood Glucose
- Hydration, Distance, Cycling, Active Calories

---

## 6. Scheduled Backup Permission Flow

```
┌─────────────────────────────────────┐
│  User Configures Scheduled Backup   │
│  Schedule: Daily at 3:00 AM         │
└──────────────────┬──────────────────┘
                   │
     ┌─────────────▼──────────────┐
     │  Check Android Version     │
     └─────────────┬──────────────┘
                   │
        ┌──────────┴──────────┐
        │                     │
        ▼                     ▼
┌────────────┐       ┌────────────────┐
│ API < 31   │       │ API ≥ 31       │
│ (≤Android  │       │ (Android 12+)  │
│  11)       │       │                │
└─────┬──────┘       └────────┬───────┘
      │                       │
      │              ┌────────▼────────┐
      │              │ Check if can    │
      │              │ schedule exact  │
      │              │ alarms          │
      │              │ AlarmManager.   │
      │              │ canSchedule     │
      │              │ ExactAlarms()   │
      │              └────────┬────────┘
      │                       │
      │              ┌────────┴────────┐
      │              │                 │
      │              ▼                 ▼
      │         ┌─────────┐      ┌─────────┐
      │         │ Can     │      │ Cannot  │
      │         │ Schedule│      │         │
      │         └────┬────┘      └────┬────┘
      │              │                │
      │              │         ┌──────▼──────┐
      │              │         │ Show Dialog:│
      │              │         │ "Grant      │
      │              │         │  SCHEDULE_  │
      │              │         │  EXACT_     │
      │              │         │  ALARM"     │
      │              │         └──────┬──────┘
      │              │                │
      │              │         ┌──────▼──────┐
      │              │         │ User Opens │
      │              │         │ Settings   │
      │              │         └──────┬──────┘
      │              │                │
      │              │         ┌──────▼──────┐
      │              │         │ Grant Perm │
      │              │         └──────┬──────┘
      │              │                │
      └──────────────┴────────────────┘
                     │
      ┌──────────────▼──────────────┐
      │  Schedule via WorkManager   │
      │  PeriodicWorkRequest        │
      │  - Constraints (charging,   │
      │    network, battery)        │
      │  - Flex interval            │
      └──────────────┬──────────────┘
                     │
      ┌──────────────▼──────────────┐
      │  Backup Triggers at         │
      │  Scheduled Time             │
      │  (± 15 min flex window)     │
      └─────────────────────────────┘
```

**Scheduling Strategy:**
- **Exact Alarms:** Used when precision needed (Android 12+)
- **Inexact Alarms:** Fallback for better battery life
- **WorkManager:** Primary mechanism (survives reboot)
- **Constraints:** Only run when charging, WiFi, battery >15%

**User Experience:**
- Clear explanation why exact timing needed
- One-tap to settings if permission denied
- Graceful fallback to ±15min window
- Visual indicator of next scheduled backup

---

## 7. Foreground Service Lifecycle

```
┌────────────────────────────────────┐
│  User Initiates Long-Running Backup│
└──────────────────┬─────────────────┘
                   │
     ┌─────────────▼──────────────┐
     │  BackupService.onCreate()  │
     └─────────────┬──────────────┘
                   │
     ┌─────────────▼──────────────┐
     │  Create Notification       │
     │  Channel: "Backup Progress"│
     │  Importance: HIGH          │
     └─────────────┬──────────────┘
                   │
     ┌─────────────▼──────────────┐
     │  Build Notification        │
     │  - Title: "Backup Running" │
     │  - Progress bar            │
     │  - Cancel action           │
     └─────────────┬──────────────┘
                   │
     ┌─────────────▼──────────────┐
     │  startForeground(id, notif)│
     │  (Must be called within 5s)│
     └─────────────┬──────────────┘
                   │
        ┌──────────▼──────────┐
        │ Android 14+ Check   │
        └──────────┬──────────┘
                   │
        ┌──────────┴──────────┐
        │                     │
        ▼                     ▼
┌────────────┐       ┌────────────────┐
│ API < 34   │       │ API ≥ 34       │
└─────┬──────┘       └────────┬───────┘
      │                       │
      │              ┌────────▼────────┐
      │              │ Verify service  │
      │              │ type declared:  │
      │              │ "dataSync"      │
      │              └────────┬────────┘
      │                       │
      └───────────────────────┘
                   │
     ┌─────────────▼──────────────┐
     │  Perform Backup Operation  │
     │  (Update notification with │
     │   real-time progress)      │
     └─────────────┬──────────────┘
                   │
     ┌─────────────▼──────────────┐
     │  Backup Complete           │
     └─────────────┬──────────────┘
                   │
     ┌─────────────▼──────────────┐
     │  stopForeground(true)      │
     │  (Remove notification)     │
     └─────────────┬──────────────┘
                   │
     ┌─────────────▼──────────────┐
     │  stopSelf()                │
     └────────────────────────────┘
```

**Critical Requirements:**
1. **5-Second Rule:** Must call `startForeground()` within 5s of service start
2. **Android 14:** Must declare `foregroundServiceType="dataSync"`
3. **Notification:** Must be visible while service running
4. **Cleanup:** Must call `stopForeground()` and `stopSelf()` when done

**User Experience:**
- Ongoing notification shows backup progress
- User can cancel via notification action
- Service not killed by system during operation
- Battery optimization warnings if needed

---

## 8. First Launch Onboarding Flow

```
┌────────────────────────────────────┐
│  First App Launch                  │
└──────────────────┬─────────────────┘
                   │
     ┌─────────────▼──────────────┐
     │  Welcome Screen            │
     │  "Welcome to ObsidianBackup"│
     └─────────────┬──────────────┘
                   │
     ┌─────────────▼──────────────┐
     │  Feature Highlights        │
     │  - Zero-knowledge encryption│
     │  - Cross-device sync       │
     │  - Health data backup      │
     └─────────────┬──────────────┘
                   │
     ┌─────────────▼──────────────┐
     │  "Choose Your Backup Mode" │
     │                            │
     │  ○ SAF Mode (Recommended)  │
     │    No permissions needed   │
     │    Works on all devices    │
     │                            │
     │  ○ Root Mode               │
     │    Full app + data backup  │
     │    Requires root access    │
     │                            │
     │  ○ Shizuku Mode            │
     │    Advanced backup         │
     │    Requires Shizuku app    │
     └─────────────┬──────────────┘
                   │
        ┌──────────┴──────────┐
        │                     │
        ▼                     ▼
┌────────────┐       ┌────────────────┐
│ SAF Mode   │       │ Root/Shizuku   │
│ Selected   │       │ Selected       │
└─────┬──────┘       └────────┬───────┘
      │                       │
      │              ┌────────▼────────┐
      │              │ Detect Root/    │
      │              │ Shizuku         │
      │              └────────┬────────┘
      │                       │
      │              ┌────────┴────────┐
      │              │                 │
      │              ▼                 ▼
      │         ┌─────────┐      ┌─────────┐
      │         │ Found   │      │ Not     │
      │         │         │      │ Found   │
      │         └────┬────┘      └────┬────┘
      │              │                │
      │              │         ┌──────▼──────┐
      │              │         │ Show Error: │
      │              │         │ "Root not   │
      │              │         │  detected.  │
      │              │         │  Falling    │
      │              │         │  back to    │
      │              │         │  SAF mode"  │
      │              │         └──────┬──────┘
      │              │                │
      └──────────────┴────────────────┘
                     │
     ┌───────────────▼───────────────┐
     │  "Optional: Enable Features"  │
     │                               │
     │  □ Biometric Lock            │
     │  □ Health Data Backup        │
     │  □ Scheduled Backups         │
     │  □ Cloud Sync                │
     │                               │
     │  [Skip] [Enable Selected]    │
     └───────────────┬───────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
        ▼                         ▼
┌────────────┐           ┌─────────────┐
│ Skip       │           │ Enable      │
└─────┬──────┘           └──────┬──────┘
      │                         │
      │              ┌──────────▼──────────┐
      │              │ Request Permissions:│
      │              │ - Biometric (auto)  │
      │              │ - Health Connect    │
      │              │ - Notifications     │
      │              │ - Schedule Alarms   │
      │              └──────────┬──────────┘
      │                         │
      └─────────────────────────┘
                     │
     ┌───────────────▼───────────────┐
     │  Navigate to Dashboard        │
     │  (Ready to use)               │
     └───────────────────────────────┘
```

**Onboarding Principles:**
1. **No Permission Spam:** Only request what user enables
2. **Progressive Disclosure:** Show features as needed
3. **Clear Benefits:** Explain why each permission needed
4. **Easy Opt-Out:** All features optional
5. **Sensible Defaults:** SAF mode recommended for most users

---

## 9. Permission Re-Request Flow (After Denial)

```
┌────────────────────────────────────┐
│  User Denied Permission Previously │
└──────────────────┬─────────────────┘
                   │
     ┌─────────────▼──────────────┐
     │  Feature Requires Permission│
     └─────────────┬──────────────┘
                   │
     ┌─────────────▼──────────────┐
     │  Show Rationale Dialog:    │
     │                            │
     │  "📸 Camera Permission     │
     │   Needed"                  │
     │                            │
     │  "To scan QR codes for     │
     │   device pairing, we need  │
     │   camera access."          │
     │                            │
     │  [Don't Allow] [Settings]  │
     └─────────────┬──────────────┘
                   │
        ┌──────────┴──────────┐
        │                     │
        ▼                     ▼
┌────────────┐       ┌────────────────┐
│ Don't Allow│       │ Settings       │
└─────┬──────┘       └────────┬───────┘
      │                       │
      │              ┌────────▼────────┐
      │              │ Open App        │
      │              │ Settings        │
      │              │ Intent          │
      │              └────────┬────────┘
      │                       │
      │              ┌────────▼────────┐
      │              │ User Grants     │
      │              │ Permission      │
      │              └────────┬────────┘
      │                       │
      │              ┌────────▼────────┐
      │              │ App Resumes     │
      │              │ onResume()      │
      │              └────────┬────────┘
      │                       │
      │              ┌────────▼────────┐
      │              │ Re-check Perm   │
      │              └────────┬────────┘
      │                       │
      │              ┌────────▼────────┐
      │              │ Permission      │
      │              │ Granted!        │
      │              └────────┬────────┘
      │                       │
      │              ┌────────▼────────┐
      │              │ Enable Feature  │
      │              └─────────────────┘
      │
      ▼
┌────────────────────┐
│ Feature Disabled   │
│ Show In-App Guide: │
│ "Feature locked.   │
│  Grant permission  │
│  in Settings to    │
│  unlock."          │
└────────────────────┘
```

**Best Practices:**
1. **Educational Rationale:** Explain specific use case
2. **Visual Aids:** Screenshots showing the feature
3. **One-Tap to Settings:** Direct link to app settings
4. **Persistent Prompt:** Show again if critically needed
5. **Feature Lock UI:** Clear indication what's locked
6. **Alternative Paths:** Offer workarounds if possible

---

## 10. Permission Revocation at Runtime

```
┌────────────────────────────────────┐
│  App Running, Permission Granted   │
└──────────────────┬─────────────────┘
                   │
     ┌─────────────▼──────────────┐
     │  USER REVOKES PERMISSION   │
     │  (from system settings)    │
     └─────────────┬──────────────┘
                   │
     ┌─────────────▼──────────────┐
     │  App Attempts Operation    │
     │  Requiring Permission      │
     └─────────────┬──────────────┘
                   │
     ┌─────────────▼──────────────┐
     │  SecurityException Thrown  │
     └─────────────┬──────────────┘
                   │
     ┌─────────────▼──────────────┐
     │  Exception Caught in       │
     │  Try-Catch Block           │
     └─────────────┬──────────────┘
                   │
     ┌─────────────▼──────────────┐
     │  Check if Permission       │
     │  Actually Revoked          │
     │  (vs. other error)         │
     └─────────────┬──────────────┘
                   │
        ┌──────────┴──────────┐
        │                     │
        ▼                     ▼
┌────────────┐       ┌────────────────┐
│ Revoked    │       │ Other Error    │
└─────┬──────┘       └────────┬───────┘
      │                       │
      │                       ▼
      │              ┌────────────────┐
      │              │ Handle Error   │
      │              │ Appropriately  │
      │              └────────────────┘
      │
      ▼
┌─────────────────────────────┐
│  Pause Current Operation    │
└─────────────┬───────────────┘
              │
┌─────────────▼───────────────┐
│  Show Dialog:               │
│  "⚠️ Permission Revoked"    │
│                             │
│  "Storage permission was    │
│   revoked. Backup paused."  │
│                             │
│  "Re-grant permission to    │
│   continue, or switch to    │
│   app-private storage."     │
│                             │
│  [Switch to SAF]            │
│  [Re-grant Permission]      │
└─────────────┬───────────────┘
              │
   ┌──────────┴──────────┐
   │                     │
   ▼                     ▼
┌──────────┐    ┌─────────────┐
│ Switch   │    │ Re-grant    │
│ to SAF   │    │             │
└────┬─────┘    └──────┬──────┘
     │                 │
     │         ┌───────▼───────┐
     │         │ Request       │
     │         │ Permission    │
     │         │ Again         │
     │         └───────┬───────┘
     │                 │
     │         ┌───────┴───────┐
     │         │               │
     │         ▼               ▼
     │    ┌─────────┐    ┌─────────┐
     │    │ Granted │    │ Denied  │
     │    └────┬────┘    └────┬────┘
     │         │              │
     │         │         ┌────▼────┐
     │         │         │ Fall    │
     │         │         │ Back to │
     │         │         │ SAF     │
     │         │         └────┬────┘
     │         │              │
     └─────────┴──────────────┘
               │
┌──────────────▼──────────────┐
│  Resume Operation with      │
│  New Permission State       │
└─────────────────────────────┘
```

**Key Behaviors:**
1. **Graceful Degradation:** Never crash, always catch
2. **User Notification:** Clear explanation of what happened
3. **Easy Re-Grant:** One-tap to permission dialog
4. **Fallback Options:** Offer alternative approaches
5. **State Persistence:** Save progress, resume after grant

---

## Summary: Permission Request Checklist

### ✅ Do's
- Request permissions **lazily** (when feature used)
- Show **clear rationale** before requesting
- Provide **fallback options** when denied
- Handle **runtime revocation** gracefully
- Cache permission state to **avoid re-checks**
- Test on **all Android versions** (API 21-35)

### ❌ Don'ts
- Request all permissions **on first launch**
- Request permissions without **explanation**
- **Crash** when permission denied
- **Spam** re-request dialogs
- Assume permissions **persist forever**
- Ignore **API level differences**

---

**Document Version:** 1.0  
**Last Updated:** 2024-01  
**Maintained By:** ObsidianBackup Security Team
