"""qualify5d.py's verdicts on hand-made launches, before any real one: python -I qualify5d_test.py

Each case is one the plan names: criterion 2's three scaled cases (a correct instrument, 120 of 200 ms
landing elsewhere, analyze5d.py dropping the delay), criterion 5 inside and outside its bounds, a
display reading that changes, and rows that differ. Only evaluate() and verdict() are exercised:
facts(), which reads a run directory, first meets a real one at the qualification.
"""
import copy
import importlib.util
import os

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("q", os.path.join(HERE, "qualify5d.py"))
q = importlib.util.module_from_spec(spec)
spec.loader.exec_module(q)

READING = "refreshRate=60.000004 frameRateOverride=[] skipwarning=[]"
BATTERY = "saver=0 ac=false usb=true wireless=false"


def record(run):
    """launchcheck.py's record of a launch that counts, as much of it as verdict() reads."""
    return {"run": run, "void": [], "battery_state": BATTERY,
            "battery": [{"level": "73", "temperature": "269"}, {"level": "73", "temperature": "270"}]}


def baseline(run, wobble=0.0):
    """A launch with no injection: page 0's history row 100 from 1000 ms, each segment 1 ms."""
    marks = {"begin": 1000.0, "mark0": 1001.0, "mark1": 1002.0, "mark2": 1003.0, "insertIn": 1004.0,
             "mark3": 1005.0, "mark4": 1006.0, "insertOut": 1007.0}
    return {
        "run": run, "commit": "test", "inject": "", "inject_arg": -1, "inject_start": None, "inject_end": None,
        "rows_in_span": 800, "span": 2400.0 + wobble, "span_end": 3400.0, "idle": 100.0,
        "segments": {key: 50.0 + wobble for key in q.SEGMENTS},
        "other_messages": 300.0 + wobble, "binds_ms": 40.0 + wobble, "act_in": 150.0 + wobble,
        "app_in": 100.0, "app_out": 120.0, "burst_end": 5000.0 + wobble,
        "frames_ran": [5000.0 + 16.7 * k for k in range(400)],
        "messages": [(999.0, 1008.0), (3000.0, 3001.0)],
        "builds": [(1005.2, 1005.8)],
        "rows": [dict(marks, page=0, source=1, ordinal=100)],
        "display": [READING, READING],
        "check": record(run),
    }


BASELINES = [baseline("b1", -3.0), baseline("b2", 0.0), baseline("b3", 4.0)]


def injected(name, start, delay, landed=None, arg=-1):
    """A launch with [name] injected at [start] for [delay] ms, of which [landed] reached its segment."""
    f = baseline("inject-" + name)
    f.update(inject=name, inject_arg=arg, inject_start=start, inject_end=start + delay)
    f["span"] += delay
    landed = delay if landed is None else landed
    if name in q.ROW_PHASES:
        key, a, b = q.ROW_PHASES[name]
        f["segments"][key] += landed
        row = f["rows"][0]
        order = ["begin", "mark0", "mark1", "mark2", "insertIn", "mark3", "mark4", "insertOut"]
        for later in order[order.index(b):]:
            row[later] += delay
        f["messages"][0] = (999.0, 1008.0 + delay)
        if name == "insert":
            f["segments"]["timeline_insert_without_view_build"] += landed
    return f


def check(label, out, expected_pass, criterion=None):
    launch = out["launches"][-1]
    got = launch["pass"] if criterion is None else launch["criteria"][criterion]["pass"]
    assert got == expected_pass, (label, launch)
    print("ok", label)


def run_one(launch):
    return q.verdict("control", copy.deepcopy(BASELINES), [launch])


# Criterion 2's cases, scaled to 200 ms, on `mention`, whose segment is begin to mark0.
check("a correct instrument: rise 200, passes", run_one(injected("mention", 1000.5, 200)), True)
check("120 of 200 elsewhere: rise 80, criterion 2 fails",
      run_one(injected("mention", 1000.5, 200, landed=80)), False, "2")
leaked = injected("mention", 1000.5, 200, landed=80)
leaked["segments"]["buddy_capture"] += 120
check("... and the 120 ms in another segment: criterion 3 fails", run_one(leaked), False, "3")
# Criterion 3 is one-sided since 2026-11-04: a segment below the baselines passes, printed as below.
lowered = injected("mention", 1000.5, 200)
lowered["segments"]["buddy_capture"] -= 120
out = run_one(lowered)
low_three = out["launches"][0]["criteria"]["3"]
assert low_three["pass"] and low_three["segments"]["buddy_capture"]["below_lower_reference"], low_three
assert low_three["rule"].startswith("one-sided"), low_three
print("ok a segment 120 ms below the baselines: criterion 3 passes, printed as below its lower reference")
at_bound = injected("mention", 1000.5, 200)
at_bound["segments"]["buddy_capture"] = max(b["segments"]["buddy_capture"] for b in BASELINES) + q.CRITERION_3_MS
check("a segment exactly at the baselines' highest plus 10 ms: criterion 3 passes", run_one(at_bound), True, "3")
over = injected("mention", 1000.5, 200)
over["segments"]["buddy_capture"] = max(b["segments"]["buddy_capture"] for b in BASELINES) + q.CRITERION_3_MS + 0.5
check("0.5 ms above it: criterion 3 fails", run_one(over), False, "3")
dropped = injected("mention", 1000.5, 200, landed=0)
check("analyze5d.py drops the delay: criterion 2 fails", run_one(dropped), False, "2")
check("... where criterion 4 alone would pass", run_one(dropped), True, "4")
late = injected("mention", 1000.5, 200)
late["inject_start"], late["inject_end"] = 1000.5 + 120, 1000.5 + 320
check("120 ms of the delay outside its segment: criterion 4 fails", run_one(late), False, "4")
check("an insert moves its own segment and the insert less its build, nothing else",
      run_one(injected("insert", 1005.5, 200)), True)

