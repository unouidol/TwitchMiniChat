# Twitch Mini Chat Repository Guidelines

## Scope and project baseline

These instructions apply to the whole repository unless a more specific nested `AGENTS.md` overrides them.

Twitch Mini Chat (TMC) is an Android application built with Kotlin, XML layouts, AppCompat, and GeckoView. The Android package is `com.fs.twitchminichat`; the application has `stable` and `dev` product flavors. Do not introduce Jetpack Compose unless the task explicitly requires it.

Important areas:

- `app/src/main/java/com/fs/twitchminichat/`: chat, authentication, notifications, local stores, and application UI.
- `app/src/main/java/com/fs/twitchminichat/pcg/`: GeckoView sessions and Pokemon Community Game (PCG) integration.
- `app/src/main/assets/pcg_probe/`: passive WebExtension and Document Object Model (DOM) probes.
- `app/src/main/res/`: XML layouts and user-facing resources.
- `app/src/test/`: local unit tests.

## Architecture rules

- Keep `ChatFragment.kt` focused on lifecycle and UI orchestration. Put new protocol, persistence, networking, parsing, policy, and state logic in focused controllers, stores, providers, clients, builders, or helpers.
- Preserve the separation between runtime Smart Presets and persistent User Presets.
- Treat `GeckoSessionManager` as the owner of GeckoView sessions, PCG probe messages, snapshots, cache validation, and manual acquisition windows.
- Keep PCG probes passive. Android code remains the source of truth for validating the active tab, accepting snapshots, and deciding when manually requested data may be captured.
- Reuse existing abstractions before adding parallel implementations. Avoid duplicating authentication headers, network behavior, preference keys, message ordering, or reconnect policy.
- Put user-visible strings in `app/src/main/res/values/strings.xml`; do not hardcode them in Kotlin or layouts.
- Use AndroidX `SharedPreferences.edit { ... }` where appropriate.
- Add KDoc (`/** ... */`) to important classes, objects, functions, properties, and constants. Use inline comments only to explain non-obvious state, source-of-truth decisions, or side effects.
- Expand technical acronyms on first use in documentation or user-visible text.

## PCG gameplay safety

TMC is a manual assistive client. These constraints are mandatory:

- Never implement auto-catch or automatically send gameplay commands from a spawn, recommendation, timer, notification, snapshot, or inventory change.
- Never queue or automatically retry gameplay commands.
- Never bypass Twitch or PCG cooldowns.
- Chat commands, presets, Quick Catch, and similar shortcuts must be user-triggered. One deliberate tap may cause at most one visible action.
- Automatic behavior may observe, cache, notify, recommend, or display information. The user must remain responsible for every gameplay or chat action.
- Describe these features as quick chat commands, manual command shortcuts, or Stream Deck-style manual shortcuts.

## Security, privacy, and backend compatibility

- Never commit OAuth tokens, backend keys, Firebase credentials, signing material, device secrets, real `google-services.json` files, or production user data.
- Use backend session Bearer authentication where supported. Never downgrade an invalid or rejected Bearer session to a legacy key automatically.
- Do not log tokens, secrets, complete authorization headers, chat message bodies, or unnecessary personal identifiers.
- Never read a file that holds secrets in full. Extract only the line the task needs, as in taking `sdk.dir` from `local.properties`, so that a keystore password or a token never reaches a transcript, a log, or a summary. "I only needed one value" is not a reason to open the rest.
- Do not modify untracked local configuration outside the objective of the task. `local.properties`, per-flavor `google-services.json` and their kind are deliberately absent from git, so an edit to them cannot be reviewed in a diff, cannot be reverted with `git checkout`, and cannot be recovered once overwritten.
- Keep Firebase Cloud Messaging (FCM) registration, profile deletion, safety/privacy controls, and local-data reset behavior coherent with backend contracts and published disclosures.
- Every policy page exists twice, and nothing keeps the two copies aligned. One is bundled in the APK under `app/src/main/assets/policies/`; it is what users read in the acceptance gate and in Safety & Privacy, so it is the copy that matters most. The other is published at `https://unouidol.github.io/tmc/`, from the `tmc/` directory of the `unouidol/unouidol.github.io` repository; it is what the app's web button and anyone outside the app reach. Change both in the same step, with the same wording and the same "Last updated" date. The published file names use hyphens where the bundled ones use underscores (`data-deletion.html`, `data_deletion.html`). Publish the web copy before the release that bundles the new text, never after. Aligning only one copy leaves the two disclosures contradicting each other with nothing in this repository to show it.
- Treat authentication, deletion, reporting, and notification changes as coordinated Android/backend changes. Introduce backward-compatible server behavior before depending on it in the app; remove legacy behavior only after deployed clients have migrated.
- Fail safely on malformed or unauthenticated backend responses. Do not silently convert an authorization failure into a less secure mode.

