# Device round — 5d, the chat timeline in a RecyclerView

**Which plan to run.** This plan covers 5d, the chat timeline moved from a LinearLayout in a
ScrollView to a RecyclerView fed only the shown rows, and the closing measurement of the start-up
work. Written before any of its device stages ran: every count below is a prediction, reasoned from
the code, and the measurement's rule was approved before any of its launches. Nothing here is
re-derived after a result is seen.

**A stage that reports no result has failed, whatever an exit status says.** No done line, no
results file, or a results file without its counts is a failure of that stage.

**The device** is identified at the start of each sitting with `adb devices -l`, never assumed.

## Which build each step applies to

| Step | Branch | Commit | APK (dev, arm64) sha256 |
|---|---|---|---|
| H1, its runs of 2026-11-02 at 09:33 and 09:35 | `feature/5d-recyclerview-timeline` | `fc63adb` | `1d377b6e3611d337c6caffb1f2ae6bc031895246ff6d0b4653b412bcde021342` |
| H1 from then on | `feature/5d-recyclerview-timeline` | `d11b629` | recorded under *Recorded results* as built |
| H1 with S3's landing check, H3, M, B1 | `feature/5d-recyclerview-timeline` | `b954eba` | recorded under *Recorded results* as built |
| H2 | one mutant of `b954eba` per run | built after the predictions | each read back from the phone |
| C, qualification, control, built on `DESKTOP-BCT68L9`; superseded on 2026-11-03, see below | `experiment/startup-5d-control` | `cc4c0cf` | `5cc84bb39e493453298bdd6ba1ed931bc019492c90ea9ebf1e4bc12a91cf2449` |
| C, qualification, 5d, built on `DESKTOP-BCT68L9`; superseded on 2026-11-03, see below | `experiment/startup-5d-treatment` | `1198c79` | `2e298b5c84f2a6f82719376ec32092c7e370136d7d5f6113766510b93d75b24f` |
| C, measurement, control, built on `DESKTOP-BCT68L9`; superseded on 2026-11-03, see below | `experiment/startup-5d-control-measurement` | `5b09932` | `5b5333f7480c5899068f913dd53b1b0dbd29ff4631198bd4e729ad60e2e220cc` |
| C, measurement, 5d, built on `DESKTOP-BCT68L9`; superseded on 2026-11-03, see below | `experiment/startup-5d-treatment-measurement` | `888489a` | `b7000916196cb85e8f259a66a5f016c5f6b5d11c630f484c67b3a2bde834b5e1` |

**Since 2026-11-03 14:39 the four builds of section C are these.** Each is its branch's tip merged
with `main-v5` at `ed41de1` (#94, *The window decided before the name is resolved*). Each is built
on `nbpromec1` under the same toolchain; the details are in *The fix on the four branches,
rebuilt*, at the end of *Recorded results*.

| Step | Branch | Commit | Replaces | APK (dev, arm64) sha256, `nbpromec1` |
|---|---|---|---|---|
| C, measurement, control | `experiment/startup-5d-control-measurement` | `dea38cc` | `5b09932` | `d005e9014367be922a4230a7a842438e723cf5b65373cab23de8ea0609824402` |
| C, measurement, 5d | `experiment/startup-5d-treatment-measurement` | `a2f12f1` | `888489a` | `ece097acf6a53230af9a000cc9ab6a81bfc942683b35710f06a4a7876dffd00d` |
| C, qualification, control | `experiment/startup-5d-control` | `2e72e42` | `cc4c0cf` | `d9970f278909e125ae15540a13cac1389e467fa3beaeb3ba5d2ff447c5c19ac8` |
| C, qualification, 5d | `experiment/startup-5d-treatment` | `550b414` | `1198c79` | `e881d0abaf9e13a2d5c62c3e20aa77c66b188766cfa364cdde6144c70182ac23` |

**Every build of `5b09932`, `888489a`, `cc4c0cf` and `1198c79` is SUPERSEDED since 2026-11-03
14:39,** with its sha256, whichever machine built it.
- This covers the `DESKTOP-BCT68L9` rows of the first table and the `nbpromec1` rows of the table
  below.
- **The reason, in the owner's words:** "the catalogue parse was removed from the launch path". That
  holds for a spawn outside the active window. A spawn inside the window is still resolved.
- The records stay. The four commits stay reachable on their branches, as first parents of the
  merges.

**From 2026-11-03 08:22 to 14:39 the four builds of section C were these**, rebuilt from the same
commits on `nbpromec1` under *one toolchain, not one machine* (below), because the sitting runs from
that machine.
Each supersedes the row above with the same commit. No counted launch ran on those four, in the owner's
account of 2026-11-03. The phone holds `5cc84bb3…`, installed at 2026-11-02 21:30:50. Whether any of
the other three was installed before it cannot be read from the phone, which keeps only its last
install (*Recorded results*, 2026-11-03).

| Step | Commit | APK (dev, arm64) sha256, `nbpromec1` | Supersedes, `DESKTOP-BCT68L9` |
|---|---|---|---|
| SUPERSEDED: C, measurement, control | `5b09932` | `6fb676a375db14d09a474e8dff54cb650f1a27afa490c25419f34178986c9e6c` | `5b5333f7…` |
| SUPERSEDED: C, measurement, 5d | `888489a` | `8f731306f38ae35db345f0fa1a4ffcd8e99ed19bddcb6022451f34ae0747c58e` | `b7000916…` |
| SUPERSEDED: C, qualification, control | `cc4c0cf` | `8b57a63488048ff3824f69304668cbd6b78cc3e6661ae0a84e46cc694d883646` | `5cc84bb3…` |
| SUPERSEDED: C, qualification, 5d | `1198c79` | `ecc3e2a43494ae8e7732acbec5769acb75c9aead602f09ca4a2eb86bd1d9feb2` | `2e298b5c…` |

**From 2026-11-02 21:19 to 2026-11-03 08:22 the four builds were the `DESKTOP-BCT68L9` rows above**,
built by the owner's decisions after the first qualification (*Decided on 2026-11-02, after the
qualification*, below).

- **The qualification builds** are `03a8775` and `0fcd674` plus three commits:
  - the injection fix, `253373a` and `500248e`;
  - the snapshots inside a wait, `2724b12` and `4d0ca9f`;
  - the row after a wait, `cc4c0cf` and `1198c79`.
- **The measurement builds** are `301111b` and `5851fde` plus the same two reads: `5e4750d` and
  `ecf6ea7`, then `5b09932` and `888489a`.

So a qualification build is again its measurement build plus the injected delays: the qualification
qualifies the instrument being measured. Verified on the commits:
- the probe is the same file on the two measurement builds, and the same file on the two
  qualification builds;
- the probe's diff from measurement build to qualification build is the same before the reads and
  after them (hash `134ed035`), so the reads add nothing between the two.

"The measurement builds do not change", below, held until then. It no longer does: they were rebuilt
and re-hashed. The builds they replace were never measured:
- `03a8775` (`a3c3ca07…`) and `0fcd674` (`163fa113…`), whose qualification is recorded under *Recorded
  results*;
- `301111b` (`6fc184a8…`) and `5851fde` (`0b232041…`), which no launch of the round used.

The measurement branches are never merged. `301111b` is `c8a9f99` plus the probe; `5851fde` is
`fc63adb` plus the same probe. `cee6695` is `301111b` plus the injected delays (`ccd2c46`) and the
200 ms decision below; `a2e72ff` is `5851fde` plus the same two commits, the second cherry-picked, and
the two pairs' diffs are identical. Each embeds only its own commit (read from its dex strings). After
the round, the phone is left on the final 5d build, read back and recorded in `RELEASE-STATE.md`.

Since 2026-11-02 13:45 the qualification builds are `03a8775` and `0fcd674`: `cee6695` and `a2e72ff`
plus the stall's new duration (`a2b413a`, cherry-picked as `bcd8ecf`) and the scripts' changes of the
same day (`03a8775`, cherry-picked as `0fcd674`), *Qualification* below; the two pairs' diffs are
identical. `cee6695` and `a2e72ff`, sha256 `94d68eb5…` and `cb7fb26b…`, were built this morning and
never installed: the stall's duration superseded them before any qualification launch. The
measurement builds do not change: the stall is not in them, and every launch of the section, theirs
included, is run and read with the host scripts of the branches' tips, `e2e8e39` and `ee6b027` since
2026-11-02 14:14. Those two commits change nothing outside `experiment/` after `03a8775` and
`0fcd674`, so the qualification APKs stay those two builds.

