import SwiftUI
import SwiftData
import Combine

@MainActor
final class SyncQueueAnalyzerViewModel: ObservableObject {
    private var modelContext: ModelContext?
    private var cancellables = Set<AnyCancellable>()
    private var activeSegment: Int = 0

    @Published var pendingItems: [SyncQueueItem] = []
    @Published var syncLogs: [SyncLog] = []
    @Published var groupedLogs: [(key: Date, value: [SyncLog])] = []

    /// Lightweight counts for tab badges — updated independently of full arrays
    /// to avoid re-laying-out the segmented control when the inactive tab's data changes.
    @Published var pendingCount: Int = 0
    @Published var historyCount: Int = 0

    /// ZP-2137 — History tab pagination.
    /// Mirrors Android's Paging 3 config (pageSize=30, initialLoadSize=60).
    /// `isLoadingMore` drives the append-load footer spinner; `hasMorePages`
    /// gates further fetches once the DB is exhausted.
    private let historyPageSize = 30
    private let historyInitialPageSize = 60
    @Published var isLoadingMore: Bool = false
    private var hasMorePages: Bool = true

    /// ZP-1847 — site name lookup keyed by SLDChoice id. Populated alongside
    /// queue/log loads so per-row UI can show a human-readable site label
    /// without an extra fetch per row.
    @Published var siteNamesById: [UUID: String] = [:]

    /// ZP-2097 / ZP-1265 (aa123c2) — id of the queue row currently being
    /// uploaded (drives the rotating sync icon). A row with `lastFailureAt`
    /// is the Failed visual state; no separate halt id is needed because
    /// the loop no longer halts on item failure. Mirrored from
    /// `NetworkState.shared.currentSyncItemId` via the Combine observer.
    @Published var currentSyncItemId: UUID?

    /// ZP-1265 (aa123c2) — true while a flush is actively running. Drives
    /// the toolbar Sync icon rotation and the per-item Retry/Delete
    /// hide-during-sync gate so the user can't kick a parallel sync.
    @Published var isFlashing: Bool = false

    /// ZP-1265 (aa123c2) — one-shot flag: latched true when the chunk
    /// processor detects mid-flush network loss. The view consumes it to
    /// render the "Internet connection lost" dialog and clears it via
    /// `acknowledgeNetworkLost()`. No auto-retry on reconnect.
    @Published var networkLostDuringSync: Bool = false

    /// ZP-1265 (aa123c2) — hardware reachability passthrough so the
    /// offline banner can pick the right copy (hardware vs manual mode).
    @Published var isNetworkAvailable: Bool = true

    var failedLogs: [SyncLog] {
        syncLogs.filter { !$0.success }
    }

    func setModelContext(_ context: ModelContext) {
        self.modelContext = context
    }

    // MARK: - Auto-Refresh via Combine

    /// Start observing sync queue count changes for real-time animated list updates.
    /// Uses throttle (not debounce) so updates arrive at regular intervals during rapid sync,
    /// rather than waiting until all operations finish.
    func startObserving(networkState: NetworkState) {
        // Cancel previous subscriptions (handles repeated onAppear calls)
        cancellables.removeAll()

        networkState.$syncQueueCount
            .removeDuplicates()
            .dropFirst() // Skip initial emit — loadData() handles the first load
            .throttle(for: .seconds(1.0), scheduler: RunLoop.main, latest: true)
            .sink { [weak self] _ in
                self?.refreshActiveDataAndCounts()
            }
            .store(in: &cancellables)

        // ZP-2097 / ZP-1265 (aa123c2) — mirror live progress into the VM so
        // SwiftUI bindings drive the per-row spinner. The toolbar Sync icon
        // rotation and the "hide per-item Retry/Delete during flush" gate
        // both read `isFlashing`.
        networkState.$currentSyncItemId
            .removeDuplicates()
            .receive(on: RunLoop.main)
            .sink { [weak self] in self?.currentSyncItemId = $0 }
            .store(in: &cancellables)

        networkState.$isSyncing
            .removeDuplicates()
            .receive(on: RunLoop.main)
            .sink { [weak self] in self?.isFlashing = $0 }
            .store(in: &cancellables)

        networkState.$networkLostDuringSync
            .removeDuplicates()
            .receive(on: RunLoop.main)
            .sink { [weak self] in self?.networkLostDuringSync = $0 }
            .store(in: &cancellables)

        networkState.$isNetworkAvailable
            .removeDuplicates()
            .receive(on: RunLoop.main)
            .sink { [weak self] in self?.isNetworkAvailable = $0 }
            .store(in: &cancellables)
    }

    /// Dismiss the "Internet connection lost" dialog. The user must
    /// explicitly tap Sync Now to resume — no auto-retry on reconnect.
    @MainActor
    func acknowledgeNetworkLost() {
        NetworkState.shared.acknowledgeNetworkLost()
    }

