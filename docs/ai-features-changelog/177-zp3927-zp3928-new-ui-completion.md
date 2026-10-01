# 177 — ZP-3927 / ZP-3928: complete the two iOS automation tickets against the current UI (2026-09-21)

**Prompt:** *"update our test case according to new ui and 3928, 3927 covered in deepth completed
this ticket."*

ZP-3927 ("QA Automation - iOS 1.57") and ZP-3928 ("QA Automation - iOS 1.56") are **our own QA
tickets** (assignee: us), not dev features. "Complete" therefore means: every acceptance criterion
covered, the tests green on the current build, and the classes wired into CI. Changelog 165 had
implemented 32 of a 104-case catalog across 7 classes; the catalog itself was never saved, so the two
Jira AC lists are the contract.

## 1. The tree did not compile — and CI had been saying so for six days

`mvn -o -q test-compile` reported exit 0. A **clean** build did not:

| File | Error |
| --- | --- |
| `BuildingPage.java:7384` | `createButton()` already defined (v1.51 composite-form copy at `:177`) |
| `BuildingPage.java:7374, :7403` | `dismissKeyboard()` — not in BasePage; each page class defines its own |
| `IssuePage.java:12410` | `By` not imported (`filterChip(String)` returns it) |

The IDE (ECJ) had compiled newer `.class` files into `target/` with the errors baked in as runtime
`java.lang.Error("...refers to the missing type By")`, and maven's stale check skipped them. The first
live test (TC_IF_01) died on that Error. **CI `static-checks` has been red since commit 51de06a
(2026-09-15)** — both ZP-392x commits — and every full parallel-suite run since failed; nobody looked.

