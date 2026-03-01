#!/usr/bin/env bash
# =============================================================================
# test_edge_cases.sh — Edge condition and boundary tests
# =============================================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"
parse_args "$@"
SUITE_NAME="edge_cases"

log_header "EDGE CASE TESTS — $VARIANT"

launch_app
sleep 3

TASKER_RECEIVER="$PKG/.automation.TaskerIntegration"

# ---------------------------------------------------------------------------
# Empty backup (no apps selected)
# ---------------------------------------------------------------------------
run_test "empty_backup" "Backup with no apps selected" '
    logcat_marker "EDGE:empty_backup"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$TASKER_RECEIVER" \
        --es "backup_type" "none" 2>/dev/null
    sleep 5
    is_app_running
'

# ---------------------------------------------------------------------------
# Restore nonexistent snapshot
# ---------------------------------------------------------------------------
run_test "restore_nonexistent" "Restore nonexistent snapshot ID" '
    logcat_marker "EDGE:restore_nonexistent"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.RESTORE_SNAPSHOT" \
        -n "$TASKER_RECEIVER" \
        --es "snapshot_id" "nonexistent_snapshot_999999" 2>/dev/null
    sleep 5
    is_app_running
'

# ---------------------------------------------------------------------------
# Delete nonexistent snapshot
# ---------------------------------------------------------------------------
run_test "delete_nonexistent" "Delete nonexistent snapshot ID" '
    logcat_marker "EDGE:delete_nonexistent"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.DELETE_SNAPSHOT" \
        -n "$TASKER_RECEIVER" \
        --es "snapshot_id" "" 2>/dev/null
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# Verify nonexistent backup
# ---------------------------------------------------------------------------
run_test "verify_nonexistent" "Verify nonexistent backup" '
    logcat_marker "EDGE:verify_nonexistent"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.VERIFY_BACKUP" \
        -n "$TASKER_RECEIVER" \
        --es "snapshot_id" "does_not_exist" 2>/dev/null
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# Unicode and special character handling
# ---------------------------------------------------------------------------
run_test "unicode_input" "Handle unicode characters in input" '
    logcat_marker "EDGE:unicode"
    # Try backup with unicode app name
    adb shell am broadcast \
        -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$TASKER_RECEIVER" \
        --es "app_name" "テスト アプリ 🎮" 2>/dev/null || true
    sleep 3
    is_app_running
'

run_test "long_string_input" "Handle extremely long string input" '
    logcat_marker "EDGE:long_string"
    local long_str
    long_str=$(python3 -c "print(\"A\" * 10000)" 2>/dev/null || printf "%10000s" | tr " " "A")
    adb shell am broadcast \
        -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$TASKER_RECEIVER" \
        --es "app_name" "$long_str" 2>/dev/null || true
    sleep 3
    is_app_running
'

run_test "special_chars_input" "Handle special characters in path" '
    logcat_marker "EDGE:special_chars"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$TASKER_RECEIVER" \
        --es "backup_path" "/sdcard/test dir/file (1) [copy].bak" 2>/dev/null || true
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# Invalid broadcast extras
# ---------------------------------------------------------------------------
run_test "missing_extras" "Broadcast with missing required extras" '
    logcat_marker "EDGE:missing_extras"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.RESTORE_SNAPSHOT" \
        -n "$TASKER_RECEIVER" 2>/dev/null
    sleep 3
    is_app_running
'

run_test "wrong_type_extras" "Broadcast with wrong type extras" '
    logcat_marker "EDGE:wrong_type"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$TASKER_RECEIVER" \
        --ei "backup_type" 42 2>/dev/null || true
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# Permission denial scenarios
# ---------------------------------------------------------------------------
run_test "revoke_storage_perm" "App handles revoked storage permission" '
    logcat_marker "EDGE:revoke_perm"
    adb shell pm revoke "$PKG" android.permission.READ_EXTERNAL_STORAGE 2>/dev/null || true
    sleep 1
    adb shell am broadcast \
        -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$TASKER_RECEIVER" \
        --es "backup_type" "media" 2>/dev/null
    sleep 3
    # Re-grant
    adb shell pm grant "$PKG" android.permission.READ_EXTERNAL_STORAGE 2>/dev/null || true
    is_app_running
'

# ---------------------------------------------------------------------------
# Multiple instances
# ---------------------------------------------------------------------------
run_test "double_launch" "Double launch via intent" '
    logcat_marker "EDGE:double_launch"
    adb shell am start -n "$PKG/$MAIN_ACTIVITY" >/dev/null 2>&1 &
    adb shell am start -n "$PKG/$MAIN_ACTIVITY" >/dev/null 2>&1 &
    wait
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# App with no data directory (fresh install)
# ---------------------------------------------------------------------------
run_test "fresh_install_launch" "Launch after clearing all data" '
    logcat_marker "EDGE:fresh_install"
    clear_app_data
    sleep 1
    launch_app
    sleep 4
    is_app_running
'

# ---------------------------------------------------------------------------
# Null/empty intent data
# ---------------------------------------------------------------------------
run_test "empty_intent" "Fire intent with no data" '
    logcat_marker "EDGE:empty_intent"
    adb shell am start -a android.intent.action.VIEW -n "$PKG/$MAIN_ACTIVITY" >/dev/null 2>&1
    sleep 3
    is_app_running
'

print_summary
