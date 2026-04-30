# 052 — ZP-323.7 T&C Checkbox Removed: 8 Tests Disabled

**Date**: 2026-04-30
**Trigger**: User said "t&c is removed".
**Outcome**: The 8 `TC_AUTH_TERMS_*` tests are disabled (`enabled = false`) — the T&C checkbox has been removed from the iOS app, so the feature no longer exists to test.

---

## What Changed

### [AuthenticationTest.java](../../src/test/java/com/egalvanic/tests/AuthenticationTest.java)

All 8 T&C tests now carry:

```java
@Test(priority = N, enabled = false,
      description = "DISABLED 2026-04-30: T&C checkbox removed from app per product decision")
```

| Test | Priority | Was Verifying |
|---|---|---|
| TC_AUTH_TERMS_01 | 39 | Checkbox unchecked by default |
| TC_AUTH_TERMS_02 | 40 | Sign In disabled when unchecked |
| TC_AUTH_TERMS_03 | 41 | Sign In enabled after checked + creds |
| TC_AUTH_TERMS_04 | 42 | T&C link opens document |
| TC_AUTH_TERMS_05 | 43 | Privacy Policy link opens document |
| TC_AUTH_TERMS_06 | 44 | Agreement label visible |
| TC_AUTH_TERMS_07 | 45 | Sign In disabled when checked but no creds |
| TC_AUTH_TERMS_08 | 46 | Checkbox can be unchecked |

The 7 supporting LoginPage methods (`getTermsCheckboxState`, `tapTermsAndConditionsLink`, `tapPrivacyPolicyLink`, `isLegalDocumentDisplayed`, `dismissLegalDocument`, `isTermsAgreementLabelVisible`, `toggleTermsCheckboxOnly`) are kept untouched — disabled tests can still reference them, and removing them creates compile churn for zero benefit. If T&C ever returns, flipping `enabled = false` → `true` is a one-line restore.

---

## Why `enabled = false` Instead Of Deletion

- **Reversibility**: If product brings T&C back, the tests are ready — just toggle the flag.
- **History**: Test code documents what the feature was supposed to do. Future devs reading "what consent flow did v1.31 use?" benefit.
- **TestNG semantics**: Disabled tests don't appear in pass/fail counts and don't affect assertion-coverage metrics (the gate scans for `@Test` methods that lack assertions; `enabled = false` is excluded from the count via TestNG's runtime).
- **Zero CI cost**: Disabled tests are skipped at collection time, not runtime — they add ~0ms.

A separate cleanup task in 6 months can remove them entirely if T&C remains gone.

---

## Final-Final ZP-323 Scorecard

| # | Feature | Status |
|---|---|---|
| 1 | AI Extraction | 🟢 sparkles button on Core Attributes |
| 2 | Edit Site - long press | 🟢 SwiftUI context menu |
| 3 | Create Asset - Detailed | 🟢 web-verified sections |
| 4 | Issue Safety/Notification | 🟢 |
| 5 | Copy to / Copy from | 🟢 ⋯ overflow menu |
| 6 | Connection Core Attributes | 🟢 |
| 7 | T&C checkbox | ⚫ **REMOVED** — 8 tests disabled |
| 8 | COM (Maintenance state) | 🟢 |
| 9 | Suggested Shortcuts | 🟢 |
| 10 | Issue IR Photos | 🟢 |
| 11 | IR Upload in Work Order | 🟢 |
| 12 | Schedule on Work Order | 🟢 |
| 13 | Edge properties in Connection | 🟢 |
| 14 | Long Press Building/Room | 🟢 SwiftUI context menu |
| 15 | Asset Listening | 🟢 Task Details pill toggle |

**Final tally**: 14 verified iOS-evidence-based, 1 removed-from-product. Zero pending unknowns.

The ZP-323 ticket is now coverage-complete from an automation-readiness perspective: every shipping feature has real-evidence locators, and the one removed feature has tests safely shelved rather than producing false positives.
