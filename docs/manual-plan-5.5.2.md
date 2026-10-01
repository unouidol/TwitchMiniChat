# Manual test round — 5.5.2

The device round for the four 5.5.2 changes, in one pass. It stands alone: every log line and
every on-screen string below was read out of the code at `50c3069`, the commit the round was
anchored to, so no pull request needs to be open beside it.

5.5.2 will never be tagged or published. It was shelved, and the next release is 5.6.0, which
carries everything 5.5.2 set out to do plus the chat work. "5.5.2" in this file names that work
as it was planned; it reaches users as 5.6.0.

## Which build this round applies to

**The anchor is retired.** The round applied to `main-v5` at `50c3069` only while 5.5.2 was
heading for a tag. What the anchor protected was that tag: the build released as 5.5.2 had to be
the application the round had tested. 5.5.2 will never be tagged, so there is nothing left for
the anchor to protect, and it is not a constraint on anything any more.

**Before 5.6.0 is released, the round has to be re-anchored and re-run against the final 5.6.0
build**, because the chat work changes the application in between. The two runs under *Status*
are results for the code they were made against, not for 5.6.0.

While the anchor stood, a build was matched to the round like this:

| | |
|---|---|
| Code the round covered | `main-v5` at **`50c3069`** |
| Built from | the tip of `main-v5`, after the check below |
| Flavour | **dev** (`:app:assembleDevDebug`) |
| Label on the login screen | `Version 5.5.1-dev (build 8, <commit>)` |

The build came from the tip of `main-v5`, and the label was not expected to read `50c3069`.
Every document merged after the code - including this file - moves the tip without changing the
application, so naming the acceptable commits in a list is wrong the moment the list is
written. The rule instead was:

```
git diff 50c3069 HEAD -- app/ gradle/ build.gradle.kts settings.gradle.kts gradle.properties
```

**Empty output meant the build was this round**, whatever commit the label named: nothing that
affects the application had changed since the code the round covered. Non-empty output meant the
code had moved, and this plan might no longer describe what the build did.

Read the label at the foot of the login screen before starting, and write down what it says. It
comes from `app_version_label`, `Version %1$s (build %2$d, %3$s)`, filled with `versionName`
`5.5.1` plus the dev flavour's `-dev` suffix, `versionCode` 8, and `BuildConfig.GIT_SHA`.

**Two labels disqualify a run outright**, whatever the `git diff` says:

- a trailing `+`, as in `50c3069+`: the build came from a tree with uncommitted or untracked
  changes, so no commit describes what is installed and the diff above proves nothing about it;
- `unknown` in place of the commit: the build could not read git at all, so the same applies.

In both cases commit or stash the tree and rebuild, rather than reinterpreting the result.

`versionCode` and `versionName` were deliberately unchanged from 5.5.1 in both runs: the round
was made **before** any release branch raised them.

## Status: executed in full, in two runs; to be re-run on the final 5.6.0 build

**First run, 2026-09-26**, on the dev flavour at **`80c042c`**: every case passed, no blocking
defect. That result stands for the code as it was then. The two corrections the round produced
landed afterwards, as `31cdedc` and `50c3069`, which is why the plan was re-anchored to `50c3069`
and these parts had to be run again:

- **A5**, the check for `31cdedc`.
- **D8**, the check for `50c3069`. It covers the only part of that change no unit test reaches.
- **C1 to C5** still describe the same behaviour, but `31cdedc` changed what the start-up pass
  logs on a healthy phone, so read A5 before trusting a C-section log capture.
- **D1, D2 and D4** still expect the same outcomes, and `50c3069` added a fourth outcome they must
  not now produce. D8 is where that is checked.

**Second run, 2026-09-30**, on the dev flavour against `main-v5` at **`bf7dd65`**: A5 and D8, the
two cases the first run never reached, were run, and both passed. No blocking defect was found in
either run.

Neither run is a result for 5.6.0: see *Which build this round applies to*.

Everything under **Naming**, **Capturing the log**, **Reading the backend registry** and
**Injecting an owed record** was learned by running it, and is written here because each of those
cost time or produced a wrong result the first time. Read them before starting, not when stuck.

## Naming: there is no "Erase everything on this device"

Use the labels exactly as the application shows them. Only these three actions exist, and two of
them are inside one dialog:

| Where | Exact label |
|---|---|
| Safety & Privacy → Data & Account Control | **Reset local data** (opens a dialog) |
| …first button in that dialog | **Reset local data, keep accounts** |
| …**second button in that dialog** | **Erase everything and remove this device from the server** |
| Safety & Privacy → Data & Account Control, separate button | **Delete app account and all data** |

**The device erase is the second button inside the *Reset local data* dialog.** Its full path is
*Reset local data* → *Erase everything and remove this device from the server*.

**"Erase everything on this device" does not exist.** It was removed in 5.5.2, and the phrase
survives in this file only in cases D5 and A2, where it names the *old* option on purpose. On the
first run, taking it for a live menu entry led to **Delete app account and all data** being run
twice in place of the erase — a different action, with a different scope and a different failure
rule — so two cases proved nothing and had to be redone. If a step below does not name one of the
four labels in that table, it is not naming a real control.

