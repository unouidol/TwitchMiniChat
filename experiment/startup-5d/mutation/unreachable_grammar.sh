#!/usr/bin/env bash
# U1: whether the state case C4 reaches with a test double is reachable with the real extractor.
# The grammar check in sitting.sh's journal() fires only if the extractor emits a line outside
# it. This feeds the REAL extractor, as committed, a hostile journal and tests every line it
# emits against the grammar taken verbatim from sitting.sh. PASS means no line fell outside it;
# FAIL means the state is reachable from some input, and the lines are left in the work dir.
# Run on 2026-11-09 it FAILED: Python's \d matches non-ASCII digits, so messageCount=٤٠٠ passes
# the extractor's shape and not the grammar, which then stops the poll loudly.
#
# Usage: REPO=<clone> unreachable_grammar.sh <commit> <work-dir>
set -u
COMMIT=${1:?commit}
WORK=${2:?a work dir, outside any clone}
mkdir -p "$WORK"
HERE=$(cd "$(dirname "$0")" && pwd)
REPO=${REPO:?a clone that holds the commit}
TP=experiment/startup-5d
PY3="py -3.14 -I -X utf8"
G=$(git -C "$REPO" show "$COMMIT:$TP/sitting.sh" | sed -n "s/.*grep -v -q -E '\(.*\)'; then$/\1/p")
[ -n "$G" ] || { echo "the grammar was not found in sitting.sh at $COMMIT" >&2; exit 2; }
git -C "$REPO" show "$COMMIT:$TP/journal_extract.py" > "$WORK/je_commit.py"
echo "extractor at $COMMIT: sha256 $(sha256sum < "$WORK/je_commit.py" | cut -d' ' -f1)"
echo "grammar at $COMMIT: ${#G} characters, from sitting.sh's journal()"
$PY3 "$HERE/hostile_journal.py" > "$WORK/hostile.log" || { echo "the generator failed" >&2; exit 2; }
total=$(wc -l < "$WORK/hostile.log")
cr=$(grep -c $'\r$' "$WORK/hostile.log")
nonascii=$($PY3 -c "import sys; print(sum(1 for l in open(sys.argv[1], 'rb') if any(b > 127 for b in l)))" "$(cygpath -w "$WORK/hostile.log")")
echo "hostile journal: $total lines, sha256 $(sha256sum < "$WORK/hostile.log" | cut -d' ' -f1), $cr ending in CR, $nonascii with non-ASCII bytes"
fails=0
for since in "" "2026-11-09 10:05:00.000"; do
  out=$($PY3 "$(cygpath -w "$WORK/je_commit.py")" "$since" < "$WORK/hostile.log" 2> "$WORK/je_err.txt")
  rc=$?
  n=$(printf '%s\n' "$out" | grep -c .)
  bad=$(printf '%s\n' "$out" | grep -v -E "$G" | grep -c .)
  echo "since='$since': extractor exit $rc, $n lines emitted, $bad outside the grammar | $(cat "$WORK/je_err.txt")"
  [ "$rc" = 0 ] && [ "$n" -gt 0 ] && [ "$bad" = 0 ] || fails=$((fails + 1))
done
echo "emitted, by event and page token (with since=''):"
$PY3 "$(cygpath -w "$WORK/je_commit.py")" "" < "$WORK/hostile.log" 2>/dev/null | awk '{print "  " $3, $4}' | sort | uniq -c | sort -rn
d1='2026-11-09 10:00:00.000 user.marker page=A shownRows=5 channel=x'
d2='2026-11-09 10:00:00.000 user.marker page=A shownRows=5'
echo "the grammar is not vacuous: rejects a doctored line $(echo "$d1" | grep -v -E "$G" | grep -c .) of 1, accepts a real one $(echo "$d2" | grep -E "$G" | grep -c .) of 1"
[ "$fails" = 0 ] && echo "U1 PASS" || { echo "U1 FAIL"; exit 1; }
