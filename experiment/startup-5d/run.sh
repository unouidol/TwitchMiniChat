#!/usr/bin/env bash
# One cold start of the dev build, measured by StartupProbe. The run.sh of a091809, with two changes:
# the probe's files on the phone are deleted only after the copy was checked, in a step of its own
# (AGENTS.md, "A delete never shares a command with the read that justifies it"), and the calibration
# file is copied when the flag asks for one.
# Usage: run.sh <label> "<flag words>" [account id]
# Nothing touches the device between the launch and the dump except the two passive
# recorders started before it: logcat and getevent.
#
# The display's refresh rate is read before each launch and after it (refresh-rate.txt): the burst
# end counts frames, so a launch measured at another rate is not comparable, and decide.py and
# qualify5d.py void what does not share one reading. 2026-11-02.
#
# ADB and PYTHON name this machine's adb and Python 3, as found on PATH unless set: the round's
# machine is recorded in docs/manual-plan-5d-recyclerview.md.
set -u
ADB="${ADB:-adb}"
PYTHON="${PYTHON:-python}"
PKG=com.fs.twitchminichat.dev
OUT="$(dirname "$0")/runs/$1"
FLAG="$2"
ACCOUNT="${3:-}"
mkdir -p "$OUT"

state() {
  local wake key
  wake=$("$ADB" shell dumpsys power | grep -o "mWakefulness=[A-Za-z]*" | head -1 | tr -d '\r')
  key=$("$ADB" shell dumpsys window | grep -o "isKeyguardShowing=[a-z]*" | head -1 | tr -d '\r')
  echo "$wake $key"
}

# The active display mode's rate, any per-app frame rate override, and Choreographer's warning limit.
display() {
  local rate override limit
  rate=$("$ADB" shell dumpsys display | grep -o "mActiveSfDisplayMode=DisplayMode{[^}]*}" | grep -o "refreshRate=[0-9.]*" | head -1 | tr -d '\r')
  override=$("$ADB" shell dumpsys display | grep -o "frameRateOverride [^,]*," | head -1 | sed 's/^frameRateOverride //; s/,$//' | tr -d '\r')
  limit=$("$ADB" shell getprop debug.choreographer.skipwarning | tr -d '\r')
  echo "${rate:-refreshRate=unread} frameRateOverride=[$override] skipwarning=[$limit]"
}

echo "before: $(display)" > "$OUT/refresh-rate.txt"
echo "before: $(state)" > "$OUT/device-state.txt"
"$ADB" shell dumpsys battery | grep -E "temperature|level" | tr -d '\r' >> "$OUT/device-state.txt"
"$ADB" shell am force-stop $PKG
sleep 3
if [ "$FLAG" = "off" ]; then
  "$ADB" shell "run-as $PKG sh -c 'rm -f files/startup-probe/flag files/startup-probe/latest'"
else
  "$ADB" shell "run-as $PKG sh -c 'mkdir -p files/startup-probe && rm -f files/startup-probe/latest && echo \"$FLAG\" > files/startup-probe/flag'"
fi
"$ADB" logcat -c
"$ADB" logcat -v monotonic -v uid > "$OUT/logcat.txt" 2>&1 &
LOGCAT=$!
"$ADB" shell getevent -lt > "$OUT/getevent.txt" 2>&1 &
GETEVENT=$!
sleep 1
date +%s.%N > "$OUT/host-launch.txt"
if [ -n "$ACCOUNT" ]; then
  "$ADB" shell am start -W -n $PKG/com.fs.twitchminichat.MainActivity --es new_account_id "$ACCOUNT" > "$OUT/am-start.txt" 2>&1
else
  "$ADB" shell am start -W -n $PKG/com.fs.twitchminichat.MainActivity > "$OUT/am-start.txt" 2>&1
fi
# The window is 14 s from the anchor; the dump follows. Wait without asking the device anything.
sleep 19
if [ "$FLAG" = "off" ]; then
  sleep 3
  kill $LOGCAT $GETEVENT 2>/dev/null
  wait $LOGCAT $GETEVENT 2>/dev/null
  echo "after: $(state)" >> "$OUT/device-state.txt"
  echo "after: $(display)" >> "$OUT/refresh-rate.txt"
  echo "ok $1 (probe off): $(grep -c . "$OUT/logcat.txt") logcat lines | $(tr '\n' ' ' < "$OUT/refresh-rate.txt")"
  exit 0
fi
latest=""
for _ in 1 2 3 4 5 6 7 8 9 10; do
  latest=$("$ADB" shell "run-as $PKG cat files/startup-probe/latest" 2>/dev/null | tr -d '\r')
  [ -n "$latest" ] && break
  sleep 2
done
case "$FLAG" in *calibrate*) sleep 8 ;; esac
kill $LOGCAT $GETEVENT 2>/dev/null
wait $LOGCAT $GETEVENT 2>/dev/null
echo "after: $(state)" >> "$OUT/device-state.txt"
echo "after: $(display)" >> "$OUT/refresh-rate.txt"
if [ -z "$latest" ]; then
  echo "NO DUMP" | tee "$OUT/ERROR"
  exit 1
fi
"$ADB" exec-out run-as $PKG cat "files/startup-probe/$latest.json" > "$OUT/probe.json"
case "$FLAG" in *calibrate*)
  "$ADB" exec-out run-as $PKG cat "files/startup-probe/$latest-calibration.json" > "$OUT/calibration.json" ;;
esac
echo "$latest" > "$OUT/dump-name.txt"
# The copy is checked here, on its own; the phone's files are deleted only if it holds a whole run.
if ! "$PYTHON" -I -c "import json,sys; d=json.load(open(sys.argv[1])); assert d['mode'] == 2" "$OUT/probe.json" 2>/dev/null; then
  echo "COPY NOT A WHOLE RUN: the phone's files are kept" | tee "$OUT/ERROR"
  exit 1
fi
echo "ok $1: $(wc -c < "$OUT/probe.json") bytes, $(grep -c . "$OUT/getevent.txt") getevent lines | $(tr '\n' ' ' < "$OUT/refresh-rate.txt")"
