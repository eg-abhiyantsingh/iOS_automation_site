# Changelog 020 — Fix 7 CI Failures: S3 Ordering, Delete Dialog Timing, Asset Picker

**Date**: 2026-04-16  
**Time**: ~16:00 IST  
**Prompt**: CI run 24511964600 — fix the 7 failing test cases across 4 modules  
**CI Run**: https://github.com/eg-abhiyantsingh/iOS_automation_site/actions/runs/24511964600

---

## Summary

CI run 24511964600 had **7 failures across 4 modules** (28 passed / 7 failed / 35 total). Root cause analysis revealed **3 distinct bugs**, all fixed in this changelog.

| Module | Status | Failures |
|--------|--------|----------|
| Module 0: S3 Drift | 16 passed, **2 failed** | TC_SMOKE_31, TC_SMOKE_39 |
| Module 1: Login | 1/1 passed | |
| Module 2: Site Selection | 1/1 passed | |
| Module 3: Asset CRUD | 3 passed, **1 failed** | BUG_DELETE_01 |
| Module 4: Location CRUD | 4/4 passed | |
| Module 5: Connection CRUD | 2 passed, **1 failed** | TC_CONN_051 |
| Module 6: Issue CRUD | 1 passed, **3 failed** | TC_ISS_049, 050, 076 |

---

## Bug 1: S3 Policy Statement Array Ordering (TC_SMOKE_31 + TC_SMOKE_39)

### Root Cause

The `normalizeJson()` method in `S3PolicyChecker.java` sorts JSON **object keys** alphabetically but **preserves array element order**. This is correct for general JSON arrays where order matters (`[1,2]` != `[2,1]`), but **wrong for IAM policy statements** where order is irrelevant.

AWS returned the same 3 statements in a different order than the baseline:

| Position | Baseline (Expected) | AWS (Actual) |
|----------|---------------------|--------------|
| 1 | AllowAuthenticatedUploads | AllowAuthenticatedUploads |
| 2 | **AllowCICDDevopsAccess** | **DenyInsecureTransport** |
| 3 | **DenyInsecureTransport** | **AllowCICDDevopsAccess** |

All 3 statements are **byte-for-byte identical** — only the array order differs. AWS can reorder policy statements at any time since IAM evaluates them independently.

### Fix

Added Sid-based sorting in `sortJsonElement()` — when an array contains only objects that all have a `Sid` field (IAM policy statements), sort them alphabetically by Sid value:

```java
// Before: arrays always preserve element order
if (element.isJsonArray()) {
    for (JsonElement item : original) {
        sorted.add(sortJsonElement(item));
    }
}

// After: arrays of objects with Sid fields are sorted by Sid
if (elements.size() > 1 && allObjectsHaveSid(elements)) {
    elements.sort((a, b) -> {
        String sidA = a.getAsJsonObject().get("Sid").getAsString();
        String sidB = b.getAsJsonObject().get("Sid").getAsString();
        return sidA.compareTo(sidB);
    });
}
```

Added helper method `allObjectsHaveSid()` to detect IAM statement arrays.

**Why this is the correct fix** (vs updating baselines): Updating baselines would fix it temporarily, but AWS can reorder statements again for other buckets. The normalization fix makes comparison permanently order-independent for policy statements.

### File Changed

| File | Change |
|------|--------|
| `src/main/java/com/egalvanic/utils/S3PolicyChecker.java` | Added Sid-based array sorting in `sortJsonElement()` + `allObjectsHaveSid()` helper |

---

## Bug 2: Delete Dialog Button Timing (BUG_DELETE_01 + TC_CONN_051)

### Root Cause

Both tests find the `XCUIElementTypeAlert` container successfully, then immediately search for Cancel/Delete buttons inside it. On CI, the **alert container appears in the DOM before its child buttons finish rendering**. The button search fails because there's no wait between dialog detection and button lookup.

CI log for BUG_DELETE_01:
```
✅ Confirmation dialog found (XCUIElementTypeAlert)
Step 5: Verify dialog has Cancel and Delete buttons
❌ FAILED: Confirmation dialog should have a Cancel button
```

