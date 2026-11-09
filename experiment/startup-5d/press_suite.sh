#!/usr/bin/env bash
# Offline cases for press.sh, against standin_adb.sh, which records each `input` in INPUT instead of
# executing it. Then two mutants, each removing one precondition, which their case must kill.
# Usage: press_suite.sh <press.sh> <standin_adb.sh> <work-dir>   (PY as for press.sh)
set -u
PRESS=$(cd "$(dirname "$1")" && pwd)/$(basename "$1"); STAND=$(cd "$(dirname "$2")" && pwd)/$(basename "$2")
WORK=${3:?work dir}; mkdir -p "$WORK"; WORK=$(cd "$WORK" && pwd)
export ADB="$STAND" PY="${PY:-py -3.14 -I -X utf8}"
PASSED=0; FAILED=0

ui() {  # ui.xml with the given refresh buttons, each "l t r b"
  { echo '<?xml version="1.0"?><hierarchy rotation="0"><node index="0" class="android.widget.FrameLayout" bounds="[0,0][1080,2400]">'
    for b in "$@"; do read -r l t r bt <<< "$b"
      echo "<node index=\"1\" text=\"secret_chat_text\" resource-id=\"com.fs.twitchminichat.dev:id/btnRefreshChat\" class=\"android.widget.ImageButton\" bounds=\"[$l,$t][$r,$bt]\" />"; done
    echo '</node></hierarchy>'; } > "$S/ui.xml"
}
start() { CASE=$1; TITLE=$2; C="$WORK/$CASE"; S="$C/S"; mkdir -p "$S" "$C/V"; cp "$TOOL" "$C/V/press.sh"; export STANDIN="$S"; echo dev > "$S/front"; ui "716 132 831 247"; }
run() { (cd "$C/V" && bash ./press.sh "$@") > "$C/out" 2>&1; RC=$?; }
check() {  # expected exit, regex on out, expected INPUT content ("" = none)
  local ok=1 why="" got
  [ "$RC" = "$1" ] || { ok=0; why="exit $RC"; }
  grep -q -E -- "$2" "$C/out" || { ok=0; why="${why:+$why; }no /$2/"; }
  got=$(cat "$S/INPUT" 2>/dev/null)
  [ "$got" = "$3" ] || { ok=0; why="${why:+$why; }input [$got], expected [$3]"; }
  [ ! -e "$S/UNMODELED" ] || { ok=0; why="${why:+$why; }unmodelled: $(head -1 "$S/UNMODELED")"; }
  grep -q secret_chat_text "$C/out" "$C/V/presses.txt" 2>/dev/null && { ok=0; why="${why:+$why; }hierarchy text leaked"; }
  if [ $ok = 1 ]; then PASSED=$((PASSED + 1)); r=PASS; else FAILED=$((FAILED + 1)); r="FAIL: $why"; fi
  printf '%-4s %-60s exit %s, expected %s  %s\n' "$CASE" "$TITLE" "$RC" "$1" "$r"
}

cases() {
  start T1 "a long-press, dev app in front, the button on screen"; run longpress lp1
  check 0 "front_before=dev front_after=dev" "input swipe 773 189 773 189 1000"
  start T2 "another app in front: nothing pressed"; echo other > "$S/front"; run longpress lp1
  check 3 "PRECONDITION VIOLATED \[front\]" ""
  start T3 "no refresh button on screen: nothing pressed"; ui; run longpress lp1
  check 3 "PRECONDITION VIOLATED \[target\]" ""
  start T4 "two refresh buttons on screen: nothing pressed"; ui "716 132 831 247" "300 132 415 247"; run tap t1
  check 3 "PRECONDITION VIOLATED \[target\]" ""
  start T5 "the only refresh button off screen, the next page's: nothing pressed"; ui "1796 132 1911 247"; run tap t1
  check 3 "PRECONDITION VIOLATED \[target\]" ""
  start T6 "no hierarchy readable: nothing pressed"; rm -f "$S/ui.xml"; run longpress lp1
  check 3 "PRECONDITION VIOLATED \[target\]" ""
  start T7 "focus lost after the press: pressed, and reported"; echo other > "$S/front.2"; run longpress lp1
  check 4 "INTERRUPTED \[front-after\]" "input swipe 773 189 773 189 1000"
  start T8 "a tap on refresh"; run tap t1
  check 0 "front_after=dev" "input tap 773 189"
  start T9 "swipe-left starts inside the button"; run swipe-left s1
  check 0 "front_after=dev" "input swipe 773 189 60 189 150"
  start T10 "swipe-right starts inside the button"; run swipe-right s2
  check 0 "front_after=dev" "input swipe 773 189 1061 189 150"
}

TOOL="$PRESS"; cases
echo "press.sh: $PASSED passed, $FAILED failed, of $((PASSED + FAILED))"
ALL_OK=$FAILED

# Mutants: each must be killed by its case.
mutant() {  # id, old text, new text, the case that must kill it
  local m="$WORK/mutant-$1" n
  mkdir -p "$m"; cp "$PRESS" "$m/press.sh"
  n=$($PY -c "import sys; p=sys.argv[1]; t=open(p,encoding='utf-8').read(); c=t.count(sys.argv[2]); open(p,'w',encoding='utf-8',newline='').write(t.replace(sys.argv[2],sys.argv[3])); print(c)" "$(cygpath -w "$m/press.sh")" "$2" "$3")
  [ "$n" = 1 ] || { echo "$1 NOT APPLIED ($n matches)"; ALL_OK=1; return; }
  TOOL="$m/press.sh"; WORK0=$WORK; WORK="$m/work"; mkdir -p "$WORK"; PASSED=0; FAILED=0
  cases > "$m/result.txt"; WORK=$WORK0
  killed=$(grep FAIL "$m/result.txt" | awk '{print $1}' | tr '\n' ' ')
  echo "$1 killed by [${killed% }], required $4: $([[ " $killed" == *" $4 "* ]] && echo KILLED || { ALL_OK=1; echo SURVIVED; })"
}
mutant MP1 '[ "$f0" = dev ] || violated front' ': || violated front' T2
mutant MP2 'case "$b" in NONE*|'"''"') violated target' 'case "$b" in IMPOSSIBLE) violated target' T3
[ "$ALL_OK" = 0 ]
