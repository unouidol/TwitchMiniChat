# Design: an alert for missing alt dex forms

**Status: design note. The backend half is merged and deployed:** tmc-backend#9, `53fe1b4`,
merged on 2026-11-25; deployed in production by the owner's account of 2026-11-26, not checked
from this repository. **The app half is in two pull requests.** A1, the model and the upload
(the category in the selection, its stores and request field, `/upload_alt_dex`'s client, the
planner's `ALT_DEX_UPLOAD` step and the handling of a 409), merged as #146 (`b44c830`). A2,
the checkbox and its hint, the bell menu's message after OK, the final strings, the bundled
policies and `CURRENT_TERMS_VERSION` 4, is the pull request that adds this sentence. The web
copy of the two policy pages waits on `prepare/policies-5.7.0` in `unouidol/unouidol.github.io`,
to be published at the release (`RELEASE-STATE.md`). The owner asked for this note on 2026-11-25, with
the decisions below, and answered its open questions the same day. It covers the app
(`unouidol/TwitchMiniChat`) and the backend (`unouidol/tmc-backend`, private), the two backend
fixes of tmc-backend#5, and the deletion of Most Wanted watchlists, all three to ship in the same
backend pull request.

***Open question 1* is answered** (decision 14, the same day): how PCG's chat names each form,
from the production backend's logs, and the matching rule that follows. It was the precondition
of decision 7, so the backend pull request may start.

Provenance is marked throughout:

- **Decided:** the owner's decision of 2026-11-25, recorded as given.
- **Read in code:** read statically, not run. App at `11ab326` (`main-v5`), backend at `8ccf210`
  (`main`). Line numbers are at those commits.
- **Computed:** worked out on 2026-11-25 by a throwaway Python script over the committed files
  named, never committed. Neither repository's code ran.
- **Observed:** seen on the test phone or on a live system, with the date. Everything observed
  here comes from `docs/design-pcg-dex-readings.md`, which says when.
- **Not observed:** stated as such, and listed again under *Open questions*.
- **Recommended:** this note's proposal, for the owner to accept or change.

## The owner's decisions (2026-11-25)

1. A separate alert category, "Missing alt dex forms", beside today's categories in the bell menu
   ("PCG notifications").
2. Off by default, for new installations and for users who update. Whoever wants it turns it on.
3. No alt-dex reading uploaded means no form alert for that profile.
4. Rare forms (Mega, PCG, Clone, the 🔒 forms and the others) are included when missing.
5. Independent of Most Wanted and of the main-dex alert. When several apply to the same spawn and
   profile, the profile gets one alert, as today.
6. The privacy policy is updated, both copies (bundled in the APK and the web copy), before any
   alt-dex data leaves the phone.

**The answers to this note's open questions, the same day:**

7. **How the chat names forms is a precondition:** no code before it is answered (*Open question
   1*). The evidence pointed to a mismatch for regional forms. Once it was in, the note was to
   state how the chat's name maps to the page's for every form class: a form-preserving mapping
   for long regional prefixes, never `registration_key`'s merges of genders or seasons. Answered
   by decision 14.
8. **Event spawns, Christmas ones included, are covered** (*d*).
9. **The stored alt-dex list is deleted when the category is off on every device** (*b*).
10. **`CURRENT_TERMS_VERSION` goes from 3 to 4** (*g*).
11. **Most Wanted watchlists are deleted by profile data deletion,** in the same backend pull
    request, with a test: the code is made to do what `data_deletion.html` promises, and the page
    is not weakened (*i*, fix 3).
12. **F3 lands before the next app release:** the owner's decision of 2026-11-20, recorded on #117.
13. **The correction to #5's fix 1 is accepted:** its condition reads the record's `profile_ids`,
    not `registered_device_profile_ids` (*i*).

**The answer to *Open question 1*, the same day:**

14. **The chat names an alt-dex form exactly as the page does, so the match is exact** (*c*).
    - **What the owner read:** the production backend's logs from 2026-10-18 to 2026-11-25, 3638
      spawns under 841 distinct names. Every alt-dex form that spawned, 116 names (80 "(F)" and
      36 other "(…)" forms), appears in the chat with exactly the page's name, character for
      character: case, parentheses, hyphens, accents, the curly apostrophe of "Farfetch’d". Forms
      are about 6% of spawns, almost all of them "(F)" and "(…)" forms.
    - **Not alt-dex names:** base and main-dex forms also appear with a suffix ("Shellos (West)",
      "Palafin (Zero)"). They are not on the alt-dex page and must not match.
    - **Not seen:** no prefixed form spawned in the period: His or Hisuian, Alo or Alolan, Gal or
      Galarian, Pal or Paldean, Mega, PCG, Sin, Clone, the typed Arceus and Silvally, Eternal
      Floette. Their chat spelling is unknown.
    - **The rule:**
      1. an exact match of the chat's name against the alt-dex page's names;
      2. only when there is no exact match, the long regional prefixes become the short ones
         ("Hisuian " → "His ", "Alolan " → "Alo ", "Galarian " → "Gal ", "Paldean " → "Pal "),
         and the result is matched exactly again. The backend's alias map is the evidence that
         the chat once used the long names;
      3. one log line per process and name when a spawn's name looks like a form (a form prefix,
         or a trailing "(…)") and matches no alt-dex name. It holds no user data, so the first
         event with regional forms shows their spelling.

      No apostrophe or accent is substituted.

**Accepted on 2026-11-25, after the backend was written:**

15. **`/upload_alt_dex` stores nothing while the category is off on the server** (*a*, *When
    the server refuses*). It answers 409 `alt_dex_alerts_off` unless a device record has the
    category on for that profile. For the app this means:
    - turning the category on sends the selection first, and the upload only after the
      server has acknowledged that selection;
    - a 409 is shown to the user with a string of its own and is never retried
      automatically;
    - a unit test covers that order.

## What stays exactly as it is

- **Reading 3 itself:** "Update Alt Dex", `PcgAltDexReadingGate`, `pcg_dex_reader.js` and store 3
  (`PcgAltDexSnapshotStore`), as `docs/design-pcg-dex-readings.md` describes them. Nothing here
  changes what a reading accepts or stores.
- **Reading 1 and `/upload_dex_list`:** the request, the handler, the profile's dex file and the
  missing-dex alert. The alt dex gets its own endpoint and its own file (*a*, *b*).
- **The probes:** `content.js`, `pcg_tab_state_scout.js`, `pcg_probe_inventory.js` and
  `pcg_dex_reader.js` stay byte for byte. "Update Inventory" and "Update Pokédex" are not
  touched (`AGENTS.md`, ruling of 2026-11-18).
- **Most Wanted, the four ordinary modes and event spawns:** same storage, same matching, same
  notification text.
- **The main dex and the alt dex are never merged**, in any store, upload, key or rule.

**PCG gameplay stays manual.** The alert observes and notifies. No reading, upload, store change
or alert sends a chat command, queues one or triggers anything.

## Summary of the choices

