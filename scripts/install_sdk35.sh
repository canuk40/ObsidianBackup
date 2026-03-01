#!/bin/bash

# SDK 35 Installation Script for ObsidianBackup
# This script helps install Android SDK 35 required for Google Play submission

set -e

echo "════════════════════════════════════════════════════════════════"
echo "  ObsidianBackup - Android SDK 35 Installation Helper"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "⚠️  IMPORTANT: Google Play requires SDK 35 as of February 2026"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Detect Android SDK location
detect_sdk_location() {
    if [ -n "$ANDROID_HOME" ]; then
        echo "$ANDROID_HOME"
    elif [ -n "$ANDROID_SDK_ROOT" ]; then
        echo "$ANDROID_SDK_ROOT"
    elif [ -d "$HOME/Android/Sdk" ]; then
        echo "$HOME/Android/Sdk"
    elif [ -d "/usr/lib/android-sdk" ]; then
        echo "/usr/lib/android-sdk"
    elif [ -d "$HOME/Library/Android/sdk" ]; then
        echo "$HOME/Library/Android/sdk"  # macOS
    else
        echo ""
    fi
}

SDK_PATH=$(detect_sdk_location)

if [ -z "$SDK_PATH" ]; then
    echo -e "${RED}❌ Android SDK not found!${NC}"
    echo ""
    echo "Please install Android SDK first:"
    echo ""
    echo "Option 1: Install Android Studio (recommended)"
    echo "  Download from: https://developer.android.com/studio"
    echo ""
    echo "Option 2: Install command-line tools"
    echo "  Download from: https://developer.android.com/studio#command-tools"
    echo ""
    echo "After installation, set ANDROID_HOME:"
    echo "  export ANDROID_HOME=/path/to/android/sdk"
    echo ""
    exit 1
fi

echo -e "${GREEN}✓ Android SDK found: $SDK_PATH${NC}"
echo ""

# Check for sdkmanager
SDKMANAGER="$SDK_PATH/cmdline-tools/latest/bin/sdkmanager"
if [ ! -f "$SDKMANAGER" ]; then
    # Try alternative locations
    SDKMANAGER="$SDK_PATH/tools/bin/sdkmanager"
    if [ ! -f "$SDKMANAGER" ]; then
        SDKMANAGER="$SDK_PATH/cmdline-tools/bin/sdkmanager"
        if [ ! -f "$SDKMANAGER" ]; then
            echo -e "${RED}❌ sdkmanager not found!${NC}"
            echo ""
            echo "Please install Android SDK Command-line Tools:"
            echo "  1. Open Android Studio"
            echo "  2. Go to Tools → SDK Manager"
            echo "  3. Select 'SDK Tools' tab"
            echo "  4. Check 'Android SDK Command-line Tools'"
            echo "  5. Click Apply"
            echo ""
            exit 1
        fi
    fi
fi

echo -e "${GREEN}✓ sdkmanager found: $SDKMANAGER${NC}"
echo ""

# Check if SDK 35 is already installed
echo "Checking for SDK 35..."
if [ -d "$SDK_PATH/platforms/android-35" ]; then
    echo -e "${GREEN}✓ SDK 35 is already installed!${NC}"
    echo ""
    echo "Location: $SDK_PATH/platforms/android-35"
    echo ""
else
    echo -e "${YELLOW}⚠  SDK 35 not found. Installing...${NC}"
    echo ""

    # Install SDK 35
    echo "Installing Android SDK Platform 35..."
    yes | $SDKMANAGER "platforms;android-35" || {
        echo -e "${RED}❌ Failed to install SDK 35 platform${NC}"
        exit 1
    }
    echo -e "${GREEN}✓ SDK 35 platform installed${NC}"
    echo ""
fi

# Check build tools
echo "Checking for Build Tools 35.0.0..."
if [ -d "$SDK_PATH/build-tools/35.0.0" ]; then
    echo -e "${GREEN}✓ Build Tools 35.0.0 already installed!${NC}"
    echo ""
else
    echo -e "${YELLOW}⚠  Build Tools 35.0.0 not found. Installing...${NC}"
    echo ""

    # Install Build Tools
    echo "Installing Android SDK Build Tools 35.0.0..."
    yes | $SDKMANAGER "build-tools;35.0.0" || {
        echo -e "${RED}❌ Failed to install Build Tools 35.0.0${NC}"
        exit 1
    }
    echo -e "${GREEN}✓ Build Tools 35.0.0 installed${NC}"
    echo ""
fi

# Verify installation
echo "════════════════════════════════════════════════════════════════"
echo "  Verification"
echo "════════════════════════════════════════════════════════════════"
echo ""

if [ -d "$SDK_PATH/platforms/android-35" ]; then
    echo -e "${GREEN}✓ SDK 35 Platform: INSTALLED${NC}"
else
    echo -e "${RED}✗ SDK 35 Platform: NOT FOUND${NC}"
fi

if [ -d "$SDK_PATH/build-tools/35.0.0" ]; then
    echo -e "${GREEN}✓ Build Tools 35.0.0: INSTALLED${NC}"
else
    echo -e "${RED}✗ Build Tools 35.0.0: NOT FOUND${NC}"
fi

echo ""
echo "════════════════════════════════════════════════════════════════"
echo "  Next Steps"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "1. Restart Android Studio (if open)"
echo "2. Sync Gradle: File → Sync Project with Gradle Files"
echo "3. Build the project: ./gradlew assembleFreeDebug"
echo ""
echo "If Gradle sync fails:"
echo "  • Stop daemon: ./gradlew --stop"
echo "  • Clean: ./gradlew clean"
echo "  • Invalidate caches: File → Invalidate Caches / Restart"
echo ""
echo -e "${GREEN}✓ SDK 35 setup complete!${NC}"
echo ""