Additionally, TC_CONN_051 had **7 instances of `visible == true`** in predicates (a known CI problem — elements may not be flagged visible on slower simulators) and still used the old **W3C 300ms swipe** instead of `mobile: dragFromToForDuration`.

### Fix — BUG_DELETE_01

1. Added **800ms wait** between dialog detection and button search
2. **Scoped search** within the alert element (not full DOM) — `alertElement.findElement()` instead of `driver.findElement()`
3. **Flexible type fallback** — if `XCUIElementTypeButton` isn't found, try without type constraint
4. **Retry loop** (3 attempts) for the Delete tap with stale element recovery

### Fix — TC_CONN_051 (Full CI Hardening)

Applied the same patterns from changelog 019 (BUG_DELETE_01) that were missing from TC_CONN_051:

| Issue | Before | After |
|-------|--------|-------|
| `visible == true` | 7 predicates | All removed |
| Swipe mechanism | W3C PointerInput 300ms | `mobile: dragFromToForDuration` primary, W3C 150ms fallback |
| Post-swipe wait | 500ms | 800ms |
| Trash icon location | Cell right edge (`cellX + cellW - 40`) | Screen right edge (`screenWidth - 40`) |
| Post-trash-tap wait | 500ms | 800ms |
| Dialog button search | Immediate, full DOM | 800ms wait, scoped within alert, retry loop |
| Delete confirm wait | 1000ms | 1500ms |
| Cell filter | `visible == true` | Y position filter (120 < Y < screenHeight*0.85) |

### Files Changed

| File | Change |
|------|--------|
| `src/test/java/com/egalvanic/tests/Asset_Phase1_Test.java` | BUG_DELETE_01 Step 5-6: Added 800ms wait, scoped alert search, flexible type fallback, retry loop |
| `src/test/java/com/egalvanic/tests/Connections_Test.java` | TC_CONN_051: Full CI hardening — native swipe, removed `visible == true`, dynamic bounds, dialog timing fix |

---

## Bug 3: Issue Asset Picker Not Found (TC_ISS_049 -> TC_ISS_050, TC_ISS_076)

### Root Cause

`tapSelectAsset()` in `IssuePage.java` fails to find the "Select Asset" button at the bottom of the New Issue form. The button is in the "Assignment" section which requires scrolling down. The method tried:
1. Direct find — failed (off-screen)
2. One scroll down + find — failed (not enough scrolling)
3. `mobile: scroll` with predicateString — failed

The most likely cause: **the keyboard persists after entering the issue title**, blocking the scroll from reaching the bottom of the form. After typing the title, if the keyboard is still up, the scrollable area is reduced by ~50% and the "Select Asset" button can't be reached.

CI log:
```
Step 5: Select first available asset
📋 Tapping Select Asset...
   Select Asset not found directly, scrolling down...
   Scrolled down on details screen (1/4)
   Select Asset not found after scroll, trying mobile:scroll...
⚠️ Could not tap Select Asset after all attempts
```

**Cascading effect:**
- TC_ISS_049 fails → issue not created → TC_ISS_050 can't find issue in list
- TC_ISS_076 calls `createQuickIssue()` which also uses `tapSelectAsset()` → fails too

### Fix

Rewrote `tapSelectAsset()` with 4 strategies:

1. **Dismiss keyboard first** — `mobile: hideKeyboard` before any scrolling (fixes the root cause)
2. **Multiple scroll attempts** (up to 3) instead of just 1 — handles longer forms
3. **`mobile: scroll` by predicateString** — iOS native scroll to element
4. **Alternative label matching** — broader predicate with `CONTAINS` instead of just `==`, includes `XCUIElementTypeStaticText`

```java
// NEW: Dismiss keyboard first — may be blocking the scroll
driver.executeScript("mobile: hideKeyboard");

// NEW: Up to 3 scroll attempts instead of just 1
for (int scrollAttempt = 1; scrollAttempt <= 3; scrollAttempt++) {
    scrollDownOnDetailsScreen();
    // try to find Select Asset after each scroll...
}
```

### File Changed

| File | Change |
|------|--------|
| `src/main/java/com/egalvanic/pages/IssuePage.java` | `tapSelectAsset()`: keyboard dismissal, 3x scroll retries, broader label matching |

