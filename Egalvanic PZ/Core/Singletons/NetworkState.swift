import SwiftUI
import SwiftData
import Combine

/// Quick QR Action options for in-session QR code scanning
enum QuickQRAction: String, CaseIterable {
    case fullAsset = "Full Asset"
    case dataCollection = "Data Collection"
    case irPhotos = "IR Photos"

    /// Convert to NodeDetailFocusMode
    var focusMode: NodeDetailFocusMode {
        switch self {
        case .fullAsset:
            return .all
        case .dataCollection:
            return .coreAttributes
        case .irPhotos:
            return .irPhotos
        }
    }
}

final class NetworkState: ObservableObject {
    // MARK: - Singleton
    static let shared = NetworkState()
    
    // MARK: - Published Properties
    @Published var mode: NetworkMode = .online {
        didSet {
            // Delegate to NetworkStateService for mode change handling
            networkStateService.mode = mode
            networkStateService.handleNetworkModeChange(from: oldValue, to: mode)
            // Keep our existing logic too (needs to run on MainActor)
            Task { @MainActor in
                handleNetworkModeChange()
            }
        }
    }
    @Published var isSyncing: Bool = false

    /// ZP-2097 — id of the queue row currently being uploaded. nil when idle.
    /// `SyncQueueAnalyzerView` reads this to drive the per-item rotating
    /// sync icon. Set by `SyncExecutionService` around each item.
    @Published var currentSyncItemId: UUID?

    /// ZP-1265 (aa123c2) — one-shot flag latched true when a flush halts
    /// because reachability dropped mid-flight. The analyzer view consumes
    /// it to render the "Internet connection lost" dialog and clears it
    /// via `acknowledgeNetworkLost()`. No auto-retry on reconnect — the
    /// user must explicitly tap Sync Now.
    @Published var networkLostDuringSync: Bool = false

    /// ZP-1265 (aa123c2) — hardware reachability passthrough so the
    /// offline banner can distinguish "no internet" (hardware) from
    /// "your offline mode is on" (manual toggle). Updated from
    /// `NetworkReachability.shared.$isConnected` by the existing
    /// reachability subscription in `setupNetworkMonitoring`.
    @Published var isNetworkAvailable: Bool = true

    /// ZP-1265 (aa123c2) — dismiss the network-lost dialog. The user must
    /// explicitly tap Sync Now after reconnecting; we don't auto-retry on
    /// reachability returning.
    @MainActor
    func acknowledgeNetworkLost() {
        networkLostDuringSync = false
    }
    @Published private(set) var syncProgress: Int = 0
    @Published private(set) var syncTotal: Int = 0
    @Published private(set) var syncQueueCount: Int = 0
    /// ZP-2173 (Android parity) — true while at least one queue row predates
    /// `userId` stamping (`SyncQueueItem.userId == nil`). Drives the gates on
    /// destructive UI surfaces (logout / site switch / schedule / pull-to-
    /// refresh / IR session refresh) that would otherwise wipe the live
    /// SwiftData entity tables those legacy rows still depend on, turning
    /// them into permanent "Entity no longer exists" failures. Kept warm by
    /// every queue-mutation path that already updates `syncQueueCount`.
    @Published private(set) var hasLegacySyncItems: Bool = false
    @Published var showNetworkAlert: Bool = false
    @Published var networkAlertTitle: String = AppStrings.Alerts.networkConnectionRequired
    @Published var networkAlertMessage: String = ""

    // MARK: - Session-scoped Settings
    /// Quick QR Action determines which mode to open EditNodeDetailViewV3 in when scanning QR codes in-session
    @Published var quickQRAction: QuickQRAction = .fullAsset
    /// Task IDs currently in "listening" mode — auto-link to new/added nodes. Ephemeral, resets on app restart.
    @Published var listeningTaskIds: Set<UUID> = []
    
    // MARK: - Private Properties
    private let api = APIClient.shared
    private(set) var modelContext: ModelContext?
    private var networkReachabilityCancellable: AnyCancellable?

    /// Timestamp of last successful server refresh, used for throttling
    private var lastRefreshTime: Date?
    /// Minimum interval between full server refreshes (seconds)
    private let minimumRefreshInterval: TimeInterval = 30

