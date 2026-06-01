import SwiftUI
import SwiftData

// MARK: - Location Descendant Counts
struct LocationDescendantCounts {
    var floors: Int = 0
    var rooms: Int = 0
    var assets: Int = 0

    var isEmpty: Bool {
        floors == 0 && rooms == 0 && assets == 0
    }
}

// MARK: - Task Descendant Counts
struct TaskDescendantCounts {
    var nodes: Int = 0
    var formInstances: Int = 0
    var photos: Int = 0
    var relatedQueueItems: Int = 0

    var isEmpty: Bool {
        nodes == 0 && formInstances == 0 && photos == 0 && relatedQueueItems == 0
    }
}

// MARK: - Issue Descendant Counts
struct IssueDescendantCounts {
    var photos: Int = 0
    var irPhotos: Int = 0
    var tasks: Int = 0
    var relatedQueueItems: Int = 0

    var isEmpty: Bool {
        photos == 0 && irPhotos == 0 && tasks == 0 && relatedQueueItems == 0
    }
}

// MARK: - Sync Queue Analyzer View
struct SyncQueueAnalyzerView: View {
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState

    @State private var selectedSegment = 0
    @State private var showingClearConfirmation = false
    @State private var exportFileURL: URL?
    @State private var exportData: ExportDataWrapper?

    // Location deletion confirmation state
    @State private var showLocationDeletionConfirmation = false
    @State private var pendingLocationDeletion: SyncQueueItem?
    @State private var locationDeletionCounts: LocationDescendantCounts?
    @State private var locationDeletionTitle = ""
    @State private var locationDeletionMessage = ""

    // Wrapper to make String identifiable for sheet(item:)
    struct ExportDataWrapper: Identifiable {
        let id = UUID()
        let jsonString: String
    }

    /// ZP-1265 (aa123c2) — Toolbar Sync icon rotation. Driven by a
    /// `withAnimation(.linear(...).repeatForever)` started/stopped via
    /// `.onChange(of: isFlashing)`.
    @State private var toolbarSpinnerAngle: Double = 0

    @StateObject private var viewModel = SyncQueueAnalyzerViewModel()
    
    var body: some View {
        VStack(spacing: 0) {
            // ZP-1265 (aa123c2) — Persistent offline banner above the
            // segment control. Hardware case ("No internet connection")
            // takes precedence; manual offline-mode shows the secondary
            // copy. Read-only — surfaces *why* the toolbar Sync button
            // is disabled.
            if !networkState.isOnline {
                HStack(spacing: 8) {
                    Image(systemName: "wifi.slash")
                        .font(.subheadline)
                        .foregroundColor(.red)
                    Text(viewModel.isNetworkAvailable
                         ? AppStrings.Diagnostics.offlineBannerOfflineMode
                         : AppStrings.Diagnostics.offlineBannerNoNetwork)
                        .font(.caption)
                        .foregroundColor(.red)
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .frame(maxWidth: .infinity)
                .background(Color.red.opacity(0.10))
            }

            // Segment control
            Picker("View", selection: $selectedSegment) {
                Text(AppStrings.Diagnostics.pendingCount(viewModel.pendingCount)).tag(0)
                Text(AppStrings.Diagnostics.historyCount(viewModel.historyCount)).tag(1)
            }
            .pickerStyle(SegmentedPickerStyle())
            .padding()

            // Content based on selection
            if selectedSegment == 0 {
                pendingQueueView
            } else {
                historyView
            }
        }
        .navigationTitle(AppStrings.Settings.syncQueueAnalyzer)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            // ZP-1265 (aa123c2) — Dedicated Sync icon button. Replaces the
            // overflow menu's "Flush Queue Now" + "Re-queue all failed".
            // Disabled when offline / pending list empty / a sync is
            // already running. Rotates while a flush is in-flight to
            // mirror the per-row spinner.
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { networkState.flushQueue() }) {
                    Image(systemName: "arrow.triangle.2.circlepath")
                        .rotationEffect(.degrees(toolbarSpinnerAngle))
                        .onChange(of: viewModel.isFlashing) { _, syncing in
                            if syncing {
                                withAnimation(.linear(duration: 1.2).repeatForever(autoreverses: false)) {
                                    toolbarSpinnerAngle = 360
                                }
                            } else {
                                withAnimation(.default) { toolbarSpinnerAngle = 0 }
                            }
                        }
                }
                .disabled(viewModel.pendingItems.isEmpty
                          || viewModel.isFlashing
                          || !networkState.isOnline)
                .accessibilityLabel(AppStrings.Diagnostics.flushQueueNow)
            }

            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button(action: exportSyncQueue) {
                        Label(AppStrings.Diagnostics.exportQueueAsJson, systemImage: "square.and.arrow.up")
                    }
                    .disabled(viewModel.pendingItems.isEmpty)

                    Divider()

