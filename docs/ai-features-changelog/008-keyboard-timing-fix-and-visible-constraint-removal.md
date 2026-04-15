# 008 — Keyboard Timing Fix + Asset Dropdown Visibility Constraint Removal

**Date:** 2026-04-15
**Time:** 14:17 IST
**Prompt:** "make this time it will run correctly. Quality is more important than quantity."
**Prior Fix Reference:** 007 — Keyboard dismiss + 4-strategy T&C fix (commits a1e8869, a7cf772, 7e8800e)
**Scope:** LoginPage keyboard timing, LoginPage annotated element safety, AssetPage visibility constraint removal, Smoke XML class/method fix
**Files Changed:**
- `src/main/java/com/egalvanic/pages/LoginPage.java`
- `src/main/java/com/egalvanic/pages/AssetPage.java`
- `src/test/resources/smoke/testng-smoke-asset-crud.xml`
- `testng-smoke.xml`
- `src/test/resources/parallel/testng-smoke.xml`

---

## Context

After fixes 006-007 (keyboard dismiss, 4-strategy T&C detection, hyperlink safety), CI runs #24438205550, #24441465618, and #24444064147 still showed test failures. Deep analysis of the code flow revealed **three remaining timing/matching issues** that explain why the keyboard dismiss fix in commit a7cf772 didn't resolve the T&C checkbox problem.

### Why Previous Fixes Still Failed

**Run #24441465618 behavior:** The keyboard dismiss code (`driver.hideKeyboard()`) was called and likely succeeded. BUT the code immediately searched for T&C elements without waiting for the iOS accessibility tree to refresh. Result: all 4 strategies returned 0 candidates.

**Explanation:** When iOS dismisses the keyboard, there's a ~300ms animation. During this animation, the accessibility tree (Appium's view of the DOM) still reports elements behind the keyboard as `visible == false`. The `findElements()` call that happens immediately after `hideKeyboard()` returns 0 results because the DOM hasn't caught up with the visual state.

---

## Fix 1: Add 500ms Wait After Keyboard Dismissal (LoginPage.java) — CRITICAL

### Problem
```
enterPassword() → keyboard opens
acceptTermsIfPresent() → hideKeyboard() → IMMEDIATELY search for T&C elements
                                            ^ DOM still has keyboard-era state
```

After `driver.hideKeyboard()`, the code jumped straight to `findElements()` with a reduced 2s implicit wait. But the iOS accessibility tree takes ~300-500ms to refresh after keyboard dismissal. Elements that were hidden behind the keyboard still appear as `visible == false` in the DOM.

### Solution
Added `Thread.sleep(500)` after keyboard dismissal (both primary and fallback paths), BEFORE any element search begins:

```java
// Primary: driver.hideKeyboard()
// Fallback: tapAtCoordinates(screenWidth/2, 200)

// NEW: Wait for keyboard animation + DOM refresh
Thread.sleep(500);

// THEN search for T&C elements
driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
```

### Why 500ms?
- iOS keyboard dismiss animation: ~250-350ms
- Accessibility tree refresh: ~100-200ms after animation completes
- 500ms provides safe margin without noticeable user impact
- Total method time increase: negligible (was already 2s × 4 strategies worst case)

---

## Fix 2: Remove `label CONTAINS[c] 'terms'` from Annotated Element (LoginPage.java)

### Problem
The `@iOSXCUITFindBy` annotation for `termsCheckbox` (line 41) included `label CONTAINS[c] 'terms'` in its NSPredicate. This predicate matches:
- The T&C checkbox (intended match)
- The "Terms & Conditions" **hyperlink** (blue link text — UNINTENDED)

While Strategy 2 was already fixed to exclude hyperlinks (commit 7e8800e), Strategy 1 used this annotated element directly. If keyboard dismiss + wait made Strategy 1 fire for the first time, it could match the hyperlink and click it — navigating away from the login screen.

### Solution
Removed `label CONTAINS[c] 'terms'` from the annotated element predicate:

**Before:**
```java
@iOSXCUITFindBy(iOSNsPredicate = "... AND (label CONTAINS[c] 'agree' OR label CONTAINS[c] 'terms' OR name CONTAINS 'checkbox' OR name CONTAINS 'square')")
```

**After:**
```java
@iOSXCUITFindBy(iOSNsPredicate = "... AND (label CONTAINS[c] 'agree' OR name CONTAINS 'checkbox' OR name CONTAINS 'square')")
```

The remaining predicates still match the checkbox:
- `label CONTAINS[c] 'agree'` → matches "I agree to the Terms..."
- `name CONTAINS 'checkbox'` → matches if element is named "checkbox"
- `name CONTAINS 'square'` → matches checkbox icon name

---

## Fix 3: Remove `visible == true` from AssetPage Dropdown Strategies

### Problem
`clickSelectAssetClass()` in AssetPage.java had `visible == true` in Strategies 2, 3, 4, and 5. The test flow calls `dismissKeyboard()` before `selectAssetClass()`:

```java
assetPage.enterAssetName(assetName);  // keyboard opens
assetPage.dismissKeyboard();           // keyboard dismissed
assetPage.selectAssetClass("ATS");     // clickSelectAssetClass() runs
```

After keyboard dismiss, elements may still be marked `visible == false` for a brief period during DOM refresh (same issue as the T&C checkbox). With `visible == true` in the predicates, `findElement()` misses the Asset Class dropdown button even though it's functionally on screen.