# Criterion 1: a delay recorded shorter than asked.
short = injected("mention", 1000.5, 150)
check("a delay recorded at 150 ms: criterion 1 fails", run_one(short), False, "1")

# Criterion 5, the stall at 8000 ms for 534.
stall = injected("stall", 8000.2, 534.0, arg=8000)
stall["messages"].append((8000.0, 8534.5))
stall["frames_ran"] = [8540.0, 8556.0]
stall["burst_end"] = 8535.0
check("the burst end between the stall's end and the next frame: criterion 5 passes", run_one(stall), True, "5")
early = copy.deepcopy(stall)
early["burst_end"] = 8350.0
check("the burst end 184 ms before the stall ended: criterion 5 fails", run_one(early), False, "5")
lateline = copy.deepcopy(stall)
lateline["burst_end"] = 8600.0
check("the burst end after the next frame: criterion 5 fails", run_one(lateline), False, "5")

# The round's own conditions.
moved = injected("mention", 1000.5, 200)
moved["display"] = [READING, READING.replace("60.000004", "90.0")]
out = run_one(moved)
assert out["verdict"].startswith("VOID"), out["verdict"]
print("ok a launch whose display changed voids the verdict")
fewer = injected("mention", 1000.5, 200)
fewer["rows_in_span"] = 799
out = run_one(fewer)
assert out["verdict"].startswith("STOPPED"), out["verdict"]
print("ok an injected launch with other rows stops the qualification")
# Every launch names where its phase sits on its build, and `build` differently on the two.
out = run_one(injected("mention", 1000.5, 200))
assert out["launches"][0]["phase_location"] == q.PHASE_LOCATIONS["control"]["mention"], out["launches"][0]
assert "not comparable" in out["phase_locations_note"], out["phase_locations_note"]
assert q.PHASE_LOCATIONS["control"]["build"] != q.PHASE_LOCATIONS["5d"]["build"]
print("ok each launch names its phase's location, and build's differs between the builds")
# Criterion 3's exemption, computed from containment.
assert q.moving_segments("control", "insert") == {"timeline_insert", "timeline_insert_without_view_build"}
assert q.moving_segments("control", "build") == {"timeline_insert", "timeline_insert_of_which_view_build"}
assert q.moving_segments("5d", "build") == set() and q.moving_segments("control", "stall") == set()
assert q.moving_segments("5d", "mention") == {"dedup_backfill_mention"}
print("ok criterion 3's exemptions follow from containment")

# The stall after the span: criterion 3 not applicable; inside it: criterion 5 fails.
stall_after = copy.deepcopy(stall)
stall_after["span_end"] = 3400.0
for b in BASELINES:
    b["span_end"] = 3400.0
out = run_one(stall_after)
three = out["launches"][0]["criteria"]["3"]
assert three["status"] == "NOT APPLICABLE" and "pass" not in three, three
assert out["launches"][0]["pass"], out["launches"][0]
print("ok a stall after the span: criterion 3 not applicable, not printed as passed")
stall_inside = copy.deepcopy(stall)
stall_inside["span_end"] = 9000.0
out = run_one(stall_inside)
assert out["launches"][0]["criteria"]["5"]["pass"] is False and "pass" in out["launches"][0]["criteria"]["3"]
print("ok a stall inside the span: criterion 5 fails and criterion 3 is held")

# A baseline with no burst, by name.
quiet = copy.deepcopy(BASELINES)
quiet[1]["burst_end"] = None
out = q.verdict("control", quiet, [stall_after])
five = out["launches"][0]["criteria"]["5"]
assert five["baselines_burst_end"]["b2"] == "no burst" and five["baselines_with_no_burst"] == ["b2"], five
assert "b2" not in five["baselines_whose_burst_ended_before_the_stall"], five
print("ok a baseline with no burst is printed as no burst")

# The conditions of 2026-11-02 after the first qualification: a void launch stops the verdict, and
# battery saver or the power source must be one state across the build's launches.
late = injected("mention", 1000.5, 200)
late["check"]["void"] = ["late delivery: 84 frames drawn inside the span, more than 5"]
out = run_one(late)
assert out["verdict"].startswith("STOPPED: void launches") and "inject-mention" in out["verdict"], out["verdict"]
print("ok a launch with more than 5 frames inside its span stops the verdict: it is re-run, not scored")
foreign = copy.deepcopy(BASELINES)
foreign[0]["check"]["void"] = ["a process other than the app's own started between the launch and the end of the span"]
out = q.verdict("control", foreign, [injected("mention", 1000.5, 200)])
assert out["verdict"].startswith("STOPPED: void launches") and "b1" in out["verdict"], out["verdict"]
print("ok a void baseline stops the verdict as well")
saver = injected("mention", 1000.5, 200)
saver["check"]["battery_state"] = BATTERY.replace("saver=0", "saver=1")
out = run_one(saver)
assert out["verdict"].startswith("VOID: the launches do not share one battery"), out["verdict"]
print("ok battery saver differing between launches voids the verdict")
levels = injected("mention", 1000.5, 200)
levels["check"]["battery"] = [{"level": "80", "temperature": "281"}, {"level": "81", "temperature": "283"}]
out = run_one(levels)
assert out["battery"]["one_state"] and not out["verdict"].startswith(("VOID", "STOPPED")), out["verdict"]
assert out["launches"][0]["pass"], out["launches"][0]
print("ok the level and the temperature moving decide nothing")
print("all cases as expected")
