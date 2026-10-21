# 193 — ZP-3914 (iOS 1.59) automation module + ZP tickets in both automation suites

**Date:** 2026-10-21 IST
**Prompt:** "ZP-3928, ZP-3914 — complete test for this. Create module and add in automation suite 2 to cover all
this changes, or in automation suite make sure to cover everything."
**App under test:** v1.69 simulator build (CI zip `084942b`). The v1.70 file in `~/Downloads` is still an
`iphoneos` device build, so it cannot run on simulators (changelog 192).

## 1. Coverage map

### ZP-3914 — QA Automation iOS 1.59 (new module, 47 tests)

| Acceptance item | Class · cases | What is asserted |
|---|---|---|
| Experience chooser | `ZP3914_ExperienceChooser_Test` TC_EXP_01-05 | Settings › Switch Experience, verbatim card copy, current card highlighted, highlight-only until Continue, AM tabs `Site Walks · Settings`, choice survives relaunch, switch back restores FT tabs |
| Site Walk: Account Manager Experience | TC_EXP_03, `ZP3914_SiteWalkWizard_Test` TC_SWW_01 | AM home = Site Walks list; Refresh / network control / status icons; backend list vs UI |
| Walk wizard | TC_SWW_02-09 | Step 1 title + customer rows + search + New Customer / Assign later + Cancel creates nothing; Step 2 (customer's sites, exact count, Assign a site later); Step 3 services gate Start Walk; mode tray Count / Journal; unassigned walk (backend walk_type count, no site); site walk (named after site, sld_id set, draft); delete asks then removes (UI + backend) |
| Count grid | `ZP3914_CountGrid_Test` TC_SWC_01-07 | Whole-site grid at zero; stepper drives subtype, class badge, unit rows, total ('1 asset' / 'N assets'); Save Assets unlocks completion + backend asset_count; search + 'No assets found'; 'Save changes?' guard (Cancel / Discard); room flow; Complete Site Walk → backend status complete |
| Offline sync · Offline support via SwiftData | `ZP3914_SiteWalkOffline_Test` TC_SWO_01-04 | Go Offline / Online menu + icon + Refresh; offline walk queued (not-synced icon, badge, absent from backend) then 'Sync N records' uploads it; queue survives a relaunch and still syncs; Settings Network Mode + Sync Queue Analyzer mirror the offline state |
| NETA-3 Forms Tab | `ZP3914_FormsTab_Test` TC_FT_01-05 | Level-1 tallies consistent; Level-2 one row per form, chips add up; search by asset/location + 'No matches'; status/room filters + 'Clear filters'; row opens single-form editor (no multi-edit chip) |
| Multi-edit working sets | `ZP3914_MultiEdit_Test` TC_ME_01-05 | Select mode circles + action bar; 'Edit' on 2 forms → 'Editing:', '2 assets · M submitted', section tools, footer; Mark As.. Draft / Submitted (dismissed, nothing changes); 'Delete 2 forms?' + warning, Cancel keeps; 'All' selects every row |
| EG Forms · Preload previous values as blue ghost values | `ZP3914_GhostValues_Test` TC_GV_01-02 | Submit form A with a unique value → new form B shows it as the empty field's placeholder; multi-edit ghost banner copy. Both forms deleted afterwards |
| Photo Viewer · Zoom tools · Markup tools | `ZP3914_PhotoMarkup_Test` TC_PM_01-07 | Review Photo (no Retake for library picks); 6 tools / 7 colours / defaults; arrow → Undo/Clear All; text label required + capped at 200; 'Discard markup?' sheet; Done stages photo, viewer pinch ≥2×, discard leaves asset unchanged; double-tap zoom (auto-skips before 1.70) |
| Terms of Use V2 · Updated T&C screen | `ZP3914_TermsOfUse_Test` TC_TOU_01-03 | /legal/acceptance/check contract; accepted user never gated (sign-in + relaunch); sign-in implicit consent with working Terms & Privacy links |
| Bulk export · Instance tags | not automatable on iOS | Only present in the embedded web bundle (`Flow/assets/*.js`), no native Swift code — web features |

### ZP-3928 — QA Automation iOS 1.56 (existing, unchanged this session)

Already automated in changelogs 177-191: `ZP3928_IssuePhotoFilter_Test`, `ZP3928_AssetIssuesDefault_Test`,
`ZP3928_IssueWorkOrderLink_Test`, `ZP3928_NumberPadDecimal_Test`, `ZP3928_PhotoCategories_Test`,
`ZP3928_CopyDataTo_Test`, `ZP3928_EgFormsMatrix_Test`, `ZP3928_Misc_Test` (IR cache busting; volume-button
capture N/A on simulators). Still blocked only by ISS-WOCARD-01.

## 2. Suites

| Suite | Change |
|---|---|
| Suite 1 (`ios-tests-parallel.yml`, this repo) | `zp3927-3928-tests` matrix gains shards `zp3914-am` and `zp3914-forms` (5 shards total); new input `zp_shards` = all / zp3914-only / zp3927-3928-only |
| Suite 2 (`ios-tests-repodeveloper-parallel.yml`, developer repo) | Ready-to-apply patch `docs/developer-repo-patches/2026-10-21-suite2-zp-tickets-job.patch`: new `zp-tickets-tests` job (same 5 shards), `job_selection` options `zp-tickets-only` / `zp3914-only`, summary counts it. `git apply --check` clean on `release/qa` 2396c69. Not pushed: QA never pushes to the developer repo |
| New XMLs | `testng-zp3914-am.xml`, `testng-zp3914-forms.xml`, `testng-zp3914.xml` (local one-shot) |

## 3. Framework changes

- `BasePage.SnapNode`: `value`, `placeholder`, `isOn()`, `text()` (label, else name — nav bars carry their title
  only in `name`).
- New snapshot page objects: `SnapPage` (base, with `rx()`/`by()` so thresholds measured on the 440×956 local
  simulator also hold on CI's 402×874 iPhone 16 Pro), `ExperiencePage`, `SiteWalkPage`, `SessionFormsTabPage`,
  `PhotoMarkupPage`, `SyncNetworkPage`, `LegalConsentPage`. `WorkOrderFormsPage.firstTextFieldState()`.
- `TestDataApi`: `listSiteWalks()` / `deleteSiteWalk(id)`. `AppConstants.appVersion()` / `appVersionAtLeast()`.
- `ZP3914BaseTest`: FT normalisation before each test, exact-id walk cleanup, and a failure screenshot taken
  BEFORE any cleanup (`<method>_FAILED_atFailure_*.png`, picked up by the defect-report PDF).

## 4. Product findings (both verified live with screenshots, both Low)

**SW-WIZ-01 — Wizard back-navigation drops the customer (TC_SWW_08, verified live + source).** Step 2's 'Assign a site
later' calls `deferAssignment()` (the step-1 "defer both" action), which clears the customer and its site list.
Going back from Services then shows the title 'Site' and "This customer has no sites yet — start the walk without
one and attach it later." for a customer that has sites. Low severity (no data loss: walks store only the site).

**MKP-LABEL-01 — Markup text label shows text past the 200 limit (TC_PM_04).** Typing 200 characters and then
10 more one key at a time leaves all 210 visible in the label field while the counter reads 200/200. The 1.70
source caps only the editor state (`MarkupEditorState.updateLabelDraft` keeps the first 200), so the last 10
characters the user sees are dropped silently when the mark is saved. Reproduced with bulk and per-key typing.

Neither is filed in Jira (no external changes without approval). Both have `bug-report-overrides.json` entries
(severity Low) so the customer defect PDF labels them correctly.

## 5. Validation (local, iPhone 17 Pro Max simulator, iOS 26.2, app v1.69)

| Class | Result | Notes |
|---|---|---|
| ExperienceChooser | 5/5 | TC_EXP_03 green after the tab-bar wait fix |
| SiteWalkWizard | 8/9 | TC_SWW_08 fails on SW-WIZ-01 (product) |
| CountGrid | 7/7 | TC_SWC_04 / 06 green after locator fixes |
| SiteWalkOffline | 4/4 | TC_SWO_02 green after menu-verified online/offline |
| FormsTab | 5/5 | first run |
| MultiEdit | 5/5 | TC_ME_02 green after footer sort fix |
| GhostValues | 2/2 | ghost placeholder proven; forms deleted (TC_GV_02 via Forms tab › Select › Delete) |
| PhotoMarkup | 5/7 | TC_PM_04 fails on MKP-LABEL-01 (product); TC_PM_07 skips on 1.69 (double-tap zoom is 1.70) |
| TermsOfUse | 3/3 | TC_TOU_03 green after exact-link helper (`LegalConsentPage`) |
| **Total** | **44 pass · 2 product fails · 1 version skip** | |

Fixed cases were confirmed by targeted re-runs; the module has not yet run end to end in one pass after the
last fixes — the CI dispatch below is that run.

Other gates: `mvn -o -DskipTests test-compile` clean; verifier self-tests 39/39; `generate_bug_report.py
--selftest` PASS; no ZP3914 test flagged by `check_assertion_coverage.py`.

Automation bugs found and fixed while validating (no product impact): nav-bar title only in `name`; wizard
Cancel only on step 1; search tapped mid-slide; Settings tab left on a pushed sub-screen; context-menu item
pressed before it rendered; 'Add asset class' counted as a class; room prompts are field placeholders; queued
walk hides the offline icon; keyboard lifts the session bar; keyboard 'done' key read as the label bar's Done;
legacy Privacy-link predicate matches the whole consent sentence; ghost cleanup could not reopen the room
(two leaked forms were deleted by a one-off purge, not committed).

Test data: every site walk created was deleted by id; Switch-1 in 'Optional Notes Room_21' is back to its one
pre-existing form.
