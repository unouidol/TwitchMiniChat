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
- `app/src/main/assets/pcg_probe/pcg_tab_state_scout.js` (blob `23f1fd0`);
- the "Update Pokédex" reading behind them: its gate, its messages, `PcgPokedexSnapshotStore`, and
  its upload to `/upload_dex_list`.

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

Each reading has its own button. One tap takes at most one reading and sends nothing to chat.
None of them clicks, scrolls or changes a filter: the user sets the page, and the app only reads
it.

| | Reading 1 (today's, unchanged) | Reading 2 (new) | Reading 3 (new) |
|---|---|---|---|
| Dex label | "Main Dex" | "Main Dex" | "Alt Dex", class `active` |
| Spawnable | on | **off** | absent from the page |
| Starters | off | **on** | absent from the page |
| Legendaries | off | **on** | absent from the page |
| Obtained | off | off | off |
| Lists | the missing Spawnable species | the missing starters and legendaries | the forms not obtained |
| Read by | `content.js` | a new passive script | the same new script |
| Button | "Update Pokédex" | new | new |

**The gates exclude one another.**

- Reading 1 needs Spawnable on; reading 2 needs it off.
- Reading 3 needs the alt dex. There `content.js` already reports `valid=false` and never
  extracts (observed).
- No page state can satisfy two readings, so no tap can produce two readings.

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

## The three stores

They are three separate stores, each with its own preferences file and schema, never merged. Each
is per profile, and each is cleared when its profile is removed and by the local-data reset, like
`PcgPokedexSnapshotStore`.

| | Store 1: `PcgPokedexSnapshotStore` | Store 2: main dex outside Spawnable | Store 3: alt dex |
|---|---|---|---|
| Written by | reading 1, unchanged | reading 2 | reading 3 |
| Holds | missing name keys, `updatedAtMs` | missing name keys, the categories read (`STARTERS`, `LEGENDARIES`), `readAtMs` | obtained forms and the last reading's missing forms; each form as normalized key, raw name and sprite number; `readAtMs` |
| On a new accepted reading | replaced | replaced | obtained forms: **only grow**, by the known forms the reading does not list; missing forms: replaced |
| A main-dex reset in PCG | stale until the next reading (as today) | stale until the next reading | unaffected: PCG never resets the alt dex |

**Why store 3 only grows:** a form once obtained stays obtained, because PCG never resets the
alt dex. The price is that a wrong "obtained" cannot be corrected by a later reading, only by
clearing the profile's data. The acceptance checks above exist to keep a half-rendered list out.

Raw names and sprite numbers are stored because 22 alt-dex names are unknown to the catalog, and
15 of them resolve to no species (observed).

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

Whether a stored list stops counting after some age is the owner's decision. No figure is
assumed here.

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
  - a Spawnable species not in store 1, when store 1 has a reading; or
  - a starter or legendary not in store 2, when store 2 has a reading.
- **Unknown:** every other case. That includes a starter or legendary while store 2 has no
  reading (#134), and any species whose store has no reading.

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
- **Main-dex resets are invisible to the app** (*Open question 3*). Stores 1 and 2 can say
  Registered for a species the user has not yet caught again since the reset. That is true of
  store 1 today.

## Open questions

1. **Does "Obtained" off in the alt dex hide the obtained forms?** Not observed: the account had
   no form. Reading 3 depends on it. If the answer is no, reading 3 needs "Obtained" on and the
   `grayscale` marker instead, at the cost of two taps on the shared filter.
2. **Is `grayscale` absent on every obtained entry, and present on every missing one, with many
   entries obtained?** Observed with one obtained entry. The counter's obtained number gives the
   check: it must equal the count of entries without `grayscale`.
3. **What does the page look like after a main-dex reset?** Not observed. Resetting changes game
   state, so a test never does it. The title read "Pokedex #1"; whether it counts runs is not
   known. If it does, store 2 can record it and treat a different title as stale. Store 1 stays
   as it is.

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

1. **Reading 1 unchanged.**
   - The two scripts' blobs equal `a11a3a0`'s, inside the installed APK.
   - With Spawnable alone, `content.js` logs its extraction as before.
   - Store 1 is not written by readings 2 and 3.
2. **Reading 2.**
   - Accepted with Starters + Legendaries, Spawnable and Obtained off. Its count equals the
     counter.
   - Rejected, each with its message, for each other combination and in the alt dex.
3. **Reading 3.**
   - Accepted in the alt dex with Obtained off.
   - Rejected in the main dex, and in the alt dex with Obtained on.
4. **Gates.**
   - Each new button is enabled only in its own state.
   - `content.js` never extracts in the alt dex.
5. **Panel.**
   - A starter, a legendary and an alt form, each before and after its reading.
   - Logcat tags, and the panel's text, as the observable results.
6. **Backend**, after the server change is deployed:
   - an upload with the field, then one without it, leaves the field stored;
   - a spawn of a missing starter targets the profile.
7. **Clean-up:** the profile's removal and the local-data reset clear stores 2 and 3.

## The second capture round

After this note is approved, on the dev app, with the owner's own PCG account, which has a real
Pokédex. It follows the method above and answers open questions 1 and 2.

| Page state | What it answers |
|---|---|
| Alt Dex, Obtained off | question 1: are obtained forms hidden? |
| Alt Dex, Obtained on | question 1, by difference; question 2 in the alt dex |
| Main Dex, Spawnable + Obtained | question 2 in the main dex, with the counter |
| Main Dex, Starters + Legendaries + Obtained | question 2 for the 🎊 and 🔒 entries |
| Main Dex, Starters + Legendaries | reading 2's state, with a real account |

Question 3 stays open unless the owner chooses to reset the main dex. A test never does.
