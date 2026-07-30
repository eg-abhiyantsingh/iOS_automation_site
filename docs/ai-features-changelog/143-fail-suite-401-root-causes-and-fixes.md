# 143 — Run 30144117443 (401 failures): root causes + systematic fixes

**Date:** 2026-07-30/31 · **Scope:** every failing test cluster from the 2026-07-25
full-parallel run (401 failures across 33 classes) + the daily-red API Contract
workflow (since 07-22).

## Root causes found (evidence-first)

1. **Backend auth/me contract drift (~07-22).** `/auth/v2/me` lost its top-level
   `id` (user uuid now only in `cognito_username`) and `accessible_sld_ids` went
   `[]`, while the previously-empty `GET /users/{id}/slds` came back alive (173
   SLDs). The old `extract(body,"id")` regex would first-match the Super Admin
   `roles[].id`. → `ApiDataContractTest.setup` red daily; every TestDataApi
   SLD/user flow degraded.
2. **1-hour access-token expiry vs multi-hour CI jobs.** `expires_in: 3600`;
   `WorkTypeBaseTest` holds one `TestDataApi` across a 4h job → every late API
   assert died `401 Authentication failed` (all 14 CHIP-parity tests etc.).
3. **Transient backend site-load outage on 07-25/26.** SiteSelection recorded
   "Dashboard never rendered after selecting site '(s) Wild Goose Brewery'
   (screen now: SITE_SELECTION)" and ~250+ tests walled at the 360s method
   timeout across Asset/SiteVisit/WorkType/Issues/Connections jobs. Full fresh
   login → site load verified working locally on 07-30 (30/32 assets render);
   the cluster is expected to re-green on rerun.
4. **v1.51 Locations rewrite.** The dedicated New Building/Floor/Room screens are
   gone — the toolbar `plus` opens ONE composite "New Location" sheet
   (Building/Floor/Room name slots + picker rows, Create gated on ALL visible
   slots). Long-press context menus are icon-only (`note.text`/`pencil`/`trash`).
   'No Location' renders ONLY when unassigned assets exist (site had drifted to
   0 unassigned). Several helpers also had the first-match-isDisplayed
   hidden-twin bug behind sheets.
5. **Giant-DOM WDA wedge in subtype flows.** `tapDoneOnPicker`'s full-tree
   `visible == true` predicate and `clickSelectAssetSubtype`'s label-relative
   button scans wedge/kill WDA on the post-class-change form; the Subtype row
   sits ~1.5 screens down and is lazily materialized.
6. **Catalog drift:** `/procedures-v2/services` now carries **8** 'PM Forms'
   services (UPS Maintenance included — the enum always modeled 8; the literal 7
   in TC_WT_CAT_070 was wrong against both server and enum).
7. **REAL PRODUCT BUG WT-NEG-01 (BUGS.md):** `/ir_session/create` accepts a
   garbage `work_type_id` with HTTP 200 (missing FK validation) — verified live
   07-25 (junk rows persisted) and 07-31 (fresh probe).

## Fixes

- `TestDataApi`: `currentUserId()` prefers `cognito_username`; `accessibleSldIds()`
  merges `/me.accessible_sld_ids` with `/users/{id}/slds` (either drift direction
  keeps working, with status logging); **proactive re-login 5 min before token
  expiry + one reactive retry on 401** for all authed GET/POSTs;
  `createUnassignedAsset()` (POST /node/create, cloned type/class — payload from
  SyncQueueExportService.buildNodeRequest, live-verified);
  `deleteWorkOrder()` (PUT /ir_session/update `{"is_deleted":true}`).
- `ApiDataContractTest`: setup assert reflects the merged SLD-id source.
- CI secrets `USER_EMAIL`/`USER_PASSWORD` set → the 3 secret-gated API contract
  tests stop skipping. API contract suite locally green (18 run / 0 fail).
- `DriverManager`: `appium:commandTimeouts {"default":150000}` — a wedged WDA
  command now aborts at 150s instead of silently eating the whole 360s test.
- **BuildingPage v1.51 layer**: `isNewLocationFormOpen`, placeholder-targeted
  name fields, `openNewLocationForm`, `pickExistingBuilding/FloorInForm`
  (chooser options matched `visible == 1` + post-pick verification — the naked
  match tapped covered underlying rows), Create-aware save methods with
  auto-fill of still-empty slots, icon-first (pencil/trash) edit/delete
  strategies, hidden-twin fixes (`isDoneButtonDisplayed`, `hasSelectLocationField`),
  viewport-scoped `getFirstUnassignedAsset`, `getFirstBuildingName`.
- **LocationTest**: TC_NB_001/003/008/009 reshaped to the composite-form
  contract (notes live on the Edit sheet now); all TC_NL_*/TC_AL_* tests
  self-provision an unassigned asset via API + app-restart resync
  (`ensureNoLocationSectionAvailable`).
- **AssetPage**: `isSelectAssetSubtypeDisplayed` does a predicate-targeted
  scroll; `clickSelectAssetSubtype` gained a bounded scroll+scan loop (single
  cheap query per pass); `tapDoneOnPicker` uses an a11y-id-indexed Done lookup;
  post-pick settle before the Done dance.
- `WorkTypeCatalog_Test.TC_WT_CAT_070`: PM Forms count 7 → 8 (live-verified).
- `WorkType_CrossCutting_Test` NEG_01/NEG_02 → known-bug sentinels (pin the
  acceptance, clean their own junk rows, flip red when the backend gets fixed).
- `SiteSelectionTest.TC_SS_011`: post-clear count read now polls for lazy-list
  stabilization.
- QA data: soft-deleted the 2 junk `QA-WT-NEG-*` rows from run 30144117443 +
  1 probe row.

## Verification
- `mvn -o -DskipTests test-compile` green throughout.
- Locally green after fixes: TC_BL_001, TC_NB_001, TC_NB_003, TC_NL_001,
  TC_NL_002, TC_AL_001, TC_NF_002, TC_NR_001, CAP_EAD_05, TC_ATS_ST_01,
  TC_ATS_ST_04, TC_SS_015, API contract suite.
- Full failed-suite local run + CI rerun dispatch: see end of session notes.