| | Choice | Section |
|---|---|---|
| Upload | the last reading's missing forms (raw names) and `read_at`, by a new endpoint `/upload_alt_dex`, Bearer only; sent only while the category is on for that profile on this phone, and after the server has acknowledged it; refused with 409 `alt_dex_alerts_off` while the server has it off (decision 15) | *a* |
| Server storage | `alt_dex_lists/<profile_id>.json`, its own file and lock, written atomically; never touched by `/upload_dex_list`; deleted by profile data deletion and when the category is off on every device | *b* |
| Matching | exact: the chat's name against the page's names, then, only without an exact match, the long regional prefixes made short and matched exactly again (decision 14); a log line for a form-like name that matches nothing; the 371 page names generated from the app's asset into a table both sides test against | *c* |
| Category | a per-profile boolean in the device record, `profile_alt_dex_form_alerts`, default false, beside `profile_event_spawn_alerts`; one target per profile and device, as today | *d* |
| Notification | its own title and body, in the existing four channels; no new channel | *e* |
| App | one checkbox in the bell menu, a hint when no reading exists, the upload after an accepted reading and when the category is turned on | *f* |
| Privacy | new sentences in privacy and data deletion, both copies; `CURRENT_TERMS_VERSION` 3 → 4, because data is newly collected | *g* |
| Order | the backend (with #5's fixes and the watchlist deletion), the policy web copy, the app release | *h* |
| Most Wanted deletion | profile data deletion deletes the profiles' watchlists on every device; no other path changes | *i*, fix 3 |

## Facts the design rests on

| Fact | Provenance |
|---|---|
| Store 3 holds, per profile, the obtained forms (they only grow) and the last reading's missing forms, each as normalized key, raw name and sprite number, and `readAtMs` (`PcgAltDexSnapshotStore.kt:18-22, 33-84`). | Read in code |
| Nothing of the alt dex leaves the phone today: `saveManualAltDexReading` stores and shows a toast, and says "Nothing is uploaded" (`GeckoSessionManager.kt:895-947`). | Read in code |
| The alt dex has 371 forms: 349 are catalogue names, 22 are not (11 Mega, 4 PCG, 2 Clone and 5 others). 46 carry 🔒. | Observed on 2026-11-23; the counts read again from `pcg_alt_dex_forms.json` (blob `651a8fd`) and computed |
| The spawn name the backend sees is what PCG's chat message carries between "A wild" and "appears" (`spawn_message_classifier.py:13-16`, `SpawnMessageParser.kt`). | Read in code |
| The Smart Catch panel matches the Form line by the catalogue's name for the spawn first, then by the chat's name (`QuickCatchProfileSpawnContext.kt:120-140`). The catalogue's name comes from `PokemonTypeDexCatalog.find`: the exact name, else an alias owned by exactly one entry (`PokemonTypeDexCatalog.kt:32-38, 124-139`). | Read in code |
| PCG's chat prints every alt-dex form that spawned from 2026-10-18 to 2026-11-25 exactly as the page names it: 116 names, 80 "(F)" and 36 other "(…)" forms. No prefixed form spawned, so how the chat prints "Alo Vulpix" is unknown. | Observed by the owner in the production backend's logs (decision 14); not seen by whoever wrote this note |
| The page's 371 names carry no 🔒 or 🎊 in the asset (the lock is a separate `locked` field), no leading, trailing or doubled spaces, and are distinct. The backend collapses whitespace in a chat message before it cuts out the name (`spawn_message_classifier.py:27-34, 37-45`). | Computed from `pcg_alt_dex_forms.json` (blob `651a8fd`); read in code |
| `/upload_dex_list` accepts the legacy key (`twitch_cache_bot_v10_token_safety.py:2222-2226`), stores a missing `wanted_pokemon` as an empty list (`:2235`) and rewrites the profile's file with four fields, with a plain `open(path, "w")`, outside `DEX_LOCK` (`:971-1006`, write at `:1002`). A field it does not know is dropped and the answer is still 200. | Read in code |
| A Bearer session is bound to its profile: any `profile_id` in the payload that differs from the session's is refused with 403 `profile_mismatch` (`backend_auth_policy.py:117-133`). | Read in code |
| Missing-dex matching reads every file in `dex_lists/` at each spawn and compares by `main_dex_key`, which merges every form of a species (`fcm_sender.py:1079-1099, 1161-1186`; `dex_interest_store.py:45-171`). | Read in code |
| Most Wanted compares by `registration_key`, which merges genders and seasons through the alias map: "Deerling (Autumn)" and "Venusaur (F)" are among the 109 alt-dex names it maps to another name's key, most of them to the base species (`fcm_sender.py:1035-1054`; `pokemon_registration_matcher.py:124-171`). | Read in code; the 109 computed against `variant_alias_maps_for_bot.json` |
| The ordinary spawn's recipients are chosen in one loop over devices and their profiles. A profile is a target when its mode matches, or when its device-scoped Most Wanted list matches, which bypasses the mode, `SPAWN_ALERT_MODE_NONE` included (`fcm_target_selector.py:488-601`, bypass at `:556-560`). | Read in code |
| One spawn gives at most one target per profile on each device token: `dedupe_targets` keeps the first target of each profile (`fcm_target_selector.py:116-168`, applied at `:584-599`). Its kind is `most_wanted` when Most Wanted matched, else `wanted` (missing dex), `a_tier` or `all_spawn`, in that order (`:570-580`, `:98-113`). One reminder follows, rebuilt from the same rules (`fcm_sender.py:1635-1657`). | Read in code |
| Event spawns take another pipeline, which knows missing-dex and event targets only: Most Wanted does not apply there (`spawn_notification_router.py:19-58`; `fcm_target_selector.py:253-428`). | Read in code |
| A device record holds, per profile, `profile_spawn_alert_modes` (0 to 3) and `profile_event_spawn_alerts` (boolean, default false); a profile stays in `profile_ids`, the delivery list, while any category is active (`spawn_alert_modes.py:3-20`; `registered_device_service.py:75-78, 201-222`). | Read in code |
| `set_spawn_alert_mode` keeps the stored event preference when the request omits `event_spawn_enabled` (`push_endpoint_service.py:239-240`; `registered_device_service.py:192-200`). | Read in code |
| The app shows the payload's `title` and `body` as they come, whatever `alert_kind` says, and picks the channel from the local sound and vibration settings alone (`MyFirebaseMessagingService.kt:157-163, 262`; `PcgNotificationChannelManager.kt:145-155`). | Read in code |
| Profile data deletion removes the profiles' `dex_lists` files, and records the deletion notice only when every step succeeded (`twitch_cache_bot_v10_token_safety.py:681-692, 735-747`). | Read in code |

## a) The upload

### What the app sends

After an accepted "Update Alt Dex", and only while the category is on (*When*):

```json
{
  "profile_id": "<profile id>",
  "profile_label": "<Twitch username>",
  "read_at": "2026-11-23T08:12:40Z",
  "missing_forms": ["Alo Vulpix", "Mega Venusaur", "PCG Absol"]
}
```

- **`missing_forms`:** store 3's `missing`, the forms the last accepted reading listed, by raw
  name as the page shows it, without 🔒 or 🎊 (`PcgAltDexKnownForms.kt:59-68` already removes
  them). Every form is sent, rare ones included (decision 4). No filter.
- **`read_at`:** store 3's `readAtMs`, as UTC ISO-8601 with a `Z`.
- **`profile_label`:** as `/upload_dex_list` sends it today (`FcmRegistrationUploader.kt:806-810`),
  because the notification body names the account by it (*e*).
- **Not sent:** the obtained forms, the sprite numbers, the normalized keys and the device
  identifier. The server needs only the missing names to match a spawn, and matches them
  exactly (*c*). Less leaves the phone, and the server cannot drift from a key the app computed.

**What "missing" means on the server.** It is the last reading's list, exactly as store 3 keeps
it. So, as for the main dex today:

- a form obtained after the reading still alerts, until the next reading replaces the list;
- a form PCG adds after the reading does not alert, until a reading lists it. Every form not
  obtained is on the page, new ones included, so one "Update Alt Dex" after PCG adds forms
  covers them. This matters for the Christmas event (*Timing*).

### By which endpoint

| | A new optional field of `/upload_dex_list` | A new endpoint `/upload_alt_dex` |
|---|---|---|
| Reading 1's path | changes: an alt-dex-only call omits `wanted_pokemon`, which today stores an empty list and wipes the main-dex list (`:2235`); so the handler must switch to "omitted means keep", and to read-merge-write | untouched |
| An app that runs ahead of the backend | the old handler drops the field and answers 200: the app reads "sent" while nothing was stored, with no error anywhere | the old server has no route: the app gets a visible failure, never a false success |
| Authentication | the endpoint accepts the legacy key (`:2225`), and the new field would inherit that | Bearer only (`allow_legacy=False`) from its first day, as `/delete_server_data` (`:1927-1931`) |
| Storage and deletion | one file per profile holding both dexes, read-merge-write | its own file, its own lock, its own deletion step |
| Cost | none in routing | a route (`:1605` pattern), an entry in `RATE_LIMITED_WRITE_PATHS` (`:1246-1255`), a handler |

**Recommended: the new endpoint `/upload_alt_dex`.** It leaves reading 1's endpoint as it is, it
fails loudly against a server that does not know it, and it never accepts the legacy key.
Reading 2's `outside_spawnable` field (`docs/design-pcg-dex-readings.md`, *The backend field*) is a
main-dex list and stays designed as a field of `/upload_dex_list`. The two choices are
independent.

**The handler:**

- Bearer session required; the payload's `profile_id` is bound to it by `authorize_backend_request`.
- `missing_forms` must be a list of strings: at most 1000 names (371 today, with room for new
  forms), each at most 120 characters (the limit Most Wanted uses,
  `pcg_custom_watchlist_store.py:17`). Names are trimmed; blank and repeated names are dropped. An empty list
  is accepted: it means nothing is missing.
- `read_at` must parse as UTC ISO-8601 and not lie more than a day in the future; otherwise 400.
- Names are **not** checked against a catalogue. Most Wanted refuses unknown names
  (`pcg_custom_watchlist_store.py:253-290`), but the alt dex is exactly where PCG adds forms the
  catalogue does not know: 22 already.
- **409 `alt_dex_alerts_off`** unless at least one device record has the category on for that
  profile (decision 15). The check and the write both run under `REGISTRY_LOCK`, the lock every
  write of the category takes. An upload racing a switch-off therefore either is refused or
  lands before the switch-off, whose cleanup then deletes it (*b*). It never leaves behind a
  list that no device wants. Nothing is stored.
- Answer 200 `{"ok": true, "count": <n>, "read_at": "<read_at>"}`.
- Log one line with no names: `[ALT_DEX] saved profile_ref=<ref> missing_count=<n>`.

### When

**Recommended: only while the category is on for that profile on this phone.** Two moments:

1. **After each accepted "Update Alt Dex"**, if the category is on for the reading's profile.
2. **Once when the user turns the category on**, if store 3 holds a reading for that profile.
   The user does not have to read the page again. **The order is fixed (decision 15):**
   1. the selection first: `set_spawn_alert_mode` with `alt_dex_form_alerts_enabled: true`;
   2. the upload only after that request succeeds, which is the moment the app records the
      acknowledgement (`FcmRegistrationUploader.kt:315-332`);
   3. if the selection fails, no upload is sent.

   Sent the other way round, the upload would meet a server that still has the category off and
   get a 409 every time. In the bell menu's handler this is one more step after the delivery
   step, and the handler already stops at the first step that fails
   (`ChatFragment.kt:4208-4247`) (*f*).

Why not always:

- **The data has one purpose, the alert.** With the category off the server would hold a list it
  never uses. Decision 2 makes the category off for everyone, so "always" would upload for
  every user who reads the alt dex, nearly all of them for nothing.
- **The policy can then say so plainly:** nothing about the alt dex is sent while these alerts are
  off on this phone (*g*). That is easier to read and to keep true than a sentence about lists
  sent for a feature the reader does not use.
- **The cost** is one more upload when the category is turned on, which the stored snapshot
  covers.

A failed upload is not retried by itself: the toast says it failed, and the next reading or the
next time the category is turned on sends it again. This is not a gameplay command, so the rule
against queues does not forbid a retry, but nothing gains from one: no alert can be owed for a
spawn that has not happened yet.

### When the server refuses: 409 `alt_dex_alerts_off`

**What it means:** the server has the category off for that profile on every device record, so
it stored nothing. The phone may still have the category on. Two ways to get there:

- the server removed this device's record after Firebase reported its token dead
  (`remove_registered_device_by_token`), and the record a later registration creates starts
  with the category off;
- an upload raced a switch-off on this phone.

**What the app does** (decided by the owner on 2026-11-25, after this note first proposed
"reopen TMC" and no change on the phone; implemented in A1):

- **Shows it with its own string,** never with the generic failure:
  `pcg_alt_dex_alert_upload_alerts_off`. Each of the two moments of *When* can show it. The
  string sends the user to "PCG notifications" to press OK; "Reopen TMC" is gone. A2's final
  wording serves both moments: "Alt Dex saved on this phone, but not sent for alerts: the server
  has not confirmed Missing Alt Dex form alerts for this account. Open PCG notifications and press
  OK to try again." After the bell menu's OK it follows the selection's own toast.
- **Marks the category unconfirmed:** it writes the acknowledged alt-dex flag false in
  `PcgProfileAlertAcknowledgementStore`, and only when an acknowledgement already exists for the
  profile. Without one the baseline below is already unconfirmed, so nothing is written. The
  local category is not touched, so the checkbox keeps showing it on.
- **The bell menu compares against the confirmed baseline,** the local selection with the
  category counted on only when it is on here *and* acknowledged on
  (`PcgProfileAlertSelection.confirmedBy`). The menu's OK, and the handler's plan and toast, use
  it. So the next OK sends `FIREBASE_DELIVERY`, then `ALT_DEX_UPLOAD`, although nothing visible
  changed.
- **A delivery answer alone never confirms the category again.**
  `PcgProfileAlertAcknowledgementStore.record` keeps the flag as it was for a selection with the
  category on, and lowers it for one with it off. Only the turn-on's upload step raises it: when
  the list is stored, or when there is no reading to send.
- **"Update Alt Dex" with the category on but unconfirmed** stores the reading, sends nothing and
  shows the same string.
- **Never retries it automatically:** no second upload, no queue, no entry in the owed-work
  store. The flag only decides what the user's next OK sends.
- **Logs** the result only: no name, label or profile identifier.

**Why the boot pass and the owed-work policy stay correct** (reasoned from the code, and pinned
by unit tests):

- The boot pass registers and restores every profile whose selection needs delivery whatever
  its acknowledgement (`PcgProfileRegistrationSyncPlanner`), and the category on makes delivery
  needed. A lowered flag therefore changes nothing there. The boot pass does not upload, and its
  answer does not raise the flag.
- For a removed profile, `OwedProfileDisablePolicy` still owes a disable while any other category
  is acknowledged on. An acknowledgement whose only active category was this one reads as
  `DISABLED` after a 409, and then owes nothing by itself. That is the server's own answer: the
  category is off on every device record, and the others were acknowledged off. A failed
  disable is still owed through its marker. A delivery whose answer was lost leaves the
  acknowledgement stale in the same way for every category, today as before.
- Since F3's PR-2 (#117), the two writes, raise and lower, are made under lock L with the
  acknowledgement write after a delivery, each through a file obtained inside L (R3; see
  `OwedWorkCoordinator`).

## b) Server storage

- **Where:** `alt_dex_lists/<profile_id>.json`, a directory beside `dex_lists/`
  (`twitch_cache_bot_v10_token_safety.py:143`), with its own lock, `ALT_DEX_LOCK`.
- **What:**

  ```json
  {
    "schema_version": 1,
    "profile_id": "<profile id>",
    "profile_label": "<Twitch username>",
    "read_at": "2026-11-23T08:12:40Z",
    "uploaded_at": "2026-11-23T08:12:41Z",
    "missing_forms": ["Alo Vulpix", "Mega Venusaur", "PCG Absol"]
  }
  ```

- **Separate from `wanted_pokemon` and from every main-dex field.** It is another file in another
  directory. `DexInterestStore` reads every `.json` in `dex_lists/` (`dex_interest_store.py:68-79`),
  so a file there would be read as a main-dex snapshot; a directory of its own keeps it out.
- **An upload that omits it keeps it.** `/upload_dex_list` never opens this file, so reading 1's
  uploads, as they are today, cannot touch it. Each `/upload_alt_dex` replaces the whole document:
  it holds one reading, and the reading is complete by the gate's checks.
  - Had the owner chosen the field of `/upload_dex_list`, `save_profile_dex_list` would have to
    become read-merge-write under `DEX_LOCK`, keeping every field the upload omits.
- **Atomic:** written to a temporary file in the same directory, flushed and `fsync`ed, then
  `os.replace`d over the old one, as `PcgCustomWatchlistStore._save_store` already does
  (`pcg_custom_watchlist_store.py:348-376`). A crash leaves the old list or the new one, never half
  of either.
- **Removed by profile data deletion:** `delete_server_app_data` deletes `alt_dex_lists/<id>.json`
  for each profile in scope, beside the `dex_lists` files (`:681-692`). A failure joins `errors`,
  so no deletion notice is recorded (`:735-747`), and the payload gains `deleted_alt_dex_count`.
- **Also removed when no device has the category on for that profile** (decision 9). The upload
  is sent only while the category is on (*a*), so the list does not outlive it. After any write
  that can turn the last flag off, the server deletes the list of each affected profile that no
  device record still has on:
  - `set_spawn_alert_mode` (the user turns it off, or the app removes the account);
  - `delete_device_server_data` (`:758`), "Erase everything and remove this device";
  - the removal of a device whose token Firebase reports dead (`fcm_sender.py:1124-1158`).

  The check and the deletion run while `REGISTRY_LOCK` is held, taking `ALT_DEX_LOCK` inside it,
  always in that order, so that an upload racing a switch-off cannot leave a list behind for a
  profile no device has on. A failed deletion is logged with the profile's reference only, and is
  retried by the next of these writes for that profile.

## c) Matching

### The rule (decision 14)

The owner read the chat's names in the production backend's logs (decision 14): every alt-dex
form that spawned appears with exactly the page's name. So the match is exact, and nothing is
normalized. There is no case folding, no Unicode normalization, and no apostrophe or accent
substitution.

A new module, `alt_dex_form_names.py`, used only for this alert, holds `alt_dex_form_key(name)`,
which is the name with surrounding whitespace trimmed and nothing else, and resolves a spawn's
name in two steps:

1. **Exact.** If the chat's name is one of the page's names, as the generated table lists them
   (*Keeping the list in step*), that name alone is the candidate.
2. **Long regional prefixes, only without an exact match.** Each whole word "Hisuian", "Alolan",
   "Galarian" or "Paldean" followed by a space becomes "His", "Alo", "Gal" or "Pal". If that
   changes the name, the result is a second candidate, matched exactly too. The words are
   matched as the chat would print them, capitalised, and wherever they stand before a word, so
   "Sin Alolan Marowak" gives "Sin Alo Marowak" as well as "Hisuian Zorua" giving "His Zorua".
   - **Evidence:** the backend's alias map, which turns 58 long regional names into the page's
     short ones (`variant_alias_maps_for_bot.json`, `combined_aliases`; computed), shows that the
     chat once used the long names (decision 14). No regional form spawned in the logs the owner
     read, so neither spelling has been seen since.
   - **No page name contains those four words** (computed over the 371), so step 2 can never turn
     one page name into another.

A profile is a target when one of the candidates is, exactly, a name of its uploaded list. A name
the generated table does not hold still matches exactly in step 1, so a form PCG adds before the
backend's table is regenerated still alerts the profiles whose reading lists it.

**What this rule never does:** it never maps a form to its base species or to another form. It
never calls `registration_key`, which merges genders and seasons, `main_dex_key`, which merges
every form of a species, or `a_tier_key`. "Venusaur (F)" matches only "Venusaur (F)", "Deerling
(Autumn)" only "Deerling (Autumn)", "Unown (F)" only "Unown (F)". The main-dex forms the chat also
prints with a suffix, such as "Shellos (West)" and "Palafin (Zero)", are on no alt-dex page,
appear in no uploaded list, and so match nothing (decision 14).

**The uploaded names are stored as sent**, trimmed of surrounding whitespace. The app sends them
as the page shows them, without 🔒 or 🎊 (*a*), and the asset holds them that way (*Facts*).

### A log line for names that look like forms (decision 14)

When a spawn's name looks like a form and resolves to no name of the generated table, the backend
logs, once per process and name:

```
[ALT_DEX] form-like spawn name matches no alt-dex form name='<name>'
```

- **"Looks like a form":** it ends in "(…)", or its first word is one of the page's form prefixes
  or their long regional spellings: Alo, Alolan, Gal, Galarian, His, Hisuian, Pal, Paldean, Mega,
  PCG, Sin, Eternal. A name of two words ending in "Arceus" or "Silvally" counts too, for the typed
  forms. Every one of the 371 page names passes this test (computed), so a form whose chat
  spelling differs from the page's still looks like a form, unless the chat drops its prefix and
  its parentheses together.
- **What it records:** the spawn's name only, which is public chat text. No profile, device or
  user.
- **Bound:** the names already logged are kept in memory, at most 2000, so that a spawn name
  already logged is not logged again. When the set is full, nothing more is logged until the
  process restarts.
- **The value that separates the cases** is the name itself: it never fires for a page name, nor
  for a long regional spelling of one (step 2 resolves those). It fires once for each suffixed
  main-dex form such as "Shellos (West)", which is expected, and for any spelling of a form that
  the rule misses. The first event with regional forms shows their spelling.

### Matching at a spawn

`load_alt_dex_interested_profiles(pokemon_name)`, beside `load_interested_profiles`
(`fcm_sender.py:1161-1186`):

- resolves the spawn's candidates once;
- reads each file in `alt_dex_lists/`, skipping a malformed file on its own, as
  `DexInterestStore` does (`dex_interest_store.py:81-99`);
- returns `{profile_id: profile_label}` for each list holding a candidate.

Every spawn reads the directory, as `dex_lists/` is read today; at today's scale that is the
existing cost, once more.

### Keeping the list in step

The server matches the names each profile uploaded, so it does not need the page's list to alert.
The table of the page's names serves the exact-first step, the log line and the tests:

- **On the backend,** a generator, `generate_alt_dex_forms_table.py`, reads
  `app/src/main/assets/pcg_alt_dex/pcg_alt_dex_forms.json` from git at a given app commit
  (`git show <commit>:<path>`). It writes `alt_dex_forms_v1.json`: the 371 names in the asset's
  order, the app commit, and the asset's blob. Its `--check` mode fails when the committed table
  differs from what the generator would write. It follows `generate_main_dex_species_table.py`,
  which does the same for the species table. A backend test checks that each table name resolves
  to itself, exactly, in step 1.
- **Shared with the app:** the app's pull request commits the same file, byte for byte, as
  `app/src/test/resources/pcg_alt_dex/pcg_alt_dex_forms_v1.json`. A Kotlin test checks that it
  lists exactly the bundled asset's names, in order, and that the uploader sends each of them
  unchanged. The two sides then test against one list, and a change to the asset that the other
  side has not taken fails a test.
- **When PCG adds forms,** the app's asset and its copy of the table change in one app pull
  request, and the backend regenerates its table in the next backend change. Until then nothing
  breaks: the new names still match exactly in step 1, and only the log line fires for them.

## d) The alert category

### Storage

- **In the device record,** beside the event preference: `profile_alt_dex_form_alerts`,
  `{profile_id: bool}`, with `DEFAULT_ALT_DEX_FORM_ALERT_ENABLED = False` in `spawn_alert_modes.py`.
  It is per profile and per device, like the ordinary mode and the event preference, because each
  phone chooses its own alerts.
- **The request field:** `alt_dex_form_alerts_enabled` on `set_spawn_alert_mode`, optional and
  boolean, like `event_spawn_enabled` (`push_endpoint_service.py:239-271`):
  - present: stored;
  - present and not boolean: 400 `alt_dex_form_alerts_enabled_must_be_boolean`;
  - absent and `enabled` false: stored as false (*Old apps*);
  - absent otherwise: the stored value is kept.
- **The answer** echoes `alt_dex_form_alerts_enabled` and `profile_alt_dex_form_alerts`, beside the
  event fields (`push_endpoint_service.py:313-328`).
- **Everything that treats the event preference as a category must treat this one too:**
  - `_is_active` (`registered_device_service.py:75-78`), so that a profile with mode 3 and this
    category on stays in `profile_ids`, at `set_spawn_alert_mode` (`:201-222`) and at
    `register_fcm`'s `upsert_registered_device` (`:129-141`). Otherwise every registration pass
    would take the profile out of the delivery list;
  - a new profile's record starts with the default (`:100-112`, `:123-128`);
  - `registered_device_profile_ids` and `remove_profiles_from_device`
    (`device_profile_scope.py:33-39, 109-123`), so that deletion scopes and removes the new map
    like the other two.
- **Default off** (decision 2): absent means off, for every record that exists today and for every
  new one. No migration.

### Old apps

- **They never send the field,** so they can never turn it on.
- **They show the alert if one reaches them,** because they display the payload's title and body
  as given (*Facts*). That happens only after a downgrade.
- **After a downgrade, the old app must still be able to stop it.** A user who turned it on in the
  new app and went back to an older one has no checkbox for it. The old app's "all off", and its
  account removal, send `enabled: false` without the new field (`FcmRegistrationUploader.kt:278-284`).
  So an absent field with `enabled` false stores false. The new app always sends the field, so
  this rule never overrides it.
- **An app newer than the server:** an old server ignores the field and keeps the profile in
  `profile_ids` because the app's `enabled` is true when only this category is on (*f*). No form
  alert is sent, since the old server has no lists. This order is excluded anyway (*h*).

### One alert per spawn, profile and device

The form check joins the existing loop of `select_token_targets_for_spawn`
(`fcm_target_selector.py:488-601`). It does not get a pass of its own, so `dedupe_targets` still
leaves one target per profile on each token.

- **A profile is a target when** its mode matches, or its Most Wanted matches, **or** the spawn is
  in its alt-dex list and its device has the category on. The form condition, like Most Wanted,
  ignores the mode, mode 3 included (decision 5).
- **The target's `alert_kind`,** the first that applies: `most_wanted`, `wanted` (missing main
  dex), `alt_dex_form`, `a_tier`, `all_spawn`. The order keeps today's for the four kinds that
  exist. The new kind sits below the main dex, because a species missing from the main dex is the
  stronger reason, and an old app already knows `wanted`.
