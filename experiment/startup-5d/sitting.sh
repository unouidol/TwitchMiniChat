#!/usr/bin/env bash
# The view-count instrument's sitting (docs/manual-plan-5d-recyclerview.md, section M, and the records
# of 2026-11-06 under *Recorded results*).
#
# Usage:
#   sitting.sh state <label>       phone state, before and after; needs no process
#   sitting.sh probedir <label>    the probe directory's listing; needs no process
#   sitting.sh bind <label>        binds the running process as THE process under measurement
#   sitting.sh dump <label>        one meminfo dump of the bound process
#   sitting.sh rows <label>        the bound process's timeline row counts
#   sitting.sh poll <label> <target-rows> <max-minutes>
#                                  rows, every POLL_SECONDS (60), until the rows reach the target
#   sitting.sh rate <label>        the refresh rate and its moment, only after 2.2 s with the app in front
#   sitting.sh journal [since]     the journal's whitelisted fields, through journal_extract.py
#   sitting.sh session-start <label>
#                                  the long session's mark, taken with the dev app not running,
#                                  before the launch whose history the session counts
#   sitting.sh session-poll <target-live-rows> <max-minutes>
#                                  the long session's poll: the invariant, the rate, the journal,
#                                  counted per page; the target is page A's live rows
#
# Every command reads; none sends input to the phone or changes it. What is saved here: the meminfo
# dumps, the state reads, and the row COUNTS only. The view hierarchy from `dumpsys activity top` is
# parsed in a pipe and never written, since it can carry more than counts.
#
# The invariant, checked before EVERY dump and EVERY row count, poll iterations included: the reading
# comes from the process bound with `bind` - same package, same pid, same start time - and, for rows,
# from that process's view hierarchy with the timeline in it. When it does not hold, the command
# prints "INVARIANT VIOLATED [...]" naming what was required and what was found, and exits 3. A poll
# stops at the first violation. On 2026-11-06 a loop without this check polled a dead process for
# eight minutes and printed "no container found" eight times: its silence was the defect.
set -u
export MSYS_NO_PATHCONV=1
ADB="${ADB:-adb}"
PKG=com.fs.twitchminichat.dev
V="$(cd "$(dirname "$0")" && pwd)"
UNDER="${UNDER:-$V/under-measurement.txt}"
PY="${PY:-py -3.10 -I -X utf8}"

# Prints the violated invariant, what it requires, and what was found, then exits 3.
fail_invariant() {
  local required
  case "$1" in
    bound) required="a process under measurement is bound, with 'sitting.sh bind <label>', before any reading" ;;
    process) required="every reading comes from $B_PKG pid $B_PID, started at $B_START (clock ticks since boot), bound at '$B_LABEL'" ;;
    timeline) required="the timeline under measurement, a chatContainer or chatList, is in the view hierarchy of pid $B_PID" ;;
    session) required="the session's mark is taken with the dev app not running, and the process bound after it, so that every history delivery the session counts is in the journal after the mark" ;;
    journal) required="the journal is read from the phone and extracted; an empty read is a failure, never 'nothing new'" ;;
    extract) required="every extracted line holds only the whitelisted events and fields" ;;
    kept) required="every journal line an earlier poll extracted is extracted again, first and in order: the journal since the mark only grows" ;;
    pages) required="the first long-press since the mark is on page A, the first page whose lines appear after the mark: the page the session measures" ;;
  esac
  {
    echo "INVARIANT VIOLATED [$1]"
    echo "  required: $required"
    echo "  found:    $2"
    case "$1" in
      bound|process|timeline) echo "  No reading from here on measures what was bound. Stopping, exit 3." ;;
      *) echo "  Nothing from here on is read or saved. Stopping, exit 3." ;;
    esac
  } >&2
  exit 3
}

# The start time of a pid, field 22 of /proc/<pid>/stat, or nothing.
proc_start() {
  "$ADB" shell cat "/proc/$1/stat" 2>/dev/null | tr -d '\r' | sed -E 's/^[0-9]+ \(.*\) //' | awk '{print $20}'
}

