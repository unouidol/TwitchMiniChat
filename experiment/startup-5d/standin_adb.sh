#!/usr/bin/env bash
# A stand-in for adb, for sitting_suite.sh. It answers exactly the reads sitting.sh makes in a long
# session, from files in $STANDIN, and nothing else: any other command is written to
# $STANDIN/UNMODELED and refused with exit 99, so that no case can pass by reaching a read this
# stand-in does not model. The suite fails every case whose UNMODELED file exists.
#
# Files in $STANDIN. A file "stem.N" answers the Nth call of its kind and every later one, until a
# higher-numbered file exists; without one, the file "stem" answers.
#   pid[.N]      the answer of `pidof`; empty means no process
#   start[.N]    the process's start time, field 22 of /proc/<pid>/stat
#   front[.N]    "dev" when the dev app's window has focus, anything else otherwise
#   ui.xml       the screen's hierarchy, for press.sh; absent, uiautomator's failure
#   INPUT        written, not read: every `input` press.sh sends, one per line
#   rate, range  the display's refresh rate and its primary range, as "90.0" and "0 90"
#   journal[.N]  the current journal file; the content "@UNREADABLE" makes the read fail, as on the
#                phone: the error on stdout, and exit 0
#   previous     the previous journal file, absent unless this file exists; absent reads as the error
#                on stdout, and exit 0
set -u
D=${STANDIN:?STANDIN must name the directory of the stand-in files}
PKG=com.fs.twitchminichat.dev

# Counts the calls of one kind, and prints the count.
count() {
  local f="$D/.n.$1" n
  n=$(( $(cat "$f" 2>/dev/null || echo 0) + 1 ))
  echo "$n" > "$f"
  echo "$n"
}

# Prints the file answering call number $2 of stem $1.
pick() {
  local i
  for ((i = $2; i >= 1; i--)); do
    [ -e "$D/$1.$i" ] && { echo "$D/$1.$i"; return; }
  done
  echo "$D/$1"
}

refuse() {
  echo "$*" >> "$D/UNMODELED"
  echo "standin_adb: no model for: $*" >&2
  exit 99
}

kind=${1:-}
[ $# -gt 0 ] && shift
cmd="$*"
case "$kind" in
  shell)
    case "$cmd" in
      "pidof $PKG")
        f=$(pick pid "$(count pid)")
        p=$(cat "$f" 2>/dev/null)
        [ -n "$p" ] || exit 1
        echo "$p" ;;
      "cat /proc/"*"/stat")
        p=${cmd#cat /proc/}; p=${p%/stat}
        f=$(pick start "$(count start)")
        echo "$p ($PKG) S 1 1 1 0 -1 4194560 100 0 0 0 1 1 0 0 20 0 30 0 $(cat "$f") 100 200" ;;
      "date '+%Y-%m-%d %H:%M:%S'")
        date '+%Y-%m-%d %H:%M:%S' ;;
      "date +%s")
        date +%s ;;
      "dumpsys window")
        if [ "$(cat "$(pick front "$(count front)")" 2>/dev/null)" = dev ]; then
          w="$PKG/com.fs.twitchminichat.MainActivity"
        else
          w="com.example.other/.Main"
        fi
        printf '  mCurrentFocus=Window{1a2b3c u0 %s}\n' "$w" ;;
      "dumpsys display")
        printf '  mActiveSfDisplayMode=DisplayMode{id=1, width=1080, height=2400, xDpi=402.0, yDpi=401.0, refreshRate=%s, appVsyncOffsetNanos=0}\n' "$(cat "$D/rate")"
        printf '  mDesiredDisplayModeSpecs: primaryRefreshRateRange=[%s]\n' "$(cat "$D/range")" ;;
      "input "*)
        # press.sh's presses: recorded, never executed, since there is no phone.
        echo "$cmd" >> "$D/INPUT" ;;
      *) refuse shell "$cmd" ;;
    esac ;;
  exec-out)
    # A failed read EXITS 0, with the error on stdout, as adb exec-out does on the phone: measured on
    # 2026-11-09 on the OnePlus AC2003, where `adb exec-out run-as ... cat <missing file>` exited 0 and
    # `adb shell` exited 1 for the same read. This stand-in exited 1 until then, which let the pipe's
    # own check catch a failed read before the extractor's exit 5, the guard the phone relies on.
    case "$cmd" in
      "run-as $PKG cat files/diagnostics/history-diagnostics-previous.log")
        if [ -e "$D/previous" ]; then
          cat "$D/previous"
        else
          echo "cat: files/diagnostics/history-diagnostics-previous.log: No such file or directory"
        fi ;;
      "run-as $PKG cat files/diagnostics/history-diagnostics.log")
        f=$(pick journal "$(count journal)")
        if [ ! -e "$f" ] || [ "$(cat "$f")" = "@UNREADABLE" ]; then
          echo "cat: files/diagnostics/history-diagnostics.log: Permission denied"
        else
          cat "$f"
        fi ;;
      "uiautomator dump /dev/tty")
        # press.sh's read of the screen: the hierarchy in ui.xml, or uiautomator's failure.
        if [ -e "$D/ui.xml" ]; then cat "$D/ui.xml"; else echo "ERROR: null root node returned by UiTestAutomationBridge."; fi ;;
      *) refuse exec-out "$cmd" ;;
    esac ;;
  *) refuse "$kind" "$cmd" ;;
esac
