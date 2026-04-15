# Changelog 012 — Fix Offline Test Failures: Keyboard Occlusion + Timeout

**Date**: 2026-04-15  
**Time**: ~19:30 IST  
**Prompt**: Investigate CI run #24449855754 (parallel tests on release/prod) — fix failing test cases  
**CI Run**: https://github.com/Egalvanic/eg-pz-mobile-iOS/actions/runs/24449855754  
**Part**: 1 of N (Offline module fixes — other modules still running)

---

## Summary

Fixed **3 test failures** in the Offline module (OfflineTest.java):
- **TC_OFF_008** — ThreadTimeoutException (420s) in `ensureOnlineState()`
- **TC_OFF_014** — AssertionError "Create Issue should be tapped successfully"
- **TC_OFF_035** — ThreadTimeoutException (420s) in `createBuilding()`

All 3 share a common theme: **missing keyboard dismissal** after text input, combined with element lookups that hang under slow Appium sessions.

---

## Failure 1: TC_OFF_008 — `ensureOnlineState()` Timeout

### What Happened
After verifying locations are available offline, the test calls `ensureOnlineState()` at line 817 (cleanup). Inside, at line 409, it calls:
```java
d.findElement(AppiumBy.accessibilityId("Wi-Fi Off")).isDisplayed();
```
When the app is already online, this element doesn't exist. `findElement()` honours Appium's **implicit wait** (30-60s), and when Appium/WDA is sluggish after offline-mode toggling, the call hangs indefinitely. The TestNG method timeout (420s) is hit.

### Root Cause
`findElement()` blocks until the element appears or the implicit wait expires. After toggling airplane mode, the Appium session can be slow to respond, turning a 30-60s wait into a 420s+ hang.

### Fix
**File**: `src/test/java/com/egalvanic/tests/OfflineTest.java` (line 409)

Replaced `findElement` with `findElements`:
```java
// BEFORE (hangs on implicit wait when element doesn't exist):
definitelyOffline = d.findElement(
    AppiumBy.accessibilityId("Wi-Fi Off")).isDisplayed();

// AFTER (returns empty list immediately — no implicit wait):
java.util.List<org.openqa.selenium.WebElement> offlineIcons =
    d.findElements(AppiumBy.accessibilityId("Wi-Fi Off"));
definitelyOffline = !offlineIcons.isEmpty() && offlineIcons.get(0).isDisplayed();
```

### Why This Works
`findElements()` returns an empty list immediately when no elements match — it does NOT wait for the implicit timeout. This is a well-known Selenium/Appium pattern for "check if element exists without waiting."

---

## Failure 2: TC_OFF_014 — Issue Creation Button Stayed Disabled

### What Happened
The test creates an issue in offline mode:
1. Select issue class: NEC Violation (OK)
2. Enter issue title (OK — but **keyboard stays open**)
3. Tap Select Asset (FAILS — keyboard covers the asset section)
4. Select first asset (FAILS — not on asset picker screen)
5. Tap Create Issue → button disabled → returns false → assertion fails

### Screenshot Evidence
The screenshot shows the "New Issue" form with:
- Classification: NEC Violation (filled)
- Title: OffIssue_1776255631470Sd (filled)
- Keyboard: **VISIBLE** (covering bottom of form)
- No asset selected (the "Select Asset" row is below the keyboard)

### Root Cause
`enterIssueTitle()` at IssuePage.java:1110 calls `sendKeys()` which opens the keyboard. The test immediately calls `tapSelectAsset()` without dismissing the keyboard. The "Select Asset" element is in the ASSIGNMENT section at the **bottom** of the form — hidden behind the keyboard.

Notably, `selectPriority()` (IssuePage.java:1126) correctly calls `dismissKeyboard()` before interacting. But OfflineTest skips Priority and goes directly to asset selection.

### Fix
**File**: `src/test/java/com/egalvanic/tests/OfflineTest.java` (before line 1363)

Added keyboard dismissal before asset selection:
```java
// CRITICAL: Dismiss keyboard left open by enterIssueTitle().sendKeys()
logStep("Dismissing keyboard before asset selection");
try {
    io.appium.java_client.ios.IOSDriver d = DriverManager.getDriver();
    d.executeScript("mobile: hideKeyboard");
    shortWait();
} catch (Exception e) {
    logWarning("Could not dismiss keyboard: " + e.getMessage());
}
```

### Why This Works
`mobile: hideKeyboard` is the Appium iOS-native command to dismiss the keyboard. Once dismissed, `tapSelectAsset()` can scroll down and find the "Select Asset" element in the ASSIGNMENT section. With an asset selected, the "Create Issue" button becomes enabled.

---

## Failure 3: TC_OFF_035 — `createBuilding()` Save Timeout

### What Happened
The test creates 3 buildings offline to verify operation queueing. Building 1 succeeds. Building 2 fails at `clickSave()` with a 420s ThreadTimeoutException.