# Loads the binding; fails when there is none.
load_binding() {
  [ -f "$UNDER" ] || { B_PKG=$PKG B_PID=? B_START=? B_LABEL=?; fail_invariant bound "no binding file at $UNDER"; }
  B_PKG=$(sed -n 's/^pkg=//p' "$UNDER"); B_PID=$(sed -n 's/^pid=//p' "$UNDER")
  B_START=$(sed -n 's/^start=//p' "$UNDER"); B_LABEL=$(sed -n 's/^label=//p' "$UNDER")
}

# The precondition of every reading: the bound process exists, and is the one bound.
require_process() {
  local pids start
  load_binding
  pids=$("$ADB" shell pidof "$B_PKG" 2>/dev/null | tr -d '\r')
  [ -n "$pids" ] || fail_invariant process "pidof $B_PKG returns no process"
  [ "$pids" = "$B_PID" ] || fail_invariant process "pidof $B_PKG returns '$pids', not $B_PID: the process was restarted, and its timeline is not the one under measurement"
  start=$(proc_start "$B_PID")
  [ "$start" = "$B_START" ] || fail_invariant process "pid $B_PID is alive but started at '${start:-unreadable}', not $B_START: that pid is not the process bound"
}

bind() {
  local pids start
  B_PKG=$PKG B_PID=? B_START=? B_LABEL=$1
  pids=$("$ADB" shell pidof $PKG 2>/dev/null | tr -d '\r')
  [ -n "$pids" ] || fail_invariant bound "pidof $PKG returns no process to bind"
  case "$pids" in *" "*) fail_invariant bound "pidof $PKG returns several processes: '$pids'" ;; esac
  start=$(proc_start "$pids")
  [ -n "$start" ] || fail_invariant bound "the start time of pid $pids could not be read from /proc/$pids/stat"
  printf 'pkg=%s\npid=%s\nstart=%s\nlabel=%s\nbound_at=%s\nphone_since=%s\n' "$PKG" "$pids" "$start" "$1" "$(date +%s.%N)" \
    "$("$ADB" shell "date '+%Y-%m-%d %H:%M:%S'" | tr -d '\r').000" > "$UNDER"
  echo "bound: $PKG pid $pids start $start at '$1' -> $UNDER"
}

state() {
  local b field path
  b=$("$ADB" shell dumpsys battery | tr -d '\r')
  field() { printf '%s\n' "$b" | grep -m1 "  $1:" | sed 's/.*: //'; }
  echo "label: $1"
  echo "host_time: $(date '+%Y-%m-%d %H:%M:%S %z')"
  echo "battery: saver=$("$ADB" shell settings get global low_power | tr -d '\r') ac=$(field 'AC powered') usb=$(field 'USB powered') wireless=$(field 'Wireless powered') status=$(field status) level=$(field level) temperature=$(field temperature)"
  echo "thermal: $("$ADB" shell dumpsys thermalservice | tr -d '\r' | grep -m1 'Thermal Status')"
  echo "display: $("$ADB" shell dumpsys display | grep -o 'mActiveSfDisplayMode=DisplayMode{[^}]*}' | grep -o 'refreshRate=[0-9.]*' | head -1 | tr -d '\r')"
  echo "stay_on_while_plugged_in: $("$ADB" shell settings get global stay_on_while_plugged_in | tr -d '\r')"
  echo "screen_off_timeout: $("$ADB" shell settings get system screen_off_timeout | tr -d '\r')"
  echo "power: $("$ADB" shell dumpsys power | tr -d '\r' | grep -o -E 'mWakefulness=[A-Za-z]+|mStayOn=[a-z]+' | sort -u | tr '\n' ' ')"
  path=$("$ADB" shell pm path $PKG | tr -d '\r' | sed 's/^package://' | head -1)
  echo "phone_sha256: $("$ADB" shell sha256sum "$path" | tr -d '\r' | cut -c1-64)"
  echo "package: $("$ADB" shell dumpsys package $PKG | tr -d '\r' | grep -o -E 'versionCode=[0-9]+|versionName=[^ ]+|lastUpdateTime=[^ ]+ [^ ]+' | sort -u | tr '\n' ' ')"
  echo "pid: $("$ADB" shell pidof $PKG | tr -d '\r')"
}

