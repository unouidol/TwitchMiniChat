# The overhead launches, resumed: an addendum to `overhead-prereg-2026-11-05.md`, before the resumed launches

Written on 2026-11-05 at about 09:30 +0100, before any resumed launch. The owner of the measurement
instructed the resumption: "retry the failed overhead launches, the phone is free and the memory
should be freed". The pre-registration (sha256 `90c4f18c…`) stands. Its expectation, its prediction,
its checks and its report do not change. This file changes only the order and the driver.

## What happened to the first sitting, 08:51 to 08:59

- **`X-O-excluded`** (`dea38cc`, `full`) gave EXCLUDED_OK, with both pages on `c8cb0e91` at 400 rows.
  - Its idle wait saw input in 14 consecutive 20 s windows, 08:51:28 to 08:56:12, before a quiet one.
  - The launch itself recorded 242 input events. An excluded launch counts for nothing.
- **`Kc-1`** (`dea38cc`, `full calibrate`) was CLEAN, and its `calibration.json` was copied.
- **`K5-1`** was interrupted.
  - Claude Code stopped the sitting's tracked shell for low memory. The driver's processes went on.
  - The agent stopped `overhead.sh` at 08:59:14, and `K5-1`'s chain at 08:59:27. By then its
    `run.sh` was inside its own `adb install`, before the force-stop and the start.
  - No launch of `K5-1` took place. `runs/K5-1/` holds only `install.txt`, without an end time.
    `idle-waits/K5-1.txt` holds its idle wait. Both are kept.
- **No probe-off launch ran.**

## The resumption

- **Kept and counted as before:** `Kc-1`.
- **The order, in one new sitting:**
  - `X-O2-excluded` (`dea38cc`, `full`) counts for nothing. Every sitting of the section begins with
    one excluded launch, and this one comes after a pause of about half an hour.
  - `K5-1-r1` (`a2f12f1`, `full calibrate`) is `K5-1`'s launch, run in its place. The label is the
    re-run form, because `K5-1` exists and the driver refuses an existing label.
    - This uses one of the slot's three re-runs, although `K5-1` was interrupted rather than void.
    - The report's `counted()` takes the slot's CLEAN launch among `K5-1` and `K5-1-r1` to `-r3`.
  - Then `Oc-off-1`, `O5-off-1`, … `Oc-off-5`, `O5-off-5`, alternating, control first, as
    pre-registered.
- **The driver, `overhead-one.sh`:** `overhead.sh` with its order loop replaced by one launch per
  call.
  - Its `launch()` function is `overhead.sh`'s, line for line (compared with `diff`).
  - Each launch runs as a foreground command of its own, so that none can be left orphaned.
  - The re-run rule is the same, applied between calls from the exit status: CLEAN goes on,
    VOID_RERUN re-runs the slot in its place as `-r1` to `-r3`, and anything else stops.
  - sha256 `6b680d75450b5219d3d46409b3a638820553e381bc3e7c9837be98c34c6d2251`.
- **`decide.py` run again** takes the counted launches: `Kc-1`, the counted `K5-1` launch and the 10
  probe-off launches, 12 in all, as pre-registered.
- **The section's state:**
  - Read before this sitting into `section-O2-before.txt`, and after its last launch into
    `section-O-after.txt`.
  - `section-O-after-interrupted.txt` holds the read made at 09:01, after the stop.
