# Changelog 014 — Fix Per-Module Emails: Consolidate into One Email Report

**Date**: 2026-04-15  
**Time**: ~21:00 IST  
**Prompt**: Fix parallel workflow sending separate email per module instead of one consolidated email  
**CI Run**: https://github.com/Egalvanic/eg-pz-mobile-iOS/actions/runs/24449855754

---

## Summary

Fixed the parallel test workflow to send **ONE consolidated email** instead of separate emails per module. Two changes:

1. **Java**: Made `SEND_EMAIL_ENABLED` environment-variable-driven (default `false`) — prevents per-module emails
2. **Workflow**: Enhanced `send-email` job to build a consolidated HTML report from TestNG XML results and attach all client reports to a single email

---

## Problem

When running the parallel test suite (17 jobs), the client was receiving **17 separate emails** — one from each module. This happened because:

### Architecture (Before Fix)

```
Parallel Workflow → 17 jobs → 17 JVMs → 17 @AfterSuite calls
                                        ↓
                                 Each calls flushReports()
                                        ↓
                         SEND_EMAIL_ENABLED == true?
                                        ↓ YES
                              EmailUtil.sendEmail()
                                        ↓
                         17 SEPARATE EMAILS SENT ❌
```

The email chain:
1. `BaseTest.suiteTeardown()` (line 77) calls `ExtentReportManager.flushReports()`
2. `flushReports()` (line 431) checks `AppConstants.SEND_EMAIL_ENABLED`
3. If `true`, calls `sendReportEmail()` → `EmailUtil.sendEmail()`
4. Each parallel job runs its own JVM with its own TestNG suite
5. Result: 17 emails, one per module

The workflow also had a `send-email` job at the end — but this only sent a simple pass/fail HTML table (no report attachments). So the user got:
- 17 per-module emails with individual client reports (from Java)
- 1 summary email without attachments (from workflow)
- Total: **18 emails** for one test run

### Architecture (After Fix)

```
Parallel Workflow → 17 jobs → 17 JVMs → 17 @AfterSuite calls
                                        ↓
                         SEND_EMAIL_ENABLED == false (default)
                                        ↓
                         NO per-module emails ✅
                                        ↓
                    All reports uploaded as artifacts
                                        ↓
                        send-email job (1 email):
                          - Downloads all artifacts
                          - Builds consolidated HTML report
                          - Attaches all client reports
                          - Sends ONE email ✅
```

---

## Fix 1: Make `SEND_EMAIL_ENABLED` Environment-Variable-Driven

**File**: `src/main/java/com/egalvanic/constants/AppConstants.java` (line 217-220)

### Before
```java
//public static final boolean SEND_EMAIL_ENABLED = true;
public static final boolean SEND_EMAIL_ENABLED = false;
```

### After
```java
// Controllable via -DSEND_EMAIL_ENABLED=true or SEND_EMAIL_ENABLED env var.
// Default: false — prevents per-module emails in parallel CI jobs.
// The workflow's send-email job handles the consolidated email instead.
public static final boolean SEND_EMAIL_ENABLED = Boolean.parseBoolean(
    getEnv("SEND_EMAIL_ENABLED", "false"));
```

### Why This Works

- `getEnv()` already exists in `AppConstants.java` — checks system properties (`-D` flags) first, then environment variables, then falls back to the default
- `Boolean.parseBoolean("false")` → `false` (default: no email)
- `Boolean.parseBoolean("true")` → `true` (only when explicitly enabled)
- `Boolean.parseBoolean(null)` → `false` (safe for missing env vars)

### How to Control

| Scenario | Setting | Emails |
|---|---|---|
| Parallel CI (default) | Not set → `false` | 0 per-module |
| Single suite run (want email) | `-DSEND_EMAIL_ENABLED=true` | 1 email |
| Local development | Not set → `false` | 0 emails |

---

## Fix 2: Consolidated Report in Workflow `send-email` Job

**File**: `.github/workflows/ios-tests-parallel.yml` (send-email job)

### New Step: "Consolidate Client Reports"

Added between "Download All Reports" and "Prepare Email Summary":

1. **Finds all Client Report HTMLs** from downloaded artifacts (`all-reports/*/reports/client/Client_Report_*.html`)
2. **Copies them to `consolidated-reports/`** with module name prefix (e.g., `auth_client_report.html`)
3. **Parses TestNG XML results** from all modules using Python:
   - Reads `testng-results.xml` from each artifact
   - Extracts passed/failed/skipped counts per module
   - Identifies failed test names
4. **Generates `Consolidated_Client_Report.html`**:
   - Professional styled HTML with summary statistics
   - Module-by-module breakdown table
   - Pass rate percentage
   - Failed test names listed per module
5. **Builds attachment list** for the email (consolidated report + individual module reports)

### Updated "Send Email" Step

Added `attachments` parameter:
```yaml
attachments: ${{ steps.reports.outputs.ATTACHMENT_LIST }}
```

This attaches:
- `Consolidated_Client_Report.html` — the merged summary (always first)
- Individual module reports (e.g., `auth_client_report.html`, `site_client_report.html`, etc.)

### What the Client Receives

**ONE email** containing:
- **Subject**: `[STATUS] iOS Suite - X/15 Passed`
- **Body**: HTML table with job-level pass/fail status
- **Attachment 1**: `Consolidated_Client_Report.html` — module-by-module test results with pass counts and failed test names
- **Attachments 2-N**: Individual module client reports (detailed ExtentReports for each module)

---

## Files Changed

| File | Change |
|------|--------|
| `src/main/java/com/egalvanic/constants/AppConstants.java` | `SEND_EMAIL_ENABLED`: hardcoded `false` → env-var-driven (default `false`) |
| `.github/workflows/ios-tests-parallel.yml` | Added "Consolidate Client Reports" step + `attachments` to Send Email |

---

## Key Concepts

### Why Per-Module Emails Happen in Parallel Workflows

Each parallel job in GitHub Actions runs on a **separate macOS runner** with its own JVM. TestNG's `@AfterSuite` fires once per suite (= once per JVM). If `SEND_EMAIL_ENABLED` is `true`, each JVM sends its own email independently — there's no coordination between them.

The only place that has visibility into ALL jobs is the final `send-email` job (which uses `needs: [...]` to wait for all jobs). This is where consolidated reporting belongs.

### `Boolean.parseBoolean()` Safety

Java's `Boolean.parseBoolean(String)` is uniquely safe for configuration:
- Only returns `true` for the exact string `"true"` (case-insensitive)
- Returns `false` for: `null`, `""`, `"false"`, `"yes"`, `"1"`, anything else
- Never throws an exception
- This prevents accidental email sending from typos or misconfiguration

### TestNG XML Results

Each job uploads `target/surefire-reports/testng-results.xml` which contains:
```xml
<testng-results skipped="0" failed="2" total="38" passed="36">
  <test-method name="TC_AUTH_001_verifyLogin" status="PASS" .../>
  <test-method name="TC_AUTH_015_verifyInvalidLogin" status="FAIL" .../>
</testng-results>
```

The consolidated report Python script parses these XMLs to build the module-by-module summary without needing the Java framework.

---

## Status

- Per-module emails: **DISABLED** (default `false` via env var)
- Consolidated email: **ENABLED** (workflow `send-email` job with attachments)
- Individual module reports: **Still generated** (just not emailed individually)
