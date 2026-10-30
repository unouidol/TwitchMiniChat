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
- `decide.py <5 control runs> -- <5 5d runs>`: the decision rule, as approved before the runs.
- `overhead.py <prefix>`: the script of `a091809`, unchanged: the probe off against on, logcat only.
- `run.sh <label> "<flag>" [account id]`: one cold start. Unlike `a091809`'s, it never deletes the
  phone's files: deleting them is a step of its own, after the copies are checked.

Flags: `full` (the measurement), `off`, `full calibrate`, `full visibility` (the control's viewport
walk, off in the measurement), and on the Q builds `full inject=<name> [arg=<n>]`.
