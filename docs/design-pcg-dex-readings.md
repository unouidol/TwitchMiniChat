# Design: reading the whole PCG main dex and the alt dex

**Status: design note, no code.** The owner chose this direction on 2026-11-23 ("Design A" of
that day's investigation). Nothing in it is started:

- the implementation waits for the owner's go;
- the second capture round (*Open questions*) waits for the owner's approval of this note.

Provenance is marked throughout:

- **Observed:** seen on the test phone on 2026-11-23, in the dev app, with one Pokémon Community
  Game (PCG) account whose main dex had one obtained entry and whose alt dex had none.
- **Read in code:** read statically, not run.
- **Not observed:** stated as such, and listed again under *Open questions*.

## What stays exactly as it is

These stay byte for byte as they are at `a11a3a0`:

- `app/src/main/assets/pcg_probe/content.js` (blob `4d07adb`);
- `app/src/main/assets/pcg_probe/pcg_tab_state_scout.js` (blob `23f1fd0`).

Reading 1, today's "Update Pokédex" reading, behaves exactly as it does today:

- its gate and its messages;
- its write to `PcgPokedexSnapshotStore`;
- its upload to `/upload_dex_list`.

Any page state that is not reading 2's goes through "Update Pokédex" by today's path, with
today's message. Two things change around reading 1:

- **"Update Pokédex"'s enabling, and one branch of its request:** see *The two buttons*.
- **An optional run number in its store:** see *Main-dex resets*.

`pcg_probe_inventory.js` and "Update Inventory" stay as they are too, under the owner's ruling of
2026-11-18 in `AGENTS.md`.

The new readings are added beside the existing one, never in place of it. The main dex and the alt
dex are never merged, in any store, upload or panel rule.

## Why

Today TMC takes one reading of the PCG Pokédex, "Update Pokédex". It is taken with the
"Spawnable" filter alone and stored as the list of missing names. That leaves two gaps:

1. **203 species are never read.** The page's three category filters, "Spawnable", "Starters"
   and "Legendaries", split the 1025 species with no overlap: 822, 81 and 122 (observed).
   - A Spawnable-only reading never lists a starter or a legendary, missing or not.
   - The Smart Catch panel can only say Unknown for them. Until #134 it said Registered for a
     missing starter.
   - tmc-backend never sends a missing-dex alert for them, because they are never in the
     uploaded list (read in code, tmc-backend `8ccf210`).
2. **The alt dex is not read at all.** The alt dex holds the forms (regional, Mega, PCG, Clone,
   Sin and others), and PCG never resets it. TMC has no form-level status.

## Facts the design rests on

| Fact | Provenance |
|---|---|
| Main and alt dex share the page's markup (`.pokedex__container`, `.pokedex__grid`, `.pokedex__entry`, `.pokedex__entry-name`). Only `label.alt-dex-label > span` tells them apart: "Main Dex" with class `""`, or "Alt Dex" with class `active`. | Observed |
| The scout and `content.js` report `pokedex` for both dexes. The active header button is marked only by `disabled`, so both fall back to "the Pokédex markup exists". | Observed |
| Main-dex filters: "Spawnable", "Starters", "Legendaries", "Obtained". The alt dex has only "Obtained"; the other three are removed from its markup. | Observed |
| "Obtained" off lists only missing entries. "Obtained" on adds the obtained ones. "Obtained" alone lists nothing. | Observed in the main dex. In the alt dex, not observed: the account had no form. |
| The "Obtained" state carries over when switching between main and alt dex. | Observed once |
| `.pokedex__counter` reads "obtained / shown". | Observed: "1 / 203" with Starters + Legendaries + Obtained |
| A missing entry's sprite has the class `grayscale`; the one obtained entry, Piplup, had none. | Observed, one obtained sample only |
| The sprite URL ends in `/pokemon/<number>.png`. Main-dex numbers are the national numbers 1 to 1025, in page order. Alt-dex numbers run from 10004 to 100432. | Observed |
| Every starter's name starts with 🎊. Every legendary's starts with 🔒, and so do 46 alt-dex forms. `content.js` removes 🎊 and rejects any list containing 🔒. | Observed. The `content.js` behaviour is read in code. |
| Every entry is in the page from the start, with nothing loaded while scrolling, and the count of entries equals the counter. | Observed, on the phone layout |
| The main dex has 1025 entries. 1007 are catalog names; 18 are base-form names such as "Vivillon (Meadow)" that the app resolves by species prefix. App and backend agree on all 1025 species. | Observed against the catalog |
| The alt dex has 371 forms: 349 catalog names and 22 names the catalog does not know (11 Mega forms, 4 PCG forms, and others). | Observed |
| The alt dex shows no progress line of its own: the two lines on its page are the main dex's. | Observed |
| `/upload_dex_list` stores only `profile_id`, `profile_label`, `updated_at` and `wanted_pokemon`, rewriting the profile's file each time. A field it does not know is dropped without error. A missing `wanted_pokemon` is stored as an empty list. | Read in code, tmc-backend `8ccf210` |

The categories as captured are committed as a test fixture,
`app/src/test/resources/pcg_main_dex_categories/pcg_main_dex_categories_2026-11-23.json`, with
their provenance (#134).

## The three readings

Two buttons take the three readings (*The two buttons*). One tap takes at most one reading and
sends nothing to chat. No reading clicks, scrolls or changes a filter: the user sets the page,
and the app only reads it.

| | Reading 1 (today's, unchanged) | Reading 2 (new) | Reading 3 (new) |
|---|---|---|---|
| Dex label | "Main Dex" | "Main Dex" | "Alt Dex", class `active` |
| Spawnable | on | **off** | absent from the page |
| Starters | off | **on** | absent from the page |
| Legendaries | off | **on** | absent from the page |
| Obtained | off | off | off |
| Lists | the missing Spawnable species | the missing starters and legendaries | the forms not obtained |
| Read by | `content.js` | a new passive script | the same new script |
| Button | "Update Pokédex" | "Update Pokédex" | "Update Alt Dex" (new) |

**The gates exclude one another.**

- Reading 1 needs Spawnable on; reading 2 needs it off.
- Reading 3 needs the alt dex. There `content.js` already reports `valid=false` and never
  extracts (observed).
- No page state can satisfy two readings, so a tap of either button takes at most one.

**Obtained stays off in all three.** The filter is shared between the dexes, so this saves the
user a tap on every switch.

**What a new reading checks before it is accepted.** Android checks these, as it does for reading
1, and a failed check is rejected with its own resource-backed message:

- the active tab, and the dex label;
- the exact filter combination, read from the live checkboxes;
- that the count of entries equals the counter's shown number, and the counter's obtained number
  is 0;
- that every entry is `grayscale`;
- reading 2 only: that every name resolves to a species of category `STARTERS` or `LEGENDARIES`
  in `MainDexSpeciesIndex`, and carries 🎊 or 🔒 (removed from the stored name, never a reason
  to reject);
- that two reads one second apart are identical, as the capture script required.

**Known forms, for reading 3.** With "Obtained" off, reading 3 lists only forms not obtained, so
"obtained" means known but not listed. The app knows a form from two sources:

- a bundled list of the alt dex's forms, captured with its provenance as the category fixture
  was;
- any name that an accepted reading has listed.

A form PCG adds later, and that the user obtains before any reading lists it, stays "?". This
depends on *Open question 1*.

## The two buttons

"Update Pokédex" takes reading 1 or reading 2, depending on the page state when it is tapped. A
new "Update Alt Dex" takes reading 3.

**Where the buttons learn the page state.** The page state they need comes from the new passive
script: the dex label, the live filter checkboxes and the title. The script reports them as a
state message, as the scout reports the tab. Neither `content.js` nor the scout is changed to
report anything.

**Enabling: `buildManualUpdateButtonState` (`GeckoSessionManager.kt:486-519`).** Today:

- `pokedexEnabled` is `isPokedexConfirmedActive`, from the scout alone;
- the scout reports `pokedex` in both dexes (observed);
- so "Update Pokédex" is enabled in the alt dex too.

With the design:

| New script's state | "Update Pokédex" | "Update Alt Dex" |
|---|---|---|
| fresh, "Main Dex" | as today: enabled when the Pokédex tab is confirmed | disabled |
| fresh, "Alt Dex" | disabled | enabled when the Pokédex tab is confirmed |
| absent or stale | as today | disabled |

- `PcgManualUpdateButtonState` gains `altDexEnabled`, and `reason` gains a value for the alt dex.
  `PcgActivity` gets the second button, with its label in `strings.xml`.
- A missing or failing new script never disables "Update Pokédex": the last row keeps today's
  behaviour.
- **In the main dex, "Update Pokédex" is enabled exactly when it is today.**
- **In the alt dex it goes from enabled to disabled.** By the code, a tap there today is rejected
  at once with `pcg_pokedex_spawnable_only_required`. The alt dex has no "Spawnable" filter, so
  `content.js` reports `spawnable_filter_off`. Read in code, not tried on the device.

**The request: `requestManualPokedexUpdate` (`GeckoSessionManager.kt:1793`).**

- **Today:** a Pokédex state with `needsSpawnableOnlyHint`, meaning Spawnable off or another filter
  on, is rejected at once with `pcg_pokedex_spawnable_only_required`. Reading 2's state is such a
  state.
- **The design changes that branch only.**
  - When the new script's fresh state is exactly reading 2's gate, the tap takes reading 2, with
    its own manual window, timeout and messages.
  - In any other state the branch rejects exactly as today.
- **Every other branch stays as it is:**
  - the wrong tab;
  - the probe not confirming the tab;
  - the recent Spawnable-only snapshot;
  - the armed window and its timeout.

**This changes what "Update Pokédex" does, so it needs the owner's go.** The 2026-11-18 ruling
keeps "Update Pokédex" as it is in every build installed for a test. The new script needs the
same go when it is implemented.

## The three stores

They are three separate stores, each with its own preferences file and schema, never merged. Each
is per profile, and each is cleared when its profile is removed and by the local-data reset, like
`PcgPokedexSnapshotStore`.

| | Store 1: `PcgPokedexSnapshotStore` | Store 2: main dex outside Spawnable | Store 3: alt dex |
|---|---|---|---|
| Written by | reading 1, unchanged | reading 2 | reading 3 |
| Holds | missing name keys, `updatedAtMs` | missing name keys, the categories read (`STARTERS`, `LEGENDARIES`), `readAtMs` | obtained forms and the last reading's missing forms; each form as normalized key, raw name and sprite number; `readAtMs` |
| Main-dex run | the title's N, only if *Open question 3* confirms it counts runs | the same | none |
| On a new accepted reading | replaced | replaced | obtained forms: **only grow**, by the known forms the reading does not list; missing forms: replaced |
| A main-dex reset in PCG | stale from a different N until the next reading; unseen otherwise, as today | the same | unaffected: PCG never resets the alt dex |

**Why store 3 only grows:** a form once obtained stays obtained, because PCG never resets the
alt dex. The price is that a wrong "obtained" cannot be corrected by a later reading, only by
clearing the profile's data. The acceptance checks above exist to keep a half-rendered list out.

Raw names and sprite numbers are stored because 22 alt-dex names are unknown to the catalog, and
15 of them resolve to no species (observed).

### Main-dex resets

Nothing in the design expires with time. A stored list stops counting only when:

- its next accepted reading replaces it; or
- the app sees a main-dex reset, as below.

**If the second capture round confirms that the page title's "Pokedex #N" counts main-dex runs:**

- stores 1 and 2 record the N they were read under, taken from the new script's fresh state when
  the reading is accepted;
- when the page shows a different N, that store is stale until its next reading;
- meanwhile the panel reads its species as Unknown, never Registered;
- store 3 is not affected.

Store 1 gets the N as an optional field under its current `schemaVersion` 1, because
`PcgPokedexSnapshotStore` reads any other schema version as no snapshot. A snapshot without the
field means "run unknown", never a reset.

**If the round does not confirm it,** *Open question 3* stays open and nothing changes. No
time-based expiry stands in for it.

## The backend field

Reading 2's missing list reaches tmc-backend as **one optional, dated field** of the dex upload,
for example:

```json
"outside_spawnable": {
  "categories": ["Starters", "Legendaries"],
  "read_at": "2026-11-23T07:24:49Z",
  "missing": ["Bulbasaur", "Mewtwo"]
}
```

**What the server must do, deployed and verified before any app build sends the field:**

1. Store the field with its own `read_at`, apart from `wanted_pokemon`.
2. Keep a stored field when an upload omits it. Reading 1's upload stays as it is and never
   carries it. Today every upload rewrites the file with four fields only.
3. If reading 2 is uploaded on its own, keep the stored `wanted_pokemon` when the upload omits
   it. Today a missing `wanted_pokemon` is stored as an empty list. The alternative is that the
   app resends store 1's list with it.
4. Match missing-dex alerts against `wanted_pokemon` and the field's `missing`, each by species,
   as `main_dex_key` matches today.
5. With no field stored, behave exactly as today. Old clients never send it.

**No age limit.** A list never expires with time, on the server as in the app.

- The server keeps the last uploaded lists until the next upload replaces them.
- It is not told about a main-dex reset: after one, its lists stay as they were until the user's
  next reading is uploaded, as `wanted_pokemon` does today.
- `read_at` records when the list was read. Nothing expires on it.

The alt dex is not uploaded in this design. A "missing in alt dex" alert would be a separate
project:

- it needs form-exact matching, not `registration_key`, which merges genders and seasons;
- it needs a new alert kind;
- it needs the policy pages updated, both copies, before any form data leaves the phone.

Authentication stays the backend session's Bearer, as for today's upload.

## The panel

**Dex entry (main dex)**, by the spawn's species as today:

- **Missing:** the species is in store 1's or store 2's missing set.
- **Registered:**
  - a Spawnable species not in store 1, when store 1 has a reading that is not stale; or
  - a starter or legendary not in store 2, when store 2 has a reading that is not stale.
- **Unknown:** every other case. That includes:
  - a starter or legendary while store 2 has no reading (#134);
  - any species whose store has no reading;
  - any species whose store is stale after a reset (*Main-dex resets*).

The Repeat Ball bonus stays tied to Registered only.

**Form (alt dex)** is a new line, shown only when the spawn's exact name is a known alt-dex form:

- **Obtained:** the form is in store 3's obtained forms.
- **Not obtained:** the form is in store 3's last missing forms.
- **?:** every other case.

Base forms ("Vivillon (Meadow)") get no form line: they exist only in the main dex, which a reset
clears. Every label is a string in `strings.xml`.

**PCG gameplay stays manual.** The readings and the panel only observe, cache and display. No
reading, store change or panel state sends a chat command, queues one, or triggers anything.

## Risks

- **The extension bundle changes.**
  - Adding a content script changes `manifest.json` and its version. `ensureBuiltIn` reloaded the
    built-in extension when its version changed, from 1.2.1 to 1.2.1.3 and back (observed). That
    it keeps the old one when the version stays the same is GeckoView's documented behaviour,
    not observed here.
  - `content.js` and the scout stay byte for byte, but every build installed for a test carries
    the new script. That needs the owner's go under the 2026-11-18 ruling.
  - The implementation pull request adds a unit test that pins `content.js`, the scout and
    `pcg_probe_inventory.js` to their blobs. No such test exists today.
- **`grayscale` rests on one obtained sample** (*Open question 2*).
- **"Obtained" is shared between the dexes.** A user who turns it on in one dex finds it on in the
  other. Every reading then refuses, with its message, until it is off.
- **Main-dex resets are invisible to the app until *Open question 3* is settled.** Until then,
  stores 1 and 2 can say Registered for a species the user has not caught again since a reset.
  Store 1 can do that today too. If the title counts runs, the recorded N closes the gap in the
  app. The server's lists stay as they were until the next upload.

## Open questions

1. **Does "Obtained" off in the alt dex hide the obtained forms?** Not observed: the account had
   no form. Reading 3 depends on it. If the answer is no, reading 3 needs "Obtained" on and the
   `grayscale` marker instead, at the cost of two taps on the shared filter.
2. **Is `grayscale` absent on every obtained entry, and present on every missing one, with many
   entries obtained?** Observed with one obtained entry. The counter's obtained number gives the
   check: it must equal the count of entries without `grayscale`.
3. **Does the page title's "Pokedex #N" count main-dex runs, and what else shows a reset?** Not
   observed. Resetting changes game state, so a test never does it. The dev account's title read
   "Pokedex #1".
   - If N counts runs, stores 1 and 2 record it (*Main-dex resets*).
   - If not, the question stays open, with no time-based expiry in its place.

## Capture method for PCG page investigations

This is the owner's decision, recorded on 2026-11-23 as the method for investigating the PCG
page.

- **Dev app only, never the stable.** The capture build is a dev debug build installed over the
  dev app (`com.fs.twitchminichat.dev`) with `adb install -r`, data kept. The stable app is not
  opened, rebuilt or reinstalled.
- **The probes stay as they are.** `content.js`, `pcg_tab_state_scout.js` and
  `pcg_probe_inventory.js` stay byte-identical, checked by blob in the tree and inside the APK.
  - The capture is a separate passive script, last in the manifest. It reads the document and
    saves it through one `when` branch and one writer class, and clicks, scrolls and sends
    nothing to PCG.
  - There is no hook on "Update Inventory" or "Update Pokédex", and neither is pressed during the
    round.
- **Throwaway.** It is built in a separate worktree, never committed and never pushed, and the
  worktree is removed afterwards. The Windows entry on `git worktree remove` in `AGENTS.md`
  applies: delete `app/build` first.
- **Captures stay out of every repository.** They are copied off the phone and verified by sha256
  before they are deleted there, in a separate step. Before any use they are screened for the
  account name, the Twitch login, tokens and other personal data. Only extracts such as names
  and numbers reach a repository.
- **The clean dev build is restored and verified by hash.**
  - Reinstall the clean dev build. Check its sha256 on the phone against the built APK, and check
    that `firstInstallTime` is unchanged.
  - Open the PCG screen once, then check that `extensions.json` lists `pcg-probe@example.com` at
    the clean version and that no capture runs any more.
- **Each round needs the owner's go.** The decision covers the method, not any particular build.

Two things the 2026-11-23 round learnt about the method:

- **The capture build raises the manifest's version** (1.2.1 to 1.2.1.x), because GeckoView is
  documented to keep the extension it already has otherwise; every capture build raised it, so
  that case was not observed. The clean build's own version then brought the clean extension
  back, after the PCG screen was opened once (observed).
- **GeckoView's message bridge rejects an array of arrays of mixed types** ("Invalid event data
  property"). Metadata is sent as a JSON string.

## Device checks for the implementation

All on the dev app. The stable app's accounts page is never screenshotted.

1. **Reading 1 behaves exactly as before.** Run the same sequence twice, with the same account
   and the same page states: first on the clean dev build of `main-v5` from before the change,
   then on the new build.
   - The blobs of `content.js` and the scout inside the installed APK equal `a11a3a0`'s.
   - In the main dex, "Update Pokédex" is enabled and disabled in the same states on both builds.
   - With Spawnable alone, one tap gives the same `PCG_PROBE` lines in the same order on both
     builds:
     - the passive extraction;
     - "manual pokedex registration completed from recent passive Spawnable-only snapshot", or
       "armed" followed by the completion;
     - the upload, and the same toast.
   - Store 1's snapshot holds the same missing keys on both builds, and their count equals the
     counter's shown number. Its only new content is the run number, if *Open question 3*
     confirmed it.
   - The upload carries the same `wanted_pokemon`. On the server, `[DEX] saved … wanted_count=`
     reads the same.
   - The rejections are the same:
     - outside the Pokédex tab: `pcg_pokedex_wrong_tab`;
     - with a filter combination that is neither reading 1's nor reading 2's, such as Spawnable +
       Starters: `pcg_pokedex_spawnable_only_required` at once.
   - This check presses "Update Pokédex" in a device test, so it needs the owner's go under the
     2026-11-18 ruling.
2. **Reading 2, through "Update Pokédex".**
   - Accepted with Starters + Legendaries on, Spawnable and Obtained off. Its count equals the
     counter.
   - Store 1 is not written.
   - Every other combination takes today's path, as in check 1.
3. **Reading 3, through "Update Alt Dex".**
   - Accepted in the alt dex with Obtained off.
   - Refused in the alt dex with Obtained on, with its message.
   - Never offered in the main dex: the button is disabled there.
4. **The buttons' enabling.**
   - Main dex: "Update Pokédex" enabled as today, and "Update Alt Dex" disabled.
   - Alt dex: "Update Pokédex" disabled, and "Update Alt Dex" enabled.
   - Before the new script's first state arrives, "Update Pokédex" behaves as today, and "Update
     Alt Dex" is disabled.
   - `content.js` never extracts in the alt dex.
5. **Panel.**
   - A starter, a legendary and an alt form, each before and after its reading.
   - Logcat tags, and the panel's text, as the observable results.
6. **Backend**, after the server change is deployed:
   - an upload with the field, then one without it, leaves the field stored;
   - a spawn of a missing starter targets the profile.
7. **Clean-up:** the profile's removal and the local-data reset clear stores 2 and 3.
8. **A reset:** checked only if *Open question 3* was confirmed, and the owner resets the main dex
   for the owner's own play. A test never does.
   - The page then shows a new N.
   - Stores 1 and 2 read as stale, and their species as Unknown, until each one's next reading.

## The second capture round

After this note is approved, on the dev app, with the owner's own PCG account, which has a real
Pokédex. It follows the method above. It answers open questions 1 and 2, and question 3 as far
as the title can.

| Page state | What it answers |
|---|---|
| Alt Dex, Obtained off | question 1: are obtained forms hidden? |
| Alt Dex, Obtained on | question 1, by difference; question 2 in the alt dex |
| Main Dex, Spawnable + Obtained | question 2 in the main dex, with the counter |
| Main Dex, Starters + Legendaries + Obtained | question 2 for the 🎊 and 🔒 entries |
| Main Dex, Starters + Legendaries | reading 2's state, with a real account |
| Main Dex, any of the above | question 3: the title's "Pokedex #N", against the main-dex runs the owner knows the account has had |

**Question 3 is settled only if the title's N matches the runs the owner knows of.** Otherwise it
stays open. A test never resets the main dex.
