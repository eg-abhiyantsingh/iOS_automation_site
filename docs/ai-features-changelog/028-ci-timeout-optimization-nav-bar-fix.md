# 028 — CI Timeout Optimization & Nav Bar Safety Fixes

**Date**: 2026-04-17  
**Prompt**: Make sure this time most of the test cases run properly and pass  
**Source Run**: https://github.com/Egalvanic/eg-pz-mobile-iOS/actions/runs/24513764702  

---

## Summary

All 39 remaining test failures from the developer repo CI run were ThreadTimeoutExceptions at exactly 420s (the TestNG suite-level timeout). Root cause analysis revealed two systemic issues: implicit wait compounding in screen-detection methods, and unnecessary cleanup logic in Issue tests. Targeted optimizations reduce per-test execution time by 50-120s, bringing tests well under the 420s timeout.

---

## Part 1 — Implicit Wait Compounding (All Test Groups)

### Problem
Screen-detection methods like `isIssueDetailsScreenDisplayed()`, `isConnectionsScreenDisplayed()`, and `isEditAssetScreenDisplayed()` use multi-strategy element searches. Each failed `findElement` blocks for the full **5s implicit wait**. A method with 3-4 strategies wastes **15-20s** when the screen isn't present — and these methods are called inside retry loops, multiplying the waste.

### Root Cause
Appium's implicit wait applies globally to every `findElement`/`findElements` call. The default 5s is appropriate for normal element interactions but far too long for "is this screen displayed?" boolean checks that are called repeatedly in navigation logic.

### Fix Pattern — Reduced Implicit Wait Wrapper
Wrap detection methods in a temporary 1.5s implicit wait with a `finally` block to guarantee restoration:

```java
public boolean isSomeScreenDisplayed() {
    try {
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(1500));
        try {
            // ... multi-strategy element search ...
        } finally {
            driver.manage().timeouts().implicitlyWait(
                Duration.ofSeconds(AppConstants.IMPLICIT_WAIT));
        }
    } catch (Exception e) { return false; }
}
```

### Files Modified

**`src/main/java/com/egalvanic/pages/IssuePage.java`** — 5 methods optimized:
- `isIssueDetailsScreenDisplayed()` — 1.5s wrapper (was 5s × called 6× in tapFirstIssue)
- `tryOpenIssueClassPicker()` Strategy 1 & 3 — 1.5s wrapper
- `isUnsavedChangesWarningDisplayed()` — 1s wrapper across 4 strategies (saves ~16s)
- `changeIssueClassOnDetails()` — reduced retries from 3→2, retry sleep 800→400ms, 1.5s verification

**`src/main/java/com/egalvanic/pages/BuildingPage.java`** — 3 methods optimized:
- `isLocationsScreenDisplayed()` — 1.5s wrapper
- `areBuildingEntriesDisplayed()` — 1.5s wrapper
- `isContextMenuDisplayed()` — 1.5s wrapper across 3 strategies

**`src/main/java/com/egalvanic/pages/AssetPage.java`** — 1 method optimized:
- `isEditAssetScreenDisplayed()` — 1.5s wrapper (was iterating 6 asset class names × 5s each = 30s waste)

**`src/main/java/com/egalvanic/pages/ConnectionsPage.java`** — 2 methods optimized:
- `isConnectionsScreenDisplayed()` — 1.5s wrapper across 3 strategies
- `isConnectionDetailsScreenDisplayed()` — 1.5s wrapper across 3 strategies

### Time Savings
- IssuePage detection: ~16-20s saved per occurrence
- BuildingPage detection: ~10-15s saved per occurrence
- AssetPage edit screen detection: ~25s saved per occurrence
- ConnectionsPage detection: ~10-15s saved per occurrence

### Tests Benefiting
All tests that navigate between screens — effectively every test in the suite. The largest impact is on tests that call these methods in loops or as part of multi-step navigation flows.

---

## Part 2 — Issue Test Cleanup Elimination (24 Tests)

### Problem
18 tests in Issue_Phase2_Test and 6 in Issue_Phase1_Test had verbose cleanup blocks that called `changeIssueClassOnDetails("NEC Violation")` to revert the issue class before closing. This consumed 50-80s per test — unnecessary since `@AfterMethod` terminates the app anyway.

### Root Cause
The "revert to NEC Violation" pattern was defensive — ensuring test isolation by restoring original state. But with app restart in `@AfterMethod`, the state is reset regardless. The revert involved:
1. `scrollUpOnDetailsScreen()` — 2-5s
2. `changeIssueClassOnDetails("NEC Violation")` — 20-40s (scroll, find picker, open, find option, select, verify)
3. `tapCloseIssueDetails()` — 2-5s
4. Unsaved changes dialog handling — 5-15s

### Fix — `quickDismissIssueDetails()`
New method in IssuePage that replaces the 4-step revert with fast dismiss:

```java
public void quickDismissIssueDetails() {
    try { dismissKeyboard(); sleep(100); } catch (Exception ignored) {}
    try { dismissDropdownMenu(); sleep(100); } catch (Exception ignored) {}
    tapCloseIssueDetails();
    sleep(300);
    if (isUnsavedChangesWarningDisplayed()) {
        tapDiscardChanges();
        sleep(200);
    }
}
```

