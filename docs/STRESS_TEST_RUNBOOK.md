# ObsidianBackup Stress Test — Manual Runbook

This runbook covers UI-heavy features that require manual interaction and cannot be fully automated via ADB. Perform these tests while logcat is running (`./logcat_capture.sh start`).

---

## Prerequisites

1. Magisk-rooted emulator is running and accessible via `adb`
2. Both Free and Premium APKs are installed
3. Logcat capture is active: `./scripts/stress-test/logcat_capture.sh start`
4. A screenshot tool is ready (or use `adb exec-out screencap -p > screenshot.png`)

---

## 1. Onboarding Flow

| Step | Action | Expected Result | Pass? |
|------|--------|-----------------|-------|
| 1.1 | Clear app data: `adb shell pm clear <pkg>` | Data cleared | |
| 1.2 | Launch app | Onboarding screen appears | |
| 1.3 | Swipe through all onboarding pages | Each page renders correctly, no crash | |
| 1.4 | Complete onboarding (tap "Get Started" or similar) | Navigates to Dashboard | |
| 1.5 | Kill and relaunch app | Onboarding does NOT show again | |

---

## 2. Biometric Authentication

| Step | Action | Expected Result | Pass? |
|------|--------|-----------------|-------|
| 2.1 | Navigate to Settings → Biometric Auth | Biometric settings screen appears | |
| 2.2 | Enable biometric lock | Prompt for fingerprint/face appears | |
| 2.3 | Authenticate with biometric | Auth succeeds, setting is enabled | |
| 2.4 | Lock the app (home → return) | Biometric prompt appears on return | |
| 2.5 | Cancel biometric prompt | App shows fallback (PIN/password) | |
| 2.6 | Disable biometric lock | Setting reverts, no crash | |

> **Emulator Note:** Use `adb -e emu finger touch 1` to simulate fingerprint.

---

## 3. Cloud Provider OAuth Flows

### 3a. Google Drive

| Step | Action | Expected Result | Pass? |
|------|--------|-----------------|-------|
| 3a.1 | Settings → Cloud Providers → Google Drive | OAuth consent screen opens | |
| 3a.2 | Complete Google sign-in | Token saved, status shows "Connected" | |
| 3a.3 | Trigger cloud sync | Upload/download begins | |
| 3a.4 | Disconnect Google Drive | Status shows "Not connected" | |
| 3a.5 | Cancel OAuth mid-flow | App handles gracefully, no crash | |

### 3b. Dropbox

| Step | Action | Expected Result | Pass? |
|------|--------|-----------------|-------|
| 3b.1 | Settings → Cloud Providers → Dropbox | OAuth consent screen opens | |
| 3b.2 | Complete Dropbox sign-in | Token saved | |
| 3b.3 | Cancel OAuth mid-flow | Handled gracefully | |

### 3c. OneDrive

| Step | Action | Expected Result | Pass? |
|------|--------|-----------------|-------|
| 3c.1 | Settings → Cloud Providers → OneDrive | MSAL auth flow opens | |
| 3c.2 | Cancel auth | Handled gracefully | |

---

## 4. Gaming Backup UI

| Step | Action | Expected Result | Pass? |
|------|--------|-----------------|-------|
| 4.1 | Navigate to Gaming screen | Gaming backup UI renders | |
| 4.2 | Verify emulator detection list | Shows supported emulators (RetroArch, Dolphin, etc.) | |
| 4.3 | Select an emulator | Shows detected save files/ROMs | |
| 4.4 | Toggle "Include ROMs" | Setting changes without crash | |
| 4.5 | Start gaming backup | Backup begins for selected saves | |
| 4.6 | Check save state screenshot preview | Preview renders (if available) | |

---

## 5. Health Connect Integration

| Step | Action | Expected Result | Pass? |
|------|--------|-----------------|-------|
| 5.1 | Navigate to Health screen | Health Connect UI renders | |
| 5.2 | Tap "Connect to Health Connect" | Permission dialog appears | |
| 5.3 | Grant selected data types | Permissions saved | |
| 5.4 | Trigger health data backup | Backup runs for granted types | |
| 5.5 | Toggle privacy anonymization | Setting changes, UI updates | |
| 5.6 | Revoke Health Connect permissions | App handles gracefully | |

---

## 6. Plugin Management UI

