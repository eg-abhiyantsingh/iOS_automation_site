# TC_WT_* Work-Type Suite — Design (2026-07-21)

Companion to `docs/worktype-gold-spec-2026-07-21.md` (domain truth). This doc
fixes the class layout, TC id allocation, and house rules for the ≥400-case
work-type suite. Foundation code (already in tree):

- `com.egalvanic.constants.WorkTypeCatalog` — the 13 types + GENERAL enum
  (display names, keys, service UUIDs, Category, deEnergized, fixtureName()).
- `com.egalvanic.api.TestDataApi` — `workTypeServicesJson()`,
  `listWorkOrdersJson(search)`, `findWorkOrderIdByName`, `workOrderWorkTypeId`,
  `createWorkOrder`, `ensureWorkOrderFixture`, `resolveSldIdByName`, `companyId()`.
- `com.egalvanic.base.WorkTypeBaseTest` — `api()`, `requireApi(tc)`,
  `ensureFixturesOnLandedSite()`, `openWorkOrdersScreenWT()`,
  `openFixtureOrSkip(wt, tc)`, `landedSiteName()/landedSldId()`.
- `WorkOrderPage` — `isWorkOrderRowVisible`, `getWorkOrderRowComposite`,
  `scrollWorkOrderListTo`, `openWorkOrderByName` (verified-nav),
  `getWorkTypeLabelOnScreen`, `screenShowsWorkType` (+ the 580 pre-existing
  methods: session tabs, stats, rooms, create form, More Actions…).

## Fixtures

Durable `QA-WT00..13` WOs (one per type + General), self-provisioned on the
landed site by `ensureFixturesOnLandedSite()`. Already live on BOTH
"(s) Wild Goose Brewery" (current iOS first-site) and "Android Qa Site1".
First-site ordering DRIFTED once (2026-07: "(s) …" sorts before "A…") — never
assume a fixed site; always go through the base helpers.

## Classes & TC id allocation (5 classes, ≥400 total)

| Class | TC prefix | Min tests | Focus |
|---|---|---|---|
| `WorkTypeCatalog_Test` | `TC_WT_CAT_*`, `TC_WT_FIX_*` | 90 | Backend catalog parity + fixture-family integrity (API-heavy) |
| `WorkType_List_Test` | `TC_WT_LIST_*` | 95 | iOS WO list per fixture: reachability, composite anatomy, open/back |
| `WorkType_Details_Test` | `TC_WT_DET_*` | 105 | Opened WO screen per fixture: header, work-type label, tabs, info, stability |
| `WorkType_Behavior_Test` | `TC_WT_BEH_*` | 75 | Per-category iOS session behavior (AF/IR/COM/Checklist/Schedule/PM/General) |
| `WorkType_CrossCutting_Test` | `TC_WT_X_*` | 55 | API↔UI parity, create-form canary, offline smoke, negatives, idempotency |

All extend `WorkTypeBaseTest`. ExtentReport: `MODULE_JOBS` + feature string
"Work Types (13-option dropdown)".

### Class 1 — WorkTypeCatalog_Test (API + fixture contracts)
Per service-backed type (13 × 5): service present by key; display name exact;
category exact; de_energized exact; id is UUIDv5 (version nibble '5') AND
matches the pinned constant. Catalog-wide: exactly 13 services; ids unique;
keys unique + slug-shaped; no "General" service; PM Forms count == 7;
AF/Checklist/COM/IR/Schedule are singletons; de-energized member set exact;
procedure_count numeric ≥ 0 for all. Fixture family (via API, after UI login +
ensure): each of 14 fixtures exists on landed site; 13 carry their exact
work_type_id; WT00 has null work_type_id (extractSibling returns null for JSON
null — assert via raw JSON contains '"work_type_id": null' inside WT00's
object, or workOrderWorkTypeId()==null while findWorkOrderIdByName()!=null);
all active; names unique.

### Class 2 — WorkType_List_Test (iOS list anatomy)
Per fixture (14 × 6): row reachable by bounded scroll; composite BEGINSWITH
exact fixtureName (no truncation); composite ENDSWITH ', Medium' (priority
chip contract, `rowPriority`); open = verified nav; opened screen alive + not
blank (verifyAppAlive/verifyNotBlank); back restores the list with the row
still present. Cross: one full sweep counts all 14 distinct QA-WT rows; no
duplicate rows per fixture name; list survives dashboard round-trip; priority
chips parse for 3 samples; WO screen header still correct after sweep.

### Class 3 — WorkType_Details_Test (opened WO screen)
Per fixture (14 × 7): screen is session details (ensureSessionDetailsOpen /
isSessionDetailsScreenDisplayed); header/nav contains fixture name; work-type
label check (see PROBE CONTRACT below); tabs render; info section present;
photo-type value readable (FLUKE for WT01-13, FLIR-SEP for WT00 — tolerate
either but assert non-empty; hard-assert only if probe confirmed); no error
alert + app alive; clean back-nav. Cross: punctuated display names render
exactly (CTT, DGA, Shutdown — via getWorkTypeLabelOnScreen equality);
label-vs-catalog sweep across all 13; refresh/re-entry keeps header; 2×
backgrounding-on-details stability (guard()).

