# Test Case Template - ObsidianBackup

Use this template for every test case. Copy the template below and fill in the details.

---

## TEST CASE TEMPLATE

### Test Case: [TC-XXX] [Feature Name]

**Feature ID:** #[Feature Number]  
**Assigned To:** [QA Engineer Name]  
**Priority:** [P0/P1/P2]  
**Estimated Duration:** [15-120] minutes  
**Test Type:** [Unit/Integration/UI/E2E/Performance/Security]  
**Created Date:** [YYYY-MM-DD]  
**Last Updated:** [YYYY-MM-DD]  

---

## PREREQUISITES & ENVIRONMENT

### Required Devices
```
Primary Device: [Model] - [Android Version] - [RAM/Storage]
Secondary Devices: [List additional test devices]
Unsupported Devices: [Devices that should NOT run this test]
```

### Required Permissions
- [ ] Storage access (READ_EXTERNAL_STORAGE, READ_MEDIA_IMAGES)
- [ ] Camera (if applicable)
- [ ] Contacts (if applicable)
- [ ] Calendar (if applicable)
- [ ] [Other permissions]

### Required Setup
- [ ] App installed (v[version]+ required)
- [ ] Device authenticated (account setup: _____)
- [ ] Cloud provider configured ([Provider]: _____)
- [ ] Free storage available: _____ GB
- [ ] Network connectivity: [WiFi/Mobile/Both]
- [ ] Battery level: [>80% / >50% / >20%]

### Test Data Required
```json
{
  "backup_size": "XXX MB",
  "file_count": "XXXX",
  "network_type": "WiFi 6E / 5G / 4G LTE",
  "device_ram": "X GB",
  "device_storage": "XXX GB free"
}
```

---

## TEST EXECUTION

### Test Steps

#### Step 1: [Initial Setup]
**Action:** [What to do]
```
1. Navigate to [Screen/Feature]
2. Tap/Click on [Element]
3. Select [Option]
```

**Expected Result:**
- Screen should display [X]
- No crashes or ANR (Application Not Responding)
- UI is responsive

**Actual Result:**
- [ ] Pass
- [ ] Fail (Reason: _______)

---

#### Step 2: [Primary Action]
**Action:** [What to do]
```
1. Perform action [X]
2. Verify result [Y]
3. Assert condition [Z]
```

**Expected Result:**
- [Specific outcome]
- No error messages
- Status shows as "[Expected Status]"

**Actual Result:**
- [ ] Pass
- [ ] Fail (Reason: _______)

---

#### Step 3: [Verification]
**Action:** [What to verify]
```
1. Check [Element]
2. Validate [Data]
3. Assert [Condition]
```

**Expected Result:**
- Data matches expected format
- No corruption detected
- Timestamps are accurate

**Actual Result:**
- [ ] Pass
- [ ] Fail (Reason: _______)

---

### Performance Metrics (Measure During Test)
```
Start Time:           [HH:MM:SS]
End Time:             [HH:MM:SS]
Duration:             [XX minutes]
Memory Usage (Start): [XXX MB]
Memory Usage (Peak):  [XXX MB]
CPU Usage:            [X%]
Battery Drain:        [X%]
Network Data:         [XX MB]
```

---

## ACCEPTANCE CRITERIA

All of the following MUST be true for test to PASS:

- [ ] Feature completes within timeout (Duration < [XX] minutes)
- [ ] No crashes or Force Close (FC)
- [ ] No ANR (Application Not Responding) errors
- [ ] No unhandled exceptions in logcat
- [ ] Data integrity verified (no corruption)
- [ ] Appropriate feedback displayed to user
- [ ] UI is responsive (no freezes >500ms)
- [ ] Analytics events logged correctly
- [ ] No memory leaks (memory returns to baseline)
- [ ] Network requests use encryption
- [ ] Data stored securely (not in cleartext)
- [ ] Appropriate error messages shown on failure

