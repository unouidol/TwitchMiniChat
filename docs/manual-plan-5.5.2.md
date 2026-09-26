# Manual test round — 5.5.2

The device round for the four 5.5.2 changes, in one pass. It stands alone: every log line and
every on-screen string below was read out of the code at the commit named here, so no pull
request needs to be open beside it.

## Which build this round applies to

| | |
|---|---|
| Branch and commit | `main-v5` at `80c042c` |
| Flavour | **dev** (`:app:assembleDevDebug`) |
| Label on the login screen | **`Version 5.5.1-dev (build 8, 80c042c)`** |

Read that label at the foot of the login screen before starting. It comes from
`app_version_label`, `Version %1$s (build %2$d, %3$s)`, filled with `versionName` `5.5.1`
plus the dev flavour's `-dev` suffix, `versionCode` 8, and `BuildConfig.GIT_SHA`.

**A round run on a different label is not this round.** In particular:

- a trailing `+`, as in `80c042c+`, means the build came from a tree with uncommitted or
  untracked changes. It is not this commit, and results from it do not belong in this round;
- `unknown` in place of the commit means the build could not read git at all;
- any commit other than `80c042c` is a different build. Rebuild rather than reinterpret.

`versionCode` and `versionName` are deliberately unchanged from 5.5.1 — this round happens
**before** the release branch raises them.

## What the round needs

- One Twitch account that can sign in, and a second one for the two-account cases.
- Access to the backend registry (Control Center) to read this device's `profile_ids`.
- A way to see a spawn that would match the account's alert selection, to confirm alerts
  arrive or stop.
- Logcat, filtered to these tags:

| Tag | What writes it |
|---|---|
| `FCM` | the boot registration pass in `MainActivity` |
| `FCM_REGISTER` | `FcmRegistrationUploader` — registration, alert selection, token deletion |
| `OWED_SERVER_OP` | the owed-work queue: `OwedServerOperationRunner` and its worker |
| `ACCOUNT_REMOVE` | `AccountProfileRemovalController` |
| `DEVICE_DELETE` | the single erase |
| `TOTAL_DELETE` | *Delete app account and all data* |
| `LOCAL_CLEAR` | *Reset local data, keep accounts* |

`DEVICE_DELETE` is the tag of the merged erase, kept from the action it replaced so existing
filters keep matching. It is not a typo for a deleted option.

## Order, and why

Non-destructive checks first, then the connected-network behaviour, then everything that needs
flight mode grouped together, then the erases. The erases sign every account out, so anything
run after one of them needs a fresh sign-in; that is why they are last.

Each case says **Do**, **Proves it** and **Failing looks like**. A case whose "proves it"
evidence is absent has not passed — an absent log line is a failure, not a pass with missing
paperwork.

---

## A. Non-destructive

### A1 — The reset dialog reads correctly and its buttons are reachable

**Do.** Safety & Privacy → Data & Account Control → *Reset local data*. Read the whole message.
Try it on the shortest screen available, and in landscape.

**Proves it.** Four paragraphs: what the dialog is for, what *keep accounts* clears, what
*erase everything* does, and the note about using the X for a single account. Exactly **two**
action buttons above *Cancel* — *Reset local data, keep accounts* and *Erase everything and
remove this device from the server*. The dialog scrolls if the text does not fit, and every
button can be reached.

**Failing looks like.** Three action buttons, which would mean the old local-only erase is
still wired. A button pushed off the bottom with no way to scroll to it. Text describing
"the last one", which no longer exists. Dismiss with *Cancel* if anything is wrong — nothing
here has been erased yet.

### A2 — The two rewritten pages render

**Do.** Safety & Privacy → open **Data Deletion**, then **Privacy Policy**. Scroll both to the
end. Then trigger the acceptance gate (a fresh install, or after A4) and open the same pages
from inside it.

**Proves it.** Both pages show `Last updated: 2026-09-26`. The deletion page has four
sections plus the email, retention and timing sections, and it has **no** section called
*Erase everything on this device*. The privacy policy's section 5 has a bullet beginning
"Removing one account does not by itself remove everything kept for it". Navigation links at
the top all work, in both the in-app and the acceptance-gate context.

