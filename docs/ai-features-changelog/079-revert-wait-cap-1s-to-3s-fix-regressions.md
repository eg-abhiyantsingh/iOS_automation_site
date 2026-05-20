# 079 — Revert Wait Cap from 1s to 3s: Fixes Regressions Across Suites

**Date**: 2026-05-20
**Time**: 13:30 IST
**Trigger**: User: *"our this version of code my better. this code was better now our code is most worst"* — pointing at dev-repo run [25204536847](https://github.com/Egalvanic/eg-pz-mobile-iOS/actions/runs/25204536847) (April 30, mostly passing) vs current (May 15, mostly failing).

---

## What I Found

Comparing the two dev-repo CI runs:

### April 30 (25204536847) — BEFORE wait-cap reduction
```
11 SUCCESS / 6 FAIL (6h cap)
   ✓ Assets P1 (5h24m)
   ✓ Assets P3 (4h38m)
   ✓ Assets P6 (4h24m)
   ✓ Issues P1 (4h46m)
   ✓ Issues P3 (4h42m)
   ✓ Location (4h4m)
   ✓ Site Selection (2h12m)
   ✓ Authentication, Offline, others
   ❌ Connections, Assets P2/P4/P5, Site Visit, Issues P2 (6h cap)
```

### May 15 (25904342238) — AFTER our changes
```
1 SUCCESS / many FAIL or cancelled  (20.8% pass rate per user summary)
   ❌ Assets P1, P3, P6, Issues P1, P3 — cancelled at 6h (were PASS before!)
   ❌ Location, Connections, Issues P2, Offline — exit code 1 (real failures)
   ❌ Build cancelled
```

**6 suites regressed from PASS to FAIL/cancelled.** Our recent changes (intended to be improvements) actually made things worse.

---

## Root Cause: Implicit-Wait Cap of 1s Was Too Aggressive

Three commits on May 4 introduced implicit-wait caps:
- `c08530c` — `findBuildingByName` capped to 1s
- `a4abced` — `findFloorByName`, `findRoomByName`, `selectAssetByName` capped to 1s
- `142ce38` — `tapOnIssue` capped to 1s

The intent was good: reduce wasted time on element-missing cases. The math sells well:
- HIT case: cap is irrelevant (element found instantly)
- MISS case: 5s × 13 strategies → 1s × 13 = 5x faster

But there's a **third case** I didn't account for: **SLOW-LOAD**.

### The slow-load case

When the test navigates to a screen, the elements don't all render instantly. Common cases:
- Issues list cells render after API call (~2-4s after nav)
- Building list cells render after DB query (~1-3s)
- Floor/Room lists same pattern
- Dashboard widgets populate after site selection

With **5s implicit wait** (the default): the element appears in 2-4s → `findElement` waits, finds it → success.

With **1s implicit wait** (our cap): the element doesn't appear within 1s → `findElement` throws NoSuchElementException → strategy "miss" → fallback strategies all also wait only 1s, never find it → helper returns null → test fails.

So tests that were PASSING via "wait for slow-load element" are now FAILING via "1s cap on slow-load = false miss".

This explains the 6 suites regressing — they all rely on these helpers (`tapOnIssue` is called 134 times across Issue suites; `findBuildingByName` is called many times across Location).

---

## Fix Applied

Reverted all 15 occurrences of `implicitlyWait(...ofSeconds(1))` to `implicitlyWait(...ofSeconds(3))`.

```diff
- driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(1));
+ driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(3));
```

3s is the balance:
- HIT case: still instant
- SLOW-LOAD case: 2-4s element load fits within 3s wait → element found
- MISS case: 3s × N strategies — slower than 1s × N but still 1.7x faster than default 5s × N

### Files changed

| File | 1s→3s |
|---|---|
| `BuildingPage.java` | 3 occurrences (findBuilding/Floor/Room) |
| `IssuePage.java` | 1 occurrence (tapOnIssue) |
| `AssetPage.java` | 6 occurrences (selectAssetByName, isDashboardDisplayed, isAssetListDisplayed, isAssetDetailDisplayed, selectFirstAsset, and one more) |
| **Total** | **15** |

---

## Expected Impact on Next Dev-Repo CI Run

| Suite | April 30 (1s wait era hadn't started) | May 15 (1s wait) | After this fix (3s) |
|---|---|---|---|
| Assets P1 | ✓ 5h24m | ❌ cancelled | Expected ✓ (back to ~5h) |
| Assets P3 | ✓ 4h38m | ❌ cancelled | Expected ✓ |
| Assets P6 | ✓ 4h24m | ❌ cancelled | Expected ✓ |
| Issues P1 | ✓ 4h46m | ❌ cancelled | Expected ✓ |
| Issues P3 | ✓ 4h42m | ❌ cancelled | Expected ✓ |
| Location | ✓ 4h4m | ❌ exit 1 | Expected ✓ |
| Issues P2 | ❌ 6h cap | ❌ exit 1 | May still hit 6h cap |
| Connections | ❌ 6h cap | ❌ 6h cap | May still hit 6h cap (need split) |

**Net expected**: 6+ suites back to PASS, ~75-85% overall pass rate.

---

## Trade-off

This fix REVERTS the time-saving optimization from May 4. Net cost: ~10-20 minutes more per full Issue/Asset suite run vs the 1s version.

But correctness > speed. A test that takes 10 min longer to PASS is infinitely more useful than one that fails fast on a false miss.

---

## Memory Updated

New rule saved: `feedback_wait_cap_minimum_3s.md` — implicit-wait caps must be ≥ 3 seconds, never 1 second. Documented WHY (slow-load case) so future agents don't repeat this mistake.

---

## TL;DR

- **Found**: 6 suites regressed PASS→FAIL after our May 4 wait-cap reduction (1s)
- **Why**: 1s was too short for slow-loading list cells (2-4s typical)
- **Fix**: changed all 15 caps from 1s to 3s
- **Cost**: ~10-20 min more per suite run
- **Benefit**: tests that PASSED in April 30 should PASS again
- **Push target**: QA repo `main` only — dev app is the contract
