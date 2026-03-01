# Specification Gaps - Action Plan

**Date:** February 8, 2026  
**Current Compliance:** 87%  
**Target Compliance:** 98%+ (Full specification match)  
**Timeline:** 4-6 weeks

---

## Quick Summary

**Critical Issues Found:** 7  
**Status:** 4 P0 (Critical), 3 P1 (High Priority)

| Component | Status | Effort |
|-----------|--------|--------|
| WebDavCloudProvider | ❌ Missing | 4-6 hours |
| DefaultAutomationPlugin | ❌ Missing | 3-4 hours |
| Merkle Tree Verification | ⚠️ 30% | 3-4 hours |
| PermissionCapabilities Detection | ⚠️ 50% | 2-3 hours |
| Split APK Handling | ❌ Missing | 4-6 hours |
| Incremental File Scanning | ⚠️ 70% | 2-3 hours |
| rclone Integration | ⚠️ 60% | 6-8 hours |

---

## Phase 1: Critical Compliance (Week 1-2)

### Goal: Resolve all P0 blockers → 95% compliance

---

### 🔴 Task 1: Implement WebDavCloudProvider
**Priority:** P0 - CRITICAL  
**Effort:** 4-6 hours  
**Blocks:** WebDAV cloud sync feature

**Specification Reference:**
- File: `specification.md` line 34
- Interface: `CloudProviderPlugin`

**Implementation Checklist:**
```kotlin
// File: app/src/main/java/com/obsidianbackup/plugins/builtin/WebDavCloudProvider.kt

- [ ] Implement CloudProviderPlugin interface
- [ ] Add WebDAV client dependency (OkHttp + WebDAV library)
- [ ] Implement initialize(config: CloudConfig) with URL/credentials
- [ ] Implement testConnection() - HTTP OPTIONS request
- [ ] Implement uploadSnapshot() - PUT with chunked transfer
- [ ] Implement downloadSnapshot() - GET with resume support
- [ ] Implement listSnapshots() - PROPFIND method
- [ ] Implement deleteSnapshot() - DELETE method
- [ ] Add progress tracking via Flow
- [ ] Handle authentication (Basic Auth, Digest Auth)
- [ ] Add error handling for network failures
- [ ] Write unit tests
```

**Dependencies to Add:**
```kotlin
// build.gradle.kts
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.github.thegrizzlylabs:sardine-android:0.8")
```

**Acceptance Criteria:**
- ✅ Can connect to WebDAV server
- ✅ Can upload/download files
- ✅ Progress tracking functional
- ✅ Error handling robust
- ✅ Tests pass

---

### 🔴 Task 2: Implement DefaultAutomationPlugin
**Priority:** P0 - CRITICAL  
**Effort:** 3-4 hours  
**Blocks:** Default automation workflows

**Specification Reference:**
- File: `specification.md` line 35
- Interface: `AutomationPlugin`

**Implementation Checklist:**
```kotlin
// File: app/src/main/java/com/obsidianbackup/plugins/builtin/DefaultAutomationPlugin.kt

- [ ] Implement AutomationPlugin interface
- [ ] Define default triggers:
      - [ ] TIME_BASED (daily, weekly, monthly)
      - [ ] APP_INSTALL (backup on new app)
      - [ ] APP_UPDATE (backup after update)
      - [ ] BATTERY_LEVEL (backup when charging)
      - [ ] NETWORK_CONNECTED (backup on WiFi)
- [ ] Implement registerTrigger() with WorkManager
- [ ] Implement unregisterTrigger() 
- [ ] Implement getActiveTriggers()
- [ ] Implement executeAction() for:
      - [ ] BACKUP_APPS
      - [ ] SYNC_TO_CLOUD
      - [ ] VERIFY_INTEGRITY
- [ ] Add trigger event broadcasting via Flow
- [ ] Integrate with existing WorkManagerScheduler
- [ ] Write unit tests
```

**Acceptance Criteria:**
- ✅ Default triggers available in UI
- ✅ Can schedule time-based backups
- ✅ Event-based triggers work (app install/update)
- ✅ Actions execute successfully
- ✅ Tests pass

---

### 🔴 Task 3: Implement Merkle Tree Verification
**Priority:** P0 - CRITICAL  
**Effort:** 3-4 hours  
**Blocks:** Cryptographic integrity feature

**Current Status:** Field exists, calculation TODO

**Files to Modify:**
- `app/src/main/java/com/obsidianbackup/cloud/CloudSyncManager.kt:60`
- `app/src/main/java/com/obsidianbackup/verification/` (create)

