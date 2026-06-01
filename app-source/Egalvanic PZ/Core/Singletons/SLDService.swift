//
//  SLDService.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/8/25.
//

import Foundation
import SwiftData

// MARK: - Clear Progress Tracking

/// Progress details for clearing site data
struct ClearProgressDetails {
    var currentStep: String = ""
    var currentEntityType: String = ""
    var currentEntityCount: Int = 0
    var processedEntities: Int = 0

    // Total counts across all entity types
    var totalSyncItems: Int = 0
    var totalIRPhotos: Int = 0
    var totalSessions: Int = 0
    var totalPhotos: Int = 0
    var totalTasks: Int = 0
    var totalForms: Int = 0
    var totalQuotes: Int = 0
    var totalIssues: Int = 0
    var totalProperties: Int = 0
    var totalEdges: Int = 0
    var totalNodes: Int = 0
    var totalSLDs: Int = 0

    // Processed counts
    var clearedSyncItems: Int = 0
    var clearedIRPhotos: Int = 0
    var clearedSessions: Int = 0
    var clearedPhotos: Int = 0
    var clearedTasks: Int = 0
    var clearedForms: Int = 0
    var clearedQuotes: Int = 0
    var clearedIssues: Int = 0
    var clearedProperties: Int = 0
    var clearedEdges: Int = 0
    var clearedNodes: Int = 0
    var clearedSLDs: Int = 0

    var totalEntities: Int {
        totalSyncItems + totalIRPhotos + totalSessions + totalPhotos +
        totalTasks + totalForms + totalQuotes + totalIssues +
        totalProperties + totalEdges + totalNodes + totalSLDs
    }

    var clearedEntities: Int {
        clearedSyncItems + clearedIRPhotos + clearedSessions + clearedPhotos +
        clearedTasks + clearedForms + clearedQuotes + clearedIssues +
        clearedProperties + clearedEdges + clearedNodes + clearedSLDs
    }

    var progress: Double {
        guard totalEntities > 0 else { return 0 }
        return Double(clearedEntities) / Double(totalEntities)
    }

    mutating func reset() {
        self = ClearProgressDetails()
    }
}

@MainActor
class SLDService: ObservableObject {
    // MARK: - Singleton
    static let shared = SLDService()

    @Published var isSwitching = false
    @Published var switchError: Error?
    @Published var isRefreshing = false
    @Published var refreshError: Error?

    // Clear progress tracking
    @Published var isClearing = false
    @Published var clearProgress = ClearProgressDetails()

    private init() {
        // NetworkState is accessed via singleton
    }
    
    /// Switch to a different SLD (site).
    ///
    /// ZP-1847 (Android-parity, post-aa123c2): the sync queue, sync log, and
    /// SLDChoice rows survive the switch — `clearSLDs(preserveSyncMetadata: true)`
    /// wipes only site-scoped entity tables. Pending items carry self-contained
    /// DTO snapshots (ZP-1847) so they flush correctly to whichever site
    /// originated them, regardless of which site is currently active. The user
    /// can switch sites freely with offline edits in the queue.
    func switchToSLD(_ sldId: UUID, siteName: String? = nil, modelContext: ModelContext) async throws {
        // Don't switch if it's already the active SLD
        guard sldId != AppStateManager.shared.activeSLDId else {
            AppLogger.log(.debug, "Already on SLD: \(sldId)", category: .sync)
            return
        }

        isSwitching = true
        defer { isSwitching = false }

        AppLogger.log(.info, "Switching from SLD \(AppStateManager.shared.activeSLDId) to \(sldId)", category: .sync)

        // Set the site name in SLDSyncService for UI display
        if let siteName = siteName {
            SLDSyncService.shared.currentSiteName = siteName
        }

        // Enable site switch mode early so the loading view shows during clearing
        SLDSyncService.shared.isSiteSwitchMode = true

        // Step 1: Clear session-related data
        clearSessionData()

        // Step 2: Clear current SLD data from local storage.
        // ZP-1847 (Android-parity): preserve `SyncQueueItem` / `SyncLog` /
        // `SLDChoice` so queued offline work survives the switch. Snapshots
        // on the queue rows carry their own siteId, so they flush to the
        // originating site even after this switch lands the user elsewhere.
        try await clearSLDs(modelContext: modelContext, preserveSyncMetadata: true)

        // Step 3: Update active SLD
        AppStateManager.shared.setActiveSLD(sldId)

        // Step 4: Ensure network state has model context
        if NetworkState.shared.modelContext == nil {
            NetworkState.shared.setModelContext(modelContext)
        }

        // Step 5: Sync new SLD from server (pass isSiteSwitch: true for full loading screen)
        try await SLDSyncService.shared.upsertAllData(sld_id: sldId, modelContext: modelContext, isSiteSwitch: true)

        // Step 6 (ZP-2061): apply the site's office language (or restore the user's
        // preferred language if the office has none). Best-effort — never blocks the switch.
        SiteLanguageController.shared.onSiteSelected(siteId: sldId, modelContext: modelContext)

        AppLogger.log(.info, "Successfully switched to SLD: \(sldId)", category: .sync)
    }
    
