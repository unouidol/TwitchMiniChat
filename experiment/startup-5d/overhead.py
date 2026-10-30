"""Step 1(c): the same build with the probe off and on, measured only by what logcat records for both."""
import os
import re
import statistics
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
RUNS = os.path.join(HERE, "runs")
LINE = re.compile(r"^\s*(\d+\.\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s+(\w)\s+([^:]+):\s?(.*)$")
WINDOW_MS = 10300.0  # anchor + 10 s, the anchor being about 0.3 s after the process starts


def parse(run):
    proc = None
    pid = None
    out = {"run": run}
    skipped = []
    history = []
    for raw in open(os.path.join(RUNS, run, "logcat.txt"), encoding="utf-8", errors="replace"):
        m = LINE.match(raw)
        if not m:
            continue
        t = float(m.group(1)) * 1000.0
        tag, msg = m.group(6).strip(), m.group(7)
        if tag == "ActivityManager" and "Start proc" in msg and "com.fs.twitchminichat.dev" in msg and proc is None:
            proc = t
            pid = int(re.search(r"Start proc (\d+):", msg).group(1))
        elif tag == "ActivityTaskManager" and msg.startswith("Displayed com.fs.twitchminichat.dev") and proc is not None:
            out["displayed"] = t - proc
        elif pid is not None and int(m.group(3)) == pid:
            if tag == "Choreographer" and msg.startswith("Skipped"):
                skipped.append((t - proc, int(re.search(r"Skipped (\d+)", msg).group(1))))
            elif tag == "TMC_HISTORY" and msg.startswith("History loaded"):
                history.append(t - proc)
    inside = [(t, f) for t, f in skipped if t <= WINDOW_MS]
    out["burst_end"] = inside[-1][0] if inside else None
    out["burst_frames"] = sum(f for _, f in inside)
    out["burst_lines"] = len(inside)
    out["history_last"] = max(history) if history else None
    am = open(os.path.join(RUNS, run, "am-start.txt"), encoding="utf-8", errors="replace").read()
    tt = re.search(r"TotalTime: (\d+)", am)
    out["am_total"] = float(tt.group(1)) if tt else None
    return out


def summary(values):
    v = [x for x in values if x is not None]
    if not v:
        return "-"
    return "mean %7.0f  median %7.0f  min %7.0f  max %7.0f" % (statistics.mean(v), statistics.median(v), min(v), max(v))


if __name__ == "__main__":
    pre = sys.argv[1] if len(sys.argv) > 1 else "o"
    off = [parse("%s-off-%d" % (pre, k)) for k in range(1, 6)]
    on = [parse("%s-on-%d" % (pre, k)) for k in range(1, 6)]
    for key in ("burst_end", "burst_frames", "burst_lines", "displayed", "am_total", "history_last"):
        print("%-14s off: %s" % (key, "  ".join("%7.0f" % r[key] if r.get(key) is not None else "      -" for r in off)))
        print("%-14s on : %s" % ("", "  ".join("%7.0f" % r[key] if r.get(key) is not None else "      -" for r in on)))
        print("%-14s off %s" % ("", summary([r.get(key) for r in off])))
        print("%-14s on  %s" % ("", summary([r.get(key) for r in on])))
        a = [r.get(key) for r in off if r.get(key) is not None]
        b = [r.get(key) for r in on if r.get(key) is not None]
        if a and b:
            dm = statistics.mean(b) - statistics.mean(a)
            dmed = statistics.median(b) - statistics.median(a)
            print("%-14s on - off: mean %+.0f (%+.1f%%), median %+.0f (%+.1f%%)" % (
                "", dm, 100 * dm / statistics.mean(a), dmed, 100 * dmed / statistics.median(a)))
        print()
