# Design: an alert for missing alt dex forms

**Status: design note. Nothing here is implemented.** The owner asked for it on 2026-11-25, with
the decisions below. It covers the app (`unouidol/TwitchMiniChat`) and the backend
(`unouidol/tmc-backend`, private), and the two backend fixes of tmc-backend#5, which are to ship
in the same backend pull request.

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
| Upload | the last reading's missing forms (raw names) and `read_at`, by a new endpoint `/upload_alt_dex`, Bearer only; sent only while the category is on for that profile on this phone | *a* |
| Server storage | `alt_dex_lists/<profile_id>.json`, its own file and lock, written atomically; never touched by `/upload_dex_list`; deleted by profile data deletion and, recommended, when the category is off on every device | *b* |
| Matching | a new `alt_dex_form_key`, the app's `PcgPokemonNameNormalizer` ported exactly, with no alias step; checked against all 371 names by a shared fixture | *c* |
| Category | a per-profile boolean in the device record, `profile_alt_dex_form_alerts`, default false, beside `profile_event_spawn_alerts`; one target per profile and device, as today | *d* |
| Notification | its own title and body, in the existing four channels; no new channel | *e* |
| App | one checkbox in the bell menu, a hint when no reading exists, the upload after an accepted reading and when the category is turned on | *f* |
| Privacy | new sentences in privacy and data deletion, both copies; `CURRENT_TERMS_VERSION` 3 → 4, because data is newly collected | *g* |
| Order | backend first (with #5's fixes), then the policy web copy, then the app release | *h* |

## Facts the design rests on

| Fact | Provenance |
|---|---|
| Store 3 holds, per profile, the obtained forms (they only grow) and the last reading's missing forms, each as normalized key, raw name and sprite number, and `readAtMs` (`PcgAltDexSnapshotStore.kt:18-22, 33-84`). | Read in code |
| Nothing of the alt dex leaves the phone today: `saveManualAltDexReading` stores and shows a toast, and says "Nothing is uploaded" (`GeckoSessionManager.kt:895-947`). | Read in code |
| The alt dex has 371 forms: 349 are catalogue names, 22 are not (11 Mega, 4 PCG, 2 Clone and 5 others). 46 carry 🔒. | Observed on 2026-11-23; the counts read again from `pcg_alt_dex_forms.json` (blob `651a8fd`) and computed |
| The spawn name the backend sees is what PCG's chat message carries between "A wild" and "appears" (`spawn_message_classifier.py:13-16`, `SpawnMessageParser.kt`). | Read in code |
| The Smart Catch panel matches the Form line by the catalogue's name for the spawn first, then by the chat's name (`QuickCatchProfileSpawnContext.kt:120-140`). The catalogue's name comes from `PokemonTypeDexCatalog.find`: the exact name, else an alias owned by exactly one entry (`PokemonTypeDexCatalog.kt:32-38, 124-139`). | Read in code |
| Whether PCG's chat prints a form exactly as the alt-dex page names it ("Alo Vulpix") or otherwise ("Alolan Vulpix") was never checked. | Not observed (*Open question 1*) |
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
  identifier. The server needs only the missing names to match a spawn, and computes the keys
  itself. Less leaves the phone, and the server cannot drift from a key the app computed.

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
  `pcg_custom_watchlist_store.py:17`). Blank names and repeated keys are dropped. An empty list
  is accepted: it means nothing is missing.
- `read_at` must parse as UTC ISO-8601 and not lie more than a day in the future; otherwise 400.
- Names are **not** checked against a catalogue. Most Wanted refuses unknown names
  (`pcg_custom_watchlist_store.py:253-290`), but the alt dex is exactly where PCG adds forms the
  catalogue does not know: 22 already.
- Answer 200 `{"ok": true, "count": <n>, "read_at": "<read_at>"}`.
- Log one line with no names: `[ALT_DEX] saved profile_ref=<ref> missing_count=<n>`.

### When

**Recommended: only while the category is on for that profile on this phone.** Two moments:

