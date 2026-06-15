# 077 — CI failure + performance overhaul (wave 1)

**Prompt:** "check still lots of test cases are failing … I think it's code issue" (run 27485736625).

## Diagnosis: it is CODE, not an app bug

Forensic read of run `27485736625` (the run the user flagged) settles the
bug-vs-code question definitively:

- **Assets P6** failed 24 tests. **18 of them failed at *exactly* `6m 0s`** —
  the signature of the `GlobalTestTimeout` per-test cap killing a *hung* test,
  not an assertion failing. Example `MOT_AST_03_verifySelectionOfLowVoltageMachine200hpOrLess`:
  the test **successfully opened the subtype picker and clicked the subtype
  button** ("✅ Clicked subtype button after scroll"), then could not locate the
  option label `Low-Voltage Machine (<= 200hp)` (`accessibilityId failed →
  mobile:scroll failed`), ran out of strategies, and **blocked on the implicit
  wait for ~3 more minutes** until the 360 s cap killed it. The app was fine; our
  locator for an option with `(<= 200hp)` in the label did not match. Classic
  automation defect.
- **Connections "94 tests in 8m50s" is a FALSE GREEN** — the suite XML has been
  `enabled="false"` since 2026-05-28 (v1.36 hid the Connection tab). It ran
  **zero** tests, Surefire exited 0, and the job advertised "94 tests" green.
- **Site Visit phase2/phase3 + Location part2** ran the full **5h50m** wall —
  i.e. were cancelled by the 6 h job cap: still hanging.
- Every "Run X Tests" step carries `continue-on-error: true` (46×), so **no
  test failure can ever turn a job red** — the only thing that reddened jobs
  was an unrelated artifact-upload error.

Underlying causes confirmed by a 12-agent analysis (adversarially verified):
per-test driver quit+recreate (11–30 s/test) forcing a full UI login per test;
1,276 raw `findElement` calls in locator cascades each burning the 5 s implicit
wait per miss (fast helpers had **zero adoption in 7 of 8 page objects**); dead
strategies that can never match on v1.36+ (e.g. `clickEdit` burns a guaranteed
12 s × 43 call-sites); a busy-spin poll that emitted 150 k log lines in one job;
and a PEP-668 pip break that silently killed the failed-suite collector and all
client emails.

## Wave-1 changes

### CI / workflow (false-green + infra)
- **`test-result-gate.sh` (NEW):** parses `testng-results.xml`; fails the job if
  the report is missing, `failed>0`, or `(total-skipped)==0` (mass-skip /
  disabled suite). Wired AFTER artifact-upload + email in all 18 module jobs.
  No more silent green on 0 tests. `continue-on-error` kept on the *test* step
  (so reports/emails still emit) but **not** on the gate.
- **PEP-668 pip fix:** every `pip install` across all workflow files now tries
  `--break-system-packages` first, plain-pip fallback — restores the failed-suite
  collector and per-module client emails on macos-15 runners.
- **Connections re-enabled:** suite XML `enabled="true"`, removed
  `Connections_Test`'s class-level `@Test(enabled=false)`. App is v1.43; the
  Connection tab is back and TC_CONN_004/059 pass locally.
- **WDA pre-build dead code:** `find … WebDriverAgent.xcodeproj -type f` →
  `-type d` (a `.xcodeproj` is a directory). The pre-build step was a 2 s no-op
  and the real 6-min `xcodebuild` was landing inside the first test session
  (~60–90 min/run wasted across 16 macOS jobs).
- `mvn test -q` → `-ntp -DfailIfNoTests=true` on all module jobs so per-test
  output streams to CI logs (the only forensic evidence on runner death) and a
  zero-test suite fails at the Maven level too.
- `timeout-minutes` floored to 360 on all test jobs (standing 6 h rule).
- `send-module-email.py`: 0-tests-ran now produces a loud (still professional)
  subject instead of a soft "NO DATA".