- **Every reason that applied is listed** in the payload's `notification_reasons`, with
  `missing_alt_dex_form` for the new one, and the boolean `is_missing_alt_dex_form` is carried on
  the target. `dedupe_targets` copies only the booleans it names (`fcm_target_selector.py:156-164`),
  so the new one must be added there, or it is dropped.
- **The reminder** is rebuilt by the same rules (`fcm_sender.py:1635-1657`), so it follows the same
  choice.
- **A device with two profiles** gets one alert per profile, as today. "One" is per profile and
  device.

**Event spawns are included** (decision 8). The event pipeline (*Facts*) is separate, and Most
Wanted does not reach it. If PCG's Christmas spawns arrive as event spawns, under the configured
event rule, a form alert limited to ordinary spawns would miss exactly the forms the owner wants
for the event. In `select_token_targets_for_event_spawn` (`fcm_target_selector.py:253-428`) the
form condition joins the missing-dex branch: one target per profile, which also suppresses the
token's event-only target, as missing dex does today (`:398-413`). The target keeps
`is_event_spawn`, so the payload builder's event handling (`fcm_notification_payload.py:177-201`)
must give `alt_dex_form` precedence over `event` for the title and body, as it already gives
missing dex, and list `event_spawn` among the reasons. `build_token_targets_for_event_spawn`
(`spawn_notification_router.py:19-37`) loads the alt-dex profiles beside the missing-dex ones.

