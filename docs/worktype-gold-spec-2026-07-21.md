# Work Order "Work Type" — Gold Spec (live-verified 2026-07-21)

Captured by driving the live web app `https://acme.qa.egalvanic.ai` (V1.34) with
Playwright and intercepting its API traffic. This is the source-of-truth for the
`TC_WT_*` iOS work-type suite. Companion memory: `worktype-catalog-gold`.

## 1. The catalog — 13 service-backed work types + "General"

Source of truth: **`GET /api/procedures-v2/services`** (fires when the web
Create-WO dialog opens). Returns exactly 13 services. The web dropdown shows
**14 options** = these 13 + a UI-only **"General"** appended last, which maps to
`work_type_id = null` on the created WO (verified via captured POST).

| # | Display name | key | Category `type` | de_energized | procedure_count |
|---|---|---|---|---|---|
| 1 | Arc Flash Data Collection | `arc-flash-study` | **AF** | false | 35 |
| 2 | Arc Flash Label Placement | `arc-flash-label-placement` | **Checklist** | false | 13 |
| 3 | Cleaning | `cleaning` | PM Forms | true | 18 |
| 4 | Clean, Tighten, Torque | `clean-tighten-torque` | PM Forms | true | 19 |
| 5 | Condition Assessment | `condition-assessment` | **COM** | false | 30 |
| 6 | De-Energized Visual Inspection | `de-energized-visual-inspection` | PM Forms | true | 19 |
| 7 | DGA / Fluid Sample Analysis | `dga-fluid-sample-analysis` | PM Forms | false | 1 |
| 8 | Infrared Thermography | `infrared-thermography` | **IR** | false | 30 |
| 9 | Insulation Resistance Testing | `insulation-resistance-testing` | PM Forms | true | 17 |
| 10 | NETA Testing | `de-energized-testing` | PM Forms | true | 19 |
| 11 | Panel Schedule Updates | `panel-schedule-updates` | **Schedule** | false | 2 |
| 12 | Shutdown (Composite) | `composite-shutdown-emp` | PM Forms | true | 0 |
| 13 | UPS Maintenance | `ups-maintenance` | PM Forms | false | 1 |

Service UUIDs (deterministic UUIDv5 — stable across syncs; version nibble = 5):

```
arc-flash-study                 d625cfa0-5447-52c5-858e-9ecd5c84d0fb
arc-flash-label-placement       9de69871-ad71-56f4-8f04-515b5738770b
cleaning                        180c4243-25df-581c-895a-9e883f38948f
clean-tighten-torque            8e578df1-2b96-5733-8f0e-c00fef0a92b8
condition-assessment            173c2ca2-8e86-5c95-9b1f-0724ddaccd8b
de-energized-visual-inspection  01ad81ff-63fe-507e-beb0-305d7f67dad9
dga-fluid-sample-analysis       5dff8199-3579-56c6-b81d-dc4e9b4dcd3d
infrared-thermography           3b732d14-461c-54a7-8e30-70391bd34dd6
insulation-resistance-testing   d9c9efef-914f-5656-b510-e156bd07ba63
de-energized-testing (NETA)     0d914f81-a750-5833-8c46-5c71064f676e
panel-schedule-updates          3f92b954-8d88-5045-83a8-ee7d9ace504d
composite-shutdown-emp          f9fb8d4a-2ccd-5bdb-baac-278dc4dc6cfb
ups-maintenance                 8c5cf34c-ed04-5c5e-9bff-973410762b13
```

Traps / notes:
- **NETA Testing's key is `de-energized-testing`** (NOT neta-*) — key ≠ slug of name.
- Category distribution: 7× PM Forms, 1 each AF / Checklist / COM / IR / Schedule.
- 6 of 13 are `de_energized: true` (Cleaning, CTT, De-Energized Visual, IRT-Insulation, NETA, Shutdown).
- "Shutdown (Composite)" has `procedure_count: 0`.
- The **create form is type-agnostic** — identical fields for all 14 dropdown
  options (verified by diffing dialog innerText per selection). Type-specific
  behavior appears on the WO DETAIL page / iOS session, not in the create dialog.

