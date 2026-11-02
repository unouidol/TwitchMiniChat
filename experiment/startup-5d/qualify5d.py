"""The qualification's verdict, computed from the recorded values, for one build.

Usage: python -I qualify5d.py <control|5d> <baseline run dir> x3 -- <injected run dir>...

Criteria 1 to 5 of docs/manual-plan-5d-recyclerview.md (*Qualification*), the rows-in-span rule and
the display reading, as written there before any qualification launch. Written on 2026-11-02 so
that the verdict is computed rather than judged; it prints every value it compares beside each
verdict, so that anyone can recheck it from the run directories. Exit status 0 only when the build
is qualified.

Where the plan's words leave a choice, this script makes it, and says so here:

- A phase's value is the one analyze5d.py reports: a sum over the rows in the span for a per-row
  segment (A_parts), B_other_messages for `gap`, the act_in boundary for `app`, and for `build` the
  view build inside the insert on the control (A_parts) and the binds on 5d (view_builds_by_kind).
- Criterion 3's "per-row segments" are A_parts' seven segments and the insert's two parts. A segment
  that contains the injected phase moves with it and is not held to criterion 3: the insert, for
  the view build inside it on the control; the insert less its view build, for `insert`.
- Criterion 4's target row is page 0's history row at the injection's ordinal (its arg, or 100). For
  `build` it is the build, or bind, whose interval overlaps the delay most; on the control, the row
  whose begin-to-insertOut contains that build is the target row of 4's third clause.
- "Inside one message the log recorded" is read against the message that overlaps the delay most;
  the part of the delay outside it must be 1 ms or less. For `gap`, that message must also hold no
  row's begin.
- Criterion 5's bounds are read from the stall's own launch: the recorded end less 1 ms, and the
  first frame the probe recorded after that end. A baseline with no burst at all counts as one whose
  burst ended before the stall.
- The display: every launch's readings, before and after, must be one reading; otherwise the build's
  verdict is VOID, not a failure of the instrument.
"""
import importlib.util
import json
import os
import statistics
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("a5", os.path.join(HERE, "analyze5d.py"))
a5 = importlib.util.module_from_spec(spec)
spec.loader.exec_module(a5)
spec = importlib.util.spec_from_file_location("decide", os.path.join(HERE, "decide.py"))
decide = importlib.util.module_from_spec(spec)
spec.loader.exec_module(decide)

INJECT_MS = 200.0
STALL_MS = 534.0
HISTORY_ORDINAL = 100
INJECTIONS = ("mention", "reconcile", "buddy", "hidden", "beforeinsert", "insert", "follow",
              "build", "gap", "app", "stall")

# The injection's segment of a row: its A_parts key and the row marks around it.
ROW_PHASES = {
    "mention": ("dedup_backfill_mention", "begin", "mark0"),
    "reconcile": ("timestamp_echo_reconcile", "mark0", "mark1"),
    "buddy": ("buddy_capture", "mark1", "mark2"),
    "hidden": ("hidden_user_check", "mark2", "insertIn"),
    "beforeinsert": ("composer_snapshot_follow_decision", "insertIn", "mark3"),
    "insert": ("timeline_insert", "mark3", "mark4"),
    "follow": ("follow_or_count_composer_restore", "mark4", "insertOut"),
}
SEGMENTS = [phase[0] for phase in ROW_PHASES.values()] + [
    "timeline_insert_of_which_view_build", "timeline_insert_without_view_build"]

CRITERION_3_MS = 10.0
CRITERION_4_OUTSIDE_MS = 1.0
CRITERION_4_ROW_MS = 5.0
LOGCAT_RESOLUTION_MS = 1.0


def ms(ns):
    return ns / 1e6


