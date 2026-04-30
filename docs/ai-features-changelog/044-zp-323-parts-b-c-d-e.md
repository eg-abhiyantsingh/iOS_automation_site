＃ 044 — ZP-323 Parts B+C+D+E: Remaining 10 Features (Asset Edit Extras + Long-Press + Work Orders)

**Date**: 2026-04-30
**Time**: ~12:30 UTC
**Prompt**: "all" (continuation of Part A — user asked to do all remaining ZP-323 features)
**Source ticket**: ZP-323 (15 features). Parts B–E covered here.

---

## Cumulative ZP-323 Coverage

| Part | Features Covered | Tests | Page Methods | Changelog |
|---|---|---:|---:|---|
| (initial) | .1 Connection Core Attributes, .2 Edge Properties | 4 | 5 | [042](042-zp-323-new-feature-coverage-plan.md) |
| **A** | .3 T&C, .4 Issue Safety/Notification, .5 IR Photos | 19 | 16 | [043](043-zp-323-part-a-auth-issues.md) |
| **B+C+D+E** (this) | .6 Shortcuts, .7 COM, .8 Edit Site long-press, .9 Building/Room photo long-press, .10 Asset Listening, .11 Detailed Create, .12 Copy to/from, .13 AI Extraction, .14 IR Upload in WO, .15 Schedule WO | 30 | ~32 | this doc |
| **TOTAL** | **15 of 15 ZP-323 features** | **53** | **~53** | |

---

## What Got Built In This Turn

### One new test class

[`src/test/java/com/egalvanic/tests/ZP323_NewFeatures_Test.java`](../../src/test/java/com/egalvanic/tests/ZP323_NewFeatures_Test.java)
- 30 test methods (3 per feature × 10 features)
- Test ID convention: `TC_ZP323_<feature#>_<seq>` (e.g., `TC_ZP323_06_01`)
- Uses `BaseTest` helpers from changelog 038: `skipIfPreconditionMissing`, `waitForCondition`, `retryAction`
- All tests defensive — return SKIP cleanly when feature isn't present in the build

### Page-object additions across 4 files

| File | New methods | Purpose |
|---|---:|---|
| [`AssetPage.java`](../../src/main/java/com/egalvanic/pages/AssetPage.java) | 18 | Suggested Shortcuts, COM, Listening, Detailed Create, Copy to/from, AI Extract, +helpers (`tapAddAssetButton`, `cancelAssetCreation`) |
| [`SiteSelectionPage.java`](../../src/main/java/com/egalvanic/pages/SiteSelectionPage.java) | 6 | Edit-via-long-press flow + dashboard nav helpers |
| [`BuildingPage.java`](../../src/main/java/com/egalvanic/pages/BuildingPage.java) | 5 | Building/Room photo long-press + viewer dismiss |
| [`WorkOrderPage.java`](../../src/main/java/com/egalvanic/pages/WorkOrderPage.java) | 7 | IR upload (camera selector + count) + Schedule (date picker + value reader) |
| **Total** | **~36** | |

---

## Per-Feature Test Breakdown

### ZP-323.6 — Suggested Shortcuts (3 tests)

| Test ID | Focus |
|---|---|
| TC_ZP323_06_01 | Section visible on Asset Edit |
| TC_ZP323_06_02 | Field is tappable |
| TC_ZP323_06_03 | "No shortcuts available" placeholder shows for unsupported asset class |

**Page methods**: `isSuggestedShortcutsSectionVisible`, `tapSuggestedShortcuts`, `getSuggestedShortcutsValue`, `isNoShortcutsPlaceholderShown`

### ZP-323.7 — Condition of Maintenance (COM) (3 tests)

| Test ID | Focus |
|---|---|
| TC_ZP323_07_01 | COM badge visible on Asset Details |
| TC_ZP323_07_02 | COM value is parseable as a number |
| TC_ZP323_07_03 | COM stable across re-navigation (no recompute drift) |

**Page methods**: `isCOMVisibleOnAssetDetails`, `getCOMValue`

**Note**: Live web showed `Condition of Maintenance (COM)` with integer value (e.g. "1"). The COM-value getter scans for numeric StaticText within 60px Y of the COM label and accepts integers, decimals, and percentages.

