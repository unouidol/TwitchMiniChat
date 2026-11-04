"""Where a criterion 3 failure's excess sits: one segment, summed by row set, in the failing launch and in
the three baselines, from recorded probe.json files only. It locates; it decides nothing and explains
nothing. The rows and segments are formed as drift_rows.py forms them (analyze5d.py's segments).

Usage: python -I location.py <run dir C> <segment> <baseline> <baseline> <baseline> -- <launch>...
Row sets: page 1; page 0's ordinals 0-10, 11-99, 100 (the row injections' target), 101-399.
"""
import importlib.util
import os
import sys

C, SEG = sys.argv[1], sys.argv[2]
rest = sys.argv[3:]
cut = rest.index("--")
BASE, LAUNCHES = rest[:cut], rest[cut + 1:]
assert len(BASE) == 3 and LAUNCHES, "three baselines, then the launches"
MARKS = {"dedup_backfill_mention": ("begin", "mark0"), "timestamp_echo_reconcile": ("mark0", "mark1"),
         "buddy_capture": ("mark1", "mark2"), "hidden_user_check": ("mark2", "insertIn"),
         "composer_snapshot_follow_decision": ("insertIn", "mark3"), "timeline_insert": ("mark3", "mark4"),
         "follow_or_count_composer_restore": ("mark4", "insertOut")}
SETS = [("page 1", lambda p, o: p == 1), ("p0 0-10", lambda p, o: p == 0 and o <= 10),
        ("p0 11-99", lambda p, o: p == 0 and 11 <= o <= 99), ("p0 100", lambda p, o: p == 0 and o == 100),
        ("p0 101-399", lambda p, o: p == 0 and o >= 101)]
spec = importlib.util.spec_from_file_location("an", os.path.join(C, "analyze.py"))
an = importlib.util.module_from_spec(spec)
spec.loader.exec_module(an)


def by_set(label):
    out, d = an.analyze(os.path.join(C, "runs", label))
    r, b = d["rows"], d["builds"]
    bms = {}
    for k in range(b["n"]):
        if b["end"][k] >= 0 and b["row"][k] >= 0:
            bms[b["row"][k]] = bms.get(b["row"][k], 0.0) + (b["end"][k] - b["start"][k]) / 1e6
    sums = {name: 0.0 for name, _ in SETS}
    for i in range(r["n"]):
        if not (out["first_row"] <= r["begin"][i] / 1e6 <= out["last_row"]) or r["source"][i] != 1 or r["insertOut"][i] < 0:
            continue
        if SEG == "timeline_insert_of_which_view_build":
            v = bms.get(i, 0.0)
        else:
            a, z = MARKS[SEG]
            v = (r[z][i] - r[a][i]) / 1e6 if r[a][i] >= 0 and r[z][i] >= 0 else 0.0
        for name, test in SETS:
            if test(r["page"][i], r["ordinal"][i]):
                sums[name] += v
    sums["total"] = sum(sums[name] for name, _ in SETS)
    return sums


base = [by_set(label) for label in BASE]
print("%s; baselines %s" % (SEG, ", ".join(BASE)))
print("%-12s %28s %8s" % ("row set", "baselines", "highest") + "".join("  %-26s" % label for label in LAUNCHES))
got = {label: by_set(label) for label in LAUNCHES}
for name in [n for n, _ in SETS] + ["total"]:
    vals = [s[name] for s in base]
    line = "%-12s %28s %8.1f" % (name, ", ".join("%.1f" % v for v in vals), max(vals))
    for label in LAUNCHES:
        v = got[label][name]
        line += "  %8.1f (%+6.1f vs highest)" % (v, v - max(vals))
    print(line)
