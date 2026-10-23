# Device round — 5c, emote sessions owned by the binding

**Which plan to run.** This plan covers 5c, the chat timeline's emote sessions, and nothing else.
It does not cover deletion, alerts or anything else `manual-plan-5.5.2.md` tests: that is a separate
round for separate changes, and neither replaces the other. Both fold into the 5.6.0 plan when that
is anchored, which runs every case of both against the final 5.6.0 build.

The device round for 5c: the binding owns each chat row's emote session, a bind ends the session of
the row the view showed before, an unbind ends it when the row leaves, and a detach only pauses it.
It holds every device step 5c needs, in the order they must run: the harness stages that prove the
change, the manual checks that cover what the harness cannot reach, and the background measurement
that checks the battery fix of #69 still holds.

This file follows the shape of `manual-plan-5.5.2.md`: what the round applies to, what it needs,
the order, then each case as **Do**, **Proves it** and **Failing looks like**. A case whose "proves
it" evidence is absent has not passed.

Everything here is written to be run. What has already been measured is under *Recorded results* at
the end, with the date and the device; until a row there is filled in, its case has not been run.

## Which build each step applies to

| Step | Commit | Why that one |
|---|---|---|
| H1, H2 | `ffb3082` — *load emote images through a source the harness can drive (5c, stage 2)* | the image source and the instruments, with the ownership before 5c |
| H3 to H5, M1 to M6, B1 | the last commit on the branch that touches `app/`: `git log -1 --format=%h -- app/` | the ownership change, `b7300e4`, and anything in the code after it |

A build for H3 to H5 is that commit's when `git diff <that commit> HEAD -- app/ gradle/
build.gradle.kts settings.gradle.kts gradle.properties` prints nothing. If `ffb3082` is rewritten,
this table is updated in the same change. A build is matched to a
step by the label the app reports: the harness writes `commit` into every results file, and a
build from a tree with uncommitted changes reports the commit with a `+`. **A `+` disqualifies the
run.**

Build the dev flavour only, `:app:assembleDevDebug`, from a tree where `git status --porcelain`
prints nothing, and install the arm64 APK,
`app/build/outputs/apk/dev/debug/app-dev-arm64-v8a-debug.apk` (check the name after building:
the ABI split decides it).

## What the round needs

- An Android phone, identified at the start of the round, never assumed: `adb devices -l`, then
  `adb shell getprop ro.product.model`, `ro.build.version.sdk` and `ro.build.version.release`.
  Write all three down with the serial `adb devices -l` printed.
- The phone awake and unlocked, with the screen on, before each harness pass. Behind the lock
  screen the activity does not finish its comparison in time.
- No touch on the phone during a harness pass. Each pass is recorded with `adb shell getevent
  -lt`, and a pass with any `EV_` line is discarded and run again.
- For M1 to M6 and B1: two Twitch accounts signed in to the dev app, and a channel whose chat has
  animated emotes.

## The harness, as one function

The harness is the dev-only `RowViewParityHarnessActivity`. Only the adb shell can start it. It
writes `files/parity-harness/<theme>.json` in the app's own storage and logs one line under
`PARITY_HARNESS` when it is done. This function runs one pass and reads it back (Git Bash, from the
repository root):

```bash
ADB=adb   # or <sdk>/platform-tools/adb, <sdk> being the sdk.dir line of local.properties
PKG=com.fs.twitchminichat.dev
harness_pass() {  # harness_pass <out-dir> <theme: day|night> [extra am arguments...]
  out=$1 theme=$2; shift 2; mkdir -p "$out"
  "$ADB" shell "run-as $PKG sh -c 'rm -f files/parity-harness/$theme.json files/parity-harness/$theme-*.png'"
  "$ADB" logcat -c
  "$ADB" shell getevent -lt > "$out/getevent-$theme.txt" 2>&1 & ge=$!
  "$ADB" shell am start -W -n $PKG/com.fs.twitchminichat.harness.RowViewParityHarnessActivity \
      --es theme "$theme" "$@" > "$out/am-start-$theme.txt" 2>&1
  for _ in $(seq 1 180); do
    line=$("$ADB" logcat -d -s PARITY_HARNESS:* | grep -E "done theme=$theme |refused|aborted" | tail -1 | tr -d '\r')
    [ -n "$line" ] && break; sleep 1
  done
  kill $ge 2>/dev/null; wait $ge 2>/dev/null
  echo "$theme: ${line:-<no PARITY_HARNESS line in 180 s>} | input events: $(grep -c 'EV_' "$out/getevent-$theme.txt")"
  "$ADB" exec-out run-as $PKG cat "files/parity-harness/$theme.json" > "$out/$theme.json"
  case "$line" in *"done theme=$theme "*) ;; *) echo "$theme: NO RESULT, the pass failed"; return 1 ;; esac
  grep -q '"commit"' "$out/$theme.json" || { echo "$theme: NO RESULTS FILE, the pass failed"; return 1; }
}
```

**A pass that reports no result has failed, whatever an exit status says:** no done line, a
`refused` or `aborted` line, or no results file is a failure, never a pass that found nothing, and
`harness_pass` returns 1 for each. A stage with such a pass is run again or stopped, never counted.

Before the passes of a stage: `"$ADB" shell am force-stop $PKG`, `"$ADB" install -r <apk>`, then
read the installed APK's hash back with `"$ADB" shell sha256sum "$("$ADB" shell pm path $PKG |
tr -d '\r' | sed 's/package://')"` and compare it with `sha256sum <apk>`. They must be equal.

