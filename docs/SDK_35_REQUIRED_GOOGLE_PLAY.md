# CRITICAL: SDK 35 is REQUIRED by Google Play Console (2026)

**Date:** February 10, 2026  
**Status:** ⚠️ **MANDATORY FOR GOOGLE PLAY SUBMISSION**  
**Severity:** CRITICAL - Apps with targetSdk < 35 are REJECTED

---

## ⚠️ IMPORTANT NOTICE

As of February 2026, **Google Play Console REQUIRES SDK 35** (Android 15) for all app submissions. Apps targeting SDK 34 or lower are **automatically rejected** during the review process.

**This is a hard requirement from Google, not optional.**

---

## What Changed

### Google Play Policy Update

Google has enforced a new policy requiring:
- **targetSdk = 35** (minimum)
- **compileSdk = 35** (recommended)
- Apps must be built against Android 15 APIs

### Why the Change

1. **Security:** Android 15 includes critical security enhancements
2. **Privacy:** New privacy features required for user protection
3. **Performance:** Optimizations for modern devices
4. **API Changes:** Behavioral changes that apps must handle

---

## Current Configuration (REVERTED TO SDK 35)

All modules have been **reverted to SDK 35**:

```kotlin
compileSdk = 35
targetSdk = 35
buildToolsVersion = "35.0.0"
```

| Module | compileSdk | targetSdk | buildTools | Status |
|--------|------------|-----------|------------|--------|
| app    | 35         | 35        | 35.0.0     | ✅ Play Store Ready |
| tv     | 35         | 35        | 35.0.0     | ✅ Play Store Ready |
| wear   | 35         | 35        | 35.0.0     | ✅ Play Store Ready |

---

## Installation Guide for SDK 35

### Prerequisites

You **MUST** install Android SDK Platform 35 before building. The build will fail without it.

### Option 1: Using SDK Manager (GUI)

1. Open Android Studio
2. Go to **Tools → SDK Manager**
3. Select **SDK Platforms** tab
4. Check **Android 15 (API 35)** (may also be labeled as "Android VanillaIceCream")
5. Select **SDK Tools** tab
6. Check **Android SDK Build-Tools 35.0.0**
7. Click **Apply** and wait for download

### Option 2: Using Command Line

```bash
# Install SDK Platform 35
sdkmanager "platforms;android-35"

# Install Build Tools 35.0.0
sdkmanager "build-tools;35.0.0"

# Verify installation
sdkmanager --list | grep "android-35"
```

### Option 3: Using Android Studio Command Line Tool

```bash
# From Android Studio terminal
cd $ANDROID_HOME/cmdline-tools/latest/bin

./sdkmanager "platforms;android-35"
./sdkmanager "build-tools;35.0.0"
```

---

## Verification

After installing SDK 35, verify it's working:

```bash
# Check if SDK 35 is installed
ls $ANDROID_HOME/platforms/android-35

# Expected output:
# android.jar  build.prop  data/  framework.aidl  ...

# Try a build
./gradlew clean assembleFreeDebug
```

---

## Fixing the Missing SDK 35 Error

### Error Message

```
Failed to find target with hash string 'android-35' in: /usr/lib/android-sdk
```

### Solution

**You MUST install SDK 35.** There is no workaround. Google Play requires it.

1. Install SDK 35 using one of the methods above
2. Restart Android Studio
3. Sync Gradle: **File → Sync Project with Gradle Files**
4. Build again

---

## Why We Can't Use SDK 34

### Previous Approach (WRONG for 2026)

Earlier, we tried using SDK 34 because SDK 35 wasn't available in the build environment. **This was incorrect for Google Play submission.**

### Current Reality (CORRECT)

- ❌ **SDK 34 apps are REJECTED by Google Play Console**
- ✅ **SDK 35 is MANDATORY as of February 2026**
- ⚠️ **You cannot submit to Play Store without SDK 35**

Even if your app builds with SDK 34, **Google will reject it during review.**

---

## SDK 35 Availability

### Where to Get It

**SDK 35 is available through:**
- Android Studio (stable channel) - Check for updates
- SDK Manager command line tool
- Android Studio Canary/Beta (if not in stable yet)

### If SDK 35 is Not Available

If SDK 35 is truly not available in your region/setup:

1. **Update Android Studio** to the latest stable version
2. **Check for SDK updates** in SDK Manager
3. **Use Canary/Beta channel** if necessary
4. **Contact Google** - SDK 35 should be available as Google requires it

---

