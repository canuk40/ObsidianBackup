#!/usr/bin/env bash
# =============================================================================
# run_all.sh — Master orchestrator for the ObsidianBackup stress test suite
# Usage: ./run_all.sh [--variant free|premium] [--build debug|release]
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Create a shared results directory for this run
export RESULTS_DIR="$SCRIPT_DIR/results/$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

source "$SCRIPT_DIR/common.sh"
parse_args "$@"

echo ""
echo "================================================================"
echo "  ObsidianBackup Comprehensive Stress Test Suite"
echo "  $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
echo "  Variant: $VARIANT | Build: $BUILD_TYPE | Pkg: $PKG"
echo "  Results: $RESULTS_DIR"
echo "================================================================"
echo ""

# ---------------------------------------------------------------------------
# Start logcat capture
# ---------------------------------------------------------------------------
log_header "Starting logcat capture"
bash "$SCRIPT_DIR/logcat_capture.sh" start --results-dir "$RESULTS_DIR"
sleep 2

# ---------------------------------------------------------------------------
# Run all test suites
# ---------------------------------------------------------------------------
TOTAL_PASS=0
TOTAL_FAIL=0
TOTAL_SKIP=0
SUITES_PASSED=0
SUITES_FAILED=0

run_suite() {
    local script="$1"
    local name=$(basename "$script" .sh)

    echo ""
    log_header "SUITE: $name"
    logcat_marker "SUITE_START:$name"

    set +e
    bash "$script" --variant "$VARIANT" --build "$BUILD_TYPE" --results "$RESULTS_DIR"
    local exit_code=$?
    set -e

    logcat_marker "SUITE_END:$name"

    # Read the suite summary
    if [[ -f "$RESULTS_DIR/${name}_summary.txt" ]]; then
        local pass=$(grep "^Passed:" "$RESULTS_DIR/${name}_summary.txt" | awk '{print $2}')
        local fail=$(grep "^Failed:" "$RESULTS_DIR/${name}_summary.txt" | awk '{print $2}')
        local skip=$(grep "^Skipped:" "$RESULTS_DIR/${name}_summary.txt" | awk '{print $2}')
        TOTAL_PASS=$((TOTAL_PASS + ${pass:-0}))
        TOTAL_FAIL=$((TOTAL_FAIL + ${fail:-0}))
        TOTAL_SKIP=$((TOTAL_SKIP + ${skip:-0}))
    fi

    if [[ $exit_code -eq 0 ]]; then
        ((SUITES_PASSED++))
    else
        ((SUITES_FAILED++))
        take_screenshot "suite_fail_${name}"
    fi
}

# Phase 0: Setup
run_suite "$SCRIPT_DIR/setup_test_env.sh"

# Phase 1: Feature tests
PHASE1_SCRIPTS=(
    "test_navigation.sh"
    "test_backup_engines.sh"
    "test_settings.sh"
    "test_deep_links.sh"
    "test_automation.sh"
    "test_cloud_providers.sh"
    "test_root_features.sh"
    "test_security.sh"
    "test_plugins.sh"
    "test_widgets.sh"
)

for script in "${PHASE1_SCRIPTS[@]}"; do
    run_suite "$SCRIPT_DIR/$script"
done

# Phase 2: Performance stress
PHASE2_SCRIPTS=(
    "stress_large_backup.sh"
    "stress_rapid_operations.sh"
    "stress_memory_battery.sh"
    "stress_interruptions.sh"
)

for script in "${PHASE2_SCRIPTS[@]}"; do
    run_suite "$SCRIPT_DIR/$script"
done

# Phase 3: Edge cases and variant tests
run_suite "$SCRIPT_DIR/test_edge_cases.sh"

# Variant-specific tests (always run both)
log_header "FREE VARIANT TESTS"
VARIANT="free" PKG="$PKG_FREE_DEBUG" run_suite "$SCRIPT_DIR/test_free_limits.sh"

log_header "PREMIUM VARIANT TESTS"
VARIANT="premium" PKG="$PKG_PREMIUM_DEBUG" run_suite "$SCRIPT_DIR/test_premium_features.sh"

# ---------------------------------------------------------------------------
# Stop logcat
# ---------------------------------------------------------------------------
bash "$SCRIPT_DIR/logcat_capture.sh" stop --results-dir "$RESULTS_DIR"

# ---------------------------------------------------------------------------
# Generate final report
# ---------------------------------------------------------------------------
TOTAL_TESTS=$((TOTAL_PASS + TOTAL_FAIL + TOTAL_SKIP))
TOTAL_SUITES=$((SUITES_PASSED + SUITES_FAILED))
CRASH_TOTAL=$(grep -c "FATAL EXCEPTION" "$RESULTS_DIR/logcat/logcat_full.log" 2>/dev/null || echo "0")
ANR_TOTAL=$(grep -c "ANR in" "$RESULTS_DIR/logcat/logcat_full.log" 2>/dev/null || echo "0")

cat > "$RESULTS_DIR/FINAL_REPORT.txt" <<EOF
================================================================
  ObsidianBackup Stress Test — FINAL REPORT
  $(date -u +"%Y-%m-%dT%H:%M:%SZ")
================================================================

ENVIRONMENT
  Package (free):    $PKG_FREE_DEBUG
  Package (premium): $PKG_PREMIUM_DEBUG

RESULTS
  Test Suites: $TOTAL_SUITES ($SUITES_PASSED passed, $SUITES_FAILED failed)
  Test Cases:  $TOTAL_TESTS ($TOTAL_PASS passed, $TOTAL_FAIL failed, $TOTAL_SKIP skipped)
  Crashes:     $CRASH_TOTAL
  ANRs:        $ANR_TOTAL

PASS RATE: $(( TOTAL_TESTS > 0 ? (TOTAL_PASS * 100 / TOTAL_TESTS) : 0 ))%

FILES
  Logcat:       $RESULTS_DIR/logcat/logcat_full.log
  Crashes:      $RESULTS_DIR/logcat/crashes_live.log
  Screenshots:  $RESULTS_DIR/screenshots/
  Test Results: $RESULTS_DIR/results.csv
  Device Info:  $RESULTS_DIR/device_info.txt
================================================================
EOF

echo ""
echo "================================================================"
echo "  FINAL RESULTS"
echo "================================================================"
cat "$RESULTS_DIR/FINAL_REPORT.txt"
echo ""

if [[ $TOTAL_FAIL -gt 0 || $CRASH_TOTAL -gt 0 ]]; then
    log_fail "Stress test completed with failures"
    exit 1
else
    log_pass "All stress tests passed!"
    exit 0
fi
