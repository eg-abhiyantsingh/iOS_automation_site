# 194 — ZP-3914 completed on app v1.73: every acceptance item covered, v1.73 breakers fixed

**Date:** 2026-10-30 IST
**Prompts:** "https://egalvanic.atlassian.net/browse/ZP-3914 complete this ticket" · mid-turn: "cover all this test case"
**App under test:** v1.73 simulator build (27 Oct 2026), now also in CI (`641fb88`, "update Z Platform-QA to v1.73").

## 1. Where the ticket stood

Changelog 193 (21 Oct, v1.69) left 47 tests: 44 pass locally, 2 product fails, 1 version skip. The one CI run
(39602048381) was far from green: zp3914-am 18/25, zp3914-forms 14/22. Bulk export and Instance tags were marked
"not automatable on iOS".

## 2. CI failures of run 39602048381: root causes and fixes

| Test(s) | Root cause | Fix |
|---|---|---|
| TC_SWW_04, TC_SWW_05, TC_SWO_03, TC_PM_06 | Every assertion passed; the 360 s default cap cut the test during cleanup. CI snapshots of these screens cost 5-20 s each | ZP job passes `-DTEST_TIMEOUT_SECONDS=900` |
| TC_SWW_01 (skip) | WDA lost the foreground app right after a soft restart: every tap failed with `point.x != INFINITY` | `SnapPage.reactivateApp()` (mobile: activateApp) + one retry in `ExperiencePage.openSettingsRoot` |
| TC_SWO_02 | A walk queued during the 503 backend outage (TC_SWC_02) synced together with the test's own walk | Expect our walk plus every walk queued before the test |
| TC_SWC_02 | Backend `GET /site-walk/list` returned 503 for the whole test | Environment; no change |
| TC_GV_01, TC_GV_02 | The editor DID open (failure shot), but only after ~29 s; the wait was 10 s and only looked for a StaticText | 20 s wait; the 'Edit Form' title is accepted on the NavigationBar too. These two runs also LEAKED two 'Bolted Connections Resistance' drafts on Switch-1 (addForm returned null, so they were never recorded); deleted 2026-10-30 |
| TC_PM_01, TC_PM_05 (skip) | The out-of-process photo picker took >12 s on its first load and swallowed the single tap | 25 s wait, up to 3 taps, real cell when exposed |
| TC_TOU_03 | The app adds the Terms / Privacy links only when their URLs arrive from the backend (`LegalConsentCheckbox`: `if let termsURL`); checked 1 s after the sentence | Wait up to 20 s; failure message lists every consent node |

## 3. App v1.73 changes that broke the module (probed live, contracts in memory `v173-ui-contracts`)

| Surface | v1.73 change | Fix |
|---|---|---|
| Login › site picker | Site rows gained a long-press menu 'Edit Site' (Edit Site › Office row › 'Select Office'). On the first launch after install, a row tap registered as a long press and the run ended on 'Select Office' | `SiteSelectionPage.escapeSiteEditSurfaces()`: Select Office › Done, Edit Site › Cancel, context menu › tap outside; then press the row again |
| Dashboard header | 'Hi, <user>!' above 'Welcome to <site>'; the greeting was read as the site name, so fixture provisioning resolved no SLD | `getCurrentSiteName()` reads the 'Welcome to' line first and skips greetings |
| Site Walks list | Floating tab bar covers the last row; its status icon is not in the snapshot (TC_SWW_01) | `walkRows()` keeps fully drawn rows only |
| Wizard customer search | iOS 26 docks the search at the bottom with a round ✕; no top-right 'Cancel' (TC_SWW_02) | `searchDismissButton()`: the Button right of the search field on the same row |
| Network menu | Popover opens over the toolbar ('Go Offline' at y≈62) (TC_SWO_01/02) | Menu band starts at y 40 |
| Settings › Switch Experience | Press swallowed once after a site-walk teardown (TC_SWC_02/06 skips) | `switchTo` retries the chooser once |
| Asset row menu (WO room) | 'Add Form' moved into 'Forms (n)' › sheet 'Select a Form' › 'Add Form'. With exactly ONE form, 'Forms (1)' opens that form directly, so there is no add path (finding EGF-ADD-01) | `WorkOrderFormsPage.offersAddForm / pressAddForm / openExistingForm`. Ghost tests keep Switch-1 at ≥2 forms by attaching one fixture form ('Clean, Tighten, Torque — Cleaning') through the web's form-coverage + bulk-forms routes, then the room's Refresh |
| WO room rows | Rows read 'Switch-1, Switch, 0 of 1 complete' and no longer list the asset's forms, so the room long-press delete is gone | Cleanup deletes through Forms tab › row long-press › Delete (`deleteInstanceRow`), never below the pre-test count |
| Forms tab | Level 1 '<title>, s/t submitted - <classes>, <state>'; Level 2 rows are Buttons '<asset>, <room> - k/n sections, <status>'; no nav 'Select'. Row long-press = Select · Copy Data To… · Delete; select mode starts with that row selected; bar 'N Selected' · Select All / Cancel · Edit · More (Mark Draft / Mark Submitted) · Delete | `SessionFormsTabPage` parses both layouts; `enterSelectModeOn(ownRow)` for destructive flows; tests read `selectedOnEntry()` (0 on 1.69, 1 on 1.73) |
| Form editor | 'Close' + new 'Photos' button; footer label is 'Save Draft' | Renderer tests accept Save / Save Draft |

