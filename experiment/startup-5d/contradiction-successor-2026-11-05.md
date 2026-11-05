# The CONTRADICTION of `K5-1-r1`, and its successor rule: pre-registered before any further launch

Written on 2026-11-05 from 09:47 +0100. It was hashed and pushed before any further launch, and
before `residual_check.py` was run on `K5-1-r1`. This is by the instruction of the owner of the
measurement. Its words are quoted where they decide something.

## 1. The CONTRADICTION, as recorded

- **What `sitting_line.py` printed:** `K5-1-r1` (`a2f12f1`, `full calibrate`, 2026-11-05 09:32) gave
  "RESULT CONTRADICTION: a recorded wait on an IGNORED_EXPIRED row". The sitting stopped, as the
  pre-registered rule says.
- **It stays recorded in those words.** It is not deleted, and it is not reinterpreted in place.
- **The records** come from `K5-1-r1`'s `probe.json` and `logcat.txt`. Times are in seconds since
  boot, on `CLOCK_MONOTONIC`.
  - Page 0, row 335: a wait of 1369.5 ms, from 10161.888 to 10163.258. The row's outcome is
    `stored`.
  - Page 1, row 103: a wait of 1174.3 ms, from 10162.083 to 10163.257. The row's outcome is
    `ignored_expired`.
  - Page 0's history worker logged `ignored_expired` at 10161.480 and `stored` at 10163.255.
  - Page 1's history worker logged `ignored_expired` at 10163.256 and `unchanged` at 10163.269.
  - Only one of the two waits is on an `ignored_expired` row. That one row is enough for the rule to
    fire.

## 2. Why it is not a contradiction of the attribution

The owner's reasons, each checked against `K5-1-r1`'s records:
- **"The two waits end 1 ms apart (10163.257 and 10163.258 s)":** read, it holds.
- **"Page 0's stored line falls 3 ms before its wait ends":** 10163.255 against 10163.258. Read, it
  holds.
- **"Page 1's ignored_expired falls 1 ms after page 0's stored":** 10163.256 against 10163.255. Read, it
  holds.
- **"Page 1's expired row reached @Synchronized ingestPokemon and blocked there".**
  - The code holds this. In `a2f12f1`, `SmartCatchSpawnCoordinator.ingestPokemon` is `@Synchronized`
    (lines 229 to 230). #94's window gate returns inside it, at lines 244 to 253, before
    `resolvePokemon` at line 256.
  - The records fit it: page 1's worker finished its own work within 1 ms of page 0's.
  - The blocking itself is inferred. No stack was taken.
- **"It did not resolve the catalogue. #94's gate worked."** These records do not show that.
  - After page 0's parse succeeded, the catalogue is cached for the process (`PokemonTypeDex`, as the
    mechanism test of 2026-11-03 read it). A resolve by page 1 would then also take about 1 ms.
  - So the 1 ms cannot tell the gate's early return from a resolve against the cached catalogue. The
    gate also applies only to a name with a non-blank identity, and the probe records no names.
  - **What the records do show** is what the attribution needs: page 1's expired row did not cause
    the wait. Page 1 did not parse for a second; it waited for page 0's parse of an in-window spawn,
    which #94 leaves on the launch path by design.
  - That #94's gate returns before the resolution is shown by the code read above and by #94's tests,
    not by this launch.

## 3. The rule was mis-specified: the owner's error

- **The rule as the owner wrote it** says "a wait on an expired row" where it meant "an expired row
  that caused a wait". It is therefore mis-specified. This is the owner's error.
- **The reason it matters:** a wait is measured where it ends, on the row the main thread reaches
  next. A row can therefore carry a wait that another page's worker caused, as page 1's row 103 did
  here.
- **Recorded as the owner's error** at the owner's instruction.

## 4. The successor rule, pre-registered

- **The owner's words:** "a wait on an ignored_expired row is a CONTRADICTION unless, in the same
  interval, the other page's worker stored an in-window spawn AND the two waits co-terminate within
  20 ms; in that case it is recorded as the known live-spawn residual, with the evidence listed."
