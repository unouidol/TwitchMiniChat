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
| H1, its runs of 2026-11-02 at 09:33 and 09:35 | `feature/5d-recyclerview-timeline` | `fc63adb` | `1d377b6e3611d337c6caffb1f2ae6bc031895246ff6d0b4653b412bcde021342` |
| H1 from then on, H3, M, B1 | `feature/5d-recyclerview-timeline` | `d11b629` | recorded under *Recorded results* as built |
| H2 | one mutant of `d11b629` per run | built after the predictions | each read back from the phone |
| C, qualification, control | `experiment/startup-5d-control` | `cee6695` | `94d68eb531f0b826b263af5600123f1ce3cb83ef4875c35c5b47509c92320fcf` |
| C, qualification, 5d | `experiment/startup-5d-treatment` | `a2e72ff` | `cb7fb26b40218f990a9a7905146eb2a1ff36da809b93f5759e02dd7b3d9b2a2a` |
| C, measurement, control | `experiment/startup-5d-control` | `301111b` | `6fc184a88261f5ee5e9123476dedc0dc612a9ffedc5542c5ef5595ac38a547d9` |
| C, measurement, 5d | `experiment/startup-5d-treatment` | `5851fde` | `0b23204146d875ee8e0c257668e443f07b4f9cbab8ae4e62a88f8d057920e55a` |

The measurement branches are never merged. `301111b` is `c8a9f99` plus the probe; `5851fde` is
`fc63adb` plus the same probe. `cee6695` is `301111b` plus the injected delays (`ccd2c46`) and the
200 ms decision below; `a2e72ff` is `5851fde` plus the same two commits, the second cherry-picked, and
the two pairs' diffs are identical. Each embeds only its own commit (read from its dex strings). After
the round, the phone is left on the final 5d build, read back and recorded in `RELEASE-STATE.md`.

