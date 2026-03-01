#!/bin/bash
set -e
echo "Running Instrumentation Tests"
./gradlew connectedDebugAndroidTest --stacktrace
