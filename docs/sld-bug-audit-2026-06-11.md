# SLD ("SLDv3") Bug Audit — 2026-06-11

Source-level audit of the SLD diagram stack in `app-source/` (the app under
test). Naming note: there is no literal `SLDv3` identifier in the code; the
current generation is the **SLDV2 entity model + per-SLD Views layer
(`SLDViewV2`/`SLDLinkV2`/`Mapping*SLDView`) + React Flow web viewer** rendered
via `WebViewBridge`, with detail screens `EditNodeDetailViewV3` /
`EditEdgeDetailViewV3` (`WebAppContainerViewWithView.swift:343,356`). That
stack is what was audited. Cross-checked against `BUGS.md`, `ready-bug/`,
`security/` — none of these were previously filed.

## Architecture (data flow)

- **API → cache:** `SLDSyncService.upsertAllData`
  (`Core/Singletons/SLDSyncService.swift:275`) fetches the SLD DTO +
  class/enum bundles concurrently, upserts into SwiftData (`NodeV2`, `EdgeV2`,
  `SLDViewV2`, `SLDLinkV2`, `MappingNodeSLDView`, `MappingEdgeSLDView`).
  Site switch: `SLDService.switchToSLD` → `clearSLDs` → re-sync;
  `SLDLoadingView` shows progress.
