# Release state

What users actually have, what is waiting to reach them, and what the test device is
running. `AGENTS.md` describes *how* to release; this file records *where things are*.

It exists because that question kept being re-derived from scratch, and answered wrongly
once from a stale note, costing more time than keeping it written down.

**Every release updates this file in the same pull request that bumps the version.**
A change that reaches `main-v5` without appearing under "Waiting for release" is a change
nobody can account for later.

Last verified: 2026-09-11, against `origin/main-v5` at `70c1d3c`.

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

There is **no 5.5.1**: no tag, no commit, no version bump. Builds newer than 5.5.0 still
report `versionName 5.5.0` and `versionCode 7`, so on a device they are indistinguishable
from the published one. Calling such a build "5.5.1" in conversation is a convenient
shorthand and nothing more. The only way to tell a newer build apart on a device is by
behaviour.

## Waiting for release — on `main-v5`, not published

Thirteen commits since the `v5.5.0` tag, four of them merge commits. None of this has
reached users.

| Commit | Date | Change |
|---|---|---|
| `a67cd37` | 08-31 | Document the release and distribution contract (docs only) |
| `f8ea482` | 09-01 | Encrypt locally stored Twitch credentials at rest |
| `a77b129` | 09-01 | Add "delete only this device" to Safety & Privacy — completes the app side of `/delete_device_data` |
| `7cf64f8` | 09-02 | Opt-out crash reporting |
| `71f9850` | 09-02 | Report the silent account storage failures |
| `502c11e` | 09-02 | Report the features that stop working without saying so |
| `f62d0bd` | 09-11 | Add `CLAUDE.md` importing `AGENTS.md`, so the repository rules load (docs only) |
| `d342e96` | 09-11 | Stop a failed read from erasing the stored accounts |
| `70c1d3c` | 09-11 | Verify the name on the certificate before sending the Twitch token |

`d342e96` and `70c1d3c` are the two that most deserve a release: one prevents permanent
loss of every stored account after a single transient Keystore failure, the other stops the
Twitch token being handed to whoever can redirect the connection.

## Waiting for release — on `work/history-diagnostics-observability`

Branched from `502c11e`. Twenty-two commits not on `main-v5`, of which three are content
duplicates of `f62d0bd`, `d342e96` and `70c1d3c` above: those were cherry-picked out of this
branch and merged separately because they did not belong to its objective. They stay here as
well, and git merges them without a conflict because the content is identical.

The remaining nineteen divide as follows.

**Eight fixes that change what users experience**

| Commit | Change |
|---|---|
| `7a57893` | Recover the window an off-screen chat page cannot measure |
| `bb319d4` | Retire the frozen notification channels and delete the ones earlier versions left behind |
| `5e352df` | Let a freshly started process settle before alerting |
| `1cad63c` | Stop alerting for spawns whose window has already closed |
| `ae141be` | Ask again for an hour the network refused to deliver |
| `570580e` | Recover the offline window for a page that never resumes |
| `39a4763` | Keep the account and channel out of Logcat |
| `b7535a3` | Recover history on a mid-session IRC reconnect with no `onStop` |

**Nine commits of diagnostics**

| Commit | Change |
|---|---|
| `73521ef` | Why the chat history backfill runs or is skipped |
| `f16aab1` | What happens to every push that arrives |
| `a4c124e` | That a spawn happened, not only that a push arrived |
| `5ca53dc` | The state Android was in when it chose not to alert |
| `af74bfb` | How old the process was when a push arrived |
| `5616371` | Whether a sound was actually played |
| `b7b43ea` | Ask the system whether it alerted, instead of inferring |
| `377d91c` | Stop the alert probe from inventing silent alerts |
| `a01f99a` | Measure how long the spawn alert actually sounds |

**Two documentation commits**: `dc9f608` created an earlier version of this file, `f7ab226`
rebuilt its branch table. Neither can be cherry-picked to `main-v5`, because the file did not
exist there until this commit created it.

### Blocking condition before this branch may merge

`b7b43ea` declares `NotificationAlertProbeService` in the manifest as an exported service
behind `BIND_NOTIFICATION_LISTENER_SERVICE`, with a
`android.service.notification.NotificationListenerService` intent filter. A notification
listener can read every notification on the device, which is the most invasive permission
this application has ever requested, and it exists to answer one open question: why some
spawn alerts are posted without making a sound.

**The listener must be removed from this branch before any merge into `main-v5`.** Merging it
as it stands would carry that capability onto the release branch and, from there, to users
who never needed it. It is not a matter of sequencing: nothing downstream fixes it.

The investigation is still open, and the service has to stay running on the test device while
it is. So the order is: finish the investigation, take the listener out, then merge. Not the
other way round.

The probe does filter to this application's own PCG channels in code, but what the user grants
to enable it is device-wide notification access, in an application whose manifest otherwise
asks only for `INTERNET` and `POST_NOTIFICATIONS`. A comment in `AndroidManifest.xml` saying
it must not ship is the weakest possible guarantee. Moving the service and its manifest entry
into a `src/debug/` source set would make shipping it impossible rather than merely
discouraged, and is the preferred resolution.

### Second decision required: the diagnostic journal itself

`HistoryDiagnosticsLog` is a development instrument and should not reach users as it stands.
Removing it is not a plain revert: the history fixes above depend on helpers introduced
alongside it by `73521ef`, so the journal and the fixes have to be separated before either
can ship. `claude/history-gap-offscreen-tabs.md` in the Claude project records the options.

This is a smaller problem than the listener — a journal of metadata is not device-wide
notification access — but it is unresolved, and it is recorded here so that settling the
listener question is not mistaken for clearing the branch.

## Waiting for release — on `work/irc-read-timeout`

Branched from `502c11e`. One commit, not yet on `origin` as a pull request.

| Commit | Change |
|---|---|
| `0d7e9b1` | Bound the IRC socket read, so a half-open connection cannot silently freeze the chat |

Two ordering constraints apply, and git enforces neither.

The first is satisfied: this change must land after `70c1d3c`, which rewrites the same ten
lines of socket construction. Applying them in the opposite order conflicts; in this order
the merge is clean and places the read timeout after the handshake, where it belongs.

The second is not: this change deliberately causes mid-session reconnects, which is exactly
the case `b7535a3` repairs. Merged before it, every timeout would open a gap in the chat that
nothing recovers. `b7535a3` is on the diagnostics branch above, so this one cannot land until
that branch does — which means it is blocked behind the listener condition too.

## Test device

Not verified from the machine this file was written on, which has no access to the device.

A `stable release` build was produced on 2026-09-10 from the diagnostics branch, signed with
the release certificate, `versionCode 7`, and intended for the test device. Whether it was
installed is unrecorded. Because that build reports the same version as the published 5.5.0,
the only way to tell which one is running is behavioural: open the diagnostic journal and look
for `elevatedSpanMs` on an `fcm.notification.audio` line. If the field is present the newer
build is installed; if it is absent, it is not.

Update this section from the device, not from intent.
