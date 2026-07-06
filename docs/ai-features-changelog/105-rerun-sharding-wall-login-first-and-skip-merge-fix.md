# 105 — Rerun sharding ×3, full-budget wall, login-first reruns, and the FAIL→SKIP merge fix

**Date:** 2026-07-06
**Trigger:** User asked (a) why the July 3–4 run (28666174784) showed "lots of skips
after rerun — even fails became skips", (b) for an in-depth failure analysis, and
(c) to fix it: raise the rerun wall toward the 6-hour job budget, shard the rerun
2–3 ways, and make rerun tests log in first.

## What the forensics showed (run 28666174784, head `b13921b`)

- Main run: 1686 tests — 578 pass / **404 fail / 704 skip**.
- The single "Rerun Failures (this run)" job re-ran the 404 fails sequentially on
  one simulator. After 4h02m it hit `SUITE_WALL_MINUTES=240` (test 119/404),
  driver rebuilds stopped, the DeadSessionCircuitBreaker opened, and the
  remaining **285 tests were fast-skipped**.
- `merge_rerun` (both report generators) let the rerun outcome override
  unconditionally, so 285 original **FAILs were re-labelled SKIP** in the
  After-Rerun report: 404 fails → "83 fails + 989 skips" looked like recovery
  but was mostly verdict loss.
- 27 of the 83 real rerun failures were one signature:
  `navigateToAssetList: could not reach the Asset List (assetsTab=false,
  sitePicker=false)` — mixed-class rerun suites start each class on whatever
  screen the previous class died on.

Full analysis: `docs/failure-analysis-2026-07-04-run-28666174784.md`.

## Changes

1. **Rerun sharded ×3** (`ios-tests-parallel.yml` job `rerun-failures`, and
   `rerun-failed-by-date.yml` job `rerun`): `strategy.matrix.shard: [1,2,3]`.
   Each shard independently derives the SAME deterministic, class-atomic,
   size-balanced split via new `build-failed-suite.py --shard I/N`
   (greedy bin-packing over classes sorted by size desc — no coordination
   artifact). Verified on `failed-2026-07-02.xml`: 167 → 56/56/55, zero
   overlap, exact partition, classes never split.
   - New `--input-suite <xml>` mode re-shards an EXISTING dated suite
     (used by rerun-failed-by-date; the input is copied aside first because
     `latest.xml` can be both input and output).
   - Shard mode skips `history.md` writes (collector owns real history).
   - Artifacts renamed `failures-rerun-report-shard{1..3}` /
     `rerun-<label>-shard{1..3}-report` (upload-artifact@v4 forbids duplicate
     names across matrix jobs).
2. **Full-budget wall for rerun shards:** `-DSUITE_WALL_MINUTES=330` on both
   rerun mvn invocations. 330, not 360: GitHub hard-cancels at
   `timeout-minutes: 360` and a cancelled job loses its artifacts — 30 min of
   headroom keeps report upload alive (the user asked for "6 hours"; this IS
   the 6-hour budget minus the upload tax, and each shard is ~1/3 the work).
3. **Login-first reruns:** new `AppConstants.RERUN_LOGIN_FIRST` (env/-D, default
   false), set `true` by both rerun workflows. `BaseTest.testSetup` now calls
   the idempotent `loginAndSelectSite()` (≈1s fast path when already on
   Dashboard) before every rerun test, so every test starts logged-in +
   site-selected + on Dashboard. `AuthenticationTest` and
   `Security_EdgeCase_Test` are exempt (they exercise logged-out states).
   Login failure logs a warning and lets the test fail on its own real error —
   it never converts the test into a config-skip.
4. **FAIL→SKIP laundering fix:** in `consolidated-report.py` and
   `ios_client_report.py` `merge_rerun`, a rerun SKIP no longer overrides a
   non-SKIP original. The original outcome stands and the detailed report
   badges it grey **NOT RERUN**. Merge log line now reports
   `N kept original outcome (rerun never reached them)`.
5. **send-email job** gathers `failures-rerun-report*` shard artifacts into one
   `rerun-results/` dir (new "Gather rerun shard results" step) and passes that
   to both report generators; old single-artifact name still tolerated.

## Validation

- `mvn -o -DskipTests test-compile` — clean.
- `mvn -o -DsuiteXmlFile=testng-verify-selftest.xml test` — 26/26 green.
- Shard splitter dry-run on the real 2026-07-02 suite — exact 3-way partition
  (56/56/55), zero method/class overlap, union == input.
- Both workflow YAMLs parse (`yaml.safe_load`).

## Expected effect on the next full run

- Rerun capacity: 1 runner × 240 min → 3 runners × 330 min (~4× wall budget);
  a 404-test failure day fits with room to spare.
- The ~27-per-run "stranded navigation" rerun failures should disappear
  (login-first).
- After-Rerun reports can no longer show fewer failures by skipping them —
  un-rerun tests keep their FAIL with a NOT RERUN badge.