probedir() {
  echo "label: $1"
  "$ADB" shell "run-as $PKG ls -la files/startup-probe" 2>&1 | tr -d '\r'
}

dump() {
  local f="$V/$1.meminfo.txt" t0 t1 dumped
  require_process
  t0=$(date +%s.%N)
  "$ADB" shell dumpsys meminfo $PKG | tr -d '\r' > "$f"
  t1=$(date +%s.%N)
  # The process can end between the check and the dump: the dump must name the bound pid.
  dumped=$(grep -m1 -o -E 'MEMINFO in pid [0-9]+' "$f" | grep -o -E '[0-9]+$')
  [ "$dumped" = "$B_PID" ] || fail_invariant process "the dump in $f names pid '${dumped:-none}', not $B_PID"
  printf '%s pid=%s start=%s end=%s seconds=%s | %s | %s | %s\n' "$1" "$dumped" "$t0" "$t1" \
    "$(awk -v a="$t0" -v b="$t1" 'BEGIN{printf "%.2f", b-a}')" \
    "$(grep -m1 -E '^ +Views:' "$f" | sed 's/  */ /g')" \
    "$(grep -m1 -E '^ +Java Heap:' "$f" | sed 's/  */ /g')" \
    "$(grep -m1 -E '^ +TOTAL PSS:' "$f" | sed 's/  */ /g')" | tee -a "$V/dumps.txt"
}

rows() {
  local counts
  require_process
  counts=$("$ADB" shell dumpsys activity top | tr -d '\r' | $PY -c '
import re, sys
pkg, want = sys.argv[1], sys.argv[2]
pid = None
seen = False
stack = []  # [indent, name, count] for open containers
out = []
for line in sys.stdin:
    if re.match(r"\s*ACTIVITY ", line):
        m = re.search(r"ACTIVITY " + re.escape(pkg) + r"/\S+ \S+ pid=(\S+)", line)
        pid, seen = (m.group(1) if m else None), False
        continue
    if pid != want:
        continue
    if not seen:
        seen = "View Hierarchy:" in line
        continue
    s = line.rstrip("\n")
    if not s.strip():
        continue
    ind = len(s) - len(s.lstrip(" "))
    while stack and ind <= stack[-1][0]:
        out.append(stack.pop())
    if stack and ind == stack[-1][0] + 2:
        stack[-1][2] += 1
    m = re.search(r"app:id/(chatContainer|chatList)[ }]", s)
    if m:
        stack.append([ind, m.group(1), 0])
while stack:
    out.append(stack.pop())
print(" ".join("%s=%d" % (n, c) for _, n, c in out))
' "$B_PKG" "$B_PID")
  [ -n "$counts" ] || fail_invariant timeline "no chatContainer or chatList in the view hierarchy of an activity of pid $B_PID"
  echo "$1 $(date +%s.%N) $counts" | tee -a "$V/rows.txt"
}

# "dev" when the focused window is the package under measurement's, "other" otherwise. Another
# app's name is never printed.
front() {
  local f
  f=$("$ADB" shell dumpsys window | tr -d '\r' | grep -m1 'mCurrentFocus=')
  case "$f" in *" $PKG/"*) echo dev ;; *) echo other ;; esac
}

