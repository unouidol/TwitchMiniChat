"""TASK 1 of 2026-11-04: the drift across launches, on rows no injection touches. Reads the sitting's
recorded probe.json files; launches nothing and scores nothing (qualify5d.py is not run).

Usage: python -I drift.py <run dir C>   (JSON on stdout)

Row sets, chosen before computing, because no injection reaches them in any of a build's 14 launches:
- P1: page 1's history rows in the span. Every row injection and the control's `build` land in page 0
  (criterion 4's target rows: page 0's row 100, and row 10 for the control's `build`); 5d's `build` is
  a bind outside every row; `gap` holds no row; `app` is before the span and `stall` after it.
- P0early: page 0's history rows with ordinal 11 to 99, which begin before any delay inside a row.
Phases, chosen before computing: on each build, the segments criterion 3 failed on there, and the
whole row (begin to insertOut). Every other segment is printed too, under "all_segments".
Each segment is analyze5d.py's own: the same marks, the same in-span rule, the view build being the
builds attributed to the row.
"""
import itertools
import json
import os
import random
import statistics
import sys

C = sys.argv[1]
ORDER = {
    "control": ["Sc-base-1", "Sc-base-2-r1", "Sc-base-3", "Sc-mention", "Sc-reconcile", "Sc-buddy", "Sc-hidden",
                "Sc-beforeinsert", "Sc-insert", "Sc-follow", "Sc-build", "Sc-gap-r2", "Sc-app", "Sc-stall"],
    "5d": ["S5-base-1", "S5-base-2", "S5-base-3", "S5-mention", "S5-reconcile", "S5-buddy-r2", "S5-hidden",
           "S5-beforeinsert", "S5-insert", "S5-follow", "S5-build", "S5-gap", "S5-app", "S5-stall-r1"],
}
FAILED = {"control": ["timeline_insert", "timeline_insert_of_which_view_build", "follow_or_count_composer_restore"],
          "5d": ["dedup_backfill_mention"]}
MARKS = [("dedup_backfill_mention", "begin", "mark0"), ("timestamp_echo_reconcile", "mark0", "mark1"),
         ("buddy_capture", "mark1", "mark2"), ("hidden_user_check", "mark2", "insertIn"),
         ("composer_snapshot_follow_decision", "insertIn", "mark3"), ("timeline_insert", "mark3", "mark4"),
         ("follow_or_count_composer_restore", "mark4", "insertOut")]

sys.path.insert(0, C)
import importlib.util  # noqa: E402
spec = importlib.util.spec_from_file_location("an", os.path.join(C, "analyze.py"))
an = importlib.util.module_from_spec(spec)
spec.loader.exec_module(an)


def ms(ns):
    return ns / 1e6


def row_sets(run):
    out, d = an.analyze(os.path.join(C, "runs", run))
    r = d["rows"]
    first, last = out["first_row"], out["last_row"]
    rows = [i for i in range(r["n"]) if first <= ms(r["begin"][i]) <= last and r["source"][i] == 1 and r["insertOut"][i] >= 0]
    b = d["builds"]
    build_by_row = {}
    for k in range(b["n"]):
        if b["end"][k] >= 0 and b["row"][k] >= 0:
            build_by_row[b["row"][k]] = build_by_row.get(b["row"][k], 0.0) + ms(b["end"][k] - b["start"][k])
    sets = {"P1": [i for i in rows if r["page"][i] == 1],
            "P0early": [i for i in rows if r["page"][i] == 0 and 11 <= r["ordinal"][i] <= 99]}
    res = {}
    for name, idx in sets.items():
        seg = {}
        for s, a, z in MARKS:
            seg[s] = sum(ms(r[z][i] - r[a][i]) for i in idx if r[a][i] >= 0 and r[z][i] >= 0)
        seg["timeline_insert_of_which_view_build"] = sum(build_by_row.get(i, 0.0) for i in idx)
        seg["timeline_insert_without_view_build"] = seg["timeline_insert"] - seg["timeline_insert_of_which_view_build"]
        totals = [ms(r["insertOut"][i] - r["begin"][i]) for i in idx]
        ages = [ms(r["begin"][i]) for i in idx]
        res[name] = {"rows": len(idx), "whole_row_sum": sum(totals), "whole_row_median_us": statistics.median(totals) * 1000,
                     "age_median_ms": statistics.median(ages), "age_first_last_ms": [min(ages), max(ages)],
                     "all_segments": seg}
    return res


