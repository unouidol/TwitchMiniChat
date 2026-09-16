# Release state

What users actually have, what is waiting to reach them, and what the test device is
running. `AGENTS.md` describes *how* to release; this file records *where things are*.

It exists because that question kept being re-derived from scratch, and answered wrongly
once from a stale note, costing more time than keeping it written down.

**Every release updates this file in the same pull request that bumps the version.**
A change that reaches `main-v5` without appearing under "Waiting for release" is a change
nobody can account for later.

Last verified: 2026-09-16, against `origin/main-v5` at `cf8832d`, the commit
`release/5.5.1` is based on.

## Published — what users have

| | |
|---|---|
| Version | **5.5.0** (`versionCode 7`) |
| Tag | `v5.5.0`, annotated, pointing at commit `f0e0d35` |
| Published | 2026-08-31 |
| Assets | `TwitchMiniChat-Android-arm64-v8a.apk`, `TwitchMiniChat-Android-armeabi-v7a.apk`, `SHA256SUMS.txt` |
| Signer | `ST=Italy, L=Italy, CN=Unouidol` |

Asset names, tag shape and checksum file match the contract in `AGENTS.md`, so the public
download buttons on `https://tmc.ircminichat.party/` resolve.

`f0e0d35` is the merge commit of pull request #14. An earlier version of this file recorded
the tag as sitting at `8478d82`, which is the last commit *of the release branch* and the
parent on the other side of that merge. Both are on `main-v5`, so the mistake was invisible,
but the tag has always pointed at `f0e0d35`. Dereference the tag rather than reading a
branch head: `git rev-parse v5.5.0^{commit}`.

**5.5.1 is not published.** The version bump exists, on `release/5.5.1`, and is listed
below as waiting like everything else. There is no `v5.5.1` tag. The order is fixed: merge
the release pull request, build the release APKs, install them and pass the manual plan on
the device, and only then tag and publish. If a case fails, the fix lands on `main-v5` and
the build is repeated under the same number, because no 5.5.1 has ever left this
repository.

## Waiting for release — on `main-v5`, not published

Everything merged since the `v5.5.0` tag. None of this has reached users. List it with
`git log --first-parent v5.5.0..main-v5` rather than trusting a count written here.

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
`fcm.notification.alert_audio` line with its outcome (`played`, `fallback`, `suppressed`)
and `processUptimeMs`. A cold alert now answers the open question on its own line.

## Waiting for release — on `release/5.5.1`

| Commit | Date | Change |
|---|---|---|
| the release pull request | 09-15 | `versionCode 8`, `versionName 5.5.1`, and this file |

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

## Not in 5.5.1 — a local reset leaves the server's copy of the settings behind

Measured on 2026-09-16, on both sides, and not a new defect: it predates this release.

- **The phone forgets.** `accountSharedPrefsToKeepForTesting` in `SafetyPrivacyFragment`
  holds only the legacy account file, so *Reset local data, keep accounts* deletes the
  preferences of `PcgSpawnAlertModeStore` along with the rest, and the app comes back showing
  the defaults.
- **The server is never told.** `FcmRegistrationUploader.setProfileSpawnAlertMode` has exactly
  two callers outside the uploader: `ChatFragment:4522`, the bell, and
  `AccountProfileRemovalController:97`, removing an account. Start-up runs only
  `uploadToken`, which does not carry the mode.

So after that reset the server keeps the previous alert mode while the app shows the
defaults, until the user touches the bell. The bad case is the app saying no alerts while
alerts keep arriving, which is the kind of thing users report.

The same measurement is what makes the data deletion page's *"On the server: nothing. No
request is sent."* true for that option: nothing goes up at start-up either.

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

From 5.5.1 on, builds are told apart by their number rather than by behaviour: the login
screen shows `Version 5.5.1 (build 8)` on a 5.5.1 candidate. Once the
candidate is installed, record it here with the date. An `alert_audio` line in the journal
and the absence of `listener.*` lines confirm the same thing from the other side.

Update this section from the device, not from intent.
