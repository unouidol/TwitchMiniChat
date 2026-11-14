# Device round — 5.6.0

**One list, in order, for the 5.6.0 release round.** It brings together every check that earlier work
deferred to this round:
- `docs/manual-plan-5.5.2.md`, re-run on the final build;
- the stable flavour's chat checks, **BLOCKING**;
- 5c's manual checks and background measurement (`docs/manual-plan-5c-emotes.md`, M1 to M6 and B1);
- 5d's section M and its hands-on pass (`docs/manual-plan-5d-recyclerview.md`);
- the scroll-on-backfill case;
- the welcome page's phone checks from #110, including the update notice against a real newer
  release and the terms gate re-showing once;
- the policy pages from #111 and #112;
- the player page's browser identification;
- the change from #108.

Written on 2026-11-13, from those sources, before any of it runs. Nothing here has been run.

Each item says **Do**, **Look for** and **Failure**. An item whose evidence is absent has not
passed. A missing log line is a failure, not a pass with missing paperwork.

## Removed, and why

- **TalkBack, 5d's case 8: CLOSED BY READING, by the owner's ruling of 2026-11-09.** Its retry is
  cancelled. The reading closes that the dismissal runs once the list is activated. It does not
  close whether TalkBack can put its focus on empty space at all; that is reasoning, not proof
  (`RELEASE-STATE.md`, *5.6.0 in progress*).
- **The row taller than the list, the last part of 5d's case 6: SUBSUMED, by the owner's ruling of
  2026-11-10.** It is part of the defect that the chat list has no minimum height. The release notes
  name that defect as a known issue.

## The builds

| | Build | Made how | Used for |
|---|---|---|---|
| **R** | stable, release-signed | `:app:assembleStableRelease`, on the machine that holds the release signing configuration | everything on the stable app: parts 1 to 6 |
| **D** | dev, debug | `:app:assembleDevDebug`, from the same commit as R | parts 7 and 8 |

- **R and D come from one commit,** with a clean tree. Their labels read `Version 5.6.0 (build 9,
  <commit>)` and `Version 5.6.0-dev (build 9, <commit>)`, with the same commit. A `+` or `unknown`
  disqualifies the build.
- **That commit is `main-v5` after the release pull request merges** (Federico's ruling 1 of
  2026-11-13; `AGENTS.md`, release step 4). The APK's embedded commit is then on the primary
  branch, and it is the commit the tag will point at.
- **Why D is needed:**
  - **C3 and D3 need `run-as`.** The 5.5.2 plan writes their owed record with it, and `run-as` works
    only on a debuggable app. A release build is not debuggable: the `release` build type in
    `app/build.gradle.kts` sets no `isDebuggable`. This is read, not run.
  - **The plans for 5.5.2 and 5c are written against the dev package,** in their commands, their
    log filters and their pid readings.
  - **The browser identification needs a debuggable app** (part 8).
- **Never on the test phone:**
  - an uninstall of the stable app or of the dev app;
  - clearing the stable app's data. Its encrypted account store and its journal cannot be
    recreated.
  - A debug-signed stable build cannot replace the release-signed one without an uninstall (5d's
    plan, case 2), so no such build is made for this round.
- **D must carry the phone's debug certificate.** Otherwise `adb install -r` cannot replace the dev
  app in place. Before installing, compare `apksigner verify --print-certs` on D with the same on
  the dev APK the phone holds.
- **Both need the machine's untracked configuration,** which git cannot supply:
  - R needs the release signing configuration in `local.properties`;
  - R and D both need the per-flavour `google-services.json`.
  `RELEASE-STATE.md`, *5.6.0 release*, records which machine was seen to hold them.
- **R stays as built.** The artifacts verified in part 1 are the ones published. Never rebuild
  between that verification and publication.

## What the round needs

- **The test phone,** the OnePlus AC2003 of `RELEASE-STATE.md`, *Test device*. Its stable app must
  be the published 5.5.1, with at least one account signed in, before part 2.
- **Two Twitch accounts,** for the two-account cases.
- **No second device.** There is no second phone, and an emulator is not feasible (Federico,
  2026-11-13). Part 9 is therefore NOT DONE; see there.
