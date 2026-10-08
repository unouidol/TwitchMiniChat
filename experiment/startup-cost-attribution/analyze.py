"""Turn one run's probe.json and logcat.txt into the quantities of the report.

Usage: python analyze.py <run dir> [--json]   (prints a summary, or one JSON object)

All times are milliseconds from the anchor (System.nanoTime in attachBaseContext).
"""
import json
import os
import re
import sys

LONG_FRAME_MS = 32.0
STABLE_WINDOW_MS = 2000.0
TICK_MS = 10  # CLK_TCK is 100 on the device


def load(run):
    with open(os.path.join(run, "probe.json"), encoding="utf-8") as f:
        return json.load(f)


def ms(ns):
    return ns / 1e6


def stable_frame(vsync_ms, start_after=None):
    """First frame F whose next 2 s of frames hold no interval over 32 ms; F's interval may be long."""
    n = len(vsync_ms)
    for i in range(1, n):
        t = vsync_ms[i]
        if start_after is not None and t < start_after:
            continue
        if t + STABLE_WINDOW_MS > vsync_ms[-1]:
            return None
        ok = True
        j = i + 1
        # Every interval that starts inside [t, t + 2 s], including the one that crosses its end.
        while j < n and vsync_ms[j - 1] < t + STABLE_WINDOW_MS:
            if vsync_ms[j] - vsync_ms[j - 1] > LONG_FRAME_MS:
                ok = False
                break
            j += 1
        if ok:
            return t
    return None


SKIPPED = re.compile(r"^\s*(\d+\.\d+)\s+.*?\b(\d+)\s+(\d+)\s+I Choreographer: Skipped (\d+) frames")


def skipped_frames(run, pid, anchor_ns):
    """Choreographer's own 'Skipped N frames' lines of this process, on the anchor's clock."""
    out = []
    path = os.path.join(run, "logcat.txt")
    if not os.path.exists(path):
        return out
    for line in open(path, encoding="utf-8", errors="replace"):
        if "Skipped" not in line or "Choreographer" not in line:
            continue
        m = re.match(r"^\s*(\d+\.\d+)\s+(\S+)\s+(\d+)\s+(\d+)\s+\w\s+Choreographer:\s+Skipped (\d+) frames", line)
        if not m:
            continue
        if int(m.group(3)) != pid:
            continue
        out.append((float(m.group(1)) * 1000.0 - anchor_ns / 1e6, int(m.group(5))))
    return out


def burst(skips, window_ms=10000.0):
    """The startup burst: every Skipped line logged within anchor + 10 s; it ends with the last one."""
    inside = [(t, f) for t, f in skips if 0.0 <= t <= window_ms]
    if not inside:
        return None, 0, 0
    return inside[-1][0], sum(f for _, f in inside), len(inside)