# The display's rate with its moment, read only with the dev app in front at two reads 2.2 s apart,
# more than the 2.1 s within which the phone re-compares the display's range after a front-app
# change (1.66 to 2.10 s in 113 launches from the launcher, docs/manual-plan-5d-recyclerview.md).
# What happens between the two reads is not read. Not a void condition: a change is recorded where
# it happens. A read the condition forbids says so in capitals.
rate() {
  local th tp f2 f3 d r range
  if [ "$(front)" != dev ]; then echo "$1 host=$(date +%s.%N) RATE NOT READ: the dev app is not in front"; return 0; fi
  sleep 2.2
  f2=$(front)
  if [ "$f2" != dev ]; then echo "$1 host=$(date +%s.%N) RATE NOT READ: the dev app was not in front 2.2 s later"; return 0; fi
  th=$(date +%s.%N)
  d=$("$ADB" shell dumpsys display | tr -d '\r')
  r=$(printf '%s\n' "$d" | grep -o 'mActiveSfDisplayMode=DisplayMode{[^}]*}' | grep -o 'refreshRate=[0-9.]*' | head -1)
  range=$(printf '%s\n' "$d" | grep -o 'primaryRefreshRateRange=\[[^]]*\]' | head -1)
  tp=$("$ADB" shell date +%s | tr -d '\r')
  f3=$(front)
  echo "$1 host=$th phone=$tp front_t0=dev front_t2.2s=dev ${r:-refreshRate=NOT_FOUND} ${range:-primaryRefreshRateRange=NOT_FOUND} front_after=$f3"
}

# The journal's whitelisted fields after an instant, then a check that every line printed has only
# those fields. The raw journal is piped from the phone and never written.
# The journal, both files in order, piped from the phone into the extractor with its argument.
journal_pipe() {
  local out ex rc
  # MSYS_NO_PATHCONV is exported for adb, so a Windows Python must be given a Windows path.
  ex=$(cygpath -w "$V/journal_extract.py")
  out=$(set -o pipefail
        { "$ADB" exec-out run-as $PKG cat files/diagnostics/history-diagnostics-previous.log
          "$ADB" exec-out run-as $PKG cat files/diagnostics/history-diagnostics.log; } 2>/dev/null |
        $PY "$ex" "$1")
  rc=$?
  [ $rc -eq 0 ] || fail_invariant journal "the read or the extraction exited $rc"
  printf '%s' "$out"
}

# The instant of the journal's last line, of any event, and nothing else.
journal_last() {
  local out
  out=$(journal_pipe --last) || exit 3
  [[ "$out" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}\ [0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]{3}$ ]] ||
    fail_invariant extract "the journal's last instant read as something other than one instant"
  printf '%s\n' "$out"
}

journal() {
  local out
  out=$(journal_pipe "$1") || exit 3
  # Every line carries its page, A to Z or "?", first (journal_extract.py).
  if [ -n "$out" ] && printf '%s\n' "$out" | grep -v -q -E '^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]{3} (backfill\.triggered page=([A-Z]|\?)( source=(first_connect|reconnect_offline|reconnect_no_pause_reference|resume|resume_no_pause|manual_refresh))?|backfill\.result page=([A-Z]|\?)( messageCount=[0-9]+)?( firstTs=[0-9]+)?( lastTs=[0-9]+)?|user\.marker page=([A-Z]|\?)( shownRows=[0-9]+)?)$'; then
    fail_invariant extract "a line holds more than the whitelisted fields; nothing of this read was saved"
  fi
  [ -z "$out" ] || printf '%s\n' "$out"
}

# The long session's mark: the instant of the journal's last line, read with the dev app not
# running, before the launch. It is in the journal's own clock, the app's, so the session's lines
# are exactly those after it, whatever the shell's clock reads. While the app is not running it
# writes nothing, so nothing of the session can precede the mark.
session_start() {
  local label=$1 pids mark f
  case "$label" in ''|*[!A-Za-z0-9._-]*) echo "session-start: the label takes letters, digits, '.', '_' and '-' only" >&2; exit 2 ;; esac
  for f in "$V/journal-extract-$label.txt" "$V/rates-$label.txt"; do
    [ ! -e "$f" ] || fail_invariant session "$f already exists: a second session under that label would mix two sessions' lines"
  done
  pids=$("$ADB" shell pidof $PKG 2>/dev/null | tr -d '\r')
  [ -z "$pids" ] || fail_invariant session "pidof $PKG returns '$pids': the app is running, so the history the session counts may have been loaded before the mark"
  mark=$(journal_last) || exit 3
  printf 'label=%s\nmark=%s\nmarked_at_host=%s\n' "$label" "$mark" "$(date +%s.%N)" > "$V/session.txt"
  echo "session '$label' marked at $mark, the journal's last line. Launch the app, then bind."
}