Fixes: v1.63 method renamed `v163CreateButton()`; `BuildingPage.dismissKeyboard()` added (mirrors
AssetPage's — BasePage cannot host it, ConnectionsPage returns `boolean`); `import org.openqa.selenium.By`.
Clean compile green; verifier self-tests green; TC_IF_01 **PASSED** (1m56s) on iPhone 17 Pro Max /
iOS 26.2 / QA 1.63. Memory note `feedback_clean_compile_before_trusting_green` records the trap.

## 2. Where the "new UI" is — and where it isn't

Newest iOS build anywhere on this machine is **1.63** (repo `apps/Z-Platform-QA.zip`, `~/Downloads/Z
Platform-QA.app`, and the sim install; the sim's `Z Platform-Dev` is 1.49). The user's screenshots are
TestFlight 1.66/1.67. So this pass verifies against 1.63 and pins every locator to copy extracted from
the build — the re-verification on 1.66+ is one `mvn test` once a build lands in `apps/`.

Real UI copy lives in **`Z Platform-QA.debug.dylib`**, not `Localizable.strings` (which only holds
localized keys; the 57 KB executable is a stub). Every string below came from there.

## 3. Probes before locators (three discovery runs, 1.63)

| Surface | Fact |
| --- | --- |
| Issues list | filter tabs are Buttons labelled `All, 3` · `Open, 3` · `In Progress` · `Pending` · `With Photos` · `Without Photos, 3` · `My Session, 1` — count only when >0; rows are unlabelled StaticText clusters (title `… on …`, priority, description, asset, status) — **no** labelled Cell/Button per row |
| Issue Details | `Work Order` card: linked ⇒ `<name>` + `Linked` + Buttons `Change work order` and `Unlink <name>`; unlinked ⇒ `Not linked to a work order` + `Select Work Order` (dylib). `IR Photos` section + `Link`. `Issue Photos` section = `Gallery` + `Camera`; `Delete Issue` |
| Gallery tap | presents the system PHPicker as a full-height sheet; its grid is NOT in the app's tree while loading (2 ActivityIndicators) — picker cells may only be addressable by coordinates |
| Long-press | on the Issues screen (All tab) raises **no** context menu, by element handle or by coordinates — "Unlink Issue" is not an Issues-screen action in 1.63 |
| Session (WO) | tabs `Details · Assets · Forms · Issues · More`; Issues tab empty state `No Issues` / `Get started by linking existing issues or creating new ones` / `Manage Issues` / `Add` |
| Copy Data To | nav `Copy Details To`; single-asset class ⇒ `No Same-Class Assets` / `There are no other Asset Class assets in this SLD to copy to.` — **no list, no search bar** |
| Asset details | `Issues` section = header `Issues` + `(N)` + `Filter` + `Add`, rows are Buttons `title, description, status`; no inline All/Open chips — the filter is behind `Filter`. Header sits ~3,300 pt down; the swipe loop demanded y∈[300,650] and scrolled past it at y≈150 |
| Session (WO) | tabs `Details · Assets · Forms · Issues · More`; `More` → `Files · IR · Tasks` |
| Session › Issues | empty: `No Issues` / `Get started by linking existing issues or creating new ones` / `Manage Issues` / `Add`. Linked: stats `Total / Open / Closed`, `Manage Issues`, `Issues` + `Sort`, rows = StaticText clusters, tab label `1, Issues` |
| Link Issues picker | nav `Link Issues` · `Cancel` · **`Update`** · `Search issues` · `Select issues to link to this session` · rows = Buttons `title, status, asset[, Linked to: <WO>], date` (value 1 when selected) · `⚠️ Issues can only be linked to one session at a time` |
| **Unlink Issue** | long-press a linked row **on the session Issues tab** → context menu `Unlink Issue` → Alert `Unlink Issue?` / `Remove “<title>” from this work order?` / `Cancel` · `Unlink Issue` → row gone (end-to-end reproduced in probe 4). On the Issues screen a long-press raises nothing |
| Session › More › IR | `0 IR Photos` · `Add IR Photos` · `File names are used as IR & visual photo keys.` · `No IR Photos` / `Tap the + button to add IR photo sets to assets`. `Add IR Photos` → popup: `From Photos` (Pick from your photo library.) · `Browse & Select` (Pick photos yourself from Files or a drive.) · `Auto-Match by Name` (Find photos on a drive whose names match your IR keys.) |
| Auto-Match, no drive | screen `Select a Drive to Match` · `Work order, <name>` · `Type, FLUKE` · `Files on the drive are matched to this work order by file name.` · `Select drive or folder` — explicit, not silent |
| Photos seeding | `xcrun simctl addmedia` takes the asset creation date from the file **mtime**, ignores EXIF → `touch -t` before adding (seeds at 5 m / 20 m / 3 h / 2 d ago now on the sim) |

## 4. What changed in the tests (32 existing)

- **Oracles that could not fail, rewritten**: `isWorkOrderLinkCardPresent` (was: any "Work Order" text)
  now needs the card's own copy; `isWorkOrderPickerOpen` needs `Select Work Order` / `No Work Orders Yet`;
  `linkedWorkOrderName` parses the `Unlink <name>` button; TC_IF_03 counts Issue-Photos thumbnails
  instead of "any image".
- **Partition law from tab counts** (TC_IF_05/06): visible-cell counting caps at one screenful and would
  false-fail past ~8 issues; the tab labels carry the counts.
- **Unlink — moved to where the feature is**: the class now rides the QA-WT04 session (WorkTypeBaseTest),
  self-provisions a linked issue through `Manage Issues` (only never-linked candidates are selected — the
  picker allows one session per issue), long-presses the row by coordinates, answers `Unlink Issue?`
  inside a paused-auto-accept window (`SessionIssuesPage.withAlertsManual`), and proves UNLINK ≠ DELETE
  two ways (picker row loses `Linked to:`; title still on the Issues screen). Every test unlinks on exit
  so the fixture is left as found. TC_UL_01-07 + TC_UL_10.
- **Asset issues default** — rewritten to the real anatomy: header `(N)` must equal the backend's
  all-status issue count for the asset (`TestDataApi.countIssuesForNode`, node matched on `label`),
  the `Filter` menu must offer All and mark it current, and Open must not survive a close/reopen.
- **Number pad**: precondition is now a *Thermal* issue (class switched on details when needed); TC_NPD_02
  is a real save → close → reopen round trip matched by title.
- **Copy Data To**: opens an asset whose class has peers (`Panelboard`/`Busway`/…) and recognises the
  `No Same-Class Assets` empty state instead of asserting a search bar on a screen that has no list.

## 5. New coverage (acceptance criteria that had none)

| AC | Class | Notes |
| --- | --- | --- |
| EG Forms addable + fillable on every WO type (3928) | `ZP3928_EgFormsMatrix_Test` | data-driven over the 13 service-backed `WorkTypeCatalog` types on the QA-WT fixtures; General as discovery |
| Photo import (3927) | `ZP3927_PhotoImport_Test` | Gallery → picker → thumbnail-count delta; cancel adds nothing; persists on reopen |
| SD-card / drive import · 30-min window · already-uploaded (3927) | `ZP3927_FlirImport_Test` | session › More › IR › `Add IR Photos`: the three routes and subtitles asserted verbatim; `From Photos` and `Browse & Select` must present real pickers; `Auto-Match by Name` must land on `Select a Drive to Match`. The drive candidate list (`Last 30 min` … `Today`, `already uploaded`, Select/Deselect All) is device-only — two explicit device-only verdicts, seeded photos ready for that run |
| Photo categories Other/Misc (3928) | `ZP3928_PhotoCategories_Test` | camera-free route (the walkthrough camera crashes on simulators, CAM-CRASH-01): Assets in Room → floating + → Quick Count → + Add Asset Type (MCC, no subtype) → Add Photoset → Add Photos → Gallery → staged photo → `PhotoCategoryChipsRow`. 1.63 ships `Other`, no `Misc` — asserted / recorded; Quick Count is cancelled on exit so no asset is created |

## 6. CI wiring

`src/test/resources/parallel/testng-zp3927-3928.xml` (listeners + 480 s time-out like the other suites),
workflow input `run_zp3927_3928`, job `zp3927-3928-tests` cloned from `issues-phase1` (report artifact
`zp3927-3928-report`, module email, result gate), added to both `needs:` lists. Before this the seven
classes lived only in `parallel-quick/` suites that no workflow referenced.

## 7. Live results on QA 1.63 (iPhone 17 Pro Max, iOS 26.2, one class at a time)

| Class | Tests | Result | Notes |
| --- | --- | --- | --- |
| `ZP3927_UnlinkIssue_Test` | 8 | _running_ | TC_UL_01–03 green so far: menu offered, alert copy verbatim (observed inside the paused-auto-accept window), row + tab badge drop by one |
| `ZP3928_IssuePhotoFilter_Test` | 6 | _queued_ | |
| `ZP3928_IssueWorkOrderLink_Test` | 3 | _queued_ | |
| `ZP3928_AssetIssuesDefault_Test` | 3 | _queued_ | |
| `ZP3928_NumberPadDecimal_Test` | 3 | _queued_ | |
| `ZP3928_Misc_Test` | 3 | _queued_ | |
| `ZP3927_PhotoImport_Test` | 4 | _queued_ | |
| `ZP3927_FlirImport_Test` | 7 | _queued_ | TC_FI_06/07 are explicit device-only verdicts |
| `ZP3927_Locations_Test` | 10 | _queued_ | |
| `ZP3928_PhotoCategories_Test` | 3 | _queued_ | |
| `ZP3928_EgFormsMatrix_Test` | 14 | _queued_ | 13 service-backed types + General discovery |

## 8. Open items

_(filled in at the end of the pass)_