`d11b629` is `fc63adb` with the harness's instrument fixes of 2026-11-02 (*H1 on `fc63adb`,
diagnosed*, below) and the round's documents. `git diff fc63adb d11b629 -- app/src/main gradle
build.gradle.kts settings.gradle.kts gradle.properties app/build.gradle.kts` prints nothing: 5d's
production code is `fc63adb`'s, so the qualification and measurement builds, made from `fc63adb`,
still measure it. The mutants are made from `d11b629`, so that each one runs the fixed harness.

### Every APK of the round comes from one machine

The comparison that decides 5d is control against treatment, and the Java Development Kit (JDK) is an
input no commit records: the gap #83's KDoc documents beside `gitShaForBuild`. Built on two machines,
the two sides could differ by JDK with nothing to show it. So every APK in the table, the mutants of
H2 included, is built on one machine, which holds the JDK constant within the round. If the round is
split across sittings, the measurement section, the qualification and the twenty launches after it,
is one sitting on one machine. A harness stage may fall in a different sitting from the next one, but
no single stage may be split.

**Refined on 2026-11-03, by the owner of the measurement: one toolchain, not one machine.** The rule
above says one machine. Its reason is the JDK, which no commit records, so the machine was a proxy for
the toolchain it carries. The rule is now this. Every APK of the round, and every launch of the
measurement section, comes from one toolchain: the same JDK, read as the binary the Gradle daemon
runs, and the same Android Gradle Plugin (AGP) and Gradle. A second machine meets the rule when all
three read the same there, read on that machine and not assumed from its name or from a note. Its
signing certificate and its Firebase configuration must also match the phone's, or an install cannot
replace the dev app in place.

Do not restore the literal form without this reason. Read literally, the rule forbids a rebuild that
its reason allows, and it protects nothing that the reason does not. What stays binding, in the
owner's words:

> The four builds and all twenty decision launches come from ONE toolchain, and the ten decision
> launches are contiguous and alternating in ONE sitting with ONE phone state.

**The machine, read on 2026-11-02:** `DESKTOP-BCT68L9`. Each APK built from the worktree
`C:\Users\Fede\StudioProjects\TwitchMiniChat-wt-5d`, from a tree where `git status --porcelain`
printed nothing before and after the build, with `:app:assembleDevDebug`, in one tree, in this
order: `fc63adb` first, every one of its 42 tasks executed, then `cee6695`, `a2e72ff`, `301111b` and
`5851fde`, each incrementally on the one before, as Gradle reported. `d11b629` followed on
2026-11-02 at 11:50, in the same worktree, incrementally (10 of its 42 tasks executed), after
`gradlew --version` there printed the same Gradle, launcher and daemon lines again: sha256
`41a9da8ba035951296d79edd039e41d3f46399eabc84e66679d17944b2cc490e`, embedding `d11b629` with no dirty
marker, signed by `9b3a523b…5c51`. `03a8775` and `0fcd674` followed at 13:43 and 13:44, the same way,
after `gradlew --version` printed the same three lines once more: incrementally, 17 and 14 of their 42
tasks executed, each embedding its own commit and no other, signed by `9b3a523b…5c51`. The APKs and
each build's log
are kept outside any clone, in `C:\Users\Fede\StudioProjects\tmc-5d-round\apks`, so that no later
build overwrites one. AGP 9.2.1 and Gradle 9.4.1, from the tree. `gradlew --version` there:
`Launcher JVM: 21.0.10 (JetBrains s.r.o. 21.0.10+1-b1163.110)`, `Daemon JVM: Compatible with Java
21, JetBrains, nativeImageCapable=false (from gradle/gradle-daemon-jvm.properties)`. That file is
git-ignored and holds `toolchainVendor=JETBRAINS`, `toolchainVersion=21` and foojay URLs. The daemon
runs
`C:\Users\Fede\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2\bin\java.exe`, which reports
`OpenJDK Runtime Environment JBR-21.0.10+1-1163.110-jcef`. Android SDK `D:\Android\SDK`. Both
flavours' `google-services.json` are the clone's own, not the CI placeholder: the Firebase project of
the dev build already on the phone (`d56812b`), checked by its project id and number in that APK's
resources. Signed with this machine's debug key, certificate SHA-256 `9b3a523b…5c51`, the signer of
the dev app the phone already held, so each install replaces it in place, with its accounts.

The hashes this table carried before 2026-11-02 were built in another clone, for `fc63adb`, the 50 ms
qualification commits `ccd2c46` and `878f7de`, `301111b` and `5851fde`. None is used. None was
installed on the phone either: its dev app was last updated on 2026-10-23 09:23, before any of
those commits existed.

**The second machine, read on 2026-11-03 from nothing, under the refinement above:** `nbpromec1`.
Every item was read there, and none was taken from this section. The JDK, AGP, Gradle, signing and
Firebase reads came before the first build; the scripts' checks ran while it built.

- **The clone and its worktree.** The clone's `git status --porcelain --untracked-files=all` printed
  nothing. The builds ran in a worktree created for them, detached, beside the clone:
  `%USERPROFILE%\Documents\doc_vari\AndroidProjects\TwitchMiniChat-wt-5d`. Its three git-ignored
  inputs were placed by hand, each read back:
  - `local.properties`, holding only the clone's `sdk.dir` line. The four release-signing keys are
    read by the build only for a release, so they were not copied;
  - `gradle/gradle-daemon-jvm.properties`, byte-identical to the clone's;
  - `app/src/dev/google-services.json`, byte-identical to the clone's.
- **AGP 9.2.1 and Gradle 9.4.1**, from the tree, as on `DESKTOP-BCT68L9`.
- **The JDK matches.**
  - `gradlew --version` printed the same `Launcher JVM` and `Daemon JVM` lines as above, character
    for character.
  - `gradle/gradle-daemon-jvm.properties` exists, git-ignored, with `toolchainVendor=JETBRAINS`,
    `toolchainVersion=21` and foojay URLs.
  - All four builds ran in one Gradle daemon. After them it was the only Gradle daemon running,
    started at 07:55, a minute after Android Studio and before any command of this sitting; that the
    builds ran in it is read from those two facts, since no build log names its daemon. It runs
    `%USERPROFILE%\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2\bin\java.exe`, which reports
    `OpenJDK Runtime Environment JBR-21.0.10+1-1163.110-jcef (build 21.0.10+1-b1163.110)`; its
    `release` file gives `JAVA_RUNTIME_VERSION="21.0.10+1-b1163.110"`. The Kotlin daemon runs the same
    binary.
  - The build declares no `jvmToolchain`, so no second JDK compiles anything.
- **The signing certificate matches.** The build signs debug with `~/.android/debug.keystore`. That
  keystore holds one entry, `androiddebugkey`, with certificate SHA-256 `9b3a523b…5c51`. It was
  compared in full with the signer of the dev app on the phone, read from the APK pulled off it, and
  is the same. So an install from `nbpromec1` replaces the dev app in place, with its accounts.
- **The Firebase configuration matches.** The dev flavour's `google-services.json` has the same
  project id, project number and dev application id as the resources of the dev APK on the phone.
- **Not matched, because no record exists for `DESKTOP-BCT68L9`:** the Android SDK's revisions. On
  `nbpromec1` they are android-36 revision 2 and build-tools 35.0.0, 36.0.0 and 36.1.0, in
  `%LOCALAPPDATA%\Android\Sdk`.
- **The host scripts' tools.** `PYTHON=py`, Python 3.10.0; `DESKTOP-BCT68L9` used 3.14.0, and a bare
  `python` on `nbpromec1` is 3.7.0. Every script of `92fbb4a` parses under 3.10, and
  `launchcheck_test.py` (4 cases) and `qualify5d_test.py` (5 cases) pass under it.

  *Withdrawn on 2026-11-03:* "5 cases" was wrong. It counted the lines left after the output had
  been cut to its last six, not the suite. Re-run in full, both suites pass with the same counts on
  both of `nbpromec1`'s Pythons:

  | Suite (blob at `92fbb4a` and `1ae4b42`) | Python 3.10.0 | Python 3.7.0 | `DESKTOP-BCT68L9`, Python 3.14.0, as the owner reports it |
  |---|---|---|---|
  | `launchcheck_test.py` (`c32aabf`) | 4 of 4 | 4 of 4 | 4 of 4 |
  | `qualify5d_test.py` (`fff69a6`) | 22 of 22 | 22 of 22 | 22 of 22 |

  Each suite is straight-line code that prints one `ok` per case and stops at the first failed
  assertion. It has no skip and no version check, so a printed count can only equal the declared
  count or stop short of it. `qualify5d_test.py` declares 22: 11 `check(...)` calls and 11 direct
  prints.

  **The Python version is part of the instrument,** since `qualify5d.py`, `launchcheck.py` and
  `decide.py` compute the verdicts. It is recorded beside the JDK, AGP and Gradle (*Order of the
  launches, and what each records*). The evidence that the difference between 3.14.0 and 3.10.0 does
  not reach the verdict is the same suites passing with the same counts on both. Had a count
  differed, the sitting would have waited.

  Also git 2.45.1.windows.1 and adb 37.0.1. `run.sh`'s install read-back, `adb shell sha256sum` on the
  `/data/app/~~…==/base.apk` path, reached the phone unconverted from this Git Bash, with and without
  `MSYS_NO_PATHCONV=1`.
- **The builds,** each `:app:assembleDevDebug` from a detached checkout of its commit, with `git
  status --porcelain --untracked-files=all` empty before and after:
  - in order, `5b09932` (all 42 of 42 tasks executed), then `888489a`, `cc4c0cf` and `1198c79`
    incrementally (14 of 42 executed each, 28 up-to-date);
  - each embeds its own commit and none of the other three, with no dirty marker, read from its dex
    strings;
  - each is signed by `9b3a523b…5c51`, package `com.fs.twitchminichat.dev`, versionCode 8,
    versionName `5.5.1-dev`;
  - the APKs and each build's log are kept outside any clone, in `%USERPROFILE%\tmc-5d-round\apks`.
- **What the two toolchains produced for the one commit both builds of are on `nbpromec1`,
  `cc4c0cf`.** The APK hashes differ: `5cc84bb3…` was pulled off the phone, where `DESKTOP-BCT68L9`
  installed it, and `8b57a634…` was built here.
  - The 13 dex files are the same 13 files, byte for byte, numbered differently.
    `DESKTOP-BCT68L9`'s `classes13.dex` is `nbpromec1`'s `classes8.dex`, and its `classes8` to
    `classes12` are `nbpromec1`'s `classes9` to `classes13`.
  - Every other entry has the same name, CRC and size; only the zip's entry order differs.
  - So the code the phone runs is the same from either machine. The difference lies in the
    numbering of the dex files and the order of the zip.
  - Why the numbering differs was not examined. Each machine had incrementally built a different
    sequence of commits in its tree before this one.
  - This was not checked for the other three commits, because no build of theirs from
    `DESKTOP-BCT68L9` is on `nbpromec1`.
  - **So one commit is COMPARED and three are INFERRED, not four verified.** For `cc4c0cf` the
    sameness of the code was compared file by file. For `5b09932`, `888489a` and `1198c79` it is
    inferred from the same verified toolchain.

## H. The harness

Run as in 5c: both themes, the APK's hash read back from the phone, 0 input events, the results file
read back. Since 5d every production row sits in a RecyclerView laid out like `chatList`, and the
frozen ones in the LinearLayout `chatContainer` was; see the harness activity's KDoc.

### H1 — The 5d tree

**Proves it,** in each theme, exactly:

- **parity 34 / 34.** `msg.hidden` compares the absence of a view with one `GONE` view: in 5d a
  hidden row is no view at all. The row before its user is hidden, 1 / 1 (`rowsBeforeHiding`).
- **controls:** the three behave as in 5c.
- **the emote layer 2 / 2, with a 404 2 / 2,** and its controls behaved.
- **pairs 658 / 658,** sessions held 658, images held 658, not vacuous. Every view is now created and
  bound through the adapter (`AdapterBinding`), as a holder back from the pool is.
- **C-R 34 / 34,** with no declared exception. Its one exception in 5c was the hidden row's `GONE`,
  set by the sync on a view the list no longer has.
- **direct bind:** each of the 9 echoes bound at once at its last status against the same echo built
  through its history: pixels 9 / 9, state 8 / 9, and the 1 declared exception met exactly
  (`echo.unconfirmed-confirmed`, the hidden status line's text).
- **preconditions 5 / 5:** two from 5a-2, unbind's from 5c, the adapter's payload and the binder's
  view type from 5d. 5a-2's "an echo is bound as sending" is gone: it was reachable from the pool.
- **the lifecycle proof 16 / 16.** L7 and L8 now find the release at the list's next layout pass,
  not at the call; L10 finds the payload bind there.
- **the recycler proof 8 / 8:** R1 to R8 (`RecyclerProof`).
- **the scroll proof 3 / 3:** S1 to S3 (`ScrollProof`).
- **list checks:** every check passed, and there was at least one.

**Failing looks like.** Any other count. A different count stops the round until it is understood.

#### H1 on `fc63adb`, diagnosed (2026-11-02)

Each mismatch was diagnosed to a cause, with its evidence, before it was classified: (a) a defect in
5d's code, (b) a defect in the instrument, (c) a wrong prediction about correct behaviour. All four
are (b). None is (a): no line of `app/src/main` changed to fix any of them. The sources cited are
RecyclerView 1.3.1's, which `devDebugRuntimeClasspath` resolves, and line numbers in the harness are
`fc63adb`'s.

- **R2, `noPoolRecycle` alone (scrolled 201 px).** R2's other five checks passed, the holder in the
  cache among them, and R3 passed all of its own, the same holder with no new load among them.
  201 px is 29 (the list's top padding) + 52 + 52 (rows 0 and 1) + 68 (the emote row, row 2), so
  rows 0 and 1 leave the view before the emote row does. The cache holds 2 (`DEFAULT_CACHE_SIZE`,
  RecyclerView.java:6528); `scrollBy` never starts the prefetcher that would enlarge it (:1940-1954;
  only :2020, :3874 and :5915 post to it); and a full cache sends its oldest holder to the pool,
  through the RecyclerListener the proof counts, before it takes the new one (:7103-7105,
  :7049-7058, :7176-7177). The clause asserted that no holder at all reached the pool, where its
  KDoc asked for "no pool recycle of it". **Cause:** the clause demanded what RecyclerView cannot do
  with the emote row at position 2. **(b).** `d11b629` scopes it to the emote holder and asserts the
  forced eviction exactly: the positions recycled during R2 must be [0]. Recorded, row 0's eviction
  becomes an observation rather than an inference.

  **What R2's failure did not do:** it did not refuse to let R3 claim the cache path. Its failing
  clause was a false alarm about another row, and the guards that protect R3 (`inTheCache`, in R2
  and as R3's precondition, and `noNewLoad`) all passed. Nor could it discriminate: it failed on
  every build, so M1's predicted R2 failure proved nothing. H1 establishes that the cache path is
  **reached** on the clean build. Whether the defect deduced on 2026-10-07 is **observed** is H2's
  question, and M1 must fail R3.
- **Direct bind, state 0 / 9, declared exceptions met 0.** Both sides are production's: one echo
  created at its last status, and the same echo built through its history by the same adapter. The
  direct bind rendered its view (RebindProof.kt:353) and then snapshotted it (:354); the fresh builds
  were snapshotted (:97) before anything rendered them. The pairs check snapshots before rendering on
  both sides, and passed 658 / 658. The parity pass renders before it snapshots, and it lists a
  rendered echo's message view with four spans: `DynamicLayout$ChangeWatcher`,
  `TextView$ChangeWatcher`, a `ForegroundColorSpan` and `EmojiTransformationMethod`. The first is
  attached by a TextView's layout to Spannable text, and draws nothing. **The decisive control:**
  `echo.sending` differed by one span too, and its last status is its first, so both sides made the
  identical call with identical data. That rules out the data and the bind, and is why this
  diagnosis is not a story. **Cause:** a view snapshotted after its layout, compared with one
  snapshotted before. **(b).** In `d11b629` both sides are snapshotted after their layout, and the
  spans each side's first layout added are recorded by class, so that the watcher is observed, not
  inferred. Pixels 9 / 9 with state 0 / 9: only the non-graphical comparison could see it.
- **R1, R5 and R7, `pixelsEqualTheReference` alone.** Each delivered the held images and drew at
  once: the list's row at its bounds as they stood, against a reference that render had laid out
  (RecyclerProof.kt:296-299). Each delivery requests a layout (TwitchEmoteImageLoader.kt:390), which
  runs at the list's next layout. The emote row is 52 px without its images (`msg.emotes` in the
  parity pass, which has no loader: 110 px less 58 of container padding) and 68 px with them (the
  emote layer's, 126 less 58). Controls: R3 compares the same holder with the same reference after
  a scroll has laid it out, and passed; the lifecycle proof renders both sides before it compares,
  and passed 16 / 16. **Cause:** the row was drawn before the layout its own delivery had asked for.
  **(b).** In `d11b629` the list is laid out after each delivery, as the next frame would lay it
  out, and the row's pending layout, its heights and both bitmaps' sizes are recorded. **Not the
  direct bind's root cause:** another mechanism at another line, and fixing either leaves the other
  failing. But the same family: a view observed before a layout its own content asked for.
- **S1, `theLayoutReportedAStillScrollWhileTheNewestRowHadNoView` alone (`stillReports` 1).** The
  layout reported once with no movement, and at that report the newest row had a view. When the row
  scrolled to has no view, LinearLayoutManager lays it out from the end, its bottom at the bottom
  edge (LinearLayoutManager.java:960-967), then fills past that edge by `getEndPadding()` (:613-614,
  :669-672), the list's bottom padding, 29 px (OrientationHelper.java:431-432). S1 appended one row
  after the one it scrolled to, and a single row always gets a view in that fill. **Cause:** S1
  never built its race. S1 was wrong, not the rule. **(b).** `d11b629` appends two rows after the one
  scrolled to; the second begins 52 px past the edge, below the fill.

  **The still-scroll check earned its keep.** S1's other checks passed without the race existing.
  Without that check, S1 would have gone green while testing nothing: a vacuous pass inside a test
  written to catch a race. Sharper still, M7 would have passed S1 as well. With one pending row the
  newest ended 52 px below the edge, within `bottomThresholdPx` (72 dp, 171 px), so "near the
  bottom" answered yes even when asked (reasoned from the code; the run recorded no gap). **The
  zero-movement rule is not yet shown right.** It is not shown wrong either: nothing has exercised
  it. Until S1 is re-run with its race, the start-up race's fix is untested code, and nothing this
  work established includes it.

**Two of the four failures, arguably three, are one property:** a snapshot or a draw taken before a
pending layout is not comparable with one taken after it. The pairs check got this right by the
order of its calls; the direct bind and the recycler proof's draw got it wrong independently.
`d11b629` makes the order structural, not a discipline. `Settled`, which every snapshot, every draw
of a row at its bounds and every render calls, fails at once, naming the view, unless the view is
laid out with no layout pending in it or in any shown descendant. Of the two forms, it is the
assertion, not helpers that lay the view out themselves, for two reasons:

- A list's rows are laid out by their list, at the list's height, which only the proof knows. A
  helper laying a row out alone would put it in a state production never reaches.
- A helper that laid every view out would hide a row that never asks for the layout its content
  needs, such as a `requestLayout` missing after an image arrives. That is production behaviour
  under test, and the assertion leaves it to show in pixels.

The one state it can require is "laid out", because the parity pass and every list row can be read
only after a layout. So the direct bind's references moved to after their layout, not the direct
bind to before its own, and the pairs check moved with them. GONE views are not held to it: no
layout lays one out.

**Corrections, both recorded.** Two of the owner's premises were retracted on 2026-11-02 after the
diagnosis: that R2's assertion had done its job (its failing clause was a false alarm about another
row), and that the direct bind's reference was assembled from frozen parts (both its sides are
production's). The amendment of the same morning, written by the agent, said criterion 2 does not
read `stall`; that was its inference, not the plan's text (*Qualification*, below).

#### From `d11b629` on: what changes, pre-registered on 2026-11-02 before its first run

Every count of *Proves it* above stands, unchanged. The instrument changed, so the changes it makes
are written here beside the counts, and the old ones stay readable above. Two assertions are new:

- **new, `Settled` refuses nothing:** no snapshot, draw or render of an unsettled view, so both
  themes reach their done line, no pass aborts and no step records an error;
- **new, R2's `onlyTheForcedEvictions` passes,** with `positionsSentToThePool` [0]: rows 0, 1 and 2
  leave a two-slot cache in that order, and row 2's entry evicts row 0. On every mutant of H2 as
  well: none of them touches the cache or the order rows leave it. R2's scoped `noPoolRecycleOfIt`
  passes, and `scrolledPx` is 201, as on `fc63adb`.

Recorded, deciding nothing, each with the value the diagnosis predicts:

- **R1, R5 and R7:** `rowLayoutPendingAfterDelivery` true, the cause seen directly. For R1 and R5,
  `rowHeightBeforeItsLayoutPx` 52 and `rowHeightAfterItsLayoutPx` 68, and `rowBitmapPx` equal to
  `referenceBitmapPx`, 1022 × 68 (1080 less 29 px of padding on each side). R3, `rowBitmapPx` and
  `referenceBitmapPx` 1022 × 68. For R7, the height after the layout greater than before, and its
  two bitmaps the same size; no value is predicted, since the echo's heights were never recorded.
- **direct bind:** `layoutsAddedOnlyTheWatcher` 9. On each side of every echo, the first layout
  added exactly one span, `android.text.DynamicLayout$ChangeWatcher`, to the message view.
- **S1:** `stillReports` 1, at which the newest row had no view (`newestRowGapAtEachStillReportPx`
  ["none"]). Every S1 check passes.

Changed by the instrument, and the counts they reach unchanged:

- **pairs:** each view is laid out before each snapshot: A before B is bound, as a holder from the
  pool had been on screen, and the rebound view after. 658 / 658, `sessionsHeld` 658, `imagesHeld`
  658, not vacuous.
- **list checks:** 124 in each theme, 0 failures: `fc63adb`'s 121, and the three layouts the recycler
  proof now makes after a delivery.

#### A row inserted above the viewport: established in the harness, not reasoned (asked 2026-11-02)

It was reasoned from LinearLayoutManager's anchoring that in 5d a row inserted above the viewport
leaves the visible rows where they are, where the ScrollView pushed them down; S3 was written to
see it. If it holds, 5d delivers scroll anchoring's benefit for free, and step 8 may leave the
release's path. S3 passed on `fc63adb` and on `d11b629`, in both themes, with the first visible
row's top at −35 px before and after. That pass does not establish it yet, because S3 never checks
where the row it inserts landed. A row landing below the viewport, or at the end, would also leave
the first visible row in place, and `twoUnseenRows` holds either way: S1's vacuous pass, in another
form.

So the harness gains the check that settles it, `oneRowWentAboveIt` (`b954eba`, which changes only
`ScrollProof.kt` after `8535b3c`): the first visible row's adapter
position rises by exactly one, which only a row inserted above it can cause. Its positions before and
after are recorded. **Pre-registered on 2026-11-02, before its first run:** it passes in both
themes, the positions recorded as p and p + 1, and every count of H1 above is unchanged. Reported as
its own result. What it establishes is 5d's behaviour, for one row, scrolled up. The ScrollView's
behaviour before 5d stays reasoned, since no harness runs it. Several rows at once, and anchoring as
a design, are not part of it.

### H2 — Predictions, then mutants

**Do.** After H1, and only then, because the step list is final only once H1 has run, write
`predictions.json`: each mutant's exact edit, and the steps and rows it must fail, in both themes.
Hash it and timestamp it before the first mutant is built; never edit it after. Then build, install
and run each mutant alone, both themes, and for M4 the unit tests too.

The mutants, as reasoned before H1. The predictions file restates and finalises them.

| Mutant | Edit | Predicted to fail |
|---|---|---|
| M1 | 5c's release on detach restored | R2, R3 (the defect itself, seen on its own path for the first time), R4 (precondition), R8, L2, L3 and L4 for each emote row |
| M2 | `onViewRecycled` does not unbind | R4, R7, L7, L8 |
| M3 | a status change notified without its payload | L10, R6, parity and C-R on `echo.unconfirmed-confirmed` |
| M4 | system lines given the message view type | parity on `system.line`; `ChatRowViewTypeTest`, 2 of its 3 tests |
| M5 | `releaseAll` removed from `onDestroyView` | R8 |
| M6 | `isNearBottom` always true | S2, S3 |
| M7 | the scroll listener's rule for reports with no movement removed | S1 |

M7 was added after Phase 1: the rule it removes was decided while writing the code, and S1 exists to
test it. M4's C-R prediction from Phase 1 is withdrawn: both of C-R's builds go through the same
view type, so it cannot see M4.

**Restated on 2026-11-02, against the fixed harness, before any mutant was built.** The table above
stays as written, and `predictions.json` finalises it.

- **M1 and R2.** Until `d11b629`, R2 failed on every build, so M1's predicted R2 failure could prove
  nothing: a check that fails unconditionally cannot discriminate. Against the fixed R2, what M1
  does there depends on its exact edit, and the table's "5c's release on detach restored" decides
  it. The release 5c removed was posted, not made in the detach: `onViewDetachedFromWindow` paused
  the row and posted `release()` to the next turn of the main loop, to run if the row was still
  detached then (`aaf718f^`, TwitchEmoteImageLoader.kt:464-481). R2 detaches the holder and checks
  it within one turn, and `ProofSteps` runs R3 on a later turn. So, with that edit, **R2 passes under
  M1, `loadsLive` included**: its loads are still live and no release is reported when R2 reads
  them. The release runs before R3, and R3 fails: the same holder comes back from the cache without
  a bind, and without its images. The owner expected `loadsLive` to fail at R2. That holds only for
  a release made inside the detach, which is not 5c's edit. Which edit M1 is, is the owner's to fix
  in `predictions.json`, before it is hashed. **Decided by the owner on 2026-11-02:** M1 is the
  posted edit, the one that reproduces the behaviour before 5c. The owner's expectation that
  `loadsLive` would fail at R2 was made without checking that the release was posted, and it is
  retracted. So of R2 and R3, M1 fails R3, the step the defect is about, and not R2. The table's
  other entries for M1 (R4's precondition, R8, L2 to L4) are to be checked against the posted edit
  in the same way when `predictions.json` is written, and the edit's identity goes into it before it
  is hashed.
- **M7 and S1.** With S1's race built, M7 fails S1 at `stillStuck`. The report with no movement
  reaches the page's listener while the newest row has no view, `isNearBottom` answers no, and the
  page stops following until the posted `scrollToBottom` runs. S1's later checks run after that and
  pass.

**A mutant that moves a release in time: its kill set is derived, never estimated** (written on
2026-11-02, before `predictions.json`). A mutant that changes when a release happens does not only
break the step that observes the release. It falsifies the preconditions of every later step that
assumes the state before the release. Twice now an estimate from the step the mutant is about came
short. O2 in #89, the same posted release on 5c, failed L2, L3 and L4 for each emote row: six steps,
where Phase 1 had estimated four (the estimate as the owner records it), L4 by its precondition. And
M1 here fails R4 by its precondition, which the restatement above first missed. So for every mutant
of this family, the kill set is derived by walking every later step of every proof, in order, with
its preconditions, against the state the mutant leaves, and the walk is done before
`predictions.json` is hashed. Two properties of the harness the walk depends on:

- **A false precondition does not stop its own phase.** `ProofSteps` and the lifecycle proof record
  it, run the rest of that phase, and skip only the step's later phases. R4 still scrolls when its
  precondition fails, and L4 still unbinds.
- **A step's state is what the earlier steps of its own proof left.** Each proof builds its own page
  and list, so a mutant's effect reaches every later step of the same proof and no step of another.

The family is M1, M2, M3 and M5. M4, M6 and M7 move no release. Each walk below is reasoned from the
code on 2026-11-02, not run.

- **M1, the posted release.** Its exact edit decides its kill set, so it is written here for
  `predictions.json`: in `LoadSession.onViewDetachedFromWindow`, after the pause,
  `textView.post { if (!isReleased && !textView.isAttachedToWindow) sessions.release(textView) }`.
  It makes two choices, the owner's to confirm:
  - **Through the registry, not by calling `release()` directly.** 5c's code called the session's
    `release()`. In 5d that leaves the session registered, so the next path that ends it (an unbind
    at recycle, a bind, `releaseAll`) releases it a second time, and `LoadSession.release`'s
    precondition, "released twice", throws. In the recycler proof that throw would come from inside
    `scrollBy`, in R4, leaving RecyclerView in the middle of a recycle, and nothing after it could be
    derived. `EmoteSessions.release` removes a session before it releases it, so the release happens
    once, as it did before 5c.
  - **Without 5c's page condition.** 5c's post released only while `chatPageView` was attached.
    5d's loader holds no page view, and the harness's fragment has none, held at CREATED, so a
    condition on the fragment's view would never be met there and M1 would release nothing. No proof
    detaches a page while its rows stay attached, so dropping it changes nothing they can see.

  Walked with that edit, M1 fails:
  - **R3:** the same holder comes back from the cache with no bind and no new load, and without its
    images. It fails `oneImagePerEmote`, `running`, `pixelsEqualTheReference` and
    `releaseNotReported`. `imagesAreTheDeliveredOnes` passes vacuously, with no image shown, and
    `oneImagePerEmote` in the same step is what keeps the step from passing.
  - **R4,** by its precondition `loadsLive`: R1's loads were released before it ran.
  - **L2, L3 and L4 for each emote row,** L4 by its precondition, as O2 did on 5c.

  It does not fail:
  - R2, which detaches the holder and checks it within one turn, before the post runs;
  - R5 to R7, each of which binds its rows afresh;
  - R8, which detaches the list and checks within one turn. `releaseAll` then ends every session, so
    each post finds its session released.

  The table's R2 and R8 for M1 are withdrawn by this walk.

  **M1 is an instrument, not a restoration.** Both choices were approved by the owner on
  2026-11-02, for the reason they share. M1's job is to produce, at R3's observation point, the
  effect the predicted defect would have, so that R3's failure proves R3 can see it. Fidelity to
  5c's text is not the goal, and in two places it is incompatible with that job:
  - called directly, the release throws inside R4's `scrollBy`, and nothing after it can be derived;
  - with the page condition, M1 is inert in the harness and kills nothing, which would be misread
    as an insensitive instrument.

  Two things are declared with it, here, in the pull request and in the mutant's own comment, as
  #81's frozen-defect discipline asks of anything deliberately not what it looks like:
  - **M1 deviates from 5c's text in exactly those two places,** each for its reason. "M1 restores
    5c's release" must not be read as "M1 is 5c's code".
  - **M1 failing R3 proves that the instrument is sensitive.** It does not prove that the code
    before 5c, on a real RecyclerView, would have shown the defect. That claim stays deduced from
    the source, as it has been since 2026-10-07: the distinction between reaching a path and
    observing a defect, one level deeper.

  Also for the pull request: under M1, R3's `imagesAreTheDeliveredOnes` passes vacuously, with no
  image shown, and `oneImagePerEmote` is what keeps R3 failing. The kill is credited to that
  assertion.
- **M2, `onViewRecycled` does not unbind:** R4 (`everyLoadCleared`, `releaseReportedOnce`), R7
  (`itsLoadsCleared`), L7 and L8, as the table has it. Every later precondition holds: R5's loads
  come from its own bind, R6's and R8's likewise, and a session left behind ends at its view's next
  bind or at `releaseAll`. The rebind proof's pairs never reach `onViewRecycled`:
  `AdapterBinding.bindAgain` binds without it.
- **M3, a status change notified without its payload:** the full bind ends the echo's session and
  starts another. So L10 and R6 fail (`noLoadCleared`, `imagesStillInstalled`, `running`), with
  parity and C-R on `echo.unconfirmed-confirmed`, as the table has it. R7 passes. Its precondition,
  the echo confirmed, holds, and the loads R6's full bind requested are cleared at its recycle, which
  is what R7 checks.
- **M5, `releaseAll` removed from `onDestroyView`:** R8 alone, at its last check. No step follows it
  in the recycler proof, and the scroll proof's rows hold no emotes.

### H3 — The clean final build, once more

Reinstall the clean final APK and run both themes again: the same counts as H1.

## M. Manual checks on the final build

5c's M1 to M6 again, and 5d's own:

- **Far up and back.** Scroll far up in a busy chat and back down: emotes come back in rows the list
  rebinds from the pool, on Glide's real timing.
- **Scrolled up while messages arrive.** The rows on screen stay where they are, the jump button
  counts the new rows, and tapping it returns to the bottom and clears the count.
- **A tap on empty space clears the channel field's focus.** With the channel field focused, tap
  between rows, on a system line, and below the last row of a short chat. Below the last row is new:
  the old container ended at its last row.
- **Swipe-to-reply inside the list,** on a message with an id.
- **An echo sent and confirmed,** and the same echo scrolled far away and back.
- **Hide a user, then show them again** from the blocked-users screen.
- **Does the history still drip in? A scope decision, not a check** (added 2026-11-02). During the
  round, with the 5d build on the phone, not after the pull request merges: Federico opens the app
  on a cached channel and says whether the history still appears to drip in rather than arrive.
  Nothing passes or fails here. 5d does not fix "messages enter one at a time": it only shortens the
  window, from about 2.4 s to about 1.0 (the prediction's figures). Whether dripping is still
  visible at about one second is a question for an eye, and the answer decides whether batching,
  step 7, is inside this release's scope at all. If it is not visible, step 7 and its reading debt
  on Chatterino's `addMessagesAtStart` both leave the release's path. Recorded here: his words, the
  build, the channel and the time.

## B. The background measurement

B1 is re-run exactly as `docs/manual-plan-5c-emotes.md` writes it: the dev app's pid, a positive
control of at least 100 frame loads in the 20 s before Home, the count from +1.0 to +30 s, and its
three-way bands.

## C. The closing measurement

**What it decides.** Whether 5d removed the start-up cost the 2026-10-08 attribution found: the view
of every history row built at insert, of which about 15 are ever seen. #84's deferral of the
toolchain upgrade waits for this measurement to be taken and recorded, not for a particular outcome.

### What each outcome means for the release (decided 2026-11-02, before any number)

**The release.** Decided by Federico on 2026-11-02: one release carries all of the chat rendering
work, 5d included, as a closed chapter. So this measurement's outcome is on the release's path, and
what each outcome means is written here before qualification has produced any number. Deciding it
after seeing the numbers is how a pre-registration launders itself, and a release schedule is exactly
the pressure that would do it.

- **The correctness gates block the release:** the harness, the lifecycle proof inside it, the
  seven mutants of H2 as predicted, the manual checks of M, and B1.
- **The prediction's outcome does not block it.** HOLDS, FAILS and INCONCLUSIVE are what the rule
  decides about a model of where start-up time goes. A span falling to 1.6 s instead of about 1.0
  means the prediction failed and the product improved: a wrong model is a knowledge problem, not a
  quality one. FAILS or INCONCLUSIVE is recorded with all its numbers and becomes an open question.
  #84's deferral says the same: it waits for the measurement to be taken and recorded, not to come
  out a particular way.
- **One exception: 5d not better on either quantity.** Then it is no longer a question about the
  model. The numbers go to Federico, and the release decision is his, taken with them visible. The
  rule decides HOLDS, FAILS or INCONCLUSIVE; what is done with anything but HOLDS is not
  pre-registered here.

  What "not better" means, as the agent reads the owner's words. **It is for the owner to confirm
  before the decision launches**, since no decision number exists until then.
  - On one quantity, "not better" is: not every one of its 25 pairings gives a ratio below 1, 5d over
    control. That is the rule's own all-pairings test, with its threshold moved to "no change".
  - "On either" means on the span, on the first stable frame, or on both.
  - A 5d launch with no burst counts as below 1, by the same dominance as the rule's no-burst clause.

  This reading sends every case in doubt, a mixed set of pairings included, to the owner rather than
  away from him. `decide.py` computes it beside the verdicts (`better_than_control`), so that it is
  no more a judgement than they are.

**What the chapter does not close,** named here so that each is a visible choice:

- **Messages missing after a reconnection,** the second symptom, is cause 4 in
  `claude/history-gap-offscreen-tabs.md`, still unattributed. The owner gives that path; the file is
  not in this clone and was not found on this machine on 2026-11-02. It is not part of this work, and
  this release does not close it. The pull request says so, so that nobody reads "the chat work is
  complete" as covering it.
- **The AGP 9.4 / Gradle 9.6 upgrade, and pinning the JDK,** unlock when this measurement is
  recorded (`RELEASE-STATE.md`, *Deferred*), but they are not in this release. A toolchain change
  immediately before a release adds risk to it, and, as the owner reads #84, an upgrade is a pull
  request with its own continuous integration (CI). The sentence was not found in #84's description
  or in the deferral's entry on 2026-11-02. When the measurement is recorded, the deferral's entry
  records that its condition is met and that the work is scheduled after the release, so that
  "unlocked" does not read as "do it now".

### The prediction, handed over, verbatim

Made on 2026-10-09, outside the repository, and recorded here as handed over, not adjusted: span from
about 2.4 s to ABOUT 1.0 s; first stable frame from about 4.0 s to ABOUT 2.6 s; skipped frames must
fall a great deal, with NO NUMBER.

Its baseline, handed over, verbatim: on 2026-10-09, three launches with #78's fix, same probe: span
2490, 2348, 2397 ms; first stable frame (burst end) 4245, 3991, 3939 ms; skipped frames 200, 227,
200. The same-session pre-fix control, run first: span 3645, 3344, 3017 ms; first stable frame 5066,
5104, 5364 ms; skipped frames 347, 327, 289. Three launches, not five. These are context; the
comparison is with this round's own control.

**The burst-end detector has never been qualified, in any round** (found on 2026-11-02, before any
launch of this one). The first stable frame is one of the two quantities this measurement decides,
and `stall` is the only injection that reaches it. 2026-10-08's stall was 50 ms (`strip.py` of
`experiment/startup-cost-attribution`), and Choreographer logs a frame only once it is 30 intervals
late, 333 ms at 90 Hz and 500 ms at 60: a 50 ms stall produces a line at no refresh rate. So the
detector has never once been shown to move to a stall, and every burst end recorded so far,
2026-10-08's and 2026-10-09's included, comes from an instrument that has never been tested. There
has never been a test for a rule to govern. That is the strongest single reason the 2026-10-09
burst-end figures are context and nothing more. This round's `stall`, 534 ms, is the detector's
first test (criterion 5).

**A corroboration that depended on an unrecorded refresh rate.** As the owner reports it, the earlier
record read the burst's 200 skipped frames as "about two seconds at 90 Hz, almost exactly the span",
and took the match as support for the model. That record is kept outside this repository: no branch
of it holds the sentence. 200 frames are 2.2 s at 90 Hz and 3.3 s at 60 Hz, against a span of about
2.4 s, so the match exists only at 90 Hz, and nobody recorded the rate of the launches it was read
from. It supports nothing until that rate is known, and it is not used here.

### The definitions

As `analyze.py` of `experiment/startup-cost-attribution` at `a091809` writes them, copied unchanged
into `experiment/startup-5d` (blob `19baef62`), which computes them for this round:

- **anchor:** `System.nanoTime` in `ProbeApplication.attachBaseContext`;
- **span:** the latest `insertOut` of a history row minus the earliest `begin` of one;
- **first stable frame:** the burst end, the time of this process's last Choreographer
  `Skipped N frames` line within anchor + 10 s;
- **skipped frames:** the sum of those lines' N.

**Why the "2 s rule" is not the first stable frame,** as given by the owner of the measurement,
verbatim: "At 90 Hz, 32 ms is three vsyncs, and every live message produces a frame of 33.3 ms. So
the rule never settles while the chat is still receiving messages: it does not measure the end of
start-up, it measures when the chat stops being live." The spread the 2026-10-08 capture shows for
it, 5721 to 10964 ms against 5290 to 5969 ms for the burst end, is that mechanism's symptom.

**Re-derived at the measured rate, 2026-11-02.** The reason as handed over is 90 Hz arithmetic. At
90 Hz a vsync is 11.1 ms: 32 ms lies just under three of them, and a frame of 33.3 ms is three. At
60 Hz, this phone's rate on 2026-11-02, a vsync is 16.7 ms: 32 ms lies just under two, and a frame of
33.3 ms is two. A live message's frame that took three vsyncs at 90 Hz did more than two vsyncs'
work, more than 22.2 ms and at most 33.3; at 60 Hz the same work takes two vsyncs, 33.3 ms, still
above 32. So the conclusion holds at either rate, and at 60 Hz the rule is stricter still: any frame
that misses a single vsync breaks it. The derivation handed over holds at 90 Hz only, and which rate
2026-10-08's capture ran at was not recorded.

### What the span brackets, on each build

`begin` is the first statement of `appendChatLine`; `insertOut` is the end of `appendChatView`. On
every build each history row passes through both. Before 5d, the timeline insert between them also
built and bound the row's view and started its emote loads. In 5d it notifies the list, and views
are built later, only for the rows that become visible. So 5d's span excludes, by construction,
view-building work the earlier span contained, because that work no longer happens for rows nobody
sees. The prediction expected exactly this: about 1.0 s came from removing about 1.4 s spent building
800 views of which about 15 are visible (handed over, from outside the repository). The ratio does
not say the same work got faster. It says less work happens in that window, which is what the step
does.

**Why the span and the first stable frame are not redundant.** The view building that leaves the
span does not leave the first stable frame. Building the handful of visible rows happens during
layout, before the frame is drawn, so it is inside that measurement. The span says the ingestion
window shrank; the first stable frame says the user-visible settling shrank, including the work that
moved. The two together are what makes the result more than bookkeeping.

### The rule

- **The threshold, per quantity,** is the midpoint between no change and the prediction,
  (1 + ρ) / 2, with ρ the prediction's own ratio: span 1.0 / 2.4, so 0.708; first stable frame
  2.6 / 4.0, so 0.825.
- **ρ is fixed, whatever the control's absolute baseline turns out to be.** `a091809` forked from
  `859ae70`, and what the bracket contains has changed since (#86 moved view building into the
  controller; #89 changed who owns an emote session), unmeasured, because neither was expected to
  move start-up. So the control may not land near 2.4 s. If it does not, ρ does not change:
  re-deriving it from the control's absolute span would be choosing the rule after seeing a number.
  The control's absolute values are reported beside the 2026-10-09 figures, and a material
  divergence is recorded as a finding, that #86 or #89 moved something nobody measured, not as a
  licence. No threshold is needed for it, because no decision hangs on it. Two instrument
  differences from 2026-10-09 are known and stated with it: the viewport walk is off (it cost 4 to
  13 ms inside the span on 2026-10-08), and each row has two more marks.
  *Where its four numbers come from, and the limitation that follows, recorded on 2026-11-03
  before the sitting:* *The provenance of ρ*, below.
- **All 25 pairings** of a 5d launch with a control launch give a ratio, 5d over control. Every
  ratio below the threshold: **HOLDS**. Every ratio above it: **FAILS**. Ratios on both sides:
  **INCONCLUSIVE**, and the round stops.
- **An inconclusive result is a result.** It is recorded with all ten values and every pairing, here
  and in `RELEASE-STATE.md`'s measurement entry. The rule is not re-derived, no launches are added to
  resolve it, and no definition is changed. What to do with a known inconclusive result is a separate
  decision, taken with the numbers visible; it does not rewrite the rule that produced them.
- **Positive controls.** Every control launch must show a burst, or the first-stable-frame
  comparison is void. Every launch must insert the same number of history rows in its span, or the
  run is void.
- **Why the no-burst cases are not symmetric.** A control launch with no burst voids the comparison;
  a 5d launch with no burst passes it. The control must show the phenomenon for the instrument to be
  qualified at all, which is B1's positive control in another form. The treatment not showing it is
  the success the step is for.
- **What "no burst" asserts, named on 2026-11-02, before any measurement.** No frame within anchor +
  10 s started 30 or more frame intervals after its vsync, Choreographer's limit for a `Skipped`
  line. So no single stall reached N = 30 × 1000 / R ms at the launches' refresh rate R: at 60 Hz,
  this phone's rate, N = 500 ms (333 ms at 90 Hz). It does not assert a smooth start: a frame 499 ms
  late at 60 Hz leaves no line. The clause stays as it was, and soundly: no burst strictly dominates
  any burst end that can be measured. It was written without saying what "no burst" means in
  milliseconds, and `decide.py` now names R and N from the launches' own readings.
- **The refresh rate is held and recorded (2026-11-02, before any measurement).** `run.sh` reads the
  display's rate, any per-app frame rate override and Choreographer's warning limit before and after
  every launch (`refresh-rate.txt`). Every reading of the section must be the same, or the round is
  void: the qualification's, the ten decision launches' and the overhead launches'. A burst end
  counts frames, so both halves of the comparison must see the same rate for their burst ends to be
  comparable, and only a per-launch reading can show that they did; the qualification also qualifies
  the burst end at one rate. It is B1's pid scoping in another form: a quantity the rule depends on
  that nothing was recording. `decide.py` voids on any differing reading of the launches it is
  given: the ten, then after `--qualified` every other launch of the section, the qualification's and
  the overhead's. `qualify5d.py` voids a build's qualification the same way. **The phone's setting is
  not changed:** it is measured as its user runs it. Setting 90 Hz would make criterion 5 pass with
  the old 400 ms stall, but it would make the round depend on a mode the system can leave under it,
  on heat, battery saver or an app's request, and move the phone away from the state its user runs.
- **These two are changes to a pre-registered rule,** made on 2026-11-02 before any measurement
  launch and before any of its numbers exists. They are legitimate for that reason: each mends an
  instrument defect found at qualification, which is when instrument defects are meant to be found.
- **Skipped frames** "fell" only if every 5d launch's total is below every control launch's. "A great
  deal" cannot be tested without a number, and none is given.

`experiment/startup-5d/decide.py` applies exactly this rule.

### The span broken down

A total alone would say the span fell, not whether the model of why was right. Each build's span is
reported broken down the same way (`analyze5d.py`): inside each history row, the dedup with the
backfill mark and the mention user; the timestamp with the echo reconcile; the buddy capture; the
hidden-user check; the composer snapshot with the follow decision; the timeline insert, and the view
build inside it; the follow-or-count bookkeeping with the composer restore. Between rows, from the
main thread's own message log: idle, frames, the rest of each row's message, other messages. And the
view builds wherever they ran, by kind, inside a row or outside, with their emote requests. A cost
the model does not predict, left in 5d's span, is the most useful thing the measurement can find.

### The probe on two builds, and its own cost

The probe is one file, byte for byte the same on both measurement builds (`StartupProbe.kt`
`2c7607be`, `ProbeApplication.kt` `704ded58`). Its call sites, counted in each committed tree:

| | control `301111b` | 5d `5851fde` |
|---|---|---|
| ChatFragment | 24: the 22 shared, the viewport hook and the span it reads | 22: the same 22 |
| where a view is built | 2: around `ContainerTimelineViews.create`, inside the insert | 4: around `onCreateViewHolder` and `onBindViewHolder`, outside any row |
| MainActivity, TwitchEmoteImageLoader | 3 + 2 | 3 + 2 |
| total | 31 | 31 |

Equal counts, different composition. What the two builds pay differs in three places, each toward
the hypothesis, since the control pays more:

- **The viewport hook** loses its call site in 5d. Its cost was its walk: 4 to 13 ms inside the
  span on 2026-10-08. The walk is off on both builds (on only with the flag word `visibility`), so
  neither pays it. A no-op hook in 5d would have equalised the count but not the cost: there is no
  reconcile in 5d to call it from, and the walk was the cost. What the control still pays per call is
  the call itself, counted and timed by the probe in each run.
- **The build hooks** run once per history row inside the control's span (about 800 rows, two calls
  each) and only for laid-out rows in 5d. They are not neutralised: they record the work the step
  removes, and a no-op in their place would record a build that does not happen. They are quantified
  instead: each run's counts times the per-call cost the calibration measures.
- **Emote requests** follow the builds, and are counted the same way.

The message log's cost is per main-thread message, so it scales with each span's own length and
messages, counted in each run, times the calibrated cost per message. The CPU snapshots run on the
probe's own thread; their count and cost are recorded per run.

**Measured, per build:** 5 launches with the probe off against the 5 measurement launches, on what
logcat records for both (`overhead.py`, unchanged from `a091809`: the burst end, the skipped frames,
`Displayed`, `TotalTime`, the last history line); and one launch with `full calibrate`, which times a
hook call and a logged message after the window closes. The report gives each build's measured
overhead, the probe's cost inside each span, and the bias it puts on each ratio.

### Qualification, before the measurement rests on the probe

On each build's qualification commit, three baselines with `full`, then one launch per injection with
`full inject=<name>`: `mention`, `reconcile`, `buddy`, `hidden`, `beforeinsert`, `insert`, `follow`
(on page 0's 100th history row), `build` (the 10th view build or bind), `gap` (a message of its own at
history post 200), `app` (in `Application.onCreate`) and `stall` (534 ms at anchor + 8000 ms, with
`arg=8000`; 400 ms until 2026-11-02, *The stall's duration* below).

**Each injection runs once per launch, never once per row.** The probe runs a delay at most once in a
launch - after the first, its `injected` returns at once - and each call site fires at one place
only: page 0's 100th history row, the 10th view build or bind, history post 200, `Application.onCreate`,
or anchor + 8000 ms. So a phase that sums one segment over every row of the span rises by one delay,
200 ms, and criterion 2's floor below, half the delay, is read against that single delay.

**The delay is 200 ms, for every injection that goes through `inject`** (the nine phases and `app`),
decided by the owner of the measurement on 2026-11-02; it was 50 ms in `ccd2c46` and `878f7de`.
The reason is in the paragraph that closes this list, which until then held the decision open.
`stall` is not one of those injections and keeps its 400 ms: it has a constant of its own
(`STALL_MS`), it was never 50, criterion 2 does not read it, and its window below is written for 400.
A delay four times larger perturbs four times as much, so before the qualification is relied on:

- **the rows in the span are read on every qualification launch,** injected or not, not only on the
  decision's launches. Baselines that differ among themselves are the account's case, under *The
  account* below. An injected launch that differs from equal baselines is the delay changing which
  rows are ingested;
- **whether any injected delay trips a timeout or changes which rows are ingested is reported,** per
  injection. If one does, the round stops there and is reported. No other number is chosen in its
  place.

Read from the code before any launch, statically, and so not a substitute for the launches: nothing
on the history path waits for the main thread with a deadline. Each page's request leaves on a thread
of its own with its window computed before any row of it is ingested. The deduplicator's 1.5 s window
applies only to a key equal to the one just before it, and two distinct history rows never share a
key (`id:<message id>`, or `hist:<ms>:<user>:<text hash>`). The buddy capture's 15 s window needs a
request in flight, and the echoes' 10 s timeout needs a sent message; a cold start has neither.

*Correction, 2026-11-02:* "criterion 2 does not read it", above, was the inference of the agent that
wrote that paragraph, not the plan's text. Nothing written before it said what `stall` qualifies or
how it passes. Criterion 5 below says both. And the `:791` above is `ccd2c46`'s line: on `cee6695`
and `a2e72ff` the sleep is at `:796`, on `03a8775` and `0fcd674` at `:804`. "Keeps its 400 ms" held
until 2026-11-02, when the stall became 534 ms (*The stall's duration*, below).

**What every quantity is measured in: wall time, and the delay is a sleep.** Every time the probe
records is `rel()`, `System.nanoTime()` less the anchor (`StartupProbe.kt:243` on the qualification
builds): row marks, inserts, view builds, the main thread's messages, frames. It counts time a thread
spends asleep or blocked as well as time it runs. The burst end comes from logcat's monotonic clock,
the same clock. The injected delay is `Thread.sleep` (`:791`), which passes wall time and uses no CPU.
So a sleep moves every phase it falls in, and criterion 4's containment and the phase's number read
the same clock. The probe's one CPU record is its per-thread snapshots, utime and stime from
`/proc` (`:705-746`), which `analyze.py`'s `cpu_phases` reads. Neither the decision rule nor the
breakdown uses them, and a sleep cannot qualify them: 2026-10-08 had busy-loop injections for that
(`spin`, `spinbg`), and this round's qualification builds have none. If the CPU figures are reported,
they are reported as unqualified context.

Decided before they run (the first three proposed on 2026-10-30 and approved; the fourth added by the
owner of the measurement the same day; the second rewritten by the owner the same day, below):

1. **the injection ran:** the probe recorded it, for at least 200 ms (534 for `stall`, 400 until
   2026-11-02);
2. **the delay reached the output:** the reported value of the target phase rises over the
   baselines' median by at least half the delay, 100 ms. A pipeline check, and only that: it fails if
   the number `analyze5d.py` reports did not take the delay in, which criterion 4, reading timestamps,
   cannot see. It was "at least 45 ms above the baselines' largest" until criterion 4 was added. The
   largest-value reference was conservative against a false pass, and criterion 4 now rules out a false
   pass on its own, so the conservatism would only buy false failures: do not restore it. The cases it
   was checked against, scaled from 50 ms to 200: a correct instrument at the median plus 200, rise
   200, passes; 120 of 200 landing elsewhere, rise 80, fails, and criterion 4 fails with it;
   `analyze5d.py` dropping the delay, rise about 0, fails, where criterion 4 would have passed;
3. **the others did not:** every other per-row segment of the breakdown lies within the baselines'
   range widened by 10 ms on each side. The between-row parts are too noisy for this and are not held
   to it; the span total is not either, its launch-to-launch spread (142 ms on 2026-10-09) being far
   wider than that margin.
   *One-sided since 2026-11-04,* after that sitting's failures were seen: only a value above the
   baselines' highest plus 10 ms fails. Their lowest less 10 ms is printed and decides nothing
   (*Decided on 2026-11-04, after the qualification and before the re-qualification*);
4. **the injection is accounted for,** whatever any phase's width. Criteria 2 and 3 compare a launch
   with other launches, and a phase whose range across launches is wider than the delay can hide the
   delay's leakage, all 200 ms of it, without failing either. This one is read inside the injected
   launch:
   - **inside its phase:** the interval the probe recorded for the delay lies inside the phase it was
     placed in - the target row's segment; for `build`, the target build; for `gap`, one main-thread
     message that carries no row. The part of the interval outside it is time the breakdown would put
     somewhere else. Bound: 1 ms.
   - **inside the main thread's log:** for every injection on the main thread, the interval lies inside
     one message the log recorded. The breakdown computes idle as the span less every message it
     recorded, so time the log does not see would land in idle, where nothing measures it. Bound: 1 ms.
     `app` is the one exception, by construction: it runs in `Application.onCreate`, before the probe
     installs the log, and before the span.
   - **the row's other work unchanged:** the target row's total less the delay, and each of its other
     segments, lie within the same row's values (same page, same history ordinal) in the three
     baselines, widened by 5 ms on each side.
   - **the total, reported:** the span's rise over the baselines' median, with their spread, and the
     change in idle.
5. **`stall` qualifies the burst end,** added on 2026-11-02 at the owner's request and derived below.
   The burst end is one of the two quantities the measurement decides, and `stall` is the only
   injection that reaches it. Qualified means that the detector moves to where the stall is, and
   that nothing else moves. In the `stall` launch, all of:
   - **the detector moves there:** the burst end `analyze5d.py` reports lies no earlier than the end
     of the stall the probe recorded, less 1 ms, and no later than the first frame the probe recorded
     after that end (`frames.ran`);
   - **from somewhere else:** every baseline's burst end lies before anchor + 8000 ms, where the stall
     is placed, so that the move is the stall's and not one the burst makes anyway;
   - **no segment moves:** criterion 3's bound, on every per-row segment;
   - **inside the main thread's log:** criterion 4's second clause, which already holds every
     injection on the main thread: the stall's interval lies inside one message the log recorded,
     within 1 ms.

**Where criterion 5's bounds come from,** each step from code, none from a launch:

- The stall is a message posted with `Handler.postAtTime` for anchor + 8000 ms
  (`StartupProbe.kt:319-323` on the qualification builds). `SystemClock.uptimeMillis`, which the
  handler reads, and `System.nanoTime`, which the anchor is, both read the monotonic clock. So the
  stall begins at 8000 ms or later, when the main thread is next free, and sleeps 534 ms
  (`STALL_MS`, `:778`, slept at `:804`, on `03a8775` and `0fcd674`).
- The probe's frame callback posts itself again at every frame until the window closes
  (`:335-353`), so a frame is always pending, and the stall delays it.
- Choreographer logs `Skipped N frames` when a frame starts at least 30 frame intervals after its
  vsync (`SKIPPED_FRAME_WARNING_LIMIT`, 30 unless `debug.choreographer.skipwarning` says otherwise),
  at the start of `doFrame`, before any callback runs (Android 12's `Choreographer.java`, read from
  its source on 2026-11-02). The pending frame's vsync comes at most one interval after the stall
  begins, so the frame starts at least the stall less one interval late, which is 30 intervals or
  more when the stall is at least 31 intervals. At 534 ms: 517 ms late against 500 at 60 Hz, and
  523 against 333 at 90, so the line is logged at either of the phone's rates. The 400 ms stall
  logged at 90 Hz (389 against 333) and not at 60 (383 against 500).
- So the line is logged after the stall's end and before the probe's own callback in the same frame,
  which is what `frames.ran` records. logcat's monotonic times have millisecond resolution and are
  truncated (`-v monotonic`, `run.sh:52` on `03a8775` and `0fcd674`): hence the 1 ms.

What the bounds leave out is any disagreement between logcat's monotonic clock and the anchor's
beyond that resolution. That is deliberate: their agreement is what places every launch's burst end
on the anchor's clock, and the `stall` launch is the one launch able to test it.

**The display, as found on 2026-11-02, and what was decided.** Read over adb that day, before any
launch: the active display mode is 60.000004 fps, of the phone's two, 60 and 90;
`peak_refresh_rate` is 60.000004 and the refresh range [0, 60]; `debug.choreographer.skipwarning` is
unset. At 60 Hz the 400 ms stall logged no line, and criterion 5 could not have passed. Decided by the
owner the same day: the phone's setting stays as it is; the stall is lengthened instead of the rate
raised; and the rate is read at every launch, the round void if it changes (*The rule*). An earlier
version of this paragraph, the same day, said the launch needed the display at 90 Hz; that was
before the decision, and it is superseded.

**The stall's duration, 534 ms, derived on 2026-11-02.** The line needs the frame 30 intervals late,
and the pending vsync may come one interval after the stall begins, so the least stall that logs is
31 intervals. Taken at the slower of the phone's two rates, 60 Hz, where an interval is 16.667 ms, it
logs at the faster one too: 516.7 ms. One interval more is the margin. It is larger, by a factor of
eight or more, than any offset the display declares between a vsync and its event (1 ms at 60 Hz,
2 ms at 90, `dumpsys display`), and than any error in a vsync's timestamp smaller than a frame. An
error of a frame or more would mean the model of how vsyncs arrive is wrong, and the stall would then
log no line and fail criterion 5 loudly rather than pass wrongly. So 32 intervals at 60 Hz, 533.3 ms,
rounded up to a whole millisecond: 534. At 500 ms, often named as the 60 Hz threshold, the stall
would fall short in the worst case: with the vsync one interval in, the frame is 483 ms late, under
500. Lengthening the stall rather than raising the rate has the right asymmetry: the qualification
then survives a change of display mode in the middle of the round, which the measurement does not,
since one voids it.

One more piece of 90 Hz arithmetic stays in the probe, in the measurement builds too, and is not
changed: `LONG_FRAME_NS`, 330 ms, "30 frames at 90 Hz", which decides when the probe takes a CPU
snapshot after a long frame. Nothing in the decision or the breakdown reads those snapshots, and the
CPU figures are unqualified context already (*What every quantity is measured in*).

**The window criterion 5 replaces.** Until 2026-11-02 the plan said `stall` "puts the burst end
between 8350 and 8600 ms, and moves no segment", phrased as an effect, with nothing saying a launch
outside it fails. It was never derived and never observed. It was written on 2026-10-30 (`5daebe6`), ten
minutes after `ccd2c46` made the stall 400 ms, before any 400 ms stall had run; 2026-10-08's stall
was 50 ms (`strip.py` of `experiment/startup-cost-attribution`), which Choreographer never logs.
Against the derivation, its lower edge lies about 50 ms before the earliest the line can be logged,
which only an unstated allowance for clock disagreement would explain. Its upper edge allowed 200 ms
for the main thread's lateness at 8000 ms and for whatever ran between the stall and the frame, also
unstated. The launch records both itself, so criterion 5 reads them there instead of bounding them by
a figure. The window stays in the list below as it was written.

**The qualification's verdict is computed, not judged (2026-11-02).** Until then no script
evaluated criteria 1 to 5, so the verdict would have been a reading of recorded values by whoever was
there: what pre-registration exists to remove, and what `decide.py` removes for ρ by fixing it in
code. `qualify5d.py` (from `03a8775` and `0fcd674`, labelled from `e2e8e39` and `ee6b027`) now
computes, for one build, criteria 1 to 5, the
rows-in-span rule and the display reading from the run directories, and prints every value it
compares beside each verdict: `python -I qualify5d.py <control|5d> <3 baselines> -- <the 11 injected
launches>`. A script rather than a hand, because criterion 4 alone compares each target row's
segments with three baselines in 22 injected launches, which is where a hand slips, and because a
computed verdict can be rerun by anyone who was not there.

The choices the plan's words leave open are made in its docstring, every one listed there, and they
are the owner's to review before the qualification runs; the owner asked for the list in full, and
it was handed over as written on 2026-11-02. Nine of them were found by auditing the code against the
first list, which had left them out. One is answered already: `build` is the view build inside the
insert on the control and the binds on 5d, which is sound because qualification is per build, against
that build's own baselines. But `build` then sits inside the span on the control and outside every
row on 5d, so its values are not comparable across the builds: the output names each phase's place
on its build (`phase_location`, and `phase_locations` with a note for the whole build), so that no
reader takes a cross-build `build` comparison as meaningful.

The owner approved all sixteen choices on 2026-11-02, with four things, each answered in the script
before any qualification launch:

- **Item 2, criterion 3's exemption, is computed, not hardcoded.** It was hardcoded: three cases in
  `moving_segments`. It is now derived from containment. Each segment is the set of a row's intervals
  between its marks, with the view build inside the insert an interval of its own. Each injection's
  call site is one of those intervals, or none, for those outside every row. A segment is exempt
  exactly when it contains the injection's interval. A new phase needs only its interval and its
  segment, and its exemption follows.
- **Item 7, criterion 1's tolerance:** at least the delay asked for, with no tolerance below and no
  bound above. Android 12's `Thread.sleep` loops on `System.nanoTime` until the requested time has
  passed (libcore's `Thread.java`, read from its source on 2026-11-02), and the probe brackets the
  sleep with `rel()`, so the recorded interval is never shorter than asked. An overshoot is real
  delay. Criteria 4 and 5 read the recorded interval, never the asked one, and criterion 2's floor
  is half the asked delay, which an overshoot can only clear more easily.
- **Items 9 and 11, where `stall` is injected:** outside the span, after it, at anchor + 8000 ms,
  where the span ends with the last history row's insert. Its placement is now checked rather than
  assumed: criterion 5 gains a clause, "after the span", met when the stall begins after the span's
  last insertOut. With it met, criterion 3 is reported NOT APPLICABLE for `stall`. No row's segment
  can contain a delay after the last row, so criterion 3 could fail only through the launch's own
  variation, never through the stall, and a check the injection cannot fail is not printed beside
  checks it can fail. Its segment values are still printed, for the record. `stall`'s qualification
  is criterion 5. If the stall falls inside the span, criterion 5 fails, and criterion 3 is held as
  for any injection. `app`, before the span, keeps criterion 3: its delay moves the whole start-up
  later and can move the span's work, and "nothing inside the span" is what the plan says it moves.
- **Item 5, a baseline with no burst,** is printed as `no burst`, by name, and not folded into
  "every baseline before 8000 ms", which would read as every baseline having had an early burst.

`qualify5d_test.py` runs it on hand-made launches: criterion 2's three scaled cases, a delay leaking
into another segment, a delay outside its segment, criterion 5 inside and outside its bounds, a
display reading that changes, rows that differ, and the phase labels. All 14 cases came out as
expected, run on this machine on 2026-11-02. What has not run is its reading of a real run directory (`facts`), since no
qualification launch exists: before the first verdict is read, the first baseline's printed facts
are checked by hand against its `probe.json`, once. No fixture is built for it: it would only move
the untested reading somewhere else. Every run directory is kept, so that the verdict can be
rechecked.

**Why the total is reported, not held to "rises by about the delay".** Two reasons, with numbers. The
span varied by 142 ms across 2026-10-09's three launches with the fix (2348 to 2490 ms). That was more
than the 50 ms delay. It is less than 200, but still most of it, so a launch's own variation could
still pass or fail "about the delay" either way. And a delay need not move the span's end: the main
thread had 56 to 156 ms of idle inside the span on 2026-10-08, and a delay can be absorbed there, as
much as 156 of its 200 ms, while the rows of the other page, whose history arrives at its own time,
set the end. Nor can "the target's rise plus every other phase's rise equals the total's rise" fail
as it stands: idle is computed as the span less every measured part, so the
parts always sum to the span. What that test is for - 120 of 200 ms going somewhere the instrument is
not looking - is caught where it can be seen: the delay's interval lies 120 ms outside its phase, or
outside any message the log recorded, and criterion 4 fails.

- `gap` moves "other messages"; `build` moves the view build inside the insert on the control and the
  bind on 5d; `app` moves `act_in` and nothing inside the span; `stall` puts the burst end between
  8350 and 8600 ms, and moves no segment (superseded on 2026-11-02 by criterion 5, which says why).

**Decided on 2026-11-02: 200 ms, for every injection.** Until then this paragraph held the question
open, and it stays as it was written, so the reason can be read. The owner adopted the proposal at its
end for its uniformity: no injection needs a special case. One case is left where criterion 2 can
fail a correct instrument: a launch whose phase happens to fall low in its own range. Against the
median, the injected launch's rise is the delay plus its own variation from the median, so it fails
when that variation is more than 25 ms below. For every other phase 2026-10-08 measured inside rows,
whose ranges were 13 ms or less, and for "other messages", whose lowest value was 6 ms below the
median, that lies outside anything seen. The phases this round adds have no history yet. For the
control's view build it is not outside: its five values on 2026-10-08 were 1507, 1555, 1560, 1591 and
1594 ms, and one of the five, 1507, lies 53 ms below their median. About one correct launch in five
would fail there, and a failed qualification stops the round. Proposed, and adopted on 2026-11-02:
the same delay for every injection, 200 ms instead of 50, so that half of it, 100 ms, lies outside
every range seen on 2026-10-08, with no injection treated differently. It needed new qualification
commits on both branches, `cee6695` and `a2e72ff`; the measurement builds did not change.

A qualification that fails stops the round before the measurement.

### Order of the launches, and what each records

1. Qualification on the control, then on 5d. Since 2026-11-02 21:19, each launch is preceded by an
   install of its build, read back from the phone, as the decision's are (*Decided on 2026-11-02,
   after the qualification*).
2. **The decision's launches, in one session, alternating, control first** (since 2026-11-02 21:19,
   decision 5; until then, five control and then five 5d): C T C T C T C T C T, five cold starts of
   `5b09932` with `full` and five of `888489a`. Each is an install of its build read back from the
   phone, a force-stop, 3 s, then `am start -W` (`run.sh` with `APK`). A void launch is re-run, in
   its place in the alternation, until five clean launches exist in each arm.
3. The overhead launches: 5 with the probe off on each build, and one `full calibrate` on each.

Recorded with every launch: the device model, serial, API and build; density and font scale; the
Stay-awake setting and the battery's temperature; the JDK and the AGP and Gradle versions each build
was made with (for this round, one toolchain's, under *Every APK of the round comes from one
machine* as refined on 2026-11-03); since 2026-11-03, the Python version the host scripts ran with,
which computes the verdicts and so is part of the instrument; both pages' channels as reported, with
the probe's channel fingerprint; the history cap;
the APK's commit and hash; the probe's flag; the rows inserted in each span; the launch method and
time; and since 2026-11-02, before and after each launch, the display's refresh rate, any per-app
frame rate override and Choreographer's warning limit (`refresh-rate.txt`, *The rule*).

**The account.** The method is 2026-10-09's: an intent launch with `new_account_id`, two chat pages
on different channels, a channel at the history cap of 400. Which account that round's intent named
was not recorded; it joins what that round did not record, beside its pid scoping and its font scale,
the same family as the battery figures. It does not block this round, which compares with its own
control. What the account protects is the row count, and the positive control already holds it: every
launch must insert the same number of history rows in its span.

So one account is chosen at the first qualification sitting, before any launch of section C, and held
for every launch of the section: the qualification, the ten decision launches and the overhead
launches. Recorded: which account, both pages' channels as reported with the probe's fingerprints, and
each launch's rows in its span, with the equality they must satisfy: all equal, 800 if both pages
reach the cap. If the qualification baselines' row counts are not equal with that account, another is
chosen, before the decision launches and never after.

### Decided on 2026-11-02, after the qualification and before any decision launch

**No compiled state is chased. The confound is measured instead of removed** (decision 1, the owner
of the measurement's).
- **The compile state.** The installed dev build runs from its APK with no compiled code:
  `[status=run-from-apk] [reason=unknown]`, a `DEBUGGABLE` package, read at 20:17 and 20:22. Every
  launch starts interpreted, the JIT compiles while the rows run, and a row's cost falls with the
  process's age (*Recorded results*, criterion 3 diagnosed).
- **The forced compile was refused.** It was attempted at 20:43 on the installed `0fcd674`:
  `cmd package compile -m speed -f com.fs.twitchminichat.dev`. It was refused by the phone maker's
  layer, which answered `Error: Failed to cpmpile !` (sic) in 0.3 s. Nothing was written to any log
  buffer, and the state read back unchanged, `run-from-apk`. The phone's help text for `compile` is
  stock Android, and `pm.dexopt.install` reads `speed-profile`, yet the adb install left the build
  uncompiled. No other mode was tried, because each is a different state from the one decided.
- **The three other ways to a compiled state are rejected:**
  - the background job is profile-guided, so each arm's compiled set would depend on its own launch
    history: an arm-dependent uncontrolled state, worse than uncompiled;
  - another phone changes a recorded parameter, and the accounts live on this one;
  - a different install path is speculative, and would yield a profile that does not exist for a
    fresh install.
- **Why the comparison stands without one.** The span is wall time. The control really did take
  about 2.3 s and 5d about 0.8: those are durations a user waits. What the JIT asymmetry
  contaminates is the attribution, not the comparison.
- **So, reported with the result:**
  - the JIT thread's CPU across each span, per launch, for both arms;
  - the history rows' cost against the process's age, for both arms;
  - the per-phase breakdown always with that curve beside it, never as clean per-phase costs.

  `launchcheck.py` prints both for every launch, and `decide.py` prints them with the verdicts.
- **The asymmetry's direction, inferred from the curve, not measured directly.** The control's span
  reaches the cheap regime, from about 2750 to 3000 ms of process age; 5d's ends around 2000 ms. So
  the control is flattered, and the measured ratio is conservative relative to a cold-against-cold
  comparison.

**What a result supports, stated wherever the result is,** in the owner's words. They replace an
earlier sentence written for identically compiled builds, which these are not:

> Measured on debuggable, uncompiled builds whose JIT compiles during the window being measured. The
> result supports "the ingestion window fell by X on these builds" and does NOT support "users see X
> less on a release build". The control's window reaches compiled code while 5d's does not, so the
> measured ratio is conservative relative to a cold-against-cold comparison.

**The figures of 2026-10-08 and 2026-10-09** were taken on debuggable, uncompiled builds too, as the
owner of the measurement records them. The model is self-consistent across them; its absolute numbers
never described a release build, which is one more reason they are context only. What this
repository's records establish is less: the package, `com.fs.twitchminichat.dev` (`run.sh` of
`experiment/startup-cost-attribution`). Neither round read the build type or the compile state.

**A launch whose history is delivered late is void,** in qualification as in the decision (decision 6).
It was pre-registered by the owner of the measurement before any decision launch:

> A LAUNCH WITH MORE THAN 5 FRAMES DRAWN INSIDE ITS SPAN IS VOID AND IS RE-RUN.

- **Frames drawn inside its span:** the probe's `frames.ran` times from the earliest `begin` of a
  history row to the latest `insertOut` of one, both ends included: the span of *The definitions*.
- **Its derivation, the owner's:** on 2026-11-02's 28 qualification launches an on-time launch drew 1
  to 3 frames inside its span and a late one 14 to 118. 5 is above the on-time maximum of 3 with
  margin and far below the late minimum of 14, so any threshold between 3 and 14 separates the
  observed classes perfectly, and 5 sits low enough to catch a weaker instance. It is derived from
  qualification data, which is what qualification data is for. It is a void condition, an instrument
  check, not a decision rule.
- **Why voiding and not alternation:** alternation is for monotonic drift and stays right for that. A
  block-structured confound that acts differently on each arm needs voiding, not balancing: late
  delivery shows on 5d as cheaper rows and on the control as a longer span.
- **It voids qualification launches too,** because criterion 3's band cannot hold otherwise.

*Retracted on 2026-11-03: the criterion, not the voiding.* The rule quoted above and its derivation
stay as they were written. Frames drawn inside the span no longer void a launch. A launch whose
history is delivered late is still void, under *A launch with a recorded wait is void* (*Decided on
2026-11-03*), which replaces this rule.
- **The launch that justifies it, `-2`,** read by running both scripts over the 15 excluded records
  on 2026-11-03:
  - it is the first launch the new rule catches and the old one missed, and of the 15 records the
    only one;
  - under the old script it was void for the display reading alone;
  - under the new script it is void also for a recorded wait of 921.8 ms before row 224 of page 0;
  - it drew 3 frames inside its span, within the on-time range of 1 to 3;
  - the other 14 drew 10 to 107 frames, and both scripts voided each of them for late delivery.
- **Why it was wrong:** frames drawn inside the span stand in for a pause in a page's history, and on
  2026-11-03 the stand-in failed. At `-2`, page 0's history paused for 922 ms before a
  spawn-classified row, and the main thread drew 3 frames inside the span. That is within the on-time
  range of 1 to 3, so the rule passed the launch. Only the display reading voided `-2`.
- **Why its derivation could not show it:** on 2026-11-02 the two classes, 1 to 3 frames and 14 to
  118, separated perfectly. The probe's wait floor was set from the same 28 launches: on-time pauses
  of at most 212 ms, late ones of at least 902 ms. So on those launches pauses and frame counts agreed
  (inferred from the two derivations, not recomputed). At `-2` they did not. Page 1's rows kept
  beginning on the main thread throughout page 0's pause: 223 of them, the last 218 ms before it
  ended. A frame count separates late launches from on-time ones only while nothing else keeps the
  main thread busy.
- **The frames count stays recorded:** `launchcheck.py` prints it for every launch, and it decides
  nothing.

**Five clean launches per arm** (decision 4). The all-25-pairings rule stands as written. Ten per arm
would make it all-100-below, stricter than the rule that does not change: a silent tightening. If the
result is INCONCLUSIVE on variance, that is the rule working, and the pre-decided path is that the
numbers go to Federico.

**Full alternation, an install before every launch, control first** (decision 5). Both arms are one
package, so alternating means an install at every switch, and the install comes before every launch,
including where the arm does not change. So every launch is a first launch after install, uniformly in
both arms. Blocks would make launch 1 of each block first-after-install and launches 2 to 5 not,
non-uniform within an arm, which is worse. All ten are recorded as first-after-install:
- `run.sh` with `APK` set installs, then reads the installed APK back from the phone (`install.txt`);
- `launchcheck.py` voids a launch without a matching read-back;
- `decide.py` voids ten that do not run C T C T C T C T C T by their launch times.

**A process started inside a launch voids it** (decision 7, the agent's proposal, accepted as written):
any process start other than the app's own, between the launch and the end of the span. It is
deliberately strict, since strict and unambiguous beats tuned: the lesson of criterion 2's reference
and of B1's pid scoping.
- **Read from:** each launch's `logcat.txt`, which holds every `ActivityManager: Start proc` line.
- **The launch** is the app's own `Start proc` line.
- **The app's own** are processes named after its package, or started for a component of it.

*Correction:* the proposal was put with "in 28 launches it caught two". Those two counted every start
in a launch's log, not the window above. Inside the window it catches none of the 28:
- `Qc-buddy`'s cell-broadcast receiver started 0.64 s before the app's own process;
- `Q5-app`'s Discord process and its WebView sandbox process started 6.1 and 7.3 s after its launch,
  after its span. The sandbox process was started for a document scanner.

**The battery,** from the owner's instruction before the qualification: battery saver, the level and
the temperature are read and recorded at every launch, before and after (`battery.txt`), beside the
power source.
- **Held:** battery saver and the power source. A launch during which either changes is void; a
  section whose launches do not share one state is void.
- **Recorded, not held:** the level and the temperature, as the agent reads the instruction "void on a
  change in any of them". The phone is to be charging, so its level rises from one launch to the
  next and its temperature moves with it, and a literal void would void every sitting. That reading
  is for the owner to confirm.

Before the decision launches, battery saver off and the phone charging are confirmed and recorded.

**What does not change:** the prediction, ρ, the thresholds, the all-25-pairings rule and the release
outcomes. Today's findings touch which launches count and what the numbers describe, not what the rule
says about them.

**The probe, after the qualification:**

- **The fix, on the qualification branches only** (injection code): `253373a` (control) and `500248e`
  (5d).
- **Read 1, two snapshots inside a wait in a page's history rows:** probe code, on all four builds
  (decision 2). The qualification must qualify the thing being measured. A qualification build
  carrying probe code its measurement build lacks would qualify a cousin of the measured build: the
  same property as a frozen copy that must not be steered, the instrument has to be the one under
  test.
- **Read 2, the first history row of a page after a wait, by shape** (decision 3), on all four builds.
  It records:
  - which of the fields the backend may leave out are present: id, emotes and timestamp. User and
    text are always set by the parser, so their presence says nothing;
  - the spawn ingestion's own classification of the message, its outcome's and source's names;
  - the emote count: the `start-end` entries of its emotes field, as the app's parser reads it, less
    the parser's check of each emote id;
  - the text's length in UTF-16 units.

  An enormous emote list and an enormous message are the two likeliest causes of a per-message stall,
  and a count and a length are not values or text. Not built, and rejected by the owner: the IRC
  command and tag names asked for first, since a history row reaches the app as backend JSON
  (`BackendHistoryResponseParser`) and has neither; and the JSON's key names, since a parser hook on
  all 400 items in every launch perturbs the thing being measured for less information.

- **The qualification sitting's launches are each preceded by an install too,** the agent's
  extension of decision 5 by decision 2's principle: the qualification qualifies the instrument under
  the conditions it measures in. It is for the owner to confirm.

### Decided on 2026-11-03, before the second qualification

**The builds come from `nbpromec1`**, under *one toolchain, not one machine* (*Every APK of the round
comes from one machine*). The four builds are the `nbpromec1` rows of the table at the top.

**The channel: both pages on one cached, full channel** (the owner of the measurement's). Federico
has no cached channel with an offline stream. So both accounts go on the same cached channel at the
400-row cap: `c8cb0e91` if it is still at the cap, otherwise whichever cached full channel he has put
them on. That is the configuration the qualification of 2026-11-02 ran on.

**A declared deviation.** The method of 2026-10-08 specifies two pages on different channels. Both
pages are on one.
- **The consequence:** every live message is handled twice inside the span, which inflates both arms.
- **Why the comparison survives it:** alternation makes that overhead common to the two arms, so it
  cancels in the ratio. What it costs is variance, and so a higher chance of INCONCLUSIVE. It is a
  loss of statistical power, not of validity.
- **Why it is declared:** the method named the requirement, so departing from it silently would hide
  a condition every result depends on.

**The live-message count is recorded with every launch.** That makes the confounder a measured
quantity rather than one argued away. A systematic difference between the arms, which alternation
should prevent, then shows in the record instead of hiding in the spread.
- **What the scripts at `92fbb4a` and `1ae4b42` print, read from them and not yet run:**
  `launchcheck.py`'s `live_rows_by_page`, each page's live rows over the probe's whole record, the same
  14 s window from the anchor in both arms.
- **Not counted, by the owner's decision of 2026-11-03: the live rows inside the span.** This is a
  decision, not an omission. **An in-span count is confounded with the outcome.** The arm with the
  longer span sees more live messages inside it, so comparing in-span counts between the arms would
  partly measure the span itself. On 2026-11-02 the span was about 2.3 s on the control and 0.8 s on
  5d. The fixed 14 s window is the same in both arms, and it is the correct denominator.

**A general note, from that decision.** A quantity that looks more precise because it is scoped to the
thing being measured can be worse, because the scope is the outcome. Before narrowing a count to the
span, the window or the phase under test, ask whether the narrowing itself depends on the result.

**Battery saver and the display rate.** Battery saver is Federico's to turn off, and he is being
asked. Once it is off, the refresh rate is read again, and whatever it then reads is held for the
whole section. Yesterday's section ran at 90 Hz, but it is being redone, so there is no comparability
with it to preserve, only constancy within this section.

**The account** is `0d3d33b8…`, as on 2026-11-02. Its full id is read from the phone, from the key
names in `shared_prefs/channel_history.xml`, by the command that uses it. It is never copied by hand
between machines. That error class has bitten this project before: a full SHA typed from memory
passed a check only because its seven-character prefix matched.

**One excluded launch before any counted launch.** It is excluded from every count, and it reports
only each page's channel fingerprint and history row count. Both pages must read 400, giving 800 in
every span. If either reads anything else, the sitting does not start, and the reading is reported.
A count that differs between launches would void the sitting partway through, wasting the whole run.

**A launch with a recorded wait is void and is re-run,** in the qualification as in the decision.
The owner of the measurement decided to rule on the waits themselves. The threshold and how it was
chosen are the agent's, given below. It replaces the frames rule of 2026-11-02, which stays in place
with its retraction beside it (*Decided on 2026-11-02, after the qualification*).
- **The rule:** a launch is void when its probe record holds one or more wait rows. That is a
  `waitRows.n` of 1 or more, in either page, before any row, whatever the row's outcome.
- **A record without the field is void, not clean.** A missing field does not mean zero waits: the
  probe writes wait rows only in `full` mode (`StartupProbe.historyPostRunBegin` returns otherwise).
  The qualification and decision launches are `full`.
- **What a wait row is,** read from `StartupProbe.kt` at `cc4c0cf`, whose recording lines are the
  same on `5b09932`, `888489a` and `1198c79`. When a page's posted history row starts on the main
  thread, the probe takes the time since that page's previous history row began, and keeps it when it
  is 300 ms or more (`HISTORY_WAIT_NS`). It keeps the slot, the row's ordinal, the instant and the
  length (`gapNs`), with the row's shape. A wait runs between two history rows of one page, so by
  construction it lies inside the span. At most 4 are kept (`WAIT_ROW_CAP`); the rule needs only one.
- **The threshold is 300 ms, the probe's own floor.** How it was chosen:
  - *Set from 2026-11-02:* no on-time launch paused a page's history rows for more than 212 ms,
    including the injected 200 ms delays, and every late launch paused a page for 902 ms or more.
    These figures are read from the probe's KDoc at `cc4c0cf` (lines 79 to 83), not recomputed. The 28
    launches' records are on `DESKTOP-BCT68L9`, and the summaries committed at `92fbb4a` hold no row
    times. 300 ms is 88 ms above the on-time maximum, so an injected 200 ms delay does not void a
    qualification launch. It is 602 ms below the late minimum.
  - *Checked against 2026-11-03:* the 15 excluded launches recorded 33 waits. 28 of them, 397 to
    1516 ms, came before spawn-classified rows; 5, 302 to 394 ms, came before ordinary rows. Every
    launch has at least one wait of 397 ms or more, so every threshold from 300 to 921 ms voids the
    same 15. These data do not choose a value within that range.
  - *Not lower:* the record holds nothing under 300 ms, so a lower threshold cannot be read from it.
    Lowering the floor is a probe change, which means rebuilding the four APKs.
  - *Not higher:* no on-time launch of 2026-11-02 showed a pause like the five waits of 302 to
    394 ms, and their cause is unknown. A higher threshold would count launches with a pause the
    reference never had.
- **What it cannot catch,** inferred from the probe's code, not observed as a missed launch:
  - a page whose history worker is held while rows it has already posted keep the main thread
    supplied. At `-2`, page 1's worker logged its spawn ingestion in the same millisecond as page
    0's, so it was held too, yet page 1's largest gap was 224 ms. That launch is caught by page 0's
    922 ms wait;
  - a delay before a page's first history row, since there is no earlier row to measure from.
- **Not yet in the scripts.** `launchcheck.py` at `92fbb4a` (`experiment/startup-5d-control`) still
  voids on frames (`FRAMES_IN_SPAN_MAX = 5`) and not on waits. Until it changes, its void list is not
  this rule. `launchcheck_test.py` has 4 cases, and read through, none is on frames. Changing the
  script is not part of this step.
- *Brought into the scripts on 2026-11-03:* `3cf6ff7` on `experiment/startup-5d-control` and
  `bad5699` on `experiment/startup-5d-treatment` (*The wait rule, brought into the scripts*, at the
  end of *Recorded results*). The sentence above stays as it was true when written.

### The provenance of ρ, recorded on 2026-11-03 before the sitting

Asked by the owner of the measurement, and committed before any launch of the qualification sitting.
ρ does not change, and the thresholds stay 0.708 and 0.825. This records an asymmetry; it does not
adjust the rule.

*Its scope, corrected on 2026-11-03 by the owner:* binding before the closing measurement, not
before the qualification (*The qualification sitting of 2026-11-03*, below).

*Its status, set on 2026-11-04 by the owner:* this section is a **pre-registered limitation** of the
closing measurement. It is binding before that measurement, not before the re-qualification. ρ and
the thresholds do not change (*Decided on 2026-11-04*, item 3).

**Where `decide.py` fixes it.**
- `RHO = {"span": 1.0 / 2.4, "burst_end": 2.6 / 4.0}` is line 32 of `decide.py` as `301111b`
  introduced it on 2026-10-30. It is line 77 on today's branch tips, `2e72e42` and `550b414`, which
  share blob `3f69c6a`.
- Line 8 says ρ is not re-derived from the control.
- The four numbers come from *The prediction, handed over, verbatim*, recorded in this plan by
  `5daebe6` on 2026-10-30. The 1.4 s of view building beside it was added by `5bdd999` the same
  day.
  - *Corrected on 2026-11-04:* `5daebe6` wrote the 1.4 s as well, not `5bdd999` (*After the
    re-qualification, 2026-11-04*, item 2). The sentence above stays as it was written.
- The prediction and its numbers were made on 2026-10-09, outside the repository, and handed over.

| Number | Measured or target | From |
|---|---|---|
| 2.4 s | measured, rounded | the span of three launches of 2026-10-09 "with #78's fix, same probe": 2490, 2348 and 2397 ms, mean 2412, median 2397 |
| 4.0 s | measured, rounded | the burst end of the same three launches: 4245, 3991 and 3939 ms, mean 4058, median 3991, from a detector never qualified |
| 1.0 s | target | the prediction for 5d's span: 2.4 s less about 1.4 s of view building, for 800 views of which about 15 are visible, as handed over |
| 2.6 s | target | the prediction for 5d's burst end; 4.0 − 2.6 is the same 1.4 s, so a derivation like 1.0 s's is inferred from the arithmetic, not stated in what was handed over |

**The build behind the two measured numbers.**
- This plan names it only as "#78's fix, same probe".
- `a091809`, on `experiment/startup-cost-after-mention-fix`, is the probe on `488cac5`, #78's commit
  before its squash.
- That it is the build of 2026-10-09 is inferred from the branch and from this plan's citations of
  `a091809`. It is not recorded beside the numbers.

**Did the span or the burst end contain the catalogue parse?**
- **The code path was in that build, read on the commits.**
  - In `a091809`, and in `ccbdab2`, the same session's pre-fix control,
    `SmartCatchSpawnCoordinator.kt`, `SmartCatchSpawnIngestion.kt` and `PokemonTypeDex.kt` are the
    same blobs as in this round's unfixed control, `c8a9f99`.
  - `resolvePokemon` is at line 215, with no window gate before it.
  - Both catalogue assets are the same blobs.
  - The history thread calls `ingestHistorySpawn` for each message (`ChatFragment.kt`, lines 2951
    and 2988).
- **Whether the parse ran inside those spans is not recorded.**
  - That probe kept no history pauses and no spawn outcomes.
  - The three launches' records are not in this repository.
  - Nothing records whether their cached history held a spawn row.
  - So for 2.4 s and 4.0 s the answer is: possible, not known.
- **Even had the parse run, it need not have lengthened the span.** This is inferred, and not shown
  for 2026-10-09.
  - Before 5d a history row cost the main thread a median of 3.4 to 3.8 ms (2026-10-08's
    `capture-table.md`), so rows queued behind it.
  - A worker paused for a second can leave a queue that keeps the main thread busy. At `-2`,
    page 1's rows did exactly that through page 0's pause.
- **What 2026-10-08's capture shows.** In the attribution's five launches (`capture-table.md`, the
  same file on both startup-cost branches), main-thread idle inside the span was 56 to 156 ms.
  - So no second-long pause left the main thread empty in those five.
  - Whether a pause occurred and was absorbed cannot be read from the table.
- **1.0 s and 2.6 s are targets,** so the parse question does not apply to them as measurements.
  But each is its baseline less 1.4 s of view building, so whatever the baseline held, the parse
  included, the target holds too.

**The direction, if 2.4 s and 4.0 s did hold the parse.**
- **As first stated, the inequality was the wrong way round.** The owner's instruction stated
  (t − k)/(c − k) > t/c for 0 < t < c and 0 < k < t. The inequality runs the other way:
  t/c − (t − k)/(c − k) = k(c − t) / (c(c − k)), which is positive for those values.
  - Removing a shared constant from both sides lowers a ratio below 1.
  - With k = 0.35 s, for example, 0.65 / 2.05 = 0.317, against 1.0 / 2.4 = 0.417.
- **k comes out of both sides in this sitting.**
  - Both qualification builds carry the gate (*The fix on the four branches, rebuilt*).
  - A launch in which the parse still runs and pauses a page for 300 ms or more is void under the
    wait rule.
  - So if the baseline of 2026-10-09 held k, the model's own expected ratio for these builds is
    below ρ. The same absolute saving V is also a larger share of a smaller control:
    (c − k − V)/(c − k) < (c − V)/c.
- **So the thresholds would be easier to pass than intended, not harder.**
  - A **HOLDS** verdict then carries the named confound: ρ's baseline held a cost that these builds
    no longer pay.
  - A **FAILS** verdict stays interpretable, since it was reached against a threshold the confound
    made easier.
  - An **INCONCLUSIVE** verdict reads the same either way.
- If 2.4 s and 4.0 s held no parse, there is no asymmetry. Which of the two is true is not known
  (above), so this stands as a conditional limitation.

**The diagnostic launch's control span, 2674 ms without the parse, against the predicted control of
2.4 s.** This reading does not explain it.
- **The size expected.** If 2.4 s had held a pause of the size this round has seen on the control,
  0.35 to 1.2 s of extra span on 2026-11-02, a control without it would be expected well below
  2.4 s, all else equal. 2674 ms is above it.
- **The control's on-time baselines of 2026-11-02 were also near 2.4 s.** They had no history pause
  above 212 ms, so no parse pause inside the span. `Qc-base-1` ran 2210.1 ms, and the three spread
  over 135 ms (`qualification-2026-11-02/`).
- **What that supports.** It is consistent with 2.4 s having held no parse pause, but cannot show
  it: #86, #89 and the instrument have moved the bracket since 2026-10-09, unmeasured (*The rule*).
- **What it leaves open.** It does not explain the 464 ms between 2674 ms and `Qc-base-1`'s
  2210 ms. One difference was seen: in the diagnostic launch, page 1's rows ran after page 0's
  instead of interleaving with them. Whether that is the difference was not examined.

### The qualification sitting of 2026-11-03: two corrections and its procedure, before its first launch

**Correction 1, the owner's: the launch design first asked for was the closing measurement's.** The
owner's instruction for this sitting first asked for C T C T C T C T C T, five launches per arm, every
launch uniformly first-after-install, and asked that design to produce the qualification's output,
criteria 1 to 5 and two verdicts. In the owner's words, those are two different sittings:
- the alternation and the uniform first-after-install exist so that the closing measurement's 25
  pairings are symmetric;
- the qualification is per build and per phase. Each build is qualified against its own baselines,
  with no comparison across the arms. So "control, then 5d" (*Order of the launches*, item 1) is the
  plan's design, and alternation has no role in it;
- asked together as written, the sitting would have produced ten launches and no computable verdict.

The agent raised the conflict before any launch, and the owner chose the plan's qualification.

**Correction 2, the owner's: the scope of *The provenance of ρ*.** The instruction that no launch run
until ρ's provenance was written down was wrong as sequencing.
- ρ lives only in `decide.py` and governs the closing measurement.
- The qualification is scored by `qualify5d.py`. It loads `decide.py` for two helpers only,
  `display_readings` and `refresh_rate` (lines 210 and 423), and reads neither `RHO` nor
  `THRESHOLD`. This was read in the script.
- So *The provenance of ρ* stays binding before the closing measurement, not before the
  qualification. It was committed before this sitting anyway (`1e13f54`). Its first sentence stays as
  it was written.

**The sitting, as it will run.** The phone was disconnected when this was written, so no launch has
run.
- **The order:**
  - one excluded launch, which reports only each page's channel fingerprint and history row count.
    Both pages must read `c8cb0e91` and 400, or the sitting does not start (*Decided on
    2026-11-03*);
  - then the control, `2e72e42`: three baselines with `full`, then one launch per injection.
    The injections are `mention`, `reconcile`, `buddy`, `hidden`, `beforeinsert`, `insert`,
    `follow`, `build`, `gap`, `app`, and `stall` with `arg=8000` (*Qualification*);
  - then 5d, `550b414`, in the same way. That makes 28 counted launches.
- **The uniformity rule within each build:** every launch, baselines and injections alike, is a first
  launch after an install of its build, read back from the phone.
  - It is decision 5's rule (*Full alternation, an install before every launch, control first*,
    under *Decided on 2026-11-02, after the qualification*). *Order of the launches*, item 1, has
    applied it to the qualification since 2026-11-02 21:19.
  - `run.sh` with `APK` installs and reads back immediately before each launch, and `launchcheck.py`
    voids a launch without a matching read-back.
  - So each injected launch meets its baselines in the same state.
- **The read-back before each launch, against STEP B's sha256:**
  - `run.sh` reads the installed APK back but compares nothing.
  - So before calling it, the driver installs the same APK, reads it back, and stops the sitting on a
    mismatch, before the launch.
  - After the launch, it holds `run.sh`'s own read-back to the same value.
  - Each launch is therefore preceded by two installs of the same file. The one immediately before
    the launch is `run.sh`'s, as in every earlier launch of the round.
- **Voids:** a void launch is re-run in its place, after it has been read (*A launch with a recorded
  wait is void and is re-run*). `qualify5d.py` stops a verdict that contains a void launch. The driver
  stops at each void, so no re-run happens unread.
- **A recorded wait on an `IGNORED_EXPIRED` row** stops the sitting. By the owner's instruction, it is
  reported as a contradiction of the diagnostic launch and not discarded as a void.
- **The scripts:**
  - The run directory `%USERPROFILE%\tmc-5d-round\C` now holds `2e72e42`'s scripts, which are the same
    blobs as `550b414`'s. All ten files were checked and read the same.
  - `launchcheck.py` and `launchcheck_test.py` there were the copies from `92fbb4a`. They were copied
    aside byte for byte before being replaced.
  - `qualify5d.py` loads `launchcheck.py` from its own directory. Without the replacement, its
    verdicts would have applied the retracted frames rule.
  - Run there on Python 3.10: `launchcheck_test.py` passes 7 of 7, and `qualify5d_test.py` 22 of 22.

**The three quantities the owner added, per launch, both builds, voiding nothing.** They are computed
by `sitting_extra.py`, outside the repository, from each launch's `probe.json` and `logcat.txt`.
- **a. The largest gap below 300 ms between consecutive history rows, per page, with the row it
  precedes.**
  - The method takes one page's history rows, sorted by `begin`, and differences successive begins.
  - It was qualified against the probe's own recorded waits. In the 15 excluded records it gives a
    gap before the same row of the same page for all 33 recorded waits, within 0.3 ms of the probe's
    value.
- **b. Page 1's start offset from page 0's first row, and whether the pages' rows interleaved or ran
  sequentially.** The rows interleave when the two pages' ranges of begins overlap.
  - In the 14 excluded records with both pages, the pages interleaved every time, with page 1
    starting 252 to 1100 ms after page 0.
  - In the diagnostic launch they ran sequentially, with page 1 starting 1688 ms after page 0.
  - This is recorded, not interpreted.
- **c. Every spawn ingestion,** with four fields:
  - its time after the anchor, on logcat's monotonic clock;
  - its outcome;
  - its page, found by matching the logging thread against each page's `historyTid`;
  - whether a recorded wait of that page holds its time, from `at − gapNs` to `at`, within 2 ms.

  What the 16 existing records give:
  - in the 15 excluded records, all 25 `ignored_expired` history ingestions lie inside a recorded
    wait of their page;
  - 3 of the 4 `unchanged` ones do too. The fourth is `-2`'s page 1, whose largest gap was 224 ms;
  - in the diagnostic launch, neither ingestion does.

### Decided on 2026-11-04, after the qualification and before the re-qualification

Asked by the owner of the measurement after the qualification sitting of 2026-11-04 (*Recorded
results*). No launch ran for any of it. Everything below was computed from that sitting's recorded
launches, or read in code, and each item says which.
- Both builds stay **NOT QUALIFIED**, as recorded.
- `qualify5d.py` was not run over the 2026-11-04 data under the changed rule, and will not be. The
  new rule is tested on a new sitting.

**1. The drift across launches, measured before anything changed.** Computed on Python 3.10.0. The
scripts and outputs are in `experiment/startup-5d/qualification-2026-11-04/drift/`, on both
experiment branches: `b808a60` on `experiment/startup-5d-control`, and `53f02e4` on
`experiment/startup-5d-treatment`.
- **The rows used are the ones no injection lands in, in any of a build's 14 launches:** page 1's
  history rows, and page 0's rows 11 to 99.
  - The row injections land in page 0's row 100, and the control's `build` in page 0's row 10.
  - 5d's `build` is a bind outside every row, `gap` holds no row, `app` is before the span, and
    `stall` is after it.
  - The extract reproduces every criterion 3 value `qualify5d.py` printed for the sitting: 162
    values, largest difference 0.000000 ms.
- **Page 1's history rows, summed per launch, in ms, in sitting order.** The labels are the counted
  launches: the control's `base-2` is `-r1` and its `gap` `-r2`; 5d's `buddy` is `-r2` and its
  `stall` `-r1`.

  | Position | Launch | Control `timeline_insert` | Control whole row | 5d `dedup_backfill_mention` | 5d whole row |
  |---|---|---|---|---|---|
  | 1 | `base-1` | 717.2 | 869.0 | 90.7 | 160.3 |
  | 2 | `base-2` | 747.7 | 909.2 | 95.8 | 163.0 |
  | 3 | `base-3` | 685.4 | 825.3 | 79.0 | 139.3 |
  | 4 | `mention` | 669.9 | 809.3 | 93.4 | 167.3 |
  | 5 | `reconcile` | 656.3 | 788.5 | 67.7 | 125.7 |
  | 6 | `buddy` | 671.2 | 811.0 | 69.7 | 141.5 |
  | 7 | `hidden` | 657.6 | 789.3 | 71.9 | 146.1 |
  | 8 | `beforeinsert` | 690.7 | 835.5 | 26.6 | 73.7 |
  | 9 | `insert` | 649.5 | 780.2 | 76.0 | 156.0 |
  | 10 | `follow` | 659.4 | 797.9 | 46.9 | 102.1 |
  | 11 | `build` | 672.0 | 812.3 | 44.9 | 101.8 |
  | 12 | `gap` | 668.3 | 808.1 | 83.8 | 154.5 |
  | 13 | `app` | 676.3 | 812.0 | 84.4 | 157.5 |
  | 14 | `stall` | 637.3 | 763.6 | 67.1 | 131.9 |

  - **No series falls monotonically.** Of the 13 steps, 7 fall on the control's two series, and 6
    on 5d's.
  - **Spearman against position, all 14 launches, with a permutation p:**
    - control: `timeline_insert` −0.508 (p 0.067), the whole row −0.516 (0.061), and
      `follow_or_count_composer_restore` −0.675 (0.010);
    - 5d: dedup −0.473 (0.090), and the whole row −0.380 (0.181).
  - **Among the eleven injections alone,** positions 4 to 14, it is −0.036 and −0.055 on the control,
    and −0.127 and −0.073 on 5d.
- **Why a rank correlation over the 14 is the wrong summary.** In this design, positions 1 to 3 and
  "baseline" are the same three launches.
  - So a correlation with position cannot tell a drift with launch order from a difference between
    the baselines and the injections.
  - Its value here comes from where the baselines sit: among the injections it is near zero.
  - What the band depends on is whether the three baselines sit apart from the eleven. The right
    summary has three parts:
    - the exact probability that three launches drawn at random from the 14 rank as high as the
      baselines, over all 364 draws;
    - the gap of medians;
    - the same comparison at matched process age.
- **The direct answer: yes, the baselines sit higher on rows no injection touched.** It holds on the
  control's page 1 and on 5d's page 1, and not on 5d's page 0.
  - **Control, page 1, `timeline_insert`.**
    - The baselines read 717.2, 747.7 and 685.4. The injections read 637.3 to 690.7, median 668.3.
    - The gap of medians is +48.9 ms, with an exact p of 0.0055.
    - The baselines rank 1, 2 and 4 of 14. One injection, `beforeinsert` at 690.7, lies above
      `base-3`.
    - The view build gives +46.0, `follow_or_count_composer_restore` +8.0, and the whole row
      +60.9, each with p 0.0055.
  - **Control, page 0's rows 11 to 99, `timeline_insert`.**
    - The baselines read 231.5, 203.4 and 229.6. The injections read 183.7 to 231.2, median 199.9.
    - That is +29.7, with p 0.063, so not below 0.05. Five injections lie above the lowest baseline.
  - **5d, page 1, dedup.**
    - The baselines read 90.7, 95.8 and 79.0. The injections read 26.6 to 93.4, median 69.7.
    - That is +21.0, with p 0.030.
    - The baselines rank 1, 3 and 6. Three injections lie above the lowest baseline: `mention` 93.4,
      `gap` 83.8 and `app` 84.4.
    - The whole row gives +18.8, with p 0.113.
  - **5d, page 0's rows 11 to 99: no gap.** Dedup gives +2.1 (p 0.184), and the whole row +1.1.
  - **At matched process age.** Every row but the target row was binned by 100 ms of its begin.
    - On the control the baselines' median per row is higher in 20 of 21 bins, for `timeline_insert`
      and for the whole row alike, and in 15 of 21 for `follow_or_count_composer_restore`.
    - On 5d it is higher in 7 of 8 bins for dedup, and 6 of 8 for the whole row.
  - **Over the whole span**, criterion 3's value, the gap of medians was +140.8 ms for the control's
    `timeline_insert`, and +32.4 ms for 5d's dedup. Gaps of medians do not add, so the row sets'
    gaps are not parts of these.
- **It is not the within-launch process-age effect.** That effect is real.
  - On the control, the median whole row cost 2343 to 2850 µs on page 0's rows 11 to 99, at age
    medians of 1589 to 1880 ms. On page 1 it cost 1837 to 2230 µs, at 3110 to 3457 ms.
  - That effect compares rows inside one launch. The drift compares launches at the same age:
    - on page 1 the ages match, 3258 to 3416 ms in the baselines and 3110 to 3457 in the injections,
      and the gap holds;
    - it also holds in 20 of 21 matched bins.
- **By a static reading of `StartupProbe.kt`, it is not the `inject=` flag.**
  - The flag only sets `inject`.
  - Each site returns at once when `inject` is not its name, and `injected()` sleeps 200 ms once.
  - So before its delay, an injected launch differs from a baseline only by those comparisons. They
    add cost and cannot remove it.
  - This was read, not tested. P1 (below) is the test.
- **Its shape is a step at the baselines, not a trend with order.**
  - The pre-registration says "the first three launches of each build's block sit above the rest".
    That overstates both builds. On the control one injection lies above the lowest baseline, and on
    5d three do.
  - The pre-registration is hashed and is not edited. The numbers above are the record.
- **Its cause is not identified.** Within a build's block, position, clock time, battery level and
  temperature all move together. The level went from 42 to 56 on the control and from 58 to 74 on
  5d; the temperature from 29.3 to 32.1 °C and from 31.6 to 32.6 °C.

**2. The re-registration.** It is made after the failures of 2026-11-04 were seen, and it is written
down as such, as the owner required.
- **The pre-registration** is `experiment/startup-5d/requalification-prereg-2026-11-04.md`. It was
  added by `67d0fdc` on `experiment/startup-5d-control` and `43294a8` on
  `experiment/startup-5d-treatment`.
  - Its sha256 is `36ce1935e44c5812bfc786831d1826d5958792f493032294d3cf63bf3a3c24a4`, git blob
    `810b8be`.
  - It was written by 09:36:53 +01:00 on 2026-11-04 (the file's time) and hashed at 09:37:02.
  - It was committed at 09:37:25 and 09:37:56, then pushed. Both branches read back on the remote
    at those commits before 09:52, by this machine's clock.
  - No launch of the re-qualification has run.
- **(a) Criterion 3 becomes one-sided.**
  - **The rule:** a held segment passes when it is at most the baselines' highest plus 10 ms. Their
    lowest less 10 ms is printed beside it and decides nothing.
  - **The reason:** a delay that leaks into another phase raises that phase. A value below the band
    is not what the band exists to catch.
  - **Not a claim that a low value cannot be leakage.** A different scheduling can lower a phase, so
    "cannot be leakage" would be too strong. What rules the low values in as something else here is
    item 1's drift.
  - **It rescues no upward failure,** and none of the 28 launches of 2026-11-04 had one: all 28
    failing values, 19 on the control and 9 on 5d, lay below their bands. The nearest to its upper
    bound was 5d `insert`'s `follow_or_count_composer_restore`, 0.0 ms below it.
  - **Its cost:** the bound is set by the highest baseline. A launch run after a step can therefore
    carry an upward leak smaller than the step without failing.
  - **The implementation** is `qualify5d.py` blob `19d110c`: `e3ac8ab` on the control's branch and
    `01eaf61` on the treatment's.
    - Only criterion 3's comparison and its printed fields change. This was read in the diff.
    - Criterion 4's same-row clause stays two-sided, at 5 ms.
    - Criterion 5's "no segment moves" is held through criterion 3's exemption after the span, not
      by a comparison of its own (read in the scorer). So the change reaches it only as text.
    - `qualify5d_test.py` blob `0f1063f` passes 25 of 25 on both branches and in the run directory
      (executed). Three of its cases are new: a value 120 ms below the baselines passes and is
      printed below its lower reference; a value exactly at the bound passes; a value 0.5 ms above it
      fails.
- **(b) The three baselines are interleaved through each build's block,** so that criterion 3's band
  and criterion 2's median span the step.
  - The order: `base-1`, `mention`, `reconcile`, `buddy`, `hidden`, `beforeinsert`, `insert`,
    `base-2`, `follow`, `build`, `gap`, `app`, `stall` with `arg=8000`, then `base-3`.
  - The baselines sit at positions 1, 8 and 14.
  - The excluded launch comes first, then the control's 14, then 5d's 14.
- **Criterion 2 under (b).** The rule does not change: a rise of at least 100 ms over the baselines'
  median, now the median of positions 1, 8 and 14.
  - On 2026-11-04 the control's `insert` rose 32.4 ms and `build` 73.4 ms, over three baselines that
    all lay inside the step.
  - **Stated in advance:** if `insert`'s `timeline_insert`, or `build`'s view build, still rises less
    than 100 ms against the interleaved median, that is a real gap in the instrument for the
    control's two longest phases, and not a criterion to loosen. The control then stays NOT
    QUALIFIED on criterion 2, and the result is reported as that gap.
- **Unchanged.**
  - The builds, `2e72e42` and `550b414`, with their sha256.
  - Every other script blob, and criteria 1, 4 and 5.
  - The uniformity rule (decision 5, applied by *Order of the launches*, item 1).
  - Battery saver off, on USB power, with one refresh rate held.
  - The excluded launch's pages on `c8cb0e91` with 400 rows.
  - The stop on a recorded wait on an `IGNORED_EXPIRED` row.
- **The predictions it commits to.**
  - **P1, decisive, and the one (a) rests on.** On page 1's history rows the interleaved baselines are
    not systematically the highest. It holds when the exact p, as `requal_p1.py` blob `916b1ee`
    computes it, exceeds 0.05 on `timeline_insert` (control) and on dedup (5d).
    - A step tied to place in the order is expected to pass P1 under (b), since only `base-1` would
      lie inside it.
    - A difference tied to being a baseline fails it, whether from the flag or from anything else
      only the baselines share.
    - If P1 fails on a build, (a)'s justification falls for that build. That build is reported NOT
      QUALIFIED, whatever `qualify5d.py` computes.
    - Run on the 2026-11-04 launches, the script gives p 0.0055 and 0.0302, the same as item 1 (a
      check of the script, executed; it computes no criterion).
  - **P2, recorded, deciding nothing.** The first baseline is the highest of the three. On
    2026-11-04 it was not, on either build: `base-2` was.
  - **P3.** One-sided criterion 3 passes on every injection of both builds.
  - **P4.** Criterion 2 passes on all ten injections it applies to, on both builds, the control's
    `insert` and `build` included.
  - **P5.** Criteria 1, 4 and 5 pass on both builds.
  - **The expected verdict:** both builds QUALIFIED.
    - Any failure is reported with its numbers, and no criterion is adjusted.
    - A failed qualification stops the round before the measurement.
- **The run directory is ready, and no launch has run.**
  - `%USERPROFILE%\tmc-5d-round\C` holds `qualify5d.py` `19d110c`, `qualify5d_test.py` `0f1063f` and
    `requal_p1.py` `916b1ee`, each checked by blob.
  - The replaced two were copied aside first, as `qualify5d.py.41ac728` and
    `qualify5d_test.py.fff69a6`.

**3. ρ's provenance** was recorded before the 2026-11-04 sitting, in *The provenance of ρ* (`1e13f54`,
with its scope corrected in `b1a3371`). It is labelled there now as a pre-registered limitation,
binding before the closing measurement, not before the re-qualification. ρ does not change, and the
thresholds stay 0.708 and 0.825.
- **2.4 s and 4.0 s were measured.** They are the span and the burst end of three launches of
  2026-10-09 with #78's fix: 2490, 2348 and 2397 ms, and 4245, 3991 and 3939 ms.
  - The burst end came from a detector never qualified.
  - Their build is `a091809` by inference.
  - The parse's code path was in that build. Whether the parse ran inside those spans is not known.
- **1.0 s and 2.6 s are targets:** each is its baseline less about 1.4 s of view building. So
  whatever the baseline held, the target holds too.
- **The limitation:** if 2.4 s and 4.0 s held the parse, the thresholds are easier to pass than
  intended, and a HOLDS verdict carries that confound. A FAILS verdict stays interpretable. If they
  held none, there is no asymmetry.

**4. Three small things.**
- **a. The last row of the JIT CPU table was the spans, in ms of wall time, not JIT CPU.** The row is
  "Span range 2117.7–2405.0 / 637.0–882.8", in the agent's report of the sitting.
  - The JIT thread's CPU was 750 to 1090 ms on the control and 600 to 820 ms on 5d.
  - This plan's own text (*Beside the verdicts*) names those ranges as spans.
  - The published `tables.md` has no such row. Its tables end with `Sc-stall` (JIT 1070.0 ms, span
    2117.7 ms) and `S5-stall-r1` (820.0 and 730.6).
- **b. 5d's `follow`: recorded as an open item, not diagnosed** (*Backlog*).
- **c. The driver now reads `launchcheck.py`'s exit status 1 as "void", and re-runs a curable void in
  place.**
  - During the sitting the fix had accepted exit statuses 0 and 1, but the driver still stopped at
    every void, so a sitting could not run unattended.
  - Now a void launch whose every reason a re-run can cure is re-run in its place, as `-r1`, `-r2`
    and `-r3`. Those reasons are a late delivery whose waits are on no `IGNORED_EXPIRED` row, a
    foreign process start, and input events. A fourth void stops the sitting.
  - Any other void reason stops it, as do a page off `c8cb0e91` or 400 rows, and a recorded wait on
    an `IGNORED_EXPIRED` row, which is reported as a contradiction.
  - The driver is outside the repository, on nbpromec1. Its labels are `Rc-*`, `R5-*` and
    `X-R-excluded`. `sitting.sh` has sha256 `c771e1b7…` and `sitting_line.py` `82276b62…`; all four
    files' full values are in the pre-registration.
  - **Executed:** `sitting_line.py` gave the expected result on 12 recorded launches of this round,
    including a contradiction.
  - **Not executed:** the re-run loop itself has not run, since that needs the phone.
  - The report helper reads the new criterion 3 fields and the sitting's labels. With the old labels
    it reproduced 2026-11-04's published `tables.md` line for line, 201 lines (executed).

**5. A declared confound for the closing measurement: the pages' order.** It is named here, not
corrected for.
- **The owner's statement:** the pages ran sequentially in 13 of the control's 14 counted launches
  and in 10 of 5d's 14, and interleaved in 1 and 4. Before the fix they interleaved in 14 of 14.
- **A correction to the "before",** read in the records and recomputed: all 14 two-page records
  before the fix ran one build, `cc4c0cf`, the pre-fix control (APK `8b57a634…`), and every one
  recorded 1 to 3 waits.
  - So the "before" holds no 5d launch at all.
  - On the control it is confounded with the waits: whether the fix changed the order, or the waits
    did, cannot be read from them.
  - After the fix, all four late deliveries of 2026-11-04 interleaved as well.
  - The first record before the fix holds one page, so there are 15 in all.
- **Why it is a confound, as reasoning, not shown.** The pages' order decides at what process age
  each row runs, and a row's cost falls with process age (item 1). An order that differs between the
  arms is therefore a cost that differs between them, which the closing measurement would attribute
  to the arm.

**Next:** one re-qualification sitting under this design, on the phone. Nothing else runs before it.

### After the re-qualification, 2026-11-04: the negative control's pre-registration, ρ's provenance re-read, five findings

Asked by the owner of the measurement after *The re-qualification sitting, 2026-11-04* (in *Recorded
results*).
- Item 1 was written and hashed before any launch of the negative control.
- Items 2 and 3 were written after it ran, from the records and commits they cite. They decide
  nothing.

**What stands, in the owner's words:** "NOT QUALIFIED stands. Criterion 3 is NOT adjusted — not its
bound, not its margin, not its baseline count." Neither sitting of 2026-11-04 is re-scored.

**1. The negative control on criterion 3, pre-registered.**
- **The file:** `experiment/startup-5d/negative-control-prereg-2026-11-04.md`, sha256
  `52e96461b0d1f9d461a07cdfb16552d0668a7ade9f1f45d46338e1c7ffb15165`.
  - Written on 2026-11-04 at 11:57:59 +0100, and hashed at 11:58:20, before any launch of the
    negative control.
  - Committed in `4b7fa55` on `experiment/startup-5d-control` and `467607d` on
    `experiment/startup-5d-treatment`. Both were read back from the remote at 12:01:28. The
    excluded launch began at 12:05:47.
  - Beside it are its scorer `negctl.py` (blob `0100391`) and `negctl_test.py` (`72bbd6f`, 28 of
    28 checks, on synthetic inputs only).
- **The question:** does criterion 3 fail on launches where no injected delay exists anywhere?
- **The launches:** 14 plain launches per build, with `full` and no injection. The excluded launch
  ran first, and the procedure is the re-qualification's.
- **The band: every band of three, not leave-one-out.**
  - Leave-one-out changes the baseline count: to 13, or to 2. At 2 a value exceeds the highest
    baseline with probability 1/3, not 1/4.
  - Criterion 3's count stays three, so the base rate carried is 1/4 per value, before the margin.
- **The phases: all of them.** The decisive statistic holds each plain launch the way each of the
  ten injections criterion 3 applied to was held, 7 to 9 segments, like for like.
- **The decisive statistic, P_null.**
  - It is the probability that a sitting shaped like the re-qualification shows at least 3 failing
    launches. Such a sitting scores ten launches per build.
  - 3 is the re-qualification's own count, 15% per launch.
  - It is taken over all 364 bands and every assignment of the ten held sets, with the two builds
    convolved.
- **The rule, with its figures:**
  - **P_null ≥ 0.05: CONSISTENT WITH NOISE.** Criterion 3 cannot discriminate leakage from noise at
    three baselines.
  - **P_null < 0.05: TIED TO THE INJECTION.** The three failures are taken as real leakage, and the
    next step is to diagnose them, not to measure anything.
  - **A section that does not hold: VOID.**
  - **For orientation only, outside the rule:** for independent launches, 0.05 corresponds to a
    per-launch false-failure probability of about 4.2%. P(Bin(20, 0.042) ≥ 3) = 0.0495.
- **Why the before-margin rate does not decide:** averaged over every band, it is 1/4 identically.
  - The re-qualification's 21% and 18% are that quantity, and show no excess.
  - They cannot show whether the 10 ms margin keeps noise out.
- **The prediction it committed to:** CONSISTENT WITH NOISE.
- Its result is in *Recorded results*, under *The negative control, 2026-11-04*.

**2. ρ's provenance, re-read on 2026-11-04.**
- **Where it already stood:**
  - The owner's instruction gave it as still outstanding from the previous step.
  - It had been recorded in *The provenance of ρ* (`1e13f54`), and labelled a pre-registered
    limitation by `3c3bb98` (*Decided on 2026-11-04*, item 3).
  - It was re-read today against its sources. Everything below was read in the commits; nothing was
    run.
- **The line in `decide.py`:**
  - `RHO = {"span": 1.0 / 2.4, "burst_end": 2.6 / 4.0}` is line 77 of blob `3f69c6a`, which
    `2e72e42` and `550b414` both carry.
  - Line 8 says ρ "is not re-derived from what the control gives".
  - In `301111b` (2026-10-30 11:41), blob `f2eb343`, it is line 32.
- **The source of the four numbers:** *The prediction, handed over, verbatim*, written by `5daebe6`
  on 2026-10-30 at 11:52.
  - The prediction is its first paragraph.
  - The measured baseline is its second.
- **One correction.**
  - *The provenance of ρ* says that the 1.4 s of view building "was added by `5bdd999` the same day".
  - `git blame` gives the lines that state it to `5daebe6`: "about 1.0 s came from removing about
    1.4 s spent building 800 views of which about 15 are visible".
  - `5bdd999`'s diff changes no line that holds 1.4 s.
  - So the 1.4 s came in with the prediction, in the same commit. No other statement of that section
    rests on which commit it was.
- **The build behind the measured numbers, re-read.**
  - `a091809` is six probe commits on `488cac5`, the head of #78, which merged as `4804609`.
  - `a091809` is dated 2026-10-08, and the launches 2026-10-09.
  - That those launches ran it stays inferred.
- **The parse path in that build, re-read.**
  - `SmartCatchSpawnCoordinator.kt`, `SmartCatchSpawnIngestion.kt` and `PokemonTypeDex.kt` are blobs
    `7b90263`, `fc4ed60` and `6fdff03` in `a091809`, in `ccbdab2` and in `c8a9f99`.
  - `resolvePokemon` is called at the coordinator's line 215.
  - In `a091809`, `ChatFragment.kt` calls `ingestHistorySpawn` at lines 2951 and 2988.

| Number | Measured or target | From | Held the catalogue parse? |
|---|---|---|---|
| 2.4 s | measured, rounded | the span of three launches of 2026-10-09 with #78's fix, 2490, 2348 and 2397 ms; their build is `a091809`, by inference | possible, not known: the path was in the build, and nothing records whether it ran inside those spans |
| 4.0 s | measured, rounded | the burst end of the same three launches, 4245, 3991 and 3939 ms, from a detector never qualified | possible, not known, as for 2.4 s |
| 1.0 s | target | the prediction for 5d's span: 2.4 s less about 1.4 s of view building | not a measurement; it is 2.4 s less a constant, so it holds whatever 2.4 s held |
| 2.6 s | target | the prediction for 5d's burst end; that it is 4.0 s less the same 1.4 s is inferred from the arithmetic | not a measurement; it holds whatever 4.0 s held |

**The pre-registered limitation of the closing measurement, binding before it.** ρ does not change,
and the thresholds stay 0.708 and 0.825.
- **L1.** The two measured numbers behind ρ, 2.4 s and 4.0 s, may have contained the catalogue
  parse. Whether they did is not known, and nothing in this repository can settle it.
- **L2. If they did, the thresholds are easier to pass than intended, never harder.**
  - Both builds of the closing measurement carry the gate.
  - A launch in which the parse pauses a page for 300 ms or more is void.
  - If 2.4 s and 4.0 s held a parse cost k, the ratio the prediction's own model expects for these
    builds is below ρ: t/c − (t − k)/(c − k) = k(c − t)/(c(c − k)), which is positive.
- **L3. What the closing measurement's report carries beside its verdict:**
  - **HOLDS:** that it was reached against thresholds which may be easier than intended, because
    ρ's baseline may have held a parse cost these builds no longer pay;
  - **FAILS:** nothing further. It was reached against thresholds the confound could only have made
    easier;
  - **INCONCLUSIVE:** nothing further. It reads the same either way.
- **L4.** If 2.4 s and 4.0 s held no parse, there is no asymmetry. The report claims neither case.

**Beside L1 to L4, added 2026-11-05 after the closing measurement: a bound on the confound, not a
change to it.** By the instruction of the owner of the measurement. The four items above stay as
they were written. ρ does not change, the thresholds stay 0.708 and 0.825, both verdicts stand, and
both HOLDS stay weak.
- **Computed by the agent** from the 25 unrounded pairings of each quantity in the closing
  measurement's `decide.json`. The owner's figures were checked against the result, not copied, and
  they agree: the span cannot fall below 0.5 and its highest ratio is 0.3537, the burst end flips at
  about 2.11 s, the parse measured 0.9 to 1.5 s, and the longest recorded wait is 1516.1 ms.
- **The model.** Suppose 2.4 s and 4.0 s held a parse cost k that these builds no longer pay. The
  threshold the prediction's own model gives is then T(k) = (1 + (t − k)/(c − k)) / 2, with
  (t, c) = (1.0, 2.4) for the span and (2.6, 4.0) for the burst end.
  - **Its valid range is 0 ≤ k < t.** k is a cost, so it is not negative. t − k is the prediction for
    5d with the cost removed, so it must stay positive.
  - **T falls as k grows.** Its derivative is (t − c) / (2(c − k)²), which is negative because t < c.
  - **At k = 0** it is (1 + ρ)/2, the thresholds as fixed: 0.70833 and 0.825.
  - **As k approaches t, T approaches 0.5.** At k = t the prediction for 5d is zero.
  - **Above t the model says nothing.** t − k is negative there, so the model predicts a negative
    duration. At k = c it is undefined.
  - So within the range neither threshold reaches 0.5.
- **Where each threshold meets the highest observed ratio r.** Solving T(k) = r with r′ = 2r − 1
  gives k = (t − r′c) / (1 − r′).
  - **Span.** The highest of the 25 ratios is 0.353698, `M5-2` over `Mc-2`; the lowest is 0.283467.
    - The solution is k = 1.317 s. That is above t = 1.0 s, outside the range: the predicted 5d span
      there would be −0.317 s.
    - Within the range the threshold stays above 0.5, and all 25 ratios are below 0.5.
    - **No k in the model's range changes the span's verdict.**
  - **Burst end.** The highest ratio is 0.628798, `M5-2` over `Mc-5`; the lowest is 0.464073.
    - The threshold meets the highest ratio at k = 2.114 s, which is inside the range.
    - For every k below 2.114 s, all 25 ratios stay below the threshold, and HOLDS stands.
    - From 2.114 s up to 2.6 s, the highest ratio is not below the threshold. The threshold stays above
      0.5 and the lowest ratio is below 0.5, so the 25 fall on both sides: INCONCLUSIVE. 18 of the 25
      are 0.5 or above.
  - **No k in the range makes either quantity FAIL.**
- **Against what the records hold.**
  - **The longest recorded wait is 1516.1 ms:** `-13` of 2026-11-03, page 0, row 141, outcome
    `ignored_expired`.
    - It is the longest of the 50 waits in the 127 probe records in the run directory on `nbpromec1`
      (`waitRows`), read for this bound.
    - Records not held there were not read. The records of 2026-11-02's 28 launches are on
      `DESKTOP-BCT68L9`, as *A launch with a recorded wait is void and is re-run* states. *Criterion
      3's failures, diagnosed from the 28 launches' own records* gives their 10 late deliveries as
      resuming 0.9 to 1.4 s later.
  - **The parse's measured range is 0.92 to 1.52 s.**
    - That is the longest wait of each of the 15 excluded launches of 2026-11-03, 921.8 to 1516.1 ms.
      *The catalogue-parse mechanism, tested against the fifteen records* attributes these waits to
      the parse.
    - Its clause c held in 14 of the 15. The exception, `-2`, is the 921.8 ms; without it the range is
      1065.0 to 1516.1 ms.
    - These are gaps between history rows on the main thread, not the parse timed on its own thread.
  - **The burst end's flip point, 2.114 s, is 598 ms above the longest recorded wait,** 1.39 times
    it.
  - **The span's range ends at 1.0 s, below most of the measured parse.** The model assumes the 1.0 s
    target carries whatever 2.4 s held. A parse of 1.0 s or more cannot have been in 2.4 s and carried
    into a 1.0 s target. For such a k the model does not describe the span; it does not predict a
    flip.
- **What the bound is, and is not.**
  - **It is the model's own flip point, set against the longest wait any record read here holds.**
    The comparison between them is inferred, not measured:
    - k is what a parse added to the spans and burst ends of 2026-10-09, whose records are not in this
      repository;
    - a wait measured on the main thread need not have added its own length to a burst end, more or
      less (*Did the span or the burst end contain the catalogue parse?*).
  - **It does not say whether 2.4 s and 4.0 s held the parse.** L1 stands: that is not known.
  - **Both HOLDS stay weak,** as L3 and section 2 of the pre-registration have them.

**3. Five findings, recorded as the owner of the measurement gave them, deciding nothing.**
- **What they were checked against:** the re-qualification's `qualify-R-control.txt`,
  `qualify-R-5d.txt` and `location.txt`. These are published in
  `experiment/startup-5d/requalification-2026-11-04/` as `control.json`, `5d.json` and
  `location.txt`.
- Where the instruction's wording and the records differ, the records are given.

**a. Criteria 1, 2, 4 and 5 passed wherever they apply, on both builds.**
- **Criterion 1 passed on all 11 launches of each build.**
  - The ten 200 ms delays were recorded at 200.11 to 200.18 ms on the control, and at 200.10 to
    200.15 ms on 5d.
  - `stall`'s were 534.59 and 535.03 ms.
- **Criterion 2 passed on the 10 launches of each build that carry it.** `stall` has no criterion 2.
  - Every injected phase rose by at least 100 ms.
  - The rises were 129.8 to 213.0 ms on the control, and 140.7 to 211.0 ms on 5d.
- **Criterion 4 passed on all 11 launches of each build.**
  - The delay lay inside its phase on the ten launches with that clause. `stall` has none.
  - It lay inside one main-thread message (`inside_the_main_threads_log`) on the ten launches with
    that clause. `app` has none by construction: `qualify5d.py` applies it only `if name != "app"`.
  - So both clauses held together on 9 of 11 per build, and `app` and `stall` carried one each.
- **Criterion 5, which only `stall` carries, passed: the burst ended inside its window.**
  - Control: at 8536.93 ms, in [8534.67, 8538.18].
  - 5d: at 8536.45 ms, in [8535.41, 8537.44].
- **The gap the pre-registration named did not occur.** The control's `insert` rose 213.0 ms and its
  `build` 129.8 ms, both at or above 100 ms.

**b. The failing magnitudes, against a 200 ms delay.**
- **Five values lay above their bound, on three launches:**
  - control `mention-r1`: +61.7 ms on `timeline_insert`, and +54.1 on its view build;
  - control `buddy`: +26.2 and +20.3, on the same two;
  - 5d `insert`: +5.4, on `follow_or_count_composer_restore`.
- Two of the five are nested: the view build is part of `timeline_insert`.
- **The 5d phase:**
  - It measured 21.8, 20.5 and 20.3 ms in the baselines, and 37.2 ms in `insert`.
  - 31.8 ms is its bound, the highest baseline plus 10, not the phase.
  - The excess is 5.4 ms over the bound, and 15.4 ms over the highest baseline.
- **Recorded, as the owner put it:**
  - No mechanism has been named by which a 200 ms sleep appears in another phase as a 5.4 ms excess
    over the bound of a phase of about 21 ms, or as the control's 20.3 to 61.7 ms.
  - Naming one is the burden of anybody claiming leakage.
  - It is not asserted to be impossible.

**c. The time-order exclusion on `mention-r1`.**
- **What it rests on:**
  - Of its `timeline_insert` excess over the highest baseline, +30.2 ms lies in page 0's rows 11 to
    99 (`location.txt`).
  - Those rows ran before row 100, where the delay is.
  - A delay cannot lengthen work that ended before it began, so that part is not the delay's leakage.
- **It is reasoning, not a test:** nothing measured it.
- **It covers that part only.** The +64.4 ms on rows 101 to 399 came after the delay, and the
  exclusion says nothing about it.
- **The owner's assessment, recorded:** it is a stronger form of leakage test than a band, because
  it is logical rather than statistical.
- It is not promoted to a criterion in this step.

**d. The pages' order did not differ between the arms in the re-qualification.**
- They ran one after the other in 14 of 14 counted launches of each build. This was read again in
  that sitting's `three-quantities.jsonl`.
- All three of its void launches interleaved.
- So the confound declared in item 5 of *Decided on 2026-11-04* did not appear on that sitting.
- On the negative control's launches, the order did differ between the arms (*The negative control,
  2026-11-04*).

**e. The owner's bound, in the owner's words:** "this negative control is the last instrument sitting
before the release decision. If the qualification does not close after it, the measurement is
recorded as NOT TAKEN, #84's deferral stays unsatisfied and is written as such, and 5d is judged on
its correctness gates alone."
- It is recorded, not applied.
- Whether the qualification closes is not decided in this step.

### Before the closing measurement, 2026-11-04: its pre-registration, the idle record, the 60 Hz readings

By the instruction of the owner of the measurement, after the negative control. The owner's decision
of this step: "The instrument qualifies for the closing measurement's two quantities and NOT for
per-phase attribution." ρ does not change, the thresholds stay 0.708 and 0.825, and the decision
rule stays as `decide.py` has it.

**1. The pre-registration of the closing measurement, before any of its launches.**
- **The file:** `experiment/startup-5d/closing-measurement-prereg-2026-11-04.md`, sha256
  `0dbec9044ea7531fd7bfe26af688037c9b89b3332143d1cede0d85673925b88f`, blob `b6d2e81`.
  - Written on 2026-11-04 at 13:59:24 +0100, and hashed at 13:59:47, before any launch of the
    closing measurement.
  - Committed in `72dedf8` on `experiment/startup-5d-control` and `1f13d5f` on
    `experiment/startup-5d-treatment`.
  - Both were read back from the remote at 14:00:28. The file fetched from the remote gave the same
    sha256.
- **What it fixes:**
  - **The scope:** qualified for the span and the burst end, on criteria 1, 2, 4 and 5.
    - Criterion 3 is UNINFORMATIVE by measurement (P_null 0.6003).
    - Per-phase attribution is NOT QUALIFIED and out of scope for this round.
    - This is a narrowing, not a loosening. Criterion 3 neither blocks nor clears, and the leakage
      question is unanswered.
  - **The direction, corrected:** (t − k) / (c − k) < t / c. So HOLDS is the weak verdict, and
    FAILS would be the strong one.
    - The owner's retraction of the opposite direction is recorded with its reason, in the owner's
      words: "a direction asserted from algebra that had not been carried out".
  - **The confounds, live:**
    - JIT, with the control flattered;
    - `run-from-apk` in every launch;
    - L1 to L4 on ρ's provenance;
    - the pages' order.
  - **The rule** as `decide.py` `3f69c6a` applies it, with its stopping rule.
  - **The agent's prediction:** HOLDS on the span and HOLDS on the burst end. It rests on the
    negative control's plain launches, on the qualification builds:
    - span ratios ran from 0.243 to 0.394, against 0.708;
    - burst-end ratios ran from 0.243 to 0.740, against 0.825.
  - **The procedure:**
    - the measurement builds `dea38cc` (`d005e901…`) and `a2f12f1` (`ece097ac…`), run with the run
      directory's host scripts;
    - one excluded launch, then C T C T C T C T C T, with an install before every launch;
    - `--qualified` names the 84 counted launches of 2026-11-04's three sittings.
  - **The driver's sha256.**

**2. The idle wait is now shown and saved on every launch.** This is the owner's fix, made before
the pre-registration was hashed.
- **What was missing:**
  - In the negative control, the first four launches' idle lines were neither shown nor saved. So
    whether the driver waited for input there is not known.
  - "Untouched" is a void condition, and a wait that was not recorded cannot be verified.
- **What changed:**
  - `idle-wait.sh` prints every line of the wait and appends it to a record, with the driver's display
    reading.
  - `launch-one.sh` copies that record into the run, as `idle-wait.txt`.
  - `sitting.sh` stops if a launch saved none, and prints the record from the saved file.
- **Tested without a launch:** against a stand-in `adb`, what was shown was byte for byte what was
  saved, in both branches. The sha256 values and the test are in the pre-registration, section 6.

**3. The 60 Hz readings: a mechanism observed later, which plausibly explains an older open item.**
- **Observed before the negative control, 2026-11-04, 12:01 to 12:05.**
  - The display read 60.000004 Hz while another app had requested that rate.
  - At 12:03 the display's votes held three app requests capped at 60.000004 Hz. Thermal status was
    0, and battery saver was off.
  - The other app's window was listed. By 12:04:57 its task was not visible.
  - From 12:04:32 the display read 90.0 Hz.
- **The older item.** On 2026-11-03, the excluded launch's re-run `-2` read 60.000004 Hz before and
  90.0 after, and its cause was recorded as unknown (*Recorded results*, 2026-11-03).
- **What the later mechanism offers it.** Another app's request for 60 Hz, lifted once that app left
  the screen, would give exactly that reading, a lower rate before the launch and 90.0 after.
- **It is a plausible explanation, not proof about `-2`.**
  - At `-2` neither the display's votes nor the app in front before the launch were read.
  - So the mechanism was observed on 2026-11-04, and its presence at `-2` is inferred.
- **It is not the 60 Hz of 2026-11-03 08:52.** That reading moved to 90.0 when battery saver was
  turned off, and the inference there was battery saver's cap.
- **From the closing measurement on,** `section-read.sh` reads the display's app-request votes beside
  the refresh rate, before the first launch and after the last.
- *Withdrawn on 2026-11-04, at about 14:02, before the closing measurement's first launch:* the votes
  this item rests on were on displays 2 to 4, not on the phone's screen. So it does not show an app
  capping the screen at 60 Hz, and `-2`'s cause stays unknown (*The closing measurement,
  2026-11-04*). The item above stays as it was written.

## Backlog

Started on 2026-11-03: this plan had no backlog before the entry below. An entry here is recorded,
not classified. Nothing here is fixed, and nothing is called a defect, until what the entry names
has been read.

- **`CurrentSpawnStore.load` returns a future-dated stored spawn with no skew limit.** At
  `CurrentSpawnStore.kt:77` a negative age returns the snapshot, while `decide` rejects an incoming
  spawn dated more than 5 s ahead.
  - **Recorded by the instruction of the owner of the measurement, 2026-11-03:** before classifying
    it, list every caller and say which one trusts the snapshot without re-testing the window. Do
    not fix it now.
  - **The callers.** These are every reference to `CurrentSpawnStore.load` on `main-v5` at
    `ed41de1`, read in the code and not run:
    1. **`SmartCatchSpawnIngestion.kt:67`, the coordinator's `loadCurrentSpawn`: it re-tests.**
       - The snapshot reaches `decide` only as an identity and a time.
       - Before comparing, `decide` drops a stored time that is more than 5 s ahead or more than
         90 s old (`SmartCatchSpawnCoordinator.kt`, lines 103 to 107). It then stores the incoming
         spawn as if nothing were stored.
    2. **`ChatFragment.kt:3971`, `currentSpawnSnapshot()`: it trusts the snapshot without
       re-testing.**
       - It is called when the Quick Catch menu opens (line 3762) and when the menu refreshes (line
         3941).
       - The expiry was tested in `load`. The skew is tested nowhere on this path.
       - `QuickCatchMenuModelFactory.build` hands the snapshot to three readers, and none of them
         tests the skew:
         - `CatchBallRecommender.spawnAgeSec` (lines 8 to 13) returns 0 for a negative age. The
           spawn therefore counts as just seen, and falls in the Quick Ball window of 0 to 15 s.
         - `currentSpawnRemainingSec` (`QuickCatchPanelModel.kt`, lines 108 to 115) coerces the age
           to at least 0, so the header shows 90 s remaining.
         - `QuickCatchProfileSpawnContextProvider.load` reads no time.
  - **Beside it: reasoning, not a classification, and not tested.**
    - The only writer is `CurrentSpawnStore.save`, called from `SmartCatchSpawnIngestion.kt:73`.
    - It stores the time `decide` clamped to the wall clock at the write (`coerceAtMost(nowMs)`).
    - So a stored time ahead of now needs the wall clock to have moved back since the write.
- **5d's `follow` on 2026-11-04: its largest gap below 300 ms is not where its delay was, and its
  dedup is among the three lowest.**
  - **Recorded by the instruction of the owner of the measurement, 2026-11-04:** record it as an open
    item, and do not diagnose it now.
  - **The delay** was 200.1 ms, at 1720.4 to 1920.6 ms after the anchor. It lay inside page 0's row
    100, in its `follow_or_count_composer_restore`, and inside one main-thread message, 1720.1 to
    1920.7 ms. Criterion 4 passed.
  - **The largest gaps below 300 ms between consecutive history rows:**
    - page 0: 217.5 ms, before row 208 (2021.2 to 2238.7 ms);
    - page 1: 206.4 ms, before row 146 (2033.0 to 2239.5 ms).
    - The pages interleaved, page 1 starting 381.8 ms after page 0.
  - **Its dedup is the lowest of the 14.** Over the span it was 97.4 ms. The next two were
    `beforeinsert`, 97.9, and `build`, 133.4, then `hidden`, 134.8. `mention`'s dedup holds its
    own delay.
    - On page 1 alone the same three are the lowest: 26.6 (`beforeinsert`), 44.9 (`build`) and 46.9
      (`follow`).
  - These were read from the published outputs and recomputed from the run's records. Nothing here
    is interpreted.

## Recorded results

Fill in each row when its case runs; add a run rather than overwriting one.

**The device, read on 2026-11-02 at the start of the round:** `adb devices -l` gave
`57ebfa06 device product:Nord_EEA model:AC2003 device:Nord transport_id:1`, already authorised for
`DESKTOP-BCT68L9`: no prompt was needed. `getprop` gave OnePlus, Android 12, API 31, build
`AC2003_11_F.24`; `wm density` 382 (physical 450), `wm size` 1080 × 2400, system font scale 1.15,
`stay_on_while_plugged_in` 7, battery 24.9 °C. Its dev app before the round was `d56812b`, sha256
`8519b894…`, last updated 2026-10-23 09:23, as `RELEASE-STATE.md` records it, read from the APK pulled
off the phone.

**The display, read on 2026-11-02 before 11:50:** active mode 60.000004 fps, of the two the phone has
(60 and 90); `peak_refresh_rate` 60.000004; refresh range [0, 60]; `debug.choreographer.skipwarning`
unset, so 30. What it means for criterion 5 and for the burst end is under *Qualification*.

**Harness stages,** every pass with 0 input events unless the row says otherwise:

| Stage | Commit | Dev APK sha256 | Day and night, each | Time |
|---|---|---|---|---|
| H1, first run | `fc63adb` | `1d377b6e…` | the same counts as the next row, in both themes; void: its day pass recorded 46 input events, the phone was in use | 09:33 |
| H1 | `fc63adb` | `1d377b6e…` | parity 34 / 34, `rowsBeforeHiding` 1 / 1; controls behaved; emote layer 2 / 2, with a 404 2 / 2, its controls behaved; pairs 658 / 658, `sessionsHeld` 658, `imagesHeld` 658, not vacuous; C-R 34 / 34, no declared exception; preconditions 5 / 5; lifecycle 16 / 16; list checks 121, 0 failures. **Not as predicted:** direct bind pixels 9 / 9, **state 0 / 9, declared exceptions met 0**: every echo's message view has 4 spans against the reference's 3, and `echo.unconfirmed-confirmed` has the declared text difference as well; **recycler 4 / 8**: R1, R5 and R7 fail only `pixelsEqualTheReference`, R2 only `noPoolRecycle` (scrolled 201 px); **scroll 2 / 3**: S1 fails only `theLayoutReportedAStillScrollWhileTheNewestRowHadNoView` (`stillReports` 1). Day and night failed the same checks. The round stops here until this is understood; H2 has not started | 09:35–09:36 |
| H1, first run | `d11b629` | `41a9da8b…` | **night:** every count of *Proves it*, and every value pre-registered in `84397ba`, as the next row gives them. **Day void:** 786 input events over about four and a half minutes, the phone in use, and it wrote no result | 11:51–11:58 |
| H1, day again | `d11b629` | `41a9da8b…` | the day pass alone, at the owner's instruction, on the same install, its hash read back and not reinstalled: parity 34 / 34, `rowsBeforeHiding` 1 / 1; controls behaved; emote layer 2 / 2, with a 404 2 / 2, its controls behaved; pairs 658 / 658, `sessionsHeld` 658, `imagesHeld` 658, not vacuous; C-R 34 / 34, no declared exception; direct bind pixels 9 / 9, state 8 / 9, the declared exception met exactly (`/children[1]/text`), `layoutsAddedOnlyTheWatcher` 9; preconditions 5 / 5; lifecycle 16 / 16; recycler 8 / 8, R2 `positionsSentToThePool` [0] at 201 px, R1, R5 and R7 `rowLayoutPendingAfterDelivery` true, heights 52 then 68, both bitmaps 1022 × 68, R3's the same; scroll 3 / 3, `stillReports` 1 with the newest row viewless (`["none"]`); list checks 124, 0 failures; no pass aborted and no step recorded an error, so `Settled` refused nothing. **As predicted, in both themes** | 12:00 |
| H1, S3's landing check | `b954eba` | `27e6fa79…` | every count of *Proves it* and every value pre-registered in `84397ba`, as on `d11b629`, in both themes; and S3's `oneRowWentAboveIt` passed, the first visible row's position 14 then 15, its top at −35 px before and after, as `1ed7915` pre-registered. **5d keeps the visible rows in place when one row is inserted above them: established in the harness, no longer reasoned** | 14:32–14:33 |

**H1 on `d11b629` holds, as pre-registered.** Day and night agree on every value. R7's heights, the one
value left unpredicted, are 52 and then 68. The stage's two passes come from two runs of it, about
two minutes apart on one install (night done at 11:58:22, day at 12:00:31): the night pass of the
first and the day pass of the second. The 09:33 void had both themes re-run; this one re-ran the day
alone, as the owner instructed. H2 has
not started.

**The qualification sitting, 2026-11-02, written before its first counted launch.**

- **The control's qualification build installed:** `03a8775`, sha256 `a3c3ca07…`, read back from the
  phone at 14:36:07. The scripts run from `C:\Users\Fede\StudioProjects\tmc-5d-round\C`, extracted
  from `0cba63c` and checked file by file against it, so that no run directory lands in a clone.
- **The account, identified by one launch excluded from every count.** Account ids are random
  identifiers held only in the encrypted account store, which is not read. So one cold start of the
  control's qualification build with `full` and no account extra (`C-identify-accounts`, 14:37) was
  run to learn them. Only these were read from its record: the account ids, and each page's channel
  fingerprint, history count and current flag. Not one timing was read.

  *Withdrawn on 2026-11-03:* "held only in the encrypted account store" was wrong. The ids are also
  the names of the `recent_channels_<id>` keys in `shared_prefs/channel_history.xml`
  (`ChannelHistoryStore`), and that is the owner's documented route to them. No file of this
  repository states the route; the code does use that key. `no_backup/accounts_v1` is encrypted with
  a Keystore key, holds access tokens, and is never read. The dev build is debuggable, so `run-as`
  reaches the file without a launch. So no identifying launch was needed, and the round of
  2026-11-03 used none (*Recorded results*, 2026-11-03).
  - There are two accounts, `0d3d33b8…` and `57f6e8bc…`.
  - One page created its view in the window: `0d3d33b8…`'s, channel `c8cb0e91`, 400 history rows,
    the cap. The second slot stayed empty, so the method's "two chat pages" did not appear at a cold
    start.
  - **Chosen and held for every launch of the section: `0d3d33b8…`.** It is the only account whose
    page loaded, its channel is at the cap, and page 0's 100th history row is there for the
    injections.
  - Whether the second page appears with the extra is read from the first baseline. If the
    baselines' rows in the span differ, another account is chosen before the decision launches, as
    *The account* says.
- **The display's rate changed before the section began, by no action of this sitting.**
  - At 11:47 it read 60.000004 Hz, with `peak_refresh_rate` 60.000004.
  - At 14:37 it read 90.0 Hz, with `peak_refresh_rate` 90.0 and the range [0, 90].
  - No command of this sitting changed a setting.

  The owner's binding requirement is that the rate is held and recorded, not which rate. So the
  section is held at the rate it begins at, 90.0 Hz, and is void if any reading differs. At 90 Hz,
  N = 333 ms; the 534 ms stall logs at either rate.
- **The phone was in use.** Two taps were recorded about 0.7 s before the identification launch
  started. So each launch first waits until the phone has recorded no input event for 20 s. A launch
  whose recorders catch any input event between its launch and its dump is void, and is run again.
  That is the harness's rule of 0 input events, applied to these launches and declared here before
  the first of them. A void launch is kept and recorded, never used.

**The qualification, 2026-11-02, 14:42 to 15:08: both builds NOT QUALIFIED.** The round stops here,
before the measurement, as *Qualification* says a failed qualification does.

- **The launches:** 28 counted, 14 per build, each with 0 input events and the held reading
  `refreshRate=90.0 frameRateOverride=[] skipwarning=[]` before and after. One more is void: the
  first `Qc-base-1`, at 14:40. It was launched without the account extra, by a fault of the driver,
  which passed a Git Bash path to Windows' Python, so the account came back empty. It is kept as
  `Qc-base-1-void-no-account-extra` and never used.
- **Rows:** 800 history rows in the span on every launch of both builds, both pages at the cap.
- **Both pages carry the same channel fingerprint, `c8cb0e91`.** The method's "two chat pages on
  different channels" is not met by this phone's two accounts. Recorded, not changed: an account's
  channel is the user's setting.
- **The untested seam:** the first baseline's facts, as `qualify5d.py` reads them, against its
  `probe.json` and `logcat.txt` read directly: 17 fields, 0 mismatches.
- **The control (`03a8775`):** criteria 1 and 2 pass on all eleven injections. Criterion 3 fails on
  nine: `reconcile`, `buddy`, `hidden`, `beforeinsert`, `insert`, `follow`, `build`, `gap` and `app`.
  Criterion 4 fails on `buddy` and `hidden`. `mention` and `stall` pass in full.
- **5d (`0fcd674`):** criteria 1 and 2 pass on all eleven. Criterion 3 fails on the same nine.
  Criterion 4 fails on `insert`. `mention` and `stall` pass in full.
- **The burst-end detector is qualified, for the first time in any round.**
  - Control: burst end 8536.8 ms, inside [8535.1, 8538.2].
  - 5d: 8535.1 ms, inside [8534.0, 8536.5].
  - On both builds, every baseline's burst ended before the stall, and the stall lay after the span.
- **Found by criterion 4, and established from the records: three delays went into a live chat
  row.** In the control's `buddy` and `hidden` and 5d's `insert`, the delay landed in a live row of
  page 0 (source 2), not in page 0's 100th history row. The cause is in the probe:
  - the row call sites pass `historyOrdinalOfCurrentRow()`, which is −1 for any row that is not one
    of page 0's history rows;
  - `inject` reads −1 as "this site has no ordinal to match";
  - so the first live row to reach the call site before the 100th history row takes the delay.

  Whether a live row gets there first depends on when live messages arrive. It is a defect of the
  probe. Criterion 2 alone passed those launches; criterion 4 caught them. It is not fixed here.
- **Not yet diagnosed: criterion 3's failures.**
  - They lie in segments the injection does not touch, by up to 27 ms on the control and 66 ms on
    5d.
  - They follow launch order rather than the injection. On 5d, `dedup_backfill_mention` is 132, 134
    and 132 ms in the three baselines, and 55 to 72 ms in the later launches. On the control it is
    92, 92 and 94 ms, and 118 to 132 ms later.
  - The criterion's band, the baselines' range widened by 10 ms, does not hold that drift. What
    moves it was not examined.
- **Also recorded, not diagnosed:** 5d's span rose by 700 to 1550 ms in seven of its injected
  launches, with idle rising by 480 to 960 ms, against baseline spans of 745 to 805 ms.
- **Published, every value `qualify5d.py` printed:** `experiment/startup-5d/qualification-2026-11-02/`
  on both experiment branches (`cf0e322`, `22f38c3`): both outputs, the hand-check, and each
  launch's line. The run directories stay in `C:\Users\Fede\StudioProjects\tmc-5d-round\C`, outside
  any clone. The phone holds the 5d qualification build, `0fcd674`, since 14:55:16.

**Criterion 3's failures, diagnosed from the 28 launches' own records** (2026-11-02, after the
qualification; nothing was launched). The scripts and their outputs are in
`C:\Users\Fede\StudioProjects\tmc-5d-round\C\diagnosis-criterion-3`, `drift.py` to `drift6.py`, with the
phone's reading of 20:22.

- **Not the live rows in the span.** There were 0 to 4 per launch, adding at most 1.2 ms to any
  segment, and the history rows alone show the same drift.
- **A row's cost falls with the process's age, the same way in every launch.** Median dedup per history
  row:
  - 5d, every launch but `stall`: 103–210 µs for rows beginning 1250 to 2250 ms after the anchor, and
    50–76 µs from 2750 ms;
  - the control's baselines: 138–174 µs, then 73–84 µs from 3000 ms.

  The build is uncompiled (`run-from-apk`, above). Around 5d's short spans its JIT thread used 730 to
  920 ms of CPU across brackets of 810 to 960 ms. That the curve is the JIT compiling during the rows
  is an inference.
- **In 10 launches the history was delivered late.** Both pages' rows stopped after the same number
  of rows and resumed together 0.9 to 1.4 s later. Meanwhile the main thread drew 14 to 118 frames,
  against 1 to 3 in every on-time launch. They came in two blocks:
  - the control, 14:46:12 to 14:47:59, `reconcile`, `buddy`, `hidden`: 175/174, 64/63, 9/9 rows
    before the wait;
  - 5d, 15:01:22 to 15:06:42, `hidden` to `app`: 187/186, 125/125, 97/96, 72/72, 61/60, 29/29, 4/4.

  Each block ends at the first launch after the count would pass zero. On 5d the count fell at about
  the live rate: 62, 28, 25, 11, 32 and 25 rows a step, against 51, 29, 41, 14, 28 and 38 from page
  0's live rate times the 53 s between launches. Both pages' responses had arrived and been parsed
  before every wait began (the `p0_history` and `p1_history` boundaries), so the wait is on the phone,
  not the network.
- **What it did to criterion 3.**
  - On 5d, the rows delivered after the wait ran on warm code and cost about half:
    `dedup_backfill_mention` fell from 132 to 55–72 ms, and the span rose by 0.7 to 1.6 s.
  - On the control, rows reach warm code in every launch, so the wait shows mostly as span, 0.35 to
    1.2 s more.

  This explains both bullets above that it was "not yet diagnosed".
- **Recorded as a candidate product defect, not a measurement artefact** (the owner's
  classification): a position in the cached history stalls delivery for about a second, for both
  pages, until live messages slide it out of the 400-row window. If it is real, users on a quiet
  channel would see it on every launch. A quiet channel is the setup recommended for this
  measurement, so that is an interaction, not a coincidence.

  Read statically, not established: each history message passes through the worker's loop, which
  calls the spawn ingestion before posting it, and the spawn store is app-wide, so both pages share
  it. Not chased: it does not hold the round.

  **Written down before anyone reads the stall as a performance bug** (the owner of the
  measurement). The lead above is read from the code, not established, and stays labelled that way.
  If it holds, the stall sits in a load-bearing path:
  - the Pokémon Community Game (PCG) probes' configuration is set the way it is for stable inventory
    and Pokédex reads, and is not changed without discussing reasons and consequences first;
  - the backfill feeds spawn detection, which is why loading fewer messages was never a free lever.

  So it is a candidate product defect whose likely location is a path that is not ours to optimise
  unilaterally. Identifying it is in scope: the history-wait reads are there for that. Changing it is
  a conversation with Federico about reasons and consequences, not a patch.
- **Open, recorded and not chased.** They become interesting if they survive a compiled state.
  - **The control's rise from `beforeinsert` on,** delivered on time and seen at the same process
    age. Dedup was 158–283 µs at 1500 to 2250 ms, against the baselines' 138–174, and 87–110 µs from
    3000 ms, against 73–84. Only dedup and follow rise.
  - **5d's `stall`,** delivered on time, about twice as slow at the same process age: 188–371 µs at
    1250 to 2250 ms. Two of its bins, 339 and 371, are above every other launch's.
  - **5d's `mention`, `reconcile` and `buddy`,** about 20% faster than its baselines at the same age:
    105–160 µs, against 137–210.

**The second sitting, 2026-11-03, on `nbpromec1`, read before any launch.**

- **The device:** `adb devices -l` gave `57ebfa06 device product:Nord_EEA model:AC2003 device:Nord
  transport_id:1`, already authorised for `nbpromec1`. The same phone: OnePlus AC2003, Android 12,
  API 31, build `AC2003_11_F.24`. `wm density` 382 (physical 450), `wm size` 1080 × 2400, system font
  scale 1.15, `stay_on_while_plugged_in` 7, `debug.choreographer.skipwarning` unset. These are the
  values of 2026-11-02.
- **The power source:** USB, charging (status 2), level 58, 25.5 °C, read at about 08:04.
- **Battery saver: ON.** The plan requires it off before the decision launches, and one state held
  across the section, qualification included (*The battery*). It reads `low_power` 1,
  `mSettingBatterySaverEnabled=true` with `mSettingAutomaticBatterySaver=0`, so it was not turned on
  by the level trigger. It is sticky, with auto-disable off, so charging will not turn it off. Not
  changed by this sitting: it is the owner's setting.
- **The display: 60.000004 Hz**, mode 1 of the two (60 and 90), with `peak_refresh_rate` 60.000004
  and SurfaceFlinger's range [0, 60]. The section of 2026-11-02 was held at 90.0 Hz. That battery
  saver caps the rate is an inference, not checked.
- **The dev app on the phone:** sha256 `5cc84bb3…`, `cc4c0cf` built on `DESKTOP-BCT68L9`, read from
  the APK pulled off the phone and from `adb shell sha256sum` on it. Its `lastUpdateTime` is
  2026-11-02 21:30:50. The record above has the phone on `0fcd674` from 14:55:16, and the install of
  21:30 is recorded nowhere: what made it, and whether a launch followed, is not known on this
  machine. `nbpromec1`'s builds replace it from the first install of the sitting. `run.sh` with
  `APK` set installs before every launch, so no separate step is needed.
- **The accounts, read at about 08:50 from the key names of `shared_prefs/channel_history.xml`**,
  through `run-as`, with no launch. Only the key names were printed; the values, the recent
  channels, were not. The file was last written 2026-11-02 16:06. It holds two keys,
  `recent_channels_57f6e8bc…` and `recent_channels_0d3d33b8…`. These are the two accounts that the
  launch of 2026-11-02 identified, each matching on its eight characters, with full ids of the
  standard 36-character form.
- **Battery saver off, read at 08:52:57**: `low_power` 0, `Battery Saver is currently: OFF`. The
  display then read 60.000004 Hz until 08:53:20 and 90.0 Hz from 08:53:25, with SurfaceFlinger's range
  going from [0, 60] to [0, 90]. It read 90.0 on all 14 readings from 08:53:25 to 08:54:47. **The
  section is held at 90.0 Hz.**
- **The excluded launch, `X-excluded-channel-check`, 08:57, excluded from every count.**
  - **How it ran:** `run.sh` from `%USERPROFILE%\tmc-5d-round\C`, extracted from `92fbb4a` and
    checked file by file against it, with `APK` set to `nbpromec1`'s `cc4c0cf` and the flag `full`.
    The account's id was read from the key names by the command that launched it.
  - **Before it:** the phone had shown input in four 20 s windows running, 238 to 1040 events each,
    and the launch waited for one with none.
  - **The install, read back:** `8b57a634…` on the phone, `lastUpdateTime` 2026-11-03 08:57:25. So
    `nbpromec1`'s builds replaced `DESKTOP-BCT68L9`'s from this launch on.
  - **What was read from its record**, and nothing else, no timing: the probe's commit, `cc4c0cf`;
    each page's channel fingerprint and history count; battery saver 0 and USB power, before and
    after; 90.0 Hz before and after; and 111 input events during the launch, so the phone was in
    use while it ran.
  - **Page 0:** `c8cb0e91`, 400 history rows. **Page 1:** `6341cf72`, **0 history rows**.
  - **Page 1 does not read 400, so the sitting does not start**, as *One excluded launch before any
    counted launch* requires. Page 1 is not on `c8cb0e91`, as it was on 2026-11-02, but on another
    channel. These reads cannot say whether its 0 means no cached history or a history not delivered
    in the window.
- **The excluded launch, rerun after Federico put page 1 back on `c8cb0e91`: fourteen launches,
  09:47 to 10:05, every one void.**
  - **How they ran:** the same driver and build as the first, `nbpromec1`'s `cc4c0cf`, each install
    read back as `8b57a634…`. Each was read through `launchcheck.py`, which was also run on the first
    launch.
  - **In every one of the fourteen:** both pages `c8cb0e91` with 400 history rows; 0 input events;
    battery saver 0 and USB power before and after.
  - **The voids:**
    - `-2`, 09:47: the display, 60.000004 Hz before and 90.0 after. Its frames, 3, passed. Why the
      reading was 60 is not known. Two minutes of readings with the app in front gave 90.0
      throughout, and `-3` read 90.0 before and after with the same app in front before its install.
      *Added on 2026-11-04:* a mechanism observed later plausibly explains it, another app's
      request for 60 Hz. It is not proof about `-2` (*Before the closing measurement, 2026-11-04*,
      item 3). The sentence above stays as it was written.
      *Withdrawn the same day:* the votes that note rested on were on other displays, and the cause
      stays unknown (*The closing measurement, 2026-11-04*).
    - `-3` to `-15`, 09:50 to 10:05: late delivery, 53 to 107 frames drawn inside the span; `-12`
      drew 10. `-10` also caught a foreign process start, `com.google.ar.core` at 2579.2 ms.

  | Launch | Frames in span | Waits caught: slot, row before which it waited, ms |
  |---|---|---|
  | first, 08:57 | 100 | 0, 96, 1198 |
  | `-2`, 09:47 | 3 | 0, 224, 922 |
  | `-3`, 09:50 | 57 | 0, 137, 1389; 1, 137, 1018 |
  | `-4`, 09:52 | 60 | 0, 110, 1409; 1, 110, 885; 1, 199, 302 |
  | `-5`, 09:53 | 71 | 1, 104, 969; 0, 104, 1493 |
  | `-6`, 09:54 | 63 | 1, 99, 756; 0, 99, 1190 |
  | `-7`, 09:56 | 74 | 1, 84, 946; 0, 84, 1214 |
  | `-8`, 09:57 | 77 | 1, 72, 893; 0, 72, 1296 |
  | `-9`, 09:58 | 90 | 1, 58, 1041; 0, 58, 1316; 0, 342, 394 |
  | `-10`, 09:59 | 107 | 0, 34, 1381; 1, 34, 1040; 1, 169, 381 |
  | `-11`, 10:00 | 104 | 1, 21, 994; 0, 21, 1265; 1, 22, 330 |
  | `-12`, 10:01 | 10 | 0, 223, 1065; 1, 222, 397 |
  | `-13`, 10:03 | 53 | 1, 141, 871; 0, 141, 1516 |
  | `-14`, 10:04 | 64 | 0, 122, 1382; 1, 122, 1061 |
  | `-15`, 10:05 | 84 | 1, 89, 1003; 0, 90, 1373; 1, 266, 386 |

  - **Established from the records:** all 28 waits of 397 ms or more came before rows of one shape.
    Each had id, emotes and timestamp present, 2 emotes and 101 UTF-16 units, and was classified by
    the spawn ingestion with source `IRC`: `IGNORED_EXPIRED`, or `UNCHANGED` at `-2` and `-12`, the
    row's first sightings near the top of the window.
  - **The other 5 waits,** 302 to 394 ms, came before rows with no emotes, `IGNORED_NOT_SPAWN`, of
    other lengths and at other positions.
  - **The rows recur.** One stood at row 96 at 08:57. One stood at row 224 at 09:47 and slid out of
    the window after 10:00, at 6 to 24 rows a minute. The next already stood at row 223 at 10:01. Since
    09:50 a spawn-classified row has been inside the 400-row window at every launch. So the late
    delivery is not the passing block of 2026-11-02 today, and launches waiting for a gap may find
    none.
  - **Inferred, not established:** the frames rule catches a wait when both pages wait together,
    which they do when they share a channel. At `-2` page 0 waited 922 ms while page 1 was still
    delivering, 74 rows begun against page 0's 223, and the main thread drew 3 frames. The rule did
    not fire.
  - **Not done:** stopped after fourteen reruns rather than waiting for a gap. Whether to wait, and
    for how long, or to move the pages, is the owner's decision. The qualification has not started.

**The catalogue-parse mechanism, tested against the fifteen records, 2026-11-03.** The mechanism
was proposed by the owner of the measurement. Everything was read from files on `nbpromec1` with the
phone disconnected: no build, no install, no launch.
- **The proposal, checked clause by clause against the code at `cc4c0cf` and `1198c79` (read):**
  - `SmartCatchSpawnIngestion.kt:61` calls `PokemonTypeDex.findByPokemonName` from the
    coordinator's `resolvePokemon`. That is reached only through `ingestPokemon`
    (`SmartCatchSpawnCoordinator.kt:208`, at line 215). An IRC message reaches it only when it comes
    from the PCG bot and `SpawnMessageParser` matches (lines 142 to 147). This holds.
  - `ensureLoaded` (`PokemonTypeDex.kt:18` to `26`) returns the `@Volatile` catalogue if it is set.
    Otherwise it parses under `synchronized(this)`, on the calling thread. The paragraph's 18 to 25
    ends where the `synchronized` block ends. "Once per process" holds for a parse that succeeds. One
    that throws leaves nothing set, and the next caller parses again.
  - The assets: `pcg_catalog/pcg_pokemon_catalog_v1.json` is 533,491 bytes in git and 556,196 in the
    CRLF checkout and in both APKs; `pokemon_type_dex.json` is 548,770 and 573,586. For the first
    file, the paragraph gave only its size in git.
  - No preload. The only callers are `SmartCatchSpawnIngestion.kt:61` and the buddy capture in
    `ChatFragment.kt`, which is at line 3604 on this branch's head, 3653 at `cc4c0cf` and 3668 at
    `1198c79`. This holds.
  - No clause is wrong. Two are inexact, as noted.
- **How it was read:** from each launch's `probe.json` and `logcat.txt`.
  - A wait runs from `at` − `gapNs` to `at`, each plus `anchorNs`. That is `CLOCK_MONOTONIC`, the clock
    of logcat's `-v monotonic`.
  - The clock check compares each page's "History loaded" line with its `pN_history` boundary. The
    same worker writes both, on consecutive statements (`ChatFragment.kt:2958` and `2965` at
    `cc4c0cf`). Over 30 checks they differ by −0.8 to +3.7 ms.
  - The ingestion lines are `SMART_CATCH_SPAWN: Spawn ingestion source=… outcome=…`
    (`SmartCatchSpawnIngestion.kt:119` to `123`). Each is assigned to a page by its history worker's
    thread id (`pages[].historyTid`).

  The table's instants are in seconds since boot. The outcome is `ignored_expired` except where named.

  | Launch | Long wait: slot, row, start–end, ms | Short wait: slot, row, start–end, ms | Starts, ends apart, ms | Ingestion lines | Long end − first line, ms |
  |---|---|---|---|---|---|
  | first | 0, 96, 8000.605–8001.803, 1197.9 | none: page 1 had no history | – | 8001.802 page 0; 8013.720 another thread | +1.2 |
  | `-2` | 0, 224, 10996.322–10997.244, 921.8 | none recorded: page 1's largest gap 224.3 | – | 10997.118 pages 0 and 1, `unchanged` | +126.1 |
  | `-3` | 0, 137, 11186.073–11187.462, 1388.8 | 1, 137, 11186.622–11187.640, 1017.8 | +548.8, +177.8 | 11187.461 both | +1.3 |
  | `-4` | 0, 110, 11309.742–11311.151, 1409.0 | 1, 110, 11310.278–11311.163, 884.6 | +536.5, +12.1 | 11311.139 both | +11.7 |
  | `-5` | 0, 104, 11374.317–11375.809, 1492.6 | 1, 104, 11374.837–11375.806, 969.3 | +519.9, −3.4 | 11375.796 both | +13.4 |
  | `-6` | 0, 99, 11439.019–11440.209, 1190.4 | 1, 99, 11439.450–11440.206, 756.0 | +431.0, −3.4 | 11440.204 both | +5.1 |
  | `-7` | 0, 84, 11504.634–11505.848, 1214.1 | 1, 84, 11504.898–11505.843, 945.5 | +264.1, −4.5 | 11505.832 both | +15.8 |
  | `-8` | 0, 72, 11568.985–11570.281, 1295.8 | 1, 72, 11569.383–11570.276, 892.7 | +398.5, −4.6 | 11570.274 both | +6.5 |
  | `-9` | 0, 58, 11634.102–11635.417, 1315.6 | 1, 58, 11634.371–11635.412, 1041.4 | +269.3, −4.9 | 11635.404 page 0, .405 page 1 | +13.4 |
  | `-10` | 0, 34, 11698.564–11699.945, 1381.5 | 1, 34, 11698.923–11699.963, 1040.5 | +359.5, +18.4 | 11699.938 both | +7.1 |
  | `-11` | 0, 21, 11761.572–11762.838, 1265.3 | 1, 21, 11761.834–11762.828, 994.4 | +261.7, −9.2 | 11762.827 both | +10.5 |
  | `-12` | 0, 223, 11851.410–11852.475, 1065.0 | 1, 222, 11852.226–11852.623, 396.7 | +815.7, +147.5 | 11852.468 both, `unchanged`; 11852.731 and .793 another thread, `fcm_reminder`, `unchanged` | +7.0 |
  | `-13` | 0, 141, 11931.542–11933.058, 1516.1 | 1, 141, 11932.183–11933.054, 871.2 | +641.3, −3.6 | 11933.053 both | +5.1 |
  | `-14` | 0, 122, 11998.217–11999.600, 1382.3 | 1, 122, 11998.686–11999.747, 1061.2 | +468.7, +147.6 | 11999.591 page 0, .592 page 1 | +8.7 |
  | `-15` | 0, 90, 12063.796–12065.169, 1373.2 | 1, 89, 12064.162–12065.165, 1002.8 | +365.8, −4.6 | 12065.155 both | +14.1 |

- **a, once per process: refuted as worded, consistent with the mechanism.**
  - As worded, "no later one, in either page", it fails. The later page's spawn-shaped row also
    waited, 396.7 to 1061.2 ms, in all 13 launches where both pages held one (`waitRows`). The claim
    falls.
  - What the mechanism predicts for that row is b: its worker is held while the first one parses,
    then does only its own work. That work is short. In 11 of the 13 pairs the two workers'
    ingestion lines fall in the same millisecond; in 2 (`-9`, `-14`) they are 1 ms apart, at
    logcat's 1 ms resolution. At `-2` the two lines also share a millisecond.
  - There were later calls in the same process. At `-12`, two `fcm_reminder` lines came 263 and
    325 ms after the workers' lines, on a thread that is neither worker. In the first launch a second
    line came 11.9 s later. No recorded wait followed any of them. Their own cost cannot be tested,
    because the probe does not time those threads.
- **b, serialised on one monitor: consistent.**
  - In all 13 pairs the shorter wait started later, by 261.7 to 815.7 ms (`waitRows`). The two
    workers finished within 1 ms of each other (above).
  - The waits are measured where they end, on the main thread. In 10 pairs the two ends were −9.2 to
    +18.4 ms apart. In the other 3 (`-3`, `-12`, `-14`) they were +177.8, +147.5 and +147.6 ms apart.
    Between the two ends, page 0 began 68, 75 and 65 history rows and page 1 began none, so page 1's
    row was queued behind page 0's on the main thread (`rows.begin`).
  - The probe took 52 snapshots inside spawn waits (`snaps`, reason bit `1 << 61`). In all 52, page
    0's worker was `R`, and page 1's was `S` or absent, with page 1's CPU unchanged within each
    launch. Between a launch's first and last snapshot, page 0's worker gained 0.83 to 0.97 ms of CPU
    per ms of wall time (in ticks of 10 ms).
  - The record does not say which monitor, since no stack was taken. By the code, the later worker
    waits on the coordinator's monitor (`@Synchronized ingestPokemon`), which it enters before
    `PokemonTypeDex`'s. Nothing contends for `PokemonTypeDex`'s own lock in these launches.
- **c, the long wait's end lines up with the first ingestion line: consistent in 14 of 15, not in
  `-2`.**
  - In 14 launches the line comes 1.2 to 15.8 ms before the end. That follows the worker's order: it
    logs the line inside the ingestion, then posts the row, and the main thread starting that row ends
    the wait.
  - At `-2` the line comes 126.1 ms before the end. In between, no history row of either page began
    and 1 frame ran. The main thread was inside messages for 113.9 ms of the 126.1, one of them
    98.3 ms long (`msgs`). The record does not say what that work was.
  - c does not discriminate: any work inside the ingestion would line up the same way.
- **The time is attributed to the catalogue by the code, not by the record.** On the spawn path,
  only the catalogue does once-per-process work on half-megabyte inputs: two assets of 556,196 and
  573,586 bytes as packaged. The record shows one thread computing for 921.8 to 1516.1 ms, in state
  `R` with CPU close to wall time, while the other is held. It does not show what that thread was
  computing.
- **Verdict: not refuted.** a falls as worded, b and c are consistent, and the attribution is still
  read from the code, not recorded.

*Retracted on 2026-11-03, by the owner of the measurement:* "the two pages serialise on
`synchronized(this)` in `PokemonTypeDex.ensureLoaded`". They serialise on `@Synchronized` on
`SmartCatchSpawnCoordinator.ingestPokemon` (`SmartCatchSpawnCoordinator.kt:208`). The dex's lock is
taken inside that one, so it is not the lock the second worker blocks on. The owner's reason: the
agent read it in the code, and the owner had recalled it.

**A change that would take the parse off the launch's critical path: designed, not implemented**
(the owner's request of 2026-11-03). No code is changed and no pull request is opened. Everything
below is read from the code, not tried.

*Rejected on 2026-11-03 by the owner of the measurement: considered, kept here, not implemented.*
The reason: about 0.8 to 1.5 s of CPU on every launch, the design's own estimate below, would be spent
inside the phase being measured, to serve a path that at launch discards the result. The change taken
instead is *The window decided before the name is resolved* (end of *Recorded results*).

- **The change:** a `PokemonTypeDex.preload(context)` that starts one thread, which calls the
  existing `ensureLoaded`. It is called once, from `MainActivity.onCreate` (`MainActivity.kt:63`),
  after `CrashReporting.applyStoredPreference`.
- **Where, and on which thread:** a new thread, `thread(name = "tmc-dex-preload")` from
  `kotlin.concurrent`, at default priority. That is how `AuthCallbackActivity.kt:41` and the
  uploader start their threads. `MainActivity.onCreate` is the earliest production code on the
  launch path, because `main-v5` has no `Application` subclass (`ProbeApplication` exists only on the
  probe commits). An `Application` would start the thread 36 to 47 ms earlier on these records
  (`app_in` against `act_in`), but it needs a manifest change, so it is not the smallest change.
- **The head start, estimated from the records:**
  - From `act_in` to the start of the first spawn wait: 1252.6 to 2411.7 ms across the 15 launches.
  - The first wait itself, which is the parse's wall time as it ran: 921.8 to 1516.1 ms.
  - If the parse took the same wall time when started at `act_in`, it would end before the spawn row
    in 13 of 15 launches. In `-11` and `-10` it would end 12.7 and 41.1 ms after it.
  - That assumes the parse costs the same when it overlaps inflation, view creation and the history
    requests instead of following them. This is not measured, and the startup work it overlaps may
    also slow down.
- **A spawn arriving before the parse ends:**
  - Its thread calls `findByPokemonName` and finds no catalogue. It waits on `PokemonTypeDex`'s lock,
    which the preload holds, until the parse ends. Then it reads the same catalogue and goes on.
  - Nothing is dropped and nothing is retried. The message is ingested as it is today, only later,
    by whatever remains of the parse.
  - Those threads are:
    - the history worker (`ChatFragment.kt:2991` at `cc4c0cf`, and 2950 for history from a closed
      connection);
    - the IRC reader (`TwitchChatClient.kt:54`, its `onMessage` at 185, ingesting at
      `ChatFragment.kt:2250` at `cc4c0cf`);
    - Firebase's message thread (`MyFirebaseMessagingService.kt:101`).

    None of them is the main thread.
- **The main thread:**
  - Its one caller is the buddy capture (`ChatFragment.kt:3604` on this branch's head, 3653 at
    `cc4c0cf`), inside `appendChatLine`. It runs only for the PCG bot's reply within 15 s of a
    `!pokebuddy` the user sent (`requestBuddyInfo`).
  - Today, if it is the first caller in a process, it parses on the main thread. With the preload it
    waits only for what remains of a parse already in progress, and not at all once that parse is
    done.
  - So the change adds no wait on the main thread; it shortens one that already exists.
- **One shared instance:** yes.
  - The catalogue stays the `@Volatile` field of the `PokemonTypeDex` object, set once under its lock.
    The preload goes through the same `ensureLoaded`, so whichever thread enters first parses, and
    every other thread gets that instance.
  - A recreated activity calls `preload` again. It returns at once if the catalogue is loaded;
    otherwise its thread waits on the lock and returns.
  - `PcgMostWantedActivity.kt:48` and `PcgMostWantedStore.kt:20` keep their own parses of the
    PCG catalogue through `PcgPokemonCatalogRepository`. Sharing those is a different change.
- **A failed parse:**
  - Today an exception from `loadCatalog` reaches the caller's `safely`
    (`SmartCatchSpawnIngestion.kt:85` to `102`) and becomes `FAILED`.
  - A bare thread has no such guard, and an uncaught exception on any thread ends an Android process.
    That is the platform's documented default, not observed here.
  - So the preload catches the exception and logs only its type, as `safely` does
    (`DiagnosticError.typeOf`). It leaves the catalogue unset, and the next caller parses as today.
- **Locks:**
  - The preload takes only `PokemonTypeDex`'s lock.
  - The spawn path takes `SmartCatchSpawnIngestion`'s briefly in `coordinator()` and releases it
    before ingesting. It then takes the coordinator's lock, then `PokemonTypeDex`'s.
  - The buddy capture takes only `PokemonTypeDex`'s.

  No path takes them in the other order, so the preload adds no deadlock.
- **What depends on the parse being synchronous:**
  - The coordinator. `ingestPokemon` resolves the name (line 215) before it stores the spawn, and it
    stores what it resolved. The design keeps the call synchronous for its caller, so this still
    holds. A variant that answered `null` while the parse ran would store a spawn without its
    catalogue fields, the case `unknownCatalogEntryStillKeepsNameAndTimerAvailable` covers. Whether
    a later sighting would fill them in was not read. That is why the design waits instead of
    answering early.
  - The history worker's order. It ingests each message before posting it (`ChatFragment.kt:2984`
    to `3001` at `cc4c0cf`), so the order of the rows does not depend on when the parse ends.
  - The buddy capture, described above.

  Nothing else calls `PokemonTypeDex` (`git grep` at the head, at `cc4c0cf` and at `1198c79`).
- **The existing tests:**
  - Three suites touch this path, with 22 cases between them:
    - `PokemonTypeDexCatalogTest`, 8 cases, builds `PokemonTypeDexCatalog` from the asset files
      through the parsers, and never uses the `PokemonTypeDex` object;
    - `SmartCatchSpawnCoordinatorTest`, 11 cases, injects its own `resolvePokemon`;
    - `SmartCatchSpawnIntegrationSourceTest`, 3 cases, checks the order of the source in
      `ChatFragment`'s IRC callback and in `MyFirebaseMessagingService`.
  - None of them would change, as long as the call goes in `MainActivity` and not in that callback.
  - No test covers the `PokemonTypeDex` object, its single load or its lock. It needs a `Context`,
    and the unit tests have only JUnit and `org.json` (`app/build.gradle.kts:215` to `216`).
- **The tests it would add:** the single load moves into a small holder that takes the loader as a
  function, so it can be tested on the JVM. Three characterization cases:
  - a preload and a concurrent lookup load once and share one instance;
  - a lookup that arrives during the preload's load waits for it and does not load again;
  - a preload whose load throws does not throw out of its thread, leaves nothing loaded, and the next
    lookup loads.
- **Gameplay:** the preload reads a bundled catalogue and sends nothing. It does not touch the
  Pokemon Community Game rules.
- **What it costs:**
  - The parse's CPU, about 0.8 to 1.5 s on these builds, moves to the start of every launch through
    `MainActivity`. That figure is inferred: the CPU rate between snapshots applied to the long waits.
    It includes launches that never see a spawn-shaped message and today never parse.
  - The catalogue is in memory from launch onwards. Its size in memory is not measured.
  - A process that Firebase starts without `MainActivity` keeps today's behaviour.
- **IT INVALIDATES THE FOUR BUILT APKs AND THEIR RECORDED SHA256 VALUES.**
  - The change touches code that all four builds contain: `PokemonTypeDex.kt` and `MainActivity.kt`.
  - A build that carries it is none of `5b09932`, `888489a`, `cc4c0cf` or `1198c79` as built on
    `nbpromec1` and recorded at the top.
  - So step 0's builds, their hashes and the qualification would all be redone, from commits that
    carry the change. It would be applied identically to all four, so that it makes no difference
    between the arms.

**The window decided before the name is resolved, 2026-11-03** (the owner of the measurement's
instruction). One change, in pull request #94 into `main-v5`, not merged. It is justified on its
own terms, as work computed and then discarded. It does not rest on the catalogue being what the
waits were: the records do not show that, and the launch after the rebuild is what tests it.
- **The premise, read on `origin/main-v5` (`SmartCatchSpawnCoordinator.kt`), before the fix was
  written:**
  - the `IGNORED_EXPIRED` branch (lines 71 to 80) reads only `nowMs` and `incomingSeenAtMs`;
  - the `IGNORED_FUTURE` branch (lines 65 to 69) reads only those two;
  - the only branch above them that reads the identity is the `isBlank` check giving
    `IGNORED_NOT_SPAWN` (lines 59 to 63);
  - `ACTIVE_WINDOW_MS` is `90_000L` (line 47) and `MAX_FUTURE_CLOCK_SKEW_MS` is `5_000L` (line 50);
  - in the 15 records the spawn-shaped row's outcome was `IGNORED_EXPIRED` in 13 and `UNCHANGED` in
    2, `-2` and `-12` (`waitRows.spawnOutcome`).
- **The defect:** `ingestPokemon` resolved the name first. The first resolution in a process parses
  1,129,782 bytes of JSON across two assets, to compute an identity that `decide` discards when the
  message is outside the window, which a history message older than 90 s is by construction. It is
  pre-existing: the coordinator, `PokemonTypeDex` and the ingestion are the same blobs on
  `origin/main-v5`, this branch and `007ee55`.
- **The change:** `SmartCatchSpawnMergePolicy.outcomeOutsideWindow` holds the window and skew test
  in one place. `decide` calls it after its identity check, and `ingestPokemon` calls it before
  resolving and returns its outcome without resolving. Inside the window nothing changes.
- **Narrower than asked, and why:** the gate applies only when the raw name gives a non-blank
  identity of its own. For a name that normalizes blank only the resolution can say whether the
  outcome is `IGNORED_NOT_SPAWN` or a window outcome, and the difference reaches the user: the
  Firebase service suppresses the spawn notification on `IGNORED_EXPIRED` and not on
  `IGNORED_NOT_SPAWN`. Such a name keeps today's path, so no outcome changes for any input.
- **Its tests:** run on the unfixed code first, the two "never resolved" cases failed and the two
  others passed; the counts with the fix are in the pull request.
  - *The blank-identity test,* `nameWithoutIdentityOfItsOwnIsStillResolvedBeforeTheWindowVerdict`,
    pins the narrower gate. The raw name `???` normalizes blank and arrives on the Firebase path
    100,001 ms after its spawn. It skips the window gate and still goes through the resolution, once,
    whatever the resolver returns.
  - *How a name gets there: reasoning about reachability, not proof.*
    - On the IRC path the raw name comes from an announcement that is already parsed. The sender
      must be the PCG account (`SmartCatchSpawnCoordinator.kt:163`), and the text must match
      `A wild (.+?) appears` (`SpawnMessageParser`).
    - So reaching the blank case there needs a name with no Latin letter, no digit, and no ♀ or ♂
      once accents are stripped.
    - The owner's instruction says punctuation-only. The normalizer's rule is wider: a name written
      wholly in another script, or in symbols, also normalizes blank (`PcgPokemonNameNormalizer`).
    - On the Firebase path the name is the payload's `pokemon` field. What the backend can send
      there was not read.
- **IT INVALIDATES THE FOUR BUILT APKs AND THEIR RECORDED SHA256 VALUES.** Step 0 and the
  qualification are redone on rebuilt binaries. No measurement APK was built in this step.
- **Expected after the rebuild, written down before it** (the owner's): most launches clean, not
  all. `-2` and `-12` carried a spawn inside the window and would still resolve, so a remaining wait
  on a recent spawn row is not a failure of the change. If the long waits persist on
  `IGNORED_EXPIRED` rows, the attribution to the catalogue is wrong, and the next instrument is a
  thread stack taken inside the wait, not another design.

**The wait rule, brought into the scripts, 2026-11-03.** `launchcheck.py` voids on any recorded wait
and on a record without `waitRows`; the frame count stays in its record and decides nothing. The
same change is on both script branches: `3cf6ff7` on `experiment/startup-5d-control` (from
`92fbb4a`) and `bad5699` on `experiment/startup-5d-treatment` (from `1ae4b42`).
- **Its suites, run:** `launchcheck_test.py` 7 of 7, 4 before, and `qualify5d_test.py` 22 of 22 beside
  it, on Python 3.10 and 3.7.
- **Over the 15 excluded records, run:** 15 of 15 void, as before. Every launch keeps its reasons, and
  `-2` adds late delivery, its 922 ms wait, to its display reading.
- The run directory `%USERPROFILE%\tmc-5d-round\C` still holds the copy from `92fbb4a`; the next
  sitting takes its scripts from the new commits.

**The fix on the four branches, rebuilt, 2026-11-03** (the owner of the measurement's instruction,
after #94 merged as `ed41de1` at 13:20 UTC). The new commits and their sha256 values are in the
second table of *Which build each step applies to*.
- **A merge, not a rebase.**
  - Each new commit is its branch's tip merged with `ed41de1`. It has two parents, the tip and
    `ed41de1`, and it was pushed as a fast-forward.
  - A merge keeps the recorded build commits as ancestors. The superseded records therefore stay
    rebuildable from their branches, and no force push is needed.
  - A rebase would have rewritten those commits and orphaned the ids this plan records.
  - On the two qualification branches the tip was the wait rule's script commit (`3cf6ff7`,
    `bad5699`). Each build commit therefore now carries those scripts, which change nothing outside
    `experiment/`.
- **What changed, read on the commits with `git diff`:**
  - From each old build commit to its new one, outside `experiment/`, exactly three files change:
    `SmartCatchSpawnCoordinator.kt`, its test, and `RELEASE-STATE.md` (from #92). Each is the same
    blob as on `ed41de1`.
  - Inside `experiment/`, nothing changes on the measurement branches. On the qualification branches
    7 script files change, the same 7 that changed from the old build commit to the tip (`3cf6ff7`,
    `bad5699`). The merge adds nothing to them.
  - The toolchain files are identical to `1198c79`'s: `gradle/`, `gradlew`, `gradlew.bat`, the
    build scripts and `gradle.properties`.
- **The gate comes before the resolution. This was read in the file, not inferred from ancestry.**
  - It was read in each new commit with `git show`, and again on disk in the worktree after each
    checkout.
  - In each, `ingestPokemon` calls `SmartCatchSpawnMergePolicy.outcomeOutsideWindow` at line 245
    and `resolvePokemon` at line 256.
  - In `cc4c0cf`, `resolvePokemon` is at line 215, with no gate before it.
- **The builds, executed.** Each was `:app:assembleDevDebug`, run from a detached checkout of its
  commit in `%USERPROFILE%\Documents\doc_vari\AndroidProjects\TwitchMiniChat-wt-5d`, the worktree of
  step 0.
  - They ran in step 0's order: `dea38cc`, `a2f12f1`, `2e72e42`, `550b414`. Each was incremental on
    the one before: 14 of 42 tasks executed and 28 up-to-date, with `compileDevDebugKotlin` among
    the executed.
  - `git status --porcelain --untracked-files=all` was empty before and after each build.
  - The three git-ignored inputs were checked again before the first build. `local.properties`
    holds one key, `sdk.dir`. The other two files are byte-identical to the clone's.
- **The toolchain, read.**
  - Before each build, `gradlew --version` printed Gradle 9.4.1 and the same `Launcher JVM` and
    `Daemon JVM` lines as step 0.
  - One Gradle daemon was running. It started at 12:52:11, so it is not step 0's daemon of 07:55. It
    runs the same binary,
    `%USERPROFILE%\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2\bin\java.exe`, whose `release`
    file gives `JAVA_RUNTIME_VERSION="21.0.10+1-b1163.110"`.
  - That the builds ran in that daemon is inferred: it was the only one, and no build log names its
    daemon.
- **Each APK, read.**
  - Each is signed by `9b3a523b…5c51`, with package `com.fs.twitchminichat.dev`, versionCode 8 and
    versionName `5.5.1-dev`.
  - Each embeds its own commit, three times in its dex, and none of the other ten commits looked
    for: the four old ones, the three other new ones, `3cf6ff7`, `bad5699` and `ed41de1`. None
    carries a dirty marker.
  - `outcomeOutsideWindow` is in each new APK's dex, and it is absent from the old `cc4c0cf` build's
    dex.
  - The APKs and each build's log are kept in `%USERPROFILE%\tmc-5d-round\apks`, beside the old
    ones, which were not deleted.
- **Not done: "Android Studio resynced after cleaning the tree".** It is a step in the Android
  Studio window, and this agent works from the command line, so it was not done. Android Studio was
  running throughout, since 07:54:25.
- **The diagnostic launch did not run.** At about 14:40 `adb devices -l` listed no device, and
  Windows listed no Android device present on USB.

**The diagnostic launch on the rebuilt control build, 2026-11-03 14:50** (the owner of the
measurement's STEP C, after the phone was reconnected). It is diagnostic only: not counted, not part
of the qualification, and not part of a sitting.
- **The build, read back.** `2e72e42` was installed by `run.sh` (`lastUpdateTime` 14:50:53).
  - Its sha256 was read from the phone twice: by `run.sh`, and afterwards with `adb shell
    sha256sum`.
  - Both readings gave `d9970f27…`, the value recorded for `2e72e42` above.
- **The launch:** `D-diagnostic-after-fix`, mode `full`, run from `%USERPROFILE%\tmc-5d-round\C`.
  - The `run.sh` there is the same blob at `92fbb4a` and at `2e72e42`.
  - It was checked with `launchcheck.py` at `2e72e42` (blob `5d7c704`), run from outside the run
    directory. Its output is the run's `launchcheck.json`.
- **The verdict, run: no void, and no recorded wait** (`waitRows` n = 0).
  - The span ran from 1558.7 to 4232.7 ms after the anchor, with 1 frame drawn inside it.
  - It recorded 0 input events.
  - The only process start was the app's own; none was foreign.
  - The refresh rate read 90.0 Hz before and after.
  - Battery saver was off (0), on USB power, charging, level 74, before and after.
  - Both pages were on `c8cb0e91`, with 400 history rows each.
  - Earlier today the phone read battery saver on and 60 Hz. Neither setting was changed by this
    agent.
- **The expired spawns were there.** Logcat holds two spawn ingestions, both `source=irc
  outcome=ignored_expired`.
  - There is one on each page's history worker, threads 19935 and 19944.
  - They came 1444.7 and 1808.7 ms after the anchor, 50 and 18 ms after each worker's `History
    loaded`.
  - No wait followed either of them.
- **The comparison.** Before the fix, every one of the 13 launches whose spawn row was
  `IGNORED_EXPIRED` recorded a wait on that row. The largest such wait per launch was 1190.4 to
  1516.1 ms. After the fix, one launch with an expired spawn on each page recorded none.
- **Beside the verdict, computed by this agent from the probe's rows (these are not the probe's own
  waits):**
  - **The largest gap between consecutive history-row begins.**
    - It was 248.5 ms on page 0, before row 349 (from 2823.8 to 3072.4 ms), and 3.5 ms on page 1.
    - The same computation gives 922.1 ms at `-2` and 1389.0 ms at `-3`, where the probe recorded
      921.8 and 1388.8.
    - 248.5 ms is below the 300 ms floor and above 2026-11-02's on-time maximum of 212 ms.
    - The gap starts 5 ms after Choreographer logged 119 skipped frames: the main thread drawing its
      first frame after 1330 ms. That the gap is that frame is inferred from the timing, not shown.
      The gap falls more than a second after both ingestions.
  - **Page 1's rows began at 3246.8 ms,** after page 0's last row at 3226.1 ms, although page 1's
    history was loaded at 1790.7 ms. At `-2` the two pages' rows interleaved.
    - This is recorded, not explained.
    - A delay before a page's first row is the case the wait rule cannot catch (*What it cannot
      catch*, *Decided on 2026-11-03*).
- **Against the owner's two outcomes, this is the first:** no long wait on an `IGNORED_EXPIRED` row.
  - By the owner's rule, the attribution is established by intervention, and the qualification can
    be run.
  - The "after" side rests on one launch.
- **Not started:** the qualification.

**The qualification sitting, 2026-11-04, 07:55 to 08:50: both builds NOT QUALIFIED.** It ran as *The
qualification sitting of 2026-11-03* sets out, once the phone was reconnected. *Qualification* says a
failed qualification stops the round before the measurement, so the round stops here. The closing
measurement was not run.

- **Battery saver was on at the start. It was turned off before the first counted launch.**
  - The excluded launch, at 07:56, read battery saver on and 60.000004 Hz, before and after.
  - The phone read `mSettingBatterySaverEnabled=true`, sticky, with `mSettingAutomaticBatterySaver=0`.
    So charging would not have turned it off.
  - Every earlier record checked read it off, at 90.0 Hz: `-13`, `-14`, `-15` and the diagnostic
    launch.
  - The plan requires it off before the decision launches, and one state across a section,
    qualification included (*The battery*, and the record of 2026-11-03). So the agent stopped
    before the first counted launch and asked. Federico turned it off; the agent did not change it.
  - At 07:59 `low_power` read 0. The display read 90.0 Hz on all 6 readings from 07:59:31 to 07:59:56,
    so the section is held at 90.0 Hz.
  - The excluded launch was not repeated. It is there for the pages' reading, and battery saver does
    not choose a page's channel or history count. That is the agent's reasoning; it was not tested.
- **The excluded launch, `X-sitting-excluded`, excluded from every count.** Both pages read
  `c8cb0e91` with 400 history rows, so the sitting started. The control build was read back as
  `d9970f27…` twice.
- **The launches: 28 counted, 14 per build, in the plan's order.** First three baselines with `full`,
  then `mention` to `app`, then `stall` with `arg=8000`. The control ran from 08:00:13 to 08:24:01,
  and 5d from 08:25:11 to 08:50:12.
  - Every launch of the sitting was preceded by the driver's install and read-back, then by
    `run.sh`'s. That makes 35 launches, counting the excluded one and the void ones. Both readings
    gave STEP B's sha256 every time: `d9970f27…` for `2e72e42`, and `e881d0ab…` for `550b414`.
  - All 28 counted launches read the same:
    - 0 input events;
    - 90.0 Hz before and after;
    - battery saver 0, on USB power;
    - 800 history rows in the span, with both pages on `c8cb0e91` at 400;
    - compile state `run-from-apk`;
    - first after install.
  - Recorded, not held:
    - the level rose from 42 to 56 over the control, and from 58 to 74 over 5d;
    - the temperature rose from 29.3 to 32.6 °C.
  - The host scripts ran on Python 3.10.0.
- **Six launches were void.** Each was read, then re-run in its place.
  - Four were late deliveries, each on a spawn still inside its window:
    - `Sc-base-2`: page 1, row 219, 433.9 ms, and page 0, row 219, 1495.4 ms, both `UNCHANGED`;
    - `Sc-gap`: page 0, row 224, 852.1 ms, `UNCHANGED`;
    - `S5-buddy`: page 0, row 180, 1351.5 ms, `STORED`, and page 1, row 180, 1084.0 ms, `UNCHANGED`;
    - `S5-stall`: page 0, row 241, 1294.1 ms, and page 1, row 241, 1031.2 ms, both `UNCHANGED`. The
      launch before it, `S5-app`, ingested a new spawn live, `stored`, 5338.9 ms after its anchor.
  - Two had a foreign process start inside the span, with no wait:
    - `Sc-gap-r1`: `.adservices`, started for `com.google.android.ext.services`'s
      `AdServicesCommonService`, at 2061.2 ms;
    - `S5-buddy-r1`: `com.heytap.mcs.statistics` and `com.heytap.mcs.biz.message`, at 268.3 and
      285.3 ms.
  - The fix gates only a spawn outside its window. A spawn inside it still reaches `resolvePokemon`.
    That the four waits are its parse is inferred from where they fall, as at `-2` and `-3`; it is
    not shown.
  - Each late delivery's re-run came 86 to 130 s later, by the driver's clock. Each read its history
    spawns as `ignored_expired`, with no wait.
- **No contradiction of the diagnostic launch.** In none of the 35 launches was a recorded wait on an
  `IGNORED_EXPIRED` row.
  - 50 history ingestions were `ignored_expired`, 44 of them in counted launches. None was followed
    by a wait.
  - 8 history ingestions were of spawns still inside their window. All 8 were in the four late
    deliveries, and 7 were followed by a wait of their page. The eighth, page 1's at 3034.6 ms in
    `Sc-gap`, was not; that page's largest gap was 232.5 ms.
- **A fault of the driver, found and fixed during the sitting.**
  - `launchcheck.py` exits 1 when a launch is void. On `Sc-base-2` the driver read that as a failure
    and stopped.
  - The launch's record was complete. The fix accepts exit status 0 and 1.
  - No launch was affected.

**The two verdicts.** They come from `qualify5d.py`, blob `41ac728`, run in the run directory on
Python 3.10.0. Before the first launch, the run directory's scripts were read to be the same blobs as
`2e72e42`'s and `550b414`'s.
- control: `NOT QUALIFIED: reconcile (criteria 3), buddy (criteria 3), hidden (criteria 3),
  beforeinsert (criteria 3), insert (criteria 2, 3), follow (criteria 3), build (criteria 2), gap
  (criteria 3), app (criteria 3)`
- 5d: `NOT QUALIFIED: reconcile (criteria 3), buddy (criteria 3), hidden (criteria 3), beforeinsert
  (criteria 3), insert (criteria 3), follow (criteria 3), build (criteria 3), gap (criteria 3), app
  (criteria 3)`
- What held on both builds:
  - one display reading and one battery state;
  - 800 rows in every span;
  - no void launch scored;
  - three baselines, which injected nothing, and each injection once.

**The criteria, control (`2e72e42`).**
- **Criterion 1** passes on all 11. The recorded delays are 200.10 to 201.35 ms against 200, and
  534.45 ms against 534 for `stall`.
- **Criterion 2** applies to 10 injections. It passes on 8, with rises of 189.5 to 201.8 ms, and
  fails on two:
  - `insert`: 1669.9 ms against the baselines' 1653.5, 1637.5 and 1584.1, a rise of 32.4 ms over
    their median;
  - `build`: 1599.5 ms against 1542.4, 1526.1 and 1477.3, a rise of 73.4 ms.
- **Criterion 3** passes on `mention` and `build`, and is NOT APPLICABLE to `stall`. It fails on the
  other 8, and every failing value lies below its band:
  - `timeline_insert`, on 7: 1469.1 to 1556.8 ms against [1574.1, 1663.5]. On `insert` it contains
    the injection, so it is not held there;
  - its view build, on 8: 1365.9 to 1451.0 ms against [1467.3, 1552.4];
  - `follow_or_count_composer_restore`, on 4 (`reconcile`, `hidden`, `insert` and `app`): 97.6 to
    99.8 ms against [100.3, 137.0].
- **Criterion 4** passes on all 11. Its reported span rise runs from −229.1 to 44.8 ms, against a
  spread of 249.6 ms across the baselines.
- **Criterion 5** passes. The burst ended at 8535.97 ms, inside [8534.38, 8537.63]. All three
  baselines' bursts ended before the stall, at 4105.2 to 4244.2 ms.

**The criteria, 5d (`550b414`).**
- **Criterion 1** passes on all 11: 200.10 to 200.15 ms, and 534.78 ms for `stall`.
- **Criterion 2** passes on all 10, with rises of 143.0 to 218.7 ms.
- **Criterion 3** passes on `mention`, and is NOT APPLICABLE to `stall`. It fails on the other 9, each
  on one segment, `dedup_backfill_mention`: 97.4 to 167.0 ms against [168.6, 203.4]. The baselines
  read 181.1, 193.4 and 178.6 ms.
- **Criterion 4** passes on all 11. Its reported span rise runs from −5.6 to 186.4 ms, against a
  spread of 175.3 ms across the baselines.
- **Criterion 5** passes. The burst ended at 8536.35 ms, inside [8534.31, 8538.00]. The baselines'
  bursts ended at 2279.4 to 2324.6 ms.

**Beside the verdicts, deciding nothing.**
- **Criterion 3's failures follow launch order again.** On both builds the baselines ran first, and
  every failing segment is lower in the later launches. This is recorded, not diagnosed, as on
  2026-11-02.
  - *Measured afterwards, on rows no injection touches* (*Decided on 2026-11-04*, item 1): it is a
    step at the baselines, not a trend with order. Among the eleven injections, Spearman against
    position is −0.04 to −0.13.
  - On the control, `insert`'s and `build`'s criterion 2 values sit in segments that fall the same
    way. That this took their rises below 100 ms is inferred from those values; it is not shown.
- **The JIT thread's CPU across the span:**
  - control: 750 to 1090 ms, across brackets of 2177.6 to 2513.2 ms and spans of 2117.7 to 2405.0 ms;
  - 5d: 600 to 820 ms, across brackets of 643.6 to 898.9 ms and spans of 637.0 to 882.8 ms.
- **A history row's cost by process age.** These are medians per row, binned by 250 ms of the row's
  begin after the anchor.
  - **Control: a whole row cost 2368 to 3468 µs in the bin from 1500 ms, and 1788 to 2132 µs in the
    bin from 3000 ms.**
    - At 3000 ms the baselines read 1940 to 2132 µs, and the later launches 1788 to 2116.
    - Dedup was highest at up to 316.4 µs, in the bins from 2000 or 2250 ms in 11 of the 14
      launches. It read 74.7 to 101.3 µs in the bin from 3000 ms.
  - **5d: every span lies between 1250 and 2500 ms.**
    - A whole row cost 148 to 513 µs, and dedup 50.1 to 290.3 µs.
    - Three launches had dedup sums of 97.9, 97.4 and 133.4 ms: `beforeinsert`, `follow` and `build`.
      These are the only three whose dedup fell to 50.1 to 81.0 µs, in the bins from 2000 or
      2250 ms.
    - Every other bin of every launch read 89.8 µs or more, and no baseline bin read less than
      124.4 µs.
- **The three quantities the owner added.**
  - **a. The largest gap below 300 ms.**
    - In the seven row injections it falls where the delay was placed, before page 0's row 101: 202.6
      to 206.2 ms on the control, and 200.8 to 201.6 ms on 5d. The exception is 5d's `follow`, whose
      largest is 217.5 ms before row 208, with 206.4 ms on page 1 at the same moment.
    - In `gap` it falls before row 200: 205.5 ms on the control, and 200.8 ms on 5d.
    - In the baselines it is 87.3 to 182.1 ms on the control's page 0, and 3.4 to 191.1 ms on its
      page 1. On 5d it is 106.6 to 130.8 ms on page 0, and 1.1 to 42.4 ms on page 1.
    - The largest in any counted launch is 298.4 ms, in 5d's `build`, on page 0 before row 156. 5d's
      `build` puts its delay in a bind, outside every row. The gap holds it, as checked after the
      sitting from the published output: the delay ran at 1726.5 to 1926.6 ms, inside the build's
      main-thread message, 1697.9 to 1995.5 ms, which lies inside the gap, 1697.4 to 1995.8 ms.
  - **b. The pages' order.**
    - The pages ran one after the other in 13 of the control's 14 counted launches, with page 1
      starting 1088.9 to 1517.4 ms after page 0. Only in `follow` did they interleave.
    - On 5d they ran one after the other in 10 of 14, with offsets from −308.9 to 656.5 ms. They
      interleaved in `base-1`, `reconcile`, `follow` and `gap`, and in `gap` page 1 began first.
    - The excluded launch ran them one after the other. All four late deliveries interleaved.
    - In the 14 two-page excluded records before this sitting, they interleaved every time. This is
      recorded, not interpreted.
      - All 14 ran `cc4c0cf`, the pre-fix control, and each recorded 1 to 3 waits (*Decided on
        2026-11-04*, item 5, where the difference is declared a confound).
  - **c. The spawn ingestions.**
    - The history ingestions are counted above.
    - Two launches ingested a spawn live, outside the history:
      - `Sc-build` at 15961.7 ms, `stored`, then `unchanged` 1 ms later;
      - `S5-app` at 5338.9 ms, the same pair.
    - Six counted launches logged no history spawn ingestion: `Sc-base-1`, `S5-mention`,
      `S5-reconcile`, `S5-build`, `S5-gap` and `S5-app`.

**Published: every value `qualify5d.py` printed.** It is in `experiment/startup-5d/qualification-2026-11-04/`
on both experiment branches:
- `74bdb16` on `experiment/startup-5d-control`, and `20bc40a` on `experiment/startup-5d-treatment`.
  Each sits directly on its build commit, so the builds stay `2e72e42` and `550b414`.
- The files hold:
  - both outputs, whole;
  - each launch's line;
  - the three quantities for all 35 launches;
  - tables derived from them, which hold every launch's criteria, JIT CPU and cost curve.
- **One edit to the outputs:** each launch record's local APK path writes its user directory as
  `%USERPROFILE%`. That is 14 lines per file; nothing else differs.
- **Scanned before the push.** The added lines hold no account id beyond its prefix, no user name, no
  token and no chat text.
- The run directories stay in `%USERPROFILE%\tmc-5d-round\C\runs`, outside any clone.

**Not done.**
- Criterion 3's failures were not diagnosed, and no criterion was adjusted.
- The closing measurement was not run.
- The phone keeps `550b414`'s build, installed at 08:49:31 for `S5-stall-r1`. It is recorded in
  `RELEASE-STATE.md` after the round, as the plan says, not now.

**The re-qualification sitting, 2026-11-04, 10:27 to 11:13: both builds NOT QUALIFIED, on criterion 3
above its bound.**
- It ran as *Decided on 2026-11-04, after the qualification and before the re-qualification* sets
  out, under its pre-registration (sha256 `36ce1935…`), once the phone was reconnected.
- That pre-registration says a failed qualification stops the round before the measurement, so the
  round stops here. The closing measurement was not run.

- **Read before the first launch, 10:27 to 10:28.**
  - The driver's four files read the sha256 the pre-registration gives. The run directory's eight
    scripts read their blobs, `qualify5d.py` `19d110c` among them. No `R` label existed.
  - Battery saver was off: `low_power` 0 and `mSettingBatterySaverEnabled=false`. The phone was on
    USB power.
  - The display read 90.0 Hz on all 6 readings from 10:27:53 to 10:28:18. So the section is held at
    90.0 Hz, as in the morning's sitting.
  - The battery read level 100 and status full (5); in the morning it was charging. The scripts
    hold battery saver and the power source, not the level or the status.
- **The excluded launch, `X-R-excluded`, at 10:28:59.** Both pages read `c8cb0e91` with 400 history
  rows, so the sitting started. It was void for a foreign process start
  (`com.amazon.clouddrive.photos`, at 2755.1 ms). That reason does not bear on what the launch is
  for.
- **The launches: 28 counted, 14 per build, in the pre-registered order,** with the baselines at
  positions 1, 8 and 14. The control ran from 10:30:36 to 10:52:15, and 5d from 10:52:30 to
  11:12:35.
  - With the excluded launch and three voids, that makes 32 launches.
  - Each was preceded by the driver's install and read-back, then by `run.sh`'s. Every reading gave
    STEP B's sha256: `d9970f27…` for `2e72e42`, and `e881d0ab…` for `550b414`.
  - All 28 counted launches read the same:
    - 0 input events and 0 foreign process starts;
    - 90.0 Hz before and after;
    - battery saver 0, on USB power;
    - 800 history rows in the span, both pages on `c8cb0e91` at 400;
    - compile state `run-from-apk`;
    - first after install.
  - Recorded, not held: the level stayed at 100. The temperature ran from 29.2 to 31.0 °C over the
    control, and from 30.7 to 31.1 °C over 5d.
- **Three launches were void, each a late delivery on a spawn still inside its window.** The driver
  re-ran each in its place. This was the first time its re-run loop ran on the phone.
  - `Rc-mention`: page 0, row 258, 1114.5 ms, `UNCHANGED`. `-r1` was clean.
  - `Rc-app`: page 0, row 308, 917.0 ms, `STORED`, and page 1, row 307, 304.8 ms, `UNCHANGED`.
    `-r1` was clean.
  - `R5-base-2`: page 1, row 235, 890.9 ms, and page 0, row 235, 1249.8 ms, both `UNCHANGED`.
    `-r1` was clean.
  - No launch needed a second re-run, and the driver stopped nowhere.
- **No contradiction of the diagnostic launch.** In none of the 32 launches was a wait recorded on
  an `IGNORED_EXPIRED` row.
  - 42 history ingestions were `ignored_expired`, 40 of them in counted launches.
  - None was followed by a wait.

**The two verdicts.** They come from `qualify5d.py` `19d110c`, run in the run directory on Python
3.10.0.
- control: `NOT QUALIFIED: mention (criteria 3), buddy (criteria 3)`
- 5d: `NOT QUALIFIED: insert (criteria 3)`
- What held on both builds:
  - one display reading and one battery state;
  - 800 rows in every span;
  - no void launch scored;
  - three baselines, which injected nothing, and each injection once.

**The criteria.**
- **Criterion 1** passes on all 11 of each build.
  - Control: 200.11 to 200.18 ms, and 534.59 ms for `stall`.
  - 5d: 200.10 to 200.15 ms, and 535.03 ms for `stall`.
- **Criterion 2** passes on all 10 of each build.
  - Control: rises of 129.8 to 213.0 ms. `insert` rose 213.0 ms and `build` 129.8.
  - 5d: rises of 140.7 to 211.0 ms.
- **Criterion 3, one-sided,** is NOT APPLICABLE to `stall` on both builds.
  - **The control fails on 2, and passes on 8:**
    - `mention-r1`: `timeline_insert` 1612.2 ms against an upper bound of 1550.5 (+61.7), and its
      view build 1498.7 against 1444.5 (+54.1);
    - `buddy`: 1576.7 against 1550.5 (+26.2), and 1464.8 against 1444.5 (+20.3).
  - **5d fails on 1, and passes on 9:** `insert`'s `follow_or_count_composer_restore`, 37.2 ms against
    31.8 (+5.4). In the morning's sitting the same value lay 0.0 ms below its bound.
  - **Below the lower reference, deciding nothing:**
    - on the control, `timeline_insert` and its view build in 6 launches (`reconcile`, `hidden`,
      `beforeinsert`, `follow`, `gap` and `app-r1`), and dedup in `app-r1`;
    - on 5d, dedup and `timestamp_echo_reconcile` in `app`.
- **Criterion 4** passes on all 11 of each build. The reported span rise ran from −114.8 to 309.7 ms
  on the control, against a spread of 46.3 ms across the baselines. On 5d it ran from −123.4 to
  214.0, against 78.2.
- **Criterion 5** passes on both builds.
  - Control: the burst ended at 8536.93 ms, inside [8534.67, 8538.18]. The baselines' bursts ended
    before the stall, at 3542.4 to 4059.4 ms.
  - 5d: it ended at 8536.45 ms, inside [8535.41, 8537.44]. The baselines' bursts ended at 1983.3 to
    2251.7 ms.

**The predictions.** `requal_p1.py` was run in the run directory, on the labels in sitting order.
- **P1 holds on both builds.**
  - Control: the exact p is 0.558, with the baselines ranking 5, 9 and 8 of 14.
  - 5d: it is 0.500, with ranks 7, 5 and 11.
- **P2 does not hold on either build.** It decides nothing.
- **P3 fails on both builds,** on the three values above.
- **P4 holds:** criterion 2 passes on all 10 of each build, the control's `insert` and `build`
  included. The instrument gap the pre-registration named in advance did not occur.
- **P5 holds.**
- **The expected verdict, both builds QUALIFIED, was not reached.** No criterion is adjusted.

**Beside the verdicts, deciding nothing.**
- **Where the failing excess sits** (`location.txt`). Each value is broken down by row set and
  compared with the highest baseline's same rows. This locates the excess; it explains nothing.
  - **`mention-r1`'s `timeline_insert`:** +30.2 ms on page 0's rows 11 to 99, and +64.4 on rows 101
    to 399. Page 1 reads −16.6, and row 100 −0.5.
    - Rows 11 to 99 ran before its delay began, since the delay is in row 100.
    - So by time order, their part is not the delay's leakage. That is reasoning, not tested.
  - **`buddy`'s:** +15.9 on page 1, +8.1 on rows 11 to 99, and +12.7 on rows 101 to 399.
  - **5d `insert`'s `follow_or_count_composer_restore`:** +7.7 on page 1, +6.1 on rows 101 to 399,
    and +0.1 on row 100.
- **How often a held value lies above the highest baseline,** before the 10 ms margin.
  - It does for 17 of the control's 80 held values (21%), and for 15 of 5d's 82 (18%).
  - `mention-r1` does on 7 of its 9 held segments.
  - A launch drawn from the same distribution as three baselines lies above all three with
    probability 1/4. That is arithmetic about order statistics, not a measurement, and it changes
    no verdict.
- **The morning's step did not recur.**
  - Page 1's `timeline_insert` read 622.8, 629.6 and 626.4 ms in the control's baselines, and 594.1
    to 645.5 in its injections.
  - In the morning it read 717.2, 747.7 and 685.4, against 637.3 to 690.7.
  - This is recorded, not interpreted.
- **The pages' order did not differ between the arms on this sitting.** This is recorded, not
  interpreted, beside the confound declared in item 5 of *Decided on 2026-11-04*.
  - The pages ran one after the other in all 14 counted launches of each build. Page 1 started
    1167.9 to 1625.8 ms after page 0 on the control, and 456.9 to 749.1 ms after on 5d.
  - All three void launches interleaved. The excluded launch ran its pages one after the other.

**Published** in `experiment/startup-5d/requalification-2026-11-04/` on both experiment branches:
`0131ce5` on `experiment/startup-5d-control`, and `9c19749` on `experiment/startup-5d-treatment`.
- The files are the morning's set, plus `p1-control.json`, `p1-5d.json`, `location.py` and
  `location.txt`.
- **One edit, as in the morning:** each launch record's local APK path writes its user directory as
  `%USERPROFILE%`. That is 14 lines per file; nothing else differs.
- **Scanned before the push.** The added lines hold no account id beyond its prefix, no user name, no
  token and no chat text.

**Not done.**
- The failures were not diagnosed, and no criterion was adjusted.
- The closing measurement was not run.
- The phone keeps `550b414`'s build. It was installed at 11:11:59 for `R5-base-3`, and read back from
  the phone at 11:16. It is recorded in `RELEASE-STATE.md` after the round, as decided, not now.

**The negative control, 2026-11-04, 12:05 to 12:59: CONSISTENT WITH NOISE, P_null 0.6003.**
- It ran under its pre-registration: sha256 `52e96461…`, item 1 of *After the re-qualification,
  2026-11-04*.
- It is scored by the rule written there. Nothing else is read from it here.

- **Read before the first launch, 12:01:40 to 12:05:34.**
  - Battery saver was off: `low_power` 0 and `mSettingBatterySaverEnabled=false`. The phone was on
    USB power.
  - The battery read level 100, status full (5), and 32.4 °C.
  - **The display did not read one rate at first.**
    - It read 60.000004 Hz on 5 readings from 12:01:40 to 12:01:56, then 90.0 on 3, then 60.000004
      on 10 from 12:02:27 to 12:03:05.
    - At 12:03 the display's votes held three app requests capped at 60.000004 Hz. Thermal status
      was 0, and battery saver was off.
      *Corrected on 2026-11-04:* those three votes were on displays 2 to 4, not on the phone's
      screen, so they do not show what the sentence implies (*The closing measurement, 2026-11-04*).
      The sentence above stays as it was written.
    - Another app had been in use on the phone, and its window was listed. By 12:04:57 its task was
      not visible.
  - **From 12:04:32 the display read 90.0 Hz** on all 9 readings to 12:05:34, and on every reading
    after. This is recorded as a parameter. The section holds the rate the launches read, and every
    one of them read 90.0.
- **The excluded launch, `X-N-excluded`, at 12:05:47:** `EXCLUDED_OK`. Both pages read `c8cb0e91`
  with 400 history rows.
- **The launches: 28 counted, 14 per build, in the pre-registered order.**
  - The control ran from 12:07:36 to 12:38:57, and 5d from 12:39:06 to 12:59:06.
  - With the excluded launch and four voids, that makes 33 launches.
  - Each was preceded by the driver's install and read-back, then by `run.sh`'s. Every reading gave
    STEP B's sha256: `d9970f27…` for `2e72e42`, and `e881d0ab…` for `550b414`.
  - **All 28 counted launches read the same:**
    - 0 input events and 0 foreign process starts;
    - 90.0 Hz before and after;
    - battery saver 0, on USB power;
    - 800 history rows in the span, both pages on `c8cb0e91` at 400;
    - compile state `run-from-apk`;
    - first after install.
  - **Recorded, not held:**
    - the level stayed at 100, and the status at full (5);
    - the temperature ran from 30.8 to 32.6 °C over the control, and from 32.0 to 32.7 °C over 5d.
- **Four launches were void, on three slots, each a late delivery.** The driver re-ran each in its
  place.
  - `Nc-plain-5`: page 0, row 251, 1233.2 ms, `UNCHANGED`. `-r1` was clean.
  - `Nc-plain-11`: page 0, row 399, 1252.9 ms, `STORED`.
    - `-r1` was void too: page 1, row 206, 936.4 ms, `UNCHANGED`.
    - `-r2` was clean.
    - This was the first slot that needed a second re-run.
  - `N5-plain-7`: page 1, row 261, 1016.5 ms, and page 0, row 261, 1502.3 ms, both `UNCHANGED`.
    `-r1` was clean.
  - The driver stopped nowhere.
- **No contradiction of the diagnostic launch.** In none of the 33 launches was a wait recorded on an
  `IGNORED_EXPIRED` row.
  - 54 history ingestions were `ignored_expired`, 52 of them in counted launches.
  - None was followed by a wait.
- **The phone was touched between launches, not inside them.**
  - Before `Nc-plain-9`, the driver's idle wait saw 81 and then 657 input events in two 20 s
    windows, from 12:27 to 12:28. It launched after a window with none.
  - For the first four counted launches, the driver's idle lines were filtered out of what the agent
    displayed, and were not saved. So whether it waited for input there is not recorded.
  - At 12:16:22, a 20 s read saw 0 input events.
  - Every counted launch recorded 0 input events inside its own record.
- **Read after the last launch, 12:59:23 to 12:59:29.**
  - Battery saver was off: `low_power` 0 and `mSettingBatterySaverEnabled=false`. The phone was on
    USB power (`mPlugType=2`).
  - The battery read level 100, status full (5), and 32.6 °C.
  - The display read 90.0 Hz on all 6 readings.
  - The installed build read back from the phone as `e881d0ab…`, which is `550b414`'s.
  - The last launch of each build read both pages on `c8cb0e91` at 400.

**The verdict,** from `negctl.py` `0100391`, run in the run directory from 13:00:21 to 13:00:46 on
Python 3.10.0, against `qualify5d.py` `19d110c`. Both blobs, and the run directory's other five, were
re-read immediately before.
- `CONSISTENT WITH NOISE: P_null 0.6003 >= 0.05. Criterion 3 cannot discriminate leakage from noise at three baselines.`
- **The section held on both builds:**
  - one display reading and one battery state, within each build and across both;
  - no void launch scored;
  - 800 rows in every span;
  - no launch carrying an injection.
- **The prediction, CONSISTENT WITH NOISE, held.**
- **The distribution behind it:**
  - Of twenty, combined: P(0) 0.2250, P(1) 0.1440, P(2) 0.0307, and P(≥ 3) 0.6003.
  - Of ten, per build:
    - control: P(0) 0.3357, expecting 2.33 failing launches;
    - 5d: P(0) 0.6703, expecting 1.23.

**Beside the verdict, deciding nothing.**
- **The fixed replica, band at positions 1, 8 and 14, all nine segments held.**
  - **The control fails on 6 of its 11 scored launches, with 13 failing values of 99:**
    - `Nc-plain-2`: `timeline_insert` +80.5, `follow_or_count_composer_restore` +5.0, view build
      +76.7;
    - `Nc-plain-3`: +88.6, +3.1, +84.5, on the same three;
    - `Nc-plain-7`: `follow_or_count_composer_restore` +8.4;
    - `Nc-plain-9`: `dedup_backfill_mention` +0.2;
    - `Nc-plain-12`: `timeline_insert` +14.1, view build +11.6;
    - `Nc-plain-13`: `dedup_backfill_mention` +3.5, `timeline_insert` +93.3, view build +84.5.
  - **5d fails on 4 of its 11, with 4 values of 99.** All are on `dedup_backfill_mention`, at +10.7,
    +11.4, +10.4 and +6.9, in `N5-plain-3` to `-6`.
- **The before-margin fraction in the fixed replica.**
  - Control: 52 of 99 (52.5%). 5d: 28 of 99 (28.3%). Both against 1/4.
  - The pre-registration reports it "only as a check that the launches are exchangeable". By that
    check, the control's band at positions 1, 8 and 14 was not exchangeable with its other eleven
    launches.
  - It decides nothing, and the decisive statistic uses every band.
- **Per launch, over every band, with all nine segments held:** the control fails at a rate of
  0.2495, and 5d at 0.1371.
- **The pages' order, recorded, not interpreted.**
  - The control's counted launches ran one after the other in 11 of 14, and interleaved in
    `Nc-plain-1`, `-7` and `-10`.
  - 5d's ran one after the other in 14 of 14.
  - All four void launches interleaved. The excluded launch ran its pages one after the other.

**Published** in `experiment/startup-5d/negative-control-2026-11-04/` on both experiment branches:
`02381b4` on `experiment/startup-5d-control`, and `0795e53` on `experiment/startup-5d-treatment`.
Both were read back from the remote at 13:04:05.
- **The files:**
  - `negctl.json`, the scorer's output;
  - `launches-control.txt` and `launches-5d.txt`, every launch's sitting line in sitting order,
    voids and re-runs included, with `X-N-excluded` first;
  - `three-quantities.jsonl`, every launch's `sitting_extra.py` record.
- **One edit, as before:** each launch record's local APK path writes its user directory as
  `%USERPROFILE%`. That is 28 lines; nothing else differs. Line endings are LF, as in the earlier
  publications.
- **Scanned before the push.** The added lines hold no user name, no account id, no token, no
  authorization text and no chat text. The channel appears only as the fingerprint `c8cb0e91`.

**Not done.**
- No failure was diagnosed, no criterion was adjusted, and neither sitting was re-scored.
- The closing measurement was not run, and the release was not decided.
- The phone keeps `550b414`'s build, read back at 12:59. It is recorded in `RELEASE-STATE.md` after
  the round, as decided, not now.

**The closing measurement, 2026-11-04, 14:03 to 14:23: span HOLDS, burst end HOLDS.**
- It ran under its pre-registration: sha256 `0dbec904…`, item 1 of *Before the closing measurement,
  2026-11-04*. That was read back from the remote at 14:00:28, before the first launch at 14:03:03.
- The verdicts are `decide.py`'s. Nothing is decided from them here.

- **Read before the first launch, 14:02:05 to 14:02:12** (`section-read.sh`, `section-M-before.txt`).
  - Battery saver was off: `low_power` 0 and `mSettingBatterySaverEnabled=false`. The phone was on
    USB power (`mPlugType=2`).
  - The battery read level 100, status full (5), and 29.2 °C.
  - Thermal status was 0.
  - The display read 90.0 Hz on all 6 readings.
  - The installed build was `e881d0ab…`, `550b414`'s, from the negative control.
  - The driver's seven files and the run directory's five scripts were re-read at 14:02, and matched
    the pre-registration.
- **The excluded launch, `X-M-excluded`, at 14:03:03 on `dea38cc`:** `EXCLUDED_OK`. Both pages read
  `c8cb0e91` with 400 history rows.
- **The ten, from 14:04:32 to 14:23:17, C T C T C T C T C T:** `Mc-1`, `M5-1`, … `Mc-5`, `M5-5`.
  - Each was preceded by the driver's install and read-back, then by `run.sh`'s. Every reading gave
    STEP B's sha256: `d005e901…` for `dea38cc`, and `ece097ac…` for `a2f12f1`.
  - **All ten were clean on their first run.** No launch was void, and none was re-run.
  - **All ten read the same:**
    - 0 input events and 0 foreign process starts;
    - 90.0 Hz before and after;
    - battery saver 0, on USB power;
    - 800 history rows in the span, both pages on `c8cb0e91` at 400;
    - compile state `run-from-apk`;
    - first after install;
    - no recorded wait.
  - **Every idle wait,** the excluded launch's included, read 0 input events in its first 20 s
    window. Each was shown and saved, and the app in front before `run.sh` was the launcher.
  - **Recorded, not held:** the level stayed at 100 and the status at full (5). The temperature ran
    from 29.5 to 30.0 °C.
  - **`decide.py` read:** the order `CTCTCTCTCT`, and one display reading and one battery state,
    across the ten and the 84 launches named under `--qualified`.
- **No contradiction of the clean ingestions.**
  - 18 history ingestions were `ignored_expired` in the ten, and 20 with the excluded launch.
  - None was followed by a wait.
  - `Mc-4` recorded no spawn ingestion.
- **Read after the last launch, 14:23:33 to 14:23:45** (`section-M-after.txt`).
  - Battery saver was off, on USB power.
  - The battery read level 100, status full (5), and 29.7 °C. Thermal status was 0.
  - The display read 90.0 Hz on all 6 readings.
  - The installed build was `ece097ac…`, which is `a2f12f1`'s.

**The verdicts,** from `decide.py` `3f69c6a`, run in the run directory from 14:24:05 to 14:24:09 on
Python 3.10.0.
- **Span: HOLDS.** First stable frame (burst end): **HOLDS.** Skipped frames: **FELL**. "'a great
  deal' cannot be tested without a number, and none was given".
- `better_than_control` was true on both quantities, and `not_better_on_either` false.

| | Control `dea38cc`: `Mc-1` to `Mc-5` | 5d `a2f12f1`: `M5-1` to `M5-5` |
|---|---|---|
| span, ms | 2394.5, 2223.1, 2409.0, 2269.4, 2257.5 | 781.2, 786.3, 749.4, 684.1, 682.9 |
| burst end, ms | 3892.1, 4273.3, 4389.5, 3806.1, 3769.7 | 2037.0, 2370.4, 2155.0, 2126.8, 2127.5 |
| skipped frames | 287, 276, 315, 268, 248 | 91, 95, 87, 100, 87 |

- **The 25 span ratios, 5d over control,** ran from 0.2835 to 0.3537, against 0.7083. All 25 are
  below the threshold.
- **The 25 burst-end ratios** ran from 0.4641 to 0.6288, against 0.825. All 25 are below it.
- **The refresh rate was 90 Hz,** so "no burst" would have meant that no single stall reached 333 ms.
  Every launch showed a burst.

| Span ratio | `Mc-1` | `Mc-2` | `Mc-3` | `Mc-4` | `Mc-5` |
|---|---|---|---|---|---|
| `M5-1` | 0.3263 | 0.3514 | 0.3243 | 0.3442 | 0.3461 |
| `M5-2` | 0.3284 | 0.3537 | 0.3264 | 0.3465 | 0.3483 |
| `M5-3` | 0.3130 | 0.3371 | 0.3111 | 0.3302 | 0.3319 |
| `M5-4` | 0.2857 | 0.3077 | 0.2840 | 0.3015 | 0.3031 |
| `M5-5` | 0.2852 | 0.3072 | 0.2835 | 0.3009 | 0.3025 |

| Burst-end ratio | `Mc-1` | `Mc-2` | `Mc-3` | `Mc-4` | `Mc-5` |
|---|---|---|---|---|---|
| `M5-1` | 0.5234 | 0.4767 | 0.4641 | 0.5352 | 0.5404 |
| `M5-2` | 0.6090 | 0.5547 | 0.5400 | 0.6228 | 0.6288 |
| `M5-3` | 0.5537 | 0.5043 | 0.4909 | 0.5662 | 0.5717 |
| `M5-4` | 0.5465 | 0.4977 | 0.4845 | 0.5588 | 0.5642 |
| `M5-5` | 0.5466 | 0.4978 | 0.4847 | 0.5590 | 0.5644 |

- **The agent's pre-registered prediction held:** HOLDS on both quantities, `FELL`, and better on
  both.
- **By section 2 of the pre-registration, these are weak HOLDS.** Each carries the confound of L1
  to L4: the thresholds were easier to pass than intended if ρ's 2.4 s and 4.0 s held the catalogue
  parse. Whether they did is not known.
  - *Added 2026-11-05:* a bound on that confound now stands beside L1 to L4, in *After the
    re-qualification, 2026-11-04*. The span's HOLDS stands for every k the model admits, and the
    burst end's for every k below 2.114 s. Both stay weak. The sentence above stays as it was
    written.
- **The scope:** the span and the burst end only. No statement about which phase 5d improves is made
  from this measurement.
- **What the result supports,** as `decide.py` prints it: "Measured on debuggable, uncompiled builds
  whose JIT compiles during the window being measured. The result supports "the ingestion window fell
  by X on these builds" and does NOT support "users see X less on a release build". The control's
  window reaches compiled code while 5d's does not, so the measured ratio is conservative relative
  to a cold-against-cold comparison."

**Beside the verdicts, deciding nothing.**
- **The JIT thread's CPU across each span, in ms of CPU:**
  - control: 1390, 950, 1360, 1250 and 1220, over spans of 2223 to 2409 ms;
  - 5d: 880, 740, 790, 800 and 750, over spans of 683 to 786 ms.
- **The history rows' cost by process age,** in µs per row, by 250 ms bins of process age, with the
  rows in each bin in `decide.json`:
  - control, from 1250 to 2750 ms: 2414 to 3387, in every launch;
  - control, from 2750 ms on: 1761 to 2452;
  - 5d, all its rows, between 1250 and 2500 ms: 264 to 552.
  - The control's rows are cheaper from about 2750 ms of process age, and 5d's span has ended before
    then. That is the shape of the JIT confound the pre-registration names; it is recorded, not
    quantified further.
- **The pages' order:**
  - the control interleaved in `Mc-2`, 1 of its 5, and ran its pages one after the other in the
    other four;
  - 5d ran one after the other in all 5;
  - the excluded launch ran one after the other.
  - Recorded, not interpreted, beside the confound the pre-registration names.
- **Beside the 2026-10-09 figures, with no threshold, since no decision hangs on them:**
  - the control's span was 2223 to 2409 ms, against 2348 to 2490 then;
  - its burst end was 3770 to 4390 ms, against 3939 to 4245;
  - its skipped frames were 248 to 315, against 200 to 227.

**The 60 Hz readings, corrected, before the closing measurement's first launch.**
- **What was read.**
  - At 14:02, `section-read.sh` listed the display's app-request votes as one of 90.0 and three of
    60.000004 Hz, while the display read 90.0 on all six readings.
  - A read of the votes by display, at 14:02, showed that they are per display. Display 0, the
    phone's screen, carried the 90.0 cap; displays 2, 3 and 4 carried the 60.000004 caps.
  - At 14:23 the list held the three of 60.000004 only, and the display again read 90.0.
- **What that does to 12:03.** The readout of 12:03, before the negative control, had the same
  order: a 90.0 vote, then three of 60.000004. So it showed no app request capping the phone's screen
  at 60 Hz.
- **The claim falls.**
  - "Another app had requested that rate", in the negative control's results and in item 3 of
    *Before the closing measurement, 2026-11-04*, is not supported by the votes it was read from.
  - It was the agent's reading at the time. It was pushed in `a246727` at 14:01:46 and found wrong at
    about 14:02.
- **What remains.**
  - The cause of the 60 Hz readings from 12:01 to 12:03 is not known.
  - Another app's window being listed, and later not visible, is a coincidence in time, not a shown
    mechanism.
  - The hypothesis is not excluded: Android also lets an app ask for a rate through the compositor,
    which this readout does not show. That is from documentation, not observed here.
- **For `-2` of 2026-11-03:** the plausible explanation of item 3 is withdrawn, and its cause stays
  unknown.
- **`section-read.sh` was not changed.** It was hashed in the pre-registration. Its vote lines do
  not name the display, so read on their own they cannot separate the two cases.

**Published** in `experiment/startup-5d/closing-measurement-2026-11-04/` on both experiment branches:
`5d672ca` on `experiment/startup-5d-control`, and `53d9233` on `experiment/startup-5d-treatment`.
Both were read back from the remote at 14:26:24.
- **The files:**
  - `decide.json`, every value `decide.py` printed, in full;
  - `launches.txt`, every launch's saved idle record and sitting line, in sitting order;
  - `three-quantities.jsonl`;
  - `section-before.txt` and `section-after.txt`.
- **One edit, as before:** each launch record's local APK path writes its user directory as
  `%USERPROFILE%`. That is 10 lines. Line endings are LF.
- **Scanned before the push.** The added lines hold no user name, no account id, no token, no
  authorization text and no chat text. The channel appears only as the fingerprint `c8cb0e91`.

**Not done.**
- Nothing was decided from the verdicts.
- `RELEASE-STATE.md` was not edited. It still owes two entries: the measurement, with #84's deferral
  condition, and the phone's build, which is now `a2f12f1`'s (`ece097ac…`), read back at 14:23.
- The overhead launches were not run.
