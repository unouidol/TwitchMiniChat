# Negative control on criterion 3: pre-registration, 2026-11-04

Written on 2026-11-04, after the re-qualification sitting of that morning and before any launch of
the negative control. It is not edited after it is hashed. Its sha256 and the time it was written are
recorded in `docs/manual-plan-5d-recyclerview.md` and in the commit that adds it.

## What stands

- Both sittings of 2026-11-04 stay **NOT QUALIFIED** on both builds, as recorded.
- Criterion 3 is not adjusted: not its bound, not its margin, not its baseline count.
- Neither sitting is re-scored. Nothing below is computed on either sitting's launches.

## The question

Does criterion 3 fail on launches where no injected delay exists anywhere? It is answered by
measurement, with criterion 3 untouched.

## The launches

- **Per build, 14 plain launches.** Each carries the flag `full` and no `inject=`, as a baseline does.
- **The procedure is the re-qualification's.** Every launch is a first launch after an install of its
  build, read back from the phone (decision 5's uniformity rule). The driver installs and reads back
  before `run.sh` does the same.
- **The order:**
  - the excluded launch first, on the control build;
  - then the control's 14, `Nc-plain-1` to `Nc-plain-14`;
  - then 5d's 14, `N5-plain-1` to `N5-plain-14`.
- **Why 14:**
  - it is the re-qualification's number and block length, so the plain launches meet the same
    conditions within a block (time, battery, temperature) as the launches whose failures they
    are to explain;
  - the decisive statistic's enumeration (364 bands) is over fourteen.
- **Voids** are re-run in place by the driver, as in the re-qualification, and stop it on the same
  conditions.
- **The section:**
  - battery saver off, on USB power;
  - one refresh rate held, on several readings;
  - the excluded launch's pages on `c8cb0e91` with 400 history rows.
  - The battery level and status are recorded as parameters, not held. The scripts hold battery
    saver and the power source only. The morning's qualification ran while charging, and the
    re-qualification at full.
- **Checks before the first launch and after the last:**
  - battery saver, power source, and several refresh-rate readings;
  - both pages on `c8cb0e91` at 400: the excluded launch reads them before; every counted launch
    reads its own, the last of each build after;
  - each build's sha256 read back from the phone: the driver does it twice before every launch, and
    the installed build is read back once more after the last.

## How criterion 3 is applied

- **It is not re-implemented.**
  - `negctl.py` hands each plain launch to `qualify5d.py`'s own `evaluate()` (blob `19d110c`), under
    an injection name that only selects which segments are held.
  - Only the criterion 3 it returns is read. Everything else `evaluate()` computes for that launch is
    discarded, since no injection exists.
  - The rule is therefore the re-qualification's: a held segment fails above the highest of three
    baselines plus 10 ms. The lower reference is printed and decides nothing.
- **Which phases: all of them, as in the sitting.**
  - A launch with no injection holds all nine segments. That is the path a launch whose injection
    site lies in no row takes (`gap`).
  - For the decisive statistic, each plain launch is also held as each of the ten injections
    criterion 3 applied to in the re-qualification was held: `mention` to `app`. (`stall` was NOT
    APPLICABLE.)
  - Those held 7 to 9 segments, not all nine: `insert`, and the control's `build`, held 7.
  - Holding all nine everywhere would make the plain launches fail more often than the injected
    launches could have. That would push the answer toward noise, so the decisive statistic is held
    like for like.

## The partition, and the base rate it carries

- **Leave-one-out is not used.** It changes criterion 3's baseline count, which stays three.
  - Scoring each launch against the other thirteen makes the count 13.
  - Scoring each baseline against the other two makes it 2, where a value exceeds the highest
    baseline with probability 1/3, not 1/4.
- **Every band is three launches.** For exchangeable launches:
  - a value lies above the highest of three baselines with probability **1/4**, before the margin;
  - for one segment, the number K of eleven scored launches above the band's highest has
    P(K = k) = C(11, k) / C(14, k) · 3 / (14 − k), with mean 2.75 and P(K = 0) = 3/14 ≈ 0.214.
- **The fixed replica, reported, deciding nothing.**
  - The band is positions 1, 8 and 14, as in the re-qualification. The other 11 are scored, with all
    nine segments held.
  - It is one band per build, and P(K = 0) = 3/14 per segment says how far one band's luck moves its
    count. So "fails on none" in a single replica is not evidence either way.
- **Every band, decisive.**
  - All 364 bands of three among a build's 14 launches are used.
  - For each band, the ten held sets are given to ten of the remaining eleven launches in every
    possible way (11! assignments), each equally likely. The count is computed exactly, not sampled.
  - This gives, per build, the distribution of failing launches that a sitting shaped like the
    re-qualification would show from these launches' own variation.
  - The two builds' distributions are convolved.

## Why the before-margin rate cannot decide

- **Averaged over every band, it is 1/4 identically, for any data without ties.** Of any four
  launches, exactly one is the highest. So it is not computed as evidence. In the fixed replica it
  is reported only as a check that the launches are exchangeable.
- **The re-qualification's 21% and 18% are that same quantity, for its injected launches.**
  - Lying at or below 1/4 shows no excess there.
  - It does not show whether criterion 3, with its 10 ms margin, fails on noise. The count after the
    margin, below, measures that.

## The decision rule, written before any number

- **The statistic, P_null.** Under the plain launches' own variation (every band, every assignment,
  both builds), it is the probability that a sitting shaped like the re-qualification shows at least
  **3** failing launches. Such a sitting scores ten launches per build.
  - 3 is the re-qualification's count: control `mention-r1` and `buddy`, and 5d `insert`, of 20.
  - Per launch, that is 15%.