    // MARK: - Service Dependencies
    private let networkStateService: NetworkStateService
    private var photoUploadService: PhotoUploadService?
    private var deduplicationService: SyncDeduplicationService?
    private var syncQueueService: SyncQueueService?
    private var syncExecutionService: SyncExecutionService?
    private var stuckPhotoRecoveryService: StuckPhotoRecoveryService?

    /// One-shot guard so orphan cleanup runs only once per app process, even though
    /// setModelContext is called on every ContentView appearance.
    private var didRunOrphanCleanup: Bool = false

    // MARK: - Initialization
    private init() {
        // Initialize services
        self.networkStateService = NetworkStateService(initialMode: .online)
        // PhotoUploadService and SyncDeduplicationService will be initialized when setModelContext is called (requires MainActor)

        // Sync initial mode from service
        self.mode = networkStateService.mode

        // Setup network reachability monitoring
        setupNetworkMonitoring()
    }

    deinit {
        networkReachabilityCancellable?.cancel()
    }
    
    // MARK: - Computed Properties (delegating to service)
    var isOnline: Bool {
        networkStateService.isOnline
    }
    
    var isOffline: Bool {
        networkStateService.isOffline
    }

    /// ZP-1847: a change can bypass the queue only when the user is online
    /// AND has zero pending items for the current user across all sites.
    /// Otherwise queue mode is forced so older items always sync first and
    /// creation-order is preserved server-side.
    ///
    /// `syncQueueCount` is already user-scoped (see `SyncQueueService.getQueueCount`).
    var canDirectSync: Bool {
        return isOnline && syncQueueCount == 0
    }
    
    // MARK: - Public Methods

    /// Setup automatic network connectivity monitoring
    private func setupNetworkMonitoring() {
        Task { @MainActor in
            let reachability = NetworkReachability.shared
            // ZP-1265 (aa123c2) — seed isNetworkAvailable from reachability
            // so the offline banner has the right state on first render
            // (before the publisher emits its first delta).
            self.isNetworkAvailable = reachability.isConnected
            networkReachabilityCancellable = reachability.$isConnected
                .dropFirst() // Skip initial value
                .sink { [weak self] isConnected in
                    guard let self = self else { return }

                    Task { @MainActor in
                        // ZP-1265 (aa123c2) — mirror reachability so the
                        // analyzer's offline banner picks the right copy
                        // (hardware-offline vs manual-offline-toggle).
                        self.isNetworkAvailable = isConnected
                        if !isConnected && self.mode == .online {
                            // Internet lost - automatically switch to offline mode
                            slog("Internet connection lost - switching to offline mode", category: .network, level: .warning)
                            // Only set self.mode - didSet will handle networkStateService update
                            self.mode = .offline
                        }
                        // Note: We don't automatically go online when internet returns
                        // User must manually switch to online mode for better UX
                    }
                }
        }
    }

    /// Toggle between online and offline modes
    @MainActor
    func toggleMode() {
        // Check actual network status if trying to go online
        if mode == .offline {
            let reachability = NetworkReachability.shared
            if !reachability.isConnected {
                slog("Cannot switch to online mode - no network connection", category: .network, level: .warning)
                // Show alert to user - delay to allow Menu to dismiss first
                Task { @MainActor in
                    try? await Task.sleep(nanoseconds: 500_000_000) // 0.5 second delay
                    self.networkAlertMessage = "No internet connection. Please check your connection and try again."
                    self.showNetworkAlert = true
                }
                return
            }
        }

        networkStateService.toggleMode()
        self.mode = networkStateService.mode
    }
    
