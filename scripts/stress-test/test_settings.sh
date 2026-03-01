#!/usr/bin/env bash
# =============================================================================
# test_settings.sh — Toggle every settings item, verify no crash
# =============================================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"
parse_args "$@"
SUITE_NAME="settings"

log_header "SETTINGS TESTS — $VARIANT"

launch_app
sleep 3

# ---------------------------------------------------------------------------
# Helper: toggle a SharedPreferences boolean via ADB
# ---------------------------------------------------------------------------
toggle_pref() {
    local key="$1"
    local value="$2"
    adb_root_shell "am broadcast \
        -a com.obsidianbackup.action.SET_PREFERENCE \
        -n '$PKG/.automation.TaskerIntegration' \
        --es 'pref_key' '$key' \
        --ez 'pref_value' $value" 2>/dev/null || true
    sleep 1
}

# Navigate to settings
navigate_settings() {
    fire_deep_link "obsidianbackup://settings"
    sleep 2
}

# ---------------------------------------------------------------------------
# Backup & Restore settings
# ---------------------------------------------------------------------------
log_header "Backup & Restore Settings"

run_test "setting_auto_backup_on" "Enable auto backup" '
    navigate_settings
    toggle_pref "auto_backup_enabled" true
    is_app_running
'

run_test "setting_auto_backup_off" "Disable auto backup" '
    toggle_pref "auto_backup_enabled" false
    is_app_running
'

run_test "setting_compression_on" "Enable compression" '
    toggle_pref "compression_enabled" true
    is_app_running
'

run_test "setting_compression_off" "Disable compression" '
    toggle_pref "compression_enabled" false
    is_app_running
'

run_test "setting_verification_on" "Enable verification" '
    toggle_pref "verification_enabled" true
    is_app_running
'

run_test "setting_verification_off" "Disable verification" '
    toggle_pref "verification_enabled" false
    is_app_running
'

# ---------------------------------------------------------------------------
# Encryption settings
# ---------------------------------------------------------------------------
log_header "Encryption Settings"

run_test "setting_encryption_on" "Enable standard encryption" '
    toggle_pref "encryption_enabled" true
    is_app_running
'

run_test "setting_encryption_off" "Disable standard encryption" '
    toggle_pref "encryption_enabled" false
    is_app_running
'

run_test "setting_zk_screen" "Open zero-knowledge encryption screen" '
    fire_deep_link "obsidianbackup://zero_knowledge"
    sleep 2
    is_app_running
'

# ---------------------------------------------------------------------------
# Cloud & Sync settings
# ---------------------------------------------------------------------------
log_header "Cloud & Sync Settings"

run_test "setting_cloud_sync_on" "Enable cloud sync" '
    toggle_pref "cloud_sync_enabled" true
    is_app_running
'

run_test "setting_cloud_sync_off" "Disable cloud sync" '
    toggle_pref "cloud_sync_enabled" false
    is_app_running
'

run_test "setting_cloud_providers" "Open cloud providers screen" '
    fire_deep_link "obsidianbackup://cloud_providers"
    sleep 2
    is_app_running
'

run_test "setting_filecoin" "Open Filecoin/IPFS screen" '
    fire_deep_link "obsidianbackup://filecoin"
    sleep 2
    is_app_running
'

# ---------------------------------------------------------------------------
# Gaming & Health settings
# ---------------------------------------------------------------------------
log_header "Gaming & Health Settings"

run_test "setting_gaming_screen" "Open gaming backups screen" '
    fire_deep_link "obsidianbackup://gaming"
    sleep 2
    is_app_running
'

run_test "setting_health_screen" "Open health connect screen" '
    fire_deep_link "obsidianbackup://health"
    sleep 2
    is_app_running
'

run_test "setting_health_privacy" "Toggle health data anonymization" '
    toggle_pref "health_anonymize_data" true
    sleep 1
    toggle_pref "health_anonymize_data" false
    is_app_running
'

# ---------------------------------------------------------------------------
# Automation & Scheduling settings
# ---------------------------------------------------------------------------
log_header "Automation Settings"

