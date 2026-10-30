# Device round — 5d, the chat timeline in a RecyclerView

**Which plan to run.** This plan covers 5d, the chat timeline moved from a LinearLayout in a
ScrollView to a RecyclerView fed only the shown rows, and the closing measurement of the start-up
work. Written before any of its device stages ran: every count below is a prediction, reasoned from
the code, and the measurement's rule was approved before any of its launches. Nothing here is
re-derived after a result is seen.

**A stage that reports no result has failed, whatever an exit status says.** No done line, no
results file, or a results file without its counts is a failure of that stage.

**The device** is identified at the start of each sitting with `adb devices -l`, never assumed.

## Which build each step applies to

| Step | Branch | Commit | APK (dev, arm64) sha256 |
|---|---|---|---|
| H1, H3, M, B1 | `feature/5d-recyclerview-timeline` | `fc63adb` | `c3e8cd05df82ca026fb87df6043d62ebd6b056fdfc87073f80ce44c8de26b361` |
| H2 | one mutant of `fc63adb` per run | built after the predictions | each read back from the phone |
| C, qualification, control | `experiment/startup-5d-control` | `ccd2c46` | `70154c5494d01d80dca2cc522753eeb0354214278efe99347bded26a1cfba128` |
| C, qualification, 5d | `experiment/startup-5d-treatment` | `878f7de` | `fb5381501004697f79f435ec2c66fdf57052c4eaa8a719ea2b22d0cc2570fa67` |
| C, measurement, control | `experiment/startup-5d-control` | `301111b` | `0003ce879295b4475f343f22315d411027378ca77f904a0a9cd75829d556c60e` |
| C, measurement, 5d | `experiment/startup-5d-treatment` | `5851fde` | `32043dd4804094bed612cf5f9af516133b7103fbff504eca67b1079bf30387fc` |

The measurement branches are never merged. `301111b` is `c8a9f99` plus the probe; `5851fde` is
`fc63adb` plus the same probe. Each embeds only its own commit (read from its dex strings). After the
round, the phone is left on the final 5d build, read back and recorded in `RELEASE-STATE.md`.

## H. The harness

Run as in 5c: both themes, the APK's hash read back from the phone, 0 input events, the results file
read back. Since 5d every production row sits in a RecyclerView laid out like `chatList`, and the
frozen ones in the LinearLayout `chatContainer` was; see the harness activity's KDoc.

### H1 — The 5d tree

**Proves it,** in each theme, exactly:

- **parity 34 / 34.** `msg.hidden` compares the absence of a view with one `GONE` view: in 5d a
  hidden row is no view at all. The row before its user is hidden, 1 / 1 (`rowsBeforeHiding`).
- **controls:** the three behave as in 5c.
- **the emote layer 2 / 2, with a 404 2 / 2,** and its controls behaved.
- **pairs 658 / 658,** sessions held 658, images held 658, not vacuous. Every view is now created and
  bound through the adapter (`AdapterBinding`), as a holder back from the pool is.
- **C-R 34 / 34,** with no declared exception. Its one exception in 5c was the hidden row's `GONE`,
  set by the sync on a view the list no longer has.
