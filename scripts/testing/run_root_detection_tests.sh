#!/bin/bash
# Root Detection Validation Test Runner
# This script runs the comprehensive root detection validation tests

set -e

echo "========================================="
echo "Root Detection Validation Test Suite"
echo "========================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Navigate to project root
cd "$(dirname "$0")"

echo "📋 Test Categories:"
echo "  1. True Positive Tests - Should detect root"
echo "  2. False Positive Tests - Should NOT detect root"
echo "  3. Edge Case Tests - Tricky scenarios"
echo "  4. Confidence Level Tests - Scoring validation"
echo "  5. Quick Check Tests - Fast synchronous checks"
echo ""

# Check for options
RUN_ALL=true
RUN_CATEGORY=""

if [ "$1" == "--help" ] || [ "$1" == "-h" ]; then
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --true-positive    Run only true positive tests"
    echo "  --false-positive   Run only false positive tests"
    echo "  --edge-case        Run only edge case tests"
    echo "  --confidence       Run only confidence level tests"
    echo "  --quick            Run only quick check tests"
    echo "  --all              Run all tests (default)"
    echo "  --help, -h         Show this help message"
    echo ""
    exit 0
fi

# Parse arguments
case "$1" in
    --true-positive)
        RUN_ALL=false
        RUN_CATEGORY="TruePositiveTests"
        ;;
    --false-positive)
        RUN_ALL=false
        RUN_CATEGORY="FalsePositiveTests"
        ;;
    --edge-case)
        RUN_ALL=false
        RUN_CATEGORY="EdgeCaseTests"
        ;;
    --confidence)
        RUN_ALL=false
        RUN_CATEGORY="ConfidenceLevelTests"
        ;;
    --quick)
        RUN_ALL=false
        RUN_CATEGORY="QuickCheckTests"
        ;;
esac

# Build test command
if [ "$RUN_ALL" = true ]; then
    echo "🚀 Running ALL validation tests..."
    TEST_CLASS="com.obsidianbackup.security.RootDetectionValidationTest"
else
    echo "🚀 Running $RUN_CATEGORY tests..."
    TEST_CLASS="com.obsidianbackup.security.RootDetectionValidationTest.$RUN_CATEGORY"
fi

echo ""
echo "⏳ Executing tests (this may take a few minutes)..."
echo ""

# Run the tests
./gradlew :app:testFreeDebugUnitTest \
    --tests "$TEST_CLASS" \
    --console=plain \
    --no-daemon

# Check exit code
EXIT_CODE=$?

echo ""
echo "========================================="
if [ $EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✅ Tests PASSED${NC}"
    echo ""
    echo "📊 View detailed report:"
    echo "  file://$(pwd)/app/build/reports/tests/testFreeDebugUnitTest/index.html"
else
    echo -e "${RED}❌ Tests FAILED${NC}"
    echo ""
    echo "Please review the errors above and check:"
    echo "  1. Are there new false positives?"
    echo "  2. Are there missed root indicators?"
    echo "  3. Is the confidence scoring correct?"
fi
echo "========================================="
echo ""

# Show coverage info if available
if [ -f "app/build/reports/jacoco/jacocoTestReport/html/index.html" ]; then
    echo "📈 Code coverage report available at:"
    echo "  file://$(pwd)/app/build/reports/jacoco/jacocoTestReport/html/index.html"
    echo ""
fi

exit $EXIT_CODE
