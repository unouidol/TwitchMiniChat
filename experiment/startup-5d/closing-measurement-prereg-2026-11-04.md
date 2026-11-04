# The closing measurement: pre-registration, 2026-11-04

Written on 2026-11-04, after the negative control and before any launch of the closing measurement.
It is not edited after it is hashed. Its sha256 and the time it was written are recorded in
`docs/manual-plan-5d-recyclerview.md` and in the commit that adds it.

## What stands

- **ρ does not change:** span 1.0 / 2.4, first stable frame (burst end) 2.6 / 4.0.
- **The thresholds stay** (1 + ρ) / 2: 0.708 for the span (0.70833…), 0.825 for the burst end.
- **The decision rule stays as `decide.py` has it,** blob `3f69c6a`, unchanged.
- **Criterion 3 is not adjusted,** and no qualification sitting is re-scored.

## 1. The scope: what the instrument is qualified for

Decided by the owner of the measurement on 2026-11-04, after the negative control.

- **Qualified: the closing measurement's two quantities, the span and the burst end.**
  - This rests on criteria 1, 2, 4 and 5 of the re-qualification of 2026-11-04.
  - Each passed on every injection of both builds that it applies to.
  - The measurement builds are the qualification builds without the injected delays: the same probe
    file, the same flag.
- **Criterion 1:** the ten 200 ms delays were recorded at 200.10 to 200.18 ms. That is 200.11 to
  200.18 on the control and 200.10 to 200.15 on 5d. `stall`'s delays were 534.59 and 535.03 ms.
- **Criterion 2:** every injected phase rose by at least 100 ms against the median of three
  baselines, interleaved at positions 1, 8 and 14.
  - The rises were 129.8 to 213.0 ms on the control, and 140.7 to 211.0 on 5d.
  - `stall` has no criterion 2, by design.
- **Criterion 4:** containment held, inside the phase and inside one main-thread message.
  - `app` has no main-thread clause, by design.
  - `stall` has no phase clause.
- **Criterion 5:** the burst ended inside its window on both builds.
  - Control: 8536.93 ms, in [8534.67, 8538.18].
  - 5d: 8536.45 ms, in [8535.41, 8537.44].
- **Criterion 3 is UNINFORMATIVE, by measurement.**
  - In the pre-registered negative control (sha256 `52e96461…`), with no injection anywhere, P_null
    was 0.6003.
  - Its fixed replica's band was mis-centred on the control. 52 of 99 held values (52.5%) lay above
    the highest baseline, against 1/4 expected.
- **Per-phase attribution is NOT QUALIFIED, and is out of scope for this round.**
  - No statement about WHICH phase 5d improves may be made from this measurement.
  - The span breakdown `decide.py` and `analyze5d.py` print is reported as numbers, and no
    attribution is drawn from it.
- **This is a narrowing, not a loosening.**
  - The round gives up claims, per-phase ones, and gains none.
  - No criterion was relaxed, and criterion 3 was not changed.
- **Criterion 3 neither blocks nor clears.** The leakage question, whether a delay in one phase
  appears in another, is unanswered.

## 2. The direction of the asymmetry, corrected

This replaces the owner's earlier statement of the opposite direction.

- **The inequality.** For 0 < t < c and 0 < k < t: (t − k) / (c − k) < t / c.
  - Multiply both sides by c(c − k), which is positive. That compares c(t − k) with t(c − k), that
    is ct − ck with ct − tk.
  - Cross-multiplying leaves ck against tk.
  - Since c > t, ck > tk, so the left side is the smaller.
- **What it means.** Removing a constant common to both arms LOWERS the ratio.
  - Suppose ρ's baselines, 2.4 s and 4.0 s, held the catalogue parse's cost k, and these builds do
    not pay it (both carry the gate).
  - Then the ratio the prediction's own model expects for these builds is below ρ.
  - So the thresholds are EASIER to pass, not harder.
- **Therefore HOLDS is the weak verdict, and FAILS would be the strong one.**
  - HOLDS carries the confound of L1 to L4 (section 3).
  - FAILS would have been reached against thresholds the confound could only have made easier.