    /// ZP-2097 / ZP-1265 (aa123c2) — per-item Retry from the Pending tab.
    /// Clears the row's failure fields and starts a flush. The failed item
    /// naturally sits at the head of the FIFO queue so the dispatch picks
    /// it up first. With strict-stop removed, retrying a single failed
    /// item also re-attempts every other failed/pending item in the queue
    /// (matching Android's "the loop runs everything" semantics).
    func retryFailedItem(_ itemId: UUID) {
        guard let context = modelContext else { return }

        let descriptor = FetchDescriptor<SyncQueueItem>(
            predicate: #Predicate { $0.id == itemId }
        )
        if let item = try? context.fetch(descriptor).first {
            item.lastFailureMessage = nil
            item.lastFailureAt = nil
            item.lastFailureCode = nil
            try? context.save()
        }

        refreshActiveDataAndCounts()
        NetworkState.shared.flushQueue()
    }

    /// Stop observing when the view disappears to avoid unnecessary work.
    func stopObserving() {
        cancellables.removeAll()
    }

    // MARK: - Data Loading

    /// Load data for the selected segment (0 = pending, 1 = history)
    func loadData(for segment: Int) {
        activeSegment = segment
        guard let context = modelContext else { return }

        loadSiteNames(context: context)
        if segment == 0 {
            loadPendingItems(context: context)
        } else {
            loadFirstHistoryPage(context: context)
        }
    }

    /// Load both pending items and sync logs
    func loadAllData() {
        guard let context = modelContext else { return }
        loadSiteNames(context: context)
        loadPendingItems(context: context)
        loadFirstHistoryPage(context: context)
    }

    /// Refresh only the active segment's full data + both badge counts (lightweight).
    /// This avoids replacing inactive tab arrays which causes segmented control flicker.
    func refreshActiveDataAndCounts() {
        guard let context = modelContext else { return }
        loadSiteNames(context: context)

        // Load full data for the active segment only
        if activeSegment == 0 {
            loadPendingItems(context: context)
            // ZP-2137 — history badge uses the same predicate-backed count
            // we'd run on the History tab, so the "(N)" stays accurate even
            // while the inactive tab isn't loaded.
            historyCount = currentUserHistoryCount(context: context)
        } else {
            // ZP-2137 — refresh only the *currently loaded* page window so
            // new logs inserted during a flush appear at the top without
            // collapsing the user's scrolled-in pages back to page 1.
            refreshLoadedHistoryWindow(context: context)
            pendingCount = filteredCount(of: SyncQueueItem.self, in: context, getUser: { $0.userId })
        }
    }

    /// ZP-1847 — current-user filter (nil userId is allowed through, matching
    /// `SyncQueueService.getPendingSyncOps`). After a non-destructive
    /// account switch, the previous user's items remain in the DB but are
    /// invisible here.
    private func currentUserId() -> UUID? {
        let nullSentinel = UUID(uuidString: "00000000-0000-0000-0000-000000000000")!
        let id = AppStateManager.shared.userId
        return id == nullSentinel ? nil : id
    }

    private func belongsToCurrentUser(_ itemUserId: UUID?) -> Bool {
        guard let activeUser = currentUserId() else { return true }
        guard let itemUser = itemUserId else { return true }
        return itemUser == activeUser
    }

    private func filteredCount<T: PersistentModel>(of: T.Type,
                                                    in context: ModelContext,
                                                    getUser: (T) -> UUID?) -> Int {
        let descriptor = FetchDescriptor<T>()
        guard let all = try? context.fetch(descriptor) else { return 0 }
        return all.reduce(0) { acc, item in
            belongsToCurrentUser(getUser(item)) ? acc + 1 : acc
        }
    }

    private func loadPendingItems(context: ModelContext) {
        // Sort oldest-first (FIFO) so the displayed order mirrors the actual
        // flush dispatch order (`SyncQueueService.getPendingSyncOps` sorts by
        // `\.createdAt` ascending). The user sees the next-to-sync row at the
        // top, the most-recently-enqueued at the bottom.
        let descriptor = FetchDescriptor<SyncQueueItem>(
            sortBy: [SortDescriptor(\.createdAt, order: .forward)]
        )
        let all = (try? context.fetch(descriptor)) ?? []
        pendingItems = all.filter { belongsToCurrentUser($0.userId) }
        pendingCount = pendingItems.count
    }

    // MARK: - History pagination (ZP-2137)

