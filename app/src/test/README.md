# ObsidianBackup Test Suite

## Overview
Comprehensive testing infrastructure for ObsidianBackup with >80% code coverage target.

## Quick Start

### Run All Tests
```bash
./scripts/run_tests.sh
```

### Run Specific Test Suites
```bash
# Unit tests only
./gradlew testDebugUnitTest

# Integration tests
./gradlew connectedDebugAndroidTest

# With coverage
./gradlew testDebugUnitTest jacocoTestReport
```

## Test Categories

### 1. Unit Tests (`app/src/test/`)
- **Cloud Tests** (`cloud/CloudBackupTest.kt`)
  - Upload/download operations
  - File existence checks
  - Error handling
  
- **Engine Tests** (`engine/BackupEngineTest.kt`)
  - Backup operations
  - Restore operations
  - Progress tracking
  
- **Repository Tests** (`repository/BackupRepositoryTest.kt`)
  - CRUD operations
  - Flow observations
  - Error handling

- **Performance Benchmarks** (`benchmarks/BackupPerformanceBenchmark.kt`)
  - Small/large file backups
  - Compression overhead
  - Encryption overhead
  - Incremental vs full backup

### 2. Integration Tests (`app/src/androidTest/integration/`)
- Full backup/restore cycles
- Incremental backup flows
- Encrypted backup/restore
- Multi-cloud sync

### 3. UI Tests (`app/src/androidTest/ui/`)
- Dashboard navigation
- Backup screen interactions
- Settings configuration
- Error states

## Test Infrastructure

### Test Helpers
- **TestFixtures.kt** - Random data generators
- **TestDataFactory.kt** - Factory for test objects
- **HiltTestRunner.kt** - Custom test runner for Hilt

### Mock Implementations
- **MockCloudProvider** - Cloud storage operations
- **MockBackupRepository** - Data persistence
- **MockBackupEngine** - Backup/restore engine

## Coverage Reports

### Generate Coverage
```bash
./scripts/generate_coverage.sh
```

### View Reports
- HTML: `app/build/reports/jacoco/jacocoTestReport/html/index.html`
- XML: `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`

### Coverage Targets
| Component | Target |
|-----------|--------|
| Overall | >80% |
| Business Logic | >90% |
| Repositories | >85% |
| ViewModels | >80% |

## CI/CD Integration

Tests run automatically on:
- Push to main/develop
- Pull requests
- Scheduled nightly builds

See `.github/workflows/ci-testing.yml` for details.

## Writing Tests

### Example Unit Test
```kotlin
@Test
fun testBackupSuccess() = runTest {
    // Given
    val config = TestDataFactory.createBackupConfig()
    
    // When
    val result = backupEngine.startBackup(config)
    
    // Then
    assertThat(result.isSuccess).isTrue()
}
```

### Example UI Test
```kotlin
@Test
fun testDashboardNavigation() {
    composeTestRule.onNodeWithText("Dashboard")
        .assertExists()
        .performClick()
}
```

## Best Practices

1. **Use descriptive test names**
2. **Follow AAA pattern** (Arrange, Act, Assert)
3. **Keep tests independent**
4. **Clean up in @AfterEach**
5. **Use test fixtures for data**
6. **Mock external dependencies**
7. **Test edge cases and error paths**

## Troubleshooting

### Tests Not Running
```bash
./gradlew clean testDebugUnitTest
```

### Coverage Not Generated
```bash
./gradlew testDebugUnitTest jacocoTestReport
```

### Instrumentation Tests Fail
```bash
adb devices  # Check device connection
adb shell pm clear com.obsidianbackup  # Clear app data
```

## Documentation

See [TESTING_GUIDE.md](../TESTING_GUIDE.md) for comprehensive documentation.

## Test Statistics

- **Total Tests**: 80+
- **Unit Tests**: 50+
- **Integration Tests**: 15+
- **UI Tests**: 15+
- **Average Execution Time**: <60 seconds
- **Coverage**: >80%

---

Last Updated: 2024