---

## PASS/FAIL CRITERIA

### ✅ Test PASSES if:
- All acceptance criteria met
- No exceptions thrown
- Feature functions as designed
- Performance within targets
- No new defects introduced

### ❌ Test FAILS if:
- Crash or Force Close occurs
- Data corruption detected
- Timeout exceeded (Duration > [XX] minutes)
- UI freezes (>500ms without response)
- Unexpected error message shown
- Feature does not work as designed
- Critical performance degradation
- Security vulnerability found

---

## DEVICE-SPECIFIC NOTES

### Pixel 6 Pro (Android 15, 12GB RAM)
**Expected:** [Specific outcome for this device]
**Notes:** [Device-specific behavior]
**Duration:** ~[X] minutes
**Status:** [ ] Tested [ ] Skipped

### Samsung S23 (Android 14, 8GB RAM)
**Expected:** [Specific outcome for this device]
**Notes:** [Device-specific behavior]
**Duration:** ~[X] minutes
**Status:** [ ] Tested [ ] Skipped

### Moto G54 (Android 13, 6GB RAM)
**Expected:** [Specific outcome for this device]
**Notes:** Slower device; may take longer
**Duration:** ~[XX] minutes
**Status:** [ ] Tested [ ] Skipped

### Tablet (Android 13, 4GB RAM)
**Expected:** [Specific outcome for this device]
**Notes:** Multi-pane layout
**Status:** [ ] Tested [ ] Skipped

---

## REGRESSION RISK ASSESSMENT

**Risk Level:** [LOW / MEDIUM / HIGH / CRITICAL]

**Changed Components:**
- [ ] Backup engine
- [ ] Cloud provider integration
- [ ] Authentication system
- [ ] UI framework
- [ ] Network layer
- [ ] Encryption/Security
- [ ] Database layer
- [ ] Other: [Specify]

**Affected Features (Potential Regression):**
1. [Feature A] - [Risk: Low/Medium/High]
2. [Feature B] - [Risk: Low/Medium/High]
3. [Feature C] - [Risk: Low/Medium/High]

**Mitigation:**
- [ ] Run existing regression tests
- [ ] Test dependent features
- [ ] Verify previous releases still work
- [ ] Check for edge cases

---

## TEST AUTOMATION STATUS

### Automation Feasibility: [HIGH / MEDIUM / LOW]

**Automated By:** [Espresso / Robolectric / JUnit / Firebase Test Lab / Manual]

**Automation Code Location:**
```
Path: [src/test/...] or [src/androidTest/...]
Test Class: [ClassName]
Test Method: [methodName]
```

**Automation Status:**
- [ ] Not Automated (manual only)
- [ ] Partially Automated ([X%] coverage)
- [ ] Fully Automated (100% coverage)

**Dependencies for Automation:**
- [ ] Mock servers / WireMock
- [ ] Test data fixtures
- [ ] Device-specific setup
- [ ] Network throttling

---

## BUGS & DEFECTS FOUND

### Bug #1
**ID:** [BUG-XXXX]  
**Severity:** [Critical/High/Medium/Low]  
**Title:** [Brief description]  

```
Reproduction Steps:
1. ...
2. ...
Expected: ...
Actual: ...
```

**Attachment:** [Screenshot / Video / Logcat]  
**Status:** [New / Assigned / In Review / Fixed / Verified]

---

### Bug #2
[Repeat above format]

---

## SIGN-OFF

**Tested By:** [Name] - [Date]  
**Verified By:** [QA Lead] - [Date]  
**Approved By:** [Engineering Lead] - [Date]  

### Test Results Summary
- **Total Steps:** [X]
- **Passed:** [X]
- **Failed:** [X]
- **Skipped:** [X]
- **Overall:** [✅ PASS / ⚠️ CONDITIONAL / ❌ FAIL]

### Comments
```
[Any additional notes, concerns, or observations]
```

---