- **The owner's retraction, recorded.**
  - The owner's instruction of 2026-11-03 stated (t − k) / (c − k) > t / c.
  - The owner retracts it. The reason, in the owner's words: "a direction asserted from algebra that
    had not been carried out".
  - The plan had corrected the inequality before the sitting of 2026-11-03 (*The provenance of ρ*,
    *The direction*). This records the retraction and its reason.

## 3. The confounds, live

Each is named here, not corrected for. Each is reported with the result.

- **JIT contamination, with the control flattered.**
  - The builds are debuggable and uncompiled, and the JIT compiles inside the window being measured.
  - The control's span reaches the cheap regime, at about 2750 to 3000 ms of process age. 5d's span
    ends around 2000 ms.
  - So the control is flattered, and the measured ratio is conservative relative to a
    cold-against-cold comparison.
  - That direction is inferred from the cost-by-process-age curve, not measured directly
    (*Decided on 2026-11-02, after the qualification*).
  - Reported per launch, for both arms: the JIT thread's CPU across the span, and the history rows'
    cost by process age.
- **The app is uncompiled, `run-from-apk`, in every launch.** The compile state is read and
  recorded per launch. The forced compile was refused on 2026-11-02, and no compiled state is
  chased.
- **L1 to L4, on ρ's provenance** (*After the re-qualification, 2026-11-04*, item 2):
  - 2.4 s and 4.0 s were measured, and possibly contained the catalogue parse. Whether they did is
    not known.
  - 1.0 s and 2.6 s are targets, and carry whatever their baselines carried.
  - If the baselines held the parse, the thresholds are easier to pass than intended (section 2).
    HOLDS then carries that confound, and FAILS and INCONCLUSIVE need nothing further.
  - If they held none, there is no asymmetry. Neither case is claimed.
- **The pages' order.**
  - It differed between the arms in the qualification of 2026-11-04: one after the other in 13 of
    the control's 14 counted launches and in 10 of 5d's.
  - It did not differ in the re-qualification: 14 of 14 in each.
  - It differed again in the negative control. The control interleaved in 3 of its 14 counted
    launches (`Nc-plain-1`, `-7`, `-10`), and 5d in none.
  - The order decides at what process age each row runs. Its direction on the ratio is not known.
  - It is recorded per launch, in the three added quantities (`b`).

The directions are not one. JIT makes HOLDS harder to reach. The parse in ρ's baselines, if it was
there, makes it easier. The pages' order has no known direction. Nothing nets them against each
other.

## 4. The prediction this commits to, and the stopping rule

**The prediction under test,** handed over on 2026-10-09: the span from about 2.4 s to about 1.0 s,
and the first stable frame from about 4.0 s to about 2.6 s.

**The rule, as `decide.py` `3f69c6a` applies it.** For each quantity, each of the 25 pairings of a
5d launch with a control launch gives a ratio, 5d over control.
- **All 25 below the threshold: HOLDS.**
- **None below it: FAILS.** A ratio equal to the threshold counts as not below, as `classify` has
  it.
- **Some on each side: INCONCLUSIVE, and the round STOPS.**
- **An inconclusive result is not reanalysed, not re-run with more launches, and not rescued by a
  different pairing.** It is recorded with all ten values and every pairing.
- **`decide.py`'s own voids stand as written:**
  - any void launch given: STOPPED;
  - the ten not running C T C T C T C T C T by host launch time: VOID;
  - more than one battery saver and power source state, across the ten and `--qualified`: VOID;
  - more than one display reading across the same launches: VOID;
  - a control launch with no burst: the burst-end comparison is VOID;
  - the rows in the span differing: VOID.
- **Printed beside the verdicts, deciding no more than `decide.py` says:** skipped frames (`FELL`
  only if every 5d total is below every control total), `better_than_control`,
  `not_better_on_either`, and `WHAT_THE_RESULT_SUPPORTS`.

**The agent's prediction: HOLDS on the span, and HOLDS on the burst end.**
- **The basis:** the negative control's 28 plain launches, on the qualification builds, with the
  same probe, the flag `full` and no injection.
  - The 196 span ratios, 5d over control, ran from 0.243 to 0.394, against 0.708.
  - The 196 burst-end ratios ran from 0.243 to 0.740, against 0.825.
  - Every 5d launch skipped fewer frames (56 to 131) than every control launch (239 to 289).
  - This basis is not the measurement builds. They lack the injection code, and nothing of theirs has
    been measured.
