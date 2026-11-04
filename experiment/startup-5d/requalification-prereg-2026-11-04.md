# Re-qualification of section C: pre-registration, 2026-11-04

Written on 2026-11-04, after the qualification sitting of that morning, before any launch of the
re-qualification. It is not edited after it is hashed. Its sha256 and the time it was written are
recorded in `docs/manual-plan-5d-recyclerview.md` (*Decided on 2026-11-04*) and in the commit that
adds it.

## What this follows

- The sitting of 2026-11-04, 07:55 to 08:50, ended with both builds **NOT QUALIFIED**. That verdict
  stands as recorded (`qualification-2026-11-04/`).
  - Control `2e72e42`: criterion 3 failed on 8 injections, and criterion 2 on `insert` and `build`.
  - 5d `550b414`: criterion 3 failed on 9 injections.
- All 28 failing criterion 3 values, 19 on the control and 9 on 5d, lay below their bands. No value
  lay above one.
- `qualify5d.py` has not been run over the 2026-11-04 data under the rule below, and it will not be.
  The new rule is tested on a new sitting.

## The drift these changes rest on (TASK 1, `qualification-2026-11-04/drift/`)

The rows used are the ones no injection touches in any of a build's 14 launches: page 1's history
rows, and page 0's rows 11 to 99.

- **Control, page 1's history rows.** The rows ran at matched process ages: medians of 3258 to 3416
  ms in the baselines, and 3110 to 3457 ms in the injections.
  - `timeline_insert`: the baselines read 717.2, 747.7 and 685.4 ms. The eleven injections read
    649.5 to 690.7, median 668.3, so the gap of medians is +48.9 ms.
  - The baselines rank 1, 2 and 4 of 14. The exact probability that three launches drawn at random
    rank this high is 0.0055.
  - The view build gives +46.0 ms, `follow_or_count_composer_restore` +8.0, and the whole row +60.9.
    Each has the same 0.0055.
- **Control, matched process age.** Every row but the injection's target row was compared, in 100 ms
  bins. The baselines' per-row cost is higher in 20 of 21 bins, for `timeline_insert` and for the
  whole row alike.
- **Control, page 0's rows 11 to 99.** `timeline_insert` gives +29.7 ms, with an exact probability of
  0.063.
- **5d, page 1's history rows.** `dedup_backfill_mention`: the baselines read 90.7, 95.8 and 79.0 ms,
  and the injections 26.6 to 93.4, median 69.7. That is +21.0 ms, with an exact probability of 0.030.
  - At matched age, the baselines are higher in 7 of 8 bins.
  - On page 0's rows 11 to 99 there is no gap: +2.1 ms.
- **The shape is a step, not a trend.**
  - On page 1, across all 14 launches, Spearman against position is −0.51 to −0.68 on the control's
    series, and −0.47 (dedup) and −0.38 (whole row) on 5d's.
  - Among the eleven injections alone it is −0.04 on the control's page 1 and −0.13 on 5d's. No series
    falls monotonically.
  - The first three launches of each build's block sit above the rest, and the rest are flat.
- **What it is not.**
  - It is not the within-launch process-age effect. That effect is real: on the control, a whole row
    cost 2343 to 2850 µs at ages near 1590 to 1880 ms, and 1837 to 2230 µs near 3110 to 3460 ms. But
    the step holds at matched age.
  - By a static reading of `StartupProbe.kt`, it is not the `inject=` flag either. An injected launch
    differs from a baseline before its delay only by a few comparisons per call site, and those add
    cost; they cannot remove it.
- **What is not identified.** Within a build's block, launch position, clock time, battery level and
  temperature all move together, so the step's cause is unknown.

## Change (a): criterion 3 becomes one-sided

- **The rule:** each segment held to criterion 3 passes when its value is at most the baselines'
  highest plus 10 ms. Their lowest less 10 ms is printed beside it and decides nothing.
- **The reason:** a delay that leaks into another phase raises that phase. A value below the band is
  not the phenomenon the band exists to catch.
- **Made after seeing a failure.** This change is made after the 2026-11-04 failures were seen, and
  it is written down as such.
- **Not a claim that a low value cannot be leakage.** A different scheduling can lower a phase, so
  "cannot be leakage" would be too strong. What rules the low values in as something else here is the
  step measured above.
- **What it does not rescue:** no upward failure. None of the 28 launches of 2026-11-04 had one.
- **The value nearest its bound** was 5d `insert`'s `follow_or_count_composer_restore`, 0.0 ms below
  its upper bound. That is the segment right after the delay's.
- **Its cost.** The upper bound is set by the highest baseline. Under the step, that is the baseline
  inside it, so a launch after the step can carry an upward leak smaller than the step unseen.
  - On 2026-11-04's control, the step in `timeline_insert` was +140.8 ms summed over the span, and
    +48.9 ms on page 1 alone, as gaps of medians.
- **Implemented in `qualify5d.py` blob `19d110c`** (`e3ac8ab` on `experiment/startup-5d-control`).
  - Only criterion 3's comparison and its printed fields change.
  - `qualify5d_test.py` blob `0f1063f` passes 25 of 25 on Python 3.10.0.

## Change (b): the three baselines are interleaved through the sitting

So that the band and criterion 2's median span the step, each build's fourteen counted launches run
in this order:

