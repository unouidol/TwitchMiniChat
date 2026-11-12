# Proposal — a welcome page, an update notice and a Bug report entry

**What this is.** A proposal, written on 2026-11-12 before any code. Nothing here is built, and
nothing here is decided except the owner's decisions listed first. Every choice that belongs to
Federico is marked **Federico's**, and is laid out with its options and their costs. It chooses none
of them: the wording, the links, the endpoint, and whether the feature enters 5.6.0 are his.

**Provenance.** Each fact below is one of two kinds, and says which:

- **read:** read from the code at `main-v5` `d81507a`, the website's clone at `unouidol/tmc-site`
  `0fa3ec0`, or the policy pages' clone at `unouidol/unouidol.github.io` `bcfbe2a`;
- **executed:** a command run on `nbpromec1` on 2026-11-12.

The Discord invite and the Ko-fi page were not opened, joined or fetched. Nothing in this step
needed their contents.

## DECIDED by Federico on 2026-11-12, after this proposal was written

**This section overrides what follows wherever the two differ.** The rest of the document is kept
as it was proposed. **He wants everything in 5.6.0, so that later versions can go to other work.**

1. **The full version.** It has the welcome page, the Bug report entry, and the update notice with
   its version check: Cut B on top of Cut A. The smaller version with no network request is not
   chosen.
2. **The endpoint is GitHub's releases API** (endpoint 1), the same source the website uses.
   - **Reason:** the privacy policy is being corrected to name GitHub anyway (item 7), so this adds
     no second recipient.
