#!/usr/bin/env bash
# =============================================================================
# stress_memory_battery.sh — Resource pressure stress test
# =============================================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"
parse_args "$@"
SUITE_NAME="stress_resources"

log_header "RESOURCE PRESSURE STRESS TEST — $VARIANT"

launch_app
sleep 3

TASKER_RECEIVER="$PKG/.automation.TaskerIntegration"

# ---------------------------------------------------------------------------
# Baseline snapshots
# ---------------------------------------------------------------------------
run_test "baseline_memory" "Capture baseline memory" '
    capture_meminfo "resource_baseline"
    adb shell dumpsys meminfo "$PKG" 2>/dev/null | head -20 > "$RESULTS_DIR/mem_baseline.txt"
    true
'

run_test "baseline_battery" "Capture baseline battery stats" '
    adb shell dumpsys batterystats --reset 2>/dev/null
    adb shell dumpsys batterystats "$PKG" 2>/dev/null | head -30 > "$RESULTS_DIR/battery_baseline.txt"
    true
'

# ---------------------------------------------------------------------------
# Memory monitoring during backup
# ---------------------------------------------------------------------------
run_test "memory_during_backup" "Monitor memory during backup" '
    logcat_marker "STRESS:memory_during_backup"
    # Start a backup
    adb shell am broadcast \
        -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$TASKER_RECEIVER" \
        --es "backup_type" "apps" 2>/dev/null
    # Sample memory 5 times during the operation
    for i in $(seq 1 5); do
        sleep 5
        local mem
        mem=$(adb shell dumpsys meminfo "$PKG" 2>/dev/null | grep "TOTAL" | head -1 || true)
        log_info "Memory sample $i: $mem"
        echo "sample_$i: $mem" >> "$RESULTS_DIR/mem_during_backup.txt"
    done
    is_app_running
'

# ---------------------------------------------------------------------------
# Force low-memory trim
# ---------------------------------------------------------------------------
run_test "low_memory_trim" "App survives TRIM_MEMORY signals" '
    logcat_marker "STRESS:trim_memory"
    # Send various trim levels
    local trim_levels=("RUNNING_LOW" "RUNNING_CRITICAL" "BACKGROUND" "MODERATE" "COMPLETE")
    for level in "${trim_levels[@]}"; do
        log_info "Sending trim: $level"
        local level_num
        case "$level" in
            RUNNING_LOW)      level_num=10 ;;
            RUNNING_CRITICAL) level_num=15 ;;
            BACKGROUND)       level_num=40 ;;
            MODERATE)         level_num=60 ;;
            COMPLETE)         level_num=80 ;;
        esac
        adb shell am send-trim-memory "$PKG" "$level_num" 2>/dev/null || true
        sleep 2
    done
    sleep 2
    is_app_running
'

# ---------------------------------------------------------------------------
# Background/foreground cycling during backup
# ---------------------------------------------------------------------------
run_test "bg_fg_cycling" "Background/foreground cycling during operation" '
    logcat_marker "STRESS:bg_fg_cycle"
    # Start a backup
    adb shell am broadcast \
        -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$TASKER_RECEIVER" \
        --es "backup_type" "apps" 2>/dev/null
    sleep 2
    # Cycle home/app 10 times
    for i in $(seq 1 10); do
        adb shell input keyevent KEYCODE_HOME
        sleep 1
        adb shell am start -n "$PKG/$MAIN_ACTIVITY" >/dev/null 2>&1
        sleep 1
    done
    sleep 3
    is_app_running
'

# ---------------------------------------------------------------------------
# Heavy GC pressure
# ---------------------------------------------------------------------------
run_test "gc_pressure" "App survives forced GC" '
    logcat_marker "STRESS:gc_pressure"
    # Force garbage collection via SIGUSR1 (kills dalvik GC)
    local pid
    pid=$(adb shell pidof "$PKG" 2>/dev/null | tr -d "\r")
    if [[ -n "$pid" ]]; then
        adb shell kill -10 "$pid" 2>/dev/null || true  # SIGUSR1 triggers GC
        sleep 2
    fi
    is_app_running
'

# ---------------------------------------------------------------------------
# CPU stress during backup
# ---------------------------------------------------------------------------
run_test "cpu_during_backup" "Monitor CPU during backup" '
    logcat_marker "STRESS:cpu_monitoring"
    adb shell am broadcast \
        -a "com.obsidianbackup.action.START_BACKUP" \
        -n "$TASKER_RECEIVER" \
        --es "backup_type" "apps" 2>/dev/null
    # Sample CPU 5 times
    for i in $(seq 1 5); do
        sleep 3
        local cpu
        cpu=$(adb shell top -n 1 -b 2>/dev/null | grep "$PKG" | head -1 || true)
        log_info "CPU sample $i: $cpu"
        echo "cpu_sample_$i: $cpu" >> "$RESULTS_DIR/cpu_during_backup.txt"
    done
    is_app_running
'

# ---------------------------------------------------------------------------
# Battery stats after operations
# ---------------------------------------------------------------------------
run_test "battery_post_stress" "Capture battery stats after stress" '
    adb shell dumpsys batterystats "$PKG" 2>/dev/null | head -50 > "$RESULTS_DIR/battery_post_stress.txt"
    log_info "Battery stats saved"
    true
'

# ---------------------------------------------------------------------------
# Final memory snapshot
# ---------------------------------------------------------------------------
run_test "memory_post_stress" "Final memory snapshot" '
    capture_meminfo "resource_post_stress"
    local mem
    mem=$(adb shell dumpsys meminfo "$PKG" 2>/dev/null | grep "TOTAL" | head -1 || true)
    log_info "Final memory: $mem"
    is_app_running
'

print_summary
