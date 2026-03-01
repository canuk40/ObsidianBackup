#!/usr/bin/env bash
# =============================================================================
# stress_rapid_operations.sh — Rapid-fire operation stress test
# =============================================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"
parse_args "$@"
SUITE_NAME="stress_rapid"

log_header "RAPID OPERATION STRESS TEST — $VARIANT"

launch_app
sleep 3

TASKER_RECEIVER="$PKG/.automation.TaskerIntegration"

# ---------------------------------------------------------------------------
# Rapid start/cancel backup (50 cycles)
# ---------------------------------------------------------------------------
run_test "rapid_backup_cancel" "Start/cancel backup 50 times" '
    logcat_marker "STRESS:rapid_backup_cancel"
    for i in $(seq 1 50); do
        adb shell am broadcast \
            -a "com.obsidianbackup.action.START_BACKUP" \
            -n "$TASKER_RECEIVER" 2>/dev/null
        sleep 0.2
        adb shell am broadcast \
            -a "com.obsidianbackup.action.CANCEL_OPERATION" \
            -n "$TASKER_RECEIVER" 2>/dev/null
        sleep 0.2
    done
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# Rapid screen navigation (100 transitions)
# ---------------------------------------------------------------------------
run_test "rapid_navigation" "Navigate 100 screen transitions" '
    logcat_marker "STRESS:rapid_navigation"
    local routes=("dashboard" "apps" "backups" "settings" "gaming" "health" "plugins" "automation" "logs")
    for i in $(seq 1 100); do
        local idx=$((RANDOM % ${#routes[@]}))
        local route="${routes[$idx]}"
        adb shell am start -n "$PKG/$MAIN_ACTIVITY" \
            --es "navigate_to" "$route" >/dev/null 2>&1
        sleep 0.1
    done
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# Rapid deep links (50 fires)
# ---------------------------------------------------------------------------
run_test "rapid_deep_links" "Fire 50 deep links rapidly" '
    logcat_marker "STRESS:rapid_deep_links"
    local links=("obsidianbackup://backup" "obsidianbackup://settings" "obsidianbackup://restore?id=test")
    for i in $(seq 1 50); do
        local idx=$((RANDOM % ${#links[@]}))
        fire_deep_link "${links[$idx]}" &
    done
    wait
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# Rapid Tasker broadcasts (100 in parallel)
# ---------------------------------------------------------------------------
run_test "rapid_tasker_broadcasts" "Fire 100 Tasker broadcasts in parallel" '
    logcat_marker "STRESS:rapid_tasker"
    for i in $(seq 1 100); do
        adb shell am broadcast \
            -a "com.obsidianbackup.action.QUERY_STATUS" \
            -n "$TASKER_RECEIVER" 2>/dev/null &
    done
    wait
    sleep 5
    is_app_running
'

# ---------------------------------------------------------------------------
# Rapid settings toggles (100 toggles)
# ---------------------------------------------------------------------------
run_test "rapid_settings_toggle" "Toggle settings 100 times" '
    logcat_marker "STRESS:rapid_settings"
    for i in $(seq 1 100); do
        local val=$((i % 2))
        local bool_val="false"
        [[ $val -eq 1 ]] && bool_val="true"
        adb shell am broadcast \
            -a "com.obsidianbackup.action.SET_PREFERENCE" \
            -n "$TASKER_RECEIVER" \
            --es "pref_key" "debug_mode_enabled" \
            --ez "pref_value" $bool_val 2>/dev/null &
    done
    wait
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# Rapid app launch/kill (20 cycles)
# ---------------------------------------------------------------------------
run_test "rapid_launch_kill" "Launch/kill app 20 times" '
    logcat_marker "STRESS:rapid_launch_kill"
    for i in $(seq 1 20); do
        launch_app
        sleep 0.5
        force_stop_app
        sleep 0.5
    done
    launch_app
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# Concurrent backup requests
# ---------------------------------------------------------------------------
run_test "concurrent_backups" "Multiple concurrent backup requests" '
    logcat_marker "STRESS:concurrent_backups"
    for type in apps contacts sms call_log wifi wallpaper; do
        adb shell am broadcast \
            -a "com.obsidianbackup.action.START_BACKUP" \
            -n "$TASKER_RECEIVER" \
            --es "backup_type" "$type" 2>/dev/null &
    done
    wait
    sleep 10
    is_app_running
'

# ---------------------------------------------------------------------------
# Memory check after rapid stress
# ---------------------------------------------------------------------------
run_test "post_rapid_memory" "Memory state after rapid stress" '
    capture_meminfo "stress_post_rapid"
    is_app_running
'

print_summary
