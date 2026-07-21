# 141 — Work Type dropdown (13 options): gold spec + fixtures + ≥400-case TC_WT_* suite

**Prompt (2026-07-21):** "work order has work type drop down with 13 options; we
need to cover all of that. it will be complex so cover everything in depth.
test cases will be more than 400." Follow-up: iOS coverage, quality over
quantity, divide into parts, use Playwright for E2E understanding, heat-and-trial.

## Parts & what was done

**Part 1 — web ground truth.** Drove the live web Create-WO dialog with
Playwright: the dropdown has **13 service-backed options + UI-only "General"**
(→ `work_type_id = null`). Source: `GET /api/procedures-v2/services` (13
services with deterministic UUIDv5 ids, key, category `type`, de_energized,
procedure_count). Full table + traps (NETA key = `de-energized-testing`;
create form is type-agnostic) in **docs/worktype-gold-spec-2026-07-21.md**.

**Part 2 — per-type detail contracts.** Opened all 14 fixture WOs on web and
diffed: category drives tabs/columns (AF → SLD + Equipment Designations + Arc
Flash column; IR → IR Photos; COM → Condition Assessment + C.O.M.; Checklist →
Tasks; Schedule → Panel Schedules; PM Forms → Forms + % once generated;
General → superset). Form/task generation is **async AND class-conditional** —
counts must never be hard-asserted (gold spec §5).

**Part 3 — iOS ground truth (probe, 4 heat-and-trial runs).** New diagnostic
`WorkTypeProbe_Test` (unwired; `mvn test -Dtest=WorkTypeProbe_Test`). Findings
(gold spec §3b):
- iOS gets sessions via the whole-SLD sync (`/sld/v3/{id}` → `ir_sessions[]`
  incl. `work_type_id` + `mappings.user_session`).
- **A WO shows in the iOS list only when the user has a user_session mapping**;
- `POST /mapping/user-session/create` silently no-ops without a client `id`;
- cold relaunch / pull-to-refresh do NOT re-sync; **site re-selection does**;
- **first-site drift**: suite now lands on "(s) Wild Goose Brewery" (sorts
  before "Android Qa Site1") — old Android-Qa-Site1-only fixtures (incl.
  changelog-140's AF WO) are invisible to first-site runs.

**Fixtures.** Durable family **QA-WT00..QA-WT13** (one WO per type + General;
Medium/8h/FLUKE; field_technician+certifier mappings for the QA admin) created
on BOTH "(s) Wild Goose Brewery" and "Android Qa Site1"; suite self-provisions
via `WorkTypeBaseTest.ensureFixturesOnLandedSite()` wherever the app lands.

**Part 4 — foundation code.**
- `constants/WorkTypeCatalog.java` — the 13+1 enum with pinned UUIDv5 ids,
  categories, fixture naming.
- `api/TestDataApi.java` — `workTypeServicesJson`, `listWorkOrdersJson` (POST
  list), `findWorkOrderIdByName`, `workOrderWorkTypeId`, `createWorkOrder`
  (byte-faithful payload; ms-precision timestamps + ASCII description are
  load-bearing — 500s otherwise), `ensureWorkOrderFixture`, `resolveSldIdByName`
  (WO-list fallback because `/users/{id}/slds` is [] for admin), `companyId()`.
- `pages/WorkOrderPage.java` — TC_WT_ helpers: `scrollWorkOrderListTo`,
  `openWorkOrderByName` (verified-nav), `getWorkOrderRowComposite`,
  `getWorkTypeLabelOnScreen` (exact-name + stacked-form strategies).
- `base/WorkTypeBaseTest.java` — shared plumbing: lazy API, self-provisioning,
  site-reselect resync, `openFixtureOrSkip`.

**Part 5 — the suite (≥400 cases, 5 class-atomic slices).**
| Class | Prefix | Focus |
|---|---|---|
| WorkTypeCatalog_Test | TC_WT_CAT/FIX | backend catalog parity + fixture integrity |
| WorkType_List_Test | TC_WT_LIST | list anatomy per fixture |
| WorkType_Details_Test | TC_WT_DET | opened-screen contract incl. work-type label |
| WorkType_Behavior_Test | TC_WT_BEH | per-category session behavior |
| WorkType_CrossCutting_Test | TC_WT_X | API↔UI parity, canaries, negatives |
Design + house rules: **docs/worktype-test-design-2026-07-21.md**. The iOS
create-form canary asserts v1.51 has NO Work Type row — flips when iOS gains
the dropdown.

**Part 7 — wiring.** 5 suites `parallel/testng-worktype-*.xml`; new CI matrix
job `worktype-tests` (5 slices, gated `run_worktype` + `run_all`); root
testng.xml "Work Type Tests" block; memory `worktype-catalog-gold`.

## Probe journey (11 runs, heat-and-trial) + fixes it forced
1. Runs 1-6: fixtures invisible on iOS despite being in `/sld/v3` — found the
   TWO blockers: (a) user_session mapping required for list visibility AND the
   mapping POST silently no-ops without a client `id`; (b) the app re-pulls
   sessions ONLY on login site-selection (cold relaunch, pull-to-refresh, and
   the dashboard Sites quick-action hop all do NOT re-sync).
2. Run 7 (fresh install = the CI condition): fixtures visible; **row anatomy
   discovered**: `<name>, <work-type label>, <priority>` — label on the row.
3. Runs 8-10: plain `element.click()` on rows is a NO-OP; generic `visible==1`
   whole-screen dumps WEDGE WDA on 100+ row lists.
4. Run 11: house activation pattern (pause alerts → coordinate-tap →
   confirm 'Start Work Order') opens the session; **header = WO name;
   work-type label renders on session details = exact catalog name; iOS tab
   strip is a COMMON strip** (SLD + Condition Assessment on an IR session) —
   NOT per-category like web.

Fixes: `openWorkOrderByName` → activation pattern + strict details-screen
verified-open; `UIStateValidator.visibleContentCount` → bounded first-match
fallback on census wedge (live-hit on TC_WT_LIST_308); Behavior cross-checks
071-077 rewritten from per-category-absence (would false-fail) to the
common-strip contract; +13 `TC_WT_LIST_301-313` label-in-composite tests
(total 452); `WorkTypeBaseTest` resync only after actual creation, recovery
via idempotent loginAndSelectSite. Also caught: IDE (ECJ) had poisoned
`target/test-classes` with broken classes newer than sources — `mvn clean`
required; "Cannot instantiate class" in surefire was that, not code.

## Validation
- `mvn -o clean test-compile` green; verifier self-tests 34/34 green.
- **Live-green on the local iPhone 17 Pro Max sim (iOS 26.2, v1.51):**
  `TC_WT_LIST_308` (53s — label-in-composite) and `TC_WT_DET_083` (1m28s —
  activation-open + label-on-details hard-assert). Both single-test driver-loop
  runs, per house rule.
- Remaining probe-dependent surfaces (per-category tabs beyond the common
  strip, photo-type row on details) stay skip-guarded until a full CI run
  inventories them per type.