The extras: `--es passes measure` runs only the render-size measurement; `--es passes emote` runs
the measurement and the emote passes (the emote layer, its 404 pass, its controls and the lifecycle
proof); no extra runs everything. `--ef fontScale <f>` runs at that font scale instead of the
phone's: the activity restarts itself once with it, and aborts if it then runs at another.

## Order, and why

H1 first, because A18's classification decides what H4 predicts. H2 before H3, because the
instruments must be seen to fail on the ownership before 5c, at the points predicted below, before
the change that makes them pass exists on the phone. H4's predictions are written after H3, so the
list of steps is final, and before the first mutant is built. The manual checks and B1 come last,
on the final build.

---

## H. The harness

### H1 — What decides A18: the render-size measurement

A18 is the mutant that reads a message view's text size before the applier sets it. On a view's
first bind that is the size the theme gives a fresh view, which TextView rounds to whole pixels,
instead of 14 sp, which it does not round. Whether the two give different emote render sizes
depends on the density and the font scale, so it is measured here.

**Do.** Build `ffb3082`, install it, and run `harness_pass h1 day --es passes measure`, then the
same with `night`.

**Proves it.** Each results file has `renderSize.atThisConfiguration` and seven
`renderSize.fontScaleOverrides` (0.85, 1.0, 1.15, 1.3, 1.5, 1.8, 2.0), each with `density`,
`densityDpi`, `fontScale`, `freshTextSizePx`, `freshTextSizeUnit`, `fourteenSpPx`,
`boundTextSizePx`, `renderSizePxFresh`, `renderSizePxBound`, `urlScaleFresh`, `urlScaleBound` and
`renderSizesEqual`. Copy the phone's row and every override into *Recorded results*.

**Then classify A18 by this rule, decided before the measurement:**

1. `renderSizesEqual` false at the phone's own configuration: A18 is killable as things are. Its
   count is predicted in H4 at the phone's configuration.
2. True at the phone's configuration and false at one or more overrides: A18 is a real defect for
   users at those font scales. Take the first override in the order above whose sizes differ, run
   the emote passes there in both themes in H4 (`--es passes emote --ef fontScale <f>`), and
   predict A18's count at that configuration. The emote passes only, never the 658 pairs.
3. True everywhere measured: A18 is equivalent by construction at every configuration measured.
   It is not built. Its record gives the reason — the theme's default text size and 14 sp give the
   same truncated render size at each of those configurations — and points at the values recorded
   below, which are what pin it.

**Failing looks like.** A file without `renderSize`, or with any of those fields missing: the
measurement did not run, and A18 cannot be classified. A `freshTextSizeUnit` other than `sp` is
recorded, not a failure: the rule reads the sizes, not the unit.

### H2 — Stage 2: the frozen layer's calibration, and the instruments failing where predicted

**Do.** With `ffb3082` installed and checked, run `harness_pass h2 day` and `harness_pass h2
night`.

**Proves it.** In each theme, every one of these, exactly:

| Field | Value | What it shows |
|---|---|---|
| `rowsIdentical` / `rowsTotal` | 34 / 34 | text parity, no loader |
| `parityPageHasNoEmoteLoader` | true | the parity pass ran without images, as before |
| `controls` | `oneCharacterApart_pixelsDiffer`, `focusableCleared_stateDiffers`, `focusableCleared_pixelsIdentical`, `productionTwice_pixelsIdentical`, `productionTwice_stateIdentical` all true | the parity comparison can fail |
| `emoteLayer.rowsIdentical` / `rowsTotal` | 2 / 2 | **the calibration of `FrozenEmoteLayer4804609`** |
| `emoteLayerWith404.rowsIdentical` / `rowsTotal` | 2 / 2 | the same with Kappa's animated version answering 404 |
| `emoteLayerControls.behaved` | true | images shown differ from none, two ids differ, a row built twice matches, a size one pixel off differs in pixels, state and request |
| `rebindProof.pairs.vacuity.pairs` | 658 | 24 × 24 + 9 × 9 + 1 × 1 |
| `rebindProof.pairs.identical` | 658 | pixels and state |
| `rebindProof.pairs.sessionsHeld` | 610 | the 48 `sessionFailures` are exactly the pairs whose A is `msg.emotes` or `msg.emotes.reply.mention` |
| `rebindProof.pairs.imagesHeld` | 658 | |
| `rebindProof.createVersusProducer.identical` | 34 of 34, 1 declared exception applied | |
| `rebindProof.preconditions` | `runnable` 3, `fired` 3, `notRun` [`unbindOfAViewCreateNeverBuilds`] | |
| `emoteLifecycle.passed` | 3: L1 for each emote row, and L9 | |
| `emoteLifecycle.failed` | 8: L2, L3 and L6 for each emote row, L7, L8 | |
| `emoteLifecycle.notRun` | 4: L4 and L5 for each emote row | no unbind on this tree |

The 2 / 2 of the emote layer is not "the pass works". It is what licenses the new frozen file: it
says that on the commit that adds the layer, before the ownership changes, the production loader
driven by the fixed source builds views identical to the frozen layer's, in pixels, in state and in
the requests made. That makes the layer a faithful transcription of 4804609's, and from then on it
never changes. Read
its blob with `git rev-parse <commit>:app/src/dev/java/com/fs/twitchminichat/harness/FrozenEmoteLayer4804609.kt`
at every later stage, as for `FrozenRowViews4804609.kt`: `213215ed8fdb19961d80679f416e2a3e8e1bd6ce`
and `2ee0bdc98ec16aac0f3ee7cf6bcb4e54be6bcd91`.

**Failing looks like.** Any count other than the ones above. **A different count stops the round:
nothing is adjusted to fit.** An emote layer that differs here means the frozen layer is not yet a
faithful transcription; it may be corrected only now, before it is calibrated, and the stage run
again.

### H3 — Stage 3: the ownership change

**Do.** Build the commit the table names for H3, install and check it, run both themes.

**Proves it.** In each theme: text parity 34 / 34; the emote layer 2 / 2 and with a 404 2 / 2; its
controls behaved; the rebind proof `passed` true with 658 pairs identical, 658 `sessionsHeld` and
658 `imagesHeld`; C-R 34 / 34; preconditions `made` 4, `fired` 4; `emoteLifecycle.allPassed` true,
16 of 16. Both frozen blobs as in H2.

The proof had 15 steps until L10 was added after the first round of mutants: an echo with emotes
confirmed in place, the path every message sent with an emote takes, which nothing covered before.
See H4 for its separate pre-registration.

**When H1 classified A18 by its second rule,** run the emote passes at the font scale H1 chose too
(`--es passes emote --ef fontScale <f>`), both themes: the baseline A18's count is measured against.
It must report that font scale and the render sizes H1 measured there, the emote layer 2 / 2 at the
bound size, with a 404 2 / 2, its controls behaved, and the lifecycle proof 15 of 15.

**Failing looks like.** Any other count. A step of the lifecycle proof that fails names its
precondition or check in the results file; read that before anything else.

### H4 — Predictions, then mutants

**Do.** After H3, write `predictions.json`: for each mutant its exact edit, its predicted count for
every instrument in each theme — the emote layer, the emote layer with a 404, the pairs' pixels,
state, session and image checks, C-R, text parity, and each lifecycle step — the exact pairs and
steps, and the reasoning. Record its sha256 and the time before building the first mutant. Then,
for each mutant alone: apply its edit to that commit, build, install, run both themes (A18 as H1
decided), restore the files from git, and check `git status --porcelain` prints nothing.

