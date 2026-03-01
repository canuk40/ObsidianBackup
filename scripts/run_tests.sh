#!/bin/bash

set -e

echo "=========================================="
echo "Running ObsidianBackup Test Suite"
echo "=========================================="

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "\n${YELLOW}Running Unit Tests...${NC}"
./gradlew testDebugUnitTest --stacktrace
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Unit tests passed${NC}"
else
    echo -e "${RED}✗ Unit tests failed${NC}"
    exit 1
fi

echo -e "\n${YELLOW}Generating Coverage Report...${NC}"
./gradlew jacocoTestReport
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Coverage report generated${NC}"
fi

echo -e "\n${YELLOW}Running Lint Checks...${NC}"
./gradlew lintDebug --stacktrace

echo -e "\n${YELLOW}Running Static Analysis (Detekt)...${NC}"
./gradlew detekt --stacktrace

echo -e "\n${GREEN}Test Suite Completed!${NC}"
