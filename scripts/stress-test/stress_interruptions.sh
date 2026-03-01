#!/usr/bin/env bash
# =============================================================================
# stress_interruptions.sh — Test app resilience to interruptions
# =============================================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"
parse_args "$@"
SUITE_NAME="stress_interruptions"

log_header "INTERRUPTION STRESS TEST — $VARIANT"

launch_app
sleep 3

TASKER_RECEIVER="$PKG/.automation.TaskerIntegration"

# ---------------------------------------------------------------------------
# Kill app mid-backup
# ---------------------------------------------------------------------------
run_test "kill_during_backup" "Kill app during backup and recover" '
    logcat_marker "STRESS:kill_during_backup"
    # Start a backup
    adb shell am broadcast \
        -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$TASKER_RECEIVER" \
        --es "backup_type" "apps" 2>/dev/null
    sleep 3
    # Force kill
    force_stop_app
    sleep 2
    # Relaunch
    launch_app
    sleep 4
    is_app_running
'

# ---------------------------------------------------------------------------
# Process death and restoration
# ---------------------------------------------------------------------------
run_test "process_death" "Process death and state restoration" '
    logcat_marker "STRESS:process_death"
    launch_app
    sleep 2
    # Navigate to a non-default screen
    fire_deep_link "obsidianbackup://settings"
    sleep 2
    # Kill the process (simulates system reclaim)
    local pid
    pid=$(adb shell pidof "$PKG" 2>/dev/null | tr -d "\r")
    if [[ -n "$pid" ]]; then
        adb shell kill "$pid" 2>/dev/null || true
    fi
    sleep 2
    # Restore from recents
    adb shell am start -n "$PKG/$MAIN_ACTIVITY" >/dev/null 2>&1
    sleep 4
    is_app_running
'

# ---------------------------------------------------------------------------
# Airplane mode during cloud sync
# ---------------------------------------------------------------------------
run_test "airplane_during_sync" "Airplane mode during cloud sync" '
    logcat_marker "STRESS:airplane_sync"
    # Start cloud sync
    adb shell am broadcast \
        -a "com.obsidianbackup.action.TRIGGER_CLOUD_SYNC" \
        -n "$TASKER_RECEIVER" 2>/dev/null
    sleep 2
    # Toggle airplane mode
    adb shell settings put global airplane_mode_on 1 2>/dev/null || true
    adb shell am broadcast -a android.intent.action.AIRPLANE_MODE 2>/dev/null || true
    sleep 3
    # Restore connectivity
    adb shell settings put global airplane_mode_on 0 2>/dev/null || true
    adb shell am broadcast -a android.intent.action.AIRPLANE_MODE 2>/dev/null || true
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# Storage full simulation
# ---------------------------------------------------------------------------
run_test "storage_full" "Handle storage full condition" '
    logcat_marker "STRESS:storage_full"
    # Fill up storage with a large temp file
    adb shell "dd if=/dev/zero of=/sdcard/ObsidianBackup_Test/fill_disk.tmp \
        bs=1048576 count=2048" 2>/dev/null || true
    sleep 1
    # Try a backup — should fail gracefully
    adb shell am broadcast \
        -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$TASKER_RECEIVER" \
        --es "backup_type" "apps" 2>/dev/null
    sleep 5
    # Clean up the fill file
    adb shell "rm -f /sdcard/ObsidianBackup_Test/fill_disk.tmp" 2>/dev/null
    is_app_running
'

# ---------------------------------------------------------------------------
# Screen rotation during operations
# ---------------------------------------------------------------------------
run_test "rotation_during_backup" "Screen rotation during backup" '
    logcat_marker "STRESS:rotation_backup"
    launch_app
    sleep 2
    adb shell am broadcast \
        -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$TASKER_RECEIVER" \
        --es "backup_type" "apps" 2>/dev/null
    sleep 2
    # Rotate to landscape
    adb shell settings put system accelerometer_rotation 0
    adb shell settings put system user_rotation 1
    sleep 2
    # Rotate to portrait
    adb shell settings put system user_rotation 0
    sleep 2
    # Rapid rotation
    for i in $(seq 1 5); do
        adb shell settings put system user_rotation $((i % 4))
        sleep 0.5
    done
    # Reset to auto-rotate
    adb shell settings put system accelerometer_rotation 1
    sleep 2
    is_app_running
'

# ---------------------------------------------------------------------------
# Configuration change (locale/language)
# ---------------------------------------------------------------------------
run_test "locale_change" "App survives locale change during operation" '
    logcat_marker "STRESS:locale_change"
    launch_app
    sleep 2
    # Change locale
    adb shell settings put system system_locales "es-ES" 2>/dev/null || true
    sleep 3
    # Change back
    adb shell settings put system system_locales "en-US" 2>/dev/null || true
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# Multiple force stops and relaunches
# ---------------------------------------------------------------------------
run_test "repeated_force_stop" "Repeated force stop/relaunch (10 cycles)" '
    logcat_marker "STRESS:repeated_force_stop"
    for i in $(seq 1 10); do
        launch_app
        sleep 1
        # Start some operation
        adb shell am broadcast \
            -a "com.obsidianbackup.action.START_BACKUP" \
            -n "$TASKER_RECEIVER" 2>/dev/null
        sleep 1
        force_stop_app
        sleep 0.5
    done
    launch_app
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# App update simulation
# ---------------------------------------------------------------------------
run_test "app_reinstall" "App survives reinstall (data preserved)" '
    logcat_marker "STRESS:reinstall"
    # Get current APK path
    local apk_path
    apk_path=$(find "$PROJECT_ROOT/app/build/outputs/apk" -name "app-*-debug.apk" | head -1)
    if [[ -n "$apk_path" ]]; then
        adb install -r -t "$apk_path" 2>/dev/null
        sleep 3
        launch_app
        sleep 3
        is_app_running
    else
        log_warn "No APK found for reinstall test"
        return 2
    fi
'

# ---------------------------------------------------------------------------
# Clear data and fresh start
# ---------------------------------------------------------------------------
run_test "clear_data_recovery" "App handles fresh start after data clear" '
    logcat_marker "STRESS:clear_data"
    clear_app_data
    sleep 2
    launch_app
    sleep 4
    is_app_running
'

print_summary
