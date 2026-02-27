# CI/CD Failure Analysis — 84 Failing Tests

## Summary

| Module | Failures | Tests Run | Fail Rate |
|--------|----------|-----------|-----------|
| OfflineTest | 29 | 34 | 85% |
| Asset_Phase1_Test | 5 | 114 | 4% |
| Asset_Phase2_Test | 2 | — | — |
| Asset_Phase3_Test | 14 | 109 | 13% |
| Asset_Phase4_Test | 1 | 97 | 1% |
| Asset_Phase5_Test | 2 | 221 | <1% |
| Asset_Phase6_Test | 1 | 137 | <1% |
| SiteSelectionTest | 2 | 5 | 40% |
| Issue_Phase1_Test | 7 | 119 | 6% |
| Issue_Phase2_Test | 8 | 60 | 13% |
| Issue_Phase3_Test | 3 | 58 | 5% |
| LocationTest | 3 | 85 | 4% |
| Connections_Test | 7 | 93 | 8% |
| **TOTAL** | **84** | **~1132** | **~7%** |

---

## Root Cause Categories

### RC-1: `goOfflineViaPopup()` Timeout — 31 tests (HIGHEST IMPACT)

**Affected:** All 29 OfflineTest failures + TC_SS_028 + TC_SS_052

**File:** `OfflineTest.java:296`, `SiteSelectionPage.java:1294-1375`

**Problem:** The WiFi popup interaction chain has compounding implicit wait timeouts:

1. `clickWifiButton()` — uses 500ms implicit wait to find WiFi button
2. `isGoOfflineOptionVisible()` → `isElementDisplayed(goOfflineText)` — @iOSXCUITFindBy uses GLOBAL implicit wait (5s+), not the 500ms local override
3. If popup didn't open, falls back to `siteSelectionPage.goOffline()` which:
   - Calls `isWifiOnline()` — makes 3-4 sequential `isElementDisplayed()` checks (5s each = 20s)
   - Calls `clickWifiButton()` again (5s)
   - Tries multiple click strategies with waits (3-5s each)
4. `dismissWifiPopup()` searches for Dashboard element (another 5s)

**Total worst case:** 40-60 seconds per `goOfflineViaPopup()` call. With 420s test timeout and tests that call this + do other work, they time out.

**TC_OFF_001 specific:** Assertion failure (not timeout). `isGoOfflineOptionVisible()` returns `false` because `shortWait()` (200ms) is insufficient for popup animation (300-500ms). The retry logic adds another 5s implicit wait before also failing.

**Fix Strategy:**
- Replace `@iOSXCUITFindBy` lazy proxy with explicit `findElements()` + 0-timeout pattern
- Increase post-click wait from 200ms to 800ms
- Use `findElements().size() > 0` (instant check) instead of `isElementDisplayed()` (5s wait)
- Add max-retry counter to prevent cascade fallbacks

---

### RC-2: `isSaveChangesButtonVisible()` uses `visible == true` — 17+ tests

**Affected:** DS_EAD_23, FUSE_EAD_24, LC_EAD_12/14/20, MOTOR_EAD_21, and all tests that assert Save Changes button

**File:** `AssetPage.java:9400-9416`

**Problem:**
```java
List<WebElement> saveBtns = driver.findElements(
    AppiumBy.iOSNsPredicateString(
        "(name CONTAINS 'Save' OR label CONTAINS 'Save') AND " +
        "type == 'XCUIElementTypeButton' AND visible == true"  // ← PROBLEM
    )
);
```

The `visible == true` predicate only matches elements **currently rendered on screen**. Tests scroll UP (`scrollFormUp()`) before checking, but Save Changes button is at the **bottom** of the form = off-screen = invisible to this predicate.

**Fix Strategy:**
- Remove `visible == true` from the predicate
- Use `(name CONTAINS 'Save' OR label CONTAINS 'Save') AND type == 'XCUIElementTypeButton'`
- Same pattern as location picker fix (MEMORY.md: "NEVER use `visible == true`")

---

### RC-3: `selectDropdownOption()` Performance — 6+ tests

**Affected:** ATS_EAD_13, ATS_EAD_17, BUG_TIMING_02, LC_EAD_23/25, MCC_EAD_21/23, PB_11

**File:** `AssetPage.java:7530-7692`

**Problem:**
```java
// Scans ALL buttons on screen (100+)
List<WebElement> buttons = driver.findElements(
    AppiumBy.className("XCUIElementTypeButton"));

// For each button, fetches TWO attributes (200+ network round-trips)
for (WebElement btn : buttons) {
    String name = btn.getAttribute("name");   // network call
    String label = btn.getAttribute("label");  // network call
    if (name.contains(fieldName) || label.contains(fieldName)) {
        btn.click();
        break;
    }
}
```

With 100+ buttons and 2 attributes each = 200+ Appium network calls. Done up to 3 retries = 600+ calls. Each call ~100-200ms = **30-60 seconds per dropdown**.

**Fix Strategy:**
- Use NSPredicate to filter server-side: `type == 'XCUIElementTypeButton' AND (name CONTAINS 'fieldName' OR label CONTAINS 'fieldName')`
- Single `findElements()` call instead of iterating all buttons
- Reduces 200+ calls to 1 call

---

### RC-4: Subcategory Search/Filter Logic — 10 tests

**Affected:** TC_ISS_107-119 (Phase1), TC_ISS_120/134/135/136 (Phase2)

**File:** `IssuePage.java:5728-5787`

**Problem:** `getFilteredSubcategoryOptions()` has an overly restrictive filter:
```java
if (label.contains("Chapter")) {  // Only returns items containing "Chapter"
    options.add(label);
}
```

