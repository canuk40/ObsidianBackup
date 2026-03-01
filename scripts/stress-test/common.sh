#!/usr/bin/env bash
# =============================================================================
# common.sh — Shared utilities for ObsidianBackup stress test suite
# Source this file from every test script: source "$(dirname "$0")/common.sh"
# =============================================================================
set -euo pipefail

# ---------------------------------------------------------------------------
# Package names
# ---------------------------------------------------------------------------
PKG_FREE_DEBUG="com.obsidianbackup.free.debug"
PKG_FREE_RELEASE="com.obsidianbackup.free"
PKG_PREMIUM_DEBUG="com.obsidianbackup.debug"
PKG_PREMIUM_RELEASE="com.obsidianbackup"
MAIN_ACTIVITY="com.obsidianbackup.ui.MainActivity"

# Default to free debug unless overridden via --variant
VARIANT="${VARIANT:-free}"
BUILD_TYPE="${BUILD_TYPE:-debug}"

resolve_package() {
    case "${VARIANT}-${BUILD_TYPE}" in
        free-debug)    echo "$PKG_FREE_DEBUG" ;;
        free-release)  echo "$PKG_FREE_RELEASE" ;;
        premium-debug) echo "$PKG_PREMIUM_DEBUG" ;;
        premium-release) echo "$PKG_PREMIUM_RELEASE" ;;
        *) echo "$PKG_FREE_DEBUG" ;;
    esac
}
PKG=$(resolve_package)

# ---------------------------------------------------------------------------
# Directories
# ---------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
RESULTS_DIR="${RESULTS_DIR:-$SCRIPT_DIR/results/$(date +%Y%m%d_%H%M%S)}"
LOGCAT_DIR="$RESULTS_DIR/logcat"
SCREENSHOTS_DIR="$RESULTS_DIR/screenshots"
mkdir -p "$RESULTS_DIR" "$LOGCAT_DIR" "$SCREENSHOTS_DIR"

# ---------------------------------------------------------------------------
# Counters
# ---------------------------------------------------------------------------
PASS_COUNT=0
FAIL_COUNT=0
SKIP_COUNT=0
CRASH_COUNT=0
ANR_COUNT=0
TEST_NAME=""
SUITE_NAME="${SUITE_NAME:-unknown}"

# ---------------------------------------------------------------------------
# Argument parsing (call parse_args "$@" in your test script)
# ---------------------------------------------------------------------------
parse_args() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --variant)   VARIANT="$2"; shift 2 ;;
            --build)     BUILD_TYPE="$2"; shift 2 ;;
            --results)   RESULTS_DIR="$2"; shift 2 ;;
            *)           shift ;;
        esac
    done
    PKG=$(resolve_package)
    mkdir -p "$RESULTS_DIR" "$LOGCAT_DIR" "$SCREENSHOTS_DIR"
}

# ---------------------------------------------------------------------------
# Logging
# ---------------------------------------------------------------------------
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

log_info()  { echo -e "${CYAN}[INFO]${NC}  $(date +%H:%M:%S) $*"; }
log_pass()  { echo -e "${GREEN}[PASS]${NC}  $(date +%H:%M:%S) $*"; }
log_fail()  { echo -e "${RED}[FAIL]${NC}  $(date +%H:%M:%S) $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $(date +%H:%M:%S) $*"; }
log_header(){ echo -e "\n${BOLD}=== $* ===${NC}"; }

# ---------------------------------------------------------------------------
# ADB helpers
# ---------------------------------------------------------------------------
adb_shell() { adb shell "$@" 2>/dev/null; }
adb_root_shell() { adb shell su -c "$*" 2>/dev/null; }

