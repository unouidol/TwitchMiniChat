# Release state

What users actually have, what is waiting to reach them, and what the test device is
running. `AGENTS.md` describes *how* to release; this file records *where things are*.

It exists because that question kept being re-derived from scratch — and answered wrongly
once, from a stale note — costing more time than keeping it written down.

**Every release updates this file in the same pull request that bumps the version.**
A change that reaches `main-v5` without appearing under "Waiting for release" is a change
nobody can account for later.

Last verified: 2026-09-09.

## Published — what users have

| | |
|---|---|
| Version | **5.5.0** (`versionCode 7`) |
| Tag | `v5.5.0` at `8478d82` |
| Published | 2026-08-31 |
| Assets | `TwitchMiniChat-Android-arm64-v8a.apk`, `TwitchMiniChat-Android-armeabi-v7a.apk`, `SHA256SUMS.txt` |
| Signer | `ST=Italy, L=Italy, CN=Unouidol` |

Asset names, tag shape and checksum file match the contract in `AGENTS.md`, so the public
download buttons on `https://tmc.ircminichat.party/` resolve. Verified 2026-09-03.

There is **no 5.5.1**: no tag, no commit, no version bump. Builds newer than 5.5.0 still
report `versionName 5.5.0` and `versionCode 7`, so they are indistinguishable from the
published one on a device. Calling such a build "5.5.1" in conversation is a convenient
shorthand and nothing more.

## Waiting for release — on `main-v5`, not published

Merged after the `v5.5.0` tag. None of this has reached users.

| Commit | Change |
|---|---|
| `f8ea482` | Encrypt locally stored Twitch credentials at rest |
| `a77b129` | Add "delete only this device" to Safety & Privacy — completes the app side of `/delete_device_data` |
| `7cf64f8` | Opt-out crash reporting |
| `71f9850` | Report the silent account storage failures |
| `502c11e` | Report the features that stop working without saying so |
| `a67cd37` | Document the release and distribution contract (docs only) |

## Waiting for release — on `work/history-diagnostics-observability`

Branched from `502c11e`. Installed on the test device only. Two sessions have been
committing here, which is why this table fell eight commits behind between 09-03 and
09-09; it is now rebuilt from `git log main-v5..HEAD`.

| Commit | Change |
|---|---|
| `73521ef` | Diagnostic journal: why the chat history backfill runs or is skipped |
| `7a57893` | Fix: recover the window an off-screen chat page cannot measure |
| `bb319d4` | Fix: retire the frozen notification channels (`_v5`) and delete the ones earlier versions left behind |
| `f16aab1` | Diagnostic journal: what happens to every push that arrives |
| `dc9f608` | This file (docs only) |
| `a4c124e` | Diagnostic journal: that a spawn happened, not only that a push arrived |
| `5ca53dc` | Diagnostic journal: the state Android was in when it chose not to alert |
| `af74bfb` | Diagnostic journal: how old the process was when a push arrived |
| `5e352df` | Fix: let a freshly started process settle before alerting |
| `5616371` | Diagnostic journal: whether a sound was actually played |
| `1cad63c` | Fix: stop alerting for spawns whose window has closed |
| `ae141be` | Fix: ask again for an hour the network refused to deliver |
| `570580e` | Fix: recover the offline window for a page that never resumes |
| `b7b43ea` | Diagnostic probe: ask the system whether it alerted, instead of inferring |
| `39a4763` | Fix: keep the account name and channel out of Logcat |
| `8371d27` | Fix: stop a failed read from erasing the stored accounts |

### Decisions required before this branch ships

**The diagnostic journal** is a development instrument and must not reach users as-is.
The history fixes depend on helpers introduced by `73521ef`, so removing the journal is
not a plain revert. See `claude/history-gap-offscreen-tabs.md` in the Claude project for
the options.

**`NotificationAlertProbeService`** (`b7b43ea`) is a `NotificationListenerService`. It
filters to this app's own PCG channels in code, but the permission the user grants to
enable it is device-wide notification access — the strongest thing this app has ever
asked for, in an application whose manifest otherwise requests only `INTERNET` and
`POST_NOTIFICATIONS`. It must not ship, and a comment in `AndroidManifest.xml` saying so
is the weakest possible guarantee: moving the service and its manifest entry into a
`src/debug/` source set would make shipping it impossible rather than merely discouraged.

**`8371d27` is independent of the diagnostics work** and touches no file the rest of this
branch touches. It is a data-loss fix and belongs on `main-v5` whether or not the journal
question is settled; it is here only because that is the branch that was checked out.
