#!/bin/bash
# Test script for Tasker/MacroDroid integration
# Run with: adb shell < test_tasker_integration.sh

echo "=== ObsidianBackup Tasker Integration Test ==="
echo ""

# Test 1: Start Backup
echo "Test 1: Triggering backup via intent..."
am broadcast \
  -a com.obsidianbackup.tasker.ACTION_START_BACKUP \
  --esa package_list "com.example.testapp1,com.example.testapp2" \
  --esa backup_components "APK,DATA" \
  --ei compression_level 9 \
  --ez incremental true \
  --es description "Test backup from shell" \
  --es calling_package "com.obsidianbackup"

echo ""
sleep 2

# Test 2: Query Status (requires work_id from previous step)
echo "Test 2: Querying backup status..."
# Note: Replace WORK_ID with actual UUID from step 1 response
# am broadcast -a com.obsidianbackup.tasker.ACTION_QUERY_STATUS --es work_request_id "WORK_ID"

echo ""
sleep 1

# Test 3: Trigger Cloud Sync
echo "Test 3: Triggering cloud sync..."
am broadcast \
  -a com.obsidianbackup.tasker.ACTION_TRIGGER_CLOUD_SYNC \
  --es cloud_provider "default" \
  --es calling_package "com.obsidianbackup"

echo ""
sleep 2

# Test 4: Query via ContentProvider
echo "Test 4: Querying latest backup via ContentProvider..."
content query \
  --uri content://com.obsidianbackup.tasker/latest

echo ""
sleep 1

# Test 5: Verify Backup
echo "Test 5: Verifying backup..."
am broadcast \
  -a com.obsidianbackup.tasker.ACTION_VERIFY_BACKUP \
  --es snapshot_id "backup_20240215_123045" \
  --es calling_package "com.obsidianbackup"

echo ""
echo "=== Tests Complete ==="
echo "Check logcat for detailed results:"
echo "  adb logcat | grep TaskerIntegration"
