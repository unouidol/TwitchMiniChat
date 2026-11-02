"""Whether one launch of the 5d measurement counts, read from its own run directory.

Usage: python -I launchcheck.py <run dir>...   prints each launch's record; exit status 1 if any is void.

The conditions, each decided before the launches it applies to (docs/manual-plan-5d-recyclerview.md,
*Decided on 2026-11-02, after the qualification*). A void launch is kept under its own name and run
again; it is never scored.

- More than FRAMES_IN_SPAN_MAX frames drawn inside the span: late delivery. The span is analyze.py's,
  from the earliest begin of a history row to the latest insertOut of one, both ends included, and a
  frame is one the probe recorded as run (frames.ran).
- A process start other than the app's own between the launch and the end of the span. The launch is
  the app's own `ActivityManager: Start proc` line; a process is the app's own when it is named after
  the app's package, or started for a component of it.
- Any input event in getevent.txt.
- No install immediately before the launch whose read-back from the phone has the APK's own hash:
  every launch is a first launch after install, in both arms.
- Battery saver or the power source changing between the launch's two readings. The level and the
  temperature are recorded, not held: the phone charges during the round, so both move. The callers
  hold battery saver and the power source to one state across a whole section.
- The display reading changing between before and after; the callers hold one reading across a
  whole section.

Recorded beside them, deciding nothing: each page's channel fingerprint and history count, whether
both reached the cap, the live rows, the compile state, the JIT thread's CPU across the span, the
history rows' cost by process age, and anything the history-wait reads caught.
"""
import json
import os
import re
import statistics
import sys

PKG = "com.fs.twitchminichat.dev"
FRAMES_IN_SPAN_MAX = 5
HISTORY_CAP = 400
SRC_HISTORY, SRC_LIVE = 1, 2
REASON_HISTORY_WAIT = 1 << 61
TICK_MS = 10.0
AGE_BIN_MS = 250.0
START_PROC = re.compile(r"^\s*(\d+\.\d+)\s+.*ActivityManager: Start proc (\d+):([^/\s]+)/\S+ for (.*)$")
STARTED_FOR = re.compile(r"\{([^/}]+)/")


def ms(ns):
    return ns / 1e6


def read_lines(run, name):
    path = os.path.join(run, name)
    if not os.path.exists(path):
        return None
    with open(path, encoding="utf-8", errors="replace") as f:
        return [line.rstrip("\r\n") for line in f]


def span_of(d):
    """The span's two ends, in ms from the anchor: the earliest begin and the latest insertOut of a history row."""
    r = d["rows"]
    history = [i for i in range(r["n"]) if r["source"][i] == SRC_HISTORY]
    ended = [ms(r["insertOut"][i]) for i in history if r["insertOut"][i] >= 0]
    if not history or not ended:
        return None, None
    return min(ms(r["begin"][i]) for i in history), max(ended)


def frames_in_span(d, lo, hi):
    if lo is None:
        return None
    return sum(1 for t in d["frames"]["ran"] if lo <= ms(t) <= hi)


def process_starts(lines, anchor_ns):
    """Every `Start proc` line, as (ms from the anchor, pid, process name, the package it was started for)."""
    out = []
    for line in lines or []:
        m = START_PROC.match(line)
        if m:
            started_for = STARTED_FOR.search(m.group(4))
            out.append((float(m.group(1)) * 1000.0 - anchor_ns / 1e6, int(m.group(2)), m.group(3),
                        started_for.group(1) if started_for else None))
    return out


def is_the_apps_own(name, for_package):
    return name == PKG or name.startswith(PKG + ":") or for_package == PKG


def foreign_starts(starts, span_end):
    """Process starts other than the app's own between the app's own start and the end of the span."""
    own = [t for t, _, name, pkg in starts if name == PKG]
    if not own or span_end is None:
        return None
    begin = min(own)
    return [{"at_ms": round(t, 1), "process": name, "for": pkg} for t, _, name, pkg in starts
            if begin <= t <= span_end and not is_the_apps_own(name, pkg)]