# The long session's counts, from every extracted line since the mark, in order, PER PAGE: each
# line's page=<label> names the page that wrote it (journal_extract.py), so the history delivered
# to a page is set against that page's own rows. Page A is the first page whose lines appear after
# the mark, the page the session measures. At each page's last long-press: the rows shown, the
# history delivered to that page before it, and their difference. History rows shown are at most
# the messages delivered, since the deduplicator can only remove rows (ChatFragment.kt:2956 counts
# the response before it), so the difference is a lower bound on that page's live rows. History
# with no page (page=?) is set against every page, which keeps each bound a lower bound. A result
# whose messageCount was rejected makes its page's bound, or every page's when it has no page, no
# bound, printed UNKNOWN. The last token, LIVE, is page A's: the target is A's live rows.
# FIRST_MARKER names the page of the first long-press, which session_poll requires to be A.
# Until 2026-11-09 this counted every page as one: B's history was set against A's rows, and the
# last long-press was taken as the count whichever page it came from.
session_counts() {
  awk '
    function field(k,   i) { for (i = 4; i <= NF; i++) if (index($i, k "=") == 1) return substr($i, length(k) + 2); return "" }
    function page(   p) { p = field("page"); return p == "" ? "?" : p }
    $3 == "backfill.triggered" { nt++; s = field("source"); if (s == "") s = "source_rejected"; by[s]++
      p = page(); if (p == "?") unattr++; else seen[p] = 1 }
    $3 == "backfill.result" { nr++; p = page(); m = field("messageCount")
      if (m == "") nomc_all++; else all += m
      if (p == "?") { unattr++; if (m == "") un_nomc++; else un_hist += m }
      else { seen[p] = 1; if (m == "") nomc[p]++; else hist[p] += m } }
    $3 == "user.marker" { p = page(); r = field("shownRows")
      if (p == "?") { unattr++; next }
      seen[p] = 1
      if (r == "") next
      if (first == "") first = p
      rows[p] = r; at[p] = $1 " " $2; hb[p] = hist[p] + un_hist; nb[p] = nomc[p] + un_nomc; nm[p]++ }
    END {
      n = split("first_connect reconnect_offline reconnect_no_pause_reference resume resume_no_pause manual_refresh source_rejected", o, " ")
      s = ""; for (i = 1; i <= n; i++) if (by[o[i]]) s = s (s == "" ? "" : ",") o[i] ":" by[o[i]]
      printf "triggered=%d[%s] results=%d history_delivered=%d", nt, s, nr, all
      if (nomc_all) printf " RESULTS_WITHOUT_MESSAGECOUNT=%d", nomc_all
      if (unattr) printf " UNATTRIBUTED=%d", unattr
      if (!nr) printf " HISTORY_NOT_YET_RECORDED_SINCE_THE_MARK"
      if (first != "") printf " FIRST_MARKER=%s", first
      for (c = 65; c <= 90; c++) {
        p = sprintf("%c", c)
        if (!(p in seen)) continue
        printf " %s_history=%d", p, hist[p]
        if (!nm[p]) { printf " %s_marker=none_yet", p; continue }
        printf " %s_marker=\"%s\" %s_shownRows=%d %s_history_before_it=%d", p, at[p], p, rows[p], p, hb[p]
        if (nb[p]) printf " %s_live=UNKNOWN", p; else printf " %s_live=%d", p, rows[p] - hb[p]
      }
      if (!nm["A"]) printf " LIVE=none\n"; else if (nb["A"]) printf " LIVE=UNKNOWN\n"; else printf " LIVE=%d\n", rows["A"] - hb["A"]
    }'
}