Inside `SiteSelectionPage.createBuilding()`:
1. `clickAddButton()` — opens "New Building" form (OK)
2. `enterBuildingName(name)` — uses `sendKeys()` → **keyboard opens**
3. `waitForElementToBeClickable(saveButton, 2)` — may work
4. `clickSave()` → `click(saveButton)` → `waitForClickable(element, 10)` → **HANGS**

### Screenshot Evidence
"New Building" form showing "Multi_Bldg2_1776259480982" with keyboard visible. Save button is at top-right (nav bar area).

### Root Cause (Deep)
The `saveButton` is defined via `@iOSXCUITFindBy(accessibility = "Save")` — a page factory proxy. When `click()` calls `waitForClickable(element, 10)`, the `elementToBeClickable` condition checks `element.isDisplayed()`. This triggers the page factory proxy's internal element lookup, which goes through:

1. Outer `WebDriverWait` (10s timeout) in `waitForClickable`
2. Inner `FluentWait` in `AppiumElementLocator.waitFor` (Appium's page factory internal wait)
3. `ByChained.findElement` → another `FluentWait`
4. Actual Appium server HTTP call → `NettyResponseFuture.get`

These **nested waits compound**: 10s outer × retry logic × inner Appium wait = far exceeds 420s when the Appium session is slow (keyboard active + form context switch from building 1 → building 2).

### Fix
**File**: `src/main/java/com/egalvanic/pages/SiteSelectionPage.java` (inside `createBuilding()`)

Added keyboard dismissal after `enterBuildingName()`:
```java
enterBuildingName(buildingName);
// Dismiss keyboard left open by sendKeys()
try {
    driver.executeScript("mobile: hideKeyboard");
    sleep(300);
} catch (Exception e) {
    System.out.println("Could not dismiss keyboard in createBuilding: " + e.getMessage());
}
waitForElementToBeClickable(saveButton, 2);
clickSave();
```

### Why This Works
Dismissing the keyboard before `clickSave()`:
1. Clears the keyboard from the accessibility tree → simpler element lookup
2. Allows `saveButton` proxy to resolve quickly through `ByChained`
3. Eliminates the compounding wait that led to the 420s timeout
4. Benefits ALL callers of `createBuilding()` — not just OfflineTest

---

## Files Changed

| File | Lines | Change |
|------|-------|--------|
| `src/test/java/com/egalvanic/tests/OfflineTest.java` | ~409 | `findElement` → `findElements` in `ensureOnlineState()` |
| `src/test/java/com/egalvanic/tests/OfflineTest.java` | ~1362 | Added `mobile: hideKeyboard` before `tapSelectAsset()` |
| `src/main/java/com/egalvanic/pages/SiteSelectionPage.java` | ~2454 | Added `mobile: hideKeyboard` in `createBuilding()` after name entry |

## Pattern: Keyboard Occlusion in iOS Automation

This is the **#1 cause of flaky iOS tests** in this codebase. The pattern:

1. `sendKeys()` or `element.click()` on a text field opens the iOS keyboard
2. The keyboard covers ~40% of the screen from the bottom
3. Elements below the keyboard become inaccessible (not in the accessibility tree or covered)
4. Subsequent `findElement`/`waitForClickable` calls either fail or hang
5. TestNG timeout is hit → `ThreadTimeoutException`

**Prevention rule**: Always call `mobile: hideKeyboard` or `driver.hideKeyboard()` after any `sendKeys()` call, BEFORE interacting with other elements. This is already done correctly in `selectPriority()` (IssuePage.java:1126-1128) and throughout ConnectionsPage — but was missing in OfflineTest and `SiteSelectionPage.createBuilding()`.

---

## Post-Fix: Compilation Error (Duplicate Variable)

**Date**: 2026-04-15 ~20:00 IST

### What Happened
The TC_OFF_014 keyboard dismissal block (Failure 2 fix above) introduced a **duplicate local variable** `d`:
```java
// Line 1306 (method scope):
io.appium.java_client.ios.IOSDriver d = DriverManager.getDriver();
// ...
// Line 1368 (inside try block — COMPILATION ERROR):
io.appium.java_client.ios.IOSDriver d = DriverManager.getDriver();
```

Java does not allow a local variable in an inner block to shadow a local variable from the enclosing block. This is different from C/C++ or JavaScript where shadowing is legal. The compiler rejects this with "Duplicate local variable d".

### Fix
Removed the redundant declaration — the `try` block now uses the existing `d` from line 1306:
```java
try {
    d.executeScript("mobile: hideKeyboard");  // uses method-scope d
    shortWait();
} catch (Exception e) {
    logWarning("Could not dismiss keyboard: " + e.getMessage());
}
```

### Lesson
When adding code to an existing method, always check whether the variable you're about to declare already exists in the enclosing scope. In this case, `d` was declared at the top of `TC_OFF_014` (line 1306) and used throughout the method (lines 1310, 1393, etc.).

---

## Status

- **Part 1 COMPLETE**: Offline module (3 fixes + 1 compilation fix)
- **Part 2 PENDING**: Waiting for Assets P1/P4/P5, Issues P1, Site Selection jobs to complete