                    Button(role: .destructive, action: { showingClearConfirmation = true }) {
                        Label(AppStrings.Diagnostics.clearAllHistory, systemImage: "trash")
                    }
                    // ZP-2137 — `syncLogs` is now just the loaded page window,
                    // so use the predicate-backed total instead.
                    .disabled(viewModel.historyCount == 0)
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
            }
        }
        .sheet(item: $exportData) { data in
            // Share the JSON string directly - more reliable than file URLs
            ShareSheet(items: [data.jsonString])
        }
        // ZP-1265 (aa123c2) — Network-lost dialog. Surfaced one-shot when a
        // flush halts because reachability dropped mid-flight. No auto-retry
        // — the user must reconnect and explicitly tap Sync Now.
        .alert(
            AppStrings.Diagnostics.networkLostDialogTitle,
            isPresented: Binding(
                get: { viewModel.networkLostDuringSync },
                set: { newValue in
                    if !newValue { viewModel.acknowledgeNetworkLost() }
                }
            )
        ) {
            Button(AppStrings.Common.ok, role: .cancel) {
                viewModel.acknowledgeNetworkLost()
            }
        } message: {
            Text(AppStrings.Diagnostics.networkLostDialogBody)
        }
        .alert(AppStrings.Diagnostics.clearSyncHistory, isPresented: $showingClearConfirmation) {
            Button(AppStrings.Common.cancel, role: .cancel) { }
            Button(AppStrings.Common.clear, role: .destructive) {
                clearSyncHistory()
            }
        } message: {
            Text(AppStrings.Diagnostics.clearSyncHistoryMessage)
        }
        .alert(locationDeletionTitle, isPresented: $showLocationDeletionConfirmation) {
            Button(AppStrings.Common.cancel, role: .cancel) {
                pendingLocationDeletion = nil
            }
            Button(AppStrings.Common.delete, role: .destructive) {
                performLocationDeletion()
            }
        } message: {
            Text(locationDeletionMessage)
        }
        .onAppear {
            viewModel.setModelContext(modelContext)
            viewModel.startObserving(networkState: networkState)
            viewModel.loadAllData()

            // ZP-1847 (Android-parity): refresh `SLDChoice` from the server
            // so per-row site labels in the Pending tab work for *any* site
            // a queued item belongs to — not just the currently-active one.
            // Without this, after a site switch the previous site's `SLDV2`
            // is gone (cleared by `clearSLDs`) and the lookup misses unless
            // `SLDChoice` was already populated, which isn't guaranteed for
            // every login flow. Mirrors Android's "Sites view open triggers
            // refresh" pattern. No-op when offline (persisted rows still
            // serve the lookup; the `SLDV2` fallback covers the active site).
            let userId = AppStateManager.shared.userId
            let nullSentinel = UUID(uuidString: "00000000-0000-0000-0000-000000000000")!
            if userId != nullSentinel, NetworkReachability.shared.isConnected {
                Task {
                    _ = await SLDService.shared.fetchAndUpsertSLDChoices(
                        user_id: userId,
                        modelContext: modelContext
                    )
                    await MainActor.run {
                        viewModel.refreshActiveDataAndCounts()
                    }
                }
            }
        }
        .onDisappear {
            viewModel.stopObserving()
        }
        .onChange(of: selectedSegment) { _, newSegment in
            viewModel.loadData(for: newSegment)
        }
        .onChange(of: networkState.isSyncing) { _, isSyncing in
            // Immediate refresh when sync completes (throttle might delay up to 1s)
            if !isSyncing {
                viewModel.refreshActiveDataAndCounts()
            }
        }
    }
    
    // MARK: - Pending Queue View
    private var pendingQueueView: some View {
        Group {
            if viewModel.pendingItems.isEmpty {
                ContentUnavailableView(
                    AppStrings.Diagnostics.noPendingOperations,
                    systemImage: "checkmark.circle",
                    description: Text(AppStrings.Diagnostics.allSyncProcessed)
                )
            } else {
                List {
                    ForEach(viewModel.pendingItems) { item in
                        let isSyncing = viewModel.currentSyncItemId == item.id
                        // ZP-2097 / ZP-1265 (aa123c2) — pass per-row state +
                        // handlers. Per-item Retry/Delete buttons are hidden
                        // during any flush (showActions = !isFlashing) so
                        // the user can't kick a parallel sync. Swipe-to-
                        // delete is also disabled mid-flush.
                        let row = PendingItemRow(
                            item: item,
                            siteNamesById: viewModel.siteNamesById,
                            isSyncing: isSyncing,
                            showActions: !viewModel.isFlashing,
                            onRetry: { viewModel.retryFailedItem(item.id) },
                            onDelete: { handleDeleteRequest(for: item) }
                        )

                        if viewModel.isFlashing || isSyncing {
                            row
                        } else {
                            row.swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                Button(role: .destructive) {
                                    handleDeleteRequest(for: item)
                                } label: {
                                    Label(AppStrings.Common.delete, systemImage: "trash")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // MARK: - History View
    private var historyView: some View {
        Group {
            if viewModel.syncLogs.isEmpty {
                ContentUnavailableView(
                    AppStrings.Diagnostics.noSyncHistory,
                    systemImage: "clock",
                    description: Text(AppStrings.Diagnostics.completedSyncWillAppear)
                )
            } else {
                // ZP-2137 — paged history. The last log row in the loaded
                // window triggers `loadNextHistoryPage` via `.onAppear`,
                // mirroring AndroidX Paging's append-load step. The footer
                // spinner is shown while the next page is in-flight.
                let lastLogId = viewModel.syncLogs.last?.id
                List {
                    ForEach(viewModel.groupedLogs, id: \.key) { day, logs in
                        Section(header: Text(formatSectionDate(day))) {
                            ForEach(logs) { log in
                                SyncLogRow(log: log, siteNamesById: viewModel.siteNamesById)
                                    .onAppear {
                                        if log.id == lastLogId {
                                            viewModel.loadNextHistoryPage(context: modelContext)
                                        }
                                    }
                            }
                        }
                    }
                    if viewModel.isLoadingMore {
                        Section {
                            HStack {
                                Spacer()
                                ProgressView()
                                Spacer()
                            }
                        }
                    }
                }
            }
        }
    }
    
    // MARK: - Helper Methods
    private func deletePendingItems(at offsets: IndexSet) {
        for index in offsets {
            modelContext.delete(viewModel.pendingItems[index])
        }
        try? modelContext.save()
        networkState.refreshSyncQueueCount()
        viewModel.loadData(for: 0)
    }

    private func handleDeleteRequest(for item: SyncQueueItem) {
        if requiresConfirmation(item) {
            if item.target == .attachment {
                // Attachment deletion — confirm since it removes the local file and entity
                if item.operation == .create {
                    pendingLocationDeletion = item
                    locationDeletionTitle = AppStrings.Sessions.deleteAttachment
                    locationDeletionMessage = AppStrings.Sessions.deleteAttachmentConfirm
                    showLocationDeletionConfirmation = true
                } else {
                    // Non-create operations (delete/update): just remove the queue item
                    deleteQueueItem(item)
                }
            } else if item.target == .userTask {
                // Task deletion confirmation
                let syncService = SyncQueueService(modelContext: modelContext)
                if let counts = syncService.countTaskDescendants(for: item) {
                    pendingLocationDeletion = item
                    locationDeletionTitle = "Delete Task?"
                    locationDeletionMessage = makeTaskDeletionMessage(item, counts: counts)
                    showLocationDeletionConfirmation = true
                } else {
                    // Task entity doesn't exist, just delete queue item
                    deleteQueueItem(item)
                }
            } else if item.target == .issue {
                // Issue deletion confirmation
                let syncService = SyncQueueService(modelContext: modelContext)
                if let counts = syncService.countIssueDescendants(for: item) {
                    pendingLocationDeletion = item
                    locationDeletionTitle = "Delete Issue?"
                    locationDeletionMessage = makeIssueDeletionMessage(item, counts: counts)
                    showLocationDeletionConfirmation = true
                } else {
                    // Issue entity doesn't exist, just delete queue item
                    deleteQueueItem(item)
                }
            } else if let counts = countDescendants(for: item) {
                pendingLocationDeletion = item
                locationDeletionCounts = counts
                locationDeletionTitle = makeLocationDeletionTitle(item)
                locationDeletionMessage = makeLocationDeletionMessage(item, counts: counts)
                showLocationDeletionConfirmation = true
            } else {
                // Entity doesn't exist, just delete queue item
                deleteQueueItem(item)
            }
        } else {
            // Non-location/task items: direct deletion
            deleteQueueItem(item)
        }
    }

    private func requiresConfirmation(_ item: SyncQueueItem) -> Bool {
        return item.target == .building || item.target == .floor || item.target == .room || item.target == .userTask || item.target == .issue || (item.target == .attachment && item.operation == .create)
    }

    private func countDescendants(for item: SyncQueueItem) -> LocationDescendantCounts? {
        guard let entity = fetchLocationEntity(for: item) else { return nil }

        var counts = LocationDescendantCounts()

        switch item.target {
        case .building:
            guard let building = entity as? Building else { return nil }
            counts.floors = building.floors.count
            for floor in building.floors {
                counts.rooms += floor.rooms.count
                for room in floor.rooms {
                    counts.assets += room.nodes.count
                }
            }

        case .floor:
            guard let floor = entity as? Floor else { return nil }
            counts.rooms = floor.rooms.count
            for room in floor.rooms {
                counts.assets += room.nodes.count
            }

        case .room:
            guard let room = entity as? Room else { return nil }
            counts.assets = room.nodes.count

        default:
            return nil
        }

        return counts
    }

    private func fetchLocationEntity(for item: SyncQueueItem) -> Any? {
        do {
            switch item.target {
            case .building:
                guard let buildingId = item.buildingId else { return nil }
                let descriptor = FetchDescriptor<Building>(
                    predicate: #Predicate<Building> { b in b.id == buildingId }
                )
                return try modelContext.fetch(descriptor).first

            case .floor:
                guard let floorId = item.floorId else { return nil }
                let descriptor = FetchDescriptor<Floor>(
                    predicate: #Predicate<Floor> { f in f.id == floorId }
                )
                return try modelContext.fetch(descriptor).first

            case .room:
                guard let roomId = item.roomId else { return nil }
                let descriptor = FetchDescriptor<Room>(
                    predicate: #Predicate<Room> { r in r.id == roomId }
                )
                return try modelContext.fetch(descriptor).first

            default:
                return nil
            }
        } catch {
            return nil
        }
    }

    private func makeLocationDeletionTitle(_ item: SyncQueueItem) -> String {
        switch item.target {
        case .building: return AppStrings.Diagnostics.deleteBuildingQuestion("")
        case .floor: return AppStrings.Diagnostics.deleteFloorQuestion
        case .room: return AppStrings.Diagnostics.deleteRoomQuestion
        default: return AppStrings.Diagnostics.deleteLocationQuestion
        }
    }

    private func makeLocationDeletionMessage(_ item: SyncQueueItem, counts: LocationDescendantCounts) -> String {
        let entityType: String
        switch item.target {
        case .building: entityType = "building"
        case .floor: entityType = "floor"
        case .room: entityType = "room"
        default: entityType = "location"
        }

        if counts.isEmpty {
            return AppStrings.Diagnostics.entityWillBeDeleted(entityType)
        }

        var parts: [String] = []
        if counts.floors > 0 {
            parts.append("\(counts.floors) floor\(counts.floors == 1 ? "" : "s")")
        }
        if counts.rooms > 0 {
            parts.append("\(counts.rooms) room\(counts.rooms == 1 ? "" : "s")")
        }
        if counts.assets > 0 {
            parts.append("\(counts.assets) asset\(counts.assets == 1 ? "" : "s")")
        }

        let countsString = parts.joined(separator: ", ")
        return AppStrings.Diagnostics.entityHasDescendants(entityType, countsString)
    }

    private func makeTaskDeletionMessage(_ item: SyncQueueItem, counts: TaskDescendantCounts) -> String {
        let isCreate = item.operation == .create

        if counts.isEmpty && counts.relatedQueueItems == 0 {
            return isCreate
                ? "This task was created offline and never synced. It will be permanently deleted."
                : "This sync queue item will be removed."
        }

        var parts: [String] = []
        if counts.nodes > 0 {
            parts.append("\(counts.nodes) linked node\(counts.nodes == 1 ? "" : "s")")
        }
        if counts.formInstances > 0 {
            parts.append("\(counts.formInstances) form instance\(counts.formInstances == 1 ? "" : "s")")
        }
        if counts.photos > 0 {
            parts.append("\(counts.photos) photo\(counts.photos == 1 ? "" : "s")")
        }
        if counts.relatedQueueItems > 0 {
            parts.append("\(counts.relatedQueueItems) related queue item\(counts.relatedQueueItems == 1 ? "" : "s")")
        }

        let countsString = parts.joined(separator: ", ")

        if isCreate {
            return "This task was created offline and has \(countsString). The task and all related items will be permanently deleted."
        } else {
            return "This task has \(countsString). All related queue items will also be removed."
        }
    }

    private func makeIssueDeletionMessage(_ item: SyncQueueItem, counts: IssueDescendantCounts) -> String {
        let isCreate = item.operation == .create

        if counts.isEmpty && counts.relatedQueueItems == 0 {
            return isCreate
                ? "This issue was created offline and never synced. It will be permanently deleted."
                : "This sync queue item will be removed."
        }

        var parts: [String] = []
        if counts.photos > 0 {
            parts.append("\(counts.photos) photo\(counts.photos == 1 ? "" : "s")")
        }
        if counts.irPhotos > 0 {
            parts.append("\(counts.irPhotos) IR photo\(counts.irPhotos == 1 ? "" : "s")")
        }
        if counts.tasks > 0 {
            parts.append("\(counts.tasks) linked task\(counts.tasks == 1 ? "" : "s")")
        }
        if counts.relatedQueueItems > 0 {
            parts.append("\(counts.relatedQueueItems) related queue item\(counts.relatedQueueItems == 1 ? "" : "s")")
        }

        let countsString = parts.joined(separator: ", ")

        if isCreate {
            return "This issue was created offline and has \(countsString). The issue and all related items will be permanently deleted."
        } else {
            return "This issue has \(countsString). All related queue items will also be removed."
        }
    }

    private func deleteQueueItem(_ item: SyncQueueItem) {
        modelContext.delete(item)
        try? modelContext.save()
        networkState.refreshSyncQueueCount()
        viewModel.loadData(for: 0)
    }

    private func performLocationDeletion() {
        guard let item = pendingLocationDeletion else { return }

        let syncService = SyncQueueService(modelContext: modelContext)

        do {
            if item.target == .attachment {
                try syncService.deleteAttachmentQueueItem(item)
            } else if item.target == .userTask {
                try syncService.deleteTaskQueueItem(item)
            } else if item.target == .issue {
                try syncService.deleteIssueQueueItem(item)
            } else {
                try syncService.deleteLocationQueueItem(item)
            }
            networkState.refreshSyncQueueCount()
        } catch {
            // Fallback: just delete queue item (which also refreshes the count)
            deleteQueueItem(item)
        }

        pendingLocationDeletion = nil
        locationDeletionCounts = nil
        viewModel.loadData(for: 0)
    }

    private func clearSyncHistory() {
        // ZP-1265 (aa123c2) — User-scoped clear. Wipe only the current
        // user's logs plus legacy/unstamped rows (userId == nil). Other
        // accounts that share this device keep their offline-survival
        // audit trail intact (matching Android's
        // `deleteForUserAndUnscoped(userId)` behavior).
        //
        // Fetch directly from the modelContext rather than `viewModel.syncLogs`
        // because that array is capped at 200 entries — clearing only the
        // visible page would leave older rows behind.
        let activeUserId: UUID? = {
            let nullSentinel = UUID(uuidString: "00000000-0000-0000-0000-000000000000")!
            let id = AppStateManager.shared.userId
            return id == nullSentinel ? nil : id
        }()

        guard let currentUser = activeUserId else {
            // Skip the destructive action when we can't identify the
            // current user — defensive, mirrors Android's same guard.
            AppLogger.log(.notice, "Skipping Clear History — no current userId available", category: .sync)
            return
        }

        let allLogsDescriptor = FetchDescriptor<SyncLog>()
        if let allLogs = try? modelContext.fetch(allLogsDescriptor) {
            for log in allLogs where log.userId == nil || log.userId == currentUser {
                modelContext.delete(log)
            }
        }
        try? modelContext.save()
        viewModel.loadData(for: selectedSegment)
    }
    
    private static let sectionDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        return formatter
    }()

    private func formatSectionDate(_ date: Date) -> String {
        if Calendar.current.isDateInToday(date) {
            return AppStrings.Diagnostics.today
        } else if Calendar.current.isDateInYesterday(date) {
            return AppStrings.Diagnostics.yesterday
        } else {
            return Self.sectionDateFormatter.string(from: date)
        }
    }
    
    private func requeueAllFailedOperations() {
        Task {
            await MainActor.run {
                // Skip mapping logs — their payload isn't recoverable from the log so
                // re-queuing would only insert orphan items that fail immediately.
                let retryable = viewModel.failedLogs.filter { !$0.target.isMapping }
                let result = networkState.requeueFailedOperations(from: retryable)
                AppLogger.log(.info, "Re-queued \(result.successful) operations, \(result.failed) failed", category: .sync)
                viewModel.loadAllData()
            }
        }
    }

    private func exportSyncQueue() {
        AppLogger.log(.info, "Exporting sync queue with \(viewModel.pendingItems.count) items...", category: .sync)

        // Generate export report
        let report = SyncQueueExportService.generateExportReport(
            queueItems: viewModel.pendingItems,
            modelContext: modelContext
        )

        // Convert to JSON string
        guard let jsonString = SyncQueueExportService.generateJSONString(from: report) else {
            AppLogger.log(.error, "Failed to generate JSON string", category: .sync)
            return
        }

        // Save to temporary file (optional, for logging)
        if let fileURL = SyncQueueExportService.saveExportToTempFile(jsonString: jsonString) {
            AppLogger.log(.info, "Export file saved: \(fileURL.path)", category: .sync)
            exportFileURL = fileURL
        }

        AppLogger.log(.info, "Export contains \(report.queueItems.count) queue items", category: .sync)

        // Show share sheet with data - using item binding ensures data is available
        exportData = ExportDataWrapper(jsonString: jsonString)
    }
}

// MARK: - Pending Item Row
struct PendingItemRow: View {
    let item: SyncQueueItem
    /// ZP-1847 — site name lookup keyed by SLDChoice id, supplied by the
    /// parent so each row can show a per-site label without an extra fetch.
    /// Defaults to empty so existing call sites keep compiling.
    var siteNamesById: [UUID: String] = [:]
    /// ZP-2097 — three row variants: idle (defaults), syncing (rotating
    /// sync icon, no swipe / actions), failed (error icon + inline failure
    /// log + Retry/Delete buttons). Failure is derived from the row's
    /// `lastFailureAt` so it survives app restart.
    var isSyncing: Bool = false
    /// ZP-1265 (aa123c2) — when false (set by the parent during a flush),
    /// the per-item Retry/Delete buttons are hidden so the user can't
    /// kick a parallel sync. The inline failure log stays visible.
    var showActions: Bool = true
    var onRetry: (() -> Void)? = nil
    var onDelete: (() -> Void)? = nil
    @State private var showingDetails = false
    @State private var spinnerAngle: Double = 0

    private var isFailed: Bool { item.lastFailureAt != nil }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Header
            HStack {
                Label {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("\(item.target) - \(item.operation)")
                            .font(.headline)
                        if isSyncing {
                            Text(AppStrings.Diagnostics.statusSyncing)
                                .font(.caption2)
                                .foregroundColor(.blue)
                        } else if isFailed {
                            Text(AppStrings.Diagnostics.statusFailed)
                                .font(.caption2)
                                .foregroundColor(.red)
                        }
                    }
                } icon: {
                    if isSyncing {
                        Image(systemName: "arrow.triangle.2.circlepath")
                            .foregroundColor(.blue)
                            .rotationEffect(.degrees(spinnerAngle))
                            .onAppear {
                                withAnimation(
                                    .linear(duration: 1.2).repeatForever(autoreverses: false)
                                ) {
                                    spinnerAngle = 360
                                }
                            }
                    } else if isFailed {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundColor(.red)
                    } else {
                        iconForTarget(item.target)
                            .foregroundColor(colorForTarget(item.target))
                    }
                }

                Spacer()

                // Show badge if extraData is present
                if let extraData = item.extraData, !extraData.isEmpty {
                    Text(AppStrings.Diagnostics.extraData)
                        .font(.caption)
                        .foregroundColor(.orange)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background(Color.orange.opacity(0.2))
                        .cornerRadius(4)
                }

                // ZP-2097 — retryCount is now an informational chip ("Retried
                // N times"), not a failure status. Color-coded only when
                // currently failed.
                if item.retryCount > 0 {
                    Text(AppStrings.Diagnostics.retriedNTimes(item.retryCount))
                        .font(.caption)
                        .foregroundColor(isFailed ? .red : .secondary)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background((isFailed ? Color.red : Color.gray).opacity(0.15))
                        .cornerRadius(4)
                }
            }
            
            // Entity Info
            if let entityInfo = getEntityInfo(for: item) {
                Text(entityInfo)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }
            
            // Site label (ZP-1847). Resolved from SLDChoice via the parent's
            // lookup map. If the device's site list doesn't contain a name
            // for this siteId yet (e.g. the SLDChoice table hasn't been
            // fetched this session, or the user no longer has access to the
            // originating site), the pill is hidden rather than showing a
            // raw UUID — the queue item's entity-info line above already
            // provides context.
            if let siteId = item.siteId, let siteName = siteNamesById[siteId] {
                HStack(spacing: 4) {
                    Image(systemName: "building.2")
                        .font(.caption2)
                    Text(siteName)
                        .font(.caption)
                        .lineLimit(1)
                }
                .foregroundColor(.indigo)
            }

            // ZP-2097 — failed-item inline log + Retry/Delete actions.
            // Driven entirely by the row's persisted `lastFailureAt`, so it
            // survives app restart even when the in-memory `haltedSyncItemId`
            // is gone.
            if isFailed, let message = item.lastFailureMessage {
                VStack(alignment: .leading, spacing: 4) {
                    // Hide the synthetic unrecoverable-legacy sentinel — it isn't a
                    // real HTTP / URLError code; the inline message already explains.
                    if let code = item.lastFailureCode,
                       code != SyncConfiguration.unrecoverableLegacyFailureCode {
                        Text(AppStrings.Diagnostics.failureCode(String(code)))
                            .font(.caption2)
                            .fontWeight(.bold)
                            .foregroundColor(.red)
                    }
                    Text(message)
                        .font(.caption)
                        .foregroundColor(.red)
                        .lineLimit(showingDetails ? nil : 2)
                    if let failedAt = item.lastFailureAt {
                        Text(AppStrings.Diagnostics.failedAt(formatDate(failedAt)))
                            .font(.caption2)
                            .foregroundColor(.red.opacity(0.8))
                    }
                }
                .padding(10)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.red.opacity(0.08))
                .cornerRadius(8)
            }

            // ZP-1265 (aa123c2) — Retry / Delete are hidden during a flush
            // (`showActions = !isFlashing`) so the user can't kick a parallel
            // sync. The inline failure log above stays visible.
            if isFailed && showActions {
                HStack(spacing: 8) {
                    Button {
                        onRetry?()
                    } label: {
                        Label(AppStrings.Diagnostics.retryAction, systemImage: "arrow.clockwise")
                            .font(.caption)
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.small)
                    .disabled(onRetry == nil)

                    Button(role: .destructive) {
                        onDelete?()
                    } label: {
                        Label(AppStrings.Diagnostics.deleteAction, systemImage: "trash")
                            .font(.caption)
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                    .disabled(onDelete == nil)
                }
            }

            // Timestamp
            HStack {
                Image(systemName: "clock")
                    .font(.caption)
                Text("\(AppStrings.Diagnostics.queued): \(item.createdAt, style: .relative) ago")
                    .font(.caption)
            }
            .foregroundColor(.secondary)
            
            // Expandable details
            if showingDetails {
                GroupBox {
                    VStack(alignment: .leading, spacing: 4) {
                        DetailRow(label: "Queue ID", value: item.id.uuidString)
                        if let nodeId = item.nodeId {
                            DetailRow(label: "Node ID", value: nodeId.uuidString)
                        }
                        if let edgeId = item.edgeId {
                            DetailRow(label: "Edge ID", value: edgeId.uuidString)
                        }
                        if let photoId = item.photoId {
                            DetailRow(label: "Photo ID", value: photoId.uuidString)
                        }

                        // Display extraData if present
                        if let extraData = item.extraData, !extraData.isEmpty {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("\(AppStrings.Diagnostics.extraData):")
                                    .fontWeight(.medium)
                                    .foregroundColor(.orange)
                                ForEach(Array(extraData.keys.sorted()), id: \.self) { key in
                                    HStack {
                                        Text("  • \(key):")
                                            .foregroundColor(.orange)
                                        Text("\(String(describing: extraData[key] ?? "nil"))")
                                            .foregroundColor(.secondary)
                                    }
                                }
                            }
                        }

                        DetailRow(label: "Created", value: formatDate(item.createdAt))
                    }
                    .font(.caption)
                }
            }
        }
        .padding(.vertical, 4)
        .onTapGesture {
            withAnimation {
                showingDetails.toggle()
            }
        }
    }
    
    private func getEntityInfo(for item: SyncQueueItem) -> String? {
        switch item.target {
        case .node:
            if let id = item.nodeId {
                return "Node: \(id.uuidString.prefix(8))..."
            }
        case .edge:
            if let id = item.edgeId {
                return "Edge: \(id.uuidString.prefix(8))..."
            }
        case .photo:
            if let id = item.photoId {
                return "Photo: \(id.uuidString.prefix(8))..."
            }
        case .userTask:
            if let id = item.userTaskId {
                return "Task: \(id.uuidString.prefix(8))..."
            }
        case .irPhoto:
            if let id = item.irPhotoId {
                return "IR Photo: \(id.uuidString.prefix(8))..."
            }
        case .irSession:
            if let id = item.irSessionId {
                return "IR Session: \(id.uuidString.prefix(8))..."
            }
        case .issue:
            if let id = item.issueId {
                return "Issue: \(id.uuidString.prefix(8))..."
            }
        case .quote:
            if let id = item.quoteId {
                return "Quote: \(id.uuidString.prefix(8))..."
            }
        case .building:
            if let id = item.buildingId {
                return "Building: \(id.uuidString.prefix(8))..."
            }
        case .floor:
            if let id = item.floorId {
                return "Floor: \(id.uuidString.prefix(8))..."
            }
        case .room:
            if let id = item.roomId {
                return "Room: \(id.uuidString.prefix(8))..."
            }
        case .mappingIssueTask:
            if let mappingData = item.mappingData {
                let issuePrefix = mappingData.issueId?.uuidString.prefix(8) ?? "?"
                let taskPrefix = mappingData.taskId?.uuidString.prefix(8) ?? "?"
                return "Issue→Task: \(issuePrefix)→\(taskPrefix)"
            }
        case .mappingTaskSession:
            if let mappingData = item.mappingData {
                let taskPrefix = mappingData.taskId?.uuidString.prefix(8) ?? "?"
                let sessionPrefix = mappingData.sessionId?.uuidString.prefix(8) ?? "?"
                return "Task→Session: \(taskPrefix)→\(sessionPrefix)"
            }
        case .mappingQuoteTask:
            if let mappingData = item.mappingData {
                let quotePrefix = mappingData.quoteId?.uuidString.prefix(8) ?? "?"
                let taskPrefix = mappingData.taskId?.uuidString.prefix(8) ?? "?"
                return "Quote→Task: \(quotePrefix)→\(taskPrefix)"
            }
        case .mappingUserTask:
            if let mappingData = item.mappingData {
                let userPrefix = mappingData.userId?.uuidString.prefix(8) ?? "?"
                let taskPrefix = mappingData.taskId?.uuidString.prefix(8) ?? "?"
                return "User→Task: \(userPrefix)→\(taskPrefix)"
            }
        case .mappingTaskNode:
            if let mappingData = item.mappingData {
                let taskPrefix = mappingData.taskId?.uuidString.prefix(8) ?? "?"
                let nodePrefix = mappingData.nodeId?.uuidString.prefix(8) ?? "?"
                return "Task→Node: \(taskPrefix)→\(nodePrefix)"
            }
        case .mappingTaskForm:
            if let mappingData = item.mappingData {
                let taskPrefix = mappingData.taskId?.uuidString.prefix(8) ?? "?"
                let formPrefix = mappingData.formId?.uuidString.prefix(8) ?? "?"
                return "Task→Form: \(taskPrefix)→\(formPrefix)"
            }
        case .formInstance:
            if let id = item.formInstanceId {
                return "Form Instance: \(id.uuidString.prefix(8))..."
            }
        case .mappingTaskFormInstance:
            if let mappingData = item.mappingData {
                let taskPrefix = mappingData.taskId?.uuidString.prefix(8) ?? "?"
                let formInstancePrefix = mappingData.formInstanceId?.uuidString.prefix(8) ?? "?"
                return "Task→Form Instance: \(taskPrefix)→\(formInstancePrefix)"
            }
        case .mappingFormInstanceNode:
            if let mappingData = item.mappingData {
                let formInstancePrefix = mappingData.formInstanceId?.uuidString.prefix(8) ?? "?"
                let nodePrefix = mappingData.nodeId?.uuidString.prefix(8) ?? "?"
                return "Form Instance→Node: \(formInstancePrefix)→\(nodePrefix)"
            }
        case .mappingNodeSession:
            if let mappingData = item.mappingData {
                let nodePrefix = mappingData.nodeId?.uuidString.prefix(8) ?? "?"
                let sessionPrefix = mappingData.sessionId?.uuidString.prefix(8) ?? "?"
                return "Node→Session: \(nodePrefix)→\(sessionPrefix)"
            }
        case .mappingUserSession:
            if let mappingData = item.mappingData {
                let userPrefix = mappingData.userId?.uuidString.prefix(8) ?? "?"
                let sessionPrefix = mappingData.sessionId?.uuidString.prefix(8) ?? "?"
                return "User→Session: \(userPrefix)→\(sessionPrefix)"
            }
        case .attachment:
            if let id = item.attachmentId {
                return "Attachment: \(id.uuidString.prefix(8))..."
            }
        case .mappingAttachmentNode:
            if let mappingData = item.mappingData {
                let attachmentPrefix = mappingData.attachmentId?.uuidString.prefix(8) ?? "?"
                let nodePrefix = mappingData.nodeId?.uuidString.prefix(8) ?? "?"
                return "Attachment→Node: \(attachmentPrefix)→\(nodePrefix)"
            }
        case .sldView:
            if let id = item.sldViewId {
                return "SLD View: \(id.uuidString.prefix(8))..."
            }
        case .mappingNodeSLDView:
            if let mappingData = item.mappingData {
                let nodePrefix = mappingData.nodeId?.uuidString.prefix(8) ?? "?"
                let viewPrefix = mappingData.sldViewId?.uuidString.prefix(8) ?? "?"
                return "Node→View: \(nodePrefix)→\(viewPrefix)"
            }
        case .mappingEdgeSLDView:
            if let mappingData = item.mappingData {
                let edgePrefix = mappingData.edgeId?.uuidString.prefix(8) ?? "?"
                let viewPrefix = mappingData.sldViewId?.uuidString.prefix(8) ?? "?"
                return "Edge→View: \(edgePrefix)→\(viewPrefix)"
            }
        case .mappingTaskNodeBulkCompletion:
            if let mappingData = item.mappingData {
                let taskPrefix = mappingData.taskId?.uuidString.prefix(8) ?? "?"
                return "Task→Nodes Bulk Completion: \(taskPrefix)"
            }
        case .egFormInstance:
            if let id = item.egFormInstanceId {
                return "EG Form Instance: \(id.uuidString.prefix(8))..."
            }
        case .mappingTaskEGFormInstance:
            if let mappingData = item.mappingData {
                let taskPrefix = mappingData.taskId?.uuidString.prefix(8) ?? "?"
                let instancePrefix = mappingData.formInstanceId?.uuidString.prefix(8) ?? "?"
                return "Task→EG Form Instance: \(taskPrefix)→\(instancePrefix)"
            }
        case .mappingEGFormInstanceNode:
            if let mappingData = item.mappingData {
                let instancePrefix = mappingData.formInstanceId?.uuidString.prefix(8) ?? "?"
                let nodePrefix = mappingData.nodeId?.uuidString.prefix(8) ?? "?"
                return "EG Form Instance→Node: \(instancePrefix)→\(nodePrefix)"
            }
        }
        return nil
    }
}

// MARK: - Sync Log Row
struct SyncLogRow: View {
    let log: SyncLog
    var siteNamesById: [UUID: String] = [:]
    @State private var showingDetails = false
    // ZP-2097: per-item retry removed from History; isRequeuing/requeueOperation
    // are kept around the file but unused. Compiler will warn but they're harmless;
    // bulk re-queue still uses requeueFailedOperations on the toolbar.
    @State private var isRequeuing = false
    @EnvironmentObject var networkState: NetworkState

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Header
            HStack {
                Label {
                    Text("\(log.target) - \(log.operation)")
                        .font(.headline)
                } icon: {
                    Image(systemName: log.success ? "checkmark.circle.fill" : "xmark.circle.fill")
                        .foregroundColor(log.success ? .green : .red)
                }

                Spacer()

                // ZP-2097 — per-item Retry was here in the old design; removed
                // in the strict-stop redesign. Pending tab is now the single
                // resolution surface — failed items live there with inline
                // Retry/Delete affordances until the user resolves them.
                // History remains a read-only audit trail; only the "Re-queue
                // all failed" toolbar action survives for bulk recovery
                // scenarios.
                if log.isRequeued {
                    Label(AppStrings.Diagnostics.queued, systemImage: "checkmark")
                        .font(.caption)
                        .foregroundColor(.green)
                }

                if let duration = log.duration {
                    Text("\(String(format: "%.2fs", duration))")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            // Entity label or error
            if !log.success, let error = log.errorMessage {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
                    .lineLimit(2)
            } else if let label = log.entityLabel {
                Text(label)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }
            
            // Site label — mirrors the Pending tab pill so History rows show
            // the originating site whenever the log carries a `siteId` and the
            // VM lookup has a name for it.
            if let siteId = log.siteId, let siteName = siteNamesById[siteId] {
                HStack(spacing: 4) {
                    Image(systemName: "building.2")
                        .font(.caption2)
                    Text(siteName)
                        .font(.caption)
                        .lineLimit(1)
                }
                .foregroundColor(.indigo)
            }

            // Timestamp
            Text(formatDate(log.timestamp))
                .font(.caption)
                .foregroundColor(.secondary)

            // Expandable details
            if showingDetails {
                GroupBox {
                    VStack(alignment: .leading, spacing: 4) {
                        DetailRow(label: "Log ID", value: log.id.uuidString)
                        if let entityId = log.entityId {
                            DetailRow(label: "Entity ID", value: entityId.uuidString)
                        }
                        
                        // Photo-specific details
                        if log.target == .photo {
                            if let nodeId = log.photoNodeId {
                                DetailRow(label: "Node ID", value: nodeId.uuidString)
                            }
                            if let originalPath = log.photoOriginalPath {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(AppStrings.Diagnostics.originalPath)
                                        .fontWeight(.medium)
                                    Text(originalPath)
                                        .foregroundColor(.secondary)
                                        .font(.system(.caption, design: .monospaced))
                                        .lineLimit(3)
                                }
                            }
                            if let attemptedPath = log.photoAttemptedPath {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(AppStrings.Diagnostics.attemptedPath)
                                        .fontWeight(.medium)
                                    Text(attemptedPath)
                                        .foregroundColor(.secondary)
                                        .font(.system(.caption, design: .monospaced))
                                        .lineLimit(3)
                                }
                            }
                            if let remoteURL = log.photoRemoteURL {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(AppStrings.Diagnostics.remoteUrl)
                                        .fontWeight(.medium)
                                    Text(remoteURL)
                                        .foregroundColor(.secondary)
                                        .font(.system(.caption, design: .monospaced))
                                        .lineLimit(3)
                                }
                            }
                        }
                        
                        if let statusCode = log.httpStatusCode,
                           statusCode != SyncConfiguration.unrecoverableLegacyFailureCode {
                            DetailRow(label: "HTTP Status", value: "\(statusCode)")
                        }
                        if let error = log.errorMessage {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(AppStrings.Diagnostics.errorLabel)
                                    .fontWeight(.medium)
                                Text(error)
                                    .foregroundColor(.red)
                                    .font(.caption)
                            }
                        }
                    }
                    .font(.caption)
                }
            }
        }
        .padding(.vertical, 4)
        .onTapGesture {
            withAnimation {
                showingDetails.toggle()
            }
        }
    }
    
    private func requeueOperation() {
        isRequeuing = true

        Task {
            await MainActor.run {
                let success = networkState.requeueFailedOperation(from: log)

                withAnimation {
                    isRequeuing = false
                    if success {
                        log.isRequeued = true
                    }
                }
            }
        }
    }
}

