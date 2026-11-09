#!/usr/bin/env bash
# Offline cases for the long session's tools, sitting.sh and journal_extract.py, run against
# standin_adb.sh instead of a phone (docs/manual-plan-5d-recyclerview.md, section M).
#
# Usage: sitting_suite.sh <tools-dir> <work-dir> <old|new>
#   tools-dir  holds the sitting.sh and journal_extract.py under test. Each case runs its own copy
#              of them, in its own directory, since sitting.sh writes its outputs beside itself.
#   work-dir   an existing, empty directory for the cases' files.
#   old|new    which expectations the P cases hold: "old" for the tools frozen on 2026-11-06,
#              which count both pages as one, "new" for the per-page tools of 2026-11-09.
#
# The cases:
#   C1-C15  the 15 cases of 2026-11-06, REBUILT on 2026-11-09 from their recorded descriptions,
#           since the suite that first ran them was not committed. Same expectations for both.
#   R1-R2   the replaced process, added on 2026-11-09 by the owner's ruling, after the mutation
#           check found the pid and its start time undemonstrated. Same expectations for both.
#   K1      the journal growing over three polls, added on 2026-11-09 after the long session L5D1
#           stopped on a CR. Under "old" it expects today's behaviour, wrong.
#   P1-P6   the two-page cases of 2026-11-09. Under "old" each expects today's behaviour, wrong
#           where the case says so; under "new", the corrected one.
#   L       nothing the cases saved or printed holds an account, a channel, a text field or any
#           other field outside the whitelist, although the synthetic journals carry all of them.
#
# Each case prints one line, with its exit, the exit expected, and PASS or FAIL; the suite ends
# with the counts and exits 0 only if every case passed. PY selects the interpreter that
# sitting.sh is given (default: py -3.14 -I -X utf8).
set -u
TOOLS=$(cd "${1:?tools dir}" && pwd)
WORK=${2:?work dir}
EXPECT=${3:?old or new}
case "$EXPECT" in old|new) ;; *) echo "the third argument is old or new" >&2; exit 2 ;; esac
[ -d "$WORK" ] && [ -z "$(ls -A "$WORK")" ] || { echo "the work dir must exist and be empty: $WORK" >&2; exit 2; }
WORK=$(cd "$WORK" && pwd)
HERE=$(cd "$(dirname "$0")" && pwd)
export ADB="$HERE/standin_adb.sh" POLL_SECONDS=0
export PY="${PY:-py -3.14 -I -X utf8}"
PASSED=0
FAILED=0

# One journal line, as ChatFragment.recordDiagnostics writes it: instant, event, fields.
jl() { local t=$1 e=$2; shift 2; echo "2026-11-09 $t $e $*"; }

# An earlier session's lines, all before the mark: the mark is their last instant, 09:30:00.000.
earlier() {
  jl 09:00:00.000 lifecycle.start account=acct_old channel=chan_old resumed=false historyLoaded=false ircConnected=false
  jl 09:00:00.002 backfill.triggered account=acct_old channel=chan_old resumed=false source=first_connect
  jl 09:00:00.400 backfill.result account=acct_old channel=chan_old resumed=false requestedSec=3600 messageCount=400 firstTs=1794196800 lastTs=1794200400
  jl 09:20:00.000 user.marker account=acct_old channel=chan_old resumed=true shownRows=900
  jl 09:30:00.000 lifecycle.stop account=acct_old channel=chan_old resumed=false ircConnected=true
}

# One page's first connect, after the mark: $1 the minute, $2 the account, $3 the messageCount.
first_connect() {
  jl "10:$1:00.000" lifecycle.start "account=$2" channel=chan_xyz resumed=false historyLoaded=false ircConnected=false
  jl "10:$1:00.002" backfill.triggered "account=$2" channel=chan_xyz resumed=false source=first_connect
  jl "10:$1:00.400" backfill.result "account=$2" channel=chan_xyz resumed=false requestedSec=3600 "messageCount=$3" firstTs=1794200400 lastTs=1794204000
}

# A long-press: $1 the instant's minute and second, $2 the account, $3 shownRows, then extra fields.
marker() { local t=$1 a=$2 r=$3; shift 3; jl "10:$t.000" user.marker "account=$a" channel=chan_xyz resumed=true "shownRows=$r" "$@"; }