## Capturing the log

**Use the Logcat panel in Android Studio, filter `package:com.fs.twitchminichat.dev`, level
Debug.** That is the only method that worked for the whole round.

Two dead ends, both measured:

- `adb logcat -d TAG:D *:S` returns **nothing** for these tags. Do not conclude from silence that
  the code did not run.
- In PowerShell, redirecting with `>` writes UTF-16, which `findstr` then cannot read, so a grep
  over the saved file comes back empty. Pipe instead: `adb logcat -d | Select-String OWED_SERVER_OP`.

**Filter by package, not by process.** Several cases end in a wipe, after which the application
restarts under a new pid; a process filter stops following it and the lines after the restart —
which are usually the ones being checked — are lost.

## Reading the backend registry

The v10 console **does not print timestamps**. Do not use the order rows appear in it as evidence
of when something changed.

The authoritative time of a registry change is **`updated_at` in `registered_devices.json`**.
Read that field when a case asks whether a `profile_ids` change happened, and record it beside the
result.

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

This is also where to fix the naming in your head before section D: the second button **is** the
device erase. Nothing anywhere is called *Erase everything on this device*.

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

### A5 — No cancellation is reported when there is nothing to cancel

The check for `31cdedc`. The defect it guards against is a line that should not be there, but
**the case passes on a line that is present**, not on one that is absent: an absent line also
happens when the policy was never consulted, and then nothing has been tested. It is a step of its
own rather than a remark under A3, because a check nobody names is a check nobody runs.

**Do.** The same restart as A3 — one account signed in, alerts on, force-stop and reopen — and
read the whole start-up pass. One log capture serves A3 and A5 together.

**Proves it.** This exact line:

```
OWED_SERVER_OP owed profile disable decision=NOTHING_OWED
```

**No `owed profile disable` line at all is not a pass.** It is an instruction: force-stop and
reopen again, and read the next start-up pass. The owed-work attempt runs at start-up before the
registration, and it consults the policy only for profiles it already knows about - one with an
acknowledged selection, or one explicitly owed. On a phone where the acknowledgement does not exist
yet there is nothing to decide, so no line is written.

This is measured, not reasoned. On 2026-09-30 the first attempt produced no `owed profile disable`
line at all, because the candidate set was empty: the owed-work attempt ran at 16:20:50.019, and
the registration that writes the acknowledgement finished at 16:20:50.343, after it. The pass
condition this case used to state - the line below absent, `NOTHING_OWED` or no line at all both
accepted - was satisfied while nothing had been tested. The proof came from a second force-stop
and reopen: the acknowledgement existed, there was a candidate, and the line read
`decision=NOTHING_OWED`. Anyone running this on a cold install will hit the same thing.

**Failing looks like.** This line, at start-up on a phone where nothing was ever removed and no
disable ever failed:

```
OWED_SERVER_OP owed profile disable decision=VOID_SIGNED_IN
```

It means the policy is inferring a debt from the acknowledged selection alone, so every ordinary
registered profile with alerts on is reported as a cancelled debt and `clearOwedProfileDisable` is
written over nothing. Nothing breaks for the user, which is why it has to be looked for
deliberately: the damage is to the log, and the next person reading it has to rule out a defect
that is not there.

Not blocking on its own — no user-visible behaviour depends on it — but report it, because it
means `31cdedc` did not take.

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

## Injecting an owed record — needed by C3 and D3

**C3 and D3 cannot be reached by hand.** Both need an owed record to still be owed at the moment
an account is signed in again, and signing in needs the network — which is the same network that
makes the queue drain, within a couple of minutes on the test device. The window closes before it
can be used. Measured on the first run: every natural attempt drained instead of voiding.

The only way to exercise them is to write the owed record directly, with the application stopped.

**1. Stop the application.** Shared preferences are cached in memory and written back on exit, so
a running process would overwrite the file.

```
adb shell am force-stop com.fs.twitchminichat.dev
```

**2. Write the record.** `run-as` gives the file the application's own uid, which a plain
`adb push` would not.

For an owed **token deletion** (D3):

```
adb shell run-as com.fs.twitchminichat.dev \
  tee /data/data/com.fs.twitchminichat.dev/shared_prefs/owed_server_operations.xml <<'XML'
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="firebase_token_deletion_owed" value="true" />
</map>
XML
```

For an owed **profile disable** (C3), where `<profile-id>` is that account's profile identifier
as the registry shows it in this device's `profile_ids`, lowercase:

```
adb shell run-as com.fs.twitchminichat.dev \
  tee /data/data/com.fs.twitchminichat.dev/shared_prefs/owed_server_operations.xml <<'XML'
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <set name="profile_alert_disables_owed">
        <string><profile-id></string>
    </set>
</map>
XML
```

