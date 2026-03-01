#!/bin/bash
# ObsidianBackup Rooted Emulator Launcher
# Launch Android 14 emulator with root access for testing

export ANDROID_HOME=/usr/lib/android-sdk
export PATH=$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH

echo "🚀 Starting ObsidianBackup Rooted Test Emulator..."
echo ""
echo "📱 Device: Pixel 6 Pro"
echo "🤖 Android: 14 (API 34)"
echo "🔓 Root: Enabled (via adb root)"
echo "💾 Storage: 8GB"
echo "🧠 RAM: 4GB"
echo ""

# Launch emulator in headless mode (no GUI - for server environments)
# Remove -no-window if you want to see the emulator UI
$ANDROID_HOME/emulator/emulator \
    -avd ObsidianBackup_Test_Rooted \
    -no-window \
    -no-audio \
    -no-boot-anim \
    -gpu swiftshader_indirect \
    -writable-system \
    -qemu -enable-kvm &

EMULATOR_PID=$!
echo "✅ Emulator started (PID: $EMULATOR_PID)"
echo ""
echo "⏳ Waiting for emulator to boot (this takes 2-3 minutes)..."

# Wait for emulator to be fully booted
$ANDROID_HOME/platform-tools/adb wait-for-device
echo "✅ Device connected"

# Wait for boot to complete
while [ "`$ANDROID_HOME/platform-tools/adb shell getprop sys.boot_completed | tr -d '\r'`" != "1" ]; do
    echo "   Still booting..."
    sleep 5
done

echo ""
echo "✅ Emulator fully booted!"
echo ""

# Enable root access
echo "🔓 Enabling root access..."
$ANDROID_HOME/platform-tools/adb root
sleep 2
$ANDROID_HOME/platform-tools/adb wait-for-device
echo "✅ Root access enabled!"
echo ""

# Remount system as writable (for testing)
echo "💾 Remounting system as writable..."
$ANDROID_HOME/platform-tools/adb remount
echo ""

# Display device info
echo "📊 Device Information:"
echo "   Android Version: $($ANDROID_HOME/platform-tools/adb shell getprop ro.build.version.release)"
echo "   API Level: $($ANDROID_HOME/platform-tools/adb shell getprop ro.build.version.sdk)"
echo "   Device Name: $($ANDROID_HOME/platform-tools/adb shell getprop ro.product.model)"
echo "   Root Status: $($ANDROID_HOME/platform-tools/adb shell whoami)"
echo ""

echo "🎯 Emulator ready for testing!"
echo ""
echo "📝 Useful commands:"
echo "   - Install APK:       adb install app/build/outputs/apk/free/debug/*.apk"
echo "   - View logs:         adb logcat | grep ObsidianBackup"
echo "   - Open shell:        adb shell"
echo "   - Stop emulator:     adb emu kill"
echo ""
echo "🔥 Emulator running in background (PID: $EMULATOR_PID)"
echo "   To stop: kill $EMULATOR_PID or adb emu kill"
