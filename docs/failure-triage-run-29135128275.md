# Per-test failure triage — run 29135128275 (full suite, 2026-07-11)

Verdicts: ALL 230/230 analyzed (207 via agent fan-out + 23 inline completion pass below). Original tally (agent portion): {'AUTOMATION_BUG': 191, 'INVALID_TEST': 2, 'APP_BUG': 1, 'ENV_INFRA': 11, 'DATA_FIXTURE': 2}

- INVALID_TEST = expectation wrong (fix the test) | AUTOMATION_BUG = our locator/timing/detection | APP_BUG = product | ENV_INFRA = breaker/sim | DATA_FIXTURE = missing data

## Domain rule (user-confirmed 2026-07-13)
Core Attributes "required" fields are required ONLY to compute the Arc Flash value — never mandatory for save. Tests must not assert save-blocking or hard toggle state.


## SiteVisit_phase2#1

### TC_JOB_100_verifyIRPhotoFilenameField  —  **AUTOMATION_BUG**
- Why: Exactly-360s timeout = hung mid-flow, not a slow assert. Its navigation path (navigateToIRPhotosSection → ensureOnSessionDetailsScreen → tapSessionTab('Assets') → tapFirstRoomWithAssets @WorkOrderPage:2298) runs unscoped whole-tree findElements (e.g. every StaticText with label CONTAINS 'asset') over the giant session Locations DOM — the documented WDA-wedge zone. TC_JOB_103 ran the identical navigation minutes later and passed, so this is the intermittent wedge from our query pattern, not a broken app or sim.
- Action: Bound/scope the Locations-tree queries in tapFirstRoomWithAssets and the Assets-tab detection (withImplicitWait(0), scoped class-chain under the expanded building, mobile:scroll predicate) and force a WDA rebuild after a per-test timeout so the wedge doesn't leak into the next test.

### TC_JOB_101_verifyVisualPhotoFilenameField  —  **AUTOMATION_BUG**
- Why: Same exact-360s hang, same unscoped giant-tree navigation as TC_JOB_100; it either inherited the WDA session TC_JOB_100 wedged (each command then hangs ~90s) or re-wedged on its own tapFirstRoomWithAssets scan. It did NOT fail fast on a dead session (that would be breaker-counted ENV_INFRA), it burned the full timeout executing our queries; TC_JOB_103's pass right after shows the flow works once the session is rebuilt.
- Action: Same fix as TC_JOB_100: scoped/bounded Locations queries + mandatory WDA rebuild after any test-level timeout before the next @Test starts.

### TC_JOB_102_verifyAddIRPhotoPairButton  —  **AUTOMATION_BUG**
- Why: Third consecutive exact-360s hang in the same unscoped Locations-tree navigation (tapFirstRoomWithAssets/getLocationsFloorEntries whole-DOM StaticText scans — the source itself warns about '300+ floors in building'). Wedged-session hang from our query pattern, confirmed recoverable since TC_JOB_103 (identical path) passed immediately after.
- Action: Same remap: scope tree queries to the expanded building node, cap each probe with withImplicitWait(0)/Waits, rebuild WDA on timeout.

### TC_JOB_104_verifyAddedIRPhotoPairInList  —  **AUTOMATION_BUG**
- Why: Test reached the IR section (gate passed) and tapAddIRPhotoPairButton returned true, but that helper (WorkOrderPage:10428) clicks any Button OR StaticText with label CONTAINS 'Add IR Photo' and returns true with no post-tap verification; both filename fields read null even BEFORE the tap (v1.49 shows placeholders — TC_JOB_103 was already reshaped for 'filenames not auto-generated'), and the detectors (StaticText 'New IR Photos' header @10521, 'IR: N' rows @10544) found nothing. The v1.49 IR-photos DOM no longer matches this locator contract.
- Action: Live-inspect the v1.49 New Asset IR section DOM (rows are likely XCUIElementTypeOther per the v1.36+ SwiftUI pattern), remap section/row/value locators, and make tapAddIRPhotoPairButton confirm a state change (pair count or section appears) before returning true; enter filenames manually first if v1.49 requires values before Add.

### TC_JOB_105_verifyMultipleIRPhotoPairs  —  **AUTOMATION_BUG**
- Why: Identical mechanism to TC_JOB_104: two 'successful' Add taps (unverified label-click, possibly a StaticText click that no-ops on iOS 18.5) yet getNewIRPhotoPairCount()==0 and every filename read [null,null] before and after — the 'IR: N' StaticText row contract the counter greps for (WorkOrderPage:10549) does not exist in the v1.49 DOM.
- Action: Same IR-photos locator remap as TC_JOB_104; after remap, assert on verified pair-count deltas, not on the tap boolean.

### TC_JOB_106_verifyIRPhotoPairEdit  —  **AUTOMATION_BUG**
- Why: 'Edit tapped: false' comes from tapPairActionIcon (WorkOrderPage:10776) returning false because it found 0 'IR:'-prefixed StaticText entries to anchor on — the pair list was never detected, same stale v1.49 row contract as TC_JOB_104/105, not an edit-icon defect. IR/Visual field reads of null match the placeholder-field change.
- Action: Blocked on the same IR-photos DOM remap; once rows are locatable, re-anchor the edit-icon Y-proximity search (or use scoped row buttons) and re-run.

### TC_JOB_107_verifyIRPhotoPairDelete  —  **AUTOMATION_BUG**
- Why: 'Delete tapped: false, count before: 0, count after: 0' — tapPairActionIcon found zero 'IR:' rows (pairIndex >= filteredEntries.size() → false at WorkOrderPage:10798) and getNewIRPhotoPairCount was already 0 before the delete. Same root cause as 104-106: the added pair (if added at all — the Add tap is unverified) is invisible to the stale StaticText locator contract.
- Action: Same remap; also make the test hard-gate on countBefore > 0 (a verified add) instead of proceeding to delete against an empty list.