def facts(run):
    """Everything the criteria read from one run directory, in milliseconds from the anchor."""
    result, out, d = a5.breakdown(run)
    r = d["rows"]
    rows = []
    for i in range(r["n"]):
        rows.append({key: (ms(r[key][i]) if r[key][i] >= 0 else None)
                     for key in ("begin", "mark0", "mark1", "mark2", "mark3", "mark4", "insertIn", "insertOut")})
        rows[-1].update(page=r["page"][i], source=r["source"][i], ordinal=r["ordinal"][i])
    builds = d["builds"]
    started = d.get("injectStartNs", -1) >= 0
    return {
        "run": os.path.basename(os.path.normpath(run)),
        "commit": d.get("commit"),
        "inject": d.get("inject", ""),
        "inject_arg": d.get("injectArg", -1),
        "inject_start": ms(d["injectStartNs"]) if started else None,
        "inject_end": ms(d["injectEndNs"]) if started else None,
        "rows_in_span": (result.get("rows_in_span_by_source") or {}).get(1),
        "span": result.get("span"),
        "idle": (result.get("B_parts") or {}).get("B_idle"),
        "segments": result.get("A_parts") or {},
        "other_messages": (result.get("B_parts") or {}).get("B_other_messages"),
        "binds_ms": ((result.get("view_builds_by_kind") or {}).get("bind") or {}).get("ms"),
        "act_in": (out.get("boundaries") or {}).get("act_in"),
        "app_in": (out.get("boundaries") or {}).get("app_in"),
        "app_out": (out.get("boundaries") or {}).get("app_out"),
        "burst_end": result.get("burst_end"),
        "frames_ran": [ms(v) for v in d["frames"]["ran"]],
        "messages": [(ms(a), ms(b)) for a, b in zip(d["msgs"]["start"], d["msgs"]["end"])] if "msgs" in d else [],
        "builds": [(ms(a), ms(b)) for a, b in zip(builds["start"], builds["end"]) if b >= 0],
        "rows": rows,
        "display": decide.display_readings(run),
    }


def outside(interval, container):
    """How much of [interval] lies outside [container], in ms; the whole interval if there is none."""
    (s, e), c = interval, container
    if c is None or c[0] is None or c[1] is None:
        return e - s
    return max(0.0, c[0] - s) + max(0.0, e - c[1])


def best_overlap(interval, containers):
    s, e = interval
    best, most = None, 0.0
    for c in containers:
        overlap = min(e, c[1]) - max(s, c[0])
        if overlap > most:
            best, most = c, overlap
    return best


def row_at(f, ordinal):
    for row in f["rows"]:
        if row["page"] == 0 and row["source"] == 1 and row["ordinal"] == ordinal:
            return row
    return None


def row_segments(row):
    """A row's own segments and total, in ms, from its marks; None where a mark is missing."""
    seg = {}
    for key, a, b in ROW_PHASES.values():
        seg[key] = (row[b] - row[a]) if row[a] is not None and row[b] is not None else None
    seg["total"] = (row["insertOut"] - row["begin"]) if row["begin"] is not None and row["insertOut"] is not None else None
    return seg


def phase_value(build, name, f):
    """The reported value of the injection's target phase; None for `stall`, which criterion 5 reads."""
    if name in ROW_PHASES:
        return f["segments"].get(ROW_PHASES[name][0])
    if name == "build":
        return f["segments"].get("timeline_insert_of_which_view_build") if build == "control" else f["binds_ms"]
    if name == "gap":
        return f["other_messages"]
    if name == "app":
        return f["act_in"]
    return None


def moving_segments(build, name):
    """The per-row segments the injection is expected to move: its own and those containing it."""
    if name == "insert":
        return {"timeline_insert", "timeline_insert_without_view_build"}
    if name == "build" and build == "control":
        return {"timeline_insert_of_which_view_build", "timeline_insert"}
    if name in ROW_PHASES:
        return {ROW_PHASES[name][0]}
    return set()


