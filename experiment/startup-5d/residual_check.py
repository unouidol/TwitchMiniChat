"""The successor to sitting_line.py's CONTRADICTION rule, pre-registered on 2026-11-05 before any further launch.

Usage: python -I -X utf8 residual_check.py <run dir>...   prints one JSON record per run.

The owner's rule (2026-11-05): a wait on an ignored_expired row is a CONTRADICTION unless, in the same
interval, the other page's worker stored an in-window spawn AND the two waits co-terminate within
20 ms; in that case it is recorded as the known live-spawn residual, with the evidence listed.

Read mechanically, as the agent operationalises it:
- a wait is a row of the probe's waitRows; it runs from at - gapNs to at, each plus anchorNs, which is
  CLOCK_MONOTONIC, the clock of logcat's -v monotonic;
- "the other page's worker stored an in-window spawn in the same interval": a logcat line
  `Spawn ingestion source=... outcome=stored`, from the app's process and the other page's history
  thread (pages[].historyTid), whose time falls inside the expired row's wait interval;
- "the two waits co-terminate within 20 ms": the other page has a recorded wait whose end lies within
  20 ms of the expired row's wait's end;
- both: LIVE_SPAWN_RESIDUAL, with the evidence; either missing: CONTRADICTION.
It decides nothing else. A recorded wait still voids its launch as a late delivery under
launchcheck.py's rule of 2026-11-03, which this does not change.
"""
import json
import os
import re
import sys

TOLERANCE_MS = 20.0
LINE = re.compile(r"^\s*(\d+\.\d+)\s+\S+\s+(\d+)\s+(\d+)\s+\w\s+SMART_CATCH_SPAWN:\s+Spawn ingestion source=(\w+) outcome=(\w+)")


def check(run):
    d = json.load(open(os.path.join(run, "probe.json"), encoding="utf-8"))
    w = d.get("waitRows") or {"n": 0}
    anchor = d["anchorNs"]
    tids = {p["slot"]: p["historyTid"] for p in d["pages"]}
    pid = d["mainTid"]
    lines = []
    for raw in open(os.path.join(run, "logcat.txt"), encoding="utf-8", errors="replace"):
        m = LINE.match(raw)
        if m and int(m.group(2)) == pid:
            lines.append({"t": float(m.group(1)), "tid": int(m.group(3)), "source": m.group(4), "outcome": m.group(5)})
    waits = [{"slot": w["slot"][i], "row": w["ordinal"][i], "ms": round(w["gapNs"][i] / 1e6, 1),
              "start_s": (w["at"][i] - w["gapNs"][i] + anchor) / 1e9, "end_s": (w["at"][i] + anchor) / 1e9,
              "outcome": w["spawnOutcome"][i]} for i in range(w["n"])]
    findings = []
    for x in waits:
        if x["outcome"] != "IGNORED_EXPIRED":
            continue
        others = [o for o in waits if o["slot"] != x["slot"]]
        other_tids = [tids[o_slot] for o_slot in tids if o_slot != x["slot"]]
        stored = [l for l in lines if l["outcome"] == "stored" and l["tid"] in other_tids
                  and x["start_s"] <= l["t"] <= x["end_s"]]
        coterm = [o for o in others if abs(o["end_s"] - x["end_s"]) * 1000.0 <= TOLERANCE_MS]
        verdict = "LIVE_SPAWN_RESIDUAL" if stored and coterm else "CONTRADICTION"
        findings.append({
            "verdict": verdict, "expired_wait": x,
            "other_page_stored_lines_in_interval": stored,
            "other_page_waits_ending_within_20_ms": [dict(o, end_apart_ms=round((o["end_s"] - x["end_s"]) * 1000.0, 3))
                                                     for o in coterm],
        })
    return {"run": os.path.basename(os.path.normpath(run)), "waits": waits,
            "ingestion_lines": lines, "expired_row_waits": findings,
            "result": ("CONTRADICTION" if any(f["verdict"] == "CONTRADICTION" for f in findings)
                       else "LIVE_SPAWN_RESIDUAL" if findings else "NONE")}


if __name__ == "__main__":
    for run in sys.argv[1:]:
        print(json.dumps(check(run)))
