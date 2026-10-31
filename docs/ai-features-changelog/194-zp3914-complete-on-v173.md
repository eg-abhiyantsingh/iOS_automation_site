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

| # | Acceptance item (ZP-3914) | Covered by |
|---|---|---|
| 1 | Site Walk: Account Manager Experience | TC_EXP_03, TC_SWW_01 |
| 2 | Experience chooser | TC_EXP_01-05 |
| 3 | Walk wizard | TC_SWW_02-09 |
| 4 | Count grid | TC_SWC_01-07 |
| 5 | Offline sync | TC_SWO_01-04 |
| 6 | NETA-3 Forms Tab | TC_FT_01-05 |
| 7 | Multi-edit working sets | TC_ME_01-05, TC_GV_02 |
| 8 | Bulk export | TC_BX_01-05 (API; no iOS UI in v1.73) |
| 9 | Instance tags | TC_TAG_01-05 (API; no iOS UI in v1.73) |
| 10 | Photo Viewer | TC_PM_01, TC_PM_06 |
| 11 | Zoom tools | TC_PM_06 (pinch), TC_PM_07 (double tap, runs from 1.70) |
| 12 | Markup tools | TC_PM_02-05 |
| 13 | EG Forms | TC_EGF_01-03 (renderer), TC_GV_01-02 |
| 14 | Preload previous asset values as blue ghost values | TC_GV_01-02 |
| 15 | Offline support via SwiftData | TC_SWO_02-03 (queue survives relaunch), TC_SWO_04 |
| 16 | Terms of Use V2 | TC_TOU_01-02 |
| 17 | Updated Terms & Conditions screen | TC_TOU_03 |

Module total: **60 tests** (47 from changelog 193 + 10 API + 3 renderer).

## 5. Validation

### Local, app v1.73 (iPhone 17 Pro Max / QA-Automation-17ProMax simulators, iOS 26.2)

| Class | Result on v1.73 | Notes |
|---|---|---|
| ExperienceChooser | 5/5 | |
| SiteWalkWizard | 8/9 + 1 product fail | TC_SWW_08 = SW-WIZ-01. TC_SWW_01 green after the tab-bar fix; TC_SWW_02 cancel fix verified on iOS 26 (rerun after CI, see below) |
| CountGrid | 5/7 | TC_SWC_02/06 skipped once on the swallowed Switch Experience press (retry added) |
| SiteWalkOffline | rerun | network popover band fixed after the first local pass |
| FormsBulkExportTags_ApiTest | 9/10 + 1 skip | TC_BX_04: no templateless form exists |
| FormsTab | 5/5 | TC_FT_04 after the 'No matches' / inline 'Clear Filters' fix |
| MultiEdit | 5/5 | v1.73 long-press select mode |
| GhostValues (ghost + renderer) | TC_GV_02, TC_EGF_01-03 pass | TC_GV_01: menu read retry added after its run (rerun after CI) |
| PhotoMarkup | 6/7 + 1 product fail | TC_PM_04 = MKP-LABEL-01; TC_PM_07 double-tap zoom now runs (440 → 1100 pt) |
| TermsOfUse | 3/3 | |

The QA-Automation simulator's display died mid-run under a load average of ~60 (three simulators booted). The remaining
tests moved to the second simulator. CI run 41080855780 (commit 50316bb, v1.73) ran both ZP-3914 shards in parallel.

Other gates: `mvn -o -DskipTests test-compile` clean; verifier self-tests 39/39; no ZP3914 test flagged by
`check_assertion_coverage.py`.

### CI, app v1.73 (iPhone 16 Pro, iOS 18.5)

**Run 41080855780** (commit 50316bb, before the later v1.73 fixes): zp3914-am **20/25** (was 18/25 on v1.69),
zp3914-forms **21/35** (6 fail, 8 skip). Everything not product-related was fixed afterwards:

