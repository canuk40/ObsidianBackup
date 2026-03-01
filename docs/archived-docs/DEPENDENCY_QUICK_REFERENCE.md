# Dependency Management Quick Reference

**Project:** ObsidianBackup  
**Last Updated:** December 2024

---

## 📋 Current Version Summary

### Core Versions
| Component | Version | Status |
|-----------|---------|--------|
| **Android Gradle Plugin** | 8.7.3 | ✅ Latest Stable |
| **Kotlin** | 1.9.25 | ✅ Latest Stable |
| **KSP** | 1.9.25-1.0.20 | ✅ Compatible |
| **Compile SDK** | 34 (Android 14) | ✅ Available |
| **Target SDK** | 34 | ✅ Recommended |
| **Min SDK** | 26 (Android 8.0) | ✅ Good Coverage |

### Framework Versions
| Framework | Version | Notes |
|-----------|---------|-------|
| **Compose BOM** | 2024.11.00 | ✅ Latest |
| **Compose Compiler** | 1.5.15 | ✅ Kotlin 1.9.25 |
| **Hilt** | 2.52 | ✅ Latest |
| **Coroutines** | 1.9.0 | ✅ Latest |
| **Room** | 2.6.1 | ✅ Latest Stable |
| **Navigation** | 2.8.4 | ✅ Updated |
| **WorkManager** | 2.9.1 | ✅ Updated |

---

## 🔧 Quick Commands

### Dependency Management
```bash
# View dependency tree
./gradlew app:dependencies

# View specific configuration
./gradlew app:dependencies --configuration premiumReleaseRuntimeClasspath

# Check for conflicts
./gradlew app:dependencies | grep "->"

# Refresh dependencies
./gradlew --refresh-dependencies

# Check for updates (if dependencyUpdates plugin added)
./gradlew dependencyUpdates
```

### Build Commands
```bash
# Clean build
./gradlew cleanAll

# Build debug
./gradlew assembleDebug

# Build release
./gradlew assembleRelease

# Build all variants
./gradlew assemble

# Generate app bundle
./gradlew bundleRelease
```

### Testing Commands
```bash
# Run unit tests
./gradlew test

# Run specific test
./gradlew testPremiumDebugUnitTest

# Run instrumentation tests
./gradlew connectedAndroidTest

# Run with coverage
./gradlew jacocoTestReport
```

---

## 📦 Module Structure

```
ObsidianBackup/
├── app/                    # Main Android app (SDK 34)
├── tv/                     # Android TV app (SDK 34)
├── wear/                   # Wear OS app (SDK 34)
└── enterprise/
    └── backend/            # Ktor backend (Kotlin 1.9.25)
```

---

## ⚙️ Version Catalog Usage

All versions managed in `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "1.9.25"
hilt = "2.52"
# ... etc

[libraries]
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }

[plugins]
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

**In build.gradle.kts:**
```kotlin
// Use version catalog reference
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)

// Or direct version (for non-catalog deps)
implementation("com.some.library:artifact:1.0.0")
```

---

## 🔍 Common Issues & Solutions

### Issue: SDK 35 not found
**Solution:** Use SDK 34 (Android 14)
```kotlin
compileSdk = 34
targetSdk = 34
buildToolsVersion = "34.0.0"
```

### Issue: Kotlin version conflict
**Solution:** Force version in all modules
```kotlin
configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:1.9.25")
    }
}
```

### Issue: Compose compiler mismatch
**Solution:** Align with Kotlin version
```kotlin
// Kotlin 1.9.25 → Compose Compiler 1.5.15
composeOptions {
    kotlinCompilerExtensionVersion = "1.5.15"
}
```

### Issue: Configuration cache failure
**Solution:** Temporarily disable or fix dependency issues
```bash
./gradlew assembleDebug --no-configuration-cache
```

---

## 🚨 Dependencies to Avoid

### Deprecated Libraries
| Library | Status | Alternative |
|---------|--------|-------------|
| `accompanist-systemuicontroller` | ⛔ Deprecated | Use Compose Edge-to-Edge |
| `accompanist-navigation-animation` | ⛔ Deprecated | Use Compose Navigation |
| `play-services-safetynet` | ⛔ Sunsetting | Use Play Integrity API |

### Alpha/Beta in Production
| Library | Issue | Recommendation |
|---------|-------|----------------|
| `biometric:1.2.0-alpha05` | ⚠️ Unstable | Use `1.1.0` stable |
| `security-crypto:1.1.0-alpha06` | ⚠️ Unstable | Use `1.0.0` stable |
| `leanback:1.2.0-alpha04` | ⚠️ Unstable | Use `1.0.0` stable |

---

## 📊 Dependency Categories

### Core Android (26 dependencies)
- AndroidX Core, Lifecycle, Activity
- Material Design Components
- DataStore, Credentials

### UI & Compose (15+ dependencies)
- Compose BOM, Material3, Navigation
- Lottie, Coil, Icons

### Backend & Storage (12 dependencies)
- Room, WorkManager, DataStore
- SQLCipher, Security Crypto

### Cloud Providers (8+ SDKs)
- Google Drive, Azure, AWS, Oracle, Alibaba
- WebDAV (Sardine), Backblaze, DigitalOcean

### Security (6 dependencies)
- Biometric, Credentials, Security Crypto
- SQLCipher, SafetyNet/Play Integrity
- Bouncy Castle (backend)

### ML & AI (5 dependencies)
- TensorFlow Lite, MLKit
- Text Recognition, Language ID

### Testing (25+ dependencies)
- JUnit 5, MockK, Espresso
- Robolectric, Truth, Turbine

### Development Tools
- Hilt, KSP, Dokka
- LeakCanary, JaCoCo, Detekt

---

## 🎯 Dependency Best Practices

### 1. Use Version Catalog
✅ Centralized version management  
✅ Type-safe accessors  
✅ Easier updates

### 2. Use BOM for Related Libraries
```kotlin
// Compose BOM manages all Compose versions
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.compose.ui)
implementation(libs.androidx.compose.material3)
```

### 3. Exclude Transitive Conflicts
```kotlin
implementation("com.some.library:artifact:1.0.0") {
    exclude(group = "conflicting.group", module = "module")
}
```

### 4. Force Consistent Versions
```kotlin
configurations.all {
    resolutionStrategy {
        force("consistent.library:artifact:1.0.0")
    }
}
```

### 5. Lock Dependency Versions
```kotlin
dependencyLocking {
    lockAllConfigurations()
}
```

---

## 📈 Monitoring & Updates

### Regular Checks
- **Weekly:** Check for security updates
- **Monthly:** Review dependency updates
- **Quarterly:** Major version upgrades

### Tools
```bash
# Gradle dependency updates plugin
./gradlew dependencyUpdates