1. **After each accepted "Update Alt Dex"**, if the category is on for the reading's profile.
2. **Once when the user turns the category on**, after the server has acknowledged the change,
   if store 3 holds a reading for that profile. The user does not have to read the page again.

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
- **Recommended: also removed when no device has the category on for that profile.** The upload
  is sent only while the category is on (*a*), so the list should not outlive it. After any write
  that can turn the last flag off, the server deletes the list of each affected profile that no
  device record still has on:
  - `set_spawn_alert_mode` (the user turns it off, or the app removes the account);
  - `delete_device_server_data` (`:758`), "Erase everything and remove this device";
  - the removal of a device whose token Firebase reports dead (`fcm_sender.py:1124-1158`).

  If this is cut to save time, the list is kept until a new upload replaces it or the profile's
  data is deleted, and the privacy text must say that instead (*g* gives both versions).

## c) Matching

### The key

A new function, `alt_dex_form_key(name)`, in its own module, used only for this alert. It is not
`main_dex_key`, which merges every form of a species, and not `registration_key`, which merges
genders and seasons (*Facts*). It is the app's `PcgPokemonNameNormalizer.normalize`
(`PcgPokemonNameNormalizer.kt:10-28`) ported step for step, after the page's two prefixes:

1. Remove 🔒 and 🎊.
2. Decompose to NFKD.
3. Remove every code point of Unicode general category M (Mn, Mc, Me), as Kotlin's `\p{M}`
   does. Not `unicodedata.combining`, which tests the combining class and misses marks of class 0.
4. Lowercase.
5. Replace each run of characters outside `a-z`, `0-9`, ♀ (U+2640) and ♂ (U+2642) with one space.
6. Trim, and collapse repeated spaces.

There is **no alias step.** A spawn alerts when its own name's key is a key of the profile's
missing names: exact form name, as decided.

- **Computed over all 371 alt-dex names:** this function and the app's normalizer give the same
  key for every name, the 22 the catalogue does not know included, and the 371 keys are distinct.
  `pcg_custom_watchlist_store.py:42-56` gives the same 371 keys too, but it keeps any Unicode
  letter and casefolds, so it would split from the app on names the app reduces to ASCII. That
  is why the port is exact rather than a reuse.
- **Why not the app's catalogue aliases.** The panel resolves a chat name through
  `PokemonTypeDexCatalog`'s unique aliases (*Facts*), so it would follow a chat name such as
  "Alolan Vulpix" to "Alo Vulpix". Those aliases serve the stats lookup, not form identity, and
  some point across forms: the entry for "Frillish (F)" carries the alias "frillish male", the same
  as plain "Frillish" (read in `pokemon_type_dex.json`). The app is safe because it uses an alias
  only when one entry owns it. A server table derived from them would need the same care, and
  would be built before anyone has seen what the chat actually prints.
- **What the exact match costs, if the chat names a form otherwise than the page:** that form
  never alerts, and nothing reports it. *Open question 1* asks the owner to settle it from the
  backend's own log before implementation. If it shows differences, the remedy is a small,
  reviewed alias table, generated from the app's assets like the species table and limited to
  aliases owned by one entry, and this note is updated first.

**A diagnostic, chosen to settle that question.** For each spawn whose name contains "(" or starts
with Alo, Gal, His, Pal, Sin, Mega or PCG, and whose key is not one of the generated table's keys
(*Keeping the list in step*), the backend logs once per process
`[ALT_DEX] spawn name not a known form key=<key>`. It records no profile. The value that
separates the cases is whether the logged key is one of the 371: the line never fires for a name
the page also uses, and fires for a form name the page spells differently.

### Matching at a spawn

`load_alt_dex_interested_profiles(pokemon_name)`, beside `load_interested_profiles`
(`fcm_sender.py:1161-1186`):

- computes the spawn's key once;
- reads each file in `alt_dex_lists/`, skipping a malformed file on its own, as
  `DexInterestStore` does (`dex_interest_store.py:81-99`);
- returns `{profile_id: profile_label}` for each list holding that key.

Keys are computed from the stored names at match time, as `DexInterestStore` does today, so a
correction to the function applies to lists already stored. Every spawn reads the directory, as
`dex_lists/` is read today; at today's scale that is the existing cost, once more.

### Keeping the list in step

The server does not need the list of forms to match: it matches the names each profile uploaded,
and a name it has never seen works the same. The list is needed for two checks only:

