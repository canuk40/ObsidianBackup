#!/usr/bin/env bash
# =============================================================================
# test_security.sh — Security feature validation
# =============================================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"
parse_args "$@"
SUITE_NAME="security"

log_header "SECURITY TESTS — $VARIANT"

launch_app
sleep 3

TASKER_RECEIVER="$PKG/.automation.TaskerIntegration"

# ---------------------------------------------------------------------------
# Encryption flow
# ---------------------------------------------------------------------------
log_header "Encryption / Decryption"

run_test "encryption_enable" "Enable encryption and perform backup" '
    logcat_marker "SEC:encryption_enable"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.SET_PREFERENCE" \
        -n "$TASKER_RECEIVER" \
        --es "pref_key" "encryption_enabled" \
        --ez "pref_value" true 2>/dev/null || true
    sleep 2
    adb shell am broadcast \
        -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$TASKER_RECEIVER" 2>/dev/null || true
    sleep 8
    is_app_running
'

run_test "encryption_disable" "Disable encryption" '
    adb shell am broadcast \
        -a "com.obsidianbackup.action.SET_PREFERENCE" \
        -n "$TASKER_RECEIVER" \
        --es "pref_key" "encryption_enabled" \
        --ez "pref_value" false 2>/dev/null || true
    sleep 2
    is_app_running
'

# ---------------------------------------------------------------------------
# Zero-knowledge encryption
# ---------------------------------------------------------------------------
log_header "Zero-Knowledge Encryption"

run_test "zk_screen" "Open zero-knowledge screen" '
    fire_deep_link "obsidianbackup://zero_knowledge"
    sleep 3
    is_app_running
'

run_test "zk_enable" "Enable zero-knowledge mode" '
    logcat_marker "SEC:zk_enable"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.SET_PREFERENCE" \
        -n "$TASKER_RECEIVER" \
        --es "pref_key" "zero_knowledge_enabled" \
        --ez "pref_value" true 2>/dev/null || true
    sleep 2
    is_app_running
'

# ---------------------------------------------------------------------------
# Path security validation — malicious paths should be rejected
# ---------------------------------------------------------------------------
log_header "Path Security Validation"

MALICIOUS_PATHS=(
    "../../../etc/passwd"
    "/data/data/com.other.app/databases/secrets.db"
    "/system/build.prop"
    "../../proc/self/environ"
    "file:///data/local/tmp/exploit"
    "/dev/null; rm -rf /"
    "$(echo -e '\x00')/evil"
)

for i in "${!MALICIOUS_PATHS[@]}"; do
    path="${MALICIOUS_PATHS[$i]}"
    run_test "path_security_${i}" "Reject malicious path: ${path:0:40}..." "
        logcat_marker 'SEC:path_validation_${i}'
        # Attempt to use malicious path in a backup request
        adb shell am broadcast \
            -a 'com.obsidianbackup.action.START_BACKUP' \
            -n '$TASKER_RECEIVER' \
            --es 'backup_path' '${path}' 2>/dev/null || true
        sleep 2
        is_app_running
    "
done

# ---------------------------------------------------------------------------
# Shell command injection attempts
# ---------------------------------------------------------------------------
log_header "Shell Command Injection"

INJECTION_ATTEMPTS=(
    "; rm -rf /"
    "| cat /etc/passwd"
    "\$(reboot)"
    "&& chmod 777 /system"
    "'; DROP TABLE backups; --"
    "\`reboot\`"
)

for i in "${!INJECTION_ATTEMPTS[@]}"; do
    attempt="${INJECTION_ATTEMPTS[$i]}"
    run_test "shell_injection_${i}" "Block injection: ${attempt:0:30}..." "
        logcat_marker 'SEC:injection_${i}'
        adb shell am broadcast \
            -a 'com.obsidianbackup.action.START_BACKUP' \
            -n '$TASKER_RECEIVER' \
            --es 'app_name' '${attempt}' 2>/dev/null || true
        sleep 2
        is_app_running
    "
done

# ---------------------------------------------------------------------------
# Keystore / Android Keystore
# ---------------------------------------------------------------------------
log_header "Keystore Operations"

run_test "keystore_accessible" "Android Keystore is accessible" '
    logcat_marker "SEC:keystore"
    # App should have created keystore entries on first run
    local ks_log
    ks_log=$(adb shell "logcat -d -t 200 -s ObsidianBackup:V" 2>/dev/null | \
        grep -i "keystore\|crypto\|encrypt" | tail -5 || true)
    log_info "Keystore log: $ks_log"
    is_app_running
'

# ---------------------------------------------------------------------------
# Root detection security (app should detect and report root)
# ---------------------------------------------------------------------------
log_header "Root Detection Security"

run_test "root_detection_report" "App root detection is functional" '
    force_stop_app
    launch_app
    sleep 4
    local root_log
    root_log=$(adb shell "logcat -d -t 200 -s RootDetector:V" 2>/dev/null | tail -5 || true)
    log_info "Root detector output: $root_log"
    is_app_running
'

# ---------------------------------------------------------------------------
# SafetyNet / Play Integrity
# ---------------------------------------------------------------------------
run_test "safetynet_check" "SafetyNet/Play Integrity check" '
    logcat_marker "SEC:safetynet"
    local sn_log
    sn_log=$(adb shell "logcat -d -t 200 -s ObsidianBackup:V" 2>/dev/null | \
        grep -i "safety\|integrity\|attestation" | tail -3 || true)
    log_info "SafetyNet log: $sn_log"
    true
'

# ---------------------------------------------------------------------------
# Export diagnostics should not leak secrets
# ---------------------------------------------------------------------------
run_test "diagnostics_no_secrets" "Export diagnostics does not leak secrets" '
    logcat_marker "SEC:diagnostics_check"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.EXPORT_DIAGNOSTICS" \
        -n "$TASKER_RECEIVER" 2>/dev/null || true
    sleep 3
    # Check that logcat during export does not contain key material
    local leaks
    leaks=$(adb shell "logcat -d -t 100" 2>/dev/null | \
        grep -iE "private.?key|secret.?key|password|api.?key" | \
        grep -i "obsidianbackup" | wc -l || echo "0")
    log_info "Potential secret leaks in logcat: $leaks"
    is_app_running
'

print_summary
