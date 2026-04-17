# Changelog 024 — Developer Repo CI Failure Analysis (Part 2: Connections + Assets P5)

**Date**: 2026-04-17  
**Time**: ~06:45 IST  
**CI Run**: https://github.com/Egalvanic/eg-pz-mobile-iOS/actions/runs/24513764702  
**Prompt**: "check all the current fail test case Quality is more important than quantity... always go in deeper"

---

## Overview

Continuation of the deep CI failure analysis from changelog 023. This part covers **Connections** and **Assets P5**, which completed during monitoring.

| Job | Tests | Passed | Failed | Skipped | Runtime | Status |
|-----|:-----:|:------:|:------:|:-------:|:-------:|--------|
| Connections | 93 | 59 | 5 | 29 | 143min | 3 assertions + 2 TIMEOUTS + 29 precondition skips |
| Assets P5 | 100 | 92 | 8 | 0 | 293min | 4 TIMEOUTS + 4 assertions (survived 365min!) |

**Cumulative (13 jobs analyzed)**: 544 tests, 389 passed, 66 failed, 89 skipped

**Still running**: Assets P4 (97), Issues P1 (119), Issues P3 (58)

---

## Module 7: Connections (93 tests — 59 passed, 5 failed, 29 skipped)

### Failure Summary

| # | Test Case | Duration | Type | Root Cause |
|---|-----------|----------|------|------------|
| 1 | TC_CONN_005 | 44.7s | Assertion | Arrow "→" is an icon/image, not Unicode text |
| 2 | TC_CONN_024 | 111.6s | Assertion | `tapOnSourceNodeDropdown()` can't reopen after selection |
| 3 | TC_CONN_036 | 138.5s | Assertion | Same asset (Trim048785) selected for Source AND Target |
| 4 | TC_CONN_059 | 420.0s | TIMEOUT | Appium hang on keyboard element search |
| 5 | TC_CONN_062 | 420.0s | TIMEOUT | Appium hang during rapid connection creation |

### The 29 Skips — NOT Cascade Failures

All 29 skipped tests (TC_CONN_048, TC_CONN_057, TC_CONN_069–TC_CONN_096) are **precondition-based skips**, not cascade skips from a dead session. Every skip message says:
- "No connections to test" / "No connections available"
- "Could not enter selection mode" / "Could not access Edit option"

These tests require existing connections in the list, but the environment doesn't have pre-created connections, and earlier creation tests (TC_CONN_036 et al.) either failed or didn't produce persistent connections. This is **intentional defensive test design** — skip gracefully instead of failing with NullPointerException.

### Failure 1: TC_CONN_005 — Arrow Format Check

**What the test does**: Verifies connection entries show "Source Node → Target Node" format.

**Screenshot evidence**: The Connections screen shows `Trim011378  →  Missing Node  ⚠️` — a connection with a red-styled arrow and a "Missing Node" warning.

**Why it failed**: `doesConnectionShowSourceToTargetFormat()` in ConnectionsPage.java:535 searches for Unicode arrow characters (`→`, `->`, `—`, `➜`) in element labels. But the arrow in the app is rendered as a **red-colored styled icon** (SF Symbol or custom image view), not as a Unicode text character. The method then falls back to searching for ANY element with arrow characters via `findElements` — but images don't have text labels matching arrow chars.

**Secondary issue**: The "Missing Node" text with ⚠️ indicates the target asset was deleted or isn't accessible in this environment. Even if the arrow detection worked, the test might fail on format validation since "Missing Node" isn't a real asset name.

**Fix approach**: `doesConnectionShowSourceToTargetFormat()` should also check for the arrow as a separate `XCUIElementTypeImage` or `XCUIElementTypeOther` positioned between two `XCUIElementTypeStaticText` elements. Alternatively, check that the connection cell contains at least 2 asset-name-like static texts.

### Failure 2: TC_CONN_024 — Can't Reopen Source Node Dropdown

**What the test does**: Selects a Source Node, then tries to tap Source Node again to change the selection.

**Screenshot evidence**: New Connection screen shows "Trim048785" selected as Source Node with its hierarchy path. Target Node shows "Select target node" placeholder.

**Why it failed**: After selecting an asset, `tapOnSourceNodeDropdown()` (ConnectionsPage.java:1590) tries 3 strategies — all designed for the **pre-selection state**:

| Strategy | Predicate | Why it fails post-selection |
|----------|-----------|---------------------------|
| 1 | `label CONTAINS 'Select source'` | Label changed to "Trim048785" |
| 2 | `label == 'Source Node' AND type == 'XCUIElementTypeButton'` | The field might not match "Source Node" as its label anymore |
| 3 | `type == 'XCUIElementTypeCell' AND label CONTAINS[cd] 'source'` | Cell label is now the asset name, not "source" |

**Fix approach**: Add a Strategy 0 (highest priority) that finds the Source Node **section container** by its static label "Source Node" (which never changes), then taps the **adjacent cell** or the first tappable child element below it.

### Failure 3: TC_CONN_036 — Source and Target Are the Same Asset

**What the test does**: Fills all connection fields with random sibling assets (different from each other) and verifies Create button enables.

