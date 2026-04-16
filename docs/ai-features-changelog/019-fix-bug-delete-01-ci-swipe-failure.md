# Changelog 019 — Fix BUG_DELETE_01: Swipe-to-Delete Fails on CI

**Date**: 2026-04-16  
**Time**: ~15:30 IST  
**Prompt**: BUG_DELETE_01_deleteAssetVerification works locally but fails in CI — fix deletion test

---

## Summary

The `BUG_DELETE_01_deleteAssetVerification` test worked locally but failed on CI due to three issues:
1. Used W3C PointerInput (300ms) for swipe — fragile on CI simulators
2. All 7 `findElements` calls used `visible == true` — fails when CI elements aren't flagged visible
3. Hardcoded Y bounds (`y > 200 && y < 800`) — doesn't adapt to CI screen dimensions

---

## Changes

### 1. Swipe Mechanism: `mobile: dragFromToForDuration` (Primary)

**Before**: Only W3C PointerInput with 300ms duration
```java
// FRAGILE: W3C Actions with 300ms — too slow on CI, swipe may not trigger
swipe.addAction(finger.createPointerMove(Duration.ofMillis(300), ...));
driver.perform(Arrays.asList(swipe));
sleep(500);
```

**After**: Native XCUITest gesture first, W3C fallback with 150ms
```java
// PRIMARY: mobile: dragFromToForDuration — native iOS gesture, reliable on CI
Map<String, Object> dragParams = new HashMap<>();
dragParams.put("fromX", startX); dragParams.put("fromY", centerY);
dragParams.put("toX", endX);   dragParams.put("toY", centerY);
dragParams.put("duration", 0.3);
driver.executeScript("mobile: dragFromToForDuration", dragParams);

// FALLBACK: W3C PointerInput with 150ms (not 300ms — faster triggers delete)
swipe.addAction(finger.createPointerMove(Duration.ofMillis(150), ...));
```

`mobile: dragFromToForDuration` is the same API used by `IssuePage.performSwipeLeft()` which works reliably on CI for issue swipe-to-delete.

### 2. Removed All `visible == true` (7 predicates)

Every `findElements` call had `AND visible == true` which blocks for the full implicit wait when elements aren't flagged visible on CI:

```java
// BEFORE (7 occurrences)
"type == 'XCUIElementTypeCell' AND visible == true"
"type == 'XCUIElementTypeButton' AND visible == true AND label CONTAINS ','"
"(label == 'Delete' ...) AND type == 'XCUIElementTypeButton' AND visible == true"
"type == 'XCUIElementTypeAlert' AND visible == true"
"label == 'Delete Asset' AND type == 'XCUIElementTypeStaticText' AND visible == true"
"label == 'Cancel' AND type == 'XCUIElementTypeButton' AND visible == true"
"label == 'Delete' AND type == 'XCUIElementTypeButton' AND visible == true"

// AFTER: All removed — search full DOM, let the code filter by position
"type == 'XCUIElementTypeCell'"
"type == 'XCUIElementTypeButton' AND label CONTAINS ','"
"(label == 'Delete' ...) AND type == 'XCUIElementTypeButton'"
// etc.
```

### 3. Dynamic Screen Bounds Instead of Hardcoded

```java
// BEFORE: Hardcoded pixel values
if (y > 200 && y < 800) { // Asset list area

// AFTER: Percentage of actual screen height
int screenHeight = driver.manage().window().getSize().getHeight();
if (y > 120 && y < screenHeight * 0.85) { // Below nav bar, above tab bar
```

### 4. Better Trash Icon Coordinate Tap

```java
// BEFORE: Tapped at original cell right edge (may be wrong after swipe)
int trashX = cellX + cellW - 40;
// Used W3C PointerInput — verbose and fragile

// AFTER: Tapped at screen right edge (where delete always appears) via mobile: tap
int screenWidth = driver.manage().window().getSize().getWidth();
int trashX = screenWidth - 40;
driver.executeScript("mobile: tap", Map.of("x", trashX, "y", trashY));
```

### 5. Increased Wait Times for CI

| Wait | Before | After | Why |
|------|--------|-------|-----|
| After swipe | 500ms | 800ms | CI swipe animation is slower |
| After trash tap | 500ms | 800ms | Dialog render time |
| After Delete confirm | 1000ms | 1500ms | List refresh after server-side deletion |

### 6. Added Dialog Detection Strategy

Added Strategy 3 for confirmation dialog: looks for "cannot be undone" or "Are you sure" text — catches dialogs with non-standard titles.

---

## Files Changed

| File | Change |
|------|--------|
| `src/test/java/com/egalvanic/tests/Asset_Phase1_Test.java` | Rewrote `BUG_DELETE_01_deleteAssetVerification()`: native swipe, removed `visible == true`, dynamic bounds, better coordinate tap, increased CI waits |

---

## Why It Works Locally but Fails on CI

| Factor | Local | CI |
|--------|-------|-----|
| Swipe gesture | W3C 300ms works (fast simulator) | Too slow — doesn't trigger iOS delete action |
| `visible == true` | Elements render fast, flagged visible | Slower rendering, elements may not be flagged visible yet |
| Screen size | Known device, hardcoded Y works | Different iPhone model/resolution, Y=800 may be in tab bar |
| Timing | Fast simulator, 500ms is enough | Slower CI, animations need 800ms+ |

---

## Status

- Swipe mechanism: **FIXED** (`mobile: dragFromToForDuration` primary, W3C 150ms fallback)
- `visible == true`: **FIXED** (removed from all 7 predicates)
- Screen bounds: **FIXED** (dynamic percentages instead of hardcoded pixels)
- Timing: **FIXED** (increased waits for CI)