- **Screenshots** stay local, outside the repository. A set for the user guide is taken during the
  round:
  - the chat page with the stream closed;
  - the Safety & Privacy menu;
  - the welcome page;
  - the update notice, if it can be shown. In this round it cannot (part 9).
- **Channels:**
  - one busy channel that sends animated emotes;
  - for the long cases, one at the history cap, where a page starts with 400 history rows and live
    messages take the timeline past 400.
- **A channel where a test message is acceptable.** Federico makes every send. A message sent in
  this round is public.
- **The backend registry,** for part 7, as the 5.5.2 plan describes.
- **Logcat.** Use Android Studio's Logcat panel, filtered by package and set to level Debug. The
  5.5.2 plan records why other methods failed.
  - The stable package is `com.fs.twitchminichat`.
  - The dev package's name starts with the same text, so check the package column of every line you
    read.
  - **The release build keeps its log lines:** the `release` build type has `isMinifyEnabled =
    false`. This is read, not run on a release build.
  - **Not tried here:** whether the panel's package filter follows an app that is not debuggable. If
    it shows nothing for R, stop and say so. Silence does not mean the code did not run.

---

## Part 1. Before anything is installed

### 1.1 Record the phone as it is

**Do.** Read the phone and write down what you read:
- both apps: `adb shell dumpsys package com.fs.twitchminichat`, and the same for
  `com.fs.twitchminichat.dev`; note `versionCode`, `versionName` and `lastUpdateTime`;
- the stable APK's sha256, with `sha256sum` on the path `pm path` gives;
- the accounts signed in on the stable app;
- these settings: `accelerometer_rotation`, TalkBack, `show_touches`, `stay_on_while_plugged_in`.

**Look for.** Stable 5.5.1 (8), sha256 `bf6c22c2…65aaf8`, the published arm64 APK. At least one
account signed in.

**Failure.** Any other stable build, or no account signed in. Stop: part 2 needs the published 5.5.1
underneath, with a signed-in account. If no account is signed in, sign one in on 5.5.1 first.

### 1.2 Verify R before it is installed (`AGENTS.md`, release step 5)

**Do.** Run these on both APKs of R:
- `apksigner verify --print-certs`;
- `aapt2 dump badging`, reading the `package` line;
- `sha256sum`.

**Look for.**
- The signer's SHA-256 is `aca1170e9dcac79abfa6db078a7a9903ab1f6a935745033c0752177d22e237ed`, the
  release certificate that `RELEASE-STATE.md` records for 5.5.1.
- The package is `com.fs.twitchminichat`.
- `versionCode` is `9` and `versionName` is `5.6.0`.

Record both sha256: they are the checksums that will be published.

**Failure.** The Android debug certificate, a `.dev` package, or any other version. Do not install.

---

## Part 2. The update in place, and its first launch

**These can be seen only once.** The first launch after the update decides the gate and the
welcome page, so nothing else may open the app before item 2.2.

### 2.1 Install R over the published 5.5.1 (release step 6)

**Before the install, as a step of its own:**
- Read the signing certificate of the stable APK on the phone, pulled off it.
- Compare it in full with R's certificate.
- **If they differ, stop.** An install would then need an uninstall, which is forbidden.
- Keep the pulled APK, with its sha256, as the way back.

**Do.** `adb install -r` R's arm64 APK. Read the package again.

**Look for.**
- `Success`.
- `5.6.0` (`9`), with a new `lastUpdateTime`.
- The sha256 on the phone is R's arm64 sha256.

**Failure.** `INSTALL_FAILED_UPDATE_INCOMPATIBLE` (another signer), any other refusal, or a sha256
that differs from R's. Stop.

### 2.2 The terms gate shows once (#111, plan 1; #110, item 4)

**Do.** Open the app.

**Look for.** The terms gate, "Before you continue", with the GitHub line: "TMC loads its stream
player page from GitHub, and at most once a day asks GitHub whether a newer version exists…".
Then tick "I have read and agree to the Terms of Use and Privacy Policy." and tap "Accept and
continue".

**Failure.** No gate: an acceptance of version 2 was counted. A gate without the GitHub line. A
crash.

### 2.3 The welcome page follows, with the chat connecting underneath (#110, item 1)

**Do.** Watch the page appear after Accept. Leave it open for 30 seconds, then tap Continue. Watch
Logcat, tags `TMC_WELCOME` and `TWITCH_IRC`.

