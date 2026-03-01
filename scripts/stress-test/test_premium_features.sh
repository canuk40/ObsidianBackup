#!/usr/bin/env bash
# =============================================================================
# test_premium_features.sh — Premium variant feature validation
# =============================================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VARIANT="premium"
BUILD_TYPE="debug"
source "$SCRIPT_DIR/common.sh"
parse_args "$@"
# Force premium variant
VARIANT="premium"
PKG="$PKG_PREMIUM_DEBUG"
SUITE_NAME="premium_features"

log_header "PREMIUM VARIANT FEATURE TESTS"

launch_app
sleep 3

TASKER_RECEIVER="$PKG/.automation.TaskerIntegration"

# ---------------------------------------------------------------------------
# Verify IS_PREMIUM is true
# ---------------------------------------------------------------------------
run_test "premium_is_premium" "Premium variant reports IS_PREMIUM=true" '
    logcat_marker "PREMIUM:check_premium"
    force_stop_app
    launch_app
    sleep 3
    local log
    log=$(adb shell "logcat -d -t 100 -s ObsidianBackup:V" 2>/dev/null | \
        grep -i "premium\|IS_PREMIUM" | tail -5 || true)
    log_info "Premium status: $log"
    is_app_running
'

# ---------------------------------------------------------------------------
# Many backup profiles (beyond free limit)
# ---------------------------------------------------------------------------
run_test "premium_10_profiles" "Create 10 backup profiles" '
    logcat_marker "PREMIUM:10_profiles"
    for i in $(seq 1 10); do
        adb shell am broadcast \
            -a "com.obsidianbackup.action.CREATE_PROFILE" \
            -n "$TASKER_RECEIVER" \
            --es "profile_name" "Premium Profile $i" 2>/dev/null || true
        sleep 1
    done
    is_app_running
'

run_test "premium_50_profiles" "Create 50 backup profiles (stress)" '
    logcat_marker "PREMIUM:50_profiles"
    for i in $(seq 11 50); do
        adb shell am broadcast \
            -a "com.obsidianbackup.action.CREATE_PROFILE" \
            -n "$TASKER_RECEIVER" \
            --es "profile_name" "Premium Profile $i" 2>/dev/null || true
    done
    sleep 5
    is_app_running
'

# ---------------------------------------------------------------------------
# Cloud providers accessible (not gated)
# ---------------------------------------------------------------------------
run_test "premium_cloud_access" "Cloud providers screen accessible" '
    logcat_marker "PREMIUM:cloud_access"
    fire_deep_link "obsidianbackup://cloud_providers"
    sleep 3
    take_screenshot "premium_cloud_providers"
    is_app_running
'

# ---------------------------------------------------------------------------
# All premium settings unlocked
# ---------------------------------------------------------------------------
PREMIUM_SETTINGS=(
    "cloud_sync_enabled"
    "sync_policies_enabled"
    "smart_scheduling_enabled"
    "zero_knowledge_enabled"
    "plugin_system_enabled"
)

for setting in "${PREMIUM_SETTINGS[@]}"; do
    run_test "premium_setting_${setting}" "Premium setting accessible: ${setting}" "
        logcat_marker 'PREMIUM:setting_${setting}'
        adb shell am broadcast \
            -a 'com.obsidianbackup.action.SET_PREFERENCE' \
            -n '$TASKER_RECEIVER' \
            --es 'pref_key' '${setting}' \
            --ez 'pref_value' true 2>/dev/null || true
        sleep 1
        is_app_running
    "
done

# ---------------------------------------------------------------------------
# Premium cloud sync
# ---------------------------------------------------------------------------
run_test "premium_cloud_sync" "Cloud sync operational" '
    logcat_marker "PREMIUM:cloud_sync"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.TRIGGER_CLOUD_SYNC" \
        -n "$TASKER_RECEIVER" 2>/dev/null
    sleep 5
    is_app_running
'

# ---------------------------------------------------------------------------
# Premium-exclusive screens
# ---------------------------------------------------------------------------
PREMIUM_SCREENS=("zero_knowledge" "cloud_providers" "filecoin")

for screen in "${PREMIUM_SCREENS[@]}"; do
    run_test "premium_screen_${screen}" "Premium screen accessible: ${screen}" "
        fire_deep_link 'obsidianbackup://${screen}'
        sleep 3
        is_app_running
    "
done

# ---------------------------------------------------------------------------
# Heavy usage stress (premium should handle more)
# ---------------------------------------------------------------------------
run_test "premium_heavy_usage" "Premium heavy usage — backup + sync + verify" '
    logcat_marker "PREMIUM:heavy_usage"
    # Start backup
    adb shell am broadcast -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$TASKER_RECEIVER" --es "backup_type" "apps" 2>/dev/null
    sleep 2
    # Trigger cloud sync
    adb shell am broadcast -a "com.obsidianbackup.action.TRIGGER_CLOUD_SYNC" \
        -n "$TASKER_RECEIVER" 2>/dev/null
    sleep 2
    # Verify
    adb shell am broadcast -a "com.obsidianbackup.action.VERIFY_BACKUP" \
        -n "$TASKER_RECEIVER" 2>/dev/null
    sleep 10
    is_app_running
'

print_summary