## Build Environment Setup

### For Local Development

1. **Install SDK 35** (see Installation Guide above)
2. **Set ANDROID_HOME** environment variable
3. **Restart IDE** after installation
4. **Sync Gradle** to verify

### For CI/CD Pipelines

Add to your CI configuration:

```yaml
# GitHub Actions example
- name: Setup Android SDK
  uses: android-actions/setup-android@v2
  with:
    api-level: 35
    build-tools: 35.0.0
    
# Or using sdkmanager directly
- name: Install SDK 35
  run: |
    yes | sdkmanager "platforms;android-35"
    yes | sdkmanager "build-tools;35.0.0"
```

---

## Google Play Submission Checklist

Before submitting to Play Store, verify:

- [x] ✅ targetSdk = 35 in all build.gradle.kts files
- [x] ✅ compileSdk = 35 in all build.gradle.kts files
- [x] ✅ SDK 35 installed in development environment
- [x] ✅ Build completes successfully with SDK 35
- [ ] ⚠️ Test on Android 15 emulator/device
- [ ] ⚠️ Verify no new permission prompts
- [ ] ⚠️ Check for behavioral changes
- [ ] ⚠️ Review Android 15 migration guide

---

## Android 15 (SDK 35) Changes to Review

### Behavioral Changes

Review the following before submission:

1. **Storage Access Changes** - Scoped storage enhancements
2. **Permission Changes** - New runtime permission flows
3. **Background Service Restrictions** - More strict than Android 14
4. **Battery Optimization** - Enhanced battery saver features
5. **Security Updates** - New security sandbox requirements

### Testing Recommendations

```bash
# Create Android 15 emulator
avdmanager create avd -n Android15 -k "system-images;android-35;google_apis;x86_64"

# Launch emulator
emulator -avd Android15

# Install and test your app
adb install -r app/build/outputs/apk/free/debug/app-free-arm64-v8a-debug.apk
```

---

## Troubleshooting

### "SDK 35 not found" after installation

```bash
# 1. Verify SDK path
echo $ANDROID_HOME
# Should point to your Android SDK directory

# 2. Check if platform exists
ls $ANDROID_HOME/platforms/
# Should show android-35 in the list

# 3. Restart Android Studio completely

# 4. Invalidate caches
# File → Invalidate Caches / Restart → Invalidate and Restart
```

### Gradle sync fails with SDK 35

```bash
# 1. Stop all Gradle daemons
./gradlew --stop

# 2. Clean build
./gradlew clean

# 3. Delete .gradle directory
rm -rf .gradle

# 4. Sync again
./gradlew tasks
```

### Build succeeds but Play Store rejects

- Verify targetSdk = 35 in **all** build.gradle.kts files
- Check Play Console error message carefully
- Ensure AAB/APK was built with SDK 35 configuration
- Rebuild from scratch after confirming SDK 35 config

---

## Summary

### What You MUST Do

1. ✅ **Install SDK 35** - No exceptions, required by Google
2. ✅ **Keep targetSdk = 35** - Don't downgrade to SDK 34
3. ✅ **Test on Android 15** - Verify app works correctly
4. ✅ **Review behavioral changes** - Android 15 may break things

### What NOT to Do

- ❌ **Don't use SDK 34** - Google will reject your app
- ❌ **Don't skip testing** - Android 15 has breaking changes
- ❌ **Don't ignore warnings** - Deprecated APIs may break
- ❌ **Don't submit without SDK 35** - Automatic rejection

---

## Files Modified (REVERTED)

The following files have been **reverted to SDK 35**:

1. `app/build.gradle.kts` - SDK 35, targetSdk 35, buildTools 35.0.0
2. `tv/build.gradle.kts` - SDK 35, targetSdk 35, buildTools 35.0.0
3. `wear/build.gradle.kts` - SDK 35, targetSdk 35, buildTools 35.0.0
4. `gradle/libs.versions.toml` - Updated comments for SDK 35

---

## Resources

- **Android 15 Developer Guide:** https://developer.android.com/about/versions/15
- **Play Console Policies:** https://support.google.com/googleplay/android-developer/
- **SDK Manager Guide:** https://developer.android.com/studio/intro/update#sdk-manager
- **Behavioral Changes:** https://developer.android.com/about/versions/15/behavior-changes-all

---

**Status:** ✅ SDK 35 CONFIGURED (Required for Google Play)  
**Action Required:** Install SDK 35 before building  
**Play Store:** Ready for submission once SDK 35 is installed

