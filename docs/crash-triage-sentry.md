# Production Crash Triage — Z‑Platform iOS (Sentry)

**Source:** Sentry org `egalvanic-yb`, project `z-platform`
**Pulled:** 2026-05-31 (live production data)
**Scope:** Critical *crashing* bugs — unhandled fatal events (`error.handled:false`), plus one
high‑volume data‑loss bug in the offline‑sync path that maps directly to this suite.

> Dashboard: https://egalvanic-yb.sentry.io/issues/?project=z-platform&query=is%3Aunresolved+error.handled%3Afalse
>
> **Note on PII:** Sentry attaches affected‑user emails/IDs to each event. Those are intentionally
> **omitted** here — issues are referenced by Sentry short‑ID + link only. Open the link to see affected users.
>
> **Note on fixes:** Root causes below are hypotheses from stack traces + event context. Confirming /
> fixing them at file:line requires the app's **Swift source** (not in this repo; the build in
> `apps/Z-Platform-QA.zip` is a compiled binary and can't be decompiled to source). Seer AI autofix is
> disabled on this Sentry plan (`402 No budget`), so analysis here is manual.

---

## TL;DR — what's actually killing the app

| Rank | Issue | Crash | Users | Events | Last seen | Type |
|------|-------|-------|------:|-------:|-----------|------|
| 1 | [Z-PLATFORM-5S](https://egalvanic-yb.sentry.io/issues/Z-PLATFORM-5S) | SwiftData fatal — resolve future for to‑many relationship (`NodeV2`) | 41 | 306 | active (2d) | Data layer |
| 2 | [Z-PLATFORM-7](https://egalvanic-yb.sentry.io/issues/Z-PLATFORM-7) | WatchdogTermination — OS killed app for RAM overuse | 36 | 132 | 4d | Memory |
| 3 | [Z-PLATFORM-1P](https://egalvanic-yb.sentry.io/issues/Z-PLATFORM-1P) | EXC_BAD_ACCESS — `_willMoveToWindow → setViewTraversalMark:` | 21 | 73 | active (2d) | UIKit lifecycle |
| 4 | [Z-PLATFORM-6P](https://egalvanic-yb.sentry.io/issues/Z-PLATFORM-6P) | EXC_BAD_ACCESS — `node` | 14 | 42 | active (2d) | Memory safety |
| 5 | [Z-PLATFORM-8P](https://egalvanic-yb.sentry.io/issues/Z-PLATFORM-8P) | SIGTRAP — "Duplicate values for key" (UUID) | 13 | 50 | 2026-03 | Swift logic |
| 6 | [Z-PLATFORM-5D](https://egalvanic-yb.sentry.io/issues/Z-PLATFORM-5D) / [6A](https://egalvanic-yb.sentry.io/issues/Z-PLATFORM-6A) / [A4](https://egalvanic-yb.sentry.io/issues/Z-PLATFORM-A4) | SIGABRT — "pointer being freed was not allocated" | 7/5/3 | 27/25/5 | active (2d) | Memory safety (double free) |
| 7 | [Z-PLATFORM-8M](https://egalvanic-yb.sentry.io/issues/Z-PLATFORM-8M) | SIGSEGV — `node` (Seer actionability: **medium**) | 6 | 9 | 2026-03 | Memory safety |
| 8 | [Z-PLATFORM-CD](https://egalvanic-yb.sentry.io/issues/Z-PLATFORM-CD) | NSRangeException — `objectAtIndex: index 1 beyond bounds [0 .. 0]` | 2 | 2 | 2026-04 | Bounds |
| — | [Z-PLATFORM-DE](https://egalvanic-yb.sentry.io/issues/Z-PLATFORM-DE) | `SyncError.fileNotFound` — pending photo gone before upload | 13 | **2359** | 2026-05 | Data loss (handled) |

Smaller siblings of the above: `AC`, `BB`, `B9`, `B0`, `B4` (more EXC_BAD_ACCESS / EXC_BREAKPOINT, 2–8 users each).

---

## ✅ Confirmed root causes — source-verified (2026-06-01)

With `app-source/` now vendored into this repo, the top three are confirmed against the **actual code**, not inferred from stack traces.

### 5S — background `ModelContext` objects escaping to the main-actor UI
- `BackgroundImporter.swift` spins up throwaway background contexts — `let modelContext = ModelContext(modelContainer)` at lines **35, 131, 622, 780, 869, 956** (file header: *"Creates ModelContext inside each method for 10x performance improvement"*) — and inserts `NodeV2` graphs there (`NodeV2(...)` :288, `modelContext.insert(newNode)` :107).
- **No `@ModelActor` anywhere in the app (0 occurrences)** — no SwiftData isolation. Nothing stops a background-context `NodeV2` (or a reference into its object graph) from reaching the main context.
- The UI faults `NodeV2` to-many relationships (`photos`, `defaultPhoto`, `node_terminals`, …) in ~98 `Views/` sites on the main actor. Once the owning background context is gone, faulting the to-many → `EXC_BREAKPOINT: "…resolve a future for a relationship that doesn't have a cached value … toManyRelationship"`.
- Corroboration: `EditNodeDetailView.swift:193` already *works around* this by avoiding `node.defaultPhoto` ("replicate … using snapshot data").
- **Fix:** isolate background SwiftData work behind a `@ModelActor`; never pass live models across contexts — return `PersistentIdentifier`s and re-fetch on the consuming (main) context. Add explicit `@Relationship(inverse:)` to the bare to-many arrays (`core_attributes`, `issues`, `ir_photos`).

### DE — missing-file photo uploads are never dead-lettered → infinite retry + silent data loss
- The *original* trigger (legacy absolute paths with a dead container UUID) is **already mitigated**: `SyncFileManager.resolveStoredPath` (:148) now stores relative paths and recovers legacy ones (`ZP-2-fix-2026-05`, Apple TN2406).
- Remaining bug: when the preserved file is *genuinely* gone, `PhotoUploadService` throws `SyncError.fileNotFound` (:144); `SyncExecutionService` correctly stops retrying *within the call* (`if case SyncError.fileNotFound = error { break }` :124) and reports via `captureSyncFailure` (:133–145) — **but the queue item is not terminally failed.** It survives across sync cycles (and per the code comments can be rebuilt from `SyncLog`), so each flush re-attempts and re-emits → **2,359 events from 13 users**, and the photo never uploads (silent data loss). *(Exact queue-lifecycle line for the non-terminal handling is the one remaining thing to pin in `SyncQueueService`.)*
- Minor: the Sentry capture hardcodes `retryCount: maxRetries` (:139) regardless of attempts actually run — so the dashboard's "retry_count: 3" is misleading.
- **Fix:** on `fileNotFound` after recovery, **dead-letter** the item (mark terminally failed; stop re-enqueueing / `SyncLog` rebuild) and surface "photo couldn't be uploaded" to the user instead of failing silently. Report the true attempt count.

### 8P — `Dictionary(uniqueKeysWithValues:)` over collections containing duplicate UUIDs
- ~20 sites build `Dictionary(uniqueKeysWithValues: xs.map { ($0.id, $0) })`, which **traps on the first duplicate key** → `SIGTRAP: "Duplicate values for key: '<UUID>'"`.
- Most exposed: `BackgroundImporter.swift:65` / `:147` build dedup lookups from *existing DB rows* that can already contain duplicate ids (from a prior bad sync/merge) — so the dedup step itself crashes — plus SLD diagram views over server-supplied data (`AssetsTabView:88`, `ConnectionsTabView:475`, `RoomDetailView:69`, `NodeEdgesViewModel:53–54`).
- **Fix (blanket — kills the whole class):** replace every `Dictionary(uniqueKeysWithValues:)` with `Dictionary(_, uniquingKeysWith: { first, _ in first })` (or `{ _, last in last }`), or a small `keyedById` helper; optionally add a duplicate-id guard in the import dedup.

---

## Cross‑cutting theme: memory pressure
Memory shows up across unrelated crashes, so it's likely systemic, not incidental:
- **Z-PLATFORM-7** is an outright OOM (`WatchdogTermination … overused RAM`).
- The #1 crash (5S) fired with only **216 MB free of 3 GB** on the device.
- The sync failure (DE) fired with **191 MB free** and **thermal_state: "serious"**, app **backgrounded**.

A low‑memory / large‑dataset soak is worth running regardless of the individual fixes.

---

## Detailed reads

### 1. Z-PLATFORM-5S — SwiftData to‑many relationship fault (top crasher)
```
EXC_BREAKPOINT: SwiftData/DefaultStore.swift:1949: Fatal error: Illegal attempt to resolve a
future for a relationship that doesn't have a cached value:
DefaultStoreSnapshotValueFuture(propertyName: "ѕ\u{01}\0\0\0s",
futureType: ...FutureType.toManyRelationship)
PersistentIdentifier( ... NodeV2/p1108 )
```
- **Read:** A SwiftData `@Model` (`NodeV2`) to‑many relationship is being resolved when its backing
  store has no cached value — classic when a `ModelContext`/model object is touched **off its actor**
  (cross‑thread access) or **after the object was deleted/faulted**. The garbled `propertyName`
  (`"ѕ\u{01}\0\0\0s"`) points to reading freed/corrupted backing memory, consistent with a
  concurrency bug. Frames `runJobInEstablishedExecutorContext` confirm it happens on a Swift
  concurrency executor.
- **Needs source to fix:** find where `NodeV2.<toManyRel>` is accessed outside its `ModelContext`'s
  actor (look for `@MainActor` boundaries, `ModelContext` passed across `Task`s, background sync).
- **Black‑box repro idea:** drive heavy navigation through node/tree screens while a sync runs in
  background (see repro matrix below).

### 5. Z-PLATFORM-8P — duplicate dictionary key (clean fix)
```
SIGTRAP: Fatal error: Duplicate values for key: 'B593EA9E-6108-4600-A306-D1CA517D733F'
```
- **Read:** `Dictionary(uniqueKeysWithValues:)` (or keyed reduction) built from a collection that
  contains two records sharing the same UUID. Real‑world data has dup IDs (offline merge, re‑sync),
  so this *will* recur.
- **Likely one‑line fix (once source is in):** switch to
  `Dictionary(grouping:by:)` or `Dictionary(seq, uniquingKeysWith: { a, _ in a })`.

### Z-PLATFORM-DE — offline photo sync references a file that's gone (data loss, 2,359 events)
```
Z_Platform.SyncError: fileNotFound(".../Documents/sync_pending_attachments/<queueItem>/<id>.jpg")
sync_operation=create  sync_target=photo  retry_count=3  queue_count=38  is_syncing=true
```
- **Read:** A photo enqueued for upload under `Documents/sync_pending_attachments/…` is **missing
  from disk** by the time the sync worker runs, so the queue item fails and retries indefinitely
  (note the stuck `queue_count: 38`, `retry_count: 3`, and **2,359 events** from only 13 users). The
  queue entry (likely persisted in SwiftData) outlived the file. Causes to check: file written to a
  purgeable location, cleanup deleting the file before the queue drains, or path rebuilt from a
  stale container UUID after app reinstall/migration.
- **Why it matters here:** this is a **data‑loss** bug — user photos never upload — and it maps
  directly onto the existing `OfflineSyncMultiSite_Test` / `OfflineTest`.

---

## Repro matrix → this Appium suite (black‑box, no source needed)

| Sentry issue | Proposed scenario | Anchor in repo |
|--------------|-------------------|----------------|
| 5S, 1P, 6P, 8M | Rapid navigation in/out of node/asset/issue trees while backgrounding & a sync runs | `Asset_Phase*`, `Issue_Phase*`, `BaseTest` (terminate/activate already present) |
| 7 (+ memory theme) | Large‑dataset soak: open biggest site, scroll long lists, leave foreground 10+ min | `OfflineSyncMultiSite_Test`, `SiteVisit_phase*` |
| 8P | Trigger a re‑sync / merge that can produce duplicate entity UUIDs | `OfflineSyncMultiSite_Test` |
| **DE** | Offline → attach photo → background/kill or clear cache → online → assert queue drains, no stuck items | `OfflineTest`, `OfflineSyncMultiSite_Test` |
| CD | Open a screen whose list can legitimately be empty (index 1 on `[0..0]`) | identify exact screen from list views |

---

## Config smell (not a crash, worth flagging)
Every event's instrumentation reports `app_identifier: "com.ericehlert.SwiftDataTutorial"` while the
release is `com.egalvanic.zplatform`. The app appears to have been scaffolded from a public SwiftData
tutorial and the leftover identifier was never changed in the crash‑SDK config. Harmless to users but
muddies attribution; worth correcting in the app project.

---

## What I can do next without anything from you
1. Deep‑dive remaining issues (full traces for 1P, 6P, 5D/6A/A4 to confirm whether the double‑frees share a stack) and group duplicates.
2. Implement the **repro matrix** scenarios as new tests in this suite (black‑box, runs against the existing `apps/Z-Platform-QA.zip`).

## What needs the Swift source (your add)
3. Line‑level root cause + fixes for 5S (SwiftData actor boundary), 8P (dup‑key dictionary), DE (pending‑file lifecycle), and the EXC_BAD_ACCESS/SIGABRT cluster.