| Mutant | Edit |
|---|---|
| A17 | `view.text = text` after `page.bindEmotes(...)` in `ChatRowViewBinder.bindMessageText` |
| A18 | `val textSizePx = view.textSize` moved above `applier.apply(view, row)` — only as H1 decided |
| O1 | `sessions.release(textView)` removed from `TwitchEmoteImageLoader.bind` |
| O2 | the detach callback posts the pre-5c release again |
| O3 | `rowViews.unbind(...)` removed from `removeAt` and `removeAll` in `ContainerTimelineViews` |
| O4 | `applyAnimationState()` removed from the detach callback |
| O5 | `unbind(view)` added to `ChatRowViewBinder.rebind`, after the tag moves: a status rebind ends the echo's emote session |

**O5 and L10 have a pre-registration of their own,** written after the first six had run and before
L10 was written or O5 built. `predictions.json` stays the record of what was predicted for the first
six, on a lifecycle proof of 15 steps; the second file adds L10 and O5 and does not amend the first.
Read them as two files.

**Proves it.** Every count equals its prediction.

**Failing looks like.** One count that differs. The round stops there, and the predictions file is
never edited after its hash was taken.

### H5 — The clean final build, once more

**Do.** Reinstall the clean final APK and run both themes again.

**Proves it.** The same counts as H3. This is what is on the phone afterwards.

---

## M. Manual checks on the final build

The harness never runs Glide: its loader draws from the fixed source. These checks are the only
cover for `GlideEmoteImageSource`.

Before them: `"$ADB" shell setprop log.tag.Engine VERBOSE`, then force-stop the app and open it
again. Glide reads that property once, when its Engine class loads (Engine.java:35, Glide 4.16.0), so
it must be set before the process starts. Every image Glide starts loading is then a line `Engine:
Started new load in …, key: EngineKey{model=…}`: an emote's own image has a model starting with
`https://static-cdn.jtvnw.net/emoticons/`, and each frame a running GIF decodes has a
`GifDecoder` model.

### M1 — Emotes in live chat and in a reply

**Do.** Open a channel whose chat sends animated emotes. Send a reply that contains an emote.

**Proves it.** Animated emotes play in the rows on screen; static ones show; the reply's emote shows.

**Failing looks like.** An invisible marker where an emote belongs, or an emote that never plays.

### M2 — Swiping between the two accounts and back

**Do.** With animated emotes on screen, swipe to the other account's page, wait 5 seconds, swipe
back.

**Proves it.** The emotes are there at once and play again. Record whether `Started new load`
lines with an `emoticons` model appear while swiping back. None is expected while the page's view is
kept, since a detach keeps a row's images; whether ViewPager2 keeps that page's view has not been
read, so such lines are recorded, not a failure by themselves.

**Failing looks like.** Emotes missing or blank after the swipe back.

### M3 — Home for ten seconds, then back

**Do.** Home, wait 10 seconds, reopen the app.

**Proves it.** The emotes show and play again.

### M4 — Rotation

**Do.** Rotate the phone with emotes on screen, then back.

**Proves it.** The emotes show after each rotation and the app does not crash.

### M5 — A channel switch

**Do.** Switch the page to another channel, then back.

**Proves it.** The old channel's rows are gone with their emotes; the new channel's emotes load.

**Failing looks like.** A crash with `An emote session was released twice` or `A load was started
for a released emote session` in the log: an ownership invariant broke.

### M6 — An echo with emotes, confirmed

On a live send the echo is never replaced. It is shown sending, with a status line and at reduced
opacity; Twitch's USERSTATE confirms it, and the app moves it to CONFIRMED in place
(`ChatFragment.kt:2515-2529`): no status line, full opacity (`PendingEchoStatus.kt:44`). The row
that shows the message from then on is the echo itself. It is replaced only when canonical live or
history data for it arrives (`ChatFragment.kt:2532-2535`), a separate path that logs
`TWITCH_IRC: outgoing message reconciled`. That Twitch does not send the sender's own message back
on the same connection, which is why a live send takes the first path, is documented behaviour; it
was not measured here.

**Do.** Send one message with an emote picked from the dev app's emote picker, and watch it.

**Proves it.** The row that appears shows the emote, and the log shows
`TWITCH_IRC: outgoing message confirmed` for the send: the echo confirmed in place, with its emote
drawn through Glide.

**Failing looks like.** A marker without its image in the confirmed row, or the image dropping out
when the status changes. L10 checks the second with the fixed source.

---

## B. The background measurement

### B1 — No emote work after Home