**Look for.**
- "Welcome to Twitch Mini Chat", the first-launch page. An existing user upgrading from 5.5.1 has
  no record of a previous start, so the decision is `FIRST_LAUNCH` (`WelcomePolicy.kt:108-109`).
- Logcat shows `start decision=FIRST_LAUNCH`.
- Logcat shows `update check status=200 tagFound=true`. The latest release is still 5.5.1, older
  than 5.6.0, so no notice follows.
- The chat behind the page is connected and receives messages. `TWITCH_IRC` shows no line of the
  connection closing while the page is open.
- #110 states this check as "no `lifecycle.stop` in the journal". The journal is read by the
  diagnostics export.

**Failure.**
- No page, or the page "Update available".
- A disconnect, or an empty chat after Continue.
- A crash.

### 2.4 A restart shows neither (#111, plan 1)

**Do.** Force-stop the app and open it again.

**Look for.** No gate and no page. Logcat shows `start decision=none`.

**Failure.** Either the gate or the page shown again.

### 2.5 The in-place update on the owner's phone, as a whole

This item gets its own result, from 2.1 to 2.4 together:
- the stable app updated in place to 5.6.0;
- the terms gate shown again once (`CURRENT_TERMS_VERSION` 3);
- then the welcome page, shown once to an existing user;
- and not shown again at the next launch.

It is a real device check, and is reported as one. **It is not a substitute for 9.1, a brand-new
user's first launch, and must not be presented as one.** The phone had TMC, accounts and an
acceptance of version 2 before the update.

**PASS** only if 2.1 to 2.4 all passed. **FAIL** if any of them failed.

---

## Part 3. The stable flavour's chat — BLOCKING