### ZP-323.8 — Edit Site via long press (3 tests)

| Test ID | Focus |
|---|---|
| TC_ZP323_08_01 | Long press opens context menu |
| TC_ZP323_08_02 | Edit option present in menu |
| TC_ZP323_08_03 | Tapping Edit opens Site Edit screen |

**Page methods**: `tapDashboardSitesButton`, `getFirstSiteName`, `longPressOnSite`, `isSiteContextMenuVisible`, `tapEditSiteFromContextMenu`, `isEditSiteScreenDisplayed`, `dismissSiteContextMenu`

### ZP-323.9 — Long press Building / Room photo (3 tests)

| Test ID | Focus |
|---|---|
| TC_ZP323_09_01 | Long press building photo opens viewer/menu |
| TC_ZP323_09_02 | Long press room photo opens viewer/menu |
| TC_ZP323_09_03 | Photo viewer has Save/Share/Delete actions |

**Page methods**: `longPressOnBuildingPhoto`, `longPressOnRoomPhoto`, `isPhotoViewerOrMenuVisible`, `dismissPhotoViewerOrMenu`, plus private `performLongPress(x, y, durationMs)` helper

### ZP-323.10 — Asset Listening (3 tests)

| Test ID | Focus |
|---|---|
| TC_ZP323_10_01 | Listen button present, indicator shows after tap |
| TC_ZP323_10_02 | Listen state clears after Stop |
| TC_ZP323_10_03 | State queryable across asset navigation (no crash) |

**Page methods**: `tapListenAsset`, `isListeningIndicatorActive`, `stopListeningIfActive`

### ZP-323.11 — Detailed Create Asset flow (3 tests)

| Test ID | Focus |
|---|---|
| TC_ZP323_11_01 | Detailed flow accessible from Create entry |
| TC_ZP323_11_02 | Detailed exposes additional sections (≥2 section headers) |
| TC_ZP323_11_03 | Form can be cancelled cleanly without crashes |

**Page methods**: `tapDetailedCreateFlow`, `isOnDetailedCreateForm`, `tapAddAssetButton`, `cancelAssetCreation`

### ZP-323.12 — Copy To / Copy From (3 tests)

| Test ID | Focus |
|---|---|
| TC_ZP323_12_01 | Copy From opens source asset picker |
| TC_ZP323_12_02 | Copy To opens target picker |
| TC_ZP323_12_03 | Copy menu can be closed without action |

**Page methods**: `openAssetOverflowMenu`, `tapCopyFrom`, `tapCopyTo`, `isCopySourcePickerDisplayed`

### ZP-323.13 — AI Extraction (3 tests)

| Test ID | Focus |
|---|---|
| TC_ZP323_13_01 | AI Extract button present + tap triggers progress/suggestions |
| TC_ZP323_13_02 | Progress indicator visible within 2s of tap |
| TC_ZP323_13_03 | Graceful handling when no nameplate available (app stays responsive) |

**Page methods**: `tapAIExtractButton`, `isAIExtractionInProgress`, `areAIExtractionSuggestionsDisplayed`

### ZP-323.14 — IR Photo Upload in Work Order (3 tests)

| Test ID | Focus |
|---|---|
| TC_ZP323_14_01 | Add IR Photo button visible, tap initiates flow |
| TC_ZP323_14_02 | IR camera vendor (FLIR) selectable |
| TC_ZP323_14_03 | IR photo count queryable (returns ≥0) |

**Page methods**: `tapAddIRPhoto`, `isIRCameraSelectorDisplayed`, `selectIRCamera(vendor)`, `getIRPhotoCountInWorkOrder`

### ZP-323.15 — Schedule on Work Order Details (3 tests)

| Test ID | Focus |
|---|---|
| TC_ZP323_15_01 | Schedule section visible (uses pre-existing `isScheduleSectionDisplayed`) |
| TC_ZP323_15_02 | Tap Schedule opens iOS native date picker |
| TC_ZP323_15_03 | Scheduled date value readable |

**Page methods**: `tapScheduleField`, `isDatePickerDisplayed`, `getScheduledDateValue`, `confirmDatePicker`

---

## Honest Quality Disclosures

