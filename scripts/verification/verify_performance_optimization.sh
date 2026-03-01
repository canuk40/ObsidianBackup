#!/bin/bash

echo "=========================================="
echo "PERFORMANCE OPTIMIZATION VERIFICATION"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $1"
        return 0
    else
        echo -e "${RED}✗${NC} $1"
        return 1
    fi
}

echo "1. Checking Performance Utility Files..."
echo "=========================================="
check_file "app/src/main/java/com/obsidianbackup/performance/BatteryOptimizationManager.kt"
check_file "app/src/main/java/com/obsidianbackup/performance/NetworkOptimizationManager.kt"
check_file "app/src/main/java/com/obsidianbackup/performance/MemoryOptimizationManager.kt"
check_file "app/src/main/java/com/obsidianbackup/performance/ImageOptimizationManager.kt"
check_file "app/src/main/java/com/obsidianbackup/performance/LazyListOptimizer.kt"
check_file "app/src/main/java/com/obsidianbackup/performance/PerformanceProfiler.kt"
check_file "app/src/main/java/com/obsidianbackup/performance/PerformanceConfig.kt"
echo ""

echo "2. Checking Optimized Database Files..."
echo "=========================================="
check_file "app/src/main/java/com/obsidianbackup/storage/BackupCatalog.kt"
check_file "app/src/main/java/com/obsidianbackup/storage/AppBackupDao.kt"
check_file "app/src/main/java/com/obsidianbackup/storage/AppBackupEntity.kt"
check_file "app/src/main/java/com/obsidianbackup/storage/migrations/DatabaseMigrations.kt"
echo ""

echo "3. Checking Optimized WorkManager Files..."
echo "=========================================="
check_file "app/src/main/java/com/obsidianbackup/work/WorkManagerScheduler.kt"
check_file "app/src/main/java/com/obsidianbackup/work/BackupWorker.kt"
echo ""

echo "4. Checking Optimized UI Files..."
echo "=========================================="
check_file "app/src/main/java/com/obsidianbackup/ui/screens/OptimizedAppsScreen.kt"
echo ""

echo "5. Checking Documentation Files..."
echo "=========================================="
check_file "PERFORMANCE_OPTIMIZATION.md"
check_file "PERFORMANCE_QUICK_REFERENCE.md"
check_file "PERFORMANCE_SUMMARY.txt"
echo ""

echo "6. Checking Build Configuration..."
echo "=========================================="
if grep -q "leakcanary" app/build.gradle.kts; then
    echo -e "${GREEN}✓${NC} LeakCanary dependency added"
else
    echo -e "${RED}✗${NC} LeakCanary dependency missing"
fi

if grep -q "coil-compose" app/build.gradle.kts; then
    echo -e "${GREEN}✓${NC} Coil dependency added"
else
    echo -e "${RED}✗${NC} Coil dependency missing"
fi

if grep -q "okhttp3" app/build.gradle.kts; then
    echo -e "${GREEN}✓${NC} OkHttp dependency added"
else
    echo -e "${RED}✗${NC} OkHttp dependency missing"
fi
echo ""

echo "7. Checking Database Optimizations..."
echo "=========================================="
if grep -q "Index(value = \[\"timestamp\"\]" app/src/main/java/com/obsidianbackup/storage/BackupCatalog.kt; then
    echo -e "${GREEN}✓${NC} Timestamp index added"
else
    echo -e "${RED}✗${NC} Timestamp index missing"
fi

if grep -q "WRITE_AHEAD_LOGGING" app/src/main/java/com/obsidianbackup/storage/BackupCatalog.kt; then
    echo -e "${GREEN}✓${NC} WAL mode enabled"
else
    echo -e "${RED}✗${NC} WAL mode not enabled"
fi

if grep -q "getSnapshotsPaged" app/src/main/java/com/obsidianbackup/storage/BackupCatalog.kt; then
    echo -e "${GREEN}✓${NC} Pagination support added"
else
    echo -e "${RED}✗${NC} Pagination support missing"
fi
echo ""

echo "8. File Statistics..."
echo "=========================================="
echo "Performance utilities: $(find app/src/main/java/com/obsidianbackup/performance -type f -name "*.kt" | wc -l) files"
echo "Documentation size: $(du -sh PERFORMANCE_OPTIMIZATION.md | cut -f1)"
echo "Quick reference size: $(du -sh PERFORMANCE_QUICK_REFERENCE.md | cut -f1)"
echo ""

echo "=========================================="
echo -e "${BLUE}VERIFICATION COMPLETE${NC}"
echo "=========================================="
