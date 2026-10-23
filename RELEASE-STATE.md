# Release state

What users actually have, what is waiting to reach them, and what the test device is
running. `AGENTS.md` describes *how* to release; this file records *where things are*.

It exists because that question kept being re-derived from scratch, and answered wrongly
once from a stale note, costing more time than keeping it written down.

**Every release updates this file in the same pull request that bumps the version.**
Between releases it changes only when a position git cannot answer changes: a release is
published, work starts or stops waiting on a branch behind a condition, or the test device
runs a different build. What has merged since the last tag, code or documentation, is not
listed here: it is read from the log, as "Waiting for release" says. A row recording a merge
cannot land with the merge it records - a squash merge's commit does not exist until the
merge has happened, and the merge cannot edit its own tree - so every such row cost a second
commit, and the rows went stale anyway. #61 (`ddf9da4`) removed them for that reason. Do not
put them back.

An earlier version of this paragraph said that a code change had to appear under "Waiting for
release". It predated #61 and was not updated with it, and neither was the matching rule in
`AGENTS.md`, so both contradicted the section they pointed to. On 2026-10-15 the `AGENTS.md`
rule produced a report of a missing update that was not missing.

Last verified: 2026-10-01, against `origin/main-v5` at `4b8abe1`, with `v5.5.1` published.
**Stale, marked 2026-10-15:** not re-verified since. `main-v5` is now at `549e2f6`, 18 merges
later, and nothing below has been checked against it. Refreshing this needs the pass #64 made:
the latest release and the commit its tag points at, both assets downloaded through
`releases/latest/download/` and hashed against the recorded values and `SHA256SUMS.txt`, the
signer's certificate digest, and the package, version and commit the APK reports.

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

Code changes for 5.6.0 have started landing; everything after the tag is no longer
documentation only. 5.6.0 carries everything 5.5.2 set out to do, listed under "5.6.0 in
progress" below as objectives only, and the chat work. Which of them have landed is this same
log, and is not copied into a table that would then have to be kept true.

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
email request. The first 5.5.2 change ends that limit by a different route - deleting the
push token, so the backend prunes the record itself - and reverses one part of `86566d7`:
the all-or-nothing rule, not the browser clear. See "5.6.0 in progress" below.

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

**The defect, as it actually was, and what the third 5.5.2 change does about it.**
`buildPlan` returned an empty list when `selection.requiresFirebaseDelivery` was false, that
is when no category is active: no ordinary mode, no event spawns, no Most Wanted.
`uploadToken` then logged `register_fcm skipped: no active alert category` and returned
before sending anything. `MainActivity`'s start-up check did the same one level up: it
skipped the call entirely for a profile whose selection needs no delivery. So a phone whose
local state said "no alerts" never told the server so at start-up, and a server still holding
an active mode kept sending. **The app could show no alerts while alerts kept arriving**, and
nothing at a later start repaired it.

Both halves are removed by the third change: the planner produces a step for that profile,
and the boot loop no longer skips it. The acknowledged record decides when there is nothing
left to send.

What does *not* produce that state is a reset on its own: the defaults restored after
*Reset local data, keep accounts* are `PcgSpawnAlertMode.DEFAULT`, which is `DEX_AND_TIER_A`,
so a category is active and the next start pushes that default up. The dangerous state is
"locally nothing active" reached some other way - a disable that failed, which nothing
retries.

**Account removal told the server once, and that half is fixed by the second 5.5.2 change.**
`AccountProfileRemovalController` removes the account and its profile-scoped data locally,
calls back, and only then calls `setProfileSpawnAlertMode` with the disabled selection. The
outcome was logged as `backend notification disable completed ok=…` and nothing else: one
request, no retry anywhere. Worse, the backend session was removed immediately afterwards
whatever the outcome, so `BackendAuthHeaderProvider` resolved `Missing` and a retry could not
have authenticated even if something had attempted one. If it failed, the profile stayed in
that device's `profile_ids` on the server for good, because the account was already gone from
the phone and start-up only iterates the accounts still stored.

From the second change the session is kept until the backend acknowledges the disable, the
owed disable joins the same queue as the owed token deletion, and the user is told that alerts
for that account may keep arriving for a while. The other half - a profile still on the phone
with no category active, which start-up never mentions to the server - is the third change,
not this one.

