# ObsidianBackup Stress Test — Results Template

> Fill in this template after running the stress test suite.
> Run date: _______________
> Tester: _______________

---

## Environment

| Property | Value |
|----------|-------|
| Device/Emulator | |
| Android Version | |
| API Level | |
| ABI | |
| Magisk Version | |
| SELinux Status | |
| RAM | |
| Free Disk | |
| Free APK Version | |
| Premium APK Version | |

---

## Automated Test Results Summary

| Suite | Pass | Fail | Skip | Crashes | ANRs | Status |
|-------|------|------|------|---------|------|--------|
| setup | | | | | | |
| navigation | | | | | | |
| backup_engines | | | | | | |
| settings | | | | | | |
| deep_links | | | | | | |
| automation | | | | | | |
| cloud_providers | | | | | | |
| root_features | | | | | | |
| security | | | | | | |
| plugins | | | | | | |
| widgets | | | | | | |
| stress_large | | | | | | |
| stress_rapid | | | | | | |
| stress_resources | | | | | | |
| stress_interruptions | | | | | | |
| edge_cases | | | | | | |
| free_limits | | | | | | |
| premium_features | | | | | | |
| **TOTAL** | | | | | | |

---

## Manual Test Results

### Onboarding
- [ ] All onboarding pages render
- [ ] Onboarding completes successfully
- [ ] Onboarding does not repeat

### Biometric Authentication
- [ ] Enable biometric lock
- [ ] Authenticate with biometric
- [ ] Fallback to PIN/password
- [ ] Disable biometric lock

### Cloud Provider OAuth
- [ ] Google Drive OAuth flow
- [ ] Dropbox OAuth flow
- [ ] OAuth cancellation handling

### Gaming Backup UI
- [ ] Emulator detection
- [ ] Save file listing
- [ ] Gaming backup execution

### Health Connect
- [ ] Permission grant flow
- [ ] Health data backup
- [ ] Privacy anonymization toggle

### Accessibility
- [ ] TalkBack navigation
- [ ] Screen reader announcements

### Theme & Display
- [ ] Dark mode rendering
- [ ] Large font scaling

---

## Performance Metrics

| Metric | Value |
|--------|-------|
| 100MB backup time | ms |
| 500MB backup time | ms |
| 50-app backup time | ms |
| Compression overhead | ms |
| Encryption overhead | ms |
| Peak memory (baseline) | KB |
| Peak memory (under load) | KB |
| Memory delta | KB |

---

## Crash Log Excerpts

### Crash 1
```
(paste FATAL EXCEPTION from logcat here)
```

### Crash 2
```
(paste if applicable)
```

---

## ANR Log Excerpts

### ANR 1
```
(paste ANR trace here)
```

---

## Failed Test Details

| Test ID | Suite | Description | Error | Screenshot |
|---------|-------|-------------|-------|------------|
| | | | | |

---

## Recommendations

1.
2.
3.

---

## Artifacts

- `results/YYYYMMDD_HHMMSS/logcat/logcat_full.log` — Full logcat
- `results/YYYYMMDD_HHMMSS/logcat/crashes_live.log` — Crash-only log
- `results/YYYYMMDD_HHMMSS/screenshots/` — Failure screenshots
- `results/YYYYMMDD_HHMMSS/results.csv` — Machine-readable results
- `results/YYYYMMDD_HHMMSS/device_info.txt` — Device details
- `results/YYYYMMDD_HHMMSS/FINAL_REPORT.txt` — Summary report