## Change discipline

- Start each task from the current canonical branch and use a focused feature branch. Do not work directly on `main-v5`.
- Preserve unrelated local changes. Never use destructive Git commands or broad staging when the worktree contains user changes.
- Keep each pull request limited to one behavior or maintenance objective. Separate functional changes from unrelated refactors.
- Add or update characterization tests for protocol, authentication, parsing, ordering, retry, persistence, and policy changes.
- Do not use `clean` as a routine verification step; it hides incremental-build behavior and wastes time.
- Before adding diagnostic instrumentation, state which recorded value would separate the healthy case from the faulty one. If no field being recorded can tell them apart, the instrument is not ready: it will produce evidence that cannot settle the question it was built to answer, and the gap only becomes visible after the next occurrence has already been missed.
- Do not edit generated files or commit local build outputs, APKs, Android App Bundles, credentials, or patch backup directories.

## Git failures already diagnosed

Each entry is a failure seen before: its symptom, how to recognise it, and what worked. None records the state of a machine. Whether a machine has the problem is read on that machine when it happens, never taken from here: a recorded machine state goes stale by itself, and a reader on another machine would apply it to theirs. Where and when an entry was seen is an observation, not a property that still holds.

**A `git push` that hangs looks like a network problem, and is not.**

- **Symptom:** the push prints nothing, not even its object count, and does not return.
- **How to recognise it:** the GitHub command-line interface (`gh`) works, and `git fetch` and `git ls-remote` against the same remote complete. That proves nothing about credentials: while the repository is public they need none, and a push is the first command that asks for any. Replacing git's credential helper for one command removed the hang, so the wait is in the helper. Do not debug the network, the proxy or GitHub.
- **What worked:** stop the hung push, check with `git ls-remote` whether it reached the remote anyway, then push once with `gh`'s credential in place of the helper, for that command only, in Git Bash:

  ```bash
  GIT_TERMINAL_PROMPT=0 GCM_INTERACTIVE=never timeout 90 git -c credential.helper= \
    -c 'credential.https://github.com.helper=!gh auth git-credential' push origin <ref>
  ```

  The two variables and `timeout` turn a wait into a failure that returns. The command borrows `gh`'s login, so check `gh auth status` first. It changes no configuration.
- **Observed** on nbpromec1 on 2026-09-30, where Git Credential Manager was found holding an invalid token and waiting on an interactive sign-in, and again on 2026-10-20, on a tag push.

**A worktree that git refuses to read, because its `.git` belongs to another account.**

- **Symptom:** every git command run inside the worktree fails with `fatal: detected dubious ownership in repository at '<path>'`, naming the owner and the current user, and suggests `git config --global --add safe.directory <path>`.
- **How to recognise it:** that message. `git worktree list`, run from the main clone, still lists the worktree and its commit: the worktree is registered and intact, and only git's ownership check is refusing it.
- **What worked:** the exception for one command, not the global one git suggests: `git -c safe.directory=<path> -C <path> status --porcelain`.
- **Why one command:** the check exists because a repository's configuration can make git run programs, so an exception declares that directory's owner trusted. Git's suggestion writes it into the global configuration, where it holds for every later command on that machine, outside this repository, where an edit cannot be reviewed in a diff or undone with `git checkout`: the reason given above for untracked local configuration. Reading a worktree's state needs that trust for one command, and the override ends with the command.
- **Observed** on nbpromec1 on 2026-10-20, with git 2.45.1, on a worktree whose `.git` was owned by `BUILTIN/Administrators`.