## e) The notification

**Text**, built by the server as every PCG alert's text is (`fcm_notification_payload.py:12-121`):

| | Title | Body |
|---|---|---|
| Initial | `{pokemon} appeared (Missing form)` | `Missing Alt Dex form • {label}` |
| Reminder | `{pokemon} (Missing form) is fleeing soon` | `Missing Alt Dex form • {label}` |

- `{pokemon}` is the name the chat gave, `{label}` the profile's label from the alt-dex list.
- The text is in English, like every server-built alert today. It is not in `strings.xml`
  because the server writes it and the app displays it as given.
- `alert_kind` is `alt_dex_form`, `missing_dex` is "0", `event_spawn` as today. The payload builder
  must not fold the new kind into `wanted`: it does so only for a target marked missing from the
  main dex (`fcm_notification_payload.py:164-201`).

**Channel: the existing four, no new one.**

- TMC's channels are delivery behaviours, not categories: silent, sound, vibration, and sound
  with vibration, chosen at display time from the user's settings in the bell menu
  (`PcgNotificationChannelManager.kt:14-26, 145-155`). Every PCG alert, Most Wanted and event
  included, goes through them.
- Android freezes a channel's sound, importance and vibration when it is created. That is why
  the ids end in `_v5` and older ones are retired (`:34-49`). The new alert needs no behaviour the
  four channels lack, so nothing is frozen wrongly, and none needs replacing.
