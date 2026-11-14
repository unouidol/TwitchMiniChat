# Release notes for 5.6.0

**This is the body of the `v5.6.0` release, approved by Federico on 2026-11-14.** The release
body is everything between the two markers below, published exactly as it stands. Nothing
outside the markers belongs in the release.

**What it was written from.** The pull requests merged on `main-v5` since `v5.5.1`, read on
2026-11-13: `git log --first-parent v5.5.1..main-v5`, up to `3269e43` (#111), and the bodies of
#49 to #58, #66 to #69, #73, #78, #89, #94, #97, #103, #108, #110 and #111, and #112, merged as
`2771c6c`. #112 adds the Pokémon Community Game statement to §4 of the privacy policy, and §2's
alert settings, watchlists and phone model.

**Rulings that still hold:**
- **The reconnect known issue stays** (Federico's ruling 3, 2026-11-13). "A few messages may be
  briefly missing right after the chat reconnects" was a known issue of 5.5.1. #66 and #67 changed
  how missed messages are recovered, and neither says that it fixes this case.
- **No speed figures** (Federico's ruling 6, 2026-11-13). The start-up measurement of the new chat
  list was taken on debug builds of the dev flavour, on one phone. Its own record supports "the
  ingestion window fell by X on these builds", and not "users see X less on a release build"
  (`RELEASE-STATE.md`, *Closing start-up measurement of the conversion*).

**What changed from the draft, and the device round item behind each change.** The round is
recorded in `docs/manual-plan-5.6.0.md`, *Results*.
- **The two checksums** are those of the artifacts verified and installed in the round, hashed
  again on 2026-11-14 before publication. They are not from a rebuild.
- **"Missed messages are recovered more reliably"** now says that TMC asks again "the next time it
  loads the recent chat". The draft said only that it asks again. That could read as a promise
  the round denies: in 5.6 and C4 the chat did not reconnect by itself after a network loss. The
  timing comes from #66's description, read and not run.
- **"One erase instead of two"** no longer says that TMC finishes "removing this device from the
  server" when the network is back. In D2, offline, only the push token deletion was left owed,
  and it completed once the network returned. The device registration stayed on the server (the
  round's registry read R8). The Data Deletion page explains that case, and the bullet points to it.
- **"Removing an account…"** now says that alerts for that account may still arrive until the
  server can be reached. That is what C1's second toast says. C2 and C4 show the switch-off
  completing once the network is back.
- **New known issue: the chat does not reconnect by itself after a period without network.** This
  is defect F1 of the round, seen in 5.6 and C4. The only workaround the round observed is closing
  TMC and opening it again.
- **New known issue: longer messages are cut short.** This is defect F2 of the round, seen in D2's
  screenshot and D8. Toasts on Android 12 and later show at most two lines; that limit comes from
  Android's documentation, and the round saw it on Android 12. The sentence on what the cut-off
  part says is taken from the strings `erase_device_nothing_reached` and
  `account_remove_alerts_owed`.

<!-- release body begins -->

## What is Twitch Mini Chat?

Twitch Mini Chat (TMC) is an Android app that combines a lightweight Twitch client with companion tools for the Pokémon Community Game (PCG) extension, built for people who use several accounts at once.

## What's new in 5.6.0?

5.6.0 rebuilds the chat, adds a welcome page and an update notice, and makes deleting your data do exactly what it says. It installs over your current version and keeps your accounts and settings.

**Chat**
- **The chat list has been rebuilt, and a channel opens faster** when its recent messages load. The list now reuses the rows that scroll off screen, instead of keeping every message you have received ready to show.
- **Starting the app no longer stalls on old spawn messages.** When the recent chat held a Pokémon spawn that was already over, TMC loaded its whole Pokémon catalogue to read it. Spawns that are over are now skipped before that.
- **Animated emotes stop when nobody can see them:** when their message scrolls out of view, and when the app is in the background. They start again when you come back.
- **Missed messages are recovered more reliably.** If loading the recent chat fails, for example because the network dropped, TMC asks again for what it missed the next time it loads the recent chat, instead of assuming it arrived. Messages from a channel you have just left no longer appear in the one you switched to.
- **Fixed a crash when rotating the phone** shortly after hiding the stream.
- The status of a message being sent reads **Sending…** again, instead of "Sending???".

**Stream**
- **The stream button now reads Show stream / Hide stream.**
- **TMC's own mute button is gone.** Use the mute control of the Twitch player itself. The stream now starts with sound when you press play.
- **The status line has moved** between the channel bar and the buttons, which leaves more room for the chat.

**Welcome page and updates**
- **A welcome page** appears the first time you open TMC and once after each update. It links to the TMC Discord server and to Ko-fi, and says where to report a bug. You can open it again from *Safety & Privacy* → **About TMC**.
- **Bug report**, in *Safety & Privacy*, tells you how to report a problem on the TMC Discord server, in the #tmc-ticket channel.
- **Update notice.** At most once a day TMC asks GitHub whether a newer version has been published. When there is one, TMC tells you once, and **Download update** opens the new APK in your browser. TMC never downloads or installs anything by itself.

**Privacy and your data**
- **The terms screen appears once more** after updating, because the privacy policy now names two more services. **GitHub** hosts the stream player page and the releases TMC checks for updates. **Cloudflare** carries every connection between the app and the TMC server. The policy also says more clearly how the Pokémon Community Game extension works inside the app, and lists what the app sends to the TMC server for alerts.
- **One erase instead of two.** In *Safety & Privacy* → *Reset local data*, *Erase everything on this device* is gone. **Erase everything and remove this device from the server** now always erases the phone, even when the server cannot be reached. If the network is down at that moment, TMC stops the alerts to this phone on its own once the network is back. What happens in that case to this phone's registration on the server is explained on the **Data Deletion** page.
- **Removing an account now stops its alerts even if the network fails at that moment.** TMC keeps trying on its own and switches them off once the server can be reached. Until then, alerts for that account may still arrive.
- **Turning every alert category off now tells the server**, so the alerts stop arriving.
- **An erase on a phone that never registered with the server now says so**, instead of saying it will try again later.
- The **Data Deletion** page has been rewritten (2026-09-28), and the **Privacy Policy** updated (2026-11-13).

## Known issues

- **A mute you set in the player is not remembered.** After you hide and show the stream, or switch channel, the stream plays with sound again.
- **The chat can be squeezed.** In landscape, or with the stream and the keyboard both open, the chat gets very little room. Holding the phone upright with the stream hidden gives it its full height.
- **Rotating the phone hides the stream.** Tap **Show stream** again.
- When a Pokémon you are missing spawns for several of your accounts at once, you get one alert per account, as intended. Now and then one of those alerts may play its sound twice. It is harmless. It was not fixed in this update.
- Right after the chat reconnects, a few messages may be briefly missing. They appear when you switch to another account and back.
- **After a period without network, the chat may not reconnect by itself.** The status line says the account could not be authenticated, no new messages arrive, and you cannot send. Close TMC completely and open it again.
- **Some longer messages are cut short.** On Android 12 and later, the short messages that appear at the bottom of the screen after an erase, or after removing an account without network, can be cut off after two lines. The part cut off can say that alerts may still arrive for a while and that TMC keeps trying to stop them on its own.

## What to download

- For almost every modern Android phone: `TwitchMiniChat-Android-arm64-v8a.apk`
- Only for older 32-bit phones: `TwitchMiniChat-Android-armeabi-v7a.apk`

Requires Android 8.0 or later. SHA-256 checksums are in `SHA256SUMS.txt`:

```
fd06d29e470337465f964841cce8f58551fe1497a45792daa5a8e4fbf0745ff3  TwitchMiniChat-Android-arm64-v8a.apk
fcbdc83f60ad6787a2c0dafb29b405cf8d0fa95bfe31a97182553fa9604473ce  TwitchMiniChat-Android-armeabi-v7a.apk
```

## How to install

1. Download the APK on your phone.
2. If Android asks, allow your browser or file manager to install unknown apps.
3. Open the APK from your Downloads folder and tap **Update** (or **Install** on a first install). Your accounts and settings are kept.
4. On first opening, accept the terms once more, and the welcome page follows.

## How to use

Sign in with Twitch to use the chat, then sign in again inside the app to open the PCG extension. The two logins serve different purposes: one connects the chat, the other the extension.

## Source code

The complete source code is in this repository. This release is under the `v5.6.0` tag.

## Disclaimer

TMC is an independent, unofficial project with no affiliation to Twitch or the Pokémon Community Game, which belong to their respective owners. TMC never catches automatically: every chat command or gameplay action is triggered by you, one tap at a time.

## Contacts

- For bug reports, feedback or suggestions, join the [TMC Discord server](https://discord.gg/E8yRUp2MBK)
- You can also find me on Twitch: [twitch.tv/unouidol](https://www.twitch.tv/unouidol)
- Business inquiries: unouidol.interactive@gmail.com
- [Privacy Policy](https://unouidol.github.io/tmc/privacy.html) · [Data Deletion](https://unouidol.github.io/tmc/data-deletion.html)

<!-- release body ends -->