    // Set the model context (called from ContentView)
    @MainActor
    func setModelContext(_ context: ModelContext) {
        self.modelContext = context
        
        // Initialize services now that we're on MainActor
        if photoUploadService == nil {
            photoUploadService = PhotoUploadService(modelContext: context)
        } else {
            photoUploadService?.setModelContext(context)
        }
        
        if deduplicationService == nil {
            deduplicationService = SyncDeduplicationService()
        }
        
        if syncQueueService == nil {
            syncQueueService = SyncQueueService(modelContext: context)
        } else {
            syncQueueService?.setModelContext(context)
        }
        
        if syncExecutionService == nil {
            syncExecutionService = SyncExecutionService(modelContext: context, photoUploadService: photoUploadService)
            syncExecutionService?.setDependencies(deduplicationService: deduplicationService, syncQueueService: syncQueueService)
        } else {
            syncExecutionService?.setModelContext(context)
            syncExecutionService?.setDependencies(deduplicationService: deduplicationService, syncQueueService: syncQueueService)
        }

        // Initialize TaskMappingService with model context
        TaskMappingService.shared.setModelContext(context)

        // Initialize the stuck-photo sweeper. Runs on boot and on online-transition to
        // recover photos left in an intermediate state by the previous upload-ordering bug
        // or by max-retry queue-item deletion.
        if let photoUploadService = photoUploadService {
            stuckPhotoRecoveryService = StuckPhotoRecoveryService(
                modelContext: context,
                networkState: self,
                photoUploadService: photoUploadService
            )
            // Defer to the next event loop tick so the UI has a chance to draw first.
            // Run orphan cleanup before stuck-photo recovery so recovery's "already queued"
            // check (which filters nil photoIds) has an accurate view of the queue.
            // Cleanup is gated by didRunOrphanCleanup so it only runs once per process,
            // even if setModelContext is re-invoked (e.g., onViewAppear).
            Task { @MainActor [weak self] in
                if let self, !self.didRunOrphanCleanup {
                    self.didRunOrphanCleanup = true
                    // Rewrite legacy absolute photoFilePath values (v1.33 and
                    // earlier) to Documents-relative form so they survive the
                    // iOS Data container UUID changing across app updates.
                    // Must run before orphan cleanup and before the first
                    // flush attempt.
                    self.syncQueueService?.migrateLegacyPhotoFilePaths()
                    self.syncQueueService?.cleanupOrphanQueueItems()
                }
                self?.stuckPhotoRecoveryService?.recoverStuckPhotos()
            }
        }

        loadSyncQueueCount()
    }
    
    // Load the current count of sync queue items
    @MainActor
    private func loadSyncQueueCount() {
        syncQueueCount = syncQueueService?.getQueueCount() ?? 0
        // ZP-2173 — keep the legacy-item flag warm alongside the queue count so
        // every existing mutation path that already refreshes the count also
        // refreshes the destructive-action gate without extra plumbing.
        hasLegacySyncItems = (syncQueueService?.getLegacyItemCount() ?? 0) > 0
    }

    /// Refresh the sync queue count from the database
    /// Call this after manually deleting queue items to keep the badge in sync
    @MainActor
    func refreshSyncQueueCount() {
        loadSyncQueueCount()
    }
    
    // Enqueue a sync operation (now delegates to SyncQueueService)
    @MainActor
    func enqueue(_ op: SyncOp) {
        guard let syncQueueService = syncQueueService else {
            slog("SyncQueueService not initialized", category: .sync, level: .error)
            return
        }
        
        syncQueueService.enqueue(op)
        loadSyncQueueCount()
    }
    
    /// Enqueue multiple sync operations with a single save for better performance
    @MainActor
    func enqueueBatch(_ ops: [SyncOp]) {
        guard let syncQueueService = syncQueueService else {
            slog("SyncQueueService not initialized", category: .sync, level: .error)
            return
        }
        syncQueueService.enqueueBatch(ops)
        loadSyncQueueCount()
    }

    /// Remove all queued operations for a given attachment
    @MainActor
    func removeQueueItems(forAttachmentId attachmentId: UUID) {
        guard let syncQueueService = syncQueueService else { return }
        syncQueueService.removeQueueItems(forAttachmentId: attachmentId)
        loadSyncQueueCount()
    }

    // Convenience methods for enqueueing mapping operations (now delegate to SyncQueueService)
    @MainActor
    func enqueueIssueTaskMapping(issueId: UUID, taskId: UUID, isDeleted: Bool) {
        guard let syncQueueService = syncQueueService else {
            slog("SyncQueueService not initialized", category: .sync, level: .error)
            return
        }
        syncQueueService.enqueueIssueTaskMapping(issueId: issueId, taskId: taskId, isDeleted: isDeleted)
        loadSyncQueueCount()
    }
    
    @MainActor
    func enqueueTaskSessionMapping(taskId: UUID, sessionId: UUID, isDeleted: Bool) {
        guard let syncQueueService = syncQueueService else {
            slog("SyncQueueService not initialized", category: .sync, level: .error)
            return
        }
        syncQueueService.enqueueTaskSessionMapping(taskId: taskId, sessionId: sessionId, isDeleted: isDeleted)
        loadSyncQueueCount()
    }
    