**Implementation Checklist:**
```kotlin
// File: app/src/main/java/com/obsidianbackup/verification/MerkleTreeVerifier.kt

- [ ] Create MerkleTreeVerifier class
- [ ] Implement buildMerkleTree(files: List<File>): MerkleNode
- [ ] Implement calculateRootHash(): String
- [ ] Implement verifyFile(file: File, proof: List<Hash>): Boolean
- [ ] Add MerkleNode data class (hash, left, right)
- [ ] Use SHA-256 for hashing
- [ ] Optimize for large file sets (streaming)
- [ ] Store Merkle proof in snapshot metadata
- [ ] Integrate with BackupCatalog
- [ ] Update CloudSyncManager to use Merkle root
- [ ] Write unit tests for tree building
- [ ] Write integration tests
```

**Replace TODOs:**
```kotlin
// CloudSyncManager.kt line 60
// BEFORE:
merkleRootHash = "" // TODO

// AFTER:
merkleRootHash = merkleTreeVerifier.calculateRootHash(archiveFile)
```

**Acceptance Criteria:**
- ✅ Merkle tree builds for snapshots
- ✅ Root hash calculated correctly
- ✅ Can verify file integrity with proof
- ✅ Stored in database
- ✅ Tests pass

---

### 🔴 Task 4: Complete PermissionCapabilities Detection
**Priority:** P0 - CRITICAL  
**Effort:** 2-3 hours  
**Blocks:** Core permission system functionality

**Current Status:** TODO placeholder at `PermissionManager.kt:6`

**Implementation Checklist:**
```kotlin
// File: app/src/main/java/com/obsidianbackup/model/PermissionManager.kt

- [ ] Implement detectCurrentMode() - actual runtime detection
- [ ] Add ROOT detection:
      - [ ] Check for su binary
      - [ ] Execute "su -c echo test"
      - [ ] Verify root access
- [ ] Add SHIZUKU detection:
      - [ ] Check Shizuku package installed
      - [ ] Verify Shizuku service running
      - [ ] Test binder connection
- [ ] Add ADB detection:
      - [ ] Check for ADB shell access
      - [ ] Test command execution
- [ ] Fallback to SAF if all fail
- [ ] Implement capability calculation:
      - [ ] ROOT: all capabilities true
      - [ ] SHIZUKU: most capabilities true
      - [ ] ADB: limited capabilities
      - [ ] SAF: minimal capabilities
- [ ] Cache detection results
- [ ] Re-detect on permission changes
- [ ] Update dashboard UI with real status
- [ ] Write unit tests (mocked)
- [ ] Write integration tests (real)
```

**Acceptance Criteria:**
- ✅ Correctly detects ROOT/Shizuku/ADB/SAF
- ✅ Capability flags accurate per mode
- ✅ UI shows real permission status
- ✅ Fallback logic works
- ✅ Tests pass

---

## Phase 2: Feature Completeness (Week 3-4)

### Goal: Implement all documented features → 98% compliance

---

### 🟡 Task 5: Implement Split APK Handling
**Priority:** P1 - HIGH  
**Effort:** 4-6 hours  
**Impact:** Modern app support (70% of Play Store apps use split APKs)

**Specification Reference:**
- File: `highlight.md` line 119 - "Split APK Handling: Automatic reconstruction"

**Implementation Checklist:**
```kotlin
// File: app/src/main/java/com/obsidianbackup/engine/SplitApkHandler.kt

- [ ] Create SplitApkHandler class
- [ ] Implement detectSplitApks(packageName: String): List<ApkInfo>
- [ ] Implement backupSplitApks(packageName: String, outputDir: File)
      - [ ] Detect base.apk
      - [ ] Detect split_config.<config>.apk
      - [ ] Detect split_<feature>.apk
      - [ ] Copy all splits to backup
      - [ ] Generate APKS bundle metadata
- [ ] Implement restoreSplitApks(packageName: String, backupDir: File)
      - [ ] Reconstruct APK bundle
      - [ ] Install via "pm install-create" session
      - [ ] Add all splits to session
      - [ ] Commit session
- [ ] Integrate with ObsidianBoxEngine.backupApk()
- [ ] Integrate with ObsidianBoxEngine.restoreApk()
- [ ] Handle version compatibility
- [ ] Write unit tests
- [ ] Write integration tests with real split APK
```

**Update ObsidianBoxEngine:**
```kotlin
// Modify backupApk() to detect and handle splits
private suspend fun backupApk(appId: AppId, appDir: File): Long {
    val apkHandler = SplitApkHandler()
    return if (apkHandler.isSplitApk(appId)) {
        apkHandler.backupSplitApks(appId, appDir)
    } else {
        // existing single APK logic
    }
}
```

**Acceptance Criteria:**
- ✅ Detects split APKs
- ✅ Backs up all splits
- ✅ Restores split APKs correctly
- ✅ Apps launch after restore
- ✅ Tests pass

