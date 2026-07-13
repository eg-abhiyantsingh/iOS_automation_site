# Arc Flash Readiness — ZP-2373 web → iOS/iPad parity map

Scope: NFPA 70E per-asset-class progress dashboard on iPhone + iPad
(ticket: extend ZP-2373 web automation to mobile form factors).

iOS coverage = the arc-flash module (`testng-arc-flash.xml`): `ArcFlash_Test`
(TC_AF_001-019), `ArcFlashPunchlist_Test` (020-025), `ArcFlashBreakdown_Test`
(030-035), `ArcFlashResponsive_Test` (040-044, runs on BOTH form factors), plus
the eng-lib canary. The same suite runs unchanged on the iPad CI job
(`run_arc_flash_ipad` → iPad Pro 13-inch simulator) — every locator is
predicate-based and every layout assertion is expressed against the live
window size, so iPhone and iPad execute identical contracts.

| Web case (ZP-2373) | iOS/iPad coverage | Notes |
|---|---|---|
| AF_01_DashboardAndTabs | TC_AF_001/002 | quick-action card + dashboard open/Done |
| AF_02/03/11/12 EngineeringMode enable/persist/recalc/revert | asset-engineer module (TC_ENG_*) | on iOS, engineering data lives in the eng-lib Engineering section, not a dashboard toggle |
| AF_04_AssetClassOptions | TC_AF_016 (closed bucket-label set) | class list = breakdown buckets on mobile |
| AF_05_AssetDetailsColumns | TC_AF_012 buckets+expansion | mobile renders rows, not grid columns |
| AF_06_FilterByAssetClassWorkflow | TC_AF_044 + TC_AF_030 (card→breakdown switch) | mobile filters by selecting metric/bucket, no dropdown filter |
| AF_07_DefaultTabIsOverview | TC_AF_009 (default breakdown = Asset Details) | |
| AF_08_ThreeReadinessGauges | TC_AF_003/005/006/007/008 + TC_AF_040 | ring + 3 metric cards + weighted average |
| AF_09_PerClassBreakdown | TC_AF_011/012/031/032/033 + TC_AF_042 | per-asset-class progress core |
| AF_10_InfoTooltips | N/A on mobile | no tooltip affordance in the iOS dashboard |
| AF_13_PerAssetCompletion | TC_AF_014 (drill to editor) + TC_AF_043 | per-asset rows inside a class bucket |
| AF_14_Pagination | N/A on mobile | breakdown is a scrolling list; scroll covered by TC_AF_041 |
| AF_15_RefreshControl | TC_AF_013 (re-entry deterministic) | mobile refresh = reopen |
| AF_16_TabStatePreserved | TC_AF_013/034 | |
| AssetClassMatrix testAssetClassFilterApplies | TC_AF_044 | |
| AssetClassMatrix testClassBreakdownConsistent | TC_AF_031 (header count == revealed rows) + TC_AF_015 | |
| AFC_01..09 Connections readiness | TC_AF_011/017/033/035 + TC_AF_023 (edge badges) | connection-details breakdown + punchlist edges; edit-modal flows live in Connections_Test |
| AFE_01..08 Engineering E2E (grid/edit/recalc/report/site-switch) | asset-engineer module + TC_AF_014 drill | report generation N/A on mobile |
| FiltersPagination (rows-per-page, displayed-rows) | N/A on mobile | no paginated grid |
| GridMatrix (column present/sortable) | N/A on mobile | no sortable grid columns |
| Platform AFP_01_RoleSelector / AFP_05_SwitchRoleView | N/A on mobile | single-role field app |
| Platform AFP_02_SldViewerPresence | Connections_Test TC_CONN_054/055 (SLD tab) | |
| Platform AFP_03_ResponsiveLayout | TC_AF_040/041 (viewport bounds + scroll round-trip, both form factors) | |
| Platform AFP_04_GenerateReportPresent | N/A on mobile | |
| RoleMatrix testRoleSelectable/testRoleViewLoads | N/A on mobile | |

**Mobile-specific interactions (ticket acceptance):** touch presses (all taps
are W3C touch), touch scroll round-trip (TC_AF_041), touch drill-in/out
(TC_AF_043/014), responsive layout bounds (TC_AF_040), form-factor detection
logged in every responsive case title (`[iPhone]` / `[iPad]`).

**CI integration:** `run_arc_flash` (iPhone 16 Pro sim) and `run_arc_flash_ipad`
(iPad Pro 13-inch sim) checkboxes in `ios-tests-parallel.yml`; both upload
form-factor-named artifacts and send module emails.