- **Read mechanically, as the agent operationalises it in `residual_check.py`:**
  - A wait runs from `at − gapNs` to `at`, each plus `anchorNs`.
  - "The other page's worker stored an in-window spawn in the same interval" means a
    `Spawn ingestion … outcome=stored` line. It must come from the app's process and from the other
    page's `historyTid`, and its time must fall inside the expired row's wait interval.
  - "The two waits co-terminate within 20 ms" means the other page has a recorded wait whose end is
    within 20 ms of the expired row's wait's end.
  - If both hold, the verdict is LIVE_SPAWN_RESIDUAL, with the evidence. If either fails, it is
    CONTRADICTION, which stops the sitting as before.
- **What it does not change.** A recorded wait still voids its launch as a late delivery, under
  `launchcheck.py`'s rule of 2026-11-03. The successor rule decides only whether the sitting STOPS.
- **Where the 20 ms comes from.** The owner grounds it in "the ten lock pairs of 2026-11-03, which
  co-terminated between −9.2 and +18.4 ms" (the plan, *The catalogue-parse mechanism, tested against
  the fifteen records*, clause b). It was not chosen on `K5-1-r1`'s numbers.
  - **The same clause records 13 lock pairs, not 10.** In the other 3 (`-3`, `-12`, `-14`) the ends
    were +177.8, +147.5 and +147.6 ms apart. Page 1's row was queued behind 65 to 75 of page 0's rows
    on the main thread.
  - So a live-spawn residual of that shape would be classed CONTRADICTION under 20 ms. The rule then
    errs toward stopping, never toward passing.
  - This is recorded for the owner. The tolerance is not changed.
- **`residual_check.py`,** sha256 `61b253c7d697f47ca9c91ce96b30aeb525dd2871220e29a0225318d1ce329568`,
  in the session scratchpad on `nbpromec1`. It was tested before this push on records other than
  `K5-1-r1`:
  - **`-13` of 2026-11-03:** CONTRADICTION on both expired rows. No `stored` line exists, and the two
    ends are 3.6 ms apart, as the plan's table gives.
  - **A copy of `-13` with one synthetic `stored` line** from page 0's worker inside page 1's wait:
    LIVE_SPAWN_RESIDUAL for page 1's row. Page 0's own expired row stays CONTRADICTION.
  - **`N5-plain-7`,** whose waits are on `unchanged` rows: no expired-row wait, so no verdict.

## 5. `K5-1-r1`, to be re-read under it after this push

- **The owner's instruction:** that it is re-read as the live-spawn residual, and that the 5d
  calibration it produced is therefore usable. `residual_check.py` is run on it only after this file
  is pushed.
- **Its calibration** was copied before the stop. `run.sh` copies `calibration.json` before the
  driver's checks run.
- **Its slot has used one of its three re-runs.** `K5-1` was interrupted, and `K5-1-r1` ran in its
  place.
- **As a launch it stays void,** as a late delivery: two recorded waits.
  - The calibration does not depend on the span. It is measured on the main thread after the window
    closes and after the dump is written.
  - That is why a calibration from a void launch can be used, while the launch's span cannot.

## 6. TASK 2's decision, read before computing

- **The owner's rule:** "If the probe-off ratios stay far below both thresholds, the overhead step
  CLOSES on the calibrations and the densities … If the probe's cost is large enough to move a ratio
  near a threshold, the probe-off launches RUN."
- **"Far" and "near" carry no number.** The agent reads them as follows, before computing:
  - If every probe-removed ratio is at or below its measured ratio, removing the probe brought no
    ratio closer to a threshold than measured. The measured margins then stand as the floor:
    0.7083 − 0.3537 for the span and 0.825 − 0.6288 for the burst end. That is read as "far below",
    and the step CLOSES.
  - If any probe-removed ratio is above its measured ratio, the agent reports the shift, and how much
    of the margin it uses. It does not decide "far" itself; that returns to the owner.
- **What is computed:** the probe's cost from the two calibrations and the ten launches' counts, as
  `overhead_report.py` defines it.
  - The control uses `Kc-1`: 266.8 ns a hook and 20.4 µs a message.
  - 5d uses `K5-1-r1`: 266.8 ns a hook and 47.1 µs a message.
  - For the burst end, the span's cost is removed, as the owner specifies. Beside it, the cost
    counted over the whole window from the anchor to each launch's burst end.
