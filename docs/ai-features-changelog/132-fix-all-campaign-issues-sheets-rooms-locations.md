# 132 — FIX-ALL campaign: Issues bottom-sheet remap, session-room provisioning, New Location composite, Edge Properties

**Date:** 2026-07-16
**Prompt:** "check all the fail test case and fix it."

## Scoreboard (all validated locally, one test at a time)

| Cluster | Before | After |
|---|---|---|
| TC_CONN_036/037 create-connection E2E | FAIL | **GREEN** (prior commit) |
| TC_CONN_041 create-without-target | FAIL | **correctly RED — real app bug CONN-VAL-01** (BUGS.md) |
| TC_CONN_097 core attributes | FAIL | **GREEN 44s** |
| TC_ISS_038/054/055/059/061/063/082 | 7 FAIL | **7 GREEN** |
| TC_OFF_011 offline create building | FAIL | **GREEN 45s** |
| TC_AF_024/025 session-room | SKIP→6m-timeout | fast honest skip → self-provisioning (link flow live-validating) |

## Root causes found (probe-first, dump-verified)

1. **v1.50 renamed CORE ATTRIBUTES → 'Edge Properties'** on the New Connection
   form (0/N counter + 'Required fields only' + type-specific fields).
   `selectConnectionType` also now self-opens the type dropdown.
2. **CONN-VAL-01 (candidate app bug):** Create enabled with NO Target Node;
   submit succeeds → the "Not Assigned → Not Assigned" debris rows.
3. **Issues v1.50 details contract:** rows are `label + control ~38px below`;
   pressing the control opens a **bottom sheet** (title + Cancel + full-width
   option Buttons). NEW: 'Pending' status. Root-caused TC_ISS_054 "1/4": the
   list screen's HIDDEN filter-tab twins (`visible=false`) come first in DOM
   order — `findElement().isDisplayed()` returned false before the real sheet
   button was ever considered. All option checks now put `visible == 1` in the
   predicate. `ensureNewIssueFormOpen` on the details screen pressed phantom
   Add buttons and stranded tests on the **Status History** sheet — details
   openers never call it now (`isOnIssueDetails()` branch).
4. **Session surface (v1.50):** tab strip Details/Assets/Tasks/Issues/IR/Files
   renders at the BOTTOM (y≈868); room rows on the Assets tab are Buttons
   named `'<Bldg> › <Floor>, <room>'`. ensureSessionDetailsOpen now hops
   site-home → WO list first (the '109, Work Orders' TILE false-positived the
   WO-screen probe). WO row signature widened to the `, <Priority>` suffix
   ('test job, Medium').
5. **Fixture gap:** every room in the active-WO fixture is EMPTY ('No assets in
   this room yet — Create or Link Existing'). tapFirstRoomWithAssets now
   SELF-PROVISIONS once via the ZP-3003 link flow — the '+' FAB menu is
   ICON-ONLY (`link.badge.plus` = Link Existing Asset) — then proceeds; hunt is
   budget-capped (150s) so it can never eat the 360s test cap again.
6. **Locations v1.50:** '+' opens a 'New Location' COMPOSITE form; Create stays
   DISABLED until Building+Floor+Room names are all filled (screenshot-proof),
   and `mobile: hideKeyboard` silently fails there. createBuilding fills all
   three, taps neutral space if the keyboard survives, presses Create, and
   verifies the form closed.

## Files
ConnectionsPage, IssuePage, WorkOrderPage, SiteSelectionPage, DebugProbe_Test
(6 new probes), BUGS.md (CONN-VAL-01).
