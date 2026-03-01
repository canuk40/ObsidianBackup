# Functional Tests Quick Reference

## Test File Locations

```
app/src/test/java/com/obsidianbackup/functional/
├── BackupFunctionalTests.kt           (651 lines, 19 tests)
├── RestoreFunctionalTests.kt          (583 lines, 16 tests)
├── CloudSyncFunctionalTests.kt        (553 lines, 21 tests)
├── AutomationFunctionalTests.kt       (556 lines, 26 tests)
├── WiFiDirectFunctionalTests.kt       (352 lines, 15 tests)
└── PermissionCapabilityTests.kt       (549 lines, 24 tests)
```

## Quick Test Commands

### Run All Functional Tests
```bash
./gradlew :app:testFreeDebugUnitTest --tests "com.obsidianbackup.functional.*"
```

### Run Individual Suites
```bash
# Backup
./gradlew test --tests "*BackupFunctionalTests*"

# Restore
./gradlew test --tests "*RestoreFunctionalTests*"

# Cloud
./gradlew test --tests "*CloudSyncFunctionalTests*"

# Automation
./gradlew test --tests "*AutomationFunctionalTests*"

# WiFi Direct
./gradlew test --tests "*WiFiDirectFunctionalTests*"

# Permissions
./gradlew test --tests "*PermissionCapabilityTests*"
```

## What Each Test File Covers

### 1. BackupFunctionalTests
- Full backup (10 apps)
- Incremental backup with baseline
- Scheduled backups
- Merkle tree verification
- Checksum verification
- Encryption on/off
- Batch operations (1-100 apps)
- Progress tracking
- Retry on failure

### 2. RestoreFunctionalTests
- Full restore
- Selective restore (APK only, DATA only)
- ACID transactions
- Rollback on failure
- Safety backups
- SELinux context restoration
- Dry run mode
- Performance (1-100 apps)

### 3. CloudSyncFunctionalTests
- Google Drive (success, rate limit, quota)
- WebDAV (auth, timeout)
- Rclone (8 backends)
- Conflict resolution (4 strategies)
- Network retry
- Offline queue
- Resume interrupted uploads
- Bandwidth throttling

### 4. AutomationFunctionalTests
- DefaultAutomationPlugin
- App install/update detection
- Scheduled backups (nightly, weekly)
- Tasker integration
- Trigger conditions (battery, WiFi, charging, storage)
- Complex conditions (AND, OR)
- Event propagation
- Error handling

### 5. WiFiDirectFunctionalTests
- Peer discovery
- Server/client connection
- Chunked transfer (64KB chunks)
- Chunk verification
- Transfer resume
- Progress persistence
- Performance testing

### 6. PermissionCapabilityTests
- Root detection
- Shizuku detection
- APK/Data access detection
- Graceful degradation (Root → Shizuku → SAF)
- Permission request flows
- Capability priority
- Permission profiles
- Runtime permissions

## Test Statistics

| Metric | Count |
|--------|-------|
| Total Lines | 3,044 |
| Test Methods | 121 |
| Nested Classes | 46 |
| TODO Comments | 0 |
| Test Files | 6 |

## Key Test Patterns Used

### 1. Arrange-Act-Assert
```kotlin
@Test
fun testExample() = runTest {
    // Arrange
    val input = createTestData()
    coEvery { mock.method() } returns expected
    
    // Act
    val result = systemUnderTest.execute(input)
    
    // Assert
    assertThat(result).isEqualTo(expected)
    coVerify { mock.method() }
}
```

### 2. Parameterized Tests
```kotlin
@ParameterizedTest
@ValueSource(ints = [1, 5, 10, 50, 100])
fun testWithDifferentSizes(size: Int) = runTest {
    // Test with variable size
}
```

### 3. Nested Test Classes
```kotlin
@Nested
@DisplayName("Feature Category")
inner class FeatureTests {
    @Test
    fun testSpecificScenario() { }
}
```

### 4. Failure Path Testing
```kotlin
@Test
fun testFailureScenario() = runTest {
    coEvery { mock.method() } throws Exception("Error")
    
    val result = systemUnderTest.execute()
    
    assertThat(result).isInstanceOf(Failure::class.java)
}
```

### 5. Retry Logic Testing
```kotlin
@Test
fun testRetry() = runTest {
    var attempts = 0
    coEvery { mock.method() } answers {
        attempts++
        if (attempts < 3) throw Exception()
        else Success()
    }
    
    val result = systemUnderTest.execute()
    
    assertThat(attempts).isEqualTo(3)
}
```

## Dependencies

All required dependencies are already in `build.gradle.kts`:
- JUnit 5
- MockK
- Google Truth
- Coroutines Test
- Turbine

## Running Tests in IDE

### Android Studio / IntelliJ IDEA
1. Right-click on test file or package
2. Select "Run Tests in 'functional'"
3. View results in Test Results panel

### Command Line with Filtering
```bash
# Run tests matching pattern
./gradlew test --tests "*Backup*Success*"

# Run with info logging
./gradlew test --tests "*Functional*" --info

# Run with stack traces
./gradlew test --tests "*Functional*" --stacktrace
```

## Continuous Integration

### GitHub Actions Example
```yaml
- name: Run Functional Tests
  run: ./gradlew :app:testFreeDebugUnitTest --tests "com.obsidianbackup.functional.*"
```

### Jenkins Example
```groovy
stage('Functional Tests') {
    steps {
        sh './gradlew :app:testFreeDebugUnitTest --tests "com.obsidianbackup.functional.*"'
    }
}
```

## Test Output

### Success Output
```
BackupFunctionalTests > Full Backup Tests > Should perform full backup of all apps successfully PASSED
RestoreFunctionalTests > Full Restore Tests > Should restore all apps successfully PASSED
...
BUILD SUCCESSFUL in 15s
121 tests completed, 121 succeeded
```

### Failure Output
```
BackupFunctionalTests > testBackup FAILED
    Expected: BackupResult.Success
    Actual: BackupResult.Failure(reason="Disk full")
    
BUILD FAILED in 12s
120 tests completed, 119 succeeded, 1 failed
```

## Coverage Report

To generate test coverage:
```bash
./gradlew :app:jacocoTestReport
```

View report at:
```
app/build/reports/jacoco/test/html/index.html
```

## Troubleshooting

### Tests Not Found
```bash
# Ensure tests are compiled
./gradlew :app:compileFreeDebugUnitTestKotlin
```

### MockK Errors
```kotlin
// Ensure relaxed mocking for complex objects
val mock = mockk<Interface>(relaxed = true)
```

### Coroutine Test Errors
```kotlin
// Use runTest for suspend functions
@Test
fun test() = runTest {
    // Test coroutine code
}
```

## Best Practices Followed

✅ **Descriptive test names** - Uses @DisplayName with clear descriptions
✅ **Single responsibility** - Each test validates one scenario
✅ **Fast execution** - All tests use mocks, no real I/O
✅ **Isolated tests** - No shared mutable state between tests
✅ **Comprehensive assertions** - Multiple assertions per test
✅ **Both paths tested** - Success and failure paths covered
✅ **Organized structure** - Nested classes for logical grouping
✅ **Parameterized where appropriate** - Reduces code duplication
✅ **Follows AAA pattern** - Arrange, Act, Assert

## Further Reading

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [MockK Documentation](https://mockk.io/)
- [Google Truth](https://truth.dev/)
- [Kotlin Coroutines Test](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/)