- **A new channel would be needed only** if the owner wanted this alert to sound or vibrate
  differently, or to be switched off in Android's settings apart from the others. Then it would
  need four channels, one per behaviour, each in `currentIds`: `deleteObsoleteChannels` deletes
  any channel whose id starts with `pcg_alerts_` and is not listed (`:120-139`). Not proposed.

## f) The app

- **The bell menu** ("PCG notifications", `PcgSpawnAlertModeMenu.kt`) gets one checkbox after
  "Enable Most Wanted alerts" (`:92-97`): `pcg_alt_dex_form_alert_label`, "Enable Missing Alt Dex
  form alerts". The bell button's description names it `pcg_alt_dex_form_alerts_name`, "Missing
  Alt Dex form alerts", as it names Most Wanted. One name everywhere: the owner's review of #148
  replaced the earlier "Notify missing Alt Dex forms". It is applied, like the others, only on OK
  (`:146-158`).
- **The selection:** `PcgProfileAlertSelection` gains `altDexFormAlertsEnabled`
  (`PcgProfileAlertSelection.kt:12-33`):
  - `requiresFirebaseDelivery` counts it;
  - `DISABLED` sets it false;
  - its store is a per-profile boolean like `PcgEventSpawnAlertStore`, default false;
  - `PcgProfileAlertAcknowledgementStore` records it with its own key prefix. An entry written
    before it existed reads as false, which is the server's default too.
- **The request:** `setProfileSpawnAlertModeBlocking` sends `alt_dex_form_alerts_enabled` beside
  `event_spawn_enabled` (`FcmRegistrationUploader.kt:270-285`).
- **The upload:** a focused client, `PcgAltDexAlertUploader`, using `BackendAuthHeaderProvider`
  as the dex upload does (`FcmRegistrationUploader.kt:797-835`). Bearer only: with no session or
  an unusable one, it sends nothing and never falls back to the legacy key. It is called:
  - by `saveManualAltDexReading` (`GeckoSessionManager.kt:897-947`), after the store is written,
    if the category is on for the reading's profile;
  - by the bell menu's handler, after the backend acknowledged a selection that turned the
    category on, if store 3 has a reading for that profile.
- **The order, in the planner** (decision 15): `PcgProfileAlertSyncStep` gains
  `ALT_DEX_UPLOAD`. `PcgProfileAlertSyncPlanner.buildPlan` (`PcgProfileAlertSelection.kt:152-199`)
  adds it as the plan's last step, after `FIREBASE_DELIVERY`, only when the category goes from
  off to on. The plan always holds `FIREBASE_DELIVERY` then, because the selection changed.
  - The handler runs the upload step only if a reading is stored, and only if every earlier step
    succeeded: it already breaks on the first failure (`ChatFragment.kt:4243-4246`).
  - The selection's toast compares confirmed selections, so the category counts as saved only
    once the upload step has confirmed it. A 409's own string follows that toast (*When the
    server refuses*). This replaces "the upload's result does not count as part of the
    selection", by the owner's decision of 2026-11-25.
  - The order is a pure function, so a unit test can hold it (*j*).
- **What the user sees:**
  - after a reading, with the category on: `pcg_alt_dex_updated_and_sent`, "Alt Dex saved and sent
    for alerts. Forms not obtained: %d.", in place of today's `pcg_alt_dex_updated`;
  - if the upload fails: `pcg_alt_dex_alert_upload_failed`, "Alt Dex saved on this phone, but it
    could not be sent for alerts. Press Update Alt Dex again later.";
  - if the server answers 409 `alt_dex_alerts_off`: `pcg_alt_dex_alert_upload_alerts_off`, the
    string of *a* (*When the server refuses*), never the generic failure, and never retried;
  - with the category off: today's toast, unchanged;
  - after the bell menu's OK, a turn-on upload that fails adds no toast of its own: the selection's
    "Only some alert settings were updated" already says it, and the category stays unconfirmed,
    so the next OK sends it again. `pcg_alt_dex_alert_upload_failed` would be wrong there, since
    it sends the user to "Update Alt Dex", which sends nothing while the category is unconfirmed
    (A2, `PcgAltDexAlertSync.messageAfterTurnOn`).
