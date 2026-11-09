#!/usr/bin/env bash
# The long session's presses (docs/manual-plan-5d-recyclerview.md, section M): the only tool of the
# round that sends input to the phone. sitting.sh only reads. Written on 2026-11-09, by the owner's
# ruling that the driver presses by coordinates, so a press made while another app is in front
# would act on an application that is not ours.
#
# Usage:
#   press.sh longpress <label>     long-press refresh, held 1,000 ms, on the page on screen
#   press.sh tap <label>           tap refresh on the page on screen: one manual refresh
#   press.sh swipe-left <label>    to the next page, a swipe that starts on the refresh button
#   press.sh swipe-right <label>   to the previous page, the same
#   press.sh swipe-from-accounts <label>
#                                  from the accounts page, where a launch opens, to the first chat
#                                  page: its [target] is no refresh button on screen and the accounts
#                                  list on screen, the swipe starting inside the list, whose rows
#                                  have no swipe action (LoginFragment.kt:64-68)
#
# Before EVERY press, two loud preconditions; when either fails nothing is pressed, and the exit is 3:
#   [front]   the dev app's window has focus, read from `dumpsys window` immediately before the press;
#   [target]  exactly one refresh button is on screen, from the screen's hierarchy, and the press, or
#             the swipe's start, lies inside it. A swipe never starts on a message row, where a
#             right-edge drag to the left arms swipe-to-reply, nor on a text field.
# After every press focus is read again; another window in front then prints
# "INTERRUPTED [front-after]" and exits 4: the press was made, and what followed it is not ours.
# Every press is appended to presses.txt beside this file, with both reads and both clocks.
# No hierarchy text is printed or saved: only the refresh button's bounds are read from it.
set -u
export MSYS_NO_PATHCONV=1
ADB="${ADB:-adb}"
PKG=com.fs.twitchminichat.dev
V="$(cd "$(dirname "$0")" && pwd)"
PY="${PY:-py -3.10 -I -X utf8}"
LOG="$V/presses.txt"

violated() {
  {
    echo "PRECONDITION VIOLATED [$1]"
    echo "  required: $2"
    echo "  found:    $3"
    echo "  Nothing was pressed. Exit 3."
  } >&2
  echo "$LABEL host=$(date '+%F %T.%3N') $ACTION NOT PRESSED [$1] $3" >> "$LOG"
  exit 3
}

# The window with focus, as "dev" or "other"; another app's name is never printed.
focus() {
  local f
  f=$("$ADB" shell dumpsys window | tr -d '\r' | grep -m1 'mCurrentFocus=')
  case "$f" in *" $PKG/"*) echo dev ;; *) echo other ;; esac
}

# The on-screen views with the given id, from one read of the screen's hierarchy, as
# "<count> left top right bottom" for the first, or "NONE <reason>" when no hierarchy was read.
bounds_of() {
  "$ADB" exec-out uiautomator dump /dev/tty 2>/dev/null | $PY -c '
import re, sys
want = sys.argv[1]
data = sys.stdin.buffer.read().decode("utf-8", "replace")
if "<hierarchy" not in data:
    print("NONE no hierarchy was read"); sys.exit()
root = re.search(r"<node\b[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", data)
width = int(root.group(3)) if root else 0
found = []
for tag in re.findall(r"<node\b[^>]*>", data):
    if not re.search(r"resource-id=\"[^\"]*:id/" + re.escape(want) + r"\"", tag):
        continue
    l, t, r, b = map(int, re.search(r"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", tag).groups())
    if 0 <= l < r <= width and t < b:
        found.append((l, t, r, b))
print(len(found), *(found[0] if found else (0, 0, 0, 0)))
' "$1"
}

ACTION=${1:-}; LABEL=${2:-}
case "$ACTION" in longpress|tap|swipe-left|swipe-right|swipe-from-accounts) ;; *) echo "usage: press.sh longpress|tap|swipe-left|swipe-right|swipe-from-accounts <label>" >&2; exit 2 ;; esac
case "$LABEL" in ''|*[!A-Za-z0-9._-]*) echo "press.sh: the label takes letters, digits, '.', '_' and '-' only" >&2; exit 2 ;; esac

b=$(bounds_of btnRefreshChat)
case "$b" in NONE*|'') violated target "the screen's hierarchy read" "${b#NONE }" ;; esac
read -r n l t r bt <<< "$b"
if [ "$ACTION" = swipe-from-accounts ]; then
  [ "$n" = 0 ] || violated target "no refresh button on screen: the accounts page, not a chat page" "$n refresh buttons on screen"
  a=$(bounds_of recyclerAccounts)
  read -r n l t r bt <<< "$a"
  [ "$n" = 1 ] || violated target "the accounts list on screen, the swipe inside it" "${a#NONE }: $n accounts lists on screen"
  x=$(( r - (r - l) / 8 )); y=$(( (t + bt) / 2 ))
  args="swipe $x $y 60 $y 150"
else
  [ "$n" = 1 ] || violated target "exactly one refresh button on screen, the press inside it" "$n refresh buttons on screen, not 1"
  x=$(( (l + r) / 2 )); y=$(( (t + bt) / 2 ))
  case "$ACTION" in
    longpress)   args="swipe $x $y $x $y 1000" ;;
    tap)         args="tap $x $y" ;;
    swipe-left)  args="swipe $x $y 60 $y 150" ;;
    swipe-right) args="swipe $x $y $(( r + (r - l) * 2 )) $y 150" ;;
  esac
fi

# The front is read last, immediately before the press, so that nothing comes between them but adb.
f0=$(focus)
[ "$f0" = dev ] || violated front "the dev app's window has focus, immediately before the press" "another window has focus"
t0=$(date '+%F %T.%3N')
"$ADB" shell input $args
rc=$?
t1=$(date '+%F %T.%3N')
f1=$(focus)
echo "$LABEL host_before=$t0 host_after=$t1 $ACTION target=[$l,$t][$r,$bt] input=\"$args\" rc=$rc front_before=$f0 front_after=$f1" | tee -a "$LOG"
if [ "$f1" != dev ]; then
  echo "INTERRUPTED [front-after] another window had focus after the press at $t1: what followed the press is not ours. Exit 4." >&2
  exit 4
fi
[ "$rc" = 0 ] || { echo "press.sh: input exited $rc" >&2; exit 5; }