#69 stopped animated emotes from decoding while the app is in the background. 5c changes when
sessions end, so this checks once, on the final build, that nothing decodes after Home.

**The method,** as the record of #69's measurement gives it. That record is kept outside this
repository; this section is its only copy here.

- two accounts, on a channel with animated emotes;
- 20 seconds in the foreground, then Home, then 60 seconds with the screen on;
- the load count from Logcat with `log.tag.Engine` at `VERBOSE`, and the CPU time of each thread;
- on ColorOS the freezer stops the process about 30 seconds after Home, so the useful window is the
  first 30 seconds of the 60.

**The parameters #69's record does not hold,** and this measurement fixes for itself, so that the
next one can repeat it:

| Parameter | This measurement |
|---|---|
| Channel | named in *Recorded results* when run |
| Whose lines count | only the dev app's: its pid, read with `adb shell pidof com.fs.twitchminichat.dev` at the start. `log.tag.Engine` is a property of the whole phone, so every app that uses Glide logs once it is set, and the first run counted another app's lines |
| Animated emotes on screen at Home | the positive control: GIF frame loads from that pid in the 20 seconds before Home, at least 100 |
| How Home is pressed | by adb, never by hand: `adb shell 'log -t TMC_ROUND home; input keyevent KEYCODE_HOME'`. The `TMC_ROUND` line puts the moment of Home in the same log as the measurement, on the same clock, and no person adds stray input |
| What counts as a load | every `Engine` line `Started new load` from that pid, whatever its model: emote images and GIF frames both |
| How CPU is read | `adb shell 'cat /proc/<pid>/task/*/stat'`, one second after Home and 30 seconds after it; for each thread, fields 14 and 15, counting the thread's name in parentheses as field 2 (user and system time in clock ticks; `adb shell getconf CLK_TCK` per second). If the shell may not read them, the same through `run-as com.fs.twitchminichat.dev` |
| Log timestamps | `adb logcat -v epoch -s Engine:V TMC_ROUND:V AndroidRuntime:E`, streamed to a file for the whole run. The phone's 256 KiB log buffer holds only about a minute and a half of GIF frame lines, so a `logcat -d` at the end can lose Home |
| `log.tag.Engine` | not persistent: set at 14:47 on 2026-10-21 and found empty on 2026-10-23. Set it, and read it back, at the start of every round, before the app process starts |
| Screen on | read, not changed: `settings get global stay_on_while_plugged_in` (7 on the test phone, all power sources) with `dumpsys battery` showing USB power keeps the screen on for the 60 s, although the phone's own timeout is 15 s |

**Do.** Set the property, read it back, and restart the app as under *M*. Open both accounts on the
channel, leave the app in the foreground for 20 seconds, press Home by the command above, keep the
screen on for 60 seconds, with the log streamed and the two CPU readings taken. Afterwards,
`adb shell setprop log.tag.Engine ""` puts the property back.

**The rule, decided before the measurement and corrected once, as below.** The run is **void** - not
healthy - when the positive control has fewer than 100 frame loads, when the dev app's pid changes
during the run, when the Home marker is missing, or when any physical input event is recorded.
Otherwise, count the dev app's `Started new load` lines timestamped more than 1.0 seconds after Home
and less than 30 seconds after it:

- **10 or fewer: healthy.** Nothing decodes in the background.
- **100 or more: the defect is back.** Before #69 the same window held thousands.
- **Between 11 and 99: inconclusive.** Stop and report the count; the measurement is not repeated
  until the reason is understood.

**Why the positive control is 100:** ten times the healthy ceiling of 10, so that a count of 10 or
fewer after Home is a real drop - at least tenfold in count, over a longer window, 29 s against
20 s - and never a small number beside a small number.

**The correction, kept written.** The rule as first written counted `Started new load` lines without
saying whose. Pre-registered on an ambiguous quantity, it was not really pre-registered, and neither
its writer nor its reviewer saw that until a second Glide app on the phone made it visible: the first
run counted 585 lines, all another app's, while the dev app logged none at all. That run is void, not
read by the rule; see *Recorded results*.

**Context, not a target.** The same record gives 4,073 loads and 29.9 s of CPU in 63 s before
#69, and with #69, 21 loads, all within 0.9 s of Home, and 1.5 s of CPU. Its channel, the rows on screen,
how Home was pressed, which line counted as a load, how per-thread CPU was read, and whose process
the counted lines came from are not recorded - so whether another Glide app was running then is not
known either. So this measurement is compared with those figures only for their order of magnitude,
never numerically, and its result is not presented as reproducing them.

