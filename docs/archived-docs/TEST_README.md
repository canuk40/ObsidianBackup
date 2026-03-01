# ObsidianBackup Test Pyramid

This document describes the comprehensive test strategy for ObsidianBackup, following a multi-level test pyramid approach.

## Test Pyramid Structure

```
┌────────────────────────────────────┐
│   E2E Tests (5%)                   │  ← Full backup/restore flows
│   - Rooted emulator                │
│   - Espresso + UI Automator        │
└────────────────────────────────────┘
            ▼
┌────────────────────────────────────┐
│   Integration Tests (15%)          │  ← Engine + Storage integration
│   - Mock shell executor            │
│   - Real Room database             │
└────────────────────────────────────┘
            ▼
┌────────────────────────────────────┐
│   Unit Tests (80%)                 │  ← Pure logic
│   - ViewModels                     │
│   - Use cases                      │
│   - Utilities                      │
└────────────────────────────────────┘
```

## Running Tests

### Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Integration Tests
```bash
./gradlew connectedDebugAndroidTest
```

### E2E Tests
```bash
# Requires rooted emulator
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.emulator=true \
  -Pandroid.testInstrumentationRunnerArguments.rooted=true
```

## Test Categories

### Unit Tests (80%)
- **Pure Logic**: Test business logic without external dependencies
- **ViewModels**: UI state management and business logic
- **Use Cases**: Application-specific operations
- **Utilities**: Helper functions and data transformations

### Integration Tests (15%)
- **Engine + Storage**: Test backup engine with real database
- **Mock Shell Executor**: Simulate shell operations safely
- **Real Room Database**: Test data persistence layer
- **Component Integration**: Verify module interactions

### E2E Tests (5%)
- **Full User Flows**: Complete backup and restore scenarios
- **Rooted Emulator**: Required for system-level operations
- **UI Automation**: Espresso for app UI, UiAutomator for system UI
- **Real Device State**: Test with actual app installations

## Test Data Setup

For E2E tests, install test APKs on the emulator:
```bash
adb install test-apps/normal-app.apk
adb install test-apps/large-app.apk
adb install test-apps/protected-app.apk
```

## CI/CD Integration

Tests run automatically on:
- **Pull Requests**: Unit tests only
- **Main Branch**: Unit + Integration tests
- **Release Builds**: All tests including E2E

## Test Coverage Goals

- **Unit Tests**: >90% coverage
- **Integration Tests**: >80% coverage
- **E2E Tests**: Key user flows covered

## Performance Benchmarks

Tests include performance assertions:
- Backup time < 30 seconds for 100MB
- Restore time < 45 seconds for 100MB
- Memory usage < 200MB during operations
- UI responsiveness < 100ms for interactions
