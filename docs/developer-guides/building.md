# Building from Source

Build ObsidianBackup from source code.

## Prerequisites

- **JDK**: OpenJDK 17 or higher
- **Android SDK**: API 35 (Android 14+)
- **Build Tools**: 35.0.0
- **Git**: For cloning repository
- **Memory**: 8GB RAM minimum, 16GB recommended

## Clone Repository

```bash
git clone https://github.com/obsidianbackup/ObsidianBackup.git
cd ObsidianBackup
```

## Setup Android SDK

### Using Android Studio

1. Install Android Studio
2. Open project
3. SDK automatically downloads

### Using Command Line

```bash
# Install SDK
export ANDROID_SDK_ROOT=$HOME/Android/Sdk
sdkmanager "platforms;android-35"
sdkmanager "build-tools;35.0.0"
sdkmanager "platform-tools"
```

## Build Variants

### Debug Build

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### All Variants

```bash
./gradlew assemble
```

## Signing Configuration

### Debug Signing

Debug APKs are automatically signed with debug key.

### Release Signing

Create `keystore.properties`:

```properties
storeFile=/path/to/keystore.jks
storePassword=yourPassword
keyAlias=yourAlias
keyPassword=yourKeyPassword
```

Add to `app/build.gradle.kts`:

```kotlin
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

android {
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

## Build Types

### Debug

- Debuggable
- No obfuscation
- Debug symbols included

### Release

- Not debuggable
- ProGuard/R8 obfuscation
- Optimized
- Signed with release key

## Running Tests

### Unit Tests

```bash
./gradlew test
```

### Instrumentation Tests

```bash
./gradlew connectedAndroidTest
```

### Test Coverage

```bash
./gradlew jacocoTestReport
```

Report: `app/build/reports/jacoco/jacocoTestReport/html/index.html`

## Code Quality

### Lint

```bash
./gradlew lint
```

Report: `app/build/reports/lint-results.html`

### Detekt

```bash
./gradlew detekt
```

Report: `build/reports/detekt/detekt.html`

## Generate Documentation

### Dokka

```bash
./gradlew dokkaHtml
```

Output: `app/build/dokka/html/`

## Common Issues

### Out of Memory

```bash
# Increase heap size
export GRADLE_OPTS="-Xmx4g"
./gradlew build
```

### SDK Not Found

```bash
# Set SDK location
echo "sdk.dir=/path/to/android/sdk" > local.properties
```

### Cloud Provider Credentials

Sensitive API credentials are stored in `local.properties` (gitignored) and injected into `BuildConfig` at compile time. Copy `local.properties.template` to `local.properties` and fill in what you have:

```bash
cp local.properties.template local.properties
```

Currently supported credential keys:

```properties
# Backblaze B2 (backblaze.com → Account → App Keys)
b2.keyId=YOUR_KEY_ID
b2.applicationKey=YOUR_APPLICATION_KEY

# Play Integrity (Google Cloud Console)
play.integrity.cloud.project=YOUR_CLOUD_PROJECT_NUMBER
```

> Providers without credentials (Azure, Box, DigitalOcean, Oracle, Alibaba) will compile fine but will fail at the auth step at runtime. Add their keys to `local.properties` + a `buildConfigField` in `app/build.gradle.kts` when available.

### Build Cache Issues

```bash
# Clean build
./gradlew clean build --no-build-cache
```

## Next Steps

- [Testing Guide](testing.md)
- [Contributing Guidelines](contributing.md)