- **P_null ≥ 0.05: CONSISTENT WITH NOISE.**
  - Criterion 3 cannot discriminate leakage from noise at three baselines.
  - The re-qualification's three failures are what the plain launches' own variation produces at
    least one time in twenty.
  - For orientation only, outside the rule: with independent launches, the threshold corresponds to
    a per-launch false-failure probability of about 4.2%. P(Bin(20, 0.042) ≥ 3) = 0.0495, and with
    0.043 it is 0.0525.
- **P_null < 0.05: TIED TO THE INJECTION.** The plain launches fail too rarely to produce three
  failures, so the three failures of the re-qualification are taken as real leakage. **The next step
  is then to diagnose them, not to measure anything.**
- **Before either, the section must hold.** Otherwise the negative control is VOID, decides nothing,
  and is reported as such.
  - one display reading and one battery state, within each build and across both;
  - no void launch scored;
  - the same rows in every span;
  - no launch carrying an injection.
- **Reported beside the verdict, deciding nothing:**
  - the fixed replica's failing launches and values, and its before-margin fraction against 1/4;
  - over every band, the per-launch failure rate with all nine segments held;
  - the expected number of failing launches of ten.

## The prediction it commits to

**CONSISTENT WITH NOISE**, P_null ≥ 0.05, on the combined result.
- The reason: in the re-qualification, the injected launches lay above the highest baseline no more
  often than exchangeable launches do, 21% and 18% against 1/4.
- If the result is TIED TO THE INJECTION, this prediction is wrong, and it is reported as wrong.

## Unchanged

- **The builds:** control `2e72e42`, sha256
  `d9970f278909e125ae15540a13cac1389e467fa3beaeb3ba5d2ff447c5c19ac8`, and 5d `550b414`, sha256
  `e881d0abaf9e13a2d5c62c3e20aa77c66b188766cfa364cdde6144c70182ac23`.
- **The scripts in the run directory:**
  - `launchcheck.py` `5d7c704`, `analyze5d.py` `20639fb`, `decide.py` `3f69c6a`;
  - `run.sh` `48b46f9`, `analyze.py` `19baef6`, `qualify5d.py` `19d110c`.
- **The driver,** outside the repository on `nbpromec1`:
  - `sitting.sh` changes only to accept the slots `plain-1` to `plain-99` (flag `full`) and the
    labels `Nc-*`, `N5-*` and `X-N-excluded`. The diff is one `case` arm and two usage comments.
    Its sha256 is now `af42bd1335793ec3a7cd37cea731f25e8e4067b738e6e3133eb0778ea7cf0b2b`.
  - These are unchanged:
    - `sitting_line.py` `82276b62ce79df2834cadc69fe67c4f4377165e91c7b3b1d94aa167208b0dc00`;
    - `sitting_extra.py` `1b5bdc3b3c3df01b309f4e7fde54fe6abf7b691975349533e59847fd17e90059`;
    - `launch-one.sh` `6233bbc308d2f591c3e8e2f4bcc61ada94f449a54132b364bf3991006499b878`.

## The scorer

- **`negctl.py`:** blob `0100391`, sha256
  `ba0ca3b33b2a31b023cea3a43b09e932561975987ba24cab1d17c4bb0f857815`, added beside this file.
- **`negctl_test.py`:** blob `72bbd6f`, sha256
  `77a94f66387a44ac04842960cead890e534d5a8448a160a1d8ba98968834f715`. It passes 28 of 28 checks on
  Python 3.10.0, all on synthetic inputs:
  - the exact distribution against brute force;
  - the held sets;
  - criterion 3 through `evaluate()` at, above and far below its bound.
- **A run on recorded data, for mechanics only.** It used 14 excluded records from before the fix,
  on `cc4c0cf`, which belong to neither sitting.
  - It ran in 39 s, and every distribution summed to 1.
  - No pass, failure or P_null from it was printed or read.
- **Usage:** `negctl.py <run dir C> <14 control labels> -- <14 5d labels>`, each build in sitting
  order.

## What this does not do

- It does not change criterion 3 or re-score either sitting.
- It does not decide the release.
- The owner's bound stands: this is the last instrument sitting before the release decision.
