＃ 045 — Pass-Anyway Quality Gate: Stop The Bleeding On Vacuous Tests

**Date**: 2026-04-30
**Time**: ~12:30 UTC
**Prompt**: "go with your recommendation" (after I flagged that 30% of existing tests have no assertions)
**Decision**: Implement Option 2 (CI gate against new pass-anyway tests). Defer Option 3 (mass-quarantine of 373 existing) for staged remediation.

---

## What Triggered This Work

You shared a local run of `TC_ISS_180_verifySaveChangesForUltrasonicAnomaly`. The log showed:

> Save Changes button NOT FOUND (3 times)
> ⚠️ Save Changes button not found for Ultrasonic Anomaly
> ✅ Test PASSED

That's a **vacuous test**: it exercised the app, observed the wrong state, and STILL reported pass. I scanned the codebase to find out how widespread this is.

---

## The Finding

| Scope | Number |
|---|---:|
| Total `@Test` methods in `src/test/java/com/egalvanic/tests/` | **1,252** |
| Methods with at least one assertion / fail / throw / SkipException | 877 |
| **Methods with NO failing condition (pass-anyway)** | **375 (29.96%)** |

### Distribution by file (worst → best)

| File | Total | No-assert | % Vacuous |
|---|---:|---:|---:|
| **S3BucketPolicyDriftTest** | 42 | 40 | **95.2%** 🚨 |
| **Asset_Phase6_Test** | 114 | 94 | **82.5%** 🚨 |
| Issue_Phase2_Test | 60 | 42 | 70.0% |
| AuthenticationTest (pre-Part-A) | 38 | 30 | 78.9% |
| Asset_Phase5_Test | 112 | 50 | 44.6% |
| Asset_Phase1_Test | 112 | 44 | 39.3% |
| Asset_Phase2_Test | 108 | 39 | 36.1% |
| Asset_Phase3_Test | 109 | 15 | 13.8% |
| Asset_Phase4_Test | 97 | 12 | 12.4% |
| Connections_Test | 97 | 3 | 3.1% |
| SiteSelectionTest | 52 | 2 | 3.8% |
| LocationTest | 81 | 1 | 1.2% |
| Issue_Phase1_Test | 130 | 1 | 0.8% |
| **Issue_Phase3_Test** | 58 | 0 | **0%** ✅ |
| **OfflineTest** | 34 | 0 | **0%** ✅ |

### Why Issue_Phase3 and Offline are clean

These two test classes use `assertTrue(...)` consistently and use `SkipException` for data-dependent skips. They're the model the rest of the codebase should follow.

### What "no assertion" means in detection

The script flags a test if its body contains NONE of:

- `assertTrue(`, `assertFalse(`, `assertEquals(`, `assertNotNull(`, `assertNull(`, `assertSame(`, `assertNotSame(`
- `fail(`, `Assert.fail(`
- `throw new ` (any exception)
- `SkipException` (clean skip)
- `logFail(`, `logFailure(`, `logCriticalFailure(`, `reportTestFailure(`

If a test has only `logStep(...)` and `logStepWithScreenshot(...)`, it's vacuous: TestNG sees no thrown exception and reports PASS regardless of product behavior.

---

## What This Means For Pass-Rate Reports

Recall the consolidated report at `testcase_file/CI-Report-Run-24876293380/Consolidated_Client_Report.html` showing **99.6% pass rate**. With this finding:

- **At most 70% of the product is actually being verified** (the 877 tests with assertions)
- **The other 30% (375 tests) cannot fail** regardless of what the app does
- A "1,297 / 1,302 passed" result includes ~370 vacuous passes that don't represent any product check
- Real coverage signal is closer to 877 actually-checking tests — and the 27 "manually verified" overrides included some that this scan would've flagged anyway

This isn't a bug per se — it's a **systemic test-quality issue** that has been growing over time as new tests were added with the same template-pattern as the old ones.

---

## What I Built In This Turn (Option 2 — Stop The Bleeding)

### 1. The check script

**File**: [`scripts/check_assertion_coverage.py`](../../scripts/check_assertion_coverage.py)

What it does:
- Walks `src/test/java/com/egalvanic/tests/*Test*.java`
- Extracts every `@Test public void X()` method
- Greps the method body for assertions / fail / throw / SkipException
- Compares findings against a **baseline** (the 375 grandfathered tests)
- Exit non-zero (when called with `--strict`) if a NEW pass-anyway test is found

How to run:

```bash
# Read-only report (exits 0)
python3 scripts/check_assertion_coverage.py

# CI gate — fails on regressions
python3 scripts/check_assertion_coverage.py --strict

# Regenerate baseline (rare — only after deliberately accepting an exception)
python3 scripts/check_assertion_coverage.py --update-baseline
```

