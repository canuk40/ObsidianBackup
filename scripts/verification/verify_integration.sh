#!/bin/bash

# Final Integration Verification Script

echo "================================"
echo "ObsidianBackup Integration Check"
echo "================================"
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $1"
        return 0
    else
        echo -e "${RED}✗${NC} $1 (MISSING)"
        return 1
    fi
}

missing=0

echo "Checking Hilt Modules..."
check_file "app/src/main/java/com/obsidianbackup/di/GamingModule.kt" || ((missing++))
check_file "app/src/main/java/com/obsidianbackup/di/HealthModule.kt" || ((missing++))
check_file "app/src/main/java/com/obsidianbackup/di/MLModule.kt" || ((missing++))
check_file "app/src/main/java/com/obsidianbackup/di/TaskerModule.kt" || ((missing++))
echo ""

echo "Checking ViewModels..."
check_file "app/src/main/java/com/obsidianbackup/presentation/gaming/GamingViewModel.kt" || ((missing++))
check_file "app/src/main/java/com/obsidianbackup/presentation/health/HealthViewModel.kt" || ((missing++))
check_file "app/src/main/java/com/obsidianbackup/presentation/plugins/PluginsViewModel.kt" || ((missing++))
echo ""

echo "Checking UI Screens..."
check_file "app/src/main/java/com/obsidianbackup/ui/screens/GamingScreen.kt" || ((missing++))
check_file "app/src/main/java/com/obsidianbackup/ui/screens/HealthScreen.kt" || ((missing++))
check_file "app/src/main/java/com/obsidianbackup/ui/screens/PluginsScreen.kt" || ((missing++))
echo ""

echo "Checking Other Components..."
check_file "app/src/main/java/com/obsidianbackup/ui/onboarding/OnboardingFlow.kt" || ((missing++))
check_file "app/src/main/java/com/obsidianbackup/widget/BackupWidget.kt" || ((missing++))
check_file "app/src/androidTest/java/com/obsidianbackup/integration/IntegrationTest.kt" || ((missing++))
echo ""

echo "Checking Updated Files..."
check_file "app/src/main/java/com/obsidianbackup/features/FeatureFlags.kt" || ((missing++))
check_file "app/src/main/java/com/obsidianbackup/ui/Navigation.kt" || ((missing++))
check_file "app/src/main/java/com/obsidianbackup/ui/ObsidianBackupApp.kt" || ((missing++))
echo ""

echo "Checking Documentation..."
check_file "FINAL_INTEGRATION.md" || ((missing++))
check_file "INTEGRATION_QUICK_REFERENCE.md" || ((missing++))
echo ""

echo "================================"
if [ $missing -eq 0 ]; then
    echo -e "${GREEN}✓ All integration files present!${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. ./gradlew clean build"
    echo "  2. ./gradlew test"
    echo "  3. ./gradlew assembleDebug"
    exit 0
else
    echo -e "${RED}✗ $missing file(s) missing${NC}"
    exit 1
fi
