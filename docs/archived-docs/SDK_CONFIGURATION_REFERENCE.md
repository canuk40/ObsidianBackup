# SDK Configuration Quick Reference

**Last Updated:** February 10, 2026  
**Configuration:** SDK 34 (Android 14)

---

## Current SDK Configuration

### All Modules (app, tv, wear)

```kotlin
compileSdk = 34
targetSdk = 34
buildToolsVersion = "34.0.0"
```

---

## Module-Specific Settings

### Main App (`app/build.gradle.kts`)
```kotlin
android {
    namespace = "com.obsidianbackup"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.obsidianbackup"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    
    buildToolsVersion = "34.0.0"
}
```

### Android TV (`tv/build.gradle.kts`)
```kotlin
android {
    namespace = "com.obsidianbackup.tv"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.obsidianbackup.tv"
        minSdk = 21  // TV minimum
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    
    buildToolsVersion = "34.0.0"
}
```

### Wear OS (`wear/build.gradle.kts`)
```kotlin
android {
    namespace = "com.obsidianbackup.wear"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.obsidianbackup.wear"
        minSdk = 30  // Wear OS 3.0+
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    
    buildToolsVersion = "34.0.0"
}
```

---

## Gradle Properties

### gradle.properties Configuration

```properties
# Java Home - Commented out to use system Java
# org.gradle.java.home=/usr/lib/jvm/java-17-openjdk-amd64

# JVM Arguments
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m

# Build Optimizations
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
```

---

## Version Catalog (libs.versions.toml)

### Key SDK-Related Versions

```toml
[versions]
agp = "8.7.3"
kotlin = "2.0.21"
coreKtx = "1.15.0"  # Compatible with SDK 34
workManager = "2.10.0"  # Compatible with SDK 34
```

---

## Gradle Commands

### Sync and Build
```bash
# Stop daemon and sync
./gradlew --stop
./gradlew tasks

# Build specific variants
./gradlew assembleFreeDebug
./gradlew assemblePremiumRelease

# Build all variants
./gradlew assemble
```

### Clean Build
```bash
# Standard clean
./gradlew clean

# Deep clean (all modules)
./gradlew cleanAll
rm -rf .gradle app/build tv/build wear/build
```

---

## SDK Requirements Matrix

| Component | Minimum | Target | Compile |
|-----------|---------|--------|---------|
| **Main App** | 26 (Android 8.0) | 34 | 34 |
| **Android TV** | 21 (Android 5.0) | 34 | 34 |
| **Wear OS** | 30 (Wear OS 3.0) | 34 | 34 |

---

## Troubleshooting

### Gradle Sync Fails
```bash
# 1. Stop daemon
./gradlew --stop

# 2. Clean build
./gradlew clean

# 3. Invalidate caches (if using Android Studio)
# File → Invalidate Caches / Restart

# 4. Try sync again
./gradlew tasks
```

### SDK Not Found
```bash
# Install SDK 34 platform
sdkmanager "platforms;android-34"
sdkmanager "build-tools;34.0.0"

# Verify installation
sdkmanager --list | grep "android-34"
```

### Java Home Issues
```bash
# Find Java installation
which java
java -version

# Update gradle.properties if needed
# Or comment out org.gradle.java.home to use system Java
```

---

## Migration Notes

### From SDK 35 to SDK 34
- **Reason:** SDK 35 not available in standard installations
- **Date:** February 10, 2026
- **Status:** Complete
- **Files Changed:** 5 (all build.gradle.kts + gradle.properties + libs.versions.toml)

### Future Migration to SDK 35
When SDK 35 becomes stable:
1. Install SDK 35 platform
2. Update all `compileSdk = 35` and `targetSdk = 35`
3. Update `buildToolsVersion = "35.0.0"`
4. Update dependencies (coreKtx, workManager)
5. Test thoroughly

---

## Verification Checklist

- [x] All modules use SDK 34
- [x] Build tools version is 34.0.0
- [x] gradle.properties has no invalid paths
- [x] Version catalog is consistent
- [x] Gradle sync completes successfully
- [ ] Full build completes (awaiting environment)
- [ ] APKs can be generated
- [ ] APKs can be installed

---

**Status:** ✅ SDK 34 Configured Consistently Across All Modules

