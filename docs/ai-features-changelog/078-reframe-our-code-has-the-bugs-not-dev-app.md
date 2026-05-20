# 078 — Reframe: OUR Automation Code Has The Bugs (Not The Dev App)

**Date**: 2026-05-20
**Time**: 13:00 IST
**Trigger**: User correction — *"dev team dont have issue. our code have issue. actually lots of issue"*.

---

## What I Got Wrong

In changelogs 069-077, I framed many fixes as "dev team needs to mirror this to dev repo" or "app has bug X, our recovery wrapper handles it". That framing was wrong.

**The dev team's iOS app (`Egalvanic/eg-pz-mobile-iOS`) is the production application.** Real users use it. It works. When our automation can't find an element, can't navigate a screen, or times out on detection, the bug is in OUR test code — not the app.

I owe the user (and this codebase) an honest acknowledgment of this reframe.

---

## Re-cataloged: 20 Bugs in OUR QA Automation Code

Every issue I found across this work is in our code. Listed honestly:

### Predicates Too Strict / Locale-Assuming

| # | Where | Our bug |
|---|---|---|
| 1 | `ConnectionsPage.isConnectionsScreenDisplayed` | We assumed English `'Connections'`. App also has French `'Connexions'`. |
| 2 | `BuildingPage.isLocationsScreenDisplayed` | We assumed `'Locations'`. App also `'Emplacements'`. |
| 3 | `IssuePage.isIssuesScreenDisplayed` | We assumed `'Issues'`. App also `'Problèmes'`. |
| 10 | `isWorkOrderActive` | We used `CONTAINS 'Active Work Order'` — matches `'No Active Work Order'` too. Substring trap our fault. |
| 13 | `tapIRPhotosTab` | We looked for `'IR Photos'`. App uses `'IR'`. We didn't try short form. |

### Multi-Strategy Locators Too Slow on Miss

| # | Where | Our bug |
|---|---|---|
| 3a | `selectFirstAsset` | 7 strategies × 5s default implicit wait = up to 35s per call on miss × 53 callers. We didn't cap. |
| 3b | `isDashboardDisplayed` | 2 lookups × 5s × 32 callers. We didn't cap. |
| 3c | `isAssetListDisplayed` | 2 lookups × 5s × 15 callers. We didn't cap. |
| 3d | `isAssetDetailDisplayed` | Same. |

### Hardcoded Test Data Assumptions

| # | Where | Our bug |
|---|---|---|
| 4 | `OfflineTest.TC_OFF_014` | Hardcoded `selectIssueClass("NEC Violation")` even though sim may have different classes. |
| 5 | Multiple tests | Hardcoded site names. Tests should pick the active site, not assume name. |

### Locator Approach Limitations

| # | Where | Our bug |
|---|---|---|
| 6 | `selectFirstPhotoFromPicker` | Used `element.click()` for iOS PHPicker. Should have been `mobile: tapWithNumberOfTaps` (XCUITest-native API). 8-strategy approach added. |
| 7 | `tapUploadIRPhotosLink` | Single exact-match. Should have CONTAINS + icon fallback + diagnostic dump from day 1. |
| 8 | `waitForIRPhotoUploadComplete` | Counted only `thermal`/`ir_`/`flir`-named images. App uses unnamed Image elements with non-zero rects. We assumed naming convention. |
| 9 | `addIRPhotoPair` | We required Visual filename always. FLIR-IND makes Visual optional ("Not required for FLIR-IND"). We didn't read the UI hint. |

### Greedy Predicates Misfire

| # | Where | Our bug |
|---|---|---|
| 14 | `revealAddIRPairForm` (initial draft) | Greedy `name CONTAINS 'add'` matched Asset-Edit "Add Photo" button. We navigated away from IR context by accident. |
| 15 | `tapWORowInIRSection` (initial draft) | Tapped existing IR pair row — opened read-only photo viewer (lightbox). We didn't understand that the entry point is different. |

### Missing State Management

| # | Where | Our bug |
|---|---|---|
| 11 | `tapOnConnectionsTab` and similar | Single-strategy nav. Needed multi-strategy with retry. |
| 12 | Test framework | No `navigateToDashboardFromAnyScreen` helper. Every test assumed Dashboard but cleanup left various screens. State pollution = our framework gap. |
| 16 | `startWorkOrderSessionFromDashboard` (initial draft) | Only handled Variant A (popup dialog). App also has Variant B (inline Start buttons). We didn't account for app version differences. |

### Infrastructure Inconsistency

