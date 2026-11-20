# Release notes for 5.6.1

**This is the draft body of the `v5.6.1` release. Federico has not approved it yet.** The release
body is everything between the two markers below, published exactly as it stands. Nothing
outside the markers belongs in the release. The two checksums are placeholders until the
artifacts are verified. They are then filled from those artifacts, never from a rebuild.

**What it was written from.** The pull requests merged on `main-v5` since `v5.6.0`, read on
2026-11-20: `git log --first-parent v5.6.0..main-v5`, up to `f07fd27` (#131). That is the bodies
of #123, #127, #129, #130 and #131. #131 makes the Smart Catch panel match by species, as the
missing-dex alerts have done since the backend change deployed on 2026-11-19.

**Rulings that still hold:**
- **No speed figures** (Federico's ruling 6, 2026-11-13).
- **The reconnect known issue stays** (Federico's ruling 3, 2026-11-13). The next section rechecks
  it against the code.

**The 5.6.0 known issues, checked against the code at `f07fd27`.** Each check reads the diff
from `v5.6.0`. None of this was run.
- **Removed: the chat stops reconnecting after a network drop.** #123 fixes it. Its device trials
  on the dev app passed on 2026-11-17, and this round's item 3 checks it on the stable build.
- **Narrowed: longer messages are cut short.** #127 shortened every toast it names, so that each
  fits two lines at the owner's settings and at factory settings. The message that follows
  removing an account without network is now a dialog. At a larger font size (#127's "big"
  column: 450 dpi, font scale 1.15), five texts still take three lines: the three erase outcomes,
  `account_add_failed` and `backend_session_persist_failed`. #124 tracks the erase outcomes. The
  known issue now names only those cases.
- **Kept: a few messages briefly missing after a reconnect.** #123 did not change how missed
  messages are recovered.
  - No commit since `v5.6.0` touches `HistoryBackfillPolicy.kt`, `HistoryBackfillState.kt` or
    `BackendHistoryClient.kt`.
  - In `ChatFragment.kt`, no changed line touches the backfill. The request made with a
    connection is the same call at the same point, `applyBackfillDecision(c,
    HistoryBackfillPolicy::onConnect)` at the start of `connectIfNeeded()`.
  - What #123 changes is when that point is reached. After a network loss, its retries now
    reconnect, where 5.6.0 stopped at "Could not authenticate". So the reconnect this known issue
    describes now also follows a network loss. Its own body says it did not touch "the
    history/missed-messages path".
- **Kept: the player's mute is not remembered; the chat can be squeezed; rotating hides the
  stream.** No layout and no stream player file changed (`chat/StreamPlayerUrl.kt` included). No
  changed line of `ChatFragment.kt` mentions the stream, the player, mute, rotation or the
  keyboard.
- **Kept: an alert may play its sound twice.** No file of the alert path changed:
  `MyFirebaseMessagingService.kt`, `NotificationAlertFallback*.kt`, the `PcgProfileAlert*` and
  `PcgSpawnAlert*` files and `pcg/PcgNotification*.kt` are as in `v5.6.0`. The only change in
  `FcmRegistrationUploader.kt` is one toast's duration, from #129.

**Added by Federico's ruling of 2026-11-20: #126.** After a failed account removal, the toast "Could
not remove this account." can be followed by the "Account removed" dialog. The dialog is new in
5.6.1 (#127), so the pair can first appear in this release. What is true in that case was read in
the code at `a11a3a0`, and not run.
- **When the pair appears.** `AccountRepository.removeById()` returns null, so `removedAccount` is
  false and the toast is `account_remove_failed`. Then the backend request that switches the
  profile's alerts off fails, and `onAlertDisableOwed` shows the dialog. Before `removeById()`,
  `AccountProfileRemovalController.removeAccountFromDevice()` has already set the profile's alerts
  off locally and cleared its local data, the Pokédex snapshot, inventory, presets, buddy, alert
  settings and Most Wanted.
- **`removeById()` returns null in two cases,** and they differ.
  - **The account id is no longer in the stored list.** The account is already gone from the
    phone, so the dialog is right and the toast is not. The owed-work queue does not find it in
    `loadAccounts()`, so `OwedProfileDisablePolicy` answers `DISABLE`. The disable is retried
    until the server confirms, and then the alerts stop.
  - **The stored list cannot be read for update** (`loadAccountsForUpdate()` returns null).
    Nothing is written, so the account stays stored. It is not removed, and the toast is right.
    `loadAccounts()` reads an unreadable store as an empty list, so the account is missing from the
    page until the list can be read again. When it is listed again, its alert settings are the
    defaults: `PcgSpawnAlertModeStore.clearProfile()` removed the saved mode, and `getMode()` then
    returns `PcgSpawnAlertMode.DEFAULT`, `DEX_AND_TIER_A`. The next start's registration pass,
    `MainActivity.fetchFcmToken()`, then sends that active selection for it. So its alerts can keep
    arriving. Any owed disable is then voided as `VOID_SIGNED_IN` and never sent. Removing it again
    with a network connection switches them off.
- **The user-facing text** says that the account's data on this phone are deleted either way. It
  tells the two cases apart by what the user sees in the list, and promises no fix date.

**Not added, by the same ruling: #122.** A 426 from the token step shows "Could not authenticate"
instead of asking to update TMC. The backend never sends 426 today, so no user can see it.

<!-- release body begins -->

## What is Twitch Mini Chat?

Twitch Mini Chat (TMC) is an Android app that combines a lightweight Twitch client with companion tools for the Pokémon Community Game (PCG) extension, built for people who use several accounts at once.

## What's new in 5.6.1?

5.6.1 makes the chat reconnect on its own after a network drop, makes longer messages readable, and makes the Smart Catch panel agree with the missing-dex alerts. It installs over your current version and keeps your accounts and settings.

**Chat**
- **The chat reconnects on its own after a network drop.** While the chat is open, TMC keeps trying during the outage and reconnects shortly after the network is back, without you touching the app. Meanwhile the status line reads **Connection lost. Reconnecting…**. To notice that the network is back, TMC uses Android's network-state permission. Android grants it at install without asking, and it only tells TMC whether a network is available.

**Messages**
- **Longer messages are no longer cut short.** The messages shown after an erase, after a login that could not be saved, and when **Update Pokédex** is pressed on the wrong tab have been shortened to fit.
- **After you remove an account without network**, a dialog now tells you that alerts for that account may still arrive for a while. TMC keeps trying to switch them off on its own, and stops when the server confirms.
- **If the built-in browser's data cannot be cleared during an erase**, TMC says so in a message of its own, after the result of the erase. The erase still completes.
- **Messages that take more than one line stay on screen longer.** Examples are a message that could not be sent, a user blocked in the app, the result of a Pokédex list update, and a stopped account deletion.

**Smart Catch**
- **The Smart Catch panel now agrees with the missing-dex alerts for every form.** The main Pokédex has one entry per species, and catching any form registers it. The panel now judges a spawn by its species, as the alerts do. Any form of a species you are missing shows **Dex entry: Missing**, and Repeat Ball is not suggested for it. That covers female forms, Vivillon patterns, Unown letters, and regional and Mega forms. A form of a species you have registered shows **Dex entry: Registered**. **Dex entry: Unknown** now appears only where TMC cannot tell: before you have updated your Pokédex in TMC, or for a Pokémon that does not normally spawn.
- **After a Pokédex reset, trust TMC's missing-dex alert and the panel.** `!pokecheck` on a form answers for the alt dex, which a reset does not clear.

**Privacy and your data**
- **The Data Deletion page** now says that the removed option *Erase everything on this device* existed before 5.6.0, not before 5.5.2. 5.5.2 was never published. The page is updated (2026-11-18). The terms screen does not appear again.

## Known issues

- **A mute you set in the player is not remembered.** After you hide and show the stream, or switch channel, the stream plays with sound again.
- **The chat can be squeezed.** In landscape, or with the stream and the keyboard both open, the chat gets very little room. Holding the phone upright with the stream hidden gives it its full height.
- **Rotating the phone hides the stream.** Tap **Show stream** again.
- When a Pokémon you are missing spawns for several of your accounts at once, you get one alert per account, as intended. Now and then one of those alerts may play its sound twice. It is harmless. It was not fixed in this update.
- Right after the chat reconnects, a few messages may be briefly missing. They appear when you switch to another account and back.
- **With a larger font size, some messages can still be cut short.** On Android 12 and later, the short message that appears at the bottom of the screen after an erase, or after a login that could not be saved, can be cut off after two lines.
- **Removing an account can show "Could not remove this account." and then "Account removed".** This is rare: it needs the removal to fail on the phone while the TMC server cannot be reached. The account's data on this phone are deleted either way. If the account is no longer in your list, it is removed, and TMC keeps trying to switch its alerts off on its own, as the dialog says. If it shows up in your list again later, it was not removed: its alert settings are back to their defaults, and its alerts may keep arriving. Remove it again while you have a network connection.

## What to download

- For almost every modern Android phone: `TwitchMiniChat-Android-arm64-v8a.apk`
- Only for older 32-bit phones: `TwitchMiniChat-Android-armeabi-v7a.apk`

Requires Android 8.0 or later. SHA-256 checksums are in `SHA256SUMS.txt`:

```
<arm64-v8a sha256, filled from the verified artifact>  TwitchMiniChat-Android-arm64-v8a.apk
<armeabi-v7a sha256, filled from the verified artifact>  TwitchMiniChat-Android-armeabi-v7a.apk
```

## How to install

1. Download the APK on your phone.
2. If Android asks, allow your browser or file manager to install unknown apps.
3. Open the APK from your Downloads folder and tap **Update** (or **Install** on a first install). Your accounts and settings are kept.
4. On first opening after the update, the welcome page appears once. The terms screen does not appear again.

## How to use

Sign in with Twitch to use the chat, then sign in again inside the app to open the PCG extension. The two logins serve different purposes: one connects the chat, the other the extension.

## Source code

The complete source code is in this repository. This release is under the `v5.6.1` tag.

## Disclaimer

TMC is an independent, unofficial project with no affiliation to Twitch or the Pokémon Community Game, which belong to their respective owners. TMC never catches automatically: every chat command or gameplay action is triggered by you, one tap at a time.

## Contacts

- For bug reports, feedback or suggestions, join the [TMC Discord server](https://discord.gg/E8yRUp2MBK)
- You can also find me on Twitch: [twitch.tv/unouidol](https://www.twitch.tv/unouidol)
- Business inquiries: unouidol.interactive@gmail.com
- [Privacy Policy](https://unouidol.github.io/tmc/privacy.html) · [Data Deletion](https://unouidol.github.io/tmc/data-deletion.html)

<!-- release body ends -->