- **Also predicted:** skipped frames `FELL`, and `better_than_control` true on both quantities.
- If any verdict is other than predicted, the prediction is wrong, and it is reported as wrong.
- By section 2, a HOLDS is reported as the weak verdict, with the confound of L1 to L4.

## 5. The procedure

- **The builds,** with the STEP B values of *Which build each step applies to*, second table:
  - control: measurement `dea38cc`, sha256
    `d005e9014367be922a4230a7a842438e723cf5b65373cab23de8ea0609824402`;
  - 5d: measurement `a2f12f1`, sha256
    `ece097acf6a53230af9a000cc9ab6a81bfc942683b35710f06a4a7876dffd00d`;
  - package `com.fs.twitchminichat.dev`.
  - Both local APKs were read at those values on 2026-11-04.
- **The host scripts** are the run directory's, as for every launch of the section. Those are the
  qualification branches' tips:
  - `decide.py` `3f69c6a`, `launchcheck.py` `5d7c704` and `analyze5d.py` `20639fb`;
  - `analyze.py` `19baef6` and `run.sh` `48b46f9`.
  - The measurement branches carry an older `decide.py` (`f2eb343`) and no `launchcheck.py`. They
    are not used.
- **The order:**
  - one excluded launch, `X-M-excluded`, on `dea38cc`. Only its pages are read: both must be on
    `c8cb0e91` at 400 history rows, or the sitting does not start (*One excluded launch before any
    counted launch*);
  - then C1 T1 C2 T2 C3 T3 C4 T4 C5 T5: `Mc-1`, `M5-1`, … `Mc-5`, `M5-5`, each with the flag
    `full`;
  - `closing.sh` refuses a slot whose predecessor has no clean launch, or whose label has been used.
- **Each launch,** each a step of its own:
  1. The driver runs `adb install -r` of its build, before EVERY launch, both arms, including
     where the arm does not change. It reads the sha256 back from the phone and compares it with
     STEP B's value. A mismatch stops the sitting before the launch.
  2. The idle wait, shown and saved (section 6).
  3. The account is read from the key names.
  4. `run.sh` installs again, reads back, and launches: force-stop, 3 s, then `am start -W`.
  5. `run.sh`'s read-back is compared with STEP B's value again.
  6. `launchcheck.py` runs, then the three added quantities and the RESULT.
- **So all ten are uniformly first-after-install.**
- **Voids:**
  - a void launch is re-run in its place, as `-r1` to `-r3`, and a fourth void stops;
  - `VOID_STOP` and `PAGES_BAD` stop the sitting;
  - every counted launch must read both pages on `c8cb0e91` at 400.
- **A recorded wait on an `IGNORED_EXPIRED` row** stops the sitting (`CONTRADICTION`).
  - It is reported as a contradiction of the clean ingestions so far, not as a void.
  - Those are the morning qualification's 50, the re-qualification's 42 and the negative control's
    54: 146 `ignored_expired` ingestions on 2026-11-04, none followed by a wait.
- **The phone untouched:**
  - an input event inside a launch voids it, and it is re-run;
  - input before a launch is waited out, and the wait is shown and saved;
  - a launch whose idle record was not saved stops the sitting.
- **Read and recorded before the first launch and after the last** (`section-read.sh`, saved as
  `section-M-before.txt` and `section-M-after.txt` in the run directory):
  - battery saver, the power source, and the battery's level, status and temperature;
  - thermal status, and the display's app-request votes;
  - six refresh-rate readings;
  - the installed build's sha256.
- **The display:**
  - The first launch does not start until all six readings give 90.0 Hz. That is the reading of every
    launch named under `--qualified`.
  - The phone's settings are not changed.
  - If it does not read 90.0, the sitting does not start, and that is reported.
- **Recorded, not held:** the battery's level, status and temperature.
  - The plan asks for the phone charging before the decision launches. On USB power at level 100, it
    reads status full (5), as in the re-qualification and the negative control.