## 2. Create-WO API (captured from the web dialog)

```
POST /api/ir_session/create          (client generates the UUID)
{"id":"<uuid4>","name":"...","description":null|"...","photo_type":"FLIR-SEP|FLUKE",
 "sld_id":"69e5c429-8d1c-4122-ae1f-bf71b0471aa3","priority":"Medium",
 "start_date":"2026-07-21T00:00:00.000+05:30","due_date":null,
 "date_created":"<iso>","active_visual_prefix":"visual_","active_ir_prefix":"ir_",
 "active":true,"job_id":null,"est_hours":8,"work_type_id":"<service uuid|null>",
 "asset_scope":null}
POST /api/mapping/user-session/create {"user_id":"...","session_id":"<id>","mapping_type":"certifier"}
```

List endpoint is **POST** (405 on GET):
`POST /api/company/{companyId}/workorders/v2` body `{"page":1,"page_size":100,"search":"","filters":{}}`
→ `data.items[]` each with `work_type_id` (null on all legacy pre-work-type WOs).

Required headers on web-origin `/api` calls: `x-subdomain: acme`,
`x-active-role-id: <role uuid>`, `x-language: en-GB` (without `x-subdomain` the
proxy serves the SPA index.html). TestDataApi (api.qa.egalvanic.ai/api) uses
`Authorization: Bearer` + `X-Subdomain` instead.

Web company id (acme): `d59d449b-09d8-45d6-8f0a-ef70024b1293`.
Fixture site (iOS first-site): sld_id `69e5c429-8d1c-4122-ae1f-bf71b0471aa3` (Android Qa Site1).
Admin web user id: `77e99d86-7f0a-4345-b056-6f470bb668ec` (abhiyant admin).

## 3. QA-WT fixture family (created 2026-07-21, active, on Android Qa Site1)

One live WO per work type + one General, named `QA-WT<NN> <Name>` (no date —
durable fixtures; description says "do not delete"). All priority Medium,
est_hours 8, photo_type FLUKE (except WT00 = FLIR-SEP form default), scope = all
assets (asset_scope null). Certifier mapping → abhiyant admin.

```
QA-WT00 General                          work_type_id=null   5bf3fcbb-23f3-4387-84ce-339b342839fb
QA-WT01 Arc Flash Data Collection        d625cfa0…           73964175…
QA-WT02 Arc Flash Label Placement        9de69871…           4f063b7c…
QA-WT03 Cleaning                         180c4243…           bc9308d0…
QA-WT04 Clean Tighten Torque             8e578df1…           e8874f34…
QA-WT05 Condition Assessment             173c2ca2…           51682320…
QA-WT06 De-Energized Visual Inspection   01ad81ff…           7c605f0f…
QA-WT07 DGA Fluid Sample Analysis        5dff8199…           4c1d37ab…
QA-WT08 Infrared Thermography            3b732d14…           bb9c945a…
QA-WT09 Insulation Resistance Testing    d9c9efef…           f18dc99d…
QA-WT10 NETA Testing                     0d914f81…           4ab6420b…
QA-WT11 Panel Schedule Updates           3f92b954…           8dc31803…
QA-WT12 Shutdown Composite               f9fb8d4a…           f9bc6090…
QA-WT13 UPS Maintenance                  8c5cf34c…           7153350a…
```

(Names intentionally strip "/" and "," — e.g. "QA-WT07 DGA Fluid Sample
Analysis" — to keep NS-predicate matching simple on iOS. Full type names still
asserted via the catalog, not the fixture name.)

There is also the pre-existing `QA-AUTO AF Data Collection 2026-07-21`
(changelog 140) with a Circuit Breaker class scope — leave it; arc-flash session
tests target it by name.

## 3b. iOS visibility contract + sync trigger (probe-derived, 2026-07-21)