    /// Refresh SLD data from server
    func refreshSLD(modelContext: ModelContext) async throws {
        guard NetworkState.shared.mode == .online else {
            throw SLDServiceError.offlineMode
        }
        
        isRefreshing = true
        defer { isRefreshing = false }
        
        AppLogger.log(.info, "Refreshing SLD: \(AppStateManager.shared.activeSLDId)", category: .sync)
        
        // Flush any pending operations
        NetworkState.shared.flushQueue()

        // Sync from server (isSiteSwitch defaults to false - won't show full loading screen)
        try await SLDSyncService.shared.upsertAllData(sld_id: AppStateManager.shared.activeSLDId, modelContext: modelContext)
        
        // Update the graph view if needed
        WebViewBridge.updateGraphFromSLD(AppStateManager.shared.activeSLDId, in: modelContext)
        
        AppLogger.log(.info, "Successfully refreshed SLD: \(AppStateManager.shared.activeSLDId)", category: .sync)
    }
    
    /// Fetch available SLDs for a user
    func fetchAvailableSLDs(for userId: UUID, modelContext: ModelContext) async throws -> [SLDChoice] {
        return await fetchAndUpsertSLDChoices(user_id: userId, modelContext: modelContext)
    }
    
    /// Clear all session-related data when switching sites
    private func clearSessionData() {
        AppStateManager.shared.setActiveSession(nil)
        AppStateManager.shared.setLastCreatedRoom(nil)
        AppStateManager.shared.setLastCreatedVisualPhotoKey(nil)
        AppStateManager.shared.setLastCreatedIRPhotoKey(nil)
        AppLogger.log(.debug, "Cleared session data", category: .sync)
    }
    
    @MainActor
    func fetchAndUpsertSLDChoices(user_id: UUID, modelContext: ModelContext) async -> [SLDChoice] {
        AppLogger.log(.debug, "Fetching User SLDs...", category: .sync)
        async let userChoiceDTOs = APIClient.shared.fetchUserChoiceDTOs(user_id: user_id)
        
        do {
            let choices = try await userChoiceDTOs
            AppLogger.log(.debug, "Received \(choices.count) SLD choices", category: .sync)
            
            var upsertedChoices: [SLDChoice] = []
            
            for dto in choices {
                // Try to fetch existing choice with the same ID
                let descriptor = FetchDescriptor<SLDChoice>(
                    predicate: #Predicate { $0.id == dto.id }
                )
                
                let existingChoices = try modelContext.fetch(descriptor)
                
                if let existingChoice = existingChoices.first {
                    // Update existing choice
                    existingChoice.name = dto.name
                    existingChoice.is_deleted = dto.is_deleted

                    // Update address fields
                    existingChoice.address_line_1 = dto.address_line_1
                    existingChoice.address_line_2 = dto.address_line_2
                    existingChoice.city = dto.city
                    existingChoice.state_province = dto.state_province
                    existingChoice.postal_code = dto.postal_code
                    existingChoice.country_code = dto.country_code
                    existingChoice.address_formatted = dto.address_formatted
                    existingChoice.address_latitude = dto.address_coordinates?["latitude"]
                    existingChoice.address_longitude = dto.address_coordinates?["longitude"]
                    existingChoice.account_id = dto.account_id
                    existingChoice.complexity_level = dto.complexity_level
                    existingChoice.labor_union_id = dto.labor_union_id
                    existingChoice.office_id = dto.office_id
                    existingChoice.office_language = dto.office_language

                    upsertedChoices.append(existingChoice)
                } else {
                    // Insert new choice
                    let newChoice = SLDChoice(
                        id: dto.id,
                        name: dto.name,
                        is_deleted: dto.is_deleted,
                        address_line_1: dto.address_line_1,
                        address_line_2: dto.address_line_2,
                        city: dto.city,
                        state_province: dto.state_province,
                        postal_code: dto.postal_code,
                        country_code: dto.country_code,
                        address_formatted: dto.address_formatted,
                        address_latitude: dto.address_coordinates?["latitude"],
                        address_longitude: dto.address_coordinates?["longitude"],
                        account_id: dto.account_id,
                        complexity_level: dto.complexity_level,
                        labor_union_id: dto.labor_union_id,
                        office_id: dto.office_id,
                        office_language: dto.office_language
                    )
                    modelContext.insert(newChoice)
                    upsertedChoices.append(newChoice)
                }
            }
            
            // Save the context
            try modelContext.save()
            AppLogger.log(.info, "Successfully upserted \(upsertedChoices.count) SLD choices", category: .sync)
            
            return upsertedChoices
            
        } catch {
            AppLogger.log(.error, "Error fetching or upserting SLD choices: \(error)", category: .sync)
            return []
        }
    }
    