- **Cache → web view:** `SLDViewSelectorView` (gates the SLD tab; "All
  Assets" or a specific view) → `SLDWebViewContainer` →
  `WebAppContainerViewWithView` (filters nodes/edges by view mappings) →
  `WebViewBridge` (WKWebView). The web view loads the bundled/OTA React Flow
  app over a custom `app://` scheme (`AppRootSchemeHandler` in
  `PlainWKWebVIew.swift`; OTA assets from `SldViewerAssetManager` take
  priority). On `didFinish`, Swift injects a pre-serialized JSON graph via
  `evaluateJavaScript` (`initialData` CustomEvent); later refreshes call
  `window.updateData(json)` through the static weak `WebViewBridge.sharedWebView`.
- **Web view → native:** 4 script message handlers (`nodeClicked`,
  `nodePositionChanged`, `nodeParentChanged`, `graphUpdate` ~14 subtypes).
  Native persists to SwiftData, then calls the API (online) or enqueues
  `SyncOp`s (offline) replayed by `SyncExecutionService`.
- **Electrical connection model:** `EdgeV2` with `source`/`target` node UUIDs
  + `sourceNodeTerminalId`/`targetNodeTerminalId`; `SLDLinkV2.edge_direction`
  for inter-view links.

## Confirmed bugs

### SLD-1 (HIGH) — View→view link navigation renders a stale diagram
`Bridges/WebViewBridge.swift:308` — `updateUIView` is **empty**, and all graph
data + callbacks are frozen in `makeCoordinator()` (lines 237–264).
`Views/SLD/SLDViewSelectorView.swift:122–131`: `onNavigateToView` only does
`selectedView = targetView` ("the view will automatically update" — it
doesn't). View→view navigation stays in the same `fullScreenCover` branch, so
SwiftUI keeps the same `WebViewBridge` identity (no `.id()` anywhere in the
chain), `makeUIView` is not re-invoked, and nothing calls `updateGraph`.
**User impact:** tapping an inter-view link shows the new view's title over
the **old view's nodes/edges**; the Coordinator's frozen closures still
capture the old `selectedViewId`, so dragging a node then writes the position
into the **wrong view's mapping** (silent data corruption).
**Repro:** SLD → View A (with link to View B) → tap link node; compare with
Back → View B.

### SLD-2 (HIGH) — Offline enclosure resize silently lost on sync replay
`Core/Services/NetworkServices/SyncExecutionService.swift:1110–1117`: the
offline replay calls `api.updateNodePositionInView(viewId:nodeId:x:y:)`
**without width/height**, although the queued item carries them
(`SyncQueueService.swift:1266–1293`), the API supports them
(`APIClient.swift:3302`), and the online path sends them
(`WebAppContainerViewWithView.swift:730–737`). Also `x ?? 0` / `y ?? 0` can
teleport a node to the origin if the queued mapping has nil coordinates.
**User impact:** resize an enclosure while offline → looks right locally,
server keeps old size; next refresh reverts the device too.
**Repro:** airplane mode → resize enclosure in a view → reconnect → flush
queue → fetch view from web app.

### SLD-3 (HIGH) — Site switch never deletes SLD view mappings
`Core/Services/BackgroundImporter.swift:1006–1071` (`deleteAllEntities`) and
`SLDService.clearSLDsOnMainContext` (`SLDService.swift:432–490`) delete
`SLDLinkV2`, `SLDViewV2`, `SLDV2`, nodes, edges, terminals — but **never
`MappingNodeSLDView` / `MappingEdgeSLDView`**. Re-sync upserts by mapping id
and never tombstones absent rows (`SLDSyncService.swift:4454–4509`). The code
already observes the symptom: `WebAppContainerViewWithView.swift:777` — "Find
ALL matching non-deleted mappings (there may be duplicates)".
**User impact:** mapping rows grow forever across site switches; with
duplicates, `updateNodeViewMapping` writes to `.first(...)` (line 601) while
rendering is last-write-wins over all duplicates — nodes can snap back to
stale positions after a move; collapse state flip-flops.

### SLD-4 (MEDIUM) — Rejected connections leave a ghost edge on the canvas
`WebAppContainerViewWithView.swift:273–305` (`onEdgeCreated`): same-node and
duplicate rejections alert and `return` **without** pushing a corrective
`updateWebViewGraph()`; React Flow drew the edge optimistically, so the user
sees a connection that exists nowhere (gone on reload — silent data loss).
Also: duplicate check is direction-sensitive only (reverse B→A duplicate is
allowed) and ignores terminals (legitimate parallel feeders between the same
two assets are blocked).

### SLD-5 (MEDIUM) — Full unfiltered graph pushed into a view-filtered web view
`WebViewBridge.updateGraphFromSLD` (`WebViewBridge.swift:892–913`) builds the
DTO with no view filtering / position overrides and pushes it to the shared
web view. Reachable while a specific view is on screen from
`EdgeService.swift:39,170`, `NodeService.swift:576,915,993` (via
`EditNodeDetailViewV3`), `SLDService.refreshSLD:162`. Mid-edit the user sees
**every node of the site** at master positions inside their curated view
(the container's own "prevent flash" comments at
`WebAppContainerViewWithView.swift:336–349` acknowledge the symptom).

### SLD-6 (MEDIUM) — Web-view search trigger only works in English
`WebViewBridge.swift:504` hooks
`document.querySelector('input[placeholder*="Search"]')`, but the viewer is
localized (`app://index.html#lng=…`). For non-English locales the
placeholder doesn't contain "Search" → the iOS-keyboard search workaround
silently dies.

### SLD-7 (MEDIUM, security) — Agent web app calls a hardcoded, unauthenticated endpoint
`app-source/Agent/assets/index-DsK7eqm2.js`:
`const og="https://bqitb0ghqa.execute-api.us-east-2.amazonaws.com"; fetch(`
`${og}/api/sessions`, {method:"POST", headers:{"Content-Type":"application/json"}})`
— no Authorization header, no tenant scoping; native injects only `sldId`
(`AgentWebViewBridge.swift:151–168`). Anyone extracting the IPA can create
sessions / query that backend for arbitrary `sldId`s. Also bypasses
`CompanyConfigService` base-URL selection (env-mismatch risk).

### SLD-8 (LOW, security) — `webView.isInspectable = true` in production
`WebViewBridge.swift:300`, `AgentWebViewBridge.swift:111`,
`PlainWKWebVIew.swift:117` — Safari Web Inspector access to diagram data and
the JS bridge on release builds (iOS 16.4+). Should be `#if DEBUG`.

## Suspicions (need device verification)

- **S1 (M)** `initialData` fired on a fixed `setTimeout(…,100)` after
  `didFinish` (`WebViewBridge.swift:454–471`); if React hydration takes
  longer, the event is missed → **blank diagram**, no retry. Same block
  interpolates `initialSLDJSON` **three times** into the JS (payload ×3 for
  large sites).
- **S2 (M)** `updateGraph` calls `window.updateData(...)` while the page may
  still be loading → "not a function", update silently dropped
  (`WebViewBridge.swift:43–63`).
- **S3 (M)** OTA asset version skew: `AppRootSchemeHandler` resolves files
  independently (downloaded-first, bundle fallback) and
  `SldViewerAssetManager.updateIndexHtmlPaths` regex-rewrites only the FIRST
  js/css reference (`SldViewerAssetManager.swift:348–399`) → mixed
  OTA/bundled chunks after app updates.
- **S4 (M)** Offline-created view mappings replayed without client id →
  server assigns new id → next sync inserts a duplicate row (feeds SLD-3).
- **S5 (L)** Cancel on delete confirmations never pushes a graph update —
  element stays visually deleted (`WebAppContainerViewWithView.swift:364–384`).
- **S6 (L)** `@State pendingNodeAPITasks` mutated from non-MainActor Task
  continuations (`WebAppContainerViewWithView.swift:427–451`).
- **S7 (L)** Custom edge routing drops point dicts missing `active`
  (`WebViewBridge.swift:765–810`).

## Untested SLD behaviors (automation gaps)

Only `TC_CONN_054/055` touch the SLD and both are assert-free;
`ConnectionsPage.isSLDDiagramDisplayed` (:5284) is a tautology (any ScrollView
with >10 elements passes) and `navigateToSLDTab` (:5238) targets a tab that
doesn't match the real flow (SLD is gated behind the view selector).

Gaps, each mapping to a bug above: view selector & All-Assets mode;
view→view link navigation (SLD-1); per-view node drag persistence; enclosure
resize online/offline (SLD-2); collapse state; mapping integrity across site
switches (SLD-3); duplicate/same-node connection alerts + canvas consistency
(SLD-4); edit-node-over-view filtering (SLD-5); webview bridge contract
(initialData/updateData renders non-blank — an `AssetLoadVerifier`-style
in-webview check would catch S1/S2); OTA viewer update path (S3); non-English
behavior (SLD-6); view form offline queueing.