# Two pages on one channel: A's page first, B's page 30 s later, and A's backfill as it comes on
# screen, as on the phone on 2026-11-09 (B's first connect 157 ms after the swipe).
two_pages() {
  first_connect 00 "$1" 400
  jl 10:00:30.000 lifecycle.start "account=$2" channel=chan_xyz resumed=false historyLoaded=false ircConnected=false
  jl 10:00:30.002 backfill.triggered "account=$2" channel=chan_xyz resumed=false source=first_connect
  jl 10:00:30.300 backfill.result "account=$2" channel=chan_xyz resumed=false requestedSec=3600 "messageCount=${3:-400}" firstTs=1794200430 lastTs=1794204030
  jl 10:00:30.400 backfill.triggered "account=$1" channel=chan_xyz resumed=true source=resume_no_pause
  jl 10:00:30.600 backfill.result "account=$1" channel=chan_xyz resumed=true requestedSec=40 messageCount=1 firstTs=1794203990 lastTs=1794204030
}

start_case() {
  CASE=$1; TITLE=$2
  C="$WORK/$CASE"; S="$C/S"; V="$C/V"
  mkdir -p "$V" "$S"
  cp "$TOOLS/sitting.sh" "$TOOLS/journal_extract.py" "$V/"
  export STANDIN="$S"
  echo dev > "$S/front"; echo 90.0 > "$S/rate"; echo "0 90" > "$S/range"; echo 4242 > "$S/start"
  : > "$S/pid"
  earlier > "$S/journal.1"
  STEP=0
}

# Runs sitting.sh in the case's directory; its output goes to out.N, its exit to RC.
sit() {
  STEP=$((STEP + 1))
  (cd "$V" && bash ./sitting.sh "$@") > "$C/out.$STEP" 2>&1
  RC=$?
}

# The usual start of a session: the mark with no process, then a process bound.
mark_and_bind() {
  printf '' > "$S/pid.1"; echo 777 > "$S/pid.2"
  sit session-start L1; MARK_RC=$RC
  sit bind b1; BIND_RC=$RC
}

