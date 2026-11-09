"""Writes a deterministic hostile journal to stdout, for the check that the real extractor cannot
emit a line outside sitting.sh's grammar (why case C4 needs a test double to reach that check).

Every line is shaped to tempt the extractor: kept and unkept events, and look-alike events; valid
and malformed instants; injected sources, including channel-shaped words and a page token;
messageCount, firstTs, lastTs and shownRows at, inside and past their shapes; an injected page=
field; duplicate keys; tokens without "="; "=" inside values; text fields; tabs, CRs and
non-ASCII.
"""
import random

rng = random.Random(20261109)
EVENTS = ["backfill.triggered", "backfill.result", "user.marker", "lifecycle.start", "backfill.skipped",
          "fcm.received", "user.marker.extra", "BACKFILL.RESULT", "backfill.result2", "spawn.observed"]
SOURCES = ["first_connect", "reconnect_offline", "reconnect_no_pause_reference", "resume", "resume_no_pause",
           "manual_refresh", "chan_xyz", "manual_refresh2", "", "page=A", "first_connect;rm", "FIRST_CONNECT",
           "resume\tresume", "résumé"]
COUNTS = ["400", "0", "999999", "1000000", "4x0", "-1", "", "12 34", "4=0", "٤٠٠", "1e3", "00400"]
TS = ["1794200400", "123", "99999999999", "999999999", "abc", "", "1794200400.5"]
ROWS = ["416", "0", "9999999", "10000000", "x", "", "-3", "4 16"]
ACCOUNTS = ["acct_zed", "acct_amy", "A", "page=B", "?", "", "acct zed", "ACCT", "акк"]
INSTANTS = ["2026-11-09 10:%02d:%02d.%03d", "2026-11-09 10:%02d:%02d", "2026-11-09T10:%02d:%02d.%03d",
            "26-11-09 10:%02d:%02d.%03d"]


def instant(i):
    fmt = INSTANTS[0] if rng.random() < 0.85 else rng.choice(INSTANTS)
    args = (i // 600 % 60, i // 10 % 60, i % 1000)
    return fmt % args[:fmt.count("%")]


for i in range(6000):
    fields = [f"account={rng.choice(ACCOUNTS)}", "channel=chan_xyz"]
    if rng.random() < 0.3:
        fields.append(f"page={rng.choice('ABZ?')}")
    fields += [f"source={rng.choice(SOURCES)}", f"messageCount={rng.choice(COUNTS)}",
               f"firstTs={rng.choice(TS)}", f"lastTs={rng.choice(TS)}", f"shownRows={rng.choice(ROWS)}"]
    if rng.random() < 0.3:
        fields.append(f"messageCount={rng.choice(COUNTS)}")
    if rng.random() < 0.3:
        fields.append("text=hello_world")
    if rng.random() < 0.2:
        fields.append("bareword")
    rng.shuffle(fields)
    k = rng.randint(0, len(fields))
    end = "\r" if rng.random() < 0.1 else ""
    print(f"{instant(i)} {rng.choice(EVENTS)} {' '.join(fields[:k])}{end}")
