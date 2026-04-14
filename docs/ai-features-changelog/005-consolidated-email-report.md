# 005 — Consolidated Email Report for Smoke Workflow

**Date:** 2026-04-14
**Scope:** CI/CD Email Reports
**Files Changed:**
- `.github/workflows/ios-tests-smoke-repodeveloper.yml`
- `src/main/java/com/egalvanic/utils/ExtentReportManager.java`

---

## Problem

The smoke workflow (`ios-tests-smoke-repodeveloper.yml`) had **no email reporting** at all. When running multiple modules sequentially (e.g., `all` suite runs 14 `run_mvn` calls), there was no consolidated email sent to stakeholders with test results.

The parallel workflow (`ios-tests-repodeveloper-parallel.yml`) already had a consolidated email step (Job 17: "Summary & Email") — this was used as the reference implementation.

Additionally, if `SEND_EMAIL_ENABLED` were ever re-enabled in Java code, each `run_mvn` call would trigger a separate email from `ExtentReportManager.flushReports()` → `sendReportEmail()`, since each call creates a new JVM with its own `@BeforeSuite`/`@AfterSuite` lifecycle.

## Root Cause

1. **Smoke workflow missing email step** — workflow ended after artifact upload with no email notification
2. **No CI-level email suppression** — Java `flushReports()` had no mechanism for CI to say "don't send email, the workflow will handle it"

## Solution

### 1. Added Consolidated Email Step to Smoke Workflow (Steps 10)

Two new steps added after artifact upload, before cleanup:

- **"Build Consolidated Email"** — Collects test results from either:
  - `SMOKE_*` env vars (set by `smoke-dashboard.sh` for smoke suite)
  - `testng-results.xml` parsing (for individual/full suites)
  
  Generates an HTML email body (`email_body.html`) with:
  - Branch, suite name, device info, triggered-by, run link
  - Color-coded status (green ✅ / red ❌)
  - Pass/fail/skip counts table with optional duration row
  - Suite-specific display names (e.g., "Smoke CRUD (7 Modules — 17 Tests)")

- **"Send Consolidated Email"** — Uses `dawidd6/action-send-mail@v3` with:
  - SMTP Gmail (`smtp.gmail.com:465`, TLS)
  - Credentials from `EMAIL_USERNAME`, `EMAIL_PASSWORD`, `EMAIL_TO` secrets
  - Subject line: `[STATUS] iOS Suite (branch) — X/Y Passed`
  - `continue-on-error: true` so email failure doesn't fail the workflow

### 2. Added `-DSEND_EMAIL_ON_FLUSH=false` to `run_mvn`

The `run_mvn()` shell function now passes `-DSEND_EMAIL_ON_FLUSH=false` as a system property. This tells the Java code to skip per-module email sending even if `SEND_EMAIL_ENABLED` is later set to `true`.

### 3. Java Guard in `ExtentReportManager.flushReports()`

Added a check for the `SEND_EMAIL_ON_FLUSH` system property:
```java
boolean emailSuppressedByCI = "false".equalsIgnoreCase(System.getProperty("SEND_EMAIL_ON_FLUSH"));
if (AppConstants.SEND_EMAIL_ENABLED && !emailSuppressedByCI) {
    sendReportEmail();
}
```

This ensures:
- **CI runs**: `-DSEND_EMAIL_ON_FLUSH=false` → no per-module email → one consolidated workflow email
- **Local runs**: No system property set → `emailSuppressedByCI = false` → behaves as before (respects `SEND_EMAIL_ENABLED`)

## Architecture: Why Two Layers of Email Control?

| Layer | Control | Purpose |
|-------|---------|---------|
| `AppConstants.SEND_EMAIL_ENABLED` | Compile-time constant | Master on/off for Java email |
| `-DSEND_EMAIL_ON_FLUSH` | Runtime system property | CI override to suppress per-module emails |
| Workflow email step | GitHub Actions | Single consolidated email after all modules |

This three-layer approach means:
- Turning `SEND_EMAIL_ENABLED` back to `true` for local dev won't cause CI email spam
- The workflow always sends exactly ONE email regardless of how many modules ran

## No Changes to Parallel Workflow

The parallel workflow already had a proper consolidated email (Job 17). Each parallel job runs one module on a separate runner, and the summary job collects all results into one email. No modifications needed.