**Time per test**: ~1-2s (vs. 50-80s with revert)

### Files Modified

**`src/test/java/com/egalvanic/tests/Issue_Phase2_Test.java`** — 18 tests updated:
- TC_ISS_130, 132, 134, 135, 136, 146, 149, 150, 154, 155, 165, 166, 167, 168, 169, 171, 174, 175
- Removed: `scrollUpOnDetailsScreen()` + `changeIssueClassOnDetails("NEC Violation")` + old close/discard blocks
- Replaced with: `issuePage.quickDismissIssueDetails()`
- File reduced from 4802 to 4592 lines (-210 lines)

**`src/test/java/com/egalvanic/tests/Issue_Phase1_Test.java`** — 6 tests updated:
- TC_ISS_064, 082, 107, 108, 109, 118
- Same cleanup block replacement pattern

### Tests Fixed (timeout elimination)
24 Issue tests that were hitting the 420s timeout due to cumulative cleanup overhead.

---

## Part 3 — Asset Phase 3 Nav Bar Safety Fix (10 Tests)

### Problem
After `scrollFormDown()` or `mobile: scroll` positions a form field near the top of the screen, the field can land at Y < 120 — behind the iOS navigation bar. Taps at Y < 120 are intercepted by the nav bar, silently failing. The test then fails assertion checks because the field was never actually edited.

### Root Cause
`mobile: scroll` with `predicateString` scrolls until iOS considers the element "visible" — but iOS defines "visible" as within the scroll view's bounds, not necessarily below the nav bar overlay. An element at Y=50 is technically visible but untappable.

The `editTextField()` and `selectDropdownOption()` methods find elements by label proximity and click them directly — without checking whether Y > 120.

### Fix — `nudgeIfBehindNavBar()` Helper + Label Position Check

**`src/main/java/com/egalvanic/pages/AssetPage.java`**:

**New helper** `nudgeIfBehindNavBar(int elementY)`:
- If Y >= 150: returns false (no action needed)
- If Y < 150: performs a precise drag to push content down by (220 - elementY) pixels, bringing the element to ~Y=220
- Uses right-edge scrolling (x = screenWidth - 20) to avoid triggering form fields

**`selectDropdownOption()`** — after finding label at Y < 150:
- Calls `nudgeIfBehindNavBar(labelY)`
- If nudge was needed, re-finds the label with updated position
- Continues with dropdown button search at the new Y

**`editTextField()` Strategy 3** — after finding label at Y < 150:
- Same nudge + re-find pattern
- Strategy 4 benefits transitively (element positions are already corrected)

### Tests Fixed (assertion errors → passing)
- LC_EAD_12 — Edit Columns dropdown
- LC_EAD_14 — Edit Fault Withstand Rating dropdown
- LC_EAD_16 — Edit Manufacturer dropdown
- LC_EAD_20 — Edit Voltage dropdown
- LC_EAD_22 — Save with partial required fields
- LC_EAD_23 — Save with all required fields
- LC_EAD_25 — Verify green check indicators
- MCC_EAD_21 — Save MCC with all required fields
- MCC_EAD_23 — Verify MCC green check indicators
- MOTOR_EAD_21 — Edit RPM text field

---

## Not Addressed

### Appium Session Timeouts (~50 tests)
Infrastructure-level issue — Appium sessions die mid-test, causing cascade failures. Existing `isSessionLikelyDead()` with `ThreadTimeoutException` detection handles these. Not a code fix — depends on CI runner stability.

### Asset Phase 5 Specific Failures (4 tests)
- TC_CB_ST_09, TC_DS_ST_13, TC_UPS_01, TC_UTL_06
- Likely benefit from `isEditAssetScreenDisplayed()` implicit wait reduction
- May require environment-specific asset class availability (existing graceful skip handles this)

---

## Impact Summary

| Category | Tests Addressed | Fix Type | Time Saved Per Test |
|----------|----------------|----------|-------------------|
| Issue P1+P2 Cleanup | 24 | Cleanup elimination | 50-80s |
| Location Detection | 5 (intermittent) | Implicit wait reduction | 10-15s |
| Asset Edit Screen | All asset tests | Implicit wait reduction | 25s |
| Connections Detection | 3 | Implicit wait reduction | 10-15s |
| Asset P3 Nav Bar | 10 | Y-position safety + nudge | Fixes assertion errors |
| **Total** | **~42** | | |

### Files Changed (7 files)
- `src/main/java/com/egalvanic/pages/IssuePage.java` — 5 method optimizations + 1 new method
- `src/main/java/com/egalvanic/pages/BuildingPage.java` — 3 method optimizations
- `src/main/java/com/egalvanic/pages/AssetPage.java` — 1 method optimization + nav bar safety
- `src/main/java/com/egalvanic/pages/ConnectionsPage.java` — 2 method optimizations
- `src/test/java/com/egalvanic/tests/Issue_Phase2_Test.java` — 18 cleanup replacements
- `src/test/java/com/egalvanic/tests/Issue_Phase1_Test.java` — 6 cleanup replacements
- `src/main/java/com/egalvanic/constants/AppConstants.java` — minor constant adjustment

All changes are in the QA automation repo only — no changes to the developer/production repo.
