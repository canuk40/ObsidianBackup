#!/usr/bin/env bash
# =============================================================================
# test_navigation.sh — Launch every screen/route via ADB intents, verify no crash
# =============================================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"
parse_args "$@"
SUITE_NAME="navigation"

log_header "NAVIGATION TESTS — $VARIANT"

# Ensure app is installed and stopped
force_stop_app

# ---------------------------------------------------------------------------
# Helper: launch main activity with a deep-link-style extra to navigate
# ---------------------------------------------------------------------------
navigate_to() {
    local route="$1"
    # Use the app's deep link scheme to reach specific screens
    adb shell am start -n "$PKG/$MAIN_ACTIVITY" \
        --es "navigate_to" "$route" >/dev/null 2>&1
    sleep 3
    # Verify app is still alive (no crash)
    is_app_running
}

navigate_via_deeplink() {
    local uri="$1"
    fire_deep_link "$uri"
    sleep 2
    is_app_running
}

# ---------------------------------------------------------------------------
# Core screens (bottom nav / drawer)
# ---------------------------------------------------------------------------
CORE_ROUTES=("dashboard" "apps" "backups" "automation" "logs" "settings")

for route in "${CORE_ROUTES[@]}"; do
    run_test "nav_${route}" "Navigate to ${route} screen" "
        logcat_marker 'NAV_START:${route}'
        force_stop_app
        launch_app
        sleep 2
        navigate_to '${route}'
    "
done

# ---------------------------------------------------------------------------
# Feature screens
# ---------------------------------------------------------------------------
FEATURE_ROUTES=(
    "gaming"
    "health"
    "plugins"
    "community"
    "feature_flags"
    "zero_knowledge"
    "cloud_providers"
    "filecoin"
    "feedback"
    "changelog"
    "tips"
    "onboarding"
)

for route in "${FEATURE_ROUTES[@]}"; do
    run_test "nav_${route}" "Navigate to ${route} screen" "
        logcat_marker 'NAV_START:${route}'
        force_stop_app
        launch_app
        sleep 2
        navigate_to '${route}'
    "
done

# ---------------------------------------------------------------------------
# Cold launch test — fresh start
# ---------------------------------------------------------------------------
run_test "cold_launch" "Cold launch from cleared state" '
    force_stop_app
    launch_app
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# Rapid navigation — switch screens quickly
# ---------------------------------------------------------------------------
run_test "rapid_nav" "Rapidly navigate through all screens" '
    launch_app
    sleep 2
    for route in dashboard apps backups settings gaming health plugins automation logs; do
        navigate_to "$route"
        sleep 0.5
    done
    is_app_running
'

# ---------------------------------------------------------------------------
# Back button handling
# ---------------------------------------------------------------------------
run_test "back_button" "Back button does not crash from sub-screens" '
    launch_app
    sleep 2
    navigate_to "settings"
    sleep 1
    adb shell input keyevent KEYCODE_BACK
    sleep 1
    adb shell input keyevent KEYCODE_BACK
    sleep 1
    is_app_running
'

# ---------------------------------------------------------------------------
# Home and recents
# ---------------------------------------------------------------------------
run_test "home_return" "App survives home button and return" '
    launch_app
    sleep 2
    adb shell input keyevent KEYCODE_HOME
    sleep 2
    adb shell am start -n "$PKG/$MAIN_ACTIVITY" >/dev/null 2>&1
    sleep 2
    is_app_running
'

run_test "recents_return" "App survives recents and return" '
    launch_app
    sleep 2
    adb shell input keyevent KEYCODE_APP_SWITCH
    sleep 2
    adb shell input keyevent KEYCODE_APP_SWITCH
    sleep 2
    is_app_running
'

print_summary
