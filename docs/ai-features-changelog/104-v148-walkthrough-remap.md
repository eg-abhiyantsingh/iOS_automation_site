# 104 — v1.48 Work-Order → Photo-Walkthrough flow re-mapped live (38+15 test unblock)

**Date:** 2026-07-03
**Method:** drove the full flow step-by-step with a raw Appium session against v1.48,
dumping each screen's real identifiers, then fixed the framework against evidence —
one local test run per fix (quality-first, per QA direction).

## The mapped v1.48 flow (all markers captured live)
Dashboard → WO card (**disabled up to ~110s while session sync runs — must wait for
enabled=true**) → "Work Orders" screen (`Available Work Orders`, per-row `Start`
buttons, `BackButton`) → **Start goes STRAIGHT to Session Details** (`WORK ORDER
DETAILS` / `IR Photo Type` / `Quick QR Action`, tabs Details/Assets/Tasks/Issues/IR/
Files) → session Assets tab (Locations tree — now 117KB post-cleanup, queryable) →
room row = plain Button named "Room …" (tap opens directly) → "Assets in Room"
(`Add` button) → Add fans out ICON-ONLY modes: `plus.square`, `link.badge.plus`,
**`camera.viewfinder` = Photo Walkthrough**, `number.square` → Photo Walkthrough
sheet (`Photo Walkthrough` nav, `Cancel`, Profile/Nameplate/Panel Schedule/Arc Flash
Sticker, `Gallery`/`Camera`).

## Fixes (each verified by a live rerun advancing further)
1. **SiteSelectionPage.clickWorkOrderCard**: waits (SITE_DASHBOARD_WAIT_SEC budget)
   for the card's `enabled=true` — tapping the disabled card silently no-ops, which
   stranded every SiteVisit test on the Dashboard.
2. **SiteVisit phase1/2/3**: `navigateToWorkOrdersScreen` now uses
   `clickWorkOrderCard` (was legacy `clickNoActiveJobCard` + blind coordinate tap).
3. **WorkOrderPage.isWorkOrdersScreenDisplayed** strategy 4 tightened: `CONTAINS
   'Work Order'` matched the DASHBOARD's own "No Active Work Order" card text →
   false positive that convinced the chain it had navigated when it hadn't.
4. **SiteVisit phase2+3 `navigateToAssetsInRoom`**: now calls
   `ensureOnSessionDetailsScreen()` first — without it, `tapSessionTab("Assets")`
   matched the Dashboard's bottom-nav Assets tab (site-level list, "Buildings
   found: 0").
5. **WorkOrderPage.tapFirstRoomWithAssets** strategy 0: tap a visible `Room …`
   Button directly (no expansion, no collapse risk).
6. **WorkOrderPage.isCreatePhotoWalkthroughOptionDisplayed / tapCreatePhotoWalkthroughOption**:
   v1.48 fan menu is icon-only — added `camera.viewfinder` as the primary strategy.

## Result
TC_JOB_234 progressed from "stranded on Dashboard, 6m timeout" to executing the FULL
walkthrough: photo added via Gallery (thumbnail verified), Done-with-asset, Classify
Asset reached. **One sub-step remains**: `selectClassifyAssetType("MCC")` cannot
select in the v1.48 classify picker (searchable-picker family — same fix pattern as
AssetPage.selectClassViaSearch). Tracked as the next increment.

## Environment note (evidence captured)
QA backend had a ~10-min outage during this session (login 504/timeout ×3, /api/docs
timeout; recovered 20:47). Run 28644884870 was contaminated by it (bulk-cancelled
jobs, 252m Assets P1) and was cancelled; its results must not be used for fix
validation.
