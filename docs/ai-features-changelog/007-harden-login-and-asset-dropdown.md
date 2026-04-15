# 007 — Harden T&C Checkbox Detection + Fix Asset Class Dropdown

**Date:** 2026-04-15
**Prompt:** "still lots of test cases are failing check directly dont wait to complete it fully" — CI run #24438205550 (in progress)
**Prior Fix Reference:** 006 — First T&C checkbox + field clearing fix (commit cf6340e)
**Scope:** LoginPage T&C handling hardened, AssetPage dropdown locator rewritten
**Files Changed:**
- `src/main/java/com/egalvanic/pages/LoginPage.java`
- `src/main/java/com/egalvanic/pages/AssetPage.java`

---

## Context

After fix 006 (commit cf6340e) was deployed, CI run #24438205550 still showed test failures. Analysis of the completed run #24400731415 revealed **two distinct root causes**:

1. **Login cascade (15 test failures):** The initial T&C checkbox fix used a single annotated element locator that may not match all SwiftUI Toggle renderings
2. **Asset Class dropdown (3 test failures):** A separate locator issue — tests successfully reached the New Asset screen but couldn't find the dropdown

### Failure Breakdown from Run #24400731415

| Category | Failures | Root Cause |
|----------|----------|------------|
| S3 Drift | 8 | Infrastructure (not code) |
| Login Cascade | 15 | T&C checkbox not handled → Modules 2,4,5,6 all fail |
| Asset Class Dropdown | 3 | Locator mismatch in Module 3 (login worked fine) |
| TC25 (no assertion) | 1 pass | Passes vacuously — no login verification |

---

## Fix 1: Harden T&C Checkbox Detection (LoginPage.java)

### Problem
SwiftUI `Toggle` renders as **two separate elements**:
- `XCUIElementTypeSwitch` — the tappable toggle (may NOT carry "agree" label)
- `XCUIElementTypeStaticText` — the label text "I agree to the Terms..."

The original fix (006) searched for elements with "agree/terms" labels of type Switch/Button/Image/Other. If the Switch doesn't carry the label (common in SwiftUI), Strategy 1 fails. Strategy 2's broad search may only find the StaticText (not clickable as a checkbox). No fallback existed.

### Solution: 4-Strategy Approach

**Strategy 1:** Direct annotated element match (same as before, but handles `value == null`)

**Strategy 2 (NEW):** Broad DOM search for any **non-StaticText** tappable element with T&C-related labels. Key improvement: filters out StaticText elements that look like matches but aren't the actual checkbox.

**Strategy 3 (NEW — coordinate tap):** If only the StaticText label is found, calculates the checkbox icon position (25px left of the label text) and taps at those coordinates using the W3C PointerInput API. This handles the case where the checkbox icon is an unlabeled image or decorative element.

**Strategy 4 (NEW — any Switch):** Finds any `XCUIElementTypeSwitch` visible on the login screen. The T&C toggle is typically the only switch present, so this catches the case where the Switch has no descriptive label.

### Performance Optimization
Temporarily reduces implicit wait from 5s to 2s during T&C detection to prevent each failed `isElementDisplayed()` from hanging. Restored via `restoreImplicitWait()` on all exit paths.

### New Helper Methods
```java
private void restoreImplicitWait()     // Restore default 5s implicit wait
private void tapAtCoordinates(int, int) // W3C PointerInput API for coordinate-based taps
```

---

## Fix 2: Asset Class Dropdown Locator (AssetPage.java)

### Problem
`clickSelectAssetClass()` had 5 strategies, all failing:

1. **PageFactory** `accessibility = "Select asset class"` — not found (SwiftUI Picker renders with combined label)
2. **Exact match** `name == 'Select asset class'` — misses "Asset Class, Select asset class" combined label
3. **Known class names + Y check (600-800px)** — hardcoded Y range wrong for iPhone 16 Pro
4. **Find label → find button below** — `CONTAINS 'Asset Class'` is **case-sensitive** in NSPredicate (needs `[c]` flag)
5. **Broad fallback** — same case-sensitive exact matches

### Root Cause: SwiftUI Picker `.menu` Pattern
SwiftUI Picker with `.menu` style renders as:
- `XCUIElementTypeStaticText` label="Asset Class" (NOT clickable)
- `XCUIElementTypeButton` label="Asset Class, Select asset class" (the actual picker)

The exact match `name == 'Select asset class'` fails because the actual name is a combined string.

### Solution

**Strategy 2 rewritten:** Uses `CONTAINS[c] 'asset class'` (case-insensitive) to match both:
- `"Select asset class"` (original)
- `"Asset Class, Select asset class"` (SwiftUI Picker combined label)

**Strategy 3 simplified:** Removed Y-range constraint (600-800px) that breaks on different screen sizes.

**Strategy 4 rewritten:**
- Label search uses `CONTAINS[c]` for case-insensitive matching
- Finds the **closest** button to the label (by absolute Y distance < 100px) instead of requiring button to be below
- Falls back to coordinate tap right of the label if no button found (taps the picker area)

**Strategy 5:** All string matching made case-insensitive with `[c]` flag.

**New helper:** `tapAtCoordinates()` using W3C PointerInput API (same pattern as LoginPage).

---

## Expected Impact

| Module | Before (23 failures) | After (expected) |
|--------|---------------------|-------------------|
| S3 Drift | 8 fail | 8 fail (infra — not code) |
| Login (TC25) | 1 pass | 1 pass |
| Site Selection | 1 fail | PASS (T&C fix → login works) |
| Asset CRUD | 3 fail | PASS (dropdown fix + T&C fix) |
| Location CRUD | 4 fail | PASS (T&C fix → login works) |
| Connection CRUD | 3 fail | PASS (T&C fix → login works) |
| Issue CRUD | 4 fail | PASS (T&C fix → login works) |

**Expected result: 19/27 pass, 8 fail (all S3 infra)**

---

## Technical Details

### NSPredicate Case Sensitivity
`CONTAINS` in NSPredicate is **case-sensitive by default**. Adding `[c]` makes it case-insensitive:
```
// Case-sensitive (default) — misses "Asset class", "ASSET CLASS"
name CONTAINS 'Asset Class'

// Case-insensitive — matches all variations
name CONTAINS[c] 'Asset Class'
```

### W3C PointerInput API
Replaces deprecated `TouchAction` for coordinate-based taps:
```java
PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
Sequence tap = new Sequence(finger, 0);
tap.addAction(finger.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), x, y));
tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
driver.perform(Collections.singletonList(tap));
```

### Implicit Wait Management
Temporarily reducing implicit wait during element detection is critical for performance:
- Default 5s × 4 strategies × 2 element checks = 40s wasted on negative lookups
- With 2s: reduces to 16s worst case
- Always restored via `restoreImplicitWait()` in try-finally pattern