| Step | Action | Expected Result | Pass? |
|------|--------|-----------------|-------|
| 6.1 | Navigate to Plugins screen | Plugin list renders | |
| 6.2 | Verify built-in plugins visible | Default automation + Filecoin shown | |
| 6.3 | Tap on a plugin | Plugin details/config screen opens | |
| 6.4 | Toggle plugin enable/disable | State changes, no crash | |
| 6.5 | Navigate back to plugin list | List is consistent | |

---

## 7. Community & Social Features

| Step | Action | Expected Result | Pass? |
|------|--------|-----------------|-------|
| 7.1 | Navigate to Community screen | Community UI renders | |
| 7.2 | View tips | Tips load and display | |
| 7.3 | View changelog | Changelog entries render | |
| 7.4 | Open feedback form | Feedback UI appears | |
| 7.5 | Submit feedback (or cancel) | No crash on submit or cancel | |

---

## 8. Accessibility (TalkBack)

| Step | Action | Expected Result | Pass? |
|------|--------|-----------------|-------|
| 8.1 | Enable TalkBack: `adb shell settings put secure enabled_accessibility_services com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService` | TalkBack active | |
| 8.2 | Navigate through main screens | All elements announced correctly | |
| 8.3 | Perform a backup via TalkBack | Backup completes with TalkBack | |
| 8.4 | Open Settings via TalkBack | All settings items focusable | |
| 8.5 | Disable TalkBack | Returns to normal | |

---

## 9. Theme & Display

| Step | Action | Expected Result | Pass? |
|------|--------|-----------------|-------|
| 9.1 | Switch to Dark Mode: `adb shell cmd uimode night yes` | App renders in dark theme | |
| 9.2 | Navigate all screens in dark mode | No text/contrast issues | |
| 9.3 | Switch to Light Mode: `adb shell cmd uimode night no` | App renders in light theme | |
| 9.4 | Enable large font: `adb shell settings put system font_scale 1.5` | Text scales, UI doesn't overflow | |
| 9.5 | Reset font scale: `adb shell settings put system font_scale 1.0` | Returns to normal | |

---

## 10. Drawer & Bottom Navigation

| Step | Action | Expected Result | Pass? |
|------|--------|-----------------|-------|
| 10.1 | Tap hamburger menu | Drawer opens | |
| 10.2 | Navigate to each drawer item | Each screen loads correctly | |
| 10.3 | Swipe drawer open/close | Smooth animation, no crash | |
| 10.4 | Tap each bottom nav item | Correct screen shown | |
| 10.5 | Double-tap bottom nav item | Scrolls to top or no-op, no crash | |
| 10.6 | Long-press bottom nav item | No crash (tooltip or no-op) | |

---

## 11. Notifications

| Step | Action | Expected Result | Pass? |
|------|--------|-----------------|-------|
| 11.1 | Start a backup operation | Foreground notification appears | |
| 11.2 | Check notification progress | Progress updates in notification | |
| 11.3 | Tap notification | Returns to app at correct screen | |
| 11.4 | Swipe-dismiss notification (if allowed) | Handled correctly | |
| 11.5 | Complete backup | Notification updates to "Complete" | |

---

## 12. Wear OS Data Layer (if Wear device available)

| Step | Action | Expected Result | Pass? |
|------|--------|-----------------|-------|
| 12.1 | Pair wear emulator | Data layer service starts | |
| 12.2 | Trigger backup from phone | Wear receives status update | |
| 12.3 | Check PhoneDataLayerListenerService | No crash in logcat | |

> **Note:** Requires Wear OS emulator — skip if not available.

---

## Logcat Commands Reference

```bash
# Start capture
./scripts/stress-test/logcat_capture.sh start

# Watch live for crashes
adb logcat -s AndroidRuntime:E ObsidianBackup:V | grep -E "FATAL|Exception|Error"

# Filter by tag
adb logcat -s ObsidianBackup:V

# Dump last 200 lines
adb logcat -d -t 200

# Stop capture
./scripts/stress-test/logcat_capture.sh stop
```

---

## Recording Results

After completing manual tests, update `docs/STRESS_TEST_RESULTS.md` with:
1. Fill in Pass/Fail for each step above
2. Note any crashes with logcat excerpts
3. Attach screenshots for visual bugs
4. Record device/emulator details