When searching for "Visual" (TC_ISS_107), results like "Visual Inspections" are filtered out because they don't contain "Chapter". For OSHA subcategories, same issue.

Also, `searchSubcategoryAndCountResults()` does a full DOM scan with broad predicates, potentially matching elements outside the dropdown.

**Fix Strategy:**
- Remove `label.contains("Chapter")` restriction
- Add Y-position filtering to only match dropdown items (not background elements)
- Use dropdown container bounds for filtering

---

### RC-5: Thermal Anomaly Fields Off-Screen — 4 tests

**Affected:** TC_ISS_159, TC_ISS_162, TC_ISS_164, TC_ISS_167

**File:** `IssuePage.java:6332+`

**Problem:** `isThermalFieldPresent()` uses direct `findElement()` without scrolling. Fields like "Problem Temp", "Reference Temp", "Current Draw", "Voltage Drop" are below the visible viewport. The method doesn't use `mobile: scroll` with predicateString to find off-screen elements.

**Fix Strategy:**
- Add `mobile: scroll` with `predicateString` before element check (same pattern as location picker)
- Or remove `visible == true` from search and use DOM-only search

---

### RC-6: `selectAssetByIndex()` No Timeout — 3 tests

**Affected:** TC_CONN_059, TC_CONN_062, TC_CONN_063

**File:** `ConnectionsPage.java:1970-1977`

**Problem:** `selectAssetByIndex()` calls `driver.findElements()` to search for asset text elements in dropdown. If dropdown hasn't fully rendered, the implicit wait can block indefinitely. No explicit timeout control.

**TC_CONN_063 specific:** Even when `fillAllConnectionFields()` runs without timeout, the offline connection creation doesn't actually increment the count (8→8), suggesting the offline creation API doesn't work or the count check is stale.

**Fix Strategy:**
- Add explicit WebDriverWait with configurable timeout
- Add validation after `fillAllConnectionFields()` before asserting count

---

### RC-7: Location Navigation Timeouts — 3 tests

**Affected:** TC_EB_003, TC_NF_001, testTeardown timeout

**File:** `BuildingPage.java:1836-1910`

**Problem:** `navigateToNewFloor()` uses uncontrolled `findElements()` calls:
- Line 1852: Searches for plus buttons without timeout
- Line 1856: Searches all plus buttons without timeout
- Line 1896: Retry search without timeout

If the plus button isn't on screen, these block for the full implicit wait.

**Fix Strategy:**
- Add explicit timeout to element searches
- Use `mobile: scroll` to find off-screen plus buttons

---

### RC-8: Toggle State Timing — 2 tests

**Affected:** ATS_EAD_14, TC_UPS_04

**File:** `Asset_Phase1_Test.java:1318`

**Problem:**
```java
assetPage.enableRequiredFieldsOnly();           // Tap toggle
boolean toggleOn = assetPage.isRequiredFieldsToggleOn();  // Check immediately — NO WAIT
if (!toggleOn) {
    assetPage.toggleRequiredFieldsOnly();       // Toggles it BACK OFF
}
assertTrue(assetPage.isRequiredFieldsToggleOn(), "Toggle should be ON");  // FAILS
```

No wait between tap and state check. iOS toggle animation takes ~300ms. Test reads pre-animation state (`false`), then toggles again (turning it OFF), then asserts ON → fails.

**Fix Strategy:**
- Add `mediumWait()` (400ms) after toggle tap before checking state
- Or use `waitForCondition()` to poll until state changes

---

### RC-9: Issue Test Data Assumptions — 4 tests

**Affected:** TC_ISS_003, TC_ISS_189, TC_ISS_211, TC_ISS_212

**Problem:**
- **TC_ISS_003:** "Closed tab" predicate `label CONTAINS 'Closed'` may not match if tab uses different text format
- **TC_ISS_189:** Assumes an "In Progress" issue exists in the list. No issue creation step.
- **TC_ISS_211/212:** `swipeLeftOnFirstIssue()` swipe distance may be insufficient to reveal delete button. `isSwipeDeleteButtonVisible()` searches full DOM, may miss cell-scoped button.

**Fix Strategy:**
- TC_ISS_003: Use more flexible predicate or check exact tab label format
- TC_ISS_189: Either create an In Progress issue first, or skip if none exists
- TC_ISS_211/212: Increase swipe distance, add wait for animation, scope delete button search to swiped cell

---

## Fix Plan — 5 Parts

### Part 1: WiFi/Offline Popup (31 tests) — HIGHEST ROI
Fix `goOfflineViaPopup()`, `clickWifiButton()`, `isGoOfflineOptionVisible()`, `goOffline()`, `goOnline()` in SiteSelectionPage + OfflineTest.
**Expected recovery: 31 tests**

### Part 2: Asset Save Button + Dropdown Performance (20+ tests)
Fix `isSaveChangesButtonVisible()` (remove `visible == true`) and optimize `selectDropdownOption()` (use NSPredicate server-side filter).
**Expected recovery: 20+ tests**

### Part 3: Asset Timeouts + Toggle (8 tests)
Fix toggle state timing, `fillAllRequiredFields()` methods, `fillLoadcenterField()`.
**Expected recovery: 8 tests**

### Part 4: Issue Module (18 tests)
Fix `getFilteredSubcategoryOptions()`, `isThermalFieldPresent()`, swipe delete, In Progress badge, Closed tab.
**Expected recovery: 18 tests**

### Part 5: Location + Connection + SiteSelection (8 tests)
Fix `navigateToNewFloor()`, `selectAssetByIndex()`, WiFi-related SiteSelection tests.
**Expected recovery: 8 tests**