- **direct bind:** each of the 9 echoes bound at once at its last status against the same echo built
  through its history: pixels 9 / 9, state 8 / 9, and the 1 declared exception met exactly
  (`echo.unconfirmed-confirmed`, the hidden status line's text).
- **preconditions 5 / 5:** two from 5a-2, unbind's from 5c, the adapter's payload and the binder's
  view type from 5d. 5a-2's "an echo is bound as sending" is gone: it was reachable from the pool.
- **the lifecycle proof 16 / 16.** L7 and L8 now find the release at the list's next layout pass,
  not at the call; L10 finds the payload bind there.
- **the recycler proof 8 / 8:** R1 to R8 (`RecyclerProof`).
- **the scroll proof 3 / 3:** S1 to S3 (`ScrollProof`).
- **list checks:** every check passed, and there was at least one.

**Failing looks like.** Any other count. A different count stops the round until it is understood.

### H2 — Predictions, then mutants

**Do.** After H1, and only then, because the step list is final only once H1 has run, write
`predictions.json`: each mutant's exact edit, and the steps and rows it must fail, in both themes.
Hash it and timestamp it before the first mutant is built; never edit it after. Then build, install
and run each mutant alone, both themes, and for M4 the unit tests too.

The mutants, as reasoned before H1. The predictions file restates and finalises them.

| Mutant | Edit | Predicted to fail |
|---|---|---|
| M1 | 5c's release on detach restored | R2, R3 (the defect itself, seen on its own path for the first time), R4 (precondition), R8, L2, L3 and L4 for each emote row |
| M2 | `onViewRecycled` does not unbind | R4, R7, L7, L8 |
| M3 | a status change notified without its payload | L10, R6, parity and C-R on `echo.unconfirmed-confirmed` |
| M4 | system lines given the message view type | parity on `system.line`; `ChatRowViewTypeTest`, 2 of its 3 tests |
| M5 | `releaseAll` removed from `onDestroyView` | R8 |
| M6 | `isNearBottom` always true | S2, S3 |
| M7 | the scroll listener's rule for reports with no movement removed | S1 |

M7 was added after Phase 1: the rule it removes was decided while writing the code, and S1 exists to
test it. M4's C-R prediction from Phase 1 is withdrawn: both of C-R's builds go through the same
view type, so it cannot see M4.

### H3 — The clean final build, once more

Reinstall the clean final APK and run both themes again: the same counts as H1.

## M. Manual checks on the final build

5c's M1 to M6 again, and 5d's own:

- **Far up and back.** Scroll far up in a busy chat and back down: emotes come back in rows the list
  rebinds from the pool, on Glide's real timing.
- **Scrolled up while messages arrive.** The rows on screen stay where they are, the jump button
  counts the new rows, and tapping it returns to the bottom and clears the count.
- **A tap on empty space clears the channel field's focus.** With the channel field focused, tap
  between rows, on a system line, and below the last row of a short chat. Below the last row is new:
  the old container ended at its last row.
- **Swipe-to-reply inside the list,** on a message with an id.
- **An echo sent and confirmed,** and the same echo scrolled far away and back.
- **Hide a user, then show them again** from the blocked-users screen.

## B. The background measurement

B1 is re-run exactly as `docs/manual-plan-5c-emotes.md` writes it: the dev app's pid, a positive
control of at least 100 frame loads in the 20 s before Home, the count from +1.0 to +30 s, and its
three-way bands.

## C. The closing measurement

**What it decides.** Whether 5d removed the start-up cost the 2026-10-08 attribution found: the view
of every history row built at insert, of which about 15 are ever seen. #84's deferral of the
toolchain upgrade waits for this measurement to be taken and recorded, not for a particular outcome.

### The prediction, handed over, verbatim

Made on 2026-10-09, outside the repository, and recorded here as handed over, not adjusted: span from
about 2.4 s to ABOUT 1.0 s; first stable frame from about 4.0 s to ABOUT 2.6 s; skipped frames must
fall a great deal, with NO NUMBER.

Its baseline, handed over, verbatim: on 2026-10-09, three launches with #78's fix, same probe: span
2490, 2348, 2397 ms; first stable frame (burst end) 4245, 3991, 3939 ms; skipped frames 200, 227,
200. The same-session pre-fix control, run first: span 3645, 3344, 3017 ms; first stable frame 5066,
5104, 5364 ms; skipped frames 347, 327, 289. Three launches, not five. These are context; the
comparison is with this round's own control.

### The definitions

As `analyze.py` of `experiment/startup-cost-attribution` at `a091809` writes them, copied unchanged
into `experiment/startup-5d` (blob `19baef62`), which computes them for this round:

- **anchor:** `System.nanoTime` in `ProbeApplication.attachBaseContext`;
- **span:** the latest `insertOut` of a history row minus the earliest `begin` of one;
- **first stable frame:** the burst end, the time of this process's last Choreographer
  `Skipped N frames` line within anchor + 10 s;
- **skipped frames:** the sum of those lines' N.

**Why the "2 s rule" is not the first stable frame,** as given by the owner of the measurement,
verbatim: "At 90 Hz, 32 ms is three vsyncs, and every live message produces a frame of 33.3 ms. So
the rule never settles while the chat is still receiving messages: it does not measure the end of
start-up, it measures when the chat stops being live." The spread the 2026-10-08 capture shows for
it, 5721 to 10964 ms against 5290 to 5969 ms for the burst end, is that mechanism's symptom.

### What the span brackets, on each build

`begin` is the first statement of `appendChatLine`; `insertOut` is the end of `appendChatView`. On
every build each history row passes through both. Before 5d, the timeline insert between them also
built and bound the row's view and started its emote loads. In 5d it notifies the list, and views
are built later, only for the rows that become visible. So 5d's span excludes, by construction,
view-building work the earlier span contained, because that work no longer happens for rows nobody
sees. The prediction expected exactly this: about 1.0 s came from removing about 1.4 s spent building
800 views of which about 15 are visible (handed over, from outside the repository). The ratio does
not say the same work got faster. It says less work happens in that window, which is what the step
does.

**Why the span and the first stable frame are not redundant.** The view building that leaves the
span does not leave the first stable frame. Building the handful of visible rows happens during
layout, before the frame is drawn, so it is inside that measurement. The span says the ingestion
window shrank; the first stable frame says the user-visible settling shrank, including the work that
moved. The two together are what makes the result more than bookkeeping.

### The rule

- **The threshold, per quantity,** is the midpoint between no change and the prediction,
  (1 + ρ) / 2, with ρ the prediction's own ratio: span 1.0 / 2.4, so 0.708; first stable frame
  2.6 / 4.0, so 0.825.
- **ρ is fixed, whatever the control's absolute baseline turns out to be.** `a091809` forked from
  `859ae70`, and what the bracket contains has changed since (#86 moved view building into the
  controller; #89 changed who owns an emote session), unmeasured, because neither was expected to
  move start-up. So the control may not land near 2.4 s. If it does not, ρ does not change:
  re-deriving it from the control's absolute span would be choosing the rule after seeing a number.
  The control's absolute values are reported beside the 2026-10-09 figures, and a material
  divergence is recorded as a finding, that #86 or #89 moved something nobody measured, not as a
  licence. No threshold is needed for it, because no decision hangs on it. Two instrument
  differences from 2026-10-09 are known and stated with it: the viewport walk is off (it cost 4 to
  13 ms inside the span on 2026-10-08), and each row has two more marks.
- **All 25 pairings** of a 5d launch with a control launch give a ratio, 5d over control. Every
  ratio below the threshold: **HOLDS**. Every ratio above it: **FAILS**. Ratios on both sides:
  **INCONCLUSIVE**, and the round stops.
- **An inconclusive result is a result.** It is recorded with all ten values and every pairing, here
  and in `RELEASE-STATE.md`'s measurement entry. The rule is not re-derived, no launches are added to
  resolve it, and no definition is changed. What to do with a known inconclusive result is a separate
  decision, taken with the numbers visible; it does not rewrite the rule that produced them.
- **Positive controls.** Every control launch must show a burst, or the first-stable-frame
  comparison is void. Every launch must insert the same number of history rows in its span, or the
  run is void.
- **Why the no-burst cases are not symmetric.** A control launch with no burst voids the comparison;
  a 5d launch with no burst passes it. The control must show the phenomenon for the instrument to be
  qualified at all, which is B1's positive control in another form. The treatment not showing it is
  the success the step is for.
- **Skipped frames** "fell" only if every 5d launch's total is below every control launch's. "A great
  deal" cannot be tested without a number, and none is given.

`experiment/startup-5d/decide.py` applies exactly this rule.

### The span broken down

A total alone would say the span fell, not whether the model of why was right. Each build's span is
reported broken down the same way (`analyze5d.py`): inside each history row, the dedup with the
backfill mark and the mention user; the timestamp with the echo reconcile; the buddy capture; the
hidden-user check; the composer snapshot with the follow decision; the timeline insert, and the view
build inside it; the follow-or-count bookkeeping with the composer restore. Between rows, from the
main thread's own message log: idle, frames, the rest of each row's message, other messages. And the
view builds wherever they ran, by kind, inside a row or outside, with their emote requests. A cost
the model does not predict, left in 5d's span, is the most useful thing the measurement can find.

### The probe on two builds, and its own cost

The probe is one file, byte for byte the same on both measurement builds (`StartupProbe.kt`
`2c7607be`, `ProbeApplication.kt` `704ded58`). Its call sites, counted in each committed tree:

| | control `301111b` | 5d `5851fde` |
|---|---|---|
| ChatFragment | 24: the 22 shared, the viewport hook and the span it reads | 22: the same 22 |
| where a view is built | 2: around `ContainerTimelineViews.create`, inside the insert | 4: around `onCreateViewHolder` and `onBindViewHolder`, outside any row |
| MainActivity, TwitchEmoteImageLoader | 3 + 2 | 3 + 2 |
| total | 31 | 31 |

Equal counts, different composition. What the two builds pay differs in three places, each toward
the hypothesis, since the control pays more:

- **The viewport hook** loses its call site in 5d. Its cost was its walk: 4 to 13 ms inside the
  span on 2026-10-08. The walk is off on both builds (on only with the flag word `visibility`), so
  neither pays it. A no-op hook in 5d would have equalised the count but not the cost: there is no
  reconcile in 5d to call it from, and the walk was the cost. What the control still pays per call is
  the call itself, counted and timed by the probe in each run.
- **The build hooks** run once per history row inside the control's span (about 800 rows, two calls
  each) and only for laid-out rows in 5d. They are not neutralised: they record the work the step
  removes, and a no-op in their place would record a build that does not happen. They are quantified
  instead: each run's counts times the per-call cost the calibration measures.
- **Emote requests** follow the builds, and are counted the same way.

The message log's cost is per main-thread message, so it scales with each span's own length and
messages, counted in each run, times the calibrated cost per message. The CPU snapshots run on the
probe's own thread; their count and cost are recorded per run.

**Measured, per build:** 5 launches with the probe off against the 5 measurement launches, on what
logcat records for both (`overhead.py`, unchanged from `a091809`: the burst end, the skipped frames,
`Displayed`, `TotalTime`, the last history line); and one launch with `full calibrate`, which times a
hook call and a logged message after the window closes. The report gives each build's measured
overhead, the probe's cost inside each span, and the bias it puts on each ratio.

### Qualification, before the measurement rests on the probe

On each build's qualification commit, three baselines with `full`, then one launch per injection with
`full inject=<name>`: `mention`, `reconcile`, `buddy`, `hidden`, `beforeinsert`, `insert`, `follow`
(on page 0's 100th history row), `build` (the 10th view build or bind), `gap` (a message of its own at
history post 200), `app` (in `Application.onCreate`) and `stall` (400 ms at anchor + 8000 ms, with
`arg=8000`). Decided before they run:

- the injection ran: the probe recorded it, for at least 50 ms;
- **its phase moved:** that phase's value is at least 45 ms above the baselines' largest;
- **the others did not:** every other per-row segment of the breakdown lies within the baselines'
  range widened by 10 ms on each side. The between-row parts are too noisy for this and are not held
  to it; the span total is not either, its launch-to-launch spread being larger than 50 ms;
- `gap` moves "other messages"; `build` moves the view build inside the insert on the control and the
  bind on 5d; `app` moves `act_in` and nothing inside the span; `stall` puts the burst end between
  8350 and 8600 ms, and moves no segment.

A qualification that fails stops the round before the measurement.

### Order of the launches, and what each records

1. Qualification on the control, then on 5d.
2. **The decision's launches, in one session, control first:** 5 cold starts of `301111b` with
   `full`, then 5 of `5851fde`. Each is a force-stop, 3 s, then `am start -W` (`run.sh`).
3. The overhead launches: 5 with the probe off on each build, and one `full calibrate` on each.

Recorded with every launch: the device model, serial, API and build; density and font scale; the
Stay-awake setting and the battery's temperature; the JDK and the AGP and Gradle versions each build
was made with; both pages' channels as reported, with the probe's channel fingerprint; the history
cap; the APK's commit and hash; the probe's flag; the rows inserted in each span; the launch method
and time. The account each launch opens is the one 2026-10-09 opened, which must be supplied: it is
not recorded here.