- **iOS gets sessions from the whole-SLD sync** (`GET /sld/v3/{sldId}` →
  `ir_sessions[]`, each WITH `work_type_id`, + `mappings.user_session[]`).
  There is no separate session-list endpoint on mobile.
- **A WO is only shown in the iOS Work Orders list when the logged-in user has
  a `user_session` mapping** (iOS-created WOs get `field_technician`; fixtures
  without mappings never appeared despite being in the payload). Fixtures now
  carry BOTH `field_technician` + `certifier` mappings for the QA admin user —
  keep it that way for anything the iOS suite must see.
- **`POST /mapping/user-session/create` silently no-ops without a client `id`**
  in the payload (returns ok-ish, persists nothing). Always send
  `{"id":"<uuid4>","user_id","session_id","mapping_type"}` → HTTP 201.
- The app does NOT re-pull the SLD on cold relaunch or WO-list pull-to-refresh
  with a warm login; **site (re-)selection is the reliable sync trigger**.
- **First-site drift:** the iOS suite's `loginAndSelectSite` picks the FIRST
  site, which is now **"(s) Wild Goose Brewery"** (sld
  `9138fd14-a3c9-495a-b086-6ef520f92168`) because "(s)" sorts before "A" —
  NOT "Android Qa Site1" anymore. QA-WT fixture family exists on BOTH sites;
  `WorkTypeBaseTest.ensureFixturesOnLandedSite()` self-provisions wherever the
  app lands. (This also means older fixtures targeted at Android Qa Site1 —
  e.g. "QA-AUTO AF Data Collection 2026-07-21" — are invisible to the current
  first-site suite runs.)

## 3c. iOS v1.51 WO list row anatomy (probe run 7, live-verified)

Typed WO rows are Buttons named **`<name>, <work-type display name>, <priority>`**
— the work-type label IS on the list row, punctuation intact:

```
QA-WT03 Cleaning, Cleaning, Medium
QA-WT04 Clean Tighten Torque, Clean, Tighten, Torque, Medium
QA-WT08 Infrared Thermography, Infrared Thermography, Medium
```