- **That both sides compute the same key.** The app commits
  `app/src/test/resources/pcg_alt_dex/pcg_alt_dex_form_keys.json`: every form of the bundled
  asset, as `name` and `key`. A Kotlin test checks that the file lists exactly the asset's forms,
  and that `PcgPokemonNameNormalizer` gives each its key.
- **On the backend,** a generator, `generate_alt_dex_form_keys.py`, copies that file from git at
  a given app commit (`git show <commit>:<path>`) into `alt_dex_form_keys_v1.json`, recording the
  commit and the blobs of the fixture and of `pcg_alt_dex_forms.json`, with a `--check` mode. It
  follows `generate_main_dex_species_table.py`, which does the same for the species table. A
  backend test checks that `alt_dex_form_key` gives each name its key.
- **When PCG adds forms,** the app's asset and fixture change in one app pull request, and the
  backend regenerates its copy in the next backend change. Until it does, nothing breaks: only
  the diagnostic line fires for the new names.

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

**Event spawns: recommended to include them.** The event pipeline (*Facts*) is separate, and Most
Wanted does not reach it. If PCG's Christmas spawns arrive as event spawns, under the configured
event rule, a form alert limited to ordinary spawns would miss exactly the forms the owner wants
for the event. In `select_token_targets_for_event_spawn` (`fcm_target_selector.py:253-428`) the
form condition would join the missing-dex branch: one target per profile, which also suppresses
the token's event-only target, as missing dex does today (`:398-413`). *Open question 2* asks the
owner.

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
  "Enable Most Wanted alerts" (`:92-97`): `pcg_alt_dex_form_alert_label`, "Notify missing Alt Dex
  forms". It is applied, like the others, only on OK (`:146-158`).
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
- **What the user sees:**
  - after a reading, with the category on: `pcg_alt_dex_updated_and_sent`, "Alt Dex saved and sent
    for alerts. Forms not obtained: %d.", in place of today's `pcg_alt_dex_updated`;
  - if the upload fails: `pcg_alt_dex_alert_upload_failed`, "Alt Dex saved on this phone, but it
    could not be sent for alerts. Press Update Alt Dex again later.";
  - with the category off: today's toast, unchanged.
- **The category on, no reading on this phone** (decision 3):
  - under the checkbox, while it is ticked and store 3 has no reading for the profile:
    `pcg_alt_dex_form_alert_no_reading`, "No Alt Dex reading on this phone yet. In the PCG
    Pokédex, switch to Alt Dex, turn Obtained off and press Update Alt Dex.";
  - after OK, the same text as a toast, once.
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

**Section 5, a new item:**

- with the removal-when-off rule of *b*:

  > The Alt Dex list sent for Missing Alt Dex form alerts is replaced by each new one. It is
  > deleted when these alerts are off for that account on every device, and by "Delete app
  > account and all data".

- without it:

  > The Alt Dex list sent for Missing Alt Dex form alerts is replaced by each new one, and kept
  > until "Delete app account and all data" deletes it.

**Section 6, a new item:**

> Missing Alt Dex form alerts are off unless you turn them on, for each account, in the PCG
> notifications menu.

**`data_deletion.html`, coherence:**

- "Delete app account and all data" (line 89): "the Pokédex lists uploaded for those accounts"
  becomes "the Pokédex and Alt Dex lists uploaded for those accounts".
- With the removal-when-off rule only, a sentence after "Removing one account", *On the server*
  (line 118): "The Alt Dex list uploaded for that account is deleted once no device has its
  Missing Alt Dex form alerts on." Without the rule, the existing "uploaded Pokédex lists ... are
  kept" already covers it once it reads "uploaded Pokédex and Alt Dex lists".
- "Erase everything and remove this device" (line 52) the same way.

### The acceptance gate

**`CURRENT_TERMS_VERSION` goes from 3 to 4** (`TermsPrefs.kt:23`), by the criterion recorded in
`RELEASE-STATE.md` on 2026-09-26:

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
choose it. *Open question 4* records the argument the other way, for the owner.

## h) Order of shipping

`AGENTS.md`: backward-compatible server behaviour before the app depends on it; the policy web copy
before the release that bundles the new text.