// MARK: - Helper Views
struct DetailRow: View {
    let label: String
    let value: String
    
    var body: some View {
        HStack {
            Text("\(label):")
                .fontWeight(.medium)
            Text(value)
                .foregroundColor(.secondary)
                .lineLimit(1)
            Spacer()
        }
    }
}

// MARK: - Helper Functions
private func iconForTarget(_ target: SyncTarget) -> Image {
    switch target {
    case .node:
        return Image(systemName: "cube.box")
    case .edge:
        return Image(systemName: "link")
    case .photo:
        return Image(systemName: "photo")
    case .userTask:
        return Image(systemName: "checklist")
    case .irPhoto:
        return Image(systemName: "camera.aperture")
    case .irSession:
        return Image(systemName: "camera.viewfinder")
    case .issue:
        return Image(systemName: "exclamationmark.triangle")
    case .quote:
        return Image(systemName: "doc.text")
    case .building:
        return Image(systemName: "building.2")
    case .floor:
        return Image(systemName: "square.stack.3d.up")
    case .room:
        return Image(systemName: "door.left.hand.open")
    case .mappingIssueTask:
        return Image(systemName: "arrow.triangle.branch")
    case .mappingTaskSession:
        return Image(systemName: "arrow.triangle.branch")
    case .mappingQuoteTask:
        return Image(systemName: "arrow.triangle.branch")
    case .mappingUserTask:
        return Image(systemName: "arrow.triangle.branch")
    case .mappingTaskNode:
        return Image(systemName: "arrow.triangle.branch")
    case .mappingTaskForm:
        return Image(systemName: "arrow.triangle.branch")
    case .formInstance:
        return Image(systemName: "doc.badge.plus")
    case .mappingTaskFormInstance:
        return Image(systemName: "arrow.triangle.branch")
    case .mappingFormInstanceNode:
        return Image(systemName: "arrow.triangle.branch")
    case .mappingNodeSession:
        return Image(systemName: "arrow.triangle.branch")
    case .mappingUserSession:
        return Image(systemName: "arrow.triangle.branch")
    case .attachment:
        return Image(systemName: "paperclip")
    case .mappingAttachmentNode:
        return Image(systemName: "arrow.triangle.branch")
    case .sldView:
        return Image(systemName: "rectangle.3.group")
    case .mappingNodeSLDView:
        return Image(systemName: "rectangle.on.rectangle")
    case .mappingEdgeSLDView:
        return Image(systemName: "arrow.left.and.right")
    case .mappingTaskNodeBulkCompletion:
        return Image(systemName: "checkmark.circle.fill")
    case .egFormInstance:
        return Image(systemName: "doc.text.fill")
    case .mappingTaskEGFormInstance:
        return Image(systemName: "arrow.triangle.branch")
    case .mappingEGFormInstanceNode:
        return Image(systemName: "arrow.triangle.branch")
    }
}