# Check vulnerabilities (if plugin added)
./gradlew dependencyCheckAnalyze

# OWASP dependency check
./gradlew dependencyCheckAggregate
```

### Automation
Consider setting up:
- **Dependabot** (GitHub)
- **Renovate Bot**
- **Gradle Versions Plugin**

---

## 🔐 Security Checklist

- [ ] No alpha/beta libraries in production (except where unavoidable)
- [ ] Latest security patches applied
- [ ] Cryptography libraries updated (Bouncy Castle, SQLCipher)
- [ ] SafetyNet migrated to Play Integrity
- [ ] All dependencies from trusted sources
- [ ] Transitive dependencies reviewed
- [ ] No known CVEs in dependency tree

---

## 📝 Update Process

### 1. Check for Updates
```bash
./gradlew dependencyUpdates
```

### 2. Update Version Catalog
```toml
# gradle/libs.versions.toml
[versions]
someLibrary = "1.0.0" → "1.1.0"
```

### 3. Test Locally
```bash
./gradlew cleanAll
./gradlew assembleDebug
./gradlew test
```

### 4. Check for Breaking Changes
- Review library changelog
- Check migration guide
- Update code if needed

### 5. Commit & Deploy
```bash
git add gradle/libs.versions.toml
git commit -m "Update [library] to v1.1.0"
```

---

## 🆘 Troubleshooting

### Problem: Build fails after update
```bash
# Clear caches
./gradlew cleanBuildCache
./gradlew cleanAll

# Refresh dependencies
./gradlew --refresh-dependencies

# Invalidate IDE caches
# Android Studio → File → Invalidate Caches / Restart
```

### Problem: Dependency conflict
```bash
# Find conflict source
./gradlew app:dependencies | grep "Library_Name"

# See conflict resolution
./gradlew app:dependencyInsight --dependency Library_Name
```

### Problem: Version not found
```bash
# Check available versions
# Visit: https://mvnrepository.com/artifact/group.id/artifact-id

# Or check directly
./gradlew app:dependencies --refresh-dependencies
```

---

## 📚 Resources

### Official Documentation
- [Gradle Version Catalog](https://docs.gradle.org/current/userguide/platforms.html)
- [Android Gradle Plugin](https://developer.android.com/build)
- [Kotlin Compatibility](https://kotlinlang.org/docs/gradle-configure-project.html)
- [Compose Kotlin Compatibility](https://developer.android.com/jetpack/androidx/releases/compose-kotlin)

### Useful Links
- [Maven Central](https://search.maven.org/)
- [Google Maven Repository](https://maven.google.com/)
- [AndroidX Releases](https://developer.android.com/jetpack/androidx/versions)
- [Kotlin Releases](https://kotlinlang.org/docs/releases.html)

### Dependency Databases
- [MVN Repository](https://mvnrepository.com/)
- [Android Arsenal](https://android-arsenal.com/)
- [Libraries.io](https://libraries.io/)

---

## 🎓 Key Learnings

### Version Compatibility Matrix
| AGP | Gradle | Kotlin | Compose Compiler |
|-----|--------|--------|------------------|
| 8.7.x | 8.9-8.13 | 1.9.25 | 1.5.15 |
| 8.6.x | 8.7-8.11 | 1.9.22 | 1.5.10 |
| 8.5.x | 8.7-8.10 | 1.9.20 | 1.5.8 |

### Common Patterns
1. **Always align SDK versions** across modules
2. **Use BOM** for framework libraries (Compose, Firebase)
3. **Force versions** for common conflicts (Kotlin stdlib, OkHttp)
4. **Exclude transitive** conflicts at source
5. **Lock versions** for reproducible builds

---

## ✅ Checklist for New Dependencies

Before adding a dependency:
- [ ] Check latest stable version
- [ ] Review library maturity (alpha/beta/stable)
- [ ] Check compatibility with existing versions
- [ ] Review library size impact
- [ ] Check for transitive dependency conflicts
- [ ] Review library license
- [ ] Check security track record
- [ ] Confirm maintenance status (last update)
- [ ] Add to version catalog if reused
- [ ] Document reason for inclusion

---

**END OF QUICK REFERENCE**

For detailed analysis, see `DEPENDENCY_ANALYSIS.md`  
For applied fixes, see `DEPENDENCY_FIXES_APPLIED.md`
