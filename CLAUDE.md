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

## How to run
- Self-tests (no device): `mvn -o -DsuiteXmlFile=testng-verify-selftest.xml test`
- Exploratory crawl (macOS + live session): `RUN_EXPLORATORY=true mvn -Dtest=ExploratoryCrawlTest test`
- CI: GitHub Actions `ios-tests-parallel.yml` (workflow_dispatch; `job_selection` incl.
  `workorder-planning-only`, `location-only`). Feature work branch: `claude/lucid-clarke-gbUOa`.
