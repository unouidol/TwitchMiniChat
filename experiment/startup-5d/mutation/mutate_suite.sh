#!/usr/bin/env bash
# Mutation check of sitting_suite.sh. For each mutant of apply_mutant.py: a fresh copy of the
# tools is taken from <commit>, the mutant is applied, the WHOLE suite runs on it with the "new"
# expectations, and the tools are then restored from the commit and verified by sha256 against
# the committed blob. A mutant is killed when at least one case fails, or when the suite times out
# inside a case, which is then named: a case that never ends has not passed.
#
# Usage: REPO=<clone> SUITE=<sitting_suite.sh> mutate_suite.sh <commit> <work-dir> <jobs> [ids...]
#   TIMEOUT (seconds, default 900) bounds each suite run; PY is passed to the suite.
set -u
COMMIT=${1:?commit}; WORK=${2:?work dir}; JOBS=${3:?jobs}; shift 3
HERE=$(cd "$(dirname "$0")" && pwd)
SUITE=${SUITE:?path to sitting_suite.sh}
REPO=${REPO:?a clone that holds the commit}
export PY="${PY:-py -3.14 -I -X utf8}"
TP="experiment/startup-5d"
mkdir -p "$WORK"
want_s=$(git -C "$REPO" show "$COMMIT:$TP/sitting.sh" | sha256sum | cut -d' ' -f1)
want_j=$(git -C "$REPO" show "$COMMIT:$TP/journal_extract.py" | sha256sum | cut -d' ' -f1)
if [ $# -gt 0 ]; then ids=("$@"); else mapfile -t ids < <($PY "$HERE/apply_mutant.py" list | cut -f1); fi
CASES="C1 C2 C3 C4 C5 C6 C7 C8 C9 C10 C11 C12 C13 C14 C15 R1 R2 P1 P2 P3 P4 P5 P6 L"

one() {
  local id=$1 d="$WORK/$1" rc s j restored failed last next ran
  mkdir -p "$d/tools" "$d/work"
  for f in sitting.sh journal_extract.py; do git -C "$REPO" show "$COMMIT:$TP/$f" > "$d/tools/$f"; done
  if ! $PY "$HERE/apply_mutant.py" "$(cygpath -w "$d/tools")" "$id" > "$d/applied.txt" 2>&1; then
    echo "$id NOT APPLIED: $(tr '\n' ' ' < "$d/applied.txt")" > "$d/result.txt"; return
  fi
  { sha256sum < "$d/tools/sitting.sh"; sha256sum < "$d/tools/journal_extract.py"; } | cut -d' ' -f1 | tr '\n' ' ' > "$d/mutated-sha256.txt"
  timeout "${TIMEOUT:-900}" bash "$SUITE" "$d/tools" "$d/work" new > "$d/suite.txt" 2>&1
  rc=$?
  for f in sitting.sh journal_extract.py; do git -C "$REPO" show "$COMMIT:$TP/$f" > "$d/tools/$f"; done
  s=$(sha256sum < "$d/tools/sitting.sh" | cut -d' ' -f1); j=$(sha256sum < "$d/tools/journal_extract.py" | cut -d' ' -f1)
  if [ "$s" = "$want_s" ] && [ "$j" = "$want_j" ]; then restored="restored: both sha256 equal the blob"; else restored="RESTORE MISMATCH"; fi
  failed=$(grep -E '^(C[0-9]+|R[0-9]|P[0-9]|L) ' "$d/suite.txt" | grep -F ' FAIL' | awk '{print $1}' | tr '\n' ' ')
  ran=$(grep -cE '^(C[0-9]+|R[0-9]|P[0-9]|L) ' "$d/suite.txt")
  if [ "$rc" = 124 ]; then
    last=$(grep -E '^(C[0-9]+|R[0-9]|P[0-9]|L) ' "$d/suite.txt" | tail -1 | awk '{print $1}')
    next=$(echo "$CASES" | tr ' ' '\n' | grep -A1 -x "${last:-NONE}" | tail -1)
    [ -n "$last" ] || next=C1
    failed="${failed}TIMEOUT-in-$next "
  fi
  echo "$id suite_exit=$rc cases_reported=$ran killed_by=[${failed% }] | $restored" > "$d/result.txt"
}

running=0
for id in "${ids[@]}"; do
  one "$id" &
  running=$((running + 1))
  if [ "$running" -ge "$JOBS" ]; then wait -n; running=$((running - 1)); fi
done
wait
for id in "${ids[@]}"; do cat "$WORK/$id/result.txt"; done