def ranks(v):
    order = sorted(range(len(v)), key=lambda i: v[i])
    rk = [0.0] * len(v)
    i = 0
    while i < len(v):
        j = i
        while j + 1 < len(v) and v[order[j + 1]] == v[order[i]]:
            j += 1
        for k in range(i, j + 1):
            rk[order[k]] = (i + j) / 2 + 1
        i = j + 1
    return rk


def spearman(x, y):
    rx, ry = ranks(x), ranks(y)
    mx, my = statistics.mean(rx), statistics.mean(ry)
    num = sum((a - mx) * (b - my) for a, b in zip(rx, ry))
    den = (sum((a - mx) ** 2 for a in rx) * sum((b - my) ** 2 for b in ry)) ** 0.5
    return num / den


def spearman_p(x, y, n=200000, seed=20261104):
    """Two-sided, by permutation of y, fixed seed."""
    obs = abs(spearman(x, y))
    rnd = random.Random(seed)
    yy = list(y)
    hits = 0
    for _ in range(n):
        rnd.shuffle(yy)
        if abs(spearman(x, yy)) >= obs - 1e-12:
            hits += 1
    return hits / n


def baselines_higher_p(values):
    """Exact, one-sided: of the C(14,3) ways to pick three launches, the share whose rank sum is at
    least the three baselines' (positions 1-3). Under exchangeability of the 14 launches."""
    rk = ranks(values)
    obs = sum(rk[:3])
    combos = list(itertools.combinations(range(len(values)), 3))
    return sum(1 for c in combos if sum(rk[i] for i in c) >= obs - 1e-9) / len(combos), obs


out = {}
for build, runs in ORDER.items():
    per = [row_sets(run) for run in runs]
    pos = list(range(1, len(runs) + 1))
    out[build] = {"launches": runs, "series": {}}
    for rs in ("P1", "P0early"):
        for phase in FAILED[build] + ["whole_row_sum"]:
            vals = [(p[rs]["all_segments"][phase] if phase != "whole_row_sum" else p[rs]["whole_row_sum"]) for p in per]
            steps = [vals[k + 1] - vals[k] for k in range(len(vals) - 1)]
            p_hi, rank_sum = baselines_higher_p(vals)
            base, inj = vals[:3], vals[3:]
            out[build]["series"]["%s:%s" % (rs, phase)] = {
                "values": [round(v, 1) for v in vals],
                "falls_monotonically": all(s < 0 for s in steps),
                "decreasing_steps": sum(1 for s in steps if s < 0), "steps": len(steps),
                "spearman_vs_position": round(spearman(pos, vals), 3),
                "spearman_p_two_sided_perm": spearman_p(pos, vals),
                "baselines": [round(v, 1) for v in base], "injections": [round(v, 1) for v in inj],
                "baselines_median": round(statistics.median(base), 1), "injections_median": round(statistics.median(inj), 1),
                "gap_medians": round(statistics.median(base) - statistics.median(inj), 1),
                "gap_lowest_baseline_minus_highest_injection": round(min(base) - max(inj), 1),
                "injections_above_lowest_baseline": sum(1 for v in inj if v >= min(base)),
                "baselines_rank_sum": rank_sum, "baselines_higher_exact_p": round(p_hi, 4),
            }
    out[build]["rows_and_ages"] = {run: {rs: {k: p[rs][k] for k in ("rows", "age_median_ms", "age_first_last_ms", "whole_row_median_us")}
                                         for rs in ("P1", "P0early")} for run, p in zip(runs, per)}
    out[build]["all_segments"] = {run: {rs: {k: round(v, 1) for k, v in p[rs]["all_segments"].items()} for rs in ("P1", "P0early")}
                                  for run, p in zip(runs, per)}
print(json.dumps(out, indent=1))
