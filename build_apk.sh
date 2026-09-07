#!/usr/bin/env bash
set -e

echo "=========================================="
echo " Building OpenHands Android APK "
echo "=========================================="

if command -v gradle &> /dev/null; then
    GRADLE_CMD="gradle"
elif [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    GRADLE_CMD="./gradlew"
else
    echo "Error: Neither 'gradle' nor './gradlew' was found."
    exit 1
fi

echo "Executing: $GRADLE_CMD assembleDebug"
$GRADLE_CMD assembleDebug --stacktrace

echo "=========================================="
echo " Build Completed Successfully! "
echo " APK location(s): "
find app/build/outputs/apk -name "*.apk" 2>/dev/null || true
echo "=========================================="