| Test(s) | Cause | Fixed in |
|---|---|---|
| TC_PM_01-07 (skip) | v1.73 asset list search is a TextField 'Search assets...' (not a SearchField) | 5ac567f |
| TC_FT_04, TC_EGF_03 | 'No matches' + inline 'Clear Filters'; drawer read by text diff | f676e0b |
| TC_ME_01, TC_ME_05 | the 9th row under the selection bar was not counted (y cap) | 6888d12 |
| TC_GV_02 | 900 s time-out (it needs ~880 s) — and the dead session leaked 2 forms (removed via bulk-forms) | 6888d12 (1500 s + backend cleanup fallback) |
| TC_TOU_03 | iOS 18 exposes the consent links only inside one StaticText | 6888d12 (sentence asserted on iOS < 26) |
| TC_SWO_02 | expected the queue to return to its pre-test size; a full sync empties it | 6888d12 |
| TC_SWC_06 | the walk needed > 20 s to reach the backend | 6888d12 (45 s) |
| TC_SWW_07 | wizard's last step unconfirmed although the walk was created (leaked c3653ac7, deleted) | 6888d12 + 92ec84d (step log, retry, track on null) |
| TC_SWW_06 (skip) | app stayed on its launch screen after login — environment | — |
| TC_SWW_08 | product defect SW-WIZ-01 | — |

**Final run 41108150037** (commit 92ec84d): zp3914-am **24/25**, zp3914-forms **31/35** (2 fail, 2 skip).

| Test | Result | Cause | Action |
|---|---|---|---|
| TC_SWW_08 | fail | product defect SW-WIZ-01 | Jira draft |
| TC_PM_04 | fail | product defect MKP-LABEL-01 | Jira draft |
| TC_ME_02 | fail | the first 'Close' tap on the 2-form editor was swallowed on iOS 18.5 (editor open, no dialog, 8 s later) | cec8001: second press when the editor stays |
| TC_EGF_02 | skip | at 402 pt the procedure table's fixed-width 'Value / Notes' column is clipped at the card edge. First read as "TextField not visible"; corrected below: the centre tap lands on the clipped edge | cec8001 (partial); real fix after run 41152258265 |
| TC_BX_04 | skip | data: no QA-WT10 session holds a templateless form next to a templated one | — (the case runs whenever such data exists) |

Everything else passed on CI, including the 9 runnable API cases, TC_SWO_02/03, TC_SWC_06, TC_SWW_02/06/07, TC_TOU_03, TC_GV_01/02 and
TC_EGF_01/03. No walk or form was leaked (GET /site-walk/list after the run: nothing created by the suite; Switch-1 back to
its 2 baseline forms).

**Forms re-check run 41131914013** (commit cec8001, new `zp_shards=zp3914-forms-only`): **29/35**. TC_ME_02 now
passes. Three tests that passed in 41108150037 failed on timing, and TC_EGF_02 still skipped:

| Test | Seen on CI | Fixed in e53640c |
|---|---|---|
| TC_TOU_03 | Connections tab up, no 'Logout' row: SettingsPage's element check reported Settings | `openSettingsRoot` trusts the tab bar's selected tab; `logoutFromSettings` presses Settings and looks again |
| TC_GV_02 | asset menu read `[]`, yet the menu was fully drawn in the failure shot | row wait + 10 s menu read + reasons logged |
| TC_GV_01 | the typed field went stale once the keyboard re-laid the form out | value read back from a fresh snapshot |
| TC_EGF_02 | the snapshot fallback found no clipped field | element-query fallback (no snapshot depth cap) + text-input diagnostics |

No data leaked: TC_GV_01's cleanup deleted its Bolted Connections Resistance form; Switch-1 stayed at its 2 baseline forms.

