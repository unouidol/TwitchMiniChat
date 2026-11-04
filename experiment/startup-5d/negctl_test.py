"""negctl.py's checks, on synthetic inputs only: no recorded launch is read.

Usage: python -I negctl_test.py <run dir C>   (loads qualify5d.py from there, as negctl.py does)
"""
import importlib.util
import itertools
import math
import os
import random
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("negctl", os.path.join(HERE, "negctl.py"))
nc = importlib.util.module_from_spec(spec)
spec.loader.exec_module(nc)
q = nc.load_qualify5d(sys.argv[1])
failed = 0


def check(name, ok):
    global failed
    print(("ok " if ok else "FAIL ") + name)
    failed += 0 if ok else 1


def brute(fails):
    n, m = len(fails), len(fails[0])
    counts = [0] * (m + 1)
    for perm in itertools.permutations(range(n), m):
        counts[sum(fails[perm[k]][k] for k in range(m))] += 1
    s = sum(counts)
    return [c / s for c in counts]


rng = random.Random(20261104)
for n, m in ((5, 5), (6, 5), (7, 4)):
    for trial in range(3):
        f = [[rng.randint(0, 1) for _ in range(m)] for _ in range(n)]
        d, b = nc.assignment_distribution(f), brute(f)
        check("assignment distribution = brute force, %d launches, %d held sets, trial %d" % (n, m, trial),
              all(abs(x - y) < 1e-12 for x, y in zip(d, b)))

# Eleven launches, ten held sets, three launches failing under every set: choosing ten of eleven, the
# count is 3 unless the left-out launch is a failing one (probability 3/11), then 2.
f = [[1] * 10 if l < 3 else [0] * 10 for l in range(11)]
d = nc.assignment_distribution(f)
check("11 x 10, three always failing: P(3) = 8/11, P(2) = 3/11", abs(d[3] - 8 / 11) < 1e-12 and abs(d[2] - 3 / 11) < 1e-12)
check("11 x 10, distribution sums to 1", abs(sum(d) - 1) < 1e-12)

a, b = [0.5, 0.5], [0.25, 0.5, 0.25]
c = nc.convolve(a, b)
check("convolution", all(abs(x - y) < 1e-12 for x, y in zip(c, [0.125, 0.375, 0.375, 0.125])))

names = nc.held_names(q)
check("ten held sets, stall left out, gap among them", len(names) == 10 and "stall" not in names and "gap" in names)
for build in ("control", "5d"):
    check("%s: gap holds all nine segments" % build, q.moving_segments(build, "gap") == set())
check("364 bands of three among fourteen", math.comb(14, 3) == 364)


def plain(run, **segments):
    seg = {s: 10.0 for s in q.SEGMENTS}
    seg.update(segments)
    return {"run": run, "inject": "", "inject_arg": -1, "inject_start": None, "inject_end": None,
            "rows_in_span": 800, "span": 2000.0, "span_end": 3500.0, "idle": 50.0, "segments": seg,
            "other_messages": 5.0, "binds_ms": 0.0, "act_in": 1200.0, "app_in": 100.0, "app_out": 200.0,
            "burst_end": 4000.0, "frames_ran": [], "messages": [], "builds": [], "rows": []}


band = [plain("b1", timeline_insert=100.0), plain("b2", timeline_insert=105.0), plain("b3", timeline_insert=98.0)]
for build in ("control", "5d"):
    r = nc.criterion_3(q, build, band, plain("at", timeline_insert=115.0), "gap")
    check("%s: exactly at the highest plus 10 ms passes" % build, r["pass"] and r["segments"]["timeline_insert"]["upper_bound"] == 115.0)
    r = nc.criterion_3(q, build, band, plain("above", timeline_insert=115.5), "gap")
    check("%s: 0.5 ms above it fails" % build, not r["pass"])
    r = nc.criterion_3(q, build, band, plain("low", timeline_insert=40.0), "gap")
    check("%s: far below passes, printed below its lower reference" % build,
          r["pass"] and r["segments"]["timeline_insert"]["below_lower_reference"])
    check("%s: gap holds nine segments" % build, len(nc.criterion_3(q, build, band, plain("x"), "gap")["segments"]) == 9)
    r = nc.criterion_3(q, build, band, plain("ins", timeline_insert=500.0), "insert")
    check("%s: under insert's held set, timeline_insert is not held" % build, r["pass"] and "timeline_insert" not in r["segments"])
r = nc.criterion_3(q, "control", band, plain("bld", timeline_insert=500.0), "build")
check("control: under build's held set, timeline_insert is not held", r["pass"] and "timeline_insert" not in r["segments"])
r = nc.criterion_3(q, "5d", band, plain("bld", timeline_insert=500.0), "build")
check("5d: under build's held set, timeline_insert is held", not r["pass"])

print("all cases as expected" if not failed else "%d FAILED" % failed)
sys.exit(1 if failed else 0)
