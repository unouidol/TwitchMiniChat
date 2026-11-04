"""The negative control on criterion 3, as negative-control-prereg-2026-11-04.md fixes it: does criterion
3 fail on launches where no injected delay exists anywhere?

Usage: python -I negctl.py <run dir C> <14 control labels> -- <14 5d labels>   (each build in sitting order)

Criterion 3 is not re-implemented here. Each plain launch is handed to qualify5d.py's own evaluate(),
blob 19d110c, loaded from the run directory, under an injection name that only selects which
segments it holds; only the criterion 3 it returns is read, and everything else evaluate() computes
for that launch is discarded, since no injection exists.
- "gap" holds all nine segments: its site lies in no row. The fixed replica uses it.
- Each of the ten injections criterion 3 applied to on 2026-11-04 (all but "stall") holds its own
  set. The decisive statistic uses them, so that the plain launches are held exactly as those ten were.

What it computes, per build and then for both:
- the section checks qualify5d.py's verdict() makes: one display reading, one battery state, no void
  launch, the same rows in every span; and that every launch injected nothing;
- the fixed replica: positions 1, 8 and 14 build the band, as in the re-qualification, and the other
  eleven are scored with all nine segments held;
- the decisive statistic, P_null. For every band of three of the fourteen (364), the ten held sets
  are given to ten of the remaining eleven launches in every possible way. Counting the launches that
  fail gives, per build, the distribution of the failures a re-qualification-shaped sitting would
  show from these launches' own variation. The two builds' distributions are convolved, and P_null
  is the probability of at least REQUAL_FAILURES failing launches, against the re-qualification's
  3 of 20.
"""
import importlib.util
import itertools
import json
import os
import sys

REQUAL_FAILURES = 3          # 2026-11-04 re-qualification: control mention-r1 and buddy, 5d insert
REQUAL_SCORED_PER_BUILD = 10  # criterion 3 applied to ten injections per build; stall was NOT APPLICABLE
P_NULL_CONSISTENT = 0.05      # P_null at or above this: consistent with noise
BAND = (0, 7, 13)             # positions 1, 8 and 14, zero-based


def load_qualify5d(c):
    spec = importlib.util.spec_from_file_location("q", os.path.join(c, "qualify5d.py"))
    q = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(q)
    return q


def held_names(q):
    """The ten injections criterion 3 applied to, in qualify5d.py's order."""
    return [name for name in q.INJECTIONS if name != "stall"]


def criterion_3(q, build, band, f, name):
    """qualify5d.py's criterion 3 for plain launch [f], against [band], holding [name]'s segments."""
    return q.evaluate(build, band, dict(f, inject=name))["criteria"]["3"]


def assignment_distribution(fails):
    """Distribution of the failure count when the len(fails[0]) held sets are given to distinct launches,
    every injective assignment equally likely. fails[l][k]: launch l fails under held set k (0 or 1).
    Exact, by dynamic programming over the set of launches already used. Returns probabilities by count."""
    n, m = len(fails), len(fails[0])
    dp = {0: [1] + [0] * m}
    for k in range(m):
        nxt = {}
        for mask, ways in dp.items():
            for l in range(n):
                if mask >> l & 1:
                    continue
                add = fails[l][k]
                vec = nxt.setdefault(mask | 1 << l, [0] * (m + 1))
                for c, w in enumerate(ways):
                    if w:
                        vec[c + add] += w
        dp = nxt
    total = [0] * (m + 1)
    for ways in dp.values():
        for c, w in enumerate(ways):
            total[c] += w
    s = sum(total)
    return [w / s for w in total]


def convolve(a, b):
    out = [0.0] * (len(a) + len(b) - 1)
    for i, x in enumerate(a):
        for j, y in enumerate(b):
            out[i + j] += x * y
    return out


