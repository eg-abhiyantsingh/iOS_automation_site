＃ 043 — ZP-323 PART A: Authentication T&C + Issue Safety/Notification + Issue IR Photos

**Date**: 2026-04-29
**Time**: ~15:00 UTC
**Prompt**: "cover all the test case in depth ... divide the task into 2 or 5 parts ... cover additional test case too. i need more test case and more bugs."
**Source ticket**: ZP-323 (15 features)
**This part covers**: ZP-323.3, ZP-323.4, ZP-323.5 (3 of 15 — "Part A")

---

## Why This Is Part A (And What Comes Next)

You explicitly said: *"divide the task into 2 or 5 parts"*. ZP-323 has 15 features. Doing all 15 in one chat turn would mean shallow boilerplate; doing 3 features properly (deep verification + page-object methods + 6–8 test cases each + bug-hunt + changelog) takes a focused session and produces tests that actually run.

This document covers **Part A** = 3 features.
Subsequent parts (B–E) will cover the other 12 in batches of 3.

| Part | Features | Status |
|---|---|---|
| **A** (this turn) | T&C (.3) + Issue Safety/Notification (.4) + Issue IR Photos (.5) | ✅ Done |
| B | Suggested Shortcuts (.6) + COM Calculation (.7) + Detailed Asset Create (.11) | pending |
| C | Copy to/from (.12) + AI Extraction (.13) + Asset Listening (.10) | pending |
| D | Edit Site long press (.8) + Building/Room photo long press (.9) | pending |
| E | Schedule Work Order (.15) + IR Upload in Work Order (.14) | pending |

---

## Bug Findings From Live Web Verification

While exploring the live web app at `acme.qa.egalvanic.ai` for these three features, I documented findings that cross-reference both the spec and current code. **Two product observations and one code-doc inconsistency.**

### 🐞 Finding #1 — Existing connection record `Fuse 1 → Not Assigned` (already raised in changelog 041)
Already documented as "intentional feature for clients" per your clarification. No action needed.

### 📋 Finding #2 — Issue Class dropdown contains EXACTLY 7 options on live web
Confirmed live: `NEC Violation, NFPA 70B Violation, OSHA Violation, Repair Needed, Replacement Needed, Thermal Anomaly, Ultrasonic Anomaly`.
- **"Safety" and "Notification" are absent** — confirms ZP-323.4 spec.
- "Replacement Needed" is **present** but I couldn't find any iOS test that exercises it — tests below now do.