**Forms re-check run 41152258265** (commit e53640c): **32/35**. TC_TOU_03, TC_GV_01, TC_GV_02 and TC_ME_02 pass;
the only fail is the product defect TC_PM_04, plus the TC_BX_04 data skip. TC_EGF_02 still skipped, and printed no
text-input diagnostics: those print only when no field is found, so on CI a field WAS found and tapped, but no
keyboard came up within 5 s. The CI simulator does show a software keyboard (TC_PM_04's failure shot), so the tap
itself misses. The ghost-value tests left nothing on Switch-1.

**TC_EGF_02 root cause (reproduced on a 402 pt iPhone 17, iOS 26.2).** The earlier "not visible" diagnosis was
wrong. At 402 pt the 'Value / Notes' TextField reads `visible="true"`, frame x 324-449, so `focusFirstTextField`
took its first branch and tapped the field's centre, x 386. That is exactly the card's clipped right edge (402 - 16),
so all three CI runs tapped the border. Live probe on the open form: tap at x 386 → no keyboard; tap at x 353 →
keyboard up, the table scrolls the field into view, and the footer (y 426) stays above the keyboard (top 583). At
440 pt the same centre lies inside the card, which is why the test always passed locally. Fix: every tap aims at the
middle of the field's on-screen part (`onScreenCenterX`), a second press goes near the leading edge, and the
diagnostics now print when a tap raises no keyboard (fde4dbd). **Local validation on the 402 pt iPhone 17: TC_EGF_02
PASS** (keyboard top 583; Sections · Save Draft · Submit at y 426).

**Asset-menu read.** Two local 402 pt runs failed before the field step: the long-press menu was fully drawn in the
failure shot, yet the read returned `[]` even after the 10 s wait from e53640c. `snapshot()` answers an empty list
when getPageSource fails, so a time-boxed wait can be used up by failed reads that look like "no menu". The menu read
now counts an empty snapshot as a failed read, needs 3 real reads (or 10 s), and after failed reads falls back to an
element query for the same buttons, logging how many page-source reads failed. The empty read did not recur on the
next two runs, so the fallback branch itself is not yet exercised. The normal path is validated: the next local
run read the full menu and passed every TC_EGF_02 assertion (WDA then died on the final screenshot, a local
load-average-25 crash also seen in the first attempt). One Bolted Connections Resistance instance
(248f18ed, created 01:56 UTC by a local TC_GV_01 run before e335c61) was removed through bulk-forms; Switch-1 is back
to its 2 baseline forms.

**Forms run 41170046135** (commit fde4dbd; 00c4dd2 came after it): **33/35. TC_EGF_02 PASSES on CI** (iPhone 16
Pro, 402 pt, 201 s; the first tap raised the keyboard, so no "no keyboard" diagnostics printed). The only fail is the
product defect TC_PM_04, plus the TC_BX_04 data skip. The Summary job's test gate is red on TC_PM_04 alone; it is not
quarantined because `failed-suites/quarantine.txt` requires a filed bug.

**Cleanup leak found after that run.** Switch-1 held two live Bolted Connections Resistance instances (bcff8752,
eb2225ca) from TC_GV_02. Its cleanup is the Forms tab multi-select 'Delete 2 forms?': the list emptied, the
assertion passed and the app read 'Forms (2)' for the rest of the run, but neither instance was ever modified on the
backend, and at 08:21 UTC the fixture site row read 'Wild Goose Brewery … 2 pending' (no 'pending' at 05:07).
TC_GV_01's per-row deletes in the same run did reach the backend. Reproduced locally on the 402 pt iPhone 17 (iOS
26.2) using those two leaked instances: Forms tab › BCR › long-press › Select › both › Delete → the backend showed 0
instances within 67 s and stayed at 0. So the multi-select delete syncs locally and this was seen once on CI only;
it is NOT reported as a product defect.

The test-side gap was real: once the UI showed the forms gone, `created` was cleared and teardown never asked the
backend. Fix: every added title is kept in `addedTitles`; teardown (`confirmGoneOnBackend`) waits up to 90 s for the
backend to drop each title that ASSET did not hold before (baseline 0), then removes what is left through bulk-forms
and logs how long it waited. A title with a non-zero or unrecorded baseline is never touched. `BaseTest.sleep()`
returns at once (WebDriverWait `until(d -> true)`), so the poll uses `Waits.until(…, 90 s, 10 s)`.
Validated driver-free against the live backend with a scratch probe calling the teardown by reflection: a seeded,
unsynced BCR was removed after 91 s (`removed: 1`), the fixture CTT form (baseline 1) and a title without a baseline
were left alone (the latter returned in under 5 s), and Switch-1 ended at its 2 baseline form ids with 0 BCR.

