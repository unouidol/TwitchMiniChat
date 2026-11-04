"""The re-qualification's predictions P1 and P2, computed as requalification-prereg-2026-11-04.md fixes
them, from a sitting's recorded launches. It decides no criterion; qualify5d.py does that.

Usage: python -I requal_p1.py <run dir C> <control|5d> <label> x14   (in sitting order)

The baselines are the three launches whose flag carries no inject=. The phase is page 1's in-span
history rows, which no injection touches: timeline_insert on the control, dedup_backfill_mention on
5d, each summed over those rows as analyze5d.py forms it.
- P1 holds when the exact probability that three launches drawn at random from the fourteen have a
  rank sum at least the baselines' is above 0.05 (ranks ascending, ties averaged).
- P2 holds when the first baseline in sitting order has the highest of the three values.
"""
import importlib.util
import itertools
import json
import os
import sys

C, BUILD, LABELS = sys.argv[1], sys.argv[2], sys.argv[3:]
assert BUILD in ("control", "5d") and len(LABELS) == 14, "a build and fourteen labels"
PHASE = {"control": ("timeline_insert", "mark3", "mark4"), "5d": ("dedup_backfill_mention", "begin", "mark0")}[BUILD]
spec = importlib.util.spec_from_file_location("an", os.path.join(C, "analyze.py"))
an = importlib.util.module_from_spec(spec)
spec.loader.exec_module(an)


def value(label):
    out, d = an.analyze(os.path.join(C, "runs", label))
    r = d["rows"]
    _, a, z = PHASE
    total = 0.0
    for i in range(r["n"]):
        if (out["first_row"] <= r["begin"][i] / 1e6 <= out["last_row"] and r["source"][i] == 1 and r["page"][i] == 1
                and r["insertOut"][i] >= 0 and r[a][i] >= 0 and r[z][i] >= 0):
            total += (r[z][i] - r[a][i]) / 1e6
    return total, d.get("inject", "")


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


vals, injects = zip(*(value(label) for label in LABELS))
base = [i for i, inj in enumerate(injects) if inj == ""]
assert len(base) == 3, "three baselines, found %d" % len(base)
rk = ranks(list(vals))
obs = sum(rk[i] for i in base)
combos = list(itertools.combinations(range(14), 3))
p = sum(1 for c in combos if sum(rk[i] for i in c) >= obs - 1e-9) / len(combos)
first = min(base)
print(json.dumps({
    "build": BUILD, "phase": PHASE[0] + ", page 1's in-span history rows",
    "launches": [{"position": k + 1, "label": LABELS[k], "inject": injects[k], "value_ms": round(vals[k], 1),
                  "rank": rk[k]} for k in range(14)],
    "baselines_positions": [i + 1 for i in base], "baselines_rank_sum": obs,
    "exact_p_baselines_rank_this_high": round(p, 4), "P1_holds": p > 0.05,
    "P2_first_baseline_highest": all(vals[first] >= vals[i] for i in base),
}, indent=1))
