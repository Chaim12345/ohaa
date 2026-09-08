#!/usr/bin/env bash
set -e

echo "=========================================="
echo " Building OpenHands Android APK "
echo "=========================================="

if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    GRADLE_CMD="./gradlew"
elif command -v gradle &> /dev/null; then
    GRADLE_CMD="gradle"
else
    echo "Error: Neither './gradlew' nor 'gradle' was found."
    exit 1
fi

echo "Executing: $GRADLE_CMD assembleDebug $@"
$GRADLE_CMD assembleDebug "$@" --stacktrace

echo "=========================================="
echo " Build Completed Successfully! "
echo " APK location(s): "
find app/build/outputs/apk -name "*.apk" 2>/dev/null || true
echo "=========================================="
