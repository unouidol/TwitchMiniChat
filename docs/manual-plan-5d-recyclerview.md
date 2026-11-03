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
   wider than that margin;
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
