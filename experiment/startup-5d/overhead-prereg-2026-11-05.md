# The overhead launches of the 5d round: what they could and could not change, written before them

Written on 2026-11-05 from 08:49 +0100, before any overhead launch, by the instruction of the owner of
the measurement: "Say in advance, before the numbers, what they could and could not change: the
probe is in both arms, so its cost is common unless it scales with something 5d alters, and the span
margin is 0.2835 to 0.3537 against 0.708. Write that expectation down before running."

The verdicts of 2026-11-04 stand as reported: span HOLDS and burst end HOLDS, both weak, and
skipped frames FELL. ρ does not change, the thresholds stay 0.708 and 0.825, and nothing here
adjusts them.

## 1. What runs, as the plan specifies it

- **The plan's two statements.**
  - *Order of the launches*, item 3: "The overhead launches: 5 with the probe off on each build, and
    one `full calibrate` on each."
  - *The probe on two builds, and its own cost*: "5 launches with the probe off against the 5
    measurement launches, on what logcat records for both (`overhead.py`, unchanged from `a091809`:
    the burst end, the skipped frames, `Displayed`, `TotalTime`, the last history line); and one
    launch with `full calibrate`, which times a hook call and a logged message after the window
    closes. The report gives each build's measured overhead, the probe's cost inside each span, and
    the bias it puts on each ratio."
- **The builds** are the measurement builds of the ten launches:
  - `dea38cc`, `d005e9014367be922a4230a7a842438e723cf5b65373cab23de8ea0609824402`;
  - `a2f12f1`, `ece097acf6a53230af9a000cc9ab6a81bfc942683b35710f06a4a7876dffd00d`.
  - They are the same local APK files, each compared with its sha256 before the sitting, and read
    back from the phone before every launch.
- **The order:**
  - `X-O-excluded` (`dea38cc`, `full`) counts for nothing. Its sitting line checks that both pages
    are still on `c8cb0e91` at 400 rows today; if not, the sitting stops.
  - Then `Kc-1` (`dea38cc`, `full calibrate`) and `K5-1` (`a2f12f1`, `full calibrate`).
  - Then `Oc-off-1`, `O5-off-1`, … `Oc-off-5`, `O5-off-5`, flag `off`, alternating, control first.
  - The order is the agent's: the plan fixes none. It ends on `a2f12f1` with the probe off, and
    `run.sh`'s `off` path removes the probe's flag file in the app's own directory.
- **The account and the pages** are the section's: the account is read by its prefix `0d3d33b8…`
  from key names, as in every launch of the section.
- **Each launch,** by `overhead.sh`:
  - an install, read back from the phone, before the launch;
  - `launch-one.sh` unchanged, so the idle wait is shown and saved, and `run.sh` makes its own install
    and read-back;
  - with the probe: `launchcheck.py`, `sitting_extra.py` and `sitting_line.py` as in the closing
    measurement, and `calibration.json` must exist;
  - without the probe: `offcheck.py`, below.
  - A void a re-run can cure is re-run in its place, up to three times.
- **The section's readings are held, and checked before each launch.** `decide.py`'s rule holds one
  display reading and one battery state across every launch of the section, the overhead launches
  included (*The refresh rate is held and recorded*).
  - The readings are `refreshRate=90.0 frameRateOverride=[] skipwarning=[]` and
    `saver=0 ac=false usb=true wireless=false`.
  - Before each launch the driver reads both as `run.sh` reads them. If either differs, it STOPS
    BEFORE THE LAUNCH: a launch at another reading would void the round.
  - After each launch, the run's own readings are compared with the section's. A difference stops the
    sitting, and is reported.
- **What a probe-off launch can be checked for.** `launchcheck.py` opens `probe.json` first, so it
  cannot read one.
  - `offcheck.py` applies the conditions that need no probe, through `launchcheck.py`'s own functions,
    loaded unchanged:
    - the app's own `Start proc` line;
    - no foreign process start before overhead.py's window ends, 10300 ms after that line;
    - no input event;
    - the install's read-back;
    - battery saver and the power source held during the launch;
    - the display reading held.
  - **Not checkable without the probe, and so not checked:** a late history delivery, the span, the
    rows in the span, and the pages' channels and counts.
    - A late delivery in a probe-off launch would lengthen its burst end. It would therefore make that
      build's measured overhead, on − off, smaller.