- **The category on, no reading on this phone** (decision 3):
  - under the checkbox, while it is ticked and store 3 has no reading for the profile:
    `pcg_alt_dex_form_alert_no_reading`, "No Alt Dex reading on this phone yet. In the PCG
    Pokédex, switch to Alt Dex, turn Obtained off and press Update Alt Dex.";
  - after OK, a short form of it as a toast, once: `pcg_alt_dex_form_alert_no_reading_toast`,
    "No Alt Dex reading yet. Press Update Alt Dex in the PCG Pokédex." The full text was cut
    to two lines in a toast (device round of 2026-11-27).
  - The setting is still saved: decision 3 makes it harmless, and the first reading then uploads.
- **Text that says the alt dex never leaves the phone** must change in the same pull request: the
  KDoc of `saveManualAltDexReading` (`GeckoSessionManager.kt:895`) and of `PcgAltDexSnapshotStore`
  ("Nothing here leaves the phone"), and `docs/design-pcg-dex-readings.md`.
- **Logging:** counts and results only. No form name, no list, no token
  (`PrivacySafeLoggingSourceTest` covers the new lines).
- **Gameplay stays manual:** the checkbox, the upload and the alert send nothing to chat.

## g) Privacy and terms

### The sentences

Both copies change in one step, with the same wording and the same "Last updated" date:
`app/src/main/assets/policies/` and `tmc/` in `unouidol/unouidol.github.io`, whose file names use
hyphens (`AGENTS.md`). On 2026-11-25 both copies read "Last updated: 2026-11-13" for the privacy
policy and "2026-11-18" for data deletion (the web copies read through GitHub's API).

**`privacy.html`, section 2, a new item after the one on alert settings (line 42):**

> When you turn on Missing Alt Dex form alerts for an account: the list of forms missing from that
> account's Alt Dex, as the last Update Alt Dex on this phone read it, with the time it was read,
> the account's identifier and its Twitch username. It is sent to the TwitchMiniChat server after
> each Update Alt Dex, and once when you turn these alerts on, so that the server can tell you
> when a missing form spawns. Nothing about your Alt Dex is sent while these alerts are off on
> this phone.

**Section 4, the Pokémon Community Game item (line 66).** Its last sentence, from "What TMC reads
stays on your phone, with one exception", becomes:

> What TMC reads stays on your phone, with two exceptions. When you tap Update Pokédex, the list of
> spawnable Pokémon missing from your Pokédex is sent to the TwitchMiniChat server, with your
> account's identifier and Twitch username, so that the server can send the spawn alerts you
> choose. When Missing Alt Dex form alerts are on for an account, the list of forms missing from
> its Alt Dex is sent the same way, after each Update Alt Dex.

**Section 5, a new item** (with the removal of *b*, decision 9):

> The Alt Dex list sent for Missing Alt Dex form alerts is replaced by each new one. It is
> deleted when these alerts are off for that account on every device, and by "Delete app
> account and all data".

**Section 6, a new item:**

> Missing Alt Dex form alerts are off unless you turn them on, for each account, in the PCG
> notifications menu.

**`data_deletion.html`, coherence:**

- "Delete app account and all data" (line 89): "the Pokédex lists uploaded for those accounts"
  becomes "the Pokédex and Alt Dex lists and the Most Wanted watchlists uploaded for those
  accounts". The watchlists are named there because fix 3 (*i*) makes the code delete them, which
  line 52 already promises; nothing is withdrawn from the page.
- "Removing one account", *On the server* (line 118), a sentence after the existing ones: "The
  Alt Dex list uploaded for that account is deleted once no device has its Missing Alt Dex form
  alerts on."
- "Erase everything and remove this device" (line 52), the same sentence after "your other
  devices are not affected".

### The acceptance gate

**`CURRENT_TERMS_VERSION` goes from 3 to 4** (`TermsPrefs.kt:23`; decision 10), by the
criterion recorded in `RELEASE-STATE.md` on 2026-09-26:

> The gate is raised for a **material** change: data newly collected, a new recipient, or a new
> purpose.

- **Data newly collected: yes.** The server receives a list it never received: the forms missing
  from an account's alt dex. Today nothing of the alt dex leaves the phone (*Facts*).
- **A new recipient: no.** The data goes to the TwitchMiniChat server, which already receives the
  main-dex list, through Cloudflare, already named. Firebase already carries every alert's
  Pokémon name and account label; this alert's payload is of the same kind.
- **A new purpose: no.** It serves optional push alerts, section 3's existing purpose.

One new line on the gate, as version 3 did for GitHub (`strings.xml:231`):
`terms_gate_alt_dex`, "If you turn on Missing Alt Dex form alerts, TMC sends the TwitchMiniChat
server the list of forms missing from your Alt Dex, so it can alert you when one spawns. They are
off unless you turn them on." `TermsVersionTest` is updated to 4.

The opt-in does not change the verdict: the criterion asks what is collected, not how many users
choose it. The argument the other way, that nothing is collected until the user turns the
category on, was put to the owner, who decided for 4.

## h) Order of shipping

`AGENTS.md`: backward-compatible server behaviour before the app depends on it; the policy web copy
before the release that bundles the new text.

0. **The precondition:** *Open question 1* answered, which it is (decision 14). Code may start.
1. **Backend pull request**, deployed and checked before any app build sends anything:
   - `/upload_alt_dex`, the store, the exact match with its regional step, the generated table
     of the page's names and the log line;
   - the category field, its default and its old-app rule;
   - the selection in both pipelines, ordinary and event, and the payload's text;
   - deletion, and the removal when the category is off on every device;
   - #5's two fixes and the watchlist deletion (*i*).

   All of it is backward compatible: no client sends the new field or calls the new endpoint, the
   default is off, the #5 fixes turn a 403 into a 200 and stop writing a record no one reads, and
   the watchlist deletion only makes an existing request delete what the page says it deletes.
   The owner deploys it on the production machine; this note never touches it.
2. **Check it live**, by the owner on the production machine: the new route answers 401 without a
   session, `set_spawn_alert_mode` echoes the new field, and the registry and dex files are as
   before.
3. **The policy web copy, published** in `unouidol/unouidol.github.io`, with the bundled copy's
   exact text and date, and checked live, before the app release is published.
4. **The app release** (5.7.0): the bell checkbox, the upload, the bundled policies,
   `CURRENT_TERMS_VERSION` 4, and #141's logging if ready (independent of all this).

The app may merge to `main-v5` before step 3, since nothing is published by a merge; the release
may not be published before it.

## i) The backend fixes: tmc-backend#5's two, and the Most Wanted watchlists

**Fix 1: a registration may complete a record with no credential and no active profile.** The
correction below to #5's wording is accepted (decision 13).

- **The change,** in `preflight_enrollment` (`device_credential_service.py:129-191`): when the
  record has no `device_secret_sha256`, the request carries a valid Bearer session and a
  `device_secret` (both already required before preflight runs,
  `twitch_cache_bot_v10_token_safety.py:1669-1704`), and the record's `profile_ids` list, with the
  legacy `profile_id`, is empty, return as for a new device (`:160-161`). `register_fcm` then
  writes the profile, and `enroll` stores the hash (`:1716-1727`).
- **The condition must read the `profile_ids` field, not `registered_device_profile_ids`.** #5's
  comment words it with `registered_device_profile_ids(device)` being empty. That helper also
  returns the keys of `profile_spawn_alert_modes` and `profile_event_spawn_alerts`
  (`device_profile_scope.py:33-39`). The record this case starts from is written by a disabled
  selection for another profile, and holds that profile in both maps
  (`registered_device_service.py:214-239`). Read that way, the helper is not empty, the fix would
  not apply, and the 403 would stay. Read in code; the production record itself was not read.
- **What stays:**
  - the 403 `device_profile_mismatch`, for a credential-less record whose `profile_ids` lists
    other profiles (legacy records);
  - the 401 `invalid_device_credential`, for a record whose hash differs.
- **A case the fix leaves as it is:** if the record's maps already hold the registering profile
  with every category off, `upsert_registered_device` does not add it to `profile_ids`
  (`registered_device_service.py:116-141`), and `enroll` answers 403 (`device_credential_service.py:229-238`).
  That is today's behaviour, for a profile whose alerts are off. It clears itself when the user
  turns an alert on, because `set_spawn_alert_mode` adds the profile. A characterization test
  pins it.

**The takeover analysis, from #5.** To bind such a record, a caller needs:

- **a valid backend session for some profile,** which is no barrier: anyone gets one by signing in
  with their own Twitch account;
- **the record's `device_id`,** which is the real secret: a random UUID v4 (`DeviceCredentialStore.kt:38`,
  122 random bits), never logged or shown by the app, and excluded from backups. It travels in
  every request over HTTPS and is stored on the backend, in the registry, in `device_name` and in
  the Control Center.

