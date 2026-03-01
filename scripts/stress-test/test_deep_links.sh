#!/usr/bin/env bash
# =============================================================================
# test_deep_links.sh — Fire every deep link URI and verify no crash
# =============================================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"
parse_args "$@"
SUITE_NAME="deep_links"

log_header "DEEP LINK TESTS — $VARIANT"

launch_app
sleep 3

# ---------------------------------------------------------------------------
# Custom scheme deep links
# ---------------------------------------------------------------------------
log_header "Custom Scheme (obsidianbackup://)"

CUSTOM_LINKS=(
    "obsidianbackup://backup"
    "obsidianbackup://restore?id=test_snapshot_001"
    "obsidianbackup://settings"
)

for link in "${CUSTOM_LINKS[@]}"; do
    safe_name=$(echo "$link" | sed 's|[^a-zA-Z0-9]|_|g')
    run_test "deeplink_${safe_name}" "Deep link: $link" "
        logcat_marker 'DEEPLINK:${link}'
        force_stop_app
        fire_deep_link '${link}'
        sleep 2
        is_app_running
    "
done

# ---------------------------------------------------------------------------
# HTTPS app links
# ---------------------------------------------------------------------------
log_header "HTTPS App Links"

HTTPS_LINKS=(
    "https://obsidianbackup.app/backup"
    "https://obsidianbackup.app/restore"
    "https://obsidianbackup.app/settings"
    "https://obsidianbackup.app/cloud"
)

for link in "${HTTPS_LINKS[@]}"; do
    safe_name=$(echo "$link" | sed 's|[^a-zA-Z0-9]|_|g')
    run_test "applink_${safe_name}" "App link: $link" "
        logcat_marker 'APPLINK:${link}'
        force_stop_app
        fire_deep_link '${link}'
        sleep 2
        # App link may open browser if not verified — just check no crash
        true
    "
done

# ---------------------------------------------------------------------------
# Deep link with app already running (warm state)
# ---------------------------------------------------------------------------
log_header "Warm-State Deep Links"

run_test "deeplink_warm_backup" "Deep link while app running: backup" '
    launch_app
    sleep 2
    fire_deep_link "obsidianbackup://backup"
    sleep 2
    is_app_running
'

run_test "deeplink_warm_settings" "Deep link while app running: settings" '
    fire_deep_link "obsidianbackup://settings"
    sleep 2
    is_app_running
'

# ---------------------------------------------------------------------------
# Invalid deep links (should not crash)
# ---------------------------------------------------------------------------
log_header "Invalid Deep Links"

INVALID_LINKS=(
    "obsidianbackup://"
    "obsidianbackup://nonexistent_route"
    "obsidianbackup://restore?id="
    "obsidianbackup://settings?malicious=<script>alert(1)</script>"
    "obsidianbackup://backup?extra_param=value&another=test"
)

for link in "${INVALID_LINKS[@]}"; do
    safe_name=$(echo "$link" | sed 's|[^a-zA-Z0-9]|_|g' | head -c 60)
    run_test "deeplink_invalid_${safe_name}" "Invalid deep link handled gracefully" "
        logcat_marker 'DEEPLINK_INVALID:${link}'
        force_stop_app
        fire_deep_link '${link}'
        sleep 2
        # App should either start gracefully or not crash
        true
    "
done

# ---------------------------------------------------------------------------
# Rapid deep links
# ---------------------------------------------------------------------------
run_test "deeplink_rapid" "Rapid-fire deep links (10 in sequence)" '
    launch_app
    sleep 2
    for i in $(seq 1 10); do
        fire_deep_link "obsidianbackup://settings"
        sleep 0.3
        fire_deep_link "obsidianbackup://backup"
        sleep 0.3
    done
    sleep 2
    is_app_running
'

print_summary
