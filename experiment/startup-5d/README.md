# The 5d closing measurement - SCAFFOLD, never merged

Host-side scripts for the two measurement builds of 5d: `experiment/startup-5d-control`, the probe
on `c8a9f99` (the pre-5d timeline), and `experiment/startup-5d-treatment`, the same probe on the 5d
head. The probe is `app/src/main/java/com/fs/twitchminichat/probe/`, the same file on both branches.
Each branch has two commits: the measurement build (M), then the qualification build (Q), which adds
the injected delays and nothing else.

- `analyze.py`: the script of `experiment/startup-cost-attribution` at `a091809`, unchanged
  (blob `19baef62`). The span, the burst end and the skipped frames come from it.
- `analyze5d.py <run dir>`: one run; those three quantities, the span broken down, the view builds,
  and the probe's own counts.
- `decide.py <5 control runs> -- <5 5d runs> [--qualified <qualification runs>]`: the decision rule,
  as approved before the runs; since 2026-11-02 it also voids the round unless every launch it is
  given shares one display reading, and names what "no burst" asserts at that rate.
- `qualify5d.py <control|5d> <3 baselines> -- <injected runs>`: the qualification's verdict for one
  build, computed from the recorded values, every compared value printed beside it (2026-11-02).
  `qualify5d_test.py` runs it on hand-made launches.
- `overhead.py <prefix>`: the script of `a091809`, unchanged: the probe off against on, logcat only.
- `run.sh <label> "<flag>" [account id]`: one cold start. Unlike `a091809`'s, it never deletes the
  phone's files: deleting them is a step of its own, after the copies are checked. It reads the
  display's refresh rate before and after the launch (`refresh-rate.txt`). `ADB` and `PYTHON` name
  the machine's adb and Python 3; each defaults to the name on PATH.

Flags: `full` (the measurement), `off`, `full calibrate`, `full visibility` (the control's viewport
walk, off in the measurement), and on the Q builds `full inject=<name> [arg=<n>]`.