## 2. What they could and could not change, before the numbers

**They cannot change the verdicts.**
- `decide.py` scores the ten measurement launches under a rule fixed before them. The plan has no
  rule that re-scores the ratios with the probe removed. The probe's cost is reported beside the
  verdicts, as the plan says.
- ρ, the thresholds and the weak label are not touched.

**They can change two things.**
- **The round's validity.** An overhead launch whose display reading or battery state is not the
  section's would void the round, under the rule above. The driver stops before such a launch rather
  than make it. `decide.py` is run again afterwards, with the counted overhead launches added under
  `--qualified` after the 84 launches it already names, and its void fields are compared with
  `decide-M.json`'s.
- **The bias reported beside each verdict.** If the probe-free ratios came near a threshold, the
  verdict would carry a second named confound: the instrument's own.

**"Common unless it scales with something 5d alters": it does, and here is how.** Read from the ten
launches' own records on 2026-11-05, before any overhead launch: their probe dumps, unchanged since
2026-11-04. The timed probe events are each a recorded `rel()` time inside the span.

| Inside each span | Control `Mc-1` to `Mc-5` | 5d `M5-1` to `M5-5` |
|---|---|---|
| timed row events (begin, insertIn, insertOut, mark0 to mark4) | 6400 in each | 6400 in each |
| timed build events (start, end) | 1600 in each | 116 to 288 |
| timed frame events | 1 or 2 | 1 to 3 |
| main-thread messages (`analyze.py`'s `messages_in_span`) | 875 to 1051 | 949 to 1213 |
| timed events per ms of span | 3.32 to 3.60 | 8.37 to 9.69 |
| messages per ms of span | 0.394 to 0.466 | 1.227 to 1.773 |

- **The consequence is algebra, not a measurement.**
  - Removing the probe's cost p from each span turns a pairing s5 / sc into (s5 − p5) / (sc − pc).
    That is lower exactly when p5 / s5 > pc / sc.
  - With p = h × events + m × messages, for per-call costs h and m that are not negative, that holds
    whenever both densities are higher in 5d. They are, in all 25 pairings.
  - **So, on the counted terms, every probe-removed span ratio is below its measured ratio, whatever
    the calibration gives.** The overhead launches can size the span's bias, but they cannot move it
    toward 0.708. On the probe's account, the measured span ratio is conservative.
  - The reason: 5d's span holds the same 800 rows' hooks in a third of the time, so a common cost
    per row is a larger share of it.
- **This reverses, for the ratio, a reading in the plan.** *The probe on two builds, and its own
  cost* says the three differences run "each toward the hypothesis, since the control pays more".
  - That holds for the absolute cost of the build hooks: 1600 timed events against 116 to 288.
  - It does not hold for the ratio, which depends on cost per ms of span.
  - The plan's sentence is not changed here. This is recorded for the owner.
- **Its limits.**
  - Hook calls that record no time are not counted: counters, early returns, and rows' copies of
    build times.
  - The probe thread's CPU snapshots are not counted either: 21 to 23 per launch, on another
    thread.
  - So the conclusion holds for the counted terms only.
- **The span's margin, for the record.** With 5d's probe cost taken as zero, the highest pairing,
  `M5-2` over `Mc-2`, reaches 0.708 only if 1113.0 ms of the control's 2223.1 ms span were the
  probe's.

**The burst end is not settled by the counts.**
- Its bias comes from `overhead.py`'s measured overhead, d = mean(on) − mean(off), per build. The
  pairing becomes (b5 − d5) / (bc − dc).
- With d5 taken as zero, the highest pairing, `M5-2` over `Mc-5`, reaches 0.825 only if dc were
  896.5 ms or more, out of 3769.7 ms.
- **What n = 5 can resolve.** Within the measurement session the burst ends had a standard deviation
  of 285.2 ms in the control and 124.0 ms in 5d.
  - So a difference of two means of 5 has a standard error of about 180 ms and 78 ms, before any
    drift between 2026-11-04 and 2026-11-05.
  - The probe-off launches run a day after the probe-on launches they are compared with, so that
    drift is confounded with the probe's cost.
  - A difference smaller than about twice the standard error cannot be told from noise.

**The agent's prediction.** Points 2 and 4 are guesses from the mechanism, not from a measurement.
1. The section's readings hold, and the sitting completes.
2. A timed hook costs no more than about 1 µs, and the dispatch log no more than about 50 µs a
   message. So the probe costs less than 100 ms inside any span of either build.
3. All 25 probe-removed span ratios are below their measured ratios, which the counts force, and so
   all 25 stay below 0.708.
4. `overhead.py`'s burst-end difference, on − off, is within the noise above for both builds. All 25
   overhead-removed burst-end ratios stay below 0.825.
5. `decide.py`, run again with the 12 counted overhead launches added under `--qualified`, gives the
   same verdicts and void fields as `decide-M.json`.

## 3. Procedure, scripts and hashes

- **The order of work.**
  - `section-read.sh` before the first launch, into `section-O-before.txt`.
  - `overhead.sh` runs the whole order.
  - `section-read.sh` after the last launch, into `section-O-after.txt`.
  - `overhead_report.py` then writes a new directory, `overhead-2026-11-05/`, in the run directory.
  - Then STOP and report.
- **The report, `overhead_report.py`.** Its docstring fixes what it computes:
  - `overhead.py` on copies of the two files it reads, each copy hashed against its source;
  - the calibration's medians;
  - the probe's cost in each span, as above;
  - the 25 pairings of each quantity, with the probe removed, against the unchanged thresholds;
  - `decide.py` run again.
- **New files, sha256, in the session scratchpad on `nbpromec1`:**
  - `overhead.sh` `2dd09430b2e342f6ef351db0f454732a4a8d279dae5e0497a7c38d794973f0d7`;
  - `offcheck.py` `28e73caba80ca01492f3f4283ad7f7efcbe950569d6940836bcc35e64c16f5af`;
  - `overhead_report.py` `5fde9fe2b9e31d42b96669da955e330eaa3974bbd7606288674e0f98261d5ab9`;
  - `qualified-84.txt` `364412a21ebea218d3cde18c1daccf4e0da8938c3881ce78a3b05abab9730c4a`. It lists the
    84 clean `Sc`, `S5`, `Rc`, `R5`, `Nc` and `N5` launches, rebuilt from their sitting lines. With
    `Mc-1` to `Mc-5` and `M5-1` to `M5-5` it reproduces `decide-M.json` byte for byte (executed).
- **Unchanged from the closing measurement's pre-registration:**
  - `launch-one.sh` `ec39ec49…`;
  - `idle-wait.sh` `42a209dc…`;
  - `sitting_line.py` `82276b62…`;
  - `sitting_extra.py` `1b5bdc3b…`;
  - `section-read.sh` `71525cfa…`.
- **The run directory's host scripts:** `decide.py` `3f69c6a`, `launchcheck.py` `5d7c704`,
  `analyze.py` `19baef6`, `run.sh` `48b46f9`, and `overhead.py` `6ca9d72`. The last is the same blob
  as at `a091809`, read on the commits.
- **Tested before the first launch, offline,** with no phone and no launch:
  - `offcheck.py` on copies of `Mc-1`'s files without `probe.json`: clean.
  - On a copy given one input event and an "after" reading of 60.000004 Hz: void, for both.
  - On `Mc-1` itself: void, since a `probe.json` exists.
  - The driver's two section checks: held on `Mc-1`, and STOP on the altered copy.
  - `overhead_report.py`'s cost function on `Mc-1` and `M5-1` gave `decide-M.json`'s span and burst
    end, 2394.5 / 3892.1 and 781.2 / 2037.0 ms.
