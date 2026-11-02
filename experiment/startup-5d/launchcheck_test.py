"""launchcheck.py's readings on hand-made lines, before any real launch: python -I launchcheck_test.py

The process-start window and whose a process is, from `Start proc` lines shaped as this phone's logcat
writes them (2026-11-02); and the battery's readings, whose level and temperature are not held.
"""
import importlib.util
import os

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("lc", os.path.join(HERE, "launchcheck.py"))
lc = importlib.util.module_from_spec(spec)
spec.loader.exec_module(lc)

ANCHOR_NS = 1_000_000_000_000  # 1000.000 s on the monotonic clock
LINES = [
    " 999.400  1000  1621  1969 I ActivityManager: Start proc 18742:com.google.android.cellbroadcastreceiver/u0a325 for broadcast {com.google.android.cellbroadcastreceiver/com.android.cellbroadcastreceiver.CellBroadcastReceiver}",
    " 999.950  1000  1621  1969 I ActivityManager: Start proc 18768:com.fs.twitchminichat.dev/u0a572 for pre-top-activity {com.fs.twitchminichat.dev/com.fs.twitchminichat.MainActivity}",
    " 1001.000  1000  1621  1969 I ActivityManager: Start proc 18800:com.fs.twitchminichat.dev:gecko/u0a572 for service {com.fs.twitchminichat.dev/org.mozilla.gecko.process.GeckoChildProcessServices}",
    " 1001.500  1000  1621  1969 I ActivityManager: Start proc 18801:com.google.android.webview:sandboxed_process0:x:1/u0i34 for  {com.fs.twitchminichat.dev/org.chromium.content.app.SandboxedProcessService0:1}",
    " 1002.000  1000  1621  1969 I ActivityManager: Start proc 18802:com.discord/u0a266 for broadcast {com.discord/com.google.firebase.iid.FirebaseInstanceIdReceiver}",
    " 1002.500  1000  1621  1969 I ActivityManager: Start proc 18803:com.example.nobraces/u0a1 for content provider",
    " 1009.000  1000  1621  1969 I ActivityManager: Start proc 18804:com.example.late/u0a2 for broadcast {com.example.late/.R}",
]

starts = lc.process_starts(LINES, ANCHOR_NS)
assert len(starts) == 7, starts
assert starts[5][3] is None, "a line without a component is still read"
print("ok every Start proc line is read, with or without its component")

foreign = lc.foreign_starts(starts, span_end=3000.0)
names = [x["process"] for x in foreign]
assert names == ["com.discord", "com.example.nobraces"], names
print("ok in the window: only processes not the app's own; before the app's start and after the span, none")
assert lc.foreign_starts([s for s in starts if s[2] != lc.PKG], 3000.0) is None
print("ok without the app's own start line the window is unknown, which launchcheck voids")

reading = {"saver": "0", "ac": "false", "usb": "true", "wireless": "false", "level": "73", "temperature": "269"}
moved = dict(reading, level="81", temperature="290")
assert lc.battery_state(reading) == lc.battery_state(moved)
assert lc.battery_state(reading) != lc.battery_state(dict(reading, saver="1"))
assert lc.battery_state(reading) != lc.battery_state(dict(reading, usb="false"))
print("ok battery saver and the power source are held; the level and the temperature are not")
print("all cases as expected")
