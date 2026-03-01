#!/usr/bin/env bash
# Launch the Magisk-rooted emulator for ObsidianBackup
# Usage: ./launch-emulator.sh [--wait] [--kill]

set -euo pipefail

AVD_NAME="obsidianbackup_rooted"
EMULATOR="$HOME/Android/Sdk/emulator/emulator"

case "${1:-}" in
  --kill)
    PIDS=$(pgrep -f "emulator.*$AVD_NAME" 2>/dev/null || true)
    if [ -n "$PIDS" ]; then
      echo "Killing emulator PIDs: $PIDS"
      echo "$PIDS" | xargs kill 2>/dev/null
    else
      echo "No running emulator found for $AVD_NAME"
    fi
    exit 0
    ;;
esac

echo "Starting $AVD_NAME (cold boot, no snapshots)..."
"$EMULATOR" -avd "$AVD_NAME" \
  -no-window -no-audio -no-boot-anim \
  -no-snapshot-load \
  -gpu swiftshader_indirect &
EMUPID=$!
echo "Emulator PID: $EMUPID"

if [ "${1:-}" = "--wait" ]; then
  echo "Waiting for ADB device..."
  adb wait-for-device
  echo "Waiting for boot to complete (up to 3 minutes)..."
  timeout 180 bash -c \
    'while [ "$(adb -s emulator-5554 shell getprop sys.boot_completed 2>/dev/null)" != "1" ]; do sleep 5; done'
  echo "Boot complete!"
  echo "Verifying Magisk su..."
  adb -s emulator-5554 shell "su -c id"
fi
