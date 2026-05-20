# 080 — BaseTest Auto-Advance from Welcome Page (Cross-Cutting Fix)

**Date**: 2026-05-20
**Time**: 16:30 IST
**Trigger**: User: *"check my code so many test case are failing first understand the code and check the issues"* → followed by *"check on iphone10 pro max real time by running"* → then guidance on breadth-first strategy across modules with two-failed-attempts cutoff.

---

## Problem

Running individual tests via `mvn -Dtest='X#Y'` failed at navigation, never reaching the actual test logic. Round 1 breadth-first sample across three modules:

| Test | Module | Original failure |
|---|---|---|
| TC_CONN_004 | Connections | "Should be on Connections screen" (12m) |
| TC_ISS_092 | Issues | "Should be on Issues screen" (6m, `Session does not exist`) |
| ATS_ECR_07 | Assets | "Failed to click Asset Class dropdown" (2m) |

All three failed BEFORE reaching their actual test logic. Root cause: each test depends on a prior test (`TC_CONN_001`, `TC_ISS_001`, `ATS_ECR_01`) having logged in. When run in isolation, the app cold-starts on the Welcome page and no shared helper auto-advances through onboarding.

---

## Fix

### 1. `BaseTest.java` — `autoAdvanceToDashboardIfNeeded()`

After `waitForAppReadyFast()` + `handleSessionExpiredIfNeeded()`, new helper runs the full onboarding chain in `@BeforeMethod`:

```java
private void autoAdvanceToDashboardIfNeeded() {
    // 1. Skip if already on Dashboard (common case — suite runs inherit logged-in state)
    if (isOnDashboardDirect(d)) return;

    // 2. Welcome page → type company code → tap Continue
    findElement(accessibilityId("Continue")) → type AppConstants.VALID_COMPANY_CODE → click

    // 3. Login page → loginPage.login(email, password)
    findElement(accessibilityId("Sign In")) → loginPage.login(VALID_EMAIL, VALID_PASSWORD)

    // 4. Site Selection → selectFirstSiteFast / selectFirstSite → waitForDashboardReady
    siteSelectionPage.isSiteListDisplayed() → selectFirstSiteFast → waitForDashboardReady
}
```

Locale-independent Dashboard check via SF Symbol icon names:
```java
findElement(accessibilityId("building.2"))  // Sites icon, language-agnostic
```

### Why direct `findElement` (not PageFactory)

First attempt used `welcomePage.isPageLoaded()` etc. These resolve PageFactory elements through `isElementDisplayed()`, which catches all exceptions and silently returns false — even on screens where the element is visible. Switched to direct `findElement(accessibilityId(…))` calls (same pattern that `waitForAppReadyFast()` already uses successfully).

### 2. `SiteSelectionPage.java` — Strategy 0 + French dashboard alts

`navigateToDashboardFromAnyScreen()` gets a Strategy 0 (Welcome page handler with disabled-Continue detection that types company code first) as a defensive fallback if a test path hits this helper before BaseTest auto-advance runs. Also extends `isOnDashboardQuick()` with French dashboard text alternates ("Bienvenue sur" / "Actions rapides").

### 3. `IssuePage.java` — Strategy 2c for SwiftUI Quick Action cards

When the app variant has no Issues bottom-tab (Site/Actifs/Connexions/SLD/Paramètres only), Issues exists only as a "Problèmes" Quick Action card. Strategy 2c finds `XCUIElementTypeStaticText` with the label and synthesizes a `mobile: tap` at the text's coordinates — SwiftUI hit-test routes the tap to the parent custom view.

### 4. `DriverManager.java` — Force English locale at app launch (user's complementary fix)

User added pre-launch hooks to write `appLanguage=en` to the app's custom plist via `xcrun simctl spawn defaults write` + `-AppleLanguages (en)` process arguments. This addresses the French-rendering UI ("Bonjour ! Bienvenue sur Wild Goose Brewery") that broke locale-sensitive test predicates. Belt-and-braces with my locale-tolerant alts in `isOnDashboardQuick`.

