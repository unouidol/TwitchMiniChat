"""Caches each counted launch's in-span history rows for the drift analysis: page, ordinal, begin,
and per row the seven segments and the view build, in ms, exactly as analyze5d.py forms them.
Reads recorded probe.json files only.

Usage: python -I drift_rows.py <run dir C> <out.json>
"""
import importlib.util
import json
import os
import sys

C, OUT = sys.argv[1], sys.argv[2]
RUNS = ["Sc-base-1", "Sc-base-2-r1", "Sc-base-3", "Sc-mention", "Sc-reconcile", "Sc-buddy", "Sc-hidden",
        "Sc-beforeinsert", "Sc-insert", "Sc-follow", "Sc-build", "Sc-gap-r2", "Sc-app", "Sc-stall",
        "S5-base-1", "S5-base-2", "S5-base-3", "S5-mention", "S5-reconcile", "S5-buddy-r2", "S5-hidden",
        "S5-beforeinsert", "S5-insert", "S5-follow", "S5-build", "S5-gap", "S5-app", "S5-stall-r1"]
MARKS = [("dedup_backfill_mention", "begin", "mark0"), ("timestamp_echo_reconcile", "mark0", "mark1"),
         ("buddy_capture", "mark1", "mark2"), ("hidden_user_check", "mark2", "insertIn"),
         ("composer_snapshot_follow_decision", "insertIn", "mark3"), ("timeline_insert", "mark3", "mark4"),
         ("follow_or_count_composer_restore", "mark4", "insertOut")]
spec = importlib.util.spec_from_file_location("an", os.path.join(C, "analyze.py"))
an = importlib.util.module_from_spec(spec)
spec.loader.exec_module(an)

res = {}
for run in RUNS:
    out, d = an.analyze(os.path.join(C, "runs", run))
    r, b = d["rows"], d["builds"]
    first, last = out["first_row"], out["last_row"]
    bms = {}
    for k in range(b["n"]):
        if b["end"][k] >= 0 and b["row"][k] >= 0:
            bms[b["row"][k]] = bms.get(b["row"][k], 0.0) + (b["end"][k] - b["start"][k]) / 1e6
    rows = []
    for i in range(r["n"]):
        if not (first <= r["begin"][i] / 1e6 <= last) or r["source"][i] != 1 or r["insertOut"][i] < 0:
            continue
        row = {"page": r["page"][i], "ordinal": r["ordinal"][i], "begin": r["begin"][i] / 1e6,
               "whole": (r["insertOut"][i] - r["begin"][i]) / 1e6,
               "timeline_insert_of_which_view_build": bms.get(i, 0.0)}
        for s, a, z in MARKS:
            row[s] = (r[z][i] - r[a][i]) / 1e6 if r[a][i] >= 0 and r[z][i] >= 0 else 0.0
        rows.append(row)
    res[run] = {"inject": d.get("inject", ""), "inject_start": d["injectStartNs"] / 1e6 if d.get("injectStartNs", -1) >= 0 else None,
                "inject_end": d["injectEndNs"] / 1e6 if d.get("injectStartNs", -1) >= 0 else None,
                "first_row": first, "last_row": last, "rows": rows}
    print(run, len(rows), file=sys.stderr)
json.dump(res, open(OUT, "w", encoding="utf-8"))