| Position | Launch |
|---|---|
| 1 | base-1 |
| 2 | mention |
| 3 | reconcile |
| 4 | buddy |
| 5 | hidden |
| 6 | beforeinsert |
| 7 | insert |
| 8 | base-2 |
| 9 | follow |
| 10 | build |
| 11 | gap |
| 12 | app |
| 13 | stall (`arg=8000`) |
| 14 | base-3 |

The control's fourteen run first, then 5d's. The excluded launch comes first of all.

## Criterion 2 under (b)

- The rule does not change: a rise of at least 100 ms over the baselines' median.
- Its reference becomes the median of the baselines at positions 1, 8 and 14.
- On 2026-11-04 the control's `insert` rose 32.4 ms and `build` 73.4 ms over a median of three
  baselines that all lay inside the step.
- **Stated now, before any number exists:** suppose `insert`'s `timeline_insert`, or `build`'s view
  build, still rises less than 100 ms against the interleaved median. That is then a real gap in the
  instrument for the control's two longest phases, and not a criterion to loosen. The control stays
  NOT QUALIFIED on criterion 2, and it is reported as that gap.

## Unchanged

- **The builds:** control `2e72e42`, sha256 `d9970f278909e125ae15540a13cac1389e467fa3beaeb3ba5d2ff447c5c19ac8`,
  and 5d `550b414`, sha256 `e881d0abaf9e13a2d5c62c3e20aa77c66b188766cfa364cdde6144c70182ac23`.
  Each is read back from the phone before every launch, twice.
- **The scripts:**
  - `launchcheck.py` `5d7c704`, `analyze5d.py` `20639fb`, `decide.py` `3f69c6a`, `run.sh` `48b46f9`;
  - `analyze.py` `19baef6`;
  - the P1 script, `requal_p1.py` `916b1ee`, added beside this file. Run on the 2026-11-04 launches,
    it reproduces TASK 1's page-1 values and probabilities exactly.
- **Criteria 1, 4 and 5,** the display reading, the battery state, the rows in the span, criterion 3's
  exemptions, and the verdict's order. The phase locations are unchanged too.
- **The uniformity rule:** every launch is a first launch after an install of its build, read back
  from the phone. This is decision 5, and *Order of the launches* item 1 applies it.
- **Before the first counted launch:**
  - battery saver must read off, on USB power, with one refresh rate held for the section (90.0 Hz
    on 2026-11-04). Otherwise the sitting does not start;
  - the excluded launch must read both pages on `c8cb0e91` with 400 history rows. Otherwise the
    sitting does not start.
- **A recorded wait on an `IGNORED_EXPIRED` row** stops the sitting. It is reported as a
  contradiction of the diagnostic launch.

## The driver, which decides which launches count

The driver is outside the repository, on `nbpromec1`. Its labels are `Rc-*`, `R5-*` and
`X-R-excluded`. The files' sha256:
- `sitting.sh` `c771e1b70c358b8dfa83d02212e1369a598fcffff84f59259988e1ce0310eb98`;
- `sitting_line.py` `82276b62ce79df2834cadc69fe67c4f4377165e91c7b3b1d94aa167208b0dc00`;
- `sitting_extra.py` `1b5bdc3b3c3df01b309f4e7fde54fe6abf7b691975349533e59847fd17e90059`;
- `launch-one.sh` `6233bbc308d2f591c3e8e2f4bcc61ada94f449a54132b364bf3991006499b878`.

Its rules:
- **A void launch is re-run in its place**, as `-r1`, `-r2` and `-r3`, with its reasons written beside
  it first. This applies only when every reason is one a re-run can cure:
  - a late delivery whose waits are on no `IGNORED_EXPIRED` row;
  - a foreign process start;
  - input events.
- **A fourth void stops the sitting.**
- **Any other void reason stops it:** no span, no `waitRows`, a missing record, the install or its
  read-back, or battery saver, the power source or the display changing.
- `launchcheck.py`'s exit status 1 is read as "void", never as a crash.

## The predictions this commits to

- **P1, the step explanation, and the one (a) rests on.** On page 1's history rows, which no injection
  touches, the three interleaved baselines are not systematically the highest launches. This is
  tested on `timeline_insert` on the control and `dedup_backfill_mention` on 5d.
  - P1 holds when the exact probability that three launches drawn at random from the fourteen rank as
    high as the baselines exceeds 0.05, as `requal_p1.py` computes it.
  - On 2026-11-04 it was 0.0055 and 0.030.
  - **If P1 fails on a build,** the low values are tied to the baselines, not to their place in the
    order, and (a)'s justification falls for that build. That build is then reported NOT QUALIFIED,
    whatever `qualify5d.py` computes, with criterion 3's lower references beside it.
- **P2, recorded, deciding nothing.** The first baseline is the highest of the three on those two
  phases, since it alone lies inside the step.
  - On 2026-11-04 it did not hold on either build: `base-2` was the highest. But all three baselines
    lay inside the step then.
- **P3.** Criterion 3, one-sided, passes on every injection of both builds.
- **P4.** Criterion 2 passes on all ten injections it applies to on both builds, the control's
  `insert` and `build` included.
- **P5.** Criteria 1, 4 and 5 pass on both builds, as on 2026-11-04.
- **The expected verdict:** both builds QUALIFIED.
  - Any failure is reported with its numbers, and no criterion is adjusted.
  - A failed qualification stops the round before the measurement.