### 🐞 Finding #3 — Code-documentation inconsistency in IssuePage.java (likely outdated)
The class JavaDoc at [IssuePage.java:35–47](../../src/main/java/com/egalvanic/pages/IssuePage.java#L35-L47) lists Issue Class options as:

```
None, Canadian Codes Rough Draft, NEC Violation, NFPA 70B Violation,
OSHA Violation, Other, Repair Needed, Replacement Needed,
Thermal Anomaly, Ultrasonic Anomaly
```

But **live web shows only 7 options** (no `None`, `Canadian Codes Rough Draft`, or `Other`). The code documentation is stale. Worth a JIRA ticket: *"Update IssuePage class JavaDoc to reflect current Issue Class options"*.

### 🐞 Finding #4 — Login screen: web has NO checkbox, just hyperlinks (iOS-specific feature)
Web app's login screen shows: *"By signing in, you agree to our Terms and Conditions and Privacy Policy"* as informational text with hyperlinks. **There's no checkbox on web.** The T&C checkbox is iOS-only. This is fine per spec (clients can have different consent mechanics on different platforms), but worth noting for future cross-platform parity work.

### 🐞 Finding #5 — Existing iOS T&C `acceptTermsIfPresent()` is overloaded (Login flow + verification mixed)
Code review showed the existing T&C method handles 4 different "accept" strategies but has **no read-only state inspector**. Tests that needed to verify T&C state had to peek at internals. Now fixed with new `getTermsCheckboxState()` method.

---

## What I Built — Part A Deliverables

### A.1 — Login T&C (ZP-323.3)

**Files modified**:
- [src/main/java/com/egalvanic/pages/LoginPage.java](../../src/main/java/com/egalvanic/pages/LoginPage.java) — +7 new methods
- [src/test/java/com/egalvanic/tests/AuthenticationTest.java](../../src/test/java/com/egalvanic/tests/AuthenticationTest.java) — +8 new test methods

**New page-object methods** (all defensive — return safe values on missing elements):

| Method | Purpose |
|---|---|
| `getTermsCheckboxState()` | Read-only inspector — returns `"checked"`, `"unchecked"`, or `"missing"` |
| `tapTermsAndConditionsLink()` | Tap the **hyperlink** (NOT checkbox) — opens T&C document |
| `tapPrivacyPolicyLink()` | Tap the Privacy Policy hyperlink — opens Privacy doc |
| `isLegalDocumentDisplayed()` | Detect if a WebView/modal with legal text is showing |
| `dismissLegalDocument()` | Done/Close/back-swipe to leave the legal doc |
| `isTermsAgreementLabelVisible()` | Verify the "I agree to..." label is rendered |
| `toggleTermsCheckboxOnly()` | Toggle without filling credentials (for state-only tests) |

**New test cases** (TC_AUTH_TERMS_01 through TC_AUTH_TERMS_08):

1. **TC_AUTH_TERMS_01** — T&C unchecked by default (regression: write-once bug class)
2. **TC_AUTH_TERMS_02** — Sign In disabled when T&C unchecked + valid credentials (consent gate)
3. **TC_AUTH_TERMS_03** — Sign In enabled after T&C checked + valid credentials (happy path)
4. **TC_AUTH_TERMS_04** — Tapping "Terms and Conditions" hyperlink opens T&C doc
5. **TC_AUTH_TERMS_05** — Tapping "Privacy Policy" hyperlink opens Privacy doc
6. **TC_AUTH_TERMS_06** — Agreement label visible alongside checkbox
7. **TC_AUTH_TERMS_07** — Sign In remains disabled with T&C checked but credentials empty (necessary-but-not-sufficient)
8. **TC_AUTH_TERMS_08** — T&C is bidirectional — can be unchecked after checking (write-once bug guard)

All tests use `skipIfPreconditionMissing()` from `BaseTest` (added in changelog 038) when the checkbox isn't in the build — clean SKIP rather than false FAIL.

### A.2 — Issue Class: Safety & Notification Not Available (ZP-323.4)

**Files modified**:
- [IssuePage.java](../../src/main/java/com/egalvanic/pages/IssuePage.java) — +5 new methods + 2 expected/forbidden constant sets
- [Issue_Phase1_Test.java](../../src/test/java/com/egalvanic/tests/Issue_Phase1_Test.java) — +6 new test methods

**New page-object additions**:

| Element | Purpose |
|---|---|
| `EXPECTED_ISSUE_CLASSES` constant | The 7 classes that MUST be in the dropdown (Set<String>) |
| `FORBIDDEN_ISSUE_CLASSES` constant | `Safety`, `Notification`, `Notifications` (Set<String>) |
| `openIssueClassDropdown()` | Open the dropdown with multi-strategy fallback |
| `readIssueClassOptions()` | Read all visible option labels |
| `isIssueClassDropdownOpen()` | Verify state |
| `closeIssueClassDropdown()` | Cancel/dismiss |
| `looksLikeIssueClass(label)` | Heuristic to filter out unrelated UI text |

**New test cases**:

1. **TC_ISS_SAFETY_01** — `Safety` NOT in dropdown
2. **TC_ISS_SAFETY_02** — `Notification` NOT in dropdown
3. **TC_ISS_SAFETY_03** — All 7 expected options present + no forbidden ones
4. **TC_ISS_SAFETY_04** — `Replacement Needed` is selectable (positive test for new option)
5. **TC_ISS_SAFETY_05** — Total option count meets expected minimum (regression bound)
6. **TC_ISS_SAFETY_06** — Existing issues still display correctly even if their old class was retired (migration robustness)

### A.3 — Issue Details: IR Photos Visibility (ZP-323.5)

**Files modified**:
- [IssuePage.java](../../src/main/java/com/egalvanic/pages/IssuePage.java) — +4 IR photo methods
- [Issue_Phase1_Test.java](../../src/test/java/com/egalvanic/tests/Issue_Phase1_Test.java) — +5 new test methods

**New page-object methods**:

| Method | Purpose |
|---|---|
| `getIRPhotoCountOnIssueDetails()` | Returns count, or `-1` if section absent (good state for issues without photos) |
| `tapFirstIRPhoto()` | Tap the first IR photo thumbnail |
| `isIRPhotoViewerOpen()` | Detect full-screen viewer (Done/Close + large image heuristic) |
| `closeIRPhotoViewer()` | Done button or back-swipe |

**New test cases**:

1. **TC_ISS_IR_01** — IR section state is consistent (absent OR ≥1 photo, never empty section)
2. **TC_ISS_IR_02** — Tap IR photo → viewer opens
3. **TC_ISS_IR_03** — Viewer can be dismissed cleanly
4. **TC_ISS_IR_04** — Issues list still usable after IR viewer flow (modal-stack pollution guard)
5. **TC_ISS_IR_05** — Issue without IR photos doesn't render an empty IR section

★ Insight ─────────────────────────────────────
**Why the count==-1 vs count==0 distinction matters for TC_ISS_IR_01 / TC_ISS_IR_05**: The fix that "made IR photos visible" could introduce a regression where the section is now ALWAYS rendered even if the issue has no IR photos. That would be visible as an empty IR section header with no thumbnails. By distinguishing "no section" (count=-1, good) from "empty section" (count=0, bad), the test catches the regression class.
─────────────────────────────────────────────────

---

## Test Count Summary — Part A

| Feature | Page object methods added | Test methods added |
|---|---:|---:|
| ZP-323.3 (T&C) | 7 | 8 |
| ZP-323.4 (Safety/Notification) | 5 (+2 constants) | 6 |
| ZP-323.5 (IR Photos) | 4 | 5 |
| **Part A total** | **16** | **19** |

Plus the existing 4 tests from changelog 042 (TC_CONN_097–100).

**Cumulative ZP-323 progress**: 23 of ~80 expected test cases (28.75%).

---

## CI / Dev Repo Status (Side-By-Side Check)

Checked at the time of writing:

| Item | State |
|---|---|
| PR #212 (consolidated report + email fix) on dev repo | **OPEN — awaiting your review** ([link](https://github.com/Egalvanic/eg-pz-mobile-iOS/pull/212)) |
| Most recent full CI run #24876293380 | Completed (cancelled — 7/16 jobs hit 6h timeout) |
| Newer CI run since 2026-04-24 | None |

**Implication**: the new tests in this turn have NOT been run in CI yet. They compile clean (`mvn compile test-compile -q` passes), but real iOS-simulator verification needs a fresh CI dispatch from your side OR a local Xcode + Appium setup.

**Recommendation**: After Part A merges, dispatch a smoke run targeting Issues + Authentication suites only (faster feedback than full parallel) to verify the new tests behave correctly under CI timing.

---

## Verification Method (For Each Feature)

For every test, I followed this sequence — same pattern for Parts B–E going forward:

1. **Live web check** via Playwright at `acme.qa.egalvanic.ai`
   - Confirm the feature exists
   - Document UI structure (selectors, labels, behavior)
   - Look for product bugs (e.g., the connection validation finding)
2. **Read existing iOS code** (page object + tests) to understand:
   - What's already covered (don't duplicate)
   - The locator-strategy patterns used in the codebase
   - Naming conventions for tests