run_test "setting_smart_scheduling_on" "Enable smart scheduling" '
    toggle_pref "smart_scheduling_enabled" true
    is_app_running
'

run_test "setting_smart_scheduling_off" "Disable smart scheduling" '
    toggle_pref "smart_scheduling_enabled" false
    is_app_running
'

run_test "setting_tasker_on" "Enable Tasker integration" '
    toggle_pref "tasker_integration_enabled" true
    is_app_running
'

# ---------------------------------------------------------------------------
# Plugin settings
# ---------------------------------------------------------------------------
log_header "Plugin Settings"

run_test "setting_plugins_screen" "Open plugins screen" '
    fire_deep_link "obsidianbackup://plugins"
    sleep 2
    is_app_running
'

run_test "setting_plugin_security" "Toggle plugin sandbox" '
    toggle_pref "plugin_sandbox_enabled" true
    is_app_running
'

# ---------------------------------------------------------------------------
# Retention & Storage settings
# ---------------------------------------------------------------------------
log_header "Retention & Storage Settings"

run_test "setting_retention_on" "Enable retention policies" '
    toggle_pref "retention_enabled" true
    is_app_running
'

run_test "setting_storage_limits" "Set storage limit" '
    toggle_pref "storage_limit_enabled" true
    is_app_running
'

# ---------------------------------------------------------------------------
# Permission mode settings
# ---------------------------------------------------------------------------
log_header "Permission Mode Settings"

PERM_MODES=("root" "shizuku" "adb" "saf")
for mode in "${PERM_MODES[@]}"; do
    run_test "setting_perm_mode_${mode}" "Set permission mode: ${mode}" "
        logcat_marker 'PERM_MODE:${mode}'
        adb shell am broadcast \
            -a com.obsidianbackup.action.SET_PREFERENCE \
            -n '$PKG/.automation.TaskerIntegration' \
            --es 'pref_key' 'permission_mode' \
            --es 'pref_value' '${mode}' 2>/dev/null || true
        sleep 2
        is_app_running
    "
done

# ---------------------------------------------------------------------------
# Advanced settings
# ---------------------------------------------------------------------------
log_header "Advanced Settings"

run_test "setting_debug_mode_on" "Enable debug mode" '
    toggle_pref "debug_mode_enabled" true
    is_app_running
'

run_test "setting_debug_mode_off" "Disable debug mode" '
    toggle_pref "debug_mode_enabled" false
    is_app_running
'

run_test "setting_export_diagnostics" "Trigger export diagnostics" '
    logcat_marker "TEST:export_diagnostics"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.EXPORT_DIAGNOSTICS" \
        -n "$PKG/.automation.TaskerIntegration" 2>/dev/null || true
    sleep 3
    is_app_running
'

run_test "setting_export_logs" "Trigger export app logs" '
    logcat_marker "TEST:export_logs"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.EXPORT_LOGS" \
        -n "$PKG/.automation.TaskerIntegration" 2>/dev/null || true
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# Feature flags — toggle all 13
# ---------------------------------------------------------------------------
log_header "Feature Flags"

FEATURE_FLAGS=(
    "PARALLEL_BACKUP"
    "INCREMENTAL_BACKUP"
    "MERKLE_VERIFICATION"
    "WIFI_DIRECT_MIGRATION"
    "PLUGIN_SYSTEM"
    "GAMING_BACKUP"
    "HEALTH_CONNECT_SYNC"
    "SMART_SCHEDULING"
    "TASKER_INTEGRATION"
    "BIOMETRIC_AUTH"
    "DEEP_LINKING"
    "CLOUD_SYNC"
    "SPLIT_APK_HANDLING"
)

for flag in "${FEATURE_FLAGS[@]}"; do
    run_test "feature_flag_${flag,,}_toggle" "Toggle feature flag: $flag" "
        logcat_marker 'FLAG_TOGGLE:${flag}'
        toggle_pref 'feature_flag_${flag}' false
        sleep 1
        toggle_pref 'feature_flag_${flag}' true
        is_app_running
    "
done

print_summary