3. **The update button opens the browser, and the app never downloads or installs an APK.** It
   requests no install permission.
   - **What it opens: the APK itself**, `releases/latest/download/<asset>`, choosing the asset that
     matches the installed one: `arm64-v8a` or `armeabi-v7a`. It is neither the website nor the
     release page.
   - **The reason** (the owner's words): "less hassle to users, less clicks are better than many
     clicks, even if it's just one".
   - **What it departs from:** the proposal did not recommend a direct link, because the app would
     "have to guess the ABI". It does not have to guess: a 64-bit process can only be running the
     `arm64-v8a` APK, since the two published APKs differ only in their native code.
   - **What it gives up:** the user sees no release notes and no install instructions.
   - The browser still does the downloading.
4. **The journal's export stays hidden,** as decided on 2026-09-15.
   - The Bug report page tells the user to describe the problem in the Discord channel, and that a
     diagnostics file may be asked for there.
   - It offers no export and does not reveal the gesture.
5. **The links,** each defined once in `ProjectLinks`:
   - the Discord invite `https://discord.gg/E8yRUp2MBK`;
   - the bug report channel `tmc-ticket`;
   - Ko-fi `https://ko-fi.com/tmc_app`. Ko-fi is shown.

   **The invite is compiled in as given.** The risk that it expires (*The risk in a compiled
   invite*) stays **open until Federico confirms that the invite is permanent**.
6. **Dev builds never show the update notice.** Option (A): dev makes no check at all.
7. **The corrections of form and of fact go in the same release.**
   - **The privacy policy names GitHub,** for the stream player page and for the version check.
   - **The terms gate is raised, by the criterion of 2026-09-26,** so every user sees the terms once
     on first opening 5.6.0.
   - **The website's fallback version is corrected.**
8. **When the page shows.** It shows:
   - on first launch;
   - **once after the app is updated to a new version;**
   - when an update is available and not yet dismissed for that version;
   - otherwise never.

   This settles the table's "existing user" row, and adds a row: every update shows the page once.
9. **The update notice names the new version and nothing else.** It has no countdown and no "you
   are late" wording: alerts inform, and the user decides.
10. **The menu entry is labelled "About TMC",** and the Bug report entry "Bug report".
11. **The page follows the terms gate on first launch,** as the table of situations says. He asked
    for that table to be implemented exactly.

## As implemented, and where it departs from the proposal

The code is in `app/src/main/java/com/fs/twitchminichat/welcome/` and `ProjectLinks.kt`. Every
decision is in `WelcomePolicy`, and each row of the table is a test in `WelcomePolicyTest`.

**Where the implementation departs from the proposal, and why:**

1. **"Dismissed" means shown.**
   - **What changed:** the notice's tag is recorded when the page is shown, not when it is closed.
   - **Why:** not every way a page leaves the screen can be observed, such as a task swiped away
     or a process killed in the background. A record kept on close would need a guard on paths
     no test reaches.
   - **What stays the same:** the outcome, which is once per version.
2. **The check records its attempt, not its success.**
   - **What changed:** `last_check_epoch_ms` is written before the request goes out.
   - **Why:** so that a failure also waits a day. Otherwise an offline phone would ask at every
     start, which is not "at most once a day".
3. **One value replaces two.**
   - **What changed:** `last_seen_version_code` stands for both "first launch seen" (absent
     until the first decision) and "last version seen".
   - **Why:** two flags could disagree, and one cannot.
   - **The file holds four keys:** that one, `latest_release_tag`, `dismissed_release_tag` and
     `last_check_epoch_ms`.
4. **An existing user's first start of 5.6.0 shows the first-launch page.**
   - **Why:** nothing recorded before 5.6.0 tells an existing user from a fresh install. Its
     title, "Welcome to Twitch Mini Chat", is true for both.
   - **From the next update on,** the page appears under its own title, "Welcome to Twitch Mini
     Chat %1$s".
5. **The notice text is shorter than proposed.** "You have %2$s." and the paragraph about
   installing over the current version are gone, under decision 9: the notice names the new
   version and nothing else.
6. **The button reads "Download update", not "Open download page",** because it opens the APK
   itself (decision 3).
7. **The pages are `DialogFragment`s.**
   - **What this gives:** a page on screen comes back by itself after a rotation.
   - **The saved state** keeps only the start's decision while it is still owed, for a rotation
     while the terms gate is up.
8. **An account start includes a sign-in.** The proposal's "started from a notification tap"
   row covers every start that opens a chat directly, and that includes a sign-in that has just
   finished (`EXTRA_NEW_ACCOUNT_ID`). The page waits for the next ordinary start.
9. **The Bug report page** has no export, under decision 4. It says "If more detail is needed,
   you may be asked there for a diagnostics file.", and the list of what to include became one
   sentence.

## What Federico asked for

1. **A welcome page, shown before the login page.** It carries:
   - the project's Discord contact;
   - a sentence saying that bugs and problems are reported in the dedicated Discord channel;
   - optionally, a Ko-fi link.
2. **An update notice on it.** When a newer version exists, a button takes the user to it.
3. **When it appears.** It does not appear at every start. It appears:
   - on first launch;
   - when an update is available.

   Otherwise it is reachable from the privacy/info menu. The page says, in one line, where to find
   it again.
4. **A "Bug report" entry among the privacy/info pages,** since nothing equivalent exists (see (a)).

## The owner's decisions, with their reasons

- **The update button opens the download page in the browser.** The app does not download or
  install the APK itself, for three reasons:
  - Android never allows a silent install.
  - An install from inside the app needs the "install unknown apps" permission, which adds attack
    surface. The manifest declares only `INTERNET` and `POST_NOTIFICATIONS` (read,
    `AndroidManifest.xml:4-5`), and this keeps it so.
  - An app that updates itself outside the store is against Play policy. That matters because a
    Play Store preparation branch, `feature/playstore-prep`, exists (read: it is in this machine's
    clone, with no remote copy).
- **The version check is a new network request.** It must:
  - fail silently;
  - never block or delay start-up;
  - be declared wherever the app documents what it contacts.

  Where it would have to be declared is under (e).
- **The links are Federico's.** He supplied them on 2026-11-12, to be used exactly as written:

  | Value | As supplied |
  |---|---|
  | Discord invite | `https://discord.gg/E8yRUp2MBK` |
  | Discord channel for bug reports | `tmc-ticket` |
  | Ko-fi | `https://ko-fi.com/tmc_app` |

  They are public by intent, so they are not secrets and need no redaction.

## Read first

### (a) The info and privacy menus today

**Two buttons reach the same menu, Safety & Privacy (`SafetyPrivacyActivity`).** Read:

- **On the login page:** a text button labelled "Safety & Privacy" (`fragment_login.xml:49-53`,
  string `safety_and_privacy`). Its handler is at `LoginFragment.kt:122-124`.
- **On the chat page:** the ⓘ icon (`fragment_chat.xml:60-72`,
  `@android:drawable/ic_menu_info_details`). Its content description is "Safety & Privacy", and its
  handler is at `ChatFragment.kt:1842-1844`.

The login page also has its own Privacy Policy and Terms of Use buttons
(`fragment_login.xml:56`, `:63`; `LoginFragment.kt:126-142`). At its foot is the version label,
`Version %1$s (build %2$d, %3$s)` (`fragment_login.xml:70`; `LoginFragment.kt:164-175`).

**The Safety & Privacy menu (`activity_safety_privacy.xml`), in order:**

| Line | Entry | Opens |
|---|---|---|
| 14-26 | title "Safety & Privacy" and subtitle "Manage reporting, blocking, privacy information, and data controls." | — |
| 30-34 | Report & Block | bundled `report_block.html` (`SafetyPrivacyActivity.kt:22`) |
| 37-41 | Blocked Users | `BlockedUsersActivity` (`:49`) |
| 44-48 | Privacy Policy | bundled `privacy.html` (`:31`) |
| 51-55 | Terms of Use | bundled `terms.html` (`:40`) |
| 58-62 | Data & Account Control | `DataAccountControlActivity` (`:53`) |
| 65-77 | crash reporting switch and its note | (`:72`, `:81`) |
| 80-100 | External links: title, subtitle, reset button | (`:59`) |
| 103-107 | Credits | bundled `credits.html` (`:63`) |

**Data & Account Control (`fragment_safety_privacy.xml`)** holds:

- Blocked Users (`:30-34`);
- Reset local data (`:37-41`);
- Delete account and all data (`:44-48`).

**Nothing tells the user how to report a bug or reach the project.** The only contact anywhere in
the app is the developer's email address in the policy pages, given each time for a narrow
purpose:

- `privacy.html:30-33` and `:109-111`, for privacy questions and deletion requests;
- `terms.html:73-75`, for questions about the terms;
- `credits.html:46-48`, for questions about credits;
- `data_deletion.html:129`, for deletion by email.

No page mentions bugs, Discord or Ko-fi. "Report & Block" is about reporting chat messages, not
bugs. That nearness of names matters for the new entry's label; see *The Bug report entry*.

Also read: the titles passed to `PolicyPageActivity` are hard-coded in Kotlin, not taken from
resources:

- `SafetyPrivacyActivity.kt:25`, `:34`, `:43` and `:66`;
- `LoginFragment.kt:129` and `:138`.

That predates this proposal. New code would take its titles from `strings.xml`.

### (b) The diagnostics journal and its export

**How it leaves the phone today: a hidden gesture only.** Read:

- A long press on the version label at the foot of the login page
  (`DiagnosticsExportGesture.attach`, `LoginFragment.kt:174`) hands
  `tmc-history-diagnostics.txt` to the system share sheet, through a `FileProvider`
  (`DiagnosticsExportGesture.kt:39-87`).
- Nothing on screen announces it.
- A visible button, "Export history diagnostics" in Safety & Privacy, was removed on 2026-09-15
  (`6f3a4ef`). Its reason: #30 had already decided on a hidden gesture and against a control.
- `RELEASE-STATE.md`, *The diagnostics journal and its hidden gesture*, records how the gesture
  could later be confined to the development flavour.

**What the export reveals to whoever receives it.** Read from `HistoryDiagnosticsLog.kt:150-170`
and from the privacy policy's own disclosure (`privacy.html:47`). The header gives:

- the app's version, build number and commit;
- the export time;
- the device model and the Android software development kit (SDK) level.

Each line then carries:

- **the Twitch account name and the channel it relates to.** This is on purpose, per
  `RELEASE-STATE.md`;
- the times the chat history was loaded, and with what result;
- app and connection events;
- how each push alert was delivered, and whether its sound played;
- the phone's sound settings at that moment: ringer mode, notification volume, Do Not Disturb, and
  whether audio was going to a Bluetooth device.

It never contains message text, passwords or sign-in tokens.

**Would offering the export on a Bug report page fit? Technically yes; it is Federico's call.**

- **The policy already covers it.** It says the record "leaves the device only if the user
  deliberately chooses to share a copy". A tap on a button is such a choice.
- **No new text is needed in the policy.** `RELEASE-STATE.md` already records the rule that "a
  policy declares data practices, not affordances".
- **What it costs:** it reverses `6f3a4ef`, and the note about confining the gesture to the
  development flavour would stop applying.
- **What it would reveal:** whoever reads the bug channel would see the reporter's Twitch account
  and the channels they watched, with times. Whether `tmc-ticket` is visible to other members was
  not checked, since the server was not opened.

### (c) How the website learns the version, and what the app could ask

**The website.** Read, `tmc-site/index.html:265-305`. The page calls
`GET https://api.github.com/repos/unouidol/TwitchMiniChat/releases/latest` with
`Accept: application/vnd.github+json`, takes `tag_name`, strips a leading `v`, and writes it into
its three `data-tmc-version` elements, caching it in `sessionStorage`. If the request fails, the
page keeps the number written in the HTML.

**The same endpoint, asked by curl.** Executed on 2026-11-12:

- **What it returned:** `200 OK` and 11,625 bytes of JSON, with no authentication:
  - `tag_name` `v5.5.1`;
  - `name` `Twitch Mini Chat 5.5.1`;
  - `draft` `false` and `prerelease` `false`;
  - `published_at` `2026-09-25T06:10:44Z`;
  - `html_url` `https://github.com/unouidol/TwitchMiniChat/releases/tag/v5.5.1`;
  - assets `SHA256SUMS.txt` and the two APKs.
- **Rate limit:** `X-RateLimit-Limit: 60` per hour on the `core` resource, unauthenticated.
  GitHub's documentation counts that limit per IP address (documented, not tested). Every phone
  behind one shared address, such as a carrier's, would draw on the same 60.
- **Caching:** `Cache-Control: public, max-age=60` and an `ETag`.
- **Drafts and pre-releases:** this endpoint never returns them (documented). A pre-release
  published for testing would therefore never trigger the notice.

**What the request discloses about the user.**

- **To GitHub:** the IP address and the time, as any request does.
- **The User-Agent header.** GitHub requires one. The app's other clients all use
  `HttpURLConnection` and set none (read, e.g. `ProfileDeletionStateClient.kt:117-135`). Android's
  default is `Dalvik/2.1.0 (Linux; U; Android <version>; <model> Build/<build id>)` (known, not
  observed here), which names the phone model and its build.
- **Recommendation:** an explicit `User-Agent: TwitchMiniChat-Android`, which names the app and
  nothing else.
- **What it carries otherwise:** no account, no token and no identifier, and no cookies. The app
  sets no `CookieHandler` (read).

**Offline.** The connection fails at the name lookup or the connect. With a timeout like the
app's others (`NETWORK_TIMEOUT_MS = 10_000`, `ProfileDeletionStateClient.kt:202`), the check:

- runs on a background thread;
- fails within that time;
- shows nothing;
- leaves the last stored answer as it was.

Start-up never waits for it.

**The alternatives, each with its cost.** **Which endpoint is Federico's,** because it decides who
receives the request.

| Endpoint | What it costs |
|---|---|
| **1. GitHub's releases API** (above) | GitHub becomes a recipient the policy must name. 60 requests per hour per shared IP address. 11.6 KB per check. Its tag is the same contract the website already relies on. |
| **2. The project's backend**, `api.ircminichat.party`, with a new endpoint returning the latest tag | No new recipient: the backend already receives requests from every installation. It is a coordinated Android/backend change, deployed before the app depends on it. The backend must learn each release, either by hand at release time or by asking GitHub itself and caching the answer. |
| **3. A static file on the project's site** | It is written by hand at every release. The evidence against it is in this very check: the website's hand-maintained fallback reads `5.5.0` at `tmc-site/index.html:65`, `:187` and `:229` (read), while `v5.5.1` has been the latest release since 2026-09-25. Which host serves `tmc.ircminichat.party` was not checked. |
| **4. Following the redirect of `github.com/unouidol/TwitchMiniChat/releases/latest`** | It answered `302` to `/releases/tag/v5.5.1` (executed). It is a web page's behaviour, not a documented interface, and is not recommended. |

### (d) First launch, where the state lives, and the resets

**Nothing records a first launch today.** The nearest thing is `TermsPrefs`, which stores
`accepted_terms_version` in `tmc_terms_prefs`, against `CURRENT_TERMS_VERSION = 2` (read,
`TermsPrefs.kt:7-12`). It reads 0 on a fresh install and after any reset, because both resets
erase it.

**The existing preference files** (read; every `getSharedPreferences` name in `app/src/main`):

> `buddy_info_store`, `catch_preset_store`, `channel_history`, `crash_reporting_prefs`,
> `current_spawn_store`, `external_link_preferences`, `fcm_registration`, `hidden_users_store`,
> `inventory_ball_store`, `oauth_pending`, `owed_server_operations`, `pcg_alert_acknowledgement`,
> `pcg_custom_watchlist`, `pcg_event_spawn_alerts`, `pcg_notification_alert_prefs`,
> `pcg_pokedex_snapshots`, `pcg_spawn_alert_modes`, `push_settings`, `tmc_terms_prefs`,
> `twitch_emote_catalogs`, `twitch_emote_recents`, `v2_accounts` (legacy accounts).

The accounts themselves are not among them: they live in an encrypted file, `accounts.bin`
(`EncryptedAccountStore.kt:252`), outside `shared_prefs`.

**None fits, so the proposal is one new file, `welcome_prefs`.** It holds:

| Key | Type | Meaning |
|---|---|---|
| `welcome_seen` | Boolean | the first-launch page has been shown |
| `latest_release_tag` | String | the tag from the last successful check |
| `update_dismissed_tag` | String | the tag whose notice was last shown and closed |
| `last_check_ms` | Long | when the last successful check ran, for the once-a-day limit |

**A Boolean, not a "last version seen".** A last-version-seen value would only be needed to show
the page after every upgrade, as a "what's new". Federico did not ask for that, so the Boolean is
enough. If he wants it, the key becomes the version name.

**The resets need no new code, and must not be given any.** Read:

- `LocalDataCleaner.clearInternal` (`LocalDataCleaner.kt:80-198`) lists every file in
  `shared_prefs` and deletes each by name (`:123-145`). A new file is therefore erased by both
  resets without being named anywhere.
- The keep-accounts reset spares only:
  - the account file its caller passes, `v2_accounts` (`SafetyPrivacyFragment.kt:24-26`);
  - three named stores (`keepAccountsExclusions`, `:69-78`).

  `welcome_prefs` must stay out of that list.
- A unit test pins that it stays out. `LocalDataCleanerKeepAccountsTest` exists for exactly this.

What a reset then does to the page:

- **After either reset,** the first-launch page shows again. So does the terms gate, which the
  data-deletion page already announces ("your acceptance of the terms, which you are asked for
  again").
- **A dismissed notice** shows once more.

### (e) The documents that describe what the app contacts or stores

| Document | Needs a change? | Why |
|---|---|---|
| Privacy policy, bundled `app/src/main/assets/policies/privacy.html` | **Yes, if the check goes to GitHub** (endpoint 1) | §4 *Who data may be shared with* names Twitch, Google/Firebase, the backend's hosting providers and authorities (read, `:61-67`). GitHub is not named. §3 *Why TMC uses data* (`:50-59`) has no purpose that covers "tell you when a new version exists". With endpoint 2, §4 already covers the recipient, and only §3 might change. |
| Privacy policy, published `https://unouidol.github.io/tmc/privacy.html` | **Yes, with the bundled copy** | `AGENTS.md`: same step, same wording, same "Last updated" date, and the web copy published before the release that bundles it. |
| Data deletion page, both copies | **No** | Its keep-accounts paragraph already says "the app's settings and preferences" are removed, which covers `welcome_prefs`. |
| Credits, both copies | Optional | §*Third-party services* lists Twitch and Firebase (read, `credits.html:40-44`). GitHub could be added. |
| Terms of Use | No | §5 speaks of third-party services in general. |
| `RELEASE-STATE.md` | **Yes, if it goes into 5.6.0** | *Two web copies are written and must not be published yet* (read, `:668-716`) holds `privacy.html` back for publication at 5.6.0, dated `2026-09-26`. A new privacy paragraph would join that same publication with a new date, and the section has to say so. |
| The user guide | — | None exists. Read: the repository holds no guide, and `README.md` names no contact. |
| The website, `tmc-site/index.html` | No | The app's check does not touch it. |

**Two related facts, recorded, not solved here.**

- **The terms gate.** On 2026-09-26 `RELEASE-STATE.md` recorded a criterion for raising
  `CURRENT_TERMS_VERSION`: "data newly collected, a new recipient, or a new purpose". A check
  against GitHub is a new recipient and a new purpose, so by that criterion the gate would be
  raised to 3, which re-shows it to every user on the release that ships it. Endpoint 2 is a new
  purpose only. **Whether to apply the criterion is Federico's.**
- **The player page.** When the stream is shown, the app already loads its player page from
  GitHub Pages (`StreamPlayerUrl.PAGE`, `https://unouidol.github.io/ircminichat/player.html`), and
  the privacy policy does not name GitHub for that either. That gap exists today, independent of
  this proposal. **Whether one sentence covers both is Federico's.**

### (f) The launch flow, and where a page fits

Read, `MainActivity.kt`:

1. `onCreate` (`:63-110`):
   - applies the crash-reporting choice;
   - creates the alert channels;
   - builds the pager, whose page 0 is the login page (`:295-297`).
2. Then, unless the app was opened from a notification or recreated, it calls
   `openSingleAccountOnStartupIfPossible` (`:100-102`, `:145`). With exactly one account, this jumps
   straight to its chat page, so **a signed-in user never sees the login page**.
3. `RemoteDeletionChecker.checkOnceOnAppOpen` (`:104-110`) then runs in the background.
   `continueStartupAfterDeletionCheck` (`:229-242`) then:
   - runs the owed server work;
   - asks for the notification permission;
   - fetches the push token.
4. `onResume` (`:345-348`) raises the terms gate, `requireTermsAccepted` (`:358-384`). It is an
   `AlertDialog` over the pager (`TermsGate.kt:35-87`).

So on a first launch, what shows before the login page is:

- the terms gate;
- on Android 13 and later, the notification-permission prompt.

Nothing else does.

**Why the page should be a dialog, as the gate is, and not an activity.** Read,
`ChatFragment.kt:2056-2076`:

- **An opaque activity on top** stops `MainActivity`. That runs the chat page's `onStop`, which
  closes the IRC connection (`closeIrcClient(resetBackoff = true)`). Closing the page would then
  reconnect it and re-run the history backfill decision on resume.
- **A dialog-themed activity** pauses it instead, and the resume still re-runs that decision
  (`:2028-2054`).
- **A plain dialog** changes neither lifecycle. **A signed-in user's chat connects underneath it
  as it does today, and is not delayed:** the page sits on top while the connection is made.

## The proposal

### The page, as a dialog over the pager

`WelcomeDialog` is built like `TermsGateDialog`: an `AlertDialog` with a custom layout. It has three
parts:

- the update block, which shows only when a newer version is known;
- the community block: Discord, the bug sentence and, optionally, Ko-fi;
- the "where to find this again" line.

**Its order with the terms gate.** On a first launch the gate comes first and the page follows the
user's Accept. The page is raised from the same place, `requireTermsAccepted(onAccepted = …)`.
There are two reasons:

- the version check, as a disclosed request, should not run before the policy that declares it
  is accepted;
- a declined gate closes the app, so a page before it would be shown for nothing.

**Federico may prefer the page first.** It is then raised in `onCreate`, and the gate follows when
it closes.

**Rotation.** Like the gate, the dialog is dismissed in `onDestroy`. A flag in the saved state
raises it again after the activity is recreated.

### The text, verbatim, for the three situations

All of it goes into `strings.xml`. The wording is **Federico's**: these are a starting point.
`%1$s` and `%2$s` are format arguments.

**1. First launch.**

> **Welcome to Twitch Mini Chat**
>
> Join the TMC community on Discord for news, help and feedback.
>
> [ Join Discord ]
>
> Found a bug or a problem? Report it on Discord, in the #tmc-ticket channel.
>
> *(optional)* TMC is free. If you would like to support its development, you can do so on
> Ko-fi.
>
> *(optional)* [ Support TMC on Ko-fi ]
>
> You can open this page again from Safety & Privacy.
>
> [ Continue ]

**2. Update available** (at a start, once per version):

> **Update available**
>
> Twitch Mini Chat %1$s is available. You have %2$s.
>
> The download page opens in your browser. Install the new version over this one to update: your
> accounts and settings stay.
>
> [ Open download page ]
>
> *(then the community block of situation 1, unchanged)*
>
> You can open this page again from Safety & Privacy.
>
> [ Not now ]

"Your accounts and settings stay" holds for an in-place update: same signing key and a higher
`versionCode`, which is what step 5 of the release procedure confirms.

**3. Opened from the menu.**

> **Twitch Mini Chat**
>
> *one of these three:*
> - when a newer version is known: the update block of situation 2, its two sentences and its
>   button;
> - when the last check found none: You have version %1$s. No newer version was found at the last
>   check.
> - when no check has succeeded yet: You have version %1$s.
>
> *(then the community block of situation 1, unchanged)*
>
> [ Close ]

The "where to find this again" line is left out here, because the reader is already there.

**The menu entry's label is Federico's.** Candidates:

- "Welcome & updates";
- "About TMC";
- "Community & updates".

### When it shows, and when it does not

The decision is made once per cold start, in a pure `WelcomePolicy` that is tested without a device.
The page is never raised by the check's answer arriving mid-session: the answer is stored, and acted
on at the next start. That is what keeps the check from interrupting or delaying anyone.

| Situation | Page at this start? | Content | Why |
|---|---|---|---|
| First launch: fresh install, or after either reset | **Yes**, after Accept | situation 1, with no update block | `welcome_seen` is absent. No check has run before the terms were accepted. |
| Existing user, first start of the version that adds the page | **Federico's** | situation 1 | (i) **show it once**: every current user meets Discord and Ko-fi. (ii) **don't**: `welcome_seen` is set silently when `accepted_terms_version` is already above 0. |
| Signed-in user, no update known | **No** | — | The chat opens directly, as today. |
| Update known and not dismissed for that tag | **Yes**, once | situation 2 | `latest_release_tag` is newer than the installed version and differs from `update_dismissed_tag`. |
| Update known, already dismissed for that tag | **No** | the menu page still shows it | Dismissal is per tag. |
| A later tag than the dismissed one | **Yes**, once | situation 2 | The tags differ. |
| Offline | Only what stored state already calls for | as stored | The check fails silently, and nothing is stored. |
| Check failed: HTTP error, 403 rate limit, unreadable JSON, unparsable tag | **No** because of it | as stored | Same as offline. Nothing is retried within that start. |
| Started from a notification tap | **No**, deferred to the next ordinary start | — | The user came for that chat (`openedFromIntent`, `MainActivity.kt:98-102`). |
| Recreated, for example by a rotation | Only if it was on screen | the same | It is restored from the saved state. |
| Terms declined | **No** | — | The app closes, as today. |
| Opened from Safety & Privacy | **Always** | situation 3 | On demand. |

**Dismissed** means: the page showed a given tag and was then closed by any of its buttons or by
back. **Open download page** also counts, so that a notice appears once per version, not at every
start until the update is installed. The menu page keeps the button available.

**The check itself:**

- it runs at most once per 24 hours, after the terms are accepted, on a background thread;
- it is never retried within a start;
- it is never run from a notification-opened start;
- a `last_check_ms` in the future, from a clock set back, counts as due, so a wrong clock cannot
  silence it for good.

The 24 hours is a proposal. Fewer checks also means less disclosure to the recipient.

### How "newer" is compared, and dev builds

**The latest tag.** It must match `^v?(\d+)\.(\d+)\.(\d+)$`. The `v` is optional because one old
tag, `5.2.0`, was published without it (executed, `gh release list`). The release contract is
`vMAJOR.MINOR.PATCH`. Anything else, such as `v5.6`, `v5.6.0-rc1` or an empty value, is
unparsable, and **an unparsable tag shows nothing**.

**The installed version.** `BuildConfig.VERSION_NAME` must match
`^(\d+)\.(\d+)\.(\d+)(-[A-Za-z0-9.]+)?$`:

- stable is `5.5.1`;
- dev is `5.5.1-dev`, because `versionNameSuffix = "-dev"` (read, `app/build.gradle.kts:147`).

**The comparison.** "Newer" means the tag's triple is greater, compared as numbers component by
component, so `5.10.0` is newer than `5.9.0`, which a string comparison gets wrong. An equal or
lower tag shows nothing, and so does a value that cannot be parsed on either side.

**Dev builds are Federico's.** The dev app is a different package, `com.fs.twitchminichat.dev`
(`applicationIdSuffix`, `:146`). The download page installs the stable app beside it and never
updates it, so for dev an "update" notice is not literally true.

- **(A) Dev never checks.** No request is made and no notice is shown. **Cost:** the notice cannot
  be seen on the test phone in dev. A stable debug build cannot be installed over the published
  release-signed stable app, because the signatures differ, and replacing that app means
  uninstalling it.
- **(B) Dev checks and compares the triple, ignoring `-dev`.** A dev build older than the latest
  release shows the notice. That is harmless, and it makes the notice testable. **Cost:** the
  button installs the stable app, not a dev update.

**Either way, seeing the notice on a phone needs an installed version below the latest release,**
`v5.5.1` today. There are two ways:

- a local, uncommitted `versionName` edit for that one test build. This conflicts with the
  device steps' "clean tree before the build";
- a Gradle property read only by the dev flavour, for example
  `-PtmcDevVersionNameOverride=5.5.0`. This keeps the tree clean, but adds build-script code that
  exists for testing.

**Which one is Federico's.**

### The links, each in one place

A new `ProjectLinks` object, beside `WebPolicies` (`WebPolicies.kt:3-9`) and `StreamPlayerUrl`,
the existing homes of external web addresses:

```kotlin
object ProjectLinks {
    const val DISCORD_INVITE_URL = "https://discord.gg/E8yRUp2MBK"
    const val BUG_REPORT_CHANNEL = "tmc-ticket"
    const val KOFI_URL = "https://ko-fi.com/tmc_app"
    const val DOWNLOAD_PAGE_URL = "<Federico's choice, below>"
    const val LATEST_RELEASE_API_URL =
        "https://api.github.com/repos/unouidol/TwitchMiniChat/releases/latest"
}
```

**How the strings use it.** They take the channel as an argument, `#%1$s`, so its name exists only
here, and changing any link is a one-line change. The backend's addresses live in `strings.xml`
instead (`delete_server_data_url` and others, read), but those are server endpoints. The
policies' web pages and the player page live in Kotlin objects, and these links are of that kind.

**Where else they appear.**

- **The release notes** have carried the Discord invite since at least `v5.4.2`, in their
  *Contacts* section: `v5.4.2`, `v5.5.0` and `v5.5.1`. `v5.5.1` says "For bug reports, feedback or
  suggestions, join the TMC Discord server" (executed, `gh release view`).
- **Nowhere else:**
  - the channel name `tmc-ticket` appears nowhere;
  - Ko-fi appears nowhere: not in those three release notes, the repository, or either site;
  - neither site, `tmc-site/index.html` or `unouidol.github.io/tmc/index.html`, carries Discord,
    Ko-fi or any contact (read);
  - there is no user guide.
- **So the app and the release notes are the two places that would carry the invite.** A changed
  invite means changing both. The published notes are edited on GitHub, not in this repository.

**Also seen:** `v5.4.2`'s note wraps the link three times,
`[[[TMC Discord server](…)](…)](…)`. It is a formatting slip in a published note, and is left
alone here.

**The download page is Federico's.** Candidates:

- `https://tmc.ircminichat.party/`, the public site. It says which APK to pick and how to install,
  and its buttons resolve to the latest release.
- `https://github.com/unouidol/TwitchMiniChat/releases/latest`, the release notes with both APKs
  and the checksums.
- The `html_url` the check returned, which is that same page pinned to the tag the notice names.

A direct APK link is not proposed. It downloads one Application Binary Interface's (ABI's) file
with no instructions, and the app would have to guess the ABI.

**Opening a link.** Each opens with `ACTION_VIEW`, as the policy page's web button does
(`PolicyPageActivity.kt:37-38`). An `ActivityNotFoundException` is caught and shown as a toast.
Unlike `PolicyPageActivity`, which does not catch it today, these links can meet a phone with no
browser. `ExternalBrowserLinkController` is not reused: it exists to warn about links in chat
messages, which these are not.

### The risk in a compiled invite, and the two ways to limit it

**The risk.** A Discord invite can be created to expire (Discord's default is seven days, known and
not verified here) or to stop after a number of uses. It can also be revoked. A link compiled into
an APK cannot be corrected until the next release. Every installed copy older than the correction
keeps the dead link for good, on the welcome page, the Bug report page and in the menu.

That the same code has been in the release notes for more than three months, since 2026-08-06, is
consistent with an invite that does not expire. It does not show that it has no use limit, and
whether it still works was not checked.

**Federico chooses; neither is chosen here.**

| | Federico confirms the invite is permanent | The app links to a page on the project's own site that redirects to Discord |
|---|---|---|
| **What it costs to build** | Nothing. | A small redirect page in `unouidol/tmc-site` (or `unouidol.github.io`), for example `/discord`. It must be published before the release that links to it, by the same rule as the policy pages. Whether the host can redirect at the server, or only with a meta refresh, was not checked. |
| **What it protects against** | Expiry and a use limit, as long as the settings stay as confirmed. | Expiry, a use limit and revocation: changing the page needs no release. |
| **What remains** | A revoked or replaced invite still needs a release, and installed copies keep the old link. The confirmation is a fact about Discord's settings that nothing in this repository can watch. | One more hop. The site's host sees the visitor's address before Discord does. Whether the Discord app still opens directly, rather than the browser first, is a phone test. A second repository now holds the live link. |
| **Applies also to** | — | Ko-fi and the download page, if wanted. |

### The Bug report entry

**Placement.** It sits in Safety & Privacy, as its own entry, together with the welcome page's entry
at the end of the list after Credits. That forms an "about" group.

**Why not next to Report & Block.** The two names are close, and "Report & Block" means reporting a
chat message. Side by side, one is easily tapped for the other. **The label and the position are
Federico's:**

- "Bug report", as he named it;
- "Report a bug".

**Its page.** A dialog, as the welcome page is, over `SafetyPrivacyActivity`. Its text is verbatim,
and its wording **Federico's**:

> **Bug report**
>
> Bugs and problems are reported on the TMC Discord server, in the #tmc-ticket channel.
>
> To help find the cause, please include:
> - what you did, what you expected, and what happened instead
> - the app version shown below
> - your phone model and Android version
> - a screenshot or a screen recording, if you can
>
> Never share your Twitch password or a sign-in code.
>
> Version %1$s (build %2$d, %3$s)
>
> [ Open Discord ]   [ Close ]

**The version line** reuses `app_version_label`, selectable as on the login page, so the reporter
copies it without leaving the page.

**Offering the journal export is Federico's.** It is under (b). If he wants it, there would be a
button, "Share diagnostic record", with this line above it:

> The record names your Twitch account and the channels you watched, with times, and your phone's
> model, Android version and sound settings. It never contains chat messages or passwords. Share
> it only with the developer.

### Two ways to cut it

These are offered because the network request is where all the policy work comes from.

- **Cut A, no network.** The welcome page on first launch and from the menu, and the Bug report
  entry. There is no check, no new recipient and no policy change. The links open in the browser
  only when the user taps them, which is user-initiated, like the policy pages' web button today.
- **Cut B, the update check and its notice,** on top of Cut A, with the policy change and the
  terms-gate decision.

**Which cut, and which release, are Federico's.**

## Tests

**Unit tests (JVM, no device):**

1. **The version comparison:**
   - `v5.6.1` is newer than `5.6.0`;
   - `5.6.0` against `v5.6.0` is equal;
   - `5.10.0` is newer than `5.9.0`;
   - `5.6.0-dev` against `v5.6.0`, under option (A) or (B);
   - lower is not newer;
   - `v5.6`, `v5.6.0-rc1`, an empty value and whitespace are unparsable.
2. **The response parser:**
   - a trimmed fixture of the real response gives `v5.5.1`;
   - a missing `tag_name` gives nothing;
   - `draft` or `prerelease` true gives nothing, as a defence, although the endpoint excludes
     both;
   - text that is not JSON gives nothing.
3. **`WelcomePolicy`:** one test for each row of the table above.
4. **The once-a-day limit:**
   - 23 hours since the last check is not due;
   - 24 hours is due;
   - a last check in the future is due.
5. **`keepAccountsExclusions`** does not contain `welcome_prefs`. This goes in the existing
   `LocalDataCleanerKeepAccountsTest`.
6. **`ProjectLinks`' values,** pinned exactly as `StreamPlayerUrlTest` pins the player page. A
   changed link then fails a test, and the change is visible in review.

**On the phone (dev flavour).** Observable results come from screenshots and a Logcat tag,
`TMC_WELCOME`. The tag carries decision names and the HTTP status only, never the response body.

1. **First launch.** After "Reset local data, keep accounts", start the app:
   - the gate shows, then the welcome page in situation 1;
   - record the order relative to the notification-permission prompt.
2. **Each link.** Each button opens its target outside the app: browser, Discord app or Ko-fi.
   Federico runs this, since the targets are his.
3. **The update notice,** under the dev option and test-build method chosen:
   - situation 2 shows;
   - after Not now and a restart, it does not;
   - the menu page still shows the update.
4. **Offline.** Start in aeroplane mode:
   - no page because of the check;
   - no error;
   - Logcat shows one failed check.
5. **A signed-in user.** With one account and no update known:
   - the chat opens directly;
   - no page shows;
   - the journal shows no `lifecycle.stop` around the start, since the dialog does not stop the
     chat.
6. **A notification-opened start.** No page shows.
7. **Rotation with the page open.** The page comes back, once, with no crash.
8. **Safety & Privacy.** Both entries open their pages, and the Bug report page shows the version.

## Size and estimate

**Files.** About 9 new source files, 4 test files and 6 to 8 modified files.

- **New source files (about 9):**
  - `ProjectLinks.kt`;
  - `ReleaseVersion.kt`, the parse and compare;
  - `LatestReleaseClient.kt`, the request and parser;
  - `WelcomePrefs.kt`;
  - `WelcomePolicy.kt`;
  - `WelcomeDialog.kt` and `dialog_welcome.xml`;
  - `BugReportDialog.kt` and `dialog_bug_report.xml`.
- **Test files (4):** 3 new, and 1 extended (`LocalDataCleanerKeepAccountsTest`).
- **Modified files (6 to 8):**
  - `MainActivity.kt`;
  - `SafetyPrivacyActivity.kt`;
  - `activity_safety_privacy.xml`;
  - `strings.xml`;
  - `privacy.html`, if Cut B goes to GitHub;
  - `RELEASE-STATE.md`, if it enters 5.6.0;
  - possibly `TermsPrefs.kt`, if the gate is raised;
  - possibly `credits.html`.
- **Outside this repository:**
  - the published `privacy.html` at release time;
  - the redirect page, if that mitigation is chosen.

**Size:** about 800 to 1,000 lines, including KDoc and tests, at this repository's density of
documentation. **Cut A alone** is about half of that, with no policy work.

**Estimate, honestly:**

| Part | Estimate |
|---|---|
| Code and unit tests | about one working day |
| The policy text in both copies, and the `RELEASE-STATE.md` entry | a few hours once decided |
| The device round | about half a day, including the lowered-version build that makes the notice visible |
| Review rounds | one or two, as recent pull requests have needed |

**In total, two to three working days of elapsed time.** Most of the waiting is on the decisions
listed below, not on the code. If it enters 5.6.0, it lengthens 5.6.0's device round rather than
adding one of its own.

## Federico's decisions, in one list

1. **Whether the feature enters 5.6.0, and as Cut A, Cut B or both.**
2. **The wording** of the three situations, the Bug report page and the two entries' labels.
3. **The endpoint:** GitHub's API, the backend, or a static file.
4. **The terms gate:** whether the check raises `CURRENT_TERMS_VERSION` to 3 by the criterion of
   2026-09-26, and whether the policy's new sentence also names GitHub Pages for the player.
5. **Existing users:** whether they see the page once, on the first start of the version that
   adds it.
6. **Dev builds:** option (A) or (B), and the method for a test build below the latest release.
7. **The download page:** the site, the release page, or the exact release's page.
8. **The invite:** a confirmed permanent invite, or a redirect page on the project's site.
9. **Ko-fi:** shown or not.
10. **The journal export:** offered on the Bug report page or not.
11. **The page's order with the terms gate on first launch:** after Accept, as proposed, or before
    it.