The stable app has never run the new chat list (5d) before this round. Every user installs it. Any
failure here stops the release (5d's plan, case 2; `RELEASE-STATE.md`, *5.6.0 in progress*).

### 3.1 Launch, sign in, and a page on a busy channel

**Do.** Sign one account in: add one, or sign an existing one in again, through the browser and back
into the app. Open a page on a busy channel.

**Look for.** The sign-in returns to the app, through `ircminichat://auth`. The page shows the
history and then live messages.

**Failure.** A crash at launch or on the page. A sign-in that does not return to the app. A blank
list. A missing resource: a blank label, a missing icon, or text that shows a resource name.

### 3.2 Scroll and follow, and an animated emote

**Do.** At the bottom, watch new messages arrive. Scroll up, then come back down. Find an animated
emote.

**Look for.**
- At the bottom the list follows the new messages.
- Scrolled up, it stays put.
- The emote shows and moves.

**Failure.** A list that does not follow. A crash. An emote that never shows.

---

## Part 4. #108, on the stable app

#108's device step ran on the dev flavour on 2026-11-12. This is its first run on the stable app.

### 4.1 The stream controls and the status line

**Do.** Look at the chat page. Tap Show stream, then press play in the player.

**Look for.**
- The button reads SHOW STREAM, and HIDE STREAM once the stream shows, each on one line.
- No mute button from the app.
- The status line sits between the channel bar and the buttons row.
- The sound plays on play with no other touch.

**Failure.** The old label ("Video ON" or "Video OFF"). A mute button. The status line elsewhere.
No sound.

### 4.2 The player's own mute, and what is known not to survive

**Do.**
1. Mute in the player, then unmute.
2. Mute, then Hide stream and Show stream.
3. Mute, then switch channel.

**Look for.**
- The player's mute works both ways.
- After hide and show, and after a channel switch, the sound comes back. This is a known issue,
  named in the release notes.

**Failure.** A mute that does not work, or a crash. If the mute now survives either step, record
that, and correct the release notes' known issue.

### 4.3 Rotation with the stream on

**Do.** Turn auto-rotate on, with the stream showing. Rotate to landscape and back. Set auto-rotate
back to its value in 1.1.

**Look for.** No crash, and no `AndroidRuntime` line. The stream comes back off, a known issue that
predates #108.

**Failure.** A crash, including #103's `IllegalStateException … not attached to a context`.

---

## Part 5. The chat on the final build: 5c's M1 to M6 and 5d's section M

Run this part on R. For the long cases, use the channel at the history cap, with live messages
arriving, so that the timeline passes 400 rows. That is the regime no automated test covers in the
list itself (5d's plan, section M).

**Before:**
- Run `adb shell setprop log.tag.Engine VERBOSE`, read it back, then force-stop and reopen the app.
  Glide reads that property once, when its process starts.
- Logcat tags: `CHAT_LONG_PRESS`, `CHAT_ACTION`, `CHAT_FOCUS`, `CHAT_IME`, `TWITCH_IRC`,
  `TWITCH_EMOTES`, `TMC_HISTORY` and `Engine`.

**On a failure,** stop that item and record what was seen and when. Go on only if the app still runs
as before.

### 5.1 Emotes in live chat and in a reply (5c M1)

**Do.** On the busy channel, Federico sends a reply that contains an emote.

**Look for.** Animated emotes play in the rows on screen. Static ones show. The reply's emote
shows.

**Failure.** An invisible marker where an emote belongs, or an emote that never plays.

### 5.2 An echo with an emote, through CONFIRMED and a recycled rebind (5c M6; 5d case 4)

**Do.** Federico sends one message with an animated emote from the emote picker and watches it until
it is confirmed. Then fling far up, so that the echo's row is recycled. Wait 10 s, and come back to
the bottom.

**Look for.**
- "Sending…", with a status line, then CONFIRMED in place with no status line.
- `TWITCH_IRC: outgoing message confirmed`.
- After the return: one copy, still confirmed, with its emote moving.

**Failure.** Stuck at sending. Shown twice. Missing. Another row's content in its place. Its emote
missing.

### 5.3 A long-press and a swipe-to-reply on a recycled row (5d case 1)

**Do.** Fling far up, then long-press a message row. Read the dialog's title, choose Copy message,
paste it into the channel field without sending, and cancel. Swipe a row with an id from its right
edge to the left, read the reply bar, and cancel the reply. Do this on three rows at three scroll
positions, one of them just scrolled into view.
- **Choose menu items by their exact label,** read from the screen as it stands, never by position.
  The positions shift when a message has no id.
- **Never tap "Report message" or "Block user in app" here.** Both act at once, with no
  confirmation.

**Look for.**
- The title is `@` and that row's user.
- The pasted text is that row's text.
- The reply bar names that row's user.
- `CHAT_LONG_PRESS: message actions opened`.

**Failure.** A title, a copied text or a reply naming another row. No menu. A swipe that does not arm,
or leaves the row displaced.

### 5.4 Hide a user, then show them again, on a long list (5d case 3)

**Do.** Pick a user with several rows on screen. Long-press one, and choose Block user in app. Then
unblock them: Safety & Privacy › Blocked Users › Unblock.

**Look for.**
- Hidden: all that user's rows leave, and every other row keeps its order and place.
  `CHAT_ACTION: hidden-user rule updated`.
- Shown again: their rows return to their places, together.
- Intended, from #71: messages that arrived while they were hidden do not come back.

**Failure.** A hidden user's row still shown. Another user's row gone. Rows out of order, duplicated
or blank. A jump. A crash.

### 5.5 Following, the jump button, and far up and back (5d case 6 and section M; the tall row removed)

**Do.**
1. At the bottom, watch messages arrive.
2. Scroll up a little and wait for a few arrivals, few enough to count. Read the jump button's
   number just before you tap it, then tap.
3. Fling far up, to the first row, and back.

**Look for.**
- At the bottom the list follows.
- Scrolled up, the rows on screen stay where they are, and the jump button counts the new rows.
- The tap returns to the bottom and clears the count.
- Emotes come back in the rows the list rebinds, on the way down.

**Failure.** No following. A count that differs from the arrivals you counted. A tap that does not
return. A jump in the scroll.

**Why the count is read here:** 5d's pass left it UNCLEAR on 2026-11-09, because it could not be
counted.

### 5.6 Scrolled up while a history backfill lands: the scroll-on-backfill case

**No source by this name was found** in the repository on 2026-11-13. This item is built from:
- #66's description, *Checked, not changed*: rows inserted above the viewport shift what the user is
  reading, a pre-existing behaviour that became more frequent. History rows that arrive while the
  user is scrolled up are counted as unseen, including rows inserted above the viewport.
- 5d's harness check S3, `oneRowWentAboveIt`, which established for **one** row that the visible
  rows stay put. Several rows at once were not part of it.

**Do.**
1. On the busy channel, scroll up so that the jump button shows. Note the top row on screen.
2. Turn flight mode on for 60 seconds, then off.
3. Wait for `TMC_HISTORY: History loaded messageCount=… seconds=…`.

**Look for.**
- The row you noted is still where it was, or near enough to be read without scrolling.
- Record the line's `messageCount`, and the jump button's number before and after.
- A number larger than the rows below you is the over-counting that #66 records. It predates this
  release. Record it; it is not a failure by itself.

**Failure.** The rows you were reading jump away. Rows duplicated or out of order. A crash.

**If no `History loaded` line appears,** the case was not reached. Record it NOT REACHED; do not pass
it.

### 5.7 Swiping between the two accounts, and the detach boundary (5c M2; 5d case 7)

**Do.**
- Scroll an animated emote's row just out of view, and back.
- Swipe to the other account's page, wait 10 s, and swipe back.

**Look for.** Each emote plays again at once, in its own row. Record any `Started new load` lines
with an `emoticons` model while you swipe back. They are recorded, not a failure by themselves.

**Failure.** An emote frozen on screen. An emote animating in a row that is not its own. A blank or
missing emote.

### 5.8 Home for ten seconds, then back (5c M3)

**Do.** Press Home, wait 10 seconds, and reopen the app.

**Look for.** The emotes show and play again.

**Failure.** Emotes missing or frozen.

### 5.9 Rotation, and the keyboard (5c M4; 5d case 5)

**Do.** Turn auto-rotate on. With emotes on screen, rotate to landscape and back. Then open the
keyboard in the message field and rotate again. Set auto-rotate back to its value in 1.1.

**Look for.**
- No crash.
- Rows shown, in order.
- Emotes show and play.
- Record the keyboard's state after each turn.

**Failure.** A crash. `An emote session was released twice` or `A load was started for a released
emote session` in the log. A blank list.

### 5.10 A channel switch (5c M5)

**Do.** Switch the page to another channel, then back.

**Look for.** The old channel's rows are gone, with their emotes, and the new channel's emotes load.

**Failure.** A crash with either of the two emote-session messages in 5.9. Rows of the old channel in
the new one.

### 5.11 A tap on empty space clears the channel field's focus (5d case 9 and section M)

**Do.** Focus the channel field, so that the keyboard is open. Then tap:
- between rows;
- on a system line;
- on a short chat, below the last row.

**Look for.** Each tap closes the keyboard and clears the field's focus. `CHAT_FOCUS: composer focus
guard disarmed reason=outside_click`.

**Failure.** The keyboard stays open. The tap below the last row does nothing.

**Afterwards:** `adb shell setprop log.tag.Engine ""`.

---

## Part 6. The welcome page's other checks, and the policy pages

### 6.1 Safety & Privacy: About TMC and Bug report (#110, item 7)

**Do.** Open Safety & Privacy, then About TMC, then Bug report.

**Look for.**
- About TMC opens the page. It reads "You have version 5.6.0. No newer version was found at the last
  check."
- Bug report opens. It shows the version and names `#tmc-ticket`.
- Open Discord leaves the app for the invite link. Joining is not part of the check.

**Failure.** Either entry missing, or opening nothing. A wrong version. A crash.

### 6.2 Rotation with the page open (#110, item 5)

**Do.** Open About TMC. Turn auto-rotate on, rotate, and rotate back. Set auto-rotate back
afterwards.

**Look for.** The page comes back once after each turn.

**Failure.** It is gone, it shows twice, or the app crashes.

### 6.3 Every button is at least 48 dp (#110, item 8)

**Do.** With each page open, scroll it to the bottom, where its buttons are. Read the real view
frames with `adb shell dumpsys activity top`, not `uiautomator` bounds. Read each button's frame
in the dumped view hierarchy, and read the density with `adb shell wm density`.

**Look for.** Every button's height is at least 48 × density ÷ 160 px. At 382 dpi that is 115 px.

**Failure.** Any button below it.

**If the dump does not include the page's window,** the item is UNCLEAR. Record that; do not fall
back to `uiautomator`. The page is a dialog, in a window of its own.

### 6.4 The policy pages inside the app (#111, plan 3; #112)

**Do.** Open Safety & Privacy, then Privacy Policy, then Credits, then Data Deletion.

**Look for.**
- **Privacy Policy:** "Last updated: 2026-11-13". §4 names GitHub and Cloudflare. If #112 merged,
  §4 also names the Pokémon Community Game extension.
- **Credits:** the GitHub and Cloudflare lines.
- **Data Deletion:** "Last updated: 2026-09-28".

**Failure.** An older date, or a missing item. If an item is missing, the bundled pages and the
pages to be published disagree. Stop before publication.

---

## Part 7. The 5.5.2 round, re-run on the final build

**Run `docs/manual-plan-5.5.2.md` in full on D.** Do the parts in its order: A1 to A5, B1 to B5, C1
to C5, then the erases, D1 to D8, last. Follow its sections on naming, log capture, the backend
registry and injecting an owed record, exactly as written.
- **Install:** D goes over the phone's dev app with `adb install -r`.
- **The anchor:** the plan's retired `50c3069` check is replaced by the rule under *The builds*. D's
  label names R's commit, with no `+`.
- **Why D, not R:**
  - C3 and D3 need `run-as` (*The builds*).
  - Every command in that plan names the dev package.
  - The erases sign out every account of the app they run on. Run on R, they would erase the stable
    app's accounts and settings on the phone.
  - The deletion code has no branch by flavour: `app/src/main` reads no `BuildConfig.FLAVOR`,
    `DEBUG` or `BUILD_TYPE`. This is read, not run.
- **The erases run on D, never on the stable app** (Federico's ruling 5 of 2026-11-13).
- **What part 2 now settles:** the 5.5.2 plan lists "the true upgrade case" among what that round
  cannot check, because the dev flavour installs beside the release. Part 2 is that case.

### 7.1 No emote work after Home (5c B1)

**Do.** Run 5c's B1 on D, by its method and its void rules (`docs/manual-plan-5c-emotes.md`, *B.
The background measurement*). Run it before the erases, while two accounts are signed in.

**Look for.** 10 `Started new load` lines or fewer from D's pid, between 1.0 s and 30 s after Home.

**Failure.** 100 lines or more: the defect is back. Between 11 and 99 is inconclusive: stop and
report the count.


### 7.2 Decline on the gate, on D, after an erase (#111, plan 4)

**Do.** At the first start after the first erase of section D, the gate shows again: both resets
clear the acceptance (`SafetyPrivacyFragment.kt`, `TermsPrefs.clearAcceptance`). Tap "Decline and
close". Then open the app again.

**Look for.** "Decline and close" closes the app. Opened again, the gate shows again, and accepting
it lets the app continue.

**Failure.** The app stays open after Decline, or reopens past the gate without an acceptance.

**It stands for both flavours.** The gate is the same code in both. That is read, not run on the
stable app. On the stable app the gate is never declined in this round, so that its acceptance of
version 3 is made once, in 2.2.

---

## Part 8. The player page's browser identification

**Why it is checked:** the privacy policy says that the player page shows GitHub "the standard
identification of the app's built-in browser". It also says that neither request to GitHub includes
"anything that identifies your phone or this installation". The stream session runs GeckoView in
desktop user-agent mode (`GeckoSessionManager.kt:1691`). What string that produces on this phone
has not been read.

**Do, on D:**
1. Enable remote debugging through GeckoView's configuration file. This is GeckoView's documented
   method; it was not tried here.
   - Write `/data/local/tmp/com.fs.twitchminichat.dev-geckoview-config.yaml`, holding
     `prefs:` → `devtools.debugger.remote-enabled: true`.
   - Run `adb shell am set-debug-app --persistent com.fs.twitchminichat.dev`.
   - Force-stop and reopen the app.
2. Show a stream.
3. In Firefox on the computer, open `about:debugging` and connect to the phone over USB. Select the
   `player.html` tab, and evaluate `navigator.userAgent` in its console.
4. Record the string verbatim.
5. Undo, as a step of its own and only after the string is recorded: run `adb shell am
   clear-debug-app`, then delete the file.

**If the method does not work,** stop and report. Do not change the app's code for it.

**Look for.** A desktop browser string that names no phone model, no manufacturer and no Android
build.

**Failure.** The string names `AC2003`, `OnePlus`, the Android build, or anything particular to this
installation. Then the policy's sentence is false. Stop: the policy changes before the release.

**It stands for both flavours.** Both open the session through the same `getOrCreateSession`. That
is read, not run.

---

## Part 9. The checks that need a second device: NOT DONE

**There is no second phone, and an emulator is not feasible** (Federico, 2026-11-13). Both items
below are therefore **NOT DONE — no second device available; covered by unit tests only; risk
accepted by the owner.**
- **Neither is written as PASS, "verified" or "covered"** without that qualifier.
- **The release is not blocked by them.** Part 3 stays BLOCKING.
- **Never simulate them on the owner's phone.** That means no uninstall of either app, and no
  clearing of the stable app's data.

The tests named below were read from the source on 2026-11-13, at `app/src/test/`.

### 9.1 A fresh install's first launch: the terms gate, then the welcome page, for a brand-new user

**NOT DONE — no second device available; covered by unit tests only; risk accepted by the owner.**

**The tests that cover it:**
- `WelcomePolicyTest.firstLaunch_showsTheFirstLaunchPage`
- `WelcomePolicyTest.firstLaunch_winsOverAKnownUpdate`
- `WelcomePolicyTest.theStartGate_letsOneResumeDecide`
- `TermsVersionTest.noAcceptance_doesNotCount`
- `WelcomePageTextTest.firstLaunch`

**What they do not exercise on a real device:** they decide on the JVM, from stored values, which
page is due. They never run `MainActivity` on a phone that never had TMC, so nothing observes the
gate followed by the page on screen, or where Android 13's notification-permission prompt falls
between them.

**Part 2.5 does not replace it.** That is an existing user's update in place.

### 9.2 The update notice against a real newer release

**NOT DONE — no second device available; covered by unit tests only; risk accepted by the owner.**

**The tests that cover it:**
- `WelcomePolicyTest`:
  - `newerReleaseNotYetShown_showsTheNotice`
  - `newerReleaseAlreadyShown_showsNothing`
  - `showingTheNotice_recordsItsTag`
  - `aStartWithoutTheNotice_keepsTheDismissedTag`
  - `aLaterReleaseThanTheDismissedOne_showsTheNoticeAgain`
  - `updatedWhileALaterReleaseIsKnown_showsOnlyTheNotice`
  - `menu_namesANewerReleaseWheneverOneIsKnown_evenIfShown`
- `WelcomePageTextTest.updateAvailable_namesTheNewVersion`
- `ReleaseVersionTest`: `newerTag_isNewer`, `componentsCompareAsNumbers`, `olderTag_isNotNewer`
- `LatestReleaseResponseTest`: `realAnswer_givesItsTag`, `draftOrPreRelease_givesNothing`
- `ProjectLinksTest`: `theDownloadIsTheInstalledBuildsApk`, `theCheckAsksTheSameEndpointAsTheWebsite`

**What they do not exercise on a real device:** they hand the decision a tag and a fixed answer.
No test makes the real request to GitHub from a phone, draws the notice on screen, or opens the
download in the browser.

**Follow-up after the release:** observe the update notice on the owner's phone when the first
release after 5.6.0 is published.
- Never publish a fake or pre-release tag to trigger it.
- Never change `UPDATE_NOTICE_ENABLED`, or the release endpoint, to force it.

### 9.3 Also NOT DONE: an offline start when the check is due (#110, item 6)

**This is not a second-device item.** On the owner's phone the check is due again only a day after
2.3's. Clearing the stable app's data to make it due sooner is forbidden.

**NOT DONE in this round.**
- **Tests:** `WelcomePolicyTest.aFailedCheck_storesNothing_soTheStoredStateDecides`,
  `WelcomePolicyTest.check_isDueWhenNeverMade_andAtMostOnceADay` and
  `LatestReleaseResponseTest.anyStatusButOk_givesNothing`.
- **To observe it later:** open the app offline, at least 24 hours after 2.3. Look for one
  `update check failed: …` line and no notice.

**Decline on the gate (#111, plan 4)** was in this part on a second device. It moves to 7.2, on D.

---

## Part 10. The end of the round

**Do.**
- Set back every setting recorded in 1.1 that changed.
- Read both apps' package, version, `lastUpdateTime` and sha256 off the phone, and write them into
  `RELEASE-STATE.md`, *Test device*. Write it from the device, not from intent.
- No `device-test/` tag is needed: R and D are built from `main-v5`, which keeps their commit. The
  release tag, at publication, points at it.

**Report** each item as PASS, FAIL, UNCLEAR or NOT REACHED, with its time and its log lines.

---

## Results — run on 2026-11-13 and 2026-11-14

Run on the OnePlus AC2003 (Android 12) with R and D built at `5ae4bf2`: R arm64-v8a
`fd06d29e…f0745ff3`, D arm64-v8a `a754cf4f…ba9eeb2`. The detailed record, with times and log lines,
stays local in the round folder and is not part of this repository. It holds no account or channel
names. Only verdicts are listed here.

**NOT DONE**
- **9.1** A fresh install's first launch — NOT DONE — no second device available; covered by unit
  tests only; risk accepted by the owner.
- **9.2** The update notice against a real newer release — NOT DONE — no second device available;
  covered by unit tests only; risk accepted by the owner.
- **9.3** An offline start when the check is due — NOT DONE: the check was not due again before
  the round ended, and clearing the stable app's data is forbidden.
- **5.5.2 D5** A failed browser clear no longer cancels the erase — NOT DONE: the browser clear
  cannot be made to fail without changing the code (Federico's ruling).
- **5.5.2 D7, its browser-failure half** — NOT DONE, for the same reason.

**Not passed**
- **5.6** Scrolled up while a history backfill lands — NOT REACHED. After a network loss the chat
  did not reconnect by itself, so no backfill landed. That is defect F1 below.

**PASS**
- **Part 1:** 1.1, 1.2.
- **Part 2:** 2.1, 2.2, 2.3, 2.4, 2.5.
- **Part 3 (BLOCKING):** 3.1, 3.2.
- **Part 4:**
  - 4.1;
  - 4.2, with its known issue confirmed;
  - 4.3, with its known issue.
- **Part 5:** 5.1, 5.2, 5.3, 5.4 (Federico's one-time exception), 5.5, 5.7, 5.8, 5.9, 5.10, 5.11.
- **Part 6:** 6.1, 6.2, 6.4, and 6.3. 6.3 was UNCLEAR at first, because the dialogs' windows are
  absent from `dumpsys activity top`. It was redone with `uiautomator dump` and passed: every
  button in both dialogs is 115 px, the 48 dp threshold at this density.
- **Part 7, D's install and first launch.**
- **Part 7, 5.5.2 A:** A1, A2, A3, A4, A5.
- **Part 7, 5.5.2 B:** B1, B2, B3, B4, B5. B2 used Federico's approved injection for its start-up
  pass.
- **Part 7, 5.5.2 C:** C1, C2, C3, C4, C5. C3 and C5 were injected, as the plan prescribes.
- **Part 7, 5.5.2 D:** D1, D2, D3, D4, D6, D7 (online), D8. D3 was injected. D3 and D8 are
  blocking.
- **Part 7, the rest:**
  - 7.1: run 1 VOID because its positive control fell short; run 2 PASS;
  - 7.2.
- **Part 8:** the player page's user agent is
  `Mozilla/5.0 (X11; Linux x86_64; rv:145.0) Gecko/20100101 Firefox/145.0`. It names no phone
  model, no manufacturer and no Android build.
- **Part 10:** every setting is as recorded in 1.1, and both apps' versions and sha256 were read
  from the device.

**Defects found, none blocking by Federico's rulings**
- **F1.** After a period without network the chat does not reconnect by itself. This was already
  the case in 5.5.1. The workaround is to close the app and open it again. It is a known issue in
  the release notes.
- **F2.** Long outcome messages are toasts, and Android 12 cuts them off at two lines, including
  the ones that say whether alerts can still arrive. It is a known issue in the release notes.
- **F3.** Owed server work can start twice in parallel: the start-up attempt and the worker
  overlapped. The calls succeeded and are idempotent.
- **F4.** After "Delete app account and all data", the profile remained in another registry record
  of the same phone. An earlier offline erase had left that record behind.

**Follow-ups**
- The Data Deletion page says "before 5.5.2 there was a second option". 5.5.2 was never published,
  so at the next change to the policy pages it should read "before 5.6.0", in both copies.
- How to render the animated GIF emotes that tier-2 subscribers send in chat.