    @MainActor
    func enqueueQuoteTaskMapping(quoteId: UUID, taskId: UUID, isDeleted: Bool) {
        guard let syncQueueService = syncQueueService else {
            slog("SyncQueueService not initialized", category: .sync, level: .error)
            return
        }
        syncQueueService.enqueueQuoteTaskMapping(quoteId: quoteId, taskId: taskId, isDeleted: isDeleted)
        loadSyncQueueCount()
    }
    
    @MainActor
    func enqueueUserTaskMapping(userId: UUID, taskId: UUID, mappingType: String, isDeleted: Bool) {
        guard let syncQueueService = syncQueueService else {
            slog("SyncQueueService not initialized", category: .sync, level: .error)
            return
        }
        syncQueueService.enqueueUserTaskMapping(userId: userId, taskId: taskId, mappingType: mappingType, isDeleted: isDeleted)
        loadSyncQueueCount()
    }

    @MainActor
    func enqueueNodeSLDViewCreate(mappingId: UUID, nodeId: UUID, viewId: UUID, x: Double, y: Double, width: Double?, height: Double?) {
        guard let syncQueueService = syncQueueService else {
            slog("SyncQueueService not initialized", category: .sync, level: .error)
            return
        }
        syncQueueService.enqueueNodeSLDViewCreate(mappingId: mappingId, nodeId: nodeId, viewId: viewId, x: x, y: y, width: width, height: height)
        loadSyncQueueCount()
    }

    @MainActor
    func enqueueNodeSLDViewUpdate(mappingId: UUID, nodeId: UUID, viewId: UUID, x: Double, y: Double, width: Double?, height: Double?, isCollapsed: Bool = false) {
        guard let syncQueueService = syncQueueService else {
            slog("SyncQueueService not initialized", category: .sync, level: .error)
            return
        }
        syncQueueService.enqueueNodeSLDViewUpdate(mappingId: mappingId, nodeId: nodeId, viewId: viewId, x: x, y: y, width: width, height: height, isCollapsed: isCollapsed)
        loadSyncQueueCount()
    }

    @MainActor
    func enqueueEdgeSLDViewUpdate(mappingId: UUID, edgeId: UUID, viewId: UUID, points: [[String: Double]]?, algorithm: String?) {
        guard let syncQueueService = syncQueueService else {
            slog("SyncQueueService not initialized", category: .sync, level: .error)
            return
        }
        syncQueueService.enqueueEdgeSLDViewUpdate(mappingId: mappingId, edgeId: edgeId, viewId: viewId, points: points, algorithm: algorithm)
        loadSyncQueueCount()
    }

    // Queue management methods have been moved to SyncQueueService
    
    // Helper method for photo creation with upload (now delegates to PhotoUploadService)
    @MainActor
    private func handlePhotoCreate(photo: Photo) async throws {
        guard let photoUploadService = photoUploadService else {
            throw NSError(domain: "NetworkState", code: -1, userInfo: [NSLocalizedDescriptionKey: "PhotoUploadService not initialized"])
        }
        
        // Delegate to PhotoUploadService
        try await photoUploadService.uploadPhoto(photo)
        
        // Log the success
        let metadata = photoUploadService.createPhotoUploadMetadata(for: photo)
        let log = SyncLog(
            target: .photo,
            operation: .create,
            entityId: photo.id,
            entityLabel: photo.filename,
            success: true,
            duration: 0 // PhotoUploadService doesn't track duration yet
        )
        log.setPhotoMetadata(
            nodeId: metadata.nodeId,
            originalPath: metadata.originalPath,
            attemptedPath: metadata.attemptedPath,
            remoteURL: metadata.remoteURL
        )
        self.logSyncOperationWithLog(log)
    }
    
    // Helper for success logging
    
    @MainActor
    private func logSuccess(target: SyncTarget, operation: SyncOperation, entityId: UUID, entityLabel: String?, startTime: Date) async {
        await MainActor.run {
            self.logSyncOperation(
                target: target,
                operation: operation,
                entityId: entityId,
                entityLabel: entityLabel,
                success: true,
                startTime: startTime
            )
        }
    }
    
    // Helper for mapping success logging
    
