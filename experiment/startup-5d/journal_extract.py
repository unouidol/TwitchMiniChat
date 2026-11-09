"""Prints only whitelisted fields of TMC's history diagnostics journal, read from stdin.

The journal writes "yyyy-MM-dd HH:mm:ss.SSS <event> key=value ...", with the account and the
channel on every line (ChatFragment.recordDiagnostics). This keeps, after the optional
"since" instant given as the first argument:

  backfill.triggered  source, only as one of the six values HistoryBackfillPolicy.kt writes
  backfill.result     messageCount, firstTs, lastTs
  user.marker         shownRows

and, first on every kept line, page=<label>: the page that wrote the line, labelled A, B, ... in
the order in which its account first appears on a kept line after "since", or "?" for a line
with no account. The label comes from that order alone, never from the account's value, which
is not printed. Added on 2026-11-09, so that the poll can count two pages apart.

Each value must match the shape its field has in the code; a value that does not is counted
and never printed. Every printed token is built here from those parts, so no other field, no
account, no channel and no text can reach the output. A summary goes to stderr.
"""
import re
import sys

# Every line is written with LF alone, here at the source and only here. On Windows a text-mode
# stdout writes CRLF, and the shell's command substitution strips the carriage return of the LAST
# line it captures only, so a saved line's bytes depended on whether it was the last of its read.
# sitting.sh's [kept] check then found one line with and without its CR, and stopped the long
# session L5D1 on 2026-11-09. A CR that came back would stop the poll the same way.
sys.stdout.reconfigure(newline="\n")

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
# Page labels by order of first appearance on a kept line after "since": the account's value is a
# key here and is never printed.
labels = {}


def page_of(account):
    """Returns the page label of a kept line: A, B, ... by first appearance, "?" with no account."""
    if account is None:
        return "?"
    if account not in labels:
        labels[account] = chr(ord("A") + len(labels)) if len(labels) < 26 else "?"
    return labels[account]

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
    out = [f"page={page_of(fields.get('account'))}"]
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
