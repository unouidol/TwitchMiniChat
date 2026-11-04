"""TASK 1, second part, from drift_rows.json: (0) the cache checked against qualify5d.py's printed
segment values; (1) the trend among the eleven injections alone; (2) where each failing segment's
whole-span gap sits, by row set, with the sets' process ages; (3) baselines against injections at
matched process age, every row but the injection's target row, in 100 ms bins of begin.

Usage: python -I drift2.py <drift_rows.json> <published dir with control.json and 5d.json>
"""
import json
import statistics
import sys

rows = json.load(open(sys.argv[1], encoding="utf-8"))
for _rec in rows.values():  # the published rows.json is columnar
    if "columns" in _rec:
        _rec["rows"] = [dict(zip(_rec["columns"], r)) for r in _rec["data"]]
PUB = sys.argv[2]
ORDER = {"control": [k for k in rows if k.startswith("Sc-")], "5d": [k for k in rows if k.startswith("S5-")]}
FAILED = {"control": ["timeline_insert", "timeline_insert_of_which_view_build", "follow_or_count_composer_restore"],
          "5d": ["dedup_backfill_mention"]}
TARGET = {"Sc-build": (0, 10)}  # page, ordinal of criterion 4's target row; the seven row injections: (0, 100)
ROW_INJ = {"mention", "reconcile", "buddy", "hidden", "beforeinsert", "insert", "follow"}


def target(run):
    inj = rows[run]["inject"]
    if run in TARGET:
        return TARGET[run]
    return (0, 100) if inj in ROW_INJ else None


def ranks(v):
    o = sorted(range(len(v)), key=lambda i: v[i])
    rk = [0.0] * len(v)
    for pos, i in enumerate(o):
        rk[i] = pos + 1.0
    return rk


def spearman(x, y):
    rx, ry = ranks(x), ranks(y)
    mx, my = statistics.mean(rx), statistics.mean(ry)
    num = sum((a - mx) * (b - my) for a, b in zip(rx, ry))
    return num / ((sum((a - mx) ** 2 for a in rx) * sum((b - my) ** 2 for b in ry)) ** 0.5)


def val(row, seg):
    if seg == "timeline_insert_without_view_build":
        return row["timeline_insert"] - row["timeline_insert_of_which_view_build"]
    return row[seg]


# (0) the cache against the published values
check = []
for build, runs in ORDER.items():
    pub = json.load(open("%s/%s.json" % (PUB, build), encoding="utf-8"))
    printed = {}
    for x in pub["launches"]:
        c3 = x["criteria"].get("3", {}).get("segments", {})
        for s, v in c3.items():
            printed[(x["run"], s)] = v["value"]
    for (run, s), v in printed.items():
        mine = sum(val(r, s) for r in rows[run]["rows"])
        check.append(abs(mine - v))
print("cache vs qualify5d.py's printed criterion 3 values: %d compared, largest difference %.6f ms" % (len(check), max(check)))

for build, runs in ORDER.items():
    print("\n=====", build)
    pos = list(range(1, 15))
    for seg in FAILED[build] + ["whole"]:
        def setsum(run, pred):
            return sum(val(r, seg) for r in rows[run]["rows"] if pred(r) and (r["page"], r["ordinal"]) != target(run))
        sets = {
            "P1 (page 1)": lambda r: r["page"] == 1,
            "P0early (page 0, 11-99)": lambda r: r["page"] == 0 and 11 <= r["ordinal"] <= 99,
            "P0late (page 0, 101-399)": lambda r: r["page"] == 0 and r["ordinal"] >= 101,
            "P0head (page 0, 0-10)": lambda r: r["page"] == 0 and r["ordinal"] <= 10,
        }
        print("--", seg)
        for name, pred in sets.items():
            v = [setsum(run, pred) for run in runs]
            ages = [statistics.median([r["begin"] for r in rows[run]["rows"] if pred(r)]) for run in runs]
            b, i = v[:3], v[3:]
            print("  %-26s base %s | inj median %.1f [%.1f..%.1f] | gap of medians %+.1f | rho(inj only, pos 4-14) %+.3f | age median base %s inj %.0f [%.0f..%.0f]" % (
                name, [round(x, 1) for x in b], statistics.median(i), min(i), max(i), statistics.median(b) - statistics.median(i),
                spearman(pos[3:], i), [round(a) for a in ages[:3]], statistics.median(ages[3:]), min(ages[3:]), max(ages[3:])))
        whole = [sum(val(r, seg) for r in rows[run]["rows"]) for run in runs]
        print("  %-26s base %s | inj median %.1f | gap of medians %+.1f  (whole span, target row included, = criterion 3's value)" % (
            "all 800 rows", [round(x, 1) for x in whole[:3]], statistics.median(whole[3:]), statistics.median(whole[:3]) - statistics.median(whole[3:])))

    # (3) matched process age: every row but the target row, 100 ms bins, per-launch median per row (us)
    print("-- matched process age: per-row median (us) by 100 ms bin of begin, every row but the target row; bins with >= 10 rows in a launch")
    for seg in FAILED[build][:1] + (["follow_or_count_composer_restore"] if build == "control" else []) + ["whole"]:
        bins = {}
        for run in runs:
            for r in rows[run]["rows"]:
                if (r["page"], r["ordinal"]) == target(run):
                    continue
                bins.setdefault(int(r["begin"] // 100) * 100, {}).setdefault(run, []).append(val(r, seg) * 1000)
        print("   ", seg)
        tally = [0, 0]
        for lo in sorted(bins):
            per = {run: statistics.median(v) for run, v in bins[lo].items() if len(v) >= 10}
            b = [per[r] for r in runs[:3] if r in per]
            i = [per[r] for r in runs[3:] if r in per]
            if len(b) < 2 or len(i) < 3:
                continue
            gap = statistics.median(b) - statistics.median(i)
            tally[0 if gap > 0 else 1] += 1
            below = sum(1 for x in i if x < min(b))
            print("      %4d ms: baselines %d, median %.1f | injections %d, median %.1f | gap %+.1f | injections below the lowest baseline %d/%d" % (
                lo, len(b), statistics.median(b), len(i), statistics.median(i), gap, below, len(i)))
        print("      bins with baselines higher: %d, lower or equal: %d" % tuple(tally))