That is the bar a brand-new device meets today: the first credentialed registration of an unknown
`device_id` binds it. A takeover would redirect nobody's alerts, because the record has no active
profile, and the answer echoes only the caller's own profile (`push_endpoint_service.py:184-193`).
The harm is a lock-out: the real installation is refused with 401 until its install identifier
changes.

**Fix 2: a disabled selection for an unknown device writes nothing.**

- **The change:** when `set_device_profile_spawn_alert_mode` finds no record and the selection is
  inactive (`registered_device_service.py:224-239`), it returns `mode_disabled_missing_device` and
  the same payload without appending the record or saving the registry.
- **Why nothing is lost:** no record already means no delivery. The app treats the 200 as
  acknowledged, and a later enable sends the whole selection again.
- **Compatibility:** the same 200, the same `result` string, the same fields.
- **Records already in that state** are completed by fix 1 at their next registration. No
  migration.

**Fix 3: profile data deletion deletes the Most Wanted watchlists** (decision 11).

- **What is wrong today** (read in code at `8ccf210`): `PcgCustomWatchlistStore` has
  `delete_profile`, which removes one profile from every device, and `delete_device`
  (`pcg_custom_watchlist_store.py:205-235`). Nothing outside the tests calls either, and
  `delete_server_app_data` does not open the store (`twitch_cache_bot_v10_token_safety.py:625-756`).
  `data_deletion.html` (line 52) lists watchlists among "the data belonging to your accounts" and
  sends the reader to "Delete app account and all data" to delete it. The production store was
  not read.
- **The deletion paths, from the code.** The backend has two, each with one caller:
  - `delete_server_app_data`, called only by `/delete_server_data` (`:1978`): "Delete app account
    and all data". **It must call `delete_profile`** for each profile in its scope, the profiles
    the server's own record of the device lists (`:1948-1957`). Per profile, on every device,
    because the page deletes "the data belonging to your accounts", as it deletes their dex lists
    whichever phone uploaded them.
  - `delete_device_server_data`, called only by `/delete_device_data` (`:2110`): "Erase everything
    and remove this device". **It must not call either method.** Its docstring keeps profile data
    (`:758-769`), and the page says the accounts' data, watchlists included, "is **not** deleted"
    (line 52). A watchlist whose device record is gone sends nothing: a target needs a device
    record with a token (`fcm_target_selector.py:500-520`).
  - "Removing one account" only sends `set_spawn_alert_mode`, and the page keeps watchlists there
    (line 118). Unchanged.
  - Nothing else in the backend deletes profile data: `delete_oauth_app_data` is called only from
    `delete_server_app_data` (`:694`), and the Control Center has no deletion of its own (read by
    search).
- **How it fails safely:** a `PcgCustomWatchlistError` from the store joins `errors`, so no
  deletion notice is recorded and the request can be retried (`:735-747`). The payload and the
  audit event gain `deleted_watchlist_count`. The log line carries counts only.
- **No app change is needed:** the request, its scope and its answer's existing fields stay as they
  are.

**What stays out.** #5 records a wider gap in how alert preferences are written. Its details stay
in that issue, in the private backend repository, because this repository is public. Closing it
needs the app to change first, and is planned after Christmas.

- **The new category does not widen it.** The field rides the existing `set_spawn_alert_mode`
  request, bound to the caller's own profile (`backend_auth_policy.py:123-131`). Whoever can set
  it can already set the ordinary mode for the same profile and the same record.
- **`/upload_alt_dex` is bound to the session's profile,** as `/upload_dex_list` is, and writes
  nothing on any device record.

## j) Tests and device checks

### Backend tests (characterization, offline)

- **The match** (*c*, decision 14):
  - each of the 371 table names resolves to itself in step 1, and matches a list holding it;
  - exact means exact: "venusaur (F)", "Venusaur (f)", "Venusaur  (F)" with two spaces, and
    "Gal Farfetch'd" with a straight apostrophe for "Gal Farfetch’d" match nothing; "Meganium (F)"
    is a page name, and its first word is not the prefix "Mega";
  - "Venusaur (F)" never matches "Venusaur", "Deerling (Autumn)" never "Deerling (Summer)",
    "Unown (F)" never "Unown (M)", and "Shellos (West)" or "Palafin (Zero)" match no list of
    page names;
  - step 2: "Hisuian Zorua", "Paldean Wooper" and "Sin Alolan Marowak" match lists holding
    "His Zorua", "Pal Wooper" and "Sin Alo Marowak"; it runs only without an exact match, and it
    changes none of the 371;
  - a name absent from the table still matches exactly, a new form PCG adds;
  - `--check` fails on a hand-edited table, and the table equals the asset at the recorded app
    commit.
- **The log line:** once per name for "Shellos (West)", "Mega Rayquaza" and "Arceus (Bug)" when
  not page names; never for a page name or for "Hisuian Zorua"; never for "Pikachu"; the record
  holds the name only; no more lines once 2000 names are held.
- **The upload:** 401 with no session; the legacy key refused; 403 for another profile; 400 for a
  wrong type, too many names, too long a name, a bad `read_at`; 409 `alt_dex_alerts_off`, with no
  file written, while no device record has the category on (decision 15); 200 writes the file atomically
  (an exception in the write leaves the old file); a second upload replaces it;
  `/upload_dex_list` leaves it as it was, and its own behaviour is unchanged (wanted list rewritten,
  missing `wanted_pokemon` still an empty list).
- **The category:** absent means off; the field stored, kept when absent, false when absent with
  `enabled` false; not boolean is 400; `_is_active` keeps a mode-3 profile with the category on in
  `profile_ids`, through `set_spawn_alert_mode` and through `register_fcm`.
- **The selection:**
  - form missing and the category on: one target, `alt_dex_form`, mode 3 included;
  - the category off, or no list, or the form not in it: no target from it (decision 3);
  - the same spawn also missing from the main dex, or on Most Wanted: still one target, of the
    higher kind, with every reason listed;
  - a base name ("Vivillon") never matches a form ("Vivillon (Fancy)"), and "Deerling (Autumn)"
    never matches "Deerling (Summer)";
  - two profiles on one device: two targets; the reminder gives the same targets;
  - an event spawn of a missing form: one target per profile, `alt_dex_form`, the token's
    event-only target suppressed, and `event_spawn` among the reasons (decision 8).
- **The payload:** title and body of *e*, initial and reminder; `alert_kind` not folded into
  `wanted`; `is_missing_alt_dex_form` survives `dedupe_targets`.
- **Deletion:** `delete_server_data` removes the list and counts it; a failure to remove it
  withholds the deletion notice; the removal when the category is off on every device, at each of
  its three points, and not while another device still has it on.
- **The watchlists (fix 3):** `delete_server_data` removes the scoped profiles' watchlists on
  every device and leaves other profiles' alone; a store error withholds the deletion notice;
  `delete_device_data` and an account removal leave watchlists as they are.
- **#5:** an empty, credential-less record lets the registration pass and enrolls the hash; one
  whose maps hold another profile, disabled, does too (the case of *i*); one listing another
  profile in `profile_ids` still gets 403; a hash mismatch still gets 401; a disabled selection
  for an unknown device writes no record and answers as before; the case left as it is.