    @MainActor
    private func logMappingSuccess(type: String, ids: String, isDeleted: Bool, startTime: Date) async {
        await MainActor.run {
            let log = SyncLog(
                target: type.contains("issue") ? .mappingIssueTask :
                        type.contains("quote") ? .mappingQuoteTask : .mappingTaskSession,
                operation: isDeleted ? .update : .create,
                entityId: nil,
                entityLabel: type,
                success: true,
                duration: Date().timeIntervalSince(startTime)
            )
            log.setMappingMetadata(type: type, ids: ids)
            self.logSyncOperationWithLog(log)
        }
    }
    
    @MainActor
    private func logFailure(op: SyncOp, error: Error, startTime: Date) async {
        // Break down the entity ID extraction
        let entityId: UUID?
        let entityLabel: String
        
        switch op.target {
        case .node:
            entityId = op.node?.id
            entityLabel = op.node?.label ?? "Unknown Node"
        case .edge:
            entityId = op.edge?.id
            entityLabel = "\(op.edge?.source?.uuidString ?? "nil") -> \(op.edge?.target?.uuidString ?? "nil")"
        case .photo:
            entityId = op.photo?.id
            entityLabel = op.photo?.filename ?? "Unknown Photo"
        case .userTask:
            entityId = op.userTask?.id
            entityLabel = op.userTask?.title ?? "Unknown Task"
        case .irPhoto:
            entityId = op.irPhoto?.id
            entityLabel = "\(op.irPhoto?.visual_photo_key ?? "") / \(op.irPhoto?.ir_photo_key ?? "")"
        case .irSession:
            entityId = op.irSession?.id
            entityLabel = op.irSession?.name ?? "Unknown Session"
        case .issue:
            entityId = op.issue?.id
            entityLabel = op.issue?.title ?? "Unknown Issue"
        case .quote:
            entityId = op.quote?.id
            entityLabel = op.quote?.title ?? "Unknown Quote"
        case .formInstance:
            entityId = op.formInstance?.id
            entityLabel = "Form Instance: \(op.formInstance?.id.uuidString ?? "Unknown")"
        case .egFormInstance:
            entityId = op.egFormInstance?.id
            entityLabel = "EG Form Instance: \(op.egFormInstance?.id.uuidString ?? "Unknown")"
        case .building:
            entityId = op.building?.id
            entityLabel = op.building?.name ?? "Unknown Building"
        case .floor:
            entityId = op.floor?.id
            entityLabel = op.floor?.name ?? "Unknown Floor"
        case .room:
            entityId = op.room?.id
            entityLabel = op.room?.name ?? "Unknown Room"
        case .attachment:
            entityId = op.attachment?.id
            entityLabel = op.attachment?.filename ?? "Unknown Attachment"
        case .sldView:
            entityId = op.sldView?.id
            entityLabel = op.sldView?.name ?? "Unknown View"
        case .mappingIssueTask, .mappingTaskSession, .mappingQuoteTask, .mappingUserTask, .mappingTaskNode, .mappingTaskForm, .mappingTaskFormInstance, .mappingFormInstanceNode, .mappingNodeSession, .mappingAttachmentNode, .mappingNodeSLDView, .mappingEdgeSLDView, .mappingUserSession, .mappingTaskNodeBulkCompletion, .mappingTaskEGFormInstance, .mappingEGFormInstanceNode:
            entityId = nil
            entityLabel = "Mapping: \(op.target)"
        }

        if op.target == .photo, let photo = op.photo {
            // Photo-specific logging
            let filename = photo.filename ?? ""
            let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            let attemptedPath = documentsURL.appendingPathComponent("photos").appendingPathComponent(filename).path
            let nodeId = photo.node?.id
            let originalPath = photo.local_filepath
            let remoteURL = photo.url
            
            await MainActor.run {
                let log = SyncLog(
                    target: .photo,
                    operation: op.operation,
                    entityId: photo.id,
                    entityLabel: filename,
                    success: false,
                    errorMessage: error.localizedDescription,
                    duration: Date().timeIntervalSince(startTime)
                )
                log.setPhotoMetadata(
                    nodeId: nodeId,
                    originalPath: originalPath,
                    attemptedPath: attemptedPath,
                    remoteURL: remoteURL
                )
                self.logSyncOperationWithLog(log)
            }
        } else if case .mappingIssueTask = op.target,
                  let mappingData = op.mappingData {
            await MainActor.run {
                let log = SyncLog(
                    target: op.target,
                    operation: op.operation,
                    entityId: nil,
                    entityLabel: "Issue-Task Mapping",
                    success: false,
                    errorMessage: error.localizedDescription,
                    duration: Date().timeIntervalSince(startTime)
                )
                log.setMappingMetadata(
                    type: "issue-task",
                    ids: "\(mappingData.issueId?.uuidString ?? "?")->\(mappingData.taskId?.uuidString ?? "?")"
                )
                self.logSyncOperationWithLog(log)
            }
        } else {
            // Standard logging
            await MainActor.run {
                self.logSyncOperation(
                    target: op.target,
                    operation: op.operation,
                    entityId: entityId,
                    entityLabel: entityLabel,
                    success: false,
                    error: error,
                    startTime: startTime
                )
            }
        }
    }
    
    
    /// Refresh local data from server after successful sync
    @MainActor
    private func refreshFromServer() async {
        guard let context = modelContext else {
            slog("No model context available for refresh", category: .sync, level: .error)
            return
        }

        // Guard: skip if already syncing
        if SLDSyncService.shared.isSyncing {
            slog("Skipping refresh - sync already in progress", category: .sync)
            return
        }

        // Throttle: skip if refreshed recently
        if let lastRefresh = lastRefreshTime,
           Date().timeIntervalSince(lastRefresh) < minimumRefreshInterval {
            slog("Skipping refresh - last refresh was \(Int(Date().timeIntervalSince(lastRefresh)))s ago (min: \(Int(minimumRefreshInterval))s)", category: .sync)
            return
        }

        do {
            slog("Refreshing local data from server...", category: .sync)
            try await SLDSyncService.shared.upsertAllData(sld_id: AppStateManager.shared.activeSLDId, modelContext: context)
            lastRefreshTime = Date()
            slog("Successfully refreshed local data from server", category: .sync)
        } catch {
            slog("Failed to refresh from server", category: .sync, level: .error, data: ["error": error.localizedDescription])
        }
    }