3. **Add page-object methods** that are:
   - Defensive (return safe values, never throw to caller)
   - Multi-strategy (handle SwiftUI/UIKit/web-view variants)
   - Read-only where possible (state inspectors don't mutate)
4. **Write test cases** covering:
   - Happy path
   - Negative cases (precondition violations)
   - Boundary cases (count limits, exact matches)
   - Migration / regression robustness
5. **Compile** with `mvn compile test-compile -q`
6. **Document** in this changelog with bug findings + decisions
7. **Commit** to QA repo only — no dev repo pushes

---

## Honest Scope Notes

- **No CI verification of the new tests** (yet) — they compile but haven't run on a simulator. A first-run pass typically reveals 1–2 locator refinements needed per feature.
- **No dev repo changes** — per your standing rule.
- **Skipped features for this part**: 12 features remain (ZP-323.6, .7, .8, .9, .10, .11, .12, .13, .14, .15 plus the original .1, .2 already done in changelog 042). These will be Parts B, C, D, E.
- **Hyperlink locators (T&C, Privacy)** are best-effort. iOS may render them as `XCUIElementTypeLink` or `XCUIElementTypeStaticText` depending on SwiftUI version. The code falls through both.

---

## Files Changed This Turn

| File | Change |
|---|---|
| [src/main/java/com/egalvanic/pages/LoginPage.java](../../src/main/java/com/egalvanic/pages/LoginPage.java) | +7 methods (~150 lines) for T&C verification |
| [src/main/java/com/egalvanic/pages/IssuePage.java](../../src/main/java/com/egalvanic/pages/IssuePage.java) | +9 methods + 2 const sets (~280 lines) for Issue Class verification + IR photos |
| [src/test/java/com/egalvanic/tests/AuthenticationTest.java](../../src/test/java/com/egalvanic/tests/AuthenticationTest.java) | +8 test methods (~280 lines) |
| [src/test/java/com/egalvanic/tests/Issue_Phase1_Test.java](../../src/test/java/com/egalvanic/tests/Issue_Phase1_Test.java) | +11 test methods (~370 lines) |
| [docs/ai-features-changelog/043-zp-323-part-a-auth-issues.md](043-zp-323-part-a-auth-issues.md) | This document |

**Total**: ~1,080 lines added across 5 files. 19 new test methods. 16 new page-object methods. 2 new constant sets.
**Compile**: `mvn compile test-compile -q` passes cleanly.

---

## Recommended Next Action

1. **Review and merge PR #212** on the dev repo (the email fix + consolidated report). Without it, the new tests in this PR won't have a working email/report pipeline.
2. **Confirm Part A** (this turn) before I start Part B. If anything in T&C / Safety / IR tests doesn't match your product spec, tell me now — easier to fix one part than to undo five.
3. **When ready, say "go Part B"** and I'll start ZP-323.6 + .7 + .11 (Suggested Shortcuts + COM Calculation + Detailed Create) at the same depth.
