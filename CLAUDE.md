# CLAUDE.md — working notes for this repo

## Working agreement (standing preference — do not re-ask)
- When asked to harden / extend the test framework, **implement the full set of changes
  without asking for permission** (create files, wire them in, rewrite pilots). Proceed
  end-to-end; don't stop to confirm scope.
- **Always validate that the implementation is actually useful**, not just present:
  1. `mvn -o -DskipTests test-compile` — it must compile.
  2. Run the driver-free self-tests (`testng-verify-selftest.xml`) — verifier logic must go
     RED on injected defects and GREEN when correct.
  3. Validate device-dependent parts on the macOS CI runners (dispatch the relevant module).
- Record notable decisions/architecture here so future sessions start fast.

## What this repo is
Java 17 (build) / Appium 8.5.1 / Selenium 4.11 / TestNG 7.10 / ExtentReports — XCUITest
automation for the eGalvanic "Z Platform" iOS app. `app-source/` is the app under test
(ignore it for framework work). ~1,672 `@Test` across 21 classes, 8 page objects, 11 modules.

Key files: `src/main/java/com/egalvanic/{base/BasePage, utils/DriverManager, constants/AppConstants}`,
`src/test/java/com/egalvanic/base/BaseTest.java` (driver lifecycle, assert wrappers @1114,
`skipIfPreconditionMissing` @1316, single `@AfterMethod testTeardown`). Driver: singleton via
`DriverManager.getDriver()`; bundle id `AppConstants.APP_BUNDLE_ID`.

## Why the suite was green but found no bugs (diagnosis)
Asserts DO throw, but the booleans fed to them are pre-digested: page objects swallow
exceptions and `return false`/safe defaults (~1,096 silent catches), asserts are weak
(2,026 `assertTrue` vs 14 `assertEquals`; 129 `assertTrue(..||..)`; tautologies like
`count >= 0`), and `skipIfPreconditionMissing` (118×) converts missing/erroring state to
SKIP. Also: 132 `Thread.sleep`; `sleep()/shortWait()` are `WebDriverWait.until(d->true)`
no-ops; `queryAppState` only used in teardown; no asset-render / state-integrity checks;
locators are predicate-heavy (2,944 predicate vs 257 accessibility-id).

## Hardening layer (added)
- `com.egalvanic.verify`: `CrashDetector`, `AssetLoadVerifier` (+ driver-free `ImageAnalysis`),
  `UIStateValidator`, `StateIntegrityChecker`, `VerificationError` (extends AssertionError so
  it can't be swallowed by `catch (Exception)`).
- `BaseTest` entry points: `verifyAppAlive(step)`, `verifyNotBlank(screen)`, `verifyNoErrorAlert()`,
  `guard(step)`; plus an automatic teardown crash-assert (`failIfAppCrashedDuringSuccessfulTest`)
  that fails a "passing" test if the app is dead at the end.
- `com.egalvanic.explore`: autonomous crawler (`ExploratoryEngine` + `Crawler`, `ActionSelector`,
  `Oracle`, `InterruptInjector`, `VisualAnomalyDetector`, `WorkflowGraph`, `AiClient`).
  AI is opt-in via `ANTHROPIC_API_KEY`; with no key it is a verifier-backed monkey.
- Pilot: `WorkOrderPlanning_Test` hardened (tautology removed, crash/blank guards, new
  `TC_WOP_015` integrity check).

## Session 2026-06-11 — speed + SLD bug audit + security tests (branch `claude/ios-automation-testing-optimization-53umv7`)
- **SLD source audit:** 8 confirmed product bugs (SLD-1..8, incl. 2 HIGH data-loss
  + 2 security) + 7 suspicions — full evidence in `docs/sld-bug-audit-2026-06-11.md`,
  summary in `BUGS.md` Category 3. Note: there is no literal "SLDv3" in code; the
  current stack is SLDV2 entities + Views layer + React Flow webview (detail views
  are `*ViewV3`).
- **Speed (framework):** XCUITest settings in `DriverManager` —
  `animationCoolOffTimeout=0`, `customSnapshotTimeout=10`, `snapshotMaxDepth=40`
  (all env/-D overridable via `AppConstants`, e.g. `SNAPSHOT_MAX_DEPTH=50` to roll
  back); fixed implicit-wait leak in `BaseTest.waitForAppReadyFast` (each failed
  probe burned the 5s implicit wait → up to 20s/poll); new `utils/Waits` (condition
  polling) + `BasePage.existsNow/isElementDisplayed(By,sec)/isElementGone/withImplicitWait`
  for fast absence checks — use these instead of new Thread.sleep / implicit-wait burns.
- **Speed (CI):** all iOS workflows — post-boot fixed `sleep 45` → `simctl bootstatus -b`
  (blocks only as long as boot needs), Appium-ready pad 10s→3s, WDA warm-up soak
  10s→3s, retry backoff 30s→10s (~45-60s saved per job × 16 jobs/run). New
  `static-checks.yml`: ubuntu fast gate (compile + verifier self-tests, ~3 min)
  before any macOS spend; assertion-coverage job included as informational.
- **Known red flag:** `scripts/check_assertion_coverage.py --strict` FAILS on this
  tree — 177 NEW pass-anyway tests beyond the 291 baseline (mostly Connections/
  Issue/SiteVisit). Burn-down needed before flipping the gate to blocking.
- **New tests:** `Security_EdgeCase_Test` (TC_SEC_001-010; injection/XSS/oversize/
  unicode/whitespace/double-tap/backgrounding, all hard-asserted) wired into
  `parallel/testng-auth.xml` + `testng.xml` (runs in the `authentication-only` CI job).

## Session 2026-07-06 — run-28666174784 forensics + sharded rerun
- **Why "fails became skips" after rerun:** single rerun job ran 404 fails
  sequentially, hit SUITE_WALL_MINUTES=240 at test 119, breaker opened, 285
  fast-skipped; merge_rerun then let rerun-SKIP override original FAIL
  (404→"83 fail + 989 skip"). Full evidence:
  `docs/failure-analysis-2026-07-04-run-28666174784.md`.
- **Fixes (changelog 105):** rerun sharded ×3 in BOTH ios-tests-parallel.yml and
  rerun-failed-by-date.yml via `build-failed-suite.py --shard I/N` (+ new
  `--input-suite`; deterministic class-atomic split, no coordination needed);
  `-DSUITE_WALL_MINUTES=330` for rerun shards; `RERUN_LOGIN_FIRST=true` →
  BaseTest.testSetup runs idempotent loginAndSelectSite() before every rerun
  test (exempt: AuthenticationTest, Security_EdgeCase_Test); merge_rerun in both
  report scripts keeps FAIL when rerun says SKIP (grey NOT RERUN badge).
- **Biggest unfixed cluster:** v1.48 changed the Issues create/details DOM —
  pickers read '' after selection, option lists read 0 items, Gallery/Delete
  "missing" (~76 fails, deterministic, reproduced on fresh sim). Needs an
  Issues remap like commit 8090737 did for SiteVisit. Location New-Floor/New-
  Room nav + CB_EAD "save not confirmed" are the next two clusters.

## How to run
- Self-tests (no device): `mvn -o -DsuiteXmlFile=testng-verify-selftest.xml test`
- Exploratory crawl (macOS + live session): `RUN_EXPLORATORY=true mvn -Dtest=ExploratoryCrawlTest test`
- CI: GitHub Actions `ios-tests-parallel.yml` (workflow_dispatch; `job_selection` incl.
  `workorder-planning-only`, `location-only`). Feature work branch: `claude/lucid-clarke-gbUOa`.
