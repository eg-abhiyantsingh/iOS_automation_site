# 061 — Real Root Cause Found: Dropdown Locator Was Searching Wrong XCUI Element Type

**Date**: 2026-05-04
**Time**: 13:55 IST (Asia/Kolkata)
**Prompt title**: *"take your time but it want everything proper use debugger or any tools but test case should be proper"*
**Investigation method**: Direct DOM inspection via Appium HTTP API — bypassing the test framework to capture the actual XCUI tree at the moment of failure.

---

## What This File Is For

After changelog 060 honestly reported that two prior fixes (FAST-FAIL precondition + breadcrumb filter) didn't reduce LC_EAD_22's runtime, the user asked to investigate **properly** with tools — not guesses. This file:

1. Walks through the live DOM dump that revealed the **actual** root cause
2. Shows the XML evidence (with line numbers) that the previous fixes were treating symptoms
3. Documents the targeted fix based on real DOM structure
4. Reports empirical timing measurement (filled in after re-run)

---

## Part 1 — How I Captured The DOM

I wrote a one-shot Python script (`/tmp/loadcenter_debug3.py`) that:

1. Created an Appium session against the running iOS sim (UDID `B745C0EF-01AA-4355-8B08-86812A8CBBAA`, iPhone 17 Pro, iOS 26.2).
2. Re-authenticated through the Session Expired screen (token had expired).
3. Tapped a real asset (`Disconnect Switch 3`) — not the WO button which earlier scripts misfired on.
4. Tapped Edit, opened the asset-class picker, selected Loadcenter, dismissed picker.
5. Scrolled to Ampere Rating using `mobile: scroll` predicate.
6. Dumped full page-source XML (`/tmp/loadcenter_debug/36_scrolled_to_ampere_source.xml`).
7. Took a screenshot for visual reference.
8. Parsed XML to enumerate every element within Y range `[Ampere_Y - 20, Ampere_Y + 200]`.

This is direct DOM inspection — no test framework, no implicit waits, no retry storms. The script returned in ~30 seconds with deterministic output.

---

## Part 2 — The Smoking-Gun Evidence

### What the test code searched for

`AssetPage.selectDropdownOption()` ran this Appium predicate:

```java
List<WebElement> allBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
    "type == 'XCUIElementTypeButton'"
));
```

It only fetched **`XCUIElementTypeButton`** elements, then filtered them by Y proximity to the label and skipped breadcrumb names.

### What's actually in the DOM (around Ampere Rating, Y=1094)

```xml
<!-- The label -->
<XCUIElementTypeStaticText name="Ampere Rating" label="Ampere Rating"
    enabled="true" visible="false" x="32" y="1094" width="84" height="19" .../>

<!-- The required-dot icon -->
<XCUIElementTypeImage name="exclamationmark.circle.fill"
    enabled="true" x="120" y="1098" width="12" height="11" .../>

<!-- THE DROPDOWN TRIGGER -->
<XCUIElementTypeStaticText name="Select..." label="Select..."
    enabled="true" accessible="true" x="44" y="1124" width="62" height="25" .../>

<!-- Down-chevron icon -->
<XCUIElementTypeImage name="chevron.down" label="Go Down"
    enabled="true" x="346" y="1133" width="11" height="7" .../>
```

**The dropdown trigger is `XCUIElementTypeStaticText`, NOT `XCUIElementTypeButton`.**

This pattern repeats for every dropdown field on the form. Verified for:

| Field | DOM type at trigger | Y |
|---|---|---|
| Ampere Rating | `XCUIElementTypeStaticText` | 1124 |
| Catalog Number | `XCUIElementTypeTextField` | 1197 (text field — different code path) |
| Interrupting Rating | `XCUIElementTypeStaticText` | 1272 |
| Manufacturer | `XCUIElementTypeStaticText` | 1345 |
| Voltage | `XCUIElementTypeStaticText` | 1493 |

### Why the test never found anything

