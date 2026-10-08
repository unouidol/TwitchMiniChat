"""Step 1: each injected 50 ms delay against three baselines, phase by phase."""
import importlib.util
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("an", os.path.join(HERE, "analyze.py"))
an = importlib.util.module_from_spec(spec)
spec.loader.exec_module(an)

RUNS = os.path.join(HERE, "runs")
BASES = ["r-base-1", "r-base-2", "r-base-3"]
INJECTIONS = ["r-app", "r-inflate", "r-viewcreated", "r-history", "r-build", "r-row", "r-gap",
              "r-spin", "r-spinbg", "r-stall", "r-scroll", "r-everyframe"]


def metrics(run):
    out, d = an.analyze(os.path.join(RUNS, run))
    b = out["boundaries"]
    r = d["rows"]
    p0 = d["pages"][0]
    m = {
        "app_onCreate": b["app_out"] - b["app_in"],
        "activity_onCreate": b["act_out"] - b["act_in"],
        "p0_inflate": b["p0_inflate_out"] - b["p0_inflate_in"],
        "p0_onViewCreated": b["p0_view_out"] - b["p0_view_in"],
        "p0_history_wait": b["p0_history"] - p0["historyRequestAtNs"] / 1e6,
        "span": out["span"], "A": out["A"], "A_build": out["A_build"], "A_insert": out["A_insert"],
        "A_pre": out["A_pre_reconcile"] + out["A_pre_buddy"] + out["A_pre_hidden"],
        "B": out["B"], "C": out["C"], "FSF": out["fsf"],
        "rows_seen": out["rows_seen"],
    }
    for key in ("A_pre_mention", "main_busy_in_span", "B_idle", "B_frames", "B_row_message_overhead", "B_other_messages"):
        if out.get(key) is not None:
            m[key] = out[key]
    if d.get("injectStartNs", -1) >= 0:
        t = d["injectStartNs"] / 1e6
        vs = [v / 1e6 for v in d["frames"]["vsync"]]
        around = [vs[k] - vs[k - 1] for k in range(1, len(vs)) if vs[k - 1] <= t + 60 and vs[k] >= t]
        m["longest_frame_around_injection"] = max(around) if around else None
    # The row the row/build injections aim at: page 0's 100th history row.
    target = [i for i in range(r["n"]) if r["page"][i] == 0 and r["source"][i] == 1 and r["ordinal"][i] == 100]
    if target:
        i = target[0]
        m["row100_build"] = (r["buildOut"][i] - r["buildIn"][i]) / 1e6
        m["row100_insert"] = (r["insertOut"][i] - r["insertIn"][i]) / 1e6
    # The largest gap between one row's end and the next row's start, anywhere in the span.
    rows = sorted((r["begin"][i], r["insertOut"][i]) for i in range(r["n"]) if r["insertOut"][i] >= 0)
    gaps = [(rows[k + 1][0] - rows[k][1]) / 1e6 for k in range(len(rows) - 1)]
    m["max_gap_between_rows"] = max(gaps) if gaps else None
    # CPU: main thread over page 0's onViewCreated; page 0's history thread over the whole window.
    cpu = an.cpu_phases(d, out)
    s = d["snaps"]
    names = d["boundaryNames"]

    def snap_for(name):
        bit = names.index(name)
        for k in range(s["n"]):
            if s["reason"][k] & (1 << bit):
                return k
        return None

    def tid_cpu(k, tid):
        for j in range(s["start"][k], s["start"][k] + s["len"][k]):
            if s["tid"][j] == tid:
                return (s["ut"][j] + s["st"][j]) * an.TICK_MS
        return 0

    ka, kb = snap_for("p0_view_in"), snap_for("p0_view_out")
    if ka is not None and kb is not None:
        m["cpu_main_p0_onViewCreated"] = tid_cpu(kb, d["mainTid"]) - tid_cpu(ka, d["mainTid"])
        m["snap_skew_view_in"] = s["at"][ka] / 1e6 - b["p0_view_in"]
        m["snap_skew_view_out"] = s["at"][kb] / 1e6 - b["p0_view_out"]
    kh = snap_for("p0_history")
    if kh is not None:
        last = s["n"] - 1
        peak = max(tid_cpu(k, p0["historyTid"]) for k in range(kh, s["n"]))
        m["cpu_history0_after_arrival"] = peak - tid_cpu(kh, p0["historyTid"])
    if d.get("injectStartNs", -1) >= 0:
        m["injected_ms"] = (d["injectEndNs"] - d["injectStartNs"]) / 1e6
        m["injected_at"] = d["injectStartNs"] / 1e6
    seen_after_8s = sum(1 for i in range(r["n"]) if r["seenAt"][i] >= 8_000_000_000)
    m["rows_seen_after_8s"] = seen_after_8s
    m["rows_seen_at_frame"] = out["rows_seen_at_frame"]
    m["rows_seen_frame_not_reconcile"] = out["rows_seen_at_frame_not_at_reconcile"]
    m["probe_viewport_ms"] = out["probe_viewport_ms"]
    m["snap_cost_ms"] = s["costNs"] / 1e6
    return m


if __name__ == "__main__":
    base = {run: metrics(run) for run in BASES}
    inj = {run: metrics(run) for run in INJECTIONS}
    keys = list(base[BASES[0]].keys())
    for k in inj.values():
        for key in k:
            if key not in keys:
                keys.append(key)
    print(json.dumps({"base": base, "inject": inj}, default=str), file=open(os.path.join(HERE, "qualify.json"), "w"))
    head = "%-30s" % "metric" + "".join("%11s" % b.replace("r-base-", "b") for b in BASES) + "  spread" + "".join(
        "%13s" % i.replace("r-", "") for i in INJECTIONS)
    print(head)
    for key in keys:
        vals = [base[b].get(key) for b in BASES]
        nums = [v for v in vals if isinstance(v, (int, float))]
        spread = (max(nums) - min(nums)) if nums else None
        line = "%-30s" % key + "".join("%11s" % ("-" if v is None else "%.1f" % v) for v in vals)
        line += "%8s" % ("-" if spread is None else "%.1f" % spread)
        line += "".join("%13s" % ("-" if inj[i].get(key) is None else "%.1f" % inj[i][key]) for i in INJECTIONS)
        print(line)