1. **Backend pull request**, deployed and checked before any app build sends anything:
   - `/upload_alt_dex`, the store, `alt_dex_form_key`, the generated key table and the diagnostic;
   - the category field, its default and its old-app rule;
   - the selection in both pipelines (the event one if *Open question 2* says so), and the
     payload's text;
   - deletion (and, if kept, the removal-when-off rule);
   - #5's two fixes (*i*).

   All of it is backward compatible: no client sends the new field or calls the new endpoint, the
   default is off, and the #5 fixes turn a 403 into a 200 and stop writing a record no one reads.
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

## i) The two backend fixes of tmc-backend#5

**Fix 1: a registration may complete a record with no credential and no active profile.**

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

- **The key:** all 371 names of the generated table give their key; the step order of *c* (a mark
  of class 0, a non-ASCII letter, ♀ and ♂, 🔒 and 🎊, "Farfetch’d" with its curly apostrophe);
  `--check` fails on a hand-edited table.
- **The upload:** 401 with no session; the legacy key refused; 403 for another profile; 400 for a
  wrong type, too many names, too long a name, a bad `read_at`; 200 writes the file atomically
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
  - event spawns, as *Open question 2* decides.
- **The payload:** title and body of *e*, initial and reminder; `alert_kind` not folded into
  `wanted`; `is_missing_alt_dex_form` survives `dedupe_targets`.
- **Deletion:** `delete_server_data` removes the list and counts it; a failure to remove it
  withholds the deletion notice; the removal-when-off rule, if kept, at each of its three points.
- **#5:** an empty, credential-less record lets the registration pass and enrolls the hash; one
  whose maps hold another profile, disabled, does too (the case of *i*); one listing another
  profile in `profile_ids` still gets 403; a hash mismatch still gets 401; a disabled selection
  for an unknown device writes no record and answers as before; the case left as it is.

### App tests (unit)

- The fixture lists exactly the bundled asset's forms, and the normalizer gives each its key.
- `PcgProfileAlertSelection`: equality with the new field; `DISABLED` has it false;
  `requiresFirebaseDelivery` is true with only this category on.
- The acknowledgement store: a round trip, and an old entry without the key reads false.
- The request payload carries `alt_dex_form_alerts_enabled`.
- The upload payload: names without 🔒 or 🎊, no sprite numbers, no obtained forms, `read_at` from
  `readAtMs`; nothing sent with the category off, nor with the session missing or unusable.
- The upload is called after an accepted reading only with the category on, and after an
  acknowledged turn-on only with a reading stored.
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
   checkbox and as a toast after OK; no `upload_alt_dex` line in Logcat.
3. **On with a reading:** on the owner's account (store 3 holds 61 missing forms, observed
   2026-11-23), turning it on gives one `upload_alt_dex responseCode=200` line; on the server, the
   owner reads `[ALT_DEX] saved ... missing_count=61`.
4. **A reading with it on:** "Update Alt Dex" gives the new toast and one upload.
5. **Off:** turning it off sends the field false; with the removal-when-off rule, the server's
   file is gone (read by the owner).
6. **Account removal and the data reset:** as today, and the server's list follows *b*.
7. **Reading 1 unchanged:** not pressed in this step, and its code untouched (read in the diff).

### What only a real form spawn can show

- **The alert itself, end to end:** a form missing from the account's alt dex spawns in a channel
  the backend watches, and the phone shows the title and body of *e*, on the channel matching its
  settings, with one reminder if reminders are on. Spawns cannot be made: a test never sends a
  chat command.
- **Whether the chat names forms as the page does** (*Open question 1*): every form spawn that
  alerts confirms its name; the diagnostic line shows those that do not.
- **During the Christmas event:** whether event forms arrive as event spawns or ordinary ones,
  and whether a reading after the event starts lists them.
- **Device check 8 of the readings note** is unrelated and stays as it is.

## Timing

**Target:** 5.7.0, before PCG's Christmas event, roughly the week before Christmas. The release
decision is on 2026-12-11, 16 days after this note.

**The work, as specified:**