---

### 🟡 Task 6: Complete IncrementalBackupStrategy File Scanning
**Priority:** P1 - HIGH  
**Effort:** 2-3 hours  
**Blocks:** Incremental backup feature

**Current Status:** TODO at `IncrementalBackupStrategy.kt:37`

**Implementation Checklist:**
```kotlin
// File: app/src/main/java/com/obsidianbackup/engine/IncrementalBackupStrategy.kt

- [ ] Implement scanForChangedFiles():
      - [ ] Get last backup snapshot
      - [ ] Load file list from previous backup
      - [ ] Walk current app directories
      - [ ] Compare file timestamps
      - [ ] Compare file sizes
      - [ ] Use MD5 for changed file detection (fast)
      - [ ] Return list of changed/new/deleted files
- [ ] Implement calculateChunkHash() for deduplication
- [ ] Cache chunk hashes (LRU already exists)
- [ ] Integrate with rsync --link-dest logic
- [ ] Add progress tracking for scan
- [ ] Handle large directories efficiently (streaming)
- [ ] Write unit tests
- [ ] Write integration tests
```

**Acceptance Criteria:**
- ✅ Detects changed files accurately
- ✅ Incremental backup only transfers changes
- ✅ Deduplication works
- ✅ Performance acceptable (<5s for 1000 files)
- ✅ Tests pass

---

### 🟡 Task 7: Implement rclone Abstraction Layer
**Priority:** P1 - HIGH  
**Effort:** 6-8 hours  
**Impact:** Multi-cloud provider support (as per specification)

**Specification Reference:**
- File: `highlight.md` line 122 - "rclone-based multi-provider support"

**Current Status:** Google Drive API implemented, rclone missing

**Implementation Checklist:**
```kotlin
// File: app/src/main/java/com/obsidianbackup/cloud/RcloneProvider.kt

- [ ] Bundle rclone binary (ARM/ARM64/x86/x86_64)
- [ ] Create RcloneProvider implementing CloudProvider
- [ ] Implement initialize() - setup rclone config
- [ ] Implement testConnection() - rclone config test
- [ ] Implement uploadSnapshot() - rclone copy with progress
- [ ] Implement downloadSnapshot() - rclone copy
- [ ] Implement listSnapshots() - rclone ls
- [ ] Implement deleteSnapshot() - rclone delete
- [ ] Parse rclone output for progress tracking
- [ ] Support multiple backends:
      - [ ] Google Drive (already works via API)
      - [ ] Dropbox
      - [ ] OneDrive
      - [ ] Amazon S3
      - [ ] WebDAV (use WebDavCloudProvider or rclone)
- [ ] Add provider selection UI
- [ ] Write unit tests
- [ ] Write integration tests (mocked)
```

**Binary Setup:**
```bash
# Add rclone binaries to app/src/main/assets/rclone/
assets/
└── rclone/
    ├── rclone-arm
    ├── rclone-arm64
    ├── rclone-x86
    └── rclone-x86_64
```

**Acceptance Criteria:**
- ✅ rclone binary extracts and executes
- ✅ Can configure multiple providers
- ✅ Upload/download works
- ✅ Progress tracking functional
- ✅ Tests pass

---

## Phase 3: Polish & Optimization (Week 5-6)

### Goal: Production quality → 100% compliance

---

### 🟢 Task 8: Consolidate Duplicate PluginManager Classes
**Priority:** P2 - MEDIUM  
**Effort:** 2-3 hours  
**Impact:** Code maintainability

**Current Issue:**
- `/plugins/core/PluginManager.kt` (DI-injectable)
- `/plugins/PluginManager.kt` (Standalone)
- Legacy implementations in merged work

**Action Plan:**
- [ ] Audit all PluginManager usages
- [ ] Choose primary implementation (DI version)
- [ ] Migrate all references
- [ ] Delete duplicate files
- [ ] Update tests
- [ ] Verify no broken imports

---

### 🟢 Task 9: Complete GoogleDriveProvider TODOs
**Priority:** P2 - MEDIUM  
**Effort:** 3-4 hours  

**Files:**
- `GoogleDriveProvider.kt:251` - Progress tracking
- `GoogleDriveProvider.kt:256` - Catalog sync
- `GoogleDriveProvider.kt:261` - Catalog retrieval

**Implementation:**
- [ ] Add real-time progress tracking during upload/download
- [ ] Implement catalog metadata sync to Drive
- [ ] Implement catalog retrieval from Drive
- [ ] Calculate duration and average speed
- [ ] Update UI with real progress

---

