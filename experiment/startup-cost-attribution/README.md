# Startup cost attribution - SCAFFOLD, never merged

Host-side scripts for the measurement on branch `experiment/startup-cost-attribution`.
The probe itself is `app/src/main/java/com/fs/twitchminichat/probe/`.

- `run.sh <label> "<flag>" <account id>`: one cold start by intent; flag `off`, `yardstick`,
  `full`, plus `harness`, `nosnap`, `nolooper`. Results go to `runs/<label>/`.
- `analyze.py <run dir>`: every quantity of one run. `report.py`: the five capture runs.
- `qualify.py`: step 1, against the qualification build `8179d54` (injections present).
- `overhead.py <prefix>`: step 1(c), probe off against on, from logcat only.
- `hooks.py` / `strip.py`: how the hooks were placed, and how build M removed the injections;
  `strip-injections.diff` is that removal.
- `capture-table.md`, `capture-cpu.md`: the results of the capture on build `2ae6027`.