def battery_readings(run):
    """battery.txt's two readings, each as a dict of its words."""
    out = []
    for line in read_lines(run, "battery.txt") or []:
        if ": " not in line:
            continue
        when, rest = line.split(": ", 1)
        words = dict(word.split("=", 1) for word in rest.split() if "=" in word)
        words["when"] = when
        out.append(words)
    return out


def battery_state(reading):
    """What a section holds: battery saver and the power source. The level and temperature move."""
    return "saver=%s ac=%s usb=%s wireless=%s" % (
        reading.get("saver"), reading.get("ac"), reading.get("usb"), reading.get("wireless"))


def install_record(run):
    lines = read_lines(run, "install.txt")
    if lines is None:
        return None
    return dict(line.split(": ", 1) for line in lines if ": " in line)


def display_readings(run):
    return [line.split(": ", 1)[1] for line in read_lines(run, "refresh-rate.txt") or [] if ": " in line]


def snapshot_cpu(s, k):
    """Thread id to utime + stime in ms, and to scheduler state, at snapshot k."""
    cpu, state = {}, {}
    states = s.get("state", "")
    for j in range(s["start"][k], s["start"][k] + s["len"][k]):
        cpu[s["tid"][j]] = (s["ut"][j] + s["st"][j]) * TICK_MS
        state[s["tid"][j]] = states[j] if j < len(states) else None
    return cpu, state


def jit_cpu_across_the_span(d, lo, hi):
    """The JIT thread's CPU between the last snapshot at or before the span and the first at or after it."""
    s = d["snaps"]
    at = [ms(v) for v in s["at"]]
    before = [k for k in range(s["n"]) if at[k] <= lo]
    after = [k for k in range(s["n"]) if at[k] >= hi]
    jit = [int(tid) for tid, name in s["names"].items() if name == "Jit thread pool"]
    if not before or not after or not jit:
        return None
    k0, k1 = before[-1], after[0]
    c0, c1 = snapshot_cpu(s, k0)[0], snapshot_cpu(s, k1)[0]
    used = sum(c1.get(t, 0.0) - c0.get(t, 0.0) for t in jit)
    return {"ms": used, "bracket_ms": [round(at[k0], 1), round(at[k1], 1)], "span_ms": round(hi - lo, 1)}