Legacy/null-type rows keep the old `<name>, <priority>` shape ("Work Order -
Jul 18, 10:17 AM, Medium"). Consequences:
- `BEGINSWITH <fixture name>` + `ENDSWITH ', <priority>'` remain safe parses;
  positional comma-splitting is NOT (labels contain commas).
- Verified-open must use the strict session-details check — the row-gone
  check false-negatives under SwiftUI previous-screen bleed-through.
- Rows visible after fresh-install sync ONLY (see §3b) — warm local sessions
  show the stale pre-sync list; CI's per-job fresh install syncs naturally.

## 3d. iOS v1.51 session-details anatomy + open contract (probe run 11)

- **Opening a WO = STARTING it**: a plain element.click() on the list row is a
  NO-OP. The working interaction (house pattern, startFirstAvailableWorkOrder):
  pause auto-alerts (`defaultAlertAction=""`), coordinate-tap the row
  (x+40, midY), confirm the **'Start Work Order?'** alert by coordinates, then
  restore `defaultAlertAction=accept`. `WorkOrderPage.openWorkOrderByName`
  encodes this; verified-open = strict `isSessionDetailsScreenDisplayed`.
- **Session-details header = the WO name** ('QA-WT08 Infrared Thermography').
- **The work-type label RENDERS on the session screen** and equals the catalog
  display name exactly — `getWorkTypeLabelOnScreen()` returned
  'Infrared Thermography' on QA-WT08. Class 3's conditional hard-asserts are
  ACTIVE on this build.
- **iOS session tabs are a COMMON strip, NOT per-category like the web**: on
  the IR-type session the visible tabs were Details · Assets · Locations ·
  Issues · Files · **Condition Assessment** · **SLD**. ("IR Photos", "Forms",
  "Tasks", "Panel Schedules", "Equipment Designations", "Arc Flash" were NOT
  detected there — not yet classified common-vs-per-category.) Behavior
  cross-checks pin the common-strip contract for SLD/Condition Assessment and
  skip-guard the unclassified tabs.
- `getSessionType()` (photo-type stacked read) returned null on this screen —
  photo-type row anatomy changed or lives elsewhere; keep that assert tolerant.

## 4. iOS v1.51 binary facts (strings dump of Z Platform-QA.app)

- Binary contains `work_type_id`, `_work_type_id`, `WorkType`, `workTypeLabel`,
  `workType=` / `) work_type_id=` (log format strings) → v1.51 **consumes and
  displays** a work-type label somewhere (exact surface confirmed in Part 3 iOS probe).
- Binary does NOT contain the 13 display names → labels are resolved at runtime
  from synced data (like enum_node_voltages), NOT hardcoded.
- `app-source/` in this repo is STALE (pre-work-type): IRSessionDTO there has no
  work_type field. Do not trust it for work-type UI anatomy.

## 5. Web WO detail page — PER-TYPE CONTRACT (live-diffed across all 14 fixtures)

**The subtitle line under the WO name = the exact work-type display name**
(incl. punctuation: "Clean, Tighten, Torque", "DGA / Fluid Sample Analysis",
"Shutdown (Composite)"; General shows "General").

| Fixture | Progress % | Tabs | Variable grid columns |
|---|---|---|---|
| WT00 General | – | Assets · Tasks · Forms · Issues · **IR Photos** · Attachments | Tasks |
| WT01 AF Data Collection | – | Assets · **SLD** · **Equipment Designations** · Issues · Attachments | **Arc Flash** |
| WT02 AF Label Placement | – | Assets · **Tasks** (badge 30) · Issues · Attachments | Tasks |
| WT03 Cleaning | 0% | Assets · **Forms** (98) · Issues · Attachments | Forms |
| WT04 Clean Tighten Torque | 0% | Assets (99+) · Forms (99+) · Issues · Attachments | Forms |
| WT05 Condition Assessment | 15% | Assets (55) · **Condition Assessment** · Issues · Attachments | Tasks · **C.O.M.** |
| WT06 De-Energized Visual | 0% | Assets · Forms (99+) · Issues · Attachments | Forms |
| WT07 DGA Fluid Sample | – | Assets · Forms (no badge) · Issues · Attachments | Forms |
| WT08 Infrared Thermography | – | Assets · Issues · **IR Photos** · Attachments | IR Photos |
| WT09 Insulation Resistance | 0% | Assets · Forms (44) · Issues · Attachments | Forms |
| WT10 NETA Testing | 0% | Assets (99+) · Forms (99+) · Issues · Attachments | Forms |
| WT11 Panel Schedule Updates | – | Assets · **Panel Schedules** · Issues · Attachments | Schedule |
| WT12 Shutdown Composite | – | Assets · Forms (no badge) · Issues · Attachments | Forms |
| WT13 UPS Maintenance | – | Assets · Forms (no badge) · Issues · Attachments | Forms |

Category rules distilled:
- **AF** → SLD + Equipment Designations tabs, Arc Flash grid column, af-ready
  tracker (changelog 140), NO %-progress chip.
- **Checklist** → Tasks tab + Tasks column (13 procedures fanned out to 30 tasks).
- **PM Forms** → Forms tab + Forms column; %-progress chip appears ONLY once
  per-asset forms are generated. Generation is **async AND class-conditional**:
  DGA / UPS / Shutdown produced 0 forms on this site (their procedures target
  classes the site lacks / 0 procedures) → no badge, no % chip. NEVER
  hard-assert form/task counts or % values; assert structure.
- **COM** → "Condition Assessment" tab; grid shows Tasks + C.O.M. columns; %
  can be non-zero immediately (computed over pre-existing condition data).
- **IR** → IR Photos tab + IR Photos column (same as the classic pre-work-type
  session view).
- **General (null)** → superset legacy view: Tasks + Forms + IR Photos tabs,
  "Data Mask" action visible.
- Asset-grid population after create is ASYNC (some fixtures showed 0 rows
  minutes in, others 52-71 rows); iOS tests must tolerate either state.