private func colorForTarget(_ target: SyncTarget) -> Color {
    switch target {
    case .node:
        return .blue
    case .edge:
        return .purple
    case .photo:
        return .orange
    case .userTask:
        return .green
    case .irPhoto:
        return .pink
    case .irSession:
        return .indigo
    case .issue:
        return .red
    case .quote:
        return .teal
    case .building:
        return .brown
    case .floor:
        return .mint
    case .room:
        return .yellow
    case .mappingIssueTask:
        return .gray
    case .mappingTaskSession:
        return .gray
    case .mappingQuoteTask:
        return .gray
    case .mappingUserTask:
        return .gray
    case .mappingTaskNode:
        return .gray
    case .mappingTaskForm:
        return .gray
    case .formInstance:
        return .cyan
    case .mappingTaskFormInstance:
        return .gray
    case .mappingFormInstanceNode:
        return .gray
    case .mappingNodeSession:
        return .gray
    case .mappingUserSession:
        return .gray
    case .attachment:
        return .orange
    case .mappingAttachmentNode:
        return .gray
    case .sldView:
        return .purple
    case .mappingNodeSLDView:
        return .cyan
    case .mappingEdgeSLDView:
        return .cyan
    case .mappingTaskNodeBulkCompletion:
        return .green
    case .egFormInstance:
        return .cyan
    case .mappingTaskEGFormInstance:
        return .gray
    case .mappingEGFormInstanceNode:
        return .gray
    }
}

private let sharedDateFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.dateStyle = .short
    formatter.timeStyle = .medium
    return formatter
}()

private func formatDate(_ date: Date) -> String {
    return sharedDateFormatter.string(from: date)
}

// MARK: - Share Sheet
struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        let controller = UIActivityViewController(activityItems: items, applicationActivities: nil)
        return controller
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {
        // No update needed
    }
}
