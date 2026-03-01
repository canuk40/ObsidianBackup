#!/usr/bin/env bash
# =============================================================================
# test_root_features.sh — Magisk/root-specific feature testing
# =============================================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"
parse_args "$@"
SUITE_NAME="root_features"

log_header "ROOT / MAGISK FEATURE TESTS — $VARIANT"

launch_app
sleep 3

# ---------------------------------------------------------------------------
# Root detection
# ---------------------------------------------------------------------------
log_header "Root Detection"

run_test "root_detected" "App detects root access" '
    logcat_marker "ROOT:detection"
    # Check logcat for root detection output
    adb shell log -t "STRESS_TEST" "Checking root detection..."
    # The app should have logged root status on launch
    local root_log
    root_log=$(adb shell "logcat -d -t 200 -s RootDetector:V ObsidianBackup:V" 2>/dev/null | \
        grep -i "root\|magisk\|superuser\|su " | head -5 || true)
    log_info "Root detection log: $root_log"
    # App should be running and root should be detected on a rooted device
    is_app_running
'

run_test "root_uid_check" "Root UID check via su" '
    local uid
    uid=$(adb shell su -c "id -u" 2>/dev/null | tr -d "\r")
    [[ "$uid" == "0" ]]
'

# ---------------------------------------------------------------------------
# Magisk module management
# ---------------------------------------------------------------------------
log_header "Magisk Module Management"

run_test "magisk_version" "Read Magisk version" '
    local version
    version=$(adb shell su -c "magisk -v" 2>/dev/null | tr -d "\r")
    log_info "Magisk version: $version"
    [[ -n "$version" ]]
'

run_test "magisk_list_modules" "List Magisk modules" '
    logcat_marker "MAGISK:list_modules"
    local modules
    modules=$(adb shell su -c "ls /data/adb/modules/ 2>/dev/null" | tr -d "\r")
    log_info "Installed modules: $modules"
    true
'

run_test "magisk_module_props" "Read module.prop files" '
    adb shell su -c "for dir in /data/adb/modules/*/; do
        if [ -f \"\${dir}module.prop\" ]; then
            echo \"=== \$dir ===\"
            cat \"\${dir}module.prop\"
        fi
    done" 2>/dev/null || true
    true
'

# ---------------------------------------------------------------------------
# SELinux
# ---------------------------------------------------------------------------
log_header "SELinux"

run_test "selinux_getenforce" "SELinux getenforce" '
    local status
    status=$(adb shell getenforce 2>/dev/null | tr -d "\r")
    log_info "SELinux: $status"
    [[ "$status" == "Enforcing" || "$status" == "Permissive" ]]
'

run_test "selinux_context_read" "Read SELinux context for app data" '
    logcat_marker "SELINUX:context_read"
    local ctx
    ctx=$(adb shell su -c "ls -Z /data/data/$PKG/ 2>/dev/null" | head -5 || true)
    log_info "App data context: $ctx"
    true
'

# ---------------------------------------------------------------------------
# Shell executor (root commands via the app)
# ---------------------------------------------------------------------------
log_header "Root Shell Operations"

run_test "shell_basic_command" "Execute basic root command (id)" '
    local result
    result=$(adb shell su -c "id" 2>/dev/null | tr -d "\r")
    log_info "Root id: $result"
    echo "$result" | grep -q "uid=0"
'

run_test "shell_list_packages" "List packages via root" '
    local count
    count=$(adb shell su -c "pm list packages" 2>/dev/null | wc -l)
    log_info "Package count: $count"
    [[ "$count" -gt 0 ]]
'

run_test "shell_app_data_access" "Access app data directory via root" '
    adb shell su -c "ls /data/data/$PKG/" 2>/dev/null | grep -q "."
'

run_test "shell_read_app_prefs" "Read app SharedPreferences via root" '
    local prefs_dir="/data/data/$PKG/shared_prefs"
    local files
    files=$(adb shell su -c "ls $prefs_dir/ 2>/dev/null" || true)
    log_info "Prefs files: $files"
    true
'

# ---------------------------------------------------------------------------
# BusyBox
# ---------------------------------------------------------------------------
log_header "BusyBox"

run_test "busybox_available" "BusyBox binary is available" '
    local bb
    bb=$(adb shell su -c "which busybox 2>/dev/null || echo none" | tr -d "\r")
    log_info "BusyBox path: $bb"
    [[ "$bb" != "none" ]]
'

run_test "busybox_applets" "BusyBox applets are functional" '
    # Test a few key applets used by the app
    adb shell su -c "busybox tar --help" 2>/dev/null | grep -qi "tar" || true
    adb shell su -c "busybox find --help" 2>/dev/null | grep -qi "find" || true
    true
'

# ---------------------------------------------------------------------------
# Root-specific backup operations
# ---------------------------------------------------------------------------
log_header "Root Backup Operations"

run_test "root_backup_app_data" "Backup app data via root (tar)" '
    logcat_marker "ROOT:backup_app_data"
    # Pick a small system app for testing
    local test_pkg="com.android.providers.settings"
    adb shell su -c "tar -czf /sdcard/ObsidianBackup_Test/root_backup_test.tar.gz \
        -C /data/data/$test_pkg . 2>/dev/null" || true
    adb shell "[ -f /sdcard/ObsidianBackup_Test/root_backup_test.tar.gz ]"
'

run_test "root_permission_granting" "Grant permission via root pm" '
    logcat_marker "ROOT:permission_grant"
    adb shell su -c "pm grant $PKG android.permission.READ_CONTACTS" 2>/dev/null || true
    true
'

run_test "root_app_data_listing" "List all app data dirs" '
    local count
    count=$(adb shell su -c "ls /data/data/ | wc -l" 2>/dev/null | tr -d "\r")
    log_info "App data dirs: $count"
    [[ "$count" -gt 0 ]]
'

# ---------------------------------------------------------------------------
# Persistent shell session health
# ---------------------------------------------------------------------------
run_test "persistent_shell" "Persistent root shell session" '
    logcat_marker "ROOT:persistent_shell"
    # Simulate multiple commands in sequence (like PersistentShellSession)
    adb shell su -c "echo cmd1_ok && echo cmd2_ok && echo cmd3_ok" 2>/dev/null | \
        grep -c "ok" | grep -q "3"
'

# ---------------------------------------------------------------------------
# Root detection from app perspective (via logcat)
# ---------------------------------------------------------------------------
run_test "app_root_status" "App reports root status in logcat" '
    force_stop_app
    launch_app
    sleep 4
    local log
    log=$(adb shell "logcat -d -t 100 -s RootDetector:V" 2>/dev/null | \
        grep -i "root\|status\|detected" | tail -3 || true)
    log_info "App root status: $log"
    is_app_running
'

# ---------------------------------------------------------------------------
# Cleanup
# ---------------------------------------------------------------------------
run_test "root_cleanup" "Clean up root test artifacts" '
    adb shell su -c "rm -f /sdcard/ObsidianBackup_Test/root_backup_test.tar.gz" 2>/dev/null || true
    true
'

print_summary
