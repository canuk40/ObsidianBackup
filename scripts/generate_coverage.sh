#!/bin/bash
set -e
echo "Generating Coverage Report"
./gradlew testDebugUnitTest jacocoTestReport --stacktrace
echo "Report: app/build/reports/jacoco/jacocoTestReport/html/index.html"