def evaluate(build, baselines, f):
    """Every criterion that applies to injected launch [f], against [baselines], with its values."""
    name = f["inject"]
    delay = STALL_MS if name == "stall" else INJECT_MS
    c = {}
    interval = (f["inject_start"], f["inject_end"]) if f["inject_start"] is not None else None
    recorded = None if interval is None else interval[1] - interval[0]

    c["1"] = {"recorded_ms": recorded, "at_least_ms": delay,
              "pass": recorded is not None and recorded >= delay}

    if name != "stall":
        values = [phase_value(build, name, b) for b in baselines]
        value = phase_value(build, name, f)
        if value is None or any(v is None for v in values):
            c["2"] = {"value": value, "baselines": values, "pass": False}
        else:
            median = statistics.median(values)
            c["2"] = {"value": value, "baselines": values, "baselines_median": median,
                      "rise": value - median, "at_least": delay / 2, "pass": value - median >= delay / 2}

    moving = moving_segments(build, name)
    held = {}
    for key in SEGMENTS:
        if key in moving:
            continue
        values = [b["segments"].get(key) for b in baselines]
        value = f["segments"].get(key)
        if value is None or any(v is None for v in values):
            held[key] = {"value": value, "baselines": values, "pass": False}
            continue
        low, high = min(values) - CRITERION_3_MS, max(values) + CRITERION_3_MS
        held[key] = {"value": value, "range": [low, high], "pass": low <= value <= high}
    c["3"] = {"segments": held, "pass": all(entry["pass"] for entry in held.values())}

    four = {}
    target_row = None
    if interval is None:
        four["inside_its_phase"] = {"pass": False, "why": "no injection recorded"}
    elif name in ROW_PHASES:
        ordinal = f["inject_arg"] if f["inject_arg"] > 0 else HISTORY_ORDINAL
        target_row = row_at(f, ordinal)
        _, a, b = ROW_PHASES[name]
        phase = None if target_row is None else (target_row[a], target_row[b])
        out_ms = outside(interval, phase)
        four["inside_its_phase"] = {"row_ordinal": ordinal, "phase": phase, "outside_ms": out_ms,
                                    "pass": target_row is not None and out_ms <= CRITERION_4_OUTSIDE_MS}
    elif name == "build":
        build_interval = best_overlap(interval, f["builds"])
        out_ms = outside(interval, build_interval)
        four["inside_its_phase"] = {"build": build_interval, "outside_ms": out_ms,
                                    "pass": build_interval is not None and out_ms <= CRITERION_4_OUTSIDE_MS}
        if build == "control" and build_interval is not None:
            for row in f["rows"]:
                if row["page"] == 0 and row["begin"] is not None and row["insertOut"] is not None and \
                        row["begin"] <= build_interval[0] and build_interval[1] <= row["insertOut"]:
                    target_row = row
                    break
    elif name == "gap":
        message = best_overlap(interval, f["messages"])
        begins = [row["begin"] for row in f["rows"] if row["begin"] is not None]
        holds_a_row = message is not None and any(message[0] <= t <= message[1] for t in begins)
        out_ms = outside(interval, message)
        four["inside_its_phase"] = {"message": message, "holds_a_row": holds_a_row, "outside_ms": out_ms,
                                    "pass": message is not None and not holds_a_row and out_ms <= CRITERION_4_OUTSIDE_MS}
    elif name == "app":
        phase = (f["app_in"], f["app_out"])
        out_ms = outside(interval, phase)
        four["inside_its_phase"] = {"phase": phase, "outside_ms": out_ms, "pass": out_ms <= CRITERION_4_OUTSIDE_MS}

    if name != "app" and interval is not None:
        message = best_overlap(interval, f["messages"])
        out_ms = outside(interval, message)
        four["inside_the_main_threads_log"] = {"message": message, "outside_ms": out_ms,
                                              "pass": message is not None and out_ms <= CRITERION_4_OUTSIDE_MS}

    if target_row is not None:
        ordinal = target_row["ordinal"]
        own = row_segments(target_row)
        theirs = [row_at(b, ordinal) for b in baselines]
        if any(row is None for row in theirs):
            four["the_rows_other_work_unchanged"] = {"row_ordinal": ordinal, "pass": False,
                                                     "why": "a baseline has no history row at this ordinal on page 0"}
        else:
            theirs = [row_segments(row) for row in theirs]
            containing = ROW_PHASES[name][0] if name in ROW_PHASES else "timeline_insert"
            compared = {}
            for key in list(own):
                value = own[key]
                if key == containing:
                    continue
                if key == "total" and value is not None:
                    value = value - recorded
                values = [s[key] for s in theirs]
                if value is None or any(v is None for v in values):
                    compared[key] = {"value": value, "baselines": values, "pass": False}
                    continue
                low, high = min(values) - CRITERION_4_ROW_MS, max(values) + CRITERION_4_ROW_MS
                compared[key] = {"value": value, "range": [low, high], "pass": low <= value <= high}
            four["the_rows_other_work_unchanged"] = {"row_ordinal": ordinal, "segments": compared,
                                                     "pass": all(entry["pass"] for entry in compared.values())}

    spans = [b["span"] for b in baselines]
    idles = [b["idle"] for b in baselines]
    four["the_total_reported"] = {
        "span_rise_over_the_baselines_median": None if f["span"] is None else f["span"] - statistics.median(spans),
        "baselines_span_spread": max(spans) - min(spans),
        "idle_change": None if f["idle"] is None or None in idles else f["idle"] - statistics.median(idles),
    }
    c["4"] = {"clauses": four, "pass": all(v.get("pass", True) for v in four.values())}

    if name == "stall":
        burst = f["burst_end"]
        end = f["inject_end"]
        after = [t for t in f["frames_ran"] if end is not None and t > end]
        first_frame = after[0] if after else None
        placed = f["inject_arg"] if f["inject_arg"] > 0 else None
        before = [b["burst_end"] for b in baselines]
        moved = burst is not None and end is not None and first_frame is not None and \
            end - LOGCAT_RESOLUTION_MS <= burst <= first_frame
        elsewhere = placed is not None and all(v is None or v < placed for v in before)
        c["5"] = {"burst_end": burst, "stall_end": end, "first_frame_after": first_frame,
                  "bounds": [None if end is None else end - LOGCAT_RESOLUTION_MS, first_frame],
                  "stall_placed_at": placed, "baselines_burst_end": before,
                  "the_detector_moves_there": moved, "from_somewhere_else": elsewhere,
                  "pass": moved and elsewhere}
    return {"run": f["run"], "inject": name, "criteria": c, "pass": all(v["pass"] for v in c.values())}