## Required verification

Run the checks relevant to the change. The repository-wide baseline is:

```powershell
.\gradlew.bat :app:testStableDebugUnitTest :app:testDevDebugUnitTest
.\gradlew.bat :app:lintStableDebug :app:lintDevDebug
.\gradlew.bat :app:assembleStableDebug :app:assembleDevDebug
```

On Linux or in GitHub Actions, use `./gradlew` with the same tasks.

Device-dependent behavior cannot be proven by local unit tests. Changes involving GeckoView, OAuth callbacks, Firebase Cloud Messaging, the software keyboard, external links, notifications, or layout behavior also require a short manual test plan and Logcat tags or observable results in the pull request.

**Every claim states its provenance.** Say whether something was executed or reasoned about statically, and never let the two read alike. A Gradle task reported as `UP-TO-DATE` did not run: its cached result may well still be valid, but presenting it as a fresh verification is false. Anyone writing "verified" reports the counts that support it — tests run and failed, warnings by severity — because a claim carrying no numbers cannot be checked by the next reader.

**A verification holds at the moment it was made, and not after.** Anything happening between the check and the act it authorizes voids it: a rebase, a commit from another session, a merged pull request, an edit to the working tree. Re-run it immediately before the operation it is meant to justify, rather than citing the earlier run.

**Address a continuous integration run by the exact SHA of the current head, never by branch.** A branch-scoped query returns the most recent run the service happens to know for that branch, which after a force-push is often the *previous* head's — and it is usually green, because that head passed. The answer then reads as a success for code nobody has tested, which is worse than an error, because nothing looks wrong. Read the head with `git rev-parse HEAD`, ask for that commit's run with `gh run list --commit <sha>` rather than `--branch`, and check that the SHA in the answer is the one asked about. The head the pull request itself reports lags a force-push by some seconds, so re-read it instead of trusting the first answer. Step 6 of the release procedure depends on this: "merge only after continuous integration passes" means the run for the commit being merged, and pinning the merge with `--match-head-commit` is worth nothing if the run that authorized it belongs to a different commit.

**A missing value is asked for, never guessed.** An instruction carrying an unresolved placeholder, a reference to something that does not exist, or a figure nobody measured is not carried out by inferring what was probably meant. Stop and ask. An invented value inside a binding specification is worse than a task left undone, because the undone task is visible and the invention is not: it reads exactly like a measurement, and every later decision made on top of it inherits the error without a way to notice.

**Lint configuration and comparison.** Lint must not report the remote version-availability checks (`GradleDependency`, `NewerVersionAvailable`, `AndroidGradlePluginVersion`). Their findings change with the dependency cache, the network, and whatever the library authors released that morning, so a count that includes them is a property of the world outside this repository rather than of the code in it, and two runs of the same commit can disagree.

A warning count is comparable only between runs of the same lint configuration. Obtain the reference by running lint on `main-v5` at the commit the branch was cut from. Do not rely on a figure recorded in a document: it is correct only until someone changes the configuration or fixes a warning, and nothing will announce that it has stopped being correct.

## Release and distribution

`RELEASE-STATE.md` is the source of truth for where every change currently is: what is
published, what is merged but unreleased, what is still waiting on a branch and behind which
condition, and what the test device is running. Read it before assuming a change has reached
users. A change whose position nobody recorded is one that will be re-derived from scratch
later, and answered wrongly at least once.

