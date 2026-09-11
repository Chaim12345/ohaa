#!/usr/bin/env bash
# Launch smoke test: install the fresh debug APK, start the launcher activity,
# verify the process is still alive after 20s. On death, dump the crash buffer
# and the relevant logcat lines to stdout so the failure is diagnosable from
# the step log alone (artifact uploads may fail on storage quota).
set -u

APK=app/build/outputs/apk/debug/app-debug.apk
PKG=com.aistudio.openhandscloud.nxvtq
ACTIVITY=com.example.MainActivity

adb install -t -r "$APK" || exit 1
adb logcat -c
adb shell am start -W -n "$PKG/$ACTIVITY" || true
sleep 20

PID=$(adb shell pidof "$PKG" | tr -d '[:space:]')
echo "App process after launch: '${PID}'"

adb logcat -d > full-logcat.txt || true
adb logcat -d -b crash > crash-logcat.txt || true

if [ -z "$PID" ]; then
  echo "---- CRASH BUFFER (adb logcat -b crash) ----"
  cat crash-logcat.txt
  echo ""
  echo "---- FATAL EXCEPTION / AndroidRuntime lines (main buffer) ----"
  grep -E "FATAL EXCEPTION|AndroidRuntime|Process: $PKG" full-logcat.txt | head -120 || true
  echo ""
  echo "---- Last 80 lines of main buffer ----"
  tail -80 full-logcat.txt
  echo "::error::App process died within 20s of launch"
  exit 1
fi

echo "APP ALIVE — smoke test passed"
