# Critical Fixes Checklist - ObsidianBackup

## 🔴 Phase 1: Critical (MUST FIX - 2-3 weeks)

### Security Fixes

- [ ] **Fix hardcoded debug flag**
  - File: `app/src/main/java/com/obsidianbackup/engine/shell/AuditLogger.kt:40`
  - Change: `val isDebug = true` → `val isDebug = BuildConfig.DEBUG`
  - Add: Enable BuildConfig in app/build.gradle.kts

- [ ] **Increase PBKDF2 iterations**
  - File: `app/src/main/java/com/obsidianbackup/crypto/EncryptionEngine.kt:77`
  - Change: `iterations = 10000` → `iterations = 600000`
  - Note: May impact performance on older devices

- [ ] **Implement cloud sync checksums**
  - File: `app/src/main/java/com/obsidianbackup/cloud/CloudSyncManager.kt:188`
  - Implement SHA-256 checksum calculation
  - Add verification before/after upload
  - Store checksums in database

### Functionality Fixes

- [ ] **Complete backup engine implementation**
  - File: `app/src/main/java/com/obsidianbackup/engine/ObsidianBoxEngine.kt`
  - Lines 18, 61, 96, 104
  - Implement or use BusyBoxEngine as fallback
  - Add integration tests

- [ ] **Complete permission detection**
  - File: `app/src/main/java/com/obsidianbackup/model/PermissionManager.kt:6`
  - Implement ROOT/Shizuku/ADB detection
  - Add fallback to SAF

### Build Configuration

- [ ] **Enable ProGuard/R8**
  - File: `app/build.gradle.kts`
  - Change: `isMinifyEnabled = false` → `true`
  - Add: `isShrinkResources = true`
  - Configure proguard-rules.pro

- [ ] **Remove duplicate source trees**
  - Delete: `app/src/src/` directory
  - Verify no broken imports
  - Clean build to confirm

### Testing

- [ ] **Add unit tests for critical paths**
  - SafeShellExecutor command validation (80% coverage target)
  - EncryptionEngine encrypt/decrypt (100% coverage)
  - BackupOrchestrator retry logic
  - Cloud sync error handling
  - **Target: 60% overall coverage**

---

## 🟡 Phase 2: Security Hardening (2 weeks)

- [ ] **Integrate SQLCipher for database encryption**
  - Add dependency: `net.zetetic:android-database-sqlcipher`
  - Update Room configuration
  - Add migration for existing users

- [ ] **Add network security config**
  - Create: `app/src/main/res/xml/network_security_config.xml`
  - Implement certificate pinning for googleapis.com
  - Reference in AndroidManifest.xml

- [ ] **Enable biometric protection**
  - File: `app/src/main/java/com/obsidianbackup/crypto/EncryptionEngine.kt:54`
  - Change: `setUserAuthenticationRequired(false)` → `true`
  - Add: `.setUserAuthenticationValidityDurationSeconds(300)`

- [ ] **Security audit of shell execution**
  - Review all ShellExecutor usages
  - Ensure all go through SafeShellExecutor
  - Add integration tests for injection attempts

---

## 🟢 Phase 3: Feature Completion (3-4 weeks)

- [ ] **Complete incremental backup strategy**
  - File: `app/src/main/java/com/obsidianbackup/engine/IncrementalBackupStrategy.kt:37`
  - Implement file scanning logic
  - Add differential detection

- [ ] **Complete parallel backup engine**
  - File: `app/src/main/java/com/obsidianbackup/engine/ParallelBackupEngine.kt`
  - Lines 29, 35
  - Implement single app backup + aggregation

- [ ] **Finish or remove migration features**
  - Files: `migration/server/MigrationServer.kt`, `migration/WiFiDirectMigration.kt`
  - Complete mDNS discovery/advertising
  - Complete HTTP server
  - OR: Remove and defer to v2.0

- [ ] **Implement missing cloud sync features**
  - GoogleDriveProvider progress tracking (line 251)
  - Catalog sync (line 256)
  - Deduplication (CloudSyncRepository.kt:114)

---

## 📋 Phase 4: Polish (1 week)

- [ ] **Replace UI TODOs**
  - File: `app/src/main/java/com/obsidianbackup/ui/screens/OtherScreens.kt`
  - Lines 150-198
  - Implement actual UI controls

- [ ] **Add KDoc documentation**
  - All public classes in `engine/`, `domain/`, `plugins/`
  - Package-level documentation
  - Complex algorithm explanations

- [ ] **Create project documentation**
  - ARCHITECTURE.md
  - CONTRIBUTING.md
  - SECURITY.md
  - CHANGELOG.md

---

## ✅ Phase 5: QA (2 weeks)

- [ ] **Achieve 80%+ test coverage**
  - Unit tests for all use cases
  - Integration tests for workflows
  - UI tests for critical flows

- [ ] **Add crash reporting**
  - Firebase Crashlytics or similar
  - Error boundaries in UI
  - Offline crash storage

- [ ] **Performance testing**
  - Large backup scenarios (100+ apps)
  - Memory leak detection
  - Battery impact analysis

- [ ] **Beta testing program**
  - Internal dogfooding (1 week)
  - Closed beta (100 users, 1 week)
  - Open beta before public release

---

## 🎯 Progress Tracking

**Total Tasks:** 32  
**Phase 1 (Critical):** 7 tasks  
**Phase 2 (Security):** 4 tasks  
**Phase 3 (Features):** 4 tasks  
**Phase 4 (Polish):** 3 tasks  
**Phase 5 (QA):** 4 tasks  

**Current Status:** 0/32 (0%)

**Release Readiness:**
- [ ] Phase 1 complete → Internal alpha
- [ ] Phase 1-2 complete → Closed beta
- [ ] Phase 1-3 complete → Open beta
- [ ] Phase 1-5 complete → Production release

---

## 📞 Questions?

Refer to full audit report: `AUDIT_REPORT.md`

**Priority Order:**
1. Security fixes (debug flag, PBKDF2, checksums)
2. Core functionality (backup engine)
3. Build configuration (ProGuard, duplicates)
4. Testing infrastructure
5. Everything else

Good luck! 🚀