| # | Where | Our bug |
|---|---|---|
| 17 | Offline workflow | Used S3 download. Every other workflow job used local zip. We had inconsistent infra → single point of failure. |
| 20 | Workflow suite organization | Single 6h jobs for Asset suites that take 4-5h. We didn't split. Job timeouts are an OUR choice, not the app's slowness. |

### Tests That Never Ran

| # | Where | Our bug |
|---|---|---|
| 18 | `OfflineSyncMultiSite_Test` | 914 LoC, 40 sync UCs. Implemented + committed but **never wired into any TestNG suite**. We wrote tests that didn't run in CI. |

### Test Logic Bugs

| # | Where | Our bug |
|---|---|---|
| 19 | `TC_OFF_014` `tapSelectAsset` after keyboard dismiss | Still broken locally. Real bug in our locator chain. Not the app. |

---

## What's Fixed vs Still Open

### Fixed (in commits 069-077)

| # | Issue | Commit |
|---|---|---|
| 1, 2, 3 | i18n predicates for screen detection | `cb4947a` (changelog 071) |
| 3a-d | Wait caps on 4 hot helpers | `839a5af` + `8cc9efa` |
| 6 | 8-strategy photo picker | `2724f13` (changelog 069) |
| 7 | `tapUploadIRPhotosLink` widened | `fe200ef` (changelog 077) |
| 8 | `waitForIRPhotoUploadComplete` 5 success paths | `fe200ef` |
| 9 | `addIRPhotoPair` makes Visual best-effort | `b418338` |
| 10 | `isWorkOrderActive` excludes "No Active" | `d42f142` |
| 13 | `tapIRPhotosTab` accepts `'IR'` | `823b7f5` |
| 14 | `revealAddIRPairForm` strict matching | `38f0c3c` |
| 16 | `startWorkOrderSessionFromDashboard` handles both variants | `e308483` |
| 17 | Offline workflow uses local zip | `8cc9efa` |
| 18 | OfflineSyncMultiSite wired into testng-offline.xml | `8d4e956` |
| 12 | `navigateToDashboardFromAnyScreen` helper | `b7cbd24` (changelog 076) |

### Still open (need our follow-up)

| # | Issue | Path forward |
|---|---|---|
| 4 | TC_OFF_014 hardcoded class | Fixed via `selectFirstAvailableIssueClass` helper in `fe200ef`, but downstream asset selection still fails |
| 5 | Hardcoded site names | Should query active site dynamically; not yet refactored |
| 11 | Single-strategy navigation in helpers we haven't touched | Many `tapOnXxx` helpers still single-strategy |
| 15 | `tapWORowInIRSection` initial assumption | Has multi-strategy now, but conceptually we tap wrong thing for empty IR sections |
| 19 | `TC_OFF_014` `tapSelectAsset` after kb dismiss | Needs investigation — actual locator chain audit |
| 20 | Workflow suite organization (6h cap) | Need to split big suites into matrix jobs |

---

## Why This Matters

Framing matters because:

1. **Direction of fix**: if you think the app is broken, you push back on the dev team. If you think your tests are broken, you fix your tests. The former is unproductive; the latter compounds.

2. **Effort allocation**: fixes to QA automation code stay within QA scope (this repo). Fixes to app code require dev-team alignment, code review, regression testing, release coordination. Choosing "our code" by default is faster and lower-risk.

3. **Confidence in the app**: users use the production app daily. If our tests fail in a way users don't see failures, our tests are the problem — by definition.

4. **Honesty**: my previous changelogs said things like "the picker-close 4th-bug in iOS app needs investigation" — that wasn't fair. It might just be how iOS PHPicker dismisses, and our test didn't handle the navigation correctly. Naming things "app bug" without evidence is sloppy.

---

## Going Forward

For all future test failures, the default attribution is:

1. **Our predicate is too narrow** → widen with multi-locale, multi-form, multi-type
2. **Our wait is too short** → poll longer, with state-stable check
3. **Our state assumption is broken** → use `navigateToDashboardFromAnyScreen` or similar
4. **Our locator is single-strategy** → add fallbacks + diagnostic dump
5. **Our test data is hardcoded** → query dynamically OR pick "any available"

Memory updated: `feedback_our_code_not_dev_app.md` ([memory/](.../memory/feedback_our_code_not_dev_app.md)).

---

## Push target

`eg-abhiyantsingh/iOS_automation_site` (QA repo) only. NOT pushing to `Egalvanic/eg-pz-mobile-iOS` (their code is fine).