def build_result(q, c, build, labels):
    facts = [q.facts(os.path.join(c, "runs", label)) for label in labels]
    band = [facts[i] for i in BAND]
    scored = [f for i, f in enumerate(facts) if i not in BAND]
    # The section checks, by qualify5d.py's own verdict(); its verdict string is not read, since these
    # launches are not three baselines and each injection once.
    v = q.verdict(build, band, [dict(f, inject="gap") for f in scored])
    out = {
        "labels": labels,
        "plain": all(f["inject"] == "" and f["inject_start"] is None for f in facts),
        "display": v["display"], "battery": v["battery"], "void_launches": v["void_launches"],
        "rows_in_span": v["rows_in_span"], "launch_records": v["launch_records"],
    }

    # The fixed replica: all nine segments held, as for a launch whose injection lies in no row.
    replica = []
    for f in scored:
        c3 = criterion_3(q, build, band, f, "gap")
        seg = c3["segments"]
        replica.append({"run": f["run"], "pass": c3["pass"], "segments": seg,
                        "above_the_highest_baseline": sorted(s for s, x in seg.items()
                                                             if x.get("value") is not None and "upper_bound" in x
                                                             and x["value"] > x["upper_bound"] - q.CRITERION_3_MS)})
    values = sum(len(r["segments"]) for r in replica)
    above = sum(len(r["above_the_highest_baseline"]) for r in replica)
    out["fixed_replica"] = {
        "band": [labels[i] for i in BAND], "scored": [f["run"] for f in scored], "launches": replica,
        "failing_launches": [r["run"] for r in replica if not r["pass"]],
        "failing_values": sum(1 for r in replica for x in r["segments"].values() if not x["pass"]),
        "held_values": values,
        "above_the_highest_baseline_before_the_margin": above,
        "fraction_above_the_highest_baseline": above / values if values else None,
        "expected_fraction_if_exchangeable": 0.25,
    }

    # Every band of three; each remaining launch under each of the ten held sets.
    names = held_names(q)
    dist = [0.0] * (len(names) + 1)
    all_nine_failing = 0
    triples = list(itertools.combinations(range(len(facts)), 3))
    for t in triples:
        b = [facts[i] for i in t]
        rest = [facts[i] for i in range(len(facts)) if i not in t]
        fails = [[0 if criterion_3(q, build, b, f, name)["pass"] else 1 for name in names] for f in rest]
        all_nine_failing += sum(row[names.index("gap")] for row in fails)  # "gap" holds all nine
        d = assignment_distribution(fails) if any(map(any, fails)) else [1.0] + [0.0] * len(names)
        dist = [x + y / len(triples) for x, y in zip(dist, d)]
    out["all_bands"] = {
        "bands": len(triples), "launches_outside_each_band": len(facts) - 3,
        "held_sets": names,
        "failure_count_distribution": dist,
        "expected_failing_launches_of_ten": sum(k * p for k, p in enumerate(dist)),
        "all_nine_held_per_launch_failure_rate": all_nine_failing / (len(triples) * (len(facts) - 3)),
    }
    return out


def main(argv):
    c = argv[0]
    cut = argv.index("--")
    labels = {"control": argv[1:cut], "5d": argv[cut + 1:]}
    assert all(len(v) == 14 for v in labels.values()), "fourteen labels per build"
    q = load_qualify5d(c)
    res = {"rule": {"requal_failures": REQUAL_FAILURES, "requal_scored_per_build": REQUAL_SCORED_PER_BUILD,
                    "p_null_consistent_at_or_above": P_NULL_CONSISTENT, "band_positions": [i + 1 for i in BAND]},
           "builds": {b: build_result(q, c, b, labels[b]) for b in ("control", "5d")}}
    blocked = []
    for b, r in res["builds"].items():
        if not r["plain"]:
            blocked.append("VOID: a %s launch carries an injection" % b)
        if not r["display"]["one_reading"]:
            blocked.append("VOID: the %s launches do not share one display reading" % b)
        if not r["battery"]["one_state"]:
            blocked.append("VOID: the %s launches do not share one battery saver and power source state" % b)
        if r["void_launches"]:
            blocked.append("STOPPED: void %s launches were given: %s" % (b, ", ".join(sorted(r["void_launches"]))))
        if not r["rows_in_span"]["baselines_equal"] or r["rows_in_span"]["injected_differing"]:
            blocked.append("STOPPED: the %s launches' rows in the span differ" % b)
    # One display reading and one battery state across both builds as well, as the sitting held them.
    shared = {b: (tuple(r["display"]["readings"]), tuple(r["battery"]["states"])) for b, r in res["builds"].items()}
    if len(set(shared.values())) != 1:
        blocked.append("VOID: the two builds' launches do not share one display reading and battery state")
    combined = convolve(res["builds"]["control"]["all_bands"]["failure_count_distribution"],
                        res["builds"]["5d"]["all_bands"]["failure_count_distribution"])
    p_null = sum(combined[REQUAL_FAILURES:])
    res["combined"] = {"failure_count_distribution_of_twenty": combined, "p_null": p_null}
    if blocked:
        res["verdict"] = "; ".join(blocked)
    elif p_null >= P_NULL_CONSISTENT:
        res["verdict"] = ("CONSISTENT WITH NOISE: P_null %.4f >= %.2f. Criterion 3 cannot discriminate leakage from "
                          "noise at three baselines." % (p_null, P_NULL_CONSISTENT))
    else:
        res["verdict"] = ("TIED TO THE INJECTION: P_null %.4f < %.2f. The re-qualification's three failures are taken "
                          "as real leakage; the next step is to diagnose them, not to measure." % (p_null, P_NULL_CONSISTENT))
    print(json.dumps(res, indent=1))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
