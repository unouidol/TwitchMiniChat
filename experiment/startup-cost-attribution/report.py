"""Step 6: the five capture runs, every quantity, with the arithmetic behind A, B and C."""
import importlib.util
import json
import os
import statistics
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("an", os.path.join(HERE, "analyze.py"))
an = importlib.util.module_from_spec(spec)
spec.loader.exec_module(an)
RUNS = [os.path.join(HERE, "runs", "c-%d" % k) for k in range(1, 6)]


def one(run):
    out, d = an.analyze(run)
    b = out["boundaries"]
    q = {}
    q["process start"] = out["process_start"]
    for key in ("app_in", "app_out", "act_in", "act_out", "p0_inflate_in", "p0_inflate_out", "p0_view_in", "p0_view_out",
                "p1_inflate_in", "p1_inflate_out", "p1_view_in", "p1_view_out", "p0_history", "p1_history"):
        q[key] = b[key]
    for p in d["pages"]:
        q["p%d_history_request" % p["slot"]] = p["historyRequestAtNs"] / 1e6
    q["first row (t_first_row)"] = out["first_row"]
    q["last row (t_last_row)"] = out["last_row"]
    q["FSF, burst end (chosen)"] = out["burst_end"]
    q["FSF, 2 s rule (other)"] = out["fsf"]
    q["anchor + 10 s"] = b["anchor_10s"]
    q["span = t_last_row - t_first_row"] = out["span"]
    q["A = per-row main-thread work"] = out["A"]
    q["  A: mention refresh"] = out["A_pre_mention"]
    q["  A: dedup/backfill/echo reconcile"] = out["A_pre_reconcile"]
    q["  A: buddy capture"] = out["A_pre_buddy"]
    q["  A: hidden-user check"] = out["A_pre_hidden"]
    q["  A: build view + start emote loads"] = out["A_build"]
    q["  A: between build and insert"] = out["A_glue"]
    q["  A: inside appendChatView (literal A)"] = out["A_insert"]
    q["B = span - A"] = out["B"]
    q["  B: main thread idle"] = out["B_idle"]
    q["  B: frames (input/anim/layout/draw)"] = out["B_frames"]
    q["  B: rest of each row's message"] = out["B_row_message_overhead"]
    q["  B: other main-thread messages"] = out["B_other_messages"]
    q["  B: of which probe visibility walk"] = out["probe_viewport_ms"]
    view_in = out["view_in"]
    q["t_onViewCreated_entry (first page)"] = view_in
    fsf = out["burst_end"]
    q["C = FSF - t_onViewCreated_entry - span"] = fsf - view_in - out["span"]
    q["  C: before the first row (t_first_row - t_onViewCreated_entry)"] = out["first_row"] - view_in
    q["  C: after the last row (FSF - t_last_row)"] = fsf - out["last_row"]
    q["C with the 2 s rule"] = (out["fsf"] - view_in - out["span"]) if out["fsf"] is not None else None
    q["skipped frames in burst"] = out["burst_frames"]
    q["Skipped lines in burst"] = out["burst_lines"]
    src = out["rows_in_span_by_source"]
    q["rows inserted in span: history"] = src[1]
    q["rows inserted in span: live"] = src[2]
    q["rows inserted in span: other"] = src[3]
    q["history posts run (both pages)"] = sum(p["postsRun"] for p in d["pages"])
    q["main-thread messages carrying rows in span"] = out["row_messages_in_span"]
    q["main-thread messages in span"] = out["messages_in_span"]
    q["scroll-to-bottom posts (whole window)"] = sum(p["scrollPosts"] for p in d["pages"])
    q["scroll-to-bottom posts from history rows"] = sum(p["scrollPostsFromHistoryRows"] for p in d["pages"])
    q["rows built (<= anchor+10 s)"] = out["rows_built"]
    q["  of which page 0 / page 1"] = "%d / %d" % tuple(out["rows_built_by_page"])
    q["rows ever visible"] = out["rows_seen"]
    q["  of which page 0 / page 1"] = "%d / %d" % tuple(out["rows_seen_by_page"])
    q["rows never visible"] = out["rows_built"] - out["rows_seen"]
    q["rows in a viewport, page current or not"] = out["rows_in_viewport_any_page"]
    q["emote requests started"] = out["emote_requests"]
    q["  at build / later (static fallback)"] = "%d / %d" % (out["emote_requests_build"], out["emote_requests_later"])
    q["emote requests for never-visible rows"] = out["emote_requests_never_seen"]
    q["A of built rows"] = out["A_built_rows"]
    q["A of never-visible rows"] = out["A_never_seen"]
    q["share of A from never-visible rows"] = out["A_never_seen_share"]
    q["per-row ms: median / p90 / max"] = "%.2f / %.2f / %.1f" % (out["row_ms_median"], out["row_ms_p90"], out["row_ms_max"])
    q["probe snapshots / probe-thread snapshot ms"] = "%d / %.0f" % (d["snaps"]["n"], d["snaps"]["costNs"] / 1e6)
    h = os.path.join(run, "harness.json")
    if os.path.exists(h):
        hj = json.load(open(h))
        if "withGeckoNs" in hj:
            w = [v / 1e6 for v in hj["withGeckoNs"]]
            wo = [v / 1e6 for v in hj["withoutGeckoNs"]]
            q["harness: inflate with GeckoView, median ms"] = statistics.median(w)
            q["harness: inflate with a View stub, median ms"] = statistics.median(wo)
            q["harness: difference of medians, ms"] = statistics.median(w) - statistics.median(wo)
            q["harness: paired differences, median ms"] = statistics.median([a - c for a, c in zip(w, wo)])
    out["fsf"] = out["burst_end"]  # per-thread CPU is cut at the chosen definition
    cpu = an.cpu_phases(d, out)
    return q, cpu, out, d