| Part | Size | Depends on |
|---|---|---|
| Backend pull request (*a* to *e*, *i*) with its tests | the larger half: a new endpoint, a store, a key and its generator, the selection in one or two pipelines, payload text, deletion, two fixes | the owner's answers to questions 1 and 2 |
| Deploy and live check | the owner, on the production machine | the backend merge |
| App pull request (*f*, *g*'s bundled copy, terms version) with its tests | medium: a checkbox, a store field, a request field, an uploader, strings, policies | the backend live, for its device step |
| Policy web copy | small | the final wording |
| Device step, then release steps 4 to 8 | one sitting each | a real form spawn for the last check |

**Verdict: it fits, with little slack, if the backend is merged and live by about 2026-12-03 and the
app is merged by about 2026-12-09.** Three things compete for the same window:

- **F3** (#117): by the owner's ruling of 2026-11-20, it lands before the next app release. That
  ruling comes from a note of an earlier session, not from a file in this repository; to be
  confirmed.
- **The end-to-end check needs a real form spawn,** which no one can schedule. Forms spawn often
  (the owner's account misses 61 of 371), so one sitting is likely enough, not certain.
- **Question 1 is unanswered.** If the chat names forms otherwise, an alias table is added first.

**What can be cut, in this order, without breaking a decision:**

1. **Event spawns,** if the owner says Christmas spawns arrive as ordinary spawns (*Open question 2*).
2. **The removal-when-off rule** (*b*): the list is then kept until profile data deletion, and the
   privacy text takes its second version.
3. **The hint under the checkbox:** a toast after OK alone.
4. **The diagnostic line** and the generated key table, keeping only the app's fixture and a
   backend test over a copy of it.

**What should not be cut:** the policy before any data leaves the phone (decision 6), the backend
before the app, and the deletion of the list with the profile's data.

**"Panel and upload in 5.7.0, alert later" is not recommended.** The Form line is already merged
(#138) and ships in 5.7.0 whatever is decided here. An upload without the alert would send data
with no use, and the policy change would describe a feature that is not there. **The fallback is
instead:** the backend ships when ready (it is harmless while no app uses it, and #5's fixes are
worth having anyway), and the app part moves to 5.7.1 if it misses 2026-12-11.

## Found while reading, outside this design

Recorded only. None of these is changed by this note.

- **Most Wanted watchlists are not deleted by any server path.** `PcgCustomWatchlistStore` has
  `delete_device` and `delete_profile` (`pcg_custom_watchlist_store.py:205, 218`), and nothing
  outside the tests calls them; `delete_server_app_data` does not touch the store
  (`twitch_cache_bot_v10_token_safety.py:625-756`). `data_deletion.html` (line 52) presents
  watchlists among the data that "Delete app account and all data" deletes. Read in code at
  `8ccf210`; the production store was not read. *Open question 5.*
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

1. **Does PCG's chat name a form exactly as the alt-dex page does?** For example "Alo Vulpix" or
   "Alolan Vulpix", "Toxtricity (Lowkey)" or "Toxtricity (Low Key)", "Unown (B)" or "Unown".
   - **Why it matters:** the alert matches by exact name. A form the chat spells otherwise never
     alerts, and nothing reports it.
   - **How to settle it cheaply, before implementation:** the backend already logs each spawn's
     name (`[FCM_METRIC] ... pokemon='<name>'`, `fcm_sender.py:1606-1611`). The owner can read on
     the production machine the names of recent form spawns and compare them with the page's
     names in `pcg_alt_dex_forms.json`. Only names, no profile data.
2. **Do PCG's Christmas spawns arrive as event spawns** (matched by `event_spawn_rule.json`) or as
   ordinary "A wild … appears"? This note recommends including event spawns either way; the
   owner may cut it if they are ordinary.
3. **The removal-when-off rule** (*b*): keep it (recommended), or keep the list until profile data
   deletion, with the second privacy text?
4. **`CURRENT_TERMS_VERSION` to 4.** The criterion says yes: data newly collected. The argument the
   other way is that nothing is collected until the user turns the category on in a menu that
   says what it does, and that the gate was raised to 3 for 5.6.0. This note follows the
   criterion; the owner decides.
5. **The Most Wanted watchlists** (*Found while reading*): deletion code, or a correction to
   `data_deletion.html`? Outside this design, and a coordinated privacy question of its own.