### 2. The baseline file

**File**: [`scripts/assertion-baseline.txt`](../../scripts/assertion-baseline.txt)

Contains exactly **375 lines** in the format `FileName.java::testMethodName`. These are the currently-known pass-anyway tests, grandfathered. The intent is the file shrinks over time as legitimate assertions are added — never grows.

If you commit a NEW `@Test` method without an assertion, the CI gate fails. Two ways out:
1. **Add an assertion** that ties the PASS to a real product behavior (the right answer)
2. **Add the test ID to the baseline** (with a comment explaining why) — for genuinely setup-style helpers

### 3. CI gate wired into the smoke workflow

[`.github/workflows/ios-tests-smoke.yml`](../../.github/workflows/ios-tests-smoke.yml) now includes a step:

```yaml
- name: Assertion Coverage Check (no NEW pass-anyway tests)
  run: python3 scripts/check_assertion_coverage.py --strict
```

It runs immediately after `actions/checkout@v4` so the gate fires fast — before any iOS build/test work begins. A regression breaks the build in <5 seconds, not after a 12-minute test run.

---

## What This Does NOT Do (deferred Option 3)

I did NOT mass-quarantine the existing 375 with `@Test(enabled = false)`. Three reasons:

1. **375 file edits is risky** — would touch every Asset/Issue/S3 test class. Scope of error introduction high.
2. **Sudden visible drop** in pass count from 1,252 to 877 would alarm anyone reading the next CI report without context. Better to triage these one-by-one.
3. **Some genuinely don't need to fail** — e.g., a test that documents a UI flow but has no specific check is more useful as a "smoke probe" than as `@Test(enabled = false)`. Quarantine without per-test review is too blunt.

The right next step is **staged remediation**:

1. Triage **S3BucketPolicyDriftTest (40 vacuous of 42)** first — it's the most important suite for production safety. Each S3 drift test should `assertTrue(driftDetected, ...)` or similar.
2. Triage **Asset_Phase6** next (94 vacuous) — high test count, lots of regression value.
3. Then Asset_Phase5, Issue_Phase2, AuthenticationTest pre-Part-A.

Each triage commit removes entries from `assertion-baseline.txt`. The baseline file is the visible scorecard — when it reaches 0, we've eliminated all pass-anyway tests.

---

## Concrete Example — TC_ISS_180

The test you ran locally. Current code:

```java
boolean saveVisible = issuePage.isSaveChangesButtonDisplayed();
logStep("Save Changes button visible: " + saveVisible);

if (noRequired && saveVisible) {
    logStep("✅ TC_ISS_180 PASSED: ...");
} else if (saveVisible) {
    logStep("ℹ️ Save Changes visible but 'No required fields' not confirmed");
} else {
    logStep("⚠️ Save Changes button not found for Ultrasonic Anomaly");
}
```

There's no `assertTrue(saveVisible, "Save Changes should be visible...")`. The test logs three different states then ends. Cannot fail.

**Fix in one line**:

```java
assertTrue(saveVisible,
    "Save Changes button should be visible for Ultrasonic Anomaly issue");
```

After that, the test:
- ✅ Genuinely passes when Save Changes is visible (the spec says it should be)
- ❌ FAILS in CI when Save Changes isn't visible (which is what your local run showed → real bug?)
- The extra logSteps stay as debugging context

---

## CI Status (in-flight at time of writing)

Run [#25150966937](https://github.com/Egalvanic/eg-pz-mobile-iOS/actions/runs/25150966937) (authentication-only, with the new T&C tests from changelog 043) — still in progress. Watcher running in background.

This run was dispatched BEFORE the new gate was added. The next run will include the gate.

---

## Files Changed In This Turn

| File | Change |
|---|---|
| `scripts/check_assertion_coverage.py` | NEW — 145-line Python script with `--strict` and `--update-baseline` modes |
| `scripts/assertion-baseline.txt` | NEW — 375 grandfathered test IDs + 6 header comment lines |
| `.github/workflows/ios-tests-smoke.yml` | +1 step (Assertion Coverage Check) right after checkout |
| `docs/ai-features-changelog/045-pass-anyway-quality-gate.md` | This document |

---

## Honest Disclosure

This change does NOT improve any existing test's quality. It just stops things from getting worse. The 30% vacuous tests still cannot fail.

What I'd recommend after this commit:
1. **Triage S3 Drift first** — 40 tests, 1–2 days of focused work
2. **Triage Asset_Phase6** — 94 tests, 3–4 days
3. By the end of next sprint, target getting under 10% vacuous (down from 30%)
4. Make this metric visible: add to the consolidated report a "Real Coverage" stat = (tests with assertions) / (total tests)

Want me to start triaging S3 Drift in the next session? It's high-impact and well-bounded.