# check <expected exit> <regex the last output must match> [<extra check's name> <0 if it holds>]...
check() {
  local want=$1 rx=$2 ok=1 why=""
  shift 2
  [ "$RC" = "$want" ] || { ok=0; why="exit $RC"; }
  grep -q -E -- "$rx" "$C/out.$STEP" || { ok=0; why="${why:+$why; }no /$rx/"; }
  while [ $# -ge 2 ]; do
    [ "$2" = 0 ] || { ok=0; why="${why:+$why; }$1"; }
    shift 2
  done
  [ ! -e "$S/UNMODELED" ] || { ok=0; why="${why:+$why; }unmodelled adb call: $(head -1 "$S/UNMODELED")"; }
  if [ $ok = 1 ]; then PASSED=$((PASSED + 1)); r=PASS; else FAILED=$((FAILED + 1)); r="FAIL: $why"; fi
  printf '%-3s %-64s exit %s, expected %s  %s\n' "$CASE" "$TITLE" "$RC" "$want" "$r"
}

# The extract a session saved, and its line count.
EX() { echo "$V/journal-extract-L1.txt"; }
lines() { [ -f "$1" ] && wc -l < "$1" | tr -d ' ' || echo 0; }
has() { grep -q -E -- "$2" "$1" 2>/dev/null; echo $?; }
hasnt() { grep -q -E -- "$2" "$1" 2>/dev/null && echo 1 || echo 0; }

# ---------------------------------------------------------------------------------------------
# C1-C15: the cases of 2026-11-06, rebuilt from their descriptions.

start_case C1 "a healthy session; an earlier session's lines excluded by the mark"
{ earlier; first_connect 00 acct_zed 400; marker 30:00 acct_zed 1250 text=hello_world; } > "$S/journal.2"
mark_and_bind; sit session-poll 800 1
check 0 "TARGET REACHED" "mark exit $MARK_RC" "$MARK_RC" "bind exit $BIND_RC" "$BIND_RC" \
  "extract is not exactly the session's 3 lines" "$([ "$(lines "$(EX)")" = 3 ] && echo 0 || echo 1)" \
  "extract holds a line from before the mark" "$(hasnt "$(EX)" ' 09:')"

start_case C2 "another app in front: the rate is not read, the session goes on"
echo other > "$S/front"
{ earlier; first_connect 00 acct_zed 400; marker 30:00 acct_zed 1250; } > "$S/journal.2"
mark_and_bind; sit session-poll 800 1
check 0 "TARGET REACHED" "rates file lacks RATE NOT READ" "$(has "$V/rates-L1.txt" 'RATE NOT READ: the dev app is not in front')"

start_case C3 "the process gone at the second poll"
{ earlier; first_connect 00 acct_zed 400; marker 30:00 acct_zed 500; } > "$S/journal.2"
mark_and_bind; printf '' > "$S/pid.4"; sit session-poll 800 1
check 3 "INVARIANT VIOLATED \[process\]" "first poll did not run" "$(has "$C/out.$STEP" '^L1-1 ')"

start_case C4 "a poll's line outside the whitelist: nothing of the read is saved"
mv "$V/journal_extract.py" "$V/journal_extract_real.py"
cat > "$V/journal_extract.py" <<'PYEOF'
# A test double: the real extractor, with one field outside the whitelist added to its first line.
import subprocess, sys
real = __file__.replace("journal_extract.py", "journal_extract_real.py")
r = subprocess.run([sys.executable, "-I", real] + sys.argv[1:], stdin=sys.stdin, capture_output=True, text=True)
sys.stderr.write(r.stderr)
out = r.stdout.splitlines()
if out and sys.argv[1:2] != ["--last"]:
    out[0] += " channel=leaked"
print("\n".join(out))
sys.exit(r.returncode)
PYEOF
{ earlier; first_connect 00 acct_zed 400; marker 30:00 acct_zed 1250; } > "$S/journal.2"
mark_and_bind; sit session-poll 800 1
check 3 "INVARIANT VIOLATED \[extract\]" "mark exit $MARK_RC" "$MARK_RC" "an extract was saved" "$([ ! -s "$(EX)" ] && echo 0 || echo 1)"

start_case C5 "a refresh rate of 60 with the range [0 60]: recorded, the session goes on"
echo 60.0 > "$S/rate"; echo "0 60" > "$S/range"
{ earlier; first_connect 00 acct_zed 400; marker 30:00 acct_zed 1250; } > "$S/journal.2"
mark_and_bind; sit session-poll 800 1
check 0 "TARGET REACHED" "rates file lacks the rate and range" "$(has "$V/rates-L1.txt" 'refreshRate=60\.0 primaryRefreshRateRange=\[0 60\]')"

start_case C6 "an unreadable journal at a poll"
echo @UNREADABLE > "$S/journal.2"
mark_and_bind; sit session-poll 800 1
check 3 "INVARIANT VIOLATED \[journal\]"

start_case C7 "a rejected messageCount: no bound until the cap"
{ earlier; first_connect 00 acct_zed 4x0; marker 30:00 acct_zed 1250; } > "$S/journal.2"
mark_and_bind; sit session-poll 800 0
check 4 "RESULTS_WITHOUT_MESSAGECOUNT=1.* LIVE=UNKNOWN" "no CAP REACHED" "$(has "$C/out.$STEP" 'CAP REACHED')"

start_case C8 "a journal rotated past the lines saved"
{ earlier; first_connect 00 acct_zed 400; marker 30:00 acct_zed 500; } > "$S/journal.2"
{ earlier; first_connect 00 acct_zed 400 | grep -v backfill.triggered; marker 30:00 acct_zed 500; marker 35:00 acct_zed 600; } > "$S/journal.3"
mark_and_bind; sit session-poll 800 1
check 3 "INVARIANT VIOLATED \[kept\]"

start_case C9 "a mark taken with the app running"
echo 777 > "$S/pid.1"
sit session-start L1
check 3 "INVARIANT VIOLATED \[session\]" "no reason given" "$(has "$C/out.$STEP" 'the app is running')"

start_case C10 "a process bound before the mark"
echo 777 > "$S/pid.1"; printf '' > "$S/pid.2"; echo 777 > "$S/pid.3"
{ earlier; first_connect 00 acct_zed 400; marker 30:00 acct_zed 1250; } > "$S/journal.2"
sit bind b1; B=$RC; sit session-start L1; M=$RC; sit session-poll 800 1
check 3 "INVARIANT VIOLATED \[session\]" "bind exit $B" "$B" "mark exit $M" "$M" "no reason given" "$(has "$C/out.$STEP" 'not after the mark')"

start_case C11 "a reused label"
: > "$V/journal-extract-L1.txt"
sit session-start L1
check 3 "INVARIANT VIOLATED \[session\]" "no reason given" "$(has "$C/out.$STEP" 'already exists')"

start_case C12 "a poll with no mark"
sit session-poll 800 1
check 3 "INVARIANT VIOLATED \[session\]" "no reason given" "$(has "$C/out.$STEP" 'no mark at')"

start_case C13 "two consecutive calls of one session"
{ earlier; first_connect 00 acct_zed 400; marker 30:00 acct_zed 500; } > "$S/journal.2"
{ earlier; first_connect 00 acct_zed 400; marker 30:00 acct_zed 500; marker 40:00 acct_zed 1250; } > "$S/journal.3"
mark_and_bind; sit session-poll 800 0; FIRST=$RC; sit session-poll 800 1
check 0 "TARGET REACHED" "first call exit $FIRST, expected 4" "$([ "$FIRST" = 4 ] && echo 0 || echo 1)" \
  "rates file is not L1-1 then L1-2" "$([ "$(cut -d' ' -f1 "$V/rates-L1.txt" | tr '\n' ' ')" = "L1-1 L1-2 " ] && echo 0 || echo 1)" \
  "extract is not the 4 lines once each" "$([ "$(lines "$(EX)")" = 4 ] && [ "$(sort -u "$(EX)" | wc -l | tr -d ' ')" = 4 ] && echo 0 || echo 1)"

start_case C14 "a mark whose journal is unreadable"
echo @UNREADABLE > "$S/journal.1"
sit session-start L1
check 3 "INVARIANT VIOLATED \[journal\]" "a mark was written" "$([ ! -e "$V/session.txt" ] && echo 0 || echo 1)"

start_case C15 "a mark whose extractor returns other than one instant"
mv "$V/journal_extract.py" "$V/journal_extract_real.py"
cat > "$V/journal_extract.py" <<'PYEOF'
# A test double: answers the mark's read with something that is not one instant.
import sys
sys.stdin.buffer.read()
print("not an instant")
PYEOF
sit session-start L1
check 3 "INVARIANT VIOLATED \[extract\]" "a mark was written" "$([ ! -e "$V/session.txt" ] && echo 0 || echo 1)"

# ---------------------------------------------------------------------------------------------
# R1-R2: the process replaced between two polls. Over a session of 65 to 140 minutes a restart is
# plausible, and a poll that followed the new process would read its history as a continuation.

start_case R1 "the process replaced: a new pid at the second poll"
{ earlier; first_connect 00 acct_zed 400; marker 30:00 acct_zed 500; } > "$S/journal.2"
mark_and_bind; echo 888 > "$S/pid.4"; sit session-poll 800 1
check 3 "INVARIANT VIOLATED \[process\]" "first poll did not run" "$(has "$C/out.$STEP" '^L1-1 ')" \
  "no reason given" "$(has "$C/out.$STEP" "returns '888', not 777")"

start_case R2 "the same pid, started again, at the second poll"
{ earlier; first_connect 00 acct_zed 400; marker 30:00 acct_zed 500; } > "$S/journal.2"
mark_and_bind; echo 5555 > "$S/start.3"; sit session-poll 800 1
check 3 "INVARIANT VIOLATED \[process\]" "first poll did not run" "$(has "$C/out.$STEP" '^L1-1 ')" \
  "no reason given" "$(has "$C/out.$STEP" "started at '5555', not 4242")"

# ---------------------------------------------------------------------------------------------
# K1: the journal growing over three polls, added on 2026-11-09 after the long session L5D1 stopped
# at its 8th poll with INVARIANT VIOLATED [kept]. On Windows the extractor wrote CRLF, and the
# shell's command substitution strips only the last captured line's CR, so a saved line's bytes
# depended on whether it was the last of its read. No case had three polls with the journal growing,
# nor had the cases of 2026-11-06, by their descriptions. Under "old", today's behaviour.

start_case K1 "the journal growing over three polls: saved lines first, byte for byte"
{ earlier; first_connect 00 acct_zed 400; marker 10:00 acct_zed 500; } > "$S/journal.2"
{ earlier; first_connect 00 acct_zed 400; marker 10:00 acct_zed 500; marker 20:00 acct_zed 600; } > "$S/journal.3"
{ earlier; first_connect 00 acct_zed 400; marker 10:00 acct_zed 500; marker 20:00 acct_zed 600; marker 30:00 acct_zed 700; } > "$S/journal.4"
mark_and_bind; sit session-poll 800 1
if [ "$EXPECT" = old ]; then check 3 "INVARIANT VIOLATED \[kept\]" "not at the third poll" "$(has "$C/out.$STEP" '^L1-2 ')"
else check 4 "^L1-3 .*LIVE=300$" "a kept violation" "$(hasnt "$C/out.$STEP" 'INVARIANT VIOLATED')" \
  "the saved extract holds a CR" "$([ "$(tr -cd '\r' < "$(EX)" | wc -c)" = 0 ] && echo 0 || echo 1)" \
  "the saved extract is not the 5 lines" "$([ "$(lines "$(EX)")" = 5 ] && echo 0 || echo 1)"; fi

# ---------------------------------------------------------------------------------------------
# P1-P6: two pages on one channel. A is the page the session measures, the first to connect.

start_case P1 "B's history counted against A's live rows (A has 800)"
{ earlier; two_pages acct_zed acct_amy; marker 40:00 acct_zed 1201; } > "$S/journal.2"
mark_and_bind; sit session-poll 800 0
if [ "$EXPECT" = old ]; then check 4 " LIVE=400$" "no CAP REACHED" "$(has "$C/out.$STEP" 'CAP REACHED')"
else check 0 "TARGET REACHED" "A's live rows not 800" "$(has "$C/out.$STEP" ' A_live=800 .*LIVE=800$')"; fi

start_case P2 "B pressed after A: the last marker is B's"
{ earlier; two_pages acct_zed acct_amy; marker 40:00 acct_zed 1201; marker 40:20 acct_amy 1150; } > "$S/journal.2"
mark_and_bind; sit session-poll 800 0
if [ "$EXPECT" = old ]; then check 4 " LIVE=349$"
else check 0 "TARGET REACHED" "B not counted on its own" "$(has "$C/out.$STEP" ' B_live=750 .*LIVE=800$')"; fi

start_case P3 "labels follow order of appearance, not the account"
{ earlier; two_pages acct_amy acct_zed; marker 40:00 acct_amy 1201; } > "$S/journal.2"
mark_and_bind; sit session-poll 800 0
if [ "$EXPECT" = old ]; then check 4 " LIVE=400$"
else check 0 "TARGET REACHED" "the first account to appear is not A" "$(has "$C/out.$STEP" ' A_history=401 .* B_history=400 ')" \
  "the extract does not label A first" "$(has "$(EX)" '^2026-11-09 10:00:00\.002 backfill\.triggered page=A ')"; fi

start_case P4 "the first long-press on B, not on the page measured"
{ earlier; two_pages acct_zed acct_amy; marker 40:00 acct_amy 900; marker 41:00 acct_zed 1201; } > "$S/journal.2"
mark_and_bind; sit session-poll 800 0
if [ "$EXPECT" = old ]; then check 4 " LIVE=400$"
else check 3 "INVARIANT VIOLATED \[pages\]"; fi

start_case P5 "B's rejected messageCount does not void A's bound"
{ earlier; two_pages acct_zed acct_amy 4x0; marker 40:00 acct_zed 1201; } > "$S/journal.2"
mark_and_bind; sit session-poll 800 0
if [ "$EXPECT" = old ]; then check 4 " LIVE=UNKNOWN$"
else check 0 "TARGET REACHED" "A's live rows not 800" "$(has "$C/out.$STEP" 'RESULTS_WITHOUT_MESSAGECOUNT=1 .* LIVE=800$')"; fi

start_case P6 "a result with no account counts against every page"
{ earlier; two_pages acct_zed acct_amy; jl 10:20:00.000 backfill.triggered channel=chan_xyz resumed=false source=reconnect_offline; jl 10:20:00.300 backfill.result channel=chan_xyz resumed=false requestedSec=30 messageCount=5 firstTs=1794205170 lastTs=1794205200; marker 40:00 acct_zed 1201; } > "$S/journal.2"
mark_and_bind; sit session-poll 800 0
if [ "$EXPECT" = old ]; then check 4 " LIVE=395$"
else check 4 "UNATTRIBUTED=2 .* LIVE=795$"; fi

# ---------------------------------------------------------------------------------------------
# L: what the cases saved and printed, against the fields the synthetic journals carried.

CASE=L; TITLE="nothing saved or printed holds a field outside the whitelist"; C="$WORK"; S="$WORK"; STEP=0
saved=$(find "$WORK" -path '*/S' -prune -o -type f \( -name 'journal-extract-*' -o -name 'rates-*' -o -name 'session.txt' -o -name 'under-measurement.txt' -o -name 'out.*' \) -print)
leaks=$(printf '%s\n' "$saved" | xargs grep -l -E 'acct_|chan_|hello_world|account=|channel=|text=|requestedSec|resumed=' 2>/dev/null | wc -l | tr -d ' ')
files=$(printf '%s\n' "$saved" | grep -c .)
nlines=$(printf '%s\n' "$saved" | xargs cat | wc -l | tr -d ' ')
echo "L: $files files, $nlines lines" > "$WORK/out.0"; RC=0
check 0 "files" "$leaks of $files files hold a field outside the whitelist" "$leaks"

echo "suite ($EXPECT expectations, $(basename "$TOOLS") tools, PY=$PY): $PASSED passed, $FAILED failed, of $((PASSED + FAILED)); L read $files files, $nlines lines"
[ "$FAILED" = 0 ]