    /// Check if we should auto-flush when coming online
    @MainActor
    func handleNetworkModeChange() {
        if mode == .online && syncQueueCount > 0 {
            slog("Came online with pending operations", category: .sync, data: ["pending_count": syncQueueCount])
        }
        // Whenever we flip to online, sweep for stuck photos. Cheap fetch; safe no-op if clean.
        // This recovers photos left in the intermediate state (upload_needed=true, no queue item)
        // by prior bugs or by max-retry queue-item deletion.
        if mode == .online {
            stuckPhotoRecoveryService?.recoverStuckPhotos()
        }
    }
    
    /// Log a sync operation for audit trail
    @MainActor
    func logSyncOperation(target: SyncTarget, operation: SyncOperation, entityId: UUID?, entityLabel: String?, success: Bool, error: Error? = nil, statusCode: Int? = nil, startTime: Date) {
        let log = SyncLog(
            target: target,
            operation: operation,
            entityId: entityId,
            entityLabel: entityLabel,
            success: success,
            errorMessage: error?.localizedDescription,
            httpStatusCode: statusCode,
            duration: Date().timeIntervalSince(startTime)
        )
        logSyncOperationWithLog(log)
    }
    
    /// Log a sync operation with a pre-configured log object
    @MainActor
    func logSyncOperationWithLog(_ log: SyncLog) {
        guard let context = modelContext else {
            slog("No model context available for logging", category: .sync, level: .error)
            return
        }

        context.insert(log)

        do {
            try context.save()
            slog("Logged sync operation", category: .sync, data: [
                "target": "\(log.target)",
                "operation": "\(log.operation)",
                "success": log.success
            ])
        } catch {
            slog("Failed to save sync log", category: .sync, level: .error, data: ["error": error.localizedDescription])
        }
    }
}

extension NetworkState {
    
