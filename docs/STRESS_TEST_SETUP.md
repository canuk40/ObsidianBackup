# ObsidianBackup Stress Test — Environment Setup Guide

## Prerequisites

- Android SDK with `adb` in PATH
- Magisk-rooted Android emulator (API 34 / Android 14 recommended)
- Bash 4+ (Linux/macOS)
- Project built (`./gradlew assembleFreeDebug assemblePremiumDebug`)

---

## Quick Start

```bash
# 1. Verify emulator is connected and rooted
adb devices
adb shell su -c "id"

# 2. Build both APK variants (produces 1 APK each)
./gradlew assembleFreeDebug assemblePremiumDebug

# 3. Run the full stress test suite
cd scripts/stress-test
chmod +x *.sh
./run_all.sh

# 4. Run a specific test suite
./test_navigation.sh --variant free
./test_root_features.sh --variant premium

# 5. Run only Free variant tests
./run_all.sh --variant free

# 6. Run only Premium variant tests
./run_all.sh --variant premium
```

---

## Emulator Setup

### Using the Existing Script

```bash
# Start a rooted emulator (from project root)
./scripts/testing/start_rooted_emulator.sh
```

### Manual Setup

```bash
# Create AVD (Pixel 6 Pro, API 34, Google APIs — NOT Play Store)
sdkmanager "system-images;android-34;google_apis;x86_64"
avdmanager create avd -n "stress_test" -k "system-images;android-34;google_apis;x86_64" -d "pixel_6_pro"

# Start with writable system
emulator -avd stress_test -writable-system -no-snapshot-load -gpu host

# Root it
adb root
adb remount
```

### Magisk Installation

See [Magisk documentation](https://topjohnwu.github.io/Magisk/install.html) for emulator-specific installation steps. The key requirement is that `adb shell su -c "id"` returns `uid=0(root)`.

---

## Package Names

| Variant | Package Name |
|---------|-------------|
| Free (debug) | `com.obsidianbackup.free.debug` |
| Free (release) | `com.obsidianbackup.free` |
| Premium (debug) | `com.obsidianbackup.debug` |
| Premium (release) | `com.obsidianbackup` |

---

## Script Arguments

All test scripts accept:

| Argument | Default | Description |
|----------|---------|-------------|
| `--variant` | `free` | `free` or `premium` |
| `--build` | `debug` | `debug` or `release` |
| `--results` | auto-generated | Custom results directory |

---

## Results Directory Structure

```
scripts/stress-test/results/YYYYMMDD_HHMMSS/
├── FINAL_REPORT.txt          # Overall summary
├── results.csv               # Machine-readable: STATUS|SUITE|TEST|DESC
├── device_info.txt           # Emulator/device details
├── crashes.log               # Crash stack traces
├── logcat/
│   ├── logcat_full.log       # Complete logcat
│   └── crashes_live.log      # Real-time crash detection log
├── screenshots/
│   ├── fail_*.png            # Failure screenshots
│   ├── crash_*.png           # Crash screenshots
│   └── suite_fail_*.png      # Suite-level failures
├── *_summary.txt             # Per-suite summaries
├── mem_*.txt                 # Memory snapshots
├── cpu_*.txt                 # CPU samples
└── battery_*.txt             # Battery stats
```

---

## Logcat Filters

```bash
# All app logs (verbose)
adb logcat -s ObsidianBackup:V

# Crashes only
adb logcat -s AndroidRuntime:E

# Root detection
adb logcat -s RootDetector:V

# Shell executor
adb logcat -s ShellExecutor:V

# Cloud sync
adb logcat -s CloudSync:V

# Stress test markers
adb logcat -s STRESS_TEST:V

# Combined (used by logcat_capture.sh)
adb logcat -v threadtime ObsidianBackup:V BackupEngine:V CloudSync:V \
    RootDetector:V ShellExecutor:V Hilt:E AndroidRuntime:E \
    ActivityManager:W STRESS_TEST:V '*:S'
```

---

## ADB Cheatsheet

```bash
# Device info
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk

# Root commands
adb shell su -c "magisk -v"          # Magisk version
adb shell su -c "getenforce"         # SELinux status
adb shell su -c "ls /data/adb/modules/"  # Installed modules

# App management
adb install -r -t app.apk            # Install/update
adb shell pm clear <pkg>             # Clear data
adb shell am force-stop <pkg>        # Force stop
adb shell pm grant <pkg> <perm>      # Grant permission

# Performance
adb shell dumpsys meminfo <pkg>      # Memory info
adb shell dumpsys batterystats <pkg> # Battery stats
adb shell top -n 1 -b | grep <pkg>  # CPU usage

# Screenshots
adb exec-out screencap -p > screenshot.png

# Simulate events
adb shell input keyevent KEYCODE_HOME
adb shell input keyevent KEYCODE_BACK
adb shell settings put system user_rotation 1  # Landscape
```

---

## Troubleshooting

### "Permission denied" on scripts
```bash
chmod +x scripts/stress-test/*.sh
```

### App not installing
```bash
# Check for conflicting signatures
adb uninstall com.obsidianbackup.free.debug
adb uninstall com.obsidianbackup.debug
# Then reinstall
```

### Root access not working
```bash
# Verify Magisk
adb shell su -c "id"
# If denied, check Magisk app → Superuser → grant shell access
```

### Multiple APKs building
The `splits` config in `build.gradle.kts` is disabled by default. If you see multiple APKs:
```bash
# Verify splits are disabled
grep -A5 "splits" app/build.gradle.kts
# Should show isEnable = false (or conditional on enableSplits property)

# Force universal APK
./gradlew assembleFreeDebug  # Should produce 1 APK
```

### Logcat not capturing
```bash
# Check if capture is running
./scripts/stress-test/logcat_capture.sh status

# Restart capture
./scripts/stress-test/logcat_capture.sh stop
./scripts/stress-test/logcat_capture.sh start
```
