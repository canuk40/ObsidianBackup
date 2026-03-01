#!/usr/bin/env bash
# =============================================================================
# stress_large_backup.sh — Large dataset stress test
# =============================================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"
parse_args "$@"
SUITE_NAME="stress_large"

log_header "LARGE BACKUP STRESS TEST — $VARIANT"

launch_app
sleep 3

TASKER_RECEIVER="$PKG/.automation.TaskerIntegration"

# ---------------------------------------------------------------------------
# Seed large test data
# ---------------------------------------------------------------------------
run_test "seed_large_data" "Create large test files on device" '
    logcat_marker "STRESS:seed_large"
    # 100MB file
    adb shell "dd if=/dev/urandom of=/sdcard/ObsidianBackup_Test/large_files/stress_100mb.bin \
        bs=1048576 count=100" 2>/dev/null
    # 500MB file
    adb shell "dd if=/dev/urandom of=/sdcard/ObsidianBackup_Test/large_files/stress_500mb.bin \
        bs=1048576 count=500" 2>/dev/null
    adb shell "ls -la /sdcard/ObsidianBackup_Test/large_files/" 2>/dev/null
    true
'

# ---------------------------------------------------------------------------
# Memory baseline
# ---------------------------------------------------------------------------
run_test "memory_baseline" "Capture memory baseline" '
    capture_meminfo "stress_baseline"
    true
'

# ---------------------------------------------------------------------------
# Large file backup — 100MB
# ---------------------------------------------------------------------------
run_test "backup_100mb" "Backup 100MB file" '
    logcat_marker "STRESS:backup_100mb"
    start_timer
    adb shell am broadcast \
        -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$TASKER_RECEIVER" \
        --es "backup_type" "media" 2>/dev/null
    sleep 30
    local elapsed=$(stop_timer)
    log_info "100MB backup time: ${elapsed}ms"
    capture_meminfo "stress_100mb"
    is_app_running
'

# ---------------------------------------------------------------------------
# Large file backup — 500MB
# ---------------------------------------------------------------------------
run_test "backup_500mb" "Backup 500MB file" '
    logcat_marker "STRESS:backup_500mb"
    start_timer
    adb shell am broadcast \
        -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$TASKER_RECEIVER" \
        --es "backup_type" "media" 2>/dev/null
    sleep 60
    local elapsed=$(stop_timer)
    log_info "500MB backup time: ${elapsed}ms"
    capture_meminfo "stress_500mb"
    is_app_running
'

# ---------------------------------------------------------------------------
# Many apps backup
# ---------------------------------------------------------------------------
run_test "backup_many_apps" "Backup 50+ apps simultaneously" '
    logcat_marker "STRESS:many_apps"
    start_timer
    adb shell am broadcast \
        -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$TASKER_RECEIVER" \
        --es "backup_type" "all_parallel" \
        --ei "max_apps" 50 2>/dev/null
    # Wait for operation — large backup
    sleep 120
    local elapsed=$(stop_timer)
    log_info "50-app backup time: ${elapsed}ms"
    capture_meminfo "stress_many_apps"
    is_app_running
'

# ---------------------------------------------------------------------------
# Compression overhead measurement
# ---------------------------------------------------------------------------
run_test "compression_overhead" "Measure compression overhead on large file" '
    logcat_marker "STRESS:compression_overhead"
    # Enable compression
    adb shell am broadcast \
        -a "com.obsidianbackup.action.SET_PREFERENCE" \
        -n "$TASKER_RECEIVER" \
        --es "pref_key" "compression_enabled" \
        --ez "pref_value" true 2>/dev/null || true
    sleep 1
    start_timer
    adb shell am broadcast \
        -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$TASKER_RECEIVER" \
        --es "backup_type" "media" 2>/dev/null
    sleep 30
    local elapsed=$(stop_timer)
    log_info "Compressed backup time: ${elapsed}ms"
    is_app_running
'

# ---------------------------------------------------------------------------
# Encryption overhead measurement
# ---------------------------------------------------------------------------
run_test "encryption_overhead" "Measure encryption overhead on large file" '
    logcat_marker "STRESS:encryption_overhead"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.SET_PREFERENCE" \
        -n "$TASKER_RECEIVER" \
        --es "pref_key" "encryption_enabled" \
        --ez "pref_value" true 2>/dev/null || true
    sleep 1
    start_timer
    adb shell am broadcast \
        -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$TASKER_RECEIVER" \
        --es "backup_type" "media" 2>/dev/null
    sleep 30
    local elapsed=$(stop_timer)
    log_info "Encrypted backup time: ${elapsed}ms"
    is_app_running
'

# ---------------------------------------------------------------------------
# Memory after all stress operations
# ---------------------------------------------------------------------------
run_test "memory_post_stress" "Memory state after large backup stress" '
    capture_meminfo "stress_post_large"
    # Check for memory pressure
    local mem_info
    mem_info=$(adb shell dumpsys meminfo "$PKG" 2>/dev/null | grep "TOTAL" | head -1 || true)
    log_info "Post-stress memory: $mem_info"
    is_app_running
'

# ---------------------------------------------------------------------------
# Cleanup large files
# ---------------------------------------------------------------------------
run_test "cleanup_large" "Remove large test files" '
    adb shell "rm -f /sdcard/ObsidianBackup_Test/large_files/stress_*.bin" 2>/dev/null
    true
'

print_summary
