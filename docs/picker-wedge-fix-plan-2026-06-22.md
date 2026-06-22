# Picker / field-helper WDA-wedge fix plan (2026-06-22)

From the multi-agent investigation of run #27834873516's Issues (42) + Loadcenter/MCC (7)
failures. **Root cause (confirmed in code):** hot-path picker/field helpers run UNSCOPED
whole-tree `iOSNsPredicate`/`className` queries while the global implicit wait is 5s
(`AppConstants.IMPLICIT_WAIT`), with NO per-call wall-clock budget and NO verification that
the menu/sheet actually opened. On the documented SwiftUI previous-screen bleed-through DOMs
(Issue Details, Edit Asset), each miss burns the full 5s and each query snapshots a two-screen
tree → a single in-flight WDA snapshot approaches the 90s readTimeout → 6m per-test cap.

The repo already ships the fix toolkit: `BasePage.withImplicitWait(0, supplier)`,
`existsNow`, `isElementGone`, `Waits.until`, scoped class-chain / `name IN {…}` predicates,
`mobile: scroll`+predicateString.

## Safe pattern (behavior-preserving)
Wrap the unscoped whole-tree finds in `withImplicitWait(0, …)` + add a wall-clock budget. Same
elements matched, just no 5s burn per miss; a wedge becomes a fast honest outcome.

## Status

### Shipped (batch 1 — safe, compiled, self-tests green)
- **seq 3 `IssuePage.changeIssueClassOnDetails`** — the method EVERY one of the 10 subcategory
  tests + the class tests runs first; was at the full 5s default with no budget. Wrapped both
  whole-tree finds (exact + CONTAINS) in `withImplicitWait(0)` + `SUBCAT_BUDGET_MS` deadline on
  the retry loop. Bleed-through exclusion (`' on '`/`,`/len>35) kept.
- **seq 10 `AssetPage.isCoreAttributesSectionVisible`** — LC_EAD_02 6m0s hang; 3 scrolls × 5
  whole-tree CONTAINS at 5s each. All probes wrapped at implicit-wait 0.
- (Part A, separate commit) `AssetPage` subtype-open Strategy 3 → single `name IN {LIVE_SUBTYPES}`
  at implicit-wait 0.

### Remaining (safe — follow-up batches, do after CI confirms batch 1)
- seq 1 `selectDropdownOption` — add `DROPDOWN_BUDGET_MS` + `withImplicitWait(0)` (LC_EAD_16/20/22, MCC_EAD_20).
- seq 4 `readIssueClassOptions`; seq 7 `getVisibleSubcategoryOptions` (already partly hardened);
  seq 8 status helpers; seq 11a `editTextField`/`fillTextField` **wrap-only**; seq 13 header/probe
  speed-ups; seq 15 `isSaveChangesButtonVisible`.
- seq 6 NEW advisory open-probes `isIssueClassMenuOpen/isSubcategoryDropdownOpen/isStatusMenuOpen`.

### BLOCKED (regression pass — do NOT do as written; CI-validate first)
- seq 11b: do NOT replace the primary text-field locator with a structural read (~94 passing call-sites).
- seq 12: do NOT delete the legacy Button-enumeration dropdown-detection branch (~16 passing sites) — scope+wrap instead.
- seq 2 / seq 5: keep the open-menu probe **advisory** (retry-enabler), not a hard abort.
- Budget aggregation: LC_EAD_22 / MCC_EAD_20 call the dropdown helper 3× — thread a single
  shared per-test deadline so 3×45s can't approach the 360s/per-test cap.

Full agent output: `scratchpad/picker_plan.json`.
