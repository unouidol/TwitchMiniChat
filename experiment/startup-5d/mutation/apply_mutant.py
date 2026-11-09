"""Applies one mutant to a copy of the long session's tools, or lists the mutants.

Usage: apply_mutant.py list
       apply_mutant.py <tools-dir> <mutant-id>

Each mutant is one exact substitution in sitting.sh or journal_extract.py, as committed at
6dc2351 / bff7379. It is applied only if its old text occurs EXACTLY ONCE; otherwise nothing is
written and the exit is 2, so a mutant can never silently miss its target. "predicted" is written
before any mutant was run: the C case expected to kill it, or SURVIVES where no C case reaches the
behaviour.
"""
import sys
from pathlib import Path

S, J = "sitting.sh", "journal_extract.py"
MUTANTS = [
    # id, family, file, old, new, predicted
    ("M01", "the mark excludes an earlier session's lines", J,
     "    if event not in KEEP or instant <= since:", "    if event not in KEEP:", "C1"),
    ("M02", "the mark is taken in the journal's own clock", S,
     "  mark=$(journal_last) || exit 3", "  mark=\"$(date '+%Y-%m-%d %H:%M:%S').000\"", "C1"),
    ("M03", "the mark refuses a running app", S,
     "  [ -z \"$pids\" ] || fail_invariant session \"pidof $PKG returns '$pids': the app is running, so the history the session counts may have been loaded before the mark\"",
     "  :", "C9"),
    ("M04", "the mark is exactly one instant", S,
     "    fail_invariant extract \"the journal's last instant read as something other than one instant\"",
     "    true", "C15"),
    ("M05", "a label is never reused", S,
     "    [ ! -e \"$f\" ] || fail_invariant session \"$f already exists: a second session under that label would mix two sessions' lines\"",
     "    :", "C11"),
    ("M06", "a poll needs a mark", S,
     "  [ -f \"$V/session.txt\" ] || fail_invariant session \"no mark at $V/session.txt: run 'sitting.sh session-start <label>' before the launch\"",
     "  :", "C12"),
    ("M07", "the process is bound after the mark", S,
     "    fail_invariant session \"the process was bound at host time '${bound:-unrecorded}', not after the mark at '${marked:-unrecorded}'\"",
     "    true", "C10"),
    ("M08", "every poll checks the bound process is alive", S,
     "    require_process\n", "", "C3"),
    ("M09", "the bound pid must keep its start time", S,
     "  [ \"$start\" = \"$B_START\" ] || fail_invariant process \"pid $B_PID is alive but started at '${start:-unreadable}', not $B_START: that pid is not the process bound\"",
     "  :", "SURVIVES"),
    ("M10", "a failed journal read stops the poll (sitting.sh)", S,
     "  [ $rc -eq 0 ] || fail_invariant journal \"the read or the extraction exited $rc\"",
     "  :", "C6, C14"),
    ("M11", "an empty journal input is an error (extractor)", J,
     "    sys.exit(5)", "    pass", "C6, C14"),
    ("M12", "lines saved by earlier polls must come first", S,
     "      fail_invariant kept \"the journal no longer yields, first, the $n lines earlier polls saved in $ex: it rotated past them, or changed\"",
     "      true", "C8"),
    ("M13", "a line outside the grammar stops the poll", S,
     "    fail_invariant extract \"a line holds more than the whitelisted fields; nothing of this read was saved\"",
     "    true", "C4"),
    ("M14", "the extractor prints only whitelisted fields", J,
     "    print(instant, event, *out)", "    print(instant, event, *out, *rest.split())", "C1"),
    ("M15", "a value must match its field's shape", J,
     "\"messageCount\": r\"\\d{1,6}\"", "\"messageCount\": r\"\\S+\"", "C7"),
    ("M16", "a source is one of the six values only", J,
     "\"backfill.triggered\": {\"source\": SOURCES},", "\"backfill.triggered\": {\"source\": r\"[a-z_]+\"},", "SURVIVES"),
    ("M17", "a rejected messageCount leaves no bound", S,
     "nb[p] = nomc[p] + un_nomc;", "nb[p] = 0;", "C7"),
    ("M18", "LIVE is the rows less the history before them", S,
     "else printf \" LIVE=%d\\n\", rows[\"A\"] - hb[\"A\"]", "else printf \" LIVE=%d\\n\", rows[\"A\"]", "SURVIVES"),
    ("M19", "the target is reached at exactly the target", S,
     "[ \"$live\" -ge \"$target\" ]; then echo \"TARGET REACHED: at least", "[ \"$live\" -gt \"$target\" ]; then echo \"TARGET REACHED: at least", "SURVIVES"),
    ("M20", "the session ends at its cap", S,
     "    if [ \"$(date +%s)\" -ge \"$deadline\" ]; then echo \"CAP REACHED: $max min\" >&2; return 4; fi",
     "    :", "C7"),
    ("M21", "no rate is read with another app in front", S,
     "  if [ \"$(front)\" != dev ]; then echo \"$1 host=$(date +%s.%N) RATE NOT READ: the dev app is not in front\"; return 0; fi",
     "  :", "C2"),
    ("M22", "the front is read again 2.2 s later", S,
     "  if [ \"$f2\" != dev ]; then echo \"$1 host=$(date +%s.%N) RATE NOT READ: the dev app was not in front 2.2 s later\"; return 0; fi",
     "  :", "SURVIVES"),
    ("M23", "the rate is recorded with its range", S,
     "${range:-primaryRefreshRateRange=NOT_FOUND} ", "", "C5"),
    ("M24", "a second call continues the labels", S,
     "  i=0; [ ! -f \"$rt\" ] || i=$(wc -l < \"$rt\")", "  i=0", "C13"),
    ("M25", "a second call continues the saved extract", S,
     "    n=0; [ ! -f \"$ex\" ] || n=$(wc -l < \"$ex\")", "    n=0", "C13, C8"),
    # Added after the first run of M01-M25, which had missed this check; predicted before its run.
    ("M26", "the live pid must be the bound pid", S,
     "  [ \"$pids\" = \"$B_PID\" ] || fail_invariant process \"pidof $B_PKG returns '$pids', not $B_PID: the process was restarted, and its timeline is not the one under measurement\"",
     "  :", "SURVIVES"),
]

if len(sys.argv) == 2 and sys.argv[1] == "list":
    for mid, family, f, _, _, predicted in MUTANTS:
        print(f"{mid}\t{f}\t{predicted}\t{family}")
    sys.exit(0)

tools, mid = Path(sys.argv[1]), sys.argv[2]
matches = [m for m in MUTANTS if m[0] == mid]
if len(matches) != 1:
    print(f"no mutant {mid}", file=sys.stderr)
    sys.exit(2)
_, family, f, old, new, _ = matches[0]
path = tools / f
text = path.read_bytes().decode("utf-8")
n = text.count(old)
if n != 1:
    print(f"{mid}: its old text occurs {n} times in {f}, not once; nothing written", file=sys.stderr)
    sys.exit(2)
path.write_bytes(text.replace(old, new).encode("utf-8"))
print(f"{mid} applied to {f}: {family}")
