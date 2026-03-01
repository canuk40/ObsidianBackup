# Root Detection Quick Reference

## Overview

The `RootDetectionManager` provides comprehensive root detection using multiple methods to minimize false positives while maintaining high accuracy.

## Quick Start

### Basic Usage

```kotlin
// Inject the manager
@Inject lateinit var rootDetectionManager: RootDetectionManager

// Quick synchronous check (< 100ms)
val quickResult = rootDetectionManager.quickRootCheck()
if (quickResult) {
    // Handle potential root
}

// Comprehensive async check (recommended)
lifecycleScope.launch {
    val result = rootDetectionManager.detectRoot()
    
    when (result.confidence) {
        DetectionConfidence.LOW -> {
            // Minimal indicators, might be false positive
            // Consider allowing with warning
        }
        DetectionConfidence.MEDIUM -> {
            // Single strong indicator detected
            // Probably rooted
        }
        DetectionConfidence.HIGH -> {
            // Multiple indicators or SafetyNet failed
            // Definitely rooted
        }
        DetectionConfidence.CRITICAL -> {
            // Many indicators detected
            // Actively rooted with management apps
        }
    }
}
```

## Detection Methods

| Method | Confidence | False Positive Risk | Description |
|--------|-----------|---------------------|-------------|
| SafetyNet | HIGH | Very Low | Google's attestation API |
| Root Apps | MEDIUM | Very Low | Magisk, SuperSU, etc. |
| Su Binaries | MEDIUM | Very Low | /system/bin/su, etc. |
| Test-Keys | LOW | Low | Build signed with test-keys |
| Dangerous Props | MEDIUM | Low | ro.secure=0 |
| Writable System | MEDIUM | Low | /system partition writable |
| Busybox (system) | LOW | **FIXED** | Only /system or /sbin |

## False Positive Prevention

### ✅ These DO NOT indicate root:
- Developer mode enabled
- ADB debugging enabled
- Custom ROM with release-keys
- Unlocked bootloader (alone)
- Xposed/EdXposed without root
- Busybox in `/data/local/`
- Emulator environment

### ✅ These DO indicate root:
- Magisk/SuperSU package installed
- Su binary in `/system` or `/sbin`
- Build signed with test-keys
- SafetyNet attestation fails
- `/system` partition writable
- `ro.secure=0` property

## Testing

### Run All Tests
```bash
./run_root_detection_tests.sh --all
```

### Run Specific Category
```bash
./run_root_detection_tests.sh --true-positive
./run_root_detection_tests.sh --false-positive
./run_root_detection_tests.sh --edge-case
./run_root_detection_tests.sh --confidence
./run_root_detection_tests.sh --quick
```

### Run with Gradle
```bash
./gradlew :app:testFreeDebugUnitTest \
  --tests "com.obsidianbackup.security.RootDetectionValidationTest"
```

## Security Policies

### High Security (Banking, Payments)
```kotlin
val result = rootDetectionManager.detectRoot()

if (result.isRooted && result.confidence >= DetectionConfidence.MEDIUM) {
    // Block access
    showRootDetectedDialog()
    finishAffinity()
}
```

### Medium Security (Social Media)
```kotlin
val result = rootDetectionManager.detectRoot()

if (result.isRooted && result.confidence >= DetectionConfidence.HIGH) {
    // Warn user but allow access
    showSecurityWarning()
}
```

### Low Security (Games, Utilities)
```kotlin
val result = rootDetectionManager.detectRoot()

if (result.isRooted && result.confidence == DetectionConfidence.CRITICAL) {
    // Optional warning
    if (shouldWarnUser()) {
        showSecurityNotice()
    }
}
```

## Debugging

### Enable Verbose Logging
```kotlin
// Logs are automatically sent to ObsidianLogger
// Check logs with:
adb logcat | grep "RootDetection"
```

### Common Log Messages
```
I/RootDetection: Root detection result: rooted=true, confidence=HIGH
W/RootDetection: Detected root app: com.topjohnwu.magisk
W/RootDetection: Detected su binary: /system/bin/su
W/RootDetection: Build signed with test-keys: test-keys
W/RootDetection: Busybox detected in system location: /system/bin/busybox
```

## Result Structure

```kotlin
data class RootDetectionResult(
    val isRooted: Boolean,              // Overall result
    val detectionMethod: String,        // Semicolon-separated methods
    val safetyNetResult: SafetyNetResult?, // SafetyNet details
    val detectedRootApps: List<String>,    // Found root apps
    val detectedSuPaths: List<String>,     // Found su binaries
    val dangerousProps: List<String>,      // Dangerous properties
    val confidence: DetectionConfidence    // Confidence level
)
```

## SafetyNet Configuration

### API Key Setup
1. Add to `local.properties` (NOT committed):
   ```properties
   safetynet.api.key=YOUR_ACTUAL_KEY_HERE
   ```

2. Key is injected into `BuildConfig.SAFETYNET_API_KEY`

3. For production, use CI/CD environment variables or fetch from backend

### Timeout
Default: 10 seconds (configurable in code)

### Unavailable Handling
If Google Play Services unavailable, returns `null` and continues with other checks.

## Performance

| Check Type | Duration | Use Case |
|------------|----------|----------|
| Quick Check | < 100ms | Initial screening |
| Full Detection (no SafetyNet) | 100-200ms | Offline check |
| Full Detection (with SafetyNet) | 1-10 seconds | Complete validation |

## Known Limitations

1. **Systemless Root with Hiding**: May evade package manager detection
2. **Custom Root Methods**: Novel root methods not in detection list
3. **Advanced Evasion**: Sophisticated hiding techniques may succeed
4. **Emulator Detection**: Doesn't specifically flag emulators as rooted

## Troubleshooting

### Issue: False Positive on Custom ROM
**Solution**: Check if ROM uses release-keys. Only test-keys should trigger.

### Issue: Missed Root Detection
**Solution**: Check which indicators should be present. File a bug with details.

### Issue: SafetyNet Always Fails
**Solution**: 
1. Verify API key is set correctly
2. Check Google Play Services is available
3. Check timeout isn't too short

### Issue: Tests Won't Run
**Solution**:
```bash
# Clean and rebuild
./gradlew clean
./gradlew :app:testFreeDebugUnitTest --rerun-tasks
```

## Contributing

### Adding New Root Detection Method
1. Add detection logic to `RootDetectionManager`
2. Add test cases to `RootDetectionValidationTest`
3. Update documentation
4. Verify no false positives introduced

### Adding New Root App
Add package name to `ROOT_APPS` array:
```kotlin
private val ROOT_APPS = arrayOf(
    // ... existing apps ...
    "com.newroot.app"  // Add here
)
```

### Adding New Su Path
Add path to `SU_PATHS` array:
```kotlin
private val SU_PATHS = arrayOf(
    // ... existing paths ...
    "/new/path/to/su"  // Add here
)
```

## Resources

- **Test File**: `app/src/test/java/com/obsidianbackup/security/RootDetectionValidationTest.kt`
- **Implementation**: `app/src/main/java/com/obsidianbackup/security/RootDetectionManager.kt`
- **Test Runner**: `run_root_detection_tests.sh`
- **Validation Report**: `docs/ROOT_DETECTION_VALIDATION_REPORT.md`
- **OWASP MASVS**: https://github.com/OWASP/owasp-masvs

## Support

For issues or questions:
1. Check existing tests for examples
2. Review validation report for common scenarios
3. Check logs for detection details
4. File an issue with device details and logs

---

**Last Updated**: 2024-02-10  
**Version**: 1.0  
**Maintainer**: ObsidianBackup Security Team
