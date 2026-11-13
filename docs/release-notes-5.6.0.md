# Release notes for 5.6.0 — draft

**This is a draft for Federico, not the published text.** The release body is everything between
the two markers below, pasted as it stands once the placeholders are filled. Nothing outside the
markers belongs in the release.

**What it was written from.** The pull requests merged on `main-v5` since `v5.5.1`, read on
2026-11-13: `git log --first-parent v5.5.1..main-v5`, up to `3269e43` (#111), and the bodies of
#49 to #58, #66 to #69, #73, #78, #89, #94, #97, #103, #108, #110 and #111. #112, the Pokémon
Community Game statement in the privacy policy, was open when this was written. The sentence that
mentions it is for the release only if #112 has merged.

**Before it is published:**
- **Fill the two checksums** in *What to download* from the artifacts that were verified and
  installed in the device round. Never use the checksums of a rebuild.
- **Read the known issues against the device round.** Each one is what the last device step on
  record found. If the round shows something different, the round wins.
- **Ask about one item that the merged work does not settle.** "A few messages may be briefly
  missing right after the chat reconnects" was a known issue of 5.5.1. #66 and #67 changed how
  missed messages are recovered after a failed request and after a channel switch. Neither says
  that it fixes this case. So the item stays below as a known issue, unless the device round shows
  it is gone.
- **No speed figures are given.** The start-up measurement of the new chat list (`RELEASE-STATE.md`,
  *Closing start-up measurement of the conversion*) was taken on debug builds of the dev flavour,
  on one phone. Its own record supports "the ingestion window fell by X on these builds", and not
  "users see X less on a release build". If figures are wanted, see the sentence offered in the
  pull request that adds this file.

<!-- release body begins -->

## What is Twitch Mini Chat?

Twitch Mini Chat (TMC) is an Android app that combines a lightweight Twitch client with companion tools for the Pokémon Community Game (PCG) extension, built for people who use several accounts at once.

## What's new in 5.6.0?

5.6.0 rebuilds the chat, adds a welcome page and an update notice, and makes deleting your data do exactly what it says. It installs over your current version and keeps your accounts and settings.

**Chat**
- **The chat list has been rebuilt, and a channel opens faster** when its recent messages load. The list now reuses the rows that scroll off screen, instead of keeping every message you have received ready to show.
- **Starting the app no longer stalls on old spawn messages.** When the recent chat held a Pokémon spawn that was already over, TMC loaded its whole Pokémon catalogue to read it. Spawns that are over are now skipped before that.
- **Animated emotes stop when nobody can see them:** when their message scrolls out of view, and when the app is in the background. They start again when you come back.
- **Missed messages are recovered more reliably.** If loading the recent chat fails, for example because the network dropped, TMC asks again for what it missed instead of assuming it arrived. Messages from a channel you have just left no longer appear in the one you switched to.
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
- **The terms screen appears once more** after updating, because the privacy policy now names two more services. **GitHub** hosts the stream player page and the releases TMC checks for updates. **Cloudflare** carries every connection between the app and the TMC server. The policy also says more clearly how the Pokémon Community Game extension works inside the app.
- **One erase instead of two.** In *Safety & Privacy* → *Reset local data*, *Erase everything on this device* is gone. **Erase everything and remove this device from the server** now always erases the phone, even when the server cannot be reached, and stops alerts to this phone. Whatever it could not tell the server at that moment, TMC finishes on its own when the network is back.
- **Removing an account stops its alerts, even if the network fails at that moment.** TMC keeps trying until the server confirms.
- **Turning every alert category off now tells the server**, so the alerts stop arriving.
- **An erase on a phone that never registered with the server now says so**, instead of saying it will try again later.
- The **Data Deletion** page has been rewritten (2026-09-28), and the **Privacy Policy** updated (2026-11-13).

## Known issues

- **A mute you set in the player is not remembered.** After you hide and show the stream, or switch channel, the stream plays with sound again.
- **The chat can be squeezed.** In landscape, or with the stream and the keyboard both open, the chat gets very little room. Holding the phone upright with the stream hidden gives it its full height.
- **Rotating the phone hides the stream.** Tap **Show stream** again.
- When a Pokémon you are missing spawns for several of your accounts at once, you get one alert per account, as intended. Now and then one of those alerts may play its sound twice. It is harmless. It was not fixed in this update.
- Right after the chat reconnects, a few messages may be briefly missing. They appear when you switch to another account and back.

## What to download

- For almost every modern Android phone: `TwitchMiniChat-Android-arm64-v8a.apk`
- Only for older 32-bit phones: `TwitchMiniChat-Android-armeabi-v7a.apk`

Requires Android 8.0 or later. SHA-256 checksums are in `SHA256SUMS.txt`:

```
<sha256 of the verified arm64-v8a APK>  TwitchMiniChat-Android-arm64-v8a.apk
<sha256 of the verified armeabi-v7a APK>  TwitchMiniChat-Android-armeabi-v7a.apk
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
