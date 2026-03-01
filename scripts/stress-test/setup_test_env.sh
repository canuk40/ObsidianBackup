#!/usr/bin/env bash
# =============================================================================
# setup_test_env.sh — Prepare the Magisk-rooted emulator for stress testing
# Verifies root, installs APKs, grants permissions, seeds test data
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"
parse_args "$@"

SUITE_NAME="setup"
log_header "ENVIRONMENT SETUP"

# ---------------------------------------------------------------------------
# 1. Verify device connectivity
# ---------------------------------------------------------------------------
run_test "device_connected" "ADB device is reachable" '
    adb devices | grep -q "device$"
'

# ---------------------------------------------------------------------------
# 2. Verify root access
# ---------------------------------------------------------------------------
run_test "root_access" "Device has root (su) access" '
    local uid
    uid=$(adb shell su -c "id -u" 2>/dev/null | tr -d "\r")
    [[ "$uid" == "0" ]]
'

# ---------------------------------------------------------------------------
# 3. Verify Magisk
# ---------------------------------------------------------------------------
run_test "magisk_installed" "Magisk is installed" '
    adb shell su -c "magisk -v" 2>/dev/null | grep -qi "magisk\|kitsune\|alpha" || \
    adb shell "[ -d /data/adb/magisk ]" 2>/dev/null
'

# ---------------------------------------------------------------------------
# 4. Check SELinux status
# ---------------------------------------------------------------------------
run_test "selinux_status" "SELinux status is readable" '
    local status
    status=$(adb shell getenforce 2>/dev/null | tr -d "\r")
    log_info "SELinux: $status"
    [[ -n "$status" ]]
'

# ---------------------------------------------------------------------------
# 5. Build APKs if needed
# ---------------------------------------------------------------------------
build_apks() {
    local free_apk="$PROJECT_ROOT/app/build/outputs/apk/free/debug/app-free-debug.apk"
    local premium_apk="$PROJECT_ROOT/app/build/outputs/apk/premium/debug/app-premium-debug.apk"

    if [[ ! -f "$free_apk" || ! -f "$premium_apk" ]]; then
        log_info "Building APKs (this may take a few minutes)..."
        cd "$PROJECT_ROOT"
        ./gradlew assembleFreeDebug assemblePremiumDebug --quiet
        cd "$SCRIPT_DIR"
    fi

    [[ -f "$free_apk" && -f "$premium_apk" ]]
}

run_test "build_apks" "Build Free and Premium debug APKs" 'build_apks'

# ---------------------------------------------------------------------------
# 6. Install APKs
# ---------------------------------------------------------------------------
install_apk() {
    local flavor="$1"
    local apk="$PROJECT_ROOT/app/build/outputs/apk/$flavor/debug/app-${flavor}-debug.apk"
    if [[ -f "$apk" ]]; then
        adb install -r -t "$apk" 2>/dev/null
    else
        log_warn "APK not found: $apk"
        return 1
    fi
}

run_test "install_free" "Install Free debug APK" 'install_apk free'
run_test "install_premium" "Install Premium debug APK" 'install_apk premium'

# ---------------------------------------------------------------------------
# 7. Grant permissions
# ---------------------------------------------------------------------------
grant_permissions() {
    local pkg="$1"
    local perms=(
        "android.permission.READ_EXTERNAL_STORAGE"
        "android.permission.WRITE_EXTERNAL_STORAGE"
        "android.permission.READ_CONTACTS"
        "android.permission.WRITE_CONTACTS"
        "android.permission.READ_CALL_LOG"
        "android.permission.WRITE_CALL_LOG"
        "android.permission.READ_SMS"
        "android.permission.POST_NOTIFICATIONS"
        "android.permission.FOREGROUND_SERVICE"
        "android.permission.QUERY_ALL_PACKAGES"
        "android.permission.ACCESS_FINE_LOCATION"
        "android.permission.ACCESS_COARSE_LOCATION"
    )
    for perm in "${perms[@]}"; do
        adb shell pm grant "$pkg" "$perm" 2>/dev/null || true
    done
    # MANAGE_EXTERNAL_STORAGE via appops (Android 11+)
    adb shell appops set "$pkg" MANAGE_EXTERNAL_STORAGE allow 2>/dev/null || true
}

run_test "grant_perms_free" "Grant permissions to Free variant" \
    "grant_permissions $PKG_FREE_DEBUG"
run_test "grant_perms_premium" "Grant permissions to Premium variant" \
    "grant_permissions $PKG_PREMIUM_DEBUG"

# ---------------------------------------------------------------------------
# 8. Seed test data
# ---------------------------------------------------------------------------
seed_test_data() {
    log_info "Seeding test data on device..."

    # Create test directories
    adb shell mkdir -p /sdcard/ObsidianBackup_Test/large_files 2>/dev/null
    adb shell mkdir -p /sdcard/ObsidianBackup_Test/gaming_saves 2>/dev/null
    adb shell mkdir -p /sdcard/ObsidianBackup_Test/media 2>/dev/null

    # Create files of various sizes for backup testing
    adb shell "dd if=/dev/urandom of=/sdcard/ObsidianBackup_Test/small_1kb.bin bs=1024 count=1" 2>/dev/null
    adb shell "dd if=/dev/urandom of=/sdcard/ObsidianBackup_Test/medium_1mb.bin bs=1024 count=1024" 2>/dev/null
    adb shell "dd if=/dev/urandom of=/sdcard/ObsidianBackup_Test/large_files/big_50mb.bin bs=1024 count=51200" 2>/dev/null

    # Create fake gaming save files
    adb shell "echo 'fake retroarch save' > /sdcard/ObsidianBackup_Test/gaming_saves/game1.srm" 2>/dev/null
    adb shell "echo 'fake dolphin save' > /sdcard/ObsidianBackup_Test/gaming_saves/game2.gci" 2>/dev/null

    # Create a text file for media backup
    adb shell "echo 'test media content' > /sdcard/ObsidianBackup_Test/media/test.txt" 2>/dev/null

    return 0
}

run_test "seed_data" "Seed test data on device" 'seed_test_data'

# ---------------------------------------------------------------------------
# 9. Verify BusyBox
# ---------------------------------------------------------------------------
run_test "busybox_available" "BusyBox is available" '
    adb shell su -c "busybox --help" 2>/dev/null | grep -q "BusyBox" || \
    adb shell su -c "which busybox" 2>/dev/null | grep -q "busybox"
'

# ---------------------------------------------------------------------------
# 10. Record device info
# ---------------------------------------------------------------------------
record_device_info() {
    cat > "$RESULTS_DIR/device_info.txt" <<EOF
Date: $(date -u +"%Y-%m-%dT%H:%M:%SZ")
Device: $(adb shell getprop ro.product.model | tr -d '\r')
Android: $(adb shell getprop ro.build.version.release | tr -d '\r')
API: $(adb shell getprop ro.build.version.sdk | tr -d '\r')
ABI: $(adb shell getprop ro.product.cpu.abi | tr -d '\r')
Magisk: $(adb shell su -c "magisk -v" 2>/dev/null | tr -d '\r')
SELinux: $(adb shell getenforce 2>/dev/null | tr -d '\r')
RAM: $(adb shell cat /proc/meminfo | grep MemTotal | tr -d '\r')
Disk: $(adb shell df /data | tail -1 | tr -d '\r')
Free pkg: $PKG_FREE_DEBUG
Premium pkg: $PKG_PREMIUM_DEBUG
EOF
    log_info "Device info saved to $RESULTS_DIR/device_info.txt"
    return 0
}

run_test "device_info" "Record device info" 'record_device_info'

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
print_summary
