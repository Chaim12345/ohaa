#!/bin/bash
# Sandbox egress MTU fix — applied 2026-10-15.
# Symptom: TLS to ANY host stalls (google/github/app.all-hands.dev time out)
# while small HTTP works; app shows "Not connected" + empty data.
# Cause: path MTU dropped to 1260B; PMTUD blackhole kills larger packets.
# Re-apply after sandbox/network resets:  bash net-mtu-fix.sh
if ! ping -M do -c1 -W2 -s 1400 1.1.1.1 >/dev/null 2>&1; then
  ip link set dev eth0 mtu 1260 && echo "eth0 mtu lowered to 1260 (egress path-MTU fix applied)"
else
  echo "egress MTU looks fine (1400B DF ping succeeds) — no change"
fi
