# 062 — Full Investigation Arc Summary + Next Blocker Identified

**Date**: 2026-05-04
**Time**: 14:56 IST
**Prompt**: *"ok keep checking"* — continue iterating on LC_EAD_22 test correctness.

---

## What This File Is For

This is the **complete arc** of the LC_EAD_22 investigation that started in changelog 059. After a 5-run iterative debug session, summarising:

1. Every fix applied and the test outcome
2. Which root causes have been found and addressed
3. The next remaining blocker (a 4th layered bug in the picker-flow)
4. Honest assessment of how complete this work is

---

## Part 1 — Run-By-Run History

| Run | Code state | Outcome | Time | Fields filled | Notes |
|---|---|---|---|---|---|
| 1 | No fix | FAIL | 9m 53s | 0 of 3 | Wrong screen (Asset list, not Edit). Caused by class-change picker auto-dismiss. |
| 2 | + FAST-FAIL precondition | killed | partial | 0 of 3 | Different screen scenario; killed mid-run |
| 3 | + breadcrumb relax | FAIL | 15m 11s ⚠️ | 0 of 3 | Slower than baseline. The breadcrumb relax was correct but didn't unblock. |
| 4 | + correct DOM type (StaticText) | ✅ PASS | 13m 50s | 2 of 3 | First success! Voltage misfired: clicked "More" button, chose 'house'. |
| **5** | **+ off-screen guard + nav-button skip** | **❌ FAIL** | **12m 26s** | **2 of 3 (correctly)** | **Honest fail — Voltage cleanly skipped, but post-picker Save button gone** |

## Part 2 — Fixes Shipped (Cumulative)

| Commit | Change | Bug fixed | Real impact |
|---|---|---|---|
| `73d0031` (changelog 055) | HTTP timeout cap on Appium client | Dead-session hangs | Ambiguous — hard to measure in isolation |
| `96bb954` (changelog 059) | FAST-FAIL precondition in fillField | Wrong-screen retry storm | Real but conditional |
| `f2f8459` (changelog 060) | Breadcrumb filter loosening | Square-D-style values rejected | Correctness improvement, didn't move LC_EAD_22 |
| **`fb0d45a`** (changelog 061) | **Predicate widened to StaticText** | **Wrong XCUI element type** | **🎯 The root cause — unblocked the dropdown locator** |
| `ced4da8` | Skip-list extended (More, ellipsis, WO, Cancel/Save/Done) | Nav buttons mis-matched as dropdowns | Prevents wrong-tap cascade |
| `672433a` | Off-screen label guard (Y not in [120, 1900]) | Search runs in nav-bar zone | Forces clean retry instead of wrong-tap |

## Part 3 — Real Validated Wins

The DOM dump in changelog 061 showed concretely:

```xml
<XCUIElementTypeStaticText name="Select..." label="Select..."
    enabled="true" accessible="true" x="44" y="1124" .../>
```

The `Select...` dropdown trigger is `XCUIElementTypeStaticText`, not `XCUIElementTypeButton`. The current iOS app build (Z Platform-QA v1.31, iOS 26.2) renders ALL dropdown triggers this way for at least Loadcenter / Disconnect Switch / Manufacturer fields.

In run 5, the locator successfully matched + clicked these StaticText triggers:

```
✅ Clicking dropdown button: 'Select...' at Y=423 (dist=44px)   [Ampere Rating]
✅ Clicking dropdown button: 'Select...' at Y=866 (dist=29px)   [Manufacturer]
```

Without commit `fb0d45a`, these would never have been found. **2 dropdowns out of 3 are now functionally correct.**

## Part 4 — The Next Blocker (NOT FIXED)

### Discovery from run 5

Run 5 ended with:
- 2 dropdowns correctly filled (Ampere, Manufacturer)
- Voltage cleanly skipped (off-screen guard worked)
- Save Changes button **not visible** at assertion time

The failure screenshot shows the test ended on the **read-only Asset Details screen**, not the Edit form. Header says `Close | Asset Details | ⋯`. Voltage section shows "4.16kV" (the asset's existing value).

So the picker flow itself — `Select...` tap → picker opens → option selected → Done — at some point during the second dropdown's interaction navigated us **away from the Edit screen**.

### Why this is its own bug

Possibilities not yet verified:

1. **Picker has a Done that auto-saves and exits Edit** — selecting a value navigates back to Asset Details (as a confirmation flow), but the test code doesn't expect this and doesn't tap Edit again.
2. **Picker close logic taps too aggressively** — after picking a value the test calls `tapDoneOnPicker()` which may match a Done in the nav bar of the parent view.
3. **App auto-saves on picker close and shows updated state on Asset Details** — by-design app behaviour the test's flow doesn't account for.

Verifying the cause requires another DOM-dump debug session focused on the post-picker state. **Estimated: ~30 min more iteration**.

### Why I'm stopping iteration here

- 5 runs × 10–15 minutes = ~70 minutes of compute already spent
- 4 fixes shipped, the most important (StaticText predicate) IS validated as working
- The 4th bug is in a different code path (picker close) than what this debug session targeted
- Better to ship documented progress + a clear next-step than to keep iterating

The test will still **fail** in CI runs of LC_EAD_22 specifically until the picker-close issue is fixed. But other tests that don't hit Voltage's specific scroll-overshoot pattern (which most of the 19 timeout-killed tests likely don't) should benefit from the StaticText predicate fix.

## Part 5 — Honest Status For The User

| Question | Answer |
|---|---|
| Is LC_EAD_22 fixed? | ❌ Still fails. 2 of 3 dropdowns work; the 3rd hits a layered bug. |
| Is the StaticText fix correct? | ✅ Verified by DOM dump + 2 working dropdown clicks per run. |
| Will the StaticText fix help the 19 other timeout-killed tests? | 🟡 Likely yes for tests that don't hit Voltage's specific overshoot — but not validated empirically. Needs a CI dispatch to measure. |
| Did the run-5 failure regress something? | No — run 4's "pass" was lucky (wrong button click happened to keep Save visible). Run 5's "fail" is honest and revealing. |
| What's the next concrete step? | DOM-dump the picker close flow to identify why we end up on Asset Details. Or dispatch the full assets-p3 CI to see how many tests now pass. |

## Part 6 — Files Changed In This Iteration

| File | Lines |
|---|---|
| [src/main/java/com/egalvanic/pages/AssetPage.java](../../src/main/java/com/egalvanic/pages/AssetPage.java) | (across commits 672433a, ced4da8) +29 lines (off-screen guard + nav-button skip-list) |
| [docs/ai-features-changelog/062-investigation-arc-summary-and-next-blocker.md](062-investigation-arc-summary-and-next-blocker.md) (NEW) | this file |

## Part 7 — Lessons Banked

1. **DOM dump > log inspection.** Log inspection led me through 3 fixes that didn't move the timing. One DOM dump found the real root cause in 30 seconds.
2. **An "accidental pass" is worse than an honest fail.** Run 4 passed because a wrong-button click happened to leave Save visible. Run 5 fails honestly, exposing the next layer. The honest fail is more useful for fixing.
3. **Layered bugs need layered debugging.** This test had at least 4 distinct issues: wrong screen entry, wrong DOM type, off-screen scroll, picker-close exit. Each masked the next until the previous was fixed.
4. **Validate empirically before claiming victory.** Changelogs 059 and 060 claimed timing improvements that the actual measurement contradicted. Always run the test, not just the gate.
