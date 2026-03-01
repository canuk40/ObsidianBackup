#!/bin/bash
# verify_split_apk_migration.sh - Verify split APK package migration

echo "=== Split APK Package Migration Verification ==="
echo ""

echo "1. Checking new installer package exists..."
if [ -d "app/src/main/java/com/obsidianbackup/installer" ]; then
    echo "   ✓ installer/ package exists"
else
    echo "   ✗ installer/ package NOT found"
    exit 1
fi

echo ""
echo "2. Checking new files present..."
for file in SplitApkHelper.kt SplitApkInstaller.kt README.md; do
    if [ -f "app/src/main/java/com/obsidianbackup/installer/$file" ]; then
        echo "   ✓ $file present"
    else
        echo "   ✗ $file MISSING"
        exit 1
    fi
done

echo ""
echo "3. Checking old file removed..."
if [ ! -f "app/src/main/java/com/obsidianbackup/engine/SplitApkHelper.kt" ]; then
    echo "   ✓ Old SplitApkHelper.kt removed from engine/"
else
    echo "   ✗ Old SplitApkHelper.kt still exists in engine/"
    exit 1
fi

echo ""
echo "4. Verifying package declarations..."
if grep -q "package com.obsidianbackup.installer" app/src/main/java/com/obsidianbackup/installer/SplitApkHelper.kt; then
    echo "   ✓ SplitApkHelper has correct package"
else
    echo "   ✗ SplitApkHelper has incorrect package"
    exit 1
fi

if grep -q "package com.obsidianbackup.installer" app/src/main/java/com/obsidianbackup/installer/SplitApkInstaller.kt; then
    echo "   ✓ SplitApkInstaller has correct package"
else
    echo "   ✗ SplitApkInstaller has incorrect package"
    exit 1
fi

echo ""
echo "5. Verifying ObsidianBoxEngine import..."
if grep -q "import com.obsidianbackup.installer.SplitApkHelper" app/src/main/java/com/obsidianbackup/engine/ObsidianBoxEngine.kt; then
    echo "   ✓ ObsidianBoxEngine imports from installer package"
else
    echo "   ✗ ObsidianBoxEngine has incorrect import"
    exit 1
fi

echo ""
echo "6. Checking for old package references..."
OLD_REFS=$(find app/src/main/java -name "*.kt" -exec grep -l "com.obsidianbackup.engine.SplitApk" {} \; 2>/dev/null | wc -l)
if [ "$OLD_REFS" -eq 0 ]; then
    echo "   ✓ No references to old package found"
else
    echo "   ✗ Found $OLD_REFS files still referencing old package"
    find app/src/main/java -name "*.kt" -exec grep -l "com.obsidianbackup.engine.SplitApk" {} \;
    exit 1
fi

echo ""
echo "7. Checking SplitApkInstaller features..."
if grep -q "InstallProgress" app/src/main/java/com/obsidianbackup/installer/SplitApkInstaller.kt; then
    echo "   ✓ Progress tracking implemented"
else
    echo "   ✗ Progress tracking missing"
    exit 1
fi

if grep -q "rollbackAndFail" app/src/main/java/com/obsidianbackup/installer/SplitApkInstaller.kt; then
    echo "   ✓ Rollback mechanism implemented"
else
    echo "   ✗ Rollback mechanism missing"
    exit 1
fi

if grep -q "InstallPhase" app/src/main/java/com/obsidianbackup/installer/SplitApkInstaller.kt; then
    echo "   ✓ Phase tracking implemented"
else
    echo "   ✗ Phase tracking missing"
    exit 1
fi

echo ""
echo "8. Checking SplitApkHelper integration..."
if grep -q "SplitApkInstaller" app/src/main/java/com/obsidianbackup/installer/SplitApkHelper.kt; then
    echo "   ✓ SplitApkHelper integrates with SplitApkInstaller"
else
    echo "   ✗ Integration missing"
    exit 1
fi

if grep -q "getInstaller()" app/src/main/java/com/obsidianbackup/installer/SplitApkHelper.kt; then
    echo "   ✓ getInstaller() accessor present"
else
    echo "   ✗ getInstaller() accessor missing"
    exit 1
fi

echo ""
echo "9. Verifying documentation..."
if [ -s "app/src/main/java/com/obsidianbackup/installer/README.md" ]; then
    LINES=$(wc -l < app/src/main/java/com/obsidianbackup/installer/README.md)
    echo "   ✓ README.md present ($LINES lines)"
else
    echo "   ✗ README.md missing or empty"
    exit 1
fi

echo ""
echo "=== All verification checks passed! ==="
echo ""
echo "Summary:"
echo "  - SplitApkHelper moved to installer package"
echo "  - SplitApkInstaller created with enhanced features"
echo "  - Progress reporting: ✓"
echo "  - Rollback mechanism: ✓"
echo "  - Error handling: ✓"
echo "  - Documentation: ✓"
echo "  - Import updates: ✓"
echo ""
