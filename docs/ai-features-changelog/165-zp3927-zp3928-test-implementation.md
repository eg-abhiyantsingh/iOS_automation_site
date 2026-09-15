# 165 — ZP-3927 / ZP-3928 catalog: first 32 tests implemented (2026-09-15)

Implementing the 104-case catalog (iOS 1.56 + 1.57). This changelog covers the first
tranche: **32 tests across 7 classes**, plus the page-object layer they all need.

## Implemented
| Class | Tests | Catalog section |
|---|---|---|
| `ZP3927_Locations_Test` | 10 | §2 New Location View + §3 independent Building/Floor/Room |
| `ZP3927_UnlinkIssue_Test` | 5 | §1 Unlink Issue on long-press |
| `ZP3928_IssuePhotoFilter_Test` | 6 | §1 With/Without Photos filters |
| `ZP3928_NumberPadDecimal_Test` | 3 | §6 decimal point |
| `ZP3928_IssueWorkOrderLink_Test` | 3 | §3 issue → work-order link card |
| `ZP3928_AssetIssuesDefault_Test` | 2 | §2 asset issues default to All |
| `ZP3928_Misc_Test` | 3 | §5 Copy-Data-To search · §8 modified_at · §9 volume (N/A) |

## Page-object layer added
- `BasePage`: `longPressElement()` (mobile:touchAndHold + W3C fallback), `isMenuItemPresent`,
  `tapMenuItem`, `isAnyTextPresent/Containing`, `tapText`.
- `BuildingPage`: the whole v1.63 create-location surface — `openNewBuildingSheetV163`,
  `typeLocationField`, `isCreateEnabled`, `tapCreate`, `isBuildingOnlyHintShown`,
  `isFloorRequiredForRoomShown`, `isNameRequiredErrorShown`, `visibleLocationRowCount`.
- `IssuePage`: photo filters (`tapPhotoFilter`, `isPhotoFilterDisplayed`, `currentIssueHasPhoto`),
  unlink (`longPressFirstIssueRow`, `isUnlinkIssueOffered`, `tapUnlinkIssue`), and the
  work-order link card (`isWorkOrderLinkCardPresent`, `tapWorkOrderLinkControl`, …).
- `AssetPage`: `isSearchFieldPresentOnScreen`, `isIssueFilterSelected`, `tapIssueFilter`.

## Two corrections the real app forced on the catalog
1. **A room is NOT independently creatable.** v1.63 ships the literal
   `A floor is required to create a room`, so catalog case TC_IND_03 ("room created on its
   own") was fiction. The test now asserts the REAL contract: with a room name but no floor,
   the app must either show that message or keep Create disabled — an orphan room must never
   be created. Independence is real for *buildings*: `Leave blank to create only the building.`
2. **Locations rows are Buttons, not Cells.** Live DOM probe: each building is a Button
   labelled with its name (`1_Del_36275`) beside a "Business" image and its own "Add" button.
   The first `visibleLocationRowCount()` counted Cells and returned 0, failing TC_LV_02. It now
   counts labelled Buttons minus chrome (Done/Add/grid-toggle/SF-Symbol ids).

All v1.63 strings used by these tests were taken verbatim from the bundle
(`New Building…` / `New Floor…` carry a real U+2026 ellipsis), never guessed.

## Validation so far
- Compile exit 0; verifier self-tests **39 run / 0 failures**.
- First live run on the sim: **TC_LV_01 PASSED**; TC_LV_02 failed and the rest skipped, which
  is what exposed the two corrections above. Both fixes are in; a re-run is queued.

## Remaining from the catalog (72 cases)
EG Forms ×13-type matrix (12), SD-card import (10), photo import (8), photo categories (5),
plus the deeper cases in the sections above. Blocked-by-fixture work is called out in the
catalog itself: the ×13 matrix needs QA-WT00..13, the import sections need seeded photo sets
with controlled timestamps, and volume-button capture stays N/A on the simulator.