**Failing looks like.** `Last updated: 2026-09-15`, which means an old copy is bundled. A
section about *Erase everything on this device*. Text running off the right edge, or a
navigation link that does nothing. The deletion page is one section's worth of bullets longer
than before, so clipping would show up here first.

### A3 — An active profile registers exactly as before

**Do.** Sign in, leave at least one alert category on, force-stop the app and reopen it.

**Proves it.** `FCM Boot registration pass profileCount=1`, then the register call, then
`FCM_REGISTER Alert selection sent ok=true deliveryRequired=true afterRegistration=true`.
A matching spawn still produces a notification.

**Failing looks like.** `deliveryRequired=true` with `afterRegistration=false`, which would
mean the token registration was skipped for an active profile. No `Alert selection sent` line
at all. Alerts that stop arriving — this case is the guard that 5.5.2 did not break the
ordinary path.

### A4 — *Reset local data, keep accounts* is unchanged

**Do.** With alerts on, run *Reset local data, keep accounts*.

**Proves it.** Toast **"Local data cleared, accounts kept"**. `LOCAL_CLEAR keep-accounts
result deletedSharedPrefs=…`. The app restarts, asks you to accept the terms again, and the
accounts are **still signed in**. In the backend registry this device keeps the same
`profile_ids` entry — no second record appears for this phone.

**Failing looks like.** Accounts gone. A second device record in the registry, which would
mean the device credential was erased. No terms prompt.

---

## B. Connected network

### B1 — Removing one account tells the backend, and the session goes

**Do.** With two accounts signed in and alerts on for both, remove one with the X next to it.

**Proves it.** `ACCOUNT_REMOVE removeAccountFromDevice requested`, then
`local removal finished removedAccount=true`, then
`ACCOUNT_REMOVE backend notification disable completed ok=true backendSessionRemoved=true`.
The usual removal toast, and **no** second toast. In the registry, the removed profile is gone
from this device's `profile_ids` and the other one is still there. A spawn that used to match
the removed account produces nothing; the remaining account still alerts.

**Failing looks like.** `backendSessionRemoved=false` after `ok=true` — the session should go
once the disable is acknowledged. A second toast about alerts continuing, which would mean the
request failed. The removed profile still in `profile_ids`. The **remaining** account losing
its alerts, which would be the worst outcome here.

### B2 — Switching every category off tells the backend

This is the case #51 exists for.

**Do.** For one profile, set the ordinary mode to *No spawns*, event spawns off, Most Wanted
off. Force-stop the app and reopen it.

**Proves it.** `FCM_REGISTER Alert selection sent ok=true deliveryRequired=false
afterRegistration=false`, and **no** register call for that profile in the same pass. In the
registry, that profile is no longer in this device's `profile_ids`. A spawn that used to match
produces no notification.

**Failing looks like.** No `Alert selection sent` line at all — the old behaviour, where the
profile was skipped and the backend was never told. `afterRegistration=true`, which would mean
a token was registered for a profile that wants no notification, putting the registration
straight back. The profile still in `profile_ids` after the restart.

The backend **drops the profile from this device's list**. It does not delete that profile's
data: uploaded Pokédex lists and sign-in records stay, and the registry should still show them.

### B3 — It does not repeat at every start

**Do.** Immediately after B2, force-stop and reopen the app again. Then a third time.

**Proves it.** `FCM_REGISTER registration pass skipped: backend already holds this selection`,
and **no** `set_spawn_alert_mode` request in the pass.

**Failing looks like.** `Alert selection sent ok=true deliveryRequired=false` again, which
means the acknowledged-selection record is not being read or not being written — one request
per launch, for ever, on any phone with its alerts switched off.

### B4 — Turning alerts back on resumes them

**Do.** After B3, re-enable one category for that profile. Force-stop and reopen.

