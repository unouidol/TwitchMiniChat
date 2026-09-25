# Release state

What users actually have, what is waiting to reach them, and what the test device is
running. `AGENTS.md` describes *how* to release; this file records *where things are*.

It exists because that question kept being re-derived from scratch, and answered wrongly
once from a stale note, costing more time than keeping it written down.

**Every release updates this file in the same pull request that bumps the version.**
A change to the code that reaches `main-v5` without appearing under "Waiting for release"
is a change nobody can account for later. Documentation commits are read from the log
instead, for the reason given in that section.

Last verified: 2026-09-25, against `origin/main-v5` at `f785008`, with `v5.5.1` published.

## Published — what users have

| | |
|---|---|
| Version | **5.5.1** (`versionCode 8`) |
| Tag | `v5.5.1`, annotated, pointing at commit `007ee55` |
| Published | 2026-09-25 |
| Assets | `TwitchMiniChat-Android-arm64-v8a.apk`, `TwitchMiniChat-Android-armeabi-v7a.apk`, `SHA256SUMS.txt` |
| Signer | `ST=Italy, L=Italy, CN=Unouidol`, SHA-256 `aca1170e9dcac79abfa6db078a7a9903ab1f6a935745033c0752177d22e237ed` |
| arm64-v8a | `bf6c22c21f4f5d0b2a49cb2e5406659141cf0e87eb917183db692af67965aaf8` |
| armeabi-v7a | `7833fe2c3905f6af62cd32ad2fc9d9633e7ccc20deb6725c8c41ef359d58bf4e` |

The published APKs were built from `007ee55` with a clean tree, so they report
`GIT_SHA 007ee55` with no `+`: the login screen reads `Version 5.5.1 (build 8, 007ee55)`
and the journal export header `# app 5.5.1 (8) 007ee55`. Checked after publication by
downloading both `releases/latest/download/…` URLs and hashing them; both matched the
checksums above.

Previously published: **5.5.0** (`versionCode 7`), tag `v5.5.0` at `f0e0d35`, 2026-08-31.

Asset names, tag shape and checksum file match the contract in `AGENTS.md`, so the public
download buttons on `https://tmc.ircminichat.party/` resolve.

`f0e0d35` is the merge commit of pull request #14. An earlier version of this file recorded
that tag as sitting at `8478d82`, which is the last commit *of its release branch* and the
parent on the other side of that merge. Both are on `main-v5`, so the mistake was invisible,
but the tag has always pointed at `f0e0d35`. Dereference a tag rather than reading a branch
head: `git rev-parse v5.5.1^{commit}` gives `007ee55…`.

**Three sets of 5.5.1 artifacts are void and must never be published**, and none of them is
what `v5.5.1` carries. Built from `cfc4591` on 2026-09-17, they play the fallback on a watch
that saw nothing: arm64 `fb2c9d59…652feaa6`, armeabi-v7a `dfb511c6…305ac00b79`. Built from
`7073284` on 2026-09-18, they still let the decision inside the sampling loop play on two
readings: arm64 `cb738466…f269b09d`, armeabi-v7a `fbb9462a…93d5fc4d`. Built from `e15c3e9`
on 2026-09-18, they carry no build identity: arm64
`d91d8dc8173f554a73f0149a3df3d285feaf4280b9567ac7f84bdc1cd8b3cda5`, armeabi-v7a
`12037cf946eb99f39bf2dbbd2cd1de4d07abac015ad026ea02f56c890fd11979`. The checksums are here
to recognise a copy if one survives.

## Waiting for release — on `main-v5`, not published

Everything merged since the `v5.5.1` tag. None of this has reached users. Read it with
`git log --first-parent v5.5.1..main-v5` rather than from a list copied here: a row added
before its own merge has to name a pull request, and then be replaced by the commit the
merge produces, which is a second edit this file kept needing.

**Everything after the tag is documentation only.** The moment a change touches code, it
gets a row here with its commit, and this sentence stops being true.

## What 5.5.1 shipped