    /// Predicate that scopes a `SyncLog` query to the current user's rows
    /// (plus legacy / unstamped rows where `userId == nil`). Returning `nil`
    /// means "no active user" — the previous behavior was to show everything,
    /// matching `belongsToCurrentUser`.
    ///
    /// Moving the user filter from a post-fetch `.filter` into the SwiftData
    /// predicate is what makes paging correct: an in-memory filter would
    /// leave gaps across `(offset, limit)` page boundaries and produce an
    /// inconsistent total. Same reasoning as ZP-2137 on Android.
    private func historyPredicate() -> Predicate<SyncLog>? {
        guard let activeUser = currentUserId() else { return nil }
        return #Predicate<SyncLog> { log in
            log.userId == nil || log.userId == activeUser
        }
    }

    /// Reactive total visible to the current user. Backs the "History (N)"
    /// segmented-tab badge. Equivalent to Android's
    /// `SyncLogDao.getCountForUserFlow`.
    private func currentUserHistoryCount(context: ModelContext) -> Int {
        var descriptor = FetchDescriptor<SyncLog>()
        descriptor.predicate = historyPredicate()
        return (try? context.fetchCount(descriptor)) ?? 0
    }

    /// Build the descriptor for a single page of history. Reverse-chronological,
    /// scoped to the current user via `historyPredicate`.
    private func historyPageDescriptor(offset: Int, limit: Int) -> FetchDescriptor<SyncLog> {
        var descriptor = FetchDescriptor<SyncLog>(
            sortBy: [SortDescriptor(\.timestamp, order: .reverse)]
        )
        descriptor.predicate = historyPredicate()
        descriptor.fetchOffset = offset
        descriptor.fetchLimit = limit
        return descriptor
    }

    /// Initial History-tab load. Resets paging state and fetches the first
    /// `historyInitialPageSize` rows. Always called on tab switch /
    /// `loadAllData` / explicit refresh — never appends.
    private func loadFirstHistoryPage(context: ModelContext) {
        hasMorePages = true
        isLoadingMore = false

        let descriptor = historyPageDescriptor(offset: 0, limit: historyInitialPageSize)
        let firstPage = (try? context.fetch(descriptor)) ?? []
        syncLogs = firstPage
        hasMorePages = firstPage.count == historyInitialPageSize
        historyCount = currentUserHistoryCount(context: context)
        rebuildGroupedLogs()
    }

    /// Append the next page when the user scrolls to the end of the loaded
    /// window. Mirrors Android's `Pager` append step. No-op if a load is
    /// already in flight or the DB is exhausted.
    func loadNextHistoryPage(context: ModelContext) {
        guard hasMorePages, !isLoadingMore else { return }
        isLoadingMore = true
        defer { isLoadingMore = false }

        let descriptor = historyPageDescriptor(offset: syncLogs.count, limit: historyPageSize)
        let nextPage = (try? context.fetch(descriptor)) ?? []
        syncLogs.append(contentsOf: nextPage)
        hasMorePages = nextPage.count == historyPageSize
        rebuildGroupedLogs()
    }

    /// Re-fetch the *currently loaded* page window (offset 0, limit = loaded
    /// row count) without changing the user's pagination depth. Driven by the
    /// Combine throttle observer during a flush so newly-inserted rows appear
    /// at the top while preserving scroll/page state. Equivalent to Room's
    /// `PagingSource.invalidate()` on the visible window.
    private func refreshLoadedHistoryWindow(context: ModelContext) {
        let limit = max(syncLogs.count, historyInitialPageSize)
        let descriptor = historyPageDescriptor(offset: 0, limit: limit)
        let refreshed = (try? context.fetch(descriptor)) ?? []
        syncLogs = refreshed
        // If the refresh returned a full window, there may still be more rows
        // beyond it — preserve `hasMorePages` so the next-page trigger keeps
        // working. If it came up short, the DB has been trimmed (e.g. clear
        // history while paged in) and we're at the end.
        if refreshed.count < limit {
            hasMorePages = false
        }
        historyCount = currentUserHistoryCount(context: context)
        rebuildGroupedLogs()
    }

    private func rebuildGroupedLogs() {
        let grouped = Dictionary(grouping: syncLogs) { log in
            Calendar.current.startOfDay(for: log.timestamp)
        }
        groupedLogs = grouped.sorted { $0.key > $1.key }
    }

    /// Build a `siteId -> displayName` lookup so per-row UI can show a
    /// human-readable site label without an extra fetch per row.
    ///
    /// We merge two sources because each one alone has gaps:
    ///   * `SLDChoice` holds every site the user has access to, but is only
    ///     populated when the user visits the Sites tab or site selector —
    ///     a user who logs in straight to a persisted active site may have
    ///     an empty SLDChoice table.
    ///   * `SLDV2` holds full data for sites the user has actually loaded
    ///     (the active one and any cached on-disk). Always available for
    ///     the current site.
    /// Either source's `(id, name)` pair populates the map; if both miss
    /// for a given siteId (e.g. the user lost access to that site), the
    /// row hides the pill rather than rendering a raw UUID.
    private func loadSiteNames(context: ModelContext) {
        var map: [UUID: String] = [:]

        if let choices = try? context.fetch(FetchDescriptor<SLDChoice>()) {
            for c in choices { map[c.id] = c.name }
        }
        if let sites = try? context.fetch(FetchDescriptor<SLDV2>()) {
            // Don't overwrite an existing SLDChoice name with the SLDV2 one
            // (SLDChoice is the canonical user-facing list); only fill gaps.
            for s in sites where map[s.id] == nil { map[s.id] = s.name }
        }

        siteNamesById = map
    }
}