The data deletion page's *"On the server: nothing. No request is sent."* was true for both
reset options that sent nothing. From the first 5.5.2 change it describes only *Reset local
data, keep accounts*: the local-only erase no longer exists, and the single erase that
replaced it always contacts the server. The page is rewritten for that in the fourth pull
request of this work, not the first.

**The work is a review, not a line of code.** At least four stores have a counterpart on the
server — the alert mode, the custom watchlist, the Pokédex snapshot and Most Wanted — across
three endpoints: `/set_spawn_alert_mode`, `/set_custom_watchlist` and `/upload_dex_list`. For
each one it has to be decided whether a local reset keeps the local state, or resets the
server's side with it. Adding a file to the reset's exclusions would answer one case by
accident and leave the rest as they are.

The page stays as it is and describes today's behaviour truthfully. When the behaviour
changes, the page changes with it, and both copies change together: the rule `000318a` (#38)
adds to `AGENTS.md`.

## Deferred — a pass over the rules an agent needs, and whether each is in this repository

Not started, by decision. Some of the method rules that govern work in this repository live outside
it, where an agent working from the clone cannot read them. That was found four times, each when such
a rule, or a fact a rule rested on, was given as if it were written here and the clone was checked
instead: the leaks inventory #80 cited, the battery figures #89's B1 started from, the project
instructions field, and a rule about interrupted mutation runs. Checking caught each, but checking
depends on someone doing it every time. The fix is a
deliberate pass over which rules an agent actually needs, and whether each is written here - not
anyone remembering to include them. The rules are not listed in this entry: finding them is the pass's
work, and a list guessed from one conversation would be the same error again. It waits until the
conversion to a recycling list is complete, 5d included: read from the clone, that has not happened
while `chatContainer` in `app/src/main/res/layout/fragment_chat.xml` is a `LinearLayout` inside a
`ScrollView`, holding a view for each row.

## Deferred — the Android Gradle Plugin 9.4 / Gradle 9.6 upgrade

Not on any branch, by decision. The upgrade from Android Gradle Plugin (AGP) 9.2.1 and Gradle
9.4.1 waits until the conversion that shows the chat's rows in a recycling list is complete -
the work whose first steps are #79 (`a382c94`) and #80 (`549e2f6`) - and its closing start-up
measurement has been taken and recorded in this file. The start-up measurements of that work
were all taken on AGP 9.2.1 builds - the version `main-v5` has carried since `453d1de` - and an
upgrade before the closing measurement would put its before and after on two toolchains, which
is what this entry exists to prevent. Both halves are read from the clone. The first does not
hold while `chatContainer` in `app/src/main/res/layout/fragment_chat.xml` is a `LinearLayout`
inside a `ScrollView`, holding a view for each row, as it is at `549e2f6`. The second does not
hold while "Closing start-up measurement of the conversion", below, holds only its placeholder.

The Java Development Kit (JDK) is part of the same deferral. Pinning it is itself a toolchain
change and would confound the same measurements, so it waits for the same moment as the AGP
upgrade. Until then each machine builds with the JDK it selects, and a build's label does not
say which: the known limitation that #83 (`0d1288d`) documents beside `gitShaForBuild` in
`app/build.gradle.kts`.

It is not optional forever. The release notes of AGP 9.4.0 (September 2026) state that the new
Variant application programming interface (API) is mandatory for all projects from AGP 10, and
introduce `android.newDsl.optOut` to migrate module by module before then:
https://developer.android.com/build/releases/agp-9-4-0-release-notes. The pressure on this
repository is smaller than that sounds. No build script here uses either Variant API, so AGP
10 would reach it through the plugins it applies (Google services and Crashlytics), if at all.
Android Studio's Upgrade Assistant recommends the upgrade; decline it until the condition above
holds.

### Closing start-up measurement of the conversion

Empty until it is taken. It will hold the start-up figures before and after the conversion,
the build each came from, the device they were measured on, and the JDK each build was made
with.

## 5.6.0 in progress — deletion does what it says, planned as 5.5.2

**5.5.2 will never be tagged or published.** It is formally closed and fully tested, and it
was deliberately shelved. The decision was: the chat work is urgent, the previous release was
only a week earlier, and shipping another update for this alone was not worth it. The next
release is **5.6.0**, which carries everything 5.5.2 set out to do plus the chat work. The
5.5.2 version number retires unused.

So "5.5.2" elsewhere in this file - "the first 5.5.2 change", "the 5.5.2 device round" - names
this work as it was planned. It reaches users as 5.6.0.

**This table is a list of objectives. It records no state.** It answers *what did 5.5.2 set out
to do*, which is now part of what 5.6.0 sets out to do; the chat work is not in it. What has
landed is a different question, and git already answers it:

```
git log --first-parent v5.5.1..main-v5
```

One objective per pull request, and the log cannot disagree with itself.

| # | Objective |
|---|---|
| 1 | One erase instead of two, and it finishes on its own |
| 2 | A profile-alert disable that survives a failed request |
| 3 | Tell the server when no alert category is active |
| 4 | Rewrite the data deletion page, both copies, and one sentence of the privacy policy |
| 5 | The device round written into the repository |
| 6 | What the round taught, folded back into the plan |
| 7 | The owed-disable log must not report a cancellation with nothing to cancel |
| 8 | The erase must tell "never registered" apart from "the network refused" |
| 9 | Re-anchor the round to `50c3069`, name the two new checks, address CI by SHA |

A condition git cannot answer - whether something was published outside this repository, what
the test device is running - is not state of a change and does not belong in a row either. It
has its own section below, where the reasoning can sit beside it.

**Why the state column is gone, kept as the evidence for it.** Rows 3 and 4 said "pull request
open" after both had merged. Rows 6, 7 and 8 did the same one round later. Row 9 recorded its
own merge, which took a pull request of its own to write down - bookkeeping that produced work
which then also needed recording. Each staleness was corrected by the next pull request to
touch the table, which is not a process, only a coincidence repeated six times.

The earlier fix was "update the row in the merge that moves it". It cannot be followed. A
squash merge's commit identifier does not exist until the merge has happened, and a squash
merge cannot edit its own tree, so a row naming a merge commit always costs a second commit
after the fact. Removing the column removes the cost and the loop together: there is nothing
left in a row that can go out of date, because a row now says only what the change was for.

**What the first one changes.** *Reset local data* offered three actions; the local-only
*Erase everything on this device* is gone, and the action that removes this device from the
server absorbed it. The erase now wipes the phone whether or not the server was reached,
because erasing one's own data must not depend on a reachable server, and it deletes the
Firebase Cloud Messaging token so that carrying on is safe: a phone with no token receives
nothing, and the backend drops the registration the next time it tries to send to it
(`fcm_sender.remove_registered_device_by_token`, on `UnregisteredError`). That is not a
deletion at the moment the user taps — if no spawn ever matches those profiles again, the
record sits there with a dead token and no way to reach the phone. No backend change is
involved anywhere in this work.

**Why a queue, and why it is not the automation the product rules forbid.** A token
deletion needs the network, so it can fail. `androidx.work` (new dependency, 2.12.0) carries
one owed-operations queue: unique work, a connected-network constraint, exponential backoff
from 30 s, and a start-up attempt as well — the worker is the normal path, and the start-up
attempt is the backstop because on the test device family background work is deferred or
killed by the vendor's battery management, the same mechanism that blinds the alert sampler.
The product rule protects gameplay and alerts from acting without the user; finishing a
server operation the user has already asked for, and that only stops something, is the
opposite case. The reasoning is in `OwedServerOperationWorker`, not only here.

**What deliberately does not survive the wipe.** `/delete_device_data` needs both a backend
session and the device credential, and the wipe erases both, so the device removal itself
can never be retried afterwards. Keeping those across the wipe was considered and refused:
it would leave a usable session on a phone the user has been told is erased. Only the token
deletion is owed, because it needs neither. `OwedServerOperationStore` records that.

**One decision from #40 is reversed, deliberately.** `86566d7` made a failed browser clear
abort the erase and erase nothing. That was right for *Erase everything on this device*,
which touched nothing outside the phone: aborting left it as it was. The merged action has
already deregistered the device and deleted the token by the time the browser is cleared, so
aborting would leave a phone removed from the server with its data still on it - the worse
half-state. A browser clear also fails locally, not for want of a network, so refusing the
erase over it denies the user the thing they came for. The failure is reported in the same
message and the erase completes. The browser is still cleared, and still before the wipe;
only the all-or-nothing rule is gone. The reasoning is in `SafetyPrivacyFragment`, not only
in the pull request.

**Two consequences that outlive the first pull request:**

- its manual case E1, from #40, changes meaning. It used to check that a failed browser
  clear erased nothing; it now checks that the erase completes and that the message says the
  browser data could not be cleared.
- **the fourth pull request must say this on the data deletion page**: the erase completes
  even when the browser data cannot be cleared, and the page must not imply that the two are
  one atomic step.
- the same pull request should add one sentence, if it fits without making the page heavier:
  until the deactivation is confirmed, the phone keeps that account's session so it can
  complete it.

### Two web copies are written and must not be published yet

The fourth pull request rewrites **two** pages in both of their copies - the data deletion page
and, in one sentence, the privacy policy - with the same wording and the same
"Last updated: 2026-09-26" on both. The bundled copies,
`app/src/main/assets/policies/data_deletion.html` and `privacy.html`, merge with it. The
published copies do **not** go out then, and this is not an oversight: `AGENTS.md` requires the
web copy to precede the release that bundles the new text, and publishing at merge time would
put public pages describing 5.5.2 behaviour in front of users who are all on 5.5.1 and have the
old behaviour. The two disclosures would contradict each other in the opposite direction from
the usual mistake.

**The two pages now carry different dates, deliberately.** `data_deletion.html` is
`2026-09-28`, `privacy.html` is still `2026-09-26`. `AGENTS.md` does prescribe a rule about that
date, but it governs the **two copies of one page** - "Change both in the same step, with the same
wording and the same 'Last updated' date" - not two different pages. Nothing anywhere requires the
deletion page and the privacy policy to agree with each other, and the earlier note in this file
recording them both as `2026-09-26` was a record of what happened, not a rule. So the eighth
change moved only the page it edited. Both copies of `data_deletion.html` carry `2026-09-28`, and
both copies of `privacy.html` carry `2026-09-26`, which is what the rule actually asks.

**Publishing them is one step of the 5.6.0 release, immediately before the tag** - between
step 7 (confirming `main-v5` holds the code the artifacts were built from) and step 8
(publishing the release) of the procedure in `AGENTS.md`. **Both pages in the same publication**,
never two publications before one tag: that is how one of them gets forgotten, and a privacy
policy that still describes the old retention while the deletion page describes the new
behaviour is the exact contradiction the two-copies rule exists to prevent.

The published copies are not stored a second time in this repository, because a stored snapshot
is free to drift from the copy that ships. Each is **derived** from its bundled copy, and by
nothing else - verified with `diff` at the time of writing:

```
report_block.html  -> report-block.html
data_control.html  -> data-control.html
data_deletion.html -> data-deletion.html
```

Applied to **every** `href`, not only the navigation: `privacy.html` links to the deletion page
from its user-controls section as well, so it takes four substitutions where the deletion page
takes three. `privacy.html`, `terms.html` and `credits.html` keep their own names in both. The
published files are LF with a trailing newline, which for `privacy.html` is a real difference -
the bundled copy has no trailing newline, and that is the difference this file has recorded
since 5.5.1.

Regenerate both at publication time, commit them as `tmc/data-deletion.html` and
`tmc/privacy.html` in `unouidol/unouidol.github.io`, then fetch each back and compare it
against the copy in the APK to confirm they agree apart from those links and that newline.

#### What the privacy policy changed, and why the acceptance gate was not raised

Section 5, Retention, said only that *"Some local settings remain on-device until the user
clears app data, removes them, or uninstalls the app"*, which implied that removing an account
removes everything kept for it. Since the second 5.5.2 change it does not: that account's
sign-in with the server is kept until the server confirms its alerts are off. One added
sentence says so in the reader's terms, and deliberately says nothing about the queue or the
mechanism - the deletion page carries that detail.

**Decision, 2026-09-26: `CURRENT_TERMS_VERSION` stays at 2**, so this does not re-show the
acceptance gate. The criterion, recorded here so the next change to these pages is judged
against it rather than by feel:

> The gate is raised for a **material** change: data newly collected, a new recipient, or a new
> purpose. It is not raised for a clarification, for a correction, or for behaviour that
> collects nothing new.

Neither 5.5.2 change is material by that test. A credential kept longer than the page implied
is a correction to a disclosure, and the background work the owed-operations queue performs
collects nothing new, adds no recipient, and serves a purpose section 3 already discloses
("to provide optional push alerts and manage device registrations"). Re-showing the gate for a
clarification teaches people to tap through it, and the gate is worth something only while they
still read it - 5.5.1 raised it a month ago, on 2026-08-31.

Also considered and deliberately **not** added: a note in section 2 about when the push
notification token is deleted. Nothing new is collected, and the token's fate is deletion
mechanics, which belong on the data deletion page.

**What the second one changes.** Three things, in the order they matter:

1. **The credentials outlive the attempt.** The backend session is removed only once the
   disable is acknowledged, not right after it is attempted. That is the whole point: the
   session is what the retry authenticates with, and throwing it away made a retry
   impossible rather than merely absent. The device credential was never removed by an
   account removal, so it was already there.

   **Decision on how long the kept session stays, 2026-09-25: it stays.** Until the disable
   goes through, until that account is signed in again, or until a full erase - and
   indefinitely if the backend were never reachable again. It is scoped to one profile,
   grants nothing that profile did not already have, and is the only thing left on the phone
   that can finish what the user asked for. A timeout would protect nobody: it would only
   guarantee that the orphaned alerts stay on for good, reachable afterwards by an email
   request and nothing else. Deliberately **not** symmetric with the erase, which keeps no
   credential across its wipe: there the user asked for everything to go, here for one thing
   to stop. The two read as contradictory taken one at a time, so the reasoning sits in
   `AccountProfileRemovalController` beside the code that keeps it.
2. **One queue, two kinds.** The owed disable goes into the same
   `OwedServerOperationWorker`, with the same network constraint, the same backoff and the
   same start-up backstop as the owed token deletion. Both kinds are attempted on every
   pass, and one failure anywhere makes the pass a failure so everything still owed is
   retried.
3. **What the backend has acknowledged is now written down.**
   `PcgProfileAlertAcknowledgementStore` records a selection only after the backend answered
   2xx for it, recorded in the one function every push funnels through. Until then the
   application knew what the user had chosen but not what the server had been told, so
   "already sent" and "never sent" were indistinguishable and nothing could be either
   retried or skipped. `PcgProfileAlertPushPolicy` compares the two, and covers both cases
   that were missing: a selection with no category active still has to be sent, and a
   profile no longer on the phone can still owe a disable. The third change uses the same
   policy for the start-up path.

A profile with no acknowledgement is deliberately **not** read as acknowledged-disabled:
nothing is known about the backend's copy, so no request is made on a guess. That leaves one
gap the acknowledgement alone cannot cover - an installation upgrading into 5.6.0 has
acknowledged nothing, so a removal failing there would look as if it owed nothing - and an
explicit owed marker in `OwedServerOperationStore`, written only after an attempt has failed,
covers it. That is an addition to the shape agreed for this work, stated rather than slipped
in.

**What the fourth one changes on the page.** The page described the old behaviour in three
ways that are now wrong, each of which said something reassuring that was not true:

- it documented *Erase everything on this device*, which no longer exists, and told the reader
  its registration stays on the server and that they should choose the next option if they
  want alerts to stop;
- it said the erase wipes the phone *only after the server has confirmed*, and that nothing is
  erased if the request fails. The erase now completes either way, and deleting the push token
  is what stops the alerts;
- it said the built-in browser data is cleared first and that nothing is erased if that fails.
  The erase now completes and reports which part failed.

What the new page is careful **not** to say: that the registration is removed the moment the
user taps. It says the server drops it the next time it tries to reach the phone, and that the
record can sit there if nothing is ever sent again. It also keeps the erase and
*Delete app account and all data* apart - the erase does not delete profile-scoped server data
- and describes switching a profile's alerts off as the server dropping that profile from this
phone's list, never as deleting anything.

**What the third one changes.** `PcgProfileRegistrationSyncPlanner.buildPlan` returned an
empty plan whenever `selection.requiresFirebaseDelivery` was false, because the plan was
derived from whether a notification would be delivered: "nothing to deliver" was read as
"nothing to say". Such a profile now gets a plan that sends the disabled selection, and only
that - registering a token for a profile that wants no notification would put the
registration straight back.

`MainActivity`'s boot loop skipped the same profile one level up, so the new step had to be
made reachable from there or it would have been a step nobody runs. The skip is gone, and the
single decision now lives in the planner. The acknowledged record from the second change is
what keeps this from being one request per launch for ever: once the backend confirms the
disabled selection, later starts send nothing.

What the backend does with a disabled selection, stated because it is easy to describe
wrongly: it **drops the profile from that device's `profile_ids`**, which is what stops the
alerts. It deletes nothing.

**The rule that would break a working install if it were wrong:** an owed token deletion is
void the moment an account is signed in again, not merely postponed. Deleting the token then
would silently cut the alerts of a registration the user has just recreated, with no symptom
but alerts that never arrive. `OwedTokenDeletionPolicy` decides it, the worker re-reads the
record before acting, and signing in clears it.

**The same rule for the owed disable**, in `OwedProfileDisablePolicy`: sending it after the
account has been signed in again would switch off the registration that sign-in created. Both
guards are there - `AuthCallbackActivity` clears the record at sign-in, and the queue
re-reads whether the account is back before acting - because either alone would leave a
window.

## The 5.5.2 device round

**Executed in full, in two runs on the dev flavour, and no blocking defect was found in
either.** The first, 2026-09-26, at `80c042c` - the commit the round was anchored to then:
every case passed. The second, 2026-09-30, against `main-v5` at `bf7dd65`, covered the two
checks the first run never reached, A5 and D8: both passed. Results and measurements are in
the project document `claude/deletion-scope-architecture.md`, sections "Giro dispositivo della
5.5.2" and "Seconda esecuzione, 2026-09-30"; this file records the outcome and what the round
taught, not the measurements.

Two corrections came out of it, both inside 5.5.2 and neither touching an endpoint or a
registration key, so no backend coordination: the owed-disable policy reporting a cancellation
where there was no debt, and the erase failing to distinguish "never registered" from "the
network refused". They are rows 7 and 8 of the table above.

**What the second one turns on.** `DeletionCredentials.Blocked` covers three reasons the request
is not sent, and only one of them means the installation never registered: the device credential
is **absent**. A missing backend session is the state of a phone whose accounts have all gone -
which is registered, and still reports `ALERTS_STOPPED` - and a credential that exists but cannot
be read means a registration may well exist while this phone can no longer prove it owns it, which
is a failure. So the signal carried to the policy is `notRegistered`, set only for the absent case,
rather than derived from `Blocked`. The new outcome takes precedence over the other two, because
both of those describe a registration that exists.

`tokenDeletionOwed` stays `!tokenDeletionOk` in that case too: the token and the registration are
different things, and the user asked for everything on this phone to be gone. The message
deliberately says nothing about the token, and nothing about a future removal - the defect it
replaces was telling the user their device would be removed at the next attempt when there had
never been anything to remove.

The round is written down in `docs/manual-plan-5.5.2.md`, in the repository rather than spread
across four pull request bodies, so it travels with the clone and can be run with nothing open
beside it.

It is **one round for the whole objective**, not one per pull request: the cases are
deduplicated, ordered non-destructive first, the flight-mode ones grouped, and the erases last,
because each erase signs every account out.

**The anchor is retired.** "The round applies to `main-v5` at `50c3069`" - set by #59, after the
two corrections landed - was true only while 5.5.2 was heading for a tag. What it protected was
the release: the build tagged as 5.5.2 had to be the application the round had tested. 5.5.2
will never be tagged, so there is nothing left for the anchor to protect, and it is not a
constraint on anything any more. Code merged after `50c3069` breaks no rule by moving away from
it.

What replaces it: **the round has to be re-run on the final 5.6.0 build before that release**,
because the chat work changes the application between now and then. The two runs above are
results for the code they were made against, not for 5.6.0.

While the anchor stood, the label on the login screen named whichever commit was built, in the
shape `Version 5.5.1-dev (build 8, <commit>)`, and was **not** expected to read `50c3069`:
every document merged after the code moves the tip without changing the application. Whether
a build was this round was decided by the check in the plan's first section:

```
git diff 50c3069 HEAD -- app/ gradle/ build.gradle.kts settings.gradle.kts gradle.properties
```

Empty meant the build was this round, whatever commit its label named. Matching the label
against a commit written here decides nothing, and naming an expected label in this file would
make it a second place to keep true - the mistake this section has already made once. A `+` for
a dirty tree, or `unknown` in place of the commit, disqualifies a run outright whatever any
check says.

Three things the round deliberately cannot settle, each stated in the plan rather than left to
be discovered: the true upgrade case, because the dev flavour is a separate application id and
installs beside the release rather than over it; the backend's pruning of a dead token, which
happens on the backend's own schedule; and the worker's behaviour under a long run of failures,
which is not reachable by hand in a sitting.

Record the outcome in the test device section below, with the label actually read on the screen.

**Why there was a second run.** The 2026-09-26 run was made at `80c042c`, and the two
corrections it produced landed after it, as `31cdedc` and `50c3069`; that is why #59
re-anchored the plan to `50c3069`. Two cases were added for them, and the second run, on
2026-09-30, was their first: **A5**, the absence check for `31cdedc` - the line
`owed profile disable decision=VOID_SIGNED_IN` must **not** appear at start-up on a healthy phone -
and **D8**, the only check anywhere for the `NOT_REGISTERED` outcome, since
`resolveDeletionCredentials` is private and no unit test reaches the flag. D8 is marked blocking:
`ALERTS_STOPPED` or `NOTHING_REACHED` there means `notRegistered` never reaches the policy. Both
passed.

**Four things the first run cost time on, now written into the plan** so a second run does not
pay for them again:

- the device erase is the **second button inside the *Reset local data* dialog**, and nothing is
  called "Erase everything on this device" any more. Taking that phrase for a live menu entry
  ran *Delete app account and all data* twice in its place, and two cases had to be redone;
- the online erase needs the account **registered** first - sign in, then force-stop and reopen -
  because `uploadToken` runs from the boot pass. Without it the server call is never attempted
  (`delete_device_data skipped: device credential missing`) and the case proves nothing;
- the two `VOID_SIGNED_IN` cases are **not reachable by hand**: signing an account back in needs
  the network, and the network drains the queue within a couple of minutes. The plan carries the
  `adb` recipe for injecting the owed record instead;
- log capture that actually works is the **Android Studio Logcat panel** filtered
  `package:com.fs.twitchminichat.dev` at Debug. `adb logcat -d TAG:D *:S` returns nothing, and
  PowerShell's `>` writes UTF-16 that `findstr` cannot read. Filter by package, not process: the
  wipes restart the application under a new pid.

Also measured: `FirebaseMessaging.deleteToken()` fails without the network (`ExecutionException`)
and succeeds with it, so flight mode is a valid way to produce the token debt. And the v10 console
prints no timestamps - the authoritative time of a registry change is `updated_at` in
`registered_devices.json`.

## Test device

**Read from the device on 2026-10-23,** over adb, from a OnePlus AC2003 on Android 12 (API 31).
Each commit below was read from the APK installed on the phone, pulled back off it, with the
build identity read from its code; the version and build number are what the phone's package
manager reports.

| App | Package | Version (build) | Commit | Installed APK, sha256 | Last updated, phone time |
|---|---|---|---|---|---|
| Stable | `com.fs.twitchminichat` | 5.5.1 (8) | `007ee55` | `bf6c22c21f4f5d0b2a49cb2e5406659141cf0e87eb917183db692af67965aaf8` | 2026-09-18 14:08 |
| Dev | `com.fs.twitchminichat.dev` | 5.5.1-dev (8) | `d56812b`, tag `device-test/5.5.1-dev-8-d56812b`, #89 | `8519b89435331cb920c7852972ad9e0fba866f1bd8c958b3c82fc5582aa5e6ce` | 2026-10-23 09:23 |

- **The stable app is the published 5.5.1.** Its APK's sha256 is the arm64 checksum recorded
  under *Published* above, as listed in the release's `SHA256SUMS.txt`, and its code carries
  `007ee55`, the commit `v5.5.1` points at.
- **The dev app is #89 as it was tested on the device, at `d56812b`:** the build its last harness
  run ran on, which reported `d56812b` itself at run time. The round's manual checks and its
  background measurement ran on `ef95c63`, whose production code `d56812b` shares: `git diff
  ef95c63 d56812b -- app/src/main gradle build.gradle.kts settings.gradle.kts gradle.properties
  app/build.gradle.kts` prints nothing. Before #89 it was #86's `c1380af`, sha256 `c4cc57f8…`,
  read from its APK on 2026-10-21 before it was replaced.
- **Not read: the login screen's line and a journal export's `# app` line,** the two places this
  section otherwise reads a build from. Both need the app operated by hand. #86's first manual
  check reads the dev app's login screen.

This section had been marked stale on 2026-10-15: builds had been installed since its previous
reading and none was recorded. That reading, from the journal export of 2026-09-14, found
`listener.*` lines and `fcm.notification.audio` lines with `elevatedSpanMs`, and no
`fcm.notification.alert_audio` line: a build of the diagnostics branch **with the notification
listener and without the fallback**, reporting `versionCode 7` and `versionName 5.5.0`.

An earlier version of this section said that from 5.5.1 on builds are told apart by their
number. That was wrong. Three sets of 5.5.1 artifacts were built from three commits, all
reporting `versionName 5.5.1` and `versionCode 8`, and nothing on the device, in the journal
export or in the APK told them apart. A version number is shared by every build of a release.

From the commit that adds `BuildIdentity`, every build carries its commit. The login screen
reads `Version 5.5.1 (build 8, abc1234)`, and the journal export header has a line
`# app 5.5.1 (8) abc1234`. A `+` after the commit means the tree the build came from had
uncommitted or untracked changes, and `unknown` means the build could not read git at all.
Record the installed candidate here with the date and that commit. An `alert_audio` line in
the journal and the absence of `listener.*` lines still confirm, from the other side, that a
candidate is a 5.5.1 build.

**A build taken from a pull request's branch is recorded with a tag and the pull request's
number, beside its commit.** The squash merge does not keep that commit on `main-v5`, so
`git log main-v5` never reaches it. Put a lightweight tag on the tested commit, named
`device-test/<versionName>-<versionCode>-<commit>` as in `device-test/5.5.1-dev-8-c1380af`,
push it, and confirm it with `git ls-remote --tags origin` before writing the record: a tag
only in the clone that made it is not a record. Push it before the pull request merges, while
its branch still reaches the commit. Write the number beside it, as the dev entry above does.
The two are not redundant: the tag is how the commit is retrieved, the number is where the
build came from, which a tag name carries badly. The commit is in the tag's name because a
version and a build number are shared by every build of a release: `86eab10`, the dev build
before #86, has 5.5.1 and build 8 in its `app/build.gradle.kts` too, so a name without the
commit would already have collided once. A record written inside a pull request cannot carry
that pull request's own squash commit: its identifier does not exist until the merge has
happened and a squash merge cannot edit its own tree, so writing it in would always cost a
second commit after the fact - the bookkeeping #61 removed.

Why the tag: a pull request's number points at a record GitHub keeps, `refs/pull/<n>/head`, and
nothing in this repository can check that GitHub still keeps it, while a tag is in the log
itself. And a tag can be made only while something still reaches the commit, so it is made at
the device test: a tag kept back as the fallback for a lost pull ref could be applied only
after that loss, when nothing might reach the commit to put it on, and a fallback that can
only be applied after the loss is not a fallback. The cost is one tag per device test, a few
per release.

Pull request #87 first proposed the number alone, with a tag only as the fallback should the
pull ref ever be gone, and was changed before it merged. The number alone left the only way
to the commit outside this repository, in a copy of the log that GitHub keeps, which is the
case `AGENTS.md` means by "The log cannot disagree with itself; a copy of it can." And the
fallback could only be applied after the loss it was meant to cover, when nothing might reach
the commit any more to put a tag on it. Without this reason the tag reads as bookkeeping that
repeats the number, and the next pass removes it.

The pull ref stays as a second path, no longer the only one: `git fetch origin pull/<n>/head`,
the commit being that ref or one of its ancestors (`c1380af` is the parent of #86's last head,
`d0a984c`). That GitHub keeps such refs after a merge is known rather than measured; what was
measured is that `refs/pull/86/head` existed on 2026-10-19.

**The example itself is in the weaker state.** On 2026-10-23, after #89 merged, no ref in the clone
reached `d0a984c`: it lived only in GitHub's `refs/pull/86/head`, read that day - the dependency the
tag moved this rule away from. It is not a defect: it was never a recorded build, and `c1380af`, the
device build of that round, has its own tag and resolves. The example still explains the mechanism if
its command one day returns nothing, so it stays; whoever next changes this section should know it
rather than find it again.

Update this section from the device, not from intent.
