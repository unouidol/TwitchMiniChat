"""The 5d closing measurement's decision rule, as approved before any number was seen.

Usage: python -I decide.py <control run dir> x5 -- <5d run dir> x5

For the span and for the first stable frame (the burst end), every pairing of a 5d launch with a
control launch gives a ratio, 5d over control: 25 ratios. The threshold is the midpoint between no
change and the prediction, (1 + rho) / 2, with rho the prediction's own ratio: span 1.0 / 2.4, first
stable frame 2.6 / 4.0. rho is fixed: it is not re-derived from what the control gives.

- every ratio below the threshold: HOLDS;
- every ratio above it: FAILS;
- ratios on both sides: INCONCLUSIVE, and the round stops. Recorded with all ten values and every
  pairing; the rule is not re-derived, no launches are added, no definition changes.

Positive controls: every control launch must show a burst, or the first-stable-frame comparison is
void (it qualifies the instrument); a 5d launch with no burst passes it (the success the step is
for). Every launch must insert the same number of history rows in its span, or the run is void.

Skipped frames: "fell" only if every 5d launch's total is below every control launch's. "A great
deal" cannot be tested without a number, and none is given.
"""
import importlib.util
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("a5", os.path.join(HERE, "analyze5d.py"))
a5 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(a5)

RHO = {"span": 1.0 / 2.4, "burst_end": 2.6 / 4.0}
THRESHOLD = {key: (1.0 + rho) / 2.0 for key, rho in RHO.items()}


def classify(ratios, threshold):
    below = [x < threshold for x in ratios]
    if all(below):
        return "HOLDS"
    if not any(below):
        return "FAILS"
    return "INCONCLUSIVE"


def main(argv):
    split = argv.index("--")
    control_dirs, treatment_dirs = argv[:split], argv[split + 1:]
    assert len(control_dirs) == 5 and len(treatment_dirs) == 5, "five launches each, control first"
    control = [a5.breakdown(path)[0] for path in control_dirs]
    treatment = [a5.breakdown(path)[0] for path in treatment_dirs]
    out = {"rho": RHO, "threshold": THRESHOLD, "control": {}, "treatment": {}, "pairings": {}, "verdicts": {}}
    for key in ("span", "burst_end", "burst_frames"):
        out["control"][key] = [c[key] for c in control]
        out["treatment"][key] = [t[key] for t in treatment]

    rows = [(c["rows_in_span_by_source"] or {}).get("1") or (c["rows_in_span_by_source"] or {}).get(1) for c in control + treatment]
    out["history_rows_in_span"] = rows
    out["void_rows"] = len(set(rows)) != 1 or None in rows
    out["void_first_stable_frame"] = any(c["burst_end"] is None for c in control)

    # Span: every pairing, 5d over control.
    span_ratios = [[t["span"] / c["span"] for c in control] for t in treatment]
    out["pairings"]["span"] = span_ratios
    out["verdicts"]["span"] = "VOID" if out["void_rows"] else classify([x for row in span_ratios for x in row], THRESHOLD["span"])

    # First stable frame: a 5d launch with no burst counts as below; a control launch with none voids the comparison.
    if out["void_first_stable_frame"]:
        out["verdicts"]["burst_end"] = "VOID: a control launch showed no burst"
    else:
        fsf = [[(t["burst_end"] / c["burst_end"]) if t["burst_end"] is not None else "no burst" for c in control]
               for t in treatment]
        out["pairings"]["burst_end"] = fsf
        flat = [x if x != "no burst" else 0.0 for row in fsf for x in row]
        out["verdicts"]["burst_end"] = "VOID" if out["void_rows"] else classify(flat, THRESHOLD["burst_end"])

    # Skipped frames: all below all, or not shown to fall. No number for "a great deal".
    worst_treatment = max(t["burst_frames"] for t in treatment)
    best_control = min(c["burst_frames"] for c in control)
    out["verdicts"]["burst_frames"] = "FELL" if worst_treatment < best_control else "NOT SHOWN TO FALL"
    out["verdicts"]["burst_frames_note"] = "'a great deal' cannot be tested without a number, and none was given"
    print(json.dumps(out, indent=2))


if __name__ == "__main__":
    main(sys.argv[1:])
