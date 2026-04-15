# 007 — Harden T&C Checkbox Detection + Fix Asset Class Dropdown

**Date:** 2026-04-15
**Prompt:** "still lots of test cases are failing check directly dont wait to complete it fully" — CI runs #24438205550, #24441465618
**Prior Fix Reference:** 006 — First T&C checkbox + field clearing fix (commit cf6340e)
**Scope:** LoginPage T&C handling hardened (keyboard dismiss + visibility fix), AssetPage dropdown locator rewritten
**Files Changed:**
- `src/main/java/com/egalvanic/pages/LoginPage.java`
- `src/main/java/com/egalvanic/pages/AssetPage.java`
**Commits:** `a1e8869`, `b948e7b`, `a7cf772`

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

### Problem (Initial — commit a1e8869)
SwiftUI `Toggle` renders as **two separate elements**:
- `XCUIElementTypeSwitch` — the tappable toggle (may NOT carry "agree" label)
- `XCUIElementTypeStaticText` — the label text "I agree to the Terms..."

The original fix (006) searched for elements with "agree/terms" labels of type Switch/Button/Image/Other. None matched because the tappable element doesn't carry the label text.

### Problem (Critical — commit a7cf772)
**CI runs #24438205550 and #24441465618 proved the 4-strategy approach still failed.** Screenshots showed the T&C checkbox clearly on screen, but logs showed "No Terms & Conditions checkbox found."

**Root cause: keyboard occlusion.** The login flow calls:
```
enterPassword() → acceptTermsIfPresent() → tapSignIn()
```
After `enterPassword()`, the iOS keyboard remains open. The T&C checkbox sits below the password field — **exactly where the keyboard covers**. In iOS, elements behind the keyboard have `visible == false`, so `isElementDisplayed()` returns false and `visible == true` predicates skip them.

### Solution: Keyboard Dismiss + Visibility-Independent Search

**Pre-step (commit a7cf772):** Dismiss keyboard before any T&C search:
```java
driver.hideKeyboard();  // Primary
tapAtCoordinates(screenWidth/2, 200);  // Fallback: tap neutral area
```

**Strategy 1:** Direct annotated element match (handles `value == null`)

**Strategy 2:** Broad DOM search for non-StaticText tappable elements — **NO `visible == true` constraint** (elements may be in DOM but marked non-visible due to keyboard timing)

**Strategy 3 (coordinate tap):** Find "I agree..." StaticText, tap 25px to its LEFT where checkbox icon sits

**Strategy 4 (any Switch):** Find any `XCUIElementTypeSwitch` — **NO `visible == true` constraint**

### Diagnostic Logging
Added `"🔍 T&C search found N candidate elements"` to confirm whether the predicate matches anything — essential for debugging future failures.

### Helper Methods
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
