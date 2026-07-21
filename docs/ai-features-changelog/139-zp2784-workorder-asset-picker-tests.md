# 139 — ZP-2784 Work Order asset-picker tests (6 new, all live-green) + wiring fix

**Prompt (2026-07-21):** "check the work order module its newly added, create test
case for that. make sure test case are helpful" — spec: `testcase_file/workorder.txt`
(ZP-2784 step-by-step plan).

## Live-probed v1.51 picker anatomy (in-session: session room FAB → Link Existing)
`NavigationBar 'Link Existing Assets'` + Cancel + search + QR button; section
Button **"OTHER LOCATIONS, <n>"** (chevron.right collapsed / chevron.down
expanded, trailing count badge); expanding reveals building disclosure Buttons
(fixture-named) each with a **scoped 'Select All'**; selecting shows a toolbar
**'Clear'**. Room / No-Location sections render only when they have candidates.
Also fixed en route: `WorkOrderPage.openFirstSessionRoom` never switched to the
session's **Assets tab** (v1.51 sessions land on Details) — rooms were
unreachable, which also silently gated TC_WO_LINK_01.

## New: `WorkOrderAssetPicker_Test` (TC_WO_PICK_01-06) — every oracle is a law, not a presence check
| TC | Law verified (spec ref) | Live result |
|---|---|---|
| 01 pickerAnatomyRenders | nav/Cancel/search + ≥1 location section + positive badge (TC-4.1) | PASS (badge 30) |
| 02 collapsedByDefaultAndExpands | collapsed start → expand reveals groups → collapse hides (TC-5.1 round-trip) | PASS |
| 03 scopedSelectAllFlipsAndClears | Select All ⇄ Deselect All label flip (TC-4.4) + toolbar Clear resets (TC-4.5) | PASS (Clear confirmed live) |
| 04 badgeDeterministicAcrossToggle | badge identical across 2 expand/collapse cycles | PASS (30==30) |
| 05 searchFiltersPickerCandidates | garbage search empties sections; clearing restores exact badge | PASS (30→0→30) |
| 06 cancelCommitsNothing | select-then-Cancel leaves room asset count unchanged (safety law) | PASS |

Helpful-by-design: honest SKIPs name the missing fixture (no WO / no room / no
Other-Locations candidates); every test cancels the picker (teardown guard too);
failure messages say what breaking means ("bucketing dropped candidates").

## Deliberately NOT automated (documented in the class javadoc)
- **Relocation commits** (TC-4.3/5.4/6.2): Done MOVES whole subtrees between
  rooms — destructive to the shared QA fixture site.
- **Bolt 3-state colors** (Suite 1): XCUITest exposes no tint on
  `bolt.circle.fill`; needs app-side accessibility value or pixel analysis.
- **French localization** (TC-5.5): app language is a custom plist key.
- Engineering gating Tests 1-5 of the txt are asset-engineer scope (eng-lib),
  not WO — candidates for AssetEngineer gap classes.

## Wiring fix
`testng-workorder-planning.xml`: **`WorkOrder_Features_Test` (8 tests,
ZP-3109/3054/3003) was wired in NO suite — it never ran in CI.** Now wired,
plus the new picker class. Suite 14 → 28 tests.

## Validation
Compile PASS; verifier self-tests 34/34; all 6 picker tests PASS live on v1.51
(one at a time, iPhone 17 Pro Max sim).