wait_for_device() {
    log_info "Waiting for device..."
    adb wait-for-device
    # Wait for boot to complete
    local timeout=60
    while [[ $timeout -gt 0 ]]; do
        local boot=$(adb_shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
        [[ "$boot" == "1" ]] && return 0
        sleep 1
        ((timeout--))
    done
    log_fail "Device did not boot in time"
    return 1
}

is_app_running() {
    adb_shell "pidof $PKG" &>/dev/null
}

launch_app() {
    adb shell am start -n "$PKG/$MAIN_ACTIVITY" >/dev/null 2>&1
    sleep 2
}

force_stop_app() {
    adb shell am force-stop "$PKG" 2>/dev/null
    sleep 1
}

clear_app_data() {
    adb shell pm clear "$PKG" 2>/dev/null
    sleep 1
}

# ---------------------------------------------------------------------------
# Deep link helper
# ---------------------------------------------------------------------------
fire_deep_link() {
    local uri="$1"
    adb shell am start -a android.intent.action.VIEW -d "$uri" 2>/dev/null
    sleep 2
}

# ---------------------------------------------------------------------------
# Broadcast helper
# ---------------------------------------------------------------------------
send_broadcast() {
    local action="$1"; shift
    adb shell am broadcast -a "$action" -n "$PKG/.automation.TaskerIntegration" "$@" 2>/dev/null
}

# ---------------------------------------------------------------------------
# Screenshot
# ---------------------------------------------------------------------------
take_screenshot() {
    local name="${1:-screenshot}"
    local ts=$(date +%Y%m%d_%H%M%S)
    local path="$SCREENSHOTS_DIR/${name}_${ts}.png"
    adb exec-out screencap -p > "$path" 2>/dev/null
    echo "$path"
}

# ---------------------------------------------------------------------------
# Memory snapshot
# ---------------------------------------------------------------------------
capture_meminfo() {
    local tag="${1:-meminfo}"
    local ts=$(date +%Y%m%d_%H%M%S)
    adb shell dumpsys meminfo "$PKG" > "$RESULTS_DIR/${tag}_${ts}.txt" 2>/dev/null
}

# ---------------------------------------------------------------------------
# Crash/ANR detection from logcat
# ---------------------------------------------------------------------------
LOGCAT_PID_FILE="$RESULTS_DIR/.logcat_pid"

check_for_crashes() {
    local logfile="$LOGCAT_DIR/logcat_full.log"
    [[ ! -f "$logfile" ]] && return 0

    local new_crashes
    new_crashes=$(grep -c "FATAL EXCEPTION" "$logfile" 2>/dev/null || true)
    local new_anrs
    new_anrs=$(grep -c "ANR in $PKG" "$logfile" 2>/dev/null || true)

    if [[ "$new_crashes" -gt "$CRASH_COUNT" ]]; then
        local delta=$((new_crashes - CRASH_COUNT))
        CRASH_COUNT=$new_crashes
        log_fail "Detected $delta new crash(es)! Total: $CRASH_COUNT"
        take_screenshot "crash_${TEST_NAME}"
        # Dump last crash from logcat
        grep -A 30 "FATAL EXCEPTION" "$logfile" | tail -35 >> "$RESULTS_DIR/crashes.log"
    fi
    if [[ "$new_anrs" -gt "$ANR_COUNT" ]]; then
        local delta=$((new_anrs - ANR_COUNT))
        ANR_COUNT=$new_anrs
        log_fail "Detected $delta new ANR(s)! Total: $ANR_COUNT"
        take_screenshot "anr_${TEST_NAME}"
    fi
}

# ---------------------------------------------------------------------------
# Test result tracking
# ---------------------------------------------------------------------------
run_test() {
    TEST_NAME="$1"
    local description="$2"
    shift 2
    local cmd="$*"

    log_info "TEST: $TEST_NAME — $description"

    local before_crashes=$CRASH_COUNT
    local before_anrs=$ANR_COUNT

    set +e
    eval "$cmd"
    local exit_code=$?
    set -e

    sleep 1
    check_for_crashes

    if [[ $exit_code -eq 0 && $CRASH_COUNT -eq $before_crashes && $ANR_COUNT -eq $before_anrs ]]; then
        log_pass "$TEST_NAME"
        ((PASS_COUNT++))
        echo "PASS|$SUITE_NAME|$TEST_NAME|$description" >> "$RESULTS_DIR/results.csv"
    elif [[ $exit_code -eq 2 ]]; then
        log_warn "SKIP: $TEST_NAME"
        ((SKIP_COUNT++))
        echo "SKIP|$SUITE_NAME|$TEST_NAME|$description" >> "$RESULTS_DIR/results.csv"
    else
        log_fail "$TEST_NAME (exit=$exit_code)"
        ((FAIL_COUNT++))
        take_screenshot "fail_${TEST_NAME}"
        echo "FAIL|$SUITE_NAME|$TEST_NAME|$description" >> "$RESULTS_DIR/results.csv"
    fi
}

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
print_summary() {
    echo ""
    log_header "TEST SUMMARY: $SUITE_NAME"
    echo -e "  ${GREEN}PASSED:${NC}  $PASS_COUNT"
    echo -e "  ${RED}FAILED:${NC}  $FAIL_COUNT"
    echo -e "  ${YELLOW}SKIPPED:${NC} $SKIP_COUNT"
    echo -e "  ${RED}CRASHES:${NC} $CRASH_COUNT"
    echo -e "  ${RED}ANRs:${NC}    $ANR_COUNT"
    local total=$((PASS_COUNT + FAIL_COUNT + SKIP_COUNT))
    echo -e "  ${BOLD}TOTAL:${NC}   $total"
    echo ""

    # Write summary file
    cat > "$RESULTS_DIR/${SUITE_NAME}_summary.txt" <<EOF
Suite: $SUITE_NAME
Date: $(date -u +"%Y-%m-%dT%H:%M:%SZ")
Variant: $VARIANT ($BUILD_TYPE)
Package: $PKG
Passed: $PASS_COUNT
Failed: $FAIL_COUNT
Skipped: $SKIP_COUNT
Crashes: $CRASH_COUNT
ANRs: $ANR_COUNT
Total: $total
EOF

    [[ $FAIL_COUNT -gt 0 || $CRASH_COUNT -gt 0 ]] && return 1
    return 0
}

# ---------------------------------------------------------------------------
# Timer
# ---------------------------------------------------------------------------
TIMER_START=0
start_timer() { TIMER_START=$(date +%s%N); }
stop_timer() {
    local end=$(date +%s%N)
    local elapsed_ms=$(( (end - TIMER_START) / 1000000 ))
    echo "$elapsed_ms"
}

# ---------------------------------------------------------------------------
# Logcat marker (writes a marker line into logcat for correlation)
# ---------------------------------------------------------------------------
logcat_marker() {
    local msg="$1"
    adb shell log -t "STRESS_TEST" "$msg" 2>/dev/null
}

log_info "common.sh loaded | pkg=$PKG | variant=$VARIANT | results=$RESULTS_DIR"
