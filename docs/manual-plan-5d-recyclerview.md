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

**Since 2026-11-09 08:40 the long session's build is this one.** It was #97's head when it was built,
on `DESKTOP-BCT68L9`, under *one toolchain, not one machine* (below, 2026-11-09). No `nbpromec1` build
of this commit exists. Its build, install and the long-press's qualification are under *Recorded
results*, 2026-11-09, 08:34 to 08:44.

| Step | Branch | Commit | APK (dev, arm64) sha256, `DESKTOP-BCT68L9` |
|---|---|---|---|
| M, the long session | `feature/5d-recyclerview-timeline` | `00c1e78` | `ba1f88e3347382f76e4d5d1c0661602e2a24e187fcbdcfc2440e64ea88f16e4a` |

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

**The first machine again, read on 2026-11-09 for the long session's build:** `DESKTOP-BCT68L9`, the
home PC. Every item was read on it that day; none was taken from the 2026-11-02 block above.
- **Its key is provably the one `nbpromec1` matched against the phone.**
  - `~/.android/debug.keystore` holds one entry, `androiddebugkey`, certificate SHA-256
    `9b3a523bb6a20c80df9c3ceffbdcd072795c7409791bedfc1b7a0506578e5c51`, in full.
  - **The link, by hashes and certificates read here:** the archived `cc4c0cf` APK in
    `C:\Users\Fede\StudioProjects\tmc-5d-round\apks` hashes to `5cc84bb3…` of the first table, the
    build the phone held when `nbpromec1`'s key was compared in full with its signer. Its signer is
    the key above, in all 64 hex characters.
  - **The phone, read the same day:** the dev app's APK, pulled off it, is signed by the same
    certificate, in all 64 hex characters. So an install from this machine replaces it in place.
- **The clones: clean, and behind, which is a state and not a divergence** (the owner's ruling).
  `git status --porcelain --untracked-files=all` printed 0 lines in the clone and in its three
  worktrees. The clone's branch was 23 commits behind `origin/main-v5`, and `TwitchMiniChat-wt-5d`
  45 behind `feature/5d-recyclerview-timeline`; the worktree was fast-forwarded.
- **AGP 9.2.1 and Gradle 9.4.1**, from the tree.
- **The JDK matches `nbpromec1`'s, character for character.** `gradlew --version` printed `Launcher
  JVM: 21.0.10 (JetBrains s.r.o. 21.0.10+1-b1163.110)` and `Daemon JVM: Compatible with Java 21,
  JetBrains, nativeImageCapable=false (from gradle/gradle-daemon-jvm.properties)`. That file is the
  same in every tree here, with `toolchainVendor=JETBRAINS` and `toolchainVersion=21`. The daemon's
  JDK, `%USERPROFILE%\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2`, reports
  `JBR-21.0.10+1-1163.110-jcef`, and its `release` file `JAVA_RUNTIME_VERSION="21.0.10+1-b1163.110"`.
- **The Android SDK, `D:\Android\SDK`: android-36 revision 2, and build-tools 35.0.0, 36.0.0 and
  36.1.0,** the same as `nbpromec1`'s. The gap above, "no record exists for `DESKTOP-BCT68L9`", is
  closed.
- **Recorded so that the next comparison has a term, since none exists for `nbpromec1`:**
  - compileSdk 36, targetSdk 36 and minSdk 26, from the tree;
  - **Kotlin 2.2.10, carried by AGP and declared nowhere.** AGP 9.2.1's resolved metadata in the
    Gradle cache names `kotlin-gradle-plugin` and `kotlin-stdlib` 2.2.10, the only Kotlin compiler in
    the cache. The build scripts are compiled by Gradle's own Kotlin, 2.3.0. The finding is under
    *Backlog*.
- **The dev flavour's Firebase configuration matches the phone's.** `TwitchMiniChat-wt-5d`'s
  `app/src/dev/google-services.json` is byte-identical to the clone's, and its project id, project
  number and dev application id are those in the resources of the dev APK on the phone. The worktree
  `TwitchMiniChat-wt-docs` carries CI's placeholder and cannot build for the phone.
- **adb 37.0.1** (`1.0.41`), as on `nbpromec1`. The phone was not visible at first, while Windows
  listed a USB device failing to identify itself. By 08:12 it was listed as the OnePlus AC2003,
  authorised.
- **Android Studio 2026.2.1** (`AI-262.9437.185.2621.16467767`), the build that ran on 2026-11-02,
  open on the clone. Its log of 2026-11-09 shows no upgrade prompt.
  - **A past event, recorded as such:** on 2026-10-15 its Upgrade Assistant proposed AGP 9.2.1 to
    9.4.1, and Gradle 9.6.0 and AGP 9.4.1 were downloaded between 08:30 and 08:35. The clone's
    `libs.versions.toml` and `gradle-wrapper.properties` were rewritten at 09:45, and both now match
    the commit. No branch declares AGP 9.4. Applied and reverted; nothing is pending.
  - **AGP 9.4 and Gradle 9.6 stay queued for after the release,** with the JDK pin.

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

**The row regime, read on 2026-11-05, corrected on 2026-11-06.** On 2026-11-05 no automated test
checked the mapping by value at or past 400 rows. Since 2026-11-06 one does, for the mapping alone:
`ChatTimelineShownRowsPastTheCapTest`, below. These checks remain the only thing that exercises the
regime in the list itself: the list sync, the RecyclerView, scrolling, emotes and memory past 400 rows.
- **Production:**
  - **The page's start:** on a channel at the history source's cap of 400 rows, a page's timeline
    starts with 400 history rows. The cap is the backend's, which is not in this repository and was
    not read.
  - **The app sets no cap.** `ChatTimeline` has no capacity. Rows leave it one echo at a time
    (`removeEcho`) or all at once (`clear`), so live messages take it past 400, and it keeps growing.
    - **Beside it, the owner's reasoning, recorded as reasoning** (2026-11-06): a cap near 1,000 would
      keep the timeline and the deduplicator's capacity consistent, which is plausibly why Chatterino2
      sits there. It is not Chatterino2's stated reason, and whether Chatterino2 deduplicates against
      its buffer was not read.
      - **The agent's precision:** consistency needs the cap at or below the deduplicator's capacity,
        800 today (`ChatMessageDeduplicator.kt:80`). Then every row shown has its key kept. A cap at
        1,000 with the capacity left at 800 would leave up to 200 shown rows without a kept key.
      - It is also item 8 of the long session's pre-registration (*Recorded results*, 2026-11-06).
  - **The measured scenario's 800 rows** were two pages of 400, so two timelines, not one of 800.
- **The tests, by reading, with the largest timeline each walk printed on its last clean run
  (`4eb73f8`):**
  - `ChatTimelineViewSyncShownRowsDifferentialTest`: 240 rows.
  - `ChatTimelineListSyncDifferentialTest`: 201.
  - `ChatTimelineShownRowsPropertyTest`: 112.
  - `ChatTimelineShownRowsTest`: 7 rows in the literal cases, 1 to 59 in the reference case.
  - `ChatTimelineInsertDifferentialTest`: 323, but it tests the timeline's order, not the mapping.
  - **One test crosses 400:** `ChatTimelineShownRowsCostTest`. It runs at 400, 1,000 and 6,400 rows
    with one row inserted at the front, the middle or the end, and grows a timeline from 400 to 800
    by appends.
    - **What it checks:** how many rows an insert reads, and the round trip of the inserted row.
      `timelineIndexOf` of `inserted`'s position must give the index back.
    - **What that catches:** a fault in the forward mapping alone, at that row, fails it there,
      because the inverse is the test's own.
    - **What it does not check:** any position by value, any other decider, or the list sync. A fault
      matched in both directions passes it, as M8 did.
  - **Added on 2026-11-06, tests only: `ChatTimelineShownRowsPastTheCapTest`.** It checks the mapping
    by value, both ways, at every row and every position.
    - **Its two cases:**
      - the reference case's shape, with system lines added, at 400, 401, 800, 801 and 1,600 rows,
        eight seeds each;
      - one timeline of 801 rows whose answers are counted by hand, not by `ChatTimelineVisibility`.
    - **M8 fails both.** But M8's fault is at positions 0 and 1, which the small cases already catch,
      so that shows only that the test checks values.
    - **M9 shows what it adds.** M9 is a fault matched in both directions that appears only past 400
      rows: it swaps the last two shown positions. Of 634 stable tests it fails exactly these two.
      `ChatTimelineShownRowsCostTest` and every other test pass it.
    - **What it still does not check:** the list sync past 400 rows, or anything on the device.
- **So these checks exercise the regime only when they run where it occurs:** on a channel at the
  history cap, with live messages arriving, so that the timeline passes 400 while rows are hidden,
  shown again, echoed and scrolled.

- **Far up and back.** Scroll far up in a busy chat and back down: emotes come back in rows the list
  rebinds from the pool, on Glide's real timing.
- **Scrolled up while messages arrive.** The rows on screen stay where they are, the jump button
  counts the new rows, and tapping it returns to the bottom and clears the count.
- **A tap on empty space clears the channel field's focus.** With the channel field focused, tap
  between rows, on a system line, and below the last row of a short chat. Below the last row is new:
  the old container ended at its last row.
- **The same click, through TalkBack** (added 2026-11-05: the owner's condition for keeping the
  `ClickableViewAccessibility` suppression at `attachEmptySpaceTaps`; approved by the owner the same
  day, with `reason=outside_click` as its decider). This tests what the check
  protects instead of declaring it met. The check asks for `performClick` because an accessibility
  service clicks through it. The list's click is the inherited `View.performClick`, which the check
  cannot see.
  - **TalkBack is a phone setting.** Turning it on needs the owner's say-so. Turn it off again
    before anything else in the round.
  - **Setup:** a channel whose messages fill the list. The channel field holds text and is focused,
    and the keyboard is open. Logcat runs with the tag `CHAT_FOCUS`.
  - **Act:** with TalkBack on, move its focus to the chat list itself, not to a row: swipe through
    the screen's elements until TalkBack announces the list. Then double-tap.
  - **Pass:** the keyboard closes, the channel field empties, and Logcat shows
    `composer focus guard disarmed reason=outside_click` at that moment.
  - **Why the Logcat line decides it, read from the code:** only the list's own listener,
    `clearChannelFocusClickListener`, writes `reason=outside_click`. A message row's click,
    `messageClickListener` (`ChatFragment.kt:772` to `775`), also closes the keyboard and empties
    the channel field, but it writes no such line.
  - **Why a full list matters, reasoned and not seen:** TalkBack's double-tap asks the focused
    node for `ACTION_CLICK`, and `ACTION_CLICK` reaches `performClick`. If TalkBack instead fell
    back to a simulated tap at the node's centre, in a full list that tap would land on a message
    row. That TalkBack prefers `ACTION_CLICK` is its documented behaviour; it was not read here.
  - **Fail, focus reached:** the list is announced, but the double-tap leaves the keyboard open, or
    writes no `outside_click` line. Then the suppression is wrong, and the check has found a real
    cause. Stop and report.
  - **Fail, never reached:** TalkBack never announces the list as an element. The click is then out
    of TalkBack's reach altogether. That is a defect of its own, not the one the check names.
    Record it, and leave it to the owner.
- **Swipe-to-reply inside the list,** on a message with an id.
- **An echo sent and confirmed,** and the same echo scrolled far away and back.
- **Hide a user, then show them again** from the blocked-users screen.
- **A long session, past 400 rows** (added 2026-11-06). No round recorded here has run it, on any
  build. It is the regime the row-regime note above names, and the one where recycling views matters
  most.
  - **Setup:** the final build, on a busy channel whose history arrives at the cap of 400 rows. The
    page stays current with the screen on, and no user is hidden, so `shownRows` counts every row.
    - **Two pages on one channel, accounts A and B,** by the owner's ruling of 2026-11-09, later that
      day. Two timelines are the architecture, not a choice: the owner's architecture-debt entry B,
      in the owner's `claude/architecture-debt.md`, which is not in this repository.
      - **The owner's earlier ruling, one page, account A, FALLS.** It rested on every launch since
        2026-11-06 starting only A's page. Why it falls, by the phone on 2026-11-09 at 08:42: a launch
        opens the accounts page, beside which only A's page is attached. The long-press needs A's page
        on screen, and the pager keeps one page attached on each side (`offscreenPageLimit = 1`,
        `MainActivity.kt:88`). So B's page was attached and connected 157 ms after the swipe, with a
        first connect of 400 rows on the same channel.
        - **Its reason, kept as given:** one growing timeline gives R = 2.71 with bands at 1.43 and
          1.90, against R = 1.92 with 1.20 and 1.60 for two pages with one idle.
      - **Only the page on screen can be pressed.** A's page is on screen at every reading, so B's
        page writes no `user.marker` unless it is brought on screen; left alone, it grows unread.
      - **B's count is MEASURED, not assumed,** by the owner's ruling of 2026-11-09, declared before
        any poll. R depends on the total rows: at the planned counts, B growing as A does gives
        R = 2.776 with bands at 1.442 and 1.924, and B held at 400 gives R = 1.888 with bands at
        1.190 and 1.587 (C = 101). Those are two different criteria. That B grows is well founded,
        since it stays STARTED with its IRC connection open, but well founded is not measured.
        - **The method, at the FIRST and at the LAST reading only:** after A's long-press and dump,
          with A on screen, swipe to B's page, long-press B's refresh, and swipe back to A. Outside any
          refresh bracket. Every dump is taken with A's page on screen, as at the other readings, so
          that the attached pages are the same at every dump.
        - **Recorded with it:** B's `shownRows`; the backfill each swipe triggers, its `source` and
          `messageCount`, for B coming on screen and for A coming back; and whether B, a page never
          before on screen or paused, triggers one at all.
        - **The fallback,** used only if the first reading's press on B writes no line, or its line
          cannot be told from A's: the verdict is reported under both bounds, N_B = N_A and
          N_B = 400, and it is INCONCLUSIVE if they disagree. It then holds for the whole session.
        - **Declared by the agent before the session: the measurement is what will be done.** The
          fallback stands behind it, in the case above only.
      - **R = 2.776 with C = 101, primary, and 2.845 with C = 67 beside it,** better discrimination
        than one page's 2.71 at no cost: more rows dominate C better (*C's configuration*, below).
      - **"A second page", below, stays,** as the text this configuration now uses.
  - **How long:** until the timeline holds at least 1,200 rows, 800 live rows past the history, with
    messages still arriving. The owner kept that bound on 2026-11-06: past Chatterino2's limit of
    1,000 is the mark that means something. That limit, and what Chatterino2 does at it, is recorded
    under *Recorded results*, 2026-11-06, "Chatterino2's scrollback cap".
    - **What it costs, as planned on 2026-11-06:** 12.3 rows a minute, the rate after dump B in the
      instrument's sitting (*Recorded results*). At that rate, 800 live rows take about 65 minutes.
      The 6 to 24 a minute recorded on 2026-11-03's quiet channel would take 33 to 133. So this is a
      leave-it-running case, not a hands-on one.
    - **The 169 rows in one minute are not relied on,** and the case assumes that no such minute comes
      (*Recorded results*, "the 169 rows in one minute"). A burst would shorten the session, but the
      plan does not count on one.
    - **The rate is still read at the start,** from the first two long-presses some minutes apart, and
      the next long-presses are timed from it. A busier channel would be faster, but no busier
      channel's rate exists in the records.
    - **Not used:**
      - rows the harness injects, since the question is about a real session;
      - the tester's own messages, which would be spam, and whose rows are echoes, not messages.
    - **The two reading hazards, by reading:**
      - **A change of channel** clears the timeline (`ChatChannelBinding.changeTo`). A reconnect or a
        refresh does not.
      - **The row-count reading is a long-press on refresh.** A tap starts a manual refresh.
        - **A tap by mistake is not silent:** it writes `backfill.triggered` with
          `source=manual_refresh`, which the session's poll prints, and the history it brings is
          counted apart like any other (below).
    - **The screen is held on by USB power, read and not changed.** `stay_on_while_plugged_in` read 7
      (AC, USB and wireless) on 2026-11-06, with `dumpsys power` showing `mStayOn=true` on USB. It
      read the same on 2026-11-02, and 5c's plan records it the same way. So "Stay awake while
      charging", which the owner approved turning on, is already on, and nothing is changed.
      - **The agent's error:** it gave the display timeout as the obstacle without reading this
        setting, which the round had already recorded. The timeout governs only off USB power. It
        read 15 s early on 2026-11-06, and 30 minutes from 09:20 the same day, a change the agent did
        not make (*Recorded results*, the instrument's sitting).
      - **Verified before and after,** like the sitting's other parameters: the setting, and USB power
        from `dumpsys battery`. If USB power is lost, the screen is no longer held, and that is
        recorded.
      - **By reading:** the app has no foreground service or wake lock, and the page's stop handler
        does not close the connection. Whether rows keep arriving while the app is stopped was not
        examined. With the screen held, the question should not arise.
    - **A realistic worst case, not a normal usage condition.** The screen is held awake for about 65
      minutes as planned, and up to the session's cap (below), on a chat with animated emotes, so their
      frames are decoded for as long as the session lasts. That costs battery and heat, on purpose, and is recorded as a condition of the
      session, not as a neutral parameter.
      - **The owner called it the path of the measured battery defect.** By the record, that holds
        for the work and not for the state. #69's measured defect was animated emotes decoding after
        Home, in the background (5c's B1). Here the app is in the foreground with the emotes on
        screen, where decoding them is intended.
      - **So nothing here tests #69.** B1 does, at the end of the round.
      - **Read at each reading, beside the counts:** the battery's temperature and the thermal status.
    - **The session's parameters,** read with the round's `sitting.sh state` before the mark and after
      the last reading, like the sitting's:
      - battery saver; AC, USB and wireless power; the battery's status, level and temperature;
      - the thermal status;
      - the display's rate;
      - **"Stay awake while charging",** `stay_on_while_plugged_in`: approved by the owner, and already
        on at 7, so read and not changed (above);
      - the display timeout, `screen_off_timeout`, with `dumpsys power`'s wakefulness and `mStayOn`;
      - the build on the phone: its APK's sha256, `versionCode`, `versionName` and last update time.
      - **The interpreter,** added on 2026-11-09: `PY`'s value and the full version it reports,
        `$PY -c "import sys; print(sys.version)"`, read before the mark and after the last reading,
        and recorded like the display and the battery. It is read by the driver, not by `state`:
        adding it there would be a tool edit outside the carve-out for how pages are read.
        - **For this session:** `PY="py -3.14 -I -X utf8"`, which reports `3.14.0
          (tags/v3.14.0:ebf955d, Oct  7 2025, 10:15:03) [MSC v.1944 64 bit (AMD64)]`, CPython
          64-bit, `D:\python\python.exe`, on `DESKTOP-BCT68L9`.
        - **A parameter, not a tool edit:** `sitting.sh:37` reads `PY` from the environment with a
          default, `PY="${PY:-py -3.10 -I -X utf8}"`, so setting it leaves the tool's bytes as they
          are.
        - **Qualified for the behaviours the suite's cases are DEMONSTRATED to look at, and only
          those.** Restated on 2026-11-09 after the mutation check (*Recorded results*, 2026-11-09,
          the mutation check), replacing "the behaviours the offline suite covers", which claimed
          more than was shown.
          - **Demonstrated by C1 to C15, each by a mutant they killed:** the mark excludes an earlier
            session's lines and is taken in the journal's own clock; it refuses a running app, and is
            exactly one instant; a label is never reused; a poll needs a mark; the process is bound
            after the mark, and is alive at every poll; a failed read stops the poll, at
            `sitting.sh`'s check of the pipe; the lines saved earlier must come first; a line outside
            the grammar stops the poll, and the extractor prints only whitelisted fields; the shape of
            `messageCount`; a rejected `messageCount` leaves no bound; the cap; no rate is read with
            another app in front; the rate is recorded with its range; a second call continues the
            labels and the saved extract.
          - **Demonstrated only by the P cases:** LIVE as the rows less the history before them, and
            the target reached at exactly the target.
          - **Demonstrated since, later on 2026-11-09** (*Recorded results*, the stand-in corrected):
            - **the extractor's exit on an empty input, the guard an unreadable journal on the phone
              relies on.** `adb exec-out` exits 0 on a failed read, with the error on stdout, as
              measured on the phone; the stand-in exited 1, so C6 and C14 never reached that guard.
              Corrected to the measured behaviour, they kill M11.
            - **a process replaced during the session,** by a new pid (M26, killed by R1) or by the
              same pid started again (M09, killed by R2).
          - **NOT DEMONSTRATED, and accepted with its reason, by the owner's ruling of 2026-11-09:**
            - **The front read again 2.2 s later.** In this session the refresh rate is not a void
              condition but a parameter recorded with its moment, so a spurious 60 Hz reading would be a
              wrong parameter, not a wrong verdict.
            - **The six sources.** Defence in depth with one layer proven: an injected source would
              pass the extractor and be stopped loudly by the grammar, whose check is demonstrated (M13,
              killed by C4).
          - **The cap's kill belongs to the harness, not to a case.** M20, the cap removed, was killed
            because C7 never ended and the driver timed it out. C7 asserts the cap, exit 4 and
            `CAP REACHED`, only once the poll returns, which with no cap it never does, so it cannot
            make this a case-level kill without being rewritten. A broken cap in the real session
            would show as a session that never ends, which Federico would see: not silent.
          - **A decided exception: the extractor's `\d` stays as frozen, matching non-ASCII digits, on
            purpose, not a guard left out.** By the owner's ruling of 2026-11-09, refused on the
            project's own rule and not on scope: an impossible case is not guarded against, it is
            proved impossible.
            - **The proof, from the code at `00c1e78`:** the four count fields are `Int` and `Long`
              values. `messageCount` is `result.messages.size`, `firstTs` and `lastTs` are `toLong()`
              values (`ChatFragment.kt:2959-2961`), and `shownRows` is `shownRowCount: Int`
              (`ChatFragment.kt:1426`, `ChatTimelineController.kt:91`). Each is written with
              `value.toString()` (`HistoryDiagnosticsLog.kt:108`), which is ASCII whatever the locale,
              and `sanitize` keeps a digit as it is (`:272`).
            - **The proof, from the data:** the phone's journal through the committed extractor, 3,201
              lines read, 1,380 kept, none outside the grammar, and not one non-ASCII byte in 403,784.
            - **And if it happened anyway,** the grammar would stop the poll loudly with
              `INVARIANT VIOLATED [extract]`.
            - **So the proof and the loud stop stand in place of a guard.** Changing `\d` to `[0-9]`
              would break the frozen hash for a case shown not to arise.
          - **Not in the suite at all:** `rows`, `dump`, `state` and `poll`. Of them only `rows` runs
            Python, and the long session does not use it.
      - **The worst-case note above goes with them, as corrected:** the screen held awake on animated
        emotes is a condition of the session, and nothing here tests #69.
  - **Three readings:** after the history (about 400 rows), at about 800, and at 1,200 or more. Each
    takes:
    - **The row count:** long-press refresh. It writes `shownRows` into the journal's `user.marker`
      event, which the session's poll reads from the journal (below).
      - **The instrument PASSES,** qualified on 2026-11-09 on `00c1e78` before the session was
        committed: four presses wrote four lines with `shownRows` and no `renderedRows` (*Recorded
        results*, 2026-11-09, 08:34 to 08:44).
        - **Its timing:** each line was in the file 19 to 119 ms after its own instant, within one
          polling interval of about 0.11 s. The true delay is below what the polling resolves, so the
          poll waits for nothing.
        - **Repeated presses:** presses 4.2 s apart each wrote a line.
    - **Memory:** `adb shell dumpsys meminfo` for the package of the build on the phone. It records
      the total PSS (proportional set size), the Java heap, and the `Views:` count under Objects. It
      reads and changes nothing.
  - **The session's poll, once a minute, by the round's `sitting.sh`,** with `journal_extract.py`, in
    `%USERPROFILE%\tmc-5d-round\V`, outside this repository, frozen at sha256 `854dca02…` and
    `f5abb492…`. Written on 2026-11-06. Nothing of it has run on the phone.
    - **Committed unchanged on 2026-11-09, and on one disk only until then** (*Recorded results*,
      2026-11-09, the tools committed). Their identity is the full sha256 from then on:
      - `sitting.sh` `854dca023884bc3dfe6f643d28b019fe0bda4f95f5df1bf4ec17d02a28d93164`, blob
        `e952ac23924f5bab51c3511a37a6a3e9eb3fae9b`;
      - `journal_extract.py` `f5abb4920daaccd9d9a9f1a9448fb705ae47d1ce28ba55dc71f778253c2cc678`,
        blob `b39667635d6f94892a241e8acd57001280a37b9c`;
      - on `experiment/startup-5d-control` at `613ff07` and `experiment/startup-5d-treatment` at
        `d467f2e`, in `experiment/startup-5d/`.
    - **It runs from `tmc-5d-round\V`, never from a clone:** `sitting.sh` writes its outputs beside
      itself (`sitting.sh:35`), which is why that folder sits outside every clone. The `adb` on
      `PATH` there is a second SDK install's, `%LOCALAPPDATA%\Android\Sdk`, byte-identical to
      `D:\Android\SDK`'s 37.0.1, so there is no adb server conflict.
    - **Changed on 2026-11-09 at 10:42, before any poll, only in how pages are read** (*Recorded
      results*, 2026-11-09, the poll counted per page). The session runs these:
      - `sitting.sh` `13cb9ff5394f3fddcf3f67e57d3c6befc1a4a28e4357d33c0cb542852e567a8f`, blob
        `0c7262a306f2c68d9459d4e2f6f73360d8047aa5`, replacing `854dca02…5d93164`;
      - `journal_extract.py` `340e263f1397e8c441d80090aca5d3b383187ef267466c471e182787deaa6e43`, blob
        `f6b5e614efdc78f9b190d832c50fa58ed5ce0f63`, replacing `f5abb492…c2cc678`;
      - on `experiment/startup-5d-control` at `bff7379` and `experiment/startup-5d-treatment` at
        `6dc2351`. The frozen versions stay at `613ff07` and `d467f2e`.
    - **The order:**
      1. `state` before;
      2. `session-start <label>`, with the dev app not running: the mark;
      3. the launch onto the channel, then `bind`;
      4. `session-poll 800 8`, called again after each return until it reports its target. Each call
         stays under the shell's ten-minute limit, in the foreground, and continues the last: its
         labels, its saved extract and its rates file carry over;
      5. `state` after the last reading.
    - **The session's cap: 140 minutes,** approved by the owner on 2026-11-06. It is past the 133
      that 6 rows a minute would take. The session ends at its target or at its cap, never on a
      `Views:` reading (the agent's addition, under the bands below).
    - **Expected input: exactly the session's own presses,** by the owner's ruling of 2026-11-09.
      - **The "0 input events" rule is the start-up measurement's** (section C), where any touch is a
        confound. Here the press is the instrument.
      - **The session's own presses:** each reading's long-presses and the swipes between its two
        pages, the refresh's one tap, each reading's scroll, follow and jump checks, and the hide,
        show and send once at 1,200 rows. Only input beyond them counts.
      - **By how `getevent` reads, not tested here:** it reads the kernel's input devices, so presses
        injected with `adb shell input` do not appear in it, and presses by hand do. With presses
        driven over adb, any `getevent` event is beyond the session's own. With presses by hand, the
        expected ones appear, and are matched to the procedure's moments.
    - **First, at every poll, the invariant:** the bound process is alive, with the pid and start time
      bound (*Recorded results*, "the poll's silence, and its invariant").
    - **The row counts and the journal, read in one pass.**
      - **Extracted, and nothing else:**
        - `backfill.triggered`'s `source`, only as one of the six values that
          `HistoryBackfillPolicy.kt:5-10` writes;
        - `backfill.result`'s `messageCount`, `firstTs` and `lastTs`;
        - `user.marker`'s `shownRows`, the row count this case already reads from the journal.
          **Approved by the owner on 2026-11-06 as an extracted field: it is a count, not a name.**
          It was flagged, not included quietly, since the owner's list first named only the two
          backfill events;
        - each line's moment. No account name, no channel name, no message text.
      - **How:** the journal is piped from the phone into `journal_extract.py`, and never written raw.
        The extractor prints only what it builds from fields whose values match their shape in the
        code. The poll then checks every extracted line against that grammar, and saves nothing of a
        read that holds a line outside it.
      - **From the mark:** the instant of the journal's last line before the launch, in the journal's
        own clock. So the first connect's history is in the extract, and no earlier session's line is.
        Every poll re-reads the extract from the mark, and the lines earlier polls saved must come
        first and unchanged: a journal that rotated past them stops the poll.
      - **History counted apart from live rows:** at each long-press, the messages delivered by every
        `backfill.result` before it are summed. History rows shown are at most that sum, since the
        deduplicator can only remove rows, so `shownRows` less the sum is a lower bound on the live
        rows. A result whose `messageCount` was rejected makes it no bound, and the poll prints it
        UNKNOWN. The target is 800 live rows by that bound.
      - **With two pages, what the frozen tools do: read on 2026-11-09, once committed, and not
        changed.**
        - **The extract carries no page identity:** `journal_extract.py` keeps `source`,
          `messageCount`, `firstTs`, `lastTs` and `shownRows`, and drops the account and the channel
          (`journal_extract.py:22-26`).
        - **The poll counts the two pages as one** (`sitting.sh:262-281`). Its history is every
          `backfill.result` since the mark, both pages'. Its row count is the LAST `user.marker`,
          whichever page wrote it. Its LIVE is that count less all the history before it.
        - **So LIVE, at A's press, is A's rows less both pages' history.** It is still a lower bound
          on A's live rows, but low by B's first connect, 400, and by every backfill B and the swipes
          bring. The target of 800 is reached near 1,600 rows on A instead of 1,200: about 33 minutes
          later at 12.3 a minute, and not before the cap at 6 a minute. The session still ends on its
          target or its cap, never on a `Views:` reading.
        - **After a press on B,** the last marker is B's, so LIVE is computed on B's rows until A's
          next press. While both pages' history exceeds B's rows, it is negative and cannot reach
          the target.
        - **The pages' markers can still be told apart by order.** The extract keeps every line's
          order and moment, and the procedure fixes A's press before B's at the first and the last
          reading. So N_A and N_B can be read from it without a change, provided nothing else
          presses between them.
        - **A backfill of B's inside the refresh bracket** cannot be attributed, but shows as a second
          triggered and result pair inside it, which makes the bracket VOID.
        - **A change that would touch only how pages are read, proposed and not made:** the extract
          prints a page label, A or B by first appearance since the mark and never the account, and
          the poll counts history and LIVE per page. Whether to make it is the owner's, under the
          three conditions recorded on 2026-11-09.
        - **MADE, on 2026-11-09, by the owner's authorisation, before any poll.** The reason, the
          owner's: at 6 rows a minute the session would reach the cap without its target, so a
          bookkeeping error of about 400 rows could produce DROPPED for a reason unrelated to the
          question.
          - **Now:** every extracted line carries `page=A`, `page=B`, … by order of appearance after
            the mark, or `page=?` without an account. Each page's history is set against its own
            rows; history with no page is set against every page, keeping each bound a lower bound.
            LIVE, the target, is page A's.
          - **A new invariant, `pages`:** the first long-press after the mark must be on page A, the
            first page connected after it, which is the page measured.
          - **So the target returns to A's live rows reaching 800, A at about 1,200 rows,** and the
            floors stay as pre-registered: 1,101 rows summed at C = 101, 1,090 at C = 67.
          - **No threshold, band or verdict rule moved.** Unchanged: the events and fields
            extracted, the target given to `session-poll`, the cap, the poll's interval, the rate
            rule, the mark and every other invariant.
      - **A read that fails is never "nothing new":** an unreadable journal, an empty extraction, or a
        line outside the grammar stops the poll with `INVARIANT VIOLATED`.
    - **The refresh rate, at every poll, with its moment. Not a void condition,** by the owner's ruling
      of 2026-11-06.
      - **The dev app stays in front for the whole session.**
      - **The rate is read only after the dev app has been in front for more than 2.1 s:** in front at
        two reads 2.2 s apart, then the rate and its range, then the front read once more. What
        happens between the two reads is not read.
      - **A poll that finds another app in front** prints `RATE NOT READ` with its moment, and the
        session goes on.
      - **A change of rate** is placed between two polls. Whether the segments around it stand is the
        owner's ruling.
    - **The suite that ran these cases was never committed,** and was not found on
      `DESKTOP-BCT68L9` on 2026-11-09. It was REBUILT that day from the descriptions below, as C1 to
      C15, with the two-page cases P1 to P6 and a whitelist check L, and committed beside the tools:
      `sitting_suite.sh` `dcb47e983395d61660ec875cc1a490022cbf04aed458200c5f5ee2090bca81bd` and
      `standin_adb.sh` `17dfbfca806b0830e049b5ff198301106ffafe1c9ecc780c12520a4988a61b1d`. A rebuild
      from the descriptions, not the suite of 2026-11-06 (*Recorded results*, 2026-11-09, the poll
      counted per page).
      - **The original was found on `nbpromec1` on 2026-11-11,** outside the repository, in the
        scratch folder of the session that wrote it. Its runner was an inline command, never a file.
        Its 15 expected exits are the rebuilt C1 to C15's, and it asserted the exit code only
        (*Recorded results*, 2026-11-11, the original suite found).
      - **As the session runs it, since 2026-11-09 at 11:59:** with the stand-in corrected to the
        phone's exit 0 on a failed read, and the replaced-process cases R1 and R2 added, 24 cases:
        `sitting_suite.sh` `99dd85509f62bb2d0f61b09aefaef81e0092a9068f5c0bd82d361f8a64e33e3e` and
        `standin_adb.sh` `84c8205e23bcf68a264b7d957eccf841bccd63361c5a2107a90e2fddc99f5405`, at
        `e519a56` and `e4d953f`. Their mutation check is in `experiment/startup-5d/mutation/`.
    - **Shown to work offline, against a stand-in for adb, in 15 cases,** each ending with the exit and
      the message expected:
      - a healthy session, with an earlier session's lines in the journal that the mark excludes;
      - another app in front; the process gone at the second poll; a poll's line outside the
        whitelist; a rate of 60 with the range [0 60]; an unreadable journal at a poll;
      - a rejected `messageCount`, which leaves no bound until the cap; a journal rotated past saved
        lines;
      - a mark with the app running; a process bound before the mark; a reused label; a poll with no
        mark; two consecutive calls of one session;
      - a mark whose journal is unreadable, or whose extractor returns something other than one
        instant.
      - **What it extracted was checked:** the 37 files the cases saved, 150 lines, hold no account,
        channel, text or other field outside the whitelist. Their input was a synthetic journal with an
        account and a channel on every line, an injected `source`, a malformed `messageCount` and a
        text field.
    - **Not yet exercised on the phone:** reading the journal with `run-as`. The mark is its first
      read, and fails loudly if it reads nothing.
      - **The read itself was exercised on 2026-11-09,** by the agent's `run-as … cat` on
        `DESKTOP-BCT68L9`, not by the session's tooling. That tooling was then on `nbpromec1` only;
        since 2026-11-09 it is committed, and in `C:\Users\Fede\StudioProjects\tmc-5d-round\V` on
        `DESKTOP-BCT68L9` (above).
    - **Duplicates:** what this session can say about them is pre-registered (*Recorded results*,
      2026-11-06, "the long session's tooling, and its duplicate prediction").
      - **Eviction is closed by reading,** the owner's of 2026-11-09: the backend returns the newest
        messages of a window (*Recorded results*, 2026-11-09). Given its premise, that the backend
        stores every message the app showed, no tap or window of this session can produce a duplicate
        by eviction.
      - **The mismatch refresh, added by the owner on 2026-11-09.** Mismatch is now the only open
        route, so this one tap is the session's whole duplicate test, not an extra. Its two values
        were set by the owner the same day, derived from the rate rather than chosen.
        - **What:** one tap on refresh, between two long-presses 10 s apart, with nothing else
          touched. It counts only if its `backfill.result` falls between them.
        - **When:** once, after AT LEAST TWO readings, not after the first, so that the rate the
          threshold rests on is measured rather than planned. "Readings" are this section's three
          readings (settled on 2026-11-09), so the refresh comes after the reading at about 800 rows.
        - **Nothing else in the bracket, a page change included:** a page coming on screen triggers
          a `resume_no_pause` backfill (seen on 2026-11-09 at 08:42:07).
        - **The bracket, 10 s, provided it contains a refresh's response. On the records it does:**
          - **No refresh is recorded.** The phone's journal, 3,185 lines from 2026-10-05, holds no
            `manual_refresh`. A refresh uses the same history client as every recorded source
            (`ChatFragment.kt:1480`), so the figure is theirs: 686 requests, from trigger to result or
            failure, median 418 ms, 95th percentile 789 ms, maximum 5,021 ms, none above 10 s. The
            slowest source is the first connect, whose window is an hour and whose response is 400
            rows; a refresh's window is 120 s.
          - **Not bounded by the code:** the client's connect and read timeouts are 5,000 ms each
            (`BackendHistoryClient.kt:191-192`), and a read timeout applies per read, so the code alone
            does not cap a request at 10 s. A response later than the bracket voids it; it cannot
            miscount it.
          - **The result line comes before its rows.** `backfill.result` is written on the worker
            thread (`ChatFragment.kt:2956`), and each row is then posted to the UI (`:2965-2990`). So
            the second long-press must come after the rows are shown, not only after the line, and
            the tap belongs at the start of the bracket.
        - **What is judged:** Δ, the change in `shownRows` across the bracket, against E, the live rows
          expected from the MEASURED live rate over the bracket's length. With no mismatch Δ is live
          rows alone. A refresh whose every copy mismatched adds its 120 s of rows on top, about 25 at
          12.3 a minute.
        - **The bands, settled by the owner on 2026-11-09 and tied to the measured rate.** With E
          unrounded: Δ below 2E is consistent with no mismatch; from 2E to below 4E it is
          INCONCLUSIVE, reported as inconclusive and not argued either way; at 4E or more it is
          mismatch, and the case FAILS.
          - **The check:** at E = 2 these are the owner's first bands, 0 to 3, 4 to 7 and 8 or more.
            At the planned E = 2.05 they are 0 to 4, 5 to 8 and 9 or more.
          - **Why a rule and not numbers:** fixed bands break as soon as the rate moves. At 24 rows a
            minute, a 10 s bracket with no mismatch reaches 4 rows in 57% of brackets.
          - **They replace** the first form of the same day: FAIL if Δ exceeds 4E, with 0 to 3, 4 to 7
            and 8 or more at the planned rate.
        - **E's rate:** the live rows between the two readings before the refresh, over the time between
          them, from the poll's live-row bound.
        - **What "the case FAILS" means:** the test's verdict, mismatch found. It is recorded under the
          pre-registration's item 7 as a candidate pre-existing defect, not as a 5d failure. At
          `00c1e78` the deduplicator, the backfill policy, the history client and the channel binding
          have the blobs item 7 recorded on `main-v5`: `4532635`, `711eb3e`, `75b05d6` and `693562b`.
        - **Reported:** the bands computed from the measured rate at the moment of the refresh, and the
          planned figures, both.
        - **The noise, the agent's arithmetic under a Poisson model, which bursts would widen:** at
          E = 2.05 with no mismatch, Δ reaches the inconclusive band, 5, in 5.7% of brackets and the
          mismatch band, 9, in 0.03%. A total mismatch falls below 9 with a probability of 2 × 10⁻⁵.
        - **What it costs the target, by reading the rule above, not run:** its `messageCount` enters
          the history sum, so the bound on live rows drops by the copies it suppresses, up to about 25
          rows, 2 minutes at 12.3 a minute.
        - **What it cannot show:** eviction, which is closed by reading anyway.
  - **The view count's instrument: `Views:` in `dumpsys meminfo`. It is not qualified.**
    - **What it counts.** This was read in Android 12's platform source (AOSP `android12-release`),
      not in this phone's OnePlus build.
      - `ActivityThread.dumpMemInfo` runs `runtime.gc()` first, "since countInstancesOfClass counts
        unreachable objects".
      - It then prints `Views:` from `ViewDebug.getViewInstanceCount()`, which is
        `VMDebug.countInstancesOfClass(View.class, true)`.
      - So it counts every instance of View or a subclass in the process, attached or not: the list's
        laid-out rows, its cache, its recycled pool, and every other view of the app, both pages
        included.
    - **It has never been run in this project.** Neither plan uses it, and it has not been seen on
      this phone.
    - **What qualifying it takes, before the case can decide anything.** This was rewritten on
      2026-11-06, before the sitting. Its results are under *Recorded results*.
      1. **Present:** one dump on this phone shows the `Views:` line.
      2. **Repeatable, against the band gap:** three dumps with the list at rest give counts whose
         largest over smallest is `f`.
         - **The rule:** f² must be smaller than 1.155², the width of the inconclusive gap (below), so
           f < 1.155.
         - **Why:** one reading of the ratio uses two counts, each anywhere in that spread, so it
           lands within a factor f of the truth either way, a span of f². A span as wide as the gap
           means the instrument cannot resolve the distance between pass and fail.
         - **Reported beside it, not deciding:** f against M/1.155, the margin from each prediction to
           the edge of its own band. If f exceeds it, a true prediction can read as inconclusive.
         - **What this spread does not cover:** how many rows the screen holds at each reading. It is
           the dump's own repeatability.
      3. **k, the views per row, on a build without recycling:** the control measurement build
         `dea38cc` (APK `d005e901…`), which is pre-5d by construction. It is installed with the
         owner's approval.
         - **The dumps:** two, at least 200 rows apart, counting both pages' rows together.
         - **The rows at each dump:** each page's `chatContainer` children, read from
           `dumpsys activity top` just before and just after the dump. On that build, every timeline
           row is one child.
      4. **C, the views that are not rows.** As asked, this is one dump with a short timeline. That
         dump cannot be taken on that build (*Recorded results*, 2026-11-06). So C is the intercept of
         step 3's two dumps: C = V − k·N at either.
      - **R, M and the bands:**
        - R = (C + 1,200k) / (C + 400k), the no-recycling prediction for one timeline;
        - M = √R;
        - pass at or below M/1.155, fail at or above M·1.155, and inconclusive between.
      - **The abandon condition: R below 1.334,** set by the owner on 2026-11-09, replacing 1.3. Below
        it the view criterion is DROPPED, not stretched.
        - **The reason, the owner's:** below R = 1.334 the pass edge √R/1.155 falls below 1.0, so the
          pass band is EMPTY, and a criterion that cannot return "pass" is not a criterion.
        - **The old value, kept:** 1.3, declared by the owner before step 1, on the ground that below
          it the bands do not separate the two predictions.
        - **The agent's arithmetic, beside it:** 1.334 is 1.155² = 1.334025 at three decimals. At that
          R both predictions sit on their band's edge, 1.0 on the pass edge and R on the fail edge;
          below it, neither reaches its own band. No whole row count at C = 67, k = 1.00 and
          N_start = 400 gives an R between 1.334 and 1.334025, so the floor (below) is the same under
          either.
        - **The owner's reading of it, 2026-11-09:** that note confirms 1.334 as the boundary. It is
          the degenerate point, the infimum of a usable R: above it both predictions sit strictly
          inside their bands. Exactly, the infimum is 1.155² = 1.334025, whose three-decimal value is
          1.334; between the two neither prediction is strictly inside, and at N_start = 400 no whole
          row count falls there.
      - **Until steps 1 and 2 pass,** the view criterion is DROPPED: an abstention, since an
        unqualified instrument cannot be said to separate the predictions. First written "goes
        unjudged"; the section's own word replaces it on 2026-11-09.
      - **C and k come from the CONTROL build, never from the session.** Binding, by the owner's ruling
        of 2026-11-09. The no-recycling prediction is a property of the build that does not recycle.
        Taken on 5d from the session's own dumps, C and k would place the bands from the very quantity
        they judge, and the criterion would be circular. Step 3 took them on `dea38cc`, correctly. Do
        not "improve" them with the session's dumps.
      - **C's configuration, read from the records on 2026-11-09, and settled the same day: C = 101
        is primary, and C = 67 is reported beside it** (the pre-registration at the end of this
        block).
        - **Step 3's dumps were taken with one chat page:** the accounts page on screen, A's page
          attached beside it, and no second chat page. *Recorded results*, the instrument's sitting,
          read it from the hierarchy: `LoginFragment` and one `ChatFragment`. The phone's journal
          agrees: on 2026-11-06 only A's page wrote lines, and B's page next wrote on 2026-11-09.
        - **The session's configuration has one chat page more:** A's on screen, with the accounts page
          and B's attached. So C = 67 lacks B's page's views that are not rows.
        - **How many, by the agent's static count of `dea38cc`'s layouts, not measured:** about 33 to
          35. That is its `fragment_chat`, 32 views, its page's container, and at most about two
          internal children.
          - **The same count reproduces step 3's 67:** `fragment_chat` 32, `fragment_login` 10, two
            `row_account` rows of 6, the pager 1, and about 3 for the pager's list and the pages'
            containers make 58. That leaves about 9 for the window's own frame views and the widgets'
            internal children.
        - **What it would move, for two pages from 400 to 1,200 rows each:** with C = 101, R = 2.776
          and the bands are at 1.4425 and 1.924; with C = 67 they are 2.845, 1.460 and 1.948. So 67
          overstates R by 2.5% and each edge by about 0.02. Recycling's 1.0 and no-recycling's 2.78
          fall in their own bands under either.
        - **A correction, 2026-11-09:** the pass edge at C = 101 is 1.4425, and it was first written
          1.443, rounded up. A rounded edge is a threshold nobody derived. So the edges are
          pre-registered as computed from the formula, unrounded (below).
        - **PRE-REGISTERED on 2026-11-09, before any poll, by the owner's ruling: the view criterion
          is judged under C = 101 and under C = 67.**
          - **The primary verdict uses C = 101,** 67 plus the 34 the static count gives B's page. The
            count's range, 100 to 102, moves the edges by under 0.001. **The verdict under C = 67 is
            reported beside it.**
          - **If the two AGREE,** that is the verdict, and there is nothing to discuss. **If they
            DISAGREE,** the reading sits in one of the two slivers between their edges, and it is
            reported INCONCLUSIVE.
          - **So the judgement does not depend on which estimate anyone prefers.**
          - **Why 101 is primary, the owner's reason: the direction, not the 2.5%.** At C = 67 the
            pass edge is 1.460 against 1.442, so a reading of 1.45 passes under 67 and is inconclusive
            under 101. The configuration error FLATTERS the hypothesis: it makes the wanted conclusion
            easier to reach. An error that pulls toward the hypothesis is not kept for convenience.
          - **What makes 101 usable rather than a guess: the method check.** Counting the same
            layouts for step 3's configuration gives 58 against the measured 67. The shortfall of
            about 9 is the window's own frame and the widgets' internal children, so the count's
            offset is consistent, and B's page is counted by the same method.
          - **No re-measurement,** by the owner: half an hour and an install swap, to move a band
            edge by 0.02, is not justified.
          - **The edges are computed from the formula on the actual counts and not rounded,** with k
            = 1.00 and N the two pages' `shownRows` summed.
          - **With B's count under the fallback** (*Setup*), the verdict is taken under each C and each
            bound, four in all. It stands only if all four agree, and is otherwise INCONCLUSIVE. The
            agent's composition of the two rulings, confirmed by the owner on 2026-11-09.
            - **Why it rarely bites, the owner's reading:** the two C values move the edges by 0.02,
              and the two B bounds move them a lot, 1.44 and 1.92 against 1.19 and 1.59. But the
              predictions sit at about 1.0 and 2.8, outside all four intervals, so a reading near 1.0
              passes on all four, and one near 2.8 fails on all four. Only a middling reading splits
              them, and a split is exactly when no verdict should be given.
            - **The agent's precision on the 2.8:** it is the no-recycling ratio with B growing. Had B
              in fact stayed at 400, a build that does not recycle would read about 1.89. That fails
              under the two 400 bounds and falls between the edges under the two N_B = N_A bounds: a
              split, rightly, since B's size is then the unknown. A recycling build reads about 1.0
              either way, so the build this session judges is not affected.
          - **The abandon floor under each C,** at the planned N_start of 800 summed rows: 1,101 rows
            summed at C = 101, and 1,090 at C = 67. **The primary governs,** as the agent proposed and
            the owner confirmed on 2026-11-09: an end between the two is DROPPED under the primary, and
            the judgement under C = 67 is reported beside it. The outcome is DROPPED, not INCONCLUSIVE.
            - **Under the fallback, the agent's proposal, not yet confirmed:** each bound's floor is
              computed at C = 101 from its own N_start, and the criterion is DROPPED if either bound's
              total is below its floor, since B's size is then the unknown.
  - **Two passes, in this order, by the owner's ruling of 2026-11-09:** the automated session first,
    which delivers the numbers, and then a separate hands-on pass, with Federico driving and its own
    checklist: the checks at each reading below, and the hide, show and send at 1,200 rows. They stay
    out of the automated session for two reasons.
    - **An action with effect outside this machine:** the send posts a chat message under Federico's
      account, which the agent does not automate.
    - **A measurement reason:** every touch by hand is an input event that can move the scroll and the
      row count, and so contaminate the `Views:` readings.
  - **At each reading** (the hands-on pass):
    - **Scroll:** fling far up, then back to the bottom.
    - **Follow the bottom:** at the bottom, new messages keep the list there. Scrolled up, the list
      stays put and the jump button counts, and tapping it returns to the bottom.
    - **Emotes:** animated emotes on screen animate. Scrolled away and back, they animate again.
  - **Once, at 1,200 rows or more** (the hands-on pass): hide a user and show them again. Then send a
    message, and watch its echo confirm.
  - **What counts as a failure, written before the run:**
    - a row out of timeline order, duplicated, missing, blank, or showing another row's content;
    - at the bottom, a new message arriving without the list following it;
    - the jump button's count disagreeing with the rows that arrived while scrolled up, or its tap not
      returning to the bottom;
    - an animated emote frozen on screen, or animating in a row that is not its own;
    - hiding or showing a user leaving a row in the wrong place, or the echo not confirming in place;
    - a crash, an ANR (Application Not Responding) dialog, or an `OutOfMemoryError` in Logcat;
    - **`Views:` growing with the rows,** judged by bands placed from the app's own C and k (settled on
      2026-11-06). The ratio is the count at the last reading that has a dump over the count at the
      first: as planned, at 1,200 rows over at 400.
      - **The bands,** with R and M from the instrument's steps: the ratio passes at or below M/1.155
        and fails at or above M·1.155. Between them it is INCONCLUSIVE, which is reported as such and
        not explained.
      - **Two outcomes that are neither pass nor fail, with two words that are never swapped,** by the
        owner's ruling of 2026-11-09. A reader a month on must not take them for the same outcome.
        - **DROPPED means an ABSTENTION:** no judgement was made, because the instrument does not
          separate the predictions at this size (below the abandon floor), or is not qualified to.
        - **INCONCLUSIVE means a judgement was made and does not decide:** a reading between its pass
          and fail edges, or judgements that disagree (C = 101 against C = 67, or the four under the
          fallback). It is reported with which of the two it was.
        - **VOID is not an outcome of a criterion:** a reading that did not happen as specified, such
          as a refresh bracket whose result falls outside it. A void reading is neither judged nor
          dropped; it is absent.
      - **Until R is measured, the bands are provisional:** 1.5x and 2.0x, as for R = 3.
      - **Measured on 2026-11-06, for one growing timeline:** R = 2.71, so pass at or below 1.43 and
        fail at or above 1.90 (*Recorded results*, the instrument's sitting). Those are the bands for
        the planned counts, 400 and 1,200. Another page setup gives other bands, recorded there too.
      - **The bands are computed from the two actual row counts, whatever they are:** above 1,200 rows
        or below, and with a first reading at 400 or away from it. This is the owner's rule, made
        general on 2026-11-09. The rule of 2026-11-06 covered only an end below 1,200 rows; its special
        cases are removed by the owner, whose they were.
        - R = (C + k·N_end) / (C + k·N_start), where N_start and N_end are the row counts at the first
          and at the last reading that has a dump. With k = 1.00 that is (67 + N_end) / (67 + N_start).
        - M = √R; pass at or below M/1.155, fail at or above M·1.155, inconclusive between.
        - **Not washing:** C, k and the two row counts are all fixed independently of the outcome.
          So the end must be too: the session ends at its target or at its cap, never on a `Views:`
          reading. That condition is the agent's addition, and it stands, by the owner's record of
          2026-11-09.
        - **The worked example, checked:** 400 to 900 rows gives R = 967/467 = 2.07, M = 1.44, and
          bands at 1.25 and 1.66. Computed: 2.0707, 1.4390, 1.2459 and 1.6620.
      - **The floor, from the abandon condition:** R ≥ 1.334 needs
        N_end ≥ (1.334 × (C + k·N_start) − C) / k. It moves with N_start, so it is computed at the
        first reading that has a dump. Computed with `py -3` on 2026-11-09.
        - **With C = 67, k = 1.00 and N_start = 400:** 555.98, so **556 rows**, the first whole count,
          where R = 1.33405. At 555, R = 1.33191.
        - **Over the recorded ranges,** C from 66 to 68 and k from 1.000 to 1.003, the floor runs from
          555.6 to 556.3, so 556 or 557 rows.
        - **Away from 400,** with C = 67 and k = 1.00: N_start = 300 gives 423 rows, and 500 gives 690.
        - **So the session has a useful minimum, 556 rows from a first reading at 400, as well as its
          cap.**
        - **If it ends below the floor,** the view criterion is DROPPED, not stretched, and reported as
          dropped. The case's other criteria are judged on what was seen. The session is recorded as
          short of the case's length, and whether to run it again is the owner's.
        - **The floor at the old threshold, kept:** at 1.3 it was 541 rows (540.1 computed; R = 1.302
          at 541 and 1.2998 at 540), and 539.7 to 540.4 over the ranges. From 541 to 555 rows a pass
          could not happen, since R there is below 1.155², and that band is why the threshold was
          raised. At 1.334 it lies below the floor, where the criterion is dropped. From 556 rows on, a
          ratio of 1.00 passes.
      - **The derivation, from the mechanism:**
        - **Recycling** holds views only for the rows laid out, plus a cache and a pool of fixed size.
          Its count stays roughly constant as rows grow: about 1.0x from 400 to 1,200.
        - **No recycling** holds a view for every row. Its count grows with the rows, and at most
          triples from 400 to 1,200. With C and k it is R = (C + 1,200k) / (C + 400k).
        - **The bands' centre is M = √R,** the geometric midpoint of 1.0 and R. Each band edge sits a
          factor of 1.155 from it. For R = 3 that centre is √3, about 1.73, with edges at 1.5x and
          2.0x: √3 / 1.5 and 2.0 / √3 are both 1.155.
      - **A second page:** R as written is for one growing timeline. If the long session holds a
        second page, its rows enter C if they do not grow, or the growth if the page shares the
        channel, and R changes. Which applies is fixed with the session's page setup, before it runs.
        - **Fixed on 2026-11-09: the second page shares the channel** (*Setup*, above). N_start and
          N_end are both pages' `shownRows`, summed, at the first and at the last reading that has a
          dump. For 400 to 1,200 rows each, R = (C + 2,400k) / (C + 800k): 2.776 with C = 101,
          primary, and 2.845 with C = 67, beside it (the instrument, above). B's rows are measured at
          the first and the last reading (*Setup*, above).
  - **Not a failure: memory growing.** The timeline's data grows with every row, because nothing caps
    it. That predates 5d and is not a 5d regression. PSS and the Java heap are recorded as figures,
    not judged.
- **Does the history still drip in? A scope decision, not a check** (added 2026-11-02). During the
  round, with the 5d build on the phone, not after the pull request merges: Federico opens the app
  on a cached channel and says whether the history still appears to drip in rather than arrive.
  Nothing passes or fails here. 5d does not fix "messages enter one at a time": it only shortens the
  window, from about 2.4 s to about 1.0 (the prediction's figures). Whether dripping is still
  visible at about one second is a question for an eye, and the answer decides whether batching,
  step 7, is inside this release's scope at all. If it is not visible, step 7 and its reading debt
  on Chatterino's `addMessagesAtStart` both leave the release's path. Recorded here: his words, the
  build, the channel and the time.

### The hands-on pass, written on 2026-11-09 before it runs

The automated session gave the numbers (*Recorded results*, `L5D2`). This pass is the feature parity
that no unit test reaches by construction, run by Federico on the dev build `ba1f88e3…` of `00c1e78`,
with the timeline well past 400 rows: that is the regime no automated test covers. Nothing is pressed
by adb. #97 is not merged until this pass is done and reported.

**Before:** record `accelerometer_rotation` (0 on 2026-11-09), TalkBack off, `show_touches` (1) and
`stay_on_while_plugged_in` (7), and restore each one changed before the pass ends. **Auto-rotate and
TalkBack are both OFF now** (read on 2026-11-09): auto-rotate is switched on for case 5 and TalkBack
for case 8, and each is turned off again when its case ends. Run Logcat with the
tags `CHAT_LONG_PRESS`, `CHAT_ACTION`, `CHAT_FOCUS`, `CHAT_IME`, `TWITCH_IRC`, `TWITCH_EMOTES` and
`TMC_HISTORY`. On a failure, stop that case, record what was seen and the time, and go on to the next
case only if the app still runs as before it.

**In order of risk,** worst to ship broken first. "Long" means the case needs A's timeline past about
1,000 rows; "short" means it needs a short chat.

1. **A long-press and a swipe-to-reply on a RECYCLED row.** Long. The path no unit test reaches. The
   long-press menu offers Reply, Copy message, Block user in app and Report message
   (`showMessageActions`, `ChatFragment.kt:3352`), so a stale binding would act on another person.
   - **Do:** fling far up so that rows are recycled, then long-press a message row; read the dialog's
     title, then choose Copy message and paste it into the channel field without sending; cancel.
     Swipe a row with an id from its right edge to the left; read the reply bar; cancel the reply.
     Three rows, at three scroll positions, one of them just scrolled into view. Never tap Report
     message or Block user in this case.
   - **Menu items are chosen BY EXACT LABEL, read from the screen as it stands at that moment, never
     by position** (added on 2026-11-09 by the owner; no criterion changed). "Report message" files a
     report at once and "Block user in app" acts at once, neither with a confirmation, and the items'
     positions shift: without a message id, "Reply" is absent and every item moves up one place
     (*Backlog*). The guide repeats to Federico at that moment that "Report message" and "Block user
     in app" act immediately and are never tapped in this case.
   - **Look for:** the title is `@` and that row's user; the pasted text is that row's text; the reply
     bar names that row's user. `CHAT_LONG_PRESS: message actions opened`.
   - **Failure:** a title, a copied text or a reply naming another row's user or text; no menu; a swipe
     that does not arm, or leaves the row displaced.
   - **Result, 2026-11-09 (*Recorded results*, the hands-on pass): PASS, three rows of three,** on A at
     2,393 shown rows (the marker at 16:28:41).
     - **Where the rows were:** row 1 mid-screen after a far fling; row 2 near the top, further up;
       row 3 just scrolled in from the bottom edge.
     - **What each showed:** the title, the pasted text and the reply bar named that row; the swipe
       armed, and the row came back aligned. In Federico's words, row 2: "titolo corrispondeva, reply
       presente, message copied ok, testo corrispondeva, risposta corrispondeva e tornata allineata".
     - **Log:** `CHAT_LONG_PRESS: message actions opened hasMessageId=true` at 16:33:00.410,
       16:42:05.615 and 16:46:45.676. The identities and the alignment rest on what he saw; no log
       line carries them.
     - **Recorded beside it:**
       - row 1's cleanup tap left no `close_composer_keyboard` line, though his re-check at
         16:37:18.735 did;
       - the channel dropdown closed and reopened on the paste tap;
       - row 3's cleanup tap was on another message, because the row lay behind the keyboard.
2. **The stable flavour.** No part of this round has exercised `com.fs.twitchminichat`, and it is what
   every user installs. **NOT skipped: MOVED to the 5.6.0 device round,** by the owner's ruling of
   2026-11-09, where a release-signed build is installed over the published one and its checks run
   anyway. The phone's stable app is the published 5.5.1, signed with the release key, so a
   debug-signed stable build cannot replace it here without an uninstall, which would destroy that
   app's data. **In that round its chat checks are BLOCKING:** the phone's stable app is 5.5.1, before
   5d, so that round is the first and only time the stable flavour executes 5d before users do (*Recorded
   results*, 2026-11-09, the owner's six records, items 2 and 3).
   - **Do, once installed:** launch, sign in, open a page on a busy channel, scroll and follow, watch an
     animated emote.
   - **Failure:** a crash at launch or on the page; a blank list; a missing resource; an emote that
     never shows.
3. **Hide a user, then show them again, while the list is long.** Long.
   - **Do:** pick a user with several rows on screen; long-press one, Block user in app; then show them
     again from the blocked-users screen.
   - **Look for:** hidden, all that user's rows leave the list and every other row keeps its order and
     place; shown again, their rows return to their places in the timeline, together. Expected and
     intended (#71): messages that arrived while they were hidden do not come back; on the neighbour
     page, not resumed since, their rows show together at its `onResume`. `CHAT_ACTION:
     hidden-user rule updated`.
   - **Failure:** a hidden user's row still shown; another user's row gone; rows out of order,
     duplicated or blank; the list jumping away from where it was; a crash.
   - **Result, 2026-11-09: PASS; the neighbour page NOT REACHED.** On A, about 3,200 rows, estimated
     from 2,393 at 16:28:41.
     - **Hidden:** both of that user's on-screen rows left, and the others kept their order and place
       with no jump: "c'erano 2 righe e sono sparite, le altre righe sono rimaste in ordine, la lista
       non è saltata". Log: `CHAT_ACTION: hidden-user rule updated added=true` at 16:52:50.930.
     - **Shown again,** through Safety & Privacy › Blocked Users › Unblock: "le righe sono tornate al
       loro posto insieme, la lista non si è mossa". No log line decides it.
     - **The neighbour page** was on a channel with no rows of that user. Rows off screen were not
       looked at.
     - **The guide's error:** it listed the toast "User blocked in app: @…" among the pass criteria,
       and withdrew it as soon as it noticed. The toast decides nothing here.
4. **Send a message, and watch its echo through to CONFIRMED, including its full bind (U5).** Long.
   **FEDERICO performs the send, not the driver.** The message is public: send it where a test message
   is acceptable.
   - **Do:** send one message with an animated emote from the dev app's emote picker. Watch it until it
     is confirmed. Then fling far up so that the echo's row is recycled, wait 10 s, and come back to the
     bottom.
   - **Look for:** sending, with a status line at reduced opacity; then CONFIRMED in place, with no
     status line at full opacity; after the return, the row rebound in full, once, still CONFIRMED,
     with its emote. `TWITCH_IRC`, and `outgoing message reconciled` if canonical data arrives.
   - **Failure:** stuck sending; back to the sending style after the return; shown twice; missing;
     another row's content in its place; its emote missing.
   - **Result, 2026-11-09: PASS; the opacity while sending UNCLEAR.** Sent by Federico on A at 17:01, on
     a channel where a test message was acceptable.
     - **The echo:** "Sending…" showed for "una frazione di secondo", then the row was confirmed.
     - **After far up, 10 s and back:** one copy, no "Sending…", the emote present and moving.
     - **Log:** `TWITCH_IRC: outgoing message confirmed` at 17:01:10.354; no `reconciled` line.
     - **The opacity:** "sending è durato troppo poco ma sembrava più trasparente". What would settle
       it is a slower confirmation, which cannot be produced on demand.
5. **Rotation, and the keyboard.** Long. Turning auto-rotate on is a phone setting: record it, and set
   it back to 0 after. `MainActivity` declares no `configChanges`, so each rotation recreates it.
   - **Do:** with animated emotes on screen, rotate to landscape and back. Then open the keyboard in
     the message field and rotate again.
   - **Look for:** no crash; the list shows rows, its count and any backfill after the recreation
     recorded as figures; emotes show and play; the keyboard's state after each turn.
   - **Failure:** a crash, `An emote session was released twice` or `A load was started for a released
     emote session` in the log; a blank list; rows out of order; emotes missing.
   - **Result, 2026-11-09: PASS.** Six rotations, 17:49:58 to 17:59:33. Auto-rotate was on from 17:50 and
     back to 0 by 18:00.
     - **The criteria:** no crash (the crash buffer stayed empty, and the process was 25051
       throughout); rows shown and in order; emotes visible and moving.
     - **Each rotation rebuilt both pages from `first_connect`:** `session ready generation=1`, and a
       fresh `messageCount=400`. The timeline went from 5,358 shown rows (17:43:34) to 482 (17:58:07),
       as this case anticipated.
     - **Keyboard, recorded only:**
       - in the channel field, opened by mistake in the first run ("ho toccato type channel"), it
         closed with the rotation;
       - in the message field, in the redo (`composer focus guard armed` at 17:59:24.815), it stayed
         open, but the cursor was lost and did not come back in portrait.
     - **Not from #97:** it changes neither the manifest nor `MainActivity`, and `main-v5` declares no
       `configChanges`.
6. **Scrolling, follow-the-bottom and the jump button, and a row taller than the list (U2/U3).** Long.
   - **Do:** at the bottom, watch new messages arrive. Scroll up a little and wait; tap the jump button.
     Fling far up and back. Find a row taller than the list, which is likelier in landscape, and scroll
     to it.
   - **Look for:** at the bottom the list follows; scrolled up it stays put while the jump button counts
     the new rows; the tap returns to the bottom and clears the count; the tall row shows its TOP.
   - **Failure:** a new message at the bottom without the list following; the count disagreeing with
     the rows that arrived; the tap not returning; a tall row showing its middle or bottom with its top
     out of reach; a jump in the scroll. If no tall row appears, that sub-case is recorded NOT REACHED.
   - **Result, 2026-11-09: PASS on four parts; the count UNCLEAR; the tall row NOT REACHED.**
     - **PASS, by observation:** the list follows at the bottom; it stays put while the count grows;
       the tap returns it to the bottom and clears the count; there was no jump far up, to the first
       row, and back. No log line covers scrolling.
     - **The count:** "il numero non lo ricordo, forse 6 … erano troppi da contare". A short retry was
       offered and skipped. What would settle it is the number read just before the tap, with few
       enough arrivals to count.
     - **The tall row:** none in portrait ("non ne ho mai visto uno così lungo"). The keyboard method
       was not tried, and landscape was not looked at.
     - **SHIPPING UNEXAMINED until the 5.6.0 device round, not closed** (the owner's ruling,
       2026-11-09). Nobody has yet seen the behaviour that now shows a tall row's top rather than its
       bottom. There, Federico sends one very long message and looks: about two minutes.
     - **2026-11-10: still SHIPPING UNEXAMINED, with the reason narrowed, and the long message
       withdrawn** (the owner's ruling). What is unknown is the list's real height in the tight
       configurations and the text's line metrics. Nothing is sent to any channel (*Recorded
       results*, the entry of 2026-11-10).
     - **2026-11-10, after the device step: SUBSUMED, and the harness row CANCELLED** (the owner's
       ruling; *Backlog*). The case is subsumed by the root, the chat list's lack of a minimum height;
       it was first recorded under the landscape instances, and corrected the same day. Where the
       list is squeezed, in portrait too, an ordinary two-line message is taller than it. The taste
       question stays open, attached to that defect.
7. **Emote animation at the detach boundary, an off-screen page included.** Long.
   - **Do:** scroll an animated emote's row just out of view and back. Swipe to page B for 10 s and back.
     Home for 10 s and back.
   - **Look for:** each emote plays again at once, in its own row. Whether a page off screen decodes
     frames cannot be seen by eye: that is B1's measurement, not this pass's.
   - **Failure:** an emote frozen on screen; an emote animating in a row that is not its own; a blank
     marker; a crash with the ownership messages above. `TWITCH_EMOTES`.
   - **Result, 2026-11-09: PASS,** by observation: "le emote ripartite senza problemi, nessuna emote
     ferma".
     - **Page B:** about 14 s (B's `resume` at 17:16:12.216, A's at 17:16:26.231).
     - **Home:** about 10 s (17:16:33 to 17:16:43).
     - **No crash,** and neither ownership message.
8. **TalkBack, with `reason=outside_click` as the decider.** Long, a full list. As written above (*The
   same click, through TalkBack*): turning it on is a phone setting and the owner's say-so, and it is
   turned off again before anything else.
   - **Result, 2026-11-09: NOT DONE, blocked by TalkBack's own tutorial.**
     - **The exit was not established at first.** Secure-settings writes were refused
       (`WRITE_SECURE_SETTINGS`), and the volume-key shortcut had no target.
     - **Then proven both ways.** Federico turned on the developer option "Disable permission
       monitoring", off before the pass and off again at 18:11.
       - **On:** `settings put secure enabled_accessibility_services
         com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService`, with
         `accessibility_enabled 1`.
       - **Off:** `settings delete secure enabled_accessibility_services`, with
         `accessibility_enabled 0`.
       - Each was confirmed with `dumpsys accessibility`.
     - **Every activation brought TalkBack's `TrainingActivity` to the front,** 4 of 4: at 17:33:57,
       17:38:43, 17:42:06 and 17:48:24.
       - It came back after Federico closed it with "Finish", and after he believed he had
         completed it.
       - It takes focus from the app, so the setup (the field focused, the keyboard open) was lost
         before any gesture.
       - TalkBack was off again each time within 23 s. No double tap was made.
     - **Read beside it, before acting.** `clearChannelFocusClickListener`, the only writer of
       `reason=outside_click`, is set on four views: the fragment's root, `chatList`, `textStatus` and
       `replyBar` (`ChatFragment.kt:1773-1778`).
       - So the line shows that one of the four was clicked.
       - What ties it to the list is "Chat messages", the list's spoken label, heard just before the
         double tap.
     - **What would settle it** is an owner's decision on reaching the list past the tutorial.
     - **Read after the pass, 2026-11-09: the suppression's second link holds** (*Recorded results*,
       *the one read that decides the merge*).
       - `performClick()` reaches `clearChannelFocusClickListener`, which is set on the list itself
         (`ChatFragment.kt:1775`). An accessibility click reaches `performClick()`.
       - So case 8 was a confirmation, not a gate, and the merge is not blocked by it.
     - **CLOSED BY READING, not forgotten; its 5.6.0 retry is cancelled** (the owner's ruling,
       2026-11-09).
       - **What the read closes:** if the list is activated, the dismissal runs, by the six-step
         path in *Recorded results*: clickable through `setOnClickListener`, `ACTION_CLICK` offered,
         RecyclerView's delegate, `AccessibilityDelegateCompat`, the platform delegate, then `View`'s
         `ACTION_CLICK`, `performClick` and `clearChannelFocusClickListener`.
       - **What it does not close:** whether TalkBack can place its focus on the empty area at all.
       - **Why no question is left for users, as REASONING, not proof:** a screen reader moves
         between elements and empty space is not one, so the gesture is probably not a
         screen-reader user's, who would dismiss with Back.
9. **A tap on empty space on a SHORT chat, with the keyboard open (U1's side effect).** Short: B's page,
   switched to a channel with few rows.
   - **Do:** focus the channel field, so that the keyboard is open; tap between rows, on a system line,
     and below the last row.
   - **Look for:** each tap closes the keyboard and clears the channel field's focus; `CHAT_FOCUS:
     composer focus guard disarmed reason=outside_click`.
   - **Failure:** the keyboard stays; the tap below the last row does nothing (the old container ended
     at its last row, so that one is new); a tap on a row taken for empty space, or the reverse.
   - **Result, 2026-11-09: PASS on the second run; the first run UNCLEAR.**
     - **Run 1,** with B on a channel with 44 history rows:
       - below the last row: NOT REACHED, because the list reached the bottom;
       - on a system line: NOT DONE, because the line was not found and the chat was tapped instead;
       - between rows: UNCLEAR.
       - There were four click events for three reported taps: `outside_click` at 15:42:35.134 and
         15:42:41.952, and message-row clicks (`close_composer_keyboard` alone) at 15:42:08.755 and
         15:43:05.979.
       - **What settles it:** a run on a short chat, one tap per message, with the log read between
         taps. Run 2 was that run.
     - **Run 2,** with B on an uncached channel with no history, one tap per message:
       - **between rows: PASS,** `outside_click` at 15:59:11.183 and 15:59:28.265 (two taps, by
         Federico's recollection);
       - **on the system line: PASS at the fourth attempt,** `outside_click` at 16:21:58.127.
         Attempts 1 and 2 were NOT DONE: the line was covered by the channel dropdown, then had
         scrolled away. Attempt 3 was UNCLEAR, because a stray tap on the status text had already
         closed the keyboard. What settled it was attempt 4: one tap on the line, with the keyboard
         open and nothing else touched.
       - **below the last row: PASS,** `outside_click` at 16:25:04.261. This is the new part.
     - **Found beside it, pre-existing:** while the channel field is focused, the channel dropdown
       covers the top of the chat.
       - A second tap on the field closes the dropdown and reopens it, although the code reads as
         closing it (`onChannelFieldTapped`). Back closes it.
       - #97 changes neither `ChannelAutoCompleteTextView.kt` nor any line naming the dropdown's
         functions.

**Grouped, so that the long timeline is built once:**
- **Before, about 5 minutes:** the records and Logcat above; launch; A's page on a busy channel; B's
  page switched to a quiet channel.
- **The short stretch, about 10 minutes, while A grows:** case 9, on B. At the 2026-11-09 rates of 22 to
  56 rows a minute, A passes 1,000 rows about 15 to 25 minutes after its first connect.
- **The long stretch, in one go, about 50 to 60 minutes:** a long-press on A to record its `shownRows`
  first; then cases 1, 3, 4, 6, 7 and 8; case 5, rotation, last, since a recreation may reload the
  timeline; a long-press on A again to record where it ended.
- **The unstructured look, about 10 minutes, on the long timeline,** immediately before the closing
  step (added on 2026-11-09 by the owner): Federico simply uses the chat and says what seems off. It
  cannot be automated, because it tests what nobody thought to assert. The drip-in look is observed
  during it, as a scope decision for batching and not as a criterion.
- **After, about 5 minutes:** auto-rotate back to 0, TalkBack off, the settings read again, the dev app
  force-stopped.
- **The stable flavour, case 2,** is not in this pass: it runs in the 5.6.0 device round, as a blocking
  check.
- **In all, about 1 hour 30 minutes to 1 hour 50 minutes,** for the dev build, the unstructured look
  included (1 hour 20 minutes to 1 hour 40 minutes before it was added).

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

**Corrected beside it, 2026-11-05, by the owner of the measurement's instruction:** "each toward the
hypothesis, since the control pays more" holds for absolute cost and reverses for the ratio.
- **For absolute cost it holds, in the three places it names.**
  - The control records 804 to 808 view builds a launch, with two hook calls each, against 176 to 216
    in 5d.
  - Its timed hooks cost 2.13 ms inside each span, against 1.74 to 1.78 ms.
  - It does not hold for the probe's total inside the span. The message log, in the paragraph below,
    costs 5d 44.70 to 57.13 ms against the control's 17.89 to 21.49 ms (*The overhead step,
    2026-11-05*, under *Recorded results*, item 4).
- **For the ratio it reverses.**
  - The probe is denser inside 5d's span, in all 25 pairings: 8.37 to 9.69 timed events and 1.227 to
    1.773 messages per ms, against 3.32 to 3.60 and 0.394 to 0.466.
  - Its cost is therefore a larger share of 5d's span. Removing it lowers every span ratio, after
    over before, so the measured ratio is conservative: the bias runs against the hypothesis, not
    toward it.
- **Why the two differ:** a ratio moves with each arm's cost as a share of its own span, not with which
  arm pays more. (s5 − p5) / (sc − pc) is below s5 / sc exactly when p5 / s5 is above pc / sc.
- The sentence above stays as it was written.

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

- **The message actions' labels are hardcoded,** "Reply", "Copy message", "Block user in app",
  "Report message" and the `@` title (`ChatFragment.kt` `showMessageActions`), against AGENTS.md's rule
  that user-visible text lives in `strings.xml`. Found on 2026-11-09 while writing the hands-on pass.
  Present on `main-v5` since `418399c` (2026-04-10), so not a 5d defect, and not in 5.6.0's scope.
- **"Report message" files a report at once, with no confirmation, in a menu whose positions shift.**
  Found on 2026-11-09 by the agent's inventory before any automation of the hands-on pass, and recorded
  by the owner's ruling as a product defect, not a test concern. Read in the code, not run.
  - **What it does:** the item calls `onReportMessageRequested`, which calls
    `FcmRegistrationUploader.reportMessage` at once (`ChatFragment.kt:3378` to `:3428`, at #97's head).
    That uploads the reported user and the message text, with the channel, the message's id and time,
    and the reporter's profile id. Nothing asks for confirmation between the tap and the upload. "Block
    user in app" (`:3373`) also acts at once, through `onHideUserRequested`.
  - **The positions shift:** `showMessageActions` adds "Reply" only for a message with an id. With one,
    the order is Reply, Copy message, Block user in app, Report message; without one, Reply is absent
    and every item moves up one place.
  - **So an irreversible action visible to a third party, a report filed against a real person, has no
    confirmation, in a menu whose positions are not fixed.**
  - **PRE-EXISTING, not a 5d defect.** On `origin/main-v5` the menu and its "Report message" item come
    from `418399c` (2026-04-10) and the upload call from `0f60388` (2026-04-14). `showMessageActions`,
    `onHideUserRequested` and `onReportMessageRequested` are byte-identical there and at #97's head
    `00c1e78`: 100 lines each, compared on 2026-11-09.
  - **Not in 5.6.0's scope** unless the owner decides otherwise.
- **No Kotlin version is declared anywhere, so the compiler is whatever AGP carries.** Recorded on
  2026-11-09, by the owner's ruling. On `DESKTOP-BCT68L9`, AGP 9.2.1 carries 2.2.10.
  - **An AGP bump can change the compiler silently.** That is the same shape as the RecyclerView
    premise that #99 now guards: a version the code relies on, left to resolution.
  - **Not in 5.6.0's scope.**
  - **The project instructions state "Kotlin 2.2.10" as though it were pinned.** They are the
    claude.ai project's instructions, not this repository's, which is why no branch here holds the
    statement. The owner will correct them there to say the version is UNDECLARED and carried by AGP
    9.2.1.
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
- **The chat list has no minimum height, so the app's primary content has no floor: OBSERVED on
  2026-11-10, with its instances enumerated and the list CLOSED the same day.** This is one defect,
  recorded as one entry with its instances, so that a fix for one instance is not mistaken for a fix
  of the others. It was found while reading
  whether U2/U3 can be reached. It was first recorded as a candidate from the layout's arithmetic,
  then measured on the phone with the dev build `00c1e78` (*Recorded results*, the device step of
  2026-11-10). Recorded by the owner's rulings.
  - **The root, by reading.** `fragment_chat.xml` is one vertical `LinearLayout`. The list's frame is
    `0dp` with weight 1 (`:130-133`), so it gets the window's height less every other child, clamped
    at 0: `remainingExcess = heightSize - mTotalLength`, then `Math.max(0, childHeight)`
    (`LinearLayout.java` in the android-36 sources, `:991`, `:1019`, `:1026-1027`). Nothing gives it
    a floor.
    - **What competes with it:** two fixed panels, the stream player at 300 dp (`:127`) and the emote
      picker at 240 dp (`view_twitch_emote_picker.xml:6`); the keyboard, where it resizes the window;
      and the bars, which never yield.
    - **Both panels are fixed in both orientations.** There is one `fragment_chat.xml`, with no
      `layout-land` or other qualified variant. No code sets the player's size or reads the
      orientation: no `onConfigurationChanged`, no orientation check, no change to
      `geckoStreamView`'s layout parameters. Nothing locks the orientation.
  - **Once the leftover runs out, by reading and observed:** the list gets 0 px. Every child without
    a weight keeps its full height and is placed below the one before it, with nothing fitted
    (`usedHeight` is 0 once a weight exists, `:877-878`; `:1729-1731`). So whatever comes after the
    overflow, the composer included, is pushed off the screen.
  - **The instances, from the closed enumeration.** *Recorded results* has every class with its number
    (the configuration space of 2026-11-10). Frames come from `dumpsys activity top`, in screen
    pixels. The instances are grouped by orientation, because the product decision turns on it.
  - **Landscape: 6 of the 8 classes give a 0 px list with the composer off screen,** under every
    keyboard state, since the landscape keyboard floats.
    - **Picker open, with or without the stream or the reply bar: 4 classes.** This is the most easily
      reached group, and it needs no stream player: opening the picker is one tap on the emote
      button.
      - With the picker alone, the list is 0 px, at y=529. The picker runs from y=529 to y=1102,
        past the window's bottom at y=1080, and the composer is at y=1125 to y=1240.
    - **Stream on with the picker closed, with or without the reply bar: 2 classes.**
      - With the stream alone, the list is 0 px, and it is placed below the window, at y=1360. The
        player runs from y=644 to y=1360, so 280 of its 716 px are past the edge. The composer is at
        y=1383 to y=1498.
       - **One screenshot** shows the channel bar, the action buttons, the status line, the mute
         button and the top of the player, cut at the bottom edge. There is no chat and no composer.
         It is kept outside the repository, in `tmc-5d-round/device-step-2026-11-10/` on
         `DESKTOP-BCT68L9`, because it shows the account and the channel.
       - **How it is reached:** the player is off on every new page (`streamEnabled = false`,
         `ChatFragment.kt:617`) and only the stream button turns it on (`:1853-1854`).
         `MainActivity` declares no `configChanges` and nothing saves the player's state, so a
         rotation brings the player back off, as the device step observed. The instance is reached
         by turning the player on while already in landscape.
    - **Not affected:** both panels closed, 389 px; the reply bar alone, 254 px.
  - **Portrait: 3 of the 16 classes give a 0 px list, each with the stream on and the keyboard up.**
    These are of the same severity as the landscape ones.
    - **Stream and picker open, keyboard up:** 0 px, with the composer off screen at y=1956 to y=2071,
      against a page bottom at y=1588. This is the configuration the crash had pre-empted. It is
      reached from the picker's search field, or from the composer by a reply started and cancelled
      while the picker is open.
    - **The same, with the reply bar:** 0 px, with the composer off screen at y=2091 to y=2206. It is
      reached by starting a reply while the stream and the picker are open.
    - **Stream on, picker closed, reply bar, keyboard up:** 0 px, with the composer partly under the
      keyboard: 45 of its 115 px fall below the page, at y=1518 to y=1633. It is reached by replying
      with the stream on, an ordinary flow.
    - **Tight, but not 0 px:**
      - 65 px with the stream, the picker and the reply bar, and no keyboard;
      - 66 px with the stream and the keyboard;
      - 189 px with the picker, the reply bar and the keyboard;
      - 200 px with the stream and the picker;
      - 324 px with the picker and the keyboard.
      - A two-line message, 97 px, does not fit in 65 or 66 px, and a five-line one, 232 px, does not
        fit in 189 or 200 px.
    - **For scale:** 1604 px with both panels closed, and 897 px with the keyboard up.
  - **The arithmetic predicted the stream instance in landscape before it was measured.** At their
    declared minimums, the other children came to 508 dp, 1213 px, at 2.3875 px/dp. A window in
    landscape is at most the display's short side: 1080 px, 452 dp.

    | Child | At least | Where |
    |---|---|---|
    | Channel bar | 64 dp, plus the status bar's inset as a top margin | `:17`, `:28`; `SystemBarsInsetHelper.kt:73-81` |
    | Action buttons | 56 dp | `:78`, `:86` |
    | Status line | 24 dp of padding, plus its text | `:104` |
    | Stream controls | 8 dp, plus the mute button the player shows | `:114`; `ChatFragment.kt:3502` |
    | Stream player | 300 dp, fixed | `:127` |
    | Composer row | 56 dp | `:190` |

  - **Not looked at before 2026-11-10.** Case 5's six rotations do not turn the player on, and its
    record does not say it was on.
  - **PRE-EXISTING, not a 5d defect.** #97's only hunk in `fragment_chat.xml` is inside the frame:
    the `ScrollView` and `chatContainer` replaced by `chatList`. The frame's `0dp` and weight 1 are
    context lines, and every bar and the player's 300 dp lie outside the hunk
    (`git diff 98f235a 5238077`). The 300 dp player and the weighted frame are both in `8b99e08`
    (2026-04-07), the first commit of the tracked V4 sources.
  - **STOPPED on 2026-11-12, by Federico's decision: the larger fix is not pursued.** This
    supersedes the decision of 2026-11-10 below. That decision is kept, as the record of what was
    decided and why. Recorded by the owner's ruling.
    - **His reasons:**
      - nobody has ever reported a problem with the player and the chat together;
      - the keyboard is open for seconds at a time;
      - a player that hides and returns on every message would annoy more than the defect does.
    - **So none of these is built:**
      - a floor for the list;
      - an emote picker that shrinks;
      - a player that hides or is refused;
      - the status moved into the channel bar.
    - **The prototype stays as a record:** `prototype/5d-layout-b` at `059bc7b`, with its tag. It
      is kept and is not developed further.
    - **The instances above stay in this entry, KNOWN AND UNREPORTED:** the six landscape classes,
      the three portrait classes at 0 px, and the tight portrait ones. Nothing closes them.
    - **U2/U3 stays subsumed here.** With no floor, it stays routine wherever the list is squeezed.
    - **What remains is one small change, a PROPOSAL, not written:** the mute control moved into
      the buttons row, and the status line tightened (*Recorded results*, 2026-11-12).
      - It is written only once Federico agrees.
      - It is not part of 5.6.0 unless he says so.
      - It moves the computed thresholds. It closes no landscape class, and two of the three
        portrait ones stay.
  - **DECIDED on 2026-11-10, and SUPERSEDED on 2026-11-12 by the stop above: the fix is a MINIMUM
    HEIGHT on the chat list.** The panels that compete
    for space give way, by collapsing, by scrolling or by not opening, instead of the chat reducing to
    nothing. Not written yet.
    - **Two things are still to be chosen, from numbers and not before them:** the floor's value, and
      how the panels give way. The layout's thresholds are in *Recorded results* (the decision and the
      thresholds, 2026-11-10).
    - **The numbers for both are in *Recorded results* (2026-11-11, the layout fix, a design
      proposal),** with the owner's principles, the variants for the top of the page and the stream
      button's labels. Nothing there is chosen: the floor, the arrangement and the words are Federico's.
    - **The player's documented minimum, checked on the phone the same day:** today's app already
      runs the embed below Twitch's documented 400 by 300, and it plays. It does so at 360 and 384 dp
      of width, and at 298 CSS px of height on this phone as set. Twitch's documentation states the
      minimum as a requirement, not a recommendation (*Recorded results*, 2026-11-11, the player below
      Twitch's minimum).
    - **The reasoning is Federico's own.** He does not use the stream and the chat together much
      himself, but will not guess what others do, and he observes that the defect may depend on screen
      size. Those two statements decide between the options:
      - locking portrait is correct only if nobody uses landscape, which is unknown;
      - constraining the player is correct only if the player is little used, which is unknown;
      - a minimum height is correct either way, because it depends on neither.
    - **The principle, recorded by the owner's ruling:** when the cheaper option is correct only given
      a fact that cannot be known, and the complete option is correct regardless, the complete one is
      the cheaper of the two. The other's saving is conditional on a bet.
    - **On the merits:** with a floor, the failure becomes "a panel will not open, or shrinks" instead
      of "the chat disappears", and the first is something a user can understand.
  - **The options as weighed before the decision,** kept as the record of what was weighed. The
    decision above supersedes them.
  - **The options, restated by the owner on 2026-11-10, replacing the earlier framing.**
    - **The earlier framing** offered three ways: the player adapting instead of a fixed 300 dp, the
      picker not pushing the composer off screen, and locking portrait. It called the last the
      cheapest, closing both defects. The first two are ways the panels could yield, so they belong
      to the fix family above.
    - **Locking portrait closes the six landscape classes,** all of them a 0 px list with the
      composer off screen.
      - **It leaves three portrait classes of the same severity:** a 0 px list with the stream on and
        the keyboard up. The composer is off screen in two of them and partly under the keyboard in
        the third.
      - **It also leaves the tight portrait classes,** 65 to 324 px.
      - **Corrected on 2026-11-10, once the list was closed:** this bullet first said that locking
        portrait closed the two worst instances, as though those made up the app-unusable class.
        That class has portrait members.
    - **A minimum height on the list closes the FAMILY,** landscape included.
    - **So it is not cheap against complete: one closes the landscape instances, the other closes the
      problem.**
    - **Locking portrait remains sensible only if nobody uses TMC in landscape AND the portrait
      instances do not bother Federico in use,** the three at 0 px among them. Only Federico knows
      the first, and the second is his observation to make.
    - **Not put to Federico again until the list was closed** (the owner's ruling). The owner has told
      him not to answer yet. The list is now closed in *Recorded results*. Putting the decision to
      him again is the owner's step, from this framing and not from an earlier one.
  - **Whether it belongs in 5.6.0 is not decided.**
  - **U2/U3 is SUBSUMED by this root, not a separate item** (the owner's ruling, corrected on
    2026-11-10 from the landscape instances to the root).
    - **The case is routine wherever the list is squeezed.** With the stream on and the keyboard up,
      at 66 px, any two-line message is taller than the list. With the stream and the picker open, at
      200 px, any five-line one is. Both configurations are portrait.
    - **How far a fix takes it out of normal use depends on the floor the fix chooses.** A row is
      taller than the list only when it is taller than that floor.
    - **The reasoning first recorded here was the owner's error, and is corrected:** he wrote that
      fixing the landscape defect would remove U2/U3 from normal use. That is false. The 200 px and
      66 px measurements are in portrait, so locking portrait would leave those configurations
      untouched, and U2/U3 routine in them (*Recorded results*, the owner's rulings on the device
      step).
    - **The harness row is CANCELLED.** An ordinary two-line message produces the case, and it can
      be looked at directly.
    - **The taste question stays open, attached to this defect:** whether a tall row should show its
      top rather than its bottom. It is Federico's question, and seeing the row is what lets him
      answer it.
  - **A prototype of variant B, for looking at, on 2026-11-11:** `059bc7b` on
    `prototype/5d-layout-b`, not for merging (*Recorded results*, the prototype of variant B).
    - Federico's choices are built into it.
    - Still open: the floor, the labels, the player's height, and what a refused panel does.
      - **Closed by the stop of 2026-11-12, above:** the floor, and what a refused panel does.
      - **Still open, and not decided by the stop:** the stream button's labels, and the player's
        height.
    - The owner's reading of the player check, his and open, is in the same entry.
- **The dev app crashed when the phone was rotated from landscape back to portrait: OBSERVED once,
  on 2026-11-10 at 15:36:03, phone time.** It was seen on the dev build `00c1e78`, 5.5.1-dev (8),
  sha256 `ba1f88e3…`, the build *Test device* records. Its code under `app/src/main` differs from
  `main-v5` by one comment line. Recorded by the owner's ruling.
  - **The sequence:** in landscape, the stream player was turned on, measured and turned off. Within
    seconds the phone was rotated back to portrait (`cmd window user-rotation lock 0`). The app
    closed, and the launcher came to the front.
  - **The stack, from the phone's crash log:**

    ```
    java.lang.IllegalStateException: Fragment ChatFragment{f505160} (…) not attached to a context.
        at androidx.fragment.app.Fragment.requireContext(Fragment.java:977)
        at androidx.fragment.app.Fragment.getResources(Fragment.java:1041)
        at androidx.fragment.app.Fragment.getString(Fragment.java:1063)
        at com.fs.twitchminichat.ChatFragment.updateJumpToBottomButton(ChatFragment.kt:3069)
        at com.fs.twitchminichat.ChatFragment.scrollToBottom$lambda$112(ChatFragment.kt:3704)
        at android.os.Handler.handleCallback(Handler.java:938)
        at android.os.Looper.loop(Looper.java:344)
        at android.app.ActivityThread.main(ActivityThread.java:8212)
    ```

  - **What it shows:** `scrollToBottom` posts a runnable on `chatList` (`:3697`). The runnable calls
    `updateJumpToBottomButton`, whose `getString` (`:3069`) needs the page attached to its activity.
    The runnable was posted while the list was attached, and it ran after the rotation had detached
    the page.
  - **Provenance: PRE-EXISTING, established from the call sites, not by running the old build.
    Confirmed by the owner on 2026-11-10: not a 5d regression, and #97's merge is unaffected.**
    - **The same five callers on both sides.** `scrollToBottom` is called at `98f235a` and at `main-v5`
      from the same five places, in the same functions and under the same conditions:
      `scheduleComposerLayoutRefresh` (`:931`, was `:914`), the keyboard controller's
      `onKeyboardShown` in `onViewCreated` (`:1620`, was `:1601`), the jump button's click (`:1850`,
      was `:1830`), `sendMessageText` (`:2761`, was `:2752`) and `appendChatView` (`:3271`, was
      `:3250`). None is new, none is gone and none moved to another function; only the line numbers
      shifted. No other file calls it.
    - **One step further back, the same.** `appendChatView`'s three callers, and their callers
      (`joinChannelFromChat`, `manualRefresh`, `sendMessageText`, `loadHistoryFromBot`,
      `openIrcClient`), are the same functions, with the same number of calls.
    - **The crashing code is unchanged.** The runnable did the same before 5d
      (`scrollChat.post { … updateJumpToBottomButton() }`, `98f235a:3668-3673`), and
      `updateJumpToBottomButton` is byte-identical.
    - **What 5d did change, and why it is not a regression:** 5d rewrote `isNearBottom`, and moved the
      writer of `stickToBottom` into a RecyclerView scroll listener.
      - That listener ignores layout reports with no movement (`:208`), as the ScrollView's listener
        saw only offset changes.
      - `appendChatView` reads `isNearBottom` only when `stickToBottom` is false.
      - So 5d's changes can alter how often `appendChatView` scrolls, but not whether the crash path
        exists. No scroll was made during the sequence.
    - **Not established:** which caller posted this time. The stack ends at the runnable. The crashed
      process's log holds no line from the app before the crash, only a window-decoration line at
      15:36:03.040, as the rotation began. Whether `98f235a` crashes in this exact sequence was not
      run.
  - **Not reproduced in one more rotation each way,** on 2026-11-10 between 16:48 and 17:01, during
    the enumeration. The process kept the same pid across both, and no crash entry was written.
    - **The sequence differed:** the stream had been off for about a minute, with other taps in
      between, rather than turned off seconds before the rotation.
    - **It stays a single observation.**
  - **The provenance changes the RECORD, not the decision to fix.** Rotating the phone is something
    people do, and a crash is a crash.
  - **An omission, not a design question.** Two sibling runnables in one file, one guarded and one
    not: the composer refresh's runnable returns when the page is gone (`if (!isAdded) return@post`,
    `:923`). `scrollToBottom`'s, posted the same way on `chatList` (`:3697`), has no such guard.
  - **The fix: that same guard at the head of `scrollToBottom`'s runnable,
    `if (!isAdded) return@post`.** It alone covers this crash, which happens only once the page is
    detached from its activity.
    - **Corrected on 2026-11-10:** this entry first gave it as `if (!isAdded || view == null)`. The
      `view == null` went beyond the sibling's pattern, and this crash does not need it.
    - **Accepted by the owner as corrected.** "Restoration of a pattern" means THAT pattern:
      `if (!isAdded) return@post`, exactly. If the extra term were needed, it would be a different
      claim, with its own burden. Narrowing the guard rather than improving it was the right call.
  - **A DECIDED EXCEPTION, by the owner's ruling of 2026-11-10,** to his rule that a guard no test
    exercises is worse than no guard. The rule is not written in this repository: no file here
    states it, searched on 2026-11-10. The rule's reasoning is that an unexercised guard appears to
    handle a case and would break in silence. The exception rests on two reasons:
    1. **This guard cannot break in silence.** If its condition is wrong, the crash returns, and a
       crash is loud.
    2. **It is not a new guard.** It restores a pattern already in the file to a sibling that lacked
       it.
  - **Not written yet.** It waited on Federico's landscape decision, since both may land together.
    That decision is made: a minimum height on the list (the entry above). The guard is still not
    written, and may land with that fix.
    - **Written on 2026-11-11, on its own, by the owner's ruling:** #103, `7b65732` on
      `fix/scroll-to-bottom-detached-guard`, exactly `if (!isAdded) return@post` at the top of
      `scrollToBottom`'s runnable. Continuous integration passed on that exact head. Not merged.
  - **The test that would exercise it: DEFERRED, WITH ITS COST WRITTEN, not forgotten.** It would be
    an instrumented test in `androidTest`, where Espresso is configured.
    - It brings a chat page up and causes one `scrollToBottom` post through an ordinary path.
    - In the same main-thread message it recreates the activity, then lets the main looper run.
    - Without the guard it fails with this exception.
    - **What the seam would take, so the deferral can be reconsidered rather than inherited:** a test
      account written through `AccountRepository`, which `ChatFragment` reads by its `accountId`
      argument (`:1476`, `:1486`), and a factory for the `TwitchChatClient` the fragment builds
      itself (`:2175`), so the page comes up with no token and no connection to Twitch. The page's
      other network callers have not been inventoried.
- **The phone's probe dumps: their deletion is BLOCKED until two comparisons exist and the owner
  rules.** Recorded on 2026-11-11 by the owner's ruling.
  - **What is known:** 133 of the phone's 211 dumps have a copy on `nbpromec1`, by name and size, and
    78 do not (*Recorded results*, the phone's probe files compared). 34 of the 78 are dated
    2026-11-02, when the sittings ran from the other machine, `DESKTOP-BCT68L9`.
  - **What is missing:** the same comparison, by name and size, against that machine's run folder.
  - **Until both comparisons exist and the owner rules, nothing on the phone is deleted.**
- **`C/run.sh`'s header comment says the script deletes the phone's dumps, and it never does: a
  comment that lies.** Recorded on 2026-11-11 by the owner's ruling.
  - **To be corrected when that file is next touched,** not on its own.
  - **The behaviour is the right one: the port deliberately never deletes.** The comment is wrong,
    not the code.
  - Its only `rm` removes the probe's `flag` and `latest` (*Recorded results*, the phone's probe
    files compared).

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
  - **Added 2026-11-05:** one calibration per build was then taken, and the ten probe-off launches
    were deliberately not run, by the owner's decision on the probe's cost. See *The overhead step,
    2026-11-05*, below.

**The overhead step, 2026-11-05: one calibration per build, a CONTRADICTION and its successor rule, and the
probe-off launches deliberately not run.**

By the instruction of the owner of the measurement. The verdicts of 2026-11-04 stand as recorded.
ρ does not change, and the thresholds stay 0.708 and 0.825.

**1. The pre-registration, before any overhead launch.**
- **The file:** `experiment/startup-5d/overhead-prereg-2026-11-05.md`, sha256 `90c4f18c…`. It was
  committed as `9bff12f` and `5c5c2b0` and read back from the remote at 08:50:46.
- **What it fixes:**
  - The overhead launches cannot change the verdicts. They can void the round, if a launch's display
    reading or battery state is not the section's, and they size the bias reported beside the
    verdicts.
  - The driver, `overhead.sh`, stops before any launch whose readings are not the section's.
    `offcheck.py` applies the void conditions a probe-off launch can be checked for.
    `overhead_report.py` fixes the report.
- **Read from the ten launches' own records before any overhead launch:** the probe is denser inside
  5d's span than inside the control's, in all 25 pairings.
  - Timed probe events per ms of span were 8.37 to 9.69 in 5d, against 3.32 to 3.60.
  - Main-thread messages per ms were 1.227 to 1.773, against 0.394 to 0.466.
  - So on the counted terms, removing the probe can only lower the span ratios, whatever it costs per
    call. This corrects a line of *The probe on two builds, and its own cost* (the note added there).

**2. The first sitting, 08:51 to 08:59: stopped for memory.**
- **`X-O-excluded`** (`dea38cc`, `full`) gave EXCLUDED_OK, with both pages on `c8cb0e91` at 400 rows.
  - Its idle wait saw input in 14 consecutive 20 s windows before a quiet one.
  - The launch itself recorded 242 input events. The phone was in use.
- **`Kc-1`** (`dea38cc`, `full calibrate`) was CLEAN, and its `calibration.json` was copied.
- **`K5-1`** was interrupted.
  - Claude Code stopped the sitting's tracked shell for low memory, and the driver's own processes
    went on. The agent stopped `overhead.sh` at 08:59:14, and `K5-1`'s chain at 08:59:27, inside
    `run.sh`'s install, before any force-stop or start.
  - No launch of `K5-1` took place. `runs/K5-1/` holds only `install.txt`, and it is kept.

**3. The resumption, from 09:30, and the CONTRADICTION.**
- **The addendum:** `experiment/startup-5d/overhead-resume-2026-11-05.md`, sha256 `ad8b3cc3…`. It was
  committed as `bede745` and `0eb91aa` and read back at 09:30:03, before the resumed launches.
  - It fixes the order: `X-O2-excluded`, then `K5-1-r1` in `K5-1`'s place, then the ten probe-off
    launches.
  - It fixes the driver, `overhead-one.sh`: one launch per foreground call, with `overhead.sh`'s
    `launch()` unchanged.
- **`X-O2-excluded`** (`dea38cc`, `full`, 09:30) gave EXCLUDED_OK. There was no input, and the
  launcher was in front.
- **`K5-1-r1`** (`a2f12f1`, `full calibrate`, 09:32) gave **"RESULT CONTRADICTION: a recorded wait on
  an IGNORED_EXPIRED row"**. The sitting stopped, as the rule says. That is recorded here in the
  rule's own words, and it is not reinterpreted in place.
  - **The records,** in seconds since boot on `CLOCK_MONOTONIC`:
    - page 0, row 335, a wait of 1369.5 ms, from 10161.888 to 10163.258, on a `stored` row;
    - page 1, row 103, a wait of 1174.3 ms, from 10162.083 to 10163.257, on an `ignored_expired` row.
  - **The ingestion lines:**
    - page 0's worker logged `ignored_expired` at 10161.480 and `stored` at 10163.255;
    - page 1's worker logged `ignored_expired` at 10163.256 and `unchanged` at 10163.269.

**Beside the CONTRADICTION, added the same morning: why it is not a contradiction of the
attribution, the rule's error, and the successor rule.** The owner gave the reasons; each was
checked against `K5-1-r1`'s records.
- **The timing holds, read:**
  - the two waits end 1 ms apart, at 10163.257 and 10163.258 (0.635 ms, from the probe's
    nanoseconds);
  - page 0's `stored` line falls 3 ms before its wait ends;
  - page 1's `ignored_expired` falls 1 ms after page 0's `stored`.
- **"Page 1's expired row reached @Synchronized ingestPokemon and blocked there": consistent, with the
  blocking inferred.**
  - In `a2f12f1`, `ingestPokemon` is `@Synchronized` (`SmartCatchSpawnCoordinator.kt:229` to `230`).
  - #94's gate returns inside it, at lines 244 to 253, before `resolvePokemon` at line 256.
  - No stack was taken, so the blocking is inferred.
- **"It did not resolve the catalogue. #94's gate worked": not shown by these records.**
  - Once page 0's parse had succeeded, the catalogue was cached for the process. A resolve by page 1
    would then also take about 1 ms, so the timing cannot tell the gate's early return from a cached
    resolve.
  - What the records show is what the attribution needs: page 1's expired row did not cause the wait.
    It waited for page 0's parse of an in-window spawn, which #94 leaves on the launch path by design.
  - That the gate returns before the resolution is shown by the code and by #94's tests, not by this
    launch.
  - **The owner's error, recorded later on 2026-11-05:** the owner's claim conflated what the records
    show (the expired row did not cause the wait) with what they cannot show (that the gate returned
    before resolving). The bullet above already says exactly that, so it is not reworded. See *The
    owner's corrections*, below, item b.
- **The rule was mis-specified, and the error is the owner's.** As the owner wrote it, it says "a wait
  on an expired row" where it meant "an expired row that caused a wait".
  - **The reason:** a wait is measured where it ends, on the row the main thread reaches next. A row
    can therefore carry a wait that another page's worker caused.
- **The successor rule,** pre-registered, hashed and pushed before any further launch:
  `experiment/startup-5d/contradiction-successor-2026-11-05.md`, sha256 `6636d8ac…`, with
  `residual_check.py` (`61b253c7…`). They were committed as `d942bd9` and `10572a5` and read back at
  09:48:35.
  - **In the owner's words:** "a wait on an ignored_expired row is a CONTRADICTION unless, in the same
    interval, the other page's worker stored an in-window spawn AND the two waits co-terminate within
    20 ms; in that case it is recorded as the known live-spawn residual, with the evidence listed."
  - **Mechanically:**
    - the other page's history thread logged `outcome=stored` inside the expired row's wait interval;
    - and the other page has a recorded wait ending within 20 ms of that wait's end.
  - **What it does not change:** a recorded wait still voids its launch as a late delivery.
  - **The 20 ms** is taken from the 2026-11-03 lock pairs that co-terminated within −9.2 to +18.4 ms
    (*The catalogue-parse mechanism, tested against the fifteen records*, clause b). It was not
    chosen on `K5-1-r1`'s numbers.
    - **That clause records 13 pairs, not 10.** In the other 3, the ends were +147.5 to +177.8 ms
      apart, with page 1's row queued behind page 0's on the main thread.
    - So a residual of that shape would be classed CONTRADICTION. The rule errs toward stopping, never
      toward passing. The tolerance is unchanged.
    - **Its known false-positive mode, recorded later on 2026-11-05 by the owner's decision.** The
      tolerance is grounded in 10 of the 13 lock pairs of 2026-11-03. A live-spawn residual whose two
      waits end 147.5 to 177.8 ms apart, as the other 3 did, would be classed CONTRADICTION, although
      it is the live-spawn residual. **Anyone meeting a CONTRADICTION from `residual_check.py` reads
      this note before concluding that #94's gate failed:** check first whether the other page's
      worker logged `stored` inside the expired row's wait, and how far apart the two waits end.
  - **Tested before the push** on records other than `K5-1-r1`:
    - `-13` of 2026-11-03 gave CONTRADICTION;
    - a copy of `-13` with one synthetic `stored` line gave LIVE_SPAWN_RESIDUAL for the row that
      waited;
    - `N5-plain-7` gave no verdict.
- **`K5-1-r1` re-read under the successor rule, after the push: LIVE_SPAWN_RESIDUAL.**
  - **The evidence:** page 0's worker (tid 24426) logged `stored` at 10163.255, inside page 1's wait.
    Page 0's own wait ends 0.635 ms after page 1's.
  - **Every other probe record of the round, 130, was read under it too.**
    - 117 have no expired-row wait: 3 of 2026-11-03, 111 of 2026-11-04 and 3 of 2026-11-05, dated by
      the copy of each record.
    - 13 read CONTRADICTION, all excluded launches of 2026-11-03. Those ran builds without #94's gate,
      where the expired spawn did parse itself.
  - **So the 5d calibration it produced is usable.**
    - `calibration.json` was copied by `run.sh` before the stop.
    - The calibration is measured on the main thread after the window closes and after the dump, so it
      does not depend on the span.
  - **As a launch, `K5-1-r1` stays void as a late delivery.** It has two recorded waits, and the
    successor rule does not change that.
  - **Its slot has used one of its three re-runs:** `K5-1` was interrupted, and `K5-1-r1` ran in its
    place.

**4. The probe's cost on both arms, from the two calibrations and the ten launches' counts.**
- **Computed by the agent with `overhead_report.py`'s definitions.** The owner's arithmetic was
  checked, not copied.
- **The decision's reading was fixed before computing,** in section 6 of the successor rule's file:
  - if every probe-removed ratio is at or below its measured ratio, the step CLOSES;
  - if not, the shift returns to the owner.
- **The calibrations:**
  - control (`Kc-1`): 266.8 ns a hook call and 20.4 µs a dispatched message;
  - 5d (`K5-1-r1`): 266.8 ns and 47.1 µs. These are medians of 5 rounds; the first round of each read
    about 650 ns a hook.

| Inside each span | Control `Mc-1` to `Mc-5` | 5d `M5-1` to `M5-5` |
|---|---|---|
| timed hooks | 2.13 ms in each | 1.74 to 1.78 ms |
| dispatch log | 17.89 to 21.49 ms | 44.70 to 57.13 ms |
| the probe's cost | **20.02 to 23.62 ms** | **46.46 to 58.87 ms** |
| as a share of the span | **0.90 to 1.05 %** | **6.00 to 8.60 %** |
| over the burst window, anchor to burst end | 37.04 to 55.22 ms | 61.39 to 63.18 ms |

| 25 pairings | Highest | Lowest | Below the threshold | Highest's distance below it |
|---|---|---|---|---|
| span, measured | 0.3537 (`M5-2`/`Mc-2`) | 0.2835 | 25 | 0.3546 below 0.7083 |
| **span, probe removed** | **0.3355** (`M5-2`/`Mc-2`) | 0.2619 | 25 | **0.3728** |
| burst end, measured | 0.6288 (`M5-2`/`Mc-5`) | 0.4641 | 25 | 0.1962 below 0.825 |
| burst end, the span's cost removed | 0.6202 (`M5-2`/`Mc-5`) | 0.4530 | 25 | 0.2048 |
| **burst end, the window's cost removed** | **0.6181** (`M5-2`/`Mc-5`) | 0.4558 | 25 | **0.2069** |

- **No probe-removed pairing is above its measured ratio,** in either quantity.
  - The mean span ratio falls from 0.3192 to 0.2992.
  - The largest drop in a span pairing is 0.0239, and in a burst-end pairing 0.0113.
- **The burst end's governing row is the window's.**
  - The premise that "the span sits inside that window" holds for 8 of the 10 launches. In `M5-1` the
    span ends 161.4 ms after the burst end, and in `M5-4` 0.7 ms after.
  - So removing the whole span's cost from those two over-removes. Counting the probe's events and
    messages from the anchor to each burst end does not. Both give the same conclusion.
- **The owner's arithmetic, checked:**
  - It agrees on the hooks (1.7 to 1.9 ms), on 5d's 6 to 9 % and on the control's 20.0 to 23.6 ms,
    near 1 %.
  - 5d's dispatch log is 44.70 to 57.13 ms from each launch's own message count, inside the owner's
    42 to 64 ms. That range came from densities times a 750 ms span.
  - The highest pairing falls to 0.3355, not about 0.332. `M5-2` carries 965 messages, and so 47.20
    ms, below the range's top.
  - The mean measured span ratio is 0.3192, not 0.326, and falls to 0.2992, not 0.306.
  - Where the two differ, these figures govern.

**5. The decision: the overhead step CLOSES, and the ten probe-off launches were NOT RUN.**
- **By the rule fixed before computing:** every probe-removed ratio is at or below its measured ratio.
  - The highest span ratio stays 0.3728 below 0.7083.
  - The highest burst-end ratio stays 0.2069 below 0.825.
  - On the probe's account the measured ratios are conservative.
- **This is a step deliberately not taken, not one quietly dropped.** *Order of the launches*, item 3,
  and *The probe on two builds, and its own cost* specify 5 probe-off launches per build, measured
  with `overhead.py`. None was run, by the owner's decision on this arithmetic.
- **What the probe-off launches would have measured that this does not:**
  - hook calls that record no time;
  - the probe thread's CPU snapshots: 21 to 23 a launch, costing 208.9 to 311.8 ms of that thread's
    CPU across the ten launches, on another thread than the main one;
  - any effect of the probe on the burst end beyond its main-thread time.
  - The bias above is therefore that of the counted terms, and of those only.
- **The open item of the interrupted step is closed** by that decision, not left open.
- **Recorded, not acted on:** `Kc-1`, `K5-1-r1` and the two excluded launches are in the run
  directory on `nbpromec1`. They are not published.

**The owner's corrections, recorded 2026-11-05 after the overhead step.** The owner of the
measurement accepted the corrections the agent reported. Each below is recorded as the owner's
error, with its reason. No figure above changes.
- **a. The probe-removed figures were the owner's approximation.**
  - The owner gave the highest span pairing as falling to about 0.332, and the mean span ratio as
    0.326 falling to 0.306.
  - From each launch's own counts, they are 0.3355, and 0.3192 falling to 0.2992 (item 4).
  - **The reason they differ:** the owner's figures came from densities times a 750 ms span. These
    come from each launch's own events and messages.
- **b. "It did not resolve the catalogue" conflated two things.**
  - **What `K5-1-r1`'s records show:** page 1's expired row did not cause the wait.
  - **What they cannot show:** that the gate returned before resolving. The catalogue was already
    cached for the process, so a resolve would also have cost about 1 ms, and the two cannot be
    told apart.
  - **What does show the gate's early return:** the code, where in `a2f12f1` the gate returns at
    `SmartCatchSpawnCoordinator.kt:244` to `253`, before `resolvePokemon` at `256`; and #94's tests.
  - Item 3 already says this, so its bullet is not reworded. A note beside it points here.
- **c. "The span sits inside the burst window" was false for two launches.**
  - In `M5-1` the span ends 161.4 ms after its burst end, and in `M5-4` 0.7 ms after.
  - That is why the burst end's governing row is the window's (item 4).
- **d. "The ten lock pairs" are 10 of 13, and the 20 ms tolerance stays as it is.**
  - The clause the owner cited records 13 pairs. The tolerance is grounded in 10 of them.
  - Its known false-positive mode is recorded beside it in item 3.
  - **Why it is not changed:** changing it now, after `K5-1-r1` passed at 0.635 ms, would fit it to
    the case it has just decided. And a rule that errs toward stopping errs the right way.

**The device-test tag of `a2f12f1`: made against #92's marker, and removed, 2026-11-05.**
- **Made:** the agent created and pushed `device-test/5.5.1-dev-8-a2f12f1`, a lightweight tag on
  `a2f12f1`.
  - It did so on the owner's instruction of 08:25 +0100, which asked for the phone's build to be
    recorded with a tag under the first form of *Test device*'s rule in `RELEASE-STATE.md`.
- **It was made against #92's marker,** at the top of that section. The marker says the round's builds
  get no device-test tag: "a tag names a tested build, and nothing is tested until the round ends".
  - The agent reported the conflict, and recorded it on `docs/state-test-device-measurement-builds`
    (`2b516f3`) without settling it.
- **The marker wins, by the owner's decision.** An instruction given to an agent is a hypothesis, and
  the marker was the contrary evidence.
  - The marker's reason is sound: a measurement build is not a release candidate.
  - Tagging one pollutes a namespace whose purpose is finding shippable builds.
- **Removed, after checking that the commit survives without the tag.**
  - **First, as a step of its own:** `git ls-remote` read 45 branch heads on the remote, at 10:07:33.
    `git merge-base --is-ancestor` was run against each one. Exactly one reaches `a2f12f1`:
    `experiment/startup-5d-treatment-measurement`, whose tip is `a2f12f1` itself.
  - **Then:** the tag was deleted on the remote, and then in the clone, each in a command of its own.
  - **Confirmed:** at 10:08:06, `git ls-remote --tags origin 'device-test/*'` listed only the tags of
    `c1380af` and `d56812b`.
- **Before the removal, only one file named the tag.** `git grep` was run on the eight branches that
  could hold a mention. The only file was `RELEASE-STATE.md` on
  `docs/state-test-device-measurement-builds`, where the second form is rewritten without it.
- **That experiment branch is now the only ref reaching `a2f12f1`.** It stays as it is.

**Before 5d's pull request, 2026-11-05: two retractions of the owner's, and the merge onto
`baef242`.** The agent reported an inventory of the change from `dea38cc` to `a2f12f1`, with every hunk
classified. It is not in this repository. Two of its items, U1 and U4, had been described by the owner
in words the code contradicts. Each is recorded here as the owner's error, with its reason.
- **U1, `ChatListEmptySpaceTaps`.** The owner called it "a behaviour change nobody asked for". It is
  the opposite: compensation for behaviour lost with the ScrollView.
  - **Why it is needed:** RecyclerView 1.3.1 never clicks on its own. In its source, read from the
    sources jar, `onTouchEvent` is at `RecyclerView.java:3750`, and the file holds no call to
    `performClick` and none to `super.onTouchEvent`.
  - **What it keeps working:** on `main-v5`, a tap on the chat area that no row takes reached
    `chatContainer.setOnClickListener(clearChannelFocusClickListener)` through `LinearLayout`'s
    `View.onTouchEvent`. That listener closes the keyboard and clears the channel field
    (`ChatFragment.kt:1744` to `1772` on this branch). Without the class, the tap would stop doing
    either.
  - **The owner's error:** the sign was inverted.
- **U4, `renderedRows`.** The owner said 5d renames it. It does not: it keeps the name and changes what
  it counts.
  - **The field:** `renderedRows`, in the journal's `user.marker` event.
    - On `baef242` it is `chatContainer.childCount` (`ChatFragment.kt:1399`): one view per timeline
      row, the rows of hidden users included.
    - On this branch it is `chatTimelineController.shownRowCount` (`ChatFragment.kt:1420`): the
      timeline's rows, less those of hidden users.
  - **Who reads it:** nothing but the journal. The search covered 45 remote branches, 3 tags, the
    host's run directory, and GitHub's code search over the owner's 7 repositories. Its only readers
    are people reading an exported journal.
  - **The owner's error:** the hazard was described as though the rename had already handled it.
- **The branch was merged with `main-v5`, not rebased, and that was right.**
  - **The merge:** `6c17c4d` merges `baef242` (#95) into this branch. Against `fc63adb`, it changes
    `app/src/main` and `app/src/test` only by #94's two files.
  - **Why not a rebase:** it would rewrite 40 public commits, and documents cite them: `8f39772` in
    `main-v5`'s `RELEASE-STATE.md`, `fc63adb` as the commit the round measured, and `cdbe6ac` and
    `6c47c7d` in this plan. Those citations would then point at commits no branch reaches.
  - **What it costs:** nothing. The squash flattens the history when the pull request merges.

**Before 5d's pull request, 2026-11-05, later: the owner's four conditions.** All four landed on this
branch before the pull request opened: `7c6f58a`, `a911bb9`, `4eb73f8`, and this record.

- **The owner reverses his own lean on U1.** He leaned toward a `RecyclerView` subclass that
  overrides `performClick`. The lint message, read with the suppression removed, settled it the
  other way.
  - **The message, executed on `d40edc3` with the suppression removed and then restored:**
    `ClickableViewAccessibility`, Error, "Custom view `RecyclerView` has `setOnTouchListener` called
    on it but does not override `performClick`", at the `setOnTouchListener` call in
    `attachEmptySpaceTaps`.
  - **What the check accepts, read from its bytecode:** only a `performClick` declared on the
    receiver's own class. So the subclass would be a new file and a new type, holding only an
    override that calls super.
  - **What the inherited method already does:** it invokes the click listener.
  - **The owner's reason, in his words:** the accessibility concern is already met, and the check
    cannot see it. "That is a false positive, not a cause, and the rule against suppressing
    warnings exists so real causes get fixed, not so false positives get worked around with a
    subclass whose only caller is a linter." The subclass is not built.
- **Condition 1, U1: the suppression kept, justified where it stands, and tested (`7c6f58a`).**
  - **The comment at the suppression** (`ChatFragment.kt:3731` on this commit's head) cites three
    lines, each read for it:
    - **The click listener:** `View.java:8083` in compileSdk 36's sources,
      `li.mOnClickListener.onClick(this);`, inside `View.performClick()`.
    - **The accessibility path:** `ACTION_CLICK` on a clickable view runs `performClickInternal`,
      which calls `performClick` (`View.java:16124` to `16127`).
    - **The delegate:** RecyclerView 1.3.1's accessibility delegate hands every action to View's
      handling first (`RecyclerViewAccessibilityDelegate.java:64`, from the sources jar).
  - **Not read:** the phone runs Android 12, and its `View.java` was not read. The line numbers
    are API 36's.
  - **What would make the suppression wrong,** as the comment states it: anything that stops the
    inherited `performClick` from reaching `clearChannelFocusClickListener`. That is an override
    that does not call super, the listener set somewhere other than the list, the click logic
    moved into the touch listener, or the list made not clickable.
  - **The TalkBack case** is in section M, as its own case. Its decisive observable is the
    `CHAT_FOCUS` line `reason=outside_click`. A message row's click does the same visible things
    and writes no such line.
- **Condition 2, U4: the field renamed (`a911bb9`).**
  - **The rename:** `renderedRows` is now `shownRows` (`ChatFragment.kt:1426`).
  - **The format note:** no separate format note for the journal exists in this repository.
    `HistoryDiagnosticsLog`'s KDoc names no field, and `RELEASE-STATE.md`'s section on the journal
    describes no fields. The field's one description is the KDoc of `markDiagnosticsMoment`, so the
    line went there.
  - **What the note says:**
    - `5.5.1` and every build before 5d write `renderedRows`, `v5.5.1` among them (read there).
    - From 5d on, builds write `shownRows`, and 5.6.0 is the first release to carry it
      (`RELEASE-STATE.md`: 5.6.0 carries the chat work).
    - Development builds on both sides of 5d report `5.5.1 (8)` in the header, so the name tells
      them apart.
- **Condition 3, U10: the oracle moved, and pinned (`4eb73f8`).**
  - **A correction of the agent's earlier reading:** "nothing blocks the move" was read for the
    class alone. `ChatTimelineViewSync.kt` also declared `RebindableTimelineView` and
    `ChatTimelineChange`, which `ChatRowViewBinder`, `PendingEchoView`, `ChatTimelineAdapter`,
    the list sync, `ChatTimelineShownRows` and the dev harness use. Moving the file whole would
    have broken main and dev.
    - Those two stay in main, in `ChatTimelineChange.kt`.
    - `ChatTimelineViews` and the sync moved to `app/src/test`.
  - **`timelineIndexOf`, decided by the rule:** the oracle needs it, so it moved to test with the
    class, into `ChatTimelineShownRowsInverse.kt`. It moved as extensions, so the calls and the
    tests that pin it did not change.
    - **The four-argument overload:** its only caller outside the tests was the sync.
    - **The three-argument overload:** it lost its production callers as well, which the owner's
      question did not name. Its only callers in main were the four-argument overload and the
      sync. It moved by the same rule.
  - **Links in production code:** none to the moved symbols is left.
    - `ChatTimelineListSync.kt:9` to `10` now names the sync in plain text.
    - `ChatTimelineShownRows`' KDoc says the way back is in the tests.
    - `ChatTimelineChange`'s KDoc no longer links `ChatTimelineViews.rebind`.
    - **Left as it is:** the dev harness's `FrozenRowViews4804609.kt:158`. It names
      `ChatTimelineViewSync.applyEchoEvent` in plain text, as the code at `4804609` handled it,
      and that is not a link.
  - **Executed on `4eb73f8`, the head before this record:**
    - **Unit tests:** stable and dev, 93 classes and 632 tests each, 0 failed, 0 skipped.
    - **Lint:** stable and dev, unit-test sources included, "no new issues (and 38 errors filtered
      by baseline)" each.
    - **Assemble:** both debug APKs built.
  - **The two holes pinned.** Two new cases in #76's test check `replaced` and
    `hiddenUsersChanged` against the 4850a83 transcription's layout, with the sync taking no part.
    - **The walk** has the same operations in the same proportions. The rows and shown rows are
      read from the layout, and the hidden users are the ones the walk gave.
    - **Neither case is vacuous.** Both assert that their checks ran away from position 0:
      - 7,589 echo changes were checked, 7,300 of them past the first position;
      - 28,791 changes of hidden users, carrying 150,343 InsertRanges away from position 0 and
        178,791 RemoveRanges.
  - **The mutants re-run against the amended test,** on `4eb73f8`, stable, every unit test. The
    predictions were written before the runs. Each mutant was applied to HEAD's files, and the
    tree was clean before and after.

    | Mutant | #76's walk | #76's new case for it | `ChatTimelineListSyncDifferentialTest` | failing tests of 632 |
    |---|---|---|---|---|
    | M4, `replaced` returns null | passes | **fails**, seed 0 step 82, a Change expected and null returned | fails, its `checkNotNull` | 4 |
    | M5, InsertRange one position early | passes | **fails**, seed 0 step 18, "ranges" | fails, seed 0 step 18, "list against the layout" | 5 |

    - **Each case kills its mutant.** Unmutated, all 632 pass.
    - **Other tests that also kill them:** `ChatTimelineShownRowsPropertyTest` kills both,
      `ChatTimelineShownRowsTest` kills M4 (`aShownRow_isInsertedRemovedAndChanged_atItsPosition`)
      and M5 (`unHidingAUser_insertsItsRows_whereTheyStandInTheTimeline`).
  - **The oracle's independence rests on two mechanisms, not one.**
    - **#76's chain:** the sync is checked against its 4850a83 transcription. It holds only where a
      decider's fault changes the sync's layout, or where one of the two new cases checks the
      decision directly.
    - **5d's own intolerance:** the list sync consumes the deciders' output as it is, with no
      fallback and no search. Its differential test then fails at once on a wrong position, as
      with M5, or a missing change, as with M4's `checkNotNull`.
    - **Before today**, M4 and M5 were caught only by the second mechanism. Anyone reading "pinned
      by #76" would have trusted one mechanism where two were doing the work.
- **Condition 4: the absolute test of the mapping.**
  - **It already exists.** `ChatTimelineShownRowsTest` pins the mapping against literal,
    hand-written values. Its timeline is alice, bob, a system line, alice, an echo, bob and carol,
    and it holds hidden rows, an echo, a system line and ordinary rows.
    - **The forward mapping**, with bob hidden, at line 43: `[0, null, 1, 2, 3, null, 4]`.
    - **The inverse mapping**, with bob and carol hidden, at line 49: `[0, 2, 3, 4]`.
    - **With every user hidden**, lines 55 to 57: the system line and the echo at 0 and 1.
    - **The predicate overloads**, lines 75 to 76.
    - **The deciders' positions**, lines 116 to 121.
    - **The two ends:** the forward cases reach both branches, counting from the start and counting
      from the end, and so do the inverse ones.
  - **No new test was added,** since the condition asked for one only if none existed.
  - **The round-trip-consistent mutant, M8,** with its predictions written first. It was executed
    on `4eb73f8`, stable, every unit test.
    - **The mutant:** the forward mapping in main swaps shown positions 0 and 1 whenever two or
      more rows are shown. The inverse in test undoes the same swap first. So the round trip is
      the identity, both ways, on every input.
    - **#76's walk passes it.**
    - **`ChatTimelineListSyncDifferentialTest` fails it,** at seed 0 step 10, "list against the
      layout".
    - **`ChatTimelineShownRowsTest`:** the literal cases fail (lines 35, 43, 49 and 55), and so
      does the reference case at line 80. `theTwoDirections_areInverse` passes, since it checks
      only the round trip.
    - **#76's new echo case** fails at seed 0 step 244.
    - **In all,** 8 of 632 tests fail.
  - **So no mutant meeting the condition could be constructed, and the reason is structural.**
    - **The two consumers differ.** The list sync consumes the forward mapping alone. The old
      sync consumes only the round trip, apart from its hidden-users path.
    - **So the round trip cannot hide a forward fault from the comparison.** The list sync's
      differential test compares a list built from forward positions with the layout the old
      sync built. A consistent pair leaves that layout right, so it exposes a forward fault the
      moment the walk meets one.
    - **What that means about the risk:** the risk the agent named holds for #76's walk alone,
      which passes M8. It does not hold for the chain. Since the move, the inverse is not
      production code at all.
  - **What remains:** a fault confined to inputs that no generator reaches.
    - **The walks' reach:** the largest timeline the walks reached was 240 rows in #76's walk and
      201 in the list sync's.
    - **The reference case's reach:** sizes of 1 to 59.
    - **The literal cases' reach:** a single timeline of 7 rows.
    - **Not tested:** a fault that appears only past those sizes.

**After #97 opened, 2026-11-05: two errors of the owner's, recorded as his.**
- **He said both differential tests would pass a round-trip-consistent fault. They would not.** M8
  showed it: #76's walk passed it, and `ChatTimelineListSyncDifferentialTest` failed it at seed 0
  step 10.
  - **Why: the two consumers use the mapping differently.** The list sync uses only the forward
    mapping. The old sync uses only the round trip, the inverse after the forward, apart from its
    hidden-users path.
  - **What follows:** a fault matched in both directions leaves the old sync's layout right. The
    list sync's comparison against that layout then exposes the forward fault.
  - **That difference, not an extra test, is what closes the class.** No test was added for it.
    `ChatTimelineShownRowsTest`'s literal cases also fail M8, and they were already there.
- **"Move ChatTimelineViewSync to test" was written having looked at the class, not the file.**
  - **What the file also held:** `ChatTimelineChange` and `RebindableTimelineView`, which main and
    the dev harness use. Moving it whole would have broken both builds.
  - **What was done instead:** the agent split it, keeping those two in main in
    `ChatTimelineChange.kt`.
  - **The agent's earlier reading** made the same mistake, as recorded above.
- **Added in the same step, none of it run:**
  - **The row regime,** at the top of section M. By reading, one test crosses 400 rows,
    `ChatTimelineShownRowsCostTest`. It checks reads and the inserted row's round trip, and no
    position by value.
  - **The TalkBack case's approval,** with `reason=outside_click` as its decider. A message row's
    click also closes the keyboard and empties the channel field, so only that Logcat line
    separates the path under test. Approving the case is not read as the go-ahead to turn TalkBack
    on: that stays the owner's at the time it runs.
  - **The label at the framework citation:** `View.java:8083` is API 36's line number. The
    suppression rests on the call, which on another API level is found by its text.

**2026-11-06: four corrections, three of them the owner's.**
- **RecyclerView 1.3.1 comes from viewpager2 1.1.0, not from material 1.13.0.** The app declares
  viewpager2 directly, and material asks for 1.2.1. The owner said material twice. The agent's body
  for #97 said the same, and was corrected on 2026-11-05.
- **"Declaring pins the premise" was wrong, and it was the owner's whole purpose for #98.** Gradle
  resolves to the highest requested version, so a later viewpager2 or material can move RecyclerView
  with nothing failing. #98 documents the dependency, and does not protect the premise.
  - **What protects it instead:** #99, into `main-v5`, open and not merged. It adds
    `CheckRecyclerViewVersion` to `app/build.gradle.kts`, which fails each variant's build when its
    runtime classpath resolves RecyclerView to anything but 1.3.1.
  - **`strictly()` was not applied.** Run on Gradle 9.4.1, a strict version declared on `:app` below
    a transitive request was selected, and the request downgraded, with the build passing.
- **The 400-row cap is the history source's, not the app's.** The timeline has no cap. Rows leave it
  one echo at a time, or all at once on clear, so live messages take it past 400 and it keeps
  growing. The owner treated the cap as the app's.
- **One test does cross 400 rows:** `ChatTimelineShownRowsCostTest`, at 400, 1,000 and 6,400. It
  checks rows read and the inserted row's round trip, not mapped values, which is why M8 passed it.
  - **This one corrects a line of this plan.** As asked on 2026-11-05, the top of section M said that
    no automated test covered the mapping at or past 400 rows. The agent wrote that line and the
    test's reach into the same block without reconciling them. What no test covered there was the
    mapping by value.
- **Added in the same step:**
  - **`ChatTimelineShownRowsPastTheCapTest`,** tests only, with no production file changed. Its
    results are in section M's row-regime note: M8 fails both its cases, and M9 fails those two and
    no other test.
  - **The long-session case,** in section M, with its failure criteria written before any run.
  - **The row-regime note,** corrected to match.

**2026-11-06, later: #99 merged, and two records.**
- **#99 merged into `main-v5` as `98f235a`.**
  - **The merge:** pinned to #99's head, `0df74ab`, after CI run `42178166044` passed on exactly
    that head. `main-v5`'s tree equals `0df74ab`'s.
  - **Into this branch:** merged as `6c1ebb1`, which brought only `app/build.gradle.kts`.
  - **On the merged tree:** the check passes for all four variants, and its grep finds 24 lines. They
    are the 23 of `3efb40e`, plus one correction line this plan added, "RecyclerView 1.3.1 comes from
    viewpager2". The message gives a grep, not a list.
- **`strictly()` was settled by running it, not by the documentation, which contradicts itself.**
  - **The documentation:** "Declaring rich versions" says a strict version on a declared dependency
    "can downgrade a version". "Graph resolution" says that if one "is declared that is lower than
    the highest, resolution will fail".
  - **The run:** on Gradle 9.4.1, a strict 1.2.1 declared on `:app` against viewpager2's transitive
    1.3.1 resolved to 1.2.1 "by ancestor". There was no warning, and the build succeeded. It failed
    only while #98's own non-strict 1.3.1 was declared too.
  - **What follows:** a strict 1.3.1 would silently downgrade a later bump that asks for more.
  - **Whose was what:** that characterisation was the agent's, not the owner's, before the run and
    after it. The owner's error was "declaring pins the premise".
- **M8 did not justify the past-the-cap test, and M9 did.**
  - **M8's fault is at positions 0 and 1,** which the small tests already catch. That both new cases
    fail it shows only that they check values.
  - **M9 is the same matched swap,** on the last two shown positions and past 400 rows only. It fails
    exactly the two new cases, and passes every other test, `ChatTimelineShownRowsCostTest`
    included.
  - **The agent noticed that M8 was insufficient,** and built the mutant that was needed.
- **The long-session case's criteria changed before any run.** All of it is in section M.
  - The 1,200-row bound was kept, with its cost recorded.
  - The view criterion was replaced by its shape.
  - Its instrument was recorded as not qualified.

**2026-11-06: the owner's three errors in the view criterion, and the instrument's sitting,
pre-registered before its first dump.**
- **The owner's three errors, in the criterion as first set:**
  - **"1.5x, midway in logarithmic terms" was wrong.** The geometric midpoint of 1.0 and 3.0 is √3,
    about 1.73. 1.5x is 37% of the way in those terms: ln 1.5 / ln 3 = 0.369.
  - **The two rules overlapped.** "Fail above 1.5x" and "inconclusive between the predictions" both
    applied at 2.0x.
  - **3.0x is only an upper bound.** The views that are not rows, and a row costing more than one
    view, pull the no-recycling prediction below it.
  - **Taken:** the agent's reconciliation. That is pass at or below 1.5x, fail at or above 2.0x, and
    inconclusive between, because √3 / 1.5 and 2.0 / √3 are both 1.155. Those figures are
    provisional until R is measured.
- **Why placing the bands from C and k is not washing the pre-registration.**
  - **C and k are properties of the app:** how many views it holds besides rows, and how many views a
    row costs on a list that does not recycle.
  - **They are measured on the control build,** which cannot recycle by construction. So they say
    nothing about whether 5d recycles.
  - **5d's ratio is not measured in this sitting.**
- **The abandon condition, declared before step 1:** if R comes out below 1.3, the bands do not
  separate the two predictions, and the view criterion is DROPPED, not stretched.
  - **The agent's note, written with it:** the exact boundary is about 1.33, which is 1.155².
  - **Why:** below it, M/1.155 is under 1.0, so the recycling prediction itself falls inside the
    inconclusive band. An R between 1.3 and 1.33 would keep a criterion that a perfectly recycling
    list cannot pass.
  - **Whose decision:** whether to raise the condition is the owner's. If R lands there, both readings
    are reported.
- **Step 4 cannot be taken as written.** This was read from the control's launches `Mc-1` to `Mc-5`.
  - **A short timeline is too brief to dump:** both page views exist from 1,192 to 1,380 ms after the
    anchor, and page 0's first history row arrives from 1,408 to 1,605 ms. A short timeline lasts
    175 to 333 ms. A dump runs a collection first, and is expected to take longer than that. Its
    duration is timed at every dump of this sitting, and recorded with the result.
  - **After that, both pages hold 400 history rows.** A shorter timeline needs both pages switched
    to a channel with short history, and back again. That is input and a change to the app's setup,
    so it is the owner's call.
  - **So C is the intercept of step 3's two dumps.** Under the line they define it is the same
    quantity, extrapolated rather than read. Step 4 as written stays open for the owner.
- **The probe is a declared confound.** This was read in both measurement builds.
  - **It creates no view.** It wraps the page's inflation and each row's creation, and reads the
    container's children.
  - **With the probe on, it holds every row view it saw built.** It keeps them in two
    `IdentityHashMap<View, Int>` maps, `rowOfView` and `buildOfView`, which nothing clears. On a
    recycling build that keeps alive the views the list lets go, and inflates the count.
  - **With the probe off, it holds nothing:** `buildIn` returns −1 unless the mode is full, and
    `buildOut` then stores nothing. The mode is off when `files/startup-probe/flag` is absent.
  - **So the sitting runs with the probe off,** with the flag's absence read before each launch.
- **The sitting's procedure:**
  - **Read before and after:**
    - USB power, battery saver, the level and the temperature;
    - thermal status and the refresh rate;
    - `stay_on_while_plugged_in`;
    - the installed APK's sha256, read on the phone;
    - the probe directory's listing by name and size, for the cleanup queued after the round.
  - **No input events.** Each launch is `am force-stop` then `am start -W`, as `run.sh` launches.
  - **Steps 1 and 2, on `a2f12f1`,** installed since 2026-11-05 09:32:32, sha256 `ece097ac…`: a
    launch, then 60 s to settle, then one dump, then three more about 20 s apart. The list stays at
    rest at the bottom throughout.
  - **Steps 3 and 4, on `dea38cc`,** installed with `adb install -r` and read back:
    - a launch, then 60 s to settle, then dump A with each page's rows;
    - the rows polled until both pages together have gained 200, then dump B. The wait is at most
      30 minutes. If 30 minutes pass first, dump B is taken anyway, and the rows it spans are
      reported.
  - **Afterwards the phone keeps `dea38cc`,** read back and recorded here. The long session installs
    its own build.
  - **What is saved:**
    - every meminfo dump, in `%USERPROFILE%\tmc-5d-round\V`;
    - from the view hierarchy, only each page's row count. The hierarchy dump itself is not saved,
      since it can carry more than the counts.

**2026-11-06: the instrument's sitting, its results.** It was executed on the phone from 09:20 to
10:38, after `9ca3dfe` had pushed the pre-registration. The dumps, the row counts and the state reads
are in `%USERPROFILE%\tmc-5d-round\V`.
- **The probe flag was present, and was removed.**
  - **What it read:** `full calibrate`, written on 2026-11-05 at 09:32. A dev process started at
    09:04:31 that morning, by no command of the agent's, was running with it.
  - **What was done:** the content was recorded first. Then only the flag was removed, in a command
    of its own. It was absent before both launches.
  - **What was kept:** `latest` and the 211 run files, for the cleanup queued after the round.
- **What the launches opened: the accounts page, page 0.** One chat page was attached off-screen
  beside it, and no second chat page existed.
  - **Read from the view hierarchy:** the pager's two attached pages, and its fragments
    (`LoginFragment` and one `ChatFragment`).
  - **So the sitting measured one timeline,** and R as written applies. The procedure's "both pages"
    did not apply.
- **Step 1, on `a2f12f1` (sha256 `ece097ac…`, read on the phone): passes.** The dump prints
  `Views:`: 97.
- **Step 2: passes.** Three dumps at rest, about 22 s apart, read 97, 97 and 97. So f = 1.000,
  against the rule f < 1.155.
  - **The list:** it laid out 25 rows at step 1's dump and 27 at step 2's, and the count did not
    move.
  - **It was off-screen,** at rest. The launch did not open the chat page, and no input was sent to
    open it.
  - **No input** was recorded in that call: getevent, run for the whole of it, recorded no event.
- **The dumps' duration:** 0.40 to 0.68 s by the host's clock, adb included, over the six dumps that
  found the process. That is longer than the short timeline's 175 to 333 ms, so step 4 as written
  stays out of reach.
- **Step 3, on `dea38cc`:** installed from 09:25:31 to 09:25:40 with `adb install -r`. It was read
  back on the phone as `d005e901…`, last updated 09:25:39.
  - **Dump A,** 60 s after the launch, at 09:27:01: 488 views, with 420 rows read before it and 421
    after.
  - **Dump B,** at 09:35:36: 861 views, with 793 rows before it and after.
  - **k = 373 / 372.5 = 1.00.** It is between 1.000 and 1.003, depending on which of A's two row
    counts is taken.
  - **By reading, 1 is the expected value.** The control builds each message row as one
    `SwipeReplyTextView`, an `AppCompatTextView` with no children (`ChatFragment.kt:702` at
    `dea38cc`).
  - **At both dumps:** `ViewRootImpl` 1, `AppContexts` 7 and `Activities` 1.
- **Step 4, C as the intercept:** C = 488 − 1.00 × 420.5 ≈ 67, the same at B. It is between 66 and
  68 by the same choice of A's count.
  - **Consistent with 5d's count, by reasoning, not a check:** 97 at rest is C, plus the 25 to 27
    rows laid out, plus the list's cache and pool. That leaves 3 to 5 for the cache and pool, if
    5d's C is the control's, which nothing measured shows.
- **R, M and the bands, for one growing timeline:**
  - R = (67 + 1,200) / (67 + 400) = 2.71, from 2.709 to 2.718 over the range of k and C;
  - M = √R = 1.65;
  - **pass at or below 1.43, fail at or above 1.90,** and inconclusive between. These replace the
    provisional 1.5x and 2.0x.
  - **The abandon condition does not apply:** R is above 1.3, and above 1.33. The view criterion
    stands.
  - **The spread's margin, reported and not deciding:** f = 1.000, against M/1.155 = 1.43.
  - **Other page setups, for information:**
    - two pages sharing the channel: (C + 2,400k) / (C + 800k) = 2.85;
    - a second page whose 400 rows stay idle: (C + 1,600k) / (C + 800k) = 1.92. That gives M = 1.39,
      and bands at 1.20 and 1.60.
    - Which applies is fixed with the long session's page setup, before it runs.
- **A confound: the owner's taps, between A and B.** getevent recorded 8 taps on the touchscreen:
  4 from 09:28:49 to 09:29:01, and 4 from 09:31:51 to 09:31:52.
  - **How the times were placed:** a `TMC_ROUND` log mark put getevent's clock against the host's.
    Host time is monotonic time plus 1,793,943,213.93 s.
  - **Whose:** likely the owner's, by the owner's word.
  - **What stayed the same:** the app stayed on its accounts page, and `ViewRootImpl` stayed at 1.
    So no dialog was open at either dump.
  - **What it could have moved, and by how much:** ΔV − ΔN = 373 − 372.5 = 0.5, between 0 and 1 by
    A's two row counts. So, if each row is one view, the views that are not rows changed by at most
    one, net, from A to B. A view added and another freed would cancel, and nothing measured
    excludes that.
  - **Not explained:** the rows rose 169 in one minute, from 560 at 09:33:06 to 729 at 09:34:07, 74 s
    after the last tap. Whether a tap on the accounts page adds rows was not examined.
- **The added check was not measured.**
  - **What it was:** declared before dump B, because of the taps: a third dump, B2, at least 200
    rows after B, with no input between them.
  - **The owner paused the sitting** after the poll at 09:41:41, at 868 rows, 75 after B. The agent
    stopped the poll and its recorder.
  - **The process ended during the pause,** at 09:49:04.841. `dumpsys activity exit-info` records
    reason 10 (USER REQUESTED), "remove task", at importance 400. The timeline went with it.
  - **Its replacement, P1 to P2, measured nothing.** It was declared on resuming, to be taken wholly
    under the recorder. Its first dump, at 10:29:20, found no process. The agent's loop did not check
    for one, and its eight polls found no list. That is the agent's error.
  - **The phone was in use:** the recorder counted 742 touch lines in that call. So step 3 was not
    relaunched.
  - **A clean repeat of step 3** needs a launch, then about 5 to 20 undisturbed minutes at the rates
    seen. Rows came at about 43 a minute from A to B, the jump included, and 12.3 a minute after B:
    75 rows in 6 min 5 s. The 12.5 first written here was 75 over 6 minutes.
    Scheduling it is the owner's.
- **Before and after,** read at 09:20:29 and 10:38:06:
  - battery saver 0 and USB power, at both;
  - the level: 94 and charging, then 100 and full;
  - the temperature: 32.7 °C, then 29.2 °C. The thermal status was 0 at both;
  - `stay_on_while_plugged_in` 7, with `mStayOn=true`, at both;
  - **the refresh rate moved:** 90.0 Hz before, and 60.000004 Hz after. The agent changed no
    setting, and the phone was in the owner's use at the end. Not examined further;
  - **`screen_off_timeout` read 1,800,000, 30 minutes, at both.** The agent had read 15,000 earlier
    the same morning, and did not change it. Section M's screen note is corrected to match.
- **The phone keeps `dea38cc`,** with the dev app not running. Read on the phone at 10:38: sha256
  `d005e901…`, last updated 2026-11-06 09:25:39.

**2026-11-06, after the sitting: step 3 not repeated, and the view criterion qualified.**
- **Step 3 is not repeated, by the owner's decision.** The reason: a 5-to-20-minute hands-off window
  is worth more than a fourth decimal place. That replaces the "clean repeat of step 3" above, which
  stays as it was written.
- **The bound the decision rests on.** The taps changed the views that are not rows by at most one,
  net (ΔV − ΔN = 0.5).
  - **With one such view added between A and B:** k = 372 / 372.5 = 0.9987 and C = 68.1, so R is
    2.709 instead of 2.713.
  - **The bands:** at the two decimals they are stated in, they stay at 1.43 and 1.90. At three, they
    move by 0.001 to 0.002.
- **The criterion is qualified.** Steps 1 to 3 passed, and step 4 was replaced by the intercept, as
  pre-registered.
- **k = 1 is an independent confirmation.** Two sources that share nothing agree on it:
  - **The code:** each row is one `SwipeReplyTextView`, with no children (`ChatFragment.kt:702` at
    `dea38cc`).
  - **The counts:** 1.00, from two dumps.

**2026-11-06: Chatterino2's scrollback cap, read at `1b2b630f`.** It is cited as evidence that a cap
belongs in a chat client, not as a design to copy.
- **The owner's error:** the owner wrote that these details "stay in the repository plan meanwhile".
  They did not.
  - **Where they were:** only in the agent's draft for `claude/architecture-debt.md`, in a temporary
    folder that gets cleaned.
  - **So they are recorded here,** to survive until the owner moves them into
    `claude/chat-rendering-plan.md`.
- **The queue and its limit:**
  - **`LimitedQueue`** is a `boost::circular_buffer` with a default limit of 1000:
    `LimitedQueue(size_t limit = 1000)`, in `src/messages/LimitedQueue.hpp`.
  - **A channel's messages** are a `LimitedQueue` sized by a setting, `scrollbackSplitLimit`, whose
    default is 1000 (`/misc/scrollback/splitLimit`). The sizing is
    `messages_(getSettings()->scrollbackSplitLimit)` in `src/common/Channel.cpp`, and the setting is
    in `src/singletons/Settings.hpp`.
- **It removes at the cap.**
  - **A live message** pushed onto a full queue drops the oldest. `pushBack` returns that one as
    `deleted`, and `Channel::addMessage` passes it to `messageRemovedFromStart`.
  - **History at the front** never evicts anything. `pushFront` inserts only what fits.
- **What marks `Disabled` instead of removing is moderation and disconnects, not the cap:**
  - a message deleted by id, through `disableMessage`;
  - every message except system lines, timeouts and whispers, through `disableAllMessages`.
- **The commit read:**
  - **It is not the tag.** `1b2b630f` is "chore: bump versions to v2.5.6 (#7348)", on Chatterino2's
    master, 2026-10-04. The tag `v2.5.6` points at another commit, `5921016f`.
  - **The two have diverged:** the tag is 4 commits ahead and 279 behind.
  - **The cited lines hold at both.** `LimitedQueue.hpp` is byte-identical at the two commits.
    `Channel.cpp` and `Settings.hpp` differ, but not in the lines cited above.

**2026-11-06: the 169 rows in one minute.** This was read from the records, and nothing was run on the
phone.
- **What was recorded.** Only the row counts and their times, in `rows.txt`, read about 60 s apart.
  - **Before the jump,** from dump A at 09:27:01: +6, +12, +23, +31, +33 and +34 a minute.
  - **The jump:** 560 rows at 09:33:06, then 729 at 09:34:07, 74 s after the last tap.
  - **After it:** +64 in the 89 s to dump B, then +22, +25, +22, 0, +1 and +5 a minute.
- **The records on the host cannot tell history from live rows.**
  - **What they hold:** a row count with its time, the `Views:` count, and the touch events.
  - **Why none of them settles it:** none carries a row's origin. Both kinds take the same path into
    the timeline, `appendChatLine`, and cost one view each.
- **What would decide it: the app's diagnostics journal,** which is on the phone and was not read. It
  is `files/diagnostics/history-diagnostics.log`, with a `-previous` file it rotates into at 512 KiB.
  By reading `dea38cc`:
  - **Every connect or reconnect, resume and manual refresh writes one line,** through
    `applyBackfillDecision` (`ChatFragment.kt:2859-2873`). The line is `backfill.triggered` with its
    `source` (`first_connect`, `reconnect_offline`, `reconnect_no_pause_reference`, `resume`,
    `resume_no_pause` or `manual_refresh`), or `backfill.skipped` with its `reason`.
  - **Every request that returns writes `backfill.result`,** with `messageCount`, `firstTs` and
    `lastTs` (`ChatFragment.kt:2975-2982`).
  - **The journal is always on.** It stamps each line with the phone's wall clock.
  - **Live messages write no journal line.**
  - **The test:**
    - no `backfill.result` between 09:33:06 and 09:34:07 means the 169 rows were live. The window is
      widened by the phone's offset from the host's clock, which was not measured;
    - a `backfill.result` in that window means history arrived. Its `messageCount` bounds what it
      could add, and `firstTs` and `lastTs` say whether its window overlapped rows already shown.
  - **What may leave the phone:** event names, times and those three fields only. The journal also
    records account and channel names.
- **One constraint from the counts, by arithmetic and not a finding.**
  - **A backfill of messages missed while disconnected** adds at most what was sent during the gap.
  - **No gap shows before 09:33:06:** the minute before the jump gained 34 rows, the most of any
    minute before it.
  - **So missed messages could reach 169 only at a live rate of about 169 a minute.** A history
    explanation would need a response adding rows that were not missed: older ones, or duplicates.
  - **Why duplicates are excluded by reading, on one condition:** live and history rows carry the
    same `id:` key, and at 560 rows the deduplicator was below its limit of 800, so it still held every
    key shown. The condition is that
    the backend relays Twitch's message ids, which cannot be checked here, because the backend is not
    in this repository.
- **What it means for the long session.**
  - **The case cannot rely on such a rate.** The same page gained 0 to 43 rows a minute in every
    other minute of this sitting, and the 169 is unexplained. A projection from two readings
    across such a minute would be off by a factor of five or more.
  - **It may wait for a burst, and must assume none.** The case is leave-it-running, and its readings
    are set by the row count, not the clock. Its duration is planned at the low rates recorded: 6 to
    24 a minute on 2026-11-03, and 12.3 a minute after dump B here. At 12.3, 800 rows take 65
    minutes.
  - **Rows from history are not "live rows past the history",** which is how the case defines its
    1,200. The session already reads the journal for its row counts (`user.marker`). So it reads
    `backfill.*` from the same file, and rows from history are counted apart.
  - **Past 800 rows, the deduplicator forgets, by reading.** It keeps the 800 most recent `id:` keys,
    on `main-v5` and on this branch alike (`ChatMessageDeduplicator.kt:80`).
    - **Why this case:** it is the first case that passes 800 rows.
    - **The open question:** whether a backfill there can return rows older than the 800 kept keys.
      That depends on the backend's response, which is not read.
    - **A duplicate row** is already one of its failure criteria.

**2026-11-06: the three 60 Hz readings, compared.** Read from the records: the plan, the runs' files,
and `-2`'s logcat. Nothing was run.

| | 2026-11-03, `-2` | 2026-11-04, before the negative control | 2026-11-06, this sitting |
|---|---|---|---|
| When it read 60 | before the launch, 09:47 | 15 of 18 readings, 12:01:40 to 12:03:05 | the after-read, 10:38:06; 90.0 at 09:20:29 |
| Battery | 100, full, 28.6 °C | 100, full, 32.4 °C | 100, full, 29.2 °C; 94 and charging at the 90.0 read |
| Battery saver, power | off, USB | off, USB | off, USB |
| Thermal status | not read | 0 | 0 at both reads |
| What ran before | another app, in front until the launch | another app had been in use; its window was listed | the owner using the phone: 742 touch lines from 10:29 to 10:37 |
| The dev app in front at the 60 read | no: the vendor's log names another app | not recorded | no: no process |
| Which range or vote | the primary range, [0 60] to [0 90] | app requests: none capped the phone's screen at 60, by their order; the votes were mapped to displays only at 14:02 | not read |
| How it ended | 90.0 2.0 s after the dev app came in front | 90.0 from 12:04:32 | not seen: it ended at 60 |

- **Shared:** a full battery at level 100, battery saver off, and USB power. In none of the three was
  the dev app shown in front at the 60 Hz reading. That is certain in the first and the third, and
  not recorded in the second.
- **Not shared:** the time of day, and what ran before, beyond "another app" and "the owner's use".
- **Nothing recorded distinguishes the conditions of the 60 Hz readings from those of the 90.0 ones.**
  - **A full battery:** 90 of the 129 launches that read 90.0 before were at a full battery too.
  - **Another app in front before the launch:** 16 launches read 90.0 with it, and their range did
    not change at the launch.
  - **A video decoder's log line before the launch:** 10 launches at 90.0 had one too. `-2`'s line
    reported `renderFps=0`.
  - **Temperature:** the three, at 28.6 to 32.4 °C, fall inside the 27.4 to 32.7 °C of the 90.0
    readings before a launch.
  - **Thermal status:** 0 wherever it was read, at both rates.
- **What the records do show, without separating the two.**
  - **No reading with the dev app in front and battery saver off has read 60.** The after-reads,
    taken with the app in front at the end of each launch, read 90.0 in all 130 such launches. The one
    after-read at 60 had battery saver on. At `-2`, two minutes of readings with the app in front also
    read 90.0 throughout.
  - **In the one instance with a log, `-2`, the cap was the primary range.** That range is the
    display's whole allowed band, not only what apps request. It was [0 60], and `DisplayManagerService`
    replaced it with [0 90] 2.0 s after the vendor's refresh-rate service (`OPlusRefreshRateService`)
    logged the dev app as the front package.
    - **The same delay comes at every launch from the launcher:** in 113 launches the display specs
      were re-compared 1.66 to 2.10 s after the front package changed, with no change in the range.
    - **What set [0 60]** happened before the log began, and is not recorded.
- **What a change of rate mid-session costs the long session.**
  - **As written, nothing: section M holds no refresh rate.** None of the long session's criteria
    times frames. The rule that voids on a change belongs to the startup measurement (*The rule*). So
    the premise that a change voids the long session is not in the plan. Making it a condition is the
    owner's decision.
  - **If it is made one:** the session runs 30 to 130 minutes. A change found only at the after-read
    voids all of it, and cannot say when the change happened.
- **What could be done.**
  - **Read the rate at every poll and every reading,** one `dumpsys display` a minute. A change is
    then caught within a minute and placed between two row counts. Whether the segments before it
    stand is the owner's ruling.
  - **Stream the display's log lines only, to a local file, for the session:**
    `DisplayManagerService` "Comparing display specs", SurfaceFlinger's `setDesiredActiveMode`, and the
    vendor service's front-package lines. In `-2` those lines carried the change and its moment. They
    name other apps, so they stay local and are reported only by category.
  - **Keep the dev app in front for the whole session:** that is the condition under which no 60 Hz
    reading has been recorded.
  - **Pin the rate with the phone's own refresh-rate setting.** That is the owner's to change, and was
    not done.

**2026-11-06: the poll's silence, and its invariant.**
- **The process under measurement ended at 09:49:04.841, during the pause.** It was a user-requested
  "remove task" (exit reason 10, importance 400), and the timeline the added check needed went with
  it.
- **The loop's silence was the defect, not the process's death.** A process ending is an event a
  sitting must expect. The loop polled a dead process for eight minutes and printed "no container
  found" eight times. That is an instrument that did not look, read as nothing to see.
- **The fix, in the round's `sitting.sh`:** it is in `%USERPROFILE%\tmc-5d-round\V`, like `run.sh`
  outside this repository.
  - **`bind`** records the process under measurement: its package, its pid, and its start time from
    `/proc/<pid>/stat`.
  - **Before every dump and every row count,** `pidof` must name that pid, and its start time must
    match.
  - **After the check:**
    - a dump must name that pid in its header;
    - a row count must come from that pid's activity, and find the timeline in its hierarchy.
  - **A violation** prints `INVARIANT VIOLATED [...]`, with what was required and what was found, and
    exits 3.
  - **`poll`** runs the check on every iteration, and stops at the first violation.
- **Shown to fire, offline,** against a stand-in for adb:
  - nothing bound;
  - the process gone;
  - restarted with a new pid;
  - the same pid with another start time;
  - the timeline not in view;
  - a dump naming another pid;
  - a poll whose process goes at its second iteration, which stops there with exit 3.
  - **And not in the healthy case:** a poll that reaches its target exits 0.
- **Shown to fire, on the phone,** pointed at the sitting's pid 25900, with one read-only `pidof`. It
  exited 3, wrote no row line, and printed:

  ```text
  INVARIANT VIOLATED [process]
    required: every reading comes from com.fs.twitchminichat.dev pid 25900, started at unrecorded (clock ticks since boot), bound at 'A, dump A of 2026-11-06 09:27:01'
    found:    pidof com.fs.twitchminichat.dev returns no process
    No reading from here on measures what was bound. Stopping, exit 3.
  ```

  - **Why "unrecorded":** pid 25900 was bound for this demonstration afterwards, and its start time
    was never read.
- **Not yet exercised on the phone:** two reads, so the first `bind` exercises both.
  - **What:** the start-time read from `/proc/<pid>/stat`, and the `pid=` of
    `dumpsys activity top`'s `ACTIVITY` line.
  - **If either reads differently on this phone,** `bind` or the first reading fails loudly, not
    silently.

**2026-11-06: the backend relays Twitch's message ids, by the owner's reading.**
- **Read by the owner in the backend clone,** not here: `history_endpoint_service.py:157` serializes
  `"id": message.message_id`, and `chat_history_store.py:184` selects `msg_id`, mapped at `:209`.
- **So the condition that "Why duplicates are excluded by reading" rested on holds** (the 169 rows,
  above). A history copy of a row already shown carries the same `id:` key, and at 560 rows the
  deduplicator still held every key.
- **The caveat, the owner's:** `msg_id` was added by a migration (`chat_history_store.py:85`). Rows
  inserted before it relay `"id": null`, and the deduplicator has no key for them: the app keys them
  `hist:` (`ChatFragment.kt:2972`), which it does not keep. True of old history, not of current rows.

**2026-11-06: the recurring 60 Hz, explained by the owner, and how far the records carry it.**
- **The owner's explanation:** the cap was the primary range, [0 60], lifted 2.0 s after the dev app
  came to the front. That matches the re-compare delay of 1.66 to 2.10 s across 113 launches, and 0 of
  130 readings were at 60 with the dev app in front and battery saver off. So those three readings
  were taken too early.
- **Set against the three, from the records above:**
  - **`-2`, 2026-11-03: it holds as recorded.** The 60 was read before the launch, with another app in
    front, and [0 60] was replaced 2.0 s after the vendor's service logged the dev app as the front
    package.
  - **This sitting's after-read, 10:38:06: consistent, not shown.** The dev app had no process, so it
    was not in front. That the cap would have lifted once it was is not observed: the reading ended at
    60, and the range was not read.
  - **The negative control's 15 readings, 2026-11-04: the rule holds, the mechanism is not shown.**
    - They came before its first launch, at 12:05:47, and which app was in front was not recorded.
    - The rate went from 60 to 90.0 and back between 12:01:56 and 12:02:27, and to 90.0 for good at
      12:04:32, with no launch of the dev app recorded at either change. So its lifts were not "2.0 s
      after the dev app came to the front".
    - Its primary range was not read.
  - **So the claim narrows.** The three were read before the dev app had been in front for 2.1 s:
    certainly in `-2` and in the sitting, and by the launch record in the control. The lifting at 2.0 s
    is shown in `-2` only.
- **What set [0 60] originally is still unknown, and stays unknown.** It happened before any log
  began, and nothing recorded can recover it.
- **The rule that follows:** read the rate only after the dev app has been in front for more than
  2.1 s.
  - 2.1 s is the largest delay seen, 2.099 s: a sample maximum, not a bound.
  - In the long session the app stays in front for minutes before most polls, far past it.
- **Section M now records the rate at every poll, with its moment, and not as a void condition,** by
  the owner's ruling. That supersedes "As written, nothing: section M holds no refresh rate", above,
  which was true when written.

**2026-11-06: the long session's tooling, and its duplicate prediction pre-registered.** Nothing in
this block ran on the phone.
- **The pre-registration:** `docs/5d-long-session-duplicates-prereg-2026-11-06.md`, sha256
  `bb846ae18860ccd6bbbe2348c57cd7820c2e1ede802eed4fcf51eaa7d3b9bca3`, git blob `93f1ef1`.
  - **Its times, by this machine's clock:** written by 12:14:25 +01:00 (the file's time), hashed at
    12:42:49, committed as `e8a9a6c` at 12:43:02, and pushed at 12:43:18.
  - **Read back from GitHub at 12:43:29:** the branch's head was `e8a9a6c`, with the same blob and
    the same sha256.
  - **No poll of the long session has run.**
  - **What it fixes:**
    - the owner's prediction;
    - the mechanism and the backfill windows, by reading;
    - the narrower prediction the reading gives: none in this session, unless a window is wider than
      about 30 minutes or the rate rises far above the plan's;
    - what is recorded, and what no duplicate would mean;
    - that it is a candidate pre-existing defect, not a 5d regression, and not in 5.6.0's scope.
  - **The agent's additions in it, marked there:**
    - the narrower prediction;
    - the line between this question and section M's "duplicated" criterion, item 7;
    - one deliberate refresh, proposed, and not part of the session unless the owner adds it, item 9.
- **The tooling changed after the pre-registration was hashed, before any poll.**
  - **It names** `sitting.sh` at sha256 `1120fb03…` and `journal_extract.py` at `34313d2f…`. They are
    now `854dca02…` and `f5abb492…`, frozen for the session.
  - **What changed: the session's mark.**
    - It was the phone shell's clock. It is now the instant of the journal's last line, read with the
      app not running.
    - The journal stamps its lines with the app's clock, and the two clocks were never compared.
    - So a difference between them would have dropped the first history from the count, or let an
      earlier session's lines in, both silently.
    - Whether the process was bound after the mark is now judged by the host's clock.
  - **What did not change:** the events and fields extracted, the counts, and every invariant the
    pre-registration relies on. The pre-registration is not edited.
- **The tooling's checks, executed offline** against a stand-in for adb: 15 cases, each ending with the
  exit and the message expected (section M). The 37 files they saved, 150 lines, hold no field outside
  the whitelist.
- **A defect of the tooling, found and fixed offline before those checks.**
  - **What happened:** the first test of the session's poll hung, printing `shownRows=none_yet` 31
    times.
  - **Why:** the extractor's path reached the Windows Python in POSIX form, because adb's path
    conversion is off for the whole script. The extractor never ran, and its empty output read as
    nothing new.
  - **Now:** the path is converted, the pipe's failure is checked, and the extractor exits 5 on an
    empty input. The case "journal unreadable" stops with `INVARIANT VIOLATED [journal]`.
  - **It is the poll's silence above,** in a new instrument.
- **The deduplicator's code paths are the same on `main-v5` and this branch:** executed, by blob
  (pre-registration, item 7).
  - **One comparison first ran on a wrong path.** `git diff --quiet` reports no difference for a path
    that is in neither tree.
  - **So all four were rerun by blob,** each path checked to exist.

**2026-11-06: the duplicate condition, derived from the code, beside the pre-registration.** Read on
this branch at `a5f0b59`, with the arithmetic computed. Nothing was run on the phone. The
pre-registration is not edited; where this narrows it, the last item says so.
- **The owner's narrowing, recorded first:** "past 800 rows duplicates are possible" named the wrong
  trigger. The row count is not it: the comparison is between the backfill window and the time span
  of the kept keys. The risk is therefore highest when the span is short, which is early in the
  session, not late. The owner reached for the row count because it was the number in front of the
  owner.
  - **What holds, by the derivation below:** the window decides, set against how far back the newest
    shown row whose key has left lies.
  - **What falls or narrows:**
    - **The row count is not idle.** No key leaves before 800 have gone in, so it is a necessary
      condition, though not the trigger.
    - **"Early" holds in one sense only.** Before the 800th key the risk is nil: every older message
      was either never shown or still has its key. After it, the newest keyless row lies 800/r
      minutes back while keys go in in message order. That is constant at a steady rate, and shortest
      when the rate is highest. Only if a history response is listed newest first does the distance
      start short: at the time since the page started, about 33 minutes right after the 800th key.
    - **The window is not the whole comparison.** The response must also hold that row, which
      depends on the backend's selection within its cap of 400.
- **The sensitivity, the owner's figure, checked:** 800 keys span less than 60 minutes above 800/60 =
  13.33 rows a minute, 8.4% above the planned 12.3. So the session records the rate as it goes, from
  its long-presses' counts and moments, as finely as they are spaced, and does not assume it. The
  figure bites only for a window that re-covers an hour of shown rows (below).
- **The condition, from the code.**
  - **One guard.** A row enters the timeline only through `appendChatLine`, whose one check is
    `shouldSuppress(key)` (`ChatFragment.kt:3644`). History reaches it in the response's order, with
    no filter (`:2965-2984`).
  - **The keys.**
    - **Live:** `id:` and the IRC message's id, or else `live:` with the time, the user and a hash of
      the text (`:2255-2266`).
    - **History:** `id:` and the backend's id, or else `hist:` with the time, the user and a hash of
      the text (`:2969-2974`).
  - **Which keys are kept:** `id:` keys only, at most 800 (`ChatMessageDeduplicator.kt:80`).
    - A new `id:` key past 800 removes the earliest inserted, and `containsKey` does not reorder
      (`:46`). So a key leaves after exactly 800 newer `id:` keys, however often it suppressed a copy.
    - `live:` and `hist:` keys are suppressed only when they equal the previous key within 1.5 s.
  - **What resets them:** a channel change, together with the timeline (`ChatChannelBinding.kt:103`).
    - The deduplicator lives with the fragment (`ChatFragment.kt:629`), the timeline with its view
      (`:1497`, in `onViewCreated`).
    - A view rebuilt under the same fragment would keep the keys and empty the timeline. That
      suppresses rows; it cannot duplicate them.
  - **So a duplicate needs one of two things:**
    - **eviction:** a response holds a message already shown whose `id:` key went in more than 800
      `id:` keys ago;
    - **mismatch:** a response holds a message already shown under another key, such as a current row
      relayed without an id, keyed `hist:` against its live `id:`. The owner's backend reading says
      current rows carry ids.
- **How far back each backfill re-covers rows already shown,** from `HistoryBackfillPolicy.kt` and
  `HistoryBackfillState.kt`. Every window ends at its request and spans 30 to 3600 s.
  - **Reconnect or resume after a stop:** `onStop` closes the connection (`ChatFragment.kt:2078`), so
    the gap's messages were never shown. The window, the gap plus 10 s (`:571-572`), re-covers 10 s
    of shown rows.
  - **Reconnect without a stop,** a dropped connection: from the newest message shown, kept as a
    maximum (`HistoryBackfillState.kt:185-187`), plus 10 s. Again 10 s of shown rows.
  - **Resume after a pause without a stop,** the connection still open: the time away plus 10 s, all
    of it shown.
  - **Manual refresh:** 120 s, about 25 rows at 12.3 a minute. Every tap sends one, with no
    suppression (`:366-373`).
  - **First connect:** 3600 s, once per page (`:225-240`).
    - **And again after a failed request.** `REQUEST_FAILED` and `SESSION_MISSING` clear
      `historyLoaded` (`:396-418`). The next connect is then a first connect, over a full timeline
      whose keys were not cleared, and it re-covers up to an hour of shown rows.
    - The pre-registration's section 3 did not have this. Apart from a long pause without a stop, it
      is the only window that re-covers more than minutes of shown rows.
  - **A window still owed** widens any of them back to a failed request's window start, or to a
    skipped one's pause or stop moment (`:659-684`, `:718-764`). That re-covers no more shown rows
    than the request it stands for.
- **Which of them can return a row whose key has gone, by eviction.** The window must re-cover more
  than 800 key-inserting messages of shown rows, and the response must hold one of the older ones.
  - **A refresh** would need more than 800 rows in 120 s, 400 a minute. No rate this channel has shown
    comes near.
  - **A reconnect or a resume after a stop** would need 800 in 10 s. So "after a reconnect" falls, for
    eviction: a reconnect re-covers only 10 s of rows already shown.
  - **The first connect again, or a long pause without a stop,** needs the hour before it to hold more
    than 800 key-inserting messages, above 13.3 a minute. The backend must also return that hour's
    older part.
  - **The backend's selection, which was not read, decides that last part.**
    - **If it returns the newest 400 of a window,** no response can hold a key that has left, at any
      rate, window or time: the newest 400 messages are among the 800 most recent keys.
    - **If it returns the oldest 400,** it can.
  - **With the history at its cap and a steady 12.3 a minute, no selection or order lets even a 3600 s
    window reach a key that has left.**
    - **Keys in message order:** the newest keyless row is 800/r = 65 minutes back, beyond any window.
    - **History listed newest first, newest 400 selected:** its newest rows leave first, but no
      response reaches them.
    - **History listed newest first, oldest 400 selected:** the history ends about 27 minutes before
      the page started, and its newest row's key leaves first, 32.5 minutes in. That row is then
      exactly 60 minutes back, at the window's edge, and every keyless row after it lies further
      back. This case does not depend on the rate: the row sits 60 − 400/r minutes before the page
      and leaves 400/r minutes into it.
- **Can it be created deliberately inside one session? Not at the planned rate.**
  - **No tap can.** The one request a tap makes is a refresh, and 120 s re-covers 25 rows.
  - **The one wide window a person can bring about is the first connect again.** A request must fail,
    and the connection must reconnect.
    - **How, by reading and not tried:** a refresh tapped with the network cut, then the network
      restored. The cut is a phone setting, airplane mode or Wi-Fi, so it is the owner's to approve,
      and it drops the connection while it lasts.
    - **At 12.3 a minute with the history at its cap, it would show no duplicate under any selection or
      order** (above). So it is not a test either: it gives the same self-confirming "no duplicates"
      that item 9 would have.
  - **When, at a higher rate: not early.** With keys in message order, a 3600 s window reaches a
    keyless row only once the history's keys and then the oldest live ones have left, after 800/r
    minutes: 40 minutes at 20 a minute. The newest-first case that starts short needs the hour before
    the page to hold fewer than 400 messages, which the case's setup, history at the cap, excludes.
- **What would be needed instead.**
  - **First, the backend's selection within its cap of 400.**
    - **If it is the newest 400,** a duplicate by eviction is impossible in any session, and the
      eviction question closes by reading. The query is the owner's to read, in the backend clone.
    - **The session's own first record could also show it:** the first connect's `firstTs` and
      `lastTs`, against its moment. The newest 400 end at the request; the oldest 400 start an hour
      before it. That needs the app's UTC offset, which the tooling does not record.
  - **If it is the oldest 400:** a different rate, a channel above 13.3 rows a minute for the hour
    before the wide window, and the network cut above, after 800/r minutes. Neither is in the session
    as planned.
  - **Or a change no manual case can make.** A unit test of `ChatMessageDeduplicator` at a small
    capacity shows the eviction itself. A dev-harness case with a stand-in history client shows the
    duplicated row. Both are code changes, outside 5.6.0.
  - **A second session alone changes nothing:** the same rate and windows, and the deduplicator starts
    empty.
- **The one deliberate tap that can show a duplicate in this session is a refresh, by mismatch, not by
  eviction.** It is not added. The owner decides.
  - **Which:** one refresh, between two long-presses, with nothing else touched. It counts only if its
    `backfill.result` falls between them.
  - **When:** any time, since this mechanism does not depend on the row count. After the first
    reading, so that the bracket is clean.
  - **What it would show:** the rows it added, that is, the change in `shownRows` less the live rows
    of the bracket. While the connection holds, its 120 s are all shown rows, so every row it adds is
    a duplicate.
    - **0, under the owner's backend reading,** where every history copy carries its live row's id.
    - **More than a few** means current rows whose history copies key differently: the mismatch the
      caveat names, found.
  - **What it cannot show:** eviction. It tests the key match, and says nothing past 800 rows.
- **Item 9 is not taken,** by the owner's decision: a refresh at 1,000 rows or more could not produce
  a duplicate by eviction.
  - **The owner's reason** set 65 minutes of kept keys against a 60-minute window.
  - **A refresh's window is 120 s,** which settles it further.
- **What this narrows in the pre-registration, which stays as hashed:**
  - **Section 1, "after a reconnect":** by eviction, a reconnect cannot.
  - **Section 3** misses the first connect that a failed request brings back, the one wide window.
  - **Section 4's "wider than about 30 minutes":** with the history at its cap, that route closes too.
    Its prediction of no duplicate in this session stands, more firmly: by eviction, none at 12.3 a
    minute, under any selection, whatever the window.
  - **Section 9:** not taken. Nothing replaces it unless the owner adds the refresh for mismatch.

**2026-11-06: four instruments today that did not look, read as nothing to see.**
- **The fourth:** `git diff --quiet` reports no difference for a path in neither tree, so a comparison
  that succeeds on a wrong path is not a comparison.
  - **What was done instead:** all four comparisons were rerun by blob, with each path checked to
    exist.
  - **Its entry is in AGENTS.md's *Git and GitHub behavior already diagnosed*,** by its own pull
    request into `main-v5`: #100, open.
- **The owner counted it the third today. It is the fourth:**
  1. the poll at 09:49 that read a dead process as "no container found" eight times: the record above
     that named the shape;
  2. the extractor whose empty output read as "nothing new", 31 times;
  3. the mark taken from the wrong clock;
  4. `git diff --quiet` on a wrong path, read as "identical".
- **Three gave a false reading; the mark did not.** It was caught by reading, before any run.
- **The extractor's fault was not new either.** A Git Bash path given to Windows' Python voided
  `Qc-base-1` on 2026-11-02, and the account came back empty then too (*The qualification,
  2026-11-02*).

**2026-11-09: the owner's four records, before the long session.** Written on `DESKTOP-BCT68L9`, on
this branch at `b52ab74`. Nothing was run on the phone for them. The pre-registration
(`docs/5d-long-session-duplicates-prereg-2026-11-06.md`) is NOT edited; where these settle or change
it, this entry says so.
1. **The eviction route to duplicates is CLOSED, by the owner's reading** in the backend clone.
   - **What was read:** `chat_history_store.py:184-199` is `ORDER BY ts DESC, id DESC LIMIT ?`,
     followed by `rows.reverse()`. So the backend returns the newest `limit` messages in the window,
     in chronological order. The backend is not in this repository, and this was not read here.
   - **Why that closes it: at most 799 keys go in after any message a response can return,** short
     of the 800 that would evict it. This is the agent's static reading, not run. By the owner's
     ruling of 2026-11-09 it replaces the owner's looser reasoning, that the newest 400 are by
     construction the last keys into the 800 kept.
     - **Its premise: the backend stores every message the app showed.**
     - A message among a response's newest 400 then has at most 399 newer ones, one key each at most.
     - Each later response's older part is a run of messages just before it, at most 400 long, so
       together they insert at most 400 older keys after it.
     - The margin is one key at its narrowest, so only a store missing messages the app showed could
       close it. Nothing read says the store misses any.
   - **Section 3 of the pre-registration's eviction case is thereby settled by reading.** Its three
     "Not read" items:
     - which 400 a window holding more returns: the newest;
     - in which order: chronological, after the reverse;
     - whether anything outside the requested window comes back: the owner's reading says "in the
       window". It does not bear on eviction, since every window ends at its request.
2. **The mismatch refresh IS added,** by the owner. Section M, *Duplicates*, carries it.
   - **What:** one refresh between two long-presses, any time after the first reading.
   - **Predicted:** 0 rows added. More than a few means history copies are keyed differently from
     their live rows.
   - **It is now the ONLY open route,** so that tap is the whole duplicate test, not an extra.
   - **Section 9 stays not taken.** It was one refresh at 1,000 rows or more, for eviction; this one is
     for mismatch and needs no row count.
   - **Left open, for the owner:** a number for "a few", and the bracket's length. Section M gives the
     sizes they would be set against.
3. **The abandon threshold is 1.334, replacing 1.3.**
   - **The reason, the owner's:** below R = 1.334 the pass edge √R/1.155 falls below 1.0, so the pass
     band is EMPTY, and a criterion that cannot return "pass" is not a criterion.
   - **The old value, 1.3, is kept** beside the new one in section M.
   - **The floor, recomputed at 1.334 with `py -3`:** with C = 67, k = 1.00 and N_start = 400, 555.98,
     so **556 rows**. R = 1.33405 at 556 and 1.33191 at 555. Over C from 66 to 68 and k from 1.000 to
     1.003, 555.6 to 556.3. At 1.3 it was 541.
   - **The agent's check beside it** is in section M: 1.334 is 1.155² at three decimals, and no whole
     row count falls between the two.
4. **The recompute rule is GENERAL.** The bands are computed from the two actual row counts, whatever
   they are: above 1,200, below it, and with a first reading away from 400.
   - **The special cases are removed,** "if the session ends below 1,200 rows" and "not covered by the
     rule as given". They were the owner's.
   - **The agent's added condition stands, as the owner reads it:** the session ends at its target or
     its cap and NEVER on a `Views:` reading, since ending on the judged quantity would break the
     not-washing argument.
   - **Section M's 2.71, 1.43 and 1.90 stay,** as the values for the planned counts, 400 and 1,200.

**2026-11-09, later: the owner's rulings, the phone read, and the refresh's two values.** On
`DESKTOP-BCT68L9`. The phone was read over adb from 08:12, with the dev app not running. Nothing was
installed, uninstalled or launched. The journal was piped from the phone into a local filter that
prints counts, event and field names, and a whitelist of count and source values; no line was
written or shown raw.
- **The rulings.**
  1. 1a is closed as far as this machine can close it: its key is provably the one `nbpromec1`
     matched against the phone (*Every APK of the round comes from one machine*, 2026-11-09).
  2. The clones being behind is a state to update, not a divergence. `322984b` was taken.
  3. Kotlin and the SDK levels are a gap in the record, not a divergence. This machine's values are
     recorded there, and the Kotlin finding is under *Backlog*.
  4. The 2026-10-15 upgrade stays recorded as a past event, applied and reverted. AGP 9.4 and Gradle
     9.6 stay queued for after the release.
  5. The 799-key bound replaces the owner's looser reasoning, with its premise named (the entry
     above, item 1).
  6. 1.334 stands, read as the degenerate point (section M, the abandon condition).
- **The phone, read.**
  - **The dev app's signer** is `9b3a523b…5c51`, equal in all 64 hex characters to this machine's
    `androiddebugkey`.
  - **Its Firebase project** is the dev configuration's: the same project id, project number and dev
    application id.
  - **The build it holds:** sha256 `d005e901…`, `dea38cc`'s, read by `sha256sum` on the device and
    from the copy pulled off it. versionCode 8, `5.5.1-dev`, last updated 2026-11-06 09:25:39, as
    recorded above at 10:38 that day.
  - **The journal kept its earlier lines:** `history-diagnostics.log`, 3,185 lines and 401,770 bytes,
    from 2026-10-05 11:45:05 to 2026-11-08 12:43:54. There is no `history-diagnostics-previous.log`.
    About 120 KB, roughly 970 lines at its mean, remain before it rotates at 512 KiB.
  - **It holds no `user.marker`,** so no long-press has been journaled on this phone, and so no
    `renderedRows` or `shownRows` field either.
  - **A launch after the last record:** on 2026-11-08 at 12:43:53 one page started, received a first
    connect of 400 rows, and stopped 0.7 s later. It was not the agent's; whose it was is not
    recorded.
  - **Two accounts, each with a recent-channels list** in `shared_prefs/channel_history.xml`, counted
    and not read. In the journal, on 2026-11-05, every launch connected both pages, on one channel,
    with 400 rows each. Since 2026-11-06 every launch has started only the first account's page, as
    that day's sitting found: the launch opens the accounts page, and only the adjacent chat page is
    attached.
- **"The 400 cached rows" are not app data.** By reading `dea38cc`, the app stores no chat rows. Its
  local data is the recent-channels list and other preferences, the journal, and the probe's files.
  Each page's 400 rows come from the backend's history at every first connect, as every excluded
  launch of the round found. So no row count exists to be read with the app stopped, and an install
  cannot remove one. The nearest reading is each page's first connect at the next launch.
  - **The prohibition on uninstalling the dev app stands, with its reason corrected** by the owner
    on 2026-11-09. The reason given before, cached rows, was wrong: there are none to lose. What an
    uninstall would destroy is the Keystore-encrypted account store, so two Twitch accounts would
    need authorising again, and the journal's lines back to 2026-10-05.
- **The refresh's two values** are in section M, *Duplicates*. The points left open there were
  settled by the owner the same day, as the agent recommended them.

**2026-11-09, 08:34 to 08:44: the long session's build, its install, and the long-press qualified.**
On `DESKTOP-BCT68L9` and the OnePlus AC2003. By the owner's instruction, the instrument was qualified
on one short launch before the session was committed: no `user.marker` had ever been journaled on
this phone.
- **The build:** `:app:assembleDevDebug` in `TwitchMiniChat-wt-5d` at `00c1e78`, #97's head. Before it,
  and again after it, `git status --porcelain --untracked-files=all` printed nothing. No toolchain file
  differs from `main-v5`, the git-ignored inputs hash as recorded above, `gradlew --version` printed
  the same three lines, and Android Studio's log shows no upgrade prompt. Nothing was cleaned, so no
  resync was needed.
  - 4 min 42 s, 43 of 43 tasks executed, one Kotlin warning (an annotation's target, at
    `PcgMostWantedSelectionReviewDialog.kt:26`).
  - **sha256 `ba1f88e3347382f76e4d5d1c0661602e2a24e187fcbdcfc2440e64ea88f16e4a`,** signed by
    `9b3a523b…5c51` in full, `com.fs.twitchminichat.dev`, versionCode 8, `5.5.1-dev`, arm64-v8a. Its 12
    dex files embed `00c1e78` twice, with no `+`, and none of the round's other commits.
  - Kept, with its log, in `C:\Users\Fede\StudioProjects\tmc-5d-round\apks`. This entry's own commit
    is documentation on top of `00c1e78`.
- **The install:** `adb install -r` of that kept file, from 08:40:21 to 08:40:31, `Success`. No
  uninstall. Read back from the phone: sha256 `ba1f88e3…6e4a`, `firstInstallTime` 2026-10-05 11:42:01
  unchanged, `lastUpdateTime` 2026-11-09 08:40:33.
- **The data survived, by hash.** Every file of the app's data outside `cache/` and `code_cache/` was
  hashed on the phone at 08:34, again at 08:40 just before the install, and after it.
  - The 16 preference files and the journal: 17 unchanged, none changed, removed or added.
  - The 398 other files: unchanged, by one combined digest over their paths and hashes.
  - The journal: 3,185 lines, sha256 `e93b0cf7…484e`, before and after.
  - This replaces the row-count check, which asked for something that does not exist.
- **The long-press, qualified.** One launch, `am start -W`, cold, 1,647 ms, at 08:41:02. It opened the
  accounts page, and one swipe brought A's page on screen. The refresh button was located from the
  screen's hierarchy, of which only its bounds were read, and each press was `input swipe` held
  1,000 ms on it. The lines, verbatim, with the account and channel replaced by labels:

  ```
  2026-11-09 08:42:23.592 user.marker account=<A> channel=<c2> resumed=true shownRows=416
  2026-11-09 08:42:43.975 user.marker account=<A> channel=<c2> resumed=true shownRows=418
  2026-11-09 08:43:39.925 user.marker account=<A> channel=<c2> resumed=true shownRows=427
  2026-11-09 08:43:44.133 user.marker account=<A> channel=<c2> resumed=true shownRows=427
  ```

  - **`shownRows` is present and `renderedRows` absent.** Beside it each line carries the account,
    the channel and `resumed`.
  - **How long until the line appears:** 91, 30, 119 and 19 ms after its own instant, all on the
    phone's clock, polling about every 0.11 s. So within 0.12 s, the polling's resolution. Each instant
    fell 0.45 s after the press began: the input tool's start and the hold to the long-press timeout.
  - **A second press writes a second line,** 20.4 s apart and 4.2 s apart alike.
  - **No press started a refresh:** no `manual_refresh` line.
- **Two things the launch showed.**
  - **B's page attached when A's came on screen,** at 08:42:07.110, 157 ms after the swipe, with a
    first connect of 400 rows on the same channel (section M, *Setup*).
  - **A page coming on screen triggers a backfill:** A's `resume_no_pause` at 08:42:07.523, 1 row.
- **Left as the round leaves it:** Home at 08:43:45, both pages' `lifecycle.stop`, then
  `am force-stop` at 08:43:48. The dev app is not running. The journal has 3,202 lines, and its first
  401,770 bytes still hash to `e93b0cf7…484e`.
- **The phone holds `00c1e78`'s build, `ba1f88e3…`,** since 2026-11-09 08:40:33. `d005e901…`, pulled off
  it, stays in the agent's scratch directory on `DESKTOP-BCT68L9` as the way back.
- **Not on `DESKTOP-BCT68L9`: the session's tooling.** `sitting.sh` and `journal_extract.py`
  (section M) are in `%USERPROFILE%\tmc-5d-round\V` on `nbpromec1`, and neither is anywhere under
  `C:\Users\Fede`.

**2026-11-09, after the qualification: the owner's rulings, and C's configuration read.** Nothing
was done on the phone.
- **The instrument passes** (section M, *Three readings*).
- **Two pages on one channel; the one-page ruling falls** (section M, *Setup*).
- **Expected input is exactly the session's own presses** (section M, after the cap).
- **C and k come from the control build only; step 3's configuration was one chat page, so C may be
  understated by about 34 views** (section M, the view count's instrument). Open, and the owner's.
- **Open: whether the frozen poll can tell two pages apart** (section M, *History counted apart*).
- **The way back is kept beside the round's builds.** `d005e901…`, pulled off the phone, was copied to
  `C:\Users\Fede\StudioProjects\tmc-5d-round\apks\dea38cc-app-dev-arm64-v8a-debug.nbpromec1-build.pulled-from-phone-2026-11-09.apk`
  and hashes to `d005e9014367be922a4230a7a842438e723cf5b65373cab23de8ea0609824402` after the copy.
- **The session's two tools are to be committed,** by the owner's ruling: their being on one disk
  only is work that gets lost. They had not reached `DESKTOP-BCT68L9` when this entry was written.

**2026-11-09, later: three rulings, pre-registered before any poll.** Nothing was done on the phone.
- **C = 101 primary, C = 67 beside it,** INCONCLUSIVE if they disagree (section M, the view count's
  instrument, *C's configuration*).
- **The tools go to both `experiment/startup-5d-control` and `experiment/startup-5d-treatment`,**
  where the five host scripts are identical, so that no divergence is created. **Their identity is
  their FULL sha256 from then on,** since eight hex characters are too few for a frozen tool and full
  ones cost nothing: this project has been bitten by a prefix before.
- **B's count is measured at the first and the last reading,** with the two-bound fallback (section
  M, *Setup*).
- **`sitting.sh` may be changed, and that is not washing,** under three conditions: declared before
  any poll, the old and the new full hashes both recorded, and the change touching only how pages
  are read. No threshold, band or verdict rule moves. The file is read, and what it does with page
  identity reported, before anything is changed.

**2026-11-09, later: the two compositions confirmed, and the outcome words fixed, before any poll.**
Nothing was done on the phone.
- **Four verdicts under the fallback,** standing only if all four agree, otherwise INCONCLUSIVE, with
  the owner's reading of why it rarely bites (section M, *C's configuration*).
- **An end between the two floors:** the primary governs, and the outcome is DROPPED.
- **DROPPED and INCONCLUSIVE are defined and kept apart** (section M, *The bands*): an abstention
  against a judgement that does not decide. "Goes unjudged" became DROPPED.
- **The rounding correction is recorded as a correction:** 1.4425 had been rounded up to 1.443.
- **Open, the agent's proposal:** the floor under the fallback's two bounds.

**2026-11-09: the session's tools committed, on one disk only until then.** On `DESKTOP-BCT68L9`.
Federico copied both files from `nbpromec1`'s `%USERPROFILE%\tmc-5d-round\V` into
`C:\Users\Fede\StudioProjects\tmc-5d-round\V`.
- **Verified against the recorded prefixes: both MATCH.**

  | File | Recorded | Computed, full sha256 | Blob |
  |---|---|---|---|
  | `sitting.sh` | `854dca02…` | `854dca023884bc3dfe6f643d28b019fe0bda4f95f5df1bf4ec17d02a28d93164` | `e952ac23924f5bab51c3511a37a6a3e9eb3fae9b` |
  | `journal_extract.py` | `f5abb492…` | `f5abb4920daaccd9d9a9f1a9448fb705ae47d1ce28ba55dc71f778253c2cc678` | `b39667635d6f94892a241e8acd57001280a37b9c` |

  - **From here on the identity is the full sha256,** by the owner's ruling: eight hex characters are
    too few for a frozen tool.
  - Both files are LF-only ASCII, of 18,921 and 2,829 bytes, last modified on 2026-11-06 at 12:45 and
    12:44, as frozen.
- **Committed unchanged, the same two blobs on both branches,** beside `run.sh`, `analyze.py`,
  `qualify5d.py`, `decide.py` and `launchcheck.py`, which are identical there too:
  - `experiment/startup-5d-control`: `613ff07bdc3953c175cfa65c2fcfd6d606e1d90e`, on `d942bd9`;
  - `experiment/startup-5d-treatment`: `d467f2e174fcc6c72923d5d1bec061590f015040`, on `10572a5`.
  - Staged with `core.autocrlf=false`, so each blob's content hashes to the frozen sha256. A Windows
    checkout with `core.autocrlf=true` writes CRLF, so the identity is checked on the blob, `git show
    <commit>:experiment/startup-5d/sitting.sh`, not on a checked-out copy.
- **Scanned before the push, the repository being public:** all 409 added lines on each branch, for
  credential words and shapes, emails, URLs, addresses, personal names, machines and paths, the
  phone's serial, the account and channel fingerprints, the Firebase identifiers and long hex. One
  hit, a false positive: a drive-path pattern matched `d:\d` in the extractor's line grammar. Both
  files were also read in full.
- **They were on one disk only, `nbpromec1`'s, from 2026-11-06 until these commits on 2026-11-09.**
- **For running them, read and not run:**
  - `sitting.sh` writes its outputs beside itself (`sitting.sh:35`): the dumps, the row counts, the
    binding, the mark, the extracts and the rates. So it runs from
    `C:\Users\Fede\StudioProjects\tmc-5d-round\V`, whose copies are the committed bytes, never from a
    clone.
  - **Its default Python is `py -3.10` (`sitting.sh:37`), which `DESKTOP-BCT68L9` does not have.** Its
    only Python is 3.14.0, and `py -3.10` answers "No suitable Python runtime found". So the default
    would stop at the first journal read with `INVARIANT VIOLATED [journal]`. Running here needs `PY`
    set, or Python 3.10 installed: the owner's to decide, since the Python version is part of the
    instrument.
  - **`adb` from `PATH`** is `%LOCALAPPDATA%\Android\Sdk\platform-tools`, the same 37.0.1 binary as
    `D:\Android\SDK`'s, by full sha256 (`b4a6b455…`), so no adb server is restarted between them.
- **What `sitting.sh` does with page identity** is in section M, *History counted apart from live
  rows*. Nothing was changed.

**2026-11-09, 10:20 to 10:43: the interpreter qualified, and the poll counted per page.** On
`DESKTOP-BCT68L9`, offline. Nothing was done on the phone, and no poll has run.
- **The interpreter is a parameter, not a tool edit:** `PY` is an environment variable that
  `sitting.sh:37` reads with a default, `py -3.10 -I -X utf8`. Setting it leaves the tool
  byte-identical.
- **The suite did not exist as a file.** The 15 cases of 2026-11-06 are recorded only as descriptions
  (section M): no runner and no stand-in for adb was committed, in `V` or under `C:\Users\Fede`. They
  were REBUILT from those descriptions, C1 to C15, each with the exit and the message the code
  implies, against `standin_adb.sh`, which answers only the reads `sitting.sh` makes in a session and
  fails the case on any other. **This is a rebuild, not the freeze's suite;** whether that one survives
  on `nbpromec1` is not known here.
- **The first run of the rebuilt suite measured nothing:** the stand-in had a syntax error (an
  apostrophe inside `${…:?…}`), so every adb call returned empty, and 18 of 22 cases failed at their
  first step. The error was fixed, and the run is not counted. It did show the checks failing when
  the stand-in is broken.
- **Python 3.14.0 is QUALIFIED, for the behaviours the suite covers.** `PY="py -3.14 -I -X utf8"`,
  `3.14.0 (tags/v3.14.0:ebf955d, Oct  7 2025, 10:15:03) [MSC v.1944 64 bit (AMD64)]`. On the frozen
  tools, 10:24 to 10:29: **C1 to C15, 15 of 15.** The bound, recorded beside it: the mark, the binding,
  the poll's invariants, counts and target, the rate rule, failed journal reads and the whitelist.
  `rows`, `dump`, `state` and `poll` are not covered.
- **The invariant did its job, executed and not reasoned.** The frozen `sitting.sh`, with `PY` unset so
  that its default `py -3.10` applied, taking a mark against the stand-in: "No suitable Python runtime
  found", then `INVARIANT VIOLATED [journal]`, "the read or the extraction exited 103", exit 3, and no
  mark written. So a sitting on a missing interpreter would have stopped at its first read rather than
  run silently on another. A loud precondition is usually noticed only when it fails to fire, which is
  why this is written down.
- **The change, by the owner's authorisation, declared before any poll, touching only how pages are
  read** (section M, *History counted apart from live rows*, MADE):

  | File | Old sha256 (frozen 2026-11-06) | New sha256 (2026-11-09 10:42) |
  |---|---|---|
  | `sitting.sh` | `854dca023884bc3dfe6f643d28b019fe0bda4f95f5df1bf4ec17d02a28d93164` | `13cb9ff5394f3fddcf3f67e57d3c6befc1a4a28e4357d33c0cb542852e567a8f` |
  | `journal_extract.py` | `f5abb4920daaccd9d9a9f1a9448fb705ae47d1ce28ba55dc71f778253c2cc678` | `340e263f1397e8c441d80090aca5d3b383187ef267466c471e182787deaa6e43` |

  - **No threshold, band or verdict rule moved,** read in the diff: only the extract's page token, the
    grammar check's page token, the per-page counts, page A as LIVE, and the `pages` invariant.
- **Exercised before the sitting,** every case under `PY="py -3.14 -I -X utf8"`:

  | Case | Frozen tools | New tools |
  |---|---|---|
  | C1 a healthy session, an earlier session's lines excluded by the mark | PASS, exit 0 | PASS, exit 0 |
  | C2 another app in front | PASS, 0 | PASS, 0 |
  | C3 the process gone at the second poll | PASS, 3 | PASS, 3 |
  | C4 a poll's line outside the whitelist, nothing saved | PASS, 3 | PASS, 3 |
  | C5 a rate of 60 with the range [0 60] | PASS, 0 | PASS, 0 |
  | C6 an unreadable journal at a poll | PASS, 3 | PASS, 3 |
  | C7 a rejected `messageCount`, no bound until the cap | PASS, 4 | PASS, 4 |
  | C8 a journal rotated past the lines saved | PASS, 3 | PASS, 3 |
  | C9 a mark with the app running | PASS, 3 | PASS, 3 |
  | C10 a process bound before the mark | PASS, 3 | PASS, 3 |
  | C11 a reused label | PASS, 3 | PASS, 3 |
  | C12 a poll with no mark | PASS, 3 | PASS, 3 |
  | C13 two consecutive calls of one session | PASS, 0 | PASS, 0 |
  | C14 a mark whose journal is unreadable | PASS, 3 | PASS, 3 |
  | C15 a mark whose extractor returns other than one instant | PASS, 3 | PASS, 3 |
  | P1 B's history against A's rows, A at 800 live | `LIVE=400`, cap, exit 4: wrong | `A_live=800 LIVE=800`, target, 0 |
  | P2 B pressed after A | `LIVE=349`, B's count: wrong | `A_live=800`, `B_live=750`, target, 0 |
  | P3 labels by order of appearance, not the account | `LIVE=400`: wrong | A is the first account to appear, target, 0 |
  | P4 the first long-press on B | not caught, exit 4: wrong | `INVARIANT VIOLATED [pages]`, 3 |
  | P5 B's rejected `messageCount` | `LIVE=UNKNOWN` for A: wrong | `LIVE=800`, target, 0 |
  | P6 a result with no account | `LIVE=395` | `UNATTRIBUTED=2`, `LIVE=795`, against every page, 4 |
  | L nothing saved or printed outside the whitelist | PASS, 115 files, 448 lines | PASS, 115 files, 451 lines |

  - **The frozen tools' column is 22 of 22 against the expectations of today's behaviour,** and the new
    tools' is 22 of 22 against the corrected ones.
  - **The P cases discriminate:** the frozen tools run against the corrected expectations fail all six,
    and pass the other 16.
  - **All three runs used copies byte-identical to the committed files,** by sha256.
- **Committed, both branches, the same blobs,** in two commits each, so that the suite precedes the
  change it tests:
  - `experiment/startup-5d-control`: the suite at `209af66`, the change at
    `bff7379989d85fbb1c0f51514df1a59ba3cfa600`;
  - `experiment/startup-5d-treatment`: the suite at `c78c52d`, the change at
    `6dc2351d58d58c996c84d37d409480e8b1c4d5fe`.
  - Scanned before the push, the repository being public: 364 added lines for the suite and 68 for the
    change, with no hit.
- **`C:\Users\Fede\StudioProjects\tmc-5d-round\V` now holds the new tools,** written from the committed
  blobs and hashed after: `13cb9ff5…` and `340e263f…`.

**2026-11-09, 10:50 to 11:40: whether the rebuilt suite's cases look, checked by mutation.** On
`DESKTOP-BCT68L9`, offline, with three read-only reads of the phone. No poll has run.
- **The owner accepted the rebuilt suite conditionally, and not for haste:** a rebuild can carry more
  evidence than the original did. The six P cases are demonstrated to look, since they fail on the
  frozen tools and pass on the new ones. C1 to C15 pass on both, so they discriminated nothing until
  shown to, and they are what the interpreter qualification rests on. A wrong extractor there would
  make the session not void but WRONG, silently.
- **The method.** 26 exact mutants of the per-page tools (`apply_mutant.py`), one per behaviour the
  qualification claimed. Each is applied only if its text occurs exactly once, to a fresh copy taken
  from `6dc2351`. The whole suite then runs on it, with the "new" expectations, and the tools are
  restored from the commit and verified by sha256 against the blob: 26 of 26. The predictions were
  written into the table before any run. M26 was added after M01 to M25, which had missed the pid
  check, and was predicted before its own run.

  | Mutant | Family | Predicted | Killed by |
  |---|---|---|---|
  | M01 | the mark excludes an earlier session's lines | C1 | C1, C2, C5, C7, C13, and P1-P6 |
  | M02 | the mark is taken in the journal's own clock | C1 | C1, C2, C4, C5, C7, C8, C13, C14, C15, and P1-P6 |
  | M03 | the mark refuses a running app | C9 | C9 |
  | M04 | the mark is exactly one instant | C15 | C15 |
  | M05 | a label is never reused | C11 | C11 |
  | M06 | a poll needs a mark | C12 | C12 |
  | M07 | the process is bound after the mark | C10 | C10 |
  | M08 | every poll checks the bound process is alive | C3 | C3 |
  | M09 | the bound pid keeps its start time | SURVIVES | **none: NOT DEMONSTRATED** |
  | M10 | a failed journal read stops the poll (`sitting.sh`) | C6, C14 | C6, C14 |
  | M11 | an empty journal input is an error (extractor) | C6, C14 | **none: NOT DEMONSTRATED** |
  | M12 | the lines saved by earlier polls come first | C8 | C8 |
  | M13 | a line outside the grammar stops the poll | C4 | C4, L |
  | M14 | the extractor prints only whitelisted fields | C1 | C1, C2, C3, C5, C7, C8, C13, and P1-P6 |
  | M15 | a value matches its field's shape (`messageCount`) | C7 | C7, P5 |
  | M16 | a source is one of the six values | SURVIVES | **none: NOT DEMONSTRATED** |
  | M17 | a rejected `messageCount` leaves no bound | C7 | C7 |
  | M18 | LIVE is the rows less the history before them | SURVIVES | not by C; P1, P2, P5, P6 |
  | M19 | the target is reached at exactly the target | SURVIVES | not by C; P1, P2, P3, P5 |
  | M20 | the session ends at its cap | C7 | C7, which never ended: the suite timed out in it |
  | M21 | no rate is read with another app in front | C2 | C2 |
  | M22 | the front is read again 2.2 s later | SURVIVES | **none: NOT DEMONSTRATED** |
  | M23 | the rate is recorded with its range | C5 | C5 |
  | M24 | a second call continues the labels | C13 | C13 |
  | M25 | a second call continues the saved extract | C13, C8 | C8, C13 |
  | M26 | the live pid is the bound pid | SURVIVES | **none: NOT DEMONSTRATED** |

  - **How each died:** an exit other than the case's expected one, or its expected message missing, or
    one of its file checks failing, as each case line records in the run's output. M20 died because C7
    never ended: the suite timed out at 900 s inside it.
  - **M02 depends on the hour:** a mark from the host clock excludes the cases' synthetic lines,
    dated 10:00 to 10:40, only when run after 10:40. It was run at 11:02.
  - **No case was adjusted, and nothing in the tools was changed.**
- **24 of the first 25 predictions held. M11 did not, and why is the finding of the check.**
  - **Measured on the phone:** `adb exec-out run-as … cat` on a missing file exits 0 and writes the
    error on stdout. `adb shell` exits 1 for the same read.
  - **`standin_adb.sh` exits 1 for that read,** so in C6 and C14 the pipe's own check catches the
    failure before the extractor's exit 5 is needed. On the phone that exit 5 is the only guard.
  - **So C6 and C14 model a failure the phone does not produce,** and the guard the phone relies on is
    not demonstrated. Correcting the stand-in to the measured exit 0 would be a change to the
    instrument, not to a case. It is the owner's to authorise.
- **U1, whether the state C4 reaches with a test double is reachable with the real extractor: it is,
  from non-ASCII digits.** The committed extractor, fed a hostile journal of 6,000 lines, emitted 24
  lines outside `sitting.sh`'s grammar, and 10 with the mark at 10:05. All were
  `messageCount=٤٠٠`: Python's `\d` matches non-ASCII digits, which the grammar's `[0-9]` does not.
  - **The consequence is loud, never silent:** the grammar stops the poll with
    `INVARIANT VIOLATED [extract]`.
  - **Unreachable from what the app writes, by reading and by the data.** The four count fields are
    `Int` and `Long` values written with `toString`, which is ASCII whatever the locale
    (`ChatFragment.kt:1426`, `:2959-2961` at `00c1e78`). The phone's journal through the committed
    extractor: 3,201 lines read, 1,380 kept, 0 outside the grammar, and 0 non-ASCII bytes in 403,784.
  - **The extractor's shapes are wider than the code's.** Making them ASCII would be a change beyond
    how pages are read, so it was not made; it is the owner's.
- **Instruments of mine that did not look, today, caught and not counted:** the stand-in's syntax
  error, which made the first suite run measure nothing (recorded above); the hostile-journal
  generator, which crashed after 18 lines on Windows' console encoding; a `since` that lay past every
  instant of that journal, so that its second run read nothing; and the stand-in's exit status for
  `exec-out`, which made C6 and C14 test the wrong guard. Each was re-run, or is reported here, before
  any result rests on it.
- **Committed beside the suite, on both branches:** `experiment/startup-5d/mutation/`, at `99750a8` on
  `experiment/startup-5d-control` and `db4d701` on `experiment/startup-5d-treatment`. It holds the
  mutant table, the driver, the hostile-journal generator and its check, and the results with every
  line above.
- **The qualification is restated** in section M, with its demonstrated coverage, and what the session
  rests on that is not demonstrated.

**2026-11-09: things done right in qualifying the tools, recorded as such by the owner.**
- **a. `PY` is an environment variable** (`sitting.sh:37`, `PY="${PY:-py -3.10 -I -X utf8}"`), so the
  interpreter is a recorded PARAMETER and the tool stays byte-identical. Of the two possibilities,
  that was the better one.
- **b. The invariant was EXECUTED, not reasoned.** With `PY` unset, the `py -3.10` default applied, and
  the tool stopped at its first read with `INVARIANT VIOLATED [journal]`, exit 3, no mark written: a
  loud precondition whose firing was actually seen.
- **c. The interpreter was kept out of `state`,** because adding it would have been an edit beyond
  page reading. Declining to improve something because it is outside the authorised change is harder
  than doing it, and it is recorded as such.
- **d. The `pages` check is approved.** It stops the poll when the first press is not on page A,
  instead of silently measuring the wrong page. It is within the carve-out, since it reads pages, and
  it has the right shape: a loud precondition naming its invariant.
- **e. The process finding: a suite that qualifies a frozen tool must be COMMITTED WITH IT.** It was
  not, which is why the original does not exist today. Committing the rebuilt suite before the change
  it tests, `209af66` and `c78c52d` before `bff7379` and `6dc2351`, is the fix for that defect, and is
  named as the fix.
- **f. The uncounted first run,** which measured nothing because the stand-in had a syntax error,
  stays recorded as uncounted. It is of the same family as the four others of the day.

**Queued, not now and not blocking:** whether the original suite survives on `nbpromec1`. If it turns
up, it is a cross-check on the rebuild, nothing more: the rebuild and its mutation check are what the
session rests on.

**2026-11-09, 11:45 to 12:00: the stand-in corrected, the replaced process covered, and three rulings,
before any poll.** On `DESKTOP-BCT68L9`, offline. No poll has run, and the tools did not change.
- **M11 was the finding that justified the pass, the owner's reading.** An unreadable journal would
  have read as "nothing new", silently, for up to 140 minutes. It was found by a mutant that SURVIVED,
  which is a mutant doing its job exactly.
- **The stand-in corrected, by the owner's authorisation:** a failed `exec-out` read exits 0 with the
  error on stdout, as measured on the phone. That is a change to the test instrument, not to a case,
  justified by a measurement on the device. `standin_adb.sh` went from `17dfbfca…` to
  `18de908d2533adde5b2e6ffd0f686ecc00abd6f7f094475385e8ca00e497245c`.
  - **Predicted before the runs:** 22 of 22 on each version, and M10 and M11 killed by C6 and C14.
  - **Run, 11:49 to 11:53:** 22 of 22 on the per-page tools and on the frozen ones. **M11 now dies, by
    C6 and C14,** and M10 still does.
  - **Under M11, C6 showed the silent failure:** the unreadable journal read as nothing new, with
    `HISTORY_NOT_YET_RECORDED_SINCE_THE_MARK LIVE=none`, and the poll ran on to `CAP REACHED`, exit 4.
    Unmutated, C6 stops with `INVARIANT VIOLATED [journal]`, "the read or the extraction exited 5":
    the guard the phone relies on, now seen to fire.
- **The replaced process, two NEW cases, no existing one changed** (by the owner's ruling, M09
  included as cheap): R1, a new pid at the second poll, and R2, the same pid started again. The
  stand-in answers the start time from `start[.N]`. `sitting_suite.sh` went from `dcb47e98…` to
  `99dd85509f62bb2d0f61b09aefaef81e0092a9068f5c0bd82d361f8a64e33e3e`, and `standin_adb.sh` from
  `18de908d…` to `84c8205e23bcf68a264b7d957eccf841bccd63361c5a2107a90e2fddc99f5405`.
  - **Predicted:** R1 kills M26 and R2 kills M09.
  - **Run, 11:54 to 11:59:** 24 of 24 on each version. **M26 was killed by R1 alone, and M09 by R2
    alone.** Under each mutant the poll followed the replaced process for 8 polls, to its cap.
  - **Unmutated, each case stops at once with its reason:** "returns '888', not 777: the process was
    restarted", and "pid 777 is alive but started at '5555', not 4242".
  - **The owner's reason for the case:** over 65 to 140 minutes with the screen awake a restart is
    plausible, and a poll following a new pid would read the new process's history as a continuation,
    with the count collapsing to 400: visible in the data but easy to misread.
- **Two behaviours stay NOT DEMONSTRATED, accepted with their reasons:** the 2.2 s rule and the six
  sources (section M, the interpreter).
- **The cap's kill is the harness's,** and recorded as such so that nobody reads it as a case-level
  kill (section M, the interpreter).
- **The digit check is NOT changed: a decided exception,** with its proof beside it (section M, the
  interpreter).
- **U1, a finding in its own right: the "unreachable" state was reachable.** The real extractor, fed a
  hostile journal, emitted lines outside the grammar, for non-ASCII digits. Going to look produced a
  better answer than a guard would have: a proof that the app cannot write such a digit, an empirical
  check that its journal holds none, and the knowledge that the grammar stops one loudly if it ever
  appears.
- **Committed on both branches, two commits each,** so that each carries the instrument its evidence
  was made with: the stand-in fix at `c33051b` on `experiment/startup-5d-control` and `d180859` on
  `experiment/startup-5d-treatment`; then R1, R2, `start[.N]` and the results at `e519a56` and
  `e4d953f`. The whole `experiment/startup-5d` tree is identical on the two branches. Every added line
  was scanned before the push.

**2026-11-09, recorded by the owner before the session: three things done right.**
- **M11's kill.** Under the mutant, C6's unreadable journal read as `LIVE=none` and the poll ran to its
  cap with exit 4; unmutated, it stops with `INVARIANT VIOLATED [journal]`, the exit 5 that is the
  phone's only guard, now seen to fire. That was the difference between a sitting and 140 minutes of
  nothing that looked like something.
- **M20 recorded as a HARNESS kill** rather than passed off as a case kill, because C7 cannot assert the
  cap without being rewritten. Declaring an accidental kill accidental is the discipline.
- **Each branch carries two commits,** one holding only the stand-in's bytes that the 22-of-22 and
  M10/M11 runs used, so that every piece of evidence points at the exact instrument version that
  produced it.

**2026-11-09, 12:00 to 12:40, STEP 0 before the mark: a press only with the dev app in front.**
- **The read:** the poll does not verify the foreground before a press, because the poll never presses.
  `sitting.sh` only reads; the presses of the qualification launch came from an ad-hoc script in the
  agent's scratch directory, which checked nothing.
- **So the check lives in `press.sh`, the round's only input tool,** committed beside the others. Before
  every press, two loud preconditions, in the form of the `pages` check: `[front]`, the dev app's window
  has focus, read immediately before the press; `[target]`, exactly one refresh button on screen and the
  press, or a swipe's start, inside it. Either failing presses nothing, exit 3. Focus is read again
  after the press; another window then prints `INTERRUPTED [front-after]`, exit 4.
  - **Why a swipe starts on the refresh button:** on a message row a drag from the right edge to the
    left arms swipe-to-reply (`ChatFragment.kt:415-452`), which would take the gesture from the pager.
  - **From the accounts page,** where a launch opens: `swipe-from-accounts`, whose target is no refresh
    button and the accounts list on screen, the swipe inside the list, whose rows have no swipe action.
  - **Only the button's or the list's bounds are read** from the screen's hierarchy; no text is printed
    or saved.
- **Exercised against the stand-in,** which now records each `input` instead of executing it:
  `press_suite.sh`, 14 of 14. Mutants, one per precondition: front, killed by T2; exactly one refresh
  button, by T3; no refresh button on the accounts page, by T14, a new case added after the mutant
  survived T12, whose screen held no accounts list; T12 is unchanged. The hierarchy-read check, removed,
  survives: the next check refuses the same screen, a redundant layer. The suite's own exit had ignored
  survivors, a flag set inside `$( )`; fixed before it counted. `sitting_suite.sh` still 24 of 24 on both
  tool versions with the extended stand-in.
- **Hashes:** `press.sh` new, `ef1cd5e50d0bdff25083b215e3f3bc13c63f18c7835d93f7aae1a048dd1eae74`;
  `press_suite.sh` new, `e7f790b078516fb829727d3a78c817d63981acd9c07d5dcebb823f32e1261b96`;
  `standin_adb.sh` `84c8205e…` to `b6ad46472925ef7239678f26098306db5ef06543f988685be9ee89cff20fb018`;
  `sitting.sh` and `journal_extract.py` unchanged. At `269acc2` on `experiment/startup-5d-control` and
  `faed125` on `experiment/startup-5d-treatment`, the trees identical. One cherry-pick onto control first
  failed, refusing a working file that an override of `core.autocrlf` made look modified; it was redone
  without the override, and the trees compared.
- **Declared before the mark, for this sitting:**
  - **The refresh bracket comes after the reading at about 800 rows,** as section M pre-registers.
    Today's instruction says "after the third reading", which is the same moment if the rate's second
    long-press is counted as a reading; the pre-registered rule is followed.
  - **The rows are read by a long-press on A every few minutes,** each followed at once by a dump, so
    that the poll's target can be reached, since LIVE moves only at a long-press. The three readings are
    those at about 400, about 800, and the one that reaches the target.
  - **Section M's checks by hand are NOT performed in this sitting:** at each reading the scroll, follow,
    jump and emote checks, and once at 1,200 rows hiding and showing a user and sending a message. They
    need Federico's hands and eyes; the send posts a chat message under his account, which the agent
    does not automate; and today's instruction counts a touch on the phone as an interruption. Their
    scheduling is the owner's. Duplicates are not identified by eye; the refresh bracket is the count.
  - **Interruptions are watched by:** `press.sh`'s front reads, the poll's invariants and its rate
    lines, and a `getevent` recorder of the touchscreen, which records touches by hand but not `adb
    input`.

**2026-11-09, 12:31 to 12:44: the long session `L5D1`, INTERRUPTED at its 8th poll by `INVARIANT
VIOLATED [kept]`, a defect of the tools on Windows, not a change in the journal.** On
`DESKTOP-BCT68L9`, with the build `ba1f88e3…` on the phone. Not continued and not restarted. Every
output, uncut, is in `C:\Users\Fede\StudioProjects\tmc-5d-round\V`: `session-L5D1.log`,
`rates-L5D1.txt`, `dumps.txt` with its two meminfo files, `presses.txt`, `journal-extract-L5D1.txt`
and `getevent-L5D1.txt`.
- **Before the mark, 12:31:29:** battery saver 0; USB power, AC and wireless off; status 5, full, level
  100; 25.8 °C; thermal status 0; `stay_on_while_plugged_in` 7, `mStayOn=true`, awake;
  `screen_off_timeout` 1,800,000; the dev app not running; `PY="py -3.14 -I -X utf8"`, `3.14.0
  (tags/v3.14.0:ebf955d, Oct  7 2025, 10:15:03) [MSC v.1944 64 bit (AMD64)]`; adb 37.0.1; the tools
  `13cb9ff5…`, `340e263f…` and `ef1cd5e5…`. The display read 90.0 Hz with the launcher in front, which
  is not the session's rate reading.
- **The mark,** 12:31:49, at the journal's last line, 08:43:46.267. A cold launch, 1,496 ms; one
  `swipe-from-accounts` to A's page; the process bound, pid 32178, start 2418583.
- **Both pages on one channel:** A's first connect, 400 rows, at 12:31:56.320, at the launch; B's, 400
  rows, at 12:32:08.719, when A came on screen; A's `resume_no_pause`, 9.
- **The rate before,** read after the dev app had been in front 2.2 s: 90.0 Hz, range [0 90].
- **Reading 1, 12:32:49 to 12:33:13:** A's long-press, `shownRows=414` at 12:32:52.811; the dump with
  A on screen, `Views: 158`, 0.66 s; B's swipe-press-swipe, B's `resume_no_pause` of 5 as it came on
  screen, B's long-press `shownRows=412` at 12:33:09.397, and A's `resume` of 5 on coming back. So N =
  826 rows at the first reading, both pages summed.
- **The polls,** 12:33:43 to 12:41:43, eight: the process invariant held at each, the rate was read at
  each, 90.0 Hz with the dev app in front at both reads, and LIVE was 5, then 226 from the count below.
- **A count, 12:39:59:** A's long-press, `shownRows=640` at 12:40:02.803, and a dump, `Views: 144`. From
  414 to 640 in 430 s, with A's 5 history messages between, the live rate was **30.8 to 31.5 rows a
  minute**, two and a half times the planned 12.3.
- **The 8th poll stopped with `INVARIANT VIOLATED [kept]`** at 12:41:43: the 13 saved lines no longer
  came first.
- **No interruption of another kind:** 0 touches recorded by hand; every press passed `[front]` and
  `[target]`, with focus on the dev app after it; no process replacement; no rate line found another
  app in front.
- **At the interruption, 12:43:47:** battery saver 0; USB power; status 5, level 100; 26.1 °C; thermal
  status 0; 90.0 Hz, read with the dev app in front 2.2 s; `stay_on_while_plugged_in` 7, awake; the
  same build; pid 32178, the bound process; the same interpreter.
- **The cause, executed and not reasoned.** Windows' Python writes each line with CRLF, and this
  shell's command substitution strips the carriage return of the LAST line it captures only. So a
  line's saved bytes depend on whether it was the last of its read: poll 1 saved 12 lines, the 12th
  without its CR; once a 13th followed, line 12 came back with its CR. Poll 7 still matched, since it
  compared 12 lines and the substitution stripped line 12's CR as the last again. Poll 8 compared 13.
  - **Reproduced offline,** with the session's very tools against the stand-in and a journal growing
    over three polls: as the session ran, `[kept]` at the third poll; with the interpreter's output
    stripped of CRs, the polls ran on to their cap, LIVE 100, 200, 300, and the saved extract held no CR.
  - **Two of the agent's instruments in that diagnosis failed first,** and neither counts: a wrapper
    meant to strip the CRs held a literal CR byte, which this shell drops from scripts, so it stripped
    nothing; and the explanation, first stated before that run, was retracted until the corrected run
    confirmed it.
- **Not caught offline because no case looks there:** no case of the suite runs three polls with the
  journal growing after a saved line; C13 has two. By their descriptions, the cases of 2026-11-06 had
  the same gap. `session-poll` had never run on the phone before this session.
- **The outcome, in section M's words.** Reading 2, the refresh bracket, reading 3 and B's last count
  did not happen: those readings are **VOID**, absent. So the view criterion has no last reading and
  was not judged. It is not DROPPED, which would say the instrument does not separate at this size,
  and not INCONCLUSIVE, which would say judgements were made and do not decide. No duplicate was
  counted, since no bracket happened.
- **Left as it stood:** the dev app still running on A's page, bound pid 32178, the screen held on by USB
  power; the touch recorder still running. Nothing was pressed or polled after 12:43:47. Whether to end
  it, or what to do next, is the owner's.

**2026-11-09, 12:56 to 13:10: L5D1 closed, the line-ending fix at the source, and the new session
recomputed, before its mark.** By the owner's four rulings.
- **Closed:** the dev app force-stopped at 12:56:19, pid 32178 gone; the touch recorder stopped.
  - **The recorder had caught two taps by hand, after the session:** at 12:42:24, at (1026, 196) on the
    header's right end and not on refresh, and at 12:42:26, at (329, 2344) near the bottom. Both came
    after the last poll, at 12:41:43, so they touch none of `L5D1`'s readings. What they did is not
    known.
- **What stays set on the phone after this sitting, read and not changed,** written down so that nobody
  finds it in a month and wonders who set it, which is the lesson of the probe flag that survived for
  days and was found by accident:
  - **"Stay awake while charging", `stay_on_while_plugged_in` = 7, STAYS ON:** the next session needs
    it. It has read 7 since 2026-11-02; the agent did not set it.
  - **"Show taps", `show_touches` = 1, ON.** A developer option, not set by the agent; it draws touch
    markers in a system layer, not in the app.
  - USB debugging 1, developer options 1, wireless debugging 0; `screen_off_timeout` 1,800,000, which
    changed on 2026-11-06 and not by the agent; battery saver 0; Do not disturb 0; animation scales
    1.0, 1.0 and the default; automatic brightness, 556.
  - **The start-up probe's flag: absent.** Its directory still holds `latest` and the 211 run files,
    about 40 MB, for the queued cleanup.
  - The dev app, `5.5.1-dev`, build `ba1f88e3…` of `00c1e78`, not running, with no battery-optimisation
    exemption.
- **The fix, at the source, by the owner's authorisation: the extractor writes LF.**
  - **Not in `sitting.sh`:** two places normalising line endings are two places that can disagree, the
    lesson of `strictly()`. **And no second layer:** it exists already, the `[kept]` invariant,
    demonstrated on the phone today. A CR that came back would stop the poll again.
  - **`[kept]` was NOT a false positive on its own terms.** The bytes of that line genuinely differed,
    and it said so; the defect was upstream. The alternative is a comparison that accepts misaligned
    bytes, and a session that ends with wrong numbers instead of an error.
  - **One line,** `sys.stdout.reconfigure(newline="\n")`, with its reason.
    `journal_extract.py` `340e263f1397e8c441d80090aca5d3b383187ef267466c471e182787deaa6e43` to
    `63ec6b914eac8efa7abb27c71c0d743a7b4946815a2fb867aba13d7750e3196b`; `sitting.sh` unchanged,
    `13cb9ff5…`.
  - **The new case, K1,** three polls with the journal growing (`sitting_suite.sh` `99dd8550…` to
    `3bb0a0505278097886ac527023a0fe4161ecfd41d0b74c89274c513d0654c646`). Predicted before the runs, and
    so run, 12:58 to 13:04: **on today's per-page tools K1 FAILS,** 24 of 25, with `[kept]` at its third
    poll and a CR in the saved extract; **on the fixed tools it PASSES,** 25 of 25, polling through to
    its cap with LIVE 100, 200, 300 and no CR saved; and the frozen tools, with the old expectations,
    pass 25 of 25, K1 recording today's behaviour.
  - **The same gap was in the case descriptions of 2026-11-06,** none of which runs three polls with the
    journal growing. So it is not a weakness of the rebuilt suite: the original would not have caught
    it either.
  - **Committed, two commits each, the case before the fix:** `7f4be02` then `6c74ed3` on
    `experiment/startup-5d-treatment`, and the same on `experiment/startup-5d-control` ending at
    `85a1314`, the trees identical. The session's folder holds `sitting.sh` `13cb9ff5…`,
    `journal_extract.py` `63ec6b91…` and `press.sh` `ef1cd5e5…`.
  - **The mutation check of the extractor ran on the version before this line,** and the line changes
    none of the texts its mutants change; it was not re-run.
- **The new session is a new session, with a fresh mark,** not a resume: `L5D1`'s saved extract carries
  the carriage returns, so a resume would compare clean bytes against dirty ones.
- **Recomputed before the mark, from the MEASURED 30.8 to 31.5 rows a minute,** not the planned 12.3:
  - **To the target:** 800 live rows on A take 25.4 to 26.0 minutes, not 65, so the 140-minute cap is no
    longer the binding constraint.
  - **The refresh bracket's E, for 10 s:** 5.13 to 5.25 rows; 2E is 10.27 to 10.50 and 4E 20.53 to
    21.00. In whole rows: consistent 0 to 10, inconclusive 11 to 20, mismatch 21 or more. The bands of
    the session are computed again at the refresh, from the live rate between its two readings and the
    bracket's measured length. With no mismatch, a Poisson model gives 1.6 to 1.9% INCONCLUSIVE and no
    false mismatch.
  - **A total mismatch would now add about 62 rows,** 61.6 to 63.0 over the refresh's 120 s window, not
    25.
  - **The floors on N_A, unchanged at 551 and 701** (the planned N_start of 800, C = 101), are reached
    4.3 to 4.4 and 9.1 to 9.3 minutes after a first reading at 414. The floor is computed again at the
    first reading, from its measured N_start.
- **The owner's first fixed bands would have been useless at this rate:** with no mismatch at all, 0-3 /
  4-7 / 8+ would have read 4 or more in 75 to 77% of brackets, and a false mismatch in 15 to 16%. Tying
  them to a measured E, computed against a 24-rows-a-minute case when the rate arrived at 31, is what
  makes the criterion survive it.
- **What does NOT change, and why:** at 31 rows a minute the 800 kept keys span 26 minutes, not 65, so
  the RATE-BASED argument against duplicates by eviction would have fallen: a 3600 s window outlasts
  them. The closure does not rest on it. It comes from the backend's query returning the newest 400,
  which holds at any rate. Reading the code was worth more than doing the arithmetic.
- **The checks by hand stay OUT of the automated session,** in a separate pass after it (section M, the
  long session).

**2026-11-09, 13:06 to 13:40: the long session `L5D2`, run to its TARGET. The view criterion PASSES,
under C = 101 and under C = 67, and the refresh bracket is consistent with no mismatch.** On
`DESKTOP-BCT68L9`, with the build `ba1f88e3…` of `00c1e78` on the phone, the tools `13cb9ff5…`,
`63ec6b91…` and `ef1cd5e5…`. Every output, uncut, is in `C:\Users\Fede\StudioProjects\tmc-5d-round\V`:
`session-L5D2.log`, `rates-L5D2.txt`, `dumps.txt` with its six meminfo files, `presses.txt`,
`journal-extract-L5D2.txt` and `getevent-L5D2.txt`; `L5D1`'s are in its subfolder.
- **Before the mark, 13:06:49:** battery saver 0; USB power, AC and wireless off; status 5, full, level
  100; 25.6 °C; thermal status 0; `stay_on_while_plugged_in` 7, `mStayOn=true`, awake;
  `screen_off_timeout` 1,800,000; the dev app not running; `PY="py -3.14 -I -X utf8"`, `3.14.0
  (tags/v3.14.0:ebf955d, Oct  7 2025, 10:15:03) [MSC v.1944 64 bit (AMD64)]`; adb 37.0.1. The display
  read 90.0 Hz with another app in front.
- **The mark,** 13:07:13, at the journal's last line, 12:54:21.600. A cold launch, 1,439 ms; one
  `swipe-from-accounts`; the process bound, pid 4469, start 2631071.
- **Both pages on one channel:** A's first connect, 400 rows, at 13:07:21.192; B's, 400, at 13:07:32.824;
  A's `resume_no_pause`, 18.
- **The rate before,** with the dev app in front 2.2 s: 90.0 Hz, range [0 90]. **At every one of the 29
  polls,** the same, with the dev app in front at both reads; and after, the same.
- **The readings and the counts,** each long-press on A followed at once by a dump with A on screen:

  | | Phone time | A `shownRows` | B `shownRows` | `Views:` | PSS, KB | Java heap, KB |
  |---|---|---|---|---|---|---|
  | Reading 1 | 13:08:02.138, B 13:08:18.954 | 416 | 425 | 163 | 133,646 | 17,544 |
  | Count | 13:14:56.029 | 586 | | 168 | 139,799 | 19,712 |
  | Reading 2 | 13:25:03.149 | 944 | | 162 | 157,625 | 29,708 |
  | Count | 13:32:09.803 | 1,132 | | 167 | 145,967 | 19,696 |
  | Reading 3, the target | 13:38:45.287, B 13:39:12.822 | 1,499 | 1,510 | 167 | 146,894 | 20,340 |

- **The target,** at the poll after reading 3: `TARGET REACHED: at least 1017 live rows >= 800`. The
  session ended there, at its target, and not on a `Views:` reading. Its length, from the mark, 31.5
  minutes.
- **The view criterion: PASS, under both C, so the verdict is PASS.** The ratio is 167 / 163 = 1.0245;
  N_start = 416 + 425 = 841 and N_end = 1,499 + 1,510 = 3,009, both pages measured, k = 1.00.
  - **C = 101, the primary:** R = 3.3015, M = 1.8170, pass at or below 1.5732, fail at or above 2.0986.
    PASS.
  - **C = 67, beside it:** R = 3.3877, M = 1.8406, pass at or below 1.5936, fail at or above 2.1258. PASS.
  - **They agree,** and k at 1.003 moves no edge by more than 0.0003. R is far above 1.334, and N_end far
    above its floor, 1,156 at C = 101, so the criterion is not DROPPED. Each row a view would have read
    about 3.30.
- **B's measured counts:** 425 at reading 1 and 1,510 at reading 3. B grew as A did, 1,085 rows against
  A's 1,083, while it was STARTED off screen: measured, no longer only well founded. As it came on
  screen at the end, after 31 minutes away, B took a `resume` backfill of 400 messages, and its count
  still ran level with A's: consistent with its connection held and those copies suppressed. An
  observation, not a test.
- **The live rate:** between readings 1 and 2, by the poll's bound, 511 live rows in 1,021.0 s, **30.03
  a minute**; 31.0 without the bound. Over the session A's bound went from −2 to 1,017, about 33 a
  minute, with a burst of about 56 a minute between the last two counts.
- **The refresh bracket, after reading 2, with nothing else touched:**
  - A's long-press, `shownRows=953` at 13:25:33.762; the tap, a `manual_refresh` triggered at
    13:25:39.176 and its result 368 ms later, `messageCount=47`; A's long-press, `shownRows=957` at
    13:25:45.351, 5.8 s after the result. No other line between them, so the bracket counts.
  - **Δ = 4 rows in 11.589 s.** At the measured 30.03 a minute, E = 5.80, so consistent below 11.60,
    inconclusive from 11.60 to below 23.20, mismatch at 23.20 or more. **Δ = 4 is consistent with no
    mismatch.** At the planned 12.3 a minute, E = 2.38 and the bands are 0-4, 5-9 and 10 or more: Δ = 4 is
    consistent there too.
  - **So all 47 of the refresh's history copies were suppressed,** keyed as their live rows were: the
    mismatch route is not found. A total mismatch would have read about 53.
- **Duplicates:** none counted. The bracket is the count; no candidate was identified by eye, the checks
  by hand being the separate pass.
- **No interruption:** 0 touches by hand over the session; every press passed `[front]` and `[target]`,
  with focus on the dev app after it; the process invariant held at every poll; no rate line found
  another app in front; the saved extract, 20 lines, holds no CR.
- **After the last poll, 13:39:50:** battery saver 0; USB power; status 5, level 100; 25.1 °C; thermal
  status 0; 90.0 Hz with the dev app in front 2.2 s; `stay_on_while_plugged_in` 7, awake; the same build;
  pid 4469, the bound process; the same interpreter and tools.
- **Closed:** the dev app force-stopped at 13:40:31 and the touch recorder stopped. Left set, as recorded
  above: `stay_on_while_plugged_in` 7, kept on; `show_touches` 1; battery saver 0; no probe flag.
- **The outcomes, in section M's words:** the view criterion PASSES; the mismatch test is consistent
  with no mismatch; no reading of this session is VOID; nothing is DROPPED or INCONCLUSIVE. The checks
  by hand remain for the hands-on pass.

**2026-11-09: what `L5D2` establishes, and its bounds, by the owner's reading.** The result is recorded
as it stands; no further automated work on the phone.
- **The headline, as a ratio of ratios:** the rows grew 3.58 times, from 841 to 3,009 across both pages,
  while the view count grew 1.02 times, from 163 to 167. Four views for 2,168 rows. A build keeping one
  view per row would have read about 3.30.
- **PASS under both C, the two agreeing, and NOT a marginal result.** The observed 1.0245 lies 0.55
  below the pass edge under C = 101, 1.5732, and 0.57 below it under C = 67, 1.5936, and nowhere near
  either fail edge, 2.0986 and 2.1258. R, at 3.30 and 3.39, is far above 1.334, and N_end, 3,009, far
  above its floor of 1,156.
- **The duplicate question is closed from two independent sides.**
  - **Eviction, by reading:** the backend's query returns the newest 400, which holds at any rate.
  - **Mismatch, by OBSERVATION:** the refresh returned 47 messages, and the timeline grew by 4 rows in a
    bracket that expected about 6 live ones. So no history copy was keyed differently from its live
    row. The band was a statistical test; the copies suppressed are the mechanism seen working, which
    is the stronger evidence.
  - **The agent's precision on the count:** Δ = 4 proves at least 43 of the 47 suppressed whatever the
    live rows. It proves all 47 if at least 4 live rows arrived in those 11.6 s, as about 6 were
    expected.
- **The fixed bands' counterfactual:** at this rate the first bands, 0-3 / 4-7 / 8+, would have read a
  false mismatch 15 to 16% of the time. That is why the bands are tied to a measured E.
- **What it establishes: recycling holds at scale.**
- **What it does NOT establish:**
  - **feature parity,** which has no unit tests by construction and rests on the hands-on pass (section
    M, *The hands-on pass*);
  - **any figure in megabytes:** this measures views, not memory. The PSS and Java heap of each dump
    are recorded as figures and not judged.
- **The owner's `claude/architecture-debt.md`** says the cost of a timeline grown past 400 rows "was
  never measured". For its view part that is now SUPERSEDED by this session, and the owner will correct
  it in the project's document. The numbers live here, in the repository plan, which is the durable
  home.
- **The hands-on pass's checklist is written,** before it runs (section M, *The hands-on pass*). #97 is
  not merged until that pass is done and reported.

**2026-11-09: six records of the owner's, before the hands-on pass.** No automated work was done.
1. **The release build type does NOT minify.** Read by the owner in `app/build.gradle.kts` on
   `origin/main-v5`, and read again by the agent there and at #97's head: `isMinifyEnabled = false`
   and `isShrinkResources = false`, with `proguardFiles` declared but inert.
   - **What it closes:** 5d has never been built with R8, and a RecyclerView adapter is exactly what R8
     touches, by stripping or renaming what reflection and binding rely on. That was a larger risk than
     the question asked about the stable flavour. With minification off, that class of defect does not
     exist.
2. **The stable flavour is NOT skipped; it MOVES to the 5.6.0 device round,** where a release-signed
   build is installed over the published one and its checks run anyway. Why the remaining risk is low,
   the owner's reasons:
   - 5d's code is in `app/src/main`; the dev harness cannot be called from main, since a flavour's
     source set is not visible from main, so main's behaviour is flavour-independent;
   - unit tests and lint pass on both flavours. That is the owner's statement; no test or lint task was
     run in this session;
   - what differs is the application id's suffix, `google-services.json` and the debuggable flag, and
     debug is the stricter of the two, the safe direction.
   - **The agent's precision, by reading `app/build.gradle.kts` at #97's head:** the flavours also
     differ in `app_name`, in the OAuth callback scheme, `ircminichat` against `ircminichatdev`
     (`authScheme`, `AUTH_SCHEME`), and in `FCM_REGISTER_URL`, which only stable defines; the dev
     flavour also carries its own resources and the harness. None of it reaches 5d: of the 12 main
     files #97 changes, none reads a flavour-dependent `BuildConfig` field. So the argument holds for 5d's
     code, which is its claim; the login and alert paths do differ by flavour, outside 5d.
3. **The consequence that changes that round's weight:** the phone's stable app is 5.5.1, from before 5d,
   so **the 5.6.0 device round is the FIRST AND ONLY time the stable flavour executes 5d** before users
   do. Its chat checks are the gate, not a formality: they are marked BLOCKING in `RELEASE-STATE.md`
   (*5.6.0 in progress*) and in `docs/manual-plan-5.5.2.md` (*Which build this round applies to*), the
   round that is to be re-run on the final 5.6.0 build. A 5.6.0 plan written as a file of its own has to
   carry the mark over.
4. **The narrowing of Δ = 4 is accepted, and both forms stand:** it proves at least 43 of the 47 copies
   suppressed, and all 47 only if at least 4 live rows arrived in those 11.6 s, with about 6 expected.
   - **It does not weaken the conclusion in substance:** 43 of 47 suppressed, with no duplicate
     observed, closes mismatch either way.
   - **But the difference between "at least 43" and "all 47" is the kind of thing that gets miscited
     later.** Cite the first as the proof, and the second only with its condition.
5. **Case 1's design is right, and was not asked for.** The long-press menu offers Report and Block, so
   a stale binding would act on another person. Verifying the binding by the dialog's `@user` title,
   the Copy pasted unsent and the reply bar, without ever tapping Report or Block, establishes it
   without taking the destructive action.
6. **The checklist's setup now says** that auto-rotate and TalkBack are both off, are switched on for
   cases 5 and 8, and are restored afterwards.

**2026-11-09: the inventory before any automation of the hands-on pass, and the owner's decision:
BY HAND.** The owner asked for the inventory before anything was automated, and no check was written.
The phone was only read: `am help`, `pm list packages`, `dumpsys package` and `input_method`,
`settings get`, `cmd window help`, `cmd clipboard help`, and one `uiautomator dump` that printed
attribute names only. In this entry, *executed* means run on the phone that day, *read* means read in
source and not run, and *unread* means not confirmed. Every effort figure is the agent's ESTIMATE, as it
was labelled. Federico's per-case minutes are the agent's split of the plan's 50-to-60-minute long
stretch, not figures from the plan.
- **The decision, the owner's:** this pass stays BY HAND, and no automation is written. "No further
  automated work on the phone" stands, and so does the checklist's "Nothing is pressed by adb".
  - **The reason is the inventory's arithmetic:** about 20 hours of the agent's effort to save about 35
    minutes of Federico's, or 8 to 10 hours for about 20 minutes with cases 1 and 9 alone.
  - **The reuse is partial:** mutants are re-applied every round, and cannot be installed over the
    release-signed stable app at all.
- **What the inventory paid for, by the owner's record: not saved time.**
  1. **It stopped case 8's automation before it was built: the most valuable result of the
     inventory.** An injected tap does not travel through TalkBack. Read in Android 12's sources,
     `android12-release`; this phone's OnePlus build was not read.
     - `input` is a script that runs `cmd input`. `InputShellCommand` calls
       `InputManager.injectInputEvent(…, INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH)` (lines 69 and 138).
       The client's binder call into `InputManagerService` was not read.
     - `InputManagerService.injectInputEventInternal` gives `nativeInjectInputEvent` the policy flag
       `FLAG_DISABLE_KEY_REPEAT` only (line 869). It never gives `FLAG_FILTERED`, which marks an event
       sent back by the input filter: `InputFilterHost.sendInputEvent` adds that one (line 3221).
     - `InputDispatcher::injectInputEvent` (line 3997) runs `interceptMotionBeforeQueueing` and queues
       the event. The input filter is offered events only in `notifyKey` (line 3791) and `notifyMotion`
       (line 3878), the path of the events the input reader produces. Nothing in the dispatcher calls
       `notifyMotion`.
     - **So an injected tap skips the accessibility input filter and TalkBack's `TouchExplorer`.** A
       double `input tap` is two ordinary taps on whatever lies under the point, and TalkBack's swipe
       navigation is out of reach the same way.
     - **And the check would have passed.** An ordinary tap on empty space writes
       `reason=outside_click` through CASE 9's path. The automated check would have PASSED without ever
       exercising the accessibility click, on the one case that tests the `ClickableViewAccessibility`
       suppression decided with the owner (*The same click, through TalkBack*).
     - **It is the sixth or later instance in this round of an instrument that would not have looked,**
       and the first caught before construction rather than after.
     - **Beside it, read:** `TouchExplorer.onDoubleTap` first asks for `ACTION_CLICK` on the
       accessibility-focused item, and simulates touches only if that fails (lines 385 to 389). The
       framework lets a service take the double tap itself (`FLAG_SERVICE_HANDLES_DOUBLE_TAP`); whether
       TalkBack 17.1.0.1003738110 sets it is unread.
     - **No screen dump while TalkBack is on.** `uiautomator dump` connects through
       `UiAutomationShellWrapper.connect()` to `UiAutomation.connect()`, flags 0, and UiAutomation's
       documentation says it "suppresses accessibility services by default". The accessibility
       service's side of that was not read.
     - **The only adb route to an accessibility click is a stand-in:** an instrumentation APK sending
       `ACTION_CLICK` through `getUiAutomation(FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)`. It reaches
       the same click in the app, but not TalkBack, so it cannot show whether TalkBack reaches the list
       at all.
  2. **The keyboard field: a wrong instrument avoided.** Executed with the keyboard closed:
     `mInputShown=false`, `mIsInputViewShown=true`, `mShowRequested=false`.
     - `mIsInputViewShown` reads true with the keyboard CLOSED, so a check built on that field would
       have been wrong.
     - Whether `mInputShown` tracks the keyboard is unread: it needs a reading with the keyboard known
       to be open.
     - **The hands-on pass avoids an unqualified instrument here,** which is a reason for it, not a
       concession.
  3. **The effort figures are ESTIMATES,** as the agent labelled them, and the per-case minutes are the
     agent's split of the plan's long stretch rather than figures from it.
- **The inventory's other general findings:**
  - **The long-press menu's "Report message" and "Block user in app" act at once, and its positions
    shift** (*Backlog*). A driver tapping by position could file a real report against a real person.
  - **A mutant's install ends the long timeline.** `install -r` restarts the app, and a timeline past
    1,000 rows regrows in 15 to 25 minutes at that day's rates; `am crash` does the same. A mutant would
    have had to be shown before the timeline was grown.
  - **The screen dump carries no opacity.** Executed: a node's attributes on this phone are `bounds,
    checkable, checked, class, clickable, content-desc, enabled, focusable, focused, index,
    long-clickable, package, password, resource-id, scrollable, selected, text`.
  - **The clipboard cannot be read from adb.** Executed: `cmd clipboard` answers "No shell command
    implementation."
  - **Executed:** `am crash [--user <USER_ID>] <PACKAGE|PID>` and `cmd window user-rotation [free|lock]
    [rotation]` exist; `user_rotation` is 0; TalkBack is `com.google.android.marvin.talkback`
    17.1.0.1003738110; `enabled_accessibility_services` is null.
  - **Read:** `app/src/androidTest` exists, with `AndroidJUnitRunner` and Espresso declared.
- **Case by case: the decider, the drive, the mutant, the judgement, and the estimates.**
  1. **Recycled long-press and swipe.**
     - **Decider, partly exact:** the dialog's `@user` title (`ChatFragment.kt:3389`), `txtReplyInfo`,
       the paste read back from `editChannel`, and `CHAT_LONG_PRESS`, each compared in-process and
       printed as match or mismatch only.
     - **Unread:** the title's resource id, the row text's layout, whether a paste survives intact, and
       whether an injected swipe arms `SwipeReplyTextView`. The long-press is qualified by `press.sh`.
     - **Mutants:** production only.
     - **Estimate:** about 3 to 4 hours, saving about 10 minutes. Its real gain would be breadth:
       dozens of recycled rows rather than three.
  2. **Stable flavour, in the 5.6.0 round.**
     - **Decider:** the crash buffer, `pidof`, the row count, and `Resources$NotFoundException`. There
       is no `run-as` on a release build. Sign-in stays Federico's.
     - **Mutants:** the crash detector through `am crash`, with the owner's say-so; the others only by
       proxy on dev.
     - **Estimate:** about 2 hours, saving about 3 minutes, in 5.6.0 only.
  3. **Hide and show.**
     - **Decider:** `CHAT_ACTION hidden-user rule updated` (`:3399`). Within the screen, the hidden
       rows gone, and the others' order and an anchor row's top kept.
     - **Unread or out of reach:** off-screen rows are unread, and `shownRows` mixes in arrivals. The
       neighbour page is likely NOT REACHED. The way to the blocked-users screen is unread.
     - **Mutants:** production only.
     - **Estimate:** about 3 hours, saving about 8 minutes.
  4. **The echo.**
     - **Decider:** the status line's node, `TWITCH_IRC outgoing message confirmed` and `reconciled`,
       and one row with the sent text.
     - **Not decidable from adb:** the opacity cannot be read, and the emote is visual. The send is
       Federico's.
     - **Estimate:** about 2.5 hours, saving about 3 minutes.
  5. **Rotation.**
     - **Decider:** the crash buffer, the row count, and the journal's figures. The two ownership
       messages are `check(...)` failures (`TwitchEmoteImageLoader.kt:311` and `:451`), so they arrive
       as crashes.
     - **Drive:** `cmd window user-rotation` would rotate without auto-rotate, and is still a phone
       setting.
     - **Estimate:** about 2.5 to 3 hours, saving about 3 minutes.
  6. **Scrolling.**
     - **Exact across dumps:** following the bottom, staying put, and the jump button's tap clearing
       the count.
     - **Unread:** the count against the rows that arrived (`countAsUnread`, `:3274`). A jump between
       two dumps is not seen.
     - **Judgements:** the tall row and the drip-in look.
     - **Estimate:** about 3 hours, saving about 4 minutes.
  7. **Emote animation.**
     - **No decider:** `EmoteAnimationGate` logs nothing, and `TWITCH_EMOTES` logs metadata only
       (`:2376`). The animation would need a frame-comparison instrument of its own.
     - **Estimate:** 5 hours or more, saving about 5 minutes.
  8. **TalkBack.**
     - **Decider, exact:** `reason=outside_click`.
     - **Drive:** the activation cannot be driven (item 1 above), so the case is wholly Federico's.
     - **Estimate:** a stand-in of about 3 hours, saving nothing.
  9. **Empty space.**
     - **Decider:** wholly exact and drivable. A tap on a row is a built-in negative.
     - **Mutant:** production, but cheap on a short chat.
     - **Estimate:** about 1.5 to 2 hours, saving about 10 minutes of attention and no elapsed time.
  - **A base shared by any automation:** about 3 to 4 hours.
- **Judgements no automation decides:**
  - the tall row's top (U2/U3);
  - the drip-in look, a scope decision for batching;
  - a jump in the scroll;
  - rows returning "together";
  - the echo's opacity;
  - TalkBack announcing the list;
  - the unstructured look.
- **The checklist's two additions, the owner's, with no case or criterion changed:**
  - case 1 chooses menu items by exact label, and the guide repeats the two immediate actions at that
    moment;
  - the 10-minute unstructured look is its own step, immediately before the closing step.
  - The total duration moved to 1 hour 30 to 1 hour 50 minutes as a consequence.
- **During the pass, the agent's role, by the owner's guided prompt:**
  - **Federico acts:** the agent guides, and Federico's hands and eyes are the instrument.
  - **What the agent drives:** only the Logcat capture, the adb reads and the TalkBack toggle.
  - **What the agent reports:** the plan's Logcat tags and the crash buffer, VERBATIM with times, and
    never a verdict from them alone. Federico looks and describes; the agent captures and correlates.
- **Carried forward, decided later and not now:** cases 1 and 9 as automation candidates for the 5.6.0
  round.
  - **The cost:** their reuse means re-applying the mutants every round.
  - **The open ruling:** qualifying them on stable would be by proxy on dev, which the owner rules on
    then, not now.

**2026-11-09, 15:29 to 18:13: the hands-on pass, run with Federico on `ba1f88e3…`.** It followed the
owner's guided prompt.
- **Who did what:** Federico acted and looked. The agent drove only the Logcat capture, the adb reads
  and the TalkBack toggle. Nothing was pressed by adb, and nothing was uninstalled.
- **Where the verdicts are:** each case's verdict, Federico's words and the deciding log lines sit
  beside that case's criteria (section M, *The hands-on pass*). His words are quoted in Italian, as he
  wrote them, with only his typing slips corrected.
- **Where the captures are:** outside the repository, in the round's tools folder.
  - Logcat, the plan's seven tags: 253 lines.
  - The crash buffer: 0 lines.
  - `CHAN_DROPDOWN`, outside the plan's tags: 0 lines.
  - A watcher of the focused package and the process id: 33 changes.
  - The journal extract from the session mark: 89 kept lines.
- **Phase 1, read from the phone at 15:29:50:**
  - `accelerometer_rotation` 0, `user_rotation` 0;
  - `accessibility_enabled` 0, `enabled_accessibility_services` null, `touch_exploration_enabled` 0,
    with TalkBack 17.1.0.1003738110 installed;
  - battery saver 0; USB power, status 5, level 100; 27.1 °C; thermal status 0;
  - 90.0 Hz, read with the launcher in front;
  - `screen_off_timeout` 1,800,000; `stay_on_while_plugged_in` 7; `show_touches` 1.
  - **The build:** `base.apk` sha256 `ba1f88e3347382f76e4d5d1c0661602e2a24e187fcbdcfc2440e64ea88f16e4a`,
    5.5.1-dev (8).
  - **The pages,** compared in-process with no name printed: A stayed on one channel; B was switched,
    its last channel different from A's.

| Case | Verdict | One line of evidence |
|---|---|---|
| 1 | PASS | three recycled rows: title, copy and reply named their row; `message actions opened` ×3 |
| 3 | PASS (neighbour page NOT REACHED) | two on-screen rows hidden and back in place; `hidden-user rule updated added=true` |
| 4 | PASS (opacity UNCLEAR) | confirmed (`outgoing message confirmed` 17:01:10.354); one copy with its emote after the far trip |
| 5 | PASS | six rotations, no crash, rows in order, emotes moving; each rotation reloaded the history |
| 6 | PASS (count UNCLEAR, tall row NOT REACHED) | follows, stays put, the jump tap returns and clears, no jump |
| 7 | PASS | emotes restart at the edge, after B and after Home; no crash |
| 8 | NOT DONE | TalkBack's tutorial took the front on 4 of 4 activations; no double tap made |
| 9 | PASS (run 2); UNCLEAR (run 1) | `outside_click` between rows, on a system line and below the last row |
| 2 | not in this pass | moved to the 5.6.0 round, BLOCKING there |

- **Nothing FAILED, and nothing observed is a 5d regression.**
  - **How "not 5d" was established:** by reading #97's diff against `origin/main-v5`, not by running
    the old build. The rotation reload, the channel dropdown and the report menu all sit in code #97
    does not touch: the report menu's functions are byte-identical, and the manifest, `MainActivity`
    and `ChannelAutoCompleteTextView.kt` are unchanged.
  - **The merge of #97 is the owner's decision,** with case 8 NOT DONE.
- **The interruption:** an incoming phone call, 16:16:50 to 16:17:21. The dialer, then the launcher,
  were in front, with the same process (25051).
  - Federico: "chiamata in corso … non ho toccato nulla".
  - Recorded, and the pass resumed after his answer, at case 9's second tap.
  - Every other change of the front was a checklist action or its setup: Home in case 7, Safety &
    Privacy in case 3, Settings and the shade for the developer option and auto-rotate, and
    TalkBack's tutorial.
  - One more happened after the look had ended: the shade at 18:09:22, "ho controllato le notifiche di
    altre app".
- **Nine reconnects, each when the app came back to the front.** Every one was `reconnect_offline` on
  both pages: 16:17:19, 16:56:59, 17:16:42, 17:33:02, 17:35:08, 17:39:12, 17:42:53, 17:49:00 and
  18:11:59.
  - A filled the gap with 8 to 34 messages each time.
  - So A's long timeline went through eight of them before case 5.
- **Recorded, not classified:**
  - **The channel dropdown** covers the top of the chat while the channel field is focused, and a
    second tap on the field closes and reopens it (case 9).
  - **A rotation reloads the history** from `first_connect`, 400 messages, on every rotation: six of
    six (case 5).
  - **`CHAT_IME` wrote no line in the whole pass,** though the keyboard opened many times.
    - Its log calls read as unconditional (`ChatFragment.kt:1883`, `ChatKeyboardLayoutController.kt:74`).
    - `log.tag.CHAT_IME` is unset, and #97 changes no line naming it.
    - The cause is not established.
  - **The keyboard on rotation:** in the channel field it closes; in the message field it stays open
    but loses the cursor.
  - **With the list scrolled up,** the row at the bottom edge goes behind the keyboard when the
    channel field takes focus.
  - **The keyboard field datapoint:** `mInputShown=true` at 18:01:00, with the keyboard open by
    Federico's report. The inventory read it as `false` earlier the same afternoon, with the keyboard
    closed and the dev app not running. One datapoint each way; this is not a qualification.
- **The unstructured look:** 18:02 to 18:08, six minutes rather than ten, by Federico's own times.
  - It ran on the reloaded timeline, 482 rows at 17:58:07, not on the long one.
  - Federico: "non ho trovato comportamenti strani o inattesi, l'app sembra funzionare bene".
  - He sent two messages on A, confirmed at 18:02:42.161 and 18:07:01.378.
  - **The drip-in look,** observed during it as a scope decision for batching: "i messaggi arrivano
    fluidi, in modo naturale".
- **The agent's own corrections, made during the pass:**
  - **It predicted from the code** that a second tap on the channel field would close the dropdown.
    The dropdown closed and reopened.
  - **It withdrew two "cross-checks against the phone's buffer"** (15:59 to 16:05). The main buffer
    is a 256 KiB ring that, by 16:17, no longer held lines two minutes old, so an absence in it shows
    nothing.
  - **It listed the case-3 toast as a criterion,** and withdrew it.
  - **It ran one extract with a later starting instant,** which labelled B's account "A". From then
    on it labelled only from the session mark.
  - **It asked one either/or question in case 5,** which a "sì" could not answer. The answer was
    recorded and not interpreted.
- **Federico's departures from the steps, recorded as he described them:**
  - case 9, run 1: he tapped in the chat when the system line could not be found;
  - case 9, run 2, tap 1: two taps where one was asked;
  - case 5, part 2: the first run opened the keyboard in the channel field, and the step was redone in
    the message field;
  - case 8: he closed the tutorial after TalkBack was already off, without being asked.
- **Phase 4, read from the phone at 18:12:39:**
  - every Phase 1 setting is back at its starting value: `accelerometer_rotation` 0,
    `user_rotation` 0, accessibility 0 / null / 0, the shortcut target empty, battery saver 0,
    `zen_mode` 0;
  - TalkBack: no process and no bound service;
  - "Disable permission monitoring" is off again, set by Federico. A same-value secure write was
    refused with `SecurityException … WRITE_SECURE_SETTINGS`, which confirms it;
  - 26.8 °C, thermal status 0, 90.0 Hz with the dev app in front;
  - the dev app was force-stopped at 18:13:07 and not uninstalled; its APK sha256 is unchanged,
    `ba1f88e3…`;
  - all captures were stopped;
  - **left set, as at the start:** `show_touches` 1 and `stay_on_while_plugged_in` 7.

**2026-11-09, after the pass: the one read that decides the merge, and three of the owner's
records.** Nothing was done on the phone.
- **The question, the owner's:** case 8's NOT DONE does not block by itself, but it leaves the
  `ClickableViewAccessibility` suppression justified by reading, through two links.
  1. The touch listener calls `performClick()`. This was already read.
  2. `performClick()` reaches `clearChannelFocusClickListener`. That holds only if an
     `OnClickListener` is set on the list.
- **The outcomes, written by the owner before the read:**
  - **if the listener is set** and `performClick()` reaches the dismissal, the reasoning holds end to
    end, case 8 was a confirmation rather than a gate, and the merge is not blocked;
  - **if it is not set,** 5d has introduced an accessibility regression, which blocks.
- **Read at #97's head; the code under `app/src/main` is identical to `00c1e78`:**
  - **The listener is set on the list itself.** `ChatFragment.kt:1775` reads
    `chatList.setOnClickListener(clearChannelFocusClickListener)`, the line before
    `attachEmptySpaceTaps(chatList)` at 1776.
    - The same listener object is also set on the root (1773), `textStatus` (1777) and `replyBar`
      (1778).
    - No line in the app sets the list's listener to null or the list to not clickable.
  - **The touch listener does not dismiss anything itself.** In `ChatListEmptySpaceTaps.kt:34-39`
    it calls `view.performClick()` on the up that ends a tap, and returns false.
  - **The dismissal is the listener's body:** `disarmComposerFocusGuard("outside_click")`, then
    `clearChannelFieldUi`, then `closeComposerKeyboard()` (`ChatFragment.kt:1767-1770`).
- **Followed through `View.performClick()` and the accessibility click.** Android 12's
  `View.java`, `android12-release`, is the phone's runtime:
  - `setOnClickListener` makes the view clickable and stores the listener (7228-7232);
  - a clickable, enabled view offers `ACTION_CLICK` on its node (10347-10348);
  - `performAccessibilityAction` goes to the view's delegate when one is set (14072-14077), and the
    platform's default delegate calls `performAccessibilityActionInternal` (29960-29961);
  - `ACTION_CLICK` on a clickable view calls `performClickInternal()` (14102-14105), which calls
    `performClick()` (7412-7418);
  - `performClick()` calls `li.mOnClickListener.onClick(this)` (7438-7441).
- **compileSdk 36's sources agree with the comment's citations:** 8083 is
  `li.mOnClickListener.onClick(this);`, and 16124-16127 handle `ACTION_CLICK`. The same chain is at
  7870-7874, 8054-8060 and 32907-32909.
- **The two libraries in between:**
  - **RecyclerView 1.3.1,** from its sources jar, overrides none of `performClick`,
    `setOnClickListener` and `setClickable`. Its delegate calls `super.performAccessibilityAction`
    first, and asks the layout manager only if that fails (`RecyclerViewAccessibilityDelegate.java`,
    64-68).
  - **androidx core 1.18.0's `AccessibilityDelegateCompat.performAccessibilityAction`** (352-371)
    first runs an `AccessibilityActionCompat` registered on the view with that id, and otherwise
    hands the action to `mOriginalDelegate` (364), the platform's default delegate (133).
  - The app registers no compat action and no accessibility delegate anywhere.
- **The comparison with `main-v5`, at `98f235a`:** the same listener, byte-identical over 22 lines,
  was set on `scrollChat` (1755) and on `chatContainer`, a `LinearLayout` (1756). There was no touch
  listener and no suppression. 5d moves the same object onto the `RecyclerView`, and the
  accessibility path there ends in the same `onClick`.
- **The verdict: the listener IS set, and `performClick()` reaches the dismissal.** By the owner's
  pre-written outcome:
  - the suppression's reasoning holds end to end, by reading;
  - case 8 was a confirmation, not a gate;
  - the merge is not blocked by it, and there is no 5d accessibility regression on this path.
  - Case 8 stays NOT DONE, queued to the 5.6.0 round.
- **The comment at the suppression is updated where it differed** from what was read
  (`ChatFragment.kt:3722-3730`).
  - **The delegate's route:** it said RecyclerView's delegate "hands every action to View's handling
    first". It now names the step in between, `AccessibilityDelegateCompat`, and its condition: a
    compat action registered with that id. It adds "a click action registered on it" to what would
    make the suppression wrong.
  - **The TalkBack check:** it said "the 5d manual plan tests this with TalkBack rather than relying
    on it". That is not what happened, so it now dates the reading and says the TalkBack check was
    not done and is queued to 5.6.0.
  - **The edit is comment only,** 7 lines changed in place, and the file keeps 4,399 lines, so no
    line number in it moves. The tested build `ba1f88e3…` is of `00c1e78`. #97's head now differs
    from it under `app/src/main` by this comment alone.
  - **Checked after the edit, in #97's worktree on `DESKTOP-BCT68L9`,** with `ANDROID_HOME` set to the
    SDK and no `local.properties`.
    - **The command:** `:app:compileDevDebugKotlin :app:compileStableDebugKotlin :app:lintDevDebug
      :app:lintStableDebug`, BUILD SUCCESSFUL in 4 min 6 s, 49 tasks executed and 20 up to date.
    - **The compile tasks ran;** they were not up to date. Their one warning is in an unrelated file,
      `PcgMostWantedSelectionReviewDialog.kt:26`.
    - **Lint, both flavours:** "no new issues", with 38 errors filtered by `lint-baseline.xml`. Each
      report holds one issue, that baseline Hint. `ClickableViewAccessibility` is 0, and the remote
      version checks are 0.
    - **Not run:** the unit tests and the assemble tasks, since the change cannot alter compiled
      code. No comparison with a `main-v5` lint reference was made.
    - **A first attempt** stopped before any task, at "SDK location not found".
- **Queued to the 5.6.0 device round,** recorded there (`docs/manual-plan-5.5.2.md` and
  `RELEASE-STATE.md`, *5.6.0 in progress*) and beside cases 6 and 8 above:
  - **(a) the row taller than the list (U2/U3), SHIPPING UNEXAMINED, not closed.** Federico sends
    one very long message and looks.
  - **(b) case 8, retried,** with TalkBack's tutorial completed or marked as seen before
    activation.
- **Recorded as done right, by the owner:**
  - **"Pre-existing" was established by reading #97's diff against `origin/main-v5`,** not by
    running the old build.
  - **"Disable permission monitoring" was verified by its consequence,** a secure-setting write
    refused with `WRITE_SECURE_SETTINGS`, not by re-reading the flag. A negative confirmed by its
    effect is stronger than one confirmed by its own value.

**2026-11-09: the merge authorised, and case 8's 5.6.0 retry cancelled, CLOSED BY READING.** These
are the owner's rulings; the read was accepted as reported.
- **The retry is cancelled, as the owner had pre-committed** if the read came back clean, and it did.
  - **Where it is removed:** the three places it had been queued, beside case 8 above,
    `docs/manual-plan-5.5.2.md` and `RELEASE-STATE.md`. Each now holds one CLOSED BY READING line.
  - **What it supersedes:** item (b) of the previous entry. That entry stays as it was written.
- **What the read closes:** if activated, the dismissal runs, by the six-step path of the previous
  entry.
- **What it does not close:** whether TalkBack can place its focus on the empty area at all.
- **Why that leaves no question a user faces, as REASONING, not proof:** a screen reader moves
  between elements and empty space is not one, so the gesture is probably not a screen-reader
  user's, who would dismiss with Back.
- **The comment at the suppression is kept as corrected,** including that the check was not done.
- **CI on `4058aa8`:** run 42668071340, `pull_request`, success on every step: check-out, JDK 17,
  Gradle, the non-production Firebase configuration, unit tests, Android lint and the debug APKs.
  - It is superseded: this record is a commit on #97 after it.
  - **The owner's choice, when asked:** commit this record on #97 and gate the merge on the head
    that carries it ("STEP 1 on #97, gate new head"). That is the same rule as #95, #98 and #99,
    green on exactly the SHA merged, applied to the SHA actually merged.

**2026-11-10: #97 merged to the gate's standard, and the suppression comment corrected.** These are
the owner's rulings. This entry is recorded in #101.
- **#97 merged as `5238077`, on 2026-11-09 at 18:10Z, from its head `a193a50`,** by the gate every
  merge of this round has used:
  - **CI green on exactly `a193a50`:** run 42669823103, `pull_request`, success on every step.
  - **Completed, not just the first "success":** for a few minutes the run read success while its
    job and the pull request's check still read in progress. The merge waited until all three read
    completed (18:03:32Z).
  - **The head re-checked immediately before merging:** still `a193a50`, with that green run on it,
    and `main-v5` (`98f235a`) its ancestor.
  - **The squash pinned to that head,** with `--match-head-commit`.
  - **The merged tree verified byte-identical** to the tested head's: both are `15298f60…`.
  - **The branch kept,** so `feature/5d-recyclerview-timeline` still reaches `00c1e78`, the build the
    round tested. The push run the merge triggered on `main-v5`, 42671588354, is green as well.
- **The comment at the suppression is corrected again.** This supersedes "kept as corrected" in the
  previous entry, which stays as it was written.
  - **Why:** its last line said the TalkBack check was "not done, queued to 5.6.0". The retry is
    cancelled, so "queued" had become false.
  - **Now:** "TalkBack check NOT DONE, CLOSED BY READING: the 5d plan, case 8". It points at case 8's
    CLOSED BY READING line in *The hands-on pass*, which holds the reasoning.
  - **One line, changed in place, comment only.** The file keeps 4,399 lines, so no line number cited
    from it moves.

**2026-11-10: whether U2/U3 can be reached, read; still SHIPPING UNEXAMINED, with the reason
narrowed.** These are the owner's rulings. Everything here was read in the code and the platform's
sources, not run: the phone was not connected.
- **What the reading established:**
  - **The app sets no length limit on a message it receives or shows.** `readLine` has no bound
    (`TwitchChatClient.kt:109`), the parser keeps all of the message after its colon
    (`TwitchIrcProtocolParser.kt:254`), the history keeps its text whole (`BackendHistoryClient.kt:69`),
    the row is built from all of it (`ChatRowText.kt:168-169`), and no row has `maxLines` or
    `ellipsize`. Twitch documents 500 characters for its Send Chat Message API's request body. The
    three pages read (the IRC guide, the API reference, the send and receive guide) state no limit for
    a message delivered over IRC.
  - **An emote is sized from the text, not from the line, and it makes its line taller.** Its size is
    `(textSizePx * 1.5f).toInt()` (`ChatFragment.kt:3743-3744`). It is set as its bounds and placed
    `ALIGN_BOTTOM` (`TwitchEmoteImageLoader.kt:380`, `:384`), which claims the whole emote above the
    baseline: `fm.ascent = -rect.bottom; fm.descent = 0;` (`DynamicDrawableSpan.java:147-152`,
    android-36). At the phone's font scale it is 57 px, against text of 38.0 px, as measured in 5c
    (`docs/manual-plan-5c-emotes.md`, A18's table).
  - **The list has no fixed height.** It takes what the window leaves, and that reaches 0 px in
    landscape with the stream player on (*Backlog*, the candidate defect).
- **Why that does not examine U2/U3.** A 0 px list shows nothing, so whether a tall row shows its top
  or its bottom means nothing there. The case that matters is a visible list shorter than a real row.
- **What is actually unknown, and nothing else:**
  - **The list's real height in the tight configurations.** Portrait with the stream and the picker
    open, landscape with both closed, landscape with the picker open, and each of these with the
    keyboard up. Only upper bounds exist, from the layout: 614 px, 583 px and 10 px, before system
    bars, the status text and the keyboard. No dump with the list's bounds was ever saved.
  - **The text's line metrics at the phone's font scale:** a line's height, and how many characters
    it holds. The emote's size is measured; these are not.
  - **Not among them: a growing composer.** The composer is single-line, `singleLine="true"`
    (`fragment_chat.xml:207`), which sets one line and horizontal scrolling (`TextView.java:12405-12411`,
    android-36). No code changes it. So a long message scrolls inside the composer and does not shorten
    the list. This is by reading; the phone has not shown it.
- **The method changes.** Nothing is sent to any channel, so the earlier "Federico sends one very long
  message" is withdrawn.
  - Once the two quantities are measured, the tall row is built in the dev harness or an instrumented
    test. Its height is chosen against a measured list height, not the harness's fixed 160 dp, and the
    check is the row's bounds against the list's.
  - Federico's only part is to look at one screenshot and say whether showing the top is acceptable.
    That decision is his.
- **On hold until the phone is connected:** the measurement step, and after it the harness row.

**2026-11-10, the device step: the list measured, two layouts with no chat, a crash, and U2/U3
subsumed.** These are the owner's rulings. The step ran over adb on the phone of *Test device*, with
the dev build `00c1e78`, on page A, and no message was sent. It supersedes the open points of the
entry above, which stays as written.
- **How it was read.**
  - Frames come from `dumpsys activity top`, which prints no view's text. uiautomator's bounds were
    used only to find controls by resource id and tap them.
  - **uiautomator clips bounds on this phone.** In portrait they stop at y=2182, 113 px above the
    page's real bottom at y=2295. In landscape they stop at x=2163, 113 px short of the real right
    edge at x=2276. Its numbers are not measurements here.
  - `settings put system user_rotation` was refused, because the shell is not granted
    `WRITE_SETTINGS`. Rotation went through `cmd window user-rotation lock <n>` instead.
- **The list's height, measured, against the bounds of the reading:**

  | Configuration | Bound | Measured |
  |---|---|---|
  | Portrait, both panels closed | ≤ 1903 px | 1604 px (672 dp) |
  | Portrait, both closed, keyboard up | not computed | 897 px (376 dp) |
  | Portrait, stream on | ≤ 1187 px | 773 px (324 dp) |
  | Portrait, stream and picker open | ≤ 614 px | 200 px (84 dp) |
  | Portrait, stream on, keyboard up from the composer, which closes the picker | not computed | 66 px (28 dp) |
  | Portrait, stream and picker open, keyboard up from the picker's search field | not computed | not measured: the crash came first |
  | Landscape, both closed | ≤ 583 px | 389 px (163 dp) |
  | Landscape, both closed, keyboard up | not computed | 389 px, with a floating keyboard over the list's right side |
  | Landscape, picker open, with or without the keyboard | ≤ 10 px | 0 px, with the composer below the screen |
  | Landscape, stream on | 0 px | 0 px, below the window, with the composer off screen |

  - In portrait the page spans y=113 to y=2295: the status bar is 113 px and the navigation bar 105
    px. In landscape it spans y=113 to y=1080.
  - In portrait the keyboard covers y=1588 to y=2400. In landscape it floats: its touchable region is
    an 813 by 701 px panel, and its inset is zero, so it covers the list instead of shrinking it.
- **Recorded as done right, by the owner: the bounds reconciled to the digit.** The bound of 614 px
  for portrait with the stream and the picker open came out at 200 px. Four omissions account for
  exactly the difference:
  - the system bars, 113 px of status bar and 105 px of navigation bar: 218;
  - the status line's text: 53, since it measured 110 px against the 24 dp, 57 px, of padding the
    bound counted;
  - the mute button the player shows: 115;
  - the Send button, which makes the composer row 162 px rather than its 56 dp, 134 px: 28.
  - 614 − 218 − 53 − 115 − 28 = 200. An arithmetic error explained to the digit is worth more than a
    corrected number.
- **The line metrics, measured from the 28 rows on screen in portrait:**
  - A one-line row is 52 px, and each further line adds 45 px. The rows were 52 px (23 of them), 97
    (2), 142 (1) and 68 (1). The line counts are inferred from the 45 px step, not read from the
    text.
  - A single line holding an emote is 68 px: the 57 px emote above the baseline, and about 11 px of
    the text's below it.
  - Rows are 1022 px wide in portrait. Characters per line were not measured, because they depend on
    text that was not read.
- **The composer does not grow, so that contributor is closed.** It is `singleLine="true"`. With 300
  characters typed it stayed 115 px tall, and the list stayed 897 px. The characters were deleted
  before the next step.
- **Observed rather than reasoned: a rotation turns the player off.** The player was on in portrait,
  and after the rotation to landscape the page came back with it gone.
- **Two layouts with no chat and no composer, and a crash on rotating back:** all three are in the
  *Backlog*, with their numbers.
- **U2/U3 is SUBSUMED by the layout defect, and the harness row is CANCELLED** (*Backlog*). The
  earlier "what is actually unknown" is answered by the table above, and the earlier method is
  withdrawn with the harness row.
- **The phone's end state:**
  - **As at the start:** rotation `lock 0`, `accelerometer_rotation` 0, `user_rotation` 0, font scale
    1.15, the launcher in front, the keyboard hidden. The player and the picker are off, because the
    crash ended the page.
  - **Different from the start:**
    - The dev app's process is running. It came back on its own after the crash; nothing launched it.
      At the start it was not running, since the step's launch was cold.
    - The crash's entry remains in the phone's crash log.

**2026-11-10, the owner's rulings on the device step: one defect with instances, his own error
corrected, the crash's guard a decided exception, and the options restated.** This entry supersedes
the parts of the entry above that it names. That entry stays as written.
- **One defect, with instances.** The two landscape layouts and the tight portrait ones are instances
  of a single defect: the chat list has no minimum height (*Backlog*). They are recorded as one entry
  so that someone fixing one instance does not find the others survive.
- **Recorded as the owner's error, by his ruling.** He wrote that fixing the landscape defect would
  remove U2/U3 from normal use. That is false.
  - The 200 px and 66 px measurements are in portrait.
  - So locking portrait, which he presented as the cheapest option closing both defects, would leave
    those configurations untouched, and U2/U3 routine in them.
  - He conflated two different things because they arrived in the same report.
  - The agent's report had attributed that reasoning to him rather than absorbing it. U2/U3 is now
    recorded as subsumed by the root, with the reasoning corrected beside it.
- **The crash: PRE-EXISTING, confirmed.** The guard is authorised as a DECIDED EXCEPTION, and its test
  is deferred with its cost written (*Backlog*). Neither is written yet: they wait on Federico's
  landscape decision.
- **The options restated, replacing the earlier framing.** Locking portrait closes two instances; a
  minimum height on the list closes the problem (*Backlog*).
- **The configuration not measured:** portrait, with the stream and the picker open and the keyboard
  raised from the picker's search field. The crash pre-empted it.
  - **Predicted from its measured parts, not measured.** With the keyboard up, the page measured
    1475 px. With the stream and the picker open, the other children measured 153 + 134 + 110 + 134
    + 716 + 573 + 162 = 1982 px. That would leave the list 0 px and the composer off screen, in
    portrait.
  - This assumes the picker stays open while its own search field has focus. That was observed in
    landscape, not in portrait.
  - **Recommendation: measure it.** It would be the first 0 px instance in portrait, which changes
    what locking portrait closes, and it takes one tap on the phone.

**2026-11-10, the configuration space enumerated, its gaps measured, and the instance list CLOSED.**
These are the owner's rulings. The step ran on the phone over adb, from 16:48 to 17:01, with the dev
build `00c1e78`, on page A. No message was sent. This entry supersedes the unmeasured configuration
of the entry above: it is measured here, and the prediction held to the pixel.
- **Recorded as the owner's METHOD error, by his ruling.** He put a decision in front of Federico
  while the instance list was still growing, twice.
  - The first time, portrait was conflated with landscape.
  - The second time, portrait was taken to have no 0 px instance, before the configuration that has
    one was measured.
  - A decision about a family of instances is taken when the family is CLOSED, not when it is the
    longest list seen so far.
- **The dimensions, and the total.**
  - Orientation: portrait or landscape.
  - The stream player: off or on.
  - The emote picker: closed or open.
  - The reply bar: hidden or shown. It is a third panel in the same `LinearLayout`
    (`fragment_chat.xml:159-183`), which the owner's list did not name.
  - The keyboard: down, or up from one of the page's three text fields: the composer, the channel
    field or the picker's search field.
  - 2 × 2 × 2 × 2 × 4 = **64 configurations.**
- **The constraints, each with its source:**
  - **The search field's keyboard needs the picker open.** The field is inside the picker, and closing
    the picker clears its focus (`TwitchEmotePickerController.kt:135`). By reading.
  - **The picker and the composer's keyboard:**
    - Touching the composer closes the picker (`ChatFragment.kt:1586-1591`). Observed in portrait.
    - Opening the picker hides the keyboard and clears the composer's focus (`:1552-1560`). By
      reading.
    - But a reply raises the composer's keyboard without closing the picker (`beginReply`,
      `:3452-3467`). Observed in portrait. The keyboard also survives the reply's cancellation, which
      was observed too.
    - So the combination is reachable in portrait. In landscape an open picker leaves the list at
      0 px (observed), so there is no row to swipe, and the combination is impossible.
  - **The channel field's keyboard leaves the picker open.** No code closes it (by reading), and it
    was observed in portrait.
  - **In portrait, every field's keyboard gives the same page, 1475 px.** Observed for all three: the
    composer, the search field and the channel field.
  - **In landscape, every field's keyboard floats and reserves no space.** Observed for all three: the
    composer, the search field and the channel field, each with the same 813 by 701 px panel. So in
    landscape every keyboard state has the list height of the keyboard-down state.
  - **The reply bar is 135 px,** in both orientations (observed). It can be started wherever rows are
    visible, and it stays while the panels change.
  - **Additivity:** in each of the 24 classes below, the list measured exactly the page's height less
    the measured heights of the other children, clamped at 0.
- **The count:**
  - **Impossible: 12.** The search field's keyboard with the picker closed accounts for 8; the
    composer's keyboard with the picker open, in landscape, for 4.
  - **Reachable: 52.**
  - **Distinct list heights: 24.** In landscape the keyboard states collapse, leaving stream × picker
    × reply bar, 8. In portrait they collapse to down or up, 16.
  - **Measured: all 24.** 8 were measured in the device step, and 16 in this one.

  | Orientation | Stream | Picker | Reply bar | Keyboard | List | Composer | Measured in |
  |---|---|---|---|---|---|---|---|
  | Portrait | off | closed | hidden | down | 1604 px | on | the device step, rechecked here |
  | Portrait | off | closed | hidden | up | 897 px | on | the device step |
  | Portrait | off | closed | shown | down | 1469 px | on | this step |
  | Portrait | off | closed | shown | up | 762 px | on | this step |
  | Portrait | off | open | hidden | down | 1031 px | on | this step |
  | Portrait | off | open | hidden | up: search field, channel field | 324 px | on | this step |
  | Portrait | off | open | shown | down | 896 px | on | this step |
  | Portrait | off | open | shown | up: composer, by a reply | 189 px | on | this step |
  | Portrait | on | closed | hidden | down | 773 px | on | the device step, rechecked here |
  | Portrait | on | closed | hidden | up | 66 px | on | the device step |
  | Portrait | on | closed | shown | down | 638 px | on | this step |
  | Portrait | on | closed | shown | up: composer, by a reply | **0 px** | **partly under the keyboard** | this step |
  | Portrait | on | open | hidden | down | 200 px | on | the device step, rechecked here |
  | Portrait | on | open | hidden | up: search field; composer, by a cancelled reply | **0 px** | **off screen** | this step |
  | Portrait | on | open | shown | down | 65 px | on | this step |
  | Portrait | on | open | shown | up: search field; composer, by a reply | **0 px** | **off screen** | this step |
  | Landscape | off | closed | hidden | any | 389 px | on | the device step, rechecked here |
  | Landscape | off | closed | shown | any | 254 px | on | this step |
  | Landscape | off | open | hidden | any but the composer's | **0 px** | **off screen** | the device step, rechecked here |
  | Landscape | off | open | shown | any but the composer's | **0 px** | **off screen** | this step |
  | Landscape | on | closed | hidden | any | **0 px** | **off screen** | the device step |
  | Landscape | on | closed | shown | any | **0 px** | **off screen** | this step |
  | Landscape | on | open | hidden | any but the composer's | **0 px** | **off screen** | this step |
  | Landscape | on | open | shown | any but the composer's | **0 px** | **off screen** | this step |

- **Every instance that gives 0 px or pushes the composer off screen, by orientation:**
  - **Landscape, 6 of 8 classes:** every class with the picker open or the stream on.
  - **Portrait, 3 of 16 classes:** the stream on and the keyboard up, together with the picker, the
    reply bar, or both.
  - The *Backlog*'s entry lists them with their numbers.
- **The list is DECLARED CLOSED,** for this space: this phone, at this font scale and density, with
  these panels, bars and keyboard, on the layout of `00c1e78`, which `main-v5` shares.
- **What would reopen it:**
  - **A new panel, or a changed height** of any child of `fragment_chat.xml`'s `LinearLayout`: the
    player's 300 dp, the picker's 240 dp, the reply bar, a bar, the composer row.
  - **The status line wrapping to a second line.**
  - **A different font scale, display size or density,** or a device of another size.
  - **A keyboard that docks in landscape, or a keyboard of another height.**
  - **A window of another size:** split screen or a pop-up window, which this space did not
    enumerate.
  - **The reversed orientations:** these were reasoned to have the same dimensions, with the insets
    on the other side, and were not measured.
  - **The system navigation mode,** which sets the navigation bar's height.
- **Not put to Federico again until now.** The owner told him not to answer yet. With the list
  closed, putting the decision to him again is the owner's step, from the *Backlog*'s framing.
- **Also observed, not diagnosed:** Back, pressed with the picker open on page A and no keyboard up,
  moved the pager to the accounts page and left the picker open. It did not close the picker. Page
  A's picker was then closed with its own close button.
- **How it was read, and one reading discarded.**
  - The stable app had been running since 16:21:46, started by someone else and not by this step,
    and `dumpsys activity top` lists its view tree first.
  - One reading, taken while the dev app showed its accounts page, came from the stable app's tree,
    and was discarded.
  - The readers were then restricted to the dev app's tree. Every number above comes from the dev
    app; 200 px and 773 px were re-read after the fix and came out the same.
- **The crash did not recur** (*Backlog*): one rotation each way, with a different sequence.
- **The phone's end state:**
  - **As at the start of this step:** rotation `lock 0`, `accelerometer_rotation` 0, `user_rotation`
    0, font scale 1.15, the launcher in front, and the keyboard down. Page A has the stream off, the
    picker closed and the reply cancelled.
  - **Different from the start of this step:**
    - The dev app's process is running, from this step's cold launch at 16:48:51. At the start it
      was not running: the process that had come back after the crash had already ended.
    - The stable app's process, from 16:21:46, is not this step's, and was not touched.
  - **No new crash entry** since 15:37.

**2026-11-10: the decision, the layout's thresholds, and the simulation blocked.** These are the
owner's rulings. The decision, a minimum height on the list, is in the *Backlog*, with Federico's
reasoning and the principle.
- **The layout's thresholds, by panel combination.** The first column is the page height below which
  the list measures 0 px. The next two are for the composer's text field, the part a user types in:
  below the second it is partly off screen, and below the third entirely.
  - **Source:** they come from the children measured on this phone, at 2.3875 px/dp. The bars take
    578 px, 242.1 dp. The stream adds 831 px, 348.1 dp: the player's 300 dp and the mute button's
    48.2 dp. The picker adds 240 dp, the reply bar 56.5 dp, and the composer row is 67.9 dp of the
    bars. The text field starts 9.6 dp below the row's top, and ends 57.8 dp below it.
  - **What "page height" means:** the display's height less the system bars and the keyboard. The
    thresholds are a property of the layout, not of this phone.
  - **This phone's margins** are given beside them, in dp of page height. Its display is 452 by 1005 dp
    at 2.3875 px/dp. Its page is 914 dp in portrait (2182 px; the phone's own configuration reports
    `h913dp`), 618 dp in portrait with the keyboard up (1475 px), and 405 dp in landscape (967 px).

  | Panels open | List 0 px below | Field partly off below | Field entirely off below | Portrait, 914 | Portrait with keyboard, 618 | Landscape, 405 |
  |---|---|---|---|---|---|---|
  | none | 242 dp | 232 dp | 184 dp | +672 | +376 | +163 |
  | reply bar | 299 dp | 289 dp | 240 dp | +615 | +319 | +106 |
  | picker | 482 dp | 472 dp | 424 dp | +432 | +136 | **−77** |
  | picker, reply bar | 539 dp | 529 dp | 480 dp | +375 | +79 | **−134** |
  | stream | 590 dp | 580 dp | 532 dp | +324 | +28 | **−185** |
  | stream, reply bar | 647 dp | 637 dp | 588 dp | +267 | **−29** | **−242** |
  | stream, picker | 830 dp | 820 dp | 772 dp | +84 | **−212** | **−425** |
  | stream, picker, reply bar | 887 dp | 877 dp | 828 dp | +27 | **−269** | **−482** |

  - **Corrected on 2026-11-10, during the simulation below.** The table first had a single composer
    column, giving the composer ROW's edge: 174 to 819 dp. That is not what a user sees leave the
    screen. At 450 dpi the page was 769.1 dp, between the row's edge (762) and the field's (772), and
    the field measured entirely off, as its own column says.

  - **As statements about the layout:**
    - With the picker open, the chat disappears on any device with less than 482 dp of page height.
    - With the stream player on, the threshold is 590 dp.
    - With the player and the picker, it is 830 dp, and 887 dp with the reply bar as well.
  - **Every negative margin above is a configuration measured at 0 px on this phone,** and every
    positive one measured exactly that many dp of list. The text field's state matched the field
    columns each time.
  - **The thresholds are for font scale 1.15.** The status line's text, 22 dp of the bars' 242, grows
    with the font scale, and so would any button whose text outgrows its 48 dp minimum. Everything
    else is in dp.
- **The owner's claim about 1005 dp: checked against what is readable, and only partly
  established.**
  - **The claim** was that 1005 dp of portrait height is above average, because dp tracks physical
    size, and many phones have a higher density at the same physical size, so the defect would be
    worse on them.
  - **What is readable: the SDK's device catalogue.** Android Studio's `sdklib.jar` holds
    `com/android/sdklib/devices/devices.xml` and `nexus.xml`. Their phones' portrait display heights,
    in dp:
    - Small Phone 640;
    - Pixel 4 829;
    - Pixel 4a and 5 851;
    - Pixel 4 XL 869;
    - Pixel 6 Pro and 7 Pro 891;
    - Medium Phone, and Pixel 6, 6a, 7, 7a, 8 and 8a 914;
    - Pixel 9, 9a, 10 and 10a 923;
    - Pixel 9 Pro and 10 Pro 952;
    - Pixel 8 Pro, 9 Pro XL and 10 Pro XL 997.
    - The SDK's emulator skins give pixel sizes only, and no system image is installed.
  - **Established, for the catalogue only:** at 1005 dp this phone is taller than every phone in the
    catalogue, whose tallest is 997 dp. The catalogue is Google's reference devices, not a sample of
    phones in use. So "above average" for phones people actually have is **the owner's unverified
    claim.**
  - **"dp tracks physical size": not as stated.** dp tracks the configured density, which the
    manufacturer sets and the user can override. This phone shows it on one screen: its physical
    density is 450, which gives 384 by 853 dp, and the override of 382 that its user chose gives 452
    by 1005 dp.
  - **"Worse on them": it follows from the thresholds** for any device with less page height. That
    includes this phone at its own default display size, where the display is 853 dp. This is
    computed and not measured, since the simulation below was blocked.
  - **A conditional, not a measurement:** on this phone the system bars take 91 dp, so the
    stream-and-picker threshold of 830 dp of page corresponds to 921 dp of display. If other phones'
    bars took the same, only the catalogue's phones of 923 dp or more would clear it. The catalogue
    gives no bar heights.
- **The simulation is BLOCKED on this phone, and nothing was changed.**
  - `wm density 382`, the current value, was refused with
    `SecurityException: Must hold permission android.permission.WRITE_SECURE_SETTINGS`.
  - `wm size 1080x2400`, the physical size, was refused as well.
  - Read afterwards: `Physical size: 1080x2400`, `Physical density: 450`, `Override density: 382`. No
    override was created, and none was lost.
  - This is the restriction the round recorded: "Disable permission monitoring" is off, set by
    Federico. The refused writes confirm it, by the same effect the round used.
  - **What running it takes:** Federico switches "Disable permission monitoring" on for the step, and
    off again after it, confirmed by a refused same-value write.
- **The restore must not be `wm density reset`.** This phone starts with a user override, density 382
  over a physical 450, and `reset` would leave it at 450. The starting state is restored with
  `wm density 382` and `wm size reset`, and then read back from the phone.

**2026-11-10, the thresholds verified on simulated geometries: the model holds.** These are the
owner's rulings. Federico switched "Disable permission monitoring" on for this step. The step ran over
adb from 17:56 to 18:10, with the dev build `00c1e78`, on page A, and no message was sent. It
supersedes the "blocked" and "computed and not measured" points of the entry above.
- **The geometries,** set with `wm size` and `wm density`, each modelled on a device in the SDK's
  catalogue:

  | Geometry | Size, density | Display | Page measured | Bars measured |
  |---|---|---|---|---|
  | This phone as set, the reference | physical, 382 | 452 × 1005 dp | 913.9; 617.8 with keyboard; 405.0 landscape | 242.1 dp |
  | This phone at its default display size | physical, 450 | 384 × 853 dp | 769.1; 514.1 with keyboard | 243.2 dp |
  | Compact phone, the catalogue's Small Phone | 720x1280, 320 | 360 × 640 dp | 539.5; 303.5 landscape | 242.5 dp |
  | Typical phone, Pixel 6 to 8 | physical, 420 | 411 × 914 dp | 827.0; 535.6 with keyboard | 243.0 dp |
  | Large phone, Pixel 9 Pro XL | 1344x2992, 480 | 448 × 997 dp | 915.7; 630.3 with keyboard | 242.4 dp |
  | Tablet, the catalogue's Medium Tablet | 1600x2560, 320 | 800 × 1280 dp | 1179.5; 889.5 with keyboard; 699.5 landscape | 242.5 dp |

- **Every reading, predicted from the thresholds and measured,** in dp. "Field" is the composer's text
  field.

  | Geometry | Configuration | Page | List, predicted | List, measured | Field, predicted | Field, measured |
  |---|---|---|---|---|---|---|
  | Default size, 450 | none | 769.1 | 527.0 | 525.9 | on | on |
  | Default size, 450 | picker | 769.1 | 287.0 | 285.9 | on | on |
  | Default size, 450 | stream | 769.1 | 178.9 | 177.8 | on | on |
  | Default size, 450 | stream, picker | 769.1 | **0** | **0** | **off** | **off** |
  | Default size, 450 | stream, picker, keyboard | 514.1 | **0** | **0** | **off** | **off** |
  | Default size, 450 | stream, keyboard | 514.1 | **0** | **0** | **off** | **off** |
  | Compact | none | 539.5 | 297.4 | 297.0 | on | on |
  | Compact | picker | 539.5 | 57.4 | 57.0 | on | on |
  | Compact | stream | 539.5 | **0** | **0** | **partly** | **partly** |
  | Compact | stream, picker | 539.5 | **0** | **0** | **off** | **off** |
  | Compact, landscape | none | 303.5 | 61.4 | 61.0 | on | on |
  | Compact, landscape | picker | 303.5 | **0** | **0** | **off** | **off** |
  | Compact, landscape | stream, picker | 303.5 | **0** | **0** | **off** | **off** |
  | Typical | none | 827.0 | 584.9 | 584.0 | on | on |
  | Typical | picker | 827.0 | 344.9 | 344.0 | on | on |
  | Typical | stream | 827.0 | 236.8 | 235.8 | on | on |
  | Typical | stream, picker | 827.0 | **0** | **0** | on | on |
  | Typical | stream, picker, keyboard | 535.6 | **0** | **0** | **off** | **off** |
  | Typical | stream, keyboard | 535.6 | **0** | **0** | **partly** | **partly** |
  | Large | none | 915.7 | 673.6 | 673.3 | on | on |
  | Large | picker | 915.7 | 433.6 | 433.3 | on | on |
  | Large | stream | 915.7 | 325.5 | 325.3 | on | on |
  | Large | stream, picker | 915.7 | 85.5 | 85.3 | on | on |
  | Large | stream, picker, keyboard | 630.3 | **0** | **0** | **off** | **off** |
  | Large | stream, keyboard | 630.3 | 40.1 | 40.0 | on | on |
  | Tablet | none | 1179.5 | 937.4 | 937.0 | on | on |
  | Tablet | picker | 1179.5 | 697.4 | 697.0 | on | on |
  | Tablet | stream | 1179.5 | 589.3 | 589.0 | on | on |
  | Tablet | stream, picker | 1179.5 | 349.3 | 349.0 | on | on |
  | Tablet | none, keyboard | 889.5 | 647.4 | 647.0 | on | on |
  | Tablet, landscape | none | 699.5 | 457.4 | 457.0 | on | on |
  | Tablet, landscape | picker | 699.5 | 217.4 | 217.0 | on | on |
  | Tablet, landscape | stream | 699.5 | 109.3 | 109.0 | on | on |
  | Tablet, landscape | stream, picker | 699.5 | **0** | **0** | **off** | **off** |

- **Where measurement and arithmetic disagree: nowhere beyond 1.1 dp.**
  - Every list measured 0.1 to 1.1 dp shorter than predicted. The bars measured 242.4 to 243.2 dp
    against the reference's 242.1, from each view's dp being rounded to whole pixels at that density.
  - Every 0 px and every field state was as predicted. That includes four readings inside the narrow
    bands where a small error would have shown:
    - the typical phone with stream and picker, at 827.0 dp, between the field's 820 and the list's
      830: chat gone, field on screen;
    - this phone at 450 with stream and picker, at 769.1 dp, below the field's 772: field off;
    - the compact phone with the stream, at 539.5 dp, between 532 and 580: field partly off;
    - the typical phone with the stream and the keyboard, at 535.6 dp, between 532 and 580: field
      partly off.
  - **No simulated geometry reproduced the defect where the arithmetic said it should not occur, and
    none spared one where it said it should.** The threshold model is not shown wrong.
- **What the numbers say, by geometry, with the keyboard down:**
  - **Portrait:** the chat disappears with the stream alone on the compact phone, and with stream and
    picker on the compact phone, on this phone at its default size and on the typical phone. It
    survives on the large phone, on this phone as set and on the tablet.
  - **Landscape:** on the compact phone with the picker alone. On the tablet, a 10-inch device, with
    stream and picker.
- **With the keyboard up, in portrait:** stream alone leaves no chat on this phone at its default
  size and on the typical phone, and 40 dp on the large phone. On this phone as set it leaves 66 px,
  27.6 dp.
- **The owner's claim, revisited:** "worse on them" is now MEASURED for this phone at its default
  display size and for the typical phone class. "Above average" stays unverified, as recorded above.
- **Not read, with the reason:**
  - **The reply bar in the simulated geometries:** it is 56.5 dp and additive on the reference.
  - **The compact phone's keyboard configurations.** The search field was at the page's edge and
    raised no keyboard. The composer's centre fell under the navigation bar: with the stream on,
    16 px of it were on screen, so it could not be tapped.
  - **The compact phone's stream alone in landscape:** a tap on the picker's close button missed, so
    that reading had both panels open. It is implied by the clamp: a shorter page than the
    reference's 405 dp, where it was 0 px.
- **How it was read.** uiautomator stopped answering at times on the tablet, while animated emotes
  ran in the picker, and two taps missed. Those readings were relabelled by the visibility they
  recorded, and the rest of the tablet was driven by taps placed from `dumpsys` frames.
- **The restore, with values read from the phone before and after:**
  - **Before, at 17:56:41:** `Physical size: 1080x2400` with no override, and `display_size_forced`
    `null`; density 450 physical, 382 override (`display_density_forced` 382); rotation `lock 0`,
    `accelerometer_rotation` 0, font scale 1.15. Settings on Developer options was in front.
  - **The restore:** `cmd window user-rotation lock 0`, `wm size reset` and `wm density 382`.
    - `wm size reset` left `display_size_forced` as an empty key, where it had been absent.
      `settings delete global display_size_forced` returned it to `null`.
    - The size and density changes had closed the Developer options screen, which was reopened with
      `android.settings.APPLICATION_DEVELOPMENT_SETTINGS`.
  - **After, at 18:09 to 18:11:** every value above is identical, with Developer options in front. The
    reference reading is identical too: page 913.9 dp, list 671.8 dp.
  - **No crash in any of the changes:** five geometries, four rotations. The dev app kept pid 22577
    throughout, and no crash entry was written after 17:50.
  - **"Disable permission monitoring" restored and confirmed, at 18:20.**
    - Federico switched it off, and at 18:17 a same-value write (`wm density 382`) was still accepted,
      with `remote exit=0`.
    - **On this phone the toggle takes effect only after a restart.** Federico rebooted it, and at
      18:20, 94 s after boot, the same write was refused with
      `SecurityException: Must hold permission android.permission.WRITE_SECURE_SETTINGS`.
    - Every value read back is still as it was before the step. The reboot ended the processes that
      had been running, the dev app's among them.

**2026-11-11: the layout fix, a DESIGN PROPOSAL checked against the measured tables; no code.** These
are the owner's rulings: Ruling 3, its addendum on the top of the page, and the update that replaced
the addendum's point (b). Read on `main-v5` at `44e22da`, on `nbpromec1`. The numbers are computed
with the plan's threshold model. Nothing ran on the phone. Nothing here chooses a floor, a variant
or a label: those are Federico's.
- **Severity, recorded in the owner's words.** Federico's phone MASKS the defect, because its display
  size is set smaller than default (density 382 against 450). At the default size, and on the typical
  phone class, portrait with the stream on and the keyboard up leaves NO chat and the composer's text
  field off or partly off screen — the main use of the player on a typical phone. Pre-existing,
  present in 5.5.1.
- **The principles, fixed by the owner.** The proposal satisfies them, or says exactly where it cannot.
  1. HARD INVARIANT: the composer's text field is always entirely on screen.
  2. The list keeps a FLOOR wherever the page has room for the bars plus the floor.
  3. YIELD ORDER: transient before persistent. The emote picker shrinks first and the stream player
     second, each to its own minimum useful height, before anything is refused.
  4. Refusing to open a panel is the last resort, and it is NEVER SILENT: the user is told why.
  5. The floor is expressed in ROWS at the user's font scale, not as a round dp number. Its count is a
     taste decision, and it is Federico's.
- **How the model was checked before it was used.** It is the *Backlog*'s clamp, with the children
  measured on 2026-11-10: bars 578 px, 242.1 dp (channel bar 153 px, action row 134, status line 110,
  the stream controls' padding 19, composer row 162), mute button 115 px, 48.2 dp, player 300 dp,
  picker 240 dp, reply bar 56.5 dp. The text field lies 9.6 to 57.8 dp below the composer row's top.
  - Recomputed, it gives all 34 predictions of the simulation table, each list within 0.15 dp and
    every field state exact.
  - It also gives the 24 reference classes, each within 0.6 dp of the measured list.
  - The tables below use each geometry's own measured bars (242.4 to 243.2 dp). The arithmetic and the
    measurements disagreed by at most 1.1 dp on 2026-11-10, and the same tolerance holds here.

**How each height is set today, read.**
- **The stream player:** `fragment_chat.xml:127`, `layout_height="300dp"`, with width `match_parent`
  (`:126`) and `gone` by default (`:128`).
  - Code changes only its visibility: `updateStreamUi` (`ChatFragment.kt:3501`), `enableStream`
    (`:3514`) and `disableStream` (`:3522`).
  - No code sets its size.
- **The mute button:** `btnMuteStream`, a `Button` with `wrap_content` (`:116-121`), the only child of
  `streamControlsBox` (`:107-122`).
  - Its height is the Material button's 48 dp minimum: 115 px measured, 48.2 dp.
  - It is shown with the player (`ChatFragment.kt:3502`). Its text is `unmute` "Unmute" while muted,
    the default (`streamMuted = true`, `:618`), and `mute` "Mute" otherwise (`:3504-3508`).
  - **The box's `paddingBottom="8dp"` (`:114`) is paid with the stream off too.** The box stays visible
    with its only child gone. That is the 19 px, 8.0 dp, among the bars.
- **The emote picker:** `view_twitch_emote_picker.xml:6`, `layout_height="240dp"`, included at
  `fragment_chat.xml:185`.
  - Its visibility is set by `TwitchEmotePickerController.open` and `close` (`:115`, `:136`).
- **The reply bar:** `fragment_chat.xml:159-183`, `wrap_content`, with 4 dp of padding top and bottom.
  - It holds a text of at most two lines (`:176`) beside the `X` button: 135 px measured, 56.5 dp.
  - It is shown by `setPendingReply` (`ChatFragment.kt:3482`) and hidden by `clearPendingReply`
    (`:3489`).
- **The composer row:** `minHeight="56dp"` (`:190`). The Send button's 56 dp height (`:225`) and the
  row's 6 dp padding (`:193`) make it 162 px, 67.9 dp.

**The picker scrolls internally. Its smallest useful height is 152 dp, by reading.**
- **Inside the 240 dp:** the 8 dp padding (`:11`) leaves 224 dp. The search row takes 48 dp (`:23`),
  and the grid takes the rest, 176 dp less its own 4 dp top and bottom (`:55-56`): 168 dp of cells.
- **The grid** is a `RecyclerView` with weight 1 (`:45-57`) on a six-column `GridLayoutManager`
  (`TwitchEmotePickerController.kt:72-91`, `GRID_COLUMN_COUNT` `:318`).
  - A cell is 80 dp (`item_twitch_emote.xml:4`). A section header takes a full row of 36 dp
    (`item_twitch_emote_section.xml:5`).
  - So today the panel shows 2.1 rows of cells, or a header and 1.6 rows.
- **Smallest useful: one complete row of six cells under the search field,**
  8 + 48 + 4 + 80 + 4 + 8 = 152 dp. Below that, no row of emotes is whole. A header at the top sits
  above a cut row until the grid is scrolled.
  - Hiding the search row under compression would reach 104 dp. It is not proposed, because it
    removes a function.
- From the dimensions; not measured.

**The player's surface: a Twitch embed filling the view, with no aspect ratio of its own. Its
documented minimum is 400 by 300 px, so it has no room to yield.**
- **What the view loads, read:** `buildStreamUrl` (`ChatFragment.kt:3303-3306`) loads
  `https://unouidol.github.io/ircminichat/player.html?channel=…&muted=…`.
  - The page lives in the public repository `unouidol/ircminichat` and is served by GitHub Pages from
    `main`, folder `/docs`.
  - `docs/player.html` and `player.html` are byte-identical, 1767 bytes. Their last commits are
    `4c3a2b1` and `2a032fd`, both of 2026-03-30.
- **What the page contains, read:**
  - a black page, with `html`, `body` and `#player` at 100% width and height and overflow hidden;
  - `new Twitch.Player("player", { width: "100%", height: "100%", channel, parent:
    ["unouidol.github.io"], autoplay: false, muted })`, from `player.twitch.tv/js/embed/v1.js`.
  - **It sets no size and keeps no ratio.** The embed fills whatever the GeckoView is given.
- **What the embed needs, read in Twitch's documentation on 2026-11-11**
  (`dev.twitch.tv/docs/embed/video-and-clips/` and `dev.twitch.tv/docs/embed/`):
  - "Embedded video windows must be at least 400x300 pixels." The iframe attribute table gives height
    "Minimum: 300" and width "Minimum: 400".
  - "Embeds must adhere to the minimum height and width requirements outlined in their respective
    attribute tables."
  - The only consequence the pages state: "Minimum size requirements and visibility are necessary for
    autoplay to begin", with mobile devices excepted. TMC sets `autoplay: false`.
  - Embeds "should not be obscured in any way by other page elements".
  - **Nothing on aspect ratio, or on how a video is fitted into a box of another shape.**
  - `getPlaybackStats()` returns `videoResolution`, "The native resolution of the current video (eg.
    640x480)", and `displayResolution`, "The current size of the video player element".
- **The 300 dp is exactly the documented minimum height.** Whether it was chosen for that reason is
  not recorded; it is inferred from the match.
- **One CSS pixel is taken as one dp.** The page's `width=device-width` viewport is what gives that.
  NOT VERIFIED on the phone: `displayResolution` would read it.
  - **Read since, on the phone, later on 2026-11-11** (*Recorded results*, the player below Twitch's
    minimum): a CSS pixel is within 1.6% of a dp. Today's 300 dp gives the embed 298 CSS px on this
    phone as set, and 295 at its default size.
  - The stream session is built with `USER_AGENT_MODE_DESKTOP` (`GeckoSessionManager.kt:1691`). That
    sets the user agent, not the viewport mode, and is not verified either.
- **Today's width is already below the documented 400 on two geometries in portrait:** 360 dp (the
  compact phone) and 384 dp (this phone at its default size). That is today's state, not the
  proposal's.
  - **Observed since, later on 2026-11-11:** on both, the embed received 360 × 300 and 378 × 295 CSS
    px, and played, with no error and no message about its size (*Recorded results*, the player
    below Twitch's minimum).
- **Smallest useful height, by the documentation: 300 px, today's.** Below it, the embed's behaviour is
  neither documented nor observed.

**Where the proposal cannot meet the principles as stated: principle 3, for the player.**
- Within Twitch's documented minimum, the player cannot shrink at all. Its minimum is its height.
- So the yield order is: the picker shrinks from 240 to 152 dp; the player keeps its 300 dp; the next
  step is a refusal.
- **Letting the player below 300 dp departs from Twitch's documented requirement.** Its useful minimum
  below 300 would then have to be measured. That is a decision for Federico and the owner, not taken
  here, and no number below 300 is assumed anywhere in this entry.

**The smallest mechanism that guarantees the invariants: an allocation made at measure time, in the
page's own `LinearLayout`.**
- **What it is.** The root of `fragment_chat.xml` becomes a small `LinearLayout` subclass. Its
  `onMeasure` does three things, in order:
  1. it measures every child that never yields: the bars, the reply bar when shown, and the composer
     row;
  2. it gives the panels their heights from a pure function, in this order: the floor; then the
     player, at full height or refused; then the picker, at 240 dp, or less down to 152, or refused;
  3. it lets the list take what is left.
  - The function holds the whole policy, and can be unit-tested without a device. The subclass only
    applies it.
- **A refusal is decided at two moments:**
  - **when a panel is asked to open and its minimum does not fit:** it does not open, and a one-line
    message says why, from a string resource;
  - **when an open panel no longer fits because the page shrank** (the keyboard, a rotation, the reply
    bar). How it gives way is A CHOICE STILL TO MAKE: closed, or held hidden until the room comes
    back. Either way the user is told once.
- **The floor is read from a row, not fixed in dp.** An emote is `textSize × 1.5`
  (`ChatFragment.kt:3743`), so a row holding one grows with the font scale. The measure reads the row
  height at the current font scale.
- **Why not a smaller mechanism:**
  - **A `minHeight` on the list's frame does nothing here.** Once the weight is shared out, the frame
    is measured with an exact spec, which a minimum does not override. Even if it held, every child
    below the frame, the composer included, would still be pushed off the window: once the space is
    spent, `LinearLayout` fits nothing (the *Backlog*'s root).
  - **A `ConstraintLayout`** can give the list a minimum and the panels maxima. It cannot express an
    order of yielding or a refusal, and the floor at the font scale would still need code. It
    replaces the whole layout to do less.
  - **Heights set from a layout listener or an insets listener in `ChatFragment`** take a second
    layout pass after every change. The frame in between shows the composer off screen, and the
    policy would live in the fragment, against *Architecture rules*.
  - **Measure time is the smallest place where the invariant holds on every frame,** the frame on
    which the keyboard arrives included.

**The floor's candidates: 2, 3 and 4 rows containing an emote, at font scale 1.15.**
- **What a row is:** a row with an emote measured 68 px, 28.5 dp, on 2026-11-10.
- **The list's padding:** its bottom padding is 12 dp. Its top padding shows rows, because the list has
  `clipToPadding="false"` (`fragment_chat.xml:144`).
- So a floor of n rows is n × 28.5 + 12 dp:

  | Candidate | Floor |
  |---|---|
  | 2 rows | 69.0 dp |
  | 3 rows | 97.4 dp |
  | 4 rows | 125.9 dp |

- None is chosen here.
- **At font scale 1.0** the rows' part would be about 1/1.15, 87%, of its height here, if a row's
  height follows the text, and the 12 dp of padding would not change: about 61.5, 86.3 and 111.1 dp.
  Computed on that assumption, not measured.
- **The "New messages" button** sits over the list's bottom right corner when shown, 48 dp tall with a
  12 dp margin. Over a 69 dp floor it covers part of the last two rows, on the right side.
- **What each candidate leaves the player on the typical phone with the keyboard up** (page 535.6 dp):
  175.5, 147.0 and 118.5 dp on today's layout, where the player needs 300. Every candidate refuses the
  player there. The variants change this; their table follows them below.

**The proposal on today's layout, for every row of the simulation table and for the reference phone's
24 classes.**

**The proposal on today's layout, every row of the simulation table.** In dp. Each floor's cell is the list, then P the picker's height and S the player's, ✗ where it is refused with a message. LIMIT: the bars and the floor do not fit, every panel is refused, and the list gets what is left.

| Geometry | Configuration | Page | Today, measured | Floor 2 rows, 69.0 | Floor 3 rows, 97.4 | Floor 4 rows, 125.9 |
|---|---|---|---|---|---|---|
| Default size, 450 | none | 769.1 | 525.9 | 525.9 | 525.9 | 525.9 |
| Default size, 450 | picker | 769.1 | 285.9 | 285.9 · P 240 | 285.9 · P 240 | 285.9 · P 240 |
| Default size, 450 | stream | 769.1 | 177.8 | 177.7 · S 300 | 177.7 · S 300 | 177.7 · S 300 |
| Default size, 450 | stream, picker | 769.1 | **0.0, field off** | 177.7 · P ✗ · S 300 | 177.7 · P ✗ · S 300 | 177.7 · P ✗ · S 300 |
| Default size, 450 | stream, picker, keyboard | 514.1 | **0.0, field off** | 69.0 · P 202 · S ✗ | 97.4 · P 173 · S ✗ | 270.9 · P ✗ · S ✗ |
| Default size, 450 | stream, keyboard | 514.1 | **0.0, field off** | 270.9 · S ✗ | 270.9 · S ✗ | 270.9 · S ✗ |
| Compact | none | 539.5 | 297.0 | 297.0 | 297.0 | 297.0 |
| Compact | picker | 539.5 | 57.0 | 69.0 · P 228 | 97.4 · P 200 | 125.9 · P 171 |
| Compact | stream | 539.5 | **0.0, field partly** | 297.0 · S ✗ | 297.0 · S ✗ | 297.0 · S ✗ |
| Compact | stream, picker | 539.5 | **0.0, field off** | 69.0 · P 228 · S ✗ | 97.4 · P 200 · S ✗ | 125.9 · P 171 · S ✗ |
| Compact, landscape | none | 303.5 | 61.0 | **LIMIT** 61.0 | **LIMIT** 61.0 | **LIMIT** 61.0 |
| Compact, landscape | picker | 303.5 | **0.0, field off** | **LIMIT** 61.0 · P ✗ | **LIMIT** 61.0 · P ✗ | **LIMIT** 61.0 · P ✗ |
| Compact, landscape | stream, picker | 303.5 | **0.0, field off** | **LIMIT** 61.0 · P ✗ · S ✗ | **LIMIT** 61.0 · P ✗ · S ✗ | **LIMIT** 61.0 · P ✗ · S ✗ |
| Typical | none | 827.0 | 584.0 | 584.0 | 584.0 | 584.0 |
| Typical | picker | 827.0 | 344.0 | 344.0 · P 240 | 344.0 · P 240 | 344.0 · P 240 |
| Typical | stream | 827.0 | 235.8 | 235.8 · S 300 | 235.8 · S 300 | 235.8 · S 300 |
| Typical | stream, picker | 827.0 | **0.0** | 69.0 · P 167 · S 300 | 235.8 · P ✗ · S 300 | 235.8 · P ✗ · S 300 |
| Typical | stream, picker, keyboard | 535.6 | **0.0, field off** | 69.0 · P 224 · S ✗ | 97.4 · P 195 · S ✗ | 125.9 · P 167 · S ✗ |
| Typical | stream, keyboard | 535.6 | **0.0, field partly** | 292.6 · S ✗ | 292.6 · S ✗ | 292.6 · S ✗ |
| Large | none | 915.7 | 673.3 | 673.3 | 673.3 | 673.3 |
| Large | picker | 915.7 | 433.3 | 433.3 · P 240 | 433.3 · P 240 | 433.3 · P 240 |
| Large | stream | 915.7 | 325.3 | 325.1 · S 300 | 325.1 · S 300 | 325.1 · S 300 |
| Large | stream, picker | 915.7 | 85.3 | 85.1 · P 240 · S 300 | 97.4 · P 228 · S 300 | 125.9 · P 199 · S 300 |
| Large | stream, picker, keyboard | 630.3 | **0.0, field off** | 147.9 · P 240 · S ✗ | 147.9 · P 240 · S ✗ | 147.9 · P 240 · S ✗ |
| Large | stream, keyboard | 630.3 | 40.0 | 387.9 · S ✗ | 387.9 · S ✗ | 387.9 · S ✗ |
| Tablet | none | 1179.5 | 937.0 | 937.0 | 937.0 | 937.0 |
| Tablet | picker | 1179.5 | 697.0 | 697.0 · P 240 | 697.0 · P 240 | 697.0 · P 240 |
| Tablet | stream | 1179.5 | 589.0 | 588.8 · S 300 | 588.8 · S 300 | 588.8 · S 300 |
| Tablet | stream, picker | 1179.5 | 349.0 | 348.8 · P 240 · S 300 | 348.8 · P 240 · S 300 | 348.8 · P 240 · S 300 |
| Tablet | none, keyboard | 889.5 | 647.0 | 647.0 | 647.0 | 647.0 |
| Tablet, landscape | none | 699.5 | 457.0 | 457.0 | 457.0 | 457.0 |
| Tablet, landscape | picker | 699.5 | 217.0 | 217.0 · P 240 | 217.0 · P 240 | 217.0 · P 240 |
| Tablet, landscape | stream | 699.5 | 109.0 | 108.8 · S 300 | 108.8 · S 300 | 457.0 · S ✗ |
| Tablet, landscape | stream, picker | 699.5 | **0.0, field off** | 108.8 · P ✗ · S 300 | 108.8 · P ✗ · S 300 | 217.0 · P 240 · S ✗ |

**The proposal on today's layout, the reference phone's 24 classes.** Page 913.9 dp in portrait, 617.8 with the keyboard up, 405.0 in landscape, where every keyboard floats. Today's lists are the measured pixels at 2.3875 px/dp.

| Orientation | Panels | Keyboard | Today, measured | Floor 2 | Floor 3 | Floor 4 |
|---|---|---|---|---|---|---|
| portrait | none | down | 671.8 | 671.8 | 671.8 | 671.8 |
| portrait | none | up | 375.7 | 375.7 | 375.7 | 375.7 |
| portrait | reply | down | 615.3 | 615.3 | 615.3 | 615.3 |
| portrait | reply | up | 319.2 | 319.2 | 319.2 | 319.2 |
| portrait | picker | down | 431.8 | 431.8 · P 240 | 431.8 · P 240 | 431.8 · P 240 |
| portrait | picker | up | 135.7 | 135.7 · P 240 | 135.7 · P 240 | 135.7 · P 240 |
| portrait | picker, reply | down | 375.3 | 375.3 · P 240 | 375.3 · P 240 | 375.3 · P 240 |
| portrait | picker, reply | up | 79.2 | 79.2 · P 240 | 97.4 · P 222 | 125.9 · P 193 |
| portrait | stream | down | 323.8 | 323.6 · S 300 | 323.6 · S 300 | 323.6 · S 300 |
| portrait | stream | up | 27.6 | 375.7 · S ✗ | 375.7 · S ✗ | 375.7 · S ✗ |
| portrait | stream, reply | down | 267.2 | 267.1 · S 300 | 267.1 · S 300 | 267.1 · S 300 |
| portrait | stream, reply | up | **0.0, composer partly under the keyboard** | 319.2 · S ✗ | 319.2 · S ✗ | 319.2 · S ✗ |
| portrait | stream, picker | down | 83.8 | 83.6 · P 240 · S 300 | 97.4 · P 226 · S 300 | 125.9 · P 198 · S 300 |
| portrait | stream, picker | up | **0.0, composer off screen** | 135.7 · P 240 · S ✗ | 135.7 · P 240 · S ✗ | 135.7 · P 240 · S ✗ |
| portrait | stream, picker, reply | down | 27.2 | 69.0 · P 198 · S 300 | 97.4 · P 170 · S 300 | 267.1 · P ✗ · S 300 |
| portrait | stream, picker, reply | up | **0.0, composer off screen** | 79.2 · P 240 · S ✗ | 97.4 · P 222 · S ✗ | 125.9 · P 193 · S ✗ |
| landscape | none | any | 162.9 | 162.9 | 162.9 | 162.9 |
| landscape | reply | any | 106.4 | 106.4 | 106.4 | **LIMIT** 106.4 |
| landscape | picker | any | **0.0, composer off screen** | 162.9 · P ✗ | 162.9 · P ✗ | 162.9 · P ✗ |
| landscape | picker, reply | any | **0.0, composer off screen** | 106.4 · P ✗ | 106.4 · P ✗ | **LIMIT** 106.4 · P ✗ |
| landscape | stream | any | **0.0, composer off screen** | 162.9 · S ✗ | 162.9 · S ✗ | 162.9 · S ✗ |
| landscape | stream, reply | any | **0.0, composer off screen** | 106.4 · S ✗ | 106.4 · S ✗ | **LIMIT** 106.4 · S ✗ |
| landscape | stream, picker | any | **0.0, composer off screen** | 162.9 · P ✗ · S ✗ | 162.9 · P ✗ · S ✗ | 162.9 · P ✗ · S ✗ |
| landscape | stream, picker, reply | any | **0.0, composer off screen** | 106.4 · P ✗ · S ✗ | 106.4 · P ✗ · S ✗ | **LIMIT** 106.4 · P ✗ · S ✗ |

- **Read from the tables:**
  - **The text field is on screen in every row and every class,** under every floor.
  - **Where today has no chat, the proposal keeps the floor and refuses a panel.** On the typical phone
    with stream and picker open, floor 2 keeps both, the picker at 167 dp. Floor 3 or 4 refuses the
    picker.
  - **Wherever the keyboard is up with the stream on, the player is refused, on every phone in the
    tables:** the large phone and the reference included. The page cannot hold the bars, the mute
    row, 300 dp of player and any of the floors. The tablet's one keyboard reading had no stream.
    This is the main use the severity names, and the proposal alone cannot keep it. The variants below
    are the owner's and Federico's first move against it.
- **Configurations where even the bars plus the floor do not fit: a LIMIT, declared, not hidden.**
  - **Today's layout:**
    - the compact phone in landscape, under every floor: 61.0 dp of list, short of floor 2 by 8.0 dp;
    - the same with the reply bar: 4.5 dp, short by 64.5;
    - this phone in landscape with the reply bar, under floor 4: 106.4 dp, short by 19.5.
  - **Variant A:** the same three, each about 6 dp less short. On the compact phone in landscape, A
    misses floor 2 by 1.9 dp.
  - **Variant B:** only the compact phone in landscape with the reply bar, short of floor 2 by
    10.4 dp; and without the reply bar, under floor 4 only, short by 10.9.
  - **What happens there:** every panel is refused, with its message, and the list gets what is left,
    below the floor.
  - **The text field stays entirely on screen in every one of them,** computed for every page of the
    simulation and of the reference phone, with the reply bar shown and hidden.
  - **So principle 2 cannot hold there, and principle 1 does.** No page measured so far breaks the
    hard invariant once the panels are refused.
  - The compact phone's keyboard configurations were not read on 2026-11-10, so they are not in these
    numbers.

**The top of the page: reclaimed first, so that the floor and the yield order are needed less often.**
- **Federico's decisions, which replace point (b) of the addendum:**
  - "START PCG" and the stream button STAY TEXT BUTTONS. No icons are proposed for them.
  - The stream button's label is unclear, and is to be replaced by action labels. Federico chooses the
    words.
  - The status line is to be compacted: the user name and the channel, with the connection state
    carried by an indicator.

**The stream button's labels today, read.**
- **While the player is off:** `stream_off`, "Video ON" (`strings.xml:12`). This is also the layout's
  initial text (`fragment_chat.xml:97`).
- **While the player is on:** `stream_on`, "Video OFF" (`strings.xml:13`).
- Both are set by `updateStreamUi` (`ChatFragment.kt:3494-3498`), and are shown in capitals, as the
  owner quotes them.
- **Both read as a state, and each names the state the player is NOT in:** "VIDEO ON" is shown while
  there is no video.
- **The string names follow the state in which each label is shown, the reverse of their text:**
  `stream_off` holds "Video ON".

**Candidate action labels, beside "START PCG".** Each width is measured in Roboto Regular at 16.1 dp
(14 sp × 1.15), in capitals, so it is a lower bound for the buttons' heavier weight:

| Off / on | Widths |
|---|---|
| WATCH STREAM / HIDE STREAM (the owner's example) | 123.1 / 102.3 dp |
| SHOW STREAM / HIDE STREAM | 113.2 / 102.3 dp |
| START STREAM / STOP STREAM | 115.9 / 107.1 dp |
| OPEN PLAYER / CLOSE PLAYER | 103.9 / 111.0 dp |
| Today: VIDEO ON / VIDEO OFF; START PCG for scale | 72.0 / 78.3 dp; 84.8 dp |

- **START STREAM / STOP STREAM** is the closest in form to START PCG. A user who streams may read "stop
  stream" as ending a broadcast.
- **The room for a label:** at 360 dp each of the two buttons is (360 − 16 − 8) / 2 = 168 dp, less the
  button's own horizontal padding. That padding was not resolved: the style is
  `?android:attr/buttonBarButtonStyle` under a MaterialComponents theme.
  - A label that wraps makes the action row taller, and reopens the instance list.
  - The screenshot step below settles it.
- **The string names would follow the action,** for example `stream_watch` and `stream_hide`. That is
  a code change, for later.
- **Where the current label is cited.** Searched in every tracked file at `44e22da`, in
  `unouidol/tmc-site` and `unouidol/unouidol.github.io`, in the notes of every published release and
  in the bundled assets:
  - **Code:** `ChatFragment.kt:3494-3498`, `fragment_chat.xml:97` and `strings.xml:12-13` only.
  - **Tests:** none. Nothing in `test` or `androidTest` names `btnToggleStream`, `btnMuteStream`,
    `btnStartPcg`, `textStatus`, `stream_on` or `stream_off`.
  - **Manual plans:** none quotes the label.
    - This plan names "the stream button" once (*Backlog*, how the landscape instance is reached). It
      names the mute button, the action buttons and the status line in its measurement records. The
      records stay as written.
  - **`RELEASE-STATE.md` and `AGENTS.md`:** none.
  - **A user guide:** none exists, in this repository or on either site, and the README names none of
    these controls.
  - **Release notes:** the published releases name only **Start PCG**, in ten of them from 2.0.0 to
    5.4.2. That button stays as it is.

**The status line today: ten texts, from nine writes in one file, and one string written nowhere.**
Only `ChatFragment` writes `textStatus`. Besides its binding (`:1495`), its one other use is the click
listener that clears the channel field's focus (`:1777`).

| State | Text | Written at |
|---|---|---|
| Loading | `status_loading`, "Loading @%1$s on #%2$s…" | `onViewCreated`, `:1745` |
| Account not found | `account_not_found`, "Account not found" | `:1745`, with no config |
| Connecting | `status_connecting`, "Connecting as @%1$s on #%2$s…" | `:2412` |
| Missing token | `status_missing_token`, "Missing token for @%1$s (login again)" | `:2419` |
| Session expired | `status_backend_session_reauthorize`, "Session expired for @%1$s. Return to Accounts and tap Refresh to reauthorize." | `:2464` (the backend's answer) and `:2332` (an authentication NOTICE) |
| Could not authenticate | `status_backend_auth_failed`, "Could not authenticate @%1$s. Check the connection or reauthorize the account manually." | `:2472` |
| Connected | `status_connected`, "Connected as @%1$s on #%2$s" | `:2209` |
| Read error | `status_read_error`, "Read error: %1$s" | `:2304` |
| Disconnected, from `onStop` | `status_disconnected_account`, "Disconnected (@%1$s)" | `:2080` |
| Disconnected, with no config | `status_disconnected`, "Disconnected" | `:2080`, and the layout's default (`fragment_chat.xml:105`) |

- **The read error's text** is the exception's message, or a literal `"unknown"` (`:2306`). That
  literal is not resource-backed. It is pre-existing, and is recorded here, not fixed.
- **`status_send_error`** is defined, and written nowhere.
- **Whether a reconnect follows a read error** was not read.

**The indicator: four states, each with its own shape and colour, and the words kept where they carry
an instruction.**
- **Connected:** a filled circle.
- **In progress** (loading, connecting): an open ring.
- **Disconnected** (both): a hollow circle with a bar.
- **Problem:** a triangle with an exclamation mark. It covers the read error, the account not found,
  the missing token, the expired session and the failed authentication.
- **The colours are green, amber, grey and red,** but the shapes alone tell the four apart. Colour is
  never the only signal: as the owner's constraint says, red against green is the pair colour-blind
  users cannot tell apart, and a screen reader does not read colour. The graphics need at least 3:1
  contrast against the bar.
- **What a screen reader hears:**
  - The indicator's content description states the state in words, from new strings: for example
    "Connected", "Connecting", "Disconnected" and "Connection problem". Federico chooses the words.
  - The row is read as one item: that description, then the names. In content, that is today's
    sentence, whose strings already exist.
- **Four states carry an instruction, and keep their words on screen:** the account not found, the
  missing token, the expired session and the failed authentication. The read error keeps its words
  too: it says what happened.
- **The compact form, the names and the indicator, serves** the connected, in-progress and
  disconnected states.

**The variants, with the dp each reclaims.** C is computed, with an example ratio.
- **A: the compact status, with the mute control as an icon on the same row.**
  - The mute row goes, and `streamControlsBox` with it, padding included.
  - The status row becomes a fixed 48 dp, for the icon's touch target: today it is 46.1 dp.
  - **Reclaims 54.2 dp with the stream on** (48.17 for the mute row and 7.96 of padding, less the
    1.93 the status row grows), and 6.0 dp with the stream off.
- **B: A, and the status merged into the channel bar,** which keeps its 64 dp.
  - **Reclaims 102.2 dp with the stream on, and 54.0 dp with the stream off.**
  - **The problem states then need a row of their own,** shown only while the problem lasts. That
    costs 46.1 dp for one line, more when the text wraps, and the mechanism counts it as a bar.
- **C: either, with the player's height taken from its width at the video's ratio.**
  - **Possible, at two costs.**
    1. **It goes below Twitch's documented 300 px minimum in portrait on every phone of the table,**
       for any video wider than the width over 300: at 411 dp, any ratio above 1.37:1.
       - With 16:9 as an EXAMPLE, the player would be 202.5 dp at 360 dp of width, 216.0 at 384,
         231.2 at 411, 252.0 at 448 and 254.3 at 452.
       - It caps at 300 wherever the width reaches 533 dp, so landscape and the tablet do not change.
    2. **The ratio is known only once a video plays,** through `getPlaybackStats().videoResolution`.
       - That needs a change to `player.html` in `unouidol/ircminichat`, and a channel from that page
         to the app. The stream session has neither today: no message delegate and no WebExtension
         (`GeckoSessionManager.kt:1678-1694`, `:2201-2207`).
       - Before the first reading (an offline channel, the load itself, an advert), the box needs a
         default, which is itself a choice.
       - A video taller than it is wide caps at 300 dp and reclaims nothing.
  - **A box ratio chosen as a layout decision,** instead of read from the video, needs no channel. But
    how the embed fits a video of another shape into it is neither documented nor observed.
  - **What it reclaims, beyond A or B:** 300 less the player's height. With the 16:9 example, 68.8 dp
    on the typical phone and 97.5 on the compact one.

**The thresholds, recomputed for each variant.** In dp of page height, at font scale 1.15. The last
three columns are the reference phone's margins for the "list 0 px" column. Today's rows repeat the
plan's table of 2026-11-10. Two of its "entirely off" values differ from it by 1 dp: 589 against 588,
and 829 against 828. That comes from rounding the composer row: 162 px is 67.85 dp, not 67.9.

**Thresholds, Today.** Bars 242.1 dp.

| Panels open | List 0 px below | Field partly off below | Field entirely off below | Floor 2 kept with all panels at full height down to | Floor 3 | Floor 4 | Reference portrait, 914 | Reference with keyboard, 618 | Reference landscape, 405 |
|---|---|---|---|---|---|---|---|---|---|
| none | 242 dp | 232 dp | 184 dp | 311 dp | 340 dp | 368 dp | +672 | +376 | +163 |
| reply | 299 dp | 289 dp | 240 dp | 368 dp | 396 dp | 425 dp | +615 | +319 | +106 |
| picker | 482 dp | 472 dp | 424 dp | 551 dp | 580 dp | 608 dp | +432 | +136 | **-77** |
| picker, reply | 539 dp | 529 dp | 480 dp | 608 dp | 636 dp | 665 dp | +375 | +79 | **-134** |
| stream | 590 dp | 580 dp | 532 dp | 659 dp | 688 dp | 716 dp | +324 | +28 | **-185** |
| stream, reply | 647 dp | 637 dp | 589 dp | 716 dp | 744 dp | 773 dp | +267 | **-29** | **-242** |
| stream, picker | 830 dp | 820 dp | 772 dp | 899 dp | 928 dp | 956 dp | +84 | **-212** | **-425** |
| stream, picker, reply | 887 dp | 877 dp | 829 dp | 956 dp | 984 dp | 1013 dp | +27 | **-269** | **-482** |

**Thresholds, A.** Bars 236.1 dp.

| Panels open | List 0 px below | Field partly off below | Field entirely off below | Floor 2 kept with all panels at full height down to | Floor 3 | Floor 4 | Reference portrait, 914 | Reference with keyboard, 618 | Reference landscape, 405 |
|---|---|---|---|---|---|---|---|---|---|
| none | 236 dp | 226 dp | 178 dp | 305 dp | 334 dp | 362 dp | +678 | +382 | +169 |
| reply | 293 dp | 283 dp | 234 dp | 362 dp | 390 dp | 418 dp | +621 | +325 | +112 |
| picker | 476 dp | 466 dp | 418 dp | 545 dp | 574 dp | 602 dp | +438 | +142 | **-71** |
| picker, reply | 533 dp | 523 dp | 474 dp | 602 dp | 630 dp | 658 dp | +381 | +85 | **-128** |
| stream | 536 dp | 526 dp | 478 dp | 605 dp | 634 dp | 662 dp | +378 | +82 | **-131** |
| stream, reply | 593 dp | 583 dp | 534 dp | 662 dp | 690 dp | 718 dp | +321 | +25 | **-188** |
| stream, picker | 776 dp | 766 dp | 718 dp | 845 dp | 874 dp | 902 dp | +138 | **-158** | **-371** |
| stream, picker, reply | 833 dp | 823 dp | 774 dp | 902 dp | 930 dp | 958 dp | +81 | **-215** | **-428** |

**Thresholds, B.** Bars 188.1 dp.

| Panels open | List 0 px below | Field partly off below | Field entirely off below | Floor 2 kept with all panels at full height down to | Floor 3 | Floor 4 | Reference portrait, 914 | Reference with keyboard, 618 | Reference landscape, 405 |
|---|---|---|---|---|---|---|---|---|---|
| none | 188 dp | 178 dp | 130 dp | 257 dp | 286 dp | 314 dp | +726 | +430 | +217 |
| reply | 245 dp | 235 dp | 186 dp | 314 dp | 342 dp | 370 dp | +669 | +373 | +160 |
| picker | 428 dp | 418 dp | 370 dp | 497 dp | 526 dp | 554 dp | +486 | +190 | **-23** |
| picker, reply | 485 dp | 475 dp | 426 dp | 554 dp | 582 dp | 610 dp | +429 | +133 | **-80** |
| stream | 488 dp | 478 dp | 430 dp | 557 dp | 586 dp | 614 dp | +426 | +130 | **-83** |
| stream, reply | 545 dp | 535 dp | 486 dp | 614 dp | 642 dp | 670 dp | +369 | +73 | **-140** |
| stream, picker | 728 dp | 718 dp | 670 dp | 797 dp | 826 dp | 854 dp | +186 | **-110** | **-323** |
| stream, picker, reply | 785 dp | 775 dp | 726 dp | 854 dp | 882 dp | 910 dp | +129 | **-167** | **-380** |

**Thresholds, C, on B, 16:9 as an example ratio.** Bars 188.1 dp. The thresholds are the typical phone's, 411 dp wide, where the player is 231.2 dp. The three reference columns use the reference phone's own widths: 452 dp in portrait, player 254.3 dp; 1005 dp in landscape, player 300 dp.

| Panels open | List 0 px below | Field partly off below | Field entirely off below | Floor 2 kept with all panels at full height down to | Floor 3 | Floor 4 | Reference portrait, 914 | Reference with keyboard, 618 | Reference landscape, 405 |
|---|---|---|---|---|---|---|---|---|---|
| none | 188 dp | 178 dp | 130 dp | 257 dp | 286 dp | 314 dp | +726 | +430 | +217 |
| reply | 245 dp | 235 dp | 186 dp | 314 dp | 342 dp | 370 dp | +669 | +373 | +160 |
| picker | 428 dp | 418 dp | 370 dp | 497 dp | 526 dp | 554 dp | +486 | +190 | **-23** |
| picker, reply | 485 dp | 475 dp | 426 dp | 554 dp | 582 dp | 610 dp | +429 | +133 | **-80** |
| stream | 419 dp | 409 dp | 361 dp | 488 dp | 517 dp | 545 dp | +472 | +175 | **-83** |
| stream, reply | 476 dp | 466 dp | 417 dp | 545 dp | 573 dp | 602 dp | +415 | +119 | **-140** |
| stream, picker | 659 dp | 649 dp | 601 dp | 728 dp | 757 dp | 785 dp | +232 | **-65** | **-323** |
| stream, picker, reply | 716 dp | 706 dp | 657 dp | 785 dp | 813 dp | 842 dp | +175 | **-121** | **-380** |

**The simulation table and the reference classes, recomputed for each variant alone,** with no floor,
beside today's measured values.

**The variants alone, with no floor, every row of the simulation table.** List in dp, and the field where it is not on screen; bold where the list is 0 or the field is not on. C is B with the player at the width over 16:9, as an EXAMPLE ratio, capped at 300 dp.

| Geometry | Configuration | Page | Today, measured | A | B | C |
|---|---|---|---|---|---|---|
| Default size, 450 | none | 769.1 | 525.9 | 531.9 | 579.9 | 579.9 |
| Default size, 450 | picker | 769.1 | 285.9 | 291.9 | 339.9 | 339.9 |
| Default size, 450 | stream | 769.1 | 177.8 | 231.9 | 279.9 | 363.9 |
| Default size, 450 | stream, picker | 769.1 | **0.0, field off** | **0.0** | 39.9 | 123.9 |
| Default size, 450 | stream, picker, keyboard | 514.1 | **0.0, field off** | **0.0, field off** | **0.0, field off** | **0.0, field off** |
| Default size, 450 | stream, keyboard | 514.1 | **0.0, field off** | **0.0, field partly** | 24.9 | 108.9 |
| Compact | none | 539.5 | 297.0 | 303.0 | 351.0 | 351.0 |
| Compact | picker | 539.5 | 57.0 | 63.0 | 111.0 | 111.0 |
| Compact | stream | 539.5 | **0.0, field partly** | 3.0 | 51.0 | 148.5 |
| Compact | stream, picker | 539.5 | **0.0, field off** | **0.0, field off** | **0.0, field off** | **0.0, field off** |
| Compact, landscape | none | 303.5 | 61.0 | 67.0 | 115.0 | 115.0 |
| Compact, landscape | picker | 303.5 | **0.0, field off** | **0.0, field off** | **0.0, field off** | **0.0, field off** |
| Compact, landscape | stream, picker | 303.5 | **0.0, field off** | **0.0, field off** | **0.0, field off** | **0.0, field off** |
| Typical | none | 827.0 | 584.0 | 590.0 | 638.0 | 638.0 |
| Typical | picker | 827.0 | 344.0 | 350.0 | 398.0 | 398.0 |
| Typical | stream | 827.0 | 235.8 | 290.0 | 338.0 | 406.8 |
| Typical | stream, picker | 827.0 | **0.0** | 50.0 | 98.0 | 166.8 |
| Typical | stream, picker, keyboard | 535.6 | **0.0, field off** | **0.0, field off** | **0.0, field off** | **0.0, field off** |
| Typical | stream, keyboard | 535.6 | **0.0, field partly** | **0.0** | 46.6 | 115.4 |
| Large | none | 915.7 | 673.3 | 679.3 | 727.3 | 727.3 |
| Large | picker | 915.7 | 433.3 | 439.3 | 487.3 | 487.3 |
| Large | stream | 915.7 | 325.3 | 379.3 | 427.3 | 475.3 |
| Large | stream, picker | 915.7 | 85.3 | 139.3 | 187.3 | 235.3 |
| Large | stream, picker, keyboard | 630.3 | **0.0, field off** | **0.0, field off** | **0.0, field off** | **0.0, field partly** |
| Large | stream, keyboard | 630.3 | 40.0 | 93.9 | 141.9 | 189.9 |
| Tablet | none | 1179.5 | 937.0 | 943.0 | 991.0 | 991.0 |
| Tablet | picker | 1179.5 | 697.0 | 703.0 | 751.0 | 751.0 |
| Tablet | stream | 1179.5 | 589.0 | 643.0 | 691.0 | 691.0 |
| Tablet | stream, picker | 1179.5 | 349.0 | 403.0 | 451.0 | 451.0 |
| Tablet | none, keyboard | 889.5 | 647.0 | 653.0 | 701.0 | 701.0 |
| Tablet, landscape | none | 699.5 | 457.0 | 463.0 | 511.0 | 511.0 |
| Tablet, landscape | picker | 699.5 | 217.0 | 223.0 | 271.0 | 271.0 |
| Tablet, landscape | stream | 699.5 | 109.0 | 163.0 | 211.0 | 211.0 |
| Tablet, landscape | stream, picker | 699.5 | **0.0, field off** | **0.0, field off** | **0.0, field partly** | **0.0, field partly** |

**The variants alone, the reference phone's 24 classes.** C uses the reference's portrait width, 452 dp, so the player is 254.3 dp in portrait and stays 300 dp in landscape.

| Orientation | Panels | Keyboard | Today, measured | A | B | C |
|---|---|---|---|---|---|---|
| portrait | none | down | 671.8 | 677.8 | 725.8 | 725.8 |
| portrait | none | up | 375.7 | 381.7 | 429.7 | 429.7 |
| portrait | reply | down | 615.3 | 621.3 | 669.3 | 669.3 |
| portrait | reply | up | 319.2 | 325.2 | 373.2 | 373.2 |
| portrait | picker | down | 431.8 | 437.8 | 485.8 | 485.8 |
| portrait | picker | up | 135.7 | 141.7 | 189.7 | 189.7 |
| portrait | picker, reply | down | 375.3 | 381.3 | 429.3 | 429.3 |
| portrait | picker, reply | up | 79.2 | 85.2 | 133.2 | 133.2 |
| portrait | stream | down | 323.8 | 377.8 | 425.8 | 471.6 |
| portrait | stream | up | 27.6 | 81.7 | 129.7 | 175.5 |
| portrait | stream, reply | down | 267.2 | 321.3 | 369.3 | 415.1 |
| portrait | stream, reply | up | **0.0, composer partly under the keyboard** | 25.2 | 73.2 | 119.0 |
| portrait | stream, picker | down | 83.8 | 137.8 | 185.8 | 231.6 |
| portrait | stream, picker | up | **0.0, composer off screen** | **0.0, field off** | **0.0, field off** | **0.0, field off** |
| portrait | stream, picker, reply | down | 27.2 | 81.3 | 129.3 | 175.1 |
| portrait | stream, picker, reply | up | **0.0, composer off screen** | **0.0, field off** | **0.0, field off** | **0.0, field off** |
| landscape | none | any | 162.9 | 168.9 | 216.9 | 216.9 |
| landscape | reply | any | 106.4 | 112.4 | 160.4 | 160.4 |
| landscape | picker | any | **0.0, composer off screen** | **0.0, field off** | **0.0, field partly** | **0.0, field partly** |
| landscape | picker, reply | any | **0.0, composer off screen** | **0.0, field off** | **0.0, field off** | **0.0, field off** |
| landscape | stream | any | **0.0, composer off screen** | **0.0, field off** | **0.0, field off** | **0.0, field off** |
| landscape | stream, reply | any | **0.0, composer off screen** | **0.0, field off** | **0.0, field off** | **0.0, field off** |
| landscape | stream, picker | any | **0.0, composer off screen** | **0.0, field off** | **0.0, field off** | **0.0, field off** |
| landscape | stream, picker, reply | any | **0.0, composer off screen** | **0.0, field off** | **0.0, field off** | **0.0, field off** |

**Rows where each variant alone still leaves the list below the floor, or the field not on, so the floor and the yield order still act:**

- A, floor 2: 14 of 34: Default size, 450: stream, picker; Default size, 450: stream, picker, keyboard; Default size, 450: stream, keyboard; Compact: picker; Compact: stream; Compact: stream, picker; Compact, landscape: none; Compact, landscape: picker; Compact, landscape: stream, picker; Typical: stream, picker; Typical: stream, picker, keyboard; Typical: stream, keyboard; Large: stream, picker, keyboard; Tablet, landscape: stream, picker
- A, floor 3: 15 of 34: Default size, 450: stream, picker; Default size, 450: stream, picker, keyboard; Default size, 450: stream, keyboard; Compact: picker; Compact: stream; Compact: stream, picker; Compact, landscape: none; Compact, landscape: picker; Compact, landscape: stream, picker; Typical: stream, picker; Typical: stream, picker, keyboard; Typical: stream, keyboard; Large: stream, picker, keyboard; Large: stream, keyboard; Tablet, landscape: stream, picker
- A, floor 4: 15 of 34: Default size, 450: stream, picker; Default size, 450: stream, picker, keyboard; Default size, 450: stream, keyboard; Compact: picker; Compact: stream; Compact: stream, picker; Compact, landscape: none; Compact, landscape: picker; Compact, landscape: stream, picker; Typical: stream, picker; Typical: stream, picker, keyboard; Typical: stream, keyboard; Large: stream, picker, keyboard; Large: stream, keyboard; Tablet, landscape: stream, picker
- B, floor 2: 11 of 34: Default size, 450: stream, picker; Default size, 450: stream, picker, keyboard; Default size, 450: stream, keyboard; Compact: stream; Compact: stream, picker; Compact, landscape: picker; Compact, landscape: stream, picker; Typical: stream, picker, keyboard; Typical: stream, keyboard; Large: stream, picker, keyboard; Tablet, landscape: stream, picker
- B, floor 3: 11 of 34: Default size, 450: stream, picker; Default size, 450: stream, picker, keyboard; Default size, 450: stream, keyboard; Compact: stream; Compact: stream, picker; Compact, landscape: picker; Compact, landscape: stream, picker; Typical: stream, picker, keyboard; Typical: stream, keyboard; Large: stream, picker, keyboard; Tablet, landscape: stream, picker
- B, floor 4: 14 of 34: Default size, 450: stream, picker; Default size, 450: stream, picker, keyboard; Default size, 450: stream, keyboard; Compact: picker; Compact: stream; Compact: stream, picker; Compact, landscape: none; Compact, landscape: picker; Compact, landscape: stream, picker; Typical: stream, picker; Typical: stream, picker, keyboard; Typical: stream, keyboard; Large: stream, picker, keyboard; Tablet, landscape: stream, picker
- C, floor 2: 7 of 34: Default size, 450: stream, picker, keyboard; Compact: stream, picker; Compact, landscape: picker; Compact, landscape: stream, picker; Typical: stream, picker, keyboard; Large: stream, picker, keyboard; Tablet, landscape: stream, picker
- C, floor 3: 7 of 34: Default size, 450: stream, picker, keyboard; Compact: stream, picker; Compact, landscape: picker; Compact, landscape: stream, picker; Typical: stream, picker, keyboard; Large: stream, picker, keyboard; Tablet, landscape: stream, picker
- C, floor 4: 12 of 34: Default size, 450: stream, picker; Default size, 450: stream, picker, keyboard; Default size, 450: stream, keyboard; Compact: picker; Compact: stream, picker; Compact, landscape: none; Compact, landscape: picker; Compact, landscape: stream, picker; Typical: stream, picker, keyboard; Typical: stream, keyboard; Large: stream, picker, keyboard; Tablet, landscape: stream, picker

- **So the floor and the yield order are still needed in every variant,** as the owner expected, in
  landscape and on the compact phone:
  - with the picker open in landscape on the compact phone, in every variant;
  - with both panels open and the keyboard up, on every phone below the tablet;
  - and with the stream and the keyboard: in A and B on this phone at its default size and on the
    typical phone, and in A on the large phone at floors 3 and 4. C needs it there only at floor 4.

**The typical phone with the stream on and the keyboard up, by variant.**

**The typical phone, stream on, keyboard up: page 535.6 dp, today 0 px of chat and the field partly off.** Room left for the player above each floor is the page less the bars, the mute row where it has one, and the floor.

| Variant | Bars | List with no floor | Field | Room for the player above floor 2 | Floor 3 | Floor 4 | Player it needs |
|---|---|---|---|---|---|---|---|
| Today | 291.2 with the mute row | 0.0 | partly | 175.5 **refused** | 147.0 **refused** | 118.5 **refused** | 300.0 |
| A | 237.0 | 0.0 | on | 229.7 **refused** | 201.2 **refused** | 172.7 **refused** | 300.0 |
| B | 189.0 | 46.6 | on | 277.7 **refused** | 249.2 **refused** | 220.7 **refused** | 300.0 |
| C on A, 16:9 example | 237.0 | 67.4 | on | 229.7 **refused** | 201.2 **refused** | 172.7 **refused** | 231.2 |
| C on B, 16:9 example | 189.0 | 115.4 | on | 277.7 fits | 249.2 fits | 220.7 **refused** | 231.2 |

- **No variant and no floor candidate keeps the player at Twitch's documented 300 dp there.** B alone,
  with no floor, gives 46.6 dp of list, less than floor 2.
- **Only C on B fits the player and a floor,** at floor 2 or 3: 277.7 or 249.2 dp of room for 231.2.
  This holds with the 16:9 example, and only below Twitch's documented minimum.

**Fitting the status at 360 dp, with a long user name and a long channel name.** Widths are measured in
Roboto Regular at 16.1 dp, lower bounds for the bold the line uses. The names are synthetic, at
Twitch's 25-character maximum, which is stated from knowledge and was not read here. On the phone the
system font may differ.
- **Today's line already wraps.** "Connected as @… on #…" with two 25-character names is 554.5 dp,
  against 336 dp of row at 360. By width, that is two lines at every portrait phone width in the
  tables, 360 to 452 dp, and three with 25 × "w" at 360 and 384 dp. The line breaks at spaces, so it
  may need more. Each line adds about 22.1 dp: the measured row, 46.1 dp, less its 24 dp of padding.
  - The plan lists "the status line wrapping to a second line" among what reopens the instance list.
    Long names reopen it today.
- **A:** the text has 256 dp at 360 with the stream on (the row's padding, the 24 dp indicator and its
  8 dp gap, and the 48 dp mute icon taken out), and 304 dp with the stream off.
  - Two 8-character names, "@… · #…", are 171.2 dp, and fit.
  - Two 25-character names are 438.4 dp, and 641.2 dp with 25 × "w". They do not fit at any width in
    the table.
  - **What happens to the text:** one line, with the user name and the channel as two separate texts,
    each cut with an ellipsis at its end, so that neither disappears whole.
  - The full sentence stays in the content description, and could also be shown on a long press.
  - Without that limit the row would wrap, as today's does.
- **B:** at 360 dp the channel bar has 200 dp with the stream off, and 152 dp with it on (the mute icon
  added), for the channel field, the indicator and the user name together.
  - "@" and an 8-character name, with the indicator, take 113.8 dp. That leaves the channel field 86 dp,
    or 38 dp with the stream on.
  - Two 25-character names cannot be shown at all.
  - **So B does not fit at 360 dp with both names visible, even with short names once the stream is
    on.** It needs a further decision. One option: the channel field shows the current channel when
    not focused; today it is cleared after every switch (`ChatFragment.kt:1060`, `:1296`, `:1387`)
    and shows only its hint. The other: one of the names moves out of view, into the content
    description or a tap.
  - **The channel is shown nowhere else on the page today.** The status line is its only place, so no
    variant may drop it.

**What each variant changes for accessibility, and what names these controls.**
- **A:**
  - The mute control becomes an `ImageButton` of 48 × 48 dp. Its content description states the
    action, "Mute stream" or "Unmute stream", in new strings; the existing `mute` and `unmute` say
    "Mute" and "Unmute".
  - Its icon also changes shape with the state.
  - The status row is 48 dp. It is clickable today, through the focus-clearing listener, and stays so.
- **B:**
  - The channel bar carries the three 48 dp icons, the mute icon when the stream is on, the indicator
    and the name.
  - The channel field narrows as computed above. Its 48 dp minimum height stays.
  - The status row's click listener moves or goes. Section M's outside-click check names `textStatus`
    as one of the four views carrying `clearChannelFocusClickListener` (`ChatFragment.kt:1773-1778`),
    and that step's list would change with it.
- **C:** no change to touch targets or descriptions. The player keeps `stream_player`.
- **Every variant changes the height of a child of the page's `LinearLayout`.** By this plan's own rule
  (*Recorded results*, the instance list closed), that REOPENS the instance list, so the regression
  matrix below is measured again on the arrangement chosen.

**How the fix would be verified: the simulated geometries, as a regression matrix.**
- **The matrix:** the 34 simulated readings and the 24 reference classes. Each carries its predicted
  list, picker, player and field, under the chosen floor and arrangement, as in the tables above.
- **Offline:** the pure function, given each row's page and bars, returns the table's values. These are
  unit tests in `app/src/test`, one case for each row, with the reply bar's classes and two more font
  scales, 1.0 and 1.3, added.
- **On the phone:** the same geometries, set with `wm size` and `wm density`, with frames read from
  `dumpsys activity top` as on 2026-11-10. Each list must come within 1.1 dp of its prediction, each
  text field's top and bottom must be inside the page, and each refusal's message must be seen.
- **Mutants, to show that the checks look.** Each must fail at least the rows named:
  1. **The floor set to 0:** every row where the list sits exactly at the floor, such as the typical
     phone with stream and picker at floor 2, or the compact phone's picker.
  2. **The yield order reversed, the player yielding before the picker:** every row where both panels
     are asked for and do not both fit at full height. For example, the typical phone with stream and
     picker at floor 2 would keep the picker at 240 and refuse the player.
  3. **The picker's minimum removed:** every row where the picker is refused. For example, the typical
     phone with stream and picker at floor 3 would open the picker at 138.4 dp.
  4. **A silent refusal:** the message's assertion, on every ✗.
  5. **A checker that reads the composer row's frame instead of the text field's.** Under the proposal
     the two never disagree, because the whole row is kept. So this mutant is run on today's build,
     where the checker must reproduce the measured field states. Computed, it misreads two rows: the
     typical phone with stream and picker at 827.0 dp (field on, row partly off), and this phone at
     450 with stream and picker at 769.1 dp (field off, row's top on screen).
  6. **The floor fixed in dp instead of read from a row:** the font-scale cases.
  7. **The reply bar left out of the bars:** every reply class, and this phone in landscape with the
     reply bar at floor 4, where it is a LIMIT.

**A screenshot step for Federico's choice of the floor: planned, not run.**
- **What it needs:** a dev build of the fix in which the floor can be switched to 2, 3 or 4 rows for the
  step, or three builds. Neither is written.
- **What it shows:**
  - the typical phone (`wm density 420` on the physical size), in portrait: with the stream on and the
    keyboard raised from the composer; then with the stream and the picker open;
  - the compact phone (`wm size 720x1280`, `wm density 320`) in landscape, where the limit is;
  - each candidate floor on each, and later the arrangement, A, B or C, once one is built.
- **How each capture is taken:** `adb exec-out screencap -p > <file>`, with the frames from
  `dumpsys activity top` read beside it.
  - The capture names no path on the device, so Git Bash has nothing to rewrite (*AGENTS.md*, the Git
    Bash entry).
  - Each file's arrival is checked in a step of its own.
- **The screenshots show the account and the channel,** so they stay outside the repository, as the
  screenshot of 2026-11-10 did.
- **The font scale stays at 1.15.** `settings put system` is refused to the shell, which lacks
  `WRITE_SETTINGS`. Another font scale is for Federico to set by hand, if one is wanted.
- **Before the step:**
  - Federico switches "Disable permission monitoring" on.
  - Read and record `wm size`, `wm density`, `display_size_forced` and `display_density_forced`, the
    rotation (`lock 0`) and `accelerometer_rotation`, the font scale, and what is in front.
- **The restore, exactly as this plan records it (2026-11-10, the thresholds verified):**
  1. `cmd window user-rotation lock 0`, `wm size reset` and `wm density 382`. **Never
     `wm density reset`:** it would leave the phone at its physical 450 and drop Federico's override.
  2. `wm size reset` leaves `display_size_forced` as an empty key where it was absent.
     `settings delete global display_size_forced` returns it to `null`.
  3. The size and density changes close Developer options. Reopen it with
     `android.settings.APPLICATION_DEVELOPMENT_SETTINGS`.
  4. Read every value back against the before reading, and read the reference again: page 913.9 dp,
     list 671.8 dp.
  5. Federico switches "Disable permission monitoring" off. On this phone the toggle takes effect only
     after a restart. So the phone is rebooted, and a same-value write (`wm density 382`) must then be
     refused with `SecurityException: Must hold permission android.permission.WRITE_SECURE_SETTINGS`.

**2026-11-11, on `nbpromec1`: the original suite of 2026-11-06 found, and the run directories
inventoried. Both read-only.** These are the owner's rulings. Nothing was changed, moved or deleted,
on this machine or on the phone, and the phone was not touched.

**The original 15-case suite: found, outside the repository, and never a file of its own.**
- **CLOSED, by the owner's ruling later on 2026-11-11.** The original suite exists. Its expected exit
  codes agree with the rebuilt suite's, case for case. The rebuilt suite is the stricter of the two:
  - it checks more than the exit code;
  - its stand-in matches the phone's exit status;
  - it adds R1, R2, K1, P1 to P6 and L.
- **Where:** `%LOCALAPPDATA%\Temp\claude\5f4e4a46f49477fa\e1b06764-32f4-46a1-83c9-251038ddd4b8\scratchpad\sitting-test\`,
  the scratch folder of the session that wrote it here on 2026-11-06. It holds 485 files.
  - **The stand-ins**, by sha256:
    - `fake-adb.sh` `d1219a673a103048c3f7044ed778c327ca85dc5348e02bbb986d9cef06131ec5`;
    - `fake-adb2.sh` `43aa4929a48b0a6dd05646650766846133ea169e54991fd11dc5ade8a1d345cf`;
    - three broken extractors, `bad-extract.sh` `92088387…`, `bad-extract2.sh` `917db5f6…` and
      `bad-extract3.sh` `69cfb1e3…`.
  - **Synthetic journals**, and six run directories. The last, `run-1793965594`, holds `t10` to
    `t24`: the 15 cases.
  - **The tools it ran** were copied into each case from `%USERPROFILE%\tmc-5d-round\V\`. Those are
    byte-identical to the first frozen commit, `613ff07`: `sitting.sh`
    `854dca023884bc3dfe6f643d28b019fe0bda4f95f5df1bf4ec17d02a28d93164`, blob `e952ac2`, and
    `journal_extract.py` `f5abb4920daaccd9d9a9f1a9448fb705ae47d1ce28ba55dc71f778253c2cc678`, blob
    `b396676`.
- **The runner was never written to a file.** It survives only as an inline command in that
  session's transcript, under `%USERPROFILE%\.claude\projects\`, run at 11:46:32 UTC. Its output
  there reads 15 of 15 "as expected", and 0 forbidden tokens in the 37 files and 150 lines the cases
  saved. Those are the counts this plan records for 2026-11-06.
- **How it differs from the rebuilt suite.** The comparison is with `sitting_suite.sh` at the tips of
  `experiment/startup-5d-control` (`85a1314`) and `experiment/startup-5d-treatment` (`6c74ed3`), both
  sha256 `3bb0a050…`, with its `standin_adb.sh`.
  - **The expected exits are the same, case for case:** 0, 0, 3, 3, 0, 3, 4, 3, 3, 3, 3, 3, 0, 3, 3.
  - **The original asserts the exit code only.** It printed the keys it found but did not compare
    them. The rebuilt suite asserts the exit, a message pattern, each case's extra checks, and that
    no adb call fell outside the stand-in's model.
  - **The original's stand-in answers a failed read with exit 1,** and an unmodelled call with a
    message and exit 2. The phone answers a failed read with exit 0, which is what the rebuilt
    stand-in was corrected to on 2026-11-09. So the original's `t15` and `t23`, the unreadable
    journal at a poll and at the mark, exercised a non-zero exit the phone does not give.
  - **The original's `t14` runs `sitting.sh rate R` alone.** The rebuilt C5 runs it inside a session.
  - **The rebuilt suite has cases the original never had:** R1, R2, K1, P1 to P6 and L.
- **No other copy was found.** A search by file name for the stand-ins, the broken extractors,
  `sitting_suite.sh` and `standin_adb.sh` covered every session folder under
  `%LOCALAPPDATA%\Temp\claude\` and `%USERPROFILE%\tmc-5d-round`, and found only these. The clone has
  only the rebuilt suite.
- **What it changes:** nothing in the plan's record. Section M's statement that the suite was never
  committed stands, and it now says where the original is.

**The measurement run directories on this machine: listed by name and size, no contents read.**
- **The list:** `docs/inventory/nbpromec1-tmc-5d-round-2026-11-11.tsv`, one row per file, `bytes`
  then `path`. The paths are relative to `%USERPROFILE%\tmc-5d-round`.
  - 1916 files, 2,913,480,487 bytes in all.
  - **By folder:**

    | Folder | Files |
    |---|---|
    | `C/`, top level | 18 |
    | `C/__pycache__/` | 5 |
    | `C/idle-waits/` | 16 |
    | `C/runs/` | 1829 |
    | `V/` | 24 |
    | `apks/` | 24 |

  - **`C/runs/` holds 132 launch directories and 8 files of its own.**
    - **131 launches hold the same 12 file names,** `probe.json` and `launchcheck.json` among them.
      115 also hold `sitting-line.txt` and `sitting-extra.json`, and 15 an `idle-wait.txt`. Two hold a
      `calibration.json`, and one a `residual-check.json`.
    - **One, `K5-1`, holds only its `install.txt`.**
    - **The 8 files** are, by their names, the qualification and decision records: `qualify-5d.txt`,
      `qualify-control.txt`, `qualify-R-5d.txt`, `qualify-R-control.txt`, `requal-R-5d.json`,
      `requal-R-control.json`, `decide-M.json` and `negctl-N.json`.
- **Searched beyond it, by file name:**
  - **Under `%USERPROFILE%`, to a depth of seven:** no `probe.json` or `launchcheck.json` outside that
    folder.
  - **The Claude session folders under `%LOCALAPPDATA%\Temp\claude\`, in full:** 11 `probe.json` and
    15 `launchcheck.json`, in folders named `residual-test`, `synth`, `linetest` and `linetest2`.
    By those names they are the tools' test cases, not sittings. They were not opened, and are not
    in the list.
- **Why it is recorded:** the phone holds 211 startup-probe files, most of them from sittings driven
  from this machine. A comparison against the other machine's folder alone would misread them as
  existing only on the phone. This list is the half that was missing.
  - Two files probably answer the comparison directly, and were NOT opened, by the ruling: names and
    sizes only.
    - `dump-name.txt`, one in each of 131 launch directories, by its name the phone-side dump's file
      name.
    - `V/probedir-before.txt`, by its name a listing of the phone's probe directory.
  - Reading them is a later step's, with its own ruling.
  - **Read since, later on 2026-11-11, by the owner's ruling, names and sizes only.** They do list the
    phone's probe files. The comparison is in *Recorded results* (the phone's probe files compared)
    and in `docs/inventory/nbpromec1-probe-compare-2026-11-11.tsv`.
- **Scanned before commit:** the list's paths hold no account name, channel name, token or message
  text. The only "channel" in it is a launch label, `X-excluded-channel-check`.

**2026-11-11, 09:49 to 10:35, on `nbpromec1`: the player below Twitch's documented minimum,
observed; the phone's probe files compared.** These are the owner's rulings. The step only observed:
the player's height was not changed, nothing was installed or uninstalled, and no message was sent.
- **The question.** The proposal says the player cannot shrink, because Twitch documents a minimum of
  400 by 300 and today's 300 dp is that height. But the minimum is also a width. The compact phone
  is 360 dp wide and this phone at its default size is 384, so on both the embed already ran below
  it. If it works there, the minimum is not a rendering wall.
- **The app:** the dev app as installed, `versionName` 5.5.1-dev, not reinstalled. It was cold-launched
  at the step's start and opened on the first account's chat page, whose channel was live. The
  stream was turned on with the stream button, and played muted from the player's own play button.

**Before anything changed, at 09:49.**
- **Read from the phone:**
  - `Physical size: 1080x2400` with no size override, and `display_size_forced` `null`;
  - density 450 physical, 382 override, and `display_density_forced` 382;
  - rotation `lock 0`, `accelerometer_rotation` 0, font scale 1.15;
  - the launcher in front, with neither TMC app running.
- **"Disable permission monitoring" was still off.** A same-value write (`wm density 382`) was
  refused with `SecurityException: Must hold permission android.permission.WRITE_SECURE_SETTINGS`.
  Federico switched it on.
- **Switching it ON took effect without a restart.** At 09:51, with the phone's uptime still 4:02,
  the same write was accepted with `remote exit=0`. Switching it OFF needs a restart, as below and as
  on 2026-11-10.

**The size the embed receives, in CSS px: read, through the player's own statistics.**
- **How it was read.** The app does not enable GeckoView's remote debugging: no
  `remoteDebuggingEnabled` anywhere in the code. So the page could not be inspected from the
  computer. The Twitch player itself offers a read-only display: settings, then *Advanced*, then
  *Video Stats*. That overlay reports a "Viewport Resolution".
  - The toggle was found off each time, and was switched on only to read the overlay.
  - It is not kept across a page load: after each recreation it was off again.
- **The readings,** one for each geometry:

  | Geometry | Density | GeckoView, device px | GeckoView, dp | Viewport Resolution, CSS px | Device px per CSS px | Device px per dp |
  |---|---|---|---|---|---|---|
  | This phone as set, the reference | 382 | 1080 × 716 | 452.4 × 299.9 | 450 × 298 | 2.400 | 2.3875 |
  | This phone at its default size | 450 | 1080 × 844 | 384.0 × 300.1 | 378 × 295 | 2.857 | 2.8125 |
  | Compact phone, 720x1280 | 320 | 720 × 600 | 360.0 × 300.0 | 360 × 300 | 2.000 | 2.000 |

- **So one CSS pixel is within 1.6% of one dp.** Today's 300 dp player gives the embed 298 CSS px of
  height on this phone as set, and 295 at its default size. Both are already below the documented
  300.
- **Why the ratio is not exactly the density: INFERRED.** All three readings fit one rule: device px
  per CSS px = 60 / round(60 / (dpi / 160)). That is Gecko's integer count of app units per device
  pixel, at 60 app units to the CSS pixel. It is inferred from the fit, and was not read in Gecko's
  source in this step.
- **The figures are the player's own.** Twitch prints whole pixels, and does not say what its
  "Viewport Resolution" measures beyond the name. Its ratio, 1.51 at the reference, is the whole
  box's, not the 16:9 video's.

**Below the minimum width, the player plays: no error and no message about its size.** One capture of
each geometry answers the question. They are kept on this machine only, in
`%USERPROFILE%\tmc-5d-round\player-width-2026-11-11\`, because they show the account and the channel.
- **The screenshots:** `12-d450-after-ad.png` and `21-compact-playing.png`. The other 16 in that
  folder show the reference geometry, and how the player's menus were reached.
- **This phone at density 450, 384 dp wide, the embed 378 × 295 CSS px:**
  - the player loaded its paused frame with the channel's overlay;
  - from its play button, it played the channel's advert break first ("Commercial break in
    progress"), then the stream;
  - Auto quality chose 1280 × 720.
- **The compact phone, 360 dp wide, the embed 360 × 300 CSS px:**
  - the same paused frame, then the stream from the play button;
  - again 1280 × 720.
- **At the reference, 450 × 298 CSS px,** the same, at 1920 × 1080.
- **The one visible effect of the width:** the paused overlay's row of buttons runs past the right
  edge at 384 and 360 dp, and "Subscribe" is cut off. Nothing else was seen to differ.
- **Not observed:** a height well below 295 CSS px. The step was forbidden to change the player's
  height, so the embed's behaviour there is still unread.
- **The app, through three density changes:**
  - the activity was recreated each time, in the same process (pid 31752), and the stream was off
    again each time, since `streamEnabled` is not kept;
  - no crash entry was written, and the newest is still 2026-11-10's;
  - with the stream on, the compact phone's list was 0 px and the composer pushed down, as the
    simulation table predicts.

**What Twitch's documentation says below the minimum: nothing.** Read on 2026-11-11 through a fetch,
not recalled.
- **`https://dev.twitch.tv/docs/embed/video-and-clips/`:**
  - "Embedded video windows must be at least 400x300 pixels."
  - The attribute tables give height "Minimum: 300" and width "Minimum: 400", for the iframe, the
    interactive player and clips.
  - The only size-linked consequence on the page is the autoplay one. "Minimum size requirements
    and visibility are necessary for autoplay to begin." Then: "The exception is mobile devices, on
    which video cannot be played without user interaction." TMC sets `autoplay: false`.
- **`https://dev.twitch.tv/docs/embed/`, "Embedded Experiences Requirements":**
  - "The use of any Twitch embeddable experience must comply with the requirements below and the
    Twitch Developer Services Agreement."
  - "Embeds must adhere to the minimum height and width requirements outlined in their respective
    attribute tables."
  - "Twitch may revoke embed usage for domains that are not in compliance."
  - The page states two other consequences. A missing `parent` "will trigger a playback error
    message". An obscured embed may have features "disabled", for example "the ability to send chat
    messages". Neither concerns size.
- **So the minimum is a REQUIREMENT, not a rendering recommendation.**
  - It is phrased as "must".
  - It sits in a list the page ties to the Twitch Developer Services Agreement.
  - Its stated enforcement is revocation for non-compliant domains.
  - No page states what happens to an embed below it.
- **The agreement itself: NOT READ.** Its pages, `https://legal.twitch.com/legal/developer-agreement/`
  and its `/en/` path, returned only the site's navigation to the fetch tool, without the
  agreement's text. Whether the agreement states a size rule, or incorporates the documentation's,
  is unknown.
- **What this means for the proposal, recorded and not decided:**
  - **Technically, the minimum is not a wall on this phone.** The app has been running the embed
    below it in width at 360 and 384 dp, and below it in height on this phone as set and at its
    default size (298 and 295 CSS px), and the embed plays.
  - **By the documentation, it is still a requirement.** The proposal's principle 3 stands as written
    there: going below 300 dp, and how far, is the owner's and Federico's decision. That decision
    now also covers the width the app already uses, which the 300 dp alone never addressed.

**The restore, exactly as this plan records it.**
- **At 10:19,** `wm size reset` and `wm density 382`, each with `exit=0`. The rotation was never
  changed.
- **Read back:** `Physical size: 1080x2400` with no override, density 450 physical and 382 override,
  `display_density_forced` 382, `lock 0`, `accelerometer_rotation` 0, font scale 1.15.
- **The residue, as on 2026-11-10:**
  - `wm size reset` left `display_size_forced` as an empty key where it had been `null`;
  - `settings delete global display_size_forced`, run as a step of its own after that read, printed
    "Deleted 1 rows";
  - it then read `null`.
- **The reference reading, identical:** with the app on its chat page and the stream off, the list
  is 1604 px, 671.8 dp. Then Home, with the launcher in front, as before.
- **"Disable permission monitoring" restored and confirmed.**
  - Federico switched it off. At 10:32, with no restart and an uptime of 4:44, the same-value write
    was still accepted with `remote exit=0`.
  - Federico restarted the phone. At 10:35, under a minute after boot, the same write was refused
    with `SecurityException: Must hold permission android.permission.WRITE_SECURE_SETTINGS`.
  - Every value read then is as it was before the step. The restart ended the dev app's process.

**The phone's probe files compared, by name and size only.**
- **The ruling** opened `V/probedir-before.txt` and the 131 `dump-name.txt` files for their names and
  sizes. What each holds was read first from the code that writes it, `C/run.sh` and
  `V/sitting.sh`. No other field was read.
  - **`dump-name.txt`** holds one line: the base name of the dump `run.sh` copied from the phone's
    `files/startup-probe/<name>.json` into `probe.json`. For a calibration run it also copied
    `<name>-calibration.json` into `calibration.json`.
  - **`V/probedir-before.txt`** is `sitting.sh probedir`'s output: a label, "before", then
    `run-as … ls -la files/startup-probe`. **So it lists the phone's probe files,** as they stood when
    it was written. Its file is dated 2026-11-06 09:21 (+01:00).
- **The listing holds 213 entries:** 211 dumps, plus the probe's `flag` and `latest`.
  - The 211 dumps are 208 `run-<ms>.json` and 3 `run-<ms>-calibration.json`.
  - That is the 211 the owner cites for the phone today. Whether the phone changed since 2026-11-06
    was not read: the phone's directory was not listed in this step.
- **`run.sh` never deletes the phone's dumps.** Its only `rm` removes `flag` and `latest`, although its
  header comment says the dumps are deleted after a checked copy. That is why the phone still holds
  them.
- **The comparison,** in `docs/inventory/nbpromec1-probe-compare-2026-11-11.tsv`. It has one row per
  name, giving the status, the phone's name and bytes, and the local path and bytes:

  | Status | Count | Bytes on the phone | Names dated (local time) |
  |---|---|---|---|
  | Match: same name, same size | 133 dumps: 131 probes, 2 calibrations | 30,805,808 | 2026-11-03 08:57 to 2026-11-05 09:32 |
  | Same name, different size | 0 | | |
  | On the phone only | 78 dumps: 77 probes, 1 calibration; plus `flag` and `latest` | 9,994,153, plus 32 | 2026-11-02 14:36 to 2026-11-05 08:59 |
  | On this machine only | 0 | | |

  - **Every copy on this machine is on the phone, byte for byte in size,** and every one of the 131
    launches with a dump name cites a different dump.
  - **The 78 found only on the phone:**
    - 34 are named on 2026-11-02, before this machine's first copy.
    - 41 are named on 2026-11-04 and 3 on 2026-11-05, among this machine's own. In all, 44 fall
      inside the span of the matched ones.
    - By names and sizes alone, where they were driven from cannot be told. The other machine's
      folder is the other half of the comparison. One launch here, `K5-1`, has no copy and no dump
      name, so its dump, if one was written, would be among them.
- **Nothing was deleted,** here or on the phone.
- **Scanned before commit:** the comparison holds dump names, local launch paths and sizes only. It
  has no account name, channel name, token or message text.

**2026-11-11, on `nbpromec1`: the owner's reading of the player check, recorded as his and OPEN;
Federico's choices; a prototype of variant B built, and photographed on the phone from 11:25 to
11:44.** These are the owner's rulings. This entry chooses no floor, no label and no player height.

**The owner's reading of the player check: HIS, not a finding, and OPEN.** The check is the entry
above. He gives it as follows:
- The embed works below the documented minimum.
- Today's player is already marginally below it in height on the reference phone: 298 and 295 CSS
  px, against 300.
- It is below it in width on any phone narrower than 400 dp, which the app cannot avoid.
- Twitch states the minimum as a requirement tied to its Developer Services Agreement, with
  revocation as the stated consequence. The agreement itself was not read.
- **His recommendation to Federico:** do NOT shrink the player below 300 by design, and bring it to
  a full 300 CSS px where it is 295 to 298 today.
- **The decision is Federico's, and is NOT TAKEN.** The prototype below keeps today's 300 dp.

**Federico's choices for the prototype,** as given:
1. **With the stream on and the keyboard up, the player STAYS.** The list takes what is left, even
   below the floor.
2. **The top of the page follows variant B of the proposal.**
   - The connection status moves into the channel bar: the user name and the channel, with a state
     indicator.
   - The mute control becomes an icon beside it.
   - The "UNMUTE" row and the status row both go.
3. **"START PCG" and the stream button stay text buttons.**

Still his to choose, from the pictures: the floor's row count, and the stream button's labels.

**The prototype: `059bc7b` on `prototype/5d-layout-b`. A PROTOTYPE, NOT FOR MERGING.**
- **Where it is:**
  - a branch off `main-v5` at `9ecb341`, dev flavour only;
  - no pull request into `main-v5`;
  - tag `device-test/5.5.1-dev-8-059bc7b`, pushed and confirmed with `git ls-remote`.
- **The hard invariant: the composer's text field is always entirely on screen.**
  - The page root is `ChatPageLayout`, a `LinearLayout` subclass. It first measures every child
    that never yields.
  - If those alone do not fit, the composer row is drawn over the bars.
- **The allocation: `ChatPagePolicy.allocate`,** a pure function with 13 unit tests. It decides in
  this order:
  1. **The player** gets its 300 dp whole, or is refused.
  2. **The picker** gets 240 dp, or less down to 152, as long as the list keeps its floor.
  3. **The list** gets what is left.
  - **The floor applies against the picker only,** never against the player.
- **The floor is in rows, at the user's font scale.**
  - `ChatRowHeightMeter` measures one emote row offscreen, the way the page draws one: 14 sp text,
    and an emote 1.5 times the text size, aligned to the bottom.
  - The floor is the rows times that height, plus the list's 12 dp bottom padding.
- **A refused panel is held hidden, at no height, until the room comes back.** A one-line message
  says so, once.
  - Whether it should close instead is NOT DECIDED. That is the proposal's open choice.
  - The prototype holds it because that needs no state change from inside a measure pass.
- **The player's height is unchanged:** 300 dp.
- **Variant B's top bar,** in the channel bar after the channel field:
  - the indicator;
  - "@user" and "#channel" on two lines at 12 sp, each cut with an ellipsis at 96 dp;
  - the mute icon, while the stream is on.
  - A problem row below the bar is shown only in the problem state.
- **The dev-only switches, set by broadcast:**
  - the floor's row count;
  - the stream labels: today's, or "Watch stream" and "Hide stream";
  - display-only demo names.
  - The default floor is 3 rows, only so that the prototype has one.

**The indicator: four states, each with its own shape and its own content description.**

| State | Shape | Colour | Content description |
|---|---|---|---|
| Connected | filled circle | green | "Connected" |
| In progress | open ring | amber | "Connecting" |
| Disconnected | hollow circle with a bar | grey | "Disconnected" |
| Problem | triangle with "!" | red | "Connection problem" |

- **The cluster is read as one item,** with today's sentence as its description. So a screen reader
  hears what the status line said before.
- **A problem's words are on screen,** in the problem row.

**Where each of the ten status texts goes.**

| Text | State | Its words |
|---|---|---|
| `status_loading` | In progress | the cluster's description |
| `status_connecting` | In progress | the cluster's description |
| `status_connected` | Connected | the cluster's description |
| `status_disconnected_account` | Disconnected | the cluster's description |
| `status_disconnected` | Disconnected | the cluster's description |
| `account_not_found` | Problem | the problem row |
| `status_missing_token` | Problem | the problem row |
| `status_backend_session_reauthorize`, from both of its writes | Problem | the problem row |
| `status_backend_auth_failed` | Problem | the problem row |
| `status_read_error` | Problem | the problem row |

- **The names** are on screen in every state where they are known.
- **The `mute` and `unmute` strings are removed.** The icon's descriptions are new strings, "Mute
  stream" and "Unmute stream".

**Verified on its tree, on this machine.**
- **`:app:testDevDebugUnitTest`:** 647 tests run, 0 failed, 0 errors, 0 skipped. 13 of them are the
  new `ChatPagePolicyTest`.
- **`:app:lintDevDebug`:** "no new issues", with the 38 errors the existing baseline filters.
  - Its first run found 12 errors, all from this change.
  - They were fixed before the commit.
- **`:app:assembleDevDebug`,** run again after the commit:
  - `BuildConfig.GIT_SHA` is `059bc7b`, with no `+`;
  - the arm64 APK's sha256 is `ad35916f22252eb948cee8a82926a3d48e8178fd918c3d9b391bea5ea34e6aaf`.
- **Not run:** the stable flavour's tests, lint and build. The prototype is for the dev flavour only.

**The device step, 11:25 to 11:44, phone time.**
- **Before anything changed, read at 11:25:**
  - `Physical size: 1080x2400` with no override, and `display_size_forced` null;
  - density 382;
  - rotation `lock 0`, and font scale 1.15.
  - Stable 5.5.1 at sha256 `bf6c22c2…`, and dev 5.5.1-dev at `ba1f88e3…`, as *Test device* in
    `RELEASE-STATE.md` recorded them.
- **"Disable permission monitoring"** was switched on by Federico. At 11:28 the same-value write,
  `wm density 382`, was accepted with `remote exit=0`.
- **The install, over the dev app only:**
  - **The installed dev APK was pulled** with `MSYS_NO_PATHCONV=1` and kept on this machine. Its
    arrival was checked, as its own step, by size and by sha256 against the phone's.
  - **Its signer and the prototype's are the same:** the Android Debug certificate, SHA-256
    `9b3a523b…5c51`.
  - **The prototype's package,** read from the APK, is `com.fs.twitchminichat.dev`.
  - **`adb install -r` at 11:28:47:** `Success`.
  - **Afterwards,** the phone's dev APK read `ad35916f…`, the one built here, and stable was
    unchanged.
  - Nothing was uninstalled.
- **The view photographed:** the dev app's first chat page as it opens, Federico's choice. The
  channel was live.
- **Nothing was typed or sent.** The only inputs were:
  - one swipe, from the accounts page to the first chat page;
  - the stream button;
  - the composer's field, to raise the keyboard;
  - the emote button;
  - Back, with the keyboard up.
- **How it was measured:** frames from `dumpsys activity top`, for the page at the pager's left edge.
  - The page's screen top came from `uiautomator`.
  - The keyboard's top came from the window manager's IME inset source.
  - **`uiautomator`'s bounds were NOT used for heights.** On this phone they clip at the app frame's
    height, 2182 px, not at the page's bottom on screen, 2295. So they show the composer cut off when
    it is not; the screenshot showed it whole.
- **The predictions** are variant B's model:
  - the bars, measured, less 54.03 dp;
  - the player at 300 dp;
  - the picker at 240 dp, down to 152;
  - a floor of n × 28.48 + 12 dp.

**Measured against predicted.** The list is in dp; rows are (list − 12 dp) over the row measured on
that geometry. P is the player and K the picker, in dp. The field was entirely on screen in every
shot.

| Geometry | Shot | Floor, rows | Predicted list | Measured list | Rows | P / K |
|---|---|---|---|---|---|---|
| Reference, 382, page 913.9, bars 188.1 | a, nothing open | any | 725.8 | 725.9 | 25.06 | |
| | b, stream | any | 425.8 | 426.0 | 14.53 | P 299.9 |
| | c, stream and keyboard, page 617.8 | any | 129.7 | 129.8 | 4.14 | P 299.9 |
| | d, picker | 2, 3, 4 | 485.8 | 485.9 at each | 16.64 | K 240.0 |
| | e, stream and picker | 2, 3, 4 | 185.8 | 186.0 at each | 6.11 | P 299.9, K 240.0 |
| | landscape, nothing open, page 405.0 | any | 216.9 | 217.0 | 7.20 | |
| | landscape, stream | any | 216.9, player refused | 217.0, player refused at 0 | 7.20 | P 0 |
| Default size, 450, page 769.1, bars 189.2 | a | any | 579.9 | 579.9 | 19.97 | |
| | b | any | 279.9 | 279.8 | 9.42 | P 300.1 |
| | c, page 514.1 | any | 24.9 | 24.9 | 0.45 | P 300.1 |
| | d | 2, 3, 4 | 339.9 | 339.9 at each | 11.53 | K 240.0 |
| | e | 4 | 125.9, K 154.0 | 125.9 | 4.00 | P 300.1, K 154.0 |
| | e | 3 | 97.4, K 182.5 | 97.4 | 3.00 | P 300.1, K 182.4 |
| | e | 2 | 69.0, K 210.9 | 69.0 | 2.00 | P 300.1, K 210.8 |
| Typical, 420, page 827.0, bars 189.0 | a | any | 638.0 | 638.1 | 21.91 | |
| | b | any | 338.0 | 337.9 | 11.41 | P 300.2 |
| | c, page 535.6 | any | 46.6 | 46.5 | 1.21 | P 300.2 |
| | d | 2, 3, 4 | 398.0 | 398.1 at each | 13.51 | K 240.0 |
| | e | 4 | 125.9, K 212.1 | 126.5 | 4.01 | P 300.2, K 211.4 |
| | e | 3 and 2 | 98.0, K 240 | 97.9 at each | 3.01 | P 300.2, K 240.0 |

- **Where the measurement and the model disagree: nowhere by more than 0.6 dp.**
  - **The largest difference** is the typical phone at floor 4: 126.5 dp against 125.9.
  - **Its cause:** that floor uses the row measured on that geometry, 75 px, which is 28.57 dp. The
    model's row is the reference phone's, 68 px or 28.48 dp.
  - **The row, measured by the prototype on each geometry:**
    - 68 px at 382;
    - 80 px at 450, which is 28.44 dp;
    - 75 px at 420;
    - 57 px at 320, which is 28.5 dp.
  - **Every other difference is 0.2 dp or less,** from rounding whole pixels.
- **The page heights and the bars match the model's inputs exactly,** on all three geometries.
- **What the shots show, beyond the numbers:**
  - **With the keyboard up, the list is below every floor candidate, as Federico chose:** 4.14 rows
    on the reference, 1.21 on the typical phone, and 0.45 at density 450, under one row.
  - **At density 450 the floor does the work:** the picker shrinks to 154.0, 182.4 and 210.8 dp, and
    the list holds 4.00, 3.00 and 2.00 rows.
  - **At 154 dp the picker** shows its search field and one row of emotes.
  - **In landscape the player is refused,** with the message. While the message shows, as a system
    toast, it covers part of the composer's field on the screen. The field itself stays laid out on
    screen.
  - **The labels:** "HIDE STREAM" and "WATCH STREAM" each fit on one line, at 452 dp and at 360 dp.
    The bars' height did not change with any label. The 360 dp pair is an extra, beyond the two
    reference shots asked for.
  - **The channel field's width, measured:**
    - 170.9 dp at 452 dp with the stream off (read after the restore);
    - 131.4 and 83.4 dp at 411 dp, with the stream off and on;
    - 102.8 and 54.8 dp at 384 dp.
    - At 452 dp with the stream on it was not measured: about 123 dp, computed as the mute icon's
      48 dp less.
  - **The long names at 360 dp (720x1280 at 320),** with synthetic 25-character names from the demo
    switch:
    - they show as "@abcdefghij…" and "#wwwwwwww…";
    - the channel field is 70.0 dp with the stream off, and 22.0 dp with it on;
    - at 22 dp it is an underline with no visible hint.
    - That is the proposal's prediction that B does not fit at 360 dp with the stream on, now
      observed.
  - **The status cluster** is 120 to 130 dp wide.
  - **No crash:** the phone's newest app-crash entry is still 2026-11-10 15:36:03's. The activity
    was recreated by each change of density or size, and by each rotation.
- **The switches were left at their defaults:** floor 3, today's labels, no demo names.

**The restore, at 11:44, exactly as this plan records it.**
- **The commands:** `cmd window user-rotation lock 0`, `wm size reset` and `wm density 382`, each
  with exit 0.
- **Read back:**
  - `Physical size: 1080x2400` with no override;
  - density 450 physical and 382 override, and `display_density_forced` 382;
  - `lock 0`, `accelerometer_rotation` 0, and font scale 1.15.
- **The residue:** `display_size_forced` was an empty key. `settings delete global
  display_size_forced`, run as a step of its own after that read, printed "Deleted 1 rows", and
  the key then read null.
- **The reference again:** the page measured as shot a did, a list of 725.9 dp. Then Home, with the
  launcher in front.
- **"Disable permission monitoring": NOT YET RESTORED, and OPEN.**
  - Federico will switch it off and restart the phone later.
  - The refused write that confirms it has not been run.
  - Until it is, the shell can change this phone's secure settings.

**What the phone runs now:** the prototype, as the dev app. `RELEASE-STATE.md`'s *Test device*
records it.

**The pictures stay on this machine,** because they show the account and the channel. They are in
`%USERPROFILE%\tmc-5d-round\prototype-b-2026-11-11\`:
- 39 screenshots;
- `measurements.tsv`;
- the prototype's APK, and the dev APK it replaced.

The two pictures named `00-check-ref-a.png` and `ref382-after-restore-a.png` are the check of the
first reading and the reading after the restore.

**2026-11-12, on `nbpromec1`: the larger layout fix STOPPED by Federico; one small change PROPOSED,
the mute control in the buttons row and the status line tightened. No code was written and the
phone was not touched.** These are the owner's rulings. The layout and the code were read on
`main-v5` at `9ecb341`, and this branch has the same code. Every height after the change is
COMPUTED with the plan's threshold model, not measured. Nothing here chooses a label.
- **The decision is Federico's.** It is recorded in the *Backlog* with his reasons. None of the
  larger fix is built: no floor, no emote picker that shrinks, no player that hides or is refused,
  no status in the channel bar.
- **The prototype, `prototype/5d-layout-b`,** stays as a record and is not developed further.
- **What remains is one small change, a PROPOSAL.**
  - It is written only once Federico agrees.
  - It is not part of 5.6.0 unless he says so.

**The top of the page today, read.** Heights are in dp. The measured column is the reference
phone's, from 2026-11-10, at 2.3875 px/dp and font scale 1.15. The lines are in
`fragment_chat.xml` unless named.

| View | Where | What sets its height | Vertical padding and margin | Measured |
|---|---|---|---|---|
| Channel bar, `channelSwitchBox` | `:12-68` | `wrap_content` (`:15`). The channel field has `minHeight="48dp"` (`:28`). The three icons are 48 × 48 dp (`:35-36`, `:49-50`, `:56-57`) | `padding="8dp"` on every side (`:17`). A top margin equal to the status bar's inset, added by `SystemBarsInsetHelper.keepBelowStatusBar` (`SystemBarsInsetHelper.kt:70-88`, called at `ChatFragment.kt:838`); it lies outside the page height | 153 px, 64.1 dp |
| Buttons row, `topActionsRow` | `:70-98` | `wrap_content` (`:74`). Both buttons have `minHeight="48dp"` (`:86`, `:96`), in `?android:attr/buttonBarButtonStyle` (`:82`, `:91`) | `paddingTop="8dp"` (`:78`), none at the bottom. Horizontally: 8 dp each side (`:76-77`), and the stream button's `layout_marginStart="8dp"` (`:95`) | 134 px, 56.1 dp |
| Status line, `textStatus` | `:100-106` | `wrap_content` (`:103`): one line of bold text, 22.1 dp at this font scale | `padding="12dp"` on every side (`:104`). No margin | 110 px, 46.1 dp |
| Mute row, `streamControlsBox` | `:107-122` | `wrap_content`. Its only child, `btnMuteStream`, is a `Button` with `wrap_content` (`:118-119`). It is 48 dp tall, from the Material button's minimum | `paddingBottom="8dp"` (`:114`), paid with the stream off too. Horizontally: 8 dp each side (`:112-113`) | Stream off: 19 px, 8.0 dp. Stream on: 134 px, 56.1 dp, of which the button is 115 px, 48.2 dp |

- **The layout sets no vertical margin on any of the four.** The only one is the channel bar's top
  margin, which comes from the inset.

**The proposal: two parts.**
1. **The mute control moves into the buttons row.**
   - **With the stream on,** the row holds "START PCG", the stream button, and a mute icon after
     them: three controls in one row.
   - **The icon is an `ImageButton` of 48 × 48 dp.** That gives a 48 dp touch target, the height
     of the text buttons' minimum, so the row does not grow.
   - **Its content description states the action,** from new strings. For example: "Mute stream"
     while there is sound, "Unmute stream" while muted. Federico chooses the words.
     - The prototype already has two such strings, `mute_stream` and `unmute_stream`, and two
       icons, `ic_volume_up_24` and `ic_volume_off_24`, at `059bc7b`.
     - Whether to reuse them is for the code step.
   - **With the stream off,** the icon is gone and the row is as today.
   - **`streamControlsBox` goes with the button,** and its 8 dp of bottom padding with it, in both
     states.
   - **The icon calls what the button calls today,** `toggleStreamMute`
     (`ChatFragment.kt:1857-1859`). Its behaviour does not change. What that call does is read
     below.
2. **The status line moves into the space between the channel bar and the buttons row.**
   - **Every one of its ten texts stays exactly as today,** from the same strings and the same
     writes (2026-11-11, the status line today).
   - **Only the vertical paddings around it are tightened:**
     - the channel bar's bottom padding;
     - the status line's top and bottom padding;
     - the buttons row's top padding.
     - The horizontal paddings stay.
   - **It is not a control, so it needs no 48 dp row.**
     - Its one listener clears the channel field's focus (`ChatFragment.kt:1777`).
     - The page's root carries the same listener (`:1773`).
     - It keeps the listener.
   - **No touch target gets shorter.** The channel field, the three icons, the two text buttons
     and the mute icon all stay 48 dp.

**What it reclaims, in dp of page height, computed.**

| Part | Stream off | Stream on |
|---|---|---|
| 1. The mute row goes, with `streamControlsBox` | 8.0, the padding | 56.1: the button's 48.2 and the padding's 8.0 |
| 2. The paddings around the status line, at most: the channel bar's bottom 8, the status line's 12 and 12, the buttons row's top 8 | 40.0 | 40.0 |
| **Total** | **48.0** | **96.1** |

- **40.0 dp is the most part 2 can take** without making a touch target shorter than 48 dp.
  - It sets all four paddings to 0.
  - The status text then sits directly between two sets of 48 dp boxes: the field and the icons
    above it, and the buttons below it.
- **Any padding kept for spacing comes off the 40 dp one for one.** How close the text may sit is a
  visual question, for Federico, from a screenshot.
- **Not touched:** the status bar's inset above the channel bar, and the channel bar's own top
  padding.

**The labels today, with their string names.** Read in `strings.xml` and in `updateStreamUi`
(`ChatFragment.kt:3492-3509`).

| Control | State | String | Text | Shown as |
|---|---|---|---|---|
| Stream button, `btnToggleStream` | player off | `stream_off` (`strings.xml:12`), also the layout's initial text (`fragment_chat.xml:97`) | "Video ON" | VIDEO ON |
| | player on | `stream_on` (`:13`) | "Video OFF" | VIDEO OFF |
| Mute button, `btnMuteStream` | muted, the default | `unmute` (`:15`), also the layout's initial text (`fragment_chat.xml:120`) | "Unmute" | UNMUTE |
| | not muted | `mute` (`:14`) | "Mute" | MUTE |

- **The stream button's labels name a state:** the one the player is NOT in.
- **The mute button's labels name the action.**

**Why the app has its own mute button: read. It duplicates the player's own control, by reloading
the page, and no reason for it is recorded.**
- **What a tap does:** it calls `toggleStreamMute` (`ChatFragment.kt:3535-3542`).
  - It flips `streamMuted` and updates the label.
  - With the stream on, it RELOADS the player page: `streamSession?.loadUri(buildStreamUrl())`.
  - `buildStreamUrl` (`:3303-3306`) puts the flag in the page's address, as `muted=true` or
    `muted=false`.
  - **It calls nothing in the player.**
- **The page:** `player.html` in `unouidol/ircminichat`, read on 2026-11-12, with `docs/player.html`
  last changed in `4c3a2b1`.
  - It reads `muted` from the address once, when it builds
    `new Twitch.Player(…, { autoplay: false, muted })`.
  - **So each tap is a new load of the embed.** With `autoplay: false`, the reloaded player stands
    paused until the user presses its play button again. That is by reading: no mute reload was run
    in this step.
  - Whether a reload also replays an advert break was not observed.
- **The page already defines `setMutedState` and `toggleMute`.** They call the player's own
  `setMuted`, without a reload. Nothing in the app calls them:
  - no file under `app/src/main` names them;
  - the stream session has no channel to the page: no message delegate and no WebExtension
    (`GeckoSessionManager.kt:1678-1699`, `:2201-2207`).
- **The stream starts muted on every new page.**
  - `streamMuted = true` (`ChatFragment.kt:618`), and the flag is not saved.
  - The page also defaults to muted when the address has no `muted`.
- **Why it starts muted: NOT RECORDED.**
  - The button, the flag and the address parameter arrive together in `d4e1157` (2026-04-04),
    "Add v3 PCG and push notification updates". That commit has no comment on muting.
  - The page arrived on 2026-03-30, in two commits named "Create player.html".
  - **Not autoplay policy.** The player never autoplays (`autoplay: false`). Twitch's documentation
    also says mobile video does not play without user interaction (*Recorded results*,
    2026-11-11). So playback always starts from the user's tap on the play button.
  - **Not audio focus.** Nothing in the app requests or reacts to audio focus for the stream.
    - The only `AudioManager` use is the notification fallback (`MyFirebaseMessagingService.kt`,
      `NotificationAlertFallback.kt`).
    - The runtime is built with content-blocking and cookie settings only, and no media or
      autoplay setting (`GeckoSessionManager.kt:1657-1666`).
  - What remains is a product choice: a chat app should not start sound unasked. That is
    inferred, not recorded.
- **The player's own control.** The embed shows its own mute control; that is the owner's
  statement, and this step did not look at it.
  - That it mutes inside the page, without a reload, and that playback goes on, is inferred from
    the documented `setMuted`. It was not run.
- **The two do not share a state.** The app cannot read the embed's state, so its label follows its
  own flag. By reading, not run:
  - After the embed's own control unmutes, the app's button still reads UNMUTE.
  - A tap on it then reloads the page unmuted. That pauses the stream, changes nothing audible, and
    turns the label to MUTE.
- **The one thing the app's button adds: its flag outlives a reload of the same page.** Every
  later load reads it:
  - the stream turned off and on again (`enableStream`, `:3516`);
  - a channel switch, which reloads the stream when it is on (`ChatChannelBinding.changeTo`,
    `ChatChannelBinding.kt:111`, through `reloadStreamForCurrentChannel`,
    `ChatFragment.kt:3544-3547`).
  - The embed's own choice is lost at each of those reloads, unless Twitch keeps one of its own.
    Whether it does, and whether the `muted` option overrides it, was not read.
- **So it is a duplicate of the player's control in function,** with no recorded reason. It is
  implemented by a reload. What it adds is the carry-over across the page's reloads.
  - **Nothing is removed.**
  - The proposal moves the control and keeps its behaviour, the reload and the separate state
    included.

**A mute control drawn over the player, shown on tap: for information only, not the chosen
option.**
- **What it would take:**
  - The GeckoView is wrapped in a `FrameLayout`, with the icon over one corner. A tap on the player
    shows it, and it hides after a delay.
  - The tap must be seen without being taken from the embed. Either a frame whose
    `onInterceptTouchEvent` notes it and returns `false`, or a touch listener on the GeckoView that
    returns `false`.
  - It reclaims the same 48.2 dp as the row does, and leaves the buttons row as it is.
- **What it risks with the embed's own controls:**
  - **One tap brings up both.** The embed also shows its controls on a tap. Each would run on its
    own timer, so they would appear and fade out of step.
  - **They can overlap.** The embed draws its control bar along its own edges, its own mute among
    them, so an icon in a corner can cover one of the embed's buttons. Which corner is free was not
    read.
  - **Twitch's requirement.** Embeds "should not be obscured in any way by other page elements"
    (*Recorded results*, 2026-11-11). An overlay obscures part of the embed by construction. The
    documented consequence of an obscured embed is that some of its features may be "disabled".
  - **Accessibility.** A screen reader does not find a control that exists only after a tap,
    unless it stays in the accessibility tree while hidden.
  - **It keeps the reload,** unless the page gets a channel to the app. That is the same work as
    calling the page's `setMutedState`.

**The list before and after, with the stream on and the keyboard up: COMPUTED from the plan's
model, not measured.**
- The model gives today's measured lists within 0.3 dp for these rows.
- Each geometry's own measured bars are used: 242.1 dp on the reference phone, 243.0 on the
  typical one.
- Rows are counted as the prototype's record counted them: the list less its 12 dp bottom padding,
  over a 28.5 dp row with an emote.

| Geometry | Page | List today, measured | List after, computed | Rows after | Text field after |
|---|---|---|---|---|---|
| Reference phone, 452 × 1005 dp | 617.8 | 27.6 | 123.7 | 3.9 | on screen |
| Typical phone, 411 × 914 dp | 535.6 | 0.0, the field partly off | 40.6 | 1.0 | on screen |

- **With the stream on and the keyboard down,** the reference phone goes from 323.8 dp to 419.8, and
  the typical phone from 235.8 to 332.0.
- **Every list that is above 0 px gains 48.0 dp with the stream off and 96.1 dp with it on.**

**The thresholds after the change.** In dp of page height, at font scale 1.15, computed. Today's
column repeats the plan's table of 2026-11-10. The last three columns are the reference phone's
margins for the list's 0 px, after the change; bold where it is negative.

| Panels open | List 0 px below, today | List 0 px below, after | Field partly off below, after | Field entirely off below, after | Reference portrait, 914 | Reference with keyboard, 618 | Reference landscape, 405 |
|---|---|---|---|---|---|---|---|
| none | 242 | 194 | 184 | 136 | +720 | +424 | +211 |
| reply bar | 299 | 251 | 241 | 192 | +663 | +367 | +154 |
| picker | 482 | 434 | 424 | 376 | +480 | +184 | **−29** |
| picker, reply bar | 539 | 491 | 481 | 432 | +423 | +127 | **−86** |
| stream | 590 | 494 | 484 | 436 | +420 | +124 | **−89** |
| stream, reply bar | 647 | 551 | 541 | 492 | +363 | +67 | **−146** |
| stream, picker | 830 | 734 | 724 | 676 | +180 | **−116** | **−329** |
| stream, picker, reply bar | 887 | 791 | 781 | 732 | +123 | **−173** | **−386** |

**What this change does NOT fix.** Computed on the same model.
- **The list still has no floor.** It can still reach 0 px, and the composer can still be pushed
  off the screen. The stopped design's hard invariant does not hold.
- **Landscape, reference phone: all six classes at 0 px today stay at 0 px.**
  - The composer's text field stays off screen in five of them.
  - In the sixth, the picker alone, it is partly off, where today it is entirely off.
- **Portrait, reference phone: two of the three classes at 0 px stay.** They are the stream and the
  picker with the keyboard up, with and without the reply bar, and the field stays off screen.
  - The third, the stream and the reply bar with the keyboard up, gets 67.2 dp.
- **The simulated geometries: 7 of the 34 rows stay at 0 px.**
  - The stream, the picker and the keyboard: on this phone at its default size, on the typical
    phone and on the large phone.
  - The stream and the picker: on the compact phone.
  - The picker, alone or with the stream: on the compact phone in landscape.
  - The stream and the picker: on the tablet in landscape, with the field partly off instead of
    off.
- **Tight lists remain, every one under two rows:**
  - 18.9 dp on this phone at its default size, with the stream and the keyboard: 0.24 of a row;
  - 33.9 dp there with the stream and the picker;
  - 40.6 dp on the typical phone with the stream and the keyboard;
  - 45.0 dp on the compact phone with the stream.
  - A two-line message is 97 px on the reference phone, 40.6 dp. It is taller than the first two,
    about as tall as the third, and only 4.4 dp shorter than the fourth. So U2/U3 stays routine
    there.
- **The status line still wraps with long names,** as today, adding 22.1 dp for each extra line.
  Tightening its paddings does not stop the wrap.
- **The mute control keeps its behaviour.** A tap still reloads the player, which then stands
  paused, and its state can still disagree with the embed's own.
- **The stream button's labels stay as they are** until Federico chooses.
- **The player keeps its 300 dp.** It stays below Twitch's documented 400 in width wherever the page
  is under 400 dp wide, and at 295 to 298 CSS px in height (*Recorded results*, 2026-11-11). The
  owner's recommendation on it stays his and open.
- **It reopens the instance list.** It changes the height of children of the page's
  `LinearLayout`, which by this plan's rule reopens the list (*Recorded results*, the instance list
  closed). The code step measures the page again on the phone.

**Where these controls are named.** Searched on 2026-11-12 in:
- every tracked file of this branch;
- `unouidol/tmc-site` at `0fa3ec0`;
- `unouidol/unouidol.github.io` at `bcfbe2a`;
- the notes of the 13 published releases.

**What the search found:**
- **Strings,** all in `app/src/main/res/values/strings.xml`, the app's only `strings.xml`:
  - the stream button: `stream_off`, "Video ON" (`:12`), and `stream_on`, "Video OFF" (`:13`);
  - the mute button: `mute`, "Mute" (`:14`), and `unmute`, "Unmute" (`:15`);
  - the status line: `status_disconnected` (`:4`), `account_not_found` (`:17`), `status_connecting`
    (`:18`), `status_loading` (`:19`), `status_connected` (`:20`), `status_missing_token` (`:21`),
    `status_backend_session_reauthorize` (`:22`), `status_backend_auth_failed` (`:23`),
    `status_read_error` (`:24`), `status_send_error` (`:25`, written nowhere) and
    `status_disconnected_account` (`:26`).
  - `app/lint-baseline.xml:171-172` names `status_send_error`, as unused.
- **Code:**
  - `fragment_chat.xml:89-97`, `:100-106` and `:107-122`;
  - `ChatFragment.kt`: the bindings (`:182`, `:245-246`, `:1495`, `:1530-1531`), the listeners
    (`:1777`, `:1853-1859`), `updateStreamUi` (`:3492-3509`), and the status writes this plan
    lists (2026-11-11, the status line today).
- **Tests: none.** Nothing in `app/src/test` or `app/src/androidTest` names `btnToggleStream`,
  `btnMuteStream`, `streamControlsBox`, `textStatus` or any of these strings.
  - **A different "status line" is named elsewhere:** the one under a pending outgoing message.
    `PendingEchoView`, `ChatRowViewBinder`, `PendingEchoStatus`, the dev harness and their tests
    (`ChatTimelineEchoTest`, `PendingEchoStatusTest`) name it, and so does
    `docs/manual-plan-5c-emotes.md:291-293`. That is another view, and this change leaves it alone.
- **Manual-plan steps, all in this plan:**
  - **Section M, the hands-on pass, case 8 (TalkBack):** `textStatus` is one of the four views
    carrying `clearChannelFocusClickListener`. This change keeps that listener, so the step stands.
  - ***Recorded results*, 2026-11-09, the one read that decides the merge:** the same four views.
  - **The records:** the *Backlog*'s entry and its table of children, the device step of
    2026-11-10, the thresholds, the design proposal of 2026-11-11 and the prototype's record. They
    name the mute button, the action buttons, the status line and the stream button.
    - They stay as written, because they describe the layout as it was measured.
- **`RELEASE-STATE.md`:** its *Test device* section describes the prototype's chat page: "the
  status sits in the channel bar, the mute control is an icon, and the page shares its height at
  measure time".
  - It describes the build on the phone, which has not changed, so it stays.
- **`AGENTS.md`, the README, the bundled assets and both sites:** none.
- **Release notes:** none names the stream button, the mute button or the status line. 5.5.1's
  "with TMC's alerts muted" is about notification alerts.

**The stream button's label is Federico's choice. None is chosen here.** These are the candidates
the plan records (2026-11-11, the design proposal). The widths are measured in Roboto Regular at
16.1 dp, so they are lower bounds.

| Off / on | Widths |
|---|---|
| WATCH STREAM / HIDE STREAM, the owner's example, built into the prototype as a switch | 123.1 / 102.3 dp |
| SHOW STREAM / HIDE STREAM | 113.2 / 102.3 dp |
| START STREAM / STOP STREAM | 115.9 / 107.1 dp |
| OPEN PLAYER / CLOSE PLAYER | 103.9 / 111.0 dp |
| Today: VIDEO ON / VIDEO OFF | 72.0 / 78.3 dp |

- **The design proposal's two notes:** START STREAM / STOP STREAM is the closest in form to START
  PCG, and a user who streams may read "stop stream" as ending a broadcast.
- **What the icon changes for them.** Only the "on" label shares the row with the icon.
  - With the icon, each text button has (W − 72) / 2 dp, less its own horizontal padding: 144.0 at
    360 dp of width, 156.0 at 384, 169.5 at 411 and 190.0 at 452.
  - Without the icon it has (W − 24) / 2: 168.0 to 214.0 at the same widths.
  - That assumes no margin before the icon. An 8 dp margin takes 4 dp from each button.
- **The longest "on" label is CLOSE PLAYER, at 111.0 dp.** START PCG is 84.8.
  - The button's own padding is still unresolved, as the design proposal recorded. So whether each
    label fits on one line at 360 dp is for the screenshot.
  - A label that wraps makes the row taller.
  - The prototype saw HIDE STREAM fit at 360 dp in a row without the icon: 168 dp per button, not
    144.

**What the code step would owe, if Federico agrees.** Nothing is written. This is recorded so
that it is not derived again.
- **The change:** `fragment_chat.xml`, two strings, and one or two icons. `updateStreamUi` sets
  the icon, its description and its visibility, where it set the button's text.
- **No unit test reaches a layout.** The check is on the phone.
- **On the phone:**
  - The top of the page is measured on the reference phone and on the typical geometry: with the
    stream off, with it on, and with it on and the keyboard up. Each list is compared with the
    numbers above, within the model's 1.1 dp.
  - The icon's frame is read from `dumpsys activity top`, and must be 48 × 48 dp.
  - Its content description is read in both states.
  - One screenshot of each, for Federico, kept outside the repository.
  - The restore as this plan records it.
- **Section M's outside-click step stands,** since `textStatus` keeps its listener.
- **The instance list, reopened by the change,** is closed again by those measurements.

**Scanned before commit:** this entry holds no account name, channel name, token or message text.
The names it quotes are view ids, string names, commit identifiers and file paths.
