# 135 — All-module sweep: kill every class-from-name inference; share the class matcher

**Prompt (2026-07-20):** after the arc-flash Node Bus catch (changelog 134), "update
more test cases like this in case if you miss … for all module."

## Shared API (new home: `AssetPage`)
The class-suffix discovery moved from `ArcFlashAssetMatrix_Test` into **AssetPage**
so every module can use it:
- `findAssetOfClass(className)` — search → class-suffix scan → bounded scroll →
  VERIFIED-unfiltered rescan; returns the cell composite (feed to
  `selectAssetByName`) or null; leaves the asset on screen.
- `firstVisibleAssetOfClass(className)` — page-source scan for a visible cell whose
  composite `"<name>, <room>, <Class>"` ends with `", <Class>"`.
- `clearSearchFilterVerified()`, `scrollAssetListDown()`, `unescapePageSource()`.
ArcFlashAssetMatrix_Test now delegates; behavior unchanged (regression-run green).

## Live-caught geometry bug in the matcher itself
In **search-active mode** the collapsed search bar sits at y≈64 and the FIRST
result cell at **y≈108** (probed live with a disposable dump test) — the 150 floor
(and a 120 retry) both cut it: a single-result Fuse search read as "no match" and
only the slow unfiltered fallback saved it. Floor is now **100** (suffix rule
already excludes chrome; the floor is defense-in-depth). Arc-flash fuse test went
2m23s → 58s (direct search path, no fallback).

## Sweep results (all 50 test classes + 10 page objects)
The background sweep workflow died on usage credits (8/9 agents), so the sweep ran
inline: greps for `searchAsset("<class>")`, `selectAssetByName` literals, and every
class-word `CONTAINS` predicate in tests+pages, then context reads of each hit.

### Fixed (the real pattern)
- **`AssetPage.selectFirstSourceNode` / `selectFirstTargetNode`** — claimed "first
  available node" but silently required `name CONTAINS 'ATS'`: matched any asset
  merely NAMED "…ATS…", and false-missed when no ATS-named asset. Now:
  `ENDSWITH ', ATS'` (true ATS class) → any composite row → TestAsset fallback.
- **`AssetPage.longPressFirstOCPItem`** — identified "OCP items" by
  `CONTAINS 'Relay'/'Fuse'/…`: an asset NAMED "Main Fuse Feed" of any class
  matched. Now suffix-first (`ENDSWITH ', Relay'` …, `LinkTest` kept) with the old
  CONTAINS set as fallback (details rows may not carry the composite).
- **`WorkOrderPage.isOCPDChildDisplayedUnderParent`** — Review-Assets OCPD-child
  detection by `label CONTAINS 'Fuse'/…`: a parent named "…Fuse…" false-positived.
  Suffix-first, original CONTAINS + indentation heuristics kept as fallback.
- **`Asset_Phase6_Test.AS_03_verifySearchByAssetType`** — was a **pass-anyway test**
  (logged counts, zero assertions). Now a real oracle: searching a type must
  surface ≥1 cell whose trailing CLASS equals it (with condition-poll). Live run
  proved its worth immediately: ATS verified via `AFR_ATS_…, Room_…, ATS` — an
  ATS-class asset whose NAME contains no "ATS", findable only by type search.

### Inspected, NOT the pattern (left alone, reasons recorded)
- `WorkOrderPage` OCPD-Subtype option census (class words ARE the option
  vocabulary of a picker sheet).
- `ConnectionsPage:5728` Busway/Cable — those are CONNECTION (edge) types.
- `ConnectionsPage.isOnAssetsScreen` — screen-detection heuristic, no class claim.
- `IssuePage` picker Strategy 4 — "any asset" last-ditch name-shape heuristic,
  no class claim.
- `AssetPage.getSourceNodeValue/getTargetNodeValue` — pinned to the
  TestAsset+Generator fixture flow (fixture-pinned, low risk).
- `ConnectionsPage.getAssetClassFromEntry` — reads the entry's 2nd StaticText
  (app data, only logged); positional but not name-inference.
- `Asset_Phase1` searches (`@#$%`, spaces) — negative search-feature tests.
- AssetEngineer suite — uses self-created "Ns" assets + documented bound fixture
  "Test Busway" (class set in-test / fixture-pinned).

## Validation
- `mvn -o -DskipTests test-compile` PASS; verifier self-tests 34/34 PASS.
- Live (one at a time): AS_03 PASS (2/3 types class-verified; MCC honestly has no
  fixture — 0 rows), TC_AF_133 PASS (delegation regression), TC_AF_110 PASS
  (direct-search path). Source/target-node + OCPD flows are suffix-first with the
  original behavior as fallback — worst case identical to before; heat them in the
  next assets/workorder CI runs.