---

## What this round cannot check, and why

- **That a RecyclerView takes the cache path in this app.** There is no RecyclerView before 5d. The
  lifecycle proof's L2 and L3 do what its cache does, detach and reattach without a bind, but not
  through it.
- **That 5d calls unbind from onViewRecycled.** 5d does not exist yet.
- **Glide's own ordering**, and a real GIF resuming on its frame: the fixed drawables have no
  frames. M2 is the only look at it.
- **The startup time.** It is measured for 5a-2, 5c and 5d together, after 5d.

## Recorded results

Fill in each row when its case runs; add a run rather than overwriting one.

**The device, read on 2026-10-21 at the start of the round:** `adb devices -l` gave serial
`57ebfa06`, model `AC2003`; `getprop` gave OnePlus, Android 12, API 31, build `AC2003_11_F.24`;
`wm density` 382 (physical 450), `wm size` 1080 × 2400, system font scale 1.15. Its dev app before
the round was `c1380af`, sha256 `c4cc57f8…`, the build #86 left on it.

**The configuration A18 was established at** (H1, 2026-10-21 14:01, `ffb3082`; day and night gave
the same values; density 2.3875, 382 dpi, in every row):

| Font scale | Fresh text size, unit | 14 sp in px | Render size fresh / bound | URL scale fresh / bound | Equal |
|---|---|---|---|---|---|
| 1.15, the phone's own | 38.0, sp | 38.4388 | 57 / 57 | 3.0 / 3.0 | yes |
| 0.85 | 28.0, sp | 28.4113 | 42 / 42 | 2.0 / 2.0 | yes |
| 1.0 | 33.0, sp | 33.4250 | 49 / 50 | 2.0 / 2.0 | **no** |
| 1.15 | 38.0, sp | 38.4388 | 57 / 57 | 3.0 / 3.0 | yes |
| 1.3 | 43.0, sp | 43.4525 | 64 / 65 | 3.0 / 3.0 | **no** |
| 1.5 | 50.0, sp | 50.1375 | 75 / 75 | 3.0 / 3.0 | yes |
| 1.8 | 60.0, sp | 60.1650 | 90 / 90 | 3.0 / 3.0 | yes |
| 2.0 | 67.0, sp | 66.8500 | 100 / 100 | 3.0 / 3.0 | yes |

**A18's classification:** the second rule. Equal at the phone's own settings, unequal at 1.0 and
1.3, so a real defect for users at those font scales at this density. Registered at 1.0, the first
unequal override in H1's order, with the emote passes only.

**Harness stages,** every pass with 0 input events unless the row says otherwise:

| Stage | Commit | Dev APK sha256 | Day and night, each | Time |
|---|---|---|---|---|
| H1 | `ffb3082` | `2b25e263…` | the table above | 14:01. A first run was discarded: its night pass recorded 11 input events, two touches |
| H2 | `ffb3082` | `2b25e263…` | parity 34 / 34; emote layer 2 / 2, with a 404 2 / 2, **the frozen layer's calibration**; pairs 658 identical, `sessionsHeld` 610 (the 48 pairs whose A is an emote row), `imagesHeld` 658; C-R 34 / 34; preconditions 3 of 3, unbind not run; lifecycle passed L1 × 2 and L9, failed L2, L3, L6 × 2 and L7, L8, not run L4, L5 × 2. Every count as predicted | 14:02 |
| H3 | `9b1b86a` | `32e40e16…` | every count as predicted; superseded, because the next row's fix moved the code | 14:04 |
| H3 at font scale 1.0, first attempt | `9b1b86a` | `32e40e16…` | no result: the activity never restarted at the font scale, stayed resumed with an empty content frame for 240 s, and logged nothing. Its night pass recorded 374 input events. Fixed by `ef95c63` | 14:04–14:18 |
| H3 at font scale 1.0 | `ef95c63` | `37e2b38d…` | font scale 1; render sizes 49 / 50; emote layer 2 / 2 at 50 px, with a 404 2 / 2; controls behaved; lifecycle 15 / 15. As predicted | 14:21 |
| H3 | `ef95c63` | `37e2b38d…` | parity 34 / 34; emote layer 2 / 2, with a 404 2 / 2; pairs 658 / 658, `sessionsHeld` 658, `imagesHeld` 658; C-R 34 / 34; preconditions 4 / 4; lifecycle 15 / 15. As predicted | 14:22 |
| H5 | `ef95c63` | `37e2b38d…` | the same as H3 | 14:36 |
| H3 with L10, 2026-10-23 | `49e2525` | `339ae2c2…` | every count as in H3, and the lifecycle proof 16 / 16. As pre-registered | 08:58 |
| H5, 2026-10-23 | `49e2525` | `339ae2c2…` | the same as the row above. The build left on the phone | 09:01 |

