# The long session's duplicate rows: what is predicted, written before its first poll

Written on 2026-11-06, before the first poll of the long session in
`docs/manual-plan-5d-recyclerview.md`, section M, by the instruction of the owner of the round:
"Pre-register the duplicate prediction. Hash and push it BEFORE the first poll." The session has not
started. Nothing here changes section M's criteria, except item 7, which draws the line between this
question and section M's own "duplicated" criterion.

Everything below is read from code or reasoned, unless it says otherwise. Nothing was run on the
phone for it.

## 1. The prediction, as the owner gave it

Past 800 rows, duplicates are possible after a reconnect or a refresh.

## 2. The mechanism, by reading this branch at `f5e4123`

- **The deduplicator keeps the 800 most recent `id:` keys** (`ChatMessageDeduplicator.kt:80`).
  - It is a `LinkedHashMap` in access order (`:15`), but it is asked with `containsKey` (`:46`),
    which does not count as an access. So a key leaves once 800 newer keys have been inserted, however
    often it suppressed a copy meanwhile. That `containsKey` is no access is the Java documentation's,
    not seen run.
  - Keys of any other prefix, `hist:` and `live:`, are never kept. They are suppressed only when they
    repeat the previous key within 1.5 s.
- **It is the only guard.** Its one caller is `appendChatLine` (`ChatFragment.kt:3644`), for live and
  history rows alike. The timeline checks no id: `ChatTimeline` orders rows by the message's
  timestamp, then by insertion (`ChatTimeline.kt:10`).
- **Live and history rows of one message share their key,** `id:` and the message's id
  (`ChatFragment.kt:2260` and `:2970`), provided the backend relays Twitch's message id.
  - **The owner read that it does,** in the backend clone: `history_endpoint_service.py:157`
    serializes `"id": message.message_id`, and `chat_history_store.py:184` selects `msg_id`, mapped at
    `:209`. The backend is not in this repository, and this was not read here.
  - **The caveat, the owner's:** `msg_id` was added by a migration (`chat_history_store.py:85`), so
    rows stored before it relay `"id": null`. The app then keys them `hist:` (`ChatFragment.kt:2972`),
    which the deduplicator does not keep. True of old history, not of current rows.
- **A channel change clears the deduplicator with the timeline** (`ChatChannelBinding.kt:103`). A
  reconnect or a refresh clears neither.
- **So a duplicate row needs one condition:** a backfill returns a message whose row is still shown
  but whose `id:` key has left the deduplicator. That is a message older than the 800 keys most
  recently inserted.

## 3. How far back a backfill reaches, by reading `HistoryBackfillPolicy.kt`

- **First connect:** 3600 s, once per page (`:233-234`).
- **Reconnect and resume:** the seconds offline, away, since the last message shown or since the last
  backfill, plus 10, within 30 and 3600 (`historyWindowSeconds`, `:571-572`).
- **Manual refresh:** 120 s (`:207`, `:369`).
- **Any of them is widened to a window still owed,** when an earlier request failed: back to the
  oldest moment not yet recovered, at most 3600 s (`:659-684`, `:489-490`).
- **The response is at most 400 rows,** the history source's cap, which is the backend's (section M).
  Not read:
  - which 400 a window holding more returns, the oldest or the newest;
  - in which order they come;
  - whether anything outside the requested window comes back.

## 4. What the reading predicts for this session: the owner's condition is needed, not enough

This is the agent's arithmetic, not a measurement.

- **Past 800 rows is when keys start leaving:** 400 history rows, then 400 live ones. At the planned
  12.3 rows a minute, that is about 33 minutes in.
- **The boundary at a steady rate.** With a steady rate r, and keys inserted in the order of their
  messages, the 800 kept keys are the last 800 messages. They span 800/r minutes: 65 minutes at 12.3.
  The widest window, 3600 s, holds about 738 messages at that rate. **So no window can reach a key
  that left while r stays below 13.3 a minute,** 800 an hour.
- **A short window cannot reach one at any plausible rate.** A refresh's 120 s, or a reconnect's few
  seconds plus 10, reaches only keys inserted in that span. They are among the kept 800 unless more
  than 800 messages came in it, which for a refresh is above 400 a minute.
- **Two ways the boundary comes closer,** both needing a long window:
  - **a burst** packs the kept 800 into less time. 169 rows in one minute were recorded on 2026-11-06,
    of unknown origin.
  - **history inserted newest first.** If a response comes newest first, its newest messages are
    inserted first and leave first. Once eviction reaches the history, from about 33 minutes in, a
    gap opens just before the page started. Any window wider than the time since the start reaches
    it, so wider than about 33 minutes.
- **So the reading predicts no duplicate in this session,** unless a backfill's window is wider than
  about 30 minutes, or the rate rises far above the plan's. While the connection holds, a window that
  wide comes only from a window still owed after a failed request, or from a long disconnect.
- **The owner's prediction and this one differ.** Both are recorded, and the session's result is read
  against both.

## 5. What is recorded

