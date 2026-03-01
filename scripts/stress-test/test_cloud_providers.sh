#!/usr/bin/env bash
# =============================================================================
# test_cloud_providers.sh — Test cloud provider initialization and error handling
# =============================================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"
parse_args "$@"
SUITE_NAME="cloud_providers"

log_header "CLOUD PROVIDER TESTS — $VARIANT"

launch_app
sleep 3

# ---------------------------------------------------------------------------
# Navigate to cloud providers screen
# ---------------------------------------------------------------------------
run_test "cloud_screen_open" "Open cloud providers screen" '
    fire_deep_link "obsidianbackup://cloud_providers"
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# Test each provider initialization (most will fail auth — that is expected)
# We are testing that the app handles failures gracefully without crashing
# ---------------------------------------------------------------------------
log_header "Provider Initialization (Graceful Failure Expected)"

PROVIDERS=(
    "google_drive"
    "dropbox"
    "onedrive"
    "webdav"
    "ftp"
    "sftp"
    "smb"
    "s3"
    "backblaze_b2"
    "pcloud"
    "mega"
    "box"
    "oracle_cloud"
    "alibaba_oss"
    "digitalocean_spaces"
    "syncthing"
)

for provider in "${PROVIDERS[@]}"; do
    run_test "cloud_init_${provider}" "Initialize provider: ${provider}" "
        logcat_marker 'CLOUD_INIT:${provider}'
        # Attempt to trigger provider setup via intent
        adb shell am broadcast \
            -a 'com.obsidianbackup.action.INIT_CLOUD_PROVIDER' \
            -n '$TASKER_RECEIVER' \
            --es 'provider' '${provider}' 2>/dev/null || true
        sleep 3
        is_app_running
    "
done

TASKER_RECEIVER="$PKG/.automation.TaskerIntegration"

# ---------------------------------------------------------------------------
# Filecoin / IPFS
# ---------------------------------------------------------------------------
log_header "Filecoin / Decentralized Storage"

run_test "filecoin_screen" "Open Filecoin screen" '
    fire_deep_link "obsidianbackup://filecoin"
    sleep 3
    is_app_running
'

run_test "filecoin_init" "Initialize Filecoin provider" '
    logcat_marker "CLOUD_INIT:filecoin"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.INIT_CLOUD_PROVIDER" \
        -n "$TASKER_RECEIVER" \
        --es "provider" "filecoin" 2>/dev/null || true
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# Cloud sync trigger (no configured provider — should handle gracefully)
# ---------------------------------------------------------------------------
log_header "Cloud Sync Operations"

run_test "cloud_sync_no_provider" "Cloud sync with no configured provider" '
    logcat_marker "CLOUD_SYNC:no_provider"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.TRIGGER_CLOUD_SYNC" \
        -n "$TASKER_RECEIVER" 2>/dev/null || true
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# Network state changes during cloud operations
# ---------------------------------------------------------------------------
run_test "cloud_airplane_mode" "Cloud provider with airplane mode" '
    logcat_marker "CLOUD:airplane_mode"
    # Enable airplane mode
    adb shell settings put global airplane_mode_on 1 2>/dev/null || true
    adb shell am broadcast -a android.intent.action.AIRPLANE_MODE 2>/dev/null || true
    sleep 2
    # Try cloud sync
    adb shell am broadcast \
        -a "com.obsidianbackup.action.TRIGGER_CLOUD_SYNC" \
        -n "$TASKER_RECEIVER" 2>/dev/null || true
    sleep 3
    # Disable airplane mode
    adb shell settings put global airplane_mode_on 0 2>/dev/null || true
    adb shell am broadcast -a android.intent.action.AIRPLANE_MODE 2>/dev/null || true
    sleep 2
    is_app_running
'

# ---------------------------------------------------------------------------
# WebDAV with invalid URL (error handling)
# ---------------------------------------------------------------------------
run_test "webdav_invalid_url" "WebDAV with invalid server URL" '
    logcat_marker "CLOUD:webdav_invalid"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.INIT_CLOUD_PROVIDER" \
        -n "$TASKER_RECEIVER" \
        --es "provider" "webdav" \
        --es "url" "https://invalid.nonexistent.server/dav" \
        --es "username" "test" \
        --es "password" "test" 2>/dev/null || true
    sleep 5
    is_app_running
'

print_summary
