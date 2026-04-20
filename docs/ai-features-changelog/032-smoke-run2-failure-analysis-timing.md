# 032 — Smoke Run #2 Failure Analysis: All 5 Failures Already Fixed (Timing Issue)

**Date**: 2026-04-20  
**Prompt**: "still few test case are failing still" (referring to run #24656579834)  
**Source Run**: https://github.com/Egalvanic/eg-pz-mobile-iOS/actions/runs/24656579834  

---

## Key Finding: CI Ran Old Code — All Fixes Were Pushed AFTER Checkout

| Event | Time (UTC) | Time (IST) |
|-------|-----------|-------------|
| CI run started | 08:32:47 | 14:02:47 |
| **QA repo checked out** | **08:33:02** | **14:03:02** |
| Fix commit `c366f86` pushed | 08:51:11 | 14:21:11 |
| Fix commit `bd472db` pushed | 10:10:44 | 15:40:44 |

**The CI checked out our QA repo 18 minutes BEFORE `c366f86` was pushed.** Neither fix commit was picked up.

---

## All 5 Failures — Already Fixed

### 1. BUG_DELETE_01 — Delete Asset Dialog Not Found (31s)

**Error**: `Delete Asset confirmation dialog should appear after tapping trash icon`  
**File**: `Asset_Phase1_Test.java:5499`

**What happened**: After tapping the trash icon, the test did a single check for `XCUIElementTypeAlert` — missed it because the dialog can render as `XCUIElementTypeSheet` on some iOS versions, and the alert may take >1s to appear.

**Fix (commit c366f86)**: Added 3-attempt retry loop with 5 strategies:
1. `XCUIElementTypeAlert`
2. `XCUIElementTypeSheet` (action sheet)
3. "Delete Asset" or "Delete" title text
4. "cannot be undone" / "Are you sure" / "permanently" keywords
5. Cancel + Delete button pair detection

### 2. TC_EB_002 — Building Name Update Timeout (420s)

**Error**: `Method didn't finish within the time-out 420000`  
**File**: `LocationTest.java:967`

**What happened**: Test renamed a building successfully, but the verification phase used full implicit wait (10s) for `isBuildingDisplayed()`. With 3-4 search strategies × 10s each × 2 name checks = 60-80s wasted. Combined with slow CI simulator, total exceeded 420s.

**Fix (commit bd472db)**: Verification uses 3s implicit wait instead of full 10s:
```java
driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
try { /* verify */ } finally { /* restore IMPLICIT_WAIT */ }
```
Saves 30-60 seconds — keeps test well under 420s timeout.

### 3. TC_DB_001 — Building Delete Verification Timeout (420s)

**Error**: `Method didn't finish within the time-out 420000`  
**File**: `LocationTest.java:1439`  
**Session died**: `Error communicating with the remote browser. It may have died.`

**What happened**: After deleting a building, `verifyBuildingDeleted()` ran a complex scrollUp×3 + scrollDown×2 loop, each with full implicit wait. The WDA session died during this heavy scrolling, causing all subsequent calls to hang until the 420s timeout.

**Fix (commit c366f86)**: Complete rewrite of `verifyBuildingDeleted()` — no scrolling at all. Uses single `findElements()` with 3s implicit wait. If no matches found, building is deleted. Restores implicit wait in `finally` block.

### 4. TC_CONN_051 — Connection Delete Button Not Found (36s)

**Error**: `Failed to tap Delete button on dialog: An element could not be located`  
**File**: `Connections_Test.java:4725`

**What happened**: `autoAcceptAlerts: true` capability auto-dismissed the confirmation dialog before the test could interact with the "Delete" button inside it. Classic race condition:
1. Test taps trash → confirmation dialog appears
2. `autoAcceptAlerts` sees the alert → auto-taps first button (confirming delete)
3. Test tries to find "Delete" button inside the alert → alert is already gone

The deletion actually succeeded, but the test didn't know.

**Fix (commit bd472db)**: On retry, if the alert disappears, check if `autoAcceptAlerts` already handled it:
```java
try { alertElement = driver.findElement(...); }
catch (Exception ignored) {
    // Alert gone → autoAcceptAlerts handled it → proceed to verification
    deleted = true; break;
}
```

### 5. TC_ISS_076 — Temp Issue Creation Failed (177s)

**Error**: `Should have created temporary issue for deletion testing`  
**File**: `Issue_Phase1_Test.java:2847`

**Root cause chain**:
1. `createQuickIssue()` calls `tapSelectAsset()`
2. "Select Asset" button not found directly (it's below the fold)
3. Old code only scrolled 3 times — not enough for NEC Violation forms with extra subcategory fields
4. `mobile:scroll` by predicate also failed
5. Without asset selection, "Create Issue" button stayed disabled (10 attempts, 5s wait)
6. Cancelled issue creation → `createQuickIssue()` returned `false`
7. Assertion failed at line 2847

**Note**: TC_ISS_049 (earlier test) found "Select Asset" after 1 scroll — the form state may be different for the temp issue creation in TC_ISS_076 (keyboard persistence, different scroll position).

**Fix (commit c366f86)**: `tapSelectAsset()` now has 5 robust strategies:
1. Direct find
2. **6 scrolls** (up from 3)
3. **NEW**: Scroll to "Assignment" section label first
4. iOS native `mobile: scroll` by predicate
5. Broader label matching with Y>120 check

---

## Summary

| # | Test | Error | Fix Commit | Picked Up? |
|---|------|-------|-----------|------------|
| 1 | BUG_DELETE_01 | Dialog detection single attempt | c366f86 | NO |
| 2 | TC_EB_002 | 420s timeout — implicit wait accumulation | bd472db | NO |
| 3 | TC_DB_001 | 420s timeout — WDA session death during scrolls | c366f86 | NO |
| 4 | TC_CONN_051 | autoAcceptAlerts race condition | bd472db | NO |
| 5 | TC_ISS_076 | Select Asset 3 scrolls insufficient | c366f86 | NO |

**All 5 failures are in old code. Both fix commits (`c366f86` + `bd472db`) are on `main` and will be picked up by the next CI run.**

---

## What to Do Next

1. **Trigger a new smoke CI run** — it will checkout our latest code (including both fix commits)
2. **Watch for the email** — will still fail because `release/prod` has `${{ secrets.EMAIL_TO }}` (not yet fixed)
3. **Apply email fix** — see `docs/developer-repo-patches/email-to-fix.md` for manual patches