## ATTACHMENTS

### Screenshots
- [ ] Screenshot 1: [Description]
- [ ] Screenshot 2: [Description]
- [ ] Screenshot 3: [Description]

### Video Recording
- [ ] Video 1: [Test execution] - [Duration: XX minutes]
- [ ] Video 2: [Bug reproduction] - [Duration: XX minutes]

### Logs
- [ ] Logcat dump: [From HH:MM:SS to HH:MM:SS]
- [ ] Analytics log: [Attached]
- [ ] Network trace (tcpdump): [Attached]

### Performance Data
- [ ] Battery Historian plot: [Attached]
- [ ] Perfetto trace: [Attached]
- [ ] Gradle Profiler report: [Attached]

---

## RELATED TEST CASES

**Depends On:**
- [ ] TC-[XXX] - [Feature Name]
- [ ] TC-[YYY] - [Feature Name]

**Related Tests:**
- [ ] TC-[ZZZ] - [Similar feature]
- [ ] TC-[AAA] - [Dependent feature]

**Blocks:**
- [ ] TC-[BBB] - [Cannot test until this passes]

---

## TEST CASE METADATA

| Property | Value |
|----------|-------|
| Created By | [Name] |
| Review Status | [New / In Review / Approved] |
| Automation Priority | [Critical / High / Medium / Low] |
| Device Coverage | [% of supported devices] |
| Last Executed | [YYYY-MM-DD] |
| Next Review | [YYYY-MM-DD] |
| Documentation | [Link to spec] |
| Related Issues | [GitHub Issues #123, #456] |

---

## CHECKLIST FOR TEST COMPLETION

- [ ] All steps executed
- [ ] Acceptance criteria verified
- [ ] Device-specific notes documented
- [ ] Screenshots/videos attached
- [ ] Bugs reported with reproduction steps
- [ ] Regression testing completed
- [ ] Performance metrics recorded
- [ ] Logcat cleaned and attached (if applicable)
- [ ] Test result submitted
- [ ] Stakeholders notified

---

**Template Version:** 1.0  
**Last Updated:** 2024  
**Used By:** ObsidianBackup QA Team

---

## EXAMPLE TEST CASE (Filled Out)

### Test Case: [TC-001] Incremental Backup with Compression

**Feature ID:** #2  
**Assigned To:** John Smith  
**Priority:** P0  
**Estimated Duration:** 30 minutes  
**Test Type:** Integration  
**Created Date:** 2024-01-15  

#### Prerequisites
- Device: Pixel 6 Pro, Android 15, 12GB RAM
- Free storage: 2GB
- Network: WiFi 6E (100+ Mbps)
- App: v1.0.0+ installed and authenticated
- Cloud provider: Google Drive configured
- Test data: 500MB test folder with 150 files

#### Test Steps
**Step 1: Initial Setup**
```
1. Open ObsidianBackup app
2. Navigate to "Backup" > "New Backup"
3. Select "Google Drive" as destination
4. Select "Incremental Backup" mode
5. Select "ZSTD Compression"
```
Expected: Settings screen displayed, no crashes
Actual: ✅ PASS

**Step 2: Create Full Backup**
```
1. Select 500MB test folder
2. Tap "Start Backup"
3. Wait for completion
4. Monitor progress bar
```
Expected: Backup completes in <10 minutes, shows 42-45% compression
Actual: ✅ PASS (Completed in 8m 32s, 43% compression)

**Step 3: Verify Incremental**
```
1. Modify 50MB of files
2. Create new incremental backup
3. Verify only ~50MB transferred
```
Expected: Only 50MB uploaded (not full 500MB)
Actual: ✅ PASS (51MB transferred)

#### Results
- **Duration:** 28 minutes (Target: 30 min) ✅
- **Memory Peak:** 287MB (Target: <300MB) ✅
- **Pass Rate:** 3/3 steps ✅
- **Overall:** ✅ PASS

