# Device round — 5.6.1

**One short list, in order, for the 5.6.1 release round,** on the OnePlus AC2003 (Android 12), with
the phone connected to `nbpromec1`. It checks:
- the update in place from the published 5.6.0;
- the stable flavour's chat, **BLOCKING**;
- F1's fix (#123) on the stable build;
- the Smart Catch panel by species (#131);
- the Data Deletion page's correction (#130).

Written on 2026-11-20, from the release request and the pull requests it names, before any of it
runs. Nothing here has been run.

Each item says **Do**, **Look for** and **Failure**. An item whose evidence is absent has not
passed. A missing log line is a failure, not a pass with missing paperwork. Each item is reported
as PASS, FAIL, UNCLEAR, NOT REACHED or NOT DONE, with its time and its log lines.

## The builds

| | Build | Made how | Used for |
|---|---|---|---|
| **R** | stable, release-signed | `:app:assembleStableRelease`, on `nbpromec1`, which holds the release signing configuration | items 1 to 5 |
| **D** | dev, debug | `:app:assembleDevDebug`, from the same commit as R | installed over the dev app; not exercised further in this round |

- **R and D come from one commit,** `main-v5` after the release pull request merges, with a clean
  tree before and after the build (`git status --porcelain --untracked-files=all` prints nothing).
  Their code carries that commit with no `+`. A `+` or `unknown` disqualifies the build.
- **R stays as built.** The artifacts verified before the round are the ones published. Never
  rebuild between that verification and publication.

## Rules for the whole round

- **Never** uninstall the stable or the dev app, or clear their data. Never read
  `no_backup/accounts_v1`.
- **The owner performs every chat send.** Nobody taps "Report message", "Block user in app",
  "Update Inventory" or "Update Pokédex".
- **No screenshot of the stable app's accounts page.** When one is needed, it is taken on the dev
  app.
- **No account names, channel names or message text** in the record, in this file or in the
  release. Log lines are copied only when they hold none.
- **Logcat:** the buffer is raised to 16M with the owner's consent. Its size is read before and
  restored at the end. Lines are read for the stable app's process only.
- **The detailed record** stays local on `nbpromec1`, in the folder `TMC-5.6.1-round` beside the
  clone, with the artifacts and their `SHA256SUMS.txt`. Only verdicts come back here.

## Before item 1

1. **Record the phone.** For both packages, read the version, `firstInstallTime`, `lastUpdateTime`,
   and the sha256 of the APK at the path `pm path` gives. Also read the Logcat buffer size.
2. **Keep the way back.** Pull the stable app's installed APK. Its sha256 must equal the published
   `v5.6.0` arm64 asset, `fd06d29e470337465f964841cce8f58551fe1497a45792daa5a8e4fbf0745ff3`.
3. **Compare certificates, as a step of its own.**
   - R's full signing certificate must equal the one of the stable APK pulled from the phone.
   - D's must equal the one of the dev APK on the phone.
   - **If either differs, stop:** an install would need an uninstall, which is forbidden.
4. **Install D over the dev app** with `adb install -r`. Look for `Success`, `5.6.1-dev` (`10`), and
   on the phone D's sha256 with `firstInstallTime` unchanged.

Nothing may open the stable app between item 1's install and its first launch.

## 1. The stable app updates in place to 5.6.1

**Do.**
1. Install R's arm64 APK over the stable app with `adb install -r`, and read the package again.
2. The owner opens the app.
3. He reads the page, then taps Continue.
4. Force-stop the app, and the owner opens it again.

Logcat tag `TMC_WELCOME`.

**Look for.**
- `Success`. The package reports `5.6.1` (`10`), with a new `lastUpdateTime` and `firstInstallTime`
  unchanged. On the phone, the sha256 is R's arm64 sha256.
- **The terms gate does not show,** because `CURRENT_TERMS_VERSION` is still 3.
- **At the first launch:** the page "Welcome to Twitch Mini Chat 5.6.1", and
  `start decision=UPDATED` in the log. The phone last ran 5.6.0 (`versionCode` 9), and the latest
  release is `v5.6.0`, which is not newer, so no "Update available" page appears.
- **At the second launch:** no page, and `start decision=none`.
- **A version check** runs only if 24 hours have passed since the last one. If it runs, it logs
  `update check status=200 tagFound=true` and no notice follows. If it does not run, this is
  recorded, and it is not a failure.

**Failure.**
- Any refusal, `INSTALL_FAILED_UPDATE_INCOMPATIBLE` included, or a sha256 other than R's. **Stop.**
- The terms gate, no page, or the page "Update available" at the first launch.
- A page at the second launch.
- A crash.

## 2. The stable flavour's chat — BLOCKING

**A failure here stops the round.** R and D become void, and the fix goes through a pull request
of its own.

**Do.**
1. The owner opens an account on a channel where people are chatting.
2. He sends one message.
3. He switches to another channel, and back.
4. He taps Show stream, then Hide stream.

Logcat tag `TWITCH_IRC`.

**Look for.**
- **Live messages arrive,** as the owner sees them, after `session ready generation=N`.
- **The sent message** appears as the owner's own, and `outgoing message confirmed` is logged.
- **After each channel switch,** a new `session ready` with a higher generation, and live messages
  on that channel.
- **The stream** shows and plays, then hides. The chat keeps receiving throughout.

**Failure.**
- No live messages.
- The message is not shown, or not confirmed.
- "Could not authenticate" or "Session expired" in the status line.
- A channel that stays silent after the switch, or a stream that does not show or hide.
- A crash.

## 3. F1 on the stable build: the chat reconnects by itself

**Do.**
1. With the chat open and in front, the owner turns airplane mode on.
2. About 30 s later he turns it off.
3. He does not touch the app until the verdict.

While this runs, the phone is polled once a second over USB for `settings get global
airplane_mode_on` and for a default network in `dumpsys connectivity`. **T0** is the first second
with airplane mode off and a default network.

**Look for.**
- **`session ready` within 20 s of T0,** with no touch.
- **During the outage,** the status line reads "Connection lost. Reconnecting…", and never "Could
  not authenticate". The owner reports it.
- **The cycle goes on:** `token step result=transient cause=network`,
  `reconnect scheduled … reason=token_transient`, then `network available awaitingRetry=…` and
  `reconnect start reason=network`.

**Failure.**
- No `session ready` within 20 s of T0.
- "Could not authenticate" at any point.
- A touch was needed.

## 4. The Smart Catch panel, by species

The panel reads the Pokédex snapshot already saved on the phone. **No Update Pokédex in this
round.**

**Do.** If a spawn happens during the round, the owner opens the Smart Catch panel for the account
in front. He reports:
- the spawn's name;
- the dex line, which is one of "Dex entry: Registered", "Dex entry: Missing" or "Dex entry:
  Unknown";
- the rows under Smart presets;
- whether that account received TMC's missing-dex alert for this spawn.

**Look for, by #131's rule:**
- **"Dex entry: Missing"** when the spawn's species is among the missing entries saved, whatever
  its form. There is no Repeat Ball row among Smart presets. If the account received TMC's
  missing-dex alert for this spawn, the line must read Missing: the alerts match by the same rule.
- **"Dex entry: Registered"** only for a species that normally spawns and is not among the missing
  entries. A Repeat Ball row is among Smart presets.
- **"Dex entry: Unknown"** only when TMC cannot tell: no snapshot is saved for the account, or the
  species does not normally spawn and is not listed missing. There is no Repeat Ball row.
- **If the spawn is a form,** such as a female or regional form, a pattern, a letter or a Mega,
  that is the case to record. The species is the same as the base Pokémon's.

**Failure.**
- A dex line that contradicts the rule, or a missing-dex alert with a line other than Missing.
- A Repeat Ball row with a line other than Registered, or none with Registered.

**If no spawn happens,** the item is NOT REACHED, with the reason. #131's unit tests cover the
rule.

## 5. Safety & Privacy, and the Data Deletion page

**Do.** The owner opens Safety & Privacy, then the Data Deletion page.

**Look for.**
- "Last updated: 2026-11-18".
- "before 5.6.0" in both of its places: "…earlier versions: before 5.6.0 there…" and
  "…"Erase everything on this device" in a version before 5.6.0…".
- No "5.5.2" anywhere on the page.

**Failure.** The old date, "before 5.5.2", or the page not opening.

## 6. NOT DONE, with the reason

- **The update notice from 5.6.0 to 5.6.1.** It cannot be observed on the owner's phone: its stable
  app is updated to 5.6.1 in this round, before the release is published, so it never runs 5.6.0
  while 5.6.1 is the latest release. It is to be confirmed by users after the release.
- **The account removal dialog and the erase toasts.** Both are destructive. They are covered by
  unit tests and by #127's device checks on the dev app.

## The end of the round

**Do.**
1. Restore the Logcat buffer to the size read before item 1.
2. For both apps, read the package, version, `firstInstallTime`, `lastUpdateTime` and sha256 off
   the phone, for `RELEASE-STATE.md`, *Test device*. Write it from the device, not from intent.

No `device-test/` tag is needed: R and D are built from `main-v5`, which keeps their commit. The
release tag points at it.

---

## Results

Not run yet.
