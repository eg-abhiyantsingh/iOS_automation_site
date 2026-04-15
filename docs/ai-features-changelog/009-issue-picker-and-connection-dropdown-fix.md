# 009 — Issue Picker Hardening + Connection Dropdown Timing Fix

**Date:** 2026-04-15
**Time:** 15:30 IST
**Prompt:** "good progress but still few case are failling"
**Prior Fix Reference:** 008 — Keyboard timing + visibility constraint removal (14/27 passing)
**Scope:** IssuePage picker locators, ConnectionsPage dropdown scan timing
**Files Changed:**
- `src/main/java/com/egalvanic/pages/IssuePage.java`
- `src/main/java/com/egalvanic/pages/ConnectionsPage.java`

---

## Context

After fix 008 brought the smoke suite from 3/27 to 14/27 passing (run #24444064147), 4 non-S3 tests remained failing:

| Test | Error | Root Cause |
|------|-------|------------|
| **TC_CONN_037** | `fillAllConnectionFields()` returns false — Source Node: 0 assets found | Dropdown scan timing + coordinate filter too narrow |
| **TC_ISS_049** | Issue Class and Priority not selected — picker buttons not found | Case-sensitive locator + no keyboard dismiss before Priority |
| **TC_ISS_050** | Cascade — can't verify issue in list | Issue wasn't created (TC_ISS_049 failed) |
| **TC_ISS_076** | Cascade — can't delete issue | Issue wasn't created (TC_ISS_049 failed) |

---

## Fix 1: Harden `selectIssueClass()` (IssuePage.java) — 3-Strategy Picker + Retry

### Problem
The original `selectIssueClass()` (line 1039) used a single strategy:
```java
WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
    "type == 'XCUIElementTypeButton' AND name CONTAINS 'Issue Class'"));
```

Issues:
1. **Case-sensitive `CONTAINS`** — if the button's name property uses different casing, it misses
2. **Single strategy** — no fallback if name property doesn't contain "Issue Class"
3. **No retry** — menu items may not be loaded when first checked (animation delay)
4. **Silent failure** — exception caught and swallowed, test proceeds with unfilled field

### Solution
Rewrote to use the existing `tryOpenIssueClassPicker()` method (line 5018) which already has 3 robust strategies:
- **Strategy 1:** Button match with `name CONTAINS` + `label CONTAINS` (covers both properties)
- **Strategy 2:** Positional — find "Issue Class" label, then find nearby button (within 50px Y)
- **Strategy 3:** Coordinate tap — find label, tap at 3/4 screen width on same Y row

Option selection now retries up to 3 times with both exact match (`label == 'NEC Violation'`) and case-insensitive CONTAINS (`label CONTAINS[c] 'NEC Violation'`).

### Why This Existed
`selectIssueClass()` was written for the initial New Issue form flow. The robust `tryOpenIssueClassPicker()` was added later for the Issue Details edit screen. Same SwiftUI Picker component, but the New Issue method never got the hardening treatment.

---

## Fix 2: Harden `selectPriority()` (IssuePage.java) — Keyboard Dismiss + 3-Strategy Picker — CRITICAL

### Problem
`selectPriority("High")` is called at TC_ISS_049 Step 4, immediately after `enterIssueTitle()` at Step 3. `enterIssueTitle()` calls `sendKeys()` which **opens the iOS keyboard**. The original code:
```java
WebElement picker = driver.findElement(AppiumBy.iOSNsPredicateString(
    "type == 'XCUIElementTypeButton' AND name CONTAINS 'Priority'"));
```

This fails because:
1. **Keyboard is still open** — Priority picker button is behind the keyboard, marked `visible == false` in DOM
2. **Case-sensitive `CONTAINS`** — same fragility as Issue Class
3. **No fallback strategies** — single attempt, no retry

### Solution
Added `dismissKeyboard()` + 500ms wait before any picker search (same pattern proven in LoginPage fix 008). Then 3 strategies for opening the picker:

- **Strategy 1:** Direct button match: `name CONTAINS[c] 'priority' OR label CONTAINS[c] 'priority'`
- **Strategy 2:** Positional — find "Priority" static text label, search for nearby buttons with matching labels (None/High/Medium/Low/Priority) within 50px Y
- **Strategy 3:** Coordinate tap — find label Y position, tap at 3/4 screen width

Option selection: same 3-attempt retry with exact + CONTAINS[c] matching.

### The Keyboard Chain (TC_ISS_049)
```
Step 2: selectIssueClass("NEC Violation")  → no keyboard yet → works with better locators
Step 3: enterIssueTitle(title)             → sendKeys() → keyboard OPENS
Step 4: selectPriority("High")             → keyboard STILL OPEN → Priority behind keyboard
         ↳ dismissKeyboard() + 500ms wait  → keyboard dismissed → DOM refreshes → picker found
```

---

## Fix 3: Increase Dropdown Scan Timing (ConnectionsPage.java)

### Problem
`selectRandomSiblingAsset()` (line 2112) waited only 400ms after dropdown tap before scanning:
```java
sleep(400);  // Wait for dropdown to open
List<String> initialNames = scanDropdownAssetNames(headersToSkip);
```

CI run #24444064147 showed:
```
Source Node — Initial visible: 0 assets — []    ← 400ms wasn't enough
Target Node — Initial visible: 16 assets — [...]  ← ran later, dropdown had time
```

The dropdown's asset list populates asynchronously. On CI's slower simulator, 400ms isn't enough for the first dropdown interaction (iOS needs to fetch + render the asset hierarchy).

### Solution
1. Increased initial wait from 400ms to **800ms**
2. Added **retry on empty**: if first scan returns 0 assets, wait 1200ms and scan again
3. Total worst-case wait: 800ms + 1200ms = 2000ms (still well under the 420s test timeout)

```java
sleep(800);  // Was 400ms — doubled for CI
List<String> initialNames = scanDropdownAssetNames(headersToSkip);
if (initialNames.isEmpty()) {
    System.out.println("   ⚠️ Initial scan found 0 assets — retrying after 1200ms...");
    sleep(1200);
    initialNames = scanDropdownAssetNames(headersToSkip);
}
```

---

## Fix 4: Widen Coordinate Filter in `scanDropdownAssetNames()` (ConnectionsPage.java)

### Problem
The X/Y coordinate filter was too restrictive:
```java
if (loc.getX() >= 30 && loc.getX() <= 90 && loc.getY() >= 280) {
```

- **X: 30-90** — asset name text may render at different X positions on different screen sizes
- **Y >= 280** — on some layouts, the first dropdown item appears above Y=280

### Solution
Widened to X: 15-200, Y >= 230:
```java
if (loc.getX() >= 15 && loc.getX() <= 200 && loc.getY() >= 230) {
```

Added diagnostic logging when all candidates are rejected by the coordinate filter — enables debugging if the issue recurs on a different device size.

### Why Wider X Is Safe
The dropdown renders each asset as two lines:
- Name text (e.g., "Disconnect Switch 1") at Y=348
- Type text (e.g., "switch") at Y=375 — gap ~27px

The Y-gap grouping (threshold: 32px) merges these into one asset, keeping only the first (name). Wider X may capture type text, but the grouping prevents it from being counted as a separate asset.

---

## Expected Impact

| Test | Before (run #24444064147) | After (expected) |
|------|---------------------------|-------------------|
| TC_CONN_037 | FAIL — Source Node 0 assets | PASS (timing + retry) |
| TC_ISS_049 | FAIL — Issue Class/Priority not found | PASS (keyboard dismiss + 3-strategy) |
| TC_ISS_050 | FAIL — cascade from TC_ISS_049 | PASS (issue now created) |
| TC_ISS_076 | FAIL — cascade from TC_ISS_049 | PASS (issue now created) |

**Expected total: 18/27 pass** (14 current + 4 fixed), 8 fail (S3 infrastructure), 1 skip (BUG_DELETE_01 — already fixed in XML, TBD)

---

## Technical Pattern: SwiftUI Picker Locator Robustness

All iOS picker failures in this project follow the same pattern. Here's the robustness checklist applied to every picker fix:

| Check | selectIssueClass (old) | selectIssueClass (new) | selectPriority (new) |
|-------|----------------------|----------------------|---------------------|
| Case-insensitive matching | No (`CONTAINS`) | Yes (`CONTAINS[c]`) | Yes (`CONTAINS[c]`) |
| Multiple name/label properties | No (name only) | Yes (name + label) | Yes (name + label) |
| Positional fallback | No | Yes (label Y ± 50px) | Yes (label Y ± 50px) |
| Coordinate tap fallback | No | Yes (3/4 screen width) | Yes (3/4 screen width) |
| Keyboard dismiss | No | Yes (safety) | Yes (CRITICAL) |
| DOM refresh wait | No | Yes (500ms) | Yes (500ms) |
| Option retry | No | Yes (3 attempts) | Yes (3 attempts) |
| Exact + CONTAINS match | Exact only | Both | Both |