- **The scoring:**
  - The command is
    `decide.py <Mc-1 … Mc-5> -- <M5-1 … M5-5> --qualified <84 run dirs>`, run in the run directory
    on Python 3.10.0.
  - Each slot's clean launch is given, in sitting order.
- **`--qualified` names the 84 counted launches of 2026-11-04's three sittings on the current
  qualification builds:** the qualification (`Sc-*`, `S5-*`), the re-qualification (`Rc-*`,
  `R5-*`) and the negative control (`Nc-*`, `N5-*`).
  - The plan's rule holds one reading across every other launch of the section.
  - The overhead launches have not run.
  - All 84 read 90.0 Hz, and battery saver 0 on USB power.
- **Reported:**
  - every value `decide.py` prints, in full and uncut;
  - the 25 span pairings and the 25 burst-end pairings, and the two verdicts;
  - the JIT CPU inside each span, and the cost-by-process-age curve;
  - the three added quantities per launch;
  - each launch's idle record, and the two section readings.
- **Then STOP.** The verdicts are reported, and nothing is decided from them.

## 6. The driver

These files are outside the repository, on `nbpromec1`.

| File | sha256 |
|---|---|
| `closing.sh` (new) | `b669622c22fdaacabaa972db9cee9b8962afd13a5577f1a7a3b828697f2036fd` |
| `sitting.sh` | `b14d8a83637359814597ce6d0fc8d824ace6915440541d5f2ec144acb2df61ec` |
| `launch-one.sh` | `ec39ec4919df1a525d671a16a1313370c34be70d3036c927a6ac5965761500d7` |
| `idle-wait.sh` (new) | `42a209dc569fef67bfbcd3bd4b67a6397ebdcfdbdc38ce076e8f2c1564ba42c3` |
| `section-read.sh` (new) | `71525cfab4f5cfd5a4beb849516ea873960216b45cd44537fe1d8c4045b870c4` |
| `sitting_line.py` (unchanged) | `82276b62ce79df2834cadc69fe67c4f4377165e91c7b3b1d94aa167208b0dc00` |
| `sitting_extra.py` (unchanged) | `1b5bdc3b3c3df01b309f4e7fde54fe6abf7b691975349533e59847fd17e90059` |

**What changed since the negative control,** when `sitting.sh` was `af42bd13…` and `launch-one.sh`
was `6233bbc3…`:
- **The idle wait is shown AND saved on every launch.**
  - In the negative control, the first four launches' idle lines were neither shown nor saved. So
    whether the driver waited for input there is not known.
  - `idle-wait.sh` prints each line and appends it to `idle-waits/<label>.txt`, with the driver's
    display reading before `run.sh`.
  - `launch-one.sh` copies that record into the run, as `idle-wait.txt`.
  - `sitting.sh` stops if a launch saved none, and prints the saved record line by line
    (`idle-record:`), so that what is shown is what was saved.
  - `idle-wait.sh` takes `ADB` as `run.sh` does, so that it can be tested without the phone.
- **`sitting.sh` takes the measurement builds,** `mcontrol` and `m5d`, and the slots `excluded` and
  1 to 5, only under `SITTING=M`. Under it, it takes nothing else.
- **`closing.sh` keeps the order.**
- **`section-read.sh` reads the section** before the first launch and after the last.

**Tested without a launch, executed:**
- `bash -n` passes on all five shell files.
- `sitting.sh` refused each of five wrong combinations of build, slot and `SITTING`.
- `closing.sh` stopped `C1` before the excluded launch, `T1` before a clean `C1` and `C2` before a
  clean `T1`, and refused `C6`. No run directory was created.
- `idle-wait.sh` was run against a stand-in `adb`:
  - one window with 3 input events, then none: 3 lines shown and saved, exit 0;
  - never idle: 30 "input seen" lines and the refusal, exit 2.
  - What was shown was byte for byte what was saved.
- **A first attempt at that test did not use the stand-in.** `timeout` found the real `adb`, so it
  read the phone's input events twice for 20 s, passively, and found none. No install and no launch
  ran.

## What this does not do

- It does not change ρ, the thresholds or the rule. It does not re-score any sitting, and it does
  not adjust criterion 3.
- It does not decide the release.
- An INCONCLUSIVE result stops the round, as written.
