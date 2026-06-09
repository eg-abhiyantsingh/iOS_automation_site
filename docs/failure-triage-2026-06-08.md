# Failure triage — all 91 failing tests (run 27122885420, 2026-06-08)

Every one of the 91 failing tests was triaged (9-agent parallel analysis + live
spot-checks). Categories below. Authoritative pass/fail comes from the fresh full
run **27212260983** (dispatched 2026-06-09, on all fixes). Source list:
`failed-suites/failed-2026-06-08.xml`.

## Category counts
| Category | Count | Meaning |
|---|---|---|
| FIXED_BY_RECENT_COMMIT | 43* | root cause addressed by 5301289/5517f30/d9ce693/dd90c78/0ae447f/bee7267 |
| SCRIPT_BUG | 32 | automation defect, fix known (below) |
| REAL_PRODUCT_BUG | 7 | app behavior / version drift |
| DATA_DEPENDENCY | 3 | needs seeded asset/issue |
| UNKNOWN | 3 | Site Selection — needs live check |
| QUARANTINED_KNOWN | 2 | B6 giant-tree / B11 Load Core-Attributes |
| FLAKY_ENV | 1 | LC_EAD_23 state-flaky |

\* **CORRECTION (live-verified):** the ~13 Thermal Phase2 tests (TC_ISS_159–171)
were *predicted* FIXED but **still fail live** — they use `changeIssueClassOnDetails("Thermal Anomaly")`
(a different path than the `selectIssueClass`/`openIssueClassDropdown` I fixed) and
fail at "Issue Class should be changed to Thermal Anomaly". So true FIXED ≈ 30, and
Thermal Phase2 moves to SCRIPT_BUG (fix `changeIssueClassOnDetails`).

## SCRIPT_BUG — actionable patterns (highest leverage first)
1. **Subtype handling (5 tests, ONE page-object fix):** `JB/MCCB/PDU/UTL/VFD_AST_01`
   verify "subtype shows None" for zero-subtype classes, but `clickSelectAssetSubtype()`
   THROWS when there's no clickable subtype. → make it return gracefully for
   zero-subtype classes; the test then asserts the "None"/non-clickable state.
2. **Subtype button not found (3 tests):** `REL_AST_03`, `SWB_AST_03`, `PB_AST_03`
   (classes that DO have subtypes) — `clickSelectAssetSubtype()`'s 5 strategies miss
   the button. → fix the button locator for the open subtype picker.
3. **SiteVisit missing nav-setup (≈14 tests):** `TC_JOB_023/024/027/028/031/032/040/051/052`
   need `ensureOnSessionDetailsScreen()` + `tapSessionTab("Issues")`; `TC_JOB_068/072/073/074/075`
   need `navigateToWorkOrdersScreen()` + `clickStartNewWorkOrder()`. Classic
   inter-test-dependency (each test must be self-contained).
4. **Thermal Phase2 (≈13):** `changeIssueClassOnDetails("Thermal Anomaly")` doesn't
   change the class. → apply the same picker-option-as-Button fix used for the
   new-issue dropdown.
5. **Asset login (2):** `BUG_DELETE_01`, `BUG_SPECIAL_01` — missing `loginAndSelectSite()`.
6. **Loadcenter→Load (3):** `LC_EAD_19/20/22` — Size/Voltage routed as dropdowns +
   LC_EAD_22 fills phantom fields; align with the Load-class real fields.
7. **WorkOrderPlanning (2):** `TC_WOP_009` deactivate-button label wrong;
   `TC_WOP_012` `isSessionStatsDisplayed` checks only Tasks/Issues, test wants Assets.

## REAL_PRODUCT_BUG (7) — app/version drift (flag, don't "fix" the test blindly)
- `MCC_EAD_02` — MCC edit form has no "Core Attributes" section (like Load/B11).
- ZP-323 features built for app **v1.31** but the QA build is now **v1.41**:
  `TC_ZP323_06_03` (Suggested Shortcuts), `07_02`/`07_03` (Condition-of-Maintenance value),
  `13_01`/`13_02` (AI Extract button/progress), `15_02` (Schedule date picker).
  → verify live whether the feature moved/renamed/removed; update or quarantine.

## DATA_DEPENDENCY (3): CB_EAD_01, DS_EAD_10 (need ≥1 seeded asset), LC_EAD_05 (Load counter).
## UNKNOWN (3): TC_SS_001, TC_SS_017, OfflineSync UC1 — need a live look.
## QUARANTINED (2): LC_EAD_02 (B11), TC_JOB_076 (B6 giant-tree).
## FLAKY (1): LC_EAD_23.

## Plan
Fix order by leverage: subtype graceful (5) → SiteVisit nav-setup (14) → Thermal
path (13) → asset login (2) → LC fields (3) → WOP (2). Verify each on the sim, then
let the full run confirm. ZP-323 reals: live-check vs v1.41 before changing.
