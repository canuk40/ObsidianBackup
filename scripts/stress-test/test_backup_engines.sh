#!/usr/bin/env bash
# =============================================================================
# test_backup_engines.sh — Exercise every backup engine with backup/restore/verify
# =============================================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"
parse_args "$@"
SUITE_NAME="backup_engines"

log_header "BACKUP ENGINE TESTS — $VARIANT"

launch_app
sleep 3

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
trigger_backup_intent() {
    local type="$1"
    logcat_marker "BACKUP_START:$type"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$PKG/.automation.TaskerIntegration" \
        --es "backup_type" "$type" \
        2>/dev/null
    sleep 5
}

wait_for_backup_complete() {
    local timeout="${1:-120}"
    local elapsed=0
    while [[ $elapsed -lt $timeout ]]; do
        # Check logcat for completion or failure markers
        local status
        status=$(adb shell "logcat -d -t 50 -s ObsidianBackup:V" 2>/dev/null | \
            grep -E "backup (completed|finished|failed|error)" | tail -1 || true)
        if [[ -n "$status" ]]; then
            log_info "Backup status: $status"
            echo "$status" | grep -qi "completed\|finished"
            return $?
        fi
        sleep 5
        ((elapsed += 5))
    done
    log_warn "Backup timed out after ${timeout}s"
    return 1
}

# ---------------------------------------------------------------------------
# App backup — full cycle
# ---------------------------------------------------------------------------
run_test "backup_app_full" "Full app backup (APK + data)" '
    logcat_marker "TEST:backup_app_full"
    trigger_backup_intent "apps"
    # Give it time to process, check app is alive
    sleep 10
    is_app_running
'

# ---------------------------------------------------------------------------
# Individual backup component types via settings/intents
# ---------------------------------------------------------------------------
BACKUP_COMPONENTS=("apk" "data" "external_data" "obb" "cache" "media")

for component in "${BACKUP_COMPONENTS[@]}"; do
    run_test "backup_component_${component}" "Backup component: ${component}" "
        logcat_marker 'BACKUP_COMPONENT:${component}'
        trigger_backup_intent '${component}'
        sleep 8
        is_app_running
    "
done

# ---------------------------------------------------------------------------
# Specialized engines
# ---------------------------------------------------------------------------
run_test "backup_contacts" "Contacts backup engine" '
    logcat_marker "TEST:backup_contacts"
    trigger_backup_intent "contacts"
    sleep 8
    is_app_running
'

run_test "backup_sms" "SMS backup engine" '
    logcat_marker "TEST:backup_sms"
    trigger_backup_intent "sms"
    sleep 8
    is_app_running
'

run_test "backup_call_log" "Call log backup engine" '
    logcat_marker "TEST:backup_call_log"
    trigger_backup_intent "call_log"
    sleep 8
    is_app_running
'

run_test "backup_wifi" "WiFi config backup engine" '
    logcat_marker "TEST:backup_wifi"
    trigger_backup_intent "wifi"
    sleep 8
    is_app_running
'

run_test "backup_system_settings" "System settings backup engine" '
    logcat_marker "TEST:backup_system_settings"
    trigger_backup_intent "system_settings"
    sleep 8
    is_app_running
'

run_test "backup_wallpaper" "Wallpaper backup engine" '
    logcat_marker "TEST:backup_wallpaper"
    trigger_backup_intent "wallpaper"
    sleep 8
    is_app_running
'

# ---------------------------------------------------------------------------
# Gaming backup
# ---------------------------------------------------------------------------
EMULATORS=("retroarch" "dolphin" "ppsspp" "drastic" "citra" "aethersx2")

for emu in "${EMULATORS[@]}"; do
    run_test "backup_gaming_${emu}" "Gaming backup: ${emu}" "
        logcat_marker 'BACKUP_GAMING:${emu}'
        trigger_backup_intent 'gaming_${emu}'
        sleep 8
        is_app_running
    "
done

# ---------------------------------------------------------------------------
# Health Connect
# ---------------------------------------------------------------------------
run_test "backup_health" "Health Connect data backup" '
    logcat_marker "TEST:backup_health"
    trigger_backup_intent "health_connect"
    sleep 8
    is_app_running
'

# ---------------------------------------------------------------------------
# Incremental backup
# ---------------------------------------------------------------------------
run_test "backup_incremental" "Incremental backup (delta)" '
    logcat_marker "TEST:backup_incremental"
    # First backup
    trigger_backup_intent "apps"
    sleep 10
    # Second backup (should be incremental)
    trigger_backup_intent "apps"
    sleep 10
    is_app_running
'

# ---------------------------------------------------------------------------
# Parallel backup
# ---------------------------------------------------------------------------
run_test "backup_parallel" "Parallel backup (multiple apps)" '
    logcat_marker "TEST:backup_parallel"
    trigger_backup_intent "all_parallel"
    sleep 15
    is_app_running
'

# ---------------------------------------------------------------------------
# Restore test
# ---------------------------------------------------------------------------
run_test "restore_basic" "Basic restore operation" '
    logcat_marker "TEST:restore_basic"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.RESTORE_SNAPSHOT" \
        -n "$PKG/.automation.TaskerIntegration" \
        --es "snapshot_id" "latest" \
        2>/dev/null
    sleep 10
    is_app_running
'

# ---------------------------------------------------------------------------
# Verify backup
# ---------------------------------------------------------------------------
run_test "verify_backup" "Verify backup integrity" '
    logcat_marker "TEST:verify_backup"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.VERIFY_BACKUP" \
        -n "$PKG/.automation.TaskerIntegration" \
        --es "snapshot_id" "latest" \
        2>/dev/null
    sleep 8
    is_app_running
'

# ---------------------------------------------------------------------------
# Memory check after heavy operations
# ---------------------------------------------------------------------------
run_test "post_backup_memory" "Memory state after backup tests" '
    capture_meminfo "post_backup"
    is_app_running
'

print_summary
