# 130 — Full local suite sweep: wave-by-wave triage and fixes

**Date:** 2026-07-14/15 (overnight)
**Prompt:** "test everything full flow parallel suite in local and fix if you see any issue."
Local = one simulator, so the parallel suites run sequentially, wave by wave,
with triage and fixes between waves.

## Wave ledger

| Wave | Suite | Result |
|---|---|---|
| 1 | smoke-demo (23, cross-module) | **23/23 GREEN** (10m13s) |
| 2 | offline (76) | 23P/30F/23S → 4 clusters (below) |
| 3 | auth (48) | 23F — ALL one environmental root (below); recovery 3/3 green |
| 4 | offline reshapes | 009 ✓ 013 ✓ 023 ✓ 030 ✓ (020 → Locations track) |
| 5 | connections (97) | 84P/11F/2S → 3 clusters; 087 ✓ 088 ✓ fixed+green; 036 fix validating |
| 6+ | issues-p1 → … | in progress |

## Wave 2 offline — clusters and fixes

- **B (v1.50 dashboard redesign)**: the 'My Tasks' tile NO LONGER EXISTS
  (AppStrings.Site.myTasks has zero usages in app source). TC_OFF_009 reshaped
  to the true v1.50 offline contract (Work Orders + Schedule tiles enabled
  offline; source: SiteTabView `isDisabled: mode == .offline` on Refresh,
  "Schedule … offline shows cached blocks"). TC_OFF_013 re-routed to the asset
  Tasks path — with three v1.50 task-form fixes (nav 'New Task' accepted by
  the detector, lowercase 'Enter task title' placeholder, 'Create Task' nav
  button + W3C press). GREEN (1m19s).
- **D (goOnlineViaPopup 6-min hangs)**: bounded popup-retry + visible-controls
  diagnostics instead of the unconditional goOnline() grind. TC_OFF_023 GREEN
  (2m18s, was a 360s ThreadTimeout).
- **A (site-selection cascade, 16 tests)**: one primary root — see wave 3.
- **C (queue-didn't-grow: 011/014/017/035 + 020)**: creation flows blocked on
  the v1.50 Locations create-building form ('Building Name' field gone) and
  the known v1.48 Issues DOM regression — both are their own remap tracks.

## Wave 3 auth — the stranded-offline poisoning (systemic find)

A failed offline test skipped its in-test cleanup; noReset persisted the
APP-LEVEL offline flag; every later login died at 'Failed to fetch company
configuration' → 23 auth "failures" + wave-2's 16-test cascade, all one root.
Fixes so THIS CAN NEVER RECUR:
1. OfflineTest + OfflineSyncMultiSite_Test @AfterClass(alwaysRun) now RESTORE
   ONLINE (bounded) before releasing the suite.
2. WelcomePage.submitCompanyCode now names the state precisely (VerificationError
   "stuck in APP-LEVEL OFFLINE mode…") instead of the misleading
   "Failed to click element after 3 attempts".
3. Session env: app reinstalled clean to clear the stranded flag.

## Wave 5 connections — clusters and fixes

- **Delete-dialog race (087/088)**: autoAcceptAlerts auto-pressed the DELETE
  confirmation between the icon tap and our button tap (auto-accept on a
  delete dialog DELETES — worse than a red test). New
  ConnectionsPage.deleteDialogDance(button) runs the whole dance under manual
  alerts (pause/restore defaultAlertAction). Both GREEN (54s/50s).
- **Source/Target picker family (024/030/036/037/041/059/062)**: two mechanics —
  the ~50pt QR-scan button beat the ~600pt row in the smallest-dy tie
  (QR scanner opened → 6-min grinds), and rows are HStack+Spacer buttons
  (Spacer hit-test dead zone). Strategy 0 on BOTH pickers now: width ≥200
  filter + widest-wins + LEFT-zone press + stale-safe loops. Plus
  scanDropdownAssetNames bounded (visible-only, 30-cap, stale-continue) —
  its unbounded variant wedged and KILLED WDA (036, 6m35s dead session).
- TC_CONN_009 (search-filter count) + TC_CONN_097 (Cable type selectable):
  singles, pending dedicated look.

## Carry-over tracks (pre-existing, confirmed by the sweep)

Locations v1.50 create-building remap (TC_OFF_011/017/020/035 + location
suites); Issues create-form remap (v1.48 DOM regression — TC_OFF_014);
WDA post-class-change snapshot wedge (FUSE_EAD_19 signature).