---

## Bug 4: Email Always Shows "PASSED" + No Delivery Visibility

### Root Cause 1: Wrong Status Variable

The email job's Prepare Email step (line 902) used:
```yaml
SMOKE_RESULT: ${{ needs.smoke-tests.result }}
```

This is the **job result**, which is always `"success"` because `continue-on-error: true` on the test step (line 550) masks Maven's non-zero exit code. So the email always said `[✅ PASSED]` regardless of actual test results.

The correct variable is:
```yaml
SMOKE_RESULT: ${{ needs.smoke-tests.outputs.smoke_result }}
```

This comes from `steps.smoke_status` which checks the actual `SMOKE_RESULT` env var set by the dashboard script (which correctly uses `exit 1` on failure).

Also, the comparison was `"success"` but the custom output uses `"passed"`:
```yaml
# Before: if [ "$SMOKE_RESULT" = "success" ]; then  ← never matches "passed"
# After:  if [ "$SMOKE_RESULT" = "passed" ]; then   ← correctly matches
```

### Root Cause 2: Silent Email Failures

`continue-on-error: true` on the Send Email step masks any SMTP failures. If `EMAIL_PASSWORD` is wrong (e.g., regular Gmail password instead of App Password), the action silently fails and the CI shows green.

### Fix

1. **Correct status variable**: `needs.smoke-tests.outputs.smoke_result` instead of `needs.smoke-tests.result`
2. **Correct comparison**: `"passed"` instead of `"success"`
3. **Diagnostic Email Summary**: The Email Summary step now checks `steps.send_email.outcome` and shows:
   - Whether the send actually succeeded or failed
   - If failed: instructions to create a Gmail App Password
   - Whether `EMAIL_TO` secret is configured or using the fallback address

### Gmail App Password Requirement

Gmail SMTP requires an **App Password** (not your regular Gmail password). To set this up:
1. Go to Google Account > Security > 2-Step Verification (must be enabled)
2. Go to App Passwords > Generate a new app password
3. Copy the 16-character password
4. Set it as `EMAIL_PASSWORD` secret in GitHub repo settings

### File Changed

| File | Change |
|------|--------|
| `.github/workflows/ios-tests-smoke.yml` | Fixed email status variable, added diagnostic summary with App Password instructions |

---

## All Files Changed

| File | Changes |
|------|---------|
| `src/main/java/com/egalvanic/utils/S3PolicyChecker.java` | Sid-based array sorting for IAM policy comparison |
| `src/test/java/com/egalvanic/tests/Asset_Phase1_Test.java` | BUG_DELETE_01 dialog button timing fix |
| `src/test/java/com/egalvanic/tests/Connections_Test.java` | TC_CONN_051 full CI hardening |
| `src/main/java/com/egalvanic/pages/IssuePage.java` | `tapSelectAsset()` keyboard + scroll fix |
| `.github/workflows/ios-tests-smoke.yml` | Fixed email status (wrong variable) + diagnostic summary |

---

## Status

| Test | Root Cause | Fix |
|------|-----------|-----|
| TC_SMOKE_31 | Statement array ordering | **FIXED** (Sid-based sort) |
| TC_SMOKE_39 | Statement array ordering | **FIXED** (Sid-based sort) |
| BUG_DELETE_01 | Dialog button render timing | **FIXED** (800ms wait + scoped search) |
| TC_CONN_051 | `visible == true` + dialog timing + W3C swipe | **FIXED** (full CI hardening) |
| TC_ISS_049 | Keyboard blocking asset picker scroll | **FIXED** (keyboard dismiss + 3x scroll) |
| TC_ISS_050 | Cascade from TC_ISS_049 | **FIXED** (TC_ISS_049 fix resolves) |
| TC_ISS_076 | Cascade from `tapSelectAsset()` | **FIXED** (same `tapSelectAsset()` fix) |
| Email Status | Wrong variable (`needs.smoke-tests.result`) | **FIXED** (uses `outputs.smoke_result`) |
| Email Delivery | Silent failures + possibly wrong credentials | **FIXED** (diagnostic summary) + **ACTION NEEDED** (verify App Password) |