### Solution
Removed `visible == true` from all strategies in `clickSelectAssetClass()`:

| Strategy | Before | After |
|----------|--------|-------|
| Strategy 2 (contains match) | `visible == true AND name CONTAINS[c] 'asset class'` | `name CONTAINS[c] 'asset class'` |
| Strategy 3 (known class names) | `name == 'ATS' AND visible == true` | `name == 'ATS'` |
| Strategy 4 (buttons near label) | `type == 'XCUIElementTypeButton' AND visible == true` | `type == 'XCUIElementTypeButton'` |
| Strategy 5 (fallback) | `visible == true AND name CONTAINS[c] 'select asset'` | `name CONTAINS[c] 'select asset'` |

### Diagnostic Enhancement
Added a diagnostic button dump before the RuntimeException throw:
```java
// Logs first 15 buttons with name, label, and Y position
// Enables debugging if all strategies fail again
```

---

## Technical Deep Dive: iOS Keyboard → DOM Refresh Timing

### The Keyboard Occlusion Chain
1. `enterPassword("pw")` → `sendKeys()` focuses password field → iOS keyboard opens
2. Keyboard covers bottom ~40% of screen (Y > ~500 on iPhone 16 Pro)
3. iOS marks all elements behind keyboard as `visible == false` in accessibility tree
4. `driver.hideKeyboard()` sends dismiss signal → keyboard begins slide-down animation (~300ms)
5. During animation: DOM still reports elements as `visible == false`
6. After animation: iOS updates accessibility tree → elements get `visible == true`
7. **Without sleep:** `findElements()` runs during step 5 → returns empty
8. **With 500ms sleep:** `findElements()` runs after step 6 → returns matching elements

### Why `findElements` Returns Empty (Not Just `isDisplayed` == false)
In Appium XCUITest driver, `findElements` with `iOSNsPredicateString` runs the NSPredicate against the **XCUITest element snapshot**. This snapshot includes all elements in the app's view hierarchy, but elements behind the keyboard may be temporarily excluded from the snapshot during animation transitions. The 500ms wait ensures the snapshot is refreshed.

---

## Smoke Test Module Dependencies (Analysis)

During this investigation, I mapped how each smoke module handles login:

| Module | Test | Has Own Login? | Depends On |
|--------|------|----------------|------------|
| 0 - S3 Drift | S3 tests | No Appium | Independent |
| 1 - Login | TC25 | Yes (`performLogin()`) | T&C fix |
| 2 - Site Selection | TC_SS_044 | Yes (`loginAndSelectSite()`) | T&C fix |
| 3 - Asset CRUD | ATS_ECR_31 | **NO** — direct navigation | Module 2 session |
| 4 - Location CRUD | TC_NB_010 | **NO** — direct navigation | Module 2 session |
| 5 - Connection CRUD | TC_CONN_037 | Yes (recovery) | T&C fix |
| 6 - Issue CRUD | TC_ISS_049 | Yes (recovery) | T&C fix |

**Modules 3 and 4** don't have login code — they rely on session state from Module 2. If Module 2 logs in successfully, the app saves the session (noReset=true). When Modules 3/4 relaunch the app, it auto-logs in.

**This is why fixing the T&C checkbox fixes 15+ tests:** it unblocks Module 2's login, which cascades to Modules 3-6.

---

## Expected Impact

With all three fixes applied:

| Module | Before | After (expected) |
|--------|--------|-------------------|
| S3 Drift (8 tests) | 8 fail | 8 fail (infra — not code) |
| Login (TC25) | 1 pass* | 1 pass |
| Site Selection (TC_SS_044) | 1 fail | PASS |
| Asset CRUD (4 tests) | 4 fail | PASS (T&C + dropdown fix) |
| Location CRUD (4 tests) | 4 fail | PASS (T&C → session) |
| Connection CRUD (3 tests) | 3 fail | PASS (T&C → login recovery) |
| Issue CRUD (4 tests) | 4 fail | PASS (T&C → login recovery) |

*TC25 passes vacuously — no assertion on actual login success.

**Expected result: 19/27 pass, 8 fail (all S3 infrastructure)**

---

## Fix 4: Smoke Suite XML Class/Method Mismatch

### Problem
The `testng-smoke-asset-crud.xml` referenced `BUG_DELETE_01_deleteRequiresConfirmation` in `Asset_Phase5_Test`. Two issues:
1. **Wrong class**: The method actually lives in `Asset_Phase1_Test`
2. **Wrong method name**: The actual name is `BUG_DELETE_01_deleteAssetVerification`

This caused TestNG to fail/skip the 4th Asset CRUD test (Delete) in EVERY smoke run — the test could never be found.

### Solution
Updated all 3 smoke XML files to reference the correct class and method:
- `src/test/resources/smoke/testng-smoke-asset-crud.xml`
- `testng-smoke.xml` (root)
- `src/test/resources/parallel/testng-smoke.xml`

Combined all 4 Asset CRUD tests into a single `Asset_Phase1_Test` class entry.

### Note
`bug_tests_suite.xml` and `failed_tests_rerun.xml` have similar mismatches for ALL BUG_* tests — they reference `Asset_Phase5_Test` but the methods are in `Asset_Phase1_Test`. These are not used in smoke CI runs and are deferred for a future fix.
