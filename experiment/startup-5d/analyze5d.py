"""One run of the 5d measurement: the quantities of the decision rule, and the span broken down.

Usage: python -I analyze5d.py <run dir> [--json]

The span, the burst end (the first stable frame in force) and the skipped frames are computed by
analyze.py, the script of experiment/startup-cost-attribution at a091809, copied here unchanged
(blob 19baef62), so the definitions cannot drift from the ones the prediction was made with. This
file only adds what the ported probe records beyond a091809: the marks around the timeline insert,
the view builds wherever they ran, and the probe's own counts.

All times are milliseconds from the anchor (System.nanoTime in attachBaseContext).
"""
import importlib.util
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("an", os.path.join(HERE, "analyze.py"))
an = importlib.util.module_from_spec(spec)
spec.loader.exec_module(an)

BUILD_KINDS = {1: "create_and_bind", 2: "create", 3: "bind"}


def ms(ns):
    return ns / 1e6


def breakdown(run):
    out, d = an.analyze(run)
    result = {
        "run": os.path.basename(run),
        "commit": d.get("commit"),
        "flag": d.get("flag"),
        "visibility": d.get("visibility"),
        # The decision rule's three quantities, as analyze.py defines them.
        "span": out.get("span"),
        "burst_end": out.get("burst_end"),
        "burst_frames": out.get("burst_frames"),
        "burst_lines": out.get("burst_lines"),
        "first_row": out.get("first_row"),
        "last_row": out.get("last_row"),
        "rows_in_span_by_source": out.get("rows_in_span_by_source"),
    }
    if d["mode"] != 2 or out.get("span") is None:
        return result, out, d
    r = d["rows"]
    n = r["n"]
    first, last = out["first_row"], out["last_row"]

    def seg(i, a, b):
        if r[a][i] < 0 or r[b][i] < 0:
            return 0.0
        return ms(r[b][i] - r[a][i])

    in_span = [i for i in range(n) if first <= ms(r["begin"][i]) <= last]
    rows = [i for i in in_span if r["source"][i] != 3 and r["insertOut"][i] >= 0]
    builds = d["builds"]
    b_n = builds["n"]
    b_rows = builds["row"]
    b_start = [ms(v) for v in builds["start"]]
    b_end = [ms(v) for v in builds["end"]]
    in_rows = set(in_span)
    build_in_rows = sum(b_end[k] - b_start[k] for k in range(b_n) if b_end[k] >= 0 and b_rows[k] in in_rows)
    in_window = [k for k in range(b_n) if b_rows[k] < 0 and b_end[k] >= 0 and first <= b_start[k] <= last]
    by_kind = {}
    for k in range(b_n):
        if b_end[k] < 0:
            continue
        name = BUILD_KINDS.get(builds["kind"][k], str(builds["kind"][k]))
        entry = by_kind.setdefault(name, {"count": 0, "ms": 0.0, "inSpanWindow": 0, "inSpanWindowMs": 0.0,
                                          "insideARow": 0, "emoteRequests": 0})
        entry["count"] += 1
        entry["ms"] += b_end[k] - b_start[k]
        entry["emoteRequests"] += builds["req"][k] + builds["reqLater"][k]
        if b_rows[k] >= 0:
            entry["insideARow"] += 1
        if first <= b_start[k] <= last:
            entry["inSpanWindow"] += 1
            entry["inSpanWindowMs"] += b_end[k] - b_start[k]

    insert = sum(seg(i, "mark3", "mark4") for i in rows)
    a = {
        "dedup_backfill_mention": sum(seg(i, "begin", "mark0") for i in rows),
        "timestamp_echo_reconcile": sum(seg(i, "mark0", "mark1") for i in rows),
        "buddy_capture": sum(seg(i, "mark1", "mark2") for i in rows),
        "hidden_user_check": sum(seg(i, "mark2", "insertIn") for i in rows),
        "composer_snapshot_follow_decision": sum(seg(i, "insertIn", "mark3") for i in rows),
        "timeline_insert": insert,
        "timeline_insert_of_which_view_build": build_in_rows,
        "timeline_insert_without_view_build": insert - build_in_rows,
        "follow_or_count_composer_restore": sum(seg(i, "mark4", "insertOut") for i in rows),
    }
    a_total = sum(ms(r["insertOut"][i] - r["begin"][i]) for i in rows)
    a["sum_of_the_parts"] = a_total and (a["dedup_backfill_mention"] + a["timestamp_echo_reconcile"] + a["buddy_capture"] +
                                         a["hidden_user_check"] + a["composer_snapshot_follow_decision"] + insert +
                                         a["follow_or_count_composer_restore"])
    result["A"] = a_total
    result["A_parts"] = a
    result["B"] = out["span"] - a_total
    result["B_parts"] = {key: out.get(key) for key in ("B_idle", "B_frames", "B_row_message_overhead", "B_other_messages")}
    result["view_builds_outside_rows_in_span_window"] = {
        "count": len(in_window), "ms": sum(b_end[k] - b_start[k] for k in in_window)}
    result["view_builds_by_kind"] = by_kind
    result["rows_in_span"] = len(rows)
    result["emote_requests"] = {
        "during_builds": sum(builds["req"]), "later_by_build": sum(builds["reqLater"]),
        "later_by_row": sum(r["reqLater"]), "unattributed": d["reqUnattributed"]}
    # The probe's own work, counted, for the overhead comparison between the two builds.
    pages = d["pages"]
    result["probe"] = {
        "viewport_calls": sum(p["viewportCalls"] for p in pages),
        "viewport_ms": sum(ms(p["viewportNs"]) for p in pages),
        "snapshots": d["snaps"]["n"],
        "snapshot_ms_probe_thread": ms(d["snaps"]["costNs"]),
        "messages_in_span": out.get("messages_in_span"),
        "rows_recorded": n,
        "builds_recorded": b_n,
        "builds_in_span_window": sum(1 for k in range(b_n) if first <= b_start[k] <= last),
        "frames": out.get("frames"),
    }
    cal_path = os.path.join(run, "calibration.json")
    if os.path.exists(cal_path):
        result["calibration"] = json.load(open(cal_path, encoding="utf-8"))
    return result, out, d


if __name__ == "__main__":
    result, _, _ = breakdown(sys.argv[1])
    if "--json" in sys.argv:
        print(json.dumps(result))
    else:
        for key, value in result.items():
            print("%-44s %s" % (key, json.dumps(value) if isinstance(value, (dict, list)) else value))