Both frozen blobs were read at `ffb3082`, `9b1b86a` and `ef95c63`: `FrozenRowViews4804609.kt`
`2ee0bdc98ec16aac0f3ee7cf6bcb4e54be6bcd91` and `FrozenEmoteLayer4804609.kt`
`213215ed8fdb19961d80679f416e2a3e8e1bd6ce` at all three.

**H4:** `predictions.json`, sha256 `611fb79ea332d64ff6b4ee49f93a36c7ca513731512a59c0eb01f09b7371f54e`,
written 2026-10-21 14:25:49 +02:00, before the first mutant was built, and unchanged after the last.
Each mutant was applied alone to `ef95c63`, built, run in both themes, and restored from git with the
tree checked clean. Every count of every mutant equalled its prediction:

| Mutant | Dev APK sha256 | Day and night, each |
|---|---|---|
| A17 | `384e4ff0…` | emote layer 0 / 2, with a 404 0 / 2; `imagesHeld` 610, the 48 pairs whose B is an emote row; lifecycle failed L1, L3, L5 × 2; parity, pixel and state pairs, sessions and C-R untouched |
| A18, at font scale 1.0, emote passes only | `d9a4c762…` | emote layer 0 / 2, with a 404 0 / 2, each row seen in pixels (one pixel shorter), in state (bounds 49 against 50) and in the request's size, not in its URL; lifecycle failed L1, L3 × 2 |
| O1 | `abe29c81…` | `sessionsHeld` 610, the 48 pairs whose A is an emote row; lifecycle failed L6 × 2; nothing else |
| O2 | `d3129d55…` | lifecycle failed L2, L3, L4 × 2; nothing else |
| O3 | `c3d91847…` | lifecycle failed L7, L8; nothing else |
| O4 | `cfe3bc96…` | lifecycle failed L2 × 2; nothing else |

**L10 and O5:** their pre-registration, sha256
`da8391ad780e03349519596c4e16fdc63571f4bddf34b6fe95e1aef2f7dae816`, written 2026-10-23 08:55:02
+02:00, before L10 was written and O5 built. L10 passed on `49e2525` in both themes. O5, applied
alone to `49e2525` (dev APK `cf6d4928…`), failed L10 and nothing else, in both themes, on the
predicted checks: its loads cleared, its images gone, its animations stopped and its release
reported, at the call and after the loop turn, and its pixels. The status change itself
(`statusChanged`, `confirmedLook`) still passed. As predicted.

No production or build file differs between `ef95c63`, the build B1, M2 and M6 ran on, and
`49e2525`: `git diff ef95c63 49e2525 -- app/src/main gradle build.gradle.kts settings.gradle.kts
gradle.properties app/build.gradle.kts` prints nothing.

**M1 to M5,** on `ef95c63`, by Federico, on 2026-10-21: passed. The app's Glide lines in that
afternoon's recording span 15:23 to 15:48, which dates the session; its pid changed once, at 15:37.
The recording, Glide's `Engine` lines and crashes only, held no crash line. M2's swipe-back time was
not noted, so M2 was run again by adb, below.

**M2 by adb,** 2026-10-23 08:16, `ef95c63`, 0 physical input events. A leftward swipe across the
status row (`adb shell input swipe 900 455 180 455 300`, above the message rows, where it cannot
start a swipe-to-reply), five seconds on the other account's page, a rightward swipe back. Read
from the app's own view tree by identity, ids and positions only:

- before: the first account's page current, `rootChat 9a931af`, `scrollChat d01ce3e`,
  `chatContainer 88fe49f`;
- away: the second account's page current; the first page one place to the left, the same three
  identities;
- back: the first page current again, **the same three identities**. Its view, and its rows with
  their images, were kept. The expectation - the pager keeps one page each side
  (`MainActivity.kt:88`), so nothing reloads - held.

