# 096 — Fix 6h job cancellations + web-styled client report + two-report rerun flow

**Date:** 2026-06-29
**Trigger run:** [28246433532](https://github.com/eg-abhiyantsingh/iOS_automation_site/actions/runs/28246433532)
(Site Visit phase2/3, Assets P1/P5, Rerun all CANCELLED at the 6h cap)

Three deliverables in one prompt:
1. Fix **why** Asset P1/P5 + Site Visit phase2/3 (and others) take so long they can't finish.
2. Make the **client report UI identical to the web client report**.
3. **Rerun** the collected failures and deliver **two** client reports — before rerun and after.

---

## 1. Root cause of the 6h cancellations (confirmed from the Assets P1 log)

Once WebDriverAgent wedges/dies on a giant SwiftUI DOM, it **cannot be rebuilt on that
runner**. Every `@BeforeMethod` `DriverManager.initDriver()` then spends **~6 minutes**
failing: `Request timeout … after 90000 ms` → `useNewWDA` rebuild → caller retry. The
`DeadSessionCircuitBreaker` *did* trip, but it only short-circuits the `@Test` body — the
`@BeforeMethod` init thrash still ran for every remaining test. Proof from the log:
consecutive **SKIPPED** tests were ~6 min apart (`CAP_EAD_22` at 243m51s, `CAP_EAD_23` at
250m0s). With 50+ tests left after the trip → 300+ min → blew the 360-min wall clock →
**job CANCELLED**, which truncated the report.

### Fix — containment (new `com.egalvanic.utils.RunHealth` gate)
- **`RunHealth`** — one shared gate read by BaseTest, DriverManager and the breaker. Three
  fast-skip triggers: `breakerOpen`, `wdaHopeless` (K consecutive init failures), and a
  per-suite **wall-clock backstop** (`SUITE_WALL_MINUTES`, default 240, < the 6h job cap).
- **`BaseTest.testSetup`** now checks `RunHealth.shouldFastSkip()` at the very top. When the
  run is doomed it nulls the driver and throws `SkipException` in ~0s — **no initDriver, no
  WDA rebuild**. This is THE fix: a fully-wedged 112-test job now finishes in ~20–30 min
  (a few ×6-min inits until `wdaHopeless` trips, then instant skips) instead of being
  cancelled at 6h. Teardown makes zero Appium HTTP calls on a fast-skip.
- **`DriverManager.initDriver`** — fast-fails in <1s once `wdaHopeless`; feeds
  `RunHealth.recordInitFailure()/Success()` (trips after `WDA_HOPELESS_AFTER`, default 4).
- **`DeadSessionCircuitBreaker`** — now also trips on **wedge** signatures
  (`Could not start a new session`, `Request timeout`, `WebDriverAgent`, `TimeoutException`),
  not just hard-death; and flips the shared `RunHealth` gate when it trips so `@BeforeMethod`
  bails too. (A wedged-but-alive WDA keeps `getSessionId()` non-null, so the old death-only
  signatures never matched — that's why wedged runners burned to 6h.)
- New constants (`AppConstants`): `WDA_HOPELESS_AFTER=4`, `SUITE_WALL_MINUTES=240`
  (env/-D overridable). The 6h job `timeout-minutes` is unchanged (per standing CI rule);
  the suite now self-terminates well before it.

### Validation
- `mvn -o test-compile` — clean.
- `testng-verify-selftest.xml` — **26/26 green** incl. new `RunHealthSelfTest` (5 cases).
- **Red-on-defect proven:** stripping the wedge signatures from the breaker makes
  `breaker_trips_onWedgeSignature_withLiveProbe` FAIL; restoring them → green.

## 2. Web-styled client report — `.github/scripts/ios_client_report.py`

New generator that produces the **same UI as the eGalvanic web client report** (ported from
`Egalvanic_Web-main/.github/scripts/client-report.py`): gradient cover banner, pass-rate SVG
ring, key-stat cards, **Feature Area Coverage** table (CI modules rolled up into client
areas — Asset Management, Issue Tracking, Site Visits, …), **Functional Issues** grouped by
area, **Verifications Passed**; print/PDF-friendly. Reads the authoritative
`testng-results.xml` (no HTML scraping). Replaces the raw ExtentReports Spark theme and the
old purple inline-python `Consolidated_Client_Report.html` as the client deliverable.

## 3. Two-report rerun flow (before + after)

- **`rerun-failures`** job now `needs:` all 18 module jobs (was parallel) and, instead of a
  stale static suite, **builds the failed suite from THIS run** via
  `build-failed-suite.py --from <downloaded artifacts> --include-skips`, then reruns it on a
  **fresh simulator** (clean WDA — the only thing that recovers a wedged runner's tests).
- **`build-failed-suite.py --include-skips`** — also reruns **cascade-skipped** tests
  (breaker / WDA-hopeless / wedge), identified by skip message; **excludes** precondition
  skips so they don't re-skip.
- **`send-email`** now generates **two** client reports with the same template:
  `Client_Report_Before_Rerun` and `Client_Report_After_Rerun` (rerun outcomes override
  originals; the after-report adds a **Rerun Recovery** banner: N recovered / M still
  failing). Both are attached to the email and uploaded as the `client-reports` artifact.

### Validation
- Workflow YAML parses (22 jobs); `rerun-failures` needs 18 jobs + `always() && run_all`;
  `send-email` needs `rerun-failures`.
- Generator rendered from synthetic module + rerun artifacts: Before = 57.6% (cascade skips),
  After = 93.5% (178 recovered, 32 still failing). Browser screenshot confirms the layout
  matches the web report.
- `--include-skips` classification unit-checked: breaker/RunHealth/wall/wedge → rerun;
  precondition/CI-skip/assertion → not.

## Files
- **New:** `src/main/java/com/egalvanic/utils/RunHealth.java`,
  `src/test/java/com/egalvanic/verify/RunHealthSelfTest.java`,
  `.github/scripts/ios_client_report.py`
- **Changed:** `AppConstants.java`, `DriverManager.java`, `BaseTest.java`,
  `DeadSessionCircuitBreaker.java`, `testng-verify-selftest.xml`,
  `.github/scripts/build-failed-suite.py`, `.github/workflows/ios-tests-parallel.yml`

## Tuning knobs (env / -D)
`WDA_HOPELESS_AFTER` (4), `SUITE_WALL_MINUTES` (240), `DEAD_SESSION_BREAKER_N` (5),
`DEAD_SESSION_BREAKER` (true).