**Screenshot evidence**: Target node picker showing a list of assets including Trim011378, Trim035514, Trim048785, etc. The assertion says `Source=Trim048785, Target=Trim048785`.

**Why it failed**: `selectRandomSiblingAsset(excludeForTarget, excludeNames036)` in ConnectionsPage.java:2112 was supposed to exclude the source asset by both index AND name. But it selected Trim048785 for both. Two possible causes:

1. **Y-coordinate drift between dropdowns**: The source dropdown and target dropdown may render the same assets at different Y-positions (scrolling state, animation). The index is based on Y-position grouping (32px threshold), so the same physical asset could get a different index in the target dropdown, bypassing `excludeIndices`.

2. **Name matching failure**: `getLastSelectedAssetName()` might return a differently-formatted string than what `scanDropdownAssetNames()` extracts — e.g., "Trim048785" vs "Trim048785\nBldg_9471 > Floor 77..." (with path info included). If the name stored includes the hierarchy path, it won't match the plain name in the dropdown, bypassing `excludeNames`.

**Fix approach**: The name exclusion should use `CONTAINS` matching instead of exact equals. Also, after target selection, add a final safety assertion that compares `getSelectedSourceNodeText()` vs `getSelectedTargetNodeText()` and retries with a different asset if they match.

### Failures 4–5: TC_CONN_059 + TC_CONN_062 — Appium Timeouts

Both hit the 420s suite-level `time-out`. Same root cause as all other timeout failures in this CI run.

**TC_CONN_059** (keyboard dismiss): Screenshot shows New Connection screen with Source Node selected but Target empty. The test was interacting with the search field and keyboard — likely hung on `isKeyboardDisplayed()` which uses `findElements(AppiumBy.className("XCUIElementTypeKey"))` — a broad element search that can hang.

**TC_CONN_062** (rapid creation): Screenshot shows "Source node has no source-eligible terminals" warning. The test creates 3 connections back-to-back, calling `fillAllConnectionFields()` each time — each invocation opens dropdowns, selects assets, and picks types. With many `findElements` calls per creation, the probability of hitting an Appium hang multiplies.

**Impact of ThreadTimeoutException fix (changelog 023)**: These 2 timeouts did NOT cause cascade skips — the session recovered afterward. The fix still helps by preventing teardown hangs if the session DOESN'T recover.

---

## Module 8: Assets P5 — UPS + Utility + VFD + Subtypes (100 tests — 92 passed, 8 failed)

### Failure Summary

| # | Test Case | Duration | Type | Root Cause |
|---|-----------|----------|------|------------|
| 1 | TC_UPS_01 | 420.0s | TIMEOUT | Stuck on Asset Class picker while selecting UPS |
| 2 | TC_UTL_06 | 420.0s | TIMEOUT | Appium hang on MCC asset details (wrong asset) |
| 3 | TC_CB_ST_09 | 420.0s | TIMEOUT | Navigation went to Task Details screen |
| 4 | TC_DS_ST_13 | 420.0s | TIMEOUT | Appium hang during subtype persistence check |
| 5 | TC_ATS_ST_09 | 390.4s | Assertion | No ATS asset in test data — fell back to MCC |
| 6 | TC_ATS_ST_10 | 330.7s | Assertion | Dependent on TC_ATS_ST_09 (same MCC fallback) |
| 7 | TC_DS_ST_01 | 51.3s | Assertion | No Disconnect Switch asset — found Circuit Breaker |
| 8 | TC_DS_ST_12 | 376.1s | Assertion | No Disconnect Switch — wrong asset for subtype test |

### Pattern A: Test Data Mismatch (4 assertions)

**TC_ATS_ST_09, TC_ATS_ST_10**: Tests look for an ATS (Automatic Transfer Switch) asset. Screenshot shows Asset Details for Trim011378 with class **MCC** — no ATS exists. The test falls back to the first available asset, selects a subtype, then asserts "Save button should appear after selecting a different subtype". But on an MCC asset, the subtype selection behavior might not trigger the Save button.

**TC_DS_ST_01**: Asserts "Asset Subtype dropdown should be visible for Disconnect Switch". Screenshot shows Asset Details with subtype "Low-Voltage Insulated Case Circuit Breaker" — the test found a Circuit Breaker asset, not a Disconnect Switch. The dropdown IS visible, but for the wrong asset class. The assertion specifically checks that the **current** asset class is Disconnect Switch before checking dropdown visibility.

**TC_DS_ST_12**: Same pattern. The test navigated to an asset but it's not a Disconnect Switch. Screenshots show Core Attributes section saying "No node class defined" — confirming the wrong asset class was loaded.

**Root cause**: The CI environment's test data was created with generic "Trim" + random number naming (Trim011378, Trim035514, Trim048785, etc.) — all appear to be in the **same room** (Room_1767700402598) under the **same location** (Bldg_9471 > Floor 77 Updated_119). The asset classes present are primarily MCC, with some Circuit Breakers. No ATS or Disconnect Switch assets were created in this environment.

### Pattern B: Navigation Drift (TC_CB_ST_09)

