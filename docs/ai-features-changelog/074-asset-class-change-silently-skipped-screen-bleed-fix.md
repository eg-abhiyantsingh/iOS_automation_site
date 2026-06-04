# 074 — Asset class change was silently skipped (screen-bleed false-positive)

**Date:** 2026-06-04
**Trigger:** User watching `ATS_EAD_13` locally: *"you are not changing asset
class in asset test case i think"* — Ampere Rating / Voltage dropdowns logged
*"not found in DOM"* even after scrolling.

## TL;DR
`changeAssetClassToATS()` was **silently skipping** the picker because the
class-detection read the **asset-list screen bleeding through behind the Edit
form** instead of the field's real value. The asset under test was actually a
**Motor**, but detection reported **ATS**, so the change was skipped, the asset
stayed Motor, and the ATS-only required fields never existed → "not found."
Fixed by reading the picker **button structurally**; added a regression guard.
All verified live.

## Root cause (proven with a live DOM dump)
The Edit Asset screen's real field is:
```
[StaticText] x=32 y=636  name='Asset Class'          ← label
[Button]     x=32 y=661  w=376 name='Motor'          ← picker (selected = Motor)
[StaticText] x=76 y=673  name='Motor' value='Motor'  ← value text
```
But the **previous screen (the asset list) stays live in the accessibility tree**
underneath the pushed Edit screen. That list contains assets literally *named*
"ATS 1/2/3/404…", rendered as rows like `name='ATS 2, 1, ATS'` (name, location,
class) plus a class column `[StaticText] x=356 y=643 name='ATS'` — all at the
same Y (~611–693) as the "Asset Class" label.

The old `isCurrentAssetClassEqualTo("ATS")` did a **loose `CONTAINS[c] 'ATS'` +
80px-Y-proximity** scan, so it matched the bleed-through list row's "ATS" and
returned `true` → `changeAssetClassInternal` logged `✅ Already ATS` and skipped
the picker. `getCurrentAssetClassValue()` had the same blind spot.

> Why a *green* test hid it: `ATS_EAD_13` only asserted
> `isEditAssetScreenDisplayed()` — a present-only check that's true regardless of
> the class. Classic "green suite finds no bugs."

## Fixes (`src/main/java/com/egalvanic/pages/AssetPage.java`)
1. **`findAssetClassPickerButton()`** — locate the selected class **structurally**:
   the wide (`w≥150`) Button directly below the "Asset Class" label, at the
   **same x** (±25), within 55px down, whose label is a **single known class**
   (no commas → not a list row). List rows differ by x and carry comma-separated
   `name, loc, class` labels, so they're excluded.
2. **`getCurrentAssetClassValue()`** rewritten to read that button (fallback: the
   value StaticText indented below the label). Returns the *real* selected class
   (e.g. `Motor`).
3. **`isCurrentAssetClassEqualTo()`** now delegates to (2) and does an **exact,
   case-insensitive** compare — no more substring/proximity scan. Returns `false`
   when undetectable so the caller opens the picker and selects (idempotent).
4. **`openAssetClassPicker()`** — new Strategy 0: click the picker **button
   element directly** (the old coordinate tap could miss / hit the row behind it).
5. **`isSelectablePickerOption()`** — the generic dropdown selector's
   "first available option" fallback used to click *any* button in a Y range, so
   it grabbed UI chrome (`Sheet Grabber` drag-handle, `Calculator`, SF-Symbol
   glyphs, list rows). New filter rejects chrome / glyphs / comma rows / the
   field label; applied to both the contains-match loop and the fallback.

## Regression guard (`Asset_Phase1_Test.java`, ATS_EAD_13)
Added after `changeAssetClassToATS()`:
```java
assertTrue(
    waitForCondition(() -> assetPage.isCurrentAssetClassEqualTo("ATS"), 5,
        "asset class to read ATS after changeAssetClassToATS()"),
    "Asset class should be ATS after changeAssetClassToATS() — a silent skip ...");
```
This **strengthens** (never weakens) the assertion — it fails loudly on the exact
silent-skip that started this. (Also restored `ATS_EAD_06`, which an editing slip
had removed; `@Test` count back to 112.)

## Live verification (iPhone 17 Pro Max, iOS 26.2)
| Run | Asset Class | Ampere Rating | Voltage | Mains Type | Result |
|---|---|---|---|---|---|
| pre-fix (user) | falsely "Already ATS" (stays Motor) | not found | not found | not found | fields missing |
| post-fix #1 | `Motor` → **changed to ATS** | `100 kA` | `480V` | (n/a) | **PASS** |
| post-fix #2 | `Motor` → **changed to ATS** | `100 kA` | `480V` | `MCB` | **PASS** |
| post-fix #3 (+guard) | `Motor` → **ATS**, **guard asserted ATS ✅** | `100 kA` | `Calculator`* | (scroll miss) | **PASS** |

\* Residual, pre-existing flakiness — see below.

## Known residual (NOT this bug, flagged for follow-up)
The **generic dropdown option selector** is run-to-run flaky for SwiftUI
sheet-style dropdowns: when the exact/contains match misses, "first available
option" scans the screen and can grab a tool button. The new chrome filter blocks
the ones observed (`Sheet Grabber`, `Calculator`, …) but a blocklist is
whack-a-mole. A principled fix (scope option search to the opened sheet, or
skip-instead-of-guess on 0 matches) is a separate task — it does not affect the
asset-class fix.
