#!/usr/bin/env bash
# =============================================================================
# test_automation.sh — Exercise Tasker integration and WorkManager scheduling
# =============================================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"
parse_args "$@"
SUITE_NAME="automation"

log_header "AUTOMATION & SCHEDULING TESTS — $VARIANT"

launch_app
sleep 3

TASKER_RECEIVER="$PKG/.automation.TaskerIntegration"

# ---------------------------------------------------------------------------
# Tasker broadcast actions
# ---------------------------------------------------------------------------
log_header "Tasker Broadcast Actions"

run_test "tasker_start_backup" "Tasker: ACTION_START_BACKUP" '
    logcat_marker "TASKER:START_BACKUP"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$TASKER_RECEIVER" 2>/dev/null
    sleep 5
    is_app_running
'

run_test "tasker_restore_snapshot" "Tasker: ACTION_RESTORE_SNAPSHOT" '
    logcat_marker "TASKER:RESTORE_SNAPSHOT"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.RESTORE_SNAPSHOT" \
        -n "$TASKER_RECEIVER" \
        --es "snapshot_id" "test_snapshot" 2>/dev/null
    sleep 5
    is_app_running
'

run_test "tasker_query_status" "Tasker: ACTION_QUERY_STATUS" '
    logcat_marker "TASKER:QUERY_STATUS"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.QUERY_STATUS" \
        -n "$TASKER_RECEIVER" 2>/dev/null
    sleep 3
    is_app_running
'

run_test "tasker_trigger_cloud_sync" "Tasker: ACTION_TRIGGER_CLOUD_SYNC" '
    logcat_marker "TASKER:CLOUD_SYNC"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.TRIGGER_CLOUD_SYNC" \
        -n "$TASKER_RECEIVER" 2>/dev/null
    sleep 5
    is_app_running
'

run_test "tasker_verify_backup" "Tasker: ACTION_VERIFY_BACKUP" '
    logcat_marker "TASKER:VERIFY"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.VERIFY_BACKUP" \
        -n "$TASKER_RECEIVER" 2>/dev/null
    sleep 5
    is_app_running
'

run_test "tasker_cancel_operation" "Tasker: ACTION_CANCEL_OPERATION" '
    logcat_marker "TASKER:CANCEL"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.CANCEL_OPERATION" \
        -n "$TASKER_RECEIVER" 2>/dev/null
    sleep 3
    is_app_running
'

run_test "tasker_delete_snapshot" "Tasker: ACTION_DELETE_SNAPSHOT" '
    logcat_marker "TASKER:DELETE"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.DELETE_SNAPSHOT" \
        -n "$TASKER_RECEIVER" \
        --es "snapshot_id" "nonexistent_snapshot" 2>/dev/null
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# Tasker sequence: backup then query status
# ---------------------------------------------------------------------------
run_test "tasker_backup_then_status" "Tasker: Start backup, then query status" '
    logcat_marker "TASKER:SEQUENCE"
    adb shell am broadcast -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$TASKER_RECEIVER" 2>/dev/null
    sleep 3
    adb shell am broadcast -a "com.obsidianbackup.action.QUERY_STATUS" \
        -n "$TASKER_RECEIVER" 2>/dev/null
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# Tasker: backup then cancel
# ---------------------------------------------------------------------------
run_test "tasker_backup_then_cancel" "Tasker: Start backup, then cancel" '
    logcat_marker "TASKER:BACKUP_CANCEL"
    adb shell am broadcast -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$TASKER_RECEIVER" 2>/dev/null
    sleep 2
    adb shell am broadcast -a "com.obsidianbackup.action.CANCEL_OPERATION" \
        -n "$TASKER_RECEIVER" 2>/dev/null
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# WorkManager — list and trigger scheduled work
# ---------------------------------------------------------------------------
log_header "WorkManager Scheduling"

run_test "workmanager_list" "List scheduled WorkManager jobs" '
    logcat_marker "WORKMANAGER:LIST"
    local output
    output=$(adb shell dumpsys jobscheduler 2>/dev/null | grep -i "obsidianbackup" | head -10)
    log_info "Scheduled jobs: $output"
    true
'

# ---------------------------------------------------------------------------
# Boot receiver
# ---------------------------------------------------------------------------
log_header "Boot & System Events"

run_test "boot_receiver" "Simulate BOOT_COMPLETED broadcast" '
    logcat_marker "BOOT:COMPLETED"
    adb shell am broadcast \
        -a android.intent.action.BOOT_COMPLETED \
        -n "$PKG/.receivers.BootReceiver" 2>/dev/null || \
    adb shell am broadcast \
        -a android.intent.action.BOOT_COMPLETED \
        -p "$PKG" 2>/dev/null || true
    sleep 3
    true
'

# ---------------------------------------------------------------------------
# Rapid Tasker broadcasts
# ---------------------------------------------------------------------------
run_test "tasker_rapid_fire" "Rapid-fire Tasker broadcasts (20x)" '
    logcat_marker "TASKER:RAPID"
    for i in $(seq 1 20); do
        adb shell am broadcast -a "com.obsidianbackup.action.QUERY_STATUS" \
            -n "$TASKER_RECEIVER" 2>/dev/null &
    done
    wait
    sleep 5
    is_app_running
'

print_summary