### 🟢 Task 10: Implement SQLCipher Database Encryption
**Priority:** P2 - MEDIUM  
**Effort:** 4-5 hours  
**Impact:** Enhanced security (audit recommendation)

**Implementation:**
- [ ] Add SQLCipher dependency
- [ ] Update Room database configuration
- [ ] Generate encryption key from KeyStore
- [ ] Add migration for existing users
- [ ] Implement key rotation
- [ ] Update tests

---

## Testing Strategy

### Unit Tests (Coverage Target: 80%+)

**Phase 1 Tests:**
- [ ] WebDavCloudProviderTest (mock server)
- [ ] DefaultAutomationPluginTest (mock WorkManager)
- [ ] MerkleTreeVerifierTest (tree building, verification)
- [ ] PermissionManagerTest (capability detection)

**Phase 2 Tests:**
- [ ] SplitApkHandlerTest (mock APK files)
- [ ] IncrementalBackupStrategyTest (file scanning)
- [ ] RcloneProviderTest (mock rclone output)

**Phase 3 Tests:**
- [ ] Integration tests for all new features
- [ ] End-to-end backup/restore tests
- [ ] Performance benchmarks

---

## Progress Tracking

### Week 1
- [x] ✅ Specification compliance audit complete
- [ ] Task 1: WebDavCloudProvider (4-6 hours)
- [ ] Task 2: DefaultAutomationPlugin (3-4 hours)

### Week 2
- [ ] Task 3: Merkle Tree Verification (3-4 hours)
- [ ] Task 4: PermissionCapabilities Detection (2-3 hours)
- [ ] Phase 1 Testing (4-6 hours)

### Week 3
- [ ] Task 5: Split APK Handling (4-6 hours)
- [ ] Task 6: Incremental File Scanning (2-3 hours)

### Week 4
- [ ] Task 7: rclone Integration (6-8 hours)
- [ ] Phase 2 Testing (4-6 hours)

### Week 5
- [ ] Task 8: Consolidate Duplicates (2-3 hours)
- [ ] Task 9: GoogleDriveProvider TODOs (3-4 hours)
- [ ] Task 10: SQLCipher Integration (4-5 hours)

### Week 6
- [ ] Phase 3 Testing (6-8 hours)
- [ ] Documentation updates
- [ ] Release preparation

---

## Success Metrics

### Compliance Milestones

| Phase | Target | Metrics |
|-------|--------|---------|
| **Current** | 87% | 4 critical gaps, 3 high priority |
| **Phase 1** | 95% | All P0 resolved |
| **Phase 2** | 98% | All P1 resolved |
| **Phase 3** | 100% | All gaps resolved, production-ready |

### Quality Gates

**Before Phase 2:**
- ✅ All P0 tasks complete
- ✅ Unit tests pass (60%+ coverage)
- ✅ Integration tests pass
- ✅ No critical bugs

**Before Phase 3:**
- ✅ All P0+P1 tasks complete
- ✅ Unit tests pass (70%+ coverage)
- ✅ Performance benchmarks meet targets
- ✅ No high-priority bugs

**Before Release:**
- ✅ All tasks complete
- ✅ Unit tests pass (80%+ coverage)
- ✅ Integration tests pass
- ✅ Security audit pass
- ✅ No open P0/P1 bugs

---

## Resource Requirements

**Developer Time:** 50-60 hours (6 weeks @ 8-10 hours/week)  
**Testing Time:** 15-20 hours  
**Total:** 65-80 hours

**Dependencies:**
- WebDAV library (Sardine)
- rclone binaries
- SQLCipher library

**Skills Required:**
- Kotlin/Android development
- Cloud provider APIs
- Cryptography (Merkle trees)
- Testing (unit + integration)

---

## Risk Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| rclone binary size | APK bloat | Use dynamic delivery or download on demand |
| Split APK compatibility | Restore failures | Test with 20+ split APK apps |
| WebDAV server diversity | Connection issues | Test with NextCloud, ownCloud, generic WebDAV |
| Permission detection edge cases | Wrong mode | Extensive device testing |
| Merkle tree performance | Slow verification | Optimize tree building, use caching |

---

## Conclusion

**This action plan provides a clear roadmap to 100% specification compliance in 4-6 weeks.**

**Current Status:** 87% compliant, production-ready for core features  
**After Phase 1:** 95% compliant, all critical features complete  
**After Phase 2:** 98% compliant, full feature parity with specs  
**After Phase 3:** 100% compliant, production-ready for public release

**Recommendation:** Execute Phase 1 immediately to unblock critical features, then proceed with Phase 2-3 in parallel with beta testing.

---

**Document Version:** 1.0  
**Last Updated:** February 8, 2026  
**Next Review:** End of Phase 1 (Week 2)