# The long session's poll: on every iteration the process invariant, the rate with its moment, and
# the journal since the mark, of which the lines earlier polls saved must come first. It ends when
# the live rows at the last long-press, counted apart from history, reach the target. A session runs
# as consecutive calls, each shorter than the shell's limit: labels continue from the rates file,
# which every iteration adds one line to, and the saved extract carries over.
session_poll() {
  local target=$1 max=$2 i label mark marked bound ex rt all n new counts live deadline
  [ -f "$V/session.txt" ] || fail_invariant session "no mark at $V/session.txt: run 'sitting.sh session-start <label>' before the launch"
  label=$(sed -n 's/^label=//p' "$V/session.txt"); mark=$(sed -n 's/^mark=//p' "$V/session.txt")
  marked=$(sed -n 's/^marked_at_host=//p' "$V/session.txt")
  load_binding
  bound=$(sed -n 's/^bound_at=//p' "$UNDER")
  # Both are the host's clock: the mark's moment and the binding's.
  awk -v b="$bound" -v m="$marked" 'BEGIN { exit !(b != "" && m != "" && b + 0 > m + 0) }' ||
    fail_invariant session "the process was bound at host time '${bound:-unrecorded}', not after the mark at '${marked:-unrecorded}'"
  ex="$V/journal-extract-$label.txt"; rt="$V/rates-$label.txt"
  i=0; [ ! -f "$rt" ] || i=$(wc -l < "$rt")
  deadline=$(( $(date +%s) + max * 60 ))
  while :; do
    i=$((i + 1))
    require_process
    rate "$label-$i" | tee -a "$rt"
    all=$(journal "$mark") || exit 3
    n=0; [ ! -f "$ex" ] || n=$(wc -l < "$ex")
    if [ "$n" -gt 0 ] && [ "$(printf '%s\n' "$all" | head -n "$n")" != "$(cat "$ex")" ]; then
      fail_invariant kept "the journal no longer yields, first, the $n lines earlier polls saved in $ex: it rotated past them, or changed"
    fi
    new=$(printf '%s\n' "$all" | tail -n +$((n + 1)))
    [ -z "$new" ] || printf '%s\n' "$new" | tee -a "$ex"
    counts=$(printf '%s\n' "$all" | session_counts)
    echo "$label-$i $counts"
    # The page pressed first is the page measured; it must be A, the first page after the mark.
    first=$(printf '%s\n' "$counts" | sed -n 's/.* FIRST_MARKER=\([^ ]*\).*/\1/p')
    [ -z "$first" ] || [ "$first" = A ] ||
      fail_invariant pages "the first long-press since the mark is on page $first: the page pressed is not the first page connected after the mark"
    live=${counts##*LIVE=}
    if [[ "$live" =~ ^[0-9]+$ ]] && [ "$live" -ge "$target" ]; then echo "TARGET REACHED: at least $live live rows >= $target"; return 0; fi
    if [ "$(date +%s)" -ge "$deadline" ]; then echo "CAP REACHED: $max min" >&2; return 4; fi
    sleep "${POLL_SECONDS:-60}"
  done
}

poll() {
  local label=$1 target=$2 max=$3 i=0 line n deadline
  deadline=$(( $(date +%s) + max * 60 ))
  while :; do
    i=$((i + 1))
    line=$(rows "$label-$i") || exit 3
    echo "$line"
    n=$(printf '%s\n' "$line" | grep -o -E '=[0-9]+' | tr -d '=' | awk '{s += $1} END {print s + 0}')
    if [ "$n" -ge "$target" ]; then echo "TARGET REACHED: $n >= $target rows at $label-$i"; return 0; fi
    if [ "$(date +%s)" -ge "$deadline" ]; then echo "CAP REACHED: $max min, $n < $target rows at $label-$i" >&2; return 4; fi
    sleep "${POLL_SECONDS:-60}"
  done
}

case "${1:-}" in
  state|probedir|bind|dump|rows|rate) "$1" "${2:?label}" ;;
  journal) journal "${2:-}" ;;
  poll) poll "${2:?label}" "${3:?target rows}" "${4:?max minutes}" ;;
  session-start) session_start "${2:?label}" ;;
  session-poll) session_poll "${2:?target live rows}" "${3:?max minutes}" ;;
  *) echo "usage: sitting.sh state|probedir|bind|dump|rows|rate <label> | journal [since] | poll <label> <target-rows> <max-minutes> | session-start <label> | session-poll <target-live-rows> <max-minutes>" >&2; exit 2 ;;
esac
