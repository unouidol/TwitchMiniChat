# The drift across launches, 2026-11-04 (TASK 1)

Computed after the qualification of 2026-11-04 failed, from that sitting's recorded launches. Nothing
was launched, and `qualify5d.py` was not run over this data under any changed rule.

- `drift_rows.py` read each counted launch's `probe.json` from the run directory, which is not
  published, and extracted every in-span history row: page, ordinal, begin, the seven segments and
  the view build, in ms, formed as `analyze5d.py` forms them.
- `rows.json` is that extract for the 28 counted launches, in columns, rounded to 1 ns. Summed, it
  reproduces every criterion 3 value `qualify5d.py` printed (`../control.json`, `../5d.json`): 162
  values, largest difference 0.000000 ms.
- `drift.py` ran on the run directory and gives `drift.json`: per build, per row set no injection
  touches, the series in sitting order, Spearman against position with a permutation p (fixed seed),
  and the exact probability that three launches drawn at random rank as high as the baselines.
- `drift2.py` gives `drift2.txt`: the same check against the printed values, the trend among the
  eleven injections alone, each failing segment's gap by row set with each set's process age, and
  baselines against injections at matched process age.

To recheck from this directory: `python -I drift2.py rows.json ..` prints `drift2.txt`.
