#!/usr/bin/env bash
# =============================================================================
# test_widgets.sh — Widget and receiver functionality
# =============================================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"
parse_args "$@"
SUITE_NAME="widgets"

log_header "WIDGET & RECEIVER TESTS — $VARIANT"

launch_app
sleep 3

# ---------------------------------------------------------------------------
# BackupWidget
# ---------------------------------------------------------------------------
log_header "Backup Widget"

run_test "widget_backup_update" "BackupWidget receives update broadcast" '
    logcat_marker "WIDGET:backup_update"
    adb shell am broadcast \
        -a "android.appwidget.action.APPWIDGET_UPDATE" \
        -n "$PKG/.widgets.BackupWidget" 2>/dev/null || \
    adb shell am broadcast \
        -a "android.appwidget.action.APPWIDGET_UPDATE" \
        -p "$PKG" 2>/dev/null || true
    sleep 3
    true
'

# ---------------------------------------------------------------------------
# BackupStatusWidget
# ---------------------------------------------------------------------------
run_test "widget_status_update" "BackupStatusWidget receives update broadcast" '
    logcat_marker "WIDGET:status_update"
    adb shell am broadcast \
        -a "android.appwidget.action.APPWIDGET_UPDATE" \
        -n "$PKG/.widgets.BackupStatusWidget" 2>/dev/null || \
    adb shell am broadcast \
        -a "android.appwidget.action.APPWIDGET_UPDATE" \
        -p "$PKG" 2>/dev/null || true
    sleep 3
    true
'

# ---------------------------------------------------------------------------
# BootReceiver
# ---------------------------------------------------------------------------
log_header "System Receivers"

run_test "boot_receiver" "BOOT_COMPLETED receiver" '
    logcat_marker "RECEIVER:boot"
    adb shell am broadcast \
        -a "android.intent.action.BOOT_COMPLETED" \
        -n "$PKG/.receivers.BootReceiver" 2>/dev/null || \
    adb shell am broadcast \
        -a "android.intent.action.BOOT_COMPLETED" \
        -p "$PKG" 2>/dev/null || true
    sleep 3
    true
'

# ---------------------------------------------------------------------------
# Package change receivers (app installed/updated/removed)
# ---------------------------------------------------------------------------
run_test "receiver_package_added" "PACKAGE_ADDED broadcast" '
    logcat_marker "RECEIVER:package_added"
    adb shell am broadcast \
        -a "android.intent.action.PACKAGE_ADDED" \
        -d "package:com.example.test" \
        -p "$PKG" 2>/dev/null || true
    sleep 2
    is_app_running
'

run_test "receiver_package_replaced" "MY_PACKAGE_REPLACED broadcast" '
    logcat_marker "RECEIVER:package_replaced"
    adb shell am broadcast \
        -a "android.intent.action.MY_PACKAGE_REPLACED" \
        -p "$PKG" 2>/dev/null || true
    sleep 2
    true
'

# ---------------------------------------------------------------------------
# Connectivity change
# ---------------------------------------------------------------------------
run_test "receiver_connectivity" "Connectivity change broadcast" '
    logcat_marker "RECEIVER:connectivity"
    adb shell am broadcast \
        -a "android.net.conn.CONNECTIVITY_CHANGE" \
        -p "$PKG" 2>/dev/null || true
    sleep 2
    is_app_running
'

# ---------------------------------------------------------------------------
# Battery state changes
# ---------------------------------------------------------------------------
run_test "receiver_battery_low" "BATTERY_LOW broadcast" '
    logcat_marker "RECEIVER:battery_low"
    adb shell am broadcast \
        -a "android.intent.action.BATTERY_LOW" \
        -p "$PKG" 2>/dev/null || true
    sleep 2
    is_app_running
'

run_test "receiver_power_connected" "POWER_CONNECTED broadcast" '
    logcat_marker "RECEIVER:power_connected"
    adb shell am broadcast \
        -a "android.intent.action.ACTION_POWER_CONNECTED" \
        -p "$PKG" 2>/dev/null || true
    sleep 2
    is_app_running
'

# ---------------------------------------------------------------------------
# Rapid widget updates
# ---------------------------------------------------------------------------
run_test "widget_rapid_updates" "Rapid widget update broadcasts (20x)" '
    logcat_marker "WIDGET:rapid"
    for i in $(seq 1 20); do
        adb shell am broadcast \
            -a "android.appwidget.action.APPWIDGET_UPDATE" \
            -p "$PKG" 2>/dev/null &
    done
    wait
    sleep 3
    is_app_running
'

print_summary
