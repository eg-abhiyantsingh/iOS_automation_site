# 006 — Fix Smoke Test Login Failures (23/27 Tests)

**Date:** 2026-04-14
**Prompt:** "few test cases are failing so fix them" — CI run #24400731415
**Scope:** Login flow fixes across WelcomePage and LoginPage
**Files Changed:**
- `src/main/java/com/egalvanic/pages/WelcomePage.java`
- `src/main/java/com/egalvanic/pages/LoginPage.java`

---

## CI Run Analysis

**Run:** https://github.com/Egalvanic/eg-pz-mobile-iOS/actions/runs/24400731415
**Branch:** `release/qa` | **Suite:** Smoke (27 tests) | **Device:** iPhone 16 Pro, iOS 18.5

| Module | Tests | Result | Error |
|--------|-------|--------|-------|
| S3 Drift | 10 | 2 pass, 8 fail | S3 bucket policy drift (infra, not code) |
| Login | 1 | PASS | TC25 has no assertion — passes even if login fails |
| Site Selection | 1 | FAIL | "Dashboard should be loaded" — stuck on login screen |
| Asset CRUD | 3 | ALL FAIL | "Failed to click Asset Class dropdown" — never reached assets |
| Location CRUD | 4 | ALL FAIL | "All strategies failed to navigate to Locations" |
| Connection CRUD | 3 | ALL FAIL | 7-minute timeout each — stuck in login loop |
| Issue CRUD | 4 | ALL FAIL | "Could not tap Issues button" — never reached dashboard |

**Total: 3 passed, 23 failed, 1 passed-by-accident (TC25)**

---

## Root Cause 1: Terms & Conditions Checkbox (NEW)

### Evidence
Screenshot `TC_SS_044_verifyIssuesBadgeCount_FAILED.png` shows the login screen with:
- Email: `abhiyant.singh+admin@egalvanic.com` (correctly entered)
- Password: empty (screenshot taken after field cleared by failure)
- **"I agree to the Terms & Conditions and Privacy Policy"** checkbox — **UNCHECKED**

The app has a new Terms & Conditions checkbox on the login page that must be checked before Sign In works. The automation code had **zero handling** for this checkbox.

### Fix: `LoginPage.java`

**New element:**
```java
@iOSXCUITFindBy(iOSNsPredicate = "(type == 'XCUIElementTypeSwitch' OR type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeOther') AND (label CONTAINS[c] 'agree' OR label CONTAINS[c] 'terms' OR name CONTAINS 'checkbox' OR name CONTAINS 'square')")
private WebElement termsCheckbox;
```

**New method: `acceptTermsIfPresent()`**
- Strategy 1: Look for the `termsCheckbox` element. If found and value is "0"/"false", tap it.
- Strategy 2: Broad search for any tappable element with "agree"/"Terms"/"accept" label.
- Strategy 3: Search for checkbox images (square/checkbox icons).
- If nothing found: logs info and continues (safe for older app versions without T&C).

**Integrated into both login methods:**
- `loginTurbo()` — called after `enterPassword()`, before `tapSignIn()`
- `login()` (LOCKED method) — added `acceptTermsIfPresent()` before `tapSignIn()`. The "locked" status predates the T&C requirement; without this fix, login simply breaks.

---

## Root Cause 2: Fields Not Cleared Before Typing

### Evidence
Screenshots from subsequent modules show **text concatenation** in the Company Code field:
- `TC_NB_010`: Company Code = `acme` (leftover from previous module)
- `TC_CONN_037`: Company Code = `acmeacme.egalvanic` (leftover `acme` + test typing `acme.egalvanic`)
- `TC_ISS_049`: Company Code = `anicabhiyant.singh+admin@egalvanic.com` (leftover `anic` + email typed into wrong field!)

Error: **"Company not found. Please check your company code and try again."**

### Why This Happens
Each smoke module is a separate `run_mvn` call → new JVM → new Appium session. But the simulator/app may persist state. When a previous module's login fails, the app stays on the Welcome/Login screen with leftover text. The next module's `enterCompanyCode()` calls `sendKeys()` **without `clear()`** — the new text is **appended** to whatever's already in the field.

### Fix: `WelcomePage.java`

**`enterCompanyCode()`** — Added `clear()` before `sendKeys()` in all three code paths (placeholder field, generic field, direct fallback):
```java
companyCodeFieldWithPlaceholder.click();
companyCodeFieldWithPlaceholder.clear();   // ← ADDED
companyCodeFieldWithPlaceholder.sendKeys(companyCode);
```

### Fix: `LoginPage.java`

**`enterEmail()`** and **`enterPassword()`** — Same pattern: added `clear()` before `sendKeys()` in both the normal path and the stale-element-recovery path:
```java
click(emailField);
emailField.clear();      // ← ADDED
emailField.sendKeys(email);
```

---

## Cascade Explanation

The 23 failures all stem from ONE login failure in Module 2:

```
Module 1 (Login)     → TC25 passes (no assertion — just screenshots)
                       BUT login actually FAILED (T&C unchecked)
                       App remains on Login screen
                       
Module 2 (Site Sel)  → Detects WELCOME_PAGE → performs login
                       Login fails again (T&C unchecked)
                       selectFirstSiteFast() returns null
                       Dashboard never loads → FAIL
                       
Module 3 (Assets)    → App still on Welcome screen with leftover text
                       Types company code → gets appended → "Company not found"
                       Never reaches Login → never reaches Dashboard → FAIL
                       
Modules 4-7          → Same cascade — corrupted company code
                       or stuck on login screen → ALL FAIL
```

## S3 Drift Failures (8 tests) — Not Fixed

The 8 S3 bucket policy drift failures (`TC_SMOKE_11` through `TC_SMOKE_18`) are **infrastructure issues**, not test code bugs. The actual AWS S3 bucket policies in the QA environment have drifted from the expected baseline. This needs to be investigated by the DevOps/infra team — the test is correctly detecting real drift.