def fmt(v):
    if v is None:
        return "-"
    if isinstance(v, float):
        if abs(v) < 1 and v != 0:
            return "%.3f" % v
        return "%.0f" % v
    return str(v)


if __name__ == "__main__":
    results = [one(run) for run in RUNS]
    keys = list(results[0][0].keys())
    lines = ["| quantity | c-1 | c-2 | c-3 | c-4 | c-5 | min-max |", "|---|---|---|---|---|---|---|"]
    for key in keys:
        vals = [r[0].get(key) for r in results]
        nums = [v for v in vals if isinstance(v, (int, float))]
        spread = ("%s-%s" % (fmt(min(nums)), fmt(max(nums)))) if len(nums) == len(vals) else ""
        lines.append("| %s | %s | %s |" % (key, " | ".join(fmt(v) for v in vals), spread))
    open(os.path.join(HERE, "capture-table.md"), "w", encoding="utf-8").write("\n".join(lines) + "\n")
    print("\n".join(lines))
    # Per-thread CPU per phase: the median over the five runs, and the range.
    phases = {}
    order = []
    for _, cpu, _, _ in results:
        for ph in cpu:
            key = "%s -> %s" % (ph["from"], ph["to"])
            if key not in phases:
                phases[key] = []
                order.append(key)
            phases[key].append(ph["threads"])
    cl = []
    for key in order:
        runs = phases[key]
        names = set()
        for t in runs:
            names.update(t.keys())
        rows = []
        for name in names:
            vals = [t.get(name, 0) for t in runs]
            rows.append((statistics.median(vals), name, vals))
        rows.sort(reverse=True)
        cl.append("### %s  (%d of 5 runs)" % (key, len(runs)))
        cl.append("| thread | median ms | per run |")
        cl.append("|---|---|---|")
        for med, name, vals in rows[:12]:
            if med < 10 and max(vals) < 20:
                continue
            cl.append("| %s | %.0f | %s |" % (name, med, " ".join("%d" % v for v in vals)))
        cl.append("")
    open(os.path.join(HERE, "capture-cpu.md"), "w", encoding="utf-8").write("\n".join(cl) + "\n")
    print("\n".join(cl))
    json.dump([{"q": r[0], "cpu": r[1]} for r in results], open(os.path.join(HERE, "capture.json"), "w"), default=str)