Screenshot shows **Task Details** screen (with "Associated Forms", "Task Photos", "Resolution: Pending", "Delete Task"). The test should be on an Asset Details screen changing Circuit Breaker subtypes. The app navigated to a task instead of staying on the asset. This suggests:
- A tap on a task card within the asset's Tasks section accidentally navigated away
- The test's navigation helper (`navigateToAssetDetails`) returned the wrong screen

### Pattern C: Appium Timeouts (4 timeouts)

All 4 hit the 420s suite `time-out`. Same intermittent Appium hang pattern:
- TC_UPS_01: Stuck on Asset Class picker (screenshot shows picker open) — `findElement` for "UPS" row may have hung
- TC_UTL_06: Stuck on MCC asset details — likely hung on a scroll or field search
- TC_CB_ST_09: After drifting to Task Details, likely hung trying to find asset elements on the wrong screen
- TC_DS_ST_13: Hung during subtype persistence check after the preceding test (TC_DS_ST_12) already failed

**Key observation**: Assets P5 ran for **293 minutes** with only 4 timeouts and survived the 365-minute kill. Compare with Assets P6/P2/P1 that all died at 365 minutes — those likely had MORE timeouts that exhausted the TestNG thread pool.

---

## Cross-Module Pattern Summary (Updated)

### Timeout Analysis Across All Modules

| Module | Timeouts | Cascade Skips | Survived 365min? |
|--------|:--------:|:-------------:|:-----------------:|
| Location | 5 | **60** | Yes (142min) |
| Issues P2 | 18 | 0 | Yes (349min) |
| Offline | 10 | 0 | Yes (121min) |
| Assets P3 | 5 | 0 | Yes (311min) |
| Assets P5 | 4 | 0 | Yes (293min) |
| Connections | 2 | 0 | Yes (143min) |
| Assets P6 | ? | ? | **NO (365min KILL)** |
| Assets P2 | ? | ? | **NO (365min KILL)** |
| Assets P1 | ? | ? | **NO (365min KILL)** |

**Key insight**: Location was the only module with cascade skips because its timeouts were **permanent** (session never recovered). All other modules had **transient** hangs (session recovered for teardown). The `isSessionLikelyDead()` fix (changelog 023) targets the permanent-hang pattern.

### Test Data Issues (CI-Specific)

The CI environment lacks several asset types that tests expect:
- **No ATS** (Automatic Transfer Switch) assets → TC_ATS_ST_09, TC_ATS_ST_10 fail
- **No Disconnect Switch** assets → TC_DS_ST_01, TC_DS_ST_12, TC_DS_ST_13 affected
- **No UPS** assets accessible quickly → TC_UPS_01 stuck on picker
- **No "Abhiyant" issue** → Issues module uses fallback strategies (from changelog 023)
- **Connections show "Missing Node"** → Target assets were deleted or inaccessible

These are **environmental** failures, not code bugs. The test framework handles them gracefully (fallback strategies, skip with message) but can't produce meaningful pass results.

---

## Files Analyzed (Not Modified — Never Push to Developer Repo)

| File | Relevance |
|------|-----------|
| `Connections_Test.java` lines 423-469, 2025-2110, 3253-3333, 4922-4943, 5183-5263 | All 5 failing test methods |
| `ConnectionsPage.java` lines 535-565, 1590-1632, 2099-2138 | Format check, dropdown tap, random selection |
| Test report `/tmp/connections-report-dev/target/surefire-reports/testng-results.xml` | 93 test results |
| Test report `/tmp/assets-p5-report-dev/target/surefire-reports/testng-results.xml` | 100 test results |
| 13 failure screenshots analyzed | Visual evidence for root cause |

---

## Recommended Fixes (For QA Automation Repo Only)

### Priority 1 — Code Fixes

1. **`tapOnSourceNodeDropdown()` post-selection strategy**: Add a strategy that finds the "Source Node" label, then taps the cell/element directly below it regardless of its current label text.

2. **`selectRandomSiblingAsset()` name comparison**: Use `contains` matching instead of exact `equals` to handle cases where stored name includes hierarchy path.

3. **`doesConnectionShowSourceToTargetFormat()` icon detection**: Add fallback that checks for image/icon elements between two static text elements, not just Unicode arrow characters.

### Priority 2 — Test Data Resilience

4. **Skip-with-reason for missing asset types**: Before Subtype tests, check if the current asset class matches the expected type. If not, skip with a message like "No {ATS/Disconnect Switch} asset found in test data" rather than proceeding with wrong asset.

### Priority 3 — CI Environment

5. **Ensure CI test data includes all asset types**: The CI environment should have at least one asset of each type tested (ATS, Disconnect Switch, UPS, Utility, VFD, Circuit Breaker).

---

## Status

| Item | Status |
|------|--------|
| Connections deep analysis | **COMPLETE** |
| Assets P5 deep analysis | **COMPLETE** |
| Assets P4 (97 tests) | **STILL RUNNING** (~165min elapsed) |
| Issues P1 (119 tests) | **STILL RUNNING** (~75min elapsed) |
| Issues P3 (58 tests) | **STILL RUNNING** (~215min elapsed) |