    /// How often to update UI during clearing loops (every N items)
    private let clearProgressUpdateInterval: Int = 25

    @MainActor
    /// Clear all site-scoped local data.
    ///
    /// `preserveSyncMetadata` (ZP-1847): when true, the sync queue and sync
    /// log are kept intact. Use for non-destructive account switching, where
    /// the prior user's queued offline work must survive on device until they
    /// log back in. `SLDChoice` rows are always preserved (this method has
    /// never deleted them).
    func clearSLDs(modelContext: ModelContext, preserveSyncMetadata: Bool = false) async throws {
        isClearing = true
        clearProgress.reset()
        defer {
            isClearing = false
            clearProgress.reset()
        }

        do {
            // Fetch all counts upfront using count queries (fast, no memory)
            clearProgress.currentStep = "Calculating data to clear..."

            clearProgress.totalSyncItems = try modelContext.fetchCount(FetchDescriptor<SyncQueueItem>())
            clearProgress.totalNodes = try modelContext.fetchCount(FetchDescriptor<NodeV2>())
            clearProgress.totalEdges = try modelContext.fetchCount(FetchDescriptor<EdgeV2>())
            clearProgress.totalPhotos = try modelContext.fetchCount(FetchDescriptor<Photo>())
            clearProgress.totalIRPhotos = try modelContext.fetchCount(FetchDescriptor<IRPhoto>())
            clearProgress.totalSessions = try modelContext.fetchCount(FetchDescriptor<IRSession>())
            clearProgress.totalTasks = try modelContext.fetchCount(FetchDescriptor<UserTask>())
            clearProgress.totalForms = try modelContext.fetchCount(FetchDescriptor<UserTaskForm>())
            clearProgress.totalQuotes = try modelContext.fetchCount(FetchDescriptor<Quote>())
            clearProgress.totalIssues = try modelContext.fetchCount(FetchDescriptor<Issue>())
            let nodePropsCount = try modelContext.fetchCount(FetchDescriptor<NodeProperty>())
            let edgePropsCount = try modelContext.fetchCount(FetchDescriptor<EdgeProperty>())
            let issuePropsCount = try modelContext.fetchCount(FetchDescriptor<IssueProperty>())
            clearProgress.totalProperties = nodePropsCount + edgePropsCount + issuePropsCount
            clearProgress.totalSLDs = try modelContext.fetchCount(FetchDescriptor<SLDV2>())

            AppLogger.log(.info, "Clearing totals - Nodes: \(clearProgress.totalNodes), Edges: \(clearProgress.totalEdges), Photos: \(clearProgress.totalPhotos), IR Photos: \(clearProgress.totalIRPhotos), Total: \(clearProgress.totalEntities)", category: .sync)

            // Use background context deletion if ModelContainer is available
            if let modelContainer = AppStateManager.shared.modelContainer {
                AppLogger.log(.info, "Using background batch deletion...", category: .sync)
                let deleter = BackgroundImporter(modelContainer: modelContainer, batchSize: 500)

                try await deleter.deleteAllEntities(preserveSyncMetadata: preserveSyncMetadata) { entityType, processed, total in
                    await MainActor.run {
                        // Update cleared counts only (totals already set upfront)
                        switch entityType {
                        case "syncItems":
                            self.clearProgress.currentStep = "Clearing sync queue..."
                            self.clearProgress.currentEntityType = "sync items"
                            self.clearProgress.clearedSyncItems = processed
                        case "irPhotos":
                            self.clearProgress.currentStep = "Clearing IR photos..."
                            self.clearProgress.currentEntityType = "IR photos"
                            self.clearProgress.clearedIRPhotos = processed
                        case "photos":
                            self.clearProgress.currentStep = "Clearing photos..."
                            self.clearProgress.currentEntityType = "photos"
                            self.clearProgress.clearedPhotos = processed
                        case "tasks":
                            self.clearProgress.currentStep = "Clearing tasks..."
                            self.clearProgress.currentEntityType = "tasks"
                            self.clearProgress.clearedTasks = processed
                        case "forms":
                            self.clearProgress.currentStep = "Clearing forms..."
                            self.clearProgress.currentEntityType = "forms"
                            self.clearProgress.clearedForms = processed
                        case "issues":
                            self.clearProgress.currentStep = "Clearing issues..."
                            self.clearProgress.currentEntityType = "issues"
                            self.clearProgress.clearedIssues = processed
                        case "quotes":
                            self.clearProgress.currentStep = "Clearing quotes..."
                            self.clearProgress.currentEntityType = "quotes"
                            self.clearProgress.clearedQuotes = processed
                        case "sessions":
                            self.clearProgress.currentStep = "Clearing sessions..."
                            self.clearProgress.currentEntityType = "sessions"
                            self.clearProgress.clearedSessions = processed
                        case "edges":
                            self.clearProgress.currentStep = "Clearing connections..."
                            self.clearProgress.currentEntityType = "connections"
                            self.clearProgress.clearedEdges = processed
                        case "nodes":
                            self.clearProgress.currentStep = "Clearing equipment..."
                            self.clearProgress.currentEntityType = "equipment"
                            self.clearProgress.clearedNodes = processed
                        case "slds":
                            self.clearProgress.currentStep = "Clearing site data..."
                            self.clearProgress.currentEntityType = "diagrams"
                            self.clearProgress.clearedSLDs = processed
                        case "nodeProperties", "edgeProperties":
                            self.clearProgress.currentStep = "Clearing properties..."
                            self.clearProgress.currentEntityType = "properties"
                        case "issueProperties":
                            self.clearProgress.currentStep = "Clearing properties..."
                            self.clearProgress.currentEntityType = "properties"
                            // Mark all properties complete when issue properties done
                            if processed == total {
                                self.clearProgress.clearedProperties = self.clearProgress.totalProperties
                            }
                        case "rooms", "floors", "buildings":
                            self.clearProgress.currentStep = "Clearing locations..."
                            self.clearProgress.currentEntityType = "locations"
                        default:
                            self.clearProgress.currentStep = "Clearing \(entityType)..."
                            self.clearProgress.currentEntityType = entityType
                        }
                        self.clearProgress.currentEntityCount = total
                    }
                }

                // Refresh main context to see the deletions
                modelContext.processPendingChanges()

            } else {
                // Fallback to main context deletion if no ModelContainer
                AppLogger.log(.notice, "ModelContainer not available, using main context deletion...", category: .sync)
                try await clearSLDsOnMainContext(modelContext: modelContext, preserveSyncMetadata: preserveSyncMetadata)
            }

            // ZP-1847 — sweep orphan files in the protected dir whose queue
            // items are no longer present. Mirrors Android's
            // SLDService.clearAllSLDData GC-sweep behavior. Safe even when
            // preserveSyncMetadata is true: the surviving queue rows still
            // identify which protected directories must be kept.
            let survivingItems = (try? modelContext.fetch(FetchDescriptor<SyncQueueItem>())) ?? []
            SyncFileManager.shared.gcOrphans(currentQueueItemIds: Set(survivingItems.map { $0.id }))

            AppLogger.log(.info, "Successfully cleared ALL SLD-related data from SwiftData", category: .sync)
        } catch {
            AppLogger.log(.error, "Failed to clear SLD data: \(error)", category: .sync)
            throw error
        }
    }

