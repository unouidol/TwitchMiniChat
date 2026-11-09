"""Prints only whitelisted fields of TMC's history diagnostics journal, read from stdin.

The journal writes "yyyy-MM-dd HH:mm:ss.SSS <event> key=value ...", with the account and the
channel on every line (ChatFragment.recordDiagnostics). This keeps, after the optional
"since" instant given as the first argument:

  backfill.triggered  source, only as one of the six values HistoryBackfillPolicy.kt writes
  backfill.result     messageCount, firstTs, lastTs
  user.marker         shownRows

Each value must match the shape its field has in the code; a value that does not is counted
and never printed. Every printed token is built here from those parts, so no other field, no
account, no channel and no text can reach the output. A summary goes to stderr.
"""
import re
import sys

LINE = re.compile(r"^(\d{4}-\d\d-\d\d \d\d:\d\d:\d\d\.\d{3}) (\S+)(.*)$")
# BackfillSource's journal values (HistoryBackfillPolicy.kt:5-10). Matching the six exactly,
# rather than a pattern, is what keeps a channel-shaped word out of this field.
SOURCES = "first_connect|reconnect_offline|reconnect_no_pause_reference|resume|resume_no_pause|manual_refresh"
KEEP = {
    "backfill.triggered": {"source": SOURCES},
    "backfill.result": {"messageCount": r"\d{1,6}", "firstTs": r"\d{9,11}", "lastTs": r"\d{9,11}"},
    "user.marker": {"shownRows": r"\d{1,7}"},
}

# "--last" prints only the instant of the journal's last line, of any event: the session's mark,
# taken in the journal's own clock, so that no other clock has to agree with it.
last_only = len(sys.argv) > 1 and sys.argv[1] == "--last"
since = sys.argv[1] if len(sys.argv) > 1 and not last_only else ""
seen = kept = rejected = 0
last_instant = None
for raw in sys.stdin.buffer:
    match = LINE.match(raw.decode("utf-8", "replace").rstrip("\r\n"))
    if not match:
        continue
    seen += 1
    instant, event, rest = match.groups()
    last_instant = instant
    if last_only:
        continue
    if event not in KEEP or instant <= since:
        continue
    fields = {}
    for token in rest.split():
        key, sep, value = token.partition("=")
        if sep:
            fields[key] = value
    out = []
    for key, shape in KEEP[event].items():
        value = fields.get(key)
        if value is None:
            continue
        if re.fullmatch(shape, value):
            out.append(f"{key}={value}")
        else:
            rejected += 1
    kept += 1
    print(instant, event, *out)
print(f"# journal extract: {seen} journal lines read, {kept} kept, {rejected} values rejected by shape", file=sys.stderr)
# A journal the app has written always has lines: none at all means it was not read, never "nothing new".
if seen == 0:
    print("# journal extract: no journal line in the input", file=sys.stderr)
    sys.exit(5)
if last_only:
    print(last_instant)