`d11b629` is `fc63adb` with the harness's instrument fixes of 2026-11-02 (*H1 on `fc63adb`,
diagnosed*, below) and the round's documents. `git diff fc63adb d11b629 -- app/src/main gradle
build.gradle.kts settings.gradle.kts gradle.properties app/build.gradle.kts` prints nothing: 5d's
production code is `fc63adb`'s, so the qualification and measurement builds, made from `fc63adb`,
still measure it. The mutants are made from `d11b629`, so that each one runs the fixed harness.

### Every APK of the round comes from one machine

The comparison that decides 5d is control against treatment, and the Java Development Kit (JDK) is an
input no commit records: the gap #83's KDoc documents beside `gitShaForBuild`. Built on two machines,
the two sides could differ by JDK with nothing to show it. So every APK in the table, the mutants of
H2 included, is built on one machine, which holds the JDK constant within the round. If the round is
split across sittings, the measurement section, the qualification and the twenty launches after it,
is one sitting on one machine. A harness stage may fall in a different sitting from the next one, but
no single stage may be split.

**The machine, read on 2026-11-02:** `DESKTOP-BCT68L9`. Each APK built from the worktree
`C:\Users\Fede\StudioProjects\TwitchMiniChat-wt-5d`, from a tree where `git status --porcelain`
printed nothing before and after the build, with `:app:assembleDevDebug`, in one tree, in this
order: `fc63adb` first, every one of its 42 tasks executed, then `cee6695`, `a2e72ff`, `301111b` and
`5851fde`, each incrementally on the one before, as Gradle reported. The APKs and each build's log
are kept outside any clone, in `C:\Users\Fede\StudioProjects\tmc-5d-round\apks`, so that no later
build overwrites one. AGP 9.2.1 and Gradle 9.4.1, from the tree. `gradlew --version` there:
`Launcher JVM: 21.0.10 (JetBrains s.r.o. 21.0.10+1-b1163.110)`, `Daemon JVM: Compatible with Java
21, JetBrains, nativeImageCapable=false (from gradle/gradle-daemon-jvm.properties)`. That file is
git-ignored and holds `toolchainVendor=JETBRAINS`, `toolchainVersion=21` and foojay URLs. The daemon
runs
`C:\Users\Fede\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2\bin\java.exe`, which reports
`OpenJDK Runtime Environment JBR-21.0.10+1-1163.110-jcef`. Android SDK `D:\Android\SDK`. Both
flavours' `google-services.json` are the clone's own, not the CI placeholder: the Firebase project of
the dev build already on the phone (`d56812b`), checked by its project id and number in that APK's
resources. Signed with this machine's debug key, certificate SHA-256 `9b3a523b…5c51`, the signer of
the dev app the phone already held, so each install replaces it in place, with its accounts.

The hashes this table carried before 2026-11-02 were built in another clone, for `fc63adb`, the 50 ms
qualification commits `ccd2c46` and `878f7de`, `301111b` and `5851fde`. None is used. None was
installed on the phone either: its dev app was last updated on 2026-10-23 09:23, before any of
those commits existed.

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

#### H1 on `fc63adb`, diagnosed (2026-11-02)

Each mismatch was diagnosed to a cause, with its evidence, before it was classified: (a) a defect in
5d's code, (b) a defect in the instrument, (c) a wrong prediction about correct behaviour. All four
are (b). None is (a): no line of `app/src/main` changed to fix any of them. The sources cited are
RecyclerView 1.3.1's, which `devDebugRuntimeClasspath` resolves, and line numbers in the harness are
`fc63adb`'s.

- **R2, `noPoolRecycle` alone (scrolled 201 px).** R2's other five checks passed, the holder in the
  cache among them, and R3 passed all of its own, the same holder with no new load among them.
  201 px is 29 (the list's top padding) + 52 + 52 (rows 0 and 1) + 68 (the emote row, row 2), so
  rows 0 and 1 leave the view before the emote row does. The cache holds 2 (`DEFAULT_CACHE_SIZE`,
  RecyclerView.java:6528); `scrollBy` never starts the prefetcher that would enlarge it (:1940-1954;
  only :2020, :3874 and :5915 post to it); and a full cache sends its oldest holder to the pool,
  through the RecyclerListener the proof counts, before it takes the new one (:7103-7105,
  :7049-7058, :7176-7177). The clause asserted that no holder at all reached the pool, where its
  KDoc asked for "no pool recycle of it". **Cause:** the clause demanded what RecyclerView cannot do
  with the emote row at position 2. **(b).** `d11b629` scopes it to the emote holder and asserts the
  forced eviction exactly: the positions recycled during R2 must be [0]. Recorded, row 0's eviction
  becomes an observation rather than an inference.

  **What R2's failure did not do:** it did not refuse to let R3 claim the cache path. Its failing
  clause was a false alarm about another row, and the guards that protect R3 (`inTheCache`, in R2
  and as R3's precondition, and `noNewLoad`) all passed. Nor could it discriminate: it failed on
  every build, so M1's predicted R2 failure proved nothing. H1 establishes that the cache path is
  **reached** on the clean build. Whether the defect deduced on 2026-10-07 is **observed** is H2's
  question, and M1 must fail R3.
- **Direct bind, state 0 / 9, declared exceptions met 0.** Both sides are production's: one echo
  created at its last status, and the same echo built through its history by the same adapter. The
  direct bind rendered its view (RebindProof.kt:353) and then snapshotted it (:354); the fresh builds
  were snapshotted (:97) before anything rendered them. The pairs check snapshots before rendering on
  both sides, and passed 658 / 658. The parity pass renders before it snapshots, and it lists a
  rendered echo's message view with four spans: `DynamicLayout$ChangeWatcher`,
  `TextView$ChangeWatcher`, a `ForegroundColorSpan` and `EmojiTransformationMethod`. The first is
  attached by a TextView's layout to Spannable text, and draws nothing. **The decisive control:**
  `echo.sending` differed by one span too, and its last status is its first, so both sides made the
  identical call with identical data. That rules out the data and the bind, and is why this
  diagnosis is not a story. **Cause:** a view snapshotted after its layout, compared with one
  snapshotted before. **(b).** In `d11b629` both sides are snapshotted after their layout, and the
  spans each side's first layout added are recorded by class, so that the watcher is observed, not
  inferred. Pixels 9 / 9 with state 0 / 9: only the non-graphical comparison could see it.
- **R1, R5 and R7, `pixelsEqualTheReference` alone.** Each delivered the held images and drew at
  once: the list's row at its bounds as they stood, against a reference that render had laid out
  (RecyclerProof.kt:296-299). Each delivery requests a layout (TwitchEmoteImageLoader.kt:390), which
  runs at the list's next layout. The emote row is 52 px without its images (`msg.emotes` in the
  parity pass, which has no loader: 110 px less 58 of container padding) and 68 px with them (the
  emote layer's, 126 less 58). Controls: R3 compares the same holder with the same reference after
  a scroll has laid it out, and passed; the lifecycle proof renders both sides before it compares,
  and passed 16 / 16. **Cause:** the row was drawn before the layout its own delivery had asked for.
  **(b).** In `d11b629` the list is laid out after each delivery, as the next frame would lay it
  out, and the row's pending layout, its heights and both bitmaps' sizes are recorded. **Not the
  direct bind's root cause:** another mechanism at another line, and fixing either leaves the other
  failing. But the same family: a view observed before a layout its own content asked for.
- **S1, `theLayoutReportedAStillScrollWhileTheNewestRowHadNoView` alone (`stillReports` 1).** The
  layout reported once with no movement, and at that report the newest row had a view. When the row
  scrolled to has no view, LinearLayoutManager lays it out from the end, its bottom at the bottom
  edge (LinearLayoutManager.java:960-967), then fills past that edge by `getEndPadding()` (:613-614,
  :669-672), the list's bottom padding, 29 px (OrientationHelper.java:431-432). S1 appended one row
  after the one it scrolled to, and a single row always gets a view in that fill. **Cause:** S1
  never built its race. S1 was wrong, not the rule. **(b).** `d11b629` appends two rows after the one
  scrolled to; the second begins 52 px past the edge, below the fill.

  **The still-scroll check earned its keep.** S1's other checks passed without the race existing.
  Without that check, S1 would have gone green while testing nothing: a vacuous pass inside a test
  written to catch a race. Sharper still, M7 would have passed S1 as well. With one pending row the
  newest ended 52 px below the edge, within `bottomThresholdPx` (72 dp, 171 px), so "near the
  bottom" answered yes even when asked (reasoned from the code; the run recorded no gap). **The
  zero-movement rule is not yet shown right.** It is not shown wrong either: nothing has exercised
  it. Until S1 is re-run with its race, the start-up race's fix is untested code, and nothing this
  work established includes it.

**Two of the four failures, arguably three, are one property:** a snapshot or a draw taken before a
pending layout is not comparable with one taken after it. The pairs check got this right by the
order of its calls; the direct bind and the recycler proof's draw got it wrong independently.
`d11b629` makes the order structural, not a discipline. `Settled`, which every snapshot, every draw
of a row at its bounds and every render calls, fails at once, naming the view, unless the view is
laid out with no layout pending in it or in any shown descendant. Of the two forms, it is the
assertion, not helpers that lay the view out themselves, for two reasons:

- A list's rows are laid out by their list, at the list's height, which only the proof knows. A
  helper laying a row out alone would put it in a state production never reaches.
- A helper that laid every view out would hide a row that never asks for the layout its content
  needs, such as a `requestLayout` missing after an image arrives. That is production behaviour
  under test, and the assertion leaves it to show in pixels.

The one state it can require is "laid out", because the parity pass and every list row can be read
only after a layout. So the direct bind's references moved to after their layout, not the direct
bind to before its own, and the pairs check moved with them. GONE views are not held to it: no
layout lays one out.

**Corrections, both recorded.** Two of the owner's premises were retracted on 2026-11-02 after the
diagnosis: that R2's assertion had done its job (its failing clause was a false alarm about another
row), and that the direct bind's reference was assembled from frozen parts (both its sides are
production's). The amendment of the same morning, written by the agent, said criterion 2 does not
read `stall`; that was its inference, not the plan's text (*Qualification*, below).

#### From `d11b629` on: what changes, pre-registered on 2026-11-02 before its first run

Every count of *Proves it* above stands, unchanged. The instrument changed, so the changes it makes
are written here beside the counts, and the old ones stay readable above. Two assertions are new:

- **new, `Settled` refuses nothing:** no snapshot, draw or render of an unsettled view, so both
  themes reach their done line, no pass aborts and no step records an error;
- **new, R2's `onlyTheForcedEvictions` passes,** with `positionsSentToThePool` [0]: rows 0, 1 and 2
  leave a two-slot cache in that order, and row 2's entry evicts row 0. On every mutant of H2 as
  well: none of them touches the cache or the order rows leave it. R2's scoped `noPoolRecycleOfIt`
  passes, and `scrolledPx` is 201, as on `fc63adb`.

Recorded, deciding nothing, each with the value the diagnosis predicts:

- **R1, R5 and R7:** `rowLayoutPendingAfterDelivery` true, the cause seen directly. For R1 and R5,
  `rowHeightBeforeItsLayoutPx` 52 and `rowHeightAfterItsLayoutPx` 68, and `rowBitmapPx` equal to
  `referenceBitmapPx`, 1022 × 68 (1080 less 29 px of padding on each side). R3, `rowBitmapPx` and
  `referenceBitmapPx` 1022 × 68. For R7, the height after the layout greater than before, and its
  two bitmaps the same size; no value is predicted, since the echo's heights were never recorded.
- **direct bind:** `layoutsAddedOnlyTheWatcher` 9. On each side of every echo, the first layout
  added exactly one span, `android.text.DynamicLayout$ChangeWatcher`, to the message view.
- **S1:** `stillReports` 1, at which the newest row had no view (`newestRowGapAtEachStillReportPx`
  ["none"]). Every S1 check passes.

Changed by the instrument, and the counts they reach unchanged:

- **pairs:** each view is laid out before each snapshot: A before B is bound, as a holder from the
  pool had been on screen, and the rebound view after. 658 / 658, `sessionsHeld` 658, `imagesHeld`
  658, not vacuous.
- **list checks:** 124 in each theme, 0 failures: `fc63adb`'s 121, and the three layouts the recycler
  proof now makes after a delivery.

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

**Restated on 2026-11-02, against the fixed harness, before any mutant was built.** The table above
stays as written, and `predictions.json` finalises it.

- **M1 and R2.** Until `d11b629`, R2 failed on every build, so M1's predicted R2 failure could prove
  nothing: a check that fails unconditionally cannot discriminate. Against the fixed R2, what M1
  does there depends on its exact edit, and the table's "5c's release on detach restored" decides
  it. The release 5c removed was posted, not made in the detach: `onViewDetachedFromWindow` paused
  the row and posted `release()` to the next turn of the main loop, to run if the row was still
  detached then (`aaf718f^`, TwitchEmoteImageLoader.kt:464-481). R2 detaches the holder and checks
  it within one turn, and `ProofSteps` runs R3 on a later turn. So, with that edit, **R2 passes under
  M1, `loadsLive` included**: its loads are still live and no release is reported when R2 reads
  them. The release runs before R3, and R3 fails: the same holder comes back from the cache without
  a bind, and without its images. The owner expected `loadsLive` to fail at R2. That holds only for
  a release made inside the detach, which is not 5c's edit. Which edit M1 is, is the owner's to fix
  in `predictions.json`, before it is hashed.
- **M7 and S1.** With S1's race built, M7 fails S1 at `stillStuck`. The report with no movement
  reaches the page's listener while the newest row has no view, `isNearBottom` answers no, and the
  page stops following until the posted `scrollToBottom` runs. S1's later checks run after that and
  pass.

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
`arg=8000`).

**Each injection runs once per launch, never once per row.** The probe runs a delay at most once in a
launch - after the first, its `injected` returns at once - and each call site fires at one place
only: page 0's 100th history row, the 10th view build or bind, history post 200, `Application.onCreate`,
or anchor + 8000 ms. So a phase that sums one segment over every row of the span rises by one delay,
200 ms, and criterion 2's floor below, half the delay, is read against that single delay.

**The delay is 200 ms, for every injection that goes through `inject`** (the nine phases and `app`),
decided by the owner of the measurement on 2026-11-02; it was 50 ms in `ccd2c46` and `878f7de`.
The reason is in the paragraph that closes this list, which until then held the decision open.
`stall` is not one of those injections and keeps its 400 ms: it has a constant of its own
(`STALL_MS`), it was never 50, criterion 2 does not read it, and its window below is written for 400.
A delay four times larger perturbs four times as much, so before the qualification is relied on:

- **the rows in the span are read on every qualification launch,** injected or not, not only on the
  decision's launches. Baselines that differ among themselves are the account's case, under *The
  account* below. An injected launch that differs from equal baselines is the delay changing which
  rows are ingested;
- **whether any injected delay trips a timeout or changes which rows are ingested is reported,** per
  injection. If one does, the round stops there and is reported. No other number is chosen in its
  place.

Read from the code before any launch, statically, and so not a substitute for the launches: nothing
on the history path waits for the main thread with a deadline. Each page's request leaves on a thread
of its own with its window computed before any row of it is ingested. The deduplicator's 1.5 s window
applies only to a key equal to the one just before it, and two distinct history rows never share a
key (`id:<message id>`, or `hist:<ms>:<user>:<text hash>`). The buddy capture's 15 s window needs a
request in flight, and the echoes' 10 s timeout needs a sent message; a cold start has neither.

*Correction, 2026-11-02:* "criterion 2 does not read it", above, was the inference of the agent that
wrote that paragraph, not the plan's text. Nothing written before it said what `stall` qualifies or
how it passes. Criterion 5 below says both. And the `:791` above is `ccd2c46`'s line: on `cee6695`
and `a2e72ff` the sleep is at `:796`.

**What every quantity is measured in: wall time, and the delay is a sleep.** Every time the probe
records is `rel()`, `System.nanoTime()` less the anchor (`StartupProbe.kt:243` on the qualification
builds): row marks, inserts, view builds, the main thread's messages, frames. It counts time a thread
spends asleep or blocked as well as time it runs. The burst end comes from logcat's monotonic clock,
the same clock. The injected delay is `Thread.sleep` (`:791`), which passes wall time and uses no CPU.
So a sleep moves every phase it falls in, and criterion 4's containment and the phase's number read
the same clock. The probe's one CPU record is its per-thread snapshots, utime and stime from
`/proc` (`:705-746`), which `analyze.py`'s `cpu_phases` reads. Neither the decision rule nor the
breakdown uses them, and a sleep cannot qualify them: 2026-10-08 had busy-loop injections for that
(`spin`, `spinbg`), and this round's qualification builds have none. If the CPU figures are reported,
they are reported as unqualified context.

Decided before they run (the first three proposed on 2026-10-30 and approved; the fourth added by the
owner of the measurement the same day; the second rewritten by the owner the same day, below):

1. **the injection ran:** the probe recorded it, for at least 200 ms (400 for `stall`);
2. **the delay reached the output:** the reported value of the target phase rises over the
   baselines' median by at least half the delay, 100 ms. A pipeline check, and only that: it fails if
   the number `analyze5d.py` reports did not take the delay in, which criterion 4, reading timestamps,
   cannot see. It was "at least 45 ms above the baselines' largest" until criterion 4 was added. The
   largest-value reference was conservative against a false pass, and criterion 4 now rules out a false
   pass on its own, so the conservatism would only buy false failures: do not restore it. The cases it
   was checked against, scaled from 50 ms to 200: a correct instrument at the median plus 200, rise
   200, passes; 120 of 200 landing elsewhere, rise 80, fails, and criterion 4 fails with it;
   `analyze5d.py` dropping the delay, rise about 0, fails, where criterion 4 would have passed;
3. **the others did not:** every other per-row segment of the breakdown lies within the baselines'
   range widened by 10 ms on each side. The between-row parts are too noisy for this and are not held
   to it; the span total is not either, its launch-to-launch spread (142 ms on 2026-10-09) being far
   wider than that margin;
4. **the injection is accounted for,** whatever any phase's width. Criteria 2 and 3 compare a launch
   with other launches, and a phase whose range across launches is wider than the delay can hide the
   delay's leakage, all 200 ms of it, without failing either. This one is read inside the injected
   launch:
   - **inside its phase:** the interval the probe recorded for the delay lies inside the phase it was
     placed in - the target row's segment; for `build`, the target build; for `gap`, one main-thread
     message that carries no row. The part of the interval outside it is time the breakdown would put
     somewhere else. Bound: 1 ms.
   - **inside the main thread's log:** for every injection on the main thread, the interval lies inside
     one message the log recorded. The breakdown computes idle as the span less every message it
     recorded, so time the log does not see would land in idle, where nothing measures it. Bound: 1 ms.
     `app` is the one exception, by construction: it runs in `Application.onCreate`, before the probe
     installs the log, and before the span.
   - **the row's other work unchanged:** the target row's total less the delay, and each of its other
     segments, lie within the same row's values (same page, same history ordinal) in the three
     baselines, widened by 5 ms on each side.
   - **the total, reported:** the span's rise over the baselines' median, with their spread, and the
     change in idle.
5. **`stall` qualifies the burst end,** added on 2026-11-02 at the owner's request and derived below.
   The burst end is one of the two quantities the measurement decides, and `stall` is the only
   injection that reaches it. Qualified means that the detector moves to where the stall is, and
   that nothing else moves. In the `stall` launch, all of:
   - **the detector moves there:** the burst end `analyze5d.py` reports lies no earlier than the end
     of the stall the probe recorded, less 1 ms, and no later than the first frame the probe recorded
     after that end (`frames.ran`);
   - **from somewhere else:** every baseline's burst end lies before anchor + 8000 ms, where the stall
     is placed, so that the move is the stall's and not one the burst makes anyway;
   - **no segment moves:** criterion 3's bound, on every per-row segment;
   - **inside the main thread's log:** criterion 4's second clause, which already holds every
     injection on the main thread: the stall's interval lies inside one message the log recorded,
     within 1 ms.

**Where criterion 5's bounds come from,** each step from code, none from a launch:

- The stall is a message posted with `Handler.postAtTime` for anchor + 8000 ms
  (`StartupProbe.kt:319-323` on the qualification builds). `SystemClock.uptimeMillis`, which the
  handler reads, and `System.nanoTime`, which the anchor is, both read the monotonic clock. So the
  stall begins at 8000 ms or later, when the main thread is next free, and sleeps 400 ms
  (`STALL_MS`, `:796`).
- The probe's frame callback posts itself again at every frame until the window closes
  (`:335-353`), so a frame is always pending, and the stall delays it.
- Choreographer logs `Skipped N frames` when a frame starts at least 30 frame intervals after its
  vsync (`SKIPPED_FRAME_WARNING_LIMIT`, 30 unless `debug.choreographer.skipwarning` says otherwise),
  at the start of `doFrame`, before any callback runs (Android 12's `Choreographer.java`, read from
  its source on 2026-11-02). The pending frame's vsync comes at most one interval after the stall
  begins, so the frame starts at least 400 ms less one interval late. That is 30 intervals or more
  when 400 ≥ 31 intervals: at 78 Hz or more. At 90 Hz, 389 ms against 333, so the line is logged; at
  60 Hz, 383 ms against 500, so no line at all.
- So the line is logged after the stall's end and before the probe's own callback in the same frame,
  which is what `frames.ran` records. logcat's monotonic times have millisecond resolution and are
  truncated (`-v monotonic`, `run.sh:34`): hence the 1 ms.

What the bounds leave out is any disagreement between logcat's monotonic clock and the anchor's
beyond that resolution. That is deliberate: their agreement is what places every launch's burst end
on the anchor's clock, and the `stall` launch is the one launch able to test it.

**The display must be at 90 Hz, and on 2026-11-02 it is not.** Read over adb that day, before any
launch: the active display mode is 60.000004 fps, of the phone's 60 and 90; `peak_refresh_rate` is
60.000004 and the refresh range [0, 60]; `debug.choreographer.skipwarning` is unset. At 60 Hz the
stall logs no line, and criterion 5 cannot pass. The refresh rate also bears on every launch's burst
end, not only on this one: a frame becomes a `Skipped` line only after a 500 ms delay at 60 Hz,
against 333 at 90, and the owner's reason for the burst end quotes 90 Hz. The refresh rate is not
among what each launch records. Whether to set the phone to 90 Hz, and whether to record the rate
with every launch, are the owner's decisions, taken before the qualification; nothing here changes
the phone's setting.

**The window criterion 5 replaces.** Until 2026-11-02 the plan said `stall` "puts the burst end
between 8350 and 8600 ms, and moves no segment", phrased as an effect, with nothing saying a launch
outside it fails. It was neither derived nor observed. It was written on 2026-10-30 (`5daebe6`), ten
minutes after `ccd2c46` made the stall 400 ms, before any 400 ms stall had run; 2026-10-08's stall
was 50 ms (`strip.py` of `experiment/startup-cost-attribution`), which Choreographer never logs.
Against the derivation, its lower edge lies about 50 ms before the earliest the line can be logged,
which only an unstated allowance for clock disagreement would explain. Its upper edge allowed 200 ms
for the main thread's lateness at 8000 ms and for whatever ran between the stall and the frame, also
unstated. The launch records both itself, so criterion 5 reads them there instead of bounding them by
a figure. The window stays in the list below as it was written.

No script evaluates criteria 1 to 5 yet: `analyze.py` reports the injection's interval (`inject_ms`,
`inject_at`) and the burst end, and the probe's record holds `frames.ran`, which criterion 5 needs.

**Why the total is reported, not held to "rises by about the delay".** Two reasons, with numbers. The
span varied by 142 ms across 2026-10-09's three launches with the fix (2348 to 2490 ms). That was more
than the 50 ms delay. It is less than 200, but still most of it, so a launch's own variation could
still pass or fail "about the delay" either way. And a delay need not move the span's end: the main
thread had 56 to 156 ms of idle inside the span on 2026-10-08, and a delay can be absorbed there, as
much as 156 of its 200 ms, while the rows of the other page, whose history arrives at its own time,
set the end. Nor can "the target's rise plus every other phase's rise equals the total's rise" fail
as it stands: idle is computed as the span less every measured part, so the
parts always sum to the span. What that test is for - 120 of 200 ms going somewhere the instrument is
not looking - is caught where it can be seen: the delay's interval lies 120 ms outside its phase, or
outside any message the log recorded, and criterion 4 fails.

- `gap` moves "other messages"; `build` moves the view build inside the insert on the control and the
  bind on 5d; `app` moves `act_in` and nothing inside the span; `stall` puts the burst end between
  8350 and 8600 ms, and moves no segment (superseded on 2026-11-02 by criterion 5, which says why).

**Decided on 2026-11-02: 200 ms, for every injection.** Until then this paragraph held the question
open, and it stays as it was written, so the reason can be read. The owner adopted the proposal at its
end for its uniformity: no injection needs a special case. One case is left where criterion 2 can
fail a correct instrument: a launch whose phase happens to fall low in its own range. Against the
median, the injected launch's rise is the delay plus its own variation from the median, so it fails
when that variation is more than 25 ms below. For every other phase 2026-10-08 measured inside rows,
whose ranges were 13 ms or less, and for "other messages", whose lowest value was 6 ms below the
median, that lies outside anything seen. The phases this round adds have no history yet. For the
control's view build it is not outside: its five values on 2026-10-08 were 1507, 1555, 1560, 1591 and
1594 ms, and one of the five, 1507, lies 53 ms below their median. About one correct launch in five
would fail there, and a failed qualification stops the round. Proposed, and adopted on 2026-11-02:
the same delay for every injection, 200 ms instead of 50, so that half of it, 100 ms, lies outside
every range seen on 2026-10-08, with no injection treated differently. It needed new qualification
commits on both branches, `cee6695` and `a2e72ff`; the measurement builds did not change.

A qualification that fails stops the round before the measurement.

### Order of the launches, and what each records

1. Qualification on the control, then on 5d.
2. **The decision's launches, in one session, control first:** 5 cold starts of `301111b` with
   `full`, then 5 of `5851fde`. Each is a force-stop, 3 s, then `am start -W` (`run.sh`).
3. The overhead launches: 5 with the probe off on each build, and one `full calibrate` on each.

Recorded with every launch: the device model, serial, API and build; density and font scale; the
Stay-awake setting and the battery's temperature; the JDK and the AGP and Gradle versions each build
was made with (for this round, one machine's, under *Every APK of the round comes from one
machine*); both pages' channels as reported, with the probe's channel fingerprint; the history cap;
the APK's commit and hash; the probe's flag; the rows inserted in each span; the launch method and
time.

**The account.** The method is 2026-10-09's: an intent launch with `new_account_id`, two chat pages
on different channels, a channel at the history cap of 400. Which account that round's intent named
was not recorded; it joins what that round did not record, beside its pid scoping and its font scale,
the same family as the battery figures. It does not block this round, which compares with its own
control. What the account protects is the row count, and the positive control already holds it: every
launch must insert the same number of history rows in its span.

So one account is chosen at the first qualification sitting, before any launch of section C, and held
for every launch of the section: the qualification, the ten decision launches and the overhead
launches. Recorded: which account, both pages' channels as reported with the probe's fingerprints, and
each launch's rows in its span, with the equality they must satisfy: all equal, 800 if both pages
reach the cap. If the qualification baselines' row counts are not equal with that account, another is
chosen, before the decision launches and never after.

## Recorded results

Fill in each row when its case runs; add a run rather than overwriting one.

**The device, read on 2026-11-02 at the start of the round:** `adb devices -l` gave
`57ebfa06 device product:Nord_EEA model:AC2003 device:Nord transport_id:1`, already authorised for
`DESKTOP-BCT68L9`: no prompt was needed. `getprop` gave OnePlus, Android 12, API 31, build
`AC2003_11_F.24`; `wm density` 382 (physical 450), `wm size` 1080 × 2400, system font scale 1.15,
`stay_on_while_plugged_in` 7, battery 24.9 °C. Its dev app before the round was `d56812b`, sha256
`8519b894…`, last updated 2026-10-23 09:23, as `RELEASE-STATE.md` records it, read from the APK pulled
off the phone.

**Harness stages,** every pass with 0 input events unless the row says otherwise:

| Stage | Commit | Dev APK sha256 | Day and night, each | Time |
|---|---|---|---|---|
| H1, first run | `fc63adb` | `1d377b6e…` | the same counts as the next row, in both themes; void: its day pass recorded 46 input events, the phone was in use | 09:33 |
| H1 | `fc63adb` | `1d377b6e…` | parity 34 / 34, `rowsBeforeHiding` 1 / 1; controls behaved; emote layer 2 / 2, with a 404 2 / 2, its controls behaved; pairs 658 / 658, `sessionsHeld` 658, `imagesHeld` 658, not vacuous; C-R 34 / 34, no declared exception; preconditions 5 / 5; lifecycle 16 / 16; list checks 121, 0 failures. **Not as predicted:** direct bind pixels 9 / 9, **state 0 / 9, declared exceptions met 0**: every echo's message view has 4 spans against the reference's 3, and `echo.unconfirmed-confirmed` has the declared text difference as well; **recycler 4 / 8**: R1, R5 and R7 fail only `pixelsEqualTheReference`, R2 only `noPoolRecycle` (scrolled 201 px); **scroll 2 / 3**: S1 fails only `theLayoutReportedAStillScrollWhileTheNewestRowHadNoView` (`stillReports` 1). Day and night failed the same checks. The round stops here until this is understood; H2 has not started | 09:35–09:36 |