### Class 4 — WorkType_Behavior_Test (per-category session behavior)
Per service type (13 × 5): open fixture → Details tab default; category
surface reachable (single dispatcher `assertCategorySurface(wt)` with
multi-strategy detection per category — AF: arc-flash overlay/filter entry;
IR: IR-photo affordance; COM: condition-assessment surface; CHECKLIST: tasks
surface; SCHEDULE: schedule/panel surface; PM_FORMS: forms/tasks surface —
UNKNOWN surface ⇒ skipIfPreconditionMissing with precise reason, never
false-fail, never pass-anyway); locations/rooms tab shows content or a clean
empty state (never blank screen — verifyNotBlank); interaction round-trip
keeps app alive; exit restores WO list. Plus WT00/General block (≥4): opens,
no type-specific surface required, superset tabs tolerated, stable back-nav.
Negative cross-checks (≥6): sample non-AF type has no AF overlay entry etc. —
only where the probe confirmed the positive contract first; otherwise skip.

### Class 5 — WorkType_CrossCutting_Test
API↔UI parity: `workOrderWorkTypeId(fixtureName) == serviceId()` for all 13 +
WT00 null (14). Per-fixture API field integrity: priority Medium, est_hours 8,
photo_type FLUKE (WT00: FLIR-SEP), active true (14 — one test per fixture).
iOS create-form canary (2): Start New Work Order form has NO Work Type row on
v1.51 — assert absence so the test FLIPS when the feature lands on iOS
(update gold spec then). ensureWorkOrderFixture idempotency (2): second call
returns the same id; no duplicate rows after. Negative (2): createWorkOrder
with a bogus work_type_id must NOT return 2xx (assert exception/failure);
services catalog identical across two calls. Legacy WOs (2): a pre-work-type
'Work Order - *' row opens cleanly, no type label forced. Offline smoke (4):
airplane-mode on → fixture rows still listed (local store), one opens; off →
recovers. Deactivate/activate round-trip (4) on 2 samples. Cross-site parity
(2): family also present on 'Android Qa Site1' via API. No-forms types
(WT07/12/13) open cleanly despite 0 generated forms (3).

## PROBE CONTRACT (fill from WorkTypeProbe_Test output before hard-asserting)

`WorkTypeProbe_Test` (diagnostic, unwired) dumps the list rows and the opened
WO screen for WT08/WT01/WT05. Its output fixes:
1. whether the work-type label renders on the opened screen (and its exact
   element/type) → decides hard-assert vs skip in Class 3;
2. which per-category surfaces exist on iOS → decides Class 4 dispatcher
   strategies;
3. whether fixture rows appear at all without pull-to-refresh → decides
   whether openWorkOrdersScreenWT needs a refresh gesture.

## House rules (non-negotiable — hardening layer)

- Asserts via BaseTest wrappers (`assertTrue/assertEquals` overloads) — they
  throw; NEVER `if (…) return;` pass-anyway shapes. No `count >= 0`
  tautologies, no `assertTrue(a || b)` unless the OR is the actual contract.
- `skipIfPreconditionMissing(() -> cond, "TC_xx: reason")` ONLY for genuine
  environment preconditions (backend down, fixture missing) — never to dodge
  an assertion.
- Crash/blank guards on every UI test: `verifyAppAlive(step)` after nav,
  `verifyNotBlank(screen)` on opened screens, `verifyNoErrorAlert()` where an
  alert would invalidate the flow.
- No `Thread.sleep`; use `Waits.until(cond, timeoutMs)`, `existsNow`,
  `isElementGone`, `withImplicitWait(0, …)` for absence checks. Implicit-wait
  caps ≥ 3s where waits are configured.
- Multi-strategy locators (2+ fallbacks) for any NEW element query; prefer the
  page-object helpers that already encode them.
- Row predicates: WO rows are Buttons named '<name>, <Priority>' — always
  BEGINSWITH-match fixture names; never index-based selection.
- After sendKeys anywhere: dismissKeyboard() before tapping buttons.
- Never open the session Assets/Locations giant tree without the bounded
  helpers (WDA wedge risk) — Class 4 uses room helpers, not whole-tree queries.
- Each test independent: full `openWorkOrdersScreenWT()`/`openFixtureOrSkip`
  entry; no ordering dependencies beyond TestNG priority for report grouping.
- ExtentReportManager.createTest(...) first line of every test; TC id in the
  test description AND method name.

## CI plan (Part 7)

5 suite XMLs `parallel/testng-worktype-<slice>.xml` (one per class,
class-atomic for rerun sharding); new matrix job `worktype-tests`
(slice: catalog/list/details/behavior/cross) in ios-tests-parallel.yml, gated
on new input `run_worktype` (+ included in run_all), timeout-minutes 360,
same sim/appium bootstrap as workorder-planning job; result badge `WT`.
Root `testng.xml` gains the 5 classes in a "Work Type Tests" block.