- **From the journal, at every poll,** by the session's tooling (`sitting.sh`, sha256 `1120fb03…`, and
  `journal_extract.py`, sha256 `34313d2f…`, outside this repository, section M):
  - each `backfill.triggered`'s `source` and moment;
  - each `backfill.result`'s `messageCount`, `firstTs`, `lastTs` and moment;
  - each long-press's `shownRows` and moment.
- **The backfills past 800 rows:** how many, by source, counted from the `backfill.triggered` lines
  after the first long-press reading 800 or more. Each one's `firstTs` is set against the boundary of
  section 4, estimated from the long-presses' counts and moments.
- **How a duplicate is identified, by eye.**
  - **What it is:** two rows with the same user and the same text, adjacent or a few rows apart,
    inside the `[firstTs, lastTs]` of a backfill that came before them. They stay so after scrolling
    away and back.
  - **Why they sit together:** the timeline orders by timestamp. Whether a live row and its history
    copy carry the same timestamp was not read, so "adjacent" is not required.
  - **Its false positive:** one user sending the same text twice. The rows show no id, so by eye the
    two cannot be told apart. An eye count is reported as candidate duplicates, never as a count of
    duplicates.
- **How many.**
  - **An exact count** needs the messages' ids, which the app neither shows nor journals. Adding them
    is a code change, outside 5.6.0.
  - **Without one,** a count is possible only across a backfill bracketed by two long-presses with
    nothing else between. The history rows it added are the change in `shownRows`, less the live rows
    that arrived meanwhile. When the connection held through its window, every one of them is a
    duplicate.
  - **Only a deliberate refresh can be bracketed** (item 9).
- **The row count at the first duplicate:** `shownRows` at the last long-press before the backfill
  that brought it, and at the first after. With them, that backfill's source, `messageCount`,
  `firstTs` and `lastTs`.

## 6. What no duplicate would mean

- **Not exercised:** no backfill past 800 rows. The session then says nothing on this question.
- **Exercised only by windows that reach no key that left.** Windows reach back seconds to minutes,
  and eviction removes the earliest-inserted keys first. So every key a short window fetches again is
  still kept. Then no duplicate is what section 4 predicts, and the defect, if it is one, was not
  reached: it needs a wider window.
- **The backend's side, not read:** a capped response that keeps the newest rows, or one that never
  goes outside the requested window, would also give none.
- **So no duplicate does not clear the deduplicator.** It is not evidence that the defect is absent.
- **What would falsify the reading:** a duplicate after a backfill whose window reached no key that
  left, or any duplicate before 800 rows.
  - **Its cause is then not eviction.** One such cause is a key mismatch: a current row relayed
    without an id, keyed `hist:` on the history side while its live row is keyed `id:`.
  - **It is reported as found,** beside the owner's caveat.

## 7. Its status, and where it stops

- **A candidate pre-existing defect, not a 5d regression.** Executed on 2026-11-06, by `git rev-parse`
  of each path in both trees:
  - `ChatMessageDeduplicator.kt`, `HistoryBackfillPolicy.kt`, `BackendHistoryClient.kt` and
    `ChatChannelBinding.kt` have the same blobs on `main-v5` (`98f235a`) and on this branch
    (`f5e4123`): `4532635`, `711eb3e`, `75b05d6` and `693562b`;
  - the lines of `ChatFragment.kt` that build the keys and call the deduplicator are the same, line
    numbers aside.
- **Not in 5.6.0's scope.**
- **The line with section M's "a row ... duplicated", the agent's.**
  - **That failure criterion stays for duplicates 5d can cause.** A row bound twice, or bound to
    another row's content, changes when it is scrolled away and back. No backfill needs to precede it,
    and `shownRows` does not count it.
  - **A duplicate in the timeline's data belongs here instead.** It persists, follows a backfill, and
    is counted. It is recorded here, not as a 5d failure.

## 8. Beside the missing cap

- **The timeline has no cap** (section M, "The app sets no cap").
- **The owner's reasoning, recorded as reasoning:** a cap near 1,000 would keep the timeline and the
  deduplicator's capacity consistent. That is plausibly why Chatterino2 sits there.
  - It is not Chatterino2's stated reason.
  - Whether Chatterino2 deduplicates against its buffer was not read.
- **The agent's precision:** consistency needs the cap at or below the deduplicator's capacity. Then
  every row shown has its key kept, and no backfill can insert a second copy. At today's 800, a cap at
  1,000 would leave up to 200 shown rows without a kept key. So a cap near 1,000 needs the capacity
  raised with it.

## 9. Proposed by the agent, and not part of the session unless the owner adds it: one deliberate refresh

- **When:** once, at 1,000 rows or more.
- **What:**
  - a long-press, one tap on refresh, about ten seconds' wait, and a second long-press, with nothing
    else touched;
  - the tap is one user-triggered action, and a refresh clears nothing (section M's reading hazards);
  - the bracket counts only if the journal shows that refresh's `backfill.result` between the two
    long-presses.
- **What it gives:** the history rows the refresh added. That is the change in `shownRows`, less the
  live rows of the bracket, taken as the session's rate times its length.
- **Predicted, by section 4:** about 0. The 120 s window holds about 25 messages at 12.3 a minute, all
  of them kept keys.
- **By the owner's prediction,** it can reach the refresh's `messageCount`.
