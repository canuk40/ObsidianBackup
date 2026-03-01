#!/usr/bin/env bash
# =============================================================================
# logcat_capture.sh — Start/stop persistent logcat capture with crash detection
# Usage:
#   ./logcat_capture.sh start [--results-dir /path]
#   ./logcat_capture.sh stop
#   ./logcat_capture.sh status
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="${RESULTS_DIR:-$SCRIPT_DIR/results/$(date +%Y%m%d_%H%M%S)}"
LOGCAT_DIR="$RESULTS_DIR/logcat"
PID_FILE="$RESULTS_DIR/.logcat_pid"

# Parse args
ACTION="${1:-status}"
shift || true
while [[ $# -gt 0 ]]; do
    case "$1" in
        --results-dir) RESULTS_DIR="$2"; LOGCAT_DIR="$RESULTS_DIR/logcat"; PID_FILE="$RESULTS_DIR/.logcat_pid"; shift 2 ;;
        *) shift ;;
    esac
done
mkdir -p "$LOGCAT_DIR"

LOGCAT_FILE="$LOGCAT_DIR/logcat_full.log"
CRASH_LOG="$LOGCAT_DIR/crashes_live.log"

start_capture() {
    # Kill any existing logcat capture
    if [[ -f "$PID_FILE" ]]; then
        local old_pid
        old_pid=$(cat "$PID_FILE")
        kill "$old_pid" 2>/dev/null || true
        rm -f "$PID_FILE"
    fi

    # Clear device logcat buffer
    adb logcat -c 2>/dev/null || true

    # Start logcat with relevant tag filters
    # Full verbose log for the app, errors for system components
    adb logcat \
        -v threadtime \
        ObsidianBackup:V \
        BackupEngine:V \
        CloudSync:V \
        RootDetector:V \
        ShellExecutor:V \
        Hilt:E \
        AndroidRuntime:E \
        ActivityManager:W \
        ActivityTaskManager:W \
        WindowManager:W \
        System.err:W \
        STRESS_TEST:V \
        '*:S' \
        > "$LOGCAT_FILE" 2>&1 &
    local pid=$!
    echo "$pid" > "$PID_FILE"

    # Also start a crash watcher in background
    (
        tail -f "$LOGCAT_FILE" 2>/dev/null | while IFS= read -r line; do
            if echo "$line" | grep -qE "FATAL EXCEPTION|ANR in|CRASH|java\.lang\.\w+Exception"; then
                echo "[$(date +%H:%M:%S)] $line" >> "$CRASH_LOG"
            fi
        done
    ) &
    local watcher_pid=$!
    echo "$watcher_pid" >> "$PID_FILE"

    echo "Logcat capture started (PID: $pid, watcher: $watcher_pid)"
    echo "  Full log:   $LOGCAT_FILE"
    echo "  Crash log:  $CRASH_LOG"
}

stop_capture() {
    if [[ -f "$PID_FILE" ]]; then
        while IFS= read -r pid; do
            kill "$pid" 2>/dev/null || true
        done < "$PID_FILE"
        rm -f "$PID_FILE"
        echo "Logcat capture stopped"

        # Print crash summary
        if [[ -f "$CRASH_LOG" ]]; then
            local crash_count
            crash_count=$(grep -c "FATAL EXCEPTION" "$CRASH_LOG" 2>/dev/null || echo "0")
            local anr_count
            anr_count=$(grep -c "ANR in" "$CRASH_LOG" 2>/dev/null || echo "0")
            echo "  Crashes detected: $crash_count"
            echo "  ANRs detected: $anr_count"
        fi
    else
        echo "No active logcat capture found"
    fi
}

show_status() {
    if [[ -f "$PID_FILE" ]]; then
        local pid
        pid=$(head -1 "$PID_FILE")
        if kill -0 "$pid" 2>/dev/null; then
            echo "Logcat capture is RUNNING (PID: $pid)"
            if [[ -f "$LOGCAT_FILE" ]]; then
                local lines
                lines=$(wc -l < "$LOGCAT_FILE")
                local size
                size=$(du -h "$LOGCAT_FILE" | cut -f1)
                echo "  Lines captured: $lines"
                echo "  Log size: $size"
            fi
        else
            echo "Logcat capture PID exists but process is dead"
            rm -f "$PID_FILE"
        fi
    else
        echo "Logcat capture is NOT running"
    fi
}

case "$ACTION" in
    start)  start_capture ;;
    stop)   stop_capture ;;
    status) show_status ;;
    *)      echo "Usage: $0 {start|stop|status} [--results-dir DIR]"; exit 1 ;;
esac