### TC_JOB_110_verifyCreateAssetWithIRPhotos  —  **AUTOMATION_BUG**
- Why: Failure is assertTrue(createTapped) with 'create tapped: false' yet 'asset created: true'. tapSessionCreateAssetButton (WorkOrderPage:11165) found no 'Create Asset'/'Create' button — the test types the asset name via setSessionNewAssetName and never dismisses the keyboard before scrolling/tapping (the repo's documented keyboard-covers-button mechanism), and isAssetCreatedSuccessfully's Strategy 3 ('no longer on New Asset form' @11244) is a weak save-evidence probe, so the test asserts on the action boolean while the outcome signal is unreliable.
- Action: dismissKeyboard() after name entry before scrolling to Create; live-verify the v1.49 Create button label and remap; flip the assert to a verified outcome (new asset visible in Assets-in-Room) instead of the tap boolean, and harden isAssetCreatedSuccessfully's Strategy 3.

### TC_JOB_112_verifyFLIRINDFilenameIncrement  —  **AUTOMATION_BUG**
- Why: Exact-360s hang inside shared helper createJobWithPhotoTypeAndNavigateToIR, entered with a live session (110/111 completed normally just before). The helper stacks known hang generators: tapPhotoTypeDropdown Strategy 3 scans ALL Button/Cell/Other elements unscoped (WorkOrderPage:14414), then getLocationsBuildingCount/getLocationsFloorEntries run whole-DOM StaticText scans over the 300+ floor tree. All three photo-type tests hung identically → deterministic hang in this helper, matching the exactly-6m00s hung-mid-flow signature.
- Action: Instrument the helper with per-step wall-guards to pinpoint the hang (picker vs tree), then scope the photo-type dropdown query to the New Job form and bound the location-tree scans; rebuild WDA on timeout.

### TC_JOB_113_verifyFLUKEFilenameIncrement  —  **AUTOMATION_BUG**
- Why: Same helper, same exact-360s hang as TC_JOB_112 (three-for-three across FLIR-IND/FLUKE/FOTRIC = deterministic hang in createJobWithPhotoTypeAndNavigateToIR's unscoped picker + giant-tree queries, possibly compounded by inheriting the wedged session from 112 — it hung executing, it did not fast-fail dead).
- Action: Same fix as TC_JOB_112; do not re-triage separately until the shared helper is instrumented and bounded.

### TC_JOB_114_verifyFOTRICFilenameIncrement  —  **AUTOMATION_BUG**
- Why: Third identical exact-360s hang in createJobWithPhotoTypeAndNavigateToIR — same unscoped Photo Type dropdown scan and whole-tree getLocationsBuildingCount/FloorEntries/RoomEntries queries over the documented giant Locations DOM.
- Action: Same fix as TC_JOB_112/113 (shared-helper instrumentation + scoped/bounded queries + WDA rebuild on timeout).

### TC_JOB_115_verifyIRPhotosNotShownWithoutActiveJob  —  **INVALID_TEST**
- Why: The test cannot verify its stated contract: after deactivating the job it RE-ACTIVATES one to reach the New Asset form (SiteVisit_phase2:1532-1535), then ends in assertTrue(true, ...) on BOTH exit paths (lines 1602 and 1617) — a pass-anyway tautology that never asserts the IR section is hidden without an active job. The observed 360s timeout is the same giant-tree navigation hang as the other timeouts, but even with that fixed the test asserts nothing.
- Action: Rewrite with a real negative assertion — create an asset via a non-session flow (or a job with no thermal photo type) and hard-assert isInfraredPhotosSectionDisplayed()==false — using the bounded navigation queries from the wedge fix.

## SiteVisit_phase2#2

### TC_JOB_117_verifyBuildingPlusButtonInLocations  —  **AUTOMATION_BUG**
- Why: Test calls tapSessionTab("Assets") (SiteVisit_phase2.java:1696) with no ensureOnSessionDetailsScreen(); prior test's cleanup ends on the Work Orders list, so the unscoped predicate 'label == Assets' (WorkOrderPage.java:1995-2033) hits the dashboard bottom-nav Assets tab. The repo's own comment at SiteVisit_phase2.java:220-223 documents this exact mechanism producing 'Buildings found: 0'. 'All have + button: true' is vacuous (loop over 0 buildings). Rerun SKIP is breaker cascade, not signal.
- Action: Add ensureOnSessionDetailsScreen() before tapSessionTab("Assets") in TC_JOB_117 (mirror navigateToAssetsInRoom), and scope the tab locator to the session tab bar container instead of whole-screen label match.

### TC_JOB_118_verifyFloorExpansionShowsRooms  —  **AUTOMATION_BUG**
- Why: Same missing-guard defect: bare tapSessionTab("Assets") at SiteVisit_phase2.java:1742 lands on the site-level Assets list (documented v1.48 mechanism, comment at lines 220-223), so getLocationsBuildingCount()==0 and the test fails 'Need at least one building'. Not DATA_FIXTURE — the session tree has buildings; the test was reading the wrong screen.
- Action: Add ensureOnSessionDetailsScreen() before the Assets-tab tap in TC_JOB_118, same fix as TC_JOB_117; consider a shared guarded helper for all Locations-tab tests.

### TC_JOB_120_verifySessionBottomTabs  —  **AUTOMATION_BUG**
- Why: TC_JOB_120 performs no navigation at all before probing tabs (SiteVisit_phase2.java:1944), relying on residual state, and getSessionBottomTabLabels/isTabDisplayed (WorkOrderPage.java:1941-1989) match any Button/StaticText on the whole screen. 'Found: [Assets, Tasks, Issues], Navigable: [Assets]' is the signature of a non-session screen (global Assets tab is a button; Tasks/Issues appear as static texts) — on real Session Details all 5 tabs including Details/Files would match.
- Action: Call ensureOnSessionDetailsScreen() at test start and scope the tab predicates to the bottom TabBar/toolbar container; re-run to confirm all 5 tabs are found on the actual Session Details screen before suspecting a v1.49 tab change.

### TC_JOB_122_verifyAssetCompletionPercentage  —  **AUTOMATION_BUG**
- Why: Failure message quotes the 'asset' name as the empty-state placeholder 'Tap the + button to add assets to this room'. getAssetsInRoomListCount (WorkOrderPage.java:3775-3785) excludes only 'No Assets'/'No Asset Types' cells, and getAssetEntries skips only labels containing "Tap '" (line 3913, with quote) — the v1.49 placeholder passes both filters, so the empty room counted as 1 asset, ensureAssetsInRoomWithAsset skipped its create-asset fallback, and the placeholder row was read as an asset with completion null.
- Action: Add the placeholder to the exclusion filters (e.g. label BEGINSWITH 'Tap the' OR label CONTAINS 'add assets to this room') in getAssetsInRoomListCount, getAssetEntries, and longPressOnAssetInRoom so the count reads 0 and the asset-creation fallback fires; also fix the stale "Tap '" filter.

### TC_JOB_123_verifyLongPressContextMenu  —  **AUTOMATION_BUG**
- Why: longPressOnAssetInRoom (WorkOrderPage.java:3993-4011) uses the same Y>200/H>40 cell filter that excludes only 'No Assets'/'No Asset Types', so it long-pressed the empty-state placeholder cell ('Tap the + button to add assets to this room' — proven present by TC_JOB_122's message in the same session). No asset was pressed, so no context menu could appear ('Menu displayed: false, Found 0/6'). Root cause is the placeholder-counted-as-asset bug, not the app's menu.
- Action: Fix the placeholder exclusion in getAssetsInRoomListCount/getAssetEntries/longPressOnAssetInRoom so ensureAssetsInRoomWithAsset actually creates an asset in the empty room, then re-run the context-menu chain.

### TC_JOB_124_verifyCollectDataOption  —  **AUTOMATION_BUG**
- Why: Same root cause as TC_JOB_123: hasAssets was a false positive from the placeholder cell, the create-asset fallback in ensureAssetsInRoomWithAsset (SiteVisit_phase2.java:1872-1875) was skipped because count>0, and the long-press target was the 'Tap the + button...' empty-state row — hence 'Menu displayed: false, option found: false'.
- Action: Same fix: exclude the 'Tap the + button to add assets to this room' placeholder from asset-cell filters so the fallback asset creation runs; no app-side action needed.

### TC_JOB_125_verifyAddTaskOption  —  **AUTOMATION_BUG**
- Why: Identical mechanism and identical failure shape ('Menu displayed: false, option found: false') as TC_JOB_123/124 in the same session: the long-press hit the empty-state placeholder cell that the asset-cell filters (WorkOrderPage.java:4000-4009) fail to exclude, so no context menu existed to contain 'Add Task'.
- Action: Apply the shared placeholder-exclusion fix; retest after ensureAssetsInRoomWithAsset provably creates an asset (assert the created asset's name appears in the list, not just count>0).

### TC_JOB_126_verifyAddIRPhotosOption  —  **AUTOMATION_BUG**
- Why: Same cluster: menu never displayed because the long-pressed 'asset' was the empty-room placeholder cell mis-counted by getAssetsInRoomListCount (WorkOrderPage.java:3775-3785); the 'Add IR Photos' option cannot appear on a non-asset row. Deterministic across the whole 123-130 chain in one session, pinned by TC_JOB_122's placeholder-name message.
- Action: Apply the shared placeholder-exclusion fix in the three WorkOrderPage helpers; then re-run TC_JOB_122-130 as a block.

### TC_JOB_127_verifyAddIssueOption  —  **AUTOMATION_BUG**
- Why: Same cluster as TC_JOB_123-126: 'Menu displayed: false, option found: false' because the long-press landed on the empty-state placeholder cell that passes the asset-cell filters; ensureAssetsInRoomWithAsset never created a real asset due to the false-positive count.
- Action: Apply the shared placeholder-exclusion fix; no Issues-DOM remap needed here (failure precedes any Issue screen — the v1.48 Issues regression is not the mechanism).

### TC_JOB_128_verifyEditConnectionsOption  —  **AUTOMATION_BUG**
- Why: Same cluster: context menu absent because the pressed cell was the 'Tap the + button to add assets to this room' placeholder, not an asset (filters at WorkOrderPage.java:4003-4007 only exclude 'No Assets'/'No Asset Types'). Option detection code itself (isContextMenuOptionDisplayed) never had a menu to inspect.
- Action: Apply the shared placeholder-exclusion fix and rerun; if the menu then appears but options mismatch, only then remap option labels.

### TC_JOB_129_verifyRemoveFromSessionOption  —  **AUTOMATION_BUG**
- Why: Same cluster: 'Menu displayed: false, option found: false' — long-press target was the empty-state placeholder cell because getAssetsInRoomListCount counted it as an asset and the create-asset fallback in ensureAssetsInRoomWithAsset was skipped (SiteVisit_phase2.java:1872-1875).
- Action: Apply the shared placeholder-exclusion fix; keep the no-tap-on-Remove safety behavior as is.

### TC_JOB_130_verifyCollectDataOpensDataCollection  —  **AUTOMATION_BUG**
- Why: Failed at the same gate as the rest of the cluster: 'Context menu did not appear after long-press' (SiteVisit_phase2.java:2706-2710) because the long-press was on the empty-room placeholder cell, not an asset; Collect Data navigation was never reachable. Rerun SKIP is the dead-session breaker cascade, not a per-test signal.
- Action: Apply the shared placeholder-exclusion fix so a real asset exists and is long-pressed; then validate the Collect Data navigation assert (currently asserts only 'tapped', consider also asserting dataScreenDisplayed once the chain is unblocked).

## SiteVisit_phase2#3

### TC_JOB_131_verifyAddIRPhotosOpensCapture  —  **AUTOMATION_BUG**
- Why: Session was alive (ensureAssetsInRoomWithAsset() gate passed, which needs working queries), then longPressOnAssetInRoom() Strategy 1 'mobile: touchAndHold' returns true even when the press silently no-ops — the W3C fallback at WorkOrderPage.java:4056 only runs if touchAndHold THROWS, and the target cell comes from an unscoped whole-DOM 'XCUIElementTypeCell' query (y>200, h>40) prone to SwiftUI bleed-through. No retry exists for 'press reported success but menu absent'.
- Action: In longPressOnAssetInRoom, verify isAssetContextMenuDisplayed() after the touchAndHold and fall through to the W3C long-press on no-menu (not only on exception); scope the cell query to the visible Assets-in-Room table to defeat bleed-through.

### TC_JOB_132_verifyRemoveFromSessionUnlinks  —  **AUTOMATION_BUG**
- Why: Identical gate to TC_JOB_131: nav and asset detection succeeded (session alive), then 'Context menu did not appear' — longPressOnAssetInRoom treats a silent-no-op touchAndHold as success (fallback only on exception, WorkOrderPage.java:4042-4079) and picks its target from an unscoped whole-DOM cell query. Rerun SKIP is breaker cascade, not signal.
- Action: Same fix as TC_JOB_131: post-press menu verification with W3C-press retry and a scoped cell locator; then re-run both context-menu tests together.

### TC_JOB_136_verifyAddAssetTypeOpensSelection  —  **AUTOMATION_BUG**
- Why: Exactly-360000ms = documented HUNG-mid-flow signature. The test's path runs the giant-DOM wedge query pattern: tapFirstRoomWithAssets() does whole-tree findElements("label CONTAINS 'asset'") on the session Locations tree (WorkOrderPage.java:2324, documented ~90s/query), and getAssetTypeOptions() loops 22 types x findElements WITHOUT withImplicitWait(0) under the 5s default implicit wait (WorkOrderPage.java:6116-6127) — up to ~110s of miss-burn alone.
- Action: Wrap getAssetTypeOptions/type loops in withImplicitWait(0) with per-step budgets and replace whole-tree Locations queries with scoped class-chain queries (per giant-DOM memory); this test also wedged WDA for the rest of the class.

### TC_JOB_137_verifyAssetTypeList  —  **AUTOMATION_BUG**
- Why: Same exactly-360000ms hung signature and same path as TC_JOB_136, plus its own aggravator: the post-scroll re-check loops findElements once per missing type under the 5s implicit wait (SiteVisit_phase2.java:3271-3290), so a not-open sheet (0 types) burns 22x5s in getAssetTypeOptions and again per missing type — enough to eat the 6-minute budget against a wedged WDA.
- Action: Zero the implicit wait around the 22-type probe loops and add a hard per-step budget; fix shared tree-navigation queries as in TC_JOB_136.

### TC_JOB_139_verifyATSSubtypeOptions  —  **AUTOMATION_BUG**
- Why: Nav gates passed (qcScreenReached true — session alive, on Quick Count), then 'sheet did not open': waitForSelectAssetTypeSheet gives only a 2s window (WorkOrderPage.java:6083-6084, below the repo's own 3s wait-cap minimum) and on miss re-taps via tapAddAssetTypeButton whose Strategy 3 is a blind bottom-center coordinate tap at height-60 (WorkOrderPage.java:6001) — on a slow-opening sheet the retry lands in the sheet's bottom/Cancel zone and dismisses it (tab-bar-zone coordinate-press family, pre-f98f5ce).
- Action: Raise the sheet wait to >=3-5s, check isSelectAssetTypeSheetDisplayed() BEFORE the retry tap, and remove the blind bottom-center fallback from tapAddAssetTypeButton.

### TC_JOB_140_verifySkipNoSubtypeOption  —  **AUTOMATION_BUG**
- Why: Fails at the identical gate as TC_JOB_139 in the same run ('Asset type selection sheet did not open' after reaching Quick Count) — the 2s sheet-wait + blind re-tap handshake in waitForSelectAssetTypeSheet/tapAddAssetTypeButton, not a per-test defect. Rerun SKIP is dead-session breaker cascade, no signal.
- Action: Same fix as TC_JOB_139; both should be re-run together after the sheet-open handshake is hardened.

### TC_JOB_141_verifyAssetTypeCardAfterSelection  —  **AUTOMATION_BUG**
- Why: 'Could not add ATS type to Quick Count' is the catch-all false from navigateToQuickCountAndAddType (SiteVisit_phase2.java:3528) covering three gates; sibling tests 139/140 in the same run pinpoint the failing gate as the Add-Asset-Type sheet never opening (2s wait + blind bottom-center retry tap). All six 'Could not add ATS' tests share this one root cause.
- Action: Fix the sheet-open handshake (TC_JOB_139 action) and make navigateToQuickCountAndAddType log/report WHICH gate failed (nav vs sheet vs card) so future failures disambiguate.

### TC_JOB_144_verifyDeleteAssetType  —  **AUTOMATION_BUG**
- Why: Same catch-all 'Could not add ATS to Quick Count' from navigateToQuickCountAndAddType; the run's 139/140 failures show the shared sheet-open handshake (waitForSelectAssetTypeSheet 2s window + tapAddAssetTypeButton blind bottom-zone retry) is the failing step — the delete assertion itself was never reached.
- Action: No per-test change needed; re-run after the shared sheet-open fix lands.

### TC_JOB_145_verifyPhotosetsSection  —  **AUTOMATION_BUG**
- Why: Failed in the shared precondition helper (navigateToQuickCountAndAddType returned false) before any Photosets assertion ran; root cause is the same Add-Asset-Type sheet handshake proven failing by TC_JOB_139/140 in this run.
- Action: Re-run after the sheet-open handshake fix; the Photosets assertions themselves (photosetsLabel || addPhotosetBtn) are untested this run.

### TC_JOB_148_verifyGalleryButton  —  **AUTOMATION_BUG**
- Why: Never reached the Gallery-button assertion — failed at the shared 'Could not add ATS to Quick Count' precondition, same sheet-open handshake defect as TC_JOB_139/140 (2s wait below the 3s minimum + blind bottom-center retry tap that can cancel a slow sheet).
- Action: Re-run after the shared fix; no Gallery-specific change indicated.

### TC_JOB_149_verifyCameraButton  —  **AUTOMATION_BUG**
- Why: Failed at the shared ATS-add precondition, so the camera path (and the CAM-CRASH-01 guardCameraTapCrash at SiteVisit_phase2.java:4743) was never reached — this is NOT the camera app bug, it is the same sheet-open handshake failure as the rest of the 141-150 block.
- Action: Re-run after the shared fix; keep the existing camera-tap guard, which correctly quarantines the real CAM-CRASH-01 app bug once the test gets that far.

### TC_JOB_150_verifyPhotosetEntryDisplaysAfterAdding  —  **AUTOMATION_BUG**
- Why: Same shared-precondition failure ('Could not add ATS to Quick Count' from navigateToQuickCountAndAddType) before any photoset assertion; root cause is the Add-Asset-Type sheet handshake shown failing by TC_JOB_139/140. Rerun SKIP = breaker cascade, no independent signal.
- Action: Re-run after the sheet-open fix; also note its final assert only checks addPhotosScreen opened (photoset-entry checks are informational), so consider hardening once the block is green.

## Issue_Phase1_Test#1

### TC_ISS_004_verifyOpenTabSelectedByDefault  —  **AUTOMATION_BUG**
- Why: isOpenTabSelected() (IssuePage:698) only returns true if the chip button's 'selected' attribute is "true" or value=="1"; v1.49 SwiftUI filter chips do not expose either, so the probe reads false even when Open is visually selected. Sibling tests on the same screen (TC_ISS_003/005/007 tapping and counting tabs) passed, so navigation and the buttons themselves are fine — only the selected-state attribute contract is stale.
- Action: Remap selected-state detection for v1.49 chips: dump the chip's actual attributes (traits/value/label) once, then detect selection via whichever signal the new DOM exposes (e.g. label diff, 'isSelected' trait via attribute('selected') on the inner element, or visual check) with multi-strategy fallbacks.

### TC_ISS_006_verifyResolvedAndClosedTabFilters  —  **AUTOMATION_BUG**
- Why: isResolvedTabSelected() (IssuePage:733) uses the identical selected=="true"/value=="1" attribute probe as TC_ISS_004; it fails even right after tapResolvedTab() located and clicked the Resolved button, proving the attribute isn't exposed rather than the tap failing. Same stale state-detection contract as TC_ISS_004.
- Action: Same fix as TC_ISS_004: remap tab-selected detection to the v1.49 chip DOM; additionally verify the tap applied via a state change (e.g. list content/count changes) instead of the selected attribute alone.

### TC_ISS_010_verifyHighPriorityBadge  —  **AUTOMATION_BUG**
- Why: The seed path createIssueWithPriority() calls selectPriority(), whose picker locators (button/StaticText containing 'priority') are proven dead on the v1.49 form by TC_ISS_036/037, and its failure is swallowed — so the seed issue is created with priority None and the helper still returns true. The badge probe isPriorityBadgeDisplayed (IssuePage:951, exact label=='High' StaticText/Button) then correctly finds no High badge.
- Action: Fix the Priority picker locators (see TC_ISS_036/037 remap), make createIssueWithPriority verify the priority chip reads back the chosen value before returning true, and widen the badge probe to match aggregated cell labels (label CONTAINS 'High' within issue cells).

### TC_ISS_015_verifySearchFiltersIssues  —  **AUTOMATION_BUG**
- Why: The search token was taken from a genuinely visible issue (getFirstIssueTitle returned 'HighPriSeed_...'), but searchIssues() (IssuePage:480) sendKeys and never dismisses the keyboard, and getVisibleIssueCount() (847) only counts cells with y>200 — in search-active layout (chips hidden, list pinned under the search bar, keyboard covering the lower half) matching rows fall outside the y>200/h>50 window, so the count reads 0.
- Action: Dismiss the keyboard after typing the query (per repo standing rule), replace the hardcoded y>200 cutoff with a dynamic threshold measured below the search bar, and log first-cell geometry plus the 'No Issues Found' state to disambiguate empty results from probe misses.

### TC_ISS_028_verifySelectingNECViolation  —  **AUTOMATION_BUG**
- Why: selectIssueClassAndGetValue → getIssueClassValue (IssuePage:2425) reads a button with name CONTAINS 'Issue Class' and returns '' from its catch when that element is missing/empty; the failure '(was: \'\')' is the exact documented v1.48/v1.49 Issues DOM regression signature ('pickers read \'\' after selection'), deterministic on rerun. TC_ISS_027 reading the open picker options passed, so only the post-selection read-back contract is stale.
- Action: Remap the Issue Class value read-back for the v1.49 form (dump the picker row's post-selection DOM; the button name likely becomes the bare selected value or moves to a sibling element), mirroring the SiteVisit remap in commit 8090737.

### TC_ISS_029_verifySelectingNFPA70BViolation  —  **AUTOMATION_BUG**
- Why: Identical helper chain and identical '(was: \'\')' signature as TC_ISS_028: getIssueClassValue() returns '' because the 'name CONTAINS Issue Class' button no longer exposes the selected value post-selection in the v1.49 New Issue form DOM.
- Action: Same Issue Class read-back remap as TC_ISS_028; fix once in getIssueClassValue and all six selection tests recover.

### TC_ISS_030_verifySelectingOSHAViolation  —  **AUTOMATION_BUG**
- Why: Same deterministic '(was: \'\')' read-back failure via getIssueClassValue (IssuePage:2425) as TC_ISS_028/029 — the stale post-selection picker contract from the v1.48 Issues DOM regression cluster, not an option-specific problem (all six classes fail identically).
- Action: Same Issue Class read-back remap as TC_ISS_028.

### TC_ISS_031_verifySelectingRepairNeeded  —  **AUTOMATION_BUG**
- Why: Same helper (selectIssueClassAndGetValue → getIssueClassValue) and same empty-string read-back as the other five class-selection tests; the uniform failure across all options confirms a single stale locator/read-back contract rather than per-option app behavior.
- Action: Same Issue Class read-back remap as TC_ISS_028.

### TC_ISS_032_verifySelectingThermalAnomaly  —  **AUTOMATION_BUG**
- Why: Same '(was: \'\')' signature from getIssueClassValue's catch/empty path (IssuePage:2425-2451) — post-selection the 'Issue Class' button is missing or empty in the v1.49 DOM, matching the documented ~76-fail v1.48 Issues regression cluster.
- Action: Same Issue Class read-back remap as TC_ISS_028.

### TC_ISS_033_verifySelectingUltrasonicAnomaly  —  **AUTOMATION_BUG**
- Why: Sixth instance of the identical empty read-back through selectIssueClassAndGetValue/getIssueClassValue; deterministic on rerun, uniform across all class options — stale post-selection picker locator contract.
- Action: Same Issue Class read-back remap as TC_ISS_028.

### TC_ISS_036_verifyPriorityDropdown  —  **AUTOMATION_BUG**
- Why: The test's ensureNewIssueFormOpen() assert passed (form detected), then isPriorityDropdownDisplayed (IssuePage:2623) found neither a button with name CONTAINS 'Priority' nor a StaticText label=='Priority' — the v1.49 New Issue form no longer exposes any 'Priority'-named element, while the Title field probes (TC_ISS_034/035) still work. Stale locator contract, same regression family as the Issue Class read-back.
- Action: Dump the v1.49 New Issue form DOM and remap the Priority row locators (likely renamed label or row rendered as XCUIElementTypeOther like the v1.36 sheet pattern); add the new names to a multi-strategy predicate.

### TC_ISS_037_verifyPriorityOptions  —  **AUTOMATION_BUG**
- Why: openPriorityDropdown (IssuePage:2672) failed all three strategies, every one of which requires an element whose name/label contains 'priority' — the same missing anchor proven by TC_ISS_036 on the same open form. The dropdown can't be opened because the anchor locator is stale, not because options are absent.
- Action: Same Priority-row remap as TC_ISS_036; once the picker anchor is found, keep the existing option assertions (High/Medium/Low) unchanged.

## Issue_Phase1_Test#2

### TC_ISS_038_verifySelectingHighPriority  —  **AUTOMATION_BUG**
- Why: Test never opens the New Issue form: unlike TC_ISS_036/037 (which call ensureNewIssueFormOpen() and PASSED this run), TC_ISS_038 starts directly at 'Step 2: Select High priority' (Issue_Phase1_Test.java:1342-1358, no Step 1). @BeforeMethod lands on the Issues LIST screen, so selectPriority()'s three open-strategies all miss ('Could not open Priority picker' path, IssuePage.java:1384-1387) and getPriorityValue()'s findElement for a 'Priority' button throws, returning '' from its catch (IssuePage.java:2663-2665) — exactly matching "(was: '')" deterministically on both runs.
- Action: Add Step 1 `assertTrue(issuePage.ensureNewIssueFormOpen(), ...)` to TC_ISS_038, identical to TC_ISS_036/037.

### TC_ISS_039_verifySelectingMediumPriority  —  **AUTOMATION_BUG**
- Why: Same missing-navigation defect as TC_ISS_038: no ensureNewIssueFormOpen() call (Issue_Phase1_Test.java:1365-1381 starts at 'Step 2'), so selectPriorityAndGetValue('Medium') runs against the Issues list where no Priority picker exists; getPriorityValue() returns '' from its catch block. TC_ISS_037 passing proves the Priority dropdown and its Medium option work when the form is actually opened.
- Action: Add Step 1 `assertTrue(issuePage.ensureNewIssueFormOpen(), ...)` before selecting priority.

### TC_ISS_040_verifySelectingLowPriority  —  **AUTOMATION_BUG**
- Why: Same missing-navigation defect: TC_ISS_040 (Issue_Phase1_Test.java:1388-1404) never opens the New Issue form before calling selectPriorityAndGetValue('Low'); with the app on the Issues list the picker can't be found and getPriorityValue() returns '', producing the deterministic "(was: '')" on both original and rerun.
- Action: Add Step 1 `assertTrue(issuePage.ensureNewIssueFormOpen(), ...)` before selecting priority.

### TC_ISS_044_verifySearchAssets  —  **AUTOMATION_BUG**
- Why: getAssetListCount() counts EVERY XCUIElementTypeCell on screen with no scoping to the picker table (IssuePage.java:2771-2780), and searchAssetsInPicker() swallows all failures with just a console warning (IssuePage.java:2795-2797) — so a silently-failed type into the reopened picker's search field, or bleed-through cells from the sheet underneath, yields the full 16-cell count instead of 0. TC_ISS_045 passing shows the picker itself opens and works; earlier Step-5 check 'filteredCount <= initialCount' (16<=16) passes even with zero filtering, so the helper has no evidence the search ever applied.
- Action: Scope the cell count to the Select Asset table/sheet container, re-read the search field value after sendKeys to verify the query applied (pick-verify pattern), and make searchAssetsInPicker fail fast instead of swallowing when the field is missing.

### TC_ISS_046_verifyAddAssetButton  —  **AUTOMATION_BUG**
- Why: isSelectAssetScreenDisplayed() assert passed (failure is on the later + button check), so the picker renders; isAddAssetButtonOnPickerDisplayed() is a single Button-type predicate 'name == Add OR name CONTAINS plus OR label == Add' (IssuePage.java:2823-2832) that misses if v1.48/49 re-typed or renamed the toolbar control — same family as the documented v1.48 Issues DOM regression ('Gallery/Delete missing'). Deterministic FAIL on both runs fits a stale locator contract, not flake.
- Action: Capture a page-source snapshot of the Select Asset picker on v1.49 and remap with multi-strategy locators (Image/Other types, icon names like plus.circle, nav-bar-scoped search, coordinate fallback); only reclassify as app change if the snapshot shows the button truly gone.

### TC_ISS_053_verifyIssueDetailsHeader  —  **AUTOMATION_BUG**
- Why: getIssueDetailTitle() does an unscoped findElements of ALL StaticTexts then per-element getAttribute/getLocation round-trips with a hardcoded 80<y<300 window (IssuePage.java:3250-3272) — on the documented giant Issue-Details DOM this is the exact WDA-wedge query pattern; the rerun died at exactly 360000ms (the 6m00s = HUNG signature) and its dead session is the cascade source for the 054-061 rerun driver-init SKIPs. Original run returned '' because the whole-tree query threw (caught, returns "") or the v1.48+ header moved outside the Y window; TC_ISS_052 passing shows details do open.
- Action: Replace with a bounded, container-scoped header query (withImplicitWait(0) + budget per giant-DOM playbook) and remap the header locator against a v1.49 snapshot instead of the Y-window heuristic.

### TC_ISS_054_verifyStatusDropdown  —  **AUTOMATION_BUG**
- Why: isStatusOptionDisplayed() delegates to isDropdownOptionDisplayed() which only probes XCUIElementTypeButton/StaticText with exact label (IssuePage.java:2470-2485), missing SwiftUI sheet/menu rows that render as XCUIElementTypeOther (documented v1.36+ pattern); the single 'found' option (Actual: 1) is almost certainly the on-screen 'Open' badge itself, meaning the menu either never opened (click() no-op quirk in openStatusDropdown Strategy 1, IssuePage.java:3423-3429) or its rows are invisible to the probe — the documented v1.48 'option lists read 0 items' regression. Rerun SKIP is driver-init failure cascade from TC_ISS_053's wedge, not per-test signal.
- Action: Verify the menu actually opened (assert a new overlay/row appears, not just click-returned-true), extend the option probe to XCUIElementTypeOther/Cell, and use W3C down-120ms-up press per the Issues remap.

### TC_ISS_055_verifyChangingStatusToInProgress  —  **AUTOMATION_BUG**
- Why: selectStatus() only taps a Button with exact label match (IssuePage.java:3463-3474) and swallows failure with a console warning; with v1.48+ menu rows rendering as XCUIElementTypeOther (and click() no-op quirks) the selection never lands, so getIssueDetailStatus() legitimately reads back 'Open' — consistent with TC_ISS_054 finding only 1 of 4 options. Original 'Expected: In Progress, Actual: Open' is deterministic automation, rerun SKIP is the dead-session cascade.
- Action: Remap status-menu row locators (Other/Cell types, W3C press), verify menu-open before selecting, and re-read the badge with the pick-verify pattern; note getIssueDetailStatus scans 'Open' first (IssuePage.java:3280-3292) so scope the badge read to the header to avoid false 'Open' matches.

### TC_ISS_056_verifyChangingStatusToResolved  —  **AUTOMATION_BUG**
- Why: Identical mechanism to TC_ISS_055: selectStatus('Resolved') misses the Other-typed menu row / needs W3C press, selection never applies, badge stays 'Open' ('Expected: Resolved, Actual: Open'). All three status-change tests failing the same way plus TC_ISS_054's 1/4 option count point at one shared stale menu-row locator contract, not the app rejecting status changes. Rerun SKIP = driver-init cascade.
- Action: Same Issues status-menu remap as TC_ISS_054/055 (single fix covers 054-057).

### TC_ISS_057_verifyChangingStatusToClosed  —  **AUTOMATION_BUG**
- Why: Identical mechanism to TC_ISS_055/056: selectStatus('Closed') tap never lands on the v1.48+ menu row (Button-only exact-label locator, IssuePage.java:3466-3468), so getIssueDetailStatus() reads 'Open'. Deterministic original FAIL; rerun SKIP is the dead-session cascade from TC_ISS_053's hang.
- Action: Same Issues status-menu remap as TC_ISS_054-056.

### TC_ISS_059_verifyIssueClassOnDetails  —  **AUTOMATION_BUG**
- Why: The presence assert passed (isIssueClassDisplayedOnDetails found the 'Issue Class' StaticText, Issue_Phase1_Test.java:2040), but findIssueClassPickerValue() requires a Button with name CONTAINS 'Issue Class' (IssuePage.java:3565-3566) and returns '' from its catch when v1.48+ re-typed/renamed the picker control — the documented v1.48 'pickers read '' after selection' regression verbatim. Rerun SKIP is the driver-init cascade, original FAIL is deterministic.
- Action: Remap the Issue Details picker locators from a v1.49 page-source snapshot (the Issues equivalent of SiteVisit commit 8090737).

### TC_ISS_061_verifyRequiredFieldsToggle  —  **AUTOMATION_BUG**
- Why: This test asserts only PRESENCE of the toggle (Issue_Phase1_Test.java:2111-2113) — not the switch's accessibility value — so it is NOT the ATS_EAD_06/14 invalid-value pattern; and TC_ISS_060 passed this run, proving the Issue Details section (header + completion %) renders on the same issue. All three strategies in isRequiredFieldsToggleDisplayed() key on the literal text 'Required' (IssuePage.java:3685-3716), so a v1.48/49 rename or re-typing of the filter control makes them all miss. Rerun SKIP is the circuit-breaker cascade, not signal.
- Action: Snapshot the Issue Details section DOM on v1.49 and remap (any Switch inside the section, adjacency to the completion % / '0/1' fraction); keep the assert presence-only — never assert the switch value (view-filter domain truth).

## Issue_Phase1_Test#3

### TC_ISS_062_verifySubcategoryFieldForNEC  —  **AUTOMATION_BUG**
- Why: Exactly-6m00s timeout = documented HUNG-mid-flow signature. getIssueClassOnDetails() returns '' under the v1.48+ Issues DOM (IssuePage.java:3563 catch → ""), forcing changeIssueClassOnDetails() into the hang-prone sheet-picker dance every run; the path also contains unbudgeted whole-tree CONTAINS queries at full implicit wait (isSubcategoryFieldDisplayed :3833, getSubcategoryPlaceholder :3864 with per-element getLocation scans) — the exact WDA-wedge pattern the code comment at IssuePage.java:5945 blames for 'TC_ISS_063..108 6m0s hangs'. Rerun SKIP is breaker cascade, no signal.
- Action: Extend the SUBCAT_BUDGET_MS/withImplicitWait(0) pattern to isSubcategoryFieldDisplayed, getSubcategoryPlaceholder and tryOpenIssueClassPicker, and remap getIssueClassOnDetails from live v1.49 Issue-Details page source so the class read stops returning '' (which triggers the picker dance).

### TC_ISS_063_verifySubcategoryDropdownOptions  —  **AUTOMATION_BUG**
- Why: 'found 0/3' matches the documented v1.48 Issues DOM regression verbatim (deterministic, reproduced on fresh sim in run 28666174784). isSubcategoryOptionDisplayed (IssuePage.java:4239) scans only StaticText|Button|Cell with label CONTAINS, but post-v1.36/v1.48 sheet-picker rows render as XCUIElementTypeOther — the dropdown opens but our predicate reads 0 rows. Stale locator contract, not missing options in the app.
- Action: Capture live v1.49 subcategory-sheet page source and remap the option predicate (add XCUIElementTypeOther / new row structure) in isSubcategoryOptionDisplayed and tapSubcategoryField, mirroring the SiteVisit remap (commit 8090737).

### TC_ISS_064_verifySelectingNECSubcategory  —  **AUTOMATION_BUG**
- Why: Exactly-6m00s timeout = HUNG mid-flow. Same wedge path as 062: class read '' forces changeIssueClassOnDetails' sheet dance; selectSubcategory's Strategy-2 scroll loop plus getSubcategoryValue's all-buttons/all-texts enumerations with getLocation per element (IssuePage.java:4363, :4405) run against the heavy bleed-through details DOM — budgeted internally but the surrounding unbudgeted helpers (tryOpenIssueClassPicker, quickDismissIssueDetails) still query the whole tree at full implicit wait and can wedge WDA.
- Action: Same fix as 062/063: remap subcategory rows + class-picker value read from v1.49 DOM and budget the remaining unbudgeted whole-tree queries in the open/dismiss helpers.

### TC_ISS_065_verifyAllNECSubcategoryOptions  —  **AUTOMATION_BUG**
- Why: '>=5 of 11 … found 0' is the exact documented v1.48 regression signature ('≥5 of 11 NEC options, found 0' in v148-issues-dom-regression.md, deterministic on fresh sim). verifyNECSubcategoryOptions (IssuePage.java:4437) uses the same stale StaticText|Button|Cell predicate as 063 — an all-zero result across 11 independent CONTAINS probes means the row element type changed, not that the app lists nothing.
- Action: Same subcategory-row remap as 063; also make verifyNECSubcategoryOptions scroll the open sheet (options beyond the fold would never match even after remap).

### TC_ISS_066_verifySubcategorySearchable  —  **AUTOMATION_BUG**
- Why: searchSubcategory (IssuePage.java:4467) probes TextField/SearchField predicates from the old DOM and silently falls through when none match, then the assert reads options via the same stale isSubcategoryOptionDisplayed predicate that returned 0 in 063/065 — so 'no Wire option' means our code never typed into the v1.49 search field and can't see rows, part of the same documented deterministic cluster.
- Action: Remap the subcategory search field + option rows from live v1.49 sheet DOM; make searchSubcategory return boolean and fail loudly when no field was found instead of silently returning.

### TC_ISS_071_verifyIssuePhotosSection  —  **AUTOMATION_BUG**
- Why: Failure is at the Gallery assert (test :2574) — the documented v1.48 cluster explicitly lists 'Details controls missing: Gallery button, Delete Issue button' as deterministic locator misses. isGalleryButtonDisplayed (IssuePage.java:4937) is a single XCUIElementTypeButton label/name-CONTAINS predicate against the old DOM; note the Photos section itself was detected (Step 3 passed), so the screen rendered and only our button predicate missed.
- Action: Remap Gallery/Camera button locators from live v1.49 Issue-Details page source (likely new element type or SF-symbol name), keeping the multi-strategy pattern.

### TC_ISS_072_verifyGalleryButton  —  **AUTOMATION_BUG**
- Why: tapGalleryButton (IssuePage.java:4974) uses the same stale predicate that made 071's Gallery assert fail and swallows its own failure ('⚠️ Could not tap Gallery button'), so the photo picker was never requested; the assert on isPhotoPickerDisplayed then correctly reports no picker. Downstream cascade of the same stale Gallery locator, not a picker problem.
- Action: Fix the Gallery locator (same remap as 071) and make tapGalleryButton return boolean so the test can distinguish 'tap failed' from 'picker did not open'.

### TC_ISS_074_verifyDeleteIssueButton  —  **AUTOMATION_BUG**
- Why: 'Delete Issue button missing' is the other named member of the documented deterministic v1.48 'Details controls missing' cluster. isDeleteIssueButtonDisplayed (IssuePage.java:5134) tries three strategies but all resolve to XCUIElementTypeButton label/name-CONTAINS 'Delete' predicates from the old DOM; a consistent miss across scroll+native-scroll strategies indicates the button's element type/label changed in v1.48+, not that the app dropped delete.
- Action: Remap the Delete Issue control from live v1.49 details DOM (check for XCUIElementTypeOther/StaticText rendering or a changed label) and update all three strategies.

### TC_ISS_075_verifyDeleteConfirmation  —  **AUTOMATION_BUG**
- Why: tapDeleteIssueButton (IssuePage.java:5178-5219) uses the same stale Delete predicate as 074 and swallows the failure ('⚠️ Could not tap Delete Issue'), so no confirmation dialog could ever appear; isDeleteConfirmationDisplayed then honestly returns false. Cascade of the stale Delete locator — the confirmation-dialog behavior itself was never exercised.
- Action: Fix the Delete locator first (same remap as 074), have tapDeleteIssueButton return boolean, and gate the confirmation assert on a successful tap.

### TC_ISS_076_verifyConfirmDeleteRemovesIssue  —  **AUTOMATION_BUG**
- Why: Exactly-6m00s timeout = HUNG mid-flow, not a slow assert (a createQuickIssue failure would have fast-failed the 'Should have created temporary issue' assert at test :2769). createQuickIssue (IssuePage.java:5632) chains many unbudgeted full-implicit-wait whole-tree queries (isNewIssueFormDisplayed, selectIssueClass on the regressed create-form picker, tapSelectAsset, selectAssetByName(null), tapCreateIssue) plus isIssueInList/searchIssues over the list — the same giant-DOM WDA-wedge pattern the :5945 comment documents as the 6m0s hang cause.
- Action: Budget/withImplicitWait(0) the createQuickIssue chain and remap the create-form Issue Class picker for v1.49; alternatively create the temp issue via TestDataApi to remove the fragile UI create path from a delete-focused test.

## SiteVisit_phase3#1

### TC_JOB_200_verifyCameraButtonInWalkthrough  —  **APP_BUG**
- Why: CI log (job 86497848922): test navigated cleanly to Photo Walkthrough, tapped Camera at 02:58:56 → 'Camera response: false (type: none)' → teardown reports 'Session appears dead'. This is CAM-CRASH-01 (UIImagePickerController SIGABRT, no isSourceTypeAvailable check, crash log Z Platform-QA-2026-07-07-195531.ips); run 29135128275 ran commit 813f726, which predates the guardCameraTapCrash() call now at SiteVisit_phase3.java:715. The 360s timeout is just the post-crash cleanup grind.
- Action: Guard already landed (f98f5ce + working tree at line 715) — test will SKIP at the tap on next run. Keep quarantined until dev adds the isSourceTypeAvailable(.camera) check; cover the tap on real hardware via REAL_DEVICE=true.

### TC_JOB_201_verifyDoneButtonEnablesAfterPhoto  —  **ENV_INFRA**
- Why: Cascade victim of TC_JOB_200's crash: the log shows 200's timed-out thread STILL running its cleanupFromPhotoWalkthrough probes interleaved with 201 (TestNG timeOut abandons the thread but it keeps driving the singleton driver), and that zombie thread tapped 'Cancel on Add Assets' at 03:05:43 — dismissing the popup 201 had just opened → 'Popup menu items found: 0' → walkthrough unreachable → cap burned. Rerun repeats it because the shard re-runs 200 (unguarded) first, re-crashing the app.
- Action: Re-run now that 200 skips before the camera tap. Framework hardening: on TestNG timeout, invalidate/quit the session (or make page-object waits interruptible) so an abandoned test thread cannot keep issuing commands on DriverManager's shared driver.

### TC_JOB_210_verifyPhotographOCPDScreenLayout  —  **AUTOMATION_BUG**
- Why: Log shows the full deep flow succeeded and the screen was live: subtext 'You can take multiple photos' found, 'No photos yet' found, Gallery/Camera/Done-with-this-OCPD all found, Done disabled — only isTakePhotosOfFirstOCPDHeadingDisplayed() (predicate: label CONTAINS 'Take photos of the first OCPD' OR 'Take photos of'+'OCPD', StaticText only) returned false; the screen detector likewise only matched via the Done-button strategy. v1.49 changed the heading copy/element, so the locator contract is stale, not the screen broken.
- Action: Dump page source / check screenshot TC_JOB_210_...FAILED_20260711_035049.png on the OCPD screen and remap the heading predicate in WorkOrderPage.isTakePhotosOfFirstOCPDHeadingDisplayed (WorkOrderPage.java:15534); until remapped, drop the heading from coreElements and log it informationally.

### TC_JOB_211_verifyDoneWithOCPDEnablesAfterPhoto  —  **AUTOMATION_BUG**
- Why: Deep flow reached Photograph OCPD and Done was correctly disabled; the second tryAddPhotoViaGallery then broke the flow: after the photo tap auto-closed the picker, the helper probed isPhotoWalkthroughScreenDisplayed() (wrong screen — we are on Photograph OCPD), got false twice, and its dismiss-picker branch tapped the OCPD screen's own Cancel ('Dismissed photo picker' 03:55:47) → walkthrough aborted → '\'Done with this OCPD\' button not found for enabled check'; cleanup then instantly found Assets in Room, confirming the Cancel exited the flow. Slow probes (10-40s each) pushed the doomed test past the 360s cap.
- Action: Make tryAddPhotoViaGallery context-aware: pass the expected return-screen probe (Photograph OCPD vs Photo Walkthrough) and only tap Cancel when a picker-specific container (Photos sheet/nav bar) is actually present; also replace the 3-7s-per-image getElementRect enumeration with a first-thumbnail tap to stop 360s budget exhaustion.

### TC_JOB_212_verifyDoneWithOCPDOpensClassifyOCPD  —  **AUTOMATION_BUG**
- Why: Same deterministic helper defect as 211: log 04:03:21 'Dismissed photo picker' (Cancel mis-tap on the Photograph OCPD screen because the helper checks isPhotoWalkthroughScreenDisplayed in an OCPD context) → 04:03:37 '\'Done with this OCPD\' button not found / not enabled after photo' → navigateToClassifyOCPDScreen returned false; the 360s cap fired during the subsequent slow cleanup before the clean 'Could not reach Classify OCPD screen' assert could report.
- Action: Same fix: context-aware tryAddPhotoViaGallery (correct post-add screen probe + picker-scoped Cancel) and faster picker selection; re-run to confirm Classify OCPD opens.

### TC_JOB_213_verifyOCPDAssetTypeOptions  —  **AUTOMATION_BUG**
- Why: Identical trace: 04:09:33 'Dismissed photo picker' → 04:09:51 '\'Done with this OCPD\' button not found / not enabled after photo' — the helper's Cancel mis-tap aborted the OCPD flow before the type dropdown could ever be opened, then the cap fired at 04:10:37 mid-grind. The dropdown-options assertion itself was never evaluated, so there is no signal about the actual OCPD type list.
- Action: Blocked on the tryAddPhotoViaGallery context fix; after that, re-run to actually exercise the Disconnect Switch/Fuse/MCC Bucket/Other (OCP)/Relay options assertion.

### TC_JOB_214_verifyMoreOCPDsScreenAfterClassification  —  **AUTOMATION_BUG**
- Why: Same trace: 04:17:35 'Dismissed photo picker' → 04:17:45 '\'Done with this OCPD\' button not found / not enabled after photo' → Classify OCPD never reached, so More OCPDs was unreachable; 360s cap fired seconds later during cleanup. Deterministic on rerun because the helper defect is code, not environment.
- Action: Same tryAddPhotoViaGallery context fix; the navigateToMoreOCPDsScreen logic itself was never reached and needs no change yet.

### TC_JOB_215_verifyAddAnotherOCPDReturnsToCapture  —  **AUTOMATION_BUG**
- Why: Original run: same Cancel mis-tap family, cap fired at 04:24:36 with the '\'Done with this OCPD\' not found/not enabled' lines printing just after (thread continuation). The rerun confirms the diagnosis with the clean assert 'Could not reach Classify OCPD screen.' — i.e., when probes run fast enough to beat the cap, the test fails exactly at navigateToClassifyOCPDScreen, the step the helper defect breaks.
- Action: Same tryAddPhotoViaGallery context fix, then re-run; expect Add-Another-OCPD round-trip to be testable again.

### TC_JOB_216_verifyDoneWithOCPProceeds  —  **AUTOMATION_BUG**
- Why: Log: deep flow reached Photograph OCPD at 04:30:43, second Gallery tap 04:30:45, and the cap fired at 04:31:04 while the helper was still enumerating the picker ('Cells found in picker: 0' at 04:31:02) — the per-image getElementRect enumeration (3-7s each, run twice per test) plus 10-40s screen probes exhausted the 360s budget before any assertion. Same helper, budget-exhaustion variant of the 211-215 defect.
- Action: Cut picker enumeration cost (tap first thumbnail predicate directly, no per-image rect logging) and apply the context-aware post-add check; consider raising the class timeOut only after the helper is fixed.

### TC_JOB_217_verifyWhatsNextScreenElements  —  **AUTOMATION_BUG**
- Why: Cap fired at 04:37:41 with '\'Done with this OCPD\' button not found for enabled check / not enabled after photo' printing immediately after — the thread was at navigateToClassifyOCPDScreen's Done check when time ran out, i.e., the same OCPD-context tryAddPhotoViaGallery defect blocked the chain, so the What's Next screen (three steps further) was never reachable.
- Action: Blocked on the tryAddPhotoViaGallery context fix; consider a shorter fixture path to What's Next (skip OCPD via 'No/Skip' on the Add OCPDs prompt) so What's-Next assertions don't depend on the OCPD photo chain.

### TC_JOB_218_verifyAssetCountInWhatsNext  —  **AUTOMATION_BUG**
- Why: Identical to 217: cap at 04:44:16 with the '\'Done with this OCPD\' not found / not enabled after photo' pair printing right after the FAIL — deterministic OCPD-context helper defect blocks the flow well before the What's Next asset-count assertion; no product signal about the count itself.
- Action: Same helper fix + route to What's Next via the 'No, skip OCPD' branch so the count assertions decouple from the broken OCPD photo chain.

### TC_JOB_219_verifyPhotoCountInWhatsNext  —  **AUTOMATION_BUG**
- Why: Same blocked chain, compounded by the zombie-thread cascade: 219's log is interleaved with 218's abandoned cleanup thread ('Dismissing Photograph OCPD', back-taps) fighting its picker flow, and the session died mid-test ('Error communicating with the remote browser' at 04:50:56). Root deterministic blocker is still the OCPD-context tryAddPhotoViaGallery defect (rerun also 360s), with the timeout-thread contention as an aggravator.
- Action: Fix tryAddPhotoViaGallery context handling first, plus the framework-level timeout-thread/session invalidation so one timed-out test can't poison the next; then re-run to get a real signal on the 'X photos total' assertion.

## SiteVisit_phase3#2

### TC_JOB_220_verifyAddAnotherAssetReturnsToCapture  —  **AUTOMATION_BUG**
- Why: Deterministic (rerun FAIL with identical 360000ms timeout = documented exactly-6m00s HUNG signature). The test runs navigateToWhatsNextScreen() — ~15 UI steps starting at the giant Locations tree (navigateToAssetsInRoom) with every probe a whole-tree iOSNsPredicateString findElements with multi-CONTAINS fallbacks (isWhatsNextScreenDisplayed @WorkOrderPage:15829, tapFirstRoomWithAssets @2298); on the bleed-through sheet stack these queries run ~90s each and wedge WDA. No camera tap anywhere in this flow (gallery only), so CAM-CRASH-01 does not apply despite changelog 116's blanket attribution.
- Action: Scope the deep-flow probes: withImplicitWait(0) + bounded/first-match queries instead of whole-tree StaticText CONTAINS scans, add per-step time budgets so the helper fails fast, and ideally seed walkthrough state via TestDataApi instead of rebuilding 15 UI steps per test.

### TC_JOB_221_verifyFinishWalkthroughOpensReviewAssets  —  **AUTOMATION_BUG**
- Why: Original run hit its own 360s per-method cap (GlobalTestTimeout) mid-flow — the test independently re-runs the full navigateToWhatsNextScreen() chain whose whole-tree predicate probes wedge WDA on the giant/bleed-through DOM. Rerun SKIP is the 330-min wall-clock cascade (no per-test signal), so the original hang is the verdict evidence.
- Action: Same fix as TC_JOB_220: scoped bounded queries + per-step budgets in navigateToWhatsNextScreen/isReviewAssetsScreenDisplayed; share or API-seed the walkthrough fixture instead of per-test UI rebuild.

### TC_JOB_222_verifyReviewAssetsScreenLayout  —  **AUTOMATION_BUG**
- Why: Original 360s hang in its own flow; on top of navigateToWhatsNextScreen() this test adds getReviewAssetsEntries()/getReviewAssetsSummaryTexts() — more whole-tree element enumeration on the wedge-prone stacked-sheet DOM. Rerun SKIP = wall-clock cascade, no counter-signal.
- Action: Bound the Review Assets entry/summary queries (scoped container query, implicit-wait 0) and add step budgets; API-seed captured-asset state.

### TC_JOB_223_verifyAssetHierarchyInReviewAssets  —  **AUTOMATION_BUG**
- Why: Original 360s hang; same navigateToWhatsNextScreen() + getReviewAssetsEntries()/isOCPDChildDisplayedUnderParent() whole-tree scans over the bleed-through DOM. The assertion itself (entries.size() >= 2 || hasOCPDChild) was never reached — the flow hung before asserting. Rerun SKIP = wall-clock cascade.
- Action: Same scoped-query/step-budget fix; log which nav step is active when the cap fires so hangs localize themselves.

### TC_JOB_224_verifyReviewAssetsSummaryCounts  —  **AUTOMATION_BUG**
- Why: Original 360s hang mid-flow before its lenient assert (hasAssets || hasPhotos on summary text) could run; identical deep-flow query pattern as siblings — each test independently burned its own 6-minute cap, so this is the query pattern itself, not a dead-session cascade victim. Rerun SKIP = wall-clock cascade.
- Action: Same scoped-query/step-budget fix as TC_JOB_220-223.

### TC_JOB_225_verifyAddMoreReturnsToCapture  —  **AUTOMATION_BUG**
- Why: Original 360s hang in navigateToWhatsNextScreen()/navigateToReviewAssetsScreen() before tapAddMoreButton was reached; same whole-tree predicate probes (isPhotoWalkthroughScreenDisplayed @WorkOrderPage:5371 runs 3 unbounded strategies) wedging WDA on the stacked-sheet DOM. Rerun SKIP = wall-clock cascade.
- Action: Same scoped-query/step-budget fix; verify Add More round-trip with one bounded probe instead of the 3-strategy whole-tree scan.

### TC_JOB_226_verifyCreateAllButtonShowsCount  —  **AUTOMATION_BUG**
- Why: Original run: own 360s hang in the same deep flow (test never reached its assert on getCreateAllButtonText). Rerun SKIP is the dead-session breaker opening — itself a downstream symptom of WDA wedged by these whole-tree queries, so it corroborates rather than contradicts the wedge mechanism.
- Action: Same scoped-query/step-budget fix; after fixing, re-run this block on a fresh sim since breaker skips left it untested.

### TC_JOB_227_verifyCreateAllInitiatesCreation  —  **AUTOMATION_BUG**
- Why: Original 360s hang before Create All was tapped (flow hangs in navigateToWhatsNextScreen's whole-tree probes); assert is lenient (creatingScreen || successAlready) and never executed. Rerun SKIP = dead-session breaker cascade, no per-test signal.
- Action: Same scoped-query/step-budget fix; API-seed a review-ready walkthrough so Create All tests exercise only creation, not the 15-step capture flow.

### TC_JOB_228_verifyCreationProgressIndicator  —  **AUTOMATION_BUG**
- Why: Original 360s hang in the same pre-assert deep flow; its rapid-fire progress polling (10x isCreationProgressIndicatorDisplayed/isSuccessDialogDisplayed whole-tree scans) adds further wedge-prone queries but the hang precedes it. Rerun SKIP = dead-session breaker cascade.
- Action: Same scoped-query/step-budget fix; bound the progress-poll probes with implicit-wait 0 so 200ms polling is actually 200ms.

### TC_JOB_229_verifySuccessDialogAfterCreation  —  **AUTOMATION_BUG**
- Why: Original msg is the test's own precondition guard: assertTrue(false, "Could not navigate to What's Next? screen.") @SiteVisit_phase3:3002 after navigateToWhatsNextScreen() returned false — the same fragile 15-step helper failing fast (a probe returned false) right after nine consecutive 6-minute WDA wedges degraded the session/app state in the same job. No evidence of app misbehavior; the helper doesn't report which step failed. Rerun SKIP = breaker cascade.
- Action: Make navigateToWhatsNextScreen() log/attach the failing step and screenshot; apply the scoped-query fix so upstream tests stop wedging the session this test inherits.

### TC_JOB_230_verifyDoneDismissesSuccessAndShowsAssets  —  **AUTOMATION_BUG**
- Why: Original msg is the guard assertTrue(false, "Could not navigate to Success dialog for this test.") @SiteVisit_phase3:3181 — navigateToSuccessDialog() (wraps navigateToWhatsNextScreen + Review Assets + Create All + waitForCreationCompletion(30) + isSuccessDialogDisplayed) returned false at an unreported step, on the session already degraded by the preceding wedge block. Same automation-side deep-flow fragility; no app-misbehavior evidence. Rerun SKIP = breaker cascade.
- Action: Instrument navigateToSuccessDialog() to report the failing step; scoped queries + step budgets; consider verifying creation via TestDataApi instead of the Success-dialog probe.

### TC_JOB_231_verifyCreatedAssetsAppearInRoomList  —  **AUTOMATION_BUG**
- Why: Original 360s hang — this test runs the longest chain of all (navigateToSuccessDialog full creation flow, then getAssetsInRoomListCount/getAssetNamesInRoomList back on the giant-tree-backed Assets in Room screen), maximizing exposure to the whole-tree query wedge. Its assert (onAssetsInRoom && assetCount > 0) never ran. Rerun SKIP = dead-session breaker cascade.
- Action: Same scoped-query/step-budget fix; verify created assets via TestDataApi room-asset query rather than re-enumerating the room list UI.

## SiteVisit_phase3#3

### TC_JOB_235_verifyMultipleAssetsInSingleWalkthrough  —  **AUTOMATION_BUG**
- Why: Exactly-360000ms timeout = documented HUNG-mid-flow signature, and the hang is produced by our own code: the flow enters via navigateToAssetsInRoom() whole-tree Locations queries (documented giant-DOM WDA wedge zone) and on any misstep falls into cleanupFromPhotoWalkthrough(), whose own comment at SiteVisit_phase3.java:276-284 admits its 10-iteration x ~12 whole-DOM screen-probe loop 'burned the full 360s cap on every phase-3 test' — the Dashboard bail-fast covers only one escape screen. Chronic: same test also failed 2026-07-02 and 2026-07-03; breaker was not open in the original run (it burned the full cap instead of fast-skipping), so this is the query-pattern side of the wedge, not a cascade victim.
- Action: Bound the deep-flow: scoped/class-chain queries with withImplicitWait(0) and per-step time budgets in navigateToAssetsInRoom and the walkthrough helpers, add a wall-clock watchdog that aborts the test well under 360s, and extend the cleanup-loop bail-fast beyond Dashboard (bail after N consecutive 'no known screen matched' iterations).

### TC_JOB_236_verifyExistingAssetTabShowsAvailableAssets  —  **AUTOMATION_BUG**
- Why: Original failure is the exactly-360000ms hang, and the test's very first step navigateToExistingAssetTab() -> navigateToAddAssetsScreen() -> navigateToAssetsInRoom() drives whole-tree queries (getLocationsBuildingCount, tapFirstRoomWithAssets) into the documented giant-DOM Locations-tree WDA wedge; its actual assertion (hasAssets || emptyState, line 3675) is lenient and was never reached. Chronic: also in failed-2026-07-02.xml and failed-2026-07-03.xml, so this is the test's own wedging query pattern, not a one-off cascade.
- Action: Same wedge fix as TC_JOB_235: scoped tree queries + per-step budgets + watchdog in navigateToAssetsInRoom; the fast-path tapFirstRoomWithAssets should run under withImplicitWait(0) with a hard timeout before falling to the slow path.

### TC_JOB_237_verifyAssetSelectionCheckbox  —  **AUTOMATION_BUG**
- Why: Identical mechanism to TC_JOB_236: exactly-360000ms hang in the shared navigateToExistingAssetTab() entry path through the giant Locations tree before any checkbox interaction could run; the final assert is only assertTrue(tapped) at line 3753, which was never reached. Chronic across 07-02/07-03 runs, and the original run burned the full cap rather than fast-skipping, ruling out a dead-session cascade victim.
- Action: Fix the shared navigation wedge (scoped queries, budgets, watchdog); once navigation is bounded, also tighten the weak assert — it currently passes if the tap landed even when the checkbox never toggled (toggledOnFirstTap/SecondTap are computed but not asserted).

### TC_JOB_238_verifyAddButtonShowsSelectedCount  —  **DATA_FIXTURE**
- Why: The failure is the explicit precondition guard at SiteVisit_phase3.java:3791 ('Need at least 2 existing assets. Found: ' + assetCount) — reaching it proves navigateToExistingAssetTab() succeeded, so the session was alive and on the correct tab. getExistingAssetListCount() (WorkOrderPage:5034, XCUIElementTypeCell y>250 30<h<200) counted >=2 with the same locator on 2026-07-02 (test absent from that failed suite), so the count contract works when assets exist; the tab's empty state is documented app behavior 'when all room assets are already linked to the session', and this run's upstream asset-creation tests (235-237) all hung, leaving nothing to replenish the pool.
- Action: Seed >=2 unlinked assets in the fixture room via TestDataApi before the test (or unlink assets in the Add-linking tests' cleanup so the pool doesn't decay); when seeded, log isNoAvailableAssetsDisplayed() alongside the count to distinguish true-empty from any future cell-type drift.

### TC_JOB_239_verifyMultipleAssetSelection  —  **DATA_FIXTURE**
- Why: Same guard as TC_JOB_238, at SiteVisit_phase3.java:3871: navigation to the Existing Asset tab succeeded (otherwise the message would be 'Could not navigate to Existing Asset tab') and getExistingAssetListCount() legitimately read 0 available assets — the fixture room's assets were either all already linked to the session (documented empty-state condition in WorkOrderPage.isNoAvailableAssetsDisplayed) or never created because the upstream walkthrough-creation chain hung this run. The same test passed on 2026-07-02 with the same locators, so this is state shortage, not detection.
- Action: Same fixture fix as TC_JOB_238: deterministic pre-test seeding of >=2 unlinked room assets via the QA backend API, plus unlink-on-cleanup in tests that consume the pool; keep the guard but make it SKIP-with-seed-attempt rather than a hard FAIL once seeding exists.

## SiteVisit_phase1#1

### TC_JOB_006_verifyWorkOrderEntryDisplaysAllInfo  —  **AUTOMATION_BUG**
- Why: Exactly-360000ms timeout (HUNG signature) on both original and rerun-with-fresh-login. The flow calls getWorkOrderEntryCount/getWorkOrderTitleElements (WorkOrderPage.java:321/:393) which fetch ALL cells/StaticTexts with no scope and issue per-element getLocation()/getSize() HTTP round-trips; sibling tests 072-075 in the same run returned clean asserts, so the session was alive — the hang is intrinsic to these unbounded scans (Work Orders list bloats every run since 044/060/069 create jobs, and an active-WO dashboard card can land the scan on the giant Session Details DOM).
- Action: Scope the entry-count/title queries to the list container with a per-query budget, hard-verify waitForWorkOrdersScreen succeeded before any scan (fail fast if on Session Details), and periodically purge accumulated test-created work orders.

### TC_JOB_044_verifyStartNewJob  —  **AUTOMATION_BUG**
- Why: Same deterministic 6m00s hang signature. Besides the unbounded getWorkOrderEntryCount scan, the test unconditionally calls ensureOnSessionDetailsScreen() right after tapping Start New (SiteVisit_phase1.java:2015) — siteSelectionPage.clickWorkOrderCard() + ensureSessionDetailsOpen() deliberately open the giant Session Details DOM mid-test, whose multi-strategy whole-tree StaticText detectors (isSessionDetailsScreenDisplayed) are the documented ~90s-per-query wedge surface.
- Action: Remove the unconditional ensureOnSessionDetailsScreen() detour; after tapping Start New, probe only for the New Job sheet (Create+Cancel, bounded waitForNewJobScreen) and treat any other outcome with a single scoped check.

### TC_JOB_060_verifyCustomJobNameSaved  —  **AUTOMATION_BUG**
- Why: Deterministic 6m00s hang. After tapCreateJobButton the verification loop calls getWorkOrderName(i) for up to 5 indices (SiteVisit_phase1.java:3414), and each call re-runs getWorkOrderTitleElements — a full type=='XCUIElementTypeStaticText' whole-tree fetch plus per-element getLocation() — so post-create verification alone is 5+ unbounded whole-tree scans on a list that grows every run.
- Action: Verify the custom name with one scoped predicate query (label CONTAINS 'abhiyant') under a query budget instead of iterating full title scans; cap the scan or fail fast if a single query exceeds ~15s.

### TC_JOB_069_verifyCreateButtonCreatesJob  —  **AUTOMATION_BUG**
- Why: Deterministic 6m00s hang in the same create-flow family: getWorkOrderEntryCount + getAvailableBadgeCount + getActiveBadgeCount before creation and again after, plus getWorkOrderName(0)/getWorkOrderDate(0) — every helper is an unbounded whole-tree findElements with per-element getLocation() loops (WorkOrderPage.java:321-464), which wedge on the bloated/bleed-through DOM long before the assert can fire.
- Action: Replace the before/after count-diff protocol with a single scoped existence check for the newly created job name, and add a per-method WDA query budget so the test fails with evidence instead of hanging.

### TC_JOB_072_verifyQuickQRActionOptions  —  **AUTOMATION_BUG**
- Why: tapQuickQRActionDropdown returned false → hard assert, deterministic. The test drives to the WRONG screen: ensureOnNewJobScreen opens the New Work Order form, but docs/site-visit-flow.md shows that form contains only Name/Photo Type/Team/Create — 'Quick QR Action' exists only on Active Work Order (Session) Details, and isSessionDetailsScreenDisplayed (WorkOrderPage.java:987) even uses it as a Session-Details-unique marker 'verified live DOM'. All 4 tap strategies anchor on a 'Quick QR Action' label or Full Asset/Data Collection/IR Photos buttons, none of which exist on the New Job form.
- Action: Retarget the test to ensureOnSessionDetailsScreen() (like TC_JOB_018/019) before touching the Quick QR Action dropdown.

### TC_JOB_073_selectFullAssetQRAction  —  **AUTOMATION_BUG**
- Why: Same wrong-screen defect: verifyQuickQRActionSelection (SiteVisit_phase1.java:6049) was 'fixed' to call ensureOnNewJobScreen, but the Quick QR Action dropdown is a Session Details element per the flow doc and the live-DOM-verified marker comment in isSessionDetailsScreenDisplayed; tapQuickQRActionDropdown can never find the label on the New Job form, so it deterministically returns false and asserts 'should be tappable to select Full Asset'.
- Action: Change verifyQuickQRActionSelection to navigate via ensureOnSessionDetailsScreen(), then re-validate the option-select + checkmark logic on the real screen.

### TC_JOB_074_selectDataCollectionQRAction  —  **AUTOMATION_BUG**
- Why: Identical to TC_JOB_073 — shared helper verifyQuickQRActionSelection navigates to the New Job form where no 'Quick QR Action' label or QR-action value buttons exist (form fields are Name/Photo Type/Team per docs/site-visit-flow.md), so the dropdown tap deterministically fails on both original and rerun.
- Action: Fix the shared helper's navigation target to Session Details; one fix clears 073/074/075.

### TC_JOB_075_selectIRPhotosQRAction  —  **AUTOMATION_BUG**
- Why: Identical to TC_JOB_073/074 — the shared helper looks for the Quick QR Action dropdown on the New Job form, a screen that does not contain it (it lives on Session Details, where it is used as that screen's unique detection marker), so tapQuickQRActionDropdown exhausts all 4 strategies and returns false every run.
- Action: Same shared-helper navigation fix (ensureOnSessionDetailsScreen before the dropdown dance).

### TC_JOB_076_verifyLocationsTabShowsBuildings  —  **AUTOMATION_BUG**
- Why: Deterministic 6m00s hang after tapSessionTab("Assets") opens the documented giant session Locations tree: isLocationsTabContentDisplayed's Strategy 2 fetches ALL cells with per-cell getLocation(), and getLocationsBuildingNames runs two whole-tree StaticText queries plus nested Y-proximity getLocation loops (WorkOrderPage.java:2044-2282) — memory documents these whole-tree queries at ~90s each on this tree, so a handful exceed the 6-min method timeout.
- Action: Replace whole-tree scans in the Locations helpers with scoped mobile:scroll-predicate probes and container-scoped queries per the sitevisit-locations-giant-tree memory, with a hard per-query budget.

### TC_JOB_077_verifyBuildingListDetails  —  **AUTOMATION_BUG**
- Why: Same giant-tree hang: getLocationsBuildingCount → getLocationsBuildingNames (double whole-tree StaticText scan + O(floors×candidates) getLocation matching) then getLocationsBuildingInfo per building, whose legacy strategy fetches all cells and runs child findElements inside each (WorkOrderPage.java:2380-2432) — an unbounded query storm on the >20-deep session tree that predictably exceeds 360000ms on both original and rerun.
- Action: Same scoped-query rewrite of the building-row helpers; anchor on the 'N floors' row via mobile:scroll predicate and read only that row's subtree.

### TC_JOB_078_verifyExpandableBuildings  —  **AUTOMATION_BUG**
- Why: Same family plus more: after the getLocationsBuildingCount/Info scans it calls tapLocationsBuildingAtIndex and isLocationsBuildingExpanded twice each — every step re-runs whole-tree queries on the giant session Assets tree, so the test hangs mid-flow at exactly the 6-minute cap deterministically (giant-DOM WDA wedge, the query-pattern side of that mechanism).
- Action: Bound and scope the expand/collapse detection (query only the tapped row's subtree at zero implicit wait) as part of the same Locations-helpers rewrite.

### TC_JOB_079_verifyNoLocationSection  —  **AUTOMATION_BUG**
- Why: Unlike siblings 077/078/080, this test never calls ensureOnSessionDetailsScreen() — it taps 'Assets' from whatever screen the prior (timed-out) test left, and TC_JOB_077's own comment (SiteVisit_phase1.java:4176) documents that this tab tap 'silently no-ops on the dashboard' (tapSessionTab strategy 3 taps ANY element labelled 'Assets', e.g. a dashboard tile). That it finished with a clean false (no 6-min hang) shows isNoLocationSectionDisplayed ran on a small non-session screen, not the giant Locations tree.
- Action: Add ensureOnSessionDetailsScreen() before the tab tap (mirror 077/078/080); if the section is then genuinely absent, gate on a TestDataApi check for unassigned assets and SKIP as DATA_FIXTURE instead of hard-failing.

## SiteVisit_phase1#2

### TC_JOB_080_verifyBuildingRowAddButton  —  **AUTOMATION_BUG**
- Why: Hangs at exactly 360000ms (HUNG signature) inside getLocationsBuildingCount()->getLocationsBuildingNames() (WorkOrderPage.java:2192), whose Step 2 queries ALL StaticTexts with label.length>2 on the giant Locations tree then runs a nested floorYPositions x candidateTexts loop calling getLocation() per pair (O(NxM) WDA round-trips at ~90s/query), plus isBuildingRowAddButtonDisplayed() x3 repeating the anchor+scan pattern. Deterministic rerun FAIL = the test itself is the wedge source, not a cascade victim.
- Action: Replace whole-tree StaticText enumeration with a single scoped/bounded query (mobile:scroll predicateString to anchor, then query only the visible row band); batch/cap getLocation() calls and add a per-query watchdog so a wedged query fails fast instead of hanging 6m.

### TC_JOB_084_verifyTappingRoomOpensAssetsInRoom  —  **AUTOMATION_BUG**
- Why: 6m00s hang mid-flow: the inline chain getLocationsBuildingCount -> tapLocationsBuildingAtIndex -> getLocationsFloorEntries (WorkOrderPage.java:3183, whole-tree StaticText scan whose own comment admits '300+ floors in building', with getLocation() per hit) -> getLocationsRoomEntries runs 4+ giant-tree queries in sequence at ~90s each. Deterministic on rerun; never reaches the assertsTrue(assetsInRoomOpen) assertion.
- Action: Use the existing fast path tapFirstRoomWithAssets() (single 'Room*' button query, WorkOrderPage.java:2298) instead of the building->floor->room expand chain; cap floor/room enumeration at first N visible rows via scoped queries.

### TC_JOB_085_verifyAssetsInRoomBreadcrumb  —  **AUTOMATION_BUG**
- Why: Hangs in navigateToAssetsInRoom() (SiteVisit_phase1.java:5986), which polls getLocationsFloorEntries()/getLocationsRoomEntries() inside Waits.until(...,3000,500) — each poll iteration re-dumps the giant Locations tree (~90s/query) so the 'poll for 3s' loop actually runs many minutes. Rerun FAIL at the same 360000ms confirms deterministic query-pattern hang, not breaker cascade.
- Action: Route navigateToAssetsInRoom() through tapFirstRoomWithAssets() and make the floor/room polls use a single cheap existsNow probe instead of full enumeration per poll iteration.

### TC_JOB_086_verifyAssetsInRoomEmptyState  —  **AUTOMATION_BUG**
- Why: Same navigateToAssetsInRoom() giant-tree hang as 085: whole-tree StaticText scans + per-element getLocation() in getLocationsFloorEntries/getLocationsRoomEntries wedge WDA before the empty-state check is ever attempted. Exactly-6m timeout on both original and rerun = hung in enumeration, not a failed assert (the final assert is only onAssetsScreen, which is lenient).
- Action: Same fix as 085 — fast-path room entry + scoped/bounded tree queries; the test body itself needs no change.

### TC_JOB_087_verifyDoneButtonClosesAssetsInRoom  —  **AUTOMATION_BUG**
- Why: Deterministic 360000ms hang inside the shared navigateToAssetsInRoom() expand chain (buildings->floors->rooms whole-tree queries at ~90s each plus polling); the Done-button logic (isAssetsInRoomDoneButtonDisplayed/tapAssetsInRoomDoneButton) is never reached. Rerun FAIL rules out dead-session-breaker cascade.
- Action: Fix the shared navigation helper (fast-path room tap, bounded queries); no change needed to the Done-button assertions.

### TC_JOB_088_verifyFloatingPlusOpensAddAssets  —  **AUTOMATION_BUG**
- Why: Hangs in navigateToAssetsInRoom() before the floating + interaction: the helper's getLocationsBuildingCount/FloorEntries/RoomEntries whole-tree scans wedge WDA on the giant Locations DOM (documented Locations giant-tree blocker, verified in WorkOrderPage.java:2192/3183/3305). 6m00s on original and rerun = hung mid-navigation, deterministic.
- Action: Same shared-helper fix; additionally give tapAssetsInRoomFloatingPlusButton/waitForAddAssetsScreen a session-alive guard so a wedged prior step fails fast with a diagnostic instead of timing out the method.

### TC_JOB_089_verifyAddAssetsLocationBreadcrumb  —  **AUTOMATION_BUG**
- Why: Identical navigateToAssetsInRoom() hang; the breadcrumb assertions (including the popup-menu carve-out at SiteVisit_phase1.java:5133) are never evaluated. Exactly-360000ms on both runs matches the HUNG-mid-flow signature of the giant-tree query pattern, not a breadcrumb-detection failure.
- Action: Fix the shared navigation helper (bounded, scoped Locations queries / fast-path room tap); re-run before touching breadcrumb locators.

### TC_JOB_090_verifyExistingAssetTabFunctionality  —  **AUTOMATION_BUG**
- Why: Original failure 'Error communicating with the remote browser. It may have died' is the WDA session dying under the wedge this test's own navigation creates (navigateToAddAssetsScreen -> navigateToAssetsInRoom whole-tree queries); rerun reproduced as a deterministic 360000ms hang. Because the test itself executes the wedging query pattern (and reruns FAIL, not SKIP), this is the query-pattern bug, not an ENV_INFRA cascade victim.
- Action: Fix navigateToAssetsInRoom() query pattern (fast-path room tap + scoped queries); add a driver-alive check between navigation steps so session death surfaces as an immediate diagnostic failure.

### TC_JOB_091_verifyNoAvailableAssetsMessage  —  **AUTOMATION_BUG**
- Why: 6m00s hang on both original and rerun inside navigateToAddAssetsScreen() -> navigateToAssetsInRoom() (SiteVisit_phase1.java:5874/5986); the No-Available-Assets check is never reached (final assert is merely onAddAssets, so it cannot itself time out). Deterministic giant-tree query wedge in our page-object enumeration code.
- Action: Same shared-helper fix as 085-090; no change to the empty-state assertions.

### TC_JOB_092_verifyNewAssetTabSelection  —  **AUTOMATION_BUG**
- Why: Hangs deterministically at 360000ms in the shared Add-Assets navigation (giant Locations tree enumeration), before isCreateNewAssetOption/QuickCount/PhotoWalkthrough checks run. The assertion itself (>=2 of 3 creation options) is a valid UI contract and is never evaluated.
- Action: Fix navigateToAssetsInRoom()/navigateToAddAssetsScreen() query pattern; then re-run to see if the creation-options assertions actually pass.

### TC_JOB_093_verifyCreateNewAssetOptionDisplay  —  **AUTOMATION_BUG**
- Why: Same deterministic 6m hang in navigateToAddAssetsScreen()'s underlying Locations-tree enumeration (whole-tree StaticText scans + per-element getLocation at ~90s/query); getNewAssetOptionDetails('Create New Asset') is never invoked, so this is not an option-locator failure.
- Action: Fix the shared navigation helper; keep the option-details assertions unchanged and re-triage only if they fail after navigation is fixed.

### TC_JOB_094_verifyCreateQuickCountOptionDisplay  —  **AUTOMATION_BUG**
- Why: Original java.util.concurrent.TimeoutException on an IOSDriver command is a WDA command starving under the giant-tree wedge created by this test's own navigation; rerun reproduced as the standard 360000ms hang. Deterministic rerun FAIL = query-pattern bug in navigateToAssetsInRoom()'s whole-tree enumeration, not sim flake or fixture shortage (test never reaches the Quick Count option).
- Action: Same shared-helper fix (fast-path room tap + scoped/bounded Locations queries + per-query watchdog); then verify the Quick Count option details assertions on a clean run.

## SiteVisit_phase1#3

### TC_JOB_095_verifyCreatePhotoWalkthroughOptionDisplay  —  **AUTOMATION_BUG**
- Why: Hung at exactly 6m00s (documented HUNG-mid-flow signature) inside the shared nav prefix navigateToAddAssetsScreen()->navigateToAssetsInRoom() (SiteVisit_phase1.java:5874/:5986), which executes the giant-DOM wedge query pattern itself: getLocationsBuildingNames() (WorkOrderPage.java:2192) does whole-tree StaticText predicate scans plus nested per-element getLocation()/getAttribute() WDA round-trips, and Waits.until at :6010 re-runs the whole-tree getLocationsFloorEntries() scan every poll (the 3s cap bounds scheduling, not each ~90s wedged query). Rerun FAIL on a fresh shard proves it is deterministic and self-inflicted, not a breaker/dead-session cascade; CAM-CRASH does not apply because the test only reads the menu row's title/description via getNewAssetOptionDetails() (itself an unbounded 'label CONTAINS' whole-tree scan) and never taps the camera.
- Action: Rewrite the Locations traversal with scoped queries: replace whole-tree StaticText scans in getLocationsBuildingNames/getLocationsFloorEntries/getLocationsRoomEntries with table-scoped class-chain or mobile:scroll-to-predicate lookups, batch per-element rect reads instead of per-element getLocation() calls, prefer the v1.48 fast path tapFirstRoomWithAssets() over expand-building/floor, and add a per-step wall-clock budget in navigateToAssetsInRoom so nav fails fast instead of eating the 360s method timeout. Also bound getNewAssetOptionDetails() to the popup container instead of 'label CONTAINS' over the entire DOM.

### TC_JOB_096_verifyCancelClosesAddAssets  —  **AUTOMATION_BUG**
- Why: Identical deterministic 6m00s hang in the same shared prefix — the test body after navigation is trivial (tapAddAssetsCancelButton + isAssetsInRoomScreenDisplayed, SiteVisit_phase1.java:5575/:5581) and is never reached; the timeout is consumed by navigateToAssetsInRoom()'s unscoped whole-tree Locations queries (getLocationsBuildingNames WorkOrderPage.java:2192, getLocationsFloorEntries :3190, getLocationsRoomEntries :3305) whose per-element attribute loops each cost a WDA round-trip on the wedged ~90s giant tree. Rerun FAIL (not SKIP) rules out the dead-session-breaker/ENV_INFRA cascade — this test executes the wedging query pattern itself, matching the repo's 'AUTOMATION_BUG for the query pattern itself' classification.
- Action: Same fix as TC_JOB_095 — it is one shared-helper defect, not two: scope/batch the Locations tree queries in navigateToAssetsInRoom and its WorkOrderPage helpers (class-chain or scroll-to-predicate, cached rects, fast room-button path) and add a nav step budget; both tests should be re-triaged together after the helper is hardened.

## LocationTest#1

### TC_AL_001_verifyAssetDetailsShowsSelectLocationForUnassignedAsset  —  **AUTOMATION_BUG**
- Why: Fails at assertTrue(isNoLocationDisplayedFast(), "'No Location' section should be visible") after scrollToNoLocationTurbo(), whose budget is only 5 fast half-screen drags (BuildingPage.java:5168) — insufficient to reach the No Location row below the documented giant buildings list; methodSetup also swallows nav failures (LocationTest.java:91), so the probe can run on the wrong screen. Fast assertion failure (not a 90s-query timeout) shows the section simply wasn't on the visible portion.
- Action: Replace the 5-drag TURBO loop with mobile:scroll + predicateString targeting 'No Location' (per giant-tree memory) and assert Locations-screen context before scrolling instead of swallowing setup failures.

### TC_AL_002_verifyTappingLocationOpensLocationPicker  —  **AUTOMATION_BUG**
- Why: The test ignores tapOnNoLocationFast()'s return and its guard areUnassignedAssetsDisplayed() is vacuous — predicate 'XCUIElementTypeCell OR (Button AND NOT Done AND NOT No Location)' (BuildingPage.java:5591) matches buildings/floors on the Locations list itself — so after a failed No-Location open it clicked a random first cell and then asserted hasSelectLocationField() ("Asset should have 'Select location' field") against the wrong screen.
- Action: Assert tapOnNoLocationFast() succeeded and verify the No Location screen title/Done before proceeding; scope the unassigned-asset guard to cells inside that screen.

### TC_AL_008_verifyReassigningAssetToDifferentRoom  —  **AUTOMATION_BUG**
- Why: Exactly-6m00s timeout = documented HUNG-mid-flow signature, and the flow chains whole-tree scans on the giant Locations DOM: getAssetCountFromRoomLabel, tapOnRoom→findRoomByName plus three picker-wide selectBuilding/Floor/DifferentRoomInPicker queries — the giant-DOM WDA-wedge query pattern itself (each wedged query ~90s, several exceed the 360s budget).
- Action: Bound each lookup (withImplicitWait(0) + Waits budget) and use scoped mobile:scroll predicateString queries instead of whole-tree 'label CONTAINS' scans; this test is also the likely session-killer for the shard, so fix before rerunning the class.

### TC_DR_001_verifyRoomDeletedImmediatelyOnTap  —  **AUTOMATION_BUG**
- Why: Exactly-6m00s timeout = HUNG signature; the test performs 4+ consecutive whole-tree scans (createTestRoom, findRoomByName(testRoomName)/('TestDelete')/('DelTest'), getFirstRoomEntry, then longPressOnRoom→findRoomByName again) on the giant Locations tree — the documented giant-DOM wedge query pattern, where each wedged query takes ~90s and the chain blows the 360s method budget.
- Action: Collapse the redundant findRoomByName retries into one bounded scoped query (mobile:scroll predicateString + withImplicitWait(0)); consider API-side room creation (TestDataApi) instead of UI createTestRoom.

### TC_DR_002_verifyRoomCountUpdatesAfterDeletion  —  **AUTOMATION_BUG**
- Why: assertTrue(deleteRoom(roomName), "Room should be deleted successfully") failed; deleteRoom = longPressOnRoom + clickDeleteRoomOption, where longPressOnRoom does a coordinate mobile:touchAndHold at cell center with no behind-tab-bar check (BuildingPage.java:3812) — the tab-bar-zone press bug fixed in f98f5ce AFTER this run and only for AssetEngineerPage — and clickDeleteRoomOption is a single-strategy plain .click() on the 'Delete Room' row (line 4004), subject to the iOS 18.5 click no-op quirk.
- Action: Port the f98f5ce pressCellAboveTabBar/nudgeListUp fix into BuildingPage.longPressOnRoom, verify the context menu actually appeared before returning true, and use a W3C down-120ms-up press for menu rows.

### TC_ER_001_verifyEditRoomOpensWithPrefilledData  —  **AUTOMATION_BUG**
- Why: assertTrue(editClicked, "'Edit Room' option should be clicked") failed even after the built-in retry because longPressOnRoom returns true merely for executing a coordinate touchAndHold (no menu-appeared verification, BuildingPage.java:3822-3825); rooms sit at the bottom of the expanded tree — the behind-tab-bar zone — so the press hits the tab bar and no context menu ever opens (the exact mechanism proven automation-side and fixed for AssetEngineerPage in f98f5ce, dated after this run).
- Action: Port the tab-bar-zone-safe press into BuildingPage.longPressOnRoom and make it return false unless isRoomContextMenuDisplayed() confirms the menu.

### TC_ER_002_verifyRoomNameCanBeUpdated  —  **AUTOMATION_BUG**
- Why: Fails at assertTrue(editClicked, "Should navigate to Edit Room screen") (LocationTest.java:4419) — identical long-press→Edit Room chain as TC_ER_001: unverified coordinate touchAndHold on a bottom-of-list room cell (behind-tab-bar zone, unpatched in BuildingPage) so clickEditRoomOption finds no menu row and returns false. Five ER tests failing at this same step deterministically points at the shared interaction helper, not the app.
- Action: Same fix as TC_ER_001: tab-bar-zone-safe long press with menu-appearance verification, W3C press for the menu row.

### TC_ER_003_verifyFloorAndBuildingFieldsNotEditable  —  **AUTOMATION_BUG**
- Why: Fails at assertTrue(isEditRoomScreenDisplayed(), "Should be on Edit Room screen") (LocationTest.java:4532) after clickEditRoomOption()'s return is ignored (line 4529) — the editor never opened because the shared long-press/context-menu chain silently failed (behind-tab-bar coordinate press, no menu verification), the same root as TC_ER_001/002.
- Action: Assert clickEditRoomOption()'s return before checking the screen, and apply the BuildingPage tab-bar-zone press fix.

### TC_ER_004_verifyAccessNotesCanBeUpdated  —  **AUTOMATION_BUG**
- Why: Fails at assertTrue(editScreenDisplayed, "Edit Room screen should be displayed") (LocationTest.java:4637) after unchecked longPressOnRoom/clickEditRoomOption calls — same shared-helper failure as the rest of the ER cluster: coordinate touchAndHold with no behind-tab-bar guard and no menu-appeared check, so the Edit Room sheet never opens.
- Action: Same fix as ER cluster: tab-bar-zone-safe long press + menu verification in BuildingPage before flagging any app-side regression.

### TC_ER_005_verifyCancelDiscardsRoomEditChanges  —  **AUTOMATION_BUG**
- Why: Fails at assertTrue(isEditRoomScreenDisplayed(), "Should be on Edit Room screen") (LocationTest.java:4759) with clickEditRoomOption()'s result ignored — fifth member of the identical long-press→Edit-Room chain failure; the deterministic 5/5 cluster at one shared helper step is automation-side (behind-tab-bar coordinate press documented and fixed post-run in f98f5ce for another page object).
- Action: Apply the shared BuildingPage long-press/menu fix; rerun the ER block to confirm red→green like TC_ENG_013 did.

### TC_NB_001_verifyNewBuildingScreenUIElements  —  **AUTOMATION_BUG**
- Why: Exactly-6m00s timeout = HUNG signature on the class's very first test (priority 1, so not a cascade victim): navigateToNewBuilding's fallback strategies scan the entire giant Locations tree with broad predicates ('name == plus...' then 'Button AND (label CONTAINS Add OR New)', BuildingPage.java:74-94), the documented giant-DOM WDA-wedge query pattern; this wedge then likely seeded the dead-session breaker that skipped the rerun shard.
- Action: Make the plus-button lookup accessibility-id-first with a 3s bounded wait (withImplicitWait) and drop the whole-tree 'CONTAINS Add/New' scan on the Locations screen.

### TC_NL_001_verifyNoLocationSectionDisplaysInLocationsList  —  **AUTOMATION_BUG**
- Why: Fails at assertTrue(isNoLocationDisplayedFast(), "'No Location' section should be displayed...") after scrollToNoLocationTurbo(), which hard-caps at 5 fast half-screen drags (BuildingPage.java:5168) — deterministically too little travel to reach the section beneath the giant buildings list; it failed fast on the assert (queries returned quickly and found nothing visible), i.e. under-scrolled rather than a fixture with zero unassigned assets, consistent with the whole NL_001-007 block failing identically.
- Action: Use mobile:scroll with predicateString 'label CONTAINS "No Location"' (handles any distance) instead of the 5-drag cap, and verify Locations-screen context first; only if a fixed scroll still finds nothing, re-triage as DATA_FIXTURE.

## LocationTest#2

### TC_NL_002_verifyTappingNoLocationOpensAssetList  —  **AUTOMATION_BUG**
- Why: Fails at buildingPage.scrollToNoLocationTurbo() ('No Location' section should be found). The helper caps at 5 fast drag-scrolls and probes via isNoLocationDisplayedFast(), an unscoped whole-tree predicate whose catch(Exception) returns false — a WDA timeout on the giant expanded tree reads as 'not found'. TC_NL_001 passed with the same probe moments earlier, so the section/fixture existed; earlier RD tests leave buildings/floors expanded (cleanup only taps Done), pushing No Location past 5 scrolls.
- Action: Replace the 5-scroll loop with mobile:scroll to predicate "label CONTAINS 'No Location'" (or collapse expanded buildings first); stop swallowing session exceptions into false so a wedged session fails as ENV, not 'element missing'.

### TC_NL_003_verifyUnassignedAssetsListDisplaysCorrectly  —  **AUTOMATION_BUG**
- Why: Fails at tapOnNoLocationFast() ('Should open No Location screen'). The test captures foundNoLocation from scrollToNoLocationTurbo() but never checks it (LocationTest.java:5969-5974), then tapOnNoLocationFast does a single unscoped findElement and returns false on any exception — tapping an element the failed scroll never brought on-screen. Same root cause as TC_NL_002.
- Action: Assert the scroll result as a precondition (or skipIfPreconditionMissing) before tapping; adopt the mobile:scroll-to-predicate fix shared with TC_NL_002.

### TC_NL_004_verifyTappingAssetOpensAssetDetailsFromNoLocation  —  **AUTOMATION_BUG**
- Why: Identical failure point: scrollToNoLocationTurbo()'s return is discarded (LocationTest.java:6062) and the test fails on tapOnNoLocationFast() 'Should open No Location screen'. The tap helper is a single whole-tree findElement with exception-swallow-to-false, so an off-screen row after the 5-scroll cap deterministically fails here.
- Action: Same fix as TC_NL_003: gate on scroll success and use mobile:scroll-to-predicate; cascade should clear once the shared scroll/probe helper is fixed.

### TC_NL_005_verifySearchInNoLocationScreen  —  **AUTOMATION_BUG**
- Why: Fails on the same unchecked-precondition pattern: scroll result ignored (LocationTest.java:6172), then tapOnNoLocationFast() returns false ('Should open No Location screen'). No evidence of app misbehavior — TC_NL_001 found the section with an asset count in the same run.
- Action: Same shared fix as TC_NL_003/004 (gate on scroll, mobile:scroll-to-predicate, don't swallow dead-session exceptions).

### TC_NL_006_verifyDoneButtonOnNoLocationScreen  —  **AUTOMATION_BUG**
- Why: Fails at tapOnNoLocationFast() ('Should open No Location screen') after ignoring scrollToNoLocationTurbo()'s result (LocationTest.java:6283-6288); same 5-scroll cap + exception-swallow root cause as the rest of the NL block.
- Action: Same shared NL fix; once the scroll/probe helper is corrected this and its siblings should be re-run as one cluster.

### TC_NL_007_verifyNoLocationIsNotEditableOrDeletable  —  **AUTOMATION_BUG**
- Why: Fails at isNoLocationDisplayedFast() (''No Location' section should be visible', LocationTest.java:6344-6345) right after the unchecked scroll. The probe is an unscoped whole-tree findElements with catch(Exception)->false (BuildingPage.java:5136-5150), so both 'off-screen after 5 scrolls' and 'WDA query timed out on giant tree' collapse into the same false.
- Action: Same NL cluster fix; additionally have isNoLocationDisplayedFast rethrow session-dead/timeout errors so a wedged session surfaces as ENV_INFRA instead of a fake assertion failure.

### TC_NR_001_verifyNewRoomScreenUIElements  —  **AUTOMATION_BUG**
- Why: Exactly 360000ms method timeout = the documented HUNG-mid-flow signature, and this test owns the wedging query pattern: expandBuilding -> findFloorByName (BuildingPage.java:3145, iterates ALL XCUIElementTypeButton on the giant Locations tree calling getAttribute per element) -> navigateToNewRoom (BuildingPage.java:3396, whole-tree plus-button scan + areFloorsVisibleUnderBuilding). Each unscoped query can take ~90s on this tree; a few of them exceed the 6m cap. Matches the documented unfixed 'Location New-Floor/New-Room nav' cluster.
- Action: Scope floor/plus-button queries to the building's cell subtree (class-chain), wrap in withImplicitWait(0) + Waits time budgets so the method fails fast instead of hanging, and use mobile:scroll-to-predicate to reach the floor row.

### TC_NR_002_verifyFloorAndBuildingFieldsPrefilledAndReadOnly  —  **AUTOMATION_BUG**
- Why: Same exactly-6m00s hang in the same nav chain (expandBuilding/getFirstFloorEntry/navigateToNewRoom, LocationTest.java:3206-3223) built on unscoped whole-tree findElements + per-element getAttribute loops — the giant-DOM WDA-wedge query pattern itself, not a cascade victim (it hung on its own queries).
- Action: Same fix as TC_NR_001; these two should be the pilot for the scoped-query remap of the New-Room navigation helpers.

### TC_RD_001_verifyRoomDetailScreenUIElements  —  **AUTOMATION_BUG**
- Why: Failed 'Search bar should be visible' after tapOnRoom() 'succeeded' — but tapOnRoom (BuildingPage.java:4240) returns true immediately after room.click() with no verification Room Detail opened. Room rows sit low in the expanded tree where a center-click lands in the translucent tab-bar zone (only fixed in f98f5ce, after this run) or no-ops on iOS 18.5; the unscoped 'label == Done' probe can match outside Room Detail, then the Room-Detail-only search-field probe correctly fails.
- Action: Make tapOnRoom verify Room Detail actually rendered (breadcrumb/nav-bar probe) and retry with a W3C press above the tab-bar zone; re-run on a build containing f98f5ce.

### TC_RD_002_verifyBreadcrumbNavigationDisplaysCorrectly  —  **AUTOMATION_BUG**
- Why: Failed 'Breadcrumb should be displayed in header' via isBreadcrumbDisplayed() (StaticText CONTAINS '>', BuildingPage.java:4303) after an unverified tapOnRoom(). Same mechanism as TC_RD_001: the tap reports true without the Room Detail opening (behind-tab-bar press / click no-op), so the breadcrumb probe runs against the Locations tree. Note TC_RD_001's soft breadcrumb check also warned, so if Room Detail does open on a f98f5ce build and this still fails, escalate to a v1.49 breadcrumb-locator remap.
- Action: Fix tapOnRoom post-tap verification (shared with RD_001/RD_006), then re-run; if breadcrumb is still missing with Room Detail confirmed open, remap the breadcrumb locator for v1.49.

### TC_RD_004_verifyAssetsListInRoom  —  **AUTOMATION_BUG**
- Why: Failed 'At least one asset should be displayed' (assetCount > 0, LocationTest.java:5541) inside the branch where areAssetsDisplayedInRoom() returned TRUE — the two detectors contradict: areAssetsDisplayedInRoom matches any Cell OR any non-Done Button (BuildingPage.java:4450), while getAssetCountInRoom counts ONLY XCUIElementTypeCell (BuildingPage.java:4770). SwiftUI rows in this app render as Button/Other, not Cell, so the count reads 0 even when assets are visible (and the gate passes even on the wrong screen).
- Action: Unify both helpers on one row locator that includes Button/Other SwiftUI rows scoped to the assets list, and scope them to the Room Detail container so they can't pass on the Locations tree.

### TC_RD_006_verifyDoneButtonNavigatesBack  —  **AUTOMATION_BUG**
- Why: Failed 'Room Detail screen should be displayed' where isRoomDetailScreenDisplayed() = isBreadcrumbDisplayed() && isDoneButtonDisplayed() (BuildingPage.java:4262) right after tapOnRoom() returned true — tapOnRoom has no post-tap verification (returns true after click()+sleep, BuildingPage.java:4240-4257), so a behind-tab-bar press or iOS 18.5 click no-op leaves the test on the Locations tree with no breadcrumb. Same cluster as RD_001/RD_002.
- Action: Same tapOnRoom fix (verify navigation, W3C press above tab-bar zone, retry once); re-run the RD trio together on a post-f98f5ce build.

## LocationTest#3

### TC_RL_001_verifyRoomsDisplayUnderFloor  —  **AUTOMATION_BUG**
- Why: Exactly-6m00s TestNG timeout (360000 ms) = hung mid-flow, and the test's helper chain is the giant-DOM wedge originator: findFloorByName Strategy 2 does driver.findElements("type == 'XCUIElementTypeButton'") over the entire nested Locations tree then getAttribute("label") per element, and expandBuilding/expandFloor fall back to a 50-iteration scroll loop re-running those full-tree finds every 5 iterations (code comment at BuildingPage.java:756-763 admits the slow path was ~4 min even after tuning). The rerun SKIP is only the dead-session breaker cascade this test itself triggered, so it adds no independent signal.
- Action: Rewrite BuildingPage tree helpers (findFloorByName, getFirstRoomEntry, areRoomsVisibleUnderFloor, expandBuilding/expandFloor slow paths) to use scoped predicates that include the target name (never bare type=='XCUIElementTypeButton'), wrap in withImplicitWait(0), cap the scroll-retry loop, and prefer mobile: scroll with a name-bearing predicate — per the documented giant-DOM fix direction (scoped class-chain queries, no whole-tree enumeration).

## Asset_Phase6_Test#1

### MCC_AST_03_verifySelectionOfMCCSubtype1000VOrLess  —  **AUTOMATION_BUG**
- Why: Hit the full 360s cap inside selectAssetSubtype(); that method already fail-fasts with VerificationError ('could not select subtype... On-screen option rows:') on a clean locator miss and short-circuits dead sessions, so a 6m00s timeout means individual WDA commands blocked in-flight — the documented giant-DOM subtype-picker wedge (budgets in tapAssetClassItem/changeAssetClassInternal only check between commands, they cannot interrupt a blocking query). clickSelectAssetSubtype Strategy 2 also enumerates ALL XCUIElementTypeButton with per-element getLocation/getAttribute round-trips.
- Action: Bound the picker flow with per-command WDA response timeouts and replace whole-tree button scans in clickSelectAssetSubtype/tapAssetClassItem with single scoped IN-predicates at implicitWait 0 (same fix already proven for the class picker); ensure teardown dismisses/relaunches past an abandoned picker sheet after a timeout.

### MCC_AST_04_verifySelectionOfMCCSubtypeOver1000V  —  **AUTOMATION_BUG**
- Why: Identical shape to MCC_AST_03 (navigate → changeAssetClassToMCC → selectAssetSubtype('Motor Control Equipment (> 1000V)')) and identical 360000ms hang — a deterministic repeat of the same picker-flow WDA wedge, not a one-off sim blip; the VerificationError fail-fast paths in selectAssetSubtype never got to run because the commands themselves were blocking.
- Action: Same fix as MCC_AST_03 (scoped bounded picker queries + per-command WDA timeout); validate both MCC subtype selections in one dispatched asset-phase6 CI job after the fix.

### MCC_OCP_08_verifyExistingNodeListDisplayed  —  **AUTOMATION_BUG**
- Why: Hung 6m inside openLinkExistingNodeScreen(): getOCPCount() enumerates every XCUIElementTypeStaticText then does per-element getLocation() WDA round-trips, and unlinkFirstOCPItem/create-child+save+reopen add more whole-tree queries on the giant MCC+OCP DOM (documented wedge zone). Discriminator: MCC_OCP_07 and MCC_OCP_10, which skip this helper, passed — the hang lives in this helper's unbounded query pattern. Note the test body has no hard asserts at all (if/log only), so hanging is its only failure mode.
- Action: Rewrite getOCPCount/longPressFirstOCPItem/getLinkableAssetsCount as single scoped predicates at implicitWait 0 with wall-clock budgets (mirror the class-picker IN-predicate fix); add a real assert on the node list so the test can fail cleanly.

### MCC_OCP_09_linkExistingNodeSuccessfully  —  **AUTOMATION_BUG**
- Why: Same openLinkExistingNodeScreen() entry plus getLinkableAssetsCount() (another whole-tree button enumeration with per-element getAttribute) — hung at the 360s cap like its three siblings while the non-unlinking OCP tests (07/10) passed, pinning the wedge on this helper's unscoped giant-DOM query loops rather than general infra.
- Action: Apply the scoped/bounded query rewrite to the link-existing-nodes flow; assert selectedCount/link success instead of the current if/log-only body.

### MCC_OCP_11_openLinkExistingNodesScreen  —  **AUTOMATION_BUG**
- Why: Calls the identical openLinkExistingNodeScreen() helper and hung at exactly the 360000ms cap (HUNG-mid-flow signature); the helper's getOCPCount whole-tree StaticText scan + unlink long-press dance on the MCC asset's OCP tree is the shared wedge across all four failing OCP tests.
- Action: Fix the shared helper (scoped queries, budgets); also make isLinkExistingNodesScreenDisplayed a hard assert so the test has a real oracle once it can complete.

### MCC_OCP_12_verifySearchFieldDisplayed  —  **AUTOMATION_BUG**
- Why: Fourth member of the same cluster — openLinkExistingNodeScreen() then search-field probes; hung 6m in the shared helper's unbounded giant-DOM queries (getOCPCount per-element getLocation loop / unlink flow), while tests that reach the OCP section without the unlink/create dance passed in the same run.
- Action: Same shared-helper fix; then assert searchVisible instead of logging it.

### MOT_AST_01_verifyDefaultAssetSubtypeIsNoneForMotor  —  **ENV_INFRA**
- Why: Failed its hard assert ('Asset Subtype field should be visible', line 875) — meaning the session was alive and responsive — immediately after MCC_AST_04 was killed at 360s mid-subtype-picker, leaving the picker sheet up; navigateToEditAssetScreen/openSharedAssetForEditOrFallback and changeAssetClassInternal swallow every nav/class-change failure ('⚠️ Failed to open Asset Class picker... return'), so the probe ran against the abandoned broken screen, not the Edit Asset form. Cascade victim of the preceding wedge, not a v1.49 locator change (the same predicate probe worked for MCC_AST_01/02 earlier in the run).
- Action: Rerun after the picker-wedge fix lands; harden the cascade seam — have navigateToEditAssetScreen hard-fail (VerificationError/skipIfPreconditionMissing) when the Edit screen isn't reached, and add post-timeout recovery (dismiss stray sheet / terminate+activate app) in teardown.

### MOT_AST_03_verifySelectionOfLowVoltageMachine200hpOrLess  —  **AUTOMATION_BUG**
- Why: Same selectAssetSubtype() 360000ms hang as the MCC selection tests — the fail-fast VerificationError path ('option not found' with an on-screen row dump) never fired, so WDA commands were blocking in-flight during the picker interaction on the class-change screen; each selection-shaped test independently re-wedged after teardown recovery, making this deterministic per test shape.
- Action: Covered by the picker-flow bounding/scoping fix; add per-command WDA timeout so a wedged query surfaces as a fast diagnosable failure.

### MOT_AST_04_verifySelectionOfMediumVoltageSynchronousMachine  —  **AUTOMATION_BUG**
- Why: Identical flow and identical 6m00s hang as MOT_AST_03 (selectAssetSubtype('Medium-Voltage Synchronous Machine')) — deterministic repeat of the subtype-picker WDA wedge; the exactly-6m timeout signature means it hung mid-flow rather than failing an assert.
- Action: Same picker-flow fix; heat-and-trial the five subtype-selection tests together in one CI dispatch to confirm the wedge is gone.

### PB_AST_01_verifyDefaultAssetSubtypeIsNoneForPanelboard  —  **ENV_INFRA**
- Why: Same signature as MOT_AST_01: reached its hard assert quickly (live session) but isSelectAssetSubtypeDisplayed() returned false, directly downstream of MOT_AST_04's mid-picker 360s abort; the swallowed-failure navigation (openSharedAssetForEditOrFallback returns instead of throwing, changeAssetClassInternal returns on picker-open failure) let the test assert field visibility on the poisoned screen state.
- Action: Rerun after the wedge fix + post-timeout app-state recovery; convert silent nav/class-change fallbacks to hard precondition failures so cascades stop masquerading as field-visibility bugs.

### REL_AST_01_verifyDefaultAssetSubtypeIsNoneForRelay  —  **ENV_INFRA**
- Why: Third instance of the cascade signature: hard assert 'Asset Subtype field should be visible' (line 1441) failed on a responsive session while every neighbouring assert-free test 'passed' silently — consistent with app state still broken from the earlier 6m picker hangs and the swallow-and-continue navigation helpers, not with a Relay-specific locator or app change (the same subtype-button predicate found the field for MCC earlier in the run).
- Action: Rerun once the picker-wedge and teardown-recovery fixes land; add a verifyOnEditScreen guard before the subtype assert to distinguish 'navigation never landed' from 'field truly missing'.

### REL_AST_03_verifySelectionOfElectromechanicalRelay  —  **AUTOMATION_BUG**
- Why: Same 360000ms hang inside selectAssetSubtype('Electromechanical Relay') as the four other selection tests — the picker interaction (clickSelectAssetSubtype whole-tree button scan + tapAssetClassItem option enumeration) wedged WDA on the class-change screen instead of hitting the built-in fail-fast paths, so commands blocked to the per-test cap.
- Action: Same picker-flow scoping/bounding fix and per-command WDA timeout; include in the post-fix validation dispatch.

## Asset_Phase6_Test#2

### REL_AST_04_verifySelectionOfSolidStateRelay  —  **AUTOMATION_BUG**
- Why: Exactly-360000ms timeout = HUNG in-flight WDA command inside selectAssetSubtype('Solid-State Relay'): clickSelectAssetSubtype Strategy 2 scans the whole tree via findElements(className 'XCUIElementTypeButton') with per-button getLocation/getAttribute, and the failure path's describeVisiblePickerOptions runs an unscoped 'visible == true' whole-tree query — on the giant/bleed-through edit-asset DOM these single commands wedge WDA past every in-process budget (TAP_OPTION_BUDGET_MS/assertClassChangeDeadline only check between commands). This is the trigger test for the whole cascade (REL_AST_01-03 passed just before it).
- Action: Scope subtype-picker queries to the picker sheet element (class-chain under the presented table) instead of whole-tree scans, drop the unscoped 'visible == true' dump in describeVisiblePickerOptions, and add an HTTP-client command read-timeout/watchdog so a wedged in-flight command fails in seconds instead of eating the 360s cap.

### SWB_AST_01_verifyDefaultAssetSubtypeIsNoneForSwitchboard  —  **ENV_INFRA**
- Why: Ran immediately after REL_AST_04's 6-min WDA wedge; isSelectAssetSubtypeDisplayed() -> BasePage.isElementDisplayed catches ALL exceptions and returns false, and navigateToEditAssetScreen/changeAssetClassToSwitchboard swallow their failures silently, so a dead/wedged session surfaces as 'Asset Subtype field should be visible'. The identical assert passed for Relay (REL_AST_01) earlier in the same run, so the locator and the app's subtype field are fine on a healthy session; rerun was a breaker skip.
- Action: Add a session-health gate (verifyAppAlive/guard) at test start so dead-session runs fail as infra not functional, make changeAssetClassInternal throw instead of print-and-return, and re-run after the subtype-picker wedge fix lands.

### SWB_AST_03_verifySelectionOfSwitchgear1000VOrLess  —  **AUTOMATION_BUG**
- Why: Independently re-triggered the same wedge: burned its own full 360s (not a fast dead-session error — selectAssetSubtype even has an isDriverActive fast-fail that never fired, proving a command was blocked in flight) in the identical selectAssetSubtype('Switchgear (<= 1000V)') path with unscoped whole-tree Button scan + 'visible == true' dump on the giant Switchboard edit DOM.
- Action: Same fix as REL_AST_04: scope picker queries to the sheet, remove whole-tree dumps, add command-level read-timeout so the wedge fails fast.

### SWB_AST_04_verifySelectionOfUnitizedSubstationOver1000V  —  **AUTOMATION_BUG**
- Why: Same exactly-360000ms in-flight hang in selectAssetSubtype('Unitized Substation (USS) (> 1000V)') — third consecutive test executing the unscoped clickSelectAssetSubtype/describeVisiblePickerOptions query pattern that wedges WDA on this DOM; in-process budgets cannot interrupt the blocked command.
- Action: Covered by the shared picker-query scoping + command watchdog fix; re-run to confirm the 6m00s signature disappears.

### TRF_AST_01_verifyDefaultAssetSubtypeIsNoneForTransformer  —  **ENV_INFRA**
- Why: Fast assert failure immediately after SWB_AST_04's 6-min hang left the session wedged/dead: isElementDisplayed swallows the session exception into 'false' and the nav/class-change helpers swallow their failures, so the infra death is reported as 'Asset Subtype field should be visible'. Same assert passed for Relay on a healthy session earlier in the run.
- Action: Session-health precondition at test start + un-swallow changeAssetClassInternal failures; re-run post wedge fix.

### TRF_AST_03_verifySelectionOfDryTypeTransformer600VOrLess  —  **AUTOMATION_BUG**
- Why: Burned its own full 360s hung mid-flow in selectAssetSubtype('Dry-Type Transformer (<= 600V)') — the same unscoped whole-tree Button scan / 'visible == true' picker dump wedging WDA on the Transformer edit DOM; the fact it hung rather than fast-failed on isDriverActive shows the command blocked in flight (not a plain dead session).
- Action: Same scoped-query + read-timeout fix as REL_AST_04.

### TRF_AST_04_verifySelectionOfOilFilledTransformer  —  **AUTOMATION_BUG**
- Why: Identical exactly-360000ms hang re-triggered in selectAssetSubtype('Oil-Filled Transformer'); same code path (clickSelectAssetSubtype Strategy 2 whole-tree scan, describeVisiblePickerOptions unscoped dump) documented as the giant-DOM WDA-wedge query pattern.
- Action: Covered by the shared picker-query scoping + command watchdog fix.

### UPS_AST_01_verifyDefaultAssetSubtypeIsNoneForUPS  —  **ENV_INFRA**
- Why: Third instance of the alternating pattern: fast 'Asset Subtype field should be visible' assert fail directly after TRF_AST_04's 6-min wedge — isElementDisplayed(selectAssetSubtypeButton) returns false for ANY exception (BasePage:242 catches Exception -> false) and openSharedAssetForEditOrFallback/changeAssetClassToUPS never throw, so the dead/wedged session masquerades as a missing field.
- Action: Session-health gate at test start (fail as infra with an honest message) and re-run once the subtype-picker wedge is fixed.

### UPS_AST_03_verifySelectionOfHybridUPSSystem  —  **AUTOMATION_BUG**
- Why: Own full-cap 360000ms hang executing selectAssetSubtype('Hybrid UPS System') — same in-flight WDA wedge from the unscoped picker queries on the UPS edit DOM; per-call budgets and the dead-session short-circuit in selectAssetSubtype cannot interrupt a blocked command.
- Action: Same scoped picker-query + HTTP command read-timeout fix; verify with a single local run of UPS_AST_03 before CI.

### UPS_AST_04_verifySelectionOfStaticUPSSystem  —  **AUTOMATION_BUG**
- Why: Sixth identical exactly-6m00s hang in the subtype-selection flow (selectAssetSubtype('Static UPS System')) — deterministic re-trigger of the same unscoped whole-tree query pattern (clickSelectAssetSubtype Strategy 2 + describeVisiblePickerOptions 'visible == true'), matching the documented giant-DOM WDA-wedge mechanism, not a per-test flake.
- Action: Covered by the shared picker-query scoping + command watchdog fix; re-run the full REL/SWB/TRF/UPS subtype block to confirm the alternating hang/assert-fail pattern is gone.

## Asset_Phase2_Test#1

### CB_EAD_12_editInterruptingRating  —  **AUTOMATION_BUG**
- Why: selectInterruptingRating() swallows failure (line 9034 prints '⚠️ Could not select' and returns void; test line 347 ignores it), so a missed pick leaves no pending change and clickSaveChanges finds no 'Save' button in the full DOM → isAssetSavedAfterEdit throws 'Save was never found/clicked'. Rerun hung at exactly 360000ms — Strategy 2 (line 9004) enumerates ALL XCUIElementTypeButton on the giant class-change DOM, the documented WDA-wedge query pattern (this test owns the query; rerun FAILed not SKIPped, so not a breaker cascade).
- Action: Rewrite selectInterruptingRating with bounded/scoped queries (withImplicitWait(0), no full-DOM button enumeration), hard-assert the chip value re-reads as '10 kA' after the pick, and have the test fail fast if the pick did not register before attempting save.

### CB_EAD_15_editVoltage  —  **AUTOMATION_BUG**
- Why: Test line 452 drops editTextField's boolean; editTextField returns false silently (line 10993 'Could not find field') when 'Voltage' doesn't match a TextField/TextView, leaving no dirty state → no Save Changes button exists (app only shows it when dirty, per clickEditSave comment line 8612) → deterministic 'Save was never found/clicked' on both runs. Sibling text-field edits (CB_EAD_13/14/16 Manufacturer/Model/Catalog) passed, so the 'Voltage' locator contract specifically is stale for CB in v1.49 (likely a picker or renamed label).
- Action: Assert editTextField's return value, then remap the CB Voltage field against the live v1.49 DOM (verify element type — may be a dropdown requiring selectDropdownOption, per node-classes gold-spec 'trust names, verify live').

### CB_EAD_18_saveWithNoRequiredFields  —  **AUTOMATION_BUG**
- Why: The test makes zero field edits and relies on the class change to dirty the form, but the shared asset (openSharedAssetForEditOrFallback, cachedCBAssetName) is already Circuit Breaker after earlier CB siblings saved it, so changeAssetClassInternal fast-paths 'Already Circuit Breaker' (line 9563) → no pending change → the app correctly shows no Save button (isSaveChangesButtonVisible comment line 10526: absence is 'NORMAL'). Deterministic on rerun because the asset class persists server-side.
- Action: Create a real dirty state before asserting save (e.g., first change class to a different class, or clear one field), or detect the no-op fast-path and skip with a precondition message instead of asserting a button that legitimately doesn't exist.

### CB_EAD_19_saveWithPartialRequiredFields  —  **AUTOMATION_BUG**
- Why: Line 581 drops editTextField('Manufacturer') return; the identical edit passes in CB_EAD_13, so here the edit silently failed to register (likely state pollution from CB_EAD_18 immediately before it, which ends on a non-dirty edit screen / possible unsaved-state residue) → no dirty state → no Save button in DOM → 'Save was never found/clicked' on both runs. The failure point is our silent-swallow helper plus unverified flow state, not the app refusing to save.
- Action: Assert editTextField's return value and re-read the field value after typing; add a screen-state guard at test start so residue from a prior failed sibling can't silently redirect the flow.

### CB_EAD_23_indicatorsDoNotBlockSave  —  **AUTOMATION_BUG**
- Why: Same structural flaw as CB_EAD_18: no field edit is made (line 699 comment 'Fields remain empty'), the shared asset is already Circuit Breaker so the class change no-ops (line 9563 fast-path), and the app only renders Save Changes when dirty — so clickSaveChanges finds nothing and isAssetSavedAfterEdit throws 'Save was never found/clicked' deterministically. The domain expectation (red indicators don't block save) is valid; the flow never creates a save to attempt.
- Action: Dirty the form first (edit then clear a field, or force a class change to a different class) so a real Save exists to click, then assert the save completes despite red indicators.

### DS_EAD_11_editInterruptingRating  —  **AUTOMATION_BUG**
- Why: Identical mechanism to CB_EAD_12: selectInterruptingRating (line 8974) silently prints '⚠️ Could not select' on failure and the test (line 1103) ignores it → no pending change → 'Save was never found/clicked'. Rerun timed out at exactly 360000ms, the documented HUNG-mid-flow signature — the helper's Strategy 2 full-DOM findElements(XCUIElementTypeButton) (line 9004) is the known giant-DOM WDA-wedge query pattern on the asset edit/class-change screen.
- Action: Same fix as CB_EAD_12: bounded scoped queries in selectInterruptingRating, verify the pick by re-reading the chip, fail fast (VerificationError) if the dropdown interaction did not register.

### DS_EAD_12_editVoltage  —  **AUTOMATION_BUG**
- Why: Original run: editTextField('Voltage') return dropped (line 1138) and edit never registered → no Save button → 'never found/clicked'. Rerun: Save WAS clicked but 'Save Changes' stayed visible — clickSaveChanges (line 8661) sets saveButtonClickedThisFlow=true on click() without verifying the tap landed, and the iOS 18.5 click-silently-no-ops quirk is documented; an un-landed tap leaves the dirty screen exactly like this. App-side validation rejecting the garbage value '240V_<epoch>' is a secondary possibility but unproven (no validation-alert capture exists).
- Action: Assert the edit registered (re-read field), replace click() with a W3C down-120ms-up press for Save, and scan for a validation alert after the click to split app-validation from tap-no-op before escalating to APP_BUG.

### DS_EAD_21_indicatorsDoNotBlockSave  —  **AUTOMATION_BUG**
- Why: Same no-dirty-state flaw as CB_EAD_23: no field is edited, the shared/first asset is already Disconnect Switch after DS siblings saved it, so changeAssetClassToDisconnectSwitch fast-paths 'Already Disconnect Switch' (line 9563) → the app legitimately renders no Save button → clickSaveChanges finds nothing → deterministic 'Save was never found/clicked' on both runs.
- Action: Force a real pending change before the save attempt (field edit or cross-class change), or skip with an explicit precondition when the class-change fast-path no-ops.

### DS_EAD_23_verifySaveButtonBehavior  —  **AUTOMATION_BUG**
- Why: Asserts isSaveChangesButtonVisible() (line 1551/1555) after changeAssetClassToDisconnectSwitch, but the first asset is already DS (persisted by earlier DS tests) so the change no-ops (line 9563) — and the app's contract is that Save Changes exists ONLY with pending changes (isSaveChangesButtonVisible's own comment, line 10526: absence is 'NORMAL'). The helper searches the full DOM with no visibility filter and still found nothing, proving no dirty state rather than a hidden button; the flow fails to establish the precondition the assert depends on.
- Action: Guarantee a real class change before asserting (read current class, change to a DIFFERENT class first, then to Disconnect Switch), or reshape the assert to the actual contract: Save button appears if-and-only-if a pending change exists.

### FUSE_EAD_11_editFuseManufacturer  —  **AUTOMATION_BUG**
- Why: Two failure faces of the same fragile flow: original run the edit registered but 'Save Changes' remained visible after a real click — clickSaveChanges never verifies the tap landed (line 8661 sets the flag on click() alone) and the iOS 18.5 silent click-no-op quirk matches exactly; rerun the 'Fuse Manufacturer' edit never registered at all (editTextField silent false, return dropped at line 1829) → no Save button. Intermittent mode-flipping across runs indicates fragile locator + unverified tap, not deterministic app misbehavior.
- Action: Assert editTextField's return, re-read the field value post-type, use W3C press for the Save tap with a post-click dirty-state re-check, and capture any validation alert text.

### FUSE_EAD_13_editVoltage  —  **AUTOMATION_BUG**
- Why: Deterministic 'Save was never found/clicked' on both runs: editTextField('Voltage') return is dropped (line 1899) and the helper returns false silently (line 10993) when the Fuse Voltage field doesn't match its TextField/TextView locators in v1.49 → no dirty state → no Save button exists in the full DOM. Mirrors CB_EAD_15/DS_EAD_12 — the 'Voltage' locator contract is stale across classes.
- Action: Remap the Voltage field per class against the live v1.49 DOM (check for picker vs text field), and make editTextField failures throw or be asserted by callers instead of silently returning false.

### FUSE_EAD_17_saveWithNoRequiredFields  —  **AUTOMATION_BUG**
- Why: Same structural no-dirty-state flaw as CB_EAD_18: zero field edits (line 2033 goes straight to clickSaveChanges), the shared Fuse asset (cachedFuseAssetName) is already class Fuse so changeAssetClassToFuse no-ops via the 'Already Fuse' fast-path (line 9563) → the app correctly shows no Save button → clickSaveChanges falls through and isAssetSavedAfterEdit throws deterministically on both runs.
- Action: Create a genuine pending change (clear/edit one field) before asserting the save succeeds with empty required fields, or skip explicitly when the class-change fast-path makes the scenario unreachable.

## Asset_Phase2_Test#2

### FUSE_EAD_18_saveWithPartialRequiredFields  —  **AUTOMATION_BUG**
- Why: Test dirties the form only via editTextField("Fuse Amperage", ...) and discards its boolean return (Asset_Phase2_Test.java:2063), but live ground truth (docs/asset-classes-ground-truth-2026-06-22.md:89) shows fuseAmperage is a SELECT picker on Fuse, and every editTextField strategy filters to XCUIElementTypeTextField/TextView (AssetPage.java:10791+) so it silently returns false. Navigation was fine (editTextField's 'not on Edit Asset screen' VerificationError did not fire); with a pristine form the Save Changes button contractually never appears (clickSaveChanges javadoc AssetPage.java:8636), so saveButtonClickedThisFlow stays false and isAssetSavedAfterEdit throws exactly the observed 'Save was never found/clicked' error — deterministic, matching the rerun FAIL.
- Action: Change the dirtying step to a real Fuse textfield (Spare Fuses / Fuse Refill Number / Type) or use the picker helper for Fuse Amperage (select an option like '30A'), and assert editTextField's return value so a silent no-op can't cascade into a misleading save failure.

### FUSE_EAD_22_indicatorsDoNotBlockSave  —  **AUTOMATION_BUG**
- Why: The test makes NO field edit ('Leaving required fields empty', Asset_Phase2_Test.java:2180) and relies solely on changeAssetClassToFuse to dirty the form, but changeAssetClassInternal early-returns 'Already Fuse' (AssetPage.java:9563) because the shared first asset was persisted as Fuse by earlier FUSE_EAD save tests. With zero changes the Save Changes button never exists (documented v1.36+ contract in clickSaveChanges javadoc AssetPage.java:8636), clickSaveChanges finds nothing, and isAssetSavedAfterEdit correctly throws the 'Save was never found/clicked' vacuous-pass guard — deterministic rerun FAIL confirms the asset stays Fuse.
- Action: Keep the valid domain expectation (red indicators don't block save) but fix the flow: dirty a non-required Fuse textfield (e.g. Spare Fuses) while leaving the required select fields empty, then click Save Changes and assert save evidence.

### FUSE_EAD_24_verifySaveButtonBehavior  —  **INVALID_TEST**
- Why: The assert 'Save Changes button should be visible for Fuse' (Asset_Phase2_Test.java:2312) fires after only a class change that no-ops on the already-Fuse first asset, with no field edited. The app contract — documented in our own page object (clickSaveChanges javadoc AssetPage.java:8636 'If asset class didn't change... Save Changes won't appear'; isSaveChangesButtonVisible:10527 treats absence as NORMAL) — is that the Save button exists only when the form has unsaved changes, so the test fails exactly when the app behaves correctly; unconditional button visibility on the Fuse edit screen is not a contract.
- Action: Reshape the test to the real contract: assert the Save Changes button is ABSENT on the pristine edit form, then make a genuine change (edit a Fuse textfield like Spare Fuses) and assert the button APPEARS — a dirty-state round-trip instead of an unconditional visibility assert.

## Asset_Phase5_Test#1

### TC_ATS_ST_01_verifyAssetSubtypeFieldVisibility  —  **AUTOMATION_BUG**
- Why: assertTrue(subtypeVisible) is fed by isSelectAssetSubtypeDisplayed() whose locator is "type == 'XCUIElementTypeButton' AND name CONTAINS 'asset subtype'" (AssetPage.java:72-73) — it only matches the 'Select asset subtype' PLACEHOLDER. Sibling tests ST_09/10 save a subtype on the same shared/first asset, so the button is named the subtype value (e.g. 'Transfer Switch (> 1000V)') and the probe returns false even though the dropdown is on screen; deterministic rerun FAIL matches the persisted fixture state. Also changeAssetClassToATS swallows picker failure (returns void) and ST_01 has no isCurrentAssetClassEqualTo gate like ST_09/10 do.
- Action: Make the visibility probe state-agnostic: match placeholder OR a selected-subtype value (reuse the LIVE_SUBTYPES IN-predicate / isSubtypeSelected dual check), and gate the assert on isCurrentAssetClassEqualTo("ATS").

### TC_ATS_ST_04_selectAutomaticTransferSwitchLow  —  **AUTOMATION_BUG**
- Why: Exactly-360000ms timeout = documented HUNG-mid-flow signature; this is the FIRST test to call selectAssetSubtype() (ST_02/03 don't hard-assert and passed). clickSelectAssetSubtype() Strategy 1's placeholder predicate misses (subtype already saved on the shared asset), then Strategies 2/4 run whole-tree findElements(XCUIElementTypeButton) + per-element getAttribute/getLocation at 5s implicit wait with NO wall-clock budget (unlike changeAssetClassInternal's CLASS_CHANGE_BUDGET_MS) — the giant-DOM WDA-wedge query pattern. Reproduced on a fresh rerun sim (deterministic FAIL).
- Action: Bound clickSelectAssetSubtype like the class-change path: run the fast implicit-wait-0 LIVE_SUBTYPES IN-predicate (current Strategy 3) FIRST, wrap Strategies 2/4 in withImplicitWait(0) with an element cap, and add a VerificationError wall-clock budget.

### TC_ATS_ST_05_selectAutomaticTransferSwitchHigh  —  **AUTOMATION_BUG**
- Why: Identical code path to ST_04 (navigateToATSEditScreen → changeAssetClassToATS → selectAssetSubtype) and identical exactly-6m00s hang in both original run and rerun. The hang is in clickSelectAssetSubtype's unbounded full-button enumeration (Strategies 2/4, AssetPage.java:6248-6380) entered because the placeholder-name predicate no longer matches the saved-subtype button.
- Action: Same fix as ST_04 (reorder/bound clickSelectAssetSubtype strategies + budget); re-run after ST_04's fix lands — one fix clears the whole cluster.

### TC_ATS_ST_06_selectTransferSwitchLow  —  **AUTOMATION_BUG**
- Why: Same selectAssetSubtype() hang path as ST_04/05, exactly-6m00s in both runs (deterministic). The only difference from passing ST_03 is that ST_03 never asserts and doesn't call selectAssetSubtype; every test in this class that calls selectAssetSubtype hit the 360s cap, pinpointing the unbounded button-enumeration strategies in clickSelectAssetSubtype.
- Action: Covered by the clickSelectAssetSubtype bounding fix; verify with a single local run of ST_06 per the one-test-at-a-time loop.

### TC_ATS_ST_07_selectTransferSwitchHigh  —  **AUTOMATION_BUG**
- Why: Original failure is the same exactly-6m00s hang in the same selectAssetSubtype() path as ST_04-06 (which reproduced deterministically on rerun). The rerun SKIP ('Failed to initialize driver: Could not start a new session') is only cascade — 04-06's back-to-back 6m wedges in the same class-atomic rerun shard killed WDA/sim before ST_07 started, so the rerun carries no independent signal.
- Action: Fix clickSelectAssetSubtype bounding (shared root cause); rerun this test on a fresh session to confirm — do not triage the driver-init message as this test's defect.

### TC_ATS_ST_08_verifySwitchingBetweenSubtypeValues  —  **AUTOMATION_BUG**
- Why: Calls selectAssetSubtype() THREE times back-to-back, tripling exposure to the unbounded clickSelectAssetSubtype enumeration — original exactly-6m00s hang is the same mechanism as ST_04-06. Rerun SKIP is dead-session cascade from earlier shard-mates, not per-test signal. Note the test's own assert is log-only (no assertTrue on 'selected'), so the ONLY way it can fail is a hang/crash.
- Action: Apply the shared clickSelectAssetSubtype fix AND add a hard assert on the final isSubtypeSelected() (currently the test can never fail on wrong behavior, only on timeout).

### TC_ATS_ST_09_saveATSAssetWithSubtypeSelected  —  **AUTOMATION_BUG**
- Why: Loops selectAssetSubtype() over up to 4 subtypes (Asset_Phase5_Test.java:1714-1727), each iteration re-entering clickSelectAssetSubtype's slow-path enumeration plus scrollFormUp + isSaveChangesButtonVisible — multiplies the wedge cost x4, guaranteeing the 360s cap once the placeholder predicate misses. Same deterministic hang family as ST_04-06; rerun SKIP (driver init) is shard cascade, not signal. Its tautological assert (assertTrue(!stillOnEdit || saveButtonVisible), line 1738) can never fail, so the timeout is the only failure mode.
- Action: Shared clickSelectAssetSubtype fix; also replace the tautology assertTrue(!stillOnEdit || saveButtonVisible) with a real post-save evidence assert.

### TC_ATS_ST_10_verifySubtypePersistenceAfterSave  —  **AUTOMATION_BUG**
- Why: Same x4 selectAssetSubtype loop as ST_09 (Asset_Phase5_Test.java:1778-1791) → same unbounded clickSelectAssetSubtype enumeration → exactly-6m00s hang in the original run. Rerun SKIP is the dead-session cascade from ST_04-06 wedging the shard. Side effect worth noting: this test's save is what persists a subtype on the shared asset, which is what breaks ST_01's placeholder-only probe on every subsequent run.
- Action: Shared clickSelectAssetSubtype fix; consider resetting the shared asset's subtype to None in cleanup so the placeholder-state tests see a deterministic fixture.

### TC_ATS_ST_12_verifySubtypeDoesNotAutoChangeCoreAttributes  —  **AUTOMATION_BUG**
- Why: Original exactly-6m00s hang; the method calls selectAssetSubtype("Automatic Transfer Switch (> 1000V)") at line 1901 plus extra scrollFormDown/isCoreAttributesSectionVisible passes on the heavy Edit-Asset DOM — same wedge path as ST_04-06 which reproduced deterministically on rerun. Rerun SKIP (driver init failure) is cascade. All of its checks are log-only (no hard assert), so a hang is its only possible failure.
- Action: Shared clickSelectAssetSubtype fix, plus add a real assert (e.g. assertEquals on core-attribute field set before/after subtype change) so the test can fail meaningfully.

### TC_ATS_ST_13_verifyCancelBehaviorAfterSubtypeChange  —  **AUTOMATION_BUG**
- Why: Original run: its own exactly-6m00s hang inside selectAssetSubtype("Transfer Switch (> 1000V)") (line 1937) — the same deterministic clickSelectAssetSubtype enumeration wedge proven by ST_04-06's rerun FAILs. The rerun 'Dead-session circuit breaker OPEN' SKIP is by definition cascade (breaker opened after 5 dead-session tests earlier in the shard) and carries no per-test signal.
- Action: Shared clickSelectAssetSubtype fix; re-run on a fresh session. Also add a hard assert on cancel outcome (currently log-only, timeout is its only failure mode).

## ZP323_NewFeatures_Test#1

### TC_ZP323_06_03_verifyNoShortcutsPlaceholderForUnsupportedClass  —  **AUTOMATION_BUG**
- Why: Unlike siblings 06_01/06_02 this test never guards on isSuggestedShortcutsSectionVisible(); it asserts 'noShortcuts || (value != null && !value.isEmpty())' where getSuggestedShortcutsValue() returns "" and isNoShortcutsPlaceholderShown() returns false on ANY exception (AssetPage.java:12103/12113, single-strategy predicates 'Button label CONTAINS shortcut' / 'StaticText CONTAINS No shortcuts', web-derived, never iOS-verified). A class with no shortcuts section at all — or an Edit screen that never opened — collapses both probes to false and fails a legitimate state. Rerun SKIP is breaker cascade, not signal.
- Action: Add skipIfPreconditionMissing(isSuggestedShortcutsSectionVisible()) first (matching 06_01/06_02), verify Edit actually opened after clickEditTurbo, and widen the value locator beyond Button-with-'shortcut' (multi-strategy per repo rule); capture a DOM dump on failure to learn the v1.49 placeholder text.

### TC_ZP323_07_02_verifyCOMValueIsParseable  —  **AUTOMATION_BUG**
- Why: The skip-guard isCOMVisibleOnAssetDetails() passed (COM label IS rendered), yet getCOMValue() (AssetPage.java:12170) returned null: it anchors on 'label CONTAINS[c] COM' — which also matches 'Recommended'/'Complete' etc. — then demands a purely numeric StaticText within ±60px of that anchor; javadoc admits it was inferred from web v1.31, never verified on iOS. COM visible + heuristic returning null points at the geometry/regex probe missing the real rendering (badge button, combined 'COM: 2' label, or non-numeric placeholder), not at the app.
- Action: Rewrite getCOMValue: anchor strictly on 'Condition of Maintenance' label, read sibling value via scoped class-chain (include Button/Other types), accept 'COM: N' combined labels, log all nearby texts on miss; treat a legit '-'/'N/A' unscored value as skip, not fail.

### TC_ZP323_07_03_verifyCOMConsistentAcrossNavigations  —  **AUTOMATION_BUG**
- Why: Same root cause as 07_02 — getCOMValue() returned null on the first read — plus a second defect in the test itself: com1 is fed unguarded into assertEquals(com2, com1), producing the raw NPE 'Cannot invoke Object.equals(Object) because "expected" is null' instead of a clean failure. The test never actually compared two COM readings.
- Action: Fix getCOMValue as in 07_02, then add skipIfPreconditionMissing/assertNotNull on com1 before the equality assert so a read-miss reports as a detection failure rather than an NPE.

### TC_ZP323_13_01_verifyAIExtractButtonPresent  —  **AUTOMATION_BUG**
- Why: The flow never uploads a nameplate: openNameplateGalleryPicker() (AssetPage.java:12529) only opens the native Photos picker — no photo is selected and nothing dismisses the picker before clickEditTurbo() runs — yet the test hard-asserts 'in-progress indicator OR suggestions panel' after one 400ms wait, contradicting the class's own comment 'Without a nameplate photo, AI Extract has nothing to read'. Detection is also blind: isAIExtractionInProgress() requires the spinner to carry an 'extracting/analyzing/processing' label (unlabeled ActivityIndicators never match), and a 'no nameplate' alert would be swallowed by autoAcceptAlerts before any check sees it.
- Action: Complete the real flow (select a photo via simctl addmedia + picker selection, dismiss picker) or downgrade the assert to button-presence when no photo was uploaded; wrap the post-tap window in withAlertsManual-style detection and match unlabeled ActivityIndicator / any suggestions sheet, not four guessed label strings.

### TC_ZP323_13_02_verifyAIExtractionShowsProgress  —  **AUTOMATION_BUG**
- Why: Same broken preconditions as 13_01 (gallery picker opened but no photo ever picked, picker left covering the app when clickEditTurbo fires), and the 2s poll can only succeed if the spinner has a label matching 'extracting|analyzing|processing' or a StaticText matches one of four web-guessed suggestion strings (AssetPage.java:12404/12446) — a plain unlabeled iOS ActivityIndicator or a differently-worded v1.49 sheet is invisible to it. The assert therefore fails even when extraction genuinely starts.
- Action: Detect progress structurally (any XCUIElementTypeActivityIndicator, sheet/modal appearance, or Core Attributes field-value change) with a longer alert-race-safe window; only assert progress when a nameplate photo is confirmed uploaded (getNameplatePhotoCount() > before), else skip as fixture-missing.

### TC_ZP323_15_02_verifyTapScheduleOpensDatePicker  —  **AUTOMATION_BUG**
- Why: The test performs no navigation — it depends on a prior test leaving Work Order Details open — and tapScheduleField() (WorkOrderPage.java:24021) returns true after clicking ANY Button/Cell/StaticText/Other whose label merely CONTAINS 'schedule' on whatever screen is current, so tapped=true does not prove the real Schedule row was hit (and the documented iOS 18.5 click() silent no-op applies). isDatePickerDisplayed() (24036) only accepts native XCUIElementTypeDatePicker/Picker types, per a javadoc that says the iOS app 'likely' mirrors the web — a custom SwiftUI calendar sheet would never match.
- Action: Navigate deterministically to a known Work Order's details inside the test, tighten the tap locator to the Schedule row (verify tap effect by re-reading screen state, W3C press fallback), and widen picker detection to include a date-sheet signature (Done/Confirm buttons, month/day wheels, 'Select date' text) alongside the native DatePicker type.

## WorkOrderPlanning_Test#1

### TC_WOP_001_verifyWorkOrdersScreenReachable  —  **ENV_INFRA**
- Why: Exactly-360000ms TestNG timeout = the documented HUNG-mid-flow signature (wedged WDA), and the test is the class opener that only does loginAndSelectSite + navigateToWorkOrdersScreen; every helper it calls (e.g. waitForWorkOrdersScreen, WorkOrderPage.java:128, a bounded 10s poll that returns false) is time-bounded, so the 6-minute hang happened below test code in the driver/WDA layer. Rerun SKIP = dead-session breaker cascade, no deterministic signal.
- Action: Pull the CI job log for run 29135128275 around TC_WOP_001 to find which WDA command stalled (~90s pattern) and what screen the noReset=true session inherited from the prior suite; then rerun the workorder-planning-only job on a fresh sim — expect green, WOP screens are documented wedge-healthy.

### TC_WOP_004_readPlanList  —  **ENV_INFRA**
- Why: Failure is a raw io.appium IOSDriver command java.util.concurrent.TimeoutException (WDA HTTP read timeout), not the test's own assertion ('First plan entry must render a non-empty name' never appears); the precondition gate waitForWorkOrdersScreen catches exceptions and returns false→SKIP, so the timeout escaped a non-swallowing verify-layer call (verifyNotBlank/guard) on the session wedged since TC_WOP_001.
- Action: No test-code change needed; rerun in the failed-suite on a healthy session. Optionally add verifyAppAlive at test entry so a wedged session fails with an explicit session-dead message instead of a bare command timeout.

### TC_WOP_009_deactivatePlan  —  **ENV_INFRA**
- Why: assertTrue(deactivated) failed because deactivateActiveJob() (WorkOrderPage.java:13125) wraps all 4 locator strategies in catch(Exception){continue} and returns false — on the wedged session (sibling tests 004 before and 015 after died on raw WDA command timeouts) every findElements throws and is swallowed, so infra death was converted into this functional-looking message. The END-button contract (Strategy 0, the prior TC_WOP_009 fix) has no evidence of a v1.49 change, and rerun SKIP was breaker cascade.
- Action: Rerun on a healthy session to confirm; harden deactivateActiveJob to rethrow dead-session/command-timeout errors (or call guard()/verifyAppAlive before the assert) so a wedged session can't masquerade as 'plan not deactivatable'.

### TC_WOP_015_planListIntegrityAcrossRoundTrip  —  **ENV_INFRA**
- Why: Same raw IOSDriver command TimeoutException as TC_WOP_004 — the test died before reaching StateIntegrityChecker.assertNoLossOrDup or the size invariant (their distinct messages are absent), so this is the wedged-WDA session cascade from TC_WOP_001, not an integrity-check or locator defect. Rerun SKIP = breaker open.
- Action: Rerun with the failed-suite shard on a fresh sim; no code change to the integrity check. Treat all four WOP failures as one session-death event and root-cause the wedge origin in the job log (what the noReset session was left on before this class started).

## Completion pass (2026-07-13, inline): the 23 tests the rate-limited agents missed

### ArcFlash_Test.TC_AF_002_dashboardOpensWithTitleAndDone  —  **AUTOMATION_BUG**
- Why: Deterministic Selenium TimeoutException (3m41s) inside the dashboard open/Done flow — a WebDriverWait in openDashboard/tapDone exceeds its budget on the grown QA site's slow first dashboard load; the suite's later tests pass once warm.
- Action: Bound the dashboard-open waits per step (Waits.until + explicit budget), retry Done once after a settle; revalidate on current main.

### ArcFlash_Test.TC_AF_014_bucketRowDrillsIntoEditorAndBack  —  **AUTOMATION_BUG**
- Why: 'Asset row tap must open the asset editor full-screen' deterministic on pre-f98f5ce code — the per-asset row press is the bottom-zone/tab-bar coordinate-press family (visible-behind-chrome rect).
- Action: Already addressed by the tab-bar-safe press batch (changelog 116/118); revalidate in the dispatched arc-flash run on current main.

### Asset_Phase1_Test.ATS_EAD_06_enableRequiredFieldsOnlyToggle  —  **INVALID_TEST**
- Why: Hard-asserted the switch's accessibility value; per user-confirmed domain truth the toggle is a view filter for arc-flash reading, not a contract.
- Action: FIXED in changelog 116 (reshaped to form-functional contract, state read informational).

### Asset_Phase1_Test.ATS_EAD_14_disableRequiredFieldsToggle  —  **INVALID_TEST**
- Why: Same as ATS_EAD_06 — round-trip state read is not a product contract.
- Action: FIXED in changelog 116.

### Asset_Phase1_Test.ATS_ECR_31_verifySaveAssetWithValidData  —  **AUTOMATION_BUG**
- Why: Exact-360s hang in the create-with-valid-data flow — save-flow hang family: post-save the flow re-queries the grown Assets list unbounded.
- Action: Bound post-save verification (search-first instead of list scan); same family as the save-evidence cluster.

### Asset_Phase3_Test.LC_EAD_02_verifyCoreAttributesSection  —  **AUTOMATION_BUG**
- Why: Failure screenshot shows Asset Details scrolled deep (Issues/Connections/Notes) — the Core Attributes probe only scrolls DOWN, so a section already above the viewport is never found. Down-only-scroll family (same as swipeToEngineeringSection fix).
- Action: Make the Core Attributes visibility probe bidirectional (scroll up 2-3 then down).

### Asset_Phase3_Test.LC_EAD_05_verifyRequiredFieldsCount  —  **AUTOMATION_BUG**
- Why: Same screen/probe family as LC_EAD_02 — the required-fields counter sits in the Core Attributes header above the current scroll position.
- Action: Same bidirectional-scroll fix; counter itself is a valid UI contract (arc-flash readiness count).

### AuthenticationTest.TC26_verifyEmailFieldClears  —  **AUTOMATION_BUG**
- Why: Orig/rerun messages are the SAME two errors swapped (Continue not clickable vs no TextField) — order-dependent start state: after TC25 logs in, the app is on Dashboard while TC26 assumes the login screen; relaunch with noReset keeps the session.
- Action: Make TC26/TC37 setup force logout or fresh-install state before asserting login-screen fields.

### AuthenticationTest.TC37_verifySessionSecurityAfterLogin  —  **AUTOMATION_BUG**
- Why: Same order-dependent state assumption as TC26 (mirror-image failure messages across runs prove it).
- Action: Same state-reset fix.

### Connections_Test.TC_CONN_024_verifyChangingSourceNodeSelection  —  **AUTOMATION_BUG**
- Why: Exact-360s hang re-opening the Source Node dropdown — the dropdown lists EVERY asset on the grown QA site and the helper scroll-scans it; sibling tests 020-023 passed but each took 4-5 min (RECOVERED on rerun).
- Action: Type-to-filter in the dropdown search FIRST, then pick from the narrowed list; never scroll-scan the full asset list.

### Connections_Test.TC_CONN_036_verifyCreateButtonEnabledAfterAllFieldsFilled  —  **AUTOMATION_BUG**
- Why: 'Target Node dropdown' open failed then rerun hung 360s — same full-asset-list dropdown scan family.
- Action: Same type-to-filter fix.

### Connections_Test.TC_CONN_059_verifyKeyboardDismissOnSelection  —  **AUTOMATION_BUG**
- Why: Exact-360s hang in the same dropdown flow (keyboard + list scan).
- Action: Same type-to-filter fix + dismissKeyboard before selection assert.

### Connections_Test.TC_CONN_062_verifyRapidMultipleConnectionCreation  —  **AUTOMATION_BUG**
- Why: Exact-360s hang — creates MULTIPLE connections back-to-back, each paying the full-list dropdown cost; budget arithmetic alone exceeds the cap on the grown site.
- Action: Type-to-filter + reduce iterations or raise explicit timeOut.

### Connections_Test.TC_CONN_097_verifyCoreAttributesSectionVisibleOnNewConnectionForm  —  **INVALID_TEST**
- Why: Failure screenshot: the fresh New Connection form by DESIGN shows only CONNECTION DETAILS; Core Attributes render only after a connection type is picked — and TC_CONN_098 (assert-after-type-selected) PASSES. The expectation is stale.
- Action: Reshape 097 to assert Core Attributes ABSENT pre-type-selection and present after (or fold into 098).

### E2E_OfflineSyncIntegrity_Test.TC_E2E_001_offlineMultiFieldEditSurvivesSyncReplay  —  **ENV_INFRA**
- Why: 'Error communicating with the remote browser. It may have died' — WDA/session death mid-replay, not an assertion outcome; not re-proven on rerun (skip).
- Action: Rerun on a fresh sim; if reproducible, split the long replay into bounded phases.

### E2E_OfflineSyncIntegrity_Test.TC_E2E_002_crossSiteOfflineEditsLandOnCorrectSite  —  **AUTOMATION_BUG**
- Why: FALSE POSITIVE: the 'leak' it counted on Site B is the search empty-state label "No Results for '_A_...'" — the assertion counts any element containing the marker, and the echo of the query text matches. The app correctly returned zero results (no corruption).
- Action: Exclude the 'No Results' empty-state label from the leak count (name BEGINSWITH 'No Results' filter).

### Issue_Phase2_Test.TC_ISS_130_verifyNoiseExcessiveOption  —  **AUTOMATION_BUG**
- Why: Exact-360s hang (7m9s) in the OSHA subcategory sheet — v1.48 Issues DOM regression family: option list reads 0 items and the helper grinds retries on the giant sheet DOM.
- Action: Covered by the Issues locator remap (open burn-down).

### Issue_Phase3_Test.TC_ISS_183_verifyUltrasonicSimilarToRepairNeeded  —  **AUTOMATION_BUG**
- Why: Exact-360s hang comparing class field sets — Issues details DOM regression family (pickers/lists unreadable, helper retries).
- Action: Issues remap burn-down.

### Issue_Phase3_Test.TC_ISS_189_verifyInProgressBadgeOnIssueEntry  —  **AUTOMATION_BUG**
- Why: Exact-360s hang scanning the issues list for a badge — unbounded list scan on grown data + regression family.
- Action: Scope the badge query to the first matching cell; Issues remap.

### Issue_Phase3_Test.TC_ISS_230_verifySortByTitle  —  **AUTOMATION_BUG**
- Why: Exact-360s hang reading the full sorted list to verify order — unbounded whole-list read on grown data (6m27s).
- Action: Verify sort on the first N visible cells only.

### OfflineSyncMultiSite_Test.UC1_singleUserMultipleSites_dataIntegrity  —  **AUTOMATION_BUG**
- Why: Exact-360s hang — multi-site switch flow pays the large-site load wait (up to 120s dashboard wait) MULTIPLE times per test; budget arithmetic exceeds the 6-min cap on the grown QA sites.
- Action: Give multi-site UCs explicit timeOut (like TC_ENG's 780s) or trim per-switch waits.

### OfflineSyncMultiSite_Test.UC3_multiSiteDataCoexistence  —  **AUTOMATION_BUG**
- Why: Same multi-switch budget arithmetic as UC1 (UC12/UC14 with similar flows pass at 3-6 min — just under the cap).
- Action: Same explicit-timeOut fix.

### SiteSelectionTest.TC_SS_039_verifyLargeSiteLoadsWithinReasonableTime  —  **AUTOMATION_BUG**
- Why: 'Search bar not found with any strategy' — after loading the large site the test expects to be back on the Select Site picker but the app is on the Dashboard; navigation-state assumption, and NOT re-proven (rerun skip).
- Action: Reopen the site picker explicitly (Sites button) before the search-bar probe.
