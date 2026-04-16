# Changelog 016 — IR Photo Navigation & Button Detection Fixes

**Date:** April 16, 2026  
**Prompt:** "Continue fixing remaining CI failures from run #24449855754"  
**CI Run:** https://github.com/Egalvanic/eg-pz-mobile-iOS/actions/runs/24449855754  
**Commit:** `68cc616`

---

## Problem Summary

After the comprehensive fix in changelog 015, several IR Photo tests (TC_JOB_104–107, 110, 112) were identified as remaining failures. Root cause analysis revealed:

1. **Blind scrolling too shallow** — `navigateToIRPhotosSection()` used 2-3 fixed swipes to scroll down the New Asset form. If the form grew (new fields added), the Infrared Photos section at the bottom was never reached.
2. **"Add IR Photo Pair" button detection limited** — Only 2 strategies (label match and Y-proximity), missing `mobile: scroll` approach.
3. **"New IR Photos" section hidden below visible area** — After tapping "Add IR Photo Pair", the resulting list section appeared below the viewport. Tests checked immediately without scrolling.
4. **"Create Asset" button unreachable** — TC_JOB_110 used a single scroll to find the Create Asset button at the bottom of the form.

---

## Files Modified (2 files)

### 1. `WorkOrderPage.java` — New `scrollToInfraredPhotosSection()` + Button Improvements

#### a) `scrollToInfraredPhotosSection()` — NEW METHOD
**What it does:**  
Uses `mobile: scroll` with NSPredicate to scroll the New Asset form directly to the "Infrared Photos" section header. This is XCUITest-native — it handles any form length automatically, unlike blind swipes which move a fixed pixel distance.

**Why `mobile: scroll` is better than blind swipes:**
- `mobile: scroll` with `predicateString` tells XCUITest to scroll until the matching element is visible — like `scrollIntoView()` in web automation
- Blind swipes (`PointerInput`) move a fixed pixel distance per swipe. If a form grows (new fields), the same number of swipes won't reach the bottom
- `mobile: scroll` handles content insets, safe areas, and scroll bouncing correctly
- On CI runners with lower CPU, blind swipes can "miss" due to scroll animation lag

**Predicate fallback chain:**
1. `label == 'Infrared Photos'` (exact match)
2. `label CONTAINS 'Infrared Photos'` (partial)
3. `label CONTAINS 'IR Photos'` (abbreviated)
4. `label == 'INFRARED PHOTOS'` (uppercase)
5. `label CONTAINS 'FLIR' OR label CONTAINS 'Thermal' OR label CONTAINS 'IR Photo Filename'` (IR content)

**Key code:**
```java
public boolean scrollToInfraredPhotosSection() {
    String[] predicates = {
        "label == 'Infrared Photos'",
        "label CONTAINS 'Infrared Photos'",
        "label CONTAINS 'IR Photos'",
        "label == 'INFRARED PHOTOS'"
    };
    for (String pred : predicates) {
        Map<String, Object> scrollParams = new HashMap<>();
        scrollParams.put("direction", "down");
        scrollParams.put("predicateString", "type == 'XCUIElementTypeStaticText' AND " + pred);
        driver.executeScript("mobile: scroll", scrollParams);
        if (isInfraredPhotosSectionDisplayed()) return true;
    }
    return false;
}
```

#### b) `tapAddIRPhotoPairButton()` — Expanded from 2 to 4 strategies

| Strategy | Method | When it helps |
|----------|--------|---------------|
| 1 | NSPredicate label match (+ "Add Pair", "Add Photo Pair") | Label unchanged |
| 2 | Y-proximity to IR header (buttons within 300px) | Label changed, still near header |
| 3 | `mobile: scroll` to find + tap button | Button below visible area |
| 4 | Any button below IR header (excluding Cancel/Done/Back/Create) | Last resort positional match |

---

### 2. `SiteVisit_phase2.java` — Test Navigation & Assertion Fixes

#### a) `navigateToIRPhotosSection()` — REWRITTEN
**Before:** 2-3 blind `scrollNewAssetFormDown()` calls.  
**After:** `mobile: scroll` via `scrollToInfraredPhotosSection()` first. If that fails, falls back to up to 6 manual swipes (was 3).

#### b) TC_JOB_104 — Scroll after adding pair
**Before:** Checked `isNewIRPhotosSectionDisplayed()` immediately after tapping "Add IR Photo Pair".  
**After:** If section not visible, scrolls down once and re-checks. The "New IR Photos" list appears below the input fields and may be below the viewport.

#### c) TC_JOB_105 — Scroll to count pairs
**Before:** Counted pairs immediately after adding two pairs.  
**After:** If `pairCount < 2`, scrolls down once to reveal all pairs.

#### d) TC_JOB_110 — Three improvements
1. **Re-navigation to IR section:** Uses `scrollToInfraredPhotosSection()` instead of 2 blind swipes after scrolling up to fill the asset name
2. **Create Asset button:** Scrolls in a loop (up to 4 times) instead of 1-2 blind swipes
3. Both use the same mobile:scroll → manual swipe fallback pattern

#### e) `createJobWithPhotoTypeAndNavigateToIR()` (TC_JOB_112–114) — REWRITTEN scroll section
Same fix as `navigateToIRPhotosSection()`: `mobile: scroll` first, 6 manual swipes as fallback.

---

## LC_EAD Analysis (No Code Changes Needed)

LC_EAD_14, 16, 20 (140-213s) and LC_EAD_22, 23 (420s timeout) were collateral damage from the toggle search exhaustion fixed in changelog 015. Assets P3 was CANCELLED (6-hour timeout) in the original run — the toggle fix resolved this. Assets P3 now passes in the live CI run (24460007762).

---

## Impact Assessment

| Test Group | Before (Run 24449855754) | Expected After |
|-----------|-------------------------|----------------|
| TC_JOB_104–107 | IR Photos section not reached | Should reach section via mobile:scroll |
| TC_JOB_110 | Create Asset button unreachable | Loop scroll finds button |
| TC_JOB_112–114 | IR section not reached (photo type) | mobile:scroll handles any form length |
| LC_EAD_14,16,20 | CANCELLED (toggle timeout) | PASS (toggle cache, confirmed in CI) |
| LC_EAD_22,23 | CANCELLED (toggle timeout) | PASS (toggle cache, confirmed in CI) |

## Live CI Run Status (24460007762 — in progress)

| Job | Previous | Current |
|-----|----------|---------|
| Assets P3 (109 tests) | CANCELLED | **SUCCESS** ✅ |
| Assets P5 (112 tests) | CANCELLED | **SUCCESS** ✅ |
| Assets P2 (108 tests) | SUCCESS | **SUCCESS** ✅ |
| Issues P3 (58 tests) | SUCCESS | **SUCCESS** ✅ |
| Offline (35 tests) | SUCCESS | **SUCCESS** ✅ |
| Connections (94 tests) | SUCCESS | **SUCCESS** ✅ |
| Issues P1 (119 tests) | CANCELLED | in_progress |
| Issues P2 (60 tests) | CANCELLED | in_progress |
| Site Visit / Work Orders | CANCELLED | in_progress |

## Remaining Considerations

1. **ATS_ECR_07, ATS_ECR_10** — These are app validation tests designed to catch bugs (spaces-only names, name trimming). They fail because the app has the bug, not because of test issues.
2. **BUG_SPECIAL_01** — Asset with special characters (`#`, `&`) causes app crash. This is an app bug, not an automation issue.
3. **Quick QR Action** — May still fail if the feature was removed from the app entirely. Debug logging from changelog 015 will help diagnose.