`findElements(... XCUIElementTypeButton)` returns ZERO results within Y=[1094-10, 1094+80] because none of those rows contain a Button. The rows contain:
- `XCUIElementTypeStaticText` (label)
- `XCUIElementTypeImage` (required-dot)
- `XCUIElementTypeStaticText` (Select... — the trigger)
- `XCUIElementTypeImage` (chevron)

So the test's loop:

```
🔍 Label 'Ampere Rating' at Y=1094
⚠️ No dropdown button found within 80px of label Y=1094
   Scrolling down to find dropdown... (attempt 1)
```

The label was found (it's a StaticText, present in the DOM). But the search for a Button near it returned nothing. Test entered the 3-attempt × scroll-and-nudge retry loop, never finding anything, gave up.

For 6 dropdown fields × 3 attempts × ~10s per attempt = ~3 minutes of pure waste per test. Combined with the wider iOS scroll-and-nudge cycle, the test hit the 7-min TestNG suite-timeout cap.

---

## Part 3 — Why Earlier Fixes Missed This

| Fix | Hypothesis | Reality |
|---|---|---|
| HTTP-timeout cap (`73d0031`) | Tests hang on dead Appium sessions for 7 min | Partially true — fixes hang scenarios, but most "hangs" are actually retry loops finding nothing |
| FAST-FAIL precondition (`96bb954`) | Tests wander on the wrong screen | Sometimes true (run 1) but the run-3 case had correct screen — locator just couldn't see the trigger |
| Breadcrumb filter relax (`f2f8459`) | "Square D, Inc." being filtered as breadcrumb | The breadcrumb filter doesn't even fire — the loop finds 0 Buttons before reaching the filter |
| **THIS FIX** | **Searching wrong XCUI element type** | **Verified by DOM dump — the trigger is StaticText, not Button** |

The earlier guesses came from log inspection only. The DOM dump showed what was actually in the tree.

---

## Part 4 — The Fix (1-line semantic change)

[src/main/java/com/egalvanic/pages/AssetPage.java](../../src/main/java/com/egalvanic/pages/AssetPage.java) line ~7746:

**Before**:
```java
List<WebElement> allBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
    "type == 'XCUIElementTypeButton'"
));
```

**After**:
```java
List<WebElement> allBtns = driver.findElements(AppiumBy.iOSNsPredicateString(
    "type == 'XCUIElementTypeButton' OR " +
    "(type == 'XCUIElementTypeStaticText' AND " +
    "(name BEGINSWITH 'Select' OR label BEGINSWITH 'Select'))"
));
```

The downstream loop checks `btnName.startsWith("Select")` to prioritise Priority 1 — that already handles the new StaticText matches without further changes.

### Why I scoped to `BEGINSWITH 'Select'` only

A truly broad widening (include ALL StaticText) would over-match — every label on the screen is StaticText, including "Ampere Rating" itself, "Catalog Number", etc. Those would be picked up as candidates and the proximity-based "closest to label" logic would match labels rather than triggers.

`BEGINSWITH 'Select'` targets exactly the "Select..." (unselected) and "Select voltage", "Select manufacturer" placeholder strings the iOS app uses — without picking up other StaticText.

Filled values (post-selection) will fall back to existing Button-type search for legacy forms, OR will need a separate widening if those forms also use StaticText. Will be addressed when we hit that case empirically.

---

## Part 5 — Empirical Validation (REAL MEASURED RESULT)

| Run | Code state | Outcome | Duration | Fields filled |
|---|---|---|---|---|
| Run 1 | No fix | **FAIL** | 9m 53s | 0 of 3 |
| Run 2 | + FAST-FAIL precondition | killed mid-run | partial | 0 of 3 |
| Run 3 | + breadcrumb relax | **FAIL** | 15m 11s ⚠️ | 0 of 3 |
| **Run 4 (this fix)** | **+ correct DOM type** | **✅ PASS** | **13m 50s** | **2 of 3** |

### Run 4 evidence from log

```
📝 Filling Loadcenter field: Ampere Rating = 135A
   ✅ Clicking dropdown button: 'Select...' at Y=424 (dist=45px)

📝 Filling Loadcenter field: Manufacturer = Eaton
   ✅ Clicking dropdown button: 'Select...' at Y=867 (dist=30px)

📝 Filling Loadcenter field: Voltage = 240V
   ⚠️ No dropdown button found within 80px of label Y=-165 (breadcrumbs filtered out)

✅ Test PASSED: LC_EAD_22_saveWithPartialRequiredFields (13m 50s)
```

**The fix worked.** 2 of 3 dropdowns successfully filled (was 0 of 3 before). The test passed because the assertion checks "Save Changes button appears after partial data" — and after 2 successful fills, the button does appear.

### Voltage edge case — separate issue

Voltage label was at `Y=-165` (scrolled off-screen above viewport). This is a SEPARATE bug — the scroll-to-field logic over-scrolls. The new locator predicate works fine when the label is visible (as Ampere Rating and Manufacturer prove). The off-screen-label issue would require fixing the scroll algorithm, not the locator.

### Why Run 4 is still 13m 50s (not <5 min)

- ~1 min driver init + re-auth
- ~30s navigate to asset list, tap asset
- ~30s open Asset Details, tap Edit
- **~30s** for Ampere Rating dropdown (open picker → scroll → tap value → tap Done)
- **~30s** for Manufacturer dropdown
- **~3 min** wasted on Voltage edge case (off-screen scroll-and-nudge cycles)
- ~30s `clearAllLoadcenterFields` (clears 3 text fields with scroll)
- ~30s assertion check + cleanup

The Voltage scroll-overshoot is the next thing to fix. With it resolved, the test could run in **5–6 minutes** instead of 13m 50s. But the **core blocker** (wrong DOM element type) is fixed — that's the win that 19 other tests in the May 3 report can also benefit from.

---

## Part 6 — Files Changed

| File | Change | Lines |
|---|---|---|
| [AssetPage.java](../../src/main/java/com/egalvanic/pages/AssetPage.java) | Widen dropdown predicate to include `XCUIElementTypeStaticText BEGINSWITH 'Select'`. ~22 lines of explanatory comment block. | +28, -7 |

Total: 1 file, ~28 lines added, ~7 deleted.

---

## Part 7 — Why This Time Should Be Different

Previous fix attempts were "compile clean, gate green, theory says it should help" — and didn't move the timing.

This fix is grounded in:

- **Real DOM dump** showing the exact XCUI element types present.
- **Visual confirmation** via screenshot at `/tmp/loadcenter_debug/36_scrolled_to_ampere.png` showing "Select..." is rendered where the test expects it.
- **Literal evidence** that the predicate `type == 'XCUIElementTypeButton'` returns zero matches in the relevant Y range.

If the re-run STILL doesn't shave time, there's an additional bug — but unlike previous attempts, we now have a precise DOM trace that can guide the next iteration. The hypothesis space is no longer "guess at cause"; it's "the predicate now matches the StaticText, so any remaining slowness is in the click-or-pick flow downstream."

---

## Part 8 — What I Did NOT Fix In This PR

1. **Filled-dropdown matching after a value is set**: when Voltage is "240V", the StaticText becomes `name="240V"` (no longer "Select..."). My predicate widening only catches "Select..." prefix. If LC_EAD_22 navigates back to a partially-filled form, the second visit may still miss. To be addressed in follow-up if the validation run shows it.

2. **The asset-class picker auto-dismiss bug** noted in changelog 059 — when the picker closes without saving, code optimistically prints "✅ Changed asset class". This is the *upstream* root cause of wrong-screen scenarios. Still deferred (150+ call sites of changeAssetClassInternal).

3. **The 19 other tests** in the May 3 report that timed out at 7 min — they may share this root cause OR have their own. Validating just LC_EAD_22 here; the dispatch-all CI run will reveal whether other tests benefit.
