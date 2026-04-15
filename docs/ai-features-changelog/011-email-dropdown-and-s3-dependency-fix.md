# Changelog 011 — Email Dropdown in Workflows & S3 Test Dependency Fix

**Date**: 2026-04-15
**Prompt**: Add email send dropdown to GitHub Actions workflows; fix S3 TestNG dependsOnMethods error

---

## Changes Made

### 1. Email Dropdown in Developer Repo Workflows (release/prod)

**Problem**: Java-level `SEND_EMAIL_ENABLED = true` fires `sendReportEmail()` inside `ExtentReportManager.flushReports()`. Smoke dashboard runs 7 separate Maven processes (one per module), each with its own JVM lifecycle — resulting in 3+ duplicate emails per smoke run instead of 1.

**Root cause chain**:
- `ExtentReportManager.flushReports()` → `sendReportEmail()` → `EmailUtil.sendTestReportEmail()`
- Each of the 7 Maven processes calls `flushReports()` independently
- No deduplication mechanism exists at the Java level

**Fix**:
- Set `SEND_EMAIL_ENABLED = false` in `AppConstants.java` (Java-level email permanently disabled)
- Added `send_email` workflow_dispatch dropdown (yes/no, default: no) to both workflows on `release/prod`:
  - `ios-tests-smoke-repodeveloper.yml` — full new email step with report discovery + dawidd6/action-send-mail@v3
  - `ios-tests-repodeveloper-parallel.yml` — conditioned existing email step on dropdown value
- Workflow-level email fires exactly once after all modules complete

**PR**: Egalvanic/eg-pz-mobile-iOS#190 (branch: `feature/add-email-dropdown-prod` → `release/prod`)

**Also created**: PR #189 against `main` branch with same changes for `run-qa-automation.yml` and `Automation-Test-Parallel.yml`

### 2. S3BucketPolicyDriftTest — Remove dependsOnMethods

**Problem**: Running individual S3 tests from VS Code IDE caused `TestNGException`:
```
TC_SMOKE_10_devSldsPolicy() is depending on method TC_SMOKE_02_verifyBaselineFilesExist(),
which is not annotated with @Test or not included
```

**Root cause**: `dependsOnMethods` requires dependency methods to be in the same test execution. When running a single test from IDE, only that method is included — its dependencies are absent.

**Fix**:
- Removed all 41 `dependsOnMethods` annotations across the file
- Added `@BeforeClass` precondition checks with static flags:
  - `awsAccessVerified` — set by `S3PolicyChecker.verifyAwsAccess()`
  - `baselinesVerified` — set by `S3PolicyChecker.verifyBaselinesExist()`
  - `preconditionError` — captures failure reason
- Added `ensurePreconditions()` helper that throws `SkipException` if preconditions failed
- Called `ensurePreconditions()` at top of `checkBucketPolicy()` helper (covers all bucket tests)
- Individual tests now runnable from IDE — they skip gracefully if AWS access or baselines unavailable

**File**: `src/test/java/com/egalvanic/tests/S3BucketPolicyDriftTest.java`

### 3. IssuePage — Case-insensitive Issue Class Picker

**Problem**: `tryOpenIssueClassPicker()` used `name CONTAINS 'Issue Class'` which could fail if iOS renders the label with different casing.

**Fix**: Changed to `name CONTAINS[c] 'issue class'` for case-insensitive matching.

**File**: `src/main/java/com/egalvanic/pages/IssuePage.java` (line 5168)

---

## Commits (QA Automation Repo)

| Commit | Description |
|--------|-------------|
| 0a827ac | fix: Case-insensitive Issue Class picker predicate |
| 25ca662 | chore: Enable SEND_EMAIL_ENABLED for smoke test verification |
| bf7ada0 | fix: Disable Java-level SEND_EMAIL_ENABLED to prevent duplicate emails |
| a3c1f27 | fix: Replace dependsOnMethods with @BeforeClass precondition checks in S3BucketPolicyDriftTest |
| 422775e | docs: Add changelog 010 for comprehensive smoke test audit |

## Architectural Insight

**Email architecture decision**: The correct layer for email in CI/CD is the **workflow orchestrator** (GitHub Actions), not the test framework (TestNG/ExtentReports). The workflow has global visibility — it knows when all modules are done and can aggregate results. Individual Maven processes only see their own module's results.

**SkipException pattern vs dependsOnMethods**: `SkipException` is more IDE-friendly because preconditions are evaluated at runtime by `@BeforeClass`, not at test-plan-build time by TestNG's dependency resolver. Tests can be run individually, in any subset, or all together — the behavior is the same.