**Proves it.** The register call **and**
`Alert selection sent ok=true deliveryRequired=true afterRegistration=true`. The profile is
back in this device's `profile_ids`, and a matching spawn alerts again.

**Failing looks like.** Alerts that never resume, or a pass that sends nothing because the
disabled selection is still treated as acknowledged.

### B5 — Two profiles, one of them off

**Do.** One account with a category on, one with everything off. Force-stop and reopen.

**Proves it.** `FCM Boot registration pass profileCount=2`. One profile registers and sends an
active selection; the other sends `deliveryRequired=false` and registers nothing. The registry
shows only the active profile in `profile_ids`. Alerts arrive for the active account only.

**Failing looks like.** Either profile affecting the other: the active one losing its
registration, or the inactive one keeping it. One profile processed and the other skipped
entirely.

---

## C. Flight mode, then restore

Group these together — each needs the network off at the moment of the action and back on
afterwards. Turn flight mode **on before** the action, not during it.

### C1 — An account removal that cannot reach the backend

**Do.** Two accounts signed in, alerts on. Flight mode on. Remove one with the X.

**Proves it.** The account disappears from the list, then a second toast: **"This account is
gone from this phone, but alerts for it may keep arriving for a while. Twitch Mini Chat keeps
trying to switch them off on its own."** Logcat: `ACCOUNT_REMOVE backend notification disable
completed ok=false; recorded as owed, backend session kept`, and `OWED_SERVER_OP owed work
enqueued`.

**Failing looks like.** No second toast, so the user is told nothing. A log line saying the
session was removed — it must be kept, because it is what the retry authenticates with. The
account still in the list.

### C2 — …and the queue finishes it

**Do.** After C1, turn flight mode off. Reopen the app.

**Proves it.** `OWED_SERVER_OP owed profile disable decision=DISABLE`, then
`owed profile disable sent ok=true`, then
`owed profile disable acknowledged backendSessionRemoved=true`. The removed profile is gone
from this device's `profile_ids`. Alerts for it have stopped; the remaining account still
alerts.

Worth watching, though not a pass/fail: whether the background worker gets there before the
app is reopened. Leave the app closed for a while with the network on and see whether
`OWED_SERVER_OP` lines appear on their own. The vendor's battery management may defer it
indefinitely, which is exactly why the start-up attempt exists.

**Failing looks like.** `decision=NOTHING_OWED`, which means the owed record was lost.
`sent ok=false` with no further attempt. `backendSessionRemoved=false`, leaving the session
behind after it was no longer needed.

### C3 — An owed disable is void once the account signs in again

**The case that would break a working install if it were wrong. Do not skip it.**

**Do.** Reach the owed state as in C1. **Before** turning the network back on, turn flight
mode off only long enough to sign the **same** account in again — or sign it in and then watch
the next pass. Then let the queue run.

**Proves it.** `OWED_SERVER_OP owed profile disable decision=VOID_SIGNED_IN`, and **no**
`owed profile disable sent` line. Alerts for that account work normally afterwards, and it is
present in this device's `profile_ids`.

**Failing looks like.** `decision=DISABLE` followed by `sent ok=true`. That switches off the
registration the sign-in just created, and **the only symptom is notifications that never
arrive** — there is no error and no toast. If this happens, alerts for that account will be
silently dead until the selection is changed again.

### C4 — A keep-accounts reset does not cancel owed work

**Do.** Reach the owed state as in C1, stay in flight mode, and run *Reset local data, keep
accounts*. Then turn the network on and reopen the app.

**Proves it.** The owed disable still completes: `decision=DISABLE`, `sent ok=true`,
`acknowledged`. The reset must not have dropped it.

**Failing looks like.** `decision=NOTHING_OWED` after the reset — the record was cleared by a
reset that is not supposed to touch it, and the orphaned alerts would stay on with nothing
left to notice them.

### C5 — Both kinds of owed work in one pass

**Do.** Reach an owed disable as in C1 and an owed token deletion as in D2 below, then turn the
network on and reopen the app.

**Proves it.** One `OWED_SERVER_OP` pass reports both:
`owed token deletion decision=RUN` with `FCM_REGISTER delete_firebase_token completed ok=true`,
and `owed profile disable decision=DISABLE` with `sent ok=true`.

**Failing looks like.** Only one of the two acted on, with the other still owed after the pass.
A failure in one kind preventing the other from being attempted at all.

Because D2 erases the phone, run this case **after** D2 and sign back in to set up the
removal side, or accept that it repeats part of D2.

---

## D. Erases — destructive, run last

Each of these signs every account out. Expect to sign in again between them.

### D1 — The erase, online, with an account signed in

**Do.** Signed in, network on. *Reset local data* → *Erase everything and remove this device
from the server*.

**Proves it.** Toast **"Everything erased, and this device removed from the server"**. Logcat,
in this order: `DEVICE_DELETE start profileCandidateCount=1`,
`Server device removal completed ok=true`, `Firebase token deletion ok=true`,
`Gecko data clear completed ok=true`, `local deletedSharedPrefs=…`,
`erase outcome=REMOVED_FROM_SERVER`. The app restarts to the login screen with no accounts and
asks for the terms again. This device is gone from the registry. A spawn that used to match
produces nothing.

**Failing looks like.** `erase outcome=` anything else while both requests reported `ok=true`.
The phone not actually erased — accounts still present after the restart. The device still in
the registry. The built-in browser still signed in to the Pokémon Community Game pages, which
means the browser data was not cleared.

### D2 — The erase, offline: the phone is erased anyway

**Do.** Sign in, alerts on, then flight mode on. Run the same erase.

**Proves it.** Toast **"Everything on this phone is erased, but alerts can still arrive. Twitch
Mini Chat keeps trying to stop them on its own, as soon as this phone is online again."**
Logcat: `Server device removal completed ok=false`, `Firebase token deletion ok=false`,
`local deletedSharedPrefs=…`, `erase outcome=NOTHING_REACHED`,
`Firebase token deletion recorded as owed`, `OWED_SERVER_OP owed work enqueued`. **The phone is
fully erased** — no accounts, terms asked again.

Then turn the network on and reopen: `OWED_SERVER_OP owed token deletion decision=RUN` followed
by `FCM_REGISTER delete_firebase_token completed ok=true`.

**Failing looks like.** Nothing erased because the server could not be reached — that is the
old behaviour and the main thing this change removes. A toast claiming the device was removed
from the server. No owed record, so the token stays alive for ever and the phone keeps
receiving alerts. `decision=NOTHING_OWED` on the pass after the network returns.

### D3 — An owed token deletion is void once an account signs in again

**The second case that would break a working install. Do not skip it.**

**Do.** Reach the owed state as in D2. **Before** the token deletion succeeds, sign an account
back in. Then let the queue run.

**Proves it.** `OWED_SERVER_OP owed token deletion decision=VOID_SIGNED_IN`, and no
`delete_firebase_token` line. Alerts for the newly signed-in account work normally.

**Failing looks like.** `decision=RUN` and a token deletion going through. That cuts the alerts
of a registration just recreated, silently — no error, no toast, alerts simply never arrive.

### D4 — The erase with no accounts left

This is the field case that started the work: a phone with no accounts that kept receiving
spawn alerts for days.

**Do.** From the state after D1 or D2 — no accounts signed in — run the erase again, with the
network on.

**Proves it.** Toast **"Everything on this phone is erased and it has stopped receiving alerts.
This device could not be removed from the server now; the server drops the registration the
next time it tries to reach it."** Logcat: `Server device removal completed ok=false` — there
is no backend session to authenticate with, and that is by design — with
`Firebase token deletion ok=true` and `erase outcome=ALERTS_STOPPED`.

**Failing looks like.** `erase outcome=NOTHING_REACHED` with the network on, which would mean
the token deletion failed too. A toast claiming the device was removed from the server. Alerts
still arriving afterwards for a profile that was on the phone.

Note what this case does **not** promise: the record can remain in the registry with a dead
token until the backend next tries to send to it. If no spawn ever matches those profiles
again, it can sit there. That is expected, and the page says so.

### D5 — A failed browser clear no longer cancels the erase

This is #40's case E1 with its meaning reversed. It used to check that a failed browser clear
erased nothing; it now checks that the erase completes anyway.

**Do.** Make `GeckoSessionManager.clearAllWebData` fail. Then run the erase.

**Proves it.** `DEVICE_DELETE Gecko data clear completed ok=false`, **followed by**
`DEVICE_DELETE local deletedSharedPrefs=…` — that second line is what proves the wipe still
ran. The toast carries the outcome **and** the clause "The built-in browser data could not be
cleared: …". The phone is erased: no accounts, terms asked again.

**Failing looks like.** No `local deletedSharedPrefs=` line after the `ok=false`, meaning the
erase aborted — the old behaviour. A toast with no mention of the browser failure, which would
hide it. The phone left half erased in some other way: accounts gone but settings intact, or
the reverse.

### D6 — *Delete app account and all data* still refuses when offline

The two paths differ here deliberately, and only this case shows it.

**Do.** Sign in, flight mode on, then *Delete app account and all data* and confirm.

**Proves it.** An error toast carrying the server's message. `TOTAL_DELETE Server deletion
completed ok=false` and **no** local wipe line. **Nothing is erased**: the accounts are still
there, the settings are still there, the terms are not asked again.

**Failing looks like.** The phone erased despite the failure. That would tell the user their
server-side profile data was deleted when it was not, and unlike the erase there would be
nothing left on the phone to try again with.

### D7 — *Delete app account and all data*, online, and its browser failure

**Do.** Sign in, network on, run it. Then, separately, make the browser clear fail and run it
again.

**Proves it.** Online: `TOTAL_DELETE Server deletion completed ok=true`,
`Firebase token deletion ok=true`, `Gecko data clear completed ok=true`,
`local deletedSharedPrefs=…`, and the phone is erased. With the browser clear failing:
the toast **"Server delete OK, but Gecko wipe failed: …"** and **no** local wipe line — this
path still aborts on a browser failure, unlike D5.

**Failing looks like.** The token deletion happening before the server call, or not at all.
A local wipe after the browser failure, which would mean the two paths were accidentally made
the same.

---

## What this round cannot check, and why

- **The true upgrade case.** #51's behaviour on an installation that has acknowledged nothing
  cannot be reproduced by installing this build over the released one: the dev flavour is
  `com.fs.twitchminichat.dev`, a separate application with its own data, so it installs beside
  5.5.1 rather than over it and always starts empty. The nearest reachable equivalent is a
  fresh dev install signed in while offline, so the first registration never succeeds and
  nothing is acknowledged, then every category switched off, then online and restarted: the
  expected result is `Alert selection sent ok=true deliveryRequired=false`. Testing the real
  upgrade needs a **stable** build, which is a release-branch job, not this round.
- **The backend's pruning of a dead token.** That a registration is dropped the next time the
  backend tries to send to a deleted token happens inside the backend, on its schedule. D2 and
  D4 check that the token is deleted; whether and when the record disappears is a registry
  observation over time, not a step with a pass condition.
- **The worker's behaviour under a long failure.** The retry shape — exponential backoff from
  30 s, giving up after 8 attempts per enqueue while leaving the record owed for the next
  start — is not reachable by hand in a sitting. `OWED_SERVER_OP worker attempt failed
  runAttemptCount=…; retrying` and `giving up this enqueue runAttemptCount=…; work stays owed`
  are the lines to look for if it ever shows up in a real log.

## Recording the result

Note, for each case, pass or fail and the evidence line you actually saw. A case recorded as
passing without its evidence cannot be checked by the next reader, and this round exists to be
checked: it is the only verification 5.5.2 has for anything involving GeckoView, Firebase, the
backend registry or the software keyboard.

Put the outcome in `RELEASE-STATE.md` under the test device section, with the date and the
label read in step one — not the label expected, the one on the screen.
