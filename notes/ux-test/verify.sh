#!/bin/bash
# Runtime verification runner (post-build): bootprobe 3x + pass12 + errorprobe
# Usage: bash /tmp/opencode/ux-test/verify.sh
set -u
cd /tmp/opencode/ohaa-kmp || { echo "WORKTREE MISSING"; exit 1; }
KEY="sk-oh-NflvnKrPQ7COIiXIWKQxAAgsE8zCnZo2"
DIST="web/build/dist/wasmJs/productionExecutable"

healthy() { curl -s -o /dev/null --max-time 3 "http://localhost:$1/health"; }

start_server() {
  local port=$1 key=$2 logfile=/tmp/opencode/server-$1.log
  if healthy "$port"; then echo "[server:$port] already healthy"; return 0; fi
  echo "[server:$port] starting..."
  if [ -n "$key" ]; then
    setsid env PORT="$port" OPENHANDS_API_KEY="$key" WEB_ROOT="$DIST" \
      nohup server/build/install/server/bin/server > "$logfile" 2>&1 < /dev/null &
  else
    setsid env PORT="$port" WEB_ROOT="$DIST" \
      nohup server/build/install/server/bin/server > "$logfile" 2>&1 < /dev/null &
  fi
  for i in $(seq 1 20); do
    healthy "$port" && { echo "[server:$port] healthy after ${i}s"; return 0; }
    sleep 1
  done
  echo "[server:$port] FAILED to become healthy — tail of log:"; tail -5 "$logfile"; return 1
}

echo "════════ 1. servers ════════"
start_server 12001 "$KEY" || exit 1
start_server 12002 "" || exit 1

echo "════════ 2. bootprobe 3x ════════"
cd /tmp/opencode/ux-test
RUNS=3 node bootprobe.mjs 2>&1 | tail -15

echo "════════ 3. pass12 full chain ════════"
node pass12.mjs 2>&1 | tail -80

echo "════════ 4. errorprobe (unkeyed 12002) ════════"
node errorprobe.mjs 2>&1 | tail -40

echo "════════ DONE ════════"
ls /tmp/opencode/ux-test/shots12 | wc -l | xargs echo "shots12 count:"