def verdict(build, baseline_facts, injected_facts):
    out = {"build": build, "baselines": [b["run"] for b in baseline_facts]}
    every = baseline_facts + injected_facts

    readings = {f["run"]: f["display"] for f in every}
    distinct = sorted({r for rs in readings.values() for r in rs})
    unread = [run for run, rs in readings.items() if len(rs) != 2 or any(decide.refresh_rate(r) is None for r in rs)]
    out["display"] = {"readings": distinct, "unread": unread, "one_reading": len(distinct) == 1 and not unread}

    rows = {f["run"]: f["rows_in_span"] for f in every}
    base_rows = {b["rows_in_span"] for b in baseline_facts}
    out["rows_in_span"] = {
        "each": rows,
        "baselines_equal": len(base_rows) == 1 and None not in base_rows,
        "injected_differing": [f["run"] for f in injected_facts if f["rows_in_span"] not in base_rows],
    }
    out["baselines_injected_nothing"] = all(b["inject"] == "" and b["inject_start"] is None for b in baseline_facts)
    names = [f["inject"] for f in injected_facts]
    out["injections"] = {"each_once": sorted(names) == sorted(INJECTIONS), "ran": names}

    out["launches"] = [evaluate(build, baseline_facts, f) for f in injected_facts]
    if not out["display"]["one_reading"]:
        out["verdict"] = "VOID: the launches do not share one display reading"
    elif not out["rows_in_span"]["baselines_equal"]:
        out["verdict"] = "STOPPED: the baselines' rows in the span differ (the plan's account case)"
    elif out["rows_in_span"]["injected_differing"]:
        out["verdict"] = "STOPPED: an injection changed which rows are ingested"
    elif not out["baselines_injected_nothing"] or not out["injections"]["each_once"]:
        out["verdict"] = "NOT QUALIFIED: the launches are not three baselines and each injection once"
    elif all(launch["pass"] for launch in out["launches"]):
        out["verdict"] = "QUALIFIED"
    else:
        out["verdict"] = "NOT QUALIFIED: " + ", ".join(
            "%s (criteria %s)" % (launch["inject"], ", ".join(k for k, v in launch["criteria"].items() if not v["pass"]))
            for launch in out["launches"] if not launch["pass"])
    return out


def main(argv):
    build = argv[0]
    assert build in ("control", "5d"), "the first argument names the build: control or 5d"
    split = argv.index("--")
    baseline_dirs, injected_dirs = argv[1:split], argv[split + 1:]
    assert len(baseline_dirs) == 3, "three baselines, then -- and the injected launches"
    out = verdict(build, [facts(p) for p in baseline_dirs], [facts(p) for p in injected_dirs])
    print(json.dumps(out, indent=2))
    return 0 if out["verdict"] == "QUALIFIED" else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
