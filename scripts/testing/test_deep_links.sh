#!/bin/bash

# Deep Link Testing Script for ObsidianBackup
# This script helps test various deep linking scenarios

set -e

PACKAGE="com.obsidianbackup"
ACTIVITY=".deeplink.DeepLinkActivity"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}ObsidianBackup Deep Link Test Suite${NC}"
echo -e "${BLUE}========================================${NC}\n"

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo -e "${RED}Error: No Android device connected${NC}"
    echo "Please connect a device or start an emulator"
    exit 1
fi

# Check if app is installed
if ! adb shell pm list packages | grep -q "$PACKAGE"; then
    echo -e "${RED}Error: ObsidianBackup is not installed${NC}"
    echo "Please install the app first"
    exit 1
fi

echo -e "${GREEN}✓ Device connected${NC}"
echo -e "${GREEN}✓ App installed${NC}\n"

# Function to test a deep link
test_link() {
    local uri="$1"
    local description="$2"
    
    echo -e "${YELLOW}Testing:${NC} $description"
    echo -e "${BLUE}URI:${NC} $uri"
    
    if adb shell am start -a android.intent.action.VIEW -d "$uri" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Success${NC}\n"
        sleep 2
    else
        echo -e "${RED}✗ Failed${NC}\n"
    fi
}

# Function to display menu
show_menu() {
    echo -e "${BLUE}Select test category:${NC}"
    echo "1) Backup Operations"
    echo "2) Restore Operations"
    echo "3) Navigation"
    echo "4) Settings"
    echo "5) Cloud Operations"
    echo "6) App Management"
    echo "7) HTTPS App Links"
    echo "8) Run All Tests"
    echo "9) Open Test Activity"
    echo "0) Exit"
    echo ""
    read -p "Enter choice: " choice
}

# Backup tests
test_backup() {
    echo -e "\n${BLUE}=== Backup Operations ===${NC}\n"
    
    test_link "obsidianbackup://backup" \
        "Start full backup"
    
    test_link "obsidianbackup://backup?packages=com.android.chrome" \
        "Backup Chrome app only"
    
    test_link "obsidianbackup://backup?packages=com.android.chrome,com.android.vending&includeData=true&includeApk=false" \
        "Backup Chrome and Play Store (data only)"
}

# Restore tests
test_restore() {
    echo -e "\n${BLUE}=== Restore Operations ===${NC}\n"
    echo -e "${YELLOW}Note: These require authentication${NC}\n"
    
    test_link "obsidianbackup://restore?snapshot=test_snapshot_123" \
        "Restore snapshot"
    
    test_link "obsidianbackup://restore?snapshot=test_snapshot_123&packages=com.android.chrome" \
        "Restore specific app from snapshot"
}

# Navigation tests
test_navigation() {
    echo -e "\n${BLUE}=== Navigation ===${NC}\n"
    
    test_link "obsidianbackup://dashboard" \
        "Open dashboard"
    
    test_link "obsidianbackup://backups" \
        "Open backups screen"
    
    test_link "obsidianbackup://automation" \
        "Open automation screen"
    
    test_link "obsidianbackup://logs" \
        "Open logs screen"
}

# Settings tests
test_settings() {
    echo -e "\n${BLUE}=== Settings ===${NC}\n"
    
    test_link "obsidianbackup://settings" \
        "Open main settings"
    
    test_link "obsidianbackup://settings/automation" \
        "Open automation settings"
    
    test_link "obsidianbackup://settings/cloud" \
        "Open cloud settings"
    
    test_link "obsidianbackup://settings/storage" \
        "Open storage settings"
    
    test_link "obsidianbackup://settings/security" \
        "Open security settings (requires auth)"
    
    test_link "obsidianbackup://settings/about" \
        "Open about screen"
}

# Cloud tests
test_cloud() {
    echo -e "\n${BLUE}=== Cloud Operations ===${NC}\n"
    echo -e "${YELLOW}Note: These require authentication${NC}\n"
    
    test_link "obsidianbackup://cloud/settings" \
        "Open cloud settings"
    
    test_link "obsidianbackup://cloud/connect?provider=webdav" \
        "Connect WebDAV provider"
    
    test_link "obsidianbackup://cloud/connect?provider=nextcloud&autoConnect=false" \
        "Connect Nextcloud provider"
}

# App management tests
test_app() {
    echo -e "\n${BLUE}=== App Management ===${NC}\n"
    
    test_link "obsidianbackup://app?package=com.android.chrome" \
        "Open Chrome app details"
    
    test_link "obsidianbackup://app?package=com.android.vending" \
        "Open Play Store app details"
}

# HTTPS App Links tests
test_https() {
    echo -e "\n${BLUE}=== HTTPS App Links ===${NC}\n"
    echo -e "${YELLOW}Note: Requires assetlinks.json verification${NC}\n"
    
    test_link "https://obsidianbackup.app/backup" \
        "App Link - Start backup"
    
    test_link "https://obsidianbackup.app/settings/automation" \
        "App Link - Automation settings"
    
    test_link "https://obsidianbackup.app/cloud/connect?provider=webdav" \
        "App Link - Connect WebDAV"
}

# Open test activity
open_test_activity() {
    echo -e "\n${BLUE}Opening Deep Link Test Activity...${NC}\n"
    adb shell am start -n "${PACKAGE}/.deeplink.DeepLinkTestActivity"
}

# Run all tests
run_all_tests() {
    test_backup
    sleep 1
    test_restore
    sleep 1
    test_navigation
    sleep 1
    test_settings
    sleep 1
    test_cloud
    sleep 1
    test_app
    sleep 1
    test_https
}

# Check verification status
check_verification() {
    echo -e "\n${BLUE}=== App Links Verification Status ===${NC}\n"
    adb shell pm get-app-links "$PACKAGE"
}

# Main loop
while true; do
    show_menu
    
    case $choice in
        1) test_backup ;;
        2) test_restore ;;
        3) test_navigation ;;
        4) test_settings ;;
        5) test_cloud ;;
        6) test_app ;;
        7) test_https ;;
        8) run_all_tests ;;
        9) open_test_activity ;;
        0) echo -e "\n${GREEN}Goodbye!${NC}\n"; exit 0 ;;
        *) echo -e "${RED}Invalid choice${NC}\n" ;;
    esac
    
    echo ""
    read -p "Press Enter to continue..."
    echo ""
done
