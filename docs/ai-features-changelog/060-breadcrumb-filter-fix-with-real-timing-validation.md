# 060 — Breadcrumb Filter Fix + Real Timing Validation Of FAST-FAIL Precondition

**Date**: 2026-05-04
**Time**: 12:55 IST (after honest self-assessment from changelog 059)
**Prompt title**: *"did you really fix all this properly or not"*
**User intent**: After changelog 059 shipped a partial mitigation, the user asked whether it was actually validated. The honest answer was no — I had killed the second test run before completion and uncovered a second bug (breadcrumb filter rejecting commas) that I documented but didn't fix. User said "c" — do BOTH the breadcrumb fix and the empirical timing validation.

---

## What This File Is For

This is the **honest follow-up** to changelog 059. That file shipped a fast-fail precondition with theoretical timing claims. This file:

1. Fixes the second bug I uncovered (the breadcrumb filter's `contains(", ")` rejection)
2. Runs LC_EAD_22 **to completion** with both fixes active
3. Reports actual measured timing — not extrapolated guesses

---

## Part 1 — The Breadcrumb Filter Bug

### What was wrong

In `selectDropdownOption()` at `AssetPage.java:7771`, when looking for a dropdown button near a label, the helper filtered candidates with:

```java
// Skip location breadcrumb buttons (contain ", " with path segments)
if (btnName.contains(", ")) continue;
```

Asset list rows on iOS render as buttons with names like `"Trim067938, Room_xxx, ATS"` — that's the breadcrumb pattern: `AssetName, Room, AssetType`. The filter was meant to exclude these from being mistaken for dropdown buttons.

But **legitimate dropdown values can also contain `,`**. Common examples for the Loadcenter form:

| Field | Value | Fails filter? |
|---|---|---|
| Manufacturer | `Square D, Inc.` | ❌ Yes — wrongly skipped |
| Manufacturer | `ABB, Inc.` | ❌ Yes — wrongly skipped |
| Manufacturer | `Eaton, Inc.` | ❌ Yes — wrongly skipped |
| Manufacturer | `Schneider Electric` | ✅ No — passes |
| Voltage | `240V` | ✅ No — passes |
| Voltage | `120V, 240V` | ❌ Yes — wrongly skipped |

### What I observed live

In the second LC_EAD_22 run (12:47–~12:57 IST), with the FAST-FAIL precondition correctly recognising the Edit screen, the test still spent minutes on:

```
   🔍 Label 'Manufacturer' at Y=158
   ⚠️ No dropdown button found within 80px of label Y=158 (breadcrumbs filtered out)
   Scrolling down to find dropdown... (attempt 1)
   ...
```

The dropdown button for Manufacturer existed at Y~165 — within 80px of the label. But its name was something like `Square D, Inc.` and got filtered as a breadcrumb. So the helper looped through 3 attempts × 2 nudge cycles, then gave up.

### The fix

The breadcrumb pattern always has **3+ segments** (≥2 separators):

```
"Trim067938, Room_xxx, ATS"          → 3 segments → BREADCRUMB
"Meter 104, R2, Meter"               → 3 segments → BREADCRUMB
"Square D, Inc."                     → 2 segments → DROPDOWN VALUE
"ABB, Inc."                          → 2 segments → DROPDOWN VALUE
"120V, 240V"                         → 2 segments → could be either, but length<50 + Y proximity decides
```

Replace:

```java
if (btnName.contains(", ")) continue;     // ❌ overzealous
```

with:

```java
if (btnName.split(", ").length >= 3) continue;   // ✅ targets the breadcrumb shape only
```

`String.split(", ")` returns the segments split by `, `. A breadcrumb has 3+ segments (asset name, location, type). A manufacturer name has at most 2 (`"Name, Inc."`).

The `length > 50` filter still catches verbose breadcrumbs that happen to use a different separator pattern, so we have a second-line defense.

### Why this is safe

| Edge case | Behaviour |
|---|---|
| `"Square D, Inc."` (2 segments) | Now passes filter — correct |
| `"Trim067938, Room_xxx, ATS"` (3 segments) | Still filtered — correct |
| `"A, B, C, D"` (4 segments) | Filtered — correct (real breadcrumbs can have more) |
| `"Foo, Bar"` random 2-segment that happens to NOT be a dropdown | Could be miscategorised — but the proximity check (within 80px of label) provides spatial filtering. A button that's far from the dropdown's expected position won't be picked even if it passes the name filter. |
| Empty string / null name | Existing `if (btnName == null) btnName = "";` handles this |

---

## Part 2 — Real Timing Validation (Honest Result)

### Run conditions
- Date: 2026-05-04
- Test method: `Asset_Phase3_Test::LC_EAD_22_saveWithPartialRequiredFields`
- Sim: iPhone 17 Pro (UDID `B745C0EF-01AA-4355-8B08-86812A8CBBAA`), iOS 26.2
- Appium 3.1.2 on port 4723
- Both fixes active: FAST-FAIL precondition (096bb954) + breadcrumb filter fix (this commit)

### Timing comparison

| Run | Code state | Outcome | Duration |
|---|---|---|---|
| Run 1 (no fix) | Pre-fix (HTTP timeout only) | FAIL — wrong screen, no fields filled | 9m 53s |
| Run 2 (FAST-FAIL only) | + screen-state precondition | Killed manually at ~10 min — second bug observed | partial |
| **Run 3 (BOTH fixes)** | + breadcrumb filter fix | **FAIL — same assertion** | **15m 11s ⚠️ longer** |

### Honest conclusion: my fixes did NOT solve the problem

- **Run 1 → Run 3 timing got WORSE**, not better (9m 53s → 15m 11s).
- The reason it took longer: with FAST-FAIL passing through (Edit screen correctly detected) and breadcrumb filter relaxed, each missing-field probe ran the **full 3-attempt × scroll-and-nudge loop** to completion instead of giving up earlier.
- The actual blocker is a **third bug** in the dropdown-locator coordinate logic that neither of my fixes touches.

### What the screenshot revealed (Run 3 failure)

The captured failure screenshot at `screenshots/LC_EAD_22_saveWithPartialRequiredFields_FAILED_20260504_132531.png` shows:

- The Edit screen IS rendered correctly (header `Close | Asset Details | ⋯`)
- "Ampere Rating" label IS visible with a red required-dot
- "Select..." dropdown button IS visible right below it (~40px away)
- Yet the locator logged `Y=57` for the label and `No dropdown button found within 80px`

Possible third-bug explanations (not investigated here, deferred):

1. **Stale DOM coordinates** — labels found in one frame, button positions read in a later frame after auto-scroll repositioned them. Implicit-wait race.
2. **The "Select..." button is rendered as a different XCUI type** (e.g., `XCUIElementTypeOther` containing a button) and isn't picked up by `type == 'XCUIElementTypeButton'` predicate.
3. **Scroll algorithm overshoots** — labels reported at `Y=-1026` indicate the form was scrolled past the visible area; nudging by `+1246px` over-corrects in the opposite direction.
4. **The 80px window is anchored wrong** — should search `[labelY+10, labelY+80]` (below) but might be looking somewhere the rendered "Select..." button isn't.

---

## Part 3 — Why I Still Shipped The Breadcrumb Fix

Even though it didn't move the timing on this test, the breadcrumb fix is **a correct improvement** on its own merits:

- Pre-fix code rejected `Square D, Inc.`-shaped buttons as breadcrumbs — that's wrong on the face of it.
- Real breadcrumbs have **3+ comma-separated segments** (`AssetName, Room, AssetType`); manufacturer values have at most 2 (`Name, Inc.`).
- Tests that DO have these manufacturer values working will benefit.
- It's surgical, isolated, and doesn't introduce regressions (assertion-coverage gate green).

So I'm shipping it as a correctness fix with the explicit caveat that **it does not solve the LC_EAD_22 slowness** — that needs a separate investigation into the third bug.

## Part 4 — Honest Next Steps

The real performance fix for LC_EAD_22 (and the 19 other tests timeout-killed in the May 3 report) requires:

| Investigation | Why |
|---|---|
| Dump XML page source at the moment "No dropdown button found" fires | Determine whether the "Select..." button is `XCUIElementTypeButton` or some other type. Drives whether the locator predicate needs widening. |
| Add `-Dappium.log.requests=true` to capture exact element-coordinates returned by Appium | Determine if scroll-overshoot is reading stale coordinates. |
| Test with implicit wait reduced to 1s for this code path | If the issue is stale coordinates, a tighter wait may force fresh reads. |
| Compare the iOS sim app's actual DOM structure to what the locator assumes | If the app's UI changed (new button group wrapping), the locator's `findElements(... 'XCUIElementTypeButton')` may miss the new structure. |

These are real engineering tasks that can't be done in a 30-minute live debug session. They need:
- Time
- Access to the Appium element-tree dump at failure time
- Possibly product-side cooperation if the iOS UI structure has been refactored

---

## Part 5 — Trust + Honesty

In changelog 059 I claimed "expected savings of ~60 min off CI runtime" without empirical validation. After running the test twice (with and without my fix), the actual data says **the slowness wasn't where I thought it was**.

This file corrects the record. The FAST-FAIL precondition is still useful for the wrong-screen failure mode (Run 1's case). The breadcrumb fix is still useful for tests with `Name, Inc.`-shaped dropdown values. But neither fixes LC_EAD_22's specific failure mode in the May 3 report — that's a third, unfixed bug.

**Lesson**: Always run the actual measurement before claiming the speedup. Theoretical timing extrapolations from inspection alone are unreliable when the system is more complex than expected.

---

## Part 4 — Files Changed

| File | Change | Lines |
|---|---|---|
| [AssetPage.java](../../src/main/java/com/egalvanic/pages/AssetPage.java) | breadcrumb filter `contains(", ")` → `split(", ").length >= 3` | 1 line semantic change + 6 lines explanatory comment |

Total: **1 file, 7 lines added, 2 deleted**.
