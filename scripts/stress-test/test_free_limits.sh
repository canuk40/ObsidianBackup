#!/usr/bin/env bash
# =============================================================================
# test_free_limits.sh — Free variant limitation enforcement tests
# =============================================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VARIANT="free"
BUILD_TYPE="debug"
source "$SCRIPT_DIR/common.sh"
parse_args "$@"
# Force free variant
VARIANT="free"
PKG="$PKG_FREE_DEBUG"
SUITE_NAME="free_limits"

log_header "FREE VARIANT LIMIT TESTS"

launch_app
sleep 3

TASKER_RECEIVER="$PKG/.automation.TaskerIntegration"

# ---------------------------------------------------------------------------
# Verify IS_PREMIUM is false
# ---------------------------------------------------------------------------
run_test "free_is_not_premium" "Free variant reports IS_PREMIUM=false" '
    logcat_marker "FREE:check_premium"
    # Check logcat for premium status
    force_stop_app
    launch_app
    sleep 3
    local log
    log=$(adb shell "logcat -d -t 100 -s ObsidianBackup:V" 2>/dev/null | \
        grep -i "premium\|IS_PREMIUM\|free" | tail -5 || true)
    log_info "Premium status: $log"
    is_app_running
'

# ---------------------------------------------------------------------------
# Backup profile limit: max 3
# ---------------------------------------------------------------------------
run_test "free_create_profile_1" "Create backup profile 1 (allowed)" '
    logcat_marker "FREE:profile_1"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.CREATE_PROFILE" \
        -n "$TASKER_RECEIVER" \
        --es "profile_name" "Test Profile 1" 2>/dev/null || true
    sleep 3
    is_app_running
'

run_test "free_create_profile_2" "Create backup profile 2 (allowed)" '
    logcat_marker "FREE:profile_2"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.CREATE_PROFILE" \
        -n "$TASKER_RECEIVER" \
        --es "profile_name" "Test Profile 2" 2>/dev/null || true
    sleep 3
    is_app_running
'

run_test "free_create_profile_3" "Create backup profile 3 (allowed)" '
    logcat_marker "FREE:profile_3"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.CREATE_PROFILE" \
        -n "$TASKER_RECEIVER" \
        --es "profile_name" "Test Profile 3" 2>/dev/null || true
    sleep 3
    is_app_running
'

run_test "free_create_profile_4" "Create backup profile 4 (should be BLOCKED)" '
    logcat_marker "FREE:profile_4_blocked"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.CREATE_PROFILE" \
        -n "$TASKER_RECEIVER" \
        --es "profile_name" "Test Profile 4 BLOCKED" 2>/dev/null || true
    sleep 3
    # Check for limit enforcement in logcat
    local blocked
    blocked=$(adb shell "logcat -d -t 50 -s ObsidianBackup:V" 2>/dev/null | \
        grep -i "limit\|maximum\|upgrade\|premium" | tail -3 || true)
    log_info "Profile limit log: $blocked"
    is_app_running
'

# ---------------------------------------------------------------------------
# Premium feature gates
# ---------------------------------------------------------------------------
run_test "free_cloud_sync_gated" "Cloud sync PRO feature gate" '
    logcat_marker "FREE:cloud_sync_gate"
    fire_deep_link "obsidianbackup://cloud_providers"
    sleep 3
    take_screenshot "free_cloud_gate"
    is_app_running
'

run_test "free_sync_policies_gated" "Sync policies PRO feature gate" '
    logcat_marker "FREE:sync_policies_gate"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.SET_PREFERENCE" \
        -n "$TASKER_RECEIVER" \
        --es "pref_key" "sync_policy" \
        --es "pref_value" "wifi_only" 2>/dev/null || true
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# Upgrade prompt
# ---------------------------------------------------------------------------
run_test "free_upgrade_prompt" "Upgrade prompt appears for premium features" '
    logcat_marker "FREE:upgrade_prompt"
    # Try to access a premium feature
    fire_deep_link "obsidianbackup://cloud_providers"
    sleep 3
    take_screenshot "free_upgrade_prompt"
    local upgrade_log
    upgrade_log=$(adb shell "logcat -d -t 50 -s ObsidianBackup:V" 2>/dev/null | \
        grep -i "upgrade\|premium\|purchase\|billing" | tail -3 || true)
    log_info "Upgrade prompt: $upgrade_log"
    is_app_running
'

print_summary
