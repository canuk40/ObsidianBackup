#!/bin/bash

echo "==================================="
echo "DefaultAutomationPlugin Verification"
echo "==================================="
echo ""

# Check file exists
echo "1. Checking main plugin file..."
if [ -f "app/src/main/java/com/obsidianbackup/plugins/builtin/DefaultAutomationPlugin.kt" ]; then
    echo "   ✓ DefaultAutomationPlugin.kt exists"
    lines=$(wc -l < app/src/main/java/com/obsidianbackup/plugins/builtin/DefaultAutomationPlugin.kt)
    echo "   ✓ $lines lines of code"
else
    echo "   ✗ DefaultAutomationPlugin.kt missing"
    exit 1
fi

echo ""
echo "2. Checking implementation completeness..."

# Check key methods exist
grep -q "fun getAvailableTriggers" app/src/main/java/com/obsidianbackup/plugins/builtin/DefaultAutomationPlugin.kt && echo "   ✓ getAvailableTriggers()" || echo "   ✗ Missing getAvailableTriggers()"
grep -q "fun registerTrigger" app/src/main/java/com/obsidianbackup/plugins/builtin/DefaultAutomationPlugin.kt && echo "   ✓ registerTrigger()" || echo "   ✗ Missing registerTrigger()"
grep -q "fun unregisterTrigger" app/src/main/java/com/obsidianbackup/plugins/builtin/DefaultAutomationPlugin.kt && echo "   ✓ unregisterTrigger()" || echo "   ✗ Missing unregisterTrigger()"
grep -q "fun executeAction" app/src/main/java/com/obsidianbackup/plugins/builtin/DefaultAutomationPlugin.kt && echo "   ✓ executeAction()" || echo "   ✗ Missing executeAction()"
grep -q "fun observeTriggerEvents" app/src/main/java/com/obsidianbackup/plugins/builtin/DefaultAutomationPlugin.kt && echo "   ✓ observeTriggerEvents()" || echo "   ✗ Missing observeTriggerEvents()"

echo ""
echo "3. Checking automation workflows..."
grep -q "scheduleNightlyBackup" app/src/main/java/com/obsidianbackup/plugins/builtin/DefaultAutomationPlugin.kt && echo "   ✓ Nightly backup" || echo "   ✗ Missing nightly backup"
grep -q "scheduleWeeklyBackup" app/src/main/java/com/obsidianbackup/plugins/builtin/DefaultAutomationPlugin.kt && echo "   ✓ Weekly backup" || echo "   ✗ Missing weekly backup"
grep -q "scheduleOnChargeBackup" app/src/main/java/com/obsidianbackup/plugins/builtin/DefaultAutomationPlugin.kt && echo "   ✓ On-charge backup" || echo "   ✗ Missing on-charge backup"
grep -q "scheduleOnWifiBackup" app/src/main/java/com/obsidianbackup/plugins/builtin/DefaultAutomationPlugin.kt && echo "   ✓ On-WiFi backup" || echo "   ✗ Missing on-WiFi backup"

echo ""
echo "4. Checking condition checking..."
grep -q "checkBatteryLevel" app/src/main/java/com/obsidianbackup/plugins/builtin/DefaultAutomationPlugin.kt && echo "   ✓ Battery level check" || echo "   ✗ Missing battery check"
grep -q "checkStorageSpace" app/src/main/java/com/obsidianbackup/plugins/builtin/DefaultAutomationPlugin.kt && echo "   ✓ Storage space check" || echo "   ✗ Missing storage check"
grep -q "checkWifiConnected" app/src/main/java/com/obsidianbackup/plugins/builtin/DefaultAutomationPlugin.kt && echo "   ✓ WiFi connection check" || echo "   ✗ Missing WiFi check"

echo ""
echo "5. Checking WorkManager integration..."
grep -q "class AutomationBackupWorker" app/src/main/java/com/obsidianbackup/plugins/builtin/DefaultAutomationPlugin.kt && echo "   ✓ AutomationBackupWorker" || echo "   ✗ Missing worker"
grep -q "PeriodicWorkRequestBuilder" app/src/main/java/com/obsidianbackup/plugins/builtin/DefaultAutomationPlugin.kt && echo "   ✓ Periodic work requests" || echo "   ✗ Missing periodic work"
grep -q "BackoffPolicy.EXPONENTIAL" app/src/main/java/com/obsidianbackup/plugins/builtin/DefaultAutomationPlugin.kt && echo "   ✓ Exponential backoff" || echo "   ✗ Missing backoff policy"

echo ""
echo "6. Checking DI integration..."
if grep -q "provideDefaultAutomationPlugin" app/src/main/java/com/obsidianbackup/di/AppModule.kt; then
    echo "   ✓ DI provider in AppModule"
else
    echo "   ✗ Missing DI provider"
fi

echo ""
echo "7. Checking plugin registration..."
if grep -q "DefaultAutomationPlugin" app/src/main/java/com/obsidianbackup/ObsidianBackupApplication.kt; then
    echo "   ✓ Registered in Application"
else
    echo "   ✗ Not registered in Application"
fi

echo ""
echo "8. Checking PluginType enum..."
if grep -q "enum class PluginType" app/src/main/java/com/obsidianbackup/plugins/core/PluginRegistry.kt; then
    echo "   ✓ PluginType enum defined"
else
    echo "   ✗ Missing PluginType enum"
fi

echo ""
echo "9. Checking documentation..."
if [ -f "app/src/main/java/com/obsidianbackup/plugins/builtin/README_AUTOMATION.md" ]; then
    echo "   ✓ Plugin documentation exists"
else
    echo "   ✗ Missing documentation"
fi

if [ -f "AUTOMATION_PLUGIN_SUMMARY.md" ]; then
    echo "   ✓ Implementation summary exists"
else
    echo "   ✗ Missing implementation summary"
fi

if [ -f "app/src/main/java/com/obsidianbackup/plugins/builtin/AutomationPluginExamples.kt" ]; then
    echo "   ✓ Usage examples exist"
else
    echo "   ✗ Missing usage examples"
fi

echo ""
echo "==================================="
echo "Verification Complete!"
echo "==================================="