### App tests (unit)

- The shared table (`pcg_alt_dex_forms_v1.json`) lists exactly the bundled asset's names, in
  order, and the uploader sends each of them unchanged.
- `PcgProfileAlertSelection`: equality with the new field; `DISABLED` has it false;
  `requiresFirebaseDelivery` is true with only this category on.
- The acknowledgement store: a round trip, and an old entry without the key reads false.
- The request payload carries `alt_dex_form_alerts_enabled`.
- The upload payload: names without 🔒 or 🎊, no sprite numbers, no obtained forms, `read_at` from
  `readAtMs`; nothing sent with the category off, nor with the session missing or unusable.
- The upload is called after an accepted reading only with the category on, and after an
  acknowledged turn-on only with a reading stored.
- **The turn-on order** (decision 15), in `PcgProfileAlertSyncPlanner`:
  - turning the category on gives a plan whose last step is `ALT_DEX_UPLOAD`, after
    `FIREBASE_DELIVERY`;
  - with Most Wanted turned on in the same confirmation, `ALT_DEX_UPLOAD` still comes last;
  - turning it off, or leaving it as it was, gives no `ALT_DEX_UPLOAD`;
  - a failed delivery step means no upload is sent.
- **A 409 `alt_dex_alerts_off`:**
  - it maps to `pcg_alt_dex_alert_upload_alerts_off`, distinct from the other failures;
  - one call sends one request;
  - nothing is queued, and the local category stays as it was;
  - the acknowledged flag goes false only where an acknowledgement exists; with none, nothing is
    written and the baseline is already unconfirmed, so the next OK resends;
  - after it, the flag returns to true only when `ALT_DEX_UPLOAD` stores the list, not on
    `FIREBASE_DELIVERY` success alone.
- Every new string is in `strings.xml`; `PrivacySafeLoggingSourceTest` passes; `TermsVersionTest`
  at 4.
- The repository baseline: unit tests, lint and both assemble tasks, for stable and dev.

### Device checks (dev app only)

Taps only with Do Not Disturb on, and only after the same command checks that a dev app window has
the focus and `zen_mode` is not 0 (`AGENTS.md`, ruling of 2026-11-23). "Update Inventory" and
"Update Pokédex" are not pressed. The stable app's accounts page is never screenshotted.

1. **Default:** after installing over the current dev build, the checkbox is off, for every
   account. `set_spawn_alert_mode`'s answer, as logged under `FCM_REGISTER`, shows it off.
2. **On with no reading:** on an account with no alt-dex reading, the hint shows under the
   checkbox, and its short form as a toast after OK; no `upload_alt_dex` line in Logcat.
3. **On with a reading:** on the owner's account (store 3 holds 61 missing forms, observed
   2026-11-23), turning it on gives one `upload_alt_dex responseCode=200` line; on the server, the
   owner reads `[ALT_DEX] saved ... missing_count=61`.
4. **A reading with it on:** "Update Alt Dex" gives the new toast and one upload.
5. **Off:** turning it off sends the field false, and the server's file is gone when no other
   device has it on (read by the owner).
6. **Account removal and the data reset:** as today, and the server's list follows *b*.
7. **Reading 1 unchanged:** not pressed in this step, and its code untouched (read in the diff).

### What only a real form spawn can show

- **The alert itself, end to end:** a form missing from the account's alt dex spawns in a channel
  the backend watches, and the phone shows the title and body of *e*, on the channel matching its
  settings, with one reminder if reminders are on. Spawns cannot be made: a test never sends a
  chat command.
- **How the chat names the forms no one has seen spawn** (decision 14): the regional, Mega,
  PCG, Sin, Clone, typed and Eternal forms. Each one that alerts confirms its name, and the log
  line shows those that do not.
- **During the Christmas event:** whether event forms arrive as event spawns or ordinary ones,
  and whether a reading after the event starts lists them.
- **Device check 8 of the readings note** is unrelated and stays as it is.

## Timing

**Target:** 5.7.0, before PCG's Christmas event, roughly the week before Christmas. The release
decision is on 2026-12-11, 16 days after this note.

**The work, as specified:**

| Part | Size | Depends on |
|---|---|---|
| *Open question 1*, the precondition | answered on 2026-11-25 from the production logs (decision 14) | done |
| Backend pull request (*a* to *e*, *i*) with its tests | the larger half: a new endpoint, a store, the exact match and its generated table, the selection in both pipelines, payload text, two deletions, three fixes | merged on 2026-11-25 (tmc-backend#9, `53fe1b4`); target for the deploy: 2026-12-03 (it needs the owner's yes) |
| Deploy and live check | the owner, on the production machine | the backend merge |
| App pull request (*f*, *g*'s bundled copy, terms version) with its tests | medium: a checkbox, a store field, a request field, an uploader, strings, policies | the backend live, for its device step |
| Policy web copy | small | the final wording |
| Device step, then release steps 4 to 8 | one sitting each | a real form spawn for the last check |

**Verdict: it fits, with little slack, if the backend is merged and live by about 2026-12-03 and the
app is merged by about 2026-12-09.** Three things compete for the same window:

- **F3** (#117) lands before the next app release: the owner's decision of 2026-11-20, recorded on
  #117 (decision 12).
- **The end-to-end check needs a real form spawn,** which no one can schedule. Forms spawn often
  (the owner's account misses 61 of 371), so one sitting is likely enough, not certain.
- **Question 1 no longer gates anything:** answered on 2026-11-25 (decision 14).

**What can be cut, in this order, without breaking a decision:**

1. **The hint under the checkbox:** a toast after OK alone.
2. **The log line** for form-like names. The generated table stays: the exact-first step and
   the tests on both sides depend on it.

**What is not cut,** because the owner decided it or the rules require it: event spawns
(decision 8), the removal when the category is off everywhere (decision 9), the watchlist deletion
(decision 11), the policy before any data leaves the phone (decision 6), and the backend before
the app.

**"Panel and upload in 5.7.0, alert later" is not recommended.** The Form line is already merged
(#138) and ships in 5.7.0 whatever is decided here. An upload without the alert would send data
with no use, and the policy change would describe a feature that is not there. **The fallback is
instead:** the backend ships when ready (it is harmless while no app uses it, and #5's fixes are
worth having anyway), and the app part moves to 5.7.1 if it misses 2026-12-11.

## Found while reading, outside this design

Recorded only. None of these is changed by this note.

- **Most Wanted watchlists were not deleted by any server path.** Found while reading for this
  note, and since decided: fix 3 of *i* (decision 11).
- **`save_profile_dex_list` writes in place**, with `open(path, "w")`, and does not take `DEX_LOCK`
  (`twitch_cache_bot_v10_token_safety.py:971-1006`), while `delete_server_app_data` removes the same
  files under it (`:681-692`) and every spawn reads them. A crash during the write leaves a
  truncated file, which the reader skips (`dex_interest_store.py:81-96`): that profile loses its
  missing-dex alerts until the next upload. Reading 2's backend field will need read-merge-write
  there anyway; the atomic write belongs with it.
- **`pokemon_type_dex.json` gives some form entries their species' aliases,** such as "frillish
  male" on "Frillish (F)" and "Delibird" on "PCG Delibird". `PokemonTypeDexCatalog` uses an alias
  only when one entry owns it, and exact names first (`PokemonTypeDexCatalog.kt:32-38, 124-139`), so
  these resolve to the right entry or to none. Read in code and data; how often a chat name falls
  through to "none" was not examined.

## Open questions

None is open.

1. **How does PCG's chat name each form, against the alt-dex page?** Answered on 2026-11-25 from
   the production backend's logs (decision 14): exactly as the page, for the 116 forms that
   spawned. The prefixed classes did not spawn in the period, so their spelling stays unseen;
   step 2 of *c* covers the long regional spellings, and the log line shows any other.

**Also answered on 2026-11-25** (decisions 8 to 13): event spawns are included; the list is deleted
when the category is off on every device; `CURRENT_TERMS_VERSION` goes to 4; profile data deletion
deletes the Most Wanted watchlists, in the same backend pull request; F3 comes before the next
release; the correction to #5's fix 1 stands.