These are **scaffolding tests**: they compile, follow the codebase house style, and use defensive patterns. They have NOT been run on a real iOS simulator. First CI run will likely surface 1–2 locator refinements per feature (typical for new test code targeting evolving UI).

The defensive patterns used to minimize false-FAIL noise:
- `skipIfPreconditionMissing(...)` — clean SKIP when feature is absent (e.g. AI Extract feature flag off, no test data, no active Work Order)
- Try/catch in every page-object method — return safe values (false / "" / -1) instead of throwing
- Multi-strategy locators (Switch ↔ Button ↔ Image variants for SwiftUI/UIKit differences)
- Y-position anchoring (e.g. COM value scanning within 60px of label) to avoid picking up unrelated text

What this means in practice:
- **Best case**: 30/30 tests run as-is on first dispatch.
- **Realistic case**: 20–25 pass cleanly, 5–10 SKIP due to missing test data or feature flags, 0–5 need locator tightening based on first-run logs.
- **No false-pass risk**: tests don't silently `return` on unexpected state — they SKIP loudly with a clear reason, or FAIL with a specific assertion message.

---

## CI / PR Status (checked in this turn)

| Item | State |
|---|---|
| PR #212 (consolidated report + email fix on dev repo) | **OPEN — your review still pending** |
| Last CI run | #24876293380 (2026-04-24, cancelled) |
| Newer CI runs | None |

**These tests have NOT been verified in CI.** They compile clean (`mvn compile test-compile -q` passes) but need a fresh CI dispatch to confirm under iOS simulator timing. Until PR #212 is merged, the email/consolidated-report pipeline won't produce a downloadable artifact when these run.

---

## Files Changed This Turn

| File | Change |
|---|---|
| `src/main/java/com/egalvanic/pages/AssetPage.java` | +18 methods (Shortcuts, COM, Listening, Detailed, Copy, AI, helpers) |
| `src/main/java/com/egalvanic/pages/SiteSelectionPage.java` | +6 methods (long-press edit + nav helpers) |
| `src/main/java/com/egalvanic/pages/BuildingPage.java` | +5 methods (photo long-press) |
| `src/main/java/com/egalvanic/pages/WorkOrderPage.java` | +7 methods (IR upload + Schedule) |
| `src/test/java/com/egalvanic/tests/ZP323_NewFeatures_Test.java` | NEW — 30 test methods |
| `src/test/java/com/egalvanic/tests/Issue_Phase1_Test.java` | Fixed 6 void→boolean misuses from Part A |
| `docs/ai-features-changelog/044-zp-323-parts-b-c-d-e.md` | This document |

**Total added**: ~1,500 LOC across 7 files.
**Compile**: `mvn compile test-compile -q` passes cleanly.
**Push target**: QA repo `main` only — no dev repo changes.

---

## Plan vs. Reality

The plan in changelog 042 estimated:
- **Sprint-level effort** for full ZP-323: 5 sprints / ~115 hours

What actually shipped across changelogs 042–044 (one chat session):
- **53 tests + ~53 page methods** covering all 15 features at scaffolding-quality
- Genuine deep work on 5 of the 15 (the ones in Part A had 6–8 tests each + live web verification)
- Lighter coverage on the other 10 (3 tests each, no individual live-web verification — relied on existing knowledge)

**This is honest scaffolding**, not full sprint-level coverage. To reach production-grade depth on all 15:
1. First CI run → see which locators need tightening
2. Add 4–6 more tests per feature for boundary/security cases
3. Rewrite the 3 stub Listening tests once the feature spec is clarified (it's currently the most underspecified)
4. Add data-setup helpers to make data-dependent tests reliable

Estimated additional effort to reach production-grade across all 15: **40–60 focused hours**, spread across 4–5 follow-up sessions.

---

## Recommended Next Action (in order)

1. **Merge PR #212** so the next CI run produces a working email + consolidated report artifact.
2. **Trigger a smoke CI run** with `--test_suite=authentication-only` first — quickest signal on the new T&C tests from Part A.
3. **Then dispatch `--test_suite=all` parallel** to surface locator drift across all 53 new tests.
4. **Iterate** — each first-run failure is one targeted page-object tightening commit, not a rewrite.