### Core framework (recovery + speed)
- `BaseTest` `@BeforeMethod` → `@BeforeMethod(alwaysRun=true)` — a single config
  failure no longer permanently skips setup (run 27320962984 threw away 65 tests
  this way). Lets `DriverManager.initDriver()`'s dead-session recovery actually run.
- **`DeadSessionCircuitBreaker` (NEW listener):** after N=5 consecutive
  driver-dead outcomes it trips OPEN and skips the rest of the run fast, handing
  off to `failed-suites/` + `rerun-failed-by-date.yml`. Healthy outcomes reset
  the count, so ordinary flake never trips it. Gated by `DEAD_SESSION_BREAKER`.
- **Session keep-alive (`KEEP_SESSION_ALIVE`, default on):** `testTeardown` skips
  the per-test driver quit when the test passed and the session is healthy (the
  `@BeforeMethod` soft restart already isolates app state). Keeps the existing
  quit-on-failure / quit-on-dead paths. Strictly safer than per-test kill w.r.t.
  the page-object cached-driver rule. ~150 min/run.
- Real `Thread.sleep(250)` between `waitForCondition` probes (the old no-op
  `sleep()` made polling loops busy-spin — 95.6 % of a 32 MB log).
- WDA prebuilt-consumption plumbing in `DriverManager` (`USE_PREBUILT_WDA`,
  `WDA_DERIVED_DATA_PATH`), env/-D overridable, off by default locally.

### Page objects (cheap locator cascades)
- `IssuePage`: sort helpers + `scrollToTopOfDetails` (was 0/28 hits at ~53 s
  each = 24 min/job) rebuilt on `mobile: scroll` + `existsNow`/`withImplicitWait(0)`
  probes (75 fast-probe usages added); `tapFirstIssue` returns boolean so callers
  `skipIfPreconditionMissing` instead of acting on nothing.
- `AssetPage`: site-context recovery as step 0 of `navigateToAssetList` (the
  ~240-min Assets P1 root cause — app never entered a site), un-swallowable
  `VerificationError` on nav failure, cache-poison rejection of site-screen
  labels ("Create New Site"), `clickEdit` reordered to probe v1.36 reality first
  (58 fast-probe usages added).
- `SiteSelectionPage`: `noReset=true` for the suite, `waitForDashboardReady` /
  `waitForSiteListReady` / `checkSelectSiteScreenElements` rebuilt on 0-implicit
  probes (~110 min/job).
- `LoginPage`: popup/keyboard/T&C cascades under `withImplicitWait(0)` with a
  winning-strategy cache; absence probes via `existsNow` (~20 min/job).

### Issues seeding + security
- `TestDataApi`: `createIssue` / `getIssueByTitle` / `searchIssues` /
  `getAssetByName` (the E2E wave needs `getAssetByName`). `Issue_Phase1_Test`
  seeds a known issue so its 26 tests stop passing vacuously against an empty list.
  Cleanup config methods guarded by `isDriverActive()` + `runWithBudget("issues-cleanup",30,…)`
  — kills the 195-min zombie skip-loop.
- `TC_SEC_009`: polls (cap ~90 s) for login-screen-or-dashboard before
  `verifyNotBlank` (the invalid-login round trip is ~61 s on CI — the old
  `mediumWait()` raced it). `UIStateValidator.assertNotBlank` now polls 10 s and
  treats a visible activity indicator as "still loading", not "blank".

## Validation
- `mvn -o -DskipTests test-compile` — clean.
- `testng-verify-selftest.xml`: **21/21 green** (8 → 21; added circuit-breaker
  exact-N trip/reset/throwable-chain, `runWithBudget` hang-bound, polling
  `assertNotBlank`). Proven to go **RED on an injected defect** (disabling the
  breaker trip → 3 self-tests fail), GREEN when correct — per the working agreement.

## Known follow-ups
- The result-gate will surface the ~177 known pass-anyway failures that
  `continue-on-error` was masking — first gated run will be noisier on purpose.
- Subtype/class **option-label** selection (`tapAssetClassItem`, the actual
  Assets P6 6-min hang) is hardened separately in wave 2.
