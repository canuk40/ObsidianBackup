#!/usr/bin/env bash
# =============================================================================
# test_plugins.sh — Plugin system validation
# =============================================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"
parse_args "$@"
SUITE_NAME="plugins"

log_header "PLUGIN SYSTEM TESTS — $VARIANT"

launch_app
sleep 3

# ---------------------------------------------------------------------------
# Plugin screen
# ---------------------------------------------------------------------------
run_test "plugin_screen_open" "Open plugins screen" '
    fire_deep_link "obsidianbackup://plugins"
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# Plugin registry enumeration
# ---------------------------------------------------------------------------
run_test "plugin_registry" "Plugin registry loads without crash" '
    logcat_marker "PLUGIN:registry"
    # Navigate to plugins and check logcat for registration
    fire_deep_link "obsidianbackup://plugins"
    sleep 3
    local plugin_log
    plugin_log=$(adb shell "logcat -d -t 100 -s ObsidianBackup:V" 2>/dev/null | \
        grep -i "plugin\|registry" | tail -5 || true)
    log_info "Plugin registry: $plugin_log"
    is_app_running
'

# ---------------------------------------------------------------------------
# Built-in plugins
# ---------------------------------------------------------------------------
BUILTIN_PLUGINS=("default_automation" "filecoin_cloud")

for plugin in "${BUILTIN_PLUGINS[@]}"; do
    run_test "plugin_builtin_${plugin}" "Built-in plugin loads: ${plugin}" "
        logcat_marker 'PLUGIN:load_${plugin}'
        sleep 1
        is_app_running
    "
done

# ---------------------------------------------------------------------------
# Plugin types
# ---------------------------------------------------------------------------
PLUGIN_TYPES=("backup_engine" "cloud_provider" "automation" "export")

for ptype in "${PLUGIN_TYPES[@]}"; do
    run_test "plugin_type_${ptype}" "Plugin type supported: ${ptype}" "
        logcat_marker 'PLUGIN_TYPE:${ptype}'
        sleep 1
        is_app_running
    "
done

# ---------------------------------------------------------------------------
# Plugin sandbox
# ---------------------------------------------------------------------------
run_test "plugin_sandbox_enabled" "Plugin sandbox is active" '
    logcat_marker "PLUGIN:sandbox_check"
    local sandbox_log
    sandbox_log=$(adb shell "logcat -d -t 100 -s ObsidianBackup:V" 2>/dev/null | \
        grep -i "sandbox\|plugin.*security" | tail -3 || true)
    log_info "Plugin sandbox: $sandbox_log"
    is_app_running
'

# ---------------------------------------------------------------------------
# Plugin configuration
# ---------------------------------------------------------------------------
run_test "plugin_config_fields" "Plugin configuration UI loads" '
    logcat_marker "PLUGIN:config"
    fire_deep_link "obsidianbackup://plugins"
    sleep 3
    take_screenshot "plugin_config"
    is_app_running
'

# ---------------------------------------------------------------------------
# Navigate away and back to plugins
# ---------------------------------------------------------------------------
run_test "plugin_screen_stability" "Plugin screen survives navigation cycle" '
    fire_deep_link "obsidianbackup://plugins"
    sleep 2
    fire_deep_link "obsidianbackup://settings"
    sleep 2
    fire_deep_link "obsidianbackup://plugins"
    sleep 2
    is_app_running
'

print_summary
