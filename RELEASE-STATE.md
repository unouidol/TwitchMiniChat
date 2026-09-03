# Release state

What users actually have, what is waiting to reach them, and what the test device is
running. `AGENTS.md` describes *how* to release; this file records *where things are*.

It exists because that question kept being re-derived from scratch — and answered wrongly
once, from a stale note — costing more time than keeping it written down.

**Every release updates this file in the same pull request that bumps the version.**
A change that reaches `main-v5` without appearing under "Waiting for release" is a change
nobody can account for later.

Last verified: 2026-09-03.

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

Branched from `502c11e`. Installed on the test device only.

| Commit | Change |
|---|---|
| `73521ef` | Diagnostic journal: why the chat history backfill runs or is skipped |
| `7a57893` | Fix: recover the window an off-screen chat page cannot measure |
| `bb319d4` | Fix: retire the frozen notification channels (`_v5`) and delete the ones earlier versions left behind |
| `f16aab1` | Diagnostic journal: what happens to every push that arrives |

**Decision required before this branch ships**: the diagnostic journal is a development
instrument and must not reach users as-is. The two fixes depend on helpers introduced by
`73521ef`, so removing the journal is not a plain revert. See
`claude/history-gap-offscreen-tabs.md` in the Claude project for the options.

## Test device

`AC2003` (ColorOS, Android 12), serial `57ebfa06`. Running a `stableRelease` build of
`work/history-diagnostics-observability` — reports `5.5.0 / versionCode 7`, same as the
published build, with all of the above on top.

Collecting diagnostic journals for two open investigations: chat history gaps, and spawn
alerts arriving late or silently. Do not reinstall while collection is in progress.

## Next release

`versionCode` **must** rise above 7: Android refuses to update a build whose `versionCode`
is not higher, so a release that keeps 7 cannot reach anyone who installed 5.5.0.
Follow the procedure in `AGENTS.md`.

## How to rebuild this file from the repository

Nothing here is remembered; all of it is derivable in under a minute. Re-derive rather than
trust, and correct the file when it disagrees.

```sh
git fetch --all --tags
git log --oneline v5.5.0..origin/main-v5                       # waiting on main-v5
git log --oneline origin/main-v5..origin/<feature-branch>       # waiting on a branch
grep -n "versionCode\|versionName" app/build.gradle.kts         # current version
```

Published releases and their exact asset names are visible at
`https://github.com/unouidol/TwitchMiniChat/releases`. The device build is identified with
`adb shell dumpsys package com.fs.twitchminichat | grep version`.