    /// Fallback deletion on main context (slower but guaranteed to work)
    private func clearSLDsOnMainContext(modelContext: ModelContext, preserveSyncMetadata: Bool = false) async throws {
        let batchSize = 500
        let progressInterval = 50

        func deleteInBatches<T: PersistentModel>(
            _ type: T.Type,
            step: String,
            entityType: String,
            updateProgress: (Int, Int) -> Void
        ) async throws {
            clearProgress.currentStep = step
            clearProgress.currentEntityType = entityType
            let items = try modelContext.fetch(FetchDescriptor<T>())
            let total = items.count
            clearProgress.currentEntityCount = total

            for (index, item) in items.enumerated() {
                modelContext.delete(item)
                if (index + 1) % progressInterval == 0 {
                    updateProgress(index + 1, total)
                }
                if (index + 1) % batchSize == 0 {
                    try modelContext.save()
                    await Task.yield()
                }
            }
            if !items.isEmpty {
                try modelContext.save()
                updateProgress(total, total)
            }
        }

        // Delete in dependency order with progress updates
        // ZP-1847: when preserveSyncMetadata is true (re-auth Switch User flow),
        // skip these so the prior user's queue + log survive the switch.
        if !preserveSyncMetadata {
            try await deleteInBatches(SyncQueueItem.self, step: "Clearing sync queue...", entityType: "sync items") { p, t in
                clearProgress.totalSyncItems = t; clearProgress.clearedSyncItems = p
            }
            try await deleteInBatches(SyncLog.self, step: "Clearing sync logs...", entityType: "sync logs") { _, _ in }
        }
        try await deleteInBatches(NodeProperty.self, step: "Clearing properties...", entityType: "properties") { p, _ in
            clearProgress.clearedProperties = p
        }
        try await deleteInBatches(EdgeProperty.self, step: "Clearing properties...", entityType: "properties") { p, _ in
            clearProgress.clearedProperties += p
        }
        try await deleteInBatches(IssueProperty.self, step: "Clearing properties...", entityType: "properties") { p, _ in
            clearProgress.clearedProperties += p
        }
        try await deleteInBatches(IRPhoto.self, step: "Clearing IR photos...", entityType: "IR photos") { p, t in
            clearProgress.totalIRPhotos = t; clearProgress.clearedIRPhotos = p
        }
        try await deleteInBatches(Photo.self, step: "Clearing photos...", entityType: "photos") { p, t in
            clearProgress.totalPhotos = t; clearProgress.clearedPhotos = p
        }
        try await deleteInBatches(FormInstance.self, step: "Clearing form instances...", entityType: "form instances") { _, _ in }
        try await deleteInBatches(UserTask.self, step: "Clearing tasks...", entityType: "tasks") { p, t in
            clearProgress.totalTasks = t; clearProgress.clearedTasks = p
        }
        try await deleteInBatches(UserTaskForm.self, step: "Clearing forms...", entityType: "forms") { p, t in
            clearProgress.totalForms = t; clearProgress.clearedForms = p
        }
        try await deleteInBatches(Issue.self, step: "Clearing issues...", entityType: "issues") { p, t in
            clearProgress.totalIssues = t; clearProgress.clearedIssues = p
        }
        try await deleteInBatches(Quote.self, step: "Clearing quotes...", entityType: "quotes") { p, t in
            clearProgress.totalQuotes = t; clearProgress.clearedQuotes = p
        }
        try await deleteInBatches(IRSession.self, step: "Clearing sessions...", entityType: "sessions") { p, t in
            clearProgress.totalSessions = t; clearProgress.clearedSessions = p
        }
        try await deleteInBatches(SLDComment.self, step: "Clearing comments...", entityType: "comments") { _, _ in }
        try await deleteInBatches(EdgeV2.self, step: "Clearing connections...", entityType: "connections") { p, t in
            clearProgress.totalEdges = t; clearProgress.clearedEdges = p
        }
        // NodeTerminal - delete before nodes (terminals reference nodes)
        try await deleteInBatches(NodeTerminal.self, step: "Clearing terminals...", entityType: "terminals") { _, _ in }
        try await deleteInBatches(NodeV2.self, step: "Clearing equipment...", entityType: "equipment") { p, t in
            clearProgress.totalNodes = t; clearProgress.clearedNodes = p
        }
        try await deleteInBatches(Room.self, step: "Clearing locations...", entityType: "locations") { _, _ in }
        try await deleteInBatches(Floor.self, step: "Clearing locations...", entityType: "locations") { _, _ in }
        try await deleteInBatches(Building.self, step: "Clearing locations...", entityType: "locations") { _, _ in }
        // SLD views and links - delete before SLDV2 (views/links reference SLDs)
        try await deleteInBatches(SLDLinkV2.self, step: "Clearing view links...", entityType: "view links") { _, _ in }
        try await deleteInBatches(SLDViewV2.self, step: "Clearing views...", entityType: "views") { _, _ in }
        try await deleteInBatches(SLDV2.self, step: "Clearing site data...", entityType: "diagrams") { p, t in
            clearProgress.totalSLDs = t; clearProgress.clearedSLDs = p
        }
    }
}

// Custom errors for SLDService
enum SLDServiceError: LocalizedError {
    case offlineMode
    case syncFailed(underlying: Error)

    var errorDescription: String? {
        switch self {
        case .offlineMode:
            return "Cannot refresh while offline"
        case .syncFailed(let error):
            return "Failed to sync data: \(error.localizedDescription)"
        }
    }

    var recoverySuggestion: String? {
        switch self {
        case .offlineMode:
            return "Please check your internet connection and try again"
        case .syncFailed:
            return "Please try again later"
        }
    }
}