def cost_by_age(d):
    """Each 250 ms bin of history-row begin times: the rows, and their median dedup and whole-row cost in µs."""
    r = d["rows"]
    rows = [i for i in range(r["n"]) if r["source"][i] == SRC_HISTORY and r["insertOut"][i] >= 0 and r["mark0"][i] >= 0]
    bins = {}
    for i in rows:
        k = int(ms(r["begin"][i]) // AGE_BIN_MS)
        bins.setdefault(k, []).append(i)
    out = []
    for k in sorted(bins):
        members = bins[k]
        out.append({"from_ms": k * AGE_BIN_MS, "rows": len(members),
                    "dedup_us": round(statistics.median(1000.0 * ms(r["mark0"][i] - r["begin"][i]) for i in members), 1),
                    "row_us": round(statistics.median(1000.0 * ms(r["insertOut"][i] - r["begin"][i]) for i in members), 1)})
    return out


def waits_caught(d):
    """What the history-wait reads recorded: the snapshots taken inside a wait, and the rows after one."""
    s = d["snaps"]
    worker = {p["slot"]: p["historyTid"] for p in d["pages"]}
    snaps = []
    for k in range(s["n"]):
        if not s["reason"][k] & REASON_HISTORY_WAIT:
            continue
        cpu, state = snapshot_cpu(s, k)
        entry = {"at_ms": round(ms(s["at"][k]), 1),
                 "main": {"cpu_ms": cpu.get(d["mainTid"]), "state": state.get(d["mainTid"])},
                 "workers": {slot: {"cpu_ms": cpu.get(tid), "state": state.get(tid)} for slot, tid in worker.items()}}
        if "queued" in s:
            pages = len(d["pages"])
            entry["posts_queued"] = s["queued"][k * pages:(k + 1) * pages]
            entry["rows_begun"] = s["begun"][k * pages:(k + 1) * pages]
        snaps.append(entry)
    rows = d.get("waitRows") or {"n": 0}
    after = [{key: rows[key][i] for key in rows if key != "n"} for i in range(rows["n"])]
    return {"snapshots": snaps, "rows_after_a_wait": after}


def check(run):
    """The launch's record and its voids. Nothing here decides a measurement; it decides whether one counts."""
    void = []
    with open(os.path.join(run, "probe.json"), encoding="utf-8") as f:
        d = json.load(f)
    lo, hi = span_of(d)
    frames = frames_in_span(d, lo, hi)
    if frames is None:
        void.append("no span")
    elif frames > FRAMES_IN_SPAN_MAX:
        void.append("late delivery: %d frames drawn inside the span, more than %d" % (frames, FRAMES_IN_SPAN_MAX))

    starts = process_starts(read_lines(run, "logcat.txt"), d["anchorNs"])
    foreign = foreign_starts(starts, hi)
    if foreign is None:
        void.append("no Start proc line of the app's own: the process starts cannot be checked")
    elif foreign:
        void.append("a process other than the app's own started between the launch and the end of the span: "
                    + ", ".join("%s at %.1f ms" % (x["process"], x["at_ms"]) for x in foreign))

    events = read_lines(run, "getevent.txt")
    input_events = None if events is None else sum(1 for line in events if "EV_" in line)
    if input_events is None:
        void.append("no getevent record")
    elif input_events:
        void.append("%d input events" % input_events)

    install = install_record(run)
    if install is None:
        void.append("no install before the launch")
    elif not install.get("local_sha256") or install.get("local_sha256") != install.get("phone_sha256"):
        void.append("the installed APK's hash read back from the phone is not the APK's")

    battery = battery_readings(run)
    if len(battery) != 2:
        void.append("battery not read before and after")
    elif battery_state(battery[0]) != battery_state(battery[1]):
        void.append("battery saver or the power source changed during the launch")

    display = display_readings(run)
    if len(display) != 2 or display[0] != display[1]:
        void.append("the display reading changed during the launch, or was not read twice")

    pages = [{"slot": p["slot"], "channel_fp": p["channelFp"], "history_count": p["historyCount"]} for p in d["pages"]]
    r = d["rows"]
    live = {p["slot"]: sum(1 for i in range(r["n"]) if r["source"][i] == SRC_LIVE and r["page"][i] == p["slot"])
            for p in d["pages"]}
    return {
        "run": os.path.basename(os.path.normpath(run)),
        "commit": d.get("commit"),
        "void": void,
        "frames_in_span": frames,
        "span_ms": None if lo is None else [round(lo, 1), round(hi, 1)],
        "foreign_process_starts": foreign,
        "input_events": input_events,
        "first_after_install": install is not None and bool(install.get("local_sha256"))
                               and install.get("local_sha256") == install.get("phone_sha256"),
        "install": install,
        "compile_state": (read_lines(run, "compile-state.txt") or [None])[0],
        "battery": battery,
        "battery_state": battery_state(battery[0]) if battery else None,
        "display": display,
        "pages": pages,
        "both_pages_at_the_cap": all(p["history_count"] == HISTORY_CAP for p in pages),
        "live_rows_by_page": live,
        "jit_cpu_across_the_span": jit_cpu_across_the_span(d, lo, hi) if lo is not None else None,
        "history_row_cost_by_process_age": cost_by_age(d),
        "history_waits_caught": waits_caught(d),
    }


def main(argv):
    void = False
    for run in argv:
        record = check(run)
        void = void or bool(record["void"])
        print(json.dumps(record, indent=1))
    return 1 if void else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