    /// Main flush queue method - delegates to SyncExecutionService
    func flushQueue(completion: (() async -> Void)? = nil) {
        Task { @MainActor in
            // Check network connectivity first
            let reachability = NetworkReachability.shared
            if !reachability.isConnected {
                // Show alert to user
                networkAlertMessage = "No network connection detected. Please check your internet connection and try again."
                showNetworkAlert = true
                slog("Sync aborted - no network connection", category: .sync, level: .error)
                await completion?()
                return
            }

            guard let syncExecutionService = syncExecutionService else {
                slog("SyncExecutionService not initialized", category: .sync, level: .error)
                await completion?()
                return
            }

            // ZP-1265 (aa123c2) — reset live progress trackers at the start
            // of every flush. `networkLostDuringSync` is one-shot and gets
            // cleared by `acknowledgeNetworkLost()` (user dismissing the
            // dialog) — we DON'T clear it here so a flush kicked off
            // immediately after a network-lost event still surfaces the
            // banner / dialog state until the user acknowledges it.
            currentSyncItemId = nil
            
            // Set up callbacks for real-time updates
            syncExecutionService.onQueueCountUpdate = { [weak self] _ in
                // Defer to loadSyncQueueCount so the legacy-item flag refreshes
                // alongside the count — keeps the destructive-action gate in
                // sync as items drain mid-flush.
                Task { @MainActor in
                    self?.loadSyncQueueCount()
                }
            }
            
            syncExecutionService.onSyncLog = { [weak self] log in
                guard let self = self else { return }
                await self.logSyncOperationWithLog(log)
            }
            
            // Create a task to monitor progress
            let progressTask = Task { @MainActor in
                while !Task.isCancelled {
                    if syncExecutionService.isSyncing {
                        self.isSyncing = syncExecutionService.isSyncing
                        self.syncProgress = syncExecutionService.syncProgress
                        self.syncTotal = syncExecutionService.syncTotal
                    }
                    try? await Task.sleep(nanoseconds: 50_000_000) // 0.05 second for smoother updates
                }
            }
            
            // Delegate to service
            let (totalSuccessful, totalFailed) = await syncExecutionService.orchestrateSyncFlush()
            
            // Cancel progress monitoring
            progressTask.cancel()

            // Only clear mirrors if no other flush still owns the slot — a
            // concurrent flushQueue() that bounced inside orchestrateSyncFlush
            // would otherwise clobber the in-flight pass's published state.
            if !syncExecutionService.isSyncing {
                self.isSyncing = false
                self.syncProgress = 0
                self.syncTotal = 0
            }
            
            // Update sync queue count after completion
            loadSyncQueueCount()
            
            // Only refresh if we had some successes and no failures
            if totalFailed == 0 && totalSuccessful > 0 {
                // Wait for mutation worker to process before refreshing
                // The backend uses async mutation middleware that returns 202 Accepted immediately
                // but processes the actual database writes in a background worker
                slog("Waiting for mutation worker to process...", category: .sync)
                try? await Task.sleep(nanoseconds: 1_500_000_000) // 1.5 seconds

                slog("Refreshing from server (all operations succeeded)...", category: .sync)
                await refreshFromServer()
                slog("Server refresh complete", category: .sync)
            } else if totalFailed > 0 {
                slog("Skipping server refresh due to failed operations", category: .sync, level: .warning, data: ["failed_count": totalFailed])
            }

            await completion?()
            slog("Flush queue completion handler executed", category: .sync)
        }
    }
    
    // MARK: - Failed Operations Management
    
    /// Re-queue a failed operation from sync log
    @MainActor
    func requeueFailedOperation(from log: SyncLog) -> Bool {
        guard let syncQueueService = syncQueueService else {
            slog("SyncQueueService not initialized", category: .sync, level: .error)
            return false
        }
        
        let success = syncQueueService.requeueFailedOperation(from: log)
        if success {
            log.isRequeued = true
            loadSyncQueueCount()
        }
        return success
    }
    
    /// Re-queue multiple failed operations
    @MainActor
    func requeueFailedOperations(from logs: [SyncLog]) -> (successful: Int, failed: Int) {
        guard let syncQueueService = syncQueueService else {
            slog("SyncQueueService not initialized", category: .sync, level: .error)
            return (0, logs.count)
        }
        
        let result = syncQueueService.requeueFailedOperations(from: logs)
        loadSyncQueueCount()
        return result
    }
    
    /// Get recent failed sync operations
    @MainActor
    func getFailedSyncLogs(limit: Int = 50) -> [SyncLog] {
        guard let context = modelContext else { return [] }
        
        let descriptor = FetchDescriptor<SyncLog>(
            predicate: #Predicate { log in
                log.success == false
            },
            sortBy: [SortDescriptor(\.timestamp, order: .reverse)]
        )
        
        do {
            var fetchDescriptor = descriptor
            fetchDescriptor.fetchLimit = limit
            return try context.fetch(fetchDescriptor)
        } catch {
            slog("Failed to fetch sync logs", category: .sync, level: .error, data: ["error": error.localizedDescription])
            return []
        }
    }
}