Everything between the `v5.5.0` and `v5.5.1` tags, kept here because the reasoning behind
these changes is what the next question about them will need. List it with
`git log --first-parent v5.5.0..v5.5.1`.

| Commit | Date | Change |
|---|---|---|
| `a67cd37` | 08-31 | Document the release and distribution contract (docs only) |
| `f8ea482` | 09-01 | Encrypt locally stored Twitch credentials at rest |
| `a77b129` | 09-01 | Add "delete only this device" to Safety & Privacy — completes the app side of `/delete_device_data` |
| `7cf64f8` | 09-02 | Opt-out crash reporting; terms version raised to 2, so existing installs see the acceptance gate again |
| `71f9850` | 09-02 | Report the silent account storage failures |
| `502c11e` | 09-02 | Report the features that stop working without saying so |
| `f62d0bd` | 09-11 | Add `CLAUDE.md` importing `AGENTS.md`, so the repository rules load (docs only) |
| `d342e96` | 09-11 | Stop a failed read from erasing the stored accounts |
| `70c1d3c` | 09-11 | Verify the name on the certificate before sending the Twitch token |
| `9c14c72` | 09-11 | Create this file on `main-v5` (docs only) |
| `7083a14` | 09-11 | Say when the notification listener can be moved (docs only) |
| `0dfbc73` | 09-11 | Stop counting lint warnings that depend on the network |
| `94cee98` | 09-11 | Write down the six rules the month showed were missing (docs only) |
| `4439682` | 09-11 | Drop the `-v26` qualifier the minimum SDK already guarantees |
| `e191a09` | 09-11 | Make a new lint warning fail the build, on a baseline both flavours share |
| `21e0184` | 09-14 | The diagnostics journal, and a hidden gesture that gets it off the device (#30) |
| `2a22fed` | 09-14 | Replace the promised identifier for #30 in this file (docs only, #32) |
| `c4f4ffb` | 09-14 | Play the alert sound when the system never does (#31) |
| `17918a8` | 09-15 | Merge of `work/history-diagnostics-observability`: seven fixes, the diagnostics behind the journal, the notification listener removed before it (#34) |
| `5b1bd57` | 09-15 | Every local reset also erases the diagnostics journal (#35) |
| `a1f4d7e` | 09-15 | The privacy policy describes the local diagnostic record (#37) |
| `6055751` | 09-15 | The keep-accounts reset keeps the device credential, so the server registration stays reachable (#40) |
| `86566d7` | 09-15 | "Erase everything on this device" also clears the built-in browser data (#40) |
| `000318a` | 09-15 | Keep the two copies of every policy page aligned (docs only, #38) |
| `cf8832d` | 09-15 | The data deletion page, rewritten from the code. Corrected in 5.5.1: the published copy had drifted 35 lines from the APK's, a May section never brought back into the app, which is why the two-copies rule exists (#39) |
| `430e2e6` | 09-16 | The acceptance gate also reaches someone who updates straight into the chat, checked on resume (#41) |
| `cfc4591` | 09-17 | `versionCode 8`, `versionName 5.5.1`, and this file (#36) |
| `7073284` | 09-18 | The fallback no longer plays when its watch saw nothing, and reads the four settings again just before playing (#42) |
| `e15c3e9` | 09-18 | Every fallback playback passes one gate - enough readings, then the four settings read again - the decision inside the sampling loop included (#43) |
| `007ee55` | 09-18 | Every build carries its commit: `BuildConfig.GIT_SHA`, shown on the login screen and in the journal export header (#44) |

`d342e96` and `70c1d3c` are the two that most deserve a release: one prevents permanent
loss of every stored account after a single transient Keystore failure, the other stops the
Twitch token being handed to whoever can redirect the connection.

A row added before its own merge names the pull request, and is replaced by the commit once
the merge produces one: writing a plausible identifier for a commit that does not exist yet
would be an invention rather than a record.

### What the merge of the diagnostics branch brought

`17918a8` is a real merge commit, not a squash: the branch carried commits with distinct
objectives. Its second parent is `901ecdc`, and its tree is identical to that commit, which
continuous integration had verified on the pull request.

**Seven fixes that change what users experience**

| Commit | Change |
|---|---|
| `7a57893` | Recover the window an off-screen chat page cannot measure |
| `570580e` | Recover the offline window for a page that never resumes |
| `b7535a3` | Recover history on a mid-session IRC reconnect with no `onStop` |
| `ae141be` | Ask again for an hour the network refused to deliver |
| `bb319d4` | Retire the frozen notification channels and delete the ones earlier versions left behind — **per-channel customisations are reset once** |
| `1cad63c` | Stop alerting for spawns whose window has already closed |
| `39a4763` | Keep the account and channel out of Logcat |

Seven, not eight. `5e352df`, a two-second settling delay for alerts from a freshly started
process, is in this history but withdrawn by `99c730a`. The fallback from #31 repairs the
silence it guarded against only when that silence actually happens, and removing the delay
turns every cold alert into a trial: `processUptimeMs` is written beside the outcome on the
`fcm.notification.alert_audio` line.

**Nine commits of diagnostics**, all writing to the journal: `73521ef`, `f16aab1`, `a4c124e`,
`5ca53dc`, `af74bfb`, `5616371`, `b7b43ea`, `377d91c`, `a01f99a`. Two of them, `b7b43ea` and
`377d91c`, concern `NotificationAlertProbeService`, which `8350d2b` (#33) removed together
with its `BIND_NOTIFICATION_LISTENER_SERVICE` declaration before the merge. They remain as
history only: **no notification listener is in 5.5.1.**

**Follow-ups made on the branch before it merged**

| Commit | Change |
|---|---|
| `cc37594` | Move the audio observation off Firebase's dispatch thread, where it held each following alert back for 2.6 s |
| `99c730a` | Withdraw `5e352df` |
| `6f3a4ef` | Keep one export for the journal, the hidden gesture; the Safety & Privacy button is gone |
| `929d2af` | Ignore exported journals in git, as a second defence |
| `3462a32` | One sampler per alert, feeding both the fallback's verdict and the observation row |
| `901ecdc` | Pin that the sampler decides early but never stops early |

`cc37594` repairs a defect that was dated backwards by a discovery, not caused by
carelessness: that Firebase delivers messages through a single-threaded executor was
established only while building #31, by reading the library's sources.

Three commits on the branch are content duplicates of `f62d0bd`, `d342e96` and `70c1d3c`;
git reconciled them without conflict. Two documentation commits, `dc9f608` and `f7ab226`,
wrote earlier versions of this file there.

### The diagnostics journal and its hidden gesture

**The journal ships in 5.5.1.** `HistoryDiagnosticsLog` writes into `filesDir`, and a
release build is not debuggable, so without a way out from inside the application the file
could not be reached on the test device at all. The export is deliberately not a control: a
long press on the version label at the foot of the login screen exports the journal and
opens the system share sheet. Two toasts are the only text a user can meet, and only after
the gesture.

It sits in `src/main` rather than `src/dev` because the investigation runs on a
`stableRelease` build, so a development-only export would not exist where the evidence is.

**Account and channel are in the journal on purpose.** The `ChatFragment` wrapper puts both
on every line it writes, because a private file in a non-debuggable release is narrower
than Logcat and those fields make a report readable. `39a4763` took the same two fields out
of Logcat. The coincidence of names is a decision, not an oversight.

**Every local reset erases it**, since `5b1bd57`, including *Reset local data, keep
accounts*. The deletion runs on the journal's writer, in order with lines already queued.
Lines from a history backfill that started before the reset are dropped rather than written
into the fresh journal. One residue is accepted knowingly: an alert's audio watcher still
running at the reset writes at most two lines after it, `fcm.notification.audio` and
`fcm.notification.alert_audio`, carrying no account and no channel.

**The privacy policy declares it**, since `a1f4d7e`: what is recorded, that it stays on the
device, is never sent automatically, leaves only by a deliberate action of the user, and is
erased by every reset. It does not describe the gesture: a policy declares data practices,
not affordances. The copy the app links to, `https://unouidol.github.io/tmc/privacy.html`,
was updated with it on 2026-09-16, so the published policy does not lag the build. Terms version 2 (`7cf64f8`) puts
the acceptance gate in front of existing users on 5.5.1, with the updated policy behind it.

`DiagnosticsExportGesture` is reachable through a single `attach(View)` with one call site,
so confining it to the development flavour later is three moves, not one:

1. `git mv` the file into `src/dev/java/com/fs/twitchminichat/diagnostics/`;
2. a twin in `src/stable/java/...` declaring the same object with an empty `attach`;
3. move `diagnostics_export_empty` and `diagnostics_export_failed` into
   `src/dev/res/values/` — otherwise `UnusedResources` reports them in the stable flavour
   and, with `warningsAsErrors` in force, brings the build down.

The call site in `LoginFragment` does not change in any of the three.

### Policy pages: two copies, and the resets they describe

Every policy page exists twice: bundled in the APK under `app/src/main/assets/policies/`,
and published at `https://unouidol.github.io/tmc/` from the `unouidol/unouidol.github.io`
repository. `000318a` (#38) makes changing both together a rule in `AGENTS.md`. The rule
comes from a measurement: on 2026-09-15 the published data deletion page differed from the
APK's by 35 lines, a section added in May that never came back into the app, with option
names that no longer matched it. `cf8832d` (#39) corrects it in 5.5.1 by rewriting the page
from the code rather than from either copy. `style.css` still differs by 48 lines; that is
presentation only, and not corrected here.

Rewriting the page from the code found two defects, fixed in 5.5.1 by #40:

- the keep-accounts reset erased the device credential, leaving the phone's server
  registration unreachable by either deletion option (`6055751`);
- "Erase everything on this device" left the built-in browser data behind (`86566d7`).

One limit remains, and the page states it: the full erase still removes the credential, as
every reset did before 5.5.1, so a registration orphaned that way is reached only by an
email request.

Both published pages went out in one publication on 2026-09-16,
`unouidol/unouidol.github.io@bcfbe2a`, before the tag. Fetched afterwards, each is identical
to the copy in the APK apart from the link names, and the privacy page's trailing newline.

### Silent alerts: where the investigation stands

The periods divide at **2026-09-11, after 15:23:25**. That is the last
`Couldn't open fd for content://settings/system/notification_sound` on the test device; in
the capture of 2026-09-14 the count is zero and the default sound is
`content://media/internal/audio/media/133`. Before that moment the device's own default
notification sound was broken, which was the dominant cause of silence and confounds every
observation made before it.

Measured on 2026-09-14 from three journal exports, deduplicated line by line because
rotation makes them overlap: **458** alerts posted, **402** in an audible environment (none
of the four suppressing conditions).

| Period | Alerts | Short or missing sound (< 1200 ms) |
|---|---|---|
| Before the boundary, 09-10 12:32 to 09-11 15:04 | 146 | 21 |
| After the boundary, 09-12 19:35 to 09-14 07:36 | 136 | 6 |

After the boundary the system's alert player never started for 3 of 135 alerts from a warm
process. There was one cold alert, which played normally with an audible span of 1743 ms,
so whether a young process alerts silently cannot be measured from these data in either
direction. Every defect after the boundary happened in a process alive for more than ten
minutes.

These figures supersede the ones in the message of `c4f4ffb` (279 alerts, 14.5% to 3.7%,
3 of 20 against 2 of 114), which cannot be reproduced and whose definition was never
written down.

What 5.5.1 adds to the question: the fallback from #31 plays the channel's sound itself when
no system player appears within 2500 ms, and every alert writes one
`fcm.notification.alert_audio` line with its outcome (`played`, `fallback`, `suppressed`,
`unobserved`), `processUptimeMs` and `sampleCount`. A cold alert now answers the open
question on its own line.

The same figures now replace the unreproducible ones in the code as well: the KDoc of
`NotificationAlertFallbackPolicy` carried the numbers from `c4f4ffb`'s message until the
change below.

#### Decision, 2026-09-18: the fallback does not play on a watch that saw nothing

This replaces an earlier instruction to play even when the watch was blind. Nothing
implementing that instruction had landed. The decision changed on data, measured on log27:
85 armed alerts between 16 and 18 September.

- **The sampler went blind 12 times, 14%.** The fallback played 13 times: once on 40
  readings, a real repair, and twelve times blind. The blind ones were heard as doubled
  alerts.
- **Silence is rare where it can be seen.** Of the 73 alerts observed regularly, 1 was
  really silent, 1.4%. Across 12 blind windows that predicts about 0.17 silent alerts.
- **So playing in the dark bought a sixth of a spawn and cost twelve doubled chimes.**

The mechanism is in the code, isolated rather than supposed. In `sampleAndDecide`, a single
`pause(60)` served after 2.7 seconds ends the `while` after one reading, before any reading
reaches the 2500 ms grace. `decided` stayed false, and the final branch played, treating
"the loop ended undecided" as "no player was born". The same branch also played when a pause
was interrupted.

What changed:

1. **No playback without adequate sampling**, at least `MIN_SAMPLES_FOR_SILENCE` readings.
   That is half the grace divided by the polling interval, derived from the two constants
   rather than written by hand: 2500 / 2 / 60, so 20 today. Below it the line reads
   `outcome=unobserved` with its `sampleCount`, and nothing plays.
2. **The four settings are read again just before playing**, not only before posting.
   Do Not Disturb switched on during the 2.5 seconds is now honoured; the line reads
   `outcome=suppressed checkedAt=before_playback` with the reason.
3. The path with no `AudioManager` used to play without a single reading. It is unreachable
   today, because a missing manager reads the ringer as a sentinel and disarms the fallback,
   but it follows the same rule now and reports `unobserved`.

The 2500 ms grace is untouched. In the field the system player started between 726 and
1039 ms across more than seventy alerts, so the margin stands.

**One gate for every playback** (the follow-up to #42). Three places could play the
fallback: the decision inside the sampling loop, the undecided end of it, and the path with
no `AudioManager`, each with its own rules. The first still played on as few as two
readings: a single pause served between 2.5 and 2.6 seconds late puts the second reading past
the grace. That case has never been seen in the field - the stretched pauses observed fell at
2700, 3700, 4600, 11200 and 25800 ms, none between 2500 and 2600 - but it is the same
conflation, and the sample is small. `PlaybackGate` in `NotificationAlertFallback` is now
the only code that calls the player. It checks the sampling first, then the four settings
through the service's `currentSuppressionReason`, so a fourth playback point cannot appear
without both checks.

## Not in 5.5.1 — `work/irc-read-timeout`

On `origin`, one commit, no pull request. Left out of this release by decision.

| Commit | Change |
|---|---|
| `0d7e9b1` | Bound the IRC socket read, so a half-open connection cannot silently freeze the chat |

Both ordering constraints are now satisfied. It must land after `70c1d3c`, which rewrites
the same ten lines of socket construction; in that order the merge places the read timeout
after the handshake, where it belongs. And it deliberately causes mid-session reconnects,
the case `b7535a3` repairs; `b7535a3` reached `main-v5` with `17918a8`, so a timeout no
longer opens a gap that nothing recovers.

The branch was cut from `502c11e`, before `f62d0bd`, so it has no `CLAUDE.md` and the
repository rules do not load on it. **Rebase it onto `main-v5` before anything else**, then
re-measure the merge rather than relying on the constraint above.

## Not in 5.5.1 — the server's copy of the alert settings can outlive the phone's

**A correction first.** An earlier version of this section said that start-up "runs only
`uploadToken`, which does not carry the mode". That was wrong, and the record is kept rather
than quietly replaced. Read again on 2026-09-25:

- `FcmRegistrationUploader.uploadToken` builds a plan with
  `PcgProfileRegistrationSyncPlanner.buildPlan(selection)` and runs it: `REGISTER_TOKEN`,
  then `RESTORE_ALERT_SELECTION`, which calls `setProfileSpawnAlertModeBlocking` with the
  **local** selection. So start-up does push the mode, and has since `092bb33`, 2026-08-20,
  "fix: restore alert selection after FCM registration".
- The local selection is read by `PcgProfileAlertSelectionStore.read`, combining
  `PcgSpawnAlertModeStore`, `PcgEventSpawnAlertStore` and `PcgMostWantedStore`.

**The defect, as it actually is.** `buildPlan` returns an empty list when
`selection.requiresFirebaseDelivery` is false, that is when no category is active: no
ordinary mode, no event spawns, no Most Wanted. `uploadToken` then logs
`register_fcm skipped: no active alert category` and returns before sending anything.
`MainActivity`'s start-up check does the same one level up: it skips the call entirely for a
profile whose selection needs no delivery. So a phone whose local state says "no alerts"
never tells the server so at start-up, and a server still holding an active mode keeps
sending. **The app can show no alerts while alerts keep arriving**, and nothing at a later
start repairs it.

What does *not* produce that state is a reset on its own: the defaults restored after
*Reset local data, keep accounts* are `PcgSpawnAlertMode.DEFAULT`, which is `DEX_AND_TIER_A`,
so a category is active and the next start pushes that default up. The dangerous state is
"locally nothing active" reached some other way - a disable that failed, which nothing
retries.

**Account removal tells the server, once.** `AccountProfileRemovalController` removes the
account and its profile-scoped data locally, calls back, and only then calls
`setProfileSpawnAlertMode` with `PcgSpawnAlertSettings.DISABLED` and
`mostWantedEnabled = false`. The outcome is logged as `backend notification disable
completed ok=…` and nothing else: `setProfileSpawnAlertMode` sends one request and reports
whether it worked, with no retry anywhere. If it fails, the profile stays in that device's
`profile_ids` on the server, and no later start can repair it, because the account is
already gone from the phone and start-up only iterates the accounts still stored.

The data deletion page's *"On the server: nothing. No request is sent."* stays true for the
reset options: those paths send nothing themselves.

**The work is a review, not a line of code.** At least four stores have a counterpart on the
server — the alert mode, the custom watchlist, the Pokédex snapshot and Most Wanted — across
three endpoints: `/set_spawn_alert_mode`, `/set_custom_watchlist` and `/upload_dex_list`. For
each one it has to be decided whether a local reset keeps the local state, or resets the
server's side with it. Adding a file to the reset's exclusions would answer one case by
accident and leave the rest as they are.

The page stays as it is and describes today's behaviour truthfully. When the behaviour
changes, the page changes with it, and both copies change together: the rule `000318a` (#38)
adds to `AGENTS.md`.

## Test device

Measured from the journal export of 2026-09-14, not from memory: it contains `listener.*`
lines and `fcm.notification.audio` lines with `elevatedSpanMs`, and no
`fcm.notification.alert_audio` line. So the device was then running a build of the
diagnostics branch **with the notification listener and without the fallback** — reporting
`versionCode 7` and `versionName 5.5.0`, like the published build.

An earlier version of this section said that from 5.5.1 on builds are told apart by their
number. That was wrong. Three sets of 5.5.1 artifacts were built from three commits, all
reporting `versionName 5.5.1` and `versionCode 8`, and nothing on the device, in the journal
export or in the APK told them apart. A version number is shared by every build of a release.

From the commit that adds `BuildIdentity`, every build carries its commit. The login screen
reads `Version 5.5.1 (build 8, abc1234)`, and the journal export header has a line
`# app 5.5.1 (8) abc1234`. A `+` after the commit means the tree the build came from had
uncommitted or untracked changes, and `unknown` means the build could not read git at all.
Record the installed candidate here with the date and that commit. An `alert_audio` line in
the journal and the absence of `listener.*` lines still confirm, from the other side, that it
is a 5.5.1 build.

Update this section from the device, not from intent.