def analyze(run):
    d = load(run)
    out = {"run": os.path.basename(run), "inject": d.get("inject", ""), "mode": d["mode"]}
    vs = [ms(v) for v in d["frames"]["vsync"]]
    out["frames"] = len(vs)
    pid = d["mainTid"]
    skips = skipped_frames(run, pid, d["anchorNs"])
    b_end, b_frames, b_lines = burst(skips)
    out["burst_end"] = b_end
    out["burst_frames"] = b_frames
    out["burst_lines"] = b_lines
    out["skipped_lines_total"] = len(skips)
    out["fsf"] = stable_frame(vs)
    if d["mode"] != 2:
        return out, d

    names = d["boundaryNames"]
    bnd = {n: (ms(v) if v >= 0 else None) for n, v in zip(names, d["boundaries"])}
    out["boundaries"] = bnd
    out["process_start"] = ms(d["processStartRelNs"])
    r = d["rows"]
    n = r["n"]
    pages = d["pages"]
    out["pages"] = [{k: p[k] for k in ("slot", "channelFp", "historyCount", "historyRequests", "postsQueued", "postsRun",
                                       "scrollPosts", "scrollPostsFromHistoryRows", "everCurrent", "viewportCalls")}
                    for p in pages]
    for p, src in zip(out["pages"], pages):
        p["viewportMs"] = ms(src["viewportNs"])
        p["account"] = src["accountId"][:8]

    hist = [i for i in range(n) if r["source"][i] == 1]
    built = [i for i in range(n) if r["buildOut"][i] >= 0]
    if not hist:
        return out, d
    first = min(ms(r["begin"][i]) for i in hist)
    last = max(ms(r["insertOut"][i]) for i in hist if r["insertOut"][i] >= 0)
    span = last - first
    out["first_row"], out["last_row"], out["span"] = first, last, span

    def in_span(i):
        return first <= ms(r["begin"][i]) <= last

    def seg(i, a, b):
        if r[a][i] < 0 or r[b][i] < 0:
            return 0.0
        return ms(r[b][i] - r[a][i])

    def row_total(i):
        """Main-thread time of one row: from appendChatLine's start, or appendChatView's for other rows."""
        if r["insertOut"][i] >= 0:
            start = r["begin"][i] if r["source"][i] != 3 else r["insertIn"][i]
            return ms(r["insertOut"][i] - start)
        return 0.0

    rows_span = [i for i in range(n) if in_span(i)]
    A_insert = sum(seg(i, "insertIn", "insertOut") for i in rows_span)
    A_build = sum(seg(i, "buildIn", "buildOut") for i in rows_span)
    if "mark0" in r:
        A_mention = sum(seg(i, "begin", "mark0") for i in rows_span if r["source"][i] != 3)
        A_pre1 = sum(seg(i, "mark0", "mark1") for i in rows_span if r["source"][i] != 3)
    else:
        A_mention = None
        A_pre1 = sum(seg(i, "begin", "mark1") for i in rows_span if r["source"][i] != 3)
    A_pre2 = sum(seg(i, "mark1", "mark2") for i in rows_span)
    A_pre3 = sum(seg(i, "mark2", "buildIn") for i in rows_span)
    A_glue = sum(seg(i, "buildOut", "insertIn") for i in rows_span)
    A = sum(row_total(i) for i in rows_span)
    out["A_pre_mention"] = A_mention
    out.update(A=A, A_insert=A_insert, A_build=A_build, A_pre_reconcile=A_pre1, A_pre_buddy=A_pre2,
               A_pre_hidden=A_pre3, A_glue=A_glue, B=span - A,
               probe_viewport_ms=sum(p["viewportMs"] for p in out["pages"]))
    out["rows_in_span"] = len(rows_span)
    out["rows_in_span_by_source"] = {s: sum(1 for i in rows_span if r["source"][i] == s) for s in (1, 2, 3)}
    out["aborted_in_span"] = sum(1 for i in rows_span if r["abort"][i] != 0)

    # B, split by the main Looper's own messages: idle, frames, the rest of each row's message, other work.
    if "msgs" in d:
        starts = [ms(v) for v in d["msgs"]["start"]]
        ends = [ms(v) for v in d["msgs"]["end"]]
        ran = [ms(v) for v in d["frames"]["ran"]]
        row_begins = sorted(ms(r["begin"][i]) for i in range(n) if r["insertOut"][i] >= 0 and r["source"][i] != 3)
        row_cost = {}
        import bisect
        busy = frames_ms = rowmsg_ms = other_ms = 0.0
        row_msgs = 0
        for a0, b0 in zip(starts, ends):
            lo, hi = max(a0, first), min(b0, last)
            if hi <= lo:
                continue
            dur = hi - lo
            busy += dur
            k = bisect.bisect_left(row_begins, a0)
            has_row = k < len(row_begins) and row_begins[k] <= b0
            kf = bisect.bisect_left(ran, a0)
            has_frame = kf < len(ran) and ran[kf] <= b0
            if has_row:
                rowmsg_ms += dur
                row_msgs += 1
            elif has_frame:
                frames_ms += dur
            else:
                other_ms += dur
        out.update(main_busy_in_span=busy, B_idle=span - busy, B_frames=frames_ms,
                   B_row_message_overhead=rowmsg_ms - A, B_other_messages=other_ms,
                   row_messages_in_span=row_msgs,
                   messages_in_span=sum(1 for a0, b0 in zip(starts, ends) if min(b0, last) > max(a0, first)))

    view_in = [bnd["p%d_view_in" % s] for s in (0, 1) if bnd.get("p%d_view_in" % s) is not None]
    out["view_in"] = min(view_in) if view_in else None
    fsf = out["fsf"]
    out["fsf_after_last_row"] = stable_frame(vs, start_after=last)
    out["C"] = (fsf - out["view_in"] - span) if (fsf is not None and out["view_in"] is not None) else None

    # Visibility, within anchor + 10 s, by the gate's rule, sampled at the gate's reconciles.
    window = 10000.0
    chat_built = [i for i in built if ms(r["begin"][i]) <= window]
    seen = [i for i in chat_built if 0 <= r["seenAt"][i]]
    never = [i for i in chat_built if r["seenAt"][i] < 0]
    req = lambda i: r["reqBuild"][i] + r["reqLater"][i]
    out["rows_built"] = len(chat_built)
    out["rows_built_by_page"] = [sum(1 for i in chat_built if r["page"][i] == s) for s in (0, 1)]
    out["rows_seen"] = len(seen)
    out["rows_seen_by_page"] = [sum(1 for i in seen if r["page"][i] == s) for s in (0, 1)]
    out["rows_in_viewport_any_page"] = sum(1 for i in chat_built if r["inViewportAt"][i] >= 0)
    if "seenFrameAt" in r:  # qualification builds only
        out["rows_seen_at_frame"] = sum(1 for i in chat_built if r["seenFrameAt"][i] >= 0)
        out["rows_seen_at_frame_not_at_reconcile"] = sum(1 for i in chat_built if r["seenFrameAt"][i] >= 0 and r["seenAt"][i] < 0)
    out["emote_requests"] = sum(req(i) for i in chat_built)
    out["emote_requests_build"] = sum(r["reqBuild"][i] for i in chat_built)
    out["emote_requests_later"] = sum(r["reqLater"][i] for i in chat_built)
    out["emote_requests_never_seen"] = sum(req(i) for i in never)
    out["emote_requests_unattributed"] = d["reqUnattributed"]
    A_rows = [i for i in rows_span if r["buildOut"][i] >= 0]
    A_built = sum(row_total(i) for i in A_rows)
    A_never = sum(row_total(i) for i in A_rows if r["seenAt"][i] < 0)
    out["A_built_rows"] = A_built
    out["A_never_seen"] = A_never
    out["A_never_seen_share"] = (A_never / A_built) if A_built else None
    per_row = sorted(row_total(i) for i in A_rows)
    if per_row:
        out["row_ms_median"] = per_row[len(per_row) // 2]
        out["row_ms_p90"] = per_row[int(len(per_row) * 0.9)]
        out["row_ms_max"] = per_row[-1]

    # Qualification: the injected delay, and the row it went into.
    if d.get("injectStartNs", -1) >= 0:  # qualification builds only
        out["inject_ms"] = ms(d["injectEndNs"] - d["injectStartNs"])
        out["inject_at"] = ms(d["injectStartNs"])
    return out, d


def cpu_phases(d, out):
    """Per-thread utime+stime deltas between the snapshots that stand for each boundary."""
    s = d["snaps"]
    names = s["names"]
    at = [ms(v) for v in s["at"]]
    reason = s["reason"]
    bnd_names = d["boundaryNames"]

    def snap_for_bit(bit):
        for k in range(s["n"]):
            if reason[k] & (1 << bit):
                return k
        return None

    def snap_at_or_after(t):
        for k in range(s["n"]):
            if at[k] >= t:
                return k
        return None

    def cpu(k):
        return {s["tid"][j]: (s["ut"][j] + s["st"][j]) * TICK_MS for j in range(s["start"][k], s["start"][k] + s["len"][k])}

    b = out["boundaries"]
    marks = [("process start", None)]
    marks.append(("first snapshot (app_out)", 0))
    for label, key in (("activity onCreate out", "act_out"), ("page 0 view_out", "p0_view_out"),
                       ("page 1 view_out", "p1_view_out")):
        k = snap_for_bit(bnd_names.index(key)) if b.get(key) is not None else None
        if k is not None:
            marks.append((label, k))
    hist = [x for x in ("p0_history", "p1_history") if b.get(x) is not None]
    if hist:
        key = min(hist, key=lambda x: b[x])
        marks.append(("first history response", snap_for_bit(bnd_names.index(key))))
    fr = [x for x in ("p0_first_row", "p1_first_row") if b.get(x) is not None]
    if fr:
        key = min(fr, key=lambda x: b[x])
        marks.append(("first row", snap_for_bit(bnd_names.index(key))))
    lr = [x for x in ("p0_last_row", "p1_last_row") if b.get(x) is not None]
    if lr:
        key = max(lr, key=lambda x: b[x])
        marks.append(("last row", snap_for_bit(bnd_names.index(key))))
    if out.get("fsf") is not None:
        marks.append(("first stable frame", snap_at_or_after(out["fsf"])))
    marks.append(("anchor + 10 s", snap_for_bit(bnd_names.index("anchor_10s"))))
    marks = [(label, k) for label, k in marks if label == "process start" or k is not None]
    # Keep the marks in time order; drop any that would run backwards.
    ordered = [marks[0]]
    for label, k in marks[1:]:
        if ordered[-1][1] is None or at[k] >= at[ordered[-1][1]]:
            ordered.append((label, k))
    phases = []
    for (la, ka), (lb, kb) in zip(ordered, ordered[1:]):
        before = cpu(ka) if ka is not None else {}
        after = cpu(kb)
        delta = {}
        for tid, v in after.items():
            dv = v - before.get(tid, 0)
            if dv > 0:
                name = names.get(str(tid), "?")
                if tid == d["mainTid"]:
                    name = "main"
                for p in d["pages"]:
                    if p["historyTid"] == tid:
                        name = "history-slot%d" % p["slot"]
                delta[name] = delta.get(name, 0) + dv
        phases.append({"from": la, "to": lb,
                       "t_from": None if ka is None else at[ka], "t_to": at[kb],
                       "snap_skew_ms": None, "threads": delta})
    return phases


if __name__ == "__main__":
    run = sys.argv[1]
    out, d = analyze(run)
    if d["mode"] == 2:
        out["cpu"] = cpu_phases(d, out)
    if "--json" in sys.argv:
        print(json.dumps(out))
    else:
        for k, v in out.items():
            if k in ("cpu", "boundaries"):
                continue
            print("%-34s %s" % (k, round(v, 1) if isinstance(v, float) else v))