That `set` entry is the **explicit** owed marker, which is what makes the debt real rather than
inferred. C3 depends on it: a profile that merely has an active acknowledged selection and is
present on the phone owes nothing, and reports `NOTHING_OWED`.

**3. Reopen the application** with the account signed in and the network on, and read the
`OWED_SERVER_OP` line.

Both files can be written in one go to set up C5. The store is
`owed_server_operations.xml`, kept by a keep-accounts reset and written after a wipe — which is
why C4 and D2 can check it at all.

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

**Do.** **Inject the owed disable** — see *Injecting an owed record* above. Doing it by hand
does not work: signing the account back in needs the network, and the network drains the queue
within a couple of minutes, so the debt is gone before it can be voided. Sign the account in,
then reopen the application with the injected record in place.

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
removal side, or accept that it repeats part of D2. Alternatively inject both records at once —
one `owed_server_operations.xml` carrying the boolean and the set together — which is faster and
checks the same pass.

---

## D. Erases — destructive, run last

Each of these signs every account out. Expect to sign in again between them.

### D1 — The erase, online, with an account signed in

**Precondition, and it is not optional.** The account must be **registered**, which means:
sign in, then **force-stop the application and reopen it** before running the erase.
`uploadToken` runs from the boot registration pass, so an account added inside a session that was
already open has no device credential yet. Without it the erase never attempts the server call at
all — `FCM_REGISTER delete_device_data skipped: device credential missing` — `ok` comes back
false, and the case proves nothing about the online path. Measured on the first run, where it
produced an `ALERTS_STOPPED` that looked like a defect and was not.

**Do.** Signed in, registered as above, network on. *Reset local data* (dialog) → second button,
*Erase everything and remove this device from the server*.

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

**Do.** Sign in, alerts on, register as in D1, then flight mode on. Run the same erase
(*Reset local data* → second button).

**Measured, so expect it:** `FirebaseMessaging.deleteToken()` really does fail with no network —
it comes back as an `ExecutionException` and the line reads `Firebase token deletion ok=false` —
and really does succeed once the network is back. Flight mode is therefore a valid way to produce
the token debt; it does not silently succeed offline.

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

**Do.** **Inject the owed token deletion** — see *Injecting an owed record* above, and the
same reason: the network needed to sign an account back in is the network that drains the debt.
Sign an account in, then reopen the application with the injected record in place.

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

### D8 — An installation that was never registered says so

The check for `50c3069`, and **the only part of it no unit test reaches**: `notRegistered` is set
inside `resolveDeletionCredentials`, which is private, so the distinction between an *absent*
device credential and an *unreadable* one is read in the code and exercised nowhere. This case is
the only evidence that the flag reaches the policy at all.

Read it together with **D4**. D4 is the erase on a *registered* phone with no accounts left, which
must still report `ALERTS_STOPPED`. D8 is the erase on a phone that never registered. The two look
alike — both fail to authenticate the server call — and they must report differently.

**Do.** This reproduction is known because it happened by accident during the first round:

1. run the erase (*Reset local data* → second button) and let the app restart;
2. sign the account in again;
3. **do not force-stop or reopen the app.** That is the whole point: `uploadToken` runs from the
   boot registration pass, so within this session no device credential has been created;
4. run the erase again.

**Proves it.** `DEVICE_DELETE erase outcome=NOT_REGISTERED`, preceded by
`FCM_REGISTER delete_device_data skipped: device credential missing` — that line is unchanged from
before this case existed and is still how the state is recognised. On screen, the new message:

> Everything on this phone is erased. It was not registered for alerts, so there was nothing to
> remove from the server.

It promises nothing about a future removal and says nothing about the push token. If the browser
clear also fails, the browser clause follows it in the same toast, as for the other three outcomes.

**Failing looks like**, and this one is **blocking**:

- `erase outcome=ALERTS_STOPPED` or `erase outcome=NOTHING_REACHED` here means `notRegistered` is
  **not reaching the policy**. The user is then told their device could not be removed from the
  server and will be removed at the next attempt, when there was never anything to remove and no
  attempt will find anything. That is the defect `50c3069` exists to remove, and seeing it here
  means the change did not take;
- the old wording on screen, for the same reason;
- `erase outcome=NOT_REGISTERED` in **D4** instead, which would mean the flag is being set for a
  missing *session* rather than a missing *credential*, and a registered phone is being told it was
  never registered.

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
- **Whether an *unreadable* device credential is told apart from an absent one.** D8 covers the
  absent case, the one with a message of its own. A credential that exists and cannot be read is
  meant to stay an ordinary failure, and reaching that state means corrupting the stored value on
  purpose; it is read in the code and exercised nowhere.
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
label read in step one — not the label expected, the one on the screen. For any case that touched
the registry, record `updated_at` from `registered_devices.json` beside the result, since the
console gives no time of its own.

The runs of 2026-09-26 and 2026-09-30 are recorded in `RELEASE-STATE.md`. When the round is run
again, add a run rather than overwriting those: two runs disagreeing is information, and the older
result is what the newer one has to be compared against.