## 4. "Cover all this test case": the two uncovered acceptance items

ZP-3914 lists **Bulk export** and **Instance tags**. The iOS v1.59 release notes trace both to **ZP-3840** ("NETA-3 Forms
tab: multi-edit working sets, bulk export, and instance tags"). On iOS, ZP-3840 shipped only the multi-edit half. The
v1.73 binary has `/eg-form-instance/bulk-patch` but no export or tags route, string or control. The export dialog and
tag UI live in the web Forms tab. Both features are therefore covered at the backend contract they share, through
the exact calls the web Forms tab makes (read from the QA web bundle `SessionDetail-*.js`):

New driver-free class `ZP3914_FormsBulkExportTags_ApiTest` (10 tests, in `testng-zp3914-forms.xml` + local suite):

| Test | Checks | Result (QA, 30 Oct) |
|---|---|---|
| TC_TAG_01 | by-session listing carries `tags` + `template_format` | PASS |
| TC_TAG_02 | bulk add on 2 instances in one call (updated=2, tags_by_instance), listing shows it, bulk remove clears it | PASS |
| TC_TAG_03 | 51-char tag rejected; the valid tag in the same request NOT applied (atomic); 50 chars accepted | PASS |
| TC_TAG_04 | 21 tags in one request rejected, nothing applied | PASS |
| TC_TAG_05 | registry stores palette key 'teal', rejects hex '#ff0000', keeps 'teal' after the rejection; restored | PASS |
| TC_BX_01 | `pdf` export of 2 forms → SUCCEEDED → downloadable joined PDF (%PDF, ≥2 pages) | PASS (131 KB, 3 page objects) |
| TC_BX_02 | `pdf_zip` → zip with one uniquely, Windows-safely named PDF per form | PASS ('Demo1 — Insulation Resistance Testing (2deab2de).pdf', …) |
| TC_BX_03 | DOCX of a non-docx template refused | PASS |
| TC_BX_04 | templateless form dropped as skipped_no_template | SKIP: no templateless form in any of the 3 QA-WT10 sessions |
| TC_BX_05 | tenant gating: unknown session starts no job; foreign instance id → 400 (export + bulk-tags) | PASS |

Observation (not filed): an export against a session id that does not exist answers **HTTP 200 with an HTML page**
(no redirect) instead of a JSON 4xx. No job starts, so gating holds, but the API should return JSON.

**EG Forms renderer** (ZP-3743, the "EG Forms" item beyond ghost values), 3 new tests in `ZP3914_GhostValues_Test`:
TC_EGF_01 'Close' top-left, fixed footer Sections · Save Draft · Submit that does not move when the form scrolls, no
'Delete' inside the form · TC_EGF_02 footer stays above the keyboard · TC_EGF_03 'Sections' drawer lists the
sections and choosing one brings it to the top.

## 4b. Coverage map: all 17 acceptance items

COVERAGE

## 5. Validation

VALIDATION

## 6. Product defects

DEFECTS