---

## Validation

### Local — TC_CONN_004 PASS

```
⚡ App ready - Welcome page detected
🧭 Auto-advance: on Welcome page, submitting company code
🧭 Auto-advance: on Login page, signing in
✅ Auto-advance: reached Dashboard
📝 ✅ Assertion passed: Should be on Connections screen
✅ Test PASSED: TC_CONN_004 (3m 26s)
```

**Before**: FAIL in 12m at line 413 "Should be on Connections screen"
**After**: PASS in 3m 26s

### Local — TC_ISS_092 (blocked by local session instability)

Auto-advance triggered correctly:
```
🧭 Auto-advance: already on Dashboard
```

But subsequent `tapOnIssuesButton` hit `"Session does not exist"` on every attempt. Local Appium/WDA state degraded after multiple back-to-back runs. CI will be the proving ground for Strategy 2c.

### Local — ATS_ECR_07 (auto-advance works, downstream bug)

Auto-advance worked. Test progressed to asset creation, then failed at "Failed to click Asset Class dropdown - no matching button found" — same SwiftUI-Other class of bug as Issues Quick Action. Not addressed in this changelog; needs its own Strategy 2c equivalent for `clickSelectAssetClass`.

### Dev-repo CI (run pre-fix)

User shared dev-repo CI log showing TC_ISS_001/TC_ISS_002 (iPhone 16 Pro / iOS 18.5) hitting the **same** Issues Quick Action bug:
- "Tab bar Issues button not found"
- "No Issues button found in tab bar area"
- "Coordinate tap did not reach Issues screen"

Confirms the bug is real on CI (not a local-only issue) and that Strategy 2c is the right approach. CI will validate after this push.

---

## Expected Impact

Single shared-layer fix in `BaseTest.@BeforeMethod` should unblock the FIRST failing test of every module that depends on prior-test login state. From the April CI 31-failure list:

- **Connections** (4 assertion failures): TC_CONN_004/005/036/041 — auto-advance unblocks navigation
- **Issues P2/P3** (15 failures): all the timeouts + assertion failures that couldn't even reach the test logic
- **Assets P1/P2** (10 failures): auto-advance + Strategy 2c equivalent for Asset Class dropdown needed

Realistic estimate: ~70% of the 31 April failures should now PASS, with the remaining ~30% needing specific Strategy 2c-equivalent fixes for SwiftUI custom views in their respective screens.

---

## Trade-off

Adds ~5s to every test's `@BeforeMethod` when the app is on cold-Welcome (one-time per test session in batch runs, since `noReset=true` preserves the logged-in state across tests). Zero overhead when already on Dashboard (fast-path returns immediately).

---

## Files Changed

| File | Lines | Change |
|---|---|---|
| `BaseTest.java` | +101 | autoAdvanceToDashboardIfNeeded + isOnDashboardDirect helpers |
| `IssuePage.java` | +85/-1 | Strategy 2c in tapOnIssuesButton (StaticText + coordinate tap) |
| `SiteSelectionPage.java` | +90/-1 | Strategy 0 in navigateToDashboardFromAnyScreen + French dashboard alts in isOnDashboardQuick |
| `DriverManager.java` | +33 | Force English locale via simctl + AppleLanguages (user's fix) |

---

## TL;DR

- **Found**: standalone test runs fail at navigation because no shared helper auto-advances from cold-Welcome
- **Fixed**: BaseTest.@BeforeMethod now auto-advances Welcome → Login → Site Selection → Dashboard using direct findElement calls
- **Validated**: TC_CONN_004 went from 12m FAIL to 3m 26s PASS
- **Pending**: Strategy 2c (Issues Quick Action) unvalidated locally due to Appium session instability — CI will validate
- **Push target**: QA repo `main` only — propagates to dev-repo CI automatically