**Confirmation run 41188709540** (commit 817143e, with 00c4dd2): **31/35**. TC_PM_04 (product) and TC_BX_04 (data) as
before. The new ghost teardown ran on CI: both ghost tests' deletes reached the backend inside the 90 s wait, so no
backend removal was needed and nothing leaked. Two other results:

- TC_EGF_02 SKIP, launch stall. After the soft restart the app sat on a screen detection could not name for 4 min
  (each detection read took ~74 s), through the test's own login retry. It never reached the form, so it says
  nothing about the field-tap fix (which passed on 41170046135); TC_EGF_03 passed straight after. CI environment.
- TC_TOU_03 FAIL "Settings › Logout must sign out", ending on the Connections tab — the same symptom as run
  41131914013. openSettingsRoot printed no "tab bar shows … selected" line, so Settings WAS selected; swipes do not
  change tabs and no alert came up, so the only tap, the Logout press, landed on the Connections tab button
  (x 154-248, top 791 at 402 pt). A full-width row's centre is x 201: Logout was resting under the tab bar, where
  it still reads visible. `BasePage.sleep()` does not wait either, so the read after each swipe can catch the row
  mid-scroll. On iOS 26.2 the swipes left Logout at y 721-771, clear of the bar, so the exact CI rest position does
  not reproduce locally (iOS 18.5 lays the list out differently). Fix: `logoutFromSettings` swipes until Logout's
  whole frame is above the tab bar, then presses the settled row with the tap clamped above the bar, and never
  presses when no part of the row clears it. Same rule as TC_EGF_02: never tap a clipped element's centre.
  Local run on the 402 pt iPhone 17: "Settings › Logout must sign out" PASSED, then the sign-in chooser and all three
  consent assertions passed. WDA died right after ("Error communicating with the remote browser", load ~39 — the
  known local crash), so the link and re-login steps ran out the 600 s timeout. The changed step is validated
  locally; the full test is left to the next CI run.

**Final run 41206906956** (commit 475f2ea): **33/35**, the best this shard can show until MKP-LABEL-01 is fixed.
TC_TOU_03 PASSED end to end in 148 s ("Settings › Logout must sign out" passed first time, with no "no 'Logout' row"
retry and no "under the tab bar" warning). TC_EGF_02 passed again (193 s). Only TC_PM_04 (product, MKP-LABEL-01) and
TC_BX_04 (data skip) remain. The ghost teardown printed no "still on 'Switch-1'" warning, so both deletes synced
inside 90 s. Backend check afterwards: Switch-1 holds exactly its 2 baseline forms (16b48cad Cleaning + 99d359ee) and
the session has 0 Bolted Connection Resistance instances. The Summary job's gate is still red on TC_PM_04 alone; it
goes green when MKP-LABEL-01 is filed and TC_PM_04 is quarantined.

## 6. Product defects

All three reproduce on v1.73. They are NOT filed yet: creating Jira issues needs the user's go-ahead (the
auto-mode classifier blocked the first create). Drafts are ready: Bug, To Do, sprint Z-26-10-S2, iOS v1.74, Kush.

| Id | Summary | Test | Severity |
|---|---|---|---|
| SW-WIZ-01 | Site Walk wizard: after "Assign a site later", going back from Services shows "This customer has no sites yet" for a customer with sites (step 2 calls the step-1 deferAssignment(), which clears the customer) | TC_SWW_08 | Low |
| MKP-LABEL-01 | Photo markup: the text label field keeps showing characters past 200 (210 visible) while the counter reads 200/200; the extra text is dropped silently on save | TC_PM_04 | Low |
| EGF-ADD-01 | WO room: an asset with exactly ONE form has no way to add another — 'Forms (1)' opens that form directly and the 'Select a Form' sheet holding 'Add Form' never shows (no add path in the asset menu, row, '+' button, Forms tab or Asset Details) | found by the ghost-value tests | Medium |

Observation (not a bug report): POST /eg-form-instance/by-session/<unknown id>/export answers 200 + HTML instead of a
JSON 4xx. No job starts.