What is merged but unreleased is the one position the file does not list. It is read from the
log, with `git log --first-parent v<latest tag>..main-v5`, and a merge owes the file no edit.
This is a decision, not an omission, and its reason is mechanical: a squash merge's commit
identifier does not exist until the merge has happened, and a squash merge cannot edit its
own tree, so a row recording a merge can never land with that merge — it always costs a
second commit after the fact. The file recorded merges in a State column until #61
(`ddf9da4`) removed it on 2026-09-29. Its rows went stale one after another, and one needed a
pull request of its own to record itself: bookkeeping that produced work which then also
needed recording. The log cannot disagree with itself; a copy of it can. Do not put the list
back.

Edit the file only when a position that git cannot answer changes: a release is published,
work starts or stops waiting on a branch behind a condition, or the test device runs a
different build. Record the test device from the device, not from intent.

Published releases are consumed by the public website at `https://tmc.ircminichat.party/`,
which links straight to GitHub release assets and reads the version from the GitHub
releases application programming interface (API). The names and formats below are a
contract with that site, not a preference: breaking one of them breaks the public
download buttons silently, with no error anywhere in this repository.

- Tag every release `vMAJOR.MINOR.PATCH` on `main-v5`. The site takes `tag_name` from the
  `releases/latest` endpoint and strips the leading `v` to display the version, so a
  differently shaped tag shows a wrong version to every visitor.
- Name the published Android Package Kit (APK) assets exactly
  `TwitchMiniChat-Android-arm64-v8a.apk` and `TwitchMiniChat-Android-armeabi-v7a.apk`.
  The site links to `releases/latest/download/<name>`, which resolves only on an exact
  filename match. Gradle's own output names must be renamed before upload.
- Publish `SHA256SUMS.txt` next to them, listing the checksum of both APKs.
- Title releases `Twitch Mini Chat MAJOR.MINOR.PATCH`.
- Keep `versionName` equal to the tag without its leading `v`, and raise `versionCode` on
  every published build. A build whose `versionCode` is not higher than the installed one
  cannot update it.
- Raise the fallback version in `unouidol/tmc-site`, `index.html`, in the same step that
  raises `versionName`. The page fills its three `data-tmc-version` elements from the
  releases API at runtime and overwrites whatever the file says, so that number is what a
  visitor reads until the call answers, and what one whose browser never runs the script
  reads for good. Left behind, it advertises the previous release from the page that
  announces the new one.
- Write release notes for users rather than for developers, following the structure of the
  previous releases: what the application is, what changed, what to download, how to
  install, and contacts.

Release procedure:

1. Branch `release/X.Y.Z` from the current `main-v5` and land every change intended for the
   release on that branch.
2. Update `versionCode` and `versionName` in `app/build.gradle.kts`.
3. Run the repository-wide verification baseline, then `:app:assembleStableRelease`.
4. Verify the built artifacts before anything is published: the signer is the release
   certificate and not the Android debug certificate, the package is
   `com.fs.twitchminichat` without the development suffix, and `versionCode` and
   `versionName` match the intended release.
5. Install the arm64 APK over the currently published stable build on a real device and
   confirm it updates in place.
6. Open a pull request into `main-v5` and merge it only after continuous integration
   passes. Continuous integration runs only for `main-v5`, so a release tagged from a
   release branch alone is never verified by it.
7. Confirm that `main-v5` contains exactly the code the artifacts were built from, then tag
   it.
8. Publish the release with the exact artifacts that were verified and installed. Never
   rebuild between verification and publication: Android builds are not reproducible byte
   for byte, so a rebuilt artifact is not the one that was tested and its checksums no
   longer match.

## Definition of done

A change is complete only when:

- the intended behavior and preserved behavior are both stated;
- relevant tests are added or updated and pass;
- stable and dev lint/build tasks pass, or any intentional limitation is documented;
- the final diff contains no unrelated edits, credentials, generated artifacts, or sensitive logs;
- user-visible text is resource-backed and understandable;
- PCG gameplay remains manual under the rules above;
- the pull request documents risk, compatibility, validation, manual testing, and rollback.