The log half of that run is void: every `Engine` line in it came from another app (see B1). And one
idea is retracted with its reason: comparing GIF decoder identities before and after could not have
answered whether the page kept its images, because Glide hands every view showing the same GIF at
the same size a new drawable on the same decoder.

**B1, first run, void:** 2026-10-23 08:16, `ef95c63`, Home by adb, 0 physical input events. The rule
then in force read 585 `Started new load` lines 1.0-30 s after Home. All 2,074 `Engine` lines of the
run came from pid 15614, `com.fs.twitchminichat`, the published stable app, running in the background
since 08:09; the dev app, pid 18481, wrote none, its 20 s in the foreground included. The run did not
measure its subject, so it is void, not read by the rule.

**B1's negative control,** from that same run: the stable app, `com.fs.twitchminichat` 5.5.1 (8),
pid 15614, decoded GIF frames throughout: 585 `Started new load` lines 1.0-30 s after the dev app's
Home, 2,074 in the 98 s window, from the same 3 GIF decoders, in the background. That build does not
contain #69, read rather than recalled: its APK on the phone hashes to `bf6c22c2…`, and its code
carries `007ee55`, both read on 2026-10-23; `git merge-base --is-ancestor 692a7dc v5.5.1` exits 1;
`EmoteAnimationGate.kt`, which #69 added, is absent from `v5.5.1`'s tree. So the rule's upper band
has fired once on a build that has the defect, which is evidence it can fire at all.

**B1, run again under the corrected rule:** written first, sha256 `a053a535…`, 08:34:17.

| Date | Device | Build | Channel | Positive control (frame loads, 20 s before Home) | Loads 1–30 s after Home | CPU 1–30 s after Home | Verdict |
|---|---|---|---|---|---|---|---|
| 2026-10-23 08:35 | AC2003, pid 18481 throughout, 0 input events, screen awake at the end | `ef95c63` | `deemonrider`, as Federico wrote it, on both accounts' pages. Reported, not read: no log line held a channel name | 642 | **0**; 18 lines in the first 0.6 s after Home, none after | 0.01 s over 48 threads | **healthy** |

Before it, the instrument was qualified: 407 frame loads from pid 18481 in 12 s, with animated emotes
on its screen.

**M6,** 2026-10-23, `ef95c63`, on the live path the corrected M6 above describes. **Its mechanism is
covered, by the log and an eye together:** the log shows the echo confirmed in place, and Federico
watched the send and saw the emote he had picked appear in that row, drawn through Glide.

- *What the eye settles.* Federico watched this send - he identified it as the most recent event in
  the log, and the confirmation below is the only and the last one the recovered log holds, up to its
  end at 7944.029 s - and the emote that appeared was the one he picked.
- *What the log settles.* `TWITCH_IRC: outgoing message confirmed` at 7899.585 s since boot, from the
  dev app's pid: an echo existed and Twitch's USERSTATE moved it to CONFIRMED in place. No
  `reconciled` line follows in the 44 s the log still holds: nothing replaced it.
- *What the log could not attribute.* In the 3.2 s before the confirmation the dev app looked up one
  emote image twice, `emotesv2_20aeab0c…/static/dark/3.0`, at -262 ms and -8 ms, both memory hits
  (`Loaded resource from active resources`). Both accounts' pages show the same channel, so every
  incoming message is bound on both and its lookups come in pairs, usually 2-10 ms apart, at 2 to 18
  lookups every 5 s that minute. The pair could be the echo on the sending page and the message
  reaching the other page, or another user's message on both. It is recorded as a reading that could
  not attribute.
- *What was lost.* The touch record, which held the moment of the tap on send, was deleted on the
  phone by the same command whose transfer of it had failed. The log above comes from the phone's own
  buffer, read afterwards.
- *Not covered, on purpose.* Whether an emote is drawn in the brief sending state before the
  confirmation: the event lasts as long as the echo is sending, and every outcome of observing it
  leads to the same action, so no instrument was built for it.
- *L10 is a different question.* It covers the ownership half - that the status rebind leaves the
  session, its images and its animations alone - with the fixed source, not Glide.
- *The first attempt, on 2026-10-21, was the same evidence under the wrong premise.* Federico reported
  then that the message appeared straight away with its emotes and no echo was seen. On the live path
  that row was the echo, confirmed in place; it was read as a miss because the plan said a confirmed
  message would replace it.
- *Corrected along the way.* It was said during the round that the echo is removed when Twitch's copy
  arrives. That is false for a live send: the echo is confirmed in place and stays the row.
