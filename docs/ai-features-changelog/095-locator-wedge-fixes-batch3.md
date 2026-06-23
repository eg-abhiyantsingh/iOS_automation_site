# 095 — CI failure triage + locator-wedge fixes, batch 3 (2026-06-23)

Per request: check all CI failures and fix the ones related to locators. Harvested the actual
failures from the latest runs (27931604528 assets+issues = 184; 27936853644 connections) via
the `failed-suite` + JUnit `TEST-*.xml` artifacts.

## Triage result: the failures ARE locator/query-wedge — confirmed from stacks
The dominant failure message is **"Method … didn't finish within the time-out"** (the test HUNG),
and the JUnit stack frames point at unhardened query methods:

| Failing test(s) | Hung in |
|---|---|
| ATS_EAD_17, CB/DS/LC edit-dropdown | `AssetPage.selectDropdownOption` |
| BUG_CHAR_04 | `clickSelectAssetClass` — *"Failed to click Asset Class dropdown - no matching button found"* |
| TC_ISS_114/119 | `IssuePage.getVisibleSubcategoryOptions` |
| TC_ISS_064/118 | `IssuePage.getSubcategoryValue` |
| TC_ISS_109/111 | `scrollUp/DownOnDetailsScreen` |
| CAP_EAD_05-25 (18) | Capacitor text edits via `editTextField`/`fillTextField`/`fillFieldAuto` |

Same WDA-wedge mechanism as batches 1-2: unscoped whole-tree queries at the 5s implicit wait
with no budget → ~90s WDA snapshot on bleed-through DOMs → 6m per-test timeout. These are OUR
automation, not app bugs.

## Fixed this batch (all behavior-preserving: `withImplicitWait(0)` + budget; no locator/contract changes)
| Method | Change |
|---|---|
| `IssuePage.getVisibleSubcategoryOptions` | **(I wrongly skipped this in batch 2 — it's a confirmed wedge.)** Added `SUBCAT_BUDGET_MS` deadline, both finds → implicit-wait 0, and a deadline **break** in the per-element `getLocation()` loop (the actual cost on a giant tree). |
| `AssetPage.selectDropdownOption` | Wrapped the post-open option-selection whole-tree finds (contains-match + first-option) at implicit-wait 0 (completes the batch-2 locate-loop budget). |
| `AssetPage.fillTextField` | 3 whole-tree finds (TextField/TextView scan ×2, label scan) → implicit-wait 0. |
| `AssetPage.fillFieldAuto` | 3 finds (label, Select-trigger, legacy Button branch) → implicit-wait 0. **Legacy button branch kept** (regression blocker — scoped/wrapped, not deleted). |
| `AssetPage.editTextField` | 5 whole-tree finds (Strategy 3+4 label + TextField scans) → implicit-wait 0. **Primary locator + structural logic untouched** (regression blocker — wrap-only). |

This converts the CAP_EAD (18), CB/DS/LC edit, and Issue-subcategory (TC_ISS_063-119) hangs into
fast outcomes. ~13 whole-tree finds wrapped + 1 budget/loop-break.

## Validation
- `mvn -o -DskipTests test-compile` → exit 0.
- `testng-verify-selftest.xml` → **21 run / 0 failures**.
- Dispatching assets+issues CI (iOS 18.5) to confirm the recovery.

## Remaining (batch 4, CI-informed)
`getSubcategoryValue`, `scrollUp/DownOnDetailsScreen`, `selectClassViaSearch`,
`isLocationPickerOpen`, `clickSelectAssetClass` — same wrap pattern; lower-frequency frames.
